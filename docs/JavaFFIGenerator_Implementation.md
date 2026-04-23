# JavaFFIGenerator 实现文档

## 概述

JavaFFIGenerator 是 Claw 编译器的 Java 目标 FFI（Foreign Function Interface）代码生成器，支持两种生成策略：
1. **Panama 策略**：使用 Java 22+ Foreign Function & Memory API（推荐）
2. **JNI 策略**：使用传统 JNI（降级方案）

## 两种生成策略

### 1. Panama 策略（推荐）

使用 Java 22+ 的 Project Panama Foreign Function & Memory API。

#### 优势
- 无需编写 JNI bridge 代码
- 类型安全（编译时检查）
- 更好的性能（避免 JNI 开销）
- 现代 Java 语法

#### 代码示例

```java
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public final class Sqlite3FFI {
    // 静态初始化：加载库和获取符号
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup SQLITE3_LOOKUP;

    static {
        SQLITE3_LOOKUP = SymbolLookup.libraryLookup(
            System.mapLibraryName("sqlite3"),
            Arena.global()
        );
    }

    // 常量
    public static final int SQLITE_OK = 0;

    // 类型转换工具
    public static MemorySegment toCString(Arena arena, String str) {
        if (str == null) return MemorySegment.NULL;
        return arena.allocateFrom(str, StandardCharsets.UTF_8);
    }

    public static String fromCString(MemorySegment segment) {
        if (segment == null || segment.equals(MemorySegment.NULL)) return null;
        return segment.getString(0, StandardCharsets.UTF_8);
    }

    // 函数绑定
    private static final MethodHandle MH_sqlite3_open;

    static {
        MH_sqlite3_open = LINKER.downcallHandle(
            findSymbol("sqlite3_open"),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS
            )
        );
    }

    // 高层包装方法
    public static int sqlite3_open(Arena arena, String filename, MemorySegment ppDb) {
        try {
            MemorySegment filename_c = toCString(arena, filename);
            return (int) MH_sqlite3_open.invokeExact(filename_c, ppDb);
        } catch (Throwable t) {
            throw new RuntimeException("FFI call failed: sqlite3_open", t);
        }
    }

    private static MemorySegment findSymbol(String name) {
        return SQLITE3_LOOKUP.find(name)
            .orElseThrow(() -> new UnsatisfiedLinkError("Symbol not found: " + name));
    }
}
```

### 2. JNI 策略（降级）

使用传统 JNI，需要手动编写 JNI bridge。

#### 优势
- 兼容旧版本 Java
- 灵活性更高

#### 缺点
- 需要编写 C bridge 代码
- 性能开销更大
- 类型安全性较差

#### 代码示例

```java
public final class Sqlite3FFI {
    static {
        System.loadLibrary("claw_jni_bridge");
    }

    public static final int SQLITE_OK = 0;

    // Native 方法声明
    public static native int sqlite3_open(String filename, long ppDb);
    public static native void sqlite3_close(long db);

    private Sqlite3FFI() {}
}
```

#### JNI Bridge 代码

```c
// claw_jni_bridge.c
#include <jni.h>
#include <sqlite3.h>

JNIEXPORT jint JNICALL Java_com_example_Sqlite3FFI_sqlite3_open
    (JNIEnv *env, jclass cls, jstring filename, jlong ppDb) {
    const char* c_filename = (*env)->GetStringUTFChars(env, filename, NULL);
    int result = sqlite3_open(c_filename, (sqlite3**)ppDb);
    (*env)->ReleaseStringUTFChars(env, filename, c_filename);
    return result;
}

JNIEXPORT void JNICALL Java_com_example_Sqlite3FFI_sqlite3_close
    (JNIEnv *env, jclass cls, jlong db) {
    sqlite3_close((sqlite3*)db);
}
```

编译：

```bash
gcc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
    -o libclaw_jni_bridge.so claw_jni_bridge.c -lsqlite3
```

## 完整功能

### 1. 库加载和符号查找

```java
private static final Linker LINKER = Linker.nativeLinker();

// 为每个库创建符号查找器
private static final SymbolLookup SQLITE3_LOOKUP;

static {
    SQLITE3_LOOKUP = SymbolLookup.libraryLookup(
        System.mapLibraryName("sqlite3"),
        Arena.global()
    );
}

// 在所有库中查找符号
private static MemorySegment findSymbol(String name) {
    var opt = SQLITE3_LOOKUP.find(name);
    if (opt.isEmpty()) {
        throw new UnsatisfiedLinkError("Symbol not found: " + name);
    }
    return opt.get();
}
```

### 2. 常量定义

```java
public static final int SQLITE_OK = 0;
public static final int SQLITE_ROW = 100;
public static final int CURLE_OK = 0;
```

### 3. 类型转换工具

```java
// String → C 字符串（MemorySegment）
public static MemorySegment toCString(Arena arena, String str) {
    if (str == null) return MemorySegment.NULL;
    return arena.allocateFrom(str, StandardCharsets.UTF_8);
}

// C 字符串 → String
public static String fromCString(MemorySegment segment) {
    if (segment == null || segment.equals(MemorySegment.NULL)) return null;
    return segment.getString(0, StandardCharsets.UTF_8);
}

// 分配指针大小内存
public static MemorySegment allocatePointer(Arena arena) {
    return arena.allocate(ValueLayout.ADDRESS);
}

// 读取指针值
public static MemorySegment readPointer(MemorySegment ptrSegment) {
    return ptrSegment.get(ValueLayout.ADDRESS, 0);
}
```

### 4. 函数绑定

```java
private static final MethodHandle MH_sqlite3_open;

static {
    MH_sqlite3_open = LINKER.downcallHandle(
        findSymbol("sqlite3_open"),
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    );
}
```

### 5. 包装方法

```java
public static int sqlite3_open(Arena arena, String filename, MemorySegment ppDb) {
    try {
        // 参数转换
        MemorySegment filename_c = toCString(arena, filename);

        // 调用 MethodHandle
        return (int) MH_sqlite3_open.invokeExact(filename_c, ppDb);
    } catch (Throwable t) {
        throw new RuntimeException("FFI call failed: sqlite3_open", t);
    }
}
```

### 6. 处理 Void 返回值

```java
public static void free(Arena arena, MemorySegment ptr) {
    try {
        MH_free.invokeExact(ptr);
    } catch (Throwable t) {
        throw new RuntimeException("FFI call failed: free", t);
    }
}
```

### 7. 处理 String 返回值

```java
public static String strdup(Arena arena, String s) {
    try {
        MemorySegment _result = (MemorySegment) MH_strdup.invokeExact(
            toCString(arena, s)
        );
        return fromCString(_result);
    } catch (Throwable t) {
        throw new RuntimeException("FFI call failed: strdup", t);
    }
}
```

## 类型映射系统

### 1. C 类型 → Java 类型（Panama）

```java
mapClawTypeToJavaType("Void")    → "void"
mapClawTypeToJavaType("Int")     → "int"
mapClawTypeToJavaType("Float")   → "double"
mapClawTypeToJavaType("String")  → "String"
mapClawTypeToJavaType("Bool")    → "boolean"
mapClawTypeToJavaType("Any")     → "MemorySegment"
mapClawTypeToJavaType("Pointer") → "MemorySegment"
mapClawTypeToJavaType("Int8")    → "byte"
mapClawTypeToJavaType("Int64")   → "long"
```

### 2. C 类型 → MemoryLayout

```java
mapClawTypeToMemoryLayout("Int")     → "ValueLayout.JAVA_INT"
mapClawTypeToMemoryLayout("Float")   → "ValueLayout.JAVA_DOUBLE"
mapClawTypeToMemoryLayout("String")  → "ValueLayout.ADDRESS"
mapClawTypeToMemoryLayout("Bool")    → "ValueLayout.JAVA_BOOLEAN"
mapClawTypeToMemoryLayout("Pointer") → "ValueLayout.ADDRESS"
```

### 3. C 类型 → JNI 类型

```java
mapClawTypeToJNIType("Void")    → "void"
mapClawTypeToJNIType("Int")     → "int"
mapClawTypeToJNIType("Float")   → "double"
mapClawTypeToJNIType("String")  → "String"
mapClawTypeToJNIType("Bool")    → "boolean"
mapClawTypeToJNIType("Pointer") → "long"
```

## 平台检测

生成的平台检测工具：

```java
final class ClawPlatform {
    enum OS { WINDOWS, LINUX, MACOS, FREEBSD, ANDROID, UNKNOWN }
    enum Arch { X86_64, ARM64, X86, ARM, UNKNOWN }

    static final OS CURRENT_OS;
    static final Arch CURRENT_ARCH;

    static {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win"))        CURRENT_OS = OS.WINDOWS;
        else if (osName.contains("mac"))   CURRENT_OS = OS.MACOS;
        else if (osName.contains("linux")) CURRENT_OS = OS.LINUX;
        else if (osName.contains("freebsd")) CURRENT_OS = OS.FREEBSD;
        else                                CURRENT_OS = OS.UNKNOWN;

        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.contains("amd64") || arch.contains("x86_64")) CURRENT_ARCH = Arch.X86_64;
        else if (arch.contains("aarch64") || arch.contains("arm64")) CURRENT_ARCH = Arch.ARM64;
        else if (arch.contains("x86") || arch.contains("i386")) CURRENT_ARCH = Arch.X86;
        else if (arch.contains("arm"))     CURRENT_ARCH = Arch.ARM;
        else                                CURRENT_ARCH = Arch.UNKNOWN;
    }

    static boolean isPlatform(OS... targets) {
        for (OS t : targets) if (t == CURRENT_OS) return true;
        return false;
    }

    static boolean isArch(Arch... targets) {
        for (Arch t : targets) if (t == CURRENT_ARCH) return true;
        return false;
    }

    static String libraryFileName(String name) {
        return switch (CURRENT_OS) {
            case WINDOWS -> name + ".dll";
            case MACOS   -> "lib" + name + ".dylib";
            default      -> "lib" + name + ".so";
        };
    }
}
```

## 使用方式

### 基本用法

```java
// 创建 FFI 绑定表
FFIBindingTable table = new FFIBindingTable();

// 添加头文件和链接
table.getAllIncludes().add("sqlite3.h");
table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

// 添加常量
ExternConstant ok = new ExternConstant();
ok.name = "SQLITE_OK";
ok.value = "0";
table.getAllConstants().put("SQLITE_OK", ok);

// 添加函数
ExternFunction openFunc = new ExternFunction("sqlite3_open",
    List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
    "Int", false);
table.getAllFunctions().put("sqlite3_open", openFunc);

// 生成 Panama 绑定
JavaFFIGenerator generator = new JavaFFIGenerator(table);
String javaCode = generator.generateAll();
```

### 使用特定策略和包名

```java
JavaFFIGenerator generator = new JavaFFIGenerator(
    table,
    JavaFFIGenerator.Strategy.PANAMA,
    "com.example.lib"
);
```

### 生成平台检测

```java
JavaFFIGenerator generator = new JavaFFIGenerator(table);
String platformDetection = generator.generatePlatformDetection();
```

### 生成条件绑定

```java
String platformBindings = generator.generatePlatformConditionalBindings(table);
```

## 最佳实践

### 1. 使用 Arena 管理内存

```java
// 推荐方式：使用局部 Arena
try (Arena arena = Arena.ofConfined()) {
    MemorySegment ptr = toCString(arena, "hello");
    int result = sqlite3_open(arena, "database.db", allocatePointer(arena));
}
```

### 2. 错误处理

```java
try {
    int result = sqlite3_open(arena, filename, ppDb);
    if (result != SQLITE_OK) {
        throw new RuntimeException("Failed to open database");
    }
} catch (RuntimeException e) {
    // 处理错误
}
```

### 3. 类型注解

```java
/**
 * Open a SQLite database connection
 *
 * @param filename Database file path
 * @param ppDb Output parameter: pointer to sqlite3* handle
 * @return SQLITE_OK on success
 */
public static int sqlite3_open(Arena arena, String filename, MemorySegment ppDb) {
    // 实现
}
```

## 性能考虑

- 使用 `Arena.ofConfined()` 处理短生命周期操作
- 使用 `Arena.global()` 处理长时间操作
- 减少不必要的类型转换
- 使用 MethodHandle.invokeExact 避免调用开销

## 测试覆盖

创建了完整的测试套件 `JavaFFIGeneratorTest.java`，包含 **26 个测试用例**：

1. `testGeneratePanamaBinding()` - 测试 Panama 绑定生成
2. `testGenerateJNIBinding()` - 测试 JNI 绑定生成
3. `testGeneratePanamaImports()` - 测试导入生成
4. `testGeneratePanamaStaticInit()` - 测试静态初始化
5. `testGeneratePanamaConstants()` - 测试常量生成
6. `testGenerateArenaHelpers()` - 测试 Arena 工具方法
7. `testGeneratePanamaFunctionHandles()` - 测试函数绑定
8. `testGeneratePanamaWrapperMethods()` - 测试包装方法
9. `testGeneratePlatformDetection()` - 测试平台检测
10. `testGeneratePlatformConditionalBindings()` - 测试条件绑定
11. `testGenerateJavaPlatformCondition()` - 测试平台条件
12. `testMapClawTypeToJavaType()` - 测试 Java 类型映射
13. `testMapClawTypeToMemoryLayout()` - 测试 MemoryLayout 映射
14. `testMapClawTypeToJNIType()` - 测试 JNI 类型映射
15. `testGetBindingClassName()` - 测试绑定类名
16. `testToPascalCase()` - 测试 Pascal Case 转换
17. `testToConstantName()` - 测试常量名转换
18. `testGenerateAllWithPanama()` - 测试完整 Panama 生成
19. `testGenerateAllWithJNI()` - 测试完整 JNI 生成
20. `testGenerateWithMultipleLibraries()` - 测试多库生成
21. `testGenerateWithVoidReturnType()` - 测试 Void 返回值
22. `testGenerateWithStringReturnType()` - 测试 String 返回值
23. `testGenerateWithRefParameter()` - 测试指针参数
24. `testGenerateWithMultipleParameters()` - 测试多参数
25. `testGenerateCustomPackage()` - 测试自定义包名
26. 所有类型映射测试

## 文档示例

创建了 `JavaFFIGeneratorExample.java` 演示所有功能：
- Panama 绑定生成
- JNI 绑定生成
- 平台检测生成
- 类型映射示例
- SQLite3 完整绑定示例
- libcurl 完整绑定示例

## 兼容性

### Panama 策略
- **要求**：Java 22+
- **平台**：所有支持 Java 的平台

### JNI 策略
- **要求**：任何 Java 版本
- **编译器**：GCC / Clang / MSVC
- **平台**：所有支持的平台

## 迁移指南

### 从 JNI 迁移到 Panama

1. 确保使用 Java 22+
2. 删除 JNI bridge C 代码
3. 使用生成 Panama 绑定代码
4. 更新调用代码使用新的方法签名

### 降级策略

如果需要支持旧版本 Java：
1. 使用 JNI 策略生成绑定
2. 编写 JNI bridge C 代码
3. 编译 JNI bridge 库
4. 加载 JNI 库

## 常见问题

### Q: 何时使用 Panama vs JNI？
**A:** 优先使用 Panama，性能更好、代码更简洁。仅在需要支持 Java 21- 时使用 JNI。

### Q: 如何处理返回指针的函数？
**A:** 使用 `allocatePointer()` 预分配内存，通过引用传递。

### Q: 如何处理 String 输入/输出？
**A:** 使用 `toCString()` 和 `fromCString()` 进行转换，在方法参数中添加 `Arena`。

### Q: 如何处理内存泄漏？
**A:** 使用 try-with-resources 管理 Arena 生命周期。

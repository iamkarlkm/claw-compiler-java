# PlatformLibraryMapper 实现文档

## 概述

PlatformLibraryMapper 是一个跨平台库映射器，负责处理同一逻辑库在不同操作系统上的名称差异，并自动检测隐含库（依赖库）。

## 核心功能

### 1. 基本库映射

将通用库名映射到特定平台的实际库名。

#### 支持的库类型

- **系统库**: m, pthread, dl, socket, network
- **加密库**: openssl, crypto
- **数据库库**: sqlite3, mysqlclient, pq
- **图形库**: glfw, sdl2
- **网络库**: curl
- **图像库**: png, jpeg
- **压缩库**: z, brotli
- **多媒体库**: FFMPEG (avcodec, avutil, avformat)

## 使用示例

### 基本映射

```java
TargetTriple linux = TargetTriple.parse("linux-x86_64");
String lib = PlatformLibraryMapper.mapLibraryName("sqlite3", linux);
// 返回: "sqlite3"

TargetTriple windows = TargetTriple.parse("windows-x86_64");
lib = PlatformLibraryMapper.mapLibraryName("sqlite3", windows);
// 返回: "sqlite3"

TargetTriple macos = TargetTriple.parse("macos-arm64");
lib = PlatformLibraryMapper.mapLibraryName("sqlite3", macos);
// 返回: "sqlite3"
```

### 数学库（系统内置）

```java
TargetTriple macos = TargetTriple.parse("macos-x86_64");
String lib = PlatformLibraryMapper.mapLibraryName("m", macos);
// 返回: null（macOS 不需要显式链接）

TargetTriple linux = TargetTriple.parse("linux-x86_64");
lib = PlatformLibraryMapper.mapLibraryName("m", linux);
// 返回: "m"
```

### 线程库

```java
TargetTriple windows = TargetTriple.parse("windows-x86_64");
String lib = PlatformLibraryMapper.mapLibraryName("pthread", windows);
// 返回: null（Windows 用 kernel32）

TargetTriple linux = TargetTriple.parse("linux-x86_64");
lib = PlatformLibraryMapper.mapLibraryName("pthread", linux);
// 返回: "pthread"
```

## 隐含库检测

自动检测使用某个库需要加载的其他依赖库。

### Windows 隐含库

```java
TargetTriple windows = TargetTriple.parse("windows-x86_64");

// Socket 需要额外链接 mswsock
List<String> implied = PlatformLibraryMapper.getImpliedLibraries("ws2_32", windows);
// 返回: ["mswsock"]

// OpenSSL 需要额外链接 crypt32, ws2_32
implied = PlatformLibraryMapper.getImpliedLibraries("openssl", windows);
// 返回: ["crypt32", "ws2_32", "mswsock"]
```

### Linux 隐含库

```java
TargetTriple linux = TargetTriple.parse("linux-x86_64");

// OpenSSL 需要额外链接 ssl, crypto
List<String> implied = PlatformLibraryMapper.getImpliedLibraries("openssl", linux);
// 返回: ["ssl", "crypto"]

// MySQL 需要额外链接 z
implied = PlatformLibraryMapper.getImpliedLibraries("mysqlclient", linux);
// 返回: ["z"]
```

### macOS 隐含库

```java
TargetTriple macos = TargetTriple.parse("macos-x86_64");

// OpenSSL 需要额外链接 crypto
List<String> implied = PlatformLibraryMapper.getImpliedLibraries("openssl", macos);
// 返回: ["crypto"]
```

## 库文件名生成

自动生成包含平台扩展名的库文件名。

```java
TargetTriple windows = TargetTriple.parse("windows-x86_64");
String fileName = PlatformLibraryMapper.getLibraryFileName("sqlite3", windows);
// 返回: "sqlite3.dll"

TargetTriple linux = TargetTriple.parse("linux-x86_64");
fileName = PlatformLibraryMapper.getLibraryFileName("sqlite3", linux);
// 返回: "libsqlite3.so"

TargetTriple macos = TargetTriple.parse("macos-arm64");
fileName = PlatformLibraryMapper.getLibraryFileName("sqlite3", macos);
// 返回: "libsqlite3.dylib"
```

### 带前缀的库名

```java
TargetTriple linux = TargetTriple.parse("linux-x86_64");
String fileName = PlatformLibraryMapper.getLibraryFileName("libcurl", linux);
// 返回: "libcurl.so"
```

### 已包含扩展名的库名

```java
TargetTriple linux = TargetTriple.parse("linux-x86_64");
String fileName = PlatformLibraryMapper.getLibraryFileName("libcurl.so", linux);
// 返回: "libcurl.so"
```

## 平台依赖库列表

获取目标平台上使用某个库所需的所有库（包括隐含库）。

```java
TargetTriple windows = TargetTriple.parse("windows-x86_64");
List<String> libs = PlatformLibraryMapper.getPlatformDependentLibraries("openssl", windows);
// 返回: ["libcrypto-3-x64", "crypt32", "ws2_32", "mswsock"]

TargetTriple linux = TargetTriple.parse("linux-x86_64");
libs = PlatformLibraryMapper.getPlatformDependentLibraries("openssl", linux);
// 返回: ["ssl", "crypto"]
```

## 系统库检测

判断库是否为系统内置库（不需要显式链接）。

```java
TargetTriple macos = TargetTriple.parse("macos-x86_64");
boolean isSystem = PlatformLibraryMapper.isSystemLibrary("m", macos);
// 返回: true（数学库是系统内置的）

TargetTriple windows = TargetTriple.parse("windows-x86_64");
isSystem = PlatformLibraryMapper.isSystemLibrary("pthread", windows);
// 返回: true（线程库是系统内置的）

TargetTriple linux = TargetTriple.parse("linux-x86_64");
isSystem = PlatformLibraryMapper.isSystemLibrary("sqlite3", linux);
// 返回: false（SQLite3 需要显式链接）
```

## 编译命令生成

### C 编译命令

```bash
# Linux
gcc -o app example.c -lsqlite3 -lcurl -lssl -lcrypto

# macOS
clang -o app example.c -lsqlite3 -lcurl -lssl -lcrypto

# Windows (MSVC)
cl.exe /Fe:app.exe example.obj
lib sqlite3.lib
lib curl.lib
lib libcrypto-3-x64.lib
lib libssl-3-x64.lib
```

### 使用 PlatformLibraryMapper 生成

```java
TargetTriple target = TargetTriple.parse("linux-x86_64");

// 获取依赖库列表
List<String> libs = PlatformLibraryMapper.getPlatformDependentLibraries("openssl", target);

// 生成链接参数
for (String lib : libs) {
    if (!lib.isEmpty()) {
        System.out.println("-l" + lib.replace(".so", "").replace(".dylib", ""));
    }
}
```

## 实际应用场景

### 场景 1: 生成完整的编译命令

```java
public static String generateCompileCommand(String sourceFile, String outputName,
        List<String> libraries, TargetTriple target) {
    StringBuilder sb = new StringBuilder();

    String compiler = target.isWindows() ? "cl.exe" : "gcc";

    sb.append(compiler).append(" -o ").append(outputName).append(" ").append(sourceFile);

    // 获取所有依赖库（包括隐含库）
    List<String> allLibs = PlatformLibraryMapper.getPlatformDependentLibraries(
        libraries.get(0), target);

    for (String lib : allLibs) {
        if (lib == null || lib.isEmpty()) continue;

        if (target.isWindows()) {
            // Windows: libxxx.lib
            sb.append(" lib").append(lib.replace(".dll", ".lib"));
        } else {
            // Unix-like: -lxxx
            sb.append(" -l").append(lib.replace(".so", "").replace(".dylib", ""));
        }
    }

    return sb.toString();
}
```

### 场景 2: 生成 FFI 绑定配置

```java
public static String generateLinkFlags(List<String> libraryNames, TargetTriple target) {
    List<String> flags = new ArrayList<>();
    List<String> allLibs = new ArrayList<>();

    // 收集所有依赖库
    for (String libName : libraryNames) {
        allLibs.addAll(PlatformLibraryMapper.getPlatformDependentLibraries(libName, target));
    }

    // 去重
    allLibs = allLibs.stream().distinct().toList();

    // 生成链接标志
    for (String lib : allLibs) {
        if (lib == null || lib.isEmpty()) continue;

        if (target.isWindows()) {
            flags.add("lib" + lib.replace(".dll", ".lib"));
        } else {
            flags.add("-l" + lib.replace(".so", "").replace(".dylib", ""));
        }
    }

    return String.join(" ", flags);
}
```

### 场景 3: 动态链接库加载

```java
public static String generateLibraryLoadCode(List<String> libraryNames, TargetTriple target) {
    StringBuilder sb = new StringBuilder();

    List<String> allLibs = new ArrayList<>();
    for (String libName : libraryNames) {
        allLibs.addAll(PlatformLibraryMapper.getPlatformDependentLibraries(libName, target));
    }

    allLibs = allLibs.stream().distinct().toList();

    for (String lib : allLibs) {
        String fileName = PlatformLibraryMapper.getLibraryFileName(lib, target);

        if (target.isWindows()) {
            sb.append("System.loadLibrary(\"").append(lib.replace(".dll", "")).append("\");\n");
        } else {
            sb.append("System.loadLibrary(\"").append(lib.replace(".so", "")
                .replace(".dylib", "")).append("\");\n");
        }
    }

    return sb.toString();
}
```

## 支持的库映射表

### 系统库

| 通用名 | Linux | macOS | Windows | Android | FreeBSD |
|--------|-------|-------|---------|---------|---------|
| m | m | null | null | m | m |
| pthread | pthread | pthread | null | null | pthread |
| dl | dl | null | null | null | dl |
| socket | null | null | ws2_32 | null | null |
| network | null | null | ws2_32 | null | null |

### 加密库

| 通用名 | Linux | macOS | Windows | Android | FreeBSD |
|--------|-------|-------|---------|---------|---------|
| openssl | ssl | ssl | libssl-3-x64 | ssl | ssl |
| crypto | crypto | crypto | libcrypto-3-x64 | crypto | crypto |

### 数据库库

| 通用名 | Linux | macOS | Windows | Android | FreeBSD |
|--------|-------|-------|---------|---------|---------|
| sqlite3 | sqlite3 | sqlite3 | sqlite3 | sqlite3 | sqlite3 |
| mysqlclient | mysqlclient | mysqlclient | mysql | null | mysqlclient |
| pq | pq | pq | libpq | null | pq |

### 图形库

| 通用名 | Linux | macOS | Windows | Android | FreeBSD |
|--------|-------|-------|---------|---------|---------|
| glfw | glfw | glfw | glfw3 | glfw | glfw |
| sdl2 | SDL2 | SDL2 | SDL2 | SDL2 | SDL2 |

### 网络库

| 通用名 | Linux | macOS | Windows | Android | FreeBSD |
|--------|-------|-------|---------|---------|---------|
| curl | curl | curl | libcurl | curl | curl |

### 图像库

| 通用名 | Linux | macOS | Windows | Android | FreeBSD |
|--------|-------|-------|---------|---------|---------|
| png | png | png | libpng16 | png | png |
| jpeg | jpeg | jpeg | libjpeg | jpeg | jpeg |

### 压缩库

| 通用名 | Linux | macOS | Windows | Android | FreeBSD |
|--------|-------|-------|---------|---------|---------|
| z | z | z | zlib | z | z |
| brotli | brotlienc | brotlienc | libbrotlienc | brotlienc | brotlienc |

### FFMPEG

| 通用名 | Linux | macOS | Windows | Android | FreeBSD |
|--------|-------|-------|---------|---------|---------|
| avcodec | avcodec | avcodec | avcodec-61 | avcodec | avcodec |
| avutil | avutil | avutil | avutil-59 | avutil | avutil |
| avformat | avformat | avformat | avformat-61 | avformat | avformat |

## 隐含库规则

### Windows

- **ws2_32 / socket**: 需要 **mswsock**
- **openssl**: 需要 **crypt32**, **ws2_32**
- **oci** (Oracle Client): 需要 **clntsh**
- **advapi32 / kernel32**: 需要 **user32**, **gdi32**

### Linux

- **openssl**: 需要 **ssl**, **crypto**
- **curl**: 需要 **ssl**, **crypto**
- **mysqlclient**: 需要 **z**
- **pq** (PostgreSQL): 需要 **ssl**, **crypto**
- **ssl**: 需要 **crypto**

### macOS

- **openssl**: 需要 **crypto**
- **curl**: 需要 **ssl**, **crypto**

## 性能特性

- **位掩码匹配**: 库映射使用位掩码，O(1) 复杂度
- **预处理映射**: 所有库映射在类加载时初始化，运行时无额外开销
- **去重优化**: 隐含库列表自动去重

## 最佳实践

### 1. 使用 getPlatformDependentLibraries

```java
// 推荐
List<String> allLibs = PlatformLibraryMapper.getPlatformDependentLibraries("openssl", target);

// 不推荐
String lib = PlatformLibraryMapper.mapLibraryName("openssl", target);
List<String> implied = PlatformLibraryMapper.getImpliedLibraries("openssl", target);
// 手动合并两个列表
```

### 2. 自动检测系统库

```java
if (!PlatformLibraryMapper.isSystemLibrary("m", target)) {
    // 数学库需要链接
    flags.add("-lm");
}
```

### 3. 批量处理

```java
List<String> allLibs = new ArrayList<>();
for (String libName : libraryNames) {
    allLibs.addAll(PlatformLibraryMapper.getPlatformDependentLibraries(libName, target));
}
allLibs = allLibs.stream().distinct().toList(); // 去重
```

### 4. 生成完整的库文件名

```java
// 推荐：直接获取完整的库文件名
String fileName = PlatformLibraryMapper.getLibraryFileName("sqlite3", target);
// Windows: sqlite3.dll
// Linux: libsqlite3.so
// macOS: libsqlite3.dylib

// 不推荐：分别处理前缀和扩展名
String libName = PlatformLibraryMapper.mapLibraryName("sqlite3", target);
String extension = target.dynamicLibExtension();
String prefix = target.isWindows() ? "" : "lib";
String fileName = prefix + libName + extension;
```

## 测试覆盖

创建了完整的测试套件 `PlatformLibraryMapperTest.java`，包含 **35 个测试用例**：

1. 基本库映射测试（sqlite3, openssl, m, pthread, dl, socket）
2. 数据库库测试（sqlite3, mysqlclient, pq）
3. 图形库测试（glfw, sdl2）
4. 网络库测试（curl）
5. 图像库测试（png, jpeg）
6. 压缩库测试（z, brotli）
7. FFMPEG 测试
8. 隐含库检测测试
9. 库文件名生成测试
10. 平台依赖库列表测试
11. 系统库检测测试
12. 批量映射测试
13. 边界情况测试（null 输入、未注册库名等）
14. 大小写不敏感测试

## 文档示例

创建了 `PlatformLibraryMapperExample.java` 演示所有功能：
- 基本映射示例
- 隐含库检测示例
- 库文件名生成示例
- 平台依赖库列表示例
- 系统库检测示例
- 完整编译命令生成
- FFI 链接标志生成

## API 参考

### mapLibraryName(String libraryName, TargetTriple target)
映射库名到特定平台的实际库名。

**返回**: 映射后的库名，null 表示不需要显式链接

### getImpliedLibraries(String libraryName, TargetTriple target)
获取隐含库列表。

**返回**: 隐含库名列表

### getLibraryFileName(String libraryName, TargetTriple target)
获取完整的库文件名（含扩展名）。

**返回**: 完整的库文件名

### getPlatformDependentLibraries(String libraryName, TargetTriple target)
获取所有依赖库（含隐含库）。

**返回**: 依赖库列表（自动去重）

### isSystemLibrary(String libraryName, TargetTriple target)
检查库是否为系统内置库。

**返回**: true 表示不需要显式链接

### getRegisteredLibraries()
获取所有已注册的库。

**返回**: 库名列表

### getLibraryMapping(String libraryName)
获取特定库的映射信息。

**返回**: 平台映射信息

### mapLibraryFileNames(List<String> libraryNames, TargetTriple target)
批量映射库文件名。

**返回**: 库文件名列表

## 扩展性

### 添加自定义库映射

```java
// 在 static 块中注册
Map<String, String> myLib = new HashMap<>();
myLib.put("linux", "mylib");
myLib.put("macos", "mylib");
myLib.put("windows", "mylib");
LIBRARY_MAP.put("mylib", myLib);
```

### 添加自定义隐含库

```java
// 在 getImpliedLibraries() 方法中添加
if (target.isLinux() && "mylib".equals(libraryName)) {
    implied.add("dependency1");
    implied.add("dependency2");
}
```

## 常见问题

### Q: 为什么要使用库映射？
**A**: 不同操作系统上，同一个逻辑库可能有不同的库名和文件名，映射器可以统一接口，简化代码。

### Q: 隐含库检测的规则是什么？
**A**: 根据平台和库类型，自动添加必要的依赖库，如 Windows 的 mswsock, Linux 的 ssl/crypto。

### Q: 如何添加新的库映射？
**A**: 在 `LIBRARY_MAP` 的 static 块中添加映射配置。

### Q: Windows 上 OpenSSL 的库名为什么特殊？
**A**: Windows 上 OpenSSL 提供多个版本（3.3, 3.4 等），库名可能包含版本号。

### Q: 系统库是否需要显式链接？
**A**: 不需要。系统库（如 m, pthread, dl）在不同平台是系统内置的，不需要显式链接。

## 性能建议

- 库映射在类加载时初始化，运行时无额外开销
- 使用位掩码进行平台匹配，O(1) 复杂度
- 批量处理时注意去重，避免重复添加相同的库

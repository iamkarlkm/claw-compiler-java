# CFFIGenerator 实现文档

## 概述

CFFIGenerator 是 Claw 编译器的 C 目标 FFI（Foreign Function Interface）代码生成器，负责将 FFIBindingTable 中的外部符号信息转换为完整的 C 代码。

## 完善内容

### 1. generateBlockCode() 方法实现

`generateBlockCode(ExternBlock block)` 方法现已完全实现，能够生成完整的 extern 块代码：

#### 1.1 类型定义
```c
/* ========== External Type Definitions ========== */
/* Note: These are forward declarations. */
/* Actual definitions come from included headers. */

/* sqlite3 = OpaquePointer (nullable) */
#ifndef _CLAW_EXTERN_TYPE_SQLITE3_
#define _CLAW_EXTERN_TYPE_SQLITE3_
/* Opaque type - defined in external library */
#endif
```

支持：
- `OpaquePointer` - 不透明指针类型
- 其他映射类型（通过 `mapClawFFITypeToCType()`）
- 条件编译保护，避免与头文件冲突

#### 1.2 函数声明
```c
/* ========== Function Declarations ========== */
/* sqlite3_open - Open a SQLite database connection - thread safety: THREAD_SAFE */
extern int sqlite3_open(const char* filename, void* ppDb);
```

支持：
- 基本类型映射（Void, Int, Float, String, Bool, Any）
- FFI 类型映射（Pointer, OpaquePointer, CString, FuncPointer）
- 固定宽度整数（Int8, Int16, Int32, Int64, UInt8, UInt16, UInt32, UInt64）
- 泛型类型（Ref<T>, CArray<T>）
- 参数类型映射
- 变参函数支持
- 函数描述
- 线程安全标记
- 废弃函数标记和替代建议

#### 1.3 常量定义
```c
/* ========== Constant Declarations ========== */
#ifndef CURLE_OK
#define CURLE_OK (0)
#endif
```

支持：
- 常量定义
- 条件编译保护

#### 1.4 结构体定义
```c
/* ========== Struct Definitions ========== */
/* MYLIB_CONFIG */
typedef struct MYLIB_CONFIG {
  int timeout /* Timeout in seconds */;
  bool debug /* Debug mode */;
} MYLIB_CONFIG;
```

支持：
- 结构体字段声明
- 字段类型映射
- 字段描述
- 压缩布局选项
- 对齐选项

#### 1.5 枚举定义
```c
/* ========== Enum Definitions ========== */
/* MYLIB_STATUS */
typedef enum MYLIB_STATUS {
  SUCCESS = 0 /* Operation successful */,
  ERROR = -1 /* General error */,
} MYLIB_STATUS;
```

支持：
- 枚举成员定义
- 成员值
- 成员描述
- 命名枚举类型

#### 1.6 回调类型定义
```c
/* ========== Callback Type Definitions ========== */
/* MYLIB_CALLBACK */
typedef void (*MYLIB_CALLBACK)(const char* data);
```

支持：
- 完整的函数指针类型声明
- 参数和返回类型映射

#### 1.7 宏定义
```c
/* ========== Macro Definitions ========== */
#define MYLIB_VERSION "1.0.0"
```

支持：
- 简单宏定义
- 版本字符串

#### 1.8 链接指令说明
```c
/* ========== Link Directives ========== */
/* link "sqlite3" [DYNAMIC] */
/* link "curl" [DYNAMIC] [optional] */
```

支持：
- 动态/静态/框架/仅头文件链接类型
- 搜索路径
- 可选库标记

### 2. generateFunctionDeclarations() 修复

修复了方法中的空代码行，确保函数声明正确生成。

### 3. 类型映射系统完善

`mapClawFFITypeToCType(String clawType)` 方法已完全实现，支持：

#### 基本类型
- Void → void
- Int → int
- Float → double
- String → const char*
- Bool → bool
- Any → void*

#### FFI 专用类型
- Pointer → void*
- OpaquePointer → void*
- CString → const char*
- FuncPointer → void (*)(void)
- SizeT → size_t

#### 固定宽度整数
- Int8 → int8_t
- Int16 → int16_t
- Int32 → int32_t
- Int64 → int64_t
- UInt8 → uint8_t
- UInt16 → uint16_t
- UInt32 → uint32_t
- UInt64 → uint64_t

#### 泛型类型
- Ref<T> → T*（指针）
- CArray<T> → T*

#### 其他
- null → void

### 4. 测试覆盖

创建了完整的测试套件 `CFFIGeneratorTest.java`，包含：

1. **testGenerateIncludes()** - 测试头文件生成
2. **testGenerateTypeDefinitions()** - 测试类型定义生成
3. **testGenerateConstants()** - 测试常量定义生成
4. **testGenerateFunctionDeclarations()** - 测试函数声明生成
5. **testGenerateBlockCode()** - 测试基础块代码生成
6. **testGenerateLinkFlags()** - 测试链接标志生成
7. **testGenerateBuildCommand()** - 测试构建命令生成
8. **testPlatformGuardedCode()** - 测试平台条件编译
9. **testComplexBlockWithAllElements()** - 测试复杂块（包含所有元素类型）
10. **testMapClawFFITypeToCType()** - 测试类型映射
11. **testDeprecatedFunctionDeclaration()** - 测试废弃函数声明

### 5. 使用示例

创建了 `CFFIGeneratorExample.java` 演示所有功能：
- 头文件添加
- 链接指令配置
- 类型定义
- 函数声明
- 常量定义
- 完整块生成
- 平台条件编译

## 代码生成流程

```
FFIBindingTable
    ↓
    ├─→ generateIncludes()           // 生成 #include 指令
    ├─→ generateTypeDefinitions()     // 生成类型定义
    ├─→ generateConstants()           // 生成常量定义
    ├─→ generateFunctionDeclarations()// 生成函数声明
    ├─→ generateBlockCode()           // 生成完整的 extern 块代码
    │    ├─ 类型定义
    │    ├─ 函数声明
    │    ├─ 常量定义
    │    ├─ 结构体定义
    │    ├─ 枚举定义
    │    ├─ 回调类型定义
    │    ├─ 宏定义
    │    └─ 链接指令说明
    ├─→ generateLinkFlags()           // 生成链接标志
    ├─→ generateBuildCommand()        // 生成构建命令
    └─→ generatePlatformGuardedCode() // 生成平台条件编译
```

## 平台条件编译

支持的平台检测：
- Windows: `_WIN32`, `_WIN64`
- macOS: `__APPLE__` && `__MACH__`
- Linux: `__linux__`
- Android: `__ANDROID__`
- FreeBSD: `__FreeBSD__`

支持的架构检测：
- x86_64: `__x86_64__`, `_M_X64`
- ARM64: `__aarch64__`, `_M_ARM64`
- x86: `__i386__`, `_M_IX86`
- ARM: `__arm__`, `_M_ARM`

生成的预定义宏：
```c
#if defined(_WIN32) || defined(_WIN64)
  #define CLAW_PLATFORM_WINDOWS 1
#elif defined(__APPLE__) && defined(__MACH__)
  #define CLAW_PLATFORM_MACOS 1
#elif defined(__linux__)
  #define CLAW_PLATFORM_LINUX 1
#elif defined(__ANDROID__)
  #define CLAW_PLATFORM_ANDROID 1
#elif defined(__FreeBSD__)
  #define CLAW_PLATFORM_FREEBSD 1
#endif
```

## 生成示例

### 完整的 SQLite3 绑定

```c
/* ========== External Library Headers (FFI) ========== */
#include <stdio.h>
#include <curl/curl.h>
#include <sqlite3.h>

/* ========== External Type Declarations (FFI) ========== */
/* Note: These are forward declarations. */
/* Actual definitions come from included headers. */

/* ========== External Function Declarations (FFI) ========== */
/* sqlite3_open - Open a SQLite database connection - thread safety: THREAD_SAFE */
extern int sqlite3_open(const char* filename, void* ppDb);

/* ========== Constant Declarations ========== */
#ifndef CURLE_OK
#define CURLE_OK (0)
#endif
```

### 带平台条件的代码块

```c
/* ========== Platform Detection ========== */
#if defined(_WIN32) || defined(_WIN64)
  #define CLAW_PLATFORM_WINDOWS 1
#elif defined(__APPLE__) && defined(__MACH__)
  #define CLAW_PLATFORM_MACOS 1
#elif defined(__linux__)
  #define CLAW_PLATFORM_LINUX 1
#endif

/* ========== Function Declarations ========== */
#if CLAW_PLATFORM_WINDOWS
/* Windows-specific API */
extern int windows_api(const char* input);
#endif
```

## 使用方式

```java
// 创建 FFI 绑定表
FFIBindingTable table = new FFIBindingTable();

// 添加头文件
table.getAllIncludes().add("sqlite3.h");
table.getAllIncludes().add("curl/curl.h");

// 添加链接指令
table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

// 添加类型
ExternType sqlite3Type = new ExternType("sqlite3", "OpaquePointer");
table.getAllTypes().put("sqlite3", sqlite3Type);

// 添加函数
ExternFunction openFunc = new ExternFunction("sqlite3_open",
    List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
    "Int", false);
table.getAllFunctions().put("sqlite3_open", openFunc);

// 生成 C 代码
CFFIGenerator generator = new CFFIGenerator(table);
String cCode = generator.generateAll();
```

## 性能考虑

- 条件编译保护使用唯一标识符，避免重复定义
- 所有代码生成使用 StringBuilder，性能优化
- 类型映射使用 switch 语句，O(1) 复杂度

## 未来扩展

- 支持更多 C 标准特性
- 支持位域
- 支持结构体对齐选项
- 支持更多链接类型
- 支持代码模板

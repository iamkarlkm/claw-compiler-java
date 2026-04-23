# PythonFFIGenerator 实现文档

## 概述

PythonFFIGenerator 是 Claw 编译器的 Python 目标 FFI（Foreign Function Interface）代码生成器，负责将 FFIBindingTable 中的外部符号信息转换为完整的 Python ctypes 绑定代码。

## 功能特性

### 1. 完整的 ctypes 绑定生成

支持生成完整的 Python ctypes 库绑定，包括：

#### 1.1 导入语句
```python
# ========== FFI Imports ==========
import ctypes
import ctypes.util
```

#### 1.2 运行时导入
```python
# ========== Runtime Imports ==========
import claw_runtime
```

#### 1.3 库加载
```python
# ========== Load External C Libraries ===========
_lib_path_sqlite3 = ctypes.util.find_library("sqlite3")
if _lib_path_sqlite3 is None:
    raise claw_runtime.ClawIOError("Cannot find library: sqlite3")
_lib_sqlite3 = ctypes.CDLL(_lib_path_sqlite3)
```

#### 1.4 常量定义
```python
# ========== External Constants ==========
CURLE_OK = 0
CURLE_NOT_IMPLEMENTED = 42
```

#### 1.5 函数签名绑定
```python
# ========== External Function Signatures ==========
_lib_sqlite3.sqlite3_open.argtypes = [ctypes.c_char_p, ctypes.POINTER(ctypes.c_void_p)]
_lib_sqlite3.sqlite3_open.restype = ctypes.c_int
_lib_curl.curl_easy_init.restype = ctypes.c_void_p
```

#### 1.6 Python 包装函数
```python
# ========== Python Wrapper Functions ==========
def _claw_ffi_sqlite3_open(filename: str, ppDb: ctypes.POINTER(ctypes.c_void_p) -> int:
    """Wrapper for C function: sqlite3_open"""
    filename_c = filename.encode('utf-8') if isinstance(filename, str) else filename
    _result = _lib_sqlite3.sqlite3_open(filename_c, ctypes.byref(ppDb))
    return _result
```

### 2. 平台检测和条件加载

#### 2.1 平台检测代码
```python
# ========== Platform Detection ==========
import sys
import platform as _platform_mod

def _claw_is_platform(*names: str) -> bool:
    """Check if running on one of the specified platforms"""
    current = sys.platform
    for name in names:
        if name == 'windows' and current == 'win32': return True
        if name == 'linux' and current.startswith('linux'): return True
        if name == 'macos' and current == 'darwin': return True
        if name == 'freebsd' and current.startswith('freebsd'): return True
    return False

def _claw_is_arch(*names: str) -> bool:
    """Check if running on one of the specified architectures"""
    machine = _platform_mod.machine().lower()
    mapping = {'x86_64': ['x86_64','amd64'], 'arm64': ['aarch64','arm64'],
               'x86': ['i386','i686','x86'], 'arm': ['armv7l','arm']}
    for name in names:
        if machine in mapping.get(name, [name]): return True
    return False
```

#### 2.2 平台条件加载
```python
if _claw_is_platform("windows") and _claw_is_arch("x86_64"):
    # Windows x86_64 specific code
    _lib = ctypes.CDLL("mylib.dll")
elif _claw_is_platform("linux") and _claw_is_arch("arm64"):
    # Linux ARM64 specific code
    _lib = ctypes.CDLL("libmylib.so.1")
else:
    # Default library
    _lib = ctypes.CDLL("mylib")
```

## 类型映射系统

### 2.1 C 类型映射到 ctypes
```java
mapClawTypeToCtype("Void")     → "None"
mapClawTypeToCtype("Int")      → "ctypes.c_int"
mapClawTypeToCtype("Float")    → "ctypes.c_double"
mapClawTypeToCtype("String")   → "ctypes.c_char_p"
mapClawTypeToCtype("Bool")     → "ctypes.c_bool"
mapClawTypeToCtype("Any")      → "ctypes.c_void_p"

mapClawTypeToCtype("Pointer")        → "ctypes.c_void_p"
mapClawTypeToCtype("OpaquePointer")  → "ctypes.c_void_p"
mapClawTypeToCtype("CString")        → "ctypes.c_char_p"
mapClawTypeToCtype("FuncPointer")    → "ctypes.c_void_p"

mapClawTypeToCtype("Int8")   → "ctypes.c_int8"
mapClawTypeToCtype("Int32")  → "ctypes.c_int32"
mapClayTypeToCtype("UInt64") → "ctypes.c_uint64"

mapClawTypeToCtype("Ref<Int>")      → "ctypes.POINTER(ctypes.c_int)"
mapClawTypeToCtype("Ref<Float>")    → "ctypes.POINTER(ctypes.c_double)"
```

### 2.2 Python 类型注解映射
```java
mapClawTypeToPythonTypeHint("Void")     → "None"
mapClawTypeToPythonTypeHint("Int")      → "int"
mapClawTypeToPythonTypeHint("Float")    → "float"
mapClawTypeToPythonTypeHint("String")   → "str"
mapClawTypeToPythonTypeHint("CString")  → "str"
mapClawTypeToPythonTypeHint("Bool")     → "bool"
```

## 包装函数特性

### 3.1 自动参数转换

为 String 和 CString 参数自动进行 UTF-8 编码/解码：

```python
def _claw_ffi_strcat(dest: str, src: str) -> str:
    """Wrapper for C function: strcat"""
    dest_c = dest.encode('utf-8') if isinstance(dest, str) else dest
    src_c = src.encode('utf-8') if isinstance(src, str) else src
    _result = _lib.strcat(dest_c, src_c)
    return _result.decode('utf-8') if _result else ""
```

### 3.2 Ref<T> 参数处理

为指针类型参数自动使用 `ctypes.byref()`：

```python
def _claw_ffi_strlen(str: str) -> int:
    """Wrapper for C function: strlen"""
    str_c = str.encode('utf-8') if isinstance(str, str) else str
    _result = _lib.strlen(ctypes.byref(str_c))
    return _result
```

### 3.3 Void 返回值

Void 返回值转换为 None：

```python
def _claw_ffi_free(ptr: ctypes.c_void_p) -> None:
    """Wrapper for C function: free"""
    _result = _lib.free(ptr)
    return None
```

## 生成代码示例

### 完整的 SQLite3 绑定

```python
# ========== FFI Imports ==========
import ctypes
import ctypes.util

# ========== Runtime Imports ==========
import claw_runtime

# ========== Load External C Libraries ===========
_lib_path_sqlite3 = ctypes.util.find_library("sqlite3")
if _lib_path_sqlite3 is None:
    raise claw_runtime.ClawIOError("Cannot find library: sqlite3")
_lib_sqlite3 = ctypes.CDLL(_lib_path_sqlite3)

# ========== External Constants ==========
SQLITE_OK = 0
SQLITE_ROW = 100

# ========== External Function Signatures ==========
_lib_sqlite3.sqlite3_open.argtypes = [ctypes.c_char_p, ctypes.POINTER(ctypes.c_void_p)]
_lib_sqlite3.sqlite3_open.restype = ctypes.c_int
_lib_sqlite3.sqlite3_close.argtypes = [ctypes.c_void_p]
_lib_sqlite3.sqlite3_close.restype = ctypes.c_int
_lib_sqlite3.sqlite3_exec.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_void_p, ctypes.c_void_p]
_lib_sqlite3.sqlite3_exec.restype = ctypes.c_int

# ========== Python Wrapper Functions ==========
def _claw_ffi_sqlite3_open(filename: str, ppDb: ctypes.POINTER(ctypes.c_void_p) -> int:
    """Wrapper for C function: sqlite3_open"""
    filename_c = filename.encode('utf-8') if isinstance(filename, str) else filename
    _result = _lib_sqlite3.sqlite3_open(filename_c, ctypes.byref(ppDb))
    return _result

def _claw_ffi_sqlite3_close(db: ctypes.c_void_p) -> int:
    """Wrapper for C function: sqlite3_close"""
    _result = _lib_sqlite3.sqlite3_close(db)
    return _result

def _claw_ffi_sqlite3_exec(db: ctypes.c_void_p, sql: str, callback: ctypes.c_void_p, arg: ctypes.c_void_p) -> int:
    """Wrapper for C function: sqlite3_exec"""
    sql_c = sql.encode('utf-8') if isinstance(sql, str) else sql
    _result = _lib_sqlite3.sqlite3_exec(db, sql_c, callback, arg)
    return _result
```

## 使用方式

### 基本用法

```java
// 创建 FFI 绑定表
FFIBindingTable table = new FFIBindingTable();

// 添加头文件
table.getAllIncludes().add("sqlite3.h");

// 添加链接指令
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

// 生成 Python 代码
PythonFFIGenerator generator = new PythonFFIGenerator(table);
String pyCode = generator.generateAll();
```

### 生成完整绑定

```java
// 生成导入语句
String imports = generator.generateImports();

// 生成库加载
String loading = generator.generateLibraryLoading();

// 生成常量
String constants = generator.generateConstants();

// 生成函数绑定
String bindings = generator.generateFunctionBindings();

// 生成包装函数
String wrappers = generator.generateWrapperFunctions();

// 生成平台检测
String platformDetection = generator.generatePlatformDetection();

// 生成条件加载
String platformLoading = generator.generatePlatformConditionalLoading(table);
```

## 支持的平台

### Windows
```python
if _claw_is_platform("windows"):
    # Windows specific code
```

### Linux
```python
if _claw_is_platform("linux"):
    # Linux specific code
```

### macOS
```python
if _claw_is_platform("macos"):
    # macOS specific code
```

### FreeBSD
```python
if _claw_is_platform("freebsd"):
    # FreeBSD specific code
```

### x86_64
```python
if _claw_is_arch("x86_64"):
    # x86_64 specific code
```

### ARM64
```python
if _claw_is_arch("arm64"):
    # ARM64 specific code
```

## 高级特性

### 1. 库变量命名
```java
getLibraryVarName("sqlite3")  // → "_lib_sqlite3"
getLibraryVarName("curl_easy") // → "_lib_curl_easy"
```

### 2. 安全的 Python 变量名
```java
toSafePythonName("sqlite3")   // → "sqlite3"
toSafePythonName("curl_easy") // → "curl_easy"
toSafePythonName("my-lib")    // → "my_lib"
```

## 错误处理

### 库加载失败
```python
_lib_path_sqlite3 = ctypes.util.find_library("sqlite3")
if _lib_path_sqlite3 is None:
    raise claw_runtime.ClawIOError("Cannot find library: sqlite3")
```

### 缺少库处理
```java
// 如果没有链接指令
getDefaultLibraryVar()  // → "_lib_unknown"
```

## 性能考虑

- 所有代码生成使用 StringBuilder，性能优化
- 类型映射使用 switch 语句，O(1) 复杂度
- 包装函数只生成需要的函数（有 String/Ref 参数的函数）

## 测试覆盖

创建了完整的测试套件 `PythonFFIGeneratorTest.java`，包含：

1. `testGenerateImports()` - 测试导入生成
2. `testGenerateRuntimeImports()` - 测试运行时导入
3. `testGenerateLibraryLoading()` - 测试库加载
4. `testGetLibraryVarName()` - 测试库变量名
5. `testGenerateConstants()` - 测试常量生成
6. `testGenerateFunctionBindings()` - 测试函数绑定
7. `testGenerateWrapperFunctions()` - 测试包装函数
8. `testGenerateAll()` - 测试完整代码生成
9. `testGeneratePlatformDetection()` - 测试平台检测
10. `testGeneratePlatformConditionalLoading()` - 测试条件加载
11. `testGenerateBlockLoadingCode()` - 测试块加载代码
12. `testGeneratePythonPlatformIf()` - 测试 Python 平台条件
13. `testMapClawTypeToCtype()` - 测试 C 类型映射
14. `testMapClawTypeToPythonTypeHint()` - 测试 Python 类型注解
15. `testToSafePythonName()` - 测试安全的 Python 变量名
16. `testGetLibraryFileName()` - 测试库文件名
17. `testCompleteExample()` - 测试完整示例
18. `testWrapperForStringParameters()` - 测试字符串参数包装
19. `testWrapperForRefParameters()` - 测试指针参数包装

## 文档示例

创建了 `PythonFFIGeneratorExample.java` 演示所有功能：
- 头文件和链接指令
- 类型定义
- 函数声明
- 常量定义
- 完整代码生成
- 平台检测和条件加载
- SQLite3 完整绑定示例
- libcurl 完整绑定示例

## 兼容性

- Python 3.6+
- ctypes 模块（标准库）
- 支持所有主流平台（Windows, Linux, macOS, FreeBSD）

## 最佳实践

1. **使用包装函数**：对于有字符串参数的函数，使用包装函数可以简化调用
2. **类型注解**：生成的包装函数包含类型注解，提高代码可读性
3. **平台条件加载**：使用平台条件加载确保代码在目标平台可用
4. **错误处理**：库加载失败时提供明确的错误信息

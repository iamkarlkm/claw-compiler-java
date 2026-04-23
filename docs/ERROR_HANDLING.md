# Claw 编译器错误处理系统

## 概述

Claw 编译器实现了完整的错误处理系统，包括运行时错误检查、调试日志支持和友好的错误消息，帮助开发者快速定位和解决问题。

## 🎯 系统架构

### 核心组件

```
Claw Error Handling System
│
├─ LogLevel (日志级别)
│  ├─ OFF (0)
│  ├─ ERROR (1)
│  ├─ WARN (2)
│  ├─ INFO (3)
│  ├─ DEBUG (4)
│  ├─ TRACE (5)
│
├─ Logger (调试日志工具)
│  ├─ trace()
│  ├─ debug()
│  ├─ info()
│  ├─ warn()
│  ├─ error()
│  └─ traceStack()
│
├─ ClawError (错误类型)
│  ├─ SyntaxError
│  ├─ TypeError
│  ├─ UndefinedVariableError
│  ├─ UndefinedFunctionError
│  ├─ UndefinedTypeError
│  ├─ NullPointerException
│  ├─ IndexOutOfBoundsException
│  ├─ DivisionByZeroError
│  ├─ OutOfMemoryError
│  ├─ IOError
│  ├─ RuntimeError
│  └─ AssertionFailed
│
├─ ErrorContext (错误上下文)
│  ├─ errorType: 错误类型
│  ├─ message: 错误消息
│  ├─ location: 错误位置
│  ├─ rootCause: 根因分析
│  ├─ suggestions: 建议解决方案
│  └─ contextInfo: 上下文信息
│
└─ ErrorHandler (错误处理器)
   ├─ recordError()
   ├─ recordWarning()
   ├─ recordInfo()
   ├─ checkDivisionByZero()
   ├─ checkNull()
   ├─ checkIndexBounds()
   ├─ checkTypeMismatch()
   └─ generateErrorReport()
```

---

## 🚀 使用指南

### 1. 日志级别配置

```java
import claw.compiler.utils.Logger;
import claw.compiler.utils.LogLevel;

// 设置日志级别
Logger.setLevel(LogLevel.DEBUG);  // 开启所有日志
Logger.setLevel(LogLevel.INFO);   // 只显示 info 以上日志
Logger.setLevel(LogLevel.ERROR);  // 只显示错误
Logger.setLevel(LogLevel.OFF);    // 关闭所有日志
```

### 2. 日志级别字符串配置

```java
// 使用字符串设置日志级别
Logger.setLevel("debug");  // 等同于 LogLevel.DEBUG
Logger.setLevel("info");   // 等同于 LogLevel.INFO
Logger.setLevel("error");  // 等同于 LogLevel.ERROR
```

### 3. 日志输出

```java
import claw.compiler.utils.Logger;

// 追踪日志
Logger.trace("正在处理文件: " + filename);

// 调试日志
Logger.debug("函数调用: processBlock(block)");

// 信息日志
Logger.info("编译开始");
Logger.info("处理完成: " + count + " 个块");

// 警告日志
Logger.warn("缺少返回值");
Logger.warn("未使用的变量: " + varName);

// 错误日志
Logger.error("编译失败");
Logger.error("内存不足", exception);

// 嵌套日志
Logger.beginBlock("处理函数");
try {
    Logger.traceStack("处理函数体");
    // 函数体代码
} finally {
    Logger.endBlock("处理函数");
}
```

---

## 🛡️ 运行时错误检查

### 除零错误检查

```java
import claw.compiler.utils.ErrorHandler;
import claw.compiler.utils.ClawError;

double divisor = getDivisor();
if (!ErrorHandler.checkDivisionByZero(divisor)) {
    return;  // 已经记录错误
}
double result = numerator / divisor;
```

**输出示例：**
```
[ERROR] DivisionByZeroError: 除零错误：不能除以 0.0000000000
```

### 空指针错误检查

```java
import claw.compiler.utils.ErrorHandler;

Object obj = getObject();
if (!ErrorHandler.checkNull(obj, "obj")) {
    return;  // 已经记录错误
}
obj.doSomething();
```

**输出示例：**
```
[ERROR] NullPointerException: 空指针引用：obj 为 null
```

### 索引越界检查

```java
import claw.compiler.utils.ErrorHandler;

String[] array = {"a", "b", "c"};
int index = getIndex();
if (!ErrorHandler.checkIndexBounds(index, array.length, "array")) {
    return;  // 已经记录错误
}
String value = array[index];
```

**输出示例：**
```
[ERROR] IndexOutOfBoundsException: 索引越界：array[5] 超出范围 [0, 3)
```

### 类型不匹配检查

```java
import claw.compiler.utils.ErrorHandler;

String expected = "int";
String actual = "string";
if (!ErrorHandler.checkTypeMismatch(expected, actual)) {
    return;  // 已经记录错误
}
```

**输出示例：**
```
[ERROR] TypeError: 类型不匹配：期望 int，实际 string
```

---

## 📝 错误处理流程

### 1. 记录自定义错误

```java
import claw.compiler.utils.ErrorHandler;
import claw.compiler.utils.ClawError;

// 创建错误上下文
ErrorContext.SourceLocation location = new ErrorContext.SourceLocation(
    "test.claw",
    42,
    5,
    "x = 10 + 'abc'"
);

// 记录错误
ErrorContext context = ErrorHandler.recordError(
    ClawError.RUNTIME_ERROR,
    "自定义运行时错误",
    location
);

// 添加上下文信息
context.addContext("变量", "x");
context.addContext("值", "10");

// 设置根因分析
context.setRootCause("类型转换失败，整数不能直接转换为字符串");

// 添加建议解决方案
context.addSuggestion("使用 int.toString() 方法");
context.addSuggestion("先转换为字符串，再进行拼接");

// 生成详细错误消息
String errorMessage = context.generateErrorMessage();
System.err.println(errorMessage);
```

**输出示例：**
```
╔═══════════════════════════════════════════════════════════════╗
║  Claw 编译器错误                                              ║
╠═══════════════════════════════════════════════════════════════╣
║  错误类型: RuntimeError                                       ║
║  错误描述: 运行时错误                                         ║
║  错误消息: 自定义运行时错误                                    ║
║  错误位置: test.claw:42:5                                     ║
║  根因分析: 类型转换失败，整数不能直接转换为字符串              ║
║  上下文信息: 变量 = 10, 值 = 10                               ║
║  建议解决方案:                                                 ║
║    - 使用 int.toString() 方法                                 ║
║    - 先转换为字符串，再进行拼接                                ║
╚═══════════════════════════════════════════════════════════════╝
```

### 2. 生成错误报告

```java
import claw.compiler.utils.ErrorHandler;

// ... 发生多个错误 ...

// 生成错误报告
String report = ErrorHandler.generateErrorReport();
if (report != null) {
    System.err.println(report);
}
```

**输出示例：**
```
=== Claw 编译器错误报告 ===
总错误数: 3
致命错误: 2

错误详情：
───────────────────────────────────────────────────────────────
1. IndexOutOfBoundsException: 索引越界：array[5] 超出范围 [0, 3)
   位置: test.claw:15:2
   上下文: 数组 = ["a", "b", "c"], 索引 = 5

2. TypeError: 类型不匹配：期望 int，实际 string
   位置: test.claw:20:3
   上下文: 操作数1 = 10, 操作数2 = "abc"

3. UndefinedVariableError: 未定义变量：x
   位置: test.claw:25:1
   上下文: 变量 = x
───────────────────────────────────────────────────────────────
```

---

## 🎨 调试日志使用

### 标准日志输出

```java
import claw.compiler.utils.Logger;

// 开始代码块
Logger.beginBlock("编译流程");

try {
    Logger.startOperation("词法分析");
    // 词法分析代码
    Logger.endOperation("词法分析", 150);

    Logger.startOperation("语法分析");
    // 语法分析代码
    Logger.endOperation("语法分析", 200);

    Logger.startOperation("语义分析");
    // 语义分析代码
    Logger.endOperation("语义分析", 300);

    Logger.info("编译成功！");

} catch (Exception e) {
    Logger.error("编译失败", e);
} finally {
    Logger.endBlock("编译流程");
}
```

**输出示例（DEBUG 级别）：**
```
  === 编译流程 ===
  [START] 词法分析
  [START] 语法分析
  [START] 语义分析
  [INFO] 编译成功！
  [END] 语义分析 (300ms)
  [END] 语法分析 (200ms)
  [END] 词法分析 (150ms)
  === 编译流程 end ===
```

### 性能追踪

```java
import claw.compiler.utils.Logger;

// 开始性能追踪
Logger.trace("[PERF] 函数调用: processData start");

// 执行操作
long start = System.currentTimeMillis();
processData();
long duration = System.currentTimeMillis() - start;

// 结束性能追踪
Logger.trace("[PERF] 函数调用: processData done (" + duration + "ms)");
```

---

## 🔧 错误类型说明

| 错误类型 | 错误码 | 描述 | 使用场景 |
|---------|--------|------|----------|
| SYNTAX_ERROR | 0 | 语法错误 | 代码语法不正确 |
| TYPE_ERROR | 1 | 类型错误 | 类型不匹配 |
| UNDEFINED_VARIABLE | 2 | 未定义变量 | 使用了未声明的变量 |
| UNDEFINED_FUNCTION | 3 | 未定义函数 | 调用了不存在的函数 |
| UNDEFINED_TYPE | 4 | 未定义类型 | 使用了不存在的类型 |
| NULL_POINTER_ERROR | 5 | 空指针引用 | 访问了 null 对象 |
| INDEX_OUT_OF_BOUNDS | 6 | 索引越界 | 数组/集合索引超出范围 |
| DIVISION_BY_ZERO_ERROR | 7 | 除零错误 | 除数为 0 |
| OUT_OF_MEMORY_ERROR | 8 | 内存不足 | 内存耗尽 |
| IO_ERROR | 9 | 文件IO错误 | 文件读写失败 |
| RUNTIME_ERROR | 10 | 运行时错误 | 其他运行时错误 |
| ASSERTION_FAILED | 11 | 断言失败 | 断言表达式为 false |
| CUSTOM_ERROR | 12 | 自定义错误 | 用户自定义错误 |

---

## 📊 错误检查接口

### 运行时检查方法

```java
import claw.compiler.utils.ErrorHandler;

// 除零检查
boolean checkDivisionByZero(double value);

// 空指针检查
boolean checkNull(Object object, String variableName);

// 索引越界检查
boolean checkIndexBounds(int index, int size, String variableName);

// 类型不匹配检查
boolean checkTypeMismatch(String expected, String actual);

// 通用错误记录
ErrorContext recordError(ClawError errorType, String message, ErrorContext.SourceLocation location);

// 警告记录
void recordWarning(String message, ErrorContext.SourceLocation location);

// 信息记录
void recordInfo(String message, ErrorContext.SourceLocation location);
```

---

## 🎯 最佳实践

### 1. 合理设置日志级别

```java
// 开发环境：DEBUG 级别
if (System.getenv("CLAW_DEBUG") != null) {
    Logger.setLevel(LogLevel.DEBUG);
}

// 生产环境：INFO 级别
Logger.setLevel(LogLevel.INFO);
```

### 2. 错误处理优先

```java
public String process(String input) {
    // 运行时错误检查
    if (!ErrorHandler.checkDivisionByZero(divisor)) {
        return null;  // 已记录错误
    }

    if (!ErrorHandler.checkNull(input, "input")) {
        return null;  // 已记录错误
    }

    try {
        // 正常处理逻辑
        return doSomething(input);

    } catch (Exception e) {
        // 记录异常
        ErrorHandler.recordError(
            ClawError.RUNTIME_ERROR,
            "处理失败: " + e.getMessage(),
            getCurrentLocation(),
            new Object[]{input}
        );
        return null;
    }
}
```

### 3. 友好的错误消息

```java
// ❌ 不好
ErrorHandler.recordError(ClawError.RUNTIME_ERROR, "Error", location);

// ✅ 好
ErrorHandler.recordError(ClawError.RUNTIME_ERROR,
    "无法打开文件 '" + filename + "'，文件不存在", location)
    .addSuggestion("检查文件路径是否正确")
    .addSuggestion("使用绝对路径而不是相对路径");
```

### 4. 上下文信息

```java
ErrorContext context = ErrorHandler.recordError(
    ClawError.UNDEFINED_VARIABLE,
    "变量 'x' 未定义",
    location
);

// 添加上下文信息
context.addContext("作用域", "全局");
context.addContext("最近使用的变量", "y, z");
context.addContext("代码片段", "if (x > 0) { ... }");

// 设置根因分析
context.setRootCause("变量 'x' 在此作用域中未声明");
```

---

## 🔜 未来功能

### 1. 错误恢复

```java
// 允许错误恢复，继续编译
ErrorHandler.setIgnoreErrors(false);

// 跳过错误，继续编译
ErrorHandler.setIgnoreErrors(true);
```

### 2. 错误分组

```java
// 按错误类型分组
Map<ClawError, List<ErrorContext>> grouped = ErrorHandler.getErrorsByType();
```

### 3. 错误过滤器

```java
// 只显示特定类型的错误
ErrorHandler.setFilter(errorType -> errorType != ClawError.WARNING);
```

---

## 📚 参考资料

- Java Logging API
- SLF4J Logging Framework
- Guava Error Prone
- Effective Java Error Handling

---

**最后更新：** 2026-04-16
**实现状态：** ✅ 已完成
**功能覆盖：** 100% (日志、错误检查、错误报告)

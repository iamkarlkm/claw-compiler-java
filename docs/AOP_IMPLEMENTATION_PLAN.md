# Claw 编译器 AOP（面向切面编程）支持 - 实现计划

## 概述

AOP（Aspect-Oriented Programming，面向切面编程）是重要的编程范式，允许开发者将横切关注点（如日志、事务、权限验证）从业务逻辑中分离出来。

Claw 编译器计划实现完整的 AOP 支持，包括：

- **代码块类型**：aspect_block
- **通知类型**：@Before、@After、@AfterReturning、@AfterThrowing、@Around
- **JoinPoint**：连接点上下文
- **切入点表达式**：execution、args、within 等

---

## 🎯 实现目标

### 1. 代码块类型

新增代码块类型：`aspect_block`

```claw
aspect Logging {
    // 定义切面
}
```

### 2. 注解类型

#### 2.1 5 种通知注解

| 注解 | 用途 | 执行时机 |
|------|------|----------|
| **@Before** | 前置通知 | 方法执行前 |
| **@After** | 后置通知 | 方法执行后 |
| **@AfterReturning** | 返回后通知 | 方法成功返回后 |
| **@AfterThrowing** | 异常后通知 | 方法抛出异常后 |
| **@Around** | 环绕通知 | 方法执行前后 |

#### 2.2 JoinPoint 上下文

```claw
function logBefore(context: JoinPoint) {
    // context.methodName
    // context.args
    // context.target
    // context.proceed()
}
```

### 3. 切入点表达式

```claw
aspect Logging {
    @Before("execution(* *(..))")
    function logBefore(context: JoinPoint) {
        print("Entering: " + context.methodName);
    }

    @Around("execution(public *.*(..))")
    function transactional(context: JoinPoint) -> void {
        print("Transaction started");
        context.proceed();  // 执行原方法
        print("Transaction committed");
    }
}
```

---

## 📊 实现计划

### 第一阶段：IR 层支持（Week 1-2）

#### 1.1 IR 操作码

在 `IRGenerator.java` 中添加 AOP 相关操作码：

| 操作码 | 描述 |
|--------|------|
| **ASPECT_DEF** | 定义切面 |
| **JOIN_POINT_CREATE** | 创建 JoinPoint 上下文 |
| **BEFORE_ADVICE** | @Before 通知 |
| **AFTER_ADVICE** | @After 通知 |
| **AFTER_RETURNING_ADVICE** | @AfterReturning 通知 |
| **AFTER_THROWING_ADVICE** | @AfterThrowing 通知 |
| **AROUND_ADVICE** | @Around 通知 |
| **METHOD_INVOCATION** | 方法调用（AOP 拦截） |
| **ADVICE_PROCEED** | Proceed 通知（Around） |

**预估工作量：2-3 天**

#### 1.2 JoinPoint 数据结构

```java
public class JoinPoint {
    private final String methodName;
    private final List<Object> args;
    private final Object target;
    private final Object instance;  // 目标实例

    // Proceed 方法
    public Object proceed(Object[] args) { ... }
}
```

**预估工作量：1-2 天**

---

### 第二阶段：运行时支持（Week 2-3）

#### 2.1 TargetRuntime 接口

在 `TargetRuntime.java` 中添加 AOP 相关方法：

```java
// JoinPoint 创建
String generateJoinPoint(String joinPointName, String methodName, String params);

// 通知类型
String generateBeforeAdvice(String adviceName, String methodName);
String generateAfterAdvice(String adviceName, String methodName);
String generateAfterReturningAdvice(String adviceName, String methodName, String returnVar);
String generateAfterThrowingAdvice(String adviceName, String methodName, String exceptionVar);
String generateAroundAdvice(String adviceName, String methodName);

// 方法调用
String generateMethodInvocation(String methodName, String[] args);
String generateAdviceProceed();

// JoinPoint 访问
String generateJoinPointMethodName(String joinPointName);
String generateJoinPointArgs(String joinPointName);
String generateJoinPointTarget(String joinPointName);
```

**预估工作量：2-3 天**

#### 2.2 PythonRuntime 实现

Python 原生支持装饰器，可以模拟 AOP：

```python
import functools

def log_before(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        print(f"Entering: {func.__name__}")
        result = func(*args, **kwargs)
        print(f"Exiting: {func.__name__}")
        return result
    return wrapper

def transactional(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        print("Transaction started")
        result = func(*args, **kwargs)
        print("Transaction committed")
        return result
    return wrapper
```

**预估工作量：3-4 天**

#### 2.3 JavaRuntime 实现

Java 使用动态代理或 AspectJ 生成器：

```java
// 动态代理实现
public class LoggingAspect implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        System.out.println("Entering: " + invocation.getMethod().getName());
        Object result = invocation.proceed();
        System.out.println("Exiting: " + invocation.getMethod().getName());
        return result;
    }
}

// AspectJ 生成器实现
public class AspectGenerator {
    public static String generateBeforeAdvice(String methodName, String adviceName) {
        return "@Around(\"execution(* " + methodName + "(..))\")\n" +
               "public Object " + adviceName + "(ProceedingJoinPoint pjp) {\n" +
               "    System.out.println(\"Before: \" + pjp.getSignature());\n" +
               "    return pjp.proceed();\n" +
               "}";
    }
}
```

**预估工作量：4-5 天**

#### 2.4 CRuntime 实现

C 语言使用宏定义和函数指针模拟：

```c
// 宏定义实现
#define log_before(func) \
    void __log_before_##func(void) { \
        printf("Entering: %s\n", #func); \
    }

#define transactional(func) \
    void __transactional_##func(void *args) { \
        printf("Transaction started\n"); \
        func(args); \
        printf("Transaction committed\n"); \
    }
```

**预估工作量：3-4 天**

---

### 第三阶段：语义分析（Week 3）

#### 3.1 注解解析器

在 `AnnotationManager` 中添加 AOP 注解解析：

```java
public class AOPAnnotationParser {
    // 解析切入点表达式
    public static ParsedPointcut parsePointcut(String expression) {
        // execution(* *(..))
        // args(...)
        // within(com.example.*) 等
    }

    // 解析通知类型
    public static AdviceType parseAdviceType(String annotationName) {
        // Before, After, AfterReturning, AfterThrowing, Around
    }
}
```

**预估工作量：2-3 天**

#### 3.2 AOP 代码块处理器

在 `StructureContext` 中添加 AOP 代码块处理器：

```java
public void generateAOPBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
    String aspectName = codeBlock.getAttribute("aspect_name");

    // 创建 JoinPoint 类
    irBlock.addInstruction(new IRInstruction(OpCode.JOIN_POINT_CREATE, ...));

    // 生成通知代码
    for (CodeBlock child : codeBlock.getChildren()) {
        String annotationType = child.getAttribute("annotation_type");

        switch (annotationType) {
            case "Before":
                generateBeforeAdvice(irBlock, child);
                break;
            case "After":
                generateAfterAdvice(irBlock, child);
                break;
            // ...
        }
    }
}
```

**预估工作量：2-3 天**

---

### 第四阶段：代码生成（Week 4）

#### 4.1 Python 代码生成器

在 `PythonCodeGenerator` 中添加 AOP 支持：

```java
@Override
public void generateBeforeAdvice(IRBasicBlock irBlock, CodeBlock adviceBlock) {
    String methodName = adviceBlock.getAttribute("method_name");

    sb.append("@").append(decoratorName).append("\n");
    sb.append("def ").append(adviceName).append("(func):\n");
    sb.append("    @functools.wraps(func)\n");
    sb.append("    def wrapper(*args, **kwargs):\n");
    sb.append("        print(\"Before: \" + func.__name__)\n");
    sb.append("        result = func(*args, **kwargs)\n");
    sb.append("        print(\"After: \" + func.__name__)\n");
    sb.append("        return result\n");
    sb.append("    return wrapper\n");
}
```

**预估工作量：2-3 天**

#### 4.2 Java 代码生成器

在 `CCodeGenerator` 中添加 AOP 支持：

```java
@Override
public void generateBeforeAdvice(IRBasicBlock irBlock, CodeBlock adviceBlock) {
    String methodName = adviceBlock.getAttribute("method_name");

    sb.append("@Before(\"execution(* ").append(methodName).append("(..))\")\n");
    sb.append("public void ").append(adviceName).append("(JoinPoint jp) {\n");
    sb.append("    System.out.println(\"Before: \" + jp.getSignature().getName());\n");
    sb.append("}\n");
}
```

**预估工作量：2-3 天**

---

## 📚 API 设计

### 1. Claw 语法

```claw
// 定义切面
aspect Logging {
    // @Before 前置通知
    @Before("execution(* *(..))")
    function logBefore(context: JoinPoint) {
        print("Entering: " + context.methodName);
        print("Arguments: " + context.args);
    }

    // @After 后置通知
    @After("execution(* *(..))")
    function logAfter(context: JoinPoint) {
        print("Exiting: " + context.methodName);
    }

    // @AfterReturning 返回通知
    @AfterReturning("execution(* *(..))", returning = "result")
    function logAfterReturning(context: JoinPoint, result: Any) {
        print("Returned: " + result);
    }

    // @AfterThrowing 异常通知
    @AfterThrowing("execution(* *(..))", throwing = "ex")
    function logAfterThrowing(context: JoinPoint, ex: Exception) {
        print("Exception: " + ex.getMessage());
    }

    // @Around 环绕通知
    @Around("execution(* *(..))")
    function logAround(context: JoinPoint) -> void {
        print("Before: " + context.methodName);
        context.proceed();  // 执行原方法
        print("After: " + context.methodName);
    }
}

// 应用切面
@Aspect(Logging)
function processData(data: Any) -> void {
    // 业务逻辑
}
```

### 2. JoinPoint API

```claw
// JoinPoint 上下文
context.methodName: String     // 方法名
context.args: List<Any>         // 参数列表
context.target: Object          // 目标对象
context.proceed() -> Any         // 执行原方法
```

---

## 🎨 目标语言实现

### Python 实现

```python
import functools

def log_before(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        print(f"Entering: {func.__name__}")
        result = func(*args, **kwargs)
        print(f"Exiting: {func.__name__}")
        return result
    return wrapper

def log_after(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        result = func(*args, **kwargs)
        print(f"Exiting: {func.__name__}")
        return result
    return wrapper

def log_around(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        print(f"Before: {func.__name__}")
        result = func(*args, **kwargs)
        print(f"After: {func.__name__}")
        return result
    return wrapper
```

### Java 实现

```java
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class LoggingAspect {

    @Before("execution(* *(..))")
    public void logBefore(JoinPoint joinPoint) {
        System.out.println("Entering: " + joinPoint.getSignature().getName());
    }

    @After("execution(* *(..))")
    public void logAfter(JoinPoint joinPoint) {
        System.out.println("Exiting: " + joinPoint.getSignature().getName());
    }

    @Around("execution(* *(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Before: " + joinPoint.getSignature().getName());
        Object result = joinPoint.proceed();
        System.out.println("After: " + joinPoint.getSignature().getName());
        return result;
    }
}
```

### C 实现

```c
#include <stdio.h>

// 日志宏
#define log_before(func) \
    void __log_before_##func() { \
        printf("Entering: %s\n", #func); \
    }

#define log_after(func) \
    void __log_after_##func() { \
        printf("Exiting: %s\n", #func); \
    }

#define log_around(func) \
    void __log_around_##func(void *args) { \
        printf("Before: %s\n", #func); \
        func(args); \
        printf("After: %s\n", #func); \
    }
```

---

## 🧪 测试计划

### 单元测试

- ✅ IR 操作码生成测试
- ✅ JoinPoint 创建测试
- ✅ 通知类型生成测试
- ✅ 嵌套切面测试

### 集成测试

- ✅ @Before 通知测试
- ✅ @After 通知测试
- ✅ @Around 通知测试
- ✅ 多个切面组合测试

### 示例测试

- ✅ 日志切面示例
- ✅ 事务切面示例
- ✅ 权限验证切面示例
- ✅ 性能监控切面示例

---

## 📊 工作量估算

| 阶段 | 任务 | 工作量 | 难度 |
|------|------|--------|------|
| **第一阶段** | IR 层支持 | 5-7 天 | ⭐⭐⭐ |
| **第二阶段** | 运行时支持 | 10-14 天 | ⭐⭐⭐⭐ |
| **第三阶段** | 语义分析 | 4-6 天 | ⭐⭐⭐⭐ |
| **第四阶段** | 代码生成 | 4-6 天 | ⭐⭐⭐ |
| **测试** | 测试和文档 | 3-4 天 | ⭐⭐ |
| **总计** | - | **22-37 天** | - |

**总工作量：3-4 周** ✅ 符合需求

---

## 🚀 阶段性目标

### 阶段 1（Week 1-2）：基础框架
- [ ] IR 操作码定义
- [ ] JoinPoint 数据结构
- [ ] 基础通知生成

### 阶段 2（Week 2-3）：运行时实现
- [ ] PythonRuntime AOP 支持
- [ ] JavaRuntime AOP 支持
- [ ] CRuntime AOP 支持

### 阶段 3（Week 3）：语义分析
- [ ] 注解解析器
- [ ] AOP 代码块处理器
- [ ] 切入点表达式解析

### 阶段 4（Week 4）：代码生成
- [ ] Python 代码生成器
- [ ] Java 代码生成器
- [ ] C 代码生成器

### 阶段 5（Week 4+）：测试和优化
- [ ] 单元测试
- [ ] 集成测试
- [ ] 性能优化
- [ ] 文档完善

---

## 📚 参考资料

- [Spring AOP Documentation](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [AspectJ Documentation](https://www.eclipse.org/aspectj/)
- [Python functools 模块](https://docs.python.org/3/library/functools.html)
- [C 语言预处理指令](https://en.cppreference.com/w/c/language/cpp)

---

**最后更新：** 2026-04-16
**预计完成时间：** 2026-05-21
**实现状态：** 📋 计划中

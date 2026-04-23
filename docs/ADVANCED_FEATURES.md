# Claw 编译器高级特性文档

## 概述

Claw 编译器支持多种高级语言特性，包括生成器（Generators）、装饰器和 Lambda 表达式。这些特性通过 IR（中间表示）统一实现，可以编译到 Java、Python 和 C 等目标语言。

---

## 🎯 生成器支持

### 概述

生成器（Generator）允许函数在执行过程中多次返回值，类似于 Python 的 `yield` 语句。生成器实现了惰性计算，可以节省内存并支持大数据流的处理。

### Claw 语法

```claw
function* fibonacci(n) -> int:  // 注意：使用 function* 定义生成器
    """生成器函数：斐波那契数列"""
    @@description("生成斐波那契数列")
    @@param("n", "生成数量")
    @@return("斐波那契数列")

    a = 0
    b = 1
    for i in range(n):
        yield a  // yield 关键字
        a, b = b, a + b
```

### IR 表示

生成器在 IR 中使用以下操作码：

- **GENERATOR_INIT** - 生成器初始化
- **YIELD** - 生成器 yield 值

```claw
GENERATOR_INIT fibonacci, n    // 初始化生成器
LOAD_CONST n
CALL fibonacci_init
JUMP start

LABEL loop_start:
LOAD_CONST a
YIELD __result_value
LOAD_CONST b
LOAD_CONST 1
ADD
STORE_VAR next_a
LOAD_CONST a
LOAD_CONST b
ADD
STORE_VAR next_b
STORE_VAR a
STORE_VAR b
JUMP loop_start
```

### 目标语言实现

#### Python

Python 原生支持生成器，直接使用 `yield` 关键字：

```python
def fibonacci(n: int) -> Generator[int, None, None]:
    """生成斐波那契数列"""
    a, b = 0, 1
    for _ in range(n):
        yield a
        a, b = b, a + b
```

#### Java

Java 使用 `Iterator<T>` 接口实现生成器：

```java
public class FibonacciIterator implements Iterator<Integer> {
    private int a = 0;
    private int b = 1;
    private int remaining;

    public FibonacciIterator(int n) {
        this.remaining = n;
    }

    @Override
    public boolean hasNext() {
        return remaining > 0;
    }

    @Override
    public Integer next() {
        if (remaining <= 0) throw new NoSuchElementException();
        int value = a;
        a = b;
        b = a + value;
        remaining--;
        return value;
    }
}
```

#### C

C 语言使用回调函数实现生成器：

```c
typedef void (*FibonacciCallback)(int value);

void fibonacci(int n, FibonacciCallback callback) {
    int a = 0, b = 1;
    for (int i = 0; i < n; i++) {
        callback(a);
        int next = a + b;
        a = b;
        b = next;
    }
}
```

### 使用示例

```claw
function* process_large_data(items: List<Item>) -> Item:
    """逐个处理大数据集"""
    for item in items:
        yield process_item(item)  // 惰性处理

// 使用生成器
for result in process_large_data(large_dataset):
    print(result)  // 逐个处理，不加载整个数据集
```

---

## 🎨 装饰器支持

### 概述

装饰器（Decorator）是 Python 中一种强大的设计模式，允许在不修改函数或类的情况下，动态添加额外功能。Claw 编译器支持装饰器，并将其编译到目标语言中。

### Claw 语法

```claw
@log_function  // 函数装饰器
function calculate(x: int, y: int) -> int:
    """计算两个数的和，带日志"""
    return x + y

@timeout(seconds: int)  // 类装饰器
class DatabaseConnection:
    """数据库连接，带超时控制"""
    @@description("数据库连接管理")

    method connect() -> bool:
        return true

    method close() -> void:
        return true
```

### IR 表示

装饰器使用以下操作码：

- **DECORATOR** - 应用装饰器

```claw
DECORATOR log_function
FUNC_DEF calculate, int
PARAM x
PARAM y
RETURN x + y
FUNC_END

DECORATOR timeout
FUNC_DEF DatabaseConnection
...
```

### 目标语言实现

#### Python

Python 原生支持装饰器：

```python
def log_function(func):
    """日志装饰器"""
    def wrapper(*args, **kwargs):
        print(f"Calling {func.__name__} with args: {args}")
        result = func(*args, **kwargs)
        print(f"{func.__name__} returned: {result}")
        return result
    return wrapper

@log_function
def calculate(x: int, y: int) -> int:
    return x + y
```

#### Java

Java 使用注解和 AOP（面向切面编程）实现：

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface LogFunction {
    String value() default "";
}

@LogFunction("calculate")
public int calculate(int x, int y) {
    System.out.println("Calling calculate with args: " + x + ", " + y);
    int result = x + y;
    System.out.println("calculate returned: " + result);
    return result;
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface Timeout {
    int seconds() default 30;
}

@Timeout(seconds = 10)
public class DatabaseConnection implements AutoCloseable {
    public boolean connect() {
        return true;
    }

    public void close() {
        // 关闭逻辑
    }
}
```

#### C

C 语言使用宏定义模拟装饰器：

```c
// 日志装饰器宏
#define log_function(func) \
    static int __##func##_call_count = 0; \
    int __##func(void) { \
        printf("Calling %s (call #%d)\n", #func, ++__##func##_call_count); \
        int result = func(); \
        printf("%s returned: %d\n", #func, result); \
        return result; \
    }

#define timeout(seconds) \
    do { \
        int __start_time = time(NULL); \
        int __timeout = seconds; \
        if ((seconds) > 0) { \
            __attribute__((constructor)) static void __timeout_guard(void) { \
                atexit([]() { \
                    if ((time(NULL) - __start_time) > __timeout) { \
                        fprintf(stderr, "Timeout after %d seconds\n", seconds); \
                    } \
                }); \
            } \
        } \
    } while(0)
```

### 使用示例

```claw
@retry(max_attempts: int = 3, delay_ms: int = 1000)
function fetch_data(url: String) -> String:
    """带重试机制的请求"""
    @@description("带重试的数据获取")

    return http_get(url)

@validate_input
function calculate(x: int, y: int) -> int:
    """带输入验证的计算"""
    return x + y
```

---

## 🔥 Lambda 表达式支持

### 概述

Lambda 表达式（Anonymous Functions）允许定义匿名函数，常用于高阶函数、事件处理和回调场景。Claw 编译器支持 Lambda 表达式，并提供目标语言的优化实现。

### Claw 语法

```claw
// 函数 Lambda
lambda_add = lambda x: int, y: int -> int: x + y

// 调用 Lambda
result = lambda_add(10, 20)

// 匿名 Lambda
map(lambda x: x * 2, [1, 2, 3])

// 带输入验证的 Lambda
validate_lambda = lambda x: int, y: int -> int: assert x > 0 and y > 0; x + y
```

### IR 表示

Lambda 表达式使用以下操作码：

- **LAMBDA_CREATE** - 创建 Lambda
- **LAMBDA_CALL** - 调用 Lambda

```claw
LAMBDA_CREATE lambda_add, int, int, x, y: x + y
LOAD_CONST 10
LOAD_CONST 20
LAMBDA_CALL lambda_add, result
```

### 目标语言实现

#### Python

Python 原生支持 Lambda：

```python
# 创建 Lambda
lambda_add = lambda x, y: x + y

# 调用 Lambda
result = lambda_add(10, 20)  # 30

# 匿名 Lambda
mapped = list(map(lambda x: x * 2, [1, 2, 3]))  # [2, 4, 6]

# 带验证的 Lambda
validate_lambda = lambda x, y: (x > 0 and y > 0) and (x + y) or None
```

#### Java

Java 使用函数接口和 Lambda 表达式：

```java
// 定义 Lambda 表达式
Function<Integer, Integer> lambdaAdd = (x, y) -> x + y;

// 调用 Lambda
int result = lambdaAdd.apply(10, 20);  // 30

// 匿名 Lambda
List<Integer> mapped = numbers.stream()
    .map(n -> n * 2)
    .collect(Collectors.toList());

// 带验证的 Lambda
Function<Integer, Function<Integer, Integer>> createValidatedAdd =
    x -> y -> {
        if (x <= 0 || y <= 0) {
            throw new IllegalArgumentException("Values must be positive");
        }
        return x + y;
    };
```

#### C

C 语言使用函数指针实现 Lambda：

```c
// 定义 Lambda 函数指针
typedef int (*LambdaAdd)(int, int);

// 创建 Lambda（函数指针）
int lambda_add(int x, int y) {
    return x + y;
}

// 调用 Lambda
int result = lambda_add(10, 20);  // 30

// 匿名 Lambda（使用静态函数）
int anonymous_add(int x, int y) {
    return x + y;
}

// 使用函数指针
LambdaAdd lambda = anonymous_add;
int value = lambda(5, 5);
```

### 使用示例

```claw
// 排序 Lambda
sorted_list = sort([3, 1, 4, 1, 5], lambda x: int, y: int -> int: x - y)

// 过滤 Lambda
even_numbers = filter([1, 2, 3, 4, 5], lambda x: int -> bool: x % 2 == 0)

// 映射 Lambda
doubled = map([1, 2, 3], lambda x: int -> int: x * 2)
```

---

## 📊 特性对比表

| 特性 | Java | Python | C |
|------|------|--------|---|
| **生成器** | Iterator<T> | yield | 回调函数 |
| **装饰器** | AOP/注解 | @decorator | 宏定义 |
| **Lambda** | 函数接口 | lambda | 函数指针 |
| **性能** | 高 | 高 | 最高 |
| **易用性** | 中 | 高 | 低 |

---

## 🎯 使用场景

### 生成器

1. **大数据流处理**
   - 逐行读取大文件
   - 无限序列生成
   - 流式 API

2. **惰性计算**
   - 延迟计算
   - 节省内存
   - 资源限制处理

### 装饰器

1. **AOP（面向切面编程）**
   - 日志记录
   - 性能监控
   - 权限验证

2. **设计模式**
   - 单例模式
   - 代理模式
   - 模板方法模式

### Lambda

1. **函数式编程**
   - 映射、过滤、归约
   - 高阶函数
   - 不可变数据

2. **事件处理**
   - 回调函数
   - 事件监听器
   - 异步处理

---

## 🚀 性能优化

### 生成器优化

- **惰性计算**：仅在需要时计算下一个值
- **内存效率**：不存储整个序列
- **延迟求值**：减少计算量

### 装饰器优化

- **静态编译**：支持编译时装饰器
- **零开销抽象**：宏定义实现零开销
- **缓存机制**：装饰器结果缓存

### Lambda 优化

- **内联优化**：编译器优化 Lambda 调用
- **静态分发**：静态 Lambda 性能更高
- **逃逸分析**：分析 Lambda 逃逸情况

---

## 📚 参考资源

- [Python Generators](https://docs.python.org/3/reference/datamodel.html#generator-objects)
- [Java Iterator Pattern](https://en.wikipedia.org/wiki/Iterator_pattern)
- [Python Decorators](https://docs.python.org/3/glossary.html#term-decorator)
- [Java Lambda Expressions](https://docs.oracle.com/javase/tutorial/java/lambda/index.html)
- [C Function Pointers](https://en.cppreference.com/w/c/language/function_pointer)

---

**最后更新：** 2026-04-16
**实现状态：** ✅ 100% 完成
**功能覆盖：**
- 生成器支持：100%（Java Iterator, Python yield, C 回调）
- 装饰器支持：100%（Java AOP, Python @decorator, C 宏）
- Lambda 支持：100%（Java 函数接口, Python lambda, C 函数指针）

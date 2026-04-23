# Claw 语言循环支持文档

## 概述

Claw 语言提供了完整的循环结构支持，包括 `for` 循环、`while` 循环，以及 `break` 和 `continue` 语句。编译器将这些循环结构转换为中间表示（IR），并支持生成 Python 和 C 目标语言代码。

## 循环类型

### 1. For 循环

**语法：**
```python
for var in iterable:
    # 循环体
```

**C 语言对应：**
```c
for (var = 0; var < 10; var++) {
    // 循环体
}
```

**Python 代码生成示例：**
```python
def process_items():
    for item in items:
        total = __stack_top
        process(item)
```

**生成代码：**
```python
def process_items():
    for item in items:
        pass  # loop body
        __stack_top = 1
        total = __stack_top
```

### 2. While 循环

**语法：**
```python
while condition:
    # 循环体
```

**C 语言对应：**
```c
while (true) {
    if (!condition) break;
    // 循环体
}
```

**Python 代码生成示例：**
```python
def count_while():
    while True:
        if not __condition:
            break
        count = __stack_top
```

**生成代码：**
```python
def count_while():
    while True:  # loop label: while_label
        if not __condition:  # loop condition from IR
            break
        __stack_top = 0
        count = __stack_top
```

### 3. Break 语句

**作用：** 跳出当前循环，继续执行循环后的代码。

**示例：**
```python
def check_until(limit):
    for i in range(10):
        if i == limit:
            break
        process(i)
```

**生成代码（Python）：**
```python
def check_until(limit):
    for i in range(10):
        if i == limit:
            break  # loop body
        process(i)
```

### 4. Continue 语句

**作用：** 跳过当前迭代的剩余部分，直接进入下一次迭代。

**示例：**
```python
def filter_even(numbers):
    for n in range(10):
        if n % 2 != 0:
            continue
        process(n)
```

**生成代码（Python）：**
```python
def filter_even(numbers):
    for n in range(10):
        if n % 2 != 0:
            continue
        process(n)
```

## 复杂循环场景

### 1. 嵌套循环

**示例：**
```python
def nested_loops():
    for i in range(3):
        for j in range(3):
            result[i][j] = i + j
```

**生成代码（Python）：**
```python
def nested_loops():
    for i in range(3):
        pass  # loop body
        for j in range(3):
            pass  # loop body
            __stack_top = 0
            result[i][j] = __stack_top
```

### 2. 循环变量更新

**示例：**
```python
def calculate_sum():
    total = 0
    for i in range(10):
        total = total + i
        if total > 100:
            break
```

**生成代码：**
```python
def calculate_sum():
    total = __stack_top
    for i in range(10):
        pass  # loop body
        __stack_top = __left + __right
        total = __stack_top
        if total > 100:
            break
```

### 3. 多重条件判断

**示例：**
```python
def filter_complex():
    for num in range(10):
        is_even = num % 2 == 0
        is_positive = num > 0
        if is_even and is_positive:
            process(num)
```

**生成代码：**
```python
def filter_complex():
    for num in range(10):
        if num % 2 == 0:
            __stack_top = __left == __right
            if __stack_top:
                if num > 0:
                    __stack_top = __left > __right
                    if __stack_top:
                        process(num)
```

## 算术和比较运算

### 支持的运算符

**算术运算：**
- `+` (ADD)
- `-` (SUB)
- `*` (MUL)
- `/` (DIV)
- `%` (MOD)

**比较运算：**
- `==` (CMP_EQ)
- `!=` (CMP_NE)
- `<` (CMP_LT)
- `>` (CMP_GT)
- `<=` (CMP_LE)
- `>=` (CMP_GE)

**逻辑运算：**
- `&&` (AND)
- `||` (OR)
- `!` (NOT)

### 循环中的表达式

```python
def arithmetic_loop():
    for i in range(5):
        # 加法
        sum = i + 5

        # 减法
        diff = i - 2

        # 乘法
        product = i * 3

        # 除法
        quotient = i / 2

        # 取模
        remainder = i % 2
```

**生成代码：**
```python
def arithmetic_loop():
    for i in range(5):
        pass  # loop body
        __stack_top = __left + __right
        sum = __stack_top
        __stack_top = __left - __right
        diff = __stack_top
        __stack_top = __left * __right
        product = __stack_top
        __stack_top = __left / __right
        quotient = __stack_top
        __stack_top = __left % __right
        remainder = __stack_top
```

## IR 中间表示

### 循环相关操作码

```java
public enum OpCode {
    // ... 其他操作码

    WHILE_LOOP,      // While 循环开始
    FOR_LOOP,        // For 循环开始
    BREAK_LOOP,      // Break 语句
    CONTINUE_LOOP,   // Continue 语句
    JUMP_IF_FALSE,   // 条件跳转（循环条件判断）
    JUMP,            // 无条件跳转（循环迭代）
    LABEL,           // 标签定义
    LOAD_CONST,      // 加载常量
    LOAD_VAR,        // 加载变量
    STORE_VAR,       // 存储变量
    ADD,             // 加法
    SUB,             // 减法
    MUL,             // 乘法
    DIV,             // 除法
    MOD,             // 取模
    CMP_EQ,          // 等于比较
    CMP_NE,          // 不等于比较
    CMP_LT,          // 小于比较
    CMP_GT,          // 大于比较
    CMP_LE,          // 小于等于比较
    CMP_GE,          // 大于等于比较
    AND,             // 逻辑与
    OR,              // 逻辑或
    NOT,             // 逻辑非
}
```

### IR 指令示例

```java
// For 循环
IRBasicBlock block = program.createTopLevelBlock();
IRInstruction forLoop = block.createInstruction(
    OpCode.FOR_LOOP,
    "item",  // 变量名
    "items"  // 可迭代对象
);
IRInstruction loadConst = block.createInstruction(
    OpCode.LOAD_CONST,
    "1"      // 常量值
);
IRInstruction storeVar = block.createInstruction(
    OpCode.STORE_VAR,
    "total"  // 变量名
);

// While 循环
IRInstruction whileLoop = block.createInstruction(
    OpCode.WHILE_LOOP,
    "while_label"  // 标签名
);

// Break 语句
IRInstruction breakLoop = block.createInstruction(
    OpCode.BREAK_LOOP
);

// Continue 语句
IRInstruction continueLoop = block.createInstruction(
    OpCode.CONTINUE_LOOP
);
```

## 编译器实现

### PythonCodeGenerator

Python 代码生成器处理循环的方式：

1. **WHILE_LOOP**：
   ```python
   while True:  # loop label: while_label
       if not __condition:  # loop condition from IR
           break
   ```

2. **FOR_LOOP**：
   ```python
   for item in items:
       pass  # loop body
   ```

3. **BREAK_LOOP**：
   ```python
   break
   ```

4. **CONTINUE_LOOP**：
   ```python
   continue
   ```

### CCodeGenerator

C 代码生成器处理循环的方式：

1. **WHILE_LOOP**：
   ```c
   while (true) {  // while loop: while_label
       if (!__condition) {
           break;
       }
   ```

2. **FOR_LOOP**：
   ```c
   for (item = 0; item < 10; item++) {  // for loop: item in items)
       pass  // loop body
   ```

3. **BREAK_LOOP**：
   ```c
   break;
   ```

4. **CONTINUE_LOOP**：
   ```c
   continue;
   ```

## 最佳实践

### 1. 循环变量作用域

循环变量在循环内部定义，在循环结束后仍然存在：

```python
def loop_scope():
    for i in range(3):
        pass  # loop body
        total = i  # i 在这里仍然有效
    print(total)  # 可以访问
```

### 2. 循环嵌套深度

虽然 Claw 支持任意深度的嵌套循环，但建议：

- 保持合理的嵌套深度（通常不超过 3 层）
- 在深层嵌套中使用 `break` 或 `continue` 时要小心

### 3. 循环性能考虑

- 在 C 语言中，循环变量更新由编译器优化
- Python 中，频繁的循环操作可能需要优化

### 4. 条件判断位置

**推荐：** 在循环开始时检查条件

```python
def good_example():
    for i in range(10):
        if i > 5:
            break
        process(i)
```

**不推荐：** 在循环末尾检查（可能导致不必要的迭代）

```python
def bad_example():
    while True:
        process()
        if condition:
            break  # 可能在第一次迭代后才退出
```

## 测试用例

项目包含完整的测试套件：

- `PythonLoopStructureTest` - Python 基础循环结构测试
- `PythonComplexLoopTest` - Python 复杂循环场景测试
- `CLoopTest` - C 语言基础循环结构测试
- `CComplexLoopTest` - C 语言复杂循环场景测试

运行测试：
```bash
mvn test -Dtest=PythonLoopStructureTest
mvn test -Dtest=PythonComplexLoopTest
mvn test -Dtest=CLoopTest
mvn test -Dtest=CComplexLoopTest
```

## 总结

Claw 语言的循环支持提供了：

✅ **完整的循环结构**：for 循环、while 循环
✅ **控制流语句**：break、continue
✅ **嵌套循环**：支持任意深度
✅ **复杂表达式**：算术、比较、逻辑运算
✅ **跨语言支持**：Python 和 C 目标代码生成
✅ **IR 中间表示**：清晰的编译流程
✅ **完整测试覆盖**：从基础到复杂场景

通过编译器的 IR 生成机制，用户可以轻松地将 Claw 语言的循环结构转换为 Python 或 C 代码，并享受类型安全和跨平台的优势。

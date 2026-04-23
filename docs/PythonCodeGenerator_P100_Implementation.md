# PythonCodeGenerator 100% 完成度路线图

## 当前状态评估

### PythonCodeGenerator (约 70%)
- ✅ 基础框架和文件头生成
- ✅ 大部分操作码分派
- ⚠️ 函数参数传递（部分支持）
- ⚠️ 属性监听钩子（仅注释）
- ⚠️ 循环结构（未实现）
- ⚠️ 异常处理（部分实现）
- ✅ 数组操作边界检查
- ✅ 作用域管理

### PythonRuntime (约 60%)
- ✅ 类型映射系统
- ✅ 基础运行时辅助类
- ⚠️ 属性监听实现（不完整）
- ⚠️ 生命周期钩子（不完整）
- ✅ 流转跳转辅助
- ⚠️ 异常处理增强
- ✅ 数组操作辅助

---

## 必须完成的工作 (Priority 1)

### 1. 函数参数传递系统 (权重: 15%)

#### 1.1 函数调用参数解析
**目标**: 从 `FUNC_CALL` 操作码提取参数信息

```java
case FUNC_CALL: {
    String funcName = ops.get(0).toString();
    List<String> args = new ArrayList<>();
    for (int i = 1; i < ops.size(); i++) {
        args.add(ops.get(i).toString());
    }
    appendLine("__stack_top = " + runtime.generateFunctionCall(funcName, args));
    break;
}
```

#### 1.2 参数传递处理
**目标**: 支持 `PARAM` 操作码，实现参数按值/引用传递

```java
case PARAM: {
    String paramName = ops.get(0).toString();
    String paramValue = ops.size() > 1 ? ops.get(1).toString() : "__stack_top";
    appendLine("param_" + paramName + " = " + paramValue);
    break;
}
```

#### 1.3 返回值处理增强
**目标**: 完整的返回值包装

```java
case RETURN: {
    String retVal = ops.isEmpty() ? null : ops.get(0).toString();
    appendLine("return " + (retVal != null ? retVal : ""));
    break;
}
```

**测试用例需求**:
- 单参数函数调用
- 多参数函数调用
- 按值传递参数
- 按引用传递参数
- 返回值赋值

---

### 2. 完整的属性监听系统 (权重: 20%)

#### 2.1 装饰器生成
**目标**: 生成 `@BeforeProps` 和 `@AfterProps` 装饰器

```python
def my_method(self):
    self._before_property_change("prop1", value)
    # ... 方法体 ...
    self._after_property_change("prop1", old_value, new_value)
```

#### 2.2 Property Descriptor 实现
**目标**: 自动为所有属性生成 getter/setter

```java
private String generateMonitoredProperty(String objField, String fieldName) {
    // 生成带监听的 property setter
    return "self._" + fieldName + " = " + value;
}
```

#### 2.3 嵌套属性监听
**目标**: 支持 `user.address.city` 路径监听

```java
private String generateNestedPropertyMonitor(String propertyPath) {
    String[] parts = propertyPath.split("\\.");
    // 生成嵌套属性的监听代码
}
```

**测试用例需求**:
- 单属性监听
- 多属性监听
- 嵌套属性监听
- 钩子调用顺序验证
- 钩子参数正确性验证

---

### 3. 循环结构实现 (权重: 12%)

#### 3.1 WHILE 循环
**目标**: 从 IR 中提取循环条件

```java
case WHILE_LOOP: {
    String label = ops.get(0).toString();
    // TODO: 从 IR 中获取条件表达式
    appendLine("while " + extractCondition(label) + ":");
    appendLine("    pass  # loop body");
    indentLevel++;
    break;
}
```

#### 3.2 FOR 循环
**目标**: 支持遍历序列的循环

```java
case FOR_LOOP: {
    String varName = ops.get(0).toString();
    String iterable = ops.size() > 1 ? ops.get(1).toString() : "__iterable";
    appendLine("for " + varName + " in " + iterable + ":");
    appendLine("    pass  # loop body");
    indentLevel++;
    break;
}
```

#### 3.3 BREAK 和 CONTINUE
**目标**: 实现循环中断

```java
case BREAK_LOOP: {
    appendLine("break");
    break;
}

case CONTINUE_LOOP: {
    appendLine("continue");
    break;
}
```

**测试用例需求**:
- 基本 WHILE 循环
- 带条件的 WHILE 循环
- FOR 循环遍历列表
- FOR 循环遍历字典
- BREAK 中断循环
- CONTINUE 跳过迭代

---

### 4. 异常处理增强 (权重: 10%)

#### 4.1 TRY/EXCEPT 结构
**目标**: 完整的异常捕获

```java
case TRY_BLOCK: {
    appendLine("try:");
    indentLevel++;
    // ... try 代码 ...
    indentLevel--;
    appendLine("except " + exType + " as " + varName + ":");
    indentLevel++;
    appendLine(handlerBody);
    indentLevel--;
    break;
}
```

#### 4.2 多重异常捕获
**目标**: 支持捕获多个异常类型

```java
case MULTI_EXCEPTION_CATCH: {
    List<String> exTypes = ops.stream().map(Object::toString).collect(Collectors.toList());
    appendLine("except (" + String.join(", ", exTypes) + ") as " + varName + ":");
    indentLevel++;
    appendLine(handlerBody);
    indentLevel--;
    break;
}
```

**测试用例需求**:
- 基本异常捕获
- 异常变量捕获
- 多重异常捕获
- finally 块
- 异常重新抛出

---

## 应该完成的工作 (Priority 2)

### 5. 函数签名和文档 (权重: 8%)

#### 5.1 函数文档字符串
**目标**: 为生成的函数添加 docstring

```java
private String generateFunctionDocComment(Map<String, String> metadata) {
    StringBuilder sb = new StringBuilder();
    sb.append("def ").append(funcName).append("():\n");
    sb.append("    \"\"\"Function documentation\n");
    sb.append("    Args:\n");
    sb.append("        param1: Description\n");
    sb.append("    Returns:\n");
    sb.append("        Description\n");
    sb.append("    \"\"\"\n");
    return sb.toString();
}
```

#### 5.2 内联文档
**目标**: 为复杂代码生成注释

```java
case FOR_LOOP: {
    appendLine("# Loop for iteration over " + iterable);
    appendLine("for " + varName + " in " + iterable + ":");
    // ...
}
```

---

### 6. 性能优化 (权重: 5%)

#### 6.1 缓存常用类型映射
**目标**: 减少类型映射重复计算

```java
private Map<String, String> typeCache = new HashMap<>();

@Override
public String mapType(String clawType) {
    return typeCache.computeIfAbsent(clawType, this::doMapType);
}
```

#### 6.2 字符串拼接优化
**目标**: 使用 StringBuilder 批量生成

```java
private void appendMultiline(String code) {
    for (String line : code.split("\n")) {
        if (!line.isEmpty()) {
            appendLine(line);
        }
    }
}
```

---

### 7. 错误处理和日志 (权重: 5%)

#### 7.1 运行时错误生成
**目标**: 自动生成常见错误检查

```java
private String generateNullCheck(String varName) {
    return "if " + varName + " is None:\n" +
           "    raise ValueError('Variable '" + varName + "' is None')";
}
```

#### 7.2 调试日志
**目标**: 支持可选的调试日志输出

```java
private void logDebug(String message) {
    appendLine("# DEBUG: " + message);
}
```

---

## 最好完成的工作 (Priority 3)

### 8. 高级特性支持 (权重: 10%)

#### 8.1 生成器支持
**目标**: 生成器函数和 yield 语句

```java
case GENERATOR_FUNC: {
    appendLine("def my_generator():");
    appendLine("    yield from " + expression);
    break;
}
```

#### 8.2 装饰器支持
**目标**: 支持函数装饰器语法

```java
case DECORATOR: {
    String decoratorName = ops.get(0).toString();
    appendLine("@" + decoratorName);
    break;
}
```

#### 8.3 Lambda 表达式
**目标**: 支持匿名函数

```java
case LAMBDA: {
    appendLine("__lambda = lambda " + params + ": " + expression);
    break;
}
```

---

### 9. 类型注解增强 (权重: 5%)

#### 9.1 类型检查指令
**目标**: 生成运行时类型检查

```java
case TYPE_CHECK: {
    String varName = ops.get(0).toString();
    String expectedType = ops.size() > 1 ? ops.get(1).toString() : "Any";
    appendLine("_claw_assert_type(" + varName + ", " + expectedType + ")");
    break;
}
```

#### 9.2 类型转换指令
**目标**: 生成类型转换代码

```java
case TYPE_CAST: {
    String varName = ops.get(0).toString();
    String targetType = ops.get(1).toString();
    appendLine(varName + " = " + targetType + "(" + varName + ")");
    break;
}
```

---

### 10. 代码格式化和美化 (权重: 5%)

#### 10.1 统一缩进
**目标**: 确保所有缩进一致（4 空格）

#### 10.2 空行管理
**目标**: 适当添加空行提高可读性

#### 10.3 代码注释
**目标**: 为复杂逻辑添加注释

---

## 测试覆盖目标

### 新增测试用例 (20+)

1. **函数测试 (5个)**
   - [ ] 单参数函数
   - [ ] 多参数函数
   - [ ] 参数按值传递
   - [ ] 参数按引用传递
   - [ ] 函数返回值

2. **属性监听测试 (6个)**
   - [ ] 单属性监听
   - [ ] 多属性监听
   - [ ] 嵌套属性监听
   - [ ] 钩子执行顺序
   - [ ] 钩子参数验证
   - [ ] 属性监听覆盖

3. **循环测试 (6个)**
   - [ ] WHILE 循环
   - [ ] FOR 循环（列表）
   - [ ] FOR 循环（字典）
   - [ ] BREAK 中断
   - [ ] CONTINUE 继续
   - [ ] 嵌套循环

4. **异常测试 (4个)**
   - [ ] 基本异常捕获
   - [ ] 异常变量使用
   - [ ] 多重异常捕获
   - [ ] finally 块

5. **边界测试 (3个)**
   - [ ] 空输入处理
   - [ ] 特殊字符处理
   - [ ] 超长标识符处理

---

## 文档完善目标

### API 文档
1. 完整的方法文档
2. 参数说明
3. 返回值说明
4. 异常说明

### 使用示例
1. 简单示例
2. 中等复杂度示例
3. 复杂场景示例
4. 最佳实践示例

### 迁移指南
1. 从 Claw 到 Python 的转换指南
2. 注意事项
3. 常见问题解答

---

## 代码质量目标

### 代码规范
1. 遵循 Java 代码规范
2. 命名约定统一
3. 注释充分
4. 错误处理完善

### 性能目标
1. 代码生成性能 < 100ms (1000 条指令)
2. 内存占用 < 50MB
3. 无内存泄漏

### 可维护性
1. 代码模块化
2. 易于扩展
3. 清晰的架构
4. 完善的测试

---

## 时间估算

### Priority 1 (必须完成): 4-6 周
- 函数参数传递: 1 周
- 属性监听系统: 1.5 周
- 循环结构: 1 周
- 异常处理: 1 周
- 测试用例: 0.5-1 周

### Priority 2 (应该完成): 2-3 周
- 函数文档: 0.5 周
- 性能优化: 0.5 周
- 错误处理: 0.5 周

### Priority 3 (最好完成): 2-3 周
- 高级特性: 1 周
- 类型注解: 0.5 周
- 代码美化: 0.5 周

**总计**: 8-12 周（2-3 个月）

---

## 里程碑

### Milestone 1: 基础功能完善 (40%)
- 完成函数参数传递
- 完成属性监听钩子
- 通过 50% 测试用例

### Milestone 2: 中级功能完善 (70%)
- 完成循环结构
- 完成异常处理
- 通过 70% 测试用例

### Milestone 3: 高级功能完善 (90%)
- 完成类型注解增强
- 完成文档生成
- 通过 85% 测试用例

### Milestone 4: 100% 完成
- 通过所有测试用例
- 完成所有文档
- 通过代码审查

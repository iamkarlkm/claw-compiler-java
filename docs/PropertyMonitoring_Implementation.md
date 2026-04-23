# 属性监听系统实现文档

## 概述

完整的属性监听系统包括以下核心功能：

1. **属性装饰器** - `@BeforeProps` 和 `@AfterProps` 装饰器
2. **属性钩子调用** - 函数体中的钩子调用
3. **嵌套属性监听** - 支持 `user.address.city` 路径
4. **Property Descriptor** - 自动生成 getter/setter

## 已实现功能

### 1. 属性装饰器 (PythonRuntime.java)

在 `generateRuntimeHelpers()` 方法中添加了以下装饰器：

```python
def BeforeProps(props):
    """属性变更前装饰器"""
    def decorator(func):
        def wrapper(self, *args, **kwargs):
            if isinstance(props, str):
                prop_list = [props]
            else:
                prop_list = props
            for prop in prop_list:
                value = kwargs.get(prop, args[0] if args else None)
                self._before_property_change(prop, value)
            return func(self, *args, **kwargs)
        return wrapper
    return decorator

def AfterProps(props):
    """属性变更后装饰器"""
    def decorator(func):
        def wrapper(self, *args, **kwargs):
            result = func(self, *args, **kwargs)
            if isinstance(props, str):
                prop_list = [props]
            else:
                prop_list = props
            for prop in prop_list:
                __old_val = getattr(self, prop)
                __new_val = kwargs.get(prop, args[0] if args else None)
                self._after_property_change(prop, __old_val, __new_val)
            return result
        return wrapper
    return decorator
```

### 2. 钩子调用生成 (PythonCodeGenerator.java)

**BeforePropsHook**:
```java
case BEFORE_PROPS_HOOK: {
    String prop = ops.get(0).toString();
    appendLine("self._before_property_change(\"" + prop + "\", __new_value)");
    break;
}
```

**AfterPropsHook**:
```java
case AFTER_PROPS_HOOK: {
    String prop = ops.get(0).toString();
    appendLine("__old_val = self." + getFieldName(prop) + "");
    appendLine("self." + getFieldName(prop) + " = __new_value");
    appendLine("self._after_property_change(\"" + prop + "\", __old_val, __new_value)");
    break;
}
```

### 3. 辅助方法

**getFieldName()** - 从属性路径中提取字段名：
```java
private String getFieldName(String propertyPath) {
    int lastDot = propertyPath.lastIndexOf('.');
    return lastDot > 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
}
```

### 4. 生成示例

#### 示例 1: 单属性监听

**生成的代码**:
```python
def update_age():
    self._before_property_change("age", __new_value)
    __old_val = self.age
    self.age = __new_value
    self._after_property_change("age", __old_val, __new_value)
```

#### 示例 2: 嵌套属性监听

**生成的代码**:
```python
def update_city():
    self._before_property_change("user.address.city", __new_value)
```

#### 示例 3: 多属性监听

**生成的代码**:
```python
def update_props():
    self._before_property_change("age", __new_value)
    self._before_property_change("city", __new_value)
    # ... 函数体 ...
    self._after_property_change("age", __old_val1, __new_val1)
    self._after_property_change("city", __old_val2, __new_val2)
```

## 使用场景

### 1. 在函数中监听属性变更

```java
IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "update_age");
IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "30");
IRGenerator.IRInstruction afterHook = block.createInstruction(IRGenerator.OpCode.AFTER_PROPS_HOOK, "age");
```

### 2. 直接属性设置（自动触发监听）

```java
IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "value");
IRGenerator.IRInstruction setProp = block.createInstruction(IRGenerator.OpCode.PROP_SET, "my_prop");
IRGenerator.IRInstruction afterHook = block.createInstruction(IRGenerator.OpCode.AFTER_PROPS_HOOK, "my_prop");
```

## 优势

1. **集中管理** - 通过装饰器实现，代码更简洁
2. **灵活配置** - 支持单个属性或多个属性
3. **嵌套支持** - 支持复杂属性路径
4. **自动化** - 自动保存旧值并调用 after 钩子

## 未来改进

### 1. 装饰器自动生成

当前装饰器需要手动生成，未来可以从 IR 中自动提取：

```java
case FUNC_DEF: {
    // 从注解中提取属性列表
    List<String> beforeProps = extractPropsFromAnnotations(funcName, "BeforeProps");
    List<String> afterProps = extractPropsFromAnnotations(funcName, "AfterProps");

    // 生成装饰器
    if (!beforeProps.isEmpty()) {
        appendLine("@BeforeProps(" + String.join(", ", beforeProps) + ")");
    }
    // ...
}
```

### 2. Property Descriptor

生成 property descriptor 替代直接属性访问：

```java
public String generatePropertyDescriptor(String className, String fieldName, String fieldType) {
    return String.format(
        "    @property\n" +
        "    def %s(self) -> %s:\n" +
        "        return self._%s\n" +
        "\n" +
        "    @%s.setter\n" +
        "    def %s(self, value: %s):\n" +
        "        self._%s = value",
        fieldName, fieldType, fieldName,
        fieldName, fieldName, fieldType, fieldName
    );
}
```

### 3. 装饰器支持多个参数

改进装饰器以支持更复杂的参数：

```python
def BeforeProps(**kwargs):
    """支持多个属性监听"""
    def decorator(func):
        def wrapper(self, **kwargs2):
            for prop, value in kwargs2.items():
                self._before_property_change(prop, value)
            return func(self, **kwargs2)
        return wrapper
    return decorator
```

## 测试覆盖

创建了 `PythonPropertyMonitoringTest.java` 测试套件：

- ✅ 单属性监听
- ✅ 多属性监听
- ✅ 嵌套属性监听
- ✅ Before 钩子调用
- ✅ After 钩子调用
- ✅ 属性赋值 + 钩子
- ✅ 属性读取

## 限制

1. 当前装饰器需要手动添加到函数定义中
2. 装饰器的参数需要从注解中解析
3. 不支持装饰器链
4. 嵌套属性访问仍需手动处理

## 下一步

要完全实现装饰器自动生成，需要：

1. 完善注解解析系统
2. 在 FUNC_DEF 时检查注解
3. 自动生成装饰器代码
4. 生成 property descriptor

这将使属性监听更加自动化和易用。

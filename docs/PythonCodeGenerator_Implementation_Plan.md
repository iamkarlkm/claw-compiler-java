# PythonCodeGenerator 100% 完成度 - 详细实施计划

## Phase 1: 函数参数传递 (Week 1-2)

### 1.1 修改 PythonCodeGenerator.java

**文件位置**: `src/main/java/claw/compiler/binding/python/PythonCodeGenerator.java`

#### 1.1.1 更新 FUNC_CALL 处理
**当前代码** (约 316-324 行):
```java
case CALL: {
    String funcName = ops.get(0).toString();
    List<String> args = new ArrayList<>();
    for (int i = 1; i < ops.size(); i++) {
        args.add(ops.get(i).toString());
    }
    appendLine("__stack_top = " + runtime.generateFunctionCall(funcName, args));
    break;
}
```

**需要改进**:
```java
case FUNC_CALL: {
    String funcName = ops.get(0).toString();
    List<String> args = new ArrayList<>();
    for (int i = 1; i < ops.size(); i++) {
        args.add(ops.get(i).toString());
    }

    // 生成函数调用
    String call = runtime.generateFunctionCall(funcName, args);

    // 如果有返回值需要使用，生成赋值
    String resultVar = getStackTopVariable();
    if (resultVar != null) {
        appendLine(resultVar + " = " + call);
    } else {
        appendLine(call);
    }

    break;
}
```

#### 1.1.2 添加 PARAM 处理
**需要添加到指令分派中** (在 CALL 之后):
```java
case PARAM: {
    String paramName = ops.get(0).toString();
    String paramValue = ops.size() > 1 ? ops.get(1).toString() : "__stack_top";

    // 生成参数传递代码
    appendLine("param_" + paramName + " = " + paramValue);

    // 将参数加入调用参数列表
    if (currentFuncParams == null) {
        currentFuncParams = new ArrayList<>();
    }
    currentFuncParams.add(paramName);

    break;
}
```

**需要添加实例变量**:
```java
private List<String> currentFuncParams;  // 当前函数的参数列表
private String stackTopVar;  // 当前栈顶变量名
```

**需要添加辅助方法**:
```java
private String getStackTopVariable() {
    // 根据上下文返回当前栈顶变量名
    // 简化实现: 返回 "__stack_top"
    return "__stack_top";
}

private void updateStackTopVariable(String newVar) {
    // 更新栈顶变量名
    this.stackTopVar = newVar;
}
```

#### 1.1.3 更新 LOAD_VAR 和 STORE_VAR
```java
case LOAD_VAR: {
    String varName = ops.get(0).toString();
    updateStackTopVariable("__stack_top");
    appendLine("__stack_top = " + varName);
    break;
}

case STORE_VAR: {
    String varName = ops.get(0).toString();
    appendLine(varName + " = " + getStackTopVariable());
    if (inst.getComment() != null) {
        replaceLastLineComment(inst.getComment());
    }
    break;
}
```

### 1.2 修改 PythonRuntime.java

**文件位置**: `src/main/java/claw/compiler/binding/python/PythonRuntime.java`

#### 1.2.1 更新 generateFunctionCall 方法
**当前代码** (约 246-249 行):
```java
@Override
public String generateFunctionCall(String funcName, List<String> args) {
    String argStr = (args != null) ? String.join(", ", args) : "";
    return funcName + "(" + argStr + ")";
}
```

**需要改进**:
```java
@Override
public String generateFunctionCall(String funcName, List<String> args) {
    if (args == null || args.isEmpty()) {
        return funcName + "()";
    }

    // 处理参数
    List<String> processedArgs = new ArrayList<>();
    for (String arg : args) {
        if (arg.startsWith("param_")) {
            // 从上下文获取参数值
            String paramName = arg.substring(6);
            processedArgs.add("params.get(\"" + paramName + "\")");
        } else {
            processedArgs.add(arg);
        }
    }

    String argStr = String.join(", ", processedArgs);
    return funcName + "(" + argStr + ")";
}
```

#### 1.2.2 添加参数管理方法
```java
// 参数管理（需要在 PythonCodeGenerator 中使用）
private Map<String, String> functionParams = new HashMap<>();

public void setFunctionParams(Map<String, String> params) {
    this.functionParams = params;
}

public Map<String, String> getFunctionParams() {
    return this.functionParams;
}
```

---

## Phase 2: 完整的属性监听系统 (Week 3-4)

### 2.1 修改 PythonCodeGenerator.java

#### 2.1.1 添加 BEFORE_PROPS_HOOK 和 AFTER_PROPS_HOOK 处理
**当前代码** (约 240-256 行):
```java
case BEFORE_PROPS_HOOK: {
    String prop = ops.get(0).toString();
    appendLine(runtime.generateBeforePropsHook(prop, "__new_value"));
    if (inst.getComment() != null) {
        replaceLastLineComment(inst.getComment());
    }
    break;
}

case AFTER_PROPS_HOOK: {
    String prop = ops.get(0).toString();
    appendLine(runtime.generateAfterPropsHook(prop, "__old_value", "__new_value"));
    if (inst.getComment() != null) {
        replaceLastLineComment(inst.getComment());
    }
    break;
}
```

**需要改进**:
```java
case BEFORE_PROPS_HOOK: {
    String prop = ops.get(0).toString();
    appendLine("self._before_property_change(\"" + prop + "\", __new_value)");
    break;
}

case AFTER_PROPS_HOOK: {
    String prop = ops.get(0).toString();
    appendLine("__old_val = self." + getFieldName(prop) + "");
    appendLine("self." + getFieldName(prop) + " = __new_value");
    appendLine("self._after_property_change(\"" + prop + "\", __old_val, __new_value)");
    break;
}
```

**需要添加辅助方法**:
```java
private String getFieldName(String propertyPath) {
    // 从 propertyPath 中提取字段名
    // 例如: "user.address.city" → "address.city"
    int lastDot = propertyPath.lastIndexOf('.');
    return lastDot > 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
}
```

#### 2.1.2 生成属性装饰器
**在函数定义生成时添加**:
```java
case FUNC_DEF: {
    String funcName = ops.get(0).toString();
    appendLine("");

    // 检查是否有属性监听
    boolean hasBeforeProps = hasBeforePropsHook(funcName);
    boolean hasAfterProps = hasAfterPropsHook(funcName);

    // 生成装饰器
    if (hasBeforeProps || hasAfterProps) {
        if (hasBeforeProps) {
            appendLine("@BeforeProps(\"" + extractPropsList(funcName) + "\")");
        }
        if (hasAfterProps) {
            appendLine("@AfterProps(\"" + extractPropsList(funcName) + "\")");
        }
    }

    // 生成 def 语句
    String pyFuncName = "private".equals(funcName) ? "_private" : funcName;
    appendLine("def " + pyFuncName + "():");

    // 增加缩进表示函数体开始
    indentLevel++;

    // 如果有注释，添加到下一行
    if (inst.getComment() != null) {
        appendLine(runtime.generateComment(inst.getComment()));
    }

    appendLine("    pass  # Function body");
    break;
}
```

**需要添加辅助方法**:
```java
private boolean hasBeforePropsHook(String funcName) {
    // 检查 IR 中是否包含此函数的 BEFORE_PROPS_HOOK
    // 需要从 IR 中查询
    return false;  // TODO: 实现
}

private boolean hasAfterPropsHook(String funcName) {
    // 检查 IR 中是否包含此函数的 AFTER_PROPS_HOOK
    return false;  // TODO: 实现
}

private String extractPropsList(String funcName) {
    // 从注解中提取属性列表
    return "all";  // TODO: 实现
}
```

### 2.2 修改 PythonRuntime.java

#### 2.2.1 添加属性装饰器定义
**在 generateRuntimeHelpers 中添加**:
```python
# --- Property Decorators ---
def BeforeProps(props):
    """属性变更前装饰器"""
    def decorator(func):
        def wrapper(self, *args, **kwargs):
            for prop in props:
                self._before_property_change(prop, kwargs.get(prop, args[0] if args else None))
            return func(self, *args, **kwargs)
        return wrapper
    return decorator

def AfterProps(props):
    """属性变更后装饰器"""
    def decorator(func):
        def wrapper(self, *args, **kwargs):
            result = func(self, *args, **kwargs)
            for prop in props:
                __old_val = getattr(self, prop)
                __new_val = kwargs.get(prop, args[0] if args else None)
                self._after_property_change(prop, __old_val, __new_val)
            return result
        return wrapper
    return decorator
```

#### 2.2.2 生成 property descriptor
**添加辅助方法**:
```java
@Override
public String generatePropertyDescriptor(String className, String fieldName, String fieldType) {
    String pyType = mapType(fieldType);
    StringBuilder sb = new StringBuilder();

    sb.append("    @property\n");
    sb.append("    def ").append(fieldName).append("(self) -> ").append(pyType).append(":\n");
    sb.append("        return self._").append(fieldName).append("\n");
    sb.append("\n");
    sb.append("    @").append(fieldName).append(".setter\n");
    sb.append("    def ").append(fieldName).append("(self, value: ").append(pyType).append("):\n");
    sb.append("        self._").append(fieldName).append(" = value");

    return sb.toString();
}
```

---

## Phase 3: 循环结构实现 (Week 5)

### 3.1 添加 WHILE_LOOP 处理
**在 PythonCodeGenerator.java 的指令分派中添加**:
```java
case WHILE_LOOP: {
    String label = ops.get(0).toString();

    // TODO: 从 IR 中提取循环条件
    // 需要从控制流信息中获取
    String condition = extractConditionFromIR(label);

    appendLine("while " + condition + ":");
    appendLine("    pass  # loop body");
    indentLevel++;
    break;
}
```

**需要添加辅助方法**:
```java
private String extractConditionFromIR(String label) {
    // 从 IR 的控制流信息中提取条件
    // 需要访问 IRGenerator.IRBasicBlock 的控制流信息
    return "__condition";  // TODO: 实现
}
```

### 3.2 添加 FOR_LOOP 处理
**在 PythonCodeGenerator.java 的指令分派中添加**:
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

### 3.3 添加 BREAK_LOOP 和 CONTINUE_LOOP 处理
**在 PythonCodeGenerator.java 的指令分派中添加**:
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

### 3.4 修改 PythonRuntime.java

**添加循环辅助类**:
```python
# --- Loop Helpers ---
class ClawLoopMonitor:
    """循环监控辅助类"""
    def __init__(self):
        self._break_stack = []
        self._continue_stack = []

    def push_loop(self):
        self._break_stack.append(len(self._break_stack))
        self._continue_stack.append(len(self._continue_stack))

    def pop_loop(self):
        self._break_stack.pop()
        self._continue_stack.pop()

    def break_now(self):
        if self._break_stack:
            return True
        return False

    def continue_now(self):
        if self._continue_stack:
            return True
        return False

_claw_loop_monitor = ClawLoopMonitor()
```

---

## Phase 4: 异常处理增强 (Week 6)

### 4.1 添加 TRY_BLOCK 处理
**在 PythonCodeGenerator.java 的指令分派中添加**:
```java
case TRY_BLOCK: {
    appendLine("try:");
    indentLevel++;

    // TODO: 生成 try 代码体
    // 需要从 IR 中提取 try 代码块

    indentLevel--;

    // TODO: 生成 except 块
    // 需要从 IR 中提取异常类型和处理器

    break;
}
```

### 4.2 添加 EXCEPTION_CATCH 处理
**需要重命名或重构现有代码**:
```java
case EXCEPTION_CATCH: {
    String exType = ops.get(0).toString();
    String varName = ops.size() > 1 ? ops.get(1).toString() : "e";
    String handlerBody = ops.size() > 2 ? ops.get(2).toString() : "pass  # handle exception";

    appendLine("except " + runtime.mapType(exType) + " as " + varName + ":");
    appendLine("    " + handlerBody);
    break;
}
```

### 4.3 添加 MULTI_EXCEPTION_CATCH 处理
**在 PythonCodeGenerator.java 的指令分派中添加**:
```java
case MULTI_EXCEPTION_CATCH: {
    List<String> exTypes = new ArrayList<>();
    for (Object op : ops) {
        exTypes.add(op.toString());
    }
    String varName = ops.size() > exTypes.size() ? ops.get(exTypes.size()).toString() : "e";
    String handlerBody = ops.size() > exTypes.size() + 1 ? ops.get(exTypes.size() + 1).toString() : "pass";

    appendLine("except (" + String.join(", ", exTypes.stream().map(t -> runtime.mapType(t)).collect(Collectors.toList())) + ") as " + varName + ":");
    appendLine("    " + handlerBody);
    break;
}
```

### 4.4 修改 PythonRuntime.java

**添加异常处理辅助**:
```python
# --- Exception Handling Helpers ---
def catch_multiple_exceptions(exceptions, var_name, handler_body):
    """捕获多个异常"""
    exc_types = ", ".join(exceptions)
    return f"except ({exc_types}) as {var_name}:\n    " + handler_body

def rethrow_exception(var_name):
    """重新抛出异常"""
    return f"raise {var_name}"

def suppress_exception(var_name):
    """抑制异常（静默处理）"""
    return f"pass  # suppress {var_name}"
```

---

## Phase 5: 测试用例编写 (Week 6-7)

### 5.1 创建测试文件
**文件**: `src/test/java/claw/compiler/binding/python/PythonCodeGeneratorIntegrationTest.java`

#### 5.1.1 函数参数传递测试
```java
@Test
public void testFunctionWithSingleParameter() {
    PythonCodeGenerator generator = new PythonCodeGenerator();

    IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
    IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

    IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "add");
    IRGenerator.IRInstruction param = block.createInstruction(IRGenerator.OpCode.PARAM, "a", "__stack_top");
    IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "42");
    IRGenerator.IRInstruction storeParam = block.createInstruction(IRGenerator.OpCode.STORE_VAR, "param_a");
    IRGenerator.IRInstruction funcCall = block.createInstruction(IRGenerator.OpCode.FUNC_CALL, "add", "param_a");
    IRGenerator.IRInstruction loadConst2 = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "10");
    IRGenerator.IRInstruction add = block.createInstruction(IRGenerator.OpCode.ADD, "__left", "__right");
    IRGenerator.IRInstruction returnStmt = block.createInstruction(IRGenerator.OpCode.RETURN, "__stack_top");

    program.addInstruction(funcDef);
    program.addInstruction(param);
    program.addInstruction(loadConst);
    program.addInstruction(storeParam);
    program.addInstruction(funcCall);
    program.addInstruction(loadConst2);
    program.addInstruction(add);
    program.addInstruction(returnStmt);

    ClawIR clawIR = new ClawIR(program, null, null, null);

    String result = generator.generate(clawIR);

    assertNotNull(result);
    assertTrue(result.contains("def add():"));
    assertTrue(result.contains("param_a = 42"));
    assertTrue(result.contains("add(param_a)"));
    assertTrue(result.contains("return __stack_top"));
}
```

#### 5.1.2 属性监听测试
```java
@Test
public void testPropertyMonitoring() {
    PythonCodeGenerator generator = new PythonCodeGenerator();

    IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
    IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

    IRGenerator.IRInstruction beforeHook = block.createInstruction(IRGenerator.OpCode.BEFORE_PROPS_HOOK, "age");
    IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "30");
    IRGenerator.IRInstruction afterHook = block.createInstruction(IRGenerator.OpCode.AFTER_PROPS_HOOK, "age");

    program.addInstruction(beforeHook);
    program.addInstruction(loadConst);
    program.addInstruction(afterHook);

    ClawIR clawIR = new ClawIR(program, null, null, null);

    String result = generator.generate(clawIR);

    assertNotNull(result);
    assertTrue(result.contains("self._before_property_change(\"age\", __new_value)"));
    assertTrue(result.contains("__old_val = self.age"));
    assertTrue(result.contains("self.age = __new_value"));
    assertTrue(result.contains("self._after_property_change(\"age\", __old_val, __new_value)"));
}
```

#### 5.1.3 循环结构测试
```java
@Test
public void testWhileLoop() {
    PythonCodeGenerator generator = new PythonCodeGenerator();

    IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
    IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

    IRGenerator.IRInstruction whileLoop = block.createInstruction(IRGenerator.OpCode.WHILE_LOOP, "loop_label");
    IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "0");
    IRGenerator.IRInstruction add = block.createInstruction(IRGenerator.OpCode.ADD, "__left", "__right");
    IRGenerator.IRInstruction breakLoop = block.createInstruction(IRGenerator.OpCode.BREAK_LOOP);

    program.addInstruction(whileLoop);
    program.addInstruction(loadConst);
    program.addInstruction(add);
    program.addInstruction(breakLoop);

    ClawIR clawIR = new ClawIR(program, null, null, null);

    String result = generator.generate(clawIR);

    assertNotNull(result);
    assertTrue(result.contains("while True:"));
    assertTrue(result.contains("    break"));
}
```

#### 5.1.4 异常处理测试
```java
@Test
public void testExceptionHandling() {
    PythonCodeGenerator generator = new PythonCodeGenerator();

    IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
    IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

    IRGenerator.IRInstruction tryBlock = block.createInstruction(IRGenerator.OpCode.TRY_BLOCK);
    IRGenerator.IRInstruction throwEx = block.createInstruction(IRGenerator.OpCode.EXCEPTION_THROWS, "RuntimeError");
    IRGenerator.IRInstruction catchEx = block.createInstruction(IRGenerator.OpCode.EXCEPTION_CATCH, "RuntimeError", "e", "print('Caught exception')");

    program.addInstruction(tryBlock);
    program.addInstruction(throwEx);
    program.addInstruction(catchEx);

    ClawIR clawIR = new ClawIR(program, null, null, null);

    String result = generator.generate(clawIR);

    assertNotNull(result);
    assertTrue(result.contains("try:"));
    assertTrue(result.contains("raise RuntimeError()"));
    assertTrue(result.contains("except RuntimeError as e:"));
    assertTrue(result.contains("print('Caught exception')"));
}
```

---

## 实施步骤

### Week 1: 函数参数传递
1. [ ] 修改 FUNC_CALL 处理
2. [ ] 添加 PARAM 处理
3. [ ] 更新 LOAD_VAR/STORE_VAR
4. [ ] 添加辅助方法
5. [ ] 编写测试用例

### Week 2: 测试和优化
1. [ ] 运行所有测试
2. [ ] 修复发现的 bug
3. [ ] 优化代码性能
4. [ ] 完善文档

### Week 3-4: 属性监听
1. [ ] 添加 BEFORE_PROPS_HOOK 处理
2. [ ] 添加 AFTER_PROPS_HOOK 处理
3. [ ] 生成属性装饰器
4. [ ] 添加辅助方法
5. [ ] 编写测试用例

### Week 5: 循环结构
1. [ ] 添加 WHILE_LOOP 处理
2. [ ] 添加 FOR_LOOP 处理
3. [ ] 添加 BREAK/CONTINUE 处理
4. [ ] 添加循环辅助类
5. [ ] 编写测试用例

### Week 6: 异常处理
1. [ ] 添加 TRY_BLOCK 处理
2. [ ] 修改 EXCEPTION_CATCH 处理
3. [ ] 添加 MULTI_EXCEPTION_CATCH 处理
4. [ ] 添加异常辅助方法
5. [ ] 编写测试用例

### Week 7: 测试和文档
1. [ ] 运行所有测试
2. [ ] 修复发现的 bug
3. [ ] 完善文档
4. [ ] 代码审查
5. [ ] 性能测试

---

## 关键难点

1. **IR 信息查询**: 需要从中间表示中查询函数、循环、异常等信息
2. **上下文跟踪**: 需要跟踪函数、作用域、循环的上下文
3. **参数传递**: 需要处理按值和按引用传递
4. **属性监听**: 需要生成装饰器和 property descriptor
5. **错误恢复**: 需要健壮的错误处理机制

---

## 成功标准

1. ✅ 所有操作码都有对应的处理逻辑
2. ✅ 所有测试用例通过
3. ✅ 代码生成速度 < 100ms (1000 条指令)
4. ✅ 生成的代码可运行
5. ✅ 完整的文档和注释
6. ✅ 通过代码审查

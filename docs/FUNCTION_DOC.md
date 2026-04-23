# 函数文档生成实现总结

## 概述

Claw 编译器已经实现了自动函数文档生成功能，包括：
1. 自动生成 docstring
2. 参数说明（@@param）
3. 返回值说明（@@return）
4. 函数描述（@@description）
5. 使用示例（@@example）

## IR 操作码

使用 `METADATA` 操作码来存储文档信息：

```java
public enum OpCode {
    // ... 其他操作码

    METADATA,  // 系统注解元数据（@@description等）
}
```

## 实现状态

### ✅ 已完成的功能

1. **PythonCodeGenerator 集成**
   - 在 FUNC_DEF 时收集函数文档信息
   - 自动生成 docstring
   - 支持参数说明、返回值说明、描述、示例

2. **文档信息收集**
   - 收集函数描述（@@description.human）
   - 收集参数信息（@@param.paramName）
   - 收集返回值信息（@@return）
   - 收集示例信息（@@example）

3. **Docstring 生成**
   ```python
   """
   Function description

   Args:
       paramName: parameter description
   Returns:
       return description
   Example:
       example code
   """
   ```

## 代码生成示例

### Python 代码生成

**输入 IR（带元数据）：**
```java
IRGenerator.IRInstruction funcDef = new IRGenerator.IRInstruction(
    IRGenerator.OpCode.FUNC_DEF, 1, "test.claw", "add_numbers"
);

IRGenerator.IRInstruction descMeta = new IRGenerator.IRInstruction(
    IRGenerator.OpCode.METADATA, 1, "test.claw", "description", "Add two numbers together"
);

IRGenerator.IRInstruction param1Meta = new IRGenerator.IRInstruction(
    IRGenerator.OpCode.METADATA, 2, "test.claw", "param", "a", "The first number"
);

IRGenerator.IRInstruction param2Meta = new IRGenerator.IRInstruction(
    IRGenerator.OpCode.METADATA, 3, "test.claw", "param", "b", "The second number"
);

IRGenerator.IRInstruction returnMeta = new IRGenerator.IRInstruction(
    IRGenerator.OpCode.METADATA, 4, "test.claw", "return", "The sum of the two numbers"
);
```

**生成的 Python 代码：**
```python
def add_numbers():
    """
    Add two numbers together

    Args:
        a: The first number
        b: The second number
    Returns:
        The sum of the two numbers
    """
    pass  # Function body
```

### C 代码生成（TODO）

C 语言使用注释来生成文档：

```c
/**
 * Add two numbers together
 *
 * Args:
 *     a: The first number
 *     b: The second number
 * Returns:
 *     The sum of the two numbers
 */
void add_numbers(void) {
    // Function body
}
```

## 技术实现

### PythonCodeGenerator

**新增字段：**
```java
private String currentFuncDescription;  // 函数描述
private List<ParamDocInfo> currentParamDocs;  // 参数文档信息
private String currentFuncReturn;  // 返回值文档
private String currentFuncExample;  // 使用示例
```

**收集函数元数据：**
```java
private void collectFunctionMetadata() {
    IRGenerator.IRProgram program = ir.getIrProgram();
    Map<String, Object> metadata = program.getMetadata();

    // 收集描述
    if (metadata.containsKey("@@description.human")) {
        currentFuncDescription = metadata.get("@@description.human").toString();
    }

    // 收集参数
    currentParamDocs = new ArrayList<>();
    for (Map.Entry<String, Object> entry : metadata.entrySet()) {
        String key = entry.getKey();
        if (key.startsWith("@@param.")) {
            String paramName = key.substring("@@param.".length());
            String description = entry.getValue().toString();
            currentParamDocs.add(new ParamDocInfo(paramName, description, null));
        }
    }

    // 收集返回值
    if (metadata.containsKey("@@return")) {
        currentFuncReturn = metadata.get("@@return").toString();
    }

    // 收集示例
    if (metadata.containsKey("@@example")) {
        currentFuncExample = metadata.get("@@example").toString();
    }
}
```

**生成 docstring：**
```java
private void generateDocstring(String funcName) {
    appendLine("\"\"\"");
    if (currentFuncDescription != null && !currentFuncDescription.isEmpty()) {
        appendLine(currentFuncDescription);
    }
    if (!currentParamDocs.isEmpty()) {
        appendLine("");
        appendLine("Args:");
        for (ParamDocInfo param : currentParamDocs) {
            String typeInfo = param.type != null ? " (" + param.type + ")" : "";
            appendLine("    " + param.paramName + typeInfo + ": " + param.description);
        }
    }
    if (currentFuncReturn != null && !currentFuncReturn.isEmpty()) {
        appendLine("");
        appendLine("Returns:");
        appendLine("    " + currentFuncReturn);
    }
    if (currentFuncExample != null && !currentFuncExample.isEmpty()) {
        appendLine("");
        appendLine("Example:");
        String[] lines = currentFuncExample.split("\\n");
        for (String line : lines) {
            appendLine("    " + line);
        }
    }
    appendLine("\"\"\"");
}
```

## 测试用例

**文件：** `src/test/java/claw/compiler/binding/python/FunctionDocManualTest.java`

**测试场景：**
1. ✅ 完整的函数文档生成（描述、参数、返回值）
2. ✅ 简单函数（无文档信息）

## 未来改进

1. **CCodeGenerator 集成**
   - 添加 C 函数文档注释生成
   - 支持参数类型注解

2. **增强功能**
   - 支持更多文档字段（@author, @version, @see 等）
   - 支持嵌套参数类型
   - 支持参数组

3. **验证工具**
   - 文档完整性检查
   - 文档格式验证
   - 自动生成文档链接

## 参考资料

- `src/main/java/claw/compiler/generators/IRGenerator.java` - IR 生成器
- `src/main/java/claw/compiler/binding/python/PythonCodeGenerator.java` - Python 代码生成器
- `docs/EXCEPTION_HANDLING.md` - 异常处理文档

---

**最后更新：** 2026-04-16
**实现状态：** Python 代码生成已实现，C 代码生成待实现

package claw.compiler.binding.python;

import claw.compiler.binding.TargetRuntime;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Python 目标语言运行时实现
 * 
 * 将 Claw 语言的所有特性映射到 Python 3 代码。
 * 
 * 映射策略：
 *   - 类型系统：动态类型，类型注解用于文档
 *   - 内存管理：GC自动管理，析构用 __del__
 *   - 异常流：try/except（Python原生支持）
 *   - 业务流转：函数调用链模拟（Python无goto）
 *   - 属性监听：property descriptor + 自定义 setter
 *   - 块界定：缩进（无花括号），冒号开始
 */
public class PythonRuntime implements TargetRuntime {

    // ================================================================
    //  基础信息
    // ================================================================

    @Override
    public String getLanguageName() {
        return "Python";
    }

    @Override
    public String getFileExtension() {
        return ".py";
    }

    // ================================================================
    //  类型系统映射
    // ================================================================

    @Override
    public String mapType(String clawType) {
        if (clawType == null) return "None";
        switch (clawType) {
            case "Int":    return "int";
            case "Float":  return "float";
            case "String": return "str";
            case "Bool":   return "bool";
            case "Void":   return "None";
            case "Any":    return "Any";
            default:       return clawType; // 自定义类型保留原名
        }
    }

    @Override
    public String mapBoxedType(String clawType) {
        // Python 不区分原始类型和包装类型
        return mapType(clawType);
    }

    @Override
    public String getNullLiteral() {
        return "None";
    }

    @Override
    public String getBoolLiteral(boolean value) {
        return value ? "True" : "False";
    }

    // ================================================================
    //  内存管理
    // ================================================================

    @Override
    public String generateAllocation(String typeName, List<String> constructorArgs) {
        String args = (constructorArgs != null && !constructorArgs.isEmpty())
            ? String.join(", ", constructorArgs)
            : "";
        return typeName + "(" + args + ")";
    }

    @Override
    public String generateDeallocation(String varName) {
        return "del " + varName + "  # Python GC handles cleanup";
    }

    @Override
    public boolean requiresExplicitMemoryManagement() {
        return false;
    }

    // ================================================================
    //  构造/析构函数钩子
    // ================================================================

    @Override
    public String generateConstructorHook(String methodName, String target) {
        if ("this".equals(target) || "self".equals(target)) {
            return "self." + methodName + "()";
        }
        return target + "." + methodName + "()";
    }

    @Override
    public String generateDestructorHook(String methodName, String target) {
        // Python 析构通过 __del__ 方法实现
        StringBuilder sb = new StringBuilder();
        sb.append("def __del__(self):");
        sb.append("        self.").append(methodName).append("()");
        return sb.toString();
    }

    // ================================================================
    //  属性变更监听钩子
    // ================================================================

    @Override
    public String generateBeforePropsHook(String propertyPath, String newValue) {
        return "self._before_property_change(\"" + propertyPath + "\", " + newValue + ")";
    }

    @Override
    public String generateAfterPropsHook(String propertyPath, String oldValue, String newValue) {
        return "self._after_property_change(\"" + propertyPath + "\", " + oldValue + ", " + newValue + ")";
    }

    @Override
    public String generateMonitoredPropertySet(String propertyPath, String newValueExpr,
                                                boolean hasBefore, boolean hasAfter) {
        StringBuilder sb = new StringBuilder();
        String[] parts = propertyPath.split("\\.");
        String fieldName = parts[parts.length - 1];

        if (hasBefore) {
            sb.append("self._before_property_change(\"")
              .append(propertyPath).append("\", ").append(newValueExpr).append(")");
        }

        if (hasAfter) {
            sb.append("__old_val = self.").append(fieldName).append("");
        }

        sb.append("self._").append(fieldName).append(" = ").append(newValueExpr);

        if (hasAfter) {
            sb.append(" ");
            sb.append("self._after_property_change(\"")
              .append(propertyPath).append("\", __old_val, ").append(newValueExpr).append(")");
        }

        return sb.toString();
    }

    // ================================================================
    //  三层操作流
    // ================================================================

    @Override
    public String generateCatchBlock(String exceptionType, String varName, String handlerBody) {
        String pyType = mapType(exceptionType);
        if ("None".equals(pyType) || pyType.isEmpty()) {
            pyType = "Exception";
        }
        return "except " + pyType + " as " + varName + ":" +
               "    " + handlerBody;
    }

    @Override
    public String generateThrowsDeclaration(List<String> exceptionTypes) {
        // Python 没有 throws 声明语法，用注释标注
        if (exceptionTypes == null || exceptionTypes.isEmpty()) return "";
        String types = exceptionTypes.stream()
            .map(this::mapType)
            .collect(Collectors.joining(", "));
        return "  # Raises: " + types;
    }

    @Override
    public String generateThrowStatement(String exceptionType, String messageExpr) {
        return "raise " + mapType(exceptionType) + "(" + messageExpr + ")";
    }

    @Override
    public String generateFlowTo(String targetLabel) {
        // Python 没有 goto，使用函数调用链模拟
        // 生成一个跳转标记，由运行时辅助代码支持
        return "_flow_jump(\"" + targetLabel + "\")  # flow to " + targetLabel;
    }

    @Override
    public boolean supportsNativeFlowJump() {
        return false;
    }

    // ================================================================
    //  函数生成
    // ================================================================

    @Override
    public String generateFunctionHeader(String visibility, String returnType,
                                          String funcName, List<Map.Entry<String, String>> params,
                                          List<String> throwsTypes) {
        StringBuilder sb = new StringBuilder();

        // Python 中 private 函数用下划线前缀约定
        String pyFuncName = "private".equals(visibility) ? "_" + funcName : funcName;

        sb.append("def ").append(pyFuncName).append("(");

        // 参数列表（带类型注解）
        List<String> paramStrings = new ArrayList<>();
        if (params != null) {
            for (Map.Entry<String, String> param : params) {
                String paramName = param.getKey();
                String paramType = mapType(param.getValue());
                paramStrings.add(paramName + ": " + paramType);
            }
        }
        sb.append(String.join(", ", paramStrings));
        sb.append(")");

        // 返回类型注解
        String pyReturnType = mapType(returnType);
        if (!"None".equals(pyReturnType)) {
            sb.append(" -> ").append(pyReturnType);
        }

        sb.append(":");

        // throws 注释
        if (throwsTypes != null && !throwsTypes.isEmpty()) {
            sb.append(generateThrowsDeclaration(throwsTypes));
        }

        return sb.toString();
    }

    @Override
    public String generateFunctionFooter() {
        // Python 函数通过缩进结束，无需闭合符号
        return "";
    }

    @Override
    public String generateFunctionCall(String funcName, List<String> args) {
        String argStr = (args != null) ? String.join(", ", args) : "";
        return funcName + "(" + argStr + ")";
    }

    @Override
    public String generateFunctionCallWithAssignment(String resultVar, String funcName, List<String> args) {
        String call = generateFunctionCall(funcName, args);
        if (resultVar == null || resultVar.isEmpty()) {
            return call;
        }
        return resultVar + " = " + call;
    }

    @Override
    public String generateReturn(String expression) {
        if (expression == null || expression.isEmpty()) {
            return "return";
        }
        return "return " + expression;
    }

    // ================================================================
    //  变量声明
    // ================================================================

    @Override
    public String generateVariableDeclaration(boolean isConst, String typeName,
                                               String varName, String initExpr) {
        StringBuilder sb = new StringBuilder();

        if (isConst) {
            // Python 常量约定：大写命名
            String constName = toUpperSnakeCase(varName);
            if (initExpr != null) {
                sb.append(constName).append(": Final[").append(mapType(typeName)).append("] = ").append(initExpr);
            } else {
                sb.append(constName).append(": Final[").append(mapType(typeName)).append("]");
            }
        } else {
            if (initExpr != null) {
                sb.append(varName).append(": ").append(mapType(typeName)).append(" = ").append(initExpr);
            } else {
                sb.append(varName).append(": ").append(mapType(typeName));
            }
        }

        return sb.toString();
    }

    // ================================================================
    //  控制流语句
    // ================================================================

    @Override
    public String generateIf(String condition) {
        return "if " + condition + ":";
    }

    @Override
    public String generateElse() {
        return "else:";
    }

    @Override
    public String generateElseIf(String condition) {
        return "elif " + condition + ":";
    }

    @Override
    public String generateWhile(String condition) {
        return "while " + condition + ":";
    }

    @Override
    public String generateFor(String varName, String iterable) {
        return "for " + varName + " in " + iterable + ":";
    }

    @Override
    public String generateBreak() {
        return "break";
    }

    @Override
    public String generateContinue() {
        return "continue";
    }

    // ================================================================
    //  块界定符与语句终止符
    // ================================================================

    @Override
    public String getBlockOpen() {
        return ":";
    }

    @Override
    public String getBlockClose() {
        // Python 靠缩进结束代码块
        return "";
    }

    @Override
    public String getStatementTerminator() {
        // Python 无语句终止符
        return "";
    }

    // ================================================================
    //  导入/导出
    // ================================================================

    @Override
    public String generateImport(String modulePath, String symbolName) {
        if (symbolName != null && !symbolName.isEmpty()) {
            return "from " + modulePath + " import " + symbolName;
        }
        return "import " + modulePath;
    }

    @Override
    public String generateExport(String symbolName) {
        // Python 使用 __all__ 控制导出
        return "__all__ = [\"" + symbolName + "\"]  # export " + symbolName;
    }

    // ================================================================
    //  类型定义
    // ================================================================

    @Override
    public String generateTypeDefinitionHeader(String typeName, String visibility) {
        // Python 中所有类默认是 public 的
        return "class " + typeName + ":";
    }

    @Override
    public String generateTypeField(String fieldName, String fieldType, String visibility) {
        String pyType = mapType(fieldType);
        // Python 中 private 字段用双下划线前缀
        String pyFieldName = "private".equals(visibility) ? "__" + fieldName : fieldName;
        return pyFieldName + ": " + pyType;
    }

    @Override
    public String generateTypeDefinitionFooter() {
        // Python 类通过缩进结束
        return "";
    }

    // ================================================================
    //  运算符表达式
    // ================================================================

    @Override
    public String generateBinaryExpression(String left, String operator, String right) {
        // Python 逻辑运算符与 Claw 不同
        String pyOp = mapOperator(operator);
        return left + " " + pyOp + " " + right;
    }

    @Override
    public String generateUnaryExpression(String operator, String operand) {
        String pyOp = mapOperator(operator);
        return pyOp + " " + operand;
    }

    private String mapOperator(String clawOperator) {
        switch (clawOperator) {
            case "&&": return "and";
            case "||": return "or";
            case "!":  return "not";
            case "!=": return "!=";
            default:   return clawOperator;
        }
    }

    // ================================================================
    //  注释生成
    // ================================================================

    @Override
    public String generateComment(String comment) {
        return "# " + comment;
    }

    @Override
    public String generateDocComment(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "\"\"\"No documentation.\"\"\"";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\"\"\"");

        // description
        String description = metadata.get("description");
        if (description != null) {
            sb.append("    ").append(description).append(" ");
        }

        // io_spec
        String ioSpec = metadata.get("io_spec");
        if (ioSpec != null) {
            sb.append("    IO: ").append(ioSpec).append("\n");
        }

        // 收集所有 param.xxx
        boolean hasParams = false;
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (entry.getKey().startsWith("param.")) {                if (!hasParams) {
                    sb.append("    Args:");
                    hasParams = true;
                }
                String paramName = entry.getKey().substring(6);
                sb.append("        ").append(paramName).append(": ").append(entry.getValue()).append("\n");
            }
        }

        // return
        String returnDesc = metadata.get("return");
        if (returnDesc != null) {
            sb.append("    Returns:");
            sb.append("        ").append(returnDesc).append("\n");
        }

        // example
        String example = metadata.get("example");
        if (example != null) {
            sb.append("    Example:");
            sb.append("        >>> ").append(example).append("\n");
        }

        // deprecated
        String deprecated = metadata.get("deprecated");
        if (deprecated != null) {
            sb.append("    .. deprecated::");
            sb.append("        ").append(deprecated);
            String alternative = metadata.get("alternative");
            if (alternative != null) {
                sb.append(" Use ").append(alternative).append(" instead.");
            }
            sb.append("\n");
        }

        sb.append("    \"\"\"");
        return sb.toString();
    }

    // ================================================================
    //  数组操作
    // ================================================================

    @Override
    public String generateArrayCreation(String elementType, String size) {
        return "[" + getNullLiteral() + "] * " + size;
    }

    @Override
    public String generateArrayGet(String arrayName, String index) {
        return arrayName + "[" + index + "]";
    }

    @Override
    public String generateArraySet(String arrayName, String index, String value) {
        return arrayName + "[" + index + "] = " + value;
    }

    // ================================================================
    //  属性访问
    // ================================================================

    @Override
    public String generatePropertyGet(String objectExpr, String fieldName) {
        return objectExpr + "." + fieldName;
    }

    @Override
    public String generatePropertySet(String objectExpr, String fieldName, String valueExpr) {
        return objectExpr + "." + fieldName + " = " + valueExpr;
    }

    // ================================================================
    //  作用域管理
    // ================================================================

    @Override
    public String generateScopeEnter(String scopeName) {
        // Python 作用域通过缩进管理
        return "# --- enter scope: " + scopeName + " ---";
    }

    @Override
    public String generateScopeExit(String scopeName) {
        return "# --- exit scope: " + scopeName + " ---";
    }

    // ================================================================
    //  运行时辅助
    // ================================================================

    @Override
    public List<String> getRequiredImports() {
        List<String> imports = new ArrayList<>();
        imports.add("from __future__ import annotations");
        imports.add("from typing import Any, Final, Optional, List");
        return imports;
    }

    @Override
    public String generateRuntimeHelpers() {
        StringBuilder sb = new StringBuilder();

        sb.append("# ============================================");
        sb.append("# Claw Runtime Helpers for Python");
        sb.append("# Auto-generated - Do not edit");
        sb.append("# ============================================");

        // 1. 属性监听基类
        sb.append("class ClawPropertyMonitor:");
        sb.append("    \"\"\"");
        sb.append("    属性变更监听基类");
        sb.append("    支持 @BeforeProps / @AfterProps 注解功能");
        sb.append("    \"\"\"");
        sb.append("    def __init__(self):");
        sb.append("        self._property_listeners_before: dict[str, list] = {}");
        sb.append("        self._property_listeners_after: dict[str, list] = {}");

        sb.append("    def _register_before_listener(self, property_path: str, callback) -> None:");
        sb.append("        if property_path not in self._property_listeners_before:");
        sb.append("            self._property_listeners_before[property_path] = []");
        sb.append("        self._property_listeners_before[property_path].append(callback)");

        sb.append("    def _register_after_listener(self, property_path: str, callback) -> None:");
        sb.append("        if property_path not in self._property_listeners_after:");
        sb.append("            self._property_listeners_after[property_path] = []");
        sb.append("        self._property_listeners_after[property_path].append(callback)");

        sb.append("    def _before_property_change(self, property_path: str, new_value) -> None:");
        sb.append("        listeners = self._property_listeners_before.get(property_path, [])");
        sb.append("        for listener in listeners:");
        sb.append("            listener(property_path, new_value)");

        sb.append("    def _after_property_change(self, property_path: str, old_value, new_value) -> None:");
        sb.append("        listeners = self._property_listeners_after.get(property_path, [])");
        sb.append("        for listener in listeners:");
        sb.append("            listener(property_path, old_value, new_value)");

        // 2. 业务流转辅助（flow to）
        sb.append("# --- Flow Control Helpers ---");
        sb.append("class FlowJumpException(Exception):");
        sb.append("    \"\"\"用于模拟 Claw flow-to 跳转的异常\"\"\"");
        sb.append("    def __init__(self, target_label: str):");
        sb.append("        self.target_label = target_label");
        sb.append("        super().__init__(f\"Flow jump to {target_label}\")");

        sb.append("def _flow_jump(target_label: str) -> None:");
        sb.append("    \"\"\"触发业务逻辑流转跳转（不记录堆栈）\"\"\"");
        sb.append("    raise FlowJumpException(target_label)");

        sb.append("def _flow_dispatch(handlers: dict, entry_label: str = \"start\") -> Any:");
        sb.append("    \"\"\"");
        sb.append("    业务流转调度器");
        sb.append("    handlers: { label_name: callable }");
        sb.append("    \"\"\"");
        sb.append("    current_label = entry_label");
        sb.append("    while current_label in handlers:");
        sb.append("        try:");
        sb.append("            result = handlers[current_label]()");
        sb.append("            return result");
        sb.append("        except FlowJumpException as fj:");
        sb.append("            current_label = fj.target_label");
        sb.append("    raise RuntimeError(f\"Unknown flow target: {current_label}\")");

        // 3. 构造/析构钩子协议
        sb.append("# --- Lifecycle Hook Protocol ---");
        sb.append("class ClawLifecycle:");
        sb.append("    \"\"\"");
        sb.append("    生命周期钩子基类");
        sb.append("    支持 @BeforeName / @AfterName 注解功能");
        sb.append("    \"\"\"");
        sb.append("    def __init_subclass__(cls, **kwargs):");
        sb.append("        super().__init_subclass__(**kwargs)");
        sb.append("        original_init = cls.__init__ if hasattr(cls, '__init__') else None");
        sb.append("        def wrapped_init(self, *args, **kw):");
        sb.append("            if hasattr(self, '_claw_before_name'):");
        sb.append("                getattr(self, self._claw_before_name)()");
        sb.append("            if original_init:");
        sb.append("                original_init(self, *args, **kw)");
        sb.append("        cls.__init__ = wrapped_init");

        sb.append("    def __del__(self):");
        sb.append("        if hasattr(self, '_claw_after_name'):");
        sb.append("            getattr(self, self._claw_after_name)()");

        return sb.toString();
    }

    // ================================================================
    //  字符串处理
    // ================================================================

    @Override
    public String generateStringLiteral(String value) {
        // 转义内部引号
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    @Override
    public String generateStringConcat(List<String> parts) {
        if (parts == null || parts.isEmpty()) return "\"\"";
        if (parts.size() == 1) return parts.get(0);
        // 使用 f-string 风格或直接拼接
        return String.join(" + ", parts);
    }

    // ================================================================
    //  异常/错误创建
    // ================================================================

    @Override
    public String generateThrow(String exceptionType, String message) {
        String pyType = mapType(exceptionType);
        if ("None".equals(pyType) || pyType.isEmpty()) {
            pyType = "Exception";
        }
        return "raise " + pyType + "(" + message + ")";
    }

    @Override
    public String generateExceptionTypeDefinition(String exceptionName, String parentType) {
        String parent = (parentType != null && !parentType.isEmpty()) ? mapType(parentType) : "Exception";
        StringBuilder sb = new StringBuilder();
        sb.append("class ").append(exceptionName).append("(").append(parent).append("):");
        sb.append("    \"\"\"Claw exception type: ").append(exceptionName).append("\"\"\"");
        sb.append("    def __init__(self, message: str = \"\"):");
        sb.append("        super().__init__(message)");
        sb.append("        self.message = message");
        return sb.toString();
    }

    // ================================================================
    //  辅助方法（内部）
    // ================================================================

    /**
     * 将驼峰命名转换为大写蛇形命名（用于常量）
     */
    private String toUpperSnakeCase(String camelCase) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }
}

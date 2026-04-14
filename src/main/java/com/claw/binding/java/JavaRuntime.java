package com.claw.binding.java;

// import claw.compiler.binding.TargetRuntime;

import java.util.*;
import java.util.stream.Collectors;

import claw.compiler.binding.TargetRuntime;

/**
 * Java 目标语言运行时实现
 * 
 * 将 Claw IR 映射为 Java 源代码。
 * 
 * 类型映射：
 *   Int    -> int / Integer
 *   Float  -> double / Double
 *   String -> String
 *   Bool   -> boolean / Boolean
 *   Void   -> void
 *   Any    -> Object
 * 
 * 特性映射：
 *   @BeforeName  -> 构造函数中调用初始化方法
 *   @AfterName   -> finalize() 或 AutoCloseable.close() 中调用清理方法
 *   @BeforeProps -> PropertyChangeSupport.firePropertyChange 前置拦截
 *   @AfterProps  -> PropertyChangeSupport.firePropertyChange 后置通知
 *   异常流       -> try-catch（Java原生，但按Claw语义简化）
 *   业务流转     -> labeled break / 方法调用链（Java无原生goto）
 */
public class JavaRuntime implements TargetRuntime {
    // ==================== MISSING ABSTRACT METHODS ====================

    @Override
    public String generatePropertySet(String target, String property, String value) {
        // e.g., target.setProperty(value);
        return target + ".set" + capitalize(property) + "(" + value + ");";
    }

    @Override
    public String generatePropertyGet(String target, String property) {
        // e.g., target.getProperty()
        return target + ".get" + capitalize(property) + "()";
    }

    @Override
    public String generateArrayCreation(String elementType, String sizeExpr) {
        // e.g., new int[size]
        return "new " + mapType(elementType) + "[" + sizeExpr + "]";
    }

    @Override
    public String generateArrayGet(String arrayExpr, String indexExpr) {
        // e.g., array[index]
        return arrayExpr + "[" + indexExpr + "]";
    }

    @Override
    public String generateArraySet(String arrayExpr, String indexExpr, String valueExpr) {
        // e.g., array[index] = value;
        return arrayExpr + "[" + indexExpr + "] = " + valueExpr + ";";
    }

    @Override
    public String generateStringConcat(List<String> parts) {
        // e.g., "a" + b + "c"
        if (parts == null || parts.isEmpty()) return "\"\"";
        return String.join(" + ", parts);
    }

    @Override
    public String generateStringLiteral(String value) {
        // Escape backslashes and quotes for Java string literals
        if (value == null) return "\"\"";
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    @Override
    public String generateThrow(String exceptionType, String messageExpr) {
        // e.g., throw new ExceptionType(message);
        return "throw new " + mapType(exceptionType) + "(" + messageExpr + ");";
    }

    @Override
    public String generateExceptionTypeDefinition(String typeName, String visibility) {
        // e.g., public class MyException extends Exception { ... }
        return visibility + " class " + typeName + " extends Exception {\n" +
               "    public " + typeName + "(String message) { super(message); }\n" +
               "}";
    }

    @Override
    public String generateScopeEnter(String label) {
        // e.g., label: {
        return label + ": {";
    }

    @Override
    public String generateScopeExit(String label) {
        // e.g., }
        return "}";
    }

    private static final Map<String, String> TYPE_MAP = new LinkedHashMap<>();
    private static final Map<String, String> BOXED_TYPE_MAP = new LinkedHashMap<>();

    static {
        // 基本类型映射
        TYPE_MAP.put("Int", "int");
        TYPE_MAP.put("Float", "double");
        TYPE_MAP.put("String", "String");
        TYPE_MAP.put("Bool", "boolean");
        TYPE_MAP.put("Void", "void");
        TYPE_MAP.put("Any", "Object");

        // 包装类型映射（用于泛型等场景）
        BOXED_TYPE_MAP.put("Int", "Integer");
        BOXED_TYPE_MAP.put("Float", "Double");
        BOXED_TYPE_MAP.put("String", "String");
        BOXED_TYPE_MAP.put("Bool", "Boolean");
        BOXED_TYPE_MAP.put("Void", "Void");
        BOXED_TYPE_MAP.put("Any", "Object");
    }

    @Override
    public String getLanguageName() {
        return "Java";
    }

    @Override
    public String getFileExtension() {
        return ".java";
    }

    // ==================== 类型映射 ====================

    @Override
    public String mapType(String clawType) {
        if (clawType == null) return "Object";
        String mapped = TYPE_MAP.get(clawType);
        return mapped != null ? mapped : clawType; // 自定义类型直接用原名
    }

    /**
     * 获取包装类型（泛型上下文中使用）
     */
    public String mapBoxedType(String clawType) {
        if (clawType == null) return "Object";
        String mapped = BOXED_TYPE_MAP.get(clawType);
        return mapped != null ? mapped : clawType;
    }

    @Override
    public String getNullLiteral() {
        return "null";
    }

    @Override
    public String getBoolLiteral(boolean value) {
        return value ? "true" : "false";
    }

    // ==================== 内存管理 ====================

    @Override
    public String generateAllocation(String typeName, List<String> constructorArgs) {
        String args = (constructorArgs != null && !constructorArgs.isEmpty())
            ? String.join(", ", constructorArgs)
            : "";
        return "new " + typeName + "(" + args + ")";
    }

    @Override
    public String generateDeallocation(String varName) {
        // Java 有 GC，不需要显式释放
        return "// GC handles deallocation of " + varName;
    }

    @Override
    public boolean requiresExplicitMemoryManagement() {
        return false;
    }

    // ==================== 构造/析构钩子 ====================

    @Override
    public String generateConstructorHook(String methodName, String target) {
        // @BeforeName("initializeUser", "this")
        // 在构造函数体的第一行调用初始化方法
        if ("this".equals(target)) {
            return "this." + methodName + "();";
        }
        return target + "." + methodName + "();";
    }

    @Override
    public String generateDestructorHook(String methodName, String target) {
        // @AfterName("cleanupUser", "this")
        // 在 close() 方法中调用（实现 AutoCloseable）
        StringBuilder sb = new StringBuilder();
        sb.append("@Override");
        sb.append("public void close() {");
        if ("this".equals(target)) {
            sb.append("    this.").append(methodName).append("();");
        } else {
            sb.append("    ").append(target).append(".").append(methodName).append("();");
        }
        sb.append("}");
        return sb.toString();
    }

    // ==================== 属性监听钩子 ====================

    @Override
    public String generateBeforePropsHook(String propertyPath, String newValue) {
        // @BeforeProps 触发：属性变更前回调
        return "this.__beforePropertyChange(\"" + propertyPath + "\", " + newValue + ");";
    }

    @Override
    public String generateAfterPropsHook(String propertyPath,
                                          String oldValue, String newValue) {
        // @AfterProps 触发：属性变更后回调
        return "this.__afterPropertyChange(\"" + propertyPath + "\", " +
               oldValue + ", " + newValue + ");";
    }

    @Override
    public String generateMonitoredPropertySet(String propertyPath, String newValueExpr,
                                                boolean hasBefore, boolean hasAfter) {
        StringBuilder sb = new StringBuilder();

        // 拆分属性路径: "user.age" -> target="user", field="age"
        String[] parts = propertyPath.split("\\.");
        String target = parts.length > 1 ? parts[0] : "this";
        String field = parts[parts.length - 1];
        String getter = target + ".get" + capitalize(field) + "()";
        String setter = target + ".set" + capitalize(field);

        if (hasBefore) {
            sb.append(generateBeforePropsHook(propertyPath, newValueExpr)).append(" ");
        }

        // 保存旧值（如果有 after hook）
        if (hasAfter) {
            String oldVarName = "__old_" + field;
            String fieldType = "Object"; // 运行时确定类型
            sb.append(fieldType).append(" ").append(oldVarName)
              .append(" = ").append(getter).append(";");
            sb.append(setter).append("(").append(newValueExpr).append(");");
            sb.append(generateAfterPropsHook(propertyPath, oldVarName, newValueExpr));
        } else {
            sb.append(setter).append("(").append(newValueExpr).append(");");
        }

        return sb.toString();
    }

    // ==================== 三层操作流 ====================

    @Override
    public String generateCatchBlock(String exceptionType, String varName, String handlerBody) {
        // Claw异常流：去掉try和{}，保留catch
        // 但Java语法要求try-catch配对，所以生成完整结构，编译器自动包裹try
        StringBuilder sb = new StringBuilder();
        sb.append("catch (").append(mapType(exceptionType)).append(" ").append(varName).append(") {");
        sb.append("    ").append(handlerBody).append(" ");
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String generateThrowsDeclaration(List<String> exceptionTypes) {
        if (exceptionTypes == null || exceptionTypes.isEmpty()) return "";
        String types = exceptionTypes.stream()
            .map(this::mapType)
            .collect(Collectors.joining(", "));
        return " throws " + types;
    }

    @Override
    public String generateThrowStatement(String exceptionType, String messageExpr) {
        // e.g., throw new ExceptionType(message);
        return "throw new " + mapType(exceptionType) + "(" + messageExpr + ");";
    }

    @Override
    public String generateFlowTo(String targetLabel) {
        // Java没有goto，用labeled break模拟业务逻辑流转
        // flow to target -> break targetLabel;
        // 编译器需要在外层包裹 targetLabel: { ... }
        return "break " + targetLabel + "; // flow to " + targetLabel;
    }

    @Override
    public boolean supportsNativeFlowJump() {
        return false; // Java不支持原生goto
    }

    // ==================== 函数生成 ====================

    @Override
    public String generateFunctionHeader(String visibility, String returnType,
                                          String funcName, List<Map.Entry<String, String>> params,
                                          List<String> throwsTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(visibility).append(" ");
        sb.append(mapType(returnType)).append(" ");
        sb.append(funcName).append("(");

        if (params != null && !params.isEmpty()) {
            sb.append(params.stream()
                .map(p -> mapType(p.getValue()) + " " + p.getKey())
                .collect(Collectors.joining(", ")));
        }

        sb.append(")");

        // throws 声明
        sb.append(generateThrowsDeclaration(throwsTypes));

        sb.append(" {");
        return sb.toString();
    }

    @Override
    public String generateFunctionFooter() {
        return "}";
    }

    @Override
    public String generateFunctionCall(String funcName, List<String> args) {
        String argStr = (args != null) ? String.join(", ", args) : "";
        return funcName + "(" + argStr + ")";
    }

    @Override
    public String generateFunctionCallWithAssignment(String resultVar, String funcName, List<String> args) {
        String argStr = (args != null) ? String.join(", ", args) : "";
        return resultVar + " = " + funcName + "(" + argStr + ")";
    }

    @Override
    public String generateReturn(String expression) {
        if (expression == null || expression.isEmpty()) {
            return "return;";
        }
        return "return " + expression + ";";
    }

    // ==================== 变量声明 ====================

    @Override
    public String generateVariableDeclaration(boolean isConst, String typeName,
                                               String varName, String initExpr) {
        StringBuilder sb = new StringBuilder();
        if (isConst) {
            sb.append("final ");
        }
        sb.append(mapType(typeName)).append(" ").append(varName);
        if (initExpr != null) {
            sb.append(" = ").append(initExpr);
        }
        sb.append(";");
        return sb.toString();
    }

    // ==================== 控制流 ====================

    @Override
    public String generateIf(String condition) {
        return "if (" + condition + ") {";
    }

    @Override
    public String generateElse() {
        return "} else {";
    }

    @Override
    public String generateElseIf(String condition) {
        return "} else if (" + condition + ") {";
    }

    @Override
    public String generateWhile(String condition) {
        return "while (" + condition + ") {";
    }

    @Override
    public String generateFor(String varName, String iterable) {
        return "for (var " + varName + " : " + iterable + ") {";
    }

    @Override
    public String generateBreak() {
        return "break;";
    }

    @Override
    public String generateContinue() {
        return "continue;";
    }

    // ==================== 块界定符 ====================

    @Override
    public String getBlockOpen() {
        return "{";
    }

    @Override
    public String getBlockClose() {
        return "}";
    }

    @Override
    public String getStatementTerminator() {
        return ";";
    }

    // ==================== 导入/导出 ====================

    @Override
    public String generateImport(String modulePath, String symbolName) {
        if (symbolName != null && !symbolName.isEmpty()) {
            return "import " + modulePath + "." + symbolName + ";";
        }
        return "import " + modulePath + ".*;";
    }

    @Override
    public String generateExport(String symbolName) {
        // Java使用public关键字控制可见性，无需额外export语句
        return "// exported: " + symbolName + " (controlled by visibility modifier)";
    }

    // ==================== 类型定义 ====================

    @Override
    public String generateTypeDefinitionHeader(String typeName, String visibility) {
        return visibility + " class " + typeName + " implements AutoCloseable {";
    }

    @Override
    public String generateTypeField(String fieldName, String fieldType, String visibility) {
        return "    " + visibility + " " + mapType(fieldType) + " " + fieldName + ";";
    }

    @Override
    public String generateTypeDefinitionFooter() {
        return "}";
    }

    // ==================== 运算符 ====================

    @Override
    public String generateBinaryExpression(String left, String operator, String right) {
        // 字符串拼接特殊处理
        if ("+".equals(operator)) {
            return left + " + " + right; // Java原生支持字符串+拼接
        }
        return left + " " + operator + " " + right;
    }

    @Override
    public String generateUnaryExpression(String operator, String operand) {
        return operator + operand;
    }

    // ==================== 注释 ====================

    @Override
    public String generateComment(String comment) {
        return "// " + comment;
    }

    @Override
    public String generateDocComment(Map<String, String> metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("/**");

        String desc = metadata.get("description");
        if (desc != null) {
            sb.append(" * ").append(desc).append("");
            sb.append(" * ");
        }

        String ioSpec = metadata.get("io_spec");
        if (ioSpec != null) {
            sb.append(" * IO: ").append(ioSpec).append(" ");
        }

        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if ("description".equals(key) || "io_spec".equals(key)) continue;

            switch (key) {
                case "param":
                    sb.append(" * @param ").append(entry.getValue()).append(" ");
                    break;
                case "return":
                    sb.append(" * @return ").append(entry.getValue()).append(" ");
                    break;
                case "example":
                    sb.append(" * @example ").append(entry.getValue()).append(" ");
                    break;
                case "deprecated":
                    sb.append(" * @deprecated ").append(entry.getValue()).append(" ");
                    break;
                default:
                    sb.append(" * @").append(key).append(" ").append(entry.getValue()).append(" ");
                    break;
            }
        }

        sb.append(" */");
        return sb.toString();
    }

    // ==================== 运行时辅助 ====================

    @Override
    public List<String> getRequiredImports() {
        List<String> imports = new ArrayList<>();
        imports.add("java.beans.PropertyChangeSupport");
        imports.add("java.beans.PropertyChangeListener");
        imports.add("java.util.*");
        return imports;
    }

    @Override
    public String generateRuntimeHelpers() {
        StringBuilder sb = new StringBuilder();
        sb.append("// ===== Claw Runtime Helpers for Java =====");

        // 属性变更监听基类
        sb.append("/**");
        sb.append(" * Claw属性监听基类");
        sb.append(" * 支持 @BeforeProps 和 @AfterProps 注解的运行时行为");
        sb.append(" */");
        sb.append("abstract class ClawPropertySupport implements AutoCloseable {");
        sb.append("    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);");
        sb.append("    private final Map<String, List<BeforePropsHandler>> beforeHandlers = new HashMap<>();");
        sb.append("    private final Map<String, List<AfterPropsHandler>> afterHandlers = new HashMap<>();");
        sb.append(" ");
        sb.append("    @FunctionalInterface");
        sb.append("    public interface BeforePropsHandler {");
        sb.append("        void onBeforeChange(String propertyPath, Object newValue);");
        sb.append("    }");
        sb.append("    @FunctionalInterface");
        sb.append("    public interface AfterPropsHandler {");
        sb.append("        void onAfterChange(String propertyPath, Object oldValue, Object newValue);");
        sb.append("    }");
        sb.append("    protected void registerBeforeProps(String propertyPath, BeforePropsHandler handler) {");
        sb.append("        beforeHandlers.computeIfAbsent(propertyPath, k -> new ArrayList<>()).add(handler);");
        sb.append("    }");
        sb.append("    protected void registerAfterProps(String propertyPath, AfterPropsHandler handler) {");
        sb.append("        afterHandlers.computeIfAbsent(propertyPath, k -> new ArrayList<>()).add(handler);");
        sb.append("    }");
        sb.append("    protected void __beforePropertyChange(String propertyPath, Object newValue) {");
        sb.append("        List<BeforePropsHandler> handlers = beforeHandlers.get(propertyPath);");
        sb.append("        if (handlers != null) {");
        sb.append("            for (BeforePropsHandler h : handlers) {");
        sb.append("                h.onBeforeChange(propertyPath, newValue);");
        sb.append("            }");
        sb.append("        }");
        sb.append("    }");
        sb.append("    protected void __afterPropertyChange(String propertyPath, Object oldValue, Object newValue) {");
        sb.append("        List<AfterPropsHandler> handlers = afterHandlers.get(propertyPath);");
        sb.append("        if (handlers != null) {");
        sb.append("            for (AfterPropsHandler h : handlers) {");
        sb.append("                h.onAfterChange(propertyPath, oldValue, newValue);");
        sb.append("            }");
        sb.append("        }");
        sb.append("        pcs.firePropertyChange(propertyPath, oldValue, newValue);");
        sb.append("    }");
        sb.append("    public void addPropertyChangeListener(PropertyChangeListener listener) {");
        sb.append("        pcs.addPropertyChangeListener(listener);");
        sb.append("    }");
        sb.append("    public void removePropertyChangeListener(PropertyChangeListener listener) {");
        sb.append("        pcs.removePropertyChangeListener(listener);");
        sb.append("    }");
        sb.append("}");

        return sb.toString();
    }

    // ==================== 私有工具方法 ====================

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}


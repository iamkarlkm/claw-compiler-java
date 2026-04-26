package com.q3lives.binding.java;

// import com.q3lives.binding.TargetRuntime;

import java.util.*;
import java.util.stream.Collectors;

import com.q3lives.binding.TargetRuntime;

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

  
    public String generatePropertySet(String target, String property, String value) {
        // e.g., target.setProperty(value);
        return target + ".set" + capitalize(property) + "(" + value + ");";
    }

  
    public String generatePropertyGet(String target, String property) {
        // e.g., target.getProperty()
        return target + ".get" + capitalize(property) + "()";
    }

  
    public String generateArrayCreation(String elementType, String sizeExpr) {
        // e.g., new int[size]
        return "new " + mapType(elementType) + "[" + sizeExpr + "]";
    }

  
    public String generateArrayGet(String arrayExpr, String indexExpr) {
        // e.g., array[index]
        return arrayExpr + "[" + indexExpr + "]";
    }

  
    public String generateArraySet(String arrayExpr, String indexExpr, String valueExpr) {
        // e.g., array[index] = value;
        return arrayExpr + "[" + indexExpr + "] = " + valueExpr + ";";
    }

  
    public String generateStringConcat(List<String> parts) {
        // e.g., "a" + b + "c"
        if (parts == null || parts.isEmpty()) return "\"\"";
        return String.join(" + ", parts);
    }

  
    public String generateStringLiteral(String value) {
        // Escape backslashes and quotes for Java string literals
        if (value == null) return "\"\"";
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

  
    public String generateThrow(String exceptionType, String messageExpr) {
        // e.g., throw new ExceptionType(message);
        return "throw new " + mapType(exceptionType) + "(" + messageExpr + ");";
    }

  
    public String generateExceptionTypeDefinition(String typeName, String visibility) {
        // e.g., public class MyException extends Exception { ... }
        return visibility + " class " + typeName + " extends Exception {\n" +
               "    public " + typeName + "(String message) { super(message); }\n" +
               "}";
    }

    // ================================================================
    //  装饰器支持
    // ================================================================

    public String generateDecorator(String decoratorName) {
        // Java 装饰器使用 @ 符号前缀
        return "@" + decoratorName;
    }

    public String generateClassDecorator(String decoratorName) {
        // Java 类装饰器与函数装饰器语法相同
        return "@" + decoratorName;
    }

    // ================================================================
    //  Lambda 表达式支持
    // ================================================================

    public String generateLambda(String lambdaName, String params, String body) {
        // Java Lambda: FunctionalInterface<T, R> lambdaName = (params) -> body;
        // 假设从参数列表推断函数接口类型
        if (params != null && !params.isEmpty()) {
            return lambdaName + " = (" + params + ") -> " + body + ";";
        } else {
            return lambdaName + " = () -> " + body + ";";
        }
    }

    public String generateLambdaCall(String lambdaName, List<String> args) {
        // Java Lambda 调用: lambdaName.apply(args)
        String argStr = (args != null) ? String.join(", ", args) : "";
        return lambdaName + ".apply(" + argStr + ")";
    }

  
    public String generateScopeEnter(String label) {
        // e.g., label: {
        return label + ": {";
    }

  
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

  
    public String getLanguageName() {
        return "Java";
    }

  
    public String getFileExtension() {
        return ".java";
    }

    // ==================== 类型映射 ====================

  
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

  
    public String getNullLiteral() {
        return "null";
    }

  
    public String getBoolLiteral(boolean value) {
        return value ? "true" : "false";
    }

    // ==================== 内存管理 ====================

  
    public String generateAllocation(String typeName, List<String> constructorArgs) {
        String args = (constructorArgs != null && !constructorArgs.isEmpty())
            ? String.join(", ", constructorArgs)
            : "";
        return "new " + typeName + "(" + args + ")";
    }

  
    public String generateDeallocation(String varName) {
        // Java 有 GC，不需要显式释放
        return "// GC handles deallocation of " + varName;
    }

  
    public boolean requiresExplicitMemoryManagement() {
        return false;
    }

    // ==================== 构造/析构钩子 ====================

  
    public String generateConstructorHook(String methodName, String target) {
        // @BeforeName("initializeUser", "this")
        // 在构造函数体的第一行调用初始化方法
        if ("this".equals(target)) {
            return "this." + methodName + "();";
        }
        return target + "." + methodName + "();";
    }

  
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

  
    public String generateBeforePropsHook(String propertyPath, String newValue) {
        // @BeforeProps 触发：属性变更前回调
        return "this.__beforePropertyChange(\"" + propertyPath + "\", " + newValue + ");";
    }

  
    public String generateAfterPropsHook(String propertyPath,
                                          String oldValue, String newValue) {
        // @AfterProps 触发：属性变更后回调
        return "this.__afterPropertyChange(\"" + propertyPath + "\", " +
               oldValue + ", " + newValue + ");";
    }

  
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

  
    public String generateCatchBlock(String exceptionType, String varName, String handlerBody) {
        // Claw异常流：去掉try和{}，保留catch
        // 但Java语法要求try-catch配对，所以生成完整结构，编译器自动包裹try
        StringBuilder sb = new StringBuilder();
        sb.append("catch (").append(mapType(exceptionType)).append(" ").append(varName).append(") {");
        sb.append("    ").append(handlerBody).append(" ");
        sb.append("}");
        return sb.toString();
    }

  
    public String generateThrowsDeclaration(List<String> exceptionTypes) {
        if (exceptionTypes == null || exceptionTypes.isEmpty()) return "";
        String types = exceptionTypes.stream()
            .map(this::mapType)
            .collect(Collectors.joining(", "));
        return " throws " + types;
    }

  
    public String generateThrowStatement(String exceptionType, String messageExpr) {
        // e.g., throw new ExceptionType(message);
        return "throw new " + mapType(exceptionType) + "(" + messageExpr + ");";
    }

  
    public String generateFlowTo(String targetLabel) {
        // Java没有goto，用labeled break模拟业务逻辑流转
        // flow to target -> break targetLabel;
        // 编译器需要在外层包裹 targetLabel: { ... }
        return "break " + targetLabel + "; // flow to " + targetLabel;
    }

  
    public boolean supportsNativeFlowJump() {
        return false; // Java不支持原生goto
    }

    // ==================== 函数生成 ====================

  
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

  
    public String generateFunctionFooter() {
        return "}";
    }

  
    public String generateFunctionCall(String funcName, List<String> args) {
        String argStr = (args != null) ? String.join(", ", args) : "";
        return funcName + "(" + argStr + ")";
    }

  
    public String generateFunctionCallWithAssignment(String resultVar, String funcName, List<String> args) {
        String argStr = (args != null) ? String.join(", ", args) : "";
        return resultVar + " = " + funcName + "(" + argStr + ")";
    }

  
    public String generateReturn(String expression) {
        if (expression == null || expression.isEmpty()) {
            return "return;";
        }
        return "return " + expression + ";";
    }

    // ==================== 变量声明 ====================

  
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

  
    public String generateIf(String condition) {
        return "if (" + condition + ") {";
    }

  
    public String generateElse() {
        return "} else {";
    }

  
    public String generateElseIf(String condition) {
        return "} else if (" + condition + ") {";
    }

  
    public String generateWhile(String condition) {
        return "while (" + condition + ") {";
    }

  
    public String generateFor(String varName, String iterable) {
        return "for (var " + varName + " : " + iterable + ") {";
    }

  
    public String generateBreak() {
        return "break;";
    }

  
    public String generateContinue() {
        return "continue;";
    }

    // ==================== 块界定符 ====================

  
    public String getBlockOpen() {
        return "{";
    }

  
    public String getBlockClose() {
        return "}";
    }

  
    public String getStatementTerminator() {
        return ";";
    }

    // ==================== 导入/导出 ====================

  
    public String generateImport(String modulePath, String symbolName) {
        // 标准库映射：Claw 模块名 -> Java 包名
        String javaModule = mapStdLibraryToJava(modulePath);
        if (symbolName != null && !symbolName.isEmpty()) {
            return "import " + javaModule + "." + symbolName + ";";
        }
        return "import " + javaModule + ".*;";
    }

    /**
     * 将 Claw 标准库模块名映射到 Java 包名。
     */
    private String mapStdLibraryToJava(String modulePath) {
        return switch (modulePath) {
            case "std.io"        -> "java.io";
            case "std.string"    -> "java.lang"; // String 在 java.lang
            case "std.math"      -> "java.lang.Math";
            case "std.memory"    -> "java.lang.ref";
            case "std.time"      -> "java.time";
            case "std.collections" -> "java.util";
            case "std.net"       -> "java.net";
            case "std.json"      -> "com.fasterxml.jackson.databind"; // 常用 JSON 库
            case "std.database"  -> "java.sql";
            case "std.concurrent"-> "java.util.concurrent";
            case "std.regex"     -> "java.util.regex";
            case "std.stream"    -> "java.util.stream";
            default              -> modulePath;
        };
    }

  
    public String generateExport(String symbolName) {
        // Java使用public关键字控制可见性，无需额外export语句
        return "// exported: " + symbolName + " (controlled by visibility modifier)";
    }

    // ==================== 类型定义 ====================

  
    public String generateTypeDefinitionHeader(String typeName, String visibility) {
        return visibility + " class " + typeName + " implements AutoCloseable {";
    }

  
    public String generateTypeField(String fieldName, String fieldType, String visibility) {
        return "    " + visibility + " " + mapType(fieldType) + " " + fieldName + ";";
    }

  
    public String generateTypeDefinitionFooter() {
        return "}";
    }

    // ==================== 运算符 ====================

  
    public String generateBinaryExpression(String left, String operator, String right) {
        // 字符串拼接特殊处理
        if ("+".equals(operator)) {
            return left + " + " + right; // Java原生支持字符串+拼接
        }
        return left + " " + operator + " " + right;
    }

  
    public String generateUnaryExpression(String operator, String operand) {
        return operator + operand;
    }

    // ==================== 注释 ====================

  
    public String generateComment(String comment) {
        return "// " + comment;
    }

  
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

  
    public List<String> getRequiredImports() {
        List<String> imports = new ArrayList<>();
        imports.add("java.beans.PropertyChangeSupport");
        imports.add("java.beans.PropertyChangeListener");
        imports.add("java.util.*");
        return imports;
    }

  
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

    // ================================================================
    //  类型注解增强 (类型检查与转换)
    // ================================================================

  
    public String generateTypeCheck(String valueExpr, String expectedType) {
        // Java 类型检查: if (!(valueExpr instanceof ExpectedType)) throw new ClassCastException(...)
        String expectedTypeName = mapType(expectedType);
        return "if (!(" + valueExpr + " instanceof " + expectedTypeName + ")) {" +
               "    throw new ClassCastException(\"Expected " + expectedTypeName + ", got \" + " + valueExpr + ".getClass().getSimpleName());" +
               "}";

    }

  
    public String generateTypeCast(String valueExpr, String targetType) {
        // Java 类型转换: ExpectedType.cast(valueExpr)
        String targetTypeName = mapType(targetType);
        return targetTypeName + ".cast(" + valueExpr + ")";
    }

  
    public String generateTypeIs(String valueExpr, String typeName) {
        // Java 类型判断: valueExpr instanceof TypeName
        String typeNameStr = mapType(typeName);
        return valueExpr + " instanceof " + typeNameStr;
    }

  
    public String generateTypeCheckAndCast(String valueExpr, String targetType) {
        // Java 类型检查并转换: ExpectedType.cast(valueExpr)
        String targetTypeName = mapType(targetType);
        return targetTypeName + ".cast(" + valueExpr + ")";
    }

  
    public String generateExplicitTypeCast(String valueExpr, String targetType, boolean useCheckedCast) {
        // Java 显式类型转换: ((TypeName)valueExpr).method()
        String targetTypeName = mapType(targetType);
        if (useCheckedCast) {
            // 使用 Checked Cast (如 Number.intValue())
            if ("int".equals(targetTypeName)) {
                return "(" + targetTypeName + ") " + valueExpr + ".intValue()";
            } else if ("float".equals(targetTypeName)) {
                return "(" + targetTypeName + ") " + valueExpr + ".doubleValue()";
            } else if ("boolean".equals(targetTypeName)) {
                return "(" + targetTypeName + ") " + valueExpr + ".booleanValue()";
            }
        }
        // 强制类型转换
        return "(" + targetTypeName + ") " + valueExpr;
    }

    // ================================================================
    //  AOP (Aspect-Oriented Programming) 支持
    // ================================================================

  
    public String generateAspectDefinition(String aspectName) {
        // Java 使用 @Aspect 注解 + @Before/@After/@Around 注解定义切面
        return "@Aspect\n" +
               "public class " + aspectName + " {\n" +
               "    // Aspect defined here\n" +
               "}";
    }

  
    public String generateJoinPointCreate(String joinPointName, String methodName, String args) {
        // Java: 使用静态工厂方法创建 JoinPoint
        return joinPointName + " = JoinPoint.create(\"" + methodName + "\", " + args + ");";
    }

  
    public String generateBeforeAdvice(String adviceName, String targetName) {
        // Java: 使用 @Before 注解 + JoinPoint 参数
        return "@Before(\"execution(* " + targetName + "(..))\")\n" +
               "public void " + adviceName + "(JoinPoint jp) {\n" +
               "    System.out.println(\"[BEFORE] \" + jp.getSignature().getName());\n" +
               "}";
    }

  
    public String generateAfterAdvice(String adviceName, String targetName) {
        // Java: 使用 @After 注解 + JoinPoint 参数
        return "@After(\"execution(* " + targetName + "(..))\")\n" +
               "public void " + adviceName + "(JoinPoint jp) {\n" +
               "    System.out.println(\"[AFTER] \" + jp.getSignature().getName());\n" +
               "}";
    }

  
    public String generateAfterReturningAdvice(String adviceName, String targetName, String returnVar) {
        // Java: 使用 @AfterReturning 注解
        return "@AfterReturning(value = \"execution(* " + targetName + "(..))\", returning = \"" + returnVar + "\")\n" +
               "public void " + adviceName + "(JoinPoint jp, " + returnVar + " returnValue) {\n" +
               "    System.out.println(\"[AFTER RETURNING] \" + jp.getSignature().getName() + \": \" + returnValue);\n" +
               "}";
    }

  
    public String generateAfterThrowingAdvice(String adviceName, String targetName, String exceptionVar) {
        // Java: 使用 @AfterThrowing 注解
        return "@AfterThrowing(value = \"execution(* " + targetName + "(..))\", throwing = \"" + exceptionVar + "\")\n" +
               "public void " + adviceName + "(JoinPoint jp, " + exceptionVar + " ex) {\n" +
               "    System.out.println(\"[AFTER THROWING] \" + jp.getSignature().getName() + \": \" + ex.getMessage());\n" +
               "}";
    }

  
    public String generateAroundAdvice(String adviceName, String targetName) {
        // Java: 使用 @Around 注解 + ProceedingJoinPoint 参数
        return "@Around(\"execution(* " + targetName + "(..))\")\n" +
               "public Object " + adviceName + "(ProceedingJoinPoint jp) throws Throwable {\n" +
               "    System.out.println(\"[BEGIN AROUND] \" + jp.getSignature().getName());\n" +
               "    Object result = jp.proceed();\n" +
               "    System.out.println(\"[END AROUND] \" + jp.getSignature().getName());\n" +
               "    return result;\n" +
               "}";
    }

  
    public String generateJoinPointMethodName(String joinPointName) {
        // Java: 调用 JoinPoint 的 getMethodName() 方法
        return joinPointName + ".getMethodName()";
    }

  
    public String generateJoinPointArgs(String joinPointName) {
        // Java: 调用 JoinPoint 的 getArgs() 方法
        return joinPointName + ".getArgs()";
    }

  
    public String generateJoinPointTarget(String joinPointName) {
        // Java: 调用 JoinPoint 的 getTarget() 方法
        return joinPointName + ".getTarget()";
    }

  
    public String generateMethodInvocation(String methodName, List<String> args) {
        // Java: 方法调用（AOP 拦截时使用）
        String argStr = (args != null) ? String.join(", ", args) : "";
        return methodName + "(" + argStr + ")";
    }

  
    public String generateAdviceProceed(String joinPointName) {
        // Java: ProceedingJoinPoint 的 proceed() 方法
        return joinPointName + ".proceed()";
    }

  
    public String generateJoinPointSupport() {
        // Java: JoinPoint 类定义和辅助方法
        StringBuilder sb = new StringBuilder();

        sb.append("// ============================================\n");
        sb.append("// AOP Support for Java\n");
        sb.append("// ============================================\n\n");

        // JoinPoint 类
        sb.append("package com.claw.binding.aop;\n\n");
        sb.append("import org.aspectj.lang.JoinPoint;\n");
        sb.append("import org.aspectj.lang.reflect.MethodSignature;\n\n");
        sb.append("public class JoinPoint {\n");
        sb.append("    private final String methodName;\n");
        sb.append("    private final Object[] args;\n");
        sb.append("    private final Object target;\n");
        sb.append("    private final Object instance;\n\n");

        sb.append("    private JoinPoint(String methodName, Object[] args, Object target, Object instance) {\n");
        sb.append("        this.methodName = methodName;\n");
        sb.append("        this.args = args;\n");
        sb.append("        this.target = target;\n");
        sb.append("        this.instance = instance;\n");
        sb.append("    }\n\n");

        sb.append("    public static JoinPoint create(String methodName, Object[] args) {\n");
        sb.append("        return new JoinPoint(methodName, args, null, null);\n");
        sb.append("    }\n\n");

        sb.append("    public static JoinPoint create(String methodName, Object[] args, Object target, Object instance) {\n");
        sb.append("        return new JoinPoint(methodName, args, target, instance);\n");
        sb.append("    }\n\n");

        sb.append("    public String getMethodName() {\n");
        sb.append("        return methodName;\n");
        sb.append("    }\n\n");

        sb.append("    public Object[] getArgs() {\n");
        sb.append("        return args;\n");
        sb.append("    }\n\n");

        sb.append("    public Object getTarget() {\n");
        sb.append("        return target;\n");
        sb.append("    }\n\n");

        sb.append("    public Object getInstance() {\n");
        sb.append("        return instance;\n");
        sb.append("    }\n\n");

        sb.append("    /**\n");
        sb.append("     * 执行目标方法（Around 通知中使用）\n");
        sb.append("     * 通过反射调用被拦截的方法\n");
        sb.append("     */\n");
        sb.append("    public Object proceed() throws Throwable {\n");
        sb.append("        if (target == null || methodName == null) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("        Object[] actualArgs = args != null ? args : new Object[0];\n");
        sb.append("        Class<?>[] argTypes = new Class[actualArgs.length];\n");
        sb.append("        for (int i = 0; i < actualArgs.length; i++) {\n");
        sb.append("            argTypes[i] = (actualArgs[i] != null) ? actualArgs[i].getClass() : Object.class;\n");
        sb.append("        }\n");
        sb.append("        try {\n");
        sb.append("            java.lang.reflect.Method method = target.getClass().getMethod(methodName, argTypes);\n");
        sb.append("            method.setAccessible(true);\n");
        sb.append("            return method.invoke(target, actualArgs);\n");
        sb.append("        } catch (NoSuchMethodException e) {\n");
        sb.append("            for (java.lang.reflect.Method m : target.getClass().getMethods()) {\n");
        sb.append("                if (m.getName().equals(methodName)) {\n");
        sb.append("                    m.setAccessible(true);\n");
        sb.append("                    return m.invoke(target, actualArgs);\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("            throw new RuntimeException(\"Method not found: \" + methodName, e);\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n\n");

        // ProceedingJoinPoint 类（继承自 JoinPoint，用于 Around 通知）
        sb.append("public class ProceedingJoinPoint extends JoinPoint {\n");
        sb.append("    public ProceedingJoinPoint(String methodName, Object[] args, Object target, Object instance) {\n");
        sb.append("        super(methodName, args, target, instance);\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    // ==================== AOP 织入支持 ====================

    @Override
    public boolean supportsAOP() {
        return true;
    }

    @Override
    public String generateBeforeAdviceCall(String adviceName, String joinPointName) {
        return adviceName + "(" + joinPointName + ");";
    }

    @Override
    public String generateAfterAdviceCall(String adviceName, String joinPointName) {
        return adviceName + "(" + joinPointName + ");";
    }

    @Override
    public String generateAroundAdviceBegin(String adviceName, String joinPointName) {
        return adviceName + "(" + joinPointName + ");";
    }

    @Override
    public String generateAroundAdviceEnd(String adviceName, String joinPointName, String resultVar) {
        return adviceName + "(" + joinPointName + ", " + resultVar + ");";
    }

    @Override
    public String generateJoinPointCreate(String joinPointName, String methodName,
                                           String argsExpr, String targetExpr) {
        return "JoinPoint " + joinPointName + " = JoinPoint.create(\"" + methodName + "\", " + argsExpr + ", " + targetExpr + ", " + targetExpr + ");";
    }

    // ==================== 私有工具方法 ====================

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}


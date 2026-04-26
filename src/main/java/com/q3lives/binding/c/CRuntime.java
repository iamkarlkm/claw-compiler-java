package com.q3lives.binding.c;



import com.q3lives.binding.TargetRuntime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * C 目标语言运行时实现
 * 
 * 将 Claw IR 映射为 C11 源代码。
 * 
 * 映射策略：
 *   - 类型系统：静态类型，Claw类型 -> C原生类型
 *   - 内存管理：malloc/free 显式管理（requiresExplicitMemoryManagement = true）
 *   - 异常流：setjmp/longjmp 模拟（C无原生异常）
 *   - 业务流转：goto + label（C原生支持！）
 *   - 属性监听：函数指针回调表
 *   - 构造/析构：手动调用 init/cleanup 函数
 *   - 类型定义：struct + 函数指针（模拟方法）
 *   - 块界定：花括号 { }
 *   - 字符串：char* / const char*
 *   - 布尔：stdbool.h 的 true/false
 */
public class CRuntime implements TargetRuntime {

    // ================================================================
    //  基础信息
    // ================================================================

    @Override
    public String getLanguageName() {
        return "C";
    }

    @Override
    public String getFileExtension() {
        return ".c";
    }

    // ================================================================
    //  类型映射
    // ================================================================

    @Override
    public String mapType(String clawType) {
        if (clawType == null) return "void*";
        switch (clawType) {
            case "Int":    return "int";
            case "Float":  return "double";
            case "String": return "const char*";
            case "Bool":   return "bool";
            case "Void":   return "void";
            case "Any":    return "void*";
            default:       return clawType + "*"; // 自定义类型映射为指针
        }
    }

    /**
     * 获取值类型（非指针，用于 struct 字段声明）
     */
    public String mapValueType(String clawType) {
        if (clawType == null) return "void*";
        switch (clawType) {
            case "Int":    return "int";
            case "Float":  return "double";
            case "String": return "const char*";
            case "Bool":   return "bool";
            case "Void":   return "void";
            case "Any":    return "void*";
            default:       return "struct " + clawType; // struct 值类型
        }
    }

    /**
     * 获取指针类型
     */
    public String mapPointerType(String clawType) {
        if (clawType == null) return "void*";
        switch (clawType) {
            case "Int":    return "int*";
            case "Float":  return "double*";
            case "String": return "char*"; // 可变字符串用 char*
            case "Bool":   return "bool*";
            case "Void":   return "void*";
            case "Any":    return "void*";
            default:       return clawType + "*";
        }
    }

    @Override
    public String mapBoxedType(String clawType) {
        // C 没有包装类型，都是指针
        return mapPointerType(clawType);
    }

    @Override
    public String getNullLiteral() {
        return "NULL";
    }

    @Override
    public String getBoolLiteral(boolean value) {
        return value ? "true" : "false";
    }

    // ================================================================
    //  内存管理（C 的核心特性）
    // ================================================================

    @Override
    public String generateAllocation(String typeName, List<String> constructorArgs) {
        // 基本类型不需要 malloc
        switch (typeName) {
            case "Int": case "Float": case "Bool":
                return "0"; // 默认值
            case "String":
                return "NULL";
            default:
                // 自定义类型：malloc + 构造函数调用
                return "(" + typeName + "*)malloc(sizeof(" + typeName + "))";
        }
    }

    @Override
    public String generateDeallocation(String varName) {
        return "free(" + varName + ");" + varName + " = NULL;";
    }

    @Override
    public boolean requiresExplicitMemoryManagement() {
        return true; // C 必须显式管理内存
    }

    /**
     * 生成带析构函数的安全释放
     */
    public String generateSafeDeallocation(String varName, String typeName, String destructorName) {
        StringBuilder sb = new StringBuilder();
        sb.append("if (").append(varName).append(" != NULL) {");
        if (destructorName != null) {
            sb.append("    ").append(destructorName).append("(").append(varName).append(");");
        }
        sb.append("    free(").append(varName).append(");");
        sb.append("    ").append(varName).append(" = NULL;");
        sb.append("}");
        return sb.toString();
    }

    // ================================================================
    //  构造/析构函数钩子
    // ================================================================

    @Override
    public String generateConstructorHook(String methodName, String target) {
        // C 中构造函数就是普通函数调用
        // @BeforeName("initializeUser", "this")
        // -> claw_initializeUser(self);
        if ("this".equals(target) || "self".equals(target)) {
            return methodName + "(self);";
        }
        return methodName + "(" + target + ");";
    }

    @Override
    public String generateDestructorHook(String methodName, String target) {
        // C 中析构函数也是普通函数调用，在 free 之前调用
        StringBuilder sb = new StringBuilder();
        sb.append("/* Destructor: @AfterName */");
        sb.append("void __claw_destructor_").append(methodName).append("(void* obj) {");
        if ("this".equals(target) || "self".equals(target)) {
            sb.append("    ").append(methodName).append("(obj);");
        } else {
            sb.append("    ").append(methodName).append("(").append(target).append(");");
        }
        sb.append("}");
        return sb.toString();
    }

    // ================================================================
    //  属性变更监听钩子
    // ================================================================

    @Override
    public String generateBeforePropsHook(String propertyPath, String newValue) {
        // C 中通过函数指针回调实现
        String safePath = propertyPath.replace(".", "_");
        return "__claw_before_prop_change(self, \"" + propertyPath + "\", (void*)&" + newValue + ");";
    }

    @Override
    public String generateAfterPropsHook(String propertyPath, String oldValue, String newValue) {
        String safePath = propertyPath.replace(".", "_");
        return "__claw_after_prop_change(self, \"" + propertyPath + "\", (void*)&" +
               oldValue + ", (void*)&" + newValue + ");";
    }

    @Override
    public String generateMonitoredPropertySet(String propertyPath, String newValueExpr,
                                                boolean hasBefore, boolean hasAfter) {
        StringBuilder sb = new StringBuilder();
        String[] parts = propertyPath.split("\\.");
        String fieldName = parts[parts.length - 1];

        if (hasBefore) {
            sb.append(generateBeforePropsHook(propertyPath, newValueExpr)).append("\n");
        }

        if (hasAfter) {
            // 保存旧值
            sb.append("typeof(self->").append(fieldName).append(") __old_")
              .append(fieldName).append(" = self->").append(fieldName).append(";");
        }

        // 实际赋值
        sb.append("self->").append(fieldName).append(" = ").append(newValueExpr).append(";");

        if (hasAfter) {
            sb.append("\n");
            sb.append(generateAfterPropsHook(propertyPath, "__old_" + fieldName, newValueExpr));
        }

        return sb.toString();
    }

    // ================================================================
    //  三层操作流
    // ================================================================

    @Override
    public String generateCatchBlock(String exceptionType, String varName, String handlerBody) {
        // C 没有原生异常，使用 setjmp/longjmp 模拟
        // 或使用错误码 + goto 模式
        StringBuilder sb = new StringBuilder();
        sb.append("/* catch ").append(exceptionType).append(" */");
        sb.append("if (__claw_exception_type == CLAW_EX_").append(exceptionType.toUpperCase()).append(") {");
        sb.append("    ClawException* ").append(varName).append(" = &__claw_current_exception;");
        sb.append("    ").append(handlerBody).append("\n");
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String generateThrowsDeclaration(List<String> exceptionTypes) {
        // C 没有 throws，用注释标注
        if (exceptionTypes == null || exceptionTypes.isEmpty()) return "";
        String types = String.join(", ", exceptionTypes);
        return " /* throws: " + types + " */";
    }

    @Override
    public String generateThrowStatement(String exceptionType, String messageExpr) {
        return "/* throw " + exceptionType + "(" + messageExpr + ") */";
    }

    @Override
    public String generateFlowTo(String targetLabel) {
        // C 原生支持 goto！这是最直接的映射
        return "goto " + targetLabel + "; /* flow to " + targetLabel + " */";
    }

    @Override
    public boolean supportsNativeFlowJump() {
        return true; // C 支持原生 goto
    }

    // ================================================================
    //  函数生成
    // ================================================================

    @Override
    public String generateFunctionHeader(String visibility, String returnType,
                                          String funcName, List<Map.Entry<String, String>> params,
                                          List<String> throwsTypes) {
        StringBuilder sb = new StringBuilder();

        // C 中 static = private（文件内可见）
        if ("private".equals(visibility)) {
            sb.append("static ");
        }

        sb.append(mapType(returnType)).append(" ");
        sb.append(funcName).append("(");

        // 参数列表
        List<String> paramStrings = new ArrayList<>();
        if (params != null) {
            for (Map.Entry<String, String> param : params) {
                paramStrings.add(mapType(param.getValue()) + " " + param.getKey());
            }
        }

        if (paramStrings.isEmpty()) {
            sb.append("void"); // C 中空参数列表写 void
        } else {
            sb.append(String.join(", ", paramStrings));
        }

        sb.append(")");

        // throws 注释
        sb.append(generateThrowsDeclaration(throwsTypes));

        sb.append(" {");
        return sb.toString();
    }

    @Override
    public String generateFunctionFooter() {
        return "}";
    }

    /**
     * 生成函数前向声明（头文件用）
     */
    public String generateFunctionPrototype(String visibility, String returnType,
                                             String funcName, List<Map.Entry<String, String>> params,
                                             List<String> throwsTypes) {
        String header = generateFunctionHeader(visibility, returnType, funcName, params, throwsTypes);
        // 去掉最后的 " {" 替换为 ";"
        return header.substring(0, header.length() - 2) + ";";
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
            return call + getStatementTerminator();
        }
        return resultVar + " = " + call + getStatementTerminator();
    }

    @Override
    public String generateReturn(String expression) {
        if (expression == null || expression.isEmpty()) {
            return "return;";
        }
        return "return " + expression + ";";
    }

    // ================================================================
    //  变量声明
    // ================================================================

    @Override
    public String generateVariableDeclaration(boolean isConst, String typeName,
                                               String varName, String initExpr) {
        StringBuilder sb = new StringBuilder();
        if (isConst) {
            sb.append("const ");
        }
        sb.append(mapType(typeName)).append(" ").append(varName);
        if (initExpr != null) {
            sb.append(" = ").append(initExpr);
        }
        sb.append(";");
        return sb.toString();
    }

    // ================================================================
    //  控制流
    // ================================================================

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
        // C 没有 for-each，生成索引循环
        return "for (int __i = 0; __i < " + iterable + "_len; __i++) { " +
               mapType("Any") + " " + varName + " = " + iterable + "[__i];";
    }

    @Override
    public String generateBreak() {
        return "break;";
    }

    @Override
    public String generateContinue() {
        return "continue;";
    }

    // ================================================================
    //  块界定符
    // ================================================================

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

    // ================================================================
    //  导入/导出
    // ================================================================

    // @Override
    // public String generateImport(String modulePath, String symbolName) {
    //     // C 使用 #include
    //     if (modulePath.startsWith("<") || modulePath.startsWith("\"")) {
    //         return "#include " + modulePath;
    //     }
    //     // Claw模块路径转换为头文件路径
    //     String headerPath = modulePath.replace(".", "/") + ".h";
    //     return "#include \"" + headerPath + "\"";
    // }

    @Override
public String generateImport(String modulePath, String symbolName) {
    // 标准库映射
    Map<String, String> stdMapping = Map.ofEntries(
        Map.entry("std.io",       "<stdio.h>"),
        Map.entry("std.string",   "<string.h>"),
        Map.entry("std.math",     "<math.h>"),
        Map.entry("std.memory",   "<stdlib.h>"),
        Map.entry("std.bool",     "<stdbool.h>"),
        Map.entry("std.time",     "<time.h>"),
        Map.entry("std.assert",   "<assert.h>"),
        Map.entry("std.errno",    "<errno.h>"),
        Map.entry("std.signal",   "<signal.h>"),
        Map.entry("std.thread",   "<pthread.h>"),
        Map.entry("std.collections", "<stdlib.h>"), // C 中集合需自定义，复用 stdlib
        Map.entry("std.net",      "<sys/socket.h>"),
        Map.entry("std.regex",    "<regex.h>"),
        Map.entry("std.concurrent", "<pthread.h>")
    );

    String mapped = stdMapping.get(modulePath);
    if (mapped != null) {
        return "#include " + mapped;
    }

    // 用户自定义模块
    String headerPath = modulePath.replace(".", "/") + ".h";
    return "#include \"" + headerPath + "\"";
}


    @Override
    public String generateExport(String symbolName) {
        // C 中导出通过头文件声明实现，源文件中用注释标记
        return "/* exported: " + symbolName + " (declared in header) */";
    }

    // ================================================================
    //  类型定义（struct）
    // ================================================================

    @Override
    public String generateTypeDefinitionHeader(String typeName, String visibility) {
        // C 中使用 typedef struct
        return "typedef struct " + typeName + " {";
    }

    @Override
    public String generateTypeField(String fieldName, String fieldType, String visibility) {
        // C 中没有访问控制，用注释标记
        String comment = "private".equals(visibility) ? " /* private */" : "";
        return "    " + mapValueType(fieldType) + " " + fieldName + ";" + comment;
    }

    @Override
    public String generateTypeDefinitionFooter() {
        return "}"; // 调用方需要追加 " TypeName;"
    }

    /**
     * 生成完整的 typedef struct（带类型别名）
     */
    public String generateCompleteTypeDefinition(String typeName) {
        return "} " + typeName + ";";
    }

    // ================================================================
    //  运算符
    // ================================================================

    @Override
    public String generateBinaryExpression(String left, String operator, String right) {
        // 字符串拼接需要特殊处理（C 中用 strcat / snprintf）
        if ("+".equals(operator)) {
            // 运行时需要判断是否是字符串拼接
            // 这里生成通用的数值加法，字符串拼接由类型检查器标记
            return left + " + " + right;
        }
        return left + " " + operator + " " + right;
    }

    @Override
    public String generateUnaryExpression(String operator, String operand) {
        return operator + operand;
    }

    // ================================================================
    //  注释
    // ================================================================

    @Override
    public String generateComment(String comment) {
        return "/* " + comment + " */";
    }

    @Override
    public String generateDocComment(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "/* No documentation. */";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("/**");

        String desc = metadata.get("description");
        if (desc != null) {
            sb.append(" * @brief ").append(desc).append("\n");
        }

        String ioSpec = metadata.get("io_spec");
        if (ioSpec != null) {
            sb.append(" * @note IO: ").append(ioSpec).append("\n");
        }

        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if ("description".equals(key) || "io_spec".equals(key)) continue;

            if (key.startsWith("param.")) {
                String paramName = key.substring(6);
                sb.append(" * @param ").append(paramName).append(" ").append(entry.getValue()).append("\n");
            } else if ("return".equals(key)) {
                sb.append(" * @return ").append(entry.getValue()).append("\n");
            } else if ("deprecated".equals(key)) {
                sb.append(" * @deprecated ").append(entry.getValue()).append("\n");
            } else if ("example".equals(key)) {
                sb.append(" * @code");
                sb.append(" *   ").append(entry.getValue()).append("\n");
                sb.append(" * @endcode");
            }
        }

        sb.append(" */");
        return sb.toString();
    }

    // ================================================================
    //  字符串操作
    // ================================================================

    @Override
    public String generateStringLiteral(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
                        //       .replace("", "\")
                              .replace("	", "	");
        return "\"" + escaped + "\"";
    }

    @Override
    public String generateStringConcat(List<String> parts) {
        if (parts == null || parts.isEmpty()) return "\"\"";
        if (parts.size() == 1) return parts.get(0);
        // C 中字符串拼接需要 snprintf 或自定义函数
        String args = String.join(", ", parts);
        return "__claw_str_concat(" + parts.size() + ", " + args + ")";
    }

    // ================================================================
    //  异常/错误
    // ================================================================

    @Override
    public String generateThrow(String exceptionType, String message) {
        // C 中用 longjmp 模拟异常抛出
        return "__claw_throw(CLAW_EX_" + exceptionType.toUpperCase() + ", " + message + ");";
    }

    @Override
    public String generateExceptionTypeDefinition(String exceptionName, String parentType) {
        // C 中异常类型用枚举值 + 结构体
        StringBuilder sb = new StringBuilder();
        sb.append("/* Exception type: ").append(exceptionName).append(" */");
        sb.append("#define CLAW_EX_").append(exceptionName.toUpperCase())
          .append(" ").append(Math.abs(exceptionName.hashCode()) % 10000).append("\n");
        return sb.toString();
    }

    // ================================================================
    //  数组操作
    // ================================================================

    @Override
    public String generateArrayCreation(String elementType, String size) {
        String cType = mapType(elementType);
        // 基本类型用栈分配，复杂类型用堆
        switch (elementType) {
            case "Int": case "Float": case "Bool":
                return "(" + cType + "*)calloc(" + size + ", sizeof(" + cType + "))";
            default:
                return "(" + cType + "*)calloc(" + size + ", sizeof(" + cType + "))";
        }
    }

    @Override
    public String generateArrayGet(String arrayName, String index) {
        return arrayName + "[" + index + "]";
    }

    @Override
    public String generateArraySet(String arrayName, String index, String value) {
        return arrayName + "[" + index + "] = " + value + ";";
    }

    // ================================================================
    //  属性访问（struct 字段）
    // ================================================================

    @Override
    public String generatePropertyGet(String objectExpr, String fieldName) {
        // C 中指针用 ->，值类型用 .
        // 默认假设自定义类型是指针
        return objectExpr + "->" + fieldName;
    }

    @Override
    public String generatePropertySet(String objectExpr, String fieldName, String valueExpr) {
        return objectExpr + "->" + fieldName + " = " + valueExpr + ";";
    }

    // ================================================================
    //  作用域
    // ================================================================

    @Override
    public String generateScopeEnter(String scopeName) {
        return "{ /* scope: " + scopeName + " */";
    }

    @Override
    public String generateScopeExit(String scopeName) {
        return "} /* end scope: " + scopeName + " */";
    }

    // ================================================================
    //  运行时辅助
    // ================================================================

    // @Override
    // public List<String> getRequiredImports() {
    //     List<String> imports = new ArrayList<>();
    //     imports.add("<stdio.h>");
    //     imports.add("<stdlib.h>");
    //     imports.add("<string.h>");
    //     imports.add("<stdbool.h>");
    //     imports.add("<setjmp.h>");
    //     return imports;
    // }
    @Override
public List<String> getRequiredImports() {
    List<String> imports = new ArrayList<>();
    imports.add("<stdio.h>");
    imports.add("<stdlib.h>");
    imports.add("<string.h>");
    imports.add("<stdbool.h>");
    imports.add("\"claw_runtime.h\"");   // 关键：包含运行时
    return imports;
}

public List<String> generateImport () {
    List<String> imports = new ArrayList<>();
    imports.add("<stdio.h>");
    imports.add("<stdlib.h>");
    imports.add("<string.h>");
    imports.add("<stdbool.h>");
    imports.add("\"claw_runtime.h\"");   // 关键：包含运行时
    return imports;
}

    @Override
    public String generateRuntimeHelpers() {
        StringBuilder sb = new StringBuilder();

        sb.append("/* ============================================ */");
        sb.append("/* Claw Runtime Helpers for C                    */");
        sb.append("/* Auto-generated - Do not edit                  */");
        sb.append("/* ============================================ */");

        // 1. 异常处理系统（setjmp/longjmp）
        sb.append("/* --- Exception System (setjmp/longjmp) --- */");
        sb.append("typedef struct ClawException {");
        sb.append("    int type;");
        sb.append("    const char* message;");
        sb.append("    const char* file;");
        sb.append("    int line;");
        sb.append("} ClawException;");

        sb.append("#define CLAW_EX_NONE 0");
        sb.append("#define CLAW_EX_GENERIC 1");
        sb.append("#define CLAW_EX_NULL_POINTER 2");
        sb.append("#define CLAW_EX_OUT_OF_BOUNDS 3");
        sb.append("#define CLAW_EX_VALIDATION 4");

        sb.append("static jmp_buf __claw_jmp_buf_stack[64];");
        sb.append("static int __claw_jmp_buf_top = -1;");
        sb.append("static int __claw_exception_type = CLAW_EX_NONE;");
        sb.append("static ClawException __claw_current_exception = {0, NULL, NULL, 0};");

        sb.append("#define CLAW_TRY \\");
        sb.append("    do { \\");
        sb.append("        __claw_jmp_buf_top++; \\");
        sb.append("        __claw_exception_type = setjmp(__claw_jmp_buf_stack[__claw_jmp_buf_top]); \\");
        sb.append("        if (__claw_exception_type == 0) {");

        sb.append("#define CLAW_CATCH(ex_type) \\");
        sb.append("        } else if (__claw_exception_type == (ex_type)) { \\");
        sb.append("            __claw_jmp_buf_top--;");

        sb.append("#define CLAW_END_TRY \\");
        sb.append("        } else { \\");
        sb.append("            __claw_jmp_buf_top--; \\");
        sb.append("            if (__claw_jmp_buf_top >= 0) { \\");
        sb.append("                longjmp(__claw_jmp_buf_stack[__claw_jmp_buf_top], __claw_exception_type); \\");
        sb.append("            } else { \\");
        sb.append("                fprintf(stderr, \"Unhandled exception: %s\", __claw_current_exception.message \\");
        sb.append("                exit(1); \\");
        sb.append("            } \\");
        sb.append("        } \\");
        sb.append("    } while(0)");

        sb.append("static void __claw_throw(int ex_type, const char* message) {");
        sb.append("    __claw_exception_type = ex_type;");
        sb.append("    __claw_current_exception.type = ex_type;");
        sb.append("    __claw_current_exception.message = message;");
        sb.append("    if (__claw_jmp_buf_top >= 0) {");
        sb.append("        longjmp(__claw_jmp_buf_stack[__claw_jmp_buf_top], ex_type);");
        sb.append("    } else {");
        sb.append("        fprintf(stderr, \"Unhandled exception [%d]: %s\", ex_type, message);");
        sb.append("        exit(1);");
        sb.append("    }");
        sb.append("}");

        // 2. 属性变更监听系统
        sb.append("/* --- Property Change Monitoring --- */");
        sb.append("typedef void (*BeforePropsCallback)(void* self, const char* prop, void* new_value);");
        sb.append("typedef void (*AfterPropsCallback)(void* self, const char* prop, void* old_value, void* new_value);");

        sb.append("#define CLAW_MAX_PROP_LISTENERS 32");

        sb.append("typedef struct ClawPropMonitor {");
        sb.append("    struct {");
        sb.append("        const char* property;");
        sb.append("        BeforePropsCallback callback;");
        sb.append("    } before_listeners[CLAW_MAX_PROP_LISTENERS];");
        sb.append("    int before_count;");
        sb.append("    struct {");
        sb.append("        const char* property;");
        sb.append("        AfterPropsCallback callback;");
        sb.append("    } after_listeners[CLAW_MAX_PROP_LISTENERS];");
        sb.append("    int after_count;");
        sb.append("} ClawPropMonitor;");

        sb.append("static void __claw_prop_monitor_init(ClawPropMonitor* monitor) {");
        sb.append("    monitor->before_count = 0;");
        sb.append("    monitor->after_count = 0;");
        sb.append("}");

        sb.append("static void __claw_register_before_prop(ClawPropMonitor* monitor, const char* prop, BeforePropsCallback cb) {");
        sb.append("    if (monitor->before_count < CLAW_MAX_PROP_LISTENERS) {");
        sb.append("        monitor->before_listeners[monitor->before_count].property = prop;");
        sb.append("        monitor->before_listeners[monitor->before_count].callback = cb;");
        sb.append("        monitor->before_count++;");
        sb.append("    }");
        sb.append("}");

        sb.append("static void __claw_register_after_prop(ClawPropMonitor* monitor, const char* prop, AfterPropsCallback cb) {");
        sb.append("    if (monitor->after_count < CLAW_MAX_PROP_LISTENERS) {");
        sb.append("        monitor->after_listeners[monitor->after_count].property = prop;");
        sb.append("        monitor->after_listeners[monitor->after_count].callback = cb;");
        sb.append("        monitor->after_count++;");
        sb.append("    }");
        sb.append("}");

        sb.append("static void __claw_before_prop_change(void* self, const char* prop, void* new_value) {");
        sb.append("    /* In real usage, self should contain a ClawPropMonitor* field */");
        sb.append("    /* Dispatch to registered before-callbacks */");
        sb.append("}");

        sb.append("static void __claw_after_prop_change(void* self, const char* prop, void* old_value, void* new_value) {");
        sb.append("    /* Dispatch to registered after-callbacks */");
        sb.append("}");

        // 3. 字符串拼接辅助
        sb.append("/* --- String Helpers --- */");
        sb.append("#define CLAW_STR_BUF_SIZE 4096");
        sb.append("static char* __claw_str_concat(int count, ...) {");
        sb.append("    static char buf[CLAW_STR_BUF_SIZE];");
        sb.append("    buf[0] = '\\0';");
        sb.append("    va_list args;");
        sb.append("    va_start(args, count);");
        sb.append("    for (int i = 0; i < count; i++) {");
        sb.append("        const char* s = va_arg(args, const char*);");
        sb.append("        if (s) strncat(buf, s, CLAW_STR_BUF_SIZE - strlen(buf) - 1);");
        sb.append("    }");
        sb.append("    va_end(args);");
        sb.append("    return buf;");
        sb.append("}");

        // 需要 va_list 支持
        // 注意：getRequiredImports 中已包含 <stdarg.h> 被遗漏，需要补充

        return sb.toString();
    }

    /**
     * 生成头文件内容
     */
    public String generateHeaderFile(String moduleName, List<String> functionPrototypes,
                                      List<String> typeDefinitions) {
        StringBuilder sb = new StringBuilder();
        String guard = moduleName.toUpperCase().replace(".", "_") + "_H";

        sb.append("#ifndef ").append(guard).append("\n");
        sb.append("#define ").append(guard).append("\n");

        // 标准头文件
        for (String imp : getRequiredImports()) {
            sb.append("#include ").append(imp).append("\n");
        }
        sb.append("#include <stdarg.h>");
        sb.append("\n");

        // 类型定义
        if (typeDefinitions != null) {
            for (String typeDef : typeDefinitions) {
                sb.append(typeDef).append("\n");
            }
        }

        // 函数原型
        if (functionPrototypes != null) {
            for (String proto : functionPrototypes) {
                sb.append(proto).append("\n");
            }
        }

        sb.append("#endif /* ").append(guard).append(" */");
        return sb.toString();
    }

    // ================================================================
    //  AOP (Aspect-Oriented Programming) 支持
    // ================================================================

    /**
     * 生成切面结构体定义（C 使用函数指针回调实现 AOP）
     */
    public String generateAspectDefinition(String aspectName) {
        return "/* AOP Aspect: " + aspectName + " */\n" +
               "typedef struct {\n" +
               "    const char* aspect_name;\n" +
               "    void (*before)(JoinPoint*);\n" +
               "    void (*after)(JoinPoint*);\n" +
               "    void* (*around)(ProceedingJoinPoint*);\n" +
               "} " + aspectName + ";";
    }

    /**
     * 生成 before 通知函数原型
     */
    public String generateBeforeAdvice(String adviceName, String targetName) {
        return "/* Before advice for " + targetName + " */\n" +
               "void " + adviceName + "(JoinPoint* jp) {\n" +
               "    printf(\"[BEFORE] %s\\n\", jp->method_name);\n" +
               "}";
    }

    /**
     * 生成 after 通知函数原型
     */
    public String generateAfterAdvice(String adviceName, String targetName) {
        return "/* After advice for " + targetName + " */\n" +
               "void " + adviceName + "(JoinPoint* jp) {\n" +
               "    printf(\"[AFTER] %s\\n\", jp->method_name);\n" +
               "}";
    }

    /**
     * 生成 around 通知函数原型
     */
    public String generateAroundAdvice(String adviceName, String targetName) {
        return "/* Around advice for " + targetName + " */\n" +
               "void* " + adviceName + "(ProceedingJoinPoint* jp) {\n" +
               "    printf(\"[BEGIN AROUND] %s\\n\", jp->method_name);\n" +
               "    void* result = jp->proceed(jp);\n" +
               "    printf(\"[END AROUND] %s\\n\", jp->method_name);\n" +
               "    return result;\n" +
               "}";
    }

    /**
     * 生成 JoinPoint 结构体定义和相关辅助函数
     */
    public String generateJoinPointSupport() {
        return "/* ============================================ */\n" +
               "/* AOP Support for C                          */\n" +
               "/* ============================================ */\n\n" +
               "/* 连接点结构体 */\n" +
               "typedef struct {\n" +
               "    const char* method_name;\n" +
               "    void** args;\n" +
               "    int arg_count;\n" +
               "    void* target;\n" +
               "    void* (*proceed)(struct _ProceedingJoinPoint*);\n" +
               "} JoinPoint;\n\n" +
               "typedef struct _ProceedingJoinPoint {\n" +
               "    JoinPoint base;\n" +
               "    void* (*proceed)(struct _ProceedingJoinPoint*);\n" +
               "} ProceedingJoinPoint;\n\n" +
               "/* 创建 JoinPoint */\n" +
               "static JoinPoint* join_point_create(const char* method_name, void** args, int arg_count, void* target) {\n" +
               "    JoinPoint* jp = (JoinPoint*)malloc(sizeof(JoinPoint));\n" +
               "    if (jp) {\n" +
               "        jp->method_name = method_name;\n" +
               "        jp->args = args;\n" +
               "        jp->arg_count = arg_count;\n" +
               "        jp->target = target;\n" +
               "        jp->proceed = NULL;\n" +
               "    }\n" +
               "    return jp;\n" +
               "}\n\n" +
               "/* 释放 JoinPoint */\n" +
               "static void join_point_free(JoinPoint* jp) {\n" +
               "    if (jp) free(jp);\n" +
               "}\n";
    }

    /**
     * 生成 JoinPoint 创建语句
     */
    public String generateJoinPointCreate(String joinPointName, String methodName, String args) {
        return joinPointName + " = join_point_create(\"" + methodName + "\", " + args + ", 0, NULL);";
    }

    /**
     * 生成 advice proceed 调用
     */
    public String generateAdviceProceed(String joinPointName) {
        return joinPointName + "->proceed(" + joinPointName + ")";
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
        return joinPointName + " = join_point_create(\"" + methodName + "\", " + argsExpr + ", 0, " + targetExpr + ");";
    }
}

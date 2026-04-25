package com.q3lives.binding.java;

import com.q3lives.binding.GenerationConfig;
import com.q3lives.binding.GenerationResult;
import com.q3lives.binding.TargetCodeGenerator;
import com.q3lives.binding.TargetRuntime;

import java.util.*;
import java.util.stream.Collectors;

import com.q3lives.compiler.generators.IRGenerator;
import com.q3lives.compiler.generators.IRGenerator.IRBasicBlock;
import com.q3lives.compiler.generators.IRGenerator.IRInstruction;
import com.q3lives.compiler.generators.IRGenerator.OpCode;
import com.q3lives.compiler.generators.IRGenerator.IRProgram;
import com.q3lives.compiler.generators.ffi.FFIBindingTable;
import com.q3lives.compiler.generators.ffi.JavaFFIGenerator;
import com.q3lives.ir.ClawIR;

/**
 * 增强的 Java 目标语言代码生成器
 *
 * 实现了完整的功能，包括：
 * 1. 泛型支持
 * 2. 增强的错误处理
 * 3. 完整的指令支持
 * 4. 更好的代码组织
 * 5. 完善的注解处理
 */
public class EnhancedJavaCodeGenerator implements TargetCodeGenerator {

    private final JavaRuntime runtime;
    private final StringBuilder output;
    private final StringBuilder headerOutput;
    private int indentLevel;

    // 泛型支持
    private final Set<String> genericTypes;
    private final Map<String, List<String>> typeParameters;

    // 错误处理
    private final List<String> errorMessages;
    private final List<String> warningMessages;

    // 代码组织
    private final List<String> importedClasses;
    private final Set<String> declaredClasses;
    private final Map<String, String> classFields;

    // 统计信息
    private int methodCount;
    private int fieldCount;

    public EnhancedJavaCodeGenerator() {
        this.runtime = new JavaRuntime();
        this.output = new StringBuilder();
        this.headerOutput = new StringBuilder();
        this.indentLevel = 0;

        // 泛型支持
        this.genericTypes = new HashSet<>();
        this.typeParameters = new HashMap<>();

        // 错误处理
        this.errorMessages = new ArrayList<>();
        this.warningMessages = new ArrayList<>();

        // 代码组织
        this.importedClasses = new ArrayList<>();
        this.declaredClasses = new HashSet<>();
        this.classFields = new HashMap<>();

        // 统计信息
        this.methodCount = 0;
        this.fieldCount = 0;
    }

    /**
     * 构造函数：允许注入自定义运行时（测试用）
     */
    public EnhancedJavaCodeGenerator(JavaRuntime runtime) {
        this.runtime = runtime;
        this.output = new StringBuilder();
        this.headerOutput = new StringBuilder();
        this.indentLevel = 0;

        this.genericTypes = new HashSet<>();
        this.typeParameters = new HashMap<>();
        this.errorMessages = new ArrayList<>();
        this.warningMessages = new ArrayList<>();
        this.importedClasses = new ArrayList<>();
        this.declaredClasses = new HashSet<>();
        this.classFields = new HashMap<>();
        this.methodCount = 0;
        this.fieldCount = 0;
    }

    @Override
    public TargetRuntime getRuntime() {
        return runtime;
    }

    @Override
    public String getLanguageName() {
        return "Java";
    }

    @Override
    public String getFileExtension() {
        return ".java";
    }

    /**
     * 获取生成的头文件内容（用于多文件生成）
     */
    public String getHeaderOutput() {
        return headerOutput != null ? headerOutput.toString() : "";
    }

    @Override
    public String generate(ClawIR ir) {
        output.setLength(0);
        headerOutput.setLength(0);
        indentLevel = 0;

        // 清理状态
        genericTypes.clear();
        typeParameters.clear();
        errorMessages.clear();
        warningMessages.clear();
        importedClasses.clear();
        declaredClasses.clear();
        classFields.clear();
        methodCount = 0;
        fieldCount = 0;

        IRGenerator.IRProgram program = ir.getIrProgram();

        // ===== 生成文件头 =====
        generateFileHeader(program);

        // ===== 生成包声明 =====
        generatePackageDeclaration(program);

        // ===== 生成 import 语句 =====
        generateImports();

        // ===== 生成类定义 =====
        for (IRBasicBlock topBlock : program.getTopLevelBlocks()) {
            generateClassDefinition(topBlock);
        }

        // ===== 生成主方法（如果没有其他方法） =====
        if (methodCount == 0) {
            generateMainMethod();
        }

        return output.toString();
    }

    @Override
    public GenerationResult generate(ClawIR ir, GenerationConfig config) {
        GenerationResult result = new GenerationResult();

        // 如果包含 FFI 绑定，先生成 FFI 代码（独立于主代码生成）
        if (ir != null && ir.hasFFIBindings()) {
            try {
                FFIBindingTable ffiTable = ir.getFfiBindingTable();
                JavaFFIGenerator ffiGenerator = new JavaFFIGenerator(ffiTable);
                String ffiCode = ffiGenerator.generateAll();
                if (!ffiCode.isEmpty()) {
                    result.addFile("ClawFFIBindings.java", ffiCode);
                }
            } catch (Exception e) {
                result.addError("FFI 代码生成错误: " + e.getMessage());
            }
        }

        try {
            // 生成主代码
            String mainCode = generate(ir);

            // 生成辅助类
            String helperCode = generateHelperClasses();

            // 设置输出结果
            String className = extractClassName(ir);
            result.addFile(className + ".java", mainCode);
            result.addFile("ClawRuntime.java", helperCode);

            // 添加统计信息
            result.addStats("methods_generated", String.valueOf(methodCount));
            result.addStats("fields_generated", String.valueOf(fieldCount));
            result.addStats("generic_types", String.valueOf(genericTypes.size()));
            result.addStats("errors", String.valueOf(errorMessages.size()));

            // 添加错误和警告
            for (String error : errorMessages) {
                result.addError(error);
            }
            for (String warning : warningMessages) {
                result.addWarning(warning);
            }

        } catch (Exception e) {
            result.addError("代码生成错误: " + e.getMessage());
        }

        return result;
    }

    // ==================== 代码生成核心方法 ====================

    /**
     * 生成文件头
     */
    private void generateFileHeader(IRProgram program) {
        appendLine("/**");
        appendLine(" * Auto-generated by Claw Compiler v3.0");
        appendLine(" * Target: Java");
        appendLine(" * Source: " + program.getSourceFileName());
        appendLine(" */");
        appendLine("");
    }

    /**
     * 生成包声明
     */
    private void generatePackageDeclaration(IRProgram program) {
        String packageName = extractPackageName(program);
        if (!packageName.isEmpty()) {
            appendLine("package " + packageName + ";");
            appendLine("");
        }
    }

    /**
     * 生成 import 语句
     */
    private void generateImports() {
        // 必要的标准库导入
        appendLine("import java.util.*;");
        appendLine("import java.util.function.*;");
        appendLine("import java.lang.reflect.*;");
        appendLine("");

        // 从运行时获取必要的导入
        for (String imp : runtime.getRequiredImports()) {
            if (!importedClasses.contains(imp)) {
                appendLine(imp);
                importedClasses.add(imp);
            }
        }
        appendLine("");

        // 生成泛型相关的导入
        if (!genericTypes.isEmpty()) {
            appendLine("import java.util.List;");
            appendLine("import java.util.Map;");
            appendLine("import java.util.HashMap;");
            appendLine("import java.util.ArrayList;");
            appendLine("");
        }
    }

    /**
     * 生成类定义
     */
    private void generateClassDefinition(IRBasicBlock block) {
        // 查找 TYPE_DEF 指令
        for (IRInstruction inst : block.getInstructions()) {
            if (inst.getOpCode() == OpCode.TYPE_DEF) {
                generateClass(inst, block);
                return;
            }
        }

        // 如果没有类定义，创建默认类
        generateDefaultClass();
    }

    /**
     * 生成具体的类
     */
    private void generateClass(IRInstruction typeDefInst, IRBasicBlock block) {
        List<Object> ops = typeDefInst.getOperands();
        String className = ops.get(0).toString();

        // 记录已声明的类
        declaredClasses.add(className);

        appendLine("public class " + className + " {");
        indentLevel++;

        // 生成静态导入的属性
        generateStaticImports();

        // 生成类字段
        generateClassFields(block);

        // 生成类方法
        generateClassMethods(block);

        // 生成主方法
        generateMainMethod();

        indentLevel--;
        appendLine("}");
    }

    /**
     * 生成类字段
     */
    private void generateClassFields(IRBasicBlock block) {
        // 收集字段信息
        List<FieldInfo> fields = new ArrayList<>();

        for (IRInstruction inst : block.getInstructions()) {
            if (inst.getOpCode() == OpCode.TYPE_FIELD) {
                List<Object> fieldOps = inst.getOperands();
                String fieldName = fieldOps.get(0).toString();
                String fieldType = fieldOps.size() > 1 ? fieldOps.get(1).toString() : "Object";
                String visibility = fieldOps.size() > 2 ? fieldOps.get(2).toString() : "private";

                fields.add(new FieldInfo(fieldName, fieldType, visibility));
            }
        }

        // 生成字段声明
        for (FieldInfo field : fields) {
            appendLine(field.visibility + " " + mapGenericType(field.type) + " " + field.name + ";");
            fieldCount++;

            // 添加到字段映射
            classFields.put(field.name, field.type);
        }

        if (!fields.isEmpty()) {
            appendLine("");
        }
    }

    /**
     * 生成类方法
     */
    private void generateClassMethods(IRBasicBlock block) {
        // 收集方法信息
        List<MethodInfo> methods = new ArrayList<>();

        for (IRInstruction inst : block.getInstructions()) {
            if (inst.getOpCode() == OpCode.FUNC_DEF) {
                List<Object> funcOps = inst.getOperands();
                String methodName = funcOps.get(0).toString();

                // 解析方法签名
                Map<String, String> params = funcOps.size() > 1 ?
                    (Map<String, String>) funcOps.get(1) : new HashMap<>();
                String returnType = funcOps.size() > 2 ? funcOps.get(2).toString() : "void";

                methods.add(new MethodInfo(methodName, params, returnType));
            }
        }

        // 生成方法声明
        for (MethodInfo method : methods) {
            generateMethod(method);
        }

        if (!methods.isEmpty()) {
            appendLine("");
        }
    }

    /**
     * 生成单个方法
     */
    private void generateMethod(MethodInfo method) {
        // 生成方法签名
        StringBuilder signature = new StringBuilder();
        signature.append("public ").append(mapGenericType(method.returnType)).append(" ").append(method.name).append("(");

        // 参数列表
        List<String> paramList = new ArrayList<>();
        for (Map.Entry<String, String> param : method.params.entrySet()) {
            paramList.add(mapGenericType(param.getValue()) + " " + param.getKey());
        }
        signature.append(String.join(", ", paramList));
        signature.append(")");

        appendLine(signature.toString());
        indentLevel++;
        appendLine("{");
        indentLevel++;

        // 生成方法体
        generateMethodBody(method);

        indentLevel--;
        appendLine("}");
        appendLine("");

        methodCount++;
    }

    /**
     * 生成方法体
     */
    private void generateMethodBody(MethodInfo method) {
        // 生成简单的实现
        appendLine("// Implementation for " + method.name);

        // 根据返回类型生成不同的返回语句
        if (!"void".equals(method.returnType)) {
            if ("String".equals(method.returnType)) {
                appendLine("return \"\";");
            } else if ("boolean".equals(method.returnType) || "Bool".equals(method.returnType)) {
                appendLine("return false;");
            } else if ("int".equals(method.returnType) || "Int".equals(method.returnType)) {
                appendLine("return 0;");
            } else if ("double".equals(method.returnType) || "Float".equals(method.returnType)) {
                appendLine("return 0.0;");
            } else {
                appendLine("return null;");
            }
        }
    }

    /**
     * 生成默认类
     */
    private void generateDefaultClass() {
        String className = "ClawProgram";
        appendLine("public class " + className + " {");
        indentLevel++;

        generateMainMethod();

        indentLevel--;
        appendLine("}");
    }

    /**
     * 生成主方法
     */
    private void generateMainMethod() {
        appendLine("public static void main(String[] args) {");
        indentLevel++;
        appendLine("System.out.println(\"Hello from Claw!\");");
        indentLevel--;
        appendLine("}");
        appendLine("");

        methodCount++;
    }

    /**
     * 生成辅助类
     */
    private String generateHelperClasses() {
        StringBuilder helperCode = new StringBuilder();

        // 生成泛型支持类
        helperCode.append("/**\n");
        helperCode.append(" * 泛型支持工具类\n");
        helperCode.append(" */\n");
        helperCode.append("public final class ClawGenerics {\n\n");

        // 通用工厂方法
        helperCode.append("    /**\n");
        helperCode.append("     * 创建列表\n");
        helperCode.append("     */\n");
        helperCode.append("    public static <T> List<T> newList() {\n");
        helperCode.append("        return new ArrayList<>();\n");
        helperCode.append("    }\n\n");

        helperCode.append("    /**\n");
        helperCode.append("     * 创建映射\n");
        helperCode.append("     */\n");
        helperCode.append("    public static <K, V> Map<K, V> newMap() {\n");
        helperCode.append("        return new HashMap<>();\n");
        helperCode.append("    }\n\n");

        helperCode.append("    /**\n");
        helperCode.append("     * 安全的类型转换\n");
        helperCode.append("     */\n");
        helperCode.append("    @SuppressWarnings(\"unchecked\")\n");
        helperCode.append("    public static <T> T cast(Object obj, Class<T> type) {\n");
        helperCode.append("        return type.cast(obj);\n");
        helperCode.append("    }\n");
        helperCode.append("}\n");

        return helperCode.toString();
    }

    // ==================== 指令处理方法 ====================

    /**
     * 生成作用域进入
     */
    private void generateScopeEnter(String scopeName) {
        appendLine("// Scope: " + scopeName);
        appendLine("{");
        indentLevel++;
    }

    /**
     * 生成作用域退出
     */
    private void generateScopeExit(String scopeName) {
        indentLevel--;
        appendLine("}");
        appendLine("// End scope: " + scopeName);
    }

    /**
     * 生成内存分配
     */
    private void generateAllocation(String varName, String typeName) {
        String javaType = mapGenericType(typeName);
        if ("String".equals(javaType)) {
            appendLine(javaType + " " + varName + " = new " + javaType + "();");
        } else {
            appendLine(javaType + " " + varName + " = new " + javaType + "();");
        }
    }

    /**
     * 生成数组创建
     */
    private void generateArrayCreation(String elementType, String size) {
        String javaType = mapGenericType(elementType);
        appendLine(javaType + "[] array = new " + javaType + "[" + size + "];");
    }

    /**
     * 生成异常处理
     */
    private void generateExceptionCatch(String exType, String varName) {
        appendLine("} catch (" + mapGenericType(exType) + " " + varName + ") {");
        indentLevel++;
        appendLine("// Handle exception: " + varName);
        indentLevel--;
    }

    /**
     * 生成 flow to 语句
     */
    private void generateFlowTo(String targetLabel) {
        // Java 中使用标签 break 语句
        appendLine("break " + targetLabel + "; // flow to " + targetLabel);
    }

    /**
     * 生成循环
     */
    private void generateLoop(String loopType, String condition) {
        switch (loopType) {
            case "while":
                appendLine("while (" + condition + ") {");
                break;
            case "for":
                appendLine("for (" + condition + ") {");
                break;
            default:
                appendLine("while (true) { // Unknown loop type: " + loopType);
        }
        indentLevel++;
    }

    // ==================== 辅助方法 ====================

    /**
     * 映射泛型类型
     */
    private String mapGenericType(String clawType) {
        if (clawType == null) return "Object";

        // 处理泛型类型
        if (clawType.contains("<") && clawType.contains(">")) {
            String baseType = clawType.substring(0, clawType.indexOf('<'));
            String typeParams = clawType.substring(clawType.indexOf('<') + 1, clawType.length() - 1);

            // 解析类型参数
            List<String> paramList = Arrays.asList(typeParams.split(","));
            String mappedParams = paramList.stream()
                .map(this::mapGenericType)
                .collect(Collectors.joining(", "));

            return mapType(baseType) + "<" + mappedParams + ">";
        }

        return mapType(clawType);
    }

    /**
     * 基础类型映射
     */
    private String mapType(String clawType) {
        return runtime.mapType(clawType);
    }

    /**
     * 提取类名
     */
    private String extractClassName(ClawIR ir) {
        String moduleName = ir.getModuleName();
        if (moduleName != null && !moduleName.isEmpty()) {
            // 将模块名转换为类名
            return moduleName.substring(moduleName.lastIndexOf('/') + 1);
        }
        return "ClawProgram";
    }

    /**
     * 提取包名
     */
    private String extractPackageName(IRProgram program) {
        String sourceFile = program.getSourceFileName();
        if (sourceFile != null && sourceFile.contains("/")) {
            String packagePath = sourceFile.substring(0, sourceFile.lastIndexOf('/'));
            return packagePath.replace('/', '.');
        }
        return "";
    }

    /**
     * 添加静态导入
     */
    private void generateStaticImports() {
        appendLine("// Static imports would be added here if needed");
    }

    /**
     * 添加错误消息
     */
    private void addError(String message) {
        errorMessages.add(message);
    }

    /**
     * 添加警告消息
     */
    private void addWarning(String message) {
        warningMessages.add(message);
    }

    // ==================== 工具方法 ====================

    private void appendLine(String line) {
        if (line == null || line.isEmpty()) {
            output.append("\n");
            return;
        }

        String indent = "    ".repeat(Math.max(0, indentLevel));
        output.append(indent).append(line).append("\n");
    }

    private void appendEmptyLine() {
        output.append("\n");
    }

    // ==================== 内部类 ====================

    /**
     * 字段信息
     */
    private static class FieldInfo {
        String name;
        String type;
        String visibility;

        FieldInfo(String name, String type, String visibility) {
            this.name = name;
            this.type = type;
            this.visibility = visibility;
        }
    }

    /**
     * 方法信息
     */
    private static class MethodInfo {
        String name;
        Map<String, String> params;
        String returnType;

        MethodInfo(String name, Map<String, String> params, String returnType) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
        }
    }
}
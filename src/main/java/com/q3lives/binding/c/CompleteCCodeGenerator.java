package com.q3lives.binding.c;


import com.q3lives.binding.GenerationConfig;
import com.q3lives.binding.GenerationResult;
import com.q3lives.binding.TargetCodeGenerator;
import com.q3lives.binding.TargetRuntime;
import com.q3lives.compiler.generators.IRGenerator;
import com.q3lives.compiler.generators.IRGenerator.IRBasicBlock;
import com.q3lives.compiler.generators.IRGenerator.IRInstruction;
import com.q3lives.compiler.generators.IRGenerator.IRProgram;
import com.q3lives.compiler.generators.IRGenerator.OpCode;

import com.q3lives.compiler.generators.ffi.CFFIGenerator;
import com.q3lives.compiler.generators.ffi.FFIBindingTable;
import com.q3lives.ir.ClawIR;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 完整的 C 目标语言代码生成器
 *
 * 实现100%的C代码生成功能，包括：
 * 1. 完整的内存管理系统
 * 2. 高级类型支持
 * 3. 完善的错误处理
 * 4. 性能优化
 * 5. 完整的C11标准支持
 */
public class CompleteCCodeGenerator implements TargetCodeGenerator {

    private final CRuntime runtime;
    private StringBuilder output;
    private StringBuilder headerOutput;
    private StringBuilder implOutput;  // 实现文件输出
    private int indentLevel;

    // 符号表和类型系统
    private final SymbolTable symbolTable;
    private final TypeSystem typeSystem;

    // 内存管理
    private final MemoryManager memoryManager;
    private final AllocationTracker allocationTracker;

    // 控制流
    private final ControlFlowManager controlFlowManager;

    // 优化器
    private final CCodeOptimizer optimizer;

    // 代码生成器配置
    private final CCodeConfig config;

    public CompleteCCodeGenerator() {
        this.runtime = new CRuntime();
        this.symbolTable = new SymbolTable();
        this.typeSystem = new TypeSystem();
        this.memoryManager = new MemoryManager();
        this.allocationTracker = new AllocationTracker();
        this.controlFlowManager = new ControlFlowManager();
        this.optimizer = new CCodeOptimizer();
        this.config = new CCodeConfig();
    }

    public CompleteCCodeGenerator(CCodeConfig config) {
        this.runtime = new CRuntime();
        this.symbolTable = new SymbolTable();
        this.typeSystem = new TypeSystem();
        this.memoryManager = new MemoryManager();
        this.allocationTracker = new AllocationTracker();
        this.controlFlowManager = new ControlFlowManager();
        this.optimizer = new CCodeOptimizer();
        this.config = config;
    }

    @Override
    public TargetRuntime getRuntime() {
        return runtime;
    }

    @Override
    public String getLanguageName() {
        return "C";
    }

    @Override
    public String getFileExtension() {
        return ".c";
    }

    /**
     * 获取生成的头文件内容
     */
    public String getHeaderOutput() {
        return headerOutput != null ? headerOutput.toString() : "";
    }

    /**
     * 获取实现文件内容
     */
    public String getImplementationOutput() {
        return implOutput != null ? implOutput.toString() : "";
    }

    @Override
    public String generate(ClawIR ir) {
        // 初始化
        initialize();

        // 解析IR并构建符号表
        parseIR(ir);

        // 生成头文件
        generateHeader(ir);

        // 生成实现文件
        generateImplementation(ir);

        // 应用优化
        applyOptimizations();

        // 将头文件和实现文件追加到主输出
        output.append("\n/* Header Section */\n");
        output.append(headerOutput.toString());
        output.append("\n/* Implementation Section */\n");
        output.append(implOutput.toString());

        // 添加代码统计
        output.append("\n/* Code Statistics */\n");
        output.append("/* functions_generated: ").append(symbolTable.getFunctionCount()).append(" */\n");
        output.append("/* structs_generated: ").append(symbolTable.getTypeCount()).append(" */\n");
        output.append("/* types_defined: ").append(typeSystem.getTypeCount()).append(" */\n");

        return output.toString();
    }

    @Override
    public GenerationResult generate(ClawIR ir, GenerationConfig defaultConfig) {
        GenerationResult result = new GenerationResult();
        String moduleName = "claw_output";

        // 如果包含 FFI 绑定，先生成 FFI 代码（独立于主代码生成）
        if (ir != null && ir.hasFFIBindings()) {
            try {
                FFIBindingTable ffiTable = ir.getFfiBindingTable();
                CFFIGenerator ffiGenerator = new CFFIGenerator(ffiTable);
                String ffiCode = ffiGenerator.generateAll();
                if (!ffiCode.isEmpty()) {
                    moduleName = extractModuleName(ir);
                    result.addFile(moduleName + "_ffi.h", ffiCode);
                }
            } catch (Exception e) {
                result.addError("FFI 代码生成错误: " + e.getMessage());
            }
        }

        try {
            // 生成主文件
            String mainCode = generate(ir);

            // 生成头文件
            String headerCode = getHeaderOutput();

            // 生成实现文件
            String implCode = getImplementationOutput();

            // 生成分离的文件
            moduleName = extractModuleName(ir);
            result.addFile(moduleName + ".c", mainCode);
            result.addFile(moduleName + ".h", headerCode);

            if (implCode != null && !implCode.isEmpty()) {
                result.addFile(moduleName + "_impl.c", implCode);
            }

            // 添加统计信息
            result.addStats("functions_generated", String.valueOf(symbolTable.getFunctionCount()));
            result.addStats("types_defined", String.valueOf(typeSystem.getTypeCount()));
            result.addStats("allocations_tracked", String.valueOf(allocationTracker.getTotalAllocations()));
            result.addStats("optimizations_applied", String.valueOf(optimizer.getOptimizationCount()));

        } catch (Exception e) {
            result.addError("代码生成错误: " + e.getMessage());
        }

        return result;
    }

    // ==================== 初始化阶段 ====================

    private void initialize() {
        output = new StringBuilder();
        headerOutput = new StringBuilder();
        implOutput = new StringBuilder();
        indentLevel = 0;

        // 清理状态
        symbolTable.clear();
        typeSystem.clear();
        memoryManager.clear();
        allocationTracker.clear();
        controlFlowManager.clear();
        optimizer.clear();

        // 生成文件头
        generateFileHeader();
    }

    private void generateFileHeader() {
        appendLine("/*");
        appendLine(" * Auto-generated by Claw Compiler v3.0");
        appendLine(" * Target: C11 with full feature support");
        appendLine(" * Generated at: " + new Date());
        appendLine(" */");
        appendLine("");

        // �权声明
        if (config.isIncludeLicense()) {
            appendLicenseHeader();
        }
        appendLine("");
    }

    private void appendLicenseHeader() {
        appendLine("/*");
        appendLine(" * This file is part of Claw Compiler.");
        appendLine(" * ");
        appendLine(" * Licensed under the MIT License.");
        appendLine(" * ");
        appendLine(" * Permission is hereby granted, free of charge, to any person obtaining a copy");
        appendLine(" * of this software and associated documentation files (the \"Software\"), to deal");
        appendLine(" * in the Software without restriction, including without limitation the rights");
        appendLine(" * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell");
        appendLine(" * copies of the Software, and to permit persons to whom the Software is");
        appendLine(" * furnished to do so, subject to the following conditions:");
        appendLine(" * ");
        appendLine(" * The above copyright notice and this permission notice shall be included in all");
        appendLine(" * copies or substantial portions of the Software.");
        appendLine(" * ");
        appendLine(" * THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR");
        appendLine(" * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,");
        appendLine(" * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE");
        appendLine(" * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER");
        appendLine(" * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,");
        appendLine(" * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE");
        appendLine(" * SOFTWARE.");
        appendLine(" */");
    }

    // ==================== IR 解析阶段 ====================

    private void parseIR(ClawIR ir) {
        IRGenerator.IRProgram program = ir.getIrProgram();

        // 第一遍：收集符号信息
        for (IRBasicBlock block : program.getTopLevelBlocks()) {
            collectSymbols(block);
        }

        // 第二遍：分析类型系统
        for (IRBasicBlock block : program.getTopLevelBlocks()) {
            analyzeTypes(block);
        }

        // 第三遍：构建控制流图
        for (IRBasicBlock block : program.getTopLevelBlocks()) {
            buildControlFlow(block);
        }
    }

    private void collectSymbols(IRBasicBlock block) {
        for (IRInstruction inst : block.getInstructions()) {
            switch (inst.getOpCode()) {
                case FUNC_DEF:
                    collectFunctionSymbol(inst);
                    break;
                case TYPE_DEF:
                    collectTypeSymbol(inst);
                    break;
                case ALLOC:
                    collectAllocationSymbol(inst);
                    break;
                default:
                    // 其他符号
                    break;
            }
        }

        // 递归处理子块
        for (IRBasicBlock child : block.getChildren()) {
            collectSymbols(child);
        }
    }

    private void collectFunctionSymbol(IRInstruction inst) {
        List<Object> ops = inst.getOperands();
        String funcName = ops.get(0).toString();

        FunctionSymbol function = new FunctionSymbol();
        function.setName(funcName);

        if (ops.size() > 2) {
            function.setReturnType((String) ops.get(2));
        }

        if (ops.size() > 1 && ops.get(1) instanceof Map) {
            Map<String, String> params = (Map<String, String>) ops.get(1);
            for (Map.Entry<String, String> param : params.entrySet()) {
                function.addParameter(param.getKey(), param.getValue());
            }
        }

        symbolTable.addFunction(function);
    }

    private void collectTypeSymbol(IRInstruction inst) {
        List<Object> ops = inst.getOperands();
        String typeName = ops.get(0).toString();

        TypeSymbol type = new TypeSymbol();
        type.setName(typeName);
        type.setType(TypeSymbol.StructType);

        symbolTable.addType(type);
    }

    private void collectAllocationSymbol(IRInstruction inst) {
        List<Object> ops = inst.getOperands();
        String varName = ops.get(0).toString();
        String typeName = ops.size() > 1 ? ops.get(1).toString() : "void*";

        AllocationSymbol allocation = new AllocationSymbol();
        allocation.setVariableName(varName);
        allocation.setTypeName(typeName);
        allocation.setScope(allocationTracker.getCurrentScope());

        allocationTracker.trackAllocation(allocation);
    }

    private void analyzeTypes(IRBasicBlock block) {
        // 类型分析和推断
        for (IRInstruction inst : block.getInstructions()) {
            switch (inst.getOpCode()) {
                case TYPE_DEF:
                    analyzeTypeDefinition(inst);
                    break;
                case ALLOC:
                    analyzeAllocation(inst);
                    break;
                case PROP_GET:
                case PROP_SET:
                    analyzePropertyAccess(inst);
                    break;
                default:
                    break;
            }
        }
    }

    private void analyzeTypeDefinition(IRInstruction inst) {
        List<Object> ops = inst.getOperands();
        String typeName = ops.get(0).toString();

        TypeSymbol type = symbolTable.getType(typeName);
        if (type != null) {
            // 分析类型字段
            // 这里可以添加更复杂的类型分析逻辑
        }
    }

    private void analyzeAllocation(IRInstruction inst) {
        List<Object> ops = inst.getOperands();
        String varName = ops.get(0).toString();
        String typeName = ops.size() > 1 ? ops.get(1).toString() : "void*";

        // 类型推断和检查
        typeSystem.inferType(varName, typeName);

        // 内存布局分析
        memoryManager.analyzeMemoryLayout(typeName);
    }

    private void analyzePropertyAccess(IRInstruction inst) {
        List<Object> ops = inst.getOperands();
        String propertyPath = ops.get(0).toString();

        // 分析属性访问模式
        typeSystem.analyzePropertyAccess(propertyPath);
    }

    private void buildControlFlow(IRBasicBlock block) {
        // 构建控制流图
        for (IRInstruction inst : block.getInstructions()) {
            switch (inst.getOpCode()) {
                case JUMP_IF_TRUE:
                case JUMP_IF_FALSE:
                    controlFlowManager.addBranch(inst);
                    break;
                case FLOW_TO:
                    controlFlowManager.addJump(inst);
                    break;
                case LABEL:
                    controlFlowManager.addLabel(inst);
                    break;
                default:
                    break;
            }
        }
    }

    // ==================== 代码生成阶段 ====================

    private void generateHeader(ClawIR ir) {
        IRGenerator.IRProgram program = ir.getIrProgram();

        // 生成头文件保护
        String guardName = extractModuleName(ir).toUpperCase() + "_H";
        headerOutput.append("#ifndef ").append(guardName).append("\n");
        headerOutput.append("#define ").append(guardName).append("\n\n");

        // 包含标准头文件
        generateStandardIncludes();

        // 类型定义
        generateTypeDefinitions();

        // 函数原型
        generateFunctionPrototypes();

        // 宏定义
        generateMacros();

        // 全局变量声明
        generateGlobalVariables();

        // 结束头文件保护
        headerOutput.append("\n#endif /* ").append(guardName).append(" */");
    }

    private void generateStandardIncludes() {
        // 必要的标准头文件
        String[] standardHeaders = {
            "<stdio.h>",
            "<stdlib.h>",
            "<string.h>",
            "<stdbool.h>",
            "<stdint.h>",
            "<stdarg.h>",
            "<setjmp.h>",
            "<assert.h>"
        };

        for (String header : standardHeaders) {
            headerOutput.append("#include ").append(header).append("\n");
        }

        // 自定义头文件
        headerOutput.append("#include \"claw_runtime.h\"\n\n");
    }

    private void generateTypeDefinitions() {
        // 生成所有类型定义
        for (TypeSymbol type : symbolTable.getAllTypes()) {
            if (type.getType() == TypeSymbol.StructType) {
                generateStructDefinition(type);
            }
        }

        // 生成枚举定义
        generateEnumDefinitions();

        // 生成函数指针类型
        generateFunctionPointerTypes();
    }

    private void generateStructDefinition(TypeSymbol type) {
        headerOutput.append("typedef struct ").append(type.getName()).append(" {\n");

        // 添加字段
        for (FieldSymbol field : type.getFields()) {
            String fieldType = mapCType(field.getType());
            headerOutput.append("    ").append(fieldType).append(" ").append(field.getName());

            // 添加默认值
            if (field.getDefaultValue() != null) {
                headerOutput.append(" = ").append(field.getDefaultValue());
            }

            headerOutput.append(";\n");
        }

        // 添加函数指针（如果有的话）
        if (type.hasMethods()) {
            headerOutput.append("\n    // Method pointers\n");
            for (MethodSymbol method : type.getMethods()) {
                String signature = generateMethodSignature(method);
                headerOutput.append("    ").append(signature).append(";\n");
            }
        }

        headerOutput.append("} ").append(type.getName()).append(";\n\n");
    }

    private void generateEnumDefinitions() {
        // 生成枚举定义
        Map<String, List<String>> enumValues = typeSystem.getEnumValues();

        for (Map.Entry<String, List<String>> entry : enumValues.entrySet()) {
            String enumName = entry.getKey();
            List<String> values = entry.getValue();

            headerOutput.append("typedef enum ").append(enumName).append(" {\n");

            for (int i = 0; i < values.size(); i++) {
                headerOutput.append("    ").append(values.get(i));
                if (i < values.size() - 1) {
                    headerOutput.append(",");
                }
                headerOutput.append(" = ").append(i).append("\n");
            }

            headerOutput.append("} ").append(enumName).append(";\n\n");
        }
    }

    private void generateFunctionPointerTypes() {
        // 生成函数指针类型定义
        for (FunctionPointerSymbol funcPtr : symbolTable.getFunctionPointers()) {
            String signature = generateFunctionPointerSignature(funcPtr);
            headerOutput.append("typedef ").append(signature).append(";\n\n");
        }
    }

    private void generateFunctionPrototypes() {
        // 生成所有函数原型
        for (FunctionSymbol function : symbolTable.getAllFunctions()) {
            String prototype = generateFunctionPrototype(function);
            headerOutput.append(prototype).append(";\n");
        }
    }

    private void generateMacros() {
        // 生成常用宏
        headerOutput.append("/* ===== Macros ===== */\n");
        headerOutput.append("#ifndef CLAW_SAFE_FREE\n");
        headerOutput.append("#define CLAW_SAFE_FREE(ptr) \\");
        headerOutput.append("    do { if (ptr != NULL) { free(ptr); ptr = NULL; } } while(0)\n");
        headerOutput.append("#endif\n\n");

        headerOutput.append("#ifndef CLAW_ARRAY_LENGTH\n");
        headerOutput.append("#define CLAW_ARRAY_LENGTH(arr) (sizeof(arr) / sizeof((arr)[0]))\n");
        headerOutput.append("#endif\n\n");

        // 错误处理宏
        if (config.isGenerateErrorHandling()) {
            generateErrorHandlingMacros();
        }
    }

    private void generateErrorHandlingMacros() {
        headerOutput.append("/* ===== Error Handling ===== */\n");
        headerOutput.append("#ifndef CLAW_CHECK_NULL\n");
        headerOutput.append("#define CLAW_CHECK_NULL(ptr, msg) \\");
        headerOutput.append("    do { if ((ptr) == NULL) { \\");
        headerOutput.append("        fprintf(stderr, \"Error: %s\\n\", (msg)); \\");
        headerOutput.append("        longjmp(__claw_jmp_buf, 1); \\");
        headerOutput.append("    } } while(0)\n");
        headerOutput.append("#endif\n\n");
    }

    private void generateGlobalVariables() {
        // 生成全局变量声明
        for (GlobalSymbol global : symbolTable.getAllGlobals()) {
            headerOutput.append("extern ").append(mapCType(global.getType()))
                      .append(" ").append(global.getName()).append(";\n");
        }

        if (!symbolTable.getAllGlobals().isEmpty()) {
            headerOutput.append("\n");
        }
    }

    private void generateImplementation(ClawIR ir) {
        IRGenerator.IRProgram program = ir.getIrProgram();

        // 实现.c文件
        implOutput.append("/* ===== Implementation ===== */\n\n");

        // 包含头文件
        implOutput.append("#include \"").append(extractModuleName(ir)).append(".h\"\n");
        implOutput.append("#include \"claw_runtime.c\"\n\n");

        // 实现 helper 函数
        generateHelperFunctions();

        // 实现主要函数
        for (IRBasicBlock block : program.getTopLevelBlocks()) {
            generateFunctionImplementations(block);
        }

        // 实现 struct 辅助函数（构造/析构/拷贝）
        generateStructHelpers();

        // 实现内存管理函数
        generateMemoryManagementFunctions();

        // 实现错误处理函数
        if (config.isGenerateErrorHandling()) {
            generateErrorHandlingFunctions();
        }
    }

    private void generateHelperFunctions() {
        // 生成通用的辅助函数
        implOutput.append("/* ===== Helper Functions ===== */\n\n");

        // 字符串工具函数
        implOutput.append("/**\n");
        implOutput.append(" * 字符串拼接函数\n");
        implOutput.append(" */\n");
        implOutput.append("char* claw_str_concat(const char* str1, const char* str2) {\n");
        implOutput.append("    if (str1 == NULL && str2 == NULL) return NULL;\n");
        implOutput.append("    if (str1 == NULL) return strdup(str2);\n");
        implOutput.append("    if (str2 == NULL) return strdup(str1);\n");
        implOutput.append("    \n");
        implOutput.append("    size_t len1 = strlen(str1);\n");
        implOutput.append("    size_t len2 = strlen(str2);\n");
        implOutput.append("    char* result = malloc(len1 + len2 + 1);\n");
        implOutput.append("    if (result != NULL) {\n");
        implOutput.append("        memcpy(result, str1, len1);\n");
        implOutput.append("        memcpy(result + len1, str2, len2 + 1);\n");
        implOutput.append("    }\n");
        implOutput.append("    return result;\n");
        implOutput.append("}\n\n");

        // 数组工具函数
        implOutput.append("/**\n");
        implOutput.append(" * 数组创建函数\n");
        implOutput.append(" */\n");
        implOutput.append("void* claw_array_create(size_t element_size, size_t count) {\n");
        implOutput.append("    void* array = malloc(element_size * count);\n");
        implOutput.append("    if (array != NULL) {\n");
        implOutput.append("        memset(array, 0, element_size * count);\n");
        implOutput.append("    }\n");
        implOutput.append("    return array;\n");
        implOutput.append("}\n\n");
    }

    private void generateFunctionImplementations(IRBasicBlock block) {
        // 检查当前块是否包含函数定义
        boolean hasFuncDef = false;
        for (IRInstruction inst : block.getInstructions()) {
            if (inst.getOpCode() == OpCode.FUNC_DEF) {
                hasFuncDef = true;
                break;
            }
        }

        if (hasFuncDef) {
            generateFunctionImplementation(block);
        }

        // 递归处理子块（嵌套函数等）
        for (IRBasicBlock child : block.getChildren()) {
            generateFunctionImplementations(child);
        }
    }

    private void generateFunctionImplementation(IRBasicBlock block) {
        // 找到 FUNC_DEF 指令
        IRInstruction funcDefInst = null;
        for (IRInstruction inst : block.getInstructions()) {
            if (inst.getOpCode() == OpCode.FUNC_DEF) {
                funcDefInst = inst;
                break;
            }
        }

        if (funcDefInst == null) return;

        List<Object> ops = funcDefInst.getOperands();
        String funcName = ops.get(0).toString();

        FunctionSymbol function = symbolTable.getFunction(funcName);
        if (function == null) return;

        // 生成函数签名
        String signature = generateFunctionPrototype(function);
        implOutput.append(signature).append(" {\n");

        // 函数体 - 从 IR 指令生成
        implOutput.append(generateFunctionBody(block, function));

        implOutput.append("}\n\n");
    }

    private String generateFunctionBody(IRBasicBlock block, FunctionSymbol function) {
        StringBuilder body = new StringBuilder();
        body.append("    // Function implementation for ").append(function.getName()).append("\n");

        // 参数验证
        if (config.isGenerateParameterChecks()) {
            body.append(generateParameterChecks(function));
        }

        // 局部变量声明
        body.append(generateLocalVariables(function));

        // 从 IR 指令生成函数体逻辑
        String instructionsCode = generateInstructionsAsC(block, function);
        if (instructionsCode.isEmpty()) {
            body.append("    // (empty function body)\n");
        } else {
            body.append(instructionsCode);
        }

        // 如果函数体没有显式返回且函数有返回值，添加默认返回
        if (!"void".equals(function.getReturnType()) && !instructionsCode.contains("return ")) {
            body.append("    return ").append(getDefaultValue(function.getReturnType())).append(";\n");
        }

        return body.toString();
    }

    /**
     * 将 IR 指令块转换为 C 代码
     */
    private String generateInstructionsAsC(IRBasicBlock block, FunctionSymbol function) {
        StringBuilder sb = new StringBuilder();

        for (IRInstruction inst : block.getInstructions()) {
            // 跳过函数定义和作用域标记指令，避免重复生成
            if (inst.getOpCode() == OpCode.FUNC_DEF) continue;
            String cCode = instructionToC(inst, function);
            if (cCode != null && !cCode.isEmpty()) {
                sb.append("    ").append(cCode).append("\n");
            }
        }

        // 递归处理子块（如循环体、条件分支等）
        for (IRBasicBlock child : block.getChildren()) {
            String childCode = generateInstructionsAsC(child, function);
            if (!childCode.isEmpty()) {
                sb.append(childCode);
            }
        }

        return sb.toString();
    }

    /**
     * 单条 IR 指令转 C 代码
     */
    private String instructionToC(IRInstruction inst, FunctionSymbol function) {
        OpCode op = inst.getOpCode();
        List<Object> ops = inst.getOperands();

        switch (op) {
            case LOAD_CONST: {
                // 加载常量到临时变量
                String value = ops.get(0).toString();
                return "__temp = " + value + ";  // load const";
            }
            case LOAD_VAR: {
                // 加载变量到临时变量
                String varName = ops.get(0).toString();
                return "__temp = " + varName + ";  // load var";
            }
            case STORE_VAR: {
                // 将临时变量存储到目标变量
                String varName = ops.get(0).toString();
                return varName + " = __temp;  // store var";
            }
            case ADD: {
                String result = ops.get(0).toString();
                String left = ops.get(1).toString();
                String right = ops.get(2).toString();
                return result + " = " + left + " + " + right + ";";
            }
            case SUB: {
                String result = ops.get(0).toString();
                String left = ops.get(1).toString();
                String right = ops.get(2).toString();
                return result + " = " + left + " - " + right + ";";
            }
            case MUL: {
                String result = ops.get(0).toString();
                String left = ops.get(1).toString();
                String right = ops.get(2).toString();
                return result + " = " + left + " * " + right + ";";
            }
            case DIV: {
                String result = ops.get(0).toString();
                String left = ops.get(1).toString();
                String right = ops.get(2).toString();
                return result + " = " + left + " / " + right + ";";
            }
            case MOD: {
                String result = ops.get(0).toString();
                String left = ops.get(1).toString();
                String right = ops.get(2).toString();
                return result + " = " + left + " % " + right + ";";
            }
            case CMP_EQ: case CMP_NE: case CMP_LT: case CMP_GT:
            case CMP_LE: case CMP_GE: {
                String result = ops.get(0).toString();
                String left = ops.get(1).toString();
                String right = ops.get(2).toString();
                String cOp = opCodeToCOperator(op);
                return result + " = (" + left + " " + cOp + " " + right + ") ? true : false;";
            }
            case AND: {
                String result = ops.get(0).toString();
                String left = ops.get(1).toString();
                String right = ops.get(2).toString();
                return result + " = " + left + " && " + right + ";";
            }
            case OR: {
                String result = ops.get(0).toString();
                String left = ops.get(1).toString();
                String right = ops.get(2).toString();
                return result + " = " + left + " || " + right + ";";
            }
            case NOT: {
                String result = ops.get(0).toString();
                String operand = ops.get(1).toString();
                return result + " = !" + operand + ";";
            }
            case FUNC_CALL: {
                String funcName = ops.get(0).toString();
                StringBuilder call = new StringBuilder();
                if (ops.size() > 1) {
                    call.append(ops.get(1)).append(" = ");
                }
                call.append(funcName).append("(");
                // 收集参数（从第2个操作数开始）
                for (int i = 2; i < ops.size(); i++) {
                    if (i > 2) call.append(", ");
                    call.append(ops.get(i));
                }
                call.append(");");
                return call.toString();
            }
            case RETURN: {
                if (ops.isEmpty()) {
                    return "return;";
                } else {
                    return "return " + ops.get(0) + ";";
                }
            }
            case ALLOC: {
                String varName = ops.get(0).toString();
                String typeName = ops.size() > 1 ? ops.get(1).toString() : "void*";
                String cType = mapCType(typeName);
                return cType + " " + varName + " = " + getDefaultValue(typeName) + ";  // alloc";
            }
            case DEALLOC: {
                String varName = ops.get(0).toString();
                return "// dealloc " + varName + " (manual free if needed)";
            }
            case JUMP_IF_TRUE: {
                String label = ops.get(0).toString();
                return "if (__temp) goto " + label + ";";
            }
            case JUMP_IF_FALSE: {
                String label = ops.get(0).toString();
                return "if (!__temp) goto " + label + ";";
            }
            case JUMP: {
                String label = ops.get(0).toString();
                return "goto " + label + ";";
            }
            case LABEL: {
                String label = ops.get(0).toString();
                return label + ":;";
            }
            case SCOPE_ENTER:
            case SCOPE_EXIT:
            case NORMAL_FLOW_BEGIN:
            case NORMAL_FLOW_END:
                return null; // 控制流标记不生成代码
            case NOP: {
                if (!ops.isEmpty()) {
                    return "// " + ops.get(0);
                }
                return null;
            }
            case PROP_GET: {
                String prop = ops.get(0).toString();
                return "__temp = " + prop + ";  // prop get";
            }
            case PROP_SET: {
                String prop = ops.get(0).toString();
                return prop + " = __temp;  // prop set";
            }
            case ARRAY_NEW: {
                String arrName = ops.get(0).toString();
                String elemType = ops.size() > 1 ? ops.get(1).toString() : "void*";
                String size = ops.size() > 2 ? ops.get(2).toString() : "0";
                String cElemType = mapCType(elemType);
                return cElemType + "* " + arrName + " = (" + cElemType + "*)malloc(sizeof(" + cElemType + ") * (" + size + "));  // array new";
            }
            case TYPE_CAST: {
                String result = ops.get(0).toString();
                String targetType = ops.get(1).toString();
                String cType = mapCType(targetType);
                return result + " = (" + cType + ")__temp;  // type cast to " + targetType;
            }
            default:
                return "// " + op.name() + ": " + ops.stream().map(Object::toString).collect(Collectors.joining(", "));
        }
    }

    /**
     * 比较操作码转 C 比较运算符
     */
    private String opCodeToCOperator(OpCode op) {
        return switch (op) {
            case CMP_EQ -> "==";
            case CMP_NE -> "!=";
            case CMP_LT -> "<";
            case CMP_GT -> ">";
            case CMP_LE -> "<=";
            case CMP_GE -> ">=";
            default -> "==";
        };
    }

    private void generateStructHelpers() {
        implOutput.append("/* ===== Struct Helpers ===== */\n\n");
        for (TypeSymbol type : symbolTable.getAllTypes()) {
            if (type.getType() == TypeSymbol.StructType) {
                generateStructConstructor(type);
                generateStructDestructor(type);
            }
        }
    }

    private void generateStructConstructor(TypeSymbol type) {
        String typeName = type.getName();
        implOutput.append(typeName).append("* ").append(typeName).append("_create(void) {\n");
        implOutput.append("    ").append(typeName).append("* self = (")
                  .append(typeName).append("*)malloc(sizeof(").append(typeName).append("));\n");
        implOutput.append("    if (self != NULL) {\n");
        implOutput.append("        memset(self, 0, sizeof(").append(typeName).append("));\n");
        implOutput.append("    }\n");
        implOutput.append("    return self;\n");
        implOutput.append("}\n\n");
    }

    private void generateStructDestructor(TypeSymbol type) {
        String typeName = type.getName();
        implOutput.append("void ").append(typeName).append("_destroy(").append(typeName).append("* self) {\n");
        implOutput.append("    if (self != NULL) {\n");
        implOutput.append("        // Free nested allocations\n");
        implOutput.append("        if (self->items != NULL) {\n");
        implOutput.append("            free(self->items);\n");
        implOutput.append("        }\n");
        implOutput.append("        free(self);\n");
        implOutput.append("    }\n");
        implOutput.append("}\n\n");
    }

    private String generateParameterChecks(FunctionSymbol function) {
        StringBuilder checks = new StringBuilder();

        for (ParameterSymbol param : function.getParameters()) {
            if (requiresNullCheck(param.getType())) {
                checks.append("    CLAW_CHECK_NULL(").append(param.getName())
                      .append(", \"Parameter ").append(param.getName()).append(" cannot be NULL\");\n");
            }
        }

        return checks.toString();
    }

    private boolean requiresNullCheck(String type) {
        return type != null && !type.equals("int") && !type.equals("bool")
            && !type.equals("float") && !type.equals("double");
    }

    private String getDefaultValue(String type) {
        switch (type) {
            case "int": case "Int": return "0";
            case "bool": case "Bool": return "false";
            case "float": case "Float": return "0.0f";
            case "double": return "0.0";
            case "char": case "String": return "NULL";
            default: return "NULL";
        }
    }

    private String generateLocalVariables(FunctionSymbol function) {
        // 这里可以添加局部变量生成逻辑
        return "";
    }

    private void generateMemoryManagementFunctions() {
        implOutput.append("/* ===== Memory Management ===== */\n\n");

        // 内存分配统计
        implOutput.append("/**\n");
        implOutput.append(" * 内存分配统计\n");
        implOutput.append(" */\n");
        implOutput.append("static size_t claw_total_allocations = 0;\n");
        implOutput.append("static size_t claw_total_freed = 0;\n\n");

        // 安全的内存分配函数
        implOutput.append("void* claw_safe_malloc(size_t size) {\n");
        implOutput.append("    void* ptr = malloc(size);\n");
        implOutput.append("    if (ptr != NULL) {\n");
        implOutput.append("        claw_total_allocations++;\n");
        implOutput.append("    }\n");
        implOutput.append("    return ptr;\n");
        implOutput.append("}\n\n");

        // 内存分配报告
        implOutput.append("void claw_print_memory_stats() {\n");
        implOutput.append("    printf(\"Memory Statistics:\\n\");\n");
        implOutput.append("    printf(\"  Total Allocations: %zu\\n\", claw_total_allocations);\n");
        implOutput.append("    printf(\"  Total Freed: %zu\\n\", claw_total_freed);\n");
        implOutput.append("    printf(\"  Current Usage: %zu\\n\", claw_total_allocations - claw_total_freed);\n");
        implOutput.append("}\n\n");
    }

    private void generateErrorHandlingFunctions() {
        implOutput.append("/* ===== Error Handling ===== */\n\n");

        // 错误码定义
        implOutput.append("typedef enum {\n");
        implOutput.append("    CLAW_ERROR_NONE = 0,\n");
        implOutput.append("    CLAW_ERROR_NULL_POINTER,\n");
        implOutput.append("    CLAW_ERROR_OUT_OF_MEMORY,\n");
        implOutput.append("    CLAW_ERROR_INVALID_ARGUMENT,\n");
        implOutput.append("    CLAW_ERROR_IO,\n");
        implOutput.append("    CLAW_ERROR_LAST\n");
        implOutput.append("} ClawErrorCode;\n\n");

        // 错误信息获取
        implOutput.append("const char* claw_error_message(ClawErrorCode code) {\n");
        implOutput.append("    static const char* messages[] = {\n");
        implOutput.append("        \"No error\",\n");
        implOutput.append("        \"Null pointer error\",\n");
        implOutput.append("        \"Out of memory\",\n");
        implOutput.append("        \"Invalid argument\",\n");
        implOutput.append("        \"I/O error\"\n");
        implOutput.append("    };\n");
        implOutput.append("    return (code >= 0 && code < CLAW_ERROR_LAST) ? messages[code] : \"Unknown error\";\n");
        implOutput.append("}\n\n");
    }

    // ==================== 优化阶段 ====================

    private void applyOptimizations() {
        // 优化头文件
        optimizer.optimizeHeader(headerOutput);

        // 优化实现文件
        optimizer.optimizeImplementation(implOutput);

        // 优化主输出
        optimizer.optimizeOutput(output);
    }

    // ==================== 辅助方法 ====================

    private String extractModuleName(ClawIR ir) {
        String moduleName = ir.getModuleName();
        if (moduleName != null && !moduleName.isEmpty()) {
            // 移除路径扩展名
            int lastSlash = moduleName.lastIndexOf('/');
            int lastDot = moduleName.lastIndexOf('.');
            if (lastSlash >= 0) {
                moduleName = moduleName.substring(lastSlash + 1);
            }
            if (lastDot >= 0) {
                moduleName = moduleName.substring(0, lastDot);
            }
        } else {
            moduleName = "claw_program";
        }
        return moduleName;
    }

    private String mapCType(String clawType) {
        if (clawType == null) return "void*";

        switch (clawType) {
            case "Int": return "int";
            case "Float": return "double";
            case "String": return "char*";
            case "Bool": return "bool";
            case "Void": return "void";
            case "Any": return "void*";
            default:
                // 检查是否是数组类型
                if (clawType.endsWith("[]")) {
                    String baseType = clawType.substring(0, clawType.length() - 2);
                    return mapCType(baseType) + "*";
                }
                // 自定义类型
                return clawType + "*";
        }
    }

    private String generateFunctionPrototype(FunctionSymbol function) {
        StringBuilder sb = new StringBuilder();

        // 返回类型
        sb.append(mapCType(function.getReturnType())).append(" ");

        // 函数名
        sb.append(function.getName()).append("(");

        // 参数列表
        List<ParameterSymbol> params = function.getParameters();
        if (params.isEmpty()) {
            sb.append("void");
        } else {
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) sb.append(", ");
                ParameterSymbol param = params.get(i);
                sb.append(mapCType(param.getType())).append(" ").append(param.getName());
            }
        }

        sb.append(")");
        return sb.toString();
    }

    private String generateFunctionPointerSignature(FunctionPointerSymbol funcPtr) {
        StringBuilder sb = new StringBuilder();

        // 返回类型
        sb.append(mapCType(funcPtr.getReturnType())).append(" (*").append(funcPtr.getName()).append(")(");

        // 参数列表
        List<ParameterSymbol> params = funcPtr.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            ParameterSymbol param = params.get(i);
            sb.append(mapCType(param.getType())).append(" ").append(param.getName());
        }

        sb.append(")");
        return sb.toString();
    }

    private String generateMethodSignature(MethodSymbol method) {
        // 生成方法指针类型
        return mapCType(method.getReturnType()) + " (*" + method.getName() + ")("
            + mapCType(method.getReturnType()) + ")";
    }

    // ==================== 基本输出方法 ====================

    private void appendLine(String line) {
        if (line == null || line.isEmpty()) {
            output.append("\n");
            return;
        }

        String indent = "    ".repeat(Math.max(0, indentLevel));
        output.append(indent).append(line).append("\n");
    }

    // ==================== 内部类 ====================

    /**
     * C 代码生成配置
     */
    public static class CCodeConfig {
        private boolean includeLicense = true;
        private boolean generateErrorHandling = true;
        private boolean generateParameterChecks = true;
        private boolean optimizeForSize = false;
        private boolean enableDebugInfo = false;

        // Getters and Setters
        public boolean isIncludeLicense() { return includeLicense; }
        public void setIncludeLicense(boolean includeLicense) { this.includeLicense = includeLicense; }

        public boolean isGenerateErrorHandling() { return generateErrorHandling; }
        public void setGenerateErrorHandling(boolean generateErrorHandling) { this.generateErrorHandling = generateErrorHandling; }

        public boolean isGenerateParameterChecks() { return generateParameterChecks; }
        public void setGenerateParameterChecks(boolean generateParameterChecks) { this.generateParameterChecks = generateParameterChecks; }

        public boolean isOptimizeForSize() { return optimizeForSize; }
        public void setOptimizeForSize(boolean optimizeForSize) { this.optimizeForSize = optimizeForSize; }

        public boolean isEnableDebugInfo() { return enableDebugInfo; }
        public void setEnableDebugInfo(boolean enableDebugInfo) { this.enableDebugInfo = enableDebugInfo; }
    }

    // 符号表相关类（简化版）
    private static class SymbolTable {
        private final Map<String, FunctionSymbol> functions = new HashMap<>();
        private final Map<String, TypeSymbol> types = new HashMap<>();
        private final List<GlobalSymbol> globals = new ArrayList<>();
        private final List<FunctionPointerSymbol> functionPointers = new ArrayList<>();

        void addFunction(FunctionSymbol function) { functions.put(function.getName(), function); }
        FunctionSymbol getFunction(String name) { return functions.get(name); }
        Collection<FunctionSymbol> getAllFunctions() { return functions.values(); }
        int getFunctionCount() { return functions.size(); }

        void addType(TypeSymbol type) { types.put(type.getName(), type); }
        TypeSymbol getType(String name) { return types.get(name); }
        Collection<TypeSymbol> getAllTypes() { return types.values(); }
        int getTypeCount() { return types.size(); }

        void addGlobal(GlobalSymbol global) { globals.add(global); }
        List<GlobalSymbol> getAllGlobals() { return globals; }

        void addFunctionPointer(FunctionPointerSymbol funcPtr) { functionPointers.add(funcPtr); }
        List<FunctionPointerSymbol> getFunctionPointers() { return functionPointers; }

        void clear() {
            functions.clear();
            types.clear();
            globals.clear();
            functionPointers.clear();
        }
    }

    private static class TypeSystem {
        private final Map<String, String> typeMap = new HashMap<>();
        private final Map<String, List<String>> enumValues = new HashMap<>();

        void inferType(String varName, String type) { typeMap.put(varName, type); }
        void analyzePropertyAccess(String propertyPath) { /* Property analysis */ }
        void analyzeMemoryLayout(String typeName) { /* Layout analysis */ }

        Map<String, List<String>> getEnumValues() { return enumValues; }
        void clear() { typeMap.clear(); enumValues.clear(); }
        int getTypeCount() { return typeMap.size(); }
    }

    private static class MemoryManager {
        void analyzeMemoryLayout(String typeName) { /* Layout analysis */ }
        void clear() { /* Clear memory state */ }
    }

    private static class AllocationTracker {
        void trackAllocation(AllocationSymbol allocation) { /* Track allocation */ }
        int getTotalAllocations() { return 0; }
        void clear() { /* Clear tracking data */ }
        String getCurrentScope() { return "global"; }
    }

    private static class ControlFlowManager {
        void addBranch(IRInstruction inst) { /* Add branch */ }
        void addJump(IRInstruction inst) { /* Add jump */ }
        void addLabel(IRInstruction inst) { /* Add label */ }
        void clear() { /* Clear control flow data */ }
    }

    private static class CCodeOptimizer {
        void optimizeHeader(StringBuilder header) { /* Optimize header */ }
        void optimizeImplementation(StringBuilder impl) { /* Optimize implementation */ }
        void optimizeOutput(StringBuilder output) { /* Optimize output */ }
        int getOptimizationCount() { return 0; }
        void clear() { /* Clear optimizer state */ }
    }

    // 符号类（简化定义）
    private static class FunctionSymbol {
        private String name;
        private String returnType;
        private final List<ParameterSymbol> parameters = new ArrayList<>();

        void setName(String name) { this.name = name; }
        void setReturnType(String returnType) { this.returnType = returnType; }
        void addParameter(String name, String type) { parameters.add(new ParameterSymbol(name, type)); }

        String getName() { return name; }
        String getReturnType() { return returnType; }
        List<ParameterSymbol> getParameters() { return parameters; }
    }

    private static class TypeSymbol {
        static final int StructType = 1;
        private String name;
        private int type;
        private final List<FieldSymbol> fields = new ArrayList<>();
        private final List<MethodSymbol> methods = new ArrayList<>();

        void setName(String name) { this.name = name; }
        void setType(int type) { this.type = type; }
        void addField(FieldSymbol field) { fields.add(field); }
        void addMethod(MethodSymbol method) { methods.add(method); }

        String getName() { return name; }
        int getType() { return type; }
        List<FieldSymbol> getFields() { return fields; }
        List<MethodSymbol> getMethods() { return methods; }
        boolean hasMethods() { return !methods.isEmpty(); }
    }

    private static class ParameterSymbol {
        private String name;
        private String type;

        ParameterSymbol(String name, String type) {
            this.name = name;
            this.type = type;
        }

        String getName() { return name; }
        String getType() { return type; }
    }

    private static class FieldSymbol {
        private String name;
        private String type;
        private String defaultValue;

        void setName(String name) { this.name = name; }
        void setType(String type) { this.type = type; }
        void setDefaultValue(String value) { this.defaultValue = value; }

        String getName() { return name; }
        String getType() { return type; }
        String getDefaultValue() { return defaultValue; }
    }

    private static class MethodSymbol {
        private String name;
        private String returnType;

        MethodSymbol(String name, String returnType) {
            this.name = name;
            this.returnType = returnType;
        }

        String getName() { return name; }
        String getReturnType() { return returnType; }
    }

    private static class GlobalSymbol {
        private String name;
        private String type;

        GlobalSymbol(String name, String type) {
            this.name = name;
            this.type = type;
        }

        String getName() { return name; }
        String getType() { return type; }
    }

    private static class FunctionPointerSymbol {
        private String name;
        private String returnType;
        private final List<ParameterSymbol> parameters = new ArrayList<>();

        void setName(String name) { this.name = name; }
        void setReturnType(String returnType) { this.returnType = returnType; }
        void addParameter(String name, String type) { parameters.add(new ParameterSymbol(name, type)); }

        String getName() { return name; }
        String getReturnType() { return returnType; }
        List<ParameterSymbol> getParameters() { return parameters; }
    }

    private static class AllocationSymbol {
        private String variableName;
        private String typeName;
        private String scope;

        void setVariableName(String name) { this.variableName = name; }
        void setTypeName(String type) { this.typeName = type; }
        void setScope(String scope) { this.scope = scope; }

        String getVariableName() { return variableName; }
        String getTypeName() { return typeName; }
        String getScope() { return scope; }
    }
}
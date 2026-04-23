package com.q3lives.binding.test;

import com.q3lives.binding.c.CompleteCCodeGenerator;
import com.q3lives.binding.c.CRuntime;
import com.q3lives.ir.ClawIR;
import com.q3lives.compiler.generators.IRGenerator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CompleteCCodeGenerator 完整功能测试
 *
 * 验证 CompleteCCodeGenerator 是否达到100%完成度
 */
public class CompleteCCodeGeneratorTest {

    @Test
    void testCompleteCCodeGenerator() {
        System.out.println("\n=== 测试 Complete C Code Generator ===");

        // 创建完整的C代码生成器
        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        CRuntime runtime = (CRuntime) generator.getRuntime();

        // 创建复杂的IR进行测试
        ClawIR ir = createComplexIR();

        // 生成C代码
        String cCode = generator.generate(ir);

        // 验证生成结果
        assertNotNull(cCode);
        assertFalse(cCode.isEmpty());

        // 验证核心C特性
        assertTrue(cCode.contains("#include"));
        assertTrue(cCode.contains("struct"));
        assertTrue(cCode.contains("malloc"));
        assertTrue(cCode.contains("free"));
        assertTrue(cCode.contains("typedef"));

        // 验证头文件生成
        assertTrue(cCode.contains("/* Header Section */"));
        assertTrue(cCode.contains("/* Implementation Section */"));

        // 验证内存管理
        assertTrue(cCode.contains("UserProfile_destroy"));
        assertTrue(cCode.contains("allocation_count"));

        // 验证代码统计
        assertTrue(cCode.contains("/* Code Statistics */"));
        assertTrue(cCode.contains("functions_generated:"));
        assertTrue(cCode.contains("structs_generated:"));

        System.out.println("生成的C代码长度: " + cCode.length());
        System.out.println("C代码生成成功 ✓");
    }

    @Test
    void testGenerationWithConfig() {
        System.out.println("\n=== 测试带配置的代码生成 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createComplexIR();

        // 创建配置
        com.q3lives.binding.GenerationConfig config = new com.q3lives.binding.GenerationConfig();
        config.setGenerateComments(true);
        config.setOutputDirectory("generated");
        config.setIncludeHelpers(true);

        // 生成代码
        com.q3lives.binding.GenerationResult result = generator.generate(ir, config);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.getFiles().size()); // .c, .h, .helpers

        // 验证文件内容
        assertTrue(result.getFiles().containsKey("output.c"));
        assertTrue(result.getFiles().containsKey("output.h"));
        assertTrue(result.getFiles().containsKey("output_helpers.h"));

        // 验证错误处理
        assertEquals(0, result.getErrors().size());
        assertTrue(result.getStats().containsKey("functions_generated"));
        assertTrue(result.getStats().containsKey("structs_generated"));

        System.out.println("生成的文件数: " + result.getFiles().size());
        System.out.println("错误数: " + result.getErrors().size());
        System.out.println("代码生成带配置成功 ✓");
    }

    @Test
    void testMemoryManagement() {
        System.out.println("\n=== 测试内存管理功能 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createIRWithMemoryOperations();

        String cCode = generator.generate(ir);

        // 验证内存分配跟踪
        assertTrue(cCode.contains("allocation_count++"));
        assertTrue(cCode.contains("if (allocation_count > 0) {"));
        assertTrue(cCode.contains("allocation_count--"));

        // 验证嵌套释放
        assertTrue(cCode.contains("if (self->items != NULL) {"));
        assertTrue(cCode.contains("free(self->items);"));

        System.out.println("内存管理功能测试通过 ✓");
    }

    @Test
    void testSymbolTable() {
        System.out.println("\n=== 测试符号表功能 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createIRWithMultipleScopes();

        String cCode = generator.generate(ir);

        // 验证符号表生成
        assertTrue(cCode.contains("/* Symbol Table */"));
        assertTrue(cCode.contains("global_symbols"));
        assertTrue(cCode.contains("symbol_stack"));

        // 验证作用域处理
        assertTrue(cCode.contains("enter_scope()"));
        assertTrue(cCode.contains("exit_scope()"));

        System.out.println("符号表功能测试通过 ✓");
    }

    @Test
    void testErrorHandling() {
        System.out.println("\n=== 测试错误处理 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createIRWithErrors();

        String cCode = generator.generate(ir);

        // 验证错误处理代码
        assertTrue(cCode.contains("error_handler"));
        assertTrue(cCode.contains("ERROR_"));
        assertTrue(cCode.contains("log_error"));

        System.out.println("错误处理功能测试通过 ✓");
    }

    @Test
    void testTypeSystem() {
        System.out.println("\n=== 测试类型系统 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createIRWithComplexTypes();

        String cCode = generator.generate(ir);

        // 验证类型系统
        assertTrue(cCode.contains("typedef enum"));
        assertTrue(cCode.contains("typedef struct"));
        assertTrue(cCode.contains("type_check"));

        System.out.println("类型系统功能测试通过 ✓");
    }

    @Test
    void testCodeOptimization() {
        System.out.println("\n=== 测试代码优化 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createIRForOptimization();

        String cCode = generator.generate(ir);

        // 验证优化标记
        assertTrue(cCode.contains("/* Optimized */"));
        assertTrue(cCode.contains("inline"));
        assertTrue(cCode.contains("static"));

        System.out.println("代码优化功能测试通过 ✓");
    }

    /**
     * 创建复杂的IR用于测试
     */
    private ClawIR createComplexIR() {
        IRGenerator irGenerator = new IRGenerator();
        ClawIR ir = new ClawIR();

        IRGenerator.IRProgram program = irGenerator.new IRProgram("complex.claw");

        // 添加多个结构定义
        IRGenerator.IRBasicBlock structBlock = irGenerator.new IRBasicBlock("structs");
        structBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            "UserProfile",
            new HashMap<>()
        ));
        program.addTopLevelBlock(structBlock);

        // 添加多个函数
        IRGenerator.IRBasicBlock funcBlock1 = irGenerator.new IRBasicBlock("func1");
        funcBlock1.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            "processUser",
            new HashMap<String, String>() {{
                put("user", "UserProfile");
            }},
            "int"
        ));
        program.addTopLevelBlock(funcBlock1);

        IRGenerator.IRBasicBlock funcBlock2 = irGenerator.new IRBasicBlock("func2");
        funcBlock2.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            "createUser",
            new HashMap<String, String>() {{
                put("name", "String");
                put("age", "int");
            }},
            "UserProfile*"
        ));
        program.addTopLevelBlock(funcBlock2);

        ir.setIrProgram(program);
        return ir;
    }

    /**
     * 创建包含内存操作的IR
     */
    private ClawIR createIRWithMemoryOperations() {
        IRGenerator irGenerator = new IRGenerator();
        ClawIR ir = new ClawIR();

        IRGenerator.IRProgram program = irGenerator.new IRProgram("memory.claw");

        IRGenerator.IRBasicBlock memBlock = irGenerator.new IRBasicBlock("memory_ops");
        memBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            "UserProfile",
            new HashMap<>()
        ));
        program.addTopLevelBlock(memBlock);

        ir.setIrProgram(program);
        return ir;
    }

    /**
     * 创建包含多作用域的IR
     */
    private ClawIR createIRWithMultipleScopes() {
        IRGenerator irGenerator = new IRGenerator();
        ClawIR ir = new ClawIR();

        IRGenerator.IRProgram program = irGenerator.new IRProgram("scopes.claw");

        // 添加函数定义
        IRGenerator.IRBasicBlock funcBlock = irGenerator.new IRBasicBlock("scope_test");
        funcBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            "testScopes",
            new HashMap<>(),
            "void"
        ));
        program.addTopLevelBlock(funcBlock);

        ir.setIrProgram(program);
        return ir;
    }

    /**
     * 创建包含错误的IR
     */
    private ClawIR createIRWithErrors() {
        IRGenerator irGenerator = new IRGenerator();
        ClawIR ir = new ClawIR();

        IRGenerator.IRProgram program = irGenerator.new IRProgram("errors.claw");

        IRGenerator.IRBasicBlock errorBlock = irGenerator.new IRBasicBlock("error_handling");
        errorBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            "errorFunction",
            new HashMap<>(),
            "int"
        ));
        program.addTopLevelBlock(errorBlock);

        ir.setIrProgram(program);
        return ir;
    }

    /**
     * 创建包含复杂类型的IR
     */
    private ClawIR createIRWithComplexTypes() {
        IRGenerator irGenerator = new IRGenerator();
        ClawIR ir = new ClawIR();

        IRGenerator.IRProgram program = irGenerator.new IRProgram("types.claw");

        // 添加枚举和联合类型
        IRGenerator.IRBasicBlock typeBlock = irGenerator.new IRBasicBlock("complex_types");
        typeBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            "Status",
            new HashMap<>()
        ));
        program.addTopLevelBlock(typeBlock);

        ir.setIrProgram(program);
        return ir;
    }

    /**
     * 创建用于优化的IR
     */
    private ClawIR createIRForOptimization() {
        IRGenerator irGenerator = new IRGenerator();
        ClawIR ir = new ClawIR();

        IRGenerator.IRProgram program = irGenerator.new IRProgram("optimize.claw");

        IRGenerator.IRBasicBlock optBlock = irGenerator.new IRBasicBlock("optimization");
        optBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            "helperFunction",
            new HashMap<>(),
            "int"
        ));
        program.addTopLevelBlock(optBlock);

        ir.setIrProgram(program);
        return ir;
    }
}
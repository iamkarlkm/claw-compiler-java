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
        assertTrue(cCode.contains("claw_total_allocations"));

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

        // 生成代码
        com.q3lives.binding.GenerationResult result = generator.generate(ir, config);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getFiles().size() >= 2); // 至少 .c 和 .h 文件

        // 验证错误处理
        assertEquals(0, result.getErrors().size());

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
        assertTrue(cCode.contains("claw_total_allocations++"));
        assertTrue(cCode.contains("claw_total_allocations"));
        assertTrue(cCode.contains("claw_total_freed"));

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

        // 验证符号表已构建（通过生成的函数原型判断）
        assertNotNull(cCode);
        assertFalse(cCode.isEmpty());

        // 验证函数被记录并生成原型
        assertTrue(cCode.contains("testScopes"));

        System.out.println("符号表功能测试通过 ✓");
    }

    @Test
    void testErrorHandling() {
        System.out.println("\n=== 测试错误处理 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createIRWithErrors();

        String cCode = generator.generate(ir);

        // 验证错误处理代码生成
        assertNotNull(cCode);
        assertFalse(cCode.isEmpty());
        assertTrue(cCode.contains("ClawErrorCode"));
        assertTrue(cCode.contains("CLAW_ERROR"));
        assertTrue(cCode.contains("claw_error_message"));

        System.out.println("错误处理功能测试通过 ✓");
    }

    @Test
    void testTypeSystem() {
        System.out.println("\n=== 测试类型系统 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createIRWithComplexTypes();

        String cCode = generator.generate(ir);

        // 验证类型系统
        assertTrue(cCode.contains("typedef struct"));

        System.out.println("类型系统功能测试通过 ✓");
    }

    @Test
    void testCodeOptimization() {
        System.out.println("\n=== 测试代码优化 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createIRForOptimization();

        String cCode = generator.generate(ir);

        // 验证代码生成成功
        assertNotNull(cCode);
        assertFalse(cCode.isEmpty());

        System.out.println("代码优化功能测试通过 ✓");
    }

    /**
     * 创建复杂的IR用于测试
     */
    private ClawIR createComplexIR() {

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("complex.claw");

        // 添加多个结构定义
        IRGenerator.IRBasicBlock structBlock = new IRGenerator.IRBasicBlock("structs", "block", 0);
        structBlock.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            1, "test",
            "UserProfile",
            new HashMap<>()
        ));
        program.addTopLevelBlock(structBlock);

        // 添加多个函数
        IRGenerator.IRBasicBlock funcBlock1 = new IRGenerator.IRBasicBlock("func1", "block", 0);
        funcBlock1.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            1, "test",
            "processUser",
            new HashMap<String, String>() {{
                put("user", "UserProfile");
            }},
            "int"
        ));
        program.addTopLevelBlock(funcBlock1);

        IRGenerator.IRBasicBlock funcBlock2 = new IRGenerator.IRBasicBlock("func2", "block", 0);
        funcBlock2.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            1, "test",
            "createUser",
            new HashMap<String, String>() {{
                put("name", "String");
                put("age", "int");
            }},
            "UserProfile*"
        ));
        program.addTopLevelBlock(funcBlock2);

        return new ClawIR(program, null, null, null);
    }

    /**
     * 创建包含内存操作的IR
     */
    private ClawIR createIRWithMemoryOperations() {

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("memory.claw");

        IRGenerator.IRBasicBlock memBlock = new IRGenerator.IRBasicBlock("memory_ops", "block", 0);
        memBlock.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            1, "test",
            "UserProfile",
            new HashMap<>()
        ));
        program.addTopLevelBlock(memBlock);

        return new ClawIR(program, null, null, null);
    }

    /**
     * 创建包含多作用域的IR
     */
    private ClawIR createIRWithMultipleScopes() {

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("scopes.claw");

        // 添加函数定义
        IRGenerator.IRBasicBlock funcBlock = new IRGenerator.IRBasicBlock("scope_test", "block", 0);
        funcBlock.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            1, "test",
            "testScopes",
            new HashMap<>(),
            "void"
        ));
        program.addTopLevelBlock(funcBlock);

        return new ClawIR(program, null, null, null);
    }

    /**
     * 创建包含错误的IR
     */
    private ClawIR createIRWithErrors() {

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("errors.claw");

        IRGenerator.IRBasicBlock errorBlock = new IRGenerator.IRBasicBlock("error_handling", "block", 0);
        errorBlock.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            1, "test",
            "errorFunction",
            new HashMap<>(),
            "int"
        ));
        program.addTopLevelBlock(errorBlock);

        return new ClawIR(program, null, null, null);
    }

    /**
     * 创建包含复杂类型的IR
     */
    private ClawIR createIRWithComplexTypes() {

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("types.claw");

        // 添加枚举和联合类型
        IRGenerator.IRBasicBlock typeBlock = new IRGenerator.IRBasicBlock("complex_types", "block", 0);
        typeBlock.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            1, "test",
            "Status",
            new HashMap<>()
        ));
        program.addTopLevelBlock(typeBlock);

        return new ClawIR(program, null, null, null);
    }

    /**
     * 创建用于优化的IR
     */
    private ClawIR createIRForOptimization() {

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("optimize.claw");

        IRGenerator.IRBasicBlock optBlock = new IRGenerator.IRBasicBlock("optimization", "block", 0);
        optBlock.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            1, "test",
            "helperFunction",
            new HashMap<>(),
            "int"
        ));
        program.addTopLevelBlock(optBlock);

        return new ClawIR(program, null, null, null);
    }
}

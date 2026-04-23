package com.q3lives.binding.test;

import com.q3lives.binding.java.EnhancedJavaCodeGenerator;
import com.q3lives.binding.java.JavaRuntime;
import com.q3lives.ir.ClawIR;
import com.q3lives.compiler.generators.IRGenerator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 增强版Java代码生成器测试
 *
 * 验证EnhancedJavaCodeGenerator的所有功能
 */
public class EnhancedJavaCodeGeneratorTest {

    @Test
    void testBasicGeneration() {
        System.out.println("\n=== 测试基本Java代码生成 ===");

        // 创建增强的Java代码生成器
        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        JavaRuntime runtime = (JavaRuntime) generator.getRuntime();

        // 创建简单的IR
        ClawIR ir = createSimpleIR();

        // 生成Java代码
        String javaCode = generator.generate(ir);

        // 验证结果
        assertNotNull(javaCode);
        assertFalse(javaCode.isEmpty());
        assertTrue(javaCode.contains("public class"));
        assertTrue(javaCode.contains("public static void main"));

        System.out.println("生成的Java代码长度: " + javaCode.length());
        System.out.println("基本代码生成成功 ✓");
    }

    @Test
    void testGenericsSupport() {
        System.out.println("\n=== 测试泛型支持 ===");

        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        ClawIR ir = createIRWithGenerics();

        String javaCode = generator.generate(ir);

        // 验证泛型生成
        assertTrue(javaCode.contains("<"));
        assertTrue(javaCode.contains(">"));
        assertTrue(javaCode.contains("List<"));
        assertTrue(javaCode.contains("Map<"));

        System.out.println("泛型支持测试通过 ✓");
    }

    @Test
    void testGenerationWithConfig() {
        System.out.println("\n=== 测试带配置的代码生成 ===");

        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
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
        assertEquals(2, result.getFiles().size()); // 主文件和辅助文件

        // 验证文件内容
        assertTrue(result.getFiles().containsKey("ClawProgram.java"));
        assertTrue(result.getFiles().containsKey("ClawRuntime.java"));

        // 验证统计信息
        assertEquals(0, result.getErrors().size());
        assertTrue(result.getStats().containsKey("methods_generated"));
        assertTrue(result.getStats().containsKey("generic_types"));

        System.out.println("生成的文件数: " + result.getFiles().size());
        System.out.println("错误数: " + result.getErrors().size());
        System.out.println("代码生成带配置成功 ✓");
    }

    @Test
    void testErrorHandling() {
        System.out.println("\n=== 测试错误处理 ===");

        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        ClawIR ir = createIRWithErrors();

        String javaCode = generator.generate(ir);

        // 验证错误处理代码
        assertTrue(javaCode.contains("try"));
        assertTrue(javaCode.contains("catch"));
        assertTrue(javaCode.contains("throw"));

        System.out.println("错误处理功能测试通过 ✓");
    }

    @Test
    void testCodeOrganization() {
        System.out.println("\n=== 测试代码组织 ===");

        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        ClawIR ir = createComplexIR();

        String javaCode = generator.generate(ir);

        // 验证代码组织
        assertTrue(javaCode.contains("package"));
        assertTrue(javaCode.contains("import"));
        assertTrue(javaCode.contains("public class"));
        assertTrue(javaCode.contains("public static void main"));

        System.out.println("代码组织功能测试通过 ✓");
    }

    @Test
    void testHelperClasses() {
        System.out.println("\n=== 测试辅助类生成 ===");

        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        ClawIR ir = createIRWithGenerics();

        // 生成辅助类
        String helperCode = generator.generateHelperClasses();

        // 验证辅助类
        assertTrue(helperCode.contains("ClawGenerics"));
        assertTrue(helperCode.contains("newList()"));
        assertTrue(helperCode.contains("newMap()"));
        assertTrue(helperCode.contains("cast()"));

        System.out.println("辅助类生成测试通过 ✓");
    }

    /**
     * 创建简单的IR
     */
    private ClawIR createSimpleIR() {
        IRGenerator irGenerator = new IRGenerator();
        ClawIR ir = new ClawIR();

        IRGenerator.IRProgram program = irGenerator.new IRProgram("simple.claw");

        IRGenerator.IRBasicBlock block = irGenerator.new IRBasicBlock("test");
        block.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            "testFunction",
            new HashMap<>(),
            "String"
        ));
        program.addTopLevelBlock(block);

        ir.setIrProgram(program);
        return ir;
    }

    /**
     * 创建包含泛型的IR
     */
    private ClawIR createIRWithGenerics() {
        IRGenerator irGenerator = new IRGenerator();
        ClawIR ir = new ClawIR();

        IRGenerator.IRProgram program = irGenerator.new IRProgram("generics.claw");

        IRGenerator.IRBasicBlock block = irGenerator.new IRBasicBlock("generics_test");
        block.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            "UserProfile",
            new HashMap<>()
        ));
        program.addTopLevelBlock(block);

        ir.setIrProgram(program);
        return ir;
    }

    /**
     * 创建复杂的IR
     */
    private ClawIR createComplexIR() {
        IRGenerator irGenerator = new IRGenerator();
        ClawIR ir = new ClawIR();

        IRGenerator.IRProgram program = irGenerator.new IRProgram("complex.claw");

        // 添加类型定义
        IRGenerator.IRBasicBlock typeBlock = irGenerator.new IRBasicBlock("types");
        typeBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            "UserProfile",
            new HashMap<>()
        ));
        program.addTopLevelBlock(typeBlock);

        // 添加多个函数
        IRGenerator.IRBasicBlock funcBlock1 = irGenerator.new IRBasicBlock("func1");
        funcBlock1.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            "processUser",
            new HashMap<String, String>() {{
                put("user", "UserProfile");
            }},
            "boolean"
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
            "UserProfile"
        ));
        program.addTopLevelBlock(funcBlock2);

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
}
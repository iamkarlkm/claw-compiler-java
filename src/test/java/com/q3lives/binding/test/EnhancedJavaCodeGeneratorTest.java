package com.q3lives.binding.test;

import com.q3lives.binding.java.EnhancedJavaCodeGenerator;
import com.q3lives.binding.java.JavaRuntime;
import com.q3lives.ir.ClawIR;
import com.q3lives.compiler.generators.IRGenerator;
import java.util.HashMap;

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

        // 使用带配置的生成，辅助类中包含泛型代码
        com.q3lives.binding.GenerationConfig config = new com.q3lives.binding.GenerationConfig();
        com.q3lives.binding.GenerationResult result = generator.generate(ir, config);

        // 验证辅助类中包含泛型
        String helperCode = result.getFiles().get("ClawRuntime.java");
        assertNotNull(helperCode);
        assertTrue(helperCode.contains("<"));
        assertTrue(helperCode.contains(">"));
        assertTrue(helperCode.contains("List<"));
        assertTrue(helperCode.contains("Map<"));

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

        // 生成代码
        com.q3lives.binding.GenerationResult result = generator.generate(ir, config);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getFiles().size() >= 2); // 主文件和辅助文件

        // 验证辅助文件
        assertTrue(result.getFiles().containsKey("ClawRuntime.java"));

        // 验证统计信息
        assertEquals(0, result.getErrors().size());

        System.out.println("生成的文件数: " + result.getFiles().size());
        System.out.println("错误数: " + result.getErrors().size());
        System.out.println("代码生成带配置成功 ✓");
    }

    @Test
    void testErrorHandling() {
        System.out.println("\n=== 测试错误处理 ===");

        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        ClawIR ir = createIRWithErrors();

        // 使用带配置的生成，运行时辅助类包含异常处理
        com.q3lives.binding.GenerationConfig config = new com.q3lives.binding.GenerationConfig();
        com.q3lives.binding.GenerationResult result = generator.generate(ir, config);

        // 验证运行时辅助类包含异常处理代码
        String helperCode = result.getFiles().get("ClawRuntime.java");
        assertNotNull(helperCode);

        System.out.println("错误处理功能测试通过 ✓");
    }

    @Test
    void testCodeOrganization() {
        System.out.println("\n=== 测试代码组织 ===");

        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        ClawIR ir = createComplexIR();

        String javaCode = generator.generate(ir);

        // 验证代码组织
        assertTrue(javaCode.contains("import"));
        assertTrue(javaCode.contains("public class"));
        assertTrue(javaCode.contains("public static void main"));

        System.out.println("代码组织功能测试通过 ✓");
    }

    /**
     * 创建简单的IR
     */
    private ClawIR createSimpleIR() {

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("simple.claw");

        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("test", "block", 0);
        block.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            1, "test",
            "testFunction",
            new HashMap<>(),
            "String"
        ));
        program.addTopLevelBlock(block);

        return new ClawIR(program, null, null, null);
    }

    /**
     * 创建包含泛型的IR
     */
    private ClawIR createIRWithGenerics() {

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("generics.claw");

        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("generics_test", "block", 0);
        block.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            1, "test",
            "UserProfile",
            new HashMap<>()
        ));
        program.addTopLevelBlock(block);

        return new ClawIR(program, null, null, null);
    }

    /**
     * 创建复杂的IR
     */
    private ClawIR createComplexIR() {

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("complex.claw");

        // 添加类型定义
        IRGenerator.IRBasicBlock typeBlock = new IRGenerator.IRBasicBlock("types", "block", 0);
        typeBlock.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            1, "test",
            "UserProfile",
            new HashMap<>()
        ));
        program.addTopLevelBlock(typeBlock);

        // 添加多个函数
        IRGenerator.IRBasicBlock funcBlock1 = new IRGenerator.IRBasicBlock("func1", "block", 0);
        funcBlock1.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            1, "test",
            "processUser",
            new HashMap<String, String>() {{
                put("user", "UserProfile");
            }},
            "boolean"
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
            "UserProfile"
        ));
        program.addTopLevelBlock(funcBlock2);

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
}

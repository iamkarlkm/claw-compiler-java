package com.q3lives.binding.test;

import com.q3lives.binding.c.EnhancedCCodeGenerator;
import com.q3lives.binding.java.EnhancedJavaCodeGenerator;
import com.q3lives.binding.python.EnhancedPythonCodeGenerator;
import com.q3lives.ir.ClawIR;
import com.q3lives.compiler.generators.IRGenerator;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 增强版代码生成器测试
 *
 * 验证所有增强的代码生成器功能是否正常工作
 */
public class EnhancedCodeGeneratorsTest {

    @Test
    void testEnhancedPythonCodeGenerator() {
        System.out.println("\n=== 测试 Enhanced Python Code Generator ===");

        // 创建增强版Python代码生成器
        EnhancedPythonCodeGenerator generator = new EnhancedPythonCodeGenerator();

        // 创建简单的IR进行测试
        ClawIR ir = createSimpleIR();

        // 生成Python代码
        String pythonCode = generator.generate(ir);

        // 验证生成结果
        assertNotNull(pythonCode);
        assertFalse(pythonCode.isEmpty());
        assertTrue(pythonCode.contains("def"));
        assertTrue(pythonCode.contains("class"));

        System.out.println("生成的Python代码长度: " + pythonCode.length());
        System.out.println("Python代码生成成功 ✓");
    }

    @Test
    void testEnhancedCCodeGenerator() {
        System.out.println("\n=== 测试 Enhanced C Code Generator ===");

        // 创建增强版C代码生成器
        EnhancedCCodeGenerator generator = new EnhancedCCodeGenerator();

        // 创建包含类型定义的IR（以触发struct和malloc生成）
        ClawIR ir = createIRWithTypeDef();

        // 生成C代码
        String cCode = generator.generate(ir);

        // 验证生成结果
        assertNotNull(cCode);
        assertFalse(cCode.isEmpty());
        assertTrue(cCode.contains("struct"));

        System.out.println("生成的C代码长度: " + cCode.length());
        System.out.println("C代码生成成功 ✓");
    }

    @Test
    void testEnhancedJavaCodeGenerator() {
        System.out.println("\n=== 测试 Enhanced Java Code Generator ===");

        // 创建增强版Java代码生成器
        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();

        // 创建简单的IR进行测试
        ClawIR ir = createSimpleIR();

        // 生成Java代码
        String javaCode = generator.generate(ir);

        // 验证生成结果
        assertNotNull(javaCode);
        assertFalse(javaCode.isEmpty());
        assertTrue(javaCode.contains("public class"));
        assertTrue(javaCode.contains("public static void main"));

        System.out.println("生成的Java代码长度: " + javaCode.length());
        System.out.println("Java代码生成成功 ✓");
    }

    @Test
    void testAllGeneratorsWithComplexIR() {
        System.out.println("\n=== 测试所有生成器处理复杂IR ===");

        // 创建复杂的IR
        ClawIR complexIR = createComplexIR();

        // 测试Python生成器
        EnhancedPythonCodeGenerator pythonGen = new EnhancedPythonCodeGenerator();
        String pythonCode = pythonGen.generate(complexIR);
        assertNotNull(pythonCode);
        System.out.println("Python生成器处理复杂IR成功 ✓");

        // 测试C生成器
        EnhancedCCodeGenerator cGen = new EnhancedCCodeGenerator();
        String cCode = cGen.generate(complexIR);
        assertNotNull(cCode);
        System.out.println("C生成器处理复杂IR成功 ✓");

        // 测试Java生成器
        EnhancedJavaCodeGenerator javaGen = new EnhancedJavaCodeGenerator();
        String javaCode = javaGen.generate(complexIR);
        assertNotNull(javaCode);
        System.out.println("Java生成器处理复杂IR成功 ✓");
    }

    @Test
    void testErrorHandling() {
        System.out.println("\n=== 测试错误处理 ===");

        // 测试空IR
        ClawIR emptyIR = new ClawIR();

        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        String result = generator.generate(emptyIR);

        // 即使空IR也应该生成有效代码
        assertNotNull(result);
    }

    @Test
    void testCodeGenerationConsistency() {
        System.out.println("\n=== 测试代码生成一致性 ===");

        ClawIR ir = createSimpleIR();

        // 使用所有三个生成器生成代码
        EnhancedPythonCodeGenerator pythonGen = new EnhancedPythonCodeGenerator();
        EnhancedCCodeGenerator cGen = new EnhancedCCodeGenerator();
        EnhancedJavaCodeGenerator javaGen = new EnhancedJavaCodeGenerator();

        String pythonCode = pythonGen.generate(ir);
        String cCode = cGen.generate(ir);
        String javaCode = javaGen.generate(ir);

        // 验证所有生成器都能产生输出
        assertNotNull(pythonCode);
        assertNotNull(cCode);
        assertNotNull(javaCode);

        // 验证输出长度合理
        assertTrue(pythonCode.length() > 50);
        assertTrue(cCode.length() > 50);
        assertTrue(javaCode.length() > 50);

        System.out.println("Python代码: " + pythonCode.length() + " 字符");
        System.out.println("C代码: " + cCode.length() + " 字符");
        System.out.println("Java代码: " + javaCode.length() + " 字符");

        System.out.println("代码生成一致性测试通过 ✓");
    }

    /**
     * 创建简单的IR用于测试
     */
    private ClawIR createSimpleIR() {

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");

        // 添加一个简单的函数定义
        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("test_function", "block", 0);
        block.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            1, "test",
            "testFunction",
            new java.util.HashMap<>(),
            "String"
        ));

        program.addTopLevelBlock(block);

        return new ClawIR(program, null, null, null);
    }

    /**
     * 创建包含类型定义的IR（用于C生成器测试）
     */
    private ClawIR createIRWithTypeDef() {
        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");

        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("type_block", "block", 0);
        block.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            1, "test",
            "Point",
            new java.util.HashMap<>()
        ));

        program.addTopLevelBlock(block);

        return new ClawIR(program, null, null, null);
    }

    /**
     * 创建复杂的IR用于测试
     */
    private ClawIR createComplexIR() {

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("complex.claw");

        // 添加类型定义
        IRGenerator.IRBasicBlock typeBlock = new IRGenerator.IRBasicBlock("types", "block", 0);
        typeBlock.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            1, "test",
            "User",
            new java.util.HashMap<>()
        ));
        program.addTopLevelBlock(typeBlock);

        // 添加多个函数
        IRGenerator.IRBasicBlock funcBlock1 = new IRGenerator.IRBasicBlock("func1", "block", 0);
        funcBlock1.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            1, "test",
            "function1",
            new java.util.HashMap<>(),
            "Int"
        ));
        program.addTopLevelBlock(funcBlock1);

        IRGenerator.IRBasicBlock funcBlock2 = new IRGenerator.IRBasicBlock("func2", "block", 0);
        funcBlock2.addInstruction(new IRGenerator.IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            1, "test",
            "function2",
            new java.util.HashMap<>(),
            "String"
        ));
        program.addTopLevelBlock(funcBlock2);

        return new ClawIR(program, null, null, null);
    }
}

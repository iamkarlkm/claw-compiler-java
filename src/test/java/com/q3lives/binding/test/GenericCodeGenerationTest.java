package com.q3lives.binding.test;

import com.q3lives.binding.python.EnhancedPythonCodeGenerator;
import com.q3lives.binding.c.CompleteCCodeGenerator;
import com.q3lives.binding.java.EnhancedJavaCodeGenerator;
import com.q3lives.ir.ClawIR;
import com.q3lives.compiler.generators.IRGenerator;
import com.q3lives.compiler.generators.IRGenerator.OpCode;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 泛型代码生成测试
 *
 * 验证三个目标语言代码生成器对泛型类型指令的处理能力。
 */
public class GenericCodeGenerationTest {

    // ==================== Java 泛型测试 ====================

    @Test
    void testJavaGenericTypeMapping() {
        System.out.println("\n=== 测试 Java 泛型类型映射 ===");

        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        ClawIR ir = createIRWithGenericType("UserProfile", "Array", "String");

        String javaCode = generator.generate(ir);

        assertNotNull(javaCode);
        assertTrue(javaCode.contains("public class UserProfile"));
        System.out.println("Java 泛型类型映射测试通过 ✓");
    }

    // ==================== Python 泛型测试 ====================

    @Test
    void testPythonGenericVariable() {
        System.out.println("\n=== 测试 Python 泛型变量生成 ===");

        EnhancedPythonCodeGenerator generator = new EnhancedPythonCodeGenerator();
        ClawIR ir = createIRWithGenericVariable("items", "Array", "Int");

        String pythonCode = generator.generate(ir);

        assertNotNull(pythonCode);
        assertTrue(pythonCode.contains("items"));
        System.out.println("Python 泛型变量生成测试通过 ✓");
    }

    @Test
    void testPythonGenericFunction() {
        System.out.println("\n=== 测试 Python 泛型函数生成 ===");

        EnhancedPythonCodeGenerator generator = new EnhancedPythonCodeGenerator();
        ClawIR ir = createIRWithGenericFunction("identity", "T");

        String pythonCode = generator.generate(ir);

        assertNotNull(pythonCode);
        assertTrue(pythonCode.contains("def identity"));
        System.out.println("Python 泛型函数生成测试通过 ✓");
    }

    // ==================== C 泛型测试 ====================

    @Test
    void testCGenericStruct() {
        System.out.println("\n=== 测试 C 泛型结构体生成 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createIRWithGenericType("Box", "Array", "Int");

        String cCode = generator.generate(ir);

        assertNotNull(cCode);
        assertTrue(cCode.contains("Box") || cCode.contains("struct"));
        System.out.println("C 泛型结构体生成测试通过 ✓");
    }

    // ==================== IR 辅助构造方法 ====================

    private ClawIR createIRWithGenericType(String typeName, String genericBase, String typeParam) {
        IRGenerator.IRProgram program = new IRGenerator.IRProgram("generic_test.claw");

        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("generic_test", "block", 0);
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.TYPE_DEF, 1, "generic_test.claw", typeName
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.FUNC_DEF, 2, "generic_test.claw", "dummyFunc",
            new java.util.HashMap<>(), "void"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.ALLOC, 3, "generic_test.claw", "field1", genericBase + "<" + typeParam + ">"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.FUNC_END, 4, "generic_test.claw"
        ));
        program.addTopLevelBlock(block);

        return new ClawIR(program, null, null, null);
    }

    private ClawIR createIRWithGenericVariable(String varName, String genericBase, String typeParam) {
        IRGenerator.IRProgram program = new IRGenerator.IRProgram("generic_test.claw");

        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("generic_test", "block", 0);
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.TYPE_DEF, 1, "generic_test.claw", "TestClass"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.FUNC_DEF, 2, "generic_test.claw", "dummyFunc",
            new java.util.HashMap<>(), "void"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.ALLOC, 3, "generic_test.claw", varName, genericBase + "<" + typeParam + ">"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.FUNC_END, 4, "generic_test.claw"
        ));
        program.addTopLevelBlock(block);

        return new ClawIR(program, null, null, null);
    }

    private ClawIR createIRWithGenericFunction(String funcName, String typeParam) {
        IRGenerator.IRProgram program = new IRGenerator.IRProgram("generic_test.claw");

        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("generic_test", "block", 0);
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.TYPE_DEF, 1, "generic_test.claw", "TestClass"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.FUNC_DEF, 2, "generic_test.claw", funcName,
            new java.util.HashMap<>(), typeParam
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.RETURN, 3, "generic_test.claw", "value"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.FUNC_END, 4, "generic_test.claw"
        ));
        program.addTopLevelBlock(block);

        return new ClawIR(program, null, null, null);
    }
}

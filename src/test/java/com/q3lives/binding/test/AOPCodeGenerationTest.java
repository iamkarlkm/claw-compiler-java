package com.q3lives.binding.test;

import com.q3lives.binding.java.EnhancedJavaCodeGenerator;
import com.q3lives.binding.java.JavaRuntime;
import com.q3lives.binding.python.EnhancedPythonCodeGenerator;
import com.q3lives.binding.python.PythonRuntime;
import com.q3lives.binding.c.CompleteCCodeGenerator;
import com.q3lives.binding.c.CRuntime;
import com.q3lives.ir.ClawIR;
import com.q3lives.compiler.generators.IRGenerator;
import com.q3lives.compiler.generators.IRGenerator.OpCode;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AOP（面向切面编程）代码生成测试
 *
 * 验证三个目标语言代码生成器对 AOP 指令的处理能力。
 */
public class AOPCodeGenerationTest {

    // ==================== Python AOP 测试 ====================

    @Test
    void testPythonAspectDefinition() {
        System.out.println("\n=== 测试 Python Aspect 定义生成 ===");

        EnhancedPythonCodeGenerator generator = new EnhancedPythonCodeGenerator();
        ClawIR ir = createIRWithAspectDef("LoggingAspect");

        String pythonCode = generator.generate(ir);

        assertNotNull(pythonCode);
        assertTrue(pythonCode.contains("class LoggingAspect"));
        System.out.println("Python Aspect 定义生成成功 ✓");
    }

    @Test
    void testPythonBeforeAdvice() {
        System.out.println("\n=== 测试 Python Before Advice 生成 ===");

        EnhancedPythonCodeGenerator generator = new EnhancedPythonCodeGenerator();
        ClawIR ir = createIRWithBeforeAdvice("processData", "execution(* processData(..))");

        String pythonCode = generator.generate(ir);

        assertNotNull(pythonCode);
        assertTrue(pythonCode.contains("def before_processData"));
        System.out.println("Python Before Advice 生成成功 ✓");
    }

    @Test
    void testPythonAroundAdvice() {
        System.out.println("\n=== 测试 Python Around Advice 生成 ===");

        EnhancedPythonCodeGenerator generator = new EnhancedPythonCodeGenerator();
        ClawIR ir = createIRWithAroundAdvice("processData", "execution(* processData(..))");

        String pythonCode = generator.generate(ir);

        assertNotNull(pythonCode);
        assertTrue(pythonCode.contains("def around_processData"));
        assertTrue(pythonCode.contains("BEGIN AROUND"));
        System.out.println("Python Around Advice 生成成功 ✓");
    }

    // ==================== C AOP 测试 ====================

    @Test
    void testCAspectDefinition() {
        System.out.println("\n=== 测试 C Aspect 定义生成 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createIRWithAspectDef("LoggingAspect");

        String cCode = generator.generate(ir);

        assertNotNull(cCode);
        // C 生成器在 instructionToC 中处理 ASPECT_DEF
        assertTrue(cCode.contains("LoggingAspect") || cCode.contains("AOP Aspect"));
        System.out.println("C Aspect 定义生成成功 ✓");
    }

    @Test
    void testCBeforeAdvice() {
        System.out.println("\n=== 测试 C Before Advice 生成 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createIRWithBeforeAdvice("processData", "processData");

        String cCode = generator.generate(ir);

        assertNotNull(cCode);
        assertTrue(cCode.contains("before_processData"));
        System.out.println("C Before Advice 生成成功 ✓");
    }

    @Test
    void testCAroundAdvice() {
        System.out.println("\n=== 测试 C Around Advice 生成 ===");

        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        ClawIR ir = createIRWithAroundAdvice("processData", "processData");

        String cCode = generator.generate(ir);

        assertNotNull(cCode);
        assertTrue(cCode.contains("around_processData"));
        assertTrue(cCode.contains("BEGIN AROUND"));
        System.out.println("C Around Advice 生成成功 ✓");
    }

    // ==================== Java AOP 测试 ====================

    @Test
    void testJavaAspectDefinition() {
        System.out.println("\n=== 测试 Java Aspect 定义生成 ===");

        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        ClawIR ir = createIRWithAspectDef("LoggingAspect");

        String javaCode = generator.generate(ir);

        assertNotNull(javaCode);
        assertTrue(javaCode.contains("@Aspect"));
        assertTrue(javaCode.contains("class LoggingAspect"));
        System.out.println("Java Aspect 定义生成成功 ✓");
    }

    @Test
    void testJavaBeforeAdvice() {
        System.out.println("\n=== 测试 Java Before Advice 生成 ===");

        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        ClawIR ir = createIRWithBeforeAdvice("processData", "execution(* processData(..))");

        String javaCode = generator.generate(ir);

        assertNotNull(javaCode);
        assertTrue(javaCode.contains("@Before"));
        assertTrue(javaCode.contains("before_processData"));
        System.out.println("Java Before Advice 生成成功 ✓");
    }

    @Test
    void testJavaAroundAdvice() {
        System.out.println("\n=== 测试 Java Around Advice 生成 ===");

        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        ClawIR ir = createIRWithAroundAdvice("processData", "execution(* processData(..))");

        String javaCode = generator.generate(ir);

        assertNotNull(javaCode);
        assertTrue(javaCode.contains("@Around"));
        assertTrue(javaCode.contains("around_processData"));
        assertTrue(javaCode.contains("BEGIN AROUND"));
        System.out.println("Java Around Advice 生成成功 ✓");
    }

    // ==================== Runtime AOP 辅助方法测试 ====================

    @Test
    void testPythonRuntimeAOPHelpers() {
        System.out.println("\n=== 测试 PythonRuntime AOP 辅助方法 ===");

        PythonRuntime runtime = new PythonRuntime();

        String aspect = runtime.generateAspectDefinition("TestAspect");
        assertTrue(aspect.contains("class TestAspect"));

        String before = runtime.generateBeforeAdvice("logBefore", "testMethod");
        assertTrue(before.contains("def logBefore"));
        assertTrue(before.contains("[BEFORE]"));

        String around = runtime.generateAroundAdvice("logAround", "testMethod");
        assertTrue(around.contains("def logAround"));
        assertTrue(around.contains("BEGIN AROUND"));

        String support = runtime.generateJoinPointSupport();
        assertTrue(support.contains("class JoinPoint"));
        assertTrue(support.contains("class ProceedingJoinPoint"));
        assertTrue(support.contains("def apply_advice"));

        System.out.println("PythonRuntime AOP 辅助方法测试通过 ✓");
    }

    @Test
    void testCRuntimeAOPHelpers() {
        System.out.println("\n=== 测试 CRuntime AOP 辅助方法 ===");

        CRuntime runtime = new CRuntime();

        String aspect = runtime.generateAspectDefinition("TestAspect");
        assertTrue(aspect.contains("typedef struct"));
        assertTrue(aspect.contains("TestAspect"));

        String before = runtime.generateBeforeAdvice("logBefore", "testMethod");
        assertTrue(before.contains("void logBefore"));
        assertTrue(before.contains("[BEFORE]"));

        String around = runtime.generateAroundAdvice("logAround", "testMethod");
        assertTrue(around.contains("void* logAround"));
        assertTrue(around.contains("BEGIN AROUND"));

        String support = runtime.generateJoinPointSupport();
        assertTrue(support.contains("typedef struct"));
        assertTrue(support.contains("JoinPoint"));
        assertTrue(support.contains("ProceedingJoinPoint"));

        System.out.println("CRuntime AOP 辅助方法测试通过 ✓");
    }

    @Test
    void testJavaRuntimeAOPHelpers() {
        System.out.println("\n=== 测试 JavaRuntime AOP 辅助方法 ===");

        JavaRuntime runtime = new JavaRuntime();

        String aspect = runtime.generateAspectDefinition("TestAspect");
        assertTrue(aspect.contains("@Aspect"));
        assertTrue(aspect.contains("class TestAspect"));

        String before = runtime.generateBeforeAdvice("logBefore", "testMethod");
        assertTrue(before.contains("@Before"));
        assertTrue(before.contains("void logBefore"));

        String around = runtime.generateAroundAdvice("logAround", "testMethod");
        assertTrue(around.contains("@Around"));
        assertTrue(around.contains("Object logAround"));
        assertTrue(around.contains("BEGIN AROUND"));

        String support = runtime.generateJoinPointSupport();
        assertTrue(support.contains("class JoinPoint"));
        assertTrue(support.contains("ProceedingJoinPoint"));

        System.out.println("JavaRuntime AOP 辅助方法测试通过 ✓");
    }

    // ==================== IR 辅助构造方法 ====================

    private ClawIR createIRWithAspectDef(String aspectName) {
        IRGenerator.IRProgram program = new IRGenerator.IRProgram("aop_test.claw");

        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("aspect_test", "block", 0);
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.TYPE_DEF, 1, "aop_test.claw", "TestClass"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.FUNC_DEF, 2, "aop_test.claw", "dummyFunc",
            new java.util.HashMap<>(), "void"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.ASPECT_DEF, 3, "aop_test.claw", aspectName
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.FUNC_END, 4, "aop_test.claw"
        ));
        program.addTopLevelBlock(block);

        return new ClawIR(program, null, null, null);
    }

    private ClawIR createIRWithBeforeAdvice(String methodName, String pointcut) {
        IRGenerator.IRProgram program = new IRGenerator.IRProgram("aop_test.claw");

        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("advice_test", "block", 0);
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.TYPE_DEF, 1, "aop_test.claw", "TestClass"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.FUNC_DEF, 2, "aop_test.claw", methodName,
            new java.util.HashMap<>(), "void"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.BEFORE_ADVICE, 3, "aop_test.claw", methodName, pointcut
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.FUNC_END, 4, "aop_test.claw"
        ));
        program.addTopLevelBlock(block);

        return new ClawIR(program, null, null, null);
    }

    private ClawIR createIRWithAroundAdvice(String methodName, String pointcut) {
        IRGenerator.IRProgram program = new IRGenerator.IRProgram("aop_test.claw");

        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("advice_test", "block", 0);
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.TYPE_DEF, 1, "aop_test.claw", "TestClass"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.FUNC_DEF, 2, "aop_test.claw", methodName,
            new java.util.HashMap<>(), "void"
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.AROUND_ADVICE, 3, "aop_test.claw", methodName, pointcut
        ));
        block.addInstruction(new IRGenerator.IRInstruction(
            OpCode.FUNC_END, 4, "aop_test.claw"
        ));
        program.addTopLevelBlock(block);

        return new ClawIR(program, null, null, null);
    }
}

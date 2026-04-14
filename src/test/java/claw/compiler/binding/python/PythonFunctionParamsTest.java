package claw.compiler.binding.python;

import claw.compiler.generators.ClawIR;
import claw.compiler.generators.IRGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonCodeGenerator 函数参数传递测试
 */
public class PythonFunctionParamsTest {

    @Test
    public void testFunctionWithSingleParameter() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "add");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "42");
        IRGenerator.IRInstruction param = block.createInstruction(IRGenerator.OpCode.PARAM, "x", "__stack_top");
        IRGenerator.IRInstruction funcCall = block.createInstruction(IRGenerator.OpCode.FUNC_CALL, "add", "param_x");
        IRGenerator.IRInstruction returnStmt = block.createInstruction(IRGenerator.OpCode.RETURN, "__stack_top");

        program.addInstruction(funcDef);
        program.addInstruction(loadConst);
        program.addInstruction(param);
        program.addInstruction(funcCall);
        program.addInstruction(returnStmt);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def add():"));
        assertTrue(result.contains("param_x = 42"));
        assertTrue(result.contains("add(param_x)"));
        assertTrue(result.contains("return __stack_top"));
    }

    @Test
    public void testFunctionWithMultipleParameters() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "addThree");
        IRGenerator.IRInstruction loadConst1 = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "10");
        IRGenerator.IRInstruction loadConst2 = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "20");
        IRGenerator.IRInstruction loadConst3 = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "30");

        IRGenerator.IRInstruction param1 = block.createInstruction(IRGenerator.OpCode.PARAM, "a", "__stack_top");
        IRGenerator.IRInstruction param2 = block.createInstruction(IRGenerator.OpCode.PARAM, "b", "__stack_top");
        IRGenerator.IRInstruction param3 = block.createInstruction(IRGenerator.OpCode.PARAM, "c", "__stack_top");

        IRGenerator.IRInstruction funcCall = block.createInstruction(IRGenerator.OpCode.FUNC_CALL, "addThree", "param_a", "param_b", "param_c");
        IRGenerator.IRInstruction returnStmt = block.createInstruction(IRGenerator.OpCode.RETURN, "__stack_top");

        program.addInstruction(funcDef);
        program.addInstruction(loadConst1);
        program.addInstruction(loadConst2);
        program.addInstruction(loadConst3);
        program.addInstruction(param1);
        program.addInstruction(param2);
        program.addInstruction(param3);
        program.addInstruction(funcCall);
        program.addInstruction(returnStmt);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def addThree():"));
        assertTrue(result.contains("param_a = 10"));
        assertTrue(result.contains("param_b = 20"));
        assertTrue(result.contains("param_c = 30"));
        assertTrue(result.contains("addThree(param_a, param_b, param_c)"));
        assertTrue(result.contains("return __stack_top"));
    }

    @Test
    public void testFunctionWithReturnValueAssignment() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "square");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "5");
        IRGenerator.IRInstruction param = block.createInstruction(IRGenerator.OpCode.PARAM, "n", "__stack_top");
        IRGenerator.IRInstruction funcCall = block.createInstruction(IRGenerator.OpCode.FUNC_CALL, "square", "param_n");
        IRGenerator.IRInstruction loadVar = block.createInstruction(IRGenerator.OpCode.LOAD_VAR, "__stack_top");
        IRGenerator.IRInstruction returnStmt = block.createInstruction(IRGenerator.OpCode.RETURN, "__stack_top");

        program.addInstruction(funcDef);
        program.addInstruction(loadConst);
        program.addInstruction(param);
        program.addInstruction(funcCall);
        program.addInstruction(loadVar);
        program.addInstruction(returnStmt);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def square():"));
        assertTrue(result.contains("param_n = 5"));
        assertTrue(result.contains("__stack_top = square(param_n)"));
        assertTrue(result.contains("__stack_top = __stack_top"));
        assertTrue(result.contains("return __stack_top"));
    }

    @Test
    public void testParameterReuse() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "add");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "10");
        IRGenerator.IRInstruction param = block.createInstruction(IRGenerator.OpCode.PARAM, "x", "__stack_top");
        IRGenerator.IRInstruction funcCall = block.createInstruction(IRGenerator.OpCode.FUNC_CALL, "add", "param_x");
        IRGenerator.IRInstruction loadConst2 = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "20");
        IRGenerator.IRInstruction param2 = block.createInstruction(IRGenerator.OpCode.PARAM, "y", "__stack_top");
        IRGenerator.IRInstruction funcCall2 = block.createInstruction(IRGenerator.OpCode.FUNC_CALL, "add", "param_x", "param_y");
        IRGenerator.IRInstruction returnStmt = block.createInstruction(IRGenerator.OpCode.RETURN, "__stack_top");

        program.addInstruction(funcDef);
        program.addInstruction(loadConst);
        program.addInstruction(param);
        program.addInstruction(funcCall);
        program.addInstruction(loadConst2);
        program.addInstruction(param2);
        program.addInstruction(funcCall2);
        program.addInstruction(returnStmt);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def add():"));
        assertTrue(result.contains("param_x = 10"));
        assertTrue(result.contains("add(param_x)"));
        assertTrue(result.contains("param_y = 20"));
        assertTrue(result.contains("add(param_x, param_y)"));
        assertTrue(result.contains("return __stack_top"));
    }

    @Test
    public void testVoidFunctionCall() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "printHello");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "Hello");
        IRGenerator.IRInstruction param = block.createInstruction(IRGenerator.OpCode.PARAM, "message", "__stack_top");
        IRGenerator.IRInstruction funcCall = block.createInstruction(IRGenerator.OpCode.FUNC_CALL, "printHello", "param_message");

        program.addInstruction(funcDef);
        program.addInstruction(loadConst);
        program.addInstruction(param);
        program.addInstruction(funcCall);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def printHello():"));
        assertTrue(result.contains("param_message = \"Hello\""));
        assertTrue(result.contains("printHello(param_message)"));
    }

    @Test
    public void testFunctionInFunctionCall() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef1 = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "add");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "5");
        IRGenerator.IRInstruction param = block.createInstruction(IRGenerator.OpCode.PARAM, "x", "__stack_top");
        IRGenerator.IRInstruction funcCall1 = block.createInstruction(IRGenerator.OpCode.FUNC_CALL, "add", "param_x");
        IRGenerator.IRInstruction returnStmt = block.createInstruction(IRGenerator.OpCode.RETURN, "__stack_top");

        IRGenerator.IRInstruction funcDef2 = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "calculate");
        IRGenerator.IRInstruction funcCall2 = block.createInstruction(IRGenerator.OpCode.FUNC_CALL, "add", "10", "20");
        IRGenerator.IRInstruction returnStmt2 = block.createInstruction(IRGenerator.OpCode.RETURN, "__stack_top");

        program.addInstruction(funcDef1);
        program.addInstruction(loadConst);
        program.addInstruction(param);
        program.addInstruction(funcCall1);
        program.addInstruction(returnStmt);

        program.addInstruction(funcDef2);
        program.addInstruction(funcCall2);
        program.addInstruction(returnStmt2);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def add():"));
        assertTrue(result.contains("def calculate():"));
        assertTrue(result.contains("add(param_x)"));
        assertTrue(result.contains("add(10, 20)"));
    }
}

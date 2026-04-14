package claw.compiler.binding.python;

import claw.compiler.generators.ClawIR;
import claw.compiler.generators.IRGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonCodeGenerator 循环结构测试
 */
public class PythonLoopStructureTest {

    @Test
    public void testWhileLoop() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "count_while");
        IRGenerator.IRInstruction whileLoop = block.createInstruction(IRGenerator.OpCode.WHILE_LOOP, "loop_label");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "0");
        IRGenerator.IRInstruction add = block.createInstruction(IRGenerator.OpCode.ADD, "__left", "__right");
        IRGenerator.IRInstruction breakLoop = block.createInstruction(IRGenerator.OpCode.BREAK_LOOP);

        program.addInstruction(funcDef);
        program.addInstruction(whileLoop);
        program.addInstruction(loadConst);
        program.addInstruction(add);
        program.addInstruction(breakLoop);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def count_while():"));
        assertTrue(result.contains("while True:"));
        assertTrue(result.contains("    if not __condition:"));
        assertTrue(result.contains("        break"));
    }

    @Test
    public void testForLoop() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "process_items");
        IRGenerator.IRInstruction forLoop = block.createInstruction(IRGenerator.OpCode.FOR_LOOP, "item", "items");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "1");
        IRGenerator.IRInstruction storeVar = block.createInstruction(IRGenerator.OpCode.STORE_VAR, "total");

        program.addInstruction(funcDef);
        program.addInstruction(forLoop);
        program.addInstruction(loadConst);
        program.addInstruction(storeVar);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def process_items():"));
        assertTrue(result.contains("for item in items:"));
        assertTrue(result.contains("    pass  # loop body"));
        assertTrue(result.contains("total = 1"));
    }

    @Test
    public void testBreakLoop() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "check_until");
        IRGenerator.IRInstruction forLoop = block.createInstruction(IRGenerator.OpCode.FOR_LOOP, "x", "range(10)");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "5");
        IRGenerator.IRInstruction breakLoop = block.createInstruction(IRGenerator.OpCode.BREAK_LOOP);

        program.addInstruction(funcDef);
        program.addInstruction(forLoop);
        program.addInstruction(loadConst);
        program.addInstruction(breakLoop);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def check_until():"));
        assertTrue(result.contains("for x in range(10):"));
        assertTrue(result.contains("    pass  # loop body"));
        assertTrue(result.contains("    break"));
    }

    @Test
    public void testContinueLoop() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "filter_even");
        IRGenerator.IRInstruction forLoop = block.createInstruction(IRGenerator.OpCode.FOR_LOOP, "n", "range(10)");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "0");
        IRGenerator.IRInstruction compare = block.createInstruction(IRGenerator.OpCode.CMP_EQ, "__left", "__right");
        IRGenerator.IRInstruction continueLoop = block.createInstruction(IRGenerator.OpCode.CONTINUE_LOOP);

        program.addInstruction(funcDef);
        program.addInstruction(forLoop);
        program.addInstruction(loadConst);
        program.addInstruction(compare);
        program.addInstruction(continueLoop);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def filter_even():"));
        assertTrue(result.contains("for n in range(10):"));
        assertTrue(result.contains("    pass  # loop body"));
        assertTrue(result.contains("    continue"));
    }

    @Test
    public void testNestedLoops() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "nested_loops");
        IRGenerator.IRInstruction outerLoop = block.createInstruction(IRGenerator.OpCode.FOR_LOOP, "i", "range(3)");
        IRGenerator.IRInstruction innerLoop = block.createInstruction(IRGenerator.OpCode.FOR_LOOP, "j", "range(3)");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "0");

        program.addInstruction(funcDef);
        program.addInstruction(outerLoop);
        program.addInstruction(innerLoop);
        program.addInstruction(loadConst);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def nested_loops():"));
        assertTrue(result.contains("for i in range(3):"));
        assertTrue(result.contains("for j in range(3):"));
        assertTrue(result.contains("    pass  # loop body"));
    }

    @Test
    public void testLoopWithCondition() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "while_condition");
        IRGenerator.IRInstruction whileLoop = block.createInstruction(IRGenerator.OpCode.WHILE_LOOP, "while_label");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "0");
        IRGenerator.IRInstruction storeVar = block.createInstruction(IRGenerator.OpCode.STORE_VAR, "count");
        IRGenerator.IRInstruction add = block.createInstruction(IRGenerator.OpCode.ADD, "__left", "__right");
        IRGenerator.IRInstruction compare = block.createInstruction(IRGenerator.OpCode.CMP_LT, "__left", "__right");
        IRGenerator.IRInstruction breakLoop = block.createInstruction(IRGenerator.OpCode.BREAK_LOOP);

        program.addInstruction(funcDef);
        program.addInstruction(whileLoop);
        program.addInstruction(loadConst);
        program.addInstruction(storeVar);
        program.addInstruction(add);
        program.addInstruction(compare);
        program.addInstruction(breakLoop);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def while_condition():"));
        assertTrue(result.contains("while True:"));
        assertTrue(result.contains("    if not __condition:"));
        assertTrue(result.contains("        break"));
        assertTrue(result.contains("count = 0"));
        assertTrue(result.contains("__stack_top = count + 1"));
    }
}

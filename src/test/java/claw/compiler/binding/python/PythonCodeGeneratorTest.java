package claw.compiler.binding.python;

import claw.compiler.generators.ClawIR;
import claw.compiler.generators.IRGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonCodeGenerator 测试类
 *
 * 测试 Python 目标代码生成器的所有功能
 */
public class PythonCodeGeneratorTest {

    @Test
    public void testFunctionDefinition() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        // 创建简单的 IR
        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "myFunction");
        IRGenerator.IRInstruction funcEnd = block.createInstruction(IRGenerator.OpCode.FUNC_END);

        program.addInstruction(funcDef);
        program.addInstruction(funcEnd);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def myFunction():"));
        assertTrue(result.contains("    pass  # Function body"));
    }

    @Test
    public void testScopeManagement() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction scopeEnter = block.createInstruction(IRGenerator.OpCode.SCOPE_ENTER, "myScope");
        IRGenerator.IRInstruction scopeExit = block.createInstruction(IRGenerator.OpCode.SCOPE_EXIT, "myScope");

        program.addInstruction(scopeEnter);
        program.addInstruction(scopeExit);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("=== Scope: myScope ==="));
        assertTrue(result.contains("=== Exit scope: myScope ==="));
    }

    @Test
    public void testArrayBoundaryCheck() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction arrayNew = block.createInstruction(IRGenerator.OpCode.ARRAY_NEW, "Int", "10");
        IRGenerator.IRInstruction arrayGet = block.createInstruction(IRGenerator.OpCode.ARRAY_GET, "arr", "i");
        IRGenerator.IRInstruction arraySet = block.createInstruction(IRGenerator.OpCode.ARRAY_SET, "arr", "i", "val");

        program.addInstruction(arrayNew);
        program.addInstruction(arrayGet);
        program.addInstruction(arraySet);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("if 0 <= i < len(arr):"));
        assertTrue(result.contains("    __stack_top = arr[i]"));
        assertTrue(result.contains("else:"));
        assertTrue(result.contains("    raise IndexError('Array index out of bounds')"));
    }

    @Test
    public void testTypeDefinition() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction typeDef = block.createInstruction(IRGenerator.OpCode.TYPE_DEF, "MyClass");

        program.addInstruction(typeDef);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("class MyClass:"));
        assertTrue(result.contains("    pass  # Type definition"));
    }

    @Test
    public void testLoopControlFlow() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction whileLoop = block.createInstruction(IRGenerator.OpCode.WHILE_LOOP, "loopLabel");
        IRGenerator.IRInstruction breakLoop = block.createInstruction(IRGenerator.OpCode.BREAK_LOOP);
        IRGenerator.IRInstruction forLoop = block.createInstruction(IRGenerator.OpCode.FOR_LOOP, "item", "items");
        IRGenerator.IRInstruction continueLoop = block.createInstruction(IRGenerator.OpCode.CONTINUE_LOOP);

        program.addInstruction(whileLoop);
        program.addInstruction(breakLoop);
        program.addInstruction(forLoop);
        program.addInstruction(continueLoop);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("while True:"));
        assertTrue(result.contains("    if not __condition:"));
        assertTrue(result.contains("        break"));
        assertTrue(result.contains("for item in items:"));
        assertTrue(result.contains("    break"));
        assertTrue(result.contains("    pass  # loop body"));
        assertTrue(result.contains("    continue"));
    }

    @Test
    public void testVariableOperations() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "42");
        IRGenerator.IRInstruction loadVar = block.createInstruction(IRGenerator.OpCode.LOAD_VAR, "x");
        IRGenerator.IRInstruction storeVar = block.createInstruction(IRGenerator.OpCode.STORE_VAR, "x");

        program.addInstruction(loadConst);
        program.addInstruction(loadVar);
        program.addInstruction(storeVar);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("__stack_top = 42"));
        assertTrue(result.contains("__stack_top = x"));
        assertTrue(result.contains("x = __stack_top"));
    }

    @Test
    public void testArithmeticOperations() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction add = block.createInstruction(IRGenerator.OpCode.ADD, "__left", "__right");
        IRGenerator.IRInstruction sub = block.createInstruction(IRGenerator.OpCode.SUB, "__left", "__right");
        IRGenerator.IRInstruction mul = block.createInstruction(IRGenerator.OpCode.MUL, "__left", "__right");
        IRGenerator.IRInstruction div = block.createInstruction(IRGenerator.OpCode.DIV, "__left", "__right");
        IRGenerator.IRInstruction mod = block.createInstruction(IRGenerator.OpCode.MOD, "__left", "__right");

        program.addInstruction(add);
        program.addInstruction(sub);
        program.addInstruction(mul);
        program.addInstruction(div);
        program.addInstruction(mod);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("__stack_top = __left + __right"));
        assertTrue(result.contains("__stack_top = __left - __right"));
        assertTrue(result.contains("__stack_top = __left * __right"));
        assertTrue(result.contains("__stack_top = __left / __right"));
        assertTrue(result.contains("__stack_top = __left % __right"));
    }

    @Test
    public void testComparisonOperations() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction eq = block.createInstruction(IRGenerator.OpCode.CMP_EQ, "__left", "__right");
        IRGenerator.IRInstruction ne = block.createInstruction(IRGenerator.OpCode.CMP_NE, "__left", "__right");
        IRGenerator.IRInstruction lt = block.createInstruction(IRGenerator.OpCode.CMP_LT, "__left", "__right");
        IRGenerator.IRInstruction gt = block.createInstruction(IRGenerator.OpCode.CMP_GT, "__left", "__right");

        program.addInstruction(eq);
        program.addInstruction(ne);
        program.addInstruction(lt);
        program.addInstruction(gt);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("__stack_top = __left == __right"));
        assertTrue(result.contains("__stack_top = __left != __right"));
        assertTrue(result.contains("__stack_top = __left < __right"));
        assertTrue(result.contains("__stack_top = __left > __right"));
    }

    @Test
    public void testLogicalOperations() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction andOp = block.createInstruction(IRGenerator.OpCode.AND, "__left", "__right");
        IRGenerator.IRInstruction orOp = block.createInstruction(IRGenerator.OpCode.OR, "__left", "__right");
        IRGenerator.IRInstruction notOp = block.createInstruction(IRGenerator.OpCode.NOT, "__operand");

        program.addInstruction(andOp);
        program.addInstruction(orOp);
        program.addInstruction(notOp);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("__stack_top = __left and __right"));
        assertTrue(result.contains("__stack_top = __left or __right"));
        assertTrue(result.contains("__stack_top = not __operand"));
    }
}

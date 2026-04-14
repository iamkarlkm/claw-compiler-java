package claw.compiler.binding.python;

import claw.compiler.generators.ClawIR;
import claw.compiler.generators.IRGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonCodeGenerator 异常处理增强测试
 */
public class PythonExceptionHandlingTest {

    @Test
    public void testTryCatchBlock() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("block1", "func_block", 0);

        // Create instructions
        IRGenerator.IRInstruction funcDef = new IRGenerator.IRInstruction(IRGenerator.OpCode.FUNC_DEF, 1, "test.claw", "safe_division");
        IRGenerator.IRInstruction tryBlock = new IRGenerator.IRInstruction(IRGenerator.OpCode.TRY_BLOCK, 2, "test.claw");
        IRGenerator.IRInstruction loadConst1 = new IRGenerator.IRInstruction(IRGenerator.OpCode.LOAD_CONST, 3, "test.claw", "10");
        IRGenerator.IRInstruction loadConst2 = new IRGenerator.IRInstruction(IRGenerator.OpCode.LOAD_CONST, 4, "test.claw", "0");
        IRGenerator.IRInstruction divide = new IRGenerator.IRInstruction(IRGenerator.OpCode.DIV, 5, "test.claw", "__left", "__right");
        IRGenerator.IRInstruction catchEx = new IRGenerator.IRInstruction(IRGenerator.OpCode.EXCEPTION_CATCH, 6, "test.claw", "ZeroDivisionError", "e", "print('Caught exception')");
        IRGenerator.IRInstruction returnStmt = new IRGenerator.IRInstruction(IRGenerator.OpCode.RETURN, 7, "test.claw", "__stack_top");

        block.addInstruction(funcDef);
        block.addInstruction(tryBlock);
        block.addInstruction(loadConst1);
        block.addInstruction(loadConst2);
        block.addInstruction(divide);
        block.addInstruction(catchEx);
        block.addInstruction(returnStmt);

        program.addTopLevelBlock(block);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def safe_division():"));
        assertTrue(result.contains("try:"));
        assertTrue(result.contains("except ZeroDivisionError as e:"));
        assertTrue(result.contains("print('Caught exception')"));
    }

    @Test
    public void testMultiExceptionCatch() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("block1", "func_block", 0);

        // Create instructions
        IRGenerator.IRInstruction funcDef = new IRGenerator.IRInstruction(IRGenerator.OpCode.FUNC_DEF, 1, "test.claw", "handle_errors");
        IRGenerator.IRInstruction tryBlock = new IRGenerator.IRInstruction(IRGenerator.OpCode.TRY_BLOCK, 2, "test.claw");
        IRGenerator.IRInstruction loadConst = new IRGenerator.IRInstruction(IRGenerator.OpCode.LOAD_CONST, 3, "test.claw", "1");
        IRGenerator.IRInstruction storeVar = new IRGenerator.IRInstruction(IRGenerator.OpCode.STORE_VAR, 4, "test.claw", "x");
        IRGenerator.IRInstruction catchMulti = new IRGenerator.IRInstruction(IRGenerator.OpCode.MULTI_EXCEPTION_CATCH, 5, "test.claw", "ValueError", "RuntimeError", "e", "handle exception");
        IRGenerator.IRInstruction returnStmt = new IRGenerator.IRInstruction(IRGenerator.OpCode.RETURN, 6, "test.claw", "__stack_top");

        block.addInstruction(funcDef);
        block.addInstruction(tryBlock);
        block.addInstruction(loadConst);
        block.addInstruction(storeVar);
        block.addInstruction(catchMulti);
        block.addInstruction(returnStmt);

        program.addTopLevelBlock(block);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def handle_errors():"));
        assertTrue(result.contains("except (ValueError, RuntimeError) as e:"));
        assertTrue(result.contains("handle exception"));
    }

    @Test
    public void testFinallyBlock() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("block1", "func_block", 0);

        // Create instructions
        IRGenerator.IRInstruction funcDef = new IRGenerator.IRInstruction(IRGenerator.OpCode.FUNC_DEF, 1, "test.claw", "cleanup");
        IRGenerator.IRInstruction tryBlock = new IRGenerator.IRInstruction(IRGenerator.OpCode.TRY_BLOCK, 2, "test.claw");
        IRGenerator.IRInstruction loadConst = new IRGenerator.IRInstruction(IRGenerator.OpCode.LOAD_CONST, 3, "test.claw", "100");
        IRGenerator.IRInstruction storeVar = new IRGenerator.IRInstruction(IRGenerator.OpCode.STORE_VAR, 4, "test.claw", "resource");
        IRGenerator.IRInstruction finallyBlock = new IRGenerator.IRInstruction(IRGenerator.OpCode.FINALLY, 5, "test.claw", "close_resource()");
        IRGenerator.IRInstruction returnStmt = new IRGenerator.IRInstruction(IRGenerator.OpCode.RETURN, 6, "test.claw", "__stack_top");

        block.addInstruction(funcDef);
        block.addInstruction(tryBlock);
        block.addInstruction(loadConst);
        block.addInstruction(storeVar);
        block.addInstruction(finallyBlock);
        block.addInstruction(returnStmt);

        program.addTopLevelBlock(block);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def cleanup():"));
        assertTrue(result.contains("try:"));
        assertTrue(result.contains("finally:"));
        assertTrue(result.contains("close_resource()"));
    }

    @Test
    public void testTryCatchFinally() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("block1", "func_block", 0);

        // Create instructions
        IRGenerator.IRInstruction funcDef = new IRGenerator.IRInstruction(IRGenerator.OpCode.FUNC_DEF, 1, "test.claw", "secure_operation");
        IRGenerator.IRInstruction tryBlock = new IRGenerator.IRInstruction(IRGenerator.OpCode.TRY_BLOCK, 2, "test.claw");
        IRGenerator.IRInstruction loadConst = new IRGenerator.IRInstruction(IRGenerator.OpCode.LOAD_CONST, 3, "test.claw", "42");
        IRGenerator.IRInstruction storeVar = new IRGenerator.IRInstruction(IRGenerator.OpCode.STORE_VAR, 4, "test.claw", "data");
        IRGenerator.IRInstruction catchEx = new IRGenerator.IRInstruction(IRGenerator.OpCode.EXCEPTION_CATCH, 5, "test.claw", "Exception", "e", "log_error(e)");
        IRGenerator.IRInstruction finallyBlock = new IRGenerator.IRInstruction(IRGenerator.OpCode.FINALLY, 6, "test.claw", "release_resources()");
        IRGenerator.IRInstruction returnStmt = new IRGenerator.IRInstruction(IRGenerator.OpCode.RETURN, 7, "test.claw", "__stack_top");

        block.addInstruction(funcDef);
        block.addInstruction(tryBlock);
        block.addInstruction(loadConst);
        block.addInstruction(storeVar);
        block.addInstruction(catchEx);
        block.addInstruction(finallyBlock);
        block.addInstruction(returnStmt);

        program.addTopLevelBlock(block);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def secure_operation():"));
        assertTrue(result.contains("try:"));
        assertTrue(result.contains("except Exception as e:"));
        assertTrue(result.contains("log_error(e)"));
        assertTrue(result.contains("finally:"));
        assertTrue(result.contains("release_resources()"));
    }

    @Test
    public void testExceptionThrowsDeclaration() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("block1", "func_block", 0);

        // Create instructions
        IRGenerator.IRInstruction funcDef = new IRGenerator.IRInstruction(IRGenerator.OpCode.FUNC_DEF, 1, "test.claw", "may_raise_error");
        IRGenerator.IRInstruction loadConst = new IRGenerator.IRInstruction(IRGenerator.OpCode.LOAD_CONST, 2, "test.claw", "5");
        IRGenerator.IRInstruction throwEx = new IRGenerator.IRInstruction(IRGenerator.OpCode.EXCEPTION_THROWS, 3, "test.claw", "RuntimeError");

        block.addInstruction(funcDef);
        block.addInstruction(loadConst);
        block.addInstruction(throwEx);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def may_raise_error():"));
        assertTrue(result.contains("# Raises: RuntimeError"));
    }

    @Test
    public void testExceptionWithMultipleTypes() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("block1", "func_block", 0);

        // Create instructions
        IRGenerator.IRInstruction funcDef = new IRGenerator.IRInstruction(IRGenerator.OpCode.FUNC_DEF, 1, "test.claw", "handle_various_errors");
        IRGenerator.IRInstruction tryBlock = new IRGenerator.IRInstruction(IRGenerator.OpCode.TRY_BLOCK, 2, "test.claw");
        IRGenerator.IRInstruction loadConst = new IRGenerator.IRInstruction(IRGenerator.OpCode.LOAD_CONST, 3, "test.claw", "10");
        IRGenerator.IRInstruction storeVar = new IRGenerator.IRInstruction(IRGenerator.OpCode.STORE_VAR, 4, "test.claw", "value");
        IRGenerator.IRInstruction loadConst2 = new IRGenerator.IRInstruction(IRGenerator.OpCode.LOAD_CONST, 5, "test.claw", "5");
        IRGenerator.IRInstruction compare = new IRGenerator.IRInstruction(IRGenerator.OpCode.CMP_EQ, 6, "test.claw", "__left", "__right");
        IRGenerator.IRInstruction catchMulti = new IRGenerator.IRInstruction(IRGenerator.OpCode.MULTI_EXCEPTION_CATCH, 7, "test.claw", "TypeError", "KeyError", "ValueError", "e", "handle_error");
        IRGenerator.IRInstruction returnStmt = new IRGenerator.IRInstruction(IRGenerator.OpCode.RETURN, 8, "test.claw", "__stack_top");

        block.addInstruction(funcDef);
        block.addInstruction(tryBlock);
        block.addInstruction(loadConst);
        block.addInstruction(storeVar);
        block.addInstruction(loadConst2);
        block.addInstruction(compare);
        block.addInstruction(catchMulti);
        block.addInstruction(returnStmt);

        program.addTopLevelBlock(block);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def handle_various_errors():"));
        assertTrue(result.contains("except (TypeError, KeyError, ValueError) as e:"));
        assertTrue(result.contains("handle_error"));
    }
}

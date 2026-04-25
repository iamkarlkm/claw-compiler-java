package com.q3lives.compiler.optimizers;

import com.q3lives.compiler.generators.IRGenerator;
import com.q3lives.compiler.generators.IRGenerator.OpCode;
import com.q3lives.ir.ClawIR;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 优化器单元测试
 *
 * 验证 ConstantFoldingPass 和 DeadCodeEliminationPass 的核心优化行为。
 */
class OptimizerTest {

    private ClawIR createSimpleIR() {
        return new ClawIR();
    }

    private IRGenerator.IRBasicBlock createBlock(String id, String type) {
        return new IRGenerator.IRBasicBlock(id, type, 0);
    }

    private IRGenerator.IRInstruction inst(OpCode op, Object... operands) {
        return new IRGenerator.IRInstruction(op, 1, "test.claw", operands);
    }

    // ==================== ConstantFoldingPass 测试 ====================

    @Test
    void testConstantFoldingAdd() {
        ClawIR ir = createSimpleIR();
        IRGenerator.IRBasicBlock block = createBlock("b1", "BLOCK");
        block.addInstruction(inst(OpCode.LOAD_CONST, 2));
        block.addInstruction(inst(OpCode.LOAD_CONST, 3));
        block.addInstruction(inst(OpCode.ADD));
        ir.getIrProgram().addTopLevelBlock(block);

        OptimizationPass pass = new ConstantFoldingPass();
        ClawIR result = pass.optimize(ir);

        var insts = result.getIrProgram().getTopLevelBlocks().get(0).getInstructions();
        assertEquals(1, insts.size(), "3 条指令应折叠为 1 条");
        assertEquals(OpCode.LOAD_CONST, insts.get(0).getOpCode());
        Object actual = insts.get(0).getOperands().get(0);
        assertTrue(actual instanceof Number, "操作数应为 Number 类型");
        assertEquals(5, ((Number) actual).intValue(), "2 + 3 = 5");
    }

    @Test
    void testConstantFoldingSub() {
        ClawIR ir = createSimpleIR();
        IRGenerator.IRBasicBlock block = createBlock("b1", "BLOCK");
        block.addInstruction(inst(OpCode.LOAD_CONST, 10));
        block.addInstruction(inst(OpCode.LOAD_CONST, 3));
        block.addInstruction(inst(OpCode.SUB));
        ir.getIrProgram().addTopLevelBlock(block);

        OptimizationPass pass = new ConstantFoldingPass();
        pass.optimize(ir);

        var insts = ir.getIrProgram().getTopLevelBlocks().get(0).getInstructions();
        assertEquals(1, insts.size());
        assertEquals(7, ((Number) insts.get(0).getOperands().get(0)).intValue(), "10 - 3 = 7");
    }

    @Test
    void testConstantFoldingMul() {
        ClawIR ir = createSimpleIR();
        IRGenerator.IRBasicBlock block = createBlock("b1", "BLOCK");
        block.addInstruction(inst(OpCode.LOAD_CONST, 4));
        block.addInstruction(inst(OpCode.LOAD_CONST, 5));
        block.addInstruction(inst(OpCode.MUL));
        ir.getIrProgram().addTopLevelBlock(block);

        new ConstantFoldingPass().optimize(ir);

        var insts = ir.getIrProgram().getTopLevelBlocks().get(0).getInstructions();
        assertEquals(20, ((Number) insts.get(0).getOperands().get(0)).intValue(), "4 * 5 = 20");
    }

    @Test
    void testConstantFoldingStringConcat() {
        ClawIR ir = createSimpleIR();
        IRGenerator.IRBasicBlock block = createBlock("b1", "BLOCK");
        block.addInstruction(inst(OpCode.LOAD_CONST, "hello"));
        block.addInstruction(inst(OpCode.LOAD_CONST, " world"));
        block.addInstruction(inst(OpCode.ADD));
        ir.getIrProgram().addTopLevelBlock(block);

        new ConstantFoldingPass().optimize(ir);

        var insts = ir.getIrProgram().getTopLevelBlocks().get(0).getInstructions();
        assertEquals("hello world", insts.get(0).getOperands().get(0));
    }

    @Test
    void testConstantFoldingNoMatch() {
        // 非 LOAD_CONST, LOAD_CONST, OP 的模式不应被折叠
        ClawIR ir = createSimpleIR();
        IRGenerator.IRBasicBlock block = createBlock("b1", "BLOCK");
        block.addInstruction(inst(OpCode.LOAD_CONST, 2));
        block.addInstruction(inst(OpCode.LOAD_VAR, "x"));
        block.addInstruction(inst(OpCode.ADD));
        ir.getIrProgram().addTopLevelBlock(block);

        new ConstantFoldingPass().optimize(ir);

        var insts = ir.getIrProgram().getTopLevelBlocks().get(0).getInstructions();
        assertEquals(3, insts.size(), "包含变量的表达式不应被折叠");
    }

    @Test
    void testConstantFoldingMultipleFolds() {
        ClawIR ir = createSimpleIR();
        IRGenerator.IRBasicBlock block = createBlock("b1", "BLOCK");
        block.addInstruction(inst(OpCode.LOAD_CONST, 1));
        block.addInstruction(inst(OpCode.LOAD_CONST, 2));
        block.addInstruction(inst(OpCode.ADD));
        block.addInstruction(inst(OpCode.LOAD_CONST, 3));
        block.addInstruction(inst(OpCode.MUL));
        ir.getIrProgram().addTopLevelBlock(block);

        new ConstantFoldingPass().optimize(ir);

        var insts = ir.getIrProgram().getTopLevelBlocks().get(0).getInstructions();
        assertEquals(1, insts.size(), "两次折叠后应只剩 1 条指令");
        assertEquals(9, ((Number) insts.get(0).getOperands().get(0)).intValue(), "(1+2)*3 = 9");
    }

    // ==================== DeadCodeEliminationPass 测试 ====================

    @Test
    void testRemoveNop() {
        ClawIR ir = createSimpleIR();
        IRGenerator.IRBasicBlock block = createBlock("b1", "BLOCK");
        block.addInstruction(inst(OpCode.NOP));
        block.addInstruction(inst(OpCode.LOAD_CONST, 42));
        ir.getIrProgram().addTopLevelBlock(block);

        new DeadCodeEliminationPass().optimize(ir);

        var insts = ir.getIrProgram().getTopLevelBlocks().get(0).getInstructions();
        assertEquals(1, insts.size(), "NOP 应被移除");
        assertEquals(OpCode.LOAD_CONST, insts.get(0).getOpCode());
    }

    @Test
    void testRemoveUnreachableAfterReturn() {
        ClawIR ir = createSimpleIR();
        IRGenerator.IRBasicBlock block = createBlock("b1", "BLOCK");
        block.addInstruction(inst(OpCode.LOAD_CONST, 1));
        block.addInstruction(inst(OpCode.RETURN));
        block.addInstruction(inst(OpCode.LOAD_CONST, 2)); // 死代码
        block.addInstruction(inst(OpCode.ADD));             // 死代码
        ir.getIrProgram().addTopLevelBlock(block);

        new DeadCodeEliminationPass().optimize(ir);

        var insts = ir.getIrProgram().getTopLevelBlocks().get(0).getInstructions();
        assertEquals(2, insts.size(), "RETURN 后的代码应被移除");
        assertEquals(OpCode.LOAD_CONST, insts.get(0).getOpCode());
        assertEquals(OpCode.RETURN, insts.get(1).getOpCode());
    }

    @Test
    void testRestoreReachableAfterLabel() {
        ClawIR ir = createSimpleIR();
        IRGenerator.IRBasicBlock block = createBlock("b1", "BLOCK");
        block.addInstruction(inst(OpCode.JUMP));
        IRGenerator.IRInstruction label = inst(OpCode.LABEL);
        label.setLabel("L1");
        block.addInstruction(label);
        block.addInstruction(inst(OpCode.LOAD_CONST, 42));
        ir.getIrProgram().addTopLevelBlock(block);

        new DeadCodeEliminationPass().optimize(ir);

        var insts = ir.getIrProgram().getTopLevelBlocks().get(0).getInstructions();
        assertEquals(3, insts.size(), "LABEL 后的代码应保持可达");
    }

    // ==================== Optimizer 协调器测试 ====================

    @Test
    void testOptimizerPipeline() {
        ClawIR ir = createSimpleIR();
        IRGenerator.IRBasicBlock block = createBlock("b1", "BLOCK");
        block.addInstruction(inst(OpCode.NOP));
        block.addInstruction(inst(OpCode.LOAD_CONST, 2));
        block.addInstruction(inst(OpCode.LOAD_CONST, 3));
        block.addInstruction(inst(OpCode.ADD));
        block.addInstruction(inst(OpCode.RETURN));
        block.addInstruction(inst(OpCode.LOAD_CONST, 99)); // 死代码
        ir.getIrProgram().addTopLevelBlock(block);

        Optimizer optimizer = Optimizer.withDefaultPasses();
        optimizer.optimize(ir);

        var insts = ir.getIrProgram().getTopLevelBlocks().get(0).getInstructions();
        assertEquals(2, insts.size(), "NOP 被移除，常量被折叠，死代码被消除");
        assertEquals(OpCode.LOAD_CONST, insts.get(0).getOpCode());
        assertEquals(5, ((Number) insts.get(0).getOperands().get(0)).intValue(), "2+3=5");
        assertEquals(OpCode.RETURN, insts.get(1).getOpCode());
    }

    @Test
    void testOptimizerInvalidIR() {
        Optimizer optimizer = Optimizer.withDefaultPasses();
        // null IR 不应抛出异常
        assertDoesNotThrow(() -> optimizer.optimize(null));
    }

    @Test
    void testPassNames() {
        assertEquals("ConstantFolding", new ConstantFoldingPass().getName());
        assertEquals("DeadCodeElimination", new DeadCodeEliminationPass().getName());
    }
}

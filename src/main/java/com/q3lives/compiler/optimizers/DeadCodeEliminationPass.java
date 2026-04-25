package com.q3lives.compiler.optimizers;

import com.q3lives.compiler.generators.IRGenerator;
import com.q3lives.compiler.generators.IRGenerator.OpCode;
import com.q3lives.ir.ClawIR;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 死代码消除优化 Pass
 *
 * 移除以下不可达或无用的指令：
 * 1. JUMP 指令后的代码（直到目标 LABEL）
 * 2. 不可达的代码块
 * 3. 未使用的 NOP 指令
 * 4. 紧随 RETURN 后的代码（在同一基本块内）
 */
@Slf4j
public class DeadCodeEliminationPass implements OptimizationPass {

    @Override
    public String getName() {
        return "DeadCodeElimination";
    }

    @Override
    public String getDescription() {
        return "移除不可达和无用的死代码";
    }

    @Override
    public ClawIR optimize(ClawIR ir) {
        if (ir == null || ir.getIrProgram() == null) {
            return ir;
        }

        int removedCount = 0;
        for (IRGenerator.IRBasicBlock block : ir.getIrProgram().getTopLevelBlocks()) {
            removedCount += eliminateInBlock(block);
        }

        if (removedCount > 0) {
            log.info("死代码消除: 移除了 {} 条无用指令", removedCount);
        }
        return ir;
    }

    /**
     * 在单个基本块内消除死代码，递归处理子块。
     */
    private int eliminateInBlock(IRGenerator.IRBasicBlock block) {
        int count = 0;
        for (IRGenerator.IRBasicBlock child : block.getChildren()) {
            count += eliminateInBlock(child);
        }

        List<IRGenerator.IRInstruction> insts = new ArrayList<>(block.getInstructions());
        if (insts.isEmpty()) return count;

        List<IRGenerator.IRInstruction> cleaned = new ArrayList<>();
        boolean unreachable = false;

        for (IRGenerator.IRInstruction inst : insts) {
            OpCode op = inst.getOpCode();

            // NOP 直接跳过
            if (op == OpCode.NOP) {
                count++;
                continue;
            }

            // 遇到无条件跳转或返回后，后续代码不可达
            if (op == OpCode.JUMP || op == OpCode.RETURN) {
                cleaned.add(inst);
                unreachable = true;
                continue;
            }

            // 遇到标签，恢复可达状态
            if (op == OpCode.LABEL) {
                cleaned.add(inst);
                unreachable = false;
                continue;
            }

            // 不可达代码直接丢弃
            if (unreachable) {
                count++;
                continue;
            }

            cleaned.add(inst);
        }

        // 清空并重新添加优化后的指令
        block.clearInstructions();
        block.addInstructions(cleaned);

        return count;
    }
}

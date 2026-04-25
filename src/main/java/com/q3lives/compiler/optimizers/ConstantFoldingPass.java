package com.q3lives.compiler.optimizers;

import com.q3lives.compiler.generators.IRGenerator;
import com.q3lives.compiler.generators.IRGenerator.IRBasicBlock;
import com.q3lives.compiler.generators.IRGenerator.IRInstruction;
import com.q3lives.compiler.generators.IRGenerator.OpCode;
import com.q3lives.ir.ClawIR;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 常量折叠优化 Pass
 *
 * 在编译时计算常量表达式，将类似：
 *   LOAD_CONST 2
 *   LOAD_CONST 3
 *   ADD
 * 折叠为：
 *   LOAD_CONST 5
 *
 * 支持的运算：ADD, SUB, MUL, DIV, MOD, CMP_*, AND, OR, NOT
 */
@Slf4j
public class ConstantFoldingPass implements OptimizationPass {

    @Override
    public String getName() {
        return "ConstantFolding";
    }

    @Override
    public String getDescription() {
        return "在编译时预计算常量表达式";
    }

    @Override
    public ClawIR optimize(ClawIR ir) {
        if (ir == null || ir.getIrProgram() == null) {
            return ir;
        }

        int foldedCount = 0;
        for (IRBasicBlock block : ir.getIrProgram().getTopLevelBlocks()) {
            foldedCount += foldBlock(block);
        }

        if (foldedCount > 0) {
            log.info("常量折叠: 合并了 {} 个常量表达式", foldedCount);
        }
        return ir;
    }

    /**
     * 对单个基本块执行常量折叠，递归处理子块。
     * 循环执行直到没有更多可折叠的常量表达式。
     */
    private int foldBlock(IRBasicBlock block) {
        int count = 0;
        // 先递归处理子块
        for (IRBasicBlock child : block.getChildren()) {
            count += foldBlock(child);
        }

        // 循环折叠直到没有更多可折叠的表达式
        boolean changed;
        do {
            changed = false;
            List<IRInstruction> insts = new ArrayList<>(block.getInstructions());
            List<IRInstruction> folded = new ArrayList<>();

            int i = 0;
            while (i < insts.size()) {
                FoldResult result = tryFoldAt(insts, i);
                if (result != null) {
                    folded.add(result.instruction);
                    i = result.nextIndex;
                    count++;
                    changed = true;
                } else {
                    folded.add(insts.get(i));
                    i++;
                }
            }

            block.clearInstructions();
            block.addInstructions(folded);
        } while (changed);

        return count;
    }

    /**
     * 尝试从索引 i 开始折叠常量表达式。
     *
     * @return 折叠结果，如果无法折叠返回 null
     */
    private FoldResult tryFoldAt(List<IRInstruction> insts, int i) {
        if (i + 2 >= insts.size()) return null;

        IRInstruction inst1 = insts.get(i);
        IRInstruction inst2 = insts.get(i + 1);
        IRInstruction inst3 = insts.get(i + 2);

        // 模式：LOAD_CONST a, LOAD_CONST b, BINARY_OP → LOAD_CONST result
        if (inst1.getOpCode() != OpCode.LOAD_CONST || inst2.getOpCode() != OpCode.LOAD_CONST) {
            return null;
        }

        Object val1 = getConstValue(inst1);
        Object val2 = getConstValue(inst2);
        if (val1 == null || val2 == null) return null;

        Object result = computeBinary(val1, val2, inst3.getOpCode());
        if (result == null) return null;

        IRInstruction folded = new IRInstruction(
            OpCode.LOAD_CONST,
            inst3.getSourceLineNumber(),
            inst3.getSourceFile(),
            result
        );
        folded.setComment("folded: " + val1 + " " + inst3.getOpCode() + " " + val2);

        return new FoldResult(folded, i + 3);
    }

    /**
     * 从 LOAD_CONST 指令中提取常量值。
     */
    private Object getConstValue(IRInstruction inst) {
        List<Object> ops = inst.getOperands();
        return ops.isEmpty() ? null : ops.get(0);
    }

    /**
     * 计算二元常量运算结果。
     *
     * @return 计算结果，如果不支持该运算返回 null
     */
    private Object computeBinary(Object left, Object right, OpCode op) {
        try {
            // 数值运算（Int 或 Float）
            if (left instanceof Number && right instanceof Number) {
                double a = ((Number) left).doubleValue();
                double b = ((Number) right).doubleValue();

                // 整数运算优先保持整数结果
                boolean bothInt = left instanceof Integer && right instanceof Integer;

                // 使用 if-else 而非 switch 表达式，避免 Java 条件表达式类型提升导致 int 变成 double
                if (op == OpCode.ADD) {
                    return bothInt ? Integer.valueOf((int) (a + b)) : Double.valueOf(a + b);
                }
                if (op == OpCode.SUB) {
                    return bothInt ? Integer.valueOf((int) (a - b)) : Double.valueOf(a - b);
                }
                if (op == OpCode.MUL) {
                    return bothInt ? Integer.valueOf((int) (a * b)) : Double.valueOf(a * b);
                }
                if (op == OpCode.DIV) {
                    return Double.valueOf(a / b);
                }
                if (op == OpCode.MOD) {
                    return bothInt ? Integer.valueOf((int) (a % b)) : Double.valueOf(a % b);
                }
                if (op == OpCode.CMP_EQ) return Boolean.valueOf(a == b);
                if (op == OpCode.CMP_NE) return Boolean.valueOf(a != b);
                if (op == OpCode.CMP_LT) return Boolean.valueOf(a < b);
                if (op == OpCode.CMP_GT) return Boolean.valueOf(a > b);
                if (op == OpCode.CMP_LE) return Boolean.valueOf(a <= b);
                if (op == OpCode.CMP_GE) return Boolean.valueOf(a >= b);
                return null;
            }

            // 字符串拼接
            if (op == OpCode.ADD && left instanceof String && right instanceof String) {
                return left.toString() + right.toString();
            }

            // 布尔运算
            if (left instanceof Boolean && right instanceof Boolean) {
                boolean a = (Boolean) left;
                boolean b = (Boolean) right;
                return switch (op) {
                    case AND    -> a && b;
                    case OR     -> a || b;
                    case CMP_EQ -> a == b;
                    case CMP_NE -> a != b;
                    default     -> null;
                };
            }

            // 一元 NOT 需要单独处理：LOAD_CONST true, NOT
            if (op == OpCode.NOT && left instanceof Boolean) {
                return !(Boolean) left;
            }

        } catch (Exception e) {
            log.debug("常量折叠计算失败: {} {} {}", left, op, right);
        }
        return null;
    }

    private record FoldResult(IRInstruction instruction, int nextIndex) {}
}

package com.q3lives.compiler.optimizers;

import com.q3lives.ir.ClawIR;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 优化器 - 协调所有优化 Pass 的执行。
 *
 * 使用 pipeline 模式依次运行注册的 OptimizationPass，
 * 每个 Pass 接收上一个 Pass 的输出作为输入。
 */
@Slf4j
public class Optimizer {

    private final List<OptimizationPass> passes;

    public Optimizer() {
        this.passes = new ArrayList<>();
    }

    /**
     * 注册一个优化 Pass。
     */
    public void registerPass(OptimizationPass pass) {
        passes.add(pass);
        log.debug("注册优化 Pass: {}", pass.getName());
    }

    /**
     * 使用默认 Pass 列表构造优化器。
     */
    public static Optimizer withDefaultPasses() {
        Optimizer optimizer = new Optimizer();
        optimizer.registerPass(new ConstantFoldingPass());
        optimizer.registerPass(new DeadCodeEliminationPass());
        return optimizer;
    }

    /**
     * 依次执行所有注册的优化 Pass。
     *
     * @param ir 输入的中间表示
     * @return 优化后的中间表示
     */
    public ClawIR optimize(ClawIR ir) {
        if (ir == null || !ir.isValid()) {
            log.warn("IR 无效或为空，跳过优化");
            return ir;
        }

        ClawIR current = ir;
        for (OptimizationPass pass : passes) {
            try {
                log.info("执行优化 Pass: {}", pass.getName());
                current = pass.optimize(current);
            } catch (Exception e) {
                log.error("优化 Pass '{}' 执行失败: {}", pass.getName(), e.getMessage(), e);
            }
        }
        return current;
    }

    /**
     * 获取已注册的 Pass 列表。
     */
    public List<OptimizationPass> getPasses() {
        return List.copyOf(passes);
    }
}

package com.q3lives.compiler.optimizers;

import com.q3lives.ir.ClawIR;

/**
 * 优化 Pass 接口
 *
 * 所有 IR 优化器必须实现此接口，对 ClawIR 进行特定优化转换。
 */
public interface OptimizationPass {

    /**
     * 执行优化，返回优化后的 ClawIR。
     *
     * @param ir 输入的中间表示
     * @return 优化后的中间表示（可能为同一对象）
     */
    ClawIR optimize(ClawIR ir);

    /**
     * 获取该优化 Pass 的名称。
     */
    String getName();

    /**
     * 获取该优化 Pass 的简要描述。
     */
    String getDescription();
}

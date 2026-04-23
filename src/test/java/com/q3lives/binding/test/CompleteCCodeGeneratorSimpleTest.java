package com.q3lives.binding.test;

import com.q3lives.binding.c.CompleteCCodeGenerator;
import com.q3lives.ir.ClawIR;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 简化的CompleteCCodeGenerator测试
 *
 * 验证基本功能，避免复杂的依赖问题
 */
public class CompleteCCodeGeneratorSimpleTest {

    @Test
    void testBasicGeneration() {
        System.out.println("\n=== 测试基本C代码生成 ===");

        try {
            // 创建代码生成器
            CompleteCCodeGenerator generator = new CompleteCCodeGenerator();

            // 创建简单的IR
            ClawIR ir = createSimpleIR();

            // 生成代码
            String cCode = generator.generate(ir);

            // 基本验证
            assertNotNull(cCode);
            assertFalse(cCode.isEmpty());

            System.out.println("基本代码生成成功 ✓");
            System.out.println("生成的代码长度: " + cCode.length());

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("基本代码生成失败");
        }
    }

    /**
     * 创建简单的IR
     */
    private ClawIR createSimpleIR() {
        // 创建空的IR进行测试
        return new ClawIR();
    }
}
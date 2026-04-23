// ==================== BlockProcessor.java (基类) ====================
package com.q3lives.compiler.processors.blocks;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.hierarchy.BlockType;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.compiler.frontend.ASTNode;
import com.q3lives.compiler.pipeline.CompilationException;

import java.util.List;
import java.util.Set;

/**
 * 块处理器抽象基类
 *
 * <p>所有块处理器的基类，定义了处理器的基本接口和通用方法。
 * 子类需要实现 {@link #doProcess(CodeBlock, List)} 方法来处理特定类型的代码块。</p>
 *
 * <h2>处理器职责</h2>
 * <ul>
 *   <li>判断是否能处理指定类型的代码块 ({@link #canProcess(CodeBlock)})</li>
 *   <li>将代码块转换为 AST 节点 ({@link #process(CodeBlock, List)})</li>
 *   <li>处理子块（可选）</li>
 * </ul>
 *
 * <h2>错误处理</h2>
 * <p>处理器应使用 {@link ProcessingException} 报告处理错误，
 * 而不是返回 null 或抛出运行时异常。</p>
 *
 * @author Claw Compiler Team
 * @since 3.0.0
 * @see CodeBlock
 * @see ASTNode
 */
public abstract class BlockProcessor {

    /**
     * 处理代码块，生成 AST 节点
     *
     * <p>此方法提供统一的错误处理和日志记录。
     * 子类应实现 {@link #doProcess(CodeBlock, List)} 方法。</p>
     *
     * @param block 要处理的代码块，不应为 null
     * @param tokens Token 列表，不应为 null
     * @return 生成的 AST 节点，如果块不适用则返回 null
     * @throws ProcessingException 如果处理过程中发生错误
     * @throws IllegalArgumentException 如果参数为 null
     */
    public ASTNode process(CodeBlock block, List<Token> tokens) {
        validateInput(block, tokens);

        if (!canProcess(block)) {
            return null;
        }

        try {
            return doProcess(block, tokens);
        } catch (ProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessingException(
                "处理块时发生意外错误: " + block.getBlockType(),
                block.getStartLine(),
                e
            );
        }
    }

    /**
     * 判断该处理器是否能处理指定类型的代码块
     *
     * @param block 要检查的代码块
     * @return 如果能处理返回 true，否则返回 false
     */
    public abstract boolean canProcess(CodeBlock block);

    /**
     * 获取该处理器支持的块类型集合
     *
     * @return 支持的块类型集合，不应为 null
     */
    public abstract Set<BlockType> getSupportedBlockTypes();

    /**
     * 实际处理逻辑，由子类实现
     *
     * @param block 要处理的代码块
     * @param tokens Token 列表
     * @return 生成的 AST 节点
     * @throws ProcessingException 如果处理过程中发生错误
     */
    protected abstract ASTNode doProcess(CodeBlock block, List<Token> tokens);

    // ==================== 输入验证 ====================

    /**
     * 验证输入参数
     *
     * @param block 代码块
     * @param tokens Token 列表
     * @throws IllegalArgumentException 如果参数无效
     */
    protected void validateInput(CodeBlock block, List<Token> tokens) {
        if (block == null) {
            throw new IllegalArgumentException("代码块不能为 null");
        }
        if (tokens == null) {
            throw new IllegalArgumentException("Token 列表不能为 null");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 处理子块
     *
     * @param parent 父节点
     * @param children 子块列表
     * @param tokens Token 列表
     */
    protected void processChildren(ASTNode parent, List<CodeBlock> children, List<Token> tokens) {
        if (parent == null || children == null) return;

        for (CodeBlock child : children) {
            ASTNode childNode = process(child, tokens);
            if (childNode != null) {
                parent.addChild(childNode);
            }
        }
    }

    // ==================== 异常类 ====================

    /**
     * 处理异常
     *
     * <p>用于报告块处理过程中的错误，包含行号信息以便定位问题。</p>
     */
    public static class ProcessingException extends CompilationException {

        private final int line;
        private final String blockTypeName;
        
        private BlockType blockType;

        /**
         * 创建处理异常
         *
         * @param message 错误消息
         * @param line 发生错误的行号
         */
        public ProcessingException(String message, int line) {
            super(message);
            this.line = line;
            this.blockTypeName = null;
        }

        /**
         * 创建处理异常，带原因
         *
         * @param message 错误消息
         * @param line 发生错误的行号
         * @param cause 原因异常
         */
        public ProcessingException(String message, int line, Throwable cause) {
            super(message, cause);
            this.line = line;
            this.blockTypeName = null;
        }

        /**
         * 创建处理异常，带块类型信息
         *
         * @param message 错误消息
         * @param line 发生错误的行号
         * @param blockType 代码块类型
         */
        public ProcessingException(String message, int line, String blockType) {
            super(message);
            this.line = line;
            this.blockTypeName = blockType;
        }

     

      public ProcessingException(String message, int line, BlockType blockType) {
            super(message);
            this.line = line;
            this.blockType = blockType;
            this.blockTypeName = blockType.name();
        }

       

        /**
         * 获取发生错误的行号
         *
         * @return 行号
         */
        public int getLine() {
            return line;
        }

        /**
         * 获取代码块类型
         *
         * @return 块类型名称，可能为 null
         */
        public String getBlockType() {
            return blockTypeName;
        }

        @Override
        public String toString() {
            String location = "行 " + line;
            if (blockType != null) {
                location += " (" + blockType + ")";
            }
            return "ProcessingException: " + getMessage() + " at " + location;
        }
    }
}

// ==================== ExpressionBlockProcessor.java ====================
package com.q3lives.compiler.processors.blocks;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.hierarchy.BlockType;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Set;

/**
 * 表达式块处理器 - 处理 expression_block, function_call_block, array_block
 */
@Slf4j
public class ExpressionBlockProcessor extends BlockProcessor {


    @Override
    public boolean canProcess(CodeBlock block) {
        return block.getBlockType() == BlockType.EXPRESSION_BLOCK ||
               block.getBlockType() == BlockType.FUNCTION_CALL_BLOCK ||
               block.getBlockType() == BlockType.ARRAY_BLOCK;
    }

    @Override
    protected ASTNode doProcess(CodeBlock block, List<Token> tokens) {
        log.debug("处理表达式块: {}", block);

        ASTNode.NodeType nodeType = switch (block.getBlockType()) {
            case EXPRESSION_BLOCK -> ASTNode.NodeType.EXPRESSION;
            case FUNCTION_CALL_BLOCK -> ASTNode.NodeType.FUNCTION_CALL;
            case ARRAY_BLOCK -> ASTNode.NodeType.ARRAY_LITERAL;
            default -> ASTNode.NodeType.EXPRESSION;
        };

        ASTNode node = new ASTNode(nodeType);
        node.setLine(block.getStartLine());
        return node;
    }

    @Override
    public Set<BlockType> getSupportedBlockTypes() {
        return Set.of(
            BlockType.EXPRESSION_BLOCK,
            BlockType.FUNCTION_CALL_BLOCK,
            BlockType.ARRAY_BLOCK
        );
    }
}


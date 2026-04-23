// ==================== ControlFlowBlockProcessor.java ====================
package com.q3lives.compiler.processors.blocks;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.hierarchy.BlockType;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Set;

/**
 * 控制流块处理器 - 处理 control_flow_block, condition_block, loop_body_block
 */
@Slf4j
public class ControlFlowBlockProcessor extends BlockProcessor {


    @Override
    public boolean canProcess(CodeBlock block) {
        return block.getBlockType() == BlockType.CONTROL_FLOW_BLOCK ||
               block.getBlockType() == BlockType.CONDITION_BLOCK ||
               block.getBlockType() == BlockType.LOOP_BODY_BLOCK;
    }

    @Override
    protected ASTNode doProcess(CodeBlock block, List<Token> tokens) {
        log.debug("处理控制流块: {}", block);

        ASTNode.NodeType nodeType = switch (block.getBlockType()) {
            case CONTROL_FLOW_BLOCK -> ASTNode.NodeType.IF_STATEMENT;
            case CONDITION_BLOCK -> ASTNode.NodeType.CONDITION;
            case LOOP_BODY_BLOCK -> ASTNode.NodeType.FOR_STATEMENT;
            default -> ASTNode.NodeType.BLOCK;
        };

        ASTNode node = new ASTNode(nodeType);
        node.setLine(block.getStartLine());

        for (CodeBlock child : block.getChildren()) {
            ASTNode childNode = process(child, tokens);
            if (childNode != null) {
                node.addChild(childNode);
            }
        }

        return node;
    }

    @Override
    public Set<BlockType> getSupportedBlockTypes() {
        return Set.of(
            BlockType.CONTROL_FLOW_BLOCK,
            BlockType.CONDITION_BLOCK,
            BlockType.LOOP_BODY_BLOCK
        );
    }
}


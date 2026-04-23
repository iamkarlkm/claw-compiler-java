// ==================== AssignmentBlockProcessor.java ====================
package com.q3lives.compiler.processors.blocks;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.hierarchy.BlockType;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Set;

/**
 * 赋值块处理器 - 处理 assignment_block
 */
@Slf4j
public class AssignmentBlockProcessor extends BlockProcessor {


    @Override
    public boolean canProcess(CodeBlock block) {
        return block.getBlockType() == BlockType.ASSIGNMENT_BLOCK;
    }

    @Override
    public ASTNode process(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
        node.setLine(block.getStartLine());
        return node;
    }

    @Override
    public Set<BlockType> getSupportedBlockTypes() {
        return Set.of(BlockType.ASSIGNMENT_BLOCK);
    }

    @Override
    protected ASTNode doProcess(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
        node.setLine(block.getStartLine());
        return node;
    }
}


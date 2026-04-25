// ==================== ScopeBlockProcessor.java ====================
package com.q3lives.compiler.processors.blocks;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.hierarchy.BlockType;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Set;

/**
 * 范围块处理器 - 处理 scope_block, type_inner_block
 */
@Slf4j
public class ScopeBlockProcessor extends BlockProcessor {

    @Override
    public boolean canProcess(CodeBlock block) {
        return block.getBlockType() == BlockType.SCOPE_BLOCK ||
               block.getBlockType() == BlockType.TYPE_INNER_BLOCK;
    }

    @Override
    protected ASTNode doProcess(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.BLOCK);
        node.setLine(block.getStartLine());
        // 递归处理当前处理器支持的子块类型
        processChildren(node, block.getChildren(), tokens);
        return node;
    }

    @Override
    public Set<BlockType> getSupportedBlockTypes() {
        return Set.of(BlockType.SCOPE_BLOCK, BlockType.TYPE_INNER_BLOCK);
    }
}


// ==================== ModuleBlockProcessor.java ====================
package com.q3lives.compiler.processors.blocks;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.hierarchy.BlockType;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Set;

/**
 * 模块块处理器 - 处理 module_block
 */
@Slf4j
public class ModuleBlockProcessor extends BlockProcessor {


    @Override
    public boolean canProcess(CodeBlock block) {
        return block.getBlockType() == BlockType.MODULE_BLOCK;
    }

    @Override
    public ASTNode process(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.MODULE);
        node.setLine(block.getStartLine());
        return node;
    }

    @Override
    public Set<BlockType> getSupportedBlockTypes() {
        return Set.of(BlockType.MODULE_BLOCK);
    }

    @Override
    protected ASTNode doProcess(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.MODULE);
        node.setLine(block.getStartLine());
        return node;
    }
}


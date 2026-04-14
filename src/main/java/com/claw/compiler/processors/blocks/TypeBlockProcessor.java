// ==================== TypeBlockProcessor.java ====================
package com.claw.compiler.processors.blocks;

import com.claw.compiler.core.Token;
import com.claw.compiler.core.TokenType;
import com.claw.compiler.hierarchy.BlockType;
import com.claw.compiler.hierarchy.CodeBlock;
import com.claw.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Set;

/**
 * 类型块处理器 - 处理 type_definition_block
 */
@Slf4j
public class TypeBlockProcessor extends BlockProcessor {

    @Override
    public boolean canProcess(CodeBlock block) {
        return block.getBlockType() == BlockType.TYPE_DEFINITION_BLOCK;
    }

    @Override
    public ASTNode process(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.TYPE_DEFINITION);
        node.setLine(block.getStartLine());

        List<Token> lineTokens = tokens.stream()
                .filter(t -> t.getLine() == block.getStartLine())
                .toList();
        for (int i = 0; i < lineTokens.size(); i++) {
            if (lineTokens.get(i).getType() == TokenType.KW_TYPE && i + 1 < lineTokens.size()) {
                node.setAttribute("name", lineTokens.get(i + 1).getValue());
            }
        }

        return node;
    }

    @Override
    public Set<BlockType> getSupportedBlockTypes() {
        return Set.of(BlockType.TYPE_DEFINITION_BLOCK);
    }

    @Override
    protected ASTNode doProcess(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.TYPE_DEFINITION);
        node.setLine(block.getStartLine());

        List<Token> lineTokens = tokens.stream()
                .filter(t -> t.getLine() == block.getStartLine())
                .toList();
        for (int i = 0; i < lineTokens.size(); i++) {
            if (lineTokens.get(i).getType() == TokenType.KW_TYPE && i + 1 < lineTokens.size()) {
                node.setAttribute("name", lineTokens.get(i + 1).getValue());
            }
        }

        return node;
    }
}


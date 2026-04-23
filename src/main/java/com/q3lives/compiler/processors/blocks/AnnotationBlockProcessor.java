// ==================== AnnotationBlockProcessor.java ====================
package com.q3lives.compiler.processors.blocks;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.TokenType;
import com.q3lives.compiler.hierarchy.BlockType;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Set;

/**
 * 注解块处理器 - 处理 annotation_block
 */
@Slf4j
public class AnnotationBlockProcessor extends BlockProcessor {


    @Override
    public boolean canProcess(CodeBlock block) {
        return block.getBlockType() == BlockType.ANNOTATION_BLOCK;
    }

    @Override
    protected ASTNode doProcess(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.ANNOTATION);
        node.setLine(block.getStartLine());

        List<Token> lineTokens = tokens.stream()
                .filter(t -> t.getLine() == block.getStartLine())
                .toList();

        if (!lineTokens.isEmpty()) {
            Token first = lineTokens.get(0);
            boolean isSystem = first.getType() == TokenType.DOUBLE_AT_SIGN;
            node.setAttribute("annotationCategory", isSystem ? "system" : "program");

            // 提取注解名称
            for (Token t : lineTokens) {
                if (t.getType() == TokenType.IDENTIFIER) {
                    node.setAttribute("annotationName", t.getValue());
                    break;
                }
            }
        }

        return node;
    }

    @Override
    public Set<BlockType> getSupportedBlockTypes() {
        return Set.of(BlockType.ANNOTATION_BLOCK);
    }
}


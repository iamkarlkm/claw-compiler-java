// ==================== FunctionBlockProcessor.java ====================
package com.claw.compiler.processors.blocks;

import com.claw.compiler.common.TokenUtils;
import com.claw.compiler.core.Token;
import com.claw.compiler.core.TokenType;
import com.claw.compiler.hierarchy.BlockType;
import com.claw.compiler.hierarchy.CodeBlock;
import com.claw.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

/**
 * 函数块处理器
 *
 * <p>处理以下类型的代码块：</p>
 * <ul>
 *   <li>{@link BlockType#FUNCTION_BLOCK} - 函数定义块</li>
 *   <li>{@link BlockType#PARAMETER_BLOCK} - 参数块</li>
 *   <li>{@link BlockType#RETURN_BLOCK} - 返回值块</li>
 * </ul>
 *
 * <h2>提取的函数属性</h2>
 * <ul>
 *   <li>name - 函数名称</li>
 *   <li>flowType - 操作流类型 (normal/exception/flow)</li>
 *   <li>visibility - 可见性 (public/private)</li>
 * </ul>
 *
 * @author Claw Compiler Team
 * @since 3.0.0
 * @see BlockProcessor
 * @see TokenUtils
 */
@Slf4j
public class FunctionBlockProcessor extends BlockProcessor {

    private static final Set<BlockType> SUPPORTED_TYPES = Set.of(
        BlockType.FUNCTION_BLOCK,
        BlockType.PARAMETER_BLOCK,
        BlockType.RETURN_BLOCK
    );

    @Override
    public boolean canProcess(CodeBlock block) {
        return block != null && SUPPORTED_TYPES.contains(block.getBlockType());
    }

    @Override
    public Set<BlockType> getSupportedBlockTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    protected ASTNode doProcess(CodeBlock block, List<Token> tokens) {
        log.debug("处理函数块: {} (行 {}-{})", block.getBlockType(),
            block.getStartLine(), block.getEndLine());

        return switch (block.getBlockType()) {
            case FUNCTION_BLOCK -> processFunctionBlock(block, tokens);
            case PARAMETER_BLOCK -> processParameterBlock(block, tokens);
            case RETURN_BLOCK -> processReturnBlock(block, tokens);
            default -> null;
        };
    }

    /**
     * 处理函数定义块
     */
    private ASTNode processFunctionBlock(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.FUNCTION_DECLARATION);
        node.setLine(block.getStartLine());

        // 使用工具类获取范围内的 Token
        List<Token> blockTokens = TokenUtils.getTokensInRange(
            tokens, block.getStartLine(), block.getEndLine()
        );

        // 提取函数信息
        extractFunctionMetadata(node, blockTokens);

        // 处理子块
        processChildren(node, block.getChildren(), tokens);

        return node;
    }

    /**
     * 从 Token 序列中提取函数元数据
     *
     * @param node 目标 AST 节点
     * @param blockTokens 块内的 Token 序列
     */
    private void extractFunctionMetadata(ASTNode node, List<Token> blockTokens) {
        for (int i = 0; i < blockTokens.size(); i++) {
            Token t = blockTokens.get(i);

            // 提取函数名：function 后的标识符
            if (t.getType() == TokenType.KW_FUNCTION) {
                Token nameToken = TokenUtils.safeGetNext(blockTokens, i);
                if (nameToken != null && nameToken.getType() == TokenType.IDENTIFIER) {
                    node.setAttribute("name", nameToken.getValue());
                }
            }

            // 提取操作流类型
            if (t.getType() == TokenType.KW_NORMAL) {
                node.setAttribute("flowType", "normal");
            } else if (t.getType() == TokenType.KW_EXCEPTION) {
                node.setAttribute("flowType", "exception");
            } else if (t.getType() == TokenType.KW_FLOW) {
                node.setAttribute("flowType", "flow");
            }

            // 提取可见性
            if (t.getType() == TokenType.KW_PUBLIC) {
                node.setAttribute("visibility", "public");
            } else if (t.getType() == TokenType.KW_PRIVATE) {
                node.setAttribute("visibility", "private");
            }
        }

        // 设置默认可见性
        if (node.getAttribute("visibility") == null) {
            node.setAttribute("visibility", "private");
        }

        // 设置默认操作流类型
        if (node.getAttribute("flowType") == null) {
            node.setAttribute("flowType", "normal");
        }
    }

    /**
     * 处理参数块
     */
    private ASTNode processParameterBlock(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.PARAMETER_LIST);
        node.setLine(block.getStartLine());

        // 使用工具类获取行内 Token
        List<Token> lineTokens = TokenUtils.getTokensOnLine(tokens, block.getStartLine());
        extractParameters(node, lineTokens);

        return node;
    }

    /**
     * 从 Token 序列中提取参数信息
     */
    private void extractParameters(ASTNode node, List<Token> lineTokens) {
        // 查找括号内的参数
        int parenDepth = 0;
        StringBuilder paramBuilder = new StringBuilder();

        for (Token t : lineTokens) {
            if (t.getType() == TokenType.OPEN_PAREN) {
                parenDepth++;
            } else if (t.getType() == TokenType.CLOSE_PAREN) {
                parenDepth--;
            } else if (parenDepth > 0) {
                paramBuilder.append(t.getValue());
            }
        }

        String params = paramBuilder.toString().trim();
        if (!params.isEmpty()) {
            node.setAttribute("params", params);
        }
    }

    /**
     * 处理返回块
     */
    private ASTNode processReturnBlock(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.RETURN_STATEMENT);
        node.setLine(block.getStartLine());

        // 使用工具类获取行内 Token
        List<Token> lineTokens = TokenUtils.getTokensOnLine(tokens, block.getStartLine());
        extractReturnValue(node, lineTokens);

        return node;
    }

    /**
     * 从 Token 序列中提取返回值
     */
    private void extractReturnValue(ASTNode node, List<Token> lineTokens) {
        Token returnKeyword = TokenUtils.findFirst(lineTokens, TokenType.KW_RETURN);
        if (returnKeyword != null) {
            // 提取 return 后的表达式
            String expr = TokenUtils.extractValueAfter(lineTokens, TokenType.KW_RETURN);
            if (!expr.isEmpty()) {
                node.setAttribute("value", expr);
            }
        }
    }
}

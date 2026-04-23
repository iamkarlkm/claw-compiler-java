// ==================== DeclarationBlockProcessor.java ====================
package com.q3lives.compiler.processors.blocks;

import com.q3lives.compiler.common.TokenUtils;
import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.TokenType;
import com.q3lives.compiler.hierarchy.BlockType;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

/**
 * 声明块处理器
 *
 * <p>处理以下类型的代码块：</p>
 * <ul>
 *   <li>{@link BlockType#VARIABLE_DECLARATION_BLOCK} - 变量声明块</li>
 *   <li>{@link BlockType#IMPORT_DECLARATION_BLOCK} - 导入声明块</li>
 * </ul>
 *
 * <h2>提取的声明属性</h2>
 * <ul>
 *   <li>name - 变量/模块名称</li>
 *   <li>type - 类型声明</li>
 *   <li>value - 初始值</li>
 *   <li>isConst - 是否为常量</li>
 *   <li>path - 导入路径</li>
 *   <li>symbols - 导入的符号列表</li>
 * </ul>
 *
 * @author Claw Compiler Team
 * @since 3.0.0
 * @see BlockProcessor
 * @see TokenUtils
 */
@Slf4j
public class DeclarationBlockProcessor extends BlockProcessor {


    private static final Set<BlockType> SUPPORTED_TYPES = Set.of(
        BlockType.VARIABLE_DECLARATION_BLOCK,
        BlockType.IMPORT_DECLARATION_BLOCK
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
        log.debug("处理声明块: {} (行 {})", block.getBlockType(), block.getStartLine());

        return switch (block.getBlockType()) {
            case IMPORT_DECLARATION_BLOCK -> processImportBlock(block, tokens);
            case VARIABLE_DECLARATION_BLOCK -> processVariableBlock(block, tokens);
            default -> null;
        };
    }

    /**
     * 处理导入声明块
     */
    private ASTNode processImportBlock(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.IMPORT_DECLARATION);
        node.setLine(block.getStartLine());

        // 使用工具类获取行内 Token
        List<Token> lineTokens = TokenUtils.getTokensOnLine(tokens, block.getStartLine());

        // 提取导入路径和符号
        extractImportInfo(node, lineTokens);

        return node;
    }

    /**
     * 从 Token 序列中提取导入信息
     *
     * @param node 目标 AST 节点
     * @param lineTokens 行内 Token 序列
     */
    private void extractImportInfo(ASTNode node, List<Token> lineTokens) {
        // 提取导入路径（import 后的内容）
        String importContent = TokenUtils.extractValueAfter(lineTokens, TokenType.KW_IMPORT);

        if (importContent.isEmpty()) {
            log.warn("导入语句缺少路径");
            return;
        }

        // 解析导入格式：module { symbol1, symbol2 } 或 module
        int braceIndex = importContent.indexOf('{');
        if (braceIndex >= 0) {
            // 带符号列表的导入
            String modulePath = importContent.substring(0, braceIndex).trim();
            String symbolsPart = importContent.substring(braceIndex + 1);
            int closeBrace = symbolsPart.indexOf('}');
            if (closeBrace >= 0) {
                symbolsPart = symbolsPart.substring(0, closeBrace).trim();
            }

            node.setAttribute("path", modulePath);
            node.setAttribute("symbols", symbolsPart);
        } else {
            // 整体导入
            node.setAttribute("path", importContent);
        }
    }

    /**
     * 处理变量声明块
     */
    private ASTNode processVariableBlock(CodeBlock block, List<Token> tokens) {
        ASTNode node = new ASTNode(ASTNode.NodeType.VARIABLE_DECLARATION);
        node.setLine(block.getStartLine());

        // 使用工具类获取行内 Token
        List<Token> lineTokens = TokenUtils.getTokensOnLine(tokens, block.getStartLine());

        // 提取变量信息
        extractVariableInfo(node, lineTokens);

        return node;
    }

    /**
     * 从 Token 序列中提取变量信息
     *
     * @param node 目标 AST 节点
     * @param lineTokens 行内 Token 序列
     */
    private void extractVariableInfo(ASTNode node, List<Token> lineTokens) {
        boolean isConst = false;
        boolean foundDeclaration = false;

        for (int i = 0; i < lineTokens.size(); i++) {
            Token t = lineTokens.get(i);

            // 检查声明关键字
            if (t.getType() == TokenType.KW_CONST) {
                isConst = true;
                foundDeclaration = true;
            } else if (t.getType() == TokenType.KW_VAR) {
                isConst = false;
                foundDeclaration = true;
            }

            // 提取变量名：声明关键字后的标识符
            if (foundDeclaration && t.getType() == TokenType.IDENTIFIER) {
                node.setAttribute("name", t.getValue());
                foundDeclaration = false; // 避免重复提取
            }

            // 提取类型注解：冒号后的类型
            if (t.getType() == TokenType.OP_COLON && i + 1 < lineTokens.size()) {
                Token typeToken = TokenUtils.safeGetNext(lineTokens, i);
                if (typeToken != null) {
                    node.setAttribute("type", typeToken.getValue());
                }
            }

            // 提取初始值：等号后的表达式
            if (t.getType() == TokenType.OP_ASSIGN && i + 1 < lineTokens.size()) {
                String value = extractValueExpression(lineTokens, i + 1);
                node.setAttribute("value", value);
            }
        }

        node.setAttribute("isConst", String.valueOf(isConst));

        // 验证必要属性
        if (node.getAttribute("name") == null) {
            log.warn("变量声明缺少名称");
        }
    }

    /**
     * 提取赋值表达式
     *
     * @param tokens Token 序列
     * @param startIndex 起始索引
     * @return 表达式字符串
     */
    private String extractValueExpression(List<Token> tokens, int startIndex) {
        StringBuilder value = new StringBuilder();
        for (int i = startIndex; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            // 遇到行结束符停止
            if (t.getType() == TokenType.NEWLINE || t.getType() == TokenType.EOF) {
                break;
            }
            // 跳过空白
            if (t.getType() == TokenType.WHITESPACE) {
                value.append(' ');
            } else {
                value.append(t.getValue());
            }
        }
        return value.toString().trim();
    }
}

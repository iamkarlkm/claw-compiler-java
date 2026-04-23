// ==================== Parser.java ====================
package com.q3lives.compiler.frontend;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.TokenType;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语法分析器 - 将Token流解析为AST
 *
 * 包含缓存机制以提高重复解析的性能
 */
@Slf4j
public class Parser {

    // 解析缓存，避免重复解析相同的token序列
    private static final Map<String, ASTNode> parseCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    private List<Token> tokens;
    private int pos;

    public ASTNode parse(List<Token> tokens) {
        // 使用缓存来提高重复解析的性能
        String cacheKey = generateCacheKey(tokens);
        ASTNode cached = getCachedParse(cacheKey);
        if (cached != null) {
            log.debug("使用缓存的解析结果: {} 个Token", tokens.size());
            return cached.deepCopy();
        }

        this.tokens = tokens;
        this.pos = 0;

        log.debug("开始语法分析: {} 个Token", tokens.size());

        ASTNode program = new ASTNode(ASTNode.NodeType.PROGRAM);

        while (!isAtEnd()) {
            ASTNode decl = parseTopLevelDeclaration();
            if (decl != null) {
                program.addChild(decl);
            }
        }

        List<ASTNode> children = new ArrayList<>();
        for (ASTNode child : program.getChildren()) {
            children.add(child);
        }
        log.info("语法分析完成: {} 个顶层声明", children.size());

        // 缓存解析结果
        cacheParse(cacheKey, program);
        return program;
    }

    /**
     * 生成缓存的key
     */
    private String generateCacheKey(List<Token> tokens) {
        if (tokens.size() > 100) { // 只对较小的token序列使用缓存
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(tokens.size(), 50); i++) {
            sb.append(tokens.get(i).getType()).append(":").append(tokens.get(i).getValue()).append(",");
        }
        return sb.toString();
    }

    /**
     * 从缓存获取解析结果
     */
    private ASTNode getCachedParse(String cacheKey) {
        if (cacheKey == null || parseCache.isEmpty()) {
            return null;
        }
        return parseCache.get(cacheKey);
    }

    /**
     * 缓存解析结果
     */
    private void cacheParse(String cacheKey, ASTNode program) {
        if (cacheKey != null && parseCache.size() < MAX_CACHE_SIZE) {
            parseCache.put(cacheKey, program);
        }

        // 简单的LRU缓存清理
        if (parseCache.size() >= MAX_CACHE_SIZE) {
            parseCache.clear();
        }
    }

    private ASTNode parseTopLevelDeclaration() {
        Token current = peek();
        if (current == null) return null;

        return switch (current.getType()) {
            case DOUBLE_AT_SIGN, AT_SIGN -> parseAnnotation();
            case KW_IMPORT -> parseImport();
            case KW_FUNCTION, KW_PUBLIC, KW_PRIVATE, KW_NORMAL, KW_EXCEPTION, KW_FLOW -> parseFunction();
            case KW_TYPE -> parseTypeDefinition();
            case KW_VAR, KW_CONST -> parseVariableDeclaration();
            case KW_EXPORT -> { advance(); yield parseTopLevelDeclaration(); }
            default -> { advance(); yield null; } // 跳过无法识别的Token
        };
    }

    private ASTNode parseAnnotation() {
        ASTNode node = new ASTNode(ASTNode.NodeType.ANNOTATION);
        Token atSign = advance();
        node.setLine(atSign.getLine());

        boolean isSystem = atSign.getType() == TokenType.DOUBLE_AT_SIGN;
        node.setAttribute("category", isSystem ? "system" : "program");

        if (!isAtEnd() && peek().getType() == TokenType.IDENTIFIER) {
            node.setAttribute("name", advance().getValue());
        }

        // 解析参数列表
        if (!isAtEnd() && peek().getType() == TokenType.OPEN_PAREN) {
            advance(); // (
            StringBuilder args = new StringBuilder();
            int depth = 1;
            while (!isAtEnd() && depth > 0) {
                Token t = advance();
                if (t.getType() == TokenType.OPEN_PAREN) depth++;
                else if (t.getType() == TokenType.CLOSE_PAREN) { depth--; if (depth == 0) break; }
                args.append(t.getValue()).append(" ");
            }
            node.setAttribute("arguments", args.toString().trim());
        }

        return node;
    }

    private ASTNode parseImport() {
        ASTNode node = new ASTNode(ASTNode.NodeType.IMPORT_DECLARATION);
        Token importToken = advance(); // import
        node.setLine(importToken.getLine());

        StringBuilder path = new StringBuilder();
        while (!isAtEnd() && peek().getLine() == importToken.getLine()) {
            path.append(advance().getValue());
        }
        node.setAttribute("path", path.toString().trim());
        return node;
    }

    private ASTNode parseFunction() {
        ASTNode node = new ASTNode(ASTNode.NodeType.FUNCTION_DECLARATION);

        // 修饰符
        while (!isAtEnd()) {
            Token t = peek();
            if (t.getType() == TokenType.KW_NORMAL) { node.setAttribute("flowType", "normal"); advance(); }
            else if (t.getType() == TokenType.KW_EXCEPTION) { node.setAttribute("flowType", "exception"); advance(); }
            else if (t.getType() == TokenType.KW_FLOW) { node.setAttribute("flowType", "flow"); advance(); }
            else if (t.getType() == TokenType.KW_PUBLIC) { node.setAttribute("visibility", "public"); advance(); }
            else if (t.getType() == TokenType.KW_PRIVATE) { node.setAttribute("visibility", "private"); advance(); }
            else break;
        }

        // function 关键字
        if (!isAtEnd() && peek().getType() == TokenType.KW_FUNCTION) {
            Token funcToken = advance();
            node.setLine(funcToken.getLine());
        }

        // 函数名
        if (!isAtEnd() && peek().getType() == TokenType.IDENTIFIER) {
            node.setAttribute("name", advance().getValue());
        }

        // 参数列表
        if (!isAtEnd() && peek().getType() == TokenType.OPEN_PAREN) {
            ASTNode params = parseParameterList();
            node.addChild(params);
        }

        // 返回类型
        if (!isAtEnd() && peek().getType() == TokenType.OP_ARROW) {
            advance(); // ->
            if (!isAtEnd()) {
                node.setAttribute("returnType", advance().getValue());
            }
        }

        // 函数体
        if (!isAtEnd() && peek().getType() == TokenType.OPEN_BRACE) {
            ASTNode body = parseBlock();
            node.addChild(body);
        }

        return node;
    }

    private ASTNode parseParameterList() {
        ASTNode params = new ASTNode(ASTNode.NodeType.PARAMETER_LIST);
        advance(); // (

        while (!isAtEnd() && peek().getType() != TokenType.CLOSE_PAREN) {
            if (peek().getType() == TokenType.IDENTIFIER) {
                ASTNode param = new ASTNode(ASTNode.NodeType.PARAMETER);
                param.setAttribute("name", advance().getValue());
                param.setLine(peek() != null ? peek().getLine() : 0);

                if (!isAtEnd() && peek().getType() == TokenType.OP_COLON) {
                    advance(); // :
                    if (!isAtEnd()) {
                        param.setAttribute("type", advance().getValue());
                    }
                }
                params.addChild(param);
            }
            if (!isAtEnd() && peek().getType() == TokenType.OP_COMMA) {
                advance();
            }
        }

        if (!isAtEnd()) advance(); // )
        return params;
    }

    private ASTNode parseTypeDefinition() {
        ASTNode node = new ASTNode(ASTNode.NodeType.TYPE_DEFINITION);
        Token typeToken = advance(); // type
        node.setLine(typeToken.getLine());

        if (!isAtEnd() && peek().getType() == TokenType.IDENTIFIER) {
            node.setAttribute("name", advance().getValue());
        }

        if (!isAtEnd() && peek().getType() == TokenType.OPEN_BRACE) {
            ASTNode body = parseBlock();
            node.addChild(body);
        }

        return node;
    }

    private ASTNode parseVariableDeclaration() {
        ASTNode node = new ASTNode(ASTNode.NodeType.VARIABLE_DECLARATION);
        Token keyword = advance(); // var or const
        node.setLine(keyword.getLine());
        node.setAttribute("mutable", keyword.getType() == TokenType.KW_VAR);

        if (!isAtEnd() && peek().getType() == TokenType.IDENTIFIER) {
            node.setAttribute("name", advance().getValue());
        }

        if (!isAtEnd() && peek().getType() == TokenType.OP_COLON) {
            advance();
            if (!isAtEnd()) node.setAttribute("type", advance().getValue());
        }

        if (!isAtEnd() && peek().getType() == TokenType.OP_ASSIGN) {
            advance();
            ASTNode value = parseExpression();
            if (value != null) node.addChild(value);
        }

        return node;
    }

    private ASTNode parseBlock() {
        ASTNode block = new ASTNode(ASTNode.NodeType.BLOCK);
        if (!isAtEnd() && peek().getType() == TokenType.OPEN_BRACE) {
            block.setLine(peek().getLine());
            advance(); // {
        }

        int depth = 1;
        while (!isAtEnd() && depth > 0) {
            Token t = peek();
            if (t.getType() == TokenType.OPEN_BRACE) depth++;
            if (t.getType() == TokenType.CLOSE_BRACE) {
                depth--;
                if (depth == 0) { advance(); break; }
            }

            ASTNode stmt = parseStatement();
            if (stmt != null) block.addChild(stmt);
        }

        return block;
    }

    private ASTNode parseStatement() {
        if (isAtEnd()) return null;
        Token t = peek();

        return switch (t.getType()) {
            case KW_VAR, KW_CONST -> parseVariableDeclaration();
            case KW_RETURN -> parseReturnStatement();
            case KW_IF -> parseIfStatement();
            case KW_FOR -> parseForStatement();
            case KW_WHILE -> parseWhileStatement();
            case KW_BREAK -> { advance(); yield new ASTNode(ASTNode.NodeType.BREAK_STATEMENT); }
            case KW_CONTINUE -> { advance(); yield new ASTNode(ASTNode.NodeType.CONTINUE_STATEMENT); }
            case KW_THROW -> parseThrowStatement();
            case KW_CATCH -> parseCatchClause();
            case KW_FLOW -> parseFlowStatement();
            default -> parseExpressionStatement();
        };
    }

    private ASTNode parseReturnStatement() {
        ASTNode node = new ASTNode(ASTNode.NodeType.RETURN_STATEMENT);
        Token ret = advance();
        node.setLine(ret.getLine());
        ASTNode expr = parseExpression();
        if (expr != null) node.addChild(expr);
        return node;
    }

    private ASTNode parseIfStatement() {
        ASTNode node = new ASTNode(ASTNode.NodeType.IF_STATEMENT);
        Token ifToken = advance();
        node.setLine(ifToken.getLine());

        // 条件
        if (!isAtEnd() && peek().getType() == TokenType.OPEN_PAREN) {
            ASTNode condition = new ASTNode(ASTNode.NodeType.CONDITION);
            advance(); // (
            int depth = 1;
            while (!isAtEnd() && depth > 0) {
                if (peek().getType() == TokenType.OPEN_PAREN) depth++;
                if (peek().getType() == TokenType.CLOSE_PAREN) { depth--; if (depth == 0) { advance(); break; } }
                advance();
            }
            node.addChild(condition);
        }

        // then块
        if (!isAtEnd() && peek().getType() == TokenType.OPEN_BRACE) {
            node.addChild(parseBlock());
        }

        // else块
        if (!isAtEnd() && peek().getType() == TokenType.KW_ELSE) {
            advance();
            ASTNode elseNode = new ASTNode(ASTNode.NodeType.ELSE_CLAUSE);
            if (!isAtEnd() && peek().getType() == TokenType.KW_IF) {
                elseNode.addChild(parseIfStatement());
            } else if (!isAtEnd() && peek().getType() == TokenType.OPEN_BRACE) {
                elseNode.addChild(parseBlock());
            }
            node.addChild(elseNode);
        }

        return node;
    }

    private ASTNode parseForStatement() {
        ASTNode node = new ASTNode(ASTNode.NodeType.FOR_STATEMENT);
        Token forToken = advance();
        node.setLine(forToken.getLine());
        // 跳过条件部分
        skipParenthesized();
        if (!isAtEnd() && peek().getType() == TokenType.OPEN_BRACE) {
            node.addChild(parseBlock());
        }
        return node;
    }

    private ASTNode parseWhileStatement() {
        ASTNode node = new ASTNode(ASTNode.NodeType.WHILE_STATEMENT);
        Token whileToken = advance();
        node.setLine(whileToken.getLine());
        skipParenthesized();
        if (!isAtEnd() && peek().getType() == TokenType.OPEN_BRACE) {
            node.addChild(parseBlock());
        }
        return node;
    }

    private ASTNode parseThrowStatement() {
        ASTNode node = new ASTNode(ASTNode.NodeType.THROW_STATEMENT);
        Token throwToken = advance();
        node.setLine(throwToken.getLine());
        ASTNode expr = parseExpression();
        if (expr != null) node.addChild(expr);
        return node;
    }

    private ASTNode parseCatchClause() {
        ASTNode node = new ASTNode(ASTNode.NodeType.CATCH_CLAUSE);
        Token catchToken = advance();
        node.setLine(catchToken.getLine());
        skipParenthesized();
        if (!isAtEnd() && peek().getType() == TokenType.OPEN_BRACE) {
            node.addChild(parseBlock());
        }
        return node;
    }

    private ASTNode parseFlowStatement() {
        ASTNode node = new ASTNode(ASTNode.NodeType.FLOW_STATEMENT);
        Token flowToken = advance();
        node.setLine(flowToken.getLine());

        // flow to target
        if (!isAtEnd() && "to".equals(peek().getValue())) {
            advance(); // to
            if (!isAtEnd()) {
                node.setAttribute("target", advance().getValue());
            }
        }
        return node;
    }

    private ASTNode parseExpressionStatement() {
        ASTNode expr = parseExpression();
        if (expr != null) {
            ASTNode stmt = new ASTNode(ASTNode.NodeType.EXPRESSION_STATEMENT);
            stmt.setLine(expr.getLine());
            stmt.addChild(expr);
            return stmt;
        }
        advance(); // 跳过无法解析的Token
        return null;
    }

    private ASTNode parseExpression() {
        if (isAtEnd()) return null;
        Token t = peek();

        // 简化的表达式解析
        if (t.getType() == TokenType.IDENTIFIER) {
            ASTNode node = new ASTNode(ASTNode.NodeType.IDENTIFIER_REF);
            node.setAttribute("name", advance().getValue());
            node.setLine(t.getLine());

            // 函数调用
            if (!isAtEnd() && peek().getType() == TokenType.OPEN_PAREN) {
                ASTNode call = new ASTNode(ASTNode.NodeType.FUNCTION_CALL);
                call.setAttribute("name", node.getAttribute("name"));
                call.setLine(t.getLine());
                skipParenthesized();
                return call;
            }

            // 成员访问
            if (!isAtEnd() && peek().getType() == TokenType.OP_DOT) {
                ASTNode access = new ASTNode(ASTNode.NodeType.MEMBER_ACCESS);
                access.setLine(t.getLine());
                access.addChild(node);
                advance(); // .
                ASTNode member = parseExpression();
                if (member != null) access.addChild(member);
                return access;
            }

            // 赋值
            if (!isAtEnd() && peek().getType() == TokenType.OP_ASSIGN) {
                ASTNode assign = new ASTNode(ASTNode.NodeType.ASSIGNMENT);
                assign.setLine(t.getLine());
                assign.addChild(node);
                advance(); // =
                ASTNode value = parseExpression();
                if (value != null) assign.addChild(value);
                return assign;
            }

            return node;
        }

        if (t.isLiteral()) {
            ASTNode.NodeType litType = switch (t.getType()) {
                case LIT_INTEGER -> ASTNode.NodeType.INTEGER_LITERAL;
                case LIT_FLOAT -> ASTNode.NodeType.FLOAT_LITERAL;
                case LIT_STRING -> ASTNode.NodeType.STRING_LITERAL;
                case KW_TRUE, KW_FALSE -> ASTNode.NodeType.BOOLEAN_LITERAL;
                case KW_NULL -> ASTNode.NodeType.NULL_LITERAL;
                default -> ASTNode.NodeType.EXPRESSION;
            };
            ASTNode node = new ASTNode(litType);
            node.setAttribute("value", advance().getValue());
            node.setLine(t.getLine());
            return node;
        }

        return null;
    }

    private void skipParenthesized() {
        if (isAtEnd() || peek().getType() != TokenType.OPEN_PAREN) return;
        advance();
        int depth = 1;
        while (!isAtEnd() && depth > 0) {
            Token t = advance();
            if (t.getType() == TokenType.OPEN_PAREN) depth++;
            if (t.getType() == TokenType.CLOSE_PAREN) depth--;
        }
    }

    // ===== 辅助方法 =====
    private Token peek() {
        return pos < tokens.size() ? tokens.get(pos) : null;
    }

    private Token advance() {
        Token t = tokens.get(pos);
        pos++;
        return t;
    }

    private boolean isAtEnd() {
        return pos >= tokens.size() || tokens.get(pos).getType() == TokenType.EOF;
    }
}

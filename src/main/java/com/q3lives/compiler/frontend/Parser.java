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
            case KW_ASPECT -> parseAspect();
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

        // 解析: import [type] module [as alias]
        // 例: import std.io
        //     import type { foo, bar } from "module"
        //     import foo as f

        // 可选类型限定: import type, import class, import function
        if (!isAtEnd() && (peek().getType() == TokenType.KW_TYPE ||
                          peek().getValue().equals("type") ||
                          peek().getValue().equals("class") ||
                          peek().getValue().equals("function"))) {
            node.setAttribute("importType", advance().getValue());
        }

        // 模块路径
        StringBuilder path = new StringBuilder();
        while (!isAtEnd() && peek().getLine() == importToken.getLine()) {
            String val = peek().getValue();
            if ("from".equals(val) || "as".equals(val)) break;
            path.append(advance().getValue());
            if (peek().getType() == TokenType.OP_DOT) {
                path.append(advance().getValue()); // .
            }
        }
        node.setAttribute("path", path.toString().trim());

        // as 别名
        if (!isAtEnd() && "as".equals(peek().getValue())) {
            advance(); // as
            node.setAttribute("alias", advance().getValue());
        }

        return node;
    }

    private ASTNode parseFunction() {
        ASTNode node = new ASTNode(ASTNode.NodeType.FUNCTION_DECLARATION);

        // 修饰符 - flowType 默认 normal（可省略）
        while (!isAtEnd()) {
            Token t = peek();
            if (t.getType() == TokenType.KW_NORMAL) { node.setAttribute("flowType", "normal"); advance(); }
            else if (t.getType() == TokenType.KW_EXCEPTION) { node.setAttribute("flowType", "exception"); advance(); }
            else if (t.getType() == TokenType.KW_FLOW) { node.setAttribute("flowType", "flow"); advance(); }
            else if (t.getType() == TokenType.KW_PUBLIC) { node.setAttribute("visibility", "public"); advance(); }
            else if (t.getType() == TokenType.KW_PRIVATE) { node.setAttribute("visibility", "private"); advance(); }
            else break;
        }

        // 默认 flowType 为 normal（无需写 normal 关键字）
        if (node.getAttribute("flowType") == null) {
            node.setAttribute("flowType", "normal");
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

        // 解析泛型类型参数 <T, U>
        parseTypeParameters(node);

        // 参数列表
        if (!isAtEnd() && peek().getType() == TokenType.OPEN_PAREN) {
            ASTNode params = parseParameterList();
            node.addChild(params);
        }

        // 返回类型
        if (!isAtEnd() && peek().getType() == TokenType.OP_ARROW) {
            advance(); // ->
            String typeName = parseTypeName();
            if (typeName != null) {
                node.setAttribute("returnType", typeName);
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
                    String typeName = parseTypeName();
                    if (typeName != null) {
                        param.setAttribute("type", typeName);
                    }
                }
                params.addChild(param);
            }
            if (!isAtEnd() && peek().getType() == TokenType.OP_COMMA) {
                advance();
            } else if (!isAtEnd() && peek().getType() != TokenType.CLOSE_PAREN && peek().getType() != TokenType.IDENTIFIER) {
                // 跳过无法识别的 token（如函数类型中的括号），避免死循环
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

        // 解析类型参数 <T, U>
        parseTypeParameters(node);

        if (!isAtEnd() && peek().getType() == TokenType.OPEN_BRACE) {
            ASTNode body = parseBlock();
            node.addChild(body);
        }

        return node;
    }

    private ASTNode parseAspect() {
        ASTNode node = new ASTNode(ASTNode.NodeType.AOP_ASPECT);
        Token aspectToken = advance(); // aspect
        node.setLine(aspectToken.getLine());

        if (!isAtEnd() && peek().getType() == TokenType.IDENTIFIER) {
            node.setAttribute("name", advance().getValue());
        }

        // aspect 体
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
            String typeName = parseTypeName();
            if (typeName != null) {
                node.setAttribute("type", typeName);
            }
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
            case KW_FUNCTION -> parseFunction();
            case KW_RETURN -> parseReturnStatement();
            case KW_IF -> parseIfStatement();
            case KW_FOR -> parseForStatement();
            case KW_WHILE -> parseWhileStatement();
            case KW_MATCH -> parseMatchExpression();
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

    /** 解析 match 表达式：match x { case A -> ... case B -> ... } */
    private ASTNode parseMatchExpression() {
        ASTNode node = new ASTNode(ASTNode.NodeType.MATCH_EXPRESSION);
        Token matchToken = advance();
        node.setLine(matchToken.getLine());

        // 解析匹配目标
        if (!isAtEnd() && peek().getType() != TokenType.OPEN_BRACE) {
            node.addChild(parseExpression());
        }

        // 解析 case 块
        if (!isAtEnd() && peek().getType() == TokenType.OPEN_BRACE) {
            advance(); // {
            while (!isAtEnd() && peek().getType() != TokenType.CLOSE_BRACE) {
                if (peek().getType() == TokenType.KW_CASE) {
                    ASTNode caseNode = new ASTNode(ASTNode.NodeType.PATTERN_MATCH);
                    advance(); // case
                    // 模式
                    caseNode.addChild(parseExpression());
                    // -> 值
                    if (!isAtEnd() && peek().getType() == TokenType.OP_ARROW) {
                        advance(); // ->
                        caseNode.addChild(parseExpression());
                    }
                    node.addChild(caseNode);
                } else if (peek().getType() == TokenType.KW_DEFAULT) {
                    ASTNode defaultNode = new ASTNode(ASTNode.NodeType.PATTERN_MATCH);
                    advance(); // default
                    if (!isAtEnd() && peek().getType() == TokenType.OP_ARROW) {
                        advance(); // ->
                        defaultNode.addChild(parseExpression());
                    }
                    node.addChild(defaultNode);
                } else {
                    advance(); // 跳过
                }
            }
            if (!isAtEnd()) advance(); // }
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

            // 可选链：?.
            if (!isAtEnd() && peek().getType() == TokenType.OP_QUESTION_DOT) {
                ASTNode access = new ASTNode(ASTNode.NodeType.OPTIONAL_CHAIN);
                access.setLine(t.getLine());
                access.addChild(node);
                advance(); // ?.
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

        // Lambda 表达式: (x) -> x + 1 或 x -> x + 1
        if (t.getType() == TokenType.OPEN_PAREN || t.getType() == TokenType.IDENTIFIER) {
            ASTNode lambda = tryParseLambda();
            if (lambda != null) return lambda;
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

    /**
     * 尝试解析 Lambda 表达式
     * 语法:
     * 1. (x, y) -> x + y    // 多参数
     * 2. x: x + 1           // 单参数冒号语法
     */
    private ASTNode tryParseLambda() {
        // 保存当前位置，如果解析失败可以回退
        int savedPos = pos;
        Token first = peek();

        // 情况1: (params) -> body
        if (first.getType() == TokenType.OPEN_PAREN) {
            ASTNode params = new ASTNode(ASTNode.NodeType.PARAMETER_LIST);
            advance(); // (

            // 解析参数列表
            while (!isAtEnd() && peek().getType() != TokenType.CLOSE_PAREN) {
                if (peek().getType() == TokenType.IDENTIFIER) {
                    ASTNode param = new ASTNode(ASTNode.NodeType.PARAMETER);
                    param.setAttribute("name", advance().getValue());
                    params.addChild(param);
                }
                if (peek().getType() == TokenType.OP_COMMA) {
                    advance(); // ,
                }
            }
            if (!isAtEnd()) advance(); // )

            // ->
            if (!isAtEnd() && "->".equals(peek().getValue())) {
                advance(); // ->
                ASTNode body = parseExpression();
                if (body != null) {
                    ASTNode lambda = new ASTNode(ASTNode.NodeType.LAMBDA_EXPRESSION);
                    lambda.addChild(params);
                    lambda.addChild(body);
                    return lambda;
                }
            }
        }

        // 情况2: x: body (单参数冒号语法)
        pos = savedPos;
        if (first.getType() == TokenType.IDENTIFIER) {
            String paramName = first.getValue();
            advance(); // x

            // 检查冒号
            if (!isAtEnd() && peek().getType() == TokenType.OP_COLON) {
                advance(); // :
                ASTNode body = parseExpression();
                if (body != null) {
                    ASTNode params = new ASTNode(ASTNode.NodeType.PARAMETER_LIST);
                    ASTNode param = new ASTNode(ASTNode.NodeType.PARAMETER);
                    param.setAttribute("name", paramName);
                    params.addChild(param);

                    ASTNode lambda = new ASTNode(ASTNode.NodeType.LAMBDA_EXPRESSION);
                    lambda.addChild(params);
                    lambda.addChild(body);
                    return lambda;
                }
            }
        }

        // 回退
        pos = savedPos;
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

    /**
     * 解析类型名称，支持泛型语法（如 Array<Int>、Map<String, Box<T>>）。
     * 返回完整的类型名字符串，供类型检查和代码生成使用。
     */
    private String parseTypeName() {
        if (isAtEnd()) return null;
        Token t = peek();
        if (t.getType() != TokenType.IDENTIFIER && !isTypeKeyword(t.getType())) {
            return null;
        }

        StringBuilder sb = new StringBuilder(advance().getValue());

        // 检查是否有泛型参数 <...\u003e
        if (!isAtEnd() && peek().getType() == TokenType.OP_LESS) {
            sb.append("<");
            advance(); // <
            int depth = 1;

            while (!isAtEnd() && depth > 0) {
                Token inner = peek();
                if (inner.getType() == TokenType.OP_LESS) {
                    depth++;
                    sb.append(advance().getValue());
                } else if (inner.getType() == TokenType.OP_GREATER) {
                    depth--;
                    sb.append(advance().getValue());
                    if (depth == 0) break;
                } else if (inner.getType() == TokenType.OP_COMMA) {
                    sb.append(advance().getValue());
                    sb.append(' ');
                } else if (inner.getType() == TokenType.IDENTIFIER || isTypeKeyword(inner.getType())) {
                    sb.append(advance().getValue());
                } else {
                    // 其他 token 也直接附加
                    sb.append(advance().getValue());
                }
            }
        }

        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * 解析可选的泛型类型参数列表 <T, U>，将结果写入节点的 typeParams 属性。
     * 支持类型边界：<T: Comparable, U: Number>
     */
    private void parseTypeParameters(ASTNode node) {
        if (isAtEnd() || peek().getType() != TokenType.OP_LESS) {
            return;
        }
        advance(); // <
        StringBuilder sb = new StringBuilder();
        StringBuilder bounds = new StringBuilder();
        boolean inBounds = false;

        while (!isAtEnd() && peek().getType() != TokenType.OP_GREATER) {
            TokenType tt = peek().getType();
            if (tt == TokenType.IDENTIFIER) {
                if (sb.length() > 0) {
                    sb.append(",");
                    if (inBounds) bounds.append(",");
                }
                sb.append(advance().getValue());
            } else if (tt == TokenType.OP_COLON) {
                advance(); // :
                inBounds = true;
                // 解析边界类型
                while (!isAtEnd() && peek().getType() != TokenType.OP_COMMA
                        && peek().getType() != TokenType.OP_GREATER) {
                    if (peek().getType() == TokenType.IDENTIFIER) {
                        if (bounds.length() > 0) bounds.append("&");
                        bounds.append(advance().getValue());
                    } else {
                        advance(); // 跳过
                    }
                }
            } else if (tt == TokenType.OP_COMMA) {
                advance(); // ,
                inBounds = false;
            } else {
                advance(); // 跳过未知 token
            }
        }
        if (!isAtEnd()) advance(); // >
        if (sb.length() > 0) {
            node.setAttribute("typeParams", sb.toString());
            if (bounds.length() > 0) {
                node.setAttribute("typeBounds", bounds.toString());
            }
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

    /**
     * 判断是否为内置类型关键字（Int、Float、String、Bool、Void、Any）。
     * 这些关键字在词法分析中被识别为特定 token，但在类型上下文中应视为类型名称。
     */
    private boolean isTypeKeyword(TokenType type) {
        return type == TokenType.KW_INT || type == TokenType.KW_FLOAT ||
               type == TokenType.KW_STRING || type == TokenType.KW_BOOL ||
               type == TokenType.KW_VOID || type == TokenType.KW_ANY;
    }
}

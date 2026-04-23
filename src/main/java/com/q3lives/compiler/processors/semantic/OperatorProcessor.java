// ==================== OperatorProcessor.java ====================
package com.q3lives.compiler.processors.semantic;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.TokenType;
import com.q3lives.compiler.hierarchy.HierarchicalBlocks;

import com.q3lives.compiler.context.SemanticContext;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 运算符处理器 - 处理所有运算符
 */
@Slf4j
public class OperatorProcessor {


    /** 运算符优先级（数字越大优先级越高） */
    private static final Map<TokenType, Integer> PRECEDENCE = new LinkedHashMap<>();
    static {
        PRECEDENCE.put(TokenType.OP_OR, 1);
        PRECEDENCE.put(TokenType.OP_AND, 2);
        PRECEDENCE.put(TokenType.OP_EQUAL, 3);
        PRECEDENCE.put(TokenType.OP_NOT_EQUAL, 3);
        PRECEDENCE.put(TokenType.OP_LESS, 4);
        PRECEDENCE.put(TokenType.OP_LESS_EQUAL, 4);
        PRECEDENCE.put(TokenType.OP_GREATER, 4);
        PRECEDENCE.put(TokenType.OP_GREATER_EQUAL, 4);
        PRECEDENCE.put(TokenType.OP_PLUS, 5);
        PRECEDENCE.put(TokenType.OP_MINUS, 5);
        PRECEDENCE.put(TokenType.OP_STAR, 6);
        PRECEDENCE.put(TokenType.OP_SLASH, 6);
        PRECEDENCE.put(TokenType.OP_PERCENT, 6);
        PRECEDENCE.put(TokenType.OP_NOT, 7);
        PRECEDENCE.put(TokenType.OP_DOT, 8);
    }

    /** 获取运算符优先级 */
    public int getPrecedence(TokenType type) {
        return PRECEDENCE.getOrDefault(type, 0);
    }

    /** 是否为二元运算符 */
    public boolean isBinaryOperator(TokenType type) {
        return PRECEDENCE.containsKey(type) && type != TokenType.OP_NOT;
    }

    /** 是否为一元运算符 */
    public boolean isUnaryOperator(TokenType type) {
        return type == TokenType.OP_NOT || type == TokenType.OP_MINUS;
    }

    /** 是否为赋值运算符 */
    public boolean isAssignmentOperator(TokenType type) {
        return type == TokenType.OP_ASSIGN || 
               type == TokenType.OP_PLUS_ASSIGN || 
               type == TokenType.OP_MINUS_ASSIGN;
    }

    /** 是否为比较运算符 */
    public boolean isComparisonOperator(TokenType type) {
        return type == TokenType.OP_EQUAL || type == TokenType.OP_NOT_EQUAL ||
               type == TokenType.OP_LESS || type == TokenType.OP_LESS_EQUAL ||
               type == TokenType.OP_GREATER || type == TokenType.OP_GREATER_EQUAL;
    }

    public Object process(List<Token> tokens, HierarchicalBlocks blocks, SemanticContext ctx) {
        for (Token token : tokens) {
            if (!PRECEDENCE.containsKey(token.getType())) {
                continue;
            }
            String symbol = mapTokenTypeToOperator(token.getType());
            if (symbol == null) {
                continue;
            }
            SemanticContext.OperatorInfo.OperatorCategory category = resolveOperatorCategory(token.getType());
            int precedence = getPrecedence(token.getType());
            boolean unary = isUnaryOperator(token.getType());
            SemanticContext.TypeInfo resultType = inferResultType(token.getType());
            ctx.registerOperator(new SemanticContext.OperatorInfo(symbol, category, precedence, unary, resultType));
        }
        return ctx;
    }

    private String mapTokenTypeToOperator(TokenType type) {
        return switch (type) {
            case OP_PLUS -> "+";
            case OP_MINUS -> "-";
            case OP_STAR -> "*";
            case OP_SLASH -> "/";
            case OP_PERCENT -> "%";
            case OP_EQUAL -> "==";
            case OP_NOT_EQUAL -> "!=";
            case OP_LESS -> "<";
            case OP_LESS_EQUAL -> "<=";
            case OP_GREATER -> ">";
            case OP_GREATER_EQUAL -> ">=";
            case OP_AND -> "&&";
            case OP_OR -> "||";
            case OP_NOT -> "!";
            case OP_PLUS_ASSIGN -> "+=";
            case OP_MINUS_ASSIGN -> "-=";
            case OP_ASSIGN -> "=";
            case OP_DOT -> ".";
            default -> null;
        };
    }

    private SemanticContext.OperatorInfo.OperatorCategory resolveOperatorCategory(TokenType type) {
        return switch (type) {
            case OP_PLUS, OP_MINUS, OP_STAR, OP_SLASH, OP_PERCENT -> SemanticContext.OperatorInfo.OperatorCategory.ARITHMETIC;
            case OP_EQUAL, OP_NOT_EQUAL, OP_LESS, OP_LESS_EQUAL, OP_GREATER, OP_GREATER_EQUAL -> SemanticContext.OperatorInfo.OperatorCategory.COMPARISON;
            case OP_AND, OP_OR, OP_NOT -> SemanticContext.OperatorInfo.OperatorCategory.LOGICAL;
            case OP_ASSIGN, OP_PLUS_ASSIGN, OP_MINUS_ASSIGN -> SemanticContext.OperatorInfo.OperatorCategory.ASSIGNMENT;
            default -> SemanticContext.OperatorInfo.OperatorCategory.ARITHMETIC;
        };
    }

    private SemanticContext.TypeInfo inferResultType(TokenType type) {
        return switch (type) {
            case OP_EQUAL, OP_NOT_EQUAL, OP_LESS, OP_LESS_EQUAL, OP_GREATER, OP_GREATER_EQUAL,
                 OP_AND, OP_OR, OP_NOT -> new SemanticContext.TypeInfo("Bool", SemanticContext.TypeInfo.TypeKind.PRIMITIVE);
            case OP_PLUS, OP_MINUS, OP_STAR, OP_SLASH, OP_PERCENT,
                 OP_ASSIGN, OP_PLUS_ASSIGN, OP_MINUS_ASSIGN -> new SemanticContext.TypeInfo("Int", SemanticContext.TypeInfo.TypeKind.PRIMITIVE);
            default -> new SemanticContext.TypeInfo("Any", SemanticContext.TypeInfo.TypeKind.PRIMITIVE);
        };
    }
}


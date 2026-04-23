// ==================== LiteralProcessor.java ====================
package com.q3lives.compiler.processors.semantic;

import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.hierarchy.HierarchicalBlocks;

import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 字面量处理器 - 处理 true, false, null, 数字, 字符串
 */
@Slf4j
public class LiteralProcessor {


    /**
     * 解析字面量Token为实际值
     */
    public Object parseLiteral(Token token) {
        return switch (token.getType()) {
            case LIT_INTEGER -> parseLong(token.getValue());
            case LIT_FLOAT -> parseDouble(token.getValue());
            case LIT_STRING -> parseString(token.getValue());
            case KW_TRUE -> Boolean.TRUE;
            case KW_FALSE -> Boolean.FALSE;
            case KW_NULL -> null;
            default -> throw new IllegalArgumentException("不是字面量Token: " + token);
        };
    }

    /** 是否为字面量Token */
    public boolean isLiteral(Token token) {
        return token.isLiteral();
    }

    /** 推断字面量类型 */
    public String inferType(Token token) {
        return switch (token.getType()) {
            case LIT_INTEGER -> "Int";
            case LIT_FLOAT -> "Float";
            case LIT_STRING -> "String";
            case KW_TRUE, KW_FALSE -> "Bool";
            case KW_NULL -> "Void";
            default -> "Any";
        };
    }

    private long parseLong(String value) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { return 0; }
    }

    private double parseDouble(String value) {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private String parseString(String value) {
        if (value.length() >= 2) {
            return value.substring(1, value.length() - 1)
                    .replace("\\", "");//TODO
                    //.replace("\	", "	")
                    // .replace("\\"", "\"")
                    // .replace("\'", "'")
                    // .replace("\\", "\");
        }
        return value;
    }

    public Object process(List<Token> tokens, HierarchicalBlocks blocks, SemanticContext ctx) {
        for (Token token : tokens) {
            if (!isLiteral(token)) {
                continue;
            }
            Object parsedValue = parseLiteral(token);
            SemanticContext.LiteralInfo.LiteralType literalType;
            switch (token.getType()) {
                case LIT_INTEGER -> literalType = SemanticContext.LiteralInfo.LiteralType.INT;
                case LIT_FLOAT -> literalType = SemanticContext.LiteralInfo.LiteralType.FLOAT;
                case LIT_STRING -> literalType = SemanticContext.LiteralInfo.LiteralType.STRING;
                case KW_TRUE, KW_FALSE -> literalType = SemanticContext.LiteralInfo.LiteralType.BOOL;
                case KW_NULL -> literalType = SemanticContext.LiteralInfo.LiteralType.NULL;
                default -> literalType = SemanticContext.LiteralInfo.LiteralType.NULL;
            }
            ctx.addLiteral(new SemanticContext.LiteralInfo(literalType, token.getValue(), parsedValue, token.getLine()));
        }
        return ctx;
    }
}


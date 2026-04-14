// ==================== Token.java ====================
package com.claw.compiler.core;

import lombok.Getter;
import lombok.ToString;

/**
 * Token - 词法单元
 */
@Getter
@ToString
public class Token {
    private final TokenType type;
    private final String value;
    private final int line;
    private final int column;
    private final int startOffset;

    public Token(TokenType type, String value, int line, int column, int startOffset) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
        this.startOffset = startOffset;
    }

    /** 是否为关键字 */
    public boolean isKeyword() {
        return type.name().startsWith("KW_");
    }

    /** 是否为运算符 */
    public boolean isOperator() {
        return type.name().startsWith("OP_");
    }

    /** 是否为字面量 */
    public boolean isLiteral() {
        return type.name().startsWith("LIT_") || 
               type == TokenType.KW_TRUE || 
               type == TokenType.KW_FALSE || 
               type == TokenType.KW_NULL;
    }

    public TokenType getType() {
        return type;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getValue() {
        return value;
    }
}


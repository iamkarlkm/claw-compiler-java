// ==================== TokenType.java ====================
package com.q3lives.compiler.core;

/**
 * Token类型
 */
public enum TokenType {
    // ====== 关键字 ======
    // 类型关键字
    KW_INT, KW_FLOAT, KW_STRING, KW_BOOL, KW_VOID, KW_ANY, KW_TYPE,
    // 函数关键字
    KW_FUNCTION, KW_PUBLIC, KW_PRIVATE, KW_RETURN,
    // 控制流关键字
    KW_IF, KW_ELSE, KW_FOR, KW_WHILE, KW_BREAK, KW_CONTINUE,
    // 声明关键字
    KW_IMPORT, KW_EXPORT, KW_CONST, KW_VAR,
    // 字面量关键字
    KW_TRUE, KW_FALSE, KW_NULL,
    // 操作流关键字
    KW_NORMAL, KW_EXCEPTION, KW_FLOW,
    // 异常处理关键字
    KW_CATCH, KW_THROWS, KW_THROW,

    // ====== 字面量 ======
    LIT_INTEGER, LIT_FLOAT, LIT_STRING, LIT_CHAR,

    // ====== 标识符 ======
    IDENTIFIER,

    // ====== 运算符 ======
    OP_PLUS, OP_MINUS, OP_STAR, OP_SLASH, OP_PERCENT,     // + - * / %
    OP_ASSIGN, OP_EQUAL, OP_NOT_EQUAL,                     // = == !=
    OP_LESS, OP_LESS_EQUAL, OP_GREATER, OP_GREATER_EQUAL,  // < <= > >=
    OP_AND, OP_OR, OP_NOT,                                 // && || !
    OP_PLUS_ASSIGN, OP_MINUS_ASSIGN,                       // += -=
    OP_ARROW,                                              // ->
    OP_DOT,                                                // .
    OP_COLON,                                              // :
    OP_COMMA,                                              // ,
    OP_SEMICOLON,                                          // ;

    // ====== 括号 ======
    OPEN_BRACE, CLOSE_BRACE,     // { }
    OPEN_PAREN, CLOSE_PAREN,     // ( )
    OPEN_BRACKET, CLOSE_BRACKET, // [ ]

    // ====== 注解 ======
    AT_SIGN,         // @
    DOUBLE_AT_SIGN,  // @@

    // ====== 特殊 ======
    NEWLINE, WHITESPACE, EOF, UNKNOWN
}


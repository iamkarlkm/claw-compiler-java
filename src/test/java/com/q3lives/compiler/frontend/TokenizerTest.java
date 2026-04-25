package com.q3lives.compiler.frontend;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.TokenType;
import com.q3lives.compiler.core.Tokenizer;
import com.q3lives.compiler.scanner.SourceScanner;
import com.q3lives.compiler.scanner.SourceView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分词器单元测试
 *
 * 验证 Tokenizer 对各种源代码的正确分词行为。
 */
class TokenizerTest {

    private final SourceScanner scanner = new SourceScanner();
    private final Tokenizer tokenizer = new Tokenizer();

    /**
     * 从源代码字符串获取 Token 列表（辅助方法）
     */
    private List<Token> tokenize(String source) {
        SourceView view = scanner.scan(source, "test.claw");
        return tokenizer.tokenize(view);
    }

    /**
     * 断言指定位置的 Token 类型和值
     */
    private void assertToken(List<Token> tokens, int index, TokenType expectedType, String expectedValue) {
        assertTrue(index < tokens.size(),
            "Token 索引 " + index + " 超出范围，列表大小为 " + tokens.size());
        Token token = tokens.get(index);
        assertEquals(expectedType, token.getType(),
            "索引 " + index + " 处的 Token 类型不匹配");
        assertEquals(expectedValue, token.getValue(),
            "索引 " + index + " 处的 Token 值不匹配");
    }

    @Test
    void testEmptySource() {
        List<Token> tokens = tokenize("");
        assertEquals(1, tokens.size(), "空源应该只产生 EOF");
        assertEquals(TokenType.EOF, tokens.get(0).getType());
    }

    @Test
    void testBasicKeywords() {
        List<Token> tokens = tokenize("function var if else return");
        assertToken(tokens, 0, TokenType.KW_FUNCTION, "function");
        assertToken(tokens, 1, TokenType.KW_VAR, "var");
        assertToken(tokens, 2, TokenType.KW_IF, "if");
        assertToken(tokens, 3, TokenType.KW_ELSE, "else");
        assertToken(tokens, 4, TokenType.KW_RETURN, "return");
        assertEquals(TokenType.EOF, tokens.get(5).getType());
    }

    @Test
    void testTypeKeywords() {
        List<Token> tokens = tokenize("Int Float String Bool Void Any type");
        assertToken(tokens, 0, TokenType.KW_INT, "Int");
        assertToken(tokens, 1, TokenType.KW_FLOAT, "Float");
        assertToken(tokens, 2, TokenType.KW_STRING, "String");
        assertToken(tokens, 3, TokenType.KW_BOOL, "Bool");
        assertToken(tokens, 4, TokenType.KW_VOID, "Void");
        assertToken(tokens, 5, TokenType.KW_ANY, "Any");
        assertToken(tokens, 6, TokenType.KW_TYPE, "type");
    }

    @Test
    void testLiteralValues() {
        List<Token> tokens = tokenize("42 3.14 \"hello\" true false null");
        assertToken(tokens, 0, TokenType.LIT_INTEGER, "42");
        assertToken(tokens, 1, TokenType.LIT_FLOAT, "3.14");
        assertToken(tokens, 2, TokenType.LIT_STRING, "\"hello\"");
        assertToken(tokens, 3, TokenType.KW_TRUE, "true");
        assertToken(tokens, 4, TokenType.KW_FALSE, "false");
        assertToken(tokens, 5, TokenType.KW_NULL, "null");
    }

    @Test
    void testIdentifierRecognition() {
        List<Token> tokens = tokenize("myVariable _privateVar count2");
        assertToken(tokens, 0, TokenType.IDENTIFIER, "myVariable");
        assertToken(tokens, 1, TokenType.IDENTIFIER, "_privateVar");
        assertToken(tokens, 2, TokenType.IDENTIFIER, "count2");
    }

    @Test
    void testOperators() {
        List<Token> tokens = tokenize("+ - * / % = == != < <= > >= && || ! -> += -=");
        assertToken(tokens, 0, TokenType.OP_PLUS, "+");
        assertToken(tokens, 1, TokenType.OP_MINUS, "-");
        assertToken(tokens, 2, TokenType.OP_STAR, "*");
        assertToken(tokens, 3, TokenType.OP_SLASH, "/");
        assertToken(tokens, 4, TokenType.OP_PERCENT, "%");
        assertToken(tokens, 5, TokenType.OP_ASSIGN, "=");
        assertToken(tokens, 6, TokenType.OP_EQUAL, "==");
        assertToken(tokens, 7, TokenType.OP_NOT_EQUAL, "!=");
        assertToken(tokens, 8, TokenType.OP_LESS, "<");
        assertToken(tokens, 9, TokenType.OP_LESS_EQUAL, "<=");
        assertToken(tokens, 10, TokenType.OP_GREATER, ">");
        assertToken(tokens, 11, TokenType.OP_GREATER_EQUAL, ">=");
        assertToken(tokens, 12, TokenType.OP_AND, "&&");
        assertToken(tokens, 13, TokenType.OP_OR, "||");
        assertToken(tokens, 14, TokenType.OP_NOT, "!");
        assertToken(tokens, 15, TokenType.OP_ARROW, "->");
        assertToken(tokens, 16, TokenType.OP_PLUS_ASSIGN, "+=");
        assertToken(tokens, 17, TokenType.OP_MINUS_ASSIGN, "-=");
    }

    @Test
    void testBracketsAndPunctuation() {
        List<Token> tokens = tokenize("{ } ( ) [ ] . : , ;");
        assertToken(tokens, 0, TokenType.OPEN_BRACE, "{");
        assertToken(tokens, 1, TokenType.CLOSE_BRACE, "}");
        assertToken(tokens, 2, TokenType.OPEN_PAREN, "(");
        assertToken(tokens, 3, TokenType.CLOSE_PAREN, ")");
        assertToken(tokens, 4, TokenType.OPEN_BRACKET, "[");
        assertToken(tokens, 5, TokenType.CLOSE_BRACKET, "]");
        assertToken(tokens, 6, TokenType.OP_DOT, ".");
        assertToken(tokens, 7, TokenType.OP_COLON, ":");
        assertToken(tokens, 8, TokenType.OP_COMMA, ",");
        assertToken(tokens, 9, TokenType.OP_SEMICOLON, ";");
    }

    @Test
    void testAnnotations() {
        List<Token> tokens = tokenize("@BeforeProps @@description");
        assertToken(tokens, 0, TokenType.AT_SIGN, "@");
        assertToken(tokens, 1, TokenType.IDENTIFIER, "BeforeProps");
        assertToken(tokens, 2, TokenType.DOUBLE_AT_SIGN, "@@");
        assertToken(tokens, 3, TokenType.IDENTIFIER, "description");
    }

    @Test
    void testStringWithEscape() {
        List<Token> tokens = tokenize("\"hello\\nworld\"");
        // Tokenizer 将 \\n 转义为真正的换行符
        assertToken(tokens, 0, TokenType.LIT_STRING, "\"hello\nworld\"");
    }

    @Test
    void testUnclosedString() {
        List<Token> tokens = tokenize("\"unclosed");
        assertToken(tokens, 0, TokenType.UNKNOWN, "未闭合的字符串");
    }

    @Test
    void testComplexExpression() {
        List<Token> tokens = tokenize("var result = a + b * 2;");
        assertToken(tokens, 0, TokenType.KW_VAR, "var");
        assertToken(tokens, 1, TokenType.IDENTIFIER, "result");
        assertToken(tokens, 2, TokenType.OP_ASSIGN, "=");
        assertToken(tokens, 3, TokenType.IDENTIFIER, "a");
        assertToken(tokens, 4, TokenType.OP_PLUS, "+");
        assertToken(tokens, 5, TokenType.IDENTIFIER, "b");
        assertToken(tokens, 6, TokenType.OP_STAR, "*");
        assertToken(tokens, 7, TokenType.LIT_INTEGER, "2");
        assertToken(tokens, 8, TokenType.OP_SEMICOLON, ";");
    }

    @Test
    void testFlowKeywords() {
        List<Token> tokens = tokenize("normal exception flow catch throws throw");
        assertToken(tokens, 0, TokenType.KW_NORMAL, "normal");
        assertToken(tokens, 1, TokenType.KW_EXCEPTION, "exception");
        assertToken(tokens, 2, TokenType.KW_FLOW, "flow");
        assertToken(tokens, 3, TokenType.KW_CATCH, "catch");
        assertToken(tokens, 4, TokenType.KW_THROWS, "throws");
        assertToken(tokens, 5, TokenType.KW_THROW, "throw");
    }
}

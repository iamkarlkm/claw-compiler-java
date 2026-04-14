// ==================== Tokenizer.java ====================
package com.claw.compiler.core;

import com.claw.compiler.scanner.LineInfo;
import com.claw.compiler.scanner.SourceView;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 分词器
 * 
 * 第1层基础处理器：将源代码分割为Token流
 */
@Slf4j
public class Tokenizer {

    /** 关键字映射 */
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        // 类型关键字
        KEYWORDS.put("Int", TokenType.KW_INT);
        KEYWORDS.put("Float", TokenType.KW_FLOAT);
        KEYWORDS.put("String", TokenType.KW_STRING);
        KEYWORDS.put("Bool", TokenType.KW_BOOL);
        KEYWORDS.put("Void", TokenType.KW_VOID);
        KEYWORDS.put("Any", TokenType.KW_ANY);
        KEYWORDS.put("type", TokenType.KW_TYPE);
        // 函数关键字
        KEYWORDS.put("function", TokenType.KW_FUNCTION);
        KEYWORDS.put("public", TokenType.KW_PUBLIC);
        KEYWORDS.put("private", TokenType.KW_PRIVATE);
        KEYWORDS.put("return", TokenType.KW_RETURN);
        // 控制流关键字
        KEYWORDS.put("if", TokenType.KW_IF);
        KEYWORDS.put("else", TokenType.KW_ELSE);
        KEYWORDS.put("for", TokenType.KW_FOR);
        KEYWORDS.put("while", TokenType.KW_WHILE);
        KEYWORDS.put("break", TokenType.KW_BREAK);
        KEYWORDS.put("continue", TokenType.KW_CONTINUE);
        // 声明关键字
        KEYWORDS.put("import", TokenType.KW_IMPORT);
        KEYWORDS.put("export", TokenType.KW_EXPORT);
        KEYWORDS.put("const", TokenType.KW_CONST);
        KEYWORDS.put("var", TokenType.KW_VAR);
        // 字面量关键字
        KEYWORDS.put("true", TokenType.KW_TRUE);
        KEYWORDS.put("false", TokenType.KW_FALSE);
        KEYWORDS.put("null", TokenType.KW_NULL);
        // 操作流关键字
        KEYWORDS.put("normal", TokenType.KW_NORMAL);
        KEYWORDS.put("exception", TokenType.KW_EXCEPTION);
        KEYWORDS.put("flow", TokenType.KW_FLOW);
        // 异常处理关键字
        KEYWORDS.put("catch", TokenType.KW_CATCH);
        KEYWORDS.put("throws", TokenType.KW_THROWS);
        KEYWORDS.put("throw", TokenType.KW_THROW);
    }

    /**
     * 对整个源代码视图进行分词
     */
    public List<Token> tokenize(SourceView sourceView) {
        log.debug("开始分词: {}", sourceView.getFileName());
        List<Token> tokens = new ArrayList<>();

        for (LineInfo line : sourceView.getEffectiveLines()) {
            List<Token> lineTokens = tokenizeLine(line);
            tokens.addAll(lineTokens);
        }

        tokens.add(new Token(TokenType.EOF, "", 
                sourceView.getTotalLines(), 0, 
                sourceView.getRawSource().length()));

        log.info("分词完成: {} 个Token", tokens.size());
        return tokens;
    }

    /**
     * 对单行进行分词
     */
    private List<Token> tokenizeLine(LineInfo lineInfo) {
        List<Token> tokens = new ArrayList<>();
        String content = lineInfo.getCleanContent();
        int line = lineInfo.getLineNumber();
        int i = 0;

        while (i < content.length()) {
            char c = content.charAt(i);

            // 跳过空白
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // 注解符号 @@ 或 @
            if (c == '@') {
                if (i + 1 < content.length() && content.charAt(i + 1) == '@') {
                    tokens.add(new Token(TokenType.DOUBLE_AT_SIGN, "@@", line, i, 
                                        lineInfo.getStartOffset() + i));
                    i += 2;
                } else {
                    tokens.add(new Token(TokenType.AT_SIGN, "@", line, i, 
                                        lineInfo.getStartOffset() + i));
                    i++;
                }
                continue;
            }

            // 字符串
            if (c == '"' || c == '\'') {
                int start = i;
                char quote = c;
                i++;
                StringBuilder sb = new StringBuilder();
                sb.append(quote);
                while (i < content.length()) {
                    char ch = content.charAt(i);
                    sb.append(ch);
                    if (ch == '\\' && i + 1 < content.length()) {
                        i++;
                        sb.append(content.charAt(i));
                    } else if (ch == quote) {
                        break;
                    }
                    i++;
                }
                i++;
                tokens.add(new Token(TokenType.LIT_STRING, sb.toString(), line, start, 
                                    lineInfo.getStartOffset() + start));
                continue;
            }

            // 数字
            if (Character.isDigit(c)) {
                int start = i;
                boolean isFloat = false;
                while (i < content.length() && 
                       (Character.isDigit(content.charAt(i)) || content.charAt(i) == '.')) {
                    if (content.charAt(i) == '.') isFloat = true;
                    i++;
                }
                String numStr = content.substring(start, i);
                TokenType type = isFloat ? TokenType.LIT_FLOAT : TokenType.LIT_INTEGER;
                tokens.add(new Token(type, numStr, line, start, 
                                    lineInfo.getStartOffset() + start));
                continue;
            }

            // 标识符/关键字
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < content.length() && 
                       (Character.isLetterOrDigit(content.charAt(i)) || content.charAt(i) == '_')) {
                    i++;
                }
                String word = content.substring(start, i);
                TokenType type = KEYWORDS.getOrDefault(word, TokenType.IDENTIFIER);
                tokens.add(new Token(type, word, line, start, 
                                    lineInfo.getStartOffset() + start));
                continue;
            }

            // 多字符运算符
            char next = (i + 1 < content.length()) ? content.charAt(i + 1) : 0;
            TokenType opType = matchOperator(c, next);
            if (opType != null) {
                int len = isDoubleCharOp(c, next) ? 2 : 1;
                String opStr = content.substring(i, i + len);
                tokens.add(new Token(opType, opStr, line, i, 
                                    lineInfo.getStartOffset() + i));
                i += len;
                continue;
            }

            // 未知字符
            tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(c), line, i, 
                                lineInfo.getStartOffset() + i));
            i++;
        }

        return tokens;
    }

    private TokenType matchOperator(char c, char next) {
        // 双字符运算符
        if (c == '-' && next == '>') return TokenType.OP_ARROW;
        if (c == '=' && next == '=') return TokenType.OP_EQUAL;
        if (c == '!' && next == '=') return TokenType.OP_NOT_EQUAL;
        if (c == '<' && next == '=') return TokenType.OP_LESS_EQUAL;
        if (c == '>' && next == '=') return TokenType.OP_GREATER_EQUAL;
        if (c == '&' && next == '&') return TokenType.OP_AND;
        if (c == '|' && next == '|') return TokenType.OP_OR;
        if (c == '+' && next == '=') return TokenType.OP_PLUS_ASSIGN;
        if (c == '-' && next == '=') return TokenType.OP_MINUS_ASSIGN;

        // 单字符运算符
        return switch (c) {
            case '+' -> TokenType.OP_PLUS;
            case '-' -> TokenType.OP_MINUS;
            case '*' -> TokenType.OP_STAR;
            case '/' -> TokenType.OP_SLASH;
            case '%' -> TokenType.OP_PERCENT;
            case '=' -> TokenType.OP_ASSIGN;
            case '<' -> TokenType.OP_LESS;
            case '>' -> TokenType.OP_GREATER;
            case '!' -> TokenType.OP_NOT;
            case '.' -> TokenType.OP_DOT;
            case ':' -> TokenType.OP_COLON;
            case ',' -> TokenType.OP_COMMA;
            case ';' -> TokenType.OP_SEMICOLON;
            case '{' -> TokenType.OPEN_BRACE;
            case '}' -> TokenType.CLOSE_BRACE;
            case '(' -> TokenType.OPEN_PAREN;
            case ')' -> TokenType.CLOSE_PAREN;
            case '[' -> TokenType.OPEN_BRACKET;
            case ']' -> TokenType.CLOSE_BRACKET;
            default -> null;
        };
    }

    private boolean isDoubleCharOp(char c, char next) {
        return (c == '-' && next == '>') || (c == '=' && next == '=') ||
               (c == '!' && next == '=') || (c == '<' && next == '=') ||
               (c == '>' && next == '=') || (c == '&' && next == '&') ||
               (c == '|' && next == '|') || (c == '+' && next == '=') ||
               (c == '-' && next == '=');
    }
}


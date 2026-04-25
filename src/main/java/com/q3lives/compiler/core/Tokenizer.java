
package com.q3lives.compiler.core;

import com.q3lives.compiler.scanner.LineInfo;
import com.q3lives.compiler.scanner.SourceView;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
                boolean escaped = false;

                while (i < content.length()) {
                    char ch = content.charAt(i);

                    if (escaped) {
                        // 处理转义字符
                        switch (ch) {
                            case 'n' -> sb.append('\n');
                            case 't' -> sb.append('\t');
                            case 'r' -> sb.append('\r');
                            case 'b' -> sb.append('\b');
                            case 'f' -> sb.append('\f');
                            case '\\' -> sb.append('\\');
                            case '\'' -> sb.append('\'');
                            case '"' -> sb.append('"');
                            default -> sb.append(ch); // 保留未知的转义字符
                        }
                        escaped = false;
                    } else if (ch == '\\') {
                        escaped = true;
                    } else if (ch == quote) {
                        break;
                    } else {
                        sb.append(ch);
                    }
                    i++;
                }

                // 添加结束引号
                if (i < content.length() && content.charAt(i) == quote) {
                    sb.append(quote);
                    i++;
                }

                String stringValue = sb.toString();
                // 验证字符串是否完整
                if (stringValue.charAt(stringValue.length() - 1) != quote) {
                    tokens.add(new Token(TokenType.UNKNOWN, "未闭合的字符串", line, start,
                                        lineInfo.getStartOffset() + start));
                } else {
                    tokens.add(new Token(TokenType.LIT_STRING, stringValue, line, start,
                                        lineInfo.getStartOffset() + start));
                }
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

    // 运算符匹配预编译表（优化性能）
    private static final Map<Character, TokenType> SINGLE_CHAR_OPS;
    static {
        Map<Character, TokenType> map = new HashMap<>();
        map.put('+', TokenType.OP_PLUS);
        map.put('-', TokenType.OP_MINUS);
        map.put('*', TokenType.OP_STAR);
        map.put('/', TokenType.OP_SLASH);
        map.put('%', TokenType.OP_PERCENT);
        map.put('=', TokenType.OP_ASSIGN);
        map.put('<', TokenType.OP_LESS);
        map.put('>', TokenType.OP_GREATER);
        map.put('!', TokenType.OP_NOT);
        map.put('.', TokenType.OP_DOT);
        map.put(':', TokenType.OP_COLON);
        map.put(',', TokenType.OP_COMMA);
        map.put(';', TokenType.OP_SEMICOLON);
        map.put('{', TokenType.OPEN_BRACE);
        map.put('}', TokenType.CLOSE_BRACE);
        map.put('(', TokenType.OPEN_PAREN);
        map.put(')', TokenType.CLOSE_PAREN);
        map.put('[', TokenType.OPEN_BRACKET);
        map.put(']', TokenType.CLOSE_BRACKET);
        SINGLE_CHAR_OPS = Collections.unmodifiableMap(map);
    }

    private static final Map<Character, Map<Character, TokenType>> DOUBLE_CHAR_OPS = Map.of(
        '-', Map.of('>', TokenType.OP_ARROW),
        '=', Map.of('=', TokenType.OP_EQUAL),
        '!', Map.of('=', TokenType.OP_NOT_EQUAL),
        '<', Map.of('=', TokenType.OP_LESS_EQUAL),
        '>', Map.of('=', TokenType.OP_GREATER_EQUAL),
        '&', Map.of('&', TokenType.OP_AND),
        '|', Map.of('|', TokenType.OP_OR),
        '+', Map.of('=', TokenType.OP_PLUS_ASSIGN),
        '-', Map.of('=', TokenType.OP_MINUS_ASSIGN)
    );

    private TokenType matchOperator(char c, char next) {
        // 首先检查双字符运算符（更精确匹配）
        Map<Character, TokenType> nextMap = DOUBLE_CHAR_OPS.get(c);
        if (nextMap != null) {
            return nextMap.get(next);
        }

        // 然后检查单字符运算符
        return SINGLE_CHAR_OPS.get(c);
    }

    private boolean isDoubleCharOp(char c, char next) {
        return (c == '-' && next == '>') || (c == '=' && next == '=') ||
               (c == '!' && next == '=') || (c == '<' && next == '=') ||
               (c == '>' && next == '=') || (c == '&' && next == '&') ||
               (c == '|' && next == '|') || (c == '+' && next == '=') ||
               (c == '-' && next == '=');
    }
}


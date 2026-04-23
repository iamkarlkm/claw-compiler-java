// ==================== TokenUtils.java ====================
package com.q3lives.compiler.common;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.TokenType;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Token 工具类
 *
 * 提供统一的 Token 过滤和处理方法，消除各处理器中的重复代码
 *
 * @author Claw Compiler Team
 * @since 3.1.0
 */
public final class TokenUtils {

    private TokenUtils() {
        // 工具类，禁止实例化
    }

    // ==================== 范围过滤 ====================

    /**
     * 获取指定行范围内的所有 Token
     *
     * @param tokens Token 列表
     * @param startLine 起始行号（包含）
     * @param endLine 结束行号（包含）
     * @return 过滤后的 Token 列表
     */
    public static List<Token> getTokensInRange(List<Token> tokens, int startLine, int endLine) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        return tokens.stream()
                .filter(t -> t.getLine() >= startLine && t.getLine() <= endLine)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定行的所有 Token
     *
     * @param tokens Token 列表
     * @param line 行号
     * @return 该行的 Token 列表
     */
    public static List<Token> getTokensOnLine(List<Token> tokens, int line) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        return tokens.stream()
                .filter(t -> t.getLine() == line)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定列范围内的 Token
     *
     * @param tokens Token 列表
     * @param startColumn 起始列号（包含）
     * @param endColumn 结束列号（包含）
     * @return 过滤后的 Token 列表
     */
    public static List<Token> getTokensInColumnRange(List<Token> tokens, int startColumn, int endColumn) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        return tokens.stream()
                .filter(t -> t.getColumn() >= startColumn && t.getColumn() <= endColumn)
                .collect(Collectors.toList());
    }

    // ==================== 类型过滤 ====================

    /**
     * 获取指定类型的所有 Token
     *
     * @param tokens Token 列表
     * @param type Token 类型
     * @return 过滤后的 Token 列表
     */
    public static List<Token> getTokensOfType(List<Token> tokens, TokenType type) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        return tokens.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定类型集合中的所有 Token
     *
     * @param tokens Token 列表
     * @param types Token 类型集合
     * @return 过滤后的 Token 列表
     */
    public static List<Token> getTokensOfTypes(List<Token> tokens, TokenType... types) {
        if (tokens == null || tokens.isEmpty() || types == null) {
            return List.of();
        }
        return tokens.stream()
                .filter(t -> containsType(types, t.getType()))
                .collect(Collectors.toList());
    }

    private static boolean containsType(TokenType[] types, TokenType type) {
        for (TokenType t : types) {
            if (t == type) return true;
        }
        return false;
    }

    // ==================== 查找方法 ====================

    /**
     * 查找第一个指定类型的 Token
     *
     * @param tokens Token 列表
     * @param type Token 类型
     * @return 找到的 Token，如果不存在返回 null
     */
    public static Token findFirst(List<Token> tokens, TokenType type) {
        if (tokens == null) return null;
        return tokens.stream()
                .filter(t -> t.getType() == type)
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找最后一个指定类型的 Token
     *
     * @param tokens Token 列表
     * @param type Token 类型
     * @return 找到的 Token，如果不存在返回 null
     */
    public static Token findLast(List<Token> tokens, TokenType type) {
        if (tokens == null) return null;
        Token result = null;
        for (Token t : tokens) {
            if (t.getType() == type) {
                result = t;
            }
        }
        return result;
    }

    /**
     * 查找指定条件下的第一个 Token
     *
     * @param tokens Token 列表
     * @param predicate 过滤条件
     * @return 找到的 Token，如果不存在返回 null
     */
    public static Token findFirst(List<Token> tokens, Predicate<Token> predicate) {
        if (tokens == null || predicate == null) return null;
        return tokens.stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找指定 Token 后的第一个指定类型的 Token
     *
     * @param tokens Token 列表
     * @param after 起始 Token
     * @param type 要查找的 Token 类型
     * @return 找到的 Token，如果不存在返回 null
     */
    public static Token findFirstAfter(List<Token> tokens, Token after, TokenType type) {
        if (tokens == null || after == null) return null;
        boolean found = false;
        for (Token t : tokens) {
            if (found && t.getType() == type) {
                return t;
            }
            if (t == after) {
                found = true;
            }
        }
        return null;
    }

    // ==================== 值提取 ====================

    /**
     * 提取 Token 的值序列
     *
     * @param tokens Token 列表
     * @param startType 起始 Token 类型
     * @return 起始 Token 后的所有值拼接成的字符串
     */
    public static String extractValueAfter(List<Token> tokens, TokenType startType) {
        if (tokens == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean afterStart = false;
        for (Token t : tokens) {
            if (t.getType() == startType) {
                afterStart = true;
                continue;
            }
            if (afterStart) {
                sb.append(t.getValue());
            }
        }
        return sb.toString().trim();
    }

    /**
     * 安全获取下一个 Token
     *
     * @param tokens Token 列表
     * @param index 当前索引
     * @return 下一个 Token，如果越界返回 null
     */
    public static Token safeGetNext(List<Token> tokens, int index) {
        if (tokens == null || index < 0 || index + 1 >= tokens.size()) {
            return null;
        }
        return tokens.get(index + 1);
    }

    /**
     * 安全获取指定索引的 Token
     *
     * @param tokens Token 列表
     * @param index 索引
     * @return Token，如果越界返回 null
     */
    public static Token safeGet(List<Token> tokens, int index) {
        if (tokens == null || index < 0 || index >= tokens.size()) {
            return null;
        }
        return tokens.get(index);
    }

    // ==================== 位置相关 ====================

    /**
     * 获取 Token 列表的起始行
     *
     * @param tokens Token 列表
     * @return 起始行号，如果列表为空返回 -1
     */
    public static int getStartLine(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) return -1;
        return tokens.get(0).getLine();
    }

    /**
     * 获取 Token 列表的结束行
     *
     * @param tokens Token 列表
     * @return 结束行号，如果列表为空返回 -1
     */
    public static int getEndLine(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) return -1;
        return tokens.get(tokens.size() - 1).getLine();
    }

    // ==================== 统计方法 ====================

    /**
     * 统计指定类型的 Token 数量
     *
     * @param tokens Token 列表
     * @param type Token 类型
     * @return 数量
     */
    public static int countTokens(List<Token> tokens, TokenType type) {
        if (tokens == null) return 0;
        return (int) tokens.stream()
                .filter(t -> t.getType() == type)
                .count();
    }

    /**
     * 检查是否存在指定类型的 Token
     *
     * @param tokens Token 列表
     * @param type Token 类型
     * @return 如果存在返回 true
     */
    public static boolean hasToken(List<Token> tokens, TokenType type) {
        if (tokens == null) return false;
        return tokens.stream().anyMatch(t -> t.getType() == type);
    }
}

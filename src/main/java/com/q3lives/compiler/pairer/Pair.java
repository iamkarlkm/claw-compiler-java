
// ==================== Pair.java ====================
package com.q3lives.compiler.pairer;

import lombok.Getter;
import lombok.ToString;

/**
 * 配对数据结构 - 记录一组匹配的符号对
 */
@Getter
@ToString
public class Pair {
    /** 配对类型 */
    private final PairType type;
    /** 开始行号 */
    private final int openLine;
    /** 开始列号 */
    private final int openCol;
    /** 结束行号 */
    private final int closeLine;
    /** 结束列号 */
    private final int closeCol;
    /** 嵌套深度 */
    private final int depth;

    public Pair(PairType type, int openLine, int openCol, 
                int closeLine, int closeCol, int depth) {
        this.type = type;
        this.openLine = openLine;
        this.openCol = openCol;
        this.closeLine = closeLine;
        this.closeCol = closeCol;
        this.depth = depth;
    }

    /** 是否为单行配对 */
    public boolean isSingleLine() {
        return openLine == closeLine;
    }
    
    public int getDepth(){
        
        return depth;
        
    }

    PairType getType() {
        return type;
    }

    public int getOpenLine() {
        return openLine;
    }

    public int getCloseLine() {
        return closeLine;
    }

    public int getOpenCol() {
        return openCol;
    }

    public int getCloseCol() {
        return closeCol;
    }

    /** 配对类型枚举 */
    public enum PairType {
        BRACE('{', '}'),        // 花括号
        PAREN('(', ')'),        // 圆括号
        BRACKET('[', ']'),      // 方括号
        DOUBLE_QUOTE('"', '"'), // 双引号
        SINGLE_QUOTE('\'', '\''); // 单引号

        @Getter
        private final char open;
        @Getter
        private final char close;

        PairType(char open, char close) {
            this.open = open;
            this.close = close;
        }

        public static PairType fromOpen(char c) {
            for (PairType t : values()) {
                if (t.open == c) return t;
            }
            return null;
        }

        public static PairType fromClose(char c) {
            for (PairType t : values()) {
                if (t.close == c) return t;
            }
            return null;
        }

        public static boolean isOpen(char c) {
            return c == '{' || c == '(' || c == '[';
        }

        public static boolean isClose(char c) {
            return c == '}' || c == ')' || c == ']';
        }
    }
}

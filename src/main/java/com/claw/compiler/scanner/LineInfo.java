// ==================== LineInfo.java ====================
package com.claw.compiler.scanner;

import lombok.Getter;
import lombok.ToString;

/**
 * 行信息 - 记录每行的详细信息
 */
@Getter
@ToString
public class LineInfo {
    /** 行号（从1开始） */
    private final int lineNumber;
    /** 原始内容 */
    private final String rawContent;
    /** 去除注释和空白后的内容 */
    private String cleanContent;
    /** 缩进级别 */
    private final int indentLevel;
    /** 起始偏移量（在整个文件中的字符偏移） */
    private final int startOffset;
    /** 是否为空行 */
    private final boolean blank;
    /** 是否为注释行 */
    private boolean commentLine;

    public LineInfo(int lineNumber, String rawContent, int startOffset) {
        this.lineNumber = lineNumber;
        this.rawContent = rawContent;
        this.startOffset = startOffset;
        this.indentLevel = calculateIndent(rawContent);
        this.blank = rawContent.trim().isEmpty();
        this.cleanContent = rawContent;
        this.commentLine = false;
    }

    private int calculateIndent(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') indent++;
            else if (c == '	') indent += 4;
            else break;
        }
        return indent;
    }

    public void setCleanContent(String cleanContent) {
        this.cleanContent = cleanContent;
    }

    public void markAsComment() {
        this.commentLine = true;
    }

    /** 获取去除首尾空白的内容 */
    public String getTrimmedContent() {
        return cleanContent != null ? cleanContent.trim() : "";
    }

    /** 该行是否有效（非空、非注释） */
    public boolean isEffective() {
        return !blank && !commentLine;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getRawContent() {
        return rawContent;
    }
}

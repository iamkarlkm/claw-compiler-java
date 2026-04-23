// ==================== SourceView.java ====================
package com.q3lives.compiler.scanner;

import lombok.Getter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 源代码视图 - 提供对源代码的各种视角访问
 * 
 * 思想2第1步：建立完整源代码视图
 */
@Getter
public class SourceView {
    /** 所有行信息 */
    private final List<LineInfo> lines;
    /** 原始源代码 */
    private final String rawSource;
    /** 文件名 */
    private final String fileName;
    /** 行号到行信息的映射 */
    private final Map<Integer, LineInfo> lineMap;

    public SourceView(String rawSource, String fileName, List<LineInfo> lines) {
        this.rawSource = rawSource;
        this.fileName = fileName;
        this.lines = Collections.unmodifiableList(lines);
        this.lineMap = new HashMap<>();
        for (LineInfo line : lines) {
            lineMap.put(line.getLineNumber(), line);
        }
    }

    /** 获取指定行 */
    public LineInfo getLine(int lineNumber) {
        return lineMap.get(lineNumber);
    }

    /** 获取总行数 */
    public int getTotalLines() {
        return lines.size();
    }

    /** 获取有效行（非空、非注释） */
    public List<LineInfo> getEffectiveLines() {
        return lines.stream()
                .filter(LineInfo::isEffective)
                .collect(Collectors.toList());
    }

    /** 获取指定范围的行 */
    public List<LineInfo> getLines(int startLine, int endLine) {
        return lines.stream()
                .filter(l -> l.getLineNumber() >= startLine && l.getLineNumber() <= endLine)
                .collect(Collectors.toList());
    }

    /** 获取指定范围的源代码文本 */
    public String getSourceRange(int startLine, int endLine) {
        return getLines(startLine, endLine).stream()
                .map(LineInfo::getRawContent)
                .collect(Collectors.joining(""));
    }

    public Iterable<LineInfo> getLines() {
        return lines;
    }

    public String getFileName() {
        return fileName;
    }
}

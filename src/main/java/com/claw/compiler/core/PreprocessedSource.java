package com.claw.compiler.core;

import java.util.ArrayList;
import java.util.List;

import com.claw.compiler.scanner.SourceView;

/**
 * 预处理后的源代码 - 存储预处理结果
 */
public class PreprocessedSource {
    // 预处理后的代码内容
    private String content;
    
    // 预处理后的行列表
    private List<String> lines;
    
    // 预处理过程中移除的注释
    private List<CommentInfo> removedComments;
    
    // 预处理过程中应用的宏
    private List<MacroInfo> appliedMacros;
    
    // 原始源代码视图
    private SourceView originalSource;
    
    public PreprocessedSource(String content, SourceView originalSource) {
        this.content = content;
        this.originalSource = originalSource;
        this.lines = new ArrayList<>();
        this.removedComments = new ArrayList<>();
        this.appliedMacros = new ArrayList<>();
        
        // 将内容按行分割
        String[] lineArray = content.split("\\r?\\n");
        for (String line : lineArray) {
            lines.add(line);
        }
    }
    
    // Getter方法
    public String getContent() {
        return content;
    }
    
    public List<String> getLines() {
        return lines;
    }
    
    public List<CommentInfo> getRemovedComments() {
        return removedComments;
    }
    
    public List<MacroInfo> getAppliedMacros() {
        return appliedMacros;
    }
    
    public SourceView getOriginalSource() {
        return originalSource;
    }
    
    // 添加移除的注释
    public void addRemovedComment(CommentInfo comment) {
        removedComments.add(comment);
    }
    
    // 添加应用的宏
    public void addAppliedMacro(MacroInfo macro) {
        appliedMacros.add(macro);
    }
    
    // 获取指定行
    public String getLine(int lineNumber) {
        if (lineNumber >= 0 && lineNumber < lines.size()) {
            return lines.get(lineNumber);
        }
        return null;
    }
    
    // 获取行数
    public int getLineCount() {
        return lines.size();
    }
    
    /**
     * 注释信息 - 存储被移除的注释信息
     */
    public static class CommentInfo {
        private String content;
        private int startLine;
        private int endLine;
        private int startColumn;
        private int endColumn;
        private boolean isBlockComment;
        
        public CommentInfo(String content, int startLine, int endLine, 
                          int startColumn, int endColumn, boolean isBlockComment) {
            this.content = content;
            this.startLine = startLine;
            this.endLine = endLine;
            this.startColumn = startColumn;
            this.endColumn = endColumn;
            this.isBlockComment = isBlockComment;
        }
        
        // Getter方法
        public String getContent() {
            return content;
        }
        
        public int getStartLine() {
            return startLine;
        }
        
        public int getEndLine() {
            return endLine;
        }
        
        public int getStartColumn() {
            return startColumn;
        }
        
        public int getEndColumn() {
            return endColumn;
        }
        
        public boolean isBlockComment() {
            return isBlockComment;
        }
    }
    
    /**
     * 宏信息 - 存储应用的宏信息
     */
    public static class MacroInfo {
        private String name;
        private String expansion;
        private int line;
        private int column;
        
        public MacroInfo(String name, String expansion, int line, int column) {
            this.name = name;
            this.expansion = expansion;
            this.line = line;
            this.column = column;
        }
        
        // Getter方法
        public String getName() {
            return name;
        }
        
        public String getExpansion() {
            return expansion;
        }
        
        public int getLine() {
            return line;
        }
        
        public int getColumn() {
            return column;
        }
    }
}

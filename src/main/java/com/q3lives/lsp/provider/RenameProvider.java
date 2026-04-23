package com.q3lives.lsp.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;

import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.lsp.utils.DiagnosticGenerator;
import com.q3lives.lsp.utils.JSONUtils;
import com.q3lives.lsp.utils.PerformanceMonitor;

/**
 * LSP 重命名提供器
 *
 * 负责实现 "重命名" 功能：
 * - 重命名变量
 * - 重命名函数
 * - 重命名类型
 * - 重命名注解
 */
public class RenameProvider {

    private final SemanticContext semanticContext;
    private final CompletionProvider completionProvider;

    public RenameProvider(SemanticContext semanticContext, CompletionProvider completionProvider) {
        this.semanticContext = semanticContext;
        this.completionProvider = completionProvider;
    }

    /**
     * 重命名符号
     *
     * @param document 文档内容
     * @param position 光标位置
     * @param newName 新名称
     * @return 工作区编辑
     */
    public WorkspaceEdit rename(String document, int line, int character, String newName) {
        long startTime = System.currentTimeMillis();

        try {
            // 识别当前符号
            String oldName = extractSymbolName(document, line, character);

            if (oldName == null || oldName.isEmpty()) {
                return new WorkspaceEdit(Collections.emptyMap());
            }

            // 获取所有引用位置
            List<Location> locations = findReferences(document, line, character, oldName);

            // 生成文本编辑列表
            List<TextEdit> edits = new ArrayList<>();

            for (Location loc : locations) {
                // 创建文本编辑
                TextEdit edit = new TextEdit();
                edit.setRange(loc.getRange());
                edit.setNewText(newName);

                // 添加到编辑列表
                edits.add(edit);
            }

            // 创建工作区编辑
            WorkspaceEdit workspaceEdit = new WorkspaceEdit();
            workspaceEdit.setChanges(Collections.singletonMap(
                document,
                edits
            ));

            return workspaceEdit;

        } catch (Exception e) {
            System.err.println("Error in rename: " + e.getMessage());
            e.printStackTrace();
            return new WorkspaceEdit(Collections.emptyMap());
        } finally {
            // 记录性能
            PerformanceMonitor.getInstance().record("rename",
                System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 从行中提取符号名称
     *
     * @param document 文档内容
     * @param line 行号
     * @param character 列号
     * @return 符号名称
     */
    private String extractSymbolName(String document, int line, int character) {
        String[] lines = document.split("\n");

        if (line < 0 || line >= lines.length) {
            return null;
        }

        String lineContent = lines[line];

        if (character <= 0 || character >= lineContent.length()) {
            return null;
        }

        // 从当前列向前查找符号的开始
        int start = character - 1;
        while (start >= 0 && isSymbolChar(lineContent.charAt(start))) {
            start--;
        }

        start++;

        // 从当前列向后查找符号的结束
        int end = character;
        while (end < lineContent.length() && isSymbolChar(lineContent.charAt(end))) {
            end++;
        }

        if (start >= end) {
            return null;
        }

        return lineContent.substring(start, end).trim();
    }

    /**
     * 检查是否是符号字符
     */
    private boolean isSymbolChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == ':' || c == '.';
    }

    /**
     * 查找所有引用位置
     *
     * @param document 文档内容
     * @param startLine 开始行号
     * @param startChar 开始列号
     * @param oldName 旧名称
     * @return 引用位置列表
     */
    private List<Location> findReferences(String document, int startLine, int startChar, String oldName) {
        List<Location> references = new ArrayList<>();

        String[] lines = document.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 查找引用
            List<Location> lineReferences = findReferencesInLine(line, i, oldName);

            if (!lineReferences.isEmpty()) {
                references.addAll(lineReferences);
            }
        }

        // 移除定义位置
        references.removeIf(loc -> loc.getRange().getStart().getLine() == startLine &&
                                   loc.getRange().getStart().getCharacter() == startChar);

        return references;
    }

    /**
     * 在行中查找引用
     *
     * @param line 当前行
     * @param lineNumber 行号
     * @param oldName 旧名称
     * @return 引用位置列表
     */
    private List<Location> findReferencesInLine(String line, int lineNumber, String oldName) {
        List<Location> references = new ArrayList<>();

        String lowerLine = line.toLowerCase();
        String lowerName = oldName.toLowerCase();

        int pos = 0;
        while ((pos = lowerLine.indexOf(lowerName, pos)) != -1) {
            // 验证前后字符，确保是独立的符号
            boolean isValid = true;

            if (pos > 0) {
                char prevChar = line.charAt(pos - 1);
                if (Character.isLetterOrDigit(prevChar) || prevChar == '_') {
                    isValid = false;
                }
            }

            if (pos + oldName.length() < line.length()) {
                char nextChar = line.charAt(pos + oldName.length());
                if (Character.isLetterOrDigit(nextChar) || nextChar == '_') {
                    isValid = false;
                }
            }

            if (isValid) {
                Location location = new Location();
                Range range = DiagnosticGenerator.createRange(
                    lineNumber,
                    pos,
                    lineNumber,
                    pos + oldName.length()
                );
                location.setRange(range);
                references.add(location);
            }

            pos += oldName.length();
        }

        return references;
    }

    /**
     * 引用位置类
     */
    private static class Location {
        private Range range;

        public Range getRange() {
            return range;
        }

        public void setRange(Range range) {
            this.range = range;
        }
    }
}

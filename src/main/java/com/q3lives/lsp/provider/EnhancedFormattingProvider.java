package com.q3lives.lsp.provider;

import com.q3lives.compiler.context.SemanticContext;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.FormattingOptions;
import java.util.*;
import java.util.regex.*;

/**
 * 增强版文档格式化提供器
 */
public class EnhancedFormattingProvider {

    private final SemanticContext semanticContext;
    private FormattingOptions formattingOptions;

    public EnhancedFormattingProvider(SemanticContext semanticContext) {
        this.semanticContext = semanticContext;
        this.formattingOptions = new FormattingOptions();
    }

    public List<TextEdit> provideFormatting(String document) {
        List<TextEdit> textEdits = new ArrayList<>();
        String[] lines = document.split("\n");

        StringBuilder formatted = new StringBuilder();
        int indentLevel = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // 处理缩进
            String indent = " ".repeat(indentLevel * formattingOptions.getTabSize());

            // 处理大括号
            if (trimmed.endsWith("}")) {
                indentLevel--;
                indent = " ".repeat(indentLevel * formattingOptions.getTabSize());
            }

            // 添加格式化后的行
            if (!trimmed.isEmpty()) {
                formatted.append(indent).append(trimmed);
            }
            formatted.append("\n");

            // 处理大括号增加缩进
            if (trimmed.endsWith("{")) {
                indentLevel++;
            }
        }

        // 创建文本编辑
        textEdits.add(new TextEdit(
            new org.eclipse.lsp4j.Range(
                new org.eclipse.lsp4j.Position(0, 0),
                new org.eclipse.lsp4j.Position(lines.length - 1, lines[lines.length - 1].length())
            ),
            formatted.toString()
        ));

        return textEdits;
    }

    public void updateConfiguration(FormattingOptions options) {
        this.formattingOptions = options;
    }
}
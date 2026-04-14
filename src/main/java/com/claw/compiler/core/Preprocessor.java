// ==================== Preprocessor.java ====================
package com.claw.compiler.core;

import com.claw.compiler.scanner.LineInfo;
import com.claw.compiler.scanner.SourceView;
import lombok.extern.slf4j.Slf4j;

/**
 * 预处理器
 * 
 * 第1层基础处理器：代码清理，行映射建立
 */
@Slf4j
public class Preprocessor {

    /**
     * 预处理源代码视图
     * @return 
     */
    public PreprocessedSource preprocess(SourceView sourceView) {
        log.debug("开始预处理: {}", sourceView.getFileName());

        boolean inBlockComment = false;

        for (LineInfo line : sourceView.getLines()) {
            String raw = line.getRawContent();
            StringBuilder clean = new StringBuilder();
            boolean inString = false;
            char stringChar = 0;

            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                char next = (i + 1 < raw.length()) ? raw.charAt(i + 1) : 0;

                // 块注释
                if (inBlockComment) {
                    if (c == '*' && next == '/') {
                        inBlockComment = false;
                        i++; // 跳过 /
                    }
                    continue;
                }

                // 字符串
                if (inString) {
                    clean.append(c);
                    if (c == '\\' && i + 1 < raw.length()) {
                        i++;
                        clean.append(raw.charAt(i));
                        continue;
                    }
                    if (c == stringChar) {
                        inString = false;
                    }
                    continue;
                }

                // 检测注释开始
                if (c == '/' && next == '/') {
                    break; // 行注释，忽略剩余
                }
                if (c == '/' && next == '*') {
                    inBlockComment = true;
                    i++; // 跳过 *
                    continue;
                }

                // 检测字符串开始
                if (c == '"' || c == '\'') {
                    inString = true;
                    stringChar = c;
                }

                clean.append(c);
            }

            String cleanStr = clean.toString();
            line.setCleanContent(cleanStr);

            // 标记纯注释行
            if (cleanStr.trim().isEmpty() && !line.getRawContent().trim().isEmpty()) {
                line.markAsComment();
            }
        }

        log.info("预处理完成: {} 行处理完毕", sourceView.getTotalLines());

        return new PreprocessedSource(null,sourceView);
    }
}


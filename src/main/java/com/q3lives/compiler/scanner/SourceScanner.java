
// ==================== SourceScanner.java ====================
package com.q3lives.compiler.scanner;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 源代码扫描器
 *
 * 思想2第1步：先扫描整个文件，保存所有lines
 */
@Slf4j
public class SourceScanner {


    /**
     * 从文件扫描
     */
    public SourceView scanFile(String filePath) throws IOException {
        log.info("扫描文件: {}", filePath);
        Path path = Path.of(filePath);

        try {
            String content = Files.readString(path);
            String fileName = path.getFileName().toString();
            return scan(content, fileName);
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            throw e;
        }
    }

    /**
     * 从字符串扫描
     */
    public SourceView scan(String source, String fileName) {
        log.debug("开始扫描源代码: {} ({} 字符)", fileName, source.length());

        List<LineInfo> lines = new ArrayList<>();
        int offset = 0;
        int lineNum = 1;

        // 使用正则表达式高效分割行，避免创建大量小字符串
        java.util.regex.Pattern linePattern = java.util.regex.Pattern.compile(".*?\\r?\\n");
        java.util.regex.Matcher lineMatcher = linePattern.matcher(source);

        while (lineMatcher.find()) {
            String line = lineMatcher.group().trim();
            if (!line.isEmpty()) {
                LineInfo lineInfo = new LineInfo(lineNum, line, offset);
                lines.add(lineInfo);
            }
            offset += lineMatcher.group().length();
            lineNum++;
        }

        // 处理最后一行（如果没有换行符结尾）
        if (offset < source.length()) {
            String lastLine = source.substring(offset).trim();
            if (!lastLine.isEmpty()) {
                LineInfo lineInfo = new LineInfo(lineNum, lastLine, offset);
                lines.add(lineInfo);
            }
        }

        SourceView view = new SourceView(source, fileName, lines);
        log.info("扫描完成: {} 行, 有效行 {}",
                view.getTotalLines(), view.getEffectiveLines().size());
        return view;
    }
}

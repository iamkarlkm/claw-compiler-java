// ==================== PairingAnalyzer.java ====================
package com.claw.compiler.pairer;

import com.claw.compiler.scanner.LineInfo;
import com.claw.compiler.scanner.SourceView;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * 配对分析器
 * 
 * 思想2第2步：配对检查，确保所有符号正确配对
 */
@Slf4j
public class PairingAnalyzer {

    /**
     * 分析源代码中的所有配对结构
     */
    public PairingResult analyze(SourceView sourceView) {
        log.debug("开始配对分析: {}", sourceView.getFileName());

        List<Pair> pairs = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Stack<OpenSymbol> stack = new Stack<>();
        boolean inString = false;
        char stringChar = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (LineInfo lineInfo : sourceView.getLines()) {
            String line = lineInfo.getRawContent();
            inLineComment = false;

            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                char next = (col + 1 < line.length()) ? line.charAt(col + 1) : 0;

                // 块注释处理
                if (inBlockComment) {
                    if (c == '*' && next == '/') {
                        inBlockComment = false;
                        col++;
                    }
                    continue;
                }

                // 行注释处理
                if (inLineComment) continue;

                // 检测注释开始
                if (!inString) {
                    if (c == '/' && next == '/') {
                        inLineComment = true;
                        continue;
                    }
                    if (c == '/' && next == '*') {
                        inBlockComment = true;
                        col++;
                        continue;
                    }
                }

                // 字符串处理
                if (inString) {
                    if (c == '\\') {
                        col++; // 跳过转义字符
                        continue;
                    }
                    if (c == stringChar) {
                        inString = false;
                    }
                    continue;
                }

                // 检测字符串开始
                if (c == '"' || c == '\'') {
                    inString = true;
                    stringChar = c;
                    continue;
                }

                // 配对符号处理
                int lineNum = lineInfo.getLineNumber();

                if (Pair.PairType.isOpen(c)) {
                    stack.push(new OpenSymbol(c, lineNum, col, stack.size()));
                } else if (Pair.PairType.isClose(c)) {
                    if (stack.isEmpty()) {
                        errors.add(String.format(
                            "第%d行第%d列: 未配对的结束符号 '%c'", lineNum, col, c));
                        continue;
                    }

                    OpenSymbol open = stack.pop();
                    Pair.PairType type = Pair.PairType.fromOpen(open.ch);

                    if (type == null || type.getClose() != c) {
                        errors.add(String.format(
                            "第%d行第%d列: 配对不匹配，'%c'(第%d行) 期望 '%s' 但得到 '%c'",
                            lineNum, col, open.ch, open.line,
                            type != null ? type.getClose() : "?", c));
                        continue;
                    }

                    pairs.add(new Pair(type, open.line, open.col, 
                                       lineNum, col, open.depth));
                }
            }
        }

        // 检查未关闭的符号
        while (!stack.isEmpty()) {
            OpenSymbol open = stack.pop();
            errors.add(String.format(
                "第%d行第%d列: 未关闭的符号 '%c'", open.line, open.col, open.ch));
        }

        if (inBlockComment) {
            errors.add("文件结尾: 未关闭的块注释");
        }

        boolean valid = errors.isEmpty();
        if (valid) {
            log.info("配对分析完成: {} 个配对, 全部正确", pairs.size());
        } else {
            log.error("配对分析发现 {} 个错误", errors.size());
            errors.forEach(e -> log.error("  {}", e));
        }

        return new PairingResult(pairs, valid, errors);
    }

    /** 内部类：记录开始符号信息 */
    private record OpenSymbol(char ch, int line, int col, int depth) {}
}


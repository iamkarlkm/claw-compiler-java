package com.q3lives.lsp.provider;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.lsp.utils.DiagnosticGenerator;

/**
 * LSP 跳转定义提供器
 *
 * 负责实现 "跳转到定义" 功能：
 * - 跳转到函数定义
 * - 跳转到类型定义
 * - 跳转到变量定义
 * - 跳转到注解定义
 */
public class DefinitionProvider {

    private final SemanticContext semanticContext;

    /**
     * 创建定义提供器
     */
    public DefinitionProvider(SemanticContext semanticContext) {
        this.semanticContext = semanticContext;
    }

    /**
     * 查找符号定义
     *
     * @param document 文档内容
     * @param position 光标位置
     * @return 定义位置列表
     */
    public List<Location> findDefinition(String document, Position position) {
        List<Location> definitions = new ArrayList<>();

        try {
            // 识别当前符号
            Symbol symbol = identifySymbol(document, position);

            if (symbol != null) {
                // 查找定义位置
                Location location = findDefinitionLocation(symbol);

                if (location != null) {
                    definitions.add(location);
                }
            }

        } catch (Exception e) {
            System.err.println("Error finding definition: " + e.getMessage());
        }

        return definitions;
    }

    /**
     * 识别当前符号
     *
     * @param document 文档内容
     * @param position 光标位置
     * @return 符号信息
     */
    private Symbol identifySymbol(String document, Position position) {
        int line = position.getLine();
        int charIndex = position.getCharacter();

        if (line < 0 || charIndex < 0) {
            return null;
        }

        String[] lines = document.split("\n");
        if (line >= lines.length) {
            return null;
        }

        String lineStr = lines[line];
        String symbolName = extractSymbolName(lineStr, charIndex);

        if (symbolName == null || symbolName.isEmpty()) {
            return null;
        }

        // 创建符号信息
        return new Symbol(symbolName, line, charIndex);
    }

    /**
     * 从行中提取符号名称
     *
     * @param line 当前行
     * @param charIndex 光标位置
     * @return 符号名称
     */
    private String extractSymbolName(String line, int charIndex) {
        if (charIndex <= 0) {
            return null;
        }

        // 从光标位置向前查找符号的开始
        int start = charIndex - 1;
        while (start >= 0 && isSymbolChar(line.charAt(start))) {
            start--;
        }

        start++;

        // 从光标位置向后查找符号的结束
        int end = charIndex;
        while (end < line.length() && isSymbolChar(line.charAt(end))) {
            end++;
        }

        if (start >= end) {
            return null;
        }

        return line.substring(start, end).trim();
    }

    /**
     * 检查是否是符号字符
     */
    private boolean isSymbolChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == ':' || c == '.';
    }

    /**
     * 查找定义位置
     *
     * @param symbol 符号信息
     * @return 定义位置
     */
    private Location findDefinitionLocation(Symbol symbol) {
        // TODO: 从语义上下文中查找定义
        // 这里应该从 semanticContext 中查找符号的定义位置

        // 临时实现：返回符号的当前位置作为占位符
        Position position = new Position(symbol.getLine(), symbol.getCharIndex());
        Range range = DiagnosticGenerator.createRange(symbol.getLine(), symbol.getCharIndex());

        return new Location(null, range);
    }

    /**
     * 获取定义位置列表（所有符号）
     */
    public List<Location> getAllDefinitions(String document) {
        List<Location> definitions = new ArrayList<>();

        try {
            String[] lines = document.split("\n");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                // 提取所有符号
                List<Symbol> symbols = extractAllSymbols(line);

                for (Symbol symbol : symbols) {
                    Location location = findDefinitionLocation(symbol);
                    if (location != null) {
                        definitions.add(location);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error getting all definitions: " + e.getMessage());
        }

        return definitions;
    }

    /**
     * 从行中提取所有符号
     */
    private List<Symbol> extractAllSymbols(String line) {
        List<Symbol> symbols = new ArrayList<>();

        StringBuilder currentSymbol = new StringBuilder();
        boolean inSymbol = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (isSymbolChar(c)) {
                if (!inSymbol) {
                    inSymbol = true;
                    currentSymbol.setLength(0);
                }
                currentSymbol.append(c);
            } else {
                if (inSymbol) {
                    String symbolName = currentSymbol.toString().trim();
                    if (!symbolName.isEmpty()) {
                        symbols.add(new Symbol(symbolName, 0, i));
                    }
                    inSymbol = false;
                }
            }
        }

        // 处理行尾的符号
        if (inSymbol) {
            String symbolName = currentSymbol.toString().trim();
            if (!symbolName.isEmpty()) {
                symbols.add(new Symbol(symbolName, 0, line.length()));
            }
        }

        return symbols;
    }

    /**
     * 符号信息类
     */
    private static class Symbol {
        private final String name;
        private final int line;
        private final int charIndex;

        public Symbol(String name, int line, int charIndex) {
            this.name = name;
            this.line = line;
            this.charIndex = charIndex;
        }

        public String getName() {
            return name;
        }

        public int getLine() {
            return line;
        }

        public int getCharIndex() {
            return charIndex;
        }
    }
}

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
 * LSP 查找引用提供器
 *
 * 负责实现 "查找引用" 功能：
 * - 查找变量引用
 * - 查找函数引用
 * - 查找类型引用
 * - 查找注解引用
 */
public class ReferenceProvider {

    private final SemanticContext semanticContext;

    /**
     * 创建引用提供器
     */
    public ReferenceProvider(SemanticContext semanticContext) {
        this.semanticContext = semanticContext;
    }

    /**
     * 查找所有引用
     *
     * @param document 文档内容
     * @param position 光标位置
     * @return 引用位置列表
     */
    public List<Location> findReferences(String document, Position position) {
        List<Location> references = new ArrayList<>();

        try {
            // 识别当前符号
            Symbol symbol = identifySymbol(document, position);

            if (symbol != null) {
                // 查找所有引用
                references = findSymbolReferences(symbol, document);
            }

        } catch (Exception e) {
            System.err.println("Error finding references: " + e.getMessage());
        }

        return references;
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
     * 查找符号的所有引用
     *
     * @param symbol 符号信息
     * @param document 文档内容
     * @return 引用位置列表
     */
    private List<Location> findSymbolReferences(Symbol symbol, String document) {
        List<Location> references = new ArrayList<>();

        try {
            String[] lines = document.split("\n");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                // 查找引用
                List<Symbol> foundSymbols = extractSymbolsByName(line, symbol.getName());

                for (Symbol foundSymbol : foundSymbols) {
                    // 跳过定义位置本身
                    if (foundSymbol.getLine() == symbol.getLine() &&
                        foundSymbol.getCharIndex() == symbol.getCharIndex()) {
                        continue;
                    }

                    // 创建引用位置
                    Range range = DiagnosticGenerator.createRange(
                        foundSymbol.getLine(),
                        foundSymbol.getCharIndex(),
                        foundSymbol.getLine(),
                        foundSymbol.getCharIndex() + symbol.getName().length()
                    );

                    Location reference = new Location(null, range);
                    references.add(reference);
                }
            }

        } catch (Exception e) {
            System.err.println("Error finding symbol references: " + e.getMessage());
        }

        return references;
    }

    /**
     * 从行中提取指定名称的所有符号
     *
     * @param line 当前行
     * @param name 符号名称
     * @return 符号列表
     */
    private List<Symbol> extractSymbolsByName(String line, String name) {
        List<Symbol> symbols = new ArrayList<>();

        String lowerLine = line.toLowerCase();
        String lowerName = name.toLowerCase();

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

            if (pos + name.length() < line.length()) {
                char nextChar = line.charAt(pos + name.length());
                if (Character.isLetterOrDigit(nextChar) || nextChar == '_') {
                    isValid = false;
                }
            }

            if (isValid) {
                symbols.add(new Symbol(name, 0, pos));
            }

            pos += name.length();
        }

        return symbols;
    }

    /**
     * 获取引用列表（所有符号）
     */
    public List<Location> getAllReferences(String document) {
        List<Location> references = new ArrayList<>();

        try {
            String[] lines = document.split("\n");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                // 提取所有符号
                List<Symbol> symbols = extractAllSymbols(line);

                for (Symbol symbol : symbols) {
                    // 查找引用
                    List<Location> symbolReferences = findSymbolReferences(symbol, document);
                    references.addAll(symbolReferences);
                }
            }

        } catch (Exception e) {
            System.err.println("Error getting all references: " + e.getMessage());
        }

        return references;
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

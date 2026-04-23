package com.q3lives.lsp.provider;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.SymbolTag;
import org.eclipse.lsp4j.TextDocumentIdentifier;

import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.lsp.utils.PerformanceMonitor;

/**
 * LSP 文档符号提供器
 *
 * 负责实现 "文档符号" 功能：
 * - 显示文档结构
 * - 显示函数列表
 * - 显示类型定义
 * - 显示嵌套结构
 */
public class DocumentSymbolProvider {

    private final SemanticContext semanticContext;

    public DocumentSymbolProvider(SemanticContext semanticContext) {
        this.semanticContext = semanticContext;
    }

    /**
     * 提供文档符号
     *
     * @param document 文档内容
     * @return 文档符号列表
     */
    public List<DocumentSymbol> provideDocumentSymbols(String document) {
        long startTime = System.currentTimeMillis();

        try {
            List<DocumentSymbol> symbols = new ArrayList<>();

            // 解析文档结构
            List<SymbolNode> symbolNodes = parseDocumentStructure(document);

            // 转换为 DocumentSymbol
            for (SymbolNode node : symbolNodes) {
                DocumentSymbol symbol = convertToDocumentSymbol(node);
                symbols.add(symbol);
            }

            return symbols;

        } catch (Exception e) {
            System.err.println("Error providing document symbols: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            // 记录性能
            PerformanceMonitor.getInstance().record("documentSymbols",
                System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 解析文档结构
     *
     * @param document 文档内容
     * @return 符号节点列表
     */
    private List<SymbolNode> parseDocumentStructure(String document) {
        List<SymbolNode> nodes = new ArrayList<>();

        String[] lines = document.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检查函数定义
            SymbolNode funcNode = parseFunction(line, i);
            if (funcNode != null) {
                nodes.add(funcNode);
            }

            // 检查类型定义
            SymbolNode typeNode = parseType(line, i);
            if (typeNode != null) {
                nodes.add(typeNode);
            }

            // 检查切面定义
            SymbolNode aspectNode = parseAspect(line, i);
            if (aspectNode != null) {
                nodes.add(aspectNode);
            }
        }

        // 简单的嵌套处理（按缩进层级）
        return buildSymbolHierarchy(nodes);
    }

    /**
     * 解析函数定义
     *
     * @param line 当前行
     * @param lineNumber 行号
     * @return 符号节点，如果不是函数返回 null
     */
    private SymbolNode parseFunction(String line, int lineNumber) {
        // 匹配函数定义模式
        if (!line.contains("function")) {
            return null;
        }

        // 简单的函数名提取
        int funcIndex = line.indexOf("function");
        int nameStart = funcIndex + "function".length();

        while (nameStart < line.length() && Character.isWhitespace(line.charAt(nameStart))) {
            nameStart++;
        }

        int nameEnd = nameStart;
        while (nameEnd < line.length() && isSymbolChar(line.charAt(nameEnd))) {
            nameEnd++;
        }

        if (nameStart >= nameEnd) {
            return null;
        }

        String functionName = line.substring(nameStart, nameEnd).trim();

        SymbolNode node = new SymbolNode();
        node.setName(functionName);
        node.setType(SymbolKind.Function);
        node.setRange(new Range(
            new Position(lineNumber, nameStart),
            new Position(lineNumber, nameEnd)
        ));
        node.setChildren(new ArrayList<>());

        // 检查是否有返回类型
        if (line.contains("->")) {
            int returnIndex = line.indexOf("->");
            int returnEnd = line.indexOf("function", returnIndex);
            if (returnEnd > returnIndex) {
                node.setDetail(line.substring(returnIndex + 2, returnEnd).trim());
            }
        }

        return node;
    }

    /**
     * 解析类型定义
     *
     * @param line 当前行
     * @param lineNumber 行号
     * @return 符号节点，如果不是类型返回 null
     */
    private SymbolNode parseType(String line, int lineNumber) {
        // 简单的类型检测（以大写字母开头）
        if (line.isEmpty() || !Character.isUpperCase(line.charAt(0))) {
            return null;
        }

        int nameEnd = 0;
        while (nameEnd < line.length() && isSymbolChar(line.charAt(nameEnd))) {
            nameEnd++;
        }

        if (nameEnd == 0) {
            return null;
        }

        String typeName = line.substring(0, nameEnd).trim();

        SymbolNode node = new SymbolNode();
        node.setName(typeName);
        node.setType(SymbolKind.Class); // 使用 Class 作为类型的符号类型
        node.setRange(new Range(
            new Position(lineNumber, 0),
            new Position(lineNumber, nameEnd)
        ));
        node.setChildren(new ArrayList<>());

        return node;
    }

    /**
     * 解析切面定义
     *
     * @param line 当前行
     * @param lineNumber 行号
     * @return 符号节点，如果不是切面返回 null
     */
    private SymbolNode parseAspect(String line, int lineNumber) {
        if (!line.contains("@Aspect") && !line.contains("aspect")) {
            return null;
        }

        // 提取切面名称
        int aspectIndex = line.indexOf("@Aspect");
        if (aspectIndex == -1) {
            aspectIndex = line.indexOf("aspect");
        }

        if (aspectIndex == -1) {
            return null;
        }

        int nameStart = aspectIndex + ("@Aspect".length());
        while (nameStart < line.length() && Character.isWhitespace(line.charAt(nameStart))) {
            nameStart++;
        }

        int nameEnd = nameStart;
        while (nameEnd < line.length() && isSymbolChar(line.charAt(nameEnd))) {
            nameEnd++;
        }

        if (nameStart >= nameEnd) {
            return null;
        }

        String aspectName = line.substring(nameStart, nameEnd).trim();

        SymbolNode node = new SymbolNode();
        node.setName(aspectName);
        node.setType(SymbolKind.Module);
        node.setRange(new Range(
            new Position(lineNumber, nameStart),
            new Position(lineNumber, nameEnd)
        ));
        node.setChildren(new ArrayList<>());

        // 添加 AOP 标签
        node.setTags(List.of(SymbolTag.Deprecated));

        return node;
    }

    /**
     * 构建符号层级结构
     *
     * @param nodes 符号节点列表
     * @return 嵌套的符号节点列表
     */
    private List<SymbolNode> buildSymbolHierarchy(List<SymbolNode> nodes) {
        List<SymbolNode> roots = new ArrayList<>();

        for (SymbolNode node : nodes) {
            // 简化实现：所有节点作为根节点
            // 实际实现需要基于缩进或注释进行嵌套
            roots.add(node);
        }

        return roots;
    }

    /**
     * 转换为 DocumentSymbol
     *
     * @param node 符号节点
     * @return DocumentSymbol
     */
    private DocumentSymbol convertToDocumentSymbol(SymbolNode node) {
        DocumentSymbol symbol = new DocumentSymbol();
        symbol.setName(node.getName());
        symbol.setKind(node.getType());
        symbol.setRange(node.getRange());
        symbol.setSelectionRange(node.getRange());
        symbol.setDetail(node.getDetail());

        // 设置子节点
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            symbol.setChildren(
                node.getChildren().stream()
                    .map(this::convertToDocumentSymbol)
                    .toList()
            );
        }

        // 设置 AOP 标签
        if (node.getTags() != null && !node.getTags().isEmpty()) {
            symbol.setTags(node.getTags());
        }

        return symbol;
    }

    /**
     * 检查是否是符号字符
     */
    private boolean isSymbolChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == ':' || c == '.';
    }

    /**
     * 符号节点类
     */
    private static class SymbolNode {
        private String name;
        private SymbolKind type;
        private Range range;
        private String detail;
        private List<SymbolNode> children;
        private List<SymbolTag> tags;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public SymbolKind getType() {
            return type;
        }

        public void setType(SymbolKind type) {
            this.type = type;
        }

        public Range getRange() {
            return range;
        }

        public void setRange(Range range) {
            this.range = range;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public List<SymbolNode> getChildren() {
            return children;
        }

        public void setChildren(List<SymbolNode> children) {
            this.children = children;
        }

        public List<SymbolTag> getTags() {
            return tags;
        }

        public void setTags(List<SymbolTag> tags) {
            this.tags = tags;
        }
    }
}

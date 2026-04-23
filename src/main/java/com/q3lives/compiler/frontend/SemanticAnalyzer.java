// ==================== SemanticAnalyzer.java ====================
package com.q3lives.compiler.frontend;

import com.q3lives.compiler.processors.semantic.TypeProcessor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语义分析器
 *
 * 优化：使用ConcurrentHashMap提高并发性能，添加缓存机制
 */
@Slf4j
public class SemanticAnalyzer {

    // 符号表缓存，避免重复分析
    private static final Map<String, Map<String, SymbolInfo>> symbolTableCache = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> errorCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    private final TypeProcessor typeProcessor;
    private final Map<String, SymbolInfo> symbolTable = new LinkedHashMap<>();
    private final List<String> errors = new ArrayList<>();

    public SemanticAnalyzer(TypeProcessor typeProcessor) {
        this.typeProcessor = typeProcessor;
    }

    /**
     * 对AST进行语义分析
     */
    public SemanticResult analyze(ASTNode ast) {
        log.debug("开始语义分析");

        // 生成缓存key
        String cacheKey = generateCacheKey(ast);

        // 检查缓存
        SemanticResult cached = getCachedAnalysis(cacheKey);
        if (cached != null) {
            log.debug("使用缓存的语义分析结果");
            return cached;
        }

        errors.clear();
        symbolTable.clear();

        long startTime = System.nanoTime();
        collectSymbols(ast);
        validateReferences(ast);
        long elapsed = System.nanoTime() - startTime;

        if (errors.isEmpty()) {
            log.info("语义分析通过: {} 个符号, 耗时: {}ms", symbolTable.size(), elapsed / 1_000_000);
        } else {
            log.warn("语义分析发现 {} 个错误", errors.size());
            errors.forEach(e -> log.warn("  {}", e));
        }

        // 缓存结果
        cacheAnalysis(cacheKey, errors.isEmpty(), errors, symbolTable);

        return new SemanticResult(errors.isEmpty(), errors, symbolTable);
    }

    /**
     * 生成缓存key
     */
    private String generateCacheKey(ASTNode ast) {
        // 使用AST的结构信息生成key
        StringBuilder sb = new StringBuilder();
        generateKeyFromAST(ast, sb, 0);
        return sb.toString();
    }

    /**
     * 从AST生成key
     */
    private void generateKeyFromAST(ASTNode node, StringBuilder sb, int depth) {
        if (depth > 3) return; // 限制深度，避免key过长

        sb.append(node.getType()).append(":");

        // 添加关键属性
        if (node.hasAttribute("name")) {
            sb.append(node.getAttribute("name")).append(",");
        }
        if (node.hasAttribute("type")) {
            sb.append(node.getAttribute("type")).append(",");
        }

        sb.append("|");

        // 递归处理子节点（限制数量）
        int childCount = 0;
        for (ASTNode child : node.getChildren()) {
            if (childCount < 3) { // 只取前3个子节点
                generateKeyFromAST(child, sb, depth + 1);
                childCount++;
            }
        }
    }

    /**
     * 从缓存获取分析结果
     */
    private SemanticResult getCachedAnalysis(String cacheKey) {
        if (cacheKey == null || symbolTableCache.isEmpty()) {
            return null;
        }

        Map<String, SymbolInfo> cachedSymbols = symbolTableCache.get(cacheKey);
        List<String> cachedErrors = errorCache.get(cacheKey);

        if (cachedSymbols != null && cachedErrors != null) {
            return new SemanticResult(cachedErrors.isEmpty(), cachedErrors,
                                   Collections.unmodifiableMap(cachedSymbols));
        }
        return null;
    }

    /**
     * 缓存分析结果
     */
    private void cacheAnalysis(String cacheKey, boolean valid, List<String> errors,
                             Map<String, SymbolInfo> symbolTable) {
        if (cacheKey == null) return;

        // 简单的LRU缓存清理
        if (symbolTableCache.size() >= MAX_CACHE_SIZE) {
            symbolTableCache.clear();
            errorCache.clear();
        }

        // 缓存结果（创建防御性副本）
        symbolTableCache.put(cacheKey, new ConcurrentHashMap<>(symbolTable));
        errorCache.put(cacheKey, new ArrayList<>(errors));
    }

    private void collectSymbols(ASTNode node) {
        switch (node.getType()) {
            case FUNCTION_DECLARATION -> {
                String name = node.getAttribute("name");
                if (name != null) {
                    if (symbolTable.containsKey(name)) {
                        errors.add(String.format("第%d行: 函数 '%s' 重复定义", node.getLine(), name));
                    } else {
                        symbolTable.put(name, new SymbolInfo(name, "function", node));
                    }
                }
            }
            case TYPE_DEFINITION -> {
                String name = node.getAttribute("name");
                if (name != null) {
                    typeProcessor.registerCustomType(name);
                    symbolTable.put(name, new SymbolInfo(name, "type", node));
                }
            }
            case VARIABLE_DECLARATION -> {
                String name = node.getAttribute("name");
                if (name != null) {
                    symbolTable.put(name, new SymbolInfo(name, "variable", node));
                }
            }
            default -> {}
        }

        for (ASTNode child : node.getChildren()) {
            collectSymbols(child);
        }
    }

    private void validateReferences(ASTNode node) {
        if (node.getType() == ASTNode.NodeType.FUNCTION_CALL) {
            String name = node.getAttribute("name");
            if (name != null && !symbolTable.containsKey(name)) {
                // 允许内置函数
                log.debug("函数调用引用: {} (可能是内置函数)", name);
            }
        }

        for (ASTNode child : node.getChildren()) {
            validateReferences(child);
        }
    }

    public record SymbolInfo(String name, String kind, ASTNode declarationNode) {}

    public record SemanticResult(boolean valid, List<String> errors, 
                                  Map<String, SymbolInfo> symbolTable) {}
}

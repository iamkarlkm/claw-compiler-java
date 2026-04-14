// ==================== SemanticAnalyzer.java ====================
package com.claw.compiler.frontend;

import com.claw.compiler.processors.semantic.TypeProcessor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 语义分析器
 */
@Slf4j
public class SemanticAnalyzer {

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
        errors.clear();
        symbolTable.clear();

        collectSymbols(ast);
        validateReferences(ast);

        if (errors.isEmpty()) {
            log.info("语义分析通过: {} 个符号", symbolTable.size());
        } else {
            log.warn("语义分析发现 {} 个错误", errors.size());
            errors.forEach(e -> log.warn("  {}", e));
        }

        return new SemanticResult(errors.isEmpty(), errors, symbolTable);
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

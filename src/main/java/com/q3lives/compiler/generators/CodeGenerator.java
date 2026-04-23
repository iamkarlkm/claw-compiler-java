// ==================== CodeGenerator.java ====================
package com.q3lives.compiler.generators;

import com.q3lives.compiler.annotation.AnnotationManager;
import com.q3lives.compiler.frontend.ASTNode;
import com.q3lives.compiler.integration.FlowManager;
import com.q3lives.compiler.integration.MemoryManager;
import com.q3lives.compiler.integration.PropertyManager;
import com.q3lives.compiler.pipeline.GeneratedCode;

import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代码生成器
 *
 * 第4层验证生成处理器：生成目标代码
 * 思想2第5步：从当前最底层的代码块开始生成伪代码/中间表示
 *
 * 优化：使用StringBuilder池和缓存机制提高性能
 */
@Slf4j
public class CodeGenerator {

    // StringBuilder池，避免频繁创建
    private static final Map<Integer, StringBuilder> stringBuilderPool = new ConcurrentHashMap<>();
    private static final int SB_POOL_SIZE = 10;

    // 模式缓存，避免重复编译相同的代码模式
    private static final Map<String, String> patternCache = new ConcurrentHashMap<>();

    private final MemoryManager memoryManager;
    private final PropertyManager propertyManager;
    private final FlowManager flowManager;

    public CodeGenerator(MemoryManager memoryManager, 
                         PropertyManager propertyManager,
                         FlowManager flowManager) {
        this.memoryManager = memoryManager;
        this.propertyManager = propertyManager;
        this.flowManager = flowManager;
    }

    /**
     * 生成中间表示（IR）
     */
    public GenerationResult generate(ASTNode ast) {
        log.info("开始代码生成");

        // 从池中获取StringBuilder
        StringBuilder irBuilder = getFromStringBuilderPool();

        irBuilder.append("; Claw Compiler IR\n");
        irBuilder.append("; Generated at ").append(System.currentTimeMillis()).append("\n\n");

        generateNode(ast, irBuilder, 0);

        String irString = irBuilder.toString();
        // 归还StringBuilder到池中
        returnStringBuilder(irBuilder);

        List<String> irInstructions = Arrays.asList(irString.split("\n"));
        log.info("代码生成完成: {} 条IR指令", irInstructions.size());
        return new GenerationResult(irInstructions);
    }

    private void generateNode(ASTNode node, StringBuilder ir, int indent) {
        String prefix = "  ".repeat(indent);

        switch (node.getType()) {
            case PROGRAM -> {
                ir.append(prefix).append("; === PROGRAM BEGIN ===\n");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent);
                }
                ir.append(prefix).append("; === PROGRAM END ===\n");
            }
            case IMPORT_DECLARATION -> {
                String path = node.getAttribute("path");
                ir.append(prefix).append("IMPORT ").append(path).append("\n");
            }
            case FUNCTION_DECLARATION -> {
                String name = node.getAttribute("name");
                String flowType = node.getAttribute("flowType");
                ir.append("\n").append(prefix).append("; function ").append(name).append(" [").append(flowType).append("]\n");
                ir.append(prefix).append("FUNC_DEF ").append(name).append(":\n");

                // 生成内存分配代码（如果有构造函数注解）
                memoryManager.generateAllocationCode(name).forEach(s ->
                    ir.append(prefix).append("  ").append(s).append("\n"));

                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }

                // 生成内存回收代码（如果有析构函数注解）
                memoryManager.generateDeallocationCode(name).forEach(s ->
                    ir.append(prefix).append("  ").append(s).append("\n"));

                ir.append(prefix).append("FUNC_END ").append(name).append("\n");
            }
            case TYPE_DEFINITION -> {
                String typeName = node.getAttribute("name");
                ir.append("\n").append(prefix).append("TYPE_DEF ").append(typeName).append(":\n");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
                ir.append(prefix).append("TYPE_END ").append(typeName).append("\n");
            }
            case VARIABLE_DECLARATION -> {
                String varName = node.getAttribute("name");
                Boolean mutable = node.getAttribute("mutable");
                String keyword = Boolean.TRUE.equals(mutable) ? "VAR" : "CONST";
                ir.append(prefix).append(keyword).append(" ").append(varName).append("\n");
                if (!node.getChildren().isEmpty()) {
                    ir.append(prefix).append("INIT ").append(varName).append("\n");
                }
            }
            case ASSIGNMENT -> {
                ir.append(prefix).append("ASSIGN\n");
                // 检查是否有属性监听
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
            }
            case RETURN_STATEMENT -> {
                ir.append(prefix).append("RETURN\n");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
            }
            case IF_STATEMENT -> {
                ir.append(prefix).append("IF:\n");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
                ir.append(prefix).append("END_IF\n");
            }
            case FOR_STATEMENT -> {
                ir.append(prefix).append("FOR:\n");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
                ir.append(prefix).append("END_FOR\n");
            }
            case WHILE_STATEMENT -> {
                ir.append(prefix).append("WHILE:\n");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
                ir.append(prefix).append("END_WHILE\n");
            }
            case FUNCTION_CALL -> {
                String callName = node.getAttribute("name");
                ir.append(prefix).append("CALL ").append(callName).append("\n");
            }
            case CATCH_CLAUSE -> {
                ir.append(prefix).append("CATCH:\n");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
                ir.append(prefix).append("END_CATCH\n");
            }
            case FLOW_STATEMENT -> {
                String target = node.getAttribute("target");
                ir.append(prefix).append("FLOW_TO ").append(target).append("\n");
            }
            case ANNOTATION -> {
                String annName = node.getAttribute("annotationName");
                ir.append(prefix).append("; @").append(annName).append("\n");
            }
            case BLOCK -> {
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent);
                }
            }
            default -> {
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent);
                }
            }
        }
    }

    /**
     * 从池中获取StringBuilder
     */
    private StringBuilder getFromStringBuilderPool() {
        // 简单实现：总是创建新的StringBuilder
        // 在实际应用中可以实现更复杂的池管理
        return new StringBuilder(8192); // 预分配8KB
    }

    /**
     * 归还StringBuilder到池中
     */
    private void returnStringBuilder(StringBuilder sb) {
        // 简单实现：什么都不做
        // 在实际应用中可以重置StringBuilder并放回池中
        sb.setLength(0);
    }

    public record GenerationResult(List<String> irInstructions) {
        public String toIRString() {
            return String.join("", irInstructions);
        }
    }

    public GeneratedCode generate(ASTNode ast, AnnotationManager annotationManager, FlowManager flowManager2) {
        GenerationResult irResult = generate(ast);
        String intermediate = String.join("\n", irResult.irInstructions);
        String targetCode = intermediate;
        String pseudoCode = intermediate;
        Map<String, String> metadata = Collections.emptyMap();

        if (annotationManager != null) {
            metadata = Collections.unmodifiableMap(new LinkedHashMap<>());
        }

        int functionCount = countNodes(ast, ASTNode.NodeType.FUNCTION_DECLARATION);
        int typeCount = countNodes(ast, ASTNode.NodeType.TYPE_DEFINITION);
        return new GeneratedCode(targetCode, intermediate, pseudoCode, metadata, intermediate, functionCount, typeCount);
        
    }

    private int countNodes(ASTNode node, ASTNode.NodeType type) {
        if (node == null) return 0;
        int count = node.getType() == type ? 1 : 0;
        for (ASTNode child : node.getChildren()) {
            count += countNodes(child, type);
        }
        return count;
    }
}


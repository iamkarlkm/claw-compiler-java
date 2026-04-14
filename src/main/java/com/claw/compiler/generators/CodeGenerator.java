// ==================== CodeGenerator.java ====================
package com.claw.compiler.generators;

import com.claw.compiler.annotation.AnnotationManager;
import com.claw.compiler.frontend.ASTNode;
import com.claw.compiler.integration.FlowManager;
import com.claw.compiler.integration.MemoryManager;
import com.claw.compiler.integration.PropertyManager;
import com.claw.compiler.pipeline.GeneratedCode;

import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 代码生成器
 * 
 * 第4层验证生成处理器：生成目标代码
 * 思想2第5步：从当前最底层的代码块开始生成伪代码/中间表示
 */
@Slf4j
public class CodeGenerator {

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

        List<String> irInstructions = new ArrayList<>();
        irInstructions.add("; Claw Compiler IR");
        irInstructions.add("; Generated at " + System.currentTimeMillis());
        irInstructions.add("");

        generateNode(ast, irInstructions, 0);

        log.info("代码生成完成: {} 条IR指令", irInstructions.size());
        return new GenerationResult(irInstructions);
    }

    private void generateNode(ASTNode node, List<String> ir, int indent) {
        String prefix = "  ".repeat(indent);

        switch (node.getType()) {
            case PROGRAM -> {
                ir.add(prefix + "; === PROGRAM BEGIN ===");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent);
                }
                ir.add(prefix + "; === PROGRAM END ===");
            }
            case IMPORT_DECLARATION -> {
                String path = node.getAttribute("path");
                ir.add(prefix + "IMPORT " + path);
            }
            case FUNCTION_DECLARATION -> {
                String name = node.getAttribute("name");
                String flowType = node.getAttribute("flowType");
                ir.add("");
                ir.add(prefix + "; function " + name + " [" + flowType + "]");
                ir.add(prefix + "FUNC_DEF " + name + ":");

                // 生成内存分配代码（如果有构造函数注解）
                ir.addAll(memoryManager.generateAllocationCode(name).stream()
                        .map(s -> prefix + "  " + s).toList());

                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }

                // 生成内存回收代码（如果有析构函数注解）
                ir.addAll(memoryManager.generateDeallocationCode(name).stream()
                        .map(s -> prefix + "  " + s).toList());

                ir.add(prefix + "FUNC_END " + name);
            }
            case TYPE_DEFINITION -> {
                String typeName = node.getAttribute("name");
                ir.add("");
                ir.add(prefix + "TYPE_DEF " + typeName + ":");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
                ir.add(prefix + "TYPE_END " + typeName);
            }
            case VARIABLE_DECLARATION -> {
                String varName = node.getAttribute("name");
                Boolean mutable = node.getAttribute("mutable");
                String keyword = Boolean.TRUE.equals(mutable) ? "VAR" : "CONST";
                ir.add(prefix + keyword + " " + varName);
                if (!node.getChildren().isEmpty()) {
                    ir.add(prefix + "INIT " + varName);
                }
            }
            case ASSIGNMENT -> {
                ir.add(prefix + "ASSIGN");
                // 检查是否有属性监听
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
            }
            case RETURN_STATEMENT -> {
                ir.add(prefix + "RETURN");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
            }
            case IF_STATEMENT -> {
                ir.add(prefix + "IF:");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
                ir.add(prefix + "END_IF");
            }
            case FOR_STATEMENT -> {
                ir.add(prefix + "FOR:");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
                ir.add(prefix + "END_FOR");
            }
            case WHILE_STATEMENT -> {
                ir.add(prefix + "WHILE:");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
                ir.add(prefix + "END_WHILE");
            }
            case FUNCTION_CALL -> {
                String callName = node.getAttribute("name");
                ir.add(prefix + "CALL " + callName);
            }
            case CATCH_CLAUSE -> {
                ir.add(prefix + "CATCH:");
                for (ASTNode child : node.getChildren()) {
                    generateNode(child, ir, indent + 1);
                }
                ir.add(prefix + "END_CATCH");
            }
            case FLOW_STATEMENT -> {
                String target = node.getAttribute("target");
                ir.add(prefix + "FLOW_TO " + target);
            }
            case ANNOTATION -> {
                String annName = node.getAttribute("annotationName");
                ir.add(prefix + "; @" + annName);
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

        return GeneratedCode.builder()
                .targetCode(targetCode)
                .intermediateRepresentation(intermediate)
                .pseudoCode(pseudoCode)
                .metadata(metadata)
                .sourceFileName("unknown")
                .functionCount(functionCount)
                .typeCount(typeCount)
                .build();
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


// ==================== NormalFlow.java ====================
package com.claw.compiler.flow;

import com.claw.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 正常流处理器
 * 
 * 思想1：normal（正常流）- 标准执行路径
 */
@Slf4j
public class NormalFlow {

    /**
     * 处理正常流函数
     */
    public FlowResult processNormal(ASTNode functionNode) {
        log.debug("处理正常流函数: {}", functionNode.getAttribute("name").toString());

        List<String> instructions = new ArrayList<>();
        generateInstructions(functionNode, instructions);

        return new FlowResult("normal", instructions);
    }

    private void generateInstructions(ASTNode node, List<String> instructions) {
        switch (node.getType()) {
            case FUNCTION_DECLARATION -> {
                String name = node.getAttribute("name");
                instructions.add("FUNC_BEGIN " + name);
                for (ASTNode child : node.getChildren()) {
                    generateInstructions(child, instructions);
                }
                instructions.add("FUNC_END " + name);
            }
            case VARIABLE_DECLARATION -> {
                String varName = node.getAttribute("name");
                instructions.add("VAR_DECL " + varName);
            }
            case ASSIGNMENT -> {
                instructions.add("ASSIGN");
            }
            case RETURN_STATEMENT -> {
                instructions.add("RETURN");
            }
            case FUNCTION_CALL -> {
                String callName = node.getAttribute("name");
                instructions.add("CALL " + callName);
            }
            default -> {
                for (ASTNode child : node.getChildren()) {
                    generateInstructions(child, instructions);
                }
            }
        }
    }

    public record FlowResult(String flowType, List<String> instructions) {}
}

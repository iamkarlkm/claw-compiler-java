// ==================== BusinessFlow.java ====================
package com.q3lives.compiler.flow;

import com.q3lives.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 业务逻辑流转处理器
 * 
 * 思想1：flow（业务逻辑流转）
 * - 控制流跳转，不记录堆栈信息
 * - 直接向后向上跳转
 * - flow to target 语法
 */
@Slf4j
public class BusinessFlow {


    /**
     * 处理业务流
     */
    public FlowResult processFlow(ASTNode functionNode) {
        log.debug("处理业务流函数: {}", functionNode.getAttribute("name").toString());

        List<String> instructions = new ArrayList<>();
        List<FlowTarget> targets = new ArrayList<>();

        generateFlowInstructions(functionNode, instructions, targets);

        return new FlowResult("flow", instructions, targets);
    }

    private void generateFlowInstructions(ASTNode node, List<String> instructions,
                                           List<FlowTarget> targets) {
        switch (node.getType()) {
            case FUNCTION_DECLARATION -> {
                String name = node.getAttribute("name");
                instructions.add("FLOW_FUNC_BEGIN " + name);
                for (ASTNode child : node.getChildren()) {
                    generateFlowInstructions(child, instructions, targets);
                }
                instructions.add("FLOW_FUNC_END " + name);
            }
            case FLOW_STATEMENT -> {
                String target = node.getAttribute("target");
                if (target != null) {
                    targets.add(new FlowTarget(target, node.getLine()));
                    // 直接跳转，不记录堆栈
                    instructions.add("FLOW_JUMP " + target);
                }
            }
            default -> {
                for (ASTNode child : node.getChildren()) {
                    generateFlowInstructions(child, instructions, targets);
                }
            }
        }
    }

    public record FlowResult(String flowType, List<String> instructions, List<FlowTarget> targets) {}
    public record FlowTarget(String name, int line) {}
}

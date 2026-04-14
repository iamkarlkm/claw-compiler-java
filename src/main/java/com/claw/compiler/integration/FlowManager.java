// ==================== FlowManager.java ====================
package com.claw.compiler.integration;

import com.claw.compiler.flow.*;
import com.claw.compiler.frontend.ASTNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 操作流管理器 - 协调三层操作流
 * 
 * 思想1：normal / exception / flow 三层操作流模型
 */
@Slf4j
public class FlowManager {

    private final NormalFlow normalFlow = new NormalFlow();
    private final ExceptionFlow exceptionFlow = new ExceptionFlow();
    private final BusinessFlow businessFlow = new BusinessFlow();

    @Getter
    private final List<Object> flowResults = new ArrayList<>();

    /**
     * 处理函数的操作流
     */
    public void processFunction(ASTNode functionNode) {
        String flowType = functionNode.getAttribute("flowType");
        String funcName = functionNode.getAttribute("name");

        if (flowType == null) flowType = "normal";

        log.debug("处理函数 {} 的 {} 流", funcName, flowType);

        Object result = switch (flowType) {
            case "normal" -> normalFlow.processNormal(functionNode);
            case "exception" -> exceptionFlow.processException(functionNode);
            case "flow" -> businessFlow.processFlow(functionNode);
            default -> normalFlow.processNormal(functionNode);
        };

        flowResults.add(result);
    }

    /**
     * 处理AST中所有函数的操作流
     */
    public void processAllFunctions(ASTNode ast) {
        processNode(ast);
        log.info("操作流处理完成: {} 个函数", flowResults.size());
    }

    private void processNode(ASTNode node) {
        if (node.getType() == ASTNode.NodeType.FUNCTION_DECLARATION) {
            processFunction(node);
        }
        for (ASTNode child : node.getChildren()) {
            processNode(child);
        }
    }
}

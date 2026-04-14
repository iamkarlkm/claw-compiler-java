// ==================== ExceptionFlow.java ====================
package com.claw.compiler.flow;

import com.claw.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 异常流处理器
 * 
 * 思想1：exception（异常流）
 * - 去掉try和{}，保留catch和throws
 * - catch与当前代码块边界一致
 * - 不生成堆栈信息，提高性能
 */
@Slf4j
public class ExceptionFlow {

    /**
     * 处理异常流
     */
    public FlowResult processException(ASTNode functionNode) {
        log.debug("处理异常流函数: {}", functionNode.getAttribute("name").toString());

        List<String> instructions = new ArrayList<>();
        List<CatchInfo> catches = new ArrayList<>();

        generateExceptionInstructions(functionNode, instructions, catches);

        return new FlowResult("exception", instructions, catches);
    }

    private void generateExceptionInstructions(ASTNode node, List<String> instructions,
                                                List<CatchInfo> catches) {
        switch (node.getType()) {
            case FUNCTION_DECLARATION -> {
                String name = node.getAttribute("name");
                instructions.add("EXCEPTION_FUNC_BEGIN " + name);
                // 异常流：整个函数体是一个隐式的异常边界
                instructions.add("EXCEPTION_BOUNDARY_BEGIN");
                for (ASTNode child : node.getChildren()) {
                    generateExceptionInstructions(child, instructions, catches);
                }
                instructions.add("EXCEPTION_BOUNDARY_END");
                instructions.add("EXCEPTION_FUNC_END " + name);
            }
            case CATCH_CLAUSE -> {
                // catch与当前代码块边界一致，不需要try
                catches.add(new CatchInfo(node.getLine()));
                instructions.add("CATCH_BEGIN");
                for (ASTNode child : node.getChildren()) {
                    generateExceptionInstructions(child, instructions, catches);
                }
                instructions.add("CATCH_END");
            }
            default -> {
                for (ASTNode child : node.getChildren()) {
                    generateExceptionInstructions(child, instructions, catches);
                }
            }
        }
    }

    public record FlowResult(String flowType, List<String> instructions, List<CatchInfo> catches) {}
    public record CatchInfo(int line) {}
}

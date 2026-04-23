// ==================== AOPBlockProcessor.java ====================
package com.q3lives.compiler.processors.blocks;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.hierarchy.BlockType;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * AOP（面向切面编程）代码块处理器
 *
 * <p>处理 AOP 相关的代码块类型，包括切面定义和通知块。</p>
 *
 * <h2>支持的块类型</h2>
 * <ul>
 *   <li>ASPECT_DEFINITION_BLOCK - 切面定义块</li>
 *   <li>ADVICE_BLOCK - 通知块</li>
 * </ul>
 *
 * <h2>处理逻辑</h2>
 * <ul>
 *   <li>将 AOP 代码块转换为 AST 节点</li>
 *   <li>提取切面名称和通知信息</li>
 *   <li>处理子通知块</li>
 * </ul>
 *
 * @author Claw Compiler Team
 * @since 3.0.0
 */
@Slf4j
public class AOPBlockProcessor extends BlockProcessor {


    /**
     * 创建新的 AOP 块处理器
     */
    public AOPBlockProcessor() {
        log.debug("初始化 AOP 块处理器");
    }

    /**
     * 判断该处理器是否能处理指定类型的代码块
     *
     * @param block 要检查的代码块
     * @return 如果能处理返回 true
     */
    @Override
    public boolean canProcess(CodeBlock block) {
        return getSupportedBlockTypes().contains(block.getBlockType());
    }

    /**
     * 获取该处理器支持的块类型集合
     *
     * @return 支持的块类型集合
     */
    @Override
    public Set<BlockType> getSupportedBlockTypes() {
        return Set.of(
            BlockType.ASPECT_DEFINITION_BLOCK,
            BlockType.ADVICE_BLOCK
        );
    }

    /**
     * 实际处理逻辑
     *
     * @param block 要处理的代码块
     * @param tokens Token 列表
     * @return 生成的 AST 节点
     */
    @Override
    protected ASTNode doProcess(CodeBlock block, List<Token> tokens) {
        try {
            log.debug("处理 AOP 代码块: {}", block.getBlockType());

            switch (block.getBlockType()) {
                case ASPECT_DEFINITION_BLOCK -> {
                    return processAspectDefinition(block, tokens);
                }
                case ADVICE_BLOCK -> {
                    return processAdviceBlock(block, tokens);
                }
                default -> {
                    throw new ProcessingException(
                        "未知的 AOP 块类型: " + block.getBlockType(),
                        block.getStartLine(),
                        block.getBlockType()
                    );
                }
            }
        } catch (ProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessingException(
                "处理 AOP 块时发生错误: " + block.getBlockType(),
                block.getStartLine(),
                e
            );
        }
    }

    /**
     * 处理切面定义块
     *
     * <p>提取切面名称和相关属性。</p>
     *
     * @param block 切面定义块
     * @param tokens Token 列表
     * @return AST 节点
     */
    private ASTNode processAspectDefinition(CodeBlock block, List<Token> tokens) {
        log.debug("处理切面定义块");

        ASTNode aspectNode = new ASTNode(
            ASTNode.NodeType.AOP_ASPECT,
            block.getStartLine()
        );

        // 设置切面名称
        String aspectName = block.getAttribute("name");
        if (aspectName != null) {
            aspectNode.setAttribute("name", aspectName);
        }

        // 设置可见性
        String visibility = block.getAttribute("visibility");
        if (visibility != null) {
            aspectNode.setAttribute("visibility", visibility);
        }

        // 设置注解
        List<String> annotations = block.getAttribute("annotations");
        if (annotations != null && !annotations.isEmpty()) {
            aspectNode.setAttribute("annotations", annotations);
        }

        // 处理子块（通知块）
        processChildren(aspectNode, block.getChildren(), tokens);

        log.debug("完成切面定义块处理: {}", aspectName);
        return aspectNode;
    }

    /**
     * 处理通知块
     *
     * <p>提取通知类型、切面名称和相关属性。</p>
     *
     * @param block 通知块
     * @param tokens Token 列表
     * @return AST 节点
     */
    private ASTNode processAdviceBlock(CodeBlock block, List<Token> tokens) {
        log.debug("处理通知块");

        ASTNode adviceNode = new ASTNode(
            ASTNode.NodeType.AOP_ADVICE,
            block.getStartLine()
        );

        // 设置通知类型
        String adviceType = block.getAttribute("adviceType");
        if (adviceType != null) {
            adviceNode.setAttribute("adviceType", adviceType);
        }

        // 设置切面名称
        String aspectName = block.getAttribute("aspectName");
        if (aspectName != null) {
            adviceNode.setAttribute("aspectName", aspectName);
        }

        // 设置注解
        String annotation = block.getAttribute("annotation");
        if (annotation != null) {
            adviceNode.setAttribute("annotation", annotation);
        }

        // 设置切入点表达式
        String pointcut = block.getAttribute("pointcut");
        if (pointcut != null) {
            adviceNode.setAttribute("pointcut", pointcut);
        }

        // 设置目标方法
        String targetMethod = block.getAttribute("targetMethod");
        if (targetMethod != null) {
            adviceNode.setAttribute("targetMethod", targetMethod);
        }

        // 设置返回值变量
        String returnVar = block.getAttribute("returnVar");
        if (returnVar != null) {
            adviceNode.setAttribute("returnVar", returnVar);
        }

        // 设置异常变量
        String exceptionVar = block.getAttribute("exceptionVar");
        if (exceptionVar != null) {
            adviceNode.setAttribute("exceptionVar", exceptionVar);
        }

        // 处理子块
        processChildren(adviceNode, block.getChildren(), tokens);

        log.debug("完成通知块处理: {}", adviceType);
        return adviceNode;
    }

    /**
     * 解析切入点表达式
     *
     * <p>简单的切入点表达式解析，提取方法名和参数信息。</p>
     *
     * @param pointcutExpression 切入点表达式
     * @return 解析结果，如果解析失败返回 null
     */
    public static Map<String, String> parsePointcutExpression(String pointcutExpression) {
        if (pointcutExpression == null || pointcutExpression.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new LinkedHashMap<>();

        try {
            // 提取方法名（简化版）
            int parenthesis = pointcutExpression.indexOf('(');
            if (parenthesis > 0) {
                String methodPart = pointcutExpression.substring(0, parenthesis).trim();
                // 移除 execution(* ... 或 args(...)
                if (methodPart.startsWith("execution(*") || methodPart.startsWith("args(")) {
                    methodPart = methodPart.substring(methodPart.indexOf('.') + 1);
                }
                result.put("method", methodPart);
            }

            // 提取参数（简化版）
            if (parenthesis > 0) {
                int closeParenthesis = pointcutExpression.indexOf(')', parenthesis);
                if (closeParenthesis > parenthesis) {
                    String argsPart = pointcutExpression.substring(parenthesis + 1, closeParenthesis).trim();
                    result.put("args", argsPart);
                }
            }

        } catch (Exception e) {
            log.warn("解析切入点表达式失败: {}", pointcutExpression, e);
        }

        return result;
    }

    /**
     * 判断通知类型
     *
     * @param annotation 注解名称
     * @return 通知类型
     */
    public static String getAdviceType(String annotation) {
        if (annotation == null) return null;

        return switch (annotation) {
            case "@Before" -> "Before";
            case "@After" -> "After";
            case "@AfterReturning" -> "AfterReturning";
            case "@AfterThrowing" -> "AfterThrowing";
            case "@Around" -> "Around";
            default -> null;
        };
    }
}

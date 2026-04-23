package claw.compiler.example;

import claw.compiler.context.StructureContext;
import claw.compiler.context.SemanticContext;
import claw.compiler.hierarchy.CodeBlock;
import claw.compiler.hierarchy.BlockType;
import claw.compiler.processors.blocks.AOPBlockProcessor;
import claw.compiler.processors.semantic.TypeProcessor;
import com.claw.compiler.processors.blocks.BlockProcessor;

import java.util.*;

/**
 * AOP 代码块处理器集成示例
 *
 * 展示如何使用 AOPBlockProcessor 处理 AOP 相关的代码块
 */
public class AOPIntegrationExample {

    public static void main(String[] args) {
        System.out.println("=== AOP 代码块处理器集成示例 ===");
        System.out.println();

        // 1. 创建代码块处理器
        AOPBlockProcessor aopBlockProcessor = new AOPBlockProcessor();
        System.out.println("✅ AOP 块处理器已创建");

        // 2. 测试支持的块类型
        System.out.println("\n支持的块类型:");
        for (BlockType type : aopBlockProcessor.getSupportedBlockTypes()) {
            System.out.println("  - " + type.name() + " (" + type.getDescription() + ")");
        }

        // 3. 创建模拟的代码块
        System.out.println("\n创建模拟的 AOP 代码块...");

        // 3.1 创建切面定义块
        CodeBlock aspectBlock = createMockAspectBlock();
        System.out.println("✅ 切面定义块已创建");

        // 3.2 创建通知块
        CodeBlock adviceBlock = createMockAdviceBlock();
        System.out.println("✅ 通知块已创建");

        // 4. 测试切面定义块处理
        System.out.println("\n处理切面定义块:");
        BlockProcessor mockTokenList = new BlockProcessor() {
            @Override
            public boolean canProcess(CodeBlock block) {
                return true;
            }

            @Override
            public Set<BlockType> getSupportedBlockTypes() {
                return Collections.emptySet();
            }

            @Override
            protected com.claw.compiler.frontend.ASTNode doProcess(CodeBlock block, List<Object> tokens) {
                return null;
            }
        };

        try {
            // 创建一个简单的 Token 列表用于测试
            List<Object> mockTokens = new ArrayList<>();
            com.claw.compiler.frontend.ASTNode aspectNode = aopBlockProcessor.process(aspectBlock, mockTokens);
            if (aspectNode != null) {
                System.out.println("  ✓ 切面定义块处理成功");
                System.out.println("    - 切面名称: " + aspectNode.getAttribute("name"));
                System.out.println("    - 可见性: " + aspectNode.getAttribute("visibility"));
                System.out.println("    - 子块数量: " + aspectNode.getChildren().size());
            } else {
                System.out.println("  ✗ 切面定义块处理失败");
            }
        } catch (Exception e) {
            System.out.println("  ✗ 处理错误: " + e.getMessage());
        }

        // 5. 测试通知块处理
        System.out.println("\n处理通知块:");
        try {
            List<Object> mockTokens2 = new ArrayList<>();
            com.claw.compiler.frontend.ASTNode adviceNode = aopBlockProcessor.process(adviceBlock, mockTokens2);
            if (adviceNode != null) {
                System.out.println("  ✓ 通知块处理成功");
                System.out.println("    - 通知类型: " + adviceNode.getAttribute("adviceType"));
                System.out.println("    - 切面名称: " + adviceNode.getAttribute("aspectName"));
                System.out.println("    - 注解: " + adviceNode.getAttribute("annotation"));
                System.out.println("    - 目标方法: " + adviceNode.getAttribute("targetMethod"));
                System.out.println("    - 子块数量: " + adviceNode.getChildren().size());
            } else {
                System.out.println("  ✗ 通知块处理失败");
            }
        } catch (Exception e) {
            System.out.println("  ✗ 处理错误: " + e.getMessage());
        }

        // 6. 测试切入点表达式解析
        System.out.println("\n测试切入点表达式解析:");
        String[] testExpressions = {
            "execution(* calculate(..))",
            "execution(public * com.example.Service.*(..))",
            "args(String)"
        };

        for (String expr : testExpressions) {
            Map<String, String> parsed = AOPBlockProcessor.parsePointcutExpression(expr);
            System.out.println("  - " + expr);
            System.out.println("    解析结果: " + parsed);
        }

        // 7. 测试通知类型判断
        System.out.println("\n测试通知类型判断:");
        String[] testAnnotations = {"@Before", "@After", "@AfterReturning", "@AfterThrowing", "@Around", "@Unknown"};
        for (String ann : testAnnotations) {
            String adviceType = AOPBlockProcessor.getAdviceType(ann);
            System.out.println("  - " + ann + " -> " + (adviceType != null ? adviceType : "null"));
        }

        System.out.println("\n=== 所有测试完成 ===");
    }

    /**
     * 创建模拟的切面定义块
     */
    private static CodeBlock createMockAspectBlock() {
        CodeBlock block = new CodeBlock("aspect1", "aspect", 1, 10, 0);
        block.setAttribute("name", "Logging");
        block.setAttribute("visibility", "public");
        block.setAttribute("annotations", Arrays.asList("@Aspect"));

        // 添加通知块作为子块
        CodeBlock beforeAdvice = new CodeBlock("advice1", "advice", 5, 8, 1);
        beforeAdvice.setAttribute("adviceType", "Before");
        beforeAdvice.setAttribute("aspectName", "Logging");
        beforeAdvice.setAttribute("annotation", "@Before");
        beforeAdvice.setAttribute("pointcut", "execution(* *(..))");
        block.addChild(beforeAdvice);

        return block;
    }

    /**
     * 创建模拟的通知块
     */
    private static CodeBlock createMockAdviceBlock() {
        CodeBlock block = new CodeBlock("advice2", "advice", 12, 15, 0);
        block.setAttribute("adviceType", "Around");
        block.setAttribute("aspectName", "Transaction");
        block.setAttribute("annotation", "@Around");
        block.setAttribute("pointcut", "execution(* *.save*(..))");
        block.setAttribute("targetMethod", "saveData");
        block.setAttribute("returnVar", "returnValue");
        block.setAttribute("exceptionVar", "ex");

        return block;
    }
}

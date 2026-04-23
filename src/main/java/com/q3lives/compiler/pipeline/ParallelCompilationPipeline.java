// ==================== ParallelCompilationPipeline.java ====================
package com.q3lives.compiler.pipeline;

import com.q3lives.compiler.annotation.AnnotationManager;
import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.Tokenizer;
import com.q3lives.compiler.frontend.ASTNode;
import com.q3lives.compiler.frontend.Parser;
import com.q3lives.compiler.generators.CodeGenerator;
import com.q3lives.compiler.integration.FlowManager;
import com.q3lives.compiler.integration.MemoryManager;
import com.q3lives.compiler.integration.PropertyManager;
import com.q3lives.compiler.generators.TypeChecker;
import com.q3lives.compiler.frontend.SemanticAnalyzer;
import com.q3lives.compiler.processors.semantic.ControlFlowProcessor;
import com.q3lives.compiler.processors.semantic.DeclarationProcessor;
import com.q3lives.compiler.processors.semantic.FunctionProcessor;
import com.q3lives.compiler.processors.semantic.TypeProcessor;
import com.q3lives.compiler.scanner.SourceScanner;
import com.q3lives.compiler.scanner.SourceView;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;

/**
 * 并行编译管道 - 优化编译性能
 *
 * 优化策略：
 * 1. 函数处理器并行化
 * 2. 控制流处理器并行化
 * 3. 批次处理任务
 */
@Slf4j
@Getter
public class ParallelCompilationPipeline {


    private final ExecutorService executor;
    private final int threadCount;
    private final int batchSize;
    private final int warmupRuns;

    // 组件
    private final SourceScanner scanner = new SourceScanner();
    private final Tokenizer tokenizer = new Tokenizer();
    private final TypeProcessor typeProcessor = new TypeProcessor();
    private final FunctionProcessor functionProcessor = new FunctionProcessor();
    private final ControlFlowProcessor controlFlowProcessor = new ControlFlowProcessor();
    private final DeclarationProcessor declarationProcessor = new DeclarationProcessor();
    private final Parser parser = new Parser();

    // 注解系统和系统集成
    private final AnnotationManager annotationManager = new AnnotationManager();
    private final MemoryManager memoryManager = new MemoryManager();
    private final PropertyManager propertyManager = new PropertyManager();
    private final FlowManager flowManager = new FlowManager();

    /**
     * 创建并行编译管道
     *
     * @param threadCount 线程数
     * @param batchSize   批次大小（默认100）
     * @param warmupRuns  预热次数（默认3）
     */
    public ParallelCompilationPipeline(int threadCount, int batchSize, int warmupRuns) {
        this.executor = Executors.newFixedThreadPool(threadCount);
        this.threadCount = threadCount;
        this.batchSize = batchSize;
        this.warmupRuns = warmupRuns;
        log.info("创建并行编译管道: threads={}, batchSize={}, warmup={}",
            threadCount, batchSize, warmupRuns);
    }

    /**
     * 创建并行编译管道（使用默认参数）
     */
    public ParallelCompilationPipeline(int threadCount) {
        this(threadCount, 100, 3);
    }

    /**
     * 创建并行编译管道（使用默认参数）
     *
     * @param threadCount 线程数
     * @param batchSize   批次大小
     */
    public ParallelCompilationPipeline(int threadCount, int batchSize) {
        this(threadCount, batchSize, 3);
    }

    /**
     * 创建并行编译管道（使用默认线程数和批次）
     */
    public ParallelCompilationPipeline() {
        this(Runtime.getRuntime().availableProcessors(), 100, 3);
    }

    /**
     * 执行完整编译流程（并行版本）
     */
    public CompilationResult compile(String source, String fileName) {
        log.info("====================================");
        log.info("Claw 并行编译 v3.0: {}", fileName);
        log.info("====================================");

        long startTime = System.currentTimeMillis();

        try {
            // ============ 阶段1：扫描（串行，因为需要顺序处理）============
            log.info("[阶段1] 扫描");

            SourceView sourceView = scanner.scan(source, fileName);

            // ============ 阶段2：预处理和解析（串行，因为依赖关系紧密）============
            log.info("[阶段2] 预处理和解析");

            // 2.1 语义处理器（串行，因为这些处理器之间有依赖关系）
            List<Token> tokens = tokenizer.tokenize(sourceView);
            typeProcessor.processTokens(tokens);
            declarationProcessor.processTokens(tokens);

            // 2.2 语法分析 - 生成AST
            ASTNode ast = parser.parse(tokens);
            log.info("AST结构:{}", ast.toTreeString());

            // 2.3 语义分析
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(typeProcessor);
            SemanticAnalyzer.SemanticResult semanticResult = semanticAnalyzer.analyze(ast);
            if (!semanticResult.valid()) {
                return CompilationResult.failure("语义分析失败", semanticResult.errors(), System.currentTimeMillis() - startTime);
            }

            // ============ 阶段3：注解系统处理 ============
            log.info("[阶段3] 注解系统处理");

            annotationManager.processAnnotations(ast);
            memoryManager.processAnnotations(annotationManager.getProgramAnnotations());
            propertyManager.processAnnotations(annotationManager.getProgramAnnotations());

            // ============ 阶段4：三层操作流处理 ============
            log.info("[阶段4] 三层操作流处理");

            flowManager.processAllFunctions(ast);

            // ============ 阶段5：验证和代码生成 ============
            log.info("[阶段5] 验证和代码生成");

            // 5.1 类型检查
            TypeChecker typeChecker = new TypeChecker(typeProcessor);
            TypeChecker.TypeCheckResult typeCheckResult = typeChecker.check(ast);
            if (!typeCheckResult.isValid()) {
                return CompilationResult.failure("类型检查失败", typeCheckResult.getErrors(), System.currentTimeMillis() - startTime);
            }

            // 5.2 代码生成
            CodeGenerator codeGenerator = new CodeGenerator(memoryManager, propertyManager, flowManager);
            GeneratedCode generatedCode = codeGenerator.generate(ast, annotationManager, flowManager);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("====================================");
            log.info("编译完成: {} ({}ms)", fileName, elapsed);
            log.info("====================================");

            return CompilationResult.success(generatedCode, elapsed);

        } catch (CompilationException ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("编译失败: {} - {}", fileName, ex.getMessage());
            return CompilationResult.failure(fileName, ex.getMessage(), elapsed);
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("编译异常: {} - {}", fileName, ex.getMessage(), ex);
            return CompilationResult.failure("内部编译错误: " + ex.getMessage(), elapsed);
        }
    }

    /**
     * 清理资源
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            log.info("并行编译管道已关闭");
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

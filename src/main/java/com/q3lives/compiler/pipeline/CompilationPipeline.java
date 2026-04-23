// ==================== CompilationPipeline.java ====================
package com.q3lives.compiler.pipeline;

import com.q3lives.annotation.*;
import com.q3lives.compiler.common.CompilerError;
import com.q3lives.compiler.core.*;
import com.q3lives.compiler.decomposer.*;
import com.q3lives.compiler.frontend.*;
import com.q3lives.compiler.generators.*;
import com.q3lives.compiler.hierarchy.*;
import com.q3lives.compiler.integration.*;
import com.q3lives.compiler.pairer.*;
import com.q3lives.compiler.processors.semantic.*;
import com.q3lives.compiler.scanner.*;
import com.q3lives.compiler.security.PathValidator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.List;

/**
 * 编译管道 - 完整的编译流程
 * 
 * 实现思想2：扫描→配对→分层→分解→生成
 * 实现思想3：4层处理器架构
 */
@Slf4j
@Getter
public class CompilationPipeline {


    // 组件
    private final SourceScanner scanner = new SourceScanner();
    private final Preprocessor preprocessor = new Preprocessor();
    private final PairingAnalyzer pairingAnalyzer = new PairingAnalyzer();
    private final HierarchyBuilder hierarchyBuilder = new HierarchyBuilder();
    private final EntityDecomposer entityDecomposer = new EntityDecomposer();
    private final Tokenizer tokenizer = new Tokenizer();
    private final Parser parser = new Parser();

    // 语义处理器
    private final TypeProcessor typeProcessor = new TypeProcessor();
    private final FunctionProcessor functionProcessor = new FunctionProcessor();
    private final ControlFlowProcessor controlFlowProcessor = new ControlFlowProcessor();
    private final DeclarationProcessor declarationProcessor = new DeclarationProcessor();
    private final LiteralProcessor literalProcessor = new LiteralProcessor();
    private final OperatorProcessor operatorProcessor = new OperatorProcessor();

    // 注解系统
    private final AnnotationManager annotationManager = new AnnotationManager();

    // 系统集成
    private final MemoryManager memoryManager = new MemoryManager();
    private final PropertyManager propertyManager = new PropertyManager();
    private final FlowManager flowManager = new FlowManager();

    /**
     * 执行完整编译流程
     */
    public CompilationResult compile(String source, String fileName) {
        log.info("====================================");
        log.info("Claw 编译器 v{} 开始编译: {}", CompilerConstants.COMPILER_VERSION, fileName);
        log.info("====================================");

        long startTime = System.currentTimeMillis();

        // 输入验证
        if (source == null || source.trim().isEmpty()) {
            return CompilationResult.failure("源代码不能为空", startTime);
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            return CompilationResult.failure("文件名不能为空", startTime);
        }

        try {
            // ============ 阶段1：扫描→配对→分层（思想2）============
            log.info("[阶段1] 扫描→配对→分层");

            // 1.1 扫描
            SourceView sourceView = scanner.scan(source, fileName);

            // 1.2 预处理
            preprocessor.preprocess(sourceView);

            // 1.3 配对分析
            PairingResult pairingResult = pairingAnalyzer.analyze(sourceView);
            if (!pairingResult.isValid()) {
                return CompilationResult.failure("配对检查失败", pairingResult.getErrors(), System.currentTimeMillis() - startTime);
            }

            // 1.4 层级构建
            HierarchicalBlocks hierarchicalBlocks = hierarchyBuilder.build(sourceView, pairingResult);

            // 1.5 实体分解
            EntityDecomposer.DecompositionResult decomposition = 
                entityDecomposer.decompose(hierarchicalBlocks);

            // ============ 阶段2：4层处理器处理（思想3）============
            log.info("[阶段2] 4层处理器处理");

            // 2.1 第1层：基础处理器 - 分词
            List<Token> tokens = tokenizer.tokenize(sourceView);

            // 2.2 第2层：语义处理器（可并行）
            typeProcessor.processTokens(tokens);
            functionProcessor.processTokens(tokens);
            controlFlowProcessor.processTokens(tokens);
            declarationProcessor.processTokens(tokens);

            // 2.3 语法分析 - 生成AST
            ASTNode ast = parser.parse(tokens);
            log.info("AST结构:{}", ast.toTreeString());

            // 2.4 语义分析
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(typeProcessor);
            SemanticAnalyzer.SemanticResult semanticResult = semanticAnalyzer.analyze(ast);
            if (!semanticResult.valid()) {
                return CompilationResult.failure("语义分析失败", semanticResult.errors(), System.currentTimeMillis() - startTime);
            }

            // ============ 阶段3：注解系统处理（精确设计）============
            log.info("[阶段3] 注解系统处理");

            annotationManager.processAnnotations(ast);

            // 系统集成：处理构造/析构和属性监听
            memoryManager.processAnnotations(annotationManager.getProgramAnnotations());
            propertyManager.processAnnotations(annotationManager.getProgramAnnotations());

            // ============ 阶段4：三层操作流处理（思想1）============
            log.info("[阶段4] 三层操作流处理");

            flowManager.processAllFunctions(ast);

                        // ============ 阶段5：验证和代码生成（思想3第4层）============
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
        } catch (IOException ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("IO错误: {} - {}", fileName, ex.getMessage());
            return CompilationResult.failure(String.format(CompilerConstants.ERROR_MESSAGE_FILE_NOT_FOUND, fileName), elapsed);
        } catch (SecurityException ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("安全错误: {} - {}", fileName, ex.getMessage());
            return CompilationResult.failure(String.format(CompilerConstants.ERROR_MESSAGE_ACCESS_DENIED, fileName), elapsed);
        } catch (RuntimeException ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("运行时异常: {} - {}", fileName, ex.getMessage(), ex);
            return CompilationResult.failure(String.format(CompilerConstants.ERROR_MESSAGE_INTERNAL_ERROR, ex.getMessage()), elapsed);
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("未预期的编译异常: {} - {}", fileName, ex.getMessage(), ex);
            return CompilationResult.failure(String.format(CompilerConstants.ERROR_MESSAGE_INTERNAL_ERROR, "意外的编译错误"), elapsed);
        }
    }
}


package com.q3lives.pipeline;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.q3lives.binding.GenerationConfig;
import com.q3lives.binding.GenerationResult;
import com.q3lives.compiler.annotation.AnnotationManager;
import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.context.StructureContext;
import com.q3lives.compiler.core.PreprocessedSource;
import com.q3lives.compiler.core.Preprocessor;
import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.Tokenizer;
import com.q3lives.compiler.generators.TypeChecker;
import com.q3lives.compiler.hierarchy.HierarchicalBlocks;
import com.q3lives.compiler.hierarchy.HierarchyBuilder;
import com.q3lives.compiler.pairer.PairingAnalyzer;
import com.q3lives.compiler.pairer.PairingResult;
import com.q3lives.compiler.processors.blocks.AnnotationBlockProcessor;
import com.q3lives.compiler.processors.blocks.AssignmentBlockProcessor;
import com.q3lives.compiler.processors.blocks.ControlFlowBlockProcessor;
import com.q3lives.compiler.processors.blocks.DeclarationBlockProcessor;
import com.q3lives.compiler.processors.blocks.ExpressionBlockProcessor;
import com.q3lives.compiler.processors.blocks.FunctionBlockProcessor;
import com.q3lives.compiler.processors.blocks.ModuleBlockProcessor;
import com.q3lives.compiler.processors.blocks.ScopeBlockProcessor;
import com.q3lives.compiler.processors.blocks.TypeBlockProcessor;
import com.q3lives.compiler.processors.semantic.ControlFlowProcessor;
import com.q3lives.compiler.processors.semantic.DeclarationProcessor;
import com.q3lives.compiler.processors.semantic.FunctionProcessor;
import com.q3lives.compiler.processors.semantic.LiteralProcessor;
import com.q3lives.compiler.processors.semantic.OperatorProcessor;
import com.q3lives.compiler.processors.semantic.TypeProcessor;
import com.q3lives.compiler.scanner.SourceScanner;
// import com.q3lives.ir.ClawIR;

import com.q3lives.binding.TargetCodeGenerator;
import com.q3lives.compiler.generators.IRGenerator;

import com.q3lives.compiler.processors.blocks.AOPBlockProcessor;
import com.q3lives.compiler.scanner.SourceView;
import com.q3lives.ir.ClawIR;



/**
 * Claw编译器主管道
 * 将已实现的各个阶段串联成完整的编译流程
 */
public class ClawCompilerPipeline {
    
    // ===== 阶段1组件（已实现） =====
    private final SourceScanner sourceScanner;
    private final PairingAnalyzer pairingAnalyzer;
    private final HierarchyBuilder hierarchyBuilder;
    
    // ===== 阶段2组件（已实现） =====
    private final Preprocessor preprocessor;
    private final Tokenizer tokenizer;
    
    // ===== 语义处理器（已实现） =====
    private final TypeProcessor typeProcessor;
    private final FunctionProcessor functionProcessor;
    private final ControlFlowProcessor controlFlowProcessor;
    private final DeclarationProcessor declarationProcessor;
    private final LiteralProcessor literalProcessor;
    private final OperatorProcessor operatorProcessor;
    
    // ===== 块处理器（已实现） =====
    private final FunctionBlockProcessor functionBlockProcessor;
    private final ControlFlowBlockProcessor controlFlowBlockProcessor;
    private final ExpressionBlockProcessor expressionBlockProcessor;
    private final DeclarationBlockProcessor declarationBlockProcessor;
    private final ScopeBlockProcessor scopeBlockProcessor;
    private final AssignmentBlockProcessor assignmentBlockProcessor;
    private final TypeBlockProcessor typeBlockProcessor;
    private final ModuleBlockProcessor moduleBlockProcessor;
    private final AnnotationBlockProcessor annotationBlockProcessor;
    private final AOPBlockProcessor aopBlockProcessor;

    // ===== 验证器（已实现） =====
    private final TypeChecker typeChecker;
    
    // ===== 注解处理器 =====
    private final AnnotationManager annotationManager;
    
    // ===== IR生成器 =====
    private final IRGenerator irGenerator;
    
    // ===== 绑定层（可替换） =====
    private final TargetCodeGenerator codeGenerator;
    
    /**
     * 通过Builder创建管道
     */
    private ClawCompilerPipeline(Builder builder) {
        // 初始化所有组件
        this.sourceScanner = builder.sourceScanner;
        this.pairingAnalyzer = builder.pairingAnalyzer;
        this.hierarchyBuilder = builder.hierarchyBuilder;
        this.preprocessor = builder.preprocessor;
        this.tokenizer = builder.tokenizer;
        this.typeProcessor = builder.typeProcessor;
        this.functionProcessor = builder.functionProcessor;
        this.controlFlowProcessor = builder.controlFlowProcessor;
        this.declarationProcessor = builder.declarationProcessor;
        this.literalProcessor = builder.literalProcessor;
        this.operatorProcessor = builder.operatorProcessor;
        this.functionBlockProcessor = builder.functionBlockProcessor;
        this.controlFlowBlockProcessor = builder.controlFlowBlockProcessor;
        this.expressionBlockProcessor = builder.expressionBlockProcessor;
        this.declarationBlockProcessor = builder.declarationBlockProcessor;
        this.scopeBlockProcessor = builder.scopeBlockProcessor;
        this.assignmentBlockProcessor = builder.assignmentBlockProcessor;
        this.typeBlockProcessor = builder.typeBlockProcessor;
        this.moduleBlockProcessor = builder.moduleBlockProcessor;
        this.annotationBlockProcessor = builder.annotationBlockProcessor;
        this.aopBlockProcessor = builder.aopBlockProcessor;
        this.typeChecker = builder.typeChecker;
        this.annotationManager = builder.annotationManager;
        this.irGenerator = builder.irGenerator;
        this.codeGenerator = builder.codeGenerator;
    }
    
    /**
     * 编译入口 - 完整管道执行
     */
    public CompilationResult compile(String sourceCode, String moduleName) {
        long startTime = System.currentTimeMillis();

        // 创建基础结果对象
        CompilationResult result = new CompilationResult(moduleName);

        try {
            // ==========================================
            // 阶段1：扫描 → 配对 → 分层（思想2）
            // ==========================================

            // 1.1 扫描整个文件
            SourceView sourceView = sourceScanner.scan(sourceCode,null);
            result.setPhaseComplete("scan");

            // 1.2 配对检查
            PairingResult pairingResult = pairingAnalyzer.analyze(sourceView);
            if (pairingResult.hasErrors()) {
                return CompilationResult.failure(moduleName,
                    "配对检查失败: " + pairingResult.formatErrors(),
                    pairingResult.getErrors(),
                    System.currentTimeMillis() - startTime);
            }
            result.setPhaseComplete("pair");

            // 1.3 自顶向下建立无限分级代码块
            HierarchicalBlocks hierarchicalBlocks = hierarchyBuilder.build(
                sourceView, pairingResult
            );
            result.setPhaseComplete("hierarchy");

            // ==========================================
            // 阶段2：4层处理器处理（思想3）
            // ==========================================

            // 2.1 第1层 - 基础处理器
            PreprocessedSource cleaned = preprocessor.preprocess(sourceView);
            List<Token> tokens = tokenizer.tokenize(sourceView);
            result.setPhaseComplete("tokenize");

            // 2.2 第2层 - 语义处理器（并行执行）
            SemanticContext semanticCtx = runSemanticProcessors(
                tokens, hierarchicalBlocks
            );
            result.setPhaseComplete("semantic");

            // 2.3 第3层 - 块处理器（并行执行）
            StructureContext structureCtx = runBlockProcessors(
                hierarchicalBlocks, tokens, semanticCtx
            );
            result.setPhaseComplete("structure");

            // 2.4 第4层 - 类型检查
            List<String> typeErrors = typeChecker.check(semanticCtx, structureCtx);
            if (!typeErrors.isEmpty()) {
                return CompilationResult.failure(moduleName,
                    "类型检查失败",
                    typeErrors,
                    System.currentTimeMillis() - startTime);
            }
            result.setPhaseComplete("typecheck");

            // ==========================================
            // 阶段3：注解处理（精确设计）
            // ==========================================

            // AnnotationType annotationResult = annotationManager.process(
            //     structureCtx, semanticCtx
            // );
            result.setPhaseComplete("annotation");

            // ==========================================
            // 阶段4：生成IR（语言无关）
            // ==========================================

            ClawIR ir = irGenerator.generate(
                moduleName, structureCtx, semanticCtx, null
            );
            result.setIR(ir);
            result.setPhaseComplete("ir");

            // ==========================================
            // 阶段5：绑定层生成目标代码（可替换）
            // ==========================================

            GenerationResult genResult = codeGenerator.generate(
                null, GenerationConfig.defaultConfig()
            );

            if (genResult.hasErrors()) {
                result.addErrors(genResult.getErrors());
            } else {
                result.setGeneratedFiles(genResult.getFiles());
                result.addWarnings(genResult.getWarnings());
            }
            result.setPhaseComplete("codegen");

            // 成功完成
            return CompilationResult.successBuilder(moduleName, null,
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            return CompilationResult.failure(moduleName,
                "编译器内部错误: " + e.getMessage(), null,
                System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * 并行执行6个语义处理器
     */
    private SemanticContext runSemanticProcessors(List<Token> tokens, 
                                                  HierarchicalBlocks blocks) {
        SemanticContext ctx = new SemanticContext();
        
        // 使用CompletableFuture并行处理
        CompletableFuture<Void> typeFuture = CompletableFuture.runAsync(
            () -> typeProcessor.process(tokens, blocks, ctx)
        );
        CompletableFuture<Void> funcFuture = CompletableFuture.runAsync(
            () -> functionProcessor.process(tokens, blocks, ctx)
        );
        CompletableFuture<Void> controlFuture = CompletableFuture.runAsync(
            () -> controlFlowProcessor.process(tokens, blocks, ctx)
        );
        CompletableFuture<Void> declFuture = CompletableFuture.runAsync(
            () -> declarationProcessor.process(tokens, blocks, ctx)
        );
        CompletableFuture<Void> literalFuture = CompletableFuture.runAsync(
            () -> literalProcessor.process(tokens, blocks, ctx)
        );
        CompletableFuture<Void> opFuture = CompletableFuture.runAsync(
            () -> operatorProcessor.process(tokens, blocks, ctx)
        );
        
        // 等待所有完成
        CompletableFuture.allOf(
            typeFuture, funcFuture, controlFuture, 
            declFuture, literalFuture, opFuture
        ).join();
        
        return ctx;
    }
    
    /**
     * 并行执行9个块处理器
     */
    private StructureContext runBlockProcessors(HierarchicalBlocks blocks,List<Token> tokens,
                                                SemanticContext semanticCtx) {
        StructureContext ctx = new StructureContext(null);
        
        CompletableFuture<Void> f1 = CompletableFuture.runAsync(
            () -> functionBlockProcessor.process(blocks.getRoot(), tokens)
        );
        CompletableFuture<Void> f2 = CompletableFuture.runAsync(
            () -> controlFlowBlockProcessor.process(blocks.getRoot(), tokens)
        );
        CompletableFuture<Void> f3 = CompletableFuture.runAsync(
            () -> expressionBlockProcessor.process(blocks.getRoot(), tokens)
        );
        CompletableFuture<Void> f4 = CompletableFuture.runAsync(
            () -> declarationBlockProcessor.process(blocks.getRoot(), tokens)
        );
        CompletableFuture<Void> f5 = CompletableFuture.runAsync(
            () -> scopeBlockProcessor.process(blocks.getRoot(), tokens)
        );
        CompletableFuture<Void> f6 = CompletableFuture.runAsync(
            () -> assignmentBlockProcessor.process(blocks.getRoot(), tokens)
        );
        CompletableFuture<Void> f7 = CompletableFuture.runAsync(
            () -> typeBlockProcessor.process(blocks.getRoot(), tokens)
        );
        CompletableFuture<Void> f8 = CompletableFuture.runAsync(
            () -> moduleBlockProcessor.process(blocks.getRoot(), tokens)
        );
        CompletableFuture<Void> f9 = CompletableFuture.runAsync(
            () -> annotationBlockProcessor.process(blocks.getRoot(), tokens)
        );
        CompletableFuture<Void> f10 = CompletableFuture.runAsync(
            () -> aopBlockProcessor.process(blocks.getRoot(), tokens)
        );

        CompletableFuture.allOf(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10).join();
        
        return ctx;
    }
    
    // ===== Builder =====
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private SourceScanner sourceScanner;
        private PairingAnalyzer pairingAnalyzer;
        private HierarchyBuilder hierarchyBuilder;
        private Preprocessor preprocessor;
        private Tokenizer tokenizer;
        private TypeProcessor typeProcessor;
        private FunctionProcessor functionProcessor;
        private ControlFlowProcessor controlFlowProcessor;
        private DeclarationProcessor declarationProcessor;
        private LiteralProcessor literalProcessor;
        private OperatorProcessor operatorProcessor;
        private FunctionBlockProcessor functionBlockProcessor;
        private ControlFlowBlockProcessor controlFlowBlockProcessor;
        private ExpressionBlockProcessor expressionBlockProcessor;
        private DeclarationBlockProcessor declarationBlockProcessor;
        private ScopeBlockProcessor scopeBlockProcessor;
        private AssignmentBlockProcessor assignmentBlockProcessor;
        private TypeBlockProcessor typeBlockProcessor;
        private ModuleBlockProcessor moduleBlockProcessor;
        private AnnotationBlockProcessor annotationBlockProcessor;
        private AOPBlockProcessor aopBlockProcessor;
        private TypeChecker typeChecker;
        private AnnotationManager annotationManager;
        private IRGenerator irGenerator;
        private TargetCodeGenerator codeGenerator;
        
        /**
         * 使用默认组件初始化所有前端处理器
         */
        public Builder withDefaultFrontend() {
            this.sourceScanner = new SourceScanner();
            this.pairingAnalyzer = new PairingAnalyzer();
            this.hierarchyBuilder = new HierarchyBuilder();
            this.preprocessor = new Preprocessor();
            this.tokenizer = new Tokenizer();
            this.typeProcessor = new TypeProcessor();
            this.functionProcessor = new FunctionProcessor();
            this.controlFlowProcessor = new ControlFlowProcessor();
            this.declarationProcessor = new DeclarationProcessor();
            this.literalProcessor = new LiteralProcessor();
            this.operatorProcessor = new OperatorProcessor();
            this.functionBlockProcessor = new FunctionBlockProcessor();
            this.controlFlowBlockProcessor = new ControlFlowBlockProcessor();
            this.expressionBlockProcessor = new ExpressionBlockProcessor();
            this.declarationBlockProcessor = new DeclarationBlockProcessor();
            this.scopeBlockProcessor = new ScopeBlockProcessor();
            this.assignmentBlockProcessor = new AssignmentBlockProcessor();
            this.typeBlockProcessor = new TypeBlockProcessor();
            this.moduleBlockProcessor = new ModuleBlockProcessor();
            this.annotationBlockProcessor = new AnnotationBlockProcessor();
            this.aopBlockProcessor = new AOPBlockProcessor();
            this.typeChecker = new TypeChecker(typeProcessor);
            this.annotationManager = new AnnotationManager();
            this.irGenerator = new IRGenerator("");
            return this;
        }
        
        /**
         * 设置目标语言代码生成器（绑定层入口）
         */
        public Builder withCodeGenerator(TargetCodeGenerator codeGenerator) {
            this.codeGenerator = codeGenerator;
            return this;
        }
        
        // 各组件的独立setter，方便替换测试...
        public Builder sourceScanner(SourceScanner s) { this.sourceScanner = s; return this; }
        public Builder pairingAnalyzer(PairingAnalyzer p) { this.pairingAnalyzer = p; return this; }
        // ... 其他setter省略
        
        public ClawCompilerPipeline build() {
            Objects.requireNonNull(codeGenerator, "必须指定目标语言代码生成器");
            return new ClawCompilerPipeline(this);
        }
    }
}

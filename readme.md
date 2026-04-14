Claw 语言编译器 - Java 实现
根据设计文档，我将严格按照 5大设计思想 和 4层处理器架构 生成完整的 Java 项目。由于项目体量很大，我会按模块逐步生成所有核心文件。

项目结构总览
claw-compiler/
├── pom.xml
├── src/main/java/claw/compiler/
│   ├── ClawCompiler.java                    # 编译器主入口
│   ├── scanner/                             # 扫描器组件
│   │   ├── SourceScanner.java
│   │   ├── SourceView.java
│   │   └── LineInfo.java
│   ├── pairer/                              # 配对器组件
│   │   ├── PairingAnalyzer.java
│   │   ├── Pair.java
│   │   └── PairingResult.java
│   ├── hierarchy/                           # 层级构建器
│   │   ├── HierarchyBuilder.java
│   │   ├── CodeBlock.java
│   │   ├── BlockType.java
│   │   └── HierarchicalBlocks.java
│   ├── decomposer/                          # 实体分解器
│   │   ├── EntityDecomposer.java
│   │   ├── EntityRecognizer.java
│   │   └── EntityType.java
│   ├── core/                                # 第1层：基础处理器
│   │   ├── Preprocessor.java
│   │   ├── Tokenizer.java
│   │   ├── Token.java
│   │   └── TokenType.java
│   ├── processors/                          # 第2-3层处理器
│   │   ├── semantic/                        # 第2层：语义处理器
│   │   │   ├── TypeProcessor.java
│   │   │   ├── FunctionProcessor.java
│   │   │   ├── ControlFlowProcessor.java
│   │   │   ├── DeclarationProcessor.java
│   │   │   ├── LiteralProcessor.java
│   │   │   └── OperatorProcessor.java
│   │   └── blocks/                          # 第3层：块处理器
│   │       ├── FunctionBlockProcessor.java
│   │       ├── ControlFlowBlockProcessor.java
│   │       ├── ExpressionBlockProcessor.java
│   │       ├── DeclarationBlockProcessor.java
│   │       ├── ScopeBlockProcessor.java
│   │       ├── AssignmentBlockProcessor.java
│   │       ├── TypeBlockProcessor.java
│   │       ├── ModuleBlockProcessor.java
│   │       └── AnnotationBlockProcessor.java
│   ├── generators/                          # 第4层：验证生成
│   │   ├── TypeChecker.java
│   │   └── CodeGenerator.java
│   ├── annotation/                          # 注解子系统
│   │   ├── ProgramAnnotations.java
│   │   ├── SystemAnnotations.java
│   │   ├── AnnotationManager.java
│   │   ├── DualFormatCompiler.java
│   │   └── DescriptionConverter.java
│   ├── flow/                                # 三层操作流
│   │   ├── NormalFlow.java
│   │   ├── ExceptionFlow.java
│   │   └── BusinessFlow.java
│   ├── integration/                         # 系统集成
│   │   ├── MemoryManager.java
│   │   ├── PropertyManager.java
│   │   └── FlowManager.java
│   ├── frontend/                            # 编译器前端
│   │   ├── Parser.java
│   │   ├── ASTNode.java
│   │   └── SemanticAnalyzer.java
│   └── pipeline/                            # 处理管道
│       └── CompilationPipeline.java
└── src/test/javaclaw/compiler/test/
    └── ClawCompilerTest.java

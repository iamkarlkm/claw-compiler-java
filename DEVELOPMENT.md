# Claw Compiler Java - 开发文档

## 1. 快速开始

### 1.1 环境要求

| 工具 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Maven | 3.8+ |
| IDE | IntelliJ IDEA（推荐）/ Eclipse / VS Code |

### 1.2 构建项目

```bash
# 克隆项目
cd claw-compiler-java

# 编译
mvn compile

# 运行测试
mvn test

# 打包
mvn package

# 运行（演示模式）
java -jar target/claw-compiler-3.0.0.jar
```

### 1.3 运行编译器

```bash
# 编译单个文件
java -jar target/claw-compiler-3.0.0.jar input.claw output.java

# 无参数运行（演示模式）
java -jar target/claw-compiler-3.0.0.jar
```

---

## 2. 项目结构详解

### 2.1 源码目录

```
src/main/java/
├── com/claw/
│   ├── compiler/           # 编译器核心实现
│   │   ├── ClawCompiler.java           # 主入口类
│   │   ├── pipeline/                   # 编译管道
│   │   ├── core/                       # 核心组件（Tokenizer）
│   │   ├── scanner/                    # 源码扫描器
│   │   ├── pairer/                     # 配对分析器
│   │   ├── hierarchy/                  # 层级构建器
│   │   ├── decomposer/                 # 实体分解器
│   │   ├── processors/                 # 处理器（第2-3层）
│   │   ├── frontend/                   # 前端（Parser、AST）
│   │   ├── annotation/                 # 注解系统
│   │   ├── flow/                       # 三层操作流
│   │   ├── integration/                # 系统集成
│   │   ├── generators/                 # 代码生成器
│   │   └── context/                    # 编译上下文
│   ├── ir/                  # 中间表示
│   │   ├── ClawIR.java                 # IR 主类
│   │   ├── IRNode.java                 # IR 节点基类
│   │   ├── IRNodeType.java             # 节点类型枚举
│   │   ├── IRAnnotation.java           # IR 注解
│   │   ├── FlowModel.java              # 流模型
│   │   └── nodes/                      # 具体节点类型
│   └── binding/             # 绑定层
│       ├── TypeMapper.java             # 类型映射接口
│       ├── GenerationResult.java       # 生成结果
│       ├── GenerationConfig.java       # 生成配置
│       └── java/                       # Java 目标绑定
│           ├── JavaCodeGenerator.java  # Java 代码生成器
│           ├── JavaTypeMapper.java     # Java 类型映射
│           └── JavaRuntime.java        # Java 运行时
└── claw/compiler/
    └── binding/            # 绑定层接口
        ├── TargetCodeGenerator.java    # 代码生成器接口
        ├── TargetRuntime.java          # 运行时接口
        ├── python/                     # Python 目标绑定
        └── c/                          # C 目标绑定
```

### 2.2 测试目录

```
src/test/java/
└── com/claw/compiler/
    ├── ClawCompilerTest.java           # 集成测试
    └── Main.java                       # 测试入口
```

### 2.3 资源目录

```
src/main/resources/
├── test.claw               # 测试用例
├── database.claw           # 数据库示例
├── clib.example.claw       # C库调用示例
└── other_lib.claw          # 其他库示例
```

---

## 3. 核心组件开发

### 3.1 词法分析器 (Tokenizer)

**位置**: `com.claw.compiler.core.Tokenizer`

**职责**: 将源代码字符串转换为 Token 序列

```java
// 使用示例
Tokenizer tokenizer = new Tokenizer();
List<Token> tokens = tokenizer.tokenize(sourceView);

// Token 结构
public class Token {
    private TokenType type;      // Token 类型
    private String value;        // 字面值
    private int line;            // 行号
    private int column;          // 列号
}
```

**支持的 Token 类型**:
- 关键字: `function`, `if`, `else`, `while`, `for`, `return`, `import` 等
- 类型: `Int`, `Float`, `String`, `Bool`, `Void` 等
- 字面量: 整数、浮点数、字符串、字符
- 运算符: `+`, `-`, `*`, `/`, `=`, `==`, `!=`, `&&`, `||` 等
- 分隔符: `{`, `}`, `(`, `)`, `[`, `]`, `,`, `;` 等

### 3.2 语法分析器 (Parser)

**位置**: `com.claw.compiler.frontend.Parser`

**职责**: 将 Token 序列转换为抽象语法树 (AST)

```java
// 使用示例
Parser parser = new Parser();
ASTNode ast = parser.parse(tokens);

// AST 节点结构
public class ASTNode {
    private IRNodeType type;           // 节点类型
    private Map<String, Object> meta;  // 元数据
    private List<ASTNode> children;    // 子节点
}
```

### 3.3 语义分析器 (SemanticAnalyzer)

**位置**: `com.claw.compiler.frontend.SemanticAnalyzer`

**职责**: 执行语义检查和类型推断

```java
// 使用示例
SemanticAnalyzer analyzer = new SemanticAnalyzer(typeProcessor);
SemanticAnalyzer.SemanticResult result = analyzer.analyze(ast);

if (!result.valid()) {
    // 处理语义错误
    List<String> errors = result.errors();
}
```

### 3.4 代码生成器 (CodeGenerator)

**位置**: `com.claw.compiler.generators.CodeGenerator`

**职责**: 从 AST 生成目标代码

```java
// 使用示例
CodeGenerator generator = new CodeGenerator(
    memoryManager, 
    propertyManager, 
    flowManager
);
GeneratedCode code = generator.generate(ast, annotationManager, flowManager);

// 生成结果
String targetCode = code.getTargetCode();           // 目标语言代码
String ir = code.getIntermediateRepresentation();   // 中间表示
String pseudo = code.getPseudoCode();               // 伪代码
```

---

## 4. 处理器开发

### 4.1 语义处理器 (第2层)

**位置**: `com.claw.compiler.processors.semantic.*`

**接口约定**:

```java
public class XxxProcessor {
    
    /**
     * 处理 Token 序列
     */
    public void processTokens(List<Token> tokens) {
        // 实现处理逻辑
    }
    
    /**
     * 获取处理结果
     */
    public XxxInfo getInfo(String name) {
        // 返回处理后的信息
    }
}
```

**已有的语义处理器**:

| 处理器 | 职责 |
|--------|------|
| TypeProcessor | 类型定义处理 |
| FunctionProcessor | 函数签名处理 |
| ControlFlowProcessor | 控制流分析 |
| DeclarationProcessor | 声明处理 |
| LiteralProcessor | 字面量处理 |
| OperatorProcessor | 运算符处理 |
| ExternProcessor | 外部声明处理 |

**添加新处理器**:

```java
// 1. 创建处理器类
public class MyProcessor {
    private final Map<String, MyInfo> infoMap = new HashMap<>();
    
    public void processTokens(List<Token> tokens) {
        // 遍历 tokens，提取信息
        for (Token token : tokens) {
            if (token.getType() == TokenType.IDENTIFIER) {
                // 处理逻辑
            }
        }
    }
    
    public MyInfo getInfo(String name) {
        return infoMap.get(name);
    }
}

// 2. 在 CompilationPipeline 中注册
public class CompilationPipeline {
    private final MyProcessor myProcessor = new MyProcessor();
    
    // 在 compile() 方法中调用
    myProcessor.processTokens(tokens);
}
```

### 4.2 块处理器 (第3层)

**位置**: `com.claw.compiler.processors.blocks.*`

**接口约定**:

```java
public interface BlockProcessor {
    
    /**
     * 处理代码块
     */
    void process(CodeBlock block, ProcessContext context);
    
    /**
     * 支持的块类型
     */
    BlockType getSupportedType();
}
```

**已有的块处理器**:

| 处理器 | 处理的块类型 |
|--------|--------------|
| FunctionBlockProcessor | FUNCTION_BLOCK |
| ControlFlowBlockProcessor | CONTROL_FLOW_BLOCK |
| ExpressionBlockProcessor | EXPRESSION_BLOCK |
| DeclarationBlockProcessor | VARIABLE_DECLARATION_BLOCK |
| ScopeBlockProcessor | SCOPE_BLOCK |
| AssignmentBlockProcessor | ASSIGNMENT_BLOCK |
| TypeBlockProcessor | TYPE_DEFINITION_BLOCK |
| ModuleBlockProcessor | MODULE_BLOCK |
| AnnotationBlockProcessor | ANNOTATION_BLOCK |
| ExternBlockProcessor | Extern 块 |

**添加新块处理器**:

```java
// 1. 创建处理器类
public class MyBlockProcessor implements BlockProcessor {
    
    @Override
    public BlockType getSupportedType() {
        return BlockType.MY_BLOCK;
    }
    
    @Override
    public void process(CodeBlock block, ProcessContext context) {
        // 1. 提取块信息
        String content = block.getContent();
        
        // 2. 解析内容
        // ...
        
        // 3. 更新上下文
        context.addInfo(...);
    }
}

// 2. 注册到处理器链
// 在 CompilationPipeline 或专门的 BlockProcessorChain 中添加
```

---

## 5. 注解系统开发

### 5.1 系统注解

**位置**: `com.claw.compiler.annotation.SystemAnnotations`

**语法**: `@@annotation_name("value")`

**内置注解**:

```claw
@@description("描述文本", "签名")
@@param("参数名", "参数描述")
@@return("返回值描述")
@@example("使用示例")
@@deprecated("废弃说明")
```

**处理流程**:

```java
// AnnotationManager 处理系统注解
annotationManager.processAnnotations(ast);

// 获取处理结果
Map<String, Object> metadata = annotationManager.getMetadata("funcName");
```

### 5.2 程序注解

**位置**: `com.claw.compiler.annotation.ProgramAnnotations`

**语法**: `@AnnotationName("method", "target")`

**内置注解**:

```claw
@BeforeName("initMethod", "this")      // 构造前钩子
@AfterName("cleanupMethod", "this")    // 析构后钩子
@BeforeProps("prop1,prop2")            // 属性变更前钩子
@AfterProps("prop1,prop2")             // 属性变更后钩子
```

**钩子注入流程**:

```java
// 1. MemoryManager 处理构造/析构钩子
memoryManager.processAnnotations(programAnnotations);

// 2. PropertyManager 处理属性变更钩子
propertyManager.processAnnotations(programAnnotations);

// 3. 代码生成时注入钩子调用
// 在 JavaCodeGenerator 中:
ctx.appendLine("this.initMethod();");  // @BeforeName
ctx.appendLine("onBeforePropertyChange(...);");  // @BeforeProps
```

### 5.3 添加自定义注解

```java
// 1. 定义注解类型
public enum AnnotationType {
    // 系统注解
    DESCRIPTION, PARAM, RETURN, EXAMPLE, DEPRECATED,
    
    // 程序注解
    BEFORE_NAME, AFTER_NAME, BEFORE_PROPS, AFTER_PROPS,
    
    // 自定义注解
    MY_CUSTOM_ANNOTATION
}

// 2. 在 AnnotationManager 中处理
public class AnnotationManager {
    
    public void processCustomAnnotation(IRAnnotation annotation) {
        if (annotation.getType() == AnnotationType.MY_CUSTOM_ANNOTATION) {
            // 自定义处理逻辑
        }
    }
}

// 3. 在代码生成器中使用
public class JavaCodeGenerator {
    
    private void generateCustomAnnotation(IRAnnotation annotation) {
        // 生成对应的 Java 代码
    }
}
```

---

## 6. 绑定层开发

### 6.1 实现新目标语言

**步骤 1: 创建类型映射器**

```java
package com.claw.binding.xxx;

public class XxxTypeMapper implements TypeMapper {
    
    private static final Map<String, String> TYPE_MAP = Map.of(
        "Int", "int",
        "Float", "double",
        "String", "char*",
        "Bool", "int",
        "Void", "void"
    );
    
    @Override
    public String mapType(String clawType) {
        return TYPE_MAP.getOrDefault(clawType, "Object");
    }
    
    @Override
    public String mapBoxedType(String clawType) {
        // 泛型上下文的包装类型
        return mapType(clawType);
    }
    
    @Override
    public String getDefaultValue(String clawType) {
        return switch (clawType) {
            case "Int" -> "0";
            case "Float" -> "0.0";
            case "String" -> "\"\"";
            case "Bool" -> "false";
            default -> "null";
        };
    }
    
    @Override
    public boolean isPrimitive(String clawType) {
        return Set.of("Int", "Float", "Bool").contains(clawType);
    }
}
```

**步骤 2: 创建运行时实现**

```java
package com.claw.binding.xxx;

public class XxxRuntime implements TargetRuntime {
    
    @Override
    public String getLanguageName() {
        return "Xxx";
    }
    
    @Override
    public String getFileExtension() {
        return ".xxx";
    }
    
    @Override
    public String generateFunctionHeader(String visibility, String returnType,
                                         String name, List<String> params,
                                         List<String> paramTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(visibility).append(" ");
        sb.append(returnType).append(" ");
        sb.append(name).append("(");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramTypes.get(i)).append(" ").append(params.get(i));
        }
        sb.append(") {");
        return sb.toString();
    }
    
    @Override
    public String generateCatchBlock(String exceptionType, String varName, String body) {
        return "catch (" + exceptionType + " " + varName + ") {\n" + body + "\n}";
    }
    
    @Override
    public String generateFlowTo(String targetName) {
        // 根据 C/Java/Python 不同实现
        return "goto " + targetName + ";";
    }
    
    // ... 实现其他方法
}
```

**步骤 3: 创建代码生成器**

```java
package com.claw.binding.xxx;

public class XxxCodeGenerator implements TargetCodeGenerator {
    
    private final XxxRuntime runtime;
    private final XxxTypeMapper typeMapper;
    
    public XxxCodeGenerator() {
        this.typeMapper = new XxxTypeMapper();
        this.runtime = new XxxRuntime();
    }
    
    @Override
    public TargetRuntime getRuntime() {
        return runtime;
    }
    
    @Override
    public String getLanguageName() {
        return "Xxx";
    }
    
    @Override
    public String getFileExtension() {
        return ".xxx";
    }
    
    @Override
    public String generate(ClawIR ir) {
        StringBuilder output = new StringBuilder();
        
        // 1. 生成导入
        for (IRNode node : ir.getNodes()) {
            if (node.getType() == IRNodeType.IMPORT) {
                output.append(runtime.generateImport(
                    node.getMeta("path", String.class), 
                    node.getMeta("symbol", String.class)
                )).append("\n");
            }
        }
        
        // 2. 生成类型定义
        for (IRNode node : ir.getNodes()) {
            if (node.getType() == IRNodeType.TYPE_DEFINITION) {
                generateTypeDefinition(node, output);
            }
        }
        
        // 3. 生成函数
        for (IRNode node : ir.getNodes()) {
            if (node.getType() == IRNodeType.FUNCTION_DECLARATION) {
                generateFunction(node, output);
            }
        }
        
        return output.toString();
    }
    
    private void generateTypeDefinition(IRNode node, StringBuilder output) {
        String typeName = node.getMeta("name", String.class);
        output.append(runtime.generateTypeDefinitionHeader(typeName, "public"));
        // ... 生成字段和方法
        output.append(runtime.generateTypeDefinitionFooter());
    }
    
    private void generateFunction(IRNode node, StringBuilder output) {
        // ... 实现函数生成
    }
}
```

### 6.2 绑定层接口参考

```java
// TargetRuntime 完整接口
public interface TargetRuntime {
    // 语言信息
    String getLanguageName();
    String getFileExtension();
    String getStatementTerminator();
    String getBlockOpen();
    String getBlockClose();
    
    // 代码生成
    String generateFunctionHeader(String visibility, String returnType, 
                                  String name, List<String> params, List<String> paramTypes);
    String generateFunctionFooter();
    String generateVariableDeclaration(boolean isConst, String type, String name, String value);
    String generateReturn(String expression);
    String generateFunctionCall(String name, List<String> args);
    
    // 流程控制
    String generateIf(String condition);
    String generateElse();
    String generateWhile(String condition);
    String generateFor(String init, String condition, String update);
    String generateBreak();
    String generateContinue();
    
    // 异常处理
    String generateTryBlock();
    String generateCatchBlock(String exceptionType, String varName, String body);
    String generateFinallyBlock(String body);
    String generateThrow(String exceptionType, String message);
    
    // 流控制
    String generateFlowTo(String targetName);
    
    // 注解钩子
    String generateConstructorHook(String methodName, String target);
    String generateDestructorHook(String methodName, String target);
    String generateBeforePropsHook(String propName, String newValue);
    String generateAfterPropsHook(String propName, String oldValue, String newValue);
    
    // 导入导出
    String generateImport(String modulePath, String symbolName);
    String generateExport(String symbolName);
    
    // 类型定义
    String generateTypeDefinitionHeader(String typeName, String visibility);
    String generateTypeDefinitionFooter();
    String generateField(String type, String name, String visibility);
    
    // 数组操作
    String generateArrayCreation(String elementType, int size);
    String generateArrayGet(String array, String index);
    String generateArraySet(String array, String index, String value);
    
    // 属性操作
    String generatePropertyGet(String object, String property);
    String generatePropertySet(String object, String property, String value);
    
    // 字符串操作
    String generateStringLiteral(String value);
    String generateStringConcat(List<String> parts);
    
    // 文档注释
    String generateDocComment(Map<String, String> metadata);
    String generateComment(String text);
    
    // 运行时依赖
    List<String> getRequiredImports();
}
```

---

## 7. 测试指南

### 7.1 单元测试

```java
// ClawCompilerTest.java 示例
@Test
@DisplayName("编译简单函数")
void testSimpleFunction() {
    String source = """
            function hello() -> Void {
                println("Hello, Claw!")
            }
            """;
    
    CompilationResult result = compiler.compile(source, "test.claw");
    
    assertTrue(result.isSuccess(), "编译应该成功");
    assertNotNull(result.getGeneratedCode());
    assertTrue(result.getGeneratedCode().getFunctionCount() >= 1);
}
```

### 7.2 运行测试

```bash
# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=ClawCompilerTest

# 运行单个测试方法
mvn test -Dtest=ClawCompilerTest#testSimpleFunction

# 带覆盖率报告
mvn test jacoco:report
```

### 7.3 测试覆盖范围

| 类别 | 测试内容 |
|------|----------|
| 词法分析 | 所有 Token 类型 |
| 语法分析 | 表达式、语句、声明 |
| 语义分析 | 类型检查、作用域 |
| 代码生成 | 各目标语言输出 |
| 注解系统 | 系统/程序注解 |
| 三层流 | normal/exception/flow |
| FFI | 外部函数调用 |
| 性能 | 大规模编译 |

---

## 8. 调试指南

### 8.1 日志配置

**logback.xml**:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.claw" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

### 8.2 调试编译管道

```java
// 在 CompilationPipeline 中添加断点
public CompilationResult compile(String source, String fileName) {
    log.info("开始编译: {}", fileName);
    
    // 断点1: 扫描结果
    SourceView sourceView = scanner.scan(source, fileName);
    
    // 断点2: Token 序列
    List<Token> tokens = tokenizer.tokenize(sourceView);
    
    // 断点3: AST 结构
    ASTNode ast = parser.parse(tokens);
    
    // 断点4: 语义分析结果
    SemanticResult semanticResult = semanticAnalyzer.analyze(ast);
    
    // 断点5: 生成的代码
    GeneratedCode code = codeGenerator.generate(ast, annotationManager, flowManager);
    
    return CompilationResult.success(code, elapsed);
}
```

### 8.3 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 配对失败 | 括号/引号不匹配 | 检查 PairingResult.getErrors() |
| 类型错误 | 类型不匹配 | 检查 TypeChecker.getErrors() |
| 空指针 | 未初始化变量 | 检查 SemanticContext |
| 生成错误 | IR 节点缺失 | 检查 ClawIR 内容 |

---

## 9. 代码风格

### 9.1 命名约定

| 类型 | 命名风格 | 示例 |
|------|----------|------|
| 类名 | PascalCase | `TypeProcessor` |
| 方法名 | camelCase | `processTokens()` |
| 常量 | UPPER_SNAKE | `MAX_RETRIES` |
| 包名 | 小写 | `com.claw.compiler` |
| 枚举 | UPPER_SNAKE | `FUNCTION_BLOCK` |

### 9.2 注释规范

```java
/**
 * 类描述
 * 
 * 实现思想X：...
 */
public class XxxProcessor {
    
    /**
     * 方法描述
     * 
     * @param tokens Token序列
     * @return 处理结果
     */
    public Result processTokens(List<Token> tokens) {
        // 实现
    }
}
```

### 9.3 Lombok 使用

```java
// 推荐注解
@Getter                    // 生成 getter
@Setter                    // 生成 setter
@ToString                  // 生成 toString
@EqualsAndHashCode         // 生成 equals 和 hashCode
@AllArgsConstructor        // 全参数构造函数
@NoArgsConstructor         // 无参构造函数
@Builder                   // 构建器模式
@Slf4j                     // 日志支持
```

---

## 10. 构建与发布

### 10.1 Maven 命令

```bash
# 清理
mvn clean

# 编译
mvn compile

# 测试
mvn test

# 打包
mvn package

# 安装到本地仓库
mvn install

# 完整构建
mvn clean package
```

### 10.2 发布检查清单

- [ ] 所有测试通过
- [ ] 代码风格检查
- [ ] 文档更新
- [ ] 版本号更新
- [ ] CHANGELOG 更新
- [ ] JAR 文件测试

---

## 11. 扩展开发

### 11.1 添加新语法特性

1. **定义 Token 类型** (TokenType.java)
2. **更新词法分析器** (Tokenizer.java)
3. **定义 AST 节点** (IRNodeType.java)
4. **更新语法分析器** (Parser.java)
5. **添加语义处理器** (processors/)
6. **更新代码生成器** (binding/)

### 11.2 添加优化 Pass

```java
public class OptimizationPass {
    
    public ClawIR optimize(ClawIR ir) {
        // 1. 常量折叠
        constantFolding(ir);
        
        // 2. 死代码消除
        deadCodeElimination(ir);
        
        // 3. 内联展开
        inlineExpansion(ir);
        
        return ir;
    }
}
```

### 11.3 添加 IDE 支持

```java
// LSP 服务器实现
public class ClawLanguageServer {
    
    public CompletionItem complete(Position position) {
        // 代码补全
    }
    
    public Diagnostic[] diagnose(String source) {
        // 语法检查
    }
    
    public Location gotoDefinition(Position position) {
        // 跳转定义
    }
}
```

---

*文档版本: 3.0.0*
*最后更新: 2026-04-12*

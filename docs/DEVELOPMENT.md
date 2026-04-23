# Claw Compiler Java 开发指南

## 🚀 快速开始

### 环境要求
- Java 17+
- Maven 3.6+
- IDE（推荐 IntelliJ IDEA 或 VS Code）

### 克隆和构建
```bash
git clone <repository-url>
cd claw-compiler-java
mvn clean install
```

### 运行演示
```bash
# 运行内置演示
java -jar target/claw-compiler-3.0.0.jar

# 编译文件
java -jar target/claw-compiler-3.0.0.jar input.claw output.java
```

## 📁 项目结构

```
claw-compiler-java/
├── src/main/java/com/q3lives/
│   ├── compiler/              # 核心编译器
│   │   ├── annotation/       # 注解系统
│   │   ├── common/          # 公共工具
│   │   ├── context/        # 编译上下文
│   │   ├── core/           # 第1层：核心处理
│   │   ├── decomposer/     # 实体分解
│   │   ├── flow/           # 三层操作流
│   │   ├── frontend/       # 前端解析
│   │   ├── generators/     # 第4层：代码生成
│   │   ├── hierarchy/      # 代码块层次
│   │   ├── integration/    # 系统集成
│   │   └── pairer/        # 代码配对
│   ├── binding/            # 目标语言绑定
│   │   ├── c/             # C 生成器
│   │   ├── java/          # Java 生成器
│   │   └── python/        # Python 生成器
│   └── lsp/               # 语言服务器
├── examples/               # 示例代码
├── docs/                  # 文档
└── test.claw             # 测试文件
```

## 🔧 开发工作流

### 1. 新功能开发
```bash
# 创建功能分支
git checkout -b feature/new-feature

# 编译和测试
mvn clean compile
mvn test

# 提交更改
git commit -m "feat: add new feature"
git push origin feature/new-feature
```

### 2. 代码规范
- 使用 Lombok 减少样板代码
- 遵循 Java 编码规范
- 所有公共 API 需要有 Javadoc
- 测试覆盖率 > 80%

### 3. 测试要求
```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=PythonLoopTest

# 运行集成测试
mvn verify
```

## 🏗️ 架构理解

### 4层处理器架构

#### 第1层：核心处理
```java
// 词法分析示例
Tokenizer tokenizer = new Tokenizer(sourceCode);
List<Token> tokens = tokenizer.tokenize();

// 预处理
Preprocessor preprocessor = new Preprocessor(tokens);
tokens = preprocessor.process();
```

#### 第2层：语义处理
```java
// 类型检查
TypeProcessor typeProcessor = new TypeProcessor(context);
typeProcessor.process(tokens);

// 函数处理
FunctionProcessor functionProcessor = new FunctionProcessor(context);
functionProcessor.process(tokens);
```

#### 第3层：块处理
```java
// 块识别
HierarchyBuilder builder = new HierarchyBuilder(tokens);
HierarchicalBlocks blocks = builder.build();

// 块处理
BlockProcessor processor = new BlockProcessor(blocks);
processor.process();
```

#### 第4层：代码生成
```java
// IR 生成
IRGenerator irGenerator = new IRGenerator(blocks);
ClawIR ir = irGenerator.generate();

// 代码生成
CodeGenerator codeGenerator = new CodeGenerator(ir);
GeneratedCode result = codeGenerator.generate();
```

### 三层操作流

#### 正常流
```java
// 正常执行路径
public CompilationResult compile(String source, String fileName) {
    // 标准编译流程
    return pipeline.compile(source, fileName);
}
```

#### 异常流
```java
// 异常处理
catch (CompilationException e) {
    // 异常处理路径
    result.addError(e.getMessage());
    return result;
}
```

#### 特殊流
```java
// 特殊流控制
public void handleFlow(FlowInstruction instruction) {
    // flow to 处理
    processor.handleFlowJump(instruction);
}
```

## 🎯 代码生成开发

### 添加新的目标语言
1. 在 `binding/` 下创建新包
2. 实现 `CodeGenerator` 接口
3. 添加相应的运行时支持
4. 编写测试用例

```java
// 示例：新目标语言生成器
public class NewLanguageCodeGenerator implements CodeGenerator {
    @Override
    public GeneratedCode generate(ClawIR ir) {
        // 实现代码生成逻辑
        return generatedCode;
    }
}
```

### FFI 开发
```java
// 添加新的 FFI 绑定
public class NewFFIBinding {
    @Extern("library")
    public native void externalFunction(String param);
}
```

## 📊 性能优化

### 编译性能优化
- 使用缓存避免重复解析
- 并行处理独立的代码块
- 延迟加载不必要的组件

### 内存优化
- 重用对象减少 GC 压力
- 使用流式处理大文件
- 及时释放临时资源

### 代码质量优化
- 避免深度嵌套
- 使用早期返回
- 提取重复代码

## 🐛 调试指南

### 编译器调试
```java
// 启用详细日志
LoggerFactory.getLogger("com.q3lives.compiler").setLevel(Level.DEBUG);

// 使用调试模式
ClawCompiler compiler = new ClawCompiler();
compiler.setDebugMode(true);
```

### 测试调试
```bash
# 运行特定测试并输出详细信息
mvn test -Dtest=TestName -X

# 生成测试报告
mvn surefire-report:report
```

### 问题排查步骤
1. 检查输入语法是否正确
2. 查看编译错误信息
3. 使用调试模式输出中间结果
4. 检查生成的目标代码

## 🔍 代码审查清单

### 功能审查
- [ ] 功能实现符合需求
- [ ] 边界情况处理
- [ ] 错误处理完善
- [ ] 性能符合要求

### 代码质量
- [ ] 代码结构清晰
- [ ] 命名规范
- [ ] 注释完整
- [ ] 测试覆盖

### 安全性
- [ ] 避免注入攻击
- [ ] 输入验证
- [ ] 资源管理
- [ ] 权限检查

## 📚 相关文档

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 系统架构设计
- [ROADMAP.md](../ROADMAP.md) - 发展路线图
- [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) - 实现总结
- [CLAUDE.md](../CLAUDE.md) - Claude Code 工作指导

## 🎓 最佳实践

1. **小步前进**：每次只做一个小的改动，然后测试
2. **频繁提交**：保持代码始终可以工作
3. **测试驱动**：先写测试，再写实现
4. **代码复用**：避免重复代码，提取公共逻辑
5. **文档更新**：及时更新相关文档
6. **性能关注**：注意性能瓶颈，适时优化

## 📞 获取帮助

遇到问题时的解决途径：
1. 查看 [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md)
2. 运行测试用例了解使用方法
3. 检查生成的代码示例
4. 提交 Issue 报告问题
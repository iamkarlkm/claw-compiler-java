# CLAUDE.md

本文档为 Claude Code (claude.ai/code) 在此代码库中工作时提供指导。

## 项目概述

Claw Compiler Java 是一个用 Java 编写的复杂多目标语言编译器。它实现了 "Claw" 编程语言，可以将 Claw 源代码编译为多种目标语言（Java、Python 和 C）。

### 核心架构概念

1. **三层操作流模型**：编译器实现了三种执行流：
   - `normal` - 标准执行路径
   - `exception` - 错误处理路径
   - `flow` - 特殊流控制（使用 `flow to` 语句）

2. **4层处理器架构**：
   - 第1层：核心处理（词法分析器、预处理器）
   - 第2层：语义处理（类型、函数、控制流）
   - 第3层：块处理（代码块层次结构）
   - 第4层：验证和代码生成

3. **18种代码块类型**：6维分类，4个粒度级别
   - 函数块、控制流块、表达式块等

4. **注解系统**：
   - 4个程序注解（@BeforeName、@AfterName、@BeforeProps、@AfterProps）
   - 5个系统注解（@@description、@@param、@@return、@@example、@@author）

## 构建和开发

### 构建命令

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 打包 JAR
mvn package

# 运行编译器（演示模式）
java -jar target/claw-compiler-3.0.0.jar

# 编译特定文件
java -jar target/claw-compiler-3.0.0.jar input.claw output.java
```

### 测试

项目使用 JUnit 5 进行测试。运行特定测试类：

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=PythonLoopStructureTest

# 运行匹配模式的测试
mvn test -Dtest="*Loop*"
```

### 项目结构（实际）

注意：实际的包结构是 `com.q3lives.compiler`，而不是某些文档中提到的 `com.claw`。

```
src/main/java/com/q3lives/
├── compiler/               # 核心编译器组件
│   ├── annotation/         # 注解系统
│   ├── common/            # 共享工具
│   ├── context/           # 编译上下文
│   ├── core/              # 核心处理（第1层）
│   ├── decomposer/        # 实体分解器
│   ├── flow/              # 三层操作流
│   ├── frontend/          # 解析器和AST
│   ├── generators/        # 代码生成（第4层）
│   ├── hierarchy/         # 代码块层次结构
│   ├── integration/       # 系统集成
│   └── pairer/            # 代码配对分析
├── binding/               # 目标语言后端
│   ├── c/                 # C 代码生成
│   ├── java/              # Java 代码生成
│   └── python/            # Python 代码生成
└── lsp/                   # 语言服务器协议实现
```

## 开发工作流

### 编译器使用

编译器有两种模式：

1. **演示模式**：无参数运行，使用内置示例代码
2. **文件模式**：编译特定的源文件

```java
// 编程方式使用
ClawCompiler compiler = new ClawCompiler();
CompilationResult result = compiler.compile(sourceCode, "file.claw");

if (result.isSuccess()) {
    String targetCode = result.getGeneratedCode().getTargetCode();
    String ir = result.getGeneratedCode().getIntermediateRepresentation();
    String pseudoCode = result.getGeneratedCode().getPseudoCode();
}
```

### 代码生成

编译器支持三种目标语言：

- **Java**：完整实现，包含类型映射和运行时支持
- **Python**：基础实现，包含类型注解
- **C**：基础实现，包含手动内存管理

每个目标在 `binding/{target}/` 下都有其自己的代码生成器包。

### 外部函数接口（FFI）

编译器包含用于跨语言调用的 FFI 系统。FFI 绑定表支持基于以下条件的特定平台过滤：
- 目标平台（Linux、Windows、macOS）
- 架构（x86、x86_64、ARM64）
- 工具链（GCC、Clang、MSVC）

## 语言特性

### Claw 语言语法

```claw
// 类型定义
type UserProfile {
    var name: String
    var age: Int
    var active: Bool
}

// 带三层操作流的函数
normal function processUser(userData: UserData) -> ProcessResult {
    // 正常执行路径
    var result = ProcessResult()
    
    catch (ValidationError e) {
        // 异常处理路径
        result.success = false
        result.message = e.message
    }
    
    flow to cleanup  // 流控制语句
    
    return result
}

// 注解
@@description("处理用户数据", "UserData -> ProcessResult")
@@param("userData", "用户数据对象")
@@return("处理结果")
@BeforeProps("user.age,user.name")
```

### 循环支持

编译器支持具有 IR 生成的全面循环结构：

- `while` 循环
- `for` 循环（带迭代变量）
- `break` 和 `continue` 语句
- 嵌套循环，具有适当的词法作用域

### 高级特性

1. **AOP（面向切面编程）**：支持切面定义，包含 before/around/after 通知
2. **属性监控**：使用 @BeforeProps/@AfterProps 自动检测属性更改
3. **跨语言 FFI**：从 Claw 导入并调用外部 C/Python 函数

## 关键文件及其用途

- `ClawCompiler.java`：主入口点，演示模式支持
- `CompilationPipeline.java`：编排4层编译过程
- `IRGenerator.java`：创建用于优化的中间表示
- `EnhancedJavaCodeGenerator.java`：100%完成的Java代码生成器
- `EnhancedPythonCodeGenerator.java`：100%完成的Python代码生成器
- `CompleteCCodeGenerator.java`：100%完成的C代码生成器
- `FFIBindingTable.java`：管理外部函数接口绑定

## 当前状态

根据路线图：
- ✅ 编译器前端（词法分析、语法分析、语义分析）- 100%
- ✅ 4层处理器架构 - 100%
- ✅ 三层操作流 - 100%
- ✅ 注解系统 - 100%
- ✅ Java 代码生成 - 100%（EnhancedJavaCodeGenerator）
- ✅ Python 代码生成 - 100%（EnhancedPythonCodeGenerator）
- ✅ C 代码生成 - 100%（CompleteCCodeGenerator）
- 🔄 FFI 系统 - 60%

## 测试指南

1. 单元测试应放在 `src/test/java/` 中
2. 测试命名约定：`{Target}{Feature}Test.java`（如 `PythonLoopTest.java`）
3. 集成测试应验证生成代码的正确性
4. 为隔离的单元测试模拟外部依赖

## 性能考虑

- 编译器每秒处理约 2,500 个函数
- IR 生成使用优化的指令构建器
- FFI 平台过滤使用位掩码编码以提高性能
- 生成的代码包含流控制等特性的运行时支持
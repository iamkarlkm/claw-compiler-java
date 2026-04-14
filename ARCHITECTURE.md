# Claw Compiler Java - 架构设计文档

## 1. 项目概述

**Claw Compiler** 是一个现代化的多目标语言编译器，采用 Java 实现。其核心设计理念是将语言前端与目标语言后端完全解耦，通过中间表示（IR）衔接，实现一次编译、多目标输出的能力。

### 1.1 版本信息

| 属性 | 值 |
|------|------|
| 版本 | 3.0.0 |
| 目标 Java 版本 | 17 |
| 构建工具 | Maven |
| 代码行数 | ~15,000+ |

### 1.2 核心特性

- **三层操作流模型**: `normal` / `exception` / `flow` 三种执行流
- **18 种代码块类型**: 6 维度分类，4 粒度分级
- **4 层处理器架构**: 从词法分析到代码生成的完整管道
- **精确注解系统**: 4 个程序注解 + 5 个系统注解
- **多目标支持**: Java、Python、C（可扩展）

---

## 2. 五大设计思想

### 思想 1: 三层操作流模型

将程序执行流分为三个独立层次，支持细粒度的流程控制：

```
┌─────────────────────────────────────────────────────┐
│                 三层操作流模型                        │
├─────────────────────────────────────────────────────┤
│  normal      │ 正常执行流                            │
│  exception   │ 异常处理流 (catch 不生成堆栈)          │
│  flow        │ 业务流转 (flow to 不记录调用栈)        │
└─────────────────────────────────────────────────────┘
```

**实现文件**:
- `src/main/java/com/claw/compiler/flow/NormalFlow.java`
- `src/main/java/com/claw/compiler/flow/ExceptionFlow.java`
- `src/main/java/com/claw/compiler/flow/BusinessFlow.java`

### 思想 2: 编译过程五阶段

```
源代码(.claw)
    │
    ▼
┌─────────┐   ┌─────────┐   ┌──────────┐   ┌───────────┐   ┌──────────┐
│  Scan   │ → │  Pair   │ → │ Hierarchy│ → │ Decompose │ → │ Generate │
│  扫描   │   │  配对   │   │  分层    │   │  分解     │   │  生成    │
└─────────┘   └─────────┘   └──────────┘   └───────────┘   └──────────┘
```

### 思想 3: 四层处理器架构

```
┌──────────────────────────────────────────────────────┐
│ Layer 4: 验证与生成层                                  │
│   TypeChecker, CodeGenerator                          │
├──────────────────────────────────────────────────────┤
│ Layer 3: 块处理器层 (10个处理器)                       │
│   FunctionBlockProcessor, ControlFlowBlockProcessor   │
│   ExpressionBlockProcessor, DeclarationBlockProcessor │
│   ScopeBlockProcessor, AssignmentBlockProcessor       │
│   TypeBlockProcessor, ModuleBlockProcessor            │
│   AnnotationBlockProcessor, ExternBlockProcessor      │
├──────────────────────────────────────────────────────┤
│ Layer 2: 语义处理器层 (7个处理器)                      │
│   TypeProcessor, FunctionProcessor                    │
│   ControlFlowProcessor, DeclarationProcessor          │
│   LiteralProcessor, OperatorProcessor                 │
│   ExternProcessor                                     │
├──────────────────────────────────────────────────────┤
│ Layer 1: 基础处理器层                                  │
│   Preprocessor, Tokenizer                             │
└──────────────────────────────────────────────────────┘
```

### 思想 4: 18 种代码块类型

按 6 个维度、4 个粒度分级：

| 分类 | 块类型 | 粒度 |
|------|--------|------|
| **函数相关** | FUNCTION_BLOCK, PARAMETER_BLOCK, RETURN_BLOCK | COARSE/FINE |
| **控制流** | CONTROL_FLOW_BLOCK, CONDITION_BLOCK, LOOP_BODY_BLOCK | MEDIUM/FINE |
| **表达式** | EXPRESSION_BLOCK, FUNCTION_CALL_BLOCK, ARRAY_BLOCK | FINE |
| **声明** | VARIABLE_DECLARATION_BLOCK, IMPORT_DECLARATION_BLOCK | FINE |
| **范围** | SCOPE_BLOCK, TYPE_INNER_BLOCK | COARSE/MEDIUM |
| **其他** | ASSIGNMENT_BLOCK, TYPE_DEFINITION_BLOCK, MODULE_BLOCK, ANNOTATION_BLOCK, ROOT_BLOCK | FINE/COARSE/TOP |

**定义文件**: `src/main/java/com/claw/compiler/hierarchy/BlockType.java`

### 思想 5: 精确注解系统

#### 程序注解 (4 个) - 影响代码生成

| 注解 | 作用 | 示例 |
|------|------|------|
| `@BeforeName` | 构造函数钩子 | `@BeforeName("init", "this")` |
| `@AfterName` | 析构函数钩子 | `@AfterName("cleanup", "this")` |
| `@BeforeProps` | 属性变更前钩子 | `@BeforeProps("user.name,user.age")` |
| `@AfterProps` | 属性变更后钩子 | `@AfterProps("user.email")` |

#### 系统注解 (5 个) - 文档生成

| 注解 | 作用 | 示例 |
|------|------|------|
| `@@description` | 描述信息 | `@@description("计算距离", "(x1,y1,x2,y2) -> Double")` |
| `@@param` | 参数说明 | `@@param("x1", "第一个点的x坐标")` |
| `@@return` | 返回值说明 | `@@return("两点之间的距离")` |
| `@@example` | 使用示例 | `@@example("distance(0,0,3,4)")` |
| `@@deprecated` | 废弃标记 | `@@deprecated("使用 newDistance 替代")` |

---

## 3. 系统架构

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Claw Compiler v3.0                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    编译器前端（语言无关）                       │   │
│  │  ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌───────────────┐   │   │
│  │  │ Scanner │→ │ Pairer  │→ │Hierarchy │→ │  Decomposer   │   │   │
│  │  └─────────┘  └─────────┘  └──────────┘  └───────────────┘   │   │
│  │       │                                                       │   │
│  │       ▼                                                       │   │
│  │  ┌─────────────────────────────────────────────────────────┐  │   │
│  │  │              4层处理器 (Layer 1-4)                        │  │   │
│  │  │  Tokenizer → SemanticProcessors → BlockProcessors        │  │   │
│  │  │            → TypeChecker + CodeGenerator                 │  │   │
│  │  └─────────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                              │                                       │
│                              ▼                                       │
│                    ┌─────────────────┐                              │
│                    │     ClawIR      │  ← 中间表示                  │
│                    └────────┬────────┘                              │
│                             │                                        │
├─────────────────────────────┼────────────────────────────────────────┤
│                   绑定层接口 │                                        │
│  ┌──────────────────────────┼────────────────────────────────────┐  │
│  │                          ▼                                     │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │  │
│  │  │ Java Binding │  │Python Binding│  │   C Binding  │        │  │
│  │  ├──────────────┤  ├──────────────┤  ├──────────────┤        │  │
│  │  │JavaCodeGen   │  │PythonCodeGen │  │CCodeGen      │        │  │
│  │  │JavaTypeMap   │  │PythonTypeMap │  │CTypeMap      │        │  │
│  │  │JavaRuntime   │  │PythonRuntime │  │CRuntime      │        │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘        │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 模块划分

```
com.claw.compiler/
├── ClawCompiler.java          # 主入口
├── pipeline/                   # 编译管道
│   ├── CompilationPipeline.java
│   ├── CompilationResult.java
│   └── CompilationException.java
├── core/                       # 第1层：基础组件
│   ├── Token.java
│   ├── TokenType.java
│   ├── Tokenizer.java
│   ├── TokenStream.java
│   ├── Preprocessor.java
│   └── PreprocessedSource.java
├── scanner/                    # 源码扫描
│   ├── SourceScanner.java
│   ├── SourceView.java
│   └── LineInfo.java
├── pairer/                     # 配对分析
│   ├── PairingAnalyzer.java
│   ├── Pair.java
│   └── PairingResult.java
├── hierarchy/                  # 层级构建
│   ├── HierarchyBuilder.java
│   ├── CodeBlock.java
│   ├── BlockType.java
│   └── HierarchicalBlocks.java
├── decomposer/                 # 实体分解
│   ├── EntityDecomposer.java
│   ├── EntityRecognizer.java
│   └── EntityType.java
├── processors/                 # 第2-3层：处理器
│   ├── semantic/               # 第2层：语义处理器
│   │   ├── TypeProcessor.java
│   │   ├── FunctionProcessor.java
│   │   ├── ControlFlowProcessor.java
│   │   ├── DeclarationProcessor.java
│   │   ├── LiteralProcessor.java
│   │   ├── OperatorProcessor.java
│   │   ├── ExternProcessor.java
│   │   └── *Info.java (数据类)
│   └── blocks/                 # 第3层：块处理器
│       ├── BlockProcessor.java
│       ├── FunctionBlockProcessor.java
│       ├── ControlFlowBlockProcessor.java
│       ├── ExpressionBlockProcessor.java
│       ├── DeclarationBlockProcessor.java
│       ├── ScopeBlockProcessor.java
│       ├── AssignmentBlockProcessor.java
│       ├── TypeBlockProcessor.java
│       ├── ModuleBlockProcessor.java
│       ├── AnnotationBlockProcessor.java
│       └── ExternBlockProcessor.java
├── frontend/                   # 前端组件
│   ├── Parser.java
│   ├── ASTNode.java
│   └── SemanticAnalyzer.java
├── annotation/                 # 注解系统
│   ├── AnnotationManager.java
│   ├── ProgramAnnotations.java
│   ├── SystemAnnotations.java
│   ├── DualFormatCompiler.java
│   ├── DescriptionConverter.java
│   └── AnnotationResult.java
├── flow/                       # 三层操作流
│   ├── NormalFlow.java
│   ├── ExceptionFlow.java
│   └── BusinessFlow.java
├── integration/                # 系统集成
│   ├── MemoryManager.java
│   ├── PropertyManager.java
│   └── FlowManager.java
├── generators/                 # 第4层：验证生成
│   ├── TypeChecker.java
│   ├── TypeChecker1.java
│   ├── CodeGenerator.java
│   └── GeneratedCode.java
├── context/                    # 上下文
│   ├── SemanticContext.java
│   └── StructureContext.java
└── common/                     # 通用组件
    └── Recyclable.java
```

---

## 4. 数据流

### 4.1 编译流程

```
           源代码 (.claw)
               │
               ▼
    ┌─────────────────────┐
    │   SourceScanner     │  扫描源码，创建 SourceView
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │   Preprocessor      │  预处理（宏展开、注释处理）
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │  PairingAnalyzer    │  配对分析（括号、引号配对）
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │  HierarchyBuilder   │  构建代码块层级结构
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │  EntityDecomposer   │  分解实体（函数、类型、变量）
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │     Tokenizer       │  词法分析，生成 Token 序列
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │   并行语义处理        │
    │ ┌─────────────────┐ │
    │ │ TypeProcessor   │ │
    │ │ FuncProcessor   │ │
    │ │ FlowProcessor   │ │
    │ │ DeclProcessor   │ │
    │ └─────────────────┘ │
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │      Parser         │  语法分析，生成 AST
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │  SemanticAnalyzer   │  语义分析
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │ AnnotationManager   │  处理系统/程序注解
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │    FlowManager      │  处理三层操作流
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │    TypeChecker      │  类型检查
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │   CodeGenerator     │  生成 ClawIR + 目标代码
    └──────────┬──────────┘
               │
               ▼
         CompilationResult
         ├── TargetCode (Java/Python/C)
         ├── IntermediateRepresentation
         ├── PseudoCode
         └── Metadata
```

### 4.2 核心数据结构

```java
// Token 类型
public enum TokenType {
    // 关键字
    KW_INT, KW_FLOAT, KW_STRING, KW_BOOL, KW_VOID, KW_ANY, KW_TYPE,
    KW_FUNCTION, KW_PUBLIC, KW_PRIVATE, KW_RETURN,
    KW_IF, KW_ELSE, KW_FOR, KW_WHILE, KW_BREAK, KW_CONTINUE,
    KW_IMPORT, KW_EXPORT, KW_CONST, KW_VAR,
    KW_TRUE, KW_FALSE, KW_NULL,
    KW_NORMAL, KW_EXCEPTION, KW_FLOW,
    KW_CATCH, KW_THROWS, KW_THROW,
    
    // 字面量
    LIT_INTEGER, LIT_FLOAT, LIT_STRING, LIT_CHAR,
    
    // 标识符
    IDENTIFIER,
    
    // 运算符
    OP_PLUS, OP_MINUS, OP_STAR, OP_SLASH, OP_PERCENT,
    OP_ASSIGN, OP_EQUAL, OP_NOT_EQUAL,
    OP_LESS, OP_LESS_EQUAL, OP_GREATER, OP_GREATER_EQUAL,
    OP_AND, OP_OR, OP_NOT,
    OP_PLUS_ASSIGN, OP_MINUS_ASSIGN,
    OP_ARROW, OP_DOT, OP_COLON, OP_COMMA, OP_SEMICOLON,
    
    // 括号
    OPEN_BRACE, CLOSE_BRACE,
    OPEN_PAREN, CLOSE_PAREN,
    OPEN_BRACKET, CLOSE_BRACKET,
    
    // 注解
    AT_SIGN, DOUBLE_AT_SIGN,
    
    // 特殊
    NEWLINE, WHITESPACE, EOF, UNKNOWN
}
```

---

## 5. 中间表示 (IR)

### 5.1 ClawIR 结构

```java
public class ClawIR {
    private final String moduleName;
    private final List<IRNode> nodes;           // IR节点列表
    private final List<IRAnnotation> annotations; // 注解列表
    private final FlowModel flowModel;          // 流模型
}
```

### 5.2 IR 节点类型

```java
public enum IRNodeType {
    // 类型定义
    TYPE_DEFINITION,
    
    // 函数
    FUNCTION_DECLARATION,
    PARAMETER,
    
    // 变量
    VARIABLE_DECLARATION,
    
    // 控制流
    IF_STATEMENT,
    FOR_LOOP,
    WHILE_LOOP,
    RETURN_STATEMENT,
    
    // 异常
    CATCH_BLOCK,
    THROW_STATEMENT,
    
    // 流控制
    FLOW_TO,
    
    // 导入导出
    IMPORT,
    EXPORT,
    
    // 表达式
    BINARY_EXPRESSION,
    UNARY_EXPRESSION,
    FUNCTION_CALL,
    MEMBER_ACCESS,
    ARRAY_ACCESS,
    
    // 字面量
    INTEGER_LITERAL,
    FLOAT_LITERAL,
    STRING_LITERAL,
    BOOLEAN_LITERAL,
    NULL_LITERAL
}
```

### 5.3 操作码 (OpCode)

```java
public enum OpCode {
    // 内存操作
    ALLOC, STORE_VAR, LOAD_VAR, LOAD_CONST,
    
    // 函数操作
    FUNC_DEF, FUNC_CALL, RETURN,
    
    // 控制流
    JUMP, JUMP_IF_TRUE, JUMP_IF_FALSE,
    
    // 作用域
    SCOPE_ENTER, SCOPE_EXIT,
    
    // 类型定义
    TYPE_DEF,
    
    // 导入导出
    IMPORT, EXPORT,
    
    // 注解钩子
    BEFORE_NAME_HOOK, AFTER_NAME_HOOK,
    BEFORE_PROPS_HOOK, AFTER_PROPS_HOOK,
    
    // 三层流
    NORMAL_FLOW_BEGIN, NORMAL_FLOW_END,
    EXCEPTION_CATCH, EXCEPTION_THROWS,
    FLOW_TO,
    
    // 属性操作
    PROP_GET, PROP_SET,
    
    // 数组操作
    ARRAY_NEW, ARRAY_GET, ARRAY_SET,
    
    // 元数据
    METADATA, NOP
}
```

---

## 6. 绑定层架构

### 6.1 绑定层接口

```java
// 目标代码生成器接口
public interface TargetCodeGenerator {
    TargetRuntime getRuntime();
    String generate(ClawIR ir);
    String getLanguageName();
    String getFileExtension();
    GenerationResult generate(ClawIR ir, GenerationConfig config);
}

// 目标运行时接口
public interface TargetRuntime {
    // 语言信息
    String getLanguageName();
    String getFileExtension();
    
    // 代码生成
    String generateFunctionHeader(String visibility, String returnType, 
                                  String name, List<String> params, 
                                  List<String> paramTypes);
    String generateFunctionFooter();
    String generateVariableDeclaration(boolean isConst, String type, 
                                       String name, String value);
    String generateReturn(String expression);
    
    // 流程控制
    String generateIf(String condition);
    String generateWhile(String condition);
    String generateFor(String init, String condition, String update);
    
    // 异常处理
    String generateTryBlock();
    String generateCatchBlock(String exceptionType, String varName, String body);
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
}

// 类型映射接口
public interface TypeMapper {
    String mapType(String clawType);
    String mapBoxedType(String clawType);
    String getDefaultValue(String clawType);
    boolean isPrimitive(String clawType);
}
```

### 6.2 目标语言对比

| 特性 | Java | Python | C |
|------|------|--------|---|
| 内存管理 | GC 自动 | GC 自动 | malloc/free 手动 |
| 异常机制 | try/catch 原生 | try/except 原生 | setjmp/longjmp 模拟 |
| flow-to | break LABEL 模拟 | FlowJumpException | goto 原生支持 |
| 类型系统 | 静态类型 | 动态+注解 | 静态类型 |
| 对象系统 | class 原生 | class 原生 | struct + 函数指针 |
| 析构函数 | AutoCloseable | __del__ | 手动调用 _destroy() |
| 块界定 | { } | 缩进 + : | { } |
| 语句终止 | ; | 无 | ; |
| 空值 | null | None | NULL |
| 布尔值 | true/false | True/False | true/false |

---

## 7. FFI 系统

### 7.1 FFI 架构

```
extern "C" { ... } 声明
        │
        ▼
┌─────────────────────────┐
│ ExternDeclarationParser │  第2层语义处理器
│ 解析 link/include/type  │
│ function/const          │
└─────────────────────────┘
        │
        ▼
┌─────────────────────────┐
│   FFIBindingTable       │  存储外部符号信息
│ - 库名列表              │
│ - 头文件列表            │
│ - 外部类型映射          │
│ - 外部函数签名          │
│ - 外部常量              │
└─────────────────────────┘
        │
        ▼
┌──────────────┬──────────────┬──────────────┐
│ C 目标       │ Python 目标  │ Java 目标    │
│ #include     │ ctypes.CDLL  │ JNI/Panama   │
│ 直接调用     │ argtypes     │ Linker       │
│ -l链接       │ find_library │ System.load  │
└──────────────┴──────────────┴──────────────┘
```

### 7.2 FFIBindingTable 数据结构

```java
FFIBindingTable
├── ExternBlock         // 一个 extern "C" { } 块
│   ├── links           // link "lib" 指令
│   ├── includes        // include "header.h" 指令
│   ├── types           // type X = OpaquePointer
│   ├── functions       // function foo(a: Int) -> Int
│   └── constants       // const X: Int = 0
├── ExternFunction      // 外部函数
│   ├── name, params, returnType, isVariadic
├── ExternParam         // 函数参数
│   ├── name, type
├── ExternType          // 外部类型
│   ├── clawTypeName, cMappingType
├── ExternConstant      // 外部常量
│   ├── name, type, value
└── LinkDirective       // 链接指令
    ├── libraryName, headerFile
```

### 7.3 标准库映射

| Claw 导入 | C 映射 | 说明 |
|-----------|--------|------|
| import std.io | #include <stdio.h> | 标准 I/O |
| import std.string | #include <string.h> | 字符串处理 |
| import std.math | #include <math.h> | 数学函数 |
| import std.memory | #include <stdlib.h> | 内存管理 |
| import std.bool | #include <stdbool.h> | 布尔类型 |
| import std.time | #include <time.h> | 时间处理 |

---

## 8. 设计模式

### 8.1 使用的设计模式

| 模式 | 应用位置 | 说明 |
|------|----------|------|
| **Pipeline** | CompilationPipeline | 编译流程阶段化 |
| **Strategy** | TargetCodeGenerator | 多目标语言生成策略 |
| **Factory** | CodeGenerator | 根据配置创建生成器 |
| **Observer** | AnnotationManager | 注解事件监听 |
| **Command** | BlockProcessor | 块处理命令封装 |
| **Visitor** | ASTNode | AST 节点遍历 |
| **Builder** | IRNode | IR 节点构建 |

### 8.2 解耦层次

```
┌─────────────────────────────────────────────────────┐
│              编译器前端（不变）                        │
│  Scanner → Pairer → Hierarchy → Processors → IR Gen │
│  产出：ClawIR（语言无关中间表示）                      │
└──────────────────────┬──────────────────────────────┘
                       │
              ClawIR 接口边界
                       │
        ┌──────────────┼──────────────┐
        │              │              │
   ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
   │  Java   │   │ Python  │   │   C     │
   │ Binding │   │ Binding │   │ Binding │
   ├─────────┤   ├─────────┤   ├─────────┤
   │CodeGen  │   │CodeGen  │   │CodeGen  │
   │TypeMap  │   │TypeMap  │   │TypeMap  │
   │Runtime  │   │Runtime  │   │Runtime  │
   └─────────┘   └─────────┘   └─────────┘
```

---

## 9. 性能特性

### 9.1 编译性能

| 指标 | 值 |
|------|------|
| 编译速度 | ~2500 函数/秒 |
| 100 函数编译 | <5 秒 |
| 内存占用 | 最小化（可回收组件） |
| 可扩展性 | 高效处理大型代码库 |

### 9.2 性能优化策略

1. **并行处理**: 第2层语义处理器可并行执行
2. **对象复用**: Recyclable 接口支持对象池
3. **惰性求值**: IR 生成按需计算
4. **缓存**: 平台过滤结果缓存

---

## 10. 扩展机制

### 10.1 添加新目标语言

只需实现三个接口：

```java
// 1. 代码生成器
public class XxxCodeGenerator implements TargetCodeGenerator {
    // 实现 generate() 方法
}

// 2. 类型映射器
public class XxxTypeMapper implements TypeMapper {
    // 实现 mapType() 方法
}

// 3. 运行时支持
public class XxxRuntime implements TargetRuntime {
    // 实现所有运行时方法
}
```

### 10.2 添加新处理器

```java
// 1. 语义处理器 (第2层)
public class XxxProcessor {
    public void processTokens(List<Token> tokens) {
        // 处理逻辑
    }
}

// 2. 块处理器 (第3层)
public class XxxBlockProcessor implements BlockProcessor {
    @Override
    public void process(CodeBlock block, ProcessContext context) {
        // 处理逻辑
    }
}
```

---

## 11. 依赖关系

### 11.1 外部依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| SLF4J | 2.0.9 | 日志接口 |
| Logback | 1.4.14 | 日志实现 |
| Lombok | 1.18.30 | 代码简化 |
| JUnit 5 | 5.10.1 | 单元测试 |

### 11.2 模块依赖

```
ClawCompiler
    │
    ├── CompilationPipeline
    │       ├── SourceScanner
    │       ├── Preprocessor
    │       ├── PairingAnalyzer
    │       ├── HierarchyBuilder
    │       ├── EntityDecomposer
    │       ├── Tokenizer
    │       ├── Parser
    │       ├── SemanticAnalyzer
    │       ├── TypeProcessor
    │       ├── FunctionProcessor
    │       ├── ControlFlowProcessor
    │       ├── DeclarationProcessor
    │       ├── LiteralProcessor
    │       ├── OperatorProcessor
    │       ├── AnnotationManager
    │       ├── MemoryManager
    │       ├── PropertyManager
    │       ├── FlowManager
    │       ├── TypeChecker
    │       └── CodeGenerator
    │
    └── TargetCodeGenerator (interface)
            ├── JavaCodeGenerator
            │       ├── JavaTypeMapper
            │       └── JavaRuntime
            ├── PythonCodeGenerator
            │       ├── PythonTypeMapper
            │       └── PythonRuntime
            └── CCodeGenerator
                    ├── CTypeMapper
                    └── CRuntime
```

---

## 12. 文件结构

```
claw-compiler-java/
├── pom.xml                              # Maven 配置
├── readme.md                            # 项目说明
├── ARCHITECTURE.md                      # 本文档
├── DEVELOPMENT.md                       # 开发指南
├── ROADMAP.md                           # 发展路线图
├── bind-std.md                          # 标准库绑定说明
├── Claw 的 FFI 系统.md                   # FFI 系统文档
├── input.claw                           # 示例源码
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/claw/
│   │   │   │   ├── compiler/            # 编译器核心
│   │   │   │   ├── ir/                  # 中间表示
│   │   │   │   └── binding/             # 绑定层
│   │   │   └── claw/compiler/
│   │   │       └── binding/             # 绑定层接口
│   │   └── resources/
│   │       └── *.claw                   # 测试资源
│   └── test/
│       └── java/
│           └── com/claw/compiler/
│               └── ClawCompilerTest.java # 集成测试
└── target/                              # 构建输出
```

---

*文档版本: 3.0.0*
*最后更新: 2026-04-12*

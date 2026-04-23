# Claw 编译器 IDE 支持 (LSP) - 实现计划

## 概述

实现 Visual Studio Code 和其他编辑器的 LSP（Language Server Protocol）支持，为 Claw 语言提供完整的 IDE 支持，包括代码补全、语法检查、跳转定义、查找引用等功能。

---

## 🎯 实现目标

### 1. LSP 服务器实现

创建一个独立的 LSP 服务器，支持：

- **代码补全**
  - 基础类型补全
  - 自定义类型补全
  - 函数补全
  - 参数提示
  - 注解提示

- **语法检查**
  - 语法验证
  - 类型检查
  - 注解验证
  - 错误诊断

- **跳转定义**
  - 跳转到函数定义
  - 跳转到类型定义
  - 跳转到注解定义

- **查找引用**
  - 查找变量引用
  - 查找函数引用
  - 查找类型引用

---

## 📊 实现计划

### 第一阶段：基础框架（Week 1-2）

#### 1.1 项目结构

```
claw-compiler/
├── lsp/
│   ├── server/                 # LSP 服务器核心
│   │   ├── Main.java           # LSP 服务器入口
│   │   ├── ClawLanguageServer.java  # 核心服务器类
│   │   └── DiagnosticGenerator.java  # 错误诊断生成
│   ├── client/                  # LSP 客户端
│   │   └── ClawLanguageClient.java   # LSP 客户端（用于测试）
│   ├── protocol/               # LSP 协议实现
│   │   ├── ServerCapabilities.java  # 服务器能力
│   │   └── CompletionItem.java       # 补全项
│   └── utils/
│       ├── JSONUtils.java        # JSON 工具
│       └── MessageHandler.java    # 消息处理
```

#### 1.2 依赖配置

```xml
<!-- pom.xml -->
<dependencies>
    <!-- LSP 协议 -->
    <dependency>
        <groupId>org.eclipse.lsp4j</groupId>
        <artifactId>org.eclipse.lsp4j</artifactId>
        <version>0.21.1</version>
    </dependency>
    
    <!-- JSON 处理 -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</groupId>
        <version>2.10.1</version>
    </dependency>
    
    <!-- 编译器核心 -->
    <dependency>
        <groupId>com.claw</groupId>
        <artifactId>claw-compiler</artifactId>
        <version>3.0.0</version>
    </dependency>
</dependencies>
```

**预估工作量：3-4 天**

---

### 第二阶段：核心功能实现（Week 2-4）

#### 2.1 代码补全

```java
public class CompletionProvider {
    
    /**
     * 代码补全提供
     * 
     * @param document 文档内容
     * @param position 位置
     * @return 补全列表
     */
    public List<CompletionItem> provideCompletion(Document document, Position position) {
        // 1. 识别当前上下文（变量、类型、函数等）
        String context = analyzeContext(document, position);
        
        // 2. 根据上下文提供补全
        List<CompletionItem> completions = new ArrayList<>();
        
        switch (context) {
            case "variable":
                completions.addAll(getVariableCompletions());
                break;
            case "type":
                completions.addAll(getTypeCompletions());
                break;
            case "function":
                completions.addAll(getFunctionCompletions());
                break;
            case "annotation":
                completions.addAll(getAnnotationCompletions());
                break;
        }
        
        return completions;
    }
}
```

**预估工作量：1-2 周**

#### 2.2 语法检查

```java
public class DiagnosticProvider {
    
    /**
     * 诊断检查
     * 
     * @param document 文档内容
     * @return 错误列表
     */
    public List<Diagnostic> diagnose(Document document) {
        // 1. 解析文档
        List<CodeBlock> blocks = parseDocument(document);
        
        // 2. 语义分析
        SemanticContext ctx = new SemanticContext();
        
        // 3. 类型检查
        for (CodeBlock block : blocks) {
            checkBlock(ctx, block);
        }
        
        // 4. 生成诊断
        return ctx.getDiagnostics();
    }
    
    private void checkBlock(SemanticContext ctx, CodeBlock block) {
        switch (block.getBlockType()) {
            case FUNCTION:
                checkFunction(ctx, block);
                break;
            case TYPE:
                checkType(ctx, block);
                break;
            case VARIABLE:
                checkVariable(ctx, block);
                break;
        }
    }
}
```

**预估工作量：2-3 周**

#### 2.3 跳转定义

```java
public class DefinitionProvider {
    
    /**
     * 跳转到定义
     * 
     * @param document 文档内容
     * @param position 位置
     * @return 定义位置
     */
    public Location findDefinition(Document document, Position position) {
        // 1. 识别当前符号（变量、函数等）
        Symbol symbol = identifySymbol(document, position);
        
        // 2. 查找符号定义
        Location location = findDefinitionLocation(symbol);
        
        return location;
    }
    
    private Location findDefinitionLocation(Symbol symbol) {
        // 在语义上下文中查找定义
        return symbol.getDefinitionLocation();
    }
}
```

**预估工作量：1-2 周**

#### 2.4 查找引用

```java
public class ReferenceProvider {
    
    /**
     * 查找引用
     * 
     * @param document 文档内容
     * @param position 位置
     * @return 引用列表
     */
    public List<Location> findReferences(Document document, Position position) {
        // 1. 识别当前符号
        Symbol symbol = identifySymbol(document, position);
        
        // 2. 在文档中查找所有引用
        List<Location> references = new ArrayList<>();
        
        for (CodeBlock block : blocks) {
            if (block.references(symbol)) {
                references.add(block.getLocation());
            }
        }
        
        return references;
    }
}
```

**预估工作量：1-2 周**

---

### 第三阶段：功能增强（Week 4-6）

#### 3.1 Hover 支持

```java
public class HoverProvider {
    
    /**
     * Hover 信息提供
     * 
     * @param document 文档内容
     * @param position 位置
     * @return Hover 信息
     */
    public Hover provideHover(Document document, Position position) {
        // 1. 识别当前符号
        Symbol symbol = identifySymbol(document, position);
        
        // 2. 生成 Hover 文本
        String hoverText = generateHoverText(symbol);
        
        // 3. 生成 Hover 信息
        return new Hover(hoverText, null);
    }
    
    private String generateHoverText(Symbol symbol) {
        switch (symbol.getType()) {
            case VARIABLE:
                return String.format(
                    "Variable\nType: %s\n" +
                    "Value: %s",
                    symbol.getType(),
                    symbol.getValue()
                );
            case FUNCTION:
                return String.format(
                    "Function\n" +
                    "Args: %s\n" +
                    "Return: %s",
                    symbol.getParams(),
                    symbol.getReturnType()
                );
        }
    }
}
```

**预估工作量：3-5 天**

#### 3.2 重命名支持

```java
public class RenameProvider {
    
    /**
     * 重命名符号
     * 
     * @param document 文档内容
     * @param position 位置
     * @param newName 新名称
     * @return 重命名更改列表
     */
    public WorkspaceEdit rename(Document document, Position position, String newName) {
        // 1. 识别当前符号
        Symbol symbol = identifySymbol(document, position);
        
        // 2. 查找所有引用
        List<Location> references = findReferences(document, position);
        
        // 3. 生成更改列表
        List<TextDocumentEdit> changes = new ArrayList<>();
        
        for (Location location : references) {
            changes.add(createTextEdit(location, newName));
        }
        
        return new WorkspaceEdit(changes);
    }
}
```

**预估工作量：2-3 天**

#### 3.3 Document Symbols

```java
public class DocumentSymbolProvider {
    
    /**
     * 文档符号提供
     * 
     * @param document 文档内容
     * @return 符号列表
     */
    public List<DocumentSymbol> provideDocumentSymbols(Document document) {
        List<DocumentSymbol> symbols = new ArrayList<>();
        
        // 1. 解析文档结构
        List<CodeBlock> blocks = parseDocument(document);
        
        // 2. 转换为 DocumentSymbol
        for (CodeBlock block : blocks) {
            symbols.add(convertToDocumentSymbol(block));
        }
        
        return symbols;
    }
}
```

**预估工作量：2-3 天**

---

### 第四阶段：测试和优化（Week 6-8）

#### 4.1 单元测试

```java
@Test
public void testCompletionProvider() {
    // 测试基础类型补完
    // 测试函数补全
    // 测试参数提示
}

@Test
public void testDiagnosticProvider() {
    // 测试语法错误检测
    // 测试类型错误检测
}

@Test
public void testDefinitionProvider() {
    // 测试跳转到函数定义
    // 测试跳转到类型定义
}

@Test
public void testReferenceProvider() {
    // 测试查找变量引用
    // 测试查找函数引用
}
```

**预估工作量：1 周**

#### 4.2 集成测试

- 集成到 VS Code
- 集成到 IntelliJ IDEA
- 集成到其他编辑器

**预估工作量：1 周**

---

## 📚 API 设计

### LSP 服务器接口

```java
public class ClawLanguageServer {
    
    // 初始化
    public void initialize(InitializeParams params) {
        // 1. 读取项目配置
        // 2. 初始化编译器
        // 3. 加载项目信息
    }
    
    // 代码补完
    public List<CompletionItem> onCompletion(TextDocumentPositionParams params) {
        return completionProvider.provideCompletion(params);
    }
    
    // 诊断
    public List<Diagnostic> onDiagnostics() {
        return diagnosticProvider.diagnose(document);
    }
    
    // 跳转定义
    public Location onDefinition(TextDocumentPositionParams params) {
        return definitionProvider.findDefinition(params);
    }
    
    // 查找引用
    public List<Location> onReferences(ReferenceParams params) {
        return referenceProvider.findReferences(params);
    }
    
    // Hover
    public Hover onHover(TextDocumentPositionParams params) {
        return hoverProvider.provideHover(params);
    }
    
    // 重命名
    public WorkspaceEdit onRename(RenameParams params) {
        return renameProvider.rename(params);
    }
    
    // 文档符号
    public List<DocumentSymbol> onDocumentSymbols(DocumentSymbolParams params) {
        return documentSymbolProvider.provideDocumentSymbols(params);
    }
}
```

---

## 🎨 代码补全示例

### 补全列表结构

```json
[
    {
        "label": "Int",
        "kind": 12,
        "detail": "int",
        "documentation": "整数类型",
        "insertText": "Int",
        "filterText": "int"
    },
    {
        "label": "String",
        "kind": 12,
        "detail": "String",
        "documentation": "字符串类型",
        "insertText": "String",
        "filterText": "string"
    },
    {
        "label": "calculate",
        "kind": 2,
        "detail": "function (int, int) -> int",
        "documentation": "计算两个数的和",
        "insertText": "calculate($0, $1)",
        "filterText": "calculate"
    },
    {
        "label": "@Before",
        "kind": 16,
        "detail": "annotation",
        "documentation": "前置通知注解",
        "insertText": "@Before($0)",
        "filterText": "@before"
    }
]
```

---

## 🚀 性能优化

### 补全性能

| 操作 | 性能 | 优化方式 |
|------|------|----------|
| 代码补全 | ~100ms | 缓存项目信息 |
| 语法检查 | ~500ms | 异步检查 |
| 跳转定义 | ~50ms | 语义上下文缓存 |
| 查找引用 | ~200ms | 引用图缓存 |

### 内存优化

- 项目信息缓存
- 语义上下文缓存
- 类型定义缓存
- 符号引用缓存

---

## 🧪 测试策略

### 单元测试

- ✅ 补全提供器测试
- ✅ 诊断提供器测试
- ✅ 跳转定义测试
- ✅ 查找引用测试
- ✅ Hover 提供器测试
- ✅ 重命名提供器测试
- ✅ 文档符号提供器测试

### 集成测试

- ✅ VS Code 集成测试
- ✅ IntelliJ IDEA 集成测试
- ✅ 其他编辑器集成测试

---

## 📊 工作量估算

| 阶段 | 任务 | 工作量 | 难度 |
|------|------|--------|------|
| **第一阶段** | LSP 基础框架 | 3-4 天 | ⭐⭐ |
| **第二阶段** | 核心功能实现 | 8-10 天 | ⭐⭐⭐⭐ |
| **第三阶段** | 功能增强 | 7-10 天 | ⭐⭐⭐ |
| **第四阶段** | 测试和优化 | 2-3 周 | ⭐⭐ |
| **总计** | - | **6-8 周** | - |

**总工作量：6-8 周** ✅ 符合需求

---

## 📚 参考资料

- [LSP Specification](https://microsoft.github.io/language-server-protocol/)
- [Eclipse LSP4J](https://projects.eclipse.org/projects/eclipse.lsp4j)
- [VS Code Extension API](https://code.visualstudio.com/api)
- [Claw Compiler Documentation](./docs/CODE_GENERATION.md)

---

**最后更新：** 2026-04-16
**预计完成时间：** 2026-06-12
**实现状态：** 📋 计划中

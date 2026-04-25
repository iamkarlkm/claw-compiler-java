package com.q3lives.lsp.provider;

import com.q3lives.compiler.context.SemanticContext;
import org.eclipse.lsp4j.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LSP Provider 单元测试
 *
 * 验证各 Provider 的核心功能行为。
 */
class LspProviderTest {

    private SemanticContext semanticContext;

    @BeforeEach
    void setUp() {
        semanticContext = new SemanticContext();
    }

    // ==================== CompletionProvider 测试 ====================

    @Test
    void testCompletionProviderReturnsItems() {
        CompletionProvider provider = new CompletionProvider(semanticContext, null);
        String doc = "function main() {\n    var x: Int = 0;\n}";
        Position pos = new Position(1, 4); // 第2行第4列

        CompletionList result = provider.provideCompletion(doc, pos);
        assertNotNull(result);
        assertFalse(result.getItems().isEmpty(), "补全列表不应为空");
    }

    @Test
    void testCompletionAtAnnotationContext() {
        CompletionProvider provider = new CompletionProvider(semanticContext, null);
        String doc = "function main() {\n    @\n}";
        Position pos = new Position(1, 5); // @ 之后

        CompletionList result = provider.provideCompletion(doc, pos);
        assertNotNull(result);
        // @ 上下文应触发注解补全（至少包含一些补全项）
        assertFalse(result.getItems().isEmpty());
    }

    @Test
    void testCompletionAllItems() {
        CompletionProvider provider = new CompletionProvider(semanticContext, null);
        List<com.q3lives.lsp.protocol.CompletionItem> all = provider.getAllCompletions();
        assertFalse(all.isEmpty(), "应有补全项");
        // 至少包含类型、函数、关键字
        assertTrue(all.stream().anyMatch(i -> i.getLabel().equals("Int")));
        assertTrue(all.stream().anyMatch(i -> i.getLabel().equals("print")));
        assertTrue(all.stream().anyMatch(i -> i.getLabel().equals("function")));
    }

    // ==================== EnhancedCompletionProvider 测试 ====================

    @Test
    void testEnhancedCompletionWithSnippet() {
        EnhancedCompletionProvider provider = new EnhancedCompletionProvider(semanticContext, null);
        String doc = "fun";
        Position pos = new Position(0, 3); // "fun" 之后

        CompletionList result = provider.provideCompletion(doc, pos);
        assertNotNull(result);
        assertFalse(result.getItems().isEmpty(), "代码片段上下文应有补全项");
    }

    @Test
    void testEnhancedCompletionForEmptyDocument() {
        EnhancedCompletionProvider provider = new EnhancedCompletionProvider(semanticContext, null);
        CompletionList result = provider.provideCompletion("", new Position(0, 0));
        assertNotNull(result);
        // 空文档应返回所有补全项
        assertFalse(result.getItems().isEmpty());
    }

    // ==================== DiagnosticProvider 测试 ====================

    @Test
    void testDiagnosticProviderBracketMismatch() {
        DiagnosticProvider provider = new DiagnosticProvider(semanticContext, null);
        TextDocumentItem doc = createDoc("function main( { }"); // 括号不匹配

        List<Diagnostic> diagnostics = provider.diagnose(doc);
        assertNotNull(diagnostics);
        assertTrue(diagnostics.stream().anyMatch(d -> d.getMessage().contains("括号")),
            "应检测到未闭合的括号");
    }

    @Test
    void testDiagnosticProviderQuoteMismatch() {
        // 注：当前实现仅在单引号和双引号同时未闭合时才报告错误
        DiagnosticProvider provider = new DiagnosticProvider(semanticContext, null);
        TextDocumentItem doc = createDoc("var s = \"unclosed' string;"); // 单双引号都未闭合

        List<Diagnostic> diagnostics = provider.diagnose(doc);
        assertNotNull(diagnostics);
        assertTrue(diagnostics.stream().anyMatch(d -> d.getMessage().contains("字符串")),
            "应检测到未闭合的字符串");
    }

    @Test
    void testDiagnosticProviderNoErrorsForValidCode() {
        DiagnosticProvider provider = new DiagnosticProvider(semanticContext, null);
        TextDocumentItem doc = createDoc("function main() { }");

        List<Diagnostic> diagnostics = provider.diagnose(doc);
        assertNotNull(diagnostics);
        // 有效代码不应有括号/引号错误
        assertTrue(diagnostics.stream().noneMatch(d -> d.getMessage().contains("括号")),
            "有效的代码不应报括号错误");
    }

    // ==================== EnhancedDiagnosticProvider 测试 ====================

    @Test
    void testEnhancedDiagnosticWithCompilationErrors() {
        EnhancedDiagnosticProvider provider =
            new EnhancedDiagnosticProvider(semanticContext, null);
        String uri = "file:///test.claw";

        // 设置编译错误
        provider.setCompilationErrors(uri, List.of("第3行: 类型不匹配", "第5行: 未定义变量 x"));

        TextDocumentItem doc = createDoc("function main() {\n    var x: Int = \"hello\";\n}");
        doc.setUri(uri);

        List<Diagnostic> diagnostics = provider.diagnose(doc);
        assertNotNull(diagnostics);
        assertTrue(diagnostics.stream().anyMatch(d -> d.getMessage().contains("类型不匹配")),
            "应包含编译器报告的'类型不匹配'错误");
        assertTrue(diagnostics.stream().anyMatch(d -> d.getMessage().contains("未定义变量")),
            "应包含编译器报告的'未定义变量'错误");
    }

    @Test
    void testEnhancedDiagnosticClearErrors() {
        EnhancedDiagnosticProvider provider =
            new EnhancedDiagnosticProvider(semanticContext, null);
        String uri = "file:///test.claw";

        provider.setCompilationErrors(uri, List.of("error"));
        provider.clearCompilationErrors(uri);

        TextDocumentItem doc = createDoc("function main() { }");
        doc.setUri(uri);

        List<Diagnostic> diagnostics = provider.diagnose(doc);
        assertTrue(diagnostics.stream().noneMatch(d -> d.getMessage().contains("error")),
            "清除后不应再包含已清除的编译错误");
    }

    @Test
    void testEnhancedDiagnosticBracketCheck() {
        EnhancedDiagnosticProvider provider =
            new EnhancedDiagnosticProvider(semanticContext, null);
        TextDocumentItem doc = createDoc("function main() {\n    if (x > 0 {\n        return x;\n    }\n}");

        List<Diagnostic> diagnostics = provider.diagnose(doc);
        assertNotNull(diagnostics);
        assertTrue(diagnostics.stream().anyMatch(d -> d.getMessage().contains("括号")),
            "应检测到未闭合的括号");
    }

    // ==================== EnhancedDefinitionProvider 测试 ====================

    @Test
    void testDefinitionProviderFindsFunction() {
        EnhancedDefinitionProvider provider = new EnhancedDefinitionProvider(semanticContext);
        String doc = "function add(a: Int, b: Int) {\n    return a + b;\n}\nfunction main() {\n    var result = add(1, 2);\n}";
        Position pos = new Position(4, 20); // 光标在 "add" 上

        List<Location> locations = provider.provideDefinition(doc, pos);
        assertFalse(locations.isEmpty(), "应找到 add 函数的定义");
        // 定义应在第 0 行
        assertEquals(0, locations.get(0).getRange().getStart().getLine());
    }

    @Test
    void testDefinitionProviderFindsVariable() {
        EnhancedDefinitionProvider provider = new EnhancedDefinitionProvider(semanticContext);
        String doc = "var count: Int = 0;\nfunction main() {\n    count = 1;\n}";
        Position pos = new Position(2, 5); // 光标在 "count" 上

        List<Location> locations = provider.provideDefinition(doc, pos);
        assertFalse(locations.isEmpty(), "应找到 count 变量的定义");
        assertEquals(0, locations.get(0).getRange().getStart().getLine());
    }

    @Test
    void testDefinitionProviderNoResultForUnknownSymbol() {
        EnhancedDefinitionProvider provider = new EnhancedDefinitionProvider(semanticContext);
        String doc = "function main() {\n    unknownFunc();\n}";
        Position pos = new Position(1, 8); // 光标在 "unknownFunc" 上

        List<Location> locations = provider.provideDefinition(doc, pos);
        // 未知符号也应尝试查找，但可能找不到定义
        // 当前实现在找不到时返回空列表
        assertNotNull(locations);
    }

    @Test
    void testDefinitionProviderInvalidPosition() {
        EnhancedDefinitionProvider provider = new EnhancedDefinitionProvider(semanticContext);
        String doc = "function main() { }";
        Position pos = new Position(10, 0); // 超出文档范围

        List<Location> locations = provider.provideDefinition(doc, pos);
        assertTrue(locations.isEmpty(), "无效位置应返回空列表");
    }

    // ==================== HoverProvider 测试 ====================

    @Test
    void testHoverProviderReturnsInfo() {
        HoverProvider provider = new HoverProvider(semanticContext, null);
        String doc = "function add(a: Int, b: Int) {\n    return a + b;\n}\nfunction main() {\n    var result = add(1, 2);\n}";
        Position pos = new Position(4, 20); // 光标在 "add" 上

        Hover hover = provider.provideHover(doc, pos);
        assertNotNull(hover, "应返回悬停信息");
        assertNotNull(hover.getContents(), "悬停内容不应为空");
    }

    @Test
    void testHoverProviderInvalidLine() {
        HoverProvider provider = new HoverProvider(semanticContext, null);
        String doc = "function main() { }";
        Position pos = new Position(100, 0); // 超出范围

        Hover hover = provider.provideHover(doc, pos);
        assertNull(hover, "无效行号应返回 null");
    }

    @Test
    void testHoverProviderInvalidCharacter() {
        HoverProvider provider = new HoverProvider(semanticContext, null);
        String doc = "function main() { }";
        Position pos = new Position(0, 100); // 超出行长度

        Hover hover = provider.provideHover(doc, pos);
        assertNull(hover, "无效列号应返回 null");
    }

    // ==================== 辅助方法 ====================

    private TextDocumentItem createDoc(String content) {
        TextDocumentItem doc = new TextDocumentItem();
        doc.setUri("file:///test.claw");
        doc.setLanguageId("claw");
        doc.setVersion(1);
        doc.setText(content);
        return doc;
    }
}

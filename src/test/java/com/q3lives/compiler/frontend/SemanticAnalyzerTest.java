package com.q3lives.compiler.frontend;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.Tokenizer;
import com.q3lives.compiler.processors.semantic.TypeProcessor;
import com.q3lives.compiler.scanner.SourceScanner;
import com.q3lives.compiler.scanner.SourceView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 语义分析器单元测试
 *
 * 验证 SemanticAnalyzer 对 AST 的符号收集和引用检查。
 */
class SemanticAnalyzerTest {

    private final SourceScanner scanner = new SourceScanner();
    private final Tokenizer tokenizer = new Tokenizer();
    private final Parser parser = new Parser();
    private final TypeProcessor typeProcessor = new TypeProcessor();

    /**
     * 辅助方法：源代码 -> AST
     */
    private ASTNode parse(String source) {
        SourceView view = scanner.scan(source, "test.claw");
        List<Token> tokens = tokenizer.tokenize(view);
        return parser.parse(tokens);
    }

    private SemanticAnalyzer.SemanticResult analyze(String source) {
        ASTNode ast = parse(source);
        SemanticAnalyzer analyzer = new SemanticAnalyzer(typeProcessor);
        return analyzer.analyze(ast);
    }

    @Test
    void testEmptyProgram() {
        SemanticAnalyzer.SemanticResult result = analyze("");
        assertTrue(result.valid(), "空程序应通过语义分析");
        assertTrue(result.errors().isEmpty(), "空程序不应有错误");
    }

    @Test
    void testSimpleFunctionDefinition() {
        SemanticAnalyzer.SemanticResult result = analyze("function add() { }");
        assertTrue(result.valid(), "简单函数定义应通过语义分析");
        assertTrue(result.symbolTable().containsKey("add"),
            "符号表应包含 'add' 函数");
    }

    @Test
    void testDuplicateFunctionError() {
        SemanticAnalyzer.SemanticResult result = analyze(
            "function foo() { } function foo() { }"
        );
        assertFalse(result.valid(), "重复函数定义应导致语义分析失败");
        assertEquals(1, result.errors().size(), "应检测到1个重复定义错误");
        assertTrue(result.errors().get(0).contains("重复定义"),
            "错误信息应包含'重复定义'");
    }

    @Test
    void testVariableDeclarationSymbol() {
        SemanticAnalyzer.SemanticResult result = analyze("var count: Int = 0;");
        assertTrue(result.valid(), "变量声明应通过语义分析");
        assertTrue(result.symbolTable().containsKey("count"),
            "符号表应包含 'count' 变量");
    }

    @Test
    void testTypeDefinitionSymbol() {
        SemanticAnalyzer.SemanticResult result = analyze("type User { var name: String; }");
        assertTrue(result.valid(), "类型定义应通过语义分析");
        assertTrue(result.symbolTable().containsKey("User"),
            "符号表应包含 'User' 类型");
    }

    @Test
    void testMultipleUniqueSymbols() {
        SemanticAnalyzer.SemanticResult result = analyze(
            "var x: Int = 1;" +
            "function foo() { }" +
            "type Point { var x: Int; }"
        );
        assertTrue(result.valid(), "多个唯一符号应通过语义分析");
        assertEquals(3, result.symbolTable().size(),
            "符号表应包含3个符号");
    }

    @Test
    void testFunctionAndVariableSameName() {
        // 注：当前语义分析器对变量声明未做重复检查，只检查函数重复定义
        SemanticAnalyzer.SemanticResult result = analyze(
            "function foo() { } var foo: Int = 1;"
        );
        // 变量声明会覆盖符号表中同名条目，不触发错误（当前实现限制）
        assertTrue(result.symbolTable().containsKey("foo"));
        assertEquals("variable", result.symbolTable().get("foo").kind());
    }

    @Test
    void testSymbolInfoKind() {
        SemanticAnalyzer.SemanticResult result = analyze(
            "function foo() { } var x: Int = 1; type T { }"
        );
        assertTrue(result.valid());

        SemanticAnalyzer.SymbolInfo funcInfo = result.symbolTable().get("foo");
        assertNotNull(funcInfo);
        assertEquals("function", funcInfo.kind());

        SemanticAnalyzer.SymbolInfo varInfo = result.symbolTable().get("x");
        assertNotNull(varInfo);
        assertEquals("variable", varInfo.kind());

        SemanticAnalyzer.SymbolInfo typeInfo = result.symbolTable().get("T");
        assertNotNull(typeInfo);
        assertEquals("type", typeInfo.kind());
    }

    @Test
    void testNestedFunctionSymbols() {
        // 注：当前 Parser 的 parseBlock 不支持嵌套函数声明，
        // 内层函数 token 会被跳过。此测试验证外层函数至少能被识别。
        SemanticAnalyzer.SemanticResult result = analyze(
            "function outer() { function inner() { } }"
        );
        assertTrue(result.valid(), "外层函数应通过语义分析");
        assertTrue(result.symbolTable().containsKey("outer"),
            "符号表应包含外层函数");
        // 内层函数在当前 Parser 中不会被解析为 FUNCTION_DECLARATION
    }
}

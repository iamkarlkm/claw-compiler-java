package com.q3lives.compiler.frontend;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.Tokenizer;
import com.q3lives.compiler.scanner.SourceScanner;
import com.q3lives.compiler.scanner.SourceView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 语法分析器单元测试
 *
 * 验证 Parser 将 Token 流正确解析为 AST。
 */
class ParserTest {

    private final SourceScanner scanner = new SourceScanner();
    private final Tokenizer tokenizer = new Tokenizer();
    private final Parser parser = new Parser();

    /**
     * 辅助方法：源代码 -> Token 列表 -> AST
     */
    private ASTNode parse(String source) {
        SourceView view = scanner.scan(source, "test.claw");
        List<Token> tokens = tokenizer.tokenize(view);
        return parser.parse(tokens);
    }

    @Test
    void testEmptyProgram() {
        ASTNode program = parse("");
        assertEquals(ASTNode.NodeType.PROGRAM, program.getType());
        assertTrue(program.getChildren().isEmpty(), "空程序不应有子节点");
    }

    @Test
    void testSimpleFunction() {
        ASTNode program = parse("function add(a: Int, b: Int) -> Int { return a + b; }");
        assertEquals(ASTNode.NodeType.PROGRAM, program.getType());
        assertEquals(1, program.getChildren().size(), "应有1个顶层声明");

        ASTNode func = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.FUNCTION_DECLARATION, func.getType());
        assertEquals("add", func.getAttribute("name"));
    }

    @Test
    void testFunctionWithFlowModifier() {
        ASTNode program = parse("normal function process() { }");
        assertEquals(1, program.getChildren().size());

        ASTNode func = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.FUNCTION_DECLARATION, func.getType());
        assertEquals("normal", func.getAttribute("flowType"));
    }

    @Test
    void testFunctionWithVisibility() {
        ASTNode program = parse("public function getName() -> String { }");
        assertEquals(1, program.getChildren().size());

        ASTNode func = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.FUNCTION_DECLARATION, func.getType());
        assertEquals("public", func.getAttribute("visibility"));
        assertEquals("String", func.getAttribute("returnType"));
    }

    @Test
    void testVariableDeclaration() {
        ASTNode program = parse("var count: Int = 0;");
        assertEquals(1, program.getChildren().size());

        ASTNode varDecl = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.VARIABLE_DECLARATION, varDecl.getType());
    }

    @Test
    void testTypeDefinition() {
        ASTNode program = parse("type User { var name: String; var age: Int; }");
        assertEquals(1, program.getChildren().size());

        ASTNode typeDef = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.TYPE_DEFINITION, typeDef.getType());
    }

    @Test
    void testImportStatement() {
        ASTNode program = parse("import std.io;");
        assertEquals(1, program.getChildren().size());

        ASTNode importDecl = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.IMPORT_DECLARATION, importDecl.getType());
    }

    @Test
    void testAnnotation() {
        ASTNode program = parse("@@description(\"测试函数\") function test() { }");
        assertEquals(2, program.getChildren().size(), "应有注解和函数两个节点");

        ASTNode annotation = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.ANNOTATION, annotation.getType());
        assertEquals("system", annotation.getAttribute("category"));
        assertEquals("description", annotation.getAttribute("name"));
    }

    @Test
    void testMultipleTopLevelDeclarations() {
        ASTNode program = parse(
            "var x: Int = 10;" +
            "function foo() { }" +
            "type Point { var x: Int; }"
        );
        assertEquals(ASTNode.NodeType.PROGRAM, program.getType());
        assertEquals(3, program.getChildren().size(), "应有3个顶层声明");
    }

    @Test
    void testIfStatement() {
        // if 语句需要作为函数体的一部分才能被正确解析
        ASTNode program = parse("function test() { if (x > 0) { return x; } }");
        assertEquals(1, program.getChildren().size());

        ASTNode func = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.FUNCTION_DECLARATION, func.getType());
        // 函数体作为子节点存在
        assertFalse(func.getChildren().isEmpty(), "函数应有子节点（参数列表和函数体）");
    }

    @Test
    void testExportStatement() {
        ASTNode program = parse("export function publicFn() { }");
        assertEquals(1, program.getChildren().size());

        ASTNode func = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.FUNCTION_DECLARATION, func.getType());
    }

    // ==================== 泛型语法测试 ====================

    @Test
    void testGenericTypeUsageInVariable() {
        ASTNode program = parse("function main() { var x: Array<Int> = []; }");
        assertEquals(1, program.getChildren().size());

        ASTNode func = program.getChildren().get(0);
        ASTNode block = func.getChildByType(ASTNode.NodeType.BLOCK);
        assertNotNull(block);
        ASTNode varDecl = block.getChildren().get(0);
        assertEquals("Array<Int>", varDecl.getAttribute("type"));
    }

    @Test
    void testGenericTypeUsageInParameter() {
        ASTNode program = parse("function process(items: Array<String>) { }");
        assertEquals(1, program.getChildren().size());

        ASTNode func = program.getChildren().get(0);
        ASTNode params = func.getChildByType(ASTNode.NodeType.PARAMETER_LIST);
        assertNotNull(params);
        ASTNode param = params.getChildren().get(0);
        assertEquals("Array<String>", param.getAttribute("type"));
    }

    @Test
    void testGenericReturnType() {
        ASTNode program = parse("function getItems() -> Array<Int> { return []; }");
        assertEquals(1, program.getChildren().size());

        ASTNode func = program.getChildren().get(0);
        assertEquals("Array<Int>", func.getAttribute("returnType"));
    }

    @Test
    void testNestedGenericType() {
        ASTNode program = parse("function main() { var x: Map<String, Array<Int>> = []; }");
        assertEquals(1, program.getChildren().size());

        ASTNode func = program.getChildren().get(0);
        ASTNode block = func.getChildByType(ASTNode.NodeType.BLOCK);
        ASTNode varDecl = block.getChildren().get(0);
        assertEquals("Map<String, Array<Int>>", varDecl.getAttribute("type"));
    }

    @Test
    void testGenericTypeDefinition() {
        ASTNode program = parse("type Box<T> { var value: T; }");
        assertEquals(1, program.getChildren().size());

        ASTNode typeDef = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.TYPE_DEFINITION, typeDef.getType());
        assertEquals("Box", typeDef.getAttribute("name"));
        assertEquals("T", typeDef.getAttribute("typeParams"));
    }

    @Test
    void testGenericFunctionDefinition() {
        ASTNode program = parse("function map<T, U>(list: Array<T>) -> Array<U> { return []; }");
        assertEquals(1, program.getChildren().size());

        ASTNode func = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.FUNCTION_DECLARATION, func.getType());
        assertEquals("map", func.getAttribute("name"));
        assertEquals("T,U", func.getAttribute("typeParams"));
        assertEquals("Array<U>", func.getAttribute("returnType"));

        ASTNode params = func.getChildByType(ASTNode.NodeType.PARAMETER_LIST);
        assertNotNull(params);
        ASTNode param = params.getChildren().get(0);
        assertEquals("Array<T>", param.getAttribute("type"));
    }

    // ==================== AOP 语法测试 ====================

    @Test
    void testAspectDeclaration() {
        ASTNode program = parse("aspect Logging { function logBefore() { } }");
        assertEquals(1, program.getChildren().size());

        ASTNode aspect = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.AOP_ASPECT, aspect.getType());
        assertEquals("Logging", aspect.getAttribute("name"));
    }

    @Test
    void testAspectWithAdviceFunction() {
        String src = "aspect Metrics { @Before(\"execution(* *(..))\") function recordStart() { } }";
        ASTNode program = parse(src);
        assertEquals(1, program.getChildren().size());

        ASTNode aspect = program.getChildren().get(0);
        assertEquals(ASTNode.NodeType.AOP_ASPECT, aspect.getType());
        assertEquals("Metrics", aspect.getAttribute("name"));

        ASTNode block = aspect.getChildByType(ASTNode.NodeType.BLOCK);
        assertNotNull(block);
        // BLOCK 中应包含注解和函数声明
        assertFalse(block.getChildren().isEmpty(), "切面体内应有子节点");
    }
}

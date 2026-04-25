package com.q3lives.compiler.generators;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.Tokenizer;
import com.q3lives.compiler.frontend.ASTNode;
import com.q3lives.compiler.frontend.Parser;
import com.q3lives.compiler.processors.semantic.TypeProcessor;
import com.q3lives.compiler.scanner.SourceScanner;
import com.q3lives.compiler.scanner.SourceView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TypeChecker 单元测试
 *
 * 验证类型检查器对函数返回类型推断、变量类型检查等核心行为。
 */
class TypeCheckerTest {

    private final SourceScanner scanner = new SourceScanner();
    private final Tokenizer tokenizer = new Tokenizer();
    private final Parser parser = new Parser();
    private final TypeProcessor typeProcessor = new TypeProcessor();

    private ASTNode parse(String source) {
        SourceView view = scanner.scan(source, "test.claw");
        List<Token> tokens = tokenizer.tokenize(view);
        return parser.parse(tokens);
    }

    private TypeChecker.TypeCheckResult check(String source) {
        ASTNode ast = parse(source);
        TypeChecker checker = new TypeChecker(typeProcessor);
        return checker.check(ast);
    }

    // 辅助方法：从 AST 中找到第一个函数声明并读取其 returnType 属性
    private String getInferredReturnType(ASTNode ast) {
        return findFunctionReturnType(ast);
    }

    private String findFunctionReturnType(ASTNode node) {
        if (node == null) return null;
        if (node.getType() == ASTNode.NodeType.FUNCTION_DECLARATION) {
            return node.getProperty("returnType");
        }
        for (ASTNode child : node.getChildren()) {
            String rt = findFunctionReturnType(child);
            if (rt != null) return rt;
        }
        return null;
    }

    @Test
    void testInferVoidForNoReturn() {
        String src = "function foo() { }";
        ASTNode ast = parse(src);
        TypeChecker checker = new TypeChecker(typeProcessor);
        checker.check(ast);
        assertEquals("Void", getInferredReturnType(ast),
            "无 return 语句的函数应推断为 Void");
    }

    @Test
    void testInferIntForSingleIntReturn() {
        String src = "function foo() { return 42; }";
        ASTNode ast = parse(src);
        TypeChecker checker = new TypeChecker(typeProcessor);
        checker.check(ast);
        assertEquals("Int", getInferredReturnType(ast),
            "返回 Int 字面量的函数应推断为 Int");
    }

    @Test
    void testInferFloatForSingleFloatReturn() {
        String src = "function foo() { return 3.14; }";
        ASTNode ast = parse(src);
        TypeChecker checker = new TypeChecker(typeProcessor);
        checker.check(ast);
        assertEquals("Float", getInferredReturnType(ast),
            "返回 Float 字面量的函数应推断为 Float");
    }

    @Test
    void testInferStringForStringReturn() {
        String src = "function foo() { return \"hello\"; }";
        ASTNode ast = parse(src);
        TypeChecker checker = new TypeChecker(typeProcessor);
        checker.check(ast);
        assertEquals("String", getInferredReturnType(ast),
            "返回 String 字面量的函数应推断为 String");
    }

    @Test
    void testInferPromoteIntFloatToFloat() {
        String src = "function foo(x: Int) { if (x > 0) { return 1; } return 2.5; }";
        ASTNode ast = parse(src);
        TypeChecker checker = new TypeChecker(typeProcessor);
        checker.check(ast);
        assertEquals("Float", getInferredReturnType(ast),
            "Int 与 Float 混合返回应提升为 Float");
    }

    @Test
    void testIncompatibleReturnTypesFallBackToAny() {
        String src = "function foo(x: Int) { if (x > 0) { return 1; } return \"no\"; }";
        ASTNode ast = parse(src);
        TypeChecker checker = new TypeChecker(typeProcessor);
        TypeChecker.TypeCheckResult result = checker.check(ast);
        assertEquals("Any", getInferredReturnType(ast),
            "不兼容返回类型应回退为 Any");
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("返回类型不一致")),
            "应报告返回类型不一致错误");
    }

    @Test
    void testNestedFunctionReturnNotAffectOuter() {
        // 内层函数返回 String，外层函数无 return，外层应推断为 Void
        String src = "function outer() { function inner() { return \"x\"; } }";
        ASTNode ast = parse(src);
        TypeChecker checker = new TypeChecker(typeProcessor);
        checker.check(ast);
        // 外层 outer
        String outerType = findNamedFunctionReturnType(ast, "outer");
        assertEquals("Void", outerType,
            "外层函数不应将内层函数的 return 计入推断");
    }

    @Test
    void testDeclaredReturnTypeNotOverwritten() {
        String src = "function foo() -> Int { return 42; }";
        ASTNode ast = parse(src);
        TypeChecker checker = new TypeChecker(typeProcessor);
        TypeChecker.TypeCheckResult result = checker.check(ast);
        assertTrue(result.isValid(), "声明类型与返回一致时应通过");
        assertEquals("Int", getInferredReturnType(ast),
            "已声明返回类型应保留");
    }

    @Test
    void testDeclaredReturnTypeMismatchReportsError() {
        String src = "function foo() -> Int { return \"hello\"; }";
        ASTNode ast = parse(src);
        TypeChecker checker = new TypeChecker(typeProcessor);
        TypeChecker.TypeCheckResult result = checker.check(ast);
        assertFalse(result.isValid(), "声明类型与实际返回不匹配时应失败");
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("返回类型不匹配")),
            "应报告返回类型不匹配错误");
    }

    @Test
    void testVariableDeclarationTypeInference() {
        String src = "function main() { var x = 42; }";
        TypeChecker.TypeCheckResult result = check(src);
        assertTrue(result.isValid(), "变量类型推断不应产生错误");
        assertTrue(result.getWarnings().isEmpty(),
            "有初始化值时不应有类型警告");
    }

    @Test
    void testConstAssignmentError() {
        String src = "function main() { const x: Int = 10; x = 20; }";
        TypeChecker.TypeCheckResult result = check(src);
        assertFalse(result.isValid(), "对 const 变量赋值应失败");
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("常量")),
            "应报告不能对常量赋值的错误");
    }

    // 辅助方法：按函数名查找 returnType
    private String findNamedFunctionReturnType(ASTNode node, String name) {
        if (node == null) return null;
        if (node.getType() == ASTNode.NodeType.FUNCTION_DECLARATION
                && name.equals(node.getProperty("name"))) {
            return node.getProperty("returnType");
        }
        for (ASTNode child : node.getChildren()) {
            String rt = findNamedFunctionReturnType(child, name);
            if (rt != null) return rt;
        }
        return null;
    }

    // ==================== 泛型类型测试 ====================

    @Test
    void testGenericVariableDeclaration() {
        String src = "function main() { var x: Array<Int> = []; }";
        TypeChecker.TypeCheckResult result = check(src);
        assertTrue(result.isValid(), "泛型类型声明不应产生错误");
    }

    @Test
    void testGenericReturnType() {
        String src = "function foo() -> Array<Int> { return []; }";
        ASTNode ast = parse(src);
        TypeChecker checker = new TypeChecker(typeProcessor);
        checker.check(ast);
        assertEquals("Array<Int>", getInferredReturnType(ast),
            "函数返回类型应保留泛型形式");
    }

    @Test
    void testGenericParameterType() {
        String src = "function foo(x: Box<String>) { }";
        ASTNode ast = parse(src);
        TypeChecker checker = new TypeChecker(typeProcessor);
        TypeChecker.TypeCheckResult result = checker.check(ast);
        assertTrue(result.isValid(), "泛型参数类型不应产生错误");
    }

    @Test
    void testNestedGenericType() {
        String src = "function main() { var x: Map<String, Array<Int>> = []; }";
        TypeChecker.TypeCheckResult result = check(src);
        assertTrue(result.isValid(), "嵌套泛型类型声明不应产生错误");
    }

    @Test
    void testGenericFunctionTypeParamVisible() {
        String src = "function identity<T>(value: T) -> T { return value; }";
        TypeChecker.TypeCheckResult result = check(src);
        assertTrue(result.isValid(), "泛型类型参数 T 在函数体内应可见");
    }

    @Test
    void testGenericTypeDefinitionTypeParamVisible() {
        String src = "type Box<T> { var value: T; }";
        TypeChecker.TypeCheckResult result = check(src);
        assertTrue(result.isValid(), "泛型类型参数 T 在类型体内应可见");
    }

    @Test
    void testGenericFunctionWithMultipleTypeParams() {
        String src = "function map<T, U>(list: Array<T>, fn: (T) -> U) -> Array<U> { return []; }";
        TypeChecker.TypeCheckResult result = check(src);
        assertTrue(result.isValid(), "多类型参数的泛型函数应通过类型检查");
    }
}

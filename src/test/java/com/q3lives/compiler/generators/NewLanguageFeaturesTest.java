package com.q3lives.compiler.generators;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.Tokenizer;
import com.q3lives.compiler.frontend.ASTNode;
import com.q3lives.compiler.frontend.Parser;
import com.q3lives.compiler.scanner.SourceScanner;
import com.q3lives.compiler.scanner.SourceView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 新语言特性解析测试
 */
class NewLanguageFeaturesTest {

    private final SourceScanner scanner = new SourceScanner();
    private final Tokenizer tokenizer = new Tokenizer();
    private final Parser parser = new Parser();

    private ASTNode parse(String source) {
        SourceView view = scanner.scan(source, "test.claw");
        List<Token> tokens = tokenizer.tokenize(view);
        return parser.parse(tokens);
    }

    // ================================================================
    // 1. match 表达式解析测试
    // ================================================================

    @Test
    void testMatchExpressionParsing() {
        String src = "function test(x) { match x { case 1 -> \"one\" case 2 -> \"two\" default -> \"other\" } }";
        ASTNode ast = parse(src);
        assertNotNull(ast);
    }

    // ================================================================
    // 2. 可选链解析测试
    // ================================================================

    @Test
    void testOptionalChainingParsing() {
        String src = "function test(x) { var y = x?.name; }";
        ASTNode ast = parse(src);
        assertNotNull(ast);
    }

    // ================================================================
    // 3. 空值合并解析测试
    // ================================================================

    @Test
    void testNullCoalesceParsing() {
        String src = "function test(x) { var y = x ?? \"default\"; }";
        ASTNode ast = parse(src);
        assertNotNull(ast);
    }

    // ================================================================
    // 4. 类型转换解析测试
    // ================================================================

    @Test
    void testTypeCastParsing() {
        String src = "function test(x) { var y = x as String; }";
        ASTNode ast = parse(src);
        assertNotNull(ast);
    }

    // ================================================================
    // 5. Lambda 表达式解析测试
    // ================================================================

    @Test
    void testLambdaExpressionParsing() {
        String src = "function test() { var f = |x| -> x + 1; }";
        ASTNode ast = parse(src);
        assertNotNull(ast);
    }

    // ================================================================
    // 6. 泛型类型参数边界解析测试
    // ================================================================

    @Test
    void testGenericBoundParsing() {
        // 类型边界 <T: Comparable>
        String src = "function foo<T: Comparable>(x: T) { }";
        ASTNode ast = parse(src);
        assertNotNull(ast);
    }
}
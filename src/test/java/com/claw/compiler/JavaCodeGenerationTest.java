package com.claw.compiler;

import com.claw.compiler.pipeline.CompilationResult;
import com.claw.compiler.pipeline.CompilationPipeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Java 代码生成测试
 *
 * 测试各种 Java 代码生成功能的正确性
 */
class JavaCodeGenerationTest {

    private final ClawCompiler compiler = new ClawCompiler();
    private final CompilationPipeline pipeline = new CompilationPipeline();

    // ==================== 类型推断测试 ====================

    @Test
    @DisplayName("测试基本类型映射")
    void testPrimitiveTypeMapping() {
        String source = """
                function test() -> Void {
                    var x: Int = 42
                    var y: Float = 3.14
                    var z: String = "hello"
                    var w: Bool = true
                }
                """;

        CompilationResult result = compiler.compile(source, "test_types.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试数组类型映射")
    void testArrayTypeMapping() {
        String source = """
                function test() -> Void {
                    var arr: Array<Int> = [1, 2, 3, 4, 5]
                    println(arr.length)
                }
                """;

        CompilationResult result = compiler.compile(source, "test_array.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试 Map 类型映射")
    void testMapTypeMapping() {
        String source = """
                function test() -> Int {
                    var map: Map<String, Int> = {"a": 1, "b": 2, "c": 3}
                    return map.size
                }
                """;

        CompilationResult result = compiler.compile(source, "test_map.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试 Set 类型映射")
    void testSetTypeMapping() {
        String source = """
                function test() -> Bool {
                    var set: Set<String> = {"apple", "banana", "cherry"}
                    return set.size > 0
                }
                """;

        CompilationResult result = compiler.compile(source, "test_set.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    // ==================== 注解钩子测试 ====================

    @Test
    @DisplayName("测试 @BeforeName 构造函数钩子")
    void testBeforeNameHook() {
        String source = """
                @BeforeName("init", "this")
                type Config {
                    var value: Int
                }

                function test() -> Void {
                    var config = Config()
                }
                """;

        CompilationResult result = compiler.compile(source, "test_before_name.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试 @AfterName 析构函数钩子")
    void testAfterNameHook() {
        String source = """
                @AfterName("cleanup", "this")
                type Resource {
                    var id: String
                }
                """;

        CompilationResult result = compiler.compile(source, "test_after_name.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    // ==================== 函数调用测试 ====================

    @Test
    @DisplayName("测试基本函数调用")
    void testFunctionCall() {
        String source = """
                function add(a: Int, b: Int) -> Int {
                    return a + b
                }

                function test() -> Int {
                    var result = add(10, 20)
                    return result
                }
                """;

        CompilationResult result = compiler.compile(source, "test_function_call.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试带参数的函数")
    void testFunctionWithParameters() {
        String source = """
                function calculate(a: Int, b: Int, c: Int) -> Int {
                    return a + b + c
                }

                function test() -> Int {
                    return calculate(1, 2, 3)
                }
                """;

        CompilationResult result = compiler.compile(source, "test_params.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    // ==================== 异常处理测试 ====================

    @Test
    @DisplayName("测试异常抛出")
    void testExceptionThrow() {
        String source = """
                function test() -> Void {
                    var x: Int = 0
                    if (x == 0) {
                        throw new ArithmeticError("除数不能为零")
                    }
                }
                """;

        CompilationResult result = compiler.compile(source, "test_exception.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试 try-catch 结构")
    void testTryCatch() {
        String source = """
                function test() -> Void {
                    try {
                        var x: Int = 0
                        if (x == 0) {
                            throw new ArithmeticError("错误")
                        }
                    } catch (ArithmeticError e) {
                        println("捕获到异常: " + e.message)
                    }
                }
                """;

        CompilationResult result = compiler.compile(source, "test_try_catch.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    // ==================== JavaDoc 注释测试 ====================

    @Test
    @DisplayName("测试 JavaDoc 生成")
    void testJavaDocGeneration() {
        String source = """
                @@description("这是一个测试函数", "Int,Int -> Int")
                @@param("x", "第一个数")
                @@param("y", "第二个数")
                @@return("两数之和")
                function add(x: Int, y: Int) -> Int {
                    return x + y
                }
                """;

        CompilationResult result = compiler.compile(source, "test_javadoc.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    // ==================== 复杂类型测试 ====================

    @Test
    @DisplayName("测试复杂类型定义")
    void testComplexType() {
        String source = """
                type Person {
                    var name: String
                    var age: Int
                    var email: String
                }

                function test() -> Void {
                    var person = Person()
                    person.name = "Alice"
                    person.age = 25
                    person.email = "alice@example.com"
                }
                """;

        CompilationResult result = compiler.compile(source, "test_complex.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    // ==================== 循环控制流测试 ====================

    @Test
    @DisplayName("测试 for 循环")
    void testForLoop() {
        String source = """
                function test() -> Int {
                    var sum = 0
                    var i = 0

                    for i from 1 to 10 {
                        sum = sum + i
                    }

                    return sum
                }
                """;

        CompilationResult result = compiler.compile(source, "test_for.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试 while 循环")
    void testWhileLoop() {
        String source = """
                function test() -> Int {
                    var count = 0
                    var num = 1

                    while (num <= 10) {
                        count = count + 1
                        num = num + 1
                    }

                    return count
                }
                """;

        CompilationResult result = compiler.compile(source, "test_while.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    // ==================== 多函数测试 ====================

    @Test
    @DisplayName("测试多个函数")
    void testMultipleFunctions() {
        String source = """
                function hello() -> String {
                    return "Hello, World!"
                }

                function square(x: Int) -> Int {
                    return x * x
                }

                function main() -> Void {
                    var msg = hello()
                    var sq = square(5)
                    println(msg)
                    println(sq)
                }
                """;

        CompilationResult result = compiler.compile(source, "test_multiple.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    // ==================== 综合测试 ====================

    @Test
    @DisplayName("测试完整示例")
    void testFullExample() {
        String source = """
                @@description("完整示例", "Void -> Void")

                type Config {
                    var maxRetries: Int
                    var timeout: Float
                    var verbose: Bool
                }

                function loadConfig() -> Config {
                    var config = Config()
                    config.maxRetries = 3
                    config.timeout = 30.0
                    config.verbose = true
                    return config
                }

                function processConfig(config: Config) -> Bool {
                    if (config.maxRetries < 0) {
                        throw new ValueError("maxRetries 不能为负数")
                    }
                    return true
                }

                function main() -> Void {
                    var config = loadConfig()
                    var success = processConfig(config)
                    println("配置处理成功: " + success)
                }
                """;

        CompilationResult result = compiler.compile(source, "test_full.claw");
        assertTrue(result.isSuccess(), "完整示例编译应该成功: " + result.getErrors());
    }
}

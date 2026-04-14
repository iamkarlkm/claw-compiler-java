package com.claw.compiler;

import com.claw.compiler.pipeline.CompilationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Java 类型映射测试
 *
 * 测试各种类型到 Java 类型的正确映射
 */
class JavaTypeMappingTest {

    private final ClawCompiler compiler = new ClawCompiler();

    @Test
    @DisplayName("测试 Byte 类型映射")
    void testByteType() {
        String source = """
                function test() -> Byte {
                    return 127 as Byte
                }
                """;

        CompilationResult result = compiler.compile(source, "test_byte.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试 Short 类型映射")
    void testShortType() {
        String source = """
                function test() -> Short {
                    return 32767 as Short
                }
                """;

        CompilationResult result = compiler.compile(source, "test_short.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试 Long 类型映射")
    void testLongType() {
        String source = """
                function test() -> Long {
                    return 9223372036854775807 as Long
                }
                """;

        CompilationResult result = compiler.compile(source, "test_long.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试 Char 类型映射")
    void testCharType() {
        String source = """
                function test() -> Char {
                    return 'A' as Char
                }
                """;

        CompilationResult result = compiler.compile(source, "test_char.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试 Float32 类型映射")
    void testFloat32Type() {
        String source = """
                function test() -> Float32 {
                    return 3.14159 as Float32
                }
                """;

        CompilationResult result = compiler.compile(source, "test_float32.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试 Optional 类型")
    void testOptionalType() {
        String source = """
                function test() -> Optional<String> {
                    var value: Optional<String>
                    if (true) {
                        value = Optional.of("hello")
                    }
                    return value
                }
                """;

        CompilationResult result = compiler.compile(source, "test_optional.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试函数类型")
    void testFunctionType() {
        String source = """
                function add(x: Int, y: Int) -> Int {
                    return x + y
                }

                function test() -> Int {
                    return add(1, 2)
                }
                """;

        CompilationResult result = compiler.compile(source, "test_function.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试元组类型")
    void testTupleType() {
        String source = """
                function test() -> Tuple<String, Int> {
                    return ("hello", 42)
                }
                """;

        CompilationResult result = compiler.compile(source, "test_tuple.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试 const 声明")
    void testConstDeclaration() {
        String source = """
                function test() -> Void {
                    const MAX_SIZE: Int = 100
                    const PI: Float = 3.14159
                    const ENABLED: Bool = true

                    if (MAX_SIZE > 50) {
                        println("启用")
                    }
                }
                """;

        CompilationResult result = compiler.compile(source, "test_const.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }
}

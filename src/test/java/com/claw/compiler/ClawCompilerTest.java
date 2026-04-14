// ==================== ClawCompilerTest.java ====================
package com.claw.compiler;

import com.claw.compiler.pipeline.CompilationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Claw 编译器集成测试
 */
class ClawCompilerTest {

    private final ClawCompiler compiler = new ClawCompiler();

    @Test
    @DisplayName("编译简单函数")
    void testSimpleFunction() {
        String source = """
                function hello() -> Void {
                    println("Hello, Claw!")
                }
                """;

        CompilationResult result = compiler.compile(source, "test_simple.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
        assertNotNull(result.getGeneratedCode());
        assertTrue(result.getGeneratedCode().getFunctionCount() >= 1);
    }

    @Test
    @DisplayName("编译带类型的变量声明")
    void testVariableDeclaration() {
        String source = """
                function test() -> Void {
                    var name: String = "Alice"
                    var age: Int = 25
                    const PI: Float = 3.14
                    var active: Bool = true
                }
                """;

        CompilationResult result = compiler.compile(source, "test_vars.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("编译控制流")
    void testControlFlow() {
        String source = """
                function checkAge(age: Int) -> String {
                    if (age > 18) {
                        return "adult"
                    } else {
                        return "minor"
                    }
                }
                
                function countdown(n: Int) -> Void {
                    var i = n
                    while (i > 0) {
                        println(i)
                        i = i - 1
                    }
                }
                """;

        CompilationResult result = compiler.compile(source, "test_control.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
        assertTrue(result.getGeneratedCode().getFunctionCount() >= 2);
    }

    @Test
    @DisplayName("编译类型定义")
    void testTypeDefinition() {
        String source = """
                type User {
                    var name: String
                    var age: Int
                    var email: String
                }
                
                function createUser(name: String, age: Int) -> User {
                    var user = User()
                    user.name = name
                    user.age = age
                    return user
                }
                """;

        CompilationResult result = compiler.compile(source, "test_type.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
        assertTrue(result.getGeneratedCode().getTypeCount() >= 1);
    }

    @Test
    @DisplayName("编译带系统注解的代码")
    void testSystemAnnotations() {
        String source = """
                @@description("计算两数之和", "Int,Int -> Int")
                @@param("a", "第一个数")
                @@param("b", "第二个数")
                @@return("两数之和")
                
                function add(a: Int, b: Int) -> Int {
                    return a + b
                }
                """;

        CompilationResult result = compiler.compile(source, "test_annotations.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
        assertFalse(result.getGeneratedCode().getMetadata().isEmpty());
    }

    @Test
    @DisplayName("编译带程序注解的代码")
    void testProgramAnnotations() {
        String source = """
                @BeforeName("init", "this")
                @AfterName("cleanup", "this")
                @BeforeProps("user.name,user.age")
                @AfterProps("user.email")
                
                function processUser(user: User) -> Void {
                    user.name = "Alice"
                    user.age = 25
                }
                """;

        CompilationResult result = compiler.compile(source, "test_prog_ann.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("编译三层操作流")
    void testThreeLayerFlow() {
        String source = """
                normal function process() -> Void {
                    var data = loadData()
                    
                    catch (IOError e) {
                        println("IO错误: " + e.message)
                    }
                    
                    flow to cleanup
                }
                """;

        CompilationResult result = compiler.compile(source, "test_flow.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("编译import声明")
    void testImport() {
        String source = """
                import utils { formatName, validatePhone }
                import models { User }
                
                function test() -> Void {
                    var name = formatName("Alice", "Smith")
                }
                """;

        CompilationResult result = compiler.compile(source, "test_import.claw");
        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("完整示例编译")
    void testFullExample() {
        String source = """
                @@description("完整示例", "Void -> Void")
                
                import utils { log }
                
                type Config {
                    var maxRetries: Int
                    var timeout: Float
                    var verbose: Bool
                }
                
                @BeforeName("initConfig", "this")
                @AfterName("destroyConfig", "this")
                
                normal function run(config: Config) -> Bool {
                    const MAX = 100
                    var count = 0
                    
                    while (count < MAX) {
                        if (count > config.maxRetries) {
                            break
                        }
                        count = count + 1
                    }
                    
                    catch (RuntimeError e) {
                        log("错误: " + e.message)
                        return false
                    }
                    
                    flow to done
                    
                    return true
                }
                
                public function main() -> Void {
                    var config = Config()
                    config.maxRetries = 3
                    config.timeout = 30.0
                    config.verbose = true
                    
                    var success = run(config)
                    println(success)
                }
                """;

        CompilationResult result = compiler.compile(source, "full_example.claw");
        assertTrue(result.isSuccess(), "完整示例编译应该成功: " + result.getErrors());

        assertNotNull(result.getGeneratedCode().getTargetCode());
        assertNotNull(result.getGeneratedCode().getIntermediateRepresentation());
        assertNotNull(result.getGeneratedCode().getPseudoCode());
        assertFalse(result.getGeneratedCode().getTargetCode().isEmpty());
        assertTrue(result.getGeneratedCode().getFunctionCount() >= 2);
        assertTrue(result.getGeneratedCode().getTypeCount() >= 1);

        System.out.println("=== 生成的目标代码 ===");
        System.out.println(result.getGeneratedCode().getTargetCode());
    }

    @Test
    @DisplayName("编译性能测试")
    void testPerformance() {
        StringBuilder source = new StringBuilder();
        source.append("@@description(\"性能测试\", \"Void -> Void\")");

        // 生成100个函数
        for (int i = 0; i < 100; i++) {
            source.append(String.format("""
                    function func_%d(x: Int, y: Int) -> Int {
                        var result = x + y
                        if (result > 100) {
                            return result
                        }
                        return 0
                    }
                    """, i));
        }

        long start = System.currentTimeMillis();
        CompilationResult result = compiler.compile(source.toString(), "perf_test.claw");
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(result.isSuccess(), "性能测试编译应该成功");
        System.out.printf("100个函数编译耗时: %dms%n", elapsed);
        assertTrue(elapsed < 5000, "编译100个函数应该在5秒内完成");
    }
}


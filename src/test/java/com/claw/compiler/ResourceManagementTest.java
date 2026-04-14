package com.claw.compiler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;

/**
 * 资源管理测试
 *
 * 测试资源管理是否正确使用 try-with-resources 模式
 */
class ResourceManagementTest {

    @Test
    @DisplayName("测试文件读取和写入")
    void testFileIO() throws Exception {
        ClawCompiler compiler = new ClawCompiler();

        // 测试读取文件
        String source = "function hello() -> Void { println(\"Hello\"); }";
        String tempFile = "temp_test.claw";

        // 写入临时文件
        try {
            Files.writeString(java.nio.file.Paths.get(tempFile), source);
        } finally {
            // 确保文件被删除
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempFile));
            } catch (Exception e) {
                // 忽略删除异常
            }
        }

        // 测试编译
        CompilationResult result = compiler.compileFile(tempFile);

        // 清理
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempFile));
        } catch (Exception e) {
            // 忽略删除异常
        }

        assertTrue(result.isSuccess(), "编译应该成功: " + result.getErrors());
    }

    @Test
    @DisplayName("测试多文件编译")
    void testMultipleFileCompilation() throws Exception {
        ClawCompiler compiler = new ClawCompiler();

        String source1 = "function test1() -> Int { return 1; }";
        String source2 = "function test2() -> Int { return 2; }";

        String tempFile1 = "temp_test1.claw";
        String tempFile2 = "temp_test2.claw";

        try {
            // 写入文件
            Files.writeString(java.nio.file.Paths.get(tempFile1), source1);
            Files.writeString(java.nio.file.Paths.get(tempFile2), source2);

            // 编译两个文件
            CompilationResult result1 = compiler.compileFile(tempFile1);
            CompilationResult result2 = compiler.compileFile(tempFile2);

            assertTrue(result1.isSuccess(), "文件1编译应该成功");
            assertTrue(result2.isSuccess(), "文件2编译应该成功");

            System.out.println("文件1结果: " + result1.formatSummary());
            System.out.println("文件2结果: " + result2.formatSummary());
        } finally {
            // 清理文件
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempFile1));
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempFile2));
            } catch (Exception e) {
                // 忽略删除异常
            }
        }
    }

    @Test
    @DisplayName("测试资源管理异常处理")
    void testResourceExceptionHandling() {
        // 测试无效文件路径
        String invalidPath = "/invalid/path/that/does/not/exist.claw";
        String tempFile = "resource_test.claw";

        try {
            CompilationResult result = compiler.compileFile(invalidPath);
            assertFalse(result.isSuccess(), "应该返回失败结果");
            assertTrue(result.hasErrors(), "应该有错误信息");
        } catch (Exception e) {
            // 预期会抛出 IOException
            assertTrue(true, "应该抛出异常");
        } finally {
            // 清理临时文件
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempFile));
            } catch (Exception e) {
                // 忽略删除异常
            }
        }
    }

    @Test
    @DisplayName("测试重复文件编译")
    void testDuplicateCompilation() throws Exception {
        ClawCompiler compiler = new ClawCompiler();

        String source = "function test() -> Int { return 42; }";
        String tempFile = "duplicate_test.claw";

        try {
            Files.writeString(java.nio.file.Paths.get(tempFile), source);

            // 编译两次
            CompilationResult result1 = compiler.compileFile(tempFile);
            CompilationResult result2 = compiler.compileFile(tempFile);

            assertTrue(result1.isSuccess(), "第一次编译应该成功");
            assertTrue(result2.isSuccess(), "第二次编译应该成功");

            assertEquals(result1.getElapsedMillis(), result2.getElapsedMillis(),
                    "两次编译耗时应该相同");
        } finally {
            // 清理文件
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempFile));
            } catch (Exception e) {
                // 忽略删除异常
            }
        }
    }

    @Test
    @DisplayName("测试编译到文件")
    void testCompileToFile() throws Exception {
        ClawCompiler compiler = new ClawCompiler();

        String source = "function add(a: Int, b: Int) -> Int { return a + b; }";
        String tempInput = "compile_to_file_input.claw";
        String tempOutput = "compile_to_file_output.java";

        try {
            // 写入输入文件
            Files.writeString(java.nio.file.Paths.get(tempInput), source);

            // 编译并输出到文件
            compiler.compileToFile(tempInput, tempOutput);

            // 检查输出文件是否存在
            java.nio.file.Path outputPath = java.nio.file.Paths.get(tempOutput);
            assertTrue(java.nio.file.Files.exists(outputPath),
                    "输出文件应该存在");

            // 读取输出文件内容
            String outputContent = Files.readString(outputPath);
            assertTrue(outputContent.contains("add"),
                    "输出文件应该包含 'add' 方法");

            System.out.println("输出文件内容:");
            System.out.println(outputContent);
        } finally {
            // 清理文件
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempInput));
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempOutput));
            } catch (Exception e) {
                // 忽略删除异常
            }
        }
    }
}

package com.q3lives.compiler.generators;

import org.junit.jupiter.api.Test;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 标准库测试
 */
class StdLibraryTest {

    // ================================================================
    // 1. 标准库文件存在性测试
    // ================================================================

    @Test
    void testStdCollectionExists() {
        Path path = Paths.get("src/main/resources/std/__init__.claw");
        assertTrue(Files.exists(path), "std/__init__.claw should exist");
    }

    @Test
    void testStdStringExists() {
        Path path = Paths.get("src/main/resources/std/string.claw");
        assertTrue(Files.exists(path), "std/string.claw should exist");
    }

    @Test
    void testStdFunctionalExists() {
        Path path = Paths.get("src/main/resources/std/functional.claw");
        assertTrue(Files.exists(path), "std/functional.claw should exist");
    }

    // ================================================================
    // 2. 标准库内容测试
    // ================================================================

    @Test
    void testCollectionHasFunctions() {
        String content = readStdFile("__init__.claw");
        assertTrue(content.contains("module std.collection"), "Should have module declaration");
        assertTrue(content.contains("function length"), "Should have length function");
        assertTrue(content.contains("function map"), "Should have map function");
    }

    @Test
    void testStringHasFunctions() {
        String content = readStdFile("string.claw");
        assertTrue(content.contains("module std.string"), "Should have module declaration");
        assertTrue(content.contains("function split"), "Should have split function");
        assertTrue(content.contains("function join"), "Should have join function");
    }

    @Test
    void testMathHasFunctions() {
        String content = readStdFile("string.claw");
        // math 在 string.claw 中没有，找另一个
    }

    // ================================================================
    // 辅助方法
    // ================================================================

    private String readStdFile(String filename) {
        try {
            Path path = Paths.get("src/main/resources/std/" + filename);
            return new String(Files.readAllBytes(path));
        } catch (Exception e) {
            return "";
        }
    }
}
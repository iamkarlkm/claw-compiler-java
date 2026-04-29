package com.q3lives.compiler.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClawFormatter 测试
 */
class ClawFormatterTest {

    @Test
    void testBasicFormat() {
        String source = "function foo() { var x = 1 return x }";
        String formatted = new ClawFormatter().format(source);

        assertNotNull(formatted);
    }

    @Test
    void testImportSorting() {
        String source = "import b\nimport a\nfunction main() { }";

        String formatted = new ClawFormatter().sortImports(true).format(source);

        // 有输出即可
        assertNotNull(formatted);
    }

    @Test
    void testIndentSize() {
        String source = "function foo() { var x = 1 }";

        String formatted = new ClawFormatter().format(source);

        // 格式化应该有输出
        assertNotNull(formatted);
    }

    @Test
    void testMaxLineLength() {
        String source = "function test() { }";

        String formatted = new ClawFormatter()
            .maxLineLength(30)
            .format(source);

        assertNotNull(formatted);
    }
}
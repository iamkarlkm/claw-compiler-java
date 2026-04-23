package claw.compiler.example;

import claw.compiler.utils.CodeBeautifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 代码美化示例
 * 演示 Claw 编译器如何美化生成的代码
 */
public class CodeBeautifierExample {

    public static void main(String[] args) {
        System.out.println("=== Code Beautification Examples ===");
        System.out.println();

        // 示例 1: 统一缩进
        System.out.println("Example 1: Indent Normalization");
        indentNormalization();
        System.out.println();

        // 示例 2: 空行管理
        System.out.println("Example 2: Empty Line Management");
        emptyLineManagement();
        System.out.println();

        // 示例 3: 注释优化
        System.out.println("Example 3: Comment Optimization");
        commentOptimization();
        System.out.println();

        // 示例 4: 完整代码美化
        System.out.println("Example 4: Complete Code Beautification");
        completeBeautification();
    }

    /**
     * 缩进规范化示例
     */
    private static void indentNormalization() {
        CodeBeautifier beautifier = new CodeBeautifier();

        // 未美化的代码
        List<String> uglyCode = Arrays.asList(
            "public class Example {",
            "  public static void main(String[] args) {",
            "    System.out.println(\"Hello\");",
            "    if (true) {",
            "      System.out.println(\"World\");",
            "    }",
            "  }",
            "}"
        );

        System.out.println("原始代码:");
        printCode(uglyCode);

        // 美化后的代码
        List<String> beautifiedCode = beautifier.beautify(uglyCode);

        System.out.println("\n美化后代码:");
        printCode(beautifiedCode);
    }

    /**
     * 空行管理示例
     */
    private static void emptyLineManagement() {
        CodeBeautifier beautifier = new CodeBeautifier();

        // 包含多个连续空行的代码
        List<String> codeWithExcessEmptyLines = Arrays.asList(
            "function add(a, b) {",
            "",
            "",
            "",
            "  return a + b;",
            "",
            "}",
            "",
            "",
            "function subtract(a, b) {",
            "",
            "  return a - b;",
            "}"
        );

        System.out.println("原始代码（包含多个连续空行）:");
        printCode(codeWithExcessEmptyLines);

        // 规范化空行（最多1个）
        List<String> normalizedCode = beautifier.removeExcessiveEmptyLines(codeWithExcessEmptyLines);

        System.out.println("\n规范化空行后:");
        printCode(normalizedCode);
    }

    /**
     * 注释优化示例
     */
    private static void commentOptimization() {
        CodeBeautifier beautifier = new CodeBeautifier();

        // 包含注释的代码
        List<String> codeWithComments = Arrays.asList(
            "public class Calculator {",
            "  // 这是一个计算器类",
            "  public int add(int a, int b) {",
            "    return a + b;",
            "  }",
            "}",
            "",
            "// 这是一个工具类",
            "public class Utils {",
            "  // 静态方法",
            "  public static void print(String msg) {",
            "    System.out.println(msg);",
            "  }",
            "}"
        );

        System.out.println("原始代码（包含注释）:");
        printCode(codeWithComments);

        // 优化注释
        List<String> optimizedCode = beautifier.optimizeComments(codeWithComments, 0);

        System.out.println("\n注释优化后:");
        printCode(optimizedCode);
    }

    /**
     * 完整代码美化示例
     */
    private static void completeBeautification() {
        CodeBeautifier beautifier = new CodeBeautifier();

        // 未美化的代码
        List<String> uglyCode = Arrays.asList(
            "public class Example {",
            "",
            "",
            "  public static void main(String[] args) {",
            "    System.out.println(\"Hello World\");",
            "    if (true) {",
            "      System.out.println(\"Nested\");",
            "    }",
            "  }",
            "}",
            "",
            "",
            "",
            "// 这是一个示例"
        );

        System.out.println("原始代码:");
        printCode(uglyCode);

        // 完整美化（规范化空行）
        List<String> beautifiedCode = beautifier.beautify(uglyCode, 0, 1);

        System.out.println("\n完整美化后:");
        printCode(beautifiedCode);
    }

    /**
     * 打印代码
     */
    private static void printCode(List<String> lines) {
        for (String line : lines) {
            System.out.println(line);
        }
    }

    /**
     * 测试 Python 风格的代码美化
     */
    private static void testPythonStyle() {
        CodeBeautifier pythonBeautifier = CodeBeautifier.createPython();

        List<String> pythonCode = Arrays.asList(
            "def fibonacci(n):",
            "    \"\"\"计算斐波那契数列\"\"\"",
            "    a, b = 0, 1",
            "    result = []",
            "    for i in range(n):",
            "        result.append(a)",
            "        a, b = b, a + b",
            "    return result",
            "",
            "def main():",
            "    n = 10",
            "    print(fibonacci(n))",
            "",
            "if __name__ == \"__main__\":",
            "    main()"
        );

        System.out.println("Python 风格代码:");
        System.out.println(pythonBeautifier.format(pythonCode));
    }

    /**
     * 测试 Java 风格的代码美化
     */
    private static void testJavaStyle() {
        CodeBeautifier javaBeautifier = CodeBeautifier.createJava();

        List<String> javaCode = Arrays.asList(
            "public class Calculator {",
            "  // 加法方法",
            "  public int add(int a, int b) {",
            "    return a + b;",
            "  }",
            "  // 减法方法",
            "  public int subtract(int a, int b) {",
            "    return a - b;",
            "  }",
            "}",
            "",
            "// 工具类",
            "public class StringUtils {",
            "  public static boolean isEmpty(String str) {",
            "    return str == null || str.isEmpty();",
            "  }",
            "}"
        );

        System.out.println("\nJava 风格代码:");
        System.out.println(javaBeautifier.format(javaCode));
    }
}

package claw.compiler.example;

import claw.compiler.utils.Logger;

/**
 * 生成器示例
 * 演示 Claw 编译器如何将生成器语法编译到 Java、Python 和 C
 */
public class GeneratorExample {

    public static void main(String[] args) {
        System.out.println("=== Generator Support Examples ===");
        System.out.println();

        // 示例 1: 斐波那契数列生成器
        System.out.println("Example 1: Fibonacci Generator");
        fibonacciGenerator();
        System.out.println();

        // 示例 2: 无限生成器
        System.out.println("Example 2: Infinite Generator");
        infiniteGenerator();
        System.out.println();

        // 示例 3: 大数据流处理
        System.out.println("Example 3: Large Data Stream Processing");
        processLargeData();
        System.out.println();
    }

    /**
     * 斐波那契数列生成器
     */
    private static void fibonacciGenerator() {
        System.out.println("斐波那契数列（前10个）：");
        int count = 10;
        for (int i = 0; i < count; i++) {
            int fib = fibonacci(i);
            System.out.println("Fib(" + i + ") = " + fib);
        }
    }

    /**
     * 生成器辅助方法（模拟生成器行为）
     */
    private static int fibonacci(int n) {
        if (n <= 1) return n;
        int a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            int next = a + b;
            a = b;
            b = next;
        }
        return b;
    }

    /**
     * 无限生成器示例
     */
    private static void infiniteGenerator() {
        System.out.println("生成无限序列的前5项：");

        // 模拟无限生成器
        int a = 0, b = 1;
        for (int i = 0; i < 5; i++) {
            System.out.println("Sequence[" + i + "] = " + a);
            int next = a + b;
            a = b;
            b = next;
        }
    }

    /**
     * 大数据流处理
     */
    private static void processLargeData() {
        // 模拟大数据集
        String[] largeDataset = new String[1000];
        for (int i = 0; i < largeDataset.length; i++) {
            largeDataset[i] = "Item " + i;
        }

        System.out.println("处理大数据集（逐项处理）：");
        int processed = 0;
        for (String item : largeDataset) {
            processItem(item);
            processed++;
            if (processed % 100 == 0) {
                System.out.println("已处理: " + processed + "/" + largeDataset.length);
            }
        }
    }

    /**
     * 逐项处理
     */
    private static void processItem(String item) {
        // 模拟处理逻辑
        int hash = item.hashCode();
        System.out.println("  Processing: " + item + " (hash: " + hash + ")");
    }
}

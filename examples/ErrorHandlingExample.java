package claw.compiler.example;

import claw.compiler.utils.*;

/**
 * 错误处理示例
 * 演示如何使用 Claw 编译器的错误处理系统
 */
public class ErrorHandlingExample {

    public static void main(String[] args) {
        // 示例 1: 配置日志级别
        System.out.println("=== 示例 1: 日志级别配置 ===");
        Logger.setLevel(LogLevel.DEBUG);
        Logger.info("日志级别已设置为 DEBUG");
        System.out.println();

        // 示例 2: 基本日志使用
        System.out.println("=== 示例 2: 基本日志使用 ===");
        Logger.trace("这是追踪日志");
        Logger.debug("这是调试日志");
        Logger.info("这是信息日志");
        Logger.warn("这是警告日志");
        Logger.error("这是错误日志");
        System.out.println();

        // 示例 3: 结构化日志
        System.out.println("=== 示例 3: 结构化日志 ===");
        Logger.beginBlock("处理文件");
        Logger.traceStack("开始读取文件: example.claw");
        Logger.info("文件读取成功，大小: " + 1024 + " 字节");
        Logger.endBlock("处理文件");
        System.out.println();

        // 示例 4: 运行时错误检查
        System.out.println("=== 示例 4: 运行时错误检查 ===");

        double numerator = 100;
        double divisor = 0;

        if (!ErrorHandler.checkDivisionByZero(divisor)) {
            Logger.warn("除零检查失败，跳过计算");
        } else {
            double result = numerator / divisor;
            Logger.info("计算结果: " + result);
        }
        System.out.println();

        // 示例 5: 空指针检查
        System.out.println("=== 示例 5: 空指针检查 ===");

        Object obj = null;
        if (!ErrorHandler.checkNull(obj, "obj")) {
            Logger.info("空指针检查失败，跳过后续操作");
        } else {
            obj.doSomething();
        }
        System.out.println();

        // 示例 6: 索引越界检查
        System.out.println("=== 示例 6: 索引越界检查 ===");

        String[] array = {"A", "B", "C"};
        int index = 5;

        if (!ErrorHandler.checkIndexBounds(index, array.length, "array")) {
            Logger.info("索引越界检查失败，跳过数组访问");
        } else {
            Logger.info("数组访问: array[" + index + "] = " + array[index]);
        }
        System.out.println();

        // 示例 7: 类型不匹配检查
        System.out.println("=== 示例 7: 类型不匹配检查 ===");

        String expected = "int";
        String actual = "string";

        if (!ErrorHandler.checkTypeMismatch(expected, actual)) {
            Logger.info("类型不匹配检查失败");
        } else {
            Logger.info("类型匹配: " + expected);
        }
        System.out.println();

        // 示例 8: 自定义错误记录
        System.out.println("=== 示例 8: 自定义错误记录 ===");

        ErrorContext.SourceLocation location =
            new ErrorContext.SourceLocation("example.claw", 42, 10, "x = 10 + 'abc'");

        ErrorContext context = ErrorHandler.recordError(
            ClawError.RUNTIME_ERROR,
            "自定义运行时错误",
            location,
            new Object[]{"x", "10"}
        );

        // 添加上下文信息
        context.addContext("变量名", "x");
        context.addContext("值", "10");
        context.addContext("操作", "+");

        // 设置根因分析
        context.setRootCause("整数不能直接与字符串相加");

        // 添加建议解决方案
        context.addSuggestion("使用 int.toString() 转换");
        context.addSuggestion("先转换为字符串再拼接");

        // 生成详细错误消息
        System.err.println(context.generateErrorMessage());
        System.out.println();

        // 示例 9: 错误报告
        System.out.println("=== 示例 9: 错误报告 ===");
        ErrorHandler.recordError(ClawError.UNDEFINED_VARIABLE, "变量 'y' 未定义", location);
        ErrorHandler.recordWarning("未使用的变量: 'unused'");
        ErrorHandler.recordWarning("潜在的类型转换问题");

        String report = ErrorHandler.generateErrorReport();
        if (report != null) {
            System.err.println(report);
        }
        System.out.println();

        // 示例 10: 性能追踪
        System.out.println("=== 示例 10: 性能追踪 ===");
        Logger.beginBlock("性能测试");

        for (int i = 0; i < 5; i++) {
            Logger.startOperation("操作 " + i);
            long start = System.nanoTime();
            // 模拟操作
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long duration = System.nanoTime() - start;
            Logger.endOperation("操作 " + i, duration / 1_000_000);
        }

        Logger.endBlock("性能测试");
        System.out.println();

        // 示例 11: 错误处理器状态检查
        System.out.println("=== 示例 11: 错误处理器状态检查 ===");
        System.out.println("错误数量: " + ErrorHandler.getErrorCount());
        System.out.println("致命错误: " + (ErrorHandler.hasFatalError() ? "是" : "否"));
        System.out.println("已忽略错误: " + (ErrorHandler.hasError() ? "是" : "否"));
        System.out.println();

        // 清空错误
        ErrorHandler.clearErrors();
        System.out.println("错误已清空，错误数量: " + ErrorHandler.getErrorCount());
    }

    // 模拟方法
    private static Object getObject() {
        return null;
    }

    private static int getIndex() {
        return 5;
    }

    private static void doSomething() {
        Logger.debug("执行操作");
    }

    private static void checkDivisionByZero() {
        // 模拟除零
        double divisor = 0;
        if (!ErrorHandler.checkDivisionByZero(divisor)) {
            throw new RuntimeException("除零检查失败");
        }
    }
}

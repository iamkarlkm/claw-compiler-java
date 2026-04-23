package claw.compiler.example;

import claw.compiler.utils.Logger;

/**
 * 装饰器示例
 * 演示 Claw 编译器如何将装饰器语法编译到 Java、Python 和 C
 */
public class DecoratorExample {

    public static void main(String[] args) {
        System.out.println("=== Decorator Support Examples ===");
        System.out.println();

        // 示例 1: 函数装饰器
        System.out.println("Example 1: Function Decorator - Logging");
        decoratedFunction(10, 20);
        System.out.println();

        // 示例 2: 类装饰器
        System.out.println("Example 2: Class Decorator - Timeout");
        timeoutDecoratorExample();
        System.out.println();

        // 示例 3: 装饰器链
        System.out.println("Example 3: Decorator Chain");
        decoratedFunctionWithMultipleDecorators(5, 3);
        System.out.println();
    }

    /**
     * 函数装饰器 - 日志记录
     * 对应 Claw: @log_function
     */
    private static void decoratedFunction(int x, int y) {
        Logger.beginBlock("Calculate: " + x + " + " + y);

        System.out.println("  Executing function: decoratedFunction");
        int result = x + y;
        System.out.println("  Result: " + result);

        Logger.endBlock("Calculate: " + x + " + " + y);
        System.out.println("  Execution complete");
    }

    /**
     * 类装饰器 - 超时控制
     * 对应 Claw: @timeout(seconds)
     */
    private static void timeoutDecoratorExample() {
        Logger.beginBlock("Timeout Decorator Example");

        try {
            DatabaseConnection connection = new DatabaseConnection(10);
            System.out.println("  DatabaseConnection created with 10s timeout");

            if (connection.connect()) {
                System.out.println("  Connection established");
            }

            connection.close();
            System.out.println("  Connection closed");

        } catch (TimeoutException e) {
            System.out.println("  Timeout occurred: " + e.getMessage());
        }

        Logger.endBlock("Timeout Decorator Example");
    }

    /**
     * 多重装饰器
     */
    private static void decoratedFunctionWithMultipleDecorators(int x, int y) {
        Logger.beginBlock("Calculate with Multiple Decorators: " + x + " * " + y);

        System.out.println("  Executing decorated function with logging and retry");

        // 模拟多重装饰器
        int result = multiplyWithDecorators(x, y);
        System.out.println("  Result: " + result);

        Logger.endBlock("Calculate with Multiple Decorators: " + x + " * " + y);
    }

    /**
     * 带装饰器的乘法
     */
    private static int multiplyWithDecorators(int a, int b) {
        return a * b;
    }
}

/**
 * 模拟类装饰器：超时控制
 * 对应 Claw: @timeout(seconds: int)
 */
class DatabaseConnection implements AutoCloseable {
    private final int timeoutSeconds;

    public DatabaseConnection(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        System.out.println("  DatabaseConnection initialized with timeout: " + timeoutSeconds + "s");
    }

    public boolean connect() throws TimeoutException {
        Logger.beginBlock("Database Connect");

        // 模拟连接操作
        try {
            Thread.sleep(100);
            System.out.println("  Connection successful");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TimeoutException("Connection interrupted");
        } finally {
            Logger.endBlock("Database Connect");
        }
    }

    public void close() {
        System.out.println("  Connection closed");
    }
}

/**
 * 超时异常
 */
class TimeoutException extends Exception {
    public TimeoutException(String message) {
        super(message);
    }
}

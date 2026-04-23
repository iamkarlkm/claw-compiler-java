package claw.compiler.example;

import claw.compiler.utils.Logger;

/**
 * 类型注解增强示例
 * 演示 Claw 编译器如何将类型检查和类型转换编译到 Java、Python 和 C
 */
public class TypeAnnotationsExample {

    public static void main(String[] args) {
        System.out.println("=== Type Annotations Enhancement Examples ===");
        System.out.println();

        // 示例 1: 基本类型检查
        System.out.println("Example 1: Basic Type Check");
        basicTypeCheck();
        System.out.println();

        // 示例 2: 类型转换
        System.out.println("Example 2: Type Casting");
        typeCastingExample();
        System.out.println();

        // 示例 3: 类型判断
        System.out.println("Example 3: Type Is Check");
        typeIsCheckExample();
        System.out.println();

        // 示例 4: 类型检查并转换
        System.out.println("Example 4: Type Check and Cast");
        typeCheckAndCastExample();
        System.out.println();

        // 示例 5: 显式类型转换
        System.out.println("Example 5: Explicit Type Cast");
        explicitTypeCastExample();
    }

    /**
     * 基本类型检查示例
     */
    private static void basicTypeCheck() {
        Logger.beginBlock("Type Check Example");

        Object value = 42;

        System.out.println("  Checking type of: " + value);

        // Java instanceof 检查
        if (value instanceof Integer) {
            System.out.println("  ✓ Value is Integer");
            Integer intVal = (Integer) value;
            System.out.println("  Converted to Integer: " + intVal);
        } else if (value instanceof String) {
            System.out.println("  Value is String: " + value);
        }

        Logger.endBlock("Type Check Example");
    }

    /**
     * 类型转换示例
     */
    private static void typeCastingExample() {
        Logger.beginBlock("Type Casting Example");

        Object number = 3.14159;

        System.out.println("  Original value: " + number + " (type: " + number.getClass().getSimpleName() + ")");

        // 转换为 int
        int intValue = ((Number) number).intValue();
        System.out.println("  Cast to int: " + intValue);

        // 转换为 float
        float floatValue = ((Number) number).floatValue();
        System.out.println("  Cast to float: " + floatValue);

        // 转换为 long
        long longValue = ((Number) number).longValue();
        System.out.println("  Cast to long: " + longValue);

        Logger.endBlock("Type Casting Example");
    }

    /**
     * 类型判断示例
     */
    private static void typeIsCheckExample() {
        Logger.beginBlock("Type Is Check Example");

        String[] values = {"42", "hello", "3.14", true, 100};

        for (Object value : values) {
            if (value instanceof Integer) {
                System.out.println("  ✓ \"" + value + "\" is Integer");
            } else if (value instanceof String) {
                System.out.println("  ✓ \"" + value + "\" is String");
            } else if (value instanceof Boolean) {
                System.out.println("  ✓ \"" + value + "\" is Boolean");
            } else {
                System.out.println("  ? \"" + value + "\" is " + value.getClass().getSimpleName());
            }
        }

        Logger.endBlock("Type Is Check Example");
    }

    /**
     * 类型检查并转换示例
     */
    private static void typeCheckAndCastExample() {
        Logger.beginBlock("Type Check and Cast Example");

        Object[] mixedData = {42, "hello", 3.14, true, 100};

        System.out.println("  Processing mixed data:");

        for (Object value : mixedData) {
            // Java: Integer.valueOf((Integer) value)
            // 等同于: check_and_cast_to_int(value)
            try {
                Integer intVal = (value instanceof Integer) ? (Integer) value : null;
                if (intVal != null) {
                    System.out.println("  ✓ Converted: " + value + " -> " + intVal);
                } else {
                    System.out.println("  ✗ Skipped: " + value + " (not an Integer)");
                }
            } catch (ClassCastException e) {
                System.out.println("  ✗ Error converting: " + value);
            }
        }

        Logger.endBlock("Type Check and Cast Example");
    }

    /**
     * 显式类型转换示例
     */
    private static void explicitTypeCastExample() {
        Logger.beginBlock("Explicit Type Cast Example");

        Number number = 42;

        System.out.println("  Original number: " + number);

        // 显式转换为 int
        int intVal = number.intValue();
        System.out.println("  Explicit to int: " + intVal);

        // 显式转换为 float
        float floatVal = number.floatValue();
        System.out.println("  Explicit to float: " + floatVal);

        // 显式转换为 long
        long longVal = number.longValue();
        System.out.println("  Explicit to long: " + longVal);

        // 显式转换为 double
        double doubleVal = number.doubleValue();
        System.out.println("  Explicit to double: " + doubleVal);

        Logger.endBlock("Explicit Type Cast Example");
    }

    /**
     * 安全类型转换工具方法
     */
    private static Integer safeCastToInt(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            // 尝试转换为 Integer
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            System.err.println("  Warning: Cannot convert '" + value + "' to int");
            return 0;
        }
    }
}

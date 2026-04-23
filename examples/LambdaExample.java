package claw.compiler.example;

import claw.compiler.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Lambda 表达式示例
 * 演示 Claw 编译器如何将 Lambda 语法编译到 Java、Python 和 C
 */
public class LambdaExample {

    public static void main(String[] args) {
        System.out.println("=== Lambda Expression Examples ===");
        System.out.println();

        // 示例 1: 基本 Lambda
        System.out.println("Example 1: Basic Lambda - Addition");
        basicLambdaExample();
        System.out.println();

        // 示例 2: 映射 Lambda
        System.out.println("Example 2: Mapping Lambda - Doubling");
        mappingLambdaExample();
        System.out.println();

        // 示例 3: 过滤 Lambda
        System.out.println("Example 3: Filtering Lambda - Even Numbers");
        filteringLambdaExample();
        System.out.println();

        // 示例 4: 带验证的 Lambda
        System.out.println("Example 4: Lambda with Validation");
        validatedLambdaExample();
        System.out.println();

        // 示例 5: Lambda 作为参数
        System.out.println("Example 5: Lambda as Argument - Sort");
        lambdaAsArgumentExample();
    }

    /**
     * 基本 Lambda 示例
     */
    private static void basicLambdaExample() {
        Logger.beginBlock("Basic Lambda Addition");

        // 定义 Lambda：lambda_add = lambda x, y: x + y
        Function<Integer, Function<Integer, Integer>> lambdaAdd = (x, y) -> x + y;

        System.out.println("  Lambda function: (x, y) -> x + y");

        // 调用 Lambda
        int result1 = lambdaAdd.apply(10, 20);
        System.out.println("  lambda_add(10, 20) = " + result1);

        int result2 = lambdaAdd.apply(5, 15);
        System.out.println("  lambda_add(5, 15) = " + result2);

        Logger.endBlock("Basic Lambda Addition");
    }

    /**
     * 映射 Lambda 示例
     */
    private static void mappingLambdaExample() {
        Logger.beginBlock("Mapping Lambda - Doubling Numbers");

        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        System.out.println("  Original list: " + numbers);

        // 定义 Lambda：lambda x: x * 2
        Function<Integer, Integer> doubleLambda = x -> x * 2;

        // 应用映射
        List<Integer> doubled = new ArrayList<>();
        for (Integer num : numbers) {
            doubled.add(doubleLambda.apply(num));
        }

        System.out.println("  Doubled list: " + doubled);

        Logger.endBlock("Mapping Lambda - Doubling Numbers");
    }

    /**
     * 过滤 Lambda 示例
     */
    private static void filteringLambdaExample() {
        Logger.beginBlock("Filtering Lambda - Even Numbers");

        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        System.out.println("  Original list: " + numbers);

        // 定义 Lambda：lambda x: x % 2 == 0
        java.util.function.Predicate<Integer> evenLambda = x -> x % 2 == 0;

        // 应用过滤
        List<Integer> evens = new ArrayList<>();
        for (Integer num : numbers) {
            if (evenLambda.test(num)) {
                evens.add(num);
            }
        }

        System.out.println("  Even numbers: " + evens);

        Logger.endBlock("Filtering Lambda - Even Numbers");
    }

    /**
     * 带验证的 Lambda 示例
     */
    private static void validatedLambdaExample() {
        Logger.beginBlock("Lambda with Validation");

        // 定义带验证的 Lambda
        Function<Integer, Function<Integer, Integer>> createValidatedAdd =
            x -> y -> {
                if (x <= 0 || y <= 0) {
                    System.out.println("  Validation failed: Values must be positive");
                    return null;
                }
                return x + y;
            };

        System.out.println("  Lambda: (x, y) -> x + y with validation");

        // 有效调用
        int result1 = createValidatedAdd.apply(5).apply(10);
        if (result1 != null) {
            System.out.println("  Valid call: 5 + 10 = " + result1);
        }

        // 无效调用
        int result2 = createValidatedAdd.apply(-5).apply(10);
        if (result2 != null) {
            System.out.println("  Valid call: -5 + 10 = " + result2);
        }

        Logger.endBlock("Lambda with Validation");
    }

    /**
     * Lambda 作为参数示例
     */
    private static void lambdaAsArgumentExample() {
        Logger.beginBlock("Lambda as Argument - Custom Sort");

        List<Integer> numbers = List.of(3, 1, 4, 1, 5, 9, 2, 6);
        System.out.println("  Original list: " + numbers);

        // 定义排序 Lambda：lambda x, y: x - y
        java.util.Comparator<Integer> descendingLambda =
            (x, y) -> y - x;  // 降序排序

        // 对列表排序
        List<Integer> sorted = new ArrayList<>(numbers);
        sorted.sort(descendingLambda);

        System.out.println("  Sorted (descending): " + sorted);

        Logger.endBlock("Lambda as Argument - Custom Sort");
    }
}

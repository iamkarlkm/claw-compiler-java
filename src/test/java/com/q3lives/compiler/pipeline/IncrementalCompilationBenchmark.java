package com.q3lives.compiler.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 增量编译性能基准测试
 */
public class IncrementalCompilationBenchmark {

    private EfficientCompiler compiler;
    private Map<String, String> testProject;

    @BeforeEach
    void setUp() {
        compiler = new EfficientCompiler(true, true, 4);
        testProject = createTestProject();
    }

    @Test
    void testFullCompilationBenchmark() {
        System.out.println("\n=== 全量编译基准测试 ===");

        long startTime = System.currentTimeMillis();
        long totalTime = 0;

        // 多次取平均值
        for (int i = 0; i < 5; i++) {
            startTime = System.currentTimeMillis();
            compiler.compileProject(testProject);
            totalTime += System.currentTimeMillis() - startTime;
        }

        long averageTime = totalTime / 5;
        System.out.printf("平均全量编译时间: %d ms%n", averageTime);

        // 断言编译时间在合理范围内
        assertTrue(averageTime < 5000, "全量编译时间过长");
    }

    @Test
    void testIncrementalCompilationBenchmark() {
        System.out.println("\n=== 增量编译基准测试 ===");

        // 首次编译（全量）
        compiler.compileProject(testProject);

        // 修改一个小文件
        modifyFile("utils.claw", "age: 25", "age: 30");

        long startTime = System.currentTimeMillis();
        compiler.compileProject(testProject);
        long incrementalTime = System.currentTimeMillis() - startTime;

        System.out.printf("增量编译时间: %d ms%n", incrementalTime);

        // 增量编译应该比全量编译快
        assertTrue(incrementalTime < 1000, "增量编译时间过长");
    }

    @Test
    void testCachePerformance() {
        System.out.println("\n=== 缓存性能测试 ===");

        // 第一次编译（应该从缓存中获取）
        long startTime = System.currentTimeMillis();
        compiler.compileProject(testProject);
        long firstCompileTime = System.currentTimeMillis() - startTime;

        // 第二次编译（应该使用缓存）
        startTime = System.currentTimeMillis();
        compiler.compileProject(testProject);
        long cachedCompileTime = System.currentTimeMillis() - startTime;

        System.out.printf("首次编译: %d ms%n", firstCompileTime);
        System.out.printf("缓存编译: %d ms%n", cachedCompileTime);

        // 缓存编译应该更快
        assertTrue(cachedCompileTime < firstCompileTime,
                  "缓存编译应该比首次编译快");
    }

    @Test
    void testParallelCompilationBenchmark() {
        System.out.println("\n=== 并行编译基准测试 ===");

        // 创建多个独立文件
        Map<String, String> independentProject = createIndependentProject();

        long startTime = System.currentTimeMillis();
        compiler.compileProject(independentProject);
        long parallelTime = System.currentTimeMillis() - startTime;

        System.out.printf("并行编译时间: %d ms%n", parallelTime);

        // 并行编译应该比顺序编译快
        assertTrue(parallelTime < 2000, "并行编译时间过长");
    }

    @Test
    void testLargeProjectPerformance() {
        System.out.println("\n=== 大型项目性能测试 ===");

        // 创建大型项目
        Map<String, String> largeProject = createLargeProject();

        long startTime = System.currentTimeMillis();
        compiler.compileProject(largeProject);
        long largeProjectTime = System.currentTimeMillis() - startTime;

        System.out.printf("大型项目编译时间: %d ms%n", largeProjectTime);

        // 大型项目编译应该在合理时间内完成
        assertTrue(largeProjectTime < 10000, "大型项目编译时间过长");
    }

    private Map<String, String> createTestProject() {
        Map<String, String> project = new HashMap<>();

        project.put("main.claw",
            "import \"utils.claw\"\n" +
            "import \"core.claw\"\n\n" +
            "normal function main() -> String {\n" +
            "    var user = User()\n" +
            "    user.name = \"Test\"\n" +
            "    user.age = 25\n" +
            "    return utils.formatUser(user)\n" +
            "}\n");

        project.put("utils.claw",
            "normal function formatUser(user: User) -> String {\n" +
            "    return \"Name: \" + user.name + \", Age: \" + user.age\n" +
            "}\n");

        project.put("core.claw",
            "type User {\n" +
            "    var name: String\n" +
            "    var age: Int\n" +
            "}\n");

        return project;
    }

    private Map<String, String> createIndependentProject() {
        Map<String, String> project = new HashMap<>();

        // 创建多个互不依赖的文件
        for (int i = 1; i <= 10; i++) {
            String fileContent = "normal function function" + i + "() -> Int {\n" +
                                "    return " + i + "\n" +
                                "}\n";
            project.put("file" + i + ".claw", fileContent);
        }

        return project;
    }

    private Map<String, String> createLargeProject() {
        Map<String, String> project = new HashMap<>();

        // 创建100个文件
        for (int i = 1; i <= 100; i++) {
            String fileContent = "normal function function" + i + "() -> Int {\n" +
                                "    // 复杂的计算逻辑\n" +
                                "    var result = 0\n" +
                                "    for (j = 0; j < 1000; j++) {\n" +
                                "        result += j\n" +
                                "    }\n" +
                                "    return result\n" +
                                "}\n";
            project.put("large_file_" + i + ".claw", fileContent);
        }

        return project;
    }

    private void modifyFile(String fileName, String oldContent, String newContent) {
        String content = testProject.get(fileName);
        testProject.put(fileName, content.replace(oldContent, newContent));
    }
}
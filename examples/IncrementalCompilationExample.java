package examples;

import com.q3lives.compiler.pipeline.CompilationCacheManager;
import com.q3lives.compiler.pipeline.CompilationResult;
import com.q3lives.compiler.pipeline.EfficientCompiler;
import com.q3lives.compiler.pipeline.FileChangeDetector;
import com.q3lives.compiler.pipeline.IncrementalCompiler;

import java.util.HashMap;
import java.util.Map;

/**
 * 增量编译示例
 *
 * 演示如何使用高效编译器进行增量编译
 */
public class IncrementalCompilationExample {

    public static void main(String[] args) {
        System.out.println("=== 增量编译示例 ===\n");

        // 创建高效编译器
        EfficientCompiler compiler = new EfficientCompiler(true, true, 4);

        // 准备测试文件
        Map<String, String> sourceFiles = createTestProject();

        // 第一次编译（全量）
        System.out.println("1. 第一次编译（全量）");
        compileAndMeasure(compiler, sourceFiles, "首次编译");

        // 第二次编译（无变更）
        System.out.println("\n2. 第二次编译（无变更）");
        compileAndMeasure(compiler, sourceFiles, "无变更编译");

        // 修改文件
        System.out.println("\n3. 修改文件后编译");
        modifyFile(sourceFiles, "utils.claw");
        compileAndMeasure(compiler, sourceFiles, "修改后编译");

        // 添加新文件
        System.out.println("\n4. 添加新文件后编译");
        addNewFile(sourceFiles);
        compileAndMeasure(compiler, sourceFiles, "添加文件编译");

        // 显示统计信息
        showStatistics(compiler);
    }

    /**
     * 创建测试项目
     */
    private static Map<String, String> createTestProject() {
        Map<String, String> files = new HashMap<>();

        // 主程序
        files.put("main.claw", "" +
            "import \"utils.claw\"\n" +
            "import \"core.claw\"\n\n" +
            "normal function main() -> String {\n" +
            "    var user = User()\n" +
            "    user.name = \"Test\"\n" +
            "    user.age = 25\n" +
            "    \n" +
            "    return utils.formatUser(user)\n" +
            "}\n");

        // 工具函数
        files.put("utils.claw", "" +
            "normal function formatUser(user: User) -> String {\n" +
            "    return \"Name: \" + user.name + \", Age: \" + user.age\n" +
            "}\n\n" +
            "normal function validateAge(age: Int) -> Bool {\n" +
            "    return age > 0 && age < 150\n" +
            "}\n");

        // 核心功能
        files.put("core.claw", "" +
            "type User {\n" +
            "    var name: String\n" +
            "    var age: Int\n" +
            "}\n\n" +
            "normal function createUser(name: String, age: Int) -> User {\n" +
            "    var user = User()\n" +
            "    user.name = name\n" +
            "    user.age = age\n" +
            "    return user\n" +
            "}\n");

        return files;
    }

    /**
     * 编译并测量时间
     */
    private static void compileAndMeasure(EfficientCompiler compiler,
                                         Map<String, String> files,
                                         String testName) {
        long startTime = System.currentTimeMillis();

        if (files.size() == 1) {
            // 单文件编译
            String fileName = files.keySet().iterator().next();
            CompilationResult result = compiler.compile(files.get(fileName), fileName);
            printResult(testName, result, System.currentTimeMillis() - startTime);
        } else {
            // 项目编译
            EfficientCompiler.ProjectCompilationResult result =
                compiler.compileProject(files);
            printProjectResult(testName, result, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 打印编译结果
     */
    private static void printResult(String testName, CompilationResult result, long elapsed) {
        System.out.printf(" [%s] 耗时: %d ms, 状态: %s%n",
                testName, elapsed,
                result.isSuccess() ? "成功" : "失败");
        if (!result.isSuccess()) {
            System.out.println("  错误: " + result.getErrorMessage());
        }
    }

    /**
     * 打印项目编译结果
     */
    private static void printProjectResult(String testName,
                                         EfficientCompiler.ProjectCompilationResult result,
                                         long elapsed) {
        System.out.printf(" [%s] 耗时: %d ms, 成功: %d/%d%n",
                testName, elapsed,
                result.getSuccessCount(), result.getTotalFiles());
        System.out.println("  失败: " + result.getFailureCount());
    }

    /**
     * 修改文件
     */
    private static void modifyFile(Map<String, String> files, String fileName) {
        String original = files.get(fileName);
        String modified = original.replace("age: 25", "age: 30");
        files.put(fileName, modified);
        System.out.println("  已修改文件: " + fileName);
    }

    /**
     * 添加新文件
     */
    private static void addNewFile(Map<String, String> files) {
        String newFileContent = "" +
            "normal function helperFunction(value: Int) -> Int {\n" +
            "    return value * 2\n" +
            "}\n";

        files.put("helper.claw", newFileContent);
        System.out.println("  已添加新文件: helper.claw");
    }

    /**
     * 显示统计信息
     */
    private static void showStatistics(EfficientCompiler compiler) {
        System.out.println("\n=== 编译统计信息 ===");

        // 获取缓存统计
        CompilationCacheManager.CacheStatistics cacheStats =
            ((CompilationCacheManager) compiler.getClass()
                .getDeclaredField("cacheManager")
                .get(compiler)).getStatistics();

        System.out.println("缓存条目数: " + cacheStats.totalEntries());
        System.out.println("缓存大小: " + formatSize(cacheStats.totalSize()));
        System.out.println("过期条目: " + cacheStats.expiredEntries());

        // 获取编译统计
        EfficientCompiler.CompilationStatistics compileStats =
            compiler.getStatistics();

        System.out.println("总编译次数: " + compileStats.totalCompilations());
        System.out.println("平均编译时间: " + String.format("%.2f", compileStats.averageCompileTime()) + " ms");
    }

    /**
     * 格式化文件大小
     */
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
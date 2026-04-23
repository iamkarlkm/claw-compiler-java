# 性能优化使用指南

## 1. PlatformConstraint使用示例

### 基本用法

```java
import claw.compiler.generators.ffi.platform.PlatformConstraint;
import claw.compiler.generators.ffi.platform.TargetTriple;

// 创建平台约束（支持多个平台、架构、工具链）
PlatformConstraint constraint = PlatformConstraint.builder()
    .platform("windows", "linux", "macos")
    .arch("x86_64", "arm64", "wasm32")
    .toolchain("gcc", "clang", "msvc")
    .build();

// 创建目标三元组
TargetTriple target = new TargetTriple("x86_64-pc-linux-gnu");

// O(1) 匹配检查
boolean matches = constraint.matches(target);
System.out.println("目标匹配: " + matches);
```

### 高级用法

```java
// 检查是否有平台约束
boolean hasPlatform = constraint.hasPlatformConstraint();

// 检查是否有架构约束
boolean hasArch = constraint.hasArchConstraint();

// 获取所有匹配的平台
List<String> platforms = constraint.getPlatforms();
List<String> archs = constraint.getArchitectures();
List<String> toolchains = constraint.getToolchains();

// 交集：两个约束的共同子集
PlatformConstraint intersection = constraint1.intersect(constraint2);

// 并集：两个约束的合并
PlatformConstraint union = constraint1.union(constraint2);

// 检查是否有重叠
boolean overlaps = constraint1.overlaps(constraint2);

// 序列化到字符串
String constraintStr = constraint.toString();
// 输出: @platform(windows,linux) @arch(x86_64) @toolchain(gcc,clang)

// 从字符串反序列化
PlatformConstraint fromString = PlatformConstraint.fromString(constraintStr);
```

### 实际应用示例

```java
public class FFIPlatformSelector {
    private final List<PlatformConstraint> targets;

    public FFIPlatformSelector(List<String> platforms,
                               List<String> archs,
                               List<String> toolchains) {
        this.targets = new ArrayList<>();
        
        // 为每个组合创建约束
        for (String platform : platforms) {
            for (String arch : archs) {
                for (String toolchain : toolchains) {
                    PlatformConstraint constraint = PlatformConstraint.builder()
                        .platform(platform)
                        .arch(arch)
                        .toolchain(toolchain)
                        .build();
                    targets.add(constraint);
                }
            }
        }
    }

    public PlatformConstraint selectBestMatch(TargetTriple target) {
        // O(N) 遍历，N是目标数量（通常很小）
        for (PlatformConstraint constraint : targets) {
            if (constraint.matches(target)) {
                return constraint;
            }
        }
        return PlatformConstraint.UNIVERSAL;  // 没有匹配，返回全平台
    }
}
```

---

## 2. ParallelCompilationPipeline使用示例

### 基本用法

```java
import claw.compiler.pipeline.ParallelCompilationPipeline;
import claw.compiler.core.CompilationResult;

// 创建并行编译管道（线程数=4）
ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(4);

// 执行编译
String sourceCode = """
    function main() -> Int {
        Int a = 1
        Int b = 2
        Int c = a + b
        return c
    }
    """;

CompilationResult result = pipeline.compile(sourceCode, "test.claw");

if (result.isSuccess()) {
    System.out.println("编译成功！");
    System.out.println("耗时: " + result.getElapsedMillis() + "ms");
} else {
    System.out.println("编译失败: " + result.getErrors());
}

// 清理资源
pipeline.shutdown();
```

### 性能监控

```java
ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(4);

// 编译
pipeline.compile(sourceCode, "test.claw");

// 监控性能
System.out.println("活跃线程数: " + pipeline.getActiveThreads());
System.out.println("已完成任务: " + pipeline.getCompletedTasks());
System.out.println("是否运行中: " + pipeline.isRunning());

// 使用Lambda获取线程数
pipeline.compile(sourceCode, "test.claw", (stats) -> {
    System.out.println("编译完成！");
    System.out.println("活跃线程: " + stats.getActiveThreads());
    System.out.println("已完成任务: " + stats.getCompletedTasks());
});
```

### 自定义批次大小

```java
// 大文件使用更大的批次
ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(
    4,  // 线程数
    500 // 批次大小
);

CompilationResult result = pipeline.compile(largeSource, "large.claw");
```

### 多文件编译

```java
ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(4);

List<String> sourceFiles = List.of(
    "main.claw",
    "utils.claw",
    "config.claw",
    "types.claw"
);

// 编译所有文件
for (String file : sourceFiles) {
    String content = readFile(file);
    CompilationResult result = pipeline.compile(content, file);
    if (!result.isSuccess()) {
        System.err.println("编译失败: " + file);
    }
}

pipeline.shutdown();
```

---

## 3. 性能调优指南

### 线程数选择

#### CPU密集型编译
```java
// CPU密集型：使用CPU核心数的0.75-1.0倍
int threads = (int) (Runtime.getRuntime().availableProcessors() * 0.8);
ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(threads);
```

#### IO密集型编译
```java
// IO密集型：使用CPU核心数的1.5-2.0倍
int threads = Runtime.getRuntime().availableProcessors() * 2;
ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(threads);
```

#### 平衡选择（推荐）
```java
// 4-8线程是最佳平衡点
int threads = Math.min(8, Runtime.getRuntime().availableProcessors());
ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(threads);
```

### 批次大小选择

#### 小文件（<10KB）
```java
ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(4, 100);
```

#### 中等文件（10-100KB）
```java
ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(4, 500);
```

#### 大文件（100-1000KB）
```java
ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(4, 1000);
```

#### 超大文件（>1MB）
```java
ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(8, 2000);
```

### 性能测试

```java
@Test
void performanceTest() {
    String largeSource = loadLargeSource(); // 100KB

    // 测试不同配置
    int[] threadCounts = {1, 2, 4, 8};
    int[] batchSizes = {100, 500, 1000, 2000};

    for (int threads : threadCounts) {
        for (int batchSize : batchSizes) {
            ParallelCompilationPipeline pipeline = 
                new ParallelCompilationPipeline(threads, batchSize);

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                pipeline.compile(largeSource, "test.claw");
            }
            long elapsed = System.currentTimeMillis() - startTime;

            System.out.printf("线程数: %d, 批次大小: %d, 时间: %dms%n",
                threads, batchSize, elapsed);
        }
    }
}
```

---

## 4. 实际应用案例

### 案例1：CI/CD中的并行编译

```java
public class CICompiler {
    private final ParallelCompilationPipeline pipeline;
    private final File projectRoot;

    public CICompiler() {
        this.pipeline = new ParallelCompilationPipeline(4);
        this.projectRoot = new File("src");
    }

    public Map<String, CompilationResult> compileAll(String... targets) {
        Map<String, CompilationResult> results = new ConcurrentHashMap<>();

        // 并行编译所有目标
        List<CompletableFuture<CompilationResult>> futures = Arrays.stream(targets)
            .map(target -> CompletableFuture.supplyAsync(() -> {
                File file = new File(projectRoot, target + ".claw");
                String content = readFile(file);
                return pipeline.compile(content, target);
            }, pipeline.executor))
            .collect(Collectors.toList());

        // 收集结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        return results;
    }
}
```

### 案例2：开发环境热编译

```java
public class HotCompiler {
    private final Map<String, Long> fileTimestamps = new ConcurrentHashMap<>();
    private final Map<String, String> fileCache = new ConcurrentHashMap<>();
    private final ParallelCompilationPipeline pipeline;
    private final int compileThreads;

    public HotCompiler() {
        this.compileThreads = Runtime.getRuntime().availableProcessors();
        this.pipeline = new ParallelCompilationPipeline(compileThreads);
    }

    public CompilationResult compileIfChanged(String filePath) {
        File file = new File(filePath);
        long currentTimestamp = file.lastModified();

        // 检查文件是否修改
        Long cachedTimestamp = fileTimestamps.get(filePath);
        if (cachedTimestamp != null && cachedTimestamp == currentTimestamp) {
            // 文件未修改，跳过编译
            return null;
        }

        // 编译文件
        String content = readFile(file);
        CompilationResult result = pipeline.compile(content, filePath);
        
        // 更新缓存
        fileTimestamps.put(filePath, currentTimestamp);
        fileCache.put(filePath, content);
        
        return result;
    }
}
```

### 案例3：多模块项目编译

```java
public class MultiModuleCompiler {
    private final Map<String, ParallelCompilationPipeline> pipelines = new HashMap<>();
    private final ExecutorService globalExecutor;

    public MultiModuleCompiler(List<String> moduleNames) {
        this.globalExecutor = Executors.newFixedThreadPool(4);
        
        // 为每个模块创建独立的编译管道
        for (String module : moduleNames) {
            pipelines.put(module, new ParallelCompilationPipeline(2));
        }
    }

    public CompletableFuture<Void> compileModule(String moduleName, String source) {
        ParallelCompilationPipeline pipeline = pipelines.get(moduleName);
        
        return CompletableFuture.supplyAsync(() -> {
            return pipeline.compile(source, moduleName);
        }, globalExecutor);
    }

    public CompletableFuture<Void> compileAll(List<ModuleCompileTask> tasks) {
        List<CompletableFuture<Void>> futures = tasks.stream()
            .map(task -> compileModule(task.moduleName, task.source))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
```

---

## 5. 性能监控和调优

### 监控指标

```java
public class CompilationMetrics {
    private final long startTime;
    private final int activeThreads;
    private final long completedTasks;
    private final long totalCompilationTime;

    public CompilationMetrics(CompilationResult result,
                            ParallelCompilationPipeline pipeline) {
        this.startTime = System.currentTimeMillis();
        this.activeThreads = pipeline.getActiveThreads();
        this.completedTasks = pipeline.getCompletedTasks();
        this.totalCompilationTime = result.getElapsedMillis();
    }

    public void printMetrics() {
        System.out.println("=== 编译性能指标 ===");
        System.out.println("总时间: " + totalCompilationTime + "ms");
        System.out.println("活跃线程数: " + activeThreads);
        System.out.println("已完成任务: " + completedTasks);
        
        if (totalCompilationTime > 0) {
            double tasksPerSecond = (completedTasks * 1000.0) / totalCompilationTime;
            System.out.println("任务吞吐量: " + String.format("%.2f", tasksPerSecond) + " tasks/sec");
        }
    }
}
```

### 调优建议

1. **小文件（<10KB）**
   - 批次大小：100-500
   - 线程数：2-4

2. **中等文件（10-100KB）**
   - 批次大小：500-1000
   - 线程数：4-6

3. **大文件（100-1000KB）**
   - 批次大小：1000-2000
   - 线程数：6-8

4. **超大文件（>1MB）**
   - 批次大小：2000+
   - 线程数：8

---

## 6. 最佳实践

### ✅ 推荐做法

1. **使用try-finally确保资源释放**
   ```java
   ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(4);
   try {
       CompilationResult result = pipeline.compile(source, "file.claw");
   } finally {
       pipeline.shutdown();
   }
   ```

2. **批量编译时使用CompletableFuture**
   ```java
   List<CompletableFuture<CompilationResult>> futures = sources.stream()
       .map(source -> CompletableFuture.supplyAsync(() -> 
           pipeline.compile(source, fileName), executor))
       .collect(Collectors.toList());
   
   CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
   ```

3. **监控性能指标**
   ```java
   System.out.println("活跃线程: " + pipeline.getActiveThreads());
   System.out.println("已完成任务: " + pipeline.getCompletedTasks());
   ```

### ❌ 避免做法

1. **不要创建过多线程**
   ```java
   // 错误：创建过多线程
   ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(100);
   
   // 正确：使用CPU核心数
   int threads = Runtime.getRuntime().availableProcessors();
   ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(threads);
   ```

2. **不要使用过小的批次**
   ```java
   // 错误：批次太小，线程切换开销大
   ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(4, 10);
   
   // 正确：批次适中
   ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(4, 500);
   ```

3. **不要忽略资源清理**
   ```java
   // 错误：忘记关闭线程池
   ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(4);
   pipeline.compile(source, "file.claw");
   // 忘记pipeline.shutdown();
   
   // 正确：使用try-finally
   try {
       ParallelCompilationPipeline pipeline = new ParallelCompilationPipeline(4);
       pipeline.compile(source, "file.claw");
   } finally {
       pipeline.shutdown();
   }
   ```

---

**创建日期：** 2026-04-17
**版本：** 1.0
**适用版本：** Claw Compiler v3.0

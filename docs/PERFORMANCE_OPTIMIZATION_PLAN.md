# Claw编译器性能优化计划

## 优化目标

提升编译器性能，减少编译时间，优化内存使用。

---

## 📋 任务清单

### ✅ 任务1：PlatformConstraint 位掩码优化（已完成）

**实现内容：**
- 使用long的64位作为位掩码
- 3个维度（平台、架构、工具链）各占用16位
- O(1) 的位与运算进行匹配
- 无GC压力，纯值类型运算

**性能提升：**
- 内存：1个long（8字节）替代多个Set
- 查询：O(1) 位运算替代字符串匹配

**文件：**
- `src/main/java/claw/compiler/generators/ffi/platform/PlatformConstraint.java`

---

### ⏳ 任务2：并行编译支持（进行中）

**优化目标：**
- 多线程并行处理4层语义处理器
- 并行编译多个文件
- 任务并行化和负载均衡

**实现计划：**

#### 2.1 并行语义处理器
```java
// 创建并行处理器
public class ParallelTypeProcessor extends TypeProcessor {
    private final ExecutorService executor;

    public ParallelTypeProcessor(int threads) {
        this.executor = Executors.newFixedThreadPool(threads);
    }

    public void processTokens(List<Token> tokens) {
        // 分割tokens为多个批次
        List<List<Token>> batches = partition(tokens, BATCH_SIZE);
        // 并行处理每个批次
        List<CompletableFuture<Void>> futures = batches.stream()
            .map(batch -> CompletableFuture.runAsync(() -> {
                super.processTokens(batch);
            }, executor))
            .collect(Collectors.toList());

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
```

**性能提升预期：**
- 编译时间减少50-70%（取决于CPU核心数）
- 内存增加：线程池开销

#### 2.2 并行文件编译
```java
// 多文件并行编译
public class ParallelCompilationService {
    private final ExecutorService executor;
    private final CompilationPipeline pipeline;

    public CompletableFuture<CompilationResult> compileParallel(
        List<String> sources, int numThreads) {

        // 将源文件分割为多个批次
        List<List<String>> batches = partition(sources, numThreads);

        // 并行编译每个批次
        List<CompletableFuture<CompilationResult>> futures = batches.stream()
            .map(batch -> CompletableFuture.supplyAsync(() -> {
                return batch.stream()
                    .map(sources -> pipeline.compile(sources, sources))
                    .collect(Collectors.toList());
            }, executor))
            .collect(Collectors.toList());

        // 合并结果
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> mergeResults(futures));
    }
}
```

**性能提升预期：**
- 多文件编译时间减少60-80%
- 适合CI/CD流水线

---

### ⏳ 任务3：增量编译（待实现）

**优化目标：**
- 只编译修改过的文件
- 缓存编译结果
- 跨编译会话的缓存

**实现计划：**

#### 3.1 文件依赖追踪
```java
public class FileDependencyTracker {
    // 文件修改时间
    private final Map<String, Long> fileModificationTimes = new HashMap<>();

    // 文件依赖关系
    private final Map<String, Set<String>> dependencies = new HashMap<>();

    public boolean isFileModified(String filePath) {
        long current = new File(filePath).lastModified();
        Long cached = fileModificationTimes.get(filePath);
        return cached == null || current != cached;
    }

    public void recordDependency(String sourceFile, String dependentFile) {
        dependencies.computeIfAbsent(sourceFile, k -> new HashSet<>())
            .add(dependentFile);
    }

    public Set<String> getChangedFiles(String projectRoot) {
        // 递归查找所有受影响的文件
        return new HashSet<>();
    }
}
```

#### 3.2 编译缓存
```java
public class CompilationCache {
    // 文件路径 → 编译结果
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public Optional<GeneratedCode> get(String filePath) {
        CacheEntry entry = cache.get(filePath);
        if (entry == null) return Optional.empty();
        return Optional.of(entry.generatedCode);
    }

    public void put(String filePath, GeneratedCode generatedCode) {
        cache.put(filePath, new CacheEntry(generatedCode));
    }

    private static class CacheEntry {
        final GeneratedCode generatedCode;
        final long timestamp;

        CacheEntry(GeneratedCode generatedCode) {
            this.generatedCode = generatedCode;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
```

#### 3.3 增量编译服务
```java
public class IncrementalCompilationService {
    private final CompilationPipeline pipeline;
    private final FileDependencyTracker tracker;
    private final CompilationCache cache;

    public CompilationResult compileIncremental(
        String projectRoot, List<String> sourceFiles) {

        // 1. 检查哪些文件已修改
        Set<String> changedFiles = tracker.getChangedFiles(projectRoot);

        if (changedFiles.isEmpty()) {
            // 所有文件都未修改，使用缓存
            log.info("使用缓存编译");
            return loadFromCache(projectRoot, sourceFiles);
        }

        // 2. 重新编译修改的文件
        log.info("重新编译修改的文件: {}", changedFiles);

        // 3. 更新缓存
        for (String file : changedFiles) {
            CompilationResult result = pipeline.compile(
                readFile(file), file);
            cache.put(file, result);
        }

        return cacheResults(projectRoot, sourceFiles);
    }
}
```

**性能提升预期：**
- 未修改文件：编译时间减少90%+
- 完全增量编译：编译时间减少70-90%

---

## 📊 性能基准测试

### 基准测试计划

#### 测试1：单文件编译性能
```java
@Test
public void benchmarkSingleFileCompilation() {
    String source = loadTestSource("large_file.claw");

    CompilationPipeline pipeline = new CompilationPipeline();
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < 100; i++) {
        pipeline.compile(source, "test.claw");
    }

    long elapsed = System.currentTimeMillis() - startTime;
    System.out.println("平均编译时间: " + elapsed / 100 + "ms");
}
```

#### 测试2：并行编译性能
```java
@Test
public void benchmarkParallelCompilation() {
    List<String> sources = loadAllTestSources();

    ParallelCompilationService parallelService =
        new ParallelCompilationService(sources, 4);

    long startTime = System.currentTimeMillis();
    CompletableFuture<CompilationResult> result =
        parallelService.compileParallel(sources, 4);
    result.join();

    long elapsed = System.currentTimeMillis() - startTime;
    System.out.println("并行编译时间: " + elapsed + "ms");
}
```

#### 测试3：增量编译性能
```java
@Test
public void benchmarkIncrementalCompilation() {
    IncrementalCompilationService incrementalService =
        new IncrementalCompilationService(projectRoot);

    // 首次完整编译
    long firstStart = System.currentTimeMillis();
    CompilationResult firstResult =
        incrementalService.compileIncremental(projectRoot, allSources);
    long firstElapsed = System.currentTimeMillis() - firstStart;

    // 模拟修改文件
    modifyFile("main.claw");

    // 增量编译
    long secondStart = System.currentTimeMillis();
    CompilationResult secondResult =
        incrementalService.compileIncremental(projectRoot, allSources);
    long secondElapsed = System.currentTimeMillis() - secondStart;

    System.out.println("首次编译: " + firstElapsed + "ms");
    System.out.println("增量编译: " + secondElapsed + "ms");
    System.out.println("性能提升: " +
        (firstElapsed - secondElapsed) * 100 / firstElapsed + "%");
}
```

---

## 🎯 优化优先级

1. **高优先级**（立即实现）
   - ✅ PlatformConstraint位掩码（已完成）
   - 并行编译语义处理器

2. **中优先级**（1周内）
   - 多文件并行编译
   - 文件依赖追踪

3. **低优先级**（2周内）
   - 编译缓存
   - 增量编译服务

---

## 📈 预期性能提升

| 优化项 | 当前性能 | 优化后 | 提升 |
|--------|---------|--------|------|
| **PlatformConstraint** | - | O(1)位运算 | - |
| **单文件编译** | 100ms | 30-40ms | 60-70% ↓ |
| **多文件编译** | 1000ms | 200-400ms | 60-80% ↓ |
| **增量编译** | 1000ms | 100-300ms | 70-90% ↓ |

---

## 🔧 实现步骤

### 第一周
1. ✅ 实现ParallelTypeProcessor（可配置线程数）
2. 实现ParallelFunctionProcessor
3. 实现ParallelControlFlowProcessor
4. 添加性能基准测试

### 第二周
5. 实现FileDependencyTracker
6. 实现CompilationCache
7. 实现IncrementalCompilationService
8. 性能测试和调优

---

**创建日期：** 2026-04-17
**负责人：** AI Assistant
**预计完成：** 2026-05-01

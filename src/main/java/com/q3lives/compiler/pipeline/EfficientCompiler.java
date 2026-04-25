package com.q3lives.compiler.pipeline;

import com.q3lives.compiler.common.CompilerConstants;
import com.q3lives.compiler.performance.PerformanceMonitor;
import com.q3lives.compiler.pipeline.GeneratedCode;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 高效编译器 - 集成增量编译和缓存
 *
 * 特性：
 * 1. 增量编译：只重新编译变更的文件
 * 2. 智能缓存：自动管理编译缓存
 * 3. 并行编译：支持并行编译独立模块
 * 4. 依赖追踪：精确追踪文件依赖关系
 */
@Slf4j
public class EfficientCompiler {

    // 核心组件
    private final CompilationPipeline fullPipeline;
    private final IncrementalCompiler incrementalCompiler;
    private final DependencyAnalyzer dependencyAnalyzer;
    private final FileChangeDetector changeDetector;
    private final CompilationCacheManager cacheManager;

    // 编译选项
    private final boolean incrementalEnabled;
    private final boolean parallelEnabled;
    private final int maxParallelThreads;

    // 监控器
    private final PerformanceMonitor monitor;

    public EfficientCompiler() {
        this(true, true, Runtime.getRuntime().availableProcessors());
    }

    public EfficientCompiler(boolean incrementalEnabled, boolean parallelEnabled, int maxThreads) {
        this.incrementalEnabled = incrementalEnabled;
        this.parallelEnabled = parallelEnabled;
        this.maxParallelThreads = maxThreads;

        // 初始化组件
        this.fullPipeline = new CompilationPipeline();
        this.incrementalCompiler = new IncrementalCompiler();
        this.dependencyAnalyzer = new DependencyAnalyzer();
        this.changeDetector = new FileChangeDetector(
            FileChangeDetector.DetectionStrategy.HYBRID, true);
        this.cacheManager = new CompilationCacheManager();
        this.monitor = new PerformanceMonitor();

        // 定期清理缓存
        scheduleCacheCleanup();
    }

    /**
     * 编译单个文件
     */
    public CompilationResult compile(String source, String fileName) {
        log.info("开始编译: {}", fileName);

        monitor.reset();

        // 1. 尝试从缓存获取
        if (incrementalEnabled) {
            IncrementalCompiler.CachedCompilationResult cached = cacheManager.get(fileName);
            if (cached != null) {
                log.debug("使用缓存结果: {}", fileName);
                return createCompilationResult(cached);
            }
        }

        // 2. 执行编译
        CompilationResult result;
        PerformanceMonitor.TimerContext timer = monitor.startTimer("编译");

        if (incrementalEnabled) {
            result = incrementalCompiler.compileIncremental(source, fileName);
        } else {
            result = fullPipeline.compile(source, fileName);
        }

        monitor.endTimer(timer);

        // 3. 缓存结果
        if (incrementalEnabled && result.isSuccess()) {
            cacheManager.put(fileName, createCachedResult(result), Collections.emptySet());
        }

        // 4. 打印统计
        if (result.isSuccess()) {
            monitor.printReport();
        }

        return result;
    }

    /**
     * 编译项目（多个文件）
     */
    public ProjectCompilationResult compileProject(Map<String, String> sourceFiles) {
        log.info("开始编译项目，{} 个文件", sourceFiles.size());

        monitor.reset();

        // 1. 分析依赖关系
        DependencyAnalyzer.DependencyResult dependencyResult =
            dependencyAnalyzer.analyzeDependencies(sourceFiles);

        // 2. 检测文件变更
        Set<String> changedFiles = changeDetector.detectChanges(sourceFiles.keySet());

        // 3. 确定编译策略
        if (changedFiles.isEmpty() && incrementalEnabled) {
            log.info("没有文件变更，使用全量缓存");
            return compileFromCache(sourceFiles.keySet());
        }

        // 4. 执行编译
        return compileFilesWithStrategy(sourceFiles, changedFiles, dependencyResult);
    }

    /**
     * 编译多个文件（并行）
     */
    private ProjectCompilationResult compileFilesWithStrategy(
        Map<String, String> sourceFiles,
        Set<String> changedFiles,
        DependencyAnalyzer.DependencyResult dependencyResult) {

        log.info("编译策略: {} 个文件变更，并行: {}",
                changedFiles.size(), parallelEnabled);

        // 1. 分组文件（根据依赖关系）
        Map<Boolean, Set<String>> fileGroups = groupFilesByDependencies(
            sourceFiles.keySet(), changedFiles, dependencyResult);

        Set<String> independentFiles = fileGroups.get(true);  // 可并行
        Set<String> dependentFiles = fileGroups.get(false);  // 需顺序

        ProjectCompilationResult result = new ProjectCompilationResult(
            sourceFiles.size(), 0, 0, new HashMap<>());

        // 2. 并行编译独立文件
        if (parallelEnabled && !independentFiles.isEmpty()) {
            compileInParallel(independentFiles, sourceFiles, changedFiles, result);
        } else {
            // 顺序编译
            for (String file : independentFiles) {
                compileSingleFile(file, sourceFiles.get(file), result);
            }
        }

        // 3. 顺序编译依赖文件
        for (String file : dependentFiles) {
            if (changedFiles.contains(file)) {
                compileSingleFile(file, sourceFiles.get(file), result);
            }
        }

        // 4. 更新依赖信息
        updateDependenciesAfterCompilation(result, dependencyResult);

        log.info("项目编译完成: 成功 {}, 失败 {}, 跳过 {}",
                result.getSuccessCount(),
                result.getFailureCount(),
                result.getSkippedCount());

        return result;
    }

    /**
     * 分组文件（可并行 vs 需顺序）
     */
    private Map<Boolean, Set<String>> groupFilesByDependencies(
        Set<String> allFiles,
        Set<String> changedFiles,
        DependencyAnalyzer.DependencyResult dependencyResult) {

        Set<String> independent = new HashSet<>();
        Set<String> dependent = new HashSet<>();

        for (String file : allFiles) {
            // 检查文件是否有依赖的文件也发生了变更
            Set<String> dependencies = dependencyResult.dependencyGraph().getOrDefault(file, Collections.emptySet());
            boolean hasChangedDependency = dependencies.stream().anyMatch(changedFiles::contains);

            if (hasChangedDependency) {
                dependent.add(file);
            } else {
                independent.add(file);
            }
        }

        return Map.of(true, independent, false, dependent);
    }

    /**
     * 并行编译文件
     */
    private void compileInParallel(Set<String> files,
                                  Map<String, String> sourceFiles,
                                  Set<String> changedFiles,
                                  ProjectCompilationResult result) {

        ExecutorService executor = Executors.newFixedThreadPool(
            Math.min(files.size(), maxParallelThreads));

        List<Future<CompilationResult>> futures = new ArrayList<>();

        for (String file : files) {
            if (changedFiles.contains(file)) {
                Future<CompilationResult> future = executor.submit(() -> {
                    return compileSingleFile(file, sourceFiles.get(file), result);
                });
                futures.add(future);
            }
        }

        // 等待所有任务完成
        for (Future<CompilationResult> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("并行编译失败", e);
            }
        }

        executor.shutdown();
    }

    /**
     * 编译单个文件
     */
    private CompilationResult compileSingleFile(String fileName, String source,
                                             ProjectCompilationResult result) {
        try {
            CompilationResult compileResult = compile(source, fileName);
            if (compileResult.isSuccess()) {
                result.addSuccess(fileName, compileResult);
            } else {
                result.addFailure(fileName, compileResult);
            }
            return compileResult;
        } catch (Exception e) {
            log.error("编译文件失败: {}", fileName, e);
            CompilationResult errorResult = CompilationResult.failure(
                fileName, "编译异常: " + e.getMessage(), 0);
            result.addFailure(fileName, errorResult);
            return errorResult;
        }
    }

    /**
     * 从缓存编译项目
     */
    private ProjectCompilationResult compileFromCache(Set<String> files) {
        ProjectCompilationResult result = new ProjectCompilationResult(
            files.size(), files.size(), 0, new HashMap<>());

        for (String file : files) {
            IncrementalCompiler.CachedCompilationResult cached = cacheManager.get(file);
            if (cached != null) {
                result.addSuccess(file, createCompilationResult(cached));
            } else {
                result.addSkipped(file, "缓存未找到");
            }
        }

        log.info("从缓存编译: {} 个文件", result.getSuccessCount());
        return result;
    }

    /**
     * 更新依赖信息
     */
    private void updateDependenciesAfterCompilation(
        ProjectCompilationResult result,
        DependencyAnalyzer.DependencyResult dependencyResult) {

        // 更新文件的依赖关系缓存
        for (Map.Entry<String, CompilationResult> entry : result.getResults().entrySet()) {
            String file = entry.getKey();
            if (entry.getValue().isSuccess()) {
                // 获取该文件的所有依赖
                Set<String> dependencies = dependencyResult.dependencyGraph()
                    .getOrDefault(file, Collections.emptySet());

                // 缓存编译结果
                cacheManager.put(file, createCachedResult(entry.getValue()), dependencies);
            }
        }
    }

    /**
     * 创建编译结果
     */
    private CompilationResult createCompilationResult(IncrementalCompiler.CachedCompilationResult cached) {
        return CompilationResult.success(cached.generatedCode(), cached.elapsedTime());
    }

    /**
     * 创建缓存结果
     */
    private IncrementalCompiler.CachedCompilationResult createCachedResult(CompilationResult result) {
        return new IncrementalCompiler.CachedCompilationResult(
            result.isSuccess(),
            result.getGeneratedCode(),
            result.getElapsedMillis()
        );
    }

    /**
     * 定期清理缓存
     */
    private void scheduleCacheCleanup() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            cacheManager.cleanupExpired();
            cacheManager.cleanupDiskSpace();
        }, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 获取编译统计信息
     */
    public CompilationStatistics getStatistics() {
        CompilationCacheManager.CacheStatistics cacheStats = cacheManager.getStatistics();
        return new CompilationStatistics(
            cacheStats.totalEntries(),
            cacheStats.totalSize(),
            monitor.getCallCount("编译"),
            monitor.getAverageTimeMillis("编译")
        );
    }

    /**
     * 项目编译结果
     */
    public static class ProjectCompilationResult {
        private final int totalFiles;
        private final int successCount;
        private final int failureCount;
        private int skippedCount;
        private final Map<String, CompilationResult> results;

        public ProjectCompilationResult(int totalFiles, int successCount,
                                       int failureCount, Map<String, CompilationResult> results) {
            this.totalFiles = totalFiles;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.skippedCount = 0;
            this.results = results;
        }

        public void addSuccess(String file, CompilationResult result) {
            results.put(file, result);
        }

        public void addFailure(String file, CompilationResult result) {
            results.put(file, result);
        }

        public void addSkipped(String file, String reason) {
            skippedCount++;
        }

        // Getters
        public int getTotalFiles() { return totalFiles; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public int getSkippedCount() { return skippedCount; }
        public Map<String, CompilationResult> getResults() { return results; }
    }

    /**
     * 编译统计信息
     */
    public record CompilationStatistics(
        long totalCachedEntries,
        long totalCacheSize,
        long totalCompilations,
        double averageCompileTime
    ) {}
}
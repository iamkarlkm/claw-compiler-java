package com.q3lives.compiler.pipeline;

import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.frontend.ASTNode;
import com.q3lives.compiler.frontend.Parser;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 增量编译器
 *
 * 实现思想：
 * 1. 文件依赖追踪：记录文件之间的依赖关系
 * 2. 变更检测：比较文件的修改时间或内容哈希
 * 3. 智能重编译：只重新编译变更的文件和其依赖
 * 4. 缓存机制：缓存已编译的结果
 */
@Slf4j
public class IncrementalCompiler {

    /** 编译缓存存储路径 */
    private final Path cacheDir;
    /** 依赖图：文件 -> 依赖的文件列表 */
    private final Map<String, Set<String>> dependencyGraph = new ConcurrentHashMap<>();
    /** 文件状态缓存：文件路径 -> 文件状态 */
    private final Map<String, FileState> fileStates = new ConcurrentHashMap<>();
    /** 编译结果缓存：文件路径 -> 编译结果 */
    private final Map<String, CachedCompilationResult> compilationCache = new ConcurrentHashMap<>();
    /** 最大缓存大小 */
    private static final int MAX_CACHE_SIZE = 1000;

    public IncrementalCompiler() {
        this.cacheDir = Paths.get(System.getProperty("user.dir"), ".claw-cache");
        initCacheDirectory();
    }

    /**
     * 初始化缓存目录
     */
    private void initCacheDirectory() {
        try {
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
            }
        } catch (IOException e) {
            log.warn("无法创建缓存目录: {}", cacheDir, e);
        }
    }

    /**
     * 执行增量编译
     */
    public CompilationResult compileIncremental(String source, String fileName) {
        log.info("开始增量编译: {}", fileName);

        // 1. 解析文件，收集依赖
        FileState currentState = analyzeFile(source, fileName);

        // 2. 检查文件是否变更
        if (!hasFileChanged(fileName, currentState)) {
            log.debug("文件未变更，使用缓存: {}", fileName);
            return getCachedResult(fileName);
        }

        // 3. 确定需要重新编译的文件
        Set<String> filesToRecompile = determineFilesToRecompile(fileName);

        // 4. 执行编译
        return compileAffectedFiles(filesToRecompile, source, fileName);
    }

    /**
     * 分析文件，收集依赖
     */
    private FileState analyzeFile(String source, String fileName) {
        log.debug("分析文件依赖: {}", fileName);

        // 解析AST
        Parser parser = new Parser();
        ASTNode ast = parser.parse(List.of()); // 这里需要从source创建tokens

        // 收集依赖（简化实现）
        Set<String> dependencies = collectDependencies(ast);

        // 计算文件哈希
        String contentHash = calculateContentHash(source);

        // 创建文件状态
        FileState state = new FileState(
            System.currentTimeMillis(),
            contentHash,
            dependencies
        );

        // 更新状态
        fileStates.put(fileName, state);
        dependencyGraph.put(fileName, dependencies);

        return state;
    }

    /**
     * 收集文件依赖
     */
    private Set<String> collectDependencies(ASTNode ast) {
        Set<String> dependencies = new HashSet<>();

        // 收集import语句
        collectImportDependencies(ast, dependencies);

        // 收含函数依赖
        collectFunctionDependencies(ast, dependencies);

        return dependencies;
    }

    /**
     * 收集import依赖
     */
    private void collectImportDependencies(ASTNode ast, Set<String> dependencies) {
        for (ASTNode node : ast.getChildren()) {
            if (node.getType() == ASTNode.NodeType.IMPORT_DECLARATION) {
                String path = node.getAttribute("path");
                if (path != null) {
                    dependencies.add(path);
                }
            }
            // 递归处理子节点
            collectImportDependencies(node, dependencies);
        }
    }

    /**
     * 收集函数依赖（简化实现）
     */
    private void collectFunctionDependencies(ASTNode ast, Set<String> dependencies) {
        // 这里可以添加更复杂的依赖分析
        // 例如：分析函数调用、类型引用等
    }

    /**
     * 检查文件是否变更
     */
    private boolean hasFileChanged(String fileName, FileState currentState) {
        FileState previousState = fileStates.get(fileName);

        if (previousState == null) {
            return true; // 新文件
        }

        // 比较哈希值
        return !currentState.contentHash.equals(previousState.contentHash);
    }

    /**
     * 获取缓存的编译结果
     */
    private CompilationResult getCachedResult(String fileName) {
        CachedCompilationResult cached = compilationCache.get(fileName);

        if (cached == null) {
            log.warn("缓存未找到: {}", fileName);
            return null;
        }

        // 创建新的CompilationResult实例（避免返回引用）
        return new CompilationResult(
            cached.getSuccess(),
            cached.getGeneratedCode(),
            cached.getElapsedTime()
        );
    }

    /**
     * 确定需要重新编译的文件
     */
    private Set<String> determineFilesToRecompile(String changedFile) {
        Set<String> toRecompile = new HashSet<>();
        Set<String> visited = new HashSet<>();

        // 广度优先搜索，找到所有受影响的文件
        Queue<String> queue = new LinkedList<>();
        queue.add(changedFile);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            // 添加当前文件到重编译列表
            toRecompile.add(current);

            // 找到所有依赖当前文件的文件
            for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
                if (entry.getValue().contains(current)) {
                    queue.add(entry.getKey());
                }
            }
        }

        log.info("需要重新编译的文件: {}", toRecompile.size());
        return toRecompile;
    }

    /**
     * 编译受影响的文件
     */
    private CompilationResult compileAffectedFiles(Set<String> filesToRecompile,
                                                 String source, String mainFile) {
        log.info("开始编译 {} 个文件", filesToRecompile.size());

        // 使用原始的编译管道
        CompilationPipeline pipeline = new CompilationPipeline();

        // 编译主文件
        CompilationResult result = pipeline.compile(source, mainFile);

        // 缓存结果
        if (result.isSuccess()) {
            cacheResult(mainFile, result);
        }

        return result;
    }

    /**
     * 缓存编译结果
     */
    private void cacheResult(String fileName, CompilationResult result) {
        // 清理旧缓存
        if (compilationCache.size() >= MAX_CACHE_SIZE) {
            cleanupCache();
        }

        // 缓存新结果
        CachedCompilationResult cached = new CachedCompilationResult(
            result.getSuccess(),
            result.getGeneratedCode(),
            result.getElapsedTime()
        );

        compilationCache.put(fileName, cached);

        // 持久化缓存到磁盘
        persistCache(fileName, cached);
    }

    /**
     * 清理缓存
     */
    private void cleanupCache() {
        // 简单的LRU策略：删除一半的缓存
        List<String> keys = new ArrayList<>(compilationCache.keySet());
        int toRemove = keys.size() / 2;

        for (int i = 0; i < toRemove; i++) {
            String key = keys.get(i);
            compilationCache.remove(key);
            deleteCachedFile(key);
        }

        log.info("缓存清理完成，剩余缓存: {}", compilationCache.size());
    }

    /**
     * 持久化缓存到磁盘
     */
    private void persistCache(String fileName, CachedCompilationResult result) {
        Path cacheFile = getCacheFilePath(fileName);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(cacheFile))) {
            oos.writeObject(result);
        } catch (IOException e) {
            log.warn("持久化缓存失败: {}", fileName, e);
        }
    }

    /**
     * 从磁盘加载缓存
     */
    private CachedCompilationResult loadCachedResult(String fileName) {
        Path cacheFile = getCacheFilePath(fileName);

        if (!Files.exists(cacheFile)) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                Files.newInputStream(cacheFile))) {
            return (CachedCompilationResult) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.warn("加载缓存失败: {}", fileName, e);
            return null;
        }
    }

    /**
     * 删除缓存的文件
     */
    private void deleteCachedFile(String fileName) {
        Path cacheFile = getCacheFilePath(fileName);
        try {
            Files.deleteIfExists(cacheFile);
        } catch (IOException e) {
            log.warn("删除缓存文件失败: {}", fileName, e);
        }
    }

    /**
     * 获取缓存文件路径
     */
    private Path getCacheFilePath(String fileName) {
        String safeName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
        return cacheDir.resolve(safeName + ".cache");
    }

    /**
     * 计算内容哈希
     */
    private String calculateContentHash(String content) {
        // 简单的哈希实现，实际可以使用更安全的哈希算法
        return content.hashCode() + "";
    }

    /**
     * 文件状态
     */
    public record FileState(long lastModified, String contentHash, Set<String> dependencies) {}

    /**
     * 缓存的编译结果
     */
    public record CachedCompilationResult(
        boolean success,
        GeneratedCode generatedCode,
        long elapsedTime
    ) {}
}
package com.q3lives.compiler.pipeline;

import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 编译缓存管理器
 *
 * 管理编译结果的持久化缓存，支持：
 * 1. 增量编译的缓存
 * 2. 版本管理
 * 3. 缓存失效策略
 * 4. 磁盘缓存清理
 */
@Slf4j
public class CompilationCacheManager {

    /** 缓存根目录 */
    private final Path cacheRootDir;
    /** 内存缓存 */
    private final Map<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();
    /** 最大内存缓存大小 */
    private static final int MAX_MEMORY_CACHE_SIZE = 500;
    /** 缓存过期时间（毫秒） */
    private static final long CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24小时

    /**
     * 缓存条目
     */
    public static class CacheEntry {
        private final String key;
        private final IncrementalCompiler.CachedCompilationResult result;
        private final long timestamp;
        private final Set<String> dependencies;
        private final long size;

        public CacheEntry(String key, IncrementalCompiler.CachedCompilationResult result,
                         Set<String> dependencies, long size) {
            this.key = key;
            this.result = result;
            this.timestamp = System.currentTimeMillis();
            this.dependencies = dependencies;
            this.size = size;
        }

        // Getters
        public String getKey() { return key; }
        public IncrementalCompiler.CachedCompilationResult getResult() { return result; }
        public long getTimestamp() { return timestamp; }
        public Set<String> getDependencies() { return dependencies; }
        public long getSize() { return size; }
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }

    public CompilationCacheManager() {
        this.cacheRootDir = Paths.get(System.getProperty("user.dir"), ".claw-compiler-cache");
        initCacheDirectory();
        loadDiskCache();
    }

    /**
     * 初始化缓存目录
     */
    private void initCacheDirectory() {
        try {
            if (!Files.exists(cacheRootDir)) {
                Files.createDirectories(cacheRootDir);
            }

            // 创建子目录
            Files.createDirectories(cacheRootDir.resolve("metadata"));
            Files.createDirectories(cacheRootDir.resolve("dependencies"));
        } catch (IOException e) {
            log.warn("无法创建缓存目录: {}", cacheRootDir, e);
        }
    }

    /**
     * 加载磁盘缓存
     */
    private void loadDiskCache() {
        log.info("加载磁盘缓存");

        try {
            Path metadataDir = cacheRootDir.resolve("metadata");
            if (Files.exists(metadataDir)) {
                Files.list(metadataDir)
                    .filter(p -> p.toString().endsWith(".meta"))
                    .forEach(this::loadCacheMetadata);
            }
        } catch (IOException e) {
            log.warn("加载磁盘缓存失败", e);
        }
    }

    /**
     * 加载缓存元数据
     */
    private void loadCacheMetadata(Path metadataFile) {
        try (ObjectInputStream ois = new ObjectInputStream(
                Files.newInputStream(metadataFile))) {

            CacheEntry entry = (CacheEntry) ois.readObject();

            // 检查是否过期
            if (!entry.isExpired()) {
                memoryCache.put(entry.getKey(), entry);
            } else {
                // 删除过期缓存
                deleteCache(entry.getKey());
            }
        } catch (IOException | ClassNotFoundException e) {
            log.warn("加载缓存元数据失败: {}", metadataFile, e);
        }
    }

    /**
     * 获取缓存结果
     */
    public IncrementalCompiler.CachedCompilationResult get(String key) {
        // 1. 检查内存缓存
        CacheEntry entry = memoryCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getResult();
        }

        // 2. 检查磁盘缓存
        Path cacheFile = getCacheFilePath(key);
        if (Files.exists(cacheFile)) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    Files.newInputStream(cacheFile))) {

                IncrementalCompiler.CachedCompilationResult result =
                    (IncrementalCompiler.CachedCompilationResult) ois.readObject();

                // 加载到内存缓存
                addToMemoryCache(key, result, Collections.emptySet(),
                                 Files.size(cacheFile));

                return result;
            } catch (IOException | ClassNotFoundException e) {
                log.warn("从磁盘加载缓存失败: {}", key, e);
            }
        }

        return null;
    }

    /**
     * 缓存编译结果
     */
    public void put(String key, IncrementalCompiler.CachedCompilationResult result,
                    Set<String> dependencies) {
        // 1. 添加到内存缓存
        long size = estimateResultSize(result);
        addToMemoryCache(key, result, dependencies, size);

        // 2. 持久化到磁盘
        persistToDisk(key, result, dependencies);

        log.debug("缓存编译结果: {}", key);
    }

    /**
     * 添加到内存缓存
     */
    private void addToMemoryCache(String key, IncrementalCompiler.CachedCompilationResult result,
                                 Set<String> dependencies, long size) {
        // LRU淘汰策略
        if (memoryCache.size() >= MAX_MEMORY_CACHE_SIZE) {
            evictLRUEntry();
        }

        CacheEntry entry = new CacheEntry(key, result, dependencies, size);
        memoryCache.put(key, entry);
    }

    /**
     * 淘汰最久未使用的条目
     */
    private void evictLRUEntry() {
        Optional<CacheEntry> lruEntry = memoryCache.values().stream()
            .min(Comparator.comparingLong(CacheEntry::getTimestamp));

        lruEntry.ifPresent(entry -> {
            memoryCache.remove(entry.getKey());
            deleteCache(entry.getKey());
            log.debug("淘汰缓存: {}", entry.getKey());
        });
    }

    /**
     * 持久化到磁盘
     */
    private void persistToDisk(String key, IncrementalCompiler.CachedCompilationResult result,
                             Set<String> dependencies) {
        try {
            // 保存编译结果
            Path cacheFile = getCacheFilePath(key);
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    Files.newOutputStream(cacheFile))) {
                oos.writeObject(result);
            }

            // 保存元数据
            CacheEntry entry = new CacheEntry(key, result, dependencies,
                                            Files.size(cacheFile));
            saveMetadata(key, entry);

            // 保存依赖关系
            saveDependencies(key, dependencies);

        } catch (IOException e) {
            log.warn("持久化缓存失败: {}", key, e);
        }
    }

    /**
     * 保存元数据
     */
    private void saveMetadata(String key, CacheEntry entry) {
        Path metadataFile = getMetadataFilePath(key);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(metadataFile))) {
            oos.writeObject(entry);
        } catch (IOException e) {
            log.warn("保存元数据失败: {}", key, e);
        }
    }

    /**
     * 保存依赖关系
     */
    private void saveDependencies(String key, Set<String> dependencies) {
        Path dependencyFile = getDependencyFilePath(key);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(dependencyFile))) {
            oos.writeObject(new ArrayList<>(dependencies));
        } catch (IOException e) {
            log.warn("保存依赖关系失败: {}", key, e);
        }
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        // 删除内存缓存
        memoryCache.remove(key);

        // 删除磁盘文件
        deleteCache(key);

        log.debug("删除缓存: {}", key);
    }

    /**
     * 删除缓存文件
     */
    private void deleteCache(String key) {
        try {
            Files.deleteIfExists(getCacheFilePath(key));
            Files.deleteIfExists(getMetadataFilePath(key));
            Files.deleteIfExists(getDependencyFilePath(key));
        } catch (IOException e) {
            log.warn("删除缓存文件失败: {}", key, e);
        }
    }

    /**
     * 清理过期缓存
     */
    public void cleanupExpired() {
        log.info("清理过期缓存");

        Set<String> expiredKeys = memoryCache.entrySet().stream()
            .filter(e -> e.getValue().isExpired())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        expiredKeys.forEach(this::delete);
        log.info("清理完成，删除 {} 个过期缓存", expiredKeys.size());
    }

    /**
     * 清理磁盘空间
     */
    public void cleanupDiskSpace() {
        log.info("清理磁盘空间");

        try {
            // 统计当前使用空间
            long usedSpace = Files.walk(cacheRootDir)
                .filter(p -> !Files.isDirectory(p))
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();

            long maxSpace = 1024 * 1024 * 1024; // 1GB
            if (usedSpace > maxSpace) {
                // 删除最老的文件直到空间足够
                deleteOldestFiles(maxSpace);
            }
        } catch (IOException e) {
            log.warn("清理磁盘空间失败", e);
        }
    }

    /**
     * 删除最老的文件
     */
    private void deleteOldestFiles(long maxSpace) throws IOException {
        List<Path> allFiles = new ArrayList<>();
        Files.walk(cacheRootDir)
            .filter(p -> !Files.isDirectory(p))
            .forEach(allFiles::add);

        // 按修改时间排序
        allFiles.sort(Comparator.comparingLong(p -> {
            try {
                return Files.getLastModifiedTime(p).toMillis();
            } catch (IOException e) {
                return Long.MAX_VALUE;
            }
        }));

        long currentSpace = Files.walk(cacheRootDir)
            .filter(p -> !Files.isDirectory(p))
            .mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    return 0;
                }
            })
            .sum();

        // 删除文件直到空间足够
        for (Path file : allFiles) {
            if (currentSpace <= maxSpace) break;

            long fileSize = Files.size(file);
            Files.deleteIfExists(file);
            currentSpace -= fileSize;
        }
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStatistics getStatistics() {
        long totalEntries = memoryCache.size();
        long totalSize = memoryCache.values().stream()
            .mapToLong(CacheEntry::getSize)
            .sum();
        long expiredEntries = memoryCache.values().stream()
            .mapToLong(e -> e.isExpired() ? 1 : 0)
            .sum();

        return new CacheStatistics(totalEntries, totalSize, expiredEntries);
    }

    /**
     * 估算结果大小
     */
    private long estimateResultSize(IncrementalCompiler.CachedCompilationResult result) {
        // 简单估算：假设每个结果1KB
        return 1024;
    }

    /**
     * 获取缓存文件路径
     */
    private Path getCacheFilePath(String key) {
        String safeKey = key.replaceAll("[^a-zA-Z0-9.-]", "_");
        return cacheRootDir.resolve(safeKey + ".cache");
    }

    /**
     * 获取元数据文件路径
     */
    private Path getMetadataFilePath(String key) {
        String safeKey = key.replaceAll("[^a-zA-Z0-9.-]", "_");
        return cacheRootDir.resolve("metadata").resolve(safeKey + ".meta");
    }

    /**
     * 获取依赖文件路径
     */
    private Path getDependencyFilePath(String key) {
        String safeKey = key.replaceAll("[^a-zA-Z0-9.-]", "_");
        return cacheRootDir.resolve("dependencies").resolve(safeKey + ".deps");
    }

    /**
     * 缓存统计信息
     */
    public record CacheStatistics(
        long totalEntries,
        long totalSize,
        long expiredEntries
    ) {}
}
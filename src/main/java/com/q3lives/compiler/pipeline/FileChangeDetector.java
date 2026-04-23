package com.q3lives.compiler.pipeline;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件变更检测器
 *
 * 实现多种变更检测策略：
 * 1. 基于修改时间
 * 2. 基于内容哈希
 * 3. 基于文件大小
 */
@Slf4j
public class FileChangeDetector {

    /** 检测策略 */
    public enum DetectionStrategy {
        MODIFICATION_TIME,  // 基于修改时间
        CONTENT_HASH,      // 基于内容哈希
        FILE_SIZE,         // 基于文件大小
        HYBRID            // 混合策略（优先使用修改时间，内容哈希作为补充）
    }

    /** 文件信息缓存 */
    private final Map<String, FileInfo> fileCache = new ConcurrentHashMap<>();
    /** 检测策略 */
    private final DetectionStrategy strategy;
    /** 是否忽略空文件 */
    private final boolean ignoreEmptyFiles;

    public FileChangeDetector() {
        this(DetectionStrategy.HYBRID);
    }

    public FileChangeDetector(DetectionStrategy strategy) {
        this(strategy, false);
    }

    public FileChangeDetector(DetectionStrategy strategy, boolean ignoreEmptyFiles) {
        this.strategy = strategy;
        this.ignoreEmptyFiles = ignoreEmptyFiles;
    }

    /**
     * 检测文件变更
     */
    public Set<String> detectChanges(Set<String> filePaths) {
        log.info("开始检测文件变更，策略: {}", strategy);

        Set<String> changedFiles = new HashSet<>();
        Set<String> deletedFiles = new HashSet<>();

        // 1. 检查现有文件
        for (String filePath : filePaths) {
            if (isFileChanged(filePath)) {
                changedFiles.add(filePath);
                log.debug("文件已变更: {}", filePath);
            }
        }

        // 2. 检查删除的文件
        Set<String> cachedFiles = new HashSet<>(fileCache.keySet());
        cachedFiles.removeAll(filePaths);
        deletedFiles.addAll(cachedFiles);

        // 3. 更新缓存
        updateFileCache(filePaths, deletedFiles);

        log.info("检测完成: {} 个文件变更, {} 个文件删除",
                changedFiles.size(), deletedFiles.size());

        return changedFiles;
    }

    /**
     * 检查单个文件是否变更
     */
    private boolean isFileChanged(String filePath) {
        try {
            Path path = Paths.get(filePath);

            // 检查文件是否存在
            if (!Files.exists(path)) {
                log.debug("文件不存在: {}", filePath);
                return false;
            }

            // 检查是否是空文件
            if (ignoreEmptyFiles && Files.size(path) == 0) {
                return false;
            }

            // 获取当前文件信息
            FileInfo currentInfo = getFileInfo(path);

            // 从缓存获取旧信息
            FileInfo cachedInfo = fileCache.get(filePath);

            if (cachedInfo == null) {
                // 新文件
                log.debug("新文件: {}", filePath);
                return true;
            }

            // 根据策略比较
            switch (strategy) {
                case MODIFICATION_TIME:
                    return compareModificationTime(currentInfo, cachedInfo);
                case CONTENT_HASH:
                    return compareContentHash(currentInfo, cachedInfo);
                case FILE_SIZE:
                    return compareFileSize(currentInfo, cachedInfo);
                case HYBRID:
                    return compareHybrid(currentInfo, cachedInfo);
                default:
                    return false;
            }
        } catch (IOException e) {
            log.warn("检查文件变更失败: {}", filePath, e);
            return true; // 出错时认为文件已变更
        }
    }

    /**
     * 比较修改时间
     */
    private boolean compareModificationTime(FileInfo current, FileInfo cached) {
        return current.lastModified() > cached.lastModified();
    }

    /**
     * 比较内容哈希
     */
    private boolean compareContentHash(FileInfo current, FileInfo cached) {
        return !current.contentHash().equals(cached.contentHash());
    }

    /**
     * 比较文件大小
     */
    private boolean compareFileSize(FileInfo current, FileInfo cached) {
        return current.size() != cached.size();
    }

    /**
     * 混合策略比较
     */
    private boolean compareHybrid(FileInfo current, FileInfo cached) {
        // 1. 首先比较修改时间
        if (current.lastModified() > cached.lastModified()) {
            // 如果修改时间有变化，比较内容哈希
            return !current.contentHash().equals(cached.contentHash());
        }

        // 2. 修改时间未变，不认为是变更
        return false;
    }

    /**
     * 获取文件信息
     */
    private FileInfo getFileInfo(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        long lastModified = attrs.lastModifiedTime().toMillis();
        long size = attrs.size();

        // 计算内容哈希
        String contentHash = calculateContentHash(path);

        return new FileInfo(lastModified, size, contentHash);
    }

    /**
     * 计算内容哈希
     */
    private String calculateContentHash(Path path) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            // 如果不支持SHA-256，使用简单的哈希
            return String.valueOf(Files.size(path));
        }

        // 读取文件内容并计算哈希
        byte[] content = Files.readAllBytes(path);
        byte[] hashBytes = md.digest(content);

        // 转换为十六进制字符串
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    /**
     * 更新文件缓存
     */
    private void updateFileCache(Set<String> existingFiles, Set<String> deletedFiles) {
        // 更新现有文件的缓存
        for (String filePath : existingFiles) {
            try {
                Path path = Paths.get(filePath);
                FileInfo info = getFileInfo(path);
                fileCache.put(filePath, info);
            } catch (IOException e) {
                log.warn("更新文件缓存失败: {}", filePath, e);
            }
        }

        // 删除不存在的文件
        for (String deletedFile : deletedFiles) {
            fileCache.remove(deletedFile);
            log.debug("从缓存移除文件: {}", deletedFile);
        }

        // 控制缓存大小
        if (fileCache.size() > 10000) {
            cleanupCache();
        }
    }

    /**
     * 清理缓存
     */
    private void cleanupCache() {
        // 简单的LRU策略：删除30%的缓存
        int toRemove = fileCache.size() / 3;
        List<String> keys = new ArrayList<>(fileCache.keySet());

        for (int i = 0; i < toRemove; i++) {
            String key = keys.get(i);
            fileCache.remove(key);
        }

        log.info("缓存清理完成，剩余: {} 个文件", fileCache.size());
    }

    /**
     * 获取文件统计信息
     */
    public FileStatistics getStatistics() {
        long totalFiles = fileCache.size();
        long totalSize = fileCache.values().stream()
            .mapToLong(FileInfo::size)
            .sum();

        return new FileStatistics(totalFiles, totalSize);
    }

    /**
     * 文件信息
     */
    public record FileInfo(
        long lastModified,
        long size,
        String contentHash
    ) {}

    /**
     * 文件统计信息
     */
    public record FileStatistics(
        long totalFiles,
        long totalSize
    ) {}
}
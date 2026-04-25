package com.q3lives.lsp.utils;

import java.nio.file.*;
import java.util.*;

/**
 * 文件监视器 - 监视工作区文件变更
 */
public class FileWatcher {

    private final List<Path> watchedPaths = new ArrayList<>();

    public FileWatcher() {
    }

    /**
     * 添加监视路径
     */
    public void watch(Path path) {
        watchedPaths.add(path);
    }

    /**
     * 获取所有监视的路径
     */
    public List<Path> getWatchedPaths() {
        return new ArrayList<>(watchedPaths);
    }
}

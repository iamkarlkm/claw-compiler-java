package com.q3lives.compiler.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 路径验证器 - 防止路径遍历攻击
 */
public class PathValidator {

    /**
     * 验证文件路径是否在允许的范围内
     * @param filePath 要验证的文件路径
     * @return 验证后的绝对路径
     * @throws IOException 如果路径无效或越权
     */
    public static Path validateFilePath(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        // 规范化路径，解析 . 和 ..
        Path baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path resolvedPath = baseDir.resolve(filePath).normalize();

        // 检查是否在允许的范围内
        if (!resolvedPath.startsWith(baseDir)) {
            throw new SecurityException("访问工作目录外的路径被拒绝: " + filePath);
        }

        return resolvedPath;
    }

    /**
     * 验证输出路径是否安全
     * @param outputPath 输出路径
     * @return 验证后的绝对路径
     * @throws IOException 如果路径无效
     */
    public static Path validateOutputPath(String outputPath) throws IOException {
        Path path = validateFilePath(outputPath);

        // 确保父目录存在
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        return path;
    }
}
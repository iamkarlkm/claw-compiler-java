package com.claw.compiler;

import java.util.Map;

import com.claw.binding.java.JavaCodeGenerator;
import com.claw.pipeline.ClawCompilerPipeline;
import com.claw.pipeline.CompilationResult;

public class Main {
    
    public static void main(String[] args) {
        // 构建编译管道 - 使用Java绑定层
        ClawCompilerPipeline pipeline = ClawCompilerPipeline.builder()
            .withDefaultFrontend()
            .withCodeGenerator(new JavaCodeGenerator())  // ← Java绑定层
            .build();
        
        // 编译
        String clawSource = readFile("example.claw");
        CompilationResult result = pipeline.compile(clawSource, "Example");
        
        if (result.isSuccess()) {
            System.out.println("编译成功！已完成阶段: " + result.getCompletedPhases());
            
            // 输出生成的文件
            for (Map.Entry<String, String> entry : result.getGeneratedFiles().entrySet()) {
                System.out.println("=== " + entry.getKey() + " ===");
                System.out.println(entry.getValue());
                writeFile("output/" + entry.getKey(), entry.getValue());
            }
        } else {
            System.err.println("编译失败：");
            result.getErrors().forEach(e -> System.err.println("  ERROR: " + e));
        }
        
        if (!result.getWarnings().isEmpty()) {
            result.getWarnings().forEach(w -> System.out.println("  WARN: " + w));
        }
    }

    private static void writeFile(String filePath, String content) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.write(path, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }

    private static String readFile(String filePath) {
        try {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)),
                java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }
}

package com.claw.binding;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 代码生成配置
 *
 * <p>配置代码生成的各种选项，包括输出格式、缩进风格、注释生成等。</p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * GenerationConfig config = GenerationConfig.builder()
 *     .outputDirectory("build/generated")
 *     .indentStyle("  ")  // 2空格缩进
 *     .generateComments(true)
 *     .build();
 * }</pre>
 *
 * @author Claw Compiler Team
 * @since 3.0.0
 */
public class GenerationConfig {

    /** 输出目录 */
    private String outputDirectory;

    /** 是否生成注释 */
    private boolean generateComments = true;

    /** 是否优化输出 */
    private boolean optimizeOutput = false;

    /** 缩进风格（默认4空格） */
    private String indentStyle = "    ";

    /** 行分隔符 */
    private String lineSeparator = System.lineSeparator();

    /** 文件编码 */
    private String encoding = "UTF-8";

    /** 是否生成源码映射 */
    private boolean generateSourceMap = false;

    /** 是否生成调试信息 */
    private boolean generateDebugInfo = false;

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     */
    public GenerationConfig() {
    }

    /**
     * 构造函数
     *
     * @param outputDirectory 输出目录
     */
    public GenerationConfig(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建默认配置
     *
     * @return 默认配置实例
     */
    public static GenerationConfig defaultConfig() {
        return new GenerationConfig();
    }

    /**
     * 创建构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    // ==================== Getter 方法 ====================

    /**
     * 获取输出目录
     *
     * @return 输出目录路径
     */
    public String getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * 获取输出目录路径对象
     *
     * @return Path 对象
     */
    public Path getOutputPath() {
        return outputDirectory != null ? Paths.get(outputDirectory) : Paths.get(".");
    }

    /**
     * 获取行分隔符
     *
     * @return 行分隔符字符串
     */
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * 获取缩进风格
     *
     * @return 缩进字符串
     */
    public String getIndentStyle() {
        return indentStyle;
    }

    /**
     * 是否生成注释
     *
     * @return 如果生成注释返回 true
     */
    public boolean isGenerateComments() {
        return generateComments;
    }

    /**
     * 是否优化输出
     *
     * @return 如果优化输出返回 true
     */
    public boolean isOptimizeOutput() {
        return optimizeOutput;
    }

    /**
     * 获取文件编码
     *
     * @return 编码名称
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * 是否生成源码映射
     *
     * @return 如果生成源码映射返回 true
     */
    public boolean isGenerateSourceMap() {
        return generateSourceMap;
    }

    /**
     * 是否生成调试信息
     *
     * @return 如果生成调试信息返回 true
     */
    public boolean isGenerateDebugInfo() {
        return generateDebugInfo;
    }

    // ==================== Setter 方法 ====================

    /**
     * 设置输出目录
     */
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * 设置是否生成注释
     */
    public void setGenerateComments(boolean generateComments) {
        this.generateComments = generateComments;
    }

    /**
     * 设置是否优化输出
     */
    public void setOptimizeOutput(boolean optimizeOutput) {
        this.optimizeOutput = optimizeOutput;
    }

    /**
     * 设置缩进风格
     */
    public void setIndentStyle(String indentStyle) {
        this.indentStyle = indentStyle;
    }

    /**
     * 设置缩进为制表符
     */
    public void useTabIndent() {
        this.indentStyle = "\t";
    }

    /**
     * 设置缩进为指定数量的空格
     *
     * @param spaces 空格数量
     */
    public void useSpaceIndent(int spaces) {
        this.indentStyle = " ".repeat(Math.max(0, spaces));
    }

    /**
     * 设置行分隔符
     */
    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    /**
     * 设置文件编码
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * 设置是否生成源码映射
     */
    public void setGenerateSourceMap(boolean generateSourceMap) {
        this.generateSourceMap = generateSourceMap;
    }

    /**
     * 设置是否生成调试信息
     */
    public void setGenerateDebugInfo(boolean generateDebugInfo) {
        this.generateDebugInfo = generateDebugInfo;
    }

    // ==================== 构建器 ====================

    /**
     * 构建器类
     */
    public static class Builder {
        private final GenerationConfig config = new GenerationConfig();

        /**
         * 设置输出目录
         */
        public Builder outputDirectory(String outputDirectory) {
            config.setOutputDirectory(outputDirectory);
            return this;
        }

        /**
         * 设置是否生成注释
         */
        public Builder generateComments(boolean generateComments) {
            config.setGenerateComments(generateComments);
            return this;
        }

        /**
         * 设置是否优化输出
         */
        public Builder optimizeOutput(boolean optimizeOutput) {
            config.setOptimizeOutput(optimizeOutput);
            return this;
        }

        /**
         * 设置缩进风格
         */
        public Builder indentStyle(String indentStyle) {
            config.setIndentStyle(indentStyle);
            return this;
        }

        /**
         * 设置行分隔符
         */
        public Builder lineSeparator(String lineSeparator) {
            config.setLineSeparator(lineSeparator);
            return this;
        }

        /**
         * 设置文件编码
         */
        public Builder encoding(String encoding) {
            config.setEncoding(encoding);
            return this;
        }

        /**
         * 设置是否生成源码映射
         */
        public Builder generateSourceMap(boolean generateSourceMap) {
            config.setGenerateSourceMap(generateSourceMap);
            return this;
        }

        /**
         * 设置是否生成调试信息
         */
        public Builder generateDebugInfo(boolean generateDebugInfo) {
            config.setGenerateDebugInfo(generateDebugInfo);
            return this;
        }

        /**
         * 构建配置实例
         */
        public GenerationConfig build() {
            return config;
        }
    }

    @Override
    public String toString() {
        return "GenerationConfig{" +
                "outputDirectory='" + outputDirectory + '\'' +
                ", generateComments=" + generateComments +
                ", optimizeOutput=" + optimizeOutput +
                ", indentStyle='" + indentStyle.replace("\t", "\\t") + '\'' +
                ", encoding='" + encoding + '\'' +
                '}';
    }
}

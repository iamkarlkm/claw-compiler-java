// ==================== CompilerError.java ====================
package com.q3lives.compiler.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 编译错误
 *
 * <p>表示编译过程中发生的错误，包含错误码、消息、位置和严重程度。</p>
 *
 * <h2>错误示例</h2>
 * <pre>{@code
 * CompilerError error = CompilerError.builder()
 *     .code(ErrorCode.SYNTAX_ERROR)
 *     .message("缺少分号")
 *     .line(42)
 *     .column(15)
 *     .severity(ErrorSeverity.ERROR)
 *     .build();
 * }</pre>
 *
 * @author Claw Compiler Team
 * @since 3.1.0
 */
public class CompilerError {

    private final ErrorCode code;
    private final String message;
    private final int line;
    private final int column;
    private final ErrorSeverity severity;
    private final String sourceFile;
    private final String context;
    private final Throwable cause;

    private CompilerError(Builder builder) {
        this.code = builder.code;
        this.message = builder.message;
        this.line = builder.line;
        this.column = builder.column;
        this.severity = builder.severity;
        this.sourceFile = builder.sourceFile;
        this.context = builder.context;
        this.cause = builder.cause;
    }

    // ==================== Getters ====================

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public ErrorCode getCode() {
        return code;
    }

    /**
     * 获取错误消息
     *
     * @return 错误消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取行号
     *
     * @return 行号，未知时为 -1
     */
    public int getLine() {
        return line;
    }

    /**
     * 获取列号
     *
     * @return 列号，未知时为 -1
     */
    public int getColumn() {
        return column;
    }

    /**
     * 获取严重程度
     *
     * @return 严重程度
     */
    public ErrorSeverity getSeverity() {
        return severity;
    }

    /**
     * 获取源文件名
     *
     * @return 源文件名，可能为 null
     */
    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * 获取上下文代码片段
     *
     * @return 上下文，可能为 null
     */
    public String getContext() {
        return context;
    }

    /**
     * 获取原因异常
     *
     * @return 原因异常，可能为 null
     */
    public Throwable getCause() {
        return cause;
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建语法错误
     *
     * @param message 错误消息
     * @param line 行号
     * @return 错误实例
     */
    public static CompilerError syntaxError(String message, int line) {
        return builder()
            .code(ErrorCode.SYNTAX_ERROR)
            .message(message)
            .line(line)
            .severity(ErrorSeverity.ERROR)
            .build();
    }

    /**
     * 创建语义错误
     *
     * @param message 错误消息
     * @param line 行号
     * @return 错误实例
     */
    public static CompilerError semanticError(String message, int line) {
        return builder()
            .code(ErrorCode.SEMANTIC_ERROR)
            .message(message)
            .line(line)
            .severity(ErrorSeverity.ERROR)
            .build();
    }

    /**
     * 创建类型错误
     *
     * @param message 错误消息
     * @param line 行号
     * @return 错误实例
     */
    public static CompilerError typeError(String message, int line) {
        return builder()
            .code(ErrorCode.TYPE_ERROR)
            .message(message)
            .line(line)
            .severity(ErrorSeverity.ERROR)
            .build();
    }

    /**
     * 创建警告
     *
     * @param message 警告消息
     * @param line 行号
     * @return 错误实例
     */
    public static CompilerError warning(String message, int line) {
        return builder()
            .code(ErrorCode.WARNING)
            .message(message)
            .line(line)
            .severity(ErrorSeverity.WARNING)
            .build();
    }

    // ==================== 格式化输出 ====================

    /**
     * 格式化为可读字符串
     *
     * @return 格式化后的错误信息
     */
    public String format() {
        StringBuilder sb = new StringBuilder();

        // 严重程度和错误码
        sb.append(String.format("[%s %s] ", severity.getDisplay(), code.getCode()));

        // 位置信息
        if (sourceFile != null) {
            sb.append(sourceFile).append(":");
        }
        if (line >= 0) {
            sb.append(line);
            if (column >= 0) {
                sb.append(":").append(column);
            }
            sb.append(": ");
        }

        // 错误消息
        sb.append(message);

        // 上下文
        if (context != null) {
            sb.append("\n  --> ").append(context);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return format();
    }

    // ==================== 构建器 ====================

    /**
     * 构建器类
     */
    public static class Builder {
        private ErrorCode code = ErrorCode.UNKNOWN_ERROR;
        private String message = "";
        private int line = -1;
        private int column = -1;
        private ErrorSeverity severity = ErrorSeverity.ERROR;
        private String sourceFile;
        private String context;
        private Throwable cause;

        /**
         * 设置错误码
         */
        public Builder code(ErrorCode code) {
            this.code = code;
            return this;
        }

        /**
         * 设置错误消息
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * 设置行号
         */
        public Builder line(int line) {
            this.line = line;
            return this;
        }

        /**
         * 设置列号
         */
        public Builder column(int column) {
            this.column = column;
            return this;
        }

        /**
         * 设置严重程度
         */
        public Builder severity(ErrorSeverity severity) {
            this.severity = severity;
            return this;
        }

        /**
         * 设置源文件
         */
        public Builder sourceFile(String sourceFile) {
            this.sourceFile = sourceFile;
            return this;
        }

        /**
         * 设置上下文
         */
        public Builder context(String context) {
            this.context = context;
            return this;
        }

        /**
         * 设置原因异常
         */
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        /**
         * 构建错误实例
         */
        public CompilerError build() {
            return new CompilerError(this);
        }
    }

    // ==================== 错误码枚举 ====================

    /**
     * 错误码枚举
     */
    public enum ErrorCode {
        // 未知错误
        UNKNOWN_ERROR("E000", "未知错误"),

        // 词法分析错误 (E1xx)
        LEXICAL_ERROR("E100", "词法错误"),
        UNEXPECTED_CHARACTER("E101", "意外的字符"),
        UNTERMINATED_STRING("E102", "未终止的字符串"),
        UNTERMINATED_COMMENT("E103", "未终止的注释"),
        INVALID_NUMBER("E104", "无效的数字格式"),

        // 语法分析错误 (E2xx)
        SYNTAX_ERROR("E200", "语法错误"),
        UNEXPECTED_TOKEN("E201", "意外的标记"),
        MISSING_TOKEN("E202", "缺少标记"),
        INVALID_SYNTAX("E203", "无效的语法"),
        MISSING_SEMICOLON("E204", "缺少分号"),
        MISSING_BRACE("E205", "缺少大括号"),
        MISSING_PARENTHESIS("E206", "缺少括号"),

        // 语义分析错误 (E3xx)
        SEMANTIC_ERROR("E300", "语义错误"),
        UNDEFINED_VARIABLE("E301", "未定义的变量"),
        UNDEFINED_FUNCTION("E302", "未定义的函数"),
        UNDEFINED_TYPE("E303", "未定义的类型"),
        DUPLICATE_DEFINITION("E304", "重复定义"),
        INVALID_OPERATION("E305", "无效的操作"),

        // 类型错误 (E4xx)
        TYPE_ERROR("E400", "类型错误"),
        TYPE_MISMATCH("E401", "类型不匹配"),
        INCOMPATIBLE_TYPES("E402", "不兼容的类型"),
        INVALID_TYPE_ARGUMENT("E403", "无效的类型参数"),
        MISSING_TYPE_ANNOTATION("E404", "缺少类型注解"),

        // 编译错误 (E5xx)
        COMPILATION_ERROR("E500", "编译错误"),
        INTERNAL_ERROR("E501", "内部错误"),
        NOT_IMPLEMENTED("E502", "功能未实现"),
        RESOURCE_ERROR("E503", "资源错误"),

        // 警告 (W0xx)
        WARNING("W000", "警告"),
        UNUSED_VARIABLE("W001", "未使用的变量"),
        UNUSED_PARAMETER("W002", "未使用的参数"),
        DEPRECATED_FEATURE("W003", "已废弃的特性"),
        UNREACHABLE_CODE("W004", "不可达代码");

        private final String code;
        private final String description;

        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        /**
         * 获取错误码
         */
        public String getCode() {
            return code;
        }

        /**
         * 获取错误描述
         */
        public String getDescription() {
            return description;
        }
    }

    // ==================== 严重程度枚举 ====================

    /**
     * 错误严重程度
     */
    public enum ErrorSeverity {
        /** 提示信息 */
        INFO("info"),
        /** 警告 */
        WARNING("warning"),
        /** 错误 */
        ERROR("error"),
        /** 致命错误 */
        FATAL("fatal");

        private final String display;

        ErrorSeverity(String display) {
            this.display = display;
        }

        /**
         * 获取显示名称
         */
        public String getDisplay() {
            return display;
        }

        /**
         * 是否阻止编译
         */
        public boolean blocksCompilation() {
            return this == ERROR || this == FATAL;
        }
    }

    // ==================== 错误集合 ====================

    /**
     * 错误集合
     *
     * <p>用于收集和管理多个编译错误。</p>
     */
    public static class Collection {
        private final List<CompilerError> errors = new ArrayList<>();
        private final List<CompilerError> warnings = new ArrayList<>();

        /**
         * 添加错误
         *
         * @param error 错误实例
         */
        public void add(CompilerError error) {
            if (error.getSeverity() == ErrorSeverity.WARNING ||
                error.getSeverity() == ErrorSeverity.INFO) {
                warnings.add(error);
            } else {
                errors.add(error);
            }
        }

        /**
         * 添加语法错误
         */
        public void addSyntaxError(String message, int line) {
            add(syntaxError(message, line));
        }

        /**
         * 添加语义错误
         */
        public void addSemanticError(String message, int line) {
            add(semanticError(message, line));
        }

        /**
         * 添加类型错误
         */
        public void addTypeError(String message, int line) {
            add(typeError(message, line));
        }

        /**
         * 添加警告
         */
        public void addWarning(String message, int line) {
            add(warning(message, line));
        }

        /**
         * 是否有错误
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        /**
         * 是否有警告
         */
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        /**
         * 获取所有错误
         */
        public List<CompilerError> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        /**
         * 获取所有警告
         */
        public List<CompilerError> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        /**
         * 获取错误数量
         */
        public int errorCount() {
            return errors.size();
        }

        /**
         * 获取警告数量
         */
        public int warningCount() {
            return warnings.size();
        }

        /**
         * 格式化输出所有错误和警告
         */
        public String formatAll() {
            StringBuilder sb = new StringBuilder();

            for (CompilerError error : errors) {
                sb.append(error.format()).append("\n");
            }
            for (CompilerError warning : warnings) {
                sb.append(warning.format()).append("\n");
            }

            // 添加统计
            sb.append(String.format("\n%d 个错误, %d 个警告",
                errors.size(), warnings.size()));

            return sb.toString();
        }

        /**
         * 清空所有错误和警告
         */
        public void clear() {
            errors.clear();
            warnings.clear();
        }
    }
}

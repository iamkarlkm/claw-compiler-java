// ==================== CompilationException.java ====================
package com.claw.compiler.pipeline;

import com.claw.compiler.pipeline.CompilerError.ErrorCode;
import com.claw.compiler.pipeline.CompilerError.ErrorSeverity;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 编译异常
 *
 * <p>编译过程中发生的可恢复错误的异常封装。
 * 包含一个或多个 {@link CompilerError} 实例。</p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 单个错误
 * throw new CompilationException("语法错误", 42, 15);
 *
 * // 多个错误
 * CompilerError.Collection errors = new CompilerError.Collection();
 * errors.addSyntaxError("缺少分号", 10);
 * errors.addTypeError("类型不匹配", 20);
 * throw new CompilationException(errors);
 * }</pre>
 *
 * @author Claw Compiler Team
 * @since 3.0.0
 * @see CompilerError
 */
@Getter
public class CompilationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 编译错误列表 */
    private final List<CompilerError> errors;

    /** 错误发生的文件名 */
    private final String sourceFile;

    /** 错误发生的行号（仅当单个错误时有效） */
    private final int line;

    /** 错误发生的列号（仅当单个错误时有效） */
    private final int column;

    // ==================== 构造函数 ====================

    /**
     * 创建简单的编译异常
     *
     * @param message 错误消息
     */
    public CompilationException(String message) {
        super(message);
        this.errors = List.of(CompilerError.builder()
            .message(message)
            .severity(ErrorSeverity.ERROR)
            .build());
        this.sourceFile = null;
        this.line = -1;
        this.column = -1;
    }

    /**
     * 创建带位置的编译异常
     *
     * @param message 错误消息
     * @param line 行号
     * @param column 列号
     */
    public CompilationException(String message, int line, int column) {
        super(formatLocationMessage(message, line, column));
        this.errors = List.of(CompilerError.builder()
            .message(message)
            .line(line)
            .column(column)
            .severity(ErrorSeverity.ERROR)
            .build());
        this.sourceFile = null;
        this.line = line;
        this.column = column;
    }

    /**
     * 创建带源文件和位置的编译异常
     *
     * @param message 错误消息
     * @param sourceFile 源文件名
     * @param line 行号
     * @param column 列号
     */
    public CompilationException(String message, String sourceFile, int line, int column) {
        super(formatFullLocationMessage(message, sourceFile, line, column));
        this.errors = List.of(CompilerError.builder()
            .message(message)
            .sourceFile(sourceFile)
            .line(line)
            .column(column)
            .severity(ErrorSeverity.ERROR)
            .build());
        this.sourceFile = sourceFile;
        this.line = line;
        this.column = column;
    }

    /**
     * 创建带多个错误的编译异常
     *
     * @param errors 错误集合
     */
    public CompilationException(CompilerError.Collection errors) {
        super(formatErrorsMessage(errors));
        this.errors = new ArrayList<>(errors.getErrors());
        this.errors.addAll(errors.getWarnings());
        this.sourceFile = null;
        this.line = -1;
        this.column = -1;
    }

    /**
     * 创建带错误列表的编译异常
     *
     * @param message 总体消息
     * @param errors 错误列表
     */
    public CompilationException(String message, List<String> errors) {
        super(message);
        List<CompilerError> compilerErrors = new ArrayList<>();
        for (String error : errors) {
            compilerErrors.add(CompilerError.builder()
                .message(error)
                .severity(ErrorSeverity.ERROR)
                .build());
        }
        this.errors = Collections.unmodifiableList(compilerErrors);
        this.sourceFile = null;
        this.line = -1;
        this.column = -1;
    }

    /**
     * 创建带原因的编译异常
     *
     * @param message 错误消息
     * @param cause 原因异常
     */
    public CompilationException(String message, Throwable cause) {
        super(message, cause);
        this.errors = List.of(CompilerError.builder()
            .message(message)
            .severity(ErrorSeverity.ERROR)
            .cause(cause)
            .build());
        this.sourceFile = null;
        this.line = -1;
        this.column = -1;
    }

    /**
     * 创建带位置和原因的编译异常
     *
     * @param message 错误消息
     * @param line 行号
     * @param cause 原因异常
     */
    public CompilationException(String message, int line, Throwable cause) {
        super(formatLocationMessage(message, line, -1), cause);
        this.errors = List.of(CompilerError.builder()
            .message(message)
            .line(line)
            .severity(ErrorSeverity.ERROR)
            .cause(cause)
            .build());
        this.sourceFile = null;
        this.line = line;
        this.column = -1;
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建词法错误异常
     *
     * @param message 错误消息
     * @param line 行号
     * @return 异常实例
     */
    public static CompilationException lexicalError(String message, int line) {
        return new CompilationException(
            CompilerError.builder()
                .code(ErrorCode.LEXICAL_ERROR)
                .message(message)
                .line(line)
                .severity(ErrorSeverity.ERROR)
                .build()
        );
    }

    /**
     * 创建语法错误异常
     *
     * @param message 错误消息
     * @param line 行号
     * @return 异常实例
     */
    public static CompilationException syntaxError(String message, int line) {
        return new CompilationException(
            CompilerError.builder()
                .code(ErrorCode.SYNTAX_ERROR)
                .message(message)
                .line(line)
                .severity(ErrorSeverity.ERROR)
                .build()
        );
    }

    /**
     * 创建语义错误异常
     *
     * @param message 错误消息
     * @param line 行号
     * @return 异常实例
     */
    public static CompilationException semanticError(String message, int line) {
        return new CompilationException(
            CompilerError.builder()
                .code(ErrorCode.SEMANTIC_ERROR)
                .message(message)
                .line(line)
                .severity(ErrorSeverity.ERROR)
                .build()
        );
    }

    /**
     * 创建类型错误异常
     *
     * @param message 错误消息
     * @param line 行号
     * @return 异常实例
     */
    public static CompilationException typeError(String message, int line) {
        return new CompilationException(
            CompilerError.builder()
                .code(ErrorCode.TYPE_ERROR)
                .message(message)
                .line(line)
                .severity(ErrorSeverity.ERROR)
                .build()
        );
    }

    /**
     * 创建内部错误异常
     *
     * @param message 错误消息
     * @param cause 原因异常
     * @return 异常实例
     */
    public static CompilationException internalError(String message, Throwable cause) {
        return new CompilationException(
            CompilerError.builder()
                .code(ErrorCode.INTERNAL_ERROR)
                .message(message)
                .severity(ErrorSeverity.FATAL)
                .cause(cause)
                .build()
        );
    }

    // ==================== 私有构造函数 ====================

    /**
     * 从单个 CompilerError 创建异常
     */
    private CompilationException(CompilerError error) {
        super(error.format());
        this.errors = List.of(error);
        this.sourceFile = error.getSourceFile();
        this.line = error.getLine();
        this.column = error.getColumn();
    }

    // ==================== 格式化方法 ====================

    private static String formatLocationMessage(String message, int line, int column) {
        if (line < 0) return message;
        if (column < 0) {
            return String.format("[%d] %s", line, message);
        }
        return String.format("[%d:%d] %s", line, column, message);
    }

    private static String formatFullLocationMessage(String message, String sourceFile, int line, int column) {
        String location = sourceFile != null ? sourceFile : "";
        if (line >= 0) {
            location += ":" + line;
            if (column >= 0) {
                location += ":" + column;
            }
        }
        if (!location.isEmpty()) {
            return String.format("%s %s", location, message);
        }
        return message;
    }

    private static String formatErrorsMessage(CompilerError.Collection collection) {
        int errorCount = collection.errorCount();
        int warningCount = collection.warningCount();
        return String.format("编译失败: %d 个错误, %d 个警告", errorCount, warningCount);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取错误数量
     *
     * @return 错误数量
     */
    public int getErrorCount() {
        return (int) errors.stream()
            .filter(e -> e.getSeverity().blocksCompilation())
            .count();
    }

    /**
     * 获取警告数量
     *
     * @return 警告数量
     */
    public int getWarningCount() {
        return (int) errors.stream()
            .filter(e -> e.getSeverity() == ErrorSeverity.WARNING)
            .count();
    }

    /**
     * 是否有错误
     *
     * @return 如果有错误返回 true
     */
    public boolean hasErrors() {
        return errors.stream().anyMatch(e -> e.getSeverity().blocksCompilation());
    }

    /**
     * 是否有警告
     *
     * @return 如果有警告返回 true
     */
    public boolean hasWarnings() {
        return errors.stream().anyMatch(e -> e.getSeverity() == ErrorSeverity.WARNING);
    }

    /**
     * 格式化输出所有错误
     *
     * @return 格式化后的错误信息
     */
    public String formatAllErrors() {
        StringBuilder sb = new StringBuilder();
        for (CompilerError error : errors) {
            sb.append(error.format()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        if (errors.size() == 1) {
            return errors.get(0).format();
        }
        return String.format("CompilationException: %d errors, %d warnings",
            getErrorCount(), getWarningCount());
    }
}

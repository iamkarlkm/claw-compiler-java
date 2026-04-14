// ==================== CompilationResult.java ====================
package com.claw.compiler.pipeline;

import com.claw.ir.ClawIR;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一的编译结果
 *
 * 合并了两个版本的 CompilationResult：
 * - com.claw.pipeline.CompilationResult 的信息（moduleName, phases, files）
 * - com.claw.compiler.pipeline.CompilationResult 的详细功能（GeneratedCode, errors, warnings）
 */
@Slf4j
@Getter
@Builder
public class CompilationResult {

    static CompilationResult failure(String message, List<String> errors) {
        List<String> allErrors = new ArrayList<>();
        if (message != null) {
            allErrors.add(message);
        }
        if (errors != null && !errors.isEmpty()) {
            allErrors.addAll(errors);
        }
        return CompilationResult.builder()
                .success(false)
                .errors(allErrors)
                .elapsedMillis(0)
                .build();
    }

    static CompilationResult success(GeneratedCode generatedCode, long elapsed) {
        return CompilationResult.builder()
                .success(true)
                .generatedCode(generatedCode)
                .elapsedMillis(elapsed)
                .build();
    }

    static CompilationResult failure(String message, long elapsed) {
        List<String> errors = new ArrayList<>();
        if (message != null) {
            errors.add(message);
        }
        return CompilationResult.builder()
                .success(false)
                .errors(errors)
                .elapsedMillis(elapsed)
                .build();
    }

    /** 模块名称 */
    private  String moduleName;

    /** 编译是否成功 */
    private  boolean success;

    /** 生成的代码 */
    private  GeneratedCode generatedCode;

    /** 中间表示（ClawIR） */
    private  com.claw.ir.ClawIR ir;

    /** 生成的文件 */
    @Builder.Default
    private  Map<String, String> generatedFiles = new ConcurrentHashMap<>();

    /** 错误列表 */
    @Builder.Default
    private  List<String> errors = new ArrayList<>();

    /** 警告列表 */
    @Builder.Default
    private  List<String> warnings = new ArrayList<>();

    /** 已完成的阶段 */
    @Builder.Default
    private  Set<String> completedPhases = new LinkedHashSet<>();

    /** 编译耗时（毫秒） */
    private  long elapsedMillis;

    private CompilationResult(String moduleName, boolean success, GeneratedCode generatedCode,
                              com.claw.ir.ClawIR ir, Map<String, String> generatedFiles,
                              List<String> errors, List<String> warnings,
                              Set<String> completedPhases, long elapsedMillis) {
        this.moduleName = moduleName;
        this.success = success;
        this.generatedCode = generatedCode;
        this.ir = ir;
        this.generatedFiles = generatedFiles != null ? generatedFiles : new ConcurrentHashMap<>();
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        this.completedPhases = completedPhases != null ? new LinkedHashSet<>(completedPhases) : new LinkedHashSet<>();
        this.elapsedMillis = elapsedMillis;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建成功的编译结果
     *
     * @param moduleName 模块名称
     * @param code 生成的代码
     * @param elapsed 编译耗时
     * @return CompilationResult
     */
    public static CompilationResult success(String moduleName, GeneratedCode code, long elapsed) {
        return CompilationResult.builder()
                .moduleName(moduleName)
                .success(true)
                .generatedCode(code)
                .elapsedMillis(elapsed)
                .build();
    }

    /**
     * 创建成功的编译结果（使用 Builder）
     *
     * @param moduleName 模块名称
     * @param code 生成的代码
     * @param elapsed 编译耗时
     * @return CompilationResult
     */
    public static CompilationResult successBuilder(String moduleName, GeneratedCode code, long elapsed) {
        return CompilationResult.builder()
                .moduleName(moduleName)
                .success(true)
                .generatedCode(code)
                .elapsedMillis(elapsed)
                .build();
    }

    /**
     * 创建失败的编译结果
     *
     * @param moduleName 模块名称
     * @param error 错误信息
     * @param elapsed 编译耗时
     * @return CompilationResult
     */
    public static CompilationResult failure(String moduleName, String error, long elapsed) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return CompilationResult.builder()
                .moduleName(moduleName)
                .success(false)
                .errors(errors)
                .elapsedMillis(elapsed)
                .build();
    }

    /**
     * 创建失败的编译结果（包含多个错误）
     *
     * @param moduleName 模块名称
     * @param message 主消息
     * @param errors 错误列表
     * @param elapsed 编译耗时
     * @return CompilationResult
     */
    public static CompilationResult failure(String moduleName, String message, List<String> errors,
                                           long elapsed) {
        List<String> allErrors = new ArrayList<>();
        allErrors.add(message);
        if (errors != null && !errors.isEmpty()) {
            allErrors.addAll(errors);
        }
        return CompilationResult.builder()
                .moduleName(moduleName)
                .success(false)
                .errors(allErrors)
                .elapsedMillis(elapsed)
                .build();
    }

    /**
     * 创建失败的编译结果（使用 Builder）
     *
     * @param moduleName 模块名称
     * @param message 主消息
     * @param errors 错误列表
     * @param elapsed 编译耗时
     * @return CompilationResult
     */
    public static CompilationResult failureBuilder(String moduleName, String message, List<String> errors,
                                                   long elapsed) {
        List<String> allErrors = new ArrayList<>();
        allErrors.add(message);
        if (errors != null) {
            allErrors.addAll(errors);
        }
        return CompilationResult.builder()
                .moduleName(moduleName)
                .success(false)
                .errors(allErrors)
                .elapsedMillis(elapsed)
                .build();
    }

    /**
     * 合并两个 CompilationResult
     *
     * @param result1 第一个结果
     * @param result2 第二个结果
     * @return 合并后的结果
     */
    public static CompilationResult merge(CompilationResult result1, CompilationResult result2) {
        log.debug("合并编译结果: {} -> {}", result1, result2);

        List<String> mergedErrors = new ArrayList<>();
        if (result1.getErrors() != null) {
            mergedErrors.addAll(result1.getErrors());
        }
        if (result2.getErrors() != null) {
            mergedErrors.addAll(result2.getErrors());
        }

        List<String> mergedWarnings = new ArrayList<>();
        if (result1.getWarnings() != null) {
            mergedWarnings.addAll(result1.getWarnings());
        }
        if (result2.getWarnings() != null) {
            mergedWarnings.addAll(result2.getWarnings());
        }

        Set<String> mergedPhases = new LinkedHashSet<>();
        if (result1.getCompletedPhases() != null) {
            mergedPhases.addAll(result1.getCompletedPhases());
        }
        if (result2.getCompletedPhases() != null) {
            mergedPhases.addAll(result2.getCompletedPhases());
        }

        Map<String, String> mergedFiles = new ConcurrentHashMap<>();
        if (result1.getGeneratedFiles() != null) {
            mergedFiles.putAll(result1.getGeneratedFiles());
        }
        if (result2.getGeneratedFiles() != null) {
            mergedFiles.putAll(result2.getGeneratedFiles());
        }

        boolean success = result1.isSuccess() && result2.isSuccess();

        return CompilationResult.builder()
                .moduleName(result1.getModuleName() != null ? result1.getModuleName() : result2.getModuleName())
                .success(success)
//                .generatedCode(success ? result1.getGeneratedCode() : null)
            .generatedCode(result1.getGeneratedCode())
//                .ir(success ? result1.getIR() : null)
             .ir(result1.getIR() )
                .generatedFiles(mergedFiles)
                .errors(mergedErrors)
                .warnings(mergedWarnings)
                .completedPhases(mergedPhases)
                .elapsedMillis(Math.max(result1.getElapsedMillis(), result2.getElapsedMillis()))
                .build();
    }

    // ==================== 方法 ====================

    /**
     * 添加阶段完成标记
     *
     * @param name 阶段名称
     */
    public void setPhaseComplete(String name) {
        completedPhases.add(name);
    }

    /**
     * 添加错误
     *
     * @param error 错误信息
     */
    public void addError(String error) {
        errors.add(error);
    }

    /**
     * 添加多个错误
     *
     * @param errs 错误列表
     */
    public void addErrors(List<String> errs) {
        if (errs != null) {
            errors.addAll(errs);
            success = false;
        }
    }

    /**
     * 添加警告
     *
     * @param warning 警告信息
     */
    public void addWarning(String warning) {
        warnings.add(warning);
    }

    /**
     * 添加多个警告
     *
     * @param warns 警告列表
     */
    public void addWarnings(List<String> warns) {
        if (warns != null) {
            warnings.addAll(warns);
        }
    }

    /**
     * 设置生成的文件
     *
     * @param files 文件名 -> 内容的映射
     */
    public void setGeneratedFiles(Map<String, String> files) {
        if (files != null) {
            this.generatedFiles.putAll(files);
        }
    }

    /**
     * 设置中间表示
     *
     * @param ir 中间表示
     */
    public void setIR(com.claw.ir.ClawIR ir) {
        this.ir = ir;
    }

    /**
     * 获取中间表示
     *
     * @return 中间表示，如果未设置返回 null
     */
    public com.claw.ir.ClawIR getIR() {
        return ir;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("CompilationResult{SUCCESS, elapsed=%dms}", elapsedMillis);
        } else {
            return String.format("CompilationResult{FAILED, errors=%d, elapsed=%dms}",
                    errors.size(), elapsedMillis);
        }
    }

    /**
     * 判断编译是否成功
     *
     * @return 如果编译成功返回 true
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 获取生成的代码
     *
     * @return 生成的代码对象，如果编译失败返回 null
     */
    public GeneratedCode getGeneratedCode() {
        return generatedCode;
    }

    /**
     * 获取错误消息列表
     *
     * @return 错误消息列表，不可修改
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * 获取警告消息列表
     *
     * @return 警告消息列表，不可修改
     */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * 获取编译耗时（毫秒）
     *
     * @return 编译耗时
     */
    public long getElapsedMillis() {
        return elapsedMillis;
    }

    /**
     * 是否有错误
     *
     * @return 如果有错误返回 true
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 是否有警告
     *
     * @return 如果有警告返回 true
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * 获取错误数量
     *
     * @return 错误数量
     */
    public int errorCount() {
        return errors.size();
    }

    /**
     * 获取警告数量
     *
     * @return 警告数量
     */
    public int warningCount() {
        return warnings.size();
    }

    /**
     * 格式化输出错误信息
     *
     * @return 格式化后的错误信息
     */
    public String formatErrors() {
        if (errors.isEmpty()) {
            return "No errors";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            sb.append(String.format("[%d] %s%n", i + 1, errors.get(i)));
        }
        return sb.toString();
    }

    /**
     * 格式化输出摘要
     *
     * @return 摘要信息
     */
    public String formatSummary() {
        return String.format(
            "Compilation %s: %d errors, %d warnings, %dms",
            success ? "succeeded" : "failed",
            errors.size(),
            warnings.size(),
            elapsedMillis
        );
    }

}

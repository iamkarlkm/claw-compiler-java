package com.q3lives.compiler.pipeline;

import com.q3lives.ir.ClawIR;
import lombok.Getter;
import lombok.Setter;
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
public class CompilationResult {

    /** 模块名称 */
    private  String moduleName;

    /** 编译是否成功 */
    private  boolean success;

    /** 生成的代码 */
    private  GeneratedCode generatedCode;

    /** 中间表示（ClawIR） */
    private  ClawIR ir;

    /** 生成的文件 */
    private  Map<String, String> generatedFiles = new ConcurrentHashMap<>();

    /** 错误列表 */
    private  List<String> errors = new ArrayList<>();

    /** 警告列表 */
    private  List<String> warnings = new ArrayList<>();

    /** 已完成的阶段 */
    private  Set<String> completedPhases = new LinkedHashSet<>();

    /** 编译耗时（毫秒） */
    private  long elapsedMillis;

    CompilationResult(String moduleName, boolean success, GeneratedCode generatedCode,
                       ClawIR ir, Map<String, String> generatedFiles,
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

    // ==================== Setters ====================

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setGeneratedCode(GeneratedCode generatedCode) {
        this.generatedCode = generatedCode;
    }

    public void setIR(ClawIR ir) {
        this.ir = ir;
    }

    public void setGeneratedFiles(Map<String, String> files) {
        if (files != null) {
            this.generatedFiles.putAll(files);
        }
    }

    public void setErrors(List<String> errors) {
        if (errors != null) {
            this.errors.clear();
            this.errors.addAll(errors);
        }
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public void addErrors(List<String> errs) {
        if (errs != null) {
            this.errors.addAll(errs);
            this.success = false;
        }
    }

    public void setWarnings(List<String> warnings) {
        if (warnings != null) {
            this.warnings.clear();
            this.warnings.addAll(warnings);
        }
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public void addWarnings(List<String> warns) {
        if (warns != null) {
            this.warnings.addAll(warns);
        }
    }

    public void setCompletedPhases(Set<String> completedPhases) {
        if (completedPhases != null) {
            this.completedPhases.clear();
            this.completedPhases.addAll(completedPhases);
        }
    }

    public void setPhaseComplete(String name) {
        this.completedPhases.add(name);
    }

    public void setElapsedMillis(long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }

    public ClawIR getIR() {
        return ir;
    }

    public ClawIR getGeneratedIR() {
        return ir;
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
        return new CompilationResult(moduleName, true, code, null, null, null, null, null, elapsed);
    }

    static CompilationResult success(GeneratedCode generatedCode, long elapsed) {
        return new CompilationResult(null, true, generatedCode, null, null, null, null, null, elapsed);
    }

    /**
     * 创建失败的编译结果
     *
     * @param message 错误信息
     * @param errors 错误列表
     * @param elapsed 编译耗时
     * @return CompilationResult
     */
    public static CompilationResult failure(String message, List<String> errors, long elapsed) {
        List<String> allErrors = new ArrayList<>();
        allErrors.add(message);
        if (errors != null && !errors.isEmpty()) {
            allErrors.addAll(errors);
        }
        return new CompilationResult(null, false, null, null, null, allErrors, null, null, elapsed);
    }

    static CompilationResult failure(String message, long elapsed) {
        List<String> errors = new ArrayList<>();
        errors.add(message);
        return new CompilationResult(null, false, null, null, null, errors, null, null, elapsed);
    }

    /**
     * 创建失败的编译结果（指定模块名）
     *
     * @param fileName 文件名
     * @param message 错误信息
     * @param elapsed 编译耗时
     * @return CompilationResult
     */
    public static CompilationResult failure(String fileName, String message, long elapsed) {
        List<String> errors = new ArrayList<>();
        errors.add(message);
        return new CompilationResult(fileName, false, null, null, null, errors, null, null, elapsed);
    }

    /**
     * 创建失败的编译结果（指定模块名和错误列表）
     *
     * @param fileName 文件名
     * @param message 主消息
     * @param errors 错误列表
     * @param elapsed 编译耗时
     * @return CompilationResult
     */
    public static CompilationResult failure(String fileName, String message, List<String> errors,
                                           long elapsed) {
        List<String> allErrors = new ArrayList<>();
        allErrors.add(message);
        if (errors != null && !errors.isEmpty()) {
            allErrors.addAll(errors);
        }
        return new CompilationResult(fileName, false, null, null, null, allErrors,
                                     null, null, elapsed);
    }

    /**
     * 创建 mock 编译结果（用于测试）
     *
     * @param moduleName 模块名称
     * @return CompilationResult
     */
    public static CompilationResult mock(String moduleName) {
        return new CompilationResult(moduleName, true, null, null, null,
                                     Collections.emptyList(), Collections.emptyList(),
                                     Collections.emptySet(), 0);
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

        return new CompilationResult(
            result1.getModuleName() != null ? result1.getModuleName() : result2.getModuleName(),
            success,
            result1.getGeneratedCode() != null ? result1.getGeneratedCode() : result2.getGeneratedCode(),
            result1.getIR() != null ? result1.getIR() : result2.getIR(),
            mergedFiles,
            mergedErrors,
            mergedWarnings,
            mergedPhases,
            Math.max(result1.getElapsedMillis(), result2.getElapsedMillis())
        );
    }

}

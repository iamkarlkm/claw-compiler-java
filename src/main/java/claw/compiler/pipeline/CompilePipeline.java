package claw.compiler.pipeline;

import claw.compiler.binding.TargetCodeGenerator;
import claw.compiler.binding.c.CCodeGenerator;
// import claw.compiler.binding.java.JavaCodeGenerator;
import claw.compiler.binding.python.PythonCodeGenerator;
import claw.compiler.generators.ffi.CFFIGenerator;
import claw.compiler.generators.ffi.FFIBindingTable;
import claw.compiler.generators.ffi.JavaFFIGenerator;
import claw.compiler.generators.ffi.PythonFFIGenerator;
import claw.compiler.processors.blocks.ExternBlockProcessor;
import claw.compiler.processors.semantic.ExternProcessor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.claw.binding.java.JavaCodeGenerator;

public class CompilePipeline {

    // ================ 输出路径策略 ================

    /**
     * 输出目录配置
     * 
     * 优先级:
     *   1. 用户显式指定 outputDir
     *   2. 与源文件同目录下的 build/ 子目录
     *   3. 当前工作目录下的 build/
     */
    private String outputDir;
    private String sourceFileName;  // 不含扩展名

    /**
     * 构造管道，自动推导输出路径
     *
     * @param sourceFilePath  源文件路径，如 "src/example.claw"
     * @param outputDir       输出目录，null 则自动推导
     * @param targetLanguage  目标语言 "java" / "python" / "c"
     */
    public CompilePipeline(String sourceFilePath, String outputDir, String targetLanguage) {
        Path sourcePath = Paths.get(sourceFilePath);

        // 提取不含扩展名的文件名
        String fileName = sourcePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        this.sourceFileName = (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;

        // 确定输出目录
        if (outputDir != null && !outputDir.isEmpty()) {
            this.outputDir = outputDir;
        } else {
            // 默认: 源文件所在目录/build/
            Path parentDir = sourcePath.getParent();
            if (parentDir == null) {
                parentDir = Paths.get(".");
            }
            this.outputDir = parentDir.resolve("build").toString();
        }
    }

    /**
     * 计算输出文件的基础路径（不含扩展名）
     *
     * 例:
     *   sourceFile = "src/my_module.claw"
     *   outputDir  = null (自动推导)
     *   
     *   -> basePath = "src/build/my_module"
     *   -> C 目标:   "src/build/my_module.c"  +  "src/build/my_module.h"
     *   -> Java:     "src/build/MyModule.java"
     *   -> Python:   "src/build/my_module.py"
     */
    private String getOutputBasePath() {
        return Paths.get(outputDir, sourceFileName).toString();
    }

    /**
     * 获取主输出文件完整路径（含扩展名）
     */
    private String getOutputFilePath(TargetCodeGenerator generator) {
        String basePath = getOutputBasePath();

        // Java 特殊处理：文件名需要首字母大写
        if ("Java".equals(generator.getLanguageName())) {
            String javaName = toPascalCase(sourceFileName);
            basePath = Paths.get(outputDir, javaName).toString();
        }

        return basePath + generator.getFileExtension();
    }

    /**
     * 获取头文件路径（仅 C 目标需要）
     */
    private String getHeaderFilePath() {
        return getOutputBasePath() + ".h";
    }

    /**
     * 获取运行时头文件路径（C 目标的 claw_runtime.h）
     */
    private String getRuntimeHeaderPath() {
        return Paths.get(outputDir, "claw_runtime.h").toString();
    }

    // ================ 主编译流程 ================

    public CompileResult compile(
        String sourceFilePath,
        String targetLanguage,
        Object structureCtx,      // StructureContext
        Object semanticCtx,       // SemanticContext
        Object annotationResult   // AnnotationResult
    ) {
        // 1. 创建代码生成器
        TargetCodeGenerator codeGenerator = createCodeGenerator(targetLanguage);

        // 2. 生成中间表示 (IR)
        // ClawIR ir = buildIR(structureCtx, semanticCtx, annotationResult);

        // 3. 生成目标代码
        // String generatedCode = codeGenerator.generate(ir);

        // 4. 确保输出目录存在
        ensureDirectoryExists(outputDir);

        // 5. 写出文件
        String mainOutputPath = getOutputFilePath(codeGenerator);
        // writeFile(mainOutputPath, generatedCode);

        CompileResult result = new CompileResult();
        result.mainOutputPath = mainOutputPath;

        // 6. C 目标的特殊处理
        if (codeGenerator instanceof CCodeGenerator) {
            CCodeGenerator cGen = (CCodeGenerator) codeGenerator;

            // 6a. 写入生成的 .h 头文件
            String headerPath = getHeaderFilePath();
            String headerCode = cGen.getHeaderOutput();
            writeFile(headerPath, headerCode);
            result.headerOutputPath = headerPath;

            // 6b. 复制/生成 claw_runtime.h 到输出目录
            String runtimePath = getRuntimeHeaderPath();
            if (!Files.exists(Paths.get(runtimePath))) {
                String runtimeCode = cGen.getRuntime().generateRuntimeHelpers();
                // 实际上应该输出完整的 claw_runtime.h
                // 这里调用专门方法生成
                writeRuntimeHeader(runtimePath);
            }
            result.runtimeHeaderPath = runtimePath;

            // 6c. 生成编译脚本
            String buildScriptPath = Paths.get(outputDir, "build.sh").toString();
            writeBuildScript(buildScriptPath, mainOutputPath, result);
            result.buildScriptPath = buildScriptPath;
        }
//         else // Python 目标的特殊处理：同时输出 .py 和 claw_runtime.py
// if (codeGenerator instanceof PythonCodeGenerator) {
//     PythonCodeGenerator pyGen = (PythonCodeGenerator) codeGenerator;
    
//     // 复制/生成 claw_runtime.py 到输出目录
//     String runtimePath = Paths.get(outputDir, "claw_runtime.py").toString();
//     if (!Files.exists(Paths.get(runtimePath))) {
//         writeRuntimeModule(runtimePath);  // 写入上面的完整 claw_runtime.py
//     }
//     result.runtimeModulePath = runtimePath;
// }

        return result;
    }

    public CompileResult compile(List<String> sourceLines, String fileName, String targetLang) {

        // ====== 第1层：基础处理 ======
        // preprocessor, tokenizer ...

        // ====== 第2层：语义处理（包括 Extern） ======
        FFIBindingTable ffiTable = new FFIBindingTable();
        ExternProcessor externProcessor = new ExternProcessor(ffiTable);
        
        boolean externOK = externProcessor.process(sourceLines, fileName);
        if (!externOK) {
            externProcessor.reportDiagnostics();
            return CompileResult.failure(externProcessor.getErrors());
        }
        // 打印警告
        externProcessor.reportDiagnostics();

        // 其他第2层处理器（type, function, control_flow 等）并行工作
        // 这些处理器通过 externProcessor.isExternSymbol() 识别 extern 符号

        // ====== 第3层：块处理（包括 ExternBlock） ======
        ExternBlockProcessor externBlockProcessor = new ExternBlockProcessor(ffiTable);
        externBlockProcessor.registerFromBindingTable(sourceLines);

        // 使用分析：传入所有 Claw 函数的行范围
        Map<String, int[]> functionBounds = new LinkedHashMap<>();
        // ... 从 FunctionBlockProcessor 获取函数边界 ...
        externBlockProcessor.analyzeSymbolUsages(sourceLines, functionBounds);

        // 作用域验证
        List<String> scopeViolations = externBlockProcessor.validateScopeRules();
        if (!scopeViolations.isEmpty()) {
            for (String v : scopeViolations) {
                System.err.println("[Scope Error] " + v);
            }
            return CompileResult.failure("Extern scope violations");
        }

        // 其他第3层处理器跳过 extern 块区域
        List<int[]> externRanges = externBlockProcessor.getExternBlockRanges();
        // 传给 FunctionBlockProcessor, ControlFlowBlockProcessor 等

        // ====== 第4层：代码生成 ======
        String ffiCode;
        switch (targetLang) {
            case "c":
                CFFIGenerator cGen = new CFFIGenerator(ffiTable);
                ffiCode = cGen.generateAll();
                break;
            case "python":
                PythonFFIGenerator pyGen = new PythonFFIGenerator(ffiTable);
                ffiCode = pyGen.generateAll();
                break;
            case "java":
                JavaFFIGenerator javaGen = new JavaFFIGenerator(ffiTable);
                ffiCode = javaGen.generateAll();
                break;
            default:
                ffiCode = "";
        }

        // ffiCode 插入到生成文件的头部
        // ...

        return CompileResult.success(ffiCode);
    }

    // ================ 编译结果 ================

    /**
     * 编译结果
     *
     * <p>包含编译输出的所有文件路径和状态信息。</p>
     */
    public static class CompileResult {

        private final boolean success;
        private final List<String> errors;
        private final List<String> warnings;
        private final String generatedCode;

        /** 主输出文件 (.c / .java / .py) */
        public String mainOutputPath;
        /** 头文件 (.h)，仅 C 目标 */
        public String headerOutputPath;
        /** claw_runtime.h，仅 C 目标 */
        public String runtimeHeaderPath;
        /** 编译脚本，仅 C 目标 */
        public String buildScriptPath;
        /** Python 运行时模块路径 */
        public String runtimeModulePath;

        private CompileResult(boolean success, String generatedCode,
                              List<String> errors, List<String> warnings) {
            this.success = success;
            this.generatedCode = generatedCode;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }

        /**
         * 创建成功的编译结果
         *
         * @param generatedCode 生成的代码
         * @return 编译结果实例
         */
        public static CompileResult success(String generatedCode) {
            return new CompileResult(true, generatedCode, null, null);
        }

        /**
         * 创建失败的编译结果（字符串错误）
         *
         * @param errorMessage 错误消息
         * @return 编译结果实例
         */
        public static CompileResult failure(String errorMessage) {
            List<String> errors = new ArrayList<>();
            errors.add(errorMessage);
            return new CompileResult(false, null, errors, null);
        }

        /**
         * 创建失败的编译结果（错误列表）
         *
         * @param errors 错误列表
         * @return 编译结果实例
         */
        public static CompileResult failure(List<ExternProcessor.ProcessingError> errors) {
            List<String> errorMessages = new ArrayList<>();
            if (errors != null) {
                for (ExternProcessor.ProcessingError error : errors) {
                    errorMessages.add(error.toString());
                }
            }
            return new CompileResult(false, null, errorMessages, null);
        }

        // Private constructor - not used
        private CompileResult() {
            // This constructor is never used - CompileResult is created via factory methods
        }

        /**
         * 判断编译是否成功
         *
         * @return 如果成功返回 true
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * 获取生成的代码
         *
         * @return 生成的代码字符串
         */
        public String getGeneratedCode() {
            return generatedCode;
        }

        /**
         * 获取错误列表
         *
         * @return 错误列表
         */
        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        /**
         * 获取警告列表
         *
         * @return 警告列表
         */
        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CompileResult{");
            sb.append(success ? "SUCCESS" : "FAILED");
            if (hasErrors()) {
                sb.append(", errors=").append(errors.size());
            }
            if (hasWarnings()) {
                sb.append(", warnings=").append(warnings.size());
            }
            sb.append("}\n");

            if (mainOutputPath != null) {
                sb.append("  Main output:    ").append(mainOutputPath).append("\n");
            }
            if (headerOutputPath != null) {
                sb.append("  Header output:  ").append(headerOutputPath).append("\n");
            }
            if (runtimeHeaderPath != null) {
                sb.append("  Runtime header: ").append(runtimeHeaderPath).append("\n");
            }
            if (buildScriptPath != null) {
                sb.append("  Build script:   ").append(buildScriptPath).append("\n");
            }
            if (runtimeModulePath != null) {
                sb.append("  Runtime module: ").append(runtimeModulePath).append("\n");
            }

            return sb.toString();
        }

        /**
         * 格式化输出所有错误
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
    }

    // ================ 辅助方法 ================

    private static TargetCodeGenerator createCodeGenerator(String targetLanguage) {
        switch (targetLanguage.toLowerCase()) {
            case "java":
                return new JavaCodeGenerator();
            case "python":
            case "py":
                return new PythonCodeGenerator();
            case "c":
                return new CCodeGenerator();
            default:
                throw new IllegalArgumentException(
                    "Unsupported target language: " + targetLanguage +
                    ". Supported: java, python, c"
                );
        }
    }

    private void ensureDirectoryExists(String dirPath) {
        try {
            Files.createDirectories(Paths.get(dirPath));
        } catch (IOException e) {
            throw new RuntimeException("Cannot create output directory: " + dirPath, e);
        }
    }

    private void writeFile(String filePath, String content) {
        try {
            Files.write(
                Paths.get(filePath),
                content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            throw new RuntimeException("Cannot write file: " + filePath, e);
        }
    }

    /**
     * 将 claw_runtime.h 的完整内容写入输出目录
     */
    private void writeRuntimeHeader(String runtimePath) {
        // 方式1：从编译器内部资源文件复制
        try (InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream("claw_runtime.h")) {

            if (resourceStream != null) {
                byte[] bytes = resourceStream.readAllBytes();
                Files.write(Paths.get(runtimePath), bytes,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                // 方式2：如果资源文件不存在，动态生成
                String runtimeContent = generateFullRuntimeHeader();
                writeFile(runtimePath, runtimeContent);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot write runtime header: " + runtimePath, e);
        }
    }

    /**
     * 生成 C 编译脚本
     */
    private void writeBuildScript(String scriptPath, String mainSource, CompileResult result) {
        String baseName = sourceFileName;

        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash");
        script.append("# Auto-generated build script for Claw -> C");
        script.append("# Generated at: ").append(java.time.LocalDateTime.now()).append("\n");

        script.append("CC=${CC:-gcc}");
        script.append("CFLAGS=\"-std=c11 -Wall -Wextra\"");
        script.append("LDFLAGS=\"-lm\"");

        script.append("echo \"Compiling ").append(baseName).append("...\"");
        script.append("$CC $CFLAGS -o ").append(baseName)
              .append(" ").append(Paths.get(mainSource).getFileName())
              .append(" $LDFLAGS");

        script.append("if [ $? -eq 0 ]; then");
        script.append("    echo \"Build successful: ./").append(baseName).append("\"");
        script.append("else");
        script.append("    echo \"Build failed\"");
        script.append("    exit 1");
        script.append("fi");

        writeFile(scriptPath, script.toString());

        // 设置可执行权限
        try {
            Paths.get(scriptPath).toFile().setExecutable(true);
        } catch (Exception ignored) {
        }
    }

    /**
     * 动态生成完整的 claw_runtime.h 内容
     * （即之前给出的那个完整头文件）
     */
    private String generateFullRuntimeHeader() {
        // 返回完整的 claw_runtime.h 内容
        // 这里省略具体内容，实际就是之前给出的那个完整头文件
        return "/* claw_runtime.h - see previous definition */";
    }

    private static String toPascalCase(String name) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == '_' || c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

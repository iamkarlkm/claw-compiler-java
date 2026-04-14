package com.claw.compiler;

import com.claw.binding.java.JavaCodeGenerator;
import com.claw.compiler.annotation.AnnotationResult;
import com.claw.compiler.context.SemanticContext;
import com.claw.compiler.context.StructureContext;

import claw.compiler.binding.TargetCodeGenerator;
import claw.compiler.binding.c.CCodeGenerator;
import claw.compiler.binding.python.PythonCodeGenerator;
import claw.compiler.generators.ClawIR;
import claw.compiler.generators.IRGenerator;

// import claw.compiler.binding.TargetCodeGenerator;
// import claw.compiler.binding.java.JavaCodeGenerator;
// // 将来：import claw.compiler.binding.python.PythonCodeGenerator;
// import claw.compiler.context.StructureContext;
// import claw.compiler.context.SemanticContext;
// import claw.compiler.annotation.AnnotationResult;
// import claw.compiler.generators.IRGenerator;
// import claw.compiler.generators.ClawIR;

/**
 * 编译管道集成示例
 * 
 * 展示 IRGenerator -> ClawIR -> TargetCodeGenerator 的完整流程
 */
public class CompilePipeline {

    public CompilePipeline(String string, Object object, String string2) {
        //TODO Auto-generated constructor stub
    }



    /**
     * 编译入口
     * 
     * @param sourceFileName  源文件名
     * @param moduleName      模块名
     * @param structureCtx    第3层输出（结构上下文）
     * @param semanticCtx     第2层输出（语义上下文）
     * @param annotationResult 注解处理结果
     * @param targetLanguage   目标语言："java" / "python"
     * @return 生成的目标语言代码
     */
    public static String compile(String sourceFileName,
                                  String moduleName,
                                  StructureContext structureCtx,
                                  SemanticContext semanticCtx,
                                  AnnotationResult annotationResult,
                                  String targetLanguage) {

        // 阶段1：生成语言无关的IR
        IRGenerator irGenerator = new IRGenerator(sourceFileName);
        ClawIR ir = irGenerator.generate(moduleName, structureCtx, semanticCtx, annotationResult);

        // 阶段2：检查IR有效性
        if (!ir.isValid()) {
            System.err.println("Compilation errors found:");
            for (SemanticContext.SemanticError error : semanticCtx.getErrors()) {
                System.err.println("  " + error);
            }
            return null;
        }

        // 阶段3：选择目标语言代码生成器（解耦切换点）
        TargetCodeGenerator codeGenerator = createCodeGenerator(targetLanguage);

        // 阶段4：生成目标代码
        String targetCode = codeGenerator.generate(ir);

        System.out.println("Compiled " + sourceFileName + " -> " +
                           codeGenerator.getLanguageName() +
                           " (" + ir.getIrProgram().getAllInstructions().size() + " IR instructions)");
        // C 目标的特殊处理：同时输出 .c 和 .h
if (codeGenerator instanceof CCodeGenerator) {
    CCodeGenerator cGen = (CCodeGenerator) codeGenerator;
    String headerCode = cGen.getHeaderOutput();
    // 写入 .h 文件
    writeFile("outputPath" + ".h", headerCode);
}

// 基本用法：自动推导输出路径
CompilePipeline pipeline = new CompilePipeline(
    "src/user_service.claw",   // 源文件
    null,                       // 输出目录（自动推导为 src/build/）
    "c"                         // 目标语言
);

// CompilePipeline.CompileResult result = pipeline.compile(
//     "src/user_service.claw", "test",
//     structureCtx, semanticCtx, annotationResult,"c"
// );

// System.out.println(result);
// 输出:
// Compile Result:
//   Main output:    src/build/user_service.c
//   Header output:  src/build/user_service.h
//   Runtime header: src/build/claw_runtime.h
//   Build script:   src/build/build.sh


// 指定输出目录
CompilePipeline pipeline2 = new CompilePipeline(
    "src/user_service.claw",
    "/tmp/claw_output",         // 显式指定
    "c"
);
// -> /tmp/claw_output/user_service.c
// -> /tmp/claw_output/user_service.h
// -> /tmp/claw_output/claw_runtime.h


        return targetCode;
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



    /**
     * 工厂方法：根据目标语言创建对应的代码生成器
     * 
     * 解耦设计：新增目标语言只需：
     * 1. 实现 TargetRuntime 接口
     * 2. 实现 TargetCodeGenerator 接口
     * 3. 在此工厂方法中注册
     */
    private static TargetCodeGenerator createCodeGenerator(String targetLanguage) {
        switch (targetLanguage.toLowerCase()) {
            case "java":
                return new JavaCodeGenerator();
            case "python":
                return new PythonCodeGenerator();
            case "c":
                return new CCodeGenerator();
            default:
                throw new IllegalArgumentException(
                    "Unsupported target language: " + targetLanguage +
                    ". Supported: java" /* + ", python, c" */);
        }
    }

    public static void main(String[] args) {
        // 编译为 Python
// String pythonCode = CompilePipeline.compile(
//     "example.claw", "example",
//     structureCtx, semanticCtx, annotationResult,
//     "python"   // 切换目标语言
// );

// 编译为 C
// String cCode = CompilePipeline.compile(
//     "example.claw", "example",
//     structureCtx, semanticCtx, annotationResult,
//     "c"
// );


    }

    public class CompileResult {
    }
}

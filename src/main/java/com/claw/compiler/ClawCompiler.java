// ==================== ClawCompiler.java ====================
package com.claw.compiler;

import com.claw.compiler.pipeline.CompilationPipeline;
import com.claw.compiler.pipeline.CompilationResult;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Claw 语言编译器主入口
 *
 * 版本：3.0（最终版）
 * 
 * 核心设计思想：
 *   思想1：三层操作流模型 (normal / exception / flow)
 *   思想2：扫描→配对→分层→分解→生成
 *   思想3：4层细粒度处理器架构
 *   思想4：18种代码块类型
 *   思想5：精确的注解系统 (4个程序注解 + 5个系统注解)
 */
@Slf4j
public class ClawCompiler {

    private final CompilationPipeline pipeline;

    public ClawCompiler() {
        this.pipeline = new CompilationPipeline();
    }

    /**
     * 编译源代码字符串
     */
    public CompilationResult compile(String source, String fileName) {
        return pipeline.compile(source, fileName);
    }

    /**
     * 编译源文件
     */
    public CompilationResult compileFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();

        try {
            String source = Files.readString(path);
            return compile(source, fileName);
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            throw e;
        }
    }

    /**
     * 编译并输出到文件
     */
    public void compileToFile(String sourcePath, String outputPath) throws IOException {
        CompilationResult result = compileFile(sourcePath);

        if (result.isSuccess()) {
            try {
                Path outPath = Paths.get(outputPath);
                Files.writeString(outPath, result.getGeneratedCode().getTargetCode(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                log.info("输出文件: {}", outputPath);

                // 同时输出IR文件
                String irPath = outputPath.replace(".java", ".ir").replace(".js", ".ir");
                Files.writeString(Paths.get(irPath),
                        result.getGeneratedCode().getIntermediateRepresentation(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                log.info("IR文件: {}", irPath);

                // 伪代码文件
                String pseudoPath = outputPath.replace(".java", ".pseudo").replace(".js", ".pseudo");
                Files.writeString(Paths.get(pseudoPath),
                        result.getGeneratedCode().getPseudoCode(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                log.info("伪代码文件: {}", pseudoPath);
            } catch (IOException e) {
                log.error("写入文件失败: {}", outputPath, e);
                throw e;
            }
        } else {
            log.error("编译失败:");
            for (String error : result.getErrors()) {
                log.error("  - {}", error);
            }
        }
    }

    /**
     * 命令行入口
     */
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║    Claw Language Compiler v3.0       ║");
        System.out.println("║    三层操作流 | 18种代码块 | 4层处理器  ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();

        if (args.length == 0) {
            // 使用内嵌示例代码运行演示
            runDemo();
            return;
        }

        ClawCompiler compiler = new ClawCompiler();

        //String sourceFile = args[0];
        //String outputFile = args.length > 1 ? args[1] : sourceFile.replace(".claw", ".java");

        try {
            //compiler.compileToFile(sourceFile, outputFile);
            compiler.compileToFile("test.claw", "test.c");
        } catch (IOException ex) {
            log.error("IO错误: {}", ex.getMessage());
            System.exit(1);
        }
    }

    /**
     * 运行演示
     */
    private static void runDemo() {
        System.out.println("运行内嵌示例...");

        String demoSource = """
                // 系统注解：用于自动化编程
                @@description("处理用户数据并监听属性变更", "UserData -> ProcessResult")
                @@param("userData", "用户数据对象")
                @@return("处理结果")
                @@example("processUser({name: 'Alice', age: 25})")
                
                // 导入模块
                import utils { formatName, validatePhone }
                import models { UserData, ProcessResult }
                
                // 类型定义
                type UserProfile {
                    var name: String
                    var age: Int
                    var email: String
                    var phone: String
                    var active: Bool
                }
                
                // 程序注解：构造/析构
                @BeforeName("initializeUser", "this")
                @AfterName("cleanupUser", "this")
                
                // 程序注解：属性变更监听
                @BeforeProps("user.age,user.name")
                @AfterProps("user.email,user.phone")
                
                // 三层操作流函数
                normal function processUser(userData: UserData) -> ProcessResult {
                    var result = ProcessResult()
                    
                    var age = calculateAge(userData.birthday)
                    var name = formatName(userData.firstName, userData.lastName)
                    
                    if (age > 18) {
                        result.category = "adult"
                    } else {
                        result.category = "minor"
                    }
                    
                    const MAX_RETRIES = 3
                    var retryCount = 0
                    
                    while (retryCount < MAX_RETRIES) {
                        var valid = validatePhone(userData.phone)
                        if (valid) {
                            break
                        }
                        retryCount = retryCount + 1
                    }
                    
                    result.success = true
                    result.message = "处理完成"
                    
                    catch (ValidationError e) {
                        result.success = false
                        result.message = "验证失败: " + e.message
                    }
                    
                    flow to cleanup
                    
                    return result
                }
                
                // 辅助函数
                function calculateAge(birthday: String) -> Int {
                    return 25
                }
                
                // 公共入口
                public function main() -> Void {
                    var userData = UserData()
                    userData.firstName = "Alice"
                    userData.lastName = "Smith"
                    userData.birthday = "2001-01-15"
                    userData.phone = "13800138000"
                    
                    var result = processUser(userData)
                    println(result.message)
                }
                """;

        ClawCompiler compiler = new ClawCompiler();
        CompilationResult result = compiler.compile(demoSource, "demo.claw");

        System.out.println("编译结果: " + result);
        System.out.println();

        if (result.isSuccess()) {
            System.out.println("========== 生成的目标代码 ==========");
            System.out.println(result.getGeneratedCode().getTargetCode());

            System.out.println("========== 中间表示 (IR) ==========");
            System.out.println(result.getGeneratedCode().getIntermediateRepresentation());

            System.out.println("========== 伪代码 ==========");
            System.out.println(result.getGeneratedCode().getPseudoCode());

            System.out.println("========== 元数据 ==========");
            result.getGeneratedCode().getMetadata().forEach((k, v) ->
                    System.out.println("  " + k + " = " + v));
        } else {
            System.out.println("编译错误:");
            result.getErrors().forEach(e -> System.out.println("  ERROR: " + e));
        }
    }
}


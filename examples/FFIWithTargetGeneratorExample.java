package examples;

import com.q3lives.binding.GenerationConfig;
import com.q3lives.binding.GenerationResult;
import com.q3lives.binding.c.CompleteCCodeGenerator;
import com.q3lives.binding.java.EnhancedJavaCodeGenerator;
import com.q3lives.compiler.generators.ffi.FFIBindingTable;
import com.q3lives.compiler.processors.semantic.ExternProcessor;
import com.q3lives.ir.ClawIR;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * FFI 与目标代码生成器集成示例
 *
 * <p>演示完整的端到端流程：</p>
 * <ol>
 *   <li>从 Claw 源代码中提取 extern "C" 块</li>
 *   <li>使用 ExternProcessor 解析为 FFIBindingTable</li>
 *   <li>将 FFIBindingTable 附加到 ClawIR</li>
 *   <li>调用目标代码生成器生成含 FFI 的完整代码</li>
 * </ol>
 */
public class FFIWithTargetGeneratorExample {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("FFI + Target Code Generator Integration");
        System.out.println("========================================\n");

        // 1. 准备包含 extern "C" 的 Claw 源代码
        String clawSource = createClawSourceWithExtern();

        // 2. 解析 extern "C" 块
        FFIBindingTable bindingTable = parseExternBlocks(clawSource);
        if (bindingTable == null || !bindingTable.hasExternDeclarations()) {
            System.out.println("No FFI bindings found.");
            return;
        }
        System.out.println("FFI bindings parsed: " + bindingTable.getExternBlocks().size() + " block(s)");
        System.out.println("  Functions: " + bindingTable.getAllFunctions().size());
        System.out.println("  Constants: " + bindingTable.getAllConstants().size());
        System.out.println("  Structs:   " + bindingTable.getAllStructs().size());
        System.out.println("  Enums:     " + bindingTable.getAllEnums().size());
        System.out.println();

        // 3. 创建模拟的 ClawIR 并附加 FFIBindingTable
        ClawIR ir = new ClawIR();
        ir.setFfiBindingTable(bindingTable);

        // 4. 生成目标语言的代码
        generateJava(ir);
        generateC(ir);

        System.out.println("\n========================================");
        System.out.println("All target codes generated successfully.");
        System.out.println("========================================");
    }

    /**
     * 创建包含 extern "C" 的示例 Claw 源代码
     */
    private static String createClawSourceWithExtern() {
        return """
            // Claw 模块定义
            module DatabaseExample

            // 外部 C 函数接口
            extern "C" {
                link "sqlite3"
                include "sqlite3.h"

                const SQLITE_OK: Int = 0
                const SQLITE_ROW: Int = 100

                function sqlite3_open(filename: String, ppDb: Pointer) -> Int
                function sqlite3_close(pDb: Pointer) -> Int
                function sqlite3_exec(pDb: Pointer, sql: String, callback: Pointer, pArg: Pointer, pErrMsg: Pointer) -> Int
            }

            // 用户类型定义
            type DatabaseHandle {
                var dbPtr: Pointer
                var path: String
            }

            // 主函数
            normal function openDatabase(path: String) -> DatabaseHandle {
                var handle = DatabaseHandle()
                handle.path = path
                // FFI call would be: sqlite3_open(path, &handle.dbPtr)
                return handle
            }

            normal function main() -> Void {
                var db = openDatabase("test.db")
                println("Database opened: " + db.path)
            }
            """;
    }

    /**
     * 解析源代码中的 extern "C" 块
     */
    private static FFIBindingTable parseExternBlocks(String source) {
        FFIBindingTable table = new FFIBindingTable();
        ExternProcessor processor = new ExternProcessor(table);
        List<String> lines = Arrays.asList(source.split("\n"));
        boolean success = processor.process(lines, "example.claw");
        if (!success) {
            System.out.println("Extern processing had errors.");
        }
        return table;
    }

    /**
     * 生成 Java 目标代码（含 FFI）
     */
    private static void generateJava(ClawIR ir) {
        System.out.println("\n---------- Java Target ----------");
        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        GenerationConfig config = new GenerationConfig();
        config.setGenerateComments(true);

        GenerationResult result = generator.generate(ir, config);

        System.out.println("Generated files:");
        for (Map.Entry<String, String> entry : result.getFiles().entrySet()) {
            String fileName = entry.getKey();
            String content = entry.getValue();
            System.out.println("  " + fileName + " (" + content.length() + " chars)");
            if (fileName.contains("FFI")) {
                // 打印 FFI 文件的前 30 行
                printPreview(content, 30);
            }
        }

        if (!result.getErrors().isEmpty()) {
            System.out.println("Errors:");
            result.getErrors().forEach(e -> System.out.println("  " + e));
        }
    }

    /**
     * 生成 C 目标代码（含 FFI）
     */
    private static void generateC(ClawIR ir) {
        System.out.println("\n---------- C Target ----------");
        CompleteCCodeGenerator generator = new CompleteCCodeGenerator();
        GenerationConfig config = new GenerationConfig();

        GenerationResult result = generator.generate(ir, config);

        System.out.println("Generated files:");
        for (Map.Entry<String, String> entry : result.getFiles().entrySet()) {
            String fileName = entry.getKey();
            String content = entry.getValue();
            System.out.println("  " + fileName + " (" + content.length() + " chars)");
            if (fileName.contains("ffi")) {
                printPreview(content, 30);
            }
        }

        if (!result.getErrors().isEmpty()) {
            System.out.println("Errors:");
            result.getErrors().forEach(e -> System.out.println("  " + e));
        }
    }

    /**
     * 打印代码预览（前 N 行）
     */
    private static void printPreview(String content, int maxLines) {
        String[] lines = content.split("\n");
        int limit = Math.min(lines.length, maxLines);
        for (int i = 0; i < limit; i++) {
            System.out.println("    " + lines[i]);
        }
        if (lines.length > maxLines) {
            System.out.println("    ... (" + (lines.length - maxLines) + " more lines)");
        }
        System.out.println();
    }
}

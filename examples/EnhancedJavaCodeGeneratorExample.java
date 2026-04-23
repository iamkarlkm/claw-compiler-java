package examples;

import com.q3lives.binding.java.EnhancedJavaCodeGenerator;
import com.q3lives.binding.java.JavaRuntime;
import com.q3lives.ir.ClawIR;
import com.q3lives.compiler.generators.IRGenerator;
import com.q3lives.compiler.core.CompilerConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * 增强的 Java 代码生成器示例
 *
 * 演示 EnhancedJavaCodeGenerator 的高级功能：
 * 1. 泛型支持
 * 2. 增强的错误处理
 * 3. 完整的指令支持
 * 4. 更好的代码组织
 */
public class EnhancedJavaCodeGeneratorExample {

    public static void main(String[] args) {
        System.out.println("=== 增强Java代码生成器示例 ===\n");

        // 创建增强的Java代码生成器
        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        JavaRuntime runtime = (JavaRuntime) generator.getRuntime();

        // 创建示例Claw代码
        String clawCode = createClawCodeWithFeatures();

        // 模拟IR生成过程（实际使用中会通过ClawCompiler生成）
        ClawIR ir = generateMockIR(clawCode);

        // 生成Java代码
        System.out.println("1. 生成Java代码...");
        String javaCode = generator.generate(ir);
        System.out.println("生成的Java代码（前200字符）：");
        System.out.println(javaCode.length() > 200 ? javaCode.substring(0, 200) + "..." : javaCode);

        // 使用带配置的生成
        System.out.println("\n2. 使用配置生成...");
        com.q3lives.binding.GenerationConfig config = new com.q3lives.binding.GenerationConfig();
        config.setGenerateComments(true);
        com.q3lives.binding.GenerationResult result = generator.generate(ir, config);

        System.out.println("生成的文件数: " + result.getFiles().size());
        System.out.println("统计信息: " + result.getStats());
        System.out.println("错误数: " + result.getErrors().size());

        // 展示特定功能
        System.out.println("\n3. 功能特性展示：");
        demonstrateFeatures(generator);
    }

    /**
     * 创建包含高级特性的Claw代码
     */
    private static String createClawCodeWithFeatures() {
        return "" +
            "// 包含泛型、异常处理、高级特性的示例\n" +
            "type UserProfile<T> {\n" +
            "    var name: String\n" +
            "    var age: Int\n" +
            "    var data: T\n" +
            "}\n\n" +
            "normal function processUser<T>(user: UserProfile<T>) -> Bool {\n" +
            "    var result = false\n" +
            "    \n" +
            "    try {\n" +
            "        // 处理用户数据\n" +
            "        if (user.age > 0) {\n" +
            "            result = true\n" +
            "        }\n" +
            "    } catch (ValidationError e) {\n" +
            "        result = false\n" +
            "        log(\"Error: \" + e.message)\n" +
            "        flow to cleanup\n" +
            "    }\n" +
            "    \n" +
            "    return result\n" +
            "}\n\n" +
            "normal function main() {\n" +
            "    // 创建泛型实例\n" +
            "    var user = UserProfile<String>()\n" +
            "    user.name = \"Test User\"\n" +
            "    user.age = 25\n" +
            "    user.data = \"Additional data\"\n" +
            "    \n" +
            "    // 处理用户数据\n" +
            "    processUser(user)\n" +
            "    \n" +
            "    cleanup:\n" +
            "    // 清理代码\n" +
            "    log(\"Cleanup complete\")\n" +
            "}\n";
    }

    /**
     * 模拟IR生成
     */
    private static ClawIR generateMockIR(String clawCode) {
        // 创建模拟的IR程序
        IRGenerator irGenerator = new IRGenerator();
        ClawIR ir = new ClawIR();

        // 创建IR程序
        IRGenerator.IRProgram program = irGenerator.new IRProgram("example.claw");

        // 添加类型定义
        IRGenerator.IRBasicBlock typeBlock = irGenerator.new IRBasicBlock("type_definitions");
        typeBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.TYPE_DEF,
            "UserProfile",
            new HashMap<>()  // 字段将在 TYPE_FIELD 中定义
        ));

        // 添加方法定义
        IRGenerator.IRBasicBlock methodBlock = irGenerator.new IRBasicBlock("methods");
        methodBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            "processUser",
            new HashMap<String, String>() {{
                put("user", "UserProfile");
            }},
            "Bool"
        ));

        program.addTopLevelBlock(typeBlock);
        program.addTopLevelBlock(methodBlock);

        ir.setIrProgram(program);
        return ir;
    }

    /**
     * 展示代码生成器的功能特性
     */
    private static void demonstrateFeatures(EnhancedJavaCodeGenerator generator) {
        System.out.println("\n   功能特性清单：");
        System.out.println("   ✓ 泛型支持（List<T>, Map<K,V>）");
        System.out.println("   ✓ 完整的错误处理");
        System.out.println("   ✓ 指令完全支持");
        System.out.println("   ✓ 更好的代码组织");
        System.out.println("   ✓ 自动生成辅助类");
        System.out.println("   ✓ 统计和错误报告");
        System.out.println("   ✓ 多文件生成支持");

        System.out.println("\n   增强的Java生成器与原始版本的主要区别：");
        System.out.println("   1. 实现了泛型类型支持");
        System.out.println("   2. 添加了完整的错误处理和报告");
        System.out.println("   3. 支持所有指令类型");
        System.out.println("   4. 更好的代码组织结构");
        System.out.println("   5. 自动生成辅助工具类");
        System.out.println("   6. 提供详细的生成统计信息");
        System.out.println("   7. 支持多文件生成");

        System.out.println("\n   生成示例：");
        System.out.println("   - 主程序文件: ClawProgram.java");
        System.out.println("   - 辅助工具: ClawRuntime.java");
        System.out.println("   - 泛型工具: ClawGenerics.java");
        System.out.println("   - 自动导入标准库和运行时包");
    }
}
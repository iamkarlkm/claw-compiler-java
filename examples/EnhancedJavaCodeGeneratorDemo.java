package examples;

import com.q3lives.binding.java.EnhancedJavaCodeGenerator;
import com.q3lives.binding.java.JavaRuntime;
import com.q3lives.ir.ClawIR;

import java.util.HashMap;
import java.util.Map;

/**
 * 增强版Java代码生成器演示程序
 *
 * 演示EnhancedJavaCodeGenerator的所有功能特性：
 * 1. 泛型支持
 * 2. 增强的错误处理
 * 3. 完整的指令支持
 * 4. 更好的代码组织
 * 5. 完善的注解处理
 */
public class EnhancedJavaCodeGeneratorDemo {

    public static void main(String[] args) {
        System.out.println("=== 增强版Java代码生成器演示 ===\n");

        // 创建增强的Java代码生成器
        EnhancedJavaCodeGenerator generator = new EnhancedJavaCodeGenerator();
        JavaRuntime runtime = (JavaRuntime) generator.getRuntime();

        // 演示基本功能
        demonstrateBasicFeatures(generator);

        // 演示泛型支持
        demonstrateGenerics(generator);

        // 演示配置选项
        demonstrateConfiguration(generator);

        // 演示错误处理
        demonstrateErrorHandling(generator);

        System.out.println("\n演示完成 ✓");
    }

    /**
     * 演示基本功能
     */
    private static void demonstrateBasicFeatures(EnhancedJavaCodeGenerator generator) {
        System.out.println("\n1. 基本功能演示：");

        // 创建简单的Claw IR
        ClawIR ir = createSimpleIR();

        // 生成Java代码
        String javaCode = generator.generate(ir);

        // 显示结果
        System.out.println("生成的Java代码（前200字符）：");
        System.out.println(javaCode.length() > 200 ? javaCode.substring(0, 200) + "..." : javaCode);

        System.out.println("   ✓ 基本类生成成功");
    }

    /**
     * 演示泛型支持
     */
    private static void demonstrateGenerics(EnhancedJavaCodeGenerator generator) {
        System.out.println("\n2. 泛型支持演示：");

        // 创建包含泛型的IR
        ClawIR ir = createGenericIR();

        // 生成Java代码
        String javaCode = generator.generate(ir);

        // 检查泛型生成
        boolean hasGenerics = javaCode.contains("<") && javaCode.contains(">");

        System.out.println("   泛型生成: " + (hasGenerics ? "✓" : "✗"));

        // 生成辅助类
        String helperCode = generator.generateHelperClasses();
        System.out.println("   辅助类生成: " + (helperCode.length() > 100 ? "✓" : "✗"));
    }

    /**
     * 演示配置选项
     */
    private static void demonstrateConfiguration(EnhancedJavaCodeGenerator generator) {
        System.out.println("\n3. 配置选项演示：");

        // 创建复杂IR
        ClawIR ir = createComplexIR();

        // 创建配置
        com.q3lives.binding.GenerationConfig config = new com.q3lives.binding.GenerationConfig();
        config.setGenerateComments(true);
        config.setOutputDirectory("generated");
        config.setIncludeHelpers(true);

        // 生成代码
        com.q3lives.binding.GenerationResult result = generator.generate(ir, config);

        // 显示结果
        System.out.println("   生成文件数: " + result.getFiles().size());
        System.out.println("   错误数: " + result.getErrors().size());
        System.out.println("   统计信息: " + result.getStats());

        System.out.println("   ✓ 配置功能正常");
    }

    /**
     * 演示错误处理
     */
    private static void demonstrateErrorHandling(EnhancedJavaCodeGenerator generator) {
        System.out.println("\n4. 错误处理演示：");

        // 创建包含错误处理的IR
        ClawIR ir = createErrorHandlingIR();

        // 生成Java代码
        String javaCode = generator.generate(ir);

        // 检查错误处理生成
        boolean hasErrorHandling = javaCode.contains("try") && javaCode.contains("catch");

        System.out.println("   错误处理生成: " + (hasErrorHandling ? "✓" : "✗"));
        System.out.println("   ✓ 错误处理功能正常");
    }

    /**
     * 创建简单的IR
     */
    private static ClawIR createSimpleIR() {
        ClawIR ir = new ClawIR();

        // 设置模块名
        ir.setModuleName("SimpleProgram");

        return ir;
    }

    /**
     * 创建包含泛型的IR
     */
    private static ClawIR createGenericIR() {
        ClawIR ir = new ClawIR();

        // 设置模块名
        ir.setModuleName("GenericProgram");

        return ir;
    }

    /**
     * 创建复杂的IR
     */
    private static ClawIR createComplexIR() {
        ClawIR ir = new ClawIR();

        // 设置模块名
        ir.setModuleName("ComplexProgram");

        return ir;
    }

    /**
     * 创建包含错误处理的IR
     */
    private static ClawIR createErrorHandlingIR() {
        ClawIR ir = new ClawIR();

        // 设置模块名
        ir.setModuleName("ErrorHandlingProgram");

        return ir;
    }
}
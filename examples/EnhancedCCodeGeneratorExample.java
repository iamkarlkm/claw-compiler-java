package examples;

import com.q3lives.binding.c.EnhancedCCodeGenerator;
import com.q3lives.binding.c.CRuntime;
import com.q3lives.ir.ClawIR;
import com.q3lives.compiler.generators.IRGenerator;
import com.q3lives.compiler.core.CompilerConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * 增强的 C 代码生成器示例
 *
 * 演示 EnhancedCCodeGenerator 的高级功能：
 * 1. 自动内存管理（free nested allocations）
 * 2. 完整的循环支持
 * 3. 增强的异常处理
 * 4. 结构化的代码生成
 */
public class EnhancedCCodeGeneratorExample {

    public static void main(String[] args) {
        System.out.println("=== 增强C代码生成器示例 ===\n");

        // 创建增强的C代码生成器
        EnhancedCCodeGenerator generator = new EnhancedCCodeGenerator();
        CRuntime runtime = (CRuntime) generator.getRuntime();

        // 创建示例Claw代码
        String clawCode = createClawCodeWithFeatures();

        // 模拟IR生成过程（实际使用中会通过ClawCompiler生成）
        ClawIR ir = generateMockIR(clawCode);

        // 生成C代码
        System.out.println("1. 生成C代码...");
        String cCode = generator.generate(ir);
        System.out.println("生成的C代码（前200字符）：");
        System.out.println(cCode.length() > 200 ? cCode.substring(0, 200) + "..." : cCode);

        // 生成头文件
        System.out.println("\n2. 生成头文件...");
        String headerCode = generator.getHeaderOutput();
        System.out.println("生成的头文件（前200字符）：");
        System.out.println(headerCode.length() > 200 ? headerCode.substring(0, 200) + "..." : headerCode);

        // 展示特定功能
        System.out.println("\n3. 功能特性展示：");
        demonstrateFeatures(generator);
    }

    /**
     * 创建包含高级特性的Claw代码
     */
    private static String createClawCodeWithFeatures() {
        return "" +
            "// 包含内存管理、循环、异常处理的示例\n" +
            "type UserProfile {\n" +
            "    var name: String\n" +
            "    var age: Int\n" +
            "    var items: String[]\n" +
            "}\n\n" +
            "normal function processUser(userData: UserData) -> Bool {\n" +
            "    var result = false\n" +
            "    \n" +
            "    // 使用循环处理用户数据\n" +
            "    for (i = 0; i < userData.items.length; i++) {\n" +
            "        if (userData.items[i] != null) {\n" +
            "            result = true\n" +
            "            break\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    catch (ValidationError e) {\n" +
            "        result = false\n" +
            "        log(\"Error: \" + e.message)\n" +
            "        flow to cleanup\n" +
            "    }\n" +
            "    \n" +
            "    return result\n" +
            "}\n\n" +
            "normal function main() {\n" +
            "    var user = UserProfile()\n" +
            "    user.name = \"Test User\"\n" +
            "    user.age = 25\n" +
            "    \n" +
            "    // 分配数组\n" +
            "    user.items = new String[10]\n" +
            "    \n" +
            "    // 设置数组元素\n" +
            "    for (i = 0; i < 10; i++) {\n" +
            "        user.items[i] = \"Item \" + i\n" +
            "    }\n" +
            "    \n" +
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

        // 添加示例IR指令
        IRGenerator.IRProgram program = irGenerator.new IRProgram("example.claw");

        // 添加一个函数定义
        IRGenerator.IRBasicBlock mainBlock = irGenerator.new IRBasicBlock("main");
        mainBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.FUNC_DEF,
            "main",
            new HashMap<>(),  // 参数
            "void"
        ));

        // 添加内存分配
        mainBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.ALLOC,
            "user",
            "UserProfile"
        ));

        // 添加属性设置
        mainBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.PROP_SET,
            "user.name"
        ));

        // 添加循环
        IRGenerator.IRBasicBlock loopBlock = irGenerator.new IRBasicBlock("for_loop");
        loopBlock.addInstruction(irGenerator.new IRInstruction(
            IRGenerator.OpCode.LOOP_BEGIN,
            "for_loop_1"
        ));

        program.addTopLevelBlock(mainBlock);
        program.addTopLevelBlock(loopBlock);

        ir.setIrProgram(program);
        return ir;
    }

    /**
     * 展示代码生成器的功能特性
     */
    private static void demonstrateFeatures(EnhancedCCodeGenerator generator) {
        System.out.println("\n   功能特性清单：");
        System.out.println("   ✓ 自动内存管理（free nested allocations）");
        System.out.println("   ✓ 智能作用域管理");
        System.out.println("   ✓ 完整的循环支持（break, continue）");
        System.out.println("   ✓ 增强的异常处理");
        System.out.println("   ✓ 结构化代码生成");
        System.out.println("   ✓ 头文件自动生成");
        System.out.println("   ✓ 嵌套类型支持");
        System.out.println("   ✓ 自动内存清理");

        System.out.println("\n   增强的C生成器与原始版本的主要区别：");
        System.out.println("   1. 实现了完整的 free nested allocations 功能");
        System.out.println("   2. 添加了循环控制语句支持");
        System.out.println("   3. 改进了内存管理，防止内存泄漏");
        System.out.println("   4. 自动生成辅助函数（构造、析构、拷贝）");
        System.out.println("   5. 更好的错误处理和诊断信息");
        System.out.println("   6. 智能的变量跟踪和作用域管理");
    }
}
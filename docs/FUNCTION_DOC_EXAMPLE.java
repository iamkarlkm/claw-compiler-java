// 简单的函数文档生成示例

import claw.compiler.generators.IRGenerator;
import claw.compiler.generators.ClawIR;
import claw.compiler.binding.python.PythonCodeGenerator;

public class FunctionDocExample {
    public static void main(String[] args) {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        // 创建 IR 程序
        IRGenerator.IRProgram program = new IRGenerator.IRProgram("example.claw");

        // 创建块
        IRGenerator.IRBasicBlock block = new IRGenerator.IRBasicBlock("test", "func", 0);

        // 添加函数定义
        IRGenerator.IRInstruction funcDef = new IRGenerator.IRInstruction(
            IRGenerator.OpCode.FUNC_DEF, 1, "example.claw", "calculate_area"
        );

        // 添加描述
        IRGenerator.IRInstruction descMeta = new IRGenerator.IRInstruction(
            IRGenerator.OpCode.METADATA, 1, "example.claw", "description", "Calculate the area of a rectangle"
        );
        descMeta.setComment("@@description(\"Calculate the area of a rectangle\", \"\")");

        // 添加参数说明
        IRGenerator.IRInstruction widthMeta = new IRGenerator.IRInstruction(
            IRGenerator.OpCode.METADATA, 2, "example.claw", "param", "width", "Width of the rectangle"
        );
        widthMeta.setComment("@@param(\"width\", \"Width of the rectangle\")");

        IRGenerator.IRInstruction heightMeta = new IRGenerator.IRInstruction(
            IRGenerator.OpCode.METADATA, 3, "example.claw", "param", "height", "Height of the rectangle"
        );
        heightMeta.setComment("@@param(\"height\", \"Height of the rectangle\")");

        // 添加返回值说明
        IRGenerator.IRInstruction returnMeta = new IRGenerator.IRInstruction(
            IRGenerator.OpCode.METADATA, 4, "example.claw", "return", "The area of the rectangle"
        );
        returnMeta.setComment("@@return(\"The area of the rectangle\", \"\")");

        // 添加返回语句
        IRGenerator.IRInstruction returnStmt = new IRGenerator.IRInstruction(
            IRGenerator.OpCode.RETURN, 5, "example.claw", "__stack_top"
        );

        block.addInstruction(funcDef);
        block.addInstruction(descMeta);
        block.addInstruction(widthMeta);
        block.addInstruction(heightMeta);
        block.addInstruction(returnMeta);
        block.addInstruction(returnStmt);

        program.addTopLevelBlock(block);

        // 生成代码
        ClawIR clawIR = new ClawIR(program, null, null, null);
        String result = generator.generate(clawIR);

        System.out.println("=== Generated Python Code ===");
        System.out.println(result);
        System.out.println("=== End Generated Code ===");

        // 验证关键内容
        if (result.contains("def calculate_area():")) {
            System.out.println("✅ Function definition generated correctly");
        } else {
            System.out.println("❌ Function definition not found");
        }

        if (result.contains("\"\"\"") && result.contains("Calculate the area of a rectangle")) {
            System.out.println("✅ Docstring with description generated correctly");
        } else {
            System.out.println("❌ Docstring not found");
        }

        if (result.contains("Args:") && result.contains("width:") && result.contains("Height of the rectangle")) {
            System.out.println("✅ Args section with parameters generated correctly");
        } else {
            System.out.println("❌ Args section not found");
        }

        if (result.contains("Returns:") && result.contains("The area of the rectangle")) {
            System.out.println("✅ Returns section generated correctly");
        } else {
            System.out.println("❌ Returns section not found");
        }
    }
}

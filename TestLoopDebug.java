import claw.compiler.generators.IRGenerator;
import claw.compiler.generators.ClawIR;
import claw.compiler.binding.python.PythonCodeGenerator;

public class TestLoopDebug {
    public static void main(String[] args) {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "process_items");
        IRGenerator.IRInstruction forLoop = block.createInstruction(IRGenerator.OpCode.FOR_LOOP, "item", "items");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "1");
        IRGenerator.IRInstruction storeVar = block.createInstruction(IRGenerator.OpCode.STORE_VAR, "total");

        program.addTopLevelBlock(block);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        System.out.println("=== Generated Python Code ===");
        System.out.println(result);
        System.out.println("=== End Generated Code ===");

        // Check expected strings
        if (result.contains("def process_items():")) {
            System.out.println("✓ Found: def process_items():");
        } else {
            System.out.println("✗ NOT Found: def process_items():");
        }

        if (result.contains("for item in items:")) {
            System.out.println("✓ Found: for item in items:");
        } else {
            System.out.println("✗ NOT Found: for item in items:");
        }

        if (result.contains("pass  # loop body")) {
            System.out.println("✓ Found: pass  # loop body");
        } else {
            System.out.println("✗ NOT Found: pass  # loop body");
        }

        if (result.contains("total = 1")) {
            System.out.println("✓ Found: total = 1");
        } else {
            System.out.println("✗ NOT Found: total = 1");
        }
    }
}

package claw.compiler.binding.python;

import claw.compiler.generators.ClawIR;
import claw.compiler.generators.IRGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonCodeGenerator 属性监听系统测试
 */
public class PythonPropertyMonitoringTest {

    @Test
    public void testBeforePropsHook() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "update_age");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "30");
        IRGenerator.IRInstruction beforeHook = block.createInstruction(IRGenerator.OpCode.BEFORE_PROPS_HOOK, "age");

        program.addInstruction(funcDef);
        program.addInstruction(loadConst);
        program.addInstruction(beforeHook);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def update_age():"));
        assertTrue(result.contains("self._before_property_change(\"age\", __new_value)"));
    }

    @Test
    public void testAfterPropsHook() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "update_age");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "30");
        IRGenerator.IRInstruction afterHook = block.createInstruction(IRGenerator.OpCode.AFTER_PROPS_HOOK, "age");

        program.addInstruction(funcDef);
        program.addInstruction(loadConst);
        program.addInstruction(afterHook);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def update_age():"));
        assertTrue(result.contains("__old_val = self.age"));
        assertTrue(result.contains("self.age = 30"));
        assertTrue(result.contains("self._after_property_change(\"age\", __old_val, 30)"));
    }

    @Test
    public void testNestedPropertyMonitoring() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "update_city");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "Beijing");
        IRGenerator.IRInstruction beforeHook = block.createInstruction(IRGenerator.OpCode.BEFORE_PROPS_HOOK, "user.address.city");

        program.addInstruction(funcDef);
        program.addInstruction(loadConst);
        program.addInstruction(beforeHook);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def update_city():"));
        assertTrue(result.contains("self._before_property_change(\"user.address.city\", __new_value)"));
    }

    @Test
    public void testMultiplePropertiesHook() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "update_props");
        IRGenerator.IRInstruction loadConst1 = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "30");
        IRGenerator.IRInstruction beforeHook1 = block.createInstruction(IRGenerator.OpCode.BEFORE_PROPS_HOOK, "age");
        IRGenerator.IRInstruction loadConst2 = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "Beijing");
        IRGenerator.IRInstruction beforeHook2 = block.createInstruction(IRGenerator.OpCode.BEFORE_PROPS_HOOK, "city");

        program.addInstruction(funcDef);
        program.addInstruction(loadConst1);
        program.addInstruction(beforeHook1);
        program.addInstruction(loadConst2);
        program.addInstruction(beforeHook2);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def update_props():"));
        assertTrue(result.contains("self._before_property_change(\"age\", __new_value)"));
        assertTrue(result.contains("self._before_property_change(\"city\", __new_value)"));
    }

    @Test
    public void testPropertyAssignmentInHook() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction funcDef = block.createInstruction(IRGenerator.OpCode.FUNC_DEF, "set_age");
        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "25");
        IRGenerator.IRInstruction setProp = block.createInstruction(IRGenerator.OpCode.PROP_SET, "age");
        IRGenerator.IRInstruction afterHook = block.createInstruction(IRGenerator.OpCode.AFTER_PROPS_HOOK, "age");

        program.addInstruction(funcDef);
        program.addInstruction(loadConst);
        program.addInstruction(setProp);
        program.addInstruction(afterHook);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("def set_age():"));
        assertTrue(result.contains("age = __new_value"));
        assertTrue(result.contains("__old_val = self.age"));
        assertTrue(result.contains("self.age = __new_value"));
        assertTrue(result.contains("self._after_property_change(\"age\", __old_val, __new_value)"));
    }

    @Test
    public void testPropertyGetOperation() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction loadProp = block.createInstruction(IRGenerator.OpCode.PROP_GET, "user.name");

        program.addInstruction(loadProp);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("__stack_top = user.name"));
    }

    @Test
    public void testSimplePropertySet() {
        PythonCodeGenerator generator = new PythonCodeGenerator();

        IRGenerator.IRProgram program = new IRGenerator.IRProgram("test.claw");
        IRGenerator.IRBasicBlock block = program.createTopLevelBlock();

        IRGenerator.IRInstruction loadConst = block.createInstruction(IRGenerator.OpCode.LOAD_CONST, "value");
        IRGenerator.IRInstruction setProp = block.createInstruction(IRGenerator.OpCode.PROP_SET, "my_prop");

        program.addInstruction(loadConst);
        program.addInstruction(setProp);

        ClawIR clawIR = new ClawIR(program, null, null, null);

        String result = generator.generate(clawIR);

        assertNotNull(result);
        assertTrue(result.contains("my_prop = __new_value"));
    }
}

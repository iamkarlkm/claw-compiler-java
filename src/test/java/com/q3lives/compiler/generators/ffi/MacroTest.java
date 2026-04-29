package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Macro 边界场景测试
 */
class MacroTest {

    private FFIBindingTable table;

    @BeforeEach
    void setUp() {
        table = new FFIBindingTable();
    }

    // ================================================================
    //  1. 常量宏测试
    // ================================================================

    @Test
    void testIntConstantMacro() {
        ExternMacro macro = new ExternMacro("MAX_VALUE", MacroKind.CONSTANT);
        macro.type = "Int";
        macro.value = "100";
        table.getAllMacros().add(macro);

        assertEquals("MAX_VALUE", macro.name);
        assertEquals(MacroKind.CONSTANT, macro.kind);
        assertEquals("Int", macro.type);
        assertEquals("100", macro.value);
    }

    @Test
    void testFloatConstantMacro() {
        ExternMacro macro = new ExternMacro("PI", MacroKind.CONSTANT);
        macro.type = "Float";
        macro.value = "3.14159";
        table.getAllMacros().add(macro);

        assertEquals("3.14159", macro.value);
    }

    @Test
    void testStringConstantMacro() {
        ExternMacro macro = new ExternMacro("HELLO", MacroKind.CONSTANT);
        macro.type = "String";
        macro.value = "\"Hello, World!\"";
        table.getAllMacros().add(macro);

        assertTrue(macro.value.startsWith("\""));
    }

    @Test
    void testBoolConstantMacro() {
        ExternMacro macro = new ExternMacro("ENABLE_FEATURE", MacroKind.CONSTANT);
        macro.type = "Bool";
        macro.value = "true";
        table.getAllMacros().add(macro);

        assertEquals("true", macro.value);
    }

    @Test
    void testHexConstantMacro() {
        ExternMacro macro = new ExternMacro("MASK", MacroKind.CONSTANT);
        macro.type = "Int";
        macro.value = "0xFF";
        table.getAllMacros().add(macro);

        assertEquals("0xFF", macro.value);
    }

    // ================================================================
    //  2. 函数宏测试
    // ================================================================

    @Test
    void testFunctionMacro() {
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("a", "Int"));
        params.add(new ExternParam("b", "Int"));

        ExternMacro macro = new ExternMacro("MAX", MacroKind.FUNCTION);
        macro.params = params;
        macro.returnType = "Int";
        macro.expansion = "(a > b ? a : b)";
        table.getAllMacros().add(macro);

        assertEquals(MacroKind.FUNCTION, macro.kind);
        assertEquals(2, macro.params.size());
        assertEquals("Int", macro.returnType);
    }

    @Test
    void testFunctionMacroNoParams() {
        ExternMacro macro = new ExternMacro("GET_TIMESTAMP", MacroKind.FUNCTION);
        macro.params = new ArrayList<>(); // 初始化空列表
        macro.returnType = "Int64";
        macro.expansion = "current_time()";
        table.getAllMacros().add(macro);

        assertEquals(0, macro.params.size());
    }

    @Test
    void testFunctionMacroWithExpansion() {
        ExternMacro macro = new ExternMacro("SQUARE", MacroKind.FUNCTION);
        macro.params = List.of(new ExternParam("x", "Int"));
        macro.returnType = "Int";
        macro.expansion = "(x * x)";
        table.getAllMacros().add(macro);

        assertEquals("(x * x)", macro.expansion);
    }

    // ================================================================
    //  3. 宏与其他类型组合测试
    // ================================================================

    @Test
    void testMacroWithStruct() {
        // 声明 struct
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        table.getAllStructs().add(point);

        // 声明与 struct 相关的宏
        ExternMacro zero = new ExternMacro("POINT_ZERO", MacroKind.CONSTANT);
        zero.type = "Pointer";
        zero.value = "0";
        table.getAllMacros().add(zero);

        assertEquals(1, table.getAllStructs().size());
        assertEquals(1, table.getAllMacros().size());
    }

    @Test
    void testMacroWithEnum() {
        // 声明 enum
        ExternEnum status = new ExternEnum("Status");
        status.members.add(new EnumMember("SUCCESS", "0"));
        status.members.add(new EnumMember("FAILURE", "1"));
        table.getAllEnums().add(status);

        // 声明与 enum 相关的宏
        ExternMacro ok = new ExternMacro("OK", MacroKind.CONSTANT);
        ok.type = "Int";
        ok.value = "0";
        table.getAllMacros().add(ok);

        assertEquals(1, table.getAllEnums().size());
        assertEquals(1, table.getAllMacros().size());
    }

    // ================================================================
    //  4. 边界情况测试
    // ================================================================

    @Test
    void testEmptyMacroName() {
        // 空名称的宏（边界情况）
        ExternMacro macro = new ExternMacro("", MacroKind.CONSTANT);
        macro.type = "Int";
        macro.value = "0";
        table.getAllMacros().add(macro);

        assertEquals("", macro.name);
    }

    @Test
    void testMacroWithDescription() {
        ExternMacro macro = new ExternMacro("VERSION", MacroKind.CONSTANT);
        macro.type = "Int";
        macro.value = "100";
        macro.description = "API version number";
        table.getAllMacros().add(macro);

        assertEquals("API version number", macro.description);
    }

    @Test
    void testManyMacros() {
        // 添加多个宏
        for (int i = 0; i < 20; i++) {
            ExternMacro macro = new ExternMacro("MACRO_" + i, MacroKind.CONSTANT);
            macro.type = "Int";
            macro.value = String.valueOf(i);
            table.getAllMacros().add(macro);
        }

        assertEquals(20, table.getAllMacros().size());
    }

    @Test
    void testMacroWithoutExpansion() {
        // 没有 expansion 的函数宏
        ExternMacro macro = new ExternMacro("MAYBE_DEFINE", MacroKind.FUNCTION);
        macro.params = List.of(new ExternParam("x", "Int"));
        macro.returnType = "Int";
        macro.expansion = null; // 可能没有定义
        table.getAllMacros().add(macro);

        assertNull(macro.expansion);
    }

    @Test
    void testDuplicateMacroNames() {
        // 允许多个同名宏（不同块）
        ExternMacro m1 = new ExternMacro("VERSION", MacroKind.CONSTANT);
        m1.value = "1";
        table.getAllMacros().add(m1);

        ExternMacro m2 = new ExternMacro("VERSION", MacroKind.CONSTANT);
        m2.value = "2";
        table.getAllMacros().add(m2);

        assertEquals(2, table.getAllMacros().size());
    }

    // ================================================================
    //  5. 特殊类型宏测试
    // ================================================================

    @Test
    void testMacroWithPointerType() {
        ExternMacro macro = new ExternMacro("NULL", MacroKind.CONSTANT);
        macro.type = "Pointer";
        macro.value = "0";
        table.getAllMacros().add(macro);

        assertEquals("Pointer", macro.type);
    }

    @Test
    void testMacroWithSizeT() {
        ExternMacro macro = new ExternMacro("BUFFER_SIZE", MacroKind.CONSTANT);
        macro.type = "SizeT";
        macro.value = "4096";
        table.getAllMacros().add(macro);

        assertEquals("SizeT", macro.type);
    }

    @Test
    void testMacroWithUInt64() {
        ExternMacro macro = new ExternMacro("LARGE_VALUE", MacroKind.CONSTANT);
        macro.type = "UInt64";
        macro.value = "18446744073709551615";
        table.getAllMacros().add(macro);

        assertEquals("UInt64", macro.type);
    }

    @Test
    void testFunctionMacroWithManyParams() {
        // 多参数函数宏
        List<ExternParam> params = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            params.add(new ExternParam("p" + i, "Int"));
        }

        ExternMacro macro = new ExternMacro("MULTI_MAX", MacroKind.FUNCTION);
        macro.params = params;
        macro.returnType = "Int";
        macro.expansion = "...";
        table.getAllMacros().add(macro);

        assertEquals(5, macro.params.size());
    }
}

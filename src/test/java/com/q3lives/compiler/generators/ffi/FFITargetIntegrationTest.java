package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import com.q3lives.ir.ClawIR;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FFI 与目标代码生成器集成测试
 *
 * <p>验证 ClawIR 能够携带 FFIBindingTable，并通过 hasFFIBindings() 正确检测。</p>
 */
class FFITargetIntegrationTest {

    @Test
    void testClawIRHasFFIBindingsPositive() {
        ClawIR ir = new ClawIR();
        FFIBindingTable table = createSampleBindingTable();
        ir.setFfiBindingTable(table);

        assertTrue(ir.hasFFIBindings(), "IR with FFI bindings should report true");
        assertNotNull(ir.getFfiBindingTable(), "FFI binding table should be retrievable");
        assertEquals(1, ir.getFfiBindingTable().getExternBlocks().size());
    }

    @Test
    void testClawIRHasFFIBindingsNegative() {
        ClawIR ir = new ClawIR();
        assertFalse(ir.hasFFIBindings(), "Empty IR should report no FFI bindings");
        assertNull(ir.getFfiBindingTable(), "FFI binding table should be null when not set");
    }

    @Test
    void testClawIRHasFFIBindingsEmptyTable() {
        ClawIR ir = new ClawIR();
        ir.setFfiBindingTable(new FFIBindingTable());
        assertFalse(ir.hasFFIBindings(), "Empty binding table should report no FFI bindings");
    }

    @Test
    void testClawIRSetFfiBindingTableTwice() {
        ClawIR ir = new ClawIR();
        FFIBindingTable first = createSampleBindingTable();
        FFIBindingTable second = new FFIBindingTable();

        ir.setFfiBindingTable(first);
        assertSame(first, ir.getFfiBindingTable());

        ir.setFfiBindingTable(second);
        assertSame(second, ir.getFfiBindingTable());
    }

    /** 构造一个包含示例声明的绑定表 */
    private FFIBindingTable createSampleBindingTable() {
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = table.newExternBlock();
        block.links.add(new LinkDirective("sqlite3", "sqlite3.h"));
        block.constants.add(new ExternConstant("SQLITE_OK", "Int", "0"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        func.libraryName = "sqlite3";
        block.functions.add(func);

        table.indexBlock(block);
        return table;
    }
}

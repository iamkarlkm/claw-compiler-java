package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import com.q3lives.ir.ClawIR;

import java.util.ArrayList;
import java.util.List;

/**
 * FFI 与目标代码生成器集成手动测试
 *
 * <p>验证 ClawIR 能够携带 FFIBindingTable，并通过 hasFFIBindings() 正确检测。</p>
 */
public class FFITargetIntegrationManualTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("FFI Target Integration Manual Test");
        System.out.println("========================================\n");

        testClawIRHasFFIBindingsPositive();
        testClawIRHasFFIBindingsNegative();
        testClawIRHasFFIBindingsEmptyTable();
        testClawIRSetFfiBindingTableTwice();

        System.out.println("\n========================================");
        System.out.println("Test complete: passed=" + passed + ", failed=" + failed);
        System.out.println("========================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (condition) {
            passed++;
            System.out.println("  PASS: " + message);
        } else {
            failed++;
            System.out.println("  FAIL: " + message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertNotNull(Object obj, String message) {
        assertTrue(obj != null, message);
    }

    private static void assertNull(Object obj, String message) {
        assertTrue(obj == null, message);
    }

    private static void assertSame(Object expected, Object actual) {
        assertTrue(expected == actual, "Expected same reference");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        boolean match = (expected == null && actual == null)
            || (expected != null && expected.equals(actual));
        if (match) {
            passed++;
            System.out.println("  PASS: " + message);
        } else {
            failed++;
            System.out.println("  FAIL: " + message);
            System.out.println("       Expected: " + expected);
            System.out.println("       Actual:   " + actual);
        }
    }

    static void testClawIRHasFFIBindingsPositive() {
        System.out.println("\n[Test] ClawIR hasFFIBindings - positive");
        ClawIR ir = new ClawIR();
        FFIBindingTable table = createSampleBindingTable();
        ir.setFfiBindingTable(table);

        assertTrue(ir.hasFFIBindings(), "IR with FFI bindings should report true");
        assertNotNull(ir.getFfiBindingTable(), "FFI binding table should be retrievable");
        assertEquals(1, ir.getFfiBindingTable().getExternBlocks().size(), "One extern block");
    }

    static void testClawIRHasFFIBindingsNegative() {
        System.out.println("\n[Test] ClawIR hasFFIBindings - negative");
        ClawIR ir = new ClawIR();
        assertFalse(ir.hasFFIBindings(), "Empty IR should report no FFI bindings");
        assertNull(ir.getFfiBindingTable(), "FFI binding table should be null when not set");
    }

    static void testClawIRHasFFIBindingsEmptyTable() {
        System.out.println("\n[Test] ClawIR hasFFIBindings - empty table");
        ClawIR ir = new ClawIR();
        ir.setFfiBindingTable(new FFIBindingTable());
        assertFalse(ir.hasFFIBindings(), "Empty binding table should report no FFI bindings");
    }

    static void testClawIRSetFfiBindingTableTwice() {
        System.out.println("\n[Test] ClawIR setFfiBindingTable twice");
        ClawIR ir = new ClawIR();
        FFIBindingTable first = createSampleBindingTable();
        FFIBindingTable second = new FFIBindingTable();

        ir.setFfiBindingTable(first);
        assertSame(first, ir.getFfiBindingTable());

        ir.setFfiBindingTable(second);
        assertSame(second, ir.getFfiBindingTable());
    }

    /** 构造一个包含示例声明的绑定表 */
    private static FFIBindingTable createSampleBindingTable() {
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

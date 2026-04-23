package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JavaFFIGenerator 手动测试运行器
 */
public class JavaFFIGeneratorManualTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("JavaFFIGenerator 手动测试套件");
        System.out.println("========================================\n");

        testGeneratePackageAndImports();
        testGenerateClassHeader();
        testGenerateLibraryLoading();
        testGenerateConstants();
        testGenerateMethodHandles();
        testGenerateWrapperMethod();
        testGenerateHelperMethods();
        testMapToJavaType();
        testMapToValueLayout();
        testGenerateAllComplete();
        testStringParameterConversion();

        System.out.println("\n========================================");
        System.out.println("测试完成: 通过=" + passed + ", 失败=" + failed);
        System.out.println("========================================");

        if (failed > 0) System.exit(1);
    }

    private static void assertTrue(boolean condition, String message) {
        if (condition) { passed++; System.out.println("  PASS: " + message); }
        else { failed++; System.out.println("  FAIL: " + message); }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        boolean match = (expected == null && actual == null) || (expected != null && expected.equals(actual));
        if (match) { passed++; System.out.println("  PASS: " + message); }
        else { failed++; System.out.println("  FAIL: " + message + " (期望=" + expected + ", 实际=" + actual + ")"); }
    }

    private static ExternBlock addBlock(FFIBindingTable table) {
        ExternBlock block = table.newExternBlock();
        table.indexBlock(block);
        return block;
    }

    static void testGeneratePackageAndImports() {
        System.out.println("\n[Test] generatePackageAndImports");
        FFIBindingTable table = new FFIBindingTable();
        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String result = generator.generatePackageAndImports();
        assertTrue(result.contains("package claw.ffi;"), "包声明");
        assertTrue(result.contains("import java.lang.foreign.*;"), "Panama 导入");
        assertTrue(result.contains("import java.lang.invoke.MethodHandle;"), "MethodHandle 导入");
    }

    static void testGenerateClassHeader() {
        System.out.println("\n[Test] generateClassHeader");
        FFIBindingTable table = new FFIBindingTable();
        JavaFFIGenerator generator = new JavaFFIGenerator(table, "Sqlite3FFI");
        String result = generator.generateClassHeader();
        assertTrue(result.contains("public final class Sqlite3FFI"), "类声明");
        assertTrue(result.contains("Auto-generated FFI bindings"), "类注释");
    }

    static void testGenerateLibraryLoading() {
        System.out.println("\n[Test] generateLibraryLoading");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.links.add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.indexBlock(block);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String result = generator.generateLibraryLoading();
        assertTrue(result.contains("private static final Linker LINKER = Linker.nativeLinker();"), "Linker");
        assertTrue(result.contains("SQLITE3_LOOKUP"), "SymbolLookup 字段");
        assertTrue(result.contains("SymbolLookup.libraryLookup("), "库加载");
        assertTrue(result.contains("System.mapLibraryName(\"sqlite3\")"), "库名映射");
    }

    static void testGenerateConstants() {
        System.out.println("\n[Test] generateConstants");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.constants.add(new ExternConstant("SQLITE_OK", "Int", "0"));
        block.constants.add(new ExternConstant("SQLITE_ROW", "Int", "100"));
        table.indexBlock(block);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String result = generator.generateConstants();
        assertTrue(result.contains("public static final int SQLITE_OK = 0;"), "int 常量");
        assertTrue(result.contains("public static final int SQLITE_ROW = 100;"), "int 常量 2");
    }

    static void testGenerateMethodHandles() {
        System.out.println("\n[Test] generateMethodHandles");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.links.add(new LinkDirective("sqlite3", "sqlite3.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        func.libraryName = "sqlite3";
        block.functions.add(func);
        table.indexBlock(block);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String result = generator.generateMethodHandles();
        assertTrue(result.contains("private static final MethodHandle MH_sqlite3_open;"), "字段声明");
        assertTrue(result.contains("LINKER.downcallHandle("), "downcallHandle");
        assertTrue(result.contains("findSymbol(SQLITE3_LOOKUP"), "符号查找");
        assertTrue(result.contains("FunctionDescriptor.of("), "函数描述符");
        assertTrue(result.contains("ValueLayout.JAVA_INT"), "返回类型 layout");
        assertTrue(result.contains("ValueLayout.ADDRESS"), "地址 layout");
    }

    static void testGenerateWrapperMethod() {
        System.out.println("\n[Test] generateWrapperMethod");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.links.add(new LinkDirective("sqlite3", "sqlite3.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        func.libraryName = "sqlite3";
        block.functions.add(func);
        table.indexBlock(block);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String result = generator.generateWrapperMethods();
        assertTrue(result.contains("public static int sqlite3_open(Arena arena, String filename, MemorySegment ppDb)"), "方法签名");
        assertTrue(result.contains("MemorySegment filename_c = toCString(arena, filename);"), "字符串转换");
        assertTrue(result.contains("MH_sqlite3_open.invokeExact("), "MethodHandle 调用");
        assertTrue(result.contains("throw new RuntimeException(\"FFI call failed: sqlite3_open\""), "异常处理");
    }

    static void testGenerateHelperMethods() {
        System.out.println("\n[Test] generateHelperMethods");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.links.add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.indexBlock(block);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String result = generator.generateHelperMethods();
        assertTrue(result.contains("private static MemorySegment toCString(Arena arena, String str)"), "toCString");
        assertTrue(result.contains("private static String fromCString(MemorySegment segment)"), "fromCString");
        assertTrue(result.contains("private static MemorySegment findSymbol(SymbolLookup lookup, String name)"), "findSymbol");
    }

    static void testMapToJavaType() {
        System.out.println("\n[Test] mapToJavaType");
        assertEquals("void", JavaFFIGenerator.mapToJavaType("Void"), "Void");
        assertEquals("int", JavaFFIGenerator.mapToJavaType("Int"), "Int");
        assertEquals("double", JavaFFIGenerator.mapToJavaType("Float"), "Float");
        assertEquals("String", JavaFFIGenerator.mapToJavaType("String"), "String");
        assertEquals("boolean", JavaFFIGenerator.mapToJavaType("Bool"), "Bool");
        assertEquals("MemorySegment", JavaFFIGenerator.mapToJavaType("Pointer"), "Pointer");
        assertEquals("MemorySegment", JavaFFIGenerator.mapToJavaType("Ref<Int>"), "Ref<Int>");
    }

    static void testMapToValueLayout() {
        System.out.println("\n[Test] mapToValueLayout");
        assertEquals("ValueLayout.JAVA_VOID", JavaFFIGenerator.mapToValueLayout("Void"), "Void");
        assertEquals("ValueLayout.JAVA_INT", JavaFFIGenerator.mapToValueLayout("Int"), "Int");
        assertEquals("ValueLayout.JAVA_DOUBLE", JavaFFIGenerator.mapToValueLayout("Float"), "Float");
        assertEquals("ValueLayout.ADDRESS", JavaFFIGenerator.mapToValueLayout("String"), "String");
        assertEquals("ValueLayout.ADDRESS", JavaFFIGenerator.mapToValueLayout("Pointer"), "Pointer");
        assertEquals("ValueLayout.ADDRESS", JavaFFIGenerator.mapToValueLayout("Ref<Int>"), "Ref<Int>");
    }

    static void testGenerateAllComplete() {
        System.out.println("\n[Test] generateAllComplete");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.links.add(new LinkDirective("sqlite3", "sqlite3.h"));
        block.constants.add(new ExternConstant("SQLITE_OK", "Int", "0"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        func.libraryName = "sqlite3";
        block.functions.add(func);
        table.indexBlock(block);

        JavaFFIGenerator generator = new JavaFFIGenerator(table, "Sqlite3FFI");
        String result = generator.generateAll();
        assertTrue(result.contains("public final class Sqlite3FFI"), "类声明");
        assertTrue(result.contains("SQLITE3_LOOKUP"), "库加载");
        assertTrue(result.contains("public static final int SQLITE_OK = 0;"), "常量");
        assertTrue(result.contains("MH_sqlite3_open"), "MethodHandle");
        assertTrue(result.contains("public static int sqlite3_open("), "包装方法");
    }

    static void testStringParameterConversion() {
        System.out.println("\n[Test] stringParameterConversion");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.links.add(new LinkDirective("c", "string.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("s", "CString"));
        ExternFunction func = new ExternFunction("strdup", params, "CString", false);
        func.libraryName = "c";
        block.functions.add(func);
        table.indexBlock(block);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String result = generator.generateWrapperMethods();
        assertTrue(result.contains("public static String strdup(Arena arena, String s)"), "CString 返回类型");
        assertTrue(result.contains("MemorySegment s_c = toCString(arena, s);"), "CString 参数转换");
        assertTrue(result.contains("MH_strdup.invokeExact(s_c)"), "转换后参数传递");
    }
}

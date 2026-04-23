package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import com.q3lives.compiler.generators.ffi.platform.PlatformConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PythonFFIGenerator 手动测试运行器
 *
 * <p>由于项目存在编译错误无法通过 Maven 运行测试，
 * 此类提供 main 方法直接运行所有测试断言。</p>
 */
public class PythonFFIGeneratorManualTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("PythonFFIGenerator 手动测试套件");
        System.out.println("========================================\n");

        testGenerateImports();
        testGenerateRuntimeImports();
        testGenerateLibraryLoading();
        testGetLibraryVarName();
        testGenerateConstants();
        testGenerateFunctionBindings();
        testGenerateFunctionBindingsMultiLibrary();
        testGenerateWrapperForStringParameters();
        testGenerateWrapperForCStringReturn();
        testGenerateWrapperForVoidReturn();
        testGenerateWrapperForRefParameter();
        testGeneratePlatformDetection();
        testMapClawTypeToCtype();
        testMapClawTypeToCtypeRef();
        testMapClawTypeToPythonTypeHint();
        testToSafePythonName();
        testGenerateStructDefinitions();
        testGenerateEnumDefinitions();
        testGenerateCallbackDefinitions();
        testGenerateMacroDefinitionsConstant();
        testGenerateAllComplete();
        testGenerateAllWithPlatformConstraints();

        System.out.println("\n========================================");
        System.out.println("测试完成: 通过=" + passed + ", 失败=" + failed);
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

    private static void assertEquals(Object expected, Object actual, String message) {
        boolean match = (expected == null && actual == null)
            || (expected != null && expected.equals(actual));
        if (match) {
            passed++;
            System.out.println("  PASS: " + message);
        } else {
            failed++;
            System.out.println("  FAIL: " + message);
            System.out.println("       期望: " + expected);
            System.out.println("       实际: " + actual);
        }
    }

    /** 辅助方法：创建 block 并索引到 table */
    private static ExternBlock addBlock(FFIBindingTable table) {
        ExternBlock block = table.newExternBlock();
        table.indexBlock(block);
        return block;
    }

    // ================================================================
    //  测试方法
    // ================================================================

    static void testGenerateImports() {
        System.out.println("\n[Test] generateImports");
        FFIBindingTable table = new FFIBindingTable();
        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateImports();
        assertTrue(result.contains("import ctypes"), "包含 ctypes 导入");
        assertTrue(result.contains("import ctypes.util"), "包含 ctypes.util 导入");
    }

    static void testGenerateRuntimeImports() {
        System.out.println("\n[Test] generateRuntimeImports");
        FFIBindingTable table = new FFIBindingTable();
        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateRuntimeImports();
        assertTrue(result.contains("import claw_runtime"), "包含 claw_runtime 导入");
    }

    static void testGenerateLibraryLoading() {
        System.out.println("\n[Test] generateLibraryLoading");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.links.add(new LinkDirective("sqlite3", "sqlite3.h"));
        // 重新索引以更新全局 links
        table.indexBlock(block);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateLibraryLoading();
        assertTrue(result.contains("_lib_path_sqlite3 = ctypes.util.find_library(\"sqlite3\")"), "库路径查找");
        assertTrue(result.contains("raise claw_runtime.ClawIOError"), "错误处理");
        assertTrue(result.contains("_lib_sqlite3 = ctypes.CDLL(_lib_path_sqlite3)"), "CDLL 加载");
    }

    static void testGetLibraryVarName() {
        System.out.println("\n[Test] getLibraryVarName");
        FFIBindingTable table = new FFIBindingTable();
        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        assertEquals("_lib_sqlite3", generator.getLibraryVarName("sqlite3"), "标准库名");
        assertEquals("_lib_my_lib", generator.getLibraryVarName("my-lib"), "含连字符库名");
    }

    static void testGenerateConstants() {
        System.out.println("\n[Test] generateConstants");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.constants.add(new ExternConstant("SQLITE_OK", "Int", "0"));
        block.constants.add(new ExternConstant("SQLITE_ROW", "Int", "100"));
        table.indexBlock(block);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateConstants();
        assertTrue(result.contains("SQLITE_OK = 0"), "常量 SQLITE_OK");
        assertTrue(result.contains("SQLITE_ROW = 100"), "常量 SQLITE_ROW");
    }

    static void testGenerateFunctionBindings() {
        System.out.println("\n[Test] generateFunctionBindings");
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

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateFunctionBindings();
        assertTrue(result.contains("_lib_sqlite3.sqlite3_open.argtypes"), "argtypes 设置");
        assertTrue(result.contains("ctypes.c_char_p"), "String 映射为 c_char_p");
        assertTrue(result.contains("ctypes.c_void_p"), "Pointer 映射为 c_void_p");
        assertTrue(result.contains("_lib_sqlite3.sqlite3_open.restype = ctypes.c_int"), "restype 设置");
    }

    static void testGenerateFunctionBindingsMultiLibrary() {
        System.out.println("\n[Test] generateFunctionBindingsMultiLibrary");
        FFIBindingTable table = new FFIBindingTable();

        ExternBlock block1 = addBlock(table);
        block1.links.add(new LinkDirective("sqlite3", "sqlite3.h"));
        ExternFunction sqliteFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("f", "String")), "Int", false);
        sqliteFunc.libraryName = "sqlite3";
        block1.functions.add(sqliteFunc);
        table.indexBlock(block1);

        ExternBlock block2 = addBlock(table);
        block2.links.add(new LinkDirective("curl", "curl.h"));
        ExternFunction curlFunc = new ExternFunction("curl_easy_init",
            Collections.emptyList(), "Pointer", false);
        curlFunc.libraryName = "curl";
        block2.functions.add(curlFunc);
        table.indexBlock(block2);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateFunctionBindings();
        assertTrue(result.contains("_lib_sqlite3.sqlite3_open.argtypes"), "sqlite3 函数绑定");
        assertTrue(result.contains("_lib_curl.curl_easy_init.restype"), "curl 函数绑定");
    }

    static void testGenerateWrapperForStringParameters() {
        System.out.println("\n[Test] generateWrapperForStringParameters");
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

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateWrapperFunctions();
        assertTrue(result.contains("def _claw_ffi_sqlite3_open("), "包装函数定义");
        assertTrue(result.contains("filename: str"), "String 类型注解");
        assertTrue(result.contains("filename_c = filename.encode('utf-8')"), "UTF-8 编码转换");
        assertTrue(result.contains("return _result"), "返回结果");
    }

    static void testGenerateWrapperForCStringReturn() {
        System.out.println("\n[Test] generateWrapperForCStringReturn");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.links.add(new LinkDirective("c", "string.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("s", "CString"));
        ExternFunction func = new ExternFunction("strdup", params, "CString", false);
        func.libraryName = "c";
        block.functions.add(func);
        table.indexBlock(block);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateWrapperFunctions();
        assertTrue(result.contains("def _claw_ffi_strdup("), "包装函数定义");
        assertTrue(result.contains("-> str:"), "CString 返回类型注解");
        assertTrue(result.contains("return _result.decode('utf-8')"), "UTF-8 解码转换");
    }

    static void testGenerateWrapperForVoidReturn() {
        System.out.println("\n[Test] generateWrapperForVoidReturn");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.links.add(new LinkDirective("c", "stdio.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("s", "CString"));
        ExternFunction func = new ExternFunction("puts", params, "Void", false);
        func.libraryName = "c";
        block.functions.add(func);
        table.indexBlock(block);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateWrapperFunctions();
        assertTrue(result.contains("def _claw_ffi_puts("), "包装函数定义");
        assertTrue(result.contains("-> None:"), "Void 返回类型注解");
        assertTrue(result.contains("return None"), "返回 None");
    }

    static void testGenerateWrapperForRefParameter() {
        System.out.println("\n[Test] generateWrapperForRefParameter");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.links.add(new LinkDirective("c", "stdlib.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("n", "Ref<Int>"));
        ExternFunction func = new ExternFunction("abs", params, "Int", false);
        func.libraryName = "c";
        block.functions.add(func);
        table.indexBlock(block);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateWrapperFunctions();
        assertTrue(result.contains("def _claw_ffi_abs("), "包装函数定义");
        assertTrue(result.contains("ctypes.byref(n)"), "byref 处理");
    }

    static void testGeneratePlatformDetection() {
        System.out.println("\n[Test] generatePlatformDetection");
        FFIBindingTable table = new FFIBindingTable();
        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generatePlatformDetection();
        assertTrue(result.contains("def _claw_is_platform"), "平台检测函数");
        assertTrue(result.contains("def _claw_is_arch"), "架构检测函数");
        assertTrue(result.contains("sys.platform"), "使用 sys.platform");
    }

    static void testMapClawTypeToCtype() {
        System.out.println("\n[Test] mapClawTypeToCtype");
        assertEquals("None", PythonFFIGenerator.mapClawTypeToCtype("Void"), "Void");
        assertEquals("ctypes.c_int", PythonFFIGenerator.mapClawTypeToCtype("Int"), "Int");
        assertEquals("ctypes.c_double", PythonFFIGenerator.mapClawTypeToCtype("Float"), "Float");
        assertEquals("ctypes.c_char_p", PythonFFIGenerator.mapClawTypeToCtype("String"), "String");
        assertEquals("ctypes.c_bool", PythonFFIGenerator.mapClawTypeToCtype("Bool"), "Bool");
    }

    static void testMapClawTypeToCtypeRef() {
        System.out.println("\n[Test] mapClawTypeToCtypeRef");
        assertEquals("ctypes.POINTER(ctypes.c_int)",
            PythonFFIGenerator.mapClawTypeToCtype("Ref<Int>"), "Ref<Int>");
        assertEquals("ctypes.POINTER(ctypes.c_double)",
            PythonFFIGenerator.mapClawTypeToCtype("Ref<Float>"), "Ref<Float>");
    }

    static void testMapClawTypeToPythonTypeHint() {
        System.out.println("\n[Test] mapClawTypeToPythonTypeHint");
        assertEquals("None", PythonFFIGenerator.mapClawTypeToPythonTypeHint("Void"), "Void");
        assertEquals("int", PythonFFIGenerator.mapClawTypeToPythonTypeHint("Int"), "Int");
        assertEquals("float", PythonFFIGenerator.mapClawTypeToPythonTypeHint("Float"), "Float");
        assertEquals("str", PythonFFIGenerator.mapClawTypeToPythonTypeHint("String"), "String");
        assertEquals("bool", PythonFFIGenerator.mapClawTypeToPythonTypeHint("Bool"), "Bool");
    }

    static void testToSafePythonName() {
        System.out.println("\n[Test] toSafePythonName");
        assertEquals("sqlite3", PythonFFIGenerator.toSafePythonName("sqlite3"), "标准名");
        assertEquals("my_lib", PythonFFIGenerator.toSafePythonName("my-lib"), "连字符");
        assertEquals("lib_2_0", PythonFFIGenerator.toSafePythonName("lib-2.0"), "版本号");
    }

    static void testGenerateStructDefinitions() {
        System.out.println("\n[Test] generateStructDefinitions");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        point.description = "2D Point";
        block.structs.add(point);
        table.indexBlock(block);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateStructDefinitions();
        assertTrue(result.contains("class Point(ctypes.Structure):"), "结构体类定义");
        assertTrue(result.contains("\"\"\"2D Point\"\"\""), "结构体文档");
        assertTrue(result.contains("_fields_ = ["), "_fields_ 定义");
        assertTrue(result.contains("(\"x\", ctypes.c_double)"), "Float 字段映射");
    }

    static void testGenerateEnumDefinitions() {
        System.out.println("\n[Test] generateEnumDefinitions");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        ExternEnum curlCode = new ExternEnum("CURLcode");
        curlCode.members.add(new EnumMember("CURLE_OK", "0"));
        curlCode.members.add(new EnumMember("CURLE_UNSUPPORTED_PROTOCOL", "1"));
        block.enums.add(curlCode);
        table.indexBlock(block);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateEnumDefinitions();
        assertTrue(result.contains("import enum"), "enum 导入");
        assertTrue(result.contains("class CURLcode(enum.IntEnum):"), "枚举类定义");
        assertTrue(result.contains("CURLE_OK = 0"), "枚举成员");
    }

    static void testGenerateCallbackDefinitions() {
        System.out.println("\n[Test] generateCallbackDefinitions");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("data", "Pointer"));
        params.add(new ExternParam("size", "SizeT"));
        ExternCallback callback = new ExternCallback("WriteCallback", params, "SizeT");
        callback.description = "Write callback";
        block.callbacks.add(callback);
        table.indexBlock(block);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateCallbackDefinitions();
        assertTrue(result.contains("WriteCallback = ctypes.CFUNCTYPE("), "CFUNCTYPE 定义");
        assertTrue(result.contains("ctypes.c_size_t"), "返回类型");
        assertTrue(result.contains("ctypes.c_void_p"), "参数类型");
    }

    static void testGenerateMacroDefinitionsConstant() {
        System.out.println("\n[Test] generateMacroDefinitionsConstant");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        ExternMacro macro = new ExternMacro("SQLITE_VERSION_NUMBER", MacroKind.CONSTANT);
        macro.value = "3039004";
        block.macros.add(macro);
        table.indexBlock(block);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateMacroDefinitions();
        assertTrue(result.contains("SQLITE_VERSION_NUMBER = 3039004"), "常量宏");
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

        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        block.structs.add(point);
        table.indexBlock(block);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateAll();

        assertTrue(result.contains("import ctypes"), "包含 ctypes");
        assertTrue(result.contains("import claw_runtime"), "包含 claw_runtime");
        assertTrue(result.contains("def _claw_is_platform"), "包含平台检测");
        assertTrue(result.contains("_lib_sqlite3 = ctypes.CDLL"), "包含库加载");
        assertTrue(result.contains("SQLITE_OK = 0"), "包含常量");
        assertTrue(result.contains("class Point(ctypes.Structure):"), "包含结构体");
        assertTrue(result.contains("_lib_sqlite3.sqlite3_open.argtypes"), "包含函数绑定");
        assertTrue(result.contains("def _claw_ffi_sqlite3_open("), "包含包装函数");
    }

    static void testGenerateAllWithPlatformConstraints() {
        System.out.println("\n[Test] generateAllWithPlatformConstraints");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = table.newExternBlock();
        block.platform = new PlatformConstraint().addPlatform("windows");
        block.links.add(new LinkDirective("kernel32", "windows.h"));
        ExternFunction func = new ExternFunction("GetLastError",
            List.of(new ExternParam("code", "UInt32")), "UInt32", false);
        func.libraryName = "kernel32";
        block.functions.add(func);
        table.getExternBlocks().add(block);
        table.indexBlock(block);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String result = generator.generateAll();

        assertTrue(result.contains("def _claw_is_platform"), "平台检测函数");
        assertTrue(result.contains("if _claw_is_platform(\"windows\"):"), "平台条件");
        assertTrue(result.contains("_lib_kernel32 = ctypes.CDLL"), "条件库加载");
    }
}

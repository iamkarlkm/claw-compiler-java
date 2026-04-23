package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import com.q3lives.compiler.generators.ffi.platform.PlatformConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonFFIGenerator 完整测试套件
 *
 * <p>覆盖所有生成方法：导入、库加载、常量、函数绑定、包装函数、
 * 平台检测、结构体、枚举、回调、宏生成等。</p>
 */
class PythonFFIGeneratorTest {

    private FFIBindingTable table;
    private PythonFFIGenerator generator;

    @BeforeEach
    void setUp() {
        table = new FFIBindingTable();
        generator = new PythonFFIGenerator(table);
    }

    // ================================================================
    //  1. 基础导入测试
    // ================================================================

    @Test
    void testGenerateImports() {
        String result = generator.generateImports();

        assertTrue(result.contains("import ctypes"));
        assertTrue(result.contains("import ctypes.util"));
        assertTrue(result.contains("# ========== FFI Imports =========="));
    }

    @Test
    void testGenerateRuntimeImports() {
        String result = generator.generateRuntimeImports();

        assertTrue(result.contains("import claw_runtime"));
        assertTrue(result.contains("# ========== Runtime Imports =========="));
    }

    // ================================================================
    //  2. 库加载测试
    // ================================================================

    @Test
    void testGenerateLibraryLoading() {
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        String result = generator.generateLibraryLoading();

        assertTrue(result.contains("_lib_path_sqlite3 = ctypes.util.find_library(\"sqlite3\")"));
        assertTrue(result.contains("if _lib_path_sqlite3 is None:"));
        assertTrue(result.contains("raise claw_runtime.ClawIOError"));
        assertTrue(result.contains("_lib_sqlite3 = ctypes.CDLL(_lib_path_sqlite3)"));
    }

    @Test
    void testGenerateLibraryLoadingWithUnsafeName() {
        table.getAllLinks().add(new LinkDirective("my-lib", "my-lib.h"));
        String result = generator.generateLibraryLoading();

        // 非安全字符应被替换为下划线
        assertTrue(result.contains("_lib_path_my_lib"));
        assertTrue(result.contains("_lib_my_lib = ctypes.CDLL"));
    }

    @Test
    void testGetLibraryVarName() {
        assertEquals("_lib_sqlite3", generator.getLibraryVarName("sqlite3"));
        assertEquals("_lib_my_lib", generator.getLibraryVarName("my-lib"));
    }

    @Test
    void testGenerateLibraryLoadingEmptyLinks() {
        String result = generator.generateLibraryLoading();
        assertEquals("", result);
    }

    // ================================================================
    //  3. 常量生成测试
    // ================================================================

    @Test
    void testGenerateConstants() {
        ExternConstant ok = new ExternConstant("SQLITE_OK", "Int", "0");
        ExternConstant row = new ExternConstant("SQLITE_ROW", "Int", "100");
        table.getAllConstants().put("SQLITE_OK", ok);
        table.getAllConstants().put("SQLITE_ROW", row);

        String result = generator.generateConstants();

        assertTrue(result.contains("SQLITE_OK = 0"));
        assertTrue(result.contains("SQLITE_ROW = 100"));
    }

    @Test
    void testGenerateConstantsEmpty() {
        String result = generator.generateConstants();
        assertEquals("", result);
    }

    // ================================================================
    //  4. 函数绑定测试
    // ================================================================

    @Test
    void testGenerateFunctionBindings() {
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction openFunc = new ExternFunction("sqlite3_open", params, "Int", false);
        openFunc.libraryName = "sqlite3";
        table.getAllFunctions().put("sqlite3_open", openFunc);

        String result = generator.generateFunctionBindings();

        assertTrue(result.contains("_lib_sqlite3.sqlite3_open.argtypes"));
        assertTrue(result.contains("ctypes.c_char_p"));
        assertTrue(result.contains("ctypes.c_void_p"));
        assertTrue(result.contains("_lib_sqlite3.sqlite3_open.restype = ctypes.c_int"));
    }

    @Test
    void testGenerateFunctionBindingsMultiLibrary() {
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllLinks().add(new LinkDirective("curl", "curl.h"));

        ExternFunction sqliteFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String")), "Int", false);
        sqliteFunc.libraryName = "sqlite3";
        table.getAllFunctions().put("sqlite3_open", sqliteFunc);

        ExternFunction curlFunc = new ExternFunction("curl_easy_init",
            Collections.emptyList(), "Pointer", false);
        curlFunc.libraryName = "curl";
        table.getAllFunctions().put("curl_easy_init", curlFunc);

        String result = generator.generateFunctionBindings();

        assertTrue(result.contains("_lib_sqlite3.sqlite3_open.argtypes"));
        assertTrue(result.contains("_lib_curl.curl_easy_init.restype"));
    }

    @Test
    void testGenerateFunctionBindingsEmpty() {
        String result = generator.generateFunctionBindings();
        assertEquals("", result);
    }

    // ================================================================
    //  5. 包装函数测试
    // ================================================================

    @Test
    void testGenerateWrapperForStringParameters() {
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        func.libraryName = "sqlite3";
        table.getAllFunctions().put("sqlite3_open", func);

        String result = generator.generateWrapperFunctions();

        assertTrue(result.contains("def _claw_ffi_sqlite3_open("));
        assertTrue(result.contains("filename: str"));
        assertTrue(result.contains("filename_c = filename.encode('utf-8')"));
        assertTrue(result.contains("return _result"));
    }

    @Test
    void testGenerateWrapperForCStringReturn() {
        table.getAllLinks().add(new LinkDirective("c", "string.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("s", "CString"));
        ExternFunction func = new ExternFunction("strdup", params, "CString", false);
        func.libraryName = "c";
        table.getAllFunctions().put("strdup", func);

        String result = generator.generateWrapperFunctions();

        assertTrue(result.contains("def _claw_ffi_strdup("));
        assertTrue(result.contains("-> str:"));
        assertTrue(result.contains("return _result.decode('utf-8')"));
    }

    @Test
    void testGenerateWrapperForVoidReturn() {
        table.getAllLinks().add(new LinkDirective("c", "stdlib.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("ptr", "Pointer"));
        ExternFunction func = new ExternFunction("free", params, "Void", false);
        func.libraryName = "c";
        table.getAllFunctions().put("free", func);

        String result = generator.generateWrapperFunctions();

        assertTrue(result.contains("def _claw_ffi_free("));
        assertTrue(result.contains("-> None:"));
        assertTrue(result.contains("return None"));
    }

    @Test
    void testGenerateWrapperForRefParameter() {
        table.getAllLinks().add(new LinkDirective("c", "stdlib.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("n", "Ref<Int>"));
        ExternFunction func = new ExternFunction("abs", params, "Int", false);
        func.libraryName = "c";
        table.getAllFunctions().put("abs", func);

        String result = generator.generateWrapperFunctions();

        assertTrue(result.contains("def _claw_ffi_abs("));
        assertTrue(result.contains("ctypes.byref(n)"));
    }

    @Test
    void testGenerateWrapperNoWrapperNeeded() {
        table.getAllLinks().add(new LinkDirective("c", "math.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("x", "Float"));
        ExternFunction func = new ExternFunction("sqrt", params, "Float", false);
        func.libraryName = "c";
        table.getAllFunctions().put("sqrt", func);

        String result = generator.generateWrapperFunctions();

        assertTrue(result.contains("# sqrt: no wrapper needed"));
    }

    // ================================================================
    //  6. 平台检测测试
    // ================================================================

    @Test
    void testGeneratePlatformDetection() {
        String result = generator.generatePlatformDetection();

        assertTrue(result.contains("def _claw_is_platform"));
        assertTrue(result.contains("def _claw_is_arch"));
        assertTrue(result.contains("sys.platform"));
        assertTrue(result.contains("windows"));
        assertTrue(result.contains("linux"));
        assertTrue(result.contains("macos"));
    }

    @Test
    void testGeneratePlatformConditionalLoading() {
        ExternBlock block = table.newExternBlock();
        block.platform = new PlatformConstraint().addPlatform("windows");
        block.links.add(new LinkDirective("kernel32", "windows.h"));
        table.getExternBlocks().add(block);

        String result = generator.generatePlatformConditionalLoading(table);

        assertTrue(result.contains("if _claw_is_platform(\"windows\"):"));
        assertTrue(result.contains("_lib_kernel32 = ctypes.CDLL"));
    }

    @Test
    void testGeneratePythonPlatformIf() {
        PlatformConstraint constraint = new PlatformConstraint()
            .addPlatform("windows", "linux")
            .addArchitecture("x86_64");

        String result = generator.generatePythonPlatformIf(constraint);

        assertTrue(result.startsWith("if "));
        assertTrue(result.contains("_claw_is_platform"));
        assertTrue(result.contains("\"windows\""));
        assertTrue(result.contains("\"linux\""));
        assertTrue(result.contains("_claw_is_arch"));
        assertTrue(result.contains("\"x86_64\""));
        assertTrue(result.contains(" and "));
    }

    // ================================================================
    //  7. 类型映射测试
    // ================================================================

    @Test
    void testMapClawTypeToCtype() {
        assertEquals("None", PythonFFIGenerator.mapClawTypeToCtype("Void"));
        assertEquals("ctypes.c_int", PythonFFIGenerator.mapClawTypeToCtype("Int"));
        assertEquals("ctypes.c_double", PythonFFIGenerator.mapClawTypeToCtype("Float"));
        assertEquals("ctypes.c_char_p", PythonFFIGenerator.mapClawTypeToCtype("String"));
        assertEquals("ctypes.c_char_p", PythonFFIGenerator.mapClawTypeToCtype("CString"));
        assertEquals("ctypes.c_bool", PythonFFIGenerator.mapClawTypeToCtype("Bool"));
        assertEquals("ctypes.c_void_p", PythonFFIGenerator.mapClawTypeToCtype("Pointer"));
        assertEquals("ctypes.c_size_t", PythonFFIGenerator.mapClawTypeToCtype("SizeT"));
        assertEquals("ctypes.c_int32", PythonFFIGenerator.mapClawTypeToCtype("Int32"));
        assertEquals("ctypes.c_uint64", PythonFFIGenerator.mapClawTypeToCtype("UInt64"));
    }

    @Test
    void testMapClawTypeToCtypeRef() {
        assertEquals("ctypes.POINTER(ctypes.c_int)",
            PythonFFIGenerator.mapClawTypeToCtype("Ref<Int>"));
        assertEquals("ctypes.POINTER(ctypes.c_double)",
            PythonFFIGenerator.mapClawTypeToCtype("Ref<Float>"));
    }

    @Test
    void testMapClawTypeToCtypeCArray() {
        assertEquals("ctypes.POINTER(ctypes.c_int)",
            PythonFFIGenerator.mapClawTypeToCtype("CArray<Int>"));
    }

    @Test
    void testMapClawTypeToCtypeUnknown() {
        assertEquals("ctypes.c_void_p",
            PythonFFIGenerator.mapClawTypeToCtype("SomeCustomType"));
    }

    @Test
    void testMapClawTypeToPythonTypeHint() {
        assertEquals("None", PythonFFIGenerator.mapClawTypeToPythonTypeHint("Void"));
        assertEquals("int", PythonFFIGenerator.mapClawTypeToPythonTypeHint("Int"));
        assertEquals("float", PythonFFIGenerator.mapClawTypeToPythonTypeHint("Float"));
        assertEquals("str", PythonFFIGenerator.mapClawTypeToPythonTypeHint("String"));
        assertEquals("str", PythonFFIGenerator.mapClawTypeToPythonTypeHint("CString"));
        assertEquals("bool", PythonFFIGenerator.mapClawTypeToPythonTypeHint("Bool"));
        assertNull(PythonFFIGenerator.mapClawTypeToPythonTypeHint("Pointer"));
    }

    // ================================================================
    //  8. 辅助方法测试
    // ================================================================

    @Test
    void testToSafePythonName() {
        assertEquals("sqlite3", PythonFFIGenerator.toSafePythonName("sqlite3"));
        assertEquals("my_lib", PythonFFIGenerator.toSafePythonName("my-lib"));
        assertEquals("lib_2_0", PythonFFIGenerator.toSafePythonName("lib-2.0"));
    }

    @Test
    void testGetLibraryFileName() {
        assertEquals("sqlite3", generator.getLibraryFileName("sqlite3"));
    }

    // ================================================================
    //  9. 结构体生成测试
    // ================================================================

    @Test
    void testGenerateStructDefinitions() {
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        point.description = "2D Point";
        table.getAllStructs().add(point);

        String result = generator.generateStructDefinitions();

        assertTrue(result.contains("class Point(ctypes.Structure):"));
        assertTrue(result.contains("\"\"\"2D Point\"\"\""));
        assertTrue(result.contains("_fields_ = ["));
        assertTrue(result.contains("(\"x\", ctypes.c_double)"));
        assertTrue(result.contains("(\"y\", ctypes.c_double)"));
    }

    @Test
    void testGenerateStructDefinitionsPacked() {
        ExternStruct packed = new ExternStruct("PackedStruct");
        packed.packed = true;
        packed.alignment = 4;
        packed.fields.add(new StructField("a", "Int8"));
        table.getAllStructs().add(packed);

        String result = generator.generateStructDefinitions();

        assertTrue(result.contains("_pack_ = 1"));
        assertTrue(result.contains("_align_ = 4"));
    }

    @Test
    void testGenerateStructDefinitionsEmpty() {
        String result = generator.generateStructDefinitions();
        assertEquals("", result);
    }

    // ================================================================
    //  10. 枚举生成测试
    // ================================================================

    @Test
    void testGenerateEnumDefinitions() {
        ExternEnum curlCode = new ExternEnum("CURLcode");
        curlCode.members.add(new EnumMember("CURLE_OK", "0"));
        curlCode.members.add(new EnumMember("CURLE_UNSUPPORTED_PROTOCOL", "1"));
        curlCode.description = "CURL error codes";
        table.getAllEnums().add(curlCode);

        String result = generator.generateEnumDefinitions();

        assertTrue(result.contains("import enum"));
        assertTrue(result.contains("class CURLcode(enum.IntEnum):"));
        assertTrue(result.contains("\"\"\"CURL error codes\"\"\""));
        assertTrue(result.contains("CURLE_OK = 0"));
        assertTrue(result.contains("CURLE_UNSUPPORTED_PROTOCOL = 1"));
    }

    @Test
    void testGenerateEnumDefinitionsBitmask() {
        ExternEnum flags = new ExternEnum("OpenFlags");
        flags.isBitmask = true;
        flags.members.add(new EnumMember("O_RDONLY", "0"));
        flags.members.add(new EnumMember("O_WRONLY", "1"));
        table.getAllEnums().add(flags);

        String result = generator.generateEnumDefinitions();

        assertTrue(result.contains("class OpenFlags(enum.IntFlag):"));
    }

    @Test
    void testGenerateEnumDefinitionsEmpty() {
        String result = generator.generateEnumDefinitions();
        assertEquals("", result);
    }

    // ================================================================
    //  11. 回调生成测试
    // ================================================================

    @Test
    void testGenerateCallbackDefinitions() {
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("data", "Pointer"));
        params.add(new ExternParam("size", "SizeT"));
        ExternCallback callback = new ExternCallback("WriteCallback", params, "SizeT");
        callback.description = "Write callback for libcurl";
        table.getAllCallbacks().add(callback);

        String result = generator.generateCallbackDefinitions();

        assertTrue(result.contains("# Write callback for libcurl"));
        assertTrue(result.contains("WriteCallback = ctypes.CFUNCTYPE("));
        assertTrue(result.contains("ctypes.c_size_t"));
        assertTrue(result.contains("ctypes.c_void_p"));
    }

    @Test
    void testGenerateCallbackDefinitionsEmpty() {
        String result = generator.generateCallbackDefinitions();
        assertEquals("", result);
    }

    // ================================================================
    //  12. 宏生成测试
    // ================================================================

    @Test
    void testGenerateMacroDefinitionsConstant() {
        ExternMacro macro = new ExternMacro("SQLITE_VERSION_NUMBER", MacroKind.CONSTANT);
        macro.value = "3039004";
        table.getAllMacros().add(macro);

        String result = generator.generateMacroDefinitions();

        assertTrue(result.contains("SQLITE_VERSION_NUMBER = 3039004"));
    }

    @Test
    void testGenerateMacroDefinitionsFunction() {
        ExternMacro macro = new ExternMacro("MAX", MacroKind.FUNCTION);
        macro.params = List.of(new ExternParam("a", "Int"), new ExternParam("b", "Int"));
        macro.expansion = "a if a > b else b";
        table.getAllMacros().add(macro);

        String result = generator.generateMacroDefinitions();

        assertTrue(result.contains("def MAX(a, b):"));
        assertTrue(result.contains("return a if a > b else b"));
    }

    @Test
    void testGenerateMacroDefinitionsEmpty() {
        String result = generator.generateMacroDefinitions();
        assertEquals("", result);
    }

    // ================================================================
    //  13. 完整生成测试
    // ================================================================

    @Test
    void testGenerateAllEmpty() {
        String result = generator.generateAll();
        assertEquals("", result);
    }

    @Test
    void testGenerateAllComplete() {
        // 设置完整的 FFI 绑定表
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        ExternConstant ok = new ExternConstant("SQLITE_OK", "Int", "0");
        table.getAllConstants().put("SQLITE_OK", ok);

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        func.libraryName = "sqlite3";
        table.getAllFunctions().put("sqlite3_open", func);

        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        table.getAllStructs().add(point);

        String result = generator.generateAll();

        // 验证包含所有部分
        assertTrue(result.contains("import ctypes"));
        assertTrue(result.contains("import claw_runtime"));
        assertTrue(result.contains("def _claw_is_platform"));
        assertTrue(result.contains("_lib_sqlite3 = ctypes.CDLL"));
        assertTrue(result.contains("SQLITE_OK = 0"));
        assertTrue(result.contains("class Point(ctypes.Structure):"));
        assertTrue(result.contains("_lib_sqlite3.sqlite3_open.argtypes"));
        assertTrue(result.contains("def _claw_ffi_sqlite3_open("));
    }

    @Test
    void testGenerateAllWithPlatformConstraints() {
        ExternBlock block = table.newExternBlock();
        block.platform = new PlatformConstraint().addPlatform("windows");
        block.links.add(new LinkDirective("kernel32", "windows.h"));
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("code", "UInt32"));
        ExternFunction func = new ExternFunction("GetLastError", params, "UInt32", false);
        func.libraryName = "kernel32";
        block.functions.add(func);
        table.getExternBlocks().add(block);
        table.indexBlock(block);

        String result = generator.generateAll();

        assertTrue(result.contains("def _claw_is_platform"));
        assertTrue(result.contains("if _claw_is_platform(\"windows\"):"));
        assertTrue(result.contains("_lib_kernel32 = ctypes.CDLL"));
    }

    // ================================================================
    //  14. 多库解析测试
    // ================================================================

    @Test
    void testResolveLibraryVarFromFunction() {
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllLinks().add(new LinkDirective("curl", "curl.h"));

        ExternFunction sqliteFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("f", "String")), "Int", false);
        sqliteFunc.libraryName = "sqlite3";

        // 通过反射调用私有方法 resolveLibraryVar
        String result = generator.getLibraryVarName(sqliteFunc.libraryName);
        assertEquals("_lib_sqlite3", result);
    }

    @Test
    void testGenerateBlockLoadingCode() {
        ExternBlock block = table.newExternBlock();
        block.links.add(new LinkDirective("testlib", "testlib.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("x", "Int"));
        ExternFunction func = new ExternFunction("test_func", params, "Int", false);
        func.libraryName = "testlib";
        block.functions.add(func);

        String result = generator.generateBlockLoadingCode(block, "");

        assertTrue(result.contains("_lib_testlib = ctypes.CDLL"));
        assertTrue(result.contains("_lib_testlib.test_func.argtypes"));
        assertTrue(result.contains("_lib_testlib.test_func.restype"));
    }
}

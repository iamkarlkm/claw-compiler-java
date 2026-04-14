package claw.compiler.generators.ffi;

import claw.compiler.generators.ffi.FFIBindingTable.ExternConstant;
import claw.compiler.generators.ffi.FFIBindingTable.ExternFunction;
import claw.compiler.generators.ffi.FFIBindingTable.ExternParam;
import claw.compiler.generators.ffi.FFIBindingTable.LinkDirective;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonFFIGenerator 测试类
 *
 * 测试 Python ctypes 绑定代码生成器的所有功能
 */
public class PythonFFIGeneratorTest {

    @Test
    public void testGenerateImports() {
        FFIBindingTable table = new FFIBindingTable();

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generateImports();

        assertNotNull(output);
        assertTrue(output.contains("import ctypes"));
        assertTrue(output.contains("import ctypes.util"));
        assertTrue(output.contains("# ========== FFI Imports =========="));
    }

    @Test
    public void testGenerateRuntimeImports() {
        FFIBindingTable table = new FFIBindingTable();

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generateRuntimeImports();

        assertNotNull(output);
        assertTrue(output.contains("import claw_runtime"));
        assertTrue(output.contains("# ========== Runtime Imports =========="));
    }

    @Test
    public void testGenerateLibraryLoading() {
        FFIBindingTable table = new FFIBindingTable();

        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllLinks().add(new LinkDirective("curl", "curl/curl.h"));

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generateLibraryLoading();

        assertNotNull(output);
        assertTrue(output.contains("_lib_path_sqlite3"));
        assertTrue(output.contains("ctypes.util.find_library(\"sqlite3\")"));
        assertTrue(output.contains("Cannot find library: sqlite3"));
        assertTrue(output.contains("_lib_sqlite3 = ctypes.CDLL"));
        assertTrue(output.contains("_lib_path_curl"));
        assertTrue(output.contains("_lib_curl = ctypes.CDLL"));
    }

    @Test
    public void testGetLibraryVarName() {
        FFIBindingTable table = new FFIBindingTable();

        PythonFFIGenerator generator = new PythonFFIGenerator(table);

        assertEquals("_lib_sqlite3", generator.getLibraryVarName("sqlite3"));
        assertEquals("_lib_curl_easy", generator.getLibraryVarName("curl_easy"));
        assertEquals("_lib_openssl", generator.getLibraryVarName("openssl"));
    }

    @Test
    public void testGenerateConstants() {
        FFIBindingTable table = new FFIBindingTable();

        ExternConstant curleOk = new ExternConstant();
        curleOk.name = "CURLE_OK";
        curleOk.value = "0";

        ExternConstant curleNotImplemented = new ExternConstant();
        curleNotImplemented.name = "CURLE_NOT_IMPLEMENTED";
        curleNotImplemented.value = "42";

        table.getAllConstants().put("CURLE_OK", curleOk);
        table.getAllConstants().put("CURLE_NOT_IMPLEMENTED", curleNotImplemented);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generateConstants();

        assertNotNull(output);
        assertTrue(output.contains("CURLE_OK = 0"));
        assertTrue(output.contains("CURLE_NOT_IMPLEMENTED = 42"));
    }

    @Test
    public void testGenerateFunctionBindings() {
        FFIBindingTable table = new FFIBindingTable();

        ExternFunction sqlite3_open = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);

        ExternFunction curl_easy_setopt = new ExternFunction("curl_easy_setopt",
            List.of(new ExternParam("curl", "Pointer"), new ExternParam("option", "Int"), new ExternParam("arg", "Any")),
            "CURLcode", false);

        table.getAllFunctions().put("sqlite3_open", sqlite3_open);
        table.getAllFunctions().put("curl_easy_setopt", curl_easy_setopt);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generateFunctionBindings();

        assertNotNull(output);
        assertTrue(output.contains("_lib_sqlite3.sqlite3_open.argtypes = ["));
        assertTrue(output.contains("ctypes.c_char_p"));
        assertTrue(output.contains("ctypes.POINTER(ctypes.c_void_p)"));
        assertTrue(output.contains("_lib_sqlite3.sqlite3_open.restype = ctypes.c_int"));
        assertTrue(output.contains("_lib_sqlite3.sqlite3_open.argtypes = [ctypes.c_char_p, ctypes.POINTER(ctypes.c_void_p)]"));
    }

    @Test
    public void testGenerateWrapperFunctions() {
        FFIBindingTable table = new FFIBindingTable();

        ExternFunction openFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);
        openFunc.description = "Open a SQLite database connection";

        ExternFunction printFunc = new ExternFunction("printf",
            List.of(new ExternParam("format", "String")),
            "Int", true);

        table.getAllFunctions().put("sqlite3_open", openFunc);
        table.getAllFunctions().put("printf", printFunc);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generateWrapperFunctions();

        assertNotNull(output);
        assertTrue(output.contains("# _claw_ffi_sqlite3_open: wrapper function"));
        assertTrue(output.contains("def _claw_ffi_sqlite3_open("));
        assertTrue(output.contains("filename: str, ppDb: ctypes.POINTER(ctypes.c_void_p) -> int:"));
        assertTrue(output.contains(".encode('utf-8') if isinstance(filename, str) else filename"));
        assertTrue(output.contains("ctypes.byref(ppDb)"));
        assertTrue(output.contains("def _claw_ffi_printf("));
        assertTrue(output.contains("format: str) -> int:"));
    }

    @Test
    public void testGenerateAll() {
        FFIBindingTable table = new FFIBindingTable();

        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllConstants().put("CURLE_OK", new ExternConstant() {{ name = "CURLE_OK"; value = "0"; }});

        ExternFunction openFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String")),
            "Int", false);

        table.getAllFunctions().put("sqlite3_open", openFunc);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generateAll();

        assertNotNull(output);
        assertTrue(output.contains("# ========== FFI Imports =========="));
        assertTrue(output.contains("# ========== Load External C Libraries ==========="));
        assertTrue(output.contains("# ========== External Constants =========="));
        assertTrue(output.contains("# ========== External Function Signatures =========="));
        assertTrue(output.contains("# ========== Python Wrapper Functions =========="));
    }

    @Test
    public void testGeneratePlatformDetection() {
        FFIBindingTable table = new FFIBindingTable();

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generatePlatformDetection();

        assertNotNull(output);
        assertTrue(output.contains("# ========== Platform Detection =========="));
        assertTrue(output.contains("import sys"));
        assertTrue(output.contains("import platform as _platform_mod"));
        assertTrue(output.contains("_CLAW_PLATFORM"));
        assertTrue(output.contains("_claw_is_platform("));
        assertTrue(output.contains("_claw_is_arch("));
    }

    @Test
    public void testGeneratePlatformConditionalLoading() {
        FFIBindingTable table = new FFIBindingTable();

        FFIBindingTable.ExternBlock block = table.newExternBlock();
        block.comment = "Windows-specific API";

        FFIBindingTable.PlatformConstraint constraint = new FFIBindingTable.PlatformConstraint();
        constraint.addPlatform("windows");
        block.platform = constraint;

        table.getExternBlocks().add(block);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generatePlatformConditionalLoading(table);

        assertNotNull(output);
        assertTrue(output.contains("_claw_is_platform(\"windows\")"));
        assertTrue(output.contains("Windows-specific API"));
        assertTrue(output.contains("ctypes.CDLL("));
    }

    @Test
    public void testGenerateBlockLoadingCode() {
        FFIBindingTable table = new FFIBindingTable();

        FFIBindingTable.ExternBlock block = table.newExternBlock();
        block.comment = "Test block";
        block.comment = "Test block";

        ExternFunction func = new ExternFunction("test_func",
            List.of(new ExternParam("arg1", "Int"), new ExternParam("arg2", "Float")),
            "Void", false);

        block.functions.add(func);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generateBlockLoadingCode(block, "    ");

        assertNotNull(output);
        assertTrue(output.contains("Test block"));
        assertTrue(output.contains("ctypes.CDLL("));
        assertTrue(output.contains("test_func.argtypes = ["));
        assertTrue(output.contains("ctypes.c_int"));
        assertTrue(output.contains("ctypes.c_double"));
        assertTrue(output.contains("test_func.restype = None"));
    }

    @Test
    public void testGeneratePythonPlatformIf() {
        FFIBindingTable table = new FFIBindingTable();

        FFIBindingTable.PlatformConstraint constraint = new FFIBindingTable.PlatformConstraint();
        constraint.addPlatform("windows");
        constraint.addPlatform("linux");
        constraint.addArchitectures("x86_64");
        constraint.addArchitectures("arm64");

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generatePythonPlatformIf(constraint);

        assertNotNull(output);
        assertTrue(output.contains("if "));
        assertTrue(output.contains("_claw_is_platform(\"windows\", \"linux\")"));
        assertTrue(output.contains("and _claw_is_arch(\"x86_64\", \"arm64\")"));
    }

    @Test
    public void testMapClawTypeToCtype() {
        // 测试基本类型
        assertEquals("None", PythonFFIGenerator.mapClawTypeToCtype("Void"));
        assertEquals("ctypes.c_int", PythonFFIGenerator.mapClawTypeToCtype("Int"));
        assertEquals("ctypes.c_double", PythonFFIGenerator.mapClawTypeToCtype("Float"));
        assertEquals("ctypes.c_char_p", PythonFFIGenerator.mapClawTypeToCtype("String"));
        assertEquals("ctypes.c_bool", PythonFFIGenerator.mapClawTypeToCtype("Bool"));
        assertEquals("ctypes.c_void_p", PythonFFIGenerator.mapClawTypeToCtype("Any"));

        // 测试 FFI 类型
        assertEquals("ctypes.c_void_p", PythonFFIGenerator.mapClawTypeToCtype("Pointer"));
        assertEquals("ctypes.c_void_p", PythonFFIGenerator.mapClawTypeToCtype("OpaquePointer"));

        // 测试整数类型
        assertEquals("ctypes.c_int8", PythonFFIGenerator.mapClawTypeToCtype("Int8"));
        assertEquals("ctypes.c_uint32", PythonFFIGenerator.mapClawTypeToCtype("UInt32"));

        // 测试泛型类型
        assertEquals("ctypes.POINTER(ctypes.c_int)", PythonFFIGenerator.mapClawTypeToCtype("Ref<Int>"));
        assertEquals("ctypes.POINTER(ctypes.c_double)", PythonFFIGenerator.mapClawTypeToCtype("Ref<Float>"));

        // 测试 null
        assertEquals("None", PythonFFIGenerator.mapClawTypeToCtype(null));
    }

    @Test
    public void testMapClawTypeToPythonTypeHint() {
        assertEquals("None", PythonFFIGenerator.mapClawTypeToPythonTypeHint("Void"));
        assertEquals("int", PythonFFIGenerator.mapClawTypeToPythonTypeHint("Int"));
        assertEquals("float", PythonFFIGenerator.mapClawTypeToPythonTypeHint("Float"));
        assertEquals("str", PythonFFIGenerator.mapClawTypeToPythonTypeHint("String"));
        assertEquals("str", PythonFFIGenerator.mapClawTypeToPythonTypeHint("CString"));
        assertEquals("bool", PythonFFIGenerator.mapClawTypeToPythonTypeHint("Bool"));
        assertEquals(null, PythonFFIGenerator.mapClawTypeToPythonTypeHint("Pointer"));
    }

    @Test
    public void testToSafePythonName() {
        PythonFFIGenerator generator = new PythonFFIGenerator(new FFIBindingTable());

        assertEquals("sqlite3", generator.toSafePythonName("sqlite3"));
        assertEquals("curl_easy", generator.toSafePythonName("curl_easy"));
        assertEquals("openssl_1_1_1", generator.toSafePythonName("openssl_1_1_1"));
        assertEquals("my_lib_name", generator.toSafePythonName("my-lib-name"));
    }

    @Test
    public void testGetLibraryFileName() {
        PythonFFIGenerator generator = new PythonFFIGenerator(new FFIBindingTable());

        assertEquals("sqlite3", generator.getLibraryFileName("sqlite3"));
        assertEquals("curl_easy", generator.getLibraryFileName("curl_easy"));
    }

    @Test
    public void testCompleteExample() {
        FFIBindingTable table = new FFIBindingTable();

        // 添加头文件和链接
        table.getAllIncludes().add("sqlite3.h");
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        // 添加常量
        ExternConstant ok = new ExternConstant();
        ok.name = "SQLITE_OK";
        ok.value = "0";
        table.getAllConstants().put("SQLITE_OK", ok);

        // 添加函数
        ExternFunction openFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);
        openFunc.description = "Open SQLite database";
        openFunc.threadSafety = FFIBindingTable.ThreadSafety.THREAD_SAFE;
        table.getAllFunctions().put("sqlite3_open", openFunc);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generateAll();

        assertNotNull(output);
        assertTrue(output.contains("# ========== FFI Imports =========="));
        assertTrue(output.contains("import ctypes"));
        assertTrue(output.contains("import ctypes.util"));
        assertTrue(output.contains("# ========== Load External C Libraries ==========="));
        assertTrue(output.contains("ctypes.util.find_library(\"sqlite3\")"));
        assertTrue(output.contains("# ========== External Constants =========="));
        assertTrue(output.contains("SQLITE_OK = 0"));
        assertTrue(output.contains("# ========== External Function Signatures =========="));
        assertTrue(output.contains("_lib_sqlite3.sqlite3_open.argtypes = [ctypes.c_char_p, ctypes.POINTER(ctypes.c_void_p)]"));
        assertTrue(output.contains("# ========== Python Wrapper Functions =========="));
        assertTrue(output.contains("def _claw_ffi_sqlite3_open("));
    }

    @Test
    public void testWrapperForStringParameters() {
        FFIBindingTable table = new FFIBindingTable();

        ExternFunction strcatFunc = new ExternFunction("strcat",
            List.of(new ExternParam("dest", "String"), new ExternParam("src", "String")),
            "String", false);

        table.getAllFunctions().put("strcat", strcatFunc);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generateWrapperFunctions();

        assertNotNull(output);
        assertTrue(output.contains("def _claw_ffi_strcat("));
        assertTrue(output.contains("dest: str, src: str) -> str:"));
        assertTrue(output.contains("dest.encode('utf-8')"));
        assertTrue(output.contains("src.encode('utf-8')"));
    }

    @Test
    public void testWrapperForRefParameters() {
        FFIBindingTable table = new FFIBindingTable();

        ExternFunction strlenFunc = new ExternFunction("strlen",
            List.of(new ExternParam("str", "String")),
            "SizeT", false);

        table.getAllFunctions().put("strlen", strlenFunc);

        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String output = generator.generateWrapperFunctions();

        assertNotNull(output);
        assertTrue(output.contains("def _claw_ffi_strlen("));
        assertTrue(output.contains("str: str) -> int:"));
        assertTrue(output.contains("ctypes.byref(str)"));
    }
}

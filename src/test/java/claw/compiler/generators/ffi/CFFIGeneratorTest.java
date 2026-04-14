package claw.compiler.generators.ffi;

import claw.compiler.generators.ffi.FFIBindingTable.ExternConstant;
import claw.compiler.generators.ffi.FFIBindingTable.ExternFunction;
import claw.compiler.generators.ffi.FFIBindingTable.ExternParam;
import claw.compiler.generators.ffi.FFIBindingTable.ExternType;
import claw.compiler.generators.ffi.FFIBindingTable.LinkDirective;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CFFIGenerator 测试类
 *
 * 测试 C 目标 FFI 代码生成器的所有功能
 */
public class CFFIGeneratorTest {

    @Test
    public void testGenerateIncludes() {
        FFIBindingTable table = new FFIBindingTable();

        // 添加一些头文件
        table.getAllIncludes().add("stdio.h");
        table.getAllIncludes().add("curl/curl.h");
        table.getAllIncludes().add("sqlite3.h");

        CFFIGenerator generator = new CFFIGenerator(table);
        String output = generator.generateIncludes();

        assertNotNull(output);
        assertTrue(output.contains("#include <stdio.h>"));
        assertTrue(output.contains("#include <curl/curl.h>"));
        assertTrue(output.contains("#include <sqlite3.h>"));
        assertTrue(output.contains("/* ========== External Library Headers (FFI) ========== */"));
    }

    @Test
    public void testGenerateTypeDefinitions() {
        FFIBindingTable table = new FFIBindingTable();

        // 添加一些类型
        ExternType type1 = new ExternType("sqlite3", "OpaquePointer");
        ExternType type2 = new ExternType("CURL", "OpaquePointer");
        type2.description = "libcurl handle type";

        table.getAllTypes().put("sqlite3", type1);
        table.getAllTypes().put("CURL", type2);

        CFFIGenerator generator = new CFFIGenerator(table);
        String output = generator.generateTypeDefinitions();

        assertNotNull(output);
        assertTrue(output.contains("/* ========== External Type Declarations (FFI) ========== */"));
        assertTrue(output.contains("_CLAW_EXTERN_TYPE_SQLITE3_"));
        assertTrue(output.contains("/* Opaque type - defined in external library */"));
        assertTrue(output.contains("sqlite3 = OpaquePointer"));
    }

    @Test
    public void testGenerateConstants() {
        FFIBindingTable table = new FFIBindingTable();

        // 添加一些常量
        ExternConstant const1 = new ExternConstant();
        const1.name = "CURLE_OK";
        const1.value = "0";

        ExternConstant const2 = new ExternConstant();
        const2.name = "CURLE_NOT_IMPLEMENTED";
        const2.value = "42";

        table.getAllConstants().put("CURLE_OK", const1);
        table.getAllConstants().put("CURLE_NOT_IMPLEMENTED", const2);

        CFFIGenerator generator = new CFFIGenerator(table);
        String output = generator.generateConstants();

        assertNotNull(output);
        assertTrue(output.contains("#define CURLE_OK (0)"));
        assertTrue(output.contains("#define CURLE_NOT_IMPLEMENTED (42)"));
    }

    @Test
    public void testGenerateFunctionDeclarations() {
        FFIBindingTable table = new FFIBindingTable();

        // 添加一些函数
        ExternFunction func1 = new ExternFunction("sqlite3_open", List.of(
            new ExternParam("filename", "String"),
            new ExternParam("ppDb", "Pointer")
        ), "Int", false);

        ExternFunction func2 = new ExternFunction("curl_easy_setopt", List.of(
            new ExternParam("curl", "Pointer"),
            new ExternParam("option", "Int"),
            new ExternParam("arg", "Any")
        ), "CURLcode", false);

        ExternFunction func3 = new ExternFunction("printf", List.of(
            new ExternParam("format", "String")
        ), "Int", true);

        table.getAllFunctions().put("sqlite3_open", func1);
        table.getAllFunctions().put("curl_easy_setopt", func2);
        table.getAllFunctions().put("printf", func3);

        CFFIGenerator generator = new CFFIGenerator(table);
        String output = generator.generateFunctionDeclarations();

        assertNotNull(output);
        assertTrue(output.contains("extern int sqlite3_open("));
        assertTrue(output.contains("extern CURLcode curl_easy_setopt("));
        assertTrue(output.contains("extern int printf("));
        assertTrue(output.contains("const char* format"));
    }

    @Test
    public void testGenerateBlockCode() {
        FFIBindingTable table = new FFIBindingTable();

        // 创建一个完整的 extern 块
        FFIBindingTable.ExternBlock block = table.newExternBlock();
        block.comment = "SQLite3 database library bindings";
        block.comment = "SQLite3 database library bindings";

        // 添加类型
        ExternType sqlite3Type = new ExternType("sqlite3", "OpaquePointer");
        block.types.add(sqlite3Type);

        // 添加函数
        ExternFunction openFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);
        openFunc.description = "Open a SQLite database connection";
        openFunc.threadSafety = FFIBindingTable.ThreadSafety.THREAD_SAFE;
        block.functions.add(openFunc);

        ExternFunction closeFunc = new ExternFunction("sqlite3_close",
            List.of(new ExternParam("db", "Pointer")),
            "Int", false);
        block.functions.add(closeFunc);

        // 添加常量
        ExternConstant okConst = new ExternConstant();
        okConst.name = "SQLITE_OK";
        okConst.value = "0";
        block.constants.add(okConst);

        CFFIGenerator generator = new CFFIGenerator(table);
        String output = generator.generateBlockCode(block);

        assertNotNull(output);
        assertTrue(output.contains("/* ========== External Type Definitions ========== */"));
        assertTrue(output.contains("/* SQLite3 database library bindings */"));
        assertTrue(output.contains("extern int sqlite3_open("));
        assertTrue(output.contains("/* sqlite3_open - Open a SQLite database connection - thread safety: THREAD_SAFE */"));
        assertTrue(output.contains("extern int sqlite3_close("));
        assertTrue(output.contains("#define SQLITE_OK (0)"));
    }

    @Test
    public void testGenerateLinkFlags() {
        FFIBindingTable table = new FFIBindingTable();

        // 添加链接指令
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllLinks().add(new LinkDirective("curl", "curl/curl.h"));

        CFFIGenerator generator = new CFFIGenerator(table);
        String output = generator.generateLinkFlags();

        assertNotNull(output);
        assertTrue(output.contains("-lsqlite3"));
        assertTrue(output.contains("-lcurl"));
    }

    @Test
    public void testGenerateBuildCommand() {
        FFIBindingTable table = new FFIBindingTable();
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        CFFIGenerator generator = new CFFIGenerator(table);
        String output = generator.generateBuildCommand("example.c", "example");

        assertNotNull(output);
        assertTrue(output.contains("gcc"));
        assertTrue(output.contains("example.c"));
        assertTrue(output.contains("-lsqlite3"));
        assertTrue(output.contains("-o example"));
    }

    @Test
    public void testPlatformGuardedCode() {
        FFIBindingTable table = new FFIBindingTable();

        // 创建一个带有平台约束的块
        FFIBindingTable.ExternBlock block = table.newExternBlock();
        block.comment = "Windows-specific API";

        // 创建平台约束
        FFIBindingTable.PlatformConstraint constraint = new FFIBindingTable.PlatformConstraint();
        constraint.addPlatform("windows");

        block.platform = constraint;

        CFFIGenerator generator = new CFFIGenerator(table);
        String output = generator.generatePlatformGuardedCode(table);

        assertNotNull(output);
        assertTrue(output.contains("#if"));
        assertTrue(output.contains("CLAW_PLATFORM_WINDOWS"));
        assertTrue(output.contains("/* Windows-specific API */"));
    }

    @Test
    public void testComplexBlockWithAllElements() {
        FFIBindingTable table = new FFIBindingTable();

        FFIBindingTable.ExternBlock block = table.newExternBlock();
        block.comment = "Complete library bindings example";

        // 类型
        ExternType handleType = new ExternType("MYLIB_HANDLE", "OpaquePointer");
        block.types.add(handleType);

        // 函数
        ExternFunction createFunc = new ExternFunction("mylib_create",
            List.of(new ExternParam("config", "String")),
            "MYLIB_HANDLE", false);
        createFunc.description = "Create a new library instance";
        block.functions.add(createFunc);

        // 常量
        ExternConstant MAX_SIZE = new ExternConstant();
        MAX_SIZE.name = "MYLIB_MAX_SIZE";
        MAX_SIZE.value = "1024";
        block.constants.add(MAX_SIZE);

        // 结构体
        FFIBindingTable.ExternStruct struct = new FFIBindingTable.ExternStruct();
        struct.name = "MYLIB_CONFIG";
        struct.fields.add(new FFIBindingTable.StructField("timeout", "Int"));
        struct.fields.add(new FFIBindingTable.StructField("debug", "Bool"));
        block.structs.add(struct);

        // 枚举
        FFIBindingTable.ExternEnum enumDecl = new FFIBindingTable.ExternEnum();
        enumDecl.name = "MYLIB_STATUS";
        enumDecl.members.add(new FFIBindingTable.EnumMember("SUCCESS", "0", "Operation successful"));
        enumDecl.members.add(new FFIBindingTable.EnumMember("ERROR", "-1", "General error"));
        block.enums.add(enumDecl);

        // 回调
        FFIBindingTable.ExternCallback callback = new FFIBindingTable.ExternCallback();
        callback.name = "MYLIB_CALLBACK";
        callback.returnType = "Void";
        callback.params.add(new ExternParam("data", "Pointer"));
        block.callbacks.add(callback);

        // 宏
        FFIBindingTable.ExternMacro macro = new FFIBindingTable.ExternMacro();
        macro.name = "MYLIB_VERSION";
        macro.value = "\"1.0.0\"";
        block.macros.add(macro);

        CFFIGenerator generator = new CFFIGenerator(table);
        String output = generator.generateBlockCode(block);

        assertNotNull(output);
        assertTrue(output.contains("/* ========== External Type Definitions ========== */"));
        assertTrue(output.contains("/* ========== Function Declarations ========== */"));
        assertTrue(output.contains("/* ========== Constant Declarations ========== */"));
        assertTrue(output.contains("/* ========== Struct Definitions ========== */"));
        assertTrue(output.contains("typedef struct MYLIB_CONFIG {"));
        assertTrue(output.contains("  int timeout /* Timeout in seconds */;"));
        assertTrue(output.contains("  bool debug /* Debug mode */;"));
        assertTrue(output.contains("} MYLIB_CONFIG;"));
        assertTrue(output.contains("/* ========== Enum Definitions ========== */"));
        assertTrue(output.contains("enum MYLIB_STATUS {"));
        assertTrue(output.contains("  SUCCESS = 0 /* Operation successful */"));
        assertTrue(output.contains("} MYLIB_STATUS;"));
        assertTrue(output.contains("/* ========== Callback Type Definitions ========== */"));
        assertTrue(output.contains("typedef void (*MYLIB_CALLBACK)("));
        assertTrue(output.contains("/* ========== Macro Definitions ========== */"));
        assertTrue(output.contains("#define MYLIB_VERSION \"1.0.0\""));
        assertTrue(output.contains("/* link"));
    }

    @Test
    public void testMapClawFFITypeToCType() {
        // 测试基本类型映射
        assertEquals("void", CFFIGenerator.mapClawFFITypeToCType("Void"));
        assertEquals("int", CFFIGenerator.mapClawFFITypeToCType("Int"));
        assertEquals("double", CFFIGenerator.mapClawFFITypeToCType("Float"));
        assertEquals("const char*", CFFIGenerator.mapClawFFITypeToCType("String"));
        assertEquals("void*", CFFIGenerator.mapClawFFITypeToCType("Any"));

        // 测试 FFI 类型
        assertEquals("void*", CFFIGenerator.mapClawFFITypeToCType("Pointer"));
        assertEquals("void*", CFFIGenerator.mapClawFFITypeToCType("OpaquePointer"));

        // 测试整数类型
        assertEquals("int8_t", CFFIGenerator.mapClawFFITypeToCType("Int8"));
        assertEquals("uint32_t", CFFIGenerator.mapClawFFITypeToCType("UInt32"));

        // 测试泛型类型
        assertEquals("int*", CFFIGenerator.mapClawFFITypeToCType("Ref<Int>"));
        assertEquals("double*", CFFIGenerator.mapClawFFITypeToCType("Ref<Float>"));

        // 测试 null 类型
        assertEquals("void", CFFIGenerator.mapClawFFITypeToCType(null));
    }

    @Test
    public void testDeprecatedFunctionDeclaration() {
        FFIBindingTable table = new FFIBindingTable();

        ExternFunction func = new ExternFunction("old_api", List.of(), "Int", false);
        func.deprecated = true;
        func.deprecatedAlt = "new_api";
        func.description = "Old deprecated API";

        table.getAllFunctions().put("old_api", func);

        CFFIGenerator generator = new CFFIGenerator(table);
        String output = generator.generateFunctionDeclarations();

        assertNotNull(output);
        assertTrue(output.contains("/* old_api - deprecated, use new_api */"));
        assertTrue(output.contains("/* old_api - Old deprecated API - thread safety: UNKNOWN */"));
    }
}

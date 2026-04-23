package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CFFIGenerator 手动测试运行器
 */
public class CFFIGeneratorManualTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("CFFIGenerator 手动测试套件");
        System.out.println("========================================\n");

        testGenerateIncludes();
        testGeneratePlatformDetection();
        testGenerateTypeDefinitions();
        testGenerateFunctionDeclarations();
        testGenerateConstants();
        testGenerateStructDefinitions();
        testGenerateEnumDefinitions();
        testGenerateCallbackDefinitions();
        testGenerateLinkDirectives();
        testMapClawFFITypeToCType();
        testMapClawFFITypeToCTypeRef();
        testGenerateAllComplete();
        testDeprecatedFunction();
        testVariadicFunction();

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

    static void testGenerateIncludes() {
        System.out.println("\n[Test] generateIncludes");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.includes.add("sqlite3.h");
        block.includes.add("curl/curl.h");
        table.indexBlock(block);

        CFFIGenerator generator = new CFFIGenerator(table);
        String result = generator.generateIncludes();
        assertTrue(result.contains("#include <sqlite3.h>"), "sqlite3 头文件");
        assertTrue(result.contains("#include <curl/curl.h>"), "curl 头文件");
    }

    static void testGeneratePlatformDetection() {
        System.out.println("\n[Test] generatePlatformDetection");
        FFIBindingTable table = new FFIBindingTable();
        CFFIGenerator generator = new CFFIGenerator(table);
        String result = generator.generatePlatformDetection();
        assertTrue(result.contains("CLAW_PLATFORM_WINDOWS"), "Windows 平台宏");
        assertTrue(result.contains("CLAW_PLATFORM_LINUX"), "Linux 平台宏");
        assertTrue(result.contains("CLAW_PLATFORM_MACOS"), "macOS 平台宏");
        assertTrue(result.contains("CLAW_ARCH_X86_64"), "x86_64 架构宏");
    }

    static void testGenerateTypeDefinitions() {
        System.out.println("\n[Test] generateTypeDefinitions");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        ExternType type = new ExternType("sqlite3", "OpaquePointer");
        block.types.add(type);
        table.indexBlock(block);

        CFFIGenerator generator = new CFFIGenerator(table);
        String result = generator.generateTypeDefinitions();
        assertTrue(result.contains("#ifndef _CLAW_EXTERN_TYPE_SQLITE3_"), "条件编译保护");
        assertTrue(result.contains("Opaque type - defined in external library"), "不透明类型注释");
    }

    static void testGenerateFunctionDeclarations() {
        System.out.println("\n[Test] generateFunctionDeclarations");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.links.add(new LinkDirective("sqlite3", "sqlite3.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        func.description = "Open a SQLite database";
        func.threadSafety = ThreadSafety.THREAD_SAFE;
        block.functions.add(func);
        table.indexBlock(block);

        CFFIGenerator generator = new CFFIGenerator(table);
        String result = generator.generateFunctionDeclarations();
        if (!result.contains("extern int sqlite3_open(const char* filename, void* ppDb);")) {
            System.out.println("  实际输出:\n" + result);
        }
        assertTrue(result.contains("extern int sqlite3_open(const char* filename, void* ppDb);"), "函数声明");
        assertTrue(result.contains("Open a SQLite database"), "函数描述");
        assertTrue(result.contains("THREAD_SAFE"), "线程安全标记");
    }

    static void testGenerateConstants() {
        System.out.println("\n[Test] generateConstants");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.constants.add(new ExternConstant("SQLITE_OK", "Int", "0"));
        block.constants.add(new ExternConstant("SQLITE_ROW", "Int", "100"));
        table.indexBlock(block);

        CFFIGenerator generator = new CFFIGenerator(table);
        String result = generator.generateConstants();
        assertTrue(result.contains("#ifndef SQLITE_OK"), "条件编译保护");
        assertTrue(result.contains("#define SQLITE_OK (0)"), "常量定义");
        assertTrue(result.contains("#define SQLITE_ROW (100)"), "常量定义 2");
    }

    static void testGenerateStructDefinitions() {
        System.out.println("\n[Test] generateStructDefinitions");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        block.structs.add(point);
        table.indexBlock(block);

        CFFIGenerator generator = new CFFIGenerator(table);
        String result = generator.generateStructDefinitions();
        assertTrue(result.contains("typedef struct Point {"), "结构体定义");
        assertTrue(result.contains("double x;"), "Float 字段");
        assertTrue(result.contains("} Point;"), "typedef 结尾");
    }

    static void testGenerateEnumDefinitions() {
        System.out.println("\n[Test] generateEnumDefinitions");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        ExternEnum status = new ExternEnum("Status");
        status.members.add(new EnumMember("OK", "0"));
        status.members.add(new EnumMember("ERROR", "-1"));
        block.enums.add(status);
        table.indexBlock(block);

        CFFIGenerator generator = new CFFIGenerator(table);
        String result = generator.generateEnumDefinitions();
        assertTrue(result.contains("typedef enum Status {"), "枚举定义");
        assertTrue(result.contains("OK = 0,"), "枚举成员");
        assertTrue(result.contains("ERROR = -1"), "枚举成员 2");
        assertTrue(result.contains("} Status;"), "typedef 结尾");
    }

    static void testGenerateCallbackDefinitions() {
        System.out.println("\n[Test] generateCallbackDefinitions");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("data", "Pointer"));
        ExternCallback cb = new ExternCallback("Callback", params, "Void");
        block.callbacks.add(cb);
        table.indexBlock(block);

        CFFIGenerator generator = new CFFIGenerator(table);
        String result = generator.generateCallbackDefinitions();
        if (!result.contains("typedef void (*Callback)(void* data);")) {
            System.out.println("  实际输出:\n" + result);
        }
        assertTrue(result.contains("typedef void (*Callback)(void* data);"), "函数指针 typedef");
    }

    static void testGenerateLinkDirectives() {
        System.out.println("\n[Test] generateLinkDirectives");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.links.add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.indexBlock(block);

        CFFIGenerator generator = new CFFIGenerator(table);
        String result = generator.generateLinkDirectives();
        assertTrue(result.contains("link \"sqlite3\""), "链接指令注释");
    }

    static void testMapClawFFITypeToCType() {
        System.out.println("\n[Test] mapClawFFITypeToCType");
        assertEquals("void", CFFIGenerator.mapClawFFITypeToCType("Void"), "Void");
        assertEquals("int", CFFIGenerator.mapClawFFITypeToCType("Int"), "Int");
        assertEquals("double", CFFIGenerator.mapClawFFITypeToCType("Float"), "Float");
        assertEquals("const char*", CFFIGenerator.mapClawFFITypeToCType("String"), "String");
        assertEquals("bool", CFFIGenerator.mapClawFFITypeToCType("Bool"), "Bool");
        assertEquals("void*", CFFIGenerator.mapClawFFITypeToCType("Pointer"), "Pointer");
        assertEquals("size_t", CFFIGenerator.mapClawFFITypeToCType("SizeT"), "SizeT");
        assertEquals("int32_t", CFFIGenerator.mapClawFFITypeToCType("Int32"), "Int32");
        assertEquals("uint64_t", CFFIGenerator.mapClawFFITypeToCType("UInt64"), "UInt64");
    }

    static void testMapClawFFITypeToCTypeRef() {
        System.out.println("\n[Test] mapClawFFITypeToCTypeRef");
        assertEquals("int*", CFFIGenerator.mapClawFFITypeToCType("Ref<Int>"), "Ref<Int>");
        assertEquals("double*", CFFIGenerator.mapClawFFITypeToCType("Ref<Float>"), "Ref<Float>");
        assertEquals("int*", CFFIGenerator.mapClawFFITypeToCType("CArray<Int>"), "CArray<Int>");
    }

    static void testGenerateAllComplete() {
        System.out.println("\n[Test] generateAllComplete");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        block.includes.add("sqlite3.h");
        block.links.add(new LinkDirective("sqlite3", "sqlite3.h"));
        block.constants.add(new ExternConstant("SQLITE_OK", "Int", "0"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        block.functions.add(func);

        ExternStruct config = new ExternStruct("Config");
        config.fields.add(new StructField("timeout", "Int"));
        block.structs.add(config);

        ExternEnum mode = new ExternEnum("Mode");
        mode.members.add(new EnumMember("READ", "0"));
        block.enums.add(mode);

        table.indexBlock(block);

        CFFIGenerator generator = new CFFIGenerator(table);
        String result = generator.generateAll();
        if (!result.contains("extern int sqlite3_open")) {
            System.out.println("  实际输出片段:\n" + result.substring(0, Math.min(result.length(), 500)));
        }
        assertTrue(result.contains("#include <sqlite3.h>"), "包含头文件");
        assertTrue(result.contains("CLAW_PLATFORM_WINDOWS"), "包含平台检测");
        assertTrue(result.contains("extern int sqlite3_open"), "包含函数声明");
        assertTrue(result.contains("#define SQLITE_OK (0)"), "包含常量");
        assertTrue(result.contains("typedef struct Config"), "包含结构体");
        assertTrue(result.contains("typedef enum Mode"), "包含枚举");
    }

    static void testDeprecatedFunction() {
        System.out.println("\n[Test] deprecatedFunction");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        ExternFunction func = new ExternFunction("old_api", Collections.emptyList(), "Int", false);
        func.deprecated = true;
        func.deprecatedAlt = "new_api";
        block.functions.add(func);
        table.indexBlock(block);

        CFFIGenerator generator = new CFFIGenerator(table);
        String result = generator.generateFunctionDeclarations();
        assertTrue(result.contains("/* DEPRECATED */"), "废弃标记");
        assertTrue(result.contains("Use new_api instead"), "替代建议");
    }

    static void testVariadicFunction() {
        System.out.println("\n[Test] variadicFunction");
        FFIBindingTable table = new FFIBindingTable();
        ExternBlock block = addBlock(table);
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("fmt", "String"));
        ExternFunction func = new ExternFunction("printf", params, "Int", true);
        block.functions.add(func);
        table.indexBlock(block);

        CFFIGenerator generator = new CFFIGenerator(table);
        String result = generator.generateFunctionDeclarations();
        assertTrue(result.contains("printf(const char* fmt, ...);"), "变参函数声明");
    }
}

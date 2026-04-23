package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CFFIGenerator JUnit test suite.
 *
 * <p>Covers all generation methods: includes, platform detection,
 * type definitions, function declarations, constants, structs, enums, callbacks.</p>
 */
class CFFIGeneratorTest {

    private FFIBindingTable table;
    private CFFIGenerator generator;

    @BeforeEach
    void setUp() {
        table = new FFIBindingTable();
        generator = new CFFIGenerator(table);
    }

    // ================================================================
    //  1. Include generation
    // ================================================================

    @Test
    void testGenerateIncludes() {
        table.getAllIncludes().add("sqlite3.h");
        table.getAllIncludes().add("curl/curl.h");

        String result = generator.generateIncludes();

        assertTrue(result.contains("#include <sqlite3.h>"));
        assertTrue(result.contains("#include <curl/curl.h>"));
        assertTrue(result.contains("External Library Headers"));
    }

    @Test
    void testGenerateIncludesEmpty() {
        assertEquals("", generator.generateIncludes());
    }

    // ================================================================
    //  2. Platform detection
    // ================================================================

    @Test
    void testGeneratePlatformDetection() {
        String result = generator.generatePlatformDetection();

        assertTrue(result.contains("CLAW_PLATFORM_WINDOWS"));
        assertTrue(result.contains("CLAW_PLATFORM_LINUX"));
        assertTrue(result.contains("CLAW_PLATFORM_MACOS"));
        assertTrue(result.contains("CLAW_ARCH_X86_64"));
        assertTrue(result.contains("CLAW_ARCH_ARM64"));
    }

    // ================================================================
    //  3. Type definitions
    // ================================================================

    @Test
    void testGenerateTypeDefinitions() {
        ExternType type = new ExternType("sqlite3", "OpaquePointer");
        table.getAllTypes().put("sqlite3", type);

        String result = generator.generateTypeDefinitions();

        assertTrue(result.contains("#ifndef _CLAW_EXTERN_TYPE_SQLITE3_"));
        assertTrue(result.contains("Opaque type - defined in external library"));
    }

    @Test
    void testGenerateTypeDefinitionsEmpty() {
        assertEquals("", generator.generateTypeDefinitions());
    }

    // ================================================================
    //  4. Function declarations
    // ================================================================

    @Test
    void testGenerateFunctionDeclarations() {
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        func.description = "Open a SQLite database";
        func.threadSafety = ThreadSafety.THREAD_SAFE;
        table.getAllFunctions().put("sqlite3_open", func);

        String result = generator.generateFunctionDeclarations();

        assertTrue(result.contains("extern int sqlite3_open(const char* filename, void* ppDb);"));
        assertTrue(result.contains("Open a SQLite database"));
        assertTrue(result.contains("THREAD_SAFE"));
    }

    @Test
    void testGenerateFunctionDeclarationsEmpty() {
        assertEquals("", generator.generateFunctionDeclarations());
    }

    @Test
    void testDeprecatedFunction() {
        ExternFunction func = new ExternFunction("old_api", Collections.emptyList(), "Int", false);
        func.deprecated = true;
        func.deprecatedAlt = "new_api";
        table.getAllFunctions().put("old_api", func);

        String result = generator.generateFunctionDeclarations();

        assertTrue(result.contains("/* DEPRECATED */"));
        assertTrue(result.contains("Use new_api instead"));
    }

    @Test
    void testVariadicFunction() {
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("fmt", "String"));
        ExternFunction func = new ExternFunction("printf", params, "Int", true);
        table.getAllFunctions().put("printf", func);

        String result = generator.generateFunctionDeclarations();

        assertTrue(result.contains("printf(const char* fmt, ...);"));
    }

    // ================================================================
    //  5. Constants
    // ================================================================

    @Test
    void testGenerateConstants() {
        table.getAllConstants().put("SQLITE_OK", new ExternConstant("SQLITE_OK", "Int", "0"));
        table.getAllConstants().put("SQLITE_ROW", new ExternConstant("SQLITE_ROW", "Int", "100"));

        String result = generator.generateConstants();

        assertTrue(result.contains("#ifndef SQLITE_OK"));
        assertTrue(result.contains("#define SQLITE_OK (0)"));
        assertTrue(result.contains("#define SQLITE_ROW (100)"));
    }

    @Test
    void testGenerateConstantsEmpty() {
        assertEquals("", generator.generateConstants());
    }

    // ================================================================
    //  6. Struct definitions
    // ================================================================

    @Test
    void testGenerateStructDefinitions() {
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        table.getAllStructs().add(point);

        String result = generator.generateStructDefinitions();

        assertTrue(result.contains("typedef struct Point {"));
        assertTrue(result.contains("double x;"));
        assertTrue(result.contains("double y;"));
        assertTrue(result.contains("} Point;"));
    }

    @Test
    void testGenerateStructDefinitionsPacked() {
        ExternStruct packed = new ExternStruct("PackedStruct");
        packed.packed = true;
        packed.alignment = 4;
        packed.fields.add(new StructField("a", "Int8"));
        table.getAllStructs().add(packed);

        String result = generator.generateStructDefinitions();

        assertTrue(result.contains("#pragma pack(push, 1)"));
        assertTrue(result.contains("#pragma pack(pop)"));
    }

    @Test
    void testGenerateStructDefinitionsEmpty() {
        assertEquals("", generator.generateStructDefinitions());
    }

    // ================================================================
    //  7. Enum definitions
    // ================================================================

    @Test
    void testGenerateEnumDefinitions() {
        ExternEnum status = new ExternEnum("Status");
        status.members.add(new EnumMember("OK", "0"));
        status.members.add(new EnumMember("ERROR", "-1"));
        table.getAllEnums().add(status);

        String result = generator.generateEnumDefinitions();

        assertTrue(result.contains("typedef enum Status {"));
        assertTrue(result.contains("OK = 0,"));
        assertTrue(result.contains("ERROR = -1"));
        assertTrue(result.contains("} Status;"));
    }

    @Test
    void testGenerateEnumDefinitionsEmpty() {
        assertEquals("", generator.generateEnumDefinitions());
    }

    // ================================================================
    //  8. Callback definitions
    // ================================================================

    @Test
    void testGenerateCallbackDefinitions() {
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("data", "Pointer"));
        ExternCallback cb = new ExternCallback("Callback", params, "Void");
        table.getAllCallbacks().add(cb);

        String result = generator.generateCallbackDefinitions();

        assertTrue(result.contains("typedef void (*Callback)(void* data);"));
    }

    @Test
    void testGenerateCallbackDefinitionsEmpty() {
        assertEquals("", generator.generateCallbackDefinitions());
    }

    // ================================================================
    //  9. Link directives
    // ================================================================

    @Test
    void testGenerateLinkDirectives() {
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        String result = generator.generateLinkDirectives();

        assertTrue(result.contains("link \"sqlite3\""));
    }

    @Test
    void testGenerateLinkDirectivesEmpty() {
        assertEquals("", generator.generateLinkDirectives());
    }

    // ================================================================
    //  10. Full generation
    // ================================================================

    @Test
    void testGenerateAllEmpty() {
        assertEquals("", generator.generateAll());
    }

    @Test
    void testGenerateAllComplete() {
        table.getAllIncludes().add("sqlite3.h");
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllConstants().put("SQLITE_OK", new ExternConstant("SQLITE_OK", "Int", "0"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        table.getAllFunctions().put("sqlite3_open", func);

        ExternStruct config = new ExternStruct("Config");
        config.fields.add(new StructField("timeout", "Int"));
        table.getAllStructs().add(config);

        ExternEnum mode = new ExternEnum("Mode");
        mode.members.add(new EnumMember("READ", "0"));
        table.getAllEnums().add(mode);

        String result = generator.generateAll();

        assertTrue(result.contains("#include <sqlite3.h>"));
        assertTrue(result.contains("CLAW_PLATFORM_WINDOWS"));
        assertTrue(result.contains("extern int sqlite3_open"));
        assertTrue(result.contains("#define SQLITE_OK (0)"));
        assertTrue(result.contains("typedef struct Config"));
        assertTrue(result.contains("typedef enum Mode"));
        assertTrue(result.contains("link \"sqlite3\""));
    }

    // ================================================================
    //  11. Type mapping
    // ================================================================

    @Test
    void testMapClawFFITypeToCType() {
        assertEquals("void", CFFIGenerator.mapClawFFITypeToCType("Void"));
        assertEquals("int", CFFIGenerator.mapClawFFITypeToCType("Int"));
        assertEquals("double", CFFIGenerator.mapClawFFITypeToCType("Float"));
        assertEquals("const char*", CFFIGenerator.mapClawFFITypeToCType("String"));
        assertEquals("bool", CFFIGenerator.mapClawFFITypeToCType("Bool"));
        assertEquals("void*", CFFIGenerator.mapClawFFITypeToCType("Pointer"));
        assertEquals("size_t", CFFIGenerator.mapClawFFITypeToCType("SizeT"));
        assertEquals("int32_t", CFFIGenerator.mapClawFFITypeToCType("Int32"));
        assertEquals("uint64_t", CFFIGenerator.mapClawFFITypeToCType("UInt64"));
    }

    @Test
    void testMapClawFFITypeToCTypeRef() {
        assertEquals("int*", CFFIGenerator.mapClawFFITypeToCType("Ref<Int>"));
        assertEquals("double*", CFFIGenerator.mapClawFFITypeToCType("Ref<Float>"));
        assertEquals("int*", CFFIGenerator.mapClawFFITypeToCType("CArray<Int>"));
    }
}

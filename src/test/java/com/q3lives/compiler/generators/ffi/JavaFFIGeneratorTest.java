package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JavaFFIGenerator JUnit test suite.
 *
 * <p>Covers all generation methods: package/imports, class header,
 * library loading, constants, method handles, wrapper methods, helpers.
</p>
 */
class JavaFFIGeneratorTest {

    private FFIBindingTable table;
    private JavaFFIGenerator generator;

    @BeforeEach
    void setUp() {
        table = new FFIBindingTable();
        generator = new JavaFFIGenerator(table);
    }

    // ================================================================
    //  1. Package and imports
    // ================================================================

    @Test
    void testGeneratePackageAndImports() {
        String result = generator.generatePackageAndImports();

        assertTrue(result.contains("package claw.ffi;"));
        assertTrue(result.contains("import java.lang.foreign.*;"));
        assertTrue(result.contains("import java.lang.invoke.MethodHandle;"));
    }

    // ================================================================
    //  2. Class header
    // ================================================================

    @Test
    void testGenerateClassHeader() {
        JavaFFIGenerator gen = new JavaFFIGenerator(table, "Sqlite3FFI");
        String result = gen.generateClassHeader();

        assertTrue(result.contains("public final class Sqlite3FFI"));
        assertTrue(result.contains("Auto-generated FFI bindings"));
    }

    // ================================================================
    //  3. Library loading
    // ================================================================

    @Test
    void testGenerateLibraryLoading() {
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        String result = generator.generateLibraryLoading();

        assertTrue(result.contains("private static final Linker LINKER = Linker.nativeLinker();"));
        assertTrue(result.contains("SQLITE3_LOOKUP"));
        assertTrue(result.contains("SymbolLookup.libraryLookup("));
        assertTrue(result.contains("System.mapLibraryName(\"sqlite3\")"));
    }

    @Test
    void testGenerateLibraryLoadingEmpty() {
        assertEquals("", generator.generateLibraryLoading());
    }

    // ================================================================
    //  4. Constants
    // ================================================================

    @Test
    void testGenerateConstants() {
        table.getAllConstants().put("SQLITE_OK", new ExternConstant("SQLITE_OK", "Int", "0"));
        table.getAllConstants().put("SQLITE_ROW", new ExternConstant("SQLITE_ROW", "Int", "100"));

        String result = generator.generateConstants();

        assertTrue(result.contains("public static final int SQLITE_OK = 0;"));
        assertTrue(result.contains("public static final int SQLITE_ROW = 100;"));
    }

    @Test
    void testGenerateConstantsEmpty() {
        assertEquals("", generator.generateConstants());
    }

    // ================================================================
    //  5. Method handles
    // ================================================================

    @Test
    void testGenerateMethodHandles() {
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        func.libraryName = "sqlite3";
        table.getAllFunctions().put("sqlite3_open", func);

        String result = generator.generateMethodHandles();

        assertTrue(result.contains("private static final MethodHandle MH_sqlite3_open;"));
        assertTrue(result.contains("LINKER.downcallHandle("));
        assertTrue(result.contains("findSymbol(SQLITE3_LOOKUP"));
        assertTrue(result.contains("FunctionDescriptor.of("));
        assertTrue(result.contains("ValueLayout.JAVA_INT"));
        assertTrue(result.contains("ValueLayout.ADDRESS"));
    }

    @Test
    void testGenerateMethodHandlesEmpty() {
        assertEquals("", generator.generateMethodHandles());
    }

    // ================================================================
    //  6. Wrapper methods
    // ================================================================

    @Test
    void testGenerateWrapperMethods() {
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        func.libraryName = "sqlite3";
        table.getAllFunctions().put("sqlite3_open", func);

        String result = generator.generateWrapperMethods();

        assertTrue(result.contains("public static int sqlite3_open(Arena arena, String filename, MemorySegment ppDb)"));
        assertTrue(result.contains("MemorySegment filename_c = toCString(arena, filename);"));
        assertTrue(result.contains("MH_sqlite3_open.invokeExact("));
        assertTrue(result.contains("throw new RuntimeException(\"FFI call failed: sqlite3_open\""));
    }

    @Test
    void testGenerateWrapperMethodsEmpty() {
        assertEquals("", generator.generateWrapperMethods());
    }

    // ================================================================
    //  7. Helper methods
    // ================================================================

    @Test
    void testGenerateHelperMethods() {
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        String result = generator.generateHelperMethods();

        assertTrue(result.contains("private static MemorySegment toCString(Arena arena, String str)"));
        assertTrue(result.contains("private static String fromCString(MemorySegment segment)"));
        assertTrue(result.contains("private static MemorySegment findSymbol(SymbolLookup lookup, String name)"));
    }

    @Test
    void testGenerateHelperMethodsNoLinks() {
        String result = generator.generateHelperMethods();

        assertTrue(result.contains("toCString"));
        assertTrue(result.contains("fromCString"));
        // findSymbol should not be present when no libraries are linked
        assertFalse(result.contains("findSymbol"));
    }

    // ================================================================
    //  8. Full generation
    // ================================================================

    @Test
    void testGenerateAllEmpty() {
        assertEquals("", generator.generateAll());
    }

    @Test
    void testGenerateAllComplete() {
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllConstants().put("SQLITE_OK", new ExternConstant("SQLITE_OK", "Int", "0"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "String"));
        params.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        func.libraryName = "sqlite3";
        table.getAllFunctions().put("sqlite3_open", func);

        JavaFFIGenerator gen = new JavaFFIGenerator(table, "Sqlite3FFI");
        String result = gen.generateAll();

        assertTrue(result.contains("public final class Sqlite3FFI"));
        assertTrue(result.contains("SQLITE3_LOOKUP"));
        assertTrue(result.contains("public static final int SQLITE_OK = 0;"));
        assertTrue(result.contains("MH_sqlite3_open"));
        assertTrue(result.contains("public static int sqlite3_open("));
    }

    // ================================================================
    //  9. Type mapping
    // ================================================================

    @Test
    void testMapToJavaType() {
        assertEquals("void", JavaFFIGenerator.mapToJavaType("Void"));
        assertEquals("int", JavaFFIGenerator.mapToJavaType("Int"));
        assertEquals("double", JavaFFIGenerator.mapToJavaType("Float"));
        assertEquals("String", JavaFFIGenerator.mapToJavaType("String"));
        assertEquals("boolean", JavaFFIGenerator.mapToJavaType("Bool"));
        assertEquals("MemorySegment", JavaFFIGenerator.mapToJavaType("Pointer"));
        assertEquals("MemorySegment", JavaFFIGenerator.mapToJavaType("Ref<Int>"));
    }

    @Test
    void testMapToValueLayout() {
        assertEquals("ValueLayout.JAVA_VOID", JavaFFIGenerator.mapToValueLayout("Void"));
        assertEquals("ValueLayout.JAVA_INT", JavaFFIGenerator.mapToValueLayout("Int"));
        assertEquals("ValueLayout.JAVA_DOUBLE", JavaFFIGenerator.mapToValueLayout("Float"));
        assertEquals("ValueLayout.ADDRESS", JavaFFIGenerator.mapToValueLayout("String"));
        assertEquals("ValueLayout.ADDRESS", JavaFFIGenerator.mapToValueLayout("Pointer"));
        assertEquals("ValueLayout.ADDRESS", JavaFFIGenerator.mapToValueLayout("Ref<Int>"));
    }

    // ================================================================
    //  10. String/CString conversion
    // ================================================================

    @Test
    void testStringParameterConversion() {
        table.getAllLinks().add(new LinkDirective("c", "string.h"));

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("s", "CString"));
        ExternFunction func = new ExternFunction("strdup", params, "CString", false);
        func.libraryName = "c";
        table.getAllFunctions().put("strdup", func);

        String result = generator.generateWrapperMethods();

        assertTrue(result.contains("public static String strdup(Arena arena, String s)"));
        assertTrue(result.contains("MemorySegment s_c = toCString(arena, s);"));
        assertTrue(result.contains("MH_strdup.invokeExact(s_c)"));
    }
}

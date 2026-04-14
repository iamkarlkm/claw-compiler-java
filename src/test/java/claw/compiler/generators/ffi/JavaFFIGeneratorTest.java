package claw.compiler.generators.ffi;

import claw.compiler.generators.ffi.FFIBindingTable.ExternConstant;
import claw.compiler.generators.ffi.FFIBindingTable.ExternFunction;
import claw.compiler.generators.ffi.FFIBindingTable.ExternParam;
import claw.compiler.generators.ffi.FFIBindingTable.LinkDirective;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JavaFFIGenerator 测试类
 *
 * 测试 Java Panama/JNI FFI 绑定代码生成器的所有功能
 */
public class JavaFFIGeneratorTest {

    @Test
    public void testGeneratePanamaBinding() {
        FFIBindingTable table = new FFIBindingTable();

        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllConstants().put("CURLE_OK", new ExternConstant() {{ name = "CURLE_OK"; value = "0"; }});

        ExternFunction openFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);

        table.getAllFunctions().put("sqlite3_open", openFunc);

        JavaFFIGenerator generator = new JavaFFIGenerator(table, JavaFFIGenerator.Strategy.PANAMA, "claw.generated");
        String output = generator.generateAll();

        assertNotNull(output);
        assertTrue(output.contains("package claw.generated;"));
        assertTrue(output.contains("import java.lang.foreign.*;"));
        assertTrue(output.contains("import java.lang.invoke.MethodHandle;"));
        assertTrue(output.contains("public final class Sqlite3FFI {"));
        assertTrue(output.contains("private static final Linker LINKER"));
        assertTrue(output.contains("private static final MethodHandle MH_sqlite3_open"));
        assertTrue(output.contains("FunctionDescriptor.of("));
        assertTrue(output.contains("public static int sqlite3_open("));
        assertTrue(output.contains("toCString(arena, filename)"));
    }

    @Test
    public void testGenerateJNIBinding() {
        FFIBindingTable table = new FFIBindingTable();

        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllConstants().put("CURLE_OK", new ExternConstant() {{ name = "CURLE_OK"; value = "0"; }});

        ExternFunction openFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);

        table.getAllFunctions().put("sqlite3_open", openFunc);

        JavaFFIGenerator generator = new JavaFFIGenerator(table, JavaFFIGenerator.Strategy.JNI, "claw.generated");
        String output = generator.generateAll();

        assertNotNull(output);
        assertTrue(output.contains("package claw.generated;"));
        assertTrue(output.contains("Strategy: JNI"));
        assertTrue(output.contains("public final class Sqlite3FFI {"));
        assertTrue(output.contains("System.loadLibrary(\"claw_jni_bridge\");"));
        assertTrue(output.contains("public static native int sqlite3_open("));
        assertTrue(output.contains("String filename, long ppDb"));
    }

    @Test
    public void testGeneratePanamaImports() {
        JavaFFIGenerator generator = new JavaFFIGenerator(new FFIBindingTable());
        String output = generator.generatePanamaImports();

        assertNotNull(output);
        assertTrue(output.contains("import java.lang.foreign.*;"));
        assertTrue(output.contains("import java.lang.invoke.MethodHandle;"));
        assertTrue(output.contains("import java.nio.charset.StandardCharsets;"));
    }

    @Test
    public void testGeneratePanamaStaticInit() {
        FFIBindingTable table = new FFIBindingTable();

        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllLinks().add(new LinkDirective("curl", "curl/curl.h"));

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String output = generator.generatePanamaStaticInit();

        assertNotNull(output);
        assertTrue(output.contains("private static final Linker LINKER"));
        assertTrue(output.contains("SymbolLookup SQLITE3_LOOKUP"));
        assertTrue(output.contains("SymbolLookup CURL_LOOKUP"));
        assertTrue(output.contains("findSymbol(\"sqlite3_open\")"));
    }

    @Test
    public void testGeneratePanamaConstants() {
        FFIBindingTable table = new FFIBindingTable();

        ExternConstant ok = new ExternConstant();
        ok.name = "CURLE_OK";
        ok.value = "0";
        ExternConstant error = new ExternConstant();
        error.name = "CURLE_ERROR";
        error.value = "-1";

        table.getAllConstants().put("CURLE_OK", ok);
        table.getAllConstants().put("CURLE_ERROR", error);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String output = generator.generatePanamaConstants();

        assertNotNull(output);
        assertTrue(output.contains("public static final int CURLE_OK = 0;"));
        assertTrue(output.contains("public static final int CURLE_ERROR = -1;"));
    }

    @Test
    public void testGenerateArenaHelpers() {
        JavaFFIGenerator generator = new JavaFFIGenerator(new FFIBindingTable());
        String output = generator.generateArenaHelpers();

        assertNotNull(output);
        assertTrue(output.contains("public static MemorySegment toCString(Arena arena, String str)"));
        assertTrue(output.contains("public static String fromCString(MemorySegment segment)"));
        assertTrue(output.contains("allocatePointer(Arena arena)"));
        assertTrue(output.contains("readPointer(MemorySegment ptrSegment)"));
    }

    @Test
    public void testGeneratePanamaFunctionHandles() {
        FFIBindingTable table = new FFIBindingTable();

        ExternFunction openFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);

        ExternFunction closeFunc = new ExternFunction("sqlite3_close",
            List.of(new ExternParam("db", "Pointer")),
            "Void", false);

        table.getAllFunctions().put("sqlite3_open", openFunc);
        table.getAllFunctions().put("sqlite3_close", closeFunc);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String output = generator.generatePanamaFunctionHandles();

        assertNotNull(output);
        assertTrue(output.contains("private static final MethodHandle MH_sqlite3_open"));
        assertTrue(output.contains("private static final MethodHandle MH_sqlite3_close"));
        assertTrue(output.contains("FunctionDescriptor.ofVoid("));
        assertTrue(output.contains("FunctionDescriptor.of("));
    }

    @Test
    public void testGeneratePanamaWrapperMethods() {
        FFIBindingTable table = new FFIBindingTable();

        ExternFunction openFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);

        ExternFunction closeFunc = new ExternFunction("sqlite3_close",
            List.of(new ExternParam("db", "Pointer")),
            "Void", false);

        table.getAllFunctions().put("sqlite3_open", openFunc);
        table.getAllFunctions().put("sqlite3_close", closeFunc);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String output = generator.generatePanamaWrapperMethods();

        assertNotNull(output);
        assertTrue(output.contains("public static int sqlite3_open("));
        assertTrue(output.contains("public static void sqlite3_close("));
        assertTrue(output.contains("try {"));
        assertTrue(output.contains("toCString(arena, filename)"));
        assertTrue(output.contains("invokeExact("));
        assertTrue(output.contains("RuntimeException"));
    }

    @Test
    public void testGeneratePlatformDetection() {
        JavaFFIGenerator generator = new JavaFFIGenerator(new FFIBindingTable());
        String output = generator.generatePlatformDetection();

        assertNotNull(output);
        assertTrue(output.contains("final class ClawPlatform {"));
        assertTrue(output.contains("enum OS { WINDOWS, LINUX, MACOS, FREEBSD, ANDROID, UNKNOWN }"));
        assertTrue(output.contains("enum Arch { X86_64, ARM64, X86, ARM, UNKNOWN }"));
        assertTrue(output.contains("static final OS CURRENT_OS"));
        assertTrue(output.contains("static boolean isPlatform(OS... targets)"));
        assertTrue(output.contains("static boolean isArch(Arch... targets)"));
        assertTrue(output.contains("static String libraryFileName(String name)"));
    }

    @Test
    public void testGeneratePlatformConditionalBindings() {
        FFIBindingTable table = new FFIBindingTable();

        FFIBindingTable.ExternBlock block = table.newExternBlock();
        block.comment = "Windows-specific bindings";

        FFIBindingTable.PlatformConstraint constraint = new FFIBindingTable.PlatformConstraint();
        constraint.addPlatform("windows");
        constraint.addArchitectures("x86_64");
        block.platform = constraint;

        table.getExternBlocks().add(block);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String output = generator.generatePlatformConditionalBindings(table);

        assertNotNull(output);
        assertTrue(output.contains("loadPlatformBindings()"));
        assertTrue(output.contains("ClawPlatform.isPlatform(ClawPlatform.OS.WINDOWS"));
        assertTrue(output.contains("ClawPlatform.isArch(ClawPlatform.Arch.X86_64"));
    }

    @Test
    public void testGenerateJavaPlatformCondition() {
        FFIBindingTable table = new FFIBindingTable();

        FFIBindingTable.PlatformConstraint constraint = new FFIBindingTable.PlatformConstraint();
        constraint.addPlatform("windows");
        constraint.addArchitectures("x86_64");

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String output = generator.generateJavaPlatformCondition(constraint);

        assertNotNull(output);
        assertTrue(output.contains("if ("));
        assertTrue(output.contains("ClawPlatform.isPlatform(ClawPlatform.OS.WINDOWS"));
        assertTrue(output.contains("&& ClawPlatform.isArch(ClawPlatform.Arch.X86_64"));
    }

    @Test
    public void testMapClawTypeToJavaType() {
        // 基本类型
        assertEquals("void", JavaFFIGenerator.mapClawTypeToJavaType("Void"));
        assertEquals("int", JavaFFIGenerator.mapClawTypeToJavaType("Int"));
        assertEquals("double", JavaFFIGenerator.mapClawTypeToJavaType("Float"));
        assertEquals("String", JavaFFIGenerator.mapClawTypeToJavaType("String"));
        assertEquals("String", JavaFFIGenerator.mapClawTypeToJavaType("CString"));
        assertEquals("boolean", JavaFFIGenerator.mapClawTypeToJavaType("Bool"));
        assertEquals("MemorySegment", JavaFFIGenerator.mapClawTypeToJavaType("Any"));

        // FFI 类型
        assertEquals("MemorySegment", JavaFFIGenerator.mapClawTypeToJavaType("Pointer"));
        assertEquals("MemorySegment", JavaFFIGenerator.mapClawTypeToJavaType("OpaquePointer"));

        // 整数类型
        assertEquals("byte", JavaFFIGenerator.mapClawTypeToJavaType("Int8"));
        assertEquals("short", JavaFFIGenerator.mapClawTypeToJavaType("Int16"));
        assertEquals("int", JavaFFIGenerator.mapClawTypeToJavaType("Int32"));
        assertEquals("long", JavaFFIGenerator.mapClawTypeToJavaType("Int64"));
        assertEquals("long", JavaFFIGenerator.mapClawTypeToJavaType("UInt64"));

        // null
        assertEquals("void", JavaFFIGenerator.mapClawTypeToJavaType(null));
    }

    @Test
    public void testMapClawTypeToMemoryLayout() {
        // 基本类型
        assertEquals("ValueLayout.JAVA_INT", JavaFFIGenerator.mapClawTypeToMemoryLayout("Int"));
        assertEquals("ValueLayout.JAVA_DOUBLE", JavaFFIGenerator.mapClawTypeToMemoryLayout("Float"));
        assertEquals("ValueLayout.JAVA_BOOLEAN", JavaFFIGenerator.mapClawTypeToMemoryLayout("Bool"));
        assertEquals("ValueLayout.ADDRESS", JavaFFIGenerator.mapClawTypeToMemoryLayout("String"));
        assertEquals("ValueLayout.ADDRESS", JavaFFIGenerator.mapClawTypeToMemoryLayout("CString"));
        assertEquals("ValueLayout.ADDRESS", JavaFFIGenerator.mapClawTypeToMemoryLayout("Any"));

        // FFI 类型
        assertEquals("ValueLayout.ADDRESS", JavaFFIGenerator.mapClawTypeToMemoryLayout("Pointer"));

        // 整数类型
        assertEquals("ValueLayout.JAVA_BYTE", JavaFFIGenerator.mapClawTypeToMemoryLayout("Int8"));
        assertEquals("ValueLayout.JAVA_SHORT", JavaFFIGenerator.mapClawTypeToMemoryLayout("Int16"));
        assertEquals("ValueLayout.JAVA_LONG", JavaFFIGenerator.mapClawTypeToMemoryLayout("Int64"));
    }

    @Test
    public void testMapClawTypeToJNIType() {
        assertEquals("void", JavaFFIGenerator.mapClawTypeToJNIType("Void"));
        assertEquals("int", JavaFFIGenerator.mapClawTypeToJNIType("Int"));
        assertEquals("double", JavaFFIGenerator.mapClawTypeToJNIType("Float"));
        assertEquals("String", JavaFFIGenerator.mapClawTypeToJNIType("String"));
        assertEquals("boolean", JavaFFIGenerator.mapClawTypeToJNIType("Bool"));
        assertEquals("long", JavaFFIGenerator.mapClawTypeToJNIType("Any"));
        assertEquals("long", JavaFFIGenerator.mapClawTypeToJNIType("Pointer"));
        assertEquals("byte", JavaFFIGenerator.mapClawTypeToJNIType("Int8"));
    }

    @Test
    public void testGetBindingClassName() {
        FFIBindingTable table = new FFIBindingTable();

        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllLinks().add(new LinkDirective("curl", "curl"));

        JavaFFIGenerator generator = new JavaFFIGenerator(table);

        assertEquals("Sqlite3FFI", generator.getBindingClassName());
        assertEquals("CurlFFI", generator.getBindingClassName());
    }

    @Test
    public void testToPascalCase() {
        JavaFFIGenerator generator = new JavaFFIGenerator(new FFIBindingTable());

        assertEquals("Sqlite3", generator.toPascalCase("sqlite3"));
        assertEquals("CurlEasy", generator.toPascalCase("curl_easy"));
        assertEquals("MyLibName", generator.toPascalCase("my-lib.name"));
        assertEquals("OpenSSL", generator.toPascalCase("openssl"));
    }

    @Test
    public void testToConstantName() {
        JavaFFIGenerator generator = new JavaFFIGenerator(new FFIBindingTable());

        assertEquals("SQLITE3", generator.toConstantName("sqlite3"));
        assertEquals("CURLE_OK", generator.toConstantName("CURLE_OK"));
        assertEquals("MYLIB_NAME", generator.toConstantName("my-lib_name"));
    }

    @Test
    public void testGenerateAllWithPanama() {
        FFIBindingTable table = new FFIBindingTable();

        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllConstants().put("CURLE_OK", new ExternConstant() {{ name = "CURLE_OK"; value = "0"; }});

        ExternFunction openFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);

        ExternFunction closeFunc = new ExternFunction("sqlite3_close",
            List.of(new ExternParam("db", "Pointer")),
            "Void", false);

        table.getAllFunctions().put("sqlite3_open", openFunc);
        table.getAllFunctions().put("sqlite3_close", closeFunc);

        JavaFFIGenerator generator = new JavaFFIGenerator(table, JavaFFIGenerator.Strategy.PANAMA, "com.example");
        String output = generator.generateAll();

        assertNotNull(output);
        assertTrue(output.contains("package com.example;"));
        assertTrue(output.contains("import java.lang.foreign.*;"));
        assertTrue(output.contains("public final class Sqlite3FFI {"));
        assertTrue(output.contains("private static final MethodHandle MH_sqlite3_open"));
        assertTrue(output.contains("public static int sqlite3_open("));
        assertTrue(output.contains("public static void sqlite3_close("));
    }

    @Test
    public void testGenerateAllWithJNI() {
        FFIBindingTable table = new FFIBindingTable();

        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        ExternFunction openFunc = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);

        table.getAllFunctions().put("sqlite3_open", openFunc);

        JavaFFIGenerator generator = new JavaFFIGenerator(table, JavaFFIGenerator.Strategy.JNI, "com.example");
        String output = generator.generateAll();

        assertNotNull(output);
        assertTrue(output.contains("Strategy: JNI"));
        assertTrue(output.contains("public final class Sqlite3FFI {"));
        assertTrue(output.contains("System.loadLibrary(\"claw_jni_bridge\");"));
        assertTrue(output.contains("public static native int sqlite3_open("));
    }

    @Test
    public void testGenerateWithMultipleLibraries() {
        FFIBindingTable table = new FFIBindingTable();

        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllLinks().add(new LinkDirective("curl", "curl/curl.h"));

        ExternFunction sqlite3_open = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String")),
            "Int", false);

        ExternFunction curl_init = new ExternFunction("curl_easy_init",
            List.of(),
            "Pointer", false);

        table.getAllFunctions().put("sqlite3_open", sqlite3_open);
        table.getAllFunctions().put("curl_easy_init", curl_init);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String output = generator.generateAll();

        assertNotNull(output);
        assertTrue(output.contains("private static final MethodHandle MH_sqlite3_open"));
        assertTrue(output.contains("private static final MethodHandle MH_curl_easy_init"));
    }

    @Test
    public void testGenerateWithVoidReturnType() {
        FFIBindingTable table = new FFIBindingTable();

        ExternFunction freeFunc = new ExternFunction("free",
            List.of(new ExternParam("ptr", "Pointer")),
            "Void", false);

        table.getAllFunctions().put("free", freeFunc);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String output = generator.generateAll();

        assertNotNull(output);
        assertTrue(output.contains("public static void free("));
        assertTrue(output.contains("FunctionDescriptor.ofVoid("));
    }

    @Test
    public void testGenerateWithStringReturnType() {
        FFIBindingTable table = new FFIBindingTable();

        ExternFunction strdupFunc = new ExternFunction("strdup",
            List.of(new ExternParam("s", "String")),
            "String", false);

        table.getAllFunctions().put("strdup", strdupFunc);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String output = generator.generateAll();

        assertNotNull(output);
        assertTrue(output.contains("public static String strdup("));
        assertTrue(output.contains("MemorySegment _result"));
        assertTrue(output.contains("return fromCString(_result)"));
    }

    @Test
    public void testGenerateWithRefParameter() {
        FFIBindingTable table = new FFIBindingTable();

        ExternFunction strlenFunc = new ExternFunction("strlen",
            List.of(new ExternParam("str", "String")),
            "SizeT", false);

        table.getAllFunctions().put("strlen", strlenFunc);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String output = generator.generateAll();

        assertNotNull(output);
        assertTrue(output.contains("public static long strlen("));
        assertTrue(output.contains("toCString(arena, str)"));
    }

    @Test
    public void testGenerateWithMultipleParameters() {
        FFIBindingTable table = new FFIBindingTable();

        ExternFunction strcpyFunc = new ExternFunction("strcpy",
            List.of(new ExternParam("dest", "String"), new ExternParam("src", "String")),
            "String", false);

        table.getAllFunctions().put("strcpy", strcpyFunc);

        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String output = generator.generateAll();

        assertNotNull(output);
        assertTrue(output.contains("public static String strcpy("));
        assertTrue(output.contains("toCString(arena, dest)"));
        assertTrue(output.contains("toCString(arena, src)"));
    }

    @Test
    public void testGenerateCustomPackage() {
        FFIBindingTable table = new FFIBindingTable();
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        JavaFFIGenerator generator = new JavaFFIGenerator(table, JavaFFIGenerator.Strategy.PANAMA, "com.mycompany.lib");
        String output = generator.generateAll();

        assertNotNull(output);
        assertTrue(output.contains("package com.mycompany.lib;"));
    }
}

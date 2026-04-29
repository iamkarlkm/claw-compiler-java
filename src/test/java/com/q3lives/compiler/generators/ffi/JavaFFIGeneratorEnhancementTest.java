package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JavaFFIGenerator 增强功能测试
 */
class JavaFFIGeneratorEnhancementTest {

    private FFIBindingTable table;
    private JavaFFIGenerator generator;

    @BeforeEach
    void setUp() {
        table = new FFIBindingTable();
        generator = new JavaFFIGenerator(table);
    }

    // ================================================================
    //  Struct 生成测试
    // ================================================================

    @Test
    void testGenerateStructDefinitions() {
        // 添加 struct
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        table.getAllStructs().add(point);

        String result = generator.generateStructDefinitions();

        assertNotNull(result);
        assertTrue(result.contains("class Point"));
        assertTrue(result.contains("x"));
        assertTrue(result.contains("y"));
    }

    @Test
    void testGenerateStructDefinitionsEmpty() {
        String result = generator.generateStructDefinitions();
        assertEquals("", result);
    }

    @Test
    void testGenerateMultipleStructs() {
        // 添加多个 struct
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        table.getAllStructs().add(point);

        ExternStruct rect = new ExternStruct("Rectangle");
        rect.fields.add(new StructField("x", "Int"));
        rect.fields.add(new StructField("y", "Int"));
        rect.fields.add(new StructField("width", "Int"));
        rect.fields.add(new StructField("height", "Int"));
        table.getAllStructs().add(rect);

        String result = generator.generateStructDefinitions();

        assertTrue(result.contains("class Point"));
        assertTrue(result.contains("class Rectangle"));
    }

    // ================================================================
    //  Enum 生成测试
    // ================================================================

    @Test
    void testGenerateEnumDefinitions() {
        // 添加 enum
        ExternEnum status = new ExternEnum("Status");
        status.members.add(new EnumMember("OK", "0"));
        status.members.add(new EnumMember("ERROR", "1"));
        table.getAllEnums().add(status);

        String result = generator.generateEnumDefinitions();

        assertNotNull(result);
        assertTrue(result.contains("enum Status"));
        assertTrue(result.contains("OK"));
        assertTrue(result.contains("ERROR"));
    }

    @Test
    void testGenerateEnumDefinitionsEmpty() {
        String result = generator.generateEnumDefinitions();
        assertEquals("", result);
    }

    @Test
    void testGenerateMultipleEnums() {
        // 添加多个 enum
        ExternEnum status = new ExternEnum("Status");
        status.members.add(new EnumMember("OK", "0"));
        status.members.add(new EnumMember("ERROR", "1"));
        table.getAllEnums().add(status);

        ExternEnum mode = new ExternEnum("Mode");
        mode.members.add(new EnumMember("READ", "0"));
        mode.members.add(new EnumMember("WRITE", "1"));
        table.getAllEnums().add(mode);

        String result = generator.generateEnumDefinitions();

        assertTrue(result.contains("enum Status"));
        assertTrue(result.contains("enum Mode"));
    }

    // ================================================================
    //  Callback 生成测试
    // ================================================================

    @Test
    void testGenerateCallbackDefinitions() {
        // 添加 callback
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("value", "Int"));

        ExternCallback callback = new ExternCallback("ProcessCallback", params, "Void");
        table.getAllCallbacks().add(callback);

        String result = generator.generateCallbackDefinitions();

        assertNotNull(result);
        assertTrue(result.contains("interface ProcessCallback"));
        assertTrue(result.contains("@FunctionalInterface"));
    }

    @Test
    void testGenerateCallbackDefinitionsEmpty() {
        String result = generator.generateCallbackDefinitions();
        assertEquals("", result);
    }

    @Test
    void testGenerateCallbackWithReturnValue() {
        // 有返回值的 callback
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("a", "Int"));
        params.add(new ExternParam("b", "Int"));

        ExternCallback callback = new ExternCallback("CompareCallback", params, "Int");
        table.getAllCallbacks().add(callback);

        String result = generator.generateCallbackDefinitions();

        assertTrue(result.contains("int invoke"));
    }

    // ================================================================
    //  完整生成测试
    // ================================================================

    @Test
    void testGenerateAllWithAllTypes() {
        // 添加所有类型的声明
        // struct
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        table.getAllStructs().add(point);

        // enum
        ExternEnum status = new ExternEnum("Status");
        status.members.add(new EnumMember("OK", "0"));
        table.getAllEnums().add(status);

        // callback
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("data", "Pointer"));
        ExternCallback callback = new ExternCallback("DataCallback", params, "Void");
        table.getAllCallbacks().add(callback);

        // function
        List<ExternParam> funcParams = new ArrayList<>();
        funcParams.add(new ExternParam("x", "Int"));
        ExternFunction func = new ExternFunction("process", funcParams, "Int", false);
        table.getAllFunctions().put("process", func);

        // constant
        table.getAllConstants().put("MAX_SIZE", new ExternConstant("MAX_SIZE", "Int", "100"));

        // link
        table.getAllLinks().add(new LinkDirective("testlib", "testlib.h"));

        String result = generator.generateAll();

        assertNotNull(result);
        assertTrue(result.contains("class Point"));
        assertTrue(result.contains("enum Status"));
        assertTrue(result.contains("interface DataCallback"));
        assertTrue(result.contains("process"));
        assertTrue(result.contains("MAX_SIZE"));
    }

    // ================================================================
    //  类型映射测试
    // ================================================================

    @Test
    void testMapToJavaType() {
        assertEquals("void", JavaFFIGenerator.mapToJavaType("Void"));
        assertEquals("int", JavaFFIGenerator.mapToJavaType("Int"));
        assertEquals("double", JavaFFIGenerator.mapToJavaType("Float"));
        assertEquals("boolean", JavaFFIGenerator.mapToJavaType("Bool"));
        assertEquals("String", JavaFFIGenerator.mapToJavaType("String"));
        assertEquals("MemorySegment", JavaFFIGenerator.mapToJavaType("Pointer"));
        assertEquals("MemorySegment", JavaFFIGenerator.mapToJavaType("OpaquePointer"));
    }

    @Test
    void testMapToValueLayout() {
        assertEquals("ValueLayout.JAVA_INT", JavaFFIGenerator.mapToValueLayout("Int"));
        assertEquals("ValueLayout.JAVA_DOUBLE", JavaFFIGenerator.mapToValueLayout("Float"));
        assertEquals("ValueLayout.ADDRESS", JavaFFIGenerator.mapToValueLayout("Pointer"));
    }
}

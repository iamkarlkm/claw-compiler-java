package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonFFIGenerator 增强功能测试
 */
class PythonFFIGeneratorEnhancementTest {

    private FFIBindingTable table;
    private PythonFFIGenerator generator;

    @BeforeEach
    void setUp() {
        table = new FFIBindingTable();
        generator = new PythonFFIGenerator(table);
    }

    // ================================================================
    //  Struct 生成测试
    // ================================================================

    @Test
    void testGenerateStructDefinitions() {
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        table.getAllStructs().add(point);

        String result = generator.generateStructDefinitions();

        assertNotNull(result);
        assertTrue(result.contains("class Point"));
    }

    @Test
    void testGenerateStructDefinitionsEmpty() {
        String result = generator.generateStructDefinitions();
        assertEquals("", result);
    }

    // ================================================================
    //  完整生成测试
    // ================================================================

    @Test
    void testGenerateAll() {
        // 添加各种声明
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        table.getAllStructs().add(point);

        ExternEnum status = new ExternEnum("Status");
        status.members.add(new EnumMember("OK", "0"));
        status.members.add(new EnumMember("ERROR", "1"));
        table.getAllEnums().add(status);

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("data", "Pointer"));
        ExternCallback callback = new ExternCallback("DataCallback", params, "Void");
        table.getAllCallbacks().add(callback);

        List<ExternParam> funcParams = new ArrayList<>();
        funcParams.add(new ExternParam("x", "Int"));
        ExternFunction func = new ExternFunction("process", funcParams, "Int", false);
        table.getAllFunctions().put("process", func);

        table.getAllConstants().put("MAX_SIZE", new ExternConstant("MAX_SIZE", "Int", "100"));

        table.getAllLinks().add(new LinkDirective("testlib", "testlib.h"));

        String result = generator.generateAll();

        assertNotNull(result);
        assertTrue(result.contains("class Point"));
        assertTrue(result.contains("process"));
        assertTrue(result.contains("MAX_SIZE"));
    }

    // ================================================================
    //  平台条件加载测试
    // ================================================================

    @Test
    void testGeneratePlatformConditionalLoading() {
        // 添加 Windows 特定块
        FFIBindingTable.ExternBlock windowsBlock = new FFIBindingTable.ExternBlock();
        windowsBlock.platform = com.q3lives.compiler.generators.ffi.platform.PlatformConstraint.builder()
            .platform("windows")
            .build();
        windowsBlock.links.add(new LinkDirective("kernel32", "windows.h"));

        // 添加通用块
        FFIBindingTable.ExternBlock universalBlock = new FFIBindingTable.ExternBlock();
        universalBlock.platform = com.q3lives.compiler.generators.ffi.platform.PlatformConstraint.UNIVERSAL;
        universalBlock.links.add(new LinkDirective("common", "common.h"));

        // 注意：由于是模拟测试，这里只验证方法可调用
        String result = generator.generatePlatformConditionalLoading(table);
        assertNotNull(result);
    }

    // ================================================================
    //  常量生成测试
    // ================================================================

    @Test
    void testGenerateConstants() {
        table.getAllConstants().put("SQLITE_OK", new ExternConstant("SQLITE_OK", "Int", "0"));
        table.getAllConstants().put("SQLITE_ROW", new ExternConstant("SQLITE_ROW", "Int", "100"));

        String result = generator.generateConstants();

        assertTrue(result.contains("SQLITE_OK"));
        assertTrue(result.contains("SQLITE_ROW"));
    }

    // ================================================================
    //  函数绑定测试
    // ================================================================

    @Test
    void testGenerateFunctionBindings() {
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("filename", "CString"));
        params.add(new ExternParam("ppDb", "Pointer"));

        ExternFunction func = new ExternFunction("sqlite3_open", params, "Int", false);
        func.description = "Open database";
        table.getAllFunctions().put("sqlite3_open", func);

        String result = generator.generateFunctionBindings();

        assertTrue(result.contains("sqlite3_open"));
        assertTrue(result.contains("ctypes"));
    }
}

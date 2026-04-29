package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FFIBindingTable 验证功能测试
 */
class FFIBindingTableValidationTest {

    private FFIBindingTable table;

    @BeforeEach
    void setUp() {
        table = new FFIBindingTable();
    }

    // ================================================================
    //  1. 有效绑定表测试
    // ================================================================

    @Test
    void testValidBindingTable() {
        // 添加有效的 extern 声明
        ExternFunction func = new ExternFunction("test_func",
            List.of(new ExternParam("x", "Int")), "Int", false);
        table.getAllFunctions().put("test_func", func);

        table.getAllLinks().add(new LinkDirective("testlib", "testlib.h"));

        // 验证
        ValidationResult result = table.validate();

        assertTrue(result.isValid(), "有效表应该通过验证: " + result.errors);
    }

    // ================================================================
    //  2. 未使用类型警告测试
    // ================================================================

    @Test
    void testUnusedTypeWarning() {
        // 添加未使用的类型
        ExternType unusedType = new ExternType("UnusedType", "Pointer");
        table.getAllTypes().put("UnusedType", unusedType);

        // 添加使用其他类型的函数
        ExternFunction func = new ExternFunction("test_func",
            List.of(new ExternParam("x", "Int")), "Int", false);
        table.getAllFunctions().put("test_func", func);

        // 验证
        ValidationResult result = table.validate();

        assertTrue(result.isValid());
        assertTrue(result.hasWarnings());
    }

    // ================================================================
    //  3. 空链接库错误测试
    // ================================================================

    @Test
    void testEmptyLinkError() {
        // 添加空链接库
        LinkDirective emptyLink = new LinkDirective("", "test.h");
        table.getAllLinks().add(emptyLink);

        // 验证
        ValidationResult result = table.validate();

        assertFalse(result.isValid());
    }

    // ================================================================
    //  4. 有效类型引用测试
    // ================================================================

    @Test
    void testValidTypeReferences() {
        // 声明类型
        table.getAllTypes().put("MyStruct", new ExternType("MyStruct", "Pointer"));

        // 使用该类型
        ExternFunction func = new ExternFunction("test_func",
            List.of(new ExternParam("ptr", "MyStruct")), "MyStruct", false);
        table.getAllFunctions().put("test_func", func);

        // 验证
        ValidationResult result = table.validate();

        assertTrue(result.isValid());
    }

    // ================================================================
    //  5. 复杂场景测试
    // ================================================================

    @Test
    void testComplexValidTable() {
        // 添加多种有效声明
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllTypes().put("sqlite3", new ExternType("sqlite3", "OpaquePointer"));
        table.getAllConstants().put("SQLITE_OK", new ExternConstant("SQLITE_OK", "Int", "0"));

        ExternFunction open = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "CString"),
                    new ExternParam("ppDb", "Pointer")),
            "Int", false);
        table.getAllFunctions().put("sqlite3_open", open);

        ValidationResult result = table.validate();
        assertTrue(result.isValid());
    }

    // ================================================================
    //  6. 空表验证测试
    // ================================================================

    @Test
    void testEmptyTableValidation() {
        ValidationResult result = table.validate();
        assertTrue(result.isValid());
        assertFalse(result.hasWarnings());
    }

    // ================================================================
    //  7. 结构体和枚举类型引用测试
    // ================================================================

    @Test
    void testStructAndEnumReferences() {
        // 添加 struct
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        table.getAllStructs().add(point);

        // 添加 enum
        ExternEnum status = new ExternEnum("Status");
        status.members.add(new EnumMember("OK", "0"));
        table.getAllEnums().add(status);

        // 使用 struct 和 enum 的函数
        ExternFunction func = new ExternFunction("process",
            List.of(new ExternParam("p", "Pointer"), new ExternParam("s", "Int")),
            "Int", false);
        table.getAllFunctions().put("process", func);

        ValidationResult result = table.validate();
        assertTrue(result.isValid());
    }

    // ================================================================
    //  8. 原始类型测试
    // ================================================================

    @Test
    void testPrimitiveTypes() {
        // 使用原始类型的函数应该有效
        ExternFunction func = new ExternFunction("test",
            List.of(new ExternParam("a", "Int"),
                    new ExternParam("b", "Float"),
                    new ExternParam("c", "Pointer")),
            "Void", false);
        table.getAllFunctions().put("test", func);

        ValidationResult result = table.validate();
        assertTrue(result.isValid());
    }

    // ================================================================
    //  9. 泛型类型测试
    // ================================================================

    @Test
    void testGenericTypes() {
        // 使用泛型类型的函数应该有效
        ExternFunction func = new ExternFunction("test",
            List.of(new ExternParam("ptr", "Ref<Int>"),
                    new ExternParam("arr", "CArray<Float>")),
            "Void", false);
        table.getAllFunctions().put("test", func);

        ValidationResult result = table.validate();
        assertTrue(result.isValid());
    }

    // ================================================================
    //  10. 回调类型测试
    // ================================================================

    @Test
    void testCallbackTypes() {
        // 使用回调的函数应该有效
        List<ExternParam> cbParams = new ArrayList<>();
        cbParams.add(new ExternParam("data", "Pointer"));
        ExternCallback callback = new ExternCallback("DataCallback", cbParams, "Void");
        table.getAllCallbacks().add(callback);

        ExternFunction func = new ExternFunction("register",
            List.of(new ExternParam("callback", "Pointer")),
            "Void", false);
        table.getAllFunctions().put("register", func);

        ValidationResult result = table.validate();
        assertTrue(result.isValid());
    }
}

package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 嵌套类型边界场景测试
 *
 * 覆盖：struct 嵌套 struct、struct 嵌套 enum、enum 作为函数返回类型、嵌套泛型类型
 */
class NestedTypeTest {

    private FFIBindingTable table;

    @BeforeEach
    void setUp() {
        table = new FFIBindingTable();
    }

    // ================================================================
    //  1. Struct 嵌套 Struct 测试
    // ================================================================

    @Test
    void testStructWithNestedStruct() {
        // 内层 struct
        ExternStruct inner = new ExternStruct("Dimensions");
        inner.fields.add(new StructField("width", "Float"));
        inner.fields.add(new StructField("height", "Float"));
        table.getAllStructs().add(inner);

        // 外层 struct 引用内层
        ExternStruct outer = new ExternStruct("Window");
        outer.fields.add(new StructField("title", "CString"));
        outer.fields.add(new StructField("size", "Pointer")); // 使用 Pointer 引用嵌套 struct
        table.getAllStructs().add(outer);

        assertEquals(2, table.getAllStructs().size());
        assertEquals("Dimensions", table.getAllStructs().get(0).name);
        assertEquals("Window", table.getAllStructs().get(1).name);
    }

    @Test
    void testStructWithMultipleNestedRefs() {
        // 一个 struct 引用多个其他 struct
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        table.getAllStructs().add(point);

        ExternStruct color = new ExternStruct("Color");
        color.fields.add(new StructField("r", "UInt8"));
        color.fields.add(new StructField("g", "UInt8"));
        color.fields.add(new StructField("b", "UInt8"));
        table.getAllStructs().add(color);

        // 带多个嵌套引用的 struct
        ExternStruct coloredPoint = new StructBuilder("ColoredPoint")
            .addField("position", "Pointer") // Point*
            .addField("color", "Pointer")   // Color*
            .build();
        table.getAllStructs().add(coloredPoint);

        assertEquals(3, table.getAllStructs().size());
    }

    // ================================================================
    //  2. Struct 嵌套 Enum 测试
    // ================================================================

    @Test
    void testStructWithNestedEnum() {
        // 定义 enum
        ExternEnum status = new ExternEnum("Status");
        status.members.add(new EnumMember("OK", "0"));
        status.members.add(new EnumMember("ERROR", "1"));
        status.members.add(new EnumMember("PENDING", "2"));
        table.getAllEnums().add(status);

        // 定义包含 enum 的 struct
        ExternStruct result = new ExternStruct("Result");
        result.fields.add(new StructField("status", "Int")); // 使用 Int 而非直接使用 enum
        result.fields.add(new StructField("message", "CString"));
        table.getAllStructs().add(result);

        assertEquals(1, table.getAllEnums().size());
        assertEquals(1, table.getAllStructs().size());
    }

    // ================================================================
    //  3. Enum 作为函数返回类型测试
    // ================================================================

    @Test
    void testFunctionReturningEnum() {
        // 定义 enum
        ExternEnum errorCode = new ExternEnum("ErrorCode");
        errorCode.members.add(new EnumMember("SUCCESS", "0"));
        errorCode.members.add(new EnumMember("NOT_FOUND", "1"));
        errorCode.members.add(new EnumMember("PERMISSION_DENIED", "2"));
        table.getAllEnums().add(errorCode);

        // 定义返回 enum 的函数
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("code", "Int"));

        ExternFunction func = new ExternFunction("getErrorCode", params, "Int", false);
        table.getAllFunctions().put("getErrorCode", func);

        assertEquals(1, table.getAllEnums().size());
        assertEquals(1, table.getAllFunctions().size());
    }

    @Test
    void testFunctionWithEnumParam() {
        // 定义 enum
        ExternEnum mode = new ExternEnum("Mode");
        mode.members.add(new EnumMember("READ", "0"));
        mode.members.add(new EnumMember("WRITE", "1"));
        table.getAllEnums().add(mode);

        // 定义接受 enum 的函数
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("mode", "Int"));

        ExternFunction func = new ExternFunction("openFile", params, "Pointer", false);
        table.getAllFunctions().put("openFile", func);

        assertNotNull(table.findFunction("openFile"));
    }

    // ================================================================
    //  4. 嵌套泛型类型测试
    // ================================================================

    @Test
    void testRefOfPrimitiveType() {
        // Ref<Int> - 基本类型指针
        ExternType refInt = new ExternType("RefInt", "Pointer");
        table.getAllTypes().put("RefInt", refInt);

        // 使用 Ref<Int> 作为参数
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("value", "Pointer"));

        ExternFunction func = new ExternFunction("getValue", params, "Pointer", false);
        table.getAllFunctions().put("getValue", func);

        assertNotNull(table.findType("RefInt"));
    }

    @Test
    void testCArrayOfPrimitiveType() {
        // CArray<Float> - 浮点数数组
        ExternType floatArray = new ExternType("FloatArray", "Pointer");
        table.getAllTypes().put("FloatArray", floatArray);

        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("array", "Pointer"));
        params.add(new ExternParam("size", "Int"));

        ExternFunction func = new ExternFunction("processArray", params, "Void", false);
        table.getAllFunctions().put("processArray", func);

        assertNotNull(table.findFunction("processArray"));
    }

    @Test
    void testNestedGenerics() {
        // 模拟嵌套泛型：Ref<CArray<Int>>
        // 实际存储为两层 Pointer

        // 内层：CArray<Int>
        ExternType intArray = new ExternType("IntArray", "Pointer");
        table.getAllTypes().put("IntArray", intArray);

        // 外层：Ref<IntArray>
        ExternType refIntArray = new ExternType("RefIntArray", "Pointer");
        table.getAllTypes().put("RefIntArray", refIntArray);

        assertNotNull(table.findType("IntArray"));
        assertNotNull(table.findType("RefIntArray"));
    }

    // ================================================================
    //  5. 复杂嵌套场景测试
    // ================================================================

    @Test
    void testComplexNestedScenario() {
        // 完整的复杂场景：带有 callback、struct、enum 的函数

        // 1. 定义 enum
        ExternEnum eventType = new ExternEnum("EventType");
        eventType.members.add(new EnumMember("CLICK", "0"));
        eventType.members.add(new EnumMember("HOVER", "1"));
        table.getAllEnums().add(eventType);

        // 2. 定义 struct
        ExternStruct event = new ExternStruct("Event");
        event.fields.add(new StructField("type", "Int"));
        event.fields.add(new StructField("timestamp", "Int64"));
        table.getAllStructs().add(event);

        // 3. 定义 callback
        List<ExternParam> cbParams = new ArrayList<>();
        cbParams.add(new ExternParam("event", "Pointer"));
        ExternCallback eventCb = new ExternCallback("EventCallback", cbParams, "Void");
        table.getAllCallbacks().add(eventCb);

        // 4. 定义使用以上所有的函数
        List<ExternParam> funcParams = new ArrayList<>();
        funcParams.add(new ExternParam("handler", "Pointer")); // callback
        funcParams.add(new ExternParam("config", "Pointer")); // struct

        ExternFunction register = new ExternFunction("registerEventHandler", funcParams, "Int", false);
        table.getAllFunctions().put("registerEventHandler", register);

        // 验证所有类型都已添加
        assertEquals(1, table.getAllEnums().size());
        assertEquals(1, table.getAllStructs().size());
        assertEquals(1, table.getAllCallbacks().size());
        assertEquals(1, table.getAllFunctions().size());
    }

    @Test
    void testSelfReferentialStruct() {
        // 自引用 struct（链表节点等）
        ExternStruct node = new ExternStruct("Node");
        node.fields.add(new StructField("value", "Int"));
        node.fields.add(new StructField("next", "Pointer")); // 自引用
        table.getAllStructs().add(node);

        assertEquals(2, node.fields.size());
        assertEquals("next", node.fields.get(1).name);
    }

    @Test
    void testUnionLikeStruct() {
        // 类似 union 的 struct（通过重叠字段）
        ExternStruct variant = new ExternStruct("Variant");
        variant.fields.add(new StructField("asInt", "Int"));
        variant.fields.add(new StructField("asFloat", "Float"));
        variant.fields.add(new StructField("asPointer", "Pointer"));
        table.getAllStructs().add(variant);

        assertEquals(3, variant.fields.size());
    }

    // ================================================================
    //  6. 类型查找测试
    // ================================================================

    @Test
    void testFindNestedTypes() {
        // 添加各种类型
        ExternStruct s = new ExternStruct("TestStruct");
        table.getAllStructs().add(s);

        ExternEnum e = new ExternEnum("TestEnum");
        table.getAllEnums().add(e);

        ExternType t = new ExternType("TestType", "Pointer");
        table.getAllTypes().put("TestType", t);

        // 验证 findType 可以找到所有类型
        assertNotNull(table.findType("TestStruct"));
        assertNotNull(table.findType("TestEnum"));
        assertNotNull(table.findType("TestType"));
    }

    @Test
    void testTypeLookupPriority() {
        // 测试类型查找优先级：type > struct > enum > callback
        ExternType t = new ExternType("MyType", "Pointer");
        table.getAllTypes().put("MyType", t);

        ExternStruct s = new ExternStruct("MyType");
        table.getAllStructs().add(s);

        // type 应该优先被找到
        Object found = table.findType("MyType");
        assertTrue(found instanceof ExternType);
    }

    // 辅助类
    private static class StructBuilder {
        private final String name;
        private final List<StructField> fields = new ArrayList<>();

        StructBuilder(String name) {
            this.name = name;
        }

        StructBuilder addField(String fieldName, String fieldType) {
            fields.add(new StructField(fieldName, fieldType));
            return this;
        }

        ExternStruct build() {
            ExternStruct struct = new ExternStruct(name);
            struct.fields.addAll(fields);
            return struct;
        }
    }
}

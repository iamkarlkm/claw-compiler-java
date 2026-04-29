package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Callback（函数指针）边界场景测试
 */
class CallbackTest {

    private FFIBindingTable table;

    @BeforeEach
    void setUp() {
        table = new FFIBindingTable();
    }

    // ================================================================
    //  1. 基础 Callback 测试
    // ================================================================

    @Test
    void testSimpleCallback() {
        ExternCallback callback = new ExternCallback("SimpleCallback",
            new ArrayList<>(), "Void");
        table.getAllCallbacks().add(callback);

        assertNotNull(callback);
        assertEquals("SimpleCallback", callback.name);
        assertEquals(0, callback.params.size());
        assertEquals("Void", callback.returnType);
    }

    @Test
    void testCallbackWithParams() {
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("value", "Int"));
        params.add(new ExternParam("data", "Pointer"));

        ExternCallback callback = new ExternCallback("DataCallback", params, "Bool");
        table.getAllCallbacks().add(callback);

        assertEquals(2, callback.params.size());
        assertEquals("Bool", callback.returnType);
    }

    // ================================================================
    //  2. Callback 作为函数参数测试
    // ================================================================

    @Test
    void testFunctionWithCallbackParam() {
        // 声明一个 Callback 类型
        List<ExternParam> cbParams = new ArrayList<>();
        cbParams.add(new ExternParam("result", "Int"));
        ExternCallback progressCb = new ExternCallback("ProgressCallback", cbParams, "Void");
        table.getAllCallbacks().add(progressCb);

        // 使用该 Callback 作为函数参数
        List<ExternParam> funcParams = new ArrayList<>();
        funcParams.add(new ExternParam("callback", "FuncPointer")); // 使用 FuncPointer 引用

        ExternFunction func = new ExternFunction("process",
            funcParams, "Int", false);
        func.description = "Process with callback";
        table.getAllFunctions().put("process", func);

        assertNotNull(table.findFunction("process"));
        assertEquals(1, table.getAllCallbacks().size());
    }

    @Test
    void testFunctionWithMultipleCallbacks() {
        // 多个 callback 参数
        List<ExternParam> onSuccessParams = new ArrayList<>();
        onSuccessParams.add(new ExternParam("data", "Pointer"));
        ExternCallback onSuccess = new ExternCallback("OnSuccess", onSuccessParams, "Void");
        table.getAllCallbacks().add(onSuccess);

        List<ExternParam> onErrorParams = new ArrayList<>();
        onErrorParams.add(new ExternParam("code", "Int"));
        onErrorParams.add(new ExternParam("message", "CString"));
        ExternCallback onError = new ExternCallback("OnError", onErrorParams, "Void");
        table.getAllCallbacks().add(onError);

        assertEquals(2, table.getAllCallbacks().size());
    }

    // ================================================================
    //  3. Callback 返回类型测试
    // ================================================================

    @Test
    void testCallbackReturningInt() {
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("index", "Int"));

        ExternCallback callback = new ExternCallback("GetValueCallback", params, "Int");
        table.getAllCallbacks().add(callback);

        assertEquals("Int", callback.returnType);
    }

    @Test
    void testCallbackReturningPointer() {
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("key", "CString"));

        ExternCallback callback = new ExternCallback("LookupCallback", params, "Pointer");
        table.getAllCallbacks().add(callback);

        assertEquals("Pointer", callback.returnType);
    }

    @Test
    void testCallbackReturningCustomType() {
        // 声明一个 struct
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        table.getAllStructs().add(point);

        // 声明一个返回该 struct 指针的 callback
        List<ExternParam> params = new ArrayList<>();
        ExternCallback callback = newExternCallback("GetPoint", params, "Pointer");
        // 注意：这里返回的是 Pointer，实际使用需要解引用
        table.getAllCallbacks().add(callback);

        assertEquals(1, table.getAllStructs().size());
        assertEquals(1, table.getAllCallbacks().size());
    }

    // ================================================================
    //  4. 特殊 Callback 类型测试
    // ================================================================

    @Test
    void testCallbackWithCallingConvention() {
        // 测试调用约定
        List<ExternParam> params = new ArrayList<>();

        ExternCallback callback = new ExternCallback("StdCallCallback", params, "Void");
        callback.callingConvention = CallingConvention.STDCALL;
        table.getAllCallbacks().add(callback);

        assertEquals(CallingConvention.STDCALL, callback.callingConvention);
    }

    @Test
    void testCallbackWithNullableParam() {
        List<ExternParam> params = new ArrayList<>();
        ExternParam param = new ExternParam("data", "Pointer");
        param.nullable = true;
        params.add(param);

        ExternCallback callback = new ExternCallback("OptionalDataCallback", params, "Void");
        table.getAllCallbacks().add(callback);

        assertTrue(callback.params.get(0).nullable);
    }

    @Test
    void testCallbackWithDefaultValue() {
        List<ExternParam> params = new ArrayList<>();
        ExternParam param = new ExternParam("timeout", "Int");
        param.defaultValue = "30";
        params.add(param);

        ExternCallback callback = new ExternCallback("TimeoutCallback", params, "Void");
        table.getAllCallbacks().add(callback);

        assertEquals("30", callback.params.get(0).defaultValue);
    }

    // ================================================================
    //  5. Callback 与其他类型组合测试
    // ================================================================

    @Test
    void testCallbackWithStruct() {
        // 声明一个 struct
        ExternStruct event = new ExternStruct("Event");
        event.fields.add(new StructField("type", "Int"));
        event.fields.add(new StructField("timestamp", "Int64"));
        table.getAllStructs().add(event);

        // 声明一个接受 struct 指针的 callback
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("event", "Pointer"));

        ExternCallback eventCb = new ExternCallback("EventCallback", params, "Void");
        table.getAllCallbacks().add(eventCb);

        assertNotNull(table.getAllStructs().get(0));
        assertNotNull(table.getAllCallbacks().get(0));
    }

    @Test
    void testCallbackWithEnum() {
        // 声明一个 enum
        ExternEnum status = new ExternEnum("Status");
        status.members.add(new EnumMember("OK", "0"));
        status.members.add(new EnumMember("ERROR", "1"));
        table.getAllEnums().add(status);

        // 声明一个返回 enum 的 callback
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("code", "Int"));

        ExternCallback statusCb = new ExternCallback("GetStatusCallback", params, "Int");
        table.getAllCallbacks().add(statusCb);

        assertEquals(1, table.getAllEnums().size());
        assertEquals(1, table.getAllCallbacks().size());
    }

    // ================================================================
    //  6. 边界情况测试
    // ================================================================

    @Test
    void testEmptyCallback() {
        // 无参数无返回值的 callback
        ExternCallback callback = new ExternCallback("EmptyCallback",
            new ArrayList<>(), "Void");
        table.getAllCallbacks().add(callback);

        assertEquals(0, callback.params.size());
        assertEquals("Void", callback.returnType);
    }

    @Test
    void testManyParamsCallback() {
        // 多参数 callback（10个参数）
        List<ExternParam> params = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            params.add(new ExternParam("param" + i, "Int"));
        }

        ExternCallback callback = new ExternCallback("ManyParamsCallback", params, "Void");
        table.getAllCallbacks().add(callback);

        assertEquals(10, callback.params.size());
    }

    @Test
    void testCallbackMetadata() {
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("value", "Int"));

        ExternCallback callback = new ExternCallback("DescribedCallback", params, "Bool");
        callback.description = "A callback that processes values";
        table.getAllCallbacks().add(callback);

        assertEquals("A callback that processes values", callback.description);
    }

    @Test
    void testDuplicateCallbackNames() {
        // 允许多个同名 callback（不同块中）
        List<ExternParam> params = new ArrayList<>();

        ExternCallback cb1 = new ExternCallback("MyCallback", params, "Void");
        table.getAllCallbacks().add(cb1);

        ExternCallback cb2 = new ExternCallback("MyCallback", params, "Int");
        table.getAllCallbacks().add(cb2);

        // 验证两个 callback 都被添加
        assertEquals(2, table.getAllCallbacks().size());
    }

    // 辅助方法
    private ExternCallback newExternCallback(String name, List<ExternParam> params, String returnType) {
        return new ExternCallback(name, params, returnType);
    }
}

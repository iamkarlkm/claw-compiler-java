package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import com.q3lives.compiler.generators.ffi.platform.PlatformConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 特殊类型边界场景测试
 *
 * 覆盖：空 struct、空 enum、变参函数、可选链接库、版本约束
 */
class SpecialTypeTest {

    private FFIBindingTable table;

    @BeforeEach
    void setUp() {
        table = new FFIBindingTable();
    }

    // ================================================================
    //  1. 空 Struct/Enum 测试
    // ================================================================

    @Test
    void testEmptyStruct() {
        // 空 struct（C 中允许，但很少见）
        ExternStruct empty = new ExternStruct("EmptyStruct");
        // 不添加任何字段
        table.getAllStructs().add(empty);

        assertEquals("EmptyStruct", empty.name);
        assertTrue(empty.fields.isEmpty());
        assertEquals(0, empty.estimateSize());
    }

    @Test
    void testEmptyEnum() {
        // 空 enum（边界情况）
        ExternEnum empty = new ExternEnum("EmptyEnum");
        // 不添加任何成员
        table.getAllEnums().add(empty);

        assertEquals("EmptyEnum", empty.name);
        assertTrue(empty.members.isEmpty());
    }

    @Test
    void testStructWithOnlyPaddingFields() {
        // 只有填充字段的 struct（对齐用）
        ExternStruct padded = new ExternStruct("PaddedStruct");
        padded.fields.add(new StructField("a", "Int8"));
        padded.fields.add(new StructField("pad", "Int7")); // 填充字段
        padded.fields.add(new StructField("b", "Int32"));
        table.getAllStructs().add(padded);

        assertEquals(3, padded.fields.size());
    }

    // ================================================================
    //  2. 变参函数测试
    // ================================================================

    @Test
    void testVariadicFunction() {
        // 变参函数（printf 风格）
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("format", "CString"));

        ExternFunction printf = new ExternFunction("printf", params, "Int", true);
        table.getAllFunctions().put("printf", printf);

        assertTrue(printf.isVariadic);
        assertEquals(1, printf.params.size());
    }

    @Test
    void testVariadicFunctionNoFixedParams() {
        // 无固定参数的变参函数
        ExternFunction varargs = new ExternFunction("variadic_sum", new ArrayList<>(), "Int", true);
        table.getAllFunctions().put("variadic_sum", varargs);

        assertTrue(varargs.isVariadic);
        assertTrue(varargs.params.isEmpty());
    }

    @Test
    void testNonVariadicFunction() {
        // 普通函数（非变参）
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("x", "Int"));
        params.add(new ExternParam("y", "Int"));

        ExternFunction add = new ExternFunction("add", params, "Int", false);
        table.getAllFunctions().put("add", add);

        assertFalse(add.isVariadic);
    }

    // ================================================================
    //  3. 可选链接库测试
    // ================================================================

    @Test
    void testOptionalLink() {
        // 可选链接库（缺失不报错）
        LinkDirective link = new LinkDirective("optional_lib", "optional_lib.h");
        link.optional = true;
        table.getAllLinks().add(link);

        assertTrue(link.optional);
        assertEquals("optional_lib", link.libraryName);
    }

    @Test
    void testRequiredLink() {
        // 必需链接库
        LinkDirective link = new LinkDirective("required_lib", "required_lib.h");
        link.optional = false;
        table.getAllLinks().add(link);

        assertFalse(link.optional);
    }

    @Test
    void testMultipleLinks() {
        // 多个链接库
        LinkDirective lib1 = new LinkDirective("sqlite3", "sqlite3.h");
        LinkDirective lib2 = new LinkDirective("curl", "curl.h");
        LinkDirective lib3 = new LinkDirective("optional_dep", null);
        lib3.optional = true;

        table.getAllLinks().add(lib1);
        table.getAllLinks().add(lib2);
        table.getAllLinks().add(lib3);

        assertEquals(3, table.getAllLinks().size());
        assertFalse(table.getAllLinks().get(0).optional);
        assertTrue(table.getAllLinks().get(2).optional);
    }

    // ================================================================
    //  4. 版本约束测试
    // ================================================================

    @Test
    void testLinkWithMinVersion() {
        // 带最低版本要求的链接
        LinkDirective link = new LinkDirective("mylib", "mylib.h");
        link.minVersion = "2.0.0";
        table.getAllLinks().add(link);

        assertEquals("2.0.0", link.minVersion);
    }

    @Test
    void testLinkWithoutVersion() {
        // 无版本要求的链接
        LinkDirective link = new LinkDirective("somelib", "somelib.h");
        // minVersion 默认为 null
        table.getAllLinks().add(link);

        assertNull(link.minVersion);
    }

    @Test
    void testLinkWithSearchPath() {
        // 带搜索路径的链接
        LinkDirective link = new LinkDirective("mylib", "mylib.h");
        link.searchPath = "/usr/local/lib";
        table.getAllLinks().add(link);

        assertEquals("/usr/local/lib", link.searchPath);
    }

    // ================================================================
    //  5. 链接类型测试
    // ================================================================

    @Test
    void testDynamicLink() {
        // 动态链接（默认）
        LinkDirective link = new LinkDirective("dynamic_lib", "dynamic_lib.h");
        assertEquals(LinkType.DYNAMIC, link.linkType);

        table.getAllLinks().add(link);
        assertEquals(LinkType.DYNAMIC, table.getAllLinks().get(0).linkType);
    }

    @Test
    void testStaticLink() {
        // 静态链接
        LinkDirective link = new LinkDirective("static_lib", "static_lib.h");
        link.linkType = LinkType.STATIC;
        table.getAllLinks().add(link);

        assertEquals(LinkType.STATIC, link.linkType);
    }

    @Test
    void testFrameworkLink() {
        // macOS Framework
        LinkDirective link = new LinkDirective("Foundation", "Foundation.framework/Foundation");
        link.linkType = LinkType.FRAMEWORK;
        table.getAllLinks().add(link);

        assertEquals(LinkType.FRAMEWORK, link.linkType);
    }

    @Test
    void testHeaderOnlyLink() {
        // 仅头文件
        LinkDirective link = new LinkDirective("header_only_lib", null);
        link.linkType = LinkType.HEADER_ONLY;
        table.getAllLinks().add(link);

        assertEquals(LinkType.HEADER_ONLY, link.linkType);
    }

    // ================================================================
    //  6. 平台约束与特殊类型组合测试
    // ================================================================

    @Test
    void testPlatformSpecificFunction() {
        // 平台特定的函数
        List<ExternParam> params = new ArrayList<>();

        ExternFunction windowsOnly = new ExternFunction("GetLastError", params, "UInt32", false);
        windowsOnly.platformConstraint = PlatformConstraint.builder()
            .platform("windows")
            .build();
        table.getAllFunctions().put("GetLastError", windowsOnly);

        assertTrue(windowsOnly.platformConstraint.hasPlatformConstraint());
        assertFalse(windowsOnly.platformConstraint.hasArchConstraint());
    }

    @Test
    void testMultiPlatformFunction() {
        // 跨平台函数（无约束）
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("ptr", "Pointer"));

        ExternFunction universal = new ExternFunction("free", params, "Void", false);
        universal.platformConstraint = PlatformConstraint.UNIVERSAL;
        table.getAllFunctions().put("free", universal);

        assertTrue(universal.platformConstraint.isUniversal());
    }

    // ================================================================
    //  7. 函数元数据测试
    // ================================================================

    @Test
    void testDeprecatedFunction() {
        // 已废弃的函数
        List<ExternParam> params = new ArrayList<>();

        ExternFunction old = new ExternFunction("old_api", params, "Int", false);
        old.deprecated = true;
        old.deprecatedAlt = "new_api";
        table.getAllFunctions().put("old_api", old);

        assertTrue(old.deprecated);
        assertEquals("new_api", old.deprecatedAlt);
    }

    @Test
    void testThreadSafeFunction() {
        // 线程安全函数
        List<ExternParam> params = new ArrayList<>();

        ExternFunction safe = new ExternFunction("thread_safe_func", params, "Void", false);
        safe.threadSafety = ThreadSafety.THREAD_SAFE;
        table.getAllFunctions().put("thread_safe_func", safe);

        assertEquals(ThreadSafety.THREAD_SAFE, safe.threadSafety);
    }

    @Test
    void testNonThreadSafeFunction() {
        // 非线程安全函数
        List<ExternParam> params = new ArrayList<>();

        ExternFunction unsafe = new ExternFunction("unsafe_func", params, "Void", false);
        unsafe.threadSafety = ThreadSafety.NOT_THREAD_SAFE;
        table.getAllFunctions().put("unsafe_func", unsafe);

        assertEquals(ThreadSafety.NOT_THREAD_SAFE, unsafe.threadSafety);
    }

    @Test
    void testCanFailFunction() {
        // 可能失败的函数
        List<ExternParam> params = new ArrayList<>();

        ExternFunction risky = new ExternFunction("risky_operation", params, "Int", false);
        risky.canFail = true;
        risky.failureIndicator = "returns -1";
        table.getAllFunctions().put("risky_operation", risky);

        assertTrue(risky.canFail);
        assertEquals("returns -1", risky.failureIndicator);
    }

    // ================================================================
    //  8. 内存所有权测试
    // ================================================================

    @Test
    void testCallerOwnedReturn() {
        // 调用者拥有返回内存
        List<ExternParam> params = new ArrayList<>();

        ExternFunction func = new ExternFunction("alloc_buffer", params, "Pointer", false);
        func.returnOwnership = MemoryOwnership.CALLER_OWNS;
        table.getAllFunctions().put("alloc_buffer", func);

        assertEquals(MemoryOwnership.CALLER_OWNS, func.returnOwnership);
    }

    @Test
    void testCalleeOwnedReturn() {
        // 被调用者拥有返回内存（不需要释放）
        List<ExternParam> params = new ArrayList<>();

        ExternFunction func = new ExternFunction("get_static_string", params, "CString", false);
        func.returnOwnership = MemoryOwnership.CALLEE_OWNS;
        table.getAllFunctions().put("get_static_string", func);

        assertEquals(MemoryOwnership.CALLEE_OWNS, func.returnOwnership);
    }

    @Test
    void testStackReturn() {
        // 栈上返回（函数返回后无效）
        List<ExternParam> params = new ArrayList<>();
        params.add(new ExternParam("result", "Pointer"));

        ExternFunction func = new ExternFunction("get_result_ptr", params, "Pointer", false);
        func.returnOwnership = MemoryOwnership.STACK;
        table.getAllFunctions().put("get_result_ptr", func);

        assertEquals(MemoryOwnership.STACK, func.returnOwnership);
    }

    // ================================================================
    //  9. 调用约定测试
    // ================================================================

    @Test
    void testCdeclConvention() {
        // C 调用约定（默认）
        List<ExternParam> params = new ArrayList<>();

        ExternFunction func = new ExternFunction("cdecl_func", params, "Void", false);
        assertEquals(CallingConvention.CDECL, func.callingConvention);

        table.getAllFunctions().put("cdecl_func", func);
    }

    @Test
    void testStdcallConvention() {
        // StdCall 调用约定（Windows API）
        List<ExternParam> params = new ArrayList<>();

        ExternFunction func = new ExternFunction("winapi_func", params, "Void", false);
        func.callingConvention = CallingConvention.STDCALL;
        table.getAllFunctions().put("winapi_func", func);

        assertEquals(CallingConvention.STDCALL, func.callingConvention);
    }

    // ================================================================
    //  10. 边界值测试
    // ================================================================

    @Test
    void testMaxParams() {
        // 最大参数数量（模拟）
        List<ExternParam> params = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            params.add(new ExternParam("p" + i, "Int"));
        }

        ExternFunction func = new ExternFunction("many_params", params, "Void", false);
        table.getAllFunctions().put("many_params", func);

        assertEquals(100, func.params.size());
    }

    @Test
    void testManyConstants() {
        // 大量常量
        for (int i = 0; i < 100; i++) {
            ExternConstant c = new ExternConstant("CONST_" + i, "Int", String.valueOf(i));
            table.getAllConstants().put("CONST_" + i, c);
        }

        assertEquals(100, table.getAllConstants().size());
    }
}

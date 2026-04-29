package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import com.q3lives.compiler.generators.ffi.platform.PlatformConstraint;
import com.q3lives.compiler.generators.ffi.platform.TargetTriple;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FFI 综合集成测试
 *
 * 验证所有 FFI 组件协同工作，覆盖完整的使用场景
 */
class FFIComprehensiveTest {

    // ================================================================
    //  1. 完整 FFI 场景测试
    // ================================================================

    @Test
    void testCompleteFFIScenario() {
        FFIBindingTable table = new FFIBindingTable();

        // === 添加链接库 ===
        LinkDirective sqlite = new LinkDirective("sqlite3", "sqlite3.h");
        table.getAllLinks().add(sqlite);

        // === 添加类型别名 ===
        ExternType sqlite3 = new ExternType("sqlite3", "OpaquePointer");
        table.getAllTypes().put("sqlite3", sqlite3);

        // === 添加常量 ===
        table.getAllConstants().put("SQLITE_OK", new ExternConstant("SQLITE_OK", "Int", "0"));
        table.getAllConstants().put("SQLITE_ROW", new ExternConstant("SQLITE_ROW", "Int", "100"));

        // === 添加结构体 ===
        ExternStruct point = new ExternStruct("Point");
        point.fields.add(new StructField("x", "Float"));
        point.fields.add(new StructField("y", "Float"));
        table.getAllStructs().add(point);

        // === 添加枚举 ===
        ExternEnum status = new ExternEnum("Status");
        status.members.add(new EnumMember("OK", "0"));
        status.members.add(new EnumMember("ERROR", "1"));
        table.getAllEnums().add(status);

        // === 添加回调函数 ===
        List<ExternParam> cbParams = new ArrayList<>();
        cbParams.add(new ExternParam("result", "Int"));
        ExternCallback progressCb = new ExternCallback("ProgressCallback", cbParams, "Void");
        table.getAllCallbacks().add(progressCb);

        // === 添加外部函数 ===
        List<ExternParam> openParams = new ArrayList<>();
        openParams.add(new ExternParam("filename", "CString"));
        openParams.add(new ExternParam("ppDb", "Pointer"));
        ExternFunction sqliteOpen = new ExternFunction("sqlite3_open", openParams, "Int", false);
        sqliteOpen.description = "Open SQLite database";
        sqliteOpen.threadSafety = ThreadSafety.THREAD_SAFE;
        table.getAllFunctions().put("sqlite3_open", sqliteOpen);

        // === 验证所有数据已正确添加 ===
        assertTrue(table.hasExternDeclarations());
        assertEquals(1, table.getAllLinks().size());
        assertEquals(1, table.getAllTypes().size());
        assertEquals(2, table.getAllConstants().size());
        assertEquals(1, table.getAllStructs().size());
        assertEquals(1, table.getAllEnums().size());
        assertEquals(1, table.getAllCallbacks().size());
        assertEquals(1, table.getAllFunctions().size());

        // === 验证类型查找 ===
        assertNotNull(table.findType("sqlite3"));
        assertNotNull(table.findType("Point"));
        assertNotNull(table.findType("Status"));

        // === 验证符号查找 ===
        assertTrue(table.isExternSymbol("sqlite3_open"));
        assertTrue(table.isExternSymbol("SQLITE_OK"));
        assertTrue(table.isExternSymbol("Point"));
    }

    // ================================================================
    //  2. 平台过滤测试
    // ================================================================

    @Test
    void testPlatformFiltering() {
        // 创建两个 extern 块，一个用于 Windows，一个用于 Linux
        FFIBindingTable table = new FFIBindingTable();

        // Windows 特定函数
        ExternFunction winFunc = new ExternFunction("GetLastError", new ArrayList<>(), "UInt32", false);
        winFunc.platformConstraint = PlatformConstraint.builder().platform("windows").build();

        // Linux 特定函数
        ExternFunction linuxFunc = new ExternFunction("geterrno", new ArrayList<>(), "Int", false);
        linuxFunc.platformConstraint = PlatformConstraint.builder().platform("linux").build();

        // 通用函数
        ExternFunction commonFunc = new ExternFunction("malloc", new ArrayList<>(), "Pointer", false);
        commonFunc.platformConstraint = PlatformConstraint.UNIVERSAL;

        table.getAllFunctions().put("GetLastError", winFunc);
        table.getAllFunctions().put("geterrno", linuxFunc);
        table.getAllFunctions().put("malloc", commonFunc);

        // 验证平台约束
        assertTrue(winFunc.platformConstraint.hasPlatformConstraint());
        assertTrue(linuxFunc.platformConstraint.hasPlatformConstraint());
        assertTrue(commonFunc.platformConstraint.isUniversal());

        // 测试匹配
        TargetTriple windows = TargetTriple.parse("x86_64-windows-msvc");
        TargetTriple linux = TargetTriple.parse("x86_64-linux");

        assertTrue(winFunc.platformConstraint.matches(windows));
        assertFalse(winFunc.platformConstraint.matches(linux));

        assertFalse(linuxFunc.platformConstraint.matches(windows));
        assertTrue(linuxFunc.platformConstraint.matches(linux));

        assertTrue(commonFunc.platformConstraint.matches(windows));
        assertTrue(commonFunc.platformConstraint.matches(linux));
    }

    // ================================================================
    //  3. 跨平台库加载测试
    // ================================================================

    @Test
    void testCrossPlatformLibraryLoading() {
        FFIBindingTable table = new FFIBindingTable();

        // Windows 库
        LinkDirective windowsLib = new LinkDirective("kernel32", "windows.h");
        windowsLib.linkType = LinkType.DYNAMIC;

        // Unix 库
        LinkDirective unixLib = new LinkDirective("c", null); // libc 是 Unix 系统标准库

        // macOS Framework
        LinkDirective framework = new LinkDirective("Foundation", "Foundation.framework/Foundation");
        framework.linkType = LinkType.FRAMEWORK;

        table.getAllLinks().add(windowsLib);
        table.getAllLinks().add(unixLib);
        table.getAllLinks().add(framework);

        assertEquals(3, table.getAllLinks().size());
        assertEquals(LinkType.DYNAMIC, windowsLib.linkType);
        assertEquals(LinkType.FRAMEWORK, framework.linkType);
    }

    // ================================================================
    //  4. 复杂类型组合测试
    // ================================================================

    @Test
    void testComplexTypeCombination() {
        FFIBindingTable table = new FFIBindingTable();

        // 定义回调类型
        List<ExternParam> cbParams = new ArrayList<>();
        cbParams.add(new ExternParam("event", "Pointer"));
        ExternCallback eventCb = new ExternCallback("EventCallback", cbParams, "Void");
        eventCb.callingConvention = CallingConvention.CDECL;
        table.getAllCallbacks().add(eventCb);

        // 定义结构体（包含回调函数指针）
        ExternStruct handler = new ExternStruct("EventHandler");
        handler.fields.add(new StructField("onEvent", "Pointer")); // 存储回调指针
        handler.fields.add(new StructField("userData", "Pointer"));
        table.getAllStructs().add(handler);

        // 定义函数（接受结构体和回调）
        List<ExternParam> registerParams = new ArrayList<>();
        registerParams.add(new ExternParam("handler", "Pointer"));
        registerParams.add(new ExternParam("callback", "Pointer"));
        ExternFunction register = new ExternFunction("registerHandler", registerParams, "Int", false);
        register.canFail = true;
        register.failureIndicator = "returns -1";
        register.threadSafety = ThreadSafety.NOT_THREAD_SAFE;
        table.getAllFunctions().put("registerHandler", register);

        // 验证
        assertEquals(1, table.getAllCallbacks().size());
        assertEquals(1, table.getAllStructs().size());
        assertEquals(1, table.getAllFunctions().size());

        // 验证函数元数据
        assertTrue(register.canFail);
        assertEquals("returns -1", register.failureIndicator);
        assertEquals(ThreadSafety.NOT_THREAD_SAFE, register.threadSafety);
    }

    // ================================================================
    //  5. 内存所有权测试
    // ================================================================

    @Test
    void testMemoryOwnership() {
        FFIBindingTable table = new FFIBindingTable();

        // 调用者需要释放的函数
        ExternFunction alloc = new ExternFunction("alloc_buffer",
            List.of(new ExternParam("size", "Int")), "Pointer", false);
        alloc.returnOwnership = MemoryOwnership.CALLER_OWNS;
        table.getAllFunctions().put("alloc_buffer", alloc);

        // 被调用者管理的函数（静态字符串）
        ExternFunction getString = new ExternFunction("get_static_string",
            new ArrayList<>(), "CString", false);
        getString.returnOwnership = MemoryOwnership.CALLEE_OWNS;
        table.getAllFunctions().put("get_static_string", getString);

        // 栈上返回
        ExternFunction getLocal = new ExternFunction("get_local_ptr",
            List.of(new ExternParam("out", "Pointer")), "Void", false);
        getLocal.returnOwnership = MemoryOwnership.STACK;
        table.getAllFunctions().put("get_local_ptr", getLocal);

        // 验证
        assertEquals(MemoryOwnership.CALLER_OWNS, alloc.returnOwnership);
        assertEquals(MemoryOwnership.CALLEE_OWNS, getString.returnOwnership);
        assertEquals(MemoryOwnership.STACK, getLocal.returnOwnership);
    }

    // ================================================================
    //  6. 宏定义测试
    // ================================================================

    @Test
    void testMacroDefinitions() {
        FFIBindingTable table = new FFIBindingTable();

        // 常量宏
        ExternMacro maxConn = new ExternMacro("MAX_CONNECTIONS", MacroKind.CONSTANT);
        maxConn.type = "Int";
        maxConn.value = "1000";
        maxConn.description = "Maximum number of connections";
        table.getAllMacros().add(maxConn);

        // 函数宏
        ExternMacro max = new ExternMacro("MAX", MacroKind.FUNCTION);
        max.params = List.of(new ExternParam("a", "Int"), new ExternParam("b", "Int"));
        max.returnType = "Int";
        max.expansion = "(a > b ? a : b)";
        table.getAllMacros().add(max);

        // 验证
        assertEquals(2, table.getAllMacros().size());
        assertEquals(MacroKind.CONSTANT, maxConn.kind);
        assertEquals(MacroKind.FUNCTION, max.kind);
        assertEquals("1000", maxConn.value);
        assertEquals("(a > b ? a : b)", max.expansion);
    }

    // ================================================================
    //  7. 多线程安全测试
    // ================================================================

    @Test
    void testThreadSafetyAnnotations() {
        FFIBindingTable table = new FFIBindingTable();

        // 线程安全函数
        ExternFunction safe1 = new ExternFunction("thread_safe_inc",
            List.of(new ExternParam("counter", "Pointer")), "Void", false);
        safe1.threadSafety = ThreadSafety.THREAD_SAFE;

        // 可重入函数
        ExternFunction reentrant = new ExternFunction("reentrant_func",
            new ArrayList<>(), "Int", false);
        reentrant.threadSafety = ThreadSafety.REENTRANT;

        // 非线程安全函数
        ExternFunction unsafe = new StaticFunction("unsafe_global_state",
            new ArrayList<>(), "Int", false);
        unsafe.threadSafety = ThreadSafety.NOT_THREAD_SAFE;

        // 未知
        ExternFunction unknown = new ExternFunction("unknown_safety",
            new ArrayList<>(), "Void", false);

        table.getAllFunctions().put("thread_safe_inc", safe1);
        table.getAllFunctions().put("reentrant_func", reentrant);
        table.getAllFunctions().put("unknown_safety", unknown);

        assertEquals(ThreadSafety.THREAD_SAFE, safe1.threadSafety);
        assertEquals(ThreadSafety.REENTRANT, reentrant.threadSafety);
        assertEquals(ThreadSafety.NOT_THREAD_SAFE, unsafe.threadSafety);
        assertEquals(ThreadSafety.UNKNOWN, unknown.threadSafety);
    }

    // ================================================================
    //  8. 版本约束测试
    // ================================================================

    @Test
    void testVersionConstraints() {
        FFIBindingTable table = new FFIBindingTable();

        // 带版本的库
        LinkDirective libv1 = new LinkDirective("mylib", "mylib.h");
        libv1.minVersion = "1.0.0";
        libv1.searchPath = "/opt/mylib";

        // 验证
        assertEquals("1.0.0", libv1.minVersion);
        assertEquals("/opt/mylib", libv1.searchPath);

        table.getAllLinks().add(libv1);
        assertEquals(1, table.getAllLinks().size());
    }

    // 辅助类
    private static class StaticFunction extends ExternFunction {
        public StaticFunction(String name, List<ExternParam> params, String returnType, boolean isVariadic) {
            super(name, params, returnType, isVariadic);
        }
    }
}

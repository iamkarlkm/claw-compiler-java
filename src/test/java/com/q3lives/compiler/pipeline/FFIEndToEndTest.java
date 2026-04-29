package com.q3lives.compiler.pipeline;

import com.q3lives.ir.ClawIR;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FFI 端到端测试
 *
 * <p>验证 CompilationPipeline 能正确解析 extern "C" 块并将 FFIBindingTable 附加到 ClawIR。</p>
 */
class FFIEndToEndTest {

    // ================================================================
    //  1. 基础 extern 块解析
    // ================================================================

    @Test
    void testCompileWithExternBlock() {
        String source = """
            extern "C" {
              link "sqlite3"
              include "sqlite3.h"
              type sqlite3 = OpaquePointer
              const SQLITE_OK: Int = 0
              function sqlite3_open(filename: CString, ppDb: Ref<sqlite3>) -> Int
            }

            function main() -> Void {
              return
            }
            """;

        CompilationPipeline pipeline = new CompilationPipeline();
        CompilationResult result = pipeline.compile(source, "test.claw");

        assertTrue(result.isSuccess(), "编译应成功: " + result.getErrors());
        assertNotNull(result.getIR(), "IR 不应为空");
        assertTrue(result.getIR().hasFFIBindings(), "IR 应包含 FFI 绑定");
        assertNotNull(result.getIR().getFfiBindingTable(), "FFIBindingTable 不应为空");

        var table = result.getIR().getFfiBindingTable();
        assertEquals(1, table.getExternBlocks().size());
        assertEquals(1, table.getAllFunctions().size());
        assertEquals("sqlite3_open", table.getAllFunctions().get("sqlite3_open").name);
        assertEquals(1, table.getAllTypes().size());
        assertEquals(1, table.getAllConstants().size());
        assertEquals(1, table.getAllLinks().size());
    }

    @Test
    void testCompileWithoutExternBlock() {
        String source = """
            function main() -> Void {
              return
            }
            """;

        CompilationPipeline pipeline = new CompilationPipeline();
        CompilationResult result = pipeline.compile(source, "test.claw");

        assertTrue(result.isSuccess());
        assertNotNull(result.getIR());
        assertFalse(result.getIR().hasFFIBindings(), "无 extern 块的 IR 不应有 FFI 绑定");
    }

    // ================================================================
    //  2. struct/enum/callback/macro 解析
    // ================================================================

    @Test
    void testCompileWithStructAndEnum() {
        String source = """
            extern "C" {
              link "mylib"

              struct Point {
                x: Float
                y: Float
              }

              enum Status {
                OK = 0
                ERROR = 1
              }

              callback ProgressCallback(done: SizeT, total: SizeT) -> Void

              macro MAX_SIZE: Int = 1024

              function process(point: Ref<Point>) -> Status
            }

            function main() -> Void {
              return
            }
            """;

        CompilationPipeline pipeline = new CompilationPipeline();
        CompilationResult result = pipeline.compile(source, "test.claw");

        assertTrue(result.isSuccess(), "编译应成功: " + result.getErrors());
        assertTrue(result.getIR().hasFFIBindings());

        var table = result.getIR().getFfiBindingTable();
        assertEquals(1, table.getAllStructs().size(), "应解析出 1 个 struct");
        assertEquals("Point", table.getAllStructs().get(0).name);

        assertEquals(1, table.getAllEnums().size(), "应解析出 1 个 enum");
        assertEquals("Status", table.getAllEnums().get(0).name);

        assertEquals(1, table.getAllCallbacks().size(), "应解析出 1 个 callback");
        assertEquals("ProgressCallback", table.getAllCallbacks().get(0).name);

        assertEquals(1, table.getAllMacros().size(), "应解析出 1 个 macro");
        assertEquals("MAX_SIZE", table.getAllMacros().get(0).name);
    }

    // ================================================================
    //  3. 平台约束解析
    // ================================================================

    @Test
    void testCompileWithPlatformConstraint() {
        String source = """
            extern "C" @platform("windows") {
              link "kernel32"
              function GetLastError() -> UInt32
            }

            function main() -> Void {
              return
            }
            """;

        CompilationPipeline pipeline = new CompilationPipeline();
        CompilationResult result = pipeline.compile(source, "test.claw");

        assertTrue(result.isSuccess(), "编译应成功: " + result.getErrors());
        assertTrue(result.getIR().hasFFIBindings());

        var block = result.getIR().getFfiBindingTable().getExternBlocks().get(0);
        assertNotNull(block.platform);
        assertFalse(block.platform.isUniversal(), "平台约束不应是 universal");
    }

    // ================================================================
    //  4. 错误处理
    // ================================================================

    @Test
    void testCompileWithInvalidExternType() {
        String source = """
            extern "C" {
              link "testlib"
              function foo(x: UnknownCustomType) -> Void
            }

            function main() -> Void {
              return
            }
            """;

        CompilationPipeline pipeline = new CompilationPipeline();
        CompilationResult result = pipeline.compile(source, "test.claw");

        assertFalse(result.isSuccess(), "未知类型应导致编译失败");
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void testCompileWithUnterminatedExternBlock() {
        String source = """
            extern "C" {
              link "testlib"
              function foo() -> Void

            function main() -> Void {
              return
            }
            """;

        CompilationPipeline pipeline = new CompilationPipeline();
        CompilationResult result = pipeline.compile(source, "test.claw");

        assertFalse(result.isSuccess(), "未终止的 extern 块应导致编译失败");
    }

    // ================================================================
    //  5. 多个 extern 块
    // ================================================================

    @Test
    void testCompileWithMultipleExternBlocks() {
        String source = """
            extern "C" {
              link "sqlite3"
              function sqlite3_open(filename: CString, ppDb: Pointer) -> Int
            }

            extern "C" {
              link "curl"
              function curl_easy_init() -> Pointer
            }

            function main() -> Void {
              return
            }
            """;

        CompilationPipeline pipeline = new CompilationPipeline();
        CompilationResult result = pipeline.compile(source, "test.claw");

        assertTrue(result.isSuccess(), "编译应成功: " + result.getErrors());
        assertTrue(result.getIR().hasFFIBindings());

        var table = result.getIR().getFfiBindingTable();
        assertEquals(2, table.getExternBlocks().size(), "应解析出 2 个 extern 块");
        assertEquals(2, table.getAllFunctions().size(), "应解析出 2 个函数");
    }

    // ================================================================
    //  6. 综合场景
    // ================================================================

    @Test
    void testCompileWithComprehensiveFFI() {
        String source = """
            // FFI 声明
            extern "C" {
              link "mylib"
              include "mylib.h"

              type Handle = OpaquePointer
              const MAX_SIZE: Int = 1024

              struct Config {
                timeout: Int
                retries: Int
              }

              enum ErrorCode {
                NONE = 0
                TIMEOUT = 1
                RETRY = 2
              }

              callback ProgressCallback(done: SizeT, total: SizeT) -> Void

              macro PI: Float = 3.14159

              function init(config: Ref<Config>) -> Handle
              function process(data: Pointer, size: SizeT) -> Int
              function cleanup(handle: Handle) -> Void
            }

            // Claw 代码
            normal function main() -> Void {
              var result = 42
              return
            }
            """;

        CompilationPipeline pipeline = new CompilationPipeline();
        CompilationResult result = pipeline.compile(source, "test.claw");

        assertTrue(result.isSuccess(), "编译应成功: " + result.getErrors());
        assertTrue(result.getIR().hasFFIBindings());

        var table = result.getIR().getFfiBindingTable();
        assertEquals(1, table.getAllLinks().size());
        assertEquals(1, table.getAllIncludes().size());
        assertEquals(1, table.getAllTypes().size());
        assertEquals(1, table.getAllConstants().size());
        assertEquals(1, table.getAllStructs().size());
        assertEquals(1, table.getAllEnums().size());
        assertEquals(1, table.getAllCallbacks().size());
        assertEquals(1, table.getAllMacros().size());
        assertEquals(3, table.getAllFunctions().size());

        // 验证 IR 被正确设置
        ClawIR ir = result.getIR();
        assertNotNull(ir.getFfiBindingTable());
        assertTrue(ir.hasFFIBindings());
    }
}

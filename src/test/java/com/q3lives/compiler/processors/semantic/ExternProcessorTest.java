package com.q3lives.compiler.processors.semantic;

import com.q3lives.compiler.generators.ffi.FFIBindingTable;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import com.q3lives.compiler.generators.ffi.platform.PlatformConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExternProcessor 单元测试
 *
 * <p>验证 extern "C" 块解析：基础声明、struct、enum、callback、macro、平台约束。</p>
 */
class ExternProcessorTest {

    private FFIBindingTable table;
    private ExternProcessor processor;

    @BeforeEach
    void setUp() {
        table = new FFIBindingTable();
        processor = new ExternProcessor(table);
    }

    // ================================================================
    //  1. 基础声明解析
    // ================================================================

    @Test
    void testParseBasicDeclarations() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  link \"sqlite3\"",
            "  include \"sqlite3.h\"",
            "  type sqlite3 = OpaquePointer",
            "  const SQLITE_OK: Int = 0",
            "  function sqlite3_open(filename: CString, ppDb: Ref<sqlite3>) -> Int",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success, "处理应成功，无错误");
        assertFalse(processor.hasErrors(), "不应有错误");

        assertEquals(1, table.getExternBlocks().size());
        FFIBindingTable.ExternBlock block = table.getExternBlocks().get(0);

        assertEquals(1, block.links.size());
        assertEquals("sqlite3", block.links.get(0).libraryName);

        assertEquals(1, block.includes.size());
        assertEquals("sqlite3.h", block.includes.get(0));

        assertEquals(1, block.types.size());
        assertEquals("sqlite3", block.types.get(0).clawTypeName);

        assertEquals(1, block.constants.size());
        assertEquals("SQLITE_OK", block.constants.get(0).name);

        assertEquals(1, block.functions.size());
        ExternFunction func = block.functions.get(0);
        assertEquals("sqlite3_open", func.name);
        assertEquals(2, func.params.size());
        assertEquals("Int", func.returnType);
    }

    @Test
    void testParseEmptyBlock() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        assertEquals(1, table.getExternBlocks().size());
    }

    @Test
    void testParseMultipleBlocks() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  link \"sqlite3\"",
            "  function f1() -> Void",
            "}",
            "",
            "extern \"C\" {",
            "  link \"curl\"",
            "  function f2() -> Void",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        assertEquals(2, table.getExternBlocks().size());
        assertEquals(2, table.getAllFunctions().size()); // 两个块共两个函数
    }

    @Test
    void testUnterminatedBlock() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  link \"sqlite3\""
            // 缺少 }
        );

        boolean success = processor.process(lines, "test.claw");

        assertFalse(success, "未终止的块应报错");
        assertTrue(processor.hasErrors());
    }

    // ================================================================
    //  2. Struct 解析
    // ================================================================

    @Test
    void testParseStruct() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  link \"testlib\"",
            "  struct Point {",
            "    x: Float",
            "    y: Float",
            "  }",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        assertEquals(1, table.getAllStructs().size());

        ExternStruct struct = table.getAllStructs().get(0);
        assertEquals("Point", struct.name);
        assertEquals(2, struct.fields.size());
        assertEquals("x", struct.fields.get(0).name);
        assertEquals("Float", struct.fields.get(0).type);
        assertEquals("y", struct.fields.get(1).name);
        assertEquals("Float", struct.fields.get(1).type);
    }

    @Test
    void testParseStructEmpty() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  struct Empty {",
            "  }",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        assertEquals(1, table.getAllStructs().size());
        assertEquals(0, table.getAllStructs().get(0).fields.size());
    }

    // ================================================================
    //  3. Enum 解析
    // ================================================================

    @Test
    void testParseEnum() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  enum Status {",
            "    OK = 0",
            "    ERROR = -1",
            "    UNKNOWN",
            "  }",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        assertEquals(1, table.getAllEnums().size());

        ExternEnum enumType = table.getAllEnums().get(0);
        assertEquals("Status", enumType.name);
        assertEquals(3, enumType.members.size());
        assertEquals("OK", enumType.members.get(0).name);
        assertEquals("0", enumType.members.get(0).value);
        assertEquals("ERROR", enumType.members.get(1).name);
        assertEquals("-1", enumType.members.get(1).value);
        assertEquals("UNKNOWN", enumType.members.get(2).name);
        assertNull(enumType.members.get(2).value); // 无显式值
    }

    // ================================================================
    //  4. Callback 解析
    // ================================================================

    @Test
    void testParseCallback() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  callback WriteCallback(data: Pointer, size: SizeT) -> SizeT",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        assertEquals(1, table.getAllCallbacks().size());

        ExternCallback cb = table.getAllCallbacks().get(0);
        assertEquals("WriteCallback", cb.name);
        assertEquals(2, cb.params.size());
        assertEquals("SizeT", cb.returnType);
    }

    // ================================================================
    //  5. Macro 解析
    // ================================================================

    @Test
    void testParseMacroConstant() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  macro SQLITE_VERSION_NUMBER: Int = 3039004",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        assertEquals(1, table.getAllMacros().size());

        ExternMacro macro = table.getAllMacros().get(0);
        assertEquals("SQLITE_VERSION_NUMBER", macro.name);
        assertEquals(MacroKind.CONSTANT, macro.kind);
        assertEquals("Int", macro.type);
        assertEquals("3039004", macro.value);
    }

    @Test
    void testParseMacroFunction() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  macro MAX(a: Int, b: Int) -> Int = (a > b ? a : b)",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        assertEquals(1, table.getAllMacros().size());

        ExternMacro macro = table.getAllMacros().get(0);
        assertEquals("MAX", macro.name);
        assertEquals(MacroKind.FUNCTION, macro.kind);
        assertEquals("Int", macro.returnType);
        assertEquals(2, macro.params.size());
    }

    // ================================================================
    //  6. 平台约束解析
    // ================================================================

    @Test
    void testParsePlatformConstraint() {
        List<String> lines = Arrays.asList(
            "extern \"C\" @platform(\"windows\") {",
            "  link \"kernel32\"",
            "  function GetLastError() -> UInt32",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        assertEquals(1, table.getExternBlocks().size());

        FFIBindingTable.ExternBlock block = table.getExternBlocks().get(0);
        assertNotNull(block.platform);
        assertFalse(block.platform.isUniversal());
        assertTrue(block.platform.matches(new com.q3lives.compiler.generators.ffi.platform.TargetTriple("windows", "x86_64", null)));
        assertFalse(block.platform.matches(new com.q3lives.compiler.generators.ffi.platform.TargetTriple("linux", "x86_64", null)));
    }

    @Test
    void testParseMultiplePlatformsConstraint() {
        List<String> lines = Arrays.asList(
            "extern \"C\" @platform(\"windows\", \"linux\") {",
            "  link \"sharedlib\"",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        FFIBindingTable.ExternBlock block = table.getExternBlocks().get(0);
        assertTrue(block.platform.matches(new com.q3lives.compiler.generators.ffi.platform.TargetTriple("windows", "x86_64", null)));
        assertTrue(block.platform.matches(new com.q3lives.compiler.generators.ffi.platform.TargetTriple("linux", "x86_64", null)));
        assertFalse(block.platform.matches(new com.q3lives.compiler.generators.ffi.platform.TargetTriple("macos", "x86_64", null)));
    }

    // ================================================================
    //  7. 重复声明检测
    // ================================================================

    @Test
    void testDuplicateFunctionWarning() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  function foo() -> Void",
            "  function foo() -> Void",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success); // 警告不导致失败
        assertFalse(processor.getWarnings().isEmpty());
        assertTrue(processor.getWarnings().get(0).message.contains("Duplicate"));
    }

    @Test
    void testDuplicateStructWarning() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  struct Point {",
            "    x: Float",
            "  }",
            "  struct Point {",
            "    y: Float",
            "  }",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        assertFalse(processor.getWarnings().isEmpty());
    }

    // ================================================================
    //  8. 类型验证
    // ================================================================

    @Test
    void testInvalidTypeError() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  function foo(x: UnknownType) -> Void",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        // validateTypeReference 会对未知自定义类型报错
        assertFalse(success, "未知类型应导致验证失败");
        assertTrue(processor.hasErrors());
        assertTrue(processor.getErrors().get(0).message.contains("Unknown type"));
    }

    @Test
    void testValidFFITypes() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  function test1(x: Int) -> Void",
            "  function test2(x: Float) -> Int",
            "  function test3(x: String) -> Bool",
            "  function test4(x: Pointer) -> CString",
            "  function test5(x: Ref<Int>) -> CArray<Float>",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        assertEquals(5, table.getAllFunctions().size());
    }

    // ================================================================
    //  9. 无 extern 块的文件
    // ================================================================

    @Test
    void testNoExternBlock() {
        List<String> lines = Arrays.asList(
            "function main() -> Void {",
            "  return",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success);
        assertTrue(table.getExternBlocks().isEmpty());
    }

    // ================================================================
    //  10. 完整综合测试
    // ================================================================

    @Test
    void testComprehensiveExternBlock() {
        List<String> lines = Arrays.asList(
            "extern \"C\" {",
            "  link \"mylib\"",
            "  include \"mylib.h\"",
            "",
            "  type Handle = OpaquePointer",
            "",
            "  const MAX_SIZE: Int = 1024",
            "",
            "  struct Config {",
            "    timeout: Int",
            "    retries: Int",
            "  }",
            "",
            "  enum ErrorCode {",
            "    NONE = 0",
            "    TIMEOUT = 1",
            "    RETRY = 2",
            "  }",
            "",
            "  callback ProgressCallback(done: SizeT, total: SizeT) -> Void",
            "",
            "  macro PI: Float = 3.14159",
            "",
            "  function init(config: Ref<Config>) -> Handle",
            "  function process(data: Pointer, size: SizeT) -> Int",
            "  function cleanup(handle: Handle) -> Void",
            "}"
        );

        boolean success = processor.process(lines, "test.claw");

        assertTrue(success, "综合测试应无错误: " + processor.getErrors());
        assertFalse(processor.hasErrors());

        assertEquals(1, table.getExternBlocks().size());

        // 验证各类声明数量
        assertEquals(1, table.getAllLinks().size());
        assertEquals(1, table.getAllIncludes().size());
        assertEquals(1, table.getAllTypes().size());
        assertEquals(1, table.getAllConstants().size());
        assertEquals(1, table.getAllStructs().size());
        assertEquals(1, table.getAllEnums().size());
        assertEquals(1, table.getAllCallbacks().size());
        assertEquals(1, table.getAllMacros().size());
        assertEquals(3, table.getAllFunctions().size());
    }
}

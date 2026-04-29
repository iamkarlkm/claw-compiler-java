package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import com.q3lives.compiler.generators.ffi.platform.PlatformConstraint;
import com.q3lives.compiler.generators.ffi.platform.TargetTriple;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FFICompilationPipeline 测试
 */
class FFICompilationPipelineTest {

    // ================================================================
    //  1. 平台过滤测试
    // ================================================================

    @Test
    void testFilterForPlatformUniversal() {
        FFIBindingTable table = createTestTable();

        TargetTriple windows = TargetTriple.parse("x86_64-windows-msvc");
        FFIBindingTable filtered = table.filterForPlatform(windows);

        // 通用块应该被包含
        assertTrue(filtered.hasExternDeclarations());
    }

    @Test
    void testFilterForPlatformSpecific() {
        FFIBindingTable table = new FFIBindingTable();

        // 添加 Windows 特定的块
        ExternBlock windowsBlock = table.newExternBlock();
        windowsBlock.platform = PlatformConstraint.builder().platform("windows").build();
        windowsBlock.links.add(new LinkDirective("kernel32", "windows.h"));
        table.indexBlock(windowsBlock);

        // 添加 Linux 特定的块
        ExternBlock linuxBlock = table.newExternBlock();
        linuxBlock.platform = PlatformConstraint.builder().platform("linux").build();
        linuxBlock.links.add(new LinkDirective("c", null));
        table.indexBlock(linuxBlock);

        // 过滤 Windows
        TargetTriple windows = TargetTriple.parse("x86_64-windows-msvc");
        FFIBindingTable filtered = table.filterForPlatform(windows);

        // 应该只包含 Windows 块
        assertEquals(1, filtered.getExternBlocks().size());
    }

    @Test
    void testFilterForPlatformNoMatch() {
        FFIBindingTable table = new FFIBindingTable();

        // 只添加 Linux 特定的块
        ExternBlock linuxBlock = table.newExternBlock();
        linuxBlock.platform = PlatformConstraint.builder().platform("linux").build();
        linuxBlock.links.add(new LinkDirective("c", null));
        table.indexBlock(linuxBlock);

        // 过滤 Windows
        TargetTriple windows = TargetTriple.parse("x86_64-windows-msvc");
        FFIBindingTable filtered = table.filterForPlatform(windows);

        // 应该为空
        assertFalse(filtered.hasExternDeclarations());
    }

    // ================================================================
    //  2. 函数过滤测试
    // ================================================================

    @Test
    void testFilterFunctionsByPlatform() {
        FFIBindingTable table = new FFIBindingTable();

        // 通过 block 添加函数
        ExternBlock block = table.newExternBlock();
        block.platform = PlatformConstraint.UNIVERSAL;

        // 通用函数
        ExternFunction commonFunc = new ExternFunction("malloc",
            List.of(new ExternParam("size", "Int")), "Pointer", false);
        commonFunc.platformConstraint = PlatformConstraint.UNIVERSAL;
        block.functions.add(commonFunc);

        // Windows 函数
        ExternFunction windowsFunc = new ExternFunction("GetLastError",
            new ArrayList<>(), "UInt32", false);
        windowsFunc.platformConstraint = PlatformConstraint.builder().platform("windows").build();
        block.functions.add(windowsFunc);

        table.indexBlock(block);

        // 过滤 Windows
        TargetTriple windows = TargetTriple.parse("x86_64-windows-msvc");
        FFIBindingTable filtered = table.filterForPlatform(windows);

        // 两个函数都应该存在（块是 universal，函数有各自约束）
        assertNotNull(filtered.findFunction("malloc"));
        assertNotNull(filtered.findFunction("GetLastError"));
    }

    // ================================================================
    //  3. 链接库映射测试
    // ================================================================

    @Test
    void testLibraryNameMapping() {
        // 测试不同平台的库名映射
        String windowsLib = "kernel32";
        String unixLib = "c";

        // Windows 平台
        TargetTriple windows = TargetTriple.parse("x86_64-windows-msvc");

        // 验证库名获取
        FFIBindingTable table = new FFIBindingTable();
        table.getAllLinks().add(new LinkDirective(windowsLib, "windows.h"));
        table.getAllLinks().add(new LinkDirective(unixLib, null));

        List<String> libs = table.getLibraryNames();
        assertTrue(libs.contains(windowsLib));
    }

    // ================================================================
    //  4. FFICompilationPipeline 集成测试
    // ================================================================

    @Test
    void testFFICompilationPipelineProcess() {
        FFIBindingTable table = createTestTable();

        TargetTriple target = TargetTriple.detectHost();
        FFICompilationPipeline pipeline = new FFICompilationPipeline(target);

        FFICompilationPipeline.FFIGenerationResult result = pipeline.process(table);

        // 验证结果
        assertTrue(result.hasFFI);
        assertNotNull(result.filteredTable);
        assertNotNull(result.linkFlags);
    }

    @Test
    void testFFICompilationPipelineNoFFI() {
        FFIBindingTable table = new FFIBindingTable();

        // 不添加任何 extern 声明
        TargetTriple target = TargetTriple.detectHost();
        FFICompilationPipeline pipeline = new FFICompilationPipeline(target);

        FFICompilationPipeline.FFIGenerationResult result = pipeline.process(table);

        // 验证结果
        assertFalse(result.hasFFI);
        assertNull(result.filteredTable);
    }

    // ================================================================
    //  5. 多平台场景测试
    // ================================================================

    @Test
    void testMultiPlatformScenarios() {
        FFIBindingTable table = new FFIBindingTable();

        // 通用块
        ExternBlock block = table.newExternBlock();
        block.platform = PlatformConstraint.UNIVERSAL;

        // Windows 专用函数
        ExternFunction winFunc = new ExternFunction("WinSpecific",
            new ArrayList<>(), "Void", false);
        winFunc.platformConstraint = PlatformConstraint.builder()
            .platform("windows")
            .build();
        block.functions.add(winFunc);

        // macOS 专用函数
        ExternFunction macFunc = new ExternFunction("MacSpecific",
            new ArrayList<>(), "Void", false);
        macFunc.platformConstraint = PlatformConstraint.builder()
            .platform("macos")
            .build();
        block.functions.add(macFunc);

        // Linux 专用函数
        ExternFunction linuxFunc = new ExternFunction("LinuxSpecific",
            new ArrayList<>(), "Void", false);
        linuxFunc.platformConstraint = PlatformConstraint.builder()
            .platform("linux")
            .build();
        block.functions.add(linuxFunc);

        // 通用函数
        ExternFunction commonFunc = new ExternFunction("Common",
            new ArrayList<>(), "Void", false);
        commonFunc.platformConstraint = PlatformConstraint.UNIVERSAL;
        block.functions.add(commonFunc);

        table.indexBlock(block);

        // 测试 Windows
        TargetTriple windows = TargetTriple.parse("x86_64-windows-msvc");
        FFIBindingTable winFiltered = table.filterForPlatform(windows);
        assertNotNull(winFiltered.findFunction("WinSpecific"));
        assertNull(winFiltered.findFunction("MacSpecific"));
        assertNull(winFiltered.findFunction("LinuxSpecific"));
        assertNotNull(winFiltered.findFunction("Common"));

        // 测试 macOS
        TargetTriple macos = TargetTriple.parse("aarch64-macos");
        FFIBindingTable macFiltered = table.filterForPlatform(macos);
        assertNull(macFiltered.findFunction("WinSpecific"));
        assertNotNull(macFiltered.findFunction("MacSpecific"));
        assertNull(macFiltered.findFunction("LinuxSpecific"));
        assertNotNull(macFiltered.findFunction("Common"));

        // 测试 Linux
        TargetTriple linux = TargetTriple.parse("x86_64-linux");
        FFIBindingTable linuxFiltered = table.filterForPlatform(linux);
        assertNull(linuxFiltered.findFunction("WinSpecific"));
        assertNull(linuxFiltered.findFunction("MacSpecific"));
        assertNotNull(linuxFiltered.findFunction("LinuxSpecific"));
        assertNotNull(linuxFiltered.findFunction("Common"));
    }

    // ================================================================
    //  6. 架构约束测试
    // ================================================================

    @Test
    void testArchitectureConstraint() {
        FFIBindingTable table = new FFIBindingTable();

        // 通用块
        ExternBlock block = table.newExternBlock();
        block.platform = PlatformConstraint.UNIVERSAL;

        // x86_64 专用函数
        ExternFunction x64Func = new ExternFunction("X64Only",
            new ArrayList<>(), "Void", false);
        x64Func.platformConstraint = PlatformConstraint.builder()
            .arch("x86_64")
            .build();
        block.functions.add(x64Func);

        // ARM64 专用函数
        ExternFunction armFunc = new ExternFunction("ARM64Only",
            new ArrayList<>(), "Void", false);
        armFunc.platformConstraint = PlatformConstraint.builder()
            .arch("arm64")
            .build();
        block.functions.add(armFunc);

        table.indexBlock(block);

        // 测试 x86_64
        TargetTriple x64 = TargetTriple.parse("x86_64-linux");
        FFIBindingTable x64Filtered = table.filterForPlatform(x64);
        assertNotNull(x64Filtered.findFunction("X64Only"));
        assertNull(x64Filtered.findFunction("ARM64Only"));

        // 测试 ARM64
        TargetTriple arm = TargetTriple.parse("aarch64-linux");
        FFIBindingTable armFiltered = table.filterForPlatform(arm);
        assertNull(armFiltered.findFunction("X64Only"));
        assertNotNull(armFiltered.findFunction("ARM64Only"));
    }

    // ================================================================
    //  7. 组合约束测试
    // ================================================================

    @Test
    void testCombinedPlatformAndArch() {
        FFIBindingTable table = new FFIBindingTable();

        // 通用块
        ExternBlock block = table.newExternBlock();
        block.platform = PlatformConstraint.UNIVERSAL;

        // Windows x64 专用
        ExternFunction win64Func = new ExternFunction("Win64Only",
            new ArrayList<>(), "Void", false);
        win64Func.platformConstraint = PlatformConstraint.builder()
            .platform("windows")
            .arch("x86_64")
            .build();
        block.functions.add(win64Func);

        table.indexBlock(block);

        // Windows x86 应该不匹配
        TargetTriple win32 = TargetTriple.parse("i386-windows");
        FFIBindingTable win32Filtered = table.filterForPlatform(win32);
        assertNull(win32Filtered.findFunction("Win64Only"));

        // Windows x64 应该匹配
        TargetTriple win64 = TargetTriple.parse("x86_64-windows");
        FFIBindingTable win64Filtered = table.filterForPlatform(win64);
        assertNotNull(win64Filtered.findFunction("Win64Only"));
    }

    // 辅助方法
    private FFIBindingTable createTestTable() {
        FFIBindingTable table = new FFIBindingTable();

        // 添加通用块
        ExternBlock block = table.newExternBlock();
        block.platform = PlatformConstraint.UNIVERSAL;
        block.links.add(new LinkDirective("testlib", "testlib.h"));

        // 添加函数
        ExternFunction func = new ExternFunction("test_func",
            List.of(new ExternParam("x", "Int")), "Int", false);
        block.functions.add(func);

        // 添加常量
        block.constants.add(new ExternConstant("TEST_VALUE", "Int", "42"));

        table.indexBlock(block);

        return table;
    }
}

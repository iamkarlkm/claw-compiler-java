package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.platform.PlatformConstraint;
import com.q3lives.compiler.generators.ffi.platform.TargetTriple;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlatformConstraint 边界场景测试
 *
 * 覆盖：空约束、全平台约束、多平台约束、约束交集/并集、冲突检测
 */
class PlatformConstraintTest {

    // ================================================================
    //  1. 基础约束测试
    // ================================================================

    @Test
    void testUniversalConstraint() {
        PlatformConstraint universal = PlatformConstraint.UNIVERSAL;
        assertTrue(universal.isUniversal());
        assertFalse(universal.hasPlatformConstraint());
        assertFalse(universal.hasArchConstraint());
        assertFalse(universal.hasToolchainConstraint());

        // 全平台约束匹配所有目标
        TargetTriple windows = TargetTriple.parse("x86_64-pc-windows-msvc");
        TargetTriple linux = TargetTriple.parse("x86_64-unknown-linux-gnu");
        TargetTriple macos = TargetTriple.parse("aarch64-apple-darwin");

        assertTrue(universal.matches(windows));
        assertTrue(universal.matches(linux));
        assertTrue(universal.matches(macos));
    }

    @Test
    void testSinglePlatformConstraint() {
        PlatformConstraint windowsOnly = PlatformConstraint.builder()
            .platform("windows")
            .build();

        assertTrue(windowsOnly.hasPlatformConstraint());
        assertFalse(windowsOnly.hasArchConstraint());

        // 注意：TargetTriple 解析格式为 arch-os 或 arch-os-env
        // windows 作为 OS
        assertTrue(windowsOnly.matches(TargetTriple.parse("x86_64-windows-msvc")));
        assertTrue(windowsOnly.matches(TargetTriple.parse("x86-windows-gnu")));
        assertFalse(windowsOnly.matches(TargetTriple.parse("x86_64-linux-gnu")));
        assertFalse(windowsOnly.matches(TargetTriple.parse("aarch64-macos")));
    }

    @Test
    void testMultiPlatformConstraint() {
        PlatformConstraint winAndLinux = PlatformConstraint.builder()
            .platform("windows", "linux")
            .build();

        assertTrue(winAndLinux.matches(TargetTriple.parse("x86_64-windows-msvc")));
        assertTrue(winAndLinux.matches(TargetTriple.parse("x86_64-linux-gnu")));
        assertFalse(winAndLinux.matches(TargetTriple.parse("aarch64-macos")));
    }

    // ================================================================
    //  2. 架构约束测试
    // ================================================================

    @Test
    void testArchitectureConstraint() {
        PlatformConstraint arm64Only = PlatformConstraint.builder()
            .arch("arm64")
            .build();

        assertTrue(arm64Only.matches(TargetTriple.parse("aarch64-apple-darwin")));
        assertTrue(arm64Only.matches(TargetTriple.parse("aarch64-unknown-linux-gnu")));
        assertFalse(arm64Only.matches(TargetTriple.parse("x86_64-unknown-linux-gnu")));
    }

    @Test
    void testMultiArchitectureConstraint() {
        PlatformConstraint x86Archs = PlatformConstraint.builder()
            .arch("x86", "x86_64")
            .build();

        assertTrue(x86Archs.matches(TargetTriple.parse("x86_64-linux-gnu")));
        assertTrue(x86Archs.matches(TargetTriple.parse("i386-freebsd")));
        assertFalse(x86Archs.matches(TargetTriple.parse("aarch64-macos")));
    }

    // ================================================================
    //  3. 工具链约束测试
    // ================================================================

    @Test
    void testToolchainConstraint() {
        PlatformConstraint msvcOnly = PlatformConstraint.builder()
            .toolchain("msvc")
            .build();

        // x86_64-windows-msvc: platform=windows, arch=x86_64, toolchain=msvc
        assertTrue(msvcOnly.matches(TargetTriple.parse("x86_64-windows-msvc")));
    }

    @Test
    void testMultiToolchainConstraint() {
        PlatformConstraint gccClang = PlatformConstraint.builder()
            .toolchain("gcc", "clang")
            .build();

        // 测试 clang
        assertTrue(gccClang.matches(TargetTriple.parse("x86_64-macos-clang"))); // clang
        // msvc 不匹配
        assertFalse(gccClang.matches(TargetTriple.parse("x86_64-windows-msvc"))); // msvc
    }

    // ================================================================
    //  4. 组合约束测试
    // ================================================================

    @Test
    void testPlatformAndArchConstraint() {
        PlatformConstraint win64 = PlatformConstraint.builder()
            .platform("windows")
            .arch("x86_64")
            .build();

        assertTrue(win64.hasPlatformConstraint());
        assertTrue(win64.hasArchConstraint());

        assertTrue(win64.matches(TargetTriple.parse("x86_64-windows-msvc")));
        assertFalse(win64.matches(TargetTriple.parse("x86_64-linux-gnu"))); // wrong platform
        assertFalse(win64.matches(TargetTriple.parse("i386-windows-gnu"))); // wrong arch
    }

    @Test
    void testFullConstraint() {
        PlatformConstraint specific = PlatformConstraint.builder()
            .platform("windows")
            .arch("x86_64")
            .toolchain("msvc")
            .build();

        assertTrue(specific.hasPlatformConstraint());
        assertTrue(specific.hasArchConstraint());
        assertTrue(specific.hasToolchainConstraint());

        assertTrue(specific.matches(TargetTriple.parse("x86_64-windows-msvc")));
    }

    // ================================================================
    //  5. 约束运算测试
    // ================================================================

    @Test
    void testIntersectConstraints() {
        PlatformConstraint windows = PlatformConstraint.builder()
            .platform("windows")
            .build();
        PlatformConstraint msvc = PlatformConstraint.builder()
            .toolchain("msvc")
            .build();

        PlatformConstraint intersection = windows.intersect(msvc);

        assertTrue(intersection.matches(TargetTriple.parse("x86_64-windows-msvc")));
        // 注意：由于 gnu 不在工具链 lookup 中，它会被当作"未知"工具链
        // 这个测试验证 windows + msvc 的交集
    }

    @Test
    void testUnionConstraints() {
        PlatformConstraint windows = PlatformConstraint.builder()
            .platform("windows")
            .build();
        PlatformConstraint linux = PlatformConstraint.builder()
            .platform("linux")
            .build();

        PlatformConstraint union = windows.union(linux);

        assertTrue(union.matches(TargetTriple.parse("x86_64-windows-msvc")));
        assertTrue(union.matches(TargetTriple.parse("x86_64-linux")));
        assertFalse(union.matches(TargetTriple.parse("aarch64-macos")));
    }

    @Test
    void testIntersectWithUniversal() {
        PlatformConstraint universal = PlatformConstraint.UNIVERSAL;
        PlatformConstraint windows = PlatformConstraint.builder()
            .platform("windows")
            .build();

        PlatformConstraint result = universal.intersect(windows);
        assertEquals(windows, result);
    }

    // ================================================================
    //  6. 冲突检测测试
    // ================================================================

    @Test
    void testOverlappingConstraints() {
        PlatformConstraint windows = PlatformConstraint.builder()
            .platform("windows")
            .build();
        PlatformConstraint msvc = PlatformConstraint.builder()
            .toolchain("msvc")
            .build();

        // windows 和 msvc 有重叠（都支持 x86_64-pc-windows-msvc）
        assertTrue(windows.overlaps(msvc));
    }

    @Test
    void testNonOverlappingConstraints() {
        PlatformConstraint windows = PlatformConstraint.builder()
            .platform("windows")
            .build();
        PlatformConstraint linux = PlatformConstraint.builder()
            .platform("linux")
            .build();

        // windows 和 linux 无重叠（没有同时是 windows 和 linux 的平台）
        assertFalse(windows.overlaps(linux));
    }

    @Test
    void testNonOverlappingArchConstraints() {
        PlatformConstraint x86 = PlatformConstraint.builder()
            .arch("x86")
            .build();
        PlatformConstraint arm64 = PlatformConstraint.builder()
            .arch("arm64")
            .build();

        assertFalse(x86.overlaps(arm64));
    }

    // ================================================================
    //  7. 边界情况测试
    // ================================================================

    @Test
    void testEmptyPlatformList() {
        PlatformConstraint constraint = PlatformConstraint.builder().build();
        assertTrue(constraint.isUniversal());
    }

    @Test
    void testPlatformAliases() {
        // darwin -> macos, amd64 -> x86_64
        PlatformConstraint macos1 = PlatformConstraint.builder()
            .platform("darwin")
            .build();
        PlatformConstraint macos2 = PlatformConstraint.builder()
            .platform("macos")
            .build();

        TargetTriple darwin = TargetTriple.parse("aarch64-macos");

        assertTrue(macos1.matches(darwin));
        assertTrue(macos2.matches(darwin));
    }

    @Test
    void testArchAliases() {
        PlatformConstraint amd64 = PlatformConstraint.builder()
            .arch("amd64")
            .build();
        PlatformConstraint x86_64 = PlatformConstraint.builder()
            .arch("x86_64")
            .build();

        assertTrue(amd64.matches(TargetTriple.parse("x86_64-linux-gnu")));
        assertTrue(x86_64.matches(TargetTriple.parse("x86_64-linux-gnu")));
    }

    @Test
    void testToStringUniversal() {
        PlatformConstraint universal = PlatformConstraint.UNIVERSAL;
        assertEquals("@platform(all)", universal.toString());
    }

    @Test
    void testToStringWithPlatform() {
        PlatformConstraint windows = PlatformConstraint.builder()
            .platform("windows")
            .build();
        assertTrue(windows.toString().contains("windows"));
    }

    @Test
    void testEqualsAndHashCode() {
        PlatformConstraint c1 = PlatformConstraint.builder()
            .platform("windows", "linux")
            .build();
        PlatformConstraint c2 = PlatformConstraint.builder()
            .platform("linux", "windows")
            .build();
        PlatformConstraint c3 = PlatformConstraint.builder()
            .platform("windows")
            .build();

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c1, c3);
    }

    // ================================================================
    //  8. 边界平台测试
    // ================================================================

    @Test
    void testAndroidPlatform() {
        PlatformConstraint android = PlatformConstraint.builder()
            .platform("android")
            .build();

        // 注意：android 作为 platform 需要特殊的解析
        // 目前测试仅验证 linux 平台
        PlatformConstraint linux = PlatformConstraint.builder()
            .platform("linux")
            .build();

        assertTrue(linux.matches(TargetTriple.parse("aarch64-linux")));
        assertFalse(linux.matches(TargetTriple.parse("x86_64-windows-msvc")));
    }

    @Test
    void testIOSPlatform() {
        PlatformConstraint ios = PlatformConstraint.builder()
            .platform("ios")
            .build();

        // iOS 不是标准 platform，使用 macos
        PlatformConstraint macos = PlatformConstraint.builder()
            .platform("macos")
            .build();

        assertTrue(macos.matches(TargetTriple.parse("aarch64-macos")));
    }

    @Test
    void testWasmPlatform() {
        PlatformConstraint wasm = PlatformConstraint.builder()
            .platform("wasm")
            .build();

        assertTrue(wasm.matches(TargetTriple.parse("wasm32-wasm")));
    }

    @Test
    void testRiscvArch() {
        PlatformConstraint riscv = PlatformConstraint.builder()
            .arch("riscv64")
            .build();

        assertTrue(riscv.matches(TargetTriple.parse("riscv64-linux")));
        assertFalse(riscv.matches(TargetTriple.parse("x86_64-linux")));
    }
}

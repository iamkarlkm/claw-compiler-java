package com.q3lives.compiler.generators.ffi.platform;

/**
 * 编译目标三元组 — 支持位掩码快速匹配
 */
public final class TargetTriple {

    public final String platform;
    public final String architecture;
    public final String toolchain;

    /**
     * 预编码的位掩码（构造时计算一次，匹配时直接使用）
     *
     * 目标三元组的位掩码特点：每个维度恰好只有一个位为1
     * （与 PlatformConstraint 不同，后者一个维度可以有多个位为1）
     */
    private final long bitmask;

    public TargetTriple(String platform, String architecture, String toolchain) {
        this.platform = normalize(platform);
        this.architecture = normalize(architecture);
        this.toolchain = toolchain != null ? normalize(toolchain) : null;

        // 构造时一次性编码
        this.bitmask = encodeToBitmask();
    }

    private long encodeToBitmask() {
        long mask = 0L;

        // 平台编码
        Long platformBit = PlatformConstraint.PLATFORM_LOOKUP.get(this.platform);
        if (platformBit != null) mask |= platformBit;

        // 架构编码
        Long archBit = PlatformConstraint.ARCH_LOOKUP.get(this.architecture);
        if (archBit != null) mask |= archBit;

        // 工具链编码（可选）
        if (this.toolchain != null) {
            Long toolchainBit = PlatformConstraint.TOOLCHAIN_LOOKUP.get(this.toolchain);
            if (toolchainBit != null) mask |= toolchainBit;
        }

        return mask;
    }

    /**
     * 获取预编码的位掩码
     * PlatformConstraint.matchesBitmask() 使用
     */
    public long toBitmask() {
        return bitmask;
    }

    /**
     * 检测当前主机环境
     */
    public static TargetTriple detectHost() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        String platform;
        if (os.contains("win"))        platform = "windows";
        else if (os.contains("mac"))   platform = "macos";
        else if (os.contains("linux")) platform = "linux";
        else if (os.contains("freebsd")) platform = "freebsd";
        else platform = os;

        String architecture;
        if (arch.contains("amd64") || arch.contains("x86_64"))       architecture = "x86_64";
        else if (arch.contains("aarch64") || arch.contains("arm64")) architecture = "arm64";
        else if (arch.contains("x86") || arch.contains("i386"))      architecture = "x86";
        else if (arch.contains("arm"))                                architecture = "arm";
        else architecture = arch;

        return new TargetTriple(platform, architecture, null);
    }

    public static TargetTriple parse(String tripleStr) {
        String[] parts = tripleStr.split("-");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid target triple: " + tripleStr);
        }
        // 标准 LLVM triple 格式: arch[-vendor]-os[-env]
        // 例如: x86_64-linux-gnu, x86_64-windows-msvc, aarch64-macos
        // 第一部分是 architecture, 第二部分是 OS
        String arch = parts[0];
        String os = parts[1];
        String env = parts.length > 2 ? parts[2] : null;
        return new TargetTriple(os, arch, env);
    }

    // 便捷查询
    public boolean isWindows()  { return "windows".equals(platform); }
    public boolean isLinux()    { return "linux".equals(platform); }
    public boolean isMacOS()    { return "macos".equals(platform); }
    public boolean is64Bit() {
        return "x86_64".equals(architecture) || "arm64".equals(architecture);
    }
    public boolean isUnixLike() {
        return "linux".equals(platform) || "macos".equals(platform)
            || "freebsd".equals(platform) || "android".equals(platform);
    }

    public String dynamicLibExtension() {
        if (isWindows()) return ".dll";
        if (isMacOS())   return ".dylib";
        return ".so";
    }

    public String dynamicLibFileName(String libraryName) {
        String prefix = isWindows() ? "" : "lib";
        return prefix + libraryName + dynamicLibExtension();
    }

    private static String normalize(String s) {
        return s == null ? null : s.toLowerCase().trim();
    }

    @Override
    public String toString() {
        return platform + "-" + architecture + (toolchain != null ? "-" + toolchain : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TargetTriple)) return false;
        return bitmask == ((TargetTriple) o).bitmask;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(bitmask);
    }
}

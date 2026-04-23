package com.q3lives.compiler.generators.ffi.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 高性能平台约束 — 位掩码实现
 *
 * 将所有平台/架构/工具链编码为位标志：
 *   - 匹配操作：O(1) 的位与运算
 *   - 内存：一个 long（8字节）替代三个 Set<String>
 *   - 无 GC 压力：纯值类型运算
 *
 * 位布局（64位 long）：
 *   [63..48] 保留
 *   [47..32] 工具链标志 (16位, 支持16种工具链)
 *   [31..16] 架构标志   (16位, 支持16种架构)
 *   [15.. 0] 平台标志   (16位, 支持16种平台)
 */
public final class PlatformConstraint {

    // ================================================================
    //  位标志定义 — 平台 (bit 0-15)
    // ================================================================

    private static final long PLATFORM_WINDOWS = 1L;
    private static final long PLATFORM_LINUX   = 1L << 1;
    private static final long PLATFORM_MACOS   = 1L << 2;
    private static final long PLATFORM_ANDROID = 1L << 3;
    private static final long PLATFORM_IOS     = 1L << 4;
    private static final long PLATFORM_FREEBSD = 1L << 5;
    private static final long PLATFORM_WASM    = 1L << 6;
    // bit 7-15: 预留给未来平台

    /** 所有平台位的掩码 */
    public static final long PLATFORM_MASK = 0xFFFFL;

    /** 全平台标志（所有已定义平台的OR） */
    private static final long ALL_PLATFORMS =
        PLATFORM_WINDOWS | PLATFORM_LINUX | PLATFORM_MACOS |
        PLATFORM_ANDROID | PLATFORM_IOS | PLATFORM_FREEBSD | PLATFORM_WASM;

    // ================================================================
    //  位标志定义 — 架构 (bit 16-31)
    // ================================================================

    private static final long ARCH_X86_64  = 1L << 16;
    private static final long ARCH_X86     = 1L << 17;
    private static final long ARCH_ARM64   = 1L << 18;
    private static final long ARCH_ARM     = 1L << 19;
    private static final long ARCH_RISCV64 = 1L << 20;
    private static final long ARCH_WASM32  = 1L << 21;
    // bit 22-31: 预留

    private static final long ARCH_MASK = 0xFFFF0000L;
    private static final long ALL_ARCHS =
        ARCH_X86_64 | ARCH_X86 | ARCH_ARM64 | ARCH_ARM | ARCH_RISCV64 | ARCH_WASM32;

    // ================================================================
    //  位标志定义 — 工具链 (bit 32-47)
    // ================================================================

    private static final long TOOLCHAIN_GCC   = 1L << 32;
    private static final long TOOLCHAIN_CLANG = 1L << 33;
    private static final long TOOLCHAIN_MSVC  = 1L << 34;
    private static final long TOOLCHAIN_MINGW = 1L << 35;
    // bit 36-47: 预留

    private static final long TOOLCHAIN_MASK = 0xFFFF00000000L;
    private static final long ALL_TOOLCHAINS =
        TOOLCHAIN_GCC | TOOLCHAIN_CLANG | TOOLCHAIN_MSVC | TOOLCHAIN_MINGW;

    // ================================================================
    //  字符串 → 位标志的查找表（解析阶段使用，只查一次）
    // ================================================================

    public static final Map<String, Long> PLATFORM_LOOKUP = new HashMap<>();
    public static final Map<String, Long> ARCH_LOOKUP = new HashMap<>();
    public static final Map<String, Long> TOOLCHAIN_LOOKUP = new HashMap<>();

    static {
        PLATFORM_LOOKUP.put("windows", PLATFORM_WINDOWS);
        PLATFORM_LOOKUP.put("linux",   PLATFORM_LINUX);
        PLATFORM_LOOKUP.put("macos",   PLATFORM_MACOS);
        PLATFORM_LOOKUP.put("darwin",  PLATFORM_MACOS);  // 别名
        PLATFORM_LOOKUP.put("android", PLATFORM_ANDROID);
        PLATFORM_LOOKUP.put("ios",     PLATFORM_IOS);
        PLATFORM_LOOKUP.put("freebsd", PLATFORM_FREEBSD);
        PLATFORM_LOOKUP.put("wasm",    PLATFORM_WASM);

        ARCH_LOOKUP.put("x86_64",  ARCH_X86_64);
        ARCH_LOOKUP.put("amd64",   ARCH_X86_64);  // 别名
        ARCH_LOOKUP.put("x86",     ARCH_X86);
        ARCH_LOOKUP.put("i386",    ARCH_X86);      // 别名
        ARCH_LOOKUP.put("i686",    ARCH_X86);      // 别名
        ARCH_LOOKUP.put("arm64",   ARCH_ARM64);
        ARCH_LOOKUP.put("aarch64", ARCH_ARM64);    // 别名
        ARCH_LOOKUP.put("arm",     ARCH_ARM);
        ARCH_LOOKUP.put("armv7l",  ARCH_ARM);      // 别名
        ARCH_LOOKUP.put("riscv64", ARCH_RISCV64);
        ARCH_LOOKUP.put("wasm32",  ARCH_WASM32);

        TOOLCHAIN_LOOKUP.put("gcc",   TOOLCHAIN_GCC);
        TOOLCHAIN_LOOKUP.put("clang", TOOLCHAIN_CLANG);
        TOOLCHAIN_LOOKUP.put("msvc",  TOOLCHAIN_MSVC);
        TOOLCHAIN_LOOKUP.put("mingw", TOOLCHAIN_MINGW);
    }

    // ================================================================
    //  核心数据：一个 long 存储所有约束
    // ================================================================

    /**
     * 约束位掩码
     *
     * 语义：
     *   - 某个维度的所有位都为0 → 该维度不限制（匹配任何值）
     *   - 某个维度的某些位为1 → 目标必须匹配其中之一（OR语义）
     */
    private final long constraintMask;

    /**
     * 预计算的匹配掩码（用于快速匹配）
     *
     * 对于不限制的维度，填充为全1，这样 AND 运算时总是通过
     */
    private final long matchMask;

    // ================================================================
    //  构造方法
    // ================================================================

    private PlatformConstraint(long constraintMask) {
        this.constraintMask = constraintMask;

        // 预计算 matchMask：空维度填全1
        long mask = constraintMask;
        if ((mask & PLATFORM_MASK) == 0)   mask |= PLATFORM_MASK;
        if ((mask & ARCH_MASK) == 0)       mask |= ARCH_MASK;
        if ((mask & TOOLCHAIN_MASK) == 0)  mask |= TOOLCHAIN_MASK;
        this.matchMask = mask;
    }

    /** 兼容默认构造函数 */
    public PlatformConstraint() {
        this(0L);
    }

    /** 全平台约束（无限制） */
    public static final PlatformConstraint UNIVERSAL = new PlatformConstraint(0L);

    // ================================================================
    //  兼容 API（来自旧的 FFIBindingTable.PlatformConstraint）
    // ================================================================

    /**
     * 添加平台约束（返回新实例，支持链式调用）
     */
    public PlatformConstraint addPlatform(String... platforms) {
        long newMask = this.constraintMask;
        for (String name : platforms) {
            Long bit = PLATFORM_LOOKUP.get(name.toLowerCase().trim());
            if (bit != null) {
                newMask |= bit;
            }
        }
        return new PlatformConstraint(newMask);
    }

    /**
     * 添加架构约束（返回新实例，支持链式调用）
     */
    public PlatformConstraint addArchitecture(String... archs) {
        long newMask = this.constraintMask;
        for (String name : archs) {
            Long bit = ARCH_LOOKUP.get(name.toLowerCase().trim());
            if (bit != null) {
                newMask |= bit;
            }
        }
        return new PlatformConstraint(newMask);
    }

    /**
     * 添加工具链约束（返回新实例，支持链式调用）
     */
    public PlatformConstraint addToolchain(String... toolchains) {
        long newMask = this.constraintMask;
        for (String name : toolchains) {
            Long bit = TOOLCHAIN_LOOKUP.get(name.toLowerCase().trim());
            if (bit != null) {
                newMask |= bit;
            }
        }
        return new PlatformConstraint(newMask);
    }

    // ================================================================
    //  Builder（解析阶段使用）
    // ================================================================

    public static class Builder {
        private long mask = 0L;

        public Builder platform(String... names) {
            for (String name : names) {
                Long bit = PLATFORM_LOOKUP.get(name.toLowerCase().trim());
                if (bit != null) {
                    mask |= bit;
                } else {
                    throw new IllegalArgumentException("Unknown platform: " + name
                        + ". Known: " + PLATFORM_LOOKUP.keySet());
                }
            }
            return this;
        }

        public Builder arch(String... names) {
            for (String name : names) {
                Long bit = ARCH_LOOKUP.get(name.toLowerCase().trim());
                if (bit != null) {
                    mask |= bit;
                } else {
                    throw new IllegalArgumentException("Unknown architecture: " + name
                        + ". Known: " + ARCH_LOOKUP.keySet());
                }
            }
            return this;
        }

        public Builder toolchain(String... names) {
            for (String name : names) {
                Long bit = TOOLCHAIN_LOOKUP.get(name.toLowerCase().trim());
                if (bit != null) {
                    mask |= bit;
                } else {
                    throw new IllegalArgumentException("Unknown toolchain: " + name
                        + ". Known: " + TOOLCHAIN_LOOKUP.keySet());
                }
            }
            return this;
        }

        public PlatformConstraint build() {
            return new PlatformConstraint(mask);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // ================================================================
    //  ★ 核心：O(1) 匹配
    // ================================================================

    /**
     * 匹配目标三元组 — 单次位与运算
     *
     * 性能：固定 O(1)，无分支、无循环、无字符串比较
     *
     * 原理：
     *   target 编码为一个 long（每个维度恰好一个位为1）
     *   constraint 的 matchMask 中，受限维度只有允许的位为1，不受限维度全1
     *   (targetBits & matchMask) == targetBits 当且仅当目标的每个维度都在允许范围内
     */
    public boolean matches(TargetTriple target) {
        long targetBits = target.toBitmask();
        return (targetBits & matchMask) == targetBits;
    }

    /**
     * 快速匹配 — 直接接受预编码的目标位掩码
     *
     * 当需要对同一目标重复匹配多个约束时，避免重复编码
     */
    public boolean matchesBitmask(long targetBits) {
        return (targetBits & matchMask) == targetBits;
    }

    // ================================================================
    //  集合运算（同样用位运算，O(1)）
    // ================================================================

    /**
     * 交集：两个约束的共同子集
     *
     * 用于嵌套 extern 块的约束合并
     */
    public PlatformConstraint intersect(PlatformConstraint other) {
        // 对每个维度：
        //   如果两方都有约束 → 取交集（AND）
        //   如果只有一方有约束 → 取该方的约束
        //   如果都没有约束 → 无约束

        long result = 0L;

        long thisPlatforms  = this.constraintMask & PLATFORM_MASK;
        long otherPlatforms = other.constraintMask & PLATFORM_MASK;
        if (thisPlatforms != 0 && otherPlatforms != 0) {
            result |= (thisPlatforms & otherPlatforms);  // 交集
        } else {
            result |= (thisPlatforms | otherPlatforms);  // 取有值的一方
        }

        long thisArchs  = this.constraintMask & ARCH_MASK;
        long otherArchs = other.constraintMask & ARCH_MASK;
        if (thisArchs != 0 && otherArchs != 0) {
            result |= (thisArchs & otherArchs);
        } else {
            result |= (thisArchs | otherArchs);
        }

        long thisToolchains  = this.constraintMask & TOOLCHAIN_MASK;
        long otherToolchains = other.constraintMask & TOOLCHAIN_MASK;
        if (thisToolchains != 0 && otherToolchains != 0) {
            result |= (thisToolchains & otherToolchains);
        } else {
            result |= (thisToolchains | otherToolchains);
        }

        return new PlatformConstraint(result);
    }

    /**
     * 并集：两个约束的合并
     */
    public PlatformConstraint union(PlatformConstraint other) {
        long result = 0L;

        long thisPlatforms  = this.constraintMask & PLATFORM_MASK;
        long otherPlatforms = other.constraintMask & PLATFORM_MASK;
        if (thisPlatforms == 0 || otherPlatforms == 0) {
            // 有一方无约束 → 结果也无约束（该维度不限制）
        } else {
            result |= (thisPlatforms | otherPlatforms);
        }

        long thisArchs  = this.constraintMask & ARCH_MASK;
        long otherArchs = other.constraintMask & ARCH_MASK;
        if (thisArchs == 0 || otherArchs == 0) {
            // 无约束
        } else {
            result |= (thisArchs | otherArchs);
        }

        long thisToolchains  = this.constraintMask & TOOLCHAIN_MASK;
        long otherToolchains = other.constraintMask & TOOLCHAIN_MASK;
        if (thisToolchains == 0 || otherToolchains == 0) {
            // 无约束
        } else {
            result |= (thisToolchains | otherToolchains);
        }

        return new PlatformConstraint(result);
    }

    /**
     * 检查两个约束是否有重叠（可能冲突）
     */
    public boolean overlaps(PlatformConstraint other) {
        // 两个约束重叠 = 存在某个目标三元组同时满足两者
        // 等价于交集非空

        long thisPlatforms  = this.constraintMask & PLATFORM_MASK;
        long otherPlatforms = other.constraintMask & PLATFORM_MASK;
        if (thisPlatforms != 0 && otherPlatforms != 0
            && (thisPlatforms & otherPlatforms) == 0) {
            return false;  // 平台维度无交集
        }

        long thisArchs  = this.constraintMask & ARCH_MASK;
        long otherArchs = other.constraintMask & ARCH_MASK;
        if (thisArchs != 0 && otherArchs != 0
            && (thisArchs & otherArchs) == 0) {
            return false;  // 架构维度无交集
        }

        return true;
    }

    // ================================================================
    //  查询方法
    // ================================================================

    public boolean isUniversal() {
        return constraintMask == 0L;
    }

    public boolean hasPlatformConstraint() {
        return (constraintMask & PLATFORM_MASK) != 0;
    }

    public boolean hasArchConstraint() {
        return (constraintMask & ARCH_MASK) != 0;
    }

    public boolean hasToolchainConstraint() {
        return (constraintMask & TOOLCHAIN_MASK) != 0;
    }

    /** 获取原始位掩码（用于序列化、缓存键等） */
    public long getRawMask() {
        return constraintMask;
    }

    // ================================================================
    //  反向解码（调试/显示用，不在热路径上）
    // ================================================================

    public List<String> getPlatforms() {
        return decodeBits(constraintMask & PLATFORM_MASK, PLATFORM_LOOKUP);
    }

    public List<String> getArchitectures() {
        return decodeBits(constraintMask & ARCH_MASK, ARCH_LOOKUP);
    }

    public List<String> getToolchains() {
        return decodeBits(constraintMask & TOOLCHAIN_MASK, TOOLCHAIN_LOOKUP);
    }

    private static List<String> decodeBits(long bits, Map<String, Long> lookup) {
        if (bits == 0) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        // 反向查找，跳过别名（只取主名称）
        Set<Long> seen = new HashSet<>();
        for (Map.Entry<String, Long> entry : lookup.entrySet()) {
            if ((bits & entry.getValue()) != 0 && seen.add(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    @Override
    public String toString() {
        if (isUniversal()) return "@platform(all)";
        StringBuilder sb = new StringBuilder();
        List<String> p = getPlatforms();
        if (!p.isEmpty()) sb.append("@platform(").append(String.join(",", p)).append(")");
        List<String> a = getArchitectures();
        if (!a.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("@arch(").append(String.join(",", a)).append(")");
        }
        List<String> t = getToolchains();
        if (!t.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("@toolchain(").append(String.join(",", t)).append(")");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlatformConstraint)) return false;
        return constraintMask == ((PlatformConstraint) o).constraintMask;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(constraintMask);
    }
}

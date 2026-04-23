package com.q3lives.compiler.generators.ffi.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台特定的库名映射
 *
 * 处理同一逻辑库在不同操作系统上名称不同的情况
 */
public class PlatformLibraryMapper {

    /**
     * 标准库映射表
     *
     * 结构: 通用名 → { 平台 → 实际库名 }
     *
     * null 表示该平台不需要显式链接（系统默认提供）
     */
    private static final Map<String, Map<String, String>> LIBRARY_MAP = new HashMap<>();

    static {
        // 数学库
        Map<String, String> mathLib = new HashMap<>();
        mathLib.put("linux",   "m");
        mathLib.put("macos",   null);  // macOS libc 已包含
        mathLib.put("windows", null);  // MSVC CRT 已包含
        mathLib.put("android", "m");
        LIBRARY_MAP.put("m", mathLib);

        // 线程库
        Map<String, String> threadLib = new HashMap<>();
        threadLib.put("linux",   "pthread");
        threadLib.put("macos",   "pthread");
        threadLib.put("windows", null);  // Windows 用 kernel32 中的 CreateThread
        threadLib.put("android", null);  // Bionic libc 已包含
        LIBRARY_MAP.put("pthread", threadLib);

        // 动态加载库
        Map<String, String> dlLib = new HashMap<>();
        dlLib.put("linux",   "dl");
        dlLib.put("macos",   null);  // 已内置
        dlLib.put("windows", null);  // 用 LoadLibrary
        LIBRARY_MAP.put("dl", dlLib);

        // Socket 库
        Map<String, String> socketLib = new HashMap<>();
        socketLib.put("linux",   null);    // libc 已包含
        socketLib.put("macos",   null);
        socketLib.put("windows", "ws2_32");  // Winsock
        LIBRARY_MAP.put("socket", socketLib);
    }

    /**
     * 映射库名
     *
     * @return 映射后的库名, null 表示该平台不需要此库
     */
    public static String mapLibraryName(String libraryName, TargetTriple target) {
        Map<String, String> platformMap = LIBRARY_MAP.get(libraryName);

        if (platformMap == null) {
            // 没有特殊映射规则，原名返回
            return libraryName;
        }

        if (platformMap.containsKey(target.platform)) {
            return platformMap.get(target.platform);  // 可能返回 null
        }

        // 没有该平台的映射，返回原名
        return libraryName;
    }

    /**
     * 获取一个库在目标平台上需要的所有隐含库
     *
     * 例如: 在 Windows 上使用网络功能，除了 ws2_32 还需要 mswsock
     */
    public static List<String> getImpliedLibraries(String libraryName, TargetTriple target) {
        List<String> implied = new ArrayList<>();

        if (target.isWindows()) {
            if ("ws2_32".equals(libraryName) || "socket".equals(libraryName)) {
                implied.add("mswsock");
            }
            if ("openssl".equals(libraryName)) {
                implied.add("crypt32");
                implied.add("ws2_32");
            }
        }

        if (target.isLinux()) {
            if ("openssl".equals(libraryName)) {
                implied.add("ssl");
                implied.add("crypto");
            }
        }

        return implied;
    }
}

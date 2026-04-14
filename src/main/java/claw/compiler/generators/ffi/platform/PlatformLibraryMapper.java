package claw.compiler.generators.ffi.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台特定的库名映射器
 *
 * 处理同一逻辑库在不同操作系统上名称不同的情况，并自动检测隐含库
 *
 * 支持的库类型:
 * - 系统库 (math, pthread, dl, socket)
 * - 加密库 (openssl, crypto)
 * - 网络库 (ssl, curl, libcurl)
 * - 数据库库 (sqlite3, mysqlclient, postgresql)
 * - 图形库 (glfw, sdl)
 * - 其他第三方库
 *
 * 使用示例:
 * <pre>
 * // 基本映射
 * String lib = PlatformLibraryMapper.mapLibraryName("sqlite3", targetTriple);
 * // Windows: sqlite3.dll
 * // Linux:   libsqlite3.so
 * // macOS:   libsqlite3.dylib
 *
 * // 获取隐含库
 * List<String> libs = PlatformLibraryMapper.getImpliedLibraries("openssl", targetTriple);
 * // Windows: [crypt32, ws2_32]
 * // Linux:   [ssl, crypto]
 * </pre>
 */
public class PlatformLibraryMapper {

    /**
     * 标准库映射表
     *
     * 结构: 通用名 → { 平台 → 实际库名, 搜索路径（可选） }
     */
    private static final Map<String, Map<String, String>> LIBRARY_MAP = new LinkedHashMap<>();

    static {
        // ==================== 系统库 ====================

        // 数学库
        registerLibrary("m", new HashMap<String, String>() {{
            put("linux", "m");
            put("macos", null);  // macOS libc 已包含
            put("windows", null);  // MSVC CRT 已包含
            put("android", "m");
            put("freebsd", "m");
        }});

        // 线程库
        registerLibrary("pthread", new HashMap<String, String>() {{
            put("linux", "pthread");
            put("macos", "pthread");
            put("windows", null);  // Windows 用 kernel32 中的 CreateThread
            put("android", null);  // Bionic libc 已包含
            put("freebsd", "pthread");
        }});

        // 动态加载库 (dlopen/dlsym)
        registerLibrary("dl", new HashMap<String, String>() {{
            put("linux", "dl");
            put("macos", null);  // 已内置
            put("windows", null);  // 用 LoadLibrary
            put("freebsd", "dl");
        }});

        // Socket 库
        registerLibrary("socket", new HashMap<String, String>() {{
            put("linux", null);    // libc 已包含
            put("macos", null);
            put("windows", "ws2_32");  // Winsock
            put("freebsd", null);
        }});

        // 网络库 (IPv4/IPv6)
        registerLibrary("network", new HashMap<String, String>() {{
            put("linux", null);    // libc 已包含
            put("macos", null);
            put("windows", "ws2_32");  // Winsock
            put("freebsd", null);
        }});

        // ==================== 加密库 ====================

        // OpenSSL
        registerLibrary("openssl", new HashMap<String, String>() {{
            put("linux", "ssl");
            put("macos", "ssl");
            put("windows", "libssl-3-x64");  // 注意：Windows 上可能有多个版本
            put("android", "ssl");
            put("freebsd", "ssl");
        }});

        // 加密核心库
        registerLibrary("crypto", new HashMap<String, String>() {{
            put("linux", "crypto");
            put("macos", "crypto");
            put("windows", "libcrypto-3-x64");
            put("android", "crypto");
            put("freebsd", "crypto");
        }});

        // ==================== 数据库库 ====================

        // SQLite3
        registerLibrary("sqlite3", new HashMap<String, String>() {{
            put("linux", "sqlite3");
            put("macos", "sqlite3");
            put("windows", "sqlite3");
            put("android", "sqlite3");
            put("freebsd", "sqlite3");
        }});

        // MySQL 客户端
        registerLibrary("mysqlclient", new HashMap<String, String>() {{
            put("linux", "mysqlclient");
            put("macos", "mysqlclient");
            put("windows", "mysql");
            put("android", null);
            put("freebsd", "mysqlclient");
        }});

        // PostgreSQL 客户端
        registerLibrary("pq", new HashMap<String, String>() {{
            put("linux", "pq");
            put("macos", "pq");
            put("windows", "libpq");
            put("android", null);
            put("freebsd", "pq");
        }});

        // ==================== 图形库 ====================

        // GLFW
        registerLibrary("glfw", new HashMap<String, String>() {{
            put("linux", "glfw");
            put("macos", "glfw");
            put("windows", "glfw3");
            put("android", "glfw");
            put("freebsd", "glfw");
        }});

        // SDL2
        registerLibrary("sdl2", new HashMap<String, String>() {{
            put("linux", "SDL2");
            put("macos", "SDL2");
            put("windows", "SDL2");
            put("android", "SDL2");
            put("freebsd", "SDL2");
        }});

        // ==================== 网络库 ====================

        // libcurl
        registerLibrary("curl", new HashMap<String, String>() {{
            put("linux", "curl");
            put("macos", "curl");
            put("windows", "libcurl");
            put("android", "curl");
            put("freebsd", "curl");
        }});

        // ==================== 图像库 ====================

        // libpng
        registerLibrary("png", new HashMap<String, String>() {{
            put("linux", "png");
            put("macos", "png");
            put("windows", "libpng16");
            put("android", "png");
            put("freebsd", "png");
        }});

        // JPEG
        registerLibrary("jpeg", new HashMap<String, String>() {{
            put("linux", "jpeg");
            put("macos", "jpeg");
            put("windows", "libjpeg");
            put("android", "jpeg");
            put("freebsd", "jpeg");
        }});

        // ==================== 其他常用库 ====================

        // zstd 压缩库
        registerLibrary("z", new HashMap<String, String>() {{
            put("linux", "z");
            put("macos", "z");
            put("windows", "zlib");
            put("android", "z");
            put("freebsd", "z");
        }});

        // Brotli 压缩库
        registerLibrary("brotli", new HashMap<String, String>() {{
            put("linux", "brotlienc");
            put("macos", "brotlienc");
            put("windows", "libbrotlienc");
            put("android", "brotlienc");
            put("freebsd", "brotlienc");
        }});

        // ICU 字符串处理库
        registerLibrary("icu", new HashMap<String, String>() {{
            put("linux", "icui18n");
            put("macos", "icui18n");
            put("windows", "icuuc");
            put("android", "icuuc");
            put("freebsd", "icui18n");
        }});

        // FFMPEG
        registerLibrary("avcodec", new HashMap<String, String>() {{
            put("linux", "avcodec");
            put("macos", "avcodec");
            put("windows", "avcodec-61");
            put("android", "avcodec");
            put("freebsd", "avcodec");
        }});

        registerLibrary("avutil", new HashMap<String, String>() {{
            put("linux", "avutil");
            put("macos", "avutil");
            put("windows", "avutil-59");
            put("android", "avutil");
            put("freebsd", "avutil");
        }});

        registerLibrary("avformat", new HashMap<String, String>() {{
            put("linux", "avformat");
            put("macos", "avformat");
            put("windows", "avformat-61");
            put("android", "avformat");
            put("freebsd", "avformat");
        }});
    }

    /**
     * 注册库映射
     */
    private static void registerLibrary(String name, Map<String, String> platformMap) {
        LIBRARY_MAP.put(name, platformMap);
    }

    /**
     * 映射库名
     *
     * 根据平台将通用库名映射到实际的库名
     *
     * @param libraryName 通用库名（如 "sqlite3", "openssl"）
     * @param target       目标平台三元组
     * @return 映射后的库名，null 表示该平台不需要显式链接
     *
     * @example
     * TargetTriple windows = TargetTriple.parse("windows-x86_64");
     * PlatformLibraryMapper.mapLibraryName("sqlite3", windows);
     * // 返回: "sqlite3"
     *
     * TargetTriple linux = TargetTriple.parse("linux-x86_64");
     * PlatformLibraryMapper.mapLibraryName("sqlite3", linux);
     * // 返回: "sqlite3"
     *
     * TargetTriple macos = TargetTriple.parse("macos-arm64");
     * PlatformLibraryMapper.mapLibraryName("sqlite3", macos);
     * // 返回: "sqlite3"
     *
     * TargetTriple windows = TargetTriple.parse("windows-x86_64");
     * PlatformLibraryMapper.mapLibraryName("crypto", windows);
     * // 返回: "libcrypto-3-x64"
     */
    public static String mapLibraryName(String libraryName, TargetTriple target) {
        if (libraryName == null || target == null) {
            return null;
        }

        Map<String, String> platformMap = LIBRARY_MAP.get(libraryName.toLowerCase());

        if (platformMap == null) {
            // 没有特殊映射规则，原名返回
            return libraryName;
        }

        // 优先精确匹配平台
        String mapped = platformMap.get(target.platform);
        if (mapped != null) {
            return mapped;
        }

        // 没有该平台的映射，返回原名
        return libraryName;
    }

    /**
     * 获取所有隐含库（依赖库）
     *
     * 自动检测目标平台需要加载的额外库
     *
     * @param libraryName 原始库名
     * @param target      目标平台三元组
     * @return 隐含库名列表（不含重复项）
     *
     * @example
     * TargetTriple windows = TargetTriple.parse("windows-x86_64");
     * PlatformLibraryMapper.getImpliedLibraries("openssl", windows);
     * // 返回: ["crypt32", "ws2_32"]
     *
     * TargetTriple linux = TargetTriple.parse("linux-x86_64");
     * PlatformLibraryMapper.getImpliedLibraries("openssl", linux);
     * // 返回: ["ssl", "crypto"]
     */
    public static List<String> getImpliedLibraries(String libraryName, TargetTriple target) {
        List<String> implied = new ArrayList<>();

        if (libraryName == null || target == null) {
            return implied;
        }

        String lib = libraryName.toLowerCase();

        // Windows 平台隐含库
        if (target.isWindows()) {
            // 网络相关
            if ("ws2_32".equals(lib) || "socket".equals(lib)) {
                implied.add("mswsock");
            }
            if ("openssl".equals(lib)) {
                implied.add("crypt32");
                implied.add("ws2_32");
            }

            // Oracle 数据库
            if ("oci".equals(lib)) {
                implied.add("clntsh");
            }

            // Windows API 相关
            if ("advapi32".equals(lib) || "kernel32".equals(lib)) {
                implied.add("user32");
                implied.add("gdi32");
            }
        }

        // Linux 平台隐含库
        if (target.isLinux()) {
            if ("openssl".equals(lib)) {
                implied.add("ssl");
                implied.add("crypto");
            }
            if ("curl".equals(lib)) {
                implied.add("ssl");
                implied.add("crypto");
            }
            if ("mysqlclient".equals(lib)) {
                implied.add("z");
            }
            if ("pq".equals(lib)) {
                implied.add("ssl");
                implied.add("crypto");
            }
            if ("ssl".equals(lib)) {
                implied.add("crypto");
            }
        }

        // macOS 平台隐含库
        if (target.isMacOS()) {
            if ("openssl".equals(lib)) {
                implied.add("crypto");
            }
            if ("curl".equals(lib)) {
                implied.add("ssl");
                implied.add("crypto");
            }
        }

        return implied;
    }

    /**
     * 获取库文件名（包括前缀和扩展名）
     *
     * @param libraryName 库名（可以是无前缀的库名或带前缀的库名）
     * @param target      目标平台
     * @return 完整的库文件名
     *
     * @example
     * TargetTriple windows = TargetTriple.parse("windows-x86_64");
     * PlatformLibraryMapper.getLibraryFileName("sqlite3", windows);
     * // 返回: "sqlite3.dll"
     *
     * TargetTriple linux = TargetTriple.parse("linux-x86_64");
     * PlatformLibraryMapper.getLibraryFileName("sqlite3", linux);
     * // 返回: "libsqlite3.so"
     *
     * TargetTriple macos = TargetTriple.parse("macos-arm64");
     * PlatformLibraryMapper.getLibraryFileName("libcurl", macos);
     * // 返回: "libcurl.dylib"
     */
    public static String getLibraryFileName(String libraryName, TargetTriple target) {
        if (libraryName == null || target == null) {
            return null;
        }

        String libName = libraryName.toLowerCase();

        // 如果已经是完整的文件名，直接返回
        if (libName.endsWith(".dll") || libName.endsWith(".so") || libName.endsWith(".dylib")) {
            return libraryName;
        }

        // 获取映射后的库名
        String mappedLib = mapLibraryName(libraryName, target);
        if (mappedLib == null) {
            return null;
        }

        // 使用 TargetTriple 的扩展名
        return target.dynamicLibFileName(mappedLib);
    }

    /**
     * 获取平台依赖库列表（包含隐含库）
     *
     * 返回目标平台上使用某个库所需的所有库
     *
     * @param libraryName 库名
     * @param target      目标平台
     * @return 所有依赖库列表
     *
     * @example
     * TargetTriple windows = TargetTriple.parse("windows-x86_64");
     * PlatformLibraryMapper.getPlatformDependentLibraries("openssl", windows);
     * // 返回: ["libcrypto-3-x64", "crypt32", "ws2_32", "mswsock"]
     */
    public static List<String> getPlatformDependentLibraries(String libraryName, TargetTriple target) {
        List<String> libraries = new ArrayList<>();
        libraries.add(mapLibraryName(libraryName, target));
        libraries.addAll(getImpliedLibraries(libraryName, target));
        return libraries;
    }

    /**
     * 检查库是否为系统内置库（不需要显式链接）
     *
     * @param libraryName 库名
     * @param target      目标平台
     * @return true 表示该库不需要显式链接
     */
    public static boolean isSystemLibrary(String libraryName, TargetTriple target) {
        String mapped = mapLibraryName(libraryName, target);
        return mapped == null;
    }

    /**
     * 获取所有已注册的库
     *
     * @return 库名列表
     */
    public static List<String> getRegisteredLibraries() {
        return new ArrayList<>(LIBRARY_MAP.keySet());
    }

    /**
     * 获取特定库的映射信息
     *
     * @param libraryName 库名
     * @return 平台映射信息，如果库未注册则返回 null
     */
    public static Map<String, String> getLibraryMapping(String libraryName) {
        return LIBRARY_MAP.get(libraryName.toLowerCase());
    }

    /**
     * 批量映射库文件名
     *
     * @param libraryNames 库名列表
     * @param target       目标平台
     * @return 库文件名列表
     */
    public static List<String> mapLibraryFileNames(List<String> libraryNames, TargetTriple target) {
        List<String> results = new ArrayList<>();
        for (String libName : libraryNames) {
            results.add(getLibraryFileName(libName, target));
        }
        return results;
    }
}

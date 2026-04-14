package claw.compiler.generators.ffi.platform;

import java.util.List;

/**
 * PlatformLibraryMapper 使用示例
 *
 * 演示如何使用跨平台库映射器进行库名映射、隐含库检测和文件名生成
 */
public class PlatformLibraryMapperExample {

    public static void main(String[] args) {
        System.out.println("========== Platform Library Mapper Example ==========\n");

        // 1. 基本库名映射
        System.out.println("1. 基本库名映射:");
        example1();

        // 2. 隐含库检测
        System.out.println("\n2. 隐含库检测:");
        example2();

        // 3. 库文件名生成
        System.out.println("\n3. 库文件名生成:");
        example3();

        // 4. 平台依赖库列表
        System.out.println("\n4. 平台依赖库列表:");
        example4();

        // 5. 系统库检测
        System.out.println("\n5. 系统库检测:");
        example5();

        // 6. 完整编译命令示例
        System.out.println("\n6. 完整编译命令示例:");
        example6();

        // 7. SQLite3 编译示例
        System.out.println("\n7. SQLite3 编译示例:");
        example7();

        // 8. OpenSSL 编译示例
        System.out.println("\n8. OpenSSL 编译示例:");
        example8();
    }

    private static void example1() {
        System.out.println("Linux: " + PlatformLibraryMapper.mapLibraryName("sqlite3", TargetTriple.parse("linux-x86_64")));
        System.out.println("macOS: " + PlatformLibraryMapper.mapLibraryName("sqlite3", TargetTriple.parse("macos-arm64")));
        System.out.println("Windows: " + PlatformLibraryMapper.mapLibraryName("sqlite3", TargetTriple.parse("windows-x86_64")));
        System.out.println("Android: " + PlatformLibraryMapper.mapLibraryName("sqlite3", TargetTriple.parse("android-arm64")));
    }

    private static void example2() {
        TargetTriple windows = TargetTriple.parse("windows-x86_64");

        System.out.println("ws2_32 隐含库: " + PlatformLibraryMapper.getImpliedLibraries("ws2_32", windows));
        System.out.println("openssl 隐含库: " + PlatformLibraryMapper.getImpliedLibraries("openssl", windows));
        System.out.println("curl 隐含库: " + PlatformLibraryMapper.getImpliedLibraries("curl", windows));
    }

    private static void example3() {
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        TargetTriple macos = TargetTriple.parse("macos-x86_64");

        System.out.println("sqlite3.dll (Windows): " + PlatformLibraryMapper.getLibraryFileName("sqlite3", windows));
        System.out.println("libsqlite3.so (Linux): " + PlatformLibraryMapper.getLibraryFileName("sqlite3", linux));
        System.out.println("libsqlite3.dylib (macOS): " + PlatformLibraryMapper.getLibraryFileName("sqlite3", macos));

        System.out.println("libcurl.so (Linux): " + PlatformLibraryMapper.getLibraryFileName("curl", linux));
        System.out.println("libcurl.dylib (macOS): " + PlatformLibraryMapper.getLibraryFileName("curl", macos));
        System.out.println("libcurl.dll (Windows): " + PlatformLibraryMapper.getLibraryFileName("curl", windows));
    }

    private static void example4() {
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        TargetTriple linux = TargetTriple.parse("linux-x86_64");

        System.out.println("Windows OpenSSL 依赖: " + PlatformLibraryMapper.getPlatformDependentLibraries("openssl", windows));
        System.out.println("Linux OpenSSL 依赖: " + PlatformLibraryMapper.getPlatformDependentLibraries("openssl", linux));
        System.out.println("Linux Curl 依赖: " + PlatformLibraryMapper.getPlatformDependentLibraries("curl", linux));
    }

    private static void example5() {
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        TargetTriple windows = TargetTriple.parse("windows-x86_64");

        System.out.println("Math 库 (macOS): " + PlatformLibraryMapper.isSystemLibrary("m", macos));
        System.out.println("Pthread (Windows): " + PlatformLibraryMapper.isSystemLibrary("pthread", windows));
        System.out.println("Dynamic Loader (macOS): " + PlatformLibraryMapper.isSystemLibrary("dl", macos));
        System.out.println("SQLite (Linux): " + PlatformLibraryMapper.isSystemLibrary("sqlite3", macos));
    }

    private static void example6() {
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        TargetTriple windows = TargetTriple.parse("windows-x86_64");

        System.out.println("===== Linux =====");
        List<String> libs = List.of("sqlite3", "curl", "openssl", "m");
        for (String lib : libs) {
            System.out.println("  " + lib + ": " + PlatformLibraryMapper.getLibraryFileName(lib, linux));
        }

        System.out.println("\n===== Windows =====");
        for (String lib : libs) {
            System.out.println("  " + lib + ": " + PlatformLibraryMapper.getLibraryFileName(lib, windows));
        }
    }

    private static void example7() {
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        TargetTriple macos = TargetTriple.parse("macos-arm64");

        System.out.println("===== SQLite3 =====");

        System.out.println("\nLinux (gcc):");
        System.out.println("  gcc -o app example.c -lsqlite3");
        System.out.println("  或:");
        System.out.println("  gcc -o app example.c -L/usr/lib -lsqlite3");

        System.out.println("\nmacOS (clang):");
        System.out.println("  clang -o app example.c -lsqlite3");

        System.out.println("\nWindows (MSVC):");
        List<String> winLibs = PlatformLibraryMapper.getPlatformDependentLibraries("sqlite3", windows);
        System.out.println("  link /out:app.exe example.obj");
        for (String lib : winLibs) {
            System.out.println("  lib " + lib + ".lib");
        }
    }

    private static void example8() {
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        TargetTriple macos = TargetTriple.parse("macos-x86_64");

        System.out.println("===== OpenSSL =====");

        System.out.println("\nLinux:");
        List<String> linuxLibs = PlatformLibraryMapper.getPlatformDependentLibraries("openssl", linux);
        System.out.println("  gcc -o app example.c -lssl -lcrypto");

        System.out.println("\nmacOS:");
        List<String> macosLibs = PlatformLibraryMapper.getPlatformDependentLibraries("openssl", macos);
        System.out.println("  clang -o app example.c -lssl -lcrypto");

        System.out.println("\nWindows:");
        List<String> winLibs = PlatformLibraryMapper.getPlatformDependentLibraries("openssl", windows);
        System.out.println("  link /out:app.exe example.obj");
        for (String lib : winLibs) {
            System.out.println("  lib " + lib + ".lib");
        }
    }

    /**
     * 高级示例：根据目标平台生成完整的编译命令
     */
    public static String generateCompileCommand(String sourceFile, String outputName,
            List<String> libraries, TargetTriple target) {
        StringBuilder sb = new StringBuilder();

        // C 编译器
        String compiler = target.isWindows() ? "cl.exe" : "gcc";

        // 生成命令
        sb.append(compiler).append(" -o ").append(outputName).append(" ").append(sourceFile);

        // 链接库
        for (String lib : libraries) {
            String libName = PlatformLibraryMapper.getLibraryFileName(lib, target);
            if (libName == null) continue;

            if (target.isWindows()) {
                // Windows: libxxx.lib
                sb.append(" lib").append(libName.replace(".dll", ".lib"));
            } else {
                // Unix-like: -lxxx
                sb.append(" -l").append(libName.replace(".so", "").replace(".dylib", ""));
            }
        }

        return sb.toString();
    }

    /**
     * 完整示例：构建 CFFIGenerator 需要的链接参数
     */
    public static void buildFFILinkFlags() {
        TargetTriple target = TargetTriple.parse("linux-x86_64");

        System.out.println("===== SQLite3 FFI =====");
        List<String> libs = PlatformLibraryMapper.getPlatformDependentLibraries("sqlite3", target);
        System.out.println("Link flags: " + libs);

        System.out.println("\n===== Curl FFI =====");
        libs = PlatformLibraryMapper.getPlatformDependentLibraries("curl", target);
        System.out.println("Link flags: " + libs);

        System.out.println("\n===== OpenSSL FFI =====");
        libs = PlatformLibraryMapper.getPlatformDependentLibraries("openssl", target);
        System.out.println("Link flags: " + libs);
    }
}

package claw.compiler.generators.ffi.platform;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PlatformLibraryMapper 测试类
 *
 * 测试跨平台库映射器的所有功能
 */
public class PlatformLibraryMapperTest {

    @Test
    public void testMapLibraryName_Sqlite3() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("sqlite3", PlatformLibraryMapper.mapLibraryName("sqlite3", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-arm64");
        assertEquals("sqlite3", PlatformLibraryMapper.mapLibraryName("sqlite3", macos));

        // Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("sqlite3", PlatformLibraryMapper.mapLibraryName("sqlite3", windows));

        // Android
        TargetTriple android = TargetTriple.parse("android-arm64");
        assertEquals("sqlite3", PlatformLibraryMapper.mapLibraryName("sqlite3", android));
    }

    @Test
    public void testMapLibraryName_OpenSSL() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("ssl", PlatformLibraryMapper.mapLibraryName("openssl", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertEquals("ssl", PlatformLibraryMapper.mapLibraryName("openssl", macos));

        // Windows - 可能返回具体版本或通用名
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        String mapped = PlatformLibraryMapper.mapLibraryName("openssl", windows);
        assertNotNull(mapped);

        // Android
        TargetTriple android = TargetTriple.parse("android-arm64");
        assertEquals("ssl", PlatformLibraryMapper.mapLibraryName("openssl", android));
    }

    @Test
    public void testMapLibraryName_MathLibrary() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("m", PlatformLibraryMapper.mapLibraryName("m", linux));

        // macOS - 不需要
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertNull(PlatformLibraryMapper.mapLibraryName("m", macos));

        // Windows - 不需要
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertNull(PlatformLibraryMapper.mapLibraryName("m", windows));
    }

    @Test
    public void testMapLibraryName_Pthread() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("pthread", PlatformLibraryMapper.mapLibraryName("pthread", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertEquals("pthread", PlatformLibraryMapper.mapLibraryName("pthread", macos));

        // Windows - 不需要
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertNull(PlatformLibraryMapper.mapLibraryName("pthread", windows));
    }

    @Test
    public void testMapLibraryName_DynamicLoader() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("dl", PlatformLibraryMapper.mapLibraryName("dl", linux));

        // macOS - 不需要
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertNull(PlatformLibraryMapper.mapLibraryName("dl", macos));

        // Windows - 不需要
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertNull(PlatformLibraryMapper.mapLibraryName("dl", windows));
    }

    @Test
    public void testMapLibraryName_Socket() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertNull(PlatformLibraryMapper.mapLibraryName("socket", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertNull(PlatformLibraryMapper.mapLibraryName("socket", macos));

        // Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("ws2_32", PlatformLibraryMapper.mapLibraryName("socket", windows));
    }

    @Test
    public void testMapLibraryName_MySQL() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("mysqlclient", PlatformLibraryMapper.mapLibraryName("mysqlclient", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertEquals("mysqlclient", PlatformLibraryMapper.mapLibraryName("mysqlclient", macos));

        // Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("mysql", PlatformLibraryMapper.mapLibraryName("mysqlclient", windows));
    }

    @Test
    public void testMapLibraryName_PostgreSQL() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("pq", PlatformLibraryMapper.mapLibraryName("pq", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertEquals("pq", PlatformLibraryMapper.mapLibraryName("pq", macos));

        // Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("libpq", PlatformLibraryMapper.mapLibraryName("pq", windows));
    }

    @Test
    public void testMapLibraryName_Glfw() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("glfw", PlatformLibraryMapper.mapLibraryName("glfw", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertEquals("glfw", PlatformLibraryMapper.mapLibraryName("glfw", macos));

        // Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("glfw3", PlatformLibraryMapper.mapLibraryName("glfw", windows));
    }

    @Test
    public void testMapLibraryName_SDL2() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("SDL2", PlatformLibraryMapper.mapLibraryName("sdl2", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertEquals("SDL2", PlatformLibraryMapper.mapLibraryName("sdl2", macos));

        // Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("SDL2", PlatformLibraryMapper.mapLibraryName("sdl2", windows));
    }

    @Test
    public void testMapLibraryName_Curl() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("curl", PlatformLibraryMapper.mapLibraryName("curl", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertEquals("curl", PlatformLibraryMapper.mapLibraryName("curl", macos));

        // Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("libcurl", PlatformLibraryMapper.mapLibraryName("curl", windows));
    }

    @Test
    public void testMapLibraryName_PNG() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("png", PlatformLibraryMapper.mapLibraryName("png", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertEquals("png", PlatformLibraryMapper.mapLibraryName("png", macos));

        // Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("libpng16", PlatformLibraryMapper.mapLibraryName("png", windows));
    }

    @Test
    public void testMapLibraryName_JPEG() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("jpeg", PlatformLibraryMapper.mapLibraryName("jpeg", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertEquals("jpeg", PlatformLibraryMapper.mapLibraryName("jpeg", macos));

        // Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("libjpeg", PlatformLibraryMapper.mapLibraryName("jpeg", windows));
    }

    @Test
    public void testMapLibraryName_Z() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("z", PlatformLibraryMapper.mapLibraryName("z", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertEquals("z", PlatformLibraryMapper.mapLibraryName("z", macos));

        // Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("zlib", PlatformLibraryMapper.mapLibraryName("z", windows));
    }

    @Test
    public void testMapLibraryName_Brotli() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("brotlienc", PlatformLibraryMapper.mapLibraryName("brotli", linux));

        // macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertEquals("brotlienc", PlatformLibraryMapper.mapLibraryName("brotli", macos));

        // Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("libbrotlienc", PlatformLibraryMapper.mapLibraryName("brotli", windows));
    }

    @Test
    public void testGetImpliedLibraries_Windows() {
        // Socket
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertTrue(PlatformLibraryMapper.getImpliedLibraries("ws2_32", windows).contains("mswsock"));

        // OpenSSL
        List<String> implied = PlatformLibraryMapper.getImpliedLibraries("openssl", windows);
        assertTrue(implied.contains("crypt32"));
        assertTrue(implied.contains("ws2_32"));
        assertTrue(implied.contains("mswsock"));
    }

    @Test
    public void testGetImpliedLibraries_Linux() {
        // OpenSSL
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        List<String> implied = PlatformLibraryMapper.getImpliedLibraries("openssl", linux);
        assertTrue(implied.contains("ssl"));
        assertTrue(implied.contains("crypto"));

        // MySQL
        List<String> mysqlImplied = PlatformLibraryMapper.getImpliedLibraries("mysqlclient", linux);
        assertTrue(mysqlImplied.contains("z"));
    }

    @Test
    public void testGetImpliedLibraries_MacOS() {
        // OpenSSL
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        List<String> implied = PlatformLibraryMapper.getImpliedLibraries("openssl", macos);
        assertTrue(implied.contains("crypto"));
    }

    @Test
    public void testGetImpliedLibraries_Empty() {
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertTrue(PlatformLibraryMapper.getImpliedLibraries("sqlite3", linux).isEmpty());
        assertTrue(PlatformLibraryMapper.getImpliedLibraries("nonexistent", linux).isEmpty());
    }

    @Test
    public void testGetLibraryFileName() {
        // SQLite3
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("sqlite3.dll", PlatformLibraryMapper.getLibraryFileName("sqlite3", windows));

        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("libsqlite3.so", PlatformLibraryMapper.getLibraryFileName("sqlite3", linux));

        TargetTriple macos = TargetTriple.parse("macos-arm64");
        assertEquals("libsqlite3.dylib", PlatformLibraryMapper.getLibraryFileName("sqlite3", macos));

        // Curl
        assertEquals("libcurl.dll", PlatformLibraryMapper.getLibraryFileName("curl", windows));
    }

    @Test
    public void testGetLibraryFileName_WithPrefix() {
        // 带 lib 前缀
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("libsqlite3.so", PlatformLibraryMapper.getLibraryFileName("libsqlite3", linux));

        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("libcurl.dll", PlatformLibraryMapper.getLibraryFileName("libcurl", windows));
    }

    @Test
    public void testGetLibraryFileName_AlreadyComplete() {
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("libcurl.so", PlatformLibraryMapper.getLibraryFileName("libcurl.so", linux));

        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("sqlite3.dll", PlatformLibraryMapper.getLibraryFileName("sqlite3.dll", windows));
    }

    @Test
    public void testGetPlatformDependentLibraries() {
        // OpenSSL in Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        List<String> libs = PlatformLibraryMapper.getPlatformDependentLibraries("openssl", windows);
        assertTrue(libs.contains("libcrypto-3-x64"));
        assertTrue(libs.contains("crypt32"));
        assertTrue(libs.contains("ws2_32"));

        // SQLite3 in Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        libs = PlatformLibraryMapper.getPlatformDependentLibraries("sqlite3", linux);
        assertTrue(libs.contains("sqlite3"));

        // Curl in macOS
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        libs = PlatformLibraryMapper.getPlatformDependentLibraries("curl", macos);
        assertTrue(libs.contains("curl"));
        assertTrue(libs.contains("ssl"));
    }

    @Test
    public void testIsSystemLibrary() {
        // Math - macOS 和 Windows 不需要
        TargetTriple macos = TargetTriple.parse("macos-x86_64");
        assertTrue(PlatformLibraryMapper.isSystemLibrary("m", macos));

        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertTrue(PlatformLibraryMapper.isSystemLibrary("m", windows));

        // Pthread - Windows 不需要
        assertTrue(PlatformLibraryMapper.isSystemLibrary("pthread", windows));

        // Dynamic loader - macOS 不需要
        assertTrue(PlatformLibraryMapper.isSystemLibrary("dl", macos));

        // SQLite - 所有平台都需要
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertFalse(PlatformLibraryMapper.isSystemLibrary("sqlite3", linux));
    }

    @Test
    public void testGetRegisteredLibraries() {
        List<String> libraries = PlatformLibraryMapper.getRegisteredLibraries();
        assertNotNull(libraries);
        assertFalse(libraries.isEmpty());

        assertTrue(libraries.contains("sqlite3"));
        assertTrue(libraries.contains("openssl"));
        assertTrue(libraries.contains("curl"));
        assertTrue(libraries.contains("m"));
        assertTrue(libraries.contains("pthread"));
    }

    @Test
    public void testGetLibraryMapping() {
        Map<String, String> mapping = PlatformLibraryMapper.getLibraryMapping("sqlite3");
        assertNotNull(mapping);
        assertTrue(mapping.containsKey("linux"));
        assertTrue(mapping.containsKey("macos"));
        assertTrue(mapping.containsKey("windows"));
        assertTrue(mapping.containsKey("android"));
    }

    @Test
    public void testGetLibraryMapping_NonExistent() {
        Map<String, String> mapping = PlatformLibraryMapper.getLibraryMapping("nonexistent");
        assertNull(mapping);
    }

    @Test
    public void testMapLibraryFileNames_Batch() {
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        TargetTriple windows = TargetTriple.parse("windows-x86_64");

        List<String> libs = List.of("sqlite3", "curl", "openssl", "m");

        List<String> linuxFiles = PlatformLibraryMapper.mapLibraryFileNames(libs, linux);
        assertEquals(4, linuxFiles.size());
        assertTrue(linuxFiles.contains("libsqlite3.so"));
        assertTrue(linuxFiles.contains("libcurl.so"));
        assertTrue(linuxFiles.contains("libcrypto.so"));  // openssl 映射到 ssl/crypto

        List<String> windowsFiles = PlatformLibraryMapper.mapLibraryFileNames(libs, windows);
        assertEquals(4, windowsFiles.size());
        assertTrue(windowsFiles.contains("sqlite3.dll"));
        assertTrue(windowsFiles.contains("libcurl.dll"));
    }

    @Test
    public void testMapLibraryName_NullInputs() {
        assertNull(PlatformLibraryMapper.mapLibraryName(null, TargetTriple.parse("linux-x86_64")));
        assertNull(PlatformLibraryMapper.mapLibraryName("sqlite3", null));
        assertNull(PlatformLibraryMapper.mapLibraryName(null, null));
    }

    @Test
    public void testMapLibraryName_MixedCase() {
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        TargetTriple macos = TargetTriple.parse("macos-arm64");

        // 大小写不敏感
        assertEquals("sqlite3", PlatformLibraryMapper.mapLibraryName("SQLite3", linux));
        assertEquals("sqlite3", PlatformLibraryMapper.mapLibraryName("SQLITE3", macos));
        assertEquals("ssl", PlatformLibraryMapper.mapLibraryName("OPENSSL", linux));
    }

    @Test
    public void testMapLibraryName_NoMapping() {
        // 未注册的库名
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("mylib", PlatformLibraryMapper.mapLibraryName("mylib", linux));
    }

    @Test
    public void testFFMPEGLibraries() {
        // Linux
        TargetTriple linux = TargetTriple.parse("linux-x86_64");
        assertEquals("avcodec", PlatformLibraryMapper.mapLibraryName("avcodec", linux));
        assertEquals("avutil", PlatformLibraryMapper.mapLibraryName("avutil", linux));
        assertEquals("avformat", PlatformLibraryMapper.mapLibraryName("avformat", linux));

        // Windows
        TargetTriple windows = TargetTriple.parse("windows-x86_64");
        assertEquals("avcodec-61", PlatformLibraryMapper.mapLibraryName("avcodec", windows));
        assertEquals("avutil-59", PlatformLibraryMapper.mapLibraryName("avutil", windows));
        assertEquals("avformat-61", PlatformLibraryMapper.mapLibraryName("avformat", windows));
    }

    @Test
    public void testDetectHost() {
        TargetTriple host = TargetTriple.detectHost();

        assertNotNull(host);
        assertNotNull(host.platform);
        assertNotNull(host.architecture);
    }
}

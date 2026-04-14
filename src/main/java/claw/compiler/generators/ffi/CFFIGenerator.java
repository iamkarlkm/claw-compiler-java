package claw.compiler.generators.ffi;

import claw.compiler.generators.ffi.FFIBindingTable.*;
import claw.compiler.generators.ffi.platform.PlatformConstraint;

import java.util.*;

/**
 * C 目标 FFI 代码生成器
 *
 * 将 FFIBindingTable 中的外部符号信息转换为 C 代码
 *
 * C 目标是最直接的：
 *   - extern 类型 → typedef / 直接使用
 *   - extern 函数 → #include 头文件即可，C 链接器自动解析
 *   - link 指令 → 编译命令的 -l 参数
 */
public class CFFIGenerator {

    private final FFIBindingTable bindingTable;

    public CFFIGenerator(FFIBindingTable bindingTable) {
        this.bindingTable = bindingTable;
    }

    // ================================================================
    //  生成 #include 指令
    // ================================================================

    /**
     * 生成所有 extern 块对应的 #include 语句
     *
     * 输出示例：
     *   #include <sqlite3.h>
     *   #include <curl/curl.h>
     */
    public String generateIncludes() {
        StringBuilder sb = new StringBuilder();
        sb.append("/* ========== External Library Headers (FFI) ========== */\n");

        for (String include : bindingTable.getAllIncludes()) {
            // 判断是系统头文件还是本地头文件
            if (include.contains("/") || isSystemHeader(include)) {
                sb.append("#include <").append(include).append(">\n");
            } else {
                sb.append("#include \"").append(include).append("\"\n");
            }
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * 判断是否为系统级头文件
     */
    private boolean isSystemHeader(String header) {
        // 常见的系统库头文件
        Set<String> systemHeaders = new HashSet<>(Arrays.asList(
            "stdio.h", "stdlib.h", "string.h", "math.h",
            "sqlite3.h", "curl/curl.h", "openssl/ssl.h",
            "pthread.h", "unistd.h", "fcntl.h",
            "zlib.h", "png.h", "jpeglib.h"
        ));
        return systemHeaders.contains(header);
    }

    // ================================================================
    //  生成类型定义
    // ================================================================

    /**
     * 生成外部类型的 C typedef
     *
     * Claw: type sqlite3 = OpaquePointer
     * C:    typedef void* sqlite3;  （如果没有 #include 提供定义的话）
     *
     * 注意：大多数情况下，C 头文件自身会定义这些类型，
     *       这里生成的是前置声明或备用定义
     */
    public String generateTypeDefinitions() {
        Map<String, ExternType> types = bindingTable.getAllTypes();
        if (types.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("/* ========== External Type Declarations (FFI) ========== */\n");
        sb.append("/* Note: These are forward declarations. */\n");
        sb.append("/* Actual definitions come from included headers. */\n");

        for (ExternType type : types.values()) {
            String cType = mapClawFFITypeToCType(type.cMappingType);
            // 生成条件编译保护，避免与头文件中的定义冲突
            sb.append("#ifndef _CLAW_EXTERN_TYPE_").append(type.clawTypeName.toUpperCase()).append("_\n");
            sb.append("#define _CLAW_EXTERN_TYPE_").append(type.clawTypeName.toUpperCase()).append("_\n");

            if ("OpaquePointer".equals(type.cMappingType)) {
                // 不透明指针：只声明结构体，不定义内部
                sb.append("/* Opaque type - defined in external library */\n");
                // 不需要额外 typedef，头文件会提供
            } else {
                sb.append("typedef ").append(cType).append(" claw_").append(type.clawTypeName).append(";\n");
            }

            sb.append("#endif\\n");
        }

        return sb.toString();
    }

    // ================================================================
    //  生成常量定义
    // ================================================================

    /**
     * 生成外部常量的 C 定义
     *
     * 通常这些常量已经在头文件中以 #define 形式存在，
     * 这里生成备用定义
     */
    public String generateConstants() {
        Map<String, ExternConstant> constants = bindingTable.getAllConstants();
        if (constants.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("/* ========== External Constants (FFI) ========== */\n");

        for (ExternConstant constant : constants.values()) {
            sb.append("#ifndef ").append(constant.name).append("\n");
            sb.append("#define ").append(constant.name).append(" (").append(constant.value).append(")\n");
            sb.append("#endif\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    // ================================================================
    //  生成函数声明（通常不需要，头文件已包含）
    // ================================================================

    /**
     * 生成外部函数的 C 前置声明
     *
     * 仅在没有头文件可用时使用
     */
    public String generateFunctionDeclarations() {
        Map<String, ExternFunction> functions = bindingTable.getAllFunctions();
        if (functions.isEmpty()) return "";

        // 检查是否有头文件 — 有的话，函数声明由头文件提供
        if (!bindingTable.getAllIncludes().isEmpty()) {
            return "/* External function declarations provided by included headers */\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("/* ========== External Function Declarations (FFI) ========== */\n");

      
        for (ExternFunction func : functions.values()) {
            String returnType = mapClawFFITypeToCType(func.returnType);
            sb.append("extern ").append(returnType).append(" ").append(func.name).append("(");

            for (int i = 0; i < func.params.size(); i++) {
                if (i > 0) sb.append(", ");
                ExternParam param = func.params.get(i);
                sb.append(mapParamTypeToCType(param.type, param.name));
            }

            if (func.isVariadic) {
                if (!func.params.isEmpty()) sb.append(", ");
                sb.append("...");
            }

            sb.append(");\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    // ================================================================
    //  生成链接参数
    // ================================================================

    /**
     * 生成编译命令的链接参数
     *
     * 返回如: "-lsqlite3 -lcurl -lm"
     */
    public String generateLinkFlags() {
        StringBuilder sb = new StringBuilder();
        for (LinkDirective link : bindingTable.getAllLinks()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("-l").append(link.libraryName);
        }
        return sb.toString();
    }

    /**
     * 生成完整的编译命令
     *
     * 返回如: "gcc -std=c11 -Wall -o output source.c -lsqlite3 -lcurl"
     */
    public String generateBuildCommand(String sourceFile, String outputName) {
        StringBuilder sb = new StringBuilder();
        sb.append("gcc -std=c11 -Wall -Wextra -o ");
        sb.append(outputName).append(" ");
        sb.append(sourceFile);

        String linkFlags = generateLinkFlags();
        if (!linkFlags.isEmpty()) {
            sb.append(" ").append(linkFlags);
        }

        return sb.toString();
    }

    // ================================================================
    //  生成完整的 FFI 代码片段
    // ================================================================

    /**
     * 生成完整的 C FFI 代码片段
     * 插入到生成的 .c 文件头部
     */
    public String generateAll() {
        if (!bindingTable.hasExternDeclarations()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(generateIncludes());
        sb.append(generateTypeDefinitions());
        sb.append(generateConstants());
        sb.append(generateFunctionDeclarations());
        return sb.toString();
    }

    // ================================================================
    //  Claw FFI 类型 → C 类型映射
    // ================================================================

    /**
     * Claw FFI 类型名映射到 C 类型
     */
    public static String mapClawFFITypeToCType(String clawType) {
        if (clawType == null) return "void";

        switch (clawType) {
            // 基本类型
            case "Void":     return "void";
            case "Int":      return "int";
            case "Float":    return "double";
            case "String":   return "const char*";
            case "Bool":     return "bool";
            case "Any":      return "void*";

            // FFI 专用类型
            case "Pointer":        return "void*";
            case "OpaquePointer":  return "void*";
            case "CString":        return "const char*";
            case "FuncPointer":    return "void (*)(void)";
            case "SizeT":          return "size_t";

            // 固定宽度整数
            case "Int8":   return "int8_t";
            case "Int16":  return "int16_t";
            case "Int32":  return "int32_t";
            case "Int64":  return "int64_t";
            case "UInt8":  return "uint8_t";
            case "UInt16": return "uint16_t";
            case "UInt32": return "uint32_t";
            case "UInt64": return "uint64_t";

            default:
                // 可能是 extern 定义的自定义类型名
                // 或泛型类型如 Ref<T>、CArray<T>
                if (clawType.startsWith("Ref<")) {
                    String inner = clawType.substring(4, clawType.length() - 1);
                    return mapClawFFITypeToCType(inner) + "*";
                }
                if (clawType.startsWith("CArray<")) {
                    String inner = clawType.substring(7, clawType.length() - 1);
                    return mapClawFFITypeToCType(inner) + "*";
                }
                // 假定是外部库定义的类型，直接使用原名
                return clawType;
        }
    }

    /**
     * 生成带参数名的 C 参数声明
     */
    private String mapParamTypeToCType(String clawType, String paramName) {
        String cType = mapClawFFITypeToCType(clawType);

        // Ref<T> 特殊处理：参数名不加额外 *
        if (clawType.startsWith("Ref<")) {
            return cType + " " + paramName;
        }

        return cType + " " + paramName;
    }

// CFFIGenerator 中的平台适配

/**
 * 生成平台条件编译的 C 代码
 *
 * 当 FFIBindingTable 包含多个平台的声明时，
 * 生成 #ifdef 包裹的条件编译代码
 */
public String generatePlatformGuardedCode(FFIBindingTable fullTable) {
    StringBuilder sb = new StringBuilder();

    sb.append("/* ========== Platform Detection ========== */\n");
    sb.append("#if defined(_WIN32) || defined(_WIN64)\n");
    sb.append("  #define CLAW_PLATFORM_WINDOWS 1\n");
    sb.append("#elif defined(__APPLE__) && defined(__MACH__)\n");
    sb.append("  #define CLAW_PLATFORM_MACOS 1\n");
    sb.append("#elif defined(__linux__)\n");
    sb.append("  #define CLAW_PLATFORM_LINUX 1\n");
    sb.append("#elif defined(__ANDROID__)\n");
    sb.append("  #define CLAW_PLATFORM_ANDROID 1\n");
    sb.append("#elif defined(__FreeBSD__)\n");
    sb.append("  #define CLAW_PLATFORM_FREEBSD 1\n");
    sb.append("#endif\n");

    sb.append("/* Architecture detection */\n");
    sb.append("#if defined(__x86_64__) || defined(_M_X64)\n");
    sb.append("  #define CLAW_ARCH_X86_64 1\n");
    sb.append("#elif defined(__aarch64__) || defined(_M_ARM64)\n");
    sb.append("  #define CLAW_ARCH_ARM64 1\n");
    sb.append("#elif defined(__i386__) || defined(_M_IX86)\n");
    sb.append("  #define CLAW_ARCH_X86 1\n");
    sb.append("#elif defined(__arm__) || defined(_M_ARM)\n");
    sb.append("  #define CLAW_ARCH_ARM 1\n");
    sb.append("#endif\n");

    // 为每个有平台约束的 extern 块生成条件编译
    for (FFIBindingTable.ExternBlock block : fullTable.getExternBlocks()) {
        if (block.platform != null && !block.platform.isUniversal()) {
            sb.append(generatePlatformIfdef(block.platform));
            sb.append(generateBlockCode(block));
            sb.append("#endif /* platform guard */\n");
        } else {
            // 全平台代码，不需要 guard
            sb.append(generateBlockCode(block));
        }
    }

    return sb.toString();
}

/**
 * 将 PlatformConstraint 转换为 C 预处理条件
 */
private String generatePlatformIfdef(FFIBindingTable.PlatformConstraint constraint) {
    List<String> conditions = new ArrayList<>();

    for (String platform : constraint.getPlatforms()) {
        switch (platform) {
            case "windows": conditions.add("defined(CLAW_PLATFORM_WINDOWS)"); break;
            case "linux":   conditions.add("defined(CLAW_PLATFORM_LINUX)"); break;
            case "macos":   conditions.add("defined(CLAW_PLATFORM_MACOS)"); break;
            case "android": conditions.add("defined(CLAW_PLATFORM_ANDROID)"); break;
            case "freebsd": conditions.add("defined(CLAW_PLATFORM_FREEBSD)"); break;
        }
    }

    for (String arch : constraint.getArchitectures()) {
        switch (arch) {
            case "x86_64": conditions.add("defined(CLAW_ARCH_X86_64)"); break;
            case "arm64":  conditions.add("defined(CLAW_ARCH_ARM64)"); break;
            case "x86":    conditions.add("defined(CLAW_ARCH_X86)"); break;
            case "arm":    conditions.add("defined(CLAW_ARCH_ARM)"); break;
        }
    }

    if (conditions.isEmpty()) return "";
    return "#if " + String.join(" || ", conditions) + "\n";
}

    /**
     * 生成 extern 块的 C 代码
     */
    private String generateBlockCode(ExternBlock block) {
        StringBuilder sb = new StringBuilder();
        // TODO: 实现生成 block 的 C 代码
        return sb.toString();
    }


}

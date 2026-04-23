package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import java.util.*;

/**
 * C 目标 FFI 代码生成器
 *
 * <p>将 FFIBindingTable 中的外部符号信息转换为完整的 C 代砂，包含：</p>
 * <ul>
 *   <li>#include 指令</li>
 *   <li>类型前向声明</li>
 *   <li>函数声明</li>
 *   <li>常量定义 (#define)</li>
 *   <li>结构体定义</li>
 *   <li>枚举定义</li>
 *   <li>回调类型定义 (函数指针)</li>
 *   <li>平台条件编译</li>
 * </ul>
 */
public class CFFIGenerator {

    private final FFIBindingTable bindingTable;

    public CFFIGenerator(FFIBindingTable bindingTable) {
        this.bindingTable = bindingTable;
    }

    // ================================================================
    //  完整代码生成
    // ================================================================

    /**
     * 生成完整的 C FFI 代码
     */
    public String generateAll() {
        if (!bindingTable.hasExternDeclarations()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(generateIncludes());
        sb.append("\n");
        sb.append(generatePlatformDetection());
        sb.append("\n");
        sb.append(generateTypeDefinitions());
        sb.append(generateStructDefinitions());
        sb.append(generateEnumDefinitions());
        sb.append(generateCallbackDefinitions());
        sb.append(generateConstants());
        sb.append(generateFunctionDeclarations());
        sb.append(generateLinkDirectives());
        return sb.toString();
    }

    // ================================================================
    //  头文件包含
    // ================================================================

    /**
     * 生成 #include 指令
     */
    public String generateIncludes() {
        List<String> includes = bindingTable.getAllIncludes();
        if (includes.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("/* ========== External Library Headers (FFI) ========== */\n");
        for (String include : includes) {
            sb.append("#include <").append(include).append(">\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    // ================================================================
    //  平台检测
    // ================================================================

    /**
     * 生成平台检测宏定义
     */
    public String generatePlatformDetection() {
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
        sb.append("#endif\n\n");

        sb.append("#if defined(__x86_64__) || defined(_M_X64)\n");
        sb.append("  #define CLAW_ARCH_X86_64 1\n");
        sb.append("#elif defined(__aarch64__) || defined(_M_ARM64)\n");
        sb.append("  #define CLAW_ARCH_ARM64 1\n");
        sb.append("#elif defined(__i386__) || defined(_M_IX86)\n");
        sb.append("  #define CLAW_ARCH_X86 1\n");
        sb.append("#elif defined(__arm__) || defined(_M_ARM)\n");
        sb.append("  #define CLAW_ARCH_ARM 1\n");
        sb.append("#endif\n\n");
        return sb.toString();
    }

    // ================================================================
    //  类型定义
    // ================================================================

    /**
     * 生成外部类型的前向声明（保护宏包围）
     */
    public String generateTypeDefinitions() {
        Map<String, ExternType> types = bindingTable.getAllTypes();
        if (types.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("/* ========== External Type Declarations (FFI) ========== */\n");
        sb.append("/* Note: These are forward declarations. */\n");
        sb.append("/* Actual definitions come from included headers. */\n\n");

        for (ExternType type : types.values()) {
            String guard = "_CLAW_EXTERN_TYPE_" + type.clawTypeName.toUpperCase() + "_";
            sb.append("/* ").append(type.clawTypeName).append(" = ").append(type.cMappingType);
            if (type.isNullable) sb.append(" (nullable)");
            sb.append(" */\n");
            sb.append("#ifndef ").append(guard).append("\n");
            sb.append("#define ").append(guard).append("\n");

            if ("OpaquePointer".equals(type.cMappingType)) {
                sb.append("/* Opaque type - defined in external library */\n");
            } else {
                String cType = mapClawFFITypeToCType(type.cMappingType);
                sb.append("typedef ").append(cType).append(" ").append(type.clawTypeName).append(";\n");
            }

            sb.append("#endif\n\n");
        }

        return sb.toString();
    }

    // ================================================================
    //  函数声明
    // ================================================================

    /**
     * 生成外部函数声明
     */
    public String generateFunctionDeclarations() {
        Map<String, ExternFunction> functions = bindingTable.getAllFunctions();
        if (functions.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("/* ========== External Function Declarations (FFI) ========== */\n");

        for (ExternFunction func : functions.values()) {
            sb.append(generateSingleFunctionDeclaration(func));
            sb.append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    private String generateSingleFunctionDeclaration(ExternFunction func) {
        StringBuilder sb = new StringBuilder();

        // 文档注释
        sb.append("/* ").append(func.name);
        if (func.description != null && !func.description.isEmpty()) {
            sb.append(" - ").append(func.description);
        }
        if (func.threadSafety != ThreadSafety.UNKNOWN) {
            sb.append(" - thread safety: ").append(func.threadSafety);
        }
        sb.append(" */\n");

        // 废弃标记
        if (func.deprecated) {
            sb.append("/* DEPRECATED */\n");
            if (func.deprecatedAlt != null) {
                sb.append("/* Use ").append(func.deprecatedAlt).append(" instead */\n");
            }
        }

        // 调用约定 / extern 声明
        String callingConv = mapCallingConvention(func.callingConvention);
        if (callingConv != null) {
            sb.append("extern ").append(callingConv).append(" ");
        } else {
            sb.append("extern ");
        }

        // 返回类型 + 函数名 + 参数列表
        sb.append(mapClawFFITypeToCType(func.returnType)).append(" ");
        sb.append(func.name).append("(");

        for (int i = 0; i < func.params.size(); i++) {
            if (i > 0) sb.append(", ");
            ExternParam param = func.params.get(i);
            sb.append(mapClawFFITypeToCType(param.type)).append(" ").append(param.name);
        }

        if (func.isVariadic) {
            if (!func.params.isEmpty()) sb.append(", ");
            sb.append("...");
        }

        sb.append(");\n");
        return sb.toString();
    }

    // ================================================================
    //  常量定义
    // ================================================================

    /**
     * 生成 #define 常量定义（带条件编译保护）
     */
    public String generateConstants() {
        Map<String, ExternConstant> constants = bindingTable.getAllConstants();
        if (constants.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("/* ========== Constant Declarations ========== */\n");

        for (ExternConstant constant : constants.values()) {
            sb.append("#ifndef ").append(constant.name).append("\n");
            sb.append("#define ").append(constant.name).append(" (")
              .append(constant.value).append(")\n");
            sb.append("#endif\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    // ================================================================
    //  结构体定义
    // ================================================================

    /**
     * 生成 C 结构体定义
     */
    public String generateStructDefinitions() {
        List<ExternStruct> structs = bindingTable.getAllStructs();
        if (structs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("/* ========== Struct Definitions ========== */\n");

        for (ExternStruct struct : structs) {
            if (struct.description != null && !struct.description.isEmpty()) {
                sb.append("/* ").append(struct.description).append(" */\n");
            }

            if (struct.packed) {
                sb.append("#pragma pack(push, 1)\n");
            }
            if (struct.alignment > 0) {
                sb.append("/* align: ").append(struct.alignment).append(" */\n");
            }

            sb.append("typedef struct ").append(struct.name).append(" {\n");
            for (StructField field : struct.fields) {
                sb.append("  ").append(mapClawFFITypeToCType(field.type))
                  .append(" ").append(field.name);
                if (field.description != null && !field.description.isEmpty()) {
                    sb.append(" /* ").append(field.description).append(" */");
                }
                sb.append(";\n");
            }
            sb.append("} ").append(struct.name).append(";\n");

            if (struct.packed) {
                sb.append("#pragma pack(pop)\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // ================================================================
    //  枚举定义
    // ================================================================

    /**
     * 生成 C 枚举定义
     */
    public String generateEnumDefinitions() {
        List<ExternEnum> enums = bindingTable.getAllEnums();
        if (enums.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("/* ========== Enum Definitions ========== */\n");

        for (ExternEnum en : enums) {
            if (en.description != null && !en.description.isEmpty()) {
                sb.append("/* ").append(en.description).append(" */\n");
            }

            sb.append("typedef enum ").append(en.name).append(" {\n");
            for (int i = 0; i < en.members.size(); i++) {
                EnumMember member = en.members.get(i);
                sb.append("  ").append(member.name).append(" = ").append(member.value);
                if (member.description != null && !member.description.isEmpty()) {
                    sb.append(" /* ").append(member.description).append(" */");
                }
                if (i < en.members.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("} ").append(en.name).append(";\n\n");
        }

        return sb.toString();
    }

    // ================================================================
    //  回调类型定义
    // ================================================================

    /**
     * 生成函数指针 typedef
     */
    public String generateCallbackDefinitions() {
        List<ExternCallback> callbacks = bindingTable.getAllCallbacks();
        if (callbacks.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("/* ========== Callback Type Definitions ========== */\n");

        for (ExternCallback callback : callbacks) {
            if (callback.description != null && !callback.description.isEmpty()) {
                sb.append("/* ").append(callback.description).append(" */\n");
            }

            sb.append("typedef ").append(mapClawFFITypeToCType(callback.returnType))
              .append(" (*").append(callback.name).append(")(");

            for (int i = 0; i < callback.params.size(); i++) {
                if (i > 0) sb.append(", ");
                ExternParam param = callback.params.get(i);
                sb.append(mapClawFFITypeToCType(param.type)).append(" ").append(param.name);
            }

            sb.append(");\n\n");
        }

        return sb.toString();
    }

    // ================================================================
    //  链接指令注释
    // ================================================================

    /**
     * 生成链接指令的注释说明
     */
    public String generateLinkDirectives() {
        List<LinkDirective> links = bindingTable.getAllLinks();
        if (links.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("/* ========== Link Directives ========== */\n");

        for (LinkDirective link : links) {
            sb.append("/* link \"").append(link.libraryName).append("\"");
            if (link.linkType != LinkType.DYNAMIC) {
                sb.append(" [").append(link.linkType).append("]");
            }
            if (link.optional) sb.append(" [optional]");
            sb.append(" */\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    // ================================================================
    //  类型映射
    // ================================================================

    /**
     * Claw FFI 类型 → C 类型
     */
    public static String mapClawFFITypeToCType(String clawType) {
        if (clawType == null) return "void";

        switch (clawType) {
            case "Void":     return "void";
            case "Int":      return "int";
            case "Float":    return "double";
            case "String":   return "const char*";
            case "CString":  return "const char*";
            case "Bool":     return "bool";
            case "Any":      return "void*";

            case "Pointer":        return "void*";
            case "OpaquePointer":  return "void*";
            case "FuncPointer":    return "void (*)(void)";
            case "SizeT":          return "size_t";

            case "Int8":   return "int8_t";
            case "Int16":  return "int16_t";
            case "Int32":  return "int32_t";
            case "Int64":  return "int64_t";
            case "UInt8":  return "uint8_t";
            case "UInt16": return "uint16_t";
            case "UInt32": return "uint32_t";
            case "UInt64": return "uint64_t";

            default:
                if (clawType.startsWith("Ref<")) {
                    String inner = clawType.substring(4, clawType.length() - 1);
                    return mapClawFFITypeToCType(inner) + "*";
                }
                if (clawType.startsWith("CArray<")) {
                    String inner = clawType.substring(7, clawType.length() - 1);
                    return mapClawFFITypeToCType(inner) + "*";
                }
                // 外部定义类型直接使用名称
                return clawType;
        }
    }

    private String mapCallingConvention(CallingConvention cc) {
        switch (cc) {
            case STDCALL:  return "__stdcall";
            case FASTCALL: return "__fastcall";
            case THISCALL: return "__thiscall";
            default:       return null; // CDECL 为默认，不需声明
        }
    }
}

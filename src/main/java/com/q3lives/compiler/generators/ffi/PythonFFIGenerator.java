package com.q3lives.compiler.generators.ffi;


import com.q3lives.compiler.generators.ffi.FFIBindingTable.ExternConstant;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.ExternFunction;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.ExternParam;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.LinkDirective;
import com.q3lives.compiler.generators.ffi.platform.PlatformConstraint;
import java.util.*;

/**
 * Python 目标 FFI 代码生成器
 *
 * 将 FFIBindingTable 中的外部符号信息转换为 Python ctypes 代码
 *
 * 策略：
 *   - link "sqlite3" → ctypes.util.find_library("sqlite3") + ctypes.CDLL
 *   - extern type    → 类型别名/占位
 *   - extern function → 设置 argtypes / restype
 *   - extern const   → Python 常量赋值
 */
public class PythonFFIGenerator {

    private final FFIBindingTable bindingTable;

    public PythonFFIGenerator(FFIBindingTable bindingTable) {
        this.bindingTable = bindingTable;
    }

    // ================================================================
    //  生成 import 语句
    // ================================================================

    /**
     * 生成 Python ctypes import
     */
    public String generateImports() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ========== FFI Imports ==========\n");
        sb.append("import ctypes\n");
        sb.append("import ctypes.util\n");
        return sb.toString();
    }

    /**
     * 生成 Python runtime import
     */
    public String generateRuntimeImports() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ========== Runtime Imports ==========\n");
        sb.append("import claw_runtime\n");
        return sb.toString();
    }

    // ================================================================
    //  生成库加载代码
    // ================================================================

    /**
     * 生成 ctypes 库加载代码
     *
     * 输出示例：
     *   _lib_path_sqlite3 = ctypes.util.find_library("sqlite3")
     *   if _lib_path_sqlite3 is None:
     *       raise claw_runtime.ClawIOError("Cannot find library: sqlite3")
     *   _sqlite3 = ctypes.CDLL(_lib_path_sqlite3)
     */
    public String generateLibraryLoading() {
        List<LinkDirective> links = bindingTable.getAllLinks();
        if (links.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("# ========== Load External C Libraries ===========\n");

        for (LinkDirective link : links) {
            String safeName = toSafePythonName(link.libraryName);
            String varName = "_lib_" + safeName;

            sb.append("_lib_path_").append(safeName)
              .append(" = ctypes.util.find_library(\"").append(link.libraryName).append("\")\n");

            sb.append("if _lib_path_").append(safeName).append(" is None:\n");
            sb.append("    raise claw_runtime.ClawIOError(\"Cannot find library: ")
              .append(link.libraryName).append("\")\n");

            sb.append(varName).append(" = ctypes.CDLL(_lib_path_").append(safeName).append(")\n");
        }

        return sb.toString();
    }

    /**
     * 获取库的 Python 变量名
     *
     * "sqlite3" -> "_lib_sqlite3"
     */
    public String getLibraryVarName(String libraryName) {
        return "_lib_" + toSafePythonName(libraryName);
    }

    // ================================================================
    //  生成常量定义
    // ================================================================

    /**
     * 生成外部常量
     *
     * 输出示例：
     *   SQLITE_OK = 0
     *   SQLITE_ROW = 100
     */
    public String generateConstants() {
        Map<String, ExternConstant> constants = bindingTable.getAllConstants();
        if (constants.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("# ========== External Constants ==========\n");

        for (ExternConstant constant : constants.values()) {
            sb.append(constant.name).append(" = ").append(constant.value).append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    // ================================================================
    //  生成函数签名绑定
    // ================================================================

    /**
     * 生成 ctypes 函数签名设置
     *
     * 输出示例：
     *   _sqlite3.sqlite3_open.argtypes = [ctypes.c_char_p, ctypes.POINTER(ctypes.c_void_p)]
     *   _sqlite3.sqlite3_open.restype = ctypes.c_int
     */
    public String generateFunctionBindings() {
        Map<String, ExternFunction> functions = bindingTable.getAllFunctions();
        if (functions.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("# ========== External Function Signatures ==========\n");

        for (ExternFunction func : functions.values()) {
            // 优先使用函数绑定的库名，其次使用默认库
            String libVar = resolveLibraryVar(func);

            // argtypes
            sb.append(libVar).append(".").append(func.name).append(".argtypes = [");
            for (int i = 0; i < func.params.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(mapClawTypeToCtype(func.params.get(i).type));
            }
            sb.append("]\n");

            // restype
            sb.append(libVar).append(".").append(func.name).append(".restype = ");
            sb.append(mapClawTypeToCtype(func.returnType));
            sb.append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    // ================================================================
    //  生成包装函数（可选，提供更 Pythonic 的接口）
    // ================================================================

    /**
     * 生成 Python 包装函数
     *
     * 为每个 extern 函数生成一个 Python 包装，处理：
     *   - String ↔ bytes 自动转换
     *   - Ref<T> 参数的 byref 处理
     *   - 错误码检查
     */
    public String generateWrapperFunctions() {
        Map<String, ExternFunction> functions = bindingTable.getAllFunctions();
        if (functions.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("# ========== Python Wrapper Functions ==========\n");

        for (ExternFunction func : functions.values()) {
            String libVar = resolveLibraryVar(func);
            sb.append(generateSingleWrapper(func, libVar));
            sb.append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * 为单个函数生成包装
     */
    private String generateSingleWrapper(ExternFunction func, String libVar) {
        StringBuilder sb = new StringBuilder();

        // 检查是否需要包装（有 String 或 Ref 参数时需要）
        boolean needsWrapper = false;
        for (ExternParam param : func.params) {
            if ("String".equals(param.type) || "CString".equals(param.type)
                || param.type.startsWith("Ref<")) {
                needsWrapper = true;
                break;
            }
        }
        if ("String".equals(func.returnType) || "CString".equals(func.returnType)) {
            needsWrapper = true;
        }

        if (!needsWrapper) {
            // 简单别名
            sb.append("# ").append(func.name).append(": no wrapper needed, use ")
              .append(libVar).append(".").append(func.name).append(" directly\n");
            return sb.toString();
        }

        // 生成类型注解
        sb.append("def _claw_ffi_").append(func.name).append("(");
        for (int i = 0; i < func.params.size(); i++) {
            if (i > 0) sb.append(", ");
            ExternParam param = func.params.get(i);
            sb.append(param.name);
            String pyType = mapClawTypeToPythonTypeHint(param.type);
            if (pyType != null) {
                sb.append(": ").append(pyType);
            }
        }
        sb.append(")");

        String returnPyType = mapClawTypeToPythonTypeHint(func.returnType);
        if (returnPyType != null) {
            sb.append(" -> ").append(returnPyType);
        }
        sb.append(":\n");

        // 函数体：参数转换
        sb.append("    \"\"\"Wrapper for C function: ").append(func.name).append("\"\"\"\n");

        // 转换参数
        List<String> callArgs = new ArrayList<>();
        for (ExternParam param : func.params) {
            if ("String".equals(param.type) || "CString".equals(param.type)) {
                String encodedVar = param.name + "_c";
                sb.append("    ").append(encodedVar).append(" = ");
                sb.append(param.name).append(".encode('utf-8') if isinstance(")
                  .append(param.name).append(", str) else ").append(param.name).append("\n");
                callArgs.add(encodedVar);
            } else if (param.type.startsWith("Ref<")) {
                callArgs.add("ctypes.byref(" + param.name + ")");
            } else {
                callArgs.add(param.name);
            }
        }

        // 调用
        sb.append("    _result = ").append(libVar).append(".").append(func.name).append("(");
        sb.append(String.join(", ", callArgs));
        sb.append(")\n");

        // 返回值转换
        if ("String".equals(func.returnType) || "CString".equals(func.returnType)) {
            sb.append("    return _result.decode('utf-8') if _result else \"\"\n");
        } else if ("Void".equals(func.returnType)) {
            sb.append("    return None\n");
        } else {
            sb.append("    return _result\n");
        }

        return sb.toString();
    }

    // ================================================================
    //  生成完整的 FFI 代码片段
    // ================================================================

    /**
     * 生成完整的 Python FFI 代码
     * 插入到生成的 .py 文件头部
     */
    public String generateAll() {
        if (!bindingTable.hasExternDeclarations()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(generateImports());
        sb.append("\n");
        sb.append(generateRuntimeImports());
        sb.append("\n");
        sb.append(generatePlatformDetection());
        sb.append("\n");

        // 如果有平台条件约束，使用条件加载；否则使用默认库加载
        boolean hasPlatformConstraints = false;
        for (FFIBindingTable.ExternBlock block : bindingTable.getExternBlocks()) {
            if (!block.platform.isUniversal()) {
                hasPlatformConstraints = true;
                break;
            }
        }

        if (hasPlatformConstraints) {
            sb.append(generatePlatformConditionalLoading(bindingTable));
        } else {
            sb.append(generateLibraryLoading());
        }
        sb.append("\n");

        sb.append(generateConstants());
        sb.append(generateStructDefinitions());
        sb.append(generateEnumDefinitions());
        sb.append(generateCallbackDefinitions());
        sb.append(generateMacroDefinitions());
        sb.append(generateFunctionBindings());
        sb.append(generateWrapperFunctions());
        return sb.toString();
    }

    // ================================================================
    //  Claw FFI 类型 → Python ctypes 类型映射
    // ================================================================

    /**
     * Claw FFI 类型映射到 ctypes 表达式
     */
    public static String mapClawTypeToCtype(String clawType) {
        if (clawType == null) return "None";

        switch (clawType) {
            case "Void":     return "None";
            case "Int":      return "ctypes.c_int";
            case "Float":    return "ctypes.c_double";
            case "String":   return "ctypes.c_char_p";
            case "CString":  return "ctypes.c_char_p";
            case "Bool":     return "ctypes.c_bool";
            case "Any":      return "ctypes.c_void_p";

            case "Pointer":        return "ctypes.c_void_p";
            case "OpaquePointer":  return "ctypes.c_void_p";
            case "FuncPointer":    return "ctypes.c_void_p";
            case "SizeT":          return "ctypes.c_size_t";

            case "Int8":   return "ctypes.c_int8";
            case "Int16":  return "ctypes.c_int16";
            case "Int32":  return "ctypes.c_int32";
            case "Int64":  return "ctypes.c_int64";
            case "UInt8":  return "ctypes.c_uint8";
            case "UInt16": return "ctypes.c_uint16";
            case "UInt32": return "ctypes.c_uint32";
            case "UInt64": return "ctypes.c_uint64";

            default:
                if (clawType.startsWith("Ref<")) {
                    String inner = clawType.substring(4, clawType.length() - 1);
                    return "ctypes.POINTER(" + mapClawTypeToCtype(inner) + ")";
                }
                if (clawType.startsWith("CArray<")) {
                    String inner = clawType.substring(7, clawType.length() - 1);
                    return "ctypes.POINTER(" + mapClawTypeToCtype(inner) + ")";
                }
                // 外部类型作为 void* 处理
                return "ctypes.c_void_p";
        }
    }

    /**
     * Claw 类型映射到 Python 类型注解
     */
    public static String mapClawTypeToPythonTypeHint(String clawType) {
        if (clawType == null) return null;

        switch (clawType) {
            case "Void":     return "None";
            case "Int":      return "int";
            case "Float":    return "float";
            case "String":   return "str";
            case "CString":  return "str";
            case "Bool":     return "bool";
            default:         return null;
        }
    }


    // PythonFFIGenerator 中的平台适配

/**
 * 生成 Python 运行时平台检测代码
 */
public String generatePlatformDetection() {
    StringBuilder sb = new StringBuilder();

    sb.append("# ========== Platform Detection ==========\n");
    sb.append("import sys\n");
    sb.append("import platform as _platform_mod\n");

    sb.append("_CLAW_PLATFORM = {\n");
    sb.append("    'windows': sys.platform == 'win32',\n");
    sb.append("    'linux':   sys.platform.startswith('linux'),\n");
    sb.append("    'macos':   sys.platform == 'darwin',\n");
    sb.append("    'freebsd': sys.platform.startswith('freebsd'),\n");
    sb.append("}[next((k for k, v in {\n");
    sb.append("    'windows': sys.platform == 'win32',\n");
    sb.append("    'linux':   sys.platform.startswith('linux'),\n");
    sb.append("    'macos':   sys.platform == 'darwin',\n");
    sb.append("}.items() if v), 'unknown')]\n");

    // 简化版
    sb.append("def _claw_is_platform(*names: str) -> bool:\n");
    sb.append("    \"\"\"Check if running on one of the specified platforms\"\"\"\n");
    sb.append("    current = sys.platform\n");
    sb.append("    for name in names:\n");
    sb.append("        if name == 'windows' and current == 'win32': return True\n");
    sb.append("        if name == 'linux' and current.startswith('linux'): return True\n");
    sb.append("        if name == 'macos' and current == 'darwin': return True\n");
    sb.append("        if name == 'freebsd' and current.startswith('freebsd'): return True\n");
    sb.append("    return False\n");

    sb.append("def _claw_is_arch(*names: str) -> bool:\n");
    sb.append("    \"\"\"Check if running on one of the specified architectures\"\"\"\n");
    sb.append("    machine = _platform_mod.machine().lower()\n");
    sb.append("    mapping = {'x86_64': ['x86_64','amd64'], 'arm64': ['aarch64','arm64'],\n");
    sb.append("               'x86': ['i386','i686','x86'], 'arm': ['armv7l','arm']}\n");
    sb.append("    for name in names:\n");
    sb.append("        if machine in mapping.get(name, [name]): return True\n");
    sb.append("    return False\n");

    return sb.toString();
}

/**
 * 生成平台条件加载
 *
 * Python 用 if 语句而非预处理器
 */
public String generatePlatformConditionalLoading(FFIBindingTable fullTable) {
    StringBuilder sb = new StringBuilder();

    for (FFIBindingTable.ExternBlock block : fullTable.getExternBlocks()) {
        if (block.platform != null && !block.platform.isUniversal()) {
            // 生成 if 条件
            sb.append(generatePythonPlatformIf(block.platform));
            sb.append(generateBlockLoadingCode(block, "    "));  // 缩进
            sb.append("\n");
        } else {
            sb.append(generateBlockLoadingCode(block, ""));
        }
    }

    return sb.toString();
}

/**
 * 生成单个 extern block 的加载代码
 *
 * @param block extern 块
 * @param indent 缩进
 * @return 加载代码
 */
public String generateBlockLoadingCode(FFIBindingTable.ExternBlock block, String indent) {
    StringBuilder sb = new StringBuilder();

    // 生成库加载
    if (!block.links.isEmpty()) {
        for (LinkDirective link : block.links) {
            String libVar = getLibraryVarName(link.libraryName);
            sb.append(indent).append(libVar).append(" = ").append("ctypes.CDLL('")
              .append(getLibraryFileName(link.libraryName)).append("')\n");
        }
    }

    // 生成函数绑定
    for (ExternFunction func : block.functions) {
        String libVar = getLibraryVarName(func.libraryName != null ? func.libraryName : "default");

        // 设置参数类型
        if (!func.params.isEmpty()) {
            sb.append(indent).append(libVar).append(".").append(func.name)
              .append(".argtypes = [");
            for (int i = 0; i < func.params.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(mapClawTypeToCtype(func.params.get(i).type));
            }
            sb.append("]\n");
        }

        // 设置返回类型
        sb.append(indent).append(libVar).append(".").append(func.name)
          .append(".restype = ").append(mapClawTypeToCtype(func.returnType)).append("\n");
    }

    return sb.toString();
}

public String generatePythonPlatformIf(PlatformConstraint constraint) {
    List<String> conditions = new ArrayList<>();

    if (!constraint.getPlatforms().isEmpty()) {
        List<String> quoted = new ArrayList<>();
        for (String p : constraint.getPlatforms()) {
            quoted.add("\"" + p + "\"");
        }
        conditions.add("_claw_is_platform(" + String.join(", ", quoted) + ")");
    }

    if (!constraint.getArchitectures().isEmpty()) {
        List<String> quoted = new ArrayList<>();
        for (String a : constraint.getArchitectures()) {
            quoted.add("\"" + a + "\"");
        }
        conditions.add("_claw_is_arch(" + String.join(", ", quoted) + ")");
    }

    return "if " + String.join(" and ", conditions) + ":";
}


    // ================================================================
    //  结构体、枚举、回调、宏生成
    // ================================================================

    /**
     * 生成 ctypes 结构体定义
     *
     * 输出示例：
     *   # ========== External Structs ==========
     *   class Point(ctypes.Structure):
     *       _fields_ = [("x", ctypes.c_double), ("y", ctypes.c_double)]
     */
    public String generateStructDefinitions() {
        List<FFIBindingTable.ExternStruct> structs = bindingTable.getAllStructs();
        if (structs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("# ========== External Structs ==========\n");

        for (FFIBindingTable.ExternStruct struct : structs) {
            sb.append("class ").append(struct.name).append("(ctypes.Structure):\n");

            if (struct.description != null && !struct.description.isEmpty()) {
                sb.append("    \"\"\"").append(struct.description).append("\"\"\"\n");
            }

            if (struct.packed) {
                sb.append("    _pack_ = 1\n");
            }
            if (struct.alignment > 0) {
                sb.append("    _align_ = ").append(struct.alignment).append("\n");
            }

            sb.append("    _fields_ = [\n");
            for (FFIBindingTable.StructField field : struct.fields) {
                sb.append("        (\"").append(field.name).append("\", ")
                  .append(mapClawTypeToCtype(field.type)).append("),\n");
            }
            sb.append("    ]\n\n");
        }

        return sb.toString();
    }

    /**
     * 生成 Python 枚举定义
     *
     * 输出示例：
     *   # ========== External Enums ==========
     *   class CURLcode(enum.IntEnum):
     *       CURLE_OK = 0
     *       CURLE_UNSUPPORTED_PROTOCOL = 1
     */
    public String generateEnumDefinitions() {
        List<FFIBindingTable.ExternEnum> enums = bindingTable.getAllEnums();
        if (enums.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("# ========== External Enums ==========\n");
        sb.append("import enum\n\n");

        for (FFIBindingTable.ExternEnum en : enums) {
            String baseClass = en.isBitmask ? "enum.IntFlag" : "enum.IntEnum";
            sb.append("class ").append(en.name).append("(").append(baseClass).append("):\n");

            if (en.description != null && !en.description.isEmpty()) {
                sb.append("    \"\"\"").append(en.description).append("\"\"\"\n");
            }

            for (FFIBindingTable.EnumMember member : en.members) {
                sb.append("    ").append(member.name).append(" = ").append(member.value).append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 生成回调函数类型定义（CFUNCTYPE）
     *
     * 输出示例：
     *   # ========== External Callbacks ==========
     *   WriteCallback = ctypes.CFUNCTYPE(ctypes.c_size_t,
     *       ctypes.c_void_p, ctypes.c_size_t, ctypes.c_size_t, ctypes.c_void_p)
     */
    public String generateCallbackDefinitions() {
        List<FFIBindingTable.ExternCallback> callbacks = bindingTable.getAllCallbacks();
        if (callbacks.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("# ========== External Callbacks ==========\n");

        for (FFIBindingTable.ExternCallback callback : callbacks) {
            if (callback.description != null && !callback.description.isEmpty()) {
                sb.append("# ").append(callback.description).append("\n");
            }

            sb.append(callback.name).append(" = ctypes.CFUNCTYPE(");
            sb.append(mapClawTypeToCtype(callback.returnType));

            for (FFIBindingTable.ExternParam param : callback.params) {
                sb.append(", ").append(mapClawTypeToCtype(param.type));
            }

            sb.append(")\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * 生成宏定义
     *
     * 常量宏 → Python 常量
     * 函数宏 → Python lambda/函数
     */
    public String generateMacroDefinitions() {
        List<FFIBindingTable.ExternMacro> macros = bindingTable.getAllMacros();
        if (macros.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("# ========== External Macros ==========\n");

        for (FFIBindingTable.ExternMacro macro : macros) {
            if (macro.kind == FFIBindingTable.MacroKind.CONSTANT) {
                sb.append(macro.name).append(" = ").append(macro.value).append("\n");
            } else if (macro.kind == FFIBindingTable.MacroKind.FUNCTION) {
                sb.append("def ").append(macro.name).append("(");
                for (int i = 0; i < macro.params.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(macro.params.get(i).name);
                }
                sb.append("):\n");

                if (macro.expansion != null) {
                    sb.append("    return ").append(macro.expansion).append("\n");
                } else {
                    sb.append("    # Macro expansion not available\n");
                    sb.append("    pass\n");
                }
            }
        }

        sb.append("\n");
        return sb.toString();
    }

    // ================================================================
    //  辅助方法
    // ================================================================

    /**
     * 解析函数所属的库变量名
     *
     * @param func 外部函数
     * @return 库变量名，如 "_lib_sqlite3"
     */
    private String resolveLibraryVar(ExternFunction func) {
        if (func.libraryName != null && !func.libraryName.isEmpty()) {
            return getLibraryVarName(func.libraryName);
        }
        return getDefaultLibraryVar();
    }

    private String getDefaultLibraryVar() {
        List<LinkDirective> links = bindingTable.getAllLinks();
        if (links.isEmpty()) {
            return "_lib_unknown";
        }
        return getLibraryVarName(links.get(0).libraryName);
    }

    /**
     * 将库名转为安全的 Python 变量名片段
     * "curl" -> "curl"
     * "openssl" -> "openssl"
     */
    public static String toSafePythonName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    public String getLibraryFileName(String libraryName) {
        // 简单实现：返回库文件名
        return libraryName;
    }
}

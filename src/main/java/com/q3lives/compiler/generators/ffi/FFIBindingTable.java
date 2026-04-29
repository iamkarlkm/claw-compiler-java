package com.q3lives.compiler.generators.ffi;

import com.q3lives.compiler.generators.ffi.platform.PlatformConstraint;
import com.q3lives.compiler.generators.ffi.platform.PlatformLibraryMapper;
import com.q3lives.compiler.generators.ffi.platform.TargetTriple;
import java.util.*;


/**
 * FFI 绑定表（扩展版）
 *
 * 存储所有 extern "C" 声明的完整信息， 供 C / Python / Java 三个目标代码生成器使用。
 *
 * 扩展内容： - 结构体 (struct) 声明 - 枚举 (enum) 声明 - 函数指针类型 (callback) 声明 - 宏定义 (macro)
 * 声明 - 平台条件编译 - 线程安全标记 - 内存管理提示 - 版本约束 - 函数分组（按库/模块）
 */
public class FFIBindingTable {

    // ================================================================
    //  核心数据：ExternBlock 列表
    // ================================================================
    private final List<ExternBlock> externBlocks = new ArrayList<>();
    private TargetTriple targetTriple;

    // ================================================================
    //  全局合并索引（跨所有 ExternBlock 的快速查找）
    // ================================================================
    private final Map<String, ExternFunction> globalFunctions = new LinkedHashMap<>();
    private final Map<String, ExternType> globalTypes = new LinkedHashMap<>();
    private final Map<String, ExternConstant> globalConstants = new LinkedHashMap<>();
    private final List<ExternStruct> globalStructs = new ArrayList<>(); // NEW
    private final List<ExternEnum> globalEnums = new ArrayList<>(); // NEW
    private final List<ExternCallback> globalCallbacks = new ArrayList<>(); // NEW
    private final List<ExternMacro> globalMacros = new ArrayList<>(); // NEW
    private final List<LinkDirective> globalLinks = new ArrayList<>();
    private final List<String> globalIncludes = new ArrayList<>();

    public List<String> getAllIncludes() {
        return globalIncludes;
    }

    public Map<String, ExternType> getAllTypes() {
        return globalTypes;
    }

    public Map<String, ExternConstant> getAllConstants() {
        return globalConstants;
    }

    public Map<String, ExternFunction> getAllFunctions() {
        return globalFunctions;
    }

    public List<FFIBindingTable.ExternStruct> getAllStructs() {
        return globalStructs;
    }

    public List<FFIBindingTable.ExternEnum> getAllEnums() {
        return globalEnums;
    }

    public List<FFIBindingTable.ExternCallback> getAllCallbacks() {
        return globalCallbacks;
    }

    public List<FFIBindingTable.ExternMacro> getAllMacros() {
        return globalMacros;
    }

    public List<LinkDirective> getAllLinks() {
        return globalLinks;
    }

    public boolean hasExternDeclarations() {
        return !externBlocks.isEmpty() || !globalFunctions.isEmpty()
            || !globalTypes.isEmpty() || !globalConstants.isEmpty()
            || !globalStructs.isEmpty() || !globalEnums.isEmpty()
            || !globalCallbacks.isEmpty() || !globalMacros.isEmpty();
    }

    public Object findType(String typeName) {
        if (typeName == null) return null;
        ExternType type = globalTypes.get(typeName);
        if (type != null) return type;
        for (ExternStruct struct : globalStructs) {
            if (typeName.equals(struct.name)) return struct;
        }
        for (ExternEnum enumType : globalEnums) {
            if (typeName.equals(enumType.name)) return enumType;
        }
        for (ExternCallback callback : globalCallbacks) {
            if (typeName.equals(callback.name)) return callback;
        }
        return null;
    }

    public List<String> getLibraryNames() {
        List<String> names = new ArrayList<>();
        for (LinkDirective link : globalLinks) {
            if (link.libraryName != null && !names.contains(link.libraryName)) {
                names.add(link.libraryName);
            }
        }
        return Collections.unmodifiableList(names);
    }

    public boolean isExternSymbol(String name) {
        if (name == null) return false;
        if (globalFunctions.containsKey(name)
            || globalConstants.containsKey(name)
            || globalTypes.containsKey(name)) {
            return true;
        }
        for (ExternStruct s : globalStructs) {
            if (name.equals(s.name)) return true;
        }
        for (ExternEnum e : globalEnums) {
            if (name.equals(e.name)) return true;
        }
        for (ExternCallback c : globalCallbacks) {
            if (name.equals(c.name)) return true;
        }
        return false;
    }

    public ExternFunction findFunction(String name) {
        return globalFunctions.get(name);
    }

    // ================================================================
    //  1. ExternBlock — 一个 extern "C" { } 块
    // ================================================================
    public static class ExternBlock {

        public int startLine;
        public int endLine;

        // 原有声明
        public final List<LinkDirective> links = new ArrayList<>();
        public final List<String> includes = new ArrayList<>();
        public final List<ExternType> types = new ArrayList<>();
        public final List<ExternFunction> functions = new ArrayList<>();
        public final List<ExternConstant> constants = new ArrayList<>();

        // ===== 新增声明 =====
        public final List<ExternStruct> structs = new ArrayList<>();  // NEW
        public final List<ExternEnum> enums = new ArrayList<>();  // NEW
        public final List<ExternCallback> callbacks = new ArrayList<>();  // NEW
        public final List<ExternMacro> macros = new ArrayList<>();  // NEW

        // ===== 块级元数据 =====
        public PlatformConstraint platform = PlatformConstraint.UNIVERSAL;  // NEW: 平台约束
        VersionConstraint version = null;  // NEW: 版本约束
        public String comment = null;  // NEW: 块注释/描述
    }

    // ================================================================
    //  2. LinkDirective — 链接指令（扩展）
    // ================================================================
    public static class LinkDirective {

        public final String libraryName;       // 库名: "sqlite3"
        public String headerFile;        // 头文件: "sqlite3.h"

        // ===== 新增字段 =====
        public LinkType linkType;          // NEW: 链接类型
        public String searchPath;        // NEW: 库搜索路径
        public String minVersion;        // NEW: 最低版本要求
        public boolean optional;          // NEW: 是否可选（缺失时不报错）

        public LinkDirective(String libraryName, String headerFile) {
            this.libraryName = libraryName;
            this.headerFile = headerFile;
            this.linkType = LinkType.DYNAMIC;
            this.optional = false;
        }

        @Override
        public String toString() {
            return "link \"" + libraryName + "\""
                + (linkType != LinkType.DYNAMIC ? " [" + linkType + "]" : "")
                + (optional ? " [optional]" : "");
        }
    }

    public static class VersionConstraint {

        public VersionConstraint() {
        }
    }

    /**
     * 链接类型
     */
    public enum LinkType {
        DYNAMIC, // 动态链接 (.so / .dll / .dylib)
        STATIC, // 静态链接 (.a / .lib)
        FRAMEWORK, // macOS Framework
        HEADER_ONLY  // 仅头文件（header-only library）
    }

    // ================================================================
    //  3. ExternType — 类型别名（原有，略微扩展）
    // ================================================================
    public static class ExternType {

        public final String clawTypeName;    // Claw 中的类型名: "sqlite3"
        public final String cMappingType;    // 映射到的 FFI 类型: "OpaquePointer"

        // ===== 新增字段 =====
        public String cOriginalType;   // NEW: C 中的原始类型名: "sqlite3*"
        public boolean isNullable;      // NEW: 是否允许 null
        public String description;     // NEW: 类型描述

        public ExternType(String clawTypeName, String cMappingType) {
            this.clawTypeName = clawTypeName;
            this.cMappingType = cMappingType;
            this.isNullable = true;
        }

        @Override
        public String toString() {
            return "type " + clawTypeName + " = " + cMappingType;
        }
    }

    // ================================================================
    //  4. ExternFunction — 外部函数（大幅扩展）
    // ================================================================
    public static class ExternFunction {

        public final String name;                    // 函数名
        public final List<ExternParam> params;       // 参数列表
        public final String returnType;              // 返回类型
        public final boolean isVariadic;             // 是否变参

        // ===== 新增字段 =====
        public CallingConvention callingConvention;  // NEW: 调用约定
        public ThreadSafety threadSafety;       // NEW: 线程安全性标记
        public MemoryOwnership returnOwnership;    // NEW: 返回值内存归属
        public boolean canFail;            // NEW: 是否可能失败（返回错误码）
        public String failureIndicator;   // NEW: 失败指示（如 "returns -1", "returns NULL"）
        public String description;        // NEW: 函数描述
        public String cPrototype;         // NEW: 原始 C 原型字符串（文档用）
        public boolean deprecated;         // NEW: 是否已废弃
        public String deprecatedAlt;      // NEW: 废弃替代函数
        public PlatformConstraint platformConstraint;
        public String libraryName;

        public ExternFunction(String name, List<ExternParam> params,
            String returnType, boolean isVariadic) {
            this.name = name;
            this.params = params != null ? params : new ArrayList<>();
            this.returnType = returnType;
            this.isVariadic = isVariadic;
            this.callingConvention = CallingConvention.CDECL;
            this.threadSafety = ThreadSafety.UNKNOWN;
            this.returnOwnership = MemoryOwnership.UNKNOWN;
            this.canFail = false;
            this.deprecated = false;
            this.platformConstraint = PlatformConstraint.UNIVERSAL;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("function ").append(name).append("(");
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(params.get(i));
            }
            if (isVariadic) {
                if (!params.isEmpty()) {
                    sb.append(", ");
                }
                sb.append("...");
            }
            sb.append(") -> ").append(returnType);
            return sb.toString();
        }
    }

    /**
     * 调用约定
     */
    public enum CallingConvention {
        CDECL, // 默认 C 调用约定（大多数情况）
        STDCALL, // Windows API 常用
        FASTCALL, // 寄存器传参
        THISCALL     // C++ 成员函数
    }

    /**
     * 线程安全性标记
     */
    public enum ThreadSafety {
        THREAD_SAFE, // 可从多线程安全调用
        NOT_THREAD_SAFE, // 非线程安全（需要外部同步）
        REENTRANT, // 可重入
        UNKNOWN          // 未知
    }

    /**
     * 返回值的内存所有权
     *
     * 用于指导 Claw 编译器生成正确的内存管理代码
     */
    public enum MemoryOwnership {
        CALLER_OWNS, // 调用者拥有，需要手动释放
        CALLEE_OWNS, // 被调用者拥有，不要释放
        SHARED, // 共享所有权（引用计数等）
        STACK, // 栈上分配，函数返回后无效
        UNKNOWN          // 未知
    }

    // ================================================================
    //  5. ExternParam — 函数参数（扩展）
    // ================================================================
    public static class ExternParam {

        public final String name;           // 参数名
        public final String type;           // 参数类型

        // ===== 新增字段 =====
        public ParamDirection direction;    // NEW: 参数方向
        public boolean nullable;     // NEW: 是否允许 null
        public String defaultValue; // NEW: 默认值（如果有）
        public String description;  // NEW: 参数描述

        public ExternParam(String name, String type) {
            this.name = name;
            this.type = type;
            this.direction = ParamDirection.IN;
            this.nullable = false;
        }

        @Override
        public String toString() {
            return name + ": " + type;
        }
    }

    /**
     * 参数方向
     *
     * 对应 C 中的常见模式： IN: const T* 或值传递 OUT: T* （输出参数） IN_OUT: T* （输入输出参数）
     */
    public enum ParamDirection {
        IN, // 输入参数（只读）
        OUT, // 输出参数（只写，调用后填充）
        IN_OUT    // 输入输出参数（读写）
    }

    // ================================================================
    //  6. ExternConstant — 常量（原有，略微扩展）
    // ================================================================
    public static class ExternConstant {

        public  String name;
        public  String type;
        public  String value;

        // ===== 新增字段 =====
        public String group;          // NEW: 常量分组名（如 "SQLITE_STATUS_*"）
        public String description;    // NEW: 常量描述

        public ExternConstant(String name, String type, String value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }

        ExternConstant() {
            this.name = null;
            this.type = null;
            this.value = null;
            this.group = null;
            this.description = null;
        }

        @Override
        public String toString() {
            return "const " + name + ": " + type + " = " + value;
        }
    }

    // ================================================================
    //  7. ExternStruct — C 结构体声明（全新）
    // ================================================================
    /**
     * C 结构体在 Claw 中的映射
     *
     * 当 C 库暴露了结构体而非不透明指针时使用
     *
     * Claw 语法： extern "C" { struct Point { x: Float y: Float } }
     */
    public static class ExternStruct {

        public  String name;                      // 结构体名
        public  List<StructField> fields;         // 字段列表

        // ===== 元数据 =====
        public boolean packed;           // NEW: 是否紧凑布局（__attribute__((packed))）
        public int alignment;        // NEW: 对齐要求（字节数，0 = 默认）
        public String description;      // NEW: 描述

        public ExternStruct(String name) {
            this.name = name;
            this.fields = new ArrayList<>();
            this.packed = false;
            this.alignment = 0;
        }

        ExternStruct() {
            this.name = null;
            this.fields = new ArrayList<>();
            this.packed = false;
            this.alignment = 0;
            this.description = null;
        }

        /**
         * 计算结构体的大小（近似值，用于代码生成参考）
         */
        public int estimateSize() {
            int size = 0;
            for (StructField field : fields) {
                size += estimateFieldSize(field.type);
            }
            return size;
        }

        private int estimateFieldSize(String type) {
            switch (type) {
                case "Int8":
                case "UInt8":
                case "Bool":
                    return 1;
                case "Int16":
                case "UInt16":
                    return 2;
                case "Int32":
                case "UInt32":
                case "Int":
                case "Float":
                    return 4;
                case "Int64":
                case "UInt64":
                case "SizeT":
                    return 8;
                case "Pointer":
                case "OpaquePointer":
                case "CString":
                    return 8; // 64-bit
                default:
                    return 8; // 假设指针大小
            }
        }

        @Override
        public String toString() {
            return "struct " + name + " { " + fields.size() + " fields }";
        }
    }

    /**
     * 结构体字段
     */
    public static class StructField {

        public  String name;        // 字段名
        public  String type;        // 字段类型
        public int bitfield;          // NEW: 位域宽度（0 = 非位域）
        public String description;       // NEW: 字段描述

        public StructField(String name, String type) {
            this.name = name;
            this.type = type;
            this.bitfield = 0;
        }

        @Override
        public String toString() {
            return name + ": " + type + (bitfield > 0 ? " :" + bitfield : "");
        }
    }

    // ================================================================
    //  8. ExternEnum — C 枚举声明（全新）
    // ================================================================
    /**
     * C 枚举在 Claw 中的映射
     *
     * Claw 语法： extern "C" { enum CURLcode { CURLE_OK = 0
     * CURLE_UNSUPPORTED_PROTOCOL = 1 CURLE_FAILED_INIT = 2 // ... } }
     */
    public static class ExternEnum {

        public  String name;                         // 枚举类型名
        public  List<EnumMember> members;            // 枚举成员列表

        // ===== 元数据 =====
        public String baseType;          // NEW: 底层类型（默认 "Int"）
        public boolean isBitmask;        // NEW: 是否为位掩码枚举（成员值可 OR 组合）
        public String description;       // NEW: 描述

        public ExternEnum(String name) {
            this.name = name;
            this.members = new ArrayList<>();
            this.baseType = "Int";
            this.isBitmask = false;
        }

        ExternEnum() {
            this.name = null;
            this.members = new ArrayList<>();
            this.baseType = "Int";
            this.isBitmask = false;
            this.description = null;
        }

        @Override
        public String toString() {
            return "enum " + name + " { " + members.size() + " members }";
        }
    }

    /**
     * 枚举成员
     */
    public static class EnumMember {

        public final String name;        // 成员名
        public final String value;       // 成员值（字符串表示）
        public String description;       // NEW: 描述

        public EnumMember(String name, String value) {
            this.name = name;
            this.value = value;
            this.description = null;
        }

        @Override
        public String toString() {
            return name + " = " + value;
        }
    }

    // ================================================================
    //  9. ExternCallback — 回调函数类型（全新）
    // ================================================================
    /**
     * C 回调函数（函数指针类型）在 Claw 中的映射
     *
     * Claw 语法： extern "C" { callback WriteCallback( data: Pointer, size: SizeT,
     * nmemb: SizeT, userdata: Pointer ) -> SizeT }
     *
     * 这对应 C 中的: typedef size_t (*WriteCallback)(void* data, size_t size, size_t
     * nmemb, void* userdata);
     */
    public static class ExternCallback {

        public  String name;                    // 回调类型名
        public  List<ExternParam> params;       // 参数列表
        public  String returnType;              // 返回类型

        // ===== 元数据 =====
        public CallingConvention callingConvention;  // NEW: 调用约定
        public String description;                   // NEW: 描述

        public ExternCallback(String name, List<ExternParam> params, String returnType) {
            this.name = name;
            this.params = params != null ? params : new ArrayList<>();
            this.returnType = returnType;
            this.callingConvention = CallingConvention.CDECL;
        }

        ExternCallback() {
            this.name = null;
            this.params = new ArrayList<>();
            this.returnType = null;
            this.callingConvention = CallingConvention.CDECL;
            this.description = null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("callback ").append(name).append("(");
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(params.get(i));
            }
            sb.append(") -> ").append(returnType);
            return sb.toString();
        }
    }

    // ================================================================
    //  10. ExternMacro — C 宏定义（全新）
    // ================================================================
    /**
     * C 宏在 Claw 中的映射
     *
     * 主要用于复杂的常量定义或简单的宏函数
     *
     * Claw 语法： extern "C" { macro SQLITE_VERSION_NUMBER: Int = 3039004 macro
     * MAKEWORD(lo: Int, hi: Int) -> Int }
     */
    public static class ExternMacro {

        public  String name;
        public  MacroKind kind;

        // 常量宏
        public String type;              // 类型（常量宏）
        public String value;             // 值（常量宏）

        // 函数宏
        public List<ExternParam> params; // 参数（函数宏）
        public String returnType;        // 返回类型（函数宏）
        public String expansion;         // 展开表达式（如有）

        public String description;

        public ExternMacro(String name, MacroKind kind) {
            this.name = name;
            this.kind = kind;
        }

        ExternMacro() {
            this.name = null;
            this.kind = MacroKind.CONSTANT;
            this.type = null;
            this.value = null;
            this.params = new ArrayList<>();
            this.returnType = null;
            this.expansion = null;
            this.description = null;
        }

        @Override
        public String toString() {
            return "macro " + name + " [" + kind + "]";
        }
    }

    /**
     * 宏类型
     */
    public enum MacroKind {
        CONSTANT, // 常量宏: #define FOO 42
        FUNCTION     // 函数宏: #define MAX(a,b) ((a)>(b)?(a):(b))
    }

    // // ================================================================
    // //  11. PlatformConstraint — 平台条件编译（全新）
    // // ================================================================
    // /**
    //  * 平台约束
    //  *
    //  * Claw 语法：
    //  *   extern "C" @platform("windows") {
    //  *       link "kernel32"
    //  *       function GetLastError() -> UInt32
    //  *   }
    //  *
    //  *   extern "C" @platform("linux", "macos") {
    //  *       link "pthread"
    //  *       function pthread_create(...) -> Int
    //  *   }
    //  */
    // public static class PlatformConstraint {
    //     public final Set<String> platforms;          // 允许的平台: "windows", "linux", "macos", "android", "ios"
    //     public final Set<String> architectures;      // 允许的架构: "x86_64", "arm64", "x86", "arm"
    //     public PlatformConstraint() {
    //         this.platforms = new LinkedHashSet<>();
    //         this.architectures = new LinkedHashSet<>();
    //     }
    //     /**
    //      * 检查当前目标是否满足约束
    //      */
    //     public boolean matches(String targetPlatform, String targetArch) {
    //         boolean platformOK = platforms.isEmpty() || platforms.contains(targetPlatform);
    //         boolean archOK = architectures.isEmpty() || architectures.contains(targetArch);
    //         return platformOK && archOK;
    //     }
    //     @Override
    //     String toString() {
    //         return "@platform(" + String.join(", ", platforms) + ")"
    //             + (!architectures.isEmpty() ? " @arch(" + String.join(", ", architectures) + ")" : "");
    //     }
    // }
    // package claw.compiler.generators.ffi.platform;
    // import java.util.Collections;
    // import java.util.LinkedHashSet;
    // import java.util.Set;
    // ================================================================
    //  FFIBindingTable 公共方法
    // ================================================================
    /**
     * 获取所有 ExternBlock
     */
    public List<ExternBlock> getExternBlocks() {
        return externBlocks;
    }

    /**
     * 创建新的 ExternBlock
     */
    public ExternBlock newExternBlock() {
        ExternBlock block = new ExternBlock();
        return block;
    }

    /**
     * 将 ExternBlock 的内容合并到全局索引
     */
    public void indexBlock(ExternBlock block) {
        externBlocks.add(block);

        for (ExternFunction func : block.functions) {
            if (!globalFunctions.containsKey(func.name)) {
                globalFunctions.put(func.name, func);
            }
        }
        for (ExternType type : block.types) {
            if (!globalTypes.containsKey(type.clawTypeName)) {
                globalTypes.put(type.clawTypeName, type);
            }
        }
        for (ExternConstant constVal : block.constants) {
            if (!globalConstants.containsKey(constVal.name)) {
                globalConstants.put(constVal.name, constVal);
            }
        }
        for (ExternStruct struct : block.structs) {
            if (globalStructs.stream().noneMatch(s -> s.name.equals(struct.name))) {
                globalStructs.add(struct);
            }
        }
        for (ExternEnum enumType : block.enums) {
            if (globalEnums.stream().noneMatch(e -> e.name.equals(enumType.name))) {
                globalEnums.add(enumType);
            }
        }
        for (ExternCallback callback : block.callbacks) {
            if (globalCallbacks.stream().noneMatch(c -> c.name.equals(callback.name))) {
                globalCallbacks.add(callback);
            }
        }
        for (ExternMacro macro : block.macros) {
            if (globalMacros.stream().noneMatch(m -> m.name.equals(macro.name))) {
                globalMacros.add(macro);
            }
        }
        for (LinkDirective link : block.links) {
            if (link.libraryName != null
                && globalLinks.stream().noneMatch(l -> l.libraryName.equals(link.libraryName))) {
                globalLinks.add(link);
            }
        }
        for (String include : block.includes) {
            if (!globalIncludes.contains(include)) {
                globalIncludes.add(include);
            }
        }
    }

    /**
     * 按目标三元组过滤，返回仅包含匹配声明的新绑定表
     */
    public FFIBindingTable filterForPlatform(TargetTriple target) {
        FFIBindingTable filtered = new FFIBindingTable();
        filtered.targetTriple = target;

        for (ExternBlock block : getExternBlocks()) {
            if (!block.platform.matches(target)) {
                continue;
            }

            ExternBlock filteredBlock = filtered.newExternBlock();
            filteredBlock.startLine = block.startLine;
            filteredBlock.endLine = block.endLine;
            filteredBlock.platform = block.platform;
            filteredBlock.version = block.version;
            filteredBlock.comment = block.comment;

            for (LinkDirective link : block.links) {
                filteredBlock.links.add(filterLink(link, target));
            }

            for (String include : block.includes) {
                filteredBlock.includes.add(mapIncludeForPlatform(include, target));
            }

            filteredBlock.types.addAll(block.types);
            filteredBlock.constants.addAll(block.constants);
            filteredBlock.structs.addAll(block.structs);
            filteredBlock.enums.addAll(block.enums);
            filteredBlock.callbacks.addAll(block.callbacks);
            filteredBlock.macros.addAll(block.macros);

            for (ExternFunction func : block.functions) {
                if (func.platformConstraint.matches(target)) {
                    filteredBlock.functions.add(func);
                }
            }

            filtered.indexBlock(filteredBlock);
        }

        return filtered;
    }

    /**
     * 处理链接指令的平台差异
     */
    private LinkDirective filterLink(LinkDirective original, TargetTriple target) {
        LinkDirective link = new LinkDirective(original.libraryName, original.headerFile);
        link.linkType = original.linkType;
        link.searchPath = original.searchPath;
        link.minVersion = original.minVersion;
        link.optional = original.optional;

        String mappedName = PlatformLibraryMapper.mapLibraryName(original.libraryName, target);
        if (mappedName != null && !mappedName.equals(original.libraryName)) {
            link = new LinkDirective(mappedName, original.headerFile);
            link.linkType = original.linkType;
        }

        return link;
    }

    /**
     * 处理头文件的平台差异
     */
    private String mapIncludeForPlatform(String include, TargetTriple target) {
        if (target.isWindows()) {
            switch (include) {
                case "unistd.h":
                    return "io.h";
                case "pthread.h":
                    return "windows.h";
                case "dlfcn.h":
                    return "windows.h";
            }
        }
        return include;
    }

    /**
     * 获取绑定表的版本号（用于缓存）
     * 简单实现：使用 hashCode 作为版本号
     */
    public long getVersion() {
        return hashCode();
    }

    // ================================================================
    //  验证功能
    // ================================================================

    /**
     * 验证绑定表的完整性
     * @return 验证结果（包含错误和警告）
     */
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();

        // 1. 检查未使用的类型声明
        Set<String> usedTypes = new HashSet<>();
        for (ExternFunction func : globalFunctions.values()) {
            collectUsedTypes(func.returnType, usedTypes);
            for (ExternParam param : func.params) {
                collectUsedTypes(param.type, usedTypes);
            }
        }
        for (ExternCallback cb : globalCallbacks) {
            collectUsedTypes(cb.returnType, usedTypes);
            for (ExternParam param : cb.params) {
                collectUsedTypes(param.type, usedTypes);
            }
        }
        for (ExternType type : globalTypes.values()) {
            if (!usedTypes.contains(type.clawTypeName)) {
                result.warnings.add("Type '" + type.clawTypeName + "' is declared but not used");
            }
        }

        // 2. 检查空链接库
        for (LinkDirective link : globalLinks) {
            if (link.libraryName == null || link.libraryName.trim().isEmpty()) {
                result.errors.add("Link directive has empty library name");
            }
        }

        // 3. 检查重复声明
        checkDuplicates(globalFunctions.keySet(), "function", result);
        checkDuplicates(globalTypes.keySet(), "type", result);
        checkDuplicates(globalConstants.keySet(), "constant", result);

        // 4. 检查函数参数类型
        for (ExternFunction func : globalFunctions.values()) {
            for (ExternParam param : func.params) {
                if (!isValidTypeReference(param.type)) {
                    result.errors.add("Function '" + func.name + "' has invalid parameter type: " + param.type);
                }
            }
            if (!isValidTypeReference(func.returnType)) {
                result.errors.add("Function '" + func.name + "' has invalid return type: " + func.returnType);
            }
        }

        return result;
    }

    private void collectUsedTypes(String type, Set<String> used) {
        if (type == null) return;
        // 提取基础类型
        int idx = type.indexOf('<');
        String baseType = idx > 0 ? type.substring(0, idx) : type;
        used.add(baseType);
    }

    private boolean isValidTypeReference(String type) {
        if (type == null) return true;
        // 提取基础类型名
        final String baseType;
        int idx = type.indexOf('<');
        if (idx > 0) {
            baseType = type.substring(0, idx);
        } else {
            baseType = type;
        }
        // 检查是否是已知类型
        if (globalTypes.containsKey(baseType)) return true;
        for (ExternStruct s : globalStructs) {
            if (s.name.equals(baseType)) return true;
        }
        for (ExternEnum e : globalEnums) {
            if (e.name.equals(baseType)) return true;
        }
        return isPrimitiveType(baseType);
    }

    private boolean isPrimitiveType(String type) {
        // 原始类型
        if ("Void".equals(type) || "Int".equals(type) || "Float".equals(type)
            || "String".equals(type) || "Bool".equals(type) || "Pointer".equals(type)
            || "OpaquePointer".equals(type) || "CString".equals(type)
            || "FuncPointer".equals(type) || "SizeT".equals(type)
            || "Any".equals(type)) {
            return true;
        }
        // 泛型容器类型
        return "Ref".equals(type) || "CArray".equals(type) || "Optional".equals(type)
            || "Result".equals(type) || "Map".equals(type) || "List".equals(type)
            || "Set".equals(type) || "Func".equals(type);
    }

    private void checkDuplicates(Set<String> names, String type, ValidationResult result) {
        Set<String> seen = new HashSet<>();
        for (String name : names) {
            if (seen.contains(name)) {
                result.errors.add("Duplicate " + type + " declaration: " + name);
            }
            seen.add(name);
        }
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        public final List<String> errors = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();

        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}

/**
 * 库版本约束
 *
 * 用于检查已安装库的版本是否符合要求
 *
 * Claw 语法： extern "C" { link "sqlite3" @version(">=3.35.0") }
 */

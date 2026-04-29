package com.q3lives.compiler.processors.semantic;

import com.q3lives.compiler.generators.ffi.FFIBindingTable;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.ExternBlock;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.ExternCallback;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.ExternConstant;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.ExternEnum;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.ExternFunction;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.ExternMacro;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.ExternParam;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.ExternStruct;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.ExternType;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.LinkDirective;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.MacroKind;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.StructField;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.EnumMember;
import com.q3lives.compiler.generators.ffi.platform.PlatformConstraint;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extern 声明语义处理器（第2层）
 *
 * 职责：
 *   1. 识别 extern "C" { ... } 块的边界
 *   2. 解析块内的各种声明（link、include、type、function、const）
 *   3. 将解析结果填充到 FFIBindingTable
 *   4. 进行语义验证（类型合法性、重复声明检查等）
 *
 * 在编译流水线中的位置：
 *   Scanner → Pairer → Hierarchy → [Preprocessor → Tokenizer] → **ExternProcessor** → ExternBlock
 *
 * 输入：Token 流（或预处理后的源代码行）
 * 输出：填充完毕的 FFIBindingTable
 */
public class ExternProcessor {

    // ================================================================
    //  正则模式：匹配 extern 块内各种声明
    // ================================================================

    /** extern "C" { 起始行，支持可选的平台约束 */
    private static final Pattern EXTERN_BLOCK_START =
        Pattern.compile("^\\s*extern\\s+\"C\"\\s*(?:@platform\\(\"(.+?)\"\\))?\\s*\\{\\s*$");

    /** link "library_name" */
    private static final Pattern LINK_PATTERN =
        Pattern.compile("^\\s*link\\s+\"([^\"]+)\"\\s*$");

    /** include "header.h" */
    private static final Pattern INCLUDE_PATTERN =
        Pattern.compile("^\\s*include\\s+\"([^\"]+)\"\\s*$");

    /** type TypeName = MappedType */
    private static final Pattern TYPE_PATTERN =
        Pattern.compile("^\\s*type\\s+(\\w+)\\s*=\\s*(\\S+)\\s*$");

    /**
     * const CONST_NAME: Type = value
     * 支持数值、字符串、十六进制
     */
    private static final Pattern CONST_PATTERN =
        Pattern.compile("^\\s*const\\s+(\\w+)\\s*:\\s*(\\w+)\\s*=\\s*(.+)\\s*$");

    /**
     * function func_name(param1: Type1, param2: Type2, ...) -> ReturnType
     * 支持变参 (...)
     */
    private static final Pattern FUNCTION_PATTERN =
    Pattern.compile("^\\s*function\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*->\\s*(\\S+)\\s*$");

    //private static final Pattern FUNCTION_PATTERN = 

       // Pattern.compile("^\\s*function\\s+(\\w+)\\s*\$([^)]*)\$\\s*->\\s*(\\S+)\\s*$");

    /** 单个参数: name: Type */
    private static final Pattern PARAM_PATTERN =
        Pattern.compile("^\\s*(\\w+)\\s*:\\s*(\\S+)\\s*$");

    /** 块结束 } */
    private static final Pattern BLOCK_END =
        Pattern.compile("^\\s*\\}\\s*$");

    /** struct Name { */
    private static final Pattern STRUCT_START =
        Pattern.compile("^\\s*struct\\s+(\\w+)\\s*\\{\\s*$");

    /** struct 字段: name: Type */
    private static final Pattern STRUCT_FIELD =
        Pattern.compile("^\\s*(\\w+)\\s*:\\s*(\\S+)\\s*$");

    /** enum Name { */
    private static final Pattern ENUM_START =
        Pattern.compile("^\\s*enum\\s+(\\w+)\\s*\\{\\s*$");

    /** enum 成员: NAME = value 或 NAME */
    private static final Pattern ENUM_MEMBER =
        Pattern.compile("^\\s*(\\w+)\\s*(?:=\\s*(.+?))?\\s*,?\\s*$");

    /** callback Name(params) -> ReturnType */
    private static final Pattern CALLBACK_PATTERN =
        Pattern.compile("^\\s*callback\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*->\\s*(\\S+)\\s*$");

    /** macro NAME: Type = value (常量宏) */
    private static final Pattern MACRO_CONST_PATTERN =
        Pattern.compile("^\\s*macro\\s+(\\w+)\\s*:\\s*(\\w+)\\s*=\\s*(.+)\\s*$");

    /** macro NAME(params) -> ReturnType = expression (函数宏) */
    private static final Pattern MACRO_FUNC_PATTERN =
        Pattern.compile("^\\s*macro\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*->\\s*(\\w+)\\s*(?:=\\s*(.+))?\\s*$");

    // ================================================================
    //  允许的 FFI 类型集合（语义验证用）
    // ================================================================

    private static final Set<String> VALID_FFI_TYPES = new HashSet<>(Arrays.asList(
        // 基本类型
        "Void", "Int", "Float", "String", "Bool", "Any",
        // FFI 专用类型
        "Pointer", "OpaquePointer", "CString", "FuncPointer", "SizeT",
        // 固定宽度整数
        "Int8", "Int16", "Int32", "Int64",
        "UInt8", "UInt16", "UInt32", "UInt64"
    ));

    // ================================================================
    //  处理器状态
    // ================================================================

    private final FFIBindingTable bindingTable;
    private final List<ProcessingError> errors;
    private final List<ProcessingWarning> warnings;

    /** 当前文件名（用于错误报告） */
    private String currentFile = "<unknown>";

    public ExternProcessor(FFIBindingTable bindingTable) {
        this.bindingTable = bindingTable;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    // ================================================================
    //  错误/警告数据结构
    // ================================================================

    public static class ProcessingError {
        public final String file;
        public final int line;
        public final String message;

        public ProcessingError(String file, int line, String message) {
            this.file = file;
            this.line = line;
            this.message = message;
        }

        @Override
        public String toString() {
            return file + ":" + line + ": error: " + message;
        }
    }

    public static class ProcessingWarning {
        public final String file;
        public final int line;
        public final String message;

        public ProcessingWarning(String file, int line, String message) {
            this.file = file;
            this.line = line;
            this.message = message;
        }

        @Override
        public String toString() {
            return file + ":" + line + ": warning: " + message;
        }
    }

    // ================================================================
    //  主处理入口
    // ================================================================

    /**
     * 处理整个源文件的所有行
     *
     * @param lines     源代码行列表
     * @param fileName  文件名（用于错误报告）
     * @return 处理是否成功（无错误）
     */
    public boolean process(List<String> lines, String fileName) {
        this.currentFile = fileName;
        errors.clear();
        warnings.clear();

        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);

            Matcher externMatch = EXTERN_BLOCK_START.matcher(line);
            if (externMatch.matches()) {
                // 找到 extern "C" { 块的开始
                i = processExternBlock(lines, i);
            } else {
                i++;
            }
        }

        return errors.isEmpty();
    }

    /**
     * 处理单个 extern "C" { ... } 块
     *
     * @param lines       所有源代码行
     * @param startLine   extern "C" { 所在的行号（0-based）
     * @return 块结束后的下一行行号
     */
    private int processExternBlock(List<String> lines, int startLine) {
        ExternBlock block = bindingTable.newExternBlock();
        block.startLine = startLine + 1;  // 转为 1-based 供错误报告

        // 解析 extern 行上的平台约束
        String externLine = lines.get(startLine);
        Matcher externMatcher = EXTERN_BLOCK_START.matcher(externLine);
        if (externMatcher.matches() && externMatcher.group(1) != null) {
            String platformStr = externMatcher.group(1);
            block.platform = parsePlatformConstraint(platformStr);
        }

        // 用于检测重复声明
        Set<String> declaredTypes = new HashSet<>();
        Set<String> declaredFunctions = new HashSet<>();
        Set<String> declaredConstants = new HashSet<>();
        Set<String> declaredStructs = new HashSet<>();
        Set<String> declaredEnums = new HashSet<>();
        Set<String> declaredCallbacks = new HashSet<>();
        Set<String> declaredMacros = new HashSet<>();

        int i = startLine + 1;  // 跳过 extern "C" { 行
        boolean foundEnd = false;

        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();
            int lineNum = i + 1;  // 1-based 行号

            // 跳过空行和注释
            if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                i++;
                continue;
            }

            // 块结束
            if (BLOCK_END.matcher(trimmed).matches()) {
                block.endLine = lineNum;
                foundEnd = true;
                i++;
                break;
            }

            // 检测多行 struct 块
            Matcher structStart = STRUCT_START.matcher(trimmed);
            if (structStart.matches()) {
                String structName = structStart.group(1);
                if (declaredStructs.contains(structName)) {
                    addWarning(lineNum, "Duplicate extern struct declaration: " + structName);
                }
                declaredStructs.add(structName);
                i = parseStructBlock(lines, i, block, structName);
                continue;
            }

            // 检测多行 enum 块
            Matcher enumStart = ENUM_START.matcher(trimmed);
            if (enumStart.matches()) {
                String enumName = enumStart.group(1);
                if (declaredEnums.contains(enumName)) {
                    addWarning(lineNum, "Duplicate extern enum declaration: " + enumName);
                }
                declaredEnums.add(enumName);
                i = parseEnumBlock(lines, i, block, enumName);
                continue;
            }

            // 解析单行声明
            if (!parseLine(trimmed, lineNum, block, declaredTypes, declaredFunctions, declaredConstants,
                           declaredCallbacks, declaredMacros)) {
                addError(lineNum, "Unrecognized extern declaration: " + trimmed);
            }

            i++;
        }

        if (!foundEnd) {
            addError(startLine + 1, "Unterminated extern \"C\" block (missing closing '}')");
        }

        // 块级语义验证
        validateExternBlock(block);

        // 将解析结果合并到全局绑定表
        bindingTable.indexBlock(block);

        return i;
    }

    // ================================================================
    //  行解析
    // ================================================================

    /**
     * 解析 extern 块内的一行声明
     *
     * @return 是否成功匹配了某种声明模式
     */
    private boolean parseLine(String line, int lineNum, ExternBlock block,
                               Set<String> declaredTypes,
                               Set<String> declaredFunctions,
                               Set<String> declaredConstants,
                               Set<String> declaredCallbacks,
                               Set<String> declaredMacros) {

        Matcher m;

        // --- link "library" ---
        m = LINK_PATTERN.matcher(line);
        if (m.matches()) {
            String libName = m.group(1);
            block.links.add(new LinkDirective(libName, null));
            return true;
        }

        // --- include "header.h" ---
        m = INCLUDE_PATTERN.matcher(line);
        if (m.matches()) {
            String header = m.group(1);
            block.includes.add(header);
            return true;
        }

        // --- type TypeName = MappedType ---
        m = TYPE_PATTERN.matcher(line);
        if (m.matches()) {
            String typeName = m.group(1);
            String mappedType = m.group(2);

            // 重复检查
            if (declaredTypes.contains(typeName)) {
                addWarning(lineNum, "Duplicate extern type declaration: " + typeName);
            }
            declaredTypes.add(typeName);

            // 映射类型验证
            if (!isValidFFIType(mappedType)) {
                addError(lineNum, "Unknown FFI mapping type: " + mappedType
                    + ". Valid types: " + VALID_FFI_TYPES);
            }

            block.types.add(new ExternType(typeName, mappedType));
            return true;
        }

        // --- const NAME: Type = value ---
        m = CONST_PATTERN.matcher(line);
        if (m.matches()) {
            String constName = m.group(1);
            String constType = m.group(2);
            String constValue = m.group(3).trim();

            // 重复检查
            if (declaredConstants.contains(constName)) {
                addWarning(lineNum, "Duplicate extern constant: " + constName);
            }
            declaredConstants.add(constName);

            // 类型验证
            if (!isValidFFIType(constType)) {
                addError(lineNum, "Invalid type for constant " + constName + ": " + constType);
            }

            // 值验证
            if (!isValidConstantValue(constType, constValue)) {
                addWarning(lineNum, "Constant value may not match type " + constType + ": " + constValue);
            }

            block.constants.add(new ExternConstant(constName, constType, constValue));
            return true;
        }

        // --- function name(params) -> ReturnType ---
        m = FUNCTION_PATTERN.matcher(line);
        if (m.matches()) {
            String funcName = m.group(1);
            String paramsStr = m.group(2).trim();
            String returnType = m.group(3);

            // 重复检查
            if (declaredFunctions.contains(funcName)) {
                addWarning(lineNum, "Duplicate extern function declaration: " + funcName);
            }
            declaredFunctions.add(funcName);

            // 解析参数
            ParsedParams parsed = parseParams(paramsStr, lineNum);

            // 返回类型验证
            if (!isValidFFIType(returnType) && !isExternDeclaredType(returnType, block)) {
                addError(lineNum, "Unknown return type for " + funcName + ": " + returnType);
            }

            block.functions.add(new ExternFunction(
                funcName, parsed.params, returnType, parsed.isVariadic
            ));
            return true;
        }

        // --- callback Name(params) -> ReturnType ---
        m = CALLBACK_PATTERN.matcher(line);
        if (m.matches()) {
            String cbName = m.group(1);
            String paramsStr = m.group(2).trim();
            String returnType = m.group(3);

            // 重复检查
            if (declaredCallbacks.contains(cbName)) {
                addWarning(lineNum, "Duplicate extern callback declaration: " + cbName);
            }
            declaredCallbacks.add(cbName);

            ParsedParams parsed = parseParams(paramsStr, lineNum);
            block.callbacks.add(new ExternCallback(cbName, parsed.params, returnType));
            return true;
        }

        // --- macro NAME: Type = value (常量宏) ---
        m = MACRO_CONST_PATTERN.matcher(line);
        if (m.matches()) {
            String macroName = m.group(1);
            String macroType = m.group(2);
            String macroValue = m.group(3).trim();

            // 重复检查
            if (declaredMacros.contains(macroName)) {
                addWarning(lineNum, "Duplicate extern macro: " + macroName);
            }
            declaredMacros.add(macroName);

            ExternMacro macro = new ExternMacro(macroName, MacroKind.CONSTANT);
            macro.type = macroType;
            macro.value = macroValue;
            block.macros.add(macro);
            return true;
        }

        // --- macro NAME(params) -> ReturnType = expression (函数宏) ---
        m = MACRO_FUNC_PATTERN.matcher(line);
        if (m.matches()) {
            String macroName = m.group(1);
            String paramsStr = m.group(2).trim();
            String returnType = m.group(3);
            String expansion = m.group(4); // 可能为 null

            // 重复检查
            if (declaredMacros.contains(macroName)) {
                addWarning(lineNum, "Duplicate extern macro: " + macroName);
            }
            declaredMacros.add(macroName);

            ExternMacro macro = new ExternMacro(macroName, MacroKind.FUNCTION);
            macro.returnType = returnType;
            macro.expansion = expansion != null ? expansion.trim() : null;
            macro.params = parseParams(paramsStr, lineNum).params;
            block.macros.add(macro);
            return true;
        }

        return false;
    }

    // ================================================================
    //  多行块解析
    // ================================================================

    /**
     * 解析 struct 多行块
     *
     * 输入时 i 指向 "struct Name {" 行
     * 返回 struct 结束后的下一行索引
     */
    private int parseStructBlock(List<String> lines, int startIdx, ExternBlock block, String structName) {
        ExternStruct struct = new ExternStruct(structName);
        int i = startIdx + 1;

        while (i < lines.size()) {
            String line = lines.get(i).trim();

            // 空行或注释
            if (line.isEmpty() || line.startsWith("//")) {
                i++;
                continue;
            }

            // struct 结束
            if (BLOCK_END.matcher(line).matches()) {
                i++;
                break;
            }

            // 解析字段
            Matcher fieldMatch = STRUCT_FIELD.matcher(line);
            if (fieldMatch.matches()) {
                String fieldName = fieldMatch.group(1);
                String fieldType = fieldMatch.group(2);
                struct.fields.add(new StructField(fieldName, fieldType));
            } else {
                addError(i + 1, "Invalid struct field syntax: " + line);
            }

            i++;
        }

        block.structs.add(struct);
        return i;
    }

    /**
     * 解析 enum 多行块
     *
     * 输入时 i 指向 "enum Name {" 行
     * 返回 enum 结束后的下一行索引
     */
    private int parseEnumBlock(List<String> lines, int startIdx, ExternBlock block, String enumName) {
        ExternEnum enumType = new ExternEnum(enumName);
        int i = startIdx + 1;

        while (i < lines.size()) {
            String line = lines.get(i).trim();

            // 空行或注释
            if (line.isEmpty() || line.startsWith("//")) {
                i++;
                continue;
            }

            // enum 结束
            if (BLOCK_END.matcher(line).matches()) {
                i++;
                break;
            }

            // 解析成员
            Matcher memberMatch = ENUM_MEMBER.matcher(line);
            if (memberMatch.matches()) {
                String memberName = memberMatch.group(1);
                String memberValue = memberMatch.group(2);
                // 如果无显式值，使用 null（后续可推断）
                enumType.members.add(new EnumMember(memberName,
                    memberValue != null ? memberValue.trim() : null));
            } else {
                addError(i + 1, "Invalid enum member syntax: " + line);
            }

            i++;
        }

        block.enums.add(enumType);
        return i;
    }

    /**
     * 解析平台约束字符串
     * 格式: "windows,linux" 或 "windows"
     */
    private PlatformConstraint parsePlatformConstraint(String platformStr) {
        PlatformConstraint constraint = new PlatformConstraint();
        String[] platforms = platformStr.split(",");
        for (String p : platforms) {
            // 去除引号、空白，统一小写
            String trimmed = p.replaceAll("\"", "").trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                constraint = constraint.addPlatform(trimmed);
            }
        }
        return constraint;
    }

    // ================================================================
    //  参数解析
    // ================================================================

    private static class ParsedParams {
        List<ExternParam> params = new ArrayList<>();
        boolean isVariadic = false;
    }

    /**
     * 解析函数参数列表字符串
     *
     * 支持格式：
     *   "filename: CString, db: Ref<sqlite3>"
     *   "format: CString, ..."
     *   "" (无参数)
     */
    private ParsedParams parseParams(String paramsStr, int lineNum) {
        ParsedParams result = new ParsedParams();

        if (paramsStr.isEmpty()) {
            return result;
        }

        // 智能分割：需要处理泛型类型中的逗号（如 Ref<Map<K,V>>）
        List<String> paramTokens = splitParams(paramsStr);

        for (String token : paramTokens) {
            String trimmed = token.trim();

            if ("...".equals(trimmed)) {
                result.isVariadic = true;
                continue;
            }

            Matcher paramMatch = PARAM_PATTERN.matcher(trimmed);
            if (paramMatch.matches()) {
                String name = paramMatch.group(1);
                String type = paramMatch.group(2);

                // 验证参数类型
                String baseType = extractBaseType(type);
                // 对于 extern 块，允许使用同块内声明的自定义类型
                // 类型验证延迟到 validateExternBlock

                result.params.add(new ExternParam(name, type));
            } else {
                addError(lineNum, "Invalid parameter syntax: " + trimmed);
            }
        }

        return result;
    }

    /**
     * 智能分割参数列表
     * 处理泛型嵌套：Ref<Map<String, Int>> 中的逗号不应被分割
     */
    private List<String> splitParams(String paramsStr) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();

        for (char c : paramsStr.toCharArray()) {
            if (c == '<') {
                depth++;
                current.append(c);
            } else if (c == '>') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    /**
     * 从泛型类型中提取基础类型名
     * "Ref<sqlite3>" -> "Ref"
     * "CArray<Int>" -> "CArray"
     * "Int" -> "Int"
     */
    private String extractBaseType(String type) {
        int idx = type.indexOf('<');
        if (idx > 0) {
            return type.substring(0, idx);
        }
        return type;
    }

    /**
     * 从泛型类型中提取内部类型
     * "Ref<sqlite3>" -> "sqlite3"
     */
    private String extractInnerType(String type) {
        int start = type.indexOf('<');
        int end = type.lastIndexOf('>');
        if (start > 0 && end > start) {
            return type.substring(start + 1, end);
        }
        return null;
    }

    // ================================================================
    //  语义验证
    // ================================================================

    /**
     * 验证整个 extern 块的语义一致性
     */
    private void validateExternBlock(ExternBlock block) {
        // 1. 检查是否至少有一个 link 指令
        if (block.links.isEmpty()) {
            addWarning(block.startLine, "extern \"C\" block has no 'link' directive. "
                + "No library will be linked.");
        }

        // 2. 收集本块内声明的自定义类型名（包括 type、struct、enum）
        Set<String> blockTypeNames = new HashSet<>();
        for (ExternType type : block.types) {
            blockTypeNames.add(type.clawTypeName);
        }
        for (ExternStruct struct : block.structs) {
            blockTypeNames.add(struct.name);
        }
        for (ExternEnum enumType : block.enums) {
            blockTypeNames.add(enumType.name);
        }

        // 3. 验证函数参数和返回类型中引用的类型是否已声明
        for (ExternFunction func : block.functions) {
            validateTypeReference(func.returnType, blockTypeNames, block.startLine,
                "return type of " + func.name);
            for (ExternParam param : func.params) {
                validateTypeReference(param.type, blockTypeNames, block.startLine,
                    "parameter '" + param.name + "' of " + func.name);
            }
        }

        // 4. 验证回调的参数和返回类型
        for (ExternCallback cb : block.callbacks) {
            validateTypeReference(cb.returnType, blockTypeNames, block.startLine,
                "return type of callback " + cb.name);
            for (ExternParam param : cb.params) {
                validateTypeReference(param.type, blockTypeNames, block.startLine,
                    "parameter '" + param.name + "' of callback " + cb.name);
            }
        }

        // 5. 检查没有函数使用的 type 声明（struct/enum 不需要此检查）
        Set<String> usedTypes = new HashSet<>();
        for (ExternFunction func : block.functions) {
            collectUsedTypes(func.returnType, usedTypes);
            for (ExternParam param : func.params) {
                collectUsedTypes(param.type, usedTypes);
            }
        }
        for (ExternCallback cb : block.callbacks) {
            collectUsedTypes(cb.returnType, usedTypes);
            for (ExternParam param : cb.params) {
                collectUsedTypes(param.type, usedTypes);
            }
        }
        for (ExternType type : block.types) {
            if (!usedTypes.contains(type.clawTypeName)) {
                addWarning(block.startLine,
                    "Extern type '" + type.clawTypeName + "' is declared but never used in any function");
            }
        }
    }

    /**
     * 验证类型引用是否合法
     */
    private void validateTypeReference(String type, Set<String> blockTypes, int lineNum, String context) {
        String baseType = extractBaseType(type);

        // 基础 FFI 类型
        if (VALID_FFI_TYPES.contains(baseType)) {
            // 如果是泛型容器，还需验证内部类型
            String innerType = extractInnerType(type);
            if (innerType != null) {
                validateTypeReference(innerType, blockTypes, lineNum, context + " (inner type)");
            }
            return;
        }

        // 泛型容器类型（Ref, CArray）
        if ("Ref".equals(baseType) || "CArray".equals(baseType)) {
            String innerType = extractInnerType(type);
            if (innerType == null) {
                addError(lineNum, baseType + " requires a type parameter, in " + context);
            } else {
                validateTypeReference(innerType, blockTypes, lineNum, context + " (inner type)");
            }
            return;
        }

        // 本块内声明的自定义类型（检查完整类型名和基础类型名）
        if (blockTypes.contains(type) || blockTypes.contains(baseType)) {
            return;
        }

        // 全局绑定表中已有的类型
        if (bindingTable.findType(type) != null) {
            return;
        }

        // 未知类型
        addError(lineNum, "Unknown type '" + type + "' in " + context
            + ". Did you forget to declare: type " + type + " = OpaquePointer ?");
    }

    /**
     * 从类型表达式中收集所有引用的自定义类型名
     */
    private void collectUsedTypes(String type, Set<String> usedTypes) {
        String baseType = extractBaseType(type);

        if (!VALID_FFI_TYPES.contains(baseType)
            && !"Ref".equals(baseType) && !"CArray".equals(baseType)) {
            usedTypes.add(type);
        }

        String innerType = extractInnerType(type);
        if (innerType != null) {
            collectUsedTypes(innerType, usedTypes);
        }
    }

    // ================================================================
    //  类型验证辅助
    // ================================================================

    /**
     * 检查是否为合法的 FFI 类型（包括泛型）
     */
    private boolean isValidFFIType(String type) {
        String baseType = extractBaseType(type);
        return VALID_FFI_TYPES.contains(baseType)
            || "Ref".equals(baseType)
            || "CArray".equals(baseType);
    }

    /**
     * 检查类型是否在当前 extern 块中已声明（包括 types、structs、enums）
     */
    private boolean isExternDeclaredType(String typeName, ExternBlock block) {
        // 检查 type 声明
        for (ExternType type : block.types) {
            if (type.clawTypeName.equals(typeName)) {
                return true;
            }
        }
        // 检查 struct 声明
        for (ExternStruct struct : block.structs) {
            if (struct.name.equals(typeName)) {
                return true;
            }
        }
        // 检查 enum 声明
        for (ExternEnum enumType : block.enums) {
            if (enumType.name.equals(typeName)) {
                return true;
            }
        }
        // 也检查全局绑定表
        return bindingTable.findType(typeName) != null;
    }

    /**
     * 验证常量值是否与声明类型匹配
     */
    private boolean isValidConstantValue(String type, String value) {
        switch (type) {
            case "Int":
            case "Int8": case "Int16": case "Int32": case "Int64":
            case "UInt8": case "UInt16": case "UInt32": case "UInt64":
                return value.matches("-?\\d+") || value.matches("0x[0-9a-fA-F]+");
            case "Float":
                return value.matches("-?\\d+\\.?\\d*([eE][+-]?\\d+)?");
            case "String":
            case "CString":
                return value.startsWith("\"") && value.endsWith("\"");
            case "Bool":
                return "true".equals(value) || "false".equals(value);
            default:
                return true;  // 未知类型不做值验证
        }
    }

    // ================================================================
    //  错误/警告管理
    // ================================================================

    private void addError(int lineNum, String message) {
        errors.add(new ProcessingError(currentFile, lineNum, message));
    }

    private void addWarning(int lineNum, String message) {
        warnings.add(new ProcessingWarning(currentFile, lineNum, message));
    }

    public List<ProcessingError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<ProcessingWarning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 打印所有错误和警告到标准错误
     */
    public void reportDiagnostics() {
        for (ProcessingWarning warning : warnings) {
            System.err.println(warning);
        }
        for (ProcessingError error : errors) {
            System.err.println(error);
        }
        if (!errors.isEmpty()) {
            System.err.println(errors.size() + " error(s) in extern declarations.");
        }
    }

    // ================================================================
    //  查询方法（供其他处理器使用）
    // ================================================================

    /**
     * 检查一个符号名是否是 extern 声明的
     * 其他处理器在解析函数调用时使用此方法
     */
    public boolean isExternSymbol(String name) {
        return bindingTable.isExternSymbol(name);
    }

    /**
     * 获取 extern 函数的签名（供类型检查器使用）
     */
    public ExternFunction getExternFunction(String name) {
        return bindingTable.findFunction(name);
    }

    /**
     * 获取填充后的绑定表
     */
    public FFIBindingTable getBindingTable() {
        return bindingTable;
    }
}

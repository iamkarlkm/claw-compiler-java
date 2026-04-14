package claw.compiler.processors.semantic;

import claw.compiler.generators.ffi.FFIBindingTable;
import claw.compiler.generators.ffi.FFIBindingTable.*;

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

    /** extern "C" { 起始行 */
    private static final Pattern EXTERN_BLOCK_START =
        Pattern.compile("^\\s*extern\\s+\"C\"\\s*\\{\\s*$");

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

        // 用于检测重复声明
        Set<String> declaredTypes = new HashSet<>();
        Set<String> declaredFunctions = new HashSet<>();
        Set<String> declaredConstants = new HashSet<>();

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

            // 解析各种声明
            if (!parseLine(trimmed, lineNum, block, declaredTypes, declaredFunctions, declaredConstants)) {
                addError(lineNum, "Unrecognized extern declaration: " + trimmed);
            }

            i++;
        }

        if (!foundEnd) {
            addError(startLine + 1, "Unterminated extern \"C\" block (missing closing '}')");
        }

        // 块级语义验证
        validateExternBlock(block);

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
                               Set<String> declaredConstants) {

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

            // 同时为对应的 link 补充 headerFile（如果已声明）
            for (LinkDirective link : block.links) {
                if (link.headerFile == null && header.contains(link.libraryName)) {
                    // 反射修改不好，这里用新对象替换
                    // 简化处理：headerFile 在 LinkDirective 中改为可变字段或重新设计
                }
            }
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

        return false;
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

        // 2. 收集本块内声明的自定义类型名
        Set<String> blockTypeNames = new HashSet<>();
        for (ExternType type : block.types) {
            blockTypeNames.add(type.clawTypeName);
        }

        // 3. 验证函数参数和返回类型中引用的类型是否已声明
        for (ExternFunction func : block.functions) {
            // 验证返回类型
            validateTypeReference(func.returnType, blockTypeNames, block.startLine,
                "return type of " + func.name);

            // 验证每个参数类型
            for (ExternParam param : func.params) {
                validateTypeReference(param.type, blockTypeNames, block.startLine,
                    "parameter '" + param.name + "' of " + func.name);
            }
        }

        // 4. 检查没有函数使用的类型声明（可能是拼写错误）
        Set<String> usedTypes = new HashSet<>();
        for (ExternFunction func : block.functions) {
            collectUsedTypes(func.returnType, usedTypes);
            for (ExternParam param : func.params) {
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

        // 本块内声明的自定义类型
        if (blockTypes.contains(type)) {
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
     * 检查类型是否在当前 extern 块中已声明
     */
    private boolean isExternDeclaredType(String typeName, ExternBlock block) {
        for (ExternType type : block.types) {
            if (type.clawTypeName.equals(typeName)) {
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

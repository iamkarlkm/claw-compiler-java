package claw.compiler.processors.blocks;

import claw.compiler.generators.ffi.FFIBindingTable;
import claw.compiler.generators.ffi.FFIBindingTable.*;
import claw.compiler.processors.semantic.ExternProcessor;

import java.util.*;

/**
 * Extern 块处理器（第3层）
 *
 * 职责：
 *   1. 与层级构建器（HierarchyBuilder）协作，识别 extern 块的代码块边界
 *   2. 将 extern 块标记为特殊的代码块类型（第19种：extern_block）
 *   3. 建立 extern 声明与使用点之间的归属关系
 *   4. 处理 extern 声明的作用域规则
 *   5. 为代码生成阶段准备结构信息
 *
 * 在编译流水线中的位置：
 *   第2层 ExternProcessor（语义解析）→ **第3层 ExternBlockProcessor**（结构处理）→ 第4层代码生成
 *
 * 与 ExternProcessor 的关系：
 *   ExternProcessor 负责解析语义（"这是什么函数、什么类型"）
 *   ExternBlockProcessor 负责处理结构（"这个块在哪里、影响哪些代码"）
 */
public class ExternBlockProcessor {

    // ================================================================
    //  代码块类型常量
    // ================================================================

    /**
     * extern 块类型标识
     * 扩展原有的 18 种代码块类型，新增第 19 种
     */
    public static final String BLOCK_TYPE_EXTERN = "extern_block";

    // ================================================================
    //  Extern 块的结构信息
    // ================================================================

    /**
     * Extern 代码块结构
     *
     * 封装了一个 extern "C" { ... } 块的完整结构信息
     */
    public static class ExternCodeBlock {
        /** 块在源文件中的起始行（1-based） */
        public final int startLine;

        /** 块在源文件中的结束行（1-based） */
        public final int endLine;

        /** 块的层级深度（通常为 0，即顶层） */
        public final int level;

        /** 对应的 FFI 绑定数据（由 ExternProcessor 填充） */
        public final ExternBlock ffiBlock;

        /** 此 extern 块中声明的所有符号名集合 */
        public final Set<String> declaredSymbols;

        /** 使用了此 extern 块中符号的代码位置 */
        public final List<SymbolUsage> usages;

        public ExternCodeBlock(int startLine, int endLine, int level, ExternBlock ffiBlock) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.level = level;
            this.ffiBlock = ffiBlock;
            this.declaredSymbols = new HashSet<>();
            this.usages = new ArrayList<>();

            // 收集所有声明的符号名
            for (ExternType type : ffiBlock.types) {
                declaredSymbols.add(type.clawTypeName);
            }
            for (ExternFunction func : ffiBlock.functions) {
                declaredSymbols.add(func.name);
            }
            for (ExternConstant constant : ffiBlock.constants) {
                declaredSymbols.add(constant.name);
            }
        }
    }

    /**
     * 符号使用记录
     *
     * 记录 extern 声明的符号在 Claw 代码中被引用的位置
     */
    public static class SymbolUsage {
        public final String symbolName;       // 符号名
        public final int line;                // 使用位置（行号）
        public final String usageContext;     // 使用上下文（如 "function_call", "type_reference"）
        public final String containingFunc;   // 所在的 Claw 函数名

        public SymbolUsage(String symbolName, int line, String usageContext, String containingFunc) {
            this.symbolName = symbolName;
            this.line = line;
            this.usageContext = usageContext;
            this.containingFunc = containingFunc;
        }

        @Override
        public String toString() {
            return symbolName + " at line " + line + " (" + usageContext + " in " + containingFunc + ")";
        }
    }

    // ================================================================
    //  处理器状态
    // ================================================================

    private final FFIBindingTable bindingTable;
    private final List<ExternCodeBlock> externBlocks;

    /**
     * 符号名 → 声明它的 ExternCodeBlock 的映射
     * 用于快速查找某个符号属于哪个 extern 块
     */
    private final Map<String, ExternCodeBlock> symbolToBlock;

    /**
     * Claw 函数名 → 该函数中使用的 extern 符号列表
     * 用于确定哪些函数需要 FFI 绑定
     */
    private final Map<String, List<String>> functionExternDependencies;

    public ExternBlockProcessor(FFIBindingTable bindingTable) {
        this.bindingTable = bindingTable;
        this.externBlocks = new ArrayList<>();
        this.symbolToBlock = new HashMap<>();
        this.functionExternDependencies = new LinkedHashMap<>();
    }

    // ================================================================
    //  阶段1：块识别与注册
    // ================================================================

    /**
     * 从层级构建器的代码块列表中识别并注册 extern 块
     *
     * 这个方法由编译流水线在层级构建完成后调用
     *
     * @param lines       源代码行
     * @param ffiBlocks   ExternProcessor 解析出的 FFI 块列表
     */
    public void registerExternBlocks(List<String> lines, List<ExternBlock> ffiBlocks) {
        for (ExternBlock ffiBlock : ffiBlocks) {
            // 确定层级深度（extern 块通常在文件顶层，level = 0）
            int level = determineBlockLevel(lines, ffiBlock.startLine);

            ExternCodeBlock codeBlock = new ExternCodeBlock(
                ffiBlock.startLine, ffiBlock.endLine, level, ffiBlock
            );

            externBlocks.add(codeBlock);

            // 建立符号 → 块的反向映射
            for (String symbol : codeBlock.declaredSymbols) {
                if (symbolToBlock.containsKey(symbol)) {
                    System.err.println("[ExternBlock Warning] Symbol '" + symbol
                        + "' declared in multiple extern blocks");
                }
                symbolToBlock.put(symbol, codeBlock);
            }
        }
    }

    /**
     * 通过获取 FFIBindingTable 中的数据来注册（替代方法）
     *
     * 直接使用 FFIBindingTable 内部数据，不需要外部传入 ffiBlocks
     */
    public void registerFromBindingTable(List<String> lines) {
        // FFIBindingTable 内部存储了所有 ExternBlock
        // 但当前设计中 externBlocks 是 private 的
        // 这里提供一种基于公开 API 的替代方案
        //
        // 遍历所有已知的外部符号，按库分组创建逻辑块
        if (!bindingTable.hasExternDeclarations()) {
            return;
        }

        // 创建一个合并的逻辑块
        ExternBlock mergedBlock = new ExternBlock();
        mergedBlock.startLine = 0;
        mergedBlock.endLine = 0;

        // 从绑定表中提取所有数据
        for (LinkDirective link : bindingTable.getAllLinks()) {
            mergedBlock.links.add(link);
        }
        for (String inc : bindingTable.getAllIncludes()) {
            mergedBlock.includes.add(inc);
        }
        for (ExternType type : bindingTable.getAllTypes().values()) {
            mergedBlock.types.add(type);
        }
        for (ExternFunction func : bindingTable.getAllFunctions().values()) {
            mergedBlock.functions.add(func);
        }
        for (ExternConstant constant : bindingTable.getAllConstants().values()) {
            mergedBlock.constants.add(constant);
        }

        ExternCodeBlock codeBlock = new ExternCodeBlock(0, 0, 0, mergedBlock);
        externBlocks.add(codeBlock);

        for (String symbol : codeBlock.declaredSymbols) {
            symbolToBlock.put(symbol, codeBlock);
        }
    }

    // ================================================================
    //  阶段2：符号使用分析
    // ================================================================

    /**
     * 扫描非 extern 的代码，查找对 extern 符号的引用
     *
     * 这个方法在第2-3层处理器完成后调用，
     * 遍历所有非 extern 代码块，检查函数调用和类型引用
     *
     * @param lines             源代码行
     * @param functionBounds    所有 Claw 函数的行范围 {funcName -> [startLine, endLine]}
     */
    public void analyzeSymbolUsages(List<String> lines, Map<String, int[]> functionBounds) {
        if (externBlocks.isEmpty()) return;

        Set<String> allExternSymbols = symbolToBlock.keySet();

        for (Map.Entry<String, int[]> entry : functionBounds.entrySet()) {
            String funcName = entry.getKey();
            int startLine = entry.getValue()[0];
            int endLine = entry.getValue()[1];

            List<String> usedSymbols = new ArrayList<>();

            for (int i = startLine - 1; i < endLine && i < lines.size(); i++) {
                String line = lines.get(i);

                for (String symbol : allExternSymbols) {
                    if (containsSymbolReference(line, symbol)) {
                        int lineNum = i + 1;
                        String context = determineUsageContext(line, symbol);

                        // 记录使用
                        ExternCodeBlock block = symbolToBlock.get(symbol);
                        if (block != null) {
                            block.usages.add(new SymbolUsage(symbol, lineNum, context, funcName));
                        }

                        if (!usedSymbols.contains(symbol)) {
                            usedSymbols.add(symbol);
                        }
                    }
                }
            }

            if (!usedSymbols.isEmpty()) {
                functionExternDependencies.put(funcName, usedSymbols);
            }
        }
    }

    /**
     * 检查一行代码是否包含对特定符号的引用
     * 简单的词法匹配（完整单词匹配，避免子串误匹配）
     */
    private boolean containsSymbolReference(String line, String symbol) {
        int idx = line.indexOf(symbol);
        while (idx >= 0) {
            // 检查前后是否为单词边界
            boolean leftBoundary = (idx == 0)
                || !Character.isLetterOrDigit(line.charAt(idx - 1)) && line.charAt(idx - 1) != '_';
            boolean rightBoundary = (idx + symbol.length() >= line.length())
                || !Character.isLetterOrDigit(line.charAt(idx + symbol.length()))
                   && line.charAt(idx + symbol.length()) != '_';

            if (leftBoundary && rightBoundary) {
                return true;
            }
            idx = line.indexOf(symbol, idx + 1);
        }
        return false;
    }

    /**
     * 判断符号在当前行中的使用上下文
     */
    private String determineUsageContext(String line, String symbol) {
        String trimmed = line.trim();

        // 函数调用：symbol(...)
        if (trimmed.contains(symbol + "(")) {
            return "function_call";
        }

        // 类型引用：: symbol 或 -> symbol
        if (trimmed.contains(": " + symbol) || trimmed.contains("-> " + symbol)) {
            return "type_reference";
        }

        // 常量引用
        if (trimmed.contains("= " + symbol) || trimmed.contains("== " + symbol)) {
            return "constant_reference";
        }

        return "reference";
    }

    // ================================================================
    //  阶段3：作用域规则
    // ================================================================

    /**
     * 验证 extern 块的作用域规则
     *
     * Claw 的 extern 作用域规则：
     *   1. extern 块必须在文件顶层（level == 0）
     *   2. extern 声明的作用域为整个文件（从声明位置之后）
     *   3. extern 符号不能在 extern 块内部被调用（仅声明）
     *   4. 不同文件的 extern 块互相独立（通过 import 共享）
     */
    public List<String> validateScopeRules() {
        List<String> violations = new ArrayList<>();

        for (ExternCodeBlock block : externBlocks) {
            // 规则1：必须在顶层
            if (block.level != 0) {
                violations.add("Line " + block.startLine
                    + ": extern \"C\" block must be at top level (current level: " + block.level + ")");
            }

            // 规则3：检查是否有符号在 extern 块范围内被调用
            for (SymbolUsage usage : block.usages) {
                if (usage.line >= block.startLine && usage.line <= block.endLine) {
                    violations.add("Line " + usage.line
                        + ": extern symbol '" + usage.symbolName
                        + "' cannot be used inside the extern block where it is declared");
                }
            }
        }

        return violations;
    }

    // ================================================================
    //  查询接口（供代码生成器使用）
    // ================================================================

    /**
     * 获取所有 extern 代码块
     */
    public List<ExternCodeBlock> getExternBlocks() {
        return Collections.unmodifiableList(externBlocks);
    }

    /**
     * 获取特定 Claw 函数依赖的 extern 符号
     *
     * 代码生成器用这个信息决定：
     *   - C 目标：需要 #include 哪些头文件
     *   - Python 目标：需要加载哪些 ctypes 库
     *   - Java 目标：需要绑定哪些 MethodHandle
     */
    public List<String> getExternDependencies(String functionName) {
        return functionExternDependencies.getOrDefault(functionName, Collections.emptyList());
    }

    /**
     * 获取所有使用了 extern 符号的函数名
     */
    public Set<String> getFunctionsWithExternDeps() {
        return Collections.unmodifiableSet(functionExternDependencies.keySet());
    }

    /**
     * 判断某个函数是否依赖 extern 符号
     */
    public boolean hasFunctionExternDeps(String functionName) {
        return functionExternDependencies.containsKey(functionName);
    }

    /**
     * 查找声明某个符号的 extern 块
     */
    public ExternCodeBlock findBlockForSymbol(String symbolName) {
        return symbolToBlock.get(symbolName);
    }

    /**
     * 获取所有需要链接的库名（去重合并）
     */
    public List<String> getRequiredLibraries() {
        return bindingTable.getLibraryNames();
    }

    /**
     * 判断指定行范围是否属于 extern 块
     * 用于其他处理器跳过 extern 块内容
     */
    public boolean isWithinExternBlock(int lineNum) {
        for (ExternCodeBlock block : externBlocks) {
            if (lineNum >= block.startLine && lineNum <= block.endLine) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有 extern 块的行范围（用于其他处理器排除这些区域）
     */
    public List<int[]> getExternBlockRanges() {
        List<int[]> ranges = new ArrayList<>();
        for (ExternCodeBlock block : externBlocks) {
            ranges.add(new int[]{block.startLine, block.endLine});
        }
        return ranges;
    }

    // ================================================================
    //  辅助方法
    // ================================================================

    /**
     * 根据缩进或括号嵌套确定块的层级深度
     */
    private int determineBlockLevel(List<String> lines, int lineNum) {
        if (lineNum <= 0 || lineNum > lines.size()) return 0;

        String line = lines.get(lineNum - 1);

        // 计算前导空格（4空格 = 1级）
        int spaces = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') spaces++;
            else if (c == '	') spaces += 4;
            else break;
        }

        return spaces / 4;
    }

    // ================================================================
    //  调试输出
    // ================================================================

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Extern Block Processor ===\n");
        sb.append("Extern blocks: ").append(externBlocks.size()).append("\n");

        for (int i = 0; i < externBlocks.size(); i++) {
            ExternCodeBlock block = externBlocks.get(i);
            sb.append("--- Block ").append(i + 1).append(" ---\n");
            sb.append("  Lines: ").append(block.startLine).append(" - ").append(block.endLine).append("\n");
            sb.append("  Level: ").append(block.level).append("\n");
            sb.append("  Symbols: ").append(block.declaredSymbols).append("\n");
            sb.append("  Usages: ").append(block.usages.size()).append("\n");
            for (SymbolUsage usage : block.usages) {
                sb.append("    ").append(usage).append("\n");
            }
        }

        sb.append("Function dependencies:");
        for (Map.Entry<String, List<String>> entry : functionExternDependencies.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return sb.toString();
    }
}

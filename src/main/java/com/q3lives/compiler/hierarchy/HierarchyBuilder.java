// ==================== HierarchyBuilder.java ====================
package com.q3lives.compiler.hierarchy;

import com.q3lives.compiler.pairer.Pair;
import com.q3lives.compiler.pairer.PairingResult;
import com.q3lives.compiler.scanner.LineInfo;
import com.q3lives.compiler.scanner.SourceView;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 层级构建器
 * 
 * 思想2第3步：自顶向下生成无限分级代码块
 * 思想4：识别18种代码块类型
 */
@Slf4j
public class HierarchyBuilder {


    // 块类型识别的关键字模式
    private static final Pattern FUNCTION_PATTERN = 
        Pattern.compile("^\\s*(normal\\s+|exception\\s+|flow\\s+)?(public\\s+|private\\s+)?function\\s+");
    private static final Pattern TYPE_PATTERN = 
        Pattern.compile("^\\s*(public\\s+|private\\s+)?type\\s+");
    // private static final Pattern IF_PATTERN = 
    //     Pattern.compile("^\\s*if\\s*\$");
    // private static final Pattern ELSE_PATTERN = 
    //     Pattern.compile("^\\s*(\\}\\s*)?else\\s*(\\{|if\\s*\$)");
    // private static final Pattern FOR_PATTERN = 
    //     Pattern.compile("^\\s*for\\s*\$");
    // private static final Pattern WHILE_PATTERN = 
    //     Pattern.compile("^\\s*while\\s*\$");
    private static final Pattern IMPORT_PATTERN = 
        Pattern.compile("^\\s*import\\s+");
    private static final Pattern VAR_PATTERN = 
        Pattern.compile("^\\s*(var|const)\\s+");
    private static final Pattern ANNOTATION_PATTERN = 
        Pattern.compile("^\\s*@@?\\w+");
    private static final Pattern RETURN_PATTERN = 
        Pattern.compile("^\\s*return\\s+");
    // private static final Pattern CATCH_PATTERN = 
    //     Pattern.compile("^\\s*(\\}\\s*)?catch\\s*\$");
        private static final Pattern IF_PATTERN = 
    Pattern.compile("^\\s*if\\s*\\(");
private static final Pattern ELSE_PATTERN = 
    Pattern.compile("^\\s*(\\}\\s*)?else\\s*(\\{|if\\s*\\()");
private static final Pattern FOR_PATTERN = 
    Pattern.compile("^\\s*for\\s*\\(");
private static final Pattern WHILE_PATTERN = 
    Pattern.compile("^\\s*while\\s*\\(");
private static final Pattern CATCH_PATTERN = 
    Pattern.compile("^\\s*(\\}\\s*)?catch\\s*\\(");


    /**
     * 构建层级代码块
     */
    public HierarchicalBlocks build(SourceView sourceView, PairingResult pairingResult) {
        log.debug("开始构建层级结构");

        // 创建根块
        CodeBlock root = new CodeBlock(0, 1);
        root.setBlockType(BlockType.ROOT_BLOCK);
        root.setEndLine(sourceView.getTotalLines());
        root.setScopeName("__root__");

        // 获取所有花括号配对，按起始位置排序
        List<Pair> bracePairs = new ArrayList<>(pairingResult.getBracePairs());
        bracePairs.sort(Comparator.comparingInt(Pair::getOpenLine)
                .thenComparingInt(Pair::getOpenCol));

        // 构建嵌套结构
        buildNested(root, bracePairs, sourceView);

        // 处理非花括号内的语句（声明、导入、注解等）
        processStandaloneStatements(root, sourceView);

        HierarchicalBlocks result = new HierarchicalBlocks(root);
        log.info("层级构建完成: {} 个块, 最大深度 {}", 
                result.getTotalBlockCount(), result.getMaxDepth());
        log.debug("层级结构:{}", result.toTreeString());
        return result;
    }

    /**
     * 递归构建嵌套的代码块
     */
    private void buildNested(CodeBlock parent, List<Pair> bracePairs, SourceView sourceView) {
        // 找到直接属于parent的花括号配对
        List<Pair> directChildren = findDirectChildPairs(parent, bracePairs);

        for (Pair pair : directChildren) {
            int blockStartLine = findBlockStartLine(pair, sourceView);
            CodeBlock child = new CodeBlock(parent.getLevel() + 1, blockStartLine);
            child.setEndLine(pair.getCloseLine());

            // 识别块类型
            BlockType type = identifyBlockType(blockStartLine, pair, sourceView);
            child.setBlockType(type);

            // 添加行信息
            for (int line = blockStartLine; line <= pair.getCloseLine(); line++) {
                LineInfo lineInfo = sourceView.getLine(line);
                if (lineInfo != null) {
                    child.addLine(lineInfo);
                }
            }

            // 收集块内容
            child.setContent(sourceView.getSourceRange(blockStartLine, pair.getCloseLine()));

            parent.addChild(child);

            // 递归处理子块内的花括号配对
            List<Pair> innerPairs = findInnerPairs(pair, bracePairs);
            if (!innerPairs.isEmpty()) {
                buildNested(child, innerPairs, sourceView);
            }
        }
    }

    /**
     * 找到直接属于父块的花括号配对（不包括被其他配对包含的）
     */
    private List<Pair> findDirectChildPairs(CodeBlock parent, List<Pair> allPairs) {
        List<Pair> direct = new ArrayList<>();
        Set<Pair> consumed = new HashSet<>();

        for (Pair pair : allPairs) {
            if (consumed.contains(pair)) continue;
            if (pair.getOpenLine() < parent.getStartLine() || 
                pair.getCloseLine() > parent.getEndLine()) continue;

            // 检查是否被其他已选中的配对包含
            boolean nested = false;
            for (Pair selected : direct) {
                if (pair.getOpenLine() > selected.getOpenLine() && 
                    pair.getCloseLine() < selected.getCloseLine()) {
                    nested = true;
                    break;
                }
            }
            if (!nested) {
                // 检查是否包含其他已选中的配对
                direct.removeIf(selected -> 
                    selected.getOpenLine() > pair.getOpenLine() && 
                    selected.getCloseLine() < pair.getCloseLine());
                direct.add(pair);
            }
        }

        // 只保留深度匹配parent.level的
        return direct.stream()
                .filter(p -> p.getDepth() == parent.getLevel())
                .toList();
    }

    /**
     * 找到配对内部的所有配对
     */
    private List<Pair> findInnerPairs(Pair outerPair, List<Pair> allPairs) {
        return allPairs.stream()
                .filter(p -> p != outerPair)
                .filter(p -> p.getOpenLine() > outerPair.getOpenLine() ||
                            (p.getOpenLine() == outerPair.getOpenLine() && 
                             p.getOpenCol() > outerPair.getOpenCol()))
                .filter(p -> p.getCloseLine() < outerPair.getCloseLine() ||
                            (p.getCloseLine() == outerPair.getCloseLine() && 
                             p.getCloseCol() < outerPair.getCloseCol()))
                .toList();
    }

    /**
     * 找到代码块的实际起始行（花括号前面的声明行）
     */
    private int findBlockStartLine(Pair bracePair, SourceView sourceView) {
        int openLine = bracePair.getOpenLine();
        // 向上查找，找到块声明的起始行
        for (int line = openLine; line >= 1; line--) {
            LineInfo lineInfo = sourceView.getLine(line);
            if (lineInfo == null || !lineInfo.isEffective()) continue;
            String trimmed = lineInfo.getTrimmedContent();
            if (FUNCTION_PATTERN.matcher(trimmed).find() ||
                TYPE_PATTERN.matcher(trimmed).find() ||
                IF_PATTERN.matcher(trimmed).find() ||
                ELSE_PATTERN.matcher(trimmed).find() ||
                FOR_PATTERN.matcher(trimmed).find() ||
                WHILE_PATTERN.matcher(trimmed).find() ||
                CATCH_PATTERN.matcher(trimmed).find()) {
                return line;
            }
            if (line < openLine) break; // 只往上看一行
        }
        return openLine;
    }

    /**
     * 识别代码块类型（18种之一）
     */
    private BlockType identifyBlockType(int startLine, Pair bracePair, SourceView sourceView) {
        LineInfo lineInfo = sourceView.getLine(startLine);
        if (lineInfo == null) return BlockType.SCOPE_BLOCK;

        String trimmed = lineInfo.getTrimmedContent();

        if (FUNCTION_PATTERN.matcher(trimmed).find()) return BlockType.FUNCTION_BLOCK;
        if (TYPE_PATTERN.matcher(trimmed).find()) return BlockType.TYPE_DEFINITION_BLOCK;
        if (IF_PATTERN.matcher(trimmed).find() || ELSE_PATTERN.matcher(trimmed).find())
            return BlockType.CONTROL_FLOW_BLOCK;
        if (FOR_PATTERN.matcher(trimmed).find()) return BlockType.LOOP_BODY_BLOCK;
        if (WHILE_PATTERN.matcher(trimmed).find()) return BlockType.LOOP_BODY_BLOCK;
        if (CATCH_PATTERN.matcher(trimmed).find()) return BlockType.CONTROL_FLOW_BLOCK;

        return BlockType.SCOPE_BLOCK;
    }

    /**
     * 处理独立语句（不在花括号内的声明、导入、注解等）
     */
    private void processStandaloneStatements(CodeBlock root, SourceView sourceView) {
        for (LineInfo line : sourceView.getEffectiveLines()) {
            String trimmed = line.getTrimmedContent();

            // 检查该行是否已被某个子块包含
            boolean covered = root.getAllDescendants().stream()
                    .anyMatch(b -> line.getLineNumber() >= b.getStartLine() && 
                                   line.getLineNumber() <= b.getEndLine());
            if (covered) continue;

            BlockType type = null;
            if (IMPORT_PATTERN.matcher(trimmed).find()) {
                type = BlockType.IMPORT_DECLARATION_BLOCK;
            } else if (VAR_PATTERN.matcher(trimmed).find()) {
                type = BlockType.VARIABLE_DECLARATION_BLOCK;
            } else if (ANNOTATION_PATTERN.matcher(trimmed).find()) {
                type = BlockType.ANNOTATION_BLOCK;
            } else if (RETURN_PATTERN.matcher(trimmed).find()) {
                type = BlockType.RETURN_BLOCK;
            } else if (trimmed.contains("=") && !trimmed.contains("==")) {
                type = BlockType.ASSIGNMENT_BLOCK;
            }

            if (type != null) {
                CodeBlock stmtBlock = new CodeBlock(1, line.getLineNumber());
                stmtBlock.setEndLine(line.getLineNumber());
                stmtBlock.setBlockType(type);
                stmtBlock.addLine(line);
                stmtBlock.setContent(trimmed);
                root.addChild(stmtBlock);
            }
        }
    }
}


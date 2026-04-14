// ==================== HierarchicalBlocks.java ====================
package com.claw.compiler.hierarchy;

import lombok.Getter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 层级化代码块集合
 */
@Getter
public class HierarchicalBlocks {
    /** 根块 */
    private final CodeBlock root;
    /** 所有块的扁平列表（用于快速查找） */
    private final Map<String, CodeBlock> blockMap;
    /** 最大嵌套深度 */
    private final int maxDepth;

    public HierarchicalBlocks(CodeBlock root) {
        this.root = root;
        this.blockMap = new LinkedHashMap<>();
        this.maxDepth = buildBlockMap(root, 0);
    }

    private int buildBlockMap(CodeBlock block, int currentDepth) {
        blockMap.put(block.getId(), block);
        int maxD = currentDepth;
        for (CodeBlock child : block.getChildren()) {
            maxD = Math.max(maxD, buildBlockMap(child, currentDepth + 1));
        }
        return maxD;
    }

    /** 根据ID查找块 */
    public Optional<CodeBlock> findById(String id) {
        return Optional.ofNullable(blockMap.get(id));
    }

    /** 根据类型查找所有块 */
    public List<CodeBlock> findByType(BlockType type) {
        return blockMap.values().stream()
                .filter(b -> b.getBlockType() == type)
                .collect(Collectors.toList());
    }

    /** 获取指定层级的所有块 */
    public List<CodeBlock> getBlocksAtLevel(int level) {
        return blockMap.values().stream()
                .filter(b -> b.getLevel() == level)
                .collect(Collectors.toList());
    }

    /** 从最底层到最顶层排列的块列表（用于自底向上生成） */
    public List<CodeBlock> getBottomUpOrder() {
        List<CodeBlock> sorted = new ArrayList<>(blockMap.values());
        sorted.sort((a, b) -> b.getLevel() - a.getLevel());
        return sorted;
    }

    /** 获取所有叶子块 */
    public List<CodeBlock> getAllLeafBlocks() {
        return root.getLeafBlocks();
    }

    /** 总块数 */
    public int getTotalBlockCount() {
        return blockMap.size();
    }

    /** 打印树结构 */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        printTree(root, "", true, sb);
        return sb.toString();
    }

    private void printTree(CodeBlock block, String prefix, boolean isLast, StringBuilder sb) {
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(block.toString()).append("");

        List<CodeBlock> children = new ArrayList<>();
        for (CodeBlock child : block.getChildren()) {
            children.add(child);
        }
        for (int i = 0; i < children.size(); i++) {
            printTree(children.get(i),
                      prefix + (isLast ? "    " : "│   "),
                      i == children.size() - 1, sb);
        }
    }

    public CodeBlock getRoot() {
        return root;
    }
}


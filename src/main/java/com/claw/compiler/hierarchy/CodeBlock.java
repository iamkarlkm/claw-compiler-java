// ==================== CodeBlock.java ====================
package com.claw.compiler.hierarchy;

import com.claw.compiler.scanner.LineInfo;
import lombok.Getter;
import lombok.Setter;
import java.util.*;

/**
 * 代码块数据结构 - 无限分级
 * 
 * 思想2：自顶向下生成无限分级代码块
 */
@Getter
public class CodeBlock {
    /** 唯一标识 */
    private final String id;
    /** 层级深度（0=根） */
    private final int level;
    /** 父块 */
    @Setter
    private CodeBlock parent;
    /** 子块列表 */
    private final List<CodeBlock> children;
    /** 开始行号 */
    private final int startLine;
    /** 结束行号 */
    @Setter
    private int endLine;
    /** 块类型（18种之一） */
    @Setter
    private BlockType blockType;
    /** 块内包含的行 */
    private final List<LineInfo> lines;
    /** 块的内容（清理后的文本） */
    @Setter
    private String content;
    /** 归属的作用域名称 */
    @Setter
    private String scopeName;
    /** 附加属性 */
    private final Map<String, Object> attributes;

    public int getEndLine() {
        return endLine;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getScopeName() {
        return scopeName;
    }

    public void setScopeName(String scopeName) {
        this.scopeName = scopeName;
    }

    public CodeBlock(int level, int startLine) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.level = level;
        this.startLine = startLine;
        this.endLine = startLine;
        this.children = new ArrayList<>();
        this.lines = new ArrayList<>();
        this.attributes = new HashMap<>();
    }

    /** 添加子块 */
    public void addChild(CodeBlock child) {
        child.setParent(this);
        children.add(child);
    }

    /** 添加行 */
    public void addLine(LineInfo line) {
        lines.add(line);
    }

    /** 设置属性 */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /** 获取属性 */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /** 是否为叶子节点（无子块） */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /** 获取所有后代块 */
    public List<CodeBlock> getAllDescendants() {
        List<CodeBlock> result = new ArrayList<>();
        for (CodeBlock child : children) {
            result.add(child);
            result.addAll(child.getAllDescendants());
        }
        return result;
    }

    /** 获取最底层的代码块（叶子节点） */
    public List<CodeBlock> getLeafBlocks() {
        List<CodeBlock> result = new ArrayList<>();
        if (isLeaf()) {
            result.add(this);
        } else {
            for (CodeBlock child : children) {
                result.addAll(child.getLeafBlocks());
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("CodeBlock[id=%s, type=%s, level=%d, lines=%d-%d, children=%d]",
                id, blockType, level, startLine, endLine, children.size());
    }

    public List<Map<String, String>> getParameterList() {
        Object raw = attributes.get("parameters");
        if (raw == null) {
            return Collections.emptyList();
        }
        String params = raw.toString().trim();
        if (params.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (String part : params.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            String name = trimmed;
            String type = "";
            int colon = trimmed.indexOf(':');
            if (colon >= 0) {
                name = trimmed.substring(0, colon).trim();
                type = trimmed.substring(colon + 1).trim();
            }
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("name", name);
            entry.put("type", type);
            result.add(entry);
        }
        return result;
    }

    public List<String> getArgumentList() {
        Object raw = attributes.get("arguments");
        if (raw == null) {
            return Collections.emptyList();
        }
        String args = raw.toString().trim();
        if (args.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String part : args.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public List<String> getImportSymbols() {
        Object raw = attributes.get("import_symbols");
        if (raw == null) {
            return Collections.emptyList();
        }
        String symbols = raw.toString().trim();
        if (symbols.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String part : symbols.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public List<String> getExportSymbols() {
        Object raw = attributes.get("export_symbols");
        if (raw == null) {
            return Collections.emptyList();
        }
        String symbols = raw.toString().trim();
        if (symbols.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String part : symbols.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public BlockType getBlockType() {
        return this.blockType;
    }

    public List<CodeBlock> getChildren() {
        return this.children;
    }

    private void setParent(CodeBlock aThis) {
        this.parent = aThis;
    }
}


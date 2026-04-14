package com.claw.compiler.context;

import java.util.*;

import com.claw.compiler.hierarchy.BlockType;

/**
 * 结构上下文 - 第3层结构处理器（9个块处理器）的聚合输出
 * 
 * 包含：层级化代码块、配对关系、模块结构、作用域树等结构信息。
 * 由 hierarchy（分层）+ decomposer（分解）阶段产出，
 * 供 IRGenerator 在生成阶段使用。
 */
public class StructureContext {

    // ==================== 代码块层级 ====================

    /**
     * 代码块节点（对应18种代码块类型之一）
     */
    public static class BlockNode {
        private final String blockId;
        private final String blockType;        // 18种之一
        private final int startLine;
        private final int endLine;
        private final int level;               // 层级深度
        private final Map<String, String> attributes;
        private final List<BlockNode> children;
        private BlockNode parent;

        public BlockNode(String blockId, String blockType, int startLine, int endLine, int level) {
            this.blockId = blockId;
            this.blockType = blockType;
            this.startLine = startLine;
            this.endLine = endLine;
            this.level = level;
            this.attributes = new LinkedHashMap<>();
            this.children = new ArrayList<>();
        }

        public void addChild(BlockNode child) {
            child.parent = this;
            children.add(child);
        }

        public void setAttribute(String key, String value) {
            attributes.put(key, value);
        }

        public String getAttribute(String key) {
            return attributes.get(key);
        }

        // --- Getters ---
        public String getBlockId() { return blockId; }
        public String getBlockType() { return blockType; }
        public int getStartLine() { return startLine; }
        public int getEndLine() { return endLine; }
        public int getLevel() { return level; }
        public Map<String, String> getAttributes() { return Collections.unmodifiableMap(attributes); }
        public List<BlockNode> getChildren() { return Collections.unmodifiableList(children); }
        public BlockNode getParent() { return parent; }

        /**
         * 递归获取所有后代节点（自下至上遍历用）
         */
        public List<BlockNode> flattenBottomUp() {
            List<BlockNode> result = new ArrayList<>();
            for (BlockNode child : children) {
                result.addAll(child.flattenBottomUp());
            }
            result.add(this);
            return result;
        }

        @Override
        public String toString() {
            return "BlockNode[" + blockId + ", type=" + blockType +
                   ", lines=" + startLine + "-" + endLine +
                   ", level=" + level + ", children=" + children.size() + "]";
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
    }

    // ==================== 配对关系 ====================

    /**
     * 配对信息（{} () [] "" ''）
     */
    public static class PairInfo {
        private final char openChar;
        private final char closeChar;
        private final int openLine;
        private final int openCol;
        private final int closeLine;
        private final int closeCol;

        public PairInfo(char openChar, char closeChar,
                        int openLine, int openCol,
                        int closeLine, int closeCol) {
            this.openChar = openChar;
            this.closeChar = closeChar;
            this.openLine = openLine;
            this.openCol = openCol;
            this.closeLine = closeLine;
            this.closeCol = closeCol;
        }

        public char getOpenChar() { return openChar; }
        public char getCloseChar() { return closeChar; }
        public int getOpenLine() { return openLine; }
        public int getOpenCol() { return openCol; }
        public int getCloseLine() { return closeLine; }
        public int getCloseCol() { return closeCol; }

        @Override
        public String toString() {
            return "Pair['" + openChar + "' at " + openLine + ":" + openCol +
                   " <-> '" + closeChar + "' at " + closeLine + ":" + closeCol + "]";
        }
    }

    // ==================== 作用域信息 ====================

    /**
     * 作用域节点（归属关系）
     */
    public static class ScopeNode {
        private final String scopeId;
        private final String scopeName;
        private final String scopeType;        // function / type / module / block / loop
        private final int startLine;
        private final int endLine;
        private final List<ScopeNode> childScopes;
        private final List<String> declaredSymbols;  // 该作用域内声明的符号名
        private ScopeNode parentScope;

        public ScopeNode(String scopeId, String scopeName, String scopeType,
                         int startLine, int endLine) {
            this.scopeId = scopeId;
            this.scopeName = scopeName;
            this.scopeType = scopeType;
            this.startLine = startLine;
            this.endLine = endLine;
            this.childScopes = new ArrayList<>();
            this.declaredSymbols = new ArrayList<>();
        }

        public void addChildScope(ScopeNode child) {
            child.parentScope = this;
            childScopes.add(child);
        }

        public void addDeclaredSymbol(String symbol) {
            declaredSymbols.add(symbol);
        }

        /**
         * 在当前作用域及祖先作用域中查找符号
         */
        public boolean resolveSymbol(String symbol) {
            if (declaredSymbols.contains(symbol)) return true;
            if (parentScope != null) return parentScope.resolveSymbol(symbol);
            return false;
        }

        public String getScopeId() { return scopeId; }
        public String getScopeName() { return scopeName; }
        public String getScopeType() { return scopeType; }
        public int getStartLine() { return startLine; }
        public int getEndLine() { return endLine; }
        public List<ScopeNode> getChildScopes() { return Collections.unmodifiableList(childScopes); }
        public List<String> getDeclaredSymbols() { return Collections.unmodifiableList(declaredSymbols); }
        public ScopeNode getParentScope() { return parentScope; }
    }

    // ==================== 模块信息 ====================

    /**
     * 模块结构
     */
    public static class ModuleInfo {
        private final String moduleName;
        private final List<String> importedModules;
        private final List<String> exportedSymbols;

        public ModuleInfo(String moduleName) {
            this.moduleName = moduleName;
            this.importedModules = new ArrayList<>();
            this.exportedSymbols = new ArrayList<>();
        }

        public void addImport(String module) { importedModules.add(module); }
        public void addExport(String symbol) { exportedSymbols.add(symbol); }

        public String getModuleName() { return moduleName; }
        public List<String> getImportedModules() { return Collections.unmodifiableList(importedModules); }
        public List<String> getExportedSymbols() { return Collections.unmodifiableList(exportedSymbols); }
    }

    // ==================== StructureContext 主体 ====================

    private final List<BlockNode> rootBlocks;         // 顶层代码块（无限分级）
    private final List<PairInfo> pairings;            // 所有配对关系
    private final ScopeNode globalScope;              // 全局作用域（作用域树根）
    private final ModuleInfo moduleInfo;              // 模块信息
    private final List<String> sourceLines;           // 原始源代码行

    public StructureContext(List<String> sourceLines) {
        this.sourceLines = new ArrayList<>(sourceLines);
        this.rootBlocks = new ArrayList<>();
        this.pairings = new ArrayList<>();
        this.globalScope = new ScopeNode("global", "global", "module", 0, sourceLines.size() - 1);
        this.moduleInfo = new ModuleInfo("default");
    }

    // --- 构建方法 ---

    public void addRootBlock(BlockNode block) {
        rootBlocks.add(block);
    }

    public void addPairing(PairInfo pair) {
        pairings.add(pair);
    }

    // --- 查询方法 ---

    public List<BlockNode> getRootBlocks() {
        return Collections.unmodifiableList(rootBlocks);
    }

    public List<PairInfo> getPairings() {
        return Collections.unmodifiableList(pairings);
    }

    public ScopeNode getGlobalScope() {
        return globalScope;
    }

    public ModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    public List<String> getSourceLines() {
        return Collections.unmodifiableList(sourceLines);
    }

    /**
     * 获取所有代码块（自下至上展平），用于IR生成
     */
    public List<BlockNode> getAllBlocksBottomUp() {
        List<BlockNode> all = new ArrayList<>();
        for (BlockNode root : rootBlocks) {
            all.addAll(root.flattenBottomUp());
        }
        return all;
    }

    /**
     * 根据行号查找所在的代码块
     */
    public BlockNode findBlockAtLine(int lineNumber) {
        for (BlockNode root : rootBlocks) {
            BlockNode found = findBlockRecursive(root, lineNumber);
            if (found != null) return found;
        }
        return null;
    }

    private BlockNode findBlockRecursive(BlockNode node, int lineNumber) {
        if (lineNumber < node.getStartLine() || lineNumber > node.getEndLine()) {
            return null;
        }
        // 优先在子块中查找（更精确）
        for (BlockNode child : node.getChildren()) {
            BlockNode found = findBlockRecursive(child, lineNumber);
            if (found != null) return found;
        }
        return node;
    }

    /**
     * 根据块类型过滤所有块
     */
    public List<BlockNode> getBlocksByType(String blockType) {
        List<BlockNode> result = new ArrayList<>();
        for (BlockNode root : rootBlocks) {
            collectByType(root, blockType, result);
        }
        return result;
    }

    private void collectByType(BlockNode node, String blockType, List<BlockNode> result) {
        if (node.getBlockType().equals(blockType)) {
            result.add(node);
        }
        for (BlockNode child : node.getChildren()) {
            collectByType(child, blockType, result);
        }
    }

    @Override
    public String toString() {
        return "StructureContext[rootBlocks=" + rootBlocks.size() +
               ", pairings=" + pairings.size() +
               ", sourceLines=" + sourceLines.size() + "]";
    }
}



// package com.claw.compiler.context;

// import java.util.HashMap;
// import java.util.Map;

// import com.claw.compiler.processors.semantic.ModuleInfo;
// import com.claw.compiler.processors.semantic.ControlFlowProcessor.ControlFlowInfo;
// import com.claw.compiler.processors.semantic.FunctionProcessor.FunctionInfo;
// import com.claw.compiler.processors.semantic.TypeProcessor.TypeInfo;

// /**
//  * 结构上下文 - 存储代码结构信息
//  */
// public class StructureContext {
//     // 存储函数信息
//     private Map<String, FunctionInfo> functions = new HashMap<>();
    
//     // 存储类型信息
//     private Map<String, TypeInfo> types = new HashMap<>();
    
//     // 存储模块信息
//     private Map<String, ModuleInfo> modules = new HashMap<>();
    
//     // 存储控制流信息
//     private Map<String, ControlFlowInfo> controlFlows = new HashMap<>();
    
//     // 添加函数
//     public void addFunction(String name, FunctionInfo info) {
//         functions.put(name, info);
//     }
    
//     // 获取函数
//     public FunctionInfo getFunction(String name) {
//         return functions.get(name);
//     }
    
//     // 添加类型
//     public void addType(String name, TypeInfo info) {
//         types.put(name, info);
//     }
    
//     // 获取类型
//     public TypeInfo getType(String name) {
//         return types.get(name);
//     }
    
//     // 添加模块
//     public void addModule(String name, ModuleInfo info) {
//         modules.put(name, info);
//     }
    
//     // 获取模块
//     public ModuleInfo getModule(String name) {
//         return modules.get(name);
//     }
    
//     // 添加控制流
//     public void addControlFlow(String name, ControlFlowInfo info) {
//         controlFlows.put(name, info);
//     }
    
//     // 获取控制流
//     public ControlFlowInfo getControlFlow(String name) {
//         return controlFlows.get(name);
//     }
    
//     // 其他辅助方法...
// }

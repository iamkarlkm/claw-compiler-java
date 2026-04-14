package com.claw.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IR节点 - 表示一个编译单元
 */
public abstract class IRNode {
    private final IRNodeType type;
    private final int sourceLine;
    private final int sourceColumn;
    private final Map<String, Object> metadata;
    private final List<IRNode> children;
    
    protected IRNode(IRNodeType type, int sourceLine, int sourceColumn) {
        this.type = type;
        this.sourceLine = sourceLine;
        this.sourceColumn = sourceColumn;
        this.metadata = new HashMap<>();
        this.children = new ArrayList<>();
    }
    
    public void addChild(IRNode child) {
        children.add(child);
    }
    
    public void setMeta(String key, Object value) {
        metadata.put(key, value);
    }
    
    public <T> T getMeta(String key, Class<T> clazz) {
        return clazz.cast(metadata.get(key));
    }
    
    // Getters
    public IRNodeType getType() { return type; }
    public int getSourceLine() { return sourceLine; }
    public int getSourceColumn() { return sourceColumn; }
    public List<IRNode> getChildren() { return Collections.unmodifiableList(children); }
    public Map<String, Object> getMetadata() { return Collections.unmodifiableMap(metadata); }
}
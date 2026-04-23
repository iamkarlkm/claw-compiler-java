package com.q3lives.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * IR注解 - 表示程序注解和系统注解
 */
public class IRAnnotation {
    
    public enum AnnotationType {
        // 程序注解
        BEFORE_NAME, AFTER_NAME, BEFORE_PROPS, AFTER_PROPS,
        // 系统注解
        DESCRIPTION, PARAM, RETURN, EXAMPLE, DEPRECATED
    }
    
    private final AnnotationType type;
    private final Map<String, String> attributes;
    private final IRNode targetNode; // 注解所附着的节点
    
    public IRAnnotation(AnnotationType type, IRNode targetNode) {
        this.type = type;
        this.attributes = new LinkedHashMap<>();
        this.targetNode = targetNode;
    }
    
    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }
    
    public String getAttribute(String key) {
        return attributes.get(key);
    }
    
    // Getters
    public AnnotationType getType() { return type; }
    public Map<String, String> getAttributes() { return Collections.unmodifiableMap(attributes); }
    public IRNode getTargetNode() { return targetNode; }
}

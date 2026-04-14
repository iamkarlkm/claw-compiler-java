package com.claw.ir;

import java.util.*;

/**
 * Claw中间表示 - 语言无关的编译产物
 * 所有目标语言绑定层都基于此IR生成代码
 */
public class ClawIR {
    private final String moduleName;
    private final List<IRNode> nodes;
    private final List<IRAnnotation> annotations;
    private final FlowModel flowModel;
    
    public ClawIR(String moduleName) {
        this.moduleName = moduleName;
        this.nodes = new ArrayList<>();
        this.annotations = new ArrayList<>();
        this.flowModel = new FlowModel();
    }
    
    public void addNode(IRNode node) {
        nodes.add(node);
    }
    
    public void addAnnotation(IRAnnotation annotation) {
        annotations.add(annotation);
    }
    
    // Getters
    public String getModuleName() { return moduleName; }
    public List<IRNode> getNodes() { return Collections.unmodifiableList(nodes); }
    public List<IRAnnotation> getAnnotations() { return Collections.unmodifiableList(annotations); }
    public FlowModel getFlowModel() { return flowModel; }
}



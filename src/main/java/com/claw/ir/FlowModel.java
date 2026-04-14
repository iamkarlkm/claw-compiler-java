package com.claw.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 三层操作流模型
 */
public class FlowModel {
    
    public enum FlowType {
        NORMAL,     // 正常流
        EXCEPTION,  // 异常流
        BUSINESS    // 业务逻辑流转
    }
    
    private final List<FlowEntry> flows = new ArrayList<>();
    
    public void addFlow(FlowEntry entry) {
        flows.add(entry);
    }
    
    public List<FlowEntry> getFlows() {
        return Collections.unmodifiableList(flows);
    }
    
    public List<FlowEntry> getFlowsByType(FlowType type) {
        return flows.stream()
            .filter(f -> f.getType() == type)
            .collect(Collectors.toList());
    }
    
    public static class FlowEntry {
        private final FlowType type;
        private final IRNode sourceNode;
        private final IRNode targetNode;   // flow to 的目标（仅BUSINESS流使用）
        private final String catchType;     // 异常类型（仅EXCEPTION流使用）
        
        public FlowEntry(FlowType type, IRNode sourceNode) {
            this(type, sourceNode, null, null);
        }
        
        public FlowEntry(FlowType type, IRNode sourceNode, 
                         IRNode targetNode, String catchType) {
            this.type = type;
            this.sourceNode = sourceNode;
            this.targetNode = targetNode;
            this.catchType = catchType;
        }
        
        // Getters
        public FlowType getType() { return type; }
        public IRNode getSourceNode() { return sourceNode; }
        public IRNode getTargetNode() { return targetNode; }
        public String getCatchType() { return catchType; }
    }
}

package com.claw.ir.nodes;

import com.claw.ir.IRNode;
import com.claw.ir.IRNodeType;

/**
 * 业务逻辑流转节点 - flow to target
 */
public class FlowToNode extends IRNode {
    private final String targetName;
    
    public FlowToNode(String targetName, int line, int col) {
        super(IRNodeType.FLOW_TO, line, col);
        this.targetName = targetName;
    }
    
    public String getTargetName() { return targetName; }
}
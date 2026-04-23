package com.q3lives.ir.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.q3lives.ir.FlowModel;
import com.q3lives.ir.IRNode;
import com.q3lives.ir.IRNodeType;

/**
 * 函数声明节点
 */
public class FunctionDeclarationNode extends IRNode {
    private final String name;
    private final String returnType;
    private final FlowModel.FlowType flowType;
    private final List<ParameterNode> parameters;
    
    public FunctionDeclarationNode(String name, String returnType,
                                    FlowModel.FlowType flowType,
                                    int line, int col) {
        super(IRNodeType.FUNCTION_DECLARATION, line, col);
        this.name = name;
        this.returnType = returnType;
        this.flowType = flowType;
        this.parameters = new ArrayList<>();
    }
    
    public void addParameter(ParameterNode param) {
        parameters.add(param);
        addChild(param);
    }
    
    // Getters
    public String getName() { return name; }
    public String getReturnType() { return returnType; }
    public FlowModel.FlowType getFlowType() { return flowType; }
    public List<ParameterNode> getParameters() { 
        return Collections.unmodifiableList(parameters); 
    }

    public List<IRNode> getChildren() {
        return super.getChildren();
    }
}



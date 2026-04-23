package com.q3lives.ir.nodes;

import com.q3lives.ir.IRNode;
import com.q3lives.ir.IRNodeType;

/**
 * 异常流节点 - catch（无需try和{}）
 */
public class CatchBlockNode extends IRNode {
    private final String exceptionType;
    private final String variableName;
    
    public CatchBlockNode(String exceptionType, String variableName,
                          int line, int col) {
        super(IRNodeType.CATCH_BLOCK, line, col);
        this.exceptionType = exceptionType;
        this.variableName = variableName;
    }
    
    public String getExceptionType() { return exceptionType; }
    public String getVariableName() { return variableName; }
}
package com.claw.ir.nodes;

import com.claw.ir.IRNode;
import com.claw.ir.IRNodeType;

/**
 * 变量声明节点
 */
public class VariableDeclarationNode extends IRNode {
    private final String name;
    private final String declaredType;
    private final boolean isConst;
    private IRNode initializer;
    
    public VariableDeclarationNode(String name, String declaredType, 
                                    boolean isConst, int line, int col) {
        super(IRNodeType.VARIABLE_DECLARATION, line, col);
        this.name = name;
        this.declaredType = declaredType;
        this.isConst = isConst;
    }
    
    public void setInitializer(IRNode initializer) {
        this.initializer = initializer;
        addChild(initializer);
    }
    
    // Getters
    public String getName() { return name; }
    public String getDeclaredType() { return declaredType; }
    public boolean isConst() { return isConst; }
    public IRNode getInitializer() { return initializer; }
}

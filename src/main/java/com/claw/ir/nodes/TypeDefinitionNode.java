package com.claw.ir.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.claw.ir.IRNode;
import com.claw.ir.IRNodeType;

/**
 * 类型定义节点
 */
public class TypeDefinitionNode extends IRNode {
    private final String name;
    private final List<FunctionDeclarationNode> fields;
    private final List<FunctionDeclarationNode> methods;
    
    public TypeDefinitionNode(String name, int line, int col) {
        super(IRNodeType.TYPE_DEFINITION, line, col);
        this.name = name;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
    }
    
    public void addField(FunctionDeclarationNode field) {
        fields.add(field);
        addChild(field);
    }
    
    public void addMethod(FunctionDeclarationNode method) {
        methods.add(method);
        addChild(method);
    }
    
    // Getters
    public String getName() { return name; }
    public List<FunctionDeclarationNode> getFields() { 
        return Collections.unmodifiableList(fields); 
    }
    public List<FunctionDeclarationNode> getMethods() { 
        return Collections.unmodifiableList(methods); 
    }
}
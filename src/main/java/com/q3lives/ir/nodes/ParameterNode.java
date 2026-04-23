package com.q3lives.ir.nodes;

import com.q3lives.ir.IRNode;
import com.q3lives.ir.IRNodeType;

/**
 * 参数节点 - 表示函数参数
 */
public class ParameterNode extends IRNode {
    // 参数名称
    private final String name;
    
    // 参数类型
    private final String type;
    
    // 参数位置（在参数列表中的位置）
    private final int position;
    
    // 是否有默认值
    private final boolean hasDefaultValue;
    
    // 默认值
    private IRNode defaultValue;
    
    public ParameterNode(String name, String type, int position, int line, int col) {
        super(IRNodeType.PARAMETER, line, col);
        this.name = name;
        this.type = type;
        this.position = position;
        this.hasDefaultValue = false;
    }
    
    public ParameterNode(String name, String type, int position, 
                         IRNode defaultValue, int line, int col) {
        super(IRNodeType.PARAMETER, line, col);
        this.name = name;
        this.type = type;
        this.position = position;
        this.hasDefaultValue = true;
        this.defaultValue = defaultValue;
        addChild(defaultValue);
    }
    
    // Getter方法
    public String getName() {
        return name;
    }
    
    public IRNodeType getType() {
        return super.getType();
    }
    
    public int getPosition() {
        return position;
    }
    
    public boolean hasDefaultValue() {
        return hasDefaultValue;
    }
    
    public IRNode getDefaultValue() {
        return defaultValue;
    }
    
    @Override
    public String toString() {
        return "ParameterNode{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", position=" + position +
                ", hasDefaultValue=" + hasDefaultValue +
                '}';
    }
}

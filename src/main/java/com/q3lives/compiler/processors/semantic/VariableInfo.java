package com.q3lives.compiler.processors.semantic;

/**
 * 变量信息 - 存储变量相关信息
 */
public class VariableInfo {
    // 变量名称
    private String name;
    
    // 变量类型
    private String type;
    
    // 变量值
    private Object value;
    
    // 是否为常量
    private boolean isConstant;
    
    // 变量作用域
    private String scope;
    
    public VariableInfo(String name, String type) {
        this.name = name;
        this.type = type;
    }
    
    // Getter和Setter方法
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
    
    public boolean isConstant() {
        return isConstant;
    }
    
    public void setConstant(boolean constant) {
        isConstant = constant;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    @Override
    public String toString() {
        return "VariableInfo{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", value=" + value +
                ", isConstant=" + isConstant +
                ", scope='" + scope + '\'' +
                '}';
    }
}

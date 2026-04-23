package com.q3lives.compiler.processors.semantic;

/**
 * 字面量信息 - 存储字面量相关信息
 */
public class LiteralInfo {
    // 字面量值
    private Object value;
    
    // 字面量类型
    private String type;
    
    // 字面量位置
    private int line;
    private int column;
    
    public LiteralInfo(Object value, String type) {
        this.value = value;
        this.type = type;
    }
    
    // Getter和Setter方法
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getLine() {
        return line;
    }
    
    public void setLine(int line) {
        this.line = line;
    }
    
    public int getColumn() {
        return column;
    }
    
    public void setColumn(int column) {
        this.column = column;
    }
    
    @Override
    public String toString() {
        return "LiteralInfo{" +
                "value=" + value +
                ", type='" + type + '\'' +
                ", line=" + line +
                ", column=" + column +
                '}';
    }
}

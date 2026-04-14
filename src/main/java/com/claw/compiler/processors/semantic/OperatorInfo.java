package com.claw.compiler.processors.semantic;

/**
 * 运算符信息 - 存储运算符相关信息
 */
public class OperatorInfo {
    // 运算符符号
    private String symbol;
    
    // 运算符类型
    private String type;
    
    // 运算符优先级
    private int precedence;
    
    // 运算符结合性
    private String associativity;
    
    public OperatorInfo(String symbol, String type) {
        this.symbol = symbol;
        this.type = type;
    }
    
    // Getter和Setter方法
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getPrecedence() {
        return precedence;
    }
    
    public void setPrecedence(int precedence) {
        this.precedence = precedence;
    }
    
    public String getAssociativity() {
        return associativity;
    }
    
    public void setAssociativity(String associativity) {
        this.associativity = associativity;
    }
    
    @Override
    public String toString() {
        return "OperatorInfo{" +
                "symbol='" + symbol + '\'' +
                ", type='" + type + '\'' +
                ", precedence=" + precedence +
                ", associativity='" + associativity + '\'' +
                '}';
    }
}

package com.q3lives.compiler.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Token流 - 存储词法分析结果
 */
public class TokenStream implements Iterable<Token> {
    // Token列表
    private final List<Token> tokens;
    
    // 当前位置
    private int position;
    
    public TokenStream() {
        this.tokens = new ArrayList<>();
        this.position = 0;
    }
    
    public TokenStream(List<Token> tokens) {
        this.tokens = new ArrayList<>(tokens);
        this.position = 0;
    }
    
    /**
     * 添加Token
     */
    public void addToken(Token token) {
        tokens.add(token);
    }
    
    /**
     * 获取当前Token
     */
    public Token current() {
        if (position < tokens.size()) {
            return tokens.get(position);
        }
        return null;
    }
    
    /**
     * 获取下一个Token并移动位置
     */
    public Token next() {
        if (position < tokens.size()) {
            return tokens.get(position++);
        }
        return null;
    }
    
    /**
     * 查看下一个Token但不移动位置
     */
    public Token peek() {
        if (position + 1 < tokens.size()) {
            return tokens.get(position + 1);
        }
        return null;
    }
    
    /**
     * 检查是否还有Token
     */
    public boolean hasNext() {
        return position < tokens.size();
    }
    
    /**
     * 重置位置到开始
     */
    public void reset() {
        this.position = 0;
    }
    
    /**
     * 获取Token数量
     */
    public int size() {
        return tokens.size();
    }
    
    /**
     * 获取指定位置的Token
     */
    public Token get(int index) {
        if (index >= 0 && index < tokens.size()) {
            return tokens.get(index);
        }
        return null;
    }
    
    /**
     * 设置当前位置
     */
    public void setPosition(int position) {
        if (position >= 0 && position <= tokens.size()) {
            this.position = position;
        }
    }
    
    /**
     * 获取当前位置
     */
    public int getPosition() {
        return position;
    }
    
    @Override
    public Iterator<Token> iterator() {
        return tokens.iterator();
    }
}

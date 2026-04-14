package com.claw.compiler.processors.semantic;

import java.util.HashMap;
import java.util.Map;

/**
 * 注解信息 - 存储注解相关信息
 */
public class AnnotationInfo {
    // 注解名称
    private String name;
    
    // 注解类型
    private String type;
    
    // 注解参数
    private Map<String, Object> parameters;
    
    // 注解作用目标
    private String target;
    
    public AnnotationInfo(String name, String type) {
        this.name = name;
        this.type = type;
        this.parameters = new HashMap<>();
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
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }
    
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
    @Override
    public String toString() {
        return "AnnotationInfo{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", parameters=" + parameters +
                ", target='" + target + '\'' +
                '}';
    }
}

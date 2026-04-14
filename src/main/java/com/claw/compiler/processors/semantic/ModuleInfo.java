package com.claw.compiler.processors.semantic;

/**
 * 模块信息 - 存储模块相关信息
 */
public class ModuleInfo {
    // 模块名称
    private String name;
    
    // 模块可见性
    private String visibility;
    
    // 模块描述
    private String description;
    
    // 模块依赖
    private java.util.List<String> dependencies;
    
    // 模块注解
    private java.util.List<AnnotationInfo> annotations;
    
    public ModuleInfo(String name) {
        this.name = name;
        this.dependencies = new java.util.ArrayList<>();
        this.annotations = new java.util.ArrayList<>();
    }
    
    // Getter和Setter方法
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getVisibility() {
        return visibility;
    }
    
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public java.util.List<String> getDependencies() {
        return dependencies;
    }
    
    public void addDependency(String dependency) {
        this.dependencies.add(dependency);
    }
    
    public java.util.List<AnnotationInfo> getAnnotations() {
        return annotations;
    }
    
    public void addAnnotation(AnnotationInfo annotation) {
        this.annotations.add(annotation);
    }
    
    @Override
    public String toString() {
        return "ModuleInfo{" +
                "name='" + name + '\'' +
                ", visibility='" + visibility + '\'' +
                ", description='" + description + '\'' +
                ", dependencies=" + dependencies +
                ", annotations=" + annotations +
                '}';
    }
}

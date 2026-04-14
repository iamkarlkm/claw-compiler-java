package com.claw.binding.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.claw.binding.GenerationConfig;
import com.claw.ir.IRAnnotation;
import com.claw.ir.IRNode;

import claw.compiler.generators.ClawIR;

/**
 * Java代码生成上下文 - 管理缩进、钩子注册、代码输出
 */
public class JavaGenerationContext {
    
    private final ClawIR ir;
    private final GenerationConfig config;
    private final JavaTypeMapper typeMapper;
    private final JavaRuntime runtime;
    
    private final StringBuilder output;
    private int indentLevel;
    
    // 注解钩子注册表
    private final List<IRAnnotation> constructorHooks;
    private final List<IRAnnotation> destructorHooks;
    private final Map<String, List<IRAnnotation>> propertyBeforeHooks;
    private final Map<String, List<IRAnnotation>> propertyAfterHooks;
    private final Map<IRNode, IRAnnotation> descriptionMap;
    
    public JavaGenerationContext(ClawIR ir, GenerationConfig config,
                                 JavaTypeMapper typeMapper, JavaRuntime runtime) {
        this.ir = ir;
        this.config = config;
        this.typeMapper = typeMapper;
        this.runtime = runtime;
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.constructorHooks = new ArrayList<>();
        this.destructorHooks = new ArrayList<>();
        this.propertyBeforeHooks = new HashMap<>();
        this.propertyAfterHooks = new HashMap<>();
        this.descriptionMap = new HashMap<>();
    }
    
    // === 缩进管理 ===
    
    public void pushIndent() { indentLevel++; }
    public void popIndent() { indentLevel = Math.max(0, indentLevel - 1); }
    
    public void appendLine(String line) {
        if (line.isEmpty()) {
            output.append(config.getLineSeparator());
        } else {
            output.append(config.getIndentStyle().repeat(indentLevel))
                  .append(line)
                  .append(config.getLineSeparator());
        }
    }
    
    // === 钩子注册 ===
    
    public void registerConstructorHook(IRAnnotation ann) {
        constructorHooks.add(ann);
    }
    
    public void registerDestructorHook(IRAnnotation ann) {
        destructorHooks.add(ann);
    }
    
    public void registerPropertyBeforeHook(IRAnnotation ann) {
        String propList = ann.getAttribute("property_list");
        if (propList != null) {
            for (String prop : propList.split(",")) {
                propertyBeforeHooks
                    .computeIfAbsent(prop.trim(), k -> new ArrayList<>())
                    .add(ann);
            }
        }
    }
    
    public void registerPropertyAfterHook(IRAnnotation ann) {
        String propList = ann.getAttribute("property_list");
        if (propList != null) {
            for (String prop : propList.split(",")) {
                propertyAfterHooks
                    .computeIfAbsent(prop.trim(), k -> new ArrayList<>())
                    .add(ann);
            }
        }
    }
    
    public void registerDescription(IRAnnotation ann) {
        descriptionMap.put(ann.getTargetNode(), ann);
    }
    
    public void registerSystemAnnotation(IRAnnotation ann) {
        // @@param, @@return, @@example, @@deprecated → Javadoc
        descriptionMap.put(ann.getTargetNode(), ann);
    }
    
    // === 钩子查询 ===
    
    public List<IRAnnotation> getConstructorHooks() {
        return Collections.unmodifiableList(constructorHooks);
    }
    
    public List<IRAnnotation> getDestructorHooks() {
        return Collections.unmodifiableList(destructorHooks);
    }
    
    public List<IRAnnotation> getPropertyBeforeHooks(String propName) {
        return propertyBeforeHooks.getOrDefault(propName, Collections.emptyList());
    }
    
    public List<IRAnnotation> getPropertyAfterHooks(String propName) {
        return propertyAfterHooks.getOrDefault(propName, Collections.emptyList());
    }
    
    // === Javadoc 生成 ===
    
    public void emitDescriptionJavadoc(IRNode node) {
        IRAnnotation desc = descriptionMap.get(node);
        if (desc != null && desc.getType() == IRAnnotation.AnnotationType.DESCRIPTION) {
            appendLine("/**");
            appendLine(" * " + desc.getAttribute("human_desc"));
            appendLine(" * IO: " + desc.getAttribute("io_spec"));
            appendLine(" */");
        }
    }
    
    // === 输出 ===
    
    public String buildOutput() {
        return output.toString();
    }
}

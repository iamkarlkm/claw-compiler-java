package com.claw.compiler.annotation;

import java.util.*;

/**
 * 注解处理结果 - 注解管理器的输出
 * 
 * 包含4个程序注解和5个系统注解的解析结果。
 */
public class AnnotationResult {

    /**
     * 程序注解数据
     */
    public static class ProgramAnnotation {
        private final String type;              // BeforeName / AfterName / BeforeProps / AfterProps
        private final String methodName;        // BeforeName/AfterName 的方法名
        private final String target;            // BeforeName/AfterName 的 target
        private final List<String> properties;  // BeforeProps/AfterProps 的属性列表

        // 构造/析构注解
        public ProgramAnnotation(String type, String methodName, String target) {
            this.type = type;
            this.methodName = methodName;
            this.target = target;
            this.properties = Collections.emptyList();
        }

        // 属性监听注解
        public ProgramAnnotation(String type, List<String> properties) {
            this.type = type;
            this.methodName = null;
            this.target = null;
            this.properties = new ArrayList<>(properties);
        }

        public String getType() { return type; }
        public String getMethodName() { return methodName; }
        public String getTarget() { return target; }
        public List<String> getProperties() { return Collections.unmodifiableList(properties); }
    }

    // ==================== 主体 ====================

    private final List<ProgramAnnotation> programAnnotations;
    private final Map<String, String> systemAnnotations;

    // 属性监听索引（快速查找）
    private final Set<String> beforePropsTargets;
    private final Set<String> afterPropsTargets;

    public AnnotationResult() {
        this.programAnnotations = new ArrayList<>();
        this.systemAnnotations = new LinkedHashMap<>();
        this.beforePropsTargets = new HashSet<>();
        this.afterPropsTargets = new HashSet<>();
    }

    // --- 程序注解 ---

    public void addProgramAnnotation(ProgramAnnotation pa) {
        programAnnotations.add(pa);

        // 建立属性监听索引
        if ("BeforeProps".equals(pa.getType())) {
            beforePropsTargets.addAll(pa.getProperties());
        } else if ("AfterProps".equals(pa.getType())) {
            afterPropsTargets.addAll(pa.getProperties());
        }
    }

    public List<ProgramAnnotation> getProgramAnnotations() {
        return Collections.unmodifiableList(programAnnotations);
    }

    /**
     * 检查某个属性是否有 @BeforeProps 监听
     */
    public boolean hasBeforePropsFor(String propertyPath) {
        return beforePropsTargets.contains(propertyPath);
    }

    /**
     * 检查某个属性是否有 @AfterProps 监听
     */
    public boolean hasAfterPropsFor(String propertyPath) {
        return afterPropsTargets.contains(propertyPath);
    }

    // --- 系统注解 ---

    public void addSystemAnnotation(String key, String value) {
        systemAnnotations.put(key, value);
    }

    public Map<String, String> getSystemAnnotations() {
        return Collections.unmodifiableMap(systemAnnotations);
    }

    public String getSystemAnnotation(String key) {
        return systemAnnotations.get(key);
    }
}

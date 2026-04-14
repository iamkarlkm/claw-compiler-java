// ==================== PropertyManager.java ====================
package com.claw.compiler.integration;

import com.claw.compiler.annotation.ProgramAnnotations;
import com.claw.compiler.annotation.ProgramAnnotations.ProgramAnnotation;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 属性管理器 - 处理 @BeforeProps/@AfterProps 属性监听注解
 */
@Slf4j
public class PropertyManager {

    /** 属性变更前监听器 */
    private final Map<String, List<PropertyListener>> beforeListeners = new LinkedHashMap<>();
    /** 属性变更后监听器 */
    private final Map<String, List<PropertyListener>> afterListeners = new LinkedHashMap<>();

    /**
     * 处理属性监听注解
     */
    public void processAnnotations(ProgramAnnotations annotations) {
        for (ProgramAnnotation ann : annotations.getBeforePropsAnnotations()) {
            for (String prop : ann.getProperties()) {
                beforeListeners.computeIfAbsent(prop, k -> new ArrayList<>())
                    .add(new PropertyListener(prop, "before", ann.getLine()));
                log.debug("注册属性变更前监听: {}", prop);
            }
        }

        for (ProgramAnnotation ann : annotations.getAfterPropsAnnotations()) {
            for (String prop : ann.getProperties()) {
                afterListeners.computeIfAbsent(prop, k -> new ArrayList<>())
                    .add(new PropertyListener(prop, "after", ann.getLine()));
                log.debug("注册属性变更后监听: {}", prop);
            }
        }

        log.info("属性管理器: {} 个before监听, {} 个after监听",
                beforeListeners.size(), afterListeners.size());
    }

    /**
     * 生成属性赋值代码（包含监听器调用）
     */
    public List<String> generatePropertySetCode(String propertyName, String value) {
        List<String> code = new ArrayList<>();

        // 变更前回调
        if (beforeListeners.containsKey(propertyName)) {
            code.add("BEFORE_PROP_CHANGE " + propertyName);
        }

        // 实际赋值
        code.add("SET_PROP " + propertyName + " = " + value);

        // 变更后回调
        if (afterListeners.containsKey(propertyName)) {
            code.add("AFTER_PROP_CHANGE " + propertyName);
        }

        return code;
    }

    public record PropertyListener(String property, String timing, int line) {}
}

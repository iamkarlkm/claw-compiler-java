// ==================== MemoryManager.java ====================
package com.claw.compiler.integration;

import com.claw.compiler.annotation.ProgramAnnotations;
import com.claw.compiler.annotation.ProgramAnnotations.ProgramAnnotation;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 内存管理器 - 处理 @BeforeName/@AfterName 构造/析构注解
 */
@Slf4j
public class MemoryManager {

    /** 构造函数注册表 */
    private final Map<String, ConstructorInfo> constructors = new LinkedHashMap<>();
    /** 析构函数注册表 */
    private final Map<String, DestructorInfo> destructors = new LinkedHashMap<>();

    /**
     * 处理构造/析构注解
     */
    public void processAnnotations(ProgramAnnotations annotations) {
        for (ProgramAnnotation ann : annotations.getConstructorAnnotations()) {
            ConstructorInfo info = new ConstructorInfo(ann.getMethodName(), ann.getTarget(), ann.getLine());
            constructors.put(ann.getTarget() + "." + ann.getMethodName(), info);
            log.debug("注册构造函数: {} -> {}", ann.getTarget(), ann.getMethodName());
        }

        for (ProgramAnnotation ann : annotations.getDestructorAnnotations()) {
            DestructorInfo info = new DestructorInfo(ann.getMethodName(), ann.getTarget(), ann.getLine());
            destructors.put(ann.getTarget() + "." + ann.getMethodName(), info);
            log.debug("注册析构函数: {} -> {}", ann.getTarget(), ann.getMethodName());
        }

        log.info("内存管理器: {} 个构造函数, {} 个析构函数", constructors.size(), destructors.size());
    }

    /**
     * 生成内存分配代码（插入构造函数调用）
     */
    public List<String> generateAllocationCode(String target) {
        List<String> code = new ArrayList<>();
        code.add("ALLOC " + target);
        constructors.values().stream()
                .filter(c -> c.target.equals(target) || "this".equals(c.target))
                .forEach(c -> code.add("CALL_CONSTRUCTOR " + c.methodName));
        return code;
    }

    /**
     * 生成内存回收代码（插入析构函数调用）
     */
    public List<String> generateDeallocationCode(String target) {
        List<String> code = new ArrayList<>();
        destructors.values().stream()
                .filter(d -> d.target.equals(target) || "this".equals(d.target))
                .forEach(d -> code.add("CALL_DESTRUCTOR " + d.methodName));
        code.add("DEALLOC " + target);
        return code;
    }

    public record ConstructorInfo(String methodName, String target, int line) {}
    public record DestructorInfo(String methodName, String target, int line) {}
}

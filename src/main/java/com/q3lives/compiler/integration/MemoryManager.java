// ==================== MemoryManager.java ====================
package com.q3lives.compiler.integration;

import com.q3lives.compiler.annotation.ProgramAnnotations;
import com.q3lives.compiler.annotation.ProgramAnnotations.ProgramAnnotation;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存管理器 - 处理 @BeforeName/@AfterName 构造/析构注解
 *
 * 优化：使用ConcurrentHashMap提高性能，集成对象池管理
 */
@Slf4j
public class MemoryManager {

    // 使用ConcurrentHashMap提高并发性能
    /** 构造函数注册表 */
    private final Map<String, ConstructorInfo> constructors = new ConcurrentHashMap<>();
    /** 析构函数注册表 */
    private final Map<String, DestructorInfo> destructors = new ConcurrentHashMap<>();

    // 对象池配置
    private static final int POOL_SIZE = 50;
    private final MemoryObjectPool<List<String>> codeListPool;

    public MemoryManager() {
        // 初始化对象池
        this.codeListPool = new MemoryObjectPool<>(
            () -> new ArrayList<>(16), // 初始容量16
            POOL_SIZE,
            POOL_SIZE * 2
        );
    }

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
        // 从对象池获取List
        List<String> code;
        try {
            code = codeListPool.borrow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
        try {
            code.clear(); // 清空复用
            code.add("ALLOC " + target);
            constructors.values().parallelStream()
                    .filter(c -> c.target.equals(target) || "this".equals(c.target))
                    .forEach(c -> code.add("CALL_CONSTRUCTOR " + c.methodName));
            return code;
        } catch (Exception e) {
            // 发生异常时返回新对象
            return new ArrayList<>();
        }
    }

    /**
     * 生成内存回收代码（插入析构函数调用）
     */
    public List<String> generateDeallocationCode(String target) {
        // 从对象池获取List
        List<String> code;
        try {
            code = codeListPool.borrow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
        try {
            code.clear(); // 清空复用
            destructors.values().parallelStream()
                    .filter(d -> d.target.equals(target) || "this".equals(d.target))
                    .forEach(d -> code.add("CALL_DESTRUCTOR " + d.methodName));
            code.add("DEALLOC " + target);
            return code;
        } catch (Exception e) {
            // 发生异常时返回新对象
            return new ArrayList<>();
        }
    }

    /**
     * 释放代码列表回池中
     */
    public void releaseCodeList(List<String> code) {
        if (code != null) {
            codeListPool.returnObject(code);
        }
    }

    /**
     * 获取内存统计信息
     */
    public MemoryStats getMemoryStats() {
        return new MemoryStats(
            constructors.size(),
            destructors.size(),
            codeListPool.getStats()
        );
    }

    /**
     * 内存统计信息
     */
    public record MemoryStats(int constructorCount, int destructorCount,
                            MemoryObjectPool.PoolStats poolStats) {}

    public record ConstructorInfo(String methodName, String target, int line) {}
    public record DestructorInfo(String methodName, String target, int line) {}
}

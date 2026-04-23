package com.q3lives.compiler.integration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内存对象池 - 减少对象创建和GC压力
 *
 * 使用场景：
 * - AST节点（临时创建和销毁）
 * - Token对象（大量创建）
 * - 临时字符串构建器
 * - 缓存条目
 */
public class MemoryObjectPool<T> {

    // 对象池配置
    private final BlockingQueue<T> pool;
    private final AtomicInteger activeObjects = new AtomicInteger(0);
    private final ObjectFactory<T> factory;
    private final int maxPoolSize;
    private final int maxActiveObjects;

    /**
     * 对象工厂接口
     */
    @FunctionalInterface
    public interface ObjectFactory<T> {
        T create();
        void reset(T obj); // 重置对象状态
    }

    public MemoryObjectPool(ObjectFactory<T> factory, int poolSize, int maxActive) {
        this.factory = factory;
        this.maxPoolSize = poolSize;
        this.maxActiveObjects = maxActive;
        this.pool = new ArrayBlockingQueue<>(poolSize);

        // 预填充池
        for (int i = 0; i < poolSize / 2; i++) {
            T obj = factory.create();
            pool.offer(obj);
        }
    }

    /**
     * 获取对象
     */
    public T borrow() throws InterruptedException {
        // 检查活跃对象数量
        if (activeObjects.get() >= maxActiveObjects) {
            throw new IllegalStateException("已达到最大活跃对象限制");
        }

        T obj = pool.poll();
        if (obj == null) {
            // 池为空，创建新对象
            obj = factory.create();
        }
        activeObjects.incrementAndGet();
        return obj;
    }

    /**
     * 归还对象
     */
    public void returnObject(T obj) {
        if (obj == null) return;

        // 重置对象状态
        factory.reset(obj);

        // 尝试放回池中
        if (!pool.offer(obj)) {
            // 池已满，对象将被GC回收
        }
        activeObjects.decrementAndGet();
    }

    /**
     * 获取池统计信息
     */
    public PoolStats getStats() {
        return new PoolStats(
            pool.size(),
            activeObjects.get(),
            maxPoolSize,
            maxActiveObjects
        );
    }

    /**
     * 池统计信息
     */
    public record PoolStats(int available, int active, int maxPool, int maxActive) {}

    // ========== 预定义的池类型 ==========

    /**
     * AST节点池（示例）
     */
    public static class ASTNodePool extends MemoryObjectPool<ASTNodeWrapper> {
        public ASTNodePool(int poolSize) {
            super(() -> new ASTNodeWrapper(), poolSize, poolSize * 2);
        }
    }

    /**
     * Token对象包装器（示例）
     */
    public static class TokenWrapper {
        // 可以包含Token相关数据
    }

    /**
     * AST节点包装器示例
     */
    public static class ASTNodeWrapper {
        // 可以包含AST节点相关数据
    }
}
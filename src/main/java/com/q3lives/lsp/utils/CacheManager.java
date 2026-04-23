package com.q3lives.lsp.utils;

import com.q3lives.lsp.protocol.CompletionItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * LSP 缓存管理器
 *
 * 提供统一的缓存策略：
 * - LRU 缓存
 * - 时间过期
 * - 自动清理
 */
public class CacheManager<K, V> {

    private final Map<K, CacheEntry<V>> cache;
    private final long ttlMillis;
    private final int maxSize;

    /**
     * 缓存条目
     */
    private static class CacheEntry<V> {
        private final V value;
        private final long timestamp;
        private final long expireTime;

        CacheEntry(V value, long ttlMillis) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
            this.expireTime = timestamp + ttlMillis;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

        public V getValue() {
            return value;
        }
    }

    /**
     * 创建缓存管理器
     *
     * @param ttlMillis TTL（毫秒）
     * @param maxSize 最大缓存大小
     */
    public CacheManager(long ttlMillis, int maxSize) {
        this.ttlMillis = ttlMillis;
        this.maxSize = maxSize;
        this.cache = new HashMap<>();
    }

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存值，如果不存在或已过期返回 null
     */
    public V get(K key) {
        if (key == null) {
            return null;
        }

        CacheEntry<V> entry = cache.get(key);

        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }

        return entry.getValue();
    }

    /**
     * 设置缓存值
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    public void put(K key, V value) {
        if (key == null || value == null) {
            return;
        }

        // 检查是否超过最大大小
        if (cache.size() >= maxSize) {
            evictLRU();
        }

        cache.put(key, new CacheEntry<>(value, ttlMillis));
    }

    /**
     * 移除缓存值
     *
     * @param key 缓存键
     */
    public void remove(K key) {
        if (key == null) {
            return;
        }
        cache.remove(key);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 获取缓存大小
     *
     * @return 当前缓存大小
     */
    public int size() {
        // 清理过期项
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return cache.size();
    }

    /**
     * 清理过期项
     */
    public void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息字符串
     */
    public String getStats() {
        cleanup();
        return String.format("Cache stats: size=%d, ttl=%dms", cache.size(), ttlMillis);
    }

    /**
     * 移除最久未使用的项
     */
    private void evictLRU() {
        if (cache.isEmpty()) {
            return;
        }

        // 找到最旧的条目
        K oldestKey = null;
        long oldestTimestamp = Long.MAX_VALUE;

        for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
            if (entry.getValue().timestamp < oldestTimestamp) {
                oldestTimestamp = entry.getValue().timestamp;
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }

    // ===== 预定义缓存实例 =====

    /**
     * 代码补全缓存（100项，30秒过期）
     */
    public static CacheManager<String, List<CompletionItem>> completionCache() {
        return new CacheManager<>(30000, 100);
    }

    /**
     * 语法检查缓存（50项，60秒过期）
     */
    public static CacheManager<String, Object> syntaxCache() {
        return new CacheManager<>(60000, 50);
    }

    /**
     * 语义上下文缓存（30项，120秒过期）
     */
    public static CacheManager<String, Object> semanticCache() {
        return new CacheManager<>(120000, 30);
    }

    /**
     * 定义查找缓存（100项，60秒过期）
     */
    public static CacheManager<String, Object> definitionCache() {
        return new CacheManager<>(60000, 100);
    }

    /**
     * 引用查找缓存（100项，60秒过期）
     */
    public static CacheManager<String, Object> referenceCache() {
        return new CacheManager<>(60000, 100);
    }
}

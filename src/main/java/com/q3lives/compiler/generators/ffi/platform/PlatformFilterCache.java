package com.q3lives.compiler.generators.ffi.platform;

import com.q3lives.compiler.generators.ffi.FFIBindingTable;
import java.util.LinkedHashMap;
import java.util.Map;



/**
 * 平台过滤缓存
 *
 * 场景：增量编译中，同一目标平台的过滤结果可以复用
 * 缓存键：目标位掩码 + 绑定表版本号
 */
public class PlatformFilterCache {

    private static final int MAX_CACHE_SIZE = 8;  // 通常不会超过几个目标平台

    /**
     * 缓存结构：目标位掩码 → {版本号, 过滤后的绑定表}
     */
    private final Map<Long, CacheEntry> cache = new LinkedHashMap<Long, CacheEntry>(
        MAX_CACHE_SIZE + 1, 0.75f, true  // accessOrder=true, LRU 淘汰
    ) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CacheEntry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private static class CacheEntry {
        final long tableVersion;
        final FFIBindingTable filteredTable;

        CacheEntry(long tableVersion, FFIBindingTable filteredTable) {
            this.tableVersion = tableVersion;
            this.filteredTable = filteredTable;
        }
    }

    /**
     * 获取过滤后的绑定表（带缓存）
     */
    public FFIBindingTable getFiltered(FFIBindingTable fullTable, TargetTriple target) {
        long cacheKey = target.toBitmask();
        long currentVersion = fullTable.getVersion();

        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && entry.tableVersion == currentVersion) {
            return entry.filteredTable;  // 缓存命中
        }

        // 缓存未命中，执行过滤
        FFIBindingTable filtered = fullTable.filterForPlatform(target);
        cache.put(cacheKey, new CacheEntry(currentVersion, filtered));
        return filtered;
    }

    public void invalidate() {
        cache.clear();
    }

    public int hitCount() {
        // 可以添加统计计数器
        return 0;
    }
}

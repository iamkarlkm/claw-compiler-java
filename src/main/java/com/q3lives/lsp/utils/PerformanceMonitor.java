package com.q3lives.lsp.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * LSP 性能监控器
 *
 * 跟踪和记录各种 LSP 操作的性能指标
 */
public class PerformanceMonitor {

    private static final PerformanceMonitor instance = new PerformanceMonitor();

    // 操作统计
    private final Map<String, OperationStats> stats = new ConcurrentHashMap<>();

    // 性能阈值（毫秒）
    private long completionThreshold = 100;
    private long syntaxCheckThreshold = 500;
    private long definitionThreshold = 50;
    private long referenceThreshold = 200;

    private PerformanceMonitor() {
    }

    public static PerformanceMonitor getInstance() {
        return instance;
    }

    /**
     * 开始监控
     *
     * @param operation 操作名称
     * @return 开始时间戳
     */
    public long start(String operation) {
        return System.currentTimeMillis();
    }

    /**
     * 结束监控
     *
     * @param operation 操作名称
     * @param startTime 开始时间戳
     */
    public void end(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        record(operation, duration);
    }

    /**
     * 记录操作性能
     *
     * @param operation 操作名称
     * @param duration 持续时间（毫秒）
     */
    public void record(String operation, long duration) {
        OperationStats stat = stats.computeIfAbsent(operation, k -> new OperationStats());

        stat.totalCalls.incrementAndGet();
        stat.totalTime.addAndGet(duration);

        if (duration > stat.maxTime.get()) {
            stat.maxTime.set(duration);
        }

        if (duration < stat.minTime.get() || stat.minTime.get() == 0) {
            stat.minTime.set(duration);
        }
    }

    /**
     * 获取操作统计
     *
     * @param operation 操作名称
     * @return 统计信息，如果不存在返回 null
     */
    public OperationStats getStats(String operation) {
        return stats.get(operation);
    }

    /**
     * 获取所有统计
     *
     * @return 所有操作的统计信息
     */
    public Map<String, OperationStats> getAllStats() {
        return new ConcurrentHashMap<>(stats);
    }

    /**
     * 清空所有统计
     */
    public void clear() {
        stats.clear();
    }

    /**
     * 设置性能阈值
     *
     * @param operation 操作名称
     * @param threshold 阈值（毫秒）
     */
    public void setThreshold(String operation, long threshold) {
        switch (operation) {
            case "completion":
                this.completionThreshold = threshold;
                break;
            case "syntaxCheck":
                this.syntaxCheckThreshold = threshold;
                break;
            case "definition":
                this.definitionThreshold = threshold;
                break;
            case "reference":
                this.referenceThreshold = threshold;
                break;
        }
    }

    /**
     * 检查操作是否超时
     *
     * @param operation 操作名称
     * @param startTime 开始时间戳
     * @return 是否超时
     */
    public boolean isTimeout(String operation, long startTime) {
        long threshold = getThreshold(operation);
        return (System.currentTimeMillis() - startTime) > threshold;
    }

    /**
     * 获取性能阈值
     *
     * @param operation 操作名称
     * @return 阈值（毫秒）
     */
    public long getThreshold(String operation) {
        switch (operation) {
            case "completion":
                return completionThreshold;
            case "syntaxCheck":
                return syntaxCheckThreshold;
            case "definition":
                return definitionThreshold;
            case "reference":
                return referenceThreshold;
            default:
                return 1000;
        }
    }

    /**
     * 操作统计类
     */
    public static class OperationStats {
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong maxTime = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        private final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();

        public long getTotalCalls() {
            return totalCalls.get();
        }

        public long getTotalTime() {
            return totalTime.get();
        }

        public long getMaxTime() {
            return maxTime.get();
        }

        public long getMinTime() {
            long min = minTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }

        public double getAverageTime() {
            long calls = totalCalls.get();
            return calls == 0 ? 0 : (double) totalTime.get() / calls;
        }

        public long getLatencyCount() {
            return latencies.size();
        }

        public void addLatency(long latency) {
            latencies.add(latency);
        }

        public String getSummary() {
            long calls = getTotalCalls();
            if (calls == 0) {
                return "No calls yet";
            }

            return String.format(
                "Total: %d calls, Avg: %.2fms, Max: %dms, Min: %dms",
                calls,
                getAverageTime(),
                getMaxTime(),
                getMinTime()
            );
        }
    }

    /**
     * 获取性能报告
     *
     * @return 性能报告字符串
     */
    public String getPerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== LSP Performance Report ===\n\n");

        for (Map.Entry<String, OperationStats> entry : stats.entrySet()) {
            OperationStats stat = entry.getValue();
            report.append(String.format("%s: %s\n", entry.getKey(), stat.getSummary()));
        }

        return report.toString();
    }
}

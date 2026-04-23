package com.q3lives.compiler.performance;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控器 - 用于编译器各阶段的性能分析
 */
@Slf4j
public class PerformanceMonitor {

    // 存储各阶段的耗时统计
    private final ConcurrentHashMap<String, AtomicLong> phaseTimings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> phaseCounts = new ConcurrentHashMap<>();

    // 线程安全的计时器上下文
    public static class TimerContext {
        private final String phaseName;
        private final long startTime;

        public TimerContext(String phaseName) {
            this.phaseName = phaseName;
            this.startTime = System.nanoTime();
        }

        public long elapsedNanos() {
            return System.nanoTime() - startTime;
        }

        public long elapsedMillis() {
            return (System.nanoTime() - startTime) / 1_000_000;
        }
    }

    /**
     * 开始计时
     */
    public TimerContext startTimer(String phaseName) {
        phaseCounts.computeIfAbsent(phaseName, k -> new AtomicLong(0)).incrementAndGet();
        return new TimerContext(phaseName);
    }

    /**
     * 结束计时并记录耗时
     */
    public void endTimer(TimerContext context) {
        long elapsedNanos = context.elapsedNanos();
        phaseTimings.computeIfAbsent(context.phaseName, k -> new AtomicLong(0))
                   .addAndGet(elapsedNanos);

        log.debug("{} 耗时: {}ms", context.phaseName, context.elapsedMillis());
    }

    /**
     * 获取平均耗时（毫秒）
     */
    public double getAverageTimeMillis(String phaseName) {
        AtomicLong totalTime = phaseTimings.get(phaseName);
        AtomicLong count = phaseCounts.get(phaseName);

        if (totalTime == null || count == null || count.get() == 0) {
            return 0.0;
        }

        return (totalTime.get() / 1_000_000.0) / count.get();
    }

    /**
     * 获取总耗时（毫秒）
     */
    public long getTotalTimeMillis(String phaseName) {
        AtomicLong totalTime = phaseTimings.get(phaseName);
        return totalTime == null ? 0 : totalTime.get() / 1_000_000;
    }

    /**
     * 获取调用次数
     */
    public long getCallCount(String phaseName) {
        AtomicLong count = phaseCounts.get(phaseName);
        return count == null ? 0 : count.get();
    }

    /**
     * 打印性能统计报告
     */
    public void printReport() {
        log.info("===== 编译器性能统计 =====");
        for (String phaseName : phaseTimings.keySet()) {
            double avgTime = getAverageTimeMillis(phaseName);
            long totalTime = getTotalTimeMillis(phaseName);
            long count = getCallCount(phaseName);

            log.info("{} - 平均: {:.2f}ms, 总计: {}ms, 次数: {}",
                    phaseName, avgTime, totalTime, count);
        }
        log.info("==========================");
    }

    /**
     * 重置统计信息
     */
    public void reset() {
        phaseTimings.clear();
        phaseCounts.clear();
    }

    /**
     * 获取编译器吞吐量（函数/秒）
     */
    public double getThroughput() {
        long totalTime = getTotalTimeMillis("all");
        return totalTime > 0 ? (getCallCount("all") * 1000.0) / totalTime : 0;
    }
}
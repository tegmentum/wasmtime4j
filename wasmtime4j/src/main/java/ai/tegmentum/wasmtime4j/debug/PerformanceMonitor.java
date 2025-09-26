package ai.tegmentum.wasmtime4j.debug;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Performance monitoring for multi-target debugging operations.
 *
 * <p>This class collects and aggregates performance metrics from debug targets,
 * providing insights into debugging overhead and system resource usage.
 *
 * @since 1.0.0
 */
final class PerformanceMonitor {

    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;
    private final Map<String, AtomicLong> operationCounters;
    private final Map<String, Long> debugOverheadTracking;

    PerformanceMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.operationCounters = new ConcurrentHashMap<>();
        this.debugOverheadTracking = new ConcurrentHashMap<>();
    }

    /**
     * Collects performance metrics from all debug targets.
     *
     * @param targets the collection of debug targets
     * @return aggregated performance metrics
     */
    MultiTargetPerformanceMetrics collectMetrics(final Collection<DebugTarget> targets) {
        final long collectionTime = System.currentTimeMillis();
        final int totalTargets = targets.size();
        final int activeTargets = (int) targets.stream()
            .filter(DebugTarget::isAlive)
            .count();

        // Collect individual target metrics
        final List<MultiTargetPerformanceMetrics.TargetPerformanceMetrics> targetMetrics = targets.stream()
            .map(this::collectTargetMetrics)
            .collect(Collectors.toList());

        // Aggregate metrics
        final long totalMemoryUsage = targetMetrics.stream()
            .mapToLong(MultiTargetPerformanceMetrics.TargetPerformanceMetrics::memoryUsage)
            .sum();

        final long averageMemoryUsage = totalTargets > 0 ? totalMemoryUsage / totalTargets : 0;

        final double totalCpuUsage = targetMetrics.stream()
            .mapToDouble(MultiTargetPerformanceMetrics.TargetPerformanceMetrics::cpuUsage)
            .sum();

        final double averageCpuUsage = totalTargets > 0 ? totalCpuUsage / totalTargets : 0.0;

        final long totalDebugOverhead = targetMetrics.stream()
            .mapToLong(MultiTargetPerformanceMetrics.TargetPerformanceMetrics::debugOverhead)
            .sum();

        final long averageDebugOverhead = totalTargets > 0 ? totalDebugOverhead / totalTargets : 0;

        // Copy operation counters
        final Map<String, Long> operationCounts = new HashMap<>();
        operationCounters.forEach((key, value) -> operationCounts.put(key, value.get()));

        return new MultiTargetPerformanceMetrics(
            totalTargets,
            activeTargets,
            totalMemoryUsage,
            averageMemoryUsage,
            totalCpuUsage,
            averageCpuUsage,
            totalDebugOverhead,
            averageDebugOverhead,
            operationCounts,
            targetMetrics,
            collectionTime
        );
    }

    /**
     * Records an operation being performed.
     *
     * @param operationType the type of operation
     */
    void recordOperation(final String operationType) {
        operationCounters.computeIfAbsent(operationType, k -> new AtomicLong(0))
            .incrementAndGet();
    }

    /**
     * Records debugging overhead for a target.
     *
     * @param targetId the target ID
     * @param overheadMicros the overhead in microseconds
     */
    void recordDebugOverhead(final String targetId, final long overheadMicros) {
        debugOverheadTracking.compute(targetId, (key, current) -> {
            return current != null ? current + overheadMicros : overheadMicros;
        });
    }

    private MultiTargetPerformanceMetrics.TargetPerformanceMetrics collectTargetMetrics(final DebugTarget target) {
        // Estimate memory usage for the target
        // In a real implementation, this would involve more sophisticated monitoring
        final long memoryUsage = estimateTargetMemoryUsage(target);

        // Estimate CPU usage for the target
        final double cpuUsage = estimateTargetCpuUsage(target);

        // Get debug overhead
        final long debugOverhead = debugOverheadTracking.getOrDefault(target.getTargetId(), 0L);

        // Count operations (simplified - would track per target in real implementation)
        final long operationCount = operationCounters.values().stream()
            .mapToLong(AtomicLong::get)
            .sum() / Math.max(1, debugOverheadTracking.size());

        return new MultiTargetPerformanceMetrics.TargetPerformanceMetrics(
            target.getTargetId(),
            memoryUsage,
            cpuUsage,
            debugOverhead,
            operationCount,
            target.isAlive()
        );
    }

    private long estimateTargetMemoryUsage(final DebugTarget target) {
        // Simplified memory usage estimation
        // In a real implementation, this would involve more sophisticated monitoring
        final long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        final long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();

        // Rough estimate: divide total JVM memory usage by number of targets
        // Plus some base overhead per target
        final long totalMemory = heapUsed + nonHeapUsed;
        final long baseOverhead = 1024 * 1024; // 1MB base overhead per target

        return (totalMemory / Math.max(1, debugOverheadTracking.size())) + baseOverhead;
    }

    private double estimateTargetCpuUsage(final DebugTarget target) {
        // Simplified CPU usage estimation
        // In a real implementation, this would involve per-target CPU monitoring
        double systemCpuLoad = osBean.getSystemCpuLoad();
        if (systemCpuLoad < 0) {
            systemCpuLoad = 0.0; // Not available
        }

        // Rough estimate: divide system CPU usage by number of active targets
        final int activeTargets = debugOverheadTracking.size();
        return activeTargets > 0 ? (systemCpuLoad * 100.0) / activeTargets : 0.0;
    }
}
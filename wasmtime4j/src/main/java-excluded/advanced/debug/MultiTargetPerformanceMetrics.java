package ai.tegmentum.wasmtime4j.debug;

import java.util.List;
import java.util.Map;

/**
 * Performance metrics for multi-target debugging operations.
 *
 * <p>This record contains aggregated performance data from all debug targets,
 * providing insights into system resource usage and debugging overhead.
 *
 * @param totalTargets the total number of active targets
 * @param activeTargets the number of currently active targets
 * @param totalMemoryUsage the total memory usage across all targets in bytes
 * @param averageMemoryUsage the average memory usage per target in bytes
 * @param totalCpuUsage the total CPU usage percentage (0.0 to 100.0)
 * @param averageCpuUsage the average CPU usage per target percentage
 * @param totalDebugOverhead the total debugging overhead in microseconds
 * @param averageDebugOverhead the average debugging overhead per target in microseconds
 * @param operationCounts the count of different operations performed
 * @param targetMetrics the individual target metrics
 * @param collectionTime the time when metrics were collected
 *
 * @since 1.0.0
 */
public record MultiTargetPerformanceMetrics(
        int totalTargets,
        int activeTargets,
        long totalMemoryUsage,
        long averageMemoryUsage,
        double totalCpuUsage,
        double averageCpuUsage,
        long totalDebugOverhead,
        long averageDebugOverhead,
        Map<String, Long> operationCounts,
        List<TargetPerformanceMetrics> targetMetrics,
        long collectionTime
) {

    /**
     * Gets the percentage of active targets.
     *
     * @return the active target percentage (0.0 to 1.0)
     */
    public double getActiveTargetPercentage() {
        return totalTargets > 0 ? (double) activeTargets / totalTargets : 0.0;
    }

    /**
     * Gets the total operation count across all operations.
     *
     * @return the total operation count
     */
    public long getTotalOperationCount() {
        return operationCounts.values().stream()
            .mapToLong(Long::longValue)
            .sum();
    }

    /**
     * Gets the debugging efficiency ratio.
     * Higher values indicate more efficient debugging.
     *
     * @return the efficiency ratio
     */
    public double getDebuggingEfficiencyRatio() {
        if (totalDebugOverhead == 0) {
            return Double.MAX_VALUE;
        }
        return (double) getTotalOperationCount() / totalDebugOverhead;
    }

    /**
     * Checks if the system is under high load.
     *
     * @return true if system is under high load
     */
    public boolean isHighLoad() {
        return totalCpuUsage > 80.0 || averageMemoryUsage > 100 * 1024 * 1024; // 100MB
    }

    /**
     * Gets memory usage formatted as a human-readable string.
     *
     * @return formatted memory usage
     */
    public String getFormattedMemoryUsage() {
        return formatBytes(totalMemoryUsage) + " total, " + formatBytes(averageMemoryUsage) + " avg";
    }

    /**
     * Gets CPU usage formatted as a percentage string.
     *
     * @return formatted CPU usage
     */
    public String getFormattedCpuUsage() {
        return String.format("%.1f%% total, %.1f%% avg", totalCpuUsage, averageCpuUsage);
    }

    /**
     * Gets debug overhead formatted as a time string.
     *
     * @return formatted debug overhead
     */
    public String getFormattedDebugOverhead() {
        return formatMicroseconds(totalDebugOverhead) + " total, " + formatMicroseconds(averageDebugOverhead) + " avg";
    }

    /**
     * Creates a summary of the performance metrics.
     *
     * @return performance summary
     */
    public PerformanceSummary getSummary() {
        return new PerformanceSummary(
            totalTargets,
            activeTargets,
            getFormattedMemoryUsage(),
            getFormattedCpuUsage(),
            getFormattedDebugOverhead(),
            getTotalOperationCount(),
            getDebuggingEfficiencyRatio(),
            isHighLoad()
        );
    }

    /**
     * Formats the metrics as a detailed string.
     *
     * @return formatted metrics
     */
    public String format() {
        return String.format(
            "Multi-Target Performance: %d targets (%d active), %s memory, %s CPU, %s overhead, %d ops, %.2f efficiency%s",
            totalTargets,
            activeTargets,
            getFormattedMemoryUsage(),
            getFormattedCpuUsage(),
            getFormattedDebugOverhead(),
            getTotalOperationCount(),
            getDebuggingEfficiencyRatio(),
            isHighLoad() ? " [HIGH LOAD]" : ""
        );
    }

    private static String formatBytes(final long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private static String formatMicroseconds(final long microseconds) {
        if (microseconds < 1000) {
            return microseconds + " μs";
        } else if (microseconds < 1000000) {
            return String.format("%.1f ms", microseconds / 1000.0);
        } else {
            return String.format("%.1f s", microseconds / 1000000.0);
        }
    }

    /**
     * Performance summary record for easier consumption.
     *
     * @param totalTargets the total number of targets
     * @param activeTargets the number of active targets
     * @param memoryUsage the formatted memory usage
     * @param cpuUsage the formatted CPU usage
     * @param debugOverhead the formatted debug overhead
     * @param totalOperations the total operation count
     * @param efficiencyRatio the efficiency ratio
     * @param highLoad whether system is under high load
     */
    public record PerformanceSummary(
            int totalTargets,
            int activeTargets,
            String memoryUsage,
            String cpuUsage,
            String debugOverhead,
            long totalOperations,
            double efficiencyRatio,
            boolean highLoad
    ) {
        /**
         * Formats the summary as a concise string.
         *
         * @return formatted summary
         */
        public String format() {
            return String.format(
                "%d/%d targets, %s mem, %s CPU, %d ops%s",
                activeTargets,
                totalTargets,
                memoryUsage,
                cpuUsage,
                totalOperations,
                highLoad ? " [HIGH LOAD]" : ""
            );
        }
    }

    /**
     * Individual target performance metrics.
     *
     * @param targetId the target identifier
     * @param memoryUsage the memory usage in bytes
     * @param cpuUsage the CPU usage percentage
     * @param debugOverhead the debugging overhead in microseconds
     * @param operationCount the number of operations performed
     * @param isActive whether the target is active
     */
    public record TargetPerformanceMetrics(
            String targetId,
            long memoryUsage,
            double cpuUsage,
            long debugOverhead,
            long operationCount,
            boolean isActive
    ) {
        /**
         * Formats the target metrics as a string.
         *
         * @return formatted target metrics
         */
        public String format() {
            return String.format(
                "%s: %s mem, %.1f%% CPU, %s overhead, %d ops%s",
                targetId,
                formatBytes(memoryUsage),
                cpuUsage,
                formatMicroseconds(debugOverhead),
                operationCount,
                isActive ? "" : " [INACTIVE]"
            );
        }
    }
}
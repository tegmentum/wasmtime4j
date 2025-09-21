package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.Map;
import java.util.Optional;

/**
 * Runtime metrics collected at the time of an error.
 *
 * <p>This interface provides comprehensive runtime metrics that help diagnose
 * performance issues, resource usage, and execution characteristics when errors occur.
 *
 * @since 1.0.0
 */
public interface RuntimeMetrics {

    /**
     * Gets the total execution time in milliseconds.
     *
     * @return the execution time, or empty if not measured
     */
    Optional<Long> getExecutionTimeMs();

    /**
     * Gets the compilation time in milliseconds.
     *
     * @return the compilation time, or empty if not measured
     */
    Optional<Long> getCompilationTimeMs();

    /**
     * Gets the instantiation time in milliseconds.
     *
     * @return the instantiation time, or empty if not measured
     */
    Optional<Long> getInstantiationTimeMs();

    /**
     * Gets the current memory usage in bytes.
     *
     * @return the memory usage, or empty if not measured
     */
    Optional<Long> getMemoryUsageBytes();

    /**
     * Gets the peak memory usage in bytes.
     *
     * @return the peak memory usage, or empty if not measured
     */
    Optional<Long> getPeakMemoryUsageBytes();

    /**
     * Gets the WebAssembly linear memory usage in bytes.
     *
     * @return the linear memory usage, or empty if not available
     */
    Optional<Long> getLinearMemoryUsageBytes();

    /**
     * Gets the number of function calls executed.
     *
     * @return the function call count, or empty if not measured
     */
    Optional<Long> getFunctionCallCount();

    /**
     * Gets the number of instructions executed.
     *
     * @return the instruction count, or empty if not measured
     */
    Optional<Long> getInstructionCount();

    /**
     * Gets the number of host function calls.
     *
     * @return the host function call count, or empty if not measured
     */
    Optional<Long> getHostFunctionCallCount();

    /**
     * Gets the fuel consumed during execution.
     *
     * @return the fuel consumed, or empty if fuel tracking is not enabled
     */
    Optional<Long> getFuelConsumed();

    /**
     * Gets the remaining fuel available.
     *
     * @return the remaining fuel, or empty if fuel tracking is not enabled
     */
    Optional<Long> getFuelRemaining();

    /**
     * Gets the number of epoch interruptions.
     *
     * @return the epoch interruption count, or empty if epoch tracking is not enabled
     */
    Optional<Long> getEpochInterruptions();

    /**
     * Gets the number of WebAssembly traps encountered.
     *
     * @return the trap count, or empty if not measured
     */
    Optional<Long> getTrapCount();

    /**
     * Gets the garbage collection statistics.
     *
     * @return the GC statistics, or empty if not available
     */
    Optional<GcStatistics> getGcStatistics();

    /**
     * Gets the JIT compilation statistics.
     *
     * @return the JIT statistics, or empty if not available
     */
    Optional<JitStatistics> getJitStatistics();

    /**
     * Gets the thread pool statistics.
     *
     * @return the thread pool statistics, or empty if not available
     */
    Optional<ThreadPoolStatistics> getThreadPoolStatistics();

    /**
     * Gets CPU usage percentage at the time of metrics collection.
     *
     * @return the CPU usage percentage (0-100), or empty if not measured
     */
    Optional<Double> getCpuUsagePercentage();

    /**
     * Gets the system load average.
     *
     * @return the load average, or empty if not available
     */
    Optional<Double> getSystemLoadAverage();

    /**
     * Gets additional runtime properties.
     *
     * @return map of runtime properties
     */
    Map<String, Object> getProperties();

    /**
     * Gets the metrics collection timestamp.
     *
     * @return the collection timestamp
     */
    long getCollectionTimestamp();

    /**
     * Gets the metrics collection duration in milliseconds.
     *
     * @return the collection duration, or empty if not measured
     */
    Optional<Long> getCollectionDurationMs();

    /**
     * Checks if the metrics are considered healthy.
     *
     * @return true if the metrics indicate healthy operation
     */
    boolean isHealthy();

    /**
     * Gets performance warnings based on the metrics.
     *
     * @return list of performance warnings
     */
    java.util.List<String> getPerformanceWarnings();

    /**
     * Gets resource utilization as a percentage (0-100).
     *
     * @return the resource utilization percentage
     */
    Optional<Double> getResourceUtilizationPercentage();

    /**
     * Creates a builder for constructing RuntimeMetrics instances.
     *
     * @return a new runtime metrics builder
     */
    static RuntimeMetricsBuilder builder() {
        return new RuntimeMetricsBuilder();
    }

    /**
     * Creates a snapshot of current runtime metrics.
     *
     * @return the current runtime metrics
     */
    static RuntimeMetrics snapshot() {
        return builder()
            .collectionTimestamp(System.currentTimeMillis())
            .build();
    }
}
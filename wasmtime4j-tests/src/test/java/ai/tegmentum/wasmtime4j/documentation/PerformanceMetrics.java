/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.time.Duration;
import java.util.Map;

/**
 * Performance metrics collected during test execution.
 *
 * <p>Provides detailed performance data including execution times,
 * memory usage, and throughput measurements.
 *
 * @since 1.0.0
 */
public interface PerformanceMetrics {

    /**
     * Returns average execution time for test methods.
     *
     * @return average execution duration
     */
    Duration getAverageExecutionTime();

    /**
     * Returns maximum execution time recorded.
     *
     * @return maximum execution duration
     */
    Duration getMaxExecutionTime();

    /**
     * Returns minimum execution time recorded.
     *
     * @return minimum execution duration
     */
    Duration getMinExecutionTime();

    /**
     * Returns execution times by test method.
     *
     * @return immutable map of method names to execution durations
     */
    Map<String, Duration> getExecutionTimesByMethod();

    /**
     * Returns peak memory usage during test execution.
     *
     * @return peak memory usage in bytes
     */
    long getPeakMemoryUsage();

    /**
     * Returns average memory usage during test execution.
     *
     * @return average memory usage in bytes
     */
    long getAverageMemoryUsage();

    /**
     * Returns memory usage by test method.
     *
     * @return immutable map of method names to memory usage in bytes
     */
    Map<String, Long> getMemoryUsageByMethod();

    /**
     * Returns throughput measurements for relevant operations.
     *
     * @return immutable map of operation names to throughput (operations per second)
     */
    Map<String, Double> getThroughputMetrics();

    /**
     * Returns garbage collection statistics during test execution.
     *
     * @return GC statistics summary
     */
    GcStatistics getGcStatistics();

    /**
     * Returns human-readable performance summary.
     *
     * @return formatted performance summary
     */
    String getSummary();

    /**
     * Garbage collection statistics.
     */
    interface GcStatistics {

        /**
         * Returns the number of GC collections.
         *
         * @return GC collection count
         */
        long getCollectionCount();

        /**
         * Returns total time spent in GC.
         *
         * @return GC time duration
         */
        Duration getCollectionTime();

        /**
         * Returns memory freed by GC.
         *
         * @return freed memory in bytes
         */
        long getFreedMemory();
    }
}
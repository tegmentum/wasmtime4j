package ai.tegmentum.wasmtime4j;

/**
 * Component metrics interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentMetrics {

  /**
   * Gets the component ID.
   *
   * @return component ID
   */
  String getComponentId();

  /**
   * Gets execution metrics.
   *
   * @return execution metrics
   */
  ExecutionMetrics getExecutionMetrics();

  /**
   * Gets memory metrics.
   *
   * @return memory metrics
   */
  MemoryMetrics getMemoryMetrics();

  /**
   * Gets performance metrics.
   *
   * @return performance metrics
   */
  PerformanceMetrics getPerformanceMetrics();

  /**
   * Gets resource metrics.
   *
   * @return resource metrics
   */
  ResourceMetrics getResourceMetrics();

  /**
   * Gets error metrics.
   *
   * @return error metrics
   */
  ErrorMetrics getErrorMetrics();

  /**
   * Gets the metrics collection start time.
   *
   * @return start timestamp
   */
  long getStartTime();

  /**
   * Gets the metrics collection end time.
   *
   * @return end timestamp
   */
  long getEndTime();

  /** Resets all metrics. */
  void reset();

  /**
   * Takes a snapshot of current metrics.
   *
   * @return metrics snapshot
   */
  MetricsSnapshot snapshot();

  /** Execution metrics interface. */
  interface ExecutionMetrics {
    /**
     * Gets total execution count.
     *
     * @return execution count
     */
    long getExecutionCount();

    /**
     * Gets successful execution count.
     *
     * @return successful count
     */
    long getSuccessfulExecutions();

    /**
     * Gets failed execution count.
     *
     * @return failed count
     */
    long getFailedExecutions();

    /**
     * Gets average execution time.
     *
     * @return average time in nanoseconds
     */
    double getAverageExecutionTime();

    /**
     * Gets minimum execution time.
     *
     * @return minimum time in nanoseconds
     */
    long getMinExecutionTime();

    /**
     * Gets maximum execution time.
     *
     * @return maximum time in nanoseconds
     */
    long getMaxExecutionTime();

    /**
     * Gets total execution time.
     *
     * @return total time in nanoseconds
     */
    long getTotalExecutionTime();

    /**
     * Gets execution rate (executions per second).
     *
     * @return execution rate
     */
    double getExecutionRate();
  }

  /** Memory metrics interface. */
  interface MemoryMetrics {
    /**
     * Gets current memory usage.
     *
     * @return current usage in bytes
     */
    long getCurrentMemoryUsage();

    /**
     * Gets peak memory usage.
     *
     * @return peak usage in bytes
     */
    long getPeakMemoryUsage();

    /**
     * Gets average memory usage.
     *
     * @return average usage in bytes
     */
    double getAverageMemoryUsage();

    /**
     * Gets total memory allocations.
     *
     * @return allocation count
     */
    long getTotalAllocations();

    /**
     * Gets total memory allocated.
     *
     * @return allocated memory in bytes
     */
    long getTotalAllocatedMemory();

    /**
     * Gets memory allocation rate.
     *
     * @return allocations per second
     */
    double getAllocationRate();

    /**
     * Gets garbage collection count.
     *
     * @return GC count
     */
    int getGcCount();

    /**
     * Gets total garbage collection time.
     *
     * @return GC time in milliseconds
     */
    long getGcTime();
  }

  /** Performance metrics interface. */
  interface PerformanceMetrics {
    /**
     * Gets instructions per second.
     *
     * @return instructions per second
     */
    double getInstructionsPerSecond();

    /**
     * Gets function calls per second.
     *
     * @return function calls per second
     */
    double getFunctionCallsPerSecond();

    /**
     * Gets throughput (operations per second).
     *
     * @return throughput
     */
    double getThroughput();

    /**
     * Gets average latency.
     *
     * @return latency in microseconds
     */
    double getAverageLatency();

    /**
     * Gets 95th percentile latency.
     *
     * @return P95 latency in microseconds
     */
    double getP95Latency();

    /**
     * Gets 99th percentile latency.
     *
     * @return P99 latency in microseconds
     */
    double getP99Latency();

    /**
     * Gets CPU utilization.
     *
     * @return CPU utilization (0.0-1.0)
     */
    double getCpuUtilization();
  }

  /** Resource metrics interface. */
  interface ResourceMetrics {
    /**
     * Gets fuel consumption.
     *
     * @return total fuel consumed
     */
    long getFuelConsumed();

    /**
     * Gets fuel consumption rate.
     *
     * @return fuel per second
     */
    double getFuelConsumptionRate();

    /**
     * Gets thread count.
     *
     * @return active thread count
     */
    int getThreadCount();

    /**
     * Gets file descriptor count.
     *
     * @return FD count
     */
    int getFileDescriptorCount();

    /**
     * Gets network connection count.
     *
     * @return connection count
     */
    int getNetworkConnectionCount();

    /**
     * Gets resource quota usage.
     *
     * @return quota usage statistics
     */
    QuotaUsageMetrics getQuotaUsage();
  }

  /** Error metrics interface. */
  interface ErrorMetrics {
    /**
     * Gets total error count.
     *
     * @return error count
     */
    long getTotalErrors();

    /**
     * Gets error rate.
     *
     * @return errors per second
     */
    double getErrorRate();

    /**
     * Gets error distribution by type.
     *
     * @return error type distribution
     */
    java.util.Map<String, Long> getErrorDistribution();

    /**
     * Gets most common errors.
     *
     * @param limit number of top errors
     * @return list of common errors
     */
    java.util.List<ErrorInfo> getMostCommonErrors(int limit);

    /**
     * Gets critical error count.
     *
     * @return critical error count
     */
    long getCriticalErrors();

    /**
     * Gets recoverable error count.
     *
     * @return recoverable error count
     */
    long getRecoverableErrors();
  }

  /** Quota usage metrics interface. */
  interface QuotaUsageMetrics {
    /**
     * Gets fuel quota usage.
     *
     * @return fuel usage (0.0-1.0)
     */
    double getFuelUsage();

    /**
     * Gets memory quota usage.
     *
     * @return memory usage (0.0-1.0)
     */
    double getMemoryUsage();

    /**
     * Gets time quota usage.
     *
     * @return time usage (0.0-1.0)
     */
    double getTimeUsage();

    /**
     * Gets instruction quota usage.
     *
     * @return instruction usage (0.0-1.0)
     */
    double getInstructionUsage();
  }

  /** Error information interface. */
  interface ErrorInfo {
    /**
     * Gets error type.
     *
     * @return error type
     */
    String getType();

    /**
     * Gets error count.
     *
     * @return error count
     */
    long getCount();

    /**
     * Gets error percentage.
     *
     * @return error percentage (0.0-1.0)
     */
    double getPercentage();

    /**
     * Gets last occurrence time.
     *
     * @return last occurrence timestamp
     */
    long getLastOccurrence();

    /**
     * Gets sample error message.
     *
     * @return sample message
     */
    String getSampleMessage();
  }

  /** Metrics snapshot interface. */
  interface MetricsSnapshot {
    /**
     * Gets snapshot timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Gets execution metrics.
     *
     * @return execution metrics
     */
    ExecutionMetrics getExecutionMetrics();

    /**
     * Gets memory metrics.
     *
     * @return memory metrics
     */
    MemoryMetrics getMemoryMetrics();

    /**
     * Gets performance metrics.
     *
     * @return performance metrics
     */
    PerformanceMetrics getPerformanceMetrics();

    /**
     * Gets resource metrics.
     *
     * @return resource metrics
     */
    ResourceMetrics getResourceMetrics();

    /**
     * Gets error metrics.
     *
     * @return error metrics
     */
    ErrorMetrics getErrorMetrics();

    /**
     * Exports snapshot data.
     *
     * @param format export format
     * @return exported data
     */
    byte[] export(ExportFormat format);
  }

  /** Export format enumeration. */
  enum ExportFormat {
    /** JSON format. */
    JSON,
    /** CSV format. */
    CSV,
    /** Binary format. */
    BINARY,
    /** Prometheus format. */
    PROMETHEUS
  }
}

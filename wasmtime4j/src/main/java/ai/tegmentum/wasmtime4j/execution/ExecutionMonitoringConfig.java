package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution monitoring configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionMonitoringConfig {

  /**
   * Checks if monitoring is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Sets monitoring enabled state.
   *
   * @param enabled enabled state
   */
  void setEnabled(boolean enabled);

  /**
   * Gets the sampling interval in milliseconds.
   *
   * @return sampling interval
   */
  long getSamplingInterval();

  /**
   * Sets the sampling interval.
   *
   * @param intervalMs sampling interval in milliseconds
   */
  void setSamplingInterval(long intervalMs);

  /**
   * Gets monitored metrics.
   *
   * @return set of monitored metrics
   */
  java.util.Set<MonitoredMetric> getMonitoredMetrics();

  /**
   * Adds a metric to monitor.
   *
   * @param metric metric to monitor
   */
  void addMonitoredMetric(MonitoredMetric metric);

  /**
   * Removes a metric from monitoring.
   *
   * @param metric metric to remove
   */
  void removeMonitoredMetric(MonitoredMetric metric);

  /**
   * Gets the monitoring output format.
   *
   * @return output format
   */
  OutputFormat getOutputFormat();

  /**
   * Sets the monitoring output format.
   *
   * @param format output format
   */
  void setOutputFormat(OutputFormat format);

  /**
   * Gets the monitoring output destination.
   *
   * @return output destination
   */
  String getOutputDestination();

  /**
   * Sets the monitoring output destination.
   *
   * @param destination output destination
   */
  void setOutputDestination(String destination);

  /**
   * Gets alert thresholds.
   *
   * @return alert thresholds
   */
  AlertThresholds getAlertThresholds();

  /**
   * Sets alert thresholds.
   *
   * @param thresholds alert thresholds
   */
  void setAlertThresholds(AlertThresholds thresholds);

  /** Monitored metric enumeration. */
  enum MonitoredMetric {
    /** CPU usage. */
    CPU_USAGE,
    /** Memory usage. */
    MEMORY_USAGE,
    /** Instruction count. */
    INSTRUCTION_COUNT,
    /** Function calls. */
    FUNCTION_CALLS,
    /** Execution time. */
    EXECUTION_TIME,
    /** Garbage collection. */
    GARBAGE_COLLECTION,
    /** Thread count. */
    THREAD_COUNT
  }

  /** Output format enumeration. */
  enum OutputFormat {
    /** JSON format. */
    JSON,
    /** CSV format. */
    CSV,
    /** Plain text. */
    TEXT,
    /** Binary format. */
    BINARY
  }

  /** Alert thresholds interface. */
  interface AlertThresholds {
    /**
     * Gets the CPU usage threshold.
     *
     * @return CPU usage threshold (0.0-1.0)
     */
    double getCpuUsageThreshold();

    /**
     * Gets the memory usage threshold.
     *
     * @return memory usage threshold (0.0-1.0)
     */
    double getMemoryUsageThreshold();

    /**
     * Gets the execution time threshold in milliseconds.
     *
     * @return execution time threshold
     */
    long getExecutionTimeThreshold();

    /**
     * Gets the error rate threshold.
     *
     * @return error rate threshold (0.0-1.0)
     */
    double getErrorRateThreshold();
  }
}

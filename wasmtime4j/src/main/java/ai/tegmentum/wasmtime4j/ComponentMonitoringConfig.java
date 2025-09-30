package ai.tegmentum.wasmtime4j;

/**
 * Component monitoring configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentMonitoringConfig {

  /**
   * Checks if monitoring is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Gets monitored metrics.
   *
   * @return set of monitored metrics
   */
  java.util.Set<MonitoredMetric> getMetrics();

  /**
   * Gets monitoring interval.
   *
   * @return interval in milliseconds
   */
  long getInterval();

  /**
   * Gets alert thresholds.
   *
   * @return alert thresholds
   */
  AlertThresholds getThresholds();

  /**
   * Gets monitoring destinations.
   *
   * @return destinations
   */
  java.util.List<String> getDestinations();

  /** Alert thresholds interface. */
  interface AlertThresholds {
    /**
     * Gets memory usage threshold.
     *
     * @return threshold (0.0-1.0)
     */
    double getMemoryThreshold();

    /**
     * Gets CPU usage threshold.
     *
     * @return threshold (0.0-1.0)
     */
    double getCpuThreshold();

    /**
     * Gets error rate threshold.
     *
     * @return threshold (0.0-1.0)
     */
    double getErrorRateThreshold();
  }

  /** Monitored metric enumeration. */
  enum MonitoredMetric {
    /** Memory usage. */
    MEMORY_USAGE,
    /** CPU usage. */
    CPU_USAGE,
    /** Execution time. */
    EXECUTION_TIME,
    /** Error rate. */
    ERROR_RATE,
    /** Throughput. */
    THROUGHPUT
  }
}

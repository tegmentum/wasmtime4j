package ai.tegmentum.wasmtime4j.execution;

/**
 * Load-based quota configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface LoadBasedQuotaConfig {

  /**
   * Checks if load-based quotas are enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Sets load-based quotas enabled state.
   *
   * @param enabled enabled state
   */
  void setEnabled(boolean enabled);

  /**
   * Gets the load monitoring strategy.
   *
   * @return load monitoring strategy
   */
  LoadMonitoringStrategy getMonitoringStrategy();

  /**
   * Sets the load monitoring strategy.
   *
   * @param strategy monitoring strategy
   */
  void setMonitoringStrategy(LoadMonitoringStrategy strategy);

  /**
   * Gets the quota adjustment strategy.
   *
   * @return adjustment strategy
   */
  QuotaAdjustmentStrategy getAdjustmentStrategy();

  /**
   * Sets the quota adjustment strategy.
   *
   * @param strategy adjustment strategy
   */
  void setAdjustmentStrategy(QuotaAdjustmentStrategy strategy);

  /**
   * Gets load thresholds.
   *
   * @return load thresholds
   */
  LoadThresholds getThresholds();

  /**
   * Sets load thresholds.
   *
   * @param thresholds load thresholds
   */
  void setThresholds(LoadThresholds thresholds);

  /**
   * Gets quota bounds.
   *
   * @return quota bounds
   */
  QuotaBounds getBounds();

  /**
   * Sets quota bounds.
   *
   * @param bounds quota bounds
   */
  void setBounds(QuotaBounds bounds);

  /**
   * Gets the monitoring interval in milliseconds.
   *
   * @return monitoring interval
   */
  long getMonitoringInterval();

  /**
   * Sets the monitoring interval.
   *
   * @param intervalMs monitoring interval in milliseconds
   */
  void setMonitoringInterval(long intervalMs);

  /**
   * Gets the adjustment smoothing factor.
   *
   * @return smoothing factor (0.0-1.0)
   */
  double getSmoothingFactor();

  /**
   * Sets the adjustment smoothing factor.
   *
   * @param factor smoothing factor (0.0-1.0)
   */
  void setSmoothingFactor(double factor);

  /**
   * Gets load metrics configuration.
   *
   * @return load metrics config
   */
  LoadMetricsConfig getMetricsConfig();

  /**
   * Sets load metrics configuration.
   *
   * @param config load metrics config
   */
  void setMetricsConfig(LoadMetricsConfig config);

  /** Load monitoring strategy enumeration. */
  enum LoadMonitoringStrategy {
    /** CPU-based monitoring. */
    CPU_BASED,
    /** Memory-based monitoring. */
    MEMORY_BASED,
    /** Throughput-based monitoring. */
    THROUGHPUT_BASED,
    /** Latency-based monitoring. */
    LATENCY_BASED,
    /** Combined metrics. */
    COMBINED,
    /** Custom monitoring. */
    CUSTOM
  }

  /** Quota adjustment strategy enumeration. */
  enum QuotaAdjustmentStrategy {
    /** Linear adjustment. */
    LINEAR,
    /** Exponential adjustment. */
    EXPONENTIAL,
    /** Step adjustment. */
    STEP,
    /** Proportional adjustment. */
    PROPORTIONAL,
    /** PID controller. */
    PID_CONTROLLER
  }

  /** Load thresholds interface. */
  interface LoadThresholds {
    /**
     * Gets the low load threshold.
     *
     * @return low threshold (0.0-1.0)
     */
    double getLowThreshold();

    /**
     * Gets the medium load threshold.
     *
     * @return medium threshold (0.0-1.0)
     */
    double getMediumThreshold();

    /**
     * Gets the high load threshold.
     *
     * @return high threshold (0.0-1.0)
     */
    double getHighThreshold();

    /**
     * Gets the critical load threshold.
     *
     * @return critical threshold (0.0-1.0)
     */
    double getCriticalThreshold();

    /**
     * Gets threshold hysteresis.
     *
     * @return hysteresis value
     */
    double getHysteresis();
  }

  /** Quota bounds interface. */
  interface QuotaBounds {
    /**
     * Gets minimum fuel quota.
     *
     * @return min fuel quota
     */
    long getMinFuelQuota();

    /**
     * Gets maximum fuel quota.
     *
     * @return max fuel quota
     */
    long getMaxFuelQuota();

    /**
     * Gets minimum memory quota.
     *
     * @return min memory quota in bytes
     */
    long getMinMemoryQuota();

    /**
     * Gets maximum memory quota.
     *
     * @return max memory quota in bytes
     */
    long getMaxMemoryQuota();

    /**
     * Gets minimum time quota.
     *
     * @return min time quota in milliseconds
     */
    long getMinTimeQuota();

    /**
     * Gets maximum time quota.
     *
     * @return max time quota in milliseconds
     */
    long getMaxTimeQuota();
  }

  /** Load metrics configuration interface. */
  interface LoadMetricsConfig {
    /**
     * Gets enabled load metrics.
     *
     * @return set of enabled metrics
     */
    java.util.Set<LoadMetric> getEnabledMetrics();

    /**
     * Gets metric weights.
     *
     * @return metric weight map
     */
    java.util.Map<LoadMetric, Double> getMetricWeights();

    /**
     * Gets metric collection interval.
     *
     * @return collection interval in milliseconds
     */
    long getCollectionInterval();

    /**
     * Gets metric smoothing window.
     *
     * @return smoothing window size
     */
    int getSmoothingWindow();

    /**
     * Gets outlier detection configuration.
     *
     * @return outlier detection config
     */
    OutlierDetectionConfig getOutlierDetection();
  }

  /** Outlier detection configuration interface. */
  interface OutlierDetectionConfig {
    /**
     * Checks if outlier detection is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets the detection method.
     *
     * @return detection method
     */
    OutlierDetectionMethod getMethod();

    /**
     * Gets the sensitivity threshold.
     *
     * @return sensitivity threshold
     */
    double getSensitivity();

    /**
     * Gets the outlier handling strategy.
     *
     * @return handling strategy
     */
    OutlierHandlingStrategy getHandlingStrategy();
  }

  /** Load metric enumeration. */
  enum LoadMetric {
    /** CPU utilization. */
    CPU_UTILIZATION,
    /** Memory utilization. */
    MEMORY_UTILIZATION,
    /** Request throughput. */
    REQUEST_THROUGHPUT,
    /** Average response time. */
    AVERAGE_RESPONSE_TIME,
    /** Error rate. */
    ERROR_RATE,
    /** Queue depth. */
    QUEUE_DEPTH,
    /** Active connections. */
    ACTIVE_CONNECTIONS
  }

  /** Outlier detection method enumeration. */
  enum OutlierDetectionMethod {
    /** Standard deviation based. */
    STANDARD_DEVIATION,
    /** Interquartile range. */
    INTERQUARTILE_RANGE,
    /** Z-score based. */
    Z_SCORE,
    /** Modified Z-score. */
    MODIFIED_Z_SCORE,
    /** Isolation forest. */
    ISOLATION_FOREST
  }

  /** Outlier handling strategy enumeration. */
  enum OutlierHandlingStrategy {
    /** Ignore outliers. */
    IGNORE,
    /** Replace with median. */
    REPLACE_WITH_MEDIAN,
    /** Replace with moving average. */
    REPLACE_WITH_MOVING_AVERAGE,
    /** Cap outliers. */
    CAP_OUTLIERS,
    /** Log and continue. */
    LOG_AND_CONTINUE
  }
}

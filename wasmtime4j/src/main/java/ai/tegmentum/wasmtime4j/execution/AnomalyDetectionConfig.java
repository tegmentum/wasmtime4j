package ai.tegmentum.wasmtime4j.execution;

/**
 * Anomaly detection configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface AnomalyDetectionConfig {

  /**
   * Checks if anomaly detection is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Sets anomaly detection enabled state.
   *
   * @param enabled enabled state
   */
  void setEnabled(boolean enabled);

  /**
   * Gets the detection algorithm.
   *
   * @return detection algorithm
   */
  DetectionAlgorithm getAlgorithm();

  /**
   * Sets the detection algorithm.
   *
   * @param algorithm detection algorithm
   */
  void setAlgorithm(DetectionAlgorithm algorithm);

  /**
   * Gets the sensitivity level.
   *
   * @return sensitivity level (0.0-1.0)
   */
  double getSensitivity();

  /**
   * Sets the sensitivity level.
   *
   * @param sensitivity sensitivity level (0.0-1.0)
   */
  void setSensitivity(double sensitivity);

  /**
   * Gets monitored metrics.
   *
   * @return set of monitored metrics
   */
  java.util.Set<AnomalyMetric> getMonitoredMetrics();

  /**
   * Gets detection thresholds.
   *
   * @return detection thresholds
   */
  DetectionThresholds getThresholds();

  /**
   * Sets detection thresholds.
   *
   * @param thresholds detection thresholds
   */
  void setThresholds(DetectionThresholds thresholds);

  /**
   * Gets the detection window size.
   *
   * @return window size
   */
  int getWindowSize();

  /**
   * Sets the detection window size.
   *
   * @param windowSize window size
   */
  void setWindowSize(int windowSize);

  /**
   * Gets anomaly handlers.
   *
   * @return list of anomaly handlers
   */
  java.util.List<AnomalyHandler> getHandlers();

  /**
   * Adds an anomaly handler.
   *
   * @param handler anomaly handler
   */
  void addHandler(AnomalyHandler handler);

  /**
   * Removes an anomaly handler.
   *
   * @param handler anomaly handler
   */
  void removeHandler(AnomalyHandler handler);

  /**
   * Gets baseline learning configuration.
   *
   * @return baseline learning config
   */
  BaselineLearningConfig getBaselineLearning();

  /**
   * Sets baseline learning configuration.
   *
   * @param config baseline learning config
   */
  void setBaselineLearning(BaselineLearningConfig config);

  /** Detection algorithm enumeration. */
  enum DetectionAlgorithm {
    /** Statistical threshold. */
    STATISTICAL_THRESHOLD,
    /** Moving average. */
    MOVING_AVERAGE,
    /** Exponential smoothing. */
    EXPONENTIAL_SMOOTHING,
    /** Isolation forest. */
    ISOLATION_FOREST,
    /** One-class SVM. */
    ONE_CLASS_SVM,
    /** Local outlier factor. */
    LOCAL_OUTLIER_FACTOR,
    /** Neural network. */
    NEURAL_NETWORK
  }

  /** Anomaly metric enumeration. */
  enum AnomalyMetric {
    /** Execution time. */
    EXECUTION_TIME,
    /** Memory usage. */
    MEMORY_USAGE,
    /** CPU usage. */
    CPU_USAGE,
    /** Error rate. */
    ERROR_RATE,
    /** Throughput. */
    THROUGHPUT,
    /** Fuel consumption. */
    FUEL_CONSUMPTION,
    /** Function call frequency. */
    FUNCTION_CALL_FREQUENCY,
    /** Custom metric. */
    CUSTOM
  }

  /** Detection thresholds interface. */
  interface DetectionThresholds {
    /**
     * Gets execution time threshold.
     *
     * @return execution time threshold
     */
    ThresholdConfig getExecutionTime();

    /**
     * Gets memory usage threshold.
     *
     * @return memory usage threshold
     */
    ThresholdConfig getMemoryUsage();

    /**
     * Gets CPU usage threshold.
     *
     * @return CPU usage threshold
     */
    ThresholdConfig getCpuUsage();

    /**
     * Gets error rate threshold.
     *
     * @return error rate threshold
     */
    ThresholdConfig getErrorRate();

    /**
     * Gets throughput threshold.
     *
     * @return throughput threshold
     */
    ThresholdConfig getThroughput();

    /**
     * Gets custom thresholds.
     *
     * @return custom threshold map
     */
    java.util.Map<String, ThresholdConfig> getCustomThresholds();
  }

  /** Threshold configuration interface. */
  interface ThresholdConfig {
    /**
     * Gets the lower threshold.
     *
     * @return lower threshold
     */
    double getLowerThreshold();

    /**
     * Gets the upper threshold.
     *
     * @return upper threshold
     */
    double getUpperThreshold();

    /**
     * Gets the threshold type.
     *
     * @return threshold type
     */
    ThresholdType getType();

    /**
     * Checks if the threshold is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();
  }

  /** Anomaly handler interface. */
  interface AnomalyHandler {
    /**
     * Handles an anomaly event.
     *
     * @param anomaly anomaly event
     */
    void handle(AnomalyEvent anomaly);

    /**
     * Gets the handler priority.
     *
     * @return handler priority
     */
    int getPriority();

    /**
     * Gets the handler name.
     *
     * @return handler name
     */
    String getName();

    /**
     * Checks if the handler is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();
  }

  /** Anomaly event interface. */
  interface AnomalyEvent {
    /**
     * Gets the anomaly ID.
     *
     * @return anomaly ID
     */
    String getId();

    /**
     * Gets the anomaly type.
     *
     * @return anomaly type
     */
    AnomalyType getType();

    /**
     * Gets the affected metric.
     *
     * @return affected metric
     */
    AnomalyMetric getMetric();

    /**
     * Gets the anomaly severity.
     *
     * @return severity level
     */
    SeverityLevel getSeverity();

    /**
     * Gets the anomaly score.
     *
     * @return anomaly score (0.0-1.0)
     */
    double getScore();

    /**
     * Gets the detection timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Gets anomaly details.
     *
     * @return details map
     */
    java.util.Map<String, Object> getDetails();

    /**
     * Gets the execution context.
     *
     * @return execution context
     */
    String getExecutionContext();
  }

  /** Baseline learning configuration interface. */
  interface BaselineLearningConfig {
    /**
     * Checks if baseline learning is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets the learning period in milliseconds.
     *
     * @return learning period
     */
    long getLearningPeriod();

    /**
     * Gets the minimum samples required.
     *
     * @return minimum samples
     */
    int getMinSamples();

    /**
     * Gets the learning algorithm.
     *
     * @return learning algorithm
     */
    LearningAlgorithm getAlgorithm();

    /**
     * Gets the baseline update strategy.
     *
     * @return update strategy
     */
    BaselineUpdateStrategy getUpdateStrategy();

    /**
     * Gets the confidence threshold.
     *
     * @return confidence threshold (0.0-1.0)
     */
    double getConfidenceThreshold();
  }

  /** Threshold type enumeration. */
  enum ThresholdType {
    /** Absolute threshold. */
    ABSOLUTE,
    /** Percentage threshold. */
    PERCENTAGE,
    /** Standard deviation. */
    STANDARD_DEVIATION,
    /** Percentile threshold. */
    PERCENTILE
  }

  /** Anomaly type enumeration. */
  enum AnomalyType {
    /** Point anomaly. */
    POINT,
    /** Contextual anomaly. */
    CONTEXTUAL,
    /** Collective anomaly. */
    COLLECTIVE,
    /** Drift anomaly. */
    DRIFT
  }

  /** Severity level enumeration. */
  enum SeverityLevel {
    /** Low severity. */
    LOW,
    /** Medium severity. */
    MEDIUM,
    /** High severity. */
    HIGH,
    /** Critical severity. */
    CRITICAL
  }

  /** Learning algorithm enumeration. */
  enum LearningAlgorithm {
    /** Online learning. */
    ONLINE,
    /** Batch learning. */
    BATCH,
    /** Incremental learning. */
    INCREMENTAL,
    /** Adaptive learning. */
    ADAPTIVE
  }

  /** Baseline update strategy enumeration. */
  enum BaselineUpdateStrategy {
    /** Replace baseline. */
    REPLACE,
    /** Merge with existing. */
    MERGE,
    /** Weighted update. */
    WEIGHTED,
    /** Exponential smoothing. */
    EXPONENTIAL_SMOOTHING
  }
}

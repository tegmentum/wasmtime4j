package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;

/**
 * Metrics about the performance overhead introduced by monitoring itself.
 *
 * <p>MonitoringOverhead provides transparency about the cost of performance monitoring and helps in
 * optimizing monitoring configuration to balance detail with performance impact.
 *
 * @since 1.0.0
 */
public interface MonitoringOverhead {

  /**
   * Gets the percentage of CPU time consumed by monitoring operations.
   *
   * <p>This represents the overhead of collecting, processing, and storing performance data
   * relative to the total execution time.
   *
   * @return monitoring CPU overhead percentage (0.0 to 100.0)
   */
  double getCpuOverheadPercentage();

  /**
   * Gets the additional memory usage introduced by monitoring.
   *
   * <p>This includes memory used for storing performance data, event queues, and monitoring
   * infrastructure.
   *
   * @return monitoring memory overhead in bytes
   */
  long getMemoryOverheadBytes();

  /**
   * Gets the average time spent per monitoring operation.
   *
   * <p>This represents the typical overhead for individual monitoring events such as function call
   * tracking or metric collection.
   *
   * @return average monitoring operation time
   */
  Duration getAverageMonitoringOperationTime();

  /**
   * Gets the total number of monitoring operations performed.
   *
   * <p>This includes all monitoring activities such as event collection, metric calculation, and
   * threshold checking.
   *
   * @return total monitoring operation count
   */
  long getTotalMonitoringOperations();

  /**
   * Gets the total time spent in monitoring operations.
   *
   * <p>This represents the cumulative overhead time consumed by all monitoring activities.
   *
   * @return total monitoring time
   */
  Duration getTotalMonitoringTime();

  /**
   * Gets the impact on WebAssembly execution throughput.
   *
   * <p>This represents the percentage reduction in execution throughput due to monitoring overhead.
   *
   * @return throughput impact percentage (0.0 to 100.0)
   */
  double getThroughputImpactPercentage();

  /**
   * Gets the number of monitoring events queued for processing.
   *
   * <p>High queue sizes may indicate monitoring overhead affecting real-time processing.
   *
   * @return current event queue size
   */
  int getEventQueueSize();

  /**
   * Gets the maximum observed event queue size.
   *
   * <p>This helps identify peak monitoring load periods.
   *
   * @return maximum event queue size observed
   */
  int getMaxEventQueueSize();

  /**
   * Gets the number of monitoring events that were dropped due to overload.
   *
   * <p>Dropped events indicate monitoring overhead causing data loss.
   *
   * @return dropped event count
   */
  long getDroppedEventCount();

  /**
   * Checks if the monitoring overhead is within acceptable limits.
   *
   * <p>This evaluates whether the current monitoring configuration maintains acceptable performance
   * impact based on configured thresholds.
   *
   * @return true if overhead is acceptable
   */
  boolean isOverheadAcceptable();

  /**
   * Gets recommendations for optimizing monitoring configuration.
   *
   * <p>Based on current overhead metrics, provides suggestions for reducing monitoring impact while
   * maintaining useful performance data collection.
   *
   * @return optimization recommendations
   */
  String getOptimizationRecommendations();

  /**
   * Creates a summary string representation of monitoring overhead.
   *
   * @return formatted overhead summary
   */
  String getSummary();
}

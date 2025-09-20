package ai.tegmentum.wasmtime4j.performance;

import java.io.Closeable;
import java.time.Duration;
import java.util.List;

/**
 * Interface for real-time performance monitoring of WebAssembly execution.
 *
 * <p>The PerformanceMonitor provides comprehensive performance monitoring capabilities including:
 *
 * <ul>
 *   <li>Real-time metrics collection and aggregation
 *   <li>Historical performance data tracking
 *   <li>Configurable monitoring intervals and thresholds
 *   <li>Low-overhead monitoring implementation
 *   <li>Event-based performance notifications
 * </ul>
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * try (PerformanceMonitor monitor = engine.createPerformanceMonitor()) {
 *   monitor.startMonitoring();
 *
 *   // Execute WebAssembly operations
 *   instance.getFunction("calculate").call();
 *
 *   // Get current performance metrics
 *   ExecutionMetrics metrics = monitor.getCurrentMetrics();
 *   System.out.println("Instructions executed: " + metrics.getInstructionsExecuted());
 *
 *   // Generate comprehensive performance report
 *   PerformanceReport report = monitor.generateReport();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface PerformanceMonitor extends Closeable {

  /**
   * Starts real-time performance monitoring.
   *
   * <p>Once started, the monitor will begin collecting metrics for all WebAssembly operations
   * executed within the monitored scope.
   *
   * @throws IllegalStateException if monitoring is already active
   */
  void startMonitoring();

  /**
   * Stops performance monitoring and finalizes data collection.
   *
   * <p>After stopping, no new metrics will be collected, but existing data remains available for
   * querying and report generation.
   */
  void stopMonitoring();

  /**
   * Checks if performance monitoring is currently active.
   *
   * @return true if monitoring is active, false otherwise
   */
  boolean isMonitoring();

  /**
   * Generates a comprehensive performance report based on collected data.
   *
   * <p>The report includes aggregated metrics, performance trends, and analysis of execution
   * patterns since monitoring began.
   *
   * @return comprehensive performance report
   * @throws IllegalStateException if no monitoring data is available
   */
  PerformanceReport generateReport();

  /**
   * Gets the current real-time performance metrics.
   *
   * <p>Returns a snapshot of current execution metrics, including counters and timing information
   * for the current monitoring session.
   *
   * @return current execution metrics
   * @throws IllegalStateException if monitoring is not active
   */
  ExecutionMetrics getCurrentMetrics();

  /**
   * Gets performance events within the specified time window.
   *
   * <p>Events include function calls, memory operations, and other significant execution milestones
   * that occurred within the time window.
   *
   * @param timeWindow duration to look back from current time
   * @return list of performance events within the time window
   * @throws IllegalArgumentException if timeWindow is null or negative
   */
  List<PerformanceEvent> getEvents(final Duration timeWindow);

  /**
   * Sets the monitoring interval for metric collection.
   *
   * <p>A shorter interval provides more granular data but may increase overhead. A longer interval
   * reduces overhead but provides less detailed timing information.
   *
   * @param interval monitoring interval duration
   * @throws IllegalArgumentException if interval is null, zero, or negative
   */
  void setMonitoringInterval(final Duration interval);

  /**
   * Gets the current monitoring interval.
   *
   * @return current monitoring interval duration
   */
  Duration getMonitoringInterval();

  /**
   * Registers a listener for performance threshold violations.
   *
   * <p>The listener will be notified when performance metrics exceed configured thresholds,
   * allowing for real-time performance issue detection.
   *
   * @param listener performance threshold listener
   * @throws IllegalArgumentException if listener is null
   */
  void addPerformanceThresholdListener(final PerformanceThresholdListener listener);

  /**
   * Removes a previously registered performance threshold listener.
   *
   * @param listener listener to remove
   * @return true if the listener was removed, false if it was not registered
   */
  boolean removePerformanceThresholdListener(final PerformanceThresholdListener listener);

  /**
   * Sets performance thresholds for automated monitoring.
   *
   * <p>When execution metrics exceed these thresholds, registered listeners will be notified.
   *
   * @param thresholds performance threshold configuration
   * @throws IllegalArgumentException if thresholds is null
   */
  void setPerformanceThresholds(final PerformanceThresholds thresholds);

  /**
   * Gets the current performance threshold configuration.
   *
   * @return current performance thresholds, or null if none are set
   */
  PerformanceThresholds getPerformanceThresholds();

  /**
   * Resets all collected performance data and counters.
   *
   * <p>This clears all historical data while preserving the monitoring configuration. Monitoring
   * can continue immediately without interruption.
   */
  void reset();

  /**
   * Gets the overhead introduced by performance monitoring itself.
   *
   * <p>This metric helps assess the cost of monitoring and can be used to adjust monitoring
   * intervals for optimal performance.
   *
   * @return monitoring overhead metrics
   */
  MonitoringOverhead getMonitoringOverhead();
}

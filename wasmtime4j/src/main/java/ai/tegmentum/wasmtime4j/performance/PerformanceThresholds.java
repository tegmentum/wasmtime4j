package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;

/**
 * Configuration for performance monitoring thresholds.
 *
 * <p>PerformanceThresholds defines limits for various performance metrics. When these thresholds
 * are exceeded, performance events are generated and listeners are notified.
 *
 * <p>Thresholds help in proactive performance monitoring and early detection of performance
 * degradation.
 *
 * @since 1.0.0
 */
public interface PerformanceThresholds {

  /**
   * Gets the maximum acceptable function execution time.
   *
   * <p>Function calls exceeding this duration will trigger threshold violation events.
   *
   * @return maximum function execution time
   */
  Duration getMaxFunctionExecutionTime();

  /**
   * Gets the maximum acceptable memory allocation size.
   *
   * <p>Single allocations exceeding this size will trigger threshold violation events.
   *
   * @return maximum allocation size in bytes
   */
  long getMaxAllocationSize();

  /**
   * Gets the maximum acceptable total memory usage.
   *
   * <p>Total memory usage exceeding this limit will trigger threshold violation events.
   *
   * @return maximum total memory usage in bytes
   */
  long getMaxTotalMemoryUsage();

  /**
   * Gets the maximum acceptable CPU usage percentage.
   *
   * <p>CPU usage exceeding this percentage will trigger threshold violation events.
   *
   * @return maximum CPU usage percentage (0.0 to 100.0)
   */
  double getMaxCpuUsage();

  /**
   * Gets the maximum acceptable instructions per second rate.
   *
   * <p>Execution rates below this threshold may indicate performance degradation.
   *
   * @return minimum instructions per second
   */
  double getMinInstructionsPerSecond();

  /**
   * Gets the maximum acceptable garbage collection frequency.
   *
   * <p>GC events exceeding this frequency may indicate memory allocation issues.
   *
   * @return maximum GC events per minute
   */
  int getMaxGcEventsPerMinute();

  /**
   * Gets the maximum acceptable JIT compilation time.
   *
   * <p>Compilation times exceeding this duration may indicate optimization issues.
   *
   * @return maximum JIT compilation time
   */
  Duration getMaxJitCompilationTime();

  /**
   * Gets the maximum acceptable host function call frequency.
   *
   * <p>High host function call rates may indicate inefficient WebAssembly design.
   *
   * @return maximum host function calls per second
   */
  double getMaxHostFunctionCallsPerSecond();

  /**
   * Gets the maximum acceptable error rate.
   *
   * <p>Error rates exceeding this threshold may indicate stability issues.
   *
   * @return maximum errors per minute
   */
  int getMaxErrorsPerMinute();

  /**
   * Gets the monitoring overhead threshold.
   *
   * <p>Monitoring overhead exceeding this percentage may require adjustment of monitoring
   * intervals or scope.
   *
   * @return maximum monitoring overhead percentage (0.0 to 100.0)
   */
  double getMaxMonitoringOverhead();

  /**
   * Checks if any threshold is configured.
   *
   * @return true if at least one threshold is set
   */
  boolean hasThresholds();

  /**
   * Creates a builder for configuring performance thresholds.
   *
   * @return new threshold builder
   */
  static PerformanceThresholdsBuilder builder() {
    return new DefaultPerformanceThresholdsBuilder();
  }

  /**
   * Builder interface for creating performance threshold configurations.
   */
  interface PerformanceThresholdsBuilder {

    /**
     * Sets the maximum function execution time threshold.
     *
     * @param maxTime maximum execution time
     * @return this builder
     */
    PerformanceThresholdsBuilder maxFunctionExecutionTime(final Duration maxTime);

    /**
     * Sets the maximum allocation size threshold.
     *
     * @param maxSize maximum allocation size in bytes
     * @return this builder
     */
    PerformanceThresholdsBuilder maxAllocationSize(final long maxSize);

    /**
     * Sets the maximum total memory usage threshold.
     *
     * @param maxMemory maximum memory usage in bytes
     * @return this builder
     */
    PerformanceThresholdsBuilder maxTotalMemoryUsage(final long maxMemory);

    /**
     * Sets the maximum CPU usage threshold.
     *
     * @param maxCpu maximum CPU usage percentage
     * @return this builder
     */
    PerformanceThresholdsBuilder maxCpuUsage(final double maxCpu);

    /**
     * Sets the minimum instructions per second threshold.
     *
     * @param minIps minimum instructions per second
     * @return this builder
     */
    PerformanceThresholdsBuilder minInstructionsPerSecond(final double minIps);

    /**
     * Sets the maximum GC events per minute threshold.
     *
     * @param maxGcEvents maximum GC events per minute
     * @return this builder
     */
    PerformanceThresholdsBuilder maxGcEventsPerMinute(final int maxGcEvents);

    /**
     * Sets the maximum JIT compilation time threshold.
     *
     * @param maxTime maximum compilation time
     * @return this builder
     */
    PerformanceThresholdsBuilder maxJitCompilationTime(final Duration maxTime);

    /**
     * Sets the maximum host function calls per second threshold.
     *
     * @param maxCalls maximum calls per second
     * @return this builder
     */
    PerformanceThresholdsBuilder maxHostFunctionCallsPerSecond(final double maxCalls);

    /**
     * Sets the maximum errors per minute threshold.
     *
     * @param maxErrors maximum errors per minute
     * @return this builder
     */
    PerformanceThresholdsBuilder maxErrorsPerMinute(final int maxErrors);

    /**
     * Sets the maximum monitoring overhead threshold.
     *
     * @param maxOverhead maximum overhead percentage
     * @return this builder
     */
    PerformanceThresholdsBuilder maxMonitoringOverhead(final double maxOverhead);

    /**
     * Builds the performance thresholds configuration.
     *
     * @return configured performance thresholds
     */
    PerformanceThresholds build();
  }
}
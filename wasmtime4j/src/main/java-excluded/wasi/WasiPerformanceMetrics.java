package ai.tegmentum.wasmtime4j.wasi;

import java.time.Duration;

/**
 * Performance metrics for WASI components and instances.
 *
 * @since 1.0.0
 */
public interface WasiPerformanceMetrics {

  /**
   * Gets the average function execution time.
   *
   * @return average execution duration
   */
  Duration getAverageExecutionTime();

  /**
   * Gets the 50th percentile (median) execution time.
   *
   * @return median execution duration
   */
  Duration getMedianExecutionTime();

  /**
   * Gets the 95th percentile execution time.
   *
   * @return 95th percentile duration
   */
  Duration getP95ExecutionTime();

  /**
   * Gets the 99th percentile execution time.
   *
   * @return 99th percentile duration
   */
  Duration getP99ExecutionTime();

  /**
   * Gets the throughput in operations per second.
   *
   * @return operations per second
   */
  double getThroughput();

  /**
   * Gets the memory efficiency metric.
   *
   * @return efficiency score (higher is better)
   */
  double getMemoryEfficiency();
}

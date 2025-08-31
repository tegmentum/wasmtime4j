package ai.tegmentum.wasmtime4j.wasi;

import java.util.Optional;

/**
 * Memory usage information for WASI component instances.
 *
 * @since 1.0.0
 */
public interface WasiMemoryInfo {

  /**
   * Gets the current memory usage in bytes.
   *
   * @return current memory usage
   */
  long getCurrentUsage();

  /**
   * Gets the peak memory usage in bytes.
   *
   * @return peak memory usage
   */
  long getPeakUsage();

  /**
   * Gets the memory limit if configured.
   *
   * @return memory limit in bytes, or empty if unlimited
   */
  Optional<Long> getLimit();

  /**
   * Gets the percentage of memory limit used.
   *
   * @return percentage used (0-100), or empty if no limit
   */
  Optional<Double> getUsagePercentage();

  /**
   * Checks if memory usage is approaching the limit.
   *
   * @return true if near limit (>80%), false otherwise
   */
  boolean isNearLimit();
}
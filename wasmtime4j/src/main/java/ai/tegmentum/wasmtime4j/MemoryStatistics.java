package ai.tegmentum.wasmtime4j;

/**
 * Comprehensive memory statistics for WebAssembly linear memory analysis.
 *
 * <p>This interface provides detailed memory usage statistics and metrics for monitoring,
 * debugging, and optimization purposes. All statistics are collected with minimal performance
 * overhead and provide accurate real-time memory usage information.
 *
 * @since 1.0.0
 */
public interface MemoryStatistics {

  /**
   * Gets the total amount of memory allocated for this WebAssembly instance.
   *
   * <p>This includes both currently used memory and any reserved but unused space.
   *
   * @return the total allocated memory in bytes
   */
  long getTotalAllocated();

  /**
   * Gets the current memory usage in bytes.
   *
   * <p>This represents the actual amount of memory currently being used by the WebAssembly
   * instance, which may be less than the total allocated.
   *
   * @return the current memory usage in bytes
   */
  long getCurrentUsage();

  /**
   * Gets the peak memory usage recorded since the last reset.
   *
   * <p>This value tracks the highest memory usage observed and is useful for capacity planning and
   * memory optimization analysis.
   *
   * @return the peak memory usage in bytes
   */
  long getPeakUsage();

  /**
   * Gets the number of active memory segments.
   *
   * <p>Memory may be divided into multiple segments for optimization purposes. This count includes
   * all currently active segments.
   *
   * @return the number of active memory segments
   */
  int getActiveSegments();

  /**
   * Gets the memory fragmentation ratio as a percentage.
   *
   * <p>Fragmentation occurs when memory is allocated in non-contiguous blocks. A higher ratio
   * indicates more fragmentation, which can impact performance.
   *
   * @return the fragmentation ratio as a value between 0.0 and 1.0
   */
  double getFragmentationRatio();

  /**
   * Gets the number of pages currently allocated.
   *
   * <p>WebAssembly memory is allocated in 64KB pages. This provides the current page count for
   * fine-grained memory tracking.
   *
   * @return the number of allocated pages
   */
  int getAllocatedPages();

  /**
   * Gets the maximum number of pages that can be allocated.
   *
   * <p>This represents the memory limit configured for this WebAssembly instance. Returns -1 if no
   * limit is set.
   *
   * @return the maximum pages, or -1 if unlimited
   */
  int getMaxPages();

  /**
   * Gets the number of memory operations performed since last reset.
   *
   * <p>This includes all read, write, grow, and other memory operations. Useful for performance
   * analysis and optimization.
   *
   * @return the total number of memory operations
   */
  long getOperationCount();

  /**
   * Gets the average memory operation latency in nanoseconds.
   *
   * <p>This metric helps identify performance bottlenecks in memory operations and can guide
   * optimization efforts.
   *
   * @return the average operation latency in nanoseconds
   */
  double getAverageOperationLatency();

  /**
   * Gets the total time spent in memory operations since last reset.
   *
   * <p>This cumulative metric helps understand the overall memory operation overhead in the
   * application.
   *
   * @return the total operation time in nanoseconds
   */
  long getTotalOperationTime();

  /**
   * Gets the memory utilization efficiency as a percentage.
   *
   * <p>This ratio compares actual memory usage to allocated memory, providing insight into memory
   * allocation efficiency.
   *
   * @return the utilization efficiency as a value between 0.0 and 1.0
   */
  double getUtilizationEfficiency();

  /**
   * Resets all resetable statistics (peak usage, operation counts, timings).
   *
   * <p>This allows for measurement of memory behavior over specific time periods while preserving
   * structural information like allocated memory and segments.
   */
  void resetStatistics();

  /**
   * Gets the timestamp when these statistics were last updated.
   *
   * <p>This helps determine the freshness of the statistical data and can be used to calculate
   * rates and trends.
   *
   * @return the last update timestamp in milliseconds since epoch
   */
  long getLastUpdateTimestamp();

  /**
   * Gets the memory pressure level as an indicator of memory stress.
   *
   * <p>This is a normalized value that considers various factors like fragmentation, utilization,
   * and growth rate to provide an overall memory health indicator.
   *
   * @return the memory pressure level between 0.0 (low) and 1.0 (high)
   */
  double getMemoryPressure();
}

package ai.tegmentum.wasmtime4j.wasi;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Statistics and metrics for WASI component instances.
 *
 * <p>Instance statistics provide detailed insight into instance execution, resource usage, and
 * performance characteristics. This information is essential for monitoring, debugging, and
 * optimization.
 *
 * <p>Statistics are collected continuously during instance execution and represent cumulative
 * values from instance creation to the time of collection.
 *
 * @since 1.0.0
 */
public interface WasiInstanceStats {

  /**
   * Gets the timestamp when these statistics were collected.
   *
   * @return the statistics collection timestamp
   */
  Instant getCollectedAt();

  /**
   * Gets the instance identifier.
   *
   * @return the unique instance identifier
   */
  long getInstanceId();

  /**
   * Gets the current state of the instance.
   *
   * @return the instance state
   */
  WasiInstanceState getState();

  /**
   * Gets the instance creation timestamp.
   *
   * @return when the instance was created
   */
  Instant getCreatedAt();

  /**
   * Gets the total uptime for this instance.
   *
   * @return duration since instance creation
   */
  Duration getUptime();

  /**
   * Gets the total execution time for this instance.
   *
   * <p>Execution time is the cumulative time spent actually executing component code, excluding I/O
   * wait time and suspension periods.
   *
   * @return total execution time
   */
  Duration getExecutionTime();

  /**
   * Gets the total number of function calls made on this instance.
   *
   * @return total function call count
   */
  long getFunctionCallCount();

  /**
   * Gets function call statistics broken down by function name.
   *
   * <p>The map contains function names as keys and call counts as values.
   *
   * @return map of function names to call counts
   */
  Map<String, Long> getFunctionCallStats();

  /**
   * Gets execution time statistics broken down by function name.
   *
   * <p>The map contains function names as keys and total execution times as values.
   *
   * @return map of function names to execution times
   */
  Map<String, Duration> getFunctionExecutionTimeStats();

  /**
   * Gets the current memory usage of this instance.
   *
   * @return current memory usage in bytes
   */
  long getCurrentMemoryUsage();

  /**
   * Gets the peak memory usage of this instance.
   *
   * @return peak memory usage in bytes since creation
   */
  long getPeakMemoryUsage();

  /**
   * Gets the number of memory allocations performed.
   *
   * @return total allocation count
   */
  long getMemoryAllocationCount();

  /**
   * Gets the total number of bytes allocated.
   *
   * @return total allocated bytes (may be higher than current usage due to deallocations)
   */
  long getTotalMemoryAllocated();

  /**
   * Gets the current number of resources owned by this instance.
   *
   * @return current resource count
   */
  int getCurrentResourceCount();

  /**
   * Gets the peak number of resources owned by this instance.
   *
   * @return peak resource count
   */
  int getPeakResourceCount();

  /**
   * Gets the total number of resources ever created by this instance.
   *
   * @return total resource creation count
   */
  long getTotalResourcesCreated();

  /**
   * Gets resource usage statistics broken down by resource type.
   *
   * <p>The map contains resource type names as keys and current counts as values.
   *
   * @return map of resource types to current counts
   */
  Map<String, Integer> getResourceUsageByType();

  /**
   * Gets the number of exceptions or errors encountered during execution.
   *
   * @return total error count
   */
  long getErrorCount();

  /**
   * Gets error statistics broken down by error type.
   *
   * <p>The map contains error type names as keys and occurrence counts as values.
   *
   * @return map of error types to counts
   */
  Map<String, Long> getErrorStats();

  /**
   * Gets the number of times this instance has been suspended.
   *
   * @return suspension count
   */
  long getSuspensionCount();

  /**
   * Gets the total time this instance has spent in suspended state.
   *
   * @return total suspension time
   */
  Duration getTotalSuspensionTime();

  /**
   * Gets the number of asynchronous operations initiated by this instance.
   *
   * @return async operation count
   */
  long getAsyncOperationCount();

  /**
   * Gets the number of currently pending asynchronous operations.
   *
   * @return pending async operation count
   */
  int getPendingAsyncOperationCount();

  /**
   * Gets network operation statistics.
   *
   * <p>Includes connection counts, bytes sent/received, and operation times.
   *
   * @return network statistics
   */
  WasiNetworkStats getNetworkStats();

  /**
   * Gets the average function call execution time.
   *
   * @return average execution time per function call
   */
  Duration getAverageExecutionTime();

  /**
   * Gets the execution throughput (function calls per second).
   *
   * <p>Calculated as total function calls divided by total execution time.
   *
   * @return function calls per second
   */
  double getThroughput();

  /**
   * Gets the memory efficiency (useful work per byte allocated).
   *
   * <p>This is a relative metric useful for comparing instance efficiency.
   *
   * @return efficiency metric (higher is better)
   */
  double getMemoryEfficiency();

  /**
   * Gets additional custom properties and metrics.
   *
   * <p>This includes implementation-specific metrics and custom statistics that may be useful for
   * debugging or monitoring.
   *
   * @return map of property names to values
   */
  Map<String, Object> getCustomProperties();

  /**
   * Creates a summary string representation of these statistics.
   *
   * <p>The summary includes key metrics in a human-readable format suitable for logging or display.
   *
   * @return formatted summary string
   */
  String getSummary();

  /**
   * Resets all statistics counters to zero.
   *
   * <p>This allows starting fresh statistics collection from the current point in time. Peak values
   * and creation timestamps are not affected.
   *
   * @throws IllegalStateException if reset is not supported or instance is not in a suitable state
   */
  void reset();
}

package ai.tegmentum.wasmtime4j.component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Statistics and metrics for a WebAssembly component instance.
 *
 * <p>ComponentInstanceStats provides detailed runtime statistics for component instance execution
 * including performance metrics, resource usage, call counts, and timing information. This data
 * is useful for monitoring, profiling, and optimization.
 *
 * <p>Statistics are collected continuously during component execution and provide both aggregate
 * totals and current state information.
 *
 * @since 1.0.0
 */
public interface ComponentInstanceStats {

  /**
   * Gets the timestamp when this instance was created.
   *
   * <p>Returns the time when the component instance was successfully instantiated.
   *
   * @return instance creation timestamp
   */
  Instant getCreationTime();

  /**
   * Gets the total execution time for this instance.
   *
   * <p>Returns the cumulative time spent executing code within this component instance across all
   * function calls and operations.
   *
   * @return total execution time
   */
  Duration getTotalExecutionTime();

  /**
   * Gets the total number of function calls made on this instance.
   *
   * <p>Returns the count of all function invocations on exported functions from this component
   * instance.
   *
   * @return total function call count
   */
  long getTotalFunctionCalls();

  /**
   * Gets the current memory usage of this instance.
   *
   * <p>Returns the amount of memory currently allocated and used by this component instance
   * including linear memory, tables, and runtime structures.
   *
   * @return current memory usage in bytes
   */
  long getCurrentMemoryUsage();

  /**
   * Gets the peak memory usage reached by this instance.
   *
   * <p>Returns the maximum amount of memory that was used by this component instance at any point
   * during its lifetime.
   *
   * @return peak memory usage in bytes
   */
  long getPeakMemoryUsage();

  /**
   * Gets the number of memory allocations performed.
   *
   * <p>Returns the total count of memory allocation operations performed by this component
   * instance.
   *
   * @return number of memory allocations
   */
  long getMemoryAllocations();

  /**
   * Gets the number of garbage collection events triggered.
   *
   * <p>Returns the count of garbage collection cycles that were triggered by or involved this
   * component instance.
   *
   * @return number of GC events
   */
  long getGarbageCollectionCount();

  /**
   * Gets the total time spent in garbage collection.
   *
   * <p>Returns the cumulative time spent in garbage collection operations related to this
   * component instance.
   *
   * @return total GC time
   */
  Duration getGarbageCollectionTime();

  /**
   * Gets per-export call statistics.
   *
   * <p>Returns detailed call statistics for each exported function including call counts,
   * execution times, and error rates.
   *
   * @return map of export names to their call statistics
   */
  Map<String, ComponentExportStats> getExportStats();

  /**
   * Gets resource usage statistics.
   *
   * <p>Returns statistics about component resource creation, usage, and lifecycle management
   * including active resource counts and resource-specific metrics.
   *
   * @return resource usage statistics
   */
  ComponentResourceStats getResourceStats();

  /**
   * Gets exception and error statistics.
   *
   * <p>Returns information about exceptions thrown, errors encountered, and error recovery
   * operations during component execution.
   *
   * @return error and exception statistics
   */
  ComponentErrorStats getErrorStats();

  /**
   * Gets performance metrics for this instance.
   *
   * <p>Returns detailed performance metrics including throughput, latency percentiles, and
   * optimization effectiveness.
   *
   * @return performance metrics
   */
  ComponentPerformanceMetrics getPerformanceMetrics();

  /**
   * Gets the number of active resources owned by this instance.
   *
   * <p>Returns the current count of resources that are actively owned and managed by this
   * component instance.
   *
   * @return number of active resources
   */
  int getActiveResourceCount();

  /**
   * Gets the number of interface calls made to other components.
   *
   * <p>Returns the total count of cross-component interface calls initiated by this instance.
   *
   * @return number of interface calls
   */
  long getInterfaceCallCount();

  /**
   * Gets the average execution time per function call.
   *
   * <p>Returns the mean execution time across all function calls made on this component instance.
   *
   * @return average execution time per call
   */
  Duration getAverageExecutionTime();

  /**
   * Gets the component linking overhead time.
   *
   * <p>Returns the time spent during instantiation resolving imports and setting up component
   * linkage.
   *
   * @return linking overhead time
   */
  Duration getLinkingOverhead();

  /**
   * Checks if this instance is currently executing.
   *
   * <p>Returns true if the component instance is currently executing code in any thread.
   *
   * @return true if currently executing, false otherwise
   */
  boolean isCurrentlyExecuting();

  /**
   * Gets the last activity timestamp.
   *
   * <p>Returns the time when this component instance last executed any code or performed any
   * operations.
   *
   * @return last activity timestamp
   */
  Instant getLastActivityTime();

  /**
   * Resets all statistics counters to zero.
   *
   * <p>Clears all accumulated statistics while preserving the instance creation time and other
   * immutable metadata. This can be useful for performance testing and monitoring.
   */
  void reset();
}
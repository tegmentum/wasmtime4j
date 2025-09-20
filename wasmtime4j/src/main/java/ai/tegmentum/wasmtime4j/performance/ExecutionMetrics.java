package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.time.Instant;

/**
 * Real-time execution metrics for WebAssembly operations.
 *
 * <p>ExecutionMetrics provides detailed performance data about WebAssembly execution including
 * instruction counts, timing information, memory usage, and function call statistics.
 *
 * <p>All metrics are cumulative since the start of monitoring and represent the state at the time
 * the metrics object was created.
 *
 * @since 1.0.0
 */
public interface ExecutionMetrics {

  /**
   * Gets the timestamp when these metrics were collected.
   *
   * @return metrics collection timestamp
   */
  Instant getTimestamp();

  /**
   * Gets the total number of WebAssembly instructions executed.
   *
   * <p>This includes all instruction types (arithmetic, memory, control flow, etc.) executed since
   * monitoring began.
   *
   * @return total instruction count
   */
  long getInstructionsExecuted();

  /**
   * Gets the cumulative execution time for all WebAssembly operations.
   *
   * <p>This represents actual CPU time spent executing WebAssembly code, excluding time spent in
   * host functions or waiting for I/O operations.
   *
   * @return total execution time
   */
  Duration getExecutionTime();

  /**
   * Gets the total number of memory allocations performed.
   *
   * <p>This includes both WebAssembly linear memory allocations and native memory allocations made
   * by the runtime for supporting WebAssembly execution.
   *
   * @return total memory allocation count
   */
  long getMemoryAllocations();

  /**
   * Gets the total number of bytes allocated.
   *
   * <p>This represents the sum of all memory allocation sizes since monitoring began.
   *
   * @return total allocated bytes
   */
  long getTotalAllocatedBytes();

  /**
   * Gets the total number of WebAssembly function calls made.
   *
   * <p>This includes calls to both imported and exported functions, but excludes calls to host
   * functions unless they subsequently call back into WebAssembly.
   *
   * @return total function call count
   */
  int getFunctionCalls();

  /**
   * Gets the average CPU usage percentage during WebAssembly execution.
   *
   * <p>This represents the percentage of available CPU time consumed by WebAssembly execution
   * relative to the total monitoring duration.
   *
   * @return CPU usage percentage (0.0 to 100.0)
   */
  double getCpuUsage();

  /**
   * Gets the current memory usage by WebAssembly instances.
   *
   * <p>This includes linear memory, table memory, and runtime overhead for all active WebAssembly
   * instances.
   *
   * @return current memory usage in bytes
   */
  long getCurrentMemoryUsage();

  /**
   * Gets the peak memory usage recorded during monitoring.
   *
   * <p>This represents the highest memory usage reached at any point during the monitoring session.
   *
   * @return peak memory usage in bytes
   */
  long getPeakMemoryUsage();

  /**
   * Gets the number of garbage collection events that occurred during WebAssembly execution.
   *
   * <p>This tracks garbage collection events in the host JVM that may have been triggered by
   * WebAssembly memory allocation patterns.
   *
   * @return garbage collection count
   */
  int getGarbageCollectionCount();

  /**
   * Gets the total time spent in garbage collection during the monitoring period.
   *
   * @return total garbage collection time
   */
  Duration getGarbageCollectionTime();

  /**
   * Gets the number of host function calls made from WebAssembly.
   *
   * <p>This tracks calls from WebAssembly code to imported host functions.
   *
   * @return host function call count
   */
  int getHostFunctionCalls();

  /**
   * Gets the total time spent executing host functions.
   *
   * <p>This represents time spent in imported host functions called from WebAssembly code.
   *
   * @return total host function execution time
   */
  Duration getHostFunctionTime();

  /**
   * Gets the average instructions per second execution rate.
   *
   * <p>This provides a measure of execution throughput based on the total instructions executed
   * divided by the total execution time.
   *
   * @return instructions per second
   */
  double getInstructionsPerSecond();

  /**
   * Gets the average function calls per second rate.
   *
   * <p>This provides a measure of function call frequency during execution.
   *
   * @return function calls per second
   */
  double getFunctionCallsPerSecond();

  /**
   * Gets the memory allocation rate in bytes per second.
   *
   * <p>This provides insight into memory allocation patterns and potential memory pressure.
   *
   * @return bytes allocated per second
   */
  double getBytesAllocatedPerSecond();

  /**
   * Gets the JIT compilation events count.
   *
   * <p>This tracks the number of Just-In-Time compilation events that occurred during WebAssembly
   * execution.
   *
   * @return JIT compilation count
   */
  int getJitCompilationCount();

  /**
   * Gets the total time spent in JIT compilation.
   *
   * <p>This represents time spent compiling WebAssembly code to optimized native code.
   *
   * @return total JIT compilation time
   */
  Duration getJitCompilationTime();

  /**
   * Creates a summary string representation of these metrics.
   *
   * <p>The summary includes key performance indicators in a human-readable format suitable for
   * logging or display.
   *
   * @return formatted metrics summary
   */
  String getSummary();
}

package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Asynchronous host function interface.
 *
 * <p>An AsyncHostFunction extends the standard HostFunction to provide non-blocking asynchronous
 * execution capabilities. This is particularly useful for host functions that perform I/O
 * operations, make network calls, or execute other time-consuming tasks.
 *
 * <p>Async host functions enable better scalability by allowing the WebAssembly runtime to continue
 * processing other tasks while waiting for host function completion.
 *
 * @since 1.0.0
 */
public interface AsyncHostFunction extends HostFunction {

  /**
   * Asynchronously invokes this host function with the given parameters.
   *
   * <p>This method allows the host function to perform non-blocking operations and return results
   * asynchronously, improving overall system responsiveness.
   *
   * @param params the parameters to pass to the host function
   * @return a CompletableFuture that completes with the function results
   */
  CompletableFuture<WasmValue[]> invokeAsync(final WasmValue... params);

  /**
   * Asynchronously invokes this host function with timeout support.
   *
   * <p>This method provides automatic timeout handling for host function execution, preventing
   * indefinitely blocking operations.
   *
   * @param timeout the maximum execution time
   * @param params the parameters to pass to the host function
   * @return a CompletableFuture that completes with the function results
   * @throws IllegalArgumentException if timeout is null or negative
   */
  CompletableFuture<WasmValue[]> invokeAsync(final Duration timeout, final WasmValue... params);

  /**
   * Asynchronously invokes this host function with custom execution options.
   *
   * <p>This method allows fine-grained control over host function execution, including custom
   * executors, cancellation support, and resource limits.
   *
   * @param options execution options
   * @param params the parameters to pass to the host function
   * @return a CompletableFuture that completes with the function results
   * @throws IllegalArgumentException if options is null
   */
  CompletableFuture<WasmValue[]> invokeAsync(
      final HostExecutionOptions options, final WasmValue... params);

  /**
   * Checks if this host function supports asynchronous execution.
   *
   * <p>Some host functions may only support synchronous execution due to their implementation or
   * the nature of their operations.
   *
   * @return true if async execution is supported
   */
  boolean supportsAsyncExecution();

  /**
   * Gets the preferred execution mode for this host function.
   *
   * @return the preferred execution mode
   */
  ExecutionMode getPreferredExecutionMode();

  /**
   * Gets statistics about async executions of this host function.
   *
   * @return async host function statistics
   */
  AsyncHostFunctionStatistics getAsyncStatistics();

  /** Execution mode preferences for host functions. */
  enum ExecutionMode {
    /** Function prefers synchronous execution */
    SYNCHRONOUS,
    /** Function prefers asynchronous execution */
    ASYNCHRONOUS,
    /** Function can execute efficiently in either mode */
    ADAPTIVE
  }

  /** Configuration options for asynchronous host function execution. */
  interface HostExecutionOptions {
    /**
     * Gets the timeout for host function execution.
     *
     * @return timeout duration, or null for no timeout
     */
    Duration getTimeout();

    /**
     * Gets the custom executor for this execution.
     *
     * @return custom executor, or null to use default
     */
    Executor getExecutor();

    /**
     * Checks if the execution can be cancelled.
     *
     * @return true if cancellation is supported
     */
    boolean isCancellable();

    /**
     * Checks if progress tracking is enabled.
     *
     * @return true if progress tracking is enabled
     */
    boolean isProgressTrackingEnabled();

    /**
     * Gets the maximum memory usage for execution.
     *
     * @return maximum memory in bytes, or -1 for unlimited
     */
    long getMaxMemoryUsage();

    /**
     * Gets the maximum execution time for nested calls.
     *
     * @return maximum nested call time, or null for no limit
     */
    Duration getMaxNestedCallTime();

    /**
     * Gets the priority level for this execution.
     *
     * @return priority level (higher values indicate higher priority)
     */
    int getPriority();

    /**
     * Checks if the host function should have exclusive access to resources.
     *
     * @return true if exclusive access is required
     */
    boolean requiresExclusiveAccess();
  }

  /** Statistics for async host function operations. */
  interface AsyncHostFunctionStatistics {
    /**
     * Gets the total number of async invocations started.
     *
     * @return number of async invocations
     */
    long getAsyncInvocationsStarted();

    /**
     * Gets the total number of async invocations completed successfully.
     *
     * @return number of successful async invocations
     */
    long getAsyncInvocationsCompleted();

    /**
     * Gets the total number of async invocations that failed.
     *
     * @return number of failed async invocations
     */
    long getAsyncInvocationsFailed();

    /**
     * Gets the total number of async invocations that were cancelled.
     *
     * @return number of cancelled async invocations
     */
    long getAsyncInvocationsCancelled();

    /**
     * Gets the total number of async invocations that timed out.
     *
     * @return number of timed out async invocations
     */
    long getAsyncInvocationsTimedOut();

    /**
     * Gets the average execution time in milliseconds.
     *
     * @return average execution time
     */
    double getAverageExecutionTimeMs();

    /**
     * Gets the maximum execution time recorded.
     *
     * @return maximum execution time in milliseconds
     */
    long getMaxExecutionTimeMs();

    /**
     * Gets the minimum execution time recorded.
     *
     * @return minimum execution time in milliseconds
     */
    long getMinExecutionTimeMs();

    /**
     * Gets the current number of active async executions.
     *
     * @return number of active async executions
     */
    int getActiveAsyncExecutions();

    /**
     * Gets the average memory usage during execution.
     *
     * @return average memory usage in bytes
     */
    long getAverageMemoryUsage();

    /**
     * Gets the peak memory usage recorded.
     *
     * @return peak memory usage in bytes
     */
    long getPeakMemoryUsage();
  }
}

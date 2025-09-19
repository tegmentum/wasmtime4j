package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Asynchronous WebAssembly function interface.
 *
 * <p>An AsyncFunction extends the standard WasmFunction with non-blocking asynchronous execution
 * capabilities. This enables better performance and responsiveness when calling long-running
 * WebAssembly functions or when processing multiple function calls concurrently.
 *
 * <p>All async operations return CompletableFuture instances that can be composed and integrated
 * with reactive programming patterns.
 *
 * @since 1.0.0
 */
public interface AsyncFunction extends WasmFunction {

  /**
   * Asynchronously calls this function with the given parameters.
   *
   * <p>This method executes the function without blocking the calling thread, making it suitable
   * for long-running computations or high-throughput scenarios.
   *
   * @param params the parameters to pass to the function
   * @return a CompletableFuture that completes with the function results
   */
  CompletableFuture<WasmValue[]> callAsync(final WasmValue... params);

  /**
   * Asynchronously calls this function with timeout support.
   *
   * <p>This method provides automatic timeout handling for function execution, preventing
   * indefinitely blocking calls.
   *
   * @param timeout the maximum execution time
   * @param params the parameters to pass to the function
   * @return a CompletableFuture that completes with the function results
   * @throws IllegalArgumentException if timeout is null or negative
   */
  CompletableFuture<WasmValue[]> callAsync(final Duration timeout, final WasmValue... params);

  /**
   * Asynchronously calls this function with custom execution options.
   *
   * <p>This method allows fine-grained control over function execution, including custom executors,
   * cancellation support, and progress tracking.
   *
   * @param options execution options
   * @param params the parameters to pass to the function
   * @return a CompletableFuture that completes with the function results
   * @throws IllegalArgumentException if options is null
   */
  CompletableFuture<WasmValue[]> callAsync(
      final ExecutionOptions options, final WasmValue... params);

  /**
   * Asynchronously calls this function expecting a single integer result.
   *
   * <p>This is a convenience method for functions that return a single i32 value.
   *
   * @param params the parameters to pass to the function
   * @return a CompletableFuture that completes with the integer result
   */
  CompletableFuture<Integer> callAsyncInt(final WasmValue... params);

  /**
   * Asynchronously calls this function expecting a single long result.
   *
   * <p>This is a convenience method for functions that return a single i64 value.
   *
   * @param params the parameters to pass to the function
   * @return a CompletableFuture that completes with the long result
   */
  CompletableFuture<Long> callAsyncLong(final WasmValue... params);

  /**
   * Asynchronously calls this function expecting a single float result.
   *
   * <p>This is a convenience method for functions that return a single f32 value.
   *
   * @param params the parameters to pass to the function
   * @return a CompletableFuture that completes with the float result
   */
  CompletableFuture<Float> callAsyncFloat(final WasmValue... params);

  /**
   * Asynchronously calls this function expecting a single double result.
   *
   * <p>This is a convenience method for functions that return a single f64 value.
   *
   * @param params the parameters to pass to the function
   * @return a CompletableFuture that completes with the double result
   */
  CompletableFuture<Double> callAsyncDouble(final WasmValue... params);

  /**
   * Asynchronously calls this function expecting no return value.
   *
   * <p>This is a convenience method for functions that return void.
   *
   * @param params the parameters to pass to the function
   * @return a CompletableFuture that completes when the function finishes
   */
  CompletableFuture<Void> callAsyncVoid(final WasmValue... params);

  /**
   * Checks if this function supports asynchronous execution.
   *
   * <p>Some functions may not support async execution due to their nature or implementation
   * constraints.
   *
   * @return true if async execution is supported
   */
  boolean supportsAsyncExecution();

  /**
   * Gets statistics about async executions of this function.
   *
   * @return async function statistics
   */
  AsyncFunctionStatistics getAsyncStatistics();

  /** Configuration options for asynchronous function execution. */
  interface ExecutionOptions {
    /**
     * Gets the timeout for function execution.
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
     * Gets the maximum number of instructions to execute.
     *
     * @return maximum instruction count, or -1 for unlimited
     */
    long getMaxInstructions();

    /**
     * Gets the priority level for this execution.
     *
     * @return priority level (higher values indicate higher priority)
     */
    int getPriority();
  }

  /** Statistics for async function operations. */
  interface AsyncFunctionStatistics {
    /**
     * Gets the total number of async calls started.
     *
     * @return number of async calls
     */
    long getAsyncCallsStarted();

    /**
     * Gets the total number of async calls completed successfully.
     *
     * @return number of successful async calls
     */
    long getAsyncCallsCompleted();

    /**
     * Gets the total number of async calls that failed.
     *
     * @return number of failed async calls
     */
    long getAsyncCallsFailed();

    /**
     * Gets the total number of async calls that were cancelled.
     *
     * @return number of cancelled async calls
     */
    long getAsyncCallsCancelled();

    /**
     * Gets the total number of async calls that timed out.
     *
     * @return number of timed out async calls
     */
    long getAsyncCallsTimedOut();

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
  }
}

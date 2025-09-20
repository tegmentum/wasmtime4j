package ai.tegmentum.wasmtime4j.concurrency;

import ai.tegmentum.wasmtime4j.WasmFunction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Thread-safe WebAssembly function interface for concurrent execution.
 *
 * <p>A ConcurrentWasmFunction extends the standard WasmFunction interface with explicit thread
 * safety guarantees and concurrent execution capabilities. Multiple threads can safely call the
 * same function simultaneously when the underlying WebAssembly code supports it.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe function calls from multiple threads
 *   <li>Asynchronous function execution with CompletableFuture
 *   <li>Batch function execution for multiple parameter sets
 *   <li>Timeout support for long-running functions
 *   <li>Execution context isolation for concurrent calls
 * </ul>
 *
 * @since 1.0.0
 */
public interface ConcurrentWasmFunction extends WasmFunction {

  /**
   * Asynchronously calls this function with the specified parameters.
   *
   * <p>This method returns immediately and executes the function call in the background. The
   * returned CompletableFuture can be used to retrieve the result or handle errors.
   *
   * @param parameters the parameters to pass to the function
   * @return a CompletableFuture that will complete with the function result
   * @throws IllegalArgumentException if parameters don't match the function signature
   */
  CompletableFuture<Object[]> callAsync(final Object... parameters);

  /**
   * Asynchronously calls this function using a custom executor.
   *
   * <p>This allows using a specific thread pool for function execution.
   *
   * @param executor the executor service to use for execution
   * @param parameters the parameters to pass to the function
   * @return a CompletableFuture that will complete with the function result
   * @throws IllegalArgumentException if executor is null or parameters don't match signature
   */
  CompletableFuture<Object[]> callAsync(final ExecutorService executor, final Object... parameters);

  /**
   * Calls this function with a timeout.
   *
   * <p>This method calls the function and cancels execution if it doesn't complete within the
   * specified timeout period.
   *
   * @param timeoutMillis maximum execution time in milliseconds
   * @param parameters the parameters to pass to the function
   * @return a CompletableFuture that will complete with the result or timeout
   * @throws IllegalArgumentException if timeout is negative or parameters don't match signature
   */
  CompletableFuture<Object[]> callWithTimeout(final long timeoutMillis, final Object... parameters);

  /**
   * Executes multiple function calls with different parameter sets concurrently.
   *
   * <p>This method optimizes the execution of multiple calls to the same function by running them
   * in parallel.
   *
   * @param parameterSets array of parameter arrays for each call
   * @return a CompletableFuture that completes with an array of results
   * @throws IllegalArgumentException if parameterSets is null or contains invalid parameters
   */
  CompletableFuture<Object[][]> callBatch(final Object[]... parameterSets);

  /**
   * Checks if this function supports concurrent execution.
   *
   * <p>Some WebAssembly functions may have dependencies or side effects that prevent safe
   * concurrent execution. This method indicates whether the function can be called concurrently
   * from multiple threads.
   *
   * @return true if the function supports concurrent execution
   */
  boolean supportsConcurrentExecution();

  /**
   * Gets the maximum number of concurrent calls allowed for this function.
   *
   * <p>This limit helps prevent resource exhaustion and ensures stable performance.
   *
   * @return the maximum number of concurrent calls
   */
  int getMaxConcurrentCalls();

  /**
   * Sets the maximum number of concurrent calls allowed for this function.
   *
   * @param maxConcurrentCalls the maximum number of concurrent calls
   * @throws IllegalArgumentException if maxConcurrentCalls is less than 1
   */
  void setMaxConcurrentCalls(final int maxConcurrentCalls);

  /**
   * Gets the number of currently active calls to this function.
   *
   * @return the number of active function calls
   */
  int getActiveCalls();

  /**
   * Gets the total number of calls made to this function.
   *
   * <p>This is a cumulative counter that tracks all calls, both synchronous and asynchronous.
   *
   * @return the total number of function calls
   */
  long getTotalCalls();

  /**
   * Waits for all pending calls to this function to complete.
   *
   * @return a CompletableFuture that completes when all pending calls finish
   */
  CompletableFuture<Void> awaitPendingCalls();

  /**
   * Cancels all pending calls to this function.
   *
   * @return the number of calls that were successfully cancelled
   */
  int cancelPendingCalls();
}

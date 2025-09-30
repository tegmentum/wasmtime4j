package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.Function;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Asynchronous WebAssembly function interface for non-blocking execution.
 *
 * <p>This interface extends the standard Function with asynchronous capabilities,
 * providing non-blocking function calls, timeout handling, and execution monitoring.
 *
 * @param <T> the type of user-defined data associated with the store
 * @since 1.0.0
 */
public interface AsyncFunction<T> extends Function<T> {

  /**
   * Calls the function asynchronously.
   *
   * @param args function arguments
   * @return CompletableFuture containing the function result
   */
  CompletableFuture<WasmValue[]> callAsync(WasmValue... args);

  /**
   * Calls the function asynchronously with a timeout.
   *
   * @param timeout maximum execution time
   * @param args function arguments
   * @return CompletableFuture containing the function result
   */
  CompletableFuture<WasmValue[]> callAsyncWithTimeout(Duration timeout, WasmValue... args);

  /**
   * Calls the function asynchronously using a custom executor.
   *
   * @param executor custom executor for the function call
   * @param args function arguments
   * @return CompletableFuture containing the function result
   */
  CompletableFuture<WasmValue[]> callAsyncWithExecutor(Executor executor, WasmValue... args);

  /**
   * Calls the function multiple times in parallel with different arguments.
   *
   * @param argsArray array of argument sets for parallel execution
   * @return CompletableFuture containing array of function results
   */
  CompletableFuture<WasmValue[][]> callParallel(WasmValue[]... argsArray);

  /**
   * Calls the function repeatedly with the same arguments.
   *
   * @param repetitions number of times to call the function
   * @param args function arguments
   * @return CompletableFuture containing array of function results
   */
  CompletableFuture<WasmValue[][]> callRepeated(int repetitions, WasmValue... args);

  /**
   * Cancels any pending async calls for this function.
   *
   * @return CompletableFuture that completes when cancellation is done
   */
  CompletableFuture<Void> cancelPendingCalls();

  /**
   * Gets async execution statistics for this function.
   *
   * @return async function statistics
   */
  AsyncFunctionStatistics getAsyncStatistics();

  /**
   * Checks if the function supports async execution.
   *
   * @return true if async execution is supported
   */
  boolean supportsAsyncExecution();

  /**
   * Gets the maximum number of parallel calls allowed.
   *
   * @return maximum parallel calls (0 for unlimited)
   */
  int getMaxParallelCalls();

  /**
   * Enables or disables automatic result caching.
   *
   * @param enabled true to enable result caching
   * @return this function for method chaining
   */
  AsyncFunction<T> setResultCaching(boolean enabled);

  /**
   * Sets the cache size for function results.
   *
   * @param cacheSize maximum number of cached results
   * @return this function for method chaining
   */
  AsyncFunction<T> setCacheSize(int cacheSize);

  /**
   * Precompiles the function for optimized async execution.
   *
   * @return CompletableFuture that completes when precompilation is done
   */
  CompletableFuture<Void> precompile();

  /**
   * Warms up the function by executing dummy calls.
   *
   * @param warmupCalls number of warmup calls to perform
   * @return CompletableFuture that completes when warmup is done
   */
  CompletableFuture<Void> warmup(int warmupCalls);

  /**
   * Gets the function's execution context.
   *
   * @return async execution context
   */
  AsyncExecutionContext getExecutionContext();

  /**
   * Sets the execution context for async calls.
   *
   * @param context the execution context
   * @return this function for method chaining
   */
  AsyncFunction<T> setExecutionContext(AsyncExecutionContext context);
}
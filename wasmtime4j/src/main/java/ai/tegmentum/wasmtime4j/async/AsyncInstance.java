package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.Function;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Asynchronous WebAssembly instance interface for non-blocking function execution.
 *
 * <p>This interface extends the standard Instance with asynchronous capabilities,
 * allowing for non-blocking function calls, parallel execution, and timeout handling.
 *
 * @param <T> the type of user-defined data associated with the store
 * @since 1.0.0
 */
public interface AsyncInstance<T> extends Instance<T> {

  /**
   * Calls a WebAssembly function asynchronously.
   *
   * @param functionName name of the function to call
   * @param args function arguments
   * @return CompletableFuture containing the function result
   */
  CompletableFuture<WasmValue[]> callAsync(String functionName, WasmValue... args);

  /**
   * Calls a WebAssembly function asynchronously with a custom executor.
   *
   * @param functionName name of the function to call
   * @param executor custom executor for the function call
   * @param args function arguments
   * @return CompletableFuture containing the function result
   */
  CompletableFuture<WasmValue[]> callAsync(
      String functionName, Executor executor, WasmValue... args);

  /**
   * Calls a WebAssembly function asynchronously with timeout.
   *
   * @param functionName name of the function to call
   * @param timeoutMillis timeout in milliseconds
   * @param args function arguments
   * @return CompletableFuture containing the function result
   */
  CompletableFuture<WasmValue[]> callAsyncWithTimeout(
      String functionName, long timeoutMillis, WasmValue... args);

  /**
   * Calls multiple WebAssembly functions in parallel.
   *
   * @param calls array of function calls to execute
   * @return CompletableFuture containing array of function results
   */
  CompletableFuture<AsyncFunctionResult[]> callParallel(AsyncFunctionCall... calls);

  /**
   * Gets an async function handle for repeated calls.
   *
   * @param functionName name of the function
   * @return async function handle
   */
  CompletableFuture<AsyncFunction<T>> getAsyncFunction(String functionName);

  /**
   * Checks if the instance supports async function calls.
   *
   * @return true if async calls are supported
   */
  boolean supportsAsyncCalls();

  /**
   * Gets the maximum number of parallel function calls allowed.
   *
   * @return maximum parallel calls (0 for unlimited)
   */
  int getMaxParallelCalls();

  /**
   * Cancels all pending async function calls.
   *
   * @return CompletableFuture that completes when all calls are cancelled
   */
  CompletableFuture<Void> cancelAllCalls();

  /**
   * Gets async execution statistics for this instance.
   *
   * @return async execution statistics
   */
  AsyncInstanceStatistics getAsyncStatistics();

  /**
   * Enables or disables function call batching for better performance.
   *
   * @param enabled true to enable batching
   * @return this instance for method chaining
   */
  AsyncInstance<T> setCallBatching(boolean enabled);

  /**
   * Sets the batch size for function calls.
   *
   * @param batchSize number of calls per batch
   * @return this instance for method chaining
   */
  AsyncInstance<T> setBatchSize(int batchSize);

  /**
   * Enables or disables automatic result caching.
   *
   * @param enabled true to enable result caching
   * @return this instance for method chaining
   */
  AsyncInstance<T> setResultCaching(boolean enabled);

  /**
   * Precompiles functions for faster async execution.
   *
   * @param functionNames names of functions to precompile
   * @return CompletableFuture that completes when precompilation is done
   */
  CompletableFuture<Void> precompileFunctions(String... functionNames);

  /**
   * Warms up the instance by executing dummy calls to optimize performance.
   *
   * @param functionNames functions to warm up
   * @return CompletableFuture that completes when warmup is done
   */
  CompletableFuture<Void> warmup(String... functionNames);

  /**
   * Gets the current execution context for async operations.
   *
   * @return async execution context
   */
  AsyncExecutionContext getExecutionContext();

  /**
   * Sets a custom execution context for async operations.
   *
   * @param context the execution context
   * @return this instance for method chaining
   */
  AsyncInstance<T> setExecutionContext(AsyncExecutionContext context);
}
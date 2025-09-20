package ai.tegmentum.wasmtime4j.concurrency;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Thread-safe WebAssembly instance interface for concurrent execution.
 *
 * <p>A ConcurrentInstance extends the standard Instance interface with explicit thread safety
 * guarantees for all operations. This allows multiple threads to safely interact with the same
 * WebAssembly instance simultaneously, enabling true concurrent execution of WebAssembly code.
 *
 * <p>Key features:
 * <ul>
 *   <li>Thread-safe function calls with concurrent execution support
 *   <li>Concurrent access to memory, globals, and tables
 *   <li>Asynchronous function execution with CompletableFuture
 *   <li>Batch function execution for multiple operations
 *   <li>Thread-local execution contexts for isolating concurrent calls
 * </ul>
 *
 * <p>Implementation requirements:
 * <ul>
 *   <li>All operations must be thread-safe without external synchronization
 *   <li>Function calls must support true concurrent execution when safe
 *   <li>Memory and global access must be properly synchronized
 *   <li>Resource access must not interfere between threads
 * </ul>
 *
 * @since 1.0.0
 */
public interface ConcurrentInstance extends Instance {

  /**
   * Gets the thread-safe module associated with this instance.
   *
   * @return the ConcurrentModule that created this instance
   */
  @Override
  ConcurrentModule getModule();

  /**
   * Gets the thread-safe store associated with this instance.
   *
   * @return the ThreadSafeStore containing this instance
   */
  @Override
  ThreadSafeStore getStore();

  /**
   * Thread-safe retrieval of exported functions.
   *
   * <p>This method is thread-safe and returns a concurrent-capable function.
   *
   * @param name the name of the exported function
   * @return the concurrent function, or null if not found
   * @throws IllegalArgumentException if name is null
   */
  @Override
  ConcurrentWasmFunction getFunction(final String name);

  /**
   * Thread-safe retrieval of exported memory.
   *
   * <p>This method is thread-safe and returns a concurrent-capable memory object.
   *
   * @param name the name of the exported memory
   * @return the concurrent memory, or null if not found
   * @throws IllegalArgumentException if name is null
   */
  @Override
  ConcurrentWasmMemory getMemory(final String name);

  /**
   * Thread-safe retrieval of exported globals.
   *
   * <p>This method is thread-safe and returns a concurrent-capable global object.
   *
   * @param name the name of the exported global
   * @return the concurrent global, or null if not found
   * @throws IllegalArgumentException if name is null
   */
  @Override
  ConcurrentWasmGlobal getGlobal(final String name);

  /**
   * Thread-safe retrieval of exported tables.
   *
   * <p>This method is thread-safe and returns a concurrent-capable table object.
   *
   * @param name the name of the exported table
   * @return the concurrent table, or null if not found
   * @throws IllegalArgumentException if name is null
   */
  @Override
  ConcurrentWasmTable getTable(final String name);

  /**
   * Asynchronously calls an exported function.
   *
   * <p>This method returns immediately and executes the function call in the background.
   * The returned CompletableFuture can be used to retrieve the result or handle errors.
   *
   * @param functionName the name of the function to call
   * @param parameters the parameters to pass to the function
   * @return a CompletableFuture that will complete with the function result
   * @throws IllegalArgumentException if functionName is null or function doesn't exist
   */
  CompletableFuture<Object[]> callFunctionAsync(
      final String functionName, final Object... parameters);

  /**
   * Asynchronously calls an exported function using a custom executor.
   *
   * <p>This allows using a specific thread pool for function execution.
   *
   * @param functionName the name of the function to call
   * @param parameters the parameters to pass to the function
   * @param executor the executor service to use for execution
   * @return a CompletableFuture that will complete with the function result
   * @throws IllegalArgumentException if functionName, executor is null, or function doesn't exist
   */
  CompletableFuture<Object[]> callFunctionAsync(
      final String functionName, final ExecutorService executor, final Object... parameters);

  /**
   * Executes multiple function calls concurrently.
   *
   * <p>This method optimizes the execution of multiple function calls by running them
   * in parallel. All calls are executed within the same instance context.
   *
   * @param functionCalls array of function call specifications
   * @return a CompletableFuture that completes with an array of results
   * @throws IllegalArgumentException if functionCalls is null or contains invalid calls
   */
  CompletableFuture<Object[][]> callFunctionsBatch(final FunctionCall... functionCalls);

  /**
   * Calls a function with a timeout.
   *
   * <p>This method calls a function and cancels execution if it doesn't complete
   * within the specified timeout period.
   *
   * @param functionName the name of the function to call
   * @param timeoutMillis maximum execution time in milliseconds
   * @param parameters the parameters to pass to the function
   * @return a CompletableFuture that will complete with the function result or timeout
   * @throws IllegalArgumentException if functionName is null or timeout is negative
   */
  CompletableFuture<Object[]> callFunctionWithTimeout(
      final String functionName, final long timeoutMillis, final Object... parameters);

  /**
   * Creates a concurrent execution context for this instance.
   *
   * <p>This creates an isolated execution context that can be used to group related
   * operations together for better performance and resource management.
   *
   * @return a new ConcurrentExecutionContext for this instance
   */
  ConcurrentExecutionContext createExecutionContext();

  /**
   * Gets the maximum number of concurrent function calls allowed.
   *
   * <p>This limit helps prevent resource exhaustion during heavy concurrent loads.
   *
   * @return the maximum number of concurrent function calls
   */
  int getMaxConcurrentCalls();

  /**
   * Sets the maximum number of concurrent function calls allowed.
   *
   * <p>Setting this limit helps control resource usage during peak loads.
   *
   * @param maxConcurrentCalls the maximum number of concurrent function calls
   * @throws IllegalArgumentException if maxConcurrentCalls is less than 1
   */
  void setMaxConcurrentCalls(final int maxConcurrentCalls);

  /**
   * Gets the number of currently active function calls.
   *
   * @return the number of active function calls
   */
  int getActiveFunctionCallCount();

  /**
   * Gets the total number of function calls executed on this instance.
   *
   * <p>This is a cumulative counter that tracks all function calls, both
   * synchronous and asynchronous.
   *
   * @return the total number of function calls
   */
  long getTotalFunctionCallCount();

  /**
   * Waits for all pending function calls to complete.
   *
   * <p>This method blocks until all currently running async function calls finish.
   * It's useful for graceful shutdown or batch completion scenarios.
   *
   * @return a CompletableFuture that completes when all pending calls are done
   */
  CompletableFuture<Void> awaitPendingCalls();

  /**
   * Cancels all pending function calls.
   *
   * <p>This attempts to cancel any queued function calls that haven't started yet.
   * Calls that are already executing may not be immediately cancellable.
   *
   * @return the number of calls that were successfully cancelled
   */
  int cancelPendingCalls();

  /**
   * Checks if this instance supports concurrent function execution.
   *
   * <p>While all ConcurrentInstance implementations should support this,
   * this method allows checking for any implementation-specific limitations.
   *
   * @return true if concurrent function execution is fully supported
   */
  boolean supportsConcurrentExecution();

  /**
   * Gets comprehensive execution statistics for this instance.
   *
   * <p>This includes metrics about function call performance, concurrency patterns,
   * and resource usage.
   *
   * @return detailed execution statistics
   */
  ExecutionStatistics getExecutionStatistics();

  /**
   * Validates that the instance is properly configured for concurrent use.
   *
   * <p>This checks internal state to ensure the instance can safely handle
   * concurrent function calls and resource access.
   *
   * @return true if the instance is properly configured for concurrency
   */
  boolean validateConcurrencyConfiguration();

  /**
   * Gets the thread pool used for asynchronous function execution.
   *
   * <p>This can be used to monitor the state of the thread pool or submit
   * additional tasks that should run in the same context.
   *
   * @return the ExecutorService used for async function calls
   */
  ExecutorService getExecutorService();

  /**
   * Sets a custom thread pool for asynchronous function execution.
   *
   * <p>This allows using a specific thread pool configuration optimized for
   * the application's function call patterns.
   *
   * @param executorService the executor service to use
   * @throws IllegalArgumentException if executorService is null
   */
  void setExecutorService(final ExecutorService executorService);

  /**
   * Creates a snapshot of the current instance state for debugging.
   *
   * <p>This creates a read-only snapshot that can be safely accessed without
   * affecting ongoing concurrent operations.
   *
   * @return a thread-safe snapshot of the instance state
   */
  InstanceSnapshot createSnapshot();
}
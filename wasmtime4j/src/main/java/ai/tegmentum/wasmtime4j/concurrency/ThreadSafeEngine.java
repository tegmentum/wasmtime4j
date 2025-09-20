package ai.tegmentum.wasmtime4j.concurrency;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Thread-safe WebAssembly engine interface for concurrent execution.
 *
 * <p>A ThreadSafeEngine extends the standard Engine interface with explicit thread safety
 * guarantees and concurrent execution capabilities. Unlike the base Engine interface,
 * ThreadSafeEngine provides thread-safe operations that can be safely called from multiple threads
 * without external synchronization.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe module compilation and store creation
 *   <li>Concurrent execution of multiple compilation operations
 *   <li>Asynchronous module compilation with CompletableFuture
 *   <li>Configurable concurrency limits and resource management
 *   <li>Deadlock prevention and thread pool management
 * </ul>
 *
 * <p>Implementation requirements:
 *
 * <ul>
 *   <li>All operations must be thread-safe without external synchronization
 *   <li>Resources must be properly managed under concurrent access
 *   <li>Performance should scale with available CPU cores
 *   <li>Proper error handling under concurrent load
 * </ul>
 *
 * @since 1.0.0
 */
public interface ThreadSafeEngine extends Engine {

  /**
   * Creates a thread-safe store associated with this engine.
   *
   * <p>This method is thread-safe and can be called concurrently from multiple threads. Each
   * returned store will be properly isolated and thread-safe for its own operations.
   *
   * @return a new thread-safe Store instance
   * @throws WasmException if the store cannot be created
   */
  @Override
  ThreadSafeStore createStore() throws WasmException;

  /**
   * Creates a thread-safe store with custom data associated with this engine.
   *
   * <p>This method is thread-safe and can be called concurrently from multiple threads.
   *
   * @param data custom data to associate with the store
   * @return a new thread-safe Store instance with the associated data
   * @throws WasmException if the store cannot be created
   */
  @Override
  ThreadSafeStore createStore(final Object data) throws WasmException;

  /**
   * Thread-safe compilation of WebAssembly bytecode into a module.
   *
   * <p>This method is thread-safe and can be called concurrently from multiple threads. Multiple
   * compilation operations can proceed in parallel without interference.
   *
   * @param wasmBytes the WebAssembly bytecode to compile
   * @return a compiled thread-safe Module
   * @throws WasmException if compilation fails due to invalid bytecode or engine issues
   * @throws IllegalArgumentException if wasmBytes is null
   */
  @Override
  ConcurrentModule compileModule(final byte[] wasmBytes) throws WasmException;

  /**
   * Asynchronously compiles WebAssembly bytecode into a module.
   *
   * <p>This method returns immediately and performs compilation in the background using the
   * engine's thread pool. The returned CompletableFuture can be used to retrieve the compiled
   * module or handle compilation errors.
   *
   * @param wasmBytes the WebAssembly bytecode to compile
   * @return a CompletableFuture that will complete with the compiled module
   * @throws IllegalArgumentException if wasmBytes is null
   */
  CompletableFuture<ConcurrentModule> compileModuleAsync(final byte[] wasmBytes);

  /**
   * Asynchronously compiles WebAssembly bytecode using a custom executor.
   *
   * <p>This allows using a specific thread pool for compilation operations, useful for controlling
   * resource allocation and thread management.
   *
   * @param wasmBytes the WebAssembly bytecode to compile
   * @param executor the executor service to use for compilation
   * @return a CompletableFuture that will complete with the compiled module
   * @throws IllegalArgumentException if wasmBytes or executor is null
   */
  CompletableFuture<ConcurrentModule> compileModuleAsync(
      final byte[] wasmBytes, final ExecutorService executor);

  /**
   * Compiles multiple WebAssembly modules concurrently.
   *
   * <p>This method optimizes the compilation of multiple modules by processing them in parallel.
   * All modules are compiled using the same engine configuration.
   *
   * @param wasmBytesArray array of WebAssembly bytecode to compile
   * @return a CompletableFuture that completes with an array of compiled modules
   * @throws IllegalArgumentException if wasmBytesArray is null or contains null elements
   */
  CompletableFuture<ConcurrentModule[]> compileModulesBatch(final byte[][] wasmBytesArray);

  /**
   * Sets the maximum number of concurrent compilation operations.
   *
   * <p>This controls how many compilation operations can run simultaneously. Setting this too high
   * may cause memory pressure, while setting it too low may underutilize available CPU cores.
   *
   * @param maxConcurrentCompilations the maximum number of concurrent compilations
   * @throws IllegalArgumentException if maxConcurrentCompilations is less than 1
   */
  void setMaxConcurrentCompilations(final int maxConcurrentCompilations);

  /**
   * Gets the maximum number of concurrent compilation operations.
   *
   * @return the maximum number of concurrent compilations
   */
  int getMaxConcurrentCompilations();

  /**
   * Gets the number of currently active compilation operations.
   *
   * @return the number of active compilations
   */
  int getActiveCompilationCount();

  /**
   * Gets the thread pool used for asynchronous operations.
   *
   * <p>This can be used to monitor the state of the thread pool or submit additional tasks that
   * should run in the same context as engine operations.
   *
   * @return the ExecutorService used for async operations
   */
  ExecutorService getExecutorService();

  /**
   * Sets a custom thread pool for asynchronous operations.
   *
   * <p>This allows using a specific thread pool configuration optimized for the application's
   * needs. The engine will use this executor for all asynchronous compilation operations.
   *
   * @param executorService the executor service to use
   * @throws IllegalArgumentException if executorService is null
   */
  void setExecutorService(final ExecutorService executorService);

  /**
   * Waits for all pending compilation operations to complete.
   *
   * <p>This method blocks until all currently running async compilation operations finish. It's
   * useful for graceful shutdown or when waiting for a batch of compilations to complete.
   *
   * @return a Future that completes when all pending operations are done
   */
  Future<Void> awaitPendingCompilations();

  /**
   * Cancels all pending compilation operations.
   *
   * <p>This attempts to cancel any queued compilation operations that haven't started yet.
   * Operations that are already running may not be immediately cancellable depending on the
   * implementation.
   *
   * @return the number of operations that were successfully cancelled
   */
  int cancelPendingCompilations();

  /**
   * Gets comprehensive concurrency statistics for this engine.
   *
   * <p>This includes information about thread usage, compilation performance, resource contention,
   * and other concurrency-related metrics.
   *
   * @return detailed concurrency statistics
   */
  ConcurrencyStatistics getConcurrencyStatistics();

  /**
   * Validates that the engine is properly configured for concurrent use.
   *
   * <p>This checks internal state and configuration to ensure the engine can safely handle
   * concurrent operations. It's useful for debugging concurrency issues or validating engine setup.
   *
   * @return true if the engine is properly configured for concurrency
   */
  boolean validateConcurrencyConfiguration();

  /**
   * Creates a scoped concurrent context for batch operations.
   *
   * <p>This creates a context that can be used to group related operations together, providing
   * better resource management and performance optimization for batch processing scenarios.
   *
   * @return a new ConcurrentExecutionContext
   */
  ConcurrentExecutionContext createConcurrentContext();
}

package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Asynchronous WebAssembly compilation engine interface.
 *
 * <p>An AsyncEngine extends the standard Engine with non-blocking asynchronous operations for
 * module compilation, store creation, and other time-intensive operations. This enables better
 * responsiveness in applications that process multiple WebAssembly modules concurrently.
 *
 * <p>All async operations return CompletableFuture instances that can be composed, combined, and
 * integrated with existing Java async patterns.
 *
 * @since 1.0.0
 */
public interface AsyncEngine extends Engine {

  /**
   * Asynchronously compiles WebAssembly bytecode into a module.
   *
   * <p>This method validates and compiles the provided WebAssembly bytecode without blocking the
   * calling thread. The compilation process includes parsing, validation, and optimization.
   *
   * @param wasmBytes the WebAssembly bytecode to compile
   * @return a CompletableFuture that completes with the compiled Module
   * @throws IllegalArgumentException if wasmBytes is null
   */
  CompletableFuture<Module> compileModuleAsync(final byte[] wasmBytes);

  /**
   * Asynchronously compiles WebAssembly bytecode from an InputStream.
   *
   * <p>This method enables streaming compilation of large WebAssembly modules without loading the
   * entire bytecode into memory first.
   *
   * @param wasmStream the InputStream containing WebAssembly bytecode
   * @return a CompletableFuture that completes with the compiled Module
   * @throws IllegalArgumentException if wasmStream is null
   */
  CompletableFuture<Module> compileModuleAsync(final InputStream wasmStream);

  /**
   * Asynchronously compiles WebAssembly bytecode with custom options.
   *
   * <p>This method allows configuration of compilation behavior such as buffer sizes, progress
   * tracking, timeouts, and custom executors.
   *
   * @param wasmBytes the WebAssembly bytecode to compile
   * @param options compilation options
   * @return a CompletableFuture that completes with the compiled Module
   * @throws IllegalArgumentException if wasmBytes or options is null
   */
  CompletableFuture<Module> compileModuleAsync(
      final byte[] wasmBytes, final CompilationOptions options);

  /**
   * Asynchronously compiles WebAssembly bytecode from a stream with custom options.
   *
   * @param wasmStream the InputStream containing WebAssembly bytecode
   * @param options compilation options
   * @return a CompletableFuture that completes with the compiled Module
   * @throws IllegalArgumentException if wasmStream or options is null
   */
  CompletableFuture<Module> compileModuleAsync(
      final InputStream wasmStream, final CompilationOptions options);

  /**
   * Asynchronously validates WebAssembly bytecode without compilation.
   *
   * <p>This method performs validation checks on WebAssembly bytecode to verify its correctness
   * without the overhead of full compilation.
   *
   * @param wasmBytes the WebAssembly bytecode to validate
   * @return a CompletableFuture that completes when validation is finished
   * @throws IllegalArgumentException if wasmBytes is null
   */
  CompletableFuture<Void> validateModuleAsync(final byte[] wasmBytes);

  /**
   * Asynchronously validates WebAssembly bytecode from a stream.
   *
   * @param wasmStream the InputStream containing WebAssembly bytecode
   * @return a CompletableFuture that completes when validation is finished
   * @throws IllegalArgumentException if wasmStream is null
   */
  CompletableFuture<Void> validateModuleAsync(final InputStream wasmStream);

  /**
   * Asynchronously creates a new store associated with this engine.
   *
   * <p>This method creates a new execution context for WebAssembly instances without blocking the
   * calling thread.
   *
   * @return a CompletableFuture that completes with a new Store instance
   */
  CompletableFuture<Store> createStoreAsync();

  /**
   * Asynchronously creates a new store with custom data.
   *
   * @param data custom data to associate with the store
   * @return a CompletableFuture that completes with a new Store instance
   */
  CompletableFuture<Store> createStoreAsync(final Object data);

  /**
   * Gets the default executor used for async operations.
   *
   * <p>This executor is used when no custom executor is specified in compilation options.
   *
   * @return the default executor for async operations
   */
  Executor getAsyncExecutor();

  /**
   * Sets a custom executor for async operations.
   *
   * <p>This allows customization of the threading behavior for async operations. If set to null,
   * the default executor will be used.
   *
   * @param executor the custom executor to use, or null for default
   */
  void setAsyncExecutor(final Executor executor);

  /**
   * Gets statistics about async operations performed by this engine.
   *
   * @return async operation statistics
   */
  AsyncEngineStatistics getAsyncStatistics();

  /** Configuration options for asynchronous compilation operations. */
  interface CompilationOptions {
    /**
     * Gets the buffer size for streaming operations.
     *
     * @return buffer size in bytes
     */
    int getBufferSize();

    /**
     * Checks if progress tracking is enabled.
     *
     * @return true if progress tracking is enabled
     */
    boolean isProgressTrackingEnabled();

    /**
     * Gets the timeout for compilation operations.
     *
     * @return timeout duration, or null for no timeout
     */
    Duration getTimeout();

    /**
     * Gets the custom executor for this operation.
     *
     * @return custom executor, or null to use engine default
     */
    Executor getExecutor();

    /**
     * Checks if compilation should use streaming mode.
     *
     * @return true if streaming compilation is enabled
     */
    boolean isStreamingEnabled();

    /**
     * Gets the maximum memory usage for compilation.
     *
     * @return maximum memory in bytes, or -1 for unlimited
     */
    long getMaxMemoryUsage();
  }

  /** Statistics for async engine operations. */
  interface AsyncEngineStatistics {
    /**
     * Gets the total number of async compilations started.
     *
     * @return number of async compilations
     */
    long getAsyncCompilationsStarted();

    /**
     * Gets the total number of async compilations completed successfully.
     *
     * @return number of successful async compilations
     */
    long getAsyncCompilationsCompleted();

    /**
     * Gets the total number of async compilations that failed.
     *
     * @return number of failed async compilations
     */
    long getAsyncCompilationsFailed();

    /**
     * Gets the total number of async validations performed.
     *
     * @return number of async validations
     */
    long getAsyncValidations();

    /**
     * Gets the total number of async store creations.
     *
     * @return number of async store creations
     */
    long getAsyncStoreCreations();

    /**
     * Gets the average compilation time in milliseconds.
     *
     * @return average compilation time
     */
    double getAverageCompilationTimeMs();

    /**
     * Gets the average validation time in milliseconds.
     *
     * @return average validation time
     */
    double getAverageValidationTimeMs();

    /**
     * Gets the current number of active async operations.
     *
     * @return number of active async operations
     */
    int getActiveAsyncOperations();
  }
}

package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Asynchronous WebAssembly engine interface for non-blocking compilation and execution.
 *
 * <p>This interface extends the standard Engine with asynchronous capabilities,
 * allowing for non-blocking module compilation, parallel execution, and progress tracking.
 *
 * @since 1.0.0
 */
public interface AsyncEngine extends Engine {

  /**
   * Compiles a WebAssembly module asynchronously.
   *
   * @param wasmBytes the WebAssembly module bytes
   * @return CompletableFuture containing the compilation result
   * @throws CompilationException if compilation fails
   */
  CompletableFuture<AsyncCompilationResult> compileAsync(byte[] wasmBytes);

  /**
   * Compiles a WebAssembly module asynchronously with progress tracking.
   *
   * @param wasmBytes the WebAssembly module bytes
   * @param progressCallback callback for progress updates
   * @return CompletableFuture containing the compilation result
   * @throws CompilationException if compilation fails
   */
  CompletableFuture<AsyncCompilationResult> compileAsync(
      byte[] wasmBytes, Consumer<AsyncCompilationProgress> progressCallback);

  /**
   * Compiles a WebAssembly module asynchronously using a custom executor.
   *
   * @param wasmBytes the WebAssembly module bytes
   * @param executor custom executor for compilation tasks
   * @return CompletableFuture containing the compilation result
   * @throws CompilationException if compilation fails
   */
  CompletableFuture<AsyncCompilationResult> compileAsync(
      byte[] wasmBytes, Executor executor);

  /**
   * Compiles a WebAssembly module asynchronously with full control.
   *
   * @param wasmBytes the WebAssembly module bytes
   * @param progressCallback callback for progress updates
   * @param executor custom executor for compilation tasks
   * @return CompletableFuture containing the compilation result
   * @throws CompilationException if compilation fails
   */
  CompletableFuture<AsyncCompilationResult> compileAsync(
      byte[] wasmBytes,
      Consumer<AsyncCompilationProgress> progressCallback,
      Executor executor);

  /**
   * Creates an asynchronous store for this engine.
   *
   * @param <T> the type of user-defined data
   * @param data user-defined data to associate with the store
   * @return CompletableFuture containing the async store
   */
  <T> CompletableFuture<AsyncStore<T>> createAsyncStore(T data);

  /**
   * Creates an asynchronous store with default configuration.
   *
   * @return CompletableFuture containing the async store
   */
  CompletableFuture<AsyncStore<Void>> createAsyncStore();

  /**
   * Gets the async compilation configuration for this engine.
   *
   * @return the async configuration
   */
  AsyncEngineConfig getAsyncConfig();

  /**
   * Checks if parallel compilation is enabled.
   *
   * @return true if parallel compilation is enabled
   */
  boolean isParallelCompilationEnabled();

  /**
   * Gets the maximum number of parallel compilation threads.
   *
   * @return maximum parallel threads (0 for unlimited)
   */
  int getMaxParallelThreads();

  /**
   * Cancels all pending async compilation operations.
   *
   * @return CompletableFuture that completes when all operations are cancelled
   */
  CompletableFuture<Void> cancelAllOperations();

  /**
   * Gets statistics about async compilation operations.
   *
   * @return async compilation statistics
   */
  AsyncEngineStatistics getStatistics();

  /**
   * Creates a new async engine with default configuration.
   *
   * @return new async engine instance
   */
  static AsyncEngine create() {
    return create(new EngineConfig());
  }

  /**
   * Creates a new async engine with the specified configuration.
   *
   * @param config engine configuration
   * @return new async engine instance
   */
  static AsyncEngine create(final EngineConfig config) {
    return create(config, AsyncEngineConfig.defaultConfig());
  }

  /**
   * Creates a new async engine with full configuration.
   *
   * @param config engine configuration
   * @param asyncConfig async-specific configuration
   * @return new async engine instance
   */
  static AsyncEngine create(final EngineConfig config, final AsyncEngineConfig asyncConfig) {
    // This will be implemented by the factory pattern
    throw new UnsupportedOperationException("Implementation provided by runtime factory");
  }
}
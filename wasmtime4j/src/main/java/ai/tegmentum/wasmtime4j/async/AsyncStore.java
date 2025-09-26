package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Asynchronous WebAssembly store interface for non-blocking instance operations.
 *
 * <p>This interface extends the standard Store with asynchronous capabilities,
 * allowing for non-blocking instance creation, function execution, and resource management.
 *
 * @param <T> the type of user-defined data associated with this store
 * @since 1.0.0
 */
public interface AsyncStore<T> extends Store<T> {

  /**
   * Creates a WebAssembly instance asynchronously.
   *
   * @param module the compiled WebAssembly module
   * @return CompletableFuture containing the instance
   */
  CompletableFuture<AsyncInstance<T>> instantiateAsync(Module module);

  /**
   * Creates a WebAssembly instance asynchronously with progress tracking.
   *
   * @param module the compiled WebAssembly module
   * @param progressCallback callback for instantiation progress updates
   * @return CompletableFuture containing the instance
   */
  CompletableFuture<AsyncInstance<T>> instantiateAsync(
      Module module, Consumer<AsyncInstantiationProgress> progressCallback);

  /**
   * Creates multiple WebAssembly instances in parallel.
   *
   * @param modules array of compiled WebAssembly modules
   * @return CompletableFuture containing array of instances
   */
  CompletableFuture<AsyncInstance<T>[]> instantiateParallel(Module... modules);

  /**
   * Creates a batch of WebAssembly instances with shared resources.
   *
   * @param modules array of compiled WebAssembly modules
   * @param sharedResources whether to enable resource sharing
   * @return CompletableFuture containing the batch instantiation result
   */
  CompletableFuture<AsyncBatchInstantiationResult<T>> instantiateBatch(
      Module[] modules, boolean sharedResources);

  /**
   * Gets the async instantiation statistics for this store.
   *
   * @return async instantiation statistics
   */
  AsyncStoreStatistics getAsyncStatistics();

  /**
   * Checks if parallel instantiation is supported.
   *
   * @return true if parallel instantiation is supported
   */
  boolean supportsParallelInstantiation();

  /**
   * Gets the maximum number of parallel instantiations allowed.
   *
   * @return maximum parallel instantiations (0 for unlimited)
   */
  int getMaxParallelInstantiations();

  /**
   * Cancels all pending async instantiation operations.
   *
   * @return CompletableFuture that completes when all operations are cancelled
   */
  CompletableFuture<Void> cancelAllInstantiations();

  /**
   * Enables or disables resource pooling for instances.
   *
   * @param enabled true to enable resource pooling
   * @return this store for method chaining
   */
  AsyncStore<T> setResourcePooling(boolean enabled);

  /**
   * Sets the maximum number of pooled resources.
   *
   * @param maxPooled maximum pooled resources
   * @return this store for method chaining
   */
  AsyncStore<T> setMaxPooledResources(int maxPooled);

  /**
   * Preloads resources for faster instantiation.
   *
   * @param module module to preload resources for
   * @return CompletableFuture that completes when preloading is done
   */
  CompletableFuture<Void> preloadResources(Module module);

  /**
   * Clears all preloaded resources to free memory.
   *
   * @return CompletableFuture that completes when cleanup is done
   */
  CompletableFuture<Void> clearPreloadedResources();

  /**
   * Gets the current resource utilization of the store.
   *
   * @return resource utilization information
   */
  AsyncStoreResourceUsage getResourceUsage();

  /**
   * Sets a callback for resource usage monitoring.
   *
   * @param callback resource usage callback
   * @return this store for method chaining
   */
  AsyncStore<T> setResourceUsageCallback(Consumer<AsyncStoreResourceUsage> callback);

  /**
   * Enables or disables automatic resource cleanup.
   *
   * @param enabled true to enable automatic cleanup
   * @return this store for method chaining
   */
  AsyncStore<T> setAutoCleanup(boolean enabled);
}
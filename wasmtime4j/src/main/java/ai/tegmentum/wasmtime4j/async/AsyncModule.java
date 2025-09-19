package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous WebAssembly module interface.
 *
 * <p>An AsyncModule extends the standard Module with non-blocking asynchronous operations
 * for instantiation and other time-intensive operations. This enables better performance
 * when creating multiple instances or when instantiation requires significant time.
 *
 * <p>All async operations return CompletableFuture instances that integrate seamlessly
 * with Java's async programming model.
 *
 * @since 1.0.0
 */
public interface AsyncModule extends Module {

  /**
   * Asynchronously creates an instance of this module in the given store.
   *
   * <p>This method creates a new execution context with its own linear memory,
   * globals, and runtime state without blocking the calling thread.
   *
   * @param store the store to create the instance in
   * @return a CompletableFuture that completes with a new Instance
   * @throws IllegalArgumentException if store is null
   */
  CompletableFuture<Instance> instantiateAsync(final Store store);

  /**
   * Asynchronously creates an instance of this module with the provided imports.
   *
   * <p>This method allows for non-blocking instantiation with custom import
   * bindings, which can be useful when imports require initialization time.
   *
   * @param store the store to create the instance in
   * @param imports the import definitions for the module
   * @return a CompletableFuture that completes with a new Instance
   * @throws IllegalArgumentException if store or imports is null
   */
  CompletableFuture<Instance> instantiateAsync(final Store store, final ImportMap imports);

  /**
   * Asynchronously creates an instance with instantiation options.
   *
   * <p>This method provides additional control over the instantiation process,
   * including timeout configuration and progress tracking.
   *
   * @param store the store to create the instance in
   * @param imports the import definitions for the module
   * @param options instantiation options
   * @return a CompletableFuture that completes with a new Instance
   * @throws IllegalArgumentException if store, imports, or options is null
   */
  CompletableFuture<Instance> instantiateAsync(final Store store, final ImportMap imports, final InstantiationOptions options);

  /**
   * Asynchronously validates that the provided imports satisfy this module's requirements.
   *
   * <p>This method performs import validation without blocking, which can be useful
   * when validation involves complex type checking or resolution.
   *
   * @param imports the import definitions to validate
   * @return a CompletableFuture that completes with true if imports are valid
   * @throws IllegalArgumentException if imports is null
   */
  CompletableFuture<Boolean> validateImportsAsync(final ImportMap imports);

  /**
   * Gets statistics about async operations performed on this module.
   *
   * @return async module statistics
   */
  AsyncModuleStatistics getAsyncStatistics();

  /**
   * Configuration options for asynchronous instantiation operations.
   */
  interface InstantiationOptions {
    /**
     * Gets the timeout for instantiation operations.
     *
     * @return timeout duration, or null for no timeout
     */
    java.time.Duration getTimeout();

    /**
     * Checks if progress tracking is enabled during instantiation.
     *
     * @return true if progress tracking is enabled
     */
    boolean isProgressTrackingEnabled();

    /**
     * Gets the maximum memory usage during instantiation.
     *
     * @return maximum memory in bytes, or -1 for unlimited
     */
    long getMaxMemoryUsage();

    /**
     * Checks if lazy initialization is enabled.
     *
     * <p>When enabled, some initialization steps may be deferred until
     * first use, potentially improving instantiation performance.
     *
     * @return true if lazy initialization is enabled
     */
    boolean isLazyInitializationEnabled();

    /**
     * Gets the priority level for this instantiation operation.
     *
     * @return priority level (higher values indicate higher priority)
     */
    int getPriority();
  }

  /**
   * Statistics for async module operations.
   */
  interface AsyncModuleStatistics {
    /**
     * Gets the total number of async instantiations started.
     *
     * @return number of async instantiations
     */
    long getAsyncInstantiationsStarted();

    /**
     * Gets the total number of async instantiations completed successfully.
     *
     * @return number of successful async instantiations
     */
    long getAsyncInstantiationsCompleted();

    /**
     * Gets the total number of async instantiations that failed.
     *
     * @return number of failed async instantiations
     */
    long getAsyncInstantiationsFailed();

    /**
     * Gets the total number of async import validations performed.
     *
     * @return number of async import validations
     */
    long getAsyncImportValidations();

    /**
     * Gets the average instantiation time in milliseconds.
     *
     * @return average instantiation time
     */
    double getAverageInstantiationTimeMs();

    /**
     * Gets the average import validation time in milliseconds.
     *
     * @return average validation time
     */
    double getAverageValidationTimeMs();

    /**
     * Gets the current number of active async instantiations.
     *
     * @return number of active async instantiations
     */
    int getActiveAsyncInstantiations();
  }
}
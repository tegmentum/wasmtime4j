package ai.tegmentum.wasmtime4j.concurrency;

import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Thread-safe WebAssembly module interface with shared instance support.
 *
 * <p>A ConcurrentModule extends the standard Module interface with explicit thread safety
 * guarantees and concurrent instantiation capabilities. Unlike the base Module interface,
 * ConcurrentModule can be safely accessed from multiple threads and supports concurrent
 * instantiation operations.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe module introspection and metadata access
 *   <li>Concurrent instance creation without contention
 *   <li>Shared module data with copy-on-write semantics where appropriate
 *   <li>Asynchronous instantiation with CompletableFuture
 *   <li>Batch instantiation for multiple stores
 * </ul>
 *
 * <p>Implementation requirements:
 *
 * <ul>
 *   <li>All operations must be thread-safe without external synchronization
 *   <li>Module metadata must be immutable and safely shareable
 *   <li>Instance creation must not interfere between threads
 *   <li>Performance should scale with concurrent access patterns
 * </ul>
 *
 * @since 1.0.0
 */
public interface ConcurrentModule extends Module {

  /**
   * Thread-safe creation of an instance in the given store.
   *
   * <p>This method is thread-safe and can be called concurrently from multiple threads. Each
   * instance will be properly isolated even when created simultaneously.
   *
   * @param store the store to create the instance in
   * @return a new thread-safe Instance of this module
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if store is null
   */
  @Override
  ConcurrentInstance instantiate(final Store store) throws WasmException;

  /**
   * Thread-safe creation of an instance with the provided imports.
   *
   * <p>This method is thread-safe and can be called concurrently from multiple threads.
   *
   * @param store the store to create the instance in
   * @param imports the import definitions for the module
   * @return a new thread-safe Instance of this module with the specified imports
   * @throws WasmException if instantiation fails or imports don't match requirements
   * @throws IllegalArgumentException if store or imports is null
   */
  @Override
  ConcurrentInstance instantiate(final Store store, final ImportMap imports) throws WasmException;

  /**
   * Asynchronously creates an instance of this module in the given store.
   *
   * <p>This method returns immediately and performs instantiation in the background. The returned
   * CompletableFuture can be used to retrieve the instance or handle errors.
   *
   * @param store the store to create the instance in
   * @return a CompletableFuture that will complete with the new instance
   * @throws IllegalArgumentException if store is null
   */
  CompletableFuture<ConcurrentInstance> instantiateAsync(final Store store);

  /**
   * Asynchronously creates an instance with the provided imports.
   *
   * <p>This method returns immediately and performs instantiation in the background.
   *
   * @param store the store to create the instance in
   * @param imports the import definitions for the module
   * @return a CompletableFuture that will complete with the new instance
   * @throws IllegalArgumentException if store or imports is null
   */
  CompletableFuture<ConcurrentInstance> instantiateAsync(
      final Store store, final ImportMap imports);

  /**
   * Asynchronously creates an instance using a custom executor.
   *
   * <p>This allows using a specific thread pool for instantiation operations.
   *
   * @param store the store to create the instance in
   * @param imports the import definitions for the module (can be null)
   * @param executor the executor service to use for instantiation
   * @return a CompletableFuture that will complete with the new instance
   * @throws IllegalArgumentException if store or executor is null
   */
  CompletableFuture<ConcurrentInstance> instantiateAsync(
      final Store store, final ImportMap imports, final ExecutorService executor);

  /**
   * Creates multiple instances concurrently across different stores.
   *
   * <p>This method optimizes the instantiation of the same module across multiple stores by
   * processing them in parallel. All instances share the same compiled module data.
   *
   * @param stores array of stores to create instances in
   * @return a CompletableFuture that completes with an array of instances
   * @throws IllegalArgumentException if stores is null or contains null elements
   */
  CompletableFuture<ConcurrentInstance[]> instantiateBatch(final Store[] stores);

  /**
   * Creates multiple instances concurrently with different import configurations.
   *
   * <p>This method creates instances across multiple stores, each with potentially different import
   * configurations.
   *
   * @param stores array of stores to create instances in
   * @param imports array of import configurations (must match stores length)
   * @return a CompletableFuture that completes with an array of instances
   * @throws IllegalArgumentException if arrays are null, different lengths, or contain nulls
   */
  CompletableFuture<ConcurrentInstance[]> instantiateBatch(
      final Store[] stores, final ImportMap[] imports);

  /**
   * Gets the number of active instances created from this module.
   *
   * <p>This count includes all instances that are currently active across all stores. The count is
   * updated atomically as instances are created and destroyed.
   *
   * @return the number of active instances
   */
  int getActiveInstanceCount();

  /**
   * Gets the total number of instances ever created from this module.
   *
   * <p>This is a cumulative counter that never decreases, useful for monitoring and debugging
   * module usage patterns.
   *
   * @return the total number of instances created
   */
  long getTotalInstanceCount();

  /**
   * Gets the engine that was used to compile this module.
   *
   * <p>For ConcurrentModule, this returns a ThreadSafeEngine.
   *
   * @return the ThreadSafeEngine used for compilation
   */
  @Override
  ThreadSafeEngine getEngine();

  /**
   * Checks if this module supports concurrent instantiation.
   *
   * <p>While all ConcurrentModule implementations should support this, this method allows checking
   * for any implementation-specific limitations.
   *
   * @return true if concurrent instantiation is fully supported
   */
  boolean supportsConcurrentInstantiation();

  /**
   * Gets the maximum number of concurrent instantiation operations.
   *
   * <p>This limit helps prevent resource exhaustion during heavy instantiation loads.
   *
   * @return the maximum number of concurrent instantiations
   */
  int getMaxConcurrentInstantiations();

  /**
   * Sets the maximum number of concurrent instantiation operations.
   *
   * <p>Setting this limit helps control resource usage during peak loads.
   *
   * @param maxConcurrentInstantiations the maximum number of concurrent instantiations
   * @throws IllegalArgumentException if maxConcurrentInstantiations is less than 1
   */
  void setMaxConcurrentInstantiations(final int maxConcurrentInstantiations);

  /**
   * Gets the number of currently active instantiation operations.
   *
   * @return the number of active instantiation operations
   */
  int getActiveInstantiationCount();

  /**
   * Waits for all pending instantiation operations to complete.
   *
   * <p>This method blocks until all currently running async instantiation operations finish. It's
   * useful for graceful shutdown or batch completion scenarios.
   *
   * @return a CompletableFuture that completes when all pending operations are done
   */
  CompletableFuture<Void> awaitPendingInstantiations();

  /**
   * Cancels all pending instantiation operations.
   *
   * <p>This attempts to cancel any queued instantiation operations that haven't started yet.
   * Operations that are already running may not be immediately cancellable.
   *
   * @return the number of operations that were successfully cancelled
   */
  int cancelPendingInstantiations();

  /**
   * Creates a shared module context for optimized instance creation.
   *
   * <p>This creates a context that can be reused across multiple instantiation operations for
   * better performance when creating many instances of the same module.
   *
   * @return a new SharedModuleContext
   */
  SharedModuleContext createSharedContext();

  /**
   * Gets comprehensive instantiation statistics for this module.
   *
   * <p>This includes metrics about instance creation performance, resource usage, and concurrency
   * patterns.
   *
   * @return detailed instantiation statistics
   */
  InstantiationStatistics getInstantiationStatistics();

  /**
   * Validates that the module is properly configured for concurrent use.
   *
   * <p>This checks internal state to ensure the module can safely handle concurrent instantiation
   * operations.
   *
   * @return true if the module is properly configured for concurrency
   */
  boolean validateConcurrencyConfiguration();
}

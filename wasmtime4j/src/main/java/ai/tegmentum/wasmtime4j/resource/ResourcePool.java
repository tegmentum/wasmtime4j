package ai.tegmentum.wasmtime4j.resource;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Enterprise-grade resource pool providing object pooling and connection pooling capabilities.
 *
 * <p>ResourcePool manages pools of reusable resources to improve performance and reduce resource
 * allocation overhead. It supports configurable pool sizes, validation, and lifecycle management
 * with automatic cleanup and monitoring.
 *
 * <p>Key features: - Object pooling for expensive-to-create resources - Connection pooling for
 * network and database resources - Configurable pool size limits and overflow handling - Resource
 * validation and health checking - Automatic cleanup of idle resources - Pool statistics and
 * monitoring - Thread-safe concurrent access
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ResourcePool pool = ResourcePool.builder()
 *     .withMaxPoolSize(10)
 *     .withMinPoolSize(2)
 *     .withMaxIdleTime(Duration.ofMinutes(30))
 *     .withValidationInterval(Duration.ofMinutes(5))
 *     .build();
 *
 * // Register a factory for creating expensive resources
 * pool.registerFactory(ResourceType.MODULE, () -> {
 *     return engine.compileModule(commonWasmBytes);
 * });
 *
 * // Acquire resource from pool
 * try (PooledResource<Module> pooled = pool.acquire(ResourceType.MODULE)) {
 *     Module module = pooled.getResource();
 *     Instance instance = module.instantiate();
 * } // Resource automatically returned to pool
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ResourcePool extends AutoCloseable {

  /**
   * Creates a new resource pool builder with default configuration.
   *
   * @return a new builder instance
   */
  static ResourcePoolBuilder builder() {
    return new ResourcePoolBuilder();
  }

  /**
   * Creates a resource pool with default configuration.
   *
   * @return a new resource pool instance
   * @throws WasmException if pool creation fails
   */
  static ResourcePool create() throws WasmException {
    return builder().build();
  }

  /**
   * Registers a factory for creating resources of a specific type.
   *
   * @param <T> the resource type
   * @param type the resource type classification
   * @param factory the factory function for creating new resources
   * @throws WasmException if factory registration fails
   * @throws IllegalArgumentException if type or factory is null
   */
  <T> void registerFactory(final ResourceType type, final Supplier<T> factory) throws WasmException;

  /**
   * Registers a factory with validation for creating resources of a specific type.
   *
   * @param <T> the resource type
   * @param type the resource type classification
   * @param factory the factory function for creating new resources
   * @param validator the validator function for checking resource health
   * @throws WasmException if factory registration fails
   * @throws IllegalArgumentException if any parameter is null
   */
  <T> void registerFactory(
      final ResourceType type, final Supplier<T> factory, final ResourceValidator<T> validator)
      throws WasmException;

  /**
   * Registers a factory with custom pool configuration.
   *
   * @param <T> the resource type
   * @param type the resource type classification
   * @param factory the factory function for creating new resources
   * @param poolConfig the pool configuration for this resource type
   * @throws WasmException if factory registration fails
   * @throws IllegalArgumentException if any parameter is null
   */
  <T> void registerFactory(
      final ResourceType type, final Supplier<T> factory, final PoolConfiguration poolConfig)
      throws WasmException;

  /**
   * Acquires a resource from the pool.
   *
   * <p>If a valid resource is available in the pool, it is returned immediately. If no resources
   * are available and the pool is below maximum size, a new resource is created. If the pool is at
   * maximum size, this method will block until a resource becomes available or the acquisition
   * timeout is exceeded.
   *
   * @param <T> the resource type
   * @param type the resource type to acquire
   * @return a pooled resource wrapper
   * @throws WasmException if resource acquisition fails
   * @throws IllegalArgumentException if type is null
   * @throws IllegalStateException if no factory is registered for the type
   */
  <T> PooledResource<T> acquire(final ResourceType type) throws WasmException;

  /**
   * Acquires a resource from the pool with a timeout.
   *
   * @param <T> the resource type
   * @param type the resource type to acquire
   * @param timeout the maximum time to wait for a resource
   * @return a pooled resource wrapper
   * @throws WasmException if resource acquisition fails or times out
   * @throws IllegalArgumentException if type is null or timeout is null/negative
   * @throws IllegalStateException if no factory is registered for the type
   */
  <T> PooledResource<T> acquire(final ResourceType type, final Duration timeout)
      throws WasmException;

  /**
   * Tries to acquire a resource from the pool without blocking.
   *
   * @param <T> the resource type
   * @param type the resource type to acquire
   * @return a pooled resource wrapper, or empty if no resource is immediately available
   * @throws WasmException if resource acquisition fails
   * @throws IllegalArgumentException if type is null
   * @throws IllegalStateException if no factory is registered for the type
   */
  <T> java.util.Optional<PooledResource<T>> tryAcquire(final ResourceType type)
      throws WasmException;

  /**
   * Asynchronously acquires a resource from the pool.
   *
   * @param <T> the resource type
   * @param type the resource type to acquire
   * @return a future that completes with a pooled resource wrapper
   * @throws IllegalArgumentException if type is null
   * @throws IllegalStateException if no factory is registered for the type
   */
  <T> CompletableFuture<PooledResource<T>> acquireAsync(final ResourceType type);

  /**
   * Returns a resource to the pool.
   *
   * <p>The resource will be validated (if a validator is configured) before being returned to the
   * pool. Invalid resources are discarded instead of being returned to the pool.
   *
   * @param <T> the resource type
   * @param pooledResource the pooled resource to return
   * @throws WasmException if resource return fails
   * @throws IllegalArgumentException if pooledResource is null
   */
  <T> void returnResource(final PooledResource<T> pooledResource) throws WasmException;

  /**
   * Invalidates and discards a resource from the pool.
   *
   * <p>This method should be used when a resource is known to be in an invalid state and should not
   * be returned to the pool for reuse.
   *
   * @param <T> the resource type
   * @param pooledResource the pooled resource to invalidate
   * @throws WasmException if resource invalidation fails
   * @throws IllegalArgumentException if pooledResource is null
   */
  <T> void invalidateResource(final PooledResource<T> pooledResource) throws WasmException;

  /**
   * Pre-populates the pool with the minimum number of resources.
   *
   * <p>This method creates the minimum number of resources for each registered type to ensure the
   * pool is ready for immediate use.
   *
   * @throws WasmException if pool population fails
   */
  void populatePool() throws WasmException;

  /**
   * Pre-populates the pool for a specific resource type.
   *
   * @param type the resource type to populate
   * @param count the number of resources to pre-create
   * @throws WasmException if pool population fails
   * @throws IllegalArgumentException if type is null or count is negative
   * @throws IllegalStateException if no factory is registered for the type
   */
  void populatePool(final ResourceType type, final int count) throws WasmException;

  /**
   * Validates all resources in the pool.
   *
   * <p>This method runs validation checks on all pooled resources and removes any that are found to
   * be invalid.
   *
   * @return the number of invalid resources that were removed
   * @throws WasmException if validation fails
   */
  int validatePool() throws WasmException;

  /**
   * Validates resources of a specific type in the pool.
   *
   * @param type the resource type to validate
   * @return the number of invalid resources that were removed
   * @throws WasmException if validation fails
   * @throws IllegalArgumentException if type is null
   */
  int validatePool(final ResourceType type) throws WasmException;

  /**
   * Cleans up idle resources that have exceeded their maximum idle time.
   *
   * @return the number of idle resources that were cleaned up
   * @throws WasmException if cleanup fails
   */
  int cleanupIdleResources() throws WasmException;

  /**
   * Cleans up idle resources of a specific type.
   *
   * @param type the resource type to clean up
   * @return the number of idle resources that were cleaned up
   * @throws WasmException if cleanup fails
   * @throws IllegalArgumentException if type is null
   */
  int cleanupIdleResources(final ResourceType type) throws WasmException;

  /**
   * Gets comprehensive pool statistics.
   *
   * @return current pool statistics
   */
  PoolStatistics getStatistics();

  /**
   * Gets pool statistics for a specific resource type.
   *
   * @param type the resource type
   * @return pool statistics for the specified type
   * @throws IllegalArgumentException if type is null
   */
  PoolStatistics getStatistics(final ResourceType type);

  /**
   * Gets the current pool configuration.
   *
   * @return the pool configuration
   */
  PoolConfiguration getConfiguration();

  /**
   * Updates the pool configuration.
   *
   * <p>Configuration changes take effect immediately and may trigger pool resizing or cleanup
   * operations.
   *
   * @param configuration the new pool configuration
   * @throws WasmException if configuration update fails
   * @throws IllegalArgumentException if configuration is null
   */
  void updateConfiguration(final PoolConfiguration configuration) throws WasmException;

  /**
   * Gets the list of registered resource types.
   *
   * @return list of resource types with registered factories
   */
  List<ResourceType> getRegisteredTypes();

  /**
   * Checks if a factory is registered for the specified resource type.
   *
   * @param type the resource type to check
   * @return true if a factory is registered, false otherwise
   * @throws IllegalArgumentException if type is null
   */
  boolean isFactoryRegistered(final ResourceType type);

  /**
   * Gets the current size of the pool for all resource types.
   *
   * @return the total number of resources in the pool
   */
  int getPoolSize();

  /**
   * Gets the current size of the pool for a specific resource type.
   *
   * @param type the resource type
   * @return the number of resources of the specified type in the pool
   * @throws IllegalArgumentException if type is null
   */
  int getPoolSize(final ResourceType type);

  /**
   * Gets the number of active (checked out) resources.
   *
   * @return the number of active resources
   */
  int getActiveResourceCount();

  /**
   * Gets the number of active resources of a specific type.
   *
   * @param type the resource type
   * @return the number of active resources of the specified type
   * @throws IllegalArgumentException if type is null
   */
  int getActiveResourceCount(final ResourceType type);

  /**
   * Gets the number of idle (available) resources.
   *
   * @return the number of idle resources
   */
  int getIdleResourceCount();

  /**
   * Gets the number of idle resources of a specific type.
   *
   * @param type the resource type
   * @return the number of idle resources of the specified type
   * @throws IllegalArgumentException if type is null
   */
  int getIdleResourceCount(final ResourceType type);

  /**
   * Checks if the pool is currently operational.
   *
   * @return true if the pool is operational, false otherwise
   */
  boolean isOperational();

  /**
   * Drains the pool by removing all idle resources.
   *
   * <p>Active resources are not affected and will not be returned to the pool when released until
   * the pool is restored.
   *
   * @return the number of resources that were removed
   * @throws WasmException if pool draining fails
   */
  int drainPool() throws WasmException;

  /**
   * Drains the pool for a specific resource type.
   *
   * @param type the resource type to drain
   * @return the number of resources that were removed
   * @throws WasmException if pool draining fails
   * @throws IllegalArgumentException if type is null
   */
  int drainPool(final ResourceType type) throws WasmException;

  /**
   * Restores the pool after draining by re-populating with minimum resources.
   *
   * @throws WasmException if pool restoration fails
   */
  void restorePool() throws WasmException;

  /**
   * Gracefully shuts down the pool.
   *
   * <p>This method will stop accepting new requests, wait for active resources to be returned, and
   * then close all pooled resources.
   *
   * @param timeout the maximum time to wait for shutdown
   * @throws WasmException if shutdown fails or times out
   * @throws IllegalArgumentException if timeout is null or negative
   */
  void shutdown(final Duration timeout) throws WasmException;

  /**
   * Asynchronously shuts down the pool.
   *
   * @param timeout the maximum time to wait for shutdown
   * @return future that completes when shutdown is finished
   * @throws IllegalArgumentException if timeout is null or negative
   */
  CompletableFuture<Void> shutdownAsync(final Duration timeout);

  /**
   * Closes the pool and releases all resources immediately.
   *
   * <p>This method performs immediate cleanup without waiting for graceful shutdown.
   */
  @Override
  void close();
}

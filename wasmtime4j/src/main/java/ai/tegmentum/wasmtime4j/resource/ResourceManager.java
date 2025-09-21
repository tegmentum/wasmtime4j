package ai.tegmentum.wasmtime4j.resource;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Enterprise-grade resource manager for comprehensive lifecycle management and cleanup mechanisms.
 *
 * <p>ResourceManager provides centralized control over all system resources including native
 * handles, memory allocations, cached objects, and pooled resources. It ensures proper resource
 * isolation, automatic cleanup, and enterprise monitoring capabilities.
 *
 * <p>Key features:
 * - Automatic resource lifecycle management with configurable cleanup policies
 * - Resource pooling and caching for performance optimization
 * - Comprehensive monitoring and metrics collection
 * - Thread-safe operations with concurrent access patterns
 * - Resource leak detection and prevention
 * - Graceful shutdown with resource cleanup guarantees
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ResourceManager manager = ResourceManager.builder()
 *     .withCleanupInterval(Duration.ofMinutes(5))
 *     .withResourceLimits(ResourceLimits.builder()
 *         .maxActiveResources(1000)
 *         .maxMemoryUsage(512 * 1024 * 1024) // 512MB
 *         .build())
 *     .withMonitoring(true)
 *     .build();
 *
 * // Register a resource with automatic cleanup
 * try (ManagedResource<Module> module = manager.manage(
 *     engine.compileModule(wasmBytes),
 *     ResourceType.MODULE,
 *     ResourcePolicy.builder()
 *         .withTtl(Duration.ofHours(24))
 *         .withCleanupStrategy(CleanupStrategy.IMMEDIATE)
 *         .build())) {
 *
 *     // Use the managed resource
 *     Instance instance = module.get().instantiate();
 * } // Automatic cleanup when try-with-resources block exits
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ResourceManager extends AutoCloseable {

    /**
     * Creates a new resource manager builder with default configuration.
     *
     * @return a new builder instance
     */
    static ResourceManagerBuilder builder() {
        return new ResourceManagerBuilder();
    }

    /**
     * Creates a resource manager with default configuration.
     *
     * @return a new resource manager instance
     * @throws WasmException if manager creation fails
     */
    static ResourceManager create() throws WasmException {
        return builder().build();
    }

    /**
     * Manages a resource with automatic lifecycle control.
     *
     * @param <T> the resource type
     * @param resource the resource to manage
     * @param type the resource type classification
     * @param policy the resource management policy
     * @return a managed resource wrapper
     * @throws WasmException if resource management setup fails
     * @throws IllegalArgumentException if any parameter is null
     */
    <T> ManagedResource<T> manage(final T resource, final ResourceType type,
                                 final ResourcePolicy policy) throws WasmException;

    /**
     * Manages a resource with default policy.
     *
     * @param <T> the resource type
     * @param resource the resource to manage
     * @param type the resource type classification
     * @return a managed resource wrapper
     * @throws WasmException if resource management setup fails
     * @throws IllegalArgumentException if any parameter is null
     */
    <T> ManagedResource<T> manage(final T resource, final ResourceType type) throws WasmException;

    /**
     * Manages a resource with a custom name for tracking.
     *
     * @param <T> the resource type
     * @param name the resource name for tracking
     * @param resource the resource to manage
     * @param type the resource type classification
     * @param policy the resource management policy
     * @return a managed resource wrapper
     * @throws WasmException if resource management setup fails
     * @throws IllegalArgumentException if any parameter is null or name is empty
     */
    <T> ManagedResource<T> manage(final String name, final T resource, final ResourceType type,
                                 final ResourcePolicy policy) throws WasmException;

    /**
     * Gets a managed resource by name.
     *
     * @param <T> the expected resource type
     * @param name the resource name
     * @param expectedType the expected resource class
     * @return the managed resource, or empty if not found
     * @throws WasmException if resource lookup fails
     * @throws IllegalArgumentException if name is null/empty or expectedType is null
     */
    <T> Optional<ManagedResource<T>> getResource(final String name, final Class<T> expectedType)
            throws WasmException;

    /**
     * Releases a managed resource immediately.
     *
     * @param resource the managed resource to release
     * @throws WasmException if resource release fails
     * @throws IllegalArgumentException if resource is null
     */
    void release(final ManagedResource<?> resource) throws WasmException;

    /**
     * Releases a managed resource by name.
     *
     * @param name the resource name to release
     * @throws WasmException if resource release fails
     * @throws IllegalArgumentException if name is null or empty
     */
    void release(final String name) throws WasmException;

    /**
     * Gets all active managed resources.
     *
     * @return list of all active managed resources
     * @throws WasmException if resource enumeration fails
     */
    List<ManagedResource<?>> getActiveResources() throws WasmException;

    /**
     * Gets all active managed resources of a specific type.
     *
     * @param <T> the resource type
     * @param type the resource type classification
     * @return list of managed resources of the specified type
     * @throws WasmException if resource enumeration fails
     * @throws IllegalArgumentException if type is null
     */
    <T> List<ManagedResource<T>> getActiveResources(final ResourceType type) throws WasmException;

    /**
     * Gets comprehensive resource metrics and statistics.
     *
     * @return current resource metrics
     */
    ResourceMetrics getMetrics();

    /**
     * Gets the resource pool for shared resource management.
     *
     * @return the resource pool instance
     */
    ResourcePool getResourcePool();

    /**
     * Gets the resource cache for caching frequently used resources.
     *
     * @return the resource cache instance
     */
    ResourceCache getResourceCache();

    /**
     * Gets the resource monitor for real-time monitoring.
     *
     * @return the resource monitor instance
     */
    ResourceMonitor getResourceMonitor();

    /**
     * Gets the memory manager for memory-related resource management.
     *
     * @return the memory manager instance
     */
    MemoryManager getMemoryManager();

    /**
     * Performs manual cleanup of inactive or expired resources.
     *
     * @return the number of resources that were cleaned up
     * @throws WasmException if cleanup fails
     */
    int cleanupExpiredResources() throws WasmException;

    /**
     * Performs asynchronous cleanup of inactive or expired resources.
     *
     * @return future containing the number of resources that were cleaned up
     */
    CompletableFuture<Integer> cleanupExpiredResourcesAsync();

    /**
     * Forces cleanup of all resources matching the given criteria.
     *
     * @param criteria the cleanup criteria
     * @return the number of resources that were cleaned up
     * @throws WasmException if forced cleanup fails
     * @throws IllegalArgumentException if criteria is null
     */
    int forceCleanup(final ResourceCleanupCriteria criteria) throws WasmException;

    /**
     * Validates all managed resources and their state.
     *
     * @throws WasmException if validation fails with details about invalid resources
     */
    void validateResources() throws WasmException;

    /**
     * Gets detailed resource utilization information.
     *
     * @return map of resource names to their utilization data
     * @throws WasmException if utilization collection fails
     */
    Map<String, ResourceUtilization> getResourceUtilization() throws WasmException;

    /**
     * Sets new resource limits for the manager.
     *
     * @param limits the new resource limits
     * @throws WasmException if limits cannot be applied
     * @throws IllegalArgumentException if limits is null
     */
    void setResourceLimits(final ResourceLimits limits) throws WasmException;

    /**
     * Gets the current resource limits.
     *
     * @return the current resource limits
     */
    ResourceLimits getResourceLimits();

    /**
     * Registers a resource lifecycle listener.
     *
     * @param listener the listener to register
     * @throws IllegalArgumentException if listener is null
     */
    void addResourceLifecycleListener(final ResourceLifecycleListener listener);

    /**
     * Unregisters a resource lifecycle listener.
     *
     * @param listener the listener to unregister
     * @throws IllegalArgumentException if listener is null
     */
    void removeResourceLifecycleListener(final ResourceLifecycleListener listener);

    /**
     * Checks if the resource manager is still operational.
     *
     * @return true if the manager is operational, false otherwise
     */
    boolean isOperational();

    /**
     * Gets the number of active managed resources.
     *
     * @return the number of active managed resources
     */
    int getActiveResourceCount();

    /**
     * Gets the number of active managed resources of a specific type.
     *
     * @param type the resource type
     * @return the number of active managed resources of the specified type
     * @throws IllegalArgumentException if type is null
     */
    int getActiveResourceCount(final ResourceType type);

    /**
     * Gracefully shuts down the resource manager.
     *
     * <p>This method will attempt to cleanly release all managed resources and stop all
     * background cleanup processes. It will block until all resources are released or
     * the shutdown timeout is exceeded.
     *
     * @param timeout the maximum time to wait for shutdown
     * @throws WasmException if shutdown fails or times out
     * @throws IllegalArgumentException if timeout is null or negative
     */
    void shutdown(final Duration timeout) throws WasmException;

    /**
     * Asynchronously shuts down the resource manager.
     *
     * @param timeout the maximum time to wait for shutdown
     * @return future that completes when shutdown is finished
     * @throws IllegalArgumentException if timeout is null or negative
     */
    CompletableFuture<Void> shutdownAsync(final Duration timeout);

    /**
     * Closes the resource manager and releases all managed resources immediately.
     *
     * <p>This method performs immediate cleanup without waiting for graceful shutdown.
     * It should be used in try-with-resources blocks or when immediate resource release
     * is required.
     */
    @Override
    void close();
}
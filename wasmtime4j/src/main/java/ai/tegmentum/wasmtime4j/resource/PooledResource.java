package ai.tegmentum.wasmtime4j.resource;

import java.time.Instant;

/**
 * A resource that has been acquired from a resource pool.
 *
 * <p>PooledResource wraps a resource obtained from a ResourcePool and provides
 * automatic return-to-pool functionality when the resource is no longer needed.
 * It tracks usage statistics and ensures proper pool lifecycle management.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (PooledResource<Module> pooled = resourcePool.acquire(ResourceType.MODULE)) {
 *     Module module = pooled.getResource();
 *     Instance instance = module.instantiate();
 *     // Resource automatically returned to pool when block exits
 * }
 * }</pre>
 *
 * @param <T> the type of the pooled resource
 * @since 1.0.0
 */
public interface PooledResource<T> extends AutoCloseable {

    /**
     * Gets the underlying pooled resource.
     *
     * @return the pooled resource
     * @throws IllegalStateException if the resource has been returned to the pool
     */
    T getResource();

    /**
     * Gets the resource type of the pooled resource.
     *
     * @return the resource type
     */
    ResourceType getResourceType();

    /**
     * Gets the pool that this resource belongs to.
     *
     * @return the resource pool
     */
    ResourcePool getPool();

    /**
     * Gets the time when this resource was acquired from the pool.
     *
     * @return the acquisition time
     */
    Instant getAcquisitionTime();

    /**
     * Gets the number of times this resource has been acquired from the pool.
     *
     * @return the acquisition count
     */
    long getAcquisitionCount();

    /**
     * Gets the total time this resource has been active (checked out from pool).
     *
     * @return the total active time in milliseconds
     */
    long getTotalActiveTime();

    /**
     * Checks if this resource is still valid and acquired from the pool.
     *
     * @return true if the resource is valid and acquired, false otherwise
     */
    boolean isAcquired();

    /**
     * Marks this resource as invalid so it won't be returned to the pool.
     *
     * <p>This should be called when the resource is known to be in an unusable
     * state and should be discarded rather than returned to the pool.
     */
    void markInvalid();

    /**
     * Checks if this resource has been marked as invalid.
     *
     * @return true if the resource is invalid, false otherwise
     */
    boolean isInvalid();

    /**
     * Returns the resource to the pool.
     *
     * <p>After calling this method, the resource becomes unavailable and
     * subsequent calls to {@link #getResource()} will throw IllegalStateException.
     */
    void returnToPool();

    /**
     * Closes the pooled resource by returning it to the pool.
     *
     * <p>This method is called automatically when used in try-with-resources blocks.
     */
    @Override
    void close();
}
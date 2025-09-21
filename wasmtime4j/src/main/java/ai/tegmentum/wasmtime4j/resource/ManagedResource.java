package ai.tegmentum.wasmtime4j.resource;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Instant;
import java.util.Optional;

/**
 * A managed resource wrapper that provides lifecycle control and automatic cleanup.
 *
 * <p>ManagedResource wraps any resource type and provides comprehensive lifecycle management
 * including automatic cleanup, access tracking, and resource state monitoring.
 *
 * <p>Resources managed by this wrapper are automatically cleaned up when: - The try-with-resources
 * block exits - The resource TTL expires - The resource manager performs cleanup - The resource is
 * explicitly released
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (ManagedResource<Module> managedModule = resourceManager.manage(
 *         engine.compileModule(wasmBytes),
 *         ResourceType.MODULE,
 *         ResourcePolicy.defaultPolicy())) {
 *
 *     Module module = managedModule.get();
 *     Instance instance = module.instantiate();
 *
 *     // Automatic cleanup when block exits
 * }
 * }</pre>
 *
 * @param <T> the type of the managed resource
 * @since 1.0.0
 */
public interface ManagedResource<T> extends AutoCloseable {

  /**
   * Gets the underlying resource.
   *
   * @return the managed resource
   * @throws IllegalStateException if the resource has been released or is invalid
   */
  T get();

  /**
   * Gets the underlying resource if it's still valid.
   *
   * @return the managed resource, or empty if released or invalid
   */
  Optional<T> getIfValid();

  /**
   * Gets the resource name assigned during management.
   *
   * @return the resource name, or empty if no name was assigned
   */
  Optional<String> getName();

  /**
   * Gets the resource type classification.
   *
   * @return the resource type
   */
  ResourceType getType();

  /**
   * Gets the resource management policy.
   *
   * @return the resource policy
   */
  ResourcePolicy getPolicy();

  /**
   * Gets the resource state information.
   *
   * @return the current resource state
   */
  ResourceState getState();

  /**
   * Gets the creation timestamp of the managed resource.
   *
   * @return the creation time
   */
  Instant getCreationTime();

  /**
   * Gets the last access timestamp of the managed resource.
   *
   * @return the last access time
   */
  Instant getLastAccessTime();

  /**
   * Gets the number of times this resource has been accessed.
   *
   * @return the access count
   */
  long getAccessCount();

  /**
   * Gets the estimated memory usage of this resource.
   *
   * @return the estimated memory usage in bytes
   */
  long getEstimatedMemoryUsage();

  /**
   * Checks if the resource is still valid and accessible.
   *
   * @return true if the resource is valid, false otherwise
   */
  boolean isValid();

  /**
   * Checks if the resource has been released.
   *
   * @return true if the resource has been released, false otherwise
   */
  boolean isReleased();

  /**
   * Checks if the resource has expired based on its TTL policy.
   *
   * @return true if the resource has expired, false otherwise
   */
  boolean isExpired();

  /**
   * Forces immediate refresh of the resource state.
   *
   * <p>This method updates the resource state by checking the underlying resource's validity and
   * updating access timestamps.
   *
   * @throws WasmException if state refresh fails
   */
  void refreshState() throws WasmException;

  /**
   * Extends the resource TTL by the specified duration.
   *
   * <p>This method can be used to keep a resource alive longer than its original TTL. The extension
   * is applied from the current time, not from the original expiration time.
   *
   * @param extensionDuration the duration to extend the TTL
   * @throws WasmException if TTL extension fails
   * @throws IllegalArgumentException if extensionDuration is null or negative
   */
  void extendTtl(final java.time.Duration extensionDuration) throws WasmException;

  /**
   * Gets metadata about the managed resource.
   *
   * @return the resource metadata
   */
  ResourceMetadata getMetadata();

  /**
   * Manually marks the resource for cleanup.
   *
   * <p>After calling this method, the resource will be eligible for cleanup during the next cleanup
   * cycle, even if it hasn't expired.
   */
  void markForCleanup();

  /**
   * Cancels any pending cleanup for this resource.
   *
   * <p>This method can be used to prevent cleanup of a resource that was previously marked for
   * cleanup, as long as the cleanup hasn't started yet.
   *
   * @return true if cleanup was successfully cancelled, false if cleanup was already in progress
   */
  boolean cancelCleanup();

  /**
   * Releases the managed resource immediately.
   *
   * <p>After calling this method, the resource becomes invalid and cannot be accessed. Any
   * subsequent calls to {@link #get()} will throw IllegalStateException.
   *
   * @throws WasmException if resource release fails
   */
  void release() throws WasmException;

  /**
   * Closes the managed resource and releases it.
   *
   * <p>This method is called automatically when used in try-with-resources blocks. It performs the
   * same operation as {@link #release()} but doesn't throw checked exceptions.
   */
  @Override
  void close();
}

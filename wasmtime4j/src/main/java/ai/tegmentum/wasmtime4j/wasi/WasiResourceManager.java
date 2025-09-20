package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for WASI Preview 2 resource management.
 *
 * <p>WasiResourceManager provides centralized management of WASI resources including their
 * lifecycle, access control, and cleanup. It ensures proper resource isolation and prevents
 * resource leaks in component execution environments.
 *
 * <p>The resource manager implements capability-based security where resources must be explicitly
 * granted to components and can be revoked as needed.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiResourceManager manager = WasiResourceManager.create();
 *
 * // Create a filesystem resource
 * WasiResource filesystem = manager.createResource(
 *     WasiResource.Type.FILESYSTEM,
 *     FilesystemResourceConfig.builder()
 *         .withRootPath("/sandbox")
 *         .withPermissions(WasiFilePermissions.READ_WRITE)
 *         .build()
 * );
 *
 * // Grant access to a component
 * manager.grantAccess(component, "filesystem", filesystem);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiResourceManager {

  /**
   * Creates a new resource manager with default configuration.
   *
   * @return a new resource manager instance
   * @throws WasmException if creation fails
   */
  static WasiResourceManager create() throws WasmException {
    return new ai.tegmentum.wasmtime4j.wasi.impl.WasiResourceManagerImpl();
  }

  /**
   * Creates a new resource manager with specific limits.
   *
   * @param limits initial resource limits
   * @return a new resource manager instance
   * @throws WasmException if creation fails
   * @throws IllegalArgumentException if limits is null
   */
  static WasiResourceManager create(final WasiResourceLimits limits) throws WasmException {
    return new ai.tegmentum.wasmtime4j.wasi.impl.WasiResourceManagerImpl(limits);
  }

  /**
   * Creates a new resource with the specified type and configuration.
   *
   * @param <T> the resource type
   * @param type the resource type class
   * @param config the resource configuration
   * @return the created resource instance
   * @throws WasmException if resource creation fails
   * @throws IllegalArgumentException if type or config is null
   */
  <T extends WasiResource> T createResource(final Class<T> type, final WasiResourceConfig config)
      throws WasmException;

  /**
   * Creates a new resource with the specified name, type, and configuration.
   *
   * @param <T> the resource type
   * @param name the resource name
   * @param type the resource type class
   * @param config the resource configuration
   * @return the created resource instance
   * @throws WasmException if resource creation fails
   * @throws IllegalArgumentException if any parameter is null or name is empty
   */
  <T extends WasiResource> T createResource(
      final String name, final Class<T> type, final WasiResourceConfig config) throws WasmException;

  /**
   * Gets an existing resource by name.
   *
   * @param name the resource name
   * @return the resource instance, or empty if not found
   * @throws WasmException if resource lookup fails
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<WasiResource> getResource(final String name) throws WasmException;

  /**
   * Gets an existing resource by name and type.
   *
   * @param <T> the expected resource type
   * @param name the resource name
   * @param type the expected resource type class
   * @return the resource instance, or empty if not found or wrong type
   * @throws WasmException if resource lookup fails
   * @throws IllegalArgumentException if name is null/empty or type is null
   */
  <T extends WasiResource> Optional<T> getResource(final String name, final Class<T> type)
      throws WasmException;

  /**
   * Releases a resource and removes it from management.
   *
   * <p>After releasing a resource, it will be closed and can no longer be accessed by components.
   * Any existing references to the resource will become invalid.
   *
   * @param resource the resource to release
   * @throws WasmException if resource release fails
   * @throws IllegalArgumentException if resource is null
   */
  void releaseResource(final WasiResource resource) throws WasmException;

  /**
   * Releases a resource by name.
   *
   * @param name the resource name to release
   * @throws WasmException if resource release fails
   * @throws IllegalArgumentException if name is null or empty
   */
  void releaseResource(final String name) throws WasmException;

  /**
   * Gets all managed resources.
   *
   * @return list of all active resources
   * @throws WasmException if resource enumeration fails
   */
  List<WasiResource> getActiveResources() throws WasmException;

  /**
   * Gets all managed resources by type.
   *
   * @param <T> the resource type
   * @param type the resource type class
   * @return list of resources of the specified type
   * @throws WasmException if resource enumeration fails
   * @throws IllegalArgumentException if type is null
   */
  <T extends WasiResource> List<T> getActiveResources(final Class<T> type) throws WasmException;

  /**
   * Gets resource usage statistics.
   *
   * <p>Returns information about resource allocation, usage patterns, and performance metrics for
   * monitoring and optimization purposes.
   *
   * @return resource usage statistics
   */
  WasiResourceUsageStats getUsageStats();

  /**
   * Sets resource limits for the manager.
   *
   * <p>Resource limits control the maximum number of resources that can be created and their
   * individual resource consumption.
   *
   * @param limits the resource limits to apply
   * @throws WasmException if limits cannot be applied
   * @throws IllegalArgumentException if limits is null
   */
  void setResourceLimits(final WasiResourceLimits limits) throws WasmException;

  /**
   * Gets the current resource limits.
   *
   * @return the current resource limits
   */
  WasiResourceLimits getResourceLimits();

  /**
   * Validates all managed resources.
   *
   * <p>Performs comprehensive validation of all active resources, checking their state,
   * configuration, and accessibility.
   *
   * @throws WasmException if validation fails with details about invalid resources
   */
  void validateResources() throws WasmException;

  /**
   * Performs cleanup of inactive or invalid resources.
   *
   * <p>Automatically releases resources that are no longer valid or accessible, freeing up system
   * resources and preventing leaks.
   *
   * @return the number of resources that were cleaned up
   * @throws WasmException if cleanup fails
   */
  int cleanupResources() throws WasmException;

  /**
   * Gets metadata about managed resources.
   *
   * <p>Returns detailed information about resource types, configurations, and usage patterns for
   * debugging and monitoring purposes.
   *
   * @return map of resource names to their metadata
   * @throws WasmException if metadata collection fails
   */
  Map<String, WasiResourceMetadata> getResourceMetadata() throws WasmException;

  /**
   * Checks if a resource with the specified name exists.
   *
   * @param name the resource name to check
   * @return true if the resource exists, false otherwise
   * @throws WasmException if lookup fails
   * @throws IllegalArgumentException if name is null or empty
   */
  boolean hasResource(final String name) throws WasmException;

  /**
   * Gets the number of active resources.
   *
   * @return the number of active resources
   */
  int getActiveResourceCount();

  /**
   * Gets the number of active resources of a specific type.
   *
   * @param type the resource type class
   * @return the number of active resources of the specified type
   * @throws IllegalArgumentException if type is null
   */
  int getActiveResourceCount(final Class<? extends WasiResource> type);

  /**
   * Checks if the resource manager is still valid and usable.
   *
   * @return true if the manager is valid, false otherwise
   */
  boolean isValid();

  /**
   * Closes the resource manager and releases all managed resources.
   *
   * <p>After calling this method, the manager becomes invalid and should not be used. All managed
   * resources will be released and closed.
   */
  void close();
}

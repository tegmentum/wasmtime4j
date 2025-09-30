package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Interface for WASI component resource management.
 *
 * <p>Resources in the WASI component model represent external entities that components can interact
 * with, such as files, network connections, timers, and custom resource types. Resources have
 * well-defined lifecycles and ownership semantics.
 *
 * <p>Resources are created and managed by component instances and can be passed between components
 * through interface calls. Each resource has a unique identifier within its owning component.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Get a resource from component interaction
 * WasiResource fileResource = instance.call("open-file", "/tmp/data.txt");
 *
 * // Use the resource
 * String resourceType = fileResource.getType();
 * boolean isOwned = fileResource.isOwned();
 *
 * // Resources are auto-closeable
 * try (WasiResource resource = fileResource) {
 *     // Use resource...
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiResource extends Closeable {

  /**
   * Gets the unique identifier for this resource.
   *
   * <p>Resource IDs are unique within the context of their owning component instance. They can be
   * used to track and manage resource usage.
   *
   * @return the unique resource identifier
   */
  long getId();

  /**
   * Gets the type name of this resource.
   *
   * <p>Resource types define the interface and operations available for a resource. Common types
   * include "wasi:filesystem/file", "wasi:sockets/tcp-socket", etc.
   *
   * @return the resource type name
   */
  String getType();

  /**
   * Gets the component instance that owns this resource.
   *
   * <p>Resources are owned by specific component instances and their lifecycle is tied to that
   * instance.
   *
   * @return the owning component instance
   */
  WasiInstance getOwner();

  /**
   * Checks if this resource is owned by the current component.
   *
   * <p>Owned resources can be freely manipulated and closed by the component. Borrowed resources
   * have restricted operations and cannot be closed directly.
   *
   * @return true if the resource is owned, false if borrowed
   */
  boolean isOwned();

  /**
   * Checks if the resource is still valid and usable.
   *
   * <p>Resources become invalid when they are closed, their owner is destroyed, or underlying
   * system resources are no longer available.
   *
   * @return true if the resource is valid, false otherwise
   */
  boolean isValid();

  /**
   * Gets the creation timestamp of this resource.
   *
   * @return when the resource was created
   */
  Instant getCreatedAt();

  /**
   * Gets the last access timestamp of this resource.
   *
   * @return when the resource was last accessed, or empty if never accessed
   */
  Optional<Instant> getLastAccessedAt();

  /**
   * Gets resource-specific metadata and properties.
   *
   * <p>The metadata contains resource-type-specific information such as file size, socket state,
   * timer settings, etc. The structure depends on the resource type.
   *
   * @return resource metadata
   * @throws WasmException if metadata cannot be retrieved or resource is invalid
   */
  WasiResourceMetadata getMetadata() throws WasmException;

  /**
   * Gets the current state of the resource.
   *
   * <p>Resource states are type-specific and indicate the current condition of the resource (e.g.,
   * open/closed for files, connected/disconnected for sockets).
   *
   * @return the current resource state
   * @throws WasmException if state cannot be retrieved or resource is invalid
   */
  WasiResourceState getState() throws WasmException;

  /**
   * Gets usage statistics for this resource.
   *
   * <p>Statistics include access counts, data transfer amounts, error counts, and other
   * type-specific metrics useful for monitoring and debugging.
   *
   * @return resource usage statistics
   */
  WasiResourceStats getStats();

  /**
   * Performs a type-specific operation on this resource.
   *
   * <p>This provides a generic interface for resource-specific operations. The available operations
   * depend on the resource type and current state.
   *
   * @param operation the operation name
   * @param parameters operation parameters
   * @return operation result, type depends on the specific operation
   * @throws WasmException if the operation fails or is not supported
   * @throws IllegalArgumentException if operation or parameters are invalid
   */
  Object invoke(final String operation, final Object... parameters) throws WasmException;

  /**
   * Lists available operations for this resource type.
   *
   * <p>Returns a list of operation names that can be used with {@link #invoke}. The list may change
   * based on the resource's current state.
   *
   * @return list of available operation names
   */
  List<String> getAvailableOperations();

  /**
   * Creates a representation of this resource that can be passed to other components.
   *
   * <p>Resource handles can be passed between components as interface parameters. The receiving
   * component gets appropriate access to the resource based on ownership and permissions.
   *
   * @return a handle representing this resource
   * @throws WasmException if a handle cannot be created or resource is invalid
   */
  WasiResourceHandle createHandle() throws WasmException;

  /**
   * Transfers ownership of this resource to another component.
   *
   * <p>Ownership transfer is only possible for owned resources. After transfer, this resource
   * becomes invalid in the current component and the target component gains ownership.
   *
   * @param targetInstance the component instance to transfer ownership to
   * @throws WasmException if transfer fails or is not allowed
   * @throws IllegalArgumentException if targetInstance is null
   * @throws IllegalStateException if the resource is not owned or already invalid
   */
  void transferOwnership(final WasiInstance targetInstance) throws WasmException;

  /**
   * Closes the resource and releases associated system resources.
   *
   * <p>Only owned resources can be closed directly. Borrowed resources are closed automatically
   * when their owner is destroyed or explicitly closes them.
   *
   * <p>After closing, the resource becomes invalid and should not be used.
   *
   * <p>If cleanup encounters errors, they are logged but do not prevent the resource from being
   * closed.
   *
   * @throws IllegalStateException if the resource is not owned
   */
  @Override
  void close();
}

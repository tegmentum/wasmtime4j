package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for instantiated WASI component operations.
 *
 * <p>A WasiInstance represents a running instance of a WASI component with its own isolated state,
 * memory, and resources. Instances provide access to exported functions and interfaces, manage
 * component lifecycle, and handle resource allocation.
 *
 * <p>Component instances are created from {@link WasiComponent} objects and configured with {@link
 * WasiConfig} settings. Each instance has its own execution context and resource namespace.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (WasiInstance instance = component.instantiate(config)) {
 *     // Call exported function
 *     Map<String, Object> result = instance.call("process-data", inputData);
 *
 *     // Access resources
 *     List<WasiResource> resources = instance.getResources();
 *
 *     // Monitor execution
 *     WasiInstanceStats stats = instance.getStats();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiInstance extends Closeable {

  /**
   * Gets the unique identifier for this instance.
   *
   * <p>Instance IDs are unique within the context of their creating component and can be used for
   * tracking and debugging purposes.
   *
   * @return the unique instance identifier
   */
  long getId();

  /**
   * Gets the component that created this instance.
   *
   * @return the parent component
   */
  WasiComponent getComponent();

  /**
   * Gets the configuration used to create this instance.
   *
   * @return the instance configuration
   */
  WasiConfig getConfig();

  /**
   * Gets the current state of the instance.
   *
   * <p>Instance states track the lifecycle from creation through execution to termination. Possible
   * states include CREATED, RUNNING, SUSPENDED, TERMINATED, and ERROR.
   *
   * @return the current instance state
   */
  WasiInstanceState getState();

  /**
   * Gets the creation timestamp of this instance.
   *
   * @return when the instance was created
   */
  Instant getCreatedAt();

  /**
   * Gets the last activity timestamp of this instance.
   *
   * @return when the instance was last active, or empty if never active
   */
  Optional<Instant> getLastActivityAt();

  /**
   * Calls an exported function from the component.
   *
   * <p>This is the primary method for invoking component functionality. Parameters and return
   * values are automatically marshaled between Java and component types according to the interface
   * specification.
   *
   * @param functionName the name of the exported function to call
   * @param parameters function parameters in the order expected by the component
   * @return function return value, or null for void functions
   * @throws WasmException if the function call fails or function doesn't exist
   * @throws IllegalArgumentException if functionName is null or parameters are invalid
   * @throws IllegalStateException if the instance is not in a callable state
   */
  Object call(final String functionName, final Object... parameters) throws WasmException;

  /**
   * Calls an exported function with a timeout.
   *
   * <p>If the function doesn't complete within the specified timeout, execution is terminated and
   * an exception is thrown.
   *
   * @param functionName the name of the exported function to call
   * @param timeout maximum execution time
   * @param parameters function parameters in the order expected by the component
   * @return function return value, or null for void functions
   * @throws WasmException if the function call fails, times out, or function doesn't exist
   * @throws IllegalArgumentException if functionName is null, timeout is invalid, or parameters are
   *     invalid
   * @throws IllegalStateException if the instance is not in a callable state
   */
  Object call(final String functionName, final Duration timeout, final Object... parameters)
      throws WasmException;

  /**
   * Calls an exported function asynchronously.
   *
   * <p>This method allows non-blocking execution of component functions. The returned future
   * completes when the function execution finishes, either successfully or with an error.
   *
   * @param functionName the name of the exported function to call
   * @param parameters function parameters in the order expected by the component
   * @return CompletableFuture that resolves to the function result
   * @throws IllegalArgumentException if functionName is null or parameters are invalid
   * @throws IllegalStateException if the instance is not in a callable state
   */
  CompletableFuture<Object> callAsync(final String functionName, final Object... parameters);

  /**
   * Gets the names of all exported functions.
   *
   * @return list of exported function names
   * @throws WasmException if exports cannot be retrieved
   */
  List<String> getExportedFunctions() throws WasmException;

  /**
   * Gets the names of all exported interfaces.
   *
   * @return list of exported interface names
   * @throws WasmException if exports cannot be retrieved
   */
  List<String> getExportedInterfaces() throws WasmException;

  /**
   * Gets metadata for an exported function.
   *
   * <p>Returns detailed information about function signatures, parameter types, and return types.
   *
   * @param functionName the function name to inspect
   * @return function metadata
   * @throws WasmException if function doesn't exist or metadata cannot be retrieved
   * @throws IllegalArgumentException if functionName is null or empty
   */
  WasiFunctionMetadata getFunctionMetadata(final String functionName) throws WasmException;

  /**
   * Gets all resources owned by this instance.
   *
   * <p>Returns resources that have been created by or transferred to this instance. Resources are
   * automatically cleaned up when the instance is closed.
   *
   * @return list of owned resources
   */
  List<WasiResource> getResources();

  /**
   * Gets resources of a specific type owned by this instance.
   *
   * @param resourceType the resource type to filter by
   * @return list of resources of the specified type
   * @throws IllegalArgumentException if resourceType is null or empty
   */
  List<WasiResource> getResources(final String resourceType);

  /**
   * Gets a specific resource by its identifier.
   *
   * @param resourceId the resource identifier
   * @return the resource with the specified ID, or empty if not found
   */
  Optional<WasiResource> getResource(final long resourceId);

  /**
   * Creates a new resource of the specified type.
   *
   * <p>This allows components to create new resources programmatically. The availability of
   * resource creation depends on the component's permissions and capabilities.
   *
   * @param resourceType the type of resource to create
   * @param parameters resource creation parameters
   * @return the newly created resource
   * @throws WasmException if resource creation fails
   * @throws IllegalArgumentException if resourceType is null or parameters are invalid
   */
  WasiResource createResource(final String resourceType, final Object... parameters)
      throws WasmException;

  /**
   * Gets comprehensive statistics about this instance.
   *
   * <p>Statistics include execution time, memory usage, resource counts, function call counts, and
   * other metrics useful for monitoring and debugging.
   *
   * @return instance statistics
   */
  WasiInstanceStats getStats();

  /**
   * Gets the current memory usage of this instance.
   *
   * <p>Returns information about allocated memory, peak usage, and memory limits.
   *
   * @return memory usage information
   */
  WasiMemoryInfo getMemoryInfo();

  /**
   * Suspends execution of this instance.
   *
   * <p>Suspended instances stop executing but retain their state and can be resumed later.
   * Suspension is useful for resource management and execution control.
   *
   * @throws WasmException if suspension fails
   * @throws IllegalStateException if the instance cannot be suspended in its current state
   */
  void suspend() throws WasmException;

  /**
   * Resumes execution of a suspended instance.
   *
   * <p>Resumed instances continue execution from where they were suspended.
   *
   * @throws WasmException if resumption fails
   * @throws IllegalStateException if the instance is not suspended
   */
  void resume() throws WasmException;

  /**
   * Terminates execution of this instance.
   *
   * <p>Terminated instances stop executing immediately and cannot be resumed. This is more forceful
   * than closing and may not allow proper cleanup.
   *
   * @throws WasmException if termination fails
   */
  void terminate() throws WasmException;

  /**
   * Checks if the instance is still valid and usable.
   *
   * <p>Instances become invalid when they are closed, terminated, or encounter fatal errors.
   *
   * @return true if the instance is valid, false otherwise
   */
  boolean isValid();

  /**
   * Checks if the instance is currently executing.
   *
   * @return true if the instance is currently executing a function call
   */
  boolean isExecuting();

  /**
   * Sets a custom property on this instance.
   *
   * <p>Custom properties allow storing arbitrary metadata associated with the instance. These can
   * be useful for application-specific tracking and debugging.
   *
   * @param key the property key
   * @param value the property value
   * @throws IllegalArgumentException if key is null or empty
   */
  void setProperty(final String key, final Object value);

  /**
   * Gets a custom property from this instance.
   *
   * @param key the property key
   * @return the property value, or empty if not set
   * @throws IllegalArgumentException if key is null or empty
   */
  Optional<Object> getProperty(final String key);

  /**
   * Gets all custom properties set on this instance.
   *
   * @return immutable map of property keys to values
   */
  Map<String, Object> getProperties();

  /**
   * Closes the instance and releases all associated resources.
   *
   * <p>After closing, the instance becomes invalid and should not be used. All owned resources are
   * automatically closed, and any ongoing execution is terminated.
   *
   * <p>This method is idempotent and can be called multiple times safely.
   *
   * <p>If cleanup encounters errors, they are logged but do not prevent the instance from being
   * closed.
   */
  @Override
  void close();
}

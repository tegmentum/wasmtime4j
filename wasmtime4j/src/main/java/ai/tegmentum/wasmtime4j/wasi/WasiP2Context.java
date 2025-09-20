package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.util.Map;

/**
 * Interface for WASI Preview 2 context management.
 *
 * <p>WasiP2Context provides the execution environment for WASI Preview 2 components, including
 * resource management, security policies, and capability control. This context enables fine-grained
 * control over component access to system resources and external capabilities.
 *
 * <p>WASI Preview 2 introduces a capability-based security model where components must be explicitly
 * granted access to resources through this context configuration.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (WasiP2Context context = WasiP2Context.builder()
 *     .withResourceLimits(limits)
 *     .withSecurityPolicy(policy)
 *     .build()) {
 *
 *   WasiComponent component = context.createComponent(wasmBytes);
 *   WasiInstance instance = component.instantiate(context.getConfig());
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiP2Context extends Closeable {

  /**
   * Creates a new WASI Preview 2 context builder.
   *
   * @return a new context builder
   */
  static WasiP2ContextBuilder builder() {
    throw new UnsupportedOperationException("WASI P2 context creation not yet implemented");
  }

  /**
   * Sets resource limits for this context.
   *
   * <p>Resource limits control memory usage, CPU time, file system access, and other system
   * resources that components can consume.
   *
   * @param limits the resource limits to apply
   * @throws WasmException if limits cannot be applied
   * @throws IllegalArgumentException if limits is null
   */
  void setResourceLimits(final WasiResourceLimits limits) throws WasmException;

  /**
   * Gets the current resource limits for this context.
   *
   * @return the current resource limits
   */
  WasiResourceLimits getResourceLimits();

  /**
   * Sets the security policy for this context.
   *
   * <p>Security policies define what capabilities and system resources components are allowed
   * to access, enabling fine-grained security control.
   *
   * @param policy the security policy to apply
   * @throws WasmException if policy cannot be applied
   * @throws IllegalArgumentException if policy is null
   */
  void setSecurityPolicy(final WasiSecurityPolicy policy) throws WasmException;

  /**
   * Gets the current security policy for this context.
   *
   * @return the current security policy
   */
  WasiSecurityPolicy getSecurityPolicy();

  /**
   * Gets all active resources managed by this context.
   *
   * <p>Returns a map of resource names to their corresponding resource instances. This can be
   * used for monitoring resource usage and debugging.
   *
   * @return map of resource names to WasiResource instances
   * @throws WasmException if resource enumeration fails
   */
  Map<String, WasiResource> getResources() throws WasmException;

  /**
   * Gets a specific resource by name.
   *
   * @param name the resource name to look up
   * @return the resource instance, or null if not found
   * @throws WasmException if resource lookup fails
   * @throws IllegalArgumentException if name is null or empty
   */
  WasiResource getResource(final String name) throws WasmException;

  /**
   * Creates a new resource with the specified configuration.
   *
   * <p>Creates and registers a new resource instance that can be accessed by components
   * instantiated with this context.
   *
   * @param name the resource name
   * @param config the resource configuration
   * @return the created resource instance
   * @throws WasmException if resource creation fails
   * @throws IllegalArgumentException if name is null/empty or config is null
   */
  WasiResource createResource(final String name, final WasiResourceConfig config) throws WasmException;

  /**
   * Releases a resource and removes it from this context.
   *
   * <p>After releasing a resource, it will no longer be available to components and any
   * references to it will become invalid.
   *
   * @param name the resource name to release
   * @throws WasmException if resource release fails
   * @throws IllegalArgumentException if name is null or empty
   */
  void releaseResource(final String name) throws WasmException;

  /**
   * Gets a list of all active resource names.
   *
   * @return list of active resource names
   * @throws WasmException if resource enumeration fails
   */
  java.util.List<String> getActiveResourceNames() throws WasmException;

  /**
   * Creates a WASI configuration suitable for component instantiation.
   *
   * <p>Generates a WasiConfig that incorporates the resource limits, security policy, and
   * resource mappings from this context.
   *
   * @return a WASI configuration for component instantiation
   * @throws WasmException if configuration generation fails
   */
  WasiConfig createConfig() throws WasmException;

  /**
   * Creates a component from WebAssembly bytes using this context.
   *
   * <p>Creates and validates a component using the security policy and resource constraints
   * defined in this context.
   *
   * @param wasmBytes the WebAssembly component bytes
   * @return a new component instance
   * @throws WasmException if component creation fails
   * @throws IllegalArgumentException if wasmBytes is null or empty
   */
  WasiComponent createComponent(final byte[] wasmBytes) throws WasmException;

  /**
   * Gets usage statistics for this context.
   *
   * <p>Returns information about resource usage, component instantiations, and performance
   * metrics for monitoring and optimization purposes.
   *
   * @return context usage statistics
   */
  WasiP2ContextStats getStats();

  /**
   * Validates that all required resources are available and properly configured.
   *
   * <p>Performs comprehensive validation of the context configuration, including resource
   * availability, security policy consistency, and limit validation.
   *
   * @throws WasmException if validation fails with details about specific issues
   */
  void validate() throws WasmException;

  /**
   * Checks if this context is still valid and usable.
   *
   * <p>Contexts become invalid when closed or when critical resources are no longer available.
   *
   * @return true if the context is valid and usable, false otherwise
   */
  boolean isValid();

  /**
   * Closes this context and releases all associated resources.
   *
   * <p>After calling this method, the context becomes invalid and should not be used. All
   * resources managed by this context will be released.
   */
  @Override
  void close();
}
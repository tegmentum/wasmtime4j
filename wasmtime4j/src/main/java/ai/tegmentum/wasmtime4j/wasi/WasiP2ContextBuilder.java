package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Map;

/**
 * Builder for creating WASI Preview 2 contexts with customized configuration.
 *
 * <p>WasiP2ContextBuilder provides a fluent API for configuring WASI Preview 2 contexts with
 * specific resource limits, security policies, and initial resource configurations.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiP2Context context = WasiP2Context.builder()
 *     .withResourceLimits(WasiResourceLimits.builder()
 *         .maxMemory(100 * 1024 * 1024) // 100MB
 *         .maxFileHandles(50)
 *         .build())
 *     .withSecurityPolicy(WasiSecurityPolicy.builder()
 *         .allowFileSystemAccess("/tmp", WasiFilePermissions.READ_WRITE)
 *         .allowNetworkAccess("localhost", 8080)
 *         .build())
 *     .withResource("filesystem", filesystemConfig)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiP2ContextBuilder {

  /**
   * Sets the resource limits for the context.
   *
   * @param limits the resource limits to apply
   * @return this builder for method chaining
   * @throws IllegalArgumentException if limits is null
   */
  WasiP2ContextBuilder withResourceLimits(final WasiResourceLimits limits);

  /**
   * Sets the security policy for the context.
   *
   * @param policy the security policy to apply
   * @return this builder for method chaining
   * @throws IllegalArgumentException if policy is null
   */
  WasiP2ContextBuilder withSecurityPolicy(final WasiSecurityPolicy policy);

  /**
   * Adds a resource configuration to the context.
   *
   * @param name the resource name
   * @param config the resource configuration
   * @return this builder for method chaining
   * @throws IllegalArgumentException if name is null/empty or config is null
   */
  WasiP2ContextBuilder withResource(final String name, final WasiResourceConfig config);

  /**
   * Adds multiple resource configurations to the context.
   *
   * @param resources map of resource names to their configurations
   * @return this builder for method chaining
   * @throws IllegalArgumentException if resources is null or contains invalid entries
   */
  WasiP2ContextBuilder withResources(final Map<String, WasiResourceConfig> resources);

  /**
   * Sets the context name for debugging and monitoring.
   *
   * @param name the context name
   * @return this builder for method chaining
   * @throws IllegalArgumentException if name is null or empty
   */
  WasiP2ContextBuilder withName(final String name);

  /**
   * Enables debug mode for additional logging and validation.
   *
   * @param enabled true to enable debug mode, false to disable
   * @return this builder for method chaining
   */
  WasiP2ContextBuilder withDebugMode(final boolean enabled);

  /**
   * Sets custom context properties.
   *
   * @param properties map of property names to values
   * @return this builder for method chaining
   * @throws IllegalArgumentException if properties is null
   */
  WasiP2ContextBuilder withProperties(final Map<String, Object> properties);

  /**
   * Sets a custom context property.
   *
   * @param key the property key
   * @param value the property value
   * @return this builder for method chaining
   * @throws IllegalArgumentException if key is null or empty
   */
  WasiP2ContextBuilder withProperty(final String key, final Object value);

  /**
   * Validates the builder configuration.
   *
   * <p>Checks that all required settings are present and that the configuration is consistent and
   * valid for creating a WASI Preview 2 context.
   *
   * @throws WasmException if validation fails with details about specific issues
   */
  void validate() throws WasmException;

  /**
   * Builds and returns a new WASI Preview 2 context with the configured settings.
   *
   * <p>Creates a new context instance using all the configuration settings provided to this
   * builder. The builder can be reused to create multiple contexts with the same configuration.
   *
   * @return a new WASI Preview 2 context
   * @throws WasmException if context creation fails
   */
  WasiP2Context build() throws WasmException;
}

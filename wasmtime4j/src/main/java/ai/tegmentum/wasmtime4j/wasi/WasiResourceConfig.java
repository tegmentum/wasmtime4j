package ai.tegmentum.wasmtime4j.wasi;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for WASI resource configuration.
 *
 * <p>WasiResourceConfig defines the configuration parameters for creating and managing WASI
 * resources. Different resource types require different configuration options, and this interface
 * provides a flexible way to specify resource-specific settings.
 *
 * <p>Resource configurations are immutable after creation to ensure consistent resource behavior
 * throughout their lifecycle.
 *
 * @since 1.0.0
 */
public interface WasiResourceConfig {

  /**
   * Gets the resource type this configuration is for.
   *
   * @return the resource type
   */
  WasiResourceType getResourceType();

  /**
   * Gets the resource name.
   *
   * @return the resource name, or empty if not specified
   */
  Optional<String> getName();

  /**
   * Gets a configuration property by name.
   *
   * @param name the property name
   * @return the property value, or empty if not set
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<Object> getProperty(final String name);

  /**
   * Gets all configuration properties.
   *
   * @return map of property names to their values
   */
  Map<String, Object> getProperties();

  /**
   * Checks if a property is set.
   *
   * @param name the property name to check
   * @return true if the property is set, false otherwise
   * @throws IllegalArgumentException if name is null or empty
   */
  boolean hasProperty(final String name);

  /**
   * Gets the resource permissions.
   *
   * @return the resource permissions
   */
  WasiResourcePermissions getPermissions();

  /**
   * Gets the resource limits.
   *
   * @return the resource limits
   */
  WasiResourceLimits getResourceLimits();

  /**
   * Gets additional metadata for this configuration.
   *
   * @return configuration metadata
   */
  Map<String, Object> getMetadata();

  /**
   * Validates this configuration for correctness and completeness.
   *
   * @throws IllegalArgumentException if validation fails with details about specific issues
   */
  void validate();

  /**
   * Checks if this configuration is compatible with another configuration.
   *
   * @param other the configuration to check compatibility with
   * @return true if configurations are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final WasiResourceConfig other);
}

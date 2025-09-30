package ai.tegmentum.wasmtime4j.wasi;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Metadata information about a WASI resource.
 *
 * <p>Resource metadata provides detailed information about resource properties, capabilities, and
 * current state. The metadata structure depends on the specific resource type.
 *
 * @since 1.0.0
 */
public interface WasiResourceMetadata {

  /**
   * Gets the resource type name.
   *
   * @return the resource type
   */
  String getResourceType();

  /**
   * Gets the resource identifier.
   *
   * @return the unique resource identifier
   */
  long getResourceId();

  /**
   * Gets the resource creation timestamp.
   *
   * @return when the resource was created
   */
  Instant getCreatedAt();

  /**
   * Gets the last modification timestamp.
   *
   * @return when the resource was last modified, or empty if never modified
   */
  Optional<Instant> getLastModifiedAt();

  /**
   * Gets the resource size if applicable.
   *
   * @return resource size in bytes, or empty if not applicable
   */
  Optional<Long> getSize();

  /**
   * Gets additional resource-specific properties.
   *
   * @return map of property names to values
   */
  Map<String, Object> getProperties();

  /**
   * Checks if the resource supports a specific capability.
   *
   * @param capability the capability name to check
   * @return true if the capability is supported, false otherwise
   */
  boolean hasCapability(final String capability);
}

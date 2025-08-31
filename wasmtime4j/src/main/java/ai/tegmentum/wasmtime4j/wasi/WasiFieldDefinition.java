package ai.tegmentum.wasmtime4j.wasi;

import java.util.Optional;

/**
 * Definition of a field in a WASI record type.
 *
 * @since 1.0.0
 */
public interface WasiFieldDefinition {

  /**
   * Gets the field name.
   *
   * @return the field name
   */
  String getName();

  /**
   * Gets the field type.
   *
   * @return the field type metadata
   */
  WasiTypeMetadata getType();

  /**
   * Gets the field documentation.
   *
   * @return field documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Checks if this field is optional.
   *
   * @return true if optional, false if required
   */
  boolean isOptional();
}
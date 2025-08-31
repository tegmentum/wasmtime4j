package ai.tegmentum.wasmtime4j.wasi;

import java.util.Optional;

/**
 * Metadata for a function parameter.
 *
 * @since 1.0.0
 */
public interface WasiParameterMetadata {

  /**
   * Gets the parameter name.
   *
   * @return the parameter name
   */
  String getName();

  /**
   * Gets the parameter type.
   *
   * @return the parameter type metadata
   */
  WasiTypeMetadata getType();

  /**
   * Gets the parameter documentation.
   *
   * @return parameter documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Checks if this parameter is optional.
   *
   * @return true if optional, false if required
   */
  boolean isOptional();
}
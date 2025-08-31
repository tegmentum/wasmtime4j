package ai.tegmentum.wasmtime4j.wasi;

import java.util.Optional;

/**
 * Definition of a variant in a WASI variant type.
 *
 * @since 1.0.0
 */
public interface WasiVariantDefinition {

  /**
   * Gets the variant name.
   *
   * @return the variant name
   */
  String getName();

  /**
   * Gets the variant payload type if any.
   *
   * @return the payload type metadata, or empty if no payload
   */
  Optional<WasiTypeMetadata> getPayloadType();

  /**
   * Gets the variant documentation.
   *
   * @return variant documentation, or empty if not available
   */
  Optional<String> getDocumentation();
}
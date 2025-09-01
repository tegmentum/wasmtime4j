package ai.tegmentum.wasmtime4j.wasi;

import java.util.List;
import java.util.Optional;

/**
 * Definition of a custom type in a WASI interface.
 *
 * @since 1.0.0
 */
public interface WasiTypeDefinition {

  /**
   * Gets the name of the type.
   *
   * @return the type name
   */
  String getName();

  /**
   * Gets the kind of type (record, variant, enum, etc.).
   *
   * @return the type kind
   */
  String getKind();

  /**
   * Gets the documentation for this type.
   *
   * @return type documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Gets the fields for record types.
   *
   * @return list of field definitions, empty for non-record types
   */
  List<WasiFieldDefinition> getFields();

  /**
   * Gets the variants for variant types.
   *
   * @return list of variant definitions, empty for non-variant types
   */
  List<WasiVariantDefinition> getVariants();
}

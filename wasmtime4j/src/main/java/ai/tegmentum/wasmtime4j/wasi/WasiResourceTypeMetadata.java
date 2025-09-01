package ai.tegmentum.wasmtime4j.wasi;

import java.util.List;
import java.util.Optional;

/**
 * Metadata information about a WASI resource type.
 *
 * @since 1.0.0
 */
public interface WasiResourceTypeMetadata {

  /**
   * Gets the name of the resource type.
   *
   * @return the resource type name
   */
  String getName();

  /**
   * Gets the documentation for this resource type.
   *
   * @return resource type documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Gets the methods available on this resource type.
   *
   * @return list of method metadata
   */
  List<WasiFunctionMetadata> getMethods();

  /**
   * Checks if this resource type supports ownership transfer.
   *
   * @return true if ownership can be transferred, false otherwise
   */
  boolean supportsOwnershipTransfer();
}

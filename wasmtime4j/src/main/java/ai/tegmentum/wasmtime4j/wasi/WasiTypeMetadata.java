package ai.tegmentum.wasmtime4j.wasi;

import java.util.Optional;

/**
 * Metadata for a WASI type.
 *
 * @since 1.0.0
 */
public interface WasiTypeMetadata {

  /**
   * Gets the type name.
   *
   * @return the type name
   */
  String getName();

  /**
   * Gets the Java class that represents this type.
   *
   * @return the Java class, or empty if not mappable
   */
  Optional<Class<?>> getJavaClass();

  /**
   * Checks if this type is a primitive type.
   *
   * @return true if primitive, false otherwise
   */
  boolean isPrimitive();

  /**
   * Checks if this type is a resource type.
   *
   * @return true if resource, false otherwise
   */
  boolean isResource();
}
package ai.tegmentum.wasmtime4j.security;

/**
 * Security capability interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface Capability {

  /**
   * Gets the capability name.
   *
   * @return the capability name
   */
  String getCapabilityName();

  /**
   * Gets the capability type.
   *
   * @return the capability type
   */
  String getType();

  /**
   * Checks if the capability is granted.
   *
   * @return true if the capability is granted
   */
  boolean isGranted();

  /**
   * Gets the capability description.
   *
   * @return the capability description
   */
  String getDescription();
}

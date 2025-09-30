package ai.tegmentum.wasmtime4j.security;

/**
 * Security role interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface Role {

  /**
   * Gets the role name.
   *
   * @return the role name
   */
  String getRoleName();

  /**
   * Gets the role description.
   *
   * @return the role description
   */
  String getDescription();

  /**
   * Gets the role level.
   *
   * @return the role level
   */
  int getLevel();

  /**
   * Checks if this role is active.
   *
   * @return true if the role is active
   */
  boolean isActive();
}

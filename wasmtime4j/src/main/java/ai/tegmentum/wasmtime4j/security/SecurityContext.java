package ai.tegmentum.wasmtime4j.security;

import java.util.Set;

/**
 * Security context interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface SecurityContext {

  /**
   * Gets the context ID.
   *
   * @return the context ID
   */
  String getContextId();

  /**
   * Gets the user roles.
   *
   * @return the set of roles
   */
  Set<Role> getRoles();

  /**
   * Gets the capabilities.
   *
   * @return the set of capabilities
   */
  Set<Capability> getCapabilities();

  /**
   * Checks if the context has the specified role.
   *
   * @param role the role to check
   * @return true if the role is present
   */
  boolean hasRole(Role role);

  /**
   * Checks if the context has the specified capability.
   *
   * @param capability the capability to check
   * @return true if the capability is present
   */
  boolean hasCapability(Capability capability);
}

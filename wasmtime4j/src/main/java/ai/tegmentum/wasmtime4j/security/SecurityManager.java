package ai.tegmentum.wasmtime4j.security;

/**
 * Security manager interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface SecurityManager {

  /**
   * Initializes the security manager.
   *
   * @param policy the security policy
   */
  void initialize(SecurityPolicy policy);

  /**
   * Checks if access is granted for the given request.
   *
   * @param request the access request
   * @return true if access is granted
   */
  boolean checkAccess(AccessRequest request);

  /**
   * Gets the current security context.
   *
   * @return the security context
   */
  SecurityContext getCurrentContext();

  /** Shuts down the security manager. */
  void shutdown();
}

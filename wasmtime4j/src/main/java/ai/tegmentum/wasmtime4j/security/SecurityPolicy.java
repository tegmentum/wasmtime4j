package ai.tegmentum.wasmtime4j.security;

/**
 * Security policy interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface SecurityPolicy {

  /**
   * Gets the policy name.
   *
   * @return the policy name
   */
  String getPolicyName();

  /**
   * Checks if the policy is enabled.
   *
   * @return true if the policy is enabled
   */
  boolean isEnabled();

  /**
   * Gets the policy configuration.
   *
   * @return the policy configuration as a string
   */
  String getConfiguration();

  /**
   * Checks if the access request is allowed.
   *
   * @param request the access request to check
   * @return true if access is allowed
   */
  boolean checkAccess(AccessRequest request);
}

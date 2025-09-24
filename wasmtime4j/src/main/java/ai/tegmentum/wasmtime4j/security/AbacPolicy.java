package ai.tegmentum.wasmtime4j.security;

/**
 * Attribute-based access control policy.
 *
 * @since 1.0.0
 */
public interface AbacPolicy {

  /**
   * Gets the policy identifier.
   *
   * @return policy ID
   */
  String getPolicyId();

  /**
   * Gets the policy description.
   *
   * @return description
   */
  String getDescription();
}

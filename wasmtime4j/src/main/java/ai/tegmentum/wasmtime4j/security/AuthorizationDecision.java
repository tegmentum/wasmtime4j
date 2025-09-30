package ai.tegmentum.wasmtime4j.security;

/**
 * Authorization decision interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface AuthorizationDecision {

  /** Decision result enumeration. */
  enum Decision {
    /** Access granted. */
    PERMIT,
    /** Access denied. */
    DENY,
    /** Decision cannot be made. */
    INDETERMINATE,
    /** Not applicable. */
    NOT_APPLICABLE
  }

  /**
   * Gets the authorization decision.
   *
   * @return the decision
   */
  Decision getDecision();

  /**
   * Gets the decision message.
   *
   * @return the decision message
   */
  String getMessage();

  /**
   * Gets the decision timestamp.
   *
   * @return the timestamp in milliseconds
   */
  long getTimestamp();
}

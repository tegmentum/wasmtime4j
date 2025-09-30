package ai.tegmentum.wasmtime4j.security;

/**
 * Security access request interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface AccessRequest {

  /**
   * Gets the resource being accessed.
   *
   * @return the resource identifier
   */
  String getResource();

  /**
   * Gets the requested action.
   *
   * @return the action name
   */
  String getAction();

  /**
   * Gets the requesting subject.
   *
   * @return the subject identifier
   */
  String getSubject();

  /**
   * Gets the request context.
   *
   * @return the security context
   */
  SecurityContext getContext();
}

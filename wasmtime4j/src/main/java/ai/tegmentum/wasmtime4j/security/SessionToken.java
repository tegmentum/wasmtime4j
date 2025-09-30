package ai.tegmentum.wasmtime4j.security;

/**
 * Security session token interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface SessionToken {

  /**
   * Gets the token ID.
   *
   * @return the token ID
   */
  String getTokenId();

  /**
   * Gets the token expiration time.
   *
   * @return the expiration time in milliseconds
   */
  long getExpirationTime();

  /**
   * Checks if the token is valid.
   *
   * @return true if the token is valid
   */
  boolean isValid();

  /**
   * Gets the associated user ID.
   *
   * @return the user ID
   */
  String getUserId();

  /**
   * Refreshes the token.
   *
   * @return true if refresh was successful
   */
  boolean refresh();
}

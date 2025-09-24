package ai.tegmentum.wasmtime4j.security;

import java.util.Set;

/**
 * Session token for authentication.
 *
 * @since 1.0.0
 */
public interface SessionToken {

  /**
   * Gets the token identifier.
   *
   * @return token ID
   */
  String getTokenId();

  /**
   * Gets the user identifier.
   *
   * @return user ID
   */
  String getUserId();

  /**
   * Gets the token scopes.
   *
   * @return scopes
   */
  Set<String> getScopes();

  /**
   * Checks if the token has expired.
   *
   * @return true if expired
   */
  boolean isExpired();

  /**
   * Checks if the token has a specific scope.
   *
   * @param scope the scope to check
   * @return true if the token has the scope
   */
  boolean hasScope(final String scope);
}

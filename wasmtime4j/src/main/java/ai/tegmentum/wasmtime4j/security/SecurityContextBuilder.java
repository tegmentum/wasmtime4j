package ai.tegmentum.wasmtime4j.security;

import java.time.Duration;
import java.time.Instant;

/**
 * Builder for creating SecurityContext instances.
 *
 * @since 1.0.0
 */
public interface SecurityContextBuilder {

  /**
   * Grants a capability to the security context.
   *
   * @param capability the capability to grant
   * @return this builder for method chaining
   */
  SecurityContextBuilder grantCapability(final Capability capability);

  /**
   * Revokes a capability from the security context.
   *
   * @param capability the capability to revoke
   * @return this builder for method chaining
   */
  SecurityContextBuilder revokeCapability(final Capability capability);

  /**
   * Sets the expiration time for the security context.
   *
   * @param expiresAt the expiration timestamp
   * @return this builder for method chaining
   */
  SecurityContextBuilder expiresAt(final Instant expiresAt);

  /**
   * Sets the expiration duration from now.
   *
   * @param duration the duration until expiration
   * @return this builder for method chaining
   */
  SecurityContextBuilder expiresIn(final Duration duration);

  /**
   * Adds metadata to the security context.
   *
   * @param key the metadata key
   * @param value the metadata value
   * @return this builder for method chaining
   */
  SecurityContextBuilder metadata(final String key, final String value);

  /**
   * Sets the security level of the context.
   *
   * @param level the security level (0-10)
   * @return this builder for method chaining
   */
  SecurityContextBuilder securityLevel(final int level);

  /**
   * Builds the SecurityContext with the configured settings.
   *
   * @return a new SecurityContext instance
   * @throws SecurityException if the configuration is invalid
   */
  SecurityContext build() throws SecurityException;
}

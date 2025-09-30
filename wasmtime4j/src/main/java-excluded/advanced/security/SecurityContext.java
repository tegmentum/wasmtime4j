package ai.tegmentum.wasmtime4j.security;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Security execution context for sandboxed WebAssembly modules.
 *
 * <p>Defines the security boundaries, capabilities, and restrictions for a specific module
 * execution environment.
 *
 * @since 1.0.0
 */
public interface SecurityContext {

  /**
   * Gets the unique identifier for this security context.
   *
   * @return the context identifier
   */
  String getId();

  /**
   * Gets the security level of this context.
   *
   * <p>Higher security levels indicate more restrictive environments.
   *
   * @return the security level (0-10, where 10 is most restrictive)
   */
  int getSecurityLevel();

  /**
   * Gets the capabilities granted to this context.
   *
   * @return the set of granted capabilities
   */
  Set<Capability> getCapabilities();

  /**
   * Checks if a specific capability is granted.
   *
   * @param capability the capability to check
   * @return true if the capability is granted, false otherwise
   */
  boolean hasCapability(final Capability capability);

  /**
   * Gets the context metadata.
   *
   * @return a copy of the context metadata
   */
  Map<String, String> getMetadata();

  /**
   * Gets a specific metadata value.
   *
   * @param key the metadata key
   * @return the metadata value, or empty if not present
   */
  Optional<String> getMetadata(final String key);

  /**
   * Gets the context creation timestamp.
   *
   * @return the creation timestamp
   */
  Instant getCreatedAt();

  /**
   * Gets the context expiration timestamp.
   *
   * @return the expiration timestamp, or empty if the context doesn't expire
   */
  Optional<Instant> getExpiresAt();

  /**
   * Checks if this context has expired.
   *
   * @return true if the context has expired, false otherwise
   */
  boolean isExpired();

  /**
   * Gets the remaining time until expiration.
   *
   * @return the time until expiration, or empty if the context doesn't expire
   */
  Optional<java.time.Duration> getTimeToExpiration();

  /**
   * Creates a builder for constructing SecurityContext instances.
   *
   * @param id the context identifier
   * @param securityLevel the security level
   * @return a new SecurityContext builder
   */
  static SecurityContextBuilder builder(final String id, final int securityLevel) {
    // Use runtime selection pattern to find appropriate implementation
    try {
      // Try Panama implementation first
      final Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.security.PanamaSecurityContextBuilder");
      return (SecurityContextBuilder)
          builderClass
              .getDeclaredConstructor(String.class, int.class)
              .newInstance(id, securityLevel);
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> builderClass =
            Class.forName("ai.tegmentum.wasmtime4j.jni.security.JniSecurityContextBuilder");
        return (SecurityContextBuilder)
            builderClass
                .getDeclaredConstructor(String.class, int.class)
                .newInstance(id, securityLevel);
      } catch (final ClassNotFoundException e2) {
        throw new RuntimeException(
            "No SecurityContext implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create security context builder", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create security context builder", e);
    }
  }

  /**
   * Creates a restrictive security context with minimal capabilities.
   *
   * @param id the context identifier
   * @return a restrictive security context
   */
  static SecurityContext restrictive(final String id) {
    return builder(id, 8).build();
  }

  /**
   * Creates a permissive security context with broad capabilities.
   *
   * @param id the context identifier
   * @return a permissive security context
   */
  static SecurityContext permissive(final String id) {
    return builder(id, 2)
        .grantCapability(Capability.memoryAccess(1024 * 1024, true)) // 1MB memory
        .grantCapability(Capability.fileSystemAccess())
        .grantCapability(Capability.networkAccess())
        .build();
  }

  /**
   * Creates a standard security context with common capabilities.
   *
   * @param id the context identifier
   * @return a standard security context
   */
  static SecurityContext standard(final String id) {
    return builder(id, 5)
        .grantCapability(Capability.memoryAccess(512 * 1024, false)) // 512KB read-only memory
        .build();
  }
}

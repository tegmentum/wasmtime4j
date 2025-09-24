package ai.tegmentum.wasmtime4j.security;

import ai.tegmentum.wasmtime4j.exception.SecurityException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Enterprise security manager for WebAssembly module execution.
 *
 * <p>Provides comprehensive security features including module signing verification, access
 * control, audit logging, and compliance reporting. This is the main entry point for all
 * security-related functionality in wasmtime4j.
 *
 * <p>The security manager integrates multiple security subsystems:
 *
 * <ul>
 *   <li>Module signature verification
 *   <li>Role-based and attribute-based access control
 *   <li>Secure sandboxing with capability management
 *   <li>Comprehensive audit logging
 *   <li>Compliance reporting for various frameworks
 * </ul>
 *
 * @since 1.0.0
 */
public interface SecurityManager {

  /**
   * Creates a new security manager with default configuration.
   *
   * @return a new SecurityManager instance
   */
  static SecurityManager create() {
    return builder().build();
  }

  /**
   * Creates a new security manager builder.
   *
   * @return a new SecurityManagerBuilder instance
   */
  static SecurityManagerBuilder builder() {
    // Use runtime selection pattern to find appropriate implementation
    try {
      // Try Panama implementation first
      final Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.security.PanamaSecurityManagerBuilder");
      return (SecurityManagerBuilder) builderClass.getDeclaredConstructor().newInstance();
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> builderClass =
            Class.forName("ai.tegmentum.wasmtime4j.jni.security.JniSecurityManagerBuilder");
        return (SecurityManagerBuilder) builderClass.getDeclaredConstructor().newInstance();
      } catch (final ClassNotFoundException e2) {
        throw new RuntimeException(
            "No SecurityManager implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create security manager builder", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create security manager builder", e);
    }
  }

  /**
   * Verifies the cryptographic signature of a WebAssembly module.
   *
   * @param moduleBytes the module bytecode
   * @param signature the module signature
   * @return true if the signature is valid and trusted, false otherwise
   * @throws SecurityException if signature verification fails
   */
  boolean verifyModuleSignature(final byte[] moduleBytes, final ModuleSignature signature)
      throws SecurityException;

  /**
   * Signs a WebAssembly module with the configured signer.
   *
   * @param moduleBytes the module bytecode to sign
   * @return the generated signature
   * @throws SecurityException if signing fails
   */
  ModuleSignature signModule(final byte[] moduleBytes) throws SecurityException;

  /**
   * Creates a secure execution context for a module.
   *
   * @param contextId unique identifier for the context
   * @param securityLevel security level (higher = more restricted)
   * @return a new SecurityContext
   */
  SecurityContext createSecurityContext(final String contextId, final int securityLevel);

  /**
   * Authorizes an access request using configured authorization engines.
   *
   * @param request the access request to authorize
   * @return the authorization decision
   */
  AuthorizationDecision authorize(final AccessRequest request);

  /**
   * Creates a sandboxed execution environment for a module.
   *
   * @param moduleId unique identifier for the module
   * @param context security context for the execution
   * @return the sandbox identifier
   * @throws SecurityException if sandbox creation fails
   */
  String createSandbox(final String moduleId, final SecurityContext context)
      throws SecurityException;

  /**
   * Checks if a sandbox has a specific capability.
   *
   * @param sandboxId the sandbox identifier
   * @param capability the capability to check
   * @return true if the capability is granted, false otherwise
   * @throws SecurityException if the check fails
   */
  boolean hasCapability(final String sandboxId, final Capability capability)
      throws SecurityException;

  /**
   * Logs a security event for audit and compliance purposes.
   *
   * @param event the audit event to log
   */
  void logAuditEvent(final AuditEvent event);

  /**
   * Generates a compliance report for the specified framework and period.
   *
   * @param framework the compliance framework
   * @param startTime the report period start time
   * @param endTime the report period end time
   * @return the generated compliance report
   */
  ComplianceReport generateComplianceReport(
      final ComplianceFramework framework, final Instant startTime, final Instant endTime);

  /**
   * Adds a trusted certificate or public key to the trust store.
   *
   * @param fingerprint the key/certificate fingerprint
   * @param publicKey the public key bytes
   */
  void addTrustedKey(final String fingerprint, final byte[] publicKey);

  /**
   * Revokes a certificate or key from the trust store.
   *
   * @param fingerprint the fingerprint to revoke
   */
  void revokeKey(final String fingerprint);

  /**
   * Creates a new user session with authentication token.
   *
   * @param userId the user identifier
   * @param scopes the session scopes
   * @return the session token
   * @throws SecurityException if session creation fails
   */
  SessionToken createSession(final String userId, final Set<String> scopes)
      throws SecurityException;

  /**
   * Validates and retrieves a session token.
   *
   * @param tokenId the token identifier
   * @return the session token if valid, empty if invalid/expired
   */
  Optional<SessionToken> validateSession(final String tokenId);

  /**
   * Revokes a session token.
   *
   * @param tokenId the token identifier to revoke
   */
  void revokeSession(final String tokenId);

  /**
   * Adds a role to the RBAC system.
   *
   * @param role the role definition
   */
  void addRole(final Role role);

  /**
   * Assigns a role to a user.
   *
   * @param userId the user identifier
   * @param roleId the role identifier
   */
  void assignRole(final String userId, final String roleId);

  /**
   * Removes a role from a user.
   *
   * @param userId the user identifier
   * @param roleId the role identifier
   */
  void removeRole(final String userId, final String roleId);

  /**
   * Adds an ABAC policy to the authorization engine.
   *
   * @param policy the ABAC policy
   */
  void addAbacPolicy(final AbacPolicy policy);

  /**
   * Removes a sandbox and cleans up its resources.
   *
   * @param sandboxId the sandbox identifier
   */
  void removeSandbox(final String sandboxId);

  /**
   * Gets current security statistics.
   *
   * @return security statistics summary
   */
  SecurityStatistics getSecurityStatistics();

  /**
   * Performs security cleanup operations (expired sessions, contexts, etc.).
   *
   * @return the number of items cleaned up
   */
  int performSecurityCleanup();

  /**
   * Saves the trust store to a file.
   *
   * @param path the file path to save to
   * @throws SecurityException if saving fails
   */
  void saveTrustStore(final Path path) throws SecurityException;

  /**
   * Loads the trust store from a file.
   *
   * @param path the file path to load from
   * @throws SecurityException if loading fails
   */
  void loadTrustStore(final Path path) throws SecurityException;

  /**
   * Sets the security policy for the manager.
   *
   * @param policy the security policy
   */
  void setSecurityPolicy(final SecurityPolicy policy);

  /**
   * Gets the current security policy.
   *
   * @return the current security policy
   */
  SecurityPolicy getSecurityPolicy();

  /**
   * Enables or disables audit logging.
   *
   * @param enabled true to enable audit logging, false to disable
   */
  void setAuditLoggingEnabled(final boolean enabled);

  /**
   * Checks if audit logging is enabled.
   *
   * @return true if audit logging is enabled, false otherwise
   */
  boolean isAuditLoggingEnabled();

  /** Flushes any buffered audit events to persistent storage. */
  void flushAuditLog();

  /** Closes the security manager and releases all resources. */
  void close();
}

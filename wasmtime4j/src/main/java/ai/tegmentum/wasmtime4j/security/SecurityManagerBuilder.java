package ai.tegmentum.wasmtime4j.security;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

/**
 * Builder for creating SecurityManager instances with custom configuration.
 *
 * @since 1.0.0
 */
public interface SecurityManagerBuilder {

  /**
   * Sets whether to require module signatures for all modules.
   *
   * @param required true to require signatures
   * @return this builder for method chaining
   */
  SecurityManagerBuilder requireSignatures(final boolean required);

  /**
   * Sets whether to enforce certificate chain validation.
   *
   * @param enforce true to enforce certificate chains
   * @return this builder for method chaining
   */
  SecurityManagerBuilder enforceCertificateChains(final boolean enforce);

  /**
   * Sets the allowed signature algorithms.
   *
   * @param algorithms the allowed algorithms
   * @return this builder for method chaining
   */
  SecurityManagerBuilder allowedSignatureAlgorithms(final Set<SignatureAlgorithm> algorithms);

  /**
   * Sets the maximum age for signatures.
   *
   * @param maxAge the maximum signature age
   * @return this builder for method chaining
   */
  SecurityManagerBuilder maxSignatureAge(final Duration maxAge);

  /**
   * Sets whether to allow self-signed certificates.
   *
   * @param allow true to allow self-signed certificates
   * @return this builder for method chaining
   */
  SecurityManagerBuilder allowSelfSigned(final boolean allow);

  /**
   * Sets the trust store file path.
   *
   * @param path the trust store file path
   * @return this builder for method chaining
   */
  SecurityManagerBuilder trustStorePath(final Path path);

  /**
   * Sets the HMAC secret for session tokens.
   *
   * @param secret the HMAC secret bytes
   * @return this builder for method chaining
   */
  SecurityManagerBuilder sessionSecret(final byte[] secret);

  /**
   * Sets the default session expiration duration.
   *
   * @param duration the session expiration duration
   * @return this builder for method chaining
   */
  SecurityManagerBuilder sessionExpiration(final Duration duration);

  /**
   * Sets the authorization combining algorithm.
   *
   * @param algorithm the combining algorithm
   * @return this builder for method chaining
   */
  SecurityManagerBuilder authorizationAlgorithm(final CombiningAlgorithm algorithm);

  /**
   * Enables or disables strict sandbox mode.
   *
   * @param strict true for strict mode
   * @return this builder for method chaining
   */
  SecurityManagerBuilder strictSandboxMode(final boolean strict);

  /**
   * Sets the maximum number of concurrent sandboxes.
   *
   * @param maxConcurrent the maximum concurrent sandboxes
   * @return this builder for method chaining
   */
  SecurityManagerBuilder maxConcurrentSandboxes(final int maxConcurrent);

  /**
   * Enables or disables audit logging.
   *
   * @param enabled true to enable audit logging
   * @return this builder for method chaining
   */
  SecurityManagerBuilder auditLogging(final boolean enabled);

  /**
   * Sets the audit log file path.
   *
   * @param path the audit log file path
   * @return this builder for method chaining
   */
  SecurityManagerBuilder auditLogPath(final Path path);

  /**
   * Sets the audit log buffer size.
   *
   * @param bufferSize the buffer size for audit events
   * @return this builder for method chaining
   */
  SecurityManagerBuilder auditBufferSize(final int bufferSize);

  /**
   * Sets the audit log flush interval.
   *
   * @param interval the flush interval
   * @return this builder for method chaining
   */
  SecurityManagerBuilder auditFlushInterval(final Duration interval);

  /**
   * Enables or disables real-time security alerting.
   *
   * @param enabled true to enable alerting
   * @return this builder for method chaining
   */
  SecurityManagerBuilder securityAlerting(final boolean enabled);

  /**
   * Sets the alert severity threshold.
   *
   * @param threshold the minimum severity for alerts
   * @return this builder for method chaining
   */
  SecurityManagerBuilder alertThreshold(final AuditSeverity threshold);

  /**
   * Enables or disables audit log integrity protection.
   *
   * @param enabled true to enable integrity protection
   * @return this builder for method chaining
   */
  SecurityManagerBuilder auditIntegrity(final boolean enabled);

  /**
   * Sets the integrity secret for audit log protection.
   *
   * @param secret the integrity secret bytes
   * @return this builder for method chaining
   */
  SecurityManagerBuilder integritySecret(final byte[] secret);

  /**
   * Sets the default resource limits for sandboxes.
   *
   * @param limits the resource limits
   * @return this builder for method chaining
   */
  SecurityManagerBuilder defaultResourceLimits(final ResourceLimits limits);

  /**
   * Enables or disables inter-module communication.
   *
   * @param enabled true to enable inter-module communication
   * @return this builder for method chaining
   */
  SecurityManagerBuilder interModuleCommunication(final boolean enabled);

  /**
   * Sets the maximum message size for inter-module communication.
   *
   * @param maxSize the maximum message size in bytes
   * @return this builder for method chaining
   */
  SecurityManagerBuilder maxMessageSize(final int maxSize);

  /**
   * Builds the SecurityManager with the configured settings.
   *
   * @return a new SecurityManager instance
   * @throws SecurityException if the configuration is invalid
   */
  SecurityManager build() throws SecurityException;
}

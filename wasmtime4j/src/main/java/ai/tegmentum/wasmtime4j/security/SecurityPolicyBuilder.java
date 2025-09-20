package ai.tegmentum.wasmtime4j.security;

import java.time.Duration;
import java.util.Set;

/**
 * Builder for creating comprehensive security policies.
 *
 * <p>Provides fluent interface for configuring enterprise-grade security policies with
 * capability-based access control, resource limits, and monitoring settings.
 *
 * @since 1.0.0
 */
public interface SecurityPolicyBuilder {

  /**
   * Enables permissive mode (allows most operations with minimal restrictions).
   *
   * <p><strong>WARNING:</strong> Should only be used in development environments.
   *
   * @param permissive true to enable permissive mode
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withPermissiveMode(final boolean permissive);

  /**
   * Enables restrictive mode (minimal permissions with strict enforcement).
   *
   * @param restrictive true to enable restrictive mode
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withRestrictiveMode(final boolean restrictive);

  /**
   * Enables sandbox mode (maximum isolation for untrusted code).
   *
   * @param sandbox true to enable sandbox mode
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withSandboxMode(final boolean sandbox);

  /**
   * Grants specific capabilities to the security policy.
   *
   * @param capabilities the capabilities to grant
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withCapabilities(final Set<Capability> capabilities);

  /**
   * Grants a single capability to the security policy.
   *
   * @param capability the capability to grant
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withCapability(final Capability capability);

  /**
   * Revokes specific capabilities from the security policy.
   *
   * @param capabilities the capabilities to revoke
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withoutCapabilities(final Set<Capability> capabilities);

  /**
   * Revokes a single capability from the security policy.
   *
   * @param capability the capability to revoke
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withoutCapability(final Capability capability);

  /**
   * Sets resource limits for WebAssembly execution.
   *
   * @param limits the resource limits to enforce
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withResourceLimits(final ResourceLimits limits);

  /**
   * Sets maximum memory usage limit.
   *
   * @param maxMemoryBytes maximum memory in bytes
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withMaxMemory(final long maxMemoryBytes);

  /**
   * Sets maximum execution time limit.
   *
   * @param maxExecutionTime maximum execution duration
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withMaxExecutionTime(final Duration maxExecutionTime);

  /**
   * Sets maximum number of instructions that can be executed.
   *
   * @param maxInstructions maximum instruction count
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withMaxInstructions(final long maxInstructions);

  /**
   * Enables or disables host function access.
   *
   * @param allowed true to allow host function access
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withHostFunctionAccess(final boolean allowed);

  /**
   * Enables or disables network access.
   *
   * @param allowed true to allow network access
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withNetworkAccess(final boolean allowed);

  /**
   * Enables or disables file system access.
   *
   * @param allowed true to allow file system access
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withFileSystemAccess(final boolean allowed);

  /**
   * Sets the security context for this policy.
   *
   * @param context the security context
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withSecurityContext(final SecurityContext context);

  /**
   * Enables or disables security audit logging.
   *
   * @param enabled true to enable audit logging
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withAuditingEnabled(final boolean enabled);

  /**
   * Sets specific audit event types to log.
   *
   * @param eventTypes the event types to audit
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withAuditEventTypes(final Set<SecurityEventType> eventTypes);

  /**
   * Enables or disables intrusion detection.
   *
   * @param enabled true to enable intrusion detection
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withIntrusionDetection(final boolean enabled);

  /**
   * Sets intrusion detection configuration.
   *
   * @param config the intrusion detection configuration
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withIntrusionDetectionConfig(final IntrusionDetectionConfig config);

  /**
   * Sets the policy enforcement level.
   *
   * @param level the enforcement level
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withEnforcementLevel(final EnforcementLevel level);

  /**
   * Sets module hash verification requirements.
   *
   * @param required true to require module hash verification
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withModuleHashVerification(final boolean required);

  /**
   * Sets trusted module sources.
   *
   * @param sources set of trusted module source identifiers
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withTrustedModuleSources(final Set<String> sources);

  /**
   * Sets rate limiting configuration for operations.
   *
   * @param rateLimits the rate limiting configuration
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withRateLimits(final RateLimits rateLimits);

  /**
   * Adds custom security validators.
   *
   * @param validators the custom security validators
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withCustomValidators(final Set<SecurityValidator> validators);

  /**
   * Sets policy metadata.
   *
   * @param metadata the policy metadata
   * @return this builder for method chaining
   */
  SecurityPolicyBuilder withMetadata(final PolicyMetadata metadata);

  /**
   * Creates the configured security policy.
   *
   * <p>Validates the configuration for consistency and completeness before creating the final
   * policy instance.
   *
   * @return a configured SecurityPolicy instance
   * @throws SecurityPolicyException if the configuration is invalid
   */
  SecurityPolicy build() throws SecurityPolicyException;
}

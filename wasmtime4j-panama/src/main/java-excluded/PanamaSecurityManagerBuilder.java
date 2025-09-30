package ai.tegmentum.wasmtime4j.panama.security;

import ai.tegmentum.wasmtime4j.security.SecurityManager;
import ai.tegmentum.wasmtime4j.security.SecurityManagerBuilder;
import ai.tegmentum.wasmtime4j.security.SecurityPolicy;

/**
 * Panama-specific security manager builder implementation.
 *
 * <p>Provides Panama Foreign Function API integration for all security features including:
 *
 * <ul>
 *   <li>Capability-based access control with native enforcement
 *   <li>Advanced sandboxing with Panama process isolation
 *   <li>Cryptographic operations using Foreign Function API
 *   <li>Real-time security monitoring and auditing
 *   <li>Secure communication with TLS/mTLS
 *   <li>Vulnerability management and scanning
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaSecurityManagerBuilder implements SecurityManagerBuilder {

  private boolean auditLoggingEnabled = true;
  private boolean requireSignatures = false;
  private boolean strictSandboxMode = false;
  private SecurityPolicy securityPolicy;

  @Override
  public SecurityManagerBuilder requireSignatures(final boolean required) {
    this.requireSignatures = required;
    return this;
  }

  @Override
  public SecurityManagerBuilder auditLogging(final boolean enabled) {
    this.auditLoggingEnabled = enabled;
    return this;
  }

  @Override
  public SecurityManagerBuilder strictSandboxMode(final boolean strict) {
    this.strictSandboxMode = strict;
    return this;
  }

  @Override
  public SecurityManagerBuilder securityPolicy(final SecurityPolicy policy) {
    this.securityPolicy = policy;
    return this;
  }

  @Override
  public SecurityManager build() {
    return new PanamaSecurityManagerImpl(
        requireSignatures, auditLoggingEnabled, strictSandboxMode, securityPolicy);
  }
}

package ai.tegmentum.wasmtime4j.jni.security;

import ai.tegmentum.wasmtime4j.security.SecurityManager;
import ai.tegmentum.wasmtime4j.security.SecurityManagerBuilder;
import ai.tegmentum.wasmtime4j.security.SecurityPolicy;

/**
 * JNI-specific security manager builder implementation.
 *
 * <p>Provides JNI runtime integration for all security features including:
 *
 * <ul>
 *   <li>Capability-based access control with native enforcement
 *   <li>Advanced sandboxing with JNI process isolation
 *   <li>Cryptographic operations using native libraries
 *   <li>Real-time security monitoring and auditing
 *   <li>Secure communication with TLS/mTLS
 *   <li>Vulnerability management and scanning
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniSecurityManagerBuilder implements SecurityManagerBuilder {

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
    return new JniSecurityManagerImpl(
        requireSignatures, auditLoggingEnabled, strictSandboxMode, securityPolicy);
  }
}

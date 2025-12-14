package ai.tegmentum.wasmtime4j.exception;

import java.util.Locale;

/**
 * Exception thrown when security violations occur in WebAssembly operations.
 *
 * <p>This exception is thrown when WebAssembly security policies are violated, including:
 *
 * <ul>
 *   <li>Unauthorized access to host functions or resources
 *   <li>Sandbox escape attempts or policy violations
 *   <li>Permission denied errors for system operations
 *   <li>Host function security validation failures
 *   <li>Module import/export security checks
 *   <li>WASI capability and permission violations
 * </ul>
 *
 * <p>SecurityException provides detailed context about security violations to help developers
 * understand and address security policy compliance issues while maintaining system security.
 *
 * @since 1.0.0
 */
public class SecurityException extends WasmException {

  private static final long serialVersionUID = 1L;

  /** The security policy that was violated. */
  private final String violatedPolicy;

  /** The attempted action that triggered the security violation. */
  private final String attemptedAction;

  /** The resource or permission that was denied. */
  private final String deniedResource;

  /** The security context in which the violation occurred. */
  private final SecurityContext securityContext;

  /** Security contexts for better error categorization and handling. */
  public enum SecurityContext {
    /** Host function security context. */
    HOST_FUNCTION,
    /** WASI capability and permission context. */
    WASI_CAPABILITY,
    /** Module import/export security context. */
    MODULE_IMPORT_EXPORT,
    /** Memory access security context. */
    MEMORY_ACCESS,
    /** File system access security context. */
    FILE_SYSTEM_ACCESS,
    /** Network access security context. */
    NETWORK_ACCESS,
    /** System resource access security context. */
    SYSTEM_RESOURCE,
    /** General sandbox security context. */
    SANDBOX_POLICY
  }

  /**
   * Creates a new security exception with the specified message.
   *
   * @param message the error message describing the security violation
   */
  public SecurityException(final String message) {
    super(message);
    this.violatedPolicy = null;
    this.attemptedAction = null;
    this.deniedResource = null;
    this.securityContext = SecurityContext.SANDBOX_POLICY;
  }

  /**
   * Creates a new security exception with the specified message and cause.
   *
   * @param message the error message describing the security violation
   * @param cause the underlying cause
   */
  public SecurityException(final String message, final Throwable cause) {
    super(message, cause);
    this.violatedPolicy = null;
    this.attemptedAction = null;
    this.deniedResource = null;
    this.securityContext = SecurityContext.SANDBOX_POLICY;
  }

  /**
   * Creates a new security exception with security-specific details.
   *
   * @param message the error message describing the security violation
   * @param violatedPolicy the security policy that was violated
   * @param attemptedAction the action that was attempted
   * @param securityContext the security context
   */
  public SecurityException(
      final String message,
      final String violatedPolicy,
      final String attemptedAction,
      final SecurityContext securityContext) {
    super(message);
    this.violatedPolicy = violatedPolicy;
    this.attemptedAction = attemptedAction;
    this.deniedResource = null;
    this.securityContext = securityContext;
  }

  /**
   * Creates a new security exception with full security details.
   *
   * @param message the error message describing the security violation
   * @param violatedPolicy the security policy that was violated
   * @param attemptedAction the action that was attempted
   * @param deniedResource the resource that was denied access
   * @param securityContext the security context
   */
  public SecurityException(
      final String message,
      final String violatedPolicy,
      final String attemptedAction,
      final String deniedResource,
      final SecurityContext securityContext) {
    super(message);
    this.violatedPolicy = violatedPolicy;
    this.attemptedAction = attemptedAction;
    this.deniedResource = deniedResource;
    this.securityContext = securityContext;
  }

  /**
   * Creates a new security exception with full details and cause.
   *
   * @param message the error message describing the security violation
   * @param violatedPolicy the security policy that was violated
   * @param attemptedAction the action that was attempted
   * @param deniedResource the resource that was denied access
   * @param securityContext the security context
   * @param cause the underlying cause
   */
  public SecurityException(
      final String message,
      final String violatedPolicy,
      final String attemptedAction,
      final String deniedResource,
      final SecurityContext securityContext,
      final Throwable cause) {
    super(message, cause);
    this.violatedPolicy = violatedPolicy;
    this.attemptedAction = attemptedAction;
    this.deniedResource = deniedResource;
    this.securityContext = securityContext;
  }

  /**
   * Gets the security policy that was violated.
   *
   * @return the violated policy, or null if not specified
   */
  public String getViolatedPolicy() {
    return violatedPolicy;
  }

  /**
   * Gets the attempted action that triggered the security violation.
   *
   * @return the attempted action, or null if not specified
   */
  public String getAttemptedAction() {
    return attemptedAction;
  }

  /**
   * Gets the resource that was denied access.
   *
   * @return the denied resource, or null if not specified
   */
  public String getDeniedResource() {
    return deniedResource;
  }

  /**
   * Gets the security context in which the violation occurred.
   *
   * @return the security context
   */
  public SecurityContext getSecurityContext() {
    return securityContext;
  }

  /**
   * Checks if this is a host function security violation.
   *
   * @return true if this is a host function security error, false otherwise
   */
  public boolean isHostFunctionViolation() {
    return securityContext == SecurityContext.HOST_FUNCTION;
  }

  /**
   * Checks if this is a WASI capability violation.
   *
   * @return true if this is a WASI capability error, false otherwise
   */
  public boolean isWasiCapabilityViolation() {
    return securityContext == SecurityContext.WASI_CAPABILITY;
  }

  /**
   * Checks if this is a file system access violation.
   *
   * @return true if this is a file system access error, false otherwise
   */
  public boolean isFileSystemViolation() {
    return securityContext == SecurityContext.FILE_SYSTEM_ACCESS;
  }

  /**
   * Checks if this is a network access violation.
   *
   * @return true if this is a network access error, false otherwise
   */
  public boolean isNetworkViolation() {
    return securityContext == SecurityContext.NETWORK_ACCESS;
  }

  /**
   * Checks if this is a memory access violation.
   *
   * @return true if this is a memory access error, false otherwise
   */
  public boolean isMemoryViolation() {
    return securityContext == SecurityContext.MEMORY_ACCESS;
  }

  /**
   * Checks if this is a sandbox policy violation.
   *
   * @return true if this is a sandbox policy error, false otherwise
   */
  public boolean isSandboxViolation() {
    return securityContext == SecurityContext.SANDBOX_POLICY;
  }

  /**
   * Provides a formatted description of the security violation for logging and debugging.
   *
   * @return formatted security violation description
   */
  public String getSecurityViolationDescription() {
    final StringBuilder desc = new StringBuilder("Security violation");

    desc.append(" [context: ")
        .append(securityContext.name().toLowerCase(Locale.ROOT).replace('_', ' '))
        .append("]");

    if (violatedPolicy != null) {
      desc.append(" [policy: ").append(violatedPolicy).append("]");
    }

    if (attemptedAction != null) {
      desc.append(" [action: ").append(attemptedAction).append("]");
    }

    if (deniedResource != null) {
      desc.append(" [resource: ").append(deniedResource).append("]");
    }

    return desc.toString();
  }
}

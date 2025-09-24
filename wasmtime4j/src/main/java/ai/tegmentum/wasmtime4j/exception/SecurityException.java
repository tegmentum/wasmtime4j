package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when security operations fail or security violations are detected.
 *
 * <p>This exception is thrown in various security-related scenarios:
 *
 * <ul>
 *   <li>Module signature verification failures
 *   <li>Authorization denials
 *   <li>Sandbox security violations
 *   <li>Capability access violations
 *   <li>Trust store operations failures
 *   <li>Cryptographic operation failures
 * </ul>
 *
 * @since 1.0.0
 */
public class SecurityException extends WasmException {

  private static final long serialVersionUID = 1L;

  /** Security violation types for classification. */
  public enum ViolationType {
    /** Signature verification failed */
    SIGNATURE_VERIFICATION_FAILED,
    /** Authorization denied */
    AUTHORIZATION_DENIED,
    /** Capability access violation */
    CAPABILITY_VIOLATION,
    /** Trust store operation failed */
    TRUST_STORE_ERROR,
    /** Cryptographic operation failed */
    CRYPTOGRAPHIC_ERROR,
    /** Sandbox security violation */
    SANDBOX_VIOLATION,
    /** Session or token error */
    SESSION_ERROR,
    /** Configuration error */
    CONFIGURATION_ERROR,
    /** Resource access violation */
    RESOURCE_ACCESS_VIOLATION,
    /** Audit or compliance violation */
    AUDIT_VIOLATION
  }

  private final ViolationType violationType;

  /**
   * Creates a new SecurityException with a message.
   *
   * @param message the exception message
   */
  public SecurityException(final String message) {
    super(message);
    this.violationType = ViolationType.CONFIGURATION_ERROR;
  }

  /**
   * Creates a new SecurityException with a message and cause.
   *
   * @param message the exception message
   * @param cause the underlying cause
   */
  public SecurityException(final String message, final Throwable cause) {
    super(message, cause);
    this.violationType = ViolationType.CONFIGURATION_ERROR;
  }

  /**
   * Creates a new SecurityException with a message and violation type.
   *
   * @param message the exception message
   * @param violationType the type of security violation
   */
  public SecurityException(final String message, final ViolationType violationType) {
    super(message);
    this.violationType = violationType;
  }

  /**
   * Creates a new SecurityException with a message, cause, and violation type.
   *
   * @param message the exception message
   * @param cause the underlying cause
   * @param violationType the type of security violation
   */
  public SecurityException(
      final String message, final Throwable cause, final ViolationType violationType) {
    super(message, cause);
    this.violationType = violationType;
  }

  /**
   * Gets the type of security violation.
   *
   * @return the violation type
   */
  public ViolationType getViolationType() {
    return violationType;
  }

  /**
   * Checks if this is a critical security violation.
   *
   * <p>Critical violations include signature verification failures, sandbox violations, and
   * cryptographic errors.
   *
   * @return true if this is a critical violation, false otherwise
   */
  public boolean isCritical() {
    switch (violationType) {
      case SIGNATURE_VERIFICATION_FAILED:
      case SANDBOX_VIOLATION:
      case CRYPTOGRAPHIC_ERROR:
      case RESOURCE_ACCESS_VIOLATION:
        return true;
      default:
        return false;
    }
  }

  /**
   * Creates a signature verification failure exception.
   *
   * @param message the failure message
   * @return a new SecurityException
   */
  public static SecurityException signatureVerificationFailed(final String message) {
    return new SecurityException(message, ViolationType.SIGNATURE_VERIFICATION_FAILED);
  }

  /**
   * Creates an authorization denied exception.
   *
   * @param message the denial message
   * @return a new SecurityException
   */
  public static SecurityException authorizationDenied(final String message) {
    return new SecurityException(message, ViolationType.AUTHORIZATION_DENIED);
  }

  /**
   * Creates a capability violation exception.
   *
   * @param message the violation message
   * @return a new SecurityException
   */
  public static SecurityException capabilityViolation(final String message) {
    return new SecurityException(message, ViolationType.CAPABILITY_VIOLATION);
  }

  /**
   * Creates a sandbox violation exception.
   *
   * @param message the violation message
   * @return a new SecurityException
   */
  public static SecurityException sandboxViolation(final String message) {
    return new SecurityException(message, ViolationType.SANDBOX_VIOLATION);
  }

  /**
   * Creates a cryptographic error exception.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @return a new SecurityException
   */
  public static SecurityException cryptographicError(final String message, final Throwable cause) {
    return new SecurityException(message, cause, ViolationType.CRYPTOGRAPHIC_ERROR);
  }

  /**
   * Creates a trust store error exception.
   *
   * @param message the error message
   * @return a new SecurityException
   */
  public static SecurityException trustStoreError(final String message) {
    return new SecurityException(message, ViolationType.TRUST_STORE_ERROR);
  }

  /**
   * Creates a session error exception.
   *
   * @param message the error message
   * @return a new SecurityException
   */
  public static SecurityException sessionError(final String message) {
    return new SecurityException(message, ViolationType.SESSION_ERROR);
  }

  @Override
  public String toString() {
    return String.format("SecurityException[%s]: %s", violationType, getMessage());
  }
}

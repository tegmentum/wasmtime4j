package ai.tegmentum.wasmtime4j.security;

/**
 * Exception thrown when sandbox operations fail.
 *
 * @since 1.0.0
 */
public class SandboxException extends SecurityException {

  /**
   * Creates a new sandbox exception.
   *
   * @param message the exception message
   */
  public SandboxException(final String message) {
    super(message, SecurityEventType.SANDBOX_BREACH_ATTEMPTED, SecuritySeverity.HIGH);
  }

  /**
   * Creates a new sandbox exception with cause.
   *
   * @param message the exception message
   * @param cause the underlying cause
   */
  public SandboxException(final String message, final Throwable cause) {
    super(message, cause, SecurityEventType.SANDBOX_BREACH_ATTEMPTED, SecuritySeverity.HIGH);
  }

  /**
   * Creates a new sandbox exception with specific event type and severity.
   *
   * @param message the exception message
   * @param eventType the associated security event type
   * @param severity the security severity level
   */
  public SandboxException(
      final String message, final SecurityEventType eventType, final SecuritySeverity severity) {
    super(message, eventType, severity);
  }

  /**
   * Creates a new sandbox exception with cause, event type and severity.
   *
   * @param message the exception message
   * @param cause the underlying cause
   * @param eventType the associated security event type
   * @param severity the security severity level
   */
  public SandboxException(
      final String message,
      final Throwable cause,
      final SecurityEventType eventType,
      final SecuritySeverity severity) {
    super(message, cause, eventType, severity);
  }
}

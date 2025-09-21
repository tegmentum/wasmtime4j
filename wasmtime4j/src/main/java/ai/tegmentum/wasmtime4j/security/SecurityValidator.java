package ai.tegmentum.wasmtime4j.security;

/**
 * Interface for custom security validators.
 *
 * @since 1.0.0
 */
public interface SecurityValidator {

  /**
   * Validates the given security event.
   *
   * @param event the security event to validate
   * @return validation result
   */
  ValidationResult validate(final SecurityEvent event);

  /**
   * Gets the name of this validator.
   *
   * @return validator name
   */
  String getName();

  /** Result of security validation. */
  class ValidationResult {
    private final boolean valid;
    private final String message;
    private final SecuritySeverity severity;

    public ValidationResult(
        final boolean valid, final String message, final SecuritySeverity severity) {
      this.valid = valid;
      this.message = message;
      this.severity = severity;
    }

    public static ValidationResult valid() {
      return new ValidationResult(true, "Valid", SecuritySeverity.INFO);
    }

    public static ValidationResult invalid(final String message) {
      return new ValidationResult(false, message, SecuritySeverity.HIGH);
    }

    public static ValidationResult invalid(final String message, final SecuritySeverity severity) {
      return new ValidationResult(false, message, severity);
    }

    public boolean isValid() {
      return valid;
    }

    public String getMessage() {
      return message;
    }

    public SecuritySeverity getSeverity() {
      return severity;
    }
  }
}

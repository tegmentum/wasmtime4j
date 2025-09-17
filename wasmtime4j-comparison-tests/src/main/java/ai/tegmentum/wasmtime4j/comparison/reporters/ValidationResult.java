package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Collections;
import java.util.List;

/** Result of schema validation. */
public final class ValidationResult {
  private final boolean valid;
  private final List<ValidationError> errors;
  private final List<ValidationWarning> warnings;

  private ValidationResult(
      final boolean valid,
      final List<ValidationError> errors,
      final List<ValidationWarning> warnings) {
    this.valid = valid;
    this.errors = List.copyOf(errors);
    this.warnings = List.copyOf(warnings);
  }

  /**
   * Creates a successful validation result.
   *
   * @param warnings the validation warnings
   * @return the validation result
   */
  public static ValidationResult success(final List<ValidationWarning> warnings) {
    return new ValidationResult(true, Collections.emptyList(), warnings);
  }

  /**
   * Creates a failed validation result.
   *
   * @param errors the validation errors
   * @param warnings the validation warnings
   * @return the validation result
   */
  public static ValidationResult failure(
      final List<ValidationError> errors, final List<ValidationWarning> warnings) {
    return new ValidationResult(false, errors, warnings);
  }

  /**
   * Creates an error validation result.
   *
   * @param message the error message
   * @param type the error type
   * @return the validation result
   */
  public static ValidationResult error(final String message, final ValidationErrorType type) {
    return new ValidationResult(
        false, List.of(new ValidationError(message, type)), Collections.emptyList());
  }

  /**
   * Checks if the validation was successful.
   *
   * @return true if valid
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * Gets the validation errors.
   *
   * @return the validation errors
   */
  public List<ValidationError> getErrors() {
    return errors;
  }

  /**
   * Gets the validation warnings.
   *
   * @return the validation warnings
   */
  public List<ValidationWarning> getWarnings() {
    return warnings;
  }

  /**
   * Checks if there are warnings.
   *
   * @return true if there are warnings
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }
}

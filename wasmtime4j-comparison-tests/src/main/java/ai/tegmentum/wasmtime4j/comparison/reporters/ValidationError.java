package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/** Validation error information. */
public final class ValidationError {
  private final String message;
  private final ValidationErrorType type;

  /**
   * Creates a new validation error.
   *
   * @param message the error message
   * @param type the error type
   */
  public ValidationError(final String message, final ValidationErrorType type) {
    this.message = Objects.requireNonNull(message, "message cannot be null");
    this.type = Objects.requireNonNull(type, "type cannot be null");
  }

  /**
   * Gets the error message.
   *
   * @return the error message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets the error type.
   *
   * @return the error type
   */
  public ValidationErrorType getType() {
    return type;
  }

  @Override
  public String toString() {
    return type + ": " + message;
  }
}

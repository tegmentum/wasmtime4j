package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/**
 * Validation warning information.
 */
public final class ValidationWarning {
  private final String message;
  private final String category;

  /**
   * Creates a new validation warning.
   *
   * @param message the warning message
   * @param category the warning category
   */
  public ValidationWarning(final String message, final String category) {
    this.message = Objects.requireNonNull(message, "message cannot be null");
    this.category = Objects.requireNonNull(category, "category cannot be null");
  }

  /**
   * Gets the warning message.
   *
   * @return the warning message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets the warning category.
   *
   * @return the warning category
   */
  public String getCategory() {
    return category;
  }

  @Override
  public String toString() {
    return category + ": " + message;
  }
}
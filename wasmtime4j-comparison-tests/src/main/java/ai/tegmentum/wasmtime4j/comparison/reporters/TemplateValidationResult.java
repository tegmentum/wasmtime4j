package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.List;

/**
 * Template validation result.
 *
 * @since 1.0.0
 */
public final class TemplateValidationResult {
  private final boolean valid;
  private final List<String> errors;
  private final List<String> warnings;

  /**
   * Constructs a template validation result.
   *
   * @param valid true if the template is valid
   * @param errors list of validation errors
   * @param warnings list of validation warnings
   */
  public TemplateValidationResult(
      final boolean valid, final List<String> errors, final List<String> warnings) {
    this.valid = valid;
    this.errors = List.copyOf(errors);
    this.warnings = List.copyOf(warnings);
  }

  public boolean isValid() {
    return valid;
  }

  public List<String> getErrors() {
    return errors;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public boolean hasIssues() {
    return !errors.isEmpty() || !warnings.isEmpty();
  }

  @Override
  public String toString() {
    return "TemplateValidationResult{"
        + "valid="
        + valid
        + ", errors="
        + errors.size()
        + ", warnings="
        + warnings.size()
        + '}';
  }
}

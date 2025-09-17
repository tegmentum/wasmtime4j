package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.List;
import java.util.Objects;

/**
 * Result of template validation.
 *
 * @since 1.0.0
 */
public final class TemplateValidationResult {
  private final boolean valid;
  private final List<String> errors;
  private final List<String> warnings;

  public TemplateValidationResult(
      final boolean valid, final List<String> errors, final List<String> warnings) {
    this.valid = valid;
    this.errors = List.copyOf(Objects.requireNonNull(errors, "errors cannot be null"));
    this.warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings cannot be null"));
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

  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final TemplateValidationResult that = (TemplateValidationResult) obj;
    return valid == that.valid
        && Objects.equals(errors, that.errors)
        && Objects.equals(warnings, that.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valid, errors, warnings);
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
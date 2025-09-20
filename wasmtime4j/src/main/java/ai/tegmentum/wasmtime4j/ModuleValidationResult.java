package ai.tegmentum.wasmtime4j;

import java.util.Collections;
import java.util.List;

/**
 * Result of WebAssembly module validation.
 *
 * <p>This class encapsulates the result of validating WebAssembly bytecode, including whether the
 * validation succeeded and any validation errors or warnings.
 *
 * @since 1.0.0
 */
public final class ModuleValidationResult {

  private final boolean isValid;
  private final List<String> errors;
  private final List<String> warnings;

  /**
   * Creates a successful validation result.
   *
   * @return a successful validation result
   */
  public static ModuleValidationResult success() {
    return new ModuleValidationResult(true, Collections.emptyList(), Collections.emptyList());
  }

  /**
   * Creates a failed validation result with errors.
   *
   * @param errors the validation errors
   * @return a failed validation result
   * @throws IllegalArgumentException if errors is null
   */
  public static ModuleValidationResult failure(final List<String> errors) {
    return new ModuleValidationResult(false, errors, Collections.emptyList());
  }

  /**
   * Creates a successful validation result with warnings.
   *
   * @param warnings the validation warnings
   * @return a successful validation result with warnings
   * @throws IllegalArgumentException if warnings is null
   */
  public static ModuleValidationResult successWithWarnings(final List<String> warnings) {
    return new ModuleValidationResult(true, Collections.emptyList(), warnings);
  }

  /**
   * Creates a validation result.
   *
   * @param isValid whether the validation succeeded
   * @param errors the validation errors (empty if none)
   * @param warnings the validation warnings (empty if none)
   * @throws IllegalArgumentException if errors or warnings is null
   */
  public ModuleValidationResult(
      final boolean isValid, final List<String> errors, final List<String> warnings) {
    if (errors == null) {
      throw new IllegalArgumentException("Errors list cannot be null");
    }
    if (warnings == null) {
      throw new IllegalArgumentException("Warnings list cannot be null");
    }
    this.isValid = isValid;
    this.errors = Collections.unmodifiableList(errors);
    this.warnings = Collections.unmodifiableList(warnings);
  }

  /**
   * Checks if the validation was successful.
   *
   * @return true if the module is valid, false otherwise
   */
  public boolean isValid() {
    return isValid;
  }

  /**
   * Gets the validation errors.
   *
   * @return an immutable list of validation errors
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Gets the validation warnings.
   *
   * @return an immutable list of validation warnings
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * Checks if there are any validation errors.
   *
   * @return true if there are errors, false otherwise
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Checks if there are any validation warnings.
   *
   * @return true if there are warnings, false otherwise
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ModuleValidationResult{valid=").append(isValid);
    if (hasErrors()) {
      sb.append(", errors=").append(errors.size());
    }
    if (hasWarnings()) {
      sb.append(", warnings=").append(warnings.size());
    }
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ModuleValidationResult that = (ModuleValidationResult) obj;
    return isValid == that.isValid && errors.equals(that.errors) && warnings.equals(that.warnings);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(isValid, errors, warnings);
  }
}

package ai.tegmentum.wasmtime4j.validation;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of import validation for a set of WebAssembly modules.
 *
 * <p>This class provides detailed information about import compatibility, missing imports, type
 * mismatches, and other validation issues.
 *
 * @since 1.0.0
 */
public final class ImportValidation {

  private final boolean valid;
  private final List<ImportIssue> issues;
  private final List<ImportInfo> validatedImports;
  private final int totalImports;
  private final int validImports;
  private final Duration validationTime;

  /**
   * Creates a new import validation result.
   *
   * @param valid whether all imports are valid
   * @param issues list of validation issues found
   * @param validatedImports list of all imports that were validated
   * @param totalImports total number of imports checked
   * @param validImports number of imports that passed validation
   * @param validationTime time taken for validation
   */
  public ImportValidation(
      final boolean valid,
      final List<ImportIssue> issues,
      final List<ImportInfo> validatedImports,
      final int totalImports,
      final int validImports,
      final Duration validationTime) {
    this.valid = valid;
    this.issues = Collections.unmodifiableList(Objects.requireNonNull(issues, "issues"));
    this.validatedImports =
        Collections.unmodifiableList(Objects.requireNonNull(validatedImports, "validatedImports"));
    this.totalImports = totalImports;
    this.validImports = validImports;
    this.validationTime = Objects.requireNonNull(validationTime, "validationTime");
  }

  /**
   * Checks whether all imports are valid.
   *
   * @return true if all imports passed validation, false otherwise
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * Gets all validation issues found.
   *
   * @return an unmodifiable list of import issues
   */
  public List<ImportIssue> getIssues() {
    return issues;
  }

  /**
   * Gets all imports that were validated.
   *
   * @return an unmodifiable list of validated imports
   */
  public List<ImportInfo> getValidatedImports() {
    return validatedImports;
  }

  /**
   * Gets the total number of imports checked.
   *
   * @return the total import count
   */
  public int getTotalImports() {
    return totalImports;
  }

  /**
   * Gets the number of imports that passed validation.
   *
   * @return the valid import count
   */
  public int getValidImports() {
    return validImports;
  }

  /**
   * Gets the time taken for validation.
   *
   * @return the validation duration
   */
  public Duration getValidationTime() {
    return validationTime;
  }

  @Override
  public String toString() {
    return String.format(
        "ImportValidation{valid=%s, imports=%d, validImports=%d (%.1f%%), "
            + "issues=%d, validationTime=%s}",
        valid,
        totalImports,
        validImports,
        totalImports == 0 ? 100.0 : (double) validImports / totalImports * 100.0,
        issues.size(),
        validationTime);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ImportValidation that = (ImportValidation) obj;
    return valid == that.valid
        && totalImports == that.totalImports
        && validImports == that.validImports
        && Objects.equals(issues, that.issues)
        && Objects.equals(validatedImports, that.validatedImports)
        && Objects.equals(validationTime, that.validationTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        valid, issues, validatedImports, totalImports, validImports, validationTime);
  }
}

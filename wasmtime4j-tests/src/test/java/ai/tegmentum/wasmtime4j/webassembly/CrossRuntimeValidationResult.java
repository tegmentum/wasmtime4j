package ai.tegmentum.wasmtime4j.webassembly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Result of validating consistency between runtime implementations. */
public final class CrossRuntimeValidationResult {
  private final String testName;
  private final boolean consistent;
  private final List<String> errors;
  private final List<String> warnings;
  private final List<String> info;

  private CrossRuntimeValidationResult(final Builder builder) {
    this.testName = Objects.requireNonNull(builder.testName, "testName cannot be null");
    this.consistent = builder.consistent;
    this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
    this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
    this.info = Collections.unmodifiableList(new ArrayList<>(builder.info));
  }

  /**
   * Gets the test name.
   *
   * @return the test name
   */
  public String getTestName() {
    return testName;
  }

  /**
   * Checks if the runtimes are consistent.
   *
   * @return true if consistent
   */
  public boolean isConsistent() {
    return consistent;
  }

  /**
   * Gets the validation errors.
   *
   * @return the list of errors
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Gets the validation warnings.
   *
   * @return the list of warnings
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * Gets the validation info.
   *
   * @return the list of info messages
   */
  public List<String> getInfo() {
    return info;
  }

  /**
   * Checks if there are validation errors.
   *
   * @return true if there are errors
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Checks if there are validation warnings.
   *
   * @return true if there are warnings
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Gets a summary of the validation result.
   *
   * @return the summary
   */
  public String getSummary() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Validation: ").append(testName).append(" - ");
    sb.append(consistent ? "CONSISTENT" : "INCONSISTENT");

    if (hasErrors()) {
      sb.append(" (").append(errors.size()).append(" errors)");
    }
    if (hasWarnings()) {
      sb.append(" (").append(warnings.size()).append(" warnings)");
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("CrossRuntimeValidationResult{\n");
    sb.append("  testName: ").append(testName).append('\n');
    sb.append("  consistent: ").append(consistent).append('\n');

    if (!errors.isEmpty()) {
      sb.append("  errors:\n");
      for (final String error : errors) {
        sb.append("    - ").append(error).append('\n');
      }
    }

    if (!warnings.isEmpty()) {
      sb.append("  warnings:\n");
      for (final String warning : warnings) {
        sb.append("    - ").append(warning).append('\n');
      }
    }

    if (!info.isEmpty()) {
      sb.append("  info:\n");
      for (final String infoItem : info) {
        sb.append("    - ").append(infoItem).append('\n');
      }
    }

    sb.append("}");
    return sb.toString();
  }

  /** Builder for validation results. */
  public static final class Builder {
    private final String testName;
    private boolean consistent = false;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> info = new ArrayList<>();

    public Builder(final String testName) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    }

    public Builder consistent(final boolean consistent) {
      this.consistent = consistent;
      return this;
    }

    public Builder addError(final String error) {
      this.errors.add(Objects.requireNonNull(error, "error cannot be null"));
      return this;
    }

    public Builder addWarning(final String warning) {
      this.warnings.add(Objects.requireNonNull(warning, "warning cannot be null"));
      return this;
    }

    public Builder addInfo(final String info) {
      this.info.add(Objects.requireNonNull(info, "info cannot be null"));
      return this;
    }

    public CrossRuntimeValidationResult build() {
      return new CrossRuntimeValidationResult(this);
    }
  }
}

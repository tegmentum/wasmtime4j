package ai.tegmentum.wasmtime4j.component;

import java.util.List;

/**
 * Result of component validation operations.
 *
 * <p>This class provides detailed information about component validation, including validation
 * status, errors, warnings, and recommendations.
 *
 * @since 1.0.0
 */
public final class ComponentValidationResult {

  private final boolean valid;
  private final List<ValidationError> errors;
  private final List<ValidationWarning> warnings;
  private final List<String> recommendations;
  private final ValidationContext context;

  /**
   * Creates a new component validation result.
   *
   * @param valid whether the component is valid
   * @param errors list of validation errors
   * @param warnings list of validation warnings
   * @param recommendations list of recommendations
   * @param context the validation context
   */
  public ComponentValidationResult(
      final boolean valid,
      final List<ValidationError> errors,
      final List<ValidationWarning> warnings,
      final List<String> recommendations,
      final ValidationContext context) {
    this.valid = valid;
    this.errors = List.copyOf(errors);
    this.warnings = List.copyOf(warnings);
    this.recommendations = List.copyOf(recommendations);
    this.context = context;
  }

  /**
   * Creates a successful validation result.
   *
   * @param context the validation context
   * @return a successful validation result
   */
  public static ComponentValidationResult success(final ValidationContext context) {
    return new ComponentValidationResult(true, List.of(), List.of(), List.of(), context);
  }

  /**
   * Creates a failed validation result with errors.
   *
   * @param errors list of validation errors
   * @param context the validation context
   * @return a failed validation result
   */
  public static ComponentValidationResult failure(
      final List<ValidationError> errors, final ValidationContext context) {
    return new ComponentValidationResult(false, errors, List.of(), List.of(), context);
  }

  /**
   * Checks if the component is valid.
   *
   * @return true if valid, false otherwise
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * Gets the list of validation errors.
   *
   * @return list of errors
   */
  public List<ValidationError> getErrors() {
    return errors;
  }

  /**
   * Gets the list of validation warnings.
   *
   * @return list of warnings
   */
  public List<ValidationWarning> getWarnings() {
    return warnings;
  }

  /**
   * Gets the list of recommendations.
   *
   * @return list of recommendations
   */
  public List<String> getRecommendations() {
    return recommendations;
  }

  /**
   * Gets the validation context.
   *
   * @return the validation context
   */
  public ValidationContext getContext() {
    return context;
  }

  /**
   * Checks if there are any errors.
   *
   * @return true if there are errors
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Checks if there are any warnings.
   *
   * @return true if there are warnings
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Gets the total number of issues (errors + warnings).
   *
   * @return total number of issues
   */
  public int getIssueCount() {
    return errors.size() + warnings.size();
  }

  /**
   * Gets a summary of the validation result.
   *
   * @return validation summary
   */
  public String getSummary() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Validation: ").append(valid ? "PASSED" : "FAILED");

    if (hasErrors()) {
      sb.append(", Errors: ").append(errors.size());
    }

    if (hasWarnings()) {
      sb.append(", Warnings: ").append(warnings.size());
    }

    if (!recommendations.isEmpty()) {
      sb.append(", Recommendations: ").append(recommendations.size());
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    return getSummary();
  }

  /** Represents a validation error. */
  public static final class ValidationError {
    private final String code;
    private final String message;
    private final String location;
    private final ErrorSeverity severity;

    /**
     * Creates a new validation error.
     *
     * @param code error code
     * @param message error message
     * @param location location where error occurred
     * @param severity severity level of the error
     */
    public ValidationError(
        final String code,
        final String message,
        final String location,
        final ErrorSeverity severity) {
      this.code = code;
      this.message = message;
      this.location = location;
      this.severity = severity;
    }

    public String getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    public String getLocation() {
      return location;
    }

    public ErrorSeverity getSeverity() {
      return severity;
    }

    @Override
    public String toString() {
      return String.format("[%s] %s at %s: %s", severity, code, location, message);
    }
  }

  /** Represents a validation warning. */
  public static final class ValidationWarning {
    private final String code;
    private final String message;
    private final String location;

    /**
     * Creates a new validation warning.
     *
     * @param code warning code
     * @param message warning message
     * @param location location where warning occurred
     */
    public ValidationWarning(final String code, final String message, final String location) {
      this.code = code;
      this.message = message;
      this.location = location;
    }

    public String getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    public String getLocation() {
      return location;
    }

    @Override
    public String toString() {
      return String.format("[WARNING] %s at %s: %s", code, location, message);
    }
  }

  /** Validation context information. */
  public static final class ValidationContext {
    private final String componentId;
    private final ComponentVersion version;
    private final long timestamp;

    /**
     * Creates a new validation context.
     *
     * @param componentId identifier of the component being validated
     * @param version version of the component
     */
    public ValidationContext(final String componentId, final ComponentVersion version) {
      this.componentId = componentId;
      this.version = version;
      this.timestamp = System.currentTimeMillis();
    }

    public String getComponentId() {
      return componentId;
    }

    public ComponentVersion getVersion() {
      return version;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }

  /** Error severity levels. */
  public enum ErrorSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }
}

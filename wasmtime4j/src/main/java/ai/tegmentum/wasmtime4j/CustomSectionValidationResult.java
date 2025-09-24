package ai.tegmentum.wasmtime4j;

import java.util.List;

/**
 * Result of custom section validation operations.
 *
 * <p>This class contains the results of validating WebAssembly custom sections, including any
 * errors, warnings, and diagnostic information discovered during validation.
 *
 * @since 1.0.0
 */
public final class CustomSectionValidationResult {

  private final boolean valid;
  private final List<ValidationIssue> errors;
  private final List<ValidationIssue> warnings;
  private final String summary;

  private CustomSectionValidationResult(final Builder builder) {
    this.valid = builder.errors.isEmpty();
    this.errors = java.util.Collections.unmodifiableList(builder.errors);
    this.warnings = java.util.Collections.unmodifiableList(builder.warnings);
    this.summary = builder.summary;
  }

  /**
   * Checks if the custom sections are valid.
   *
   * @return true if no errors were found
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * Gets all validation errors.
   *
   * @return an immutable list of validation errors
   */
  public List<ValidationIssue> getErrors() {
    return errors;
  }

  /**
   * Gets all validation warnings.
   *
   * @return an immutable list of validation warnings
   */
  public List<ValidationIssue> getWarnings() {
    return warnings;
  }

  /**
   * Gets the validation summary.
   *
   * @return a human-readable summary of the validation results
   */
  public String getSummary() {
    return summary != null ? summary : generateDefaultSummary();
  }

  /**
   * Checks if there are any validation issues.
   *
   * @return true if there are errors or warnings
   */
  public boolean hasIssues() {
    return !errors.isEmpty() || !warnings.isEmpty();
  }

  /**
   * Gets the total number of issues.
   *
   * @return the sum of errors and warnings
   */
  public int getIssueCount() {
    return errors.size() + warnings.size();
  }

  /**
   * Gets all validation issues (errors and warnings combined).
   *
   * @return an immutable list of all issues
   */
  public List<ValidationIssue> getAllIssues() {
    final List<ValidationIssue> allIssues = new java.util.ArrayList<>();
    allIssues.addAll(errors);
    allIssues.addAll(warnings);
    return java.util.Collections.unmodifiableList(allIssues);
  }

  /**
   * Gets validation issues for a specific custom section.
   *
   * @param sectionName the name of the section
   * @return list of issues for the specified section
   * @throws IllegalArgumentException if sectionName is null
   */
  public List<ValidationIssue> getIssuesForSection(final String sectionName) {
    if (sectionName == null) {
      throw new IllegalArgumentException("Section name cannot be null");
    }

    return getAllIssues().stream()
        .filter(issue -> sectionName.equals(issue.getSectionName()))
        .collect(java.util.stream.Collectors.collectingAndThen(
            java.util.stream.Collectors.toList(),
            java.util.Collections::unmodifiableList));
  }

  /**
   * Creates a successful validation result.
   *
   * @return a validation result with no errors or warnings
   */
  public static CustomSectionValidationResult success() {
    return builder().build();
  }

  /**
   * Creates a successful validation result with warnings.
   *
   * @param warnings the validation warnings
   * @return a validation result with no errors but with warnings
   * @throws IllegalArgumentException if warnings is null
   */
  public static CustomSectionValidationResult successWithWarnings(final List<ValidationIssue> warnings) {
    if (warnings == null) {
      throw new IllegalArgumentException("Warnings cannot be null");
    }
    return builder().setWarnings(warnings).build();
  }

  /**
   * Creates a failed validation result.
   *
   * @param errors the validation errors
   * @return a validation result with errors
   * @throws IllegalArgumentException if errors is null or empty
   */
  public static CustomSectionValidationResult failure(final List<ValidationIssue> errors) {
    if (errors == null || errors.isEmpty()) {
      throw new IllegalArgumentException("Errors cannot be null or empty for failure result");
    }
    return builder().setErrors(errors).build();
  }

  /**
   * Creates a failed validation result with warnings.
   *
   * @param errors the validation errors
   * @param warnings the validation warnings
   * @return a validation result with both errors and warnings
   * @throws IllegalArgumentException if errors is null or empty, or warnings is null
   */
  public static CustomSectionValidationResult failure(final List<ValidationIssue> errors,
                                                      final List<ValidationIssue> warnings) {
    if (errors == null || errors.isEmpty()) {
      throw new IllegalArgumentException("Errors cannot be null or empty for failure result");
    }
    if (warnings == null) {
      throw new IllegalArgumentException("Warnings cannot be null");
    }
    return builder().setErrors(errors).setWarnings(warnings).build();
  }

  /**
   * Creates a new builder for constructing validation results.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  private String generateDefaultSummary() {
    if (valid && warnings.isEmpty()) {
      return "All custom sections are valid";
    } else if (valid) {
      return String.format("Custom sections are valid with %d warning(s)", warnings.size());
    } else {
      return String.format("Custom sections validation failed with %d error(s) and %d warning(s)",
          errors.size(), warnings.size());
    }
  }

  @Override
  public String toString() {
    return String.format("CustomSectionValidationResult{valid=%s, errors=%d, warnings=%d}",
        valid, errors.size(), warnings.size());
  }

  /**
   * Builder for constructing CustomSectionValidationResult instances.
   */
  public static final class Builder {
    private final List<ValidationIssue> errors = new java.util.ArrayList<>();
    private final List<ValidationIssue> warnings = new java.util.ArrayList<>();
    private String summary;

    private Builder() {}

    /**
     * Sets the validation errors.
     *
     * @param errors list of validation errors
     * @return this builder
     */
    public Builder setErrors(final List<ValidationIssue> errors) {
      this.errors.clear();
      if (errors != null) {
        this.errors.addAll(errors);
      }
      return this;
    }

    /**
     * Sets the validation warnings.
     *
     * @param warnings list of validation warnings
     * @return this builder
     */
    public Builder setWarnings(final List<ValidationIssue> warnings) {
      this.warnings.clear();
      if (warnings != null) {
        this.warnings.addAll(warnings);
      }
      return this;
    }

    /**
     * Adds a validation error.
     *
     * @param error the validation error to add
     * @return this builder
     * @throws IllegalArgumentException if error is null
     */
    public Builder addError(final ValidationIssue error) {
      if (error == null) {
        throw new IllegalArgumentException("Validation error cannot be null");
      }
      this.errors.add(error);
      return this;
    }

    /**
     * Adds a validation warning.
     *
     * @param warning the validation warning to add
     * @return this builder
     * @throws IllegalArgumentException if warning is null
     */
    public Builder addWarning(final ValidationIssue warning) {
      if (warning == null) {
        throw new IllegalArgumentException("Validation warning cannot be null");
      }
      this.warnings.add(warning);
      return this;
    }

    /**
     * Adds a validation error with section and message.
     *
     * @param sectionName the section name
     * @param message the error message
     * @return this builder
     * @throws IllegalArgumentException if sectionName or message is null
     */
    public Builder addError(final String sectionName, final String message) {
      return addError(ValidationIssue.error(sectionName, message));
    }

    /**
     * Adds a validation warning with section and message.
     *
     * @param sectionName the section name
     * @param message the warning message
     * @return this builder
     * @throws IllegalArgumentException if sectionName or message is null
     */
    public Builder addWarning(final String sectionName, final String message) {
      return addWarning(ValidationIssue.warning(sectionName, message));
    }

    /**
     * Sets the validation summary.
     *
     * @param summary the validation summary
     * @return this builder
     */
    public Builder setSummary(final String summary) {
      this.summary = summary;
      return this;
    }

    /**
     * Builds the validation result.
     *
     * @return a new CustomSectionValidationResult instance
     */
    public CustomSectionValidationResult build() {
      return new CustomSectionValidationResult(this);
    }
  }

  /**
   * Represents a single validation issue.
   */
  public static final class ValidationIssue {
    private final ValidationIssueType type;
    private final String sectionName;
    private final String message;
    private final String details;

    /**
     * Creates a new validation issue.
     *
     * @param type the issue type
     * @param sectionName the section name
     * @param message the issue message
     * @param details additional details
     * @throws IllegalArgumentException if type, sectionName, or message is null
     */
    public ValidationIssue(final ValidationIssueType type,
                          final String sectionName,
                          final String message,
                          final String details) {
      if (type == null) {
        throw new IllegalArgumentException("Validation issue type cannot be null");
      }
      if (sectionName == null) {
        throw new IllegalArgumentException("Section name cannot be null");
      }
      if (message == null) {
        throw new IllegalArgumentException("Message cannot be null");
      }

      this.type = type;
      this.sectionName = sectionName;
      this.message = message;
      this.details = details;
    }

    /**
     * Creates a new validation issue without details.
     *
     * @param type the issue type
     * @param sectionName the section name
     * @param message the issue message
     * @throws IllegalArgumentException if type, sectionName, or message is null
     */
    public ValidationIssue(final ValidationIssueType type,
                          final String sectionName,
                          final String message) {
      this(type, sectionName, message, null);
    }

    /**
     * Creates a validation error.
     *
     * @param sectionName the section name
     * @param message the error message
     * @return a new validation error
     * @throws IllegalArgumentException if sectionName or message is null
     */
    public static ValidationIssue error(final String sectionName, final String message) {
      return new ValidationIssue(ValidationIssueType.ERROR, sectionName, message);
    }

    /**
     * Creates a validation warning.
     *
     * @param sectionName the section name
     * @param message the warning message
     * @return a new validation warning
     * @throws IllegalArgumentException if sectionName or message is null
     */
    public static ValidationIssue warning(final String sectionName, final String message) {
      return new ValidationIssue(ValidationIssueType.WARNING, sectionName, message);
    }

    /**
     * Gets the issue type.
     *
     * @return the validation issue type
     */
    public ValidationIssueType getType() {
      return type;
    }

    /**
     * Gets the section name.
     *
     * @return the section name
     */
    public String getSectionName() {
      return sectionName;
    }

    /**
     * Gets the issue message.
     *
     * @return the issue message
     */
    public String getMessage() {
      return message;
    }

    /**
     * Gets additional details.
     *
     * @return the additional details, or null if not provided
     */
    public String getDetails() {
      return details;
    }

    /**
     * Checks if this is an error.
     *
     * @return true if this is an error
     */
    public boolean isError() {
      return type == ValidationIssueType.ERROR;
    }

    /**
     * Checks if this is a warning.
     *
     * @return true if this is a warning
     */
    public boolean isWarning() {
      return type == ValidationIssueType.WARNING;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(type).append(" in section '").append(sectionName).append("': ").append(message);
      if (details != null && !details.trim().isEmpty()) {
        sb.append(" (").append(details).append(")");
      }
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
      final ValidationIssue that = (ValidationIssue) obj;
      return type == that.type
          && sectionName.equals(that.sectionName)
          && message.equals(that.message)
          && java.util.Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(type, sectionName, message, details);
    }
  }

  /**
   * Types of validation issues.
   */
  public enum ValidationIssueType {
    /** Critical validation error */
    ERROR,
    /** Non-critical validation warning */
    WARNING
  }
}
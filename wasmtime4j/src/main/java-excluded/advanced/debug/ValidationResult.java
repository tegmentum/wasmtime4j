package ai.tegmentum.wasmtime4j.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of source map or DWARF information validation.
 *
 * <p>This class contains validation errors and warnings discovered during
 * source map or debugging information parsing and validation. It provides
 * detailed feedback to help identify and resolve issues.
 *
 * @since 1.0.0
 */
public final class ValidationResult {

    private final List<String> errors;
    private final List<String> warnings;
    private final long validationTime;

    /**
     * Creates a new validation result.
     */
    public ValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.validationTime = System.currentTimeMillis();
    }

    /**
     * Creates a validation result with initial errors and warnings.
     *
     * @param errors initial errors
     * @param warnings initial warnings
     */
    public ValidationResult(final List<String> errors, final List<String> warnings) {
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        this.validationTime = System.currentTimeMillis();
    }

    /**
     * Adds a validation error.
     *
     * @param error the error message
     * @throws IllegalArgumentException if error is null or empty
     */
    public void addError(final String error) {
        if (error == null || error.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }
        errors.add(error);
    }

    /**
     * Adds a validation warning.
     *
     * @param warning the warning message
     * @throws IllegalArgumentException if warning is null or empty
     */
    public void addWarning(final String warning) {
        if (warning == null || warning.trim().isEmpty()) {
            throw new IllegalArgumentException("Warning message cannot be null or empty");
        }
        warnings.add(warning);
    }

    /**
     * Gets the list of validation errors.
     *
     * @return an immutable list of error messages
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Gets the list of validation warnings.
     *
     * @return an immutable list of warning messages
     */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Checks if validation passed (no errors).
     *
     * @return true if there are no errors
     */
    public boolean isValid() {
        return errors.isEmpty();
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
     * Checks if there are any errors.
     *
     * @return true if there are errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Gets the total number of issues (errors + warnings).
     *
     * @return the total issue count
     */
    public int getTotalIssues() {
        return errors.size() + warnings.size();
    }

    /**
     * Gets the number of errors.
     *
     * @return the error count
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Gets the number of warnings.
     *
     * @return the warning count
     */
    public int getWarningCount() {
        return warnings.size();
    }

    /**
     * Gets the validation timestamp.
     *
     * @return the validation time in milliseconds since epoch
     */
    public long getValidationTime() {
        return validationTime;
    }

    /**
     * Merges another validation result into this one.
     *
     * @param other the other validation result
     * @throws IllegalArgumentException if other is null
     */
    public void merge(final ValidationResult other) {
        if (other == null) {
            throw new IllegalArgumentException("Other validation result cannot be null");
        }
        errors.addAll(other.errors);
        warnings.addAll(other.warnings);
    }

    /**
     * Clears all errors and warnings.
     */
    public void clear() {
        errors.clear();
        warnings.clear();
    }

    /**
     * Creates a formatted summary of the validation results.
     *
     * @return formatted summary string
     */
    public String getSummary() {
        if (isValid() && !hasWarnings()) {
            return "Validation passed with no issues";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Validation completed with ");

        if (hasErrors()) {
            sb.append(errors.size()).append(" error").append(errors.size() == 1 ? "" : "s");
            if (hasWarnings()) {
                sb.append(" and ");
            }
        }

        if (hasWarnings()) {
            sb.append(warnings.size()).append(" warning").append(warnings.size() == 1 ? "" : "s");
        }

        return sb.toString();
    }

    /**
     * Creates a detailed report of all issues.
     *
     * @return detailed report string
     */
    public String getDetailedReport() {
        if (isValid() && !hasWarnings()) {
            return "Validation passed with no issues";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Validation Report:\n");

        if (hasErrors()) {
            sb.append("\nErrors (").append(errors.size()).append("):\n");
            for (int i = 0; i < errors.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
            }
        }

        if (hasWarnings()) {
            sb.append("\nWarnings (").append(warnings.size()).append("):\n");
            for (int i = 0; i < warnings.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(warnings.get(i)).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Creates a successful validation result.
     *
     * @return a validation result with no errors or warnings
     */
    public static ValidationResult success() {
        return new ValidationResult();
    }

    /**
     * Creates a validation result with a single error.
     *
     * @param error the error message
     * @return a validation result with the error
     */
    public static ValidationResult withError(final String error) {
        final ValidationResult result = new ValidationResult();
        result.addError(error);
        return result;
    }

    /**
     * Creates a validation result with a single warning.
     *
     * @param warning the warning message
     * @return a validation result with the warning
     */
    public static ValidationResult withWarning(final String warning) {
        final ValidationResult result = new ValidationResult();
        result.addWarning(warning);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final ValidationResult that = (ValidationResult) obj;
        return Objects.equals(errors, that.errors) && Objects.equals(warnings, that.warnings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errors, warnings);
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
package ai.tegmentum.wasmtime4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Result of WIT interface evolution validation.
 *
 * <p>This class provides comprehensive validation information for interface evolution operations,
 * including constraint violations, compatibility issues, and evolution recommendations.
 *
 * @since 1.0.0
 */
public final class WitEvolutionValidation {

  private final boolean valid;
  private final List<ConstraintViolation> violations;
  private final List<CompatibilityIssue> issues;
  private final List<EvolutionWarning> warnings;
  private final EvolutionRisk riskAssessment;
  private final List<String> recommendations;
  private final Instant validationTime;
  private final ValidationMetrics metrics;

  /**
   * Creates a new evolution validation result.
   *
   * @param valid whether validation passed
   * @param violations constraint violations
   * @param issues compatibility issues
   * @param warnings evolution warnings
   * @param riskAssessment risk assessment
   * @param recommendations evolution recommendations
   * @param validationTime validation timestamp
   * @param metrics validation metrics
   */
  public WitEvolutionValidation(
      final boolean valid,
      final List<ConstraintViolation> violations,
      final List<CompatibilityIssue> issues,
      final List<EvolutionWarning> warnings,
      final EvolutionRisk riskAssessment,
      final List<String> recommendations,
      final Instant validationTime,
      final ValidationMetrics metrics) {
    this.valid = valid;
    this.violations = List.copyOf(violations);
    this.issues = List.copyOf(issues);
    this.warnings = List.copyOf(warnings);
    this.riskAssessment = riskAssessment;
    this.recommendations = List.copyOf(recommendations);
    this.validationTime = validationTime;
    this.metrics = metrics;
  }

  /**
   * Creates a successful validation result.
   *
   * @param recommendations evolution recommendations
   * @param metrics validation metrics
   * @return successful validation result
   */
  public static WitEvolutionValidation success(
      final List<String> recommendations, final ValidationMetrics metrics) {
    return new WitEvolutionValidation(
        true,
        List.of(),
        List.of(),
        List.of(),
        EvolutionRisk.LOW,
        recommendations,
        Instant.now(),
        metrics);
  }

  /**
   * Creates a failed validation result.
   *
   * @param violations constraint violations
   * @param issues compatibility issues
   * @param metrics validation metrics
   * @return failed validation result
   */
  public static WitEvolutionValidation failure(
      final List<ConstraintViolation> violations,
      final List<CompatibilityIssue> issues,
      final ValidationMetrics metrics) {
    return new WitEvolutionValidation(
        false,
        violations,
        issues,
        List.of(),
        EvolutionRisk.HIGH,
        List.of(),
        Instant.now(),
        metrics);
  }

  /**
   * Creates a validation result with warnings.
   *
   * @param warnings evolution warnings
   * @param riskAssessment risk assessment
   * @param recommendations evolution recommendations
   * @param metrics validation metrics
   * @return validation result with warnings
   */
  public static WitEvolutionValidation withWarnings(
      final List<EvolutionWarning> warnings,
      final EvolutionRisk riskAssessment,
      final List<String> recommendations,
      final ValidationMetrics metrics) {
    return new WitEvolutionValidation(
        true,
        List.of(),
        List.of(),
        warnings,
        riskAssessment,
        recommendations,
        Instant.now(),
        metrics);
  }

  /**
   * Checks if validation passed.
   *
   * @return true if valid
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * Gets constraint violations.
   *
   * @return list of violations
   */
  public List<ConstraintViolation> getViolations() {
    return violations;
  }

  /**
   * Gets compatibility issues.
   *
   * @return list of issues
   */
  public List<CompatibilityIssue> getIssues() {
    return issues;
  }

  /**
   * Gets evolution warnings.
   *
   * @return list of warnings
   */
  public List<EvolutionWarning> getWarnings() {
    return warnings;
  }

  /**
   * Gets risk assessment.
   *
   * @return risk assessment
   */
  public EvolutionRisk getRiskAssessment() {
    return riskAssessment;
  }

  /**
   * Gets evolution recommendations.
   *
   * @return list of recommendations
   */
  public List<String> getRecommendations() {
    return recommendations;
  }

  /**
   * Gets validation timestamp.
   *
   * @return validation time
   */
  public Instant getValidationTime() {
    return validationTime;
  }

  /**
   * Gets validation metrics.
   *
   * @return validation metrics
   */
  public ValidationMetrics getMetrics() {
    return metrics;
  }

  /**
   * Checks if there are any violations.
   *
   * @return true if there are violations
   */
  public boolean hasViolations() {
    return !violations.isEmpty();
  }

  /**
   * Checks if there are any issues.
   *
   * @return true if there are issues
   */
  public boolean hasIssues() {
    return !issues.isEmpty();
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
   * Gets violations of a specific type.
   *
   * @param type violation type
   * @return filtered violations
   */
  public List<ConstraintViolation> getViolationsByType(final ViolationType type) {
    return violations.stream().filter(v -> v.getType() == type).toList();
  }

  /**
   * Gets issues of a specific severity.
   *
   * @param severity issue severity
   * @return filtered issues
   */
  public List<CompatibilityIssue> getIssuesBySeverity(final IssueSeverity severity) {
    return issues.stream().filter(i -> i.getSeverity() == severity).toList();
  }

  @Override
  public String toString() {
    return "WitEvolutionValidation{"
        + "valid="
        + valid
        + ", violations="
        + violations.size()
        + ", issues="
        + issues.size()
        + ", warnings="
        + warnings.size()
        + ", riskAssessment="
        + riskAssessment
        + ", validationTime="
        + validationTime
        + '}';
  }

  /** Constraint violation information. */
  public static final class ConstraintViolation {
    private final ViolationType type;
    private final String description;
    private final String location;
    private final ViolationSeverity severity;
    private final Optional<String> suggestion;

    /**
     * Creates a constraint violation.
     *
     * @param type violation type
     * @param description violation description
     * @param location violation location
     * @param severity violation severity
     * @param suggestion optional suggestion
     */
    public ConstraintViolation(
        final ViolationType type,
        final String description,
        final String location,
        final ViolationSeverity severity,
        final Optional<String> suggestion) {
      this.type = type;
      this.description = description;
      this.location = location;
      this.severity = severity;
      this.suggestion = suggestion;
    }

    /**
     * Gets violation type.
     *
     * @return violation type
     */
    public ViolationType getType() {
      return type;
    }

    /**
     * Gets violation description.
     *
     * @return description
     */
    public String getDescription() {
      return description;
    }

    /**
     * Gets violation location.
     *
     * @return location
     */
    public String getLocation() {
      return location;
    }

    /**
     * Gets violation severity.
     *
     * @return severity
     */
    public ViolationSeverity getSeverity() {
      return severity;
    }

    /**
     * Gets suggestion for fixing the violation.
     *
     * @return optional suggestion
     */
    public Optional<String> getSuggestion() {
      return suggestion;
    }

    @Override
    public String toString() {
      return "ConstraintViolation{"
          + "type="
          + type
          + ", description='"
          + description
          + '\''
          + ", location='"
          + location
          + '\''
          + ", severity="
          + severity
          + '}';
    }
  }

  /** Compatibility issue information. */
  public static final class CompatibilityIssue {
    private final IssueType type;
    private final String description;
    private final String location;
    private final IssueSeverity severity;
    private final List<String> resolutions;
    private final CompatibilityLevel impactLevel;

    /**
     * Creates a compatibility issue.
     *
     * @param type issue type
     * @param description issue description
     * @param location issue location
     * @param severity issue severity
     * @param resolutions possible resolutions
     * @param impactLevel compatibility impact level
     */
    public CompatibilityIssue(
        final IssueType type,
        final String description,
        final String location,
        final IssueSeverity severity,
        final List<String> resolutions,
        final CompatibilityLevel impactLevel) {
      this.type = type;
      this.description = description;
      this.location = location;
      this.severity = severity;
      this.resolutions = List.copyOf(resolutions);
      this.impactLevel = impactLevel;
    }

    /**
     * Gets issue type.
     *
     * @return issue type
     */
    public IssueType getType() {
      return type;
    }

    /**
     * Gets issue description.
     *
     * @return description
     */
    public String getDescription() {
      return description;
    }

    /**
     * Gets issue location.
     *
     * @return location
     */
    public String getLocation() {
      return location;
    }

    /**
     * Gets issue severity.
     *
     * @return severity
     */
    public IssueSeverity getSeverity() {
      return severity;
    }

    /**
     * Gets possible resolutions.
     *
     * @return list of resolutions
     */
    public List<String> getResolutions() {
      return resolutions;
    }

    /**
     * Gets compatibility impact level.
     *
     * @return impact level
     */
    public CompatibilityLevel getImpactLevel() {
      return impactLevel;
    }

    @Override
    public String toString() {
      return "CompatibilityIssue{"
          + "type="
          + type
          + ", description='"
          + description
          + '\''
          + ", location='"
          + location
          + '\''
          + ", severity="
          + severity
          + ", impactLevel="
          + impactLevel
          + '}';
    }
  }

  /** Evolution warning information. */
  public static final class EvolutionWarning {
    private final WarningType type;
    private final String message;
    private final String location;
    private final Optional<String> recommendation;

    /**
     * Creates an evolution warning.
     *
     * @param type warning type
     * @param message warning message
     * @param location warning location
     * @param recommendation optional recommendation
     */
    public EvolutionWarning(
        final WarningType type,
        final String message,
        final String location,
        final Optional<String> recommendation) {
      this.type = type;
      this.message = message;
      this.location = location;
      this.recommendation = recommendation;
    }

    /**
     * Gets warning type.
     *
     * @return warning type
     */
    public WarningType getType() {
      return type;
    }

    /**
     * Gets warning message.
     *
     * @return message
     */
    public String getMessage() {
      return message;
    }

    /**
     * Gets warning location.
     *
     * @return location
     */
    public String getLocation() {
      return location;
    }

    /**
     * Gets warning recommendation.
     *
     * @return optional recommendation
     */
    public Optional<String> getRecommendation() {
      return recommendation;
    }

    @Override
    public String toString() {
      return "EvolutionWarning{"
          + "type="
          + type
          + ", message='"
          + message
          + '\''
          + ", location='"
          + location
          + '\''
          + '}';
    }
  }

  /** Validation metrics information. */
  public interface ValidationMetrics {
    /**
     * Gets validation duration.
     *
     * @return validation duration
     */
    java.time.Duration getValidationDuration();

    /**
     * Gets number of constraints checked.
     *
     * @return constraint count
     */
    int getConstraintsChecked();

    /**
     * Gets number of types analyzed.
     *
     * @return type count
     */
    int getTypesAnalyzed();

    /**
     * Gets number of functions analyzed.
     *
     * @return function count
     */
    int getFunctionsAnalyzed();

    /**
     * Gets validation score (0.0 to 1.0).
     *
     * @return validation score
     */
    double getValidationScore();

    /**
     * Gets detailed metrics.
     *
     * @return metrics map
     */
    Map<String, Object> getDetailedMetrics();
  }

  // Enums for categorization
  /** Types of evolution validation violations. */
  public enum ViolationType {
    SEMANTIC_VERSION_VIOLATION,
    BACKWARD_COMPATIBILITY_VIOLATION,
    TYPE_SAFETY_VIOLATION,
    INTERFACE_CONTRACT_VIOLATION,
    DEPENDENCY_VIOLATION
  }

  /** Severity levels for validation violations. */
  public enum ViolationSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  /** Types of issues found during validation. */
  public enum IssueType {
    TYPE_MISMATCH,
    MISSING_FUNCTION,
    INCOMPATIBLE_SIGNATURE,
    VERSION_CONFLICT,
    DEPENDENCY_ISSUE
  }

  /** Severity levels for validation issues. */
  public enum IssueSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
  }

  /** Types of warnings during evolution validation. */
  public enum WarningType {
    PERFORMANCE_WARNING,
    DEPRECATION_WARNING,
    COMPATIBILITY_WARNING,
    BEST_PRACTICE_WARNING,
    SECURITY_WARNING
  }

  /** Risk levels for interface evolution. */
  public enum EvolutionRisk {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  /** Levels of backward compatibility. */
  public enum CompatibilityLevel {
    FULL,
    PARTIAL,
    LIMITED,
    NONE
  }
}

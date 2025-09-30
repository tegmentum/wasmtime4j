package ai.tegmentum.wasmtime4j.execution;

/**
 * Controller validation result interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ControllerValidationResult {

  /**
   * Checks if validation was successful.
   *
   * @return true if validation passed
   */
  boolean isValid();

  /**
   * Gets validation errors.
   *
   * @return list of validation errors
   */
  java.util.List<ValidationError> getErrors();

  /**
   * Gets validation warnings.
   *
   * @return list of validation warnings
   */
  java.util.List<ValidationWarning> getWarnings();

  /**
   * Gets the validation timestamp.
   *
   * @return validation timestamp
   */
  long getTimestamp();

  /**
   * Gets the validation duration.
   *
   * @return validation duration in milliseconds
   */
  long getDuration();

  /**
   * Gets the validator version.
   *
   * @return validator version
   */
  String getValidatorVersion();

  /**
   * Gets validation context information.
   *
   * @return validation context
   */
  ValidationContext getContext();

  /**
   * Gets validation statistics.
   *
   * @return validation statistics
   */
  ValidationStatistics getStatistics();

  /**
   * Gets the validation summary.
   *
   * @return validation summary
   */
  String getSummary();

  /**
   * Gets detailed validation report.
   *
   * @return detailed report
   */
  ValidationReport getDetailedReport();

  /**
   * Gets validation recommendations.
   *
   * @return list of recommendations
   */
  java.util.List<ValidationRecommendation> getRecommendations();

  /** Validation error interface. */
  interface ValidationError {
    /**
     * Gets the error code.
     *
     * @return error code
     */
    String getCode();

    /**
     * Gets the error message.
     *
     * @return error message
     */
    String getMessage();

    /**
     * Gets the error severity.
     *
     * @return error severity
     */
    ErrorSeverity getSeverity();

    /**
     * Gets the error category.
     *
     * @return error category
     */
    ErrorCategory getCategory();

    /**
     * Gets the error location.
     *
     * @return error location
     */
    String getLocation();

    /**
     * Gets suggested fixes.
     *
     * @return list of suggested fixes
     */
    java.util.List<String> getSuggestedFixes();

    /**
     * Gets related documentation links.
     *
     * @return list of documentation links
     */
    java.util.List<String> getDocumentationLinks();
  }

  /** Validation warning interface. */
  interface ValidationWarning {
    /**
     * Gets the warning code.
     *
     * @return warning code
     */
    String getCode();

    /**
     * Gets the warning message.
     *
     * @return warning message
     */
    String getMessage();

    /**
     * Gets the warning category.
     *
     * @return warning category
     */
    WarningCategory getCategory();

    /**
     * Gets the warning location.
     *
     * @return warning location
     */
    String getLocation();

    /**
     * Gets the impact assessment.
     *
     * @return impact assessment
     */
    String getImpactAssessment();

    /**
     * Checks if the warning can be suppressed.
     *
     * @return true if suppressible
     */
    boolean isSuppressible();
  }

  /** Validation context interface. */
  interface ValidationContext {
    /**
     * Gets the context ID.
     *
     * @return context ID
     */
    String getId();

    /**
     * Gets the validation type.
     *
     * @return validation type
     */
    ValidationType getType();

    /**
     * Gets the validation scope.
     *
     * @return validation scope
     */
    ValidationScope getScope();

    /**
     * Gets validation parameters.
     *
     * @return validation parameters
     */
    java.util.Map<String, Object> getParameters();

    /**
     * Gets the environment information.
     *
     * @return environment info
     */
    EnvironmentInfo getEnvironment();
  }

  /** Validation statistics interface. */
  interface ValidationStatistics {
    /**
     * Gets total checks performed.
     *
     * @return check count
     */
    int getTotalChecks();

    /**
     * Gets passed checks count.
     *
     * @return passed count
     */
    int getPassedChecks();

    /**
     * Gets failed checks count.
     *
     * @return failed count
     */
    int getFailedChecks();

    /**
     * Gets warning checks count.
     *
     * @return warning count
     */
    int getWarningChecks();

    /**
     * Gets skipped checks count.
     *
     * @return skipped count
     */
    int getSkippedChecks();

    /**
     * Gets validation coverage.
     *
     * @return coverage percentage (0.0-1.0)
     */
    double getCoverage();

    /**
     * Gets performance metrics.
     *
     * @return performance metrics
     */
    ValidationPerformanceMetrics getPerformanceMetrics();
  }

  /** Validation report interface. */
  interface ValidationReport {
    /**
     * Gets the report format.
     *
     * @return report format
     */
    ReportFormat getFormat();

    /**
     * Gets the report content.
     *
     * @return report content
     */
    String getContent();

    /**
     * Gets report sections.
     *
     * @return list of report sections
     */
    java.util.List<ReportSection> getSections();

    /**
     * Exports the report.
     *
     * @param format export format
     * @return exported report data
     */
    byte[] export(ExportFormat format);
  }

  /** Validation recommendation interface. */
  interface ValidationRecommendation {
    /**
     * Gets the recommendation ID.
     *
     * @return recommendation ID
     */
    String getId();

    /**
     * Gets the recommendation title.
     *
     * @return title
     */
    String getTitle();

    /**
     * Gets the recommendation description.
     *
     * @return description
     */
    String getDescription();

    /**
     * Gets the recommendation priority.
     *
     * @return priority level
     */
    RecommendationPriority getPriority();

    /**
     * Gets the estimated impact.
     *
     * @return impact level
     */
    ImpactLevel getEstimatedImpact();

    /**
     * Gets implementation steps.
     *
     * @return list of implementation steps
     */
    java.util.List<String> getImplementationSteps();

    /**
     * Gets related errors or warnings.
     *
     * @return list of related issue IDs
     */
    java.util.List<String> getRelatedIssues();
  }

  /** Environment information interface. */
  interface EnvironmentInfo {
    /**
     * Gets the Java version.
     *
     * @return Java version
     */
    String getJavaVersion();

    /**
     * Gets the platform information.
     *
     * @return platform info
     */
    String getPlatform();

    /**
     * Gets the Wasmtime version.
     *
     * @return Wasmtime version
     */
    String getWasmtimeVersion();

    /**
     * Gets system properties.
     *
     * @return system properties
     */
    java.util.Map<String, String> getSystemProperties();
  }

  /** Validation performance metrics interface. */
  interface ValidationPerformanceMetrics {
    /**
     * Gets total validation time.
     *
     * @return time in milliseconds
     */
    long getTotalTime();

    /**
     * Gets average check time.
     *
     * @return average time in milliseconds
     */
    double getAverageCheckTime();

    /**
     * Gets memory usage during validation.
     *
     * @return memory usage in bytes
     */
    long getMemoryUsage();

    /**
     * Gets time breakdown by check type.
     *
     * @return time breakdown map
     */
    java.util.Map<String, Long> getTimeBreakdown();
  }

  /** Report section interface. */
  interface ReportSection {
    /**
     * Gets the section title.
     *
     * @return section title
     */
    String getTitle();

    /**
     * Gets the section content.
     *
     * @return section content
     */
    String getContent();

    /**
     * Gets the section level.
     *
     * @return section level
     */
    int getLevel();

    /**
     * Gets subsections.
     *
     * @return list of subsections
     */
    java.util.List<ReportSection> getSubsections();
  }

  /** Error severity enumeration. */
  enum ErrorSeverity {
    /** Critical error. */
    CRITICAL,
    /** High severity. */
    HIGH,
    /** Medium severity. */
    MEDIUM,
    /** Low severity. */
    LOW
  }

  /** Error category enumeration. */
  enum ErrorCategory {
    /** Configuration error. */
    CONFIGURATION,
    /** Resource error. */
    RESOURCE,
    /** Security error. */
    SECURITY,
    /** Performance error. */
    PERFORMANCE,
    /** Compatibility error. */
    COMPATIBILITY,
    /** Logic error. */
    LOGIC
  }

  /** Warning category enumeration. */
  enum WarningCategory {
    /** Performance warning. */
    PERFORMANCE,
    /** Security warning. */
    SECURITY,
    /** Compatibility warning. */
    COMPATIBILITY,
    /** Best practice warning. */
    BEST_PRACTICE,
    /** Deprecation warning. */
    DEPRECATION
  }

  /** Validation type enumeration. */
  enum ValidationType {
    /** Full validation. */
    FULL,
    /** Quick validation. */
    QUICK,
    /** Security validation. */
    SECURITY,
    /** Performance validation. */
    PERFORMANCE,
    /** Configuration validation. */
    CONFIGURATION
  }

  /** Validation scope enumeration. */
  enum ValidationScope {
    /** Controller only. */
    CONTROLLER,
    /** Configuration only. */
    CONFIGURATION,
    /** Resources only. */
    RESOURCES,
    /** All components. */
    ALL
  }

  /** Report format enumeration. */
  enum ReportFormat {
    /** Plain text. */
    TEXT,
    /** HTML format. */
    HTML,
    /** JSON format. */
    JSON,
    /** XML format. */
    XML
  }

  /** Export format enumeration. */
  enum ExportFormat {
    /** PDF format. */
    PDF,
    /** HTML format. */
    HTML,
    /** JSON format. */
    JSON,
    /** CSV format. */
    CSV
  }

  /** Recommendation priority enumeration. */
  enum RecommendationPriority {
    /** Low priority. */
    LOW,
    /** Medium priority. */
    MEDIUM,
    /** High priority. */
    HIGH,
    /** Critical priority. */
    CRITICAL
  }

  /** Impact level enumeration. */
  enum ImpactLevel {
    /** Minimal impact. */
    MINIMAL,
    /** Low impact. */
    LOW,
    /** Medium impact. */
    MEDIUM,
    /** High impact. */
    HIGH,
    /** Significant impact. */
    SIGNIFICANT
  }
}

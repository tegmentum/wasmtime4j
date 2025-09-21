package ai.tegmentum.wasmtime4j.diagnostics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive diagnostic report containing results from various diagnostic tests.
 *
 * <p>This class aggregates diagnostic information from environment checks, runtime validation,
 * performance tests, and error scenario testing to provide a complete picture of the
 * WebAssembly system health and capability.
 *
 * @since 1.0.0
 */
public final class DiagnosticReport {

  private Instant startTime;
  private Instant endTime;

  // Environment and system information
  private Map<String, String> environmentInfo;
  private Map<String, Object> runtimeInfo;
  private Map<String, Object> memoryInfo;

  // Test results
  private boolean moduleValidationResult;
  private boolean errorHandlingResult;
  private boolean performanceResult;

  // Errors and warnings encountered during diagnostics
  private final List<DiagnosticError> errors = new ArrayList<>();
  private final List<DiagnosticWarning> warnings = new ArrayList<>();

  /**
   * Gets the diagnostic execution start time.
   *
   * @return the start time
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Sets the diagnostic execution start time.
   *
   * @param startTime the start time
   */
  public void setStartTime(final Instant startTime) {
    this.startTime = startTime;
  }

  /**
   * Gets the diagnostic execution end time.
   *
   * @return the end time
   */
  public Instant getEndTime() {
    return endTime;
  }

  /**
   * Sets the diagnostic execution end time.
   *
   * @param endTime the end time
   */
  public void setEndTime(final Instant endTime) {
    this.endTime = endTime;
  }

  /**
   * Gets the total diagnostic execution duration.
   *
   * @return the execution duration, or null if start or end time not set
   */
  public Duration getExecutionDuration() {
    if (startTime == null || endTime == null) {
      return null;
    }
    return Duration.between(startTime, endTime);
  }

  /**
   * Gets environment information.
   *
   * @return the environment information map
   */
  public Map<String, String> getEnvironmentInfo() {
    return environmentInfo;
  }

  /**
   * Sets environment information.
   *
   * @param environmentInfo the environment information map
   */
  public void setEnvironmentInfo(final Map<String, String> environmentInfo) {
    this.environmentInfo = environmentInfo;
  }

  /**
   * Gets runtime information.
   *
   * @return the runtime information map
   */
  public Map<String, Object> getRuntimeInfo() {
    return runtimeInfo;
  }

  /**
   * Sets runtime information.
   *
   * @param runtimeInfo the runtime information map
   */
  public void setRuntimeInfo(final Map<String, Object> runtimeInfo) {
    this.runtimeInfo = runtimeInfo;
  }

  /**
   * Gets memory information.
   *
   * @return the memory information map
   */
  public Map<String, Object> getMemoryInfo() {
    return memoryInfo;
  }

  /**
   * Sets memory information.
   *
   * @param memoryInfo the memory information map
   */
  public void setMemoryInfo(final Map<String, Object> memoryInfo) {
    this.memoryInfo = memoryInfo;
  }

  /**
   * Gets the module validation test result.
   *
   * @return true if module validation test passed
   */
  public boolean isModuleValidationResult() {
    return moduleValidationResult;
  }

  /**
   * Sets the module validation test result.
   *
   * @param moduleValidationResult true if module validation test passed
   */
  public void setModuleValidationResult(final boolean moduleValidationResult) {
    this.moduleValidationResult = moduleValidationResult;
  }

  /**
   * Gets the error handling test result.
   *
   * @return true if error handling test passed
   */
  public boolean isErrorHandlingResult() {
    return errorHandlingResult;
  }

  /**
   * Sets the error handling test result.
   *
   * @param errorHandlingResult true if error handling test passed
   */
  public void setErrorHandlingResult(final boolean errorHandlingResult) {
    this.errorHandlingResult = errorHandlingResult;
  }

  /**
   * Gets the performance test result.
   *
   * @return true if performance test passed
   */
  public boolean isPerformanceResult() {
    return performanceResult;
  }

  /**
   * Sets the performance test result.
   *
   * @param performanceResult true if performance test passed
   */
  public void setPerformanceResult(final boolean performanceResult) {
    this.performanceResult = performanceResult;
  }

  /**
   * Adds an error encountered during diagnostics.
   *
   * @param message the error message
   * @param throwable the associated throwable (may be null)
   */
  public void addError(final String message, final Throwable throwable) {
    errors.add(new DiagnosticError(message, throwable));
  }

  /**
   * Adds a warning encountered during diagnostics.
   *
   * @param message the warning message
   */
  public void addWarning(final String message) {
    warnings.add(new DiagnosticWarning(message));
  }

  /**
   * Gets all errors encountered during diagnostics.
   *
   * @return the list of diagnostic errors
   */
  public List<DiagnosticError> getErrors() {
    return new ArrayList<>(errors);
  }

  /**
   * Gets all warnings encountered during diagnostics.
   *
   * @return the list of diagnostic warnings
   */
  public List<DiagnosticWarning> getWarnings() {
    return new ArrayList<>(warnings);
  }

  /**
   * Checks if any errors were encountered during diagnostics.
   *
   * @return true if errors were encountered
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Checks if any warnings were encountered during diagnostics.
   *
   * @return true if warnings were encountered
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Checks if all diagnostic tests passed.
   *
   * @return true if all tests passed and no errors occurred
   */
  public boolean isHealthy() {
    return !hasErrors() && moduleValidationResult && errorHandlingResult && performanceResult;
  }

  /**
   * Gets an overall health status summary.
   *
   * @return health status string
   */
  public String getHealthStatus() {
    if (isHealthy()) {
      return "HEALTHY";
    } else if (hasErrors()) {
      return "ERROR";
    } else {
      return "WARNING";
    }
  }

  /**
   * Generates a formatted diagnostic report.
   *
   * @return a formatted string containing the full diagnostic report
   */
  public String getFormattedReport() {
    final StringBuilder report = new StringBuilder();

    // Header
    report.append("=== WebAssembly Diagnostic Report ===\n");
    if (startTime != null) {
      report.append("Start Time: ").append(startTime).append("\n");
    }
    if (endTime != null) {
      report.append("End Time: ").append(endTime).append("\n");
    }
    if (getExecutionDuration() != null) {
      report.append("Duration: ").append(getExecutionDuration().toMillis()).append(" ms\n");
    }
    report.append("Overall Status: ").append(getHealthStatus()).append("\n\n");

    // Environment Information
    report.append("--- Environment Information ---\n");
    if (environmentInfo != null) {
      environmentInfo.forEach((key, value) ->
          report.append(String.format("%-20s: %s\n", key, value)));
    }
    report.append("\n");

    // Runtime Information
    report.append("--- Runtime Information ---\n");
    if (runtimeInfo != null) {
      runtimeInfo.forEach((key, value) ->
          report.append(String.format("%-20s: %s\n", key, value)));
    }
    report.append("\n");

    // Memory Information
    report.append("--- Memory Information ---\n");
    if (memoryInfo != null) {
      memoryInfo.forEach((key, value) -> {
        if (value instanceof Number) {
          final long bytes = ((Number) value).longValue();
          report.append(String.format("%-20s: %s (%d bytes)\n", key, formatMemory(bytes), bytes));
        } else {
          report.append(String.format("%-20s: %s\n", key, value));
        }
      });
    }
    report.append("\n");

    // Test Results
    report.append("--- Test Results ---\n");
    report.append(String.format("Module Validation : %s\n", formatTestResult(moduleValidationResult)));
    report.append(String.format("Error Handling    : %s\n", formatTestResult(errorHandlingResult)));
    report.append(String.format("Performance       : %s\n", formatTestResult(performanceResult)));
    report.append("\n");

    // Warnings
    if (hasWarnings()) {
      report.append("--- Warnings ---\n");
      for (final DiagnosticWarning warning : warnings) {
        report.append("⚠ ").append(warning.getMessage()).append("\n");
      }
      report.append("\n");
    }

    // Errors
    if (hasErrors()) {
      report.append("--- Errors ---\n");
      for (final DiagnosticError error : errors) {
        report.append("✗ ").append(error.getMessage()).append("\n");
        if (error.getThrowable() != null) {
          report.append("  Exception: ").append(error.getThrowable().getClass().getSimpleName())
              .append(": ").append(error.getThrowable().getMessage()).append("\n");
        }
      }
      report.append("\n");
    }

    // Recommendations
    report.append("--- Recommendations ---\n");
    appendRecommendations(report);

    return report.toString();
  }

  /**
   * Generates a compact summary of the diagnostic results.
   *
   * @return a compact summary string
   */
  public String getCompactSummary() {
    final String status = getHealthStatus();
    final int errorCount = errors.size();
    final int warningCount = warnings.size();

    if (errorCount == 0 && warningCount == 0) {
      return String.format("Status: %s - All tests passed", status);
    } else {
      return String.format("Status: %s - %d errors, %d warnings", status, errorCount, warningCount);
    }
  }

  private String formatTestResult(final boolean passed) {
    return passed ? "✓ PASS" : "✗ FAIL";
  }

  private String formatMemory(final long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    } else if (bytes < 1024 * 1024) {
      return String.format("%.1f KB", bytes / 1024.0);
    } else if (bytes < 1024 * 1024 * 1024) {
      return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    } else {
      return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
  }

  private void appendRecommendations(final StringBuilder report) {
    if (isHealthy()) {
      report.append("✓ System appears healthy - no immediate action required\n");
      report.append("• Consider enabling performance monitoring for production use\n");
      report.append("• Review error handling configuration for optimal settings\n");
    } else {
      if (hasErrors()) {
        report.append("✗ Critical issues detected - immediate action required\n");
        report.append("• Review error details above and address root causes\n");
        report.append("• Check native library installation and compatibility\n");
        report.append("• Verify Java version and WebAssembly module format\n");
      }

      if (!moduleValidationResult) {
        report.append("• Module validation failed - check WebAssembly module format\n");
        report.append("• Ensure modules have correct magic number and version\n");
      }

      if (!errorHandlingResult) {
        report.append("• Error handling test failed - check exception handling configuration\n");
        report.append("• Review error logging configuration and levels\n");
      }

      if (!performanceResult) {
        report.append("• Performance test failed - check performance monitoring setup\n");
        report.append("• Review diagnostic configuration and memory settings\n");
      }

      if (hasWarnings()) {
        report.append("• Address warnings to improve system reliability\n");
        report.append("• Review diagnostic configuration for optimal settings\n");
      }
    }
  }

  /**
   * Represents an error encountered during diagnostics.
   */
  public static final class DiagnosticError {
    private final String message;
    private final Throwable throwable;

    DiagnosticError(final String message, final Throwable throwable) {
      this.message = message;
      this.throwable = throwable;
    }

    public String getMessage() {
      return message;
    }

    public Throwable getThrowable() {
      return throwable;
    }
  }

  /**
   * Represents a warning encountered during diagnostics.
   */
  public static final class DiagnosticWarning {
    private final String message;

    DiagnosticWarning(final String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }
}
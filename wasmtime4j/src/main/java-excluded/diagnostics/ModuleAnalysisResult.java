package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.ArrayList;
import java.util.List;

/**
 * Results from WebAssembly module analysis and validation.
 *
 * <p>This class contains detailed information about the analysis of a WebAssembly module,
 * including format validation, compilation results, performance metrics, and any errors
 * or warnings encountered during the analysis process.
 *
 * @since 1.0.0
 */
public final class ModuleAnalysisResult {

  // Format validation
  private boolean validFormat;

  // Compilation results
  private boolean compilationSuccessful;
  private String compilationError;
  private long compilationTime = -1;

  // Module characteristics
  private long moduleSize;

  // Analysis errors and warnings
  private final List<String> errors = new ArrayList<>();
  private final List<String> warnings = new ArrayList<>();

  /**
   * Checks if the module has a valid WebAssembly format.
   *
   * @return true if the format is valid
   */
  public boolean isValidFormat() {
    return validFormat;
  }

  /**
   * Sets whether the module has a valid WebAssembly format.
   *
   * @param validFormat true if the format is valid
   */
  public void setValidFormat(final boolean validFormat) {
    this.validFormat = validFormat;
  }

  /**
   * Checks if module compilation was successful.
   *
   * @return true if compilation was successful
   */
  public boolean isCompilationSuccessful() {
    return compilationSuccessful;
  }

  /**
   * Sets whether module compilation was successful.
   *
   * @param compilationSuccessful true if compilation was successful
   */
  public void setCompilationSuccessful(final boolean compilationSuccessful) {
    this.compilationSuccessful = compilationSuccessful;
  }

  /**
   * Gets the compilation error message, if any.
   *
   * @return the compilation error message, or null if compilation was successful
   */
  public String getCompilationError() {
    return compilationError;
  }

  /**
   * Sets the compilation error message.
   *
   * @param compilationError the compilation error message
   */
  public void setCompilationError(final String compilationError) {
    this.compilationError = compilationError;
  }

  /**
   * Gets the compilation time in milliseconds.
   *
   * @return the compilation time, or -1 if not measured
   */
  public long getCompilationTime() {
    return compilationTime;
  }

  /**
   * Sets the compilation time in milliseconds.
   *
   * @param compilationTime the compilation time
   */
  public void setCompilationTime(final long compilationTime) {
    this.compilationTime = compilationTime;
  }

  /**
   * Gets the module size in bytes.
   *
   * @return the module size
   */
  public long getModuleSize() {
    return moduleSize;
  }

  /**
   * Sets the module size in bytes.
   *
   * @param moduleSize the module size
   */
  public void setModuleSize(final long moduleSize) {
    this.moduleSize = moduleSize;
  }

  /**
   * Adds an error encountered during analysis.
   *
   * @param message the error message
   * @param throwable the associated throwable (may be null)
   */
  public void addError(final String message, final Throwable throwable) {
    if (throwable != null) {
      errors.add(message + ": " + throwable.getMessage());
    } else {
      errors.add(message);
    }
  }

  /**
   * Adds a warning encountered during analysis.
   *
   * @param message the warning message
   */
  public void addWarning(final String message) {
    warnings.add(message);
  }

  /**
   * Gets all errors encountered during analysis.
   *
   * @return the list of error messages
   */
  public List<String> getErrors() {
    return new ArrayList<>(errors);
  }

  /**
   * Gets all warnings encountered during analysis.
   *
   * @return the list of warning messages
   */
  public List<String> getWarnings() {
    return new ArrayList<>(warnings);
  }

  /**
   * Checks if any errors were encountered.
   *
   * @return true if errors were encountered
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Checks if any warnings were encountered.
   *
   * @return true if warnings were encountered
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Checks if the overall analysis was successful.
   *
   * @return true if the module is valid and compilable without errors
   */
  public boolean isSuccessful() {
    return validFormat && compilationSuccessful && !hasErrors();
  }

  /**
   * Gets a summary status of the analysis.
   *
   * @return status string ("SUCCESS", "ERROR", or "WARNING")
   */
  public String getStatus() {
    if (hasErrors() || !compilationSuccessful) {
      return "ERROR";
    } else if (hasWarnings()) {
      return "WARNING";
    } else {
      return "SUCCESS";
    }
  }

  /**
   * Generates a formatted analysis report.
   *
   * @return a formatted string containing the analysis results
   */
  public String getFormattedReport() {
    final StringBuilder report = new StringBuilder();

    report.append("=== WebAssembly Module Analysis ===\n");
    report.append("Status: ").append(getStatus()).append("\n");
    report.append("Module Size: ").append(formatSize(moduleSize)).append("\n");
    report.append("Valid Format: ").append(validFormat ? "✓" : "✗").append("\n");

    if (validFormat) {
      report.append("Compilation: ").append(compilationSuccessful ? "✓" : "✗").append("\n");
      if (compilationTime >= 0) {
        report.append("Compilation Time: ").append(compilationTime).append(" ms\n");
      }
      if (!compilationSuccessful && compilationError != null) {
        report.append("Compilation Error: ").append(compilationError).append("\n");
      }
    }

    if (hasWarnings()) {
      report.append("\n--- Warnings ---\n");
      for (final String warning : warnings) {
        report.append("⚠ ").append(warning).append("\n");
      }
    }

    if (hasErrors()) {
      report.append("\n--- Errors ---\n");
      for (final String error : errors) {
        report.append("✗ ").append(error).append("\n");
      }
    }

    // Analysis summary
    report.append("\n--- Analysis Summary ---\n");
    if (isSuccessful()) {
      report.append("✓ Module is valid and ready for use\n");
      if (compilationTime >= 0) {
        if (compilationTime < 100) {
          report.append("✓ Fast compilation time\n");
        } else if (compilationTime < 1000) {
          report.append("⚠ Moderate compilation time\n");
        } else {
          report.append("⚠ Slow compilation time - consider optimization\n");
        }
      }
    } else {
      report.append("✗ Module has issues that need to be addressed\n");
      if (!validFormat) {
        report.append("• Fix WebAssembly format issues\n");
      }
      if (!compilationSuccessful) {
        report.append("• Fix compilation errors\n");
      }
    }

    return report.toString();
  }

  /**
   * Generates a compact summary of the analysis results.
   *
   * @return a compact summary string
   */
  public String getCompactSummary() {
    final StringBuilder summary = new StringBuilder();
    summary.append(getStatus()).append(": ");

    if (isSuccessful()) {
      summary.append("Valid module, ").append(formatSize(moduleSize));
      if (compilationTime >= 0) {
        summary.append(", ").append(compilationTime).append("ms compilation");
      }
    } else {
      if (!validFormat) {
        summary.append("Invalid format");
      } else if (!compilationSuccessful) {
        summary.append("Compilation failed");
      }
      if (hasErrors()) {
        summary.append(", ").append(errors.size()).append(" errors");
      }
      if (hasWarnings()) {
        summary.append(", ").append(warnings.size()).append(" warnings");
      }
    }

    return summary.toString();
  }

  private String formatSize(final long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    } else if (bytes < 1024 * 1024) {
      return String.format("%.1f KB", bytes / 1024.0);
    } else {
      return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
  }
}

/**
 * Results from error recovery testing.
 */
final class ErrorRecoveryTestResult {
  private boolean compilationErrorRecovery;
  private boolean runtimeErrorRecovery;
  private boolean resourceErrorRecovery;

  public boolean isCompilationErrorRecovery() {
    return compilationErrorRecovery;
  }

  public void setCompilationErrorRecovery(final boolean compilationErrorRecovery) {
    this.compilationErrorRecovery = compilationErrorRecovery;
  }

  public boolean isRuntimeErrorRecovery() {
    return runtimeErrorRecovery;
  }

  public void setRuntimeErrorRecovery(final boolean runtimeErrorRecovery) {
    this.runtimeErrorRecovery = runtimeErrorRecovery;
  }

  public boolean isResourceErrorRecovery() {
    return resourceErrorRecovery;
  }

  public void setResourceErrorRecovery(final boolean resourceErrorRecovery) {
    this.resourceErrorRecovery = resourceErrorRecovery;
  }

  public boolean allRecoveryTestsPassed() {
    return compilationErrorRecovery && runtimeErrorRecovery && resourceErrorRecovery;
  }
}

/**
 * Results from health check testing.
 */
final class HealthCheckResult {
  private boolean runtimeDetected;
  private Object detectedRuntime;
  private boolean engineCreation;
  private boolean storeCreation;
  private boolean basicCompilation;
  private final List<String> errors = new ArrayList<>();

  public boolean isRuntimeDetected() {
    return runtimeDetected;
  }

  public void setRuntimeDetected(final boolean runtimeDetected) {
    this.runtimeDetected = runtimeDetected;
  }

  public Object getDetectedRuntime() {
    return detectedRuntime;
  }

  public void setDetectedRuntime(final Object detectedRuntime) {
    this.detectedRuntime = detectedRuntime;
  }

  public boolean isEngineCreation() {
    return engineCreation;
  }

  public void setEngineCreation(final boolean engineCreation) {
    this.engineCreation = engineCreation;
  }

  public boolean isStoreCreation() {
    return storeCreation;
  }

  public void setStoreCreation(final boolean storeCreation) {
    this.storeCreation = storeCreation;
  }

  public boolean isBasicCompilation() {
    return basicCompilation;
  }

  public void setBasicCompilation(final boolean basicCompilation) {
    this.basicCompilation = basicCompilation;
  }

  public void addError(final String message, final Throwable throwable) {
    if (throwable != null) {
      errors.add(message + ": " + throwable.getMessage());
    } else {
      errors.add(message);
    }
  }

  public List<String> getErrors() {
    return new ArrayList<>(errors);
  }

  public boolean isHealthy() {
    return runtimeDetected && engineCreation && storeCreation && basicCompilation && errors.isEmpty();
  }
}

/**
 * Results from error scenario reproduction.
 */
final class ErrorReproductionResult {
  private DiagnosticTool.ErrorScenario scenario;
  private java.time.Instant startTime;
  private java.time.Instant endTime;
  private boolean expectedError;
  private boolean actualError;
  private String errorMessage;
  private final List<String> errors = new ArrayList<>();

  public DiagnosticTool.ErrorScenario getScenario() {
    return scenario;
  }

  public void setScenario(final DiagnosticTool.ErrorScenario scenario) {
    this.scenario = scenario;
  }

  public java.time.Instant getStartTime() {
    return startTime;
  }

  public void setStartTime(final java.time.Instant startTime) {
    this.startTime = startTime;
  }

  public java.time.Instant getEndTime() {
    return endTime;
  }

  public void setEndTime(final java.time.Instant endTime) {
    this.endTime = endTime;
  }

  public boolean isExpectedError() {
    return expectedError;
  }

  public void setExpectedError(final boolean expectedError) {
    this.expectedError = expectedError;
  }

  public boolean isActualError() {
    return actualError;
  }

  public void setActualError(final boolean actualError) {
    this.actualError = actualError;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(final String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public void addError(final String message, final Throwable throwable) {
    if (throwable != null) {
      errors.add(message + ": " + throwable.getMessage());
    } else {
      errors.add(message);
    }
  }

  public List<String> getErrors() {
    return new ArrayList<>(errors);
  }

  public boolean isSuccessful() {
    return expectedError == actualError && errors.isEmpty();
  }
}
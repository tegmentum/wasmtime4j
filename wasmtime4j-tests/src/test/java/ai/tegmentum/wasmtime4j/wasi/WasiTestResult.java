package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestResult;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a WASI integration test execution. Extends cross-runtime testing with WASI-specific
 * validation and reporting.
 */
public final class WasiTestResult {
  private final String testName;
  private final boolean wasiCompliant;
  private final CrossRuntimeTestResult crossRuntimeResult;
  private final Optional<Path> testEnvironmentPath;
  private final List<String> errors;
  private final List<String> warnings;
  private final List<String> info;
  private final Instant executionTime;

  private WasiTestResult(final Builder builder) {
    this.testName = Objects.requireNonNull(builder.testName, "testName cannot be null");
    this.wasiCompliant = builder.wasiCompliant;
    this.crossRuntimeResult = builder.crossRuntimeResult;
    this.testEnvironmentPath = Optional.ofNullable(builder.testEnvironmentPath);
    this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
    this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
    this.info = Collections.unmodifiableList(new ArrayList<>(builder.info));
    this.executionTime = builder.executionTime;
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
   * Checks if the test demonstrates WASI compliance.
   *
   * @return true if WASI compliant
   */
  public boolean isWasiCompliant() {
    return wasiCompliant;
  }

  /**
   * Gets the underlying cross-runtime test result.
   *
   * @return the cross-runtime result if available
   */
  public Optional<CrossRuntimeTestResult> getCrossRuntimeResult() {
    return Optional.ofNullable(crossRuntimeResult);
  }

  /**
   * Gets the test environment path used for the test.
   *
   * @return the test environment path if available
   */
  public Optional<Path> getTestEnvironmentPath() {
    return testEnvironmentPath;
  }

  /**
   * Gets the WASI test errors.
   *
   * @return the list of errors
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Gets the WASI test warnings.
   *
   * @return the list of warnings
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * Gets the WASI test informational messages.
   *
   * @return the list of info messages
   */
  public List<String> getInfo() {
    return info;
  }

  /**
   * Gets the execution time.
   *
   * @return the execution time
   */
  public Instant getExecutionTime() {
    return executionTime;
  }

  /**
   * Checks if there are errors.
   *
   * @return true if there are errors
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Checks if there are warnings.
   *
   * @return true if there are warnings
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Checks if the test was successful overall.
   *
   * @return true if successful
   */
  public boolean isSuccessful() {
    return wasiCompliant
        && !hasErrors()
        && crossRuntimeResult != null
        && crossRuntimeResult.bothSuccessful();
  }

  /**
   * Gets a summary of the WASI test result.
   *
   * @return the test summary
   */
  public String getSummary() {
    final StringBuilder sb = new StringBuilder();
    sb.append("WASI Test: ").append(testName).append(" - ");

    if (isSuccessful()) {
      sb.append("SUCCESS");
    } else if (hasErrors()) {
      sb.append("FAILED");
    } else {
      sb.append("PARTIAL");
    }

    sb.append(" (WASI Compliant: ").append(wasiCompliant).append(")");

    if (hasErrors()) {
      sb.append(" [").append(errors.size()).append(" errors]");
    }
    if (hasWarnings()) {
      sb.append(" [").append(warnings.size()).append(" warnings]");
    }

    return sb.toString();
  }

  /**
   * Gets a detailed report of the WASI test execution.
   *
   * @return the detailed report
   */
  public String getDetailedReport() {
    final StringBuilder sb = new StringBuilder();
    sb.append("WASI Integration Test Result: ").append(testName).append('\n');
    sb.append("=====================================\n");
    sb.append("Execution Time: ").append(executionTime).append('\n');
    sb.append("WASI Compliant: ").append(wasiCompliant).append('\n');
    sb.append("Overall Success: ").append(isSuccessful()).append('\n');

    if (testEnvironmentPath.isPresent()) {
      sb.append("Test Environment: ").append(testEnvironmentPath.get()).append('\n');
    }

    sb.append('\n');

    // Cross-runtime results
    if (crossRuntimeResult != null) {
      sb.append("Cross-Runtime Results:\n");
      sb.append(crossRuntimeResult.getDetailedReport());
      sb.append('\n');
    }

    // WASI-specific results
    if (!errors.isEmpty()) {
      sb.append("WASI Errors:\n");
      for (final String error : errors) {
        sb.append("  - ").append(error).append('\n');
      }
      sb.append('\n');
    }

    if (!warnings.isEmpty()) {
      sb.append("WASI Warnings:\n");
      for (final String warning : warnings) {
        sb.append("  - ").append(warning).append('\n');
      }
      sb.append('\n');
    }

    if (!info.isEmpty()) {
      sb.append("WASI Information:\n");
      for (final String infoItem : info) {
        sb.append("  - ").append(infoItem).append('\n');
      }
      sb.append('\n');
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

    final WasiTestResult that = (WasiTestResult) obj;
    return wasiCompliant == that.wasiCompliant
        && Objects.equals(testName, that.testName)
        && Objects.equals(crossRuntimeResult, that.crossRuntimeResult)
        && Objects.equals(testEnvironmentPath, that.testEnvironmentPath)
        && Objects.equals(errors, that.errors)
        && Objects.equals(warnings, that.warnings)
        && Objects.equals(info, that.info)
        && Objects.equals(executionTime, that.executionTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        testName,
        wasiCompliant,
        crossRuntimeResult,
        testEnvironmentPath,
        errors,
        warnings,
        info,
        executionTime);
  }

  @Override
  public String toString() {
    return "WasiTestResult{"
        + "testName='"
        + testName
        + '\''
        + ", wasiCompliant="
        + wasiCompliant
        + ", successful="
        + isSuccessful()
        + ", errorCount="
        + errors.size()
        + ", warningCount="
        + warnings.size()
        + ", executionTime="
        + executionTime
        + '}';
  }

  /** Builder for creating WASI test results. */
  public static final class Builder {
    private final String testName;
    private boolean wasiCompliant = false;
    private CrossRuntimeTestResult crossRuntimeResult;
    private Path testEnvironmentPath;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> info = new ArrayList<>();
    private final Instant executionTime;

    /**
     * Creates a new builder.
     *
     * @param testName the test name
     */
    public Builder(final String testName) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
      this.executionTime = Instant.now();
    }

    /**
     * Sets whether the test is WASI compliant.
     *
     * @param wasiCompliant true if WASI compliant
     * @return this builder
     */
    public Builder wasiCompliant(final boolean wasiCompliant) {
      this.wasiCompliant = wasiCompliant;
      return this;
    }

    /**
     * Sets the cross-runtime test result.
     *
     * @param crossRuntimeResult the cross-runtime result
     * @return this builder
     */
    public Builder crossRuntimeResult(final CrossRuntimeTestResult crossRuntimeResult) {
      this.crossRuntimeResult = crossRuntimeResult;
      return this;
    }

    /**
     * Sets the test environment path.
     *
     * @param testEnvironmentPath the test environment path
     * @return this builder
     */
    public Builder testEnvironmentPath(final Path testEnvironmentPath) {
      this.testEnvironmentPath = testEnvironmentPath;
      return this;
    }

    /**
     * Adds an error.
     *
     * @param error the error message
     * @return this builder
     */
    public Builder addError(final String error) {
      this.errors.add(Objects.requireNonNull(error, "error cannot be null"));
      return this;
    }

    /**
     * Adds a warning.
     *
     * @param warning the warning message
     * @return this builder
     */
    public Builder addWarning(final String warning) {
      this.warnings.add(Objects.requireNonNull(warning, "warning cannot be null"));
      return this;
    }

    /**
     * Adds an informational message.
     *
     * @param info the info message
     * @return this builder
     */
    public Builder addInfo(final String info) {
      this.info.add(Objects.requireNonNull(info, "info cannot be null"));
      return this;
    }

    /**
     * Builds the WASI test result.
     *
     * @return the test result
     */
    public WasiTestResult build() {
      return new WasiTestResult(this);
    }
  }
}

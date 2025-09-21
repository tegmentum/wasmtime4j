package ai.tegmentum.wasmtime4j.comparison.runners;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Comprehensive results of cross-runtime validation execution, containing detailed analysis
 * of behavioral consistency, production readiness metrics, and overall validation outcomes.
 *
 * @since 1.0.0
 */
public final class CrossRuntimeValidationResults {
  private final List<TestSuiteValidationResult> suiteResults;
  private final OverallValidationResult overallResult;
  private final Duration executionDuration;
  private final Instant startTime;
  private final Instant endTime;
  private final boolean executionFailed;
  private final String failureReason;

  private CrossRuntimeValidationResults(final Builder builder) {
    this.suiteResults = List.copyOf(builder.suiteResults);
    this.overallResult = builder.overallResult;
    this.executionDuration = builder.executionDuration;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.executionFailed = builder.executionFailed;
    this.failureReason = builder.failureReason;
  }

  public List<TestSuiteValidationResult> getSuiteResults() {
    return suiteResults;
  }

  public OverallValidationResult getOverallResult() {
    return overallResult;
  }

  public Duration getExecutionDuration() {
    return executionDuration;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public boolean isExecutionFailed() {
    return executionFailed;
  }

  public String getFailureReason() {
    return failureReason;
  }

  /**
   * Checks if the validation was successful and meets production requirements.
   *
   * @return true if validation succeeded and meets requirements
   */
  public boolean isValidationSuccessful() {
    return !executionFailed && overallResult != null && overallResult.meetsProductionRequirements();
  }

  /**
   * Checks if zero discrepancy requirement was achieved.
   *
   * @return true if zero discrepancy was achieved
   */
  public boolean achievesZeroDiscrepancy() {
    return !executionFailed && overallResult != null && overallResult.achievesZeroDiscrepancy();
  }

  /**
   * Gets a summary report of the validation results.
   *
   * @return formatted summary report
   */
  public String getSummaryReport() {
    if (executionFailed) {
      return String.format("Validation FAILED: %s", failureReason);
    }

    if (overallResult == null) {
      return "Validation completed with no results";
    }

    return String.format(
        "Cross-Runtime Validation Summary:%n" +
        "Overall Consistency: %.2f%%%n" +
        "Total Tests: %d across %d suites%n" +
        "Production Ready: %d/%d tests (%.1f%%)%n" +
        "Zero Discrepancy: %s%n" +
        "Execution Time: %d seconds%n" +
        "Status: %s",
        overallResult.getOverallConsistencyScore() * 100,
        overallResult.getTotalTests(),
        overallResult.getTotalSuites(),
        overallResult.getProductionReadyTests(),
        overallResult.getTotalTests(),
        overallResult.getTotalTests() > 0 ?
            (double) overallResult.getProductionReadyTests() / overallResult.getTotalTests() * 100 : 0,
        overallResult.achievesZeroDiscrepancy() ? "ACHIEVED" : "NOT ACHIEVED",
        executionDuration.getSeconds(),
        isValidationSuccessful() ? "PASSED" : "FAILED");
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CrossRuntimeValidationResults that = (CrossRuntimeValidationResults) obj;
    return executionFailed == that.executionFailed
        && Objects.equals(suiteResults, that.suiteResults)
        && Objects.equals(overallResult, that.overallResult)
        && Objects.equals(executionDuration, that.executionDuration)
        && Objects.equals(startTime, that.startTime)
        && Objects.equals(endTime, that.endTime)
        && Objects.equals(failureReason, that.failureReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(suiteResults, overallResult, executionDuration, startTime, endTime, executionFailed, failureReason);
  }

  @Override
  public String toString() {
    return String.format(
        "CrossRuntimeValidationResults{suites=%d, successful=%s, duration=%ds}",
        suiteResults.size(), isValidationSuccessful(), executionDuration.getSeconds());
  }

  /** Builder for CrossRuntimeValidationResults. */
  public static final class Builder {
    private List<TestSuiteValidationResult> suiteResults = List.of();
    private OverallValidationResult overallResult;
    private Duration executionDuration = Duration.ZERO;
    private Instant startTime = Instant.now();
    private Instant endTime = Instant.now();
    private boolean executionFailed = false;
    private String failureReason;

    public Builder suiteResults(final List<TestSuiteValidationResult> suiteResults) {
      this.suiteResults = Objects.requireNonNull(suiteResults, "suiteResults cannot be null");
      return this;
    }

    public Builder overallResult(final OverallValidationResult overallResult) {
      this.overallResult = overallResult;
      return this;
    }

    public Builder executionDuration(final Duration executionDuration) {
      this.executionDuration = Objects.requireNonNull(executionDuration, "executionDuration cannot be null");
      return this;
    }

    public Builder startTime(final Instant startTime) {
      this.startTime = Objects.requireNonNull(startTime, "startTime cannot be null");
      return this;
    }

    public Builder endTime(final Instant endTime) {
      this.endTime = Objects.requireNonNull(endTime, "endTime cannot be null");
      return this;
    }

    public Builder executionFailed(final boolean executionFailed) {
      this.executionFailed = executionFailed;
      return this;
    }

    public Builder failureReason(final String failureReason) {
      this.failureReason = failureReason;
      return this;
    }

    public CrossRuntimeValidationResults build() {
      return new CrossRuntimeValidationResults(this);
    }
  }
}
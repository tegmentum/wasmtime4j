package ai.tegmentum.wasmtime4j.comparison.runners;

import java.util.List;
import java.util.Objects;

/**
 * Result of validating a single test suite across multiple WebAssembly runtime implementations,
 * containing individual test results and suite-level consistency analysis.
 *
 * @since 1.0.0
 */
public final class TestSuiteValidationResult {
  private final String suiteName;
  private final List<CrossRuntimeTestResult> testResults;
  private final SuiteConsistencyAnalysis suiteAnalysis;
  private final boolean executionSuccessful;
  private final String failureReason;

  private TestSuiteValidationResult(final Builder builder) {
    this.suiteName = Objects.requireNonNull(builder.suiteName, "suiteName cannot be null");
    this.testResults = List.copyOf(builder.testResults);
    this.suiteAnalysis = builder.suiteAnalysis;
    this.executionSuccessful = builder.executionSuccessful;
    this.failureReason = builder.failureReason;
  }

  public String getSuiteName() {
    return suiteName;
  }

  public List<CrossRuntimeTestResult> getTestResults() {
    return testResults;
  }

  public SuiteConsistencyAnalysis getSuiteAnalysis() {
    return suiteAnalysis;
  }

  public boolean isExecutionSuccessful() {
    return executionSuccessful;
  }

  public String getFailureReason() {
    return failureReason;
  }

  /**
   * Checks if the suite meets production requirements.
   *
   * @return true if suite meets production requirements
   */
  public boolean meetsProductionRequirements() {
    return executionSuccessful
        && suiteAnalysis != null
        && suiteAnalysis.getProductionReadinessRate() >= 0.98;
  }

  /**
   * Gets the average consistency score across all tests in the suite.
   *
   * @return the average consistency score
   */
  public double getAverageConsistencyScore() {
    return suiteAnalysis != null ? suiteAnalysis.getAverageConsistencyScore() : 0.0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final TestSuiteValidationResult that = (TestSuiteValidationResult) obj;
    return executionSuccessful == that.executionSuccessful
        && Objects.equals(suiteName, that.suiteName)
        && Objects.equals(testResults, that.testResults)
        && Objects.equals(suiteAnalysis, that.suiteAnalysis)
        && Objects.equals(failureReason, that.failureReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(suiteName, testResults, suiteAnalysis, executionSuccessful, failureReason);
  }

  @Override
  public String toString() {
    return String.format(
        "TestSuiteValidationResult{suiteName='%s', tests=%d, successful=%s, avgConsistency=%.2f}",
        suiteName, testResults.size(), executionSuccessful, getAverageConsistencyScore());
  }

  /** Builder for TestSuiteValidationResult. */
  public static final class Builder {
    private final String suiteName;
    private List<CrossRuntimeTestResult> testResults = List.of();
    private SuiteConsistencyAnalysis suiteAnalysis;
    private boolean executionSuccessful = false;
    private String failureReason;

    public Builder(final String suiteName) {
      this.suiteName = Objects.requireNonNull(suiteName, "suiteName cannot be null");
    }

    public Builder testResults(final List<CrossRuntimeTestResult> testResults) {
      this.testResults = Objects.requireNonNull(testResults, "testResults cannot be null");
      return this;
    }

    public Builder suiteAnalysis(final SuiteConsistencyAnalysis suiteAnalysis) {
      this.suiteAnalysis = suiteAnalysis;
      return this;
    }

    public Builder executionSuccessful(final boolean executionSuccessful) {
      this.executionSuccessful = executionSuccessful;
      return this;
    }

    public Builder failureReason(final String failureReason) {
      this.failureReason = failureReason;
      return this;
    }

    public TestSuiteValidationResult build() {
      return new TestSuiteValidationResult(this);
    }
  }
}

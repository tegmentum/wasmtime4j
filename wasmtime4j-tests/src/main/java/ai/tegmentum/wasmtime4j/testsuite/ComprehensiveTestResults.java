package ai.tegmentum.wasmtime4j.testsuite;

/** Comprehensive test results containing execution results and analysis reports. */
public final class ComprehensiveTestResults {

  private final TestExecutionResults executionResults;
  private final TestAnalysisReport analysisReport;
  private final long executionTimeMs;

  private ComprehensiveTestResults(final Builder builder) {
    this.executionResults = builder.executionResults;
    this.analysisReport = builder.analysisReport;
    this.executionTimeMs = builder.executionTimeMs;
  }

  // Getters
  public TestExecutionResults getExecutionResults() {
    return executionResults;
  }

  public TestAnalysisReport getAnalysisReport() {
    return analysisReport;
  }

  public long getExecutionTimeMs() {
    return executionTimeMs;
  }

  /**
   * Creates a new builder for ComprehensiveTestResults.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ComprehensiveTestResults. */
  public static final class Builder {
    private TestExecutionResults executionResults;
    private TestAnalysisReport analysisReport;
    private long executionTimeMs;

    public Builder executionResults(final TestExecutionResults executionResults) {
      this.executionResults = executionResults;
      return this;
    }

    public Builder analysisReport(final TestAnalysisReport analysisReport) {
      this.analysisReport = analysisReport;
      return this;
    }

    public Builder executionTimeMs(final long executionTimeMs) {
      this.executionTimeMs = executionTimeMs;
      return this;
    }

    public ComprehensiveTestResults build() {
      if (executionResults == null) {
        throw new IllegalStateException("Execution results must be set");
      }
      return new ComprehensiveTestResults(this);
    }
  }
}

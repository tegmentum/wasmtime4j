package ai.tegmentum.wasmtime4j.testsuite;

/**
 * Comprehensive analysis report containing single-runtime analysis results.
 *
 * <p>For cross-runtime comparison analysis, use CrossRuntimeAnalysis from the
 * wasmtime4j-comparison-tests module.
 */
public final class TestAnalysisReport {

  private final PerformanceAnalysis performanceAnalysis;
  private final CoverageAnalysis coverageAnalysis;
  private final TestInsights insights;

  private TestAnalysisReport(final Builder builder) {
    this.performanceAnalysis = builder.performanceAnalysis;
    this.coverageAnalysis = builder.coverageAnalysis;
    this.insights = builder.insights;
  }

  // Getters
  public PerformanceAnalysis getPerformanceAnalysis() {
    return performanceAnalysis;
  }

  public CoverageAnalysis getCoverageAnalysis() {
    return coverageAnalysis;
  }

  public TestInsights getInsights() {
    return insights;
  }

  /**
   * Creates a new builder for TestAnalysisReport.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for TestAnalysisReport. */
  public static final class Builder {
    private PerformanceAnalysis performanceAnalysis;
    private CoverageAnalysis coverageAnalysis;
    private TestInsights insights;

    public Builder performanceAnalysis(final PerformanceAnalysis performanceAnalysis) {
      this.performanceAnalysis = performanceAnalysis;
      return this;
    }

    public Builder coverageAnalysis(final CoverageAnalysis coverageAnalysis) {
      this.coverageAnalysis = coverageAnalysis;
      return this;
    }

    public Builder insights(final TestInsights insights) {
      this.insights = insights;
      return this;
    }

    public TestAnalysisReport build() {
      return new TestAnalysisReport(this);
    }
  }
}

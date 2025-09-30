package ai.tegmentum.wasmtime4j.testsuite;

/** Comprehensive analysis report containing all analysis results. */
public final class TestAnalysisReport {

  private final CrossRuntimeAnalysis crossRuntimeAnalysis;
  private final PerformanceAnalysis performanceAnalysis;
  private final CoverageAnalysis coverageAnalysis;
  private final TestInsights insights;

  private TestAnalysisReport(final Builder builder) {
    this.crossRuntimeAnalysis = builder.crossRuntimeAnalysis;
    this.performanceAnalysis = builder.performanceAnalysis;
    this.coverageAnalysis = builder.coverageAnalysis;
    this.insights = builder.insights;
  }

  // Getters
  public CrossRuntimeAnalysis getCrossRuntimeAnalysis() {
    return crossRuntimeAnalysis;
  }

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
    private CrossRuntimeAnalysis crossRuntimeAnalysis;
    private PerformanceAnalysis performanceAnalysis;
    private CoverageAnalysis coverageAnalysis;
    private TestInsights insights;

    public Builder crossRuntimeAnalysis(final CrossRuntimeAnalysis crossRuntimeAnalysis) {
      this.crossRuntimeAnalysis = crossRuntimeAnalysis;
      return this;
    }

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

package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Comprehensive result of insight analysis containing performance optimization insights,
 * runtime-specific recommendations, cross-cutting observations, and strategic guidance.
 *
 * @since 1.0.0
 */
public final class InsightAnalysisResult {
  private final String testName;
  private final List<PerformanceInsight> performanceInsights;
  private final Map<RuntimeType, RuntimeInsight> runtimeInsights;
  private final List<CrossCuttingInsight> crossCuttingInsights;
  private final List<StrategicInsight> strategicInsights;
  private final List<PatternInsight> patternInsights;
  private final List<TrendInsight> trendInsights;
  private final InsightConfidenceMetrics confidenceMetrics;
  private final Instant analysisTime;

  private InsightAnalysisResult(final Builder builder) {
    this.testName = Objects.requireNonNull(builder.testName, "testName cannot be null");
    this.performanceInsights = List.copyOf(builder.performanceInsights);
    this.runtimeInsights = Map.copyOf(builder.runtimeInsights);
    this.crossCuttingInsights = List.copyOf(builder.crossCuttingInsights);
    this.strategicInsights = List.copyOf(builder.strategicInsights);
    this.patternInsights = List.copyOf(builder.patternInsights);
    this.trendInsights = List.copyOf(builder.trendInsights);
    this.confidenceMetrics =
        Objects.requireNonNull(builder.confidenceMetrics, "confidenceMetrics cannot be null");
    this.analysisTime = Instant.now();
  }

  public String getTestName() {
    return testName;
  }

  public List<PerformanceInsight> getPerformanceInsights() {
    return performanceInsights;
  }

  public Map<RuntimeType, RuntimeInsight> getRuntimeInsights() {
    return runtimeInsights;
  }

  public List<CrossCuttingInsight> getCrossCuttingInsights() {
    return crossCuttingInsights;
  }

  public List<StrategicInsight> getStrategicInsights() {
    return strategicInsights;
  }

  public List<PatternInsight> getPatternInsights() {
    return patternInsights;
  }

  public List<TrendInsight> getTrendInsights() {
    return trendInsights;
  }

  public InsightConfidenceMetrics getConfidenceMetrics() {
    return confidenceMetrics;
  }

  public Instant getAnalysisTime() {
    return analysisTime;
  }

  /**
   * Gets the total number of insights generated.
   *
   * @return total insight count
   */
  public int getTotalInsightCount() {
    return performanceInsights.size()
        + runtimeInsights.size()
        + crossCuttingInsights.size()
        + strategicInsights.size()
        + patternInsights.size()
        + trendInsights.size();
  }

  /**
   * Gets high-severity insights requiring immediate attention.
   *
   * @return list of high-severity insights
   */
  public List<Object> getHighSeverityInsights() {
    final List<Object> highSeverityInsights = new java.util.ArrayList<>();

    performanceInsights.stream()
        .filter(insight -> insight.getSeverity() == InsightSeverity.HIGH)
        .forEach(highSeverityInsights::add);

    runtimeInsights.values().stream()
        .filter(insight -> insight.getSeverity() == InsightSeverity.HIGH)
        .forEach(highSeverityInsights::add);

    crossCuttingInsights.stream()
        .filter(insight -> insight.getSeverity() == InsightSeverity.HIGH)
        .forEach(highSeverityInsights::add);

    strategicInsights.stream()
        .filter(insight -> insight.getImportance() == StrategicImportance.HIGH)
        .forEach(highSeverityInsights::add);

    return highSeverityInsights;
  }

  /**
   * Generates an executive summary of all insights.
   *
   * @return formatted executive summary
   */
  public String getExecutiveSummary() {
    return String.format(
        "Insight Analysis Summary for %s%n"
            + "===================================%n"
            + "Analysis Time: %s%n"
            + "Overall Confidence: %.1f%%%n%n"
            + "Insights Generated:%n"
            + "  Performance: %d%n"
            + "  Runtime-Specific: %d%n"
            + "  Cross-Cutting: %d%n"
            + "  Strategic: %d%n"
            + "  Pattern: %d%n"
            + "  Trend: %d%n%n"
            + "High-Priority Items: %d%n"
            + "Total Insights: %d",
        testName,
        analysisTime,
        confidenceMetrics.getOverallConfidence() * 100,
        performanceInsights.size(),
        runtimeInsights.size(),
        crossCuttingInsights.size(),
        strategicInsights.size(),
        patternInsights.size(),
        trendInsights.size(),
        getHighSeverityInsights().size(),
        getTotalInsightCount());
  }

  /**
   * Gets all insights in a single collection.
   *
   * @return list of all insights
   */
  public List<Object> getInsights() {
    final List<Object> allInsights = new java.util.ArrayList<>();
    allInsights.addAll(performanceInsights);
    allInsights.addAll(runtimeInsights.values());
    allInsights.addAll(crossCuttingInsights);
    allInsights.addAll(strategicInsights);
    allInsights.addAll(patternInsights);
    allInsights.addAll(trendInsights);
    return allInsights;
  }

  /**
   * Gets the overall confidence score for the analysis.
   *
   * @return confidence score (0.0 to 1.0)
   */
  public double getConfidenceScore() {
    return confidenceMetrics.getOverallConfidence();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final InsightAnalysisResult that = (InsightAnalysisResult) obj;
    return Objects.equals(testName, that.testName)
        && Objects.equals(performanceInsights, that.performanceInsights)
        && Objects.equals(runtimeInsights, that.runtimeInsights)
        && Objects.equals(crossCuttingInsights, that.crossCuttingInsights)
        && Objects.equals(strategicInsights, that.strategicInsights)
        && Objects.equals(patternInsights, that.patternInsights)
        && Objects.equals(trendInsights, that.trendInsights)
        && Objects.equals(confidenceMetrics, that.confidenceMetrics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        testName,
        performanceInsights,
        runtimeInsights,
        crossCuttingInsights,
        strategicInsights,
        patternInsights,
        trendInsights,
        confidenceMetrics);
  }

  @Override
  public String toString() {
    return "InsightAnalysisResult{"
        + "testName='"
        + testName
        + '\''
        + ", totalInsights="
        + getTotalInsightCount()
        + ", confidence="
        + String.format("%.1f%%", confidenceMetrics.getOverallConfidence() * 100)
        + '}';
  }

  /** Builder for InsightAnalysisResult. */
  public static final class Builder {
    private final String testName;
    private List<PerformanceInsight> performanceInsights = Collections.emptyList();
    private Map<RuntimeType, RuntimeInsight> runtimeInsights = Collections.emptyMap();
    private List<CrossCuttingInsight> crossCuttingInsights = Collections.emptyList();
    private List<StrategicInsight> strategicInsights = Collections.emptyList();
    private List<PatternInsight> patternInsights = Collections.emptyList();
    private List<TrendInsight> trendInsights = Collections.emptyList();
    private InsightConfidenceMetrics confidenceMetrics;

    public Builder(final String testName) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    }

    public Builder performanceInsights(final List<PerformanceInsight> insights) {
      this.performanceInsights = Objects.requireNonNull(insights, "insights cannot be null");
      return this;
    }

    public Builder runtimeInsights(final Map<RuntimeType, RuntimeInsight> insights) {
      this.runtimeInsights = Objects.requireNonNull(insights, "insights cannot be null");
      return this;
    }

    public Builder crossCuttingInsights(final List<CrossCuttingInsight> insights) {
      this.crossCuttingInsights = Objects.requireNonNull(insights, "insights cannot be null");
      return this;
    }

    public Builder strategicInsights(final List<StrategicInsight> insights) {
      this.strategicInsights = Objects.requireNonNull(insights, "insights cannot be null");
      return this;
    }

    public Builder patternInsights(final List<PatternInsight> insights) {
      this.patternInsights = Objects.requireNonNull(insights, "insights cannot be null");
      return this;
    }

    public Builder trendInsights(final List<TrendInsight> insights) {
      this.trendInsights = Objects.requireNonNull(insights, "insights cannot be null");
      return this;
    }

    public Builder confidenceMetrics(final InsightConfidenceMetrics metrics) {
      this.confidenceMetrics = Objects.requireNonNull(metrics, "metrics cannot be null");
      return this;
    }

    /**
     * Builds the insight analysis result.
     *
     * @return new InsightAnalysisResult instance
     * @throws IllegalStateException if confidenceMetrics is not set
     */
    public InsightAnalysisResult build() {
      if (confidenceMetrics == null) {
        throw new IllegalStateException("confidenceMetrics must be set");
      }
      return new InsightAnalysisResult(this);
    }
  }
}

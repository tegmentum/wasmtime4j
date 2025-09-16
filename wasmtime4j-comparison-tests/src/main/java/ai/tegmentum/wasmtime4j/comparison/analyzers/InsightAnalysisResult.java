package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    this.confidenceMetrics = Objects.requireNonNull(builder.confidenceMetrics, "confidenceMetrics cannot be null");
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
    return performanceInsights.size() + runtimeInsights.size() + crossCuttingInsights.size() +
           strategicInsights.size() + patternInsights.size() + trendInsights.size();
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
        "Insight Analysis Summary for %s%n" +
        "===================================%n" +
        "Analysis Time: %s%n" +
        "Overall Confidence: %.1f%%%n%n" +
        "Insights Generated:%n" +
        "  Performance: %d%n" +
        "  Runtime-Specific: %d%n" +
        "  Cross-Cutting: %d%n" +
        "  Strategic: %d%n" +
        "  Pattern: %d%n" +
        "  Trend: %d%n%n" +
        "High-Priority Items: %d%n" +
        "Total Insights: %d",
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
        getTotalInsightCount()
    );
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
    return Objects.equals(testName, that.testName) &&
           Objects.equals(performanceInsights, that.performanceInsights) &&
           Objects.equals(runtimeInsights, that.runtimeInsights) &&
           Objects.equals(crossCuttingInsights, that.crossCuttingInsights) &&
           Objects.equals(strategicInsights, that.strategicInsights) &&
           Objects.equals(patternInsights, that.patternInsights) &&
           Objects.equals(trendInsights, that.trendInsights) &&
           Objects.equals(confidenceMetrics, that.confidenceMetrics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(testName, performanceInsights, runtimeInsights, crossCuttingInsights,
                       strategicInsights, patternInsights, trendInsights, confidenceMetrics);
  }

  @Override
  public String toString() {
    return "InsightAnalysisResult{" +
           "testName='" + testName + '\'' +
           ", totalInsights=" + getTotalInsightCount() +
           ", confidence=" + String.format("%.1f%%", confidenceMetrics.getOverallConfidence() * 100) +
           '}';
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

    public InsightAnalysisResult build() {
      if (confidenceMetrics == null) {
        throw new IllegalStateException("confidenceMetrics must be set");
      }
      return new InsightAnalysisResult(this);
    }
  }
}

/** Performance optimization insight with specific recommendations. */
final class PerformanceInsight {
  private final PerformanceInsightType type;
  private final String description;
  private final String runtime;
  private final List<String> recommendations;
  private final InsightSeverity severity;
  private final double confidenceScore;

  public PerformanceInsight(
      final PerformanceInsightType type,
      final String description,
      final String runtime,
      final List<String> recommendations,
      final InsightSeverity severity,
      final double confidenceScore) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.runtime = Objects.requireNonNull(runtime, "runtime cannot be null");
    this.recommendations = List.copyOf(recommendations);
    this.severity = Objects.requireNonNull(severity, "severity cannot be null");
    this.confidenceScore = confidenceScore;
  }

  public PerformanceInsightType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public String getRuntime() {
    return runtime;
  }

  public List<String> getRecommendations() {
    return recommendations;
  }

  public InsightSeverity getSeverity() {
    return severity;
  }

  public double getConfidenceScore() {
    return confidenceScore;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final PerformanceInsight that = (PerformanceInsight) obj;
    return Double.compare(that.confidenceScore, confidenceScore) == 0 &&
           type == that.type &&
           Objects.equals(description, that.description) &&
           Objects.equals(runtime, that.runtime) &&
           Objects.equals(recommendations, that.recommendations) &&
           severity == that.severity;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, description, runtime, recommendations, severity, confidenceScore);
  }

  @Override
  public String toString() {
    return "PerformanceInsight{" +
           "type=" + type +
           ", runtime='" + runtime + '\'' +
           ", severity=" + severity +
           '}';
  }
}

/** Runtime-specific insight with targeted recommendations. */
final class RuntimeInsight {
  private final RuntimeType runtime;
  private final RuntimeInsightType type;
  private final List<String> observations;
  private final List<String> recommendations;
  private final InsightSeverity severity;
  private final double confidenceScore;

  public RuntimeInsight(
      final RuntimeType runtime,
      final RuntimeInsightType type,
      final List<String> observations,
      final List<String> recommendations,
      final InsightSeverity severity,
      final double confidenceScore) {
    this.runtime = Objects.requireNonNull(runtime, "runtime cannot be null");
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.observations = List.copyOf(observations);
    this.recommendations = List.copyOf(recommendations);
    this.severity = Objects.requireNonNull(severity, "severity cannot be null");
    this.confidenceScore = confidenceScore;
  }

  public RuntimeType getRuntime() {
    return runtime;
  }

  public RuntimeInsightType getType() {
    return type;
  }

  public List<String> getObservations() {
    return observations;
  }

  public List<String> getRecommendations() {
    return recommendations;
  }

  public InsightSeverity getSeverity() {
    return severity;
  }

  public double getConfidenceScore() {
    return confidenceScore;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final RuntimeInsight that = (RuntimeInsight) obj;
    return Double.compare(that.confidenceScore, confidenceScore) == 0 &&
           runtime == that.runtime &&
           type == that.type &&
           Objects.equals(observations, that.observations) &&
           Objects.equals(recommendations, that.recommendations) &&
           severity == that.severity;
  }

  @Override
  public int hashCode() {
    return Objects.hash(runtime, type, observations, recommendations, severity, confidenceScore);
  }

  @Override
  public String toString() {
    return "RuntimeInsight{" +
           "runtime=" + runtime +
           ", type=" + type +
           ", severity=" + severity +
           '}';
  }
}

/** Cross-cutting insight spanning multiple areas. */
final class CrossCuttingInsight {
  private final CrossCuttingInsightType type;
  private final String description;
  private final List<String> observations;
  private final List<String> recommendations;
  private final InsightSeverity severity;
  private final double confidenceScore;

  public CrossCuttingInsight(
      final CrossCuttingInsightType type,
      final String description,
      final List<String> observations,
      final List<String> recommendations,
      final InsightSeverity severity,
      final double confidenceScore) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.observations = List.copyOf(observations);
    this.recommendations = List.copyOf(recommendations);
    this.severity = Objects.requireNonNull(severity, "severity cannot be null");
    this.confidenceScore = confidenceScore;
  }

  public CrossCuttingInsightType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getObservations() {
    return observations;
  }

  public List<String> getRecommendations() {
    return recommendations;
  }

  public InsightSeverity getSeverity() {
    return severity;
  }

  public double getConfidenceScore() {
    return confidenceScore;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final CrossCuttingInsight that = (CrossCuttingInsight) obj;
    return Double.compare(that.confidenceScore, confidenceScore) == 0 &&
           type == that.type &&
           Objects.equals(description, that.description) &&
           Objects.equals(observations, that.observations) &&
           Objects.equals(recommendations, that.recommendations) &&
           severity == that.severity;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, description, observations, recommendations, severity, confidenceScore);
  }

  @Override
  public String toString() {
    return "CrossCuttingInsight{" +
           "type=" + type +
           ", severity=" + severity +
           '}';
  }
}

/** Strategic insight for high-level decision making. */
final class StrategicInsight {
  private final StrategicInsightType type;
  private final String description;
  private final List<String> observations;
  private final List<String> recommendations;
  private final StrategicImportance importance;
  private final double confidenceScore;

  public StrategicInsight(
      final StrategicInsightType type,
      final String description,
      final List<String> observations,
      final List<String> recommendations,
      final StrategicImportance importance,
      final double confidenceScore) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.observations = List.copyOf(observations);
    this.recommendations = List.copyOf(recommendations);
    this.importance = Objects.requireNonNull(importance, "importance cannot be null");
    this.confidenceScore = confidenceScore;
  }

  public StrategicInsightType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getObservations() {
    return observations;
  }

  public List<String> getRecommendations() {
    return recommendations;
  }

  public StrategicImportance getImportance() {
    return importance;
  }

  public double getConfidenceScore() {
    return confidenceScore;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final StrategicInsight that = (StrategicInsight) obj;
    return Double.compare(that.confidenceScore, confidenceScore) == 0 &&
           type == that.type &&
           Objects.equals(description, that.description) &&
           Objects.equals(observations, that.observations) &&
           Objects.equals(recommendations, that.recommendations) &&
           importance == that.importance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, description, observations, recommendations, importance, confidenceScore);
  }

  @Override
  public String toString() {
    return "StrategicInsight{" +
           "type=" + type +
           ", importance=" + importance +
           '}';
  }
}

/** Insight based on observed patterns across multiple tests. */
final class PatternInsight {
  private final PatternType patternType;
  private final String description;
  private final int occurrenceCount;
  private final double confidenceScore;
  private final List<String> recommendations;

  public PatternInsight(
      final PatternType patternType,
      final String description,
      final int occurrenceCount,
      final double confidenceScore,
      final List<String> recommendations) {
    this.patternType = Objects.requireNonNull(patternType, "patternType cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.occurrenceCount = occurrenceCount;
    this.confidenceScore = confidenceScore;
    this.recommendations = List.copyOf(recommendations);
  }

  public PatternType getPatternType() {
    return patternType;
  }

  public String getDescription() {
    return description;
  }

  public int getOccurrenceCount() {
    return occurrenceCount;
  }

  public double getConfidenceScore() {
    return confidenceScore;
  }

  public List<String> getRecommendations() {
    return recommendations;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final PatternInsight that = (PatternInsight) obj;
    return occurrenceCount == that.occurrenceCount &&
           Double.compare(that.confidenceScore, confidenceScore) == 0 &&
           patternType == that.patternType &&
           Objects.equals(description, that.description) &&
           Objects.equals(recommendations, that.recommendations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(patternType, description, occurrenceCount, confidenceScore, recommendations);
  }

  @Override
  public String toString() {
    return "PatternInsight{" +
           "type=" + patternType +
           ", occurrences=" + occurrenceCount +
           ", confidence=" + String.format("%.2f", confidenceScore) +
           '}';
  }
}

/** Trend-based insight showing changes over time. */
final class TrendInsight {
  private final String metric;
  private final TrendDirection direction;
  private final double strength;
  private final String description;
  private final List<String> recommendations;

  public TrendInsight(
      final String metric,
      final TrendDirection direction,
      final double strength,
      final String description,
      final List<String> recommendations) {
    this.metric = Objects.requireNonNull(metric, "metric cannot be null");
    this.direction = Objects.requireNonNull(direction, "direction cannot be null");
    this.strength = strength;
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.recommendations = List.copyOf(recommendations);
  }

  public String getMetric() {
    return metric;
  }

  public TrendDirection getDirection() {
    return direction;
  }

  public double getStrength() {
    return strength;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getRecommendations() {
    return recommendations;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final TrendInsight that = (TrendInsight) obj;
    return Double.compare(that.strength, strength) == 0 &&
           Objects.equals(metric, that.metric) &&
           direction == that.direction &&
           Objects.equals(description, that.description) &&
           Objects.equals(recommendations, that.recommendations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metric, direction, strength, description, recommendations);
  }

  @Override
  public String toString() {
    return "TrendInsight{" +
           "metric='" + metric + '\'' +
           ", direction=" + direction +
           ", strength=" + String.format("%.2f", strength) +
           '}';
  }
}

/** Confidence metrics for insight analysis. */
final class InsightConfidenceMetrics {
  private final double overallConfidence;
  private final double performanceConfidence;
  private final double runtimeConfidence;
  private final double crossCuttingConfidence;
  private final double strategicConfidence;

  public InsightConfidenceMetrics(
      final double overallConfidence,
      final double performanceConfidence,
      final double runtimeConfidence,
      final double crossCuttingConfidence,
      final double strategicConfidence) {
    this.overallConfidence = overallConfidence;
    this.performanceConfidence = performanceConfidence;
    this.runtimeConfidence = runtimeConfidence;
    this.crossCuttingConfidence = crossCuttingConfidence;
    this.strategicConfidence = strategicConfidence;
  }

  public double getOverallConfidence() {
    return overallConfidence;
  }

  public double getPerformanceConfidence() {
    return performanceConfidence;
  }

  public double getRuntimeConfidence() {
    return runtimeConfidence;
  }

  public double getCrossCuttingConfidence() {
    return crossCuttingConfidence;
  }

  public double getStrategicConfidence() {
    return strategicConfidence;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final InsightConfidenceMetrics that = (InsightConfidenceMetrics) obj;
    return Double.compare(that.overallConfidence, overallConfidence) == 0 &&
           Double.compare(that.performanceConfidence, performanceConfidence) == 0 &&
           Double.compare(that.runtimeConfidence, runtimeConfidence) == 0 &&
           Double.compare(that.crossCuttingConfidence, crossCuttingConfidence) == 0 &&
           Double.compare(that.strategicConfidence, strategicConfidence) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(overallConfidence, performanceConfidence, runtimeConfidence,
                       crossCuttingConfidence, strategicConfidence);
  }

  @Override
  public String toString() {
    return "InsightConfidenceMetrics{" +
           "overall=" + String.format("%.2f", overallConfidence) +
           ", performance=" + String.format("%.2f", performanceConfidence) +
           ", runtime=" + String.format("%.2f", runtimeConfidence) +
           ", crossCutting=" + String.format("%.2f", crossCuttingConfidence) +
           ", strategic=" + String.format("%.2f", strategicConfidence) +
           '}';
  }
}

// Enums for insight categorization

/** Types of performance insights. */
enum PerformanceInsightType {
  HIGH_VARIABILITY,
  MEMORY_SPIKE,
  PERFORMANCE_OUTLIERS,
  RUNTIME_PERFORMANCE_GAP
}

/** Types of runtime-specific insights. */
enum RuntimeInsightType {
  GENERAL,
  BEHAVIORAL_ISSUES,
  RELIABILITY_ISSUES,
  FEATURE_GAPS,
  PERFORMANCE_OPTIMIZATION
}

/** Types of cross-cutting insights. */
enum CrossCuttingInsightType {
  BEHAVIORAL_PERFORMANCE_CORRELATION,
  COVERAGE_PERFORMANCE_RELATIONSHIP,
  FEATURE_INTERACTION_COMPLEXITY
}

/** Types of strategic insights. */
enum StrategicInsightType {
  DEVELOPMENT_FOCUS,
  RUNTIME_OPTIMIZATION,
  TESTING_STRATEGY
}

/** Severity levels for insights. */
enum InsightSeverity {
  LOW,
  MEDIUM,
  HIGH
}

/** Strategic importance levels. */
enum StrategicImportance {
  LOW,
  MEDIUM,
  HIGH
}

/** Types of patterns that can be identified. */
enum PatternType {
  HIGH_VARIABILITY,
  MEMORY_SPIKES,
  RUNTIME_SPECIFIC
}

/** Trend directions for time-series analysis. */
enum TrendDirection {
  IMPROVING,
  DECLINING,
  STABLE
}

/** Container for complete test analysis results. */
final class CompleteTestAnalysis {
  private final BehavioralAnalysisResult behavioralResults;
  private final PerformanceAnalyzer.PerformanceComparisonResult performanceResults;
  private final CoverageAnalysisResult coverageResults;
  private final RecommendationResult recommendationResults;

  public CompleteTestAnalysis(
      final BehavioralAnalysisResult behavioralResults,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults,
      final CoverageAnalysisResult coverageResults,
      final RecommendationResult recommendationResults) {
    this.behavioralResults = Objects.requireNonNull(behavioralResults, "behavioralResults cannot be null");
    this.performanceResults = Objects.requireNonNull(performanceResults, "performanceResults cannot be null");
    this.coverageResults = Objects.requireNonNull(coverageResults, "coverageResults cannot be null");
    this.recommendationResults = Objects.requireNonNull(recommendationResults, "recommendationResults cannot be null");
  }

  public BehavioralAnalysisResult getBehavioralResults() {
    return behavioralResults;
  }

  public PerformanceAnalyzer.PerformanceComparisonResult getPerformanceResults() {
    return performanceResults;
  }

  public CoverageAnalysisResult getCoverageResults() {
    return coverageResults;
  }

  public RecommendationResult getRecommendationResults() {
    return recommendationResults;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final CompleteTestAnalysis that = (CompleteTestAnalysis) obj;
    return Objects.equals(behavioralResults, that.behavioralResults) &&
           Objects.equals(performanceResults, that.performanceResults) &&
           Objects.equals(coverageResults, that.coverageResults) &&
           Objects.equals(recommendationResults, that.recommendationResults);
  }

  @Override
  public int hashCode() {
    return Objects.hash(behavioralResults, performanceResults, coverageResults, recommendationResults);
  }

  @Override
  public String toString() {
    return "CompleteTestAnalysis{" +
           "behavioral=" + behavioralResults +
           ", performance=" + performanceResults +
           ", coverage=" + coverageResults +
           ", recommendations=" + recommendationResults +
           '}';
  }
}
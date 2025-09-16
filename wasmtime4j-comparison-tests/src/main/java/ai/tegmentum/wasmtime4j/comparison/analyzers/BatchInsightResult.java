package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Comprehensive batch insight result containing global patterns, runtime health scores, and
 * executive insights across multiple test analyses.
 *
 * @since 1.0.0
 */
public final class BatchInsightResult {
  private final Map<String, InsightAnalysisResult> testInsights;
  private final List<GlobalPattern> globalPatterns;
  private final Map<RuntimeType, RuntimeHealthScore> runtimeHealthScores;
  private final List<ExecutiveInsight> executiveInsights;
  private final double batchConfidenceScore;
  private final Instant analysisTime;

  public BatchInsightResult(
      final Map<String, InsightAnalysisResult> testInsights,
      final List<GlobalPattern> globalPatterns,
      final Map<RuntimeType, RuntimeHealthScore> runtimeHealthScores,
      final List<ExecutiveInsight> executiveInsights,
      final double batchConfidenceScore,
      final Instant analysisTime) {
    this.testInsights = Map.copyOf(testInsights);
    this.globalPatterns = List.copyOf(globalPatterns);
    this.runtimeHealthScores = Map.copyOf(runtimeHealthScores);
    this.executiveInsights = List.copyOf(executiveInsights);
    this.batchConfidenceScore = batchConfidenceScore;
    this.analysisTime = Objects.requireNonNull(analysisTime, "analysisTime cannot be null");
  }

  public Map<String, InsightAnalysisResult> getTestInsights() {
    return testInsights;
  }

  public List<GlobalPattern> getGlobalPatterns() {
    return globalPatterns;
  }

  public Map<RuntimeType, RuntimeHealthScore> getRuntimeHealthScores() {
    return runtimeHealthScores;
  }

  public List<ExecutiveInsight> getExecutiveInsights() {
    return executiveInsights;
  }

  public double getBatchConfidenceScore() {
    return batchConfidenceScore;
  }

  public Instant getAnalysisTime() {
    return analysisTime;
  }

  /**
   * Gets the total number of insights across all tests.
   *
   * @return total insight count
   */
  public int getTotalInsightCount() {
    return testInsights.values().stream()
        .mapToInt(InsightAnalysisResult::getTotalInsightCount)
        .sum();
  }

  /**
   * Gets the runtime with the best overall health score.
   *
   * @return runtime with best health score
   */
  public RuntimeType getBestPerformingRuntime() {
    return runtimeHealthScores.entrySet().stream()
        .max(
            Map.Entry.comparingByValue(
                (score1, score2) ->
                    Double.compare(score1.getOverallScore(), score2.getOverallScore())))
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /**
   * Generates an executive dashboard summary.
   *
   * @return formatted executive dashboard
   */
  public String getExecutiveDashboard() {
    final RuntimeType bestRuntime = getBestPerformingRuntime();
    final long criticalPatterns =
        globalPatterns.stream().filter(pattern -> pattern.getFrequency() >= 0.5).count();

    return String.format(
        "Executive Dashboard - Batch Analysis%n"
            + "====================================%n"
            + "Analysis Time: %s%n"
            + "Tests Analyzed: %d%n"
            + "Overall Confidence: %.1f%%%n%n"
            + "Global Insights:%n"
            + "  Patterns Identified: %d%n"
            + "  Critical Patterns: %d%n"
            + "  Executive Insights: %d%n%n"
            + "Runtime Health:%n"
            + "  Best Performing: %s%n"
            + "%s%n"
            + "Total Insights Generated: %d",
        analysisTime,
        testInsights.size(),
        batchConfidenceScore * 100,
        globalPatterns.size(),
        criticalPatterns,
        executiveInsights.size(),
        bestRuntime != null ? bestRuntime.name() : "UNKNOWN",
        formatRuntimeHealthScores(),
        getTotalInsightCount());
  }

  private String formatRuntimeHealthScores() {
    final StringBuilder sb = new StringBuilder();
    for (final Map.Entry<RuntimeType, RuntimeHealthScore> entry : runtimeHealthScores.entrySet()) {
      sb.append(
          String.format(
              "  %s: %.1f%% (%s)%n",
              entry.getKey(),
              entry.getValue().getOverallScore() * 100,
              entry.getValue().getHealthStatus()));
    }
    return sb.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final BatchInsightResult that = (BatchInsightResult) obj;
    return Double.compare(that.batchConfidenceScore, batchConfidenceScore) == 0
        && Objects.equals(testInsights, that.testInsights)
        && Objects.equals(globalPatterns, that.globalPatterns)
        && Objects.equals(runtimeHealthScores, that.runtimeHealthScores)
        && Objects.equals(executiveInsights, that.executiveInsights)
        && Objects.equals(analysisTime, that.analysisTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        testInsights,
        globalPatterns,
        runtimeHealthScores,
        executiveInsights,
        batchConfidenceScore,
        analysisTime);
  }

  @Override
  public String toString() {
    return "BatchInsightResult{"
        + "tests="
        + testInsights.size()
        + ", patterns="
        + globalPatterns.size()
        + ", confidence="
        + String.format("%.1f%%", batchConfidenceScore * 100)
        + '}';
  }
}

/** Global pattern identified across multiple tests. */
final class GlobalPattern {
  private final GlobalPatternType type;
  private final String patternId;
  private final String description;
  private final int occurrenceCount;
  private final double frequency;

  public GlobalPattern(
      final GlobalPatternType type,
      final String patternId,
      final String description,
      final int occurrenceCount,
      final double frequency) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.patternId = Objects.requireNonNull(patternId, "patternId cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.occurrenceCount = occurrenceCount;
    this.frequency = frequency;
  }

  public GlobalPatternType getType() {
    return type;
  }

  public String getPatternId() {
    return patternId;
  }

  public String getDescription() {
    return description;
  }

  public int getOccurrenceCount() {
    return occurrenceCount;
  }

  public double getFrequency() {
    return frequency;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final GlobalPattern that = (GlobalPattern) obj;
    return occurrenceCount == that.occurrenceCount
        && Double.compare(that.frequency, frequency) == 0
        && type == that.type
        && Objects.equals(patternId, that.patternId)
        && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, patternId, description, occurrenceCount, frequency);
  }

  @Override
  public String toString() {
    return "GlobalPattern{"
        + "type="
        + type
        + ", id='"
        + patternId
        + '\''
        + ", frequency="
        + String.format("%.1f%%", frequency * 100)
        + '}';
  }
}

/** Runtime health score with detailed metrics. */
final class RuntimeHealthScore {
  private final RuntimeType runtime;
  private final double overallScore;
  private final double performanceScore;
  private final double behavioralScore;
  private final double coverageScore;
  private final HealthStatus healthStatus;

  public RuntimeHealthScore(
      final RuntimeType runtime,
      final double overallScore,
      final double performanceScore,
      final double behavioralScore,
      final double coverageScore,
      final HealthStatus healthStatus) {
    this.runtime = Objects.requireNonNull(runtime, "runtime cannot be null");
    this.overallScore = overallScore;
    this.performanceScore = performanceScore;
    this.behavioralScore = behavioralScore;
    this.coverageScore = coverageScore;
    this.healthStatus = Objects.requireNonNull(healthStatus, "healthStatus cannot be null");
  }

  public RuntimeType getRuntime() {
    return runtime;
  }

  public double getOverallScore() {
    return overallScore;
  }

  public double getPerformanceScore() {
    return performanceScore;
  }

  public double getBehavioralScore() {
    return behavioralScore;
  }

  public double getCoverageScore() {
    return coverageScore;
  }

  public HealthStatus getHealthStatus() {
    return healthStatus;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final RuntimeHealthScore that = (RuntimeHealthScore) obj;
    return Double.compare(that.overallScore, overallScore) == 0
        && Double.compare(that.performanceScore, performanceScore) == 0
        && Double.compare(that.behavioralScore, behavioralScore) == 0
        && Double.compare(that.coverageScore, coverageScore) == 0
        && runtime == that.runtime
        && healthStatus == that.healthStatus;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        runtime, overallScore, performanceScore, behavioralScore, coverageScore, healthStatus);
  }

  @Override
  public String toString() {
    return "RuntimeHealthScore{"
        + "runtime="
        + runtime
        + ", overall="
        + String.format("%.1f%%", overallScore * 100)
        + ", status="
        + healthStatus
        + '}';
  }
}

/** Executive insight for high-level strategic guidance. */
final class ExecutiveInsight {
  private final ExecutiveInsightType type;
  private final String description;
  private final HealthStatus status;
  private final List<String> keyPoints;

  public ExecutiveInsight(
      final ExecutiveInsightType type,
      final String description,
      final HealthStatus status,
      final List<String> keyPoints) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.status = Objects.requireNonNull(status, "status cannot be null");
    this.keyPoints = List.copyOf(keyPoints);
  }

  public ExecutiveInsightType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public HealthStatus getStatus() {
    return status;
  }

  public List<String> getKeyPoints() {
    return keyPoints;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final ExecutiveInsight that = (ExecutiveInsight) obj;
    return type == that.type
        && Objects.equals(description, that.description)
        && status == that.status
        && Objects.equals(keyPoints, that.keyPoints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, description, status, keyPoints);
  }

  @Override
  public String toString() {
    return "ExecutiveInsight{" + "type=" + type + ", status=" + status + '}';
  }
}

/** Optimization recommendation with priority and confidence. */
final class OptimizationRecommendation {
  private final PatternType patternType;
  private final String description;
  private final List<String> actionItems;
  private final OptimizationPriority priority;
  private final double confidenceScore;

  public OptimizationRecommendation(
      final PatternType patternType,
      final String description,
      final List<String> actionItems,
      final OptimizationPriority priority,
      final double confidenceScore) {
    this.patternType = Objects.requireNonNull(patternType, "patternType cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.actionItems = List.copyOf(actionItems);
    this.priority = Objects.requireNonNull(priority, "priority cannot be null");
    this.confidenceScore = confidenceScore;
  }

  public PatternType getPatternType() {
    return patternType;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getActionItems() {
    return actionItems;
  }

  public OptimizationPriority getPriority() {
    return priority;
  }

  public double getConfidenceScore() {
    return confidenceScore;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final OptimizationRecommendation that = (OptimizationRecommendation) obj;
    return Double.compare(that.confidenceScore, confidenceScore) == 0
        && patternType == that.patternType
        && Objects.equals(description, that.description)
        && Objects.equals(actionItems, that.actionItems)
        && priority == that.priority;
  }

  @Override
  public int hashCode() {
    return Objects.hash(patternType, description, actionItems, priority, confidenceScore);
  }

  @Override
  public String toString() {
    return "OptimizationRecommendation{"
        + "type="
        + patternType
        + ", priority="
        + priority
        + ", confidence="
        + String.format("%.2f", confidenceScore)
        + '}';
  }
}

/** Trend data for time-series analysis. */
final class TrendData {
  private final TrendDirection trendDirection;
  private final double trendStrength;
  private final String description;
  private final List<String> recommendations;

  public TrendData(
      final TrendDirection trendDirection,
      final double trendStrength,
      final String description,
      final List<String> recommendations) {
    this.trendDirection = Objects.requireNonNull(trendDirection, "trendDirection cannot be null");
    this.trendStrength = trendStrength;
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.recommendations = List.copyOf(recommendations);
  }

  public TrendDirection getTrendDirection() {
    return trendDirection;
  }

  public double getTrendStrength() {
    return trendStrength;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getRecommendations() {
    return recommendations;
  }

  /**
   * Checks if this trend is significant enough to warrant attention.
   *
   * @return true if trend is significant
   */
  public boolean hasSignificantTrend() {
    return trendStrength > 5.0 && trendDirection != TrendDirection.STABLE;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final TrendData that = (TrendData) obj;
    return Double.compare(that.trendStrength, trendStrength) == 0
        && trendDirection == that.trendDirection
        && Objects.equals(description, that.description)
        && Objects.equals(recommendations, that.recommendations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trendDirection, trendStrength, description, recommendations);
  }

  @Override
  public String toString() {
    return "TrendData{"
        + "direction="
        + trendDirection
        + ", strength="
        + String.format("%.2f", trendStrength)
        + '}';
  }
}

// Enums for batch insight analysis

/** Types of global patterns. */
enum GlobalPatternType {
  PERFORMANCE,
  BEHAVIORAL,
  COVERAGE
}

/** Types of executive insights. */
enum ExecutiveInsightType {
  SYSTEM_HEALTH,
  CRITICAL_PATTERNS,
  RUNTIME_COMPARISON
}

/** Health status levels. */
enum HealthStatus {
  EXCELLENT,
  GOOD,
  FAIR,
  POOR
}

/** Optimization priority levels. */
enum OptimizationPriority {
  LOW,
  MEDIUM,
  HIGH
}

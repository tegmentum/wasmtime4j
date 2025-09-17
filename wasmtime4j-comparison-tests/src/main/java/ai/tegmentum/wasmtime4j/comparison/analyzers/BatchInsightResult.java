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

  /**
   * Creates a new batch insight result with comprehensive analysis data.
   *
   * @param testInsights individual test insight analysis results
   * @param globalPatterns global patterns identified across tests
   * @param runtimeHealthScores health scores per runtime
   * @param executiveInsights high-level executive insights
   * @param batchConfidenceScore overall confidence score for the batch
   * @param analysisTime when the analysis was performed
   */
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
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
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

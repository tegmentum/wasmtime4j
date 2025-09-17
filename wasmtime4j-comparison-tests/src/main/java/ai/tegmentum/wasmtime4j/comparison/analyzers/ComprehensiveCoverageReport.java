package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Comprehensive coverage report providing a global view of test coverage across all analyzed tests,
 * feature categories, and runtime implementations.
 *
 * @since 1.0.0
 */
public final class ComprehensiveCoverageReport {
  private final Map<String, Double> categoryCompleteness;
  private final List<String> uncoveredFeatures;
  private final Map<RuntimeType, Double> runtimeCoverageScores;
  private final List<CoverageRecommendation> recommendations;
  private final CoverageTrend coverageTrend;
  private final int totalTestsAnalyzed;
  private final Instant reportGeneratedAt;

  /**
   * Creates a new comprehensive coverage report with the specified metrics and analysis results.
   *
   * @param categoryCompleteness completion percentages by feature category
   * @param uncoveredFeatures list of features that lack test coverage
   * @param runtimeCoverageScores coverage scores per WebAssembly runtime
   * @param recommendations list of coverage improvement recommendations
   * @param coverageTrend overall coverage trend analysis
   * @param totalTestsAnalyzed total number of tests included in the analysis
   * @param reportGeneratedAt timestamp when this report was generated
   */
  public ComprehensiveCoverageReport(
      final Map<String, Double> categoryCompleteness,
      final List<String> uncoveredFeatures,
      final Map<RuntimeType, Double> runtimeCoverageScores,
      final List<CoverageRecommendation> recommendations,
      final CoverageTrend coverageTrend,
      final int totalTestsAnalyzed,
      final Instant reportGeneratedAt) {
    this.categoryCompleteness = Map.copyOf(categoryCompleteness);
    this.uncoveredFeatures = List.copyOf(uncoveredFeatures);
    this.runtimeCoverageScores = Map.copyOf(runtimeCoverageScores);
    this.recommendations = List.copyOf(recommendations);
    this.coverageTrend = Objects.requireNonNull(coverageTrend, "coverageTrend cannot be null");
    this.totalTestsAnalyzed = totalTestsAnalyzed;
    this.reportGeneratedAt =
        Objects.requireNonNull(reportGeneratedAt, "reportGeneratedAt cannot be null");
  }

  public Map<String, Double> getCategoryCompleteness() {
    return categoryCompleteness;
  }

  public List<String> getUncoveredFeatures() {
    return uncoveredFeatures;
  }

  public Map<RuntimeType, Double> getRuntimeCoverageScores() {
    return runtimeCoverageScores;
  }

  public List<CoverageRecommendation> getRecommendations() {
    return recommendations;
  }

  public CoverageTrend getCoverageTrend() {
    return coverageTrend;
  }

  public int getTotalTestsAnalyzed() {
    return totalTestsAnalyzed;
  }

  public Instant getReportGeneratedAt() {
    return reportGeneratedAt;
  }

  /**
   * Gets the overall coverage score across all categories.
   *
   * @return average coverage percentage
   */
  public double getOverallCoverageScore() {
    return categoryCompleteness.values().stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.0);
  }

  /**
   * Gets the number of high-priority recommendations.
   *
   * @return count of high-priority recommendations
   */
  public long getHighPriorityRecommendationCount() {
    return recommendations.stream()
        .filter(rec -> rec.getPriority() == RecommendationPriority.HIGH)
        .count();
  }

  /**
   * Generates a formatted executive summary of the coverage report.
   *
   * @return formatted executive summary
   */
  public String getExecutiveSummary() {
    return String.format(
        "Coverage Report Executive Summary%n"
            + "================================%n"
            + "Report Generated: %s%n"
            + "Tests Analyzed: %d%n"
            + "Overall Coverage: %.1f%%%n"
            + "Uncovered Features: %d%n"
            + "High-Priority Recommendations: %d%n"
            + "Coverage Trend: %s (%.1f%% change)%n%n"
            + "Runtime Coverage Scores:%n%s%n%n"
            + "Category Completeness:%n%s",
        reportGeneratedAt,
        totalTestsAnalyzed,
        getOverallCoverageScore(),
        uncoveredFeatures.size(),
        getHighPriorityRecommendationCount(),
        coverageTrend.getDirection(),
        coverageTrend.getChange(),
        formatRuntimeScores(),
        formatCategoryCompleteness());
  }

  private String formatRuntimeScores() {
    final StringBuilder sb = new StringBuilder();
    for (final Map.Entry<RuntimeType, Double> entry : runtimeCoverageScores.entrySet()) {
      sb.append(String.format("  %s: %.1f%%%n", entry.getKey(), entry.getValue()));
    }
    return sb.toString();
  }

  private String formatCategoryCompleteness() {
    final StringBuilder sb = new StringBuilder();
    for (final Map.Entry<String, Double> entry : categoryCompleteness.entrySet()) {
      sb.append(String.format("  %s: %.1f%%%n", entry.getKey(), entry.getValue()));
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

    final ComprehensiveCoverageReport that = (ComprehensiveCoverageReport) obj;
    return totalTestsAnalyzed == that.totalTestsAnalyzed
        && Objects.equals(categoryCompleteness, that.categoryCompleteness)
        && Objects.equals(uncoveredFeatures, that.uncoveredFeatures)
        && Objects.equals(runtimeCoverageScores, that.runtimeCoverageScores)
        && Objects.equals(recommendations, that.recommendations)
        && Objects.equals(coverageTrend, that.coverageTrend)
        && Objects.equals(reportGeneratedAt, that.reportGeneratedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        categoryCompleteness,
        uncoveredFeatures,
        runtimeCoverageScores,
        recommendations,
        coverageTrend,
        totalTestsAnalyzed,
        reportGeneratedAt);
  }

  @Override
  public String toString() {
    return "ComprehensiveCoverageReport{"
        + "tests="
        + totalTestsAnalyzed
        + ", coverage="
        + String.format("%.1f%%", getOverallCoverageScore())
        + ", uncovered="
        + uncoveredFeatures.size()
        + ", recommendations="
        + recommendations.size()
        + '}';
  }
}

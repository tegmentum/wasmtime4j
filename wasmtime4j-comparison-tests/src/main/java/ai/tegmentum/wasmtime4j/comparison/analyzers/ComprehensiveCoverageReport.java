package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Comprehensive coverage report providing a global view of test coverage across
 * all analyzed tests, feature categories, and runtime implementations.
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
    this.reportGeneratedAt = Objects.requireNonNull(reportGeneratedAt, "reportGeneratedAt cannot be null");
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
        "Coverage Report Executive Summary%n" +
        "================================%n" +
        "Report Generated: %s%n" +
        "Tests Analyzed: %d%n" +
        "Overall Coverage: %.1f%%%n" +
        "Uncovered Features: %d%n" +
        "High-Priority Recommendations: %d%n" +
        "Coverage Trend: %s (%.1f%% change)%n%n" +
        "Runtime Coverage Scores:%n%s%n%n" +
        "Category Completeness:%n%s",
        reportGeneratedAt,
        totalTestsAnalyzed,
        getOverallCoverageScore(),
        uncoveredFeatures.size(),
        getHighPriorityRecommendationCount(),
        coverageTrend.getDirection(),
        coverageTrend.getChange(),
        formatRuntimeScores(),
        formatCategoryCompleteness()
    );
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
    return totalTestsAnalyzed == that.totalTestsAnalyzed &&
           Objects.equals(categoryCompleteness, that.categoryCompleteness) &&
           Objects.equals(uncoveredFeatures, that.uncoveredFeatures) &&
           Objects.equals(runtimeCoverageScores, that.runtimeCoverageScores) &&
           Objects.equals(recommendations, that.recommendations) &&
           Objects.equals(coverageTrend, that.coverageTrend) &&
           Objects.equals(reportGeneratedAt, that.reportGeneratedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(categoryCompleteness, uncoveredFeatures, runtimeCoverageScores,
                       recommendations, coverageTrend, totalTestsAnalyzed, reportGeneratedAt);
  }

  @Override
  public String toString() {
    return "ComprehensiveCoverageReport{" +
           "tests=" + totalTestsAnalyzed +
           ", coverage=" + String.format("%.1f%%", getOverallCoverageScore()) +
           ", uncovered=" + uncoveredFeatures.size() +
           ", recommendations=" + recommendations.size() +
           '}';
  }
}

/** Global coverage statistics across all tests and runtimes. */
final class GlobalCoverageStatistics {
  private final int totalFeatures;
  private final int coveredFeatures;
  private final double overallCoveragePercentage;
  private final int totalTestsAnalyzed;
  private final int totalCategories;

  public GlobalCoverageStatistics(
      final int totalFeatures,
      final int coveredFeatures,
      final double overallCoveragePercentage,
      final int totalTestsAnalyzed,
      final int totalCategories) {
    this.totalFeatures = totalFeatures;
    this.coveredFeatures = coveredFeatures;
    this.overallCoveragePercentage = overallCoveragePercentage;
    this.totalTestsAnalyzed = totalTestsAnalyzed;
    this.totalCategories = totalCategories;
  }

  public int getTotalFeatures() {
    return totalFeatures;
  }

  public int getCoveredFeatures() {
    return coveredFeatures;
  }

  public double getOverallCoveragePercentage() {
    return overallCoveragePercentage;
  }

  public int getTotalTestsAnalyzed() {
    return totalTestsAnalyzed;
  }

  public int getTotalCategories() {
    return totalCategories;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final GlobalCoverageStatistics that = (GlobalCoverageStatistics) obj;
    return totalFeatures == that.totalFeatures &&
           coveredFeatures == that.coveredFeatures &&
           Double.compare(that.overallCoveragePercentage, overallCoveragePercentage) == 0 &&
           totalTestsAnalyzed == that.totalTestsAnalyzed &&
           totalCategories == that.totalCategories;
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalFeatures, coveredFeatures, overallCoveragePercentage,
                       totalTestsAnalyzed, totalCategories);
  }

  @Override
  public String toString() {
    return "GlobalCoverageStatistics{" +
           "features=" + coveredFeatures + "/" + totalFeatures +
           ", coverage=" + String.format("%.1f%%", overallCoveragePercentage) +
           ", tests=" + totalTestsAnalyzed +
           '}';
  }
}

/** Actionable recommendation for improving test coverage. */
final class CoverageRecommendation {
  private final RecommendationType type;
  private final String description;
  private final RecommendationPriority priority;
  private final Set<String> affectedAreas;

  public CoverageRecommendation(
      final RecommendationType type,
      final String description,
      final RecommendationPriority priority,
      final Set<String> affectedAreas) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.priority = Objects.requireNonNull(priority, "priority cannot be null");
    this.affectedAreas = Set.copyOf(affectedAreas);
  }

  public RecommendationType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public RecommendationPriority getPriority() {
    return priority;
  }

  public Set<String> getAffectedAreas() {
    return affectedAreas;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CoverageRecommendation that = (CoverageRecommendation) obj;
    return type == that.type &&
           Objects.equals(description, that.description) &&
           priority == that.priority &&
           Objects.equals(affectedAreas, that.affectedAreas);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, description, priority, affectedAreas);
  }

  @Override
  public String toString() {
    return "CoverageRecommendation{" +
           "type=" + type +
           ", priority=" + priority +
           ", areas=" + affectedAreas.size() +
           '}';
  }
}

/** Types of coverage recommendations. */
enum RecommendationType {
  /** Increase coverage for a specific category. */
  INCREASE_CATEGORY_COVERAGE,

  /** Improve existing coverage for a category. */
  IMPROVE_CATEGORY_COVERAGE,

  /** Improve coverage for a specific runtime. */
  IMPROVE_RUNTIME_COVERAGE,

  /** Add tests for feature interactions. */
  ADD_INTERACTION_TESTS,

  /** Address high-severity coverage gaps. */
  ADDRESS_CRITICAL_GAPS
}

/** Priority levels for recommendations. */
enum RecommendationPriority {
  /** Low priority recommendation. */
  LOW,

  /** Medium priority recommendation. */
  MEDIUM,

  /** High priority recommendation requiring immediate attention. */
  HIGH
}

/** Coverage trend analysis over time. */
final class CoverageTrend {
  private final double currentCoverage;
  private final double previousCoverage;
  private final double change;
  private final TrendDirection direction;

  public CoverageTrend(
      final double currentCoverage,
      final double previousCoverage,
      final double change,
      final TrendDirection direction) {
    this.currentCoverage = currentCoverage;
    this.previousCoverage = previousCoverage;
    this.change = change;
    this.direction = Objects.requireNonNull(direction, "direction cannot be null");
  }

  public double getCurrentCoverage() {
    return currentCoverage;
  }

  public double getPreviousCoverage() {
    return previousCoverage;
  }

  public double getChange() {
    return change;
  }

  public TrendDirection getDirection() {
    return direction;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CoverageTrend that = (CoverageTrend) obj;
    return Double.compare(that.currentCoverage, currentCoverage) == 0 &&
           Double.compare(that.previousCoverage, previousCoverage) == 0 &&
           Double.compare(that.change, change) == 0 &&
           direction == that.direction;
  }

  @Override
  public int hashCode() {
    return Objects.hash(currentCoverage, previousCoverage, change, direction);
  }

  @Override
  public String toString() {
    return "CoverageTrend{" +
           "current=" + String.format("%.1f%%", currentCoverage) +
           ", change=" + String.format("%.1f%%", change) +
           ", direction=" + direction +
           '}';
  }
}

/** Direction of coverage trend. */
enum TrendDirection {
  /** Coverage is improving. */
  IMPROVING,

  /** Coverage is declining. */
  DECLINING,

  /** Coverage is stable. */
  STABLE
}
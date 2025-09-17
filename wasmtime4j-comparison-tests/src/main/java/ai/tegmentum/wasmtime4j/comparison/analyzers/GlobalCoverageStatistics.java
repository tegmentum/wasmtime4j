package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;

/**
 * Global coverage statistics across all tests and runtimes.
 *
 * @since 1.0.0
 */
public final class GlobalCoverageStatistics {
  private final int totalFeatures;
  private final int coveredFeatures;
  private final double overallCoveragePercentage;
  private final int totalTestsAnalyzed;
  private final int totalCategories;

  /**
   * Constructs a new GlobalCoverageStatistics with the specified data.
   *
   * @param totalFeatures the total number of features
   * @param coveredFeatures the number of covered features
   * @param overallCoveragePercentage the overall coverage percentage
   * @param totalTestsAnalyzed the total number of tests analyzed
   * @param totalCategories the total number of categories
   */
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
    return totalFeatures == that.totalFeatures
        && coveredFeatures == that.coveredFeatures
        && Double.compare(that.overallCoveragePercentage, overallCoveragePercentage) == 0
        && totalTestsAnalyzed == that.totalTestsAnalyzed
        && totalCategories == that.totalCategories;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        totalFeatures,
        coveredFeatures,
        overallCoveragePercentage,
        totalTestsAnalyzed,
        totalCategories);
  }

  @Override
  public String toString() {
    return "GlobalCoverageStatistics{"
        + "features="
        + coveredFeatures
        + "/"
        + totalFeatures
        + ", coverage="
        + String.format("%.1f%%", overallCoveragePercentage)
        + ", tests="
        + totalTestsAnalyzed
        + '}';
  }
}

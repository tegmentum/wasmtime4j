package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Map;
import java.util.Objects;

/**
 * Metrics related to feature coverage analysis.
 *
 * @since 1.0.0
 */
public final class CoverageMetrics {
  private final int totalDetectedFeatures;
  private final int coveredFeatures;
  private final double overallCoveragePercentage;
  private final Map<RuntimeType, Double> runtimeCoveragePercentages;
  private final double successRate;

  /**
   * Creates new coverage metrics.
   *
   * @param totalDetectedFeatures the total number of detected features
   * @param coveredFeatures the number of covered features
   * @param overallCoveragePercentage the overall coverage percentage
   * @param runtimeCoveragePercentages the coverage percentages by runtime
   * @param successRate the success rate
   */
  public CoverageMetrics(
      final int totalDetectedFeatures,
      final int coveredFeatures,
      final double overallCoveragePercentage,
      final Map<RuntimeType, Double> runtimeCoveragePercentages,
      final double successRate) {
    this.totalDetectedFeatures = totalDetectedFeatures;
    this.coveredFeatures = coveredFeatures;
    this.overallCoveragePercentage = overallCoveragePercentage;
    this.runtimeCoveragePercentages = Map.copyOf(runtimeCoveragePercentages);
    this.successRate = successRate;
  }

  /**
   * Gets the total detected features.
   *
   * @return the total detected features
   */
  public int getTotalDetectedFeatures() {
    return totalDetectedFeatures;
  }

  /**
   * Gets the covered features.
   *
   * @return the covered features
   */
  public int getCoveredFeatures() {
    return coveredFeatures;
  }

  /**
   * Gets the overall coverage percentage.
   *
   * @return the overall coverage percentage
   */
  public double getOverallCoveragePercentage() {
    return overallCoveragePercentage;
  }

  /**
   * Gets the runtime coverage percentages.
   *
   * @return the runtime coverage percentages
   */
  public Map<RuntimeType, Double> getRuntimeCoveragePercentages() {
    return runtimeCoveragePercentages;
  }

  /**
   * Gets the success rate.
   *
   * @return the success rate
   */
  public double getSuccessRate() {
    return successRate;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CoverageMetrics that = (CoverageMetrics) obj;
    return totalDetectedFeatures == that.totalDetectedFeatures
        && coveredFeatures == that.coveredFeatures
        && Double.compare(that.overallCoveragePercentage, overallCoveragePercentage) == 0
        && Double.compare(that.successRate, successRate) == 0
        && Objects.equals(runtimeCoveragePercentages, that.runtimeCoveragePercentages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        totalDetectedFeatures,
        coveredFeatures,
        overallCoveragePercentage,
        runtimeCoveragePercentages,
        successRate);
  }

  @Override
  public String toString() {
    return "CoverageMetrics{"
        + "total="
        + totalDetectedFeatures
        + ", covered="
        + coveredFeatures
        + ", overall="
        + String.format("%.1f%%", overallCoveragePercentage)
        + ", success="
        + String.format("%.1f%%", successRate)
        + '}';
  }
}

package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Summary of performance analysis across all tests.
 *
 * @since 1.0.0
 */
public final class PerformanceAnalysisSummary {
  private final double averagePerformanceVariance;
  private final double maxPerformanceVariance;
  private final Map<RuntimeType, Double> runtimePerformanceScores;
  private final List<String> outlierTests;
  private final Map<String, Double> performanceTrends;

  /**
   * Constructs a new PerformanceAnalysisSummary with the specified performance data.
   *
   * @param averagePerformanceVariance the average performance variance across tests
   * @param maxPerformanceVariance the maximum performance variance observed
   * @param runtimePerformanceScores performance scores for each runtime
   * @param outlierTests list of tests that are performance outliers
   * @param performanceTrends performance trend data
   */
  public PerformanceAnalysisSummary(
      final double averagePerformanceVariance,
      final double maxPerformanceVariance,
      final Map<RuntimeType, Double> runtimePerformanceScores,
      final List<String> outlierTests,
      final Map<String, Double> performanceTrends) {
    this.averagePerformanceVariance = averagePerformanceVariance;
    this.maxPerformanceVariance = maxPerformanceVariance;
    this.runtimePerformanceScores = Map.copyOf(runtimePerformanceScores);
    this.outlierTests = List.copyOf(outlierTests);
    this.performanceTrends = Map.copyOf(performanceTrends);
  }

  public double getAveragePerformanceVariance() {
    return averagePerformanceVariance;
  }

  public double getMaxPerformanceVariance() {
    return maxPerformanceVariance;
  }

  public Map<RuntimeType, Double> getRuntimePerformanceScores() {
    return runtimePerformanceScores;
  }

  public List<String> getOutlierTests() {
    return outlierTests;
  }

  public Map<String, Double> getPerformanceTrends() {
    return performanceTrends;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final PerformanceAnalysisSummary that = (PerformanceAnalysisSummary) obj;
    return Double.compare(that.averagePerformanceVariance, averagePerformanceVariance) == 0
        && Double.compare(that.maxPerformanceVariance, maxPerformanceVariance) == 0
        && Objects.equals(runtimePerformanceScores, that.runtimePerformanceScores)
        && Objects.equals(outlierTests, that.outlierTests)
        && Objects.equals(performanceTrends, that.performanceTrends);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        averagePerformanceVariance,
        maxPerformanceVariance,
        runtimePerformanceScores,
        outlierTests,
        performanceTrends);
  }

  @Override
  public String toString() {
    return "PerformanceAnalysisSummary{"
        + "avgVariance="
        + String.format("%.2f", averagePerformanceVariance)
        + ", maxVariance="
        + String.format("%.2f", maxPerformanceVariance)
        + ", outliers="
        + outlierTests.size()
        + '}';
  }
}

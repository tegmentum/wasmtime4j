package ai.tegmentum.wasmtime4j.webassembly;

import java.time.Duration;
import java.util.Objects;

/**
 * Comparison between two performance benchmark results for the same test case. Provides statistical
 * analysis of performance differences between implementations.
 */
public final class PerformanceComparison {
  private final PerformanceBenchmarkResult result1;
  private final PerformanceBenchmarkResult result2;
  private final boolean successful;
  private final double performanceDifference;
  private final boolean statisticallySignificant;

  private PerformanceComparison(
      final PerformanceBenchmarkResult result1, final PerformanceBenchmarkResult result2) {
    this.result1 = result1;
    this.result2 = result2;
    this.successful = result1.isSuccessful() && result2.isSuccessful();

    if (successful) {
      final Duration time1 = result1.getStatistics().getMeanExecutionTime();
      final Duration time2 = result2.getStatistics().getMeanExecutionTime();

      // Calculate performance difference (negative means result1 is faster)
      this.performanceDifference = (time1.toNanos() - time2.toNanos()) / (double) time2.toNanos();

      // Simple statistical significance test (in real implementation would use proper statistical
      // tests)
      final double cv1 = result1.getStatistics().getCoefficientOfVariation();
      final double cv2 = result2.getStatistics().getCoefficientOfVariation();
      final double absDifference = Math.abs(performanceDifference);

      // Consider significant if difference > 5% and both results have low variability
      this.statisticallySignificant = absDifference > 0.05 && cv1 < 10.0 && cv2 < 10.0;
    } else {
      this.performanceDifference = 0.0;
      this.statisticallySignificant = false;
    }
  }

  /**
   * Creates a performance comparison between two benchmark results.
   *
   * @param result1 the first benchmark result
   * @param result2 the second benchmark result
   * @return the performance comparison
   */
  public static PerformanceComparison create(
      final PerformanceBenchmarkResult result1, final PerformanceBenchmarkResult result2) {
    Objects.requireNonNull(result1, "result1 cannot be null");
    Objects.requireNonNull(result2, "result2 cannot be null");

    if (!result1.getTestName().equals(result2.getTestName())) {
      throw new IllegalArgumentException("Cannot compare results from different tests");
    }

    return new PerformanceComparison(result1, result2);
  }

  /**
   * Gets the first benchmark result.
   *
   * @return the first benchmark result
   */
  public PerformanceBenchmarkResult getResult1() {
    return result1;
  }

  /**
   * Gets the second benchmark result.
   *
   * @return the second benchmark result
   */
  public PerformanceBenchmarkResult getResult2() {
    return result2;
  }

  /**
   * Checks if both benchmark results were successful.
   *
   * @return true if both results were successful
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets the performance difference between the two results.
   *
   * @return the performance difference as a ratio (negative means result1 is faster)
   */
  public double getPerformanceDifference() {
    return performanceDifference;
  }

  /**
   * Checks if the performance difference is statistically significant.
   *
   * @return true if the difference is statistically significant
   */
  public boolean isStatisticallySignificant() {
    return statisticallySignificant;
  }

  /**
   * Gets the performance improvement percentage (positive means result1 is faster).
   *
   * @return the performance improvement as a percentage
   */
  public double getPerformanceImprovementPercentage() {
    return -performanceDifference * 100.0;
  }

  /**
   * Checks if result1 is faster than result2.
   *
   * @return true if result1 is faster
   */
  public boolean isResult1Faster() {
    return performanceDifference < 0.0;
  }

  /**
   * Gets a description of the performance difference.
   *
   * @return a human-readable description
   */
  public String getPerformanceDescription() {
    if (!successful) {
      return "Cannot compare - one or both benchmarks failed";
    }

    final double improvementPercentage = Math.abs(getPerformanceImprovementPercentage());
    final String fasterResult =
        isResult1Faster() ? result1.getRuntimeType().name() : result2.getRuntimeType().name();

    if (improvementPercentage < 1.0) {
      return "Performance is essentially equivalent";
    } else if (improvementPercentage < 5.0) {
      return String.format("%s is slightly faster (%.1f%%)", fasterResult, improvementPercentage);
    } else if (improvementPercentage < 20.0) {
      return String.format("%s is moderately faster (%.1f%%)", fasterResult, improvementPercentage);
    } else {
      return String.format(
          "%s is significantly faster (%.1f%%)", fasterResult, improvementPercentage);
    }
  }

  @Override
  public String toString() {
    if (!successful) {
      return String.format("PerformanceComparison{test=%s, failed}", result1.getTestName());
    }

    return String.format(
        "PerformanceComparison{test=%s, diff=%.2f%%, significant=%s}",
        result1.getTestName(), performanceDifference * 100, statisticallySignificant);
  }
}

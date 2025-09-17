package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Objects;

/**
 * Runtime health score with detailed metrics.
 *
 * @since 1.0.0
 */
public final class RuntimeHealthScore {
  private final RuntimeType runtime;
  private final double overallScore;
  private final double performanceScore;
  private final double behavioralScore;
  private final double coverageScore;
  private final HealthStatus healthStatus;

  /**
   * Constructs a new RuntimeHealthScore with the specified metrics.
   *
   * @param runtime the runtime type
   * @param overallScore the overall health score
   * @param performanceScore the performance score
   * @param behavioralScore the behavioral score
   * @param coverageScore the coverage score
   * @param healthStatus the health status
   */
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
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
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

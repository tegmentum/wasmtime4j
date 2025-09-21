package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;

/**
 * Result of performance consistency validation across runtimes, analyzing execution time variance,
 * throughput parity, and performance regression patterns to ensure production-ready performance
 * characteristics.
 *
 * @since 1.0.0
 */
public final class PerformanceConsistencyResult {
  private final boolean consistent;
  private final double varianceRatio;
  private final double throughputParity;
  private final boolean withinToleranceBands;
  private final PerformanceRegressionStatus regressionStatus;

  private PerformanceConsistencyResult(final Builder builder) {
    this.consistent = builder.consistent;
    this.varianceRatio = builder.varianceRatio;
    this.throughputParity = builder.throughputParity;
    this.withinToleranceBands = builder.withinToleranceBands;
    this.regressionStatus = builder.regressionStatus;
  }

  public boolean isConsistent() {
    return consistent;
  }

  public double getVarianceRatio() {
    return varianceRatio;
  }

  public double getThroughputParity() {
    return throughputParity;
  }

  public boolean isWithinToleranceBands() {
    return withinToleranceBands;
  }

  public PerformanceRegressionStatus getRegressionStatus() {
    return regressionStatus;
  }

  /**
   * Checks if performance meets production consistency requirements.
   *
   * @return true if performance is production-ready
   */
  public boolean meetsProductionRequirements() {
    return consistent && withinToleranceBands && varianceRatio <= 1.2;
  }

  /**
   * Gets performance variance severity level.
   *
   * @return severity of performance variance
   */
  public PerformanceVarianceSeverity getVarianceSeverity() {
    if (varianceRatio <= 1.1) {
      return PerformanceVarianceSeverity.MINIMAL;
    } else if (varianceRatio <= 1.2) {
      return PerformanceVarianceSeverity.ACCEPTABLE;
    } else if (varianceRatio <= 1.5) {
      return PerformanceVarianceSeverity.MODERATE;
    } else if (varianceRatio <= 2.0) {
      return PerformanceVarianceSeverity.HIGH;
    } else {
      return PerformanceVarianceSeverity.CRITICAL;
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final PerformanceConsistencyResult that = (PerformanceConsistencyResult) obj;
    return consistent == that.consistent
        && Double.compare(that.varianceRatio, varianceRatio) == 0
        && Double.compare(that.throughputParity, throughputParity) == 0
        && withinToleranceBands == that.withinToleranceBands
        && regressionStatus == that.regressionStatus;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        consistent, varianceRatio, throughputParity, withinToleranceBands, regressionStatus);
  }

  @Override
  public String toString() {
    return "PerformanceConsistencyResult{"
        + "consistent="
        + consistent
        + ", varianceRatio="
        + String.format("%.2f", varianceRatio)
        + ", throughputParity="
        + String.format("%.2f", throughputParity)
        + ", severity="
        + getVarianceSeverity()
        + ", regressionStatus="
        + regressionStatus
        + '}';
  }

  /** Performance variance severity levels. */
  public enum PerformanceVarianceSeverity {
    MINIMAL,
    ACCEPTABLE,
    MODERATE,
    HIGH,
    CRITICAL
  }

  /** Performance regression status. */
  public enum PerformanceRegressionStatus {
    NO_REGRESSION,
    MINOR_REGRESSION,
    MODERATE_REGRESSION,
    SEVERE_REGRESSION,
    UNKNOWN
  }

  /** Builder for PerformanceConsistencyResult. */
  public static final class Builder {
    private boolean consistent = true;
    private double varianceRatio = 1.0;
    private double throughputParity = 1.0;
    private boolean withinToleranceBands = true;
    private PerformanceRegressionStatus regressionStatus =
        PerformanceRegressionStatus.NO_REGRESSION;

    public Builder consistent(final boolean consistent) {
      this.consistent = consistent;
      return this;
    }

    public Builder varianceRatio(final double varianceRatio) {
      if (varianceRatio < 0.0) {
        throw new IllegalArgumentException("varianceRatio must be non-negative");
      }
      this.varianceRatio = varianceRatio;
      return this;
    }

    public Builder throughputParity(final double throughputParity) {
      if (throughputParity < 0.0) {
        throw new IllegalArgumentException("throughputParity must be non-negative");
      }
      this.throughputParity = throughputParity;
      return this;
    }

    public Builder withinToleranceBands(final boolean withinToleranceBands) {
      this.withinToleranceBands = withinToleranceBands;
      return this;
    }

    public Builder regressionStatus(final PerformanceRegressionStatus regressionStatus) {
      this.regressionStatus =
          Objects.requireNonNull(regressionStatus, "regressionStatus cannot be null");
      return this;
    }

    public PerformanceConsistencyResult build() {
      return new PerformanceConsistencyResult(this);
    }
  }
}

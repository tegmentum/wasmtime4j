package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;

/**
 * Confidence metrics for insight analysis.
 *
 * @since 1.0.0
 */
public final class InsightConfidenceMetrics {
  private final double overallConfidence;
  private final double performanceConfidence;
  private final double runtimeConfidence;
  private final double crossCuttingConfidence;
  private final double strategicConfidence;

  /**
   * Creates new insight confidence metrics.
   *
   * @param overallConfidence the overall confidence level
   * @param performanceConfidence the performance confidence level
   * @param runtimeConfidence the runtime confidence level
   * @param crossCuttingConfidence the cross-cutting confidence level
   * @param strategicConfidence the strategic confidence level
   */
  public InsightConfidenceMetrics(
      final double overallConfidence,
      final double performanceConfidence,
      final double runtimeConfidence,
      final double crossCuttingConfidence,
      final double strategicConfidence) {
    this.overallConfidence = overallConfidence;
    this.performanceConfidence = performanceConfidence;
    this.runtimeConfidence = runtimeConfidence;
    this.crossCuttingConfidence = crossCuttingConfidence;
    this.strategicConfidence = strategicConfidence;
  }

  /**
   * Gets the overall confidence level.
   *
   * @return the overall confidence
   */
  public double getOverallConfidence() {
    return overallConfidence;
  }

  /**
   * Gets the performance confidence level.
   *
   * @return the performance confidence
   */
  public double getPerformanceConfidence() {
    return performanceConfidence;
  }

  /**
   * Gets the runtime confidence level.
   *
   * @return the runtime confidence
   */
  public double getRuntimeConfidence() {
    return runtimeConfidence;
  }

  /**
   * Gets the cross-cutting confidence level.
   *
   * @return the cross-cutting confidence
   */
  public double getCrossCuttingConfidence() {
    return crossCuttingConfidence;
  }

  /**
   * Gets the strategic confidence level.
   *
   * @return the strategic confidence
   */
  public double getStrategicConfidence() {
    return strategicConfidence;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final InsightConfidenceMetrics that = (InsightConfidenceMetrics) obj;
    return Double.compare(that.overallConfidence, overallConfidence) == 0
        && Double.compare(that.performanceConfidence, performanceConfidence) == 0
        && Double.compare(that.runtimeConfidence, runtimeConfidence) == 0
        && Double.compare(that.crossCuttingConfidence, crossCuttingConfidence) == 0
        && Double.compare(that.strategicConfidence, strategicConfidence) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        overallConfidence,
        performanceConfidence,
        runtimeConfidence,
        crossCuttingConfidence,
        strategicConfidence);
  }

  @Override
  public String toString() {
    return "InsightConfidenceMetrics{"
        + "overall="
        + String.format("%.2f", overallConfidence)
        + ", performance="
        + String.format("%.2f", performanceConfidence)
        + ", runtime="
        + String.format("%.2f", runtimeConfidence)
        + ", crossCutting="
        + String.format("%.2f", crossCuttingConfidence)
        + ", strategic="
        + String.format("%.2f", strategicConfidence)
        + '}';
  }

  /** Builder for InsightConfidenceMetrics. */
  public static final class Builder {
    private double overallConfidence = 0.8;
    private double performanceConfidence = 0.9;
    private double runtimeConfidence = 0.7;
    private double crossCuttingConfidence = 0.8;
    private double strategicConfidence = 0.6;

    /**
     * Sets the overall confidence score.
     *
     * @param overallConfidence overall confidence (0.0 to 1.0)
     * @return this builder
     */
    public Builder overallConfidence(final double overallConfidence) {
      this.overallConfidence = overallConfidence;
      return this;
    }

    /**
     * Sets the performance confidence score.
     *
     * @param performanceConfidence performance confidence (0.0 to 1.0)
     * @return this builder
     */
    public Builder performanceConfidence(final double performanceConfidence) {
      this.performanceConfidence = performanceConfidence;
      return this;
    }

    /**
     * Sets the runtime confidence score.
     *
     * @param runtimeConfidence runtime confidence (0.0 to 1.0)
     * @return this builder
     */
    public Builder runtimeConfidence(final double runtimeConfidence) {
      this.runtimeConfidence = runtimeConfidence;
      return this;
    }

    /**
     * Sets the cross-cutting confidence score.
     *
     * @param crossCuttingConfidence cross-cutting confidence (0.0 to 1.0)
     * @return this builder
     */
    public Builder crossCuttingConfidence(final double crossCuttingConfidence) {
      this.crossCuttingConfidence = crossCuttingConfidence;
      return this;
    }

    /**
     * Sets the strategic confidence score.
     *
     * @param strategicConfidence strategic confidence (0.0 to 1.0)
     * @return this builder
     */
    public Builder strategicConfidence(final double strategicConfidence) {
      this.strategicConfidence = strategicConfidence;
      return this;
    }

    /**
     * Builds the InsightConfidenceMetrics.
     *
     * @return new InsightConfidenceMetrics instance
     */
    public InsightConfidenceMetrics build() {
      return new InsightConfidenceMetrics(
          overallConfidence,
          performanceConfidence,
          runtimeConfidence,
          crossCuttingConfidence,
          strategicConfidence);
    }
  }
}

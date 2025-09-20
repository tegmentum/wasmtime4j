/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.util.Map;
import java.util.Objects;

/**
 * Result of performance parity validation between JNI and Panama implementations.
 *
 * <p>Contains detailed performance metrics and comparison results to assess whether both
 * implementations meet performance requirements.
 *
 * @since 1.0.0
 */
public final class PerformanceParityResult {

  private final Map<String, PerformanceMetric> jniMetrics;
  private final Map<String, PerformanceMetric> panamaMetrics;
  private final Map<String, Double> performanceDifferences;
  private final boolean parityAchieved;
  private final double toleranceThreshold;
  private final String summary;

  /**
   * Creates a new performance parity result.
   *
   * @param jniMetrics performance metrics for JNI implementation
   * @param panamaMetrics performance metrics for Panama implementation
   * @param performanceDifferences percentage differences by metric
   * @param parityAchieved whether performance parity was achieved
   * @param toleranceThreshold acceptable performance difference threshold
   * @param summary human-readable summary of results
   */
  public PerformanceParityResult(
      final Map<String, PerformanceMetric> jniMetrics,
      final Map<String, PerformanceMetric> panamaMetrics,
      final Map<String, Double> performanceDifferences,
      final boolean parityAchieved,
      final double toleranceThreshold,
      final String summary) {
    this.jniMetrics = Map.copyOf(Objects.requireNonNull(jniMetrics, "jniMetrics"));
    this.panamaMetrics = Map.copyOf(Objects.requireNonNull(panamaMetrics, "panamaMetrics"));
    this.performanceDifferences =
        Map.copyOf(Objects.requireNonNull(performanceDifferences, "performanceDifferences"));
    this.parityAchieved = parityAchieved;
    this.toleranceThreshold = toleranceThreshold;
    this.summary = Objects.requireNonNull(summary, "summary");
  }

  /**
   * Returns performance metrics for JNI implementation.
   *
   * @return immutable map of metric names to values
   */
  public Map<String, PerformanceMetric> getJniMetrics() {
    return jniMetrics;
  }

  /**
   * Returns performance metrics for Panama implementation.
   *
   * @return immutable map of metric names to values
   */
  public Map<String, PerformanceMetric> getPanamaMetrics() {
    return panamaMetrics;
  }

  /**
   * Returns percentage differences between implementations.
   *
   * @return immutable map of metric names to percentage differences
   */
  public Map<String, Double> getPerformanceDifferences() {
    return performanceDifferences;
  }

  /**
   * Checks if performance parity was achieved.
   *
   * @return {@code true} if parity achieved, {@code false} otherwise
   */
  public boolean isParityAchieved() {
    return parityAchieved;
  }

  /**
   * Returns the tolerance threshold for performance differences.
   *
   * @return tolerance threshold as percentage
   */
  public double getToleranceThreshold() {
    return toleranceThreshold;
  }

  /**
   * Returns human-readable summary of results.
   *
   * @return performance analysis summary
   */
  public String getSummary() {
    return summary;
  }

  /**
   * Returns the maximum performance difference detected.
   *
   * @return maximum difference percentage
   */
  public double getMaximumDifference() {
    return performanceDifferences.values().stream().mapToDouble(Math::abs).max().orElse(0.0);
  }

  @Override
  public String toString() {
    return "PerformanceParityResult{"
        + "parityAchieved="
        + parityAchieved
        + ", maxDifference="
        + String.format("%.2f%%", getMaximumDifference())
        + ", threshold="
        + String.format("%.2f%%", toleranceThreshold)
        + ", metrics="
        + jniMetrics.size()
        + '}';
  }

  /** Represents a single performance metric measurement. */
  public static final class PerformanceMetric {
    private final String name;
    private final double value;
    private final String unit;
    private final double standardDeviation;

    /**
     * Creates a new performance metric.
     *
     * @param name metric name
     * @param value measured value
     * @param unit measurement unit
     * @param standardDeviation standard deviation of measurements
     */
    public PerformanceMetric(
        final String name, final double value, final String unit, final double standardDeviation) {
      this.name = Objects.requireNonNull(name, "name");
      this.value = value;
      this.unit = Objects.requireNonNull(unit, "unit");
      this.standardDeviation = standardDeviation;
    }

    public String getName() {
      return name;
    }

    public double getValue() {
      return value;
    }

    public String getUnit() {
      return unit;
    }

    public double getStandardDeviation() {
      return standardDeviation;
    }

    @Override
    public String toString() {
      return String.format("%.3f ± %.3f %s", value, standardDeviation, unit);
    }
  }
}

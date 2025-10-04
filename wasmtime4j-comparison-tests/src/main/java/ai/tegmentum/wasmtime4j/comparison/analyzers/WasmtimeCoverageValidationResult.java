package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.List;

/**
 * Result of validating coverage targets against the 95% Wasmtime test suite coverage and 100% API
 * compatibility goals.
 *
 * @since 1.0.0
 */
public final class WasmtimeCoverageValidationResult {
  private final boolean meets95PercentTarget;
  private final boolean is100PercentCompatible;
  private final double actualCoveragePercentage;
  private final double actualCompatibilityScore;
  private final List<WasmtimeRecommendation> recommendations;

  /**
   * Creates a coverage validation result with target compliance metrics.
   *
   * @param meets95PercentTarget true if 95% coverage target is met
   * @param is100PercentCompatible true if 100% compatibility is achieved
   * @param actualCoveragePercentage actual coverage percentage achieved
   * @param actualCompatibilityScore actual compatibility score achieved
   * @param recommendations list of recommendations for improvement
   */
  public WasmtimeCoverageValidationResult(
      final boolean meets95PercentTarget,
      final boolean is100PercentCompatible,
      final double actualCoveragePercentage,
      final double actualCompatibilityScore,
      final List<WasmtimeRecommendation> recommendations) {
    this.meets95PercentTarget = meets95PercentTarget;
    this.is100PercentCompatible = is100PercentCompatible;
    this.actualCoveragePercentage = actualCoveragePercentage;
    this.actualCompatibilityScore = actualCompatibilityScore;
    this.recommendations = List.copyOf(recommendations);
  }

  public boolean meets95PercentTarget() {
    return meets95PercentTarget;
  }

  public boolean is100PercentCompatible() {
    return is100PercentCompatible;
  }

  public double getActualCoveragePercentage() {
    return actualCoveragePercentage;
  }

  public double getActualCompatibilityScore() {
    return actualCompatibilityScore;
  }

  public List<WasmtimeRecommendation> getRecommendations() {
    return recommendations;
  }

  public boolean isFullyCompliant() {
    return meets95PercentTarget && is100PercentCompatible;
  }

  public double getCoverageGap() {
    return Math.max(0.0, 95.0 - actualCoveragePercentage);
  }

  public double getCompatibilityGap() {
    return Math.max(0.0, 100.0 - actualCompatibilityScore);
  }
}

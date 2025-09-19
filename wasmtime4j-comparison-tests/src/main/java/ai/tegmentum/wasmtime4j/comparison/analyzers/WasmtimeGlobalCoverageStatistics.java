package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Global coverage statistics specifically for Wasmtime test suite analysis.
 *
 * @since 1.0.0
 */
public final class WasmtimeGlobalCoverageStatistics {
  private final int totalWasmtimeFeatures;
  private final int coveredWasmtimeFeatures;
  private final double overallCoveragePercentage;
  private final int totalAnalyzedTests;
  private final int totalCategories;
  private final double compatibilityScore;

  public WasmtimeGlobalCoverageStatistics(
      final int totalWasmtimeFeatures,
      final int coveredWasmtimeFeatures,
      final double overallCoveragePercentage,
      final int totalAnalyzedTests,
      final int totalCategories,
      final double compatibilityScore) {
    this.totalWasmtimeFeatures = totalWasmtimeFeatures;
    this.coveredWasmtimeFeatures = coveredWasmtimeFeatures;
    this.overallCoveragePercentage = overallCoveragePercentage;
    this.totalAnalyzedTests = totalAnalyzedTests;
    this.totalCategories = totalCategories;
    this.compatibilityScore = compatibilityScore;
  }

  public int getTotalWasmtimeFeatures() {
    return totalWasmtimeFeatures;
  }

  public int getCoveredWasmtimeFeatures() {
    return coveredWasmtimeFeatures;
  }

  public double getOverallCoveragePercentage() {
    return overallCoveragePercentage;
  }

  public int getTotalAnalyzedTests() {
    return totalAnalyzedTests;
  }

  public int getTotalCategories() {
    return totalCategories;
  }

  public double getCompatibilityScore() {
    return compatibilityScore;
  }

  public boolean meets95PercentTarget() {
    return overallCoveragePercentage >= 95.0;
  }

  public boolean isFullyCompatible() {
    return compatibilityScore >= 95.0;
  }
}

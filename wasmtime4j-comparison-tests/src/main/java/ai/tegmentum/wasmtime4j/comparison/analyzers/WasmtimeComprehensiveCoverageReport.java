package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive Wasmtime-specific coverage report.
 *
 * @since 1.0.0
 */
public final class WasmtimeComprehensiveCoverageReport {
  private final Map<String, Double> wasmtimeCategoryCompleteness;
  private final List<String> uncoveredWasmtimeFeatures;
  private final Map<RuntimeType, Double> wasmtimeCompatibilityScores;
  private final List<WasmtimeRecommendation> wasmtimeRecommendations;
  private final WasmtimeTestSuiteCoverage testSuiteCoverage;
  private final int totalAnalyzedTests;
  private final Instant generatedAt;

  /**
   * Creates a comprehensive coverage report with complete Wasmtime analysis results.
   *
   * @param wasmtimeCategoryCompleteness category-specific coverage completeness scores
   * @param uncoveredWasmtimeFeatures list of Wasmtime features not yet covered
   * @param wasmtimeCompatibilityScores compatibility scores by runtime type
   * @param wasmtimeRecommendations recommendations for improving coverage
   * @param testSuiteCoverage test suite coverage metrics
   * @param totalAnalyzedTests total number of tests analyzed
   * @param generatedAt timestamp when report was generated
   */
  public WasmtimeComprehensiveCoverageReport(
      final Map<String, Double> wasmtimeCategoryCompleteness,
      final List<String> uncoveredWasmtimeFeatures,
      final Map<RuntimeType, Double> wasmtimeCompatibilityScores,
      final List<WasmtimeRecommendation> wasmtimeRecommendations,
      final WasmtimeTestSuiteCoverage testSuiteCoverage,
      final int totalAnalyzedTests,
      final Instant generatedAt) {
    this.wasmtimeCategoryCompleteness = Map.copyOf(wasmtimeCategoryCompleteness);
    this.uncoveredWasmtimeFeatures = List.copyOf(uncoveredWasmtimeFeatures);
    this.wasmtimeCompatibilityScores = Map.copyOf(wasmtimeCompatibilityScores);
    this.wasmtimeRecommendations = List.copyOf(wasmtimeRecommendations);
    this.testSuiteCoverage = testSuiteCoverage;
    this.totalAnalyzedTests = totalAnalyzedTests;
    this.generatedAt = generatedAt;
  }

  public Map<String, Double> getWasmtimeCategoryCompleteness() {
    return wasmtimeCategoryCompleteness;
  }

  public List<String> getUncoveredWasmtimeFeatures() {
    return uncoveredWasmtimeFeatures;
  }

  public Map<RuntimeType, Double> getWasmtimeCompatibilityScores() {
    return wasmtimeCompatibilityScores;
  }

  public List<WasmtimeRecommendation> getWasmtimeRecommendations() {
    return wasmtimeRecommendations;
  }

  public WasmtimeTestSuiteCoverage getTestSuiteCoverage() {
    return testSuiteCoverage;
  }

  public int getTotalAnalyzedTests() {
    return totalAnalyzedTests;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public boolean meets95PercentTarget() {
    return testSuiteCoverage.getCoveragePercentage() >= 95.0;
  }

  public boolean isFullyCompatible() {
    return wasmtimeCompatibilityScores.values().stream().allMatch(score -> score >= 95.0);
  }
}

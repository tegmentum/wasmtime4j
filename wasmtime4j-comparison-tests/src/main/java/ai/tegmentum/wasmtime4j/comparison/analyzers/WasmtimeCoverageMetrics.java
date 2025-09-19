package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Map;

/**
 * Wasmtime-specific coverage metrics extending base coverage analysis.
 *
 * @since 1.0.0
 */
public final class WasmtimeCoverageMetrics {
  private final int totalDetectedFeatures;
  private final double overallWasmtimeCoverage;
  private final Map<RuntimeType, Double> runtimeWasmtimeCoverage;
  private final double compatibilityScore;
  private final double testSuiteCoveragePercentage;

  public WasmtimeCoverageMetrics(
      final int totalDetectedFeatures,
      final double overallWasmtimeCoverage,
      final Map<RuntimeType, Double> runtimeWasmtimeCoverage,
      final double compatibilityScore,
      final double testSuiteCoveragePercentage) {
    this.totalDetectedFeatures = totalDetectedFeatures;
    this.overallWasmtimeCoverage = overallWasmtimeCoverage;
    this.runtimeWasmtimeCoverage = Map.copyOf(runtimeWasmtimeCoverage);
    this.compatibilityScore = compatibilityScore;
    this.testSuiteCoveragePercentage = testSuiteCoveragePercentage;
  }

  public int getTotalDetectedFeatures() {
    return totalDetectedFeatures;
  }

  public double getOverallWasmtimeCoverage() {
    return overallWasmtimeCoverage;
  }

  public Map<RuntimeType, Double> getRuntimeWasmtimeCoverage() {
    return runtimeWasmtimeCoverage;
  }

  public double getCompatibilityScore() {
    return compatibilityScore;
  }

  public double getTestSuiteCoveragePercentage() {
    return testSuiteCoveragePercentage;
  }

  public boolean meets95PercentTarget() {
    return overallWasmtimeCoverage >= 95.0;
  }

  public boolean isCompatible() {
    return compatibilityScore >= 95.0;
  }
}

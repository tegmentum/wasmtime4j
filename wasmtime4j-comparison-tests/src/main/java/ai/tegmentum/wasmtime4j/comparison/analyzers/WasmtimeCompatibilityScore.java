package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compatibility score for wasmtime4j implementations against native Wasmtime behavior.
 *
 * @since 1.0.0
 */
public final class WasmtimeCompatibilityScore {
  private final String testName;
  private final Map<RuntimeType, Double> runtimeScores;
  private final Map<String, Double> featureScores;
  private final double overallScore;
  private final Set<RuntimeType> failedRuntimes;

  private WasmtimeCompatibilityScore(
      final String testName,
      final Map<RuntimeType, Double> runtimeScores,
      final Map<String, Double> featureScores,
      final double overallScore,
      final Set<RuntimeType> failedRuntimes) {
    this.testName = testName;
    this.runtimeScores = Map.copyOf(runtimeScores);
    this.featureScores = Map.copyOf(featureScores);
    this.overallScore = overallScore;
    this.failedRuntimes = Set.copyOf(failedRuntimes);
  }

  public String getTestName() {
    return testName;
  }

  public Map<RuntimeType, Double> getRuntimeScores() {
    return runtimeScores;
  }

  public Map<String, Double> getFeatureScores() {
    return featureScores;
  }

  public double getOverallScore() {
    return overallScore;
  }

  public Set<RuntimeType> getFailedRuntimes() {
    return failedRuntimes;
  }

  public double getFeatureScore(final String feature) {
    return featureScores.getOrDefault(feature, 0.0);
  }

  public double getRuntimeScore(final RuntimeType runtime) {
    return runtimeScores.getOrDefault(runtime, 0.0);
  }

  public boolean isCompatible() {
    return overallScore >= 95.0;
  }

  public static final class Builder {
    private final String testName;
    private Map<RuntimeType, Double> runtimeScores;
    private Map<String, Double> featureScores;
    private double overallScore;
    private Set<RuntimeType> failedRuntimes;

    public Builder(final String testName) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    }

    public Builder runtimeScores(final Map<RuntimeType, Double> runtimeScores) {
      this.runtimeScores = runtimeScores;
      return this;
    }

    public Builder featureScores(final Map<String, Double> featureScores) {
      this.featureScores = featureScores;
      return this;
    }

    public Builder overallScore(final double overallScore) {
      this.overallScore = overallScore;
      return this;
    }

    public Builder failedRuntimes(final Set<RuntimeType> failedRuntimes) {
      this.failedRuntimes = failedRuntimes;
      return this;
    }

    public WasmtimeCompatibilityScore build() {
      return new WasmtimeCompatibilityScore(
          testName, runtimeScores, featureScores, overallScore, failedRuntimes);
    }
  }
}

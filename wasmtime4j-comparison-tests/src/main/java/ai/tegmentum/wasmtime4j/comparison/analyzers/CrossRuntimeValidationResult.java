package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of comprehensive cross-runtime validation analysis comparing execution results between JNI
 * and Panama implementations with strict tolerance requirements for production readiness.
 *
 * @since 1.0.0
 */
public final class CrossRuntimeValidationResult {
  private final double consistencyScore;
  private final JniPanamaEquivalenceResult jniPanamaEquivalence;
  private final PerformanceConsistencyResult performanceConsistency;
  private final boolean crossRuntimeEquivalent;

  private CrossRuntimeValidationResult(final Builder builder) {
    this.consistencyScore = builder.consistencyScore;
    this.jniPanamaEquivalence = builder.jniPanamaEquivalence;
    this.performanceConsistency = builder.performanceConsistency;
    this.crossRuntimeEquivalent = builder.crossRuntimeEquivalent;
  }

  public double getConsistencyScore() {
    return consistencyScore;
  }

  public Optional<JniPanamaEquivalenceResult> getJniPanamaEquivalence() {
    return Optional.ofNullable(jniPanamaEquivalence);
  }

  public Optional<PerformanceConsistencyResult> getPerformanceConsistency() {
    return Optional.ofNullable(performanceConsistency);
  }

  public boolean isCrossRuntimeEquivalent() {
    return crossRuntimeEquivalent;
  }

  /**
   * Checks if the cross-runtime validation meets the >98% consistency requirement.
   *
   * @return true if consistency meets production requirements
   */
  public boolean meetsProductionRequirements() {
    return consistencyScore >= 0.98 && crossRuntimeEquivalent;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CrossRuntimeValidationResult that = (CrossRuntimeValidationResult) obj;
    return Double.compare(that.consistencyScore, consistencyScore) == 0
        && crossRuntimeEquivalent == that.crossRuntimeEquivalent
        && Objects.equals(jniPanamaEquivalence, that.jniPanamaEquivalence)
        && Objects.equals(performanceConsistency, that.performanceConsistency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        consistencyScore, jniPanamaEquivalence, performanceConsistency, crossRuntimeEquivalent);
  }

  @Override
  public String toString() {
    return "CrossRuntimeValidationResult{"
        + "consistencyScore="
        + String.format("%.2f", consistencyScore)
        + ", crossRuntimeEquivalent="
        + crossRuntimeEquivalent
        + ", meetsRequirements="
        + meetsProductionRequirements()
        + '}';
  }

  /** Builder for CrossRuntimeValidationResult. */
  public static final class Builder {
    private double consistencyScore = 1.0;
    private JniPanamaEquivalenceResult jniPanamaEquivalence;
    private PerformanceConsistencyResult performanceConsistency;
    private boolean crossRuntimeEquivalent = true;

    /**
     * Sets the consistency score for cross-runtime validation.
     *
     * @param consistencyScore consistency score between 0.0 and 1.0
     * @return this builder
     * @throws IllegalArgumentException if score is not between 0.0 and 1.0
     */
    public Builder consistencyScore(final double consistencyScore) {
      if (consistencyScore < 0.0 || consistencyScore > 1.0) {
        throw new IllegalArgumentException("consistencyScore must be between 0.0 and 1.0");
      }
      this.consistencyScore = consistencyScore;
      return this;
    }

    public Builder jniPanamaEquivalence(final JniPanamaEquivalenceResult jniPanamaEquivalence) {
      this.jniPanamaEquivalence = jniPanamaEquivalence;
      return this;
    }

    public Builder performanceConsistency(
        final PerformanceConsistencyResult performanceConsistency) {
      this.performanceConsistency = performanceConsistency;
      return this;
    }

    public Builder crossRuntimeEquivalent(final boolean crossRuntimeEquivalent) {
      this.crossRuntimeEquivalent = crossRuntimeEquivalent;
      return this;
    }

    public CrossRuntimeValidationResult build() {
      return new CrossRuntimeValidationResult(this);
    }
  }
}

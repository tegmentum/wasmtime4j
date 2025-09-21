package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of detailed equivalence analysis between JNI and Panama implementations, tracking
 * execution status, return values, exceptions, and performance characteristics to ensure zero
 * functional discrepancies.
 *
 * @since 1.0.0
 */
public final class JniPanamaEquivalenceResult {
  private final boolean statusEquivalent;
  private final boolean valueEquivalent;
  private final boolean exceptionEquivalent;
  private final boolean performanceEquivalent;
  private final ValueComparisonResult valueComparisonDetails;
  private final ExceptionComparisonResult exceptionComparisonDetails;
  private final TimingComparisonResult timingComparisonDetails;
  private final double overallEquivalenceScore;

  private JniPanamaEquivalenceResult(final Builder builder) {
    this.statusEquivalent = builder.statusEquivalent;
    this.valueEquivalent = builder.valueEquivalent;
    this.exceptionEquivalent = builder.exceptionEquivalent;
    this.performanceEquivalent = builder.performanceEquivalent;
    this.valueComparisonDetails = builder.valueComparisonDetails;
    this.exceptionComparisonDetails = builder.exceptionComparisonDetails;
    this.timingComparisonDetails = builder.timingComparisonDetails;
    this.overallEquivalenceScore = calculateOverallEquivalenceScore();
  }

  public boolean isStatusEquivalent() {
    return statusEquivalent;
  }

  public boolean isValueEquivalent() {
    return valueEquivalent;
  }

  public boolean isExceptionEquivalent() {
    return exceptionEquivalent;
  }

  public boolean isPerformanceEquivalent() {
    return performanceEquivalent;
  }

  public Optional<ValueComparisonResult> getValueComparisonDetails() {
    return Optional.ofNullable(valueComparisonDetails);
  }

  public Optional<ExceptionComparisonResult> getExceptionComparisonDetails() {
    return Optional.ofNullable(exceptionComparisonDetails);
  }

  public Optional<TimingComparisonResult> getTimingComparisonDetails() {
    return Optional.ofNullable(timingComparisonDetails);
  }

  public double getOverallEquivalenceScore() {
    return overallEquivalenceScore;
  }

  /**
   * Checks if JNI and Panama implementations are fully equivalent with zero discrepancies.
   *
   * @return true if implementations are equivalent
   */
  public boolean isFullyEquivalent() {
    return statusEquivalent && valueEquivalent && exceptionEquivalent && performanceEquivalent;
  }

  /**
   * Checks if equivalence meets the zero discrepancy requirement for production readiness.
   *
   * @return true if zero discrepancy requirement is met
   */
  public boolean meetsZeroDiscrepancyRequirement() {
    return overallEquivalenceScore >= 0.98 && statusEquivalent;
  }

  /** Calculates overall equivalence score based on component scores. */
  private double calculateOverallEquivalenceScore() {
    double score = 0.0;
    int componentCount = 0;

    // Status equivalence (critical requirement)
    if (statusEquivalent) {
      score += 0.4; // 40% weight
    }
    componentCount++;

    // Value equivalence (critical for functional consistency)
    if (valueEquivalent) {
      score += 0.3; // 30% weight
    }
    componentCount++;

    // Exception equivalence (important for error handling consistency)
    if (exceptionEquivalent) {
      score += 0.2; // 20% weight
    }
    componentCount++;

    // Performance equivalence (important for production characteristics)
    if (performanceEquivalent) {
      score += 0.1; // 10% weight
    }
    componentCount++;

    return score; // Already weighted, no need to divide
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final JniPanamaEquivalenceResult that = (JniPanamaEquivalenceResult) obj;
    return statusEquivalent == that.statusEquivalent
        && valueEquivalent == that.valueEquivalent
        && exceptionEquivalent == that.exceptionEquivalent
        && performanceEquivalent == that.performanceEquivalent
        && Double.compare(that.overallEquivalenceScore, overallEquivalenceScore) == 0
        && Objects.equals(valueComparisonDetails, that.valueComparisonDetails)
        && Objects.equals(exceptionComparisonDetails, that.exceptionComparisonDetails)
        && Objects.equals(timingComparisonDetails, that.timingComparisonDetails);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        statusEquivalent,
        valueEquivalent,
        exceptionEquivalent,
        performanceEquivalent,
        valueComparisonDetails,
        exceptionComparisonDetails,
        timingComparisonDetails,
        overallEquivalenceScore);
  }

  @Override
  public String toString() {
    return "JniPanamaEquivalenceResult{"
        + "fullyEquivalent="
        + isFullyEquivalent()
        + ", score="
        + String.format("%.2f", overallEquivalenceScore)
        + ", statusEquivalent="
        + statusEquivalent
        + ", valueEquivalent="
        + valueEquivalent
        + ", exceptionEquivalent="
        + exceptionEquivalent
        + ", performanceEquivalent="
        + performanceEquivalent
        + '}';
  }

  /** Builder for JniPanamaEquivalenceResult. */
  public static final class Builder {
    private boolean statusEquivalent = false;
    private boolean valueEquivalent = false;
    private boolean exceptionEquivalent = false;
    private boolean performanceEquivalent = false;
    private ValueComparisonResult valueComparisonDetails;
    private ExceptionComparisonResult exceptionComparisonDetails;
    private TimingComparisonResult timingComparisonDetails;

    public Builder statusEquivalent(final boolean statusEquivalent) {
      this.statusEquivalent = statusEquivalent;
      return this;
    }

    public Builder valueEquivalent(final boolean valueEquivalent) {
      this.valueEquivalent = valueEquivalent;
      return this;
    }

    public Builder exceptionEquivalent(final boolean exceptionEquivalent) {
      this.exceptionEquivalent = exceptionEquivalent;
      return this;
    }

    public Builder performanceEquivalent(final boolean performanceEquivalent) {
      this.performanceEquivalent = performanceEquivalent;
      return this;
    }

    public Builder valueComparisonDetails(final ValueComparisonResult valueComparisonDetails) {
      this.valueComparisonDetails = valueComparisonDetails;
      return this;
    }

    public Builder exceptionComparisonDetails(
        final ExceptionComparisonResult exceptionComparisonDetails) {
      this.exceptionComparisonDetails = exceptionComparisonDetails;
      return this;
    }

    public Builder timingComparisonDetails(final TimingComparisonResult timingComparisonDetails) {
      this.timingComparisonDetails = timingComparisonDetails;
      return this;
    }

    public JniPanamaEquivalenceResult build() {
      return new JniPanamaEquivalenceResult(this);
    }
  }
}

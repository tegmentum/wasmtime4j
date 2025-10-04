package ai.tegmentum.wasmtime4j.comparison.runners;

import java.util.Objects;

/**
 * Overall result of cross-runtime validation analysis, aggregating metrics from all test suites to
 * provide comprehensive assessment of behavioral consistency and production readiness.
 *
 * @since 1.0.0
 */
public final class OverallValidationResult {
  private final int totalTests;
  private final int totalSuites;
  private final double overallConsistencyScore;
  private final int productionReadyTests;
  private final boolean meetsProductionRequirements;
  private final boolean achievesZeroDiscrepancy;
  private final ValidationOutcome outcome;

  private OverallValidationResult(final Builder builder) {
    this.totalTests = builder.totalTests;
    this.totalSuites = builder.totalSuites;
    this.overallConsistencyScore = builder.overallConsistencyScore;
    this.productionReadyTests = builder.productionReadyTests;
    this.meetsProductionRequirements = builder.meetsProductionRequirements;
    this.achievesZeroDiscrepancy = builder.achievesZeroDiscrepancy;
    this.outcome = determineOutcome();
  }

  public int getTotalTests() {
    return totalTests;
  }

  public int getTotalSuites() {
    return totalSuites;
  }

  public double getOverallConsistencyScore() {
    return overallConsistencyScore;
  }

  public int getProductionReadyTests() {
    return productionReadyTests;
  }

  public boolean meetsProductionRequirements() {
    return meetsProductionRequirements;
  }

  public boolean achievesZeroDiscrepancy() {
    return achievesZeroDiscrepancy;
  }

  public ValidationOutcome getOutcome() {
    return outcome;
  }

  /**
   * Gets the production readiness rate as a percentage.
   *
   * @return production readiness percentage (0-100)
   */
  public double getProductionReadinessPercentage() {
    return totalTests > 0 ? (double) productionReadyTests / totalTests * 100 : 0.0;
  }

  /**
   * Gets the consistency score as a percentage.
   *
   * @return consistency percentage (0-100)
   */
  public double getConsistencyPercentage() {
    return overallConsistencyScore * 100;
  }

  /** Determines the overall validation outcome based on metrics. */
  private ValidationOutcome determineOutcome() {
    if (achievesZeroDiscrepancy) {
      return ValidationOutcome.ZERO_DISCREPANCY_ACHIEVED;
    } else if (meetsProductionRequirements) {
      return ValidationOutcome.PRODUCTION_READY;
    } else if (overallConsistencyScore >= 0.85) {
      return ValidationOutcome.ACCEPTABLE_CONSISTENCY;
    } else if (overallConsistencyScore >= 0.70) {
      return ValidationOutcome.POOR_CONSISTENCY;
    } else {
      return ValidationOutcome.VALIDATION_FAILED;
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

    final OverallValidationResult that = (OverallValidationResult) obj;
    return totalTests == that.totalTests
        && totalSuites == that.totalSuites
        && Double.compare(that.overallConsistencyScore, overallConsistencyScore) == 0
        && productionReadyTests == that.productionReadyTests
        && meetsProductionRequirements == that.meetsProductionRequirements
        && achievesZeroDiscrepancy == that.achievesZeroDiscrepancy
        && outcome == that.outcome;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        totalTests,
        totalSuites,
        overallConsistencyScore,
        productionReadyTests,
        meetsProductionRequirements,
        achievesZeroDiscrepancy,
        outcome);
  }

  @Override
  public String toString() {
    return String.format(
        "OverallValidationResult{outcome=%s, consistency=%.1f%%, productionReady=%d/%d,"
            + " zeroDiscrepancy=%s}",
        outcome,
        getConsistencyPercentage(),
        productionReadyTests,
        totalTests,
        achievesZeroDiscrepancy);
  }

  /** Possible validation outcomes. */
  public enum ValidationOutcome {
    ZERO_DISCREPANCY_ACHIEVED,
    PRODUCTION_READY,
    ACCEPTABLE_CONSISTENCY,
    POOR_CONSISTENCY,
    VALIDATION_FAILED
  }

  /** Builder for OverallValidationResult. */
  public static final class Builder {
    private int totalTests = 0;
    private int totalSuites = 0;
    private double overallConsistencyScore = 0.0;
    private int productionReadyTests = 0;
    private boolean meetsProductionRequirements = false;
    private boolean achievesZeroDiscrepancy = false;

    /**
     * Sets the total number of tests executed.
     *
     * @param totalTests total test count (must be non-negative)
     * @return this builder instance
     */
    public Builder totalTests(final int totalTests) {
      if (totalTests < 0) {
        throw new IllegalArgumentException("totalTests must be non-negative");
      }
      this.totalTests = totalTests;
      return this;
    }

    /**
     * Sets the total number of test suites executed.
     *
     * @param totalSuites total suite count (must be non-negative)
     * @return this builder instance
     */
    public Builder totalSuites(final int totalSuites) {
      if (totalSuites < 0) {
        throw new IllegalArgumentException("totalSuites must be non-negative");
      }
      this.totalSuites = totalSuites;
      return this;
    }

    /**
     * Sets the overall consistency score.
     *
     * @param overallConsistencyScore consistency score between 0.0 and 1.0
     * @return this builder instance
     */
    public Builder overallConsistencyScore(final double overallConsistencyScore) {
      if (overallConsistencyScore < 0.0 || overallConsistencyScore > 1.0) {
        throw new IllegalArgumentException("overallConsistencyScore must be between 0.0 and 1.0");
      }
      this.overallConsistencyScore = overallConsistencyScore;
      return this;
    }

    /**
     * Sets the number of tests meeting production readiness criteria.
     *
     * @param productionReadyTests production ready test count (must be non-negative)
     * @return this builder instance
     */
    public Builder productionReadyTests(final int productionReadyTests) {
      if (productionReadyTests < 0) {
        throw new IllegalArgumentException("productionReadyTests must be non-negative");
      }
      this.productionReadyTests = productionReadyTests;
      return this;
    }

    public Builder meetsProductionRequirements(final boolean meetsProductionRequirements) {
      this.meetsProductionRequirements = meetsProductionRequirements;
      return this;
    }

    /**
     * Sets whether zero discrepancy requirement was achieved.
     *
     * @param achievesZeroDiscrepancy true if zero discrepancy achieved
     * @return this builder instance
     */
    public Builder achievesZeroDiscrepancy(final boolean achievesZeroDiscrepancy) {
      this.achievesZeroDiscrepancy = achievesZeroDiscrepancy;
      return this;
    }

    /**
     * Builds the overall validation result instance.
     *
     * @return new overall validation result
     */
    public OverallValidationResult build() {
      if (productionReadyTests > totalTests) {
        throw new IllegalStateException("productionReadyTests cannot exceed totalTests");
      }
      return new OverallValidationResult(this);
    }
  }
}

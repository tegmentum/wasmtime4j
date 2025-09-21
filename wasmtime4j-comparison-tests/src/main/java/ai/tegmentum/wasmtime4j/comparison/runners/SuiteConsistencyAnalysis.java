package ai.tegmentum.wasmtime4j.comparison.runners;

import java.util.Objects;

/**
 * Analysis of consistency metrics across all tests within a test suite, providing
 * suite-level behavioral assessment and production readiness evaluation.
 *
 * @since 1.0.0
 */
public final class SuiteConsistencyAnalysis {
  private final double averageConsistencyScore;
  private final double productionReadinessRate;
  private final int totalTests;
  private final int productionReadyTests;
  private final ConsistencyGrade grade;

  private SuiteConsistencyAnalysis(final Builder builder) {
    this.averageConsistencyScore = builder.averageConsistencyScore;
    this.productionReadinessRate = builder.productionReadinessRate;
    this.totalTests = builder.totalTests;
    this.productionReadyTests = builder.productionReadyTests;
    this.grade = calculateGrade();
  }

  public double getAverageConsistencyScore() {
    return averageConsistencyScore;
  }

  public double getProductionReadinessRate() {
    return productionReadinessRate;
  }

  public int getTotalTests() {
    return totalTests;
  }

  public int getProductionReadyTests() {
    return productionReadyTests;
  }

  public ConsistencyGrade getGrade() {
    return grade;
  }

  /**
   * Checks if the suite meets production requirements (>98% consistency).
   *
   * @return true if suite meets production requirements
   */
  public boolean meetsProductionRequirements() {
    return averageConsistencyScore >= 0.98 && productionReadinessRate >= 0.98;
  }

  /**
   * Checks if the suite achieves zero discrepancy (100% production ready).
   *
   * @return true if zero discrepancy is achieved
   */
  public boolean achievesZeroDiscrepancy() {
    return productionReadinessRate >= 1.0 && averageConsistencyScore >= 0.999;
  }

  /** Calculates the overall consistency grade for the suite. */
  private ConsistencyGrade calculateGrade() {
    if (averageConsistencyScore >= 0.98 && productionReadinessRate >= 0.98) {
      return ConsistencyGrade.EXCELLENT;
    } else if (averageConsistencyScore >= 0.95 && productionReadinessRate >= 0.95) {
      return ConsistencyGrade.GOOD;
    } else if (averageConsistencyScore >= 0.85 && productionReadinessRate >= 0.85) {
      return ConsistencyGrade.ACCEPTABLE;
    } else if (averageConsistencyScore >= 0.70 && productionReadinessRate >= 0.70) {
      return ConsistencyGrade.POOR;
    } else {
      return ConsistencyGrade.FAILING;
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

    final SuiteConsistencyAnalysis that = (SuiteConsistencyAnalysis) obj;
    return Double.compare(that.averageConsistencyScore, averageConsistencyScore) == 0
        && Double.compare(that.productionReadinessRate, productionReadinessRate) == 0
        && totalTests == that.totalTests
        && productionReadyTests == that.productionReadyTests
        && grade == that.grade;
  }

  @Override
  public int hashCode() {
    return Objects.hash(averageConsistencyScore, productionReadinessRate, totalTests, productionReadyTests, grade);
  }

  @Override
  public String toString() {
    return String.format(
        "SuiteConsistencyAnalysis{grade=%s, avgConsistency=%.2f, productionReady=%d/%d (%.1f%%)}",
        grade, averageConsistencyScore, productionReadyTests, totalTests, productionReadinessRate * 100);
  }

  /** Consistency grades for suite-level assessment. */
  public enum ConsistencyGrade {
    EXCELLENT,
    GOOD,
    ACCEPTABLE,
    POOR,
    FAILING
  }

  /** Builder for SuiteConsistencyAnalysis. */
  public static final class Builder {
    private double averageConsistencyScore = 0.0;
    private double productionReadinessRate = 0.0;
    private int totalTests = 0;
    private int productionReadyTests = 0;

    public Builder averageConsistencyScore(final double averageConsistencyScore) {
      if (averageConsistencyScore < 0.0 || averageConsistencyScore > 1.0) {
        throw new IllegalArgumentException("averageConsistencyScore must be between 0.0 and 1.0");
      }
      this.averageConsistencyScore = averageConsistencyScore;
      return this;
    }

    public Builder productionReadinessRate(final double productionReadinessRate) {
      if (productionReadinessRate < 0.0 || productionReadinessRate > 1.0) {
        throw new IllegalArgumentException("productionReadinessRate must be between 0.0 and 1.0");
      }
      this.productionReadinessRate = productionReadinessRate;
      return this;
    }

    public Builder totalTests(final int totalTests) {
      if (totalTests < 0) {
        throw new IllegalArgumentException("totalTests must be non-negative");
      }
      this.totalTests = totalTests;
      return this;
    }

    public Builder productionReadyTests(final int productionReadyTests) {
      if (productionReadyTests < 0) {
        throw new IllegalArgumentException("productionReadyTests must be non-negative");
      }
      this.productionReadyTests = productionReadyTests;
      return this;
    }

    public SuiteConsistencyAnalysis build() {
      if (productionReadyTests > totalTests) {
        throw new IllegalStateException("productionReadyTests cannot exceed totalTests");
      }
      return new SuiteConsistencyAnalysis(this);
    }
  }
}
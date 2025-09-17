package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;

/**
 * Result of a value comparison operation, providing detailed information about the equivalence and
 * exact matching status of two values, along with comparison metadata.
 *
 * @since 1.0.0
 */
public final class ValueComparisonResult {
  private final boolean equivalent;
  private final boolean exactMatch;
  private final ComparisonType comparisonType;
  private final String details;
  private final double confidenceScore;

  private ValueComparisonResult(final Builder builder) {
    this.equivalent = builder.equivalent;
    this.exactMatch = builder.exactMatch;
    this.comparisonType = builder.comparisonType;
    this.details = builder.details;
    this.confidenceScore = builder.confidenceScore;
  }

  /**
   * Checks if the values are considered equivalent within tolerance levels.
   *
   * @return true if values are equivalent
   */
  public boolean isEquivalent() {
    return equivalent;
  }

  /**
   * Checks if the values are exactly equal.
   *
   * @return true if values are exactly equal
   */
  public boolean isExactMatch() {
    return exactMatch;
  }

  /**
   * Gets the type of comparison that was performed.
   *
   * @return the comparison type
   */
  public ComparisonType getComparisonType() {
    return comparisonType;
  }

  /**
   * Gets detailed information about the comparison.
   *
   * @return comparison details
   */
  public String getDetails() {
    return details;
  }

  /**
   * Gets the confidence score for this comparison result (0.0 to 1.0).
   *
   * @return confidence score
   */
  public double getConfidenceScore() {
    return confidenceScore;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ValueComparisonResult that = (ValueComparisonResult) obj;
    return equivalent == that.equivalent
        && exactMatch == that.exactMatch
        && Double.compare(that.confidenceScore, confidenceScore) == 0
        && comparisonType == that.comparisonType
        && Objects.equals(details, that.details);
  }

  @Override
  public int hashCode() {
    return Objects.hash(equivalent, exactMatch, comparisonType, details, confidenceScore);
  }

  @Override
  public String toString() {
    return "ValueComparisonResult{"
        + "equivalent="
        + equivalent
        + ", exactMatch="
        + exactMatch
        + ", comparisonType="
        + comparisonType
        + ", confidenceScore="
        + confidenceScore
        + '}';
  }

  /** Builder for ValueComparisonResult. */
  public static final class Builder {
    private boolean equivalent = false;
    private boolean exactMatch = false;
    private ComparisonType comparisonType = ComparisonType.UNKNOWN;
    private String details = "";
    private double confidenceScore = 1.0;

    public Builder equivalent(final boolean equivalent) {
      this.equivalent = equivalent;
      return this;
    }

    public Builder exactMatch(final boolean exactMatch) {
      this.exactMatch = exactMatch;
      return this;
    }

    public Builder comparisonType(final ComparisonType comparisonType) {
      this.comparisonType = Objects.requireNonNull(comparisonType, "comparisonType cannot be null");
      return this;
    }

    public Builder details(final String details) {
      this.details = Objects.requireNonNull(details, "details cannot be null");
      return this;
    }

    /**
     * Sets the confidence score for this comparison result.
     *
     * @param confidenceScore confidence score between 0.0 and 1.0
     * @return this builder
     */
    public Builder confidenceScore(final double confidenceScore) {
      if (confidenceScore < 0.0 || confidenceScore > 1.0) {
        throw new IllegalArgumentException("confidenceScore must be between 0.0 and 1.0");
      }
      this.confidenceScore = confidenceScore;
      return this;
    }

    public ValueComparisonResult build() {
      return new ValueComparisonResult(this);
    }
  }
}

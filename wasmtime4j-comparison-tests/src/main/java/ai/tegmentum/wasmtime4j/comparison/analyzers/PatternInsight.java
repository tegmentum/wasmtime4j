package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.List;
import java.util.Objects;

/**
 * Insight based on observed patterns across multiple tests.
 *
 * @since 1.0.0
 */
public final class PatternInsight {
  private final PatternType patternType;
  private final String description;
  private final int occurrenceCount;
  private final double confidenceScore;
  private final List<String> recommendations;

  /**
   * Creates a new pattern insight.
   *
   * @param patternType the type of pattern identified
   * @param description the description of the pattern
   * @param occurrenceCount the number of occurrences
   * @param confidenceScore the confidence score (0.0 to 1.0)
   * @param recommendations the list of recommendations
   */
  public PatternInsight(
      final PatternType patternType,
      final String description,
      final int occurrenceCount,
      final double confidenceScore,
      final List<String> recommendations) {
    this.patternType = Objects.requireNonNull(patternType, "patternType cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.occurrenceCount = occurrenceCount;
    this.confidenceScore = confidenceScore;
    this.recommendations = List.copyOf(recommendations);
  }

  /**
   * Gets the pattern type.
   *
   * @return the pattern type
   */
  public PatternType getPatternType() {
    return patternType;
  }

  /**
   * Gets the pattern description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the occurrence count.
   *
   * @return the number of occurrences
   */
  public int getOccurrenceCount() {
    return occurrenceCount;
  }

  /**
   * Gets the confidence score.
   *
   * @return the confidence score
   */
  public double getConfidenceScore() {
    return confidenceScore;
  }

  /**
   * Gets the recommendations.
   *
   * @return the list of recommendations
   */
  public List<String> getRecommendations() {
    return recommendations;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final PatternInsight that = (PatternInsight) obj;
    return occurrenceCount == that.occurrenceCount
        && Double.compare(that.confidenceScore, confidenceScore) == 0
        && patternType == that.patternType
        && Objects.equals(description, that.description)
        && Objects.equals(recommendations, that.recommendations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        patternType, description, occurrenceCount, confidenceScore, recommendations);
  }

  @Override
  public String toString() {
    return "PatternInsight{"
        + "type="
        + patternType
        + ", occurrences="
        + occurrenceCount
        + ", confidence="
        + String.format("%.2f", confidenceScore)
        + '}';
  }
}

package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.List;
import java.util.Objects;

/**
 * Optimization recommendation with priority and confidence.
 *
 * @since 1.0.0
 */
public final class OptimizationRecommendation {
  private final PatternType patternType;
  private final String description;
  private final List<String> actionItems;
  private final OptimizationPriority priority;
  private final double confidenceScore;

  /**
   * Constructs a new OptimizationRecommendation with the specified properties.
   *
   * @param patternType the pattern type
   * @param description the recommendation description
   * @param actionItems the list of action items
   * @param priority the optimization priority
   * @param confidenceScore the confidence score
   */
  public OptimizationRecommendation(
      final PatternType patternType,
      final String description,
      final List<String> actionItems,
      final OptimizationPriority priority,
      final double confidenceScore) {
    this.patternType = Objects.requireNonNull(patternType, "patternType cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.actionItems = List.copyOf(actionItems);
    this.priority = Objects.requireNonNull(priority, "priority cannot be null");
    this.confidenceScore = confidenceScore;
  }

  public PatternType getPatternType() {
    return patternType;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getActionItems() {
    return actionItems;
  }

  public OptimizationPriority getPriority() {
    return priority;
  }

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
    final OptimizationRecommendation that = (OptimizationRecommendation) obj;
    return Double.compare(that.confidenceScore, confidenceScore) == 0
        && patternType == that.patternType
        && Objects.equals(description, that.description)
        && Objects.equals(actionItems, that.actionItems)
        && priority == that.priority;
  }

  @Override
  public int hashCode() {
    return Objects.hash(patternType, description, actionItems, priority, confidenceScore);
  }

  @Override
  public String toString() {
    return "OptimizationRecommendation{"
        + "type="
        + patternType
        + ", priority="
        + priority
        + ", confidence="
        + String.format("%.2f", confidenceScore)
        + '}';
  }
}

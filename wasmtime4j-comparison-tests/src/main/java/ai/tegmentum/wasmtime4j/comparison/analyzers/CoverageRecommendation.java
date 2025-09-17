package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;
import java.util.Set;

/**
 * Actionable recommendation for improving test coverage.
 *
 * @since 1.0.0
 */
public final class CoverageRecommendation {
  private final RecommendationType type;
  private final String description;
  private final RecommendationPriority priority;
  private final Set<String> affectedAreas;

  /**
   * Constructs a new CoverageRecommendation with the specified properties.
   *
   * @param type the type of recommendation
   * @param description the recommendation description
   * @param priority the priority level
   * @param affectedAreas the set of affected areas
   */
  public CoverageRecommendation(
      final RecommendationType type,
      final String description,
      final RecommendationPriority priority,
      final Set<String> affectedAreas) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.priority = Objects.requireNonNull(priority, "priority cannot be null");
    this.affectedAreas = Set.copyOf(affectedAreas);
  }

  public RecommendationType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public RecommendationPriority getPriority() {
    return priority;
  }

  public Set<String> getAffectedAreas() {
    return affectedAreas;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CoverageRecommendation that = (CoverageRecommendation) obj;
    return type == that.type
        && Objects.equals(description, that.description)
        && priority == that.priority
        && Objects.equals(affectedAreas, that.affectedAreas);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, description, priority, affectedAreas);
  }

  @Override
  public String toString() {
    return "CoverageRecommendation{"
        + "type="
        + type
        + ", priority="
        + priority
        + ", areas="
        + affectedAreas.size()
        + '}';
  }
}

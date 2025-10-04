package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Set;

/**
 * Wasmtime-specific recommendation for improving coverage or compatibility.
 *
 * @since 1.0.0
 */
public final class WasmtimeRecommendation {
  private final WasmtimeRecommendationType type;
  private final String description;
  private final RecommendationPriority priority;
  private final Set<String> targetAreas;

  /**
   * Creates a Wasmtime-specific recommendation for improving coverage or compatibility.
   *
   * @param type type of recommendation
   * @param description detailed description of the recommendation
   * @param priority priority level for implementation
   * @param targetAreas specific areas targeted by this recommendation
   */
  public WasmtimeRecommendation(
      final WasmtimeRecommendationType type,
      final String description,
      final RecommendationPriority priority,
      final Set<String> targetAreas) {
    this.type = type;
    this.description = description;
    this.priority = priority;
    this.targetAreas = Set.copyOf(targetAreas);
  }

  public WasmtimeRecommendationType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public RecommendationPriority getPriority() {
    return priority;
  }

  public Set<String> getTargetAreas() {
    return targetAreas;
  }
}

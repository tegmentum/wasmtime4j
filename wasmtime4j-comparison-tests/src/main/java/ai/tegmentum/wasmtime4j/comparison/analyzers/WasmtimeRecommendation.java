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

/** Types of Wasmtime-specific recommendations. */
enum WasmtimeRecommendationType {
  INCREASE_CATEGORY_COVERAGE,
  IMPROVE_COMPATIBILITY,
  ADD_TEST_CASES,
  FIX_RUNTIME_ISSUES,
  ENHANCE_FEATURE_SUPPORT
}

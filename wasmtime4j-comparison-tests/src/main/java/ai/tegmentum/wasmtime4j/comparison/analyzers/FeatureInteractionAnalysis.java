package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Analysis of feature interactions and combinations.
 *
 * @since 1.0.0
 */
public final class FeatureInteractionAnalysis {
  private final Map<String, Set<String>> featureCombinations;
  private final List<String> problematicInteractions;
  private final double interactionComplexity;

  /**
   * Creates a new feature interaction analysis.
   *
   * @param featureCombinations the feature combinations
   * @param problematicInteractions the problematic interactions
   * @param interactionComplexity the interaction complexity score
   */
  public FeatureInteractionAnalysis(
      final Map<String, Set<String>> featureCombinations,
      final List<String> problematicInteractions,
      final double interactionComplexity) {
    this.featureCombinations = Map.copyOf(featureCombinations);
    this.problematicInteractions = List.copyOf(problematicInteractions);
    this.interactionComplexity = interactionComplexity;
  }

  /**
   * Gets the feature combinations.
   *
   * @return the feature combinations map
   */
  public Map<String, Set<String>> getFeatureCombinations() {
    return featureCombinations;
  }

  /**
   * Gets the problematic interactions.
   *
   * @return the list of problematic interactions
   */
  public List<String> getProblematicInteractions() {
    return problematicInteractions;
  }

  /**
   * Gets the interaction complexity score.
   *
   * @return the interaction complexity
   */
  public double getInteractionComplexity() {
    return interactionComplexity;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final FeatureInteractionAnalysis that = (FeatureInteractionAnalysis) obj;
    return Double.compare(that.interactionComplexity, interactionComplexity) == 0
        && Objects.equals(featureCombinations, that.featureCombinations)
        && Objects.equals(problematicInteractions, that.problematicInteractions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(featureCombinations, problematicInteractions, interactionComplexity);
  }

  @Override
  public String toString() {
    return "FeatureInteractionAnalysis{"
        + "combinations="
        + featureCombinations.size()
        + ", problematic="
        + problematicInteractions.size()
        + ", complexity="
        + String.format("%.2f", interactionComplexity)
        + '}';
  }
}

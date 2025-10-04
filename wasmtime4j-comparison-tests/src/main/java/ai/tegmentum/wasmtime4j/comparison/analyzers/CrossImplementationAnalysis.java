package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analysis of feature coverage across different runtime implementations (JNI vs Panama).
 *
 * @since 1.0.0
 */
public final class CrossImplementationAnalysis {
  private final Map<RuntimeType, Set<String>> runtimeFeatures;
  private final List<String> consistentFeatures;
  private final List<String> inconsistentFeatures;
  private final double crossImplementationScore;

  /**
   * Creates a new cross-implementation analysis instance.
   *
   * @param runtimeFeatures features available in each runtime
   * @param consistentFeatures features with consistent behavior across runtimes
   * @param inconsistentFeatures features with inconsistent behavior across runtimes
   * @param crossImplementationScore overall cross-implementation consistency score
   */
  public CrossImplementationAnalysis(
      final Map<RuntimeType, Set<String>> runtimeFeatures,
      final List<String> consistentFeatures,
      final List<String> inconsistentFeatures,
      final double crossImplementationScore) {
    this.runtimeFeatures = Map.copyOf(runtimeFeatures);
    this.consistentFeatures = List.copyOf(consistentFeatures);
    this.inconsistentFeatures = List.copyOf(inconsistentFeatures);
    this.crossImplementationScore = crossImplementationScore;
  }

  public Map<RuntimeType, Set<String>> getRuntimeFeatures() {
    return runtimeFeatures;
  }

  public List<String> getConsistentFeatures() {
    return consistentFeatures;
  }

  public List<String> getInconsistentFeatures() {
    return inconsistentFeatures;
  }

  public double getCrossImplementationScore() {
    return crossImplementationScore;
  }

  public boolean hasConsistentBehavior() {
    return crossImplementationScore >= 95.0;
  }
}

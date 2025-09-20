package ai.tegmentum.wasmtime4j.performance;

import java.util.Map;

/**
 * Optimization hint providing guidance for JIT compiler optimization decisions.
 *
 * <p>OptimizationHint contains information about expected usage patterns, performance
 * characteristics, and optimization preferences for specific functions or code patterns. These
 * hints help the JIT compiler make better optimization decisions.
 *
 * @since 1.0.0
 */
public interface OptimizationHint {

  /**
   * Gets the type of optimization hint.
   *
   * @return hint type
   */
  OptimizationHintType getType();

  /**
   * Gets the priority level for this optimization hint.
   *
   * @return priority level (1 = highest, 5 = lowest)
   */
  int getPriority();

  /**
   * Gets the confidence level for this hint.
   *
   * <p>Higher confidence levels indicate more reliable profiling data or stronger evidence
   * supporting the hint.
   *
   * @return confidence level from 0.0 (no confidence) to 1.0 (full confidence)
   */
  double getConfidence();

  /**
   * Gets the expected execution frequency for the target code.
   *
   * @return execution frequency hint
   */
  ExecutionFrequency getExecutionFrequency();

  /**
   * Gets the expected call pattern for function calls.
   *
   * @return call pattern hint, or null if not applicable
   */
  CallPattern getCallPattern();

  /**
   * Gets memory access pattern hints.
   *
   * @return memory access pattern, or null if not applicable
   */
  MemoryAccessPattern getMemoryAccessPattern();

  /**
   * Gets branch prediction hints.
   *
   * @return branch prediction hint, or null if not applicable
   */
  BranchPredictionHint getBranchPredictionHint();

  /**
   * Gets vectorization hints.
   *
   * @return vectorization hint, or null if not applicable
   */
  VectorizationHint getVectorizationHint();

  /**
   * Gets the suggested optimization level for the target code.
   *
   * @return suggested optimization level
   */
  OptimizationLevel getSuggestedOptimizationLevel();

  /**
   * Gets specific optimization strategies to prefer.
   *
   * @return preferred optimization strategies
   */
  java.util.Set<OptimizationStrategy> getPreferredStrategies();

  /**
   * Gets optimization strategies to avoid.
   *
   * @return strategies to avoid
   */
  java.util.Set<OptimizationStrategy> getAvoidedStrategies();

  /**
   * Gets additional metadata associated with this hint.
   *
   * @return hint metadata
   */
  Map<String, Object> getMetadata();

  /**
   * Gets the source of this optimization hint.
   *
   * @return hint source (profiling, static analysis, manual, etc.)
   */
  HintSource getSource();

  /**
   * Gets the validity duration for this hint.
   *
   * <p>Some hints may become stale over time as execution patterns change.
   *
   * @return validity duration, or null if hint doesn't expire
   */
  java.time.Duration getValidityDuration();

  /**
   * Gets the timestamp when this hint was created.
   *
   * @return creation timestamp
   */
  java.time.Instant getCreationTime();

  /**
   * Checks if this hint is still valid based on its age and validity duration.
   *
   * @return true if the hint is still valid
   */
  boolean isValid();

  /**
   * Gets a human-readable description of this optimization hint.
   *
   * @return hint description
   */
  String getDescription();

  /**
   * Combines this hint with another hint to create a merged hint.
   *
   * <p>Merging helps consolidate multiple hints for the same target, with appropriate confidence
   * and priority adjustments.
   *
   * @param other the other hint to merge with
   * @return merged optimization hint
   * @throws IllegalArgumentException if hints cannot be merged
   */
  OptimizationHint merge(final OptimizationHint other);
}

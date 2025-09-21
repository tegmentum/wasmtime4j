package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Map;
import java.util.Objects;

/**
 * Result of side effect analysis across runtime implementations, tracking memory state changes,
 * system state modifications, and other observable effects to ensure behavioral consistency.
 *
 * @since 1.0.0
 */
public final class SideEffectAnalysisResult {
  private final Map<RuntimeType, MemoryStateChange> memoryStateChanges;
  private final Map<RuntimeType, SystemStateChange> systemStateChanges;
  private final double consistencyScore;
  private final boolean sideEffectsConsistent;

  private SideEffectAnalysisResult(final Builder builder) {
    this.memoryStateChanges = Map.copyOf(builder.memoryStateChanges);
    this.systemStateChanges = Map.copyOf(builder.systemStateChanges);
    this.consistencyScore = builder.consistencyScore;
    this.sideEffectsConsistent = builder.sideEffectsConsistent;
  }

  public Map<RuntimeType, MemoryStateChange> getMemoryStateChanges() {
    return memoryStateChanges;
  }

  public Map<RuntimeType, SystemStateChange> getSystemStateChanges() {
    return systemStateChanges;
  }

  public double getConsistencyScore() {
    return consistencyScore;
  }

  public boolean areSideEffectsConsistent() {
    return sideEffectsConsistent;
  }

  /**
   * Checks if side effects meet the zero discrepancy requirement.
   *
   * @return true if side effects are sufficiently consistent
   */
  public boolean meetsZeroDiscrepancyRequirement() {
    return consistencyScore >= 0.98 && sideEffectsConsistent;
  }

  /**
   * Gets the maximum memory variance ratio across runtimes.
   *
   * @return maximum memory variance ratio
   */
  public double getMaxMemoryVarianceRatio() {
    if (memoryStateChanges.size() < 2) {
      return 1.0;
    }

    final long maxHeap =
        memoryStateChanges.values().stream()
            .mapToLong(MemoryStateChange::getHeapUsed)
            .max()
            .orElse(0);

    final long minHeap =
        memoryStateChanges.values().stream()
            .mapToLong(MemoryStateChange::getHeapUsed)
            .min()
            .orElse(0);

    return minHeap > 0 ? (double) maxHeap / minHeap : 1.0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final SideEffectAnalysisResult that = (SideEffectAnalysisResult) obj;
    return Double.compare(that.consistencyScore, consistencyScore) == 0
        && sideEffectsConsistent == that.sideEffectsConsistent
        && Objects.equals(memoryStateChanges, that.memoryStateChanges)
        && Objects.equals(systemStateChanges, that.systemStateChanges);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        memoryStateChanges, systemStateChanges, consistencyScore, sideEffectsConsistent);
  }

  @Override
  public String toString() {
    return "SideEffectAnalysisResult{"
        + "consistent="
        + sideEffectsConsistent
        + ", consistencyScore="
        + String.format("%.2f", consistencyScore)
        + ", memoryVarianceRatio="
        + String.format("%.2f", getMaxMemoryVarianceRatio())
        + ", meetsRequirements="
        + meetsZeroDiscrepancyRequirement()
        + '}';
  }

  /** Builder for SideEffectAnalysisResult. */
  public static final class Builder {
    private Map<RuntimeType, MemoryStateChange> memoryStateChanges = Map.of();
    private Map<RuntimeType, SystemStateChange> systemStateChanges = Map.of();
    private double consistencyScore = 1.0;
    private boolean sideEffectsConsistent = true;

    public Builder memoryStateChanges(
        final Map<RuntimeType, MemoryStateChange> memoryStateChanges) {
      this.memoryStateChanges =
          Objects.requireNonNull(memoryStateChanges, "memoryStateChanges cannot be null");
      return this;
    }

    public Builder systemStateChanges(
        final Map<RuntimeType, SystemStateChange> systemStateChanges) {
      this.systemStateChanges =
          Objects.requireNonNull(systemStateChanges, "systemStateChanges cannot be null");
      return this;
    }

    public Builder consistencyScore(final double consistencyScore) {
      if (consistencyScore < 0.0 || consistencyScore > 1.0) {
        throw new IllegalArgumentException("consistencyScore must be between 0.0 and 1.0");
      }
      this.consistencyScore = consistencyScore;
      return this;
    }

    public Builder sideEffectsConsistent(final boolean sideEffectsConsistent) {
      this.sideEffectsConsistent = sideEffectsConsistent;
      return this;
    }

    public SideEffectAnalysisResult build() {
      return new SideEffectAnalysisResult(this);
    }
  }
}

package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.time.Duration;
import java.util.Objects;

/**
 * Represents memory state changes observed during test execution, tracking heap usage, non-heap
 * usage, and timing characteristics for cross-runtime consistency analysis.
 *
 * @since 1.0.0
 */
public final class MemoryStateChange {
  private final long heapUsed;
  private final long nonHeapUsed;
  private final Duration executionDuration;
  private final long totalMemoryFootprint;

  /**
   * Creates a new memory state change with the specified memory characteristics.
   *
   * @param heapUsed heap memory used in bytes
   * @param nonHeapUsed non-heap memory used in bytes
   * @param executionDuration duration of execution when memory was measured
   */
  public MemoryStateChange(
      final long heapUsed, final long nonHeapUsed, final Duration executionDuration) {
    if (heapUsed < 0) {
      throw new IllegalArgumentException("heapUsed must be non-negative");
    }
    if (nonHeapUsed < 0) {
      throw new IllegalArgumentException("nonHeapUsed must be non-negative");
    }

    this.heapUsed = heapUsed;
    this.nonHeapUsed = nonHeapUsed;
    this.executionDuration =
        Objects.requireNonNull(executionDuration, "executionDuration cannot be null");
    this.totalMemoryFootprint = heapUsed + nonHeapUsed;
  }

  public long getHeapUsed() {
    return heapUsed;
  }

  public long getNonHeapUsed() {
    return nonHeapUsed;
  }

  public Duration getExecutionDuration() {
    return executionDuration;
  }

  public long getTotalMemoryFootprint() {
    return totalMemoryFootprint;
  }

  /**
   * Calculates memory efficiency as bytes per millisecond.
   *
   * @return memory efficiency ratio
   */
  public double getMemoryEfficiency() {
    final long durationMs = executionDuration.toMillis();
    return durationMs > 0 ? (double) totalMemoryFootprint / durationMs : 0.0;
  }

  /**
   * Compares this memory state change with another for consistency analysis.
   *
   * @param other the other memory state change to compare with
   * @return comparison result with ratio and consistency metrics
   */
  public MemoryComparisonMetrics compareWith(final MemoryStateChange other) {
    Objects.requireNonNull(other, "other cannot be null");

    final double heapRatio =
        this.heapUsed > 0 && other.heapUsed > 0
            ? Math.max(this.heapUsed, other.heapUsed)
                / (double) Math.min(this.heapUsed, other.heapUsed)
            : 1.0;

    final double nonHeapRatio =
        this.nonHeapUsed > 0 && other.nonHeapUsed > 0
            ? Math.max(this.nonHeapUsed, other.nonHeapUsed)
                / (double) Math.min(this.nonHeapUsed, other.nonHeapUsed)
            : 1.0;

    final double totalRatio =
        this.totalMemoryFootprint > 0 && other.totalMemoryFootprint > 0
            ? Math.max(this.totalMemoryFootprint, other.totalMemoryFootprint)
                / (double) Math.min(this.totalMemoryFootprint, other.totalMemoryFootprint)
            : 1.0;

    return new MemoryComparisonMetrics(heapRatio, nonHeapRatio, totalRatio);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final MemoryStateChange that = (MemoryStateChange) obj;
    return heapUsed == that.heapUsed
        && nonHeapUsed == that.nonHeapUsed
        && totalMemoryFootprint == that.totalMemoryFootprint
        && Objects.equals(executionDuration, that.executionDuration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(heapUsed, nonHeapUsed, executionDuration, totalMemoryFootprint);
  }

  @Override
  public String toString() {
    return "MemoryStateChange{"
        + "heapUsed="
        + heapUsed
        + ", nonHeapUsed="
        + nonHeapUsed
        + ", totalFootprint="
        + totalMemoryFootprint
        + ", duration="
        + executionDuration.toMillis()
        + "ms"
        + ", efficiency="
        + String.format("%.2f", getMemoryEfficiency())
        + '}';
  }

  /** Memory comparison metrics for consistency analysis. */
  public static final class MemoryComparisonMetrics {
    private final double heapRatio;
    private final double nonHeapRatio;
    private final double totalRatio;

    private MemoryComparisonMetrics(
        final double heapRatio, final double nonHeapRatio, final double totalRatio) {
      this.heapRatio = heapRatio;
      this.nonHeapRatio = nonHeapRatio;
      this.totalRatio = totalRatio;
    }

    public double getHeapRatio() {
      return heapRatio;
    }

    public double getNonHeapRatio() {
      return nonHeapRatio;
    }

    public double getTotalRatio() {
      return totalRatio;
    }

    /**
     * Checks if memory usage is considered consistent within acceptable variance.
     *
     * @return true if memory usage is consistent
     */
    public boolean isConsistent() {
      // Memory is consistent if all ratios are within 1.5x variance
      return heapRatio <= 1.5 && nonHeapRatio <= 1.5 && totalRatio <= 1.5;
    }

    @Override
    public String toString() {
      return "MemoryComparisonMetrics{"
          + "heapRatio="
          + String.format("%.2f", heapRatio)
          + ", nonHeapRatio="
          + String.format("%.2f", nonHeapRatio)
          + ", totalRatio="
          + String.format("%.2f", totalRatio)
          + ", consistent="
          + isConsistent()
          + '}';
    }
  }
}

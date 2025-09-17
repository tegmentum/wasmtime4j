package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;

/**
 * Result of comparing memory usage between two executions.
 *
 * @since 1.0.0
 */
public final class MemoryComparisonResult {
  private final long heapDifference;
  private final long nonHeapDifference;
  private final boolean heapWithinTolerance;
  private final boolean nonHeapWithinTolerance;

  /**
   * Constructs a new MemoryComparisonResult with the specified memory data.
   *
   * @param heapDifference the difference in heap memory usage
   * @param nonHeapDifference the difference in non-heap memory usage
   * @param heapWithinTolerance whether heap usage is within tolerance
   * @param nonHeapWithinTolerance whether non-heap usage is within tolerance
   */
  public MemoryComparisonResult(
      final long heapDifference,
      final long nonHeapDifference,
      final boolean heapWithinTolerance,
      final boolean nonHeapWithinTolerance) {
    this.heapDifference = heapDifference;
    this.nonHeapDifference = nonHeapDifference;
    this.heapWithinTolerance = heapWithinTolerance;
    this.nonHeapWithinTolerance = nonHeapWithinTolerance;
  }

  public long getHeapDifference() {
    return heapDifference;
  }

  public long getNonHeapDifference() {
    return nonHeapDifference;
  }

  public boolean isHeapWithinTolerance() {
    return heapWithinTolerance;
  }

  public boolean isNonHeapWithinTolerance() {
    return nonHeapWithinTolerance;
  }

  public boolean isWithinTolerance() {
    return heapWithinTolerance && nonHeapWithinTolerance;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final MemoryComparisonResult that = (MemoryComparisonResult) obj;
    return heapDifference == that.heapDifference
        && nonHeapDifference == that.nonHeapDifference
        && heapWithinTolerance == that.heapWithinTolerance
        && nonHeapWithinTolerance == that.nonHeapWithinTolerance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        heapDifference, nonHeapDifference, heapWithinTolerance, nonHeapWithinTolerance);
  }

  @Override
  public String toString() {
    return "MemoryComparisonResult{"
        + "heapDiff="
        + heapDifference
        + "b"
        + ", nonHeapDiff="
        + nonHeapDifference
        + "b"
        + ", withinTolerance="
        + isWithinTolerance()
        + '}';
  }
}

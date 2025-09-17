package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Objects;

/**
 * Represents a pairwise comparison between two runtimes.
 *
 * @since 1.0.0
 */
public final class RuntimeComparison {
  private final RuntimeType runtime1;
  private final RuntimeType runtime2;
  private final ComparisonResult comparisonResult;

  /**
   * Constructs a new RuntimeComparison between two runtimes.
   *
   * @param runtime1 the first runtime
   * @param runtime2 the second runtime
   * @param comparisonResult the comparison result
   */
  public RuntimeComparison(
      final RuntimeType runtime1,
      final RuntimeType runtime2,
      final ComparisonResult comparisonResult) {
    this.runtime1 = Objects.requireNonNull(runtime1, "runtime1 cannot be null");
    this.runtime2 = Objects.requireNonNull(runtime2, "runtime2 cannot be null");
    this.comparisonResult =
        Objects.requireNonNull(comparisonResult, "comparisonResult cannot be null");
  }

  public RuntimeType getRuntime1() {
    return runtime1;
  }

  public RuntimeType getRuntime2() {
    return runtime2;
  }

  public ComparisonResult getComparisonResult() {
    return comparisonResult;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final RuntimeComparison that = (RuntimeComparison) obj;
    return runtime1 == that.runtime1
        && runtime2 == that.runtime2
        && Objects.equals(comparisonResult, that.comparisonResult);
  }

  @Override
  public int hashCode() {
    return Objects.hash(runtime1, runtime2, comparisonResult);
  }

  @Override
  public String toString() {
    return "RuntimeComparison{"
        + runtime1
        + " vs "
        + runtime2
        + ", score="
        + comparisonResult.getOverallScore()
        + '}';
  }
}

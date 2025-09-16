package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Detailed result of comparing test execution between two specific WebAssembly runtimes. Provides
 * granular analysis of execution status, return values, exceptions, timing, and memory usage
 * differences.
 *
 * @since 1.0.0
 */
public final class ComparisonResult {
  private final RuntimeType runtime1;
  private final RuntimeType runtime2;
  private final boolean statusMatch;
  private final ValueComparisonResult valueComparison;
  private final ExceptionComparisonResult exceptionComparison;
  private final TimingComparisonResult timingComparison;
  private final MemoryComparisonResult memoryComparison;
  private final double overallScore;

  private ComparisonResult(final Builder builder) {
    this.runtime1 = builder.runtime1;
    this.runtime2 = builder.runtime2;
    this.statusMatch = builder.statusMatch;
    this.valueComparison = builder.valueComparison;
    this.exceptionComparison = builder.exceptionComparison;
    this.timingComparison = builder.timingComparison;
    this.memoryComparison = builder.memoryComparison;
    this.overallScore = calculateOverallScore();
  }

  public RuntimeType getRuntime1() {
    return runtime1;
  }

  public RuntimeType getRuntime2() {
    return runtime2;
  }

  public boolean isStatusMatch() {
    return statusMatch;
  }

  public Optional<ValueComparisonResult> getValueComparison() {
    return Optional.ofNullable(valueComparison);
  }

  public Optional<ExceptionComparisonResult> getExceptionComparison() {
    return Optional.ofNullable(exceptionComparison);
  }

  public Optional<TimingComparisonResult> getTimingComparison() {
    return Optional.ofNullable(timingComparison);
  }

  public Optional<MemoryComparisonResult> getMemoryComparison() {
    return Optional.ofNullable(memoryComparison);
  }

  public double getOverallScore() {
    return overallScore;
  }

  /**
   * Checks if the comparison indicates the runtimes are equivalent.
   *
   * @return true if runtimes are equivalent
   */
  public boolean isEquivalent() {
    return overallScore >= 0.95; // 95% threshold for equivalence
  }

  /** Calculates the overall comparison score (0.0 to 1.0). */
  private double calculateOverallScore() {
    double score = 0.0;
    int componentCount = 0;

    // Execution status (most important)
    if (statusMatch) {
      score += 0.4; // 40% weight
    }
    componentCount++;

    // Value comparison (if applicable)
    if (valueComparison != null) {
      score += valueComparison.isEquivalent() ? 0.3 : 0.0; // 30% weight
      componentCount++;
    }

    // Exception comparison (if applicable)
    if (exceptionComparison != null) {
      score += exceptionComparison.getScore() * 0.2; // 20% weight
      componentCount++;
    }

    // Timing comparison (if applicable)
    if (timingComparison != null) {
      score += timingComparison.isWithinTolerance() ? 0.1 : 0.0; // 10% weight
      componentCount++;
    }

    // Memory comparison (bonus, doesn't affect base score)
    if (memoryComparison != null && memoryComparison.isWithinTolerance()) {
      score += 0.05; // 5% bonus
    }

    return Math.min(1.0, score); // Cap at 1.0
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ComparisonResult that = (ComparisonResult) obj;
    return statusMatch == that.statusMatch
        && Double.compare(that.overallScore, overallScore) == 0
        && runtime1 == that.runtime1
        && runtime2 == that.runtime2
        && Objects.equals(valueComparison, that.valueComparison)
        && Objects.equals(exceptionComparison, that.exceptionComparison)
        && Objects.equals(timingComparison, that.timingComparison)
        && Objects.equals(memoryComparison, that.memoryComparison);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        runtime1,
        runtime2,
        statusMatch,
        valueComparison,
        exceptionComparison,
        timingComparison,
        memoryComparison,
        overallScore);
  }

  @Override
  public String toString() {
    return "ComparisonResult{"
        + runtime1
        + " vs "
        + runtime2
        + ", score="
        + String.format("%.2f", overallScore)
        + ", statusMatch="
        + statusMatch
        + '}';
  }

  /** Builder for ComparisonResult. */
  public static final class Builder {
    private final RuntimeType runtime1;
    private final RuntimeType runtime2;
    private boolean statusMatch = false;
    private ValueComparisonResult valueComparison;
    private ExceptionComparisonResult exceptionComparison;
    private TimingComparisonResult timingComparison;
    private MemoryComparisonResult memoryComparison;

    public Builder(final RuntimeType runtime1, final RuntimeType runtime2) {
      this.runtime1 = Objects.requireNonNull(runtime1, "runtime1 cannot be null");
      this.runtime2 = Objects.requireNonNull(runtime2, "runtime2 cannot be null");
    }

    public Builder statusMatch(final boolean statusMatch) {
      this.statusMatch = statusMatch;
      return this;
    }

    public Builder valueComparison(final ValueComparisonResult valueComparison) {
      this.valueComparison = valueComparison;
      return this;
    }

    public Builder exceptionComparison(final ExceptionComparisonResult exceptionComparison) {
      this.exceptionComparison = exceptionComparison;
      return this;
    }

    public Builder timingComparison(final TimingComparisonResult timingComparison) {
      this.timingComparison = timingComparison;
      return this;
    }

    public Builder memoryComparison(final MemoryComparisonResult memoryComparison) {
      this.memoryComparison = memoryComparison;
      return this;
    }

    public ComparisonResult build() {
      return new ComparisonResult(this);
    }
  }
}

/** Result of comparing exceptions between two executions. */
final class ExceptionComparisonResult {
  private final boolean typeMatch;
  private final boolean messageMatch;
  private final boolean semanticMatch;
  private final double score;

  public ExceptionComparisonResult(
      final boolean typeMatch,
      final boolean messageMatch,
      final boolean semanticMatch,
      final double score) {
    this.typeMatch = typeMatch;
    this.messageMatch = messageMatch;
    this.semanticMatch = semanticMatch;
    this.score = Math.max(0.0, Math.min(1.0, score));
  }

  public boolean isTypeMatch() {
    return typeMatch;
  }

  public boolean isMessageMatch() {
    return messageMatch;
  }

  public boolean isSemanticMatch() {
    return semanticMatch;
  }

  public double getScore() {
    return score;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ExceptionComparisonResult that = (ExceptionComparisonResult) obj;
    return typeMatch == that.typeMatch
        && messageMatch == that.messageMatch
        && semanticMatch == that.semanticMatch
        && Double.compare(that.score, score) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(typeMatch, messageMatch, semanticMatch, score);
  }

  @Override
  public String toString() {
    return "ExceptionComparisonResult{"
        + "score="
        + String.format("%.2f", score)
        + ", typeMatch="
        + typeMatch
        + ", messageMatch="
        + messageMatch
        + ", semanticMatch="
        + semanticMatch
        + '}';
  }
}

/** Result of comparing execution timing between two executions. */
final class TimingComparisonResult {
  private final Duration difference;
  private final boolean withinTolerance;
  private final double ratio;

  public TimingComparisonResult(
      final Duration difference, final boolean withinTolerance, final double ratio) {
    this.difference = Objects.requireNonNull(difference, "difference cannot be null");
    this.withinTolerance = withinTolerance;
    this.ratio = ratio;
  }

  public Duration getDifference() {
    return difference;
  }

  public boolean isWithinTolerance() {
    return withinTolerance;
  }

  public double getRatio() {
    return ratio;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final TimingComparisonResult that = (TimingComparisonResult) obj;
    return withinTolerance == that.withinTolerance
        && Double.compare(that.ratio, ratio) == 0
        && Objects.equals(difference, that.difference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(difference, withinTolerance, ratio);
  }

  @Override
  public String toString() {
    return "TimingComparisonResult{"
        + "difference="
        + difference.toMillis()
        + "ms"
        + ", withinTolerance="
        + withinTolerance
        + ", ratio="
        + String.format("%.2f", ratio)
        + '}';
  }
}

/** Result of comparing memory usage between two executions. */
final class MemoryComparisonResult {
  private final long heapDifference;
  private final long nonHeapDifference;
  private final boolean heapWithinTolerance;
  private final boolean nonHeapWithinTolerance;

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

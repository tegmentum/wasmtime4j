package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Comprehensive result of behavioral analysis across multiple WebAssembly runtime implementations.
 * Contains detailed comparison results, discrepancy analysis, and overall behavioral assessment.
 *
 * @since 1.0.0
 */
public final class BehavioralAnalysisResult {
  private final String testName;
  private final List<RuntimeComparison> pairwiseComparisons;
  private final List<BehavioralDiscrepancy> discrepancies;
  private final ExecutionPattern executionPattern;
  private final double consistencyScore;
  private final BehavioralVerdict verdict;
  private final Instant analysisTime;

  private BehavioralAnalysisResult(final Builder builder) {
    this.testName = builder.testName;
    this.pairwiseComparisons = Collections.unmodifiableList(builder.pairwiseComparisons);
    this.discrepancies = Collections.unmodifiableList(builder.discrepancies);
    this.executionPattern = builder.executionPattern;
    this.consistencyScore = builder.consistencyScore;
    this.verdict = builder.verdict;
    this.analysisTime = Instant.now();
  }

  public String getTestName() {
    return testName;
  }

  public List<RuntimeComparison> getPairwiseComparisons() {
    return pairwiseComparisons;
  }

  public List<BehavioralDiscrepancy> getDiscrepancies() {
    return discrepancies;
  }

  public ExecutionPattern getExecutionPattern() {
    return executionPattern;
  }

  public double getConsistencyScore() {
    return consistencyScore;
  }

  public BehavioralVerdict getVerdict() {
    return verdict;
  }

  public Instant getAnalysisTime() {
    return analysisTime;
  }

  /**
   * Gets the number of critical discrepancies.
   *
   * @return count of critical discrepancies
   */
  public long getCriticalDiscrepancyCount() {
    return discrepancies.stream().filter(BehavioralDiscrepancy::isCritical).count();
  }

  /**
   * Checks if the analysis indicates behavioral compatibility.
   *
   * @return true if runtimes are behaviorally compatible
   */
  public boolean isCompatible() {
    return verdict == BehavioralVerdict.CONSISTENT
        || verdict == BehavioralVerdict.MOSTLY_CONSISTENT;
  }

  /**
   * Gets a summary report of the analysis.
   *
   * @return formatted summary report
   */
  public String getSummaryReport() {
    return String.format(
        "Behavioral Analysis Summary for %s:%n"
            + "Verdict: %s%n"
            + "Consistency Score: %.2f%n"
            + "Discrepancies: %d total (%d critical)%n"
            + "Pairwise Comparisons: %d%n"
            + "Analysis Time: %s",
        testName,
        verdict,
        consistencyScore,
        discrepancies.size(),
        getCriticalDiscrepancyCount(),
        pairwiseComparisons.size(),
        analysisTime);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final BehavioralAnalysisResult that = (BehavioralAnalysisResult) obj;
    return Double.compare(that.consistencyScore, consistencyScore) == 0
        && Objects.equals(testName, that.testName)
        && Objects.equals(pairwiseComparisons, that.pairwiseComparisons)
        && Objects.equals(discrepancies, that.discrepancies)
        && Objects.equals(executionPattern, that.executionPattern)
        && verdict == that.verdict;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        testName, pairwiseComparisons, discrepancies, executionPattern, consistencyScore, verdict);
  }

  @Override
  public String toString() {
    return "BehavioralAnalysisResult{"
        + "testName='"
        + testName
        + '\''
        + ", verdict="
        + verdict
        + ", consistencyScore="
        + consistencyScore
        + ", discrepancies="
        + discrepancies.size()
        + '}';
  }

  /** Builder for BehavioralAnalysisResult. */
  public static final class Builder {
    private final String testName;
    private List<RuntimeComparison> pairwiseComparisons = Collections.emptyList();
    private List<BehavioralDiscrepancy> discrepancies = Collections.emptyList();
    private ExecutionPattern executionPattern;
    private double consistencyScore = 0.0;
    private BehavioralVerdict verdict = BehavioralVerdict.UNKNOWN;

    public Builder(final String testName) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    }

    public Builder pairwiseComparisons(final List<RuntimeComparison> pairwiseComparisons) {
      this.pairwiseComparisons =
          Objects.requireNonNull(pairwiseComparisons, "pairwiseComparisons cannot be null");
      return this;
    }

    public Builder discrepancies(final List<BehavioralDiscrepancy> discrepancies) {
      this.discrepancies = Objects.requireNonNull(discrepancies, "discrepancies cannot be null");
      return this;
    }

    public Builder executionPattern(final ExecutionPattern executionPattern) {
      this.executionPattern =
          Objects.requireNonNull(executionPattern, "executionPattern cannot be null");
      return this;
    }

    public Builder consistencyScore(final double consistencyScore) {
      if (consistencyScore < 0.0 || consistencyScore > 1.0) {
        throw new IllegalArgumentException("consistencyScore must be between 0.0 and 1.0");
      }
      this.consistencyScore = consistencyScore;
      return this;
    }

    public Builder verdict(final BehavioralVerdict verdict) {
      this.verdict = Objects.requireNonNull(verdict, "verdict cannot be null");
      return this;
    }

    public BehavioralAnalysisResult build() {
      if (executionPattern == null) {
        throw new IllegalStateException("executionPattern must be set");
      }
      return new BehavioralAnalysisResult(this);
    }
  }
}

/** Represents a pairwise comparison between two runtimes. */
final class RuntimeComparison {
  private final RuntimeType runtime1;
  private final RuntimeType runtime2;
  private final ComparisonResult comparisonResult;

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

/** Represents the execution pattern observed across runtimes. */
final class ExecutionPattern {
  private final int successCount;
  private final int failureCount;
  private final int skipCount;
  private final int uniqueReturnValues;
  private final int uniqueExceptionTypes;
  private final double timingVariance;

  public ExecutionPattern(
      final int successCount,
      final int failureCount,
      final int skipCount,
      final int uniqueReturnValues,
      final int uniqueExceptionTypes,
      final double timingVariance) {
    this.successCount = successCount;
    this.failureCount = failureCount;
    this.skipCount = skipCount;
    this.uniqueReturnValues = uniqueReturnValues;
    this.uniqueExceptionTypes = uniqueExceptionTypes;
    this.timingVariance = timingVariance;
  }

  public int getSuccessCount() {
    return successCount;
  }

  public int getFailureCount() {
    return failureCount;
  }

  public int getSkipCount() {
    return skipCount;
  }

  public int getUniqueReturnValues() {
    return uniqueReturnValues;
  }

  public int getUniqueExceptionTypes() {
    return uniqueExceptionTypes;
  }

  public double getTimingVariance() {
    return timingVariance;
  }

  public int getTotalRuntimes() {
    return successCount + failureCount + skipCount;
  }

  public double getSuccessRate() {
    final int total = getTotalRuntimes();
    return total == 0 ? 0.0 : (double) successCount / total;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ExecutionPattern that = (ExecutionPattern) obj;
    return successCount == that.successCount
        && failureCount == that.failureCount
        && skipCount == that.skipCount
        && uniqueReturnValues == that.uniqueReturnValues
        && uniqueExceptionTypes == that.uniqueExceptionTypes
        && Double.compare(that.timingVariance, timingVariance) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        successCount,
        failureCount,
        skipCount,
        uniqueReturnValues,
        uniqueExceptionTypes,
        timingVariance);
  }

  @Override
  public String toString() {
    return "ExecutionPattern{"
        + "success="
        + successCount
        + ", failure="
        + failureCount
        + ", skip="
        + skipCount
        + ", uniqueValues="
        + uniqueReturnValues
        + ", uniqueExceptions="
        + uniqueExceptionTypes
        + ", timingVar="
        + String.format("%.2f", timingVariance)
        + '}';
  }
}

/** Overall behavioral verdict for runtime compatibility. */
enum BehavioralVerdict {
  /** Runtimes behave consistently across all comparisons. */
  CONSISTENT("Consistent behavior"),

  /** Runtimes behave mostly consistently with minor variations. */
  MOSTLY_CONSISTENT("Mostly consistent behavior"),

  /** Runtimes show inconsistent behavior that should be investigated. */
  INCONSISTENT("Inconsistent behavior"),

  /** Runtimes are incompatible and show fundamental differences. */
  INCOMPATIBLE("Incompatible behavior"),

  /** Unable to determine behavioral compatibility. */
  UNKNOWN("Unknown behavior");

  private final String description;

  BehavioralVerdict(final String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}

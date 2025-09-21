package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Comprehensive result of enhanced behavioral analysis across multiple WebAssembly runtime
 * implementations. Contains detailed comparison results, discrepancy analysis, cross-runtime
 * validation, execution path analysis, side effect detection, and overall behavioral assessment.
 *
 * @since 1.0.0
 */
public final class BehavioralAnalysisResult {
  private final String testName;
  private final List<RuntimeComparison> pairwiseComparisons;
  private final List<BehavioralDiscrepancy> discrepancies;
  private final ExecutionPattern executionPattern;
  private final CrossRuntimeValidationResult crossRuntimeValidation;
  private final ExecutionPathValidationResult executionPathValidation;
  private final SideEffectAnalysisResult sideEffectAnalysis;
  private final double consistencyScore;
  private final BehavioralVerdict verdict;
  private final Instant analysisTime;

  private BehavioralAnalysisResult(final Builder builder) {
    this.testName = builder.testName;
    this.pairwiseComparisons = Collections.unmodifiableList(builder.pairwiseComparisons);
    this.discrepancies = Collections.unmodifiableList(builder.discrepancies);
    this.executionPattern = builder.executionPattern;
    this.crossRuntimeValidation = builder.crossRuntimeValidation;
    this.executionPathValidation = builder.executionPathValidation;
    this.sideEffectAnalysis = builder.sideEffectAnalysis;
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

  public CrossRuntimeValidationResult getCrossRuntimeValidation() {
    return crossRuntimeValidation;
  }

  public ExecutionPathValidationResult getExecutionPathValidation() {
    return executionPathValidation;
  }

  public SideEffectAnalysisResult getSideEffectAnalysis() {
    return sideEffectAnalysis;
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
   * Checks if the analysis meets production readiness requirements (>98% consistency).
   *
   * @return true if analysis meets production requirements
   */
  public boolean meetsProductionRequirements() {
    return consistencyScore >= 0.98
        && verdict == BehavioralVerdict.CONSISTENT
        && (crossRuntimeValidation == null || crossRuntimeValidation.meetsProductionRequirements())
        && (executionPathValidation == null
            || executionPathValidation.meetsZeroDivergenceRequirement())
        && (sideEffectAnalysis == null || sideEffectAnalysis.meetsZeroDiscrepancyRequirement());
  }

  /**
   * Checks if zero discrepancy requirement is met across all validation dimensions.
   *
   * @return true if zero discrepancy requirement is satisfied
   */
  public boolean meetsZeroDiscrepancyRequirement() {
    return getCriticalDiscrepancyCount() == 0 && meetsProductionRequirements();
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
    private CrossRuntimeValidationResult crossRuntimeValidation;
    private ExecutionPathValidationResult executionPathValidation;
    private SideEffectAnalysisResult sideEffectAnalysis;
    private double consistencyScore = 0.0;
    private BehavioralVerdict verdict = BehavioralVerdict.UNKNOWN;

    public Builder(final String testName) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    }

    /**
     * Sets the pairwise runtime comparisons.
     *
     * @param pairwiseComparisons the pairwise runtime comparisons
     * @return this builder
     */
    public Builder pairwiseComparisons(final List<RuntimeComparison> pairwiseComparisons) {
      this.pairwiseComparisons =
          Objects.requireNonNull(pairwiseComparisons, "pairwiseComparisons cannot be null");
      return this;
    }

    public Builder discrepancies(final List<BehavioralDiscrepancy> discrepancies) {
      this.discrepancies = Objects.requireNonNull(discrepancies, "discrepancies cannot be null");
      return this;
    }

    /**
     * Sets the execution pattern analysis result.
     *
     * @param executionPattern the execution pattern analysis result
     * @return this builder
     */
    public Builder executionPattern(final ExecutionPattern executionPattern) {
      this.executionPattern =
          Objects.requireNonNull(executionPattern, "executionPattern cannot be null");
      return this;
    }

    /**
     * Sets the consistency score across runtimes.
     *
     * @param consistencyScore the consistency score (0.0 to 1.0)
     * @return this builder
     */
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

    /**
     * Sets the cross-runtime validation result.
     *
     * @param crossRuntimeValidation the cross-runtime validation result
     * @return this builder
     */
    public Builder crossRuntimeValidation(
        final CrossRuntimeValidationResult crossRuntimeValidation) {
      this.crossRuntimeValidation = crossRuntimeValidation;
      return this;
    }

    /**
     * Sets the execution path validation result.
     *
     * @param executionPathValidation the execution path validation result
     * @return this builder
     */
    public Builder executionPathValidation(
        final ExecutionPathValidationResult executionPathValidation) {
      this.executionPathValidation = executionPathValidation;
      return this;
    }

    /**
     * Sets the side effect analysis result.
     *
     * @param sideEffectAnalysis the side effect analysis result
     * @return this builder
     */
    public Builder sideEffectAnalysis(final SideEffectAnalysisResult sideEffectAnalysis) {
      this.sideEffectAnalysis = sideEffectAnalysis;
      return this;
    }

    /**
     * Builds the behavioral analysis result.
     *
     * @return new BehavioralAnalysisResult instance
     * @throws IllegalStateException if executionPattern is not set
     */
    public BehavioralAnalysisResult build() {
      if (executionPattern == null) {
        throw new IllegalStateException("executionPattern must be set");
      }
      return new BehavioralAnalysisResult(this);
    }
  }
}

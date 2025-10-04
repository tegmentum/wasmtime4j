package ai.tegmentum.wasmtime4j.comparison.runners;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalyzer;
import java.util.Map;
import java.util.Objects;

/**
 * Result of executing a cross-runtime test across multiple WebAssembly runtime implementations,
 * containing execution results, behavioral analysis, and consistency metrics.
 *
 * @since 1.0.0
 */
public final class CrossRuntimeTestResult {
  private final String testName;
  private final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults;
  private final BehavioralAnalysisResult behavioralAnalysis;
  private final double consistencyScore;
  private final boolean meetsProductionRequirements;

  /**
   * Creates a new cross-runtime test result with the specified details.
   *
   * @param testName the name of the test
   * @param executionResults the execution results for each runtime
   * @param behavioralAnalysis the behavioral analysis result
   * @param consistencyScore the overall consistency score
   * @param meetsProductionRequirements whether the test meets production requirements
   */
  public CrossRuntimeTestResult(
      final String testName,
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults,
      final BehavioralAnalysisResult behavioralAnalysis,
      final double consistencyScore,
      final boolean meetsProductionRequirements) {
    this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    this.executionResults =
        Map.copyOf(Objects.requireNonNull(executionResults, "executionResults cannot be null"));
    this.behavioralAnalysis =
        Objects.requireNonNull(behavioralAnalysis, "behavioralAnalysis cannot be null");
    this.consistencyScore = consistencyScore;
    this.meetsProductionRequirements = meetsProductionRequirements;
  }

  public String getTestName() {
    return testName;
  }

  public Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> getExecutionResults() {
    return executionResults;
  }

  public BehavioralAnalysisResult getBehavioralAnalysis() {
    return behavioralAnalysis;
  }

  public double getConsistencyScore() {
    return consistencyScore;
  }

  public boolean meetsProductionRequirements() {
    return meetsProductionRequirements;
  }

  /**
   * Checks if the test passed on all runtimes.
   *
   * @return true if all runtimes executed successfully
   */
  public boolean allRuntimesSucceeded() {
    return executionResults.values().stream()
        .allMatch(BehavioralAnalyzer.TestExecutionResult::isSuccessful);
  }

  /**
   * Checks if the test failed on all runtimes.
   *
   * @return true if all runtimes failed
   */
  public boolean allRuntimesFailed() {
    return executionResults.values().stream()
        .noneMatch(BehavioralAnalyzer.TestExecutionResult::isSuccessful);
  }

  /**
   * Gets the number of runtimes that executed successfully.
   *
   * @return the successful runtime count
   */
  public long getSuccessfulRuntimeCount() {
    return executionResults.values().stream()
        .mapToLong(result -> result.isSuccessful() ? 1 : 0)
        .sum();
  }

  /**
   * Checks if the test results indicate zero functional discrepancies.
   *
   * @return true if zero discrepancy requirement is met
   */
  public boolean meetsZeroDiscrepancyRequirement() {
    return behavioralAnalysis.meetsZeroDiscrepancyRequirement();
  }

  /**
   * Gets the number of critical behavioral discrepancies.
   *
   * @return the critical discrepancy count
   */
  public long getCriticalDiscrepancyCount() {
    return behavioralAnalysis.getCriticalDiscrepancyCount();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CrossRuntimeTestResult that = (CrossRuntimeTestResult) obj;
    return Double.compare(that.consistencyScore, consistencyScore) == 0
        && meetsProductionRequirements == that.meetsProductionRequirements
        && Objects.equals(testName, that.testName)
        && Objects.equals(executionResults, that.executionResults)
        && Objects.equals(behavioralAnalysis, that.behavioralAnalysis);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        testName,
        executionResults,
        behavioralAnalysis,
        consistencyScore,
        meetsProductionRequirements);
  }

  @Override
  public String toString() {
    return String.format(
        "CrossRuntimeTestResult{testName='%s', consistencyScore=%.2f, productionReady=%s,"
            + " runtimesSucceeded=%d/%d}",
        testName,
        consistencyScore,
        meetsProductionRequirements,
        getSuccessfulRuntimeCount(),
        executionResults.size());
  }
}

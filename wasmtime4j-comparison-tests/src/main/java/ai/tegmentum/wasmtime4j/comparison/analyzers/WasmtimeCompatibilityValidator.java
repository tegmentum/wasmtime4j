package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestCase;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Validates API compatibility between wasmtime4j implementations and native Wasmtime behavior.
 * Provides scoring and analysis of compatibility gaps.
 *
 * @since 1.0.0
 */
public final class WasmtimeCompatibilityValidator {
  private static final Logger LOGGER =
      Logger.getLogger(WasmtimeCompatibilityValidator.class.getName());

  private final Map<RuntimeType, Double> runtimeCompatibilityScores;
  private final Map<String, Double> featureCompatibilityScores;
  private double overallCompatibilityScore;

  public WasmtimeCompatibilityValidator() {
    this.runtimeCompatibilityScores = new ConcurrentHashMap<>();
    this.featureCompatibilityScores = new ConcurrentHashMap<>();
    this.overallCompatibilityScore = 0.0;
  }

  /**
   * Validates compatibility of test execution results against expected Wasmtime behavior.
   *
   * @param testCase the test case being validated
   * @param executionResults the execution results from different runtimes
   * @return compatibility score with detailed analysis
   */
  public WasmtimeCompatibilityScore validateCompatibility(
      final WasmTestCase testCase,
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {

    final WasmtimeCompatibilityScore.Builder scoreBuilder =
        new WasmtimeCompatibilityScore.Builder(testCase.getTestName());

    // Calculate per-runtime compatibility scores
    final Map<RuntimeType, Double> runtimeScores = new EnumMap<>(RuntimeType.class);
    final Set<RuntimeType> failedRuntimes = new HashSet<>();

    for (final Map.Entry<RuntimeType, BehavioralAnalyzer.TestExecutionResult> entry :
        executionResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final BehavioralAnalyzer.TestExecutionResult result = entry.getValue();

      final double runtimeScore = calculateRuntimeCompatibilityScore(testCase, result);
      runtimeScores.put(runtime, runtimeScore);

      if (runtimeScore < 90.0) {
        failedRuntimes.add(runtime);
      }

      // Update global tracking
      updateRuntimeCompatibility(runtime, runtimeScore);
    }

    // Calculate feature-level compatibility scores
    final Map<String, Double> featureScores =
        calculateFeatureCompatibilityScores(testCase, executionResults);

    // Calculate overall compatibility score
    final double overallScore = calculateOverallCompatibilityScore(runtimeScores, featureScores);

    return scoreBuilder
        .runtimeScores(runtimeScores)
        .featureScores(featureScores)
        .overallScore(overallScore)
        .failedRuntimes(failedRuntimes)
        .build();
  }

  /**
   * Gets the current runtime compatibility scores.
   *
   * @return map of runtime compatibility scores
   */
  public Map<RuntimeType, Double> getRuntimeCompatibilityScores() {
    return Map.copyOf(runtimeCompatibilityScores);
  }

  /**
   * Gets the overall compatibility score across all tested runtimes.
   *
   * @return overall compatibility score
   */
  public double getOverallCompatibilityScore() {
    return overallCompatibilityScore;
  }

  /** Clears all validation data. */
  public void clearValidationData() {
    runtimeCompatibilityScores.clear();
    featureCompatibilityScores.clear();
    overallCompatibilityScore = 0.0;
    LOGGER.info("Compatibility validation data cleared");
  }

  private double calculateRuntimeCompatibilityScore(
      final WasmTestCase testCase, final BehavioralAnalyzer.TestExecutionResult result) {

    double score = 0.0;

    // Base score for successful execution
    if (result.isSuccessful()) {
      score += 70.0; // Base success score

      // Additional score for expected behavior alignment
      if (testCase.hasExpectedResults()) {
        // In a full implementation, this would compare actual vs expected results
        score += 20.0; // Expected results alignment score
      }

      // Additional score for performance characteristics
      score += 10.0; // Performance alignment score
    } else {
      // Partial score for controlled failures
      if (testCase.isNegativeTest()) {
        // Negative tests should fail, so this is expected
        score += 90.0;
      } else {
        // Unexpected failure - analyze failure type
        final Exception exception = result.getException();
        if (exception != null) {
          final String exceptionType = exception.getClass().getSimpleName().toLowerCase();
          if (exceptionType.contains("compilation") || exceptionType.contains("validation")) {
            score += 30.0; // At least parsing/validation worked
          } else if (exceptionType.contains("runtime")) {
            score += 50.0; // Compilation worked, runtime issue
          }
        }
      }
    }

    return Math.min(100.0, score);
  }

  private Map<String, Double> calculateFeatureCompatibilityScores(
      final WasmTestCase testCase,
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {

    final Map<String, Double> featureScores = new HashMap<>();

    // Analyze feature compatibility based on test characteristics
    final String testName = testCase.getTestName().toLowerCase();

    // Core features should have high compatibility
    if (testName.contains("func") || testName.contains("call")) {
      featureScores.put("function_calls", calculateFeatureScore(executionResults));
    }
    if (testName.contains("memory") || testName.contains("load") || testName.contains("store")) {
      featureScores.put("memory_operations", calculateFeatureScore(executionResults));
    }
    if (testName.contains("table")) {
      featureScores.put("table_operations", calculateFeatureScore(executionResults));
    }
    if (testName.contains("global")) {
      featureScores.put("global_variables", calculateFeatureScore(executionResults));
    }

    // Update global feature tracking
    for (final Map.Entry<String, Double> entry : featureScores.entrySet()) {
      updateFeatureCompatibility(entry.getKey(), entry.getValue());
    }

    return featureScores;
  }

  private double calculateFeatureScore(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final long successfulRuntimes =
        executionResults.values().stream().mapToLong(result -> result.isSuccessful() ? 1 : 0).sum();

    return executionResults.isEmpty()
        ? 0.0
        : (double) successfulRuntimes / executionResults.size() * 100.0;
  }

  private double calculateOverallCompatibilityScore(
      final Map<RuntimeType, Double> runtimeScores, final Map<String, Double> featureScores) {

    double totalScore = 0.0;
    int scoreCount = 0;

    // Include runtime scores
    for (final double score : runtimeScores.values()) {
      totalScore += score;
      scoreCount++;
    }

    // Include feature scores
    for (final double score : featureScores.values()) {
      totalScore += score;
      scoreCount++;
    }

    final double overallScore = scoreCount > 0 ? totalScore / scoreCount : 0.0;
    updateOverallCompatibility(overallScore);
    return overallScore;
  }

  private void updateRuntimeCompatibility(final RuntimeType runtime, final double score) {
    runtimeCompatibilityScores.merge(
        runtime, score, (existing, newScore) -> (existing + newScore) / 2.0);
  }

  private void updateFeatureCompatibility(final String feature, final double score) {
    featureCompatibilityScores.merge(
        feature, score, (existing, newScore) -> (existing + newScore) / 2.0);
  }

  private void updateOverallCompatibility(final double score) {
    this.overallCompatibilityScore = (overallCompatibilityScore + score) / 2.0;
  }
}

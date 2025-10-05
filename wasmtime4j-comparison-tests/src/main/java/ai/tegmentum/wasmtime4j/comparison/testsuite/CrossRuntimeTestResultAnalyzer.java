package ai.tegmentum.wasmtime4j.comparison.testsuite;

import ai.tegmentum.wasmtime4j.comparison.analysis.CrossRuntimeAnalysis;
import ai.tegmentum.wasmtime4j.testsuite.TestExecutionResults;
import ai.tegmentum.wasmtime4j.testsuite.TestResult;
import ai.tegmentum.wasmtime4j.testsuite.TestRuntime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Analyzer for cross-runtime comparison test results. Identifies behavioral discrepancies between
 * JNI and Panama implementations.
 */
public final class CrossRuntimeTestResultAnalyzer {

  private static final Logger LOGGER =
      Logger.getLogger(CrossRuntimeTestResultAnalyzer.class.getName());

  /**
   * Performs cross-runtime analysis comparing results between different runtimes.
   *
   * @param results test execution results
   * @return cross-runtime analysis
   */
  public CrossRuntimeAnalysis performCrossRuntimeAnalysis(final TestExecutionResults results) {
    LOGGER.info("Performing cross-runtime analysis");

    final Map<String, List<TestResult>> resultsByTestCase = results.getResultsByTestCase();
    final List<String> discrepancies = new ArrayList<>();
    final Map<TestRuntime, Integer> runtimeSuccessRates = new HashMap<>();

    // Calculate success rates per runtime
    for (final TestRuntime runtime : results.getRuntimeResults().keySet()) {
      final List<TestResult> runtimeResults = results.getResultsForRuntime(runtime);
      final long successCount =
          runtimeResults.stream().mapToLong(result -> result.isSuccess() ? 1 : 0).sum();
      final int successRate = (int) ((double) successCount / runtimeResults.size() * 100);
      runtimeSuccessRates.put(runtime, successRate);
    }

    // Find discrepancies between runtimes
    for (final Map.Entry<String, List<TestResult>> entry : resultsByTestCase.entrySet()) {
      final String testId = entry.getKey();
      final List<TestResult> testResults = entry.getValue();

      if (testResults.size() > 1) {
        // Check if results differ between runtimes
        final boolean hasSuccesses = testResults.stream().anyMatch(TestResult::isSuccess);
        final boolean hasFailures = testResults.stream().anyMatch(TestResult::isFailure);

        if (hasSuccesses && hasFailures) {
          final StringBuilder discrepancy = new StringBuilder();
          discrepancy
              .append("Test ")
              .append(testId)
              .append(" has different results across runtimes: ");

          for (final TestResult result : testResults) {
            discrepancy
                .append(result.getRuntime().getId())
                .append("=")
                .append(result.getStatus())
                .append(" ");
          }

          discrepancies.add(discrepancy.toString().trim());
        }
      }
    }

    return new CrossRuntimeAnalysis(runtimeSuccessRates, discrepancies);
  }
}

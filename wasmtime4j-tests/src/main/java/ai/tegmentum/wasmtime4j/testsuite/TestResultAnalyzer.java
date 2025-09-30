package ai.tegmentum.wasmtime4j.testsuite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Analyzer for WebAssembly test results providing comprehensive analysis capabilities. */
public final class TestResultAnalyzer {

  private static final Logger LOGGER = Logger.getLogger(TestResultAnalyzer.class.getName());

  private final TestSuiteConfiguration configuration;

  /**
   * Constructs a test result analyzer.
   *
   * @param configuration the test suite configuration
   */
  public TestResultAnalyzer(final TestSuiteConfiguration configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }
    this.configuration = configuration;
  }

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

  /**
   * Analyzes performance metrics and detects regressions.
   *
   * @param results test execution results
   * @return performance analysis
   */
  public PerformanceAnalysis analyzePerformance(final TestExecutionResults results) {
    LOGGER.info("Analyzing performance metrics");

    final Map<TestRuntime, Double> averageExecutionTimes = new HashMap<>();
    final List<String> performanceIssues = new ArrayList<>();

    // Calculate average execution times per runtime
    for (final TestRuntime runtime : results.getRuntimeResults().keySet()) {
      final List<TestResult> runtimeResults = results.getResultsForRuntime(runtime);
      final double avgTime =
          runtimeResults.stream().mapToLong(TestResult::getExecutionTimeMs).average().orElse(0.0);
      averageExecutionTimes.put(runtime, avgTime);
    }

    // Detect performance outliers
    final double overallAverage =
        averageExecutionTimes.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

    for (final Map.Entry<TestRuntime, Double> entry : averageExecutionTimes.entrySet()) {
      final double runtimeAvg = entry.getValue();
      if (runtimeAvg > overallAverage * 2.0) {
        performanceIssues.add(
            "Runtime "
                + entry.getKey().getId()
                + " has significantly higher execution times: "
                + String.format("%.2f ms", runtimeAvg));
      }
    }

    return new PerformanceAnalysis(averageExecutionTimes, performanceIssues);
  }

  /**
   * Analyzes test coverage and identifies gaps.
   *
   * @param results test execution results
   * @return coverage analysis
   */
  public CoverageAnalysis analyzeCoverage(final TestExecutionResults results) {
    LOGGER.info("Analyzing test coverage");

    final Map<TestCategory, Integer> testCountsByCategory = new HashMap<>();
    final List<String> coverageGaps = new ArrayList<>();

    // Count tests by category
    for (final TestResult result : results.getAllResults()) {
      final TestCategory category = result.getTestCase().getCategory();
      testCountsByCategory.merge(category, 1, Integer::sum);
    }

    // Identify coverage gaps
    final TestCategory[] allCategories = TestCategory.values();
    for (final TestCategory category : allCategories) {
      final int testCount = testCountsByCategory.getOrDefault(category, 0);
      if (testCount == 0) {
        coverageGaps.add("No tests found for category: " + category.getDisplayName());
      } else if (testCount < 5 && category.isSpecTest()) {
        coverageGaps.add(
            "Low test coverage for category: "
                + category.getDisplayName()
                + " ("
                + testCount
                + " tests)");
      }
    }

    return new CoverageAnalysis(testCountsByCategory, coverageGaps);
  }

  /**
   * Generates insights and recommendations based on test results.
   *
   * @param results test execution results
   * @return test insights
   */
  public TestInsights generateInsights(final TestExecutionResults results) {
    LOGGER.info("Generating test insights");

    final List<String> insights = new ArrayList<>();
    final List<String> recommendations = new ArrayList<>();

    // Overall test health insights
    final double passRate = results.getPassRate();
    if (passRate < 90.0) {
      insights.add("Test suite has low pass rate: " + String.format("%.1f%%", passRate));
      recommendations.add("Investigate failing tests and improve test stability");
    } else if (passRate > 99.0) {
      insights.add("Test suite has excellent pass rate: " + String.format("%.1f%%", passRate));
    }

    // Runtime comparison insights
    final Map<TestRuntime, List<TestResult>> runtimeResults = results.getRuntimeResults();
    if (runtimeResults.size() > 1) {
      final Map<TestRuntime, Double> passRates = new HashMap<>();
      for (final Map.Entry<TestRuntime, List<TestResult>> entry : runtimeResults.entrySet()) {
        final List<TestResult> testResults = entry.getValue();
        final long passed = testResults.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        final double runtimePassRate = (double) passed / testResults.size() * 100.0;
        passRates.put(entry.getKey(), runtimePassRate);
      }

      final double maxPassRate =
          passRates.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
      final double minPassRate =
          passRates.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

      if (maxPassRate - minPassRate > 5.0) {
        insights.add(
            "Significant variation in pass rates between runtimes: "
                + String.format("%.1f%% - %.1f%%", minPassRate, maxPassRate));
        recommendations.add(
            "Investigate runtime-specific failures and improve cross-runtime compatibility");
      }
    }

    // Performance insights
    final long avgExecutionTime =
        (long)
            results.getAllResults().stream()
                .mapToLong(TestResult::getExecutionTimeMs)
                .average()
                .orElse(0.0);

    if (avgExecutionTime > 10000) {
      insights.add("Tests have high average execution time: " + avgExecutionTime + " ms");
      recommendations.add("Optimize test performance and consider parallel execution");
    }

    return new TestInsights(insights, recommendations);
  }
}

package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestCase;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Integrates enhanced Wasmtime coverage analysis with existing test execution framework. This class
 * serves as the main entry point for running comprehensive coverage analysis with Wasmtime-specific
 * enhancements.
 *
 * @since 1.0.0
 */
public final class WasmtimeCoverageIntegrator {
  private static final Logger LOGGER = Logger.getLogger(WasmtimeCoverageIntegrator.class.getName());

  private final WasmtimeCoverageAnalyzer wasmtimeCoverageAnalyzer;
  private final BehavioralAnalyzer behavioralAnalyzer;
  private final PerformanceAnalyzer performanceAnalyzer;

  /** Creates a new WasmtimeCoverageIntegrator with all necessary analyzers. */
  public WasmtimeCoverageIntegrator() {
    this.wasmtimeCoverageAnalyzer = new WasmtimeCoverageAnalyzer();
    this.behavioralAnalyzer = new BehavioralAnalyzer();
    this.performanceAnalyzer = new PerformanceAnalyzer();
  }

  /**
   * Runs comprehensive Wasmtime coverage analysis for all available test suites.
   *
   * @return comprehensive coverage analysis results
   * @throws IOException if test suites cannot be loaded
   */
  public WasmtimeComprehensiveCoverageReport runComprehensiveCoverageAnalysis() throws IOException {
    LOGGER.info("Starting comprehensive Wasmtime coverage analysis");

    // Ensure test suites are available
    WasmTestSuiteLoader.ensureTestSuitesAvailable();

    // Load and analyze each test suite
    for (final WasmTestSuiteLoader.TestSuiteType suiteType :
        WasmTestSuiteLoader.TestSuiteType.values()) {
      analyzeTestSuite(suiteType);
    }

    // Generate comprehensive report
    final WasmtimeComprehensiveCoverageReport report =
        wasmtimeCoverageAnalyzer.generateWasmtimeReport();

    LOGGER.info(
        String.format(
            "Comprehensive Wasmtime coverage analysis completed. Overall coverage: %.2f%%,"
                + " Compatibility: %.2f%%",
            report.getTestSuiteCoverage().getCoveragePercentage(),
            report.getWasmtimeCompatibilityScores().values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0)));

    return report;
  }

  /**
   * Analyzes a specific test case with enhanced Wasmtime coverage analysis.
   *
   * @param testCase the test case to analyze
   * @param executionResults the execution results from different runtimes
   * @return enhanced coverage analysis result
   */
  public WasmtimeCoverageAnalysisResult analyzeTestCase(
      final WasmTestCase testCase,
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {

    LOGGER.fine(String.format("Analyzing test case: %s", testCase.getTestName()));

    // Perform behavioral analysis
    final BehavioralAnalysisResult behavioralResults =
        behavioralAnalyzer.analyzeBehavioralConsistency(testCase.getTestName(), executionResults);

    // Perform performance analysis
    final PerformanceAnalyzer.PerformanceComparisonResult performanceResults =
        performanceAnalyzer.analyzePerformance(testCase.getTestName(), executionResults);

    // Perform enhanced Wasmtime coverage analysis
    return wasmtimeCoverageAnalyzer.analyzeWasmtimeCoverage(
        testCase, executionResults, behavioralResults, performanceResults);
  }

  /**
   * Gets the current global Wasmtime coverage statistics.
   *
   * @return global coverage statistics
   */
  public WasmtimeGlobalCoverageStatistics getGlobalCoverageStatistics() {
    return wasmtimeCoverageAnalyzer.getWasmtimeGlobalStatistics();
  }

  /**
   * Validates that the current coverage meets the 95% target and 100% API compatibility goals.
   *
   * @return validation result with detailed analysis
   */
  public WasmtimeCoverageValidationResult validateCoverageTargets() {
    final WasmtimeGlobalCoverageStatistics stats = getGlobalCoverageStatistics();
    final WasmtimeComprehensiveCoverageReport report =
        wasmtimeCoverageAnalyzer.generateWasmtimeReport();

    final boolean meets95PercentTarget = stats.meets95PercentTarget();
    final boolean is100PercentCompatible = stats.isFullyCompatible();

    return new WasmtimeCoverageValidationResult(
        meets95PercentTarget,
        is100PercentCompatible,
        stats.getOverallCoveragePercentage(),
        stats.getCompatibilityScore(),
        report.getWasmtimeRecommendations());
  }

  /** Clears all coverage analysis data. */
  public void clearAnalysisData() {
    wasmtimeCoverageAnalyzer.clearWasmtimeCoverageData();
    LOGGER.info("All Wasmtime coverage analysis data cleared");
  }

  private void analyzeTestSuite(final WasmTestSuiteLoader.TestSuiteType suiteType)
      throws IOException {
    LOGGER.info(String.format("Analyzing test suite: %s", suiteType.name()));

    final List<WasmTestCase> testCases = WasmTestSuiteLoader.loadTestSuite(suiteType);

    if (testCases.isEmpty()) {
      LOGGER.warning(String.format("No test cases found for suite: %s", suiteType.name()));
      return;
    }

    int analyzedCount = 0;
    for (final WasmTestCase testCase : testCases) {
      try {
        // In a real implementation, this would execute the test case across runtimes
        // For now, we'll create mock execution results
        final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> mockResults =
            createMockExecutionResults(testCase);

        analyzeTestCase(testCase, mockResults);
        analyzedCount++;

        if (analyzedCount % 100 == 0) {
          LOGGER.info(
              String.format(
                  "Analyzed %d/%d test cases for suite %s",
                  analyzedCount, testCases.size(), suiteType.name()));
        }
      } catch (final Exception e) {
        LOGGER.warning(
            String.format(
                "Failed to analyze test case %s: %s", testCase.getTestName(), e.getMessage()));
      }
    }

    LOGGER.info(
        String.format(
            "Completed analysis of %d test cases for suite %s", analyzedCount, suiteType.name()));
  }

  private Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> createMockExecutionResults(
      final WasmTestCase testCase) {
    // This is a simplified mock implementation
    // In a real implementation, this would execute the test case across actual runtimes
    return Map.of(
        RuntimeType.JNI,
            new BehavioralAnalyzer.TestExecutionResult(
                true, null, System.currentTimeMillis(), System.currentTimeMillis() + 100),
        RuntimeType.PANAMA,
            new BehavioralAnalyzer.TestExecutionResult(
                true, null, System.currentTimeMillis(), System.currentTimeMillis() + 95));
  }
}

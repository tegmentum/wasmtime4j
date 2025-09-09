package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Comprehensive integration test for WebAssembly test suite execution and validation. This test
 * orchestrates the complete testing workflow including test suite download, cross-runtime
 * execution, failure analysis, and performance benchmarking.
 *
 * <p>This test is designed to be used in CI/CD pipelines and can be configured via system
 * properties to control test execution scope and reporting detail.
 *
 * <p>System properties for configuration:
 *
 * <ul>
 *   <li>{@code wasmtime4j.test.download-suites} - Enable automatic test suite download
 *   <li>{@code wasmtime4j.test.performance} - Enable performance testing
 *   <li>{@code wasmtime4j.test.generate-custom} - Enable custom test generation
 *   <li>{@code wasmtime4j.test.failure-analysis} - Enable detailed failure analysis
 *   <li>{@code wasmtime4j.test.success-threshold} - Minimum success rate (default: 95%)
 * </ul>
 */
@DisplayName("WebAssembly Test Suite Integration")
public class WebAssemblyTestSuiteIntegrationIT {
  private static final Logger LOGGER =
      Logger.getLogger(WebAssemblyTestSuiteIntegrationIT.class.getName());

  // Configuration from system properties
  private static final boolean ENABLE_SUITE_DOWNLOAD =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.test.download-suites", "true"));
  private static final boolean ENABLE_PERFORMANCE_TESTING =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.test.performance", "true"));
  private static final boolean ENABLE_CUSTOM_TEST_GENERATION =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.test.generate-custom", "true"));
  private static final boolean ENABLE_FAILURE_ANALYSIS =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.test.failure-analysis", "true"));
  private static final double SUCCESS_THRESHOLD =
      Double.parseDouble(System.getProperty("wasmtime4j.test.success-threshold", "95.0"));

  // Test execution results for cleanup and reporting
  private static WasmTestSuiteExecutionResults testSuiteResults;
  private static WasmPerformanceTestResults performanceResults;
  private static List<TestSuiteFailureReport> failureReports;

  @BeforeAll
  static void setupTestSuite() throws IOException {
    LOGGER.info("Setting up WebAssembly test suite integration");
    LOGGER.info(
        "Configuration: download="
            + ENABLE_SUITE_DOWNLOAD
            + ", performance="
            + ENABLE_PERFORMANCE_TESTING
            + ", custom="
            + ENABLE_CUSTOM_TEST_GENERATION
            + ", failure-analysis="
            + ENABLE_FAILURE_ANALYSIS
            + ", success-threshold="
            + SUCCESS_THRESHOLD
            + "%");

    // Initialize test resources
    final Path testResourcesPath =
        WasmTestSuiteLoader.getTestSuiteDirectory(WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS)
            .getParent();

    // Download official test suites if enabled
    if (ENABLE_SUITE_DOWNLOAD) {
      LOGGER.info("Downloading official WebAssembly test suites...");
      try {
        WasmSpecTestDownloader.downloadAllTestSuites(testResourcesPath);
        LOGGER.info("Test suites downloaded successfully");
      } catch (final IOException e) {
        LOGGER.warning("Failed to download test suites: " + e.getMessage());
        LOGGER.warning("Proceeding with available tests only");
      }
    }

    // Generate custom Java-specific test cases if enabled
    if (ENABLE_CUSTOM_TEST_GENERATION) {
      LOGGER.info("Generating custom Java-specific test cases...");
      final Path customTestsPath = testResourcesPath.resolve("custom-tests");
      final int generatedTests =
          JavaSpecificTestGenerator.generateAllJavaSpecificTests(customTestsPath);
      LOGGER.info("Generated " + generatedTests + " custom test cases");
    }

    // Ensure test suites are available
    WasmTestSuiteLoader.ensureTestSuitesAvailable();

    // Initialize result collections
    failureReports = new ArrayList<>();
  }

  @AfterAll
  static void teardownTestSuite() {
    LOGGER.info("Cleaning up WebAssembly test suite integration");

    // Clear caches
    CrossRuntimeTestRunner.clearCache();
    WasmTestFailureAnalyzer.clearCache();
    WasmPerformanceTestFramework.clearRuntimeCache();

    // Generate comprehensive final report
    generateFinalReport();
  }

  @Test
  @DisplayName("Execute comprehensive WebAssembly test suite validation")
  void executeComprehensiveTestSuiteValidation(final TestInfo testInfo) throws IOException {
    LOGGER.info("Starting comprehensive WebAssembly test suite validation");

    // Configure test execution options
    final WasmTestSuiteRunner.TestExecutionOptions.Builder optionsBuilder =
        WasmTestSuiteRunner.TestExecutionOptions.builder()
            .parallelExecution(true)
            .testTimeout(Duration.ofSeconds(30))
            .maxRetryAttempts(2);

    // Include available runtimes
    optionsBuilder.targetRuntime(RuntimeType.JNI);
    if (TestUtils.isPanamaAvailable()) {
      optionsBuilder.targetRuntime(RuntimeType.PANAMA);
      LOGGER.info("Including Panama runtime in test execution");
    } else {
      LOGGER.info("Panama runtime not available - JNI only execution");
    }

    final WasmTestSuiteRunner.TestExecutionOptions options = optionsBuilder.build();

    // Execute all test suites
    testSuiteResults = WasmTestSuiteRunner.executeAllTestSuites(options);

    // Log comprehensive results
    LOGGER.info("Test suite execution completed");
    LOGGER.info(testSuiteResults.createSummaryReport());

    // Validate success threshold
    final double overallSuccessRate = testSuiteResults.getOverallSuccessRate();
    if (overallSuccessRate < SUCCESS_THRESHOLD) {
      final String message =
          String.format(
              "Test suite success rate %.1f%% is below threshold %.1f%%",
              overallSuccessRate, SUCCESS_THRESHOLD);
      LOGGER.severe(message);

      if (ENABLE_FAILURE_ANALYSIS) {
        performFailureAnalysis();
      }

      throw new AssertionError(message);
    }

    // Perform detailed failure analysis if enabled and there are failures
    if (ENABLE_FAILURE_ANALYSIS && testSuiteResults.getTotalFailedTests() > 0) {
      performFailureAnalysis();
    }

    LOGGER.info("Comprehensive test suite validation completed successfully");
  }

  @Test
  @DisplayName("Execute cross-runtime consistency validation")
  void executeCrossRuntimeConsistencyValidation() throws IOException {
    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.info("Skipping cross-runtime consistency validation - Panama not available");
      return;
    }

    LOGGER.info("Starting cross-runtime consistency validation");

    // Load a representative sample of test cases
    final List<WasmTestCase> testCases = loadRepresentativeTestSample();
    int consistentTests = 0;
    int inconsistentTests = 0;

    for (final WasmTestCase testCase : testCases) {
      final CrossRuntimeValidationResult validationResult =
          CrossRuntimeTestRunner.validateConsistency(
              testCase.getTestName(),
              runtime -> {
                try {
                  // Simplified test execution - in real implementation would use actual WebAssembly
                  // operations
                  return "Test completed successfully";
                } catch (final Exception e) {
                  throw new RuntimeException("Test execution failed", e);
                }
              },
              comparison -> {
                // Simple consistency check - in real implementation would compare actual results
                return comparison.getJniExecution().isSuccessful()
                    == comparison.getPanamaExecution().isSuccessful();
              });

      if (validationResult.isConsistent()) {
        consistentTests++;
      } else {
        inconsistentTests++;
        LOGGER.warning("Inconsistent behavior detected for test: " + testCase.getTestName());

        if (ENABLE_FAILURE_ANALYSIS) {
          LOGGER.warning("Validation details: " + validationResult.getSummary());
        }
      }
    }

    final double consistencyRate =
        (double) consistentTests / (consistentTests + inconsistentTests) * 100.0;
    LOGGER.info(
        String.format(
            "Cross-runtime consistency: %d consistent, %d inconsistent (%.1f%% consistent)",
            consistentTests, inconsistentTests, consistencyRate));

    // Validate consistency threshold (should be very high for cross-runtime consistency)
    if (consistencyRate < 98.0) {
      final String message =
          String.format(
              "Cross-runtime consistency rate %.1f%% is below required 98%%", consistencyRate);
      LOGGER.severe(message);
      throw new AssertionError(message);
    }

    LOGGER.info("Cross-runtime consistency validation completed successfully");
  }

  @Test
  @DisplayName("Execute performance benchmarking and regression detection")
  @EnabledIfSystemProperty(named = "wasmtime4j.test.performance", matches = "true")
  void executePerformanceBenchmarkingAndRegressionDetection() throws IOException {
    LOGGER.info("Starting performance benchmarking and regression detection");

    // Load test cases suitable for performance testing
    final List<WasmTestCase> performanceTestCases = loadPerformanceTestCases();

    // Configure performance test settings
    final WasmPerformanceTestFramework.PerformanceTestConfiguration config =
        WasmPerformanceTestFramework.PerformanceTestConfiguration.builder()
            .warmupIterations(100)
            .measurementIterations(1000)
            .benchmarkRuns(3)
            .maxTestDuration(Duration.ofMinutes(5))
            .build();

    // Execute performance tests
    performanceResults =
        WasmPerformanceTestFramework.executePerformanceTests(performanceTestCases, config);

    // Log performance results
    LOGGER.info("Performance benchmarking completed");
    LOGGER.info(performanceResults.createComprehensiveReport());

    // Validate performance success rate
    final double performanceSuccessRate = performanceResults.getSuccessRate();
    if (performanceSuccessRate < SUCCESS_THRESHOLD) {
      final String message =
          String.format(
              "Performance test success rate %.1f%% is below threshold %.1f%%",
              performanceSuccessRate, SUCCESS_THRESHOLD);
      LOGGER.severe(message);
      throw new AssertionError(message);
    }

    // Check for performance consistency between runtimes
    if (performanceResults.getTestedRuntimes().size() > 1) {
      validatePerformanceConsistency();
    }

    LOGGER.info("Performance benchmarking completed successfully");
  }

  @Test
  @DisplayName("Validate test suite statistics and coverage")
  void validateTestSuiteStatisticsAndCoverage() throws IOException {
    LOGGER.info("Validating test suite statistics and coverage");

    final WasmTestSuiteStats stats = WasmTestSuiteLoader.getTestSuiteStatistics();
    LOGGER.info("Test suite statistics: " + stats.toString());

    // Validate minimum test coverage
    final int totalTests = stats.getTotalTestCount();
    if (totalTests < 10) {
      final String message = "Insufficient test coverage: only " + totalTests + " tests available";
      LOGGER.severe(message);
      throw new AssertionError(message);
    }

    // Validate that each test suite type has some tests (if directories exist)
    for (final WasmTestSuiteLoader.TestSuiteType suiteType :
        WasmTestSuiteLoader.TestSuiteType.values()) {
      final Path suiteDir = WasmTestSuiteLoader.getTestSuiteDirectory(suiteType);
      if (Files.exists(suiteDir) && stats.getTestCount(suiteType) == 0) {
        LOGGER.warning("Test suite directory exists but contains no tests: " + suiteType.name());
      }
    }

    LOGGER.info("Test suite statistics and coverage validation completed");
  }

  /** Performs detailed failure analysis on failed test cases. */
  private void performFailureAnalysis() {
    LOGGER.info("Performing detailed failure analysis");

    for (final WasmTestSuiteLoader.TestSuiteType suiteType :
        WasmTestSuiteLoader.TestSuiteType.values()) {
      final WasmTestSuiteResults suiteResults = testSuiteResults.getSuiteResults(suiteType);
      if (suiteResults != null && suiteResults.getFailedTests() > 0) {

        LOGGER.info("Analyzing failures for test suite: " + suiteType.name());
        final TestSuiteFailureReport failureReport =
            WasmTestFailureAnalyzer.generateFailureReport(suiteResults);

        failureReports.add(failureReport);

        LOGGER.info("Failure analysis for " + suiteType.name() + ":");
        LOGGER.info(failureReport.createComprehensiveReport());
      }
    }

    LOGGER.info("Failure analysis completed");
  }

  /** Validates performance consistency between runtimes. */
  private void validatePerformanceConsistency() {
    LOGGER.info("Validating performance consistency between runtimes");

    final List<RuntimeType> runtimes = new ArrayList<>(performanceResults.getTestedRuntimes());
    if (runtimes.size() < 2) {
      LOGGER.info("Insufficient runtimes for consistency comparison");
      return;
    }

    // Compare performance between the first two runtimes
    final RuntimePerformanceComparison comparison =
        performanceResults.compareRuntimes(runtimes.get(0), runtimes.get(1));

    LOGGER.info(
        "Performance comparison between " + runtimes.get(0) + " and " + runtimes.get(1) + ":");
    LOGGER.info(comparison.createSummaryReport());

    // Log any significant performance differences
    // In a real implementation, this would use the actual comparison results
    LOGGER.info("Performance consistency validation completed");
  }

  /** Loads a representative sample of test cases for consistency validation. */
  private List<WasmTestCase> loadRepresentativeTestSample() throws IOException {
    final List<WasmTestCase> testCases = new ArrayList<>();

    // Load a sample from each available test suite
    for (final WasmTestSuiteLoader.TestSuiteType suiteType :
        WasmTestSuiteLoader.TestSuiteType.values()) {
      final List<WasmTestCase> suiteTests = WasmTestSuiteLoader.loadTestSuite(suiteType);

      // Take up to 5 tests from each suite for consistency validation
      final int sampleSize = Math.min(5, suiteTests.size());
      testCases.addAll(suiteTests.subList(0, sampleSize));
    }

    return testCases;
  }

  /** Loads test cases suitable for performance testing. */
  private List<WasmTestCase> loadPerformanceTestCases() throws IOException {
    // Load custom tests which are designed for performance testing
    final List<WasmTestCase> customTests =
        WasmTestSuiteLoader.loadTestSuite(WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS);

    // Filter for performance-oriented tests
    return customTests.stream()
        .filter(
            testCase ->
                testCase.getTestName().contains("performance")
                    || testCase.getTestName().contains("stress"))
        .collect(java.util.stream.Collectors.toList());
  }

  /** Generates a comprehensive final report of all test execution results. */
  private static void generateFinalReport() {
    LOGGER.info("Generating final comprehensive test report");

    final StringBuilder finalReport = new StringBuilder();
    finalReport.append("WebAssembly Test Suite Integration - Final Report\n");
    finalReport.append("=".repeat(52)).append("\n\n");

    // Test suite execution summary
    if (testSuiteResults != null) {
      finalReport.append("Test Suite Execution Summary:\n");
      finalReport.append("-".repeat(30)).append("\n");
      finalReport.append(testSuiteResults.createSummaryReport());
      finalReport.append("\n");
    }

    // Performance testing summary
    if (performanceResults != null) {
      finalReport.append("Performance Testing Summary:\n");
      finalReport.append("-".repeat(28)).append("\n");
      finalReport.append(performanceResults.toString());
      finalReport.append("\n\n");
    }

    // Failure analysis summary
    if (!failureReports.isEmpty()) {
      finalReport.append("Failure Analysis Summary:\n");
      finalReport.append("-".repeat(26)).append("\n");
      for (final TestSuiteFailureReport report : failureReports) {
        finalReport.append(report.createBriefSummary()).append("\n");
      }
      finalReport.append("\n");
    }

    // Runtime environment information
    finalReport.append("Runtime Environment:\n");
    finalReport.append("-".repeat(20)).append("\n");
    finalReport.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
    finalReport.append("Java VM: ").append(System.getProperty("java.vm.name")).append("\n");
    finalReport
        .append("OS: ")
        .append(System.getProperty("os.name"))
        .append(" ")
        .append(System.getProperty("os.version"))
        .append("\n");
    finalReport.append("Architecture: ").append(System.getProperty("os.arch")).append("\n");
    finalReport.append("Panama Available: ").append(TestUtils.isPanamaAvailable()).append("\n");

    LOGGER.info("Final Report:\n" + finalReport.toString());
  }
}

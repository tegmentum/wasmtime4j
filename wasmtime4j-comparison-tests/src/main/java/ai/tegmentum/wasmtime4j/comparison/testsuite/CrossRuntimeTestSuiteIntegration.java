package ai.tegmentum.wasmtime4j.comparison.testsuite;

import ai.tegmentum.wasmtime4j.comparison.analysis.CrossRuntimeAnalysis;
import ai.tegmentum.wasmtime4j.testsuite.TestAnalysisReport;
import ai.tegmentum.wasmtime4j.testsuite.TestDiscoveryEngine;
import ai.tegmentum.wasmtime4j.testsuite.TestExecutionEngine;
import ai.tegmentum.wasmtime4j.testsuite.TestExecutionResults;
import ai.tegmentum.wasmtime4j.testsuite.TestReporter;
import ai.tegmentum.wasmtime4j.testsuite.TestResult;
import ai.tegmentum.wasmtime4j.testsuite.TestResultAnalyzer;
import ai.tegmentum.wasmtime4j.testsuite.TestRuntime;
import ai.tegmentum.wasmtime4j.testsuite.TestSuiteConfiguration;
import ai.tegmentum.wasmtime4j.testsuite.TestSuiteException;
import ai.tegmentum.wasmtime4j.testsuite.WebAssemblyTestCase;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Cross-runtime comparison test suite for wasmtime4j. Executes tests across both JNI and Panama
 * implementations and identifies behavioral discrepancies.
 *
 * <p>This class is the entry point for cross-runtime comparison testing. It orchestrates test
 * discovery, execution across multiple runtimes, cross-runtime analysis, and reporting.
 */
public final class CrossRuntimeTestSuiteIntegration {

  private static final Logger LOGGER =
      Logger.getLogger(CrossRuntimeTestSuiteIntegration.class.getName());

  private final TestSuiteConfiguration configuration;
  private final TestDiscoveryEngine discoveryEngine;
  private final TestExecutionEngine executionEngine;
  private final TestResultAnalyzer singleRuntimeAnalyzer;
  private final CrossRuntimeTestResultAnalyzer crossRuntimeAnalyzer;
  private final TestReporter reporter;

  /**
   * Creates a new cross-runtime test suite integration.
   *
   * @param configuration test suite configuration (must enable multiple runtimes)
   */
  public CrossRuntimeTestSuiteIntegration(final TestSuiteConfiguration configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("Test suite configuration cannot be null");
    }
    if (configuration.getEnabledRuntimes().size() < 2) {
      throw new IllegalArgumentException(
          "Cross-runtime testing requires at least 2 runtimes. "
              + "Use WebAssemblyTestSuiteIntegration for single-runtime testing.");
    }
    this.configuration = configuration;
    this.discoveryEngine = new TestDiscoveryEngine(configuration);
    this.executionEngine = new TestExecutionEngine(configuration);
    this.singleRuntimeAnalyzer = new TestResultAnalyzer(configuration);
    this.crossRuntimeAnalyzer = new CrossRuntimeTestResultAnalyzer();
    this.reporter = new TestReporter(configuration);
  }

  /**
   * Discovers all available WebAssembly tests based on configuration.
   *
   * @return discovered test cases
   * @throws TestSuiteException if test discovery fails
   */
  public Collection<WebAssemblyTestCase> discoverTests() throws TestSuiteException {
    LOGGER.info("Starting cross-runtime test discovery");

    try {
      final List<WebAssemblyTestCase> discoveredTests = new ArrayList<>();

      // Discover official WebAssembly specification tests
      if (configuration.isOfficialTestsEnabled()) {
        discoveredTests.addAll(discoveryEngine.discoverOfficialSpecTests());
      }

      // Discover Wasmtime-specific tests
      if (configuration.isWasmtimeTestsEnabled()) {
        discoveredTests.addAll(discoveryEngine.discoverWasmtimeTests());
      }

      // Discover custom Java-specific tests
      if (configuration.isCustomTestsEnabled()) {
        discoveredTests.addAll(discoveryEngine.discoverCustomTests());
      }

      LOGGER.info("Discovered " + discoveredTests.size() + " test cases for cross-runtime testing");
      return discoveredTests;

    } catch (final Exception e) {
      throw new TestSuiteException("Failed to discover tests for cross-runtime comparison", e);
    }
  }

  /**
   * Executes discovered test cases across all configured runtimes.
   *
   * @param testCases test cases to execute
   * @return execution results
   * @throws TestSuiteException if test execution fails
   */
  public TestExecutionResults executeTests(final Collection<WebAssemblyTestCase> testCases)
      throws TestSuiteException {
    LOGGER.info(
        "Starting cross-runtime test execution for "
            + testCases.size()
            + " test cases across "
            + configuration.getEnabledRuntimes().size()
            + " runtimes");

    try {
      final TestExecutionResults.Builder resultsBuilder = TestExecutionResults.builder();
      final ExecutorService executor =
          Executors.newFixedThreadPool(configuration.getMaxConcurrentTests());

      try {
        // Execute tests for each configured runtime
        for (final TestRuntime runtime : configuration.getEnabledRuntimes()) {
          LOGGER.info("Executing tests for runtime: " + runtime);

          final List<CompletableFuture<TestResult>> futures = new ArrayList<>();

          for (final WebAssemblyTestCase testCase : testCases) {
            final CompletableFuture<TestResult> future =
                CompletableFuture.supplyAsync(
                        () -> executionEngine.executeTest(testCase, runtime), executor)
                    .exceptionally(
                        throwable ->
                            TestResult.failure(
                                testCase, runtime, throwable.getMessage(), throwable));
            futures.add(future);
          }

          // Wait for all tests to complete
          final CompletableFuture<Void> allTests =
              CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
          allTests.get(configuration.getTestTimeoutMinutes(), TimeUnit.MINUTES);

          // Collect results
          final List<TestResult> runtimeResults = new ArrayList<>();
          for (final CompletableFuture<TestResult> future : futures) {
            runtimeResults.add(future.get());
          }

          resultsBuilder.addRuntimeResults(runtime, runtimeResults);
        }

      } finally {
        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      }

      final TestExecutionResults results = resultsBuilder.build();
      LOGGER.info("Cross-runtime test execution completed. Total results: "
          + results.getTotalTestCount());

      return results;

    } catch (final Exception e) {
      throw new TestSuiteException("Failed to execute cross-runtime tests", e);
    }
  }

  /**
   * Analyzes cross-runtime test results for discrepancies and performance differences.
   *
   * @param results test execution results
   * @return cross-runtime analysis
   * @throws TestSuiteException if analysis fails
   */
  public CrossRuntimeAnalysis analyzeCrossRuntimeResults(final TestExecutionResults results)
      throws TestSuiteException {
    LOGGER.info("Starting cross-runtime analysis");

    try {
      final CrossRuntimeAnalysis analysis = crossRuntimeAnalyzer.performCrossRuntimeAnalysis(results);

      LOGGER.info("Cross-runtime analysis completed. Discrepancies found: "
          + analysis.getDiscrepancies().size());

      return analysis;

    } catch (final Exception e) {
      throw new TestSuiteException("Failed to perform cross-runtime analysis", e);
    }
  }

  /**
   * Generates comprehensive cross-runtime comparison reports.
   *
   * @param results test execution results
   * @param crossRuntimeAnalysis cross-runtime analysis
   * @throws TestSuiteException if report generation fails
   */
  public void generateReports(
      final TestExecutionResults results, final CrossRuntimeAnalysis crossRuntimeAnalysis)
      throws TestSuiteException {
    LOGGER.info("Starting cross-runtime comparison report generation");

    try {
      // Log cross-runtime discrepancies to console
      if (configuration.isConsoleReportEnabled()) {
        logCrossRuntimeResults(results, crossRuntimeAnalysis);
      }

      // Additional reporting can be implemented here
      LOGGER.info("Cross-runtime comparison report generation completed");

    } catch (final Exception e) {
      throw new TestSuiteException("Failed to generate cross-runtime comparison reports", e);
    }
  }

  /**
   * Logs cross-runtime comparison results to console.
   *
   * @param results test execution results
   * @param analysis cross-runtime analysis
   */
  private void logCrossRuntimeResults(
      final TestExecutionResults results, final CrossRuntimeAnalysis analysis) {
    final StringBuilder report = new StringBuilder();
    report.append("\n=== Cross-Runtime Comparison Results ===\n");
    report.append("Total tests: ").append(results.getTotalTestCount()).append("\n");

    // Runtime success rates
    report.append("\nRuntime Success Rates:\n");
    for (final Map.Entry<TestRuntime, Integer> entry :
        analysis.getRuntimeSuccessRates().entrySet()) {
      report
          .append("  ")
          .append(entry.getKey().getDisplayName())
          .append(": ")
          .append(entry.getValue())
          .append("%\n");
    }

    // Discrepancies
    if (analysis.hasDiscrepancies()) {
      report.append("\nCross-Runtime Discrepancies (").append(analysis.getDiscrepancies().size())
          .append("):\n");
      for (final String discrepancy : analysis.getDiscrepancies()) {
        report.append("  - ").append(discrepancy).append("\n");
      }
    } else {
      report.append("\nNo cross-runtime discrepancies detected!\n");
    }

    LOGGER.info(report.toString());
  }

  /**
   * Runs the complete cross-runtime comparison test workflow: discovery, execution across multiple
   * runtimes, cross-runtime analysis, and reporting.
   *
   * @return cross-runtime analysis results
   * @throws TestSuiteException if any phase of the workflow fails
   */
  public CrossRuntimeComparisonResults runCompleteComparison() throws TestSuiteException {
    LOGGER.info("Starting complete cross-runtime comparison");

    final long startTime = System.currentTimeMillis();

    try {
      // Phase 1: Test Discovery
      final Collection<WebAssemblyTestCase> testCases = discoverTests();

      // Phase 2: Cross-Runtime Test Execution
      final TestExecutionResults executionResults = executeTests(testCases);

      // Phase 3: Cross-Runtime Analysis
      final CrossRuntimeAnalysis crossRuntimeAnalysis =
          analyzeCrossRuntimeResults(executionResults);

      // Phase 4: Report Generation
      generateReports(executionResults, crossRuntimeAnalysis);

      final long duration = System.currentTimeMillis() - startTime;

      final CrossRuntimeComparisonResults results =
          new CrossRuntimeComparisonResults(
              executionResults, crossRuntimeAnalysis, duration);

      LOGGER.info("Complete cross-runtime comparison finished in " + duration + " ms");

      return results;

    } catch (final Exception e) {
      final long duration = System.currentTimeMillis() - startTime;
      LOGGER.severe("Cross-runtime comparison failed after " + duration + " ms: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Creates a default cross-runtime comparison configuration.
   *
   * @return default cross-runtime configuration
   */
  public static TestSuiteConfiguration createDefaultConfiguration() {
    return TestSuiteConfiguration.builder()
        .enableOfficialTests(true)
        .enableWasmtimeTests(true)
        .enableCustomTests(true)
        .enabledRuntimes(EnumSet.allOf(TestRuntime.class))
        .maxConcurrentTests(Runtime.getRuntime().availableProcessors())
        .testTimeoutMinutes(30)
        .enablePerformanceAnalysis(true)
        .enableCoverageAnalysis(true)
        .enableConsoleReport(true)
        .enableHtmlReport(true)
        .enableJsonReport(true)
        .enableXmlReport(true)
        .build();
  }

  /** Results from a complete cross-runtime comparison run. */
  public static final class CrossRuntimeComparisonResults {
    private final TestExecutionResults executionResults;
    private final CrossRuntimeAnalysis crossRuntimeAnalysis;
    private final long executionTimeMs;

    private CrossRuntimeComparisonResults(
        final TestExecutionResults executionResults,
        final CrossRuntimeAnalysis crossRuntimeAnalysis,
        final long executionTimeMs) {
      this.executionResults = executionResults;
      this.crossRuntimeAnalysis = crossRuntimeAnalysis;
      this.executionTimeMs = executionTimeMs;
    }

    public TestExecutionResults getExecutionResults() {
      return executionResults;
    }

    public CrossRuntimeAnalysis getCrossRuntimeAnalysis() {
      return crossRuntimeAnalysis;
    }

    public long getExecutionTimeMs() {
      return executionTimeMs;
    }

    /**
     * Checks if cross-runtime comparison passed (no discrepancies).
     *
     * @return true if comparison passed
     */
    public boolean isPassed() {
      return !crossRuntimeAnalysis.hasDiscrepancies();
    }
  }
}

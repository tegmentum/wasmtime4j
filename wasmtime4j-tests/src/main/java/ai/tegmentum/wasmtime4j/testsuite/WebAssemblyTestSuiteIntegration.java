package ai.tegmentum.wasmtime4j.testsuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Comprehensive WebAssembly test suite integration for wasmtime4j.
 * Provides automatic test discovery, execution, and reporting across JNI and Panama implementations.
 */
public final class WebAssemblyTestSuiteIntegration {

    private static final Logger LOGGER = Logger.getLogger(WebAssemblyTestSuiteIntegration.class.getName());

    private final TestSuiteConfiguration configuration;
    private final TestDiscoveryEngine discoveryEngine;
    private final TestExecutionEngine executionEngine;
    private final TestResultAnalyzer resultAnalyzer;
    private final TestReporter reporter;

    public WebAssemblyTestSuiteIntegration(final TestSuiteConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Test suite configuration cannot be null");
        }
        this.configuration = configuration;
        this.discoveryEngine = new TestDiscoveryEngine(configuration);
        this.executionEngine = new TestExecutionEngine(configuration);
        this.resultAnalyzer = new TestResultAnalyzer(configuration);
        this.reporter = new TestReporter(configuration);
    }

    /**
     * Discovers all available WebAssembly tests based on configuration.
     *
     * @return discovered test cases
     * @throws TestSuiteException if test discovery fails
     */
    public Collection<WebAssemblyTestCase> discoverTests() throws TestSuiteException {
        LOGGER.info("Starting WebAssembly test discovery");

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

            // Apply test filtering
            final Collection<WebAssemblyTestCase> filteredTests =
                applyTestFiltering(discoveredTests, configuration.getTestFilters());

            LOGGER.info("Discovered " + filteredTests.size() + " WebAssembly test cases");
            return filteredTests;

        } catch (final Exception e) {
            throw new TestSuiteException("Failed to discover WebAssembly tests", e);
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
        LOGGER.info("Starting WebAssembly test execution for " + testCases.size() + " test cases");

        try {
            final TestExecutionResults.Builder resultsBuilder = TestExecutionResults.builder();
            final ExecutorService executor = Executors.newFixedThreadPool(
                configuration.getMaxConcurrentTests());

            try {
                // Execute tests for each configured runtime
                for (final TestRuntime runtime : configuration.getEnabledRuntimes()) {
                    LOGGER.info("Executing tests for runtime: " + runtime);

                    final List<CompletableFuture<TestResult>> futures = new ArrayList<>();

                    for (final WebAssemblyTestCase testCase : testCases) {
                        final CompletableFuture<TestResult> future = CompletableFuture
                            .supplyAsync(() -> executionEngine.executeTest(testCase, runtime), executor)
                            .exceptionally(throwable -> TestResult.failure(
                                testCase, runtime, throwable.getMessage(), throwable));
                        futures.add(future);
                    }

                    // Wait for all tests to complete
                    final CompletableFuture<Void> allTests = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]));
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
            LOGGER.info("Test execution completed. Total results: " + results.getTotalTestCount());

            return results;

        } catch (final Exception e) {
            throw new TestSuiteException("Failed to execute WebAssembly tests", e);
        }
    }

    /**
     * Analyzes test results for regressions, performance issues, and coverage gaps.
     *
     * @param results test execution results
     * @return analysis report
     * @throws TestSuiteException if analysis fails
     */
    public TestAnalysisReport analyzeResults(final TestExecutionResults results)
            throws TestSuiteException {
        LOGGER.info("Starting test result analysis");

        try {
            final TestAnalysisReport.Builder reportBuilder = TestAnalysisReport.builder();

            // Perform cross-runtime comparison
            if (configuration.isCrossRuntimeComparisonEnabled() &&
                results.getRuntimeResults().size() > 1) {
                final CrossRuntimeAnalysis crossRuntimeAnalysis =
                    resultAnalyzer.performCrossRuntimeAnalysis(results);
                reportBuilder.crossRuntimeAnalysis(crossRuntimeAnalysis);
            }

            // Detect performance regressions
            if (configuration.isPerformanceAnalysisEnabled()) {
                final PerformanceAnalysis performanceAnalysis =
                    resultAnalyzer.analyzePerformance(results);
                reportBuilder.performanceAnalysis(performanceAnalysis);
            }

            // Analyze test coverage
            if (configuration.isCoverageAnalysisEnabled()) {
                final CoverageAnalysis coverageAnalysis =
                    resultAnalyzer.analyzeCoverage(results);
                reportBuilder.coverageAnalysis(coverageAnalysis);
            }

            // Generate insights and recommendations
            final TestInsights insights = resultAnalyzer.generateInsights(results);
            reportBuilder.insights(insights);

            final TestAnalysisReport report = reportBuilder.build();
            LOGGER.info("Test analysis completed");

            return report;

        } catch (final Exception e) {
            throw new TestSuiteException("Failed to analyze test results", e);
        }
    }

    /**
     * Generates comprehensive test reports in multiple formats.
     *
     * @param results test execution results
     * @param analysisReport test analysis report
     * @throws TestSuiteException if report generation fails
     */
    public void generateReports(final TestExecutionResults results,
                               final TestAnalysisReport analysisReport)
            throws TestSuiteException {
        LOGGER.info("Starting test report generation");

        try {
            // Generate console report for immediate feedback
            if (configuration.isConsoleReportEnabled()) {
                reporter.generateConsoleReport(results, analysisReport);
            }

            // Generate detailed HTML dashboard
            if (configuration.isHtmlReportEnabled()) {
                final Path htmlReportPath = reporter.generateHtmlReport(results, analysisReport);
                LOGGER.info("HTML report generated: " + htmlReportPath);
            }

            // Generate JSON report for CI integration
            if (configuration.isJsonReportEnabled()) {
                final Path jsonReportPath = reporter.generateJsonReport(results, analysisReport);
                LOGGER.info("JSON report generated: " + jsonReportPath);
            }

            // Generate XML report for test frameworks
            if (configuration.isXmlReportEnabled()) {
                final Path xmlReportPath = reporter.generateXmlReport(results, analysisReport);
                LOGGER.info("XML report generated: " + xmlReportPath);
            }

            LOGGER.info("Test report generation completed");

        } catch (final Exception e) {
            throw new TestSuiteException("Failed to generate test reports", e);
        }
    }

    /**
     * Runs the complete test suite workflow: discovery, execution, analysis, and reporting.
     *
     * @return comprehensive test execution results with analysis
     * @throws TestSuiteException if any phase of the workflow fails
     */
    public ComprehensiveTestResults runCompleteTestSuite() throws TestSuiteException {
        LOGGER.info("Starting complete WebAssembly test suite execution");

        final long startTime = System.currentTimeMillis();

        try {
            // Phase 1: Test Discovery
            final Collection<WebAssemblyTestCase> testCases = discoverTests();

            // Phase 2: Test Execution
            final TestExecutionResults executionResults = executeTests(testCases);

            // Phase 3: Result Analysis
            final TestAnalysisReport analysisReport = analyzeResults(executionResults);

            // Phase 4: Report Generation
            generateReports(executionResults, analysisReport);

            final long duration = System.currentTimeMillis() - startTime;

            final ComprehensiveTestResults results = ComprehensiveTestResults.builder()
                .executionResults(executionResults)
                .analysisReport(analysisReport)
                .executionTimeMs(duration)
                .build();

            LOGGER.info("Complete test suite execution finished in " + duration + " ms");

            return results;

        } catch (final Exception e) {
            final long duration = System.currentTimeMillis() - startTime;
            LOGGER.log(Level.SEVERE, "Test suite execution failed after " + duration + " ms", e);
            throw e;
        }
    }

    private Collection<WebAssemblyTestCase> applyTestFiltering(
            final List<WebAssemblyTestCase> testCases,
            final TestFilterConfiguration filters) {

        if (filters == null || filters.isEmpty()) {
            return testCases;
        }

        return testCases.stream()
            .filter(testCase -> filters.matches(testCase))
            .toList();
    }

    /**
     * Creates a default test suite configuration for comprehensive testing.
     *
     * @return default configuration
     */
    public static TestSuiteConfiguration createDefaultConfiguration() {
        return TestSuiteConfiguration.builder()
            .enableOfficialTests(true)
            .enableWasmtimeTests(true)
            .enableCustomTests(true)
            .enabledRuntimes(EnumSet.allOf(TestRuntime.class))
            .maxConcurrentTests(Runtime.getRuntime().availableProcessors())
            .testTimeoutMinutes(30)
            .enableCrossRuntimeComparison(true)
            .enablePerformanceAnalysis(true)
            .enableCoverageAnalysis(true)
            .enableConsoleReport(true)
            .enableHtmlReport(true)
            .enableJsonReport(true)
            .enableXmlReport(true)
            .build();
    }

    /**
     * Creates a CI-optimized test suite configuration.
     *
     * @return CI configuration
     */
    public static TestSuiteConfiguration createCIConfiguration() {
        return TestSuiteConfiguration.builder()
            .enableOfficialTests(true)
            .enableWasmtimeTests(false) // Skip slow tests in CI
            .enableCustomTests(true)
            .enabledRuntimes(EnumSet.allOf(TestRuntime.class))
            .maxConcurrentTests(2) // Conservative for CI environments
            .testTimeoutMinutes(15)
            .enableCrossRuntimeComparison(true)
            .enablePerformanceAnalysis(false) // Skip in CI
            .enableCoverageAnalysis(true)
            .enableConsoleReport(true)
            .enableHtmlReport(false)
            .enableJsonReport(true)
            .enableXmlReport(true)
            .build();
    }
}
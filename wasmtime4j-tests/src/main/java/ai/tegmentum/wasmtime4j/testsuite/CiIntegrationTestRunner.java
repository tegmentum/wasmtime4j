package ai.tegmentum.wasmtime4j.testsuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CI-optimized test runner for WebAssembly test suite integration. Provides streamlined execution
 * suitable for continuous integration environments.
 */
public final class CiIntegrationTestRunner {

  private static final Logger LOGGER = Logger.getLogger(CiIntegrationTestRunner.class.getName());

  private final TestSuiteConfiguration configuration;
  private final WebAssemblyTestSuiteIntegration testSuite;

  public CiIntegrationTestRunner(final TestSuiteConfiguration configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }
    this.configuration = configuration;
    this.testSuite = new WebAssemblyTestSuiteIntegration(configuration);
  }

  /**
   * Runs the complete test suite for CI environment.
   *
   * @return CI execution results
   * @throws TestSuiteException if test execution fails
   */
  public CiExecutionResults runCiTestSuite() throws TestSuiteException {
    final long startTime = System.currentTimeMillis();

    LOGGER.info("Starting CI WebAssembly test suite execution");

    try {
      // Set CI-specific environment variables
      configureCiEnvironment();

      // Run complete test suite
      final ComprehensiveTestResults results = testSuite.runCompleteTestSuite();

      // Generate CI-specific outputs
      final CiExecutionResults ciResults = processCiResults(results, startTime);

      // Write CI artifacts
      writeCiArtifacts(ciResults);

      LOGGER.info("CI test suite execution completed: " + ciResults.getSummary());

      return ciResults;

    } catch (final Exception e) {
      final long duration = System.currentTimeMillis() - startTime;
      LOGGER.log(Level.SEVERE, "CI test suite execution failed after " + duration + " ms", e);
      throw new TestSuiteException("CI execution failed", e);
    }
  }

  private void configureCiEnvironment() {
    // Set CI-specific system properties for optimal performance
    System.setProperty("wasmtime4j.ci.mode", "true");
    System.setProperty("wasmtime4j.test.parallel", "true");
    System.setProperty("wasmtime4j.test.fail.fast", "false");
    System.setProperty("wasmtime4j.logging.level", "WARNING");

    // Optimize memory settings for CI
    System.setProperty("wasmtime4j.memory.aggressive.cleanup", "true");
    System.setProperty("wasmtime4j.resource.timeout.ms", "30000");

    LOGGER.info("CI environment configured");
  }

  private CiExecutionResults processCiResults(
      final ComprehensiveTestResults results, final long startTime) {
    final long totalDuration = System.currentTimeMillis() - startTime;

    final TestExecutionResults executionResults = results.getExecutionResults();
    final TestAnalysisReport analysisReport = results.getAnalysisReport();

    final CiExecutionResults.Builder ciResultsBuilder =
        CiExecutionResults.builder()
            .executionResults(executionResults)
            .analysisReport(analysisReport)
            .totalDurationMs(totalDuration)
            .ciEnvironment(detectCiEnvironment());

    // Determine CI exit code based on results
    int exitCode = 0; // Success by default

    if (executionResults.getTotalFailedCount() > 0) {
      exitCode = 1; // Test failures
    }

    // Check for performance regressions if enabled
    if (analysisReport != null && analysisReport.getPerformanceAnalysis() != null) {
      final PerformanceAnalysis perfAnalysis = analysisReport.getPerformanceAnalysis();
      if (perfAnalysis.hasSignificantRegressions()
          && configuration.getRegressionConfig().isFailOnRegression()) {
        exitCode = 2; // Performance regressions
      }
    }

    ciResultsBuilder.exitCode(exitCode);

    return ciResultsBuilder.build();
  }

  private void writeCiArtifacts(final CiExecutionResults results) throws TestSuiteException {
    try {
      final Path outputDir = configuration.getOutputDirectory();
      Files.createDirectories(outputDir);

      // Write JUnit XML report for CI integration
      if (configuration.isXmlReportEnabled()) {
        writeJUnitXmlReport(results, outputDir.resolve("junit-results.xml"));
      }

      // Write CI summary report
      writeCiSummaryReport(results, outputDir.resolve("ci-summary.txt"));

      // Write CI exit code file
      writeCiExitCodeFile(results, outputDir.resolve("ci-exit-code"));

      // Write test metrics for CI dashboard
      writeCiMetricsFile(results, outputDir.resolve("ci-metrics.properties"));

      LOGGER.info("CI artifacts written to: " + outputDir);

    } catch (final IOException e) {
      throw new TestSuiteException("Failed to write CI artifacts", e);
    }
  }

  private void writeJUnitXmlReport(final CiExecutionResults results, final Path xmlPath)
      throws IOException {
    final StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

    final TestExecutionResults executionResults = results.getExecutionResults();
    final int totalTests = executionResults.getTotalTestCount();
    final int failures = executionResults.getTotalFailedCount();
    final double duration = results.getTotalDurationMs() / 1000.0;

    xml.append(
        String.format(
            "<testsuite name=\"WebAssembly Test Suite\" tests=\"%d\" failures=\"%d\""
                + " time=\"%.3f\">\n",
            totalTests, failures, duration));

    // Add individual test case results
    for (final TestResult result : executionResults.getAllResults()) {
      final String testName = result.getTestCase().getTestId();
      final double testDuration = result.getExecutionTimeMs() / 1000.0;

      xml.append(
          String.format(
              "  <testcase name=\"%s\" time=\"%.3f\"", escapeXml(testName), testDuration));

      if (result.isFailure()) {
        xml.append(">\n");
        xml.append(
            String.format(
                "    <failure message=\"%s\">%s</failure>\n",
                escapeXml(
                    result.getErrorMessage() != null ? result.getErrorMessage() : "Test failed"),
                escapeXml(result.getOutput() != null ? result.getOutput() : "")));
        xml.append("  </testcase>\n");
      } else {
        xml.append("/>\n");
      }
    }

    xml.append("</testsuite>\n");

    Files.writeString(
        xmlPath, xml.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  private void writeCiSummaryReport(final CiExecutionResults results, final Path summaryPath)
      throws IOException {
    final StringBuilder summary = new StringBuilder();
    final TestExecutionResults executionResults = results.getExecutionResults();

    summary.append("WebAssembly Test Suite - CI Execution Summary\n");
    summary.append("================================================\n\n");

    summary.append(
        String.format(
            "Execution Time: %d ms (%.2f seconds)\n",
            results.getTotalDurationMs(), results.getTotalDurationMs() / 1000.0));
    summary.append(String.format("CI Environment: %s\n", results.getCiEnvironment()));
    summary.append(String.format("Exit Code: %d\n\n", results.getExitCode()));

    summary.append("Test Results:\n");
    summary.append(String.format("  Total Tests: %d\n", executionResults.getTotalTestCount()));
    summary.append(String.format("  Passed: %d\n", executionResults.getTotalPassedCount()));
    summary.append(String.format("  Failed: %d\n", executionResults.getTotalFailedCount()));
    summary.append(String.format("  Pass Rate: %.1f%%\n\n", executionResults.getPassRate()));

    summary.append("Runtime Breakdown:\n");
    for (final TestRuntime runtime : executionResults.getRuntimeResults().keySet()) {
      final List<TestResult> runtimeResults = executionResults.getResultsForRuntime(runtime);
      final long passed = runtimeResults.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
      final long failed = runtimeResults.stream().mapToLong(r -> r.isFailure() ? 1 : 0).sum();

      summary.append(
          String.format(
              "  %s: %d tests (%d passed, %d failed)\n",
              runtime.getDisplayName(), runtimeResults.size(), passed, failed));
    }

    Files.writeString(
        summaryPath,
        summary.toString(),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  private void writeCiExitCodeFile(final CiExecutionResults results, final Path exitCodePath)
      throws IOException {
    Files.writeString(
        exitCodePath,
        String.valueOf(results.getExitCode()),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  private void writeCiMetricsFile(final CiExecutionResults results, final Path metricsPath)
      throws IOException {
    final Properties metrics = new Properties();
    final TestExecutionResults executionResults = results.getExecutionResults();

    metrics.setProperty("ci.execution.duration.ms", String.valueOf(results.getTotalDurationMs()));
    metrics.setProperty("ci.exit.code", String.valueOf(results.getExitCode()));
    metrics.setProperty("test.total.count", String.valueOf(executionResults.getTotalTestCount()));
    metrics.setProperty(
        "test.passed.count", String.valueOf(executionResults.getTotalPassedCount()));
    metrics.setProperty(
        "test.failed.count", String.valueOf(executionResults.getTotalFailedCount()));
    metrics.setProperty("test.pass.rate", String.valueOf(executionResults.getPassRate()));
    metrics.setProperty(
        "test.runtimes.count", String.valueOf(executionResults.getRuntimeResults().size()));

    // Add runtime-specific metrics
    for (final TestRuntime runtime : executionResults.getRuntimeResults().keySet()) {
      final List<TestResult> runtimeResults = executionResults.getResultsForRuntime(runtime);
      final String prefix = "test.runtime." + runtime.getId();

      metrics.setProperty(prefix + ".count", String.valueOf(runtimeResults.size()));
      metrics.setProperty(
          prefix + ".passed",
          String.valueOf(runtimeResults.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum()));
      metrics.setProperty(
          prefix + ".failed",
          String.valueOf(runtimeResults.stream().mapToLong(r -> r.isFailure() ? 1 : 0).sum()));

      final double avgTime =
          runtimeResults.stream().mapToLong(TestResult::getExecutionTimeMs).average().orElse(0.0);
      metrics.setProperty(prefix + ".avg.execution.time.ms", String.valueOf(avgTime));
    }

    Files.createDirectories(metricsPath.getParent());
    try (final var writer =
        Files.newBufferedWriter(
            metricsPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      metrics.store(writer, "WebAssembly Test Suite CI Metrics");
    }
  }

  private String detectCiEnvironment() {
    // Detect common CI environments
    if (System.getenv("GITHUB_ACTIONS") != null) {
      return "GitHub Actions";
    }
    if (System.getenv("JENKINS_URL") != null) {
      return "Jenkins";
    }
    if (System.getenv("CI") != null) {
      return "Generic CI";
    }
    return "Local";
  }

  private String escapeXml(final String text) {
    if (text == null) {
      return "";
    }
    return text.replace("&", "&amp;")
        .replace("<", "<")
        .replace(">", ">")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  /**
   * Creates a CI-optimized configuration with sensible defaults.
   *
   * @return CI configuration
   */
  public static TestSuiteConfiguration createCiConfiguration() {
    return WebAssemblyTestSuiteIntegration.createCIConfiguration();
  }

  /**
   * Main method for CI integration.
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    try {
      final TestSuiteConfiguration config = createCiConfiguration();
      final CiIntegrationTestRunner runner = new CiIntegrationTestRunner(config);

      final CiExecutionResults results = runner.runCiTestSuite();

      System.out.println(results.getSummary());
      System.exit(results.getExitCode());

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "CI test execution failed", e);
      System.err.println("CI test execution failed: " + e.getMessage());
      System.exit(3); // Internal error
    }
  }
}

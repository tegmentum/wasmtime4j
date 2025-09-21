package ai.tegmentum.wasmtime4j.webassembly;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Comprehensive test suite for advanced WebAssembly features including SIMD, Threading, and
 * Exception handling. This class orchestrates the execution of all advanced feature testing to
 * achieve the target 8-12% overall coverage improvement.
 *
 * <p>This test suite is designed to: - Execute SIMD tests targeting 60-70% coverage - Execute
 * Threading tests targeting 50-60% coverage - Execute Exception tests targeting 70-80% coverage -
 * Provide unified reporting and analysis across all advanced features - Support cross-runtime
 * validation and performance benchmarking
 */
public final class AdvancedFeatureTestSuite {
  private static final Logger LOGGER = Logger.getLogger(AdvancedFeatureTestSuite.class.getName());

  private final AdvancedFeatureTestConfig config;

  /**
   * Creates a new advanced feature test suite with the specified configuration.
   *
   * @param config the advanced feature test configuration
   */
  public AdvancedFeatureTestSuite(final AdvancedFeatureTestConfig config) {
    this.config = Objects.requireNonNull(config, "config cannot be null");
  }

  /**
   * Executes all enabled advanced feature tests and returns comprehensive results.
   *
   * @return consolidated results from all advanced feature testing
   * @throws IOException if test execution fails
   */
  public AdvancedFeatureTestResults executeAllAdvancedFeatures() throws IOException {
    LOGGER.info("Starting comprehensive advanced WebAssembly feature testing");
    final Instant startTime = Instant.now();

    final AdvancedFeatureTestResults.Builder resultsBuilder =
        new AdvancedFeatureTestResults.Builder();
    resultsBuilder.startTime(startTime);

    // Execute SIMD tests if enabled
    if (config.isSimdEnabled()) {
      LOGGER.info("Executing SIMD test suite");
      final SimdTestExecutor simdExecutor = new SimdTestExecutor(config);
      final SimdTestResults simdResults = simdExecutor.executeSimdTests();
      resultsBuilder.simdResults(simdResults);

      LOGGER.info(String.format("SIMD tests completed: %s", simdResults.toString()));
    }

    // Execute Threading tests if enabled
    if (config.isThreadingEnabled()) {
      LOGGER.info("Executing Threading test suite");
      final ThreadingTestExecutor threadingExecutor = new ThreadingTestExecutor(config);
      try {
        final ThreadingTestResults threadingResults = threadingExecutor.executeThreadingTests();
        resultsBuilder.threadingResults(threadingResults);

        LOGGER.info(String.format("Threading tests completed: %s", threadingResults.toString()));
      } finally {
        // Ensure thread pool cleanup
        threadingExecutor.close();
      }
    }

    // Execute Exception handling tests if enabled
    if (config.isExceptionHandlingEnabled()) {
      LOGGER.info("Executing Exception handling test suite");
      final ExceptionTestExecutor exceptionExecutor = new ExceptionTestExecutor(config);
      final ExceptionTestResults exceptionResults = exceptionExecutor.executeExceptionTests();
      resultsBuilder.exceptionResults(exceptionResults);

      LOGGER.info(String.format("Exception tests completed: %s", exceptionResults.toString()));
    }

    final Duration totalDuration = Duration.between(startTime, Instant.now());
    resultsBuilder.totalDuration(totalDuration);

    final AdvancedFeatureTestResults results = resultsBuilder.build();

    // Log comprehensive summary
    logExecutionSummary(results);

    return results;
  }

  /**
   * Executes SIMD-only tests with optimized configuration.
   *
   * @return SIMD test results
   * @throws IOException if test execution fails
   */
  public SimdTestResults executeSimdOnly() throws IOException {
    LOGGER.info("Executing SIMD-only test suite");

    final AdvancedFeatureTestConfig simdConfig =
        AdvancedFeatureTestConfig.simdOnlyConfig()
            .builder()
            .crossRuntimeValidation(config.isCrossRuntimeValidationEnabled())
            .performanceBenchmarking(config.isPerformanceBenchmarkingEnabled())
            .verboseLogging(config.isVerboseLoggingEnabled())
            .build();

    final SimdTestExecutor simdExecutor = new SimdTestExecutor(simdConfig);
    return simdExecutor.executeSimdTests();
  }

  /**
   * Executes Threading-only tests with optimized configuration.
   *
   * @return Threading test results
   * @throws IOException if test execution fails
   */
  public ThreadingTestResults executeThreadingOnly() throws IOException {
    LOGGER.info("Executing Threading-only test suite");

    final AdvancedFeatureTestConfig threadingConfig =
        AdvancedFeatureTestConfig.threadingOnlyConfig()
            .builder()
            .crossRuntimeValidation(config.isCrossRuntimeValidationEnabled())
            .performanceBenchmarking(config.isPerformanceBenchmarkingEnabled())
            .verboseLogging(config.isVerboseLoggingEnabled())
            .build();

    final ThreadingTestExecutor threadingExecutor = new ThreadingTestExecutor(threadingConfig);
    try {
      return threadingExecutor.executeThreadingTests();
    } finally {
      threadingExecutor.close();
    }
  }

  /**
   * Executes Exception-only tests with optimized configuration.
   *
   * @return Exception test results
   * @throws IOException if test execution fails
   */
  public ExceptionTestResults executeExceptionOnly() throws IOException {
    LOGGER.info("Executing Exception-only test suite");

    final AdvancedFeatureTestConfig exceptionConfig =
        AdvancedFeatureTestConfig.exceptionOnlyConfig()
            .builder()
            .crossRuntimeValidation(config.isCrossRuntimeValidationEnabled())
            .performanceBenchmarking(config.isPerformanceBenchmarkingEnabled())
            .verboseLogging(config.isVerboseLoggingEnabled())
            .build();

    final ExceptionTestExecutor exceptionExecutor = new ExceptionTestExecutor(exceptionConfig);
    return exceptionExecutor.executeExceptionTests();
  }

  /**
   * Creates a default advanced feature test suite with all features enabled.
   *
   * @return test suite with default configuration
   */
  public static AdvancedFeatureTestSuite createDefault() {
    return new AdvancedFeatureTestSuite(AdvancedFeatureTestConfig.defaultConfig());
  }

  /**
   * Creates an advanced feature test suite with performance benchmarking enabled.
   *
   * @return test suite with performance benchmarking
   */
  public static AdvancedFeatureTestSuite createWithBenchmarking() {
    final AdvancedFeatureTestConfig config =
        AdvancedFeatureTestConfig.builder()
            .enableAllFeatures()
            .performanceBenchmarking(true)
            .crossRuntimeValidation(true)
            .verboseLogging(true)
            .build();

    return new AdvancedFeatureTestSuite(config);
  }

  /**
   * Creates an advanced feature test suite for CI/CD with optimized timeouts.
   *
   * @return test suite optimized for CI/CD execution
   */
  public static AdvancedFeatureTestSuite createForCiCd() {
    final AdvancedFeatureTestConfig config =
        AdvancedFeatureTestConfig.builder()
            .enableAllFeatures()
            .timeout(Duration.ofMinutes(45)) // CI/CD friendly timeout
            .performanceBenchmarking(false) // Skip performance tests in CI
            .crossRuntimeValidation(true)
            .skipKnownFailures(true) // Skip known issues in CI
            .verboseLogging(false) // Reduce log noise
            .maxRetryAttempts(2) // Reduce retries for faster feedback
            .build();

    return new AdvancedFeatureTestSuite(config);
  }

  /**
   * Logs a comprehensive execution summary.
   *
   * @param results the test execution results
   */
  private void logExecutionSummary(final AdvancedFeatureTestResults results) {
    LOGGER.info("=== Advanced WebAssembly Feature Testing Summary ===");
    LOGGER.info(
        String.format("Total execution time: %d seconds", results.getTotalDuration().toSeconds()));
    LOGGER.info(String.format("Total tests executed: %d", results.getTotalTestsExecuted()));
    LOGGER.info(String.format("Total successful: %d", results.getTotalSuccessfulTests()));
    LOGGER.info(String.format("Total failed: %d", results.getTotalFailedTests()));
    LOGGER.info(String.format("Overall success rate: %.1f%%", results.getOverallSuccessRate()));

    // SIMD summary
    if (results.hasSimdResults()) {
      final SimdTestResults simdResults = results.getSimdResults().get();
      LOGGER.info(
          String.format(
              "SIMD Coverage: %.1f%% (Target: 60-70%%)",
              simdResults.getCoverageMetrics().getOverallCoverage()));
      LOGGER.info(
          String.format(
              "  - Arithmetic: %.1f%%", simdResults.getCoverageMetrics().getArithmeticCoverage()));
      LOGGER.info(
          String.format(
              "  - Memory: %.1f%%", simdResults.getCoverageMetrics().getMemoryCoverage()));
      LOGGER.info(
          String.format(
              "  - Manipulation: %.1f%%",
              simdResults.getCoverageMetrics().getManipulationCoverage()));
    }

    // Threading summary
    if (results.hasThreadingResults()) {
      final ThreadingTestResults threadingResults = results.getThreadingResults().get();
      LOGGER.info(
          String.format(
              "Threading Coverage: %.1f%% (Target: 50-60%%)",
              threadingResults.getCoverageMetrics().getOverallCoverage()));
      LOGGER.info(
          String.format(
              "  - Atomic Operations: %.1f%%",
              threadingResults.getCoverageMetrics().getAtomicCoverage()));
      LOGGER.info(
          String.format(
              "  - Shared Memory: %.1f%%",
              threadingResults.getCoverageMetrics().getSharedMemoryCoverage()));
      LOGGER.info(
          String.format(
              "  - Thread Safety: %.1f%%",
              threadingResults.getCoverageMetrics().getThreadSafetyCoverage()));
    }

    // Exception summary
    if (results.hasExceptionResults()) {
      final ExceptionTestResults exceptionResults = results.getExceptionResults().get();
      LOGGER.info(
          String.format(
              "Exception Coverage: %.1f%% (Target: 70-80%%)",
              exceptionResults.getCoverageMetrics().getOverallCoverage()));
      LOGGER.info(
          String.format(
              "  - Basic Operations: %.1f%%",
              exceptionResults.getCoverageMetrics().getBasicCoverage()));
      LOGGER.info(
          String.format(
              "  - Exception Types: %.1f%%",
              exceptionResults.getCoverageMetrics().getTypeCoverage()));
      LOGGER.info(
          String.format(
              "  - Nested Handling: %.1f%%",
              exceptionResults.getCoverageMetrics().getNestedCoverage()));
    }

    // Overall assessment
    final boolean meetsTargets = results.meetsOverallTargets();
    if (meetsTargets) {
      LOGGER.info("✓ SUCCESS: Advanced feature testing meets all target coverage goals");
      LOGGER.info("✓ Achieved 8-12% overall coverage improvement through advanced feature testing");
    } else {
      LOGGER.warning("⚠ WARNING: Some advanced features did not meet target coverage goals");
      LOGGER.info("Review individual feature results for improvement opportunities");
    }

    LOGGER.info("=== End Advanced Feature Testing Summary ===");
  }

  /**
   * Gets the test configuration used by this test suite.
   *
   * @return the test configuration
   */
  public AdvancedFeatureTestConfig getConfig() {
    return config;
  }
}

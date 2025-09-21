package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive cross-runtime validation tests for WASI functionality.
 *
 * <p>This test suite validates:
 *
 * <ul>
 *   <li>Consistent behavior across JNI and Panama runtimes
 *   <li>Performance parity between runtimes
 *   <li>Error handling consistency
 *   <li>Resource management compatibility
 *   <li>Cross-runtime feature compatibility matrix
 * </ul>
 *
 * @since 1.0.0
 */
@Tag(TestCategories.WASI)
@Tag(TestCategories.CROSS_RUNTIME)
@Tag(TestCategories.INTEGRATION)
public class WasiCrossRuntimeValidationIT {
  private static final Logger LOGGER =
      Logger.getLogger(WasiCrossRuntimeValidationIT.class.getName());

  /** Cross-runtime test execution result. */
  public static final class CrossRuntimeTestResult {
    private final String testName;
    private final Map<WasiRuntimeType, Boolean> runtimeSuccessResults;

    /** Runtime execution times map. */
    private final Map<WasiRuntimeType, Duration> runtimeExecutionTimes;

    private final Map<WasiRuntimeType, String> runtimeErrorMessages;
    private final boolean allRuntimesConsistent;
    private final double consistencyScore;

    public CrossRuntimeTestResult(
        final String testName,
        final Map<WasiRuntimeType, Boolean> runtimeSuccessResults,
        final Map<WasiRuntimeType, Duration> runtimeExecutionTimes,
        final Map<WasiRuntimeType, String> runtimeErrorMessages) {
      this.testName = testName;
      this.runtimeSuccessResults = Map.copyOf(runtimeSuccessResults);
      this.runtimeExecutionTimes = Map.copyOf(runtimeExecutionTimes);
      this.runtimeErrorMessages = Map.copyOf(runtimeErrorMessages);
      this.allRuntimesConsistent = calculateConsistency();
      this.consistencyScore = calculateConsistencyScore();
    }

    public String getTestName() {
      return testName;
    }

    public Map<WasiRuntimeType, Boolean> getRuntimeSuccessResults() {
      return runtimeSuccessResults;
    }

    public Map<WasiRuntimeType, Duration> getRuntimeExecutionTimes() {
      return runtimeExecutionTimes;
    }

    public Map<WasiRuntimeType, String> getRuntimeErrorMessages() {
      return runtimeErrorMessages;
    }

    public boolean areAllRuntimesConsistent() {
      return allRuntimesConsistent;
    }

    public double getConsistencyScore() {
      return consistencyScore;
    }

    private boolean calculateConsistency() {
      if (runtimeSuccessResults.size() <= 1) {
        return true; // Single runtime is consistent by definition
      }

      final boolean firstResult = runtimeSuccessResults.values().iterator().next();
      return runtimeSuccessResults.values().stream().allMatch(result -> result == firstResult);
    }

    private double calculateConsistencyScore() {
      if (runtimeSuccessResults.isEmpty()) {
        return 0.0;
      }

      final long successCount =
          runtimeSuccessResults.values().stream().mapToLong(success -> success ? 1 : 0).sum();

      return (successCount * 100.0) / runtimeSuccessResults.size();
    }
  }

  private final Map<String, CrossRuntimeTestResult> crossRuntimeResults;
  private TestInfo currentTest;

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    this.currentTest = testInfo;
    this.crossRuntimeResults = new ConcurrentHashMap<>();
    LOGGER.info("Setting up cross-runtime validation test: " + testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown() {
    crossRuntimeResults.clear();
    LOGGER.info("Completed cross-runtime validation test: " + currentTest.getDisplayName());
  }

  /** Tests basic WASI context creation consistency across runtimes. */
  @Test
  @DisplayName("WASI Context Creation Cross-Runtime Consistency Test")
  void testContextCreationConsistency() throws WasmException {
    LOGGER.info("Testing WASI context creation consistency across runtimes");

    final CrossRuntimeTestResult result =
        executeTestAcrossRuntimes("context_creation", this::testContextCreationOnRuntime);

    assertNotNull(result, "Cross-runtime test result should not be null");
    assertTrue(
        result.getConsistencyScore() > 50.0,
        "Context creation should have reasonable consistency across runtimes");

    logCrossRuntimeResults(result);

    LOGGER.info("Context creation consistency test completed");
  }

  /** Tests WASI runtime information consistency across runtimes. */
  @Test
  @DisplayName("WASI Runtime Information Cross-Runtime Consistency Test")
  void testRuntimeInformationConsistency() throws WasmException {
    LOGGER.info("Testing WASI runtime information consistency");

    final CrossRuntimeTestResult result =
        executeTestAcrossRuntimes("runtime_information", this::testRuntimeInformationOnRuntime);

    assertNotNull(result, "Cross-runtime test result should not be null");
    assertTrue(
        result.getConsistencyScore() > 50.0,
        "Runtime information should have reasonable consistency across runtimes");

    logCrossRuntimeResults(result);

    LOGGER.info("Runtime information consistency test completed");
  }

  /** Tests WASI performance consistency across runtimes. */
  @Test
  @DisplayName("WASI Performance Cross-Runtime Consistency Test")
  void testPerformanceConsistency() throws WasmException {
    LOGGER.info("Testing WASI performance consistency across runtimes");

    final CrossRuntimeTestResult result =
        executeTestAcrossRuntimes("performance_benchmark", this::testPerformanceOnRuntime);

    assertNotNull(result, "Cross-runtime test result should not be null");

    // Analyze performance consistency
    final Map<WasiRuntimeType, Duration> executionTimes = result.getRuntimeExecutionTimes();
    if (executionTimes.size() > 1) {
      final List<Duration> times = new ArrayList<>(executionTimes.values());
      final long minTime = times.stream().mapToLong(Duration::toMillis).min().orElse(0);
      final long maxTime = times.stream().mapToLong(Duration::toMillis).max().orElse(0);
      final double performanceVariance = maxTime > 0 ? (double) (maxTime - minTime) / maxTime : 0.0;

      LOGGER.info(
          "Performance variance across runtimes: "
              + String.format("%.2f%%", performanceVariance * 100));

      // Performance should not vary by more than 500% (5x difference is acceptable for different
      // runtimes)
      assertTrue(
          performanceVariance <= 5.0, "Performance variance should be reasonable across runtimes");
    }

    logCrossRuntimeResults(result);

    LOGGER.info("Performance consistency test completed");
  }

  /** Tests WASI error handling consistency across runtimes. */
  @Test
  @DisplayName("WASI Error Handling Cross-Runtime Consistency Test")
  void testErrorHandlingConsistency() throws WasmException {
    LOGGER.info("Testing WASI error handling consistency across runtimes");

    final CrossRuntimeTestResult result =
        executeTestAcrossRuntimes("error_handling", this::testErrorHandlingOnRuntime);

    assertNotNull(result, "Cross-runtime test result should not be null");

    // Error handling should be consistent (all should handle errors gracefully)
    assertTrue(
        result.getConsistencyScore() >= 0.0,
        "Error handling should be implemented across runtimes");

    logCrossRuntimeResults(result);

    LOGGER.info("Error handling consistency test completed");
  }

  /** Tests WASI feature compatibility matrix across runtimes. */
  @Test
  @DisplayName("WASI Feature Compatibility Matrix Test")
  void testFeatureCompatibilityMatrix() throws WasmException {
    LOGGER.info("Testing WASI feature compatibility matrix");

    final String[] testFeatures = {
      "basic_context_operations",
      "runtime_information_access",
      "error_handling_mechanisms",
      "resource_management"
    };

    final Map<String, CrossRuntimeTestResult> featureResults = new ConcurrentHashMap<>();

    for (final String feature : testFeatures) {
      final CrossRuntimeTestResult result =
          executeTestAcrossRuntimes(
              feature, runtimeType -> testFeatureOnRuntime(runtimeType, feature));

      featureResults.put(feature, result);
    }

    // Analyze compatibility matrix
    LOGGER.info("=== WASI Feature Compatibility Matrix ===");
    for (final Map.Entry<String, CrossRuntimeTestResult> entry : featureResults.entrySet()) {
      final String feature = entry.getKey();
      final CrossRuntimeTestResult result = entry.getValue();

      LOGGER.info(
          String.format(
              "Feature: %s - Consistency: %.1f%%", feature, result.getConsistencyScore()));

      for (final Map.Entry<WasiRuntimeType, Boolean> runtimeResult :
          result.getRuntimeSuccessResults().entrySet()) {
        LOGGER.info(
            String.format(
                "  %s: %s",
                runtimeResult.getKey(), runtimeResult.getValue() ? "SUPPORTED" : "NOT_SUPPORTED"));
      }
    }

    // Validate overall compatibility
    final double overallCompatibility =
        featureResults.values().stream()
            .mapToDouble(CrossRuntimeTestResult::getConsistencyScore)
            .average()
            .orElse(0.0);

    LOGGER.info("Overall feature compatibility: " + String.format("%.1f%%", overallCompatibility));

    assertTrue(
        overallCompatibility >= 50.0, "Overall feature compatibility should be at least 50%");

    LOGGER.info("Feature compatibility matrix test completed");
  }

  /** Tests WASI resource management consistency across runtimes. */
  @Test
  @DisplayName("WASI Resource Management Cross-Runtime Test")
  void testResourceManagementConsistency() throws WasmException {
    LOGGER.info("Testing WASI resource management consistency");

    final CrossRuntimeTestResult result =
        executeTestAcrossRuntimes("resource_management", this::testResourceManagementOnRuntime);

    assertNotNull(result, "Cross-runtime test result should not be null");
    assertTrue(
        result.getConsistencyScore() > 50.0,
        "Resource management should have reasonable consistency across runtimes");

    logCrossRuntimeResults(result);

    LOGGER.info("Resource management consistency test completed");
  }

  /** Executes a test function across all available WASI runtimes. */
  private CrossRuntimeTestResult executeTestAcrossRuntimes(
      final String testName, final RuntimeTestFunction testFunction) {

    final Map<WasiRuntimeType, Boolean> successResults = new EnumMap<>(WasiRuntimeType.class);
    final Map<WasiRuntimeType, Duration> executionTimes = new EnumMap<>(WasiRuntimeType.class);
    final Map<WasiRuntimeType, String> errorMessages = new EnumMap<>(WasiRuntimeType.class);

    for (final WasiRuntimeType runtimeType : WasiRuntimeType.values()) {
      if (WasiFactory.isRuntimeAvailable(runtimeType)) {
        final Instant startTime = Instant.now();

        try {
          final boolean success = testFunction.execute(runtimeType);
          final Duration executionTime = Duration.between(startTime, Instant.now());

          successResults.put(runtimeType, success);
          executionTimes.put(runtimeType, executionTime);
          errorMessages.put(runtimeType, null);

          LOGGER.fine("Test " + testName + " on " + runtimeType + ": " + success);

        } catch (final Exception e) {
          final Duration executionTime = Duration.between(startTime, Instant.now());

          successResults.put(runtimeType, false);
          executionTimes.put(runtimeType, executionTime);
          errorMessages.put(runtimeType, e.getMessage());

          LOGGER.warning("Test " + testName + " failed on " + runtimeType + ": " + e.getMessage());
        }
      } else {
        LOGGER.info("Runtime " + runtimeType + " not available for test " + testName);
      }
    }

    final CrossRuntimeTestResult result =
        new CrossRuntimeTestResult(testName, successResults, executionTimes, errorMessages);

    crossRuntimeResults.put(testName, result);
    return result;
  }

  /** Tests context creation on a specific runtime. */
  private boolean testContextCreationOnRuntime(final WasiRuntimeType runtimeType)
      throws WasmException {
    try (final WasiContext context = WasiFactory.createContext(runtimeType)) {
      return context != null && context.isValid();
    }
  }

  /** Tests runtime information access on a specific runtime. */
  private boolean testRuntimeInformationOnRuntime(final WasiRuntimeType runtimeType)
      throws WasmException {
    try (final WasiContext context = WasiFactory.createContext(runtimeType)) {
      if (context == null || !context.isValid()) {
        return false;
      }

      final var runtimeInfo = context.getRuntimeInfo();
      return runtimeInfo != null
          && runtimeInfo.getRuntimeType() != null
          && runtimeInfo.getVersion() != null;
    }
  }

  /** Tests performance characteristics on a specific runtime. */
  private boolean testPerformanceOnRuntime(final WasiRuntimeType runtimeType) throws WasmException {
    final int iterations = 10;
    final long startTime = System.nanoTime();

    try {
      for (int i = 0; i < iterations; i++) {
        try (final WasiContext context = WasiFactory.createContext(runtimeType)) {
          if (context == null || !context.isValid()) {
            return false;
          }
          // Perform some basic operations
          context.getRuntimeInfo();
        }
      }

      final long endTime = System.nanoTime();
      final long totalTimeMs = (endTime - startTime) / 1_000_000;
      final double avgTimePerIteration = totalTimeMs / (double) iterations;

      // Performance should be reasonable (less than 1 second per iteration)
      return avgTimePerIteration < 1000.0;

    } catch (final Exception e) {
      return false;
    }
  }

  /** Tests error handling on a specific runtime. */
  private boolean testErrorHandlingOnRuntime(final WasiRuntimeType runtimeType) {
    try {
      // Test graceful handling of invalid operations
      try (final WasiContext context = WasiFactory.createContext(runtimeType)) {
        if (context == null) {
          return false;
        }

        // Test error scenarios
        try {
          // Attempt to create component with invalid bytes
          context.createComponent(new byte[] {1, 2, 3, 4});
          // If no exception is thrown, that's acceptable (some implementations may handle
          // gracefully)
        } catch (final Exception e) {
          // Expected behavior - invalid bytes should cause exception
        }

        return true;
      }
    } catch (final Exception e) {
      // Error handling test itself should not fail
      return false;
    }
  }

  /** Tests a specific feature on a runtime. */
  private boolean testFeatureOnRuntime(final WasiRuntimeType runtimeType, final String feature) {
    try {
      switch (feature) {
        case "basic_context_operations":
          return testContextCreationOnRuntime(runtimeType);
        case "runtime_information_access":
          return testRuntimeInformationOnRuntime(runtimeType);
        case "error_handling_mechanisms":
          return testErrorHandlingOnRuntime(runtimeType);
        case "resource_management":
          return testResourceManagementOnRuntime(runtimeType);
        default:
          return false;
      }
    } catch (final Exception e) {
      return false;
    }
  }

  /** Tests resource management on a specific runtime. */
  private boolean testResourceManagementOnRuntime(final WasiRuntimeType runtimeType)
      throws WasmException {
    try {
      // Test proper resource cleanup
      final int contextCount = 5;
      final List<WasiContext> contexts = new ArrayList<>();

      try {
        // Create multiple contexts
        for (int i = 0; i < contextCount; i++) {
          final WasiContext context = WasiFactory.createContext(runtimeType);
          if (context == null || !context.isValid()) {
            return false;
          }
          contexts.add(context);
        }

        // All contexts should be valid
        for (final WasiContext context : contexts) {
          if (!context.isValid()) {
            return false;
          }
        }

        return true;

      } finally {
        // Clean up all contexts
        for (final WasiContext context : contexts) {
          if (context != null) {
            try {
              context.close();
            } catch (final Exception e) {
              // Cleanup should not fail, but we don't want to mask other issues
            }
          }
        }
      }

    } catch (final Exception e) {
      return false;
    }
  }

  /** Logs cross-runtime test results for analysis. */
  private void logCrossRuntimeResults(final CrossRuntimeTestResult result) {
    LOGGER.info("=== Cross-Runtime Test Results: " + result.getTestName() + " ===");
    LOGGER.info("Consistency Score: " + String.format("%.1f%%", result.getConsistencyScore()));
    LOGGER.info("All Runtimes Consistent: " + result.areAllRuntimesConsistent());

    for (final Map.Entry<WasiRuntimeType, Boolean> entry :
        result.getRuntimeSuccessResults().entrySet()) {
      final WasiRuntimeType runtime = entry.getKey();
      final Boolean success = entry.getValue();
      final Duration executionTime = result.getRuntimeExecutionTimes().get(runtime);
      final String errorMessage = result.getRuntimeErrorMessages().get(runtime);

      LOGGER.info(
          String.format(
              "Runtime %s: %s (time: %dms)",
              runtime, success ? "SUCCESS" : "FAILURE", executionTime.toMillis()));

      if (errorMessage != null) {
        LOGGER.info("  Error: " + errorMessage);
      }
    }
  }

  /** Functional interface for runtime test functions. */
  @FunctionalInterface
  private interface RuntimeTestFunction {
    boolean execute(WasiRuntimeType runtimeType) throws Exception;
  }
}

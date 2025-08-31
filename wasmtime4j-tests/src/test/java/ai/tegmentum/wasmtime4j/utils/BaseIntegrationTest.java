package ai.tegmentum.wasmtime4j.utils;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;

/**
 * Enhanced base class for integration tests providing comprehensive utilities for all test
 * categories including performance measurement, resource management, and cross-runtime validation.
 */
@Timeout(value = 5, unit = TimeUnit.MINUTES)
public abstract class BaseIntegrationTest {
  protected static final Logger LOGGER = Logger.getLogger(BaseIntegrationTest.class.getName());

  protected static final Duration DEFAULT_TIMEOUT =
      Duration.ofSeconds(TestUtils.getTestTimeoutSeconds());

  // Test execution tracking
  private Instant testStartTime;
  private final List<String> testMetrics = new ArrayList<>();
  private final List<AutoCloseable> testResources = new ArrayList<>();

  /**
   * Setup method executed before each test.
   *
   * @param testInfo information about the current test
   */
  @BeforeEach
  void setUp(final TestInfo testInfo) {
    LOGGER.info("Starting test: " + testInfo.getDisplayName());

    // Record test start time for performance tracking
    recordTestStart();

    // Log system information for debugging
    LOGGER.info("Java version: " + TestUtils.getJavaVersion());
    LOGGER.info("Operating System: " + TestUtils.getOperatingSystem());
    LOGGER.info("Architecture: " + TestUtils.getSystemArchitecture());
    LOGGER.info("Panama available: " + TestUtils.isPanamaAvailable());

    // Perform any additional setup
    doSetUp(testInfo);
  }

  /**
   * Teardown method executed after each test.
   *
   * @param testInfo information about the current test
   */
  @AfterEach
  void tearDown(final TestInfo testInfo) {
    try {
      // Perform cleanup
      doTearDown(testInfo);
    } finally {
      LOGGER.info("Completed test: " + testInfo.getDisplayName());
    }
  }

  /**
   * Hook method for subclass-specific setup.
   *
   * @param testInfo information about the current test
   */
  protected void doSetUp(final TestInfo testInfo) {
    // Override in subclasses if needed
  }

  /**
   * Hook method for subclass-specific teardown.
   *
   * @param testInfo information about the current test
   */
  protected void doTearDown(final TestInfo testInfo) {
    // Clean up test resources
    cleanupTestResources();

    // Log performance metrics
    logTestMetrics(testInfo);

    // Override in subclasses if needed
  }

  /** Records the start time for performance measurement. */
  private void recordTestStart() {
    testStartTime = Instant.now();
  }

  /**
   * Adds a performance metric for this test.
   *
   * @param metric the metric description
   */
  protected void addTestMetric(final String metric) {
    testMetrics.add(metric);
  }

  /**
   * Gets the test execution duration.
   *
   * @return the duration since test start
   */
  protected Duration getTestDuration() {
    if (testStartTime == null) {
      return Duration.ZERO;
    }
    return Duration.between(testStartTime, Instant.now());
  }

  /**
   * Registers a resource for automatic cleanup.
   *
   * @param resource the resource to clean up
   */
  protected void registerForCleanup(final AutoCloseable resource) {
    testResources.add(resource);
  }

  /** Cleans up all registered test resources. */
  private void cleanupTestResources() {
    for (final AutoCloseable resource : testResources) {
      try {
        if (resource != null) {
          resource.close();
        }
      } catch (final Exception e) {
        LOGGER.warning("Failed to clean up resource: " + e.getMessage());
      }
    }
    testResources.clear();
  }

  /**
   * Logs performance metrics for the test.
   *
   * @param testInfo the test information
   */
  private void logTestMetrics(final TestInfo testInfo) {
    if (!testMetrics.isEmpty() || testStartTime != null) {
      LOGGER.info("Performance metrics for " + testInfo.getDisplayName() + ":");
      if (testStartTime != null) {
        LOGGER.info("  Execution time: " + getTestDuration().toMillis() + "ms");
      }
      testMetrics.forEach(metric -> LOGGER.info("  " + metric));
      testMetrics.clear();
    }
  }

  /**
   * Creates a test runtime with automatic resource management.
   *
   * @param runtimeType the runtime type to create
   * @return the created runtime (automatically registered for cleanup)
   */
  protected WasmRuntime createTestRuntime(final RuntimeType runtimeType) throws WasmException {
    final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType);
    registerForCleanup(runtime);
    return runtime;
  }

  /**
   * Executes a test with both JNI and Panama runtimes for cross-runtime validation.
   *
   * @param testFunction the test function to execute
   */
  protected void runWithBothRuntimes(final RuntimeTestFunction testFunction) {
    // Test with JNI runtime
    LOGGER.info("Testing with JNI runtime");
    try (final WasmRuntime jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
      testFunction.execute(jniRuntime, RuntimeType.JNI);
    } catch (final Exception e) {
      throw new RuntimeException("JNI runtime test failed", e);
    }

    // Test with Panama runtime if available
    if (TestUtils.isPanamaAvailable()) {
      LOGGER.info("Testing with Panama runtime");
      try (final WasmRuntime panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA)) {
        testFunction.execute(panamaRuntime, RuntimeType.PANAMA);
      } catch (final Exception e) {
        throw new RuntimeException("Panama runtime test failed", e);
      }
    } else {
      LOGGER.info("Panama runtime not available, skipping cross-runtime test");
    }
  }

  /** Functional interface for runtime-specific test execution. */
  @FunctionalInterface
  protected interface RuntimeTestFunction {
    void execute(WasmRuntime runtime, RuntimeType runtimeType) throws Exception;
  }

  /**
   * Skips the current test if the specified condition is not met.
   *
   * @param condition the condition that must be true for the test to run
   * @param message the message to display when skipping the test
   */
  protected void skipIfNot(final boolean condition, final String message) {
    org.junit.jupiter.api.Assumptions.assumeTrue(condition, message);
  }

  /** Skips the current test if running on Windows. */
  protected void skipOnWindows() {
    skipIfNot(!TestUtils.isWindows(), "Test skipped on Windows platform");
  }

  /** Skips the current test if running on Linux. */
  protected void skipOnLinux() {
    skipIfNot(!TestUtils.isLinux(), "Test skipped on Linux platform");
  }

  /** Skips the current test if running on macOS. */
  protected void skipOnMacOs() {
    skipIfNot(!TestUtils.isMacOs(), "Test skipped on macOS platform");
  }

  /** Skips the current test if Panama is not available. */
  protected void skipIfPanamaNotAvailable() {
    skipIfNot(TestUtils.isPanamaAvailable(), "Test requires Panama FFI (Java 23+)");
  }

  /** Skips the current test if running on ARM64 architecture. */
  protected void skipOnArm64() {
    skipIfNot(!TestUtils.isArm64(), "Test skipped on ARM64 architecture");
  }

  /** Skips the current test if running on x86_64 architecture. */
  protected void skipOnX86_64() {
    skipIfNot(!TestUtils.isX86_64(), "Test skipped on x86_64 architecture");
  }

  /**
   * Skips the current test if the specified test category is not enabled.
   *
   * @param category the test category to check
   */
  protected void skipIfCategoryNotEnabled(final String category) {
    skipIfNot(
        TestUtils.isTestCategoryEnabled(category),
        "Test category '" + category + "' is not enabled");
  }

  /**
   * Measures execution time of a code block.
   *
   * @param description description of what is being measured
   * @param operation the operation to measure
   * @return the execution duration
   */
  protected Duration measureExecutionTime(final String description, final Runnable operation) {
    final Instant start = Instant.now();
    operation.run();
    final Duration duration = Duration.between(start, Instant.now());
    addTestMetric(description + ": " + duration.toMillis() + "ms");
    return duration;
  }

  /**
   * Verifies that a runtime operation completes within expected time bounds.
   *
   * @param maxDuration maximum allowed duration
   * @param operation the operation to execute
   * @param description description for logging
   */
  protected void assertExecutionTime(
      final Duration maxDuration, final Runnable operation, final String description) {
    final Duration actualDuration = measureExecutionTime(description, operation);
    if (actualDuration.compareTo(maxDuration) > 0) {
      throw new AssertionError(
          String.format(
              "Operation '%s' took %dms but expected maximum %dms",
              description, actualDuration.toMillis(), maxDuration.toMillis()));
    }
  }
}

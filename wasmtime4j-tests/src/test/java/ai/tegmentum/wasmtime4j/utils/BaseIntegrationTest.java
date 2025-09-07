package ai.tegmentum.wasmtime4j.utils;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * Base class for Wasmtime4j integration tests. Provides common functionality for test setup,
 * category-based test skipping, and resource management.
 */
public abstract class BaseIntegrationTest {

  protected static final Logger LOGGER = Logger.getLogger(BaseIntegrationTest.class.getName());

  private final List<AutoCloseable> cleanupResources = new ArrayList<>();

  /**
   * Sets up the test before each test method execution. Calls the concrete implementation's doSetUp
   * method.
   *
   * @param testInfo the test information
   */
  @BeforeEach
  public final void setUp(final TestInfo testInfo) {
    LOGGER.info("Setting up test: " + testInfo.getDisplayName());
    cleanupResources(); // Clean up any leftover resources
    doSetUp(testInfo);
  }

  /**
   * Template method for test-specific setup. Override this method in concrete test classes to
   * perform custom setup.
   *
   * @param testInfo the test information
   */
  protected void doSetUp(final TestInfo testInfo) {
    // Default implementation - no setup required
  }

  /**
   * Tears down the test after each test method execution. Cleans up registered resources.
   *
   * @param testInfo the test information
   */
  @AfterEach
  public final void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down test: " + testInfo.getDisplayName());
    cleanupResources();
  }

  /**
   * Skips the test if the specified category is not enabled. Categories can be enabled by setting
   * system properties like: wasmtime4j.test.{category}.enabled=true
   *
   * @param category the test category to check
   */
  protected final void skipIfCategoryNotEnabled(final String category) {
    final String propertyName = "wasmtime4j.test." + category + ".enabled";
    final String enabled = System.getProperty(propertyName);

    if (!"true".equalsIgnoreCase(enabled)) {
      LOGGER.info("Skipping test - category not enabled: " + category);
      Assumptions.assumeTrue(
          false,
          "Test category not enabled: "
              + category
              + ". Set system property "
              + propertyName
              + "=true to enable.");
    }
  }

  /**
   * Skips the test if the given condition is not met.
   *
   * @param condition the condition to check
   * @param message the message to display if the test is skipped
   */
  protected final void skipIfNot(final boolean condition, final String message) {
    if (!condition) {
      LOGGER.info("Skipping test - condition not met: " + message);
      Assumptions.assumeTrue(false, message);
    }
  }

  /** Skips the test if Panama runtime is not available. */
  protected final void skipIfPanamaNotAvailable() {
    skipIfNot(TestUtils.isPanamaAvailable(), "Panama runtime not available");
  }

  /**
   * Skips the test if running on an unsupported platform.
   *
   * @param supportedPlatforms the platforms that are supported
   */
  protected final void skipIfPlatformNot(final String... supportedPlatforms) {
    final String currentPlatform = TestUtils.getOperatingSystem();
    boolean supported = false;

    for (final String platform : supportedPlatforms) {
      if (currentPlatform.toLowerCase().contains(platform.toLowerCase())) {
        supported = true;
        break;
      }
    }

    skipIfNot(supported, "Test not supported on platform: " + currentPlatform);
  }

  /**
   * Gets the logger for this test class.
   *
   * @return the logger
   */
  protected final Logger getLogger() {
    return Logger.getLogger(this.getClass().getName());
  }

  /** Functional interface for operations that need to be tested with both runtimes. */
  @FunctionalInterface
  protected interface BiRuntimeOperation {
    void execute(WasmRuntime runtime, String runtimeType) throws Exception;
  }

  /**
   * Runs an operation with both JNI and Panama runtimes for comparison.
   *
   * @param operation the operation to run with both runtimes
   */
  protected final void runWithBothRuntimes(final BiRuntimeOperation operation) {
    try {
      // Test with JNI runtime
      try (final WasmRuntime jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
        operation.execute(jniRuntime, "JNI");
      }

      // Test with Panama runtime if available
      if (TestUtils.isPanamaAvailable()) {
        try (final WasmRuntime panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA)) {
          operation.execute(panamaRuntime, "PANAMA");
        }
      } else {
        LOGGER.warning("Panama runtime not available - skipping Panama tests");
      }
    } catch (final Exception e) {
      throw new RuntimeException("Runtime operation failed", e);
    }
  }

  /**
   * Measures the execution time of an operation and logs the result.
   *
   * @param description description of the operation being measured
   * @param operation the operation to measure
   * @param <T> the return type of the operation
   * @return the result of the operation
   */
  protected final <T> T measureExecutionTime(
      final String description, final Callable<T> operation) {
    final Instant start = Instant.now();
    try {
      final T result = operation.call();
      final Duration duration = Duration.between(start, Instant.now());
      LOGGER.info(description + " completed in " + duration.toMillis() + "ms");
      return result;
    } catch (final Exception e) {
      final Duration duration = Duration.between(start, Instant.now());
      LOGGER.warning(
          description + " failed after " + duration.toMillis() + "ms: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Measures the execution time of a runnable operation and logs the duration.
   *
   * @param description a description of the operation being timed
   * @param operation the operation to measure
   */
  protected final void measureExecutionTime(final String description, final Runnable operation) {
    final Instant start = Instant.now();
    try {
      operation.run();
      final Duration duration = Duration.between(start, Instant.now());
      LOGGER.info(description + " completed in " + duration.toMillis() + "ms");
    } catch (final Exception e) {
      final Duration duration = Duration.between(start, Instant.now());
      LOGGER.warning(
          description + " failed after " + duration.toMillis() + "ms: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Times the execution of a runnable operation and returns the duration.
   *
   * @param description a description of the operation being timed
   * @param operation the operation to measure
   * @return the duration of the operation
   */
  protected final Duration timeOperation(final String description, final Runnable operation) {
    final Instant start = Instant.now();
    try {
      operation.run();
      final Duration duration = Duration.between(start, Instant.now());
      LOGGER.info(description + " completed in " + duration.toMillis() + "ms");
      return duration;
    } catch (final Exception e) {
      final Duration duration = Duration.between(start, Instant.now());
      LOGGER.warning(
          description + " failed after " + duration.toMillis() + "ms: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Asserts that an operation completes within the specified time limit.
   *
   * @param maxDuration the maximum allowed duration for the operation
   * @param operation the operation to time
   * @param description a description of the operation
   */
  protected final void assertExecutionTime(
      final Duration maxDuration, final Runnable operation, final String description) {
    final Instant start = Instant.now();
    try {
      operation.run();
      final Duration elapsed = Duration.between(start, Instant.now());
      if (elapsed.compareTo(maxDuration) > 0) {
        throw new AssertionError(
            String.format(
                "%s took %dms, which exceeds the maximum allowed %dms",
                description, elapsed.toMillis(), maxDuration.toMillis()));
      }
      LOGGER.info(
          String.format(
              "%s completed in %dms (limit: %dms)",
              description, elapsed.toMillis(), maxDuration.toMillis()));
    } catch (final Exception e) {
      final Duration elapsed = Duration.between(start, Instant.now());
      LOGGER.warning(
          String.format(
              "%s failed after %dms: %s", description, elapsed.toMillis(), e.getMessage()));
      throw new RuntimeException(e);
    }
  }

  /**
   * Registers a resource for cleanup after the test completes.
   *
   * @param resource the resource to register for cleanup
   */
  protected final void registerForCleanup(final AutoCloseable resource) {
    if (resource != null) {
      cleanupResources.add(resource);
    }
  }

  /** Cleans up all registered resources. Should be called after each test. */
  protected final void cleanupResources() {
    for (final AutoCloseable resource : cleanupResources) {
      try {
        resource.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    cleanupResources.clear();
  }

  /**
   * Creates a test runtime for the specified runtime type.
   *
   * @param runtimeType the type of runtime to create
   * @return a WasmRuntime instance for testing
   */
  protected final WasmRuntime createTestRuntime(final RuntimeType runtimeType) {
    try {
      final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType);
      registerForCleanup(runtime);
      return runtime;
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create test runtime for type: " + runtimeType, e);
    }
  }
}

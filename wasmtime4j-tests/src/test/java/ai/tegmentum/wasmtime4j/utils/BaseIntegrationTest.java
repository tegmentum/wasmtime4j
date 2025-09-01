package ai.tegmentum.wasmtime4j.utils;

import java.util.logging.Logger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * Base class for Wasmtime4j integration tests. Provides common functionality for test setup,
 * category-based test skipping, and resource management.
 */
public abstract class BaseIntegrationTest {

  protected static final Logger LOGGER = Logger.getLogger(BaseIntegrationTest.class.getName());

  /**
   * Sets up the test before each test method execution. Calls the concrete implementation's
   * doSetUp method.
   *
   * @param testInfo the test information
   */
  @BeforeEach
  public final void setUp(final TestInfo testInfo) {
    LOGGER.info("Setting up test: " + testInfo.getDisplayName());
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
      Assumptions.assumeTrue(false, "Test category not enabled: " + category
          + ". Set system property " + propertyName + "=true to enable.");
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

  /**
   * Skips the test if Panama runtime is not available.
   */
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

  /**
   * Functional interface for operations that need to be tested with both runtimes.
   */
  @FunctionalInterface
  protected interface BiRuntimeOperation {
    void execute(Object runtime, String runtimeType) throws Exception;
  }

  /**
   * Runs an operation with both JNI and Panama runtimes for comparison.
   *
   * @param operation the operation to run with both runtimes
   */
  protected final void runWithBothRuntimes(final BiRuntimeOperation operation) {
    try {
      // Test with JNI runtime
      operation.execute(null, "JNI");
      
      // Test with Panama runtime if available
      if (TestUtils.isPanamaAvailable()) {
        operation.execute(null, "PANAMA");
      } else {
        LOGGER.warning("Panama runtime not available - skipping Panama tests");
      }
    } catch (final Exception e) {
      throw new RuntimeException("Runtime operation failed", e);
    }
  }
}
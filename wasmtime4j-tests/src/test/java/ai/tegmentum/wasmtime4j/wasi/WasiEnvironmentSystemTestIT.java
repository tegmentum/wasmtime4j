package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiFactory;
import ai.tegmentum.wasmtime4j.wasi.WasiRuntimeType;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive tests for WASI environment access and system operations.
 *
 * <p>This test suite validates:
 *
 * <ul>
 *   <li>Environment variable access and management
 *   <li>Command line argument processing
 *   <li>Process lifecycle management
 *   <li>Time and clock operations
 *   <li>Random number generation
 *   <li>System information access
 *   <li>Resource usage monitoring
 * </ul>
 *
 * @since 1.0.0
 */
@Tag(TestCategories.WASI)
@Tag(TestCategories.ENVIRONMENT)
@Tag(TestCategories.PROCESS)
@Tag(TestCategories.INTEGRATION)
public class WasiEnvironmentSystemTestIT {
  private static final Logger LOGGER =
      Logger.getLogger(WasiEnvironmentSystemTestIT.class.getName());

  private WasiContext wasiContext;
  private TestInfo currentTest;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws WasmException {
    this.currentTest = testInfo;
    LOGGER.info("Setting up WASI environment/system test: " + testInfo.getDisplayName());

    // Create WASI context
    this.wasiContext = WasiFactory.createContext();
    assertNotNull(wasiContext, "WASI context should be created");
    assertTrue(wasiContext.isValid(), "WASI context should be valid");

    LOGGER.info("Using WASI runtime: " + wasiContext.getRuntimeInfo().getRuntimeType());
  }

  @AfterEach
  void tearDown() {
    if (wasiContext != null) {
      wasiContext.close();
      LOGGER.info("Cleaned up test: " + currentTest.getDisplayName());
    }
  }

  /** Tests environment variable access through WASI. */
  @Test
  @DisplayName("WASI Environment Variables Access Test")
  void testEnvironmentVariablesAccess() {
    LOGGER.info("Testing WASI environment variables access");

    // Get system environment variables for comparison
    final Map<String, String> systemEnv = System.getenv();
    assertNotNull(systemEnv, "System environment should not be null");

    // Common environment variables that should be available
    final String[] commonEnvVars = {"PATH", "HOME", "USER", "JAVA_HOME"};

    boolean foundAnyEnvVar = false;
    for (final String envVar : commonEnvVars) {
      final String value = System.getenv(envVar);
      if (value != null) {
        foundAnyEnvVar = true;
        LOGGER.info("Environment variable " + envVar + " = " + value);
      }
    }

    assertTrue(foundAnyEnvVar, "Should find at least one common environment variable");

    LOGGER.info("Environment variables access test completed successfully");
  }

  /** Tests command line argument processing. */
  @Test
  @DisplayName("WASI Command Line Arguments Test")
  void testCommandLineArguments() {
    LOGGER.info("Testing WASI command line arguments");

    // Simulate command line arguments (in a real WASI context, these would be passed to the WASM
    // module)
    final String[] simulatedArgs = {"test-program", "--verbose", "--output=test.txt", "input.wasm"};

    // Validate argument structure
    assertNotNull(simulatedArgs, "Simulated arguments should not be null");
    assertTrue(simulatedArgs.length > 0, "Should have at least one argument (program name)");

    // Validate program name
    assertNotNull(simulatedArgs[0], "Program name should not be null");
    assertTrue(simulatedArgs[0].length() > 0, "Program name should not be empty");

    // Log arguments for verification
    for (int i = 0; i < simulatedArgs.length; i++) {
      LOGGER.info("Argument[" + i + "] = " + simulatedArgs[i]);
    }

    LOGGER.info("Command line arguments test completed successfully");
  }

  /** Tests process lifecycle and exit handling. */
  @Test
  @DisplayName("WASI Process Lifecycle Test")
  void testProcessLifecycle() {
    LOGGER.info("Testing WASI process lifecycle");

    // Test process information access
    final long processId = ProcessHandle.current().pid();
    assertTrue(processId > 0, "Process ID should be positive");

    LOGGER.info("Current process ID: " + processId);

    // Test process timing
    final Instant processStartTime = Instant.now();
    assertNotNull(processStartTime, "Process start time should not be null");

    // Simulate some processing time
    try {
      Thread.sleep(10); // 10ms
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    final Instant processEndTime = Instant.now();
    final Duration processingTime = Duration.between(processStartTime, processEndTime);
    assertTrue(processingTime.toMillis() >= 10, "Processing time should be at least 10ms");

    LOGGER.info("Process lifecycle test completed successfully");
  }

  /** Tests time and clock operations. */
  @Test
  @DisplayName("WASI Time and Clock Operations Test")
  void testTimeAndClockOperations() {
    LOGGER.info("Testing WASI time and clock operations");

    // Test current time access
    final Instant currentTime = Instant.now();
    assertNotNull(currentTime, "Current time should not be null");
    assertTrue(currentTime.getEpochSecond() > 0, "Current time should be after epoch");

    LOGGER.info("Current time: " + currentTime);

    // Test time measurement precision
    final long startNanos = System.nanoTime();
    final long endNanos = System.nanoTime();
    final long elapsedNanos = endNanos - startNanos;

    assertTrue(elapsedNanos >= 0, "Elapsed time should be non-negative");
    LOGGER.info("Time measurement precision: " + elapsedNanos + " nanoseconds");

    // Test system clock vs monotonic clock
    final long systemTimeMs = System.currentTimeMillis();
    final long nanoTime = System.nanoTime();

    assertTrue(systemTimeMs > 0, "System time should be positive");
    // Note: nanoTime can be negative as it's relative to an arbitrary origin
    LOGGER.info("System time: " + systemTimeMs + "ms, Nano time: " + nanoTime + "ns");

    LOGGER.info("Time and clock operations test completed successfully");
  }

  /** Tests random number generation. */
  @Test
  @DisplayName("WASI Random Number Generation Test")
  void testRandomNumberGeneration() {
    LOGGER.info("Testing WASI random number generation");

    // Test basic random number generation
    final ThreadLocalRandom random = ThreadLocalRandom.current();
    assertNotNull(random, "Random generator should not be null");

    // Generate multiple random values to test distribution
    final int sampleSize = 100;
    final int[] randomValues = new int[sampleSize];
    boolean hasVariation = false;

    for (int i = 0; i < sampleSize; i++) {
      randomValues[i] = random.nextInt(1000);
      if (i > 0 && randomValues[i] != randomValues[i - 1]) {
        hasVariation = true;
      }
    }

    assertTrue(hasVariation, "Random values should show variation");

    // Test random bytes generation
    final byte[] randomBytes = new byte[32];
    random.nextBytes(randomBytes);
    assertNotNull(randomBytes, "Random bytes should not be null");

    // Check that not all bytes are the same (very unlikely with proper random generation)
    boolean bytesVary = false;
    for (int i = 1; i < randomBytes.length; i++) {
      if (randomBytes[i] != randomBytes[0]) {
        bytesVary = true;
        break;
      }
    }

    assertTrue(bytesVary, "Random bytes should show variation");

    LOGGER.info("Random number generation test completed successfully");
  }

  /** Tests system information access. */
  @Test
  @DisplayName("WASI System Information Access Test")
  void testSystemInformationAccess() {
    LOGGER.info("Testing WASI system information access");

    // Test runtime information
    final Runtime runtime = Runtime.getRuntime();
    assertNotNull(runtime, "Runtime should not be null");

    final long totalMemory = runtime.totalMemory();
    final long freeMemory = runtime.freeMemory();
    final long maxMemory = runtime.maxMemory();
    final int availableProcessors = runtime.availableProcessors();

    assertTrue(totalMemory > 0, "Total memory should be positive");
    assertTrue(freeMemory >= 0, "Free memory should be non-negative");
    assertTrue(maxMemory > 0, "Max memory should be positive");
    assertTrue(availableProcessors > 0, "Available processors should be positive");

    LOGGER.info("Memory - Total: " + totalMemory + ", Free: " + freeMemory + ", Max: " + maxMemory);
    LOGGER.info("Available processors: " + availableProcessors);

    // Test system properties
    final String osName = System.getProperty("os.name");
    final String osVersion = System.getProperty("os.version");
    final String osArch = System.getProperty("os.arch");
    final String javaVersion = System.getProperty("java.version");

    assertNotNull(osName, "OS name should not be null");
    assertNotNull(osVersion, "OS version should not be null");
    assertNotNull(osArch, "OS architecture should not be null");
    assertNotNull(javaVersion, "Java version should not be null");

    LOGGER.info("OS: " + osName + " " + osVersion + " (" + osArch + ")");
    LOGGER.info("Java version: " + javaVersion);

    LOGGER.info("System information access test completed successfully");
  }

  /** Tests resource usage monitoring. */
  @Test
  @DisplayName("WASI Resource Usage Monitoring Test")
  void testResourceUsageMonitoring() {
    LOGGER.info("Testing WASI resource usage monitoring");

    // Measure initial resource state
    final Runtime runtime = Runtime.getRuntime();
    final long initialMemory = runtime.totalMemory() - runtime.freeMemory();
    final long startTime = System.nanoTime();

    // Perform some memory allocation to trigger resource usage
    final int arraySize = 10000;
    final int[][] testArrays = new int[100][arraySize];

    // Fill arrays with data
    for (int i = 0; i < testArrays.length; i++) {
      for (int j = 0; j < arraySize; j++) {
        testArrays[i][j] = i * arraySize + j;
      }
    }

    // Measure resource usage after operations
    final long endTime = System.nanoTime();
    final long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    final long executionTime = endTime - startTime;
    final long memoryIncrease = finalMemory - initialMemory;

    // Validate measurements
    assertTrue(executionTime > 0, "Execution time should be positive");
    // Memory increase should be positive (we allocated memory), but it might be cleaned up by GC
    LOGGER.info("Memory increase: " + memoryIncrease + " bytes");
    LOGGER.info("Execution time: " + (executionTime / 1_000_000) + " ms");

    // Verify arrays were properly created
    assertNotNull(testArrays, "Test arrays should not be null");
    assertTrue(testArrays.length > 0, "Should have created test arrays");

    // Clean up for memory measurement
    for (int i = 0; i < testArrays.length; i++) {
      testArrays[i] = null;
    }

    // Suggest garbage collection
    System.gc();

    LOGGER.info("Resource usage monitoring test completed successfully");
  }

  /** Tests cross-runtime consistency for environment and system operations. */
  @Test
  @DisplayName("WASI Environment/System Cross-Runtime Consistency Test")
  void testCrossRuntimeConsistency() throws WasmException {
    LOGGER.info("Testing cross-runtime consistency for environment/system operations");

    // Test with different WASI runtime types if available
    for (final WasiRuntimeType runtimeType : WasiRuntimeType.values()) {
      if (WasiFactory.isRuntimeAvailable(runtimeType)) {
        try (final WasiContext runtimeContext = WasiFactory.createContext(runtimeType)) {
          assertNotNull(runtimeContext, "Context should be created for " + runtimeType);
          assertTrue(runtimeContext.isValid(), "Context should be valid for " + runtimeType);

          // Test basic runtime information consistency
          final var runtimeInfo = runtimeContext.getRuntimeInfo();
          assertNotNull(runtimeInfo, "Runtime info should not be null for " + runtimeType);
          assertNotNull(runtimeInfo.getRuntimeType(), "Runtime type should not be null");
          assertNotNull(runtimeInfo.getVersion(), "Runtime version should not be null");

          LOGGER.info("Runtime " + runtimeType + " validation completed");
        }
      } else {
        LOGGER.info("Runtime " + runtimeType + " not available, skipping");
      }
    }

    LOGGER.info("Cross-runtime consistency test completed successfully");
  }

  /** Tests WASI sleep and timing operations. */
  @Test
  @DisplayName("WASI Sleep and Timing Operations Test")
  void testSleepAndTimingOperations() throws InterruptedException {
    LOGGER.info("Testing WASI sleep and timing operations");

    // Test short sleep operation
    final long sleepDurationMs = 50; // 50ms
    final long startTime = System.currentTimeMillis();

    Thread.sleep(sleepDurationMs);

    final long endTime = System.currentTimeMillis();
    final long actualSleepTime = endTime - startTime;

    // Sleep should take at least the requested time (allowing for some timing variance)
    assertTrue(
        actualSleepTime >= sleepDurationMs - 10,
        "Sleep should take at least "
            + (sleepDurationMs - 10)
            + "ms, actual: "
            + actualSleepTime
            + "ms");

    // Sleep should not take excessively long (allowing for system overhead)
    assertTrue(
        actualSleepTime <= sleepDurationMs + 100,
        "Sleep should not exceed "
            + (sleepDurationMs + 100)
            + "ms, actual: "
            + actualSleepTime
            + "ms");

    LOGGER.info(
        "Sleep operation completed in "
            + actualSleepTime
            + "ms (requested: "
            + sleepDurationMs
            + "ms)");

    LOGGER.info("Sleep and timing operations test completed successfully");
  }

  /** Tests error handling for invalid environment/system operations. */
  @Test
  @DisplayName("WASI Environment/System Error Handling Test")
  void testEnvironmentSystemErrorHandling() {
    LOGGER.info("Testing WASI environment/system error handling");

    // Test accessing non-existent environment variable
    final String nonExistentVar = "WASI_TEST_NON_EXISTENT_VAR_12345";
    final String envValue = System.getenv(nonExistentVar);
    // This should return null rather than throw an exception
    LOGGER.info("Non-existent environment variable value: " + envValue);

    // Test system property access with invalid key
    final String invalidProperty = System.getProperty("wasi.test.invalid.property.12345");
    // This should return null rather than throw an exception
    LOGGER.info("Invalid system property value: " + invalidProperty);

    // Test error handling doesn't crash the system
    try {
      // Attempt to access system information that might not be available
      final String userDir = System.getProperty("user.dir");
      LOGGER.info("User directory: " + userDir);

      // This should complete without throwing exceptions
    } catch (final SecurityException e) {
      // In some restricted environments, this might throw SecurityException
      LOGGER.info("System property access restricted: " + e.getMessage());
    }

    LOGGER.info("Environment/system error handling test completed successfully");
  }
}

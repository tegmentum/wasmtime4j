package ai.tegmentum.wasmtime4j.comparison.analyzers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestCase;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Integration tests for WASI test integration and execution framework. Validates comprehensive WASI
 * functionality including test discovery, environment setup, execution across runtimes, and
 * performance analysis.
 *
 * <p>These tests verify:
 *
 * <ul>
 *   <li>WASI test discovery from Wasmtime test suite
 *   <li>WASI environment setup and isolation
 *   <li>Cross-runtime WASI test execution
 *   <li>WASI Preview 1/2 compatibility validation
 *   <li>WASI performance benchmarking and analysis
 *   <li>I/O redirection and filesystem simulation
 * </ul>
 *
 * @since 1.0.0
 */
@Tag(TestCategories.WASI)
@Tag(TestCategories.INTEGRATION)
@Tag(TestCategories.CROSS_RUNTIME)
public class WasiIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WasiIntegrationTest.class.getName());

  private WasiTestIntegrator wasiIntegrator;
  private WasiTestDiscovery wasiDiscovery;
  private WasiTestExecutor wasiExecutor;
  private Path tempDirectory;
  private TestInfo currentTest;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws IOException {
    this.currentTest = testInfo;
    LOGGER.info("Setting up WASI integration test: " + testInfo.getDisplayName());

    this.wasiIntegrator = new WasiTestIntegrator();
    this.wasiDiscovery = new WasiTestDiscovery();
    this.wasiExecutor = new WasiTestExecutor();
    this.tempDirectory = Files.createTempDirectory("wasi-integration-test-");

    LOGGER.info("WASI integration test setup completed");
  }

  @AfterEach
  void tearDown() throws IOException {
    if (wasiExecutor != null) {
      wasiExecutor.shutdown();
    }
    if (wasiIntegrator != null) {
      wasiIntegrator.clearResults();
    }
    if (wasiDiscovery != null) {
      wasiDiscovery.clearCache();
    }
    if (tempDirectory != null && Files.exists(tempDirectory)) {
      deleteRecursively(tempDirectory);
    }
    LOGGER.info("WASI integration test cleanup completed: " + currentTest.getDisplayName());
  }

  @Test
  @DisplayName("WASI Test Integrator Creation and Basic Operations")
  @Timeout(Duration.ofSeconds(30))
  void testWasiIntegratorBasicOperations() {
    LOGGER.info("Testing WASI integrator basic operations");

    // Test integrator creation
    assertNotNull(wasiIntegrator, "WASI integrator should be created");

    // Test default environment creation
    final WasiTestIntegrator.WasiEnvironmentConfiguration defaultEnv =
        wasiIntegrator.createDefaultWasiEnvironment();
    assertNotNull(defaultEnv, "Default WASI environment should be created");
    assertNotNull(defaultEnv.getWorkingDirectory(), "Working directory should be set");
    assertEquals(1, defaultEnv.getPreviewVersion(), "Default should be Preview 1");
    assertFalse(defaultEnv.isNetworkingAllowed(), "Networking should be disabled by default");

    // Test categorized tests initialization
    final Map<WasiTestIntegrator.WasiTestCategory, ?> categorizedTests =
        wasiIntegrator.getCategorizedTests();
    assertNotNull(categorizedTests, "Categorized tests should be initialized");
    assertEquals(
        WasiTestIntegrator.WasiTestCategory.values().length,
        categorizedTests.size(),
        "All WASI categories should be initialized");

    // Test execution results initialization
    final Map<String, ?> executionResults = wasiIntegrator.getExecutionResults();
    assertNotNull(executionResults, "Execution results should be initialized");
    assertTrue(executionResults.isEmpty(), "Execution results should be initially empty");

    LOGGER.info("WASI integrator basic operations test completed successfully");
  }

  @Test
  @DisplayName("WASI Test Discovery Configuration and Execution")
  @Timeout(Duration.ofSeconds(45))
  void testWasiTestDiscovery() throws IOException {
    LOGGER.info("Testing WASI test discovery");

    // Create mock Wasmtime directory structure
    final Path mockWasmtimeDir = createMockWasmtimeDirectory();

    // Test default configuration creation
    final WasiTestDiscovery.WasiDiscoveryConfiguration defaultConfig =
        WasiTestDiscovery.createDefaultConfiguration(mockWasmtimeDir);
    assertNotNull(defaultConfig, "Default discovery configuration should be created");
    assertEquals(mockWasmtimeDir, defaultConfig.getWasmtimeRootDirectory());
    assertTrue(
        defaultConfig.isIncludePerformanceTests(),
        "Performance tests should be included by default");
    assertFalse(
        defaultConfig.isIncludeExperimentalTests(),
        "Experimental tests should be excluded by default");
    assertTrue(
        defaultConfig.isStrictValidation(), "Strict validation should be enabled by default");

    // Test custom configuration creation
    final WasiTestDiscovery.WasiDiscoveryConfiguration customConfig =
        new WasiTestDiscovery.WasiDiscoveryConfiguration.Builder()
            .wasmtimeRootDirectory(mockWasmtimeDir)
            .enableCategory(WasiTestIntegrator.WasiTestCategory.FILESYSTEM)
            .enableCategory(WasiTestIntegrator.WasiTestCategory.STDIO)
            .testNameFilter("simple")
            .maxTestsPerCategory(5)
            .includeExperimentalTests(true)
            .strictValidation(false)
            .build();

    assertNotNull(customConfig, "Custom discovery configuration should be created");
    assertEquals(2, customConfig.getEnabledCategories().size(), "Should have 2 enabled categories");
    assertTrue(
        customConfig
            .getEnabledCategories()
            .contains(WasiTestIntegrator.WasiTestCategory.FILESYSTEM));
    assertTrue(
        customConfig.getEnabledCategories().contains(WasiTestIntegrator.WasiTestCategory.STDIO));
    assertEquals(1, customConfig.getTestNameFilters().size(), "Should have 1 test name filter");
    assertTrue(customConfig.getTestNameFilters().contains("simple"));
    assertEquals(5, customConfig.getMaxTestsPerCategory(), "Max tests per category should be 5");

    // Test discovery execution (will find our mock tests)
    final WasiTestDiscovery.WasiDiscoveryResult discoveryResult =
        wasiDiscovery.discoverWasiTests(defaultConfig);
    assertNotNull(discoveryResult, "Discovery result should not be null");
    assertNotNull(discoveryResult.getCategorizedTests(), "Categorized tests should not be null");
    assertNotNull(discoveryResult.getTestMetadata(), "Test metadata should not be null");
    assertNotNull(discoveryResult.getStatistics(), "Statistics should not be null");
    assertNotNull(discoveryResult.getDiscoveryWarnings(), "Warnings should not be null");
    assertNotNull(discoveryResult.getValidationErrors(), "Validation errors should not be null");

    LOGGER.info("WASI test discovery completed successfully");
  }

  @Test
  @DisplayName("WASI Environment Configuration and Isolation")
  @Timeout(Duration.ofSeconds(30))
  void testWasiEnvironmentConfiguration() throws IOException {
    LOGGER.info("Testing WASI environment configuration");

    // Test comprehensive environment configuration
    final Path customWorkingDir = tempDirectory.resolve("custom-wd");
    Files.createDirectories(customWorkingDir);

    final WasiTestIntegrator.WasiEnvironmentConfiguration envConfig =
        new WasiTestIntegrator.WasiEnvironmentConfiguration.Builder()
            .workingDirectory(customWorkingDir)
            .preOpenedDirectory(customWorkingDir)
            .environmentVariable("TEST_VAR", "test_value")
            .environmentVariable("PATH", "/usr/bin:/bin")
            .commandLineArgument("--test-arg")
            .commandLineArgument("value")
            .previewVersion(2)
            .allowNetworking(true)
            .build();

    assertNotNull(envConfig, "Environment configuration should be created");
    assertEquals(customWorkingDir, envConfig.getWorkingDirectory());
    assertTrue(envConfig.getPreOpenedDirectories().contains(customWorkingDir));
    assertEquals("test_value", envConfig.getEnvironmentVariables().get("TEST_VAR"));
    assertEquals("/usr/bin:/bin", envConfig.getEnvironmentVariables().get("PATH"));
    assertEquals(2, envConfig.getCommandLineArguments().size());
    assertTrue(envConfig.getCommandLineArguments().contains("--test-arg"));
    assertTrue(envConfig.getCommandLineArguments().contains("value"));
    assertEquals(2, envConfig.getPreviewVersion());
    assertTrue(envConfig.isNetworkingAllowed());

    // Test environment isolation through executor
    try (final WasiTestExecutor.WasiExecutionEnvironment execEnv =
        new WasiTestExecutor.WasiExecutionEnvironment(envConfig)) {
      assertNotNull(execEnv, "Execution environment should be created");
      assertNotNull(execEnv.getWorkingDirectory(), "Working directory should be set");
      assertNotNull(execEnv.getStdoutCapture(), "Stdout capture should be set");
      assertNotNull(execEnv.getStderrCapture(), "Stderr capture should be set");
      assertNotNull(execEnv.getStdinInput(), "Stdin input should be set");

      assertTrue(Files.exists(execEnv.getWorkingDirectory()), "Working directory should exist");
      assertTrue(Files.exists(execEnv.getStdoutCapture()), "Stdout capture file should exist");
      assertTrue(Files.exists(execEnv.getStderrCapture()), "Stderr capture file should exist");
      assertTrue(Files.exists(execEnv.getStdinInput()), "Stdin input file should exist");

      assertEquals(2, execEnv.getPreviewVersion(), "Preview version should match configuration");
      assertTrue(execEnv.isNetworkingEnabled(), "Networking should be enabled");
    }

    LOGGER.info("WASI environment configuration test completed successfully");
  }

  @Test
  @DisplayName("WASI Test Execution Across Runtimes")
  @Timeout(Duration.ofSeconds(60))
  @EnabledIf("hasMinimalWasmTest")
  void testWasiExecutionAcrossRuntimes() throws IOException {
    LOGGER.info("Testing WASI execution across runtimes");

    // Create a minimal WASI test case
    final WasmTestCase testCase = createMinimalWasiTestCase();
    final WasiTestIntegrator.WasiEnvironmentConfiguration envConfig =
        wasiIntegrator.createDefaultWasiEnvironment();

    // Test execution across all runtimes
    final Map<RuntimeType, WasiTestExecutor.WasiExecutionResult> results =
        wasiExecutor.executeWasiTestAcrossRuntimes(testCase, envConfig);

    assertNotNull(results, "Execution results should not be null");
    assertFalse(results.isEmpty(), "Should have execution results for at least one runtime");

    // Verify results for each runtime
    for (final Map.Entry<RuntimeType, WasiTestExecutor.WasiExecutionResult> entry :
        results.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final WasiTestExecutor.WasiExecutionResult result = entry.getValue();

      assertNotNull(result, "Execution result should not be null for runtime: " + runtime);
      assertEquals(testCase.getTestName(), result.getTestName(), "Test name should match");
      assertEquals(runtime, result.getRuntime(), "Runtime should match");
      assertNotNull(result.getStdoutOutput(), "Stdout output should not be null");
      assertNotNull(result.getStderrOutput(), "Stderr output should not be null");
      assertNotNull(result.getPerformanceMetrics(), "Performance metrics should not be null");
      assertNotNull(result.getWasiFeaturesCalled(), "WASI features called should not be null");
      assertNotNull(result.getExecutionTime(), "Execution time should not be null");

      LOGGER.fine(
          "WASI test execution completed on " + runtime + ": success=" + result.isSuccessful());
    }

    // Test individual runtime execution
    final WasiTestExecutor.WasiExecutionResult individualResult =
        wasiExecutor.executeWasiTestOnRuntime(testCase, envConfig, RuntimeType.JNI);
    assertNotNull(individualResult, "Individual execution result should not be null");
    assertEquals(RuntimeType.JNI, individualResult.getRuntime(), "Runtime should be JNI");

    LOGGER.info("WASI execution across runtimes test completed successfully");
  }

  @Test
  @DisplayName("WASI Test Integration End-to-End")
  @Timeout(Duration.ofSeconds(90))
  @EnabledIf("hasMinimalWasmTest")
  void testWasiIntegrationEndToEnd() throws IOException {
    LOGGER.info("Testing WASI integration end-to-end");

    // Create minimal test case
    final WasmTestCase testCase = createMinimalWasiTestCase();
    final WasiTestIntegrator.WasiEnvironmentConfiguration envConfig =
        wasiIntegrator.createDefaultWasiEnvironment();

    // Execute test through integrator
    final WasiTestIntegrator.WasiTestExecutionResult integrationResult =
        wasiIntegrator.executeWasiTest(testCase, envConfig);

    assertNotNull(integrationResult, "Integration result should not be null");
    assertEquals(testCase.getTestName(), integrationResult.getTestName());
    assertNotNull(integrationResult.getCategory(), "Category should be determined");
    assertEquals(envConfig, integrationResult.getEnvironment());
    assertNotNull(integrationResult.getRuntimeResults(), "Runtime results should not be null");
    assertNotNull(
        integrationResult.getCompatibilityAnalysis(), "Compatibility analysis should not be null");
    assertNotNull(
        integrationResult.getPerformanceMetrics(), "Performance metrics should not be null");
    assertNotNull(integrationResult.getExecutionTime(), "Execution time should not be null");

    // Verify compatibility analysis
    final WasiTestIntegrator.WasiCompatibilityAnalysis compatAnalysis =
        integrationResult.getCompatibilityAnalysis();
    assertNotNull(
        compatAnalysis.getPreviewCompatibilityScores(),
        "Preview compatibility scores should not be null");
    assertNotNull(
        compatAnalysis.getSupportedWasiFeatures(), "Supported WASI features should not be null");
    assertNotNull(
        compatAnalysis.getUnsupportedWasiFeatures(),
        "Unsupported WASI features should not be null");
    assertNotNull(
        compatAnalysis.getCompatibilityIssues(), "Compatibility issues should not be null");
    assertTrue(
        compatAnalysis.getOverallCompatibilityScore() >= 0.0
            && compatAnalysis.getOverallCompatibilityScore() <= 100.0,
        "Overall compatibility score should be between 0 and 100");

    // Verify performance metrics
    final WasiTestIntegrator.WasiPerformanceMetrics perfMetrics =
        integrationResult.getPerformanceMetrics();
    assertNotNull(perfMetrics.getIoOperationTimes(), "I/O operation times should not be null");
    assertNotNull(
        perfMetrics.getFilesystemOperationTimes(), "Filesystem operation times should not be null");
    assertNotNull(
        perfMetrics.getNetworkOperationTimes(), "Network operation times should not be null");
    assertNotNull(perfMetrics.getSystemCallCounts(), "System call counts should not be null");
    assertNotNull(perfMetrics.getMemoryUsage(), "Memory usage should not be null");

    // Verify execution results are stored in integrator
    final Map<String, WasiTestIntegrator.WasiTestExecutionResult> storedResults =
        wasiIntegrator.getExecutionResults();
    assertTrue(
        storedResults.containsKey(testCase.getTestName()),
        "Test result should be stored in integrator");

    LOGGER.info("WASI integration end-to-end test completed successfully");
  }

  @Test
  @DisplayName("WASI Performance and Resource Management")
  @Timeout(Duration.ofSeconds(45))
  void testWasiPerformanceAndResourceManagement() throws IOException {
    LOGGER.info("Testing WASI performance and resource management");

    // Test multiple environment creation and cleanup
    final int environmentCount = 5;
    final WasiTestIntegrator.WasiEnvironmentConfiguration baseConfig =
        wasiIntegrator.createDefaultWasiEnvironment();

    for (int i = 0; i < environmentCount; i++) {
      try (final WasiTestExecutor.WasiExecutionEnvironment env =
          new WasiTestExecutor.WasiExecutionEnvironment(baseConfig)) {
        assertNotNull(env, "Environment " + i + " should be created");
        assertTrue(Files.exists(env.getWorkingDirectory()), "Working directory should exist");

        // Verify environment isolation
        assertNotNull(env.getCreationTime(), "Creation time should be set");
        final Path uniqueFile = env.getWorkingDirectory().resolve("test-" + i + ".txt");
        Files.writeString(uniqueFile, "test content " + i);
        assertTrue(Files.exists(uniqueFile), "Unique file should be created");
      }
      // Environment should be cleaned up automatically
    }

    // Test executor resource management
    final long startMemory = getUsedMemory();

    // Create multiple executors to test resource handling
    for (int i = 0; i < 3; i++) {
      final WasiTestExecutor executor = new WasiTestExecutor();
      executor.shutdown();
    }

    // Force garbage collection and check memory usage
    System.gc();
    final long endMemory = getUsedMemory();

    LOGGER.info(
        "Memory usage: start="
            + startMemory
            + ", end="
            + endMemory
            + ", difference="
            + (endMemory - startMemory));

    // Test integrator cleanup
    wasiIntegrator.clearResults();
    assertTrue(wasiIntegrator.getExecutionResults().isEmpty(), "Results should be cleared");

    // Test discovery cache management
    wasiDiscovery.clearCache();

    LOGGER.info("WASI performance and resource management test completed successfully");
  }

  // Helper methods

  private Path createMockWasmtimeDirectory() throws IOException {
    final Path mockDir = tempDirectory.resolve("mock-wasmtime");
    Files.createDirectories(mockDir);

    // Create mock WASI test directories
    final Path wasiTestsDir = mockDir.resolve("crates/wasi-tests");
    Files.createDirectories(wasiTestsDir);

    // Create a simple mock WASM file (minimal valid WebAssembly module)
    final byte[] minimalWasm = {
      0x00, 0x61, 0x73, 0x6d, // WASM magic
      0x01, 0x00, 0x00, 0x00 // Version
    };

    final Path mockWasmFile = wasiTestsDir.resolve("simple_filesystem_test.wasm");
    Files.write(mockWasmFile, minimalWasm);

    return mockDir;
  }

  private WasmTestCase createMinimalWasiTestCase() throws IOException {
    // Create a minimal valid WebAssembly module for testing
    final byte[] minimalWasm = {
      0x00,
      0x61,
      0x73,
      0x6d, // WASM magic
      0x01,
      0x00,
      0x00,
      0x00, // Version
      0x01,
      0x07, // Type section
      0x01, // 1 type
      0x60,
      0x00,
      0x01,
      0x7f, // func type (no params, returns i32)
      0x03,
      0x02, // Function section
      0x01,
      0x00, // 1 function of type 0
      0x07,
      0x08, // Export section
      0x01, // 1 export
      0x04,
      0x6d,
      0x61,
      0x69,
      0x6e, // "main"
      0x00,
      0x00, // function 0
      0x0a,
      0x06, // Code section
      0x01, // 1 function body
      0x04, // body size
      0x00, // 0 locals
      0x41,
      0x2a, // i32.const 42
      0x0b // end
    };

    final Path testFile = tempDirectory.resolve("minimal_wasi_test.wasm");
    Files.write(testFile, minimalWasm);

    return WasmTestCase.fromFile(testFile, WasmTestSuiteLoader.TestSuiteType.WASI_TESTS);
  }

  private long getUsedMemory() {
    final Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  private void deleteRecursively(final Path path) throws IOException {
    if (Files.exists(path)) {
      if (Files.isDirectory(path)) {
        try (var stream = Files.list(path)) {
          for (final Path child : stream.toList()) {
            deleteRecursively(child);
          }
        }
      }
      Files.delete(path);
    }
  }

  // Condition methods for @EnabledIf

  static boolean hasMinimalWasmTest() {
    // Always return true for our mock tests
    // In a real implementation, this would check for actual test availability
    return true;
  }
}

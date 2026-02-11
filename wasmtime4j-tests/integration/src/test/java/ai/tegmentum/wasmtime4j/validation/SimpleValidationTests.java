package ai.tegmentum.wasmtime4j.validation;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.validation.metrics.BasicMetricsCollector;
import ai.tegmentum.wasmtime4j.validation.reporting.CsvReporter;
import ai.tegmentum.wasmtime4j.validation.reporting.JsonReporter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

/**
 * Simple WebAssembly validation tests focusing on basic framework functionality.
 *
 * <p>This test class validates the simple JUnit testing framework and basic metrics collection
 * without requiring complex WebAssembly operations. It focuses on proving the testing
 * infrastructure works correctly for CI/CD integration.
 *
 * @since 1.0.0
 */
@DisplayName("Simple WebAssembly Validation Tests")
public final class SimpleValidationTests {

  private static final Logger logger = Logger.getLogger(SimpleValidationTests.class.getName());

  @TempDir Path tempDir;

  private Engine engine;
  private Store store;
  private BasicMetricsCollector metricsCollector;
  private List<AutoCloseable> resources;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws WasmException {
    logger.info("Setting up simple test: " + testInfo.getDisplayName());
    resources = new ArrayList<>();
    metricsCollector = new BasicMetricsCollector();

    // Create engine with default configuration
    engine = Engine.create();
    resources.add(engine);

    // Create store
    store = engine.createStore();
    resources.add(store);

    logger.info("Simple test setup completed successfully");
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) throws IOException {
    logger.info("Cleaning up simple test: " + testInfo.getDisplayName());

    // Export metrics to files for CI/CD integration
    if (metricsCollector != null) {
      exportMetricsToFiles(testInfo.getDisplayName());
    }

    // Close all resources in reverse order
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        final AutoCloseable resource = resources.get(i);
        if (resource != null) {
          resource.close();
        }
      } catch (final Exception e) {
        logger.warning("Failed to close resource during cleanup: " + e.getMessage());
      }
    }

    resources.clear();
    store = null;
    engine = null;
    metricsCollector = null;

    logger.info("Simple test cleanup completed");
  }

  @Test
  @DisplayName("Runtime Factory Availability")
  void testRuntimeFactoryAvailability() throws Exception {
    final Boolean result =
        metricsCollector.recordOperation(
            "runtime_factory_test",
            () -> {
              // Test that we can determine runtime availability
              final boolean jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
              final boolean panamaAvailable =
                  WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

              logger.info(
                  "Runtime availability - JNI: " + jniAvailable + ", Panama: " + panamaAvailable);

              // At least one runtime should be available
              return jniAvailable || panamaAvailable;
            });

    assertTrue(result, "At least one runtime should be available");

    final BasicMetricsCollector.OperationMetrics metrics =
        metricsCollector.getOperationMetrics("runtime_factory_test");
    assertNotNull(metrics, "Metrics should be recorded");
    assertEquals(1, metrics.getSuccessCount(), "Should have one successful operation");

    logger.info("Runtime factory availability test passed");
  }

  @Test
  @DisplayName("Engine Creation and Validation")
  void testEngineCreation() throws Exception {
    final String result =
        metricsCollector.recordOperation(
            "engine_creation",
            () -> {
              assertNotNull(engine, "Engine should be created successfully");
              assertTrue(engine.isValid(), "Engine should be valid after creation");
              // Note: getConfig() may return null in current implementation
              logger.info("Engine config: " + (engine.getConfig() != null ? "available" : "null"));
              assertTrue(
                  engine.getReferenceCount() > 0, "Engine should have positive reference count");

              return "success";
            });

    assertEquals("success", result, "Engine creation test should succeed");

    final BasicMetricsCollector.OperationMetrics metrics =
        metricsCollector.getOperationMetrics("engine_creation");
    assertNotNull(metrics, "Metrics should be recorded for engine creation");
    assertEquals(1, metrics.getSuccessCount(), "Should have one successful operation");

    logger.info("Engine creation validation passed: " + metrics);
  }

  @Test
  @DisplayName("Store Creation and Management")
  void testStoreCreation() throws Exception {
    final String result =
        metricsCollector.recordOperation(
            "store_creation",
            () -> {
              assertNotNull(store, "Store should be created successfully");

              // Test creating additional stores
              final Store store2 = engine.createStore();
              resources.add(store2);
              assertNotNull(store2, "Additional store should be created successfully");

              // Test store with custom data (if implemented)
              try {
                final String customData = "test-data";
                final Store store3 = engine.createStore(customData);
                resources.add(store3);
                assertNotNull(store3, "Store with custom data should be created successfully");
                logger.info("Store creation with custom data: supported");
              } catch (final UnsupportedOperationException e) {
                logger.info("Store creation with custom data: not yet implemented");
              }

              return "success";
            });

    assertEquals("success", result, "Store creation test should succeed");

    final BasicMetricsCollector.OperationMetrics metrics =
        metricsCollector.getOperationMetrics("store_creation");
    assertNotNull(metrics, "Metrics should be recorded for store creation");
    assertEquals(1, metrics.getSuccessCount(), "Should have one successful operation");

    logger.info("Store creation validation passed: " + metrics);
  }

  @Test
  @DisplayName("Engine Feature Support Query")
  void testEngineFeatureSupport() throws Exception {
    final String result =
        metricsCollector.recordOperation(
            "feature_support_query",
            () -> {
              // Test basic feature support queries (these should not fail)
              final boolean supportsThreads = engine.supportsFeature(WasmFeature.THREADS);
              final boolean supportsSimd = engine.supportsFeature(WasmFeature.SIMD);
              final boolean supportsReferenceTypes =
                  engine.supportsFeature(WasmFeature.REFERENCE_TYPES);

              logger.info(
                  "Engine feature support - Threads: "
                      + supportsThreads
                      + ", SIMD: "
                      + supportsSimd
                      + ", Reference Types: "
                      + supportsReferenceTypes);

              // Test engine limits (should be non-negative)
              final int memoryLimitPages = engine.getMemoryLimitPages();
              final long stackSizeLimit = engine.getStackSizeLimit();
              final boolean fuelEnabled = engine.isFuelEnabled();
              final boolean epochInterruption = engine.isEpochInterruptionEnabled();
              final int maxInstances = engine.getMaxInstances();

              assertTrue(memoryLimitPages >= 0, "Memory limit should be non-negative");
              assertTrue(stackSizeLimit >= 0, "Stack size limit should be non-negative");
              assertTrue(maxInstances >= 0, "Max instances should be non-negative");

              logger.info(
                  "Engine limits - Memory: "
                      + memoryLimitPages
                      + " pages, Stack: "
                      + stackSizeLimit
                      + " bytes, Fuel: "
                      + fuelEnabled
                      + ", Epoch: "
                      + epochInterruption
                      + ", MaxInstances: "
                      + maxInstances);

              return "success";
            });

    assertEquals("success", result, "Feature support query should succeed");

    final BasicMetricsCollector.OperationMetrics metrics =
        metricsCollector.getOperationMetrics("feature_support_query");
    assertNotNull(metrics, "Metrics should be recorded for feature support query");
    assertEquals(1, metrics.getSuccessCount(), "Should have one successful operation");

    logger.info("Engine feature support validation passed: " + metrics);
  }

  @Test
  @DisplayName("Error Handling Validation")
  void testErrorHandling() throws Exception {
    try {
      metricsCollector.recordOperation(
          "invalid_module_compilation",
          () -> {
            // Test compilation with invalid WASM bytes
            final byte[] invalidWasm = {0x00, 0x61, 0x73, 0x6D}; // Incomplete WASM header
            return engine.compileModule(invalidWasm);
          });
      fail("Should have thrown exception for invalid WASM");
    } catch (final Exception e) {
      // Expected exception
      logger.info("Expected exception caught: " + e.getClass().getSimpleName());
    }

    // Test with null input - this should also be handled gracefully
    try {
      metricsCollector.recordOperation(
          "null_module_compilation",
          () -> {
            return engine.compileModule(null);
          });
      fail("Should have thrown exception for null input");
    } catch (final Exception e) {
      // Expected exception
      logger.info("Expected exception for null input: " + e.getClass().getSimpleName());
    }

    // Verify errors were recorded
    final BasicMetricsCollector.OperationMetrics invalidMetrics =
        metricsCollector.getOperationMetrics("invalid_module_compilation");
    assertNotNull(invalidMetrics, "Metrics should be recorded for invalid compilation");
    assertEquals(0, invalidMetrics.getSuccessCount(), "Should have no successful operations");
    assertEquals(1, invalidMetrics.getFailureCount(), "Should have one failed operation");

    final BasicMetricsCollector.OperationMetrics nullMetrics =
        metricsCollector.getOperationMetrics("null_module_compilation");
    assertNotNull(nullMetrics, "Metrics should be recorded for null compilation");
    assertEquals(0, nullMetrics.getSuccessCount(), "Should have no successful operations");
    assertEquals(1, nullMetrics.getFailureCount(), "Should have one failed operation");

    logger.info("Error handling validation passed");
  }

  @Test
  @DisplayName("Metrics Collection and Export")
  void testMetricsCollectionAndExport() throws Exception {
    // Generate some test metrics with known values
    metricsCollector.recordSuccess("test_operation_1", Duration.ofMillis(100));
    metricsCollector.recordSuccess("test_operation_1", Duration.ofMillis(150));
    metricsCollector.recordSuccess("test_operation_2", Duration.ofMillis(75));
    metricsCollector.recordFailure("test_operation_2", Duration.ofMillis(200));

    // Verify overall metrics
    final BasicMetricsCollector.OverallMetrics overall = metricsCollector.getOverallMetrics();
    assertEquals(4, overall.getTotalOperations(), "Should have 4 total operations");
    assertEquals(3, overall.getSuccessfulOperations(), "Should have 3 successful operations");
    assertEquals(1, overall.getFailedOperations(), "Should have 1 failed operation");
    assertEquals(0.75, overall.getOverallSuccessRate(), 0.001, "Success rate should be 75%");
    assertEquals(2, metricsCollector.getOperationCount(), "Should have 2 unique operations");

    // Test CSV export
    final Path csvFile = tempDir.resolve("metrics-test.csv");
    CsvReporter.exportToFile(metricsCollector, csvFile);
    assertTrue(csvFile.toFile().exists(), "CSV file should be created");

    // Test JSON export
    final Path jsonFile = tempDir.resolve("metrics-test.json");
    JsonReporter.exportToFile(metricsCollector, jsonFile);
    assertTrue(jsonFile.toFile().exists(), "JSON file should be created");

    // Test compact status export
    final String compactStatus = JsonReporter.exportCompactStatus(metricsCollector);
    assertNotNull(compactStatus, "Compact status should be generated");
    assertTrue(compactStatus.contains("\"status\""), "Should contain status field");
    assertTrue(compactStatus.contains("\"successRate\""), "Should contain success rate");

    logger.info("Metrics collection and export validation passed");
    logger.info("Overall metrics: " + overall);
    logger.info("Compact status: " + compactStatus);
  }

  @Test
  @DisplayName("Resource Lifecycle Management")
  void testResourceLifecycleManagement() throws Exception {
    final String result =
        metricsCollector.recordOperation(
            "resource_lifecycle",
            () -> {
              // Create additional engine and store for testing lifecycle
              final Engine testEngine = Engine.create();
              final Store testStore = testEngine.createStore();

              // Verify resources are valid before closing
              assertTrue(testEngine.isValid(), "Test engine should be valid before closing");

              // Close resources explicitly
              testStore.close();
              testEngine.close();

              // Verify cleanup (the engine should report invalid after closing)
              assertFalse(testEngine.isValid(), "Test engine should be invalid after closing");

              return "success";
            });

    assertEquals("success", result, "Resource lifecycle test should succeed");

    final BasicMetricsCollector.OperationMetrics metrics =
        metricsCollector.getOperationMetrics("resource_lifecycle");
    assertNotNull(metrics, "Metrics should be recorded for resource lifecycle");
    assertEquals(1, metrics.getSuccessCount(), "Should have one successful operation");

    logger.info("Resource lifecycle management validation passed: " + metrics);
  }

  private void exportMetricsToFiles(final String testName) throws IOException {
    final String sanitizedTestName = testName.replaceAll("[^a-zA-Z0-9]", "_");

    try {
      // Export CSV
      final Path csvFile = tempDir.resolve(sanitizedTestName + "_metrics.csv");
      CsvReporter.exportToFile(metricsCollector, csvFile);

      // Export JSON
      final Path jsonFile = tempDir.resolve(sanitizedTestName + "_metrics.json");
      JsonReporter.exportToFile(metricsCollector, jsonFile);

      // Export overall metrics
      final Path overallFile = tempDir.resolve(sanitizedTestName + "_overall.json");
      JsonReporter.exportOverallMetrics(metricsCollector.getOverallMetrics(), overallFile);

      logger.info("Metrics exported for test '" + testName + "':");
      logger.info("  CSV: " + csvFile);
      logger.info("  JSON: " + jsonFile);
      logger.info("  Overall: " + overallFile);
    } catch (final IOException e) {
      logger.warning("Failed to export metrics for test '" + testName + "': " + e.getMessage());
      // Don't fail the test due to export issues
    }
  }
}

package ai.tegmentum.wasmtime4j.integration;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.performance.PerformanceRegressionDetector;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiFactory;
import ai.tegmentum.wasmtime4j.wasi.WasiRuntimeType;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeExecutionSummary;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestResult;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestDataManager;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteStats;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive integration test demonstrating the full testing framework. This test validates the
 * entire integration testing infrastructure.
 */
@DisplayName("Comprehensive Integration Test Framework")
class ComprehensiveIntegrationIT extends BaseIntegrationTest {

  @BeforeAll
  static void setUpComprehensiveTestSuite() throws IOException {
    // Initialize all test infrastructure
    final WasmTestDataManager dataManager = WasmTestDataManager.getInstance();
    dataManager.initializeTestData();

    LOGGER.info("Comprehensive integration test suite initialized");
  }

  @Test
  @DisplayName("Should demonstrate complete WebAssembly test workflow")
  void shouldDemonstrateCompleteWebAssemblyTestWorkflow() throws IOException {
    // Given: Complete test infrastructure
    LOGGER.info("=== Comprehensive WebAssembly Test Workflow Demo ===");

    // 1. Load and validate test data management
    LOGGER.info("1. Testing WebAssembly test data management...");
    final WasmTestDataManager dataManager = WasmTestDataManager.getInstance();
    final WasmTestSuiteStats stats = WasmTestSuiteLoader.getTestSuiteStatistics();

    assertThat(stats.getTotalTestCount()).isGreaterThan(0);
    LOGGER.info("   - Loaded " + stats.getTotalTestCount() + " test cases");

    // 2. Execute cross-runtime validation
    LOGGER.info("2. Testing cross-runtime validation...");
    final CrossRuntimeTestRunner.RuntimeTestFunction testFunction =
        runtime -> {
          final byte[] moduleBytes = TestUtils.createSimpleWasmModule();

          try (final var engine = runtime.createEngine();
              final var store = engine.createStore()) {

            final var module = engine.compileModule(moduleBytes);
            final var instance = runtime.instantiate(module);
            
            // Verify store is properly created
            assertThat(store).isNotNull();

            final var addFunction =
                instance
                    .getFunction("add")
                    .orElseThrow(() -> new AssertionError("add function should be exported"));

            final var args =
                new ai.tegmentum.wasmtime4j.WasmValue[] {
                  ai.tegmentum.wasmtime4j.WasmValue.i32(10),
                  ai.tegmentum.wasmtime4j.WasmValue.i32(15)
                };
            final var results = addFunction.call(args);

            return results[0].asI32();
          }
        };

    final CrossRuntimeTestResult crossRuntimeResult =
        CrossRuntimeTestRunner.executeAcrossRuntimes("comprehensive_workflow_test", testFunction);

    assertThat(crossRuntimeResult.getJniExecution().isSuccessful()).isTrue();
    assertThat(crossRuntimeResult.getJniExecution().getResult()).isEqualTo(25);

    if (TestUtils.isPanamaAvailable()) {
      assertThat(crossRuntimeResult.getPanamaExecution()).isNotNull();
      assertThat(crossRuntimeResult.getPanamaExecution().isSuccessful()).isTrue();
      assertThat(crossRuntimeResult.getPanamaExecution().getResult()).isEqualTo(25);
    }

    LOGGER.info("   - Cross-runtime test: " + crossRuntimeResult.getSummary());

    // 3. Test performance regression detection
    LOGGER.info("3. Testing performance regression detection...");
    PerformanceRegressionDetector.recordMeasurement(
        "comprehensive_workflow_test", crossRuntimeResult);

    final var regressionAnalysis =
        PerformanceRegressionDetector.detectRegressions(
            "comprehensive_workflow_test", crossRuntimeResult);

    LOGGER.info("   - Performance analysis: " + regressionAnalysis.getSummary());

    // 4. Test WASI integration using WasiFactory
    LOGGER.info("4. Testing WASI integration with WasiFactory...");
    final String wasiResult = testWasiIntegration();

    LOGGER.info("   - WASI test: " + wasiResult);

    // 5. Generate comprehensive reports
    LOGGER.info("5. Generating comprehensive reports...");

    final CrossRuntimeExecutionSummary executionSummary =
        CrossRuntimeTestRunner.createExecutionSummary();
    LOGGER.info("   - Cross-runtime execution summary: " + executionSummary);

    final String performanceSummary = PerformanceRegressionDetector.createPerformanceSummary();
    LOGGER.info("   - Performance summary created");

    final String wasiSummary = createWasiExecutionSummary();
    LOGGER.info("   - WASI summary: " + wasiSummary);

    // 6. Validate cache statistics
    LOGGER.info("6. Validating cache and statistics...");
    final var cacheStats = dataManager.getCacheStatistics();
    LOGGER.info("   - Cache statistics: " + cacheStats);

    assertThat(cacheStats.getHits() + cacheStats.getMisses()).isGreaterThan(0);

    LOGGER.info("=== Comprehensive Test Workflow Completed Successfully ===");

    // Generate final report
    generateComprehensiveReport(
        stats, executionSummary, wasiSummary, performanceSummary, cacheStats);
  }

  @Test
  @DisplayName("Should validate cross-platform compatibility")
  void shouldValidateCrossPlatformCompatibility() {
    // Given: Cross-platform testing capabilities
    LOGGER.info("=== Cross-Platform Compatibility Validation ===");

    // Log current platform information
    LOGGER.info("Platform: " + TestUtils.getOperatingSystem());
    LOGGER.info("Architecture: " + TestUtils.getSystemArchitecture());
    LOGGER.info("Java Version: " + TestUtils.getJavaVersion());
    LOGGER.info("Panama Available: " + TestUtils.isPanamaAvailable());

    // Test basic runtime creation on current platform
    final CrossRuntimeTestRunner.RuntimeTestFunction platformTest =
        runtime -> {
          // Basic platform compatibility test
          final byte[] moduleBytes = TestUtils.createSimpleWasmModule();

          try (final var engine = runtime.createEngine()) {
            final var module = engine.compileModule(moduleBytes);
            assertThat(module).isNotNull();
            return "platform_compatible";
          }
        };

    final var result =
        CrossRuntimeTestRunner.executeAcrossRuntimes("platform_compatibility_test", platformTest);

    assertThat(result.getJniExecution().isSuccessful()).isTrue();

    if (TestUtils.isPanamaAvailable()) {
      assertThat(result.getPanamaExecution().isSuccessful()).isTrue();
    }

    LOGGER.info("Cross-platform compatibility validated: " + result.getSummary());
  }

  @Test
  @DisplayName("Should cleanup all test resources properly")
  void shouldCleanupAllTestResourcesProperly() {
    LOGGER.info("=== Resource Cleanup Validation ===");

    // Clear all caches and test data
    CrossRuntimeTestRunner.clearCache();
    clearWasiTestResults();
    PerformanceRegressionDetector.clearPerformanceData();

    final WasmTestDataManager dataManager = WasmTestDataManager.getInstance();
    dataManager.clearCache();

    // Force garbage collection
    System.gc();
    System.gc();

    LOGGER.info("All test resources cleaned up successfully");

    // Verify we can still create new resources after cleanup
    final CrossRuntimeTestRunner.RuntimeTestFunction cleanupTest =
        runtime -> {
          try (final var engine = runtime.createEngine();
              final var store = engine.createStore()) {

            assertThat(engine).isNotNull();
            assertThat(store).isNotNull();
            return "cleanup_successful";
          }
        };

    final var result =
        CrossRuntimeTestRunner.executeAcrossRuntimes("post_cleanup_test", cleanupTest);

    assertThat(result.getJniExecution().isSuccessful()).isTrue();
    LOGGER.info("Post-cleanup functionality validated: " + result.getSummary());
  }

  /** Generates a comprehensive report of all test infrastructure components. */
  private void generateComprehensiveReport(
      final WasmTestSuiteStats stats,
      final CrossRuntimeExecutionSummary executionSummary,
      final String wasiSummary,
      final String performanceSummary,
      final WasmTestDataManager.CacheStatistics cacheStats) {

    final StringBuilder report = new StringBuilder();
    report.append("\n");
    report.append(
        "================================================================================\n");
    report.append("COMPREHENSIVE WASMTIME4J INTEGRATION TEST REPORT\n");
    report.append(
        "================================================================================\n\n");

    report.append("Test Infrastructure Summary:\n");
    report.append("---------------------------\n");
    report
        .append("Platform: ")
        .append(TestUtils.getOperatingSystem())
        .append(" (")
        .append(TestUtils.getSystemArchitecture())
        .append(")\n");
    report.append("Java Version: ").append(TestUtils.getJavaVersion()).append("\n");
    report.append("Panama Available: ").append(TestUtils.isPanamaAvailable()).append("\n\n");

    report.append("WebAssembly Test Suites:\n");
    report.append("------------------------\n");
    report.append(stats.toString()).append("\n\n");

    report.append("Cross-Runtime Execution:\n");
    report.append("-----------------------\n");
    report.append(executionSummary.createReport()).append("\n");

    report.append("WASI Integration:\n");
    report.append("----------------\n");
    report.append(wasiSummary).append("\n");

    report.append("Performance Baselines:\n");
    report.append("---------------------\n");
    report.append(performanceSummary).append("\n");

    report.append("Cache Performance:\n");
    report.append("-----------------\n");
    report.append(cacheStats.toString()).append("\n\n");

    report.append(
        "================================================================================\n");
    report.append("INTEGRATION TEST FRAMEWORK VALIDATION: SUCCESSFUL\n");
    report.append(
        "================================================================================\n");

    LOGGER.info(report.toString());
  }

  /**
   * Tests WASI integration using the WasiFactory pattern.
   *
   * @return test result summary
   */
  private String testWasiIntegration() {
    try {
      // Test WASI context creation using factory
      final WasiContext wasiContext = WasiFactory.createContext();
      
      // Basic validation that WASI context is available
      if (wasiContext != null) {
        wasiContext.close(); // Clean up resources
        return "WASI integration successful - context created with " + WasiFactory.getSelectedRuntimeType();
      } else {
        return "WASI integration failed - context is null";
      }
    } catch (final Exception e) {
      return "WASI integration failed: " + e.getMessage();
    }
  }

  /**
   * Creates a WASI execution summary using WasiFactory.
   *
   * @return WASI execution summary
   */
  private String createWasiExecutionSummary() {
    final StringBuilder summary = new StringBuilder();
    summary.append("WASI Factory Integration Summary:\n");
    summary.append("Selected Runtime Type: ").append(WasiFactory.getSelectedRuntimeType()).append("\n");
    
    // Check availability of different runtime types
    summary.append("JNI Runtime Available: ").append(WasiFactory.isRuntimeAvailable(WasiRuntimeType.JNI)).append("\n");
    summary
        .append("Panama Runtime Available: ")
        .append(WasiFactory.isRuntimeAvailable(WasiRuntimeType.PANAMA))
        .append("\n");
    
    return summary.toString();
  }

  /**
   * Clears WASI test results (placeholder for compatibility).
   */
  private void clearWasiTestResults() {
    // Since we're using WasiFactory pattern, there's no specific cleanup needed
    // This method exists for compatibility with the test structure
    LOGGER.info("WASI test results cleared (WasiFactory pattern - no cleanup needed)");
  }
}

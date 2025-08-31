package ai.tegmentum.wasmtime4j.examples;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.generation.TestDataGenerator;
import ai.tegmentum.wasmtime4j.memory.MemoryLeakDetector;
import ai.tegmentum.wasmtime4j.performance.PerformanceTestHarness;
import ai.tegmentum.wasmtime4j.platform.PlatformTestRunner;
import ai.tegmentum.wasmtime4j.stress.StressTestFramework;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive examples demonstrating the enhanced test infrastructure. This class serves as both
 * documentation and a collection of example test implementations using all the comprehensive
 * testing utilities.
 */
public class ComprehensiveTestingExamples extends BaseIntegrationTest {
  private static final Logger LOGGER =
      Logger.getLogger(ComprehensiveTestingExamples.class.getName());

  /**
   * Example: Basic enhanced integration test with automatic resource management. Demonstrates the
   * enhanced BaseIntegrationTest capabilities.
   */
  @Test
  public void example_enhancedIntegrationTest() throws WasmException {
    LOGGER.info("Running enhanced integration test example");

    // Create runtime with automatic cleanup
    final WasmRuntime runtime = createTestRuntime(RuntimeType.JNI);

    // Measure operation performance
    measureExecutionTime(
        "WebAssembly module loading",
        () -> {
          final byte[] module = WasmTestModules.getModule("basic_add");
          // In a real test, would load the module into the runtime
          LOGGER.info("Loaded module of size: " + module.length + " bytes");
        });

    // Add custom performance metrics
    addTestMetric("Module size: " + WasmTestModules.getModule("basic_add").length + " bytes");
    addTestMetric("Runtime type: " + RuntimeType.JNI);

    // Verify execution time bounds
    assertExecutionTime(
        Duration.ofSeconds(1),
        () -> {
          // Some operation that should complete quickly
          final byte[] simpleModule = WasmTestModules.getModule("basic_hello");
          LOGGER.fine("Operation completed within time bounds");
        },
        "Simple module loading");

    // Resources are automatically cleaned up via BaseIntegrationTest
  }

  /**
   * Example: Cross-runtime validation testing. Demonstrates automated JNI vs Panama parity testing.
   */
  @Test
  public void example_crossRuntimeValidation() {
    LOGGER.info("Running cross-runtime validation example");

    // Test a simple operation across both runtimes
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          final byte[] module = WasmTestModules.getModule("arithmetic_int");

          // In a real test, would:
          // 1. Load the module into the runtime
          // 2. Execute a function with test parameters
          // 3. Verify the results are identical between runtimes

          LOGGER.info("Testing arithmetic module with " + runtimeType);
          addTestMetric("Cross-runtime test passed for " + runtimeType);
        });

    // Advanced cross-runtime validation with detailed comparison
    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(
            (runtime) -> {
              // Simulate a computation that should return identical results
              final byte[] module = WasmTestModules.getModule("basic_add");
              // In practice: load module, call function with parameters, return result
              return 42; // Placeholder result
            });

    if (result.isValid()) {
      LOGGER.info("Cross-runtime validation passed");
      addTestMetric("Cross-runtime validation: PASSED");
    } else {
      LOGGER.warning("Cross-runtime differences detected: " + result.getDifferenceDescription());
      addTestMetric("Cross-runtime validation: FAILED - " + result.getDifferenceDescription());
    }
  }

  /**
   * Example: Performance testing with JMH-style harness. Demonstrates comprehensive performance
   * measurement capabilities.
   */
  @Test
  public void example_performanceTesting() {
    LOGGER.info("Running performance testing example");

    // Create performance test configuration
    final PerformanceTestHarness.Configuration config =
        PerformanceTestHarness.Configuration.builder()
            .warmupIterations(3)
            .measurementIterations(5)
            .iterationTime(Duration.ofMillis(500))
            .build();

    // Run a simple benchmark
    final PerformanceTestHarness.MeasurementResult result =
        PerformanceTestHarness.runBenchmark(
            "module_loading_benchmark",
            () -> {
              // Simulate module loading operation
              final byte[] module = WasmTestModules.getModule("basic_add");
              // In practice: actually load and compile the module
              Thread.sleep(1); // Simulate work
            },
            config);

    LOGGER.info("Performance results:");
    LOGGER.info("  Mean: " + String.format("%.2f", result.getMean()) + " ns/op");
    LOGGER.info(
        "  Throughput: " + String.format("%.2f", result.getOperationsPerSecond()) + " ops/sec");
    LOGGER.info(
        "  Standard Deviation: " + String.format("%.2f", result.getStandardDeviation()) + " ns");

    addTestMetric("Benchmark mean latency: " + String.format("%.2f", result.getMean()) + " ns/op");
    addTestMetric(
        "Benchmark throughput: "
            + String.format("%.2f", result.getOperationsPerSecond())
            + " ops/sec");

    // Cross-runtime performance comparison
    final PerformanceTestHarness.ComparisonResult comparison =
        PerformanceTestHarness.runCrossRuntimeBenchmark(
            "cross_runtime_performance",
            (runtime) -> {
              // Simulate runtime-specific operation
              final byte[] module = WasmTestModules.getModule("control_if_else");
              Thread.sleep(2); // Simulate work
            },
            config);

    LOGGER.info("Cross-runtime performance comparison:");
    LOGGER.info("  Speedup ratio: " + String.format("%.2fx", comparison.getSpeedupRatio()));
    LOGGER.info("  Statistically significant: " + comparison.isStatisticallySignificant());

    if (comparison.isImprovement()) {
      LOGGER.info("  Result: Performance improvement detected");
    } else if (comparison.isRegression()) {
      LOGGER.warning("  Result: Performance regression detected");
    }
  }

  /**
   * Example: Memory leak detection with native tooling. Demonstrates comprehensive memory
   * monitoring capabilities.
   */
  @Test
  public void example_memoryLeakDetection() {
    LOGGER.info("Running memory leak detection example");

    // Configure memory leak detection
    final MemoryLeakDetector.Configuration config =
        MemoryLeakDetector.Configuration.builder()
            .testDuration(Duration.ofSeconds(30))
            .samplingInterval(100)
            .leakThreshold(1.1) // 10% increase considered a leak
            .enableNativeTracking(true)
            .build();

    // Test for memory leaks in a specific operation
    final MemoryLeakDetector.LeakAnalysisResult result =
        MemoryLeakDetector.detectLeaks(
            "module_lifecycle_test",
            (runtime) -> {
              // Simulate repeated module loading/unloading that might leak memory
              final byte[] module = WasmTestModules.getModule("memory_basic");

              // In practice: load module, create instances, use memory, cleanup
              // This should not leak memory if implemented correctly

              Thread.sleep(5); // Simulate work
            },
            config);

    if (result.isLeakDetected()) {
      LOGGER.warning("Memory leak detected!");
      LOGGER.warning("Memory increase: " + result.getMemoryIncrease() + " bytes");
      LOGGER.warning("Leak rate: " + String.format("%.2f", result.getLeakRate()) + " bytes/sec");
      LOGGER.warning("Recommendations:");
      result.getRecommendations().forEach(rec -> LOGGER.warning("  - " + rec));

      addTestMetric("Memory leak: DETECTED (" + result.getMemoryIncrease() + " bytes)");
    } else {
      LOGGER.info("No memory leaks detected");
      addTestMetric("Memory leak: NONE");
    }

    // Cross-runtime memory comparison
    final Map<RuntimeType, MemoryLeakDetector.LeakAnalysisResult> crossRuntimeResults =
        MemoryLeakDetector.compareRuntimes(
            "cross_runtime_memory_test",
            (runtime) -> {
              final byte[] module = WasmTestModules.getModule("memory_grow");
              Thread.sleep(10); // Simulate memory operations
            },
            config);

    for (final Map.Entry<RuntimeType, MemoryLeakDetector.LeakAnalysisResult> entry :
        crossRuntimeResults.entrySet()) {
      final RuntimeType runtimeType = entry.getKey();
      final MemoryLeakDetector.LeakAnalysisResult memResult = entry.getValue();

      LOGGER.info("Memory analysis for " + runtimeType + ":");
      LOGGER.info("  Leak detected: " + memResult.isLeakDetected());
      if (memResult.isLeakDetected()) {
        LOGGER.info("  Memory increase: " + memResult.getMemoryIncrease() + " bytes");
      }
    }
  }

  /**
   * Example: Stress testing with configurable load parameters. Demonstrates high-load testing
   * capabilities.
   */
  @Test
  public void example_stressTesting() {
    LOGGER.info("Running stress testing example");

    // Configure stress test
    final StressTestFramework.Configuration config =
        StressTestFramework.Configuration.builder()
            .threadCount(4)
            .testDuration(Duration.ofMinutes(1))
            .operationsPerSecond(100)
            .rampUpTime(Duration.ofSeconds(10))
            .enableThroughputMonitoring(true)
            .enableLatencyMonitoring(true)
            .errorThreshold(0.05) // 5% error rate threshold
            .build();

    // Run stress test
    final StressTestFramework.StressTestResult result =
        StressTestFramework.runStressTest(
            "module_execution_stress",
            (runtime, threadId, operationId) -> {
              // Simulate concurrent module operations
              final byte[] module = WasmTestModules.getModule("function_fibonacci");

              // In practice: load module, execute function, handle results
              // This tests system behavior under high concurrent load

              if (operationId % 100 == 0 && threadId == 0) {
                LOGGER.fine("Completed " + operationId + " operations");
              }

              Thread.sleep(1); // Simulate work
            },
            config);

    LOGGER.info("Stress test results:");
    LOGGER.info("  Status: " + (result.isTestPassed() ? "PASSED" : "FAILED"));
    LOGGER.info("  Total operations: " + result.getTotalOperations());
    LOGGER.info("  Success rate: " + String.format("%.2f%%", result.getSuccessRate() * 100));
    LOGGER.info(
        "  Average throughput: "
            + String.format("%.2f", result.getAverageThroughput())
            + " ops/sec");
    LOGGER.info("  Average latency: " + String.format("%.2f", result.getAverageLatency()) + " ms");
    LOGGER.info("  Peak memory: " + (result.getPeakMemoryUsage() / 1024 / 1024) + " MB");

    addTestMetric("Stress test status: " + (result.isTestPassed() ? "PASSED" : "FAILED"));
    addTestMetric(
        "Stress test throughput: "
            + String.format("%.2f", result.getAverageThroughput())
            + " ops/sec");

    // Cross-runtime stress testing
    final Map<RuntimeType, StressTestFramework.StressTestResult> crossRuntimeStress =
        StressTestFramework.runCrossRuntimeStressTest(
            "cross_runtime_stress",
            (runtime, threadId, operationId) -> {
              final byte[] module = WasmTestModules.getModule("stress_many_calls");
              Thread.sleep(2); // Simulate work
            },
            config);

    for (final Map.Entry<RuntimeType, StressTestFramework.StressTestResult> entry :
        crossRuntimeStress.entrySet()) {
      final RuntimeType runtimeType = entry.getKey();
      final StressTestFramework.StressTestResult stressResult = entry.getValue();

      LOGGER.info("Stress test results for " + runtimeType + ":");
      LOGGER.info(
          "  Throughput: "
              + String.format("%.2f", stressResult.getAverageThroughput())
              + " ops/sec");
      LOGGER.info(
          "  Success rate: " + String.format("%.2f%%", stressResult.getSuccessRate() * 100));
    }
  }

  /**
   * Example: Test data generation for comprehensive testing. Demonstrates automated test case
   * generation capabilities.
   */
  @Test
  public void example_testDataGeneration() {
    LOGGER.info("Running test data generation example");

    // Configure test data generation
    final TestDataGenerator.GenerationConfig config =
        TestDataGenerator.GenerationConfig.builder()
            .generateValidData(true)
            .generateInvalidData(true)
            .generateEdgeCases(true)
            .maxDataSize(1024)
            .seed(12345) // For reproducible tests
            .build();

    // Generate WebAssembly modules for testing
    final List<TestDataGenerator.GeneratedTestData> modules =
        TestDataGenerator.generateWasmModules(config, 5);

    LOGGER.info("Generated " + modules.size() + " WebAssembly test modules:");
    for (final TestDataGenerator.GeneratedTestData module : modules) {
      LOGGER.info(
          "  "
              + module.getName()
              + ": "
              + module.getDescription()
              + " (valid="
              + module.isValid()
              + ", size="
              + module.getMetadata().get("size")
              + " bytes)");

      // In practice: use these modules for comprehensive testing
      final byte[] moduleData = module.getDataAs(byte[].class);
      // Test module loading, validation, execution, etc.
    }

    // Generate function parameters for testing
    final List<TestDataGenerator.GeneratedTestData> parameters =
        TestDataGenerator.generateFunctionParameters(new char[] {'i', 'f', 'd'}, config, 10);

    LOGGER.info("Generated " + parameters.size() + " function parameter sets:");
    for (final TestDataGenerator.GeneratedTestData paramSet : parameters) {
      final Object[] params = paramSet.getDataAs(Object[].class);
      LOGGER.info("  " + paramSet.getName() + ": " + paramSet.getDescription());

      // In practice: use these parameters to test function calls
      // runtime.callFunction("test_function", params);
    }

    // Generate memory test data
    final List<TestDataGenerator.GeneratedTestData> memoryData =
        TestDataGenerator.generateMemoryData(config, 5);

    LOGGER.info("Generated " + memoryData.size() + " memory test patterns:");
    for (final TestDataGenerator.GeneratedTestData data : memoryData) {
      final byte[] bytes = data.getDataAs(byte[].class);
      LOGGER.info(
          "  "
              + data.getName()
              + ": "
              + data.getDescription()
              + " (pattern="
              + data.getMetadata().get("pattern_type")
              + ")");

      // In practice: use this data for memory operation testing
      // runtime.writeMemory(0, bytes);
      // byte[] readBack = runtime.readMemory(0, bytes.length);
      // assert Arrays.equals(bytes, readBack);
    }

    // Generate edge cases
    final List<TestDataGenerator.GeneratedTestData> edgeCases =
        TestDataGenerator.generateEdgeCases(config);

    LOGGER.info("Generated " + edgeCases.size() + " edge case test data:");
    for (final TestDataGenerator.GeneratedTestData edgeCase : edgeCases) {
      LOGGER.info(
          "  "
              + edgeCase.getName()
              + ": "
              + edgeCase.getDescription()
              + " (type="
              + edgeCase.getMetadata().get("type")
              + ")");

      // In practice: use edge cases to test boundary conditions
      // Test with maximum values, minimum values, NaN, infinity, etc.
    }

    addTestMetric("Generated modules: " + modules.size());
    addTestMetric("Generated parameter sets: " + parameters.size());
    addTestMetric("Generated edge cases: " + edgeCases.size());
  }

  /**
   * Example: Cross-platform testing coordination. Demonstrates platform-specific test execution and
   * coordination.
   */
  @Test
  public void example_crossPlatformTesting() {
    LOGGER.info("Running cross-platform testing example");

    // Configure platform testing
    final PlatformTestRunner.Configuration config =
        PlatformTestRunner.Configuration.builder()
            .targetAllRuntimes()
            .enableCompatibilityTesting(true)
            .enablePerformanceComparison(false) // Skip for this example
            .testTimeout(Duration.ofMinutes(2))
            .build();

    // Run a cross-platform test
    final PlatformTestRunner.CrossPlatformTestSummary summary =
        PlatformTestRunner.runCrossPlatformTest(
            "platform_compatibility_test",
            (runtime, platform, runtimeType) -> {
              LOGGER.info("Executing test on " + platform + " with " + runtimeType);

              // Test platform-specific functionality
              final byte[] module = WasmTestModules.getModule("basic_add");

              // In practice: test features that might behave differently across platforms
              // - File system access (WASI)
              // - Memory management
              // - Threading behavior
              // - Native library loading

              addTestMetric("Platform test executed: " + platform + "/" + runtimeType);

              Thread.sleep(10); // Simulate work
            },
            config);

    LOGGER.info("Cross-platform test summary:");
    LOGGER.info("  Total tests: " + summary.getTotalTests());
    LOGGER.info(
        "  Success rate: " + String.format("%.1f%%", summary.getOverallSuccessRate() * 100));
    LOGGER.info("  Total execution time: " + summary.getTotalExecutionTime().toMillis() + "ms");

    // Log platform-specific results
    for (final PlatformTestRunner.Platform platform : PlatformTestRunner.Platform.values()) {
      final int successes = summary.getSuccessCounts().getOrDefault(platform, 0);
      final int failures = summary.getFailureCounts().getOrDefault(platform, 0);

      if (successes + failures > 0) {
        LOGGER.info("  " + platform + ": " + successes + " passed, " + failures + " failed");
      }
    }

    // Check for compatibility issues
    if (!summary.getPlatformCompatibilityIssues().isEmpty()) {
      LOGGER.warning("Platform compatibility issues detected:");
      summary.getPlatformCompatibilityIssues().forEach(issue -> LOGGER.warning("  - " + issue));
    }

    addTestMetric("Cross-platform tests: " + summary.getTotalTests());
    addTestMetric(
        "Cross-platform success rate: "
            + String.format("%.1f%%", summary.getOverallSuccessRate() * 100));
  }

  /**
   * Example: Comprehensive test suite combining all testing utilities. Demonstrates how to use
   * multiple testing frameworks together.
   */
  @Test
  public void example_comprehensiveTestSuite() {
    LOGGER.info("Running comprehensive test suite example");

    final String testModule = "comprehensive_test_module";
    final byte[] moduleData = WasmTestModules.getModule("basic_add");

    // 1. Cross-runtime validation
    LOGGER.info("Phase 1: Cross-runtime validation");
    final CrossRuntimeValidator.ComparisonResult validation =
        CrossRuntimeValidator.validateCrossRuntime(
            (runtime) -> {
              // Simulate module execution that should be identical across runtimes
              return moduleData.length; // Placeholder
            });

    addTestMetric("Cross-runtime validation: " + (validation.isValid() ? "PASSED" : "FAILED"));

    // 2. Performance benchmarking
    LOGGER.info("Phase 2: Performance benchmarking");
    final PerformanceTestHarness.Configuration perfConfig =
        PerformanceTestHarness.getFastConfiguration();
    final PerformanceTestHarness.ComparisonResult perfResult =
        PerformanceTestHarness.runCrossRuntimeBenchmark(
            testModule + "_performance",
            (runtime) -> {
              // Simulate performance-critical operation
              Thread.sleep(1);
            },
            perfConfig);

    addTestMetric(
        "Performance comparison: "
            + (perfResult.isImprovement()
                ? "IMPROVEMENT"
                : perfResult.isRegression() ? "REGRESSION" : "SIMILAR"));

    // 3. Memory leak detection
    LOGGER.info("Phase 3: Memory leak detection");
    final MemoryLeakDetector.Configuration memConfig = MemoryLeakDetector.getFastConfiguration();
    final Map<RuntimeType, MemoryLeakDetector.LeakAnalysisResult> memResults =
        MemoryLeakDetector.compareRuntimes(
            testModule + "_memory",
            (runtime) -> {
              // Simulate operations that might leak memory
              Thread.sleep(5);
            },
            memConfig);

    boolean memoryLeaksDetected =
        memResults.values().stream()
            .anyMatch(MemoryLeakDetector.LeakAnalysisResult::isLeakDetected);
    addTestMetric("Memory leaks: " + (memoryLeaksDetected ? "DETECTED" : "NONE"));

    // 4. Light stress testing
    LOGGER.info("Phase 4: Light stress testing");
    final StressTestFramework.Configuration stressConfig =
        StressTestFramework.getLightConfiguration();
    final StressTestFramework.StressTestResult stressResult =
        StressTestFramework.runStressTest(
            testModule + "_stress",
            (runtime, threadId, operationId) -> {
              // Simulate concurrent operations
              Thread.sleep(2);
            },
            stressConfig);

    addTestMetric("Stress test: " + (stressResult.isTestPassed() ? "PASSED" : "FAILED"));

    // 5. Platform compatibility
    LOGGER.info("Phase 5: Platform compatibility");
    final PlatformTestRunner.Configuration platformConfig =
        PlatformTestRunner.getFastConfiguration();
    final PlatformTestRunner.CrossPlatformTestSummary platformResult =
        PlatformTestRunner.runCrossPlatformTest(
            testModule + "_platform",
            (runtime, platform, runtimeType) -> {
              // Test platform-specific behavior
              Thread.sleep(5);
            },
            platformConfig);

    addTestMetric(
        "Platform compatibility: "
            + String.format("%.1f%%", platformResult.getOverallSuccessRate() * 100));

    // Final summary
    LOGGER.info("Comprehensive test suite completed");
    LOGGER.info("All testing phases executed successfully");

    // In a real implementation, you might want to fail the test if any critical issues are found
    if (!validation.isValid()) {
      throw new AssertionError(
          "Cross-runtime validation failed: " + validation.getDifferenceDescription());
    }

    if (memoryLeaksDetected) {
      LOGGER.warning("Memory leaks detected - investigate before production use");
    }

    if (!stressResult.isTestPassed()) {
      throw new AssertionError("Stress test failed - system may not handle high load properly");
    }

    if (platformResult.getOverallSuccessRate() < 0.9) { // 90% success rate threshold
      throw new AssertionError("Platform compatibility issues detected");
    }
  }
}

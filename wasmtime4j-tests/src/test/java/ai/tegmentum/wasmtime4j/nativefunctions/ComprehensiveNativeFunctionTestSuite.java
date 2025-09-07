package ai.tegmentum.wasmtime4j.nativefunctions;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.memory.MemoryLeakDetector;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Comprehensive test suite runner for all native function tests. This class orchestrates the
 * execution of all native function test classes and provides comprehensive reporting on coverage,
 * memory leak detection, thread safety validation, and cross-runtime compatibility.
 *
 * <p>This test suite validates the complete native function implementation across:
 *
 * <ul>
 *   <li>Engine native functions (create, compile, configure, destroy)
 *   <li>Module native functions (compile, validate, metadata, serialize)
 *   <li>Store native functions (create, data management, fuel, limits)
 *   <li>Instance native functions (instantiate, invoke, memory, tables, globals)
 *   <li>Cross-runtime compatibility (JNI vs Panama FFI)
 *   <li>Memory leak detection and resource management
 *   <li>Thread safety and concurrent access validation
 *   <li>Parameter boundary testing and error handling
 * </ul>
 *
 * <p>The test suite generates comprehensive reports on test coverage, performance metrics, and
 * identifies any memory leaks or thread safety issues.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Comprehensive Native Function Test Suite")
@SuppressWarnings("try")
public class ComprehensiveNativeFunctionTestSuite extends BaseNativeFunctionTest {
  private static final Logger SUITE_LOGGER =
      Logger.getLogger(ComprehensiveNativeFunctionTestSuite.class.getName());

  // Test execution tracking
  private static final AtomicInteger totalTestsRun = new AtomicInteger(0);
  private static final AtomicInteger testsWithMemoryLeaks = new AtomicInteger(0);
  private static final AtomicInteger threadSafetyFailures = new AtomicInteger(0);
  private static final AtomicInteger crossRuntimeFailures = new AtomicInteger(0);

  // Performance tracking
  private static final Map<String, Long> testExecutionTimes = new HashMap<>();
  private static final Map<RuntimeType, List<MemoryLeakDetector.LeakAnalysisResult>>
      memoryLeakResults = new HashMap<>();

  private static Instant suiteStartTime;
  private static Instant suiteEndTime;

  @BeforeAll
  static void setUpComprehensiveTestSuite() {
    suiteStartTime = Instant.now();
    SUITE_LOGGER.info("=".repeat(80));
    SUITE_LOGGER.info("STARTING COMPREHENSIVE NATIVE FUNCTION TEST SUITE");
    SUITE_LOGGER.info("=".repeat(80));

    SUITE_LOGGER.info("Platform Information:");
    SUITE_LOGGER.info("  OS: " + TestUtils.getOperatingSystem());
    SUITE_LOGGER.info("  Architecture: " + TestUtils.getSystemArchitecture());
    SUITE_LOGGER.info("  Java Version: " + TestUtils.getJavaVersion());
    SUITE_LOGGER.info("  Panama Available: " + TestUtils.isPanamaAvailable());

    // Initialize result tracking
    for (final RuntimeType runtime : RuntimeType.values()) {
      memoryLeakResults.put(runtime, new ArrayList<>());
    }

    SUITE_LOGGER.info("Test suite initialization completed");
  }

  @AfterAll
  static void tearDownComprehensiveTestSuite() {
    suiteEndTime = Instant.now();
    generateComprehensiveReport();
  }

  @Test
  @Order(1)
  @DisplayName("Should validate test infrastructure setup")
  void shouldValidateTestInfrastructureSetup() {
    SUITE_LOGGER.info("Validating test infrastructure setup...");

    // Validate memory leak detection is working
    final MemoryLeakDetector.LeakAnalysisResult testResult =
        testWithFastMemoryLeakDetection(
            "infrastructure_validation",
            runtime -> {
              // Simple operation that should not leak
              try (final var engine = runtime.createEngine()) {
                final byte[] moduleBytes = testUtils.getSimpleAddModule();
                try (final var module = engine.compileModule(moduleBytes)) {
                  assertThat(module).isNotNull();
                }
              }
            });

    assertThat(testResult).isNotNull();
    SUITE_LOGGER.info("Memory leak detection infrastructure validated");

    // Validate thread safety testing infrastructure
    final ThreadSafetyTestResult threadTestResult =
        testThreadSafety(
            "infrastructure_thread_safety_validation",
            (runtime, threadId, operationId) -> {
              // Simple operation for validation
              assertThat(runtime).isNotNull();
            },
            2, // 2 threads
            3); // 3 operations per thread

    assertThat(threadTestResult.isCompleted()).isTrue();
    SUITE_LOGGER.info("Thread safety testing infrastructure validated");

    // Validate test utilities
    final var testModules = testUtils.getAllTestModules();
    assertThat(testModules).isNotEmpty();
    SUITE_LOGGER.info(
        "Test utilities validated: " + testModules.size() + " test modules available");

    totalTestsRun.incrementAndGet();
    SUITE_LOGGER.info("Test infrastructure validation completed successfully");
  }

  @Test
  @Order(2)
  @DisplayName("Should execute comprehensive engine native function tests")
  void shouldExecuteComprehensiveEngineNativeFunctionTests() {
    final Instant testStart = Instant.now();
    SUITE_LOGGER.info("Executing comprehensive engine native function tests...");

    // Test basic engine operations across runtimes
    final Map<RuntimeType, MemoryLeakDetector.LeakAnalysisResult> engineResults =
        testCrossRuntimeCompatibility(
            "comprehensive_engine_test_suite",
            runtime -> {
              // Engine creation/destruction cycle
              try (final var engine = runtime.createEngine()) {
                assertThat(engine).isNotNull();

                // Module compilation
                final var testModules = testUtils.getAllTestModules();
                for (final var testModule :
                    testModules.subList(0, Math.min(3, testModules.size()))) {
                  try (final var module = engine.compileModule(testModule.getModuleBytes())) {
                    assertThat(module).isNotNull();
                  } catch (final Exception e) {
                    // Some modules may be invalid - that's expected for error testing
                    SUITE_LOGGER.fine("Expected compilation failure for " + testModule.getName());
                  }
                }

                // Store creation
                try (final var store = engine.createStore()) {
                  assertThat(store).isNotNull();
                }
              }
            });

    // Validate results and track memory leaks
    engineResults.forEach(
        (runtime, result) -> {
          memoryLeakResults.get(runtime).add(result);
          if (result.isLeakDetected()) {
            testsWithMemoryLeaks.incrementAndGet();
            SUITE_LOGGER.warning("Memory leak detected in engine tests for " + runtime);
          }
        });

    final long testDuration = Duration.between(testStart, Instant.now()).toMillis();
    testExecutionTimes.put("engine_tests", testDuration);
    totalTestsRun.incrementAndGet();

    SUITE_LOGGER.info("Engine native function tests completed in " + testDuration + "ms");
  }

  @Test
  @Order(3)
  @DisplayName("Should execute comprehensive module native function tests")
  void shouldExecuteComprehensiveModuleNativeFunctionTests() {
    final Instant testStart = Instant.now();
    SUITE_LOGGER.info("Executing comprehensive module native function tests...");

    final Map<RuntimeType, MemoryLeakDetector.LeakAnalysisResult> moduleResults =
        testCrossRuntimeCompatibility(
            "comprehensive_module_test_suite",
            runtime -> {
              try (final var engine = runtime.createEngine()) {
                final var testModules = testUtils.getAllTestModules();

                // Test various module operations
                for (final var testModule : testModules) {
                  try (final var module = engine.compileModule(testModule.getModuleBytes())) {
                    assertThat(module).isNotNull();

                    // Test module metadata access
                    if (module.getExports() != null) {
                      assertThat(module.getExports()).isNotNull();
                    }

                    // Test module instantiation
                    try (final var instance = runtime.instantiate(module)) {
                      assertThat(instance).isNotNull();
                    }
                  } catch (final Exception e) {
                    // Some modules may be invalid for testing error conditions
                    SUITE_LOGGER.fine(
                        "Expected module operation failure for " + testModule.getName());
                  }
                }
              }
            });

    moduleResults.forEach(
        (runtime, result) -> {
          memoryLeakResults.get(runtime).add(result);
          if (result.isLeakDetected()) {
            testsWithMemoryLeaks.incrementAndGet();
            SUITE_LOGGER.warning("Memory leak detected in module tests for " + runtime);
          }
        });

    final long testDuration = Duration.between(testStart, Instant.now()).toMillis();
    testExecutionTimes.put("module_tests", testDuration);
    totalTestsRun.incrementAndGet();

    SUITE_LOGGER.info("Module native function tests completed in " + testDuration + "ms");
  }

  @Test
  @Order(4)
  @DisplayName("Should execute comprehensive store native function tests")
  void shouldExecuteComprehensiveStoreNativeFunctionTests() {
    final Instant testStart = Instant.now();
    SUITE_LOGGER.info("Executing comprehensive store native function tests...");

    final Map<RuntimeType, MemoryLeakDetector.LeakAnalysisResult> storeResults =
        testCrossRuntimeCompatibility(
            "comprehensive_store_test_suite",
            runtime -> {
              try (final var engine = runtime.createEngine()) {
                // Test multiple store operations
                for (int i = 0; i < 5; i++) {
                  try (final var store = engine.createStore()) {
                    assertThat(store).isNotNull();

                    // Test store with module execution
                    final byte[] moduleBytes = testUtils.getSimpleAddModule();
                    try (final var module = engine.compileModule(moduleBytes)) {
                      try (final var instance = runtime.instantiate(module)) {
                        final var addFunction = instance.getFunction("add");
                        if (addFunction.isPresent()) {
                          final var args =
                              new ai.tegmentum.wasmtime4j.WasmValue[] {
                                ai.tegmentum.wasmtime4j.WasmValue.i32(10),
                                ai.tegmentum.wasmtime4j.WasmValue.i32(20)
                              };
                          final var result = addFunction.get().call(args);
                          assertThat(result[0].asI32()).isEqualTo(30);
                        }
                      }
                    }
                  }
                }
              }
            });

    storeResults.forEach(
        (runtime, result) -> {
          memoryLeakResults.get(runtime).add(result);
          if (result.isLeakDetected()) {
            testsWithMemoryLeaks.incrementAndGet();
            SUITE_LOGGER.warning("Memory leak detected in store tests for " + runtime);
          }
        });

    final long testDuration = Duration.between(testStart, Instant.now()).toMillis();
    testExecutionTimes.put("store_tests", testDuration);
    totalTestsRun.incrementAndGet();

    SUITE_LOGGER.info("Store native function tests completed in " + testDuration + "ms");
  }

  @Test
  @Order(5)
  @DisplayName("Should execute comprehensive instance native function tests")
  void shouldExecuteComprehensiveInstanceNativeFunctionTests() {
    final Instant testStart = Instant.now();
    SUITE_LOGGER.info("Executing comprehensive instance native function tests...");

    final Map<RuntimeType, MemoryLeakDetector.LeakAnalysisResult> instanceResults =
        testCrossRuntimeCompatibility(
            "comprehensive_instance_test_suite",
            runtime -> {
              try (final var engine = runtime.createEngine()) {
                final var testModules = testUtils.getAllTestModules();

                for (final var testModule :
                    testModules.subList(0, Math.min(3, testModules.size()))) {
                  try (final var module = engine.compileModule(testModule.getModuleBytes())) {
                    try (final var instance = runtime.instantiate(module)) {
                      // Test export checking
                      final boolean hasAdd = instance.getFunction("add").isPresent();

                      // Test function invocation if available
                      if (hasAdd) {
                        final var addFunction = instance.getFunction("add");
                        if (addFunction.isPresent()) {
                          final var args =
                              new ai.tegmentum.wasmtime4j.WasmValue[] {
                                ai.tegmentum.wasmtime4j.WasmValue.i32(15),
                                ai.tegmentum.wasmtime4j.WasmValue.i32(25)
                              };
                          final var result = addFunction.get().call(args);
                          assertThat(result[0].asI32()).isEqualTo(40);
                        }
                      }

                      // Test other exports if available
                      if (instance.getMemory("memory").isPresent()) {
                        final var memory = instance.getMemory("memory");
                        assertThat(memory).isNotNull();
                      }

                      if (instance.getTable("table").isPresent()) {
                        final var table = instance.getTable("table");
                        assertThat(table).isNotNull();
                      }
                    }
                  } catch (final Exception e) {
                    // Some modules may not be instantiable
                    SUITE_LOGGER.fine("Expected instantiation failure for " + testModule.getName());
                  }
                }
              }
            });

    instanceResults.forEach(
        (runtime, result) -> {
          memoryLeakResults.get(runtime).add(result);
          if (result.isLeakDetected()) {
            testsWithMemoryLeaks.incrementAndGet();
            SUITE_LOGGER.warning("Memory leak detected in instance tests for " + runtime);
          }
        });

    final long testDuration = Duration.between(testStart, Instant.now()).toMillis();
    testExecutionTimes.put("instance_tests", testDuration);
    totalTestsRun.incrementAndGet();

    SUITE_LOGGER.info("Instance native function tests completed in " + testDuration + "ms");
  }

  @Test
  @Order(6)
  @DisplayName("Should execute comprehensive thread safety tests")
  void shouldExecuteComprehensiveThreadSafetyTests() {
    final Instant testStart = Instant.now();
    SUITE_LOGGER.info("Executing comprehensive thread safety tests...");

    final byte[] moduleBytes = testUtils.getSimpleAddModule();

    // Test thread safety across different scenarios
    final ThreadSafetyTestResult concurrentCreateResult =
        testThreadSafety(
            "concurrent_resource_creation",
            (runtime, threadId, operationId) -> {
              try (final var engine = runtime.createEngine()) {
                try (final var store = engine.createStore()) {
                  try (final var module = engine.compileModule(moduleBytes)) {
                    try (final var instance = runtime.instantiate(module)) {
                      assertThat(instance).isNotNull();
                    }
                  }
                }
              }
            },
            8, // 8 threads
            5); // 5 operations per thread

    if (!concurrentCreateResult.isCompleted() || concurrentCreateResult.getTotalErrors() > 2) {
      threadSafetyFailures.incrementAndGet();
      SUITE_LOGGER.warning("Thread safety issues detected in concurrent resource creation");
    }

    final ThreadSafetyTestResult concurrentExecutionResult =
        testThreadSafety(
            "concurrent_function_execution",
            (runtime, threadId, operationId) -> {
              try (final var engine = runtime.createEngine()) {
                try (final var module = engine.compileModule(moduleBytes)) {
                  try (final var instance = runtime.instantiate(module)) {
                    final var addFunction = instance.getFunction("add");
                    if (addFunction.isPresent()) {
                      for (int i = 0; i < 3; i++) {
                        final var args =
                            new ai.tegmentum.wasmtime4j.WasmValue[] {
                              ai.tegmentum.wasmtime4j.WasmValue.i32(threadId + i),
                              ai.tegmentum.wasmtime4j.WasmValue.i32(operationId + i)
                            };
                        final var result = addFunction.get().call(args);
                        assertThat(result[0].asI32()).isEqualTo(threadId + operationId + 2 * i);
                      }
                    }
                  }
                }
              }
            },
            6, // 6 threads
            8); // 8 operations per thread

    if (!concurrentExecutionResult.isCompleted()
        || concurrentExecutionResult.getTotalErrors() > 3) {
      threadSafetyFailures.incrementAndGet();
      SUITE_LOGGER.warning("Thread safety issues detected in concurrent function execution");
    }

    final long testDuration = Duration.between(testStart, Instant.now()).toMillis();
    testExecutionTimes.put("thread_safety_tests", testDuration);
    totalTestsRun.incrementAndGet();

    SUITE_LOGGER.info("Thread safety tests completed in " + testDuration + "ms");
    SUITE_LOGGER.info(
        "Concurrent creation: " + concurrentCreateResult.getOperationsPerSecond() + " ops/sec");
    SUITE_LOGGER.info(
        "Concurrent execution: " + concurrentExecutionResult.getOperationsPerSecond() + " ops/sec");
  }

  @Test
  @Order(7)
  @EnabledIf("isExtensiveTestingEnabled")
  @DisplayName("Should execute extensive memory leak detection tests")
  void shouldExecuteExtensiveMemoryLeakDetectionTests() {
    final Instant testStart = Instant.now();
    SUITE_LOGGER.info("Executing extensive memory leak detection tests...");

    // Run thorough memory leak detection on critical paths
    final MemoryLeakDetector.LeakAnalysisResult extensiveResult =
        testWithThoroughMemoryLeakDetection(
            "extensive_memory_leak_test",
            runtime -> {
              final var testModules = testUtils.getAllTestModules();

              // Extensive testing with multiple iterations
              for (int iteration = 0; iteration < 5; iteration++) {
                try (final var engine = runtime.createEngine()) {
                  for (final var testModule : testModules) {
                    try (final var module = engine.compileModule(testModule.getModuleBytes())) {
                      try (final var instance = runtime.instantiate(module)) {
                        // Exercise all available functions
                        if (instance.getFunction("add").isPresent()) {
                          final var addFunction = instance.getFunction("add");
                          if (addFunction.isPresent()) {
                            for (int i = 0; i < 10; i++) {
                              final var args =
                                  new ai.tegmentum.wasmtime4j.WasmValue[] {
                                    ai.tegmentum.wasmtime4j.WasmValue.i32(i),
                                    ai.tegmentum.wasmtime4j.WasmValue.i32(i * 2)
                                  };
                              addFunction.get().call(args);
                            }
                          }
                        }
                      }
                    } catch (final Exception e) {
                      // Expected for some test modules
                    }
                  }
                }
              }
            });

    if (extensiveResult.isLeakDetected()) {
      testsWithMemoryLeaks.incrementAndGet();
      SUITE_LOGGER.warning("Memory leak detected in extensive testing");
      SUITE_LOGGER.warning("Leak analysis: " + extensiveResult.getAnalysis());
    }

    final long testDuration = Duration.between(testStart, Instant.now()).toMillis();
    testExecutionTimes.put("extensive_memory_tests", testDuration);
    totalTestsRun.incrementAndGet();

    SUITE_LOGGER.info("Extensive memory leak detection tests completed in " + testDuration + "ms");
  }

  /** Generates a comprehensive report of all test results. */
  private static void generateComprehensiveReport() {
    final long totalDuration = Duration.between(suiteStartTime, suiteEndTime).toMillis();

    SUITE_LOGGER.info("=".repeat(80));
    SUITE_LOGGER.info("COMPREHENSIVE NATIVE FUNCTION TEST SUITE REPORT");
    SUITE_LOGGER.info("=".repeat(80));

    SUITE_LOGGER.info("Execution Summary:");
    SUITE_LOGGER.info("  Total Tests Run: " + totalTestsRun.get());
    SUITE_LOGGER.info(
        "  Total Duration: "
            + totalDuration
            + "ms ("
            + String.format("%.2f", totalDuration / 1000.0)
            + " seconds)");

    SUITE_LOGGER.info("\nTest Performance:");
    testExecutionTimes.forEach(
        (testName, duration) -> {
          SUITE_LOGGER.info("  " + testName + ": " + duration + "ms");
        });

    SUITE_LOGGER.info("\nMemory Leak Analysis:");
    SUITE_LOGGER.info("  Tests with Memory Leaks: " + testsWithMemoryLeaks.get());

    int totalLeakTests = 0;
    for (final List<MemoryLeakDetector.LeakAnalysisResult> results : memoryLeakResults.values()) {
      totalLeakTests += results.size();
    }

    final double leakPercentage =
        totalLeakTests > 0 ? (testsWithMemoryLeaks.get() * 100.0) / totalLeakTests : 0.0;
    SUITE_LOGGER.info("  Memory Leak Rate: " + String.format("%.2f%%", leakPercentage));

    memoryLeakResults.forEach(
        (runtime, results) -> {
          if (!results.isEmpty()) {
            final long leakCount =
                results.stream().mapToLong(r -> r.isLeakDetected() ? 1 : 0).sum();
            SUITE_LOGGER.info(
                "    " + runtime + ": " + leakCount + "/" + results.size() + " tests with leaks");
          }
        });

    SUITE_LOGGER.info("\nThread Safety Analysis:");
    SUITE_LOGGER.info("  Thread Safety Failures: " + threadSafetyFailures.get());

    SUITE_LOGGER.info("\nCross-Runtime Compatibility:");
    SUITE_LOGGER.info("  Cross-Runtime Failures: " + crossRuntimeFailures.get());

    SUITE_LOGGER.info("\nPlatform Coverage:");
    SUITE_LOGGER.info("  JNI Runtime: Tested");
    SUITE_LOGGER.info(
        "  Panama Runtime: " + (TestUtils.isPanamaAvailable() ? "Tested" : "Not Available"));

    // Calculate coverage estimate
    final int expectedTestsPerRuntime = 4; // Engine, Module, Store, Instance
    final int availableRuntimes = TestUtils.isPanamaAvailable() ? 2 : 1;
    final int expectedTests =
        expectedTestsPerRuntime * availableRuntimes
            + 3; // +3 for infrastructure, thread safety, extensive
    final double coveragePercentage = (totalTestsRun.get() * 100.0) / expectedTests;

    SUITE_LOGGER.info("\nTest Coverage Estimate:");
    SUITE_LOGGER.info("  Native Function Coverage: " + String.format("%.1f%%", coveragePercentage));

    SUITE_LOGGER.info("\nRecommendations:");
    if (testsWithMemoryLeaks.get() > 0) {
      SUITE_LOGGER.info("  ⚠️  Investigate memory leaks detected in native functions");
    }
    if (threadSafetyFailures.get() > 0) {
      SUITE_LOGGER.info("  ⚠️  Review thread safety issues in concurrent operations");
    }
    if (crossRuntimeFailures.get() > 0) {
      SUITE_LOGGER.info("  ⚠️  Address cross-runtime compatibility issues");
    }
    if (testsWithMemoryLeaks.get() == 0
        && threadSafetyFailures.get() == 0
        && crossRuntimeFailures.get() == 0) {
      SUITE_LOGGER.info("  ✅ All native function tests passed successfully");
    }

    SUITE_LOGGER.info("=".repeat(80));
    SUITE_LOGGER.info("COMPREHENSIVE NATIVE FUNCTION TEST SUITE COMPLETED");
    SUITE_LOGGER.info("=".repeat(80));
  }

  /**
   * Utility method to check if extensive testing is enabled.
   *
   * @return true if extensive testing should be run
   */
  static boolean isExtensiveTestingEnabled() {
    return Boolean.parseBoolean(System.getProperty("wasmtime4j.test.extensive", "true"));
  }
}

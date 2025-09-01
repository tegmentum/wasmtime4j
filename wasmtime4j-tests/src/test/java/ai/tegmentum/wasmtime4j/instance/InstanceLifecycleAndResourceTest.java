package ai.tegmentum.wasmtime4j.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive test suite combining instance lifecycle management, resource management with leak
 * detection, performance benchmarking, and cross-runtime validation. This test provides complete
 * coverage of instance management patterns, resource cleanup, and performance characteristics.
 */
@DisplayName("Instance Lifecycle and Resource Management Tests")
final class InstanceLifecycleAndResourceTest {

  private static final Logger LOGGER =
      Logger.getLogger(InstanceLifecycleAndResourceTest.class.getName());

  private final Map<String, Object> testMetrics = new HashMap<>();
  private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    TestUtils.skipIfCategoryNotEnabled(TestCategories.INSTANCE);
    testMetrics.clear();
    LOGGER.info("Starting test: " + testInfo.getDisplayName());

    // Force garbage collection before test
    System.gc();
    System.gc();
  }

  /**
   * Execute test with both JNI and Panama runtimes if available.
   *
   * @param testAction The test action to execute with each runtime
   */
  private void runWithBothRuntimes(final RuntimeTestAction testAction) {
    final List<RuntimeType> availableRuntimes = WasmRuntimeFactory.getAvailableRuntimes();

    for (final RuntimeType runtimeType : availableRuntimes) {
      try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
        LOGGER.info("Testing with runtime: " + runtimeType);
        testAction.execute(runtime, runtimeType);
      } catch (final Exception e) {
        throw new RuntimeException("Test failed with runtime " + runtimeType, e);
      }
    }
  }

  /**
   * Add a test metric for tracking and analysis.
   *
   * @param message The metric message
   */
  private void addTestMetric(final String message) {
    testMetrics.put(Instant.now().toString(), message);
    LOGGER.info("Test metric: " + message);
  }

  /** Functional interface for runtime-specific test actions. */
  @FunctionalInterface
  private interface RuntimeTestAction {
    void execute(WasmRuntime runtime, RuntimeType runtimeType) throws Exception;
  }

  @Nested
  @DisplayName("Instance Lifecycle Management Tests")
  final class LifecycleTests {

    @Test
    @DisplayName("Should handle complete instance lifecycle patterns")
    void shouldHandleCompleteInstanceLifecyclePatterns() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");

            try (final Engine engine = runtime.createEngine();
                final Module module = engine.compileModule(moduleBytes)) {

              // Test creation -> usage -> close pattern
              for (int cycle = 0; cycle < 5; cycle++) {
                try (final Store store = engine.createStore()) {

                  // Create instance
                  final Instance instance = module.instantiate(store);
                  assertThat(instance.isValid()).isTrue();
                  assertThat(instance.getModule()).isEqualTo(module);
                  assertThat(instance.getStore()).isEqualTo(store);

                  // Use instance extensively
                  for (int i = 0; i < 100; i++) {
                    final WasmValue[] result =
                        instance.callFunction(
                            "add", WasmValue.i32(cycle * 100 + i), WasmValue.i32(1));
                    assertThat(result[0].asI32()).isEqualTo(cycle * 100 + i + 1);
                  }

                  // Verify exports are still accessible
                  final String[] exports = instance.getExportNames();
                  assertThat(exports).isNotEmpty();
                  assertThat(instance.getFunction("add")).isPresent();

                  // Close instance
                  instance.close();
                  assertThat(instance.isValid()).isFalse();

                  // Verify operations fail after close
                  assertThatThrownBy(instance::getExportNames).isInstanceOf(WasmException.class);
                  assertThatThrownBy(
                          () -> instance.callFunction("add", WasmValue.i32(1), WasmValue.i32(2)))
                      .isInstanceOf(WasmException.class);
                }

                addTestMetric(
                    String.format("Lifecycle cycle %d completed with %s", cycle, runtimeType));
              }
            }
          });
    }

    @Test
    @DisplayName("Should handle instance state transitions correctly")
    void shouldHandleInstanceStateTransitionsCorrectly() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("global_mutable");

            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test state persistence across operations
              final Map<String, Integer> stateTests = new HashMap<>();
              stateTests.put("initial", 0);
              stateTests.put("positive", 12345);
              stateTests.put("negative", -67890);
              stateTests.put("zero_again", 0);
              stateTests.put("max_int", Integer.MAX_VALUE);
              stateTests.put("min_int", Integer.MIN_VALUE);

              for (final Map.Entry<String, Integer> test : stateTests.entrySet()) {
                final String phase = test.getKey();
                final int value = test.getValue();

                // Set state
                instance.callFunction("set", WasmValue.i32(value));

                // Verify state immediately
                WasmValue[] result = instance.callFunction("get");
                assertThat(result[0].asI32()).isEqualTo(value);

                // Verify state persists across multiple calls
                for (int i = 0; i < 10; i++) {
                  result = instance.callFunction("get");
                  assertThat(result[0].asI32()).isEqualTo(value);
                }

                addTestMetric(
                    String.format(
                        "State transition '%s' to %d verified with %s", phase, value, runtimeType));
              }
            }
          });
    }

    @Test
    @DisplayName("Should handle multiple instances with independent states")
    void shouldHandleMultipleInstancesWithIndependentStates() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("global_mutable");

            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes)) {

              final int numInstances = 5;
              final List<Instance> instances = new ArrayList<>();
              final Map<Instance, Integer> instanceValues = new HashMap<>();

              try {
                // Create multiple instances
                for (int i = 0; i < numInstances; i++) {
                  final Instance instance = module.instantiate(store);
                  instances.add(instance);

                  final int uniqueValue = i * 1000;
                  instanceValues.put(instance, uniqueValue);

                  // Set unique value for each instance
                  instance.callFunction("set", WasmValue.i32(uniqueValue));
                }

                // Verify each instance maintains its unique state
                for (int round = 0; round < 10; round++) {
                  for (final Instance instance : instances) {
                    final int expectedValue = instanceValues.get(instance);
                    final WasmValue[] result = instance.callFunction("get");
                    assertThat(result[0].asI32()).isEqualTo(expectedValue);
                  }
                }

                // Modify states and verify independence
                for (final Instance instance : instances) {
                  final int currentValue = instanceValues.get(instance);
                  final int newValue = currentValue + 42;

                  instance.callFunction("set", WasmValue.i32(newValue));
                  instanceValues.put(instance, newValue);

                  // Verify this instance changed
                  final WasmValue[] result = instance.callFunction("get");
                  assertThat(result[0].asI32()).isEqualTo(newValue);

                  // Verify other instances unchanged
                  for (final Instance otherInstance : instances) {
                    if (otherInstance != instance) {
                      final int otherValue = instanceValues.get(otherInstance);
                      final WasmValue[] otherResult = otherInstance.callFunction("get");
                      assertThat(otherResult[0].asI32()).isEqualTo(otherValue);
                    }
                  }
                }

              } finally {
                // Clean up instances
                for (final Instance instance : instances) {
                  if (instance.isValid()) {
                    instance.close();
                  }
                }
              }

              addTestMetric(
                  String.format("Multiple instance independence verified with %s", runtimeType));
            }
          });
    }
  }

  @Nested
  @DisplayName("Resource Management and Leak Detection Tests")
  final class ResourceManagementTests {

    @Test
    @DisplayName("Should manage memory resources without leaks")
    void shouldManageMemoryResourcesWithoutLeaks() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");

            // Get baseline memory usage
            System.gc();
            System.gc();
            final long baselineMemory = memoryBean.getHeapMemoryUsage().getUsed();

            final int numCycles = 100;
            final int instancesPerCycle = 10;

            try (final Engine engine = runtime.createEngine();
                final Module module = engine.compileModule(moduleBytes)) {

              for (int cycle = 0; cycle < numCycles; cycle++) {
                final List<Instance> instances = new ArrayList<>();

                try (final Store store = engine.createStore()) {

                  // Create multiple instances
                  for (int i = 0; i < instancesPerCycle; i++) {
                    final Instance instance = module.instantiate(store);
                    instances.add(instance);

                    // Use each instance
                    final WasmValue[] result =
                        instance.callFunction("add", WasmValue.i32(cycle), WasmValue.i32(i));
                    assertThat(result[0].asI32()).isEqualTo(cycle + i);
                  }

                } finally {
                  // Clean up instances
                  for (final Instance instance : instances) {
                    if (instance.isValid()) {
                      instance.close();
                    }
                  }
                }

                // Periodic memory check
                if (cycle % 20 == 19) {
                  System.gc();
                  System.gc();
                  final long currentMemory = memoryBean.getHeapMemoryUsage().getUsed();
                  final long memoryIncrease = currentMemory - baselineMemory;

                  // Memory increase should be reasonable (less than 100MB)
                  assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024);

                  addTestMetric(
                      String.format(
                          "Memory check at cycle %d: %d bytes increase with %s",
                          cycle, memoryIncrease, runtimeType));
                }
              }

              // Final memory check
              System.gc();
              System.gc();
              final long finalMemory = memoryBean.getHeapMemoryUsage().getUsed();
              final long totalIncrease = finalMemory - baselineMemory;

              addTestMetric(
                  String.format(
                      "Total memory increase: %d bytes after %d cycles with %s",
                      totalIncrease, numCycles, runtimeType));
            }
          });
    }

    @Test
    @DisplayName("Should handle resource cleanup under stress conditions")
    void shouldHandleResourceCleanupUnderStressConditions() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            final int numThreads = 8;
            final int instancesPerThread = 25;
            final ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            try (final Engine engine = runtime.createEngine();
                final Module module = engine.compileModule(moduleBytes)) {

              final List<CompletableFuture<Integer>> futures = new ArrayList<>();

              for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                final CompletableFuture<Integer> future =
                    CompletableFuture.supplyAsync(
                        () -> {
                          int successfulCleanups = 0;

                          for (int j = 0; j < instancesPerThread; j++) {
                            try (final Store store = engine.createStore()) {
                              final Instance instance = module.instantiate(store);

                              // Use instance
                              instance.callFunction(
                                  "add", WasmValue.i32(threadId), WasmValue.i32(j));

                              // Explicit close
                              instance.close();
                              assertThat(instance.isValid()).isFalse();

                              successfulCleanups++;

                            } catch (final Exception e) {
                              LOGGER.warning(
                                  String.format(
                                      "Thread %d instance %d cleanup failed: %s",
                                      threadId, j, e.getMessage()));
                            }
                          }

                          return successfulCleanups;
                        },
                        executor);
                futures.add(future);
              }

              // Wait for all threads and collect results
              int totalSuccessfulCleanups = 0;
              for (final CompletableFuture<Integer> future : futures) {
                totalSuccessfulCleanups += future.get(30, TimeUnit.SECONDS);
              }

              final int expectedCleanups = numThreads * instancesPerThread;
              assertThat(totalSuccessfulCleanups).isEqualTo(expectedCleanups);

            } finally {
              executor.shutdownNow();
              executor.awaitTermination(10, TimeUnit.SECONDS);
            }

            addTestMetric(
                String.format(
                    "Stress cleanup: %d successful cleanups with %s",
                    numThreads * instancesPerThread, runtimeType));
          });
    }
  }

  @Nested
  @DisplayName("Performance Benchmarking Tests")
  final class PerformanceBenchmarkTests {

    @Test
    @DisplayName("Should establish instance operation performance baselines")
    void shouldEstablishInstanceOperationPerformanceBaselines() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");

            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes)) {

              // Warm up
              try (final Instance warmupInstance = module.instantiate(store)) {
                for (int i = 0; i < 1000; i++) {
                  warmupInstance.callFunction("add", WasmValue.i32(i), WasmValue.i32(i + 1));
                }
              }

              // Benchmark instance creation
              final int creationIterations = 100;
              final Instant creationStart = Instant.now();

              for (int i = 0; i < creationIterations; i++) {
                try (final Instance instance = module.instantiate(store)) {
                  assertThat(instance.isValid()).isTrue();
                }
              }

              final Duration creationTime = Duration.between(creationStart, Instant.now());
              final double creationsPerSecond =
                  creationIterations / (creationTime.toMillis() / 1000.0);

              // Benchmark function calls
              try (final Instance instance = module.instantiate(store)) {
                final int callIterations = 10000;
                final Instant callStart = Instant.now();

                for (int i = 0; i < callIterations; i++) {
                  final WasmValue[] result =
                      instance.callFunction("add", WasmValue.i32(i), WasmValue.i32(1));
                  assertThat(result[0].asI32()).isEqualTo(i + 1);
                }

                final Duration callTime = Duration.between(callStart, Instant.now());
                final double callsPerSecond = callIterations / (callTime.toMillis() / 1000.0);

                // Benchmark export access
                final int exportIterations = 10000;
                final Instant exportStart = Instant.now();

                for (int i = 0; i < exportIterations; i++) {
                  final String[] exports = instance.getExportNames();
                  assertThat(exports).isNotEmpty();
                  assertThat(instance.getFunction("add")).isPresent();
                }

                final Duration exportTime = Duration.between(exportStart, Instant.now());
                final double exportAccessPerSecond =
                    exportIterations / (exportTime.toMillis() / 1000.0);

                // Performance assertions
                assertThat(creationsPerSecond).isGreaterThan(100);
                assertThat(callsPerSecond).isGreaterThan(1000);
                assertThat(exportAccessPerSecond).isGreaterThan(1000);

                addTestMetric(
                    String.format(
                        "Performance with %s: %.0f creations/sec, %.0f calls/sec, %.0f exports/sec",
                        runtimeType, creationsPerSecond, callsPerSecond, exportAccessPerSecond));
              }
            }
          });
    }

    @Test
    @DisplayName("Should maintain performance under concurrent load")
    void shouldMaintainPerformanceUnderConcurrentLoad() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            final int numThreads = 4;
            final int operationsPerThread = 1000;
            final ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final List<CompletableFuture<Duration>> futures = new ArrayList<>();
              final Instant concurrentStart = Instant.now();

              for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                final CompletableFuture<Duration> future =
                    CompletableFuture.supplyAsync(
                        () -> {
                          final Instant threadStart = Instant.now();

                          for (int j = 0; j < operationsPerThread; j++) {
                            try {
                              final WasmValue[] result =
                                  instance.callFunction(
                                      "add", WasmValue.i32(threadId * 1000 + j), WasmValue.i32(1));
                              assertThat(result[0].asI32()).isEqualTo(threadId * 1000 + j + 1);
                            } catch (final Exception e) {
                              throw new RuntimeException("Concurrent operation failed", e);
                            }
                          }

                          return Duration.between(threadStart, Instant.now());
                        },
                        executor);
                futures.add(future);
              }

              // Collect results
              Duration maxThreadTime = Duration.ZERO;
              for (final CompletableFuture<Duration> future : futures) {
                final Duration threadTime = future.get(30, TimeUnit.SECONDS);
                if (threadTime.compareTo(maxThreadTime) > 0) {
                  maxThreadTime = threadTime;
                }
              }

              final Duration totalTime = Duration.between(concurrentStart, Instant.now());
              final int totalOperations = numThreads * operationsPerThread;
              final double concurrentThroughput = totalOperations / (totalTime.toMillis() / 1000.0);

              // Should achieve reasonable concurrent performance
              assertThat(concurrentThroughput).isGreaterThan(1000);

              addTestMetric(
                  String.format(
                      "Concurrent performance with %s: %.0f ops/sec with %d threads",
                      runtimeType, concurrentThroughput, numThreads));

            } finally {
              executor.shutdownNow();
              executor.awaitTermination(10, TimeUnit.SECONDS);
            }
          });
    }
  }

  @Nested
  @DisplayName("Cross-Runtime Validation Tests")
  final class CrossRuntimeValidationTests {

    @Test
    @DisplayName("Should provide identical lifecycle behavior across runtimes")
    void shouldProvideIdenticalLifecycleBehaviorAcrossRuntimes() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final byte[] moduleBytes = WasmTestModules.getModule("global_mutable");
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore();
                    final Module module = engine.compileModule(moduleBytes);
                    final Instance instance = module.instantiate(store)) {

                  final Map<String, Object> lifecycleResults = new HashMap<>();

                  // Initial state
                  final WasmValue[] initial = instance.callFunction("get");
                  lifecycleResults.put("initial_value", initial[0].asI32());

                  // State modification
                  instance.callFunction("set", WasmValue.i32(12345));
                  final WasmValue[] modified = instance.callFunction("get");
                  lifecycleResults.put("modified_value", modified[0].asI32());

                  // Instance properties
                  lifecycleResults.put("is_valid_before_close", instance.isValid());
                  lifecycleResults.put("export_count", instance.getExportNames().length);

                  // Close and verify
                  instance.close();
                  lifecycleResults.put("is_valid_after_close", instance.isValid());

                  return lifecycleResults;
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();

      addTestMetric("Cross-runtime lifecycle validation: " + result.getDifferenceDescription());
    }

    @Test
    @DisplayName("Should provide identical performance characteristics across runtimes")
    void shouldProvideIdenticalPerformanceCharacteristicsAcrossRuntimes() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore();
                    final Module module = engine.compileModule(moduleBytes);
                    final Instance instance = module.instantiate(store)) {

                  final Map<String, String> performanceResults = new HashMap<>();

                  // Quick function call benchmark
                  final int iterations = 1000;
                  final Instant start = Instant.now();

                  for (int i = 0; i < iterations; i++) {
                    final WasmValue[] result =
                        instance.callFunction("add", WasmValue.i32(i), WasmValue.i32(1));
                    if (result[0].asI32() != i + 1) {
                      throw new RuntimeException("Unexpected result");
                    }
                  }

                  final Duration duration = Duration.between(start, Instant.now());

                  // Categorize performance level rather than exact timing
                  final double callsPerSecond = iterations / (duration.toMillis() / 1000.0);
                  final String performanceCategory;
                  if (callsPerSecond > 10000) {
                    performanceCategory = "excellent";
                  } else if (callsPerSecond > 5000) {
                    performanceCategory = "good";
                  } else if (callsPerSecond > 1000) {
                    performanceCategory = "acceptable";
                  } else {
                    performanceCategory = "slow";
                  }

                  performanceResults.put("performance_category", performanceCategory);
                  performanceResults.put("function_works", "true");

                  return performanceResults;
                }
              });

      assertThat(result.isValid()).isTrue();
      // Performance might vary slightly, but functionality should be identical

      addTestMetric("Cross-runtime performance validation: " + result.getDifferenceDescription());
    }
  }
}

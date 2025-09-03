package ai.tegmentum.wasmtime4j.memory;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Resource leak detection tests for Engine and Store APIs. These tests verify that resources are
 * properly cleaned up and no memory leaks occur during normal operations and error scenarios.
 */
@DisplayName("Engine & Store Resource Leak Detection Tests")
final class EngineStoreLeakDetectionIT extends BaseIntegrationTest {

  private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();
  private static final long MEMORY_THRESHOLD_MB = 100; // MB
  private static final int STRESS_ITERATIONS = 50;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // skipIfCategoryNotEnabled(TestCategories.MEMORY);
  }

  @Nested
  @DisplayName("Engine Resource Leak Tests")
  final class EngineResourceLeakTests {

    @Test
    @DisplayName("Should not leak memory during repeated engine creation and closure")
    void shouldNotLeakMemoryDuringRepeatedEngineCreationAndClosure() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing engine creation/closure memory leaks on " + runtimeType);

            final MemoryUsage initialMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long initialUsedMB = initialMemory.getUsed() / (1024 * 1024);

            // Perform repeated engine creation and closure
            measureExecutionTime(
                "Engine creation/closure stress test on " + runtimeType,
                () -> {
                  for (int i = 0; i < STRESS_ITERATIONS; i++) {
                    try {
                      final Engine engine = runtime.createEngine();
                      assertThat(engine.isValid()).isTrue();
                      engine.close();
                      assertThat(engine.isValid()).isFalse();
                    } catch (final Exception e) {
                      throw new RuntimeException("Failed at iteration " + i, e);
                    }

                    // Force garbage collection periodically
                    if (i % 10 == 0) {
                      System.gc();
                      Thread.yield();
                    }
                  }
                });

            // Force final garbage collection
            System.gc();
            Thread.sleep(1000); // Give GC time to work
            System.gc();

            final MemoryUsage finalMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long finalUsedMB = finalMemory.getUsed() / (1024 * 1024);
            final long memoryIncreaseMB = finalUsedMB - initialUsedMB;

            LOGGER.info(
                String.format(
                    "Memory usage on %s: Initial=%dMB, Final=%dMB, Increase=%dMB",
                    runtimeType, initialUsedMB, finalUsedMB, memoryIncreaseMB));

            assertThat(memoryIncreaseMB)
                .as("Memory leak detected during engine creation/closure on " + runtimeType)
                .isLessThan(MEMORY_THRESHOLD_MB);

            // addTestMetric(
            //     "Memory increase after "
            //         + STRESS_ITERATIONS
            //         + " engine cycles: "
            //         + memoryIncreaseMB
            //         + "MB on "
            //         + runtimeType);
          });
    }

    @Test
    @DisplayName("Should not leak memory with various engine configurations")
    void shouldNotLeakMemoryWithVariousEngineConfigurations() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing engine configuration memory leaks on " + runtimeType);

            final MemoryUsage initialMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long initialUsedMB = initialMemory.getUsed() / (1024 * 1024);

            // Test different configurations
            final EngineConfig[] configs = {
              new EngineConfig(), // Default
              EngineConfig.forSpeed(),
              EngineConfig.forSize(),
              EngineConfig.forDebug(),
              new EngineConfig()
                  .debugInfo(true)
                  .consumeFuel(true)
                  .optimizationLevel(OptimizationLevel.SPEED_AND_SIZE)
            };

            measureExecutionTime(
                "Engine configuration memory test on " + runtimeType,
                () -> {
                  for (int i = 0; i < STRESS_ITERATIONS; i++) {
                    final EngineConfig config = configs[i % configs.length];
                    try {
                      final Engine engine = runtime.createEngine(config);
                      assertThat(engine.isValid()).isTrue();
                      assertThat(engine.getConfig()).isNotNull();
                      engine.close();
                    } catch (final Exception e) {
                      throw new RuntimeException(
                          "Failed with config " + config + " at iteration " + i, e);
                    }

                    if (i % 10 == 0) {
                      System.gc();
                      Thread.yield();
                    }
                  }
                });

            System.gc();
            Thread.sleep(1000);
            System.gc();

            final MemoryUsage finalMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long finalUsedMB = finalMemory.getUsed() / (1024 * 1024);
            final long memoryIncreaseMB = finalUsedMB - initialUsedMB;

            LOGGER.info(
                String.format(
                    "Configuration memory usage on %s: Initial=%dMB, Final=%dMB, Increase=%dMB",
                    runtimeType, initialUsedMB, finalUsedMB, memoryIncreaseMB));

            assertThat(memoryIncreaseMB)
                .as("Memory leak detected with engine configurations on " + runtimeType)
                .isLessThan(MEMORY_THRESHOLD_MB);

            // addTestMetric(
            //     "Memory increase after "
            //         + STRESS_ITERATIONS
            //         + " config cycles: "
            //         + memoryIncreaseMB
            //         + "MB on "
            //         + runtimeType);
          });
    }

    @Test
    @DisplayName("Should not leak memory during module compilation")
    void shouldNotLeakMemoryDuringModuleCompilation() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing module compilation memory leaks on " + runtimeType);

            final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
            final MemoryUsage initialMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long initialUsedMB = initialMemory.getUsed() / (1024 * 1024);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            measureExecutionTime(
                "Module compilation memory test on " + runtimeType,
                () -> {
                  for (int i = 0; i < STRESS_ITERATIONS; i++) {
                    try {
                      final Module module = engine.compileModule(wasmBytes);
                      assertThat(module.isValid()).isTrue();
                      module.close();
                      assertThat(module.isValid()).isFalse();
                    } catch (final Exception e) {
                      throw new RuntimeException("Failed at iteration " + i, e);
                    }

                    if (i % 10 == 0) {
                      System.gc();
                      Thread.yield();
                    }
                  }
                });

            System.gc();
            Thread.sleep(1000);
            System.gc();

            final MemoryUsage finalMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long finalUsedMB = finalMemory.getUsed() / (1024 * 1024);
            final long memoryIncreaseMB = finalUsedMB - initialUsedMB;

            LOGGER.info(
                String.format(
                    "Module compilation memory usage on %s: Initial=%dMB, Final=%dMB,"
                        + " Increase=%dMB",
                    runtimeType, initialUsedMB, finalUsedMB, memoryIncreaseMB));

            assertThat(memoryIncreaseMB)
                .as("Memory leak detected during module compilation on " + runtimeType)
                .isLessThan(MEMORY_THRESHOLD_MB);

            // addTestMetric(
            //     "Memory increase after "
            //         + STRESS_ITERATIONS
            //         + " compilations: "
            //         + memoryIncreaseMB
            //         + "MB on "
            //         + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Store Resource Leak Tests")
  final class StoreResourceLeakTests {

    @Test
    @DisplayName("Should not leak memory during repeated store creation and closure")
    void shouldNotLeakMemoryDuringRepeatedStoreCreationAndClosure() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store creation/closure memory leaks on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final MemoryUsage initialMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long initialUsedMB = initialMemory.getUsed() / (1024 * 1024);

            measureExecutionTime(
                "Store creation/closure stress test on " + runtimeType,
                () -> {
                  for (int i = 0; i < STRESS_ITERATIONS; i++) {
                    try {
                      final Store store = engine.createStore();
                      assertThat(store.isValid()).isTrue();
                      store.close();
                      assertThat(store.isValid()).isFalse();
                    } catch (final Exception e) {
                      throw new RuntimeException("Failed at iteration " + i, e);
                    }

                    if (i % 10 == 0) {
                      System.gc();
                      Thread.yield();
                    }
                  }
                });

            System.gc();
            Thread.sleep(1000);
            System.gc();

            final MemoryUsage finalMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long finalUsedMB = finalMemory.getUsed() / (1024 * 1024);
            final long memoryIncreaseMB = finalUsedMB - initialUsedMB;

            LOGGER.info(
                String.format(
                    "Store memory usage on %s: Initial=%dMB, Final=%dMB, Increase=%dMB",
                    runtimeType, initialUsedMB, finalUsedMB, memoryIncreaseMB));

            assertThat(memoryIncreaseMB)
                .as("Memory leak detected during store creation/closure on " + runtimeType)
                .isLessThan(MEMORY_THRESHOLD_MB);

            // addTestMetric(
            //     "Memory increase after "
            //         + STRESS_ITERATIONS
            //         + " store cycles: "
            //         + memoryIncreaseMB
            //         + "MB on "
            //         + runtimeType);
          });
    }

    @Test
    @DisplayName("Should not leak memory with store data management")
    void shouldNotLeakMemoryWithStoreDataManagement() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store data management memory leaks on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final MemoryUsage initialMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long initialUsedMB = initialMemory.getUsed() / (1024 * 1024);

            measureExecutionTime(
                "Store data management memory test on " + runtimeType,
                () -> {
                  for (int i = 0; i < STRESS_ITERATIONS; i++) {
                    try {
                      // Set various data types
                      store.setData("string-data-" + i);
                      store.getData();

                      store.setData(i);
                      store.getData();

                      store.setData(List.of("item-" + i, "item-" + (i + 1)));
                      store.getData();

                      store.setData(null);
                      store.getData();
                    } catch (final Exception e) {
                      throw new RuntimeException("Failed at iteration " + i, e);
                    }

                    if (i % 10 == 0) {
                      System.gc();
                      Thread.yield();
                    }
                  }
                });

            System.gc();
            Thread.sleep(1000);
            System.gc();

            final MemoryUsage finalMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long finalUsedMB = finalMemory.getUsed() / (1024 * 1024);
            final long memoryIncreaseMB = finalUsedMB - initialUsedMB;

            LOGGER.info(
                String.format(
                    "Store data memory usage on %s: Initial=%dMB, Final=%dMB, Increase=%dMB",
                    runtimeType, initialUsedMB, finalUsedMB, memoryIncreaseMB));

            assertThat(memoryIncreaseMB)
                .as("Memory leak detected during store data management on " + runtimeType)
                .isLessThan(MEMORY_THRESHOLD_MB);

            // addTestMetric(
            //     "Memory increase after "
            //         + STRESS_ITERATIONS
            //         + " data operations: "
            //         + memoryIncreaseMB
            //         + "MB on "
            //         + runtimeType);
          });
    }

    @Test
    @DisplayName("Should not leak memory with store fuel management")
    void shouldNotLeakMemoryWithStoreFuelManagement() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store fuel management memory leaks on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final MemoryUsage initialMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long initialUsedMB = initialMemory.getUsed() / (1024 * 1024);

            measureExecutionTime(
                "Store fuel management memory test on " + runtimeType,
                () -> {
                  for (int i = 0; i < STRESS_ITERATIONS; i++) {
                    try {
                      store.setFuel(1000L + i);
                      store.getFuel();

                      store.addFuel(100L);
                      store.getFuel();

                      store.setFuel(0L);
                      store.getFuel();
                    } catch (final Exception e) {
                      throw new RuntimeException("Failed at iteration " + i, e);
                    }

                    if (i % 10 == 0) {
                      System.gc();
                      Thread.yield();
                    }
                  }
                });

            System.gc();
            Thread.sleep(1000);
            System.gc();

            final MemoryUsage finalMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long finalUsedMB = finalMemory.getUsed() / (1024 * 1024);
            final long memoryIncreaseMB = finalUsedMB - initialUsedMB;

            LOGGER.info(
                String.format(
                    "Store fuel memory usage on %s: Initial=%dMB, Final=%dMB, Increase=%dMB",
                    runtimeType, initialUsedMB, finalUsedMB, memoryIncreaseMB));

            assertThat(memoryIncreaseMB)
                .as("Memory leak detected during store fuel management on " + runtimeType)
                .isLessThan(MEMORY_THRESHOLD_MB);

            // addTestMetric(
            //     "Memory increase after "
            //         + STRESS_ITERATIONS
            //         + " fuel operations: "
            //         + memoryIncreaseMB
            //         + "MB on "
            //         + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Concurrent Resource Leak Tests")
  final class ConcurrentResourceLeakTests {

    @Test
    @DisplayName("Should not leak memory during concurrent engine operations")
    void shouldNotLeakMemoryDuringConcurrentEngineOperations() throws Exception {
      // skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing concurrent engine operations memory leaks on " + runtimeType);

            final MemoryUsage initialMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long initialUsedMB = initialMemory.getUsed() / (1024 * 1024);

            final int threadCount = 4;
            final int operationsPerThread = 10;
            final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            final CountDownLatch latch = new CountDownLatch(threadCount * operationsPerThread);

            try {
              final List<CompletableFuture<Void>> futures = new ArrayList<>();

              measureExecutionTime(
                  "Concurrent engine operations memory test on " + runtimeType,
                  () -> {
                    for (int t = 0; t < threadCount; t++) {
                      for (int op = 0; op < operationsPerThread; op++) {
                        final CompletableFuture<Void> future =
                            CompletableFuture.runAsync(
                                () -> {
                                  try {
                                    final Engine engine = runtime.createEngine();
                                    assertThat(engine.isValid()).isTrue();

                                    final Store store = engine.createStore();
                                    assertThat(store.isValid()).isTrue();

                                    final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
                                    final Module module = engine.compileModule(wasmBytes);
                                    assertThat(module.isValid()).isTrue();

                                    module.close();
                                    store.close();
                                    engine.close();

                                    latch.countDown();
                                  } catch (final Exception e) {
                                    latch.countDown();
                                    throw new RuntimeException(e);
                                  }
                                },
                                executor);
                        futures.add(future);
                      }
                    }

                    try {
                      assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();

                      // Wait for all futures to complete
                      for (final CompletableFuture<Void> future : futures) {
                        future.get();
                      }
                    } catch (final Exception e) {
                      throw new RuntimeException(e);
                    }
                  });

              System.gc();
              Thread.sleep(2000); // Give more time for concurrent cleanup
              System.gc();

              final MemoryUsage finalMemory = MEMORY_BEAN.getHeapMemoryUsage();
              final long finalUsedMB = finalMemory.getUsed() / (1024 * 1024);
              final long memoryIncreaseMB = finalUsedMB - initialUsedMB;

              LOGGER.info(
                  String.format(
                      "Concurrent memory usage on %s: Initial=%dMB, Final=%dMB, Increase=%dMB",
                      runtimeType, initialUsedMB, finalUsedMB, memoryIncreaseMB));

              assertThat(memoryIncreaseMB)
                  .as("Memory leak detected during concurrent operations on " + runtimeType)
                  .isLessThan(
                      MEMORY_THRESHOLD_MB * 2); // Allow more tolerance for concurrent operations

              // addTestMetric(
              //     "Memory increase after "
              //         + (threadCount * operationsPerThread)
              //         + " concurrent operations: "
              //         + memoryIncreaseMB
              //         + "MB on "
              //         + runtimeType);
            } finally {
              executor.shutdown();
              if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
              }
            }
          });
    }
  }

  @Nested
  @DisplayName("Error Scenario Resource Leak Tests")
  final class ErrorScenarioResourceLeakTests {

    @Test
    @DisplayName("Should not leak memory during compilation errors")
    void shouldNotLeakMemoryDuringCompilationErrors() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing compilation error memory leaks on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final MemoryUsage initialMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long initialUsedMB = initialMemory.getUsed() / (1024 * 1024);

            final byte[] invalidWasm = "invalid wasm".getBytes();

            measureExecutionTime(
                "Compilation error memory test on " + runtimeType,
                () -> {
                  for (int i = 0; i < STRESS_ITERATIONS; i++) {
                    try {
                      engine.compileModule(invalidWasm);
                      throw new AssertionError("Expected compilation to fail");
                    } catch (final Exception e) {
                      // Expected failure - just continue
                    }

                    if (i % 10 == 0) {
                      System.gc();
                      Thread.yield();
                    }
                  }
                });

            System.gc();
            Thread.sleep(1000);
            System.gc();

            final MemoryUsage finalMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long finalUsedMB = finalMemory.getUsed() / (1024 * 1024);
            final long memoryIncreaseMB = finalUsedMB - initialUsedMB;

            LOGGER.info(
                String.format(
                    "Compilation error memory usage on %s: Initial=%dMB, Final=%dMB, Increase=%dMB",
                    runtimeType, initialUsedMB, finalUsedMB, memoryIncreaseMB));

            assertThat(memoryIncreaseMB)
                .as("Memory leak detected during compilation errors on " + runtimeType)
                .isLessThan(MEMORY_THRESHOLD_MB);

            // addTestMetric(
            //     "Memory increase after "
            //         + STRESS_ITERATIONS
            //         + " compilation errors: "
            //         + memoryIncreaseMB
            //         + "MB on "
            //         + runtimeType);
          });
    }

    @Test
    @DisplayName("Should not leak memory with improper resource handling")
    void shouldNotLeakMemoryWithImproperResourceHandling() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing improper resource handling memory leaks on " + runtimeType);

            final MemoryUsage initialMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long initialUsedMB = initialMemory.getUsed() / (1024 * 1024);

            measureExecutionTime(
                "Improper resource handling memory test on " + runtimeType,
                () -> {
                  for (int i = 0;
                      i < STRESS_ITERATIONS / 2;
                      i++) { // Fewer iterations due to potential resource buildup
                    try {
                      // Create resources but don't explicitly close them
                      // The garbage collector and finalizers should clean them up
                      final Engine engine = runtime.createEngine();
                      final Store store = engine.createStore();

                      // Use the resources
                      assertThat(engine.isValid()).isTrue();
                      assertThat(store.isValid()).isTrue();
                      store.setData("test-data-" + i);

                      // Let them go out of scope without explicit cleanup
                      // (In real code, you should always close resources explicitly)
                    } catch (final Exception e) {
                      throw new RuntimeException("Failed at iteration " + i, e);
                    }

                    // Force GC more frequently due to potential resource buildup
                    if (i % 5 == 0) {
                      System.gc();
                      try {
                        Thread.sleep(100);
                      } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during GC wait", e);
                      }
                      System.gc();
                    }
                  }
                });

            // Give finalizers time to run
            System.gc();
            Thread.sleep(2000);
            System.gc();
            Thread.sleep(1000);
            System.gc();

            final MemoryUsage finalMemory = MEMORY_BEAN.getHeapMemoryUsage();
            final long finalUsedMB = finalMemory.getUsed() / (1024 * 1024);
            final long memoryIncreaseMB = finalUsedMB - initialUsedMB;

            LOGGER.info(
                String.format(
                    "Improper resource handling memory usage on %s: Initial=%dMB, Final=%dMB,"
                        + " Increase=%dMB",
                    runtimeType, initialUsedMB, finalUsedMB, memoryIncreaseMB));

            // Allow higher threshold due to potential finalizer delays
            assertThat(memoryIncreaseMB)
                .as("Excessive memory usage with improper resource handling on " + runtimeType)
                .isLessThan(MEMORY_THRESHOLD_MB * 3);

            // addTestMetric(
            //     "Memory increase after "
            //         + (STRESS_ITERATIONS / 2)
            //         + " improper resource cycles: "
            //         + memoryIncreaseMB
            //         + "MB on "
            //         + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Memory Usage Profiling Tests")
  final class MemoryUsageProfilingTests {

    @Test
    @DisplayName("Should profile memory usage patterns")
    void shouldProfileMemoryUsagePatterns() throws Exception {
      // skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Profiling memory usage patterns on " + runtimeType);

            final List<Long> memorySnapshots = new ArrayList<>();
            final MemoryUsage initialMemory = MEMORY_BEAN.getHeapMemoryUsage();
            memorySnapshots.add(initialMemory.getUsed());

            // Profile different operations
            measureExecutionTime(
                "Memory profiling on " + runtimeType,
                () -> {
                  try {
                    // Engine creation
                    final Engine engine = runtime.createEngine();
                    memorySnapshots.add(MEMORY_BEAN.getHeapMemoryUsage().getUsed());

                    // Store creation
                    final Store store = engine.createStore();
                    memorySnapshots.add(MEMORY_BEAN.getHeapMemoryUsage().getUsed());

                    // Module compilation
                    final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
                    final Module module = engine.compileModule(wasmBytes);
                    memorySnapshots.add(MEMORY_BEAN.getHeapMemoryUsage().getUsed());

                    // Store operations
                    store.setData("profiling-test-data");
                    if (engine.getConfig().isConsumeFuel()) {
                      store.setFuel(1000L);
                    }
                    memorySnapshots.add(MEMORY_BEAN.getHeapMemoryUsage().getUsed());

                    // Cleanup
                    module.close();
                    memorySnapshots.add(MEMORY_BEAN.getHeapMemoryUsage().getUsed());

                    store.close();
                    memorySnapshots.add(MEMORY_BEAN.getHeapMemoryUsage().getUsed());

                    engine.close();
                    memorySnapshots.add(MEMORY_BEAN.getHeapMemoryUsage().getUsed());

                    // Force GC
                    System.gc();
                    Thread.sleep(500);
                    System.gc();
                    memorySnapshots.add(MEMORY_BEAN.getHeapMemoryUsage().getUsed());

                  } catch (final Exception e) {
                    throw new RuntimeException(e);
                  }
                });

            // Log memory usage pattern
            final StringBuilder profile = new StringBuilder();
            profile.append("Memory usage profile for ").append(runtimeType).append(":\n");
            final String[] stages = {
              "Initial",
              "Engine Created",
              "Store Created",
              "Module Compiled",
              "Store Configured",
              "Module Closed",
              "Store Closed",
              "Engine Closed",
              "After GC"
            };

            for (int i = 0; i < memorySnapshots.size() && i < stages.length; i++) {
              final long memoryMB = memorySnapshots.get(i) / (1024 * 1024);
              profile.append(String.format("  %s: %dMB", stages[i], memoryMB));
              if (i > 0) {
                final long deltaMB =
                    (memorySnapshots.get(i) - memorySnapshots.get(i - 1)) / (1024 * 1024);
                profile.append(String.format(" (Δ%+dMB)", deltaMB));
              }
              profile.append("\n");
            }

            LOGGER.info(profile.toString());

            final long totalIncrease =
                (memorySnapshots.get(memorySnapshots.size() - 1) - memorySnapshots.get(0))
                    / (1024 * 1024);
            // addTestMetric(
            //     "Total memory increase during profiling: "
            //         + totalIncrease
            //         + "MB on "
            //         + runtimeType);
          });
    }
  }
}

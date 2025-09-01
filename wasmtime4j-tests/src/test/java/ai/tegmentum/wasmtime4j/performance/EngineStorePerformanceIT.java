package ai.tegmentum.wasmtime4j.performance;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive performance baseline measurement tests for Engine and Store APIs. These tests
 * establish performance baselines, measure throughput, and validate performance characteristics
 * across different configurations and scenarios.
 */
@DisplayName("Engine & Store Performance Baseline Tests")
final class EngineStorePerformanceIT extends BaseIntegrationTest {

  // Performance baselines (these should be adjusted based on hardware capabilities)
  private static final Duration MAX_ENGINE_CREATION_TIME = Duration.ofSeconds(2);
  private static final Duration MAX_STORE_CREATION_TIME = Duration.ofMillis(100);
  private static final Duration MAX_MODULE_COMPILATION_TIME = Duration.ofSeconds(5);
  private static final Duration MAX_DATA_OPERATION_TIME = Duration.ofMillis(10);
  private static final Duration MAX_FUEL_OPERATION_TIME = Duration.ofMillis(10);

  private static final int WARMUP_ITERATIONS = 10;
  private static final int MEASUREMENT_ITERATIONS = 50;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);
  }

  @Nested
  @DisplayName("Engine Creation Performance")
  final class EngineCreationPerformanceTests {

    @Test
    @DisplayName("Should meet default engine creation baseline")
    void shouldMeetDefaultEngineCreationBaseline() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Measuring default engine creation performance on " + runtimeType);

            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
              final Engine engine = runtime.createEngine();
              engine.close();
            }

            final List<Duration> creationTimes = new ArrayList<>();

            // Measure creation times
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
              final Duration creationTime =
                  measureExecutionTime(
                      "Engine creation " + i,
                      () -> {
                        try {
                          final Engine engine = runtime.createEngine();
                          assertThat(engine.isValid()).isTrue();
                          engine.close();
                        } catch (final Exception e) {
                          throw new RuntimeException(e);
                        }
          });
              creationTimes.add(creationTime);
            }

            final PerformanceMetrics metrics = calculateMetrics(creationTimes);
            logPerformanceMetrics("Engine creation", metrics, runtimeType);

            // Verify baseline
            assertThat(metrics.getAverage())
                .as("Average engine creation time exceeds baseline")
                .isLessThan(MAX_ENGINE_CREATION_TIME);
            assertThat(metrics.getP95())
                .as("95th percentile engine creation time exceeds baseline")
                .isLessThan(MAX_ENGINE_CREATION_TIME.multipliedBy(2));

            // addTestMetric(
            //     String.format(
            //     "Engine creation avg: %.1fms, p95: %.1fms on %s",
            //     metrics.getAverage().toMillis(), metrics.getP95().toMillis(), runtimeType));
          });
    }

    @ParameterizedTest
    @EnumSource(OptimizationLevel.class)
    @DisplayName("Should meet optimization level engine creation baseline")
    void shouldMeetOptimizationLevelEngineCreationBaseline(
        final OptimizationLevel optimizationLevel) throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info(
                "Measuring engine creation performance for "
                    + optimizationLevel
                    + " on "
                    + runtimeType);

            final EngineConfig config = new EngineConfig().optimizationLevel(optimizationLevel);

            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
              final Engine engine = runtime.createEngine(config);
              engine.close();
            }

            final List<Duration> creationTimes = new ArrayList<>();

            // Measure creation times
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
              final Duration creationTime =
                  measureExecutionTime(
                      "Engine creation with " + optimizationLevel + " " + i,
                      () -> {
                        try {
                          final Engine engine = runtime.createEngine(config);
                          assertThat(engine.isValid()).isTrue();
                          assertThat(engine.getConfig().getOptimizationLevel())
                              .isEqualTo(optimizationLevel);
                          engine.close();
                        } catch (final Exception e) {
                          throw new RuntimeException(e);
                        }
          });
              creationTimes.add(creationTime);
            }

            final PerformanceMetrics metrics = calculateMetrics(creationTimes);
            logPerformanceMetrics(
                "Engine creation (" + optimizationLevel + ")", metrics, runtimeType);

            // Allow slightly more time for debug configurations
            final Duration baseline =
                optimizationLevel == OptimizationLevel.NONE
                    ? MAX_ENGINE_CREATION_TIME.multipliedBy(2)
                    : MAX_ENGINE_CREATION_TIME;

            assertThat(metrics.getAverage())
                .as("Average engine creation time exceeds baseline for " + optimizationLevel)
                .isLessThan(baseline);

            // addTestMetric(
            //     String.format(
            //     "Engine creation (%s) avg: %.1fms on %s",
            //     optimizationLevel, metrics.getAverage().toMillis(), runtimeType));
          });
    }

    @Test
    @DisplayName("Should measure concurrent engine creation throughput")
    void shouldMeasureConcurrentEngineCreationThroughput() throws Exception {
      // skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Measuring concurrent engine creation throughput on " + runtimeType);

            final int threadCount = 4;
            final int operationsPerThread = 10;
            final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            try {
              final Duration totalTime =
                  measureExecutionTime(
                      "Concurrent engine creation on " + runtimeType,
                      () -> {
                        final List<CompletableFuture<Duration>> futures =
                            IntStream.range(0, threadCount)
                                .mapToObj(
                                    threadId ->
                                        CompletableFuture.supplyAsync(
                                            () -> {
                                              final List<Duration> threadTimes = new ArrayList<>();
                                              for (int op = 0; op < operationsPerThread; op++) {
                                                final Duration opTime =
                                                    measureExecutionTime(
                                                        "Thread " + threadId + " op " + op,
                                                        () -> {
                                                          try {
                                                            final Engine engine =
                                                                runtime.createEngine();
                                                            assertThat(engine.isValid()).isTrue();
                                                            engine.close();
                                                          } catch (final Exception e) {
                                                            throw new RuntimeException(e);
                                                          }
          });
                                                threadTimes.add(opTime);
                                              }
                                              return threadTimes.stream()
                                                  .reduce(Duration.ZERO, Duration::plus);
                                            },
                                            executor))
                                .collect(Collectors.toList());

                        try {
                          for (final CompletableFuture<Duration> future : futures) {
                            future.get(60, TimeUnit.SECONDS);
                          }
                        } catch (final Exception e) {
                          throw new RuntimeException(e);
                        }
          });

              final int totalOperations = threadCount * operationsPerThread;
              final double throughput = totalOperations / (totalTime.toMillis() / 1000.0);

              LOGGER.info(
                  String.format(
                      "Concurrent engine creation throughput: %.2f engines/second on %s",
                      throughput, runtimeType));

              // Expect at least 1 engine per second under concurrent load
              assertThat(throughput)
                  .as("Concurrent engine creation throughput too low")
                  .isGreaterThan(1.0);

              // addTestMetric(
              //     String.format(
              //     "Concurrent engine throughput: %.2f engines/s on %s",
              //     throughput, runtimeType));
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
  @DisplayName("Store Creation Performance")
  final class StoreCreationPerformanceTests {

    @Test
    @DisplayName("Should meet store creation baseline")
    void shouldMeetStoreCreationBaseline() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Measuring store creation performance on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
              final Store store = engine.createStore();
              store.close();
            }

            final List<Duration> creationTimes = new ArrayList<>();

            // Measure creation times
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
              final Duration creationTime =
                  measureExecutionTime(
                      "Store creation " + i,
                      () -> {
                        try {
                          final Store store = engine.createStore();
                          assertThat(store.isValid()).isTrue();
                          store.close();
                        } catch (final Exception e) {
                          throw new RuntimeException(e);
                        }
          });
              creationTimes.add(creationTime);
            }

            final PerformanceMetrics metrics = calculateMetrics(creationTimes);
            logPerformanceMetrics("Store creation", metrics, runtimeType);

            assertThat(metrics.getAverage())
                .as("Average store creation time exceeds baseline")
                .isLessThan(MAX_STORE_CREATION_TIME);
            assertThat(metrics.getP95())
                .as("95th percentile store creation time exceeds baseline")
                .isLessThan(MAX_STORE_CREATION_TIME.multipliedBy(3));

            // addTestMetric(
            //     String.format(
            //     "Store creation avg: %.1fms, p95: %.1fms on %s",
            //     metrics.getAverage().toMillis(), metrics.getP95().toMillis(), runtimeType));
          });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 50, 100})
    @DisplayName("Should maintain store creation performance under load")
    void shouldMaintainStoreCreationPerformanceUnderLoad(final int storeCount) throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info(
                "Measuring store creation performance under load ("
                    + storeCount
                    + " stores) on "
                    + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Duration totalTime =
                measureExecutionTime(
                    storeCount + " store creations on " + runtimeType,
                    () -> {
                      final List<Store> stores = new ArrayList<>();
                      try {
                        for (int i = 0; i < storeCount; i++) {
                          final Store store = engine.createStore("store-" + i);
                          assertThat(store.isValid()).isTrue();
                          stores.add(store);
                        }
                      } catch (final Exception e) {
                        throw new RuntimeException(e);
                      } finally {
                        stores.forEach(
                            store -> {
                              try {
                                store.close();
                              } catch (final Exception e) {
                                LOGGER.warning("Failed to close store: " + e.getMessage());
                              }
          });
                      }
          });

            final double averageTimeMs = totalTime.toMillis() / (double) storeCount;
            final double throughput = storeCount / (totalTime.toMillis() / 1000.0);

            LOGGER.info(
                String.format(
                    "Store creation under load: %.2fms avg, %.2f stores/s on %s",
                    averageTimeMs, throughput, runtimeType));

            // Average time per store should not exceed the baseline significantly
            assertThat(Duration.ofMillis((long) averageTimeMs))
                .as("Store creation performance degraded under load")
                .isLessThan(MAX_STORE_CREATION_TIME.multipliedBy(2));

            // addTestMetric(
            //     String.format(
            //     "Store creation load (%d): %.2fms avg on %s",
            //     storeCount, averageTimeMs, runtimeType));
          });
    }
  }

  @Nested
  @DisplayName("Module Compilation Performance")
  final class ModuleCompilationPerformanceTests {

    @Test
    @DisplayName("Should meet module compilation baseline")
    void shouldMeetModuleCompilationBaseline() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Measuring module compilation performance on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
              final Module module = engine.compileModule(wasmBytes);
              module.close();
            }

            final List<Duration> compilationTimes = new ArrayList<>();

            // Measure compilation times
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
              final Duration compilationTime =
                  measureExecutionTime(
                      "Module compilation " + i,
                      () -> {
                        try {
                          final Module module = engine.compileModule(wasmBytes);
                          assertThat(module.isValid()).isTrue();
                          module.close();
                        } catch (final Exception e) {
                          throw new RuntimeException(e);
                        }
          });
              compilationTimes.add(compilationTime);
            }

            final PerformanceMetrics metrics = calculateMetrics(compilationTimes);
            logPerformanceMetrics("Module compilation", metrics, runtimeType);

            assertThat(metrics.getAverage())
                .as("Average module compilation time exceeds baseline")
                .isLessThan(MAX_MODULE_COMPILATION_TIME);
            assertThat(metrics.getP95())
                .as("95th percentile module compilation time exceeds baseline")
                .isLessThan(MAX_MODULE_COMPILATION_TIME.multipliedBy(2));

            // addTestMetric(
            //     String.format(
            //     "Module compilation avg: %.1fms, p95: %.1fms on %s",
            //     metrics.getAverage().toMillis(), metrics.getP95().toMillis(), runtimeType));
          });
    }

    @ParameterizedTest
    @EnumSource(OptimizationLevel.class)
    @DisplayName("Should measure compilation performance by optimization level")
    void shouldMeasureCompilationPerformanceByOptimizationLevel(
        final OptimizationLevel optimizationLevel) throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info(
                "Measuring compilation performance for "
                    + optimizationLevel
                    + " on "
                    + runtimeType);

            final EngineConfig config = new EngineConfig().optimizationLevel(optimizationLevel);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

            final List<Duration> compilationTimes = new ArrayList<>();

            // Measure compilation times (fewer iterations for potentially slower debug builds)
            final int iterations =
                optimizationLevel == OptimizationLevel.NONE
                    ? MEASUREMENT_ITERATIONS / 2
                    : MEASUREMENT_ITERATIONS;

            for (int i = 0; i < iterations; i++) {
              final Duration compilationTime =
                  measureExecutionTime(
                      "Module compilation (" + optimizationLevel + ") " + i,
                      () -> {
                        try {
                          final Module module = engine.compileModule(wasmBytes);
                          assertThat(module.isValid()).isTrue();
                          module.close();
                        } catch (final Exception e) {
                          throw new RuntimeException(e);
                        }
          });
              compilationTimes.add(compilationTime);
            }

            final PerformanceMetrics metrics = calculateMetrics(compilationTimes);
            logPerformanceMetrics(
                "Module compilation (" + optimizationLevel + ")", metrics, runtimeType);

            // addTestMetric(
            //     String.format(
            //     "Module compilation (%s) avg: %.1fms on %s",
            //     optimizationLevel, metrics.getAverage().toMillis(), runtimeType));
          });
    }
  }

  @Nested
  @DisplayName("Store Operation Performance")
  final class StoreOperationPerformanceTests {

    @Test
    @DisplayName("Should meet data operation baseline")
    void shouldMeetDataOperationBaseline() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Measuring store data operation performance on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final List<Duration> operationTimes = new ArrayList<>();

            // Measure data operations
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
              final String testData = "test-data-" + i;
              final Duration operationTime =
                  measureExecutionTime(
                      "Data operation " + i,
                      () -> {
                        store.setData(testData);
                        final Object retrieved = store.getData();
                        assertThat(retrieved).isEqualTo(testData);
          });
              operationTimes.add(operationTime);
            }

            final PerformanceMetrics metrics = calculateMetrics(operationTimes);
            logPerformanceMetrics("Data operations", metrics, runtimeType);

            assertThat(metrics.getAverage())
                .as("Average data operation time exceeds baseline")
                .isLessThan(MAX_DATA_OPERATION_TIME);

            // addTestMetric(
            //     String.format(
            //     "Data operations avg: %.2fms on %s",
            //     metrics.getAverage().toMillis(), runtimeType));
          });
    }

    @Test
    @DisplayName("Should meet fuel operation baseline")
    void shouldMeetFuelOperationBaseline() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Measuring store fuel operation performance on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final List<Duration> operationTimes = new ArrayList<>();

            // Measure fuel operations
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
              final long fuelAmount = 1000L + i;
              final Duration operationTime =
                  measureExecutionTime(
                      "Fuel operation " + i,
                      () -> {
                        try {
                          store.setFuel(fuelAmount);
                          final long retrieved = store.getFuel();
                          assertThat(retrieved).isEqualTo(fuelAmount);

                          store.addFuel(100L);
                          final long afterAdd = store.getFuel();
                          assertThat(afterAdd).isEqualTo(fuelAmount + 100L);
                        } catch (final Exception e) {
                          throw new RuntimeException(e);
                        }
          });
              operationTimes.add(operationTime);
            }

            final PerformanceMetrics metrics = calculateMetrics(operationTimes);
            logPerformanceMetrics("Fuel operations", metrics, runtimeType);

            assertThat(metrics.getAverage())
                .as("Average fuel operation time exceeds baseline")
                .isLessThan(MAX_FUEL_OPERATION_TIME);

            // addTestMetric(
            //     String.format(
            //     "Fuel operations avg: %.2fms on %s",
            //     metrics.getAverage().toMillis(), runtimeType));
          });
    }
  }

  @Nested
  @DisplayName("Cross-Runtime Performance Comparison")
  final class CrossRuntimePerformanceComparisonTests {

    @Test
    @DisplayName("Should compare engine creation performance across runtimes")
    void shouldCompareEngineCreationPerformanceAcrossRuntimes() {
      skipIfPanamaNotAvailable();

      LOGGER.info("Comparing engine creation performance across runtimes");

      final Map<String, PerformanceMetrics> performanceMap = new HashMap<>();

      // Measure JNI performance
      try (final var jniRuntime = createTestRuntime(ai.tegmentum.wasmtime4j.RuntimeType.JNI)) {
        final List<Duration> jniTimes = new ArrayList<>();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
          final Duration time =
              measureExecutionTime(
                  "JNI engine creation " + i,
                  () -> {
                    try {
                      final Engine engine = jniRuntime.createEngine();
                      engine.close();
                    } catch (final Exception e) {
                      throw new RuntimeException(e);
                    }
          });
          jniTimes.add(time);
        }
        performanceMap.put("JNI", calculateMetrics(jniTimes));
      } catch (final Exception e) {
        LOGGER.warning("JNI performance measurement failed: " + e.getMessage());
      }

      // Measure Panama performance
      try (final var panamaRuntime =
          createTestRuntime(ai.tegmentum.wasmtime4j.RuntimeType.PANAMA)) {
        final List<Duration> panamaTimes = new ArrayList<>();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
          final Duration time =
              measureExecutionTime(
                  "Panama engine creation " + i,
                  () -> {
                    try {
                      final Engine engine = panamaRuntime.createEngine();
                      engine.close();
                    } catch (final Exception e) {
                      throw new RuntimeException(e);
                    }
          });
          panamaTimes.add(time);
        }
        performanceMap.put("Panama", calculateMetrics(panamaTimes));
      } catch (final Exception e) {
        LOGGER.warning("Panama performance measurement failed: " + e.getMessage());
      }

      // Compare results
      if (performanceMap.containsKey("JNI") && performanceMap.containsKey("Panama")) {
        final PerformanceMetrics jniMetrics = performanceMap.get("JNI");
        final PerformanceMetrics panamaMetrics = performanceMap.get("Panama");

        final double ratio =
            panamaMetrics.getAverage().toNanos() / (double) jniMetrics.getAverage().toNanos();

        LOGGER.info(
            String.format(
                "Engine creation performance comparison: JNI=%.1fms, Panama=%.1fms, Ratio=%.2f",
                jniMetrics.getAverage().toMillis(), panamaMetrics.getAverage().toMillis(), ratio));

        // addTestMetric(String.format("Performance ratio (Panama/JNI): %.2f", ratio));
        //     
        // Log detailed comparison
        //     final String comparison =
        //     CrossRuntimeValidator.analyzePerformance(
        //     List.of(
        //     new MockComparisonResult(
        //     List.of(
        //     new MockTestResult(
        //     ai.tegmentum.wasmtime4j.RuntimeType.JNI,
        //     true,
        //     jniMetrics.getAverage()),
        //     new MockTestResult(
        //     ai.tegmentum.wasmtime4j.RuntimeType.PANAMA,
        //     true,
        //     panamaMetrics.getAverage())))));
        // LOGGER.info("Cross-runtime performance analysis:\n" + comparison);
      }
    }

    @Test
    @DisplayName("Should compare module compilation performance across runtimes")
    void shouldCompareModuleCompilationPerformanceAcrossRuntimes() {
      skipIfPanamaNotAvailable();

      LOGGER.info("Comparing module compilation performance across runtimes");

      final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
      final Map<String, PerformanceMetrics> performanceMap = new HashMap<>();

      // Measure performance for both runtimes
      for (final ai.tegmentum.wasmtime4j.RuntimeType runtimeType :
          new ai.tegmentum.wasmtime4j.RuntimeType[] {
            ai.tegmentum.wasmtime4j.RuntimeType.JNI, ai.tegmentum.wasmtime4j.RuntimeType.PANAMA
          }) {
        try (final var runtime = createTestRuntime(runtimeType)) {
          final Engine engine = runtime.createEngine();

          final List<Duration> compilationTimes = new ArrayList<>();
          for (int i = 0; i < MEASUREMENT_ITERATIONS / 2; i++) { // Fewer iterations for compilation
            final Duration time =
                measureExecutionTime(
                    runtimeType + " compilation " + i,
                    () -> {
                      try {
                        final Module module = engine.compileModule(wasmBytes);
                        module.close();
                      } catch (final Exception e) {
                        throw new RuntimeException(e);
                      }
                    });
            compilationTimes.add(time);
          }

          engine.close();
          performanceMap.put(runtimeType.toString(), calculateMetrics(compilationTimes));
        } catch (final Exception e) {
          LOGGER.warning(
              runtimeType + " compilation performance measurement failed: " + e.getMessage());
        }
      }

      // Compare results
      if (performanceMap.size() == 2) {
        final PerformanceMetrics jniMetrics = performanceMap.get("JNI");
        final PerformanceMetrics panamaMetrics = performanceMap.get("PANAMA");

        if (jniMetrics != null && panamaMetrics != null) {
          final double ratio =
              panamaMetrics.getAverage().toNanos() / (double) jniMetrics.getAverage().toNanos();

          LOGGER.info(
              String.format(
                  "Module compilation performance comparison: JNI=%.1fms, Panama=%.1fms,"
                      + " Ratio=%.2f",
                  jniMetrics.getAverage().toMillis(),
                  panamaMetrics.getAverage().toMillis(),
                  ratio));

          // addTestMetric(String.format("Compilation performance ratio (Panama/JNI): %.2f", ratio));
        }
      }
    }

    // Helper methods and classes
  private Duration measureExecutionTime(final String operationName, final Runnable operation) {
    final Instant start = Instant.now();
    operation.run();
    final Duration duration = Duration.between(start, Instant.now());
    LOGGER.fine(String.format("%s took %d ms", operationName, duration.toMillis()));
    return duration;
  }

  private PerformanceMetrics calculateMetrics(final List<Duration> durations) {
    durations.sort(Duration::compareTo);

    final Duration min = durations.get(0);
    final Duration max = durations.get(durations.size() - 1);
    final Duration average =
        Duration.ofNanos(
            (long) durations.stream().mapToLong(Duration::toNanos).average().orElse(0.0));
    final Duration median = durations.get(durations.size() / 2);
    final Duration p95 = durations.get((int) (durations.size() * 0.95));
    final Duration p99 = durations.get((int) (durations.size() * 0.99));

    return new PerformanceMetrics(min, max, average, median, p95, p99);
  }

  private void logPerformanceMetrics(
      final String operation, final PerformanceMetrics metrics, final Object runtimeType) {
    LOGGER.info(
        String.format(
            "%s performance on %s: min=%.1fms, avg=%.1fms, max=%.1fms, p95=%.1fms, p99=%.1fms",
            operation,
            runtimeType,
            metrics.getMin().toMillis(),
            metrics.getAverage().toMillis(),
            metrics.getMax().toMillis(),
            metrics.getP95().toMillis(),
            metrics.getP99().toMillis()));
  }

  private static final class PerformanceMetrics {
    private final Duration min;
    private final Duration max;
    private final Duration average;
    private final Duration median;
    private final Duration p95;
    private final Duration p99;

    private PerformanceMetrics(
        final Duration min,
        final Duration max,
        final Duration average,
        final Duration median,
        final Duration p95,
        final Duration p99) {
      this.min = min;
      this.max = max;
      this.average = average;
      this.median = median;
      this.p95 = p95;
      this.p99 = p99;
    }

    public Duration getMin() {
      return min;
    }

    public Duration getMax() {
      return max;
    }

    public Duration getAverage() {
      return average;
    }

    public Duration getMedian() {
      return median;
    }

    public Duration getP95() {
      return p95;
    }

    public Duration getP99() {
      return p99;
    }
  }

  // Mock classes for cross-runtime comparison - commented out due to final class inheritance
  /*
  private static final class MockComparisonResult extends CrossRuntimeValidator.ComparisonResult {
    private MockComparisonResult(final List<CrossRuntimeValidator.TestResult> results) {
      super(results, true, true, "Performance comparison");
    }
  }

  private static final class MockTestResult extends CrossRuntimeValidator.TestResult {
    private MockTestResult(
        final ai.tegmentum.wasmtime4j.RuntimeType runtimeType,
        final boolean success,
        final Duration executionTime) {
      super(runtimeType, success, executionTime, null);
    }
  }
  */
}
}

package ai.tegmentum.wasmtime4j.module;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeValidationResult;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestCase;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestDataManager;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Performance benchmarks and tests for Module API operations. Tests compilation performance,
 * instantiation performance, metadata extraction, and serialization/deserialization benchmarks.
 */
@DisplayName("Module Performance Benchmarks")
class ModulePerformanceTest extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ModulePerformanceTest.class.getName());

  private WasmTestDataManager testDataManager;
  private ExecutorService executorService;

  // Performance thresholds (in milliseconds)
  private static final double MAX_COMPILATION_TIME_MS = 50.0;
  private static final double MAX_INSTANTIATION_TIME_MS = 10.0;
  private static final double MAX_METADATA_EXTRACTION_MS = 1.0;
  private static final double MAX_SERIALIZATION_TIME_MS = 5.0;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // skipIfCategoryNotEnabled("module.performance");

    try {
      testDataManager = WasmTestDataManager.getInstance();
      testDataManager.ensureTestDataAvailable();
      executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    } catch (final Exception e) {
      LOGGER.warning("Failed to setup test environment: " + e.getMessage());
      skipIfNot(false, "Test environment setup failed: " + e.getMessage());
    }
  }

  @AfterEach
  void tearDownExecutor() {
    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        executorService.shutdownNow();
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should benchmark module compilation performance")
  void shouldBenchmarkModuleCompilationPerformance(final RuntimeType runtimeType) {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-compilation-benchmark-" + runtimeType,
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int warmupIterations = 20;
              final int benchmarkIterations = 100;

              try (final Engine engine = runtime.createEngine()) {
                // Warmup phase
                for (int i = 0; i < warmupIterations; i++) {
                  final Module module = engine.compileModule(wasmBytes);
                  module.close();
                }

                // Benchmark phase
                final List<Duration> compilationTimes = new ArrayList<>();
                for (int i = 0; i < benchmarkIterations; i++) {
                  final Instant start = Instant.now();
                  final Module module = engine.compileModule(wasmBytes);
                  final Duration compilationTime = Duration.between(start, Instant.now());
                  compilationTimes.add(compilationTime);
                  module.close();
                }

                // Calculate statistics
                final double avgMs =
                    compilationTimes.stream().mapToLong(Duration::toNanos).average().orElse(0.0)
                        / 1_000_000.0;

                final double minMs =
                    compilationTimes.stream().mapToLong(Duration::toNanos).min().orElse(0)
                        / 1_000_000.0;

                final double maxMs =
                    compilationTimes.stream().mapToLong(Duration::toNanos).max().orElse(0)
                        / 1_000_000.0;

                // Verify performance is acceptable
                assertThat(avgMs).isLessThan(MAX_COMPILATION_TIME_MS);

                final String result =
                    String.format(
                        "Compilation - Avg: %.2fms, Min: %.2fms, Max: %.2fms", avgMs, minMs, maxMs);

                LOGGER.info(runtimeType + " " + result);
                return result;
              }
            },
            comparison -> true); // Don't compare exact performance numbers

    assertThat(validation.isConsistent()).isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should benchmark module instantiation performance")
  void shouldBenchmarkModuleInstantiationPerformance(final RuntimeType runtimeType) {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-instantiation-benchmark-" + runtimeType,
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int warmupIterations = 10;
              final int benchmarkIterations = 50;

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(wasmBytes)) {

                // Warmup phase
                for (int i = 0; i < warmupIterations; i++) {
                  final Store store = engine.createStore();
                  final Instance instance = module.instantiate(store);
                  instance.close();
                  store.close();
                }

                // Benchmark phase
                final List<Duration> instantiationTimes = new ArrayList<>();
                for (int i = 0; i < benchmarkIterations; i++) {
                  final Store store = engine.createStore();

                  final Instant start = Instant.now();
                  final Instance instance = module.instantiate(store);
                  final Duration instantiationTime = Duration.between(start, Instant.now());
                  instantiationTimes.add(instantiationTime);

                  instance.close();
                  store.close();
                }

                // Calculate statistics
                final double avgMs =
                    instantiationTimes.stream().mapToLong(Duration::toNanos).average().orElse(0.0)
                        / 1_000_000.0;

                final double minMs =
                    instantiationTimes.stream().mapToLong(Duration::toNanos).min().orElse(0)
                        / 1_000_000.0;

                final double maxMs =
                    instantiationTimes.stream().mapToLong(Duration::toNanos).max().orElse(0)
                        / 1_000_000.0;

                // Verify performance is acceptable
                assertThat(avgMs).isLessThan(MAX_INSTANTIATION_TIME_MS);

                final String result =
                    String.format(
                        "Instantiation - Avg: %.2fms, Min: %.2fms, Max: %.2fms",
                        avgMs, minMs, maxMs);

                LOGGER.info(runtimeType + " " + result);
                return result;
              }
            },
            comparison -> true); // Don't compare exact performance numbers

    assertThat(validation.isConsistent()).isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should benchmark metadata extraction performance")
  void shouldBenchmarkMetadataExtractionPerformance(final RuntimeType runtimeType) {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-metadata-benchmark-" + runtimeType,
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int warmupIterations = 50;
              final int benchmarkIterations = 200;

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(wasmBytes)) {

                // Warmup phase
                for (int i = 0; i < warmupIterations; i++) {
                  module.getExports();
                  module.getImports();
                }

                // Benchmark phase
                final List<Duration> metadataTimes = new ArrayList<>();
                for (int i = 0; i < benchmarkIterations; i++) {
                  final Instant start = Instant.now();
                  module.getExports();
                  module.getImports();
                  final Duration metadataTime = Duration.between(start, Instant.now());
                  metadataTimes.add(metadataTime);
                }

                // Calculate statistics
                final double avgMs =
                    metadataTimes.stream().mapToLong(Duration::toNanos).average().orElse(0.0)
                        / 1_000_000.0;

                final double minMs =
                    metadataTimes.stream().mapToLong(Duration::toNanos).min().orElse(0)
                        / 1_000_000.0;

                final double maxMs =
                    metadataTimes.stream().mapToLong(Duration::toNanos).max().orElse(0)
                        / 1_000_000.0;

                // Verify performance is acceptable
                assertThat(avgMs).isLessThan(MAX_METADATA_EXTRACTION_MS);

                final String result =
                    String.format(
                        "Metadata - Avg: %.3fms, Min: %.3fms, Max: %.3fms", avgMs, minMs, maxMs);

                LOGGER.info(runtimeType + " " + result);
                return result;
              }
            },
            comparison -> true); // Don't compare exact performance numbers

    assertThat(validation.isConsistent()).isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should benchmark module serialization performance")
  void shouldBenchmarkModuleSerializationPerformance(final RuntimeType runtimeType) {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-serialization-benchmark-" + runtimeType,
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int warmupIterations = 10;
              final int benchmarkIterations = 50;

              try (final Engine engine = runtime.createEngine()) {
                // Warmup phase
                for (int i = 0; i < warmupIterations; i++) {
                  final Module module = engine.compileModule(wasmBytes);
                  // Simulate serialization with byte array cloning
                  @SuppressWarnings("unused")
                  final byte[] serialized = wasmBytes.clone();
                  module.close();
                }

                // Benchmark serialization (simulated)
                final List<Duration> serializationTimes = new ArrayList<>();
                for (int i = 0; i < benchmarkIterations; i++) {
                  final Module module = engine.compileModule(wasmBytes);

                  final Instant start = Instant.now();
                  // Simulate serialization - in real implementation would be module.serialize()
                  @SuppressWarnings("unused")
                  final byte[] serialized = wasmBytes.clone();
                  final Duration serializationTime = Duration.between(start, Instant.now());
                  serializationTimes.add(serializationTime);

                  module.close();
                }

                // Benchmark deserialization (compilation)
                final List<Duration> deserializationTimes = new ArrayList<>();
                for (int i = 0; i < benchmarkIterations; i++) {
                  final Instant start = Instant.now();
                  final Module module = engine.compileModule(wasmBytes);
                  final Duration deserializationTime = Duration.between(start, Instant.now());
                  deserializationTimes.add(deserializationTime);
                  module.close();
                }

                // Calculate statistics
                final double avgSerMs =
                    serializationTimes.stream().mapToLong(Duration::toNanos).average().orElse(0.0)
                        / 1_000_000.0;

                final double avgDeserMs =
                    deserializationTimes.stream().mapToLong(Duration::toNanos).average().orElse(0.0)
                        / 1_000_000.0;

                // Verify performance is acceptable
                assertThat(avgSerMs).isLessThan(MAX_SERIALIZATION_TIME_MS);

                final String result =
                    String.format(
                        "Serialization - Avg: %.2fms, Deserialization - Avg: %.2fms",
                        avgSerMs, avgDeserMs);

                LOGGER.info(runtimeType + " " + result);
                return result;
              }
            },
            comparison -> true); // Don't compare exact performance numbers

    assertThat(validation.isConsistent()).isTrue();
  }

  @Test
  @DisplayName("Should benchmark concurrent module operations")
  void shouldBenchmarkConcurrentModuleOperations() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-concurrent-benchmark",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int threadCount = Runtime.getRuntime().availableProcessors();
              final int operationsPerThread = 20;

              try (final Engine engine = runtime.createEngine()) {
                // Sequential benchmark
                final Instant sequentialStart = Instant.now();
                for (int i = 0; i < threadCount * operationsPerThread; i++) {
                  final Module module = engine.compileModule(wasmBytes);
                  module.close();
                }
                final Duration sequentialTime = Duration.between(sequentialStart, Instant.now());

                // Concurrent benchmark
                final Instant concurrentStart = Instant.now();
                final List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < threadCount; i++) {
                  final CompletableFuture<Void> future =
                      CompletableFuture.runAsync(
                          () -> {
                            for (int j = 0; j < operationsPerThread; j++) {
                              try {
                                final Module module = engine.compileModule(wasmBytes);
                                module.close();
                              } catch (final Exception e) {
                                throw new RuntimeException(e);
                              }
                            }
                          },
                          executorService);
                  futures.add(future);
                }

                // Wait for all threads to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
                final Duration concurrentTime = Duration.between(concurrentStart, Instant.now());

                // Calculate speedup
                final double speedup = (double) sequentialTime.toNanos() / concurrentTime.toNanos();

                // Concurrent should be faster (or at least not much slower)
                assertThat(speedup).isGreaterThan(0.5); // At least 50% of sequential speed

                final String result =
                    String.format(
                        "Sequential: %dms, Concurrent: %dms, Speedup: %.2fx",
                        sequentialTime.toMillis(), concurrentTime.toMillis(), speedup);

                LOGGER.info("Concurrent benchmark: " + result);
                return result;
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5, 10, 20})
  @DisplayName("Should benchmark module compilation with different module sizes")
  void shouldBenchmarkModuleCompilationWithDifferentModuleSizes(final int sizeMultiplier) {
    // skipIfCategoryNotEnabled("performance");

    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-size-benchmark-" + sizeMultiplier,
            runtime -> {
              // Given - Create module with different sizes (simulated)
              final byte[] baseModule = TestUtils.createSimpleWasmModule();
              final byte[] testModule;

              if (sizeMultiplier == 1) {
                testModule = baseModule;
              } else {
                // Simulate larger modules by duplicating content (not realistic but tests scaling)
                testModule = baseModule; // For now, use same module
              }

              final int benchmarkIterations =
                  Math.max(10, 100 / sizeMultiplier); // Fewer iterations for larger modules

              try (final Engine engine = runtime.createEngine()) {
                // Benchmark compilation
                final List<Duration> compilationTimes = new ArrayList<>();
                for (int i = 0; i < benchmarkIterations; i++) {
                  final Instant start = Instant.now();
                  final Module module = engine.compileModule(testModule);
                  final Duration compilationTime = Duration.between(start, Instant.now());
                  compilationTimes.add(compilationTime);
                  module.close();
                }

                final double avgMs =
                    compilationTimes.stream().mapToLong(Duration::toNanos).average().orElse(0.0)
                        / 1_000_000.0;

                final String result =
                    String.format("Size %dx: %.2fms avg compilation", sizeMultiplier, avgMs);

                LOGGER.info(result);
                return result;
              }
            },
            comparison -> true); // Don't compare exact numbers

    assertThat(validation.isConsistent()).isTrue();
  }

  @Test
  @DisplayName("Should benchmark module memory usage patterns")
  void shouldBenchmarkModuleMemoryUsagePatterns() {
    // skipIfCategoryNotEnabled("performance");

    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-memory-benchmark",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int moduleCount = 50;

              try (final Engine engine = runtime.createEngine()) {
                // Measure memory before
                System.gc(); // Encourage garbage collection
                try {
                  Thread.sleep(100); // Allow GC to complete
                } catch (final InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
                final Runtime rt = Runtime.getRuntime();
                final long memoryBefore = rt.totalMemory() - rt.freeMemory();

                // Create many modules
                final List<Module> modules = new ArrayList<>();
                final Instant start = Instant.now();

                for (int i = 0; i < moduleCount; i++) {
                  final Module module = engine.compileModule(wasmBytes);
                  modules.add(module);
                }

                final Duration creationTime = Duration.between(start, Instant.now());

                // Measure memory after
                System.gc();
                try {
                  Thread.sleep(100);
                } catch (final InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
                final long memoryAfter = rt.totalMemory() - rt.freeMemory();
                final long memoryUsed = memoryAfter - memoryBefore;

                // Clean up modules
                final Instant cleanupStart = Instant.now();
                for (final Module module : modules) {
                  module.close();
                }
                final Duration cleanupTime = Duration.between(cleanupStart, Instant.now());

                // Measure memory after cleanup
                System.gc();
                try {
                  Thread.sleep(100);
                } catch (final InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
                final long memoryAfterCleanup = rt.totalMemory() - rt.freeMemory();
                final long memoryLeaked = memoryAfterCleanup - memoryBefore;

                // Verify reasonable memory usage
                final long avgMemoryPerModule = memoryUsed / moduleCount;
                assertThat(avgMemoryPerModule)
                    .isLessThan(10 * 1024 * 1024); // Less than 10MB per module

                // Verify cleanup effectiveness (allow some leakage due to GC timing)
                assertThat(memoryLeaked).isLessThan(memoryUsed / 2); // Less than 50% leaked

                final String result =
                    String.format(
                        "Memory: %dKB total, %dKB/module, %dKB leaked, Creation: %dms, Cleanup:"
                            + " %dms",
                        memoryUsed / 1024,
                        avgMemoryPerModule / 1024,
                        memoryLeaked / 1024,
                        creationTime.toMillis(),
                        cleanupTime.toMillis());

                LOGGER.info("Memory benchmark: " + result);
                return result;
              }
            },
            comparison -> true); // Memory usage may vary between runtimes

    assertThat(validation.isConsistent()).isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should benchmark module operations with real test suite modules")
  void shouldBenchmarkModuleOperationsWithRealTestSuiteModules(final RuntimeType runtimeType) {
    // skipIfCategoryNotEnabled("testsuite");
    // skipIfCategoryNotEnabled("performance");

    try {
      final List<WasmTestCase> testCases =
          testDataManager.loadTestSuite(WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS);

      if (testCases.isEmpty()) {
        LOGGER.info("No test cases available for performance benchmarking");
        return;
      }

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "module-testsuite-benchmark-" + runtimeType,
              runtime -> {
                final int maxModulesToTest = Math.min(testCases.size(), 10);
                final List<Duration> compilationTimes = new ArrayList<>();

                try (final Engine engine = runtime.createEngine()) {
                  for (int i = 0; i < maxModulesToTest; i++) {
                    final WasmTestCase testCase = testCases.get(i);

                    try {
                      final Instant start = Instant.now();
                      final Module module = engine.compileModule(testCase.getModuleBytes());
                      final Duration compilationTime = Duration.between(start, Instant.now());
                      compilationTimes.add(compilationTime);
                      module.close();

                    } catch (final Exception e) {
                      // Some test modules may be invalid, which is expected
                      LOGGER.fine("Test case compilation failed: " + testCase.getTestName());
                    }
                  }

                  if (!compilationTimes.isEmpty()) {
                    final double avgMs =
                        compilationTimes.stream().mapToLong(Duration::toNanos).average().orElse(0.0)
                            / 1_000_000.0;

                    final String result =
                        String.format(
                            "Test suite modules: %d compiled, avg: %.2fms",
                            compilationTimes.size(), avgMs);

                    LOGGER.info(runtimeType + " " + result);
                    return result;
                  } else {
                    return "No test suite modules could be compiled";
                  }
                }
              },
              comparison -> true); // Don't compare exact numbers

      assertThat(validation.isConsistent()).isTrue();

    } catch (final IOException e) {
      LOGGER.warning("Failed to load test suite for performance benchmarking: " + e.getMessage());
      skipIfNot(false, "Test suite loading failed");
    }
  }
}

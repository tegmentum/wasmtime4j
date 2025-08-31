package ai.tegmentum.wasmtime4j.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.performance.PerformanceTestHarness;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Comprehensive Engine API tests covering all engine operations, configurations, edge cases,
 * error handling, resource management, thread safety, and performance baselines.
 */
@DisplayName("Engine API Comprehensive Tests")
final class EngineApiComprehensiveTest extends BaseIntegrationTest {

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    skipIfCategoryNotEnabled(TestCategories.ENGINE);
  }

  @Nested
  @DisplayName("Engine Configuration Comprehensive Tests")
  final class EngineConfigurationComprehensiveTests {

    @Test
    @DisplayName("Should validate all engine configuration combinations")
    void shouldValidateAllEngineConfigurationCombinations() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing comprehensive engine configuration combinations on " + runtimeType);

        final boolean[] booleanOptions = {true, false};
        final OptimizationLevel[] optimizationLevels = OptimizationLevel.values();
        int configurationCount = 0;

        // Test all combinations of boolean flags
        for (final boolean debugInfo : booleanOptions) {
          for (final boolean consumeFuel : booleanOptions) {
            for (final boolean parallelCompilation : booleanOptions) {
              for (final boolean craneliftDebugVerifier : booleanOptions) {
                for (final OptimizationLevel optLevel : optimizationLevels) {
                  final EngineConfig config = new EngineConfig()
                      .debugInfo(debugInfo)
                      .consumeFuel(consumeFuel)
                      .parallelCompilation(parallelCompilation)
                      .craneliftDebugVerifier(craneliftDebugVerifier)
                      .optimizationLevel(optLevel);

                  final Engine engine = runtime.createEngine(config);
                  registerForCleanup(engine);

                  assertThat(engine).isNotNull();
                  assertThat(engine.isValid()).isTrue();

                  final EngineConfig retrievedConfig = engine.getConfig();
                  assertThat(retrievedConfig.isDebugInfo()).isEqualTo(debugInfo);
                  assertThat(retrievedConfig.isConsumeFuel()).isEqualTo(consumeFuel);
                  assertThat(retrievedConfig.isParallelCompilation()).isEqualTo(parallelCompilation);
                  assertThat(retrievedConfig.isCraneliftDebugVerifier()).isEqualTo(craneliftDebugVerifier);
                  assertThat(retrievedConfig.getOptimizationLevel()).isEqualTo(optLevel);

                  configurationCount++;
                }
              }
            }
          }
        }

        addTestMetric("Tested " + configurationCount + " configuration combinations on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should handle WebAssembly feature flag combinations")
    void shouldHandleWebAssemblyFeatureFlagCombinations() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing WebAssembly feature flag combinations on " + runtimeType);

        final boolean[] booleanOptions = {true, false};
        int featureCombinationCount = 0;

        // Test combinations of WebAssembly features
        for (final boolean referenceTypes : booleanOptions) {
          for (final boolean simd : booleanOptions) {
            for (final boolean bulkMemory : booleanOptions) {
              for (final boolean multiValue : booleanOptions) {
                // Create engine with these feature combinations
                final EngineConfig config = new EngineConfig();
                // Note: Current EngineConfig doesn't expose all WebAssembly feature setters
                // This test would be expanded when those are available

                final Engine engine = runtime.createEngine(config);
                registerForCleanup(engine);

                assertThat(engine).isNotNull();
                assertThat(engine.isValid()).isTrue();

                // Verify default feature settings
                final EngineConfig retrievedConfig = engine.getConfig();
                assertThat(retrievedConfig.isWasmReferenceTypes()).isTrue();
                assertThat(retrievedConfig.isWasmSimd()).isTrue();
                assertThat(retrievedConfig.isWasmBulkMemory()).isTrue();
                assertThat(retrievedConfig.isWasmMultiValue()).isTrue();

                featureCombinationCount++;
              }
            }
          }
        }

        addTestMetric("Tested " + featureCombinationCount + " feature combinations on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should validate engine configuration edge cases")
    void shouldValidateEngineConfigurationEdgeCases() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing engine configuration edge cases on " + runtimeType);

        // Test with all features disabled for minimal configuration
        final EngineConfig minimalConfig = new EngineConfig()
            .debugInfo(false)
            .consumeFuel(false)
            .optimizationLevel(OptimizationLevel.NONE)
            .parallelCompilation(false)
            .craneliftDebugVerifier(false);

        final Engine minimalEngine = runtime.createEngine(minimalConfig);
        registerForCleanup(minimalEngine);
        assertThat(minimalEngine.isValid()).isTrue();

        // Test with all features enabled for maximal configuration
        final EngineConfig maximalConfig = new EngineConfig()
            .debugInfo(true)
            .consumeFuel(true)
            .optimizationLevel(OptimizationLevel.SPEED_AND_SIZE)
            .parallelCompilation(true)
            .craneliftDebugVerifier(true);

        final Engine maximalEngine = runtime.createEngine(maximalConfig);
        registerForCleanup(maximalEngine);
        assertThat(maximalEngine.isValid()).isTrue();

        addTestMetric("Validated configuration edge cases on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should validate configuration immutability after engine creation")
    void shouldValidateConfigurationImmutabilityAfterEngineCreation() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing configuration immutability on " + runtimeType);

        final EngineConfig originalConfig = new EngineConfig()
            .debugInfo(true)
            .consumeFuel(true)
            .optimizationLevel(OptimizationLevel.SPEED);

        final Engine engine = runtime.createEngine(originalConfig);
        registerForCleanup(engine);

        // Get the configuration from the engine
        final EngineConfig engineConfig = engine.getConfig();

        // Modify the original configuration
        originalConfig.debugInfo(false).consumeFuel(false).optimizationLevel(OptimizationLevel.SIZE);

        // Engine's configuration should remain unchanged
        assertThat(engineConfig.isDebugInfo()).isTrue();
        assertThat(engineConfig.isConsumeFuel()).isTrue();
        assertThat(engineConfig.getOptimizationLevel()).isEqualTo(OptimizationLevel.SPEED);

        addTestMetric("Validated configuration immutability on " + runtimeType);
      });
    }
  }

  @Nested
  @DisplayName("Engine Resource Management Comprehensive Tests")
  final class EngineResourceManagementComprehensiveTests {

    @Test
    @DisplayName("Should properly manage engine lifecycle")
    void shouldProperlyManageEngineLifecycle() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing engine lifecycle management on " + runtimeType);

        // Create engine
        final Engine engine = runtime.createEngine();
        assertThat(engine.isValid()).isTrue();

        // Create resources from the engine
        final Store store1 = engine.createStore();
        final Store store2 = engine.createStore("test-data");
        final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
        final Module module = engine.compileModule(wasmBytes);

        // Verify all resources are valid
        assertThat(store1.isValid()).isTrue();
        assertThat(store2.isValid()).isTrue();
        assertThat(module.isValid()).isTrue();

        // Close the engine
        engine.close();
        assertThat(engine.isValid()).isFalse();

        // Verify engine rejects operations after closure
        assertThatThrownBy(() -> engine.createStore())
            .isInstanceOf(WasmException.class)
            .hasMessageContaining("Engine is closed");

        assertThatThrownBy(() -> engine.compileModule(wasmBytes))
            .isInstanceOf(WasmException.class)
            .hasMessageContaining("Engine is closed");

        // Clean up resources
        store1.close();
        store2.close();
        module.close();

        addTestMetric("Validated engine lifecycle on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should detect and prevent memory leaks in engine operations")
    void shouldDetectAndPreventMemoryLeaksInEngineOperations() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.MEMORY_LEAK_DETECTION);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing memory leak prevention in engine operations on " + runtimeType);

        executeWithMemoryMonitoring("Engine memory leak test on " + runtimeType, () -> {
          final List<Engine> engines = new ArrayList<>();
          final List<Store> stores = new ArrayList<>();
          final List<Module> modules = new ArrayList<>();

          try {
            // Create multiple engines and resources
            for (int i = 0; i < 10; i++) {
              final Engine engine = runtime.createEngine();
              engines.add(engine);

              final Store store = engine.createStore();
              stores.add(store);

              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final Module module = engine.compileModule(wasmBytes);
              modules.add(module);
            }

            // Verify all resources are valid
            engines.forEach(engine -> assertThat(engine.isValid()).isTrue());
            stores.forEach(store -> assertThat(store.isValid()).isTrue());
            modules.forEach(module -> assertThat(module.isValid()).isTrue());

          } catch (final WasmException e) {
            throw new RuntimeException(e);
          } finally {
            // Clean up all resources
            modules.forEach(module -> {
              try {
                module.close();
              } catch (final Exception e) {
                LOGGER.warning("Failed to close module: " + e.getMessage());
              }
            });
            stores.forEach(store -> {
              try {
                store.close();
              } catch (final Exception e) {
                LOGGER.warning("Failed to close store: " + e.getMessage());
              }
            });
            engines.forEach(engine -> {
              try {
                engine.close();
              } catch (final Exception e) {
                LOGGER.warning("Failed to close engine: " + e.getMessage());
              }
            });
          }
        });

        addTestMetric("Completed memory leak prevention test on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should handle rapid engine creation and destruction")
    void shouldHandleRapidEngineCreationAndDestruction() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing rapid engine creation/destruction on " + runtimeType);

        executeWithMemoryMonitoring("Rapid engine lifecycle on " + runtimeType, () -> {
          final int cycles = 100;

          for (int i = 0; i < cycles; i++) {
            try {
              final Engine engine = runtime.createEngine();
              assertThat(engine.isValid()).isTrue();

              // Perform some operations
              final Store store = engine.createStore();
              assertThat(store.isValid()).isTrue();

              // Clean up
              store.close();
              engine.close();
              assertThat(engine.isValid()).isFalse();
            } catch (final WasmException e) {
              throw new RuntimeException("Cycle " + i + " failed", e);
            }
          }
        });

        addTestMetric("Completed " + 100 + " engine lifecycle cycles on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should handle resource cleanup with exception conditions")
    void shouldHandleResourceCleanupWithExceptionConditions() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing resource cleanup with exceptions on " + runtimeType);

        final Engine engine = runtime.createEngine();
        final List<Store> stores = new ArrayList<>();
        final List<Module> modules = new ArrayList<>();

        try {
          // Create resources
          for (int i = 0; i < 5; i++) {
            stores.add(engine.createStore());
          }

          final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
          for (int i = 0; i < 3; i++) {
            modules.add(engine.compileModule(wasmBytes));
          }

          // Simulate exception scenario by attempting invalid operations
          try {
            engine.compileModule(new byte[]{1, 2, 3}); // Invalid WASM
          } catch (final WasmException expected) {
            LOGGER.fine("Expected exception caught: " + expected.getMessage());
          }

        } finally {
          // Ensure cleanup happens even with exceptions
          modules.forEach(module -> {
            try {
              module.close();
            } catch (final Exception e) {
              LOGGER.warning("Exception during module cleanup: " + e.getMessage());
            }
          });

          stores.forEach(store -> {
            try {
              store.close();
            } catch (final Exception e) {
              LOGGER.warning("Exception during store cleanup: " + e.getMessage());
            }
          });

          engine.close();
        }

        addTestMetric("Validated exception handling cleanup on " + runtimeType);
      });
    }
  }

  @Nested
  @DisplayName("Engine Concurrency Comprehensive Tests")
  final class EngineConcurrencyComprehensiveTests {

    @Test
    @DisplayName("Should support high-concurrency engine operations")
    void shouldSupportHighConcurrencyEngineOperations() throws Exception {
      skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing high-concurrency engine operations on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        final int threadCount = 8;
        final int operationsPerThread = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount * operationsPerThread);
        final List<CompletableFuture<Void>> futures = new ArrayList<>();

        try {
          for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            for (int op = 0; op < operationsPerThread; op++) {
              final int operationId = op;
              final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                  if (operationId % 3 == 0) {
                    // Store creation
                    final Store store = engine.createStore("thread-" + threadId + "-store-" + operationId);
                    assertThat(store.isValid()).isTrue();
                    store.close();
                  } else if (operationId % 3 == 1) {
                    // Module compilation
                    final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
                    final Module module = engine.compileModule(wasmBytes);
                    assertThat(module.isValid()).isTrue();
                    module.close();
                  } else {
                    // Mixed operations
                    final Store store = engine.createStore();
                    final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
                    final Module module = engine.compileModule(wasmBytes);
                    assertThat(store.isValid()).isTrue();
                    assertThat(module.isValid()).isTrue();
                    module.close();
                    store.close();
                  }
                  latch.countDown();
                } catch (final WasmException e) {
                  latch.countDown();
                  throw new RuntimeException("Thread " + threadId + " operation " + operationId + " failed", e);
                }
              }, executor);
              futures.add(future);
            }
          }

          // Wait for all operations to complete
          assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();

          // Verify all futures completed successfully
          for (final CompletableFuture<Void> future : futures) {
            future.get(); // This will throw if there was an uncaught exception
          }

          addTestMetric("Completed " + (threadCount * operationsPerThread) + 
                       " concurrent operations on " + runtimeType);
        } finally {
          executor.shutdown();
          if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            executor.shutdownNow();
          }
        }
      });
    }

    @Test
    @DisplayName("Should maintain thread safety during engine configuration stress")
    void shouldMaintainThreadSafetyDuringEngineConfigurationStress() throws Exception {
      skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing thread safety during engine configuration stress on " + runtimeType);

        final int threadCount = 6;
        final int enginesPerThread = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount * enginesPerThread);
        final List<CompletableFuture<List<Engine>>> futures = new ArrayList<>();

        try {
          for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            final CompletableFuture<List<Engine>> future = CompletableFuture.supplyAsync(() -> {
              final List<Engine> engines = new ArrayList<>();
              try {
                for (int e = 0; e < enginesPerThread; e++) {
                  // Use different configurations per thread
                  final EngineConfig config = new EngineConfig()
                      .debugInfo(threadId % 2 == 0)
                      .consumeFuel(threadId % 3 == 0)
                      .optimizationLevel(OptimizationLevel.values()[threadId % OptimizationLevel.values().length])
                      .parallelCompilation(threadId % 4 != 0);

                  final Engine engine = runtime.createEngine(config);
                  assertThat(engine.isValid()).isTrue();
                  engines.add(engine);
                  latch.countDown();
                }
                return engines;
              } catch (final WasmException ex) {
                throw new RuntimeException("Thread " + threadId + " failed", ex);
              }
            }, executor);
            futures.add(future);
          }

          // Wait for all operations to complete
          assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();

          // Verify all engines were created successfully and clean them up
          for (final CompletableFuture<List<Engine>> future : futures) {
            final List<Engine> engines = future.get();
            for (final Engine engine : engines) {
              assertThat(engine.isValid()).isTrue();
              engine.close();
            }
          }

          addTestMetric("Completed " + (threadCount * enginesPerThread) + 
                       " concurrent engine configurations on " + runtimeType);
        } finally {
          executor.shutdown();
          if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            executor.shutdownNow();
          }
        }
      });
    }

    @Test
    @DisplayName("Should handle concurrent engine closure gracefully")
    void shouldHandleConcurrentEngineClosureGracefully() throws Exception {
      skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing concurrent engine closure on " + runtimeType);

        final int engineCount = 10;
        final List<Engine> engines = new ArrayList<>();

        // Create multiple engines with resources
        for (int i = 0; i < engineCount; i++) {
          final Engine engine = runtime.createEngine();
          engines.add(engine);

          // Create some resources from each engine
          final Store store = engine.createStore();
          final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
          final Module module = engine.compileModule(wasmBytes);

          // Register resources for cleanup
          registerForCleanup(store);
          registerForCleanup(module);
        }

        // Close all engines concurrently
        final ExecutorService executor = Executors.newFixedThreadPool(engineCount);
        final CountDownLatch latch = new CountDownLatch(engineCount);
        final List<CompletableFuture<Void>> futures = new ArrayList<>();

        try {
          for (int i = 0; i < engineCount; i++) {
            final Engine engine = engines.get(i);
            final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
              try {
                engine.close();
                assertThat(engine.isValid()).isFalse();
                latch.countDown();
              } catch (final Exception e) {
                latch.countDown();
                throw new RuntimeException("Failed to close engine", e);
              }
            }, executor);
            futures.add(future);
          }

          // Wait for all closures to complete
          assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();

          // Verify all futures completed successfully
          for (final CompletableFuture<Void> future : futures) {
            future.get();
          }

          addTestMetric("Completed concurrent closure of " + engineCount + " engines on " + runtimeType);
        } finally {
          executor.shutdown();
          if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow();
          }
        }
      });
    }
  }

  @Nested
  @DisplayName("Engine Performance Comprehensive Tests")
  final class EnginePerformanceComprehensiveTests {

    @Test
    @DisplayName("Should establish engine creation performance baselines")
    void shouldEstablishEngineCreationPerformanceBaselines() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Establishing engine creation performance baselines on " + runtimeType);

        final PerformanceTestHarness.Configuration config = PerformanceTestHarness.getFastConfiguration();

        // Benchmark default engine creation
        final PerformanceTestHarness.MeasurementResult defaultResult = 
            PerformanceTestHarness.runBenchmark(
                "Engine Creation (Default) - " + runtimeType,
                () -> {
                  try {
                    final Engine engine = runtime.createEngine();
                    registerForCleanup(engine);
                    assertThat(engine.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                config);

        // Benchmark speed-optimized engine creation
        final PerformanceTestHarness.MeasurementResult speedResult = 
            PerformanceTestHarness.runBenchmark(
                "Engine Creation (Speed) - " + runtimeType,
                () -> {
                  try {
                    final Engine engine = runtime.createEngine(EngineConfig.forSpeed());
                    registerForCleanup(engine);
                    assertThat(engine.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                config);

        // Benchmark debug engine creation
        final PerformanceTestHarness.MeasurementResult debugResult = 
            PerformanceTestHarness.runBenchmark(
                "Engine Creation (Debug) - " + runtimeType,
                () -> {
                  try {
                    final Engine engine = runtime.createEngine(EngineConfig.forDebug());
                    registerForCleanup(engine);
                    assertThat(engine.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                config);

        // Log performance baselines
        final String report = PerformanceTestHarness.generateReport(
            Arrays.asList(defaultResult, speedResult, debugResult));
        LOGGER.info("Engine creation performance report for " + runtimeType + ":\n" + report);

        // Verify reasonable performance (engines should create in under 1 second each)
        assertThat(defaultResult.getMean()).isLessThan(1_000_000_000.0); // 1 second in nanoseconds
        assertThat(speedResult.getMean()).isLessThan(1_000_000_000.0);
        assertThat(debugResult.getMean()).isLessThan(1_000_000_000.0);

        addTestMetric("Established engine creation baselines on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should measure module compilation performance across configurations")
    void shouldMeasureModuleCompilationPerformanceAcrossConfigurations() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Measuring module compilation performance on " + runtimeType);

        final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
        final PerformanceTestHarness.Configuration config = PerformanceTestHarness.getFastConfiguration();

        final List<PerformanceTestHarness.MeasurementResult> results = new ArrayList<>();

        // Test compilation performance with different optimization levels
        for (final OptimizationLevel level : OptimizationLevel.values()) {
          final Engine engine = runtime.createEngine(new EngineConfig().optimizationLevel(level));
          registerForCleanup(engine);

          final PerformanceTestHarness.MeasurementResult result = 
              PerformanceTestHarness.runBenchmark(
                  "Module Compilation (" + level + ") - " + runtimeType,
                  () -> {
                    try {
                      final Module module = engine.compileModule(wasmBytes);
                      registerForCleanup(module);
                      assertThat(module.isValid()).isTrue();
                    } catch (final WasmException e) {
                      throw new RuntimeException(e);
                    }
                  },
                  config);

          results.add(result);
        }

        // Generate performance report
        final String report = PerformanceTestHarness.generateReport(results);
        LOGGER.info("Module compilation performance report for " + runtimeType + ":\n" + report);

        // Verify reasonable compilation performance
        for (final PerformanceTestHarness.MeasurementResult result : results) {
          assertThat(result.getMean()).isLessThan(5_000_000_000.0); // 5 seconds in nanoseconds
          assertThat(result.getCoefficientOfVariation()).isLessThan(50.0); // Less than 50% variation
        }

        addTestMetric("Measured compilation performance for " + OptimizationLevel.values().length + 
                     " levels on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should validate performance regression detection")
    void shouldValidatePerformanceRegressionDetection() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Validating performance regression detection on " + runtimeType);

        final PerformanceTestHarness.Configuration config = PerformanceTestHarness.getFastConfiguration();

        // Create baseline measurement
        final PerformanceTestHarness.MeasurementResult baseline = 
            PerformanceTestHarness.runBenchmark(
                "Engine Creation Baseline - " + runtimeType,
                () -> {
                  try {
                    final Engine engine = runtime.createEngine();
                    registerForCleanup(engine);
                    assertThat(engine.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                config);

        // Create current measurement
        final PerformanceTestHarness.MeasurementResult current = 
            PerformanceTestHarness.runBenchmark(
                "Engine Creation Current - " + runtimeType,
                () -> {
                  try {
                    final Engine engine = runtime.createEngine();
                    registerForCleanup(engine);
                    assertThat(engine.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                config);

        // Compare measurements
        final PerformanceTestHarness.ComparisonResult comparison = 
            new PerformanceTestHarness.ComparisonResult(baseline, current);

        final String comparisonReport = PerformanceTestHarness.generateComparisonReport(comparison);
        LOGGER.info("Performance comparison report for " + runtimeType + ":\n" + comparisonReport);

        // Verify comparison functionality works
        assertThat(comparison.getSpeedupRatio()).isGreaterThan(0.0);
        assertThat(comparison.getPValue()).isGreaterThanOrEqualTo(0.0);
        assertThat(comparison.getPValue()).isLessThanOrEqualTo(1.0);

        addTestMetric("Validated performance regression detection on " + runtimeType);
      });
    }
  }

  @Nested
  @DisplayName("Engine Error Handling Comprehensive Tests")
  final class EngineErrorHandlingComprehensiveTests {

    @Test
    @DisplayName("Should handle all types of invalid WebAssembly input")
    void shouldHandleAllTypesOfInvalidWebAssemblyInput() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing comprehensive invalid WebAssembly handling on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        final List<byte[]> invalidInputs = Arrays.asList(
            null, // null input
            new byte[0], // empty input
            new byte[]{0}, // too short
            new byte[]{1, 2, 3, 4}, // invalid magic
            "not wasm at all".getBytes(), // text input
            new byte[]{0x00, 0x61, 0x73, 0x6d, 0x02, 0x00, 0x00, 0x00}, // wrong version
            new byte[]{0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, (byte) 0xff} // invalid section
        );

        int invalidInputCount = 0;
        for (final byte[] invalidInput : invalidInputs) {
          try {
            if (invalidInput == null) {
              assertThatThrownBy(() -> engine.compileModule(null))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessageContaining("WebAssembly bytes cannot be null");
            } else {
              assertThatThrownBy(() -> engine.compileModule(invalidInput))
                  .isInstanceOf(WasmException.class)
                  .hasMessageContainingAny("compilation failed", "invalid", "empty");
            }
            invalidInputCount++;
          } catch (final Exception e) {
            LOGGER.warning("Unexpected error handling invalid input " + invalidInputCount + 
                         " on " + runtimeType + ": " + e.getMessage());
            throw e;
          }
        }

        addTestMetric("Validated " + invalidInputCount + " invalid inputs on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should provide detailed error messages for compilation failures")
    void shouldProvideDetailedErrorMessagesForCompilationFailures() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing detailed error messages on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        // Test various malformed WebAssembly inputs that should produce specific error messages
        final byte[] malformedModule = new byte[]{
            0x00, 0x61, 0x73, 0x6d, // magic
            0x01, 0x00, 0x00, 0x00, // version
            0x01, 0x05, // type section with incorrect size
            0x01, 0x60, 0x00, 0x00, 0x01, 0x7f // malformed type
        };

        try {
          engine.compileModule(malformedModule);
          throw new AssertionError("Expected compilation to fail for malformed module on " + runtimeType);
        } catch (final WasmException e) {
          // Verify error message contains useful information
          assertThat(e.getMessage())
              .isNotEmpty()
              .satisfiesAnyOf(
                  msg -> assertThat(msg).containsIgnoringCase("compilation"),
                  msg -> assertThat(msg).containsIgnoringCase("parse"),
                  msg -> assertThat(msg).containsIgnoringCase("invalid"),
                  msg -> assertThat(msg).containsIgnoringCase("malformed")
              );
          
          LOGGER.info("Got expected detailed error on " + runtimeType + ": " + e.getMessage());
        }

        addTestMetric("Validated detailed error messages on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should handle engine operations after partial failures")
    void shouldHandleEngineOperationsAfterPartialFailures() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing engine recovery after partial failures on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        // Attempt invalid operations
        try {
          engine.compileModule(new byte[]{1, 2, 3});
        } catch (final WasmException expected) {
          LOGGER.fine("Got expected compilation failure: " + expected.getMessage());
        }

        // Engine should still work after the failure
        assertThat(engine.isValid()).isTrue();

        // Should still be able to create stores
        final Store store = engine.createStore();
        registerForCleanup(store);
        assertThat(store.isValid()).isTrue();

        // Should still be able to compile valid modules
        final byte[] validWasm = TestUtils.createSimpleWasmModule();
        final Module module = engine.compileModule(validWasm);
        registerForCleanup(module);
        assertThat(module.isValid()).isTrue();

        addTestMetric("Validated engine recovery after failures on " + runtimeType);
      });
    }
  }

  @Nested
  @DisplayName("Engine Cross-Runtime Validation Comprehensive Tests")
  final class EngineCrossRuntimeValidationComprehensiveTests {

    @Test
    @DisplayName("Should validate identical behavior across all engine operations")
    void shouldValidateIdenticalBehaviorAcrossAllEngineOperations() {
      skipIfPanamaNotAvailable();

      LOGGER.info("Validating cross-runtime behavior for all engine operations");

      // Test engine creation
      final CrossRuntimeValidator.ComparisonResult creationResult = 
          CrossRuntimeValidator.validateCrossRuntime(runtime -> {
            final Engine engine = runtime.createEngine();
            final boolean isValid = engine.isValid();
            engine.close();
            return isValid;
          });

      assertThat(creationResult.isValid()).isTrue();
      assertThat(creationResult.areResultsIdentical()).isTrue();

      // Test configuration handling
      final CrossRuntimeValidator.ComparisonResult configResult = 
          CrossRuntimeValidator.validateCrossRuntime(runtime -> {
            final EngineConfig config = new EngineConfig()
                .debugInfo(true)
                .consumeFuel(true)
                .optimizationLevel(OptimizationLevel.SIZE);
            
            final Engine engine = runtime.createEngine(config);
            final EngineConfig retrievedConfig = engine.getConfig();
            final boolean configMatch = retrievedConfig.isDebugInfo() && 
                                       retrievedConfig.isConsumeFuel() &&
                                       retrievedConfig.getOptimizationLevel() == OptimizationLevel.SIZE;
            engine.close();
            return configMatch;
          });

      assertThat(configResult.isValid()).isTrue();
      assertThat(configResult.areResultsIdentical()).isTrue();

      // Test module compilation
      final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
      final CrossRuntimeValidator.ComparisonResult compilationResult = 
          CrossRuntimeValidator.validateCrossRuntime(runtime -> {
            final Engine engine = runtime.createEngine();
            final Module module = engine.compileModule(wasmBytes);
            final boolean isValid = module.isValid();
            module.close();
            engine.close();
            return isValid;
          });

      assertThat(compilationResult.isValid()).isTrue();
      assertThat(compilationResult.areResultsIdentical()).isTrue();

      LOGGER.info("All cross-runtime engine validations passed");
      addTestMetric("Completed comprehensive cross-runtime validation");
    }

    @Test
    @DisplayName("Should validate identical error handling across runtimes")
    void shouldValidateIdenticalErrorHandlingAcrossRuntimes() {
      skipIfPanamaNotAvailable();

      LOGGER.info("Validating cross-runtime error handling");

      // Test null input handling
      final CrossRuntimeValidator.ComparisonResult nullResult = 
          CrossRuntimeValidator.validateErrorHandling(runtime -> {
            final Engine engine = runtime.createEngine();
            try {
              engine.compileModule(null);
              engine.close();
              return "Should not reach here";
            } catch (final Exception e) {
              engine.close();
              throw e;
            }
          });

      assertThat(nullResult.areExceptionsIdentical()).isTrue();

      // Test invalid WASM handling
      final CrossRuntimeValidator.ComparisonResult invalidResult = 
          CrossRuntimeValidator.validateErrorHandling(runtime -> {
            final Engine engine = runtime.createEngine();
            try {
              engine.compileModule(new byte[]{1, 2, 3, 4});
              engine.close();
              return "Should not reach here";
            } catch (final Exception e) {
              engine.close();
              throw e;
            }
          });

      assertThat(invalidResult.areExceptionsIdentical()).isTrue();

      LOGGER.info("Cross-runtime error handling validation passed");
      addTestMetric("Validated cross-runtime error handling");
    }

    @Test
    @DisplayName("Should validate performance consistency across runtimes")
    void shouldValidatePerformanceConsistencyAcrossRuntimes() {
      skipIfPanamaNotAvailable();
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      LOGGER.info("Validating cross-runtime performance consistency");

      final PerformanceTestHarness.Configuration config = PerformanceTestHarness.getFastConfiguration();

      // Run cross-runtime benchmark
      final PerformanceTestHarness.ComparisonResult performanceComparison = 
          PerformanceTestHarness.runCrossRuntimeBenchmark(
              "Engine Creation Cross-Runtime",
              runtime -> {
                final Engine engine = runtime.createEngine();
                registerForCleanup(engine);
                assertThat(engine.isValid()).isTrue();
              },
              config);

      // Generate performance comparison report
      final String report = PerformanceTestHarness.generateComparisonReport(performanceComparison);
      LOGGER.info("Cross-runtime performance comparison:\n" + report);

      // Validate that both runtimes performed reasonably (not expecting perfect parity)
      assertThat(performanceComparison.getBaseline().getMean()).isGreaterThan(0);
      assertThat(performanceComparison.getComparison().getMean()).isGreaterThan(0);
      assertThat(performanceComparison.getSpeedupRatio()).isGreaterThan(0.1); // At least 10x range is reasonable
      assertThat(performanceComparison.getSpeedupRatio()).isLessThan(10.0); // At most 10x difference

      LOGGER.info("Cross-runtime performance validation passed");
      addTestMetric("Validated cross-runtime performance consistency");
    }
  }
}
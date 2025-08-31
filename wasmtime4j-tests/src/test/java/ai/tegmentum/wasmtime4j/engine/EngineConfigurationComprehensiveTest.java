package ai.tegmentum.wasmtime4j.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.RuntimeType;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive Engine Configuration tests covering all configuration options, combinations,
 * edge cases, validation, performance impact, and cross-runtime consistency.
 */
@DisplayName("Engine Configuration Comprehensive Tests")
final class EngineConfigurationComprehensiveTest extends BaseIntegrationTest {

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    skipIfCategoryNotEnabled(TestCategories.ENGINE_CONFIGURATION);
  }

  @Nested
  @DisplayName("Configuration Factory Methods Tests")
  final class ConfigurationFactoryMethodsTests {

    @Test
    @DisplayName("Should create speed-optimized configuration correctly")
    void shouldCreateSpeedOptimizedConfigurationCorrectly() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing speed-optimized configuration on " + runtimeType);

        final EngineConfig speedConfig = EngineConfig.forSpeed();
        assertThat(speedConfig.getOptimizationLevel()).isEqualTo(OptimizationLevel.SPEED);
        assertThat(speedConfig.isParallelCompilation()).isTrue();

        final Engine engine = runtime.createEngine(speedConfig);
        registerForCleanup(engine);

        assertThat(engine.isValid()).isTrue();
        final EngineConfig retrievedConfig = engine.getConfig();
        assertThat(retrievedConfig.getOptimizationLevel()).isEqualTo(OptimizationLevel.SPEED);
        assertThat(retrievedConfig.isParallelCompilation()).isTrue();

        addTestMetric("Created speed-optimized engine on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should create size-optimized configuration correctly")
    void shouldCreateSizeOptimizedConfigurationCorrectly() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing size-optimized configuration on " + runtimeType);

        final EngineConfig sizeConfig = EngineConfig.forSize();
        assertThat(sizeConfig.getOptimizationLevel()).isEqualTo(OptimizationLevel.SIZE);
        assertThat(sizeConfig.isParallelCompilation()).isTrue();

        final Engine engine = runtime.createEngine(sizeConfig);
        registerForCleanup(engine);

        assertThat(engine.isValid()).isTrue();
        final EngineConfig retrievedConfig = engine.getConfig();
        assertThat(retrievedConfig.getOptimizationLevel()).isEqualTo(OptimizationLevel.SIZE);
        assertThat(retrievedConfig.isParallelCompilation()).isTrue();

        addTestMetric("Created size-optimized engine on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should create debug configuration correctly")
    void shouldCreateDebugConfigurationCorrectly() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing debug configuration on " + runtimeType);

        final EngineConfig debugConfig = EngineConfig.forDebug();
        assertThat(debugConfig.isDebugInfo()).isTrue();
        assertThat(debugConfig.getOptimizationLevel()).isEqualTo(OptimizationLevel.NONE);
        assertThat(debugConfig.isCraneliftDebugVerifier()).isTrue();

        final Engine engine = runtime.createEngine(debugConfig);
        registerForCleanup(engine);

        assertThat(engine.isValid()).isTrue();
        final EngineConfig retrievedConfig = engine.getConfig();
        assertThat(retrievedConfig.isDebugInfo()).isTrue();
        assertThat(retrievedConfig.getOptimizationLevel()).isEqualTo(OptimizationLevel.NONE);
        assertThat(retrievedConfig.isCraneliftDebugVerifier()).isTrue();

        addTestMetric("Created debug engine on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should validate factory method independence")
    void shouldValidateFactoryMethodIndependence() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing factory method independence on " + runtimeType);

        // Create multiple configurations using factory methods
        final EngineConfig speedConfig1 = EngineConfig.forSpeed();
        final EngineConfig speedConfig2 = EngineConfig.forSpeed();
        final EngineConfig sizeConfig = EngineConfig.forSize();
        final EngineConfig debugConfig = EngineConfig.forDebug();

        // Verify each configuration is independent
        assertThat(speedConfig1).isNotSameAs(speedConfig2);
        assertThat(speedConfig1.getOptimizationLevel()).isEqualTo(speedConfig2.getOptimizationLevel());

        // Modify one configuration
        speedConfig1.debugInfo(true);
        
        // Verify other configurations are not affected
        assertThat(speedConfig2.isDebugInfo()).isFalse();
        assertThat(sizeConfig.isDebugInfo()).isFalse();
        assertThat(debugConfig.isDebugInfo()).isTrue(); // This one should naturally have debug info

        // Create engines from each configuration
        final List<Engine> engines = Arrays.asList(
            runtime.createEngine(speedConfig1),
            runtime.createEngine(speedConfig2),
            runtime.createEngine(sizeConfig),
            runtime.createEngine(debugConfig)
        );

        engines.forEach(this::registerForCleanup);

        // Verify all engines are valid and configured correctly
        engines.forEach(engine -> assertThat(engine.isValid()).isTrue());

        addTestMetric("Validated factory method independence on " + runtimeType);
      });
    }
  }

  @Nested
  @DisplayName("Configuration Validation Tests")
  final class ConfigurationValidationTests {

    @Test
    @DisplayName("Should validate all optimization levels")
    void shouldValidateAllOptimizationLevels() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing all optimization levels on " + runtimeType);

        final OptimizationLevel[] levels = OptimizationLevel.values();
        final List<Engine> engines = new ArrayList<>();

        for (final OptimizationLevel level : levels) {
          final EngineConfig config = new EngineConfig().optimizationLevel(level);
          final Engine engine = runtime.createEngine(config);
          engines.add(engine);
          registerForCleanup(engine);

          assertThat(engine.isValid()).isTrue();
          assertThat(engine.getConfig().getOptimizationLevel()).isEqualTo(level);
          
          LOGGER.fine("Successfully created engine with " + level + " on " + runtimeType);
        }

        // Test compilation behavior with different optimization levels
        final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
        
        for (int i = 0; i < engines.size(); i++) {
          final Engine engine = engines.get(i);
          final OptimizationLevel level = levels[i];
          
          measureExecutionTime("Module compilation (" + level + ") on " + runtimeType, () -> {
            try {
              final ai.tegmentum.wasmtime4j.Module module = engine.compileModule(wasmBytes);
              registerForCleanup(module);
              assertThat(module.isValid()).isTrue();
            } catch (final WasmException e) {
              throw new RuntimeException(e);
            }
          });
        }

        addTestMetric("Tested " + levels.length + " optimization levels on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should reject invalid configuration parameters")
    void shouldRejectInvalidConfigurationParameters() {
      LOGGER.info("Testing invalid configuration parameter rejection");

      // Test null optimization level
      assertThatThrownBy(() -> new EngineConfig().optimizationLevel(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Optimization level cannot be null");

      // Test configuration method chaining with invalid values
      final EngineConfig config = new EngineConfig();
      
      // These should not throw as they accept boolean values
      config.debugInfo(true).debugInfo(false);
      config.consumeFuel(true).consumeFuel(false);
      config.parallelCompilation(true).parallelCompilation(false);
      config.craneliftDebugVerifier(true).craneliftDebugVerifier(false);

      addTestMetric("Validated invalid parameter rejection");
    }

    @Test
    @DisplayName("Should validate configuration immutability after engine creation")
    void shouldValidateConfigurationImmutabilityAfterEngineCreation() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing configuration immutability after engine creation on " + runtimeType);

        // Create mutable configuration
        final EngineConfig originalConfig = new EngineConfig()
            .debugInfo(false)
            .consumeFuel(false)
            .optimizationLevel(OptimizationLevel.SPEED)
            .parallelCompilation(true)
            .craneliftDebugVerifier(false);

        // Create engine from configuration
        final Engine engine = runtime.createEngine(originalConfig);
        registerForCleanup(engine);

        // Get configuration from engine
        final EngineConfig engineConfig = engine.getConfig();
        
        // Verify initial state
        assertThat(engineConfig.isDebugInfo()).isFalse();
        assertThat(engineConfig.isConsumeFuel()).isFalse();
        assertThat(engineConfig.getOptimizationLevel()).isEqualTo(OptimizationLevel.SPEED);
        assertThat(engineConfig.isParallelCompilation()).isTrue();
        assertThat(engineConfig.isCraneliftDebugVerifier()).isFalse();

        // Modify original configuration
        originalConfig
            .debugInfo(true)
            .consumeFuel(true)
            .optimizationLevel(OptimizationLevel.SIZE)
            .parallelCompilation(false)
            .craneliftDebugVerifier(true);

        // Verify engine configuration remains unchanged
        final EngineConfig unchangedConfig = engine.getConfig();
        assertThat(unchangedConfig.isDebugInfo()).isFalse();
        assertThat(unchangedConfig.isConsumeFuel()).isFalse();
        assertThat(unchangedConfig.getOptimizationLevel()).isEqualTo(OptimizationLevel.SPEED);
        assertThat(unchangedConfig.isParallelCompilation()).isTrue();
        assertThat(unchangedConfig.isCraneliftDebugVerifier()).isFalse();

        addTestMetric("Validated configuration immutability on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should validate WebAssembly feature flag consistency")
    void shouldValidateWebAssemblyFeatureFlagConsistency() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing WebAssembly feature flag consistency on " + runtimeType);

        final EngineConfig config = new EngineConfig();
        final Engine engine = runtime.createEngine(config);
        registerForCleanup(engine);

        final EngineConfig retrievedConfig = engine.getConfig();
        
        // Verify default WebAssembly feature settings are consistent
        final Map<String, Boolean> features = new HashMap<>();
        features.put("ReferenceTypes", retrievedConfig.isWasmReferenceTypes());
        features.put("SIMD", retrievedConfig.isWasmSimd());
        features.put("RelaxedSIMD", retrievedConfig.isWasmRelaxedSimd());
        features.put("MultiValue", retrievedConfig.isWasmMultiValue());
        features.put("BulkMemory", retrievedConfig.isWasmBulkMemory());
        features.put("Threads", retrievedConfig.isWasmThreads());
        features.put("TailCall", retrievedConfig.isWasmTailCall());
        features.put("MultiMemory", retrievedConfig.isWasmMultiMemory());
        features.put("Memory64", retrievedConfig.isWasmMemory64());

        // Log feature status for debugging
        features.forEach((feature, enabled) -> 
            LOGGER.fine("WebAssembly " + feature + ": " + enabled + " on " + runtimeType));

        // Verify commonly enabled features
        assertThat(retrievedConfig.isWasmReferenceTypes()).isTrue();
        assertThat(retrievedConfig.isWasmSimd()).isTrue();
        assertThat(retrievedConfig.isWasmMultiValue()).isTrue();
        assertThat(retrievedConfig.isWasmBulkMemory()).isTrue();

        // Verify advanced features are typically disabled by default
        assertThat(retrievedConfig.isWasmThreads()).isFalse();
        assertThat(retrievedConfig.isWasmTailCall()).isFalse();
        assertThat(retrievedConfig.isWasmMultiMemory()).isFalse();
        assertThat(retrievedConfig.isWasmMemory64()).isFalse();

        addTestMetric("Validated " + features.size() + " WebAssembly features on " + runtimeType);
      });
    }
  }

  @Nested
  @DisplayName("Configuration Performance Impact Tests")
  final class ConfigurationPerformanceImpactTests {

    @Test
    @DisplayName("Should measure performance impact of optimization levels")
    void shouldMeasurePerformanceImpactOfOptimizationLevels() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Measuring optimization level performance impact on " + runtimeType);

        final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
        final PerformanceTestHarness.Configuration config = PerformanceTestHarness.getFastConfiguration();
        final List<PerformanceTestHarness.MeasurementResult> results = new ArrayList<>();

        // Benchmark each optimization level
        for (final OptimizationLevel level : OptimizationLevel.values()) {
          final Engine engine = runtime.createEngine(new EngineConfig().optimizationLevel(level));
          registerForCleanup(engine);

          final PerformanceTestHarness.MeasurementResult result = 
              PerformanceTestHarness.runBenchmark(
                  "Compilation (" + level + ") - " + runtimeType,
                  () -> {
                    try {
                      final ai.tegmentum.wasmtime4j.Module module = engine.compileModule(wasmBytes);
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
        LOGGER.info("Optimization level performance impact for " + runtimeType + ":\n" + report);

        // Compare optimization levels
        if (results.size() >= 2) {
          final PerformanceTestHarness.MeasurementResult noneResult = results.stream()
              .filter(r -> r.getTestName().contains("NONE"))
              .findFirst()
              .orElse(results.get(0));
          
          final PerformanceTestHarness.MeasurementResult speedResult = results.stream()
              .filter(r -> r.getTestName().contains("SPEED"))
              .findFirst()
              .orElse(results.get(results.size() - 1));

          if (noneResult != speedResult) {
            final PerformanceTestHarness.ComparisonResult comparison = 
                new PerformanceTestHarness.ComparisonResult(noneResult, speedResult);
            
            final String comparisonReport = PerformanceTestHarness.generateComparisonReport(comparison);
            LOGGER.info("NONE vs SPEED optimization comparison:\n" + comparisonReport);
          }
        }

        addTestMetric("Measured performance impact of " + OptimizationLevel.values().length + 
                     " optimization levels on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should measure performance impact of parallel compilation")
    void shouldMeasurePerformanceImpactOfParallelCompilation() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Measuring parallel compilation performance impact on " + runtimeType);

        final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
        final PerformanceTestHarness.Configuration config = PerformanceTestHarness.getFastConfiguration();

        // Benchmark without parallel compilation
        final Engine serialEngine = runtime.createEngine(
            new EngineConfig().parallelCompilation(false));
        registerForCleanup(serialEngine);

        final PerformanceTestHarness.MeasurementResult serialResult = 
            PerformanceTestHarness.runBenchmark(
                "Serial Compilation - " + runtimeType,
                () -> {
                  try {
                    final ai.tegmentum.wasmtime4j.Module module = serialEngine.compileModule(wasmBytes);
                    registerForCleanup(module);
                    assertThat(module.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                config);

        // Benchmark with parallel compilation
        final Engine parallelEngine = runtime.createEngine(
            new EngineConfig().parallelCompilation(true));
        registerForCleanup(parallelEngine);

        final PerformanceTestHarness.MeasurementResult parallelResult = 
            PerformanceTestHarness.runBenchmark(
                "Parallel Compilation - " + runtimeType,
                () -> {
                  try {
                    final ai.tegmentum.wasmtime4j.Module module = parallelEngine.compileModule(wasmBytes);
                    registerForCleanup(module);
                    assertThat(module.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                config);

        // Compare results
        final PerformanceTestHarness.ComparisonResult comparison = 
            new PerformanceTestHarness.ComparisonResult(serialResult, parallelResult);

        final String comparisonReport = PerformanceTestHarness.generateComparisonReport(comparison);
        LOGGER.info("Serial vs Parallel compilation comparison for " + runtimeType + ":\n" + comparisonReport);

        // For simple modules, parallel compilation might not show benefits
        // Just verify both approaches work and produce reasonable results
        assertThat(serialResult.getMean()).isGreaterThan(0);
        assertThat(parallelResult.getMean()).isGreaterThan(0);

        addTestMetric("Measured parallel compilation impact on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should measure performance impact of debug features")
    void shouldMeasurePerformanceImpactOfDebugFeatures() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Measuring debug features performance impact on " + runtimeType);

        final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
        final PerformanceTestHarness.Configuration config = PerformanceTestHarness.getFastConfiguration();

        // Benchmark production configuration
        final Engine prodEngine = runtime.createEngine(EngineConfig.forSpeed());
        registerForCleanup(prodEngine);

        final PerformanceTestHarness.MeasurementResult prodResult = 
            PerformanceTestHarness.runBenchmark(
                "Production Configuration - " + runtimeType,
                () -> {
                  try {
                    final ai.tegmentum.wasmtime4j.Module module = prodEngine.compileModule(wasmBytes);
                    registerForCleanup(module);
                    assertThat(module.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                config);

        // Benchmark debug configuration
        final Engine debugEngine = runtime.createEngine(EngineConfig.forDebug());
        registerForCleanup(debugEngine);

        final PerformanceTestHarness.MeasurementResult debugResult = 
            PerformanceTestHarness.runBenchmark(
                "Debug Configuration - " + runtimeType,
                () -> {
                  try {
                    final ai.tegmentum.wasmtime4j.Module module = debugEngine.compileModule(wasmBytes);
                    registerForCleanup(module);
                    assertThat(module.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                config);

        // Compare results
        final PerformanceTestHarness.ComparisonResult comparison = 
            new PerformanceTestHarness.ComparisonResult(prodResult, debugResult);

        final String comparisonReport = PerformanceTestHarness.generateComparisonReport(comparison);
        LOGGER.info("Production vs Debug configuration comparison for " + runtimeType + ":\n" + comparisonReport);

        // Debug configuration typically slower than production
        // But both should produce reasonable results
        assertThat(prodResult.getMean()).isGreaterThan(0);
        assertThat(debugResult.getMean()).isGreaterThan(0);

        addTestMetric("Measured debug features impact on " + runtimeType);
      });
    }
  }

  @Nested
  @DisplayName("Configuration Concurrency Tests")
  final class ConfigurationConcurrencyTests {

    @Test
    @DisplayName("Should handle concurrent engine creation with different configurations")
    void shouldHandleConcurrentEngineCreationWithDifferentConfigurations() throws Exception {
      skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing concurrent engine creation with different configurations on " + runtimeType);

        final int threadCount = 4;
        final int enginesPerThread = 5;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
          final List<CompletableFuture<List<Engine>>> futures = new ArrayList<>();

          for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            final CompletableFuture<List<Engine>> future = CompletableFuture.supplyAsync(() -> {
              final List<Engine> engines = new ArrayList<>();
              
              for (int e = 0; e < enginesPerThread; e++) {
                try {
                  // Create different configurations per thread
                  final EngineConfig config;
                  switch (threadId % 4) {
                    case 0:
                      config = EngineConfig.forSpeed();
                      break;
                    case 1:
                      config = EngineConfig.forSize();
                      break;
                    case 2:
                      config = EngineConfig.forDebug();
                      break;
                    default:
                      config = new EngineConfig()
                          .debugInfo(e % 2 == 0)
                          .consumeFuel(e % 3 == 0)
                          .optimizationLevel(OptimizationLevel.values()[e % OptimizationLevel.values().length]);
                      break;
                  }

                  final Engine engine = runtime.createEngine(config);
                  engines.add(engine);
                  
                  // Verify engine is properly configured
                  assertThat(engine.isValid()).isTrue();
                  final EngineConfig retrievedConfig = engine.getConfig();
                  assertThat(retrievedConfig).isNotNull();
                  
                } catch (final WasmException ex) {
                  throw new RuntimeException("Thread " + threadId + " engine " + e + " failed", ex);
                }
              }
              
              return engines;
            }, executor);
            futures.add(future);
          }

          // Wait for all threads to complete and collect engines
          final List<Engine> allEngines = new ArrayList<>();
          for (final CompletableFuture<List<Engine>> future : futures) {
            final List<Engine> threadEngines = future.get(30, TimeUnit.SECONDS);
            allEngines.addAll(threadEngines);
          }

          // Verify all engines were created successfully
          assertThat(allEngines).hasSize(threadCount * enginesPerThread);
          for (final Engine engine : allEngines) {
            assertThat(engine.isValid()).isTrue();
            registerForCleanup(engine); // Register for cleanup
          }

          addTestMetric("Created " + allEngines.size() + " engines concurrently on " + runtimeType);
        } finally {
          executor.shutdown();
          if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow();
          }
        }
      });
    }

    @Test
    @DisplayName("Should handle configuration stress testing")
    void shouldHandleConfigurationStressTesting() throws Exception {
      skipIfCategoryNotEnabled(TestCategories.STRESS);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Running configuration stress testing on " + runtimeType);

        executeWithMemoryMonitoring("Configuration stress test on " + runtimeType, () -> {
          final int iterationCount = 100;
          final List<Engine> engines = new ArrayList<>();

          for (int i = 0; i < iterationCount; i++) {
            try {
              // Create varied configurations
              final EngineConfig config = new EngineConfig()
                  .debugInfo(i % 2 == 0)
                  .consumeFuel(i % 3 == 0)
                  .optimizationLevel(OptimizationLevel.values()[i % OptimizationLevel.values().length])
                  .parallelCompilation(i % 4 != 0)
                  .craneliftDebugVerifier(i % 5 == 0);

              final Engine engine = runtime.createEngine(config);
              engines.add(engine);

              // Occasionally test the engine with module compilation
              if (i % 10 == 0) {
                final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
                final ai.tegmentum.wasmtime4j.Module module = engine.compileModule(wasmBytes);
                assertThat(module.isValid()).isTrue();
                module.close();
              }

            } catch (final WasmException e) {
              throw new RuntimeException("Stress test iteration " + i + " failed", e);
            }
          }

          // Verify all engines are valid
          for (final Engine engine : engines) {
            assertThat(engine.isValid()).isTrue();
          }

          // Clean up all engines
          for (final Engine engine : engines) {
            engine.close();
          }
        });

        addTestMetric("Completed configuration stress test with 100 iterations on " + runtimeType);
      });
    }
  }

  @Nested
  @DisplayName("Configuration Cross-Runtime Validation Tests")
  final class ConfigurationCrossRuntimeValidationTests {

    @Test
    @DisplayName("Should validate identical configuration behavior across runtimes")
    void shouldValidateIdenticalConfigurationBehaviorAcrossRuntimes() {
      skipIfPanamaNotAvailable();

      LOGGER.info("Validating cross-runtime configuration behavior");

      // Test default configuration
      final CrossRuntimeValidator.ComparisonResult defaultResult = 
          CrossRuntimeValidator.validateCrossRuntime(runtime -> {
            final Engine engine = runtime.createEngine();
            final EngineConfig config = engine.getConfig();
            engine.close();
            
            return Arrays.asList(
                config.getOptimizationLevel(),
                config.isParallelCompilation(),
                config.isWasmReferenceTypes(),
                config.isWasmSimd(),
                config.isWasmBulkMemory(),
                config.isWasmMultiValue()
            );
          });

      assertThat(defaultResult.isValid()).isTrue();
      assertThat(defaultResult.areResultsIdentical()).isTrue();

      // Test speed configuration
      final CrossRuntimeValidator.ComparisonResult speedResult = 
          CrossRuntimeValidator.validateCrossRuntime(runtime -> {
            final Engine engine = runtime.createEngine(EngineConfig.forSpeed());
            final EngineConfig config = engine.getConfig();
            engine.close();
            
            return Arrays.asList(
                config.getOptimizationLevel(),
                config.isParallelCompilation()
            );
          });

      assertThat(speedResult.isValid()).isTrue();
      assertThat(speedResult.areResultsIdentical()).isTrue();

      // Test debug configuration
      final CrossRuntimeValidator.ComparisonResult debugResult = 
          CrossRuntimeValidator.validateCrossRuntime(runtime -> {
            final Engine engine = runtime.createEngine(EngineConfig.forDebug());
            final EngineConfig config = engine.getConfig();
            engine.close();
            
            return Arrays.asList(
                config.isDebugInfo(),
                config.getOptimizationLevel(),
                config.isCraneliftDebugVerifier()
            );
          });

      assertThat(debugResult.isValid()).isTrue();
      assertThat(debugResult.areResultsIdentical()).isTrue();

      LOGGER.info("All cross-runtime configuration validations passed");
      addTestMetric("Completed comprehensive cross-runtime configuration validation");
    }

    @Test
    @DisplayName("Should validate configuration performance consistency across runtimes")
    void shouldValidateConfigurationPerformanceConsistencyAcrossRuntimes() {
      skipIfPanamaNotAvailable();
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      LOGGER.info("Validating cross-runtime configuration performance consistency");

      final PerformanceTestHarness.Configuration config = PerformanceTestHarness.getFastConfiguration();
      final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

      // Compare speed configuration performance
      final PerformanceTestHarness.ComparisonResult speedComparison = 
          PerformanceTestHarness.runCrossRuntimeBenchmark(
              "Speed Configuration Cross-Runtime",
              runtime -> {
                final Engine engine = runtime.createEngine(EngineConfig.forSpeed());
                final ai.tegmentum.wasmtime4j.Module module = engine.compileModule(wasmBytes);
                registerForCleanup(module);
                registerForCleanup(engine);
                assertThat(module.isValid()).isTrue();
              },
              config);

      // Compare debug configuration performance
      final PerformanceTestHarness.ComparisonResult debugComparison = 
          PerformanceTestHarness.runCrossRuntimeBenchmark(
              "Debug Configuration Cross-Runtime",
              runtime -> {
                final Engine engine = runtime.createEngine(EngineConfig.forDebug());
                final ai.tegmentum.wasmtime4j.Module module = engine.compileModule(wasmBytes);
                registerForCleanup(module);
                registerForCleanup(engine);
                assertThat(module.isValid()).isTrue();
              },
              config);

      // Generate performance comparison reports
      final String speedReport = PerformanceTestHarness.generateComparisonReport(speedComparison);
      final String debugReport = PerformanceTestHarness.generateComparisonReport(debugComparison);
      
      LOGGER.info("Cross-runtime speed configuration performance:\n" + speedReport);
      LOGGER.info("Cross-runtime debug configuration performance:\n" + debugReport);

      // Validate reasonable performance ratios
      assertThat(speedComparison.getSpeedupRatio()).isGreaterThan(0.1);
      assertThat(speedComparison.getSpeedupRatio()).isLessThan(10.0);
      assertThat(debugComparison.getSpeedupRatio()).isGreaterThan(0.1);
      assertThat(debugComparison.getSpeedupRatio()).isLessThan(10.0);

      LOGGER.info("Cross-runtime configuration performance validation passed");
      addTestMetric("Validated cross-runtime configuration performance consistency");
    }

    @Test
    @DisplayName("Should validate comprehensive configuration matrix across runtimes")
    void shouldValidateComprehensiveConfigurationMatrixAcrossRuntimes() {
      skipIfPanamaNotAvailable();

      LOGGER.info("Validating comprehensive configuration matrix across runtimes");

      // Test multiple configuration combinations
      final List<EngineConfig> configurations = Arrays.asList(
          new EngineConfig(), // Default
          EngineConfig.forSpeed(),
          EngineConfig.forSize(),
          EngineConfig.forDebug(),
          new EngineConfig().debugInfo(true).consumeFuel(true),
          new EngineConfig().parallelCompilation(false).optimizationLevel(OptimizationLevel.NONE),
          new EngineConfig().consumeFuel(true).optimizationLevel(OptimizationLevel.SPEED_AND_SIZE)
      );

      for (int i = 0; i < configurations.size(); i++) {
        final EngineConfig testConfig = configurations.get(i);
        final int configIndex = i;
        
        final CrossRuntimeValidator.ComparisonResult result = 
            CrossRuntimeValidator.validateCrossRuntime(runtime -> {
              final Engine engine = runtime.createEngine(testConfig);
              final EngineConfig retrievedConfig = engine.getConfig();
              engine.close();
              
              return Arrays.asList(
                  retrievedConfig.isDebugInfo(),
                  retrievedConfig.isConsumeFuel(),
                  retrievedConfig.getOptimizationLevel(),
                  retrievedConfig.isParallelCompilation(),
                  retrievedConfig.isCraneliftDebugVerifier()
              );
            });

        assertThat(result.isValid())
            .as("Configuration " + configIndex + " should be identical across runtimes")
            .isTrue();
        assertThat(result.areResultsIdentical())
            .as("Configuration " + configIndex + " results should be identical across runtimes")
            .isTrue();
        
        LOGGER.fine("Configuration " + configIndex + " validated across runtimes");
      }

      addTestMetric("Validated " + configurations.size() + " configurations across runtimes");
    }
  }
}
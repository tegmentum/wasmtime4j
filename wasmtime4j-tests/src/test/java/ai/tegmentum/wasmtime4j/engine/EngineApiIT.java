package ai.tegmentum.wasmtime4j.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.time.Duration;
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
 * Comprehensive integration tests for Engine API functionality. Tests engine creation,
 * configuration, lifecycle management, and cross-runtime validation.
 */
@DisplayName("Engine API Integration Tests")
final class EngineApiIT extends BaseIntegrationTest {

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    skipIfCategoryNotEnabled(TestCategories.ENGINE);
  }

  @Nested
  @DisplayName("Engine Creation Tests")
  final class EngineCreationTests {

    @Test
    @DisplayName("Should create engine with default configuration")
    void shouldCreateEngineWithDefaultConfiguration() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing engine creation with default config on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            assertThat(engine).isNotNull();
            assertThat(engine.isValid()).isTrue();
            assertThat(engine.getConfig()).isNotNull();

            addTestMetric("Created engine with default config on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should create engine with custom configuration")
    void shouldCreateEngineWithCustomConfiguration() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing engine creation with custom config on " + runtimeType);

            final EngineConfig config =
                new EngineConfig()
                    .debugInfo(true)
                    .consumeFuel(true)
                    .optimizationLevel(OptimizationLevel.SIZE)
                    .parallelCompilation(false);

            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            assertThat(engine).isNotNull();
            assertThat(engine.isValid()).isTrue();
            assertThat(engine.getConfig()).isNotNull();
            assertThat(engine.getConfig().isDebugInfo()).isTrue();
            assertThat(engine.getConfig().isConsumeFuel()).isTrue();
            assertThat(engine.getConfig().getOptimizationLevel()).isEqualTo(OptimizationLevel.SIZE);
            assertThat(engine.getConfig().isParallelCompilation()).isFalse();

            addTestMetric("Created engine with custom config on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should create engine with speed-optimized configuration")
    void shouldCreateEngineWithSpeedConfiguration() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing engine creation with speed config on " + runtimeType);

            final Engine engine = runtime.createEngine(EngineConfig.forSpeed());
            registerForCleanup(engine);

            assertThat(engine).isNotNull();
            assertThat(engine.isValid()).isTrue();
            assertThat(engine.getConfig().getOptimizationLevel())
                .isEqualTo(OptimizationLevel.SPEED);
            assertThat(engine.getConfig().isParallelCompilation()).isTrue();

            addTestMetric("Created speed-optimized engine on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should create engine with size-optimized configuration")
    void shouldCreateEngineWithSizeConfiguration() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing engine creation with size config on " + runtimeType);

            final Engine engine = runtime.createEngine(EngineConfig.forSize());
            registerForCleanup(engine);

            assertThat(engine).isNotNull();
            assertThat(engine.isValid()).isTrue();
            assertThat(engine.getConfig().getOptimizationLevel()).isEqualTo(OptimizationLevel.SIZE);
            assertThat(engine.getConfig().isParallelCompilation()).isTrue();

            addTestMetric("Created size-optimized engine on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should create engine with debug configuration")
    void shouldCreateEngineWithDebugConfiguration() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing engine creation with debug config on " + runtimeType);

            final Engine engine = runtime.createEngine(EngineConfig.forDebug());
            registerForCleanup(engine);

            assertThat(engine).isNotNull();
            assertThat(engine.isValid()).isTrue();
            assertThat(engine.getConfig().isDebugInfo()).isTrue();
            assertThat(engine.getConfig().getOptimizationLevel()).isEqualTo(OptimizationLevel.NONE);
            assertThat(engine.getConfig().isCraneliftDebugVerifier()).isTrue();

            addTestMetric("Created debug engine on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should reject null configuration")
    void shouldRejectNullConfiguration() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing null config rejection on " + runtimeType);

            assertThatThrownBy(() -> runtime.createEngine(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("configuration cannot be null");

            addTestMetric("Rejected null config on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Engine Configuration Tests")
  final class EngineConfigurationTests {

    @Test
    @DisplayName("Should validate all engine configuration options")
    void shouldValidateAllEngineConfigurationOptions() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing all configuration options on " + runtimeType);

            final EngineConfig config =
                new EngineConfig()
                    .debugInfo(true)
                    .consumeFuel(true)
                    .optimizationLevel(OptimizationLevel.SPEED_AND_SIZE)
                    .parallelCompilation(true)
                    .craneliftDebugVerifier(true);

            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final EngineConfig retrievedConfig = engine.getConfig();
            assertThat(retrievedConfig).isNotNull();
            assertThat(retrievedConfig.isDebugInfo()).isTrue();
            assertThat(retrievedConfig.isConsumeFuel()).isTrue();
            assertThat(retrievedConfig.getOptimizationLevel())
                .isEqualTo(OptimizationLevel.SPEED_AND_SIZE);
            assertThat(retrievedConfig.isParallelCompilation()).isTrue();
            assertThat(retrievedConfig.isCraneliftDebugVerifier()).isTrue();

            addTestMetric("Validated all config options on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle optimization level edge cases")
    void shouldHandleOptimizationLevelEdgeCases() throws WasmException {
      final OptimizationLevel[] levels = OptimizationLevel.values();

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing optimization levels on " + runtimeType);

            for (final OptimizationLevel level : levels) {
              final Engine engine =
                  runtime.createEngine(new EngineConfig().optimizationLevel(level));
              registerForCleanup(engine);

              assertThat(engine.getConfig().getOptimizationLevel()).isEqualTo(level);
              LOGGER.fine("Successfully created engine with " + level + " on " + runtimeType);
            }

            addTestMetric("Tested " + levels.length + " optimization levels on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should reject invalid optimization level")
    void shouldRejectInvalidOptimizationLevel() {
      assertThatThrownBy(() -> new EngineConfig().optimizationLevel(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Optimization level cannot be null");
    }
  }

  @Nested
  @DisplayName("Engine Lifecycle Tests")
  final class EngineLifecycleTests {

    @Test
    @DisplayName("Should properly close engine")
    void shouldProperlyCloseEngine() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing engine closure on " + runtimeType);

            final Engine engine = runtime.createEngine();
            assertThat(engine.isValid()).isTrue();

            engine.close();
            assertThat(engine.isValid()).isFalse();

            // Multiple closes should be safe
            engine.close();
            assertThat(engine.isValid()).isFalse();

            addTestMetric("Validated engine closure on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should reject operations on closed engine")
    void shouldRejectOperationsOnClosedEngine() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing closed engine operations rejection on " + runtimeType);

            final Engine engine = runtime.createEngine();
            engine.close();

            // Should reject store creation
            assertThatThrownBy(() -> engine.createStore())
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("Engine is closed");

            // Should reject module compilation
            final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
            assertThatThrownBy(() -> engine.compileModule(wasmBytes))
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("Engine is closed");

            addTestMetric("Rejected operations on closed engine on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Engine Module Compilation Tests")
  final class EngineModuleCompilationTests {

    @Test
    @DisplayName("Should compile valid WebAssembly module")
    void shouldCompileValidWebAssemblyModule() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing module compilation on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
            final Module module =
                measureExecutionTime(
                    "Module compilation on " + runtimeType,
                    () -> {
                      try {
                        return engine.compileModule(wasmBytes);
                      } catch (final WasmException e) {
                        throw new RuntimeException(e);
                      }
                    });
            registerForCleanup(module);

            assertThat(module).isNotNull();
            assertThat(module.isValid()).isTrue();
          });
    }

    @Test
    @DisplayName("Should reject null WebAssembly bytes")
    void shouldRejectNullWebAssemblyBytes() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing null bytes rejection on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            assertThatThrownBy(() -> engine.compileModule(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WebAssembly bytes cannot be null");

            addTestMetric("Rejected null bytes on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should reject empty WebAssembly bytes")
    void shouldRejectEmptyWebAssemblyBytes() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing empty bytes rejection on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            assertThatThrownBy(() -> engine.compileModule(new byte[0]))
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("empty");

            addTestMetric("Rejected empty bytes on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should reject invalid WebAssembly bytes")
    void shouldRejectInvalidWebAssemblyBytes() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing invalid bytes rejection on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final byte[] invalidBytes = "invalid wasm".getBytes();
            assertThatThrownBy(() -> engine.compileModule(invalidBytes))
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("compilation failed");

            addTestMetric("Rejected invalid bytes on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Engine Store Creation Tests")
  final class EngineStoreCreationTests {

    @Test
    @DisplayName("Should create store without custom data")
    void shouldCreateStoreWithoutCustomData() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store creation without data on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            assertThat(store).isNotNull();
            assertThat(store.isValid()).isTrue();
            assertThat(store.getEngine()).isSameAs(engine);
            assertThat(store.getData()).isNull();

            addTestMetric("Created store without data on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should create store with custom data")
    void shouldCreateStoreWithCustomData() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store creation with data on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final String customData = "test-data-" + runtimeType;
            final Store store = engine.createStore(customData);
            registerForCleanup(store);

            assertThat(store).isNotNull();
            assertThat(store.isValid()).isTrue();
            assertThat(store.getEngine()).isSameAs(engine);
            assertThat(store.getData()).isEqualTo(customData);

            addTestMetric("Created store with data on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should create multiple stores from same engine")
    void shouldCreateMultipleStoresFromSameEngine() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing multiple store creation on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final List<Store> stores = new ArrayList<>();
            final int storeCount = 5;

            for (int i = 0; i < storeCount; i++) {
              final Store store = engine.createStore("store-" + i);
              registerForCleanup(store);
              stores.add(store);
            }

            assertThat(stores).hasSize(storeCount);
            for (int i = 0; i < storeCount; i++) {
              final Store store = stores.get(i);
              assertThat(store.isValid()).isTrue();
              assertThat(store.getEngine()).isSameAs(engine);
              assertThat(store.getData()).isEqualTo("store-" + i);
            }

            addTestMetric("Created " + storeCount + " stores on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Engine Thread Safety Tests")
  final class EngineThreadSafetyTests {

    @Test
    @DisplayName("Should support concurrent module compilation")
    void shouldSupportConcurrentModuleCompilation() throws Exception {
      skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing concurrent module compilation on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final int threadCount = 4;
            final int operationsPerThread = 3;
            final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            final CountDownLatch latch = new CountDownLatch(threadCount * operationsPerThread);
            final List<CompletableFuture<Module>> futures = new ArrayList<>();

            try {
              for (int t = 0; t < threadCount; t++) {
                for (int op = 0; op < operationsPerThread; op++) {
                  final CompletableFuture<Module> future =
                      CompletableFuture.supplyAsync(
                          () -> {
                            try {
                              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
                              final Module module = engine.compileModule(wasmBytes);
                              latch.countDown();
                              return module;
                            } catch (final WasmException e) {
                              latch.countDown();
                              throw new RuntimeException(e);
                            }
                          },
                          executor);
                  futures.add(future);
                }
              }

              // Wait for all operations to complete
              assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();

              // Verify all modules were compiled successfully
              for (final CompletableFuture<Module> future : futures) {
                final Module module = future.get();
                assertThat(module).isNotNull();
                assertThat(module.isValid()).isTrue();
                registerForCleanup(module);
              }

              addTestMetric(
                  "Completed "
                      + (threadCount * operationsPerThread)
                      + " concurrent compilations on "
                      + runtimeType);
            } finally {
              executor.shutdown();
              if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
              }
            }
          });
    }

    @Test
    @DisplayName("Should support concurrent store creation")
    void shouldSupportConcurrentStoreCreation() throws Exception {
      skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing concurrent store creation on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final int threadCount = 4;
            final int storesPerThread = 5;
            final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            final CountDownLatch latch = new CountDownLatch(threadCount * storesPerThread);
            final List<CompletableFuture<Store>> futures = new ArrayList<>();

            try {
              for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                for (int s = 0; s < storesPerThread; s++) {
                  final int storeId = s;
                  final CompletableFuture<Store> future =
                      CompletableFuture.supplyAsync(
                          () -> {
                            try {
                              final Store store =
                                  engine.createStore("thread-" + threadId + "-store-" + storeId);
                              latch.countDown();
                              return store;
                            } catch (final WasmException e) {
                              latch.countDown();
                              throw new RuntimeException(e);
                            }
                          },
                          executor);
                  futures.add(future);
                }
              }

              // Wait for all operations to complete
              assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();

              // Verify all stores were created successfully
              for (final CompletableFuture<Store> future : futures) {
                final Store store = future.get();
                assertThat(store).isNotNull();
                assertThat(store.isValid()).isTrue();
                assertThat(store.getEngine()).isSameAs(engine);
                registerForCleanup(store);
              }

              addTestMetric(
                  "Completed "
                      + (threadCount * storesPerThread)
                      + " concurrent store creations on "
                      + runtimeType);
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
  @DisplayName("Cross-Runtime Validation Tests")
  final class CrossRuntimeValidationTests {

    @Test
    @DisplayName("Should produce identical engine creation results")
    void shouldProduceIdenticalEngineCreationResults() {
      skipIfPanamaNotAvailable();

      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final Engine engine = runtime.createEngine();
                final boolean isValid = engine.isValid();
                engine.close();
                return isValid;
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info("Cross-runtime engine creation validation: " + result.getDifferenceDescription());
    }

    @Test
    @DisplayName("Should produce identical module compilation results")
    void shouldProduceIdenticalModuleCompilationResults() {
      skipIfPanamaNotAvailable();

      final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final Engine engine = runtime.createEngine();
                final Module module = engine.compileModule(wasmBytes);
                final boolean isValid = module.isValid();
                module.close();
                engine.close();
                return isValid;
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info(
          "Cross-runtime module compilation validation: " + result.getDifferenceDescription());
    }

    @Test
    @DisplayName("Should produce identical store creation results")
    void shouldProduceIdenticalStoreCreationResults() {
      skipIfPanamaNotAvailable();

      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final Engine engine = runtime.createEngine();
                final Store store = engine.createStore("test-data");
                final Object data = store.getData();
                store.close();
                engine.close();
                return data;
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info("Cross-runtime store creation validation: " + result.getDifferenceDescription());
    }
  }

  @Nested
  @DisplayName("Engine Performance Tests")
  final class EnginePerformanceTests {

    @Test
    @DisplayName("Should meet engine creation performance baseline")
    void shouldMeetEngineCreationPerformanceBaseline() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing engine creation performance on " + runtimeType);

            final Duration maxCreationTime = Duration.ofSeconds(2);

            assertExecutionTime(
                maxCreationTime,
                () -> {
                  try {
                    final Engine engine = runtime.createEngine();
                    registerForCleanup(engine);
                    assertThat(engine.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                "Engine creation on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should meet module compilation performance baseline")
    void shouldMeetModuleCompilationPerformanceBaseline() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing module compilation performance on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Duration maxCompilationTime = Duration.ofSeconds(5);

            assertExecutionTime(
                maxCompilationTime,
                () -> {
                  try {
                    final Module module = engine.compileModule(wasmBytes);
                    registerForCleanup(module);
                    assertThat(module.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                "Module compilation on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should meet store creation performance baseline")
    void shouldMeetStoreCreationPerformanceBaseline() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store creation performance on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Duration maxCreationTime = Duration.ofMillis(500);

            assertExecutionTime(
                maxCreationTime,
                () -> {
                  try {
                    final Store store = engine.createStore();
                    registerForCleanup(store);
                    assertThat(store.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                "Store creation on " + runtimeType);
          });
    }
  }
}

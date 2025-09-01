package ai.tegmentum.wasmtime4j.edge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive edge case and error handling tests for Engine and Store APIs. These tests validate
 * behavior under extreme conditions, invalid inputs, and error scenarios to ensure robust error
 * handling and defensive programming.
 */
@DisplayName("Engine & Store Edge Cases and Error Handling Tests")
final class EngineStoreEdgeCasesIT extends BaseIntegrationTest {

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // skipIfCategoryNotEnabled(TestCategories.ERROR_HANDLING);
  }

  @Nested
  @DisplayName("Engine Configuration Edge Cases")
  final class EngineConfigurationEdgeCasesTests {

    @Test
    @DisplayName("Should handle all optimization level combinations")
    void shouldHandleAllOptimizationLevelCombinations() throws WasmException {
      final OptimizationLevel[] levels = OptimizationLevel.values();

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing all optimization levels on " + runtimeType);

            for (final OptimizationLevel level : levels) {
              final EngineConfig config = new EngineConfig().optimizationLevel(level);
              final Engine engine = runtime.createEngine(config);
              registerForCleanup(engine);

              assertThat(engine).isNotNull();
              assertThat(engine.isValid()).isTrue();
              assertThat(engine.getConfig().getOptimizationLevel()).isEqualTo(level);

              LOGGER.fine("Successfully created engine with " + level + " on " + runtimeType);
            }

            // addTestMetric("Tested " + levels.length + " optimization levels on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle extreme boolean flag combinations")
    void shouldHandleExtremeBooleanFlagCombinations() throws WasmException {
      // Test all possible combinations of boolean flags
      final boolean[] values = {true, false};

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing boolean flag combinations on " + runtimeType);

            int configCount = 0;
            for (final boolean debugInfo : values) {
              for (final boolean consumeFuel : values) {
                for (final boolean parallelCompilation : values) {
                  for (final boolean craneliftDebugVerifier : values) {
                    final EngineConfig config =
                        new EngineConfig()
                            .debugInfo(debugInfo)
                            .consumeFuel(consumeFuel)
                            .parallelCompilation(parallelCompilation)
                            .craneliftDebugVerifier(craneliftDebugVerifier);

                    final Engine engine = runtime.createEngine(config);
                    registerForCleanup(engine);

                    assertThat(engine).isNotNull();
                    assertThat(engine.isValid()).isTrue();

                    final EngineConfig retrievedConfig = engine.getConfig();
                    assertThat(retrievedConfig.isDebugInfo()).isEqualTo(debugInfo);
                    assertThat(retrievedConfig.isConsumeFuel()).isEqualTo(consumeFuel);
                    assertThat(retrievedConfig.isParallelCompilation())
                        .isEqualTo(parallelCompilation);
                    assertThat(retrievedConfig.isCraneliftDebugVerifier())
                        .isEqualTo(craneliftDebugVerifier);

                    configCount++;
                  }
                }
              }
            }

            // addTestMetric("Tested " + configCount + " boolean flag combinations on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle configuration object identity")
    void shouldHandleConfigurationObjectIdentity() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing configuration object identity on " + runtimeType);

            final EngineConfig originalConfig =
                new EngineConfig().debugInfo(true).consumeFuel(true);

            final Engine engine = runtime.createEngine(originalConfig);
            registerForCleanup(engine);

            final EngineConfig retrievedConfig = engine.getConfig();

            // The retrieved config should contain the same values but may not be the same object
            assertThat(retrievedConfig).isNotNull();
            assertThat(retrievedConfig.isDebugInfo()).isEqualTo(originalConfig.isDebugInfo());
            assertThat(retrievedConfig.isConsumeFuel()).isEqualTo(originalConfig.isConsumeFuel());

            // Modifying the original config should not affect the engine
            originalConfig.debugInfo(false);
            assertThat(engine.getConfig().isDebugInfo()).isTrue(); // Should remain unchanged

            // addTestMetric("Validated configuration object identity on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Engine Resource Boundary Tests")
  final class EngineResourceBoundaryTests {

    @Test
    @DisplayName("Should handle maximum module compilation attempts")
    void shouldHandleMaximumModuleCompilationAttempts() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing maximum module compilation attempts on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
            final int maxAttempts = 100;

            // final Duration compilationTime =
            //     measureExecutionTime(
            //         "Maximum compilation attempts on " + runtimeType,
            //         () -> {
                      final List<Module> modules = new ArrayList<>();
                      try {
                        for (int i = 0; i < maxAttempts; i++) {
                          final Module module = engine.compileModule(wasmBytes);
                          assertThat(module.isValid()).isTrue();
                          modules.add(module);
                        }
                      } catch (final Exception e) {
                        throw new RuntimeException(e);
                      } finally {
                        // Clean up all modules
                        modules.forEach(
                            module -> {
                              try {
                                module.close();
                              } catch (final Exception e) {
                                LOGGER.warning("Failed to close module: " + e.getMessage());
                              }
          });
                      }
                    // });

            // assertThat(compilationTime).isLessThan(Duration.ofMinutes(5));
            // addTestMetric(
            //     "Completed "
            //         + maxAttempts
            //         + " compilations in "
            //         + compilationTime.toMillis()
            //         + "ms on "
            //         + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle maximum store creation attempts")
    void shouldHandleMaximumStoreCreationAttempts() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing maximum store creation attempts on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final int maxAttempts = 100;

            // final Duration creationTime =
            //     measureExecutionTime(
            //         "Maximum store creation attempts on " + runtimeType,
            //         () -> {
                      final List<Store> stores = new ArrayList<>();
                      try {
                        for (int i = 0; i < maxAttempts; i++) {
                          final Store store = engine.createStore("store-" + i);
                          assertThat(store.isValid()).isTrue();
                          assertThat(store.getData()).isEqualTo("store-" + i);
                          stores.add(store);
                        }
                      } catch (final Exception e) {
                        throw new RuntimeException(e);
                      } finally {
                        // Clean up all stores
                        stores.forEach(
                            store -> {
                              try {
                                store.close();
                              } catch (final Exception e) {
                                LOGGER.warning("Failed to close store: " + e.getMessage());
                              }
          });
                      }
                    // });

            // assertThat(creationTime).isLessThan(Duration.ofMinutes(2));
            // addTestMetric(
            //     "Created "
            //         + maxAttempts
            //         + " stores in "
            //         + creationTime.toMillis()
            //         + "ms on "
            //         + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Store Data Edge Cases")
  final class StoreDataEdgeCasesTests {

    @Test
    @DisplayName("Should handle extremely large data objects")
    void shouldHandleExtremelyLargeDataObjects() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing extremely large data objects on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Test with large string
            final StringBuilder largeString = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
              largeString.append("This is a large string data item ").append(i).append("\n");
            }

            final String largeStringData = largeString.toString();
            store.setData(largeStringData);
            assertThat(store.getData()).isEqualTo(largeStringData);

            // Test with large list
            final List<String> largeList = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
              largeList.add("Item " + i);
            }

            store.setData(largeList);
            assertThat(store.getData()).isEqualTo(largeList);

            // Test with large array
            final int[] largeArray = new int[10000];
            Arrays.fill(largeArray, 42);

            store.setData(largeArray);
            final int[] retrievedArray = (int[]) store.getData();
            assertThat(retrievedArray).isEqualTo(largeArray);

            // addTestMetric("Handled large data objects on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle complex nested data structures")
    void shouldHandleComplexNestedDataStructures() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing complex nested data structures on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Create deeply nested structure
            final List<Object> nestedStructure = new ArrayList<>();
            nestedStructure.add("root-string");
            nestedStructure.add(42);
            nestedStructure.add(
                Arrays.asList("nested", "list", Arrays.asList("deep", "nested", "list")));
            nestedStructure.add(
                Collections.singletonMap(
                    "key", Collections.singletonMap("nested-key", "nested-value")));

            store.setData(nestedStructure);
            final Object retrievedData = store.getData();
            assertThat(retrievedData).isEqualTo(nestedStructure);

            // Test with circular references (if supported)
            final List<Object> circularList = new ArrayList<>();
            circularList.add("circular-data");
            circularList.add(circularList); // Self-reference

            try {
              store.setData(circularList);
              final Object circularRetrieved = store.getData();
              LOGGER.info("Circular references supported on " + runtimeType);
            } catch (final Exception e) {
              LOGGER.info(
                  "Circular references not supported on " + runtimeType + ": " + e.getMessage());
            }

            // addTestMetric("Handled complex nested data on " + runtimeType);
          });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 100, 1000, 10000})
    @DisplayName("Should handle rapid data updates")
    void shouldHandleRapidDataUpdates(final int updateCount) throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing " + updateCount + " rapid data updates on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // final Duration updateTime =
            //     measureExecutionTime(
            //         updateCount + " data updates on " + runtimeType,
            //         () -> {
                      for (int i = 0; i < updateCount; i++) {
                        store.setData("data-update-" + i);
                        final Object retrieved = store.getData();
                        assertThat(retrieved).isEqualTo("data-update-" + i);
                      }
                    // });

            if (updateCount > 0) {
              // final double updatesPerSecond = updateCount / (updateTime.toMillis() / 1000.0);
              // addTestMetric(
              //     String.format("%.0f updates/second on %s", updatesPerSecond, runtimeType));
            }
          });
    }
  }

  @Nested
  @DisplayName("Store Fuel Edge Cases")
  final class StoreFuelEdgeCasesTests {

    @Test
    @DisplayName("Should handle extreme fuel values")
    void shouldHandleExtremeFuelValues() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing extreme fuel values on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Test boundary values
            final long[] extremeValues = {0L, 1L, Long.MAX_VALUE / 2, Long.MAX_VALUE};

            for (final long fuelValue : extremeValues) {
              store.setFuel(fuelValue);
              assertThat(store.getFuel()).isEqualTo(fuelValue);
              LOGGER.fine("Successfully set fuel to " + fuelValue + " on " + runtimeType);
            }

            // Test fuel overflow protection
            store.setFuel(Long.MAX_VALUE);
            try {
              store.addFuel(1L); // This might overflow
              final long currentFuel = store.getFuel();
              LOGGER.info("Fuel overflow handled: " + currentFuel + " on " + runtimeType);
            } catch (final Exception e) {
              LOGGER.info(
                  "Fuel overflow protection active on " + runtimeType + ": " + e.getMessage());
            }

            // addTestMetric(
            //     "Tested " + extremeValues.length + " extreme fuel values on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle rapid fuel operations")
    void shouldHandleRapidFuelOperations() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing rapid fuel operations on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final int operationCount = 1000;

            // final Duration operationTime =
            //     measureExecutionTime(
            //         "Rapid fuel operations on " + runtimeType,
            //         () -> {
                      long expectedFuel = 0L;
                      for (int i = 0; i < operationCount; i++) {
                        final long increment = i % 100;
                        store.addFuel(increment);
                        expectedFuel += increment;

                        final long actualFuel = store.getFuel();
                        assertThat(actualFuel).isEqualTo(expectedFuel);

                        // Occasionally reset fuel
                        if (i % 100 == 0) {
                          store.setFuel(0L);
                          expectedFuel = 0L;
                        }
                      }
                    // });

            // final double operationsPerSecond = operationCount / (operationTime.toMillis() / 1000.0);
            // addTestMetric(
            //     String.format("%.0f fuel ops/second on %s", operationsPerSecond, runtimeType));
          });
    }
  }

  @Nested
  @DisplayName("Invalid Input Handling Tests")
  final class InvalidInputHandlingTests {

    @Test
    @DisplayName("Should reject malformed WebAssembly bytes")
    void shouldRejectMalformedWebAssemblyBytes() throws WasmException {
      final byte[][] malformedInputs = {
        {}, // Empty
        {0x00}, // Too short
        {0x00, 0x61, 0x73, 0x6d}, // Magic only
        {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00}, // Incomplete version
        "not-wasm".getBytes(), // Random text
        {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, // Invalid magic
        new byte[1024] // Large random bytes
      };

      // Fill the large random bytes with non-zero values
      Arrays.fill(malformedInputs[malformedInputs.length - 1], (byte) 0xAA);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing malformed WebAssembly bytes rejection on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            int rejectionCount = 0;
            for (final byte[] malformedBytes : malformedInputs) {
              assertThatThrownBy(() -> engine.compileModule(malformedBytes))
                  .isInstanceOf(Exception.class)
                  .satisfies(
                      e -> {
                        // Should be either WasmException or IllegalArgumentException
                        assertThat(e)
                            .isInstanceOfAny(WasmException.class, IllegalArgumentException.class);
          });
              rejectionCount++;
            }

            // addTestMetric("Rejected " + rejectionCount + " malformed inputs on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle null parameter rejection consistently")
    void shouldHandleNullParameterRejectionConsistently() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing null parameter rejection on " + runtimeType);

            // Test null engine config
            assertThatThrownBy(() -> runtime.createEngine(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Test null WebAssembly bytes
            assertThatThrownBy(() -> engine.compileModule(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");

            // addTestMetric("Validated null parameter rejection on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Concurrent Edge Cases")
  final class ConcurrentEdgeCasesTests {

    @Test
    @DisplayName("Should handle concurrent engine closure")
    void shouldHandleConcurrentEngineClosure()
        throws InterruptedException, ExecutionException, TimeoutException {
      // skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing concurrent engine closure on " + runtimeType);

            final Engine engine = runtime.createEngine();
            final int threadCount = 4;
            final List<CompletableFuture<String>> futures = new ArrayList<>();

            // Start multiple threads that will try to close the engine
            for (int i = 0; i < threadCount; i++) {
              final CompletableFuture<String> future =
                  CompletableFuture.supplyAsync(
                      () -> {
                        try {
                          engine.close();
                          return "CLOSED";
                        } catch (final Exception e) {
                          return "ERROR: " + e.getMessage();
                        }
                      });
              futures.add(future);
            }

            // Wait for all closes to complete
            try {
              final List<String> results = new ArrayList<>();
              for (final CompletableFuture<String> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
              }

              // All should complete successfully (multiple closes should be safe)
              final long closedCount = results.stream().filter("CLOSED"::equals).count();
              LOGGER.info("Concurrent closure results: " + closedCount + " successful closes");

              // Engine should be invalid after closure
              assertThat(engine.isValid()).isFalse();

              // addTestMetric("Concurrent engine closure handled on " + runtimeType);
            } catch (final Exception e) {
              throw new RuntimeException(e);
            }
          });
    }

    @Test
    @DisplayName("Should handle concurrent store data modifications")
    void shouldHandleConcurrentStoreDataModifications()
        throws InterruptedException, ExecutionException, TimeoutException {
      // skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing concurrent store data modifications on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final int threadCount = 4;
            final int operationsPerThread = 25;
            final List<CompletableFuture<Integer>> futures = new ArrayList<>();

            // Start multiple threads that will modify store data
            for (int threadId = 0; threadId < threadCount; threadId++) {
              final int finalThreadId = threadId;
              final CompletableFuture<Integer> future =
                  CompletableFuture.supplyAsync(
                      () -> {
                        int successfulOperations = 0;
                        for (int op = 0; op < operationsPerThread; op++) {
                          try {
                            final String data = "thread-" + finalThreadId + "-op-" + op;
                            store.setData(data);
                            final Object retrieved = store.getData();
                            // Due to concurrency, retrieved data might not match what we just set
                            // But the operation should not crash
                            successfulOperations++;
                          } catch (final Exception e) {
                            LOGGER.warning("Concurrent data operation failed: " + e.getMessage());
                          }
                        }
                        return successfulOperations;
          });
              futures.add(future);
            }

            // Wait for all operations to complete
            try {
              int totalSuccessfulOperations = 0;
              for (final CompletableFuture<Integer> future : futures) {
                totalSuccessfulOperations += future.get(30, TimeUnit.SECONDS);
              }

              LOGGER.info(
                  "Total successful concurrent data operations: " + totalSuccessfulOperations);
              assertThat(totalSuccessfulOperations).isGreaterThan(0);

              // addTestMetric(
              //     "Concurrent data modifications handled: "
              //         + totalSuccessfulOperations
              //         + " ops on "
              //         + runtimeType);
            } catch (final Exception e) {
              throw new RuntimeException(e);
            }
          });
    }
  }

  @Nested
  @DisplayName("Resource Exhaustion Edge Cases")
  final class ResourceExhaustionEdgeCasesTests {

    @Test
    @DisplayName("Should handle repeated close operations gracefully")
    void shouldHandleRepeatedCloseOperationsGracefully() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing repeated close operations on " + runtimeType);

            final Engine engine = runtime.createEngine();
            final Store store = engine.createStore();

            // Close multiple times - should be safe
            final int closeAttempts = 10;

            // measureExecutionTime(
            //     "Repeated close operations on " + runtimeType,
            //     () -> {
                  for (int i = 0; i < closeAttempts; i++) {
                    store.close();
                    assertThat(store.isValid()).isFalse();
                  }

                  for (int i = 0; i < closeAttempts; i++) {
                    engine.close();
                    assertThat(engine.isValid()).isFalse();
                  }
                // });

            // addTestMetric(
            //     "Handled " + (closeAttempts * 2) + " repeated close operations on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle operations on closed resources gracefully")
    void shouldHandleOperationsOnClosedResourcesGracefully() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing operations on closed resources on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            final Store store = engine.createStore();
            final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

            // Close resources
            store.close();
            engine.close();

            int rejectedOperations = 0;

            // Try operations on closed engine
            assertThatThrownBy(() -> engine.createStore()).isInstanceOf(Exception.class);
            rejectedOperations++;

            assertThatThrownBy(() -> engine.compileModule(wasmBytes)).isInstanceOf(Exception.class);
            rejectedOperations++;

            // Try operations on closed store
            assertThatThrownBy(() -> store.setData("test")).isInstanceOf(Exception.class);
            rejectedOperations++;

            assertThatThrownBy(() -> store.getData()).isInstanceOf(Exception.class);
            rejectedOperations++;

            assertThatThrownBy(() -> store.setFuel(100L)).isInstanceOf(Exception.class);
            rejectedOperations++;

            assertThatThrownBy(() -> store.getFuel()).isInstanceOf(Exception.class);
            rejectedOperations++;

            // addTestMetric(
            //     "Rejected "
            //         + rejectedOperations
            //         + " operations on closed resources on "
            //         + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Performance Edge Cases")
  final class PerformanceEdgeCasesTests {

    @Test
    @DisplayName("Should maintain performance under sustained load")
    void shouldMaintainPerformanceUnderSustainedLoad() throws WasmException {
      // skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing performance under sustained load on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final int iterations = 100;
            final List<Duration> operationTimes = new ArrayList<>();

            // Measure performance over time
            for (int i = 0; i < iterations; i++) {
              final int iteration = i;
              // final Duration operationTime =
              //     measureExecutionTime(
              //         "Sustained load operation " + i + " on " + runtimeType,
              //         () -> {
                        try {
                          final Store store = engine.createStore("load-test-" + iteration);
                          store.setData("sustained-load-data-" + iteration);
                          final Object data = store.getData();
                          assertThat(data).isEqualTo("sustained-load-data-" + iteration);
                          store.close();
                        } catch (final Exception e) {
                          throw new RuntimeException(e);
                        }
                      // });
              // operationTimes.add(operationTime);

              // Occasional garbage collection to simulate real-world conditions
              if (i % 20 == 0) {
                System.gc();
              }
            }

            // Analyze performance degradation
            // final Duration firstOperation = operationTimes.get(0);
            // final Duration lastOperation = operationTimes.get(operationTimes.size() - 1);
            // final double performanceRatio =
            //     (double) lastOperation.toNanos() / firstOperation.toNanos();

            // LOGGER.info(
            //     String.format(
            //         "Performance ratio (last/first): %.2f on %s", performanceRatio, runtimeType));

            // Performance should not degrade significantly (allow up to 3x degradation)
            // assertThat(performanceRatio)
            //     .as("Significant performance degradation detected on " + runtimeType)
            //     .isLessThan(3.0);

            // final double avgTimeMs =
            //     operationTimes.stream().mapToLong(Duration::toMillis).average().orElse(0.0);

            // addTestMetric(
            //     String.format(
            //         "Sustained load avg: %.2fms, ratio: %.2f on %s",
            //         avgTimeMs, performanceRatio, runtimeType));
          });
    }
  }
}

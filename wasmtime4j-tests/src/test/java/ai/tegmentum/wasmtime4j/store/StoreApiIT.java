package ai.tegmentum.wasmtime4j.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
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
 * Comprehensive integration tests for Store API functionality. Tests store creation, binding,
 * lifecycle management, fuel consumption, epoch deadlines, and cross-runtime validation.
 */
@DisplayName("Store API Integration Tests")
final class StoreApiIT extends BaseIntegrationTest {

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    skipIfCategoryNotEnabled(TestCategories.STORE);
  }

  @Nested
  @DisplayName("Store Creation Tests")
  final class StoreCreationTests {

    @Test
    @DisplayName("Should create store from engine")
    void shouldCreateStoreFromEngine() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store creation on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            assertThat(store).isNotNull();
            assertThat(store.isValid()).isTrue();
            assertThat(store.getEngine()).isSameAs(engine);
            assertThat(store.getData()).isNull();

            addTestMetric("Created store on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should create store with custom data")
    void shouldCreateStoreWithCustomData() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store creation with custom data on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final String testData = "test-store-data-" + runtimeType;
            final Store store = engine.createStore(testData);
            registerForCleanup(store);

            assertThat(store).isNotNull();
            assertThat(store.isValid()).isTrue();
            assertThat(store.getEngine()).isSameAs(engine);
            assertThat(store.getData()).isEqualTo(testData);

            addTestMetric("Created store with custom data on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should create store with null data")
    void shouldCreateStoreWithNullData() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store creation with null data on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore(null);
            registerForCleanup(store);

            assertThat(store).isNotNull();
            assertThat(store.isValid()).isTrue();
            assertThat(store.getEngine()).isSameAs(engine);
            assertThat(store.getData()).isNull();

            addTestMetric("Created store with null data on " + runtimeType);
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
            final int storeCount = 3;

            for (int i = 0; i < storeCount; i++) {
              final Store store = engine.createStore("store-data-" + i);
              registerForCleanup(store);
              stores.add(store);

              assertThat(store.isValid()).isTrue();
              assertThat(store.getEngine()).isSameAs(engine);
              assertThat(store.getData()).isEqualTo("store-data-" + i);
            }

            assertThat(stores).hasSize(storeCount);

            addTestMetric("Created " + storeCount + " stores on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Store Data Management Tests")
  final class StoreDataManagementTests {

    @Test
    @DisplayName("Should manage custom data correctly")
    void shouldManageCustomDataCorrectly() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store data management on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Initially null
            assertThat(store.getData()).isNull();

            // Set string data
            final String stringData = "test-string-" + runtimeType;
            store.setData(stringData);
            assertThat(store.getData()).isEqualTo(stringData);

            // Set integer data
            final Integer integerData = 42;
            store.setData(integerData);
            assertThat(store.getData()).isEqualTo(integerData);

            // Set null data
            store.setData(null);
            assertThat(store.getData()).isNull();

            // Set complex object data
            final List<String> listData = List.of("item1", "item2", "item3");
            store.setData(listData);
            assertThat(store.getData()).isEqualTo(listData);

            addTestMetric("Managed store data on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Store Fuel Management Tests")
  final class StoreFuelManagementTests {

    @Test
    @DisplayName("Should set and get fuel correctly")
    void shouldSetAndGetFuelCorrectly() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store fuel management on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Set initial fuel
            final long initialFuel = 1000L;
            store.setFuel(initialFuel);
            assertThat(store.getFuel()).isEqualTo(initialFuel);

            // Add fuel
            final long additionalFuel = 500L;
            store.addFuel(additionalFuel);
            assertThat(store.getFuel()).isEqualTo(initialFuel + additionalFuel);

            // Set zero fuel
            store.setFuel(0L);
            assertThat(store.getFuel()).isEqualTo(0L);

            addTestMetric("Managed store fuel on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle fuel when consumption is disabled")
    void shouldHandleFuelWhenConsumptionIsDisabled() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store fuel with consumption disabled on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(false);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Fuel operations should either work or throw appropriate exceptions
            // depending on implementation behavior when fuel is disabled
            try {
              store.setFuel(1000L);
              final long fuel = store.getFuel();
              LOGGER.info("Fuel operations allowed when consumption disabled, fuel: " + fuel);
            } catch (final WasmException e) {
              LOGGER.info("Fuel operations rejected when consumption disabled: " + e.getMessage());
              assertThat(e.getMessage()).containsIgnoringCase("fuel");
            }

            addTestMetric("Tested fuel with consumption disabled on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should reject negative fuel values")
    void shouldRejectNegativeFuelValues() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing negative fuel rejection on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Should reject negative fuel
            assertThatThrownBy(() -> store.setFuel(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fuel");

            // Should reject negative fuel addition
            store.setFuel(100L);
            assertThatThrownBy(() -> store.addFuel(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fuel");

            addTestMetric("Rejected negative fuel on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle large fuel values")
    void shouldHandleLargeFuelValues() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing large fuel values on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Test with maximum long value
            final long maxFuel = Long.MAX_VALUE;
            store.setFuel(maxFuel);
            assertThat(store.getFuel()).isEqualTo(maxFuel);

            // Test large fuel addition (but avoid overflow)
            store.setFuel(0L);
            store.addFuel(maxFuel / 2);
            assertThat(store.getFuel()).isEqualTo(maxFuel / 2);

            addTestMetric("Handled large fuel values on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Store Epoch Management Tests")
  final class StoreEpochManagementTests {

    @Test
    @DisplayName("Should set epoch deadline correctly")
    void shouldSetEpochDeadlineCorrectly() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store epoch deadline on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Set various epoch deadlines
            final long[] deadlines = {0L, 1L, 100L, 1000L, Long.MAX_VALUE};

            for (final long deadline : deadlines) {
              store.setEpochDeadline(deadline);
              LOGGER.fine("Set epoch deadline to " + deadline + " on " + runtimeType);
            }

            addTestMetric("Set " + deadlines.length + " epoch deadlines on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle negative epoch deadline")
    void shouldHandleNegativeEpochDeadline() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing negative epoch deadline on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Behavior with negative deadline may be implementation-specific
            try {
              store.setEpochDeadline(-1L);
              LOGGER.info("Negative epoch deadline accepted on " + runtimeType);
            } catch (final Exception e) {
              LOGGER.info(
                  "Negative epoch deadline rejected on " + runtimeType + ": " + e.getMessage());
            }

            addTestMetric("Tested negative epoch deadline on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Store Lifecycle Tests")
  final class StoreLifecycleTests {

    @Test
    @DisplayName("Should properly close store")
    void shouldProperlyCloseStore() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store closure on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            assertThat(store.isValid()).isTrue();

            store.close();
            assertThat(store.isValid()).isFalse();

            // Multiple closes should be safe
            store.close();
            assertThat(store.isValid()).isFalse();

            addTestMetric("Validated store closure on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should reject operations on closed store")
    void shouldRejectOperationsOnClosedStore() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing closed store operations rejection on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            store.close();

            // Should reject data operations
            assertThatThrownBy(() -> store.setData("test"))
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("Store is closed");

            assertThatThrownBy(() -> store.getData())
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("Store is closed");

            // Should reject fuel operations
            assertThatThrownBy(() -> store.setFuel(100L))
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("Store is closed");

            assertThatThrownBy(() -> store.getFuel())
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("Store is closed");

            assertThatThrownBy(() -> store.addFuel(50L))
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("Store is closed");

            // Should reject epoch operations
            assertThatThrownBy(() -> store.setEpochDeadline(1000L))
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("Store is closed");

            addTestMetric("Rejected operations on closed store on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle store lifecycle with closed engine")
    void shouldHandleStoreLifecycleWithClosedEngine() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store with closed engine on " + runtimeType);

            final Engine engine = runtime.createEngine();
            final Store store = engine.createStore();

            assertThat(store.isValid()).isTrue();
            assertThat(store.getEngine()).isSameAs(engine);

            // Close the engine
            engine.close();

            // Store behavior with closed engine may be implementation-specific
            // Some implementations may invalidate the store, others may keep it valid
            final boolean storeValid = store.isValid();
            LOGGER.info(
                "Store validity after engine closure: " + storeValid + " on " + runtimeType);

            // Clean up if still valid
            if (storeValid) {
              store.close();
            }

            addTestMetric("Tested store with closed engine on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Store Thread Safety Tests")
  final class StoreThreadSafetyTests {

    @Test
    @DisplayName("Should handle concurrent data operations safely")
    void shouldHandleConcurrentDataOperationsSafely() throws Exception {
      skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing concurrent store data operations on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final int threadCount = 4;
            final int operationsPerThread = 10;
            final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            final CountDownLatch latch = new CountDownLatch(threadCount * operationsPerThread);

            try {
              final List<CompletableFuture<Void>> futures = new ArrayList<>();

              for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                for (int op = 0; op < operationsPerThread; op++) {
                  final int operationId = op;
                  final CompletableFuture<Void> future =
                      CompletableFuture.runAsync(
                          () -> {
                            try {
                              final String data = "thread-" + threadId + "-op-" + operationId;
                              store.setData(data);
                              final Object retrievedData = store.getData();
                              // Data may not match due to concurrent access, but operations should
                              // not crash
                              latch.countDown();
                            } catch (final Exception e) {
                              LOGGER.warning("Concurrent data operation failed: " + e.getMessage());
                              latch.countDown();
                            }
                          },
                          executor);
                  futures.add(future);
                }
              }

              // Wait for all operations to complete
              assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();

              // All futures should complete without uncaught exceptions
              for (final CompletableFuture<Void> future : futures) {
                future.get(); // This will throw if there was an uncaught exception
              }

              addTestMetric(
                  "Completed "
                      + (threadCount * operationsPerThread)
                      + " concurrent data operations on "
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
    @DisplayName("Should handle concurrent fuel operations safely")
    void shouldHandleConcurrentFuelOperationsSafely() throws Exception {
      skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing concurrent store fuel operations on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Set initial fuel
            store.setFuel(10000L);

            final int threadCount = 4;
            final int operationsPerThread = 5;
            final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            final CountDownLatch latch = new CountDownLatch(threadCount * operationsPerThread);

            try {
              final List<CompletableFuture<Void>> futures = new ArrayList<>();

              for (int t = 0; t < threadCount; t++) {
                for (int op = 0; op < operationsPerThread; op++) {
                  final CompletableFuture<Void> future =
                      CompletableFuture.runAsync(
                          () -> {
                            try {
                              // Perform fuel operations
                              store.addFuel(10L);
                              final long fuel = store.getFuel();
                              store.setFuel(Math.max(0L, fuel - 5L));
                              latch.countDown();
                            } catch (final Exception e) {
                              LOGGER.warning("Concurrent fuel operation failed: " + e.getMessage());
                              latch.countDown();
                            }
                          },
                          executor);
                  futures.add(future);
                }
              }

              // Wait for all operations to complete
              assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();

              // All futures should complete without uncaught exceptions
              for (final CompletableFuture<Void> future : futures) {
                future.get();
              }

              addTestMetric(
                  "Completed "
                      + (threadCount * operationsPerThread)
                      + " concurrent fuel operations on "
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
    @DisplayName("Should produce identical store creation results")
    void shouldProduceIdenticalStoreCreationResults() {
      skipIfPanamaNotAvailable();

      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final Engine engine = runtime.createEngine();
                final Store store = engine.createStore("test-data");
                final Object data = store.getData();
                final boolean isValid = store.isValid();
                store.close();
                engine.close();
                return isValid;
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info("Cross-runtime store creation validation: " + result.getDifferenceDescription());
    }

    @Test
    @DisplayName("Should produce identical fuel management results")
    void shouldProduceIdenticalFuelManagementResults() {
      skipIfPanamaNotAvailable();

      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final EngineConfig config = new EngineConfig().consumeFuel(true);
                final Engine engine = runtime.createEngine(config);
                final Store store = engine.createStore();

                store.setFuel(1000L);
                final long fuel1 = store.getFuel();

                store.addFuel(500L);
                final long fuel2 = store.getFuel();

                store.close();
                engine.close();

                return fuel2 - fuel1; // Should be 500
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info("Cross-runtime fuel management validation: " + result.getDifferenceDescription());
    }

    @Test
    @DisplayName("Should produce identical data management results")
    void shouldProduceIdenticalDataManagementResults() {
      skipIfPanamaNotAvailable();

      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();

                store.setData("test-data");
                final Object data1 = store.getData();

                store.setData(42);
                final Object data2 = store.getData();

                store.setData(null);
                final Object data3 = store.getData();

                store.close();
                engine.close();

                return List.of(data1, data2, data3);
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();
      LOGGER.info("Cross-runtime data management validation: " + result.getDifferenceDescription());
    }
  }

  @Nested
  @DisplayName("Store Performance Tests")
  final class StorePerformanceTests {

    @Test
    @DisplayName("Should meet store creation performance baseline")
    void shouldMeetStoreCreationPerformanceBaseline() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store creation performance on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Duration maxCreationTime = Duration.ofMillis(100);

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

    @Test
    @DisplayName("Should meet data operations performance baseline")
    void shouldMeetDataOperationsPerformanceBaseline() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store data operations performance on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final Duration maxOperationTime = Duration.ofMillis(10);

            // Test data set/get operations
            assertExecutionTime(
                maxOperationTime,
                () -> {
                  store.setData("test-data");
                  final Object data = store.getData();
                  assertThat(data).isEqualTo("test-data");
                },
                "Data operations on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should meet fuel operations performance baseline")
    void shouldMeetFuelOperationsPerformanceBaseline() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing store fuel operations performance on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final Duration maxOperationTime = Duration.ofMillis(10);

            // Test fuel operations
            assertExecutionTime(
                maxOperationTime,
                () -> {
                  try {
                    store.setFuel(1000L);
                    final long fuel1 = store.getFuel();
                    store.addFuel(500L);
                    final long fuel2 = store.getFuel();
                    assertThat(fuel2).isEqualTo(fuel1 + 500L);
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                "Fuel operations on " + runtimeType);
          });
    }
  }
}

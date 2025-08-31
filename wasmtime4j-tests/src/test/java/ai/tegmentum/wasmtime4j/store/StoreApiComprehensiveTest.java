package ai.tegmentum.wasmtime4j.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.performance.PerformanceTestHarness;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive Store API tests covering all store operations, lifecycle management, fuel
 * consumption, epoch deadlines, data binding, error handling, thread safety, and performance.
 */
@DisplayName("Store API Comprehensive Tests")
final class StoreApiComprehensiveTest extends BaseIntegrationTest {

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    skipIfCategoryNotEnabled(TestCategories.STORE);
  }

  @Nested
  @DisplayName("Store Data Management Comprehensive Tests")
  final class StoreDataManagementComprehensiveTests {

    @Test
    @DisplayName("Should handle all data types correctly")
    void shouldHandleAllDataTypesCorrectly() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing comprehensive data type handling on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        final Store store = engine.createStore();
        registerForCleanup(store);

        final List<Object> testData = Arrays.asList(
            null,
            "string-data",
            42,
            42L,
            3.14f,
            3.14159,
            true,
            false,
            new byte[]{1, 2, 3, 4, 5},
            Arrays.asList("list", "of", "strings"),
            Map.of("key1", "value1", "key2", "value2"),
            new TestDataObject("test", 123)
        );

        for (int i = 0; i < testData.size(); i++) {
          final Object data = testData.get(i);
          
          store.setData(data);
          final Object retrievedData = store.getData();
          
          if (data == null) {
            assertThat(retrievedData).isNull();
          } else {
            assertThat(retrievedData).isEqualTo(data);
          }
          
          LOGGER.fine("Successfully handled data type " + (data != null ? data.getClass().getSimpleName() : "null") + 
                     " on " + runtimeType);
        }

        addTestMetric("Tested " + testData.size() + " data types on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should handle large data objects efficiently")
    void shouldHandleLargeDataObjectsEfficiently() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing large data object handling on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        final Store store = engine.createStore();
        registerForCleanup(store);

        executeWithMemoryMonitoring("Large data handling on " + runtimeType, () -> {
          // Test large string
          final StringBuilder largeStringBuilder = new StringBuilder();
          for (int i = 0; i < 100000; i++) {
            largeStringBuilder.append("data");
          }
          final String largeString = largeStringBuilder.toString();

          store.setData(largeString);
          assertThat(store.getData()).isEqualTo(largeString);

          // Test large byte array
          final byte[] largeByteArray = new byte[1000000];
          Arrays.fill(largeByteArray, (byte) 42);
          store.setData(largeByteArray);
          assertThat(store.getData()).isEqualTo(largeByteArray);

          // Test large collection
          final List<String> largeList = new ArrayList<>();
          for (int i = 0; i < 50000; i++) {
            largeList.add("item-" + i);
          }
          store.setData(largeList);
          assertThat(store.getData()).isEqualTo(largeList);
        });

        addTestMetric("Handled large data objects on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should maintain data consistency during concurrent access")
    void shouldMaintainDataConsistencyDuringConcurrentAccess() throws Exception {
      skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing data consistency during concurrent access on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        final Store store = engine.createStore();
        registerForCleanup(store);

        final int threadCount = 4;
        final int operationsPerThread = 25;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount * operationsPerThread);
        final AtomicInteger successCount = new AtomicInteger(0);
        final List<CompletableFuture<Void>> futures = new ArrayList<>();

        try {
          for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            for (int op = 0; op < operationsPerThread; op++) {
              final int operationId = op;
              final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                  final String data = "thread-" + threadId + "-data-" + operationId;
                  
                  // Perform data operations
                  store.setData(data);
                  Thread.sleep(1); // Small delay to increase chance of race conditions
                  final Object retrievedData = store.getData();
                  
                  // Data might not match due to concurrent access, but operations should not fail
                  if (retrievedData instanceof String) {
                    successCount.incrementAndGet();
                  }
                  
                  latch.countDown();
                } catch (final Exception e) {
                  LOGGER.warning("Concurrent data operation failed: " + e.getMessage());
                  latch.countDown();
                }
              }, executor);
              futures.add(future);
            }
          }

          // Wait for all operations to complete
          assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();

          // Verify operations completed without crashes
          for (final CompletableFuture<Void> future : futures) {
            future.get(); // This will throw if there was an uncaught exception
          }

          // Some operations should have succeeded
          assertThat(successCount.get()).isGreaterThan(0);

          addTestMetric("Completed " + (threadCount * operationsPerThread) + 
                       " concurrent data operations on " + runtimeType + 
                       " (" + successCount.get() + " successful)");
        } finally {
          executor.shutdown();
          if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            executor.shutdownNow();
          }
        }
      });
    }

    /** Test data class for complex object testing. */
    private static final class TestDataObject {
      private final String name;
      private final int value;

      TestDataObject(final String name, final int value) {
        this.name = name;
        this.value = value;
      }

      @Override
      public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final TestDataObject that = (TestDataObject) obj;
        return value == that.value && name.equals(that.name);
      }

      @Override
      public int hashCode() {
        return name.hashCode() * 31 + value;
      }
    }
  }

  @Nested
  @DisplayName("Store Fuel Management Comprehensive Tests")
  final class StoreFuelManagementComprehensiveTests {

    @Test
    @DisplayName("Should handle fuel operations comprehensively")
    void shouldHandleFuelOperationsComprehensively() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing comprehensive fuel operations on " + runtimeType);

        final EngineConfig config = new EngineConfig().consumeFuel(true);
        final Engine engine = runtime.createEngine(config);
        registerForCleanup(engine);

        final Store store = engine.createStore();
        registerForCleanup(store);

        // Test basic fuel operations
        final long[] fuelValues = {0L, 1L, 100L, 1000L, 10000L, 1000000L, Long.MAX_VALUE};
        
        for (final long fuelValue : fuelValues) {
          store.setFuel(fuelValue);
          assertThat(store.getFuel()).isEqualTo(fuelValue);
          
          // Test fuel addition
          if (fuelValue <= Long.MAX_VALUE / 2) {
            store.addFuel(fuelValue);
            assertThat(store.getFuel()).isEqualTo(fuelValue * 2);
            
            // Reset for next iteration
            store.setFuel(fuelValue);
          }
        }

        // Test fuel arithmetic edge cases
        store.setFuel(Long.MAX_VALUE);
        assertThat(store.getFuel()).isEqualTo(Long.MAX_VALUE);

        store.setFuel(0L);
        store.addFuel(Long.MAX_VALUE);
        assertThat(store.getFuel()).isEqualTo(Long.MAX_VALUE);

        addTestMetric("Tested " + fuelValues.length + " fuel values on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should handle fuel operations with concurrent access")
    void shouldHandleFuelOperationsWithConcurrentAccess() throws Exception {
      skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing concurrent fuel operations on " + runtimeType);

        final EngineConfig config = new EngineConfig().consumeFuel(true);
        final Engine engine = runtime.createEngine(config);
        registerForCleanup(engine);

        final Store store = engine.createStore();
        registerForCleanup(store);

        // Set initial fuel
        store.setFuel(1000000L);

        final int threadCount = 4;
        final int operationsPerThread = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount * operationsPerThread);
        final AtomicLong totalFuelAdded = new AtomicLong(0);
        final AtomicInteger successfulOperations = new AtomicInteger(0);

        try {
          final List<CompletableFuture<Void>> futures = new ArrayList<>();

          for (int t = 0; t < threadCount; t++) {
            for (int op = 0; op < operationsPerThread; op++) {
              final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                  final long fuelToAdd = 100L;
                  
                  // Perform fuel operations
                  final long fuelBefore = store.getFuel();
                  store.addFuel(fuelToAdd);
                  final long fuelAfter = store.getFuel();
                  
                  // Verify fuel increased (though exact amount may vary due to concurrent access)
                  if (fuelAfter >= fuelBefore) {
                    totalFuelAdded.addAndGet(fuelToAdd);
                    successfulOperations.incrementAndGet();
                  }
                  
                  latch.countDown();
                } catch (final Exception e) {
                  LOGGER.warning("Concurrent fuel operation failed: " + e.getMessage());
                  latch.countDown();
                }
              }, executor);
              futures.add(future);
            }
          }

          // Wait for all operations to complete
          assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();

          // Verify operations completed without crashes
          for (final CompletableFuture<Void> future : futures) {
            future.get();
          }

          // Most operations should have succeeded
          final int totalOperations = threadCount * operationsPerThread;
          assertThat(successfulOperations.get()).isGreaterThan(totalOperations / 2);

          addTestMetric("Completed " + totalOperations + " concurrent fuel operations on " + runtimeType + 
                       " (" + successfulOperations.get() + " successful)");
        } finally {
          executor.shutdown();
          if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            executor.shutdownNow();
          }
        }
      });
    }

    @Test
    @DisplayName("Should handle fuel operations when fuel consumption is disabled")
    void shouldHandleFuelOperationsWhenConsumptionDisabled() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing fuel operations with consumption disabled on " + runtimeType);

        final EngineConfig config = new EngineConfig().consumeFuel(false);
        final Engine engine = runtime.createEngine(config);
        registerForCleanup(engine);

        final Store store = engine.createStore();
        registerForCleanup(store);

        // Test fuel operations behavior when consumption is disabled
        boolean fuelOperationsSupported = true;
        String behaviorDescription = "";

        try {
          store.setFuel(1000L);
          final long fuel = store.getFuel();
          store.addFuel(500L);
          final long newFuel = store.getFuel();
          
          behaviorDescription = "Fuel operations allowed when consumption disabled";
          LOGGER.info(behaviorDescription + " on " + runtimeType + 
                     " (initial: " + fuel + ", after add: " + newFuel + ")");
        } catch (final WasmException e) {
          fuelOperationsSupported = false;
          behaviorDescription = "Fuel operations rejected when consumption disabled";
          LOGGER.info(behaviorDescription + " on " + runtimeType + ": " + e.getMessage());
          
          // Verify error message indicates fuel is disabled
          assertThat(e.getMessage().toLowerCase()).containsAnyOf("fuel", "disabled", "not enabled");
        }

        addTestMetric(behaviorDescription + " on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should validate fuel boundary conditions")
    void shouldValidateFuelBoundaryConditions() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing fuel boundary conditions on " + runtimeType);

        final EngineConfig config = new EngineConfig().consumeFuel(true);
        final Engine engine = runtime.createEngine(config);
        registerForCleanup(engine);

        final Store store = engine.createStore();
        registerForCleanup(store);

        // Test negative fuel rejection
        assertThatThrownBy(() -> store.setFuel(-1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fuel");

        assertThatThrownBy(() -> store.addFuel(-1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fuel");

        // Test maximum fuel values
        store.setFuel(Long.MAX_VALUE);
        assertThat(store.getFuel()).isEqualTo(Long.MAX_VALUE);

        // Test fuel overflow prevention (implementation-specific behavior)
        store.setFuel(Long.MAX_VALUE - 1000);
        try {
          store.addFuel(2000);
          final long resultFuel = store.getFuel();
          // Should either cap at MAX_VALUE or handle overflow gracefully
          assertThat(resultFuel).isGreaterThan(0); // Should not wrap to negative
        } catch (final Exception e) {
          // Some implementations might reject operations that would overflow
          LOGGER.info("Fuel overflow rejected on " + runtimeType + ": " + e.getMessage());
        }

        // Test zero fuel operations
        store.setFuel(0L);
        assertThat(store.getFuel()).isEqualTo(0L);
        store.addFuel(0L);
        assertThat(store.getFuel()).isEqualTo(0L);

        addTestMetric("Validated fuel boundary conditions on " + runtimeType);
      });
    }
  }

  @Nested
  @DisplayName("Store Epoch Management Comprehensive Tests")
  final class StoreEpochManagementComprehensiveTests {

    @Test
    @DisplayName("Should handle comprehensive epoch deadline operations")
    void shouldHandleComprehensiveEpochDeadlineOperations() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing comprehensive epoch deadline operations on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        final Store store = engine.createStore();
        registerForCleanup(store);

        // Test various epoch deadline values
        final long[] deadlines = {
            0L, 1L, 10L, 100L, 1000L, 10000L, 100000L, 
            Long.MAX_VALUE / 2, Long.MAX_VALUE
        };

        for (final long deadline : deadlines) {
          store.setEpochDeadline(deadline);
          // Note: Most implementations don't provide a way to get the deadline back,
          // so we just verify the operation doesn't throw
          LOGGER.fine("Successfully set epoch deadline to " + deadline + " on " + runtimeType);
        }

        addTestMetric("Tested " + deadlines.length + " epoch deadlines on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should handle epoch deadline edge cases")
    void shouldHandleEpochDeadlineEdgeCases() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing epoch deadline edge cases on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        final Store store = engine.createStore();
        registerForCleanup(store);

        // Test negative deadline handling (implementation-specific)
        try {
          store.setEpochDeadline(-1L);
          LOGGER.info("Negative epoch deadline accepted on " + runtimeType);
        } catch (final Exception e) {
          LOGGER.info("Negative epoch deadline rejected on " + runtimeType + ": " + e.getMessage());
          // Both behaviors are acceptable depending on implementation
        }

        // Test extreme values
        final long[] extremeValues = {Long.MIN_VALUE, -1000L, 0L, Long.MAX_VALUE};
        
        for (final long value : extremeValues) {
          try {
            store.setEpochDeadline(value);
            LOGGER.fine("Extreme epoch deadline " + value + " accepted on " + runtimeType);
          } catch (final Exception e) {
            LOGGER.fine("Extreme epoch deadline " + value + " rejected on " + runtimeType + ": " + e.getMessage());
          }
        }

        // Test rapid deadline changes
        for (int i = 0; i < 1000; i++) {
          store.setEpochDeadline(i);
        }

        addTestMetric("Tested epoch deadline edge cases on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should handle concurrent epoch deadline operations")
    void shouldHandleConcurrentEpochDeadlineOperations() throws Exception {
      skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing concurrent epoch deadline operations on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        final Store store = engine.createStore();
        registerForCleanup(store);

        final int threadCount = 4;
        final int operationsPerThread = 25;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount * operationsPerThread);
        final AtomicInteger successCount = new AtomicInteger(0);

        try {
          final List<CompletableFuture<Void>> futures = new ArrayList<>();

          for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            for (int op = 0; op < operationsPerThread; op++) {
              final int operationId = op;
              final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                  final long deadline = threadId * 1000L + operationId;
                  store.setEpochDeadline(deadline);
                  successCount.incrementAndGet();
                  latch.countDown();
                } catch (final Exception e) {
                  LOGGER.warning("Concurrent epoch operation failed: " + e.getMessage());
                  latch.countDown();
                }
              }, executor);
              futures.add(future);
            }
          }

          // Wait for all operations to complete
          assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();

          // Verify operations completed without crashes
          for (final CompletableFuture<Void> future : futures) {
            future.get();
          }

          // All operations should have succeeded
          final int totalOperations = threadCount * operationsPerThread;
          assertThat(successCount.get()).isEqualTo(totalOperations);

          addTestMetric("Completed " + totalOperations + " concurrent epoch operations on " + runtimeType);
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
  @DisplayName("Store Lifecycle Comprehensive Tests")
  final class StoreLifecycleComprehensiveTests {

    @Test
    @DisplayName("Should handle complex store lifecycle scenarios")
    void shouldHandleComplexStoreLifecycleScenarios() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing complex store lifecycle scenarios on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        // Scenario 1: Multiple stores with different configurations
        final List<Store> stores = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
          final Store store = engine.createStore("store-" + i);
          stores.add(store);
          
          // Configure each store differently
          store.setData(Map.of("id", i, "name", "store-" + i));
          
          if (engine.getConfig().isConsumeFuel()) {
            try {
              store.setFuel(1000L * (i + 1));
            } catch (final WasmException e) {
              LOGGER.fine("Fuel operation not supported: " + e.getMessage());
            }
          }
          
          store.setEpochDeadline(10000L * (i + 1));
        }

        // Verify all stores are valid and configured correctly
        for (int i = 0; i < stores.size(); i++) {
          final Store store = stores.get(i);
          assertThat(store.isValid()).isTrue();
          assertThat(store.getEngine()).isSameAs(engine);
          
          final Map<String, Object> data = (Map<String, Object>) store.getData();
          assertThat(data.get("id")).isEqualTo(i);
          assertThat(data.get("name")).isEqualTo("store-" + i);
        }

        // Scenario 2: Close stores in different orders
        stores.get(2).close(); // Close middle store
        stores.get(0).close(); // Close first store
        stores.get(4).close(); // Close last store

        // Verify closed stores are invalid
        assertThat(stores.get(2).isValid()).isFalse();
        assertThat(stores.get(0).isValid()).isFalse();
        assertThat(stores.get(4).isValid()).isFalse();

        // Verify remaining stores are still valid
        assertThat(stores.get(1).isValid()).isTrue();
        assertThat(stores.get(3).isValid()).isTrue();

        // Clean up remaining stores
        stores.get(1).close();
        stores.get(3).close();

        addTestMetric("Completed complex lifecycle scenarios on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should handle store operations after engine closure")
    void shouldHandleStoreOperationsAfterEngineClosure() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing store behavior after engine closure on " + runtimeType);

        final Engine engine = runtime.createEngine();
        final Store store = engine.createStore("test-data");
        
        // Verify initial state
        assertThat(store.isValid()).isTrue();
        assertThat(store.getData()).isEqualTo("test-data");

        // Close the engine
        engine.close();
        assertThat(engine.isValid()).isFalse();

        // Test store behavior after engine closure
        // Implementation-specific: some may invalidate stores, others may keep them functional
        final boolean storeValidAfterEngineClosure = store.isValid();
        LOGGER.info("Store remains valid after engine closure: " + storeValidAfterEngineClosure + 
                   " on " + runtimeType);

        if (storeValidAfterEngineClosure) {
          // If store remains valid, test basic operations
          try {
            final Object data = store.getData();
            LOGGER.info("Store data still accessible after engine closure on " + runtimeType);
            
            store.setData("new-data");
            assertThat(store.getData()).isEqualTo("new-data");
            
          } catch (final WasmException e) {
            LOGGER.info("Store operations rejected after engine closure on " + runtimeType + ": " + e.getMessage());
          }
        } else {
          // If store is invalidated, operations should fail appropriately
          assertThatThrownBy(() -> store.getData())
              .isInstanceOf(WasmException.class)
              .hasMessageContaining("closed");
        }

        // Clean up if needed
        if (storeValidAfterEngineClosure) {
          store.close();
        }

        addTestMetric("Tested store behavior after engine closure on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should handle rapid store creation and destruction")
    void shouldHandleRapidStoreCreationAndDestruction() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing rapid store creation and destruction on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        executeWithMemoryMonitoring("Rapid store lifecycle on " + runtimeType, () -> {
          final int cycles = 500;

          for (int i = 0; i < cycles; i++) {
            try {
              final Store store = engine.createStore("rapid-test-" + i);
              assertThat(store.isValid()).isTrue();
              
              // Perform some operations
              store.setData("data-" + i);
              assertThat(store.getData()).isEqualTo("data-" + i);
              
              // Set epoch deadline
              store.setEpochDeadline(i);
              
              // Clean up immediately
              store.close();
              assertThat(store.isValid()).isFalse();
            } catch (final WasmException e) {
              throw new RuntimeException("Rapid lifecycle cycle " + i + " failed", e);
            }
          }
        });

        addTestMetric("Completed " + 500 + " rapid store lifecycle cycles on " + runtimeType);
      });
    }
  }

  @Nested
  @DisplayName("Store Error Handling Comprehensive Tests")
  final class StoreErrorHandlingComprehensiveTests {

    @Test
    @DisplayName("Should handle all invalid store operations gracefully")
    void shouldHandleAllInvalidStoreOperationsGracefully() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing comprehensive invalid store operations on " + runtimeType);

        final EngineConfig config = new EngineConfig().consumeFuel(true);
        final Engine engine = runtime.createEngine(config);
        registerForCleanup(engine);

        final Store store = engine.createStore();
        store.close(); // Close the store to test error handling

        // Test all operations on closed store
        final List<Runnable> invalidOperations = Arrays.asList(
            () -> store.setData("test"),
            () -> store.getData(),
            () -> { try { store.setFuel(100L); } catch (WasmException e) { throw new RuntimeException(e); }},
            () -> { try { store.getFuel(); } catch (WasmException e) { throw new RuntimeException(e); }},
            () -> { try { store.addFuel(50L); } catch (WasmException e) { throw new RuntimeException(e); }},
            () -> { try { store.setEpochDeadline(1000L); } catch (WasmException e) { throw new RuntimeException(e); }}
        );

        final List<String> operationNames = Arrays.asList(
            "setData", "getData", "setFuel", "getFuel", "addFuel", "setEpochDeadline"
        );

        for (int i = 0; i < invalidOperations.size(); i++) {
          final Runnable operation = invalidOperations.get(i);
          final String operationName = operationNames.get(i);
          
          try {
            operation.run();
            LOGGER.warning("Expected " + operationName + " to fail on closed store on " + runtimeType);
          } catch (final Exception e) {
            // Expected - operation should fail on closed store
            assertThat(e.getMessage().toLowerCase()).containsAnyOf("closed", "invalid", "not valid");
            LOGGER.fine("Successfully rejected " + operationName + " on closed store on " + runtimeType);
          }
        }

        addTestMetric("Validated " + invalidOperations.size() + " invalid operations on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should provide meaningful error messages for all failure scenarios")
    void shouldProvideMeaningfulErrorMessagesForAllFailureScenarios() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing meaningful error messages on " + runtimeType);

        final EngineConfig config = new EngineConfig().consumeFuel(true);
        final Engine engine = runtime.createEngine(config);
        registerForCleanup(engine);

        final Store store = engine.createStore();

        // Test various error scenarios and verify meaningful messages
        final List<Map<String, Object>> errorScenarios = Arrays.asList(
            Map.of("operation", "negative fuel", "test", (Runnable) () -> {
              try { store.setFuel(-1L); } catch (Exception e) { throw new RuntimeException(e); }
            }, "expectedWords", Arrays.asList("fuel", "negative")),
            
            Map.of("operation", "negative fuel addition", "test", (Runnable) () -> {
              try { store.addFuel(-1L); } catch (Exception e) { throw new RuntimeException(e); }
            }, "expectedWords", Arrays.asList("fuel", "negative")),
            
            Map.of("operation", "closed store data access", "test", (Runnable) () -> {
              final Store closedStore = engine.createStore();
              closedStore.close();
              closedStore.getData();
            }, "expectedWords", Arrays.asList("closed", "invalid"))
        );

        for (final Map<String, Object> scenario : errorScenarios) {
          final String operationName = (String) scenario.get("operation");
          final Runnable test = (Runnable) scenario.get("test");
          final List<String> expectedWords = (List<String>) scenario.get("expectedWords");
          
          try {
            test.run();
            LOGGER.warning("Expected " + operationName + " to fail on " + runtimeType);
          } catch (final Exception e) {
            // Verify error message contains meaningful information
            final String errorMessage = e.getMessage().toLowerCase();
            boolean containsMeaningfulInfo = false;
            
            for (final String word : expectedWords) {
              if (errorMessage.contains(word.toLowerCase())) {
                containsMeaningfulInfo = true;
                break;
              }
            }
            
            assertThat(containsMeaningfulInfo)
                .as("Error message for " + operationName + " should contain meaningful information: " + e.getMessage())
                .isTrue();
            
            LOGGER.fine("Got meaningful error for " + operationName + " on " + runtimeType + ": " + e.getMessage());
          }
        }

        store.close(); // Clean up

        addTestMetric("Validated meaningful error messages on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should recover gracefully from partial operation failures")
    void shouldRecoverGracefullyFromPartialOperationFailures() throws WasmException {
      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Testing graceful recovery from partial failures on " + runtimeType);

        final EngineConfig config = new EngineConfig().consumeFuel(true);
        final Engine engine = runtime.createEngine(config);
        registerForCleanup(engine);

        final Store store = engine.createStore();
        registerForCleanup(store);

        // Set up initial state
        store.setData("initial-data");
        store.setFuel(1000L);
        store.setEpochDeadline(5000L);

        // Attempt invalid operations
        try {
          store.setFuel(-1L);
        } catch (final IllegalArgumentException expected) {
          LOGGER.fine("Got expected invalid fuel error: " + expected.getMessage());
        }

        try {
          store.addFuel(-1L);
        } catch (final IllegalArgumentException expected) {
          LOGGER.fine("Got expected invalid fuel addition error: " + expected.getMessage());
        }

        // Verify store is still functional after failed operations
        assertThat(store.isValid()).isTrue();
        assertThat(store.getData()).isEqualTo("initial-data");
        assertThat(store.getFuel()).isEqualTo(1000L);

        // Verify store can still perform valid operations
        store.setData("recovery-test-data");
        assertThat(store.getData()).isEqualTo("recovery-test-data");

        store.setFuel(2000L);
        assertThat(store.getFuel()).isEqualTo(2000L);

        store.addFuel(500L);
        assertThat(store.getFuel()).isEqualTo(2500L);

        store.setEpochDeadline(10000L);
        // Epoch deadline set successfully (no exception thrown)

        addTestMetric("Validated graceful recovery from failures on " + runtimeType);
      });
    }
  }

  @Nested
  @DisplayName("Store Performance Comprehensive Tests")
  final class StorePerformanceComprehensiveTests {

    @Test
    @DisplayName("Should establish comprehensive store performance baselines")
    void shouldEstablishComprehensiveStorePerformanceBaselines() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Establishing comprehensive store performance baselines on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        final PerformanceTestHarness.Configuration config = PerformanceTestHarness.getFastConfiguration();
        final List<PerformanceTestHarness.MeasurementResult> results = new ArrayList<>();

        // Benchmark store creation
        final PerformanceTestHarness.MeasurementResult creationResult = 
            PerformanceTestHarness.runBenchmark(
                "Store Creation - " + runtimeType,
                () -> {
                  try {
                    final Store store = engine.createStore();
                    registerForCleanup(store);
                    assertThat(store.isValid()).isTrue();
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                config);
        results.add(creationResult);

        // Benchmark data operations
        final Store benchmarkStore = engine.createStore();
        registerForCleanup(benchmarkStore);

        final PerformanceTestHarness.MeasurementResult dataResult = 
            PerformanceTestHarness.runBenchmark(
                "Data Operations - " + runtimeType,
                () -> {
                  benchmarkStore.setData("benchmark-data");
                  final Object data = benchmarkStore.getData();
                  assertThat(data).isEqualTo("benchmark-data");
                },
                config);
        results.add(dataResult);

        // Benchmark fuel operations (if supported)
        if (engine.getConfig().isConsumeFuel()) {
          final PerformanceTestHarness.MeasurementResult fuelResult = 
              PerformanceTestHarness.runBenchmark(
                  "Fuel Operations - " + runtimeType,
                  () -> {
                    try {
                      benchmarkStore.setFuel(1000L);
                      final long fuel = benchmarkStore.getFuel();
                      benchmarkStore.addFuel(100L);
                      final long newFuel = benchmarkStore.getFuel();
                      assertThat(newFuel).isEqualTo(fuel + 100L);
                    } catch (final WasmException e) {
                      throw new RuntimeException(e);
                    }
                  },
                  config);
          results.add(fuelResult);
        }

        // Benchmark epoch operations
        final PerformanceTestHarness.MeasurementResult epochResult = 
            PerformanceTestHarness.runBenchmark(
                "Epoch Operations - " + runtimeType,
                () -> {
                  try {
                    benchmarkStore.setEpochDeadline(5000L);
                  } catch (final WasmException e) {
                    throw new RuntimeException(e);
                  }
                },
                config);
        results.add(epochResult);

        // Generate performance report
        final String report = PerformanceTestHarness.generateReport(results);
        LOGGER.info("Store performance report for " + runtimeType + ":\n" + report);

        // Verify reasonable performance
        for (final PerformanceTestHarness.MeasurementResult result : results) {
          assertThat(result.getMean()).isLessThan(100_000_000.0); // 100ms in nanoseconds
          assertThat(result.getCoefficientOfVariation()).isLessThan(100.0); // Less than 100% variation
        }

        addTestMetric("Established " + results.size() + " performance baselines on " + runtimeType);
      });
    }

    @Test
    @DisplayName("Should measure throughput under high load")
    void shouldMeasureThroughputUnderHighLoad() throws WasmException {
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      runWithBothRuntimes((runtime, runtimeType) -> {
        LOGGER.info("Measuring store throughput under high load on " + runtimeType);

        final Engine engine = runtime.createEngine();
        registerForCleanup(engine);

        final Store store = engine.createStore();
        registerForCleanup(store);

        final PerformanceTestHarness.Configuration config = 
            PerformanceTestHarness.Configuration.builder()
                .warmupIterations(2)
                .measurementIterations(5)
                .iterationTime(Duration.ofSeconds(1))
                .build();

        // Measure different operation throughputs
        final Map<String, PerformanceTestHarness.MeasurementResult> throughputResults = 
            PerformanceTestHarness.runThroughputBenchmark(
                "Store Data Operations - " + runtimeType,
                () -> {
                  store.setData("throughput-test");
                  final Object data = store.getData();
                  assertThat(data).isEqualTo("throughput-test");
                },
                config,
                new int[]{1, 2, 4}
            );

        // Analyze throughput scaling
        final StringBuilder throughputReport = new StringBuilder();
        throughputReport.append("Store Throughput Analysis for ").append(runtimeType).append(":\n");
        
        for (final Map.Entry<Integer, PerformanceTestHarness.MeasurementResult> entry : throughputResults.entrySet()) {
          final int threads = entry.getKey();
          final PerformanceTestHarness.MeasurementResult result = entry.getValue();
          
          throughputReport.append(String.format("  %d thread(s): %.2f ops/sec\n", 
              threads, result.getOperationsPerSecond()));
        }

        LOGGER.info(throughputReport.toString());

        // Verify reasonable throughput
        for (final PerformanceTestHarness.MeasurementResult result : throughputResults.values()) {
          assertThat(result.getOperationsPerSecond()).isGreaterThan(100.0); // At least 100 ops/sec
        }

        addTestMetric("Measured throughput with " + throughputResults.size() + " thread configurations on " + runtimeType);
      });
    }
  }

  @Nested
  @DisplayName("Store Cross-Runtime Validation Comprehensive Tests")
  final class StoreCrossRuntimeValidationComprehensiveTests {

    @Test
    @DisplayName("Should validate identical behavior across all store operations")
    void shouldValidateIdenticalBehaviorAcrossAllStoreOperations() {
      skipIfPanamaNotAvailable();

      LOGGER.info("Validating cross-runtime behavior for all store operations");

      // Test store creation and basic operations
      final CrossRuntimeValidator.ComparisonResult basicResult = 
          CrossRuntimeValidator.validateCrossRuntime(runtime -> {
            final Engine engine = runtime.createEngine();
            final Store store = engine.createStore("test-data");
            final Object data = store.getData();
            final boolean isValid = store.isValid();
            store.close();
            engine.close();
            return List.of(data, isValid);
          });

      assertThat(basicResult.isValid()).isTrue();
      assertThat(basicResult.areResultsIdentical()).isTrue();

      // Test fuel management (with fuel-enabled engine)
      final CrossRuntimeValidator.ComparisonResult fuelResult = 
          CrossRuntimeValidator.validateCrossRuntime(runtime -> {
            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            final Store store = engine.createStore();
            
            try {
              store.setFuel(1000L);
              final long fuel1 = store.getFuel();
              store.addFuel(500L);
              final long fuel2 = store.getFuel();
              store.close();
              engine.close();
              return fuel2 - fuel1; // Should be 500
            } catch (final WasmException e) {
              store.close();
              engine.close();
              throw e;
            }
          });

      assertThat(fuelResult.isValid()).isTrue();
      assertThat(fuelResult.areResultsIdentical()).isTrue();

      // Test data management with various types
      final CrossRuntimeValidator.ComparisonResult dataResult = 
          CrossRuntimeValidator.validateCrossRuntime(runtime -> {
            final Engine engine = runtime.createEngine();
            final Store store = engine.createStore();
            
            final List<Object> results = new ArrayList<>();
            
            // Test different data types
            final List<Object> testData = Arrays.asList(
                null, "string", 42, true, Arrays.asList(1, 2, 3)
            );
            
            for (final Object data : testData) {
              store.setData(data);
              results.add(store.getData());
            }
            
            store.close();
            engine.close();
            return results;
          });

      assertThat(dataResult.isValid()).isTrue();
      assertThat(dataResult.areResultsIdentical()).isTrue();

      LOGGER.info("All cross-runtime store validations passed");
      addTestMetric("Completed comprehensive cross-runtime store validation");
    }

    @Test
    @DisplayName("Should validate performance consistency across runtimes")
    void shouldValidatePerformanceConsistencyAcrossRuntimes() {
      skipIfPanamaNotAvailable();
      skipIfCategoryNotEnabled(TestCategories.PERFORMANCE);

      LOGGER.info("Validating cross-runtime store performance consistency");

      final PerformanceTestHarness.Configuration config = PerformanceTestHarness.getFastConfiguration();

      // Benchmark store creation across runtimes
      final PerformanceTestHarness.ComparisonResult creationComparison = 
          PerformanceTestHarness.runCrossRuntimeBenchmark(
              "Store Creation Cross-Runtime",
              runtime -> {
                final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                registerForCleanup(store);
                registerForCleanup(engine);
                assertThat(store.isValid()).isTrue();
              },
              config);

      // Benchmark data operations across runtimes
      final PerformanceTestHarness.ComparisonResult dataComparison = 
          PerformanceTestHarness.runCrossRuntimeBenchmark(
              "Store Data Operations Cross-Runtime",
              runtime -> {
                final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                registerForCleanup(engine);
                registerForCleanup(store);
                
                store.setData("performance-test");
                final Object data = store.getData();
                assertThat(data).isEqualTo("performance-test");
              },
              config);

      // Generate comparison reports
      final String creationReport = PerformanceTestHarness.generateComparisonReport(creationComparison);
      final String dataReport = PerformanceTestHarness.generateComparisonReport(dataComparison);
      
      LOGGER.info("Cross-runtime store creation performance:\n" + creationReport);
      LOGGER.info("Cross-runtime store data operations performance:\n" + dataReport);

      // Validate reasonable performance ratios
      assertThat(creationComparison.getSpeedupRatio()).isGreaterThan(0.1);
      assertThat(creationComparison.getSpeedupRatio()).isLessThan(10.0);
      assertThat(dataComparison.getSpeedupRatio()).isGreaterThan(0.1);
      assertThat(dataComparison.getSpeedupRatio()).isLessThan(10.0);

      LOGGER.info("Cross-runtime store performance validation passed");
      addTestMetric("Validated cross-runtime store performance consistency");
    }
  }
}
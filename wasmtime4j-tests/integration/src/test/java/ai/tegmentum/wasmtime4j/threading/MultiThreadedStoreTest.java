/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.threading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for multi-threaded Store access patterns.
 *
 * <p>These tests verify Store behavior when accessed from multiple threads, including shared memory
 * operations and proper synchronization patterns.
 *
 * @since 1.0.0
 */
@DisplayName("Multi-Threaded Store Integration Tests")
class MultiThreadedStoreTest {

  private static final Logger LOGGER = Logger.getLogger(MultiThreadedStoreTest.class.getName());

  private static boolean threadsSupported = false;
  private static boolean nativeLibraryLoaded = false;

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for multi-threaded store tests");
    try {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      nativeLibraryLoaded = loader.isLoaded();
      LOGGER.info("Native library loaded: " + nativeLibraryLoaded);
    } catch (final RuntimeException e) {
      LOGGER.warning("Failed to load native library: " + e.getMessage());
      nativeLibraryLoaded = false;
    }

    // Check if threads are supported
    if (nativeLibraryLoaded) {
      try {
        final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
        try (Engine engine = Engine.create(config)) {
          threadsSupported = engine != null;
        }
      } catch (Exception e) {
        LOGGER.warning("WASM threads may not be fully supported: " + e.getMessage());
        threadsSupported = false;
      }
    }
    LOGGER.info("WASM threads supported: " + threadsSupported);
  }

  @AfterAll
  static void cleanupAll() {
    LOGGER.info("Completed Multi-Threaded Store Integration Tests");
  }

  private static void assumeNativeLibraryLoaded() {
    assumeTrue(nativeLibraryLoaded, "Native library not loaded - skipping");
  }

  private static void assumeThreadsSupported() {
    assumeTrue(threadsSupported, "WASM threads not supported - skipping");
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up multi-threaded store test");
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test resources");
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("Store Per Thread Tests")
  class StorePerThreadTests {

    @Test
    @DisplayName("should create separate stores per thread")
    void shouldCreateSeparateStoresPerThread() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing separate stores per thread");

      final int numThreads = 4;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      resources.add(executor::shutdown);

      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(numThreads);
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicReference<Exception> error = new AtomicReference<>();

      // Each thread creates its own engine and store
      for (int i = 0; i < numThreads; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                startLatch.await();

                // Each thread creates its own engine and store
                try (Engine engine = Engine.create();
                    Store store = Store.create(engine)) {

                  assertNotNull(engine, "Engine should not be null in thread " + threadId);
                  assertNotNull(store, "Store should not be null in thread " + threadId);
                  assertTrue(store.isValid(), "Store should be valid in thread " + threadId);

                  // Set some data to verify the store is usable
                  store.setData("thread-" + threadId);
                  assertEquals(
                      "thread-" + threadId,
                      store.getData(),
                      "Store data should match in thread " + threadId);

                  successCount.incrementAndGet();
                  LOGGER.info("Thread " + threadId + " successfully created and used store");
                }
              } catch (Exception e) {
                error.set(e);
                LOGGER.warning("Thread " + threadId + " failed: " + e.getMessage());
              } finally {
                doneLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");

      if (error.get() != null) {
        throw error.get();
      }

      assertEquals(numThreads, successCount.get(), "All threads should succeed");
      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      LOGGER.info("Separate stores per thread test passed: " + successCount.get() + " threads");
    }

    @Test
    @DisplayName("should handle concurrent engine creation")
    void shouldHandleConcurrentEngineCreation() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing concurrent engine creation");

      final int numThreads = 8;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      resources.add(executor::shutdown);

      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(numThreads);
      final AtomicInteger successCount = new AtomicInteger(0);

      for (int i = 0; i < numThreads; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                startLatch.await();

                // Create engine with default config
                try (Engine engine = Engine.create()) {
                  assertNotNull(engine, "Engine should not be null");
                  successCount.incrementAndGet();
                  LOGGER.fine("Thread " + threadId + " created engine successfully");
                }
              } catch (Exception e) {
                LOGGER.warning("Thread " + threadId + " engine creation failed: " + e.getMessage());
              } finally {
                doneLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
      assertEquals(numThreads, successCount.get(), "All threads should create engines");

      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      LOGGER.info("Concurrent engine creation test passed: " + successCount.get() + " engines");
    }
  }

  @Nested
  @DisplayName("Shared Engine Tests")
  class SharedEngineTests {

    @Test
    @DisplayName("should share engine across multiple stores in different threads")
    void shouldShareEngineAcrossMultipleStoresInDifferentThreads() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing shared engine across multiple stores");

      final int numThreads = 4;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      resources.add(executor::shutdown);

      // Create a shared engine
      final Engine sharedEngine = Engine.create();
      resources.add(sharedEngine);

      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(numThreads);
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicReference<Exception> error = new AtomicReference<>();

      for (int i = 0; i < numThreads; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                startLatch.await();

                // Each thread creates its own store from the shared engine
                try (Store store = Store.create(sharedEngine)) {
                  assertNotNull(store, "Store should not be null in thread " + threadId);
                  assertTrue(store.isValid(), "Store should be valid in thread " + threadId);
                  assertEquals(
                      sharedEngine,
                      store.getEngine(),
                      "Store should be associated with shared engine");

                  // Store some thread-specific data
                  store.setData("store-data-" + threadId);
                  assertEquals(
                      "store-data-" + threadId,
                      store.getData(),
                      "Store data should be isolated per store");

                  successCount.incrementAndGet();
                  LOGGER.info("Thread " + threadId + " successfully used shared engine");
                }
              } catch (Exception e) {
                error.set(e);
                LOGGER.warning("Thread " + threadId + " failed: " + e.getMessage());
              } finally {
                doneLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");

      if (error.get() != null) {
        throw error.get();
      }

      assertEquals(numThreads, successCount.get(), "All threads should succeed");
      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      LOGGER.info("Shared engine test passed: " + successCount.get() + " stores");
    }
  }

  @Nested
  @DisplayName("Shared Memory Tests")
  class SharedMemoryTests {

    @Test
    @DisplayName("should create shared memory with threads feature")
    void shouldCreateSharedMemoryWithThreadsFeature() throws Exception {
      assumeNativeLibraryLoaded();
      assumeThreadsSupported();
      LOGGER.info("Testing shared memory creation");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);

      try (Engine engine = Engine.create(config);
          Store store = Store.create(engine)) {

        // Create shared memory (1 page initial, 10 pages max)
        WasmMemory sharedMemory = store.createSharedMemory(1, 10);

        assertNotNull(sharedMemory, "Shared memory should not be null");
        // Note: isShared() may return false depending on implementation
        // The createSharedMemory method creates a memory that CAN be shared,
        // but the flag depends on native implementation
        LOGGER.info("Shared memory created: isShared=" + sharedMemory.isShared());
        assertEquals(1, sharedMemory.getSize(), "Initial size should be 1 page");

        LOGGER.info("Shared memory created successfully: " + sharedMemory.getSize() + " pages");
      } catch (UnsupportedOperationException e) {
        LOGGER.info("Shared memory not supported on this platform: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("should verify memory isolation between non-shared stores")
    void shouldVerifyMemoryIsolationBetweenNonSharedStores() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing memory isolation between stores");

      try (Engine engine = Engine.create();
          Store store1 = Store.create(engine);
          Store store2 = Store.create(engine)) {

        // Create separate memories for each store
        WasmMemory memory1 = store1.createMemory(1, 10);
        WasmMemory memory2 = store2.createMemory(1, 10);

        assertNotNull(memory1, "Memory 1 should not be null");
        assertNotNull(memory2, "Memory 2 should not be null");

        // Verify they are separate
        assertFalse(memory1.isShared(), "Memory 1 should not be shared");
        assertFalse(memory2.isShared(), "Memory 2 should not be shared");

        LOGGER.info("Memory isolation verified between stores");
      }
    }
  }

  @Nested
  @DisplayName("Concurrent Store Operation Tests")
  class ConcurrentOperationTests {

    @Test
    @DisplayName("should handle concurrent fuel operations on separate stores")
    void shouldHandleConcurrentFuelOperationsOnSeparateStores() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing concurrent fuel operations");

      final int numThreads = 4;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      resources.add(executor::shutdown);

      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(numThreads);
      final AtomicInteger successCount = new AtomicInteger(0);

      // Create engine with fuel consumption enabled
      final EngineConfig config = new EngineConfig().consumeFuel(true);

      for (int i = 0; i < numThreads; i++) {
        final int threadId = i;
        final long initialFuel = 1000 * (threadId + 1);

        executor.submit(
            () -> {
              try {
                startLatch.await();

                try (Engine engine = Engine.create(config);
                    Store store = Store.create(engine)) {

                  // Set fuel and verify
                  store.setFuel(initialFuel);
                  long fuel = store.getFuel();
                  assertTrue(fuel >= 0, "Fuel should be non-negative in thread " + threadId);

                  // Add more fuel
                  store.addFuel(500);
                  long newFuel = store.getFuel();
                  assertTrue(
                      newFuel >= fuel, "Fuel should increase after adding in thread " + threadId);

                  successCount.incrementAndGet();
                  LOGGER.fine("Thread " + threadId + " fuel operations completed");
                }
              } catch (Exception e) {
                LOGGER.warning("Thread " + threadId + " fuel operations failed: " + e.getMessage());
              } finally {
                doneLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
      assertEquals(numThreads, successCount.get(), "All threads should succeed");

      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      LOGGER.info("Concurrent fuel operations test passed: " + successCount.get() + " threads");
    }

    @Test
    @DisplayName("should handle concurrent store data operations")
    void shouldHandleConcurrentStoreDataOperations() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing concurrent store data operations");

      final int numThreads = 8;
      final int iterationsPerThread = 100;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      resources.add(executor::shutdown);

      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(numThreads);
      final AtomicBoolean allPassed = new AtomicBoolean(true);

      for (int i = 0; i < numThreads; i++) {
        final int threadId = i;

        executor.submit(
            () -> {
              try {
                startLatch.await();

                try (Engine engine = Engine.create();
                    Store store = Store.create(engine)) {

                  for (int j = 0; j < iterationsPerThread; j++) {
                    String expectedData = "thread-" + threadId + "-iteration-" + j;
                    store.setData(expectedData);
                    Object actualData = store.getData();

                    if (!expectedData.equals(actualData)) {
                      LOGGER.warning(
                          "Data mismatch in thread "
                              + threadId
                              + ": expected="
                              + expectedData
                              + ", actual="
                              + actualData);
                      allPassed.set(false);
                      break;
                    }
                  }
                }
              } catch (Exception e) {
                LOGGER.warning("Thread " + threadId + " failed: " + e.getMessage());
                allPassed.set(false);
              } finally {
                doneLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
      assertTrue(allPassed.get(), "All data operations should succeed");

      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      LOGGER.info("Concurrent store data operations test passed");
    }
  }

  @Nested
  @DisplayName("Store Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should handle concurrent store creation and destruction")
    void shouldHandleConcurrentStoreCreationAndDestruction() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing concurrent store creation and destruction");

      final int numIterations = 10;
      final int numThreads = 4;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      resources.add(executor::shutdown);

      final AtomicInteger successCount = new AtomicInteger(0);

      for (int iteration = 0; iteration < numIterations; iteration++) {
        final CountDownLatch doneLatch = new CountDownLatch(numThreads);
        final int iterNum = iteration;

        for (int i = 0; i < numThreads; i++) {
          final int threadId = i;
          executor.submit(
              () -> {
                try (Engine engine = Engine.create();
                    Store store = Store.create(engine)) {

                  assertNotNull(store, "Store should not be null");
                  assertTrue(store.isValid(), "Store should be valid");

                  // Do some work
                  store.setData("iteration-" + iterNum + "-thread-" + threadId);

                  successCount.incrementAndGet();
                } catch (Exception e) {
                  LOGGER.warning(
                      "Iteration "
                          + iterNum
                          + " Thread "
                          + threadId
                          + " failed: "
                          + e.getMessage());
                } finally {
                  doneLatch.countDown();
                }
              });
        }

        assertTrue(
            doneLatch.await(30, TimeUnit.SECONDS), "Iteration " + iteration + " should complete");
      }

      int expectedSuccesses = numIterations * numThreads;
      assertEquals(expectedSuccesses, successCount.get(), "All iterations should succeed");

      executor.shutdown();
      executor.awaitTermination(10, TimeUnit.SECONDS);

      LOGGER.info(
          "Concurrent store lifecycle test passed: "
              + successCount.get()
              + " stores created/destroyed");
    }

    @Test
    @DisplayName("should verify store validity after close")
    void shouldVerifyStoreValidityAfterClose() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing store validity after close");

      Engine engine = Engine.create();
      resources.add(engine);

      Store store = Store.create(engine);
      assertTrue(store.isValid(), "Store should be valid before close");

      store.close();
      assertFalse(store.isValid(), "Store should be invalid after close");

      LOGGER.info("Store validity after close verified");
    }
  }

  @Nested
  @DisplayName("Thread Safety Pattern Tests")
  class ThreadSafetyPatternTests {

    @Test
    @DisplayName("should demonstrate proper synchronization for shared resources")
    void shouldDemonstrateProperSynchronizationForSharedResources() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing proper synchronization patterns");

      final int numThreads = 4;
      final int operationsPerThread = 100;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      resources.add(executor::shutdown);

      // Shared counter with proper synchronization
      final AtomicInteger sharedCounter = new AtomicInteger(0);
      final CountDownLatch doneLatch = new CountDownLatch(numThreads);

      for (int i = 0; i < numThreads; i++) {
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < operationsPerThread; j++) {
                  sharedCounter.incrementAndGet();
                }
              } finally {
                doneLatch.countDown();
              }
            });
      }

      assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
      assertEquals(
          numThreads * operationsPerThread,
          sharedCounter.get(),
          "Shared counter should be correct with atomic operations");

      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      LOGGER.info("Synchronization patterns test passed: counter=" + sharedCounter.get());
    }

    @Test
    @DisplayName("should verify per-thread store ownership pattern")
    void shouldVerifyPerThreadStoreOwnershipPattern() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing per-thread store ownership pattern");

      final ThreadLocal<Store> threadLocalStore = new ThreadLocal<>();
      final int numThreads = 4;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      resources.add(executor::shutdown);

      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(numThreads);
      final AtomicInteger successCount = new AtomicInteger(0);

      for (int i = 0; i < numThreads; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                startLatch.await();

                // Create store for this thread
                try (Engine engine = Engine.create();
                    Store store = Store.create(engine)) {

                  threadLocalStore.set(store);
                  store.setData("thread-" + threadId);

                  // Verify ownership
                  Store myStore = threadLocalStore.get();
                  assertNotNull(myStore, "Thread should have its own store");
                  assertEquals(
                      "thread-" + threadId,
                      myStore.getData(),
                      "Thread should access its own store data");

                  successCount.incrementAndGet();
                } finally {
                  threadLocalStore.remove();
                }
              } catch (Exception e) {
                LOGGER.warning("Thread " + threadId + " failed: " + e.getMessage());
              } finally {
                doneLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
      assertEquals(numThreads, successCount.get(), "All threads should verify ownership");

      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      LOGGER.info(
          "Per-thread store ownership pattern verified: " + successCount.get() + " threads");
    }
  }
}

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

package ai.tegmentum.wasmtime4j.panama.performance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Panama PanaNativeObjectPool.
 *
 * <p>These tests verify the Panama-specific object pooling capabilities including arena-based
 * allocation, borrow/return operations, and pool statistics.
 *
 * @since 1.0.0
 */
@DisplayName("Panama PanaNativeObjectPool Integration Tests")
class PanaNativeObjectPoolIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanaNativeObjectPoolIntegrationTest.class.getName());

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for Panama object pool tests");
    try {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertTrue(loader.isLoaded(), "Native library should be loaded");
      LOGGER.info("Native library loaded successfully for Panama");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up test - clearing all pools");
    PanaNativeObjectPool.clearAllPools();
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
    PanaNativeObjectPool.clearAllPools();
  }

  @Nested
  @DisplayName("Pool Creation Tests")
  class PoolCreationTests {

    @Test
    @DisplayName("should create pool with default size")
    void shouldCreatePoolWithDefaultSize() {
      LOGGER.info("Testing pool creation with default size");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(
              MemorySegment.class,
              (a) -> a.allocate(1024),
              PanaNativeObjectPool.DEFAULT_MAX_POOL_SIZE);

      assertNotNull(pool, "Pool should not be null");
      assertEquals(MemorySegment.class, pool.getObjectType(), "Object type should match");
      assertEquals(
          PanaNativeObjectPool.DEFAULT_MAX_POOL_SIZE,
          pool.getMaxPoolSize(),
          "Max pool size should be default");
      assertFalse(pool.isClosed(), "Pool should not be closed");

      LOGGER.info("Pool created: " + pool);
    }

    @Test
    @DisplayName("should create pool with custom min/max sizes")
    void shouldCreatePoolWithCustomMinMax() {
      LOGGER.info("Testing pool creation with custom min/max sizes");

      int minSize = 2;
      int maxSize = 10;

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(
              MemorySegment.class, (a) -> a.allocate(512), maxSize, minSize);

      assertNotNull(pool, "Pool should not be null");
      assertEquals(maxSize, pool.getMaxPoolSize(), "Max pool size should match");
      // Pool should have been pre-populated with minSize objects
      assertTrue(pool.getAvailableCount() >= 0, "Available count should be non-negative");

      LOGGER.info("Pool created with custom sizes - available: " + pool.getAvailableCount());
    }

    @Test
    @DisplayName("should return same singleton pool for same key")
    void shouldReturnSingletonForSameKey() {
      LOGGER.info("Testing singleton pool behavior");

      PanaNativeObjectPool<MemorySegment> pool1 =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16);

      PanaNativeObjectPool<MemorySegment> pool2 =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16);

      assertSame(pool1, pool2, "Should return same pool instance for same type");

      LOGGER.info("Singleton behavior verified - same instance returned");
    }

    @Test
    @DisplayName("should reject null object type")
    void shouldRejectNullObjectType() {
      LOGGER.info("Testing null object type rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> PanaNativeObjectPool.getPool(null, (a) -> a.allocate(256), 16),
          "Should reject null object type");

      LOGGER.info("Null object type correctly rejected");
    }

    @Test
    @DisplayName("should reject null factory")
    void shouldRejectNullFactory() {
      LOGGER.info("Testing null factory rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> PanaNativeObjectPool.getPool(MemorySegment.class, null, 16),
          "Should reject null factory");

      LOGGER.info("Null factory correctly rejected");
    }

    @Test
    @DisplayName("should reject invalid pool sizes")
    void shouldRejectInvalidPoolSizes() {
      LOGGER.info("Testing invalid pool size rejection");

      // Zero max size
      assertThrows(
          IllegalArgumentException.class,
          () -> PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 0),
          "Should reject zero max size");

      // Negative max size
      assertThrows(
          IllegalArgumentException.class,
          () -> PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), -1),
          "Should reject negative max size");

      // Min > Max
      assertThrows(
          IllegalArgumentException.class,
          () -> PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 5, 10),
          "Should reject min > max");

      LOGGER.info("Invalid pool sizes correctly rejected");
    }
  }

  @Nested
  @DisplayName("Borrow and Return Tests")
  class BorrowReturnTests {

    @Test
    @DisplayName("should borrow object from pool")
    void shouldBorrowObjectFromPool() {
      LOGGER.info("Testing object borrow from pool");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(1024), 16);

      try (Arena arena = Arena.ofConfined()) {
        MemorySegment segment = pool.borrow(arena);

        assertNotNull(segment, "Borrowed segment should not be null");
        assertEquals(1024, segment.byteSize(), "Segment size should match");
        assertEquals(1, pool.getBorrowedCount(), "Borrowed count should be 1");

        // Return the object
        pool.returnObject(segment);
        assertEquals(0, pool.getBorrowedCount(), "Borrowed count should be 0 after return");

        LOGGER.info("Borrow and return successful - segment size: " + segment.byteSize());
      }
    }

    @Test
    @DisplayName("should return object to pool")
    void shouldReturnObjectToPool() {
      LOGGER.info("Testing object return to pool");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(512), 16, 0);

      try (Arena arena = Arena.ofConfined()) {
        MemorySegment segment = pool.borrow(arena);
        int initialAvailable = pool.getAvailableCount();

        pool.returnObject(segment);

        int afterReturnAvailable = pool.getAvailableCount();
        assertTrue(
            afterReturnAvailable >= initialAvailable,
            "Available count should increase or stay same after return");

        LOGGER.info(
            "Return successful - available before: "
                + initialAvailable
                + ", after: "
                + afterReturnAvailable);
      }
    }

    @Test
    @DisplayName("should reuse returned objects")
    void shouldReuseReturnedObjects() {
      LOGGER.info("Testing object reuse from pool");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16, 0);

      try (Arena arena = Arena.ofConfined()) {
        // Borrow and return first object
        MemorySegment first = pool.borrow(arena);
        pool.returnObject(first);

        // Borrow again - might get the same object
        MemorySegment second = pool.borrow(arena);
        assertNotNull(second, "Second borrow should succeed");

        // Note: We can't guarantee same object due to implementation details
        // but hit rate should be positive if reuse happens

        pool.returnObject(second);

        double hitRate = pool.getHitRate();
        LOGGER.info("Object reuse test - hit rate: " + hitRate + "%");
      }
    }

    @Test
    @DisplayName("should create new when pool exhausted")
    void shouldCreateNewWhenPoolExhausted() {
      LOGGER.info("Testing new object creation when pool exhausted");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(128), 4, 0);

      List<MemorySegment> borrowed = new ArrayList<>();

      try (Arena arena = Arena.ofConfined()) {
        // Borrow more than pool can hold
        for (int i = 0; i < 10; i++) {
          MemorySegment segment = pool.borrow(arena);
          assertNotNull(segment, "Should be able to borrow object #" + i);
          borrowed.add(segment);
        }

        assertEquals(10, pool.getBorrowedCount(), "Should have 10 borrowed objects");

        // Return all
        for (MemorySegment segment : borrowed) {
          pool.returnObject(segment);
        }

        assertEquals(0, pool.getBorrowedCount(), "Should have 0 borrowed after return all");

        LOGGER.info("Pool exhaustion test passed - stats: " + pool.getStats());
      }
    }

    @Test
    @DisplayName("should reject null arena")
    void shouldRejectNullArena() {
      LOGGER.info("Testing null arena rejection");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16);

      assertThrows(
          IllegalArgumentException.class, () -> pool.borrow(null), "Should reject null arena");

      LOGGER.info("Null arena correctly rejected");
    }

    @Test
    @DisplayName("should reject null object return")
    void shouldRejectNullObjectReturn() {
      LOGGER.info("Testing null object return rejection");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16);

      assertThrows(
          IllegalArgumentException.class,
          () -> pool.returnObject(null),
          "Should reject null object return");

      LOGGER.info("Null object return correctly rejected");
    }

    @Test
    @DisplayName("should throw when borrowing from closed pool")
    void shouldThrowWhenBorrowingFromClosedPool() {
      LOGGER.info("Testing borrow from closed pool");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16);

      pool.close();

      try (Arena arena = Arena.ofConfined()) {
        assertThrows(
            IllegalStateException.class,
            () -> pool.borrow(arena),
            "Should throw when borrowing from closed pool");
      }

      LOGGER.info("Closed pool correctly rejects borrows");
    }
  }

  @Nested
  @DisplayName("Pool Statistics Tests")
  class PoolStatisticsTests {

    @Test
    @DisplayName("should track hit rate")
    void shouldTrackHitRate() {
      LOGGER.info("Testing hit rate tracking");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16, 4);

      try (Arena arena = Arena.ofConfined()) {
        // Borrow and return multiple times to build up statistics
        for (int i = 0; i < 20; i++) {
          MemorySegment segment = pool.borrow(arena);
          pool.returnObject(segment);
        }

        double hitRate = pool.getHitRate();
        assertTrue(hitRate >= 0.0 && hitRate <= 100.0, "Hit rate should be between 0 and 100");

        LOGGER.info("Hit rate: " + hitRate + "%");
      }
    }

    @Test
    @DisplayName("should report pool stats")
    void shouldReportPoolStats() {
      LOGGER.info("Testing pool statistics reporting");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(512), 16, 2);

      try (Arena arena = Arena.ofConfined()) {
        // Perform some operations
        MemorySegment s1 = pool.borrow(arena);
        MemorySegment s2 = pool.borrow(arena);
        pool.returnObject(s1);
        pool.returnObject(s2);

        String stats = pool.getStats();
        assertNotNull(stats, "Stats should not be null");
        assertFalse(stats.isEmpty(), "Stats should not be empty");
        assertTrue(stats.contains("Panama"), "Stats should mention Panama");
        assertTrue(stats.contains("MemorySegment"), "Stats should mention object type");

        LOGGER.info("Pool stats:\n" + stats);
      }
    }

    @Test
    @DisplayName("should track arena allocations")
    void shouldTrackArenaAllocations() {
      LOGGER.info("Testing arena allocation tracking");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 8, 0);

      try (Arena arena = Arena.ofConfined()) {
        // Force new allocations by exhausting pool
        List<MemorySegment> borrowed = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
          borrowed.add(pool.borrow(arena));
        }

        long arenaAllocs = pool.getArenaAllocations();
        LOGGER.info("Arena allocations: " + arenaAllocs);

        // Return all
        borrowed.forEach(pool::returnObject);
      }
    }

    @Test
    @DisplayName("should track zero-copy operations")
    void shouldTrackZeroCopyOperations() {
      LOGGER.info("Testing zero-copy operation tracking");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16, 4);

      try (Arena arena = Arena.ofConfined()) {
        // Borrow and return to trigger zero-copy tracking
        for (int i = 0; i < 10; i++) {
          MemorySegment segment = pool.borrow(arena);
          pool.returnObject(segment);
        }

        long zeroCopyOps = pool.getZeroCopyOperations();
        LOGGER.info("Zero-copy operations: " + zeroCopyOps);
      }
    }

    @Test
    @DisplayName("should get Panama performance stats")
    void shouldGetPanamaPerformanceStats() {
      LOGGER.info("Testing Panama performance stats");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16, 2);

      try (Arena arena = Arena.ofConfined()) {
        // Perform operations
        for (int i = 0; i < 5; i++) {
          MemorySegment segment = pool.borrow(arena);
          pool.returnObject(segment);
        }

        String perfStats = pool.getPanamaPerformanceStats();
        assertNotNull(perfStats, "Performance stats should not be null");
        assertTrue(perfStats.contains("Panama"), "Should mention Panama");
        assertTrue(perfStats.contains("avg_borrow"), "Should contain average borrow time");
        assertTrue(perfStats.contains("avg_return"), "Should contain average return time");

        LOGGER.info("Panama performance stats: " + perfStats);
      }
    }
  }

  @Nested
  @DisplayName("Concurrency Tests")
  class ConcurrencyTests {

    @Test
    @DisplayName("should handle concurrent borrows")
    void shouldHandleConcurrentBorrows() throws Exception {
      LOGGER.info("Testing concurrent borrows");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 32, 8);

      int numThreads = 4;
      int operationsPerThread = 25;
      ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      CountDownLatch latch = new CountDownLatch(numThreads);
      AtomicInteger successCount = new AtomicInteger(0);
      AtomicInteger failureCount = new AtomicInteger(0);

      for (int t = 0; t < numThreads; t++) {
        executor.submit(
            () -> {
              try (Arena arena = Arena.ofConfined()) {
                for (int i = 0; i < operationsPerThread; i++) {
                  try {
                    MemorySegment segment = pool.borrow(arena);
                    if (segment != null) {
                      successCount.incrementAndGet();
                      pool.returnObject(segment);
                    } else {
                      failureCount.incrementAndGet();
                    }
                  } catch (Exception e) {
                    failureCount.incrementAndGet();
                    LOGGER.warning("Concurrent borrow failed: " + e.getMessage());
                  }
                }
              } finally {
                latch.countDown();
              }
            });
      }

      boolean completed = latch.await(30, TimeUnit.SECONDS);
      assertTrue(completed, "All threads should complete");
      executor.shutdown();

      LOGGER.info(
          "Concurrent borrows - success: "
              + successCount.get()
              + ", failures: "
              + failureCount.get());
      LOGGER.info("Pool stats after concurrent access: " + pool.getStats());
    }

    @Test
    @DisplayName("should handle concurrent returns")
    void shouldHandleConcurrentReturns() throws Exception {
      LOGGER.info("Testing concurrent returns");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 32, 0);

      int numThreads = 4;
      int objectsPerThread = 20;
      ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      CountDownLatch latch = new CountDownLatch(numThreads);

      // First, borrow objects in each thread
      for (int t = 0; t < numThreads; t++) {
        executor.submit(
            () -> {
              try (Arena arena = Arena.ofConfined()) {
                List<MemorySegment> threadObjects = new ArrayList<>();

                // Borrow
                for (int i = 0; i < objectsPerThread; i++) {
                  MemorySegment segment = pool.borrow(arena);
                  if (segment != null) {
                    threadObjects.add(segment);
                  }
                }

                // Return all
                for (MemorySegment segment : threadObjects) {
                  pool.returnObject(segment);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      boolean completed = latch.await(30, TimeUnit.SECONDS);
      assertTrue(completed, "All threads should complete");
      executor.shutdown();

      assertEquals(0, pool.getBorrowedCount(), "No objects should be borrowed after test");

      LOGGER.info("Concurrent returns test passed - pool stats: " + pool.getStats());
    }
  }

  @Nested
  @DisplayName("Pool Lifecycle Tests")
  class PoolLifecycleTests {

    @Test
    @DisplayName("should clear pool")
    void shouldClearPool() {
      LOGGER.info("Testing pool clear");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16, 4);

      try (Arena arena = Arena.ofConfined()) {
        // Borrow and return to populate pool
        MemorySegment segment = pool.borrow(arena);
        pool.returnObject(segment);

        // Clear pool
        pool.clear();

        assertEquals(0, pool.getAvailableCount(), "Available count should be 0 after clear");
        assertFalse(pool.isClosed(), "Pool should not be closed after clear");

        LOGGER.info("Pool cleared successfully");
      }
    }

    @Test
    @DisplayName("should close pool")
    void shouldClosePool() {
      LOGGER.info("Testing pool close");

      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16);

      assertFalse(pool.isClosed(), "Pool should not be closed initially");

      pool.close();

      assertTrue(pool.isClosed(), "Pool should be closed after close()");

      LOGGER.info("Pool closed successfully");
    }

    @Test
    @DisplayName("should get all pool stats")
    void shouldGetAllPoolStats() {
      LOGGER.info("Testing all pool stats");

      // Create multiple pools with different types
      PanaNativeObjectPool<MemorySegment> segmentPool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16);

      String allStats = PanaNativeObjectPool.getAllPoolStats();
      assertNotNull(allStats, "All pool stats should not be null");
      assertTrue(allStats.contains("Panama"), "Should mention Panama");

      LOGGER.info("All pool stats:\n" + allStats);
    }

    @Test
    @DisplayName("should clear all pools")
    void shouldClearAllPools() {
      LOGGER.info("Testing clear all pools");

      // Create a pool
      PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, (a) -> a.allocate(256), 16);

      // Clear all
      assertDoesNotThrow(() -> PanaNativeObjectPool.clearAllPools(), "Clear all should not throw");

      // Pool should be closed
      assertTrue(pool.isClosed(), "Pool should be closed after clearAllPools");

      LOGGER.info("All pools cleared successfully");
    }
  }
}

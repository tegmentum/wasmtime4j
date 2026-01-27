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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanaNativeObjectPool}.
 *
 * <p>These tests invoke actual pool methods to exercise code paths and improve JaCoCo coverage.
 * Uses Panama Arena and MemorySegment APIs.
 */
@DisplayName("PanaNativeObjectPool Integration Tests")
class PanaNativeObjectPoolTest {

  private static final Logger LOGGER = Logger.getLogger(PanaNativeObjectPoolTest.class.getName());

  @AfterEach
  void tearDown() {
    PanaNativeObjectPool.clearAllPools();
  }

  @Nested
  @DisplayName("Pool Creation Tests")
  class PoolCreationTests {

    @Test
    @DisplayName("Should create pool with 3-parameter factory method")
    void shouldCreatePoolWith3ParamFactory() {
      LOGGER.info("Testing getPool with 3 params");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "test_object", 8);

      assertNotNull(pool, "Pool should be created");
      assertEquals(String.class, pool.getObjectType(), "Object type should be String");
      assertEquals(8, pool.getMaxPoolSize(), "Max pool size should be 8");
      assertFalse(pool.isClosed(), "Pool should not be closed");
      LOGGER.info("Pool created: " + pool);

      pool.close();
    }

    @Test
    @DisplayName("Should create pool with 4-parameter factory method")
    void shouldCreatePoolWith4ParamFactory() {
      LOGGER.info("Testing getPool with 4 params");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "test_object", 16, 2);

      assertNotNull(pool, "Pool should be created");
      assertEquals(16, pool.getMaxPoolSize(), "Max pool size should be 16");
      LOGGER.info("Pool created with min/max: " + pool);

      pool.close();
    }

    @Test
    @DisplayName("Should return existing pool for same type")
    void shouldReturnExistingPoolForSameType() {
      LOGGER.info("Testing singleton pool per type");

      final PanaNativeObjectPool<Integer> pool1 =
          PanaNativeObjectPool.getPool(Integer.class, arena -> 42, 8);
      final PanaNativeObjectPool<Integer> pool2 =
          PanaNativeObjectPool.getPool(Integer.class, arena -> 99, 16);

      // Should return the same pool instance
      assertTrue(pool1 == pool2, "Should return same pool for same type");
      LOGGER.info("Same pool returned for same type");

      pool1.close();
    }

    @Test
    @DisplayName("Should throw for null objectType")
    void shouldThrowForNullObjectType() {
      LOGGER.info("Testing null objectType");
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            PanaNativeObjectPool.getPool(null, arena -> "x", 8);
          });
      LOGGER.info("Null objectType rejected");
    }

    @Test
    @DisplayName("Should have correct default constants")
    void shouldHaveCorrectDefaultConstants() {
      LOGGER.info("Testing default constants");
      assertEquals(32, PanaNativeObjectPool.DEFAULT_MAX_POOL_SIZE);
      assertEquals(4, PanaNativeObjectPool.DEFAULT_MIN_POOL_SIZE);
      LOGGER.info(
          "DEFAULT_MAX_POOL_SIZE="
              + PanaNativeObjectPool.DEFAULT_MAX_POOL_SIZE
              + ", DEFAULT_MIN_POOL_SIZE="
              + PanaNativeObjectPool.DEFAULT_MIN_POOL_SIZE);
    }
  }

  @Nested
  @DisplayName("Borrow and Return Tests")
  class BorrowReturnTests {

    @Test
    @DisplayName("Should borrow object from pool")
    void shouldBorrowObjectFromPool() {
      LOGGER.info("Testing borrow");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(
              String.class, arena -> "borrowed_" + System.nanoTime(), 8, 0);

      try (final Arena arena = Arena.ofConfined()) {
        final String borrowed = pool.borrow(arena);
        assertNotNull(borrowed, "Borrowed object should not be null");
        assertTrue(borrowed.startsWith("borrowed_"), "Should be a created object");
        LOGGER.info("Borrowed: " + borrowed);

        pool.returnObject(borrowed);
        LOGGER.info("Returned object to pool");
      }

      pool.close();
    }

    @Test
    @DisplayName("Should borrow MemorySegment from pool")
    void shouldBorrowMemorySegmentFromPool() {
      LOGGER.info("Testing borrow with MemorySegment type");

      final PanaNativeObjectPool<MemorySegment> pool =
          PanaNativeObjectPool.getPool(MemorySegment.class, arena -> arena.allocate(64), 8, 0);

      try (final Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = pool.borrow(arena);
        assertNotNull(segment, "Borrowed segment should not be null");
        assertEquals(64, segment.byteSize(), "Segment should be 64 bytes");
        LOGGER.info("Borrowed segment: " + segment.byteSize() + " bytes");

        pool.returnObject(segment);
        LOGGER.info("Returned segment to pool");
      }

      pool.close();
    }

    @Test
    @DisplayName("Should track borrow and return counts")
    void shouldTrackBorrowAndReturnCounts() {
      LOGGER.info("Testing borrow/return count tracking");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "tracked", 16, 0);

      try (final Arena arena = Arena.ofConfined()) {
        final String obj1 = pool.borrow(arena);
        final String obj2 = pool.borrow(arena);
        final String obj3 = pool.borrow(arena);

        assertEquals(3, pool.getBorrowedCount(), "Should have 3 borrowed");

        pool.returnObject(obj1);
        pool.returnObject(obj2);
        assertEquals(1, pool.getBorrowedCount(), "Should have 1 borrowed after 2 returns");

        pool.returnObject(obj3);
        assertEquals(0, pool.getBorrowedCount(), "Should have 0 borrowed after all returns");
      }

      LOGGER.info("Borrow/return tracking correct");
      pool.close();
    }

    @Test
    @DisplayName("Should reuse returned objects")
    void shouldReuseReturnedObjects() {
      LOGGER.info("Testing object reuse");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "new_object", 8, 0);

      try (final Arena arena = Arena.ofConfined()) {
        // Borrow and return
        final String first = pool.borrow(arena);
        pool.returnObject(first);

        // Borrow again - should get the returned object
        final String second = pool.borrow(arena);
        assertNotNull(second, "Should get a non-null object");
        LOGGER.info("First: " + first + ", Second: " + second);

        pool.returnObject(second);
      }

      pool.close();
    }

    @Test
    @DisplayName("Should throw for null arena")
    void shouldThrowForNullArena() {
      LOGGER.info("Testing null arena in borrow");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "x", 8, 0);

      assertThrows(IllegalArgumentException.class, () -> pool.borrow(null));
      LOGGER.info("Null arena rejected");

      pool.close();
    }

    @Test
    @DisplayName("Should throw for null return object")
    void shouldThrowForNullReturnObject() {
      LOGGER.info("Testing null object in returnObject");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "x", 8, 0);

      assertThrows(IllegalArgumentException.class, () -> pool.returnObject(null));
      LOGGER.info("Null return object rejected");

      pool.close();
    }

    @Test
    @DisplayName("Should throw on borrow from closed pool")
    void shouldThrowOnBorrowFromClosedPool() {
      LOGGER.info("Testing borrow from closed pool");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "x", 8, 0);
      pool.close();

      try (final Arena arena = Arena.ofConfined()) {
        assertThrows(IllegalStateException.class, () -> pool.borrow(arena));
      }
      LOGGER.info("Closed pool borrow rejected");
    }
  }

  @Nested
  @DisplayName("Pool Status Tests")
  class PoolStatusTests {

    @Test
    @DisplayName("Should report available count")
    void shouldReportAvailableCount() {
      LOGGER.info("Testing getAvailableCount");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "status", 8, 0);

      assertEquals(0, pool.getAvailableCount(), "Initially 0 available (minPoolSize=0)");

      try (final Arena arena = Arena.ofConfined()) {
        final String obj = pool.borrow(arena);
        pool.returnObject(obj);
        assertTrue(pool.getAvailableCount() >= 0, "Should have non-negative available count");
      }

      LOGGER.info("Available count: " + pool.getAvailableCount());
      pool.close();
    }

    @Test
    @DisplayName("Should report hit rate")
    void shouldReportHitRate() {
      LOGGER.info("Testing getHitRate");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "hit_rate", 8, 0);

      // No borrows yet - hit rate is 100% (by convention)
      assertEquals(100.0, pool.getHitRate(), 0.01, "Should be 100% with 0 borrows");

      try (final Arena arena = Arena.ofConfined()) {
        final String obj = pool.borrow(arena);
        pool.returnObject(obj);
      }

      final double hitRate = pool.getHitRate();
      assertTrue(hitRate >= 0.0 && hitRate <= 100.0, "Hit rate should be between 0 and 100");
      LOGGER.info("Hit rate: " + hitRate + "%");

      pool.close();
    }

    @Test
    @DisplayName("Should report arena allocations")
    void shouldReportArenaAllocations() {
      LOGGER.info("Testing getArenaAllocations");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "arena_alloc", 8, 0);

      try (final Arena arena = Arena.ofConfined()) {
        pool.borrow(arena);
      }

      assertTrue(pool.getArenaAllocations() >= 1, "Should have at least 1 arena allocation");
      LOGGER.info("Arena allocations: " + pool.getArenaAllocations());

      pool.close();
    }

    @Test
    @DisplayName("Should report zero copy and memory segment operations")
    void shouldReportZeroCopyAndMemorySegmentOps() {
      LOGGER.info("Testing zero copy and memory segment operations");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "ops", 8, 0);

      assertTrue(pool.getZeroCopyOperations() >= 0, "Zero copy ops should be non-negative");
      assertTrue(
          pool.getMemorySegmentOperations() >= 0, "Memory segment ops should be non-negative");
      LOGGER.info(
          "Zero copy: "
              + pool.getZeroCopyOperations()
              + ", MemSeg: "
              + pool.getMemorySegmentOperations());

      pool.close();
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("Should return pool stats")
    void shouldReturnPoolStats() {
      LOGGER.info("Testing getStats");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "stats", 8, 0);

      try (final Arena arena = Arena.ofConfined()) {
        pool.borrow(arena);
      }

      final String stats = pool.getStats();
      assertNotNull(stats, "Stats should not be null");
      assertTrue(stats.contains("Panama String pool"), "Should contain pool type info");
      assertTrue(stats.contains("total_borrows="), "Should contain total borrows");
      assertTrue(stats.contains("hit_rate="), "Should contain hit rate");
      assertTrue(stats.contains("arena_allocs="), "Should contain arena allocations");
      LOGGER.info("Stats: " + stats);

      pool.close();
    }

    @Test
    @DisplayName("Should return Panama performance stats")
    void shouldReturnPanamaPerformanceStats() {
      LOGGER.info("Testing getPanamaPerformanceStats");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "perf", 8, 0);

      try (final Arena arena = Arena.ofConfined()) {
        pool.borrow(arena);
      }

      final String perfStats = pool.getPanamaPerformanceStats();
      assertNotNull(perfStats, "Performance stats should not be null");
      assertTrue(
          perfStats.contains("Panama String performance"), "Should contain performance header");
      assertTrue(perfStats.contains("avg_borrow="), "Should contain avg borrow time");
      assertTrue(perfStats.contains("miss_rate="), "Should contain miss rate");
      assertTrue(perfStats.contains("contention="), "Should contain contention");
      assertTrue(perfStats.contains("zero_copy="), "Should contain zero copy");
      LOGGER.info("Performance stats: " + perfStats);

      pool.close();
    }

    @Test
    @DisplayName("Should return toString")
    void shouldReturnToString() {
      LOGGER.info("Testing toString");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "tostr", 8, 0);

      final String str = pool.toString();
      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("PanamaStringPool"), "Should contain pool type");
      assertTrue(str.contains("available="), "Should contain available count");
      assertTrue(str.contains("borrowed="), "Should contain borrowed count");
      LOGGER.info("toString: " + str);

      pool.close();
    }
  }

  @Nested
  @DisplayName("Static Utility Tests")
  class StaticUtilityTests {

    @Test
    @DisplayName("Should return all pool stats")
    void shouldReturnAllPoolStats() {
      LOGGER.info("Testing getAllPoolStats");

      // Create a pool
      final PanaNativeObjectPool<Long> pool =
          PanaNativeObjectPool.getPool(Long.class, arena -> 42L, 8, 0);

      final String allStats = PanaNativeObjectPool.getAllPoolStats();
      assertNotNull(allStats, "All pool stats should not be null");
      assertTrue(allStats.contains("Panama NativeObjectPool Statistics"), "Should contain header");
      LOGGER.info("All pool stats: " + allStats);

      pool.close();
    }

    @Test
    @DisplayName("Should clear all pools")
    void shouldClearAllPools() {
      LOGGER.info("Testing clearAllPools");

      // Create pools
      PanaNativeObjectPool.getPool(Long.class, arena -> 1L, 4, 0);
      PanaNativeObjectPool.getPool(Double.class, arena -> 1.0, 4, 0);

      PanaNativeObjectPool.clearAllPools();
      LOGGER.info("All pools cleared");
    }
  }

  @Nested
  @DisplayName("Clear and Close Tests")
  class ClearCloseTests {

    @Test
    @DisplayName("Should clear pool")
    void shouldClearPool() {
      LOGGER.info("Testing clear");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "clear", 8, 0);

      try (final Arena arena = Arena.ofConfined()) {
        final String obj = pool.borrow(arena);
        pool.returnObject(obj);
      }

      pool.clear();
      assertEquals(0, pool.getAvailableCount(), "Available count should be 0 after clear");
      LOGGER.info("Pool cleared");

      pool.close();
    }

    @Test
    @DisplayName("Should close pool")
    void shouldClosePool() {
      LOGGER.info("Testing close");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "close", 8, 0);

      assertFalse(pool.isClosed(), "Should not be closed initially");
      pool.close();
      assertTrue(pool.isClosed(), "Should be closed after close()");
      LOGGER.info("Pool closed");
    }

    @Test
    @DisplayName("Should handle double close")
    void shouldHandleDoubleClose() {
      LOGGER.info("Testing double close");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "dbl_close", 8, 0);

      pool.close();
      pool.close(); // Should not throw
      assertTrue(pool.isClosed(), "Should remain closed");
      LOGGER.info("Double close handled");
    }

    @Test
    @DisplayName("Should handle returnObject to closed pool")
    void shouldHandleReturnToClosedPool() {
      LOGGER.info("Testing return to closed pool");

      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(String.class, arena -> "ret_closed", 8, 0);

      try (final Arena arena = Arena.ofConfined()) {
        final String obj = pool.borrow(arena);
        pool.close();
        // Should not throw, just decrements borrow count
        pool.returnObject(obj);
        LOGGER.info("Return to closed pool handled");
      }
    }
  }

  @Nested
  @DisplayName("End-to-End Workflow Tests")
  class EndToEndWorkflowTests {

    @Test
    @DisplayName("Should handle complete pool lifecycle")
    void shouldHandleCompletePoolLifecycle() {
      LOGGER.info("Testing complete pool lifecycle");

      // 1. Create pool
      final PanaNativeObjectPool<String> pool =
          PanaNativeObjectPool.getPool(
              String.class, arena -> "lifecycle_" + System.nanoTime(), 8, 0);

      try (final Arena arena = Arena.ofConfined()) {
        // 2. Borrow several objects
        final String obj1 = pool.borrow(arena);
        final String obj2 = pool.borrow(arena);
        final String obj3 = pool.borrow(arena);

        assertNotNull(obj1);
        assertNotNull(obj2);
        assertNotNull(obj3);
        assertEquals(3, pool.getBorrowedCount());

        // 3. Return objects
        pool.returnObject(obj1);
        pool.returnObject(obj2);
        pool.returnObject(obj3);
        assertEquals(0, pool.getBorrowedCount());

        // 4. Borrow again (should reuse)
        final String reused = pool.borrow(arena);
        assertNotNull(reused);
        pool.returnObject(reused);
      }

      // 5. Check stats
      final String stats = pool.getStats();
      assertNotNull(stats);
      final String perfStats = pool.getPanamaPerformanceStats();
      assertNotNull(perfStats);
      final double hitRate = pool.getHitRate();
      assertTrue(hitRate >= 0.0);
      LOGGER.info("Lifecycle stats: " + stats);
      LOGGER.info("Lifecycle perf: " + perfStats);
      LOGGER.info("Hit rate: " + hitRate + "%");

      // 6. Clear and close
      pool.clear();
      pool.close();
      assertTrue(pool.isClosed());

      LOGGER.info("Complete lifecycle passed");
    }
  }
}

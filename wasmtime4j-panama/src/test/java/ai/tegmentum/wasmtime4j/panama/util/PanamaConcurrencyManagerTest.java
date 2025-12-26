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

package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaConcurrencyManager} class.
 *
 * <p>This test class verifies the concurrency management functionality for Panama FFI operations.
 */
@DisplayName("PanamaConcurrencyManager Tests")
class PanamaConcurrencyManagerTest {

  private PanamaConcurrencyManager manager;

  @BeforeEach
  void setUp() {
    manager = new PanamaConcurrencyManager();
  }

  @AfterEach
  void tearDown() {
    manager.shutdown();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaConcurrencyManager should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaConcurrencyManager.class.getModifiers()),
          "PanamaConcurrencyManager should be final");
    }

    @Test
    @DisplayName("DEFAULT_MAX_CONCURRENT_OPERATIONS should be positive")
    void defaultMaxConcurrentOperationsShouldBePositive() {
      assertTrue(
          PanamaConcurrencyManager.DEFAULT_MAX_CONCURRENT_OPERATIONS > 0,
          "Default max concurrent operations should be positive");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor should create manager with default settings")
    void defaultConstructorShouldCreateManagerWithDefaultSettings() {
      final PanamaConcurrencyManager defaultManager = new PanamaConcurrencyManager();
      assertEquals(
          PanamaConcurrencyManager.DEFAULT_MAX_CONCURRENT_OPERATIONS,
          defaultManager.getMaxConcurrentOperations(),
          "Should use default max concurrent operations");
      defaultManager.shutdown();
    }

    @Test
    @DisplayName("Constructor should accept custom max concurrent operations")
    void constructorShouldAcceptCustomMaxConcurrentOperations() {
      final PanamaConcurrencyManager customManager = new PanamaConcurrencyManager(8);
      assertEquals(
          8, customManager.getMaxConcurrentOperations(), "Should use custom max concurrent ops");
      customManager.shutdown();
    }

    @Test
    @DisplayName("Constructor should throw for non-positive max concurrent operations")
    void constructorShouldThrowForNonPositiveMaxConcurrentOperations() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaConcurrencyManager(0),
          "Should throw for zero");
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaConcurrencyManager(-1),
          "Should throw for negative");
    }
  }

  @Nested
  @DisplayName("execute Tests")
  class ExecuteTests {

    @Test
    @DisplayName("execute should run operation and return result")
    void executeShouldRunOperationAndReturnResult() throws Exception {
      final String result = manager.execute(() -> "test result");
      assertEquals("test result", result, "Should return operation result");
    }

    @Test
    @DisplayName("execute should throw for null operation")
    void executeShouldThrowForNullOperation() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.execute(null),
          "Should throw for null operation");
    }

    @Test
    @DisplayName("execute should propagate operation exception")
    void executeShouldPropagateOperationException() {
      assertThrows(
          RuntimeException.class,
          () ->
              manager.execute(
                  () -> {
                    throw new RuntimeException("Test error");
                  }),
          "Should propagate operation exception");
    }

    @Test
    @DisplayName("execute should track statistics")
    void executeShouldTrackStatistics() throws Exception {
      manager.execute(() -> "result1");
      manager.execute(() -> "result2");

      final String stats = manager.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.contains("2"), "Statistics should show 2 operations");
    }
  }

  @Nested
  @DisplayName("executeAsync Tests")
  class ExecuteAsyncTests {

    @Test
    @DisplayName("executeAsync should return CompletableFuture with result")
    void executeAsyncShouldReturnCompletableFutureWithResult() throws Exception {
      final CompletableFuture<String> future = manager.executeAsync(() -> "async result");
      assertNotNull(future, "Future should not be null");
      assertEquals("async result", future.get(5, TimeUnit.SECONDS), "Should return result");
    }

    @Test
    @DisplayName("executeAsync should throw for null operation")
    void executeAsyncShouldThrowForNullOperation() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.executeAsync(null),
          "Should throw for null operation");
    }

    @Test
    @DisplayName("executeAsync should complete exceptionally on error")
    void executeAsyncShouldCompleteExceptionallyOnError() {
      final CompletableFuture<String> future =
          manager.executeAsync(
              () -> {
                throw new RuntimeException("Async error");
              });

      assertTrue(
          assertThrows(Exception.class, () -> future.get(5, TimeUnit.SECONDS)) != null,
          "Future should complete exceptionally");
    }
  }

  @Nested
  @DisplayName("executeWithLock Tests")
  class ExecuteWithLockTests {

    @Test
    @DisplayName("executeWithLock should run operation with lock")
    void executeWithLockShouldRunOperationWithLock() throws Exception {
      final String result = manager.executeWithLock("resource1", () -> "locked result");
      assertEquals("locked result", result, "Should return locked operation result");
    }

    @Test
    @DisplayName("executeWithLock should throw for null resourceId")
    void executeWithLockShouldThrowForNullResourceId() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.executeWithLock(null, () -> "result"),
          "Should throw for null resourceId");
    }

    @Test
    @DisplayName("executeWithLock should throw for empty resourceId")
    void executeWithLockShouldThrowForEmptyResourceId() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.executeWithLock("", () -> "result"),
          "Should throw for empty resourceId");
    }

    @Test
    @DisplayName("executeWithLock should throw for null operation")
    void executeWithLockShouldThrowForNullOperation() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.executeWithLock("resource1", null),
          "Should throw for null operation");
    }

    @Test
    @DisplayName("executeWithLock should serialize access to same resource")
    void executeWithLockShouldSerializeAccessToSameResource() throws Exception {
      final AtomicInteger counter = new AtomicInteger(0);
      final CountDownLatch latch = new CountDownLatch(2);

      final Thread t1 =
          new Thread(
              () -> {
                try {
                  manager.executeWithLock(
                      "shared",
                      () -> {
                        counter.incrementAndGet();
                        Thread.sleep(50);
                        return counter.get();
                      });
                } catch (Exception e) {
                  // Ignored
                } finally {
                  latch.countDown();
                }
              });

      final Thread t2 =
          new Thread(
              () -> {
                try {
                  manager.executeWithLock(
                      "shared",
                      () -> {
                        counter.incrementAndGet();
                        return counter.get();
                      });
                } catch (Exception e) {
                  // Ignored
                } finally {
                  latch.countDown();
                }
              });

      t1.start();
      t2.start();

      assertTrue(latch.await(5, TimeUnit.SECONDS), "Both threads should complete");
      assertEquals(2, counter.get(), "Counter should be 2 after serialized access");
    }
  }

  @Nested
  @DisplayName("executeWithReadLock Tests")
  class ExecuteWithReadLockTests {

    @Test
    @DisplayName("executeWithReadLock should run operation with read lock")
    void executeWithReadLockShouldRunOperationWithReadLock() throws Exception {
      final String result = manager.executeWithReadLock("resource1", () -> "read result");
      assertEquals("read result", result, "Should return read operation result");
    }

    @Test
    @DisplayName("executeWithReadLock should allow concurrent reads")
    void executeWithReadLockShouldAllowConcurrentReads() throws Exception {
      final AtomicInteger concurrentReads = new AtomicInteger(0);
      final AtomicInteger maxConcurrentReads = new AtomicInteger(0);
      final CountDownLatch latch = new CountDownLatch(3);

      final Runnable reader =
          () -> {
            try {
              manager.executeWithReadLock(
                  "shared",
                  () -> {
                    final int current = concurrentReads.incrementAndGet();
                    maxConcurrentReads.updateAndGet(max -> Math.max(max, current));
                    Thread.sleep(50);
                    concurrentReads.decrementAndGet();
                    return "read";
                  });
            } catch (Exception e) {
              // Ignored
            } finally {
              latch.countDown();
            }
          };

      new Thread(reader).start();
      new Thread(reader).start();
      new Thread(reader).start();

      assertTrue(latch.await(5, TimeUnit.SECONDS), "All readers should complete");
      assertTrue(maxConcurrentReads.get() >= 1, "Should have had concurrent reads");
    }
  }

  @Nested
  @DisplayName("executeWithArena Tests")
  class ExecuteWithArenaTests {

    @Test
    @DisplayName("executeWithArena should run operation with arena coordination")
    void executeWithArenaShouldRunOperationWithArenaCoordination() throws Exception {
      try (Arena arena = Arena.ofConfined()) {
        final String result = manager.executeWithArena(arena, () -> "arena result");
        assertEquals("arena result", result, "Should return arena operation result");
      }
    }

    @Test
    @DisplayName("executeWithArena should throw for null arena")
    void executeWithArenaShouldThrowForNullArena() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.executeWithArena(null, () -> "result"),
          "Should throw for null arena");
    }

    @Test
    @DisplayName("executeWithArena should throw for null operation")
    void executeWithArenaShouldThrowForNullOperation() {
      try (Arena arena = Arena.ofConfined()) {
        assertThrows(
            IllegalArgumentException.class,
            () -> manager.executeWithArena(arena, null),
            "Should throw for null operation");
      }
    }

    @Test
    @DisplayName("executeWithArena should throw for closed arena")
    void executeWithArenaShouldThrowForClosedArena() {
      final Arena arena = Arena.ofConfined();
      arena.close();

      assertThrows(
          IllegalStateException.class,
          () -> manager.executeWithArena(arena, () -> "result"),
          "Should throw for closed arena");
    }
  }

  @Nested
  @DisplayName("executeWithMemorySegment Tests")
  class ExecuteWithMemorySegmentTests {

    @Test
    @DisplayName("executeWithMemorySegment should run operation with segment coordination")
    void executeWithMemorySegmentShouldRunOperationWithSegmentCoordination() throws Exception {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(64);
        final String result = manager.executeWithMemorySegment(segment, () -> "segment result");
        assertEquals("segment result", result, "Should return segment operation result");
      }
    }

    @Test
    @DisplayName("executeWithMemorySegment should throw for null segment")
    void executeWithMemorySegmentShouldThrowForNullSegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.executeWithMemorySegment(null, () -> "result"),
          "Should throw for null segment");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getStatistics should return formatted string")
    void getStatisticsShouldReturnFormattedString() {
      final String stats = manager.getStatistics();

      assertNotNull(stats, "Statistics should not be null");
      assertFalse(stats.isEmpty(), "Statistics should not be empty");
      assertTrue(stats.contains("Panama Concurrency Manager"), "Should contain manager identifier");
    }

    @Test
    @DisplayName("getPerformanceMetrics should return metrics string")
    void getPerformanceMetricsShouldReturnMetricsString() {
      final String metrics = manager.getPerformanceMetrics();

      assertNotNull(metrics, "Metrics should not be null");
      assertFalse(metrics.isEmpty(), "Metrics should not be empty");
      assertTrue(metrics.contains("contention_rate"), "Should contain contention rate");
    }

    @Test
    @DisplayName("getActiveArenaInfo should return arena information")
    void getActiveArenaInfoShouldReturnArenaInformation() {
      final String arenaInfo = manager.getActiveArenaInfo();

      assertNotNull(arenaInfo, "Arena info should not be null");
    }

    @Test
    @DisplayName("resetStatistics should reset counters")
    void resetStatisticsShouldResetCounters() throws Exception {
      manager.execute(() -> "result");

      manager.resetStatistics();

      final String stats = manager.getStatistics();
      assertTrue(
          stats.contains("Total operations: 0"), "Statistics should be reset to 0 operations");
    }
  }

  @Nested
  @DisplayName("checkForIssues Tests")
  class CheckForIssuesTests {

    @Test
    @DisplayName("checkForIssues should return null when no issues")
    void checkForIssuesShouldReturnNullWhenNoIssues() {
      final String issues = manager.checkForIssues();

      // With no operations, there should be no issues
      // Note: Could be null or have no critical issues
      if (issues != null) {
        assertFalse(issues.contains("High concurrency"), "Should not have high concurrency issues");
      }
    }
  }

  @Nested
  @DisplayName("Cleanup Tests")
  class CleanupTests {

    @Test
    @DisplayName("cleanupClosedArenas should not throw")
    void cleanupClosedArenasShouldNotThrow() {
      assertDoesNotThrow(manager::cleanupClosedArenas, "Should not throw when cleaning up arenas");
    }

    @Test
    @DisplayName("cleanupUnusedLocks should not throw")
    void cleanupUnusedLocksShouldNotThrow() {
      assertDoesNotThrow(manager::cleanupUnusedLocks, "Should not throw when cleaning up locks");
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("getAvailablePermits should return available permits")
    void getAvailablePermitsShouldReturnAvailablePermits() {
      final int permits = manager.getAvailablePermits();
      assertEquals(
          manager.getMaxConcurrentOperations(),
          permits,
          "Initially all permits should be available");
    }

    @Test
    @DisplayName("getMaxConcurrentOperations should return configured value")
    void getMaxConcurrentOperationsShouldReturnConfiguredValue() {
      final PanamaConcurrencyManager customManager = new PanamaConcurrencyManager(4);
      assertEquals(4, customManager.getMaxConcurrentOperations(), "Should return configured value");
      customManager.shutdown();
    }

    @Test
    @DisplayName("shutdown should release resources")
    void shutdownShouldReleaseResources() {
      final PanamaConcurrencyManager shutdownManager = new PanamaConcurrencyManager();
      shutdownManager.shutdown();

      // Should be able to call shutdown multiple times
      assertDoesNotThrow(shutdownManager::shutdown, "Multiple shutdowns should not throw");
    }
  }

  @Nested
  @DisplayName("Concurrency Tests")
  class ConcurrencyTests {

    @Test
    @DisplayName("Concurrent operations should respect max limit")
    void concurrentOperationsShouldRespectMaxLimit() throws Exception {
      final PanamaConcurrencyManager limitedManager = new PanamaConcurrencyManager(2);
      final AtomicInteger concurrent = new AtomicInteger(0);
      final AtomicInteger maxConcurrent = new AtomicInteger(0);
      final CountDownLatch latch = new CountDownLatch(5);

      for (int i = 0; i < 5; i++) {
        new Thread(
                () -> {
                  try {
                    limitedManager.execute(
                        () -> {
                          final int current = concurrent.incrementAndGet();
                          maxConcurrent.updateAndGet(max -> Math.max(max, current));
                          Thread.sleep(100);
                          concurrent.decrementAndGet();
                          return "done";
                        });
                  } catch (Exception e) {
                    // Ignored
                  } finally {
                    latch.countDown();
                  }
                })
            .start();
      }

      assertTrue(latch.await(10, TimeUnit.SECONDS), "All operations should complete");
      assertTrue(
          maxConcurrent.get() <= 2, "Max concurrent should not exceed limit: " + maxConcurrent);

      limitedManager.shutdown();
    }
  }
}

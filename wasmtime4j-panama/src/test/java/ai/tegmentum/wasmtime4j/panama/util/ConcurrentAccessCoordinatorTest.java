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
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ConcurrentAccessCoordinator} class.
 *
 * <p>This test class verifies the concurrent access coordination functionality.
 */
@DisplayName("ConcurrentAccessCoordinator Tests")
class ConcurrentAccessCoordinatorTest {

  private ConcurrentAccessCoordinator coordinator;

  @BeforeEach
  void setUp() {
    coordinator = new ConcurrentAccessCoordinator();
  }

  @AfterEach
  void tearDown() {
    coordinator.shutdown();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ConcurrentAccessCoordinator should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(ConcurrentAccessCoordinator.class.getModifiers()),
          "ConcurrentAccessCoordinator should be final");
    }

    @Test
    @DisplayName("CoordinationStatistics should be final class")
    void coordinationStatisticsShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(
              ConcurrentAccessCoordinator.CoordinationStatistics.class.getModifiers()),
          "CoordinationStatistics should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor should create coordinator")
    void defaultConstructorShouldCreateCoordinator() {
      final ConcurrentAccessCoordinator defaultCoordinator = new ConcurrentAccessCoordinator();
      assertNotNull(defaultCoordinator, "Coordinator should be created");
      assertFalse(defaultCoordinator.isShutdownRequested(), "Should not be shutdown initially");
      defaultCoordinator.shutdown();
    }

    @Test
    @DisplayName("Custom constructor should accept parameters")
    void customConstructorShouldAcceptParameters() {
      final Executor executor = ForkJoinPool.commonPool();
      final ConcurrentAccessCoordinator customCoordinator =
          new ConcurrentAccessCoordinator(executor, 50, 10000L);

      assertNotNull(customCoordinator, "Coordinator should be created");
      customCoordinator.shutdown();
    }
  }

  @Nested
  @DisplayName("executeWithArenaCoordination Tests")
  class ExecuteWithArenaCoordinationTests {

    @Test
    @DisplayName("executeWithArenaCoordination should execute operation and return result")
    void executeWithArenaCoordinationShouldExecuteOperationAndReturnResult() {
      try (Arena arena = Arena.ofConfined()) {
        final String result =
            coordinator.executeWithArenaCoordination(arena, a -> "coordinated result");

        assertEquals("coordinated result", result, "Should return operation result");
      }
    }

    @Test
    @DisplayName("executeWithArenaCoordination should throw when shutdown requested")
    void executeWithArenaCoordinationShouldThrowWhenShutdownRequested() {
      coordinator.shutdown();

      try (Arena arena = Arena.ofConfined()) {
        assertThrows(
            IllegalStateException.class,
            () -> coordinator.executeWithArenaCoordination(arena, a -> "result"),
            "Should throw when shutdown");
      }
    }

    @Test
    @DisplayName("executeWithArenaCoordination should handle operation exceptions")
    void executeWithArenaCoordinationShouldHandleOperationExceptions() {
      try (Arena arena = Arena.ofConfined()) {
        assertThrows(
            RuntimeException.class,
            () ->
                coordinator.executeWithArenaCoordination(
                    arena,
                    a -> {
                      throw new RuntimeException("Test error");
                    }),
            "Should propagate operation exception");
      }
    }
  }

  @Nested
  @DisplayName("executeBulkOperation Tests")
  class ExecuteBulkOperationTests {

    @Test
    @DisplayName("executeBulkOperation should execute on resources and return result")
    void executeBulkOperationShouldExecuteOnResourcesAndReturnResult() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment[] resources = {arena.allocate(64), arena.allocate(64)};

        final Integer result = coordinator.executeBulkOperation(resources, r -> r.length);

        assertEquals(2, result, "Should return resource count");
      }
    }

    @Test
    @DisplayName("executeBulkOperation should throw for empty resources")
    void executeBulkOperationShouldThrowForEmptyResources() {
      final MemorySegment[] empty = new MemorySegment[0];

      assertThrows(
          IllegalArgumentException.class,
          () -> coordinator.executeBulkOperation(empty, r -> r.length),
          "Should throw for empty resources");
    }

    @Test
    @DisplayName("executeBulkOperation should throw when shutdown requested")
    void executeBulkOperationShouldThrowWhenShutdownRequested() {
      coordinator.shutdown();

      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment[] resources = {arena.allocate(64)};

        assertThrows(
            IllegalStateException.class,
            () -> coordinator.executeBulkOperation(resources, r -> r.length),
            "Should throw when shutdown");
      }
    }
  }

  @Nested
  @DisplayName("executeAsync Tests")
  class ExecuteAsyncTests {

    @Test
    @DisplayName("executeAsync should return CompletableFuture with result")
    void executeAsyncShouldReturnCompletableFutureWithResult() throws Exception {
      final CompletableFuture<String> future = coordinator.executeAsync(() -> "async result");

      assertNotNull(future, "Future should not be null");
      assertEquals("async result", future.get(5, TimeUnit.SECONDS), "Should return result");
    }

    @Test
    @DisplayName("executeAsync should complete exceptionally on error")
    void executeAsyncShouldCompleteExceptionallyOnError() {
      final CompletableFuture<String> future =
          coordinator.executeAsync(
              () -> {
                throw new RuntimeException("Async error");
              });

      assertTrue(
          assertThrows(Exception.class, () -> future.get(5, TimeUnit.SECONDS)) != null,
          "Future should complete exceptionally");
    }

    @Test
    @DisplayName("executeAsync should fail when shutdown requested")
    void executeAsyncShouldFailWhenShutdownRequested() {
      coordinator.shutdown();

      final CompletableFuture<String> future = coordinator.executeAsync(() -> "result");

      assertTrue(future.isCompletedExceptionally(), "Future should be completed exceptionally");
    }
  }

  @Nested
  @DisplayName("executeBatch Tests")
  class ExecuteBatchTests {

    @Test
    @DisplayName("executeBatch should execute all operations and return results")
    void executeBatchShouldExecuteAllOperationsAndReturnResults() throws Exception {
      @SuppressWarnings("unchecked")
      final CompletableFuture<Integer[]> future =
          coordinator.executeBatch(() -> 1, () -> 2, () -> 3);

      final Integer[] results = future.get(5, TimeUnit.SECONDS);

      assertEquals(3, results.length, "Should have 3 results");
      assertEquals(1, results[0], "First result should be 1");
      assertEquals(2, results[1], "Second result should be 2");
      assertEquals(3, results[2], "Third result should be 3");
    }

    @Test
    @DisplayName("executeBatch should return empty array for no operations")
    void executeBatchShouldReturnEmptyArrayForNoOperations() throws Exception {
      final CompletableFuture<Object[]> future = coordinator.executeBatch();

      final Object[] results = future.get(5, TimeUnit.SECONDS);

      assertEquals(0, results.length, "Should return empty array");
    }

    @Test
    @DisplayName("executeBatch should fail when shutdown requested")
    void executeBatchShouldFailWhenShutdownRequested() {
      coordinator.shutdown();

      final CompletableFuture<Integer[]> future = coordinator.executeBatch(() -> 1);

      assertTrue(future.isCompletedExceptionally(), "Future should be completed exceptionally");
    }
  }

  @Nested
  @DisplayName("getResourceTypeLock Tests")
  class GetResourceTypeLockTests {

    @Test
    @DisplayName("getResourceTypeLock should return StampedLock for resource type")
    void getResourceTypeLockShouldReturnStampedLockForResourceType() {
      final StampedLock lock = coordinator.getResourceTypeLock("memory");

      assertNotNull(lock, "Lock should not be null");
    }

    @Test
    @DisplayName("getResourceTypeLock should return same lock for same resource type")
    void getResourceTypeLockShouldReturnSameLockForSameResourceType() {
      final StampedLock lock1 = coordinator.getResourceTypeLock("memory");
      final StampedLock lock2 = coordinator.getResourceTypeLock("memory");

      assertEquals(lock1, lock2, "Should return same lock");
    }

    @Test
    @DisplayName("getResourceTypeLock should return different locks for different types")
    void getResourceTypeLockShouldReturnDifferentLocksForDifferentTypes() {
      final StampedLock memoryLock = coordinator.getResourceTypeLock("memory");
      final StampedLock tableLock = coordinator.getResourceTypeLock("table");

      assertFalse(memoryLock == tableLock, "Should return different locks");
    }
  }

  @Nested
  @DisplayName("getStatistics Tests")
  class GetStatisticsTests {

    @Test
    @DisplayName("getStatistics should return CoordinationStatistics")
    void getStatisticsShouldReturnCoordinationStatistics() {
      final ConcurrentAccessCoordinator.CoordinationStatistics stats = coordinator.getStatistics();

      assertNotNull(stats, "Statistics should not be null");
      assertEquals(0, stats.getTotalOperations(), "Initial operations should be 0");
      assertEquals(0, stats.getActiveBulkOperations(), "Initial active bulk should be 0");
    }

    @Test
    @DisplayName("getStatistics should track operations")
    void getStatisticsShouldTrackOperations() throws Exception {
      try (Arena arena = Arena.ofConfined()) {
        coordinator.executeWithArenaCoordination(arena, a -> "result");
      }

      final ConcurrentAccessCoordinator.CoordinationStatistics stats = coordinator.getStatistics();

      assertEquals(1, stats.getTotalOperations(), "Should count operation");
    }
  }

  @Nested
  @DisplayName("CoordinationStatistics Tests")
  class CoordinationStatisticsTests {

    @Test
    @DisplayName("CoordinationStatistics should have all getters")
    void coordinationStatisticsShouldHaveAllGetters() {
      final ConcurrentAccessCoordinator.CoordinationStatistics stats = coordinator.getStatistics();

      assertDoesNotThrow(stats::getTotalOperations, "getTotalOperations should work");
      assertDoesNotThrow(stats::getActiveBulkOperations, "getActiveBulkOperations should work");
      assertDoesNotThrow(stats::getPendingOperations, "getPendingOperations should work");
      assertDoesNotThrow(stats::getResourceTypeLocks, "getResourceTypeLocks should work");
    }

    @Test
    @DisplayName("CoordinationStatistics toString should return formatted string")
    void coordinationStatisticsToStringShouldReturnFormattedString() {
      final ConcurrentAccessCoordinator.CoordinationStatistics stats = coordinator.getStatistics();

      final String str = stats.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("totalOperations"), "Should contain totalOperations");
      assertTrue(str.contains("activeBulkOperations"), "Should contain activeBulkOperations");
    }
  }

  @Nested
  @DisplayName("Shutdown Tests")
  class ShutdownTests {

    @Test
    @DisplayName("shutdown should set shutdown flag")
    void shutdownShouldSetShutdownFlag() {
      assertFalse(coordinator.isShutdownRequested(), "Initially should not be shutdown");

      coordinator.shutdown();

      assertTrue(coordinator.isShutdownRequested(), "Should be shutdown after calling shutdown");
    }

    @Test
    @DisplayName("shutdown should be idempotent")
    void shutdownShouldBeIdempotent() {
      coordinator.shutdown();

      assertDoesNotThrow(coordinator::shutdown, "Multiple shutdowns should not throw");
      assertTrue(coordinator.isShutdownRequested(), "Should still be shutdown");
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Concurrent arena operations should be thread-safe")
    void concurrentArenaOperationsShouldBeThreadSafe() throws InterruptedException {
      final AtomicInteger successCount = new AtomicInteger(0);
      final CountDownLatch latch = new CountDownLatch(10);

      try (Arena arena = Arena.ofShared()) {
        for (int i = 0; i < 10; i++) {
          new Thread(
                  () -> {
                    try {
                      coordinator.executeWithArenaCoordination(
                          arena,
                          a -> {
                            successCount.incrementAndGet();
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

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
        assertEquals(10, successCount.get(), "All operations should succeed");
      }
    }

    @Test
    @DisplayName("Optimistic read should fall back to pessimistic on contention")
    void optimisticReadShouldFallBackToPessimisticOnContention() throws InterruptedException {
      final AtomicBoolean allSucceeded = new AtomicBoolean(true);
      final CountDownLatch latch = new CountDownLatch(5);

      try (Arena arena = Arena.ofShared()) {
        for (int i = 0; i < 5; i++) {
          new Thread(
                  () -> {
                    try {
                      for (int j = 0; j < 10; j++) {
                        coordinator.executeWithArenaCoordination(arena, a -> "result");
                      }
                    } catch (Exception e) {
                      allSucceeded.set(false);
                    } finally {
                      latch.countDown();
                    }
                  })
              .start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
        assertTrue(allSucceeded.get(), "All operations should succeed");
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full coordination lifecycle should work correctly")
    void fullCoordinationLifecycleShouldWorkCorrectly() throws Exception {
      // Arena coordination
      try (Arena arena = Arena.ofConfined()) {
        final String arenaResult = coordinator.executeWithArenaCoordination(arena, a -> "arena");
        assertEquals("arena", arenaResult, "Arena result should match");
      }

      // Bulk operation
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment[] resources = {arena.allocate(64)};
        final Integer bulkResult = coordinator.executeBulkOperation(resources, r -> r.length);
        assertEquals(1, bulkResult, "Bulk result should match");
      }

      // Async operation
      final String asyncResult = coordinator.executeAsync(() -> "async").get(5, TimeUnit.SECONDS);
      assertEquals("async", asyncResult, "Async result should match");

      // Batch operation
      final Integer[] batchResults =
          coordinator.executeBatch(() -> 1, () -> 2).get(5, TimeUnit.SECONDS);
      assertEquals(2, batchResults.length, "Batch should have 2 results");

      // Resource type locks
      final StampedLock lock = coordinator.getResourceTypeLock("memory");
      assertNotNull(lock, "Lock should be available");

      // Statistics
      final ConcurrentAccessCoordinator.CoordinationStatistics stats = coordinator.getStatistics();
      assertTrue(stats.getTotalOperations() > 0, "Should have recorded operations");

      // Shutdown
      coordinator.shutdown();
      assertTrue(coordinator.isShutdownRequested(), "Should be shutdown");
    }
  }
}

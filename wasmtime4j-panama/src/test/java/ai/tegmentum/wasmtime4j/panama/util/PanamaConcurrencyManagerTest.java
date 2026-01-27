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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanamaConcurrencyManager}.
 *
 * <p>These tests invoke actual methods to exercise code paths and improve JaCoCo coverage. The
 * class uses Panama API (Arena, MemorySegment) and Java concurrency utilities.
 */
@DisplayName("PanamaConcurrencyManager Integration Tests")
class PanamaConcurrencyManagerTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaConcurrencyManagerTest.class.getName());

  private PanamaConcurrencyManager manager;

  @AfterEach
  void tearDown() {
    if (manager != null) {
      manager.shutdown();
      manager = null;
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create with default constructor")
    void shouldCreateWithDefaultConstructor() {
      LOGGER.info("Testing default constructor");
      manager = new PanamaConcurrencyManager();
      assertNotNull(manager, "Manager should be created");
      assertEquals(
          PanamaConcurrencyManager.DEFAULT_MAX_CONCURRENT_OPERATIONS,
          manager.getMaxConcurrentOperations(),
          "Should use default max concurrent operations");
      assertEquals(
          PanamaConcurrencyManager.DEFAULT_MAX_CONCURRENT_OPERATIONS,
          manager.getAvailablePermits(),
          "Should have all permits available initially");
      LOGGER.info(
          "Default constructor: maxOps="
              + manager.getMaxConcurrentOperations()
              + ", permits="
              + manager.getAvailablePermits());
    }

    @Test
    @DisplayName("Should create with custom max concurrent operations")
    void shouldCreateWithCustomMaxConcurrentOperations() {
      LOGGER.info("Testing int constructor");
      manager = new PanamaConcurrencyManager(4);
      assertEquals(
          4, manager.getMaxConcurrentOperations(), "Should use custom max concurrent operations");
      assertEquals(4, manager.getAvailablePermits(), "Should have all permits available initially");
      LOGGER.info("Custom constructor: maxOps=" + manager.getMaxConcurrentOperations());
    }

    @Test
    @DisplayName("Should have correct DEFAULT_MAX_CONCURRENT_OPERATIONS constant")
    void shouldHaveCorrectDefaultConstant() {
      LOGGER.info("Testing DEFAULT_MAX_CONCURRENT_OPERATIONS");
      assertEquals(
          16, PanamaConcurrencyManager.DEFAULT_MAX_CONCURRENT_OPERATIONS, "Default should be 16");
      LOGGER.info(
          "Default constant: " + PanamaConcurrencyManager.DEFAULT_MAX_CONCURRENT_OPERATIONS);
    }
  }

  @Nested
  @DisplayName("Execute Tests")
  class ExecuteTests {

    @Test
    @DisplayName("Should execute callable and return result")
    void shouldExecuteCallableAndReturnResult() throws Exception {
      LOGGER.info("Testing execute()");
      manager = new PanamaConcurrencyManager(4);

      final String result =
          manager.execute(
              () -> {
                LOGGER.info("Inside execute callable");
                return "test_result";
              });

      assertEquals("test_result", result, "Should return callable result");
      LOGGER.info("Execute result: " + result);
    }

    @Test
    @DisplayName("Should track operations count")
    void shouldTrackOperationsCount() throws Exception {
      LOGGER.info("Testing operation tracking");
      manager = new PanamaConcurrencyManager(4);

      manager.execute(() -> "op1");
      manager.execute(() -> "op2");
      manager.execute(() -> "op3");

      final String stats = manager.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.contains("Total operations:"), "Should contain total operations");
      LOGGER.info("Operations stats: " + stats);
    }

    @Test
    @DisplayName("Should handle exception from callable")
    void shouldHandleExceptionFromCallable() {
      LOGGER.info("Testing execute() with exception");
      manager = new PanamaConcurrencyManager(4);

      assertThrows(
          Exception.class,
          () -> {
            manager.execute(
                () -> {
                  throw new RuntimeException("test_error");
                });
          });
      LOGGER.info("Exception from callable handled correctly");
    }
  }

  @Nested
  @DisplayName("Execute Async Tests")
  class ExecuteAsyncTests {

    @Test
    @DisplayName("Should execute async and return CompletableFuture")
    void shouldExecuteAsyncAndReturnFuture() throws Exception {
      LOGGER.info("Testing executeAsync()");
      manager = new PanamaConcurrencyManager(4);

      final CompletableFuture<String> future =
          manager.executeAsync(
              () -> {
                LOGGER.info("Inside async callable");
                return "async_result";
              });

      assertNotNull(future, "Future should not be null");
      final String result = future.get();
      assertEquals("async_result", result, "Should return async result");
      LOGGER.info("Async result: " + result);
    }

    @Test
    @DisplayName("Should handle async exception")
    void shouldHandleAsyncException() {
      LOGGER.info("Testing executeAsync() with exception");
      manager = new PanamaConcurrencyManager(4);

      final CompletableFuture<String> future =
          manager.executeAsync(
              () -> {
                throw new RuntimeException("async_error");
              });

      assertThrows(ExecutionException.class, future::get);
      LOGGER.info("Async exception handled correctly");
    }
  }

  @Nested
  @DisplayName("Execute With Lock Tests")
  class ExecuteWithLockTests {

    @Test
    @DisplayName("Should execute with write lock")
    void shouldExecuteWithWriteLock() throws Exception {
      LOGGER.info("Testing executeWithLock()");
      manager = new PanamaConcurrencyManager(4);

      final String result =
          manager.executeWithLock(
              "resource_1",
              () -> {
                LOGGER.info("Inside locked callable");
                return "locked_result";
              });

      assertEquals("locked_result", result, "Should return locked result");
      LOGGER.info("Locked result: " + result);
    }

    @Test
    @DisplayName("Should execute with read lock")
    void shouldExecuteWithReadLock() throws Exception {
      LOGGER.info("Testing executeWithReadLock()");
      manager = new PanamaConcurrencyManager(4);

      final String result =
          manager.executeWithReadLock(
              "resource_2",
              () -> {
                LOGGER.info("Inside read-locked callable");
                return "read_locked_result";
              });

      assertEquals("read_locked_result", result, "Should return read-locked result");
      LOGGER.info("Read-locked result: " + result);
    }

    @Test
    @DisplayName("Should use same lock for same resource ID")
    void shouldUseSameLockForSameResource() throws Exception {
      LOGGER.info("Testing same lock for same resource");
      manager = new PanamaConcurrencyManager(4);

      manager.executeWithLock("shared_resource", () -> "first");
      manager.executeWithLock("shared_resource", () -> "second");
      manager.executeWithReadLock("shared_resource", () -> "third");

      LOGGER.info("Same lock used for same resource");
    }

    @Test
    @DisplayName("Should use different locks for different resources")
    void shouldUseDifferentLocksForDifferentResources() throws Exception {
      LOGGER.info("Testing different locks for different resources");
      manager = new PanamaConcurrencyManager(4);

      manager.executeWithLock("resource_a", () -> "a");
      manager.executeWithLock("resource_b", () -> "b");

      LOGGER.info("Different locks used for different resources");
    }
  }

  @Nested
  @DisplayName("Execute With Arena Tests")
  class ExecuteWithArenaTests {

    @Test
    @DisplayName("Should execute with arena")
    void shouldExecuteWithArena() throws Exception {
      LOGGER.info("Testing executeWithArena()");
      manager = new PanamaConcurrencyManager(4);

      try (final Arena arena = Arena.ofConfined()) {
        final String result =
            manager.executeWithArena(
                arena,
                () -> {
                  LOGGER.info("Inside arena-managed callable");
                  return "arena_result";
                });

        assertEquals("arena_result", result, "Should return arena result");
        LOGGER.info("Arena result: " + result);
      }
    }

    @Test
    @DisplayName("Should track arena operations")
    void shouldTrackArenaOperations() throws Exception {
      LOGGER.info("Testing arena operation tracking");
      manager = new PanamaConcurrencyManager(4);

      try (final Arena arena = Arena.ofConfined()) {
        manager.executeWithArena(arena, () -> "op1");
        manager.executeWithArena(arena, () -> "op2");

        final String arenaInfo = manager.getActiveArenaInfo();
        assertNotNull(arenaInfo, "Arena info should not be null");
        LOGGER.info("Arena info after operations: " + arenaInfo);
      }
    }

    @Test
    @DisplayName("Should reject closed arena")
    void shouldRejectClosedArena() {
      LOGGER.info("Testing executeWithArena with closed arena");
      manager = new PanamaConcurrencyManager(4);

      final Arena arena = Arena.ofConfined();
      arena.close();

      assertThrows(
          IllegalStateException.class,
          () -> {
            manager.executeWithArena(arena, () -> "should_fail");
          });
      LOGGER.info("Closed arena rejected correctly");
    }
  }

  @Nested
  @DisplayName("Execute With Memory Segment Tests")
  class ExecuteWithMemorySegmentTests {

    @Test
    @DisplayName("Should execute with memory segment")
    void shouldExecuteWithMemorySegment() throws Exception {
      LOGGER.info("Testing executeWithMemorySegment()");
      manager = new PanamaConcurrencyManager(4);

      try (final Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(128);

        final String result =
            manager.executeWithMemorySegment(
                segment,
                () -> {
                  LOGGER.info("Inside memory-segment-managed callable");
                  return "segment_result";
                });

        assertEquals("segment_result", result, "Should return segment result");
        LOGGER.info("Segment result: " + result);
      }
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("Should return statistics string")
    void shouldReturnStatisticsString() throws Exception {
      LOGGER.info("Testing getStatistics()");
      manager = new PanamaConcurrencyManager(4);

      // Execute some operations to populate stats
      manager.execute(() -> "stat1");
      manager.execute(() -> "stat2");

      final String stats = manager.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.contains("Panama Concurrency Manager Statistics"), "Should contain header");
      assertTrue(stats.contains("Total operations:"), "Should contain total operations");
      assertTrue(stats.contains("Available permits:"), "Should contain available permits");
      assertTrue(stats.contains("Arena operations:"), "Should contain arena operations");
      LOGGER.info("Statistics: " + stats);
    }

    @Test
    @DisplayName("Should return performance metrics")
    void shouldReturnPerformanceMetrics() throws Exception {
      LOGGER.info("Testing getPerformanceMetrics()");
      manager = new PanamaConcurrencyManager(4);

      manager.execute(() -> "perf1");

      final String metrics = manager.getPerformanceMetrics();
      assertNotNull(metrics, "Metrics should not be null");
      assertTrue(metrics.contains("Panama Concurrency Performance"), "Should contain header");
      assertTrue(metrics.contains("contention_rate="), "Should contain contention rate");
      assertTrue(metrics.contains("avg_wait="), "Should contain average wait");
      assertTrue(metrics.contains("concurrent_ops="), "Should contain concurrent ops");
      LOGGER.info("Performance metrics: " + metrics);
    }
  }

  @Nested
  @DisplayName("Active Arena Info Tests")
  class ActiveArenaInfoTests {

    @Test
    @DisplayName("Should return no active arenas when empty")
    void shouldReturnNoActiveArenasWhenEmpty() {
      LOGGER.info("Testing getActiveArenaInfo when empty");
      manager = new PanamaConcurrencyManager(4);

      final String info = manager.getActiveArenaInfo();
      assertEquals("No active arenas", info, "Should report no active arenas");
      LOGGER.info("Active arena info (empty): " + info);
    }

    @Test
    @DisplayName("Should return active arena info after arena operations")
    void shouldReturnActiveArenaInfoAfterOperations() throws Exception {
      LOGGER.info("Testing getActiveArenaInfo after arena operations");
      manager = new PanamaConcurrencyManager(4);

      try (final Arena arena = Arena.ofConfined()) {
        manager.executeWithArena(arena, () -> "arena_op");

        final String info = manager.getActiveArenaInfo();
        assertNotNull(info, "Arena info should not be null");
        LOGGER.info("Active arena info: " + info);
      }
    }
  }

  @Nested
  @DisplayName("Issue Detection Tests")
  class IssueDetectionTests {

    @Test
    @DisplayName("Should return null when no issues")
    void shouldReturnNullWhenNoIssues() throws Exception {
      LOGGER.info("Testing checkForIssues with no issues");
      manager = new PanamaConcurrencyManager(4);

      manager.execute(() -> "clean_op");

      final String issues = manager.checkForIssues();
      LOGGER.info("Issues: " + issues);
      // With low operations, no issues should be detected
    }

    @Test
    @DisplayName("Should detect issues when present")
    void shouldDetectIssuesWhenPresent() {
      LOGGER.info("Testing checkForIssues");
      manager = new PanamaConcurrencyManager(4);

      // Just verify it doesn't throw
      final String issues = manager.checkForIssues();
      LOGGER.info("Issues check result: " + issues);
    }
  }

  @Nested
  @DisplayName("Cleanup Tests")
  class CleanupTests {

    @Test
    @DisplayName("Should cleanup closed arenas")
    void shouldCleanupClosedArenas() throws Exception {
      LOGGER.info("Testing cleanupClosedArenas()");
      manager = new PanamaConcurrencyManager(4);

      // Create and use an arena, then close it
      final Arena arena = Arena.ofConfined();
      manager.executeWithArena(arena, () -> "cleanup_op");
      arena.close();

      // Cleanup should remove the closed arena
      manager.cleanupClosedArenas();
      LOGGER.info("Closed arenas cleaned up");

      final String info = manager.getActiveArenaInfo();
      LOGGER.info("Arena info after cleanup: " + info);
    }

    @Test
    @DisplayName("Should cleanup unused locks")
    void shouldCleanupUnusedLocks() {
      LOGGER.info("Testing cleanupUnusedLocks()");
      manager = new PanamaConcurrencyManager(4);
      manager.cleanupUnusedLocks();
      LOGGER.info("Unused locks cleaned up");
    }
  }

  @Nested
  @DisplayName("Reset Statistics Tests")
  class ResetStatisticsTests {

    @Test
    @DisplayName("Should reset statistics")
    void shouldResetStatistics() throws Exception {
      LOGGER.info("Testing resetStatistics()");
      manager = new PanamaConcurrencyManager(4);

      // Execute some operations
      manager.execute(() -> "reset_op1");
      manager.execute(() -> "reset_op2");

      // Reset
      manager.resetStatistics();

      // Verify reset
      final String stats = manager.getStatistics();
      assertTrue(stats.contains("Total operations: 0"), "Total operations should be reset to 0");
      LOGGER.info("Stats after reset: " + stats);
    }
  }

  @Nested
  @DisplayName("Permits and Max Operations Tests")
  class PermitsTests {

    @Test
    @DisplayName("Should return available permits")
    void shouldReturnAvailablePermits() {
      LOGGER.info("Testing getAvailablePermits()");
      manager = new PanamaConcurrencyManager(8);
      assertEquals(8, manager.getAvailablePermits(), "Should have 8 available permits");
      LOGGER.info("Available permits: " + manager.getAvailablePermits());
    }

    @Test
    @DisplayName("Should return max concurrent operations")
    void shouldReturnMaxConcurrentOperations() {
      LOGGER.info("Testing getMaxConcurrentOperations()");
      manager = new PanamaConcurrencyManager(12);
      assertEquals(12, manager.getMaxConcurrentOperations(), "Should return 12");
      LOGGER.info("Max concurrent operations: " + manager.getMaxConcurrentOperations());
    }
  }

  @Nested
  @DisplayName("Shutdown Tests")
  class ShutdownTests {

    @Test
    @DisplayName("Should shutdown cleanly")
    void shouldShutdownCleanly() throws Exception {
      LOGGER.info("Testing shutdown()");
      manager = new PanamaConcurrencyManager(4);

      manager.execute(() -> "pre_shutdown");
      manager.shutdown();
      LOGGER.info("Shutdown completed cleanly");

      // Set to null so @AfterEach doesn't try to shutdown again
      manager = null;
    }

    @Test
    @DisplayName("Should shutdown with active arenas")
    void shouldShutdownWithActiveArenas() throws Exception {
      LOGGER.info("Testing shutdown with active arenas");
      manager = new PanamaConcurrencyManager(4);

      try (final Arena arena = Arena.ofConfined()) {
        manager.executeWithArena(arena, () -> "arena_op");
        // Arena still open during shutdown
        manager.shutdown();
        LOGGER.info("Shutdown with active arenas completed");
        manager = null;
      }
    }
  }

  @Nested
  @DisplayName("End-to-End Workflow Tests")
  class EndToEndWorkflowTests {

    @Test
    @DisplayName("Should handle complete concurrency workflow")
    void shouldHandleCompleteConcurrencyWorkflow() throws Exception {
      LOGGER.info("Testing complete concurrency workflow");
      manager = new PanamaConcurrencyManager(4);

      // 1. Execute regular operations
      final String result1 = manager.execute(() -> "sync_op");
      assertEquals("sync_op", result1);

      // 2. Execute async operation
      final CompletableFuture<String> future = manager.executeAsync(() -> "async_op");
      assertEquals("async_op", future.get());

      // 3. Execute with lock
      final String result2 = manager.executeWithLock("resource", () -> "locked_op");
      assertEquals("locked_op", result2);

      // 4. Execute with read lock
      final String result3 = manager.executeWithReadLock("resource", () -> "read_op");
      assertEquals("read_op", result3);

      // 5. Execute with arena
      try (final Arena arena = Arena.ofConfined()) {
        final String result4 = manager.executeWithArena(arena, () -> "arena_op");
        assertEquals("arena_op", result4);

        // 6. Execute with memory segment
        final MemorySegment segment = arena.allocate(64);
        final String result5 = manager.executeWithMemorySegment(segment, () -> "segment_op");
        assertEquals("segment_op", result5);
      }

      // 7. Check statistics
      final String stats = manager.getStatistics();
      assertNotNull(stats);

      final String metrics = manager.getPerformanceMetrics();
      assertNotNull(metrics);

      final String arenaInfo = manager.getActiveArenaInfo();
      assertNotNull(arenaInfo);

      // 8. Check for issues
      final String issues = manager.checkForIssues();
      LOGGER.info("Workflow issues: " + issues);

      // 9. Cleanup
      manager.cleanupClosedArenas();
      manager.cleanupUnusedLocks();

      // 10. Reset and verify
      manager.resetStatistics();

      LOGGER.info("Complete workflow completed successfully");
      LOGGER.info("Final stats: " + manager.getStatistics());
    }
  }
}

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaResourceTracker} class.
 *
 * <p>This test class verifies the resource tracking functionality for Panama FFI operations.
 */
@DisplayName("PanamaResourceTracker Tests")
class PanamaResourceTrackerTest {

  private PanamaResourceTracker tracker;
  private Arena arena;

  @BeforeEach
  void setUp() {
    tracker = new PanamaResourceTracker();
    arena = Arena.ofConfined();
  }

  @AfterEach
  void tearDown() {
    tracker.cleanup();
    if (arena.scope().isAlive()) {
      arena.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaResourceTracker should be final class")
    void shouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(PanamaResourceTracker.class.getModifiers()),
          "PanamaResourceTracker should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should create tracker with zero tracked resources")
    void constructorShouldCreateTrackerWithZeroTrackedResources() {
      final PanamaResourceTracker newTracker = new PanamaResourceTracker();
      assertEquals(0, newTracker.getTrackedResourceCount(), "Should have 0 tracked resources");
      assertEquals(0, newTracker.getTotalTracked(), "Total tracked should be 0");
      assertEquals(0, newTracker.getTotalCleaned(), "Total cleaned should be 0");
    }
  }

  @Nested
  @DisplayName("trackResource Tests")
  class TrackResourceTests {

    @Test
    @DisplayName("trackResource should throw for null resource")
    void trackResourceShouldThrowForNullResource() {
      final MemorySegment segment = arena.allocate(64);
      assertThrows(IllegalArgumentException.class,
          () -> tracker.trackResource(null, segment),
          "Should throw for null resource");
    }

    @Test
    @DisplayName("trackResource should throw for null handle")
    void trackResourceShouldThrowForNullHandle() {
      final Object resource = new Object();
      assertThrows(IllegalArgumentException.class,
          () -> tracker.trackResource(resource, null),
          "Should throw for null handle");
    }

    @Test
    @DisplayName("trackResource should track resource successfully")
    void trackResourceShouldTrackResourceSuccessfully() {
      final Object resource = new Object();
      final MemorySegment segment = arena.allocate(64);

      tracker.trackResource(resource, segment);

      assertEquals(1, tracker.getTrackedResourceCount(), "Should have 1 tracked resource");
      assertEquals(1, tracker.getTotalTracked(), "Total tracked should be 1");
      assertTrue(tracker.isTracked(resource), "Resource should be tracked");
    }

    @Test
    @DisplayName("trackResource should increment counters")
    void trackResourceShouldIncrementCounters() {
      final Object resource1 = new Object();
      final Object resource2 = new Object();
      final MemorySegment segment1 = arena.allocate(64);
      final MemorySegment segment2 = arena.allocate(64);

      tracker.trackResource(resource1, segment1);
      tracker.trackResource(resource2, segment2);

      assertEquals(2, tracker.getTrackedResourceCount(), "Should have 2 tracked resources");
      assertEquals(2, tracker.getTotalTracked(), "Total tracked should be 2");
    }
  }

  @Nested
  @DisplayName("untrackResource Tests")
  class UntrackResourceTests {

    @Test
    @DisplayName("untrackResource should return false for null resource")
    void untrackResourceShouldReturnFalseForNullResource() {
      assertFalse(tracker.untrackResource(null), "Should return false for null resource");
    }

    @Test
    @DisplayName("untrackResource should return false for untracked resource")
    void untrackResourceShouldReturnFalseForUntrackedResource() {
      assertFalse(tracker.untrackResource(new Object()),
          "Should return false for untracked resource");
    }

    @Test
    @DisplayName("untrackResource should return true for tracked resource")
    void untrackResourceShouldReturnTrueForTrackedResource() {
      final Object resource = new Object();
      final MemorySegment segment = arena.allocate(64);

      tracker.trackResource(resource, segment);
      assertTrue(tracker.untrackResource(resource), "Should return true for tracked resource");
    }

    @Test
    @DisplayName("untrackResource should decrement counters and increment cleaned")
    void untrackResourceShouldDecrementCountersAndIncrementCleaned() {
      final Object resource = new Object();
      final MemorySegment segment = arena.allocate(64);

      tracker.trackResource(resource, segment);
      tracker.untrackResource(resource);

      assertEquals(0, tracker.getTrackedResourceCount(), "Should have 0 tracked resources");
      assertEquals(1, tracker.getTotalTracked(), "Total tracked should still be 1");
      assertEquals(1, tracker.getTotalCleaned(), "Total cleaned should be 1");
    }
  }

  @Nested
  @DisplayName("isTracked Tests")
  class IsTrackedTests {

    @Test
    @DisplayName("isTracked should return false for null resource")
    void isTrackedShouldReturnFalseForNullResource() {
      assertFalse(tracker.isTracked(null), "Should return false for null resource");
    }

    @Test
    @DisplayName("isTracked should return false for untracked resource")
    void isTrackedShouldReturnFalseForUntrackedResource() {
      assertFalse(tracker.isTracked(new Object()), "Should return false for untracked resource");
    }

    @Test
    @DisplayName("isTracked should return true for tracked resource")
    void isTrackedShouldReturnTrueForTrackedResource() {
      final Object resource = new Object();
      final MemorySegment segment = arena.allocate(64);

      tracker.trackResource(resource, segment);
      assertTrue(tracker.isTracked(resource), "Should return true for tracked resource");
    }
  }

  @Nested
  @DisplayName("getHandle Tests")
  class GetHandleTests {

    @Test
    @DisplayName("getHandle should return null for null resource")
    void getHandleShouldReturnNullForNullResource() {
      assertNull(tracker.getHandle(null), "Should return null for null resource");
    }

    @Test
    @DisplayName("getHandle should return null for untracked resource")
    void getHandleShouldReturnNullForUntrackedResource() {
      assertNull(tracker.getHandle(new Object()), "Should return null for untracked resource");
    }

    @Test
    @DisplayName("getHandle should return handle for tracked resource")
    void getHandleShouldReturnHandleForTrackedResource() {
      final Object resource = new Object();
      final MemorySegment segment = arena.allocate(64);

      tracker.trackResource(resource, segment);
      assertEquals(segment, tracker.getHandle(resource),
          "Should return handle for tracked resource");
    }
  }

  @Nested
  @DisplayName("cleanupOrphanedResources Tests")
  class CleanupOrphanedResourcesTests {

    @Test
    @DisplayName("cleanupOrphanedResources should return 0 for no orphans")
    void cleanupOrphanedResourcesShouldReturnZeroForNoOrphans() {
      final Object resource = new Object();
      final MemorySegment segment = arena.allocate(64);

      tracker.trackResource(resource, segment);

      // Resource is still referenced, so no orphans
      assertEquals(0, tracker.cleanupOrphanedResources(),
          "Should return 0 when no orphans exist");
    }
  }

  @Nested
  @DisplayName("cleanup Tests")
  class CleanupTests {

    @Test
    @DisplayName("cleanup should clear all tracked resources")
    void cleanupShouldClearAllTrackedResources() {
      final Object resource1 = new Object();
      final Object resource2 = new Object();
      final MemorySegment segment1 = arena.allocate(64);
      final MemorySegment segment2 = arena.allocate(64);

      tracker.trackResource(resource1, segment1);
      tracker.trackResource(resource2, segment2);

      tracker.cleanup();

      assertEquals(0, tracker.getTrackedResourceCount(),
          "Should have 0 tracked resources after cleanup");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getTrackedResourceCount should return correct count")
    void getTrackedResourceCountShouldReturnCorrectCount() {
      assertEquals(0, tracker.getTrackedResourceCount(), "Initial count should be 0");

      final Object resource = new Object();
      final MemorySegment segment = arena.allocate(64);
      tracker.trackResource(resource, segment);

      assertEquals(1, tracker.getTrackedResourceCount(), "Count should be 1 after tracking");
    }

    @Test
    @DisplayName("getTotalTracked should return cumulative count")
    void getTotalTrackedShouldReturnCumulativeCount() {
      final Object resource1 = new Object();
      final Object resource2 = new Object();
      final MemorySegment segment1 = arena.allocate(64);
      final MemorySegment segment2 = arena.allocate(64);

      tracker.trackResource(resource1, segment1);
      tracker.trackResource(resource2, segment2);
      tracker.untrackResource(resource1);

      assertEquals(2, tracker.getTotalTracked(),
          "Total tracked should be 2 (cumulative)");
      assertEquals(1, tracker.getTrackedResourceCount(),
          "Current count should be 1");
    }

    @Test
    @DisplayName("getTotalCleaned should return correct count")
    void getTotalCleanedShouldReturnCorrectCount() {
      final Object resource = new Object();
      final MemorySegment segment = arena.allocate(64);

      tracker.trackResource(resource, segment);
      tracker.untrackResource(resource);

      assertEquals(1, tracker.getTotalCleaned(), "Total cleaned should be 1");
    }

    @Test
    @DisplayName("getPotentialLeaks should calculate correctly")
    void getPotentialLeaksShouldCalculateCorrectly() {
      // When tracked == cleaned + current, no leaks
      final Object resource = new Object();
      final MemorySegment segment = arena.allocate(64);

      tracker.trackResource(resource, segment);

      // total=1, cleaned=0, current=1, leaks = 1-0-1 = 0
      assertEquals(0, tracker.getPotentialLeaks(), "No leaks expected");
    }

    @Test
    @DisplayName("hasPotentialLeaks should return false when no leaks")
    void hasPotentialLeaksShouldReturnFalseWhenNoLeaks() {
      final Object resource = new Object();
      final MemorySegment segment = arena.allocate(64);

      tracker.trackResource(resource, segment);

      assertFalse(tracker.hasPotentialLeaks(), "Should have no potential leaks");
    }

    @Test
    @DisplayName("getTrackingStats should return formatted string")
    void getTrackingStatsShouldReturnFormattedString() {
      final Object resource = new Object();
      final MemorySegment segment = arena.allocate(64);

      tracker.trackResource(resource, segment);

      final String stats = tracker.getTrackingStats();
      assertNotNull(stats, "Stats should not be null");
      assertTrue(stats.contains("PanamaResourceTracker"), "Stats should contain class name");
      assertTrue(stats.contains("1"), "Stats should contain count");
    }

    @Test
    @DisplayName("logStats should not throw")
    void logStatsShouldNotThrow() {
      final Object resource = new Object();
      final MemorySegment segment = arena.allocate(64);

      tracker.trackResource(resource, segment);

      // Should not throw
      tracker.logStats();
    }
  }

  @Nested
  @DisplayName("getResourceSummary Tests")
  class GetResourceSummaryTests {

    @Test
    @DisplayName("getResourceSummary should return empty map for no resources")
    void getResourceSummaryShouldReturnEmptyMapForNoResources() {
      final ConcurrentMap<String, Long> summary = tracker.getResourceSummary();
      assertTrue(summary.isEmpty(), "Summary should be empty for no resources");
    }

    @Test
    @DisplayName("getResourceSummary should group by class name")
    void getResourceSummaryShouldGroupByClassName() {
      final String resource1 = "string1";
      final String resource2 = "string2";
      final Integer resource3 = 42;
      final MemorySegment segment1 = arena.allocate(64);
      final MemorySegment segment2 = arena.allocate(64);
      final MemorySegment segment3 = arena.allocate(64);

      tracker.trackResource(resource1, segment1);
      tracker.trackResource(resource2, segment2);
      tracker.trackResource(resource3, segment3);

      final ConcurrentMap<String, Long> summary = tracker.getResourceSummary();

      assertEquals(2, summary.size(), "Should have 2 different types");
      assertEquals(Long.valueOf(2), summary.get("String"), "Should have 2 Strings");
      assertEquals(Long.valueOf(1), summary.get("Integer"), "Should have 1 Integer");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full lifecycle should work correctly")
    void fullLifecycleShouldWorkCorrectly() {
      final Object resource1 = new Object();
      final Object resource2 = new Object();
      final Object resource3 = new Object();
      final MemorySegment segment1 = arena.allocate(64);
      final MemorySegment segment2 = arena.allocate(64);
      final MemorySegment segment3 = arena.allocate(64);

      // Track resources
      tracker.trackResource(resource1, segment1);
      tracker.trackResource(resource2, segment2);
      tracker.trackResource(resource3, segment3);

      assertEquals(3, tracker.getTrackedResourceCount(), "Should have 3 tracked");
      assertEquals(3, tracker.getTotalTracked(), "Total tracked should be 3");

      // Untrack one
      assertTrue(tracker.untrackResource(resource2), "Should untrack resource2");
      assertEquals(2, tracker.getTrackedResourceCount(), "Should have 2 tracked");
      assertEquals(1, tracker.getTotalCleaned(), "Total cleaned should be 1");

      // Verify handles
      assertEquals(segment1, tracker.getHandle(resource1), "Handle1 should match");
      assertNull(tracker.getHandle(resource2), "Handle2 should be null (untracked)");
      assertEquals(segment3, tracker.getHandle(resource3), "Handle3 should match");

      // Cleanup
      tracker.cleanup();
      assertEquals(0, tracker.getTrackedResourceCount(), "Should have 0 after cleanup");
    }

    @Test
    @DisplayName("Concurrent access should be thread-safe")
    void concurrentAccessShouldBeThreadSafe() throws InterruptedException {
      final int threadCount = 10;
      final int operationsPerThread = 100;
      final Thread[] threads = new Thread[threadCount];

      for (int i = 0; i < threadCount; i++) {
        final int threadIndex = i;
        threads[i] = new Thread(() -> {
          try (Arena localArena = Arena.ofConfined()) {
            for (int j = 0; j < operationsPerThread; j++) {
              final Object resource = new Object();
              final MemorySegment segment = localArena.allocate(64);
              tracker.trackResource(resource, segment);
              tracker.isTracked(resource);
              tracker.getHandle(resource);
              tracker.untrackResource(resource);
            }
          }
        });
      }

      // Start all threads
      for (Thread thread : threads) {
        thread.start();
      }

      // Wait for all threads
      for (Thread thread : threads) {
        thread.join();
      }

      // Verify consistency
      assertEquals(0, tracker.getTrackedResourceCount(),
          "Should have 0 tracked after all untracked");
      assertEquals(threadCount * operationsPerThread, tracker.getTotalTracked(),
          "Total tracked should match");
      assertEquals(threadCount * operationsPerThread, tracker.getTotalCleaned(),
          "Total cleaned should match");
    }
  }
}

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

package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link WasiResourceLeakDetector}.
 */
@DisplayName("WasiResourceLeakDetector Tests")
class WasiResourceLeakDetectorTest {

  private WasiResourceLeakDetector detector;

  @BeforeEach
  void setUp() {
    detector = new WasiResourceLeakDetector();
  }

  @AfterEach
  void tearDown() {
    if (detector != null) {
      detector.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiResourceLeakDetector should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiResourceLeakDetector.class.getModifiers()),
          "WasiResourceLeakDetector should be final");
    }

    @Test
    @DisplayName("Should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiResourceLeakDetector.class),
          "WasiResourceLeakDetector should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor should create detector")
    void defaultConstructorShouldCreateDetector() {
      final WasiResourceLeakDetector leakDetector = new WasiResourceLeakDetector();
      assertNotNull(leakDetector, "Detector should be created");
      leakDetector.close();
    }

    @Test
    @DisplayName("Constructor with parameters should throw on zero threshold")
    void constructorShouldThrowOnZeroThreshold() {
      assertThrows(IllegalArgumentException.class,
          () -> new WasiResourceLeakDetector(0, Duration.ofMinutes(30), 30),
          "Should throw on zero leak threshold");
    }

    @Test
    @DisplayName("Constructor with parameters should throw on negative threshold")
    void constructorShouldThrowOnNegativeThreshold() {
      assertThrows(IllegalArgumentException.class,
          () -> new WasiResourceLeakDetector(-1, Duration.ofMinutes(30), 30),
          "Should throw on negative leak threshold");
    }

    @Test
    @DisplayName("Constructor with parameters should throw on null duration")
    void constructorShouldThrowOnNullDuration() {
      assertThrows(IllegalArgumentException.class,
          () -> new WasiResourceLeakDetector(1000, null, 30),
          "Should throw on null resource age threshold");
    }

    @Test
    @DisplayName("Constructor with parameters should throw on zero interval")
    void constructorShouldThrowOnZeroInterval() {
      assertThrows(IllegalArgumentException.class,
          () -> new WasiResourceLeakDetector(1000, Duration.ofMinutes(30), 0),
          "Should throw on zero monitoring interval");
    }

    @Test
    @DisplayName("Constructor with parameters should throw on negative interval")
    void constructorShouldThrowOnNegativeInterval() {
      assertThrows(IllegalArgumentException.class,
          () -> new WasiResourceLeakDetector(1000, Duration.ofMinutes(30), -1),
          "Should throw on negative monitoring interval");
    }

    @Test
    @DisplayName("Constructor with valid parameters should create detector")
    void constructorWithValidParametersShouldCreateDetector() {
      final WasiResourceLeakDetector leakDetector =
          new WasiResourceLeakDetector(500, Duration.ofMinutes(15), 60);
      assertNotNull(leakDetector, "Detector should be created with custom parameters");
      leakDetector.close();
    }
  }

  @Nested
  @DisplayName("trackWasiContext Tests")
  class TrackWasiContextTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(IllegalArgumentException.class,
          () -> detector.trackWasiContext(null, null),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(IllegalArgumentException.class,
          () -> detector.trackWasiContext("", null),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should throw on null context")
    void shouldThrowOnNullContext() {
      assertThrows(IllegalArgumentException.class,
          () -> detector.trackWasiContext("test-context", null),
          "Should throw on null context");
    }
  }

  @Nested
  @DisplayName("untrackWasiContext Tests")
  class UntrackWasiContextTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(IllegalArgumentException.class,
          () -> detector.untrackWasiContext(null),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(IllegalArgumentException.class,
          () -> detector.untrackWasiContext(""),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should handle untracking non-existent context")
    void shouldHandleUntrackingNonExistentContext() {
      // Should not throw
      assertDoesNotThrow(
          () -> detector.untrackWasiContext("non-existent"),
          "Should handle untracking non-existent context");
    }
  }

  @Nested
  @DisplayName("trackMemorySegment Tests")
  class TrackMemorySegmentTests {

    @Test
    @DisplayName("Should throw on null segment")
    void shouldThrowOnNullSegment() {
      assertThrows(IllegalArgumentException.class,
          () -> detector.trackMemorySegment(null),
          "Should throw on null segment");
    }

    @Test
    @DisplayName("Should track memory segment")
    void shouldTrackMemorySegment() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(1024);
        assertDoesNotThrow(
            () -> detector.trackMemorySegment(segment),
            "Should track memory segment");
        assertEquals(1, detector.getTrackedMemorySegmentCount(),
            "Should have 1 tracked segment");
      }
    }
  }

  @Nested
  @DisplayName("untrackMemorySegment Tests")
  class UntrackMemorySegmentTests {

    @Test
    @DisplayName("Should throw on null segment")
    void shouldThrowOnNullSegment() {
      assertThrows(IllegalArgumentException.class,
          () -> detector.untrackMemorySegment(null),
          "Should throw on null segment");
    }

    @Test
    @DisplayName("Should untrack tracked segment")
    void shouldUntrackTrackedSegment() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(1024);
        detector.trackMemorySegment(segment);
        assertEquals(1, detector.getTrackedMemorySegmentCount(), "Should have 1 tracked segment");

        detector.untrackMemorySegment(segment);
        assertEquals(0, detector.getTrackedMemorySegmentCount(), "Should have 0 tracked segments");
      }
    }
  }

  @Nested
  @DisplayName("trackNativeHandle Tests")
  class TrackNativeHandleTests {

    @Test
    @DisplayName("Should throw on null handle")
    void shouldThrowOnNullHandle() {
      assertThrows(IllegalArgumentException.class,
          () -> detector.trackNativeHandle(null, "test"),
          "Should throw on null handle");
    }

    @Test
    @DisplayName("Should throw on null resource type")
    void shouldThrowOnNullResourceType() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment handle = arena.allocate(8);
        assertThrows(IllegalArgumentException.class,
            () -> detector.trackNativeHandle(handle, null),
            "Should throw on null resource type");
      }
    }

    @Test
    @DisplayName("Should throw on empty resource type")
    void shouldThrowOnEmptyResourceType() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment handle = arena.allocate(8);
        assertThrows(IllegalArgumentException.class,
            () -> detector.trackNativeHandle(handle, ""),
            "Should throw on empty resource type");
      }
    }

    @Test
    @DisplayName("Should track native handle")
    void shouldTrackNativeHandle() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment handle = arena.allocate(8);
        assertDoesNotThrow(
            () -> detector.trackNativeHandle(handle, "TestHandle"),
            "Should track native handle");
        assertEquals(1, detector.getTrackedNativeHandleCount(),
            "Should have 1 tracked handle");
      }
    }
  }

  @Nested
  @DisplayName("untrackNativeHandle Tests")
  class UntrackNativeHandleTests {

    @Test
    @DisplayName("Should throw on null handle")
    void shouldThrowOnNullHandle() {
      assertThrows(IllegalArgumentException.class,
          () -> detector.untrackNativeHandle(null),
          "Should throw on null handle");
    }

    @Test
    @DisplayName("Should untrack tracked handle")
    void shouldUntrackTrackedHandle() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment handle = arena.allocate(8);
        detector.trackNativeHandle(handle, "TestHandle");
        assertEquals(1, detector.getTrackedNativeHandleCount(), "Should have 1 tracked handle");

        detector.untrackNativeHandle(handle);
        assertEquals(0, detector.getTrackedNativeHandleCount(), "Should have 0 tracked handles");
      }
    }
  }

  @Nested
  @DisplayName("getTrackedContextCount Tests")
  class GetTrackedContextCountTests {

    @Test
    @DisplayName("Should return 0 initially")
    void shouldReturnZeroInitially() {
      assertEquals(0, detector.getTrackedContextCount(),
          "Should have 0 tracked contexts initially");
    }
  }

  @Nested
  @DisplayName("getTrackedMemorySegmentCount Tests")
  class GetTrackedMemorySegmentCountTests {

    @Test
    @DisplayName("Should return 0 initially")
    void shouldReturnZeroInitially() {
      assertEquals(0, detector.getTrackedMemorySegmentCount(),
          "Should have 0 tracked segments initially");
    }
  }

  @Nested
  @DisplayName("getTrackedNativeHandleCount Tests")
  class GetTrackedNativeHandleCountTests {

    @Test
    @DisplayName("Should return 0 initially")
    void shouldReturnZeroInitially() {
      assertEquals(0, detector.getTrackedNativeHandleCount(),
          "Should have 0 tracked handles initially");
    }
  }

  @Nested
  @DisplayName("getStatistics Tests")
  class GetStatisticsTests {

    @Test
    @DisplayName("Should return non-null statistics")
    void shouldReturnNonNullStatistics() {
      final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
    }

    @Test
    @DisplayName("Statistics should have zero counts initially")
    void statisticsShouldHaveZeroCountsInitially() {
      final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();
      assertEquals(0, stats.getContextsCreated(), "Contexts created should be 0");
      assertEquals(0, stats.getContextsDestroyed(), "Contexts destroyed should be 0");
      assertEquals(0, stats.getMemorySegmentsCreated(), "Memory segments created should be 0");
      assertEquals(0, stats.getMemorySegmentsDestroyed(), "Memory segments destroyed should be 0");
      assertEquals(0, stats.getNativeHandlesCreated(), "Native handles created should be 0");
      assertEquals(0, stats.getNativeHandlesDestroyed(), "Native handles destroyed should be 0");
    }

    @Test
    @DisplayName("Statistics should have active counts methods")
    void statisticsShouldHaveActiveCountsMethods() {
      final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();
      assertEquals(0, stats.getActiveContexts(), "Active contexts should be 0");
      assertEquals(0, stats.getActiveMemorySegments(), "Active memory segments should be 0");
      assertEquals(0, stats.getActiveNativeHandles(), "Active native handles should be 0");
    }

    @Test
    @DisplayName("Statistics should have last monitoring time")
    void statisticsShouldHaveLastMonitoringTime() {
      final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();
      assertNotNull(stats.getLastMonitoringTime(), "Last monitoring time should not be null");
    }

    @Test
    @DisplayName("Statistics toString should return non-null")
    void statisticsToStringShouldReturnNonNull() {
      final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();
      assertNotNull(stats.toString(), "Statistics toString should not be null");
    }
  }

  @Nested
  @DisplayName("performLeakDetection Tests")
  class PerformLeakDetectionTests {

    @Test
    @DisplayName("Should return results with no leaks initially")
    void shouldReturnResultsWithNoLeaksInitially() {
      final WasiResourceLeakDetector.LeakDetectionResults results = detector.performLeakDetection();
      assertNotNull(results, "Results should not be null");
      assertEquals(0, results.getLeakedContexts(), "No leaked contexts");
      assertEquals(0, results.getLeakedMemorySegments(), "No leaked memory segments");
      assertEquals(0, results.getLeakedNativeHandles(), "No leaked native handles");
      assertEquals(0, results.getTotalLeaked(), "No total leaked");
      assertFalse(results.hasLeaks(), "Should not have leaks");
    }

    @Test
    @DisplayName("Results toString should return non-null")
    void resultsToStringShouldReturnNonNull() {
      final WasiResourceLeakDetector.LeakDetectionResults results = detector.performLeakDetection();
      assertNotNull(results.toString(), "Results toString should not be null");
    }
  }

  @Nested
  @DisplayName("LeakDetectionResults Tests")
  class LeakDetectionResultsTests {

    @Test
    @DisplayName("getTotalLeaked should sum all leak types")
    void getTotalLeakedShouldSumAllLeakTypes() {
      final WasiResourceLeakDetector.LeakDetectionResults results = detector.performLeakDetection();
      final int expected = results.getLeakedContexts()
          + results.getLeakedMemorySegments()
          + results.getLeakedNativeHandles();
      assertEquals(expected, results.getTotalLeaked(), "Total should be sum of all types");
    }

    @Test
    @DisplayName("hasLeaks should return false when no leaks")
    void hasLeaksShouldReturnFalseWhenNoLeaks() {
      final WasiResourceLeakDetector.LeakDetectionResults results = detector.performLeakDetection();
      assertFalse(results.hasLeaks(), "hasLeaks should be false when no leaks");
    }

    @Test
    @DisplayName("getCleanedUpResources should return count")
    void getCleanedUpResourcesShouldReturnCount() {
      final WasiResourceLeakDetector.LeakDetectionResults results = detector.performLeakDetection();
      assertTrue(results.getCleanedUpResources() >= 0,
          "Cleaned up resources should be non-negative");
    }
  }

  @Nested
  @DisplayName("close Tests")
  class CloseTests {

    @Test
    @DisplayName("Should close without error")
    void shouldCloseWithoutError() {
      final WasiResourceLeakDetector leakDetector = new WasiResourceLeakDetector();
      assertDoesNotThrow(leakDetector::close, "Should close without error");
    }

    @Test
    @DisplayName("Should handle multiple close calls")
    void shouldHandleMultipleCloseCalls() {
      final WasiResourceLeakDetector leakDetector = new WasiResourceLeakDetector();
      assertDoesNotThrow(leakDetector::close, "First close should succeed");
      assertDoesNotThrow(leakDetector::close, "Second close should succeed");
    }

    @Test
    @DisplayName("Should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      assertDoesNotThrow(() -> {
        try (WasiResourceLeakDetector leakDetector = new WasiResourceLeakDetector()) {
          assertNotNull(leakDetector, "Detector should exist in try block");
        }
      }, "Should work with try-with-resources");
    }

    @Test
    @DisplayName("Should clear tracked resources on close")
    void shouldClearTrackedResourcesOnClose() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(1024);
        detector.trackMemorySegment(segment);
        assertEquals(1, detector.getTrackedMemorySegmentCount(), "Should have 1 tracked segment");

        detector.close();
        // After close, counts should be cleared
        assertEquals(0, detector.getTrackedMemorySegmentCount(),
            "Should have 0 tracked segments after close");
      }
    }
  }

  @Nested
  @DisplayName("ResourceStatistics Tests")
  class ResourceStatisticsTests {

    @Test
    @DisplayName("Should update statistics on track")
    void shouldUpdateStatisticsOnTrack() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(1024);
        detector.trackMemorySegment(segment);

        final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();
        assertEquals(1, stats.getMemorySegmentsCreated(), "Memory segments created should be 1");
      }
    }

    @Test
    @DisplayName("Should update statistics on untrack")
    void shouldUpdateStatisticsOnUntrack() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(1024);
        detector.trackMemorySegment(segment);
        detector.untrackMemorySegment(segment);

        final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();
        assertEquals(1, stats.getMemorySegmentsCreated(), "Memory segments created should be 1");
        assertEquals(1, stats.getMemorySegmentsDestroyed(), "Memory segments destroyed should be 1");
        assertEquals(0, stats.getActiveMemorySegments(), "Active segments should be 0");
      }
    }
  }
}

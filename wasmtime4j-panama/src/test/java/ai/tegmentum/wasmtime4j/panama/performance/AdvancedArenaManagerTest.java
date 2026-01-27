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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link AdvancedArenaManager}.
 *
 * <p>These tests invoke actual methods on the singleton arena manager to exercise code paths and
 * improve JaCoCo coverage. The class uses Panama API (Arena, MemorySegment, MemoryLayout) for
 * pooled allocation, zero-copy, bulk operations, and adaptive memory management.
 *
 * <p>Note: AdvancedArenaManager is a singleton with no reset method. Tests use delta-based
 * assertions where absolute values cannot be guaranteed due to shared state across tests.
 */
@DisplayName("AdvancedArenaManager Integration Tests")
class AdvancedArenaManagerTest {

  private static final Logger LOGGER = Logger.getLogger(AdvancedArenaManagerTest.class.getName());

  private AdvancedArenaManager manager;

  @BeforeEach
  void setUp() {
    manager = AdvancedArenaManager.getInstance();
    AdvancedArenaManager.setEnabled(true);
  }

  @AfterEach
  void tearDown() {
    AdvancedArenaManager.setEnabled(true);
  }

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("should return same instance on multiple calls")
    void shouldReturnSameInstance() {
      final AdvancedArenaManager first = AdvancedArenaManager.getInstance();
      final AdvancedArenaManager second = AdvancedArenaManager.getInstance();
      assertSame(first, second, "getInstance should return the same singleton instance");
      LOGGER.info("Singleton instance verified: " + first);
    }

    @Test
    @DisplayName("should return non-null instance")
    void shouldReturnNonNullInstance() {
      assertNotNull(AdvancedArenaManager.getInstance(), "getInstance should never return null");
    }
  }

  @Nested
  @DisplayName("Enable/Disable Tests")
  class EnableDisableTests {

    @Test
    @DisplayName("should be enabled in test setup")
    void shouldBeEnabledInSetup() {
      assertTrue(
          AdvancedArenaManager.isEnabled(),
          "Arena management should be enabled (set in @BeforeEach)");
    }

    @Test
    @DisplayName("should disable management")
    void shouldDisableManagement() {
      AdvancedArenaManager.setEnabled(false);
      assertFalse(
          AdvancedArenaManager.isEnabled(),
          "Management should be disabled after setEnabled(false)");
      LOGGER.info("Disabled arena management");
    }

    @Test
    @DisplayName("should re-enable management")
    void shouldReEnableManagement() {
      AdvancedArenaManager.setEnabled(false);
      assertFalse(AdvancedArenaManager.isEnabled());
      AdvancedArenaManager.setEnabled(true);
      assertTrue(
          AdvancedArenaManager.isEnabled(), "Management should be enabled after setEnabled(true)");
    }
  }

  @Nested
  @DisplayName("Allocate Optimized By Size Tests")
  class AllocateOptimizedBySizeTests {

    @Test
    @DisplayName("should allocate small segment (< 1024 bytes, zero-copy strategy)")
    void shouldAllocateSmallSegment() {
      final MemorySegment segment = manager.allocateOptimized(64);
      assertNotNull(segment, "Allocated segment should not be null");
      assertEquals(64, segment.byteSize(), "Segment should have exactly 64 bytes");
      assertTrue(segment.scope().isAlive(), "Segment scope should be alive");
      LOGGER.info("Allocated small segment: size=" + segment.byteSize());
    }

    @Test
    @DisplayName("should allocate medium segment (> 1024 bytes, optimized strategy)")
    void shouldAllocateMediumSegment() {
      final MemorySegment segment = manager.allocateOptimized(4096);
      assertNotNull(segment, "Segment should not be null");
      assertEquals(4096, segment.byteSize(), "Segment should have exactly 4096 bytes");
      LOGGER.info("Allocated medium segment: size=" + segment.byteSize());
    }

    @Test
    @DisplayName("should allocate large segment (> max arena size)")
    void shouldAllocateLargeSegment() {
      final MemorySegment segment = manager.allocateOptimized(100000);
      assertNotNull(segment, "Large segment should not be null");
      assertEquals(100000, segment.byteSize(), "Segment should have exactly 100000 bytes");
      LOGGER.info("Allocated large segment: size=" + segment.byteSize());
    }

    @Test
    @DisplayName("should allocate multiple segments sequentially")
    void shouldAllocateMultipleSegments() {
      for (int i = 0; i < 10; i++) {
        final MemorySegment segment = manager.allocateOptimized(128);
        assertNotNull(segment, "Segment " + i + " should not be null");
        assertEquals(128, segment.byteSize(), "Segment " + i + " should have 128 bytes");
      }
      LOGGER.info("Allocated 10 segments of 128 bytes");
    }

    @Test
    @DisplayName("should allocate when management is disabled")
    void shouldAllocateWhenDisabled() {
      AdvancedArenaManager.setEnabled(false);
      final MemorySegment segment = manager.allocateOptimized(256);
      assertNotNull(segment, "Segment should be allocated even when disabled");
      assertEquals(256, segment.byteSize(), "Segment should have exactly 256 bytes");
      LOGGER.info("Allocated segment with management disabled");
    }

    @Test
    @DisplayName("should write and read data in allocated segment")
    void shouldWriteAndReadData() {
      final MemorySegment segment = manager.allocateOptimized(32);
      segment.set(ValueLayout.JAVA_LONG, 0, 987654321L);
      final long value = segment.get(ValueLayout.JAVA_LONG, 0);
      assertEquals(987654321L, value, "Read should match written value");
      LOGGER.info("Write/read verified: " + value);
    }
  }

  @Nested
  @DisplayName("Allocate Optimized By Layout Tests")
  class AllocateOptimizedByLayoutTests {

    @Test
    @DisplayName("should allocate with JAVA_INT layout")
    void shouldAllocateWithIntLayout() {
      final MemorySegment segment = manager.allocateOptimized(ValueLayout.JAVA_INT);
      assertNotNull(segment, "Int layout segment should not be null");
      assertEquals(
          ValueLayout.JAVA_INT.byteSize(),
          segment.byteSize(),
          "Segment should match JAVA_INT size");
      LOGGER.info("Allocated JAVA_INT segment: size=" + segment.byteSize());
    }

    @Test
    @DisplayName("should allocate with JAVA_LONG layout")
    void shouldAllocateWithLongLayout() {
      final MemorySegment segment = manager.allocateOptimized(ValueLayout.JAVA_LONG);
      assertNotNull(segment);
      assertEquals(ValueLayout.JAVA_LONG.byteSize(), segment.byteSize());
    }

    @Test
    @DisplayName("should allocate with JAVA_DOUBLE ValueLayout (triggers alignment counter)")
    void shouldAllocateWithValueLayout() {
      // ValueLayout instanceof check triggers alignmentOptimizations counter
      final MemorySegment segment = manager.allocateOptimized(ValueLayout.JAVA_DOUBLE);
      assertNotNull(segment);
      assertEquals(ValueLayout.JAVA_DOUBLE.byteSize(), segment.byteSize());
      LOGGER.info("ValueLayout allocation triggered alignment optimization");
    }

    @Test
    @DisplayName("should allocate with struct layout")
    void shouldAllocateWithStructLayout() {
      final MemoryLayout structLayout =
          MemoryLayout.structLayout(
              ValueLayout.JAVA_INT.withName("x"), ValueLayout.JAVA_INT.withName("y"));
      final MemorySegment segment = manager.allocateOptimized(structLayout);
      assertNotNull(segment);
      assertEquals(structLayout.byteSize(), segment.byteSize());
      LOGGER.info("Allocated struct layout: " + structLayout.byteSize() + " bytes");
    }

    @Test
    @DisplayName("should allocate layout when disabled")
    void shouldAllocateLayoutWhenDisabled() {
      AdvancedArenaManager.setEnabled(false);
      final MemorySegment segment = manager.allocateOptimized(ValueLayout.JAVA_INT);
      assertNotNull(segment);
      assertEquals(ValueLayout.JAVA_INT.byteSize(), segment.byteSize());
    }
  }

  @Nested
  @DisplayName("Zero-Copy Tests")
  class ZeroCopyTests {

    @Test
    @DisplayName("should reuse source segment when large enough")
    void shouldReuseSourceSegment() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment source = arena.allocate(256);
        source.set(ValueLayout.JAVA_INT, 0, 42);

        final Integer result =
            manager.executeZeroCopy(
                source,
                128,
                segment -> {
                  // segment should be a slice of source since source >= required size
                  return segment.get(ValueLayout.JAVA_INT, 0);
                });

        assertEquals(42, result, "Should read data from source segment (zero-copy reuse)");
        LOGGER.info("Zero-copy reused source segment, value=" + result);
      }
    }

    @Test
    @DisplayName("should allocate new segment when source is too small")
    void shouldAllocateWhenSourceTooSmall() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment source = arena.allocate(32);

        final Long result =
            manager.executeZeroCopy(
                source,
                256,
                segment -> {
                  assertNotNull(segment, "Segment should not be null");
                  assertTrue(segment.byteSize() >= 256, "New segment should be at least 256 bytes");
                  return segment.byteSize();
                });

        assertTrue(result >= 256, "Allocated segment should be >= 256 bytes");
        LOGGER.info("New segment allocated because source was too small: " + result);
      }
    }

    @Test
    @DisplayName("should handle null source segment")
    void shouldHandleNullSourceSegment() {
      final Long result =
          manager.executeZeroCopy(
              null,
              64,
              segment -> {
                assertNotNull(segment);
                return segment.byteSize();
              });
      assertTrue(result >= 64, "Should allocate new segment when source is null");
      LOGGER.info("Handled null source, allocated new segment: " + result);
    }

    @Test
    @DisplayName("should execute zero-copy when disabled")
    void shouldExecuteZeroCopyWhenDisabled() {
      AdvancedArenaManager.setEnabled(false);
      final Long result =
          manager.executeZeroCopy(
              null,
              128,
              segment -> {
                assertNotNull(segment);
                return segment.byteSize();
              });
      assertTrue(result >= 128, "Should work when disabled");
    }
  }

  @Nested
  @DisplayName("Bulk Allocation Tests")
  class BulkAllocationTests {

    @Test
    @DisplayName("should allocate bulk with element layout")
    void shouldAllocateBulkWithElementLayout() {
      final MemorySegment segment = manager.allocateBulkOptimized(10, ValueLayout.JAVA_INT);
      assertNotNull(segment, "Bulk segment should not be null");
      final long expectedSize = 10 * ValueLayout.JAVA_INT.byteSize();
      assertEquals(
          expectedSize,
          segment.byteSize(),
          "Bulk segment should be exactly " + expectedSize + " bytes");
      LOGGER.info("Bulk allocated 10 ints: " + segment.byteSize() + " bytes");
    }

    @Test
    @DisplayName("should allocate bulk with long elements")
    void shouldAllocateBulkWithLongElements() {
      final MemorySegment segment = manager.allocateBulkOptimized(100, ValueLayout.JAVA_LONG);
      assertNotNull(segment);
      assertEquals(100 * ValueLayout.JAVA_LONG.byteSize(), segment.byteSize());
      LOGGER.info("Bulk allocated 100 longs: " + segment.byteSize() + " bytes");
    }

    @Test
    @DisplayName("should allocate bulk when disabled")
    void shouldAllocateBulkWhenDisabled() {
      AdvancedArenaManager.setEnabled(false);
      final MemorySegment segment = manager.allocateBulkOptimized(5, ValueLayout.JAVA_INT);
      assertNotNull(segment);
      final long expectedSize = 5 * ValueLayout.JAVA_INT.byteSize();
      assertEquals(expectedSize, segment.byteSize());
    }

    @Test
    @DisplayName("should write and read bulk data")
    void shouldWriteAndReadBulkData() {
      final MemorySegment segment = manager.allocateBulkOptimized(5, ValueLayout.JAVA_INT);

      for (int i = 0; i < 5; i++) {
        segment.set(ValueLayout.JAVA_INT, (long) i * 4, i * 100);
      }

      for (int i = 0; i < 5; i++) {
        final int value = segment.get(ValueLayout.JAVA_INT, (long) i * 4);
        assertEquals(i * 100, value, "Value at index " + i + " should match");
      }
      LOGGER.info("Bulk data write/read verified for 5 ints");
    }
  }

  @Nested
  @DisplayName("Release Tests")
  class ReleaseTests {

    @Test
    @DisplayName("should release allocated segment and decrease memory usage")
    void shouldReleaseAllocatedSegment() {
      final long usageBefore = manager.getCurrentMemoryUsage();
      final MemorySegment segment = manager.allocateOptimized(256);
      final long usageAfterAlloc = manager.getCurrentMemoryUsage();
      assertTrue(
          usageAfterAlloc > usageBefore,
          "Usage should increase after allocation: before="
              + usageBefore
              + " after="
              + usageAfterAlloc);

      manager.releaseOptimized(segment);

      final long usageAfterRelease = manager.getCurrentMemoryUsage();
      assertTrue(
          usageAfterRelease < usageAfterAlloc,
          "Memory usage should decrease after release: afterAlloc="
              + usageAfterAlloc
              + " afterRelease="
              + usageAfterRelease);
      LOGGER.info(
          "Released segment, usage: "
              + usageBefore
              + " -> "
              + usageAfterAlloc
              + " -> "
              + usageAfterRelease);
    }

    @Test
    @DisplayName("should handle null segment release gracefully")
    void shouldHandleNullRelease() {
      assertDoesNotThrow(
          () -> manager.releaseOptimized(null), "Release of null segment should not throw");
      LOGGER.info("Null release handled gracefully");
    }

    @Test
    @DisplayName("should handle release when disabled")
    void shouldHandleReleaseWhenDisabled() {
      AdvancedArenaManager.setEnabled(false);
      final MemorySegment segment = manager.allocateOptimized(64);
      assertDoesNotThrow(
          () -> manager.releaseOptimized(segment), "Release when disabled should not throw");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("should return statistics string when enabled")
    void shouldReturnStatisticsWhenEnabled() {
      final String stats = manager.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertFalse(stats.isEmpty(), "Statistics should not be empty");
      assertTrue(
          stats.contains("Advanced Arena Management Statistics"), "Stats should contain header");
      assertTrue(stats.contains("Memory pressure"), "Stats should contain memory pressure");
      assertTrue(stats.contains("Total allocations"), "Stats should contain total allocations");
      assertTrue(stats.contains("Pool hits"), "Stats should contain pool hits");
      LOGGER.info("Statistics:\n" + stats);
    }

    @Test
    @DisplayName("should return disabled message when disabled")
    void shouldReturnDisabledMessage() {
      AdvancedArenaManager.setEnabled(false);
      final String stats = manager.getStatistics();
      assertEquals(
          "Advanced arena management is disabled", stats, "Should return disabled message");
      LOGGER.info("Disabled stats message: " + stats);
    }

    @Test
    @DisplayName("should include arena pool statistics section")
    void shouldShowArenaPoolStatistics() {
      // Force some allocations to populate stats
      manager.allocateOptimized(512);
      manager.allocateOptimized(4096);

      final String stats = manager.getStatistics();
      assertTrue(
          stats.contains("Arena Pool Statistics"), "Stats should contain arena pool section");
      LOGGER.info("Arena pool stats present");
    }

    @Test
    @DisplayName("should track allocation count increase")
    void shouldTrackAllocationCount() {
      final long initialAllocations = parseTotalAllocations(manager.getStatistics());

      manager.allocateOptimized(128);
      manager.allocateOptimized(256);
      manager.allocateOptimized(512);

      final long afterAllocations = parseTotalAllocations(manager.getStatistics());
      assertTrue(
          afterAllocations >= initialAllocations + 3,
          "Should track at least 3 new allocations: initial="
              + initialAllocations
              + " after="
              + afterAllocations);
      LOGGER.info("Allocation tracking: " + initialAllocations + " -> " + afterAllocations);
    }

    @Test
    @DisplayName("should include current and peak memory in stats")
    void shouldIncludeMemoryStats() {
      manager.allocateOptimized(1024);
      final String stats = manager.getStatistics();
      assertTrue(stats.contains("Current memory"), "Stats should contain current memory");
      assertTrue(stats.contains("Peak memory"), "Stats should contain peak memory");
    }

    @Test
    @DisplayName("should include zero-copy and alignment counts")
    void shouldIncludeZeroCopyAndAlignmentCounts() {
      final String stats = manager.getStatistics();
      assertTrue(stats.contains("Zero-copies"), "Stats should contain zero-copy count");
      assertTrue(stats.contains("Alignment optimizations"), "Stats should contain alignment count");
    }

    private long parseTotalAllocations(final String stats) {
      for (final String line : stats.split("\n")) {
        if (line.contains("Total allocations:")) {
          final String value = line.replaceAll("[^0-9]", "");
          return Long.parseLong(value);
        }
      }
      return 0;
    }
  }

  @Nested
  @DisplayName("Memory Usage Tests")
  class MemoryUsageTests {

    @Test
    @DisplayName("should increase memory usage after allocation")
    void shouldIncreaseMemoryUsageAfterAllocation() {
      final long before = manager.getCurrentMemoryUsage();
      manager.allocateOptimized(1024);
      final long after = manager.getCurrentMemoryUsage();
      assertTrue(
          after > before,
          "Memory usage should increase after allocation: before=" + before + " after=" + after);
      LOGGER.info("Memory usage increased by " + (after - before) + " bytes");
    }

    @Test
    @DisplayName("should decrease memory usage after release")
    void shouldDecreaseMemoryUsageAfterRelease() {
      final MemorySegment segment = manager.allocateOptimized(2048);
      final long afterAlloc = manager.getCurrentMemoryUsage();

      manager.releaseOptimized(segment);
      final long afterRelease = manager.getCurrentMemoryUsage();

      assertTrue(afterRelease < afterAlloc, "Memory usage should decrease after release");
      LOGGER.info("After alloc=" + afterAlloc + " after release=" + afterRelease);
    }
  }

  @Nested
  @DisplayName("Pool Hit Rate Tests")
  class PoolHitRateTests {

    @Test
    @DisplayName("should return valid hit rate between 0 and 100")
    void shouldReturnValidHitRate() {
      final double hitRate = manager.getPoolHitRate();
      assertTrue(hitRate >= 0.0, "Hit rate should be >= 0");
      assertTrue(hitRate <= 100.0, "Hit rate should be <= 100");
      LOGGER.info("Pool hit rate: " + hitRate + "%");
    }

    @Test
    @DisplayName("should include pool hit rate in statistics")
    void shouldHaveHitRateInStatistics() {
      final String stats = manager.getStatistics();
      assertTrue(stats.contains("Pool hit rate"), "Statistics should contain pool hit rate");
      LOGGER.info("Pool hit rate found in statistics");
    }
  }

  @Nested
  @DisplayName("Enum Tests")
  class EnumTests {

    @Test
    @DisplayName("MemoryPressure should have all 4 levels")
    void memoryPressureShouldHaveAllValues() {
      final AdvancedArenaManager.MemoryPressure[] values =
          AdvancedArenaManager.MemoryPressure.values();
      assertEquals(4, values.length, "Should have 4 memory pressure levels");

      assertEquals(
          AdvancedArenaManager.MemoryPressure.LOW,
          AdvancedArenaManager.MemoryPressure.valueOf("LOW"));
      assertEquals(
          AdvancedArenaManager.MemoryPressure.MEDIUM,
          AdvancedArenaManager.MemoryPressure.valueOf("MEDIUM"));
      assertEquals(
          AdvancedArenaManager.MemoryPressure.HIGH,
          AdvancedArenaManager.MemoryPressure.valueOf("HIGH"));
      assertEquals(
          AdvancedArenaManager.MemoryPressure.CRITICAL,
          AdvancedArenaManager.MemoryPressure.valueOf("CRITICAL"));
      LOGGER.info("All MemoryPressure enum values verified");
    }

    @Test
    @DisplayName("AllocationStrategy should have all 4 strategies")
    void allocationStrategyShouldHaveAllValues() {
      final AdvancedArenaManager.AllocationStrategy[] values =
          AdvancedArenaManager.AllocationStrategy.values();
      assertEquals(4, values.length, "Should have 4 allocation strategies");

      assertEquals(
          AdvancedArenaManager.AllocationStrategy.POOL_REUSE,
          AdvancedArenaManager.AllocationStrategy.valueOf("POOL_REUSE"));
      assertEquals(
          AdvancedArenaManager.AllocationStrategy.NEW_OPTIMIZED,
          AdvancedArenaManager.AllocationStrategy.valueOf("NEW_OPTIMIZED"));
      assertEquals(
          AdvancedArenaManager.AllocationStrategy.DIRECT,
          AdvancedArenaManager.AllocationStrategy.valueOf("DIRECT"));
      assertEquals(
          AdvancedArenaManager.AllocationStrategy.ZERO_COPY,
          AdvancedArenaManager.AllocationStrategy.valueOf("ZERO_COPY"));
      LOGGER.info("All AllocationStrategy enum values verified");
    }

    @Test
    @DisplayName("MemoryPressure enum ordinals should match declaration order")
    void memoryPressureOrdinalsShouldMatch() {
      assertEquals(0, AdvancedArenaManager.MemoryPressure.LOW.ordinal());
      assertEquals(1, AdvancedArenaManager.MemoryPressure.MEDIUM.ordinal());
      assertEquals(2, AdvancedArenaManager.MemoryPressure.HIGH.ordinal());
      assertEquals(3, AdvancedArenaManager.MemoryPressure.CRITICAL.ordinal());
    }

    @Test
    @DisplayName("AllocationStrategy enum ordinals should match declaration order")
    void allocationStrategyOrdinalsShouldMatch() {
      assertEquals(0, AdvancedArenaManager.AllocationStrategy.POOL_REUSE.ordinal());
      assertEquals(1, AdvancedArenaManager.AllocationStrategy.NEW_OPTIMIZED.ordinal());
      assertEquals(2, AdvancedArenaManager.AllocationStrategy.DIRECT.ordinal());
      assertEquals(3, AdvancedArenaManager.AllocationStrategy.ZERO_COPY.ordinal());
    }
  }

  @Nested
  @DisplayName("End-to-End Workflow Tests")
  class EndToEndWorkflowTests {

    @Test
    @DisplayName("should support complete arena management workflow")
    void shouldSupportCompleteWorkflow() {
      LOGGER.info("Starting end-to-end arena management workflow");

      // 1. Verify singleton
      final AdvancedArenaManager mgr = AdvancedArenaManager.getInstance();
      assertNotNull(mgr);

      // 2. Enable management
      AdvancedArenaManager.setEnabled(true);
      assertTrue(AdvancedArenaManager.isEnabled());

      // 3. Allocate by size (small - zero-copy strategy)
      final MemorySegment smallSeg = mgr.allocateOptimized(64);
      assertNotNull(smallSeg);
      assertEquals(64, smallSeg.byteSize());

      // 4. Allocate by size (large - optimized strategy)
      final MemorySegment largeSeg = mgr.allocateOptimized(10000);
      assertNotNull(largeSeg);
      assertEquals(10000, largeSeg.byteSize());
      LOGGER.info("Allocated segments by size");

      // 5. Allocate by layout
      final MemorySegment intSeg = mgr.allocateOptimized(ValueLayout.JAVA_INT);
      assertNotNull(intSeg);
      assertEquals(ValueLayout.JAVA_INT.byteSize(), intSeg.byteSize());
      LOGGER.info("Allocated segment by layout");

      // 6. Bulk allocation
      final MemorySegment bulkSeg = mgr.allocateBulkOptimized(20, ValueLayout.JAVA_LONG);
      assertNotNull(bulkSeg);
      assertEquals(20 * ValueLayout.JAVA_LONG.byteSize(), bulkSeg.byteSize());
      LOGGER.info("Bulk allocated 20 longs");

      // 7. Zero-copy operation (reuse source)
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment source = arena.allocate(512);
        source.set(ValueLayout.JAVA_INT, 0, 99);

        final Integer zeroCopyResult =
            mgr.executeZeroCopy(source, 256, segment -> segment.get(ValueLayout.JAVA_INT, 0));
        assertEquals(99, zeroCopyResult, "Zero-copy should read from reused source");
        LOGGER.info("Zero-copy result: " + zeroCopyResult);
      }

      // 8. Release segments
      mgr.releaseOptimized(smallSeg);
      mgr.releaseOptimized(largeSeg);
      LOGGER.info("Released segments");

      // 9. Release null (edge case)
      mgr.releaseOptimized(null);

      // 10. Check statistics
      final String stats = mgr.getStatistics();
      assertNotNull(stats);
      assertFalse(stats.isEmpty());
      assertTrue(stats.contains("Advanced Arena Management Statistics"));
      LOGGER.info("Final statistics:\n" + stats);

      // 11. Check memory usage and hit rate
      final long memUsage = mgr.getCurrentMemoryUsage();
      final double hitRate = mgr.getPoolHitRate();
      assertTrue(hitRate >= 0.0 && hitRate <= 100.0);
      LOGGER.info("Memory usage: " + memUsage + " bytes, hit rate: " + hitRate + "%");

      LOGGER.info("End-to-end workflow completed successfully");
    }
  }
}

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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanamaOptimizationEngine}.
 *
 * <p>These tests invoke actual methods on the singleton optimization engine to exercise code paths
 * and improve JaCoCo coverage. The class uses Panama API (Arena, MemorySegment, MethodHandle) for
 * memory pooling, zero-copy, bulk operations, and layout optimization.
 */
@DisplayName("PanamaOptimizationEngine Integration Tests")
class PanamaOptimizationEngineTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaOptimizationEngineTest.class.getName());

  private PanamaOptimizationEngine engine;

  @BeforeEach
  void setUp() {
    engine = PanamaOptimizationEngine.getInstance();
    PanamaOptimizationEngine.setOptimizationEnabled(true);
    engine.reset();
  }

  @AfterEach
  void tearDown() {
    PanamaOptimizationEngine.setOptimizationEnabled(true);
    engine.reset();
  }

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("should return same instance on multiple calls")
    void shouldReturnSameInstance() {
      final PanamaOptimizationEngine first = PanamaOptimizationEngine.getInstance();
      final PanamaOptimizationEngine second = PanamaOptimizationEngine.getInstance();
      assertSame(first, second, "getInstance should return the same singleton instance");
      LOGGER.info("Singleton instance verified: " + first);
    }

    @Test
    @DisplayName("should return non-null instance")
    void shouldReturnNonNullInstance() {
      assertNotNull(PanamaOptimizationEngine.getInstance(), "getInstance should never return null");
    }
  }

  @Nested
  @DisplayName("Enable/Disable Tests")
  class EnableDisableTests {

    @Test
    @DisplayName("should be enabled by default in test setup")
    void shouldBeEnabledByDefault() {
      assertTrue(
          PanamaOptimizationEngine.isOptimizationEnabled(),
          "Optimization should be enabled (set in @BeforeEach)");
    }

    @Test
    @DisplayName("should disable optimization")
    void shouldDisableOptimization() {
      PanamaOptimizationEngine.setOptimizationEnabled(false);
      assertFalse(
          PanamaOptimizationEngine.isOptimizationEnabled(),
          "Optimization should be disabled after setOptimizationEnabled(false)");
      LOGGER.info(
          "Disabled optimization, isEnabled=" + PanamaOptimizationEngine.isOptimizationEnabled());
    }

    @Test
    @DisplayName("should re-enable optimization")
    void shouldReEnableOptimization() {
      PanamaOptimizationEngine.setOptimizationEnabled(false);
      assertFalse(PanamaOptimizationEngine.isOptimizationEnabled());
      PanamaOptimizationEngine.setOptimizationEnabled(true);
      assertTrue(
          PanamaOptimizationEngine.isOptimizationEnabled(),
          "Optimization should be enabled after setOptimizationEnabled(true)");
    }
  }

  @Nested
  @DisplayName("Allocate Optimized By Size Tests")
  class AllocateOptimizedBySizeTests {

    @Test
    @DisplayName("should allocate small segment matching pool size (64 bytes)")
    void shouldAllocateSmallPoolableSegment() {
      final MemorySegment segment = engine.allocateOptimized(64);
      assertNotNull(segment, "Allocated segment should not be null");
      assertEquals(64, segment.byteSize(), "Segment should have exactly 64 bytes");
      assertTrue(segment.scope().isAlive(), "Segment scope should be alive");
      LOGGER.info("Allocated 64-byte poolable segment: byteSize=" + segment.byteSize());
    }

    @Test
    @DisplayName("should allocate medium segment matching pool size (4096 bytes)")
    void shouldAllocateMediumPoolableSegment() {
      final MemorySegment segment = engine.allocateOptimized(4096);
      assertNotNull(segment, "Allocated segment should not be null");
      assertEquals(4096, segment.byteSize(), "Segment should have exactly 4096 bytes");
      LOGGER.info("Allocated 4096-byte poolable segment");
    }

    @Test
    @DisplayName("should allocate non-poolable segment via thread-local arena")
    void shouldAllocateNonPoolableSegment() {
      // 100 is not a pool size, so falls through to thread-local arena
      final MemorySegment segment = engine.allocateOptimized(100);
      assertNotNull(segment, "Segment should not be null");
      assertEquals(100, segment.byteSize(), "Segment should have exactly 100 bytes");
      LOGGER.info("Allocated 100-byte non-poolable segment via arena");
    }

    @Test
    @DisplayName("should allocate large segment exceeding max pool size")
    void shouldAllocateLargeSegment() {
      final MemorySegment segment = engine.allocateOptimized(100000);
      assertNotNull(segment, "Large segment should not be null");
      assertEquals(100000, segment.byteSize(), "Segment should have exactly 100000 bytes");
      LOGGER.info("Allocated 100KB non-poolable segment");
    }

    @Test
    @DisplayName("should allocate multiple segments")
    void shouldAllocateMultipleSegments() {
      for (int i = 0; i < 10; i++) {
        final MemorySegment segment = engine.allocateOptimized(256);
        assertNotNull(segment, "Segment " + i + " should not be null");
        assertEquals(256, segment.byteSize(), "Segment " + i + " should have 256 bytes");
      }
      LOGGER.info("Allocated 10 segments of 256 bytes each");
    }

    @Test
    @DisplayName("should allocate when optimization is disabled")
    void shouldAllocateWhenDisabled() {
      PanamaOptimizationEngine.setOptimizationEnabled(false);
      final MemorySegment segment = engine.allocateOptimized(128);
      assertNotNull(segment, "Segment should be allocated even when disabled");
      assertEquals(128, segment.byteSize(), "Segment size should match when disabled");
      LOGGER.info("Allocated segment with optimization disabled");
    }
  }

  @Nested
  @DisplayName("Allocate Optimized By Layout Tests")
  class AllocateOptimizedByLayoutTests {

    @Test
    @DisplayName("should allocate with JAVA_INT layout")
    void shouldAllocateWithIntLayout() {
      final MemorySegment segment = engine.allocateOptimized(ValueLayout.JAVA_INT);
      assertNotNull(segment, "Int segment should not be null");
      assertEquals(
          ValueLayout.JAVA_INT.byteSize(),
          segment.byteSize(),
          "Segment should match JAVA_INT size");
      LOGGER.info("Allocated JAVA_INT segment: size=" + segment.byteSize());
    }

    @Test
    @DisplayName("should allocate with JAVA_LONG layout")
    void shouldAllocateWithLongLayout() {
      final MemorySegment segment = engine.allocateOptimized(ValueLayout.JAVA_LONG);
      assertNotNull(segment, "Long segment should not be null");
      assertEquals(
          ValueLayout.JAVA_LONG.byteSize(),
          segment.byteSize(),
          "Segment should match JAVA_LONG size");
    }

    @Test
    @DisplayName("should allocate with struct layout")
    void shouldAllocateWithStructLayout() {
      final MemoryLayout structLayout =
          MemoryLayout.structLayout(
              ValueLayout.JAVA_INT.withName("x"),
              ValueLayout.JAVA_INT.withName("y"),
              ValueLayout.JAVA_LONG.withName("z"));
      final MemorySegment segment = engine.allocateOptimized(structLayout);
      assertNotNull(segment, "Struct segment should not be null");
      assertEquals(
          structLayout.byteSize(), segment.byteSize(), "Segment should match struct layout size");
      LOGGER.info("Allocated struct segment: " + structLayout.byteSize() + " bytes");
    }

    @Test
    @DisplayName("should allocate layout when disabled")
    void shouldAllocateLayoutWhenDisabled() {
      PanamaOptimizationEngine.setOptimizationEnabled(false);
      final MemorySegment segment = engine.allocateOptimized(ValueLayout.JAVA_DOUBLE);
      assertNotNull(segment, "Segment should be allocated even when disabled");
      assertEquals(ValueLayout.JAVA_DOUBLE.byteSize(), segment.byteSize());
    }
  }

  @Nested
  @DisplayName("Zero-Copy Operation Tests")
  class ZeroCopyTests {

    @Test
    @DisplayName("should execute zero-copy operation and return result")
    void shouldExecuteZeroCopyOperation() throws Exception {
      final Integer result =
          engine.executeZeroCopy(
              64,
              segment -> {
                assertNotNull(segment, "Segment provided to zero-copy should not be null");
                assertTrue(segment.byteSize() >= 64, "Segment should be at least 64 bytes");
                segment.set(ValueLayout.JAVA_INT, 0, 42);
                return segment.get(ValueLayout.JAVA_INT, 0);
              });
      assertEquals(42, result, "Zero-copy should return the value written");
      LOGGER.info("Zero-copy operation returned: " + result);
    }

    @Test
    @DisplayName("should execute zero-copy with large segment")
    void shouldExecuteZeroCopyWithLargeSegment() throws Exception {
      final long size = 100000;
      final Long result =
          engine.executeZeroCopy(
              size,
              segment -> {
                assertTrue(segment.byteSize() >= size, "Segment should be at least requested size");
                return segment.byteSize();
              });
      assertTrue(result >= size, "Result should reflect segment size");
      LOGGER.info("Zero-copy large segment size: " + result);
    }

    @Test
    @DisplayName("should execute zero-copy when disabled")
    void shouldExecuteZeroCopyWhenDisabled() throws Exception {
      PanamaOptimizationEngine.setOptimizationEnabled(false);
      final String result =
          engine.executeZeroCopy(
              32,
              segment -> {
                assertNotNull(segment, "Segment should not be null when disabled");
                return "done";
              });
      assertEquals("done", result);
    }
  }

  @Nested
  @DisplayName("Bulk Operation Tests")
  class BulkOperationTests {

    @Test
    @DisplayName("should execute bulk operation with multiple segments")
    void shouldExecuteBulkOperation() throws Exception {
      final Integer count =
          engine.executeBulk(
              5,
              128,
              segments -> {
                assertNotNull(segments, "Segments array should not be null");
                assertEquals(5, segments.length, "Should have 5 segments");
                for (int i = 0; i < segments.length; i++) {
                  assertNotNull(segments[i], "Segment " + i + " should not be null");
                  assertTrue(
                      segments[i].byteSize() >= 128,
                      "Segment " + i + " should be at least 128 bytes");
                }
                return segments.length;
              });
      assertEquals(5, count, "Bulk operation should process 5 segments");
      LOGGER.info("Bulk operation processed " + count + " segments");
    }

    @Test
    @DisplayName("should execute bulk operation with single segment")
    void shouldExecuteBulkWithSingleSegment() throws Exception {
      final Long size =
          engine.executeBulk(
              1,
              256,
              segments -> {
                assertEquals(1, segments.length, "Should have 1 segment");
                return segments[0].byteSize();
              });
      assertTrue(size >= 256, "Single segment should be at least 256 bytes");
    }

    @Test
    @DisplayName("should execute bulk when disabled")
    void shouldExecuteBulkWhenDisabled() throws Exception {
      PanamaOptimizationEngine.setOptimizationEnabled(false);
      final Integer result =
          engine.executeBulk(
              3,
              64,
              segments -> {
                assertEquals(3, segments.length, "Should have 3 segments even when disabled");
                return segments.length;
              });
      assertEquals(3, result);
    }
  }

  @Nested
  @DisplayName("Optimized Copy Tests")
  class OptimizedCopyTests {

    @Test
    @DisplayName("should copy small data between segments (standard path)")
    void shouldCopySmallData() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment source = arena.allocate(64);
        final MemorySegment dest = arena.allocate(64);

        for (int i = 0; i < 16; i++) {
          source.set(ValueLayout.JAVA_INT, (long) i * 4, i * 10);
        }

        engine.optimizedCopy(source, dest, 64);

        for (int i = 0; i < 16; i++) {
          final int expected = i * 10;
          final int actual = dest.get(ValueLayout.JAVA_INT, (long) i * 4);
          assertEquals(expected, actual, "Int at position " + i + " should match after copy");
        }
        LOGGER.info("Small copy (64 bytes, standard path) verified");
      }
    }

    @Test
    @DisplayName("should copy large data between segments (vectorized path)")
    void shouldCopyLargeData() {
      try (Arena arena = Arena.ofConfined()) {
        final int size = 2048;
        final MemorySegment source = arena.allocate(size);
        final MemorySegment dest = arena.allocate(size);

        for (int i = 0; i < size / 8; i++) {
          source.set(ValueLayout.JAVA_LONG, (long) i * 8, (long) i * 100);
        }

        engine.optimizedCopy(source, dest, size);

        for (int i = 0; i < size / 8; i++) {
          final long expected = (long) i * 100;
          final long actual = dest.get(ValueLayout.JAVA_LONG, (long) i * 8);
          assertEquals(expected, actual, "Long at position " + i + " should match after copy");
        }
        LOGGER.info("Large copy (" + size + " bytes, vectorized path) verified");
      }
    }

    @Test
    @DisplayName("should copy large data with non-8-byte-aligned size")
    void shouldCopyNonAlignedLargeSize() {
      try (Arena arena = Arena.ofConfined()) {
        final int size = 2051; // Not 8-byte aligned, exercises remainder path
        final MemorySegment source = arena.allocate(size);
        final MemorySegment dest = arena.allocate(size);

        for (int i = 0; i < size; i++) {
          source.set(ValueLayout.JAVA_BYTE, i, (byte) (i % 127));
        }

        engine.optimizedCopy(source, dest, size);

        for (int i = 0; i < size; i++) {
          final byte expected = (byte) (i % 127);
          final byte actual = dest.get(ValueLayout.JAVA_BYTE, i);
          assertEquals(expected, actual, "Byte at position " + i + " should match");
        }
        LOGGER.info("Non-aligned large copy (" + size + " bytes) verified");
      }
    }

    @Test
    @DisplayName("should copy when optimization is disabled")
    void shouldCopyWhenDisabled() {
      PanamaOptimizationEngine.setOptimizationEnabled(false);
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment source = arena.allocate(32);
        final MemorySegment dest = arena.allocate(32);
        source.set(ValueLayout.JAVA_LONG, 0, 999L);

        engine.optimizedCopy(source, dest, 32);

        assertEquals(
            999L,
            dest.get(ValueLayout.JAVA_LONG, 0),
            "Copy should work when optimization is disabled");
      }
    }
  }

  @Nested
  @DisplayName("Optimize Layout Tests")
  class OptimizeLayoutTests {

    @Test
    @DisplayName("should optimize int layout")
    void shouldOptimizeIntLayout() {
      final ValueLayout layout = engine.optimizeLayout(int.class);
      assertNotNull(layout, "Int layout should not be null");
      assertEquals(4, layout.byteSize(), "Int layout should be 4 bytes");
      LOGGER.info("int layout: " + layout + " alignment=" + layout.byteAlignment());
    }

    @Test
    @DisplayName("should optimize Integer layout")
    void shouldOptimizeIntegerLayout() {
      final ValueLayout layout = engine.optimizeLayout(Integer.class);
      assertNotNull(layout, "Integer layout should not be null");
      assertEquals(4, layout.byteSize(), "Integer layout should be 4 bytes");
    }

    @Test
    @DisplayName("should optimize long layout")
    void shouldOptimizeLongLayout() {
      final ValueLayout layout = engine.optimizeLayout(long.class);
      assertNotNull(layout, "Long layout should not be null");
      assertEquals(8, layout.byteSize(), "Long layout should be 8 bytes");
    }

    @Test
    @DisplayName("should optimize Long layout")
    void shouldOptimizeLongBoxedLayout() {
      final ValueLayout layout = engine.optimizeLayout(Long.class);
      assertEquals(8, layout.byteSize(), "Long layout should be 8 bytes");
    }

    @Test
    @DisplayName("should optimize float layout")
    void shouldOptimizeFloatLayout() {
      final ValueLayout layout = engine.optimizeLayout(float.class);
      assertEquals(4, layout.byteSize(), "Float layout should be 4 bytes");
    }

    @Test
    @DisplayName("should optimize Float layout")
    void shouldOptimizeFloatBoxedLayout() {
      final ValueLayout layout = engine.optimizeLayout(Float.class);
      assertEquals(4, layout.byteSize(), "Float layout should be 4 bytes");
    }

    @Test
    @DisplayName("should optimize double layout")
    void shouldOptimizeDoubleLayout() {
      final ValueLayout layout = engine.optimizeLayout(double.class);
      assertEquals(8, layout.byteSize(), "Double layout should be 8 bytes");
    }

    @Test
    @DisplayName("should optimize Double layout")
    void shouldOptimizeDoubleBoxedLayout() {
      final ValueLayout layout = engine.optimizeLayout(Double.class);
      assertEquals(8, layout.byteSize(), "Double layout should be 8 bytes");
    }

    @Test
    @DisplayName("should optimize byte layout")
    void shouldOptimizeByteLayout() {
      final ValueLayout layout = engine.optimizeLayout(byte.class);
      assertEquals(1, layout.byteSize(), "Byte layout should be 1 byte");
    }

    @Test
    @DisplayName("should optimize Byte layout")
    void shouldOptimizeByteBoxedLayout() {
      final ValueLayout layout = engine.optimizeLayout(Byte.class);
      assertEquals(1, layout.byteSize(), "Byte layout should be 1 byte");
    }

    @Test
    @DisplayName("should optimize short layout")
    void shouldOptimizeShortLayout() {
      final ValueLayout layout = engine.optimizeLayout(short.class);
      assertEquals(2, layout.byteSize(), "Short layout should be 2 bytes");
    }

    @Test
    @DisplayName("should optimize Short layout")
    void shouldOptimizeShortBoxedLayout() {
      final ValueLayout layout = engine.optimizeLayout(Short.class);
      assertEquals(2, layout.byteSize(), "Short layout should be 2 bytes");
    }

    @Test
    @DisplayName("should return ADDRESS layout for unknown type")
    void shouldReturnAddressForUnknownType() {
      final ValueLayout layout = engine.optimizeLayout(String.class);
      assertNotNull(layout, "Unknown type layout should not be null");
      assertEquals(
          ValueLayout.ADDRESS.byteSize(),
          layout.byteSize(),
          "Unknown type should use ADDRESS layout");
      LOGGER.info("Unknown type (String) layout: " + layout);
    }

    @Test
    @DisplayName("should return standard layouts when disabled")
    void shouldReturnStandardLayoutsWhenDisabled() {
      PanamaOptimizationEngine.setOptimizationEnabled(false);
      final ValueLayout intLayout = engine.optimizeLayout(int.class);
      assertEquals(4, intLayout.byteSize(), "Int should still be 4 bytes when disabled");

      final ValueLayout longLayout = engine.optimizeLayout(long.class);
      assertEquals(8, longLayout.byteSize(), "Long should still be 8 bytes when disabled");

      final ValueLayout unknownLayout = engine.optimizeLayout(String.class);
      assertEquals(
          ValueLayout.ADDRESS.byteSize(),
          unknownLayout.byteSize(),
          "Unknown should use ADDRESS when disabled");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("should return non-empty statistics string")
    void shouldReturnStatisticsString() {
      final String stats = engine.getOptimizationStats();
      assertNotNull(stats, "Statistics should not be null");
      assertFalse(stats.isEmpty(), "Statistics should not be empty");
      assertTrue(stats.contains("Panama Optimization Statistics"), "Stats should contain header");
      assertTrue(stats.contains("Optimization enabled"), "Stats should contain enabled status");
      assertTrue(stats.contains("Total operations"), "Stats should contain total operations");
      LOGGER.info("Statistics:\n" + stats);
    }

    @Test
    @DisplayName("should track allocation statistics after allocations")
    void shouldTrackAllocationStatistics() {
      for (int i = 0; i < 5; i++) {
        engine.allocateOptimized(256);
      }

      final String stats = engine.getOptimizationStats();
      assertTrue(stats.contains("Arena allocations"), "Stats should track arena allocations");
      assertTrue(stats.contains("Arena bytes"), "Stats should track arena bytes");
      LOGGER.info("After 5 allocations:\n" + stats);
    }

    @Test
    @DisplayName("should track zero-copy call count")
    void shouldTrackZeroCopyStatistics() throws Exception {
      engine.executeZeroCopy(64, segment -> "result");
      engine.executeZeroCopy(128, segment -> "result");

      final String stats = engine.getOptimizationStats();
      assertTrue(stats.contains("Zero-copy calls"), "Stats should track zero-copy calls");
      LOGGER.info("After zero-copy operations:\n" + stats);
    }

    @Test
    @DisplayName("should include segment pool statistics section")
    void shouldReportSegmentPoolStatistics() {
      final String stats = engine.getOptimizationStats();
      assertTrue(stats.contains("Segment pools"), "Stats should mention segment pools");
      assertTrue(
          stats.contains("Segment pool statistics"),
          "Stats should include pool statistics section");
    }

    @Test
    @DisplayName("should include method handle section")
    void shouldIncludeMethodHandleSection() {
      final String stats = engine.getOptimizationStats();
      assertTrue(
          stats.contains("Top method handles"), "Stats should include method handle section");
    }
  }

  @Nested
  @DisplayName("Reset Tests")
  class ResetTests {

    @Test
    @DisplayName("should reset all statistics to zero")
    void shouldResetAllStatistics() {
      engine.allocateOptimized(256);
      engine.allocateOptimized(512);

      engine.reset();

      final String stats = engine.getOptimizationStats();
      assertTrue(stats.contains("Total operations: 0"), "Total operations should be 0 after reset");
      assertTrue(
          stats.contains("Arena allocations: 0"), "Arena allocations should be 0 after reset");
      assertTrue(stats.contains("Zero-copy calls: 0"), "Zero-copy calls should be 0 after reset");
      LOGGER.info("After reset:\n" + stats);
    }

    @Test
    @DisplayName("should remain functional after reset")
    void shouldStillWorkAfterReset() {
      engine.reset();
      final MemorySegment segment = engine.allocateOptimized(128);
      assertNotNull(segment, "Should allocate after reset");
      assertEquals(128, segment.byteSize(), "Size should be correct after reset");
    }
  }

  @Nested
  @DisplayName("Method Handle Tests")
  class MethodHandleTests {

    @Test
    @DisplayName("should throw for non-existent function when enabled")
    void shouldThrowForMissingFunctionWhenEnabled() {
      final SymbolLookup lookup = Linker.nativeLinker().defaultLookup();
      final FunctionDescriptor desc =
          FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.getOptimizedMethodHandle(lookup, "nonexistent_function_xyz123", desc),
          "Should throw for missing function");
      LOGGER.info("Correctly threw IllegalArgumentException for missing function");
    }

    @Test
    @DisplayName("should throw for non-existent function when disabled")
    void shouldThrowForMissingFunctionWhenDisabled() {
      PanamaOptimizationEngine.setOptimizationEnabled(false);
      final SymbolLookup lookup = Linker.nativeLinker().defaultLookup();
      final FunctionDescriptor desc =
          FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.getOptimizedMethodHandle(lookup, "nonexistent_function_xyz123", desc),
          "Should throw for missing function when disabled");
    }
  }

  @Nested
  @DisplayName("Shutdown Tests")
  class ShutdownTests {

    @Test
    @DisplayName("should shutdown and remain usable after reset")
    void shouldShutdownWithoutError() {
      engine.shutdown();
      LOGGER.info("Shutdown completed");

      // Engine should be usable after reset
      engine.reset();
      final MemorySegment segment = engine.allocateOptimized(64);
      assertNotNull(segment, "Engine should be usable after shutdown+reset");
    }
  }

  @Nested
  @DisplayName("Functional Interface Tests")
  class FunctionalInterfaceTests {

    @Test
    @DisplayName("ZeroCopyOperation should accept lambda")
    void zeroCopyOperationShouldAcceptLambda() {
      final PanamaOptimizationEngine.ZeroCopyOperation<String> op = segment -> "hello";
      assertNotNull(op, "Lambda should create ZeroCopyOperation");
      assertTrue(
          PanamaOptimizationEngine.ZeroCopyOperation.class.isInterface(),
          "ZeroCopyOperation should be an interface");
    }

    @Test
    @DisplayName("BulkOperation should accept lambda")
    void bulkOperationShouldAcceptLambda() {
      final PanamaOptimizationEngine.BulkOperation<Integer> op = segments -> segments.length;
      assertNotNull(op, "Lambda should create BulkOperation");
      assertTrue(
          PanamaOptimizationEngine.BulkOperation.class.isInterface(),
          "BulkOperation should be an interface");
    }

    @Test
    @DisplayName("ZeroCopyOperation lambda should execute correctly")
    void zeroCopyOperationLambdaShouldExecute() throws Exception {
      final PanamaOptimizationEngine.ZeroCopyOperation<Long> op = MemorySegment::byteSize;
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(128);
        final Long result = op.execute(segment);
        assertEquals(128L, result, "Lambda should return segment size");
      }
    }

    @Test
    @DisplayName("BulkOperation lambda should execute correctly")
    void bulkOperationLambdaShouldExecute() throws Exception {
      final PanamaOptimizationEngine.BulkOperation<Integer> op = segments -> segments.length;
      final MemorySegment[] segments = new MemorySegment[3];
      final Integer count = op.execute(segments);
      assertEquals(3, count, "Lambda should return array length");
    }
  }

  @Nested
  @DisplayName("End-to-End Workflow Tests")
  class EndToEndWorkflowTests {

    @Test
    @DisplayName("should support complete optimization workflow")
    void shouldSupportCompleteWorkflow() throws Exception {
      LOGGER.info("Starting end-to-end optimization workflow");

      // 1. Verify singleton
      final PanamaOptimizationEngine eng = PanamaOptimizationEngine.getInstance();
      assertNotNull(eng);

      // 2. Enable optimization
      PanamaOptimizationEngine.setOptimizationEnabled(true);
      assertTrue(PanamaOptimizationEngine.isOptimizationEnabled());

      // 3. Allocate by size
      final MemorySegment small = eng.allocateOptimized(64);
      assertNotNull(small);
      assertEquals(64, small.byteSize());
      final MemorySegment layoutSeg = eng.allocateOptimized(ValueLayout.JAVA_LONG);
      assertNotNull(layoutSeg);
      LOGGER.info("Allocated small and layout segments");

      // 4. Zero-copy operation
      final int zeroCopyResult =
          eng.executeZeroCopy(
              128,
              segment -> {
                segment.set(ValueLayout.JAVA_INT, 0, 77);
                return segment.get(ValueLayout.JAVA_INT, 0);
              });
      assertEquals(77, zeroCopyResult);
      LOGGER.info("Zero-copy result: " + zeroCopyResult);

      // 5. Bulk operation
      final int bulkResult =
          eng.executeBulk(
              3,
              256,
              segments -> {
                int sum = 0;
                for (int i = 0; i < segments.length; i++) {
                  segments[i].set(ValueLayout.JAVA_INT, 0, i + 1);
                  sum += segments[i].get(ValueLayout.JAVA_INT, 0);
                }
                return sum;
              });
      assertEquals(6, bulkResult, "1+2+3 = 6");
      LOGGER.info("Bulk operation sum: " + bulkResult);

      // 6. Optimized copy
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment src = arena.allocate(256);
        final MemorySegment dst = arena.allocate(256);
        src.set(ValueLayout.JAVA_LONG, 0, 12345L);
        eng.optimizedCopy(src, dst, 256);
        assertEquals(12345L, dst.get(ValueLayout.JAVA_LONG, 0));
        LOGGER.info("Optimized copy verified");
      }

      // 7. Layout optimization
      final ValueLayout intLayout = eng.optimizeLayout(int.class);
      assertEquals(4, intLayout.byteSize());

      // 8. Statistics
      final String stats = eng.getOptimizationStats();
      assertNotNull(stats);
      assertFalse(stats.isEmpty());
      LOGGER.info("Final statistics:\n" + stats);

      // 9. Reset
      eng.reset();
      final String afterReset = eng.getOptimizationStats();
      assertTrue(afterReset.contains("Total operations: 0"));
      LOGGER.info("End-to-end workflow completed successfully");
    }
  }
}

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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaMemoryManager} class.
 *
 * <p>This test class verifies the memory management functionality for Panama FFI operations.
 */
@DisplayName("PanamaMemoryManager Tests")
class PanamaMemoryManagerTest {

  private Arena arena;
  private PanamaMemoryManager memoryManager;

  @BeforeEach
  void setUp() {
    arena = Arena.ofConfined();
    memoryManager = new PanamaMemoryManager(arena);
  }

  @AfterEach
  void tearDown() {
    if (arena.scope().isAlive()) {
      arena.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaMemoryManager should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaMemoryManager.class.getModifiers()),
          "PanamaMemoryManager should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid arena")
    void constructorShouldAcceptValidArena() {
      final PanamaMemoryManager manager = new PanamaMemoryManager(arena);
      assertNotNull(manager, "Memory manager should be created");
      assertEquals(arena, manager.getArena(), "Arena should match");
    }

    @Test
    @DisplayName("Constructor should throw for null arena")
    void constructorShouldThrowForNullArena() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaMemoryManager(null),
          "Should throw for null arena");
    }
  }

  @Nested
  @DisplayName("allocate(size) Tests")
  class AllocateSizeTests {

    @Test
    @DisplayName("allocate should allocate memory of specified size")
    void allocateShouldAllocateMemoryOfSpecifiedSize() {
      final MemorySegment segment = memoryManager.allocate(128);

      assertNotNull(segment, "Segment should not be null");
      assertEquals(128, segment.byteSize(), "Segment should have correct size");
    }

    @Test
    @DisplayName("allocate should throw for negative size")
    void allocateShouldThrowForNegativeSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.allocate(-1),
          "Should throw for negative size");
    }

    @Test
    @DisplayName("allocate should return NULL for zero size")
    void allocateShouldReturnNullForZeroSize() {
      final MemorySegment segment = memoryManager.allocate(0);
      assertEquals(MemorySegment.NULL, segment, "Should return NULL for zero size");
    }

    @Test
    @DisplayName("allocate should track allocation")
    void allocateShouldTrackAllocation() {
      memoryManager.allocate(64);

      assertEquals(64, memoryManager.getTotalAllocated(), "Should track allocation size");
      assertEquals(1, memoryManager.getAllocationCount(), "Should track allocation count");
    }
  }

  @Nested
  @DisplayName("allocate(layout) Tests")
  class AllocateLayoutTests {

    @Test
    @DisplayName("allocate with layout should allocate memory")
    void allocateWithLayoutShouldAllocateMemory() {
      final MemorySegment segment = memoryManager.allocate(ValueLayout.JAVA_LONG);

      assertNotNull(segment, "Segment should not be null");
      assertEquals(ValueLayout.JAVA_LONG.byteSize(), segment.byteSize(), "Size should match layout");
    }

    @Test
    @DisplayName("allocate with layout should throw for null layout")
    void allocateWithLayoutShouldThrowForNullLayout() {
      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.allocate((ValueLayout) null),
          "Should throw for null layout");
    }
  }

  @Nested
  @DisplayName("allocateBytes Tests")
  class AllocateBytesTests {

    @Test
    @DisplayName("allocateBytes should allocate and copy data")
    void allocateBytesShouldAllocateAndCopyData() {
      final byte[] data = {1, 2, 3, 4, 5};

      final MemorySegment segment = memoryManager.allocateBytes(data);

      assertNotNull(segment, "Segment should not be null");
      final byte[] result = segment.toArray(ValueLayout.JAVA_BYTE);
      assertArrayEquals(data, result, "Data should match");
    }

    @Test
    @DisplayName("allocateBytes should throw for null data")
    void allocateBytesShouldThrowForNullData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.allocateBytes(null),
          "Should throw for null data");
    }

    @Test
    @DisplayName("allocateBytes should return NULL for empty array")
    void allocateBytesShouldReturnNullForEmptyArray() {
      final MemorySegment segment = memoryManager.allocateBytes(new byte[0]);
      assertEquals(MemorySegment.NULL, segment, "Should return NULL for empty array");
    }
  }

  @Nested
  @DisplayName("allocateString Tests")
  class AllocateStringTests {

    @Test
    @DisplayName("allocateString should allocate null-terminated string")
    void allocateStringShouldAllocateNullTerminatedString() {
      final MemorySegment segment = memoryManager.allocateString("hello");

      assertNotNull(segment, "Segment should not be null");
      assertEquals("hello", segment.getString(0), "String should match");
    }

    @Test
    @DisplayName("allocateString should throw for null string")
    void allocateStringShouldThrowForNullString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.allocateString(null),
          "Should throw for null string");
    }

    @Test
    @DisplayName("allocateString should handle empty string")
    void allocateStringShouldHandleEmptyString() {
      final MemorySegment segment = memoryManager.allocateString("");

      assertNotNull(segment, "Segment should not be null");
      assertEquals("", segment.getString(0), "Should handle empty string");
    }
  }

  @Nested
  @DisplayName("allocateArray Tests")
  class AllocateArrayTests {

    @Test
    @DisplayName("allocateArray should allocate array of segments")
    void allocateArrayShouldAllocateArrayOfSegments() {
      final MemorySegment[] segments = memoryManager.allocateArray(5, 32);

      assertEquals(5, segments.length, "Should allocate 5 segments");
      for (MemorySegment segment : segments) {
        assertNotNull(segment, "Each segment should not be null");
        assertEquals(32, segment.byteSize(), "Each segment should have correct size");
      }
    }

    @Test
    @DisplayName("allocateArray should throw for negative count")
    void allocateArrayShouldThrowForNegativeCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.allocateArray(-1, 32),
          "Should throw for negative count");
    }

    @Test
    @DisplayName("allocateArray should throw for negative segment size")
    void allocateArrayShouldThrowForNegativeSegmentSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.allocateArray(5, -1),
          "Should throw for negative segment size");
    }

    @Test
    @DisplayName("allocateArray should return empty array for zero count")
    void allocateArrayShouldReturnEmptyArrayForZeroCount() {
      final MemorySegment[] segments = memoryManager.allocateArray(0, 32);
      assertEquals(0, segments.length, "Should return empty array");
    }
  }

  @Nested
  @DisplayName("allocateIntArray Tests")
  class AllocateIntArrayTests {

    @Test
    @DisplayName("allocateIntArray should allocate and copy int data")
    void allocateIntArrayShouldAllocateAndCopyIntData() {
      final int[] data = {10, 20, 30, 40, 50};

      final MemorySegment segment = memoryManager.allocateIntArray(data);

      assertNotNull(segment, "Segment should not be null");
      for (int i = 0; i < data.length; i++) {
        assertEquals(data[i], segment.getAtIndex(ValueLayout.JAVA_INT, i), "Int at " + i + " should match");
      }
    }

    @Test
    @DisplayName("allocateIntArray should throw for null data")
    void allocateIntArrayShouldThrowForNullData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.allocateIntArray(null),
          "Should throw for null data");
    }
  }

  @Nested
  @DisplayName("allocatePointerArray Tests")
  class AllocatePointerArrayTests {

    @Test
    @DisplayName("allocatePointerArray should allocate pointer array")
    void allocatePointerArrayShouldAllocatePointerArray() {
      final MemorySegment segment = memoryManager.allocatePointerArray(10);

      assertNotNull(segment, "Segment should not be null");
      assertEquals(
          ValueLayout.ADDRESS.byteSize() * 10, segment.byteSize(), "Size should match pointer count");
    }

    @Test
    @DisplayName("allocatePointerArray should throw for negative count")
    void allocatePointerArrayShouldThrowForNegativeCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.allocatePointerArray(-1),
          "Should throw for negative count");
    }
  }

  @Nested
  @DisplayName("allocateSize Tests")
  class AllocateSizeMethodTests {

    @Test
    @DisplayName("allocateSize should allocate size_t segment")
    void allocateSizeShouldAllocateSizeTSegment() {
      final MemorySegment segment = memoryManager.allocateSize();

      assertNotNull(segment, "Segment should not be null");
      assertEquals(ValueLayout.JAVA_LONG.byteSize(), segment.byteSize(), "Size should be long size");
    }
  }

  @Nested
  @DisplayName("ofAddress Tests")
  class OfAddressTests {

    @Test
    @DisplayName("ofAddress should create segment reference")
    void ofAddressShouldCreateSegmentReference() {
      // First allocate a segment to get a valid address
      final MemorySegment original = memoryManager.allocate(64);
      final long address = original.address();

      final MemorySegment referenced = memoryManager.ofAddress(address, 64);

      assertNotNull(referenced, "Referenced segment should not be null");
      assertEquals(64, referenced.byteSize(), "Referenced segment should have correct size");
    }

    @Test
    @DisplayName("ofAddress should throw for zero address")
    void ofAddressShouldThrowForZeroAddress() {
      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.ofAddress(0, 64),
          "Should throw for zero address");
    }

    @Test
    @DisplayName("ofAddress should throw for negative size")
    void ofAddressShouldThrowForNegativeSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.ofAddress(1000, -1),
          "Should throw for negative size");
    }
  }

  @Nested
  @DisplayName("copy Tests")
  class CopyTests {

    @Test
    @DisplayName("copy should copy data between segments")
    void copyShouldCopyDataBetweenSegments() {
      final MemorySegment source = memoryManager.allocateBytes(new byte[] {1, 2, 3, 4, 5});
      final MemorySegment destination = memoryManager.allocate(5);

      memoryManager.copy(source, destination, 5);

      final byte[] result = destination.toArray(ValueLayout.JAVA_BYTE);
      assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, result, "Data should be copied");
    }

    @Test
    @DisplayName("copy should throw for null source")
    void copyShouldThrowForNullSource() {
      final MemorySegment destination = memoryManager.allocate(5);

      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.copy(null, destination, 5),
          "Should throw for null source");
    }

    @Test
    @DisplayName("copy should throw for null destination")
    void copyShouldThrowForNullDestination() {
      final MemorySegment source = memoryManager.allocate(5);

      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.copy(source, null, 5),
          "Should throw for null destination");
    }

    @Test
    @DisplayName("copy should throw for negative size")
    void copyShouldThrowForNegativeSize() {
      final MemorySegment source = memoryManager.allocate(5);
      final MemorySegment destination = memoryManager.allocate(5);

      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.copy(source, destination, -1),
          "Should throw for negative size");
    }

    @Test
    @DisplayName("copy should handle zero size")
    void copyShouldHandleZeroSize() {
      final MemorySegment source = memoryManager.allocate(5);
      final MemorySegment destination = memoryManager.allocate(5);

      assertDoesNotThrow(
          () -> memoryManager.copy(source, destination, 0), "Should handle zero size");
    }
  }

  @Nested
  @DisplayName("fill Tests")
  class FillTests {

    @Test
    @DisplayName("fill should fill segment with value")
    void fillShouldFillSegmentWithValue() {
      final MemorySegment segment = memoryManager.allocate(5);

      memoryManager.fill(segment, (byte) 0xFF);

      final byte[] result = segment.toArray(ValueLayout.JAVA_BYTE);
      for (byte b : result) {
        assertEquals((byte) 0xFF, b, "All bytes should be 0xFF");
      }
    }

    @Test
    @DisplayName("fill should throw for null segment")
    void fillShouldThrowForNullSegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> memoryManager.fill(null, (byte) 0),
          "Should throw for null segment");
    }

    @Test
    @DisplayName("fill should handle NULL segment")
    void fillShouldHandleNullSegment() {
      assertDoesNotThrow(
          () -> memoryManager.fill(MemorySegment.NULL, (byte) 0), "Should handle NULL segment");
    }
  }

  @Nested
  @DisplayName("freeMemory Tests")
  class FreeMemoryTests {

    @Test
    @DisplayName("freeMemory should not throw for null segment")
    void freeMemoryShouldNotThrowForNullSegment() {
      assertDoesNotThrow(() -> memoryManager.freeMemory(null), "Should not throw for null");
    }

    @Test
    @DisplayName("freeMemory should not throw for NULL segment")
    void freeMemoryShouldNotThrowForNullMemorySegment() {
      assertDoesNotThrow(
          () -> memoryManager.freeMemory(MemorySegment.NULL), "Should not throw for NULL");
    }

    @Test
    @DisplayName("freeMemory should handle arena-allocated segment")
    void freeMemoryShouldHandleArenaAllocatedSegment() {
      final MemorySegment segment = memoryManager.allocate(64);

      assertDoesNotThrow(
          () -> memoryManager.freeMemory(segment), "Should handle arena-allocated segment");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getTotalAllocated should track allocations")
    void getTotalAllocatedShouldTrackAllocations() {
      memoryManager.allocate(100);
      memoryManager.allocate(50);

      assertEquals(150, memoryManager.getTotalAllocated(), "Should track total allocated");
    }

    @Test
    @DisplayName("getAllocationCount should track allocation count")
    void getAllocationCountShouldTrackAllocationCount() {
      memoryManager.allocate(100);
      memoryManager.allocate(50);
      memoryManager.allocate(25);

      assertEquals(3, memoryManager.getAllocationCount(), "Should count allocations");
    }

    @Test
    @DisplayName("getMemoryStats should return formatted string")
    void getMemoryStatsShouldReturnFormattedString() {
      memoryManager.allocate(100);

      final String stats = memoryManager.getMemoryStats();

      assertNotNull(stats, "Stats should not be null");
      assertFalse(stats.isEmpty(), "Stats should not be empty");
      assertTrue(stats.contains("PanamaMemoryManager"), "Should contain class name");
      assertTrue(stats.contains("1"), "Should contain allocation count");
      assertTrue(stats.contains("100"), "Should contain allocated size");
    }
  }

  @Nested
  @DisplayName("getArena Tests")
  class GetArenaTests {

    @Test
    @DisplayName("getArena should return the arena")
    void getArenaShouldReturnTheArena() {
      assertEquals(arena, memoryManager.getArena(), "Should return the arena");
    }
  }
}

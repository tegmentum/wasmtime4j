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

package ai.tegmentum.wasmtime4j.panama.ffi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link MemorySegmentManager} class.
 *
 * <p>This test class verifies the memory segment management functionality for Panama FFI context.
 */
@DisplayName("MemorySegmentManager Tests")
class MemorySegmentManagerTest {

  private Arena arena;
  private MemorySegmentManager manager;

  @BeforeEach
  void setUp() {
    arena = Arena.ofConfined();
    manager = new MemorySegmentManager(arena);
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
    @DisplayName("MemorySegmentManager should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(MemorySegmentManager.class.getModifiers()),
          "MemorySegmentManager should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid arena")
    void constructorShouldAcceptValidArena() {
      try (Arena testArena = Arena.ofConfined()) {
        final MemorySegmentManager testManager = new MemorySegmentManager(testArena);

        assertNotNull(testManager, "Manager should be created");
        assertSame(testArena, testManager.getArena(), "Should store the arena");
      }
    }

    @Test
    @DisplayName("Constructor should throw for null arena")
    void constructorShouldThrowForNullArena() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new MemorySegmentManager(null),
          "Should throw for null arena");
    }
  }

  @Nested
  @DisplayName("fromByteArray Tests")
  class FromByteArrayTests {

    @Test
    @DisplayName("fromByteArray should create segment from byte array")
    void fromByteArrayShouldCreateSegmentFromByteArray() {
      final byte[] data = {1, 2, 3, 4, 5};

      final MemorySegment segment = manager.fromByteArray(data);

      assertNotNull(segment, "Segment should be created");
      assertEquals(5, segment.byteSize(), "Segment should have correct size");
    }

    @Test
    @DisplayName("fromByteArray should copy data correctly")
    void fromByteArrayShouldCopyDataCorrectly() {
      final byte[] data = {10, 20, 30, 40, 50};

      final MemorySegment segment = manager.fromByteArray(data);
      final byte[] result = manager.toByteArray(segment);

      assertArrayEquals(data, result, "Data should be copied correctly");
    }

    @Test
    @DisplayName("fromByteArray should throw for null data")
    void fromByteArrayShouldThrowForNullData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.fromByteArray(null),
          "Should throw for null data");
    }

    @Test
    @DisplayName("fromByteArray should return NULL segment for empty array")
    void fromByteArrayShouldReturnNullSegmentForEmptyArray() {
      final byte[] data = new byte[0];

      final MemorySegment segment = manager.fromByteArray(data);

      assertEquals(MemorySegment.NULL, segment, "Should return NULL segment for empty array");
    }

    @Test
    @DisplayName("fromByteArray should handle large arrays")
    void fromByteArrayShouldHandleLargeArrays() {
      final byte[] data = new byte[1024 * 1024]; // 1 MB
      for (int i = 0; i < data.length; i++) {
        data[i] = (byte) (i % 256);
      }

      final MemorySegment segment = manager.fromByteArray(data);

      assertEquals(data.length, segment.byteSize(), "Segment should have correct size");
    }
  }

  @Nested
  @DisplayName("fromByteBuffer Tests")
  class FromByteBufferTests {

    @Test
    @DisplayName("fromByteBuffer should create segment from heap buffer")
    void fromByteBufferShouldCreateSegmentFromHeapBuffer() {
      final ByteBuffer buffer = ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5});

      final MemorySegment segment = manager.fromByteBuffer(buffer);

      assertNotNull(segment, "Segment should be created");
      assertEquals(5, segment.byteSize(), "Segment should have correct size");
    }

    @Test
    @DisplayName("fromByteBuffer should copy data correctly")
    void fromByteBufferShouldCopyDataCorrectly() {
      final byte[] data = {10, 20, 30, 40, 50};
      final ByteBuffer buffer = ByteBuffer.wrap(data);

      final MemorySegment segment = manager.fromByteBuffer(buffer);
      final byte[] result = manager.toByteArray(segment);

      assertArrayEquals(data, result, "Data should be copied correctly");
    }

    @Test
    @DisplayName("fromByteBuffer should preserve buffer position")
    void fromByteBufferShouldPreserveBufferPosition() {
      final ByteBuffer buffer = ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5});
      buffer.position(2); // Set position

      final int originalPosition = buffer.position();
      manager.fromByteBuffer(buffer);

      assertEquals(originalPosition, buffer.position(), "Buffer position should be preserved");
    }

    @Test
    @DisplayName("fromByteBuffer should throw for null buffer")
    void fromByteBufferShouldThrowForNullBuffer() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.fromByteBuffer(null),
          "Should throw for null buffer");
    }

    @Test
    @DisplayName("fromByteBuffer should return NULL segment for empty buffer")
    void fromByteBufferShouldReturnNullSegmentForEmptyBuffer() {
      final ByteBuffer buffer = ByteBuffer.allocate(0);

      final MemorySegment segment = manager.fromByteBuffer(buffer);

      assertEquals(MemorySegment.NULL, segment, "Should return NULL segment for empty buffer");
    }

    @Test
    @DisplayName("fromByteBuffer should handle direct buffer")
    void fromByteBufferShouldHandleDirectBuffer() {
      final ByteBuffer buffer = ByteBuffer.allocateDirect(10);
      buffer.put(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
      buffer.flip();

      final MemorySegment segment = manager.fromByteBuffer(buffer);

      assertNotNull(segment, "Segment should be created");
      assertEquals(10, segment.byteSize(), "Segment should have correct size");
    }
  }

  @Nested
  @DisplayName("toByteArray Tests")
  class ToByteArrayTests {

    @Test
    @DisplayName("toByteArray should convert segment to byte array")
    void toByteArrayShouldConvertSegmentToByteArray() {
      final byte[] original = {1, 2, 3, 4, 5};
      final MemorySegment segment = manager.fromByteArray(original);

      final byte[] result = manager.toByteArray(segment);

      assertArrayEquals(original, result, "Should convert correctly");
    }

    @Test
    @DisplayName("toByteArray should throw for null segment")
    void toByteArrayShouldThrowForNullSegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.toByteArray(null),
          "Should throw for null segment");
    }

    @Test
    @DisplayName("toByteArray should return empty array for NULL segment")
    void toByteArrayShouldReturnEmptyArrayForNullSegment() {
      final byte[] result = manager.toByteArray(MemorySegment.NULL);

      assertEquals(0, result.length, "Should return empty array for NULL segment");
    }

    @Test
    @DisplayName("toByteArray should handle large segments")
    void toByteArrayShouldHandleLargeSegments() {
      final byte[] original = new byte[1024 * 1024]; // 1 MB
      for (int i = 0; i < original.length; i++) {
        original[i] = (byte) (i % 256);
      }
      final MemorySegment segment = manager.fromByteArray(original);

      final byte[] result = manager.toByteArray(segment);

      assertArrayEquals(original, result, "Should handle large segments correctly");
    }
  }

  @Nested
  @DisplayName("asByteBuffer Tests")
  class AsByteBufferTests {

    @Test
    @DisplayName("asByteBuffer should create ByteBuffer view")
    void asByteBufferShouldCreateByteBufferView() {
      final byte[] data = {1, 2, 3, 4, 5};
      final MemorySegment segment = manager.fromByteArray(data);

      final ByteBuffer buffer = manager.asByteBuffer(segment);

      assertNotNull(buffer, "Buffer should be created");
      assertEquals(5, buffer.remaining(), "Buffer should have correct size");
    }

    @Test
    @DisplayName("asByteBuffer should contain correct data")
    void asByteBufferShouldContainCorrectData() {
      final byte[] data = {10, 20, 30, 40, 50};
      final MemorySegment segment = manager.fromByteArray(data);

      final ByteBuffer buffer = manager.asByteBuffer(segment);
      final byte[] result = new byte[buffer.remaining()];
      buffer.get(result);

      assertArrayEquals(data, result, "Buffer should contain correct data");
    }

    @Test
    @DisplayName("asByteBuffer should throw for null segment")
    void asByteBufferShouldThrowForNullSegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.asByteBuffer(null),
          "Should throw for null segment");
    }

    @Test
    @DisplayName("asByteBuffer should return empty buffer for NULL segment")
    void asByteBufferShouldReturnEmptyBufferForNullSegment() {
      final ByteBuffer buffer = manager.asByteBuffer(MemorySegment.NULL);

      assertEquals(0, buffer.remaining(), "Should return empty buffer for NULL segment");
    }
  }

  @Nested
  @DisplayName("allocate Tests")
  class AllocateTests {

    @Test
    @DisplayName("allocate should create segment of specified size")
    void allocateShouldCreateSegmentOfSpecifiedSize() {
      final MemorySegment segment = manager.allocate(100);

      assertNotNull(segment, "Segment should be created");
      assertEquals(100, segment.byteSize(), "Segment should have correct size");
    }

    @Test
    @DisplayName("allocate should throw for negative size")
    void allocateShouldThrowForNegativeSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.allocate(-1),
          "Should throw for negative size");
    }

    @Test
    @DisplayName("allocate should return NULL segment for zero size")
    void allocateShouldReturnNullSegmentForZeroSize() {
      final MemorySegment segment = manager.allocate(0);

      assertEquals(MemorySegment.NULL, segment, "Should return NULL segment for zero size");
    }

    @Test
    @DisplayName("allocate should handle large sizes")
    void allocateShouldHandleLargeSizes() {
      final MemorySegment segment = manager.allocate(1024 * 1024); // 1 MB

      assertEquals(1024 * 1024, segment.byteSize(), "Should allocate large segment");
    }
  }

  @Nested
  @DisplayName("allocateAndFill Tests")
  class AllocateAndFillTests {

    @Test
    @DisplayName("allocateAndFill should create and fill segment")
    void allocateAndFillShouldCreateAndFillSegment() {
      final MemorySegment segment = manager.allocateAndFill(10, (byte) 0x42);

      assertNotNull(segment, "Segment should be created");
      assertEquals(10, segment.byteSize(), "Segment should have correct size");

      final byte[] result = manager.toByteArray(segment);
      for (byte b : result) {
        assertEquals(0x42, b, "All bytes should be filled with specified value");
      }
    }

    @Test
    @DisplayName("allocateAndFill should throw for negative size")
    void allocateAndFillShouldThrowForNegativeSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.allocateAndFill(-1, (byte) 0),
          "Should throw for negative size");
    }

    @Test
    @DisplayName("allocateAndFill should return NULL segment for zero size")
    void allocateAndFillShouldReturnNullSegmentForZeroSize() {
      final MemorySegment segment = manager.allocateAndFill(0, (byte) 0x42);

      assertEquals(MemorySegment.NULL, segment, "Should return NULL segment for zero size");
    }

    @Test
    @DisplayName("allocateAndFill should handle zero fill value")
    void allocateAndFillShouldHandleZeroFillValue() {
      final MemorySegment segment = manager.allocateAndFill(5, (byte) 0);

      final byte[] result = manager.toByteArray(segment);
      for (byte b : result) {
        assertEquals(0, b, "All bytes should be zero");
      }
    }
  }

  @Nested
  @DisplayName("fromString Tests")
  class FromStringTests {

    @Test
    @DisplayName("fromString should create null-terminated segment")
    void fromStringShouldCreateNullTerminatedSegment() {
      final MemorySegment segment = manager.fromString("Hello");

      assertNotNull(segment, "Segment should be created");
      // String + null terminator
      assertTrue(segment.byteSize() >= 5, "Segment should have correct size");
    }

    @Test
    @DisplayName("fromString should throw for null string")
    void fromStringShouldThrowForNullString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.fromString(null),
          "Should throw for null string");
    }

    @Test
    @DisplayName("fromString should handle empty string")
    void fromStringShouldHandleEmptyString() {
      final MemorySegment segment = manager.fromString("");

      assertNotNull(segment, "Segment should be created");
      // Should at least have null terminator
      assertTrue(segment.byteSize() >= 1, "Should have at least null terminator");
    }

    @Test
    @DisplayName("fromString should handle special characters")
    void fromStringShouldHandleSpecialCharacters() {
      final MemorySegment segment = manager.fromString("Hello\nWorld\t!");

      assertNotNull(segment, "Segment should be created");
    }

    @Test
    @DisplayName("fromString should handle unicode characters")
    void fromStringShouldHandleUnicodeCharacters() {
      final MemorySegment segment = manager.fromString("Hello 世界");

      assertNotNull(segment, "Segment should be created");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringMethodTests {

    @Test
    @DisplayName("toString should read string from segment")
    void toStringShouldReadStringFromSegment() {
      final MemorySegment segment = manager.fromString("Hello World");

      final String result = manager.toString(segment);

      assertEquals("Hello World", result, "Should read string correctly");
    }

    @Test
    @DisplayName("toString should throw for null segment")
    void toStringShouldThrowForNullSegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.toString(null),
          "Should throw for null segment");
    }

    @Test
    @DisplayName("toString should return empty string for NULL segment")
    void toStringShouldReturnEmptyStringForNullSegment() {
      final String result = manager.toString(MemorySegment.NULL);

      assertEquals("", result, "Should return empty string for NULL segment");
    }

    @Test
    @DisplayName("toString should handle empty string")
    void toStringShouldHandleEmptyString() {
      final MemorySegment segment = manager.fromString("");

      final String result = manager.toString(segment);

      assertEquals("", result, "Should handle empty string");
    }
  }

  @Nested
  @DisplayName("getArena Tests")
  class GetArenaTests {

    @Test
    @DisplayName("getArena should return the arena")
    void getArenaShouldReturnTheArena() {
      assertSame(arena, manager.getArena(), "Should return the same arena");
    }
  }

  @Nested
  @DisplayName("Round-trip Tests")
  class RoundTripTests {

    @Test
    @DisplayName("Byte array round-trip should preserve data")
    void byteArrayRoundTripShouldPreserveData() {
      final byte[] original = {0, 1, 2, 127, -128, -1};

      final MemorySegment segment = manager.fromByteArray(original);
      final byte[] result = manager.toByteArray(segment);

      assertArrayEquals(original, result, "Data should be preserved in round-trip");
    }

    @Test
    @DisplayName("String round-trip should preserve data")
    void stringRoundTripShouldPreserveData() {
      final String original = "Hello, World! 123";

      final MemorySegment segment = manager.fromString(original);
      final String result = manager.toString(segment);

      assertEquals(original, result, "String should be preserved in round-trip");
    }

    @Test
    @DisplayName("ByteBuffer round-trip should preserve data")
    void byteBufferRoundTripShouldPreserveData() {
      final byte[] data = {1, 2, 3, 4, 5};
      final ByteBuffer original = ByteBuffer.wrap(data);

      final MemorySegment segment = manager.fromByteBuffer(original);
      final ByteBuffer result = manager.asByteBuffer(segment);

      final byte[] resultData = new byte[result.remaining()];
      result.get(resultData);

      assertArrayEquals(data, resultData, "Data should be preserved in round-trip");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full lifecycle should work correctly")
    void fullLifecycleShouldWorkCorrectly() {
      // Create manager
      try (Arena testArena = Arena.ofConfined()) {
        final MemorySegmentManager testManager = new MemorySegmentManager(testArena);

        // Allocate memory
        final MemorySegment allocated = testManager.allocate(100);
        assertEquals(100, allocated.byteSize(), "Allocation should work");

        // Create from byte array
        final byte[] data = {1, 2, 3, 4, 5};
        final MemorySegment fromArray = testManager.fromByteArray(data);
        assertArrayEquals(
            data, testManager.toByteArray(fromArray), "Byte array conversion should work");

        // Create from string
        final String str = "Test String";
        final MemorySegment fromString = testManager.fromString(str);
        assertEquals(str, testManager.toString(fromString), "String conversion should work");

        // Create from ByteBuffer
        final ByteBuffer buffer = ByteBuffer.wrap(new byte[] {10, 20, 30});
        final MemorySegment fromBuffer = testManager.fromByteBuffer(buffer);
        assertEquals(3, fromBuffer.byteSize(), "ByteBuffer conversion should work");

        // Arena should still be valid
        assertTrue(testArena.scope().isAlive(), "Arena should still be alive");
      }
    }

    @Test
    @DisplayName("Multiple allocations should work correctly")
    void multipleAllocationsShouldWorkCorrectly() {
      final MemorySegment seg1 = manager.allocate(10);
      final MemorySegment seg2 = manager.allocate(20);
      final MemorySegment seg3 = manager.allocate(30);

      assertEquals(10, seg1.byteSize(), "First segment should have correct size");
      assertEquals(20, seg2.byteSize(), "Second segment should have correct size");
      assertEquals(30, seg3.byteSize(), "Third segment should have correct size");
    }

    @Test
    @DisplayName("Mixed operations should work correctly")
    void mixedOperationsShouldWorkCorrectly() {
      // Allocate and fill
      final MemorySegment filled = manager.allocateAndFill(5, (byte) 0xFF);

      // Convert to byte array
      final byte[] data = manager.toByteArray(filled);
      assertEquals(5, data.length, "Should have correct length");

      // All bytes should be 0xFF
      for (byte b : data) {
        assertEquals((byte) 0xFF, b, "All bytes should be filled");
      }

      // Convert to ByteBuffer
      final ByteBuffer buffer = manager.asByteBuffer(filled);
      assertEquals(5, buffer.remaining(), "Buffer should have correct size");
    }
  }
}

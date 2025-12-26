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

package ai.tegmentum.wasmtime4j.coredump;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultCoreDumpMemory} class.
 *
 * <p>DefaultCoreDumpMemory is the default implementation of the CoreDumpMemory interface.
 */
@DisplayName("DefaultCoreDumpMemory Tests")
class DefaultCoreDumpMemoryTest {

  private static final long PAGE_SIZE = 65536L;

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create builder via static method")
    void shouldCreateBuilderViaStaticMethod() {
      final DefaultCoreDumpMemory.Builder builder = DefaultCoreDumpMemory.builder();
      assertNotNull(builder, "Builder should not be null");
    }

    @Test
    @DisplayName("should build with instanceIndex")
    void shouldBuildWithInstanceIndex() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().instanceIndex(2).build();
      assertEquals(2, memory.getInstanceIndex(), "InstanceIndex should match");
    }

    @Test
    @DisplayName("should build with memoryIndex")
    void shouldBuildWithMemoryIndex() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().memoryIndex(1).build();
      assertEquals(1, memory.getMemoryIndex(), "MemoryIndex should match");
    }

    @Test
    @DisplayName("should build with name")
    void shouldBuildWithName() {
      final DefaultCoreDumpMemory memory =
          DefaultCoreDumpMemory.builder().name("main-memory").build();
      assertTrue(memory.getName().isPresent(), "Name should be present");
      assertEquals("main-memory", memory.getName().get(), "Name should match");
    }

    @Test
    @DisplayName("should build without name")
    void shouldBuildWithoutName() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().build();
      assertFalse(memory.getName().isPresent(), "Name should not be present");
    }

    @Test
    @DisplayName("should build with sizeInPages")
    void shouldBuildWithSizeInPages() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().sizeInPages(10).build();
      assertEquals(10, memory.getSizeInPages(), "SizeInPages should match");
    }

    @Test
    @DisplayName("should build with memory64 flag")
    void shouldBuildWithMemory64Flag() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().memory64(true).build();
      assertTrue(memory.isMemory64(), "Memory64 should be true");
    }

    @Test
    @DisplayName("should build with minPages")
    void shouldBuildWithMinPages() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().minPages(1).build();
      assertEquals(1, memory.getMinPages(), "MinPages should match");
    }

    @Test
    @DisplayName("should build with maxPages")
    void shouldBuildWithMaxPages() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().maxPages(100L).build();
      assertTrue(memory.getMaxPages().isPresent(), "MaxPages should be present");
      assertEquals(100L, memory.getMaxPages().get(), "MaxPages should match");
    }

    @Test
    @DisplayName("should build without maxPages")
    void shouldBuildWithoutMaxPages() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().build();
      assertFalse(memory.getMaxPages().isPresent(), "MaxPages should not be present");
    }
  }

  @Nested
  @DisplayName("Size Calculation Tests")
  class SizeCalculationTests {

    @Test
    @DisplayName("should calculate size in bytes correctly")
    void shouldCalculateSizeInBytesCorrectly() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().sizeInPages(2).build();
      assertEquals(2 * PAGE_SIZE, memory.getSizeInBytes(), "Size in bytes should be 2 pages");
    }

    @Test
    @DisplayName("should return zero for zero pages")
    void shouldReturnZeroForZeroPages() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().sizeInPages(0).build();
      assertEquals(0, memory.getSizeInBytes(), "Size should be zero");
    }
  }

  @Nested
  @DisplayName("Segment Tests")
  class SegmentTests {

    @Test
    @DisplayName("should build with single segment")
    void shouldBuildWithSingleSegment() {
      final byte[] data = {0x01, 0x02, 0x03, 0x04};
      final DefaultCoreDumpMemory memory =
          DefaultCoreDumpMemory.builder().sizeInPages(1).addSegment(0, data).build();
      assertEquals(1, memory.getSegments().size(), "Should have 1 segment");
    }

    @Test
    @DisplayName("should build with multiple segments")
    void shouldBuildWithMultipleSegments() {
      final DefaultCoreDumpMemory memory =
          DefaultCoreDumpMemory.builder()
              .sizeInPages(1)
              .addSegment(0, new byte[] {0x01})
              .addSegment(100, new byte[] {0x02})
              .addSegment(200, new byte[] {0x03})
              .build();
      assertEquals(3, memory.getSegments().size(), "Should have 3 segments");
    }

    @Test
    @DisplayName("should build with segment object")
    void shouldBuildWithSegmentObject() {
      final CoreDumpMemory.MemorySegment segment =
          new DefaultCoreDumpMemory.DefaultMemorySegment(50, new byte[] {0x0A, 0x0B});
      final DefaultCoreDumpMemory memory =
          DefaultCoreDumpMemory.builder().sizeInPages(1).addSegment(segment).build();
      assertEquals(1, memory.getSegments().size(), "Should have 1 segment");
      assertEquals(50, memory.getSegments().get(0).getOffset(), "Segment offset should match");
    }

    @Test
    @DisplayName("should build with segments list")
    void shouldBuildWithSegmentsList() {
      final List<CoreDumpMemory.MemorySegment> segments =
          Arrays.asList(
              new DefaultCoreDumpMemory.DefaultMemorySegment(0, new byte[] {0x01}),
              new DefaultCoreDumpMemory.DefaultMemorySegment(10, new byte[] {0x02}));
      final DefaultCoreDumpMemory memory =
          DefaultCoreDumpMemory.builder().sizeInPages(1).addSegments(segments).build();
      assertEquals(2, memory.getSegments().size(), "Should have 2 segments");
    }
  }

  @Nested
  @DisplayName("DefaultMemorySegment Tests")
  class DefaultMemorySegmentTests {

    @Test
    @DisplayName("should create segment with offset and data")
    void shouldCreateSegmentWithOffsetAndData() {
      final byte[] data = {0x01, 0x02, 0x03};
      final DefaultCoreDumpMemory.DefaultMemorySegment segment =
          new DefaultCoreDumpMemory.DefaultMemorySegment(100, data);
      assertEquals(100, segment.getOffset(), "Offset should match");
      assertArrayEquals(data, segment.getData(), "Data should match");
    }

    @Test
    @DisplayName("should return size from data length")
    void shouldReturnSizeFromDataLength() {
      final byte[] data = new byte[50];
      final DefaultCoreDumpMemory.DefaultMemorySegment segment =
          new DefaultCoreDumpMemory.DefaultMemorySegment(0, data);
      assertEquals(50, segment.getSize(), "Size should be 50");
    }

    @Test
    @DisplayName("should return copy of data")
    void shouldReturnCopyOfData() {
      final byte[] original = {0x01, 0x02, 0x03};
      final DefaultCoreDumpMemory.DefaultMemorySegment segment =
          new DefaultCoreDumpMemory.DefaultMemorySegment(0, original);
      final byte[] retrieved = segment.getData();
      retrieved[0] = (byte) 0xFF;
      assertArrayEquals(original, segment.getData(), "Internal data should not be modified");
    }

    @Test
    @DisplayName("should throw on negative offset")
    void shouldThrowOnNegativeOffset() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DefaultCoreDumpMemory.DefaultMemorySegment(-1, new byte[] {0x01}),
          "Should throw on negative offset");
    }

    @Test
    @DisplayName("should throw on null data")
    void shouldThrowOnNullData() {
      assertThrows(
          NullPointerException.class,
          () -> new DefaultCoreDumpMemory.DefaultMemorySegment(0, null),
          "Should throw on null data");
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final DefaultCoreDumpMemory.DefaultMemorySegment segment1 =
          new DefaultCoreDumpMemory.DefaultMemorySegment(10, new byte[] {0x01, 0x02});
      final DefaultCoreDumpMemory.DefaultMemorySegment segment2 =
          new DefaultCoreDumpMemory.DefaultMemorySegment(10, new byte[] {0x01, 0x02});
      final DefaultCoreDumpMemory.DefaultMemorySegment segment3 =
          new DefaultCoreDumpMemory.DefaultMemorySegment(20, new byte[] {0x01, 0x02});

      assertEquals(segment1, segment2, "Equal segments should be equal");
      assertFalse(segment1.equals(segment3), "Different segments should not be equal");
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final DefaultCoreDumpMemory.DefaultMemorySegment segment1 =
          new DefaultCoreDumpMemory.DefaultMemorySegment(10, new byte[] {0x01, 0x02});
      final DefaultCoreDumpMemory.DefaultMemorySegment segment2 =
          new DefaultCoreDumpMemory.DefaultMemorySegment(10, new byte[] {0x01, 0x02});
      assertEquals(
          segment1.hashCode(), segment2.hashCode(), "Equal segments should have same hash");
    }

    @Test
    @DisplayName("should return meaningful toString")
    void shouldReturnMeaningfulToString() {
      final DefaultCoreDumpMemory.DefaultMemorySegment segment =
          new DefaultCoreDumpMemory.DefaultMemorySegment(100, new byte[25]);
      final String str = segment.toString();
      assertTrue(str.contains("MemorySegment"), "Should contain class name");
      assertTrue(str.contains("100"), "Should contain offset");
      assertTrue(str.contains("25"), "Should contain size");
    }
  }

  @Nested
  @DisplayName("Read Tests")
  class ReadTests {

    @Test
    @DisplayName("should read data from segment")
    void shouldReadDataFromSegment() {
      final byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
      final DefaultCoreDumpMemory memory =
          DefaultCoreDumpMemory.builder().sizeInPages(1).addSegment(0, data).build();
      final byte[] result = memory.read(0, 5);
      assertArrayEquals(data, result, "Read data should match segment");
    }

    @Test
    @DisplayName("should read partial data from segment")
    void shouldReadPartialDataFromSegment() {
      final byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
      final DefaultCoreDumpMemory memory =
          DefaultCoreDumpMemory.builder().sizeInPages(1).addSegment(0, data).build();
      final byte[] result = memory.read(1, 3);
      assertArrayEquals(new byte[] {0x02, 0x03, 0x04}, result, "Read data should match partial");
    }

    @Test
    @DisplayName("should read across multiple segments")
    void shouldReadAcrossMultipleSegments() {
      final DefaultCoreDumpMemory memory =
          DefaultCoreDumpMemory.builder()
              .sizeInPages(1)
              .addSegment(0, new byte[] {0x01, 0x02})
              .addSegment(5, new byte[] {0x03, 0x04})
              .build();
      final byte[] result = memory.read(0, 7);
      final byte[] expected = {0x01, 0x02, 0x00, 0x00, 0x00, 0x03, 0x04};
      assertArrayEquals(expected, result, "Read should include both segments with gap");
    }

    @Test
    @DisplayName("should return zeros for uninitialized memory")
    void shouldReturnZerosForUninitializedMemory() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().sizeInPages(1).build();
      final byte[] result = memory.read(0, 10);
      assertArrayEquals(new byte[10], result, "Uninitialized memory should be zeros");
    }

    @Test
    @DisplayName("should throw on negative offset")
    void shouldThrowOnNegativeOffset() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().sizeInPages(1).build();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.read(-1, 10),
          "Should throw on negative offset");
    }

    @Test
    @DisplayName("should throw on negative length")
    void shouldThrowOnNegativeLength() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().sizeInPages(1).build();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.read(0, -1),
          "Should throw on negative length");
    }

    @Test
    @DisplayName("should throw on out of bounds read")
    void shouldThrowOnOutOfBoundsRead() {
      final DefaultCoreDumpMemory memory = DefaultCoreDumpMemory.builder().sizeInPages(1).build();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.read(PAGE_SIZE - 10, 20),
          "Should throw on out of bounds read");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return meaningful string representation")
    void shouldReturnMeaningfulStringRepresentation() {
      final DefaultCoreDumpMemory memory =
          DefaultCoreDumpMemory.builder()
              .instanceIndex(0)
              .memoryIndex(0)
              .name("main")
              .sizeInPages(10)
              .memory64(false)
              .addSegment(0, new byte[100])
              .build();
      final String str = memory.toString();
      assertTrue(str.contains("CoreDumpMemory"), "Should contain class name");
      assertTrue(str.contains("main"), "Should contain name");
      assertTrue(str.contains("10"), "Should contain size in pages");
    }
  }
}

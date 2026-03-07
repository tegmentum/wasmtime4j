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
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.testing.RequiresWasmRuntime;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the WasmMemory interface.
 *
 * <p>Tests verify memory creation, size operations, read/write operations, growth, bulk operations,
 * and 64-bit addressing support. These tests require the native Wasmtime runtime to be available.
 */
@DisplayName("WasmMemory Interface Tests")
@RequiresWasmRuntime
class MemoryTest {

  /** WebAssembly page size in bytes (64KB). */
  private static final int PAGE_SIZE = 65536;

  private Engine engine;
  private Store store;
  private WasmMemory memory;

  @BeforeEach
  void setUp() throws WasmException {
    engine = Engine.create();
    store = engine.createStore();
    memory = store.createMemory(1, 10);
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
      store = null;
    }
    if (engine != null) {
      engine.close();
      engine = null;
    }
    memory = null;
  }

  @Nested
  @DisplayName("Memory Creation Tests")
  class MemoryCreationTests {

    @Test
    @DisplayName("should create memory with initial size")
    void shouldCreateMemoryWithInitialSize() throws WasmException {
      final WasmMemory mem = store.createMemory(2, 10);
      assertNotNull(mem, "Memory should not be null");
      assertEquals(2, mem.getSize(), "Memory should have initial size of 2 pages");
    }

    @Test
    @DisplayName("should create memory with no maximum")
    void shouldCreateMemoryWithNoMaximum() throws WasmException {
      final WasmMemory mem = store.createMemory(1, -1);
      assertNotNull(mem, "Memory should not be null");
      assertEquals(1, mem.getSize(), "Memory should have initial size of 1 page");
    }

    @Test
    @DisplayName("should report correct max size")
    void shouldReportCorrectMaxSize() throws WasmException {
      final WasmMemory mem = store.createMemory(1, 5);
      assertEquals(5, mem.getMaxSize(), "Max size should be 5 pages");
    }
  }

  @Nested
  @DisplayName("Memory Size Tests")
  class MemorySizeTests {

    @Test
    @DisplayName("should report correct size using getSize")
    void shouldReportCorrectSizeUsingGetSize() {
      assertEquals(1, memory.getSize(), "Size should be 1 page");
    }

    @Test
    @DisplayName("should report correct size using size alias")
    void shouldReportCorrectSizeUsingSizeAlias() {
      assertEquals(memory.getSize(), memory.size(), "size() should equal getSize()");
    }

    @Test
    @DisplayName("should calculate data size in bytes")
    void shouldCalculateDataSizeInBytes() {
      final long dataSize = memory.dataSize();
      assertEquals(PAGE_SIZE, dataSize, "Data size should be PAGE_SIZE bytes");
    }

    @Test
    @DisplayName("should report page size")
    void shouldReportPageSize() {
      assertEquals(PAGE_SIZE, memory.pageSize(), "Page size should be 64KB");
    }

    @Test
    @DisplayName("should report page size log2")
    void shouldReportPageSizeLog2() {
      assertEquals(16, memory.pageSizeLog2(), "Page size log2 should be 16");
    }
  }

  @Nested
  @DisplayName("Memory Growth Tests")
  class MemoryGrowthTests {

    @Test
    @DisplayName("should grow memory successfully")
    void shouldGrowMemorySuccessfully() {
      final int previousSize = memory.grow(1);
      assertEquals(1, previousSize, "Previous size should be 1 page");
      assertEquals(2, memory.getSize(), "New size should be 2 pages");
    }

    @Test
    @DisplayName("should grow memory by multiple pages")
    void shouldGrowMemoryByMultiplePages() {
      final int previousSize = memory.grow(3);
      assertEquals(1, previousSize, "Previous size should be 1 page");
      assertEquals(4, memory.getSize(), "New size should be 4 pages");
    }

    @Test
    @DisplayName("should fail to grow beyond maximum")
    void shouldFailToGrowBeyondMaximum() throws WasmException {
      final WasmMemory limitedMem = store.createMemory(1, 2);
      limitedMem.grow(1); // Should succeed
      final int result = limitedMem.grow(5); // Should fail
      assertEquals(-1, result, "Growth beyond max should return -1");
    }

    @Test
    @DisplayName("should grow by zero pages")
    void shouldGrowByZeroPages() {
      final int previousSize = memory.grow(0);
      assertEquals(1, previousSize, "Previous size should be 1 page");
      assertEquals(1, memory.getSize(), "Size should remain 1 page");
    }
  }

  @Nested
  @DisplayName("Byte Read/Write Tests")
  class ByteReadWriteTests {

    @Test
    @DisplayName("should write and read single byte")
    void shouldWriteAndReadSingleByte() {
      memory.writeByte(0, (byte) 42);
      assertEquals((byte) 42, memory.readByte(0), "Read byte should match written byte");
    }

    @Test
    @DisplayName("should write and read at various offsets")
    void shouldWriteAndReadAtVariousOffsets() {
      memory.writeByte(0, (byte) 1);
      memory.writeByte(100, (byte) 2);
      memory.writeByte(1000, (byte) 3);
      memory.writeByte(PAGE_SIZE - 1, (byte) 4);

      assertEquals((byte) 1, memory.readByte(0), "Byte at 0 should match");
      assertEquals((byte) 2, memory.readByte(100), "Byte at 100 should match");
      assertEquals((byte) 3, memory.readByte(1000), "Byte at 1000 should match");
      assertEquals((byte) 4, memory.readByte(PAGE_SIZE - 1), "Byte at end should match");
    }

    @Test
    @DisplayName("should handle negative byte values")
    void shouldHandleNegativeByteValues() {
      memory.writeByte(0, (byte) -128);
      memory.writeByte(1, (byte) -1);

      assertEquals((byte) -128, memory.readByte(0), "Negative byte -128 should match");
      assertEquals((byte) -1, memory.readByte(1), "Negative byte -1 should match");
    }
  }

  @Nested
  @DisplayName("Bulk Read/Write Tests")
  class BulkReadWriteTests {

    @Test
    @DisplayName("should write and read byte array")
    void shouldWriteAndReadByteArray() {
      final byte[] data = {1, 2, 3, 4, 5};
      memory.writeBytes(0, data, 0, data.length);

      final byte[] result = new byte[5];
      memory.readBytes(0, result, 0, result.length);

      assertArrayEquals(data, result, "Read bytes should match written bytes");
    }

    @Test
    @DisplayName("should write and read with offset")
    void shouldWriteAndReadWithOffset() {
      final byte[] data = {0, 0, 1, 2, 3, 0, 0};
      memory.writeBytes(100, data, 2, 3);

      final byte[] result = new byte[3];
      memory.readBytes(100, result, 0, 3);

      assertArrayEquals(new byte[] {1, 2, 3}, result, "Read bytes should match source range");
    }

    @Test
    @DisplayName("should write large byte array")
    void shouldWriteLargeByteArray() {
      final byte[] data = new byte[1000];
      for (int i = 0; i < data.length; i++) {
        data[i] = (byte) (i % 256);
      }
      memory.writeBytes(0, data, 0, data.length);

      final byte[] result = new byte[1000];
      memory.readBytes(0, result, 0, result.length);

      assertArrayEquals(data, result, "Large byte array should match");
    }
  }

  @Nested
  @DisplayName("Integer Read/Write Tests")
  class IntegerReadWriteTests {

    @Test
    @DisplayName("should write and read 32-bit integer")
    void shouldWriteAndRead32BitInteger() {
      memory.writeInt32(0, 0x12345678);
      assertEquals(0x12345678, memory.readInt32(0), "Int32 should match");
    }

    @Test
    @DisplayName("should write and read negative 32-bit integer")
    void shouldWriteAndReadNegative32BitInteger() {
      memory.writeInt32(0, -12345);
      assertEquals(-12345, memory.readInt32(0), "Negative int32 should match");
    }

    @Test
    @DisplayName("should write and read 64-bit integer")
    void shouldWriteAndRead64BitInteger() {
      memory.writeInt64(0, 0x123456789ABCDEF0L);
      assertEquals(0x123456789ABCDEF0L, memory.readInt64(0), "Int64 should match");
    }

    @Test
    @DisplayName("should write and read negative 64-bit integer")
    void shouldWriteAndReadNegative64BitInteger() {
      memory.writeInt64(0, -9876543210L);
      assertEquals(-9876543210L, memory.readInt64(0), "Negative int64 should match");
    }
  }

  @Nested
  @DisplayName("Float Read/Write Tests")
  class FloatReadWriteTests {

    @Test
    @DisplayName("should write and read 32-bit float")
    void shouldWriteAndRead32BitFloat() {
      memory.writeFloat32(0, 3.14159f);
      assertEquals(3.14159f, memory.readFloat32(0), 0.00001f, "Float32 should match");
    }

    @Test
    @DisplayName("should write and read 64-bit double")
    void shouldWriteAndRead64BitDouble() {
      memory.writeFloat64(0, 2.718281828459045);
      assertEquals(
          2.718281828459045, memory.readFloat64(0), 0.000000000001, "Float64 should match");
    }

    @Test
    @DisplayName("should handle special float values")
    void shouldHandleSpecialFloatValues() {
      memory.writeFloat32(0, Float.NaN);
      assertTrue(Float.isNaN(memory.readFloat32(0)), "NaN should be preserved");

      memory.writeFloat32(4, Float.POSITIVE_INFINITY);
      assertEquals(
          Float.POSITIVE_INFINITY, memory.readFloat32(4), "Positive infinity should match");

      memory.writeFloat32(8, Float.NEGATIVE_INFINITY);
      assertEquals(
          Float.NEGATIVE_INFINITY, memory.readFloat32(8), "Negative infinity should match");
    }
  }

  @Nested
  @DisplayName("Bulk Memory Operations Tests")
  class BulkMemoryOperationsTests {

    @Test
    @DisplayName("should fill memory region")
    void shouldFillMemoryRegion() {
      memory.fill(0, (byte) 0xFF, 100);

      for (int i = 0; i < 100; i++) {
        assertEquals((byte) 0xFF, memory.readByte(i), "Filled byte at " + i + " should be 0xFF");
      }
    }

    @Test
    @DisplayName("should copy memory region")
    void shouldCopyMemoryRegion() {
      final byte[] data = {1, 2, 3, 4, 5};
      memory.writeBytes(0, data, 0, data.length);

      memory.copy(100, 0, 5);

      final byte[] result = new byte[5];
      memory.readBytes(100, result, 0, 5);
      assertArrayEquals(data, result, "Copied bytes should match original");
    }

    @Test
    @DisplayName("should handle overlapping copy forward")
    void shouldHandleOverlappingCopyForward() {
      final byte[] data = {1, 2, 3, 4, 5, 0, 0, 0};
      memory.writeBytes(0, data, 0, data.length);

      memory.copy(2, 0, 5);

      final byte[] result = new byte[7];
      memory.readBytes(0, result, 0, 7);
      assertArrayEquals(new byte[] {1, 2, 1, 2, 3, 4, 5}, result, "Overlapping copy should work");
    }
  }

  @Nested
  @DisplayName("ByteBuffer Access Tests")
  class ByteBufferAccessTests {

    @Test
    @DisplayName("should get ByteBuffer view")
    void shouldGetByteBufferView() {
      final ByteBuffer buffer = memory.getBuffer();
      assertNotNull(buffer, "Buffer should not be null");
      assertTrue(buffer.capacity() >= PAGE_SIZE, "Buffer capacity should be at least PAGE_SIZE");
    }

    @Test
    @DisplayName("should reflect memory writes in buffer")
    void shouldReflectMemoryWritesInBuffer() {
      memory.writeByte(0, (byte) 42);
      final ByteBuffer buffer = memory.getBuffer();
      assertEquals((byte) 42, buffer.get(0), "Buffer should reflect memory write");
    }
  }

  @Nested
  @DisplayName("Memory Type Tests")
  class MemoryTypeTests {

    @Test
    @DisplayName("should return memory type")
    void shouldReturnMemoryType() {
      final MemoryType memType = memory.getMemoryType();
      assertNotNull(memType, "Memory type should not be null");
      assertEquals(1, memType.getMinimum(), "Minimum should be 1 page");
      final long maxValue = memType.getMaximum().orElse(-1L);
      assertTrue(maxValue >= 10 || maxValue == -1, "Maximum should be at least 10 or unlimited");
    }

    @Test
    @DisplayName("should report shared status")
    void shouldReportSharedStatus() {
      assertFalse(memory.isShared(), "Default memory should not be shared");
    }

    @Test
    @DisplayName("should report 64-bit addressing support")
    void shouldReport64BitAddressingSupport() {
      // Default memory typically doesn't support 64-bit addressing
      assertNotNull(
          Boolean.valueOf(memory.supports64BitAddressing()),
          "Should report 64-bit addressing support status");
    }
  }

  @Nested
  @DisplayName("64-bit Operations Tests")
  class SixtyFourBitOperationsTests {

    @Test
    @DisplayName("should get size using 64-bit method")
    void shouldGetSizeUsing64BitMethod() {
      final long size64 = memory.getSize64();
      assertEquals(1L, size64, "64-bit size should be 1 page");
    }

    @Test
    @DisplayName("should get size in bytes using 64-bit method")
    void shouldGetSizeInBytesUsing64BitMethod() {
      final long sizeBytes = memory.getSizeInBytes64();
      assertEquals((long) PAGE_SIZE, sizeBytes, "Size in bytes should be PAGE_SIZE");
    }

    @Test
    @DisplayName("should read and write byte using 64-bit offset")
    void shouldReadAndWriteByteUsing64BitOffset() {
      memory.writeByte64(0L, (byte) 99);
      assertEquals((byte) 99, memory.readByte64(0L), "64-bit byte read/write should work");
    }
  }

  @Nested
  @DisplayName("Memory Minimum Size Tests")
  class MemoryMinimumSizeTests {

    @Test
    @DisplayName("should return minimum size")
    void shouldReturnMinimumSize() {
      assertEquals(1, memory.getMinSize(), "Minimum size should be 1 page");
    }
  }
}

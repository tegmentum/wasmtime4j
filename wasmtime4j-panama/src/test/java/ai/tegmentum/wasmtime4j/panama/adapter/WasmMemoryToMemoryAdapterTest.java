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

package ai.tegmentum.wasmtime4j.panama.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Memory;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Panama {@link WasmMemoryToMemoryAdapter} class.
 *
 * <p>This test class verifies the adapter that bridges WasmMemory to Memory interface.
 */
@DisplayName("Panama WasmMemoryToMemoryAdapter Tests")
class WasmMemoryToMemoryAdapterTest {

  private static final int PAGE_SIZE = 65536; // 64KB

  /**
   * Creates a mock WasmMemory backed by a byte array for testing.
   */
  private WasmMemory createMockMemory(final int initialPages, final int maxPages) {
    return new WasmMemory() {
      private int pages = initialPages;
      private final int maxPagesLimit = maxPages;
      private byte[] data = new byte[initialPages * PAGE_SIZE];

      @Override
      public long getSize() {
        return pages;
      }

      @Override
      public long getMaxSize() {
        return maxPagesLimit;
      }

      @Override
      public int grow(final int deltaPages) {
        if (pages + deltaPages > maxPagesLimit) {
          return -1;
        }
        final int oldPages = pages;
        pages += deltaPages;
        final byte[] newData = new byte[pages * PAGE_SIZE];
        System.arraycopy(data, 0, newData, 0, data.length);
        data = newData;
        return oldPages;
      }

      @Override
      public byte readByte(final int offset) {
        if (offset < 0 || offset >= data.length) {
          throw new IndexOutOfBoundsException("Offset out of bounds: " + offset);
        }
        return data[offset];
      }

      @Override
      public void writeByte(final int offset, final byte value) {
        if (offset < 0 || offset >= data.length) {
          throw new IndexOutOfBoundsException("Offset out of bounds: " + offset);
        }
        data[offset] = value;
      }

      @Override
      public byte readByte64(final long offset) {
        return readByte((int) offset);
      }

      @Override
      public void writeByte64(final long offset, final byte value) {
        writeByte((int) offset, value);
      }

      @Override
      public void readBytes(final int offset, final byte[] dest, final int destOffset,
          final int length) {
        if (offset < 0 || offset + length > data.length) {
          throw new IndexOutOfBoundsException("Read out of bounds");
        }
        System.arraycopy(data, offset, dest, destOffset, length);
      }

      @Override
      public void writeBytes(final int offset, final byte[] src, final int srcOffset,
          final int length) {
        if (offset < 0 || offset + length > data.length) {
          throw new IndexOutOfBoundsException("Write out of bounds");
        }
        System.arraycopy(src, srcOffset, data, offset, length);
      }

      @Override
      public void readBytes64(final long offset, final byte[] dest, final int destOffset,
          final int length) {
        readBytes((int) offset, dest, destOffset, length);
      }

      @Override
      public void writeBytes64(final long offset, final byte[] src, final int srcOffset,
          final int length) {
        writeBytes((int) offset, src, srcOffset, length);
      }

      @Override
      public int readInt32(final long offset) {
        final int intOffset = (int) offset;
        if (intOffset < 0 || intOffset + 4 > data.length) {
          throw new IndexOutOfBoundsException("Read int32 out of bounds");
        }
        return ByteBuffer.wrap(data, intOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
      }

      @Override
      public void writeInt32(final long offset, final int value) {
        final int intOffset = (int) offset;
        if (intOffset < 0 || intOffset + 4 > data.length) {
          throw new IndexOutOfBoundsException("Write int32 out of bounds");
        }
        ByteBuffer.wrap(data, intOffset, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(value);
      }

      @Override
      public long readInt64(final long offset) {
        final int intOffset = (int) offset;
        if (intOffset < 0 || intOffset + 8 > data.length) {
          throw new IndexOutOfBoundsException("Read int64 out of bounds");
        }
        return ByteBuffer.wrap(data, intOffset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
      }

      @Override
      public void writeInt64(final long offset, final long value) {
        final int intOffset = (int) offset;
        if (intOffset < 0 || intOffset + 8 > data.length) {
          throw new IndexOutOfBoundsException("Write int64 out of bounds");
        }
        ByteBuffer.wrap(data, intOffset, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(value);
      }
    };
  }

  /**
   * Creates a mock WasmMemory that throws on getSize().
   */
  private WasmMemory createFailingMemory() {
    return new WasmMemory() {
      @Override
      public long getSize() {
        throw new RuntimeException("Memory access failed");
      }

      @Override
      public long getMaxSize() {
        return 0;
      }

      @Override
      public int grow(final int deltaPages) {
        return -1;
      }

      @Override
      public byte readByte(final int offset) {
        throw new RuntimeException("Memory access failed");
      }

      @Override
      public void writeByte(final int offset, final byte value) {
        throw new RuntimeException("Memory access failed");
      }

      @Override
      public byte readByte64(final long offset) {
        throw new RuntimeException("Memory access failed");
      }

      @Override
      public void writeByte64(final long offset, final byte value) {
        throw new RuntimeException("Memory access failed");
      }

      @Override
      public void readBytes(final int offset, final byte[] dest, final int destOffset,
          final int length) {
        throw new RuntimeException("Memory access failed");
      }

      @Override
      public void writeBytes(final int offset, final byte[] src, final int srcOffset,
          final int length) {
        throw new RuntimeException("Memory access failed");
      }

      @Override
      public void readBytes64(final long offset, final byte[] dest, final int destOffset,
          final int length) {
        throw new RuntimeException("Memory access failed");
      }

      @Override
      public void writeBytes64(final long offset, final byte[] src, final int srcOffset,
          final int length) {
        throw new RuntimeException("Memory access failed");
      }

      @Override
      public int readInt32(final long offset) {
        throw new RuntimeException("Memory access failed");
      }

      @Override
      public void writeInt32(final long offset, final int value) {
        throw new RuntimeException("Memory access failed");
      }

      @Override
      public long readInt64(final long offset) {
        throw new RuntimeException("Memory access failed");
      }

      @Override
      public void writeInt64(final long offset, final long value) {
        throw new RuntimeException("Memory access failed");
      }
    };
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasmMemoryToMemoryAdapter should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasmMemoryToMemoryAdapter.class.getModifiers()),
          "WasmMemoryToMemoryAdapter should be final");
    }

    @Test
    @DisplayName("WasmMemoryToMemoryAdapter should implement Memory")
    void shouldImplementMemory() {
      assertTrue(
          Memory.class.isAssignableFrom(WasmMemoryToMemoryAdapter.class),
          "WasmMemoryToMemoryAdapter should implement Memory");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid delegate")
    void constructorShouldAcceptValidDelegate() {
      final WasmMemory delegate = createMockMemory(1, 10);

      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertNotNull(adapter, "Adapter should not be null");
      assertSame(delegate, adapter.getDelegate(), "Delegate should match");
    }

    @Test
    @DisplayName("Constructor should throw on null delegate")
    void constructorShouldThrowOnNullDelegate() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasmMemoryToMemoryAdapter(null),
          "Should throw on null delegate");
    }
  }

  @Nested
  @DisplayName("getSize Tests")
  class GetSizeTests {

    @Test
    @DisplayName("getSize should return page count")
    void getSizeShouldReturnPageCount() {
      final WasmMemory delegate = createMockMemory(5, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertEquals(5, adapter.getSize(), "Size should be 5 pages");
    }

    @Test
    @DisplayName("getSize should return 1 for single page memory")
    void getSizeShouldReturnOneForSinglePageMemory() {
      final WasmMemory delegate = createMockMemory(1, 1);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertEquals(1, adapter.getSize(), "Size should be 1 page");
    }
  }

  @Nested
  @DisplayName("getSizeInBytes Tests")
  class GetSizeInBytesTests {

    @Test
    @DisplayName("getSizeInBytes should return correct byte count")
    void getSizeInBytesShouldReturnCorrectByteCount() {
      final WasmMemory delegate = createMockMemory(2, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertEquals(2L * PAGE_SIZE, adapter.getSizeInBytes(),
          "Size in bytes should be 2 * 64KB");
    }

    @Test
    @DisplayName("getSizeInBytes should return 64KB for single page")
    void getSizeInBytesShouldReturn64KbForSinglePage() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertEquals(PAGE_SIZE, adapter.getSizeInBytes(),
          "Size in bytes should be 64KB for single page");
    }
  }

  @Nested
  @DisplayName("grow Tests")
  class GrowTests {

    @Test
    @DisplayName("grow should return old page count on success")
    void growShouldReturnOldPageCountOnSuccess() throws WasmException {
      final WasmMemory delegate = createMockMemory(2, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      final long result = adapter.grow(3);

      assertEquals(2, result, "Should return old page count");
      assertEquals(5, adapter.getSize(), "New size should be 5 pages");
    }

    @Test
    @DisplayName("grow should fail when exceeding max pages")
    void growShouldFailWhenExceedingMaxPages() throws WasmException {
      final WasmMemory delegate = createMockMemory(5, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      final long result = adapter.grow(10);

      assertEquals(-1, result, "Should return -1 on failure");
      assertEquals(5, adapter.getSize(), "Size should remain unchanged");
    }

    @Test
    @DisplayName("grow should throw on excessively large delta")
    void growShouldThrowOnExcessivelyLargeDelta() {
      final WasmMemory delegate = createMockMemory(1, 100);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(WasmException.class, () -> adapter.grow((long) Integer.MAX_VALUE + 1),
          "Should throw for delta exceeding Integer.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("read Tests")
  class ReadTests {

    @Test
    @DisplayName("read should copy data to buffer")
    void readShouldCopyDataToBuffer() throws WasmException {
      final WasmMemory delegate = createMockMemory(1, 10);
      delegate.writeByte(0, (byte) 0x41);
      delegate.writeByte(1, (byte) 0x42);
      delegate.writeByte(2, (byte) 0x43);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      final ByteBuffer buffer = ByteBuffer.allocate(3);
      final int bytesRead = adapter.read(0, buffer);

      assertEquals(3, bytesRead, "Should read 3 bytes");
      buffer.flip();
      assertEquals((byte) 0x41, buffer.get(0), "First byte should be 0x41");
      assertEquals((byte) 0x42, buffer.get(1), "Second byte should be 0x42");
      assertEquals((byte) 0x43, buffer.get(2), "Third byte should be 0x43");
    }

    @Test
    @DisplayName("read should throw on null buffer")
    void readShouldThrowOnNullBuffer() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(IllegalArgumentException.class, () -> adapter.read(0, null),
          "Should throw on null buffer");
    }

    @Test
    @DisplayName("read should throw on negative offset")
    void readShouldThrowOnNegativeOffset() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(IllegalArgumentException.class,
          () -> adapter.read(-1, ByteBuffer.allocate(10)),
          "Should throw on negative offset");
    }

    @Test
    @DisplayName("read should throw on out of bounds")
    void readShouldThrowOnOutOfBounds() {
      final WasmMemory delegate = createMockMemory(1, 1);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(WasmException.class,
          () -> adapter.read(PAGE_SIZE + 1, ByteBuffer.allocate(10)),
          "Should throw on out of bounds read");
    }
  }

  @Nested
  @DisplayName("write Tests")
  class WriteTests {

    @Test
    @DisplayName("write should copy data from buffer")
    void writeShouldCopyDataFromBuffer() throws WasmException {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      final ByteBuffer buffer = ByteBuffer.wrap(new byte[]{0x58, 0x59, 0x5A});
      final int bytesWritten = adapter.write(0, buffer);

      assertEquals(3, bytesWritten, "Should write 3 bytes");
      assertEquals((byte) 0x58, delegate.readByte(0), "First byte should be 0x58");
      assertEquals((byte) 0x59, delegate.readByte(1), "Second byte should be 0x59");
      assertEquals((byte) 0x5A, delegate.readByte(2), "Third byte should be 0x5A");
    }

    @Test
    @DisplayName("write should throw on null buffer")
    void writeShouldThrowOnNullBuffer() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(IllegalArgumentException.class, () -> adapter.write(0, null),
          "Should throw on null buffer");
    }

    @Test
    @DisplayName("write should throw on negative offset")
    void writeShouldThrowOnNegativeOffset() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(IllegalArgumentException.class,
          () -> adapter.write(-1, ByteBuffer.wrap(new byte[10])),
          "Should throw on negative offset");
    }
  }

  @Nested
  @DisplayName("readByte Tests")
  class ReadByteTests {

    @Test
    @DisplayName("readByte should return correct value")
    void readByteShouldReturnCorrectValue() throws WasmException {
      final WasmMemory delegate = createMockMemory(1, 10);
      delegate.writeByte(100, (byte) 0xFF);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertEquals((byte) 0xFF, adapter.readByte(100), "Should read correct byte");
    }

    @Test
    @DisplayName("readByte should throw on negative offset")
    void readByteShouldThrowOnNegativeOffset() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(IllegalArgumentException.class, () -> adapter.readByte(-1),
          "Should throw on negative offset");
    }
  }

  @Nested
  @DisplayName("writeByte Tests")
  class WriteByteTests {

    @Test
    @DisplayName("writeByte should write correct value")
    void writeByteShouldWriteCorrectValue() throws WasmException {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      adapter.writeByte(50, (byte) 0xAB);

      assertEquals((byte) 0xAB, delegate.readByte(50), "Should write correct byte");
    }

    @Test
    @DisplayName("writeByte should throw on negative offset")
    void writeByteShouldThrowOnNegativeOffset() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(IllegalArgumentException.class, () -> adapter.writeByte(-1, (byte) 0),
          "Should throw on negative offset");
    }
  }

  @Nested
  @DisplayName("readInt32 Tests")
  class ReadInt32Tests {

    @Test
    @DisplayName("readInt32 should return correct value")
    void readInt32ShouldReturnCorrectValue() throws WasmException {
      final WasmMemory delegate = createMockMemory(1, 10);
      delegate.writeInt32(0, 0x12345678);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertEquals(0x12345678, adapter.readInt32(0), "Should read correct int32");
    }

    @Test
    @DisplayName("readInt32 should throw on negative offset")
    void readInt32ShouldThrowOnNegativeOffset() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(IllegalArgumentException.class, () -> adapter.readInt32(-1),
          "Should throw on negative offset");
    }

    @Test
    @DisplayName("readInt32 should throw on out of bounds")
    void readInt32ShouldThrowOnOutOfBounds() {
      final WasmMemory delegate = createMockMemory(1, 1);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(WasmException.class, () -> adapter.readInt32(PAGE_SIZE - 2),
          "Should throw on out of bounds");
    }
  }

  @Nested
  @DisplayName("writeInt32 Tests")
  class WriteInt32Tests {

    @Test
    @DisplayName("writeInt32 should write correct value")
    void writeInt32ShouldWriteCorrectValue() throws WasmException {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      adapter.writeInt32(0, Integer.MAX_VALUE);

      assertEquals(Integer.MAX_VALUE, delegate.readInt32(0), "Should write correct int32");
    }

    @Test
    @DisplayName("writeInt32 should throw on negative offset")
    void writeInt32ShouldThrowOnNegativeOffset() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(IllegalArgumentException.class, () -> adapter.writeInt32(-1, 0),
          "Should throw on negative offset");
    }
  }

  @Nested
  @DisplayName("readInt64 Tests")
  class ReadInt64Tests {

    @Test
    @DisplayName("readInt64 should return correct value")
    void readInt64ShouldReturnCorrectValue() throws WasmException {
      final WasmMemory delegate = createMockMemory(1, 10);
      delegate.writeInt64(0, Long.MAX_VALUE);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertEquals(Long.MAX_VALUE, adapter.readInt64(0), "Should read correct int64");
    }

    @Test
    @DisplayName("readInt64 should throw on negative offset")
    void readInt64ShouldThrowOnNegativeOffset() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(IllegalArgumentException.class, () -> adapter.readInt64(-1),
          "Should throw on negative offset");
    }
  }

  @Nested
  @DisplayName("writeInt64 Tests")
  class WriteInt64Tests {

    @Test
    @DisplayName("writeInt64 should write correct value")
    void writeInt64ShouldWriteCorrectValue() throws WasmException {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      adapter.writeInt64(0, Long.MIN_VALUE);

      assertEquals(Long.MIN_VALUE, delegate.readInt64(0), "Should write correct int64");
    }

    @Test
    @DisplayName("writeInt64 should throw on negative offset")
    void writeInt64ShouldThrowOnNegativeOffset() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertThrows(IllegalArgumentException.class, () -> adapter.writeInt64(-1, 0L),
          "Should throw on negative offset");
    }
  }

  @Nested
  @DisplayName("getMaxSize Tests")
  class GetMaxSizeTests {

    @Test
    @DisplayName("getMaxSize should return correct value")
    void getMaxSizeShouldReturnCorrectValue() {
      final WasmMemory delegate = createMockMemory(1, 100);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertEquals(100, adapter.getMaxSize(), "Max size should be 100");
    }
  }

  @Nested
  @DisplayName("isValid Tests")
  class IsValidTests {

    @Test
    @DisplayName("isValid should return true for valid memory")
    void isValidShouldReturnTrueForValidMemory() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertTrue(adapter.isValid(), "Should be valid");
    }

    @Test
    @DisplayName("isValid should return false when delegate throws")
    void isValidShouldReturnFalseWhenDelegateThrows() {
      final WasmMemory delegate = createFailingMemory();
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertFalse(adapter.isValid(), "Should be invalid when delegate throws");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should support sequential read and write operations")
    void shouldSupportSequentialReadAndWriteOperations() throws WasmException {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      // Write some data
      adapter.writeInt32(0, 100);
      adapter.writeInt64(4, 200L);
      adapter.writeByte(12, (byte) 0xFF);

      // Read it back
      assertEquals(100, adapter.readInt32(0), "Int32 should match");
      assertEquals(200L, adapter.readInt64(4), "Int64 should match");
      assertEquals((byte) 0xFF, adapter.readByte(12), "Byte should match");
    }

    @Test
    @DisplayName("Should support buffer-based read and write")
    void shouldSupportBufferBasedReadAndWrite() throws WasmException {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      // Write using buffer
      final byte[] writeData = {1, 2, 3, 4, 5};
      final ByteBuffer writeBuffer = ByteBuffer.wrap(writeData);
      adapter.write(100, writeBuffer);

      // Read using buffer
      final ByteBuffer readBuffer = ByteBuffer.allocate(5);
      adapter.read(100, readBuffer);

      readBuffer.flip();
      for (int i = 0; i < 5; i++) {
        assertEquals(writeData[i], readBuffer.get(i), "Byte " + i + " should match");
      }
    }

    @Test
    @DisplayName("Should maintain data after grow")
    void shouldMaintainDataAfterGrow() throws WasmException {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      // Write data before grow
      adapter.writeInt32(0, 42);

      // Grow memory
      adapter.grow(1);

      // Verify data is preserved
      assertEquals(42, adapter.readInt32(0), "Data should be preserved after grow");
      assertEquals(2, adapter.getSize(), "Size should be 2 pages after grow");
    }
  }

  @Nested
  @DisplayName("getDelegate Tests")
  class GetDelegateTests {

    @Test
    @DisplayName("getDelegate should return original delegate")
    void getDelegateShouldReturnOriginalDelegate() {
      final WasmMemory delegate = createMockMemory(1, 10);
      final WasmMemoryToMemoryAdapter adapter = new WasmMemoryToMemoryAdapter(delegate);

      assertSame(delegate, adapter.getDelegate(), "Should return same delegate");
    }
  }
}

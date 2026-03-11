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
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmMemory} default methods.
 *
 * <p>These tests use anonymous implementations to exercise default method behavior without
 * requiring a native runtime.
 */
@DisplayName("WasmMemory Default Method Tests")
class WasmMemoryTest {

  /** A simple in-memory WasmMemory implementation backed by a byte array. */
  private WasmMemory createTestMemory(final int pages) {
    final int pageSize = 65536;
    final byte[] data = new byte[pages * pageSize];

    return new WasmMemory() {
      private int currentPages = pages;

      @Override
      public int getSize() {
        return currentPages;
      }

      @Override
      public int grow(final int additionalPages) {
        int prev = currentPages;
        currentPages += additionalPages;
        return prev;
      }

      @Override
      public int getMaxSize() {
        return 100;
      }

      @Override
      public MemoryType getMemoryType() {
        return new MemoryType() {
          @Override
          public long getMinimum() {
            return pages;
          }

          @Override
          public Optional<Long> getMaximum() {
            return Optional.of(100L);
          }

          @Override
          public boolean is64Bit() {
            return false;
          }

          @Override
          public boolean isShared() {
            return false;
          }

          @Override
          public ai.tegmentum.wasmtime4j.type.WasmTypeKind getKind() {
            return ai.tegmentum.wasmtime4j.type.WasmTypeKind.MEMORY;
          }
        };
      }

      @Override
      public ByteBuffer getBuffer() {
        return ByteBuffer.wrap(data);
      }

      @Override
      public byte readByte(final int offset) {
        return data[offset];
      }

      @Override
      public void writeByte(final int offset, final byte value) {
        data[offset] = value;
      }

      @Override
      public void readBytes(
          final int offset, final byte[] dest, final int destOffset, final int length) {
        System.arraycopy(data, offset, dest, destOffset, length);
      }

      @Override
      public void writeBytes(
          final int offset, final byte[] src, final int srcOffset, final int length) {
        System.arraycopy(src, srcOffset, data, offset, length);
      }

      @Override
      public void copy(final int destOffset, final int srcOffset, final int length) {
        System.arraycopy(data, srcOffset, data, destOffset, length);
      }

      @Override
      public void fill(final int offset, final byte value, final int length) {
        java.util.Arrays.fill(data, offset, offset + length, value);
      }

      @Override
      public void init(
          final int destOffset, final int dataSegmentIndex, final int srcOffset, final int length) {
        throw new UnsupportedOperationException("Not implemented for test");
      }

      @Override
      public void dropDataSegment(final int dataSegmentIndex) {
        throw new UnsupportedOperationException("Not implemented for test");
      }

      @Override
      public boolean isShared() {
        return false;
      }

      @Override
      public int atomicCompareAndSwapInt(final int offset, final int expected, final int newValue) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long atomicCompareAndSwapLong(
          final int offset, final long expected, final long newValue) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int atomicLoadInt(final int offset) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long atomicLoadLong(final int offset) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void atomicStoreInt(final int offset, final int value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void atomicStoreLong(final int offset, final long value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int atomicAddInt(final int offset, final int value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long atomicAddLong(final int offset, final long value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int atomicAndInt(final int offset, final int value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int atomicOrInt(final int offset, final int value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int atomicXorInt(final int offset, final int value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long atomicAndLong(final int offset, final long value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long atomicOrLong(final int offset, final long value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long atomicXorLong(final int offset, final long value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void atomicFence() {
        throw new UnsupportedOperationException();
      }

      @Override
      public int atomicNotify(final int offset, final int count) {
        throw new UnsupportedOperationException();
      }

      @Override
      public WaitResult atomicWait32(
          final int offset, final int expected, final long timeoutNanos) {
        throw new UnsupportedOperationException();
      }

      @Override
      public WaitResult atomicWait64(
          final int offset, final long expected, final long timeoutNanos) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long growAsync(final long pages) throws WasmException {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Nested
  @DisplayName("Alias Default Methods")
  class AliasTests {

    @Test
    @DisplayName("size() should delegate to getSize()")
    void sizeShouldDelegateToGetSize() {
      final WasmMemory memory = createTestMemory(2);
      assertEquals(2, memory.size());
      assertEquals(memory.getSize(), memory.size());
    }
  }

  @Nested
  @DisplayName("64-bit Addressing Defaults")
  class Addressing64BitTests {

    @Test
    @DisplayName("supports64BitAddressing() should return false by default")
    void supports64BitAddressingShouldReturnFalseByDefault() {
      assertFalse(createTestMemory(1).supports64BitAddressing());
    }

    @Test
    @DisplayName("getSize64() should delegate to getSize() by default")
    void getSize64ShouldDelegateToGetSize() {
      final WasmMemory memory = createTestMemory(3);
      assertEquals(3L, memory.getSize64());
    }

    @Test
    @DisplayName("getMaxSize64() should delegate to getMaxSize()")
    void getMaxSize64ShouldDelegateToGetMaxSize() {
      final WasmMemory memory = createTestMemory(1);
      assertEquals(100L, memory.getMaxSize64());
    }

    @Test
    @DisplayName("grow64() should delegate to grow() for small values")
    void grow64ShouldDelegateToGrowForSmallValues() {
      final WasmMemory memory = createTestMemory(2);
      final long prev = memory.grow64(3);
      assertEquals(2L, prev);
    }

    @Test
    @DisplayName("grow64() should return -1 for values exceeding Integer.MAX_VALUE")
    void grow64ShouldReturnNegativeOneForLargeValues() {
      final WasmMemory memory = createTestMemory(1);
      assertEquals(-1L, memory.grow64((long) Integer.MAX_VALUE + 1));
    }
  }

  @Nested
  @DisplayName("64-bit Read/Write Byte Methods")
  class ReadWrite64BitTests {

    @Test
    @DisplayName("readByte64() should delegate to readByte() for small offsets")
    void readByte64ShouldDelegateForSmallOffsets() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeByte(10, (byte) 42);
      assertEquals((byte) 42, memory.readByte64(10L));
    }

    @Test
    @DisplayName("writeByte64() should delegate to writeByte() for small offsets")
    void writeByte64ShouldDelegateForSmallOffsets() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeByte64(10L, (byte) 99);
      assertEquals((byte) 99, memory.readByte(10));
    }

    @Test
    @DisplayName("readByte64() should throw for negative offset")
    void readByte64ShouldThrowForNegativeOffset() {
      final WasmMemory memory = createTestMemory(1);
      assertThrows(IndexOutOfBoundsException.class, () -> memory.readByte64(-1L));
    }

    @Test
    @DisplayName("writeByte64() should throw for negative offset")
    void writeByte64ShouldThrowForNegativeOffset() {
      final WasmMemory memory = createTestMemory(1);
      assertThrows(IndexOutOfBoundsException.class, () -> memory.writeByte64(-1L, (byte) 0));
    }

    @Test
    @DisplayName("readByte64() should throw for offset exceeding 32-bit limit")
    void readByte64ShouldThrowForLargeOffset() {
      final WasmMemory memory = createTestMemory(1);
      assertThrows(
          IndexOutOfBoundsException.class, () -> memory.readByte64((long) Integer.MAX_VALUE + 1));
    }

    @Test
    @DisplayName("writeByte64() should throw for offset exceeding 32-bit limit")
    void writeByte64ShouldThrowForLargeOffset() {
      final WasmMemory memory = createTestMemory(1);
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.writeByte64((long) Integer.MAX_VALUE + 1, (byte) 0));
    }
  }

  @Nested
  @DisplayName("64-bit Bulk Read/Write Methods")
  class BulkReadWrite64BitTests {

    @Test
    @DisplayName("readBytes64() should delegate to readBytes() for small offsets")
    void readBytes64ShouldDelegateForSmallOffsets() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeByte(0, (byte) 1);
      memory.writeByte(1, (byte) 2);
      memory.writeByte(2, (byte) 3);
      final byte[] dest = new byte[3];
      memory.readBytes64(0L, dest, 0, 3);
      assertArrayEquals(new byte[] {1, 2, 3}, dest);
    }

    @Test
    @DisplayName("writeBytes64() should delegate to writeBytes() for small offsets")
    void writeBytes64ShouldDelegateForSmallOffsets() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeBytes64(0L, new byte[] {10, 20, 30}, 0, 3);
      assertEquals((byte) 10, memory.readByte(0));
      assertEquals((byte) 20, memory.readByte(1));
      assertEquals((byte) 30, memory.readByte(2));
    }

    @Test
    @DisplayName("readBytes64() should throw for negative offset")
    void readBytes64ShouldThrowForNegativeOffset() {
      final WasmMemory memory = createTestMemory(1);
      assertThrows(
          IndexOutOfBoundsException.class, () -> memory.readBytes64(-1L, new byte[1], 0, 1));
    }

    @Test
    @DisplayName("writeBytes64() should throw for negative offset")
    void writeBytes64ShouldThrowForNegativeOffset() {
      final WasmMemory memory = createTestMemory(1);
      assertThrows(
          IndexOutOfBoundsException.class, () -> memory.writeBytes64(-1L, new byte[1], 0, 1));
    }

    @Test
    @DisplayName("readBytes64() should throw for offset exceeding 32-bit limit")
    void readBytes64ShouldThrowForLargeOffset() {
      final WasmMemory memory = createTestMemory(1);
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.readBytes64((long) Integer.MAX_VALUE + 1, new byte[1], 0, 1));
    }

    @Test
    @DisplayName("writeBytes64() should throw for offset exceeding 32-bit limit")
    void writeBytes64ShouldThrowForLargeOffset() {
      final WasmMemory memory = createTestMemory(1);
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.writeBytes64((long) Integer.MAX_VALUE + 1, new byte[1], 0, 1));
    }
  }

  @Nested
  @DisplayName("64-bit Copy/Fill/Init Defaults")
  class CopyFillInit64Tests {

    @Test
    @DisplayName("copy64() should delegate to copy(int) for small values")
    void copy64ShouldDelegateForSmallValues() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeByte(0, (byte) 1);
      memory.writeByte(1, (byte) 2);
      memory.copy64(10L, 0L, 2L);
      assertEquals((byte) 1, memory.readByte(10));
      assertEquals((byte) 2, memory.readByte(11));
    }

    @Test
    @DisplayName("copy64() should throw for values exceeding 32-bit limit")
    void copy64ShouldThrowForLargeValues() {
      final WasmMemory memory = createTestMemory(1);
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.copy64((long) Integer.MAX_VALUE + 1, 0L, 1L));
    }

    @Test
    @DisplayName("fill64() should delegate to fill(int) for small values")
    void fill64ShouldDelegateForSmallValues() {
      final WasmMemory memory = createTestMemory(1);
      memory.fill64(0L, (byte) 0xAB, 5L);
      for (int i = 0; i < 5; i++) {
        assertEquals((byte) 0xAB, memory.readByte(i));
      }
    }

    @Test
    @DisplayName("fill64() should throw for values exceeding 32-bit limit")
    void fill64ShouldThrowForLargeValues() {
      final WasmMemory memory = createTestMemory(1);
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.fill64((long) Integer.MAX_VALUE + 1, (byte) 0, 1L));
    }

    @Test
    @DisplayName("init64() should throw for values exceeding 32-bit limit")
    void init64ShouldThrowForLargeValues() {
      final WasmMemory memory = createTestMemory(1);
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.init64((long) Integer.MAX_VALUE + 1, 0, 0L, 1L));
    }
  }

  @Nested
  @DisplayName("Size Calculation Defaults")
  class SizeCalculationTests {

    @Test
    @DisplayName("getSizeInBytes64() should compute pages * 65536")
    void getSizeInBytes64ShouldComputeCorrectly() {
      final WasmMemory memory = createTestMemory(3);
      assertEquals(3L * 65536L, memory.getSizeInBytes64());
    }

    @Test
    @DisplayName("getMaxSizeInBytes64() should compute maxPages * 65536")
    void getMaxSizeInBytes64ShouldComputeCorrectly() {
      final WasmMemory memory = createTestMemory(1);
      assertEquals(100L * 65536L, memory.getMaxSizeInBytes64());
    }

    @Test
    @DisplayName("getMaxSizeInBytes64() should return -1 when unlimited")
    void getMaxSizeInBytes64ShouldReturnNegativeOneWhenUnlimited() {
      final WasmMemory memory =
          new WasmMemory() {
            @Override
            public int getSize() {
              return 1;
            }

            @Override
            public int grow(final int pages) {
              return -1;
            }

            @Override
            public int getMaxSize() {
              return -1;
            }

            @Override
            public MemoryType getMemoryType() {
              return null;
            }

            @Override
            public ByteBuffer getBuffer() {
              return null;
            }

            @Override
            public byte readByte(final int offset) {
              return 0;
            }

            @Override
            public void writeByte(final int offset, final byte value) {}

            @Override
            public void readBytes(int offset, byte[] dest, int destOffset, int length) {}

            @Override
            public void writeBytes(int offset, byte[] src, int srcOffset, int length) {}

            @Override
            public void copy(int destOffset, int srcOffset, int length) {}

            @Override
            public void fill(int offset, byte value, int length) {}

            @Override
            public void init(int destOffset, int dataSegIdx, int srcOffset, int length) {}

            @Override
            public void dropDataSegment(int dataSegmentIndex) {}

            @Override
            public boolean isShared() {
              return false;
            }

            @Override
            public int atomicCompareAndSwapInt(int offset, int expected, int newValue) {
              return 0;
            }

            @Override
            public long atomicCompareAndSwapLong(int offset, long expected, long newValue) {
              return 0;
            }

            @Override
            public int atomicLoadInt(int offset) {
              return 0;
            }

            @Override
            public long atomicLoadLong(int offset) {
              return 0;
            }

            @Override
            public void atomicStoreInt(int offset, int value) {}

            @Override
            public void atomicStoreLong(int offset, long value) {}

            @Override
            public int atomicAddInt(int offset, int value) {
              return 0;
            }

            @Override
            public long atomicAddLong(int offset, long value) {
              return 0;
            }

            @Override
            public int atomicAndInt(int offset, int value) {
              return 0;
            }

            @Override
            public int atomicOrInt(int offset, int value) {
              return 0;
            }

            @Override
            public int atomicXorInt(int offset, int value) {
              return 0;
            }

            @Override
            public long atomicAndLong(int offset, long value) {
              return 0;
            }

            @Override
            public long atomicOrLong(int offset, long value) {
              return 0;
            }

            @Override
            public long atomicXorLong(int offset, long value) {
              return 0;
            }

            @Override
            public void atomicFence() {}

            @Override
            public int atomicNotify(int offset, int count) {
              return 0;
            }

            @Override
            public WaitResult atomicWait32(int offset, int expected, long timeoutNanos) {
              return null;
            }

            @Override
            public WaitResult atomicWait64(int offset, long expected, long timeoutNanos) {
              return null;
            }

            @Override
            public long growAsync(long pages) {
              return 0;
            }
          };
      assertEquals(-1L, memory.getMaxSizeInBytes64());
    }
  }

  @Nested
  @DisplayName("Typed Read/Write Defaults (Little-Endian)")
  class TypedReadWriteTests {

    @Test
    @DisplayName("writeInt32/readInt32 should round-trip correctly")
    void writeAndReadInt32() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeInt32(0L, 0x04030201);
      assertEquals(0x04030201, memory.readInt32(0L));
    }

    @Test
    @DisplayName("writeInt64/readInt64 should round-trip correctly")
    void writeAndReadInt64() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeInt64(0L, 0x0807060504030201L);
      assertEquals(0x0807060504030201L, memory.readInt64(0L));
    }

    @Test
    @DisplayName("writeFloat32/readFloat32 should round-trip correctly")
    void writeAndReadFloat32() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeFloat32(0L, 3.14f);
      assertEquals(3.14f, memory.readFloat32(0L), 0.0001f);
    }

    @Test
    @DisplayName("writeFloat64/readFloat64 should round-trip correctly")
    void writeAndReadFloat64() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeFloat64(0L, 2.718281828);
      assertEquals(2.718281828, memory.readFloat64(0L), 0.0000001);
    }

    @Test
    @DisplayName("readInt32 should use little-endian byte order")
    void readInt32ShouldUseLittleEndian() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeByte(0, (byte) 0x01);
      memory.writeByte(1, (byte) 0x02);
      memory.writeByte(2, (byte) 0x03);
      memory.writeByte(3, (byte) 0x04);
      assertEquals(0x04030201, memory.readInt32(0L));
    }

    @Test
    @DisplayName("writeInt32 should write little-endian bytes")
    void writeInt32ShouldWriteLittleEndian() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeInt32(0L, 0x04030201);
      assertEquals((byte) 0x01, memory.readByte(0));
      assertEquals((byte) 0x02, memory.readByte(1));
      assertEquals((byte) 0x03, memory.readByte(2));
      assertEquals((byte) 0x04, memory.readByte(3));
    }

    @Test
    @DisplayName("writeInt32 with negative value should round-trip")
    void writeInt32NegativeValue() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeInt32(0L, -1);
      assertEquals(-1, memory.readInt32(0L));
    }

    @Test
    @DisplayName("writeInt64 with negative value should round-trip")
    void writeInt64NegativeValue() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeInt64(0L, -1L);
      assertEquals(-1L, memory.readInt64(0L));
    }
  }

  @Nested
  @DisplayName("Direct Memory Access Defaults")
  class DirectMemoryAccessTests {

    @Test
    @DisplayName("dataPtr() should return 0 by default")
    void dataPtrShouldReturnZeroByDefault() {
      assertEquals(0L, createTestMemory(1).dataPtr());
    }

    @Test
    @DisplayName("dataSize() should compute getSize() * 65536")
    void dataSizeShouldComputeCorrectly() {
      final WasmMemory memory = createTestMemory(4);
      assertEquals(4L * 65536L, memory.dataSize());
    }

    @Test
    @DisplayName("pageSize() should return 65536 by default")
    void pageSizeShouldReturnDefault() {
      assertEquals(65536, createTestMemory(1).pageSize());
    }

    @Test
    @DisplayName("pageSizeLog2() should return 16 by default")
    void pageSizeLog2ShouldReturnDefault() {
      assertEquals(16, createTestMemory(1).pageSizeLog2());
    }

    @Test
    @DisplayName("getMinSize() should derive from MemoryType minimum")
    void getMinSizeShouldDeriveFromMemoryType() {
      final WasmMemory memory = createTestMemory(5);
      assertEquals(5, memory.getMinSize());
    }
  }

  @Nested
  @DisplayName("Long-addressed copy() and fill() Defaults")
  class LongCopyFillTests {

    @Test
    @DisplayName("copy(long,long,long) with zero length should be no-op")
    void copyLongZeroLengthShouldBeNoOp() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeByte(0, (byte) 42);
      memory.copy(0L, 5L, 0L); // zero length
      assertEquals((byte) 42, memory.readByte(0));
    }

    @Test
    @DisplayName("copy(long,long,long) with same src and dest should be no-op")
    void copyLongSameSrcDestShouldBeNoOp() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeByte(0, (byte) 42);
      memory.copy(0L, 0L, 10L); // same offset
      assertEquals((byte) 42, memory.readByte(0));
    }

    @Test
    @DisplayName("copy(long,long,long) forward should work")
    void copyLongForwardShouldWork() {
      final WasmMemory memory = createTestMemory(1);
      memory.writeByte(0, (byte) 1);
      memory.writeByte(1, (byte) 2);
      memory.writeByte(2, (byte) 3);
      memory.copy(100L, 0L, 3L);
      assertEquals((byte) 1, memory.readByte(100));
      assertEquals((byte) 2, memory.readByte(101));
      assertEquals((byte) 3, memory.readByte(102));
    }

    @Test
    @DisplayName("fill(long,byte,long) should fill correctly")
    void fillLongShouldFillCorrectly() {
      final WasmMemory memory = createTestMemory(1);
      memory.fill(0L, (byte) 0xFF, 10L);
      for (int i = 0; i < 10; i++) {
        assertEquals((byte) 0xFF, memory.readByte(i));
      }
      assertEquals((byte) 0, memory.readByte(10));
    }
  }
}

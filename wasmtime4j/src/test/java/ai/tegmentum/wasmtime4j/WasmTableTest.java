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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmTable} default methods.
 *
 * <p>These tests use anonymous implementations to exercise default method behavior without
 * requiring a native runtime.
 */
@DisplayName("WasmTable Default Method Tests")
class WasmTableTest {

  /** A simple in-memory WasmTable implementation backed by an array. */
  private WasmTable createTestTable(final int initialSize) {
    final Object[] elements = new Object[initialSize + 100]; // room to grow

    return new WasmTable() {
      private int currentSize = initialSize;

      @Override
      public int getSize() {
        return currentSize;
      }

      @Override
      public WasmValueType getType() {
        return WasmValueType.FUNCREF;
      }

      @Override
      public TableType getTableType() {
        return new TableType() {
          @Override
          public WasmValueType getElementType() {
            return WasmValueType.FUNCREF;
          }

          @Override
          public long getMinimum() {
            return initialSize;
          }

          @Override
          public Optional<Long> getMaximum() {
            return Optional.of(200L);
          }

          @Override
          public WasmTypeKind getKind() {
            return WasmTypeKind.TABLE;
          }
        };
      }

      @Override
      public int grow(final int additionalElements, final Object initValue) {
        int prev = currentSize;
        for (int i = currentSize; i < currentSize + additionalElements; i++) {
          elements[i] = initValue;
        }
        currentSize += additionalElements;
        return prev;
      }

      @Override
      public int getMaxSize() {
        return 200;
      }

      @Override
      public Object get(final int index) {
        if (index >= currentSize) {
          throw new IndexOutOfBoundsException("Index: " + index);
        }
        return elements[index];
      }

      @Override
      public void set(final int index, final Object value) {
        if (index >= currentSize) {
          throw new IndexOutOfBoundsException("Index: " + index);
        }
        elements[index] = value;
      }

      @Override
      public WasmValueType getElementType() {
        return WasmValueType.FUNCREF;
      }

      @Override
      public void fill(final int start, final int count, final Object value) {
        for (int i = start; i < start + count; i++) {
          elements[i] = value;
        }
      }

      @Override
      public void copy(final int dst, final int src, final int count) {
        System.arraycopy(elements, src, elements, dst, count);
      }

      @Override
      public void copy(final int dst, final WasmTable src, final int srcIndex, final int count) {
        for (int i = 0; i < count; i++) {
          elements[dst + i] = src.get(srcIndex + i);
        }
      }

      @Override
      public void init(int destOffset, int elemSegIdx, int srcOffset, int length) {
        throw new UnsupportedOperationException("Not implemented for test");
      }

      @Override
      public void dropElementSegment(int elemSegIdx) {
        throw new UnsupportedOperationException("Not implemented for test");
      }

      @Override
      public int growAsync(int elements, Object initValue) throws WasmException {
        throw new UnsupportedOperationException("Not implemented for test");
      }
    };
  }

  @Nested
  @DisplayName("Alias Default Methods")
  class AliasTests {

    @Test
    @DisplayName("size() should delegate to getSize()")
    void sizeShouldDelegateToGetSize() {
      final WasmTable table = createTestTable(10);
      assertEquals(10, table.size());
      assertEquals(table.getSize(), table.size());
    }
  }

  @Nested
  @DisplayName("64-bit Addressing Defaults")
  class Addressing64BitTests {

    @Test
    @DisplayName("supports64BitAddressing() should return false by default")
    void supports64BitAddressingShouldReturnFalseByDefault() {
      assertFalse(createTestTable(5).supports64BitAddressing());
    }

    @Test
    @DisplayName("getSize64() should throw when 64-bit not supported")
    void getSize64ShouldThrowWhenNotSupported() {
      final WasmTable table = createTestTable(5);
      assertThrows(UnsupportedOperationException.class, table::getSize64);
    }

    @Test
    @DisplayName("get64() should throw when 64-bit not supported")
    void get64ShouldThrowWhenNotSupported() {
      final WasmTable table = createTestTable(5);
      assertThrows(UnsupportedOperationException.class, () -> table.get64(0L));
    }

    @Test
    @DisplayName("set64() should throw when 64-bit not supported")
    void set64ShouldThrowWhenNotSupported() {
      final WasmTable table = createTestTable(5);
      assertThrows(
          UnsupportedOperationException.class, () -> table.set64(0L, WasmValue.externref(null)));
    }

    @Test
    @DisplayName("grow64() should throw when 64-bit not supported")
    void grow64ShouldThrowWhenNotSupported() {
      final WasmTable table = createTestTable(5);
      assertThrows(
          UnsupportedOperationException.class, () -> table.grow64(1L, WasmValue.externref(null)));
    }

    @Test
    @DisplayName("fill64() should throw when 64-bit not supported")
    void fill64ShouldThrowWhenNotSupported() {
      final WasmTable table = createTestTable(5);
      assertThrows(
          UnsupportedOperationException.class,
          () -> table.fill64(0L, 5L, WasmValue.externref(null)));
    }

    @Test
    @DisplayName("copy64() should throw when 64-bit not supported")
    void copy64ShouldThrowWhenNotSupported() {
      final WasmTable table = createTestTable(5);
      assertThrows(UnsupportedOperationException.class, () -> table.copy64(0L, table, 0L, 1L));
    }

    @Test
    @DisplayName("init64() should throw when 64-bit not supported")
    void init64ShouldThrowWhenNotSupported() {
      final WasmTable table = createTestTable(5);
      assertThrows(UnsupportedOperationException.class, () -> table.init64(0L, 0, 0L, 1L));
    }
  }

  @Nested
  @DisplayName("64-bit Methods With Support Enabled")
  class Addressing64BitEnabledTests {

    private WasmTable createTable64() {
      final WasmTable base = createTestTable(10);
      return new WasmTable() {
        @Override
        public boolean supports64BitAddressing() {
          return true;
        }

        @Override
        public int getSize() {
          return base.getSize();
        }

        @Override
        public WasmValueType getType() {
          return base.getType();
        }

        @Override
        public TableType getTableType() {
          return base.getTableType();
        }

        @Override
        public int grow(int elements, Object initValue) {
          return base.grow(elements, initValue);
        }

        @Override
        public int getMaxSize() {
          return base.getMaxSize();
        }

        @Override
        public Object get(int index) {
          return base.get(index);
        }

        @Override
        public void set(int index, Object value) {
          base.set(index, value);
        }

        @Override
        public WasmValueType getElementType() {
          return base.getElementType();
        }

        @Override
        public void fill(int start, int count, Object value) {
          base.fill(start, count, value);
        }

        @Override
        public void copy(int dst, int src, int count) {
          base.copy(dst, src, count);
        }

        @Override
        public void copy(int dst, WasmTable src, int srcIndex, int count) {
          base.copy(dst, src, srcIndex, count);
        }

        @Override
        public void init(int destOffset, int elemSegIdx, int srcOffset, int length) {
          base.init(destOffset, elemSegIdx, srcOffset, length);
        }

        @Override
        public void dropElementSegment(int elemSegIdx) {
          base.dropElementSegment(elemSegIdx);
        }

        @Override
        public int growAsync(int elements, Object initValue) throws WasmException {
          return base.growAsync(elements, initValue);
        }
      };
    }

    @Test
    @DisplayName("getSize64() should return size when 64-bit supported")
    void getSize64ShouldReturnSizeWhenSupported() {
      assertEquals(10L, createTable64().getSize64());
    }

    @Test
    @DisplayName("get64() should delegate to get() for small indices")
    void get64ShouldDelegateForSmallIndices() {
      final WasmTable table = createTable64();
      table.set(0, "hello");
      final WasmValue val = table.get64(0L);
      assertEquals("hello", val.getValue());
    }

    @Test
    @DisplayName("get64() should throw for index exceeding 32-bit limit")
    void get64ShouldThrowForLargeIndex() {
      final WasmTable table = createTable64();
      assertThrows(
          IndexOutOfBoundsException.class, () -> table.get64((long) Integer.MAX_VALUE + 1));
    }

    @Test
    @DisplayName("grow64() should delegate to grow() for small values")
    void grow64ShouldDelegateForSmallValues() {
      final WasmTable table = createTable64();
      final long prev = table.grow64(5L, WasmValue.externref(null));
      assertEquals(10L, prev);
    }

    @Test
    @DisplayName("grow64() should throw for element count exceeding 32-bit limit")
    void grow64ShouldThrowForLargeElementCount() {
      final WasmTable table = createTable64();
      assertThrows(
          IllegalArgumentException.class,
          () -> table.grow64((long) Integer.MAX_VALUE + 1, WasmValue.externref(null)));
    }

    @Test
    @DisplayName("fill64() should delegate to fill() for small values")
    void fill64ShouldDelegateForSmallValues() {
      final WasmTable table = createTable64();
      table.fill64(0L, 3L, WasmValue.externref("filled"));
      assertEquals("filled", table.get(0));
      assertEquals("filled", table.get(1));
      assertEquals("filled", table.get(2));
    }

    @Test
    @DisplayName("fill64() should throw for parameters exceeding 32-bit limits")
    void fill64ShouldThrowForLargeParams() {
      final WasmTable table = createTable64();
      assertThrows(
          IllegalArgumentException.class,
          () -> table.fill64((long) Integer.MAX_VALUE + 1, 1L, WasmValue.externref(null)));
    }

    @Test
    @DisplayName("copy64() should throw for parameters exceeding 32-bit limits")
    void copy64ShouldThrowForLargeParams() {
      final WasmTable table = createTable64();
      assertThrows(
          IllegalArgumentException.class,
          () -> table.copy64(0L, table, 0L, (long) Integer.MAX_VALUE + 1));
    }

    @Test
    @DisplayName("init64() should throw for parameters exceeding 32-bit limits")
    void init64ShouldThrowForLargeParams() {
      final WasmTable table = createTable64();
      assertThrows(
          IllegalArgumentException.class,
          () -> table.init64(0L, 0, (long) Integer.MAX_VALUE + 1, 1L));
    }
  }
}

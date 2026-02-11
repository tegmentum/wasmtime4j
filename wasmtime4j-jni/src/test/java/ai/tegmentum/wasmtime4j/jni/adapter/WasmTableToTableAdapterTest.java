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

package ai.tegmentum.wasmtime4j.jni.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.memory.Table;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasmTableToTableAdapter} class.
 *
 * <p>This test class verifies the adapter that bridges WasmTable to Table interface.
 */
@DisplayName("WasmTableToTableAdapter Tests")
class WasmTableToTableAdapterTest {

  /** Creates a mock WasmTable for testing. */
  private WasmTable createMockTable(
      final WasmValueType elementType, final int initialSize, final int maxSize) {
    return new TestWasmTable(elementType, initialSize, maxSize);
  }

  /** Test implementation of WasmTable backed by an array. */
  private static class TestWasmTable implements WasmTable {
    private Object[] elements;
    private int size;
    private final int maxElements;
    private final WasmValueType elemType;
    private final int initSize;

    TestWasmTable(final WasmValueType elementType, final int initialSize, final int maxSize) {
      this.elemType = elementType;
      this.initSize = initialSize;
      this.elements = new Object[initialSize];
      this.size = initialSize;
      this.maxElements = maxSize;
    }

    @Override
    public int getSize() {
      return size;
    }

    @Override
    public int getMaxSize() {
      return maxElements;
    }

    @Override
    public WasmValueType getType() {
      return elemType;
    }

    @Override
    public int grow(final int deltaElements, final Object initValue) {
      if (size + deltaElements > maxElements) {
        return -1;
      }
      final int oldSize = size;
      final Object[] newElements = new Object[size + deltaElements];
      System.arraycopy(elements, 0, newElements, 0, size);
      for (int i = size; i < size + deltaElements; i++) {
        newElements[i] = initValue;
      }
      elements = newElements;
      size += deltaElements;
      return oldSize;
    }

    @Override
    public Object get(final int index) {
      if (index < 0 || index >= size) {
        throw new IndexOutOfBoundsException("Index out of bounds: " + index);
      }
      return elements[index];
    }

    @Override
    public void set(final int index, final Object value) {
      if (index < 0 || index >= size) {
        throw new IndexOutOfBoundsException("Index out of bounds: " + index);
      }
      elements[index] = value;
    }

    @Override
    public WasmValueType getElementType() {
      return elemType;
    }

    @Override
    public void fill(final int start, final int count, final Object value) {
      if (start < 0 || start + count > size) {
        throw new IndexOutOfBoundsException("Fill operation out of bounds");
      }
      for (int i = start; i < start + count; i++) {
        elements[i] = value;
      }
    }

    @Override
    public void copy(final int dst, final int src, final int count) {
      if (dst < 0 || src < 0 || dst + count > size || src + count > size) {
        throw new IndexOutOfBoundsException("Copy operation out of bounds");
      }
      System.arraycopy(elements, src, elements, dst, count);
    }

    @Override
    public void copy(final int dst, final WasmTable srcTable, final int srcIndex, final int count) {
      if (dst < 0 || srcIndex < 0 || dst + count > size) {
        throw new IndexOutOfBoundsException("Cross-table copy out of bounds");
      }
      for (int i = 0; i < count; i++) {
        elements[dst + i] = srcTable.get(srcIndex + i);
      }
    }

    @Override
    public void init(
        final int destOffset,
        final int elementSegmentIndex,
        final int srcOffset,
        final int length) {
      throw new UnsupportedOperationException("init not supported in test");
    }

    @Override
    public void dropElementSegment(final int elementSegmentIndex) {
      throw new UnsupportedOperationException("dropElementSegment not supported in test");
    }

    @Override
    public TableType getTableType() {
      final WasmValueType elementType = elemType;
      final long minimum = initSize;
      final long maximum = maxElements;
      return new TableType() {
        @Override
        public WasmValueType getElementType() {
          return elementType;
        }

        @Override
        public long getMinimum() {
          return minimum;
        }

        @Override
        public java.util.Optional<Long> getMaximum() {
          return java.util.Optional.of(maximum);
        }
      };
    }
  }

  /** Creates a mock WasmTable that throws on getSize(). */
  private WasmTable createFailingTable() {
    return new FailingWasmTable();
  }

  /** Test implementation of WasmTable that throws on all operations. */
  private static class FailingWasmTable implements WasmTable {

    @Override
    public int getSize() {
      throw new RuntimeException("Table access failed");
    }

    @Override
    public int getMaxSize() {
      return 0;
    }

    @Override
    public WasmValueType getType() {
      return WasmValueType.FUNCREF;
    }

    @Override
    public int grow(final int deltaElements, final Object initValue) {
      return -1;
    }

    @Override
    public Object get(final int index) {
      throw new RuntimeException("Table access failed");
    }

    @Override
    public void set(final int index, final Object value) {
      throw new RuntimeException("Table access failed");
    }

    @Override
    public WasmValueType getElementType() {
      return WasmValueType.FUNCREF;
    }

    @Override
    public void fill(final int start, final int count, final Object value) {
      throw new RuntimeException("Table access failed");
    }

    @Override
    public void copy(final int dst, final int src, final int count) {
      throw new RuntimeException("Table access failed");
    }

    @Override
    public void copy(final int dst, final WasmTable srcTable, final int srcIndex, final int count) {
      throw new RuntimeException("Table access failed");
    }

    @Override
    public void init(
        final int destOffset,
        final int elementSegmentIndex,
        final int srcOffset,
        final int length) {
      throw new RuntimeException("Table access failed");
    }

    @Override
    public void dropElementSegment(final int elementSegmentIndex) {
      throw new RuntimeException("Table access failed");
    }

    @Override
    public TableType getTableType() {
      return null;
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasmTableToTableAdapter should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasmTableToTableAdapter.class.getModifiers()),
          "WasmTableToTableAdapter should be final");
    }

    @Test
    @DisplayName("WasmTableToTableAdapter should implement Table")
    void shouldImplementTable() {
      assertTrue(
          Table.class.isAssignableFrom(WasmTableToTableAdapter.class),
          "WasmTableToTableAdapter should implement Table");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid delegate")
    void constructorShouldAcceptValidDelegate() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);

      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertNotNull(adapter, "Adapter should not be null");
      assertSame(delegate, adapter.getDelegate(), "Delegate should match");
    }

    @Test
    @DisplayName("Constructor should throw on null delegate")
    void constructorShouldThrowOnNullDelegate() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasmTableToTableAdapter(null),
          "Should throw on null delegate");
    }
  }

  @Nested
  @DisplayName("getSize Tests")
  class GetSizeTests {

    @Test
    @DisplayName("getSize should return element count")
    void getSizeShouldReturnElementCount() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 25, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertEquals(25, adapter.getSize(), "Size should be 25 elements");
    }

    @Test
    @DisplayName("getSize should return 0 for empty table")
    void getSizeShouldReturnZeroForEmptyTable() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 0, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertEquals(0, adapter.getSize(), "Size should be 0");
    }
  }

  @Nested
  @DisplayName("grow Tests")
  class GrowTests {

    @Test
    @DisplayName("grow should return old size on success")
    void growShouldReturnOldSizeOnSuccess() throws WasmException {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      final long result = adapter.grow(5, null);

      assertEquals(10, result, "Should return old size");
      assertEquals(15, adapter.getSize(), "New size should be 15");
    }

    @Test
    @DisplayName("grow should initialize new elements with init value")
    void growShouldInitializeNewElementsWithInitValue() throws WasmException {
      final WasmTable delegate = createMockTable(WasmValueType.EXTERNREF, 2, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);
      final String initValue = "initialized";

      adapter.grow(3, initValue);

      assertEquals(initValue, adapter.get(2), "New element at index 2 should be initialized");
      assertEquals(initValue, adapter.get(3), "New element at index 3 should be initialized");
      assertEquals(initValue, adapter.get(4), "New element at index 4 should be initialized");
    }

    @Test
    @DisplayName("grow should fail when exceeding max size")
    void growShouldFailWhenExceedingMaxSize() throws WasmException {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 50, 60);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      final long result = adapter.grow(20, null);

      assertEquals(-1, result, "Should return -1 on failure");
      assertEquals(50, adapter.getSize(), "Size should remain unchanged");
    }

    @Test
    @DisplayName("grow should throw on excessively large delta")
    void growShouldThrowOnExcessivelyLargeDelta() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          WasmException.class,
          () -> adapter.grow((long) Integer.MAX_VALUE + 1, null),
          "Should throw for delta exceeding Integer.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("get Tests")
  class GetTests {

    @Test
    @DisplayName("get should return element at index")
    void getShouldReturnElementAtIndex() throws WasmException {
      final WasmTable delegate = createMockTable(WasmValueType.EXTERNREF, 10, 100);
      delegate.set(5, "test-value");
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertEquals("test-value", adapter.get(5), "Should return element at index 5");
    }

    @Test
    @DisplayName("get should return null for uninitialized element")
    void getShouldReturnNullForUninitializedElement() throws WasmException {
      final WasmTable delegate = createMockTable(WasmValueType.EXTERNREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertNull(adapter.get(0), "Should return null for uninitialized element");
    }

    @Test
    @DisplayName("get should throw on negative index")
    void getShouldThrowOnNegativeIndex() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          IllegalArgumentException.class, () -> adapter.get(-1), "Should throw on negative index");
    }

    @Test
    @DisplayName("get should throw on out of bounds index")
    void getShouldThrowOnOutOfBoundsIndex() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          WasmException.class, () -> adapter.get(10), "Should throw on out of bounds index");
    }

    @Test
    @DisplayName("get should throw on excessively large index")
    void getShouldThrowOnExcessivelyLargeIndex() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          WasmException.class,
          () -> adapter.get((long) Integer.MAX_VALUE + 1),
          "Should throw for index exceeding Integer.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("set Tests")
  class SetTests {

    @Test
    @DisplayName("set should store element at index")
    void setShouldStoreElementAtIndex() throws WasmException {
      final WasmTable delegate = createMockTable(WasmValueType.EXTERNREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      adapter.set(3, "stored-value");

      assertEquals("stored-value", adapter.get(3), "Should store and retrieve element");
    }

    @Test
    @DisplayName("set should throw on negative index")
    void setShouldThrowOnNegativeIndex() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          IllegalArgumentException.class,
          () -> adapter.set(-1, "value"),
          "Should throw on negative index");
    }

    @Test
    @DisplayName("set should throw on out of bounds index")
    void setShouldThrowOnOutOfBoundsIndex() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          WasmException.class,
          () -> adapter.set(10, "value"),
          "Should throw on out of bounds index");
    }
  }

  @Nested
  @DisplayName("getElementType Tests")
  class GetElementTypeTests {

    @Test
    @DisplayName("getElementType should return FUNCREF for FUNCREF table")
    void getElementTypeShouldReturnFuncrefForFuncrefTable() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertEquals(
          Table.TableElementType.FUNCREF,
          adapter.getElementType(),
          "Element type should be FUNCREF");
    }

    @Test
    @DisplayName("getElementType should return EXTERNREF for EXTERNREF table")
    void getElementTypeShouldReturnExternrefForExternrefTable() {
      final WasmTable delegate = createMockTable(WasmValueType.EXTERNREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertEquals(
          Table.TableElementType.EXTERNREF,
          adapter.getElementType(),
          "Element type should be EXTERNREF");
    }

    @Test
    @DisplayName("getElementType should default to EXTERNREF for null type")
    void getElementTypeShouldDefaultToExternrefForNullType() {
      final WasmTable delegate = createMockTable(null, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertEquals(
          Table.TableElementType.EXTERNREF,
          adapter.getElementType(),
          "Element type should default to EXTERNREF");
    }

    @Test
    @DisplayName("getElementType should default to EXTERNREF for I32 type")
    void getElementTypeShouldDefaultToExternrefForI32Type() {
      final WasmTable delegate = createMockTable(WasmValueType.I32, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertEquals(
          Table.TableElementType.EXTERNREF,
          adapter.getElementType(),
          "Element type should default to EXTERNREF for non-ref types");
    }
  }

  @Nested
  @DisplayName("getMaxSize Tests")
  class GetMaxSizeTests {

    @Test
    @DisplayName("getMaxSize should return correct value")
    void getMaxSizeShouldReturnCorrectValue() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 500);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertEquals(500, adapter.getMaxSize(), "Max size should be 500");
    }
  }

  @Nested
  @DisplayName("isValid Tests")
  class IsValidTests {

    @Test
    @DisplayName("isValid should return true for valid table")
    void isValidShouldReturnTrueForValidTable() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertTrue(adapter.isValid(), "Should be valid");
    }

    @Test
    @DisplayName("isValid should return false when delegate throws")
    void isValidShouldReturnFalseWhenDelegateThrows() {
      final WasmTable delegate = createFailingTable();
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertFalse(adapter.isValid(), "Should be invalid when delegate throws");
    }
  }

  @Nested
  @DisplayName("fill Tests")
  class FillTests {

    @Test
    @DisplayName("fill should set elements in range")
    void fillShouldSetElementsInRange() throws WasmException {
      final WasmTable delegate = createMockTable(WasmValueType.EXTERNREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      adapter.fill(2, "fill-value", 5);

      for (int i = 2; i < 7; i++) {
        assertEquals("fill-value", adapter.get(i), "Element at index " + i + " should be filled");
      }
    }

    @Test
    @DisplayName("fill should throw on negative dstIndex")
    void fillShouldThrowOnNegativeDstIndex() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          IllegalArgumentException.class,
          () -> adapter.fill(-1, null, 5),
          "Should throw on negative dstIndex");
    }

    @Test
    @DisplayName("fill should throw on negative length")
    void fillShouldThrowOnNegativeLength() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          IllegalArgumentException.class,
          () -> adapter.fill(0, null, -1),
          "Should throw on negative length");
    }

    @Test
    @DisplayName("fill should throw on out of bounds")
    void fillShouldThrowOnOutOfBounds() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          WasmException.class,
          () -> adapter.fill(8, null, 5),
          "Should throw on out of bounds fill");
    }
  }

  @Nested
  @DisplayName("copy Tests")
  class CopyTests {

    @Test
    @DisplayName("copy should copy elements within same table")
    void copyShouldCopyElementsWithinSameTable() throws WasmException {
      final WasmTable delegate = createMockTable(WasmValueType.EXTERNREF, 10, 100);
      delegate.set(0, "A");
      delegate.set(1, "B");
      delegate.set(2, "C");
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      adapter.copy(5, adapter, 0, 3);

      assertEquals("A", adapter.get(5), "Element at index 5 should be A");
      assertEquals("B", adapter.get(6), "Element at index 6 should be B");
      assertEquals("C", adapter.get(7), "Element at index 7 should be C");
    }

    @Test
    @DisplayName("copy should throw on null srcTable")
    void copyShouldThrowOnNullSrcTable() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          IllegalArgumentException.class,
          () -> adapter.copy(0, null, 0, 5),
          "Should throw on null srcTable");
    }

    @Test
    @DisplayName("copy should throw on negative dstIndex")
    void copyShouldThrowOnNegativeDstIndex() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          IllegalArgumentException.class,
          () -> adapter.copy(-1, adapter, 0, 5),
          "Should throw on negative dstIndex");
    }

    @Test
    @DisplayName("copy should throw on negative srcIndex")
    void copyShouldThrowOnNegativeSrcIndex() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          IllegalArgumentException.class,
          () -> adapter.copy(0, adapter, -1, 5),
          "Should throw on negative srcIndex");
    }

    @Test
    @DisplayName("copy should throw on negative length")
    void copyShouldThrowOnNegativeLength() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertThrows(
          IllegalArgumentException.class,
          () -> adapter.copy(0, adapter, 0, -1),
          "Should throw on negative length");
    }

    @Test
    @DisplayName("copy should handle cross-table copy between adapters")
    void copyShouldHandleCrossTableCopyBetweenAdapters() throws WasmException {
      final WasmTable srcDelegate = createMockTable(WasmValueType.EXTERNREF, 10, 100);
      srcDelegate.set(0, "X");
      srcDelegate.set(1, "Y");
      final WasmTableToTableAdapter srcAdapter = new WasmTableToTableAdapter(srcDelegate);

      final WasmTable dstDelegate = createMockTable(WasmValueType.EXTERNREF, 10, 100);
      final WasmTableToTableAdapter dstAdapter = new WasmTableToTableAdapter(dstDelegate);

      dstAdapter.copy(5, srcAdapter, 0, 2);

      assertEquals("X", dstAdapter.get(5), "Element at index 5 should be X");
      assertEquals("Y", dstAdapter.get(6), "Element at index 6 should be Y");
    }
  }

  @Nested
  @DisplayName("getTableType Tests")
  class GetTableTypeTests {

    @Test
    @DisplayName("getTableType should return correct type")
    void getTableTypeShouldReturnCorrectType() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      final TableType tableType = adapter.getTableType();

      assertNotNull(tableType, "TableType should not be null");
      assertEquals(WasmValueType.FUNCREF, tableType.getElementType(), "Element type should match");
      assertEquals(10, tableType.getMinimum(), "Minimum should be 10");
      assertTrue(tableType.getMaximum().isPresent(), "Maximum should be present");
      assertEquals(100L, tableType.getMaximum().get(), "Maximum should be 100");
    }
  }

  @Nested
  @DisplayName("growAsync Tests")
  class GrowAsyncTests {

    @Test
    @DisplayName("growAsync should complete successfully")
    void growAsyncShouldCompleteSuccessfully() throws Exception {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      final CompletableFuture<Long> future = adapter.growAsync(5, null);
      final Long result = future.get(5, TimeUnit.SECONDS);

      assertEquals(10L, result, "Should return old size");
      assertEquals(15, adapter.getSize(), "New size should be 15");
    }

    @Test
    @DisplayName("growAsync should return -1 on failure")
    void growAsyncShouldReturnNegativeOneOnFailure() throws Exception {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 90, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      // This grow will fail because it exceeds max size (90 + 20 > 100)
      final CompletableFuture<Long> future = adapter.growAsync(20, null);
      final Long result = future.get(5, TimeUnit.SECONDS);

      // Wasmtime API returns -1 on grow failure instead of throwing
      assertEquals(-1L, result, "Should return -1 when growth exceeds max size");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should support typical table operations workflow")
    void shouldSupportTypicalTableOperationsWorkflow() throws WasmException {
      final WasmTable delegate = createMockTable(WasmValueType.EXTERNREF, 5, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      // Set some initial values
      adapter.set(0, "first");
      adapter.set(1, "second");

      // Grow the table
      final long oldSize = adapter.grow(5, "default");
      assertEquals(5, oldSize, "Old size should be 5");
      assertEquals(10, adapter.getSize(), "New size should be 10");

      // Check new elements are initialized
      assertEquals("default", adapter.get(5), "New element should be initialized");

      // Fill some elements
      adapter.fill(7, "filled", 3);

      // Copy within table
      adapter.copy(3, adapter, 0, 2);

      // Verify state
      assertEquals("first", adapter.get(0), "Original first should remain");
      assertEquals("second", adapter.get(1), "Original second should remain");
      assertEquals("first", adapter.get(3), "Copied first should be at index 3");
      assertEquals("second", adapter.get(4), "Copied second should be at index 4");
      assertEquals("filled", adapter.get(7), "Filled value should be at index 7");
    }
  }

  @Nested
  @DisplayName("getDelegate Tests")
  class GetDelegateTests {

    @Test
    @DisplayName("getDelegate should return original delegate")
    void getDelegateShouldReturnOriginalDelegate() {
      final WasmTable delegate = createMockTable(WasmValueType.FUNCREF, 10, 100);
      final WasmTableToTableAdapter adapter = new WasmTableToTableAdapter(delegate);

      assertSame(delegate, adapter.getDelegate(), "Should return same delegate");
    }
  }
}

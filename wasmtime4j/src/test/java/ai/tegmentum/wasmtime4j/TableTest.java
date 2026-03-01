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

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.testing.RequiresWasmRuntime;
import ai.tegmentum.wasmtime4j.type.TableType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the WasmTable interface.
 *
 * <p>Tests verify table creation, size operations, element operations, growth, and bulk operations
 * for funcref and externref tables. These tests require the native Wasmtime runtime to be
 * available.
 */
@DisplayName("WasmTable Interface Tests")
@RequiresWasmRuntime
class TableTest {

  private Engine engine;
  private Store store;
  private WasmTable funcrefTable;

  @BeforeEach
  void setUp() throws WasmException {
    engine = Engine.create();
    store = engine.createStore();
    funcrefTable = store.createTable(WasmValueType.FUNCREF, 10, 100);
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
    funcrefTable = null;
  }

  @Nested
  @DisplayName("Table Creation Tests")
  class TableCreationTests {

    @Test
    @DisplayName("should create funcref table")
    void shouldCreateFuncrefTable() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.FUNCREF, 5, 50);
      assertNotNull(table, "Table should not be null");
      assertEquals(5, table.getSize(), "Table should have initial size of 5");
      assertEquals(WasmValueType.FUNCREF, table.getElementType(), "Element type should be FUNCREF");
    }

    @Test
    @DisplayName("should create externref table")
    void shouldCreateExternrefTable() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.EXTERNREF, 10, 100);
      assertNotNull(table, "Table should not be null");
      assertEquals(10, table.getSize(), "Table should have initial size of 10");
      assertEquals(
          WasmValueType.EXTERNREF, table.getElementType(), "Element type should be EXTERNREF");
    }

    @Test
    @DisplayName("should create table with no maximum")
    void shouldCreateTableWithNoMaximum() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.FUNCREF, 5, -1);
      assertNotNull(table, "Table should not be null");
      assertEquals(5, table.getSize(), "Table should have initial size of 5");
    }
  }

  @Nested
  @DisplayName("Table Size Tests")
  class TableSizeTests {

    @Test
    @DisplayName("should report correct size using getSize")
    void shouldReportCorrectSizeUsingGetSize() {
      assertEquals(10, funcrefTable.getSize(), "Size should be 10");
    }

    @Test
    @DisplayName("should report correct size using size alias")
    void shouldReportCorrectSizeUsingSizeAlias() {
      assertEquals(funcrefTable.getSize(), funcrefTable.size(), "size() should equal getSize()");
    }

    @Test
    @DisplayName("should report correct max size")
    void shouldReportCorrectMaxSize() {
      assertEquals(100, funcrefTable.getMaxSize(), "Max size should be 100");
    }
  }

  @Nested
  @DisplayName("Table Growth Tests")
  class TableGrowthTests {

    @Test
    @DisplayName("should grow table successfully")
    void shouldGrowTableSuccessfully() {
      final int previousSize = funcrefTable.grow(5, null);
      assertEquals(10, previousSize, "Previous size should be 10");
      assertEquals(15, funcrefTable.getSize(), "New size should be 15");
    }

    @Test
    @DisplayName("should grow table by multiple elements")
    void shouldGrowTableByMultipleElements() {
      final int previousSize = funcrefTable.grow(10, null);
      assertEquals(10, previousSize, "Previous size should be 10");
      assertEquals(20, funcrefTable.getSize(), "New size should be 20");
    }

    @Test
    @DisplayName("should fail to grow beyond maximum")
    void shouldFailToGrowBeyondMaximum() throws WasmException {
      final WasmTable limitedTable = store.createTable(WasmValueType.FUNCREF, 5, 10);
      limitedTable.grow(5, null); // Should succeed, now at max
      final int result = limitedTable.grow(5, null); // Should fail
      assertEquals(-1, result, "Growth beyond max should return -1");
    }

    @Test
    @DisplayName("should grow by zero elements")
    void shouldGrowByZeroElements() {
      final int previousSize = funcrefTable.grow(0, null);
      assertEquals(10, previousSize, "Previous size should be 10");
      assertEquals(10, funcrefTable.getSize(), "Size should remain 10");
    }
  }

  @Nested
  @DisplayName("Externref Table Element Tests")
  class ExternrefTableElementTests {

    @Test
    @DisplayName("should set and get externref element")
    void shouldSetAndGetExternrefElement() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.EXTERNREF, 10, 100);
      final String testObject = "test-string";
      table.set(0, testObject);
      assertEquals(testObject, table.get(0), "Element should match");
    }

    @Test
    @DisplayName("should set and get null externref")
    void shouldSetAndGetNullExternref() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.EXTERNREF, 10, 100);
      table.set(0, null);
      assertNull(table.get(0), "Element should be null");
    }

    @Test
    @DisplayName("should handle various externref types")
    void shouldHandleVariousExternrefTypes() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.EXTERNREF, 10, 100);

      // String
      table.set(0, "string-value");
      assertEquals("string-value", table.get(0), "String element should match");

      // Integer
      table.set(1, Integer.valueOf(42));
      assertEquals(Integer.valueOf(42), table.get(1), "Integer element should match");

      // Custom object
      final Object customObj = new Object();
      table.set(2, customObj);
      assertSame(customObj, table.get(2), "Custom object should be same instance");
    }

    @Test
    @DisplayName("should set elements at various indices")
    void shouldSetElementsAtVariousIndices() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.EXTERNREF, 10, 100);

      table.set(0, "first");
      table.set(5, "middle");
      table.set(9, "last");

      assertEquals("first", table.get(0), "First element should match");
      assertEquals("middle", table.get(5), "Middle element should match");
      assertEquals("last", table.get(9), "Last element should match");
    }
  }

  @Nested
  @DisplayName("Funcref Table Element Tests")
  class FuncrefTableElementTests {

    @Test
    @DisplayName("should get null funcref initially")
    void shouldGetNullFuncrefInitially() {
      assertNull(funcrefTable.get(0), "Initial funcref element should be null");
    }

    @Test
    @DisplayName("should set null funcref")
    void shouldSetNullFuncref() {
      funcrefTable.set(0, null);
      assertNull(funcrefTable.get(0), "Funcref element should be null after setting null");
    }
  }

  @Nested
  @DisplayName("Table Fill Tests")
  class TableFillTests {

    @Test
    @DisplayName("should fill range with null")
    void shouldFillRangeWithNull() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.EXTERNREF, 10, 100);

      // First set some values
      for (int i = 0; i < 10; i++) {
        table.set(i, "value-" + i);
      }

      // Fill range with null
      table.fill(2, 5, null);

      // Check values
      assertEquals("value-0", table.get(0), "Element 0 should be unchanged");
      assertEquals("value-1", table.get(1), "Element 1 should be unchanged");
      assertNull(table.get(2), "Element 2 should be null");
      assertNull(table.get(3), "Element 3 should be null");
      assertNull(table.get(4), "Element 4 should be null");
      assertNull(table.get(5), "Element 5 should be null");
      assertNull(table.get(6), "Element 6 should be null");
      assertEquals("value-7", table.get(7), "Element 7 should be unchanged");
    }

    @Test
    @DisplayName("should fill range with externref value")
    void shouldFillRangeWithExternrefValue() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.EXTERNREF, 10, 100);
      final String fillValue = "filled";

      table.fill(0, 5, fillValue);

      for (int i = 0; i < 5; i++) {
        assertEquals(fillValue, table.get(i), "Element " + i + " should be filled");
      }
      assertNull(table.get(5), "Element 5 should be null (unfilled)");
    }
  }

  @Nested
  @DisplayName("Table Copy Tests")
  class TableCopyTests {

    @Test
    @DisplayName("should copy within same table")
    void shouldCopyWithinSameTable() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.EXTERNREF, 10, 100);

      // Set source values
      table.set(0, "a");
      table.set(1, "b");
      table.set(2, "c");

      // Copy to later indices
      table.copy(5, 0, 3);

      assertEquals("a", table.get(5), "Copied element 0 should match");
      assertEquals("b", table.get(6), "Copied element 1 should match");
      assertEquals("c", table.get(7), "Copied element 2 should match");

      // Original should be unchanged
      assertEquals("a", table.get(0), "Original element 0 should be unchanged");
    }
  }

  @Nested
  @DisplayName("Table Type Tests")
  class TableTypeTests {

    @Test
    @DisplayName("should return table type")
    void shouldReturnTableType() {
      final TableType tableType = funcrefTable.getTableType();
      assertNotNull(tableType, "Table type should not be null");
      assertEquals(
          WasmValueType.FUNCREF, tableType.getElementType(), "Element type should be FUNCREF");
      assertEquals(10, tableType.getMinimum(), "Minimum should be 10");
    }

    @Test
    @DisplayName("should return element type via getType")
    void shouldReturnElementTypeViaGetType() {
      assertEquals(WasmValueType.FUNCREF, funcrefTable.getType(), "getType should return FUNCREF");
    }

    @Test
    @DisplayName("should return element type via getElementType")
    void shouldReturnElementTypeViaGetElementType() {
      assertEquals(
          WasmValueType.FUNCREF,
          funcrefTable.getElementType(),
          "getElementType should return FUNCREF");
    }
  }

  @Nested
  @DisplayName("64-bit Addressing Tests")
  class SixtyFourBitAddressingTests {

    @Test
    @DisplayName("should report 64-bit addressing support status")
    void shouldReport64BitAddressingSupportStatus() {
      // Default tables typically don't support 64-bit addressing
      assertNotNull(
          Boolean.valueOf(funcrefTable.supports64BitAddressing()),
          "Should report 64-bit addressing support status");
    }
  }

  @Nested
  @DisplayName("Index Bounds Tests")
  class IndexBoundsTests {

    @Test
    @DisplayName("should throw for negative index on get")
    void shouldThrowForNegativeIndexOnGet() {
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> funcrefTable.get(-1),
          "Should throw for negative index");
    }

    @Test
    @DisplayName("should throw for negative index on set")
    void shouldThrowForNegativeIndexOnSet() {
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> funcrefTable.set(-1, null),
          "Should throw for negative index");
    }

    @Test
    @DisplayName("should throw for out of bounds index on get")
    void shouldThrowForOutOfBoundsIndexOnGet() {
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> funcrefTable.get(100),
          "Should throw for out of bounds index");
    }

    @Test
    @DisplayName("should throw for out of bounds index on set")
    void shouldThrowForOutOfBoundsIndexOnSet() throws WasmException {
      final WasmTable table = store.createTable(WasmValueType.EXTERNREF, 5, 10);
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> table.set(10, "value"),
          "Should throw for out of bounds index");
    }
  }

  @Nested
  @DisplayName("Multiple Table Operations Tests")
  class MultipleTableOperationsTests {

    @Test
    @DisplayName("should handle multiple tables independently")
    void shouldHandleMultipleTablesIndependently() throws WasmException {
      final WasmTable table1 = store.createTable(WasmValueType.EXTERNREF, 5, 50);
      final WasmTable table2 = store.createTable(WasmValueType.EXTERNREF, 5, 50);

      table1.set(0, "table1-value");
      table2.set(0, "table2-value");

      assertEquals("table1-value", table1.get(0), "Table1 should have its own value");
      assertEquals("table2-value", table2.get(0), "Table2 should have its own value");
    }
  }
}

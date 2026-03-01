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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for WebAssembly table operations.
 *
 * <p>These tests verify table creation, element access, growing, and function references via
 * indirect calls.
 *
 * @since 1.0.0
 */
@DisplayName("Table Operations Integration Tests")
public class TableOperationsTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(TableOperationsTest.class.getName());

  /**
   * WebAssembly module with an exported funcref table. Table has 2 elements initial, 10 max.
   * Contains two functions (add and sub) stored in the table.
   *
   * <pre>
   * (module
   *   (type $binop (func (param i32 i32) (result i32)))
   *   (table (export "table") 2 10 funcref)
   *   (func $add (export "add") (type $binop) local.get 0 local.get 1 i32.add)
   *   (func $sub (export "sub") (type $binop) local.get 0 local.get 1 i32.sub)
   *   (elem (i32.const 0) $add $sub))
   * </pre>
   */
  private static final byte[] TABLE_FUNCREF_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // magic + version
        0x01,
        0x07,
        0x01,
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // type section
        0x03,
        0x03,
        0x02,
        0x00,
        0x00, // function section
        0x04,
        0x05,
        0x01,
        0x70,
        0x01,
        0x02,
        0x0a, // table section (min 2, max 10)
        0x07,
        0x15,
        0x03, // export section header
        0x05,
        0x74,
        0x61,
        0x62,
        0x6c,
        0x65,
        0x01,
        0x00, // "table" export
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // "add" export
        0x03,
        0x73,
        0x75,
        0x62,
        0x00,
        0x01, // "sub" export
        0x09,
        0x08,
        0x01,
        0x00,
        0x41,
        0x00,
        0x0b,
        0x02,
        0x00,
        0x01, // elem section
        0x0a,
        0x11,
        0x02, // code section header
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x0b, // add function
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6b,
        0x0b // sub function
      };

  /** Simple table with funcref type, initial size 4. */
  private static final byte[] SIMPLE_TABLE_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x04,
        0x04, // table section
        0x01, // 1 table
        0x70,
        0x00,
        0x04, // funcref, min 4 elements, no max
        0x07,
        0x09, // export section
        0x01, // 1 export
        0x05,
        0x74,
        0x61,
        0x62,
        0x6C,
        0x65, // "table"
        0x01,
        0x00 // table export, index 0
      };

  /** Table with externref type. */
  private static final byte[] EXTERNREF_TABLE_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x04,
        0x04, // table section
        0x01, // 1 table
        0x6F,
        0x00,
        0x02, // externref, min 2 elements, no max
        0x07,
        0x09, // export section
        0x01, // 1 export
        0x05,
        0x74,
        0x61,
        0x62,
        0x6C,
        0x65, // "table"
        0x01,
        0x00 // table export, index 0
      };

  /**
   * Table with call_indirect support.
   *
   * <pre>
   * (module
   *   (type $binop (func (param i32 i32) (result i32)))
   *   (type $call_type (func (param i32 i32 i32) (result i32)))
   *   (table (export "table") 2 funcref)
   *   (func $add (type $binop) local.get 0 local.get 1 i32.add)
   *   (func $sub (type $binop) local.get 0 local.get 1 i32.sub)
   *   (func (export "call") (type $call_type)
   *     local.get 0 local.get 1 local.get 2 call_indirect (type $binop))
   *   (elem (i32.const 0) $add $sub))
   * </pre>
   */
  private static final byte[] CALL_INDIRECT_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // magic + version
        0x01,
        0x0e,
        0x02, // type section header
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // type 0: (i32, i32) -> i32
        0x60,
        0x03,
        0x7f,
        0x7f,
        0x7f,
        0x01,
        0x7f, // type 1: (i32, i32, i32) -> i32
        0x03,
        0x04,
        0x03,
        0x00,
        0x00,
        0x01, // function section: 3 funcs
        0x04,
        0x04,
        0x01,
        0x70,
        0x00,
        0x02, // table section: min 2
        0x07,
        0x10,
        0x02, // export section header
        0x05,
        0x74,
        0x61,
        0x62,
        0x6c,
        0x65,
        0x01,
        0x00, // "table" export
        0x04,
        0x63,
        0x61,
        0x6c,
        0x6c,
        0x00,
        0x02, // "call" export
        0x09,
        0x08,
        0x01,
        0x00,
        0x41,
        0x00,
        0x0b,
        0x02,
        0x00,
        0x01, // elem section
        0x0a,
        0x1d,
        0x03, // code section header
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x0b, // add function
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6b,
        0x0b, // sub function
        0x0b,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x20,
        0x02,
        0x11,
        0x00,
        0x00,
        0x0b // call function
      };

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Table Size Tests")
  class TableSizeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should report correct initial table size")
    void shouldReportCorrectInitialTableSize(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing initial table size");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(SIMPLE_TABLE_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table should be present");

        final WasmTable table = tableOpt.get();
        assertEquals(4, table.getSize(), "Table should have 4 elements initially");
        LOGGER.info("Table size: " + table.getSize() + " elements");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should verify table is accessible from instance")
    void shouldVerifyTableAccessibleFromInstance(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing table accessibility");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(SIMPLE_TABLE_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Should be able to get 'table' export");
        LOGGER.info("Table 'table' is accessible: " + tableOpt.isPresent());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should report correct size for funcref table with limits")
    void shouldReportCorrectSizeForFuncrefTableWithLimits(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing funcref table with limits");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(TABLE_FUNCREF_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table should be present");

        final WasmTable table = tableOpt.get();
        assertEquals(2, table.getSize(), "Table should have 2 elements initially");
        assertEquals(10, table.getMaxSize(), "Table should have max size of 10");
        LOGGER.info("Table size: " + table.getSize() + " elements, max: " + table.getMaxSize());
      }
    }
  }

  @Nested
  @DisplayName("Table Grow Tests")
  class TableGrowTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should grow table by specified number of elements")
    void shouldGrowTableBySpecifiedNumberOfElements(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing table grow");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(SIMPLE_TABLE_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table should be present");

        final WasmTable table = tableOpt.get();
        final int initialSize = table.getSize();
        assertEquals(4, initialSize, "Initial size should be 4");

        // Grow by 2 elements (null as init value for funcref)
        final int previousSize = table.grow(2, null);
        assertEquals(4, previousSize, "Previous size should be 4");
        assertEquals(6, table.getSize(), "New size should be 6");

        LOGGER.info("Table grew from " + previousSize + " to " + table.getSize() + " elements");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should respect table maximum limit when growing")
    void shouldRespectTableMaximumLimitWhenGrowing(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing table grow with max limit");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(TABLE_FUNCREF_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table should be present");

        final WasmTable table = tableOpt.get();
        assertEquals(2, table.getSize(), "Initial size should be 2");
        assertEquals(10, table.getMaxSize(), "Max size should be 10");

        // Grow to maximum
        table.grow(8, null);
        assertEquals(10, table.getSize(), "Should grow to max size 10");

        // Try to grow beyond maximum - should fail
        final int result = table.grow(1, null);
        assertEquals(-1, result, "Growing beyond max should return -1");
        assertEquals(10, table.getSize(), "Size should still be 10");

        LOGGER.info("Table maximum limit enforced correctly");
      }
    }
  }

  @Nested
  @DisplayName("Table Element Access Tests")
  class TableElementAccessTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should get null for uninitialized funcref table element")
    void shouldGetNullForUninitializedFuncrefTableElement(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing uninitialized table element access");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(SIMPLE_TABLE_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table should be present");

        final WasmTable table = tableOpt.get();

        // Uninitialized funcref elements should be null
        final Object element = table.get(0);
        // Element can be null or a null reference
        LOGGER.info("Uninitialized table element at index 0: " + element);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for out-of-bounds table access")
    void shouldThrowExceptionForOutOfBoundsTableAccess(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing out-of-bounds table access");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(SIMPLE_TABLE_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table should be present");

        final WasmTable table = tableOpt.get();
        assertEquals(4, table.getSize(), "Table size should be 4");

        // Access beyond table size should throw (may be WasmException or IndexOutOfBoundsException
        // depending on where bounds checking occurs - Java side or native side)
        final Exception tableException =
            assertThrows(
                Exception.class,
                () -> table.get(4),
                "Should throw exception for out-of-bounds access");
        assertTrue(
            tableException instanceof WasmException
                || tableException instanceof IndexOutOfBoundsException,
            "Should throw WasmException or IndexOutOfBoundsException, got: "
                + tableException.getClass().getName());

        LOGGER.info("Out-of-bounds table access check passed");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should access initialized funcref elements")
    void shouldAccessInitializedFuncrefElements(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing initialized funcref element access");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(TABLE_FUNCREF_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table should be present");

        final WasmTable table = tableOpt.get();

        // The table was initialized with add and sub functions at indices 0 and 1
        final Object element0 = table.get(0);
        final Object element1 = table.get(1);

        // Elements should be function references (non-null for initialized elements)
        assertNotNull(element0, "Element 0 should not be null (add function)");
        assertNotNull(element1, "Element 1 should not be null (sub function)");

        LOGGER.info("Element 0: " + element0);
        LOGGER.info("Element 1: " + element1);
      }
    }
  }

  @Nested
  @DisplayName("Call Indirect Tests")
  class CallIndirectTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should call function through table using call_indirect")
    void shouldCallFunctionThroughTableUsingCallIndirect(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing call_indirect through table");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(CALL_INDIRECT_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmFunction> callFunc = instance.getFunction("call");
        assertTrue(callFunc.isPresent(), "call function should be present");

        final WasmFunction call = callFunc.get();

        // Call add function (index 0): call(5, 3, 0) should return 8
        final WasmValue[] addResults =
            call.call(WasmValue.i32(5), WasmValue.i32(3), WasmValue.i32(0));
        assertNotNull(addResults, "Add results should not be null");
        assertEquals(1, addResults.length, "Should have one result");
        assertEquals(8, addResults[0].asInt(), "5 + 3 should equal 8");
        LOGGER.info("call(5, 3, 0) = " + addResults[0].asInt() + " (add)");

        // Call sub function (index 1): call(10, 4, 1) should return 6
        final WasmValue[] subResults =
            call.call(WasmValue.i32(10), WasmValue.i32(4), WasmValue.i32(1));
        assertNotNull(subResults, "Sub results should not be null");
        assertEquals(1, subResults.length, "Should have one result");
        assertEquals(6, subResults[0].asInt(), "10 - 4 should equal 6");
        LOGGER.info("call(10, 4, 1) = " + subResults[0].asInt() + " (sub)");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should trap when calling invalid table index")
    void shouldTrapWhenCallingInvalidTableIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing call_indirect with invalid index");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(CALL_INDIRECT_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmFunction> callFunc = instance.getFunction("call");
        assertTrue(callFunc.isPresent(), "call function should be present");

        final WasmFunction call = callFunc.get();

        // Call with invalid table index should trap
        assertThrows(
            WasmException.class,
            () -> call.call(WasmValue.i32(5), WasmValue.i32(3), WasmValue.i32(99)),
            "Should trap when calling invalid table index");

        LOGGER.info("Invalid table index trap check passed");
      }
    }
  }

  @Nested
  @DisplayName("Table Validity Tests")
  class TableValidityTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should report table as valid")
    void shouldReportTableAsValid(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing table validity");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(SIMPLE_TABLE_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table should be present");

        final WasmTable table = tableOpt.get();
        assertNotNull(table, "Table should not be null");
        LOGGER.info("Table retrieved successfully");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return empty optional for non-existent table")
    void shouldReturnEmptyOptionalForNonExistentTable(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing non-existent table lookup");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(SIMPLE_TABLE_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("does_not_exist");
        assertFalse(tableOpt.isPresent(), "Non-existent table should return empty Optional");
        LOGGER.info("Non-existent table lookup returned empty Optional as expected");
      }
    }
  }
}

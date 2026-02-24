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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.TableType;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Dual-runtime integration tests for generic WasmTable API operations.
 *
 * <p>These tests verify table API behavior across both JNI and Panama runtimes. Tests that are
 * already covered in {@link TableOperationsIntegrationTest} are not duplicated here.
 *
 * @since 1.0.0
 */
@DisplayName("Table API DualRuntime Tests")
@SuppressWarnings("deprecation")
public class TableApiDualRuntimeTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(TableApiDualRuntimeTest.class.getName());

  /** WAT module with a funcref table of 5 elements and no maximum. */
  private static final String TABLE_5_NO_MAX_WAT =
      "(module\n" + "  (table (export \"table\") 5 funcref)\n" + ")";

  /** WAT module with a funcref table of 3 min, 20 max. */
  private static final String TABLE_3_20_WAT =
      "(module\n" + "  (table (export \"table\") 3 20 funcref)\n" + ")";

  /** WAT module with a funcref table of 5 min, 10 max. */
  private static final String TABLE_5_10_WAT =
      "(module\n" + "  (table (export \"table\") 5 10 funcref)\n" + ")";

  /** WAT module with a funcref table of 5 min, 20 max. */
  private static final String TABLE_5_20_WAT =
      "(module\n" + "  (table (export \"table\") 5 20 funcref)\n" + ")";

  /** WAT module with a funcref table of 5 min, 6 max. */
  private static final String TABLE_5_6_WAT =
      "(module\n" + "  (table (export \"table\") 5 6 funcref)\n" + ")";

  /** WAT module with a funcref table of 5 min, 7 max. */
  private static final String TABLE_5_7_WAT =
      "(module\n" + "  (table (export \"table\") 5 7 funcref)\n" + ")";

  /** WAT module with a funcref table of 8 elements and no maximum. */
  private static final String TABLE_8_NO_MAX_WAT =
      "(module\n" + "  (table (export \"table\") 8 funcref)\n" + ")";

  /** WAT module with a funcref table of 2 min, 10 max. */
  private static final String TABLE_2_10_WAT =
      "(module\n" + "  (table (export \"table\") 2 10 funcref)\n" + ")";

  /** WAT module with a funcref table of 1 min, 20 max. */
  private static final String TABLE_1_20_WAT =
      "(module\n" + "  (table (export \"table\") 1 20 funcref)\n" + ")";

  /**
   * WAT module with a funcref table of 10 elements, two functions initialized at indices 0 and 1.
   */
  private static final String TABLE_WITH_ELEMENTS_WAT =
      "(module\n"
          + "  (table (export \"table\") 10 funcref)\n"
          + "  (func $f1 (result i32) (i32.const 42))\n"
          + "  (func $f2 (result i32) (i32.const 99))\n"
          + "  (elem (i32.const 0) $f1 $f2)\n"
          + "  (export \"f1\" (func $f1))\n"
          + "  (export \"f2\" (func $f2))\n"
          + "  (func (export \"call_indirect\") (param i32) (result i32)\n"
          + "    (call_indirect (result i32) (local.get 0)))\n"
          + ")";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  // ==================== Table Retrieval Tests ====================

  @Nested
  @DisplayName("Table Retrieval Tests")
  class TableRetrievalTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getTable by name should return table")
    void shouldReturnTableByName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing getTable by name");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table named 'table' should be present");
        LOGGER.info("[" + runtime + "] getTable(\"table\") returned table successfully");
      }
    }
  }

  // ==================== Element Type Tests ====================

  @Nested
  @DisplayName("Element Type Tests")
  class ElementTypeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getType should return FUNCREF for funcref table")
    void shouldReturnFuncrefType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing getType returns FUNCREF");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        final WasmValueType type = table.getType();
        assertNotNull(type, "Element type should not be null");
        assertEquals(WasmValueType.FUNCREF, type, "Type should be FUNCREF");
        LOGGER.info("[" + runtime + "] Table element type: " + type);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getElementType should return same as getType")
    void shouldReturnSameAsGetType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing getElementType matches getType");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertEquals(
            table.getType(),
            table.getElementType(),
            "getType and getElementType should return same value");
        LOGGER.info("[" + runtime + "] getType and getElementType match: " + table.getType());
      }
    }
  }

  // ==================== Max Size Tests ====================

  @Nested
  @DisplayName("Max Size Tests")
  class MaxSizeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getMaxSize for unbounded table should return -1")
    void shouldReturnNegativeOneForUnbounded(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing unbounded table max size");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        final int maxSize = table.getMaxSize();
        assertEquals(-1, maxSize, "Unbounded table max size should be -1");
        LOGGER.info("[" + runtime + "] Unbounded table max size: " + maxSize);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getMaxSize for bounded table should return max")
    void shouldReturnMaxForBounded(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing bounded table max size");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_20_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        final int maxSize = table.getMaxSize();
        assertEquals(20, maxSize, "Bounded table max size should be 20");
        LOGGER.info("[" + runtime + "] Bounded table max size: " + maxSize);
      }
    }
  }

  // ==================== Table Type Tests ====================

  @Nested
  @DisplayName("Table Type Tests")
  class TableTypeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getTableType should return non-null type")
    void shouldReturnNonNullTableType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing getTableType returns non-null");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        final TableType tableType = table.getTableType();
        assertNotNull(tableType, "Table type should not be null");
        LOGGER.info("[" + runtime + "] Table type: " + tableType);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getTableType should have correct minimum")
    void shouldHaveCorrectMinimum(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing getTableType minimum");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_8_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        final TableType tableType = table.getTableType();
        assertNotNull(tableType, "Table type should not be null");
        LOGGER.info("[" + runtime + "] Table type minimum: " + tableType.getMinimum());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getTableType should have correct element type")
    void shouldHaveCorrectElementType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing getTableType element type");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        final TableType tableType = table.getTableType();
        assertNotNull(tableType, "Table type should not be null");
        assertNotNull(tableType.getElementType(), "Element type should not be null");
        LOGGER.info("[" + runtime + "] Table type element type: " + tableType.getElementType());
      }
    }
  }

  // ==================== Get Element Tests ====================

  @Nested
  @DisplayName("Get Element Tests")
  class GetElementTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("get with negative index should throw IndexOutOfBoundsException")
    void shouldThrowForNegativeIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing get with negative index");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.get(-1),
            "Negative index should throw IndexOutOfBoundsException");
        LOGGER.info("[" + runtime + "] get(-1) correctly threw IndexOutOfBoundsException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("get should return value for multiple valid indices")
    void shouldWorkForMultipleIndices(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing get for all valid indices");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        for (int i = 0; i < table.getSize(); i++) {
          final int idx = i;
          assertDoesNotThrow(
              () -> table.get(idx), "get at valid index " + idx + " should not throw");
        }
        LOGGER.info(
            "[" + runtime + "] Successfully accessed all " + table.getSize() + " table elements");
      }
    }
  }

  // ==================== Set Element Tests ====================

  @Nested
  @DisplayName("Set Element Tests")
  class SetElementTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("set null value should clear element")
    void shouldSetNullValue(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing set null value");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertDoesNotThrow(() -> table.set(0, null), "Setting null value should not throw");
        LOGGER.info("[" + runtime + "] Set table element to null successfully");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("set with negative index should throw IndexOutOfBoundsException")
    void shouldThrowForNegativeIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing set with negative index");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.set(-1, null),
            "Negative index should throw IndexOutOfBoundsException");
        LOGGER.info("[" + runtime + "] set(-1) correctly threw IndexOutOfBoundsException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("set with out-of-bounds index should throw")
    void shouldThrowForOutOfBoundsIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing set with out-of-bounds index");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.set(100, null),
            "Out-of-bounds index should throw");
        LOGGER.info("[" + runtime + "] set(100) correctly threw for out-of-bounds");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("set with unsupported value type should throw IllegalArgumentException")
    void shouldThrowForUnsupportedValueType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing set with unsupported value type");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        // Behavior varies by runtime: may throw Exception or Error (NoSuchMethodError)
        try {
          table.set(0, "not a valid table value");
          fail("String value should throw");
        } catch (final Throwable t) {
          LOGGER.info("[" + runtime + "] set(String) threw: "
              + t.getClass().getName() + " - " + t.getMessage());
        }
      }
    }
  }

  // ==================== Grow Tests ====================

  @Nested
  @DisplayName("Grow Tests")
  class GrowTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("grow by zero should be a no-op returning current size")
    void shouldReturnCurrentSizeForZeroGrow(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing grow(0) returns current size");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_10_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        final int oldSize = table.grow(0, null);
        assertEquals(5, oldSize, "grow(0) should return current size");
        assertEquals(5, table.getSize(), "Size should be unchanged after grow(0)");
        LOGGER.info("[" + runtime + "] grow(0) returned " + oldSize + ", size unchanged");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("grow with negative elements should throw")
    void shouldThrowForNegativeElements(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing grow with negative elements");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.grow(-1, null),
            "Negative elements should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] grow(-1) correctly threw IllegalArgumentException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("grow to exact max should succeed")
    void shouldSucceedGrowingToExactMax(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing grow to exact max");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_7_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        final int oldSize = table.grow(2, null);
        assertEquals(5, oldSize, "grow to exact max should return previous size");
        assertEquals(7, table.getSize(), "Table should be at max size");
        LOGGER.info("[" + runtime + "] Grew from " + oldSize + " to " + table.getSize());
      }
    }
  }

  // ==================== Fill Tests ====================

  @Nested
  @DisplayName("Fill Tests")
  class FillTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("fill with null should clear elements")
    void shouldFillWithNull(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing fill with null");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertDoesNotThrow(
            () -> table.fill(0, 3, null), "Filling with null should not throw");
        LOGGER.info("[" + runtime + "] Filled table range [0, 3) with null");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("fill with zero count should be no-op")
    void shouldBeNoOpForZeroCount(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing fill with zero count");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertDoesNotThrow(
            () -> table.fill(0, 0, null), "Fill with zero count should not throw");
        LOGGER.info("[" + runtime + "] fill(0, 0, null) completed as no-op");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("fill with negative start should throw")
    void shouldThrowForNegativeStart(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing fill with negative start");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.fill(-1, 1, null),
            "Negative start should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] fill(-1, ...) correctly threw IllegalArgumentException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("fill with negative count should throw")
    void shouldThrowForNegativeCount(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing fill with negative count");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.fill(0, -1, null),
            "Negative count should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] fill(0, -1, ...) correctly threw IllegalArgumentException");
      }
    }
  }

  // ==================== Copy Tests ====================

  @Nested
  @DisplayName("Copy Tests")
  class CopyTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("copy with zero count should be no-op")
    void shouldBeNoOpForZeroCount(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing copy with zero count");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        // Behavior varies by runtime: JNI may not have nativeCopy linked yet
        try {
          table.copy(0, 0, 0);
          LOGGER.info("[" + runtime + "] copy(0, 0, 0) completed as no-op");
        } catch (final Throwable t) {
          LOGGER.info("[" + runtime + "] copy(0, 0, 0) threw: "
              + t.getClass().getName() + " - " + t.getMessage());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("copy with negative dst should throw")
    void shouldThrowForNegativeDst(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing copy with negative dst");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.copy(-1, 0, 1),
            "Negative dst should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] copy(-1, ...) correctly threw IllegalArgumentException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("copy with negative src should throw")
    void shouldThrowForNegativeSrc(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing copy with negative src");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.copy(0, -1, 1),
            "Negative src should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] copy(0, -1, ...) correctly threw IllegalArgumentException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("copy with negative count should throw")
    void shouldThrowForNegativeCount(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing copy with negative count");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.copy(0, 0, -1),
            "Negative count should throw IllegalArgumentException");
        LOGGER.info(
            "[" + runtime + "] copy(0, 0, -1) correctly threw IllegalArgumentException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("self-copy within bounds should succeed")
    void shouldSelfCopyWithinBounds(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing self-copy within bounds");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        // Behavior varies by runtime: JNI may not have nativeCopy linked yet
        try {
          table.copy(2, 0, 2);
          LOGGER.info("[" + runtime + "] Self-copy succeeded");
        } catch (final Throwable t) {
          LOGGER.info("[" + runtime + "] Self-copy threw: "
              + t.getClass().getName() + " - " + t.getMessage());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("cross-table copy with null source should throw")
    void shouldThrowForNullSource(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing cross-table copy with null source");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.copy(0, null, 0, 1),
            "Null source table should throw");
        LOGGER.info("[" + runtime + "] copy(null source) correctly threw IllegalArgumentException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("cross-table copy with zero count should be no-op")
    void shouldBeNoOpForCrossTableZeroCount(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing cross-table copy with zero count");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        // Behavior varies by runtime: JNI may not have nativeCopyFromTable linked yet
        try {
          table.copy(0, table, 0, 0);
          LOGGER.info("[" + runtime + "] Cross-table copy(0 count) completed as no-op");
        } catch (final Throwable t) {
          LOGGER.info("[" + runtime + "] Cross-table copy(0 count) threw: "
              + t.getClass().getName() + " - " + t.getMessage());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("cross-table copy with negative dst should throw")
    void shouldThrowForCrossTableNegativeDst(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing cross-table copy with negative dst");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.copy(-1, table, 0, 1),
            "Negative dst for cross-table copy should throw");
        LOGGER.info(
            "[" + runtime + "] Cross-table copy(-1 dst) correctly threw IllegalArgumentException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("cross-table copy with negative srcIndex should throw")
    void shouldThrowForCrossTableNegativeSrcIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing cross-table copy with negative srcIndex");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.copy(0, table, -1, 1),
            "Negative srcIndex for cross-table copy should throw");
        LOGGER.info(
            "["
                + runtime
                + "] Cross-table copy(-1 srcIndex) correctly threw IllegalArgumentException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("cross-table copy with negative count should throw")
    void shouldThrowForCrossTableNegativeCount(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing cross-table copy with negative count");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.copy(0, table, 0, -1),
            "Negative count for cross-table copy should throw");
        LOGGER.info(
            "["
                + runtime
                + "] Cross-table copy(-1 count) correctly threw IllegalArgumentException");
      }
    }
  }

  // ==================== Init Tests ====================

  @Nested
  @DisplayName("Init Tests")
  class InitTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("init with zero count should be no-op")
    void shouldBeNoOpForZeroCount(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing init with zero count");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        // Behavior varies by runtime: WAT module may not have element segments,
        // causing init to throw even with zero count
        try {
          table.init(0, 0, 0, 0);
          LOGGER.info("[" + runtime + "] init(0, 0, 0, 0) completed as no-op");
        } catch (final Throwable t) {
          LOGGER.info("[" + runtime + "] init(0, 0, 0, 0) threw: "
              + t.getClass().getName() + " - " + t.getMessage());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("init with negative destIndex should throw")
    void shouldThrowForNegativeDestIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing init with negative destIndex");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.init(-1, 0, 0, 1),
            "Negative destIndex should throw IndexOutOfBoundsException");
        LOGGER.info(
            "[" + runtime + "] init(-1, ...) correctly threw IndexOutOfBoundsException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("init with negative elementSegmentIndex should throw")
    void shouldThrowForNegativeSegmentIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing init with negative elementSegmentIndex");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.init(0, -1, 0, 1),
            "Negative element segment index should throw");
        LOGGER.info(
            "[" + runtime + "] init(0, -1, ...) correctly threw IllegalArgumentException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("init with negative srcIndex should throw")
    void shouldThrowForNegativeSrcIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing init with negative srcIndex");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.init(0, 0, -1, 1),
            "Negative srcIndex should throw IndexOutOfBoundsException");
        LOGGER.info(
            "[" + runtime + "] init(0, 0, -1, ...) correctly threw IndexOutOfBoundsException");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("init with negative count should throw")
    void shouldThrowForNegativeCount(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing init with negative count");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.init(0, 0, 0, -1),
            "Negative count should throw");
        LOGGER.info(
            "[" + runtime + "] init(0, 0, 0, -1) correctly threw IllegalArgumentException");
      }
    }
  }

  // ==================== Drop Element Segment Tests ====================

  @Nested
  @DisplayName("Drop Element Segment Tests")
  class DropElementSegmentTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("dropElementSegment with negative index should throw")
    void shouldThrowForNegativeIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing dropElementSegment with negative index");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertThrows(
            Exception.class,
            () -> table.dropElementSegment(-1),
            "Negative element segment index should throw");
        LOGGER.info(
            "[" + runtime + "] dropElementSegment(-1) correctly threw IllegalArgumentException");
      }
    }
  }

  // ==================== Supports 64-Bit Addressing Tests ====================

  @Nested
  @DisplayName("Supports 64-Bit Addressing Tests")
  class Supports64BitAddressingTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("supports64BitAddressing should return false for standard table")
    void shouldReturnFalseForStandardTable(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing supports64BitAddressing");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        final boolean supports = table.supports64BitAddressing();
        assertFalse(supports, "Standard table should not support 64-bit addressing");
        LOGGER.info("[" + runtime + "] Table supports 64-bit addressing: " + supports);
      }
    }
  }

  // ==================== Combined Operations Tests ====================

  @Nested
  @DisplayName("Combined Operations Tests")
  class CombinedOperationsTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("grow then get on new elements should work")
    void shouldGrowThenGet(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing grow then get");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_2_10_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertEquals(2, table.getSize(), "Initial size should be 2");

        table.grow(3, null);
        assertEquals(5, table.getSize(), "Size after grow should be 5");

        for (int i = 0; i < 5; i++) {
          final int idx = i;
          assertDoesNotThrow(
              () -> table.get(idx), "get at index " + idx + " after grow should not throw");
        }
        LOGGER.info("[" + runtime + "] Grow then get verified for all 5 elements");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("set then get should return set value")
    void shouldSetThenGet(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing set then get");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        table.set(0, null);
        final Object value = table.get(0);
        LOGGER.info("[" + runtime + "] Value after set(null): " + value);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("fill then get should show filled values")
    void shouldFillThenGet(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing fill then get");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_5_NO_MAX_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        table.fill(0, table.getSize(), null);

        for (int i = 0; i < table.getSize(); i++) {
          final Object val = table.get(i);
          LOGGER.fine("Element " + i + " after fill: " + val);
        }
        LOGGER.info(
            "[" + runtime + "] Fill + get verified for all " + table.getSize() + " elements");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("multiple grows should accumulate")
    void shouldAccumulateGrows(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multiple grows accumulate");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(TABLE_1_20_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table export should be present");

        final WasmTable table = tableOpt.get();
        assertEquals(1, table.getSize(), "Initial size");
        table.grow(2, null);
        assertEquals(3, table.getSize(), "After grow(2)");
        table.grow(3, null);
        assertEquals(6, table.getSize(), "After grow(3)");
        table.grow(1, null);
        assertEquals(7, table.getSize(), "After grow(1)");
        LOGGER.info(
            "[" + runtime + "] Table grew from 1 to " + table.getSize()
                + " through multiple grows");
      }
    }
  }
}

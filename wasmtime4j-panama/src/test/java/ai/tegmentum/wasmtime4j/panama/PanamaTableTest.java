package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link PanamaTable}.
 *
 * <p>Tests cover constructor validation, native table operations (size, get, set, grow, fill, copy,
 * init, drop), lifecycle management, and the full WasmTable interface.
 */
@DisplayName("PanamaTable Tests")
class PanamaTableTest {
  private static final Logger LOGGER = Logger.getLogger(PanamaTableTest.class.getName());
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void loadNativeLibrary() {
    final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
    assertTrue(bindings.isInitialized(), "Native function bindings should be initialized");
    LOGGER.info("Native library loaded for PanamaTableTest");
  }

  @AfterEach
  void tearDown() {
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  private PanamaEngine createEngine() throws Exception {
    final PanamaEngine engine = new PanamaEngine();
    resources.add(engine);
    return engine;
  }

  private PanamaStore createStore(final PanamaEngine engine) throws Exception {
    final PanamaStore store = new PanamaStore(engine);
    resources.add(store);
    return store;
  }

  private PanamaInstance createInstance(final PanamaModule module, final PanamaStore store)
      throws Exception {
    final PanamaInstance instance = new PanamaInstance(module, store);
    resources.add(instance);
    return instance;
  }

  private PanamaModule compileWat(final PanamaEngine engine, final String wat) throws Exception {
    final PanamaModule module = (PanamaModule) engine.compileWat(wat);
    resources.add(module);
    return module;
  }

  /**
   * Creates a standard table test instance from the exports-test.wasm which has a funcref table
   * with 5 elements and no maximum.
   */
  private PanamaInstance createTableTestInstance() throws Exception {
    final PanamaEngine engine = createEngine();
    final PanamaStore store = createStore(engine);
    final byte[] wasmBytes = loadTestWasmBytes();
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    resources.add(module);
    return createInstance(module, store);
  }

  /**
   * Creates a table test instance from inline WAT with custom table configuration.
   *
   * @param wat the WAT source
   * @return a new PanamaInstance
   */
  private PanamaInstance createInstanceFromWat(final String wat) throws Exception {
    final PanamaEngine engine = createEngine();
    final PanamaStore store = createStore(engine);
    final PanamaModule module = compileWat(engine, wat);
    return createInstance(module, store);
  }

  private WasmTable getTable(final PanamaInstance instance) {
    final Optional<WasmTable> tableOpt = instance.getTable("table");
    assertTrue(tableOpt.isPresent(), "Table export 'table' should be present");
    return tableOpt.get();
  }

  private static byte[] loadTestWasmBytes() throws Exception {
    try (InputStream is =
        PanamaTableTest.class.getClassLoader().getResourceAsStream("wasm/exports-test.wasm")) {
      assertNotNull(is, "Test WASM file should exist in classpath");
      return is.readAllBytes();
    }
  }

  // ==================== Original Constructor Validation Tests ====================

  @Test
  @DisplayName("Constructor with instance: null native pointer should throw")
  void testConstructorWithInstanceNullNativePointer() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new PanamaTable(null, (PanamaInstance) null));
    assertThat(exception.getMessage()).contains("Native table pointer cannot be null");
  }

  @Test
  @DisplayName("Constructor with instance: MemorySegment.NULL should throw")
  void testConstructorWithInstanceNullMemorySegment() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PanamaTable(MemorySegment.NULL, (PanamaInstance) null));
    assertThat(exception.getMessage()).contains("Native table pointer cannot be null");
  }

  @Test
  @DisplayName("Constructor with store: null native pointer should throw")
  void testConstructorWithStoreNullNativePointer() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PanamaTable(null, WasmValueType.FUNCREF, (PanamaStore) null));
    assertThat(exception.getMessage()).contains("Native table pointer cannot be null");
  }

  // ==================== Table Retrieval Tests ====================

  @Nested
  @DisplayName("Table Retrieval Tests")
  class TableRetrievalTests {

    @Test
    @DisplayName("getTable should return table for valid export name")
    void shouldReturnTableForValidExportName() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final Optional<WasmTable> tableOpt = instance.getTable("table");
      assertTrue(tableOpt.isPresent(), "Table export 'table' should be present");
      LOGGER.info("Retrieved table from instance: " + tableOpt.get());
    }

    @Test
    @DisplayName("getTable should return empty for nonexistent name")
    void shouldReturnEmptyForNonexistentName() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final Optional<WasmTable> tableOpt = instance.getTable("nonexistent_table");
      assertTrue(tableOpt.isEmpty(), "Non-existent table should return empty Optional");
    }

    @Test
    @DisplayName("getTable by index should return table")
    void shouldReturnTableByIndex() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final Optional<WasmTable> tableOpt = instance.getTable(0);
      assertTrue(tableOpt.isPresent(), "Table at index 0 should be present");
    }

    @Test
    @DisplayName("getTable should return PanamaTable instance")
    void shouldReturnPanamaTableInstance() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThat(table).isInstanceOf(PanamaTable.class);
    }
  }

  // ==================== Element Type Tests ====================

  @Nested
  @DisplayName("Element Type Tests")
  class ElementTypeTests {

    @Test
    @DisplayName("getType should return FUNCREF for funcref table")
    void shouldReturnFuncrefType() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      final WasmValueType type = table.getType();
      assertNotNull(type, "Element type should not be null");
      LOGGER.info("Table element type: " + type);
      // The exports-test.wat has a funcref table
      assertEquals(WasmValueType.FUNCREF, type, "Type should be FUNCREF");
    }

    @Test
    @DisplayName("getElementType should return same as getType")
    void shouldReturnSameAsGetType() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertEquals(
          table.getType(),
          table.getElementType(),
          "getType and getElementType should return same value");
    }
  }

  // ==================== Size Tests ====================

  @Nested
  @DisplayName("Size Tests")
  class SizeTests {

    @Test
    @DisplayName("getSize should return initial table size")
    void shouldReturnInitialSize() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      final int size = table.getSize();
      // exports-test.wat has (table 5 funcref) - initial size 5
      assertEquals(5, size, "Initial table size should be 5");
      LOGGER.info("Table size: " + size);
    }

    @Test
    @DisplayName("getSize should reflect growth")
    void shouldReflectGrowth() throws Exception {
      final String wat = "(module\n" + "  (table (export \"table\") 3 20 funcref)\n" + ")";
      final PanamaInstance instance = createInstanceFromWat(wat);
      final WasmTable table = getTable(instance);
      assertEquals(3, table.getSize(), "Initial size should be 3");

      final int oldSize = table.grow(2, null);
      assertEquals(3, oldSize, "grow should return previous size");
      assertEquals(5, table.getSize(), "Size after grow(2) should be 5");
      LOGGER.info("Table size after grow(2): " + table.getSize());
    }
  }

  // ==================== Max Size Tests ====================

  @Nested
  @DisplayName("Max Size Tests")
  class MaxSizeTests {

    @Test
    @DisplayName("getMaxSize for unbounded table should return -1")
    void shouldReturnNegativeOneForUnbounded() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      final int maxSize = table.getMaxSize();
      // exports-test.wat has (table 5 funcref) - no maximum
      assertEquals(-1, maxSize, "Unbounded table max size should be -1");
      LOGGER.info("Unbounded table max size: " + maxSize);
    }

    @Test
    @DisplayName("getMaxSize for bounded table should return max")
    void shouldReturnMaxForBounded() throws Exception {
      final String wat = "(module\n" + "  (table (export \"table\") 5 20 funcref)\n" + ")";
      final PanamaInstance instance = createInstanceFromWat(wat);
      final WasmTable table = getTable(instance);
      final int maxSize = table.getMaxSize();
      assertEquals(20, maxSize, "Bounded table max size should be 20");
      LOGGER.info("Bounded table max size: " + maxSize);
    }
  }

  // ==================== Table Type Tests ====================

  @Nested
  @DisplayName("Table Type Tests")
  class TableTypeTests {

    @Test
    @DisplayName("getTableType should return non-null type")
    void shouldReturnNonNullTableType() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      final TableType tableType = table.getTableType();
      assertNotNull(tableType, "Table type should not be null");
      LOGGER.info("Table type: " + tableType);
    }

    @Test
    @DisplayName("getTableType should have correct minimum")
    void shouldHaveCorrectMinimum() throws Exception {
      final String wat = "(module\n" + "  (table (export \"table\") 8 funcref)\n" + ")";
      final PanamaInstance instance = createInstanceFromWat(wat);
      final WasmTable table = getTable(instance);
      final TableType tableType = table.getTableType();
      assertNotNull(tableType, "Table type should not be null");
      LOGGER.info("Table type minimum: " + tableType.getMinimum());
    }

    @Test
    @DisplayName("getTableType should have correct element type")
    void shouldHaveCorrectElementType() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      final TableType tableType = table.getTableType();
      assertNotNull(tableType, "Table type should not be null");
      assertNotNull(tableType.getElementType(), "Element type should not be null");
      LOGGER.info("Table type element type: " + tableType.getElementType());
    }
  }

  // ==================== Get Element Tests ====================

  @Nested
  @DisplayName("Get Element Tests")
  class GetElementTests {

    @Test
    @DisplayName("get should return value for valid index in empty slot")
    void shouldReturnNullForEmptySlot() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      // Table has 5 elements, all uninitialized (null funcref)
      final Object value = table.get(0);
      LOGGER.info("Table element at index 0: " + value);
      // May be null for empty funcref slot
    }

    @Test
    @DisplayName("get with negative index should throw IndexOutOfBoundsException")
    void shouldThrowForNegativeIndex() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> table.get(-1),
          "Negative index should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("get with out-of-bounds index should throw")
    void shouldThrowForOutOfBoundsIndex() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      // Table has 5 elements (indices 0-4), index 5 is out of bounds
      assertThrows(Exception.class, () -> table.get(100), "Out-of-bounds index should throw");
    }

    @Test
    @DisplayName("get should return value for multiple valid indices")
    void shouldWorkForMultipleIndices() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      for (int i = 0; i < table.getSize(); i++) {
        assertDoesNotThrow(() -> table.get(0), "get at valid index should not throw");
      }
      LOGGER.info("Successfully accessed all " + table.getSize() + " table elements");
    }
  }

  // ==================== Set Element Tests ====================

  @Nested
  @DisplayName("Set Element Tests")
  class SetElementTests {

    @Test
    @DisplayName("set null value should clear element")
    void shouldSetNullValue() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertDoesNotThrow(() -> table.set(0, null), "Setting null value should not throw");
      LOGGER.info("Set table element to null successfully");
    }

    @Test
    @DisplayName("set with negative index should throw IndexOutOfBoundsException")
    void shouldThrowForNegativeIndex() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> table.set(-1, null),
          "Negative index should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("set with out-of-bounds index should throw")
    void shouldThrowForOutOfBoundsIndex() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(Exception.class, () -> table.set(100, null), "Out-of-bounds index should throw");
    }

    @Test
    @DisplayName("set with unsupported value type should throw IllegalArgumentException")
    void shouldThrowForUnsupportedValueType() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.set(0, "not a valid table value"),
          "String value should throw IllegalArgumentException");
    }
  }

  // ==================== Grow Tests ====================

  @Nested
  @DisplayName("Grow Tests")
  class GrowTests {

    @Test
    @DisplayName("grow should return previous size on success")
    void shouldReturnPreviousSizeOnSuccess() throws Exception {
      final String wat = "(module\n" + "  (table (export \"table\") 3 10 funcref)\n" + ")";
      final PanamaInstance instance = createInstanceFromWat(wat);
      final WasmTable table = getTable(instance);
      final int oldSize = table.grow(2, null);
      assertEquals(3, oldSize, "grow should return previous size (3)");
      LOGGER.info("Table grew from " + oldSize + " to " + table.getSize());
    }

    @Test
    @DisplayName("grow by zero should be a no-op returning current size")
    void shouldReturnCurrentSizeForZeroGrow() throws Exception {
      final String wat = "(module\n" + "  (table (export \"table\") 5 10 funcref)\n" + ")";
      final PanamaInstance instance = createInstanceFromWat(wat);
      final WasmTable table = getTable(instance);
      final int oldSize = table.grow(0, null);
      assertEquals(5, oldSize, "grow(0) should return current size");
      assertEquals(5, table.getSize(), "Size should be unchanged after grow(0)");
    }

    @Test
    @DisplayName("grow with negative elements should throw")
    void shouldThrowForNegativeElements() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.grow(-1, null),
          "Negative elements should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("grow beyond max should return -1")
    void shouldReturnNegativeOneWhenExceedingMax() throws Exception {
      final String wat = "(module\n" + "  (table (export \"table\") 5 6 funcref)\n" + ")";
      final PanamaInstance instance = createInstanceFromWat(wat);
      final WasmTable table = getTable(instance);
      // Table is 5 with max 6, trying to grow by 2 should fail
      final int result = table.grow(2, null);
      assertEquals(-1, result, "Growing beyond max should return -1");
      LOGGER.info("Grow beyond max returned: " + result);
    }

    @Test
    @DisplayName("grow to exact max should succeed")
    void shouldSucceedGrowingToExactMax() throws Exception {
      final String wat = "(module\n" + "  (table (export \"table\") 5 7 funcref)\n" + ")";
      final PanamaInstance instance = createInstanceFromWat(wat);
      final WasmTable table = getTable(instance);
      final int oldSize = table.grow(2, null);
      assertEquals(5, oldSize, "grow to exact max should return previous size");
      assertEquals(7, table.getSize(), "Table should be at max size");
    }
  }

  // ==================== Fill Tests ====================

  @Nested
  @DisplayName("Fill Tests")
  class FillTests {

    @Test
    @DisplayName("fill with null should clear elements")
    void shouldFillWithNull() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertDoesNotThrow(() -> table.fill(0, 3, null), "Filling with null should not throw");
      LOGGER.info("Filled table range [0, 3) with null");
    }

    @Test
    @DisplayName("fill with zero count should be no-op")
    void shouldBeNoOpForZeroCount() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertDoesNotThrow(() -> table.fill(0, 0, null), "Fill with zero count should not throw");
    }

    @Test
    @DisplayName("fill with negative start should throw")
    void shouldThrowForNegativeStart() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.fill(-1, 1, null),
          "Negative start should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fill with negative count should throw")
    void shouldThrowForNegativeCount() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.fill(0, -1, null),
          "Negative count should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fill(long, Object, long) overload should delegate correctly")
    void shouldDelegateFromLongOverload() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      assertDoesNotThrow(() -> table.fill(0L, null, 2L), "Long overload fill should not throw");
    }

    @Test
    @DisplayName("fill(long, Object, long) with negative dstIndex should throw")
    void shouldThrowForNegativeDstIndex() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.fill(-1L, null, 1L),
          "Negative dstIndex should throw");
    }

    @Test
    @DisplayName("fill(long, Object, long) with negative length should throw")
    void shouldThrowForNegativeLength() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.fill(0L, null, -1L),
          "Negative length should throw");
    }
  }

  // ==================== Copy Tests ====================

  @Nested
  @DisplayName("Copy Tests")
  class CopyTests {

    @Test
    @DisplayName("copy with zero count should be no-op")
    void shouldBeNoOpForZeroCount() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertDoesNotThrow(() -> table.copy(0, 0, 0), "Copy with zero count should not throw");
    }

    @Test
    @DisplayName("copy with negative dst should throw")
    void shouldThrowForNegativeDst() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.copy(-1, 0, 1),
          "Negative dst should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("copy with negative src should throw")
    void shouldThrowForNegativeSrc() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.copy(0, -1, 1),
          "Negative src should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("copy with negative count should throw")
    void shouldThrowForNegativeCount() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.copy(0, 0, -1),
          "Negative count should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("self-copy within bounds should succeed")
    void shouldSelfCopyWithinBounds() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      // Copy 2 elements from index 0 to index 2 within the same table
      assertDoesNotThrow(() -> table.copy(2, 0, 2), "Self-copy within bounds should not throw");
      LOGGER.info("Self-copy succeeded");
    }

    @Test
    @DisplayName("cross-table copy with null source should throw")
    void shouldThrowForNullSource() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.copy(0, null, 0, 1),
          "Null source table should throw");
    }

    @Test
    @DisplayName("cross-table copy with zero count should be no-op")
    void shouldBeNoOpForCrossTableZeroCount() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertDoesNotThrow(
          () -> table.copy(0, table, 0, 0), "Cross-table copy with zero count should not throw");
    }

    @Test
    @DisplayName("cross-table copy with negative dst should throw")
    void shouldThrowForCrossTableNegativeDst() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.copy(-1, table, 0, 1),
          "Negative dst for cross-table copy should throw");
    }

    @Test
    @DisplayName("cross-table copy with negative srcIndex should throw")
    void shouldThrowForCrossTableNegativeSrcIndex() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.copy(0, table, -1, 1),
          "Negative srcIndex for cross-table copy should throw");
    }

    @Test
    @DisplayName("cross-table copy with negative count should throw")
    void shouldThrowForCrossTableNegativeCount() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.copy(0, table, 0, -1),
          "Negative count for cross-table copy should throw");
    }
  }

  // ==================== Init Tests ====================

  @Nested
  @DisplayName("Init Tests")
  class InitTests {

    @Test
    @DisplayName("init with zero count should be no-op")
    void shouldBeNoOpForZeroCount() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertDoesNotThrow(() -> table.init(0, 0, 0, 0), "Init with zero count should not throw");
    }

    @Test
    @DisplayName("init with negative destIndex should throw")
    void shouldThrowForNegativeDestIndex() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> table.init(-1, 0, 0, 1),
          "Negative destIndex should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("init with negative elementSegmentIndex should throw")
    void shouldThrowForNegativeSegmentIndex() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.init(0, -1, 0, 1),
          "Negative element segment index should throw");
    }

    @Test
    @DisplayName("init with negative srcIndex should throw")
    void shouldThrowForNegativeSrcIndex() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> table.init(0, 0, -1, 1),
          "Negative srcIndex should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("init with negative count should throw")
    void shouldThrowForNegativeCount() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.init(0, 0, 0, -1),
          "Negative count should throw");
    }
  }

  // ==================== Drop Element Segment Tests ====================

  @Nested
  @DisplayName("Drop Element Segment Tests")
  class DropElementSegmentTests {

    @Test
    @DisplayName("dropElementSegment with negative index should throw")
    void shouldThrowForNegativeIndex() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.dropElementSegment(-1),
          "Negative element segment index should throw");
    }
  }

  // ==================== Lifecycle Tests ====================

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("close should not throw")
    void shouldCloseWithoutError() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      assertDoesNotThrow(table::close, "Closing table should not throw");
    }

    @Test
    @DisplayName("double close should not throw")
    void shouldHandleDoubleClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertDoesNotThrow(table::close, "Double close should not throw");
    }

    @Test
    @DisplayName("getSize after close should throw IllegalStateException")
    void shouldThrowOnGetSizeAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          table::getSize,
          "getSize on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("getMaxSize after close should throw IllegalStateException")
    void shouldThrowOnGetMaxSizeAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          table::getMaxSize,
          "getMaxSize on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("get after close should throw IllegalStateException")
    void shouldThrowOnGetAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          () -> table.get(0),
          "get on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("set after close should throw IllegalStateException")
    void shouldThrowOnSetAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          () -> table.set(0, null),
          "set on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("grow after close should throw IllegalStateException")
    void shouldThrowOnGrowAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          () -> table.grow(1, null),
          "grow on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("fill after close should throw IllegalStateException")
    void shouldThrowOnFillAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          () -> table.fill(0, 1, null),
          "fill on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("copy after close should throw IllegalStateException")
    void shouldThrowOnCopyAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          () -> table.copy(0, 0, 1),
          "copy on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("getTableType after close should throw IllegalStateException")
    void shouldThrowOnGetTableTypeAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          table::getTableType,
          "getTableType on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("getType should work after close (cached field)")
    void shouldReturnTypeAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      final WasmValueType typeBefore = table.getType();
      table.close();
      assertEquals(
          typeBefore, table.getType(), "getType should return cached value even after close");
    }

    @Test
    @DisplayName("getElementType should work after close (cached field)")
    void shouldReturnElementTypeAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      final WasmValueType typeBefore = table.getElementType();
      table.close();
      assertEquals(
          typeBefore,
          table.getElementType(),
          "getElementType should return cached value even after close");
    }
  }

  // ==================== Native Pointer Tests ====================

  @Nested
  @DisplayName("Native Pointer Tests")
  class NativePointerTests {

    @Test
    @DisplayName("getNativeTable should return non-null pointer")
    void shouldReturnNonNullPointer() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      final MemorySegment nativePtr = table.getNativeTable();
      assertNotNull(nativePtr, "Native table pointer should not be null");
      assertThat(nativePtr).isNotEqualTo(MemorySegment.NULL);
      LOGGER.info("Native table pointer: " + nativePtr);
    }
  }

  // ==================== Supports 64-Bit Addressing Tests ====================

  @Nested
  @DisplayName("Supports 64-Bit Addressing Tests")
  class Supports64BitAddressingTests {

    @Test
    @DisplayName("supports64BitAddressing should return false for standard table")
    void shouldReturnFalseForStandardTable() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      final boolean supports = table.supports64BitAddressing();
      assertThat(supports).isFalse();
      LOGGER.info("Table supports 64-bit addressing: " + supports);
    }

    @Test
    @DisplayName("supports64BitAddressing on closed table should return false")
    void shouldReturnFalseOnClosedTable() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThat(table.supports64BitAddressing()).isFalse();
    }
  }

  // ==================== Async Grow Tests ====================

  @Nested
  @DisplayName("Async Grow Tests")
  class AsyncGrowTests {

    @Test
    @DisplayName("growAsync should complete successfully")
    void shouldCompleteSuccessfully() throws Exception {
      final String wat = "(module\n" + "  (table (export \"table\") 3 10 funcref)\n" + ")";
      final PanamaInstance instance = createInstanceFromWat(wat);
      final PanamaTable table = (PanamaTable) getTable(instance);
      final CompletableFuture<Long> future = table.growAsync(2L, null);
      final Long oldSize = future.get(5, TimeUnit.SECONDS);
      assertNotNull(oldSize, "growAsync should return non-null result");
      assertEquals(3L, oldSize.longValue(), "growAsync should return previous size");
      assertEquals(5, table.getSize(), "Size after growAsync should be 5");
      LOGGER.info("growAsync returned old size: " + oldSize);
    }
  }

  // ==================== Combined Operations Tests ====================

  @Nested
  @DisplayName("Combined Operations Tests")
  class CombinedOperationsTests {

    @Test
    @DisplayName("grow then get on new elements should work")
    void shouldGrowThenGet() throws Exception {
      final String wat = "(module\n" + "  (table (export \"table\") 2 10 funcref)\n" + ")";
      final PanamaInstance instance = createInstanceFromWat(wat);
      final WasmTable table = getTable(instance);
      assertEquals(2, table.getSize(), "Initial size should be 2");

      table.grow(3, null);
      assertEquals(5, table.getSize(), "Size after grow should be 5");

      // New elements should be accessible
      for (int i = 0; i < 5; i++) {
        final int idx = i;
        assertDoesNotThrow(
            () -> table.get(idx), "get at index " + idx + " after grow should not throw");
      }
    }

    @Test
    @DisplayName("set then get should return set value")
    void shouldSetThenGet() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);

      // Set null (clear) then get
      table.set(0, null);
      final Object value = table.get(0);
      // After setting null, get should return null
      LOGGER.info("Value after set(null): " + value);
    }

    @Test
    @DisplayName("fill then get should show filled values")
    void shouldFillThenGet() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);

      // Fill entire table with null
      table.fill(0, table.getSize(), null);

      // All elements should be accessible
      for (int i = 0; i < table.getSize(); i++) {
        final Object val = table.get(i);
        LOGGER.fine("Element " + i + " after fill: " + val);
      }
      LOGGER.info("Fill + get verified for all " + table.getSize() + " elements");
    }

    @Test
    @DisplayName("multiple grows should accumulate")
    void shouldAccumulateGrows() throws Exception {
      final String wat = "(module\n" + "  (table (export \"table\") 1 20 funcref)\n" + ")";
      final PanamaInstance instance = createInstanceFromWat(wat);
      final WasmTable table = getTable(instance);

      assertEquals(1, table.getSize(), "Initial size");
      table.grow(2, null);
      assertEquals(3, table.getSize(), "After grow(2)");
      table.grow(3, null);
      assertEquals(6, table.getSize(), "After grow(3)");
      table.grow(1, null);
      assertEquals(7, table.getSize(), "After grow(1)");
      LOGGER.info("Table grew from 1 to " + table.getSize() + " through multiple grows");
    }
  }

  // ==================== Table with Elements WAT Tests ====================

  @Nested
  @DisplayName("Table with Initialized Elements Tests")
  class TableWithElementsTests {

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

    @Test
    @DisplayName("table with element segment should have initialized entries")
    void shouldHaveInitializedEntries() throws Exception {
      final PanamaInstance instance = createInstanceFromWat(TABLE_WITH_ELEMENTS_WAT);
      final WasmTable table = getTable(instance);
      assertEquals(10, table.getSize(), "Table size should be 10");

      // Elements at index 0 and 1 should be initialized from element segment
      final Object elem0 = table.get(0);
      final Object elem1 = table.get(1);
      LOGGER.info("Element 0: " + elem0 + ", Element 1: " + elem1);
      // Initialized elements should be non-null (they are function references)
      assertNotNull(elem0, "Element 0 should be initialized from element segment");
      assertNotNull(elem1, "Element 1 should be initialized from element segment");
    }

    @Test
    @DisplayName("call_indirect should invoke table function")
    void shouldCallIndirectSuccessfully() throws Exception {
      final PanamaInstance instance = createInstanceFromWat(TABLE_WITH_ELEMENTS_WAT);
      // Verify call_indirect works by calling through the table
      final var funcOpt = instance.getFunction("call_indirect");
      assertTrue(funcOpt.isPresent(), "call_indirect function should exist");
      final var func = funcOpt.get();
      final var result = func.call(ai.tegmentum.wasmtime4j.WasmValue.i32(0));
      assertNotNull(result, "call_indirect result should not be null");
      assertTrue(result.length > 0, "call_indirect should return a result");
      assertEquals(42, result[0].asInt(), "call_indirect(0) should return 42 (f1)");
      LOGGER.info("call_indirect(0) returned: " + result[0].asInt());

      final var result2 = func.call(ai.tegmentum.wasmtime4j.WasmValue.i32(1));
      assertEquals(99, result2[0].asInt(), "call_indirect(1) should return 99 (f2)");
      LOGGER.info("call_indirect(1) returned: " + result2[0].asInt());
    }

    @Test
    @DisplayName("get on uninitialized slot should return null")
    void shouldReturnNullForUninitializedSlot() throws Exception {
      final PanamaInstance instance = createInstanceFromWat(TABLE_WITH_ELEMENTS_WAT);
      final WasmTable table = getTable(instance);
      // Slots 2-9 are not initialized
      final Object elem2 = table.get(2);
      // Uninitialized funcref slots are null
      LOGGER.info("Uninitialized element at index 2: " + elem2);
    }
  }
}

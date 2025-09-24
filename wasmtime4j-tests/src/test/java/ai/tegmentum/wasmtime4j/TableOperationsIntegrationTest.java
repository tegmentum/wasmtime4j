package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Comprehensive integration tests for WebAssembly table operations across runtime implementations.
 *
 * <p>This test suite validates complete table functionality including: - Table creation with
 * different reference types (funcref, externref) - Basic table operations (get, set, size, grow) -
 * Bulk operations (fill, copy) - Cross-table operations - Error handling and bounds checking -
 * Reference type validation
 *
 * <p>Tests are designed to work with both JNI and Panama runtime implementations, ensuring
 * consistent behavior across all supported Java versions.
 */
@EnabledIfSystemProperty(named = "test.integration", matches = "true")
public class TableOperationsIntegrationTest {

  private static final Logger logger =
      Logger.getLogger(TableOperationsIntegrationTest.class.getName());

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    logger.info("Setting up test: " + testInfo.getDisplayName());

    try {
      runtime = WasmRuntimeFactory.createRuntime();
      engine = runtime.createEngine();
      store = runtime.createStore(engine);

      logger.info("Using runtime implementation: " + runtime.getClass().getSimpleName());
    } catch (Exception e) {
      logger.severe("Failed to set up test environment: " + e.getMessage());
      throw new RuntimeException("Test setup failed", e);
    }
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      try {
        store.close();
      } catch (Exception e) {
        logger.warning("Failed to close store: " + e.getMessage());
      }
    }

    if (engine != null) {
      try {
        engine.close();
      } catch (Exception e) {
        logger.warning("Failed to close engine: " + e.getMessage());
      }
    }

    if (runtime != null) {
      try {
        runtime.close();
      } catch (Exception e) {
        logger.warning("Failed to close runtime: " + e.getMessage());
      }
    }
  }

  @Test
  @DisplayName("Create table with funcref element type")
  void testCreateFuncRefTable() throws WasmException {
    final String wat =
        """
            (module
              (table $test_table 5 10 funcref)
              (export "test_table" (table $test_table))
            )
            """;

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);

    WasmTable table = instance.getTable("test_table");
    assertNotNull(table, "Table should not be null");
    assertEquals(5, table.getSize(), "Table size should be 5");
    assertEquals(10, table.getMaxSize(), "Table max size should be 10");
    assertEquals(
        WasmValueType.FUNCREF, table.getElementType(), "Table element type should be FUNCREF");

    logger.info("Successfully created funcref table with size 5 and max size 10");
  }

  @Test
  @DisplayName("Create table with externref element type")
  void testCreateExternRefTable() throws WasmException {
    final String wat =
        """
            (module
              (table $test_table 3 externref)
              (export "test_table" (table $test_table))
            )
            """;

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);

    WasmTable table = instance.getTable("test_table");
    assertNotNull(table, "Table should not be null");
    assertEquals(3, table.getSize(), "Table size should be 3");
    assertEquals(-1, table.getMaxSize(), "Table should have unlimited max size");
    assertEquals(
        WasmValueType.EXTERNREF, table.getElementType(), "Table element type should be EXTERNREF");

    logger.info("Successfully created externref table with size 3 and unlimited max size");
  }

  @Test
  @DisplayName("Test basic table get and set operations")
  void testTableGetSet() throws WasmException {
    final String wat =
        """
            (module
              (table $test_table 5 funcref)
              (export "test_table" (table $test_table))
            )
            """;

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table = instance.getTable("test_table");

    // Initially all elements should be null
    for (int i = 0; i < table.getSize(); i++) {
      Object element = table.get(i);
      assertNull(element, "Initial table element should be null at index " + i);
    }

    // Test setting and getting null (should work)
    table.set(0, null);
    assertNull(table.get(0), "Element should remain null after setting null");

    // Test bounds checking
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> table.get(10),
        "Getting element beyond table bounds should throw IndexOutOfBoundsException");
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> table.set(10, null),
        "Setting element beyond table bounds should throw IndexOutOfBoundsException");
    assertThrows(
        IllegalArgumentException.class,
        () -> table.get(-1),
        "Getting element with negative index should throw IllegalArgumentException");
    assertThrows(
        IllegalArgumentException.class,
        () -> table.set(-1, null),
        "Setting element with negative index should throw IllegalArgumentException");

    logger.info("Successfully tested basic table get/set operations with proper bounds checking");
  }

  @Test
  @DisplayName("Test table growth operations")
  void testTableGrow() throws WasmException {
    final String wat =
        """
            (module
              (table $test_table 2 10 funcref)
              (export "test_table" (table $test_table))
            )
            """;

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table = instance.getTable("test_table");

    assertEquals(2, table.getSize(), "Initial table size should be 2");

    // Test growing table
    int previousSize = table.grow(3, null);
    assertEquals(2, previousSize, "Previous size should be returned");
    assertEquals(5, table.getSize(), "Table size should be 5 after growth");

    // Verify new elements are initialized with null
    for (int i = 2; i < 5; i++) {
      assertNull(table.get(i), "New element should be null at index " + i);
    }

    // Test growing beyond maximum (should fail)
    int failResult = table.grow(10, null);
    assertEquals(-1, failResult, "Growing beyond maximum should return -1");
    assertEquals(5, table.getSize(), "Table size should remain 5 after failed growth");

    // Test growing with negative delta
    assertThrows(
        IllegalArgumentException.class,
        () -> table.grow(-1, null),
        "Growing with negative delta should throw IllegalArgumentException");

    logger.info("Successfully tested table growth operations with proper limit enforcement");
  }

  @Test
  @DisplayName("Test table fill operations")
  void testTableFill() throws WasmException {
    final String wat =
        """
            (module
              (table $test_table 10 externref)
              (export "test_table" (table $test_table))
            )
            """;

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table = instance.getTable("test_table");

    // Test filling with null
    table.fill(2, 5, null);

    // Verify elements before fill range are untouched
    assertNull(table.get(0), "Element before fill range should be null");
    assertNull(table.get(1), "Element before fill range should be null");

    // Verify fill range elements
    for (int i = 2; i < 7; i++) {
      assertNull(table.get(i), "Filled element should be null at index " + i);
    }

    // Verify elements after fill range are untouched
    assertNull(table.get(7), "Element after fill range should be null");

    // Test fill bounds checking
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> table.fill(8, 5, null),
        "Fill operation exceeding table bounds should throw IndexOutOfBoundsException");
    assertThrows(
        IllegalArgumentException.class,
        () -> table.fill(-1, 3, null),
        "Fill with negative start should throw IllegalArgumentException");
    assertThrows(
        IllegalArgumentException.class,
        () -> table.fill(2, -1, null),
        "Fill with negative count should throw IllegalArgumentException");

    logger.info("Successfully tested table fill operations with proper bounds checking");
  }

  @Test
  @DisplayName("Test table copy within same table")
  void testTableCopyWithin() throws WasmException {
    final String wat =
        """
            (module
              (table $test_table 10 funcref)
              (export "test_table" (table $test_table))
            )
            """;

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table = instance.getTable("test_table");

    // Since we can't easily create function references in this test,
    // we'll test copying null references and verify bounds checking

    // Test copying within table (should work even with null elements)
    table.copy(5, 0, 3); // Copy 3 elements from index 0-2 to index 5-7

    // Test copy bounds checking
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> table.copy(8, 0, 5),
        "Copy destination exceeding bounds should throw IndexOutOfBoundsException");
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> table.copy(0, 8, 5),
        "Copy source exceeding bounds should throw IndexOutOfBoundsException");
    assertThrows(
        IllegalArgumentException.class,
        () -> table.copy(-1, 0, 3),
        "Copy with negative destination should throw IllegalArgumentException");
    assertThrows(
        IllegalArgumentException.class,
        () -> table.copy(0, -1, 3),
        "Copy with negative source should throw IllegalArgumentException");
    assertThrows(
        IllegalArgumentException.class,
        () -> table.copy(0, 0, -1),
        "Copy with negative count should throw IllegalArgumentException");

    logger.info("Successfully tested table copy within same table with proper bounds checking");
  }

  @Test
  @DisplayName("Test table copy between different tables")
  void testTableCopyBetweenTables() throws WasmException {
    final String wat =
        """
            (module
              (table $table1 5 funcref)
              (table $table2 7 funcref)
              (export "table1" (table $table1))
              (export "table2" (table $table2))
            )
            """;

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table1 = instance.getTable("table1");
    WasmTable table2 = instance.getTable("table2");

    // Test copying between compatible tables
    table1.copy(1, table2, 2, 3); // Copy 3 elements from table2[2:5] to table1[1:4]

    // Test cross-table copy bounds checking
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> table1.copy(3, table2, 0, 5),
        "Cross-table copy with destination exceeding bounds should throw"
            + " IndexOutOfBoundsException");
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> table1.copy(0, table2, 5, 5),
        "Cross-table copy with source exceeding bounds should throw IndexOutOfBoundsException");
    assertThrows(
        IllegalArgumentException.class,
        () -> table1.copy(-1, table2, 0, 3),
        "Cross-table copy with negative destination should throw IllegalArgumentException");
    assertThrows(
        IllegalArgumentException.class,
        () -> table1.copy(0, table2, -1, 3),
        "Cross-table copy with negative source should throw IllegalArgumentException");
    assertThrows(
        IllegalArgumentException.class,
        () -> table1.copy(0, table2, 0, -1),
        "Cross-table copy with negative count should throw IllegalArgumentException");
    assertThrows(
        IllegalArgumentException.class,
        () -> table1.copy(0, null, 0, 3),
        "Cross-table copy with null source should throw IllegalArgumentException");

    logger.info("Successfully tested cross-table copy operations with proper bounds checking");
  }

  @Test
  @DisplayName("Test table type compatibility in cross-table operations")
  void testTableTypeCompatibility() throws WasmException {
    final String wat =
        """
            (module
              (table $funcref_table 5 funcref)
              (table $externref_table 5 externref)
              (export "funcref_table" (table $funcref_table))
              (export "externref_table" (table $externref_table))
            )
            """;

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable funcrefTable = instance.getTable("funcref_table");
    WasmTable externrefTable = instance.getTable("externref_table");

    // Test that copying between incompatible table types fails
    assertThrows(
        IllegalArgumentException.class,
        () -> funcrefTable.copy(0, externrefTable, 0, 2),
        "Copying between incompatible table types should throw IllegalArgumentException");

    logger.info("Successfully tested table type compatibility enforcement");
  }

  @Test
  @DisplayName("Test table operations with invalid parameters")
  void testTableInvalidParameters() throws WasmException {
    final String wat =
        """
            (module
              (table $test_table 5 funcref)
              (export "test_table" (table $test_table))
            )
            """;

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table = instance.getTable("test_table");

    // Test all operations with various invalid parameters
    assertThrows(IllegalArgumentException.class, () -> table.get(-1));
    assertThrows(IllegalArgumentException.class, () -> table.set(-1, null));
    assertThrows(IllegalArgumentException.class, () -> table.grow(-1, null));
    assertThrows(IllegalArgumentException.class, () -> table.fill(-1, 1, null));
    assertThrows(IllegalArgumentException.class, () -> table.fill(0, -1, null));
    assertThrows(IllegalArgumentException.class, () -> table.copy(-1, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> table.copy(0, -1, 1));
    assertThrows(IllegalArgumentException.class, () -> table.copy(0, 0, -1));

    // Test bounds checking
    assertThrows(IndexOutOfBoundsException.class, () -> table.get(10));
    assertThrows(IndexOutOfBoundsException.class, () -> table.set(10, null));
    assertThrows(IndexOutOfBoundsException.class, () -> table.fill(4, 5, null));
    assertThrows(IndexOutOfBoundsException.class, () -> table.copy(4, 0, 5));
    assertThrows(IndexOutOfBoundsException.class, () -> table.copy(0, 4, 5));

    logger.info("Successfully validated error handling for invalid parameters");
  }

  @Test
  @DisplayName("Test table operations with extreme values")
  void testTableExtremeValues() throws WasmException {
    final String wat =
        """
            (module
              (table $test_table 1 funcref)
              (export "test_table" (table $test_table))
            )
            """;

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table = instance.getTable("test_table");

    // Test operations with zero count (should be no-op)
    table.fill(0, 0, null); // Should not throw
    table.copy(0, 0, 0); // Should not throw

    // Test operations at exact bounds
    table.get(0); // Should not throw
    table.set(0, null); // Should not throw
    table.fill(0, 1, null); // Should not throw
    table.copy(0, 0, 1); // Should not throw

    logger.info("Successfully tested table operations with extreme values");
  }

  @Test
  @DisplayName("Test table metadata consistency")
  void testTableMetadataConsistency() throws WasmException {
    final String wat =
        """
            (module
              (table $test_table 3 8 externref)
              (export "test_table" (table $test_table))
            )
            """;

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable table = instance.getTable("test_table");

    // Verify metadata consistency
    assertEquals(3, table.getSize(), "Size should be consistent");
    assertEquals(8, table.getMaxSize(), "Max size should be consistent");
    assertEquals(
        WasmValueType.EXTERNREF, table.getElementType(), "Element type should be consistent");
    assertEquals(WasmValueType.EXTERNREF, table.getType(), "Type should match element type");

    // Test size consistency after growth
    table.grow(2, null);
    assertEquals(5, table.getSize(), "Size should be updated after growth");
    assertEquals(8, table.getMaxSize(), "Max size should remain unchanged");
    assertEquals(
        WasmValueType.EXTERNREF, table.getElementType(), "Element type should remain unchanged");

    logger.info("Successfully verified table metadata consistency");
  }

  @Test
  @DisplayName("Test comprehensive table workflow")
  void testComprehensiveTableWorkflow() throws WasmException {
    final String wat =
        """
            (module
              (table $main_table 4 20 funcref)
              (table $aux_table 6 funcref)
              (export "main_table" (table $main_table))
              (export "aux_table" (table $aux_table))
            )
            """;

    Module module = runtime.compileModule(wat);
    Instance instance = runtime.instantiate(module, store);
    WasmTable mainTable = instance.getTable("main_table");
    WasmTable auxTable = instance.getTable("aux_table");

    // Step 1: Verify initial state
    assertEquals(4, mainTable.getSize());
    assertEquals(6, auxTable.getSize());
    for (int i = 0; i < mainTable.getSize(); i++) {
      assertNull(mainTable.get(i), "Initial element should be null");
    }

    // Step 2: Fill sections with null (test bulk operations)
    mainTable.fill(1, 2, null);
    auxTable.fill(0, 3, null);

    // Step 3: Test growth
    int oldSize = mainTable.grow(3, null);
    assertEquals(4, oldSize);
    assertEquals(7, mainTable.getSize());

    // Step 4: Test copy within table
    mainTable.copy(5, 1, 2);

    // Step 5: Test cross-table copy (both should be funcref)
    mainTable.copy(2, auxTable, 1, 2);

    // Step 6: Verify final state consistency
    assertEquals(7, mainTable.getSize());
    assertEquals(6, auxTable.getSize());
    assertEquals(WasmValueType.FUNCREF, mainTable.getElementType());
    assertEquals(WasmValueType.FUNCREF, auxTable.getElementType());

    logger.info("Successfully completed comprehensive table workflow test");
  }
}

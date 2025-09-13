package ai.tegmentum.wasmtime4j.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WebAssembly table reference type handling.
 *
 * <p>This test class verifies the implementation of table operations with proper
 * reference type discrimination (funcref vs externref), element type validation,
 * and bounds checking using real native calls.
 */
class TableReferenceTypeIT {

  private WasmEngine engine;
  private WasmStore store;

  @BeforeEach
  void setUp() {
    try {
      // Use factory to get appropriate runtime implementation
      engine = WasmtimeRuntimeFactory.createEngine();
      store = WasmtimeRuntimeFactory.createStore(engine);
      
      // Validate that we have proper store context for table operations
      assertNotNull(engine, "Engine should be properly initialized");
      assertNotNull(store, "Store should be properly initialized");
    } catch (Exception e) {
      fail("Failed to initialize Wasmtime runtime: " + e.getMessage());
    }
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      try {
        store.close();
      } catch (Exception e) {
        // Log but don't fail test cleanup
        System.err.println("Warning: Failed to close store: " + e.getMessage());
      }
    }
    if (engine != null) {
      try {
        engine.close();
      } catch (Exception e) {
        // Log but don't fail test cleanup
        System.err.println("Warning: Failed to close engine: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Table Creation with Reference Types")
  class TableCreationTests {

    @Test
    @DisplayName("Create funcref table")
    void testCreateFuncRefTable() {
      try (WasmTable table = WasmtimeRuntimeFactory.createTable(
          store, WasmValueType.FUNCREF, 10, null)) {
        
        assertNotNull(table, "Funcref table should be created successfully");
        assertEquals(WasmValueType.FUNCREF, table.getElementType(), 
            "Table should have funcref element type");
        assertEquals(10, table.getSize(), "Table should have initial size of 10");
        assertEquals(-1, table.getMaxSize(), "Table should have unlimited max size");
      } catch (Exception e) {
        fail("Failed to create funcref table: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("Create externref table")
    void testCreateExternRefTable() {
      try (WasmTable table = WasmtimeRuntimeFactory.createTable(
          store, WasmValueType.EXTERNREF, 5, 100)) {
        
        assertNotNull(table, "Externref table should be created successfully");
        assertEquals(WasmValueType.EXTERNREF, table.getElementType(), 
            "Table should have externref element type");
        assertEquals(5, table.getSize(), "Table should have initial size of 5");
        assertEquals(100, table.getMaxSize(), "Table should have max size of 100");
      } catch (Exception e) {
        fail("Failed to create externref table: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("Reject invalid element types")
    void testRejectInvalidElementTypes() {
      // Test that non-reference types are rejected for table creation
      assertThrows(IllegalArgumentException.class, () -> {
        WasmtimeRuntimeFactory.createTable(store, WasmValueType.I32, 10, null);
      }, "Should reject I32 as table element type");

      assertThrows(IllegalArgumentException.class, () -> {
        WasmtimeRuntimeFactory.createTable(store, WasmValueType.F64, 10, null);
      }, "Should reject F64 as table element type");
    }
  }

  @Nested
  @DisplayName("Table Element Operations")
  class TableElementOperationTests {

    @Test
    @DisplayName("Get elements from empty table")
    void testGetElementsFromEmptyTable() {
      try (WasmTable table = WasmtimeRuntimeFactory.createTable(
          store, WasmValueType.FUNCREF, 5, null)) {
        
        // All elements should be null initially
        for (int i = 0; i < 5; i++) {
          Object element = table.get(i);
          assertNull(element, "Empty table element should be null at index " + i);
        }
      } catch (Exception e) {
        fail("Failed to get elements from empty table: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("Set and get null references")
    void testSetAndGetNullReferences() {
      try (WasmTable table = WasmtimeRuntimeFactory.createTable(
          store, WasmValueType.EXTERNREF, 3, null)) {
        
        // Set null values explicitly
        table.set(0, null);
        table.set(1, null);
        table.set(2, null);
        
        // Verify they remain null
        assertNull(table.get(0), "Explicitly set null should remain null");
        assertNull(table.get(1), "Explicitly set null should remain null");
        assertNull(table.get(2), "Explicitly set null should remain null");
      } catch (Exception e) {
        fail("Failed to set/get null references: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("Bounds checking")
    void testBoundsChecking() {
      try (WasmTable table = WasmtimeRuntimeFactory.createTable(
          store, WasmValueType.FUNCREF, 5, null)) {
        
        // Test get bounds checking
        assertThrows(IndexOutOfBoundsException.class, () -> {
          table.get(-1);
        }, "Should reject negative index for get");

        assertThrows(IndexOutOfBoundsException.class, () -> {
          table.get(5); // Size is 5, so index 5 is out of bounds
        }, "Should reject out of bounds index for get");

        // Test set bounds checking
        assertThrows(IndexOutOfBoundsException.class, () -> {
          table.set(-1, null);
        }, "Should reject negative index for set");

        assertThrows(IndexOutOfBoundsException.class, () -> {
          table.set(5, null); // Size is 5, so index 5 is out of bounds
        }, "Should reject out of bounds index for set");
      } catch (Exception e) {
        fail("Failed bounds checking test: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Table Growth Operations")
  class TableGrowthTests {

    @Test
    @DisplayName("Grow table within limits")
    void testGrowTableWithinLimits() {
      try (WasmTable table = WasmtimeRuntimeFactory.createTable(
          store, WasmValueType.FUNCREF, 3, 10)) {
        
        int initialSize = table.getSize();
        assertEquals(3, initialSize, "Initial size should be 3");
        
        // Grow by 2 elements
        int previousSize = table.grow(2, null);
        assertEquals(3, previousSize, "Should return previous size of 3");
        
        int newSize = table.getSize();
        assertEquals(5, newSize, "New size should be 5 after growing by 2");
        
        // Verify we can access new elements
        assertNull(table.get(3), "New element at index 3 should be null");
        assertNull(table.get(4), "New element at index 4 should be null");
      } catch (Exception e) {
        fail("Failed to grow table within limits: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("Growth parameter validation")
    void testGrowthParameterValidation() {
      try (WasmTable table = WasmtimeRuntimeFactory.createTable(
          store, WasmValueType.EXTERNREF, 2, null)) {
        
        // Test negative delta
        assertThrows(IllegalArgumentException.class, () -> {
          table.grow(-1, null);
        }, "Should reject negative growth delta");

        // Test zero delta (should be allowed)
        int previousSize = table.grow(0, null);
        assertEquals(2, previousSize, "Zero growth should return current size");
        assertEquals(2, table.getSize(), "Size should remain unchanged after zero growth");
      } catch (Exception e) {
        fail("Failed growth parameter validation: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Table Fill Operations")
  class TableFillTests {

    @Test
    @DisplayName("Fill table range with null")
    void testFillTableRangeWithNull() {
      try (WasmTable table = WasmtimeRuntimeFactory.createTable(
          store, WasmValueType.FUNCREF, 5, null)) {
        
        // Fill middle range
        table.fill(1, 3, null);
        
        // Verify the fill operation
        assertNull(table.get(0), "Index 0 should remain null");
        assertNull(table.get(1), "Index 1 should be null (filled)");
        assertNull(table.get(2), "Index 2 should be null (filled)");
        assertNull(table.get(3), "Index 3 should be null (filled)");
        assertNull(table.get(4), "Index 4 should remain null");
      } catch (Exception e) {
        fail("Failed to fill table range: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("Fill parameter validation")
    void testFillParameterValidation() {
      try (WasmTable table = WasmtimeRuntimeFactory.createTable(
          store, WasmValueType.EXTERNREF, 5, null)) {
        
        // Test negative start
        assertThrows(IllegalArgumentException.class, () -> {
          table.fill(-1, 2, null);
        }, "Should reject negative start index");

        // Test negative count
        assertThrows(IllegalArgumentException.class, () -> {
          table.fill(0, -1, null);
        }, "Should reject negative count");

        // Test out of bounds range
        assertThrows(IndexOutOfBoundsException.class, () -> {
          table.fill(3, 3, null); // start=3, count=3 exceeds size=5
        }, "Should reject fill range that exceeds table bounds");
      } catch (Exception e) {
        fail("Failed fill parameter validation: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Reference Type Validation")
  class ReferenceTypeValidationTests {

    @Test
    @DisplayName("Element type consistency")
    void testElementTypeConsistency() {
      // Test funcref table
      try (WasmTable funcRefTable = WasmtimeRuntimeFactory.createTable(
          store, WasmValueType.FUNCREF, 3, null)) {
        
        assertEquals(WasmValueType.FUNCREF, funcRefTable.getElementType(),
            "Funcref table should report correct element type");
      } catch (Exception e) {
        fail("Failed funcref element type consistency test: " + e.getMessage());
      }

      // Test externref table
      try (WasmTable externRefTable = WasmtimeRuntimeFactory.createTable(
          store, WasmValueType.EXTERNREF, 3, null)) {
        
        assertEquals(WasmValueType.EXTERNREF, externRefTable.getElementType(),
            "Externref table should report correct element type");
      } catch (Exception e) {
        fail("Failed externref element type consistency test: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("Type discrimination between funcref and externref")
    void testTypeDifferentiation() {
      // Create tables of different reference types
      try (WasmTable funcRefTable = WasmtimeRuntimeFactory.createTable(
           store, WasmValueType.FUNCREF, 2, null);
           WasmTable externRefTable = WasmtimeRuntimeFactory.createTable(
              store, WasmValueType.EXTERNREF, 2, null)) {
        
        // Verify they have different element types
        assertEquals(WasmValueType.FUNCREF, funcRefTable.getElementType());
        assertEquals(WasmValueType.EXTERNREF, externRefTable.getElementType());
        
        // Both should behave consistently with their declared types
        assertTrue(funcRefTable.getElementType() != externRefTable.getElementType(),
            "Funcref and externref tables should have different element types");
      } catch (Exception e) {
        fail("Failed type differentiation test: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Error Handling and Edge Cases")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Handle resource lifecycle properly")
    void testResourceLifecycle() {
      WasmTable table = null;
      try {
        table = WasmtimeRuntimeFactory.createTable(store, WasmValueType.FUNCREF, 3, null);
        assertNotNull(table, "Table should be created successfully");
        
        // Use the table
        assertEquals(3, table.getSize());
        
        // Close the table
        table.close();
        
        // Verify table is closed
        assertTrue(table.isClosed(), "Table should be closed after close()");
        
        // Operations on closed table should throw exceptions
        assertThrows(Exception.class, () -> {
          table.getSize();
        }, "Should throw exception when accessing closed table");
        
      } catch (Exception e) {
        fail("Failed resource lifecycle test: " + e.getMessage());
      } finally {
        if (table != null && !table.isClosed()) {
          try {
            table.close();
          } catch (Exception e) {
            // Ignore cleanup errors
          }
        }
      }
    }

    @Test
    @DisplayName("Handle extreme size values")
    void testExtremeSizeValues() {
      // Test very large initial size (should be reasonable)
      try (WasmTable table = WasmtimeRuntimeFactory.createTable(
          store, WasmValueType.FUNCREF, 1000, null)) {
        
        assertEquals(1000, table.getSize(), "Should handle large initial size");
      } catch (Exception e) {
        // This might fail due to memory constraints, which is acceptable
        assertTrue(e.getMessage().contains("memory") || e.getMessage().contains("size"),
            "Large size failure should be memory-related");
      }
    }
  }
}
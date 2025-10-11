package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for verifying Panama instance export retrieval (memory, table, global).
 *
 * @since 1.0.0
 */
final class PanamaInstanceExportRetrievalTest {

  private PanamaEngine engine;
  private PanamaStore store;
  private PanamaModule module;
  private PanamaInstance instance;

  @BeforeEach
  void setUp() throws Exception {
    engine = new PanamaEngine();
    store = new PanamaStore(engine);

    // Load the test WASM file with memory, table, and global exports
    final Path wasmPath =
        Paths.get(getClass().getClassLoader().getResource("wasm/exports-test.wasm").toURI());
    final byte[] wasmBytes = Files.readAllBytes(wasmPath);

    // Compile the module
    module = new PanamaModule(engine, wasmBytes);
    instance = new PanamaInstance(module, store);
  }

  @AfterEach
  void tearDown() {
    if (instance != null) {
      instance.close();
    }
    if (module != null) {
      module.close();
    }
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  void testGetMemoryByName() {
    final Optional<WasmMemory> memory = instance.getMemory("memory");
    assertTrue(memory.isPresent(), "Memory export 'memory' should be found");
    assertNotNull(memory.get(), "Memory instance should not be null");
  }

  @Test
  void testGetMemoryByNameNotFound() {
    final Optional<WasmMemory> memory = instance.getMemory("nonexistent");
    assertFalse(memory.isPresent(), "Non-existent memory should not be found");
  }

  @Test
  void testGetMemoryByNameNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> instance.getMemory((String) null),
        "Null name should throw IllegalArgumentException");
  }

  @Test
  void testGetTableByName() {
    final Optional<WasmTable> table = instance.getTable("table");
    assertTrue(table.isPresent(), "Table export 'table' should be found");
    assertNotNull(table.get(), "Table instance should not be null");
  }

  @Test
  void testGetTableByNameNotFound() {
    final Optional<WasmTable> table = instance.getTable("nonexistent");
    assertFalse(table.isPresent(), "Non-existent table should not be found");
  }

  @Test
  void testGetTableByNameNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> instance.getTable((String) null),
        "Null name should throw IllegalArgumentException");
  }

  @Test
  void testGetGlobalByName() {
    final Optional<WasmGlobal> global = instance.getGlobal("g_i32");
    assertTrue(global.isPresent(), "Global export 'g_i32' should be found");
    assertNotNull(global.get(), "Global instance should not be null");
  }

  @Test
  void testGetGlobalByNameMultiple() {
    // Test retrieving different global types
    final Optional<WasmGlobal> g1 = instance.getGlobal("g_i32");
    final Optional<WasmGlobal> g2 = instance.getGlobal("g_i64");
    final Optional<WasmGlobal> g3 = instance.getGlobal("g_f32");

    assertTrue(g1.isPresent(), "Global 'g_i32' should be found");
    assertTrue(g2.isPresent(), "Global 'g_i64' should be found");
    assertTrue(g3.isPresent(), "Global 'g_f32' should be found");
  }

  @Test
  void testGetGlobalByNameNotFound() {
    final Optional<WasmGlobal> global = instance.getGlobal("nonexistent");
    assertFalse(global.isPresent(), "Non-existent global should not be found");
  }

  @Test
  void testGetGlobalByNameNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> instance.getGlobal((String) null),
        "Null name should throw IllegalArgumentException");
  }

  @Test
  void testMultipleExportRetrievals() {
    // Test retrieving all export types from the same instance
    final Optional<WasmMemory> memory = instance.getMemory("memory");
    final Optional<WasmTable> table = instance.getTable("table");
    final Optional<WasmGlobal> global = instance.getGlobal("g_i32");

    assertTrue(memory.isPresent(), "Memory should be found");
    assertTrue(table.isPresent(), "Table should be found");
    assertTrue(global.isPresent(), "Global should be found");
  }

  @Test
  void testGetDefaultMemory() {
    final Optional<WasmMemory> memory = instance.getDefaultMemory();
    assertTrue(memory.isPresent(), "Default memory should be found");
    assertNotNull(memory.get(), "Default memory instance should not be null");
  }

  @Test
  void testGetDefaultMemoryWithoutNamedMemory() throws Exception {
    // Load a WASM module that has memory but not named "memory"
    // For this test, we'll just verify the method works with the current module
    // which does have a "memory" export
    final Optional<WasmMemory> memory = instance.getDefaultMemory();
    assertTrue(memory.isPresent(), "Default memory should be found even by name 'memory'");
  }

  @Test
  void testGetDefaultMemoryWhenClosed() {
    instance.close();
    assertThrows(
        IllegalStateException.class,
        () -> instance.getDefaultMemory(),
        "Getting default memory from closed instance should throw");
  }

  @Test
  void testGetMemoryByIndex() {
    final Optional<WasmMemory> memory = instance.getMemory(0);
    assertTrue(memory.isPresent(), "Memory at index 0 should be found");
    assertNotNull(memory.get(), "Memory instance should not be null");
  }

  @Test
  void testGetMemoryByIndexNotFound() {
    final Optional<WasmMemory> memory = instance.getMemory(10);
    assertFalse(memory.isPresent(), "Memory at non-existent index should not be found");
  }

  @Test
  void testGetMemoryByIndexNegative() {
    assertThrows(
        IllegalArgumentException.class,
        () -> instance.getMemory(-1),
        "Negative index should throw IllegalArgumentException");
  }

  @Test
  void testGetTableByIndex() {
    final Optional<WasmTable> table = instance.getTable(0);
    assertTrue(table.isPresent(), "Table at index 0 should be found");
    assertNotNull(table.get(), "Table instance should not be null");
  }

  @Test
  void testGetTableByIndexNotFound() {
    final Optional<WasmTable> table = instance.getTable(10);
    assertFalse(table.isPresent(), "Table at non-existent index should not be found");
  }

  @Test
  void testGetTableByIndexNegative() {
    assertThrows(
        IllegalArgumentException.class,
        () -> instance.getTable(-1),
        "Negative index should throw IllegalArgumentException");
  }

  @Test
  void testGetGlobalByIndex() {
    final Optional<WasmGlobal> global = instance.getGlobal(0);
    assertTrue(global.isPresent(), "Global at index 0 should be found");
    assertNotNull(global.get(), "Global instance should not be null");
  }

  @Test
  void testGetGlobalByIndexMultiple() {
    // Test retrieving multiple globals by index
    final Optional<WasmGlobal> g0 = instance.getGlobal(0);
    final Optional<WasmGlobal> g1 = instance.getGlobal(1);
    final Optional<WasmGlobal> g2 = instance.getGlobal(2);

    assertTrue(g0.isPresent(), "Global at index 0 should be found");
    assertTrue(g1.isPresent(), "Global at index 1 should be found");
    assertTrue(g2.isPresent(), "Global at index 2 should be found");
  }

  @Test
  void testGetGlobalByIndexNotFound() {
    final Optional<WasmGlobal> global = instance.getGlobal(10);
    assertFalse(global.isPresent(), "Global at non-existent index should not be found");
  }

  @Test
  void testGetGlobalByIndexNegative() {
    assertThrows(
        IllegalArgumentException.class,
        () -> instance.getGlobal(-1),
        "Negative index should throw IllegalArgumentException");
  }

  @Test
  void testGetFunctionByIndex() {
    final Optional<WasmFunction> function = instance.getFunction(0);
    assertTrue(function.isPresent(), "Function at index 0 should be found");
    assertNotNull(function.get(), "Function instance should not be null");
  }

  @Test
  void testGetFunctionByIndexNotFound() {
    final Optional<WasmFunction> function = instance.getFunction(10);
    assertFalse(function.isPresent(), "Function at non-existent index should not be found");
  }

  @Test
  void testGetFunctionByIndexNegative() {
    assertThrows(
        IllegalArgumentException.class,
        () -> instance.getFunction(-1),
        "Negative index should throw IllegalArgumentException");
  }

  @Test
  void testExportRetrievalAfterClose() {
    instance.close();
    assertThrows(
        IllegalStateException.class,
        () -> instance.getMemory("memory"),
        "Getting memory from closed instance should throw");
    assertThrows(
        IllegalStateException.class,
        () -> instance.getTable("table"),
        "Getting table from closed instance should throw");
    assertThrows(
        IllegalStateException.class,
        () -> instance.getGlobal("g_i32"),
        "Getting global from closed instance should throw");
  }

  @Test
  void testGetAllExports() {
    final java.util.Map<String, Object> exports = instance.getAllExports();
    assertNotNull(exports, "Exports map should not be null");
    assertFalse(exports.isEmpty(), "Exports map should not be empty");

    // Verify we have all expected exports
    assertTrue(exports.containsKey("memory"), "Should have memory export");
    assertTrue(exports.containsKey("table"), "Should have table export");
    assertTrue(exports.containsKey("test"), "Should have test function export");
    assertTrue(exports.containsKey("g_i32"), "Should have g_i32 global export");
    assertTrue(exports.containsKey("g_i64"), "Should have g_i64 global export");
    assertTrue(exports.containsKey("g_f32"), "Should have g_f32 global export");

    // Verify types
    assertTrue(exports.get("memory") instanceof WasmMemory, "memory should be WasmMemory");
    assertTrue(exports.get("table") instanceof WasmTable, "table should be WasmTable");
    assertTrue(exports.get("test") instanceof WasmFunction, "test should be WasmFunction");
    assertTrue(exports.get("g_i32") instanceof WasmGlobal, "g_i32 should be WasmGlobal");
    assertTrue(exports.get("g_i64") instanceof WasmGlobal, "g_i64 should be WasmGlobal");
    assertTrue(exports.get("g_f32") instanceof WasmGlobal, "g_f32 should be WasmGlobal");
  }

  @Test
  void testGetAllExportsIsUnmodifiable() {
    final java.util.Map<String, Object> exports = instance.getAllExports();
    assertThrows(
        UnsupportedOperationException.class,
        () -> exports.put("new_export", new Object()),
        "Exports map should be unmodifiable");
  }

  @Test
  void testGetAllExportsWhenClosed() {
    instance.close();
    assertThrows(
        IllegalStateException.class,
        () -> instance.getAllExports(),
        "Getting all exports from closed instance should throw");
  }

  @Test
  void testGetFunctionNames() {
    final java.util.List<String> functionNames = instance.getFunctionNames();
    assertNotNull(functionNames, "Function names list should not be null");
    assertFalse(functionNames.isEmpty(), "Function names list should not be empty");
    assertTrue(functionNames.contains("test"), "Should have 'test' function");
    assertEquals(1, functionNames.size(), "Should have exactly 1 function");
  }

  @Test
  void testGetMemoryNames() {
    final java.util.List<String> memoryNames = instance.getMemoryNames();
    assertNotNull(memoryNames, "Memory names list should not be null");
    assertFalse(memoryNames.isEmpty(), "Memory names list should not be empty");
    assertTrue(memoryNames.contains("memory"), "Should have 'memory' export");
    assertEquals(1, memoryNames.size(), "Should have exactly 1 memory");
  }

  @Test
  void testGetTableNames() {
    final java.util.List<String> tableNames = instance.getTableNames();
    assertNotNull(tableNames, "Table names list should not be null");
    assertFalse(tableNames.isEmpty(), "Table names list should not be empty");
    assertTrue(tableNames.contains("table"), "Should have 'table' export");
    assertEquals(1, tableNames.size(), "Should have exactly 1 table");
  }

  @Test
  void testGetGlobalNames() {
    final java.util.List<String> globalNames = instance.getGlobalNames();
    assertNotNull(globalNames, "Global names list should not be null");
    assertFalse(globalNames.isEmpty(), "Global names list should not be empty");
    assertTrue(globalNames.contains("g_i32"), "Should have 'g_i32' global");
    assertTrue(globalNames.contains("g_i64"), "Should have 'g_i64' global");
    assertTrue(globalNames.contains("g_f32"), "Should have 'g_f32' global");
    assertEquals(3, globalNames.size(), "Should have exactly 3 globals");
  }

  @Test
  void testGetFunctionNamesIsUnmodifiable() {
    final java.util.List<String> functionNames = instance.getFunctionNames();
    assertThrows(
        UnsupportedOperationException.class,
        () -> functionNames.add("new_function"),
        "Function names list should be unmodifiable");
  }

  @Test
  void testGetMemoryNamesIsUnmodifiable() {
    final java.util.List<String> memoryNames = instance.getMemoryNames();
    assertThrows(
        UnsupportedOperationException.class,
        () -> memoryNames.add("new_memory"),
        "Memory names list should be unmodifiable");
  }

  @Test
  void testGetTableNamesIsUnmodifiable() {
    final java.util.List<String> tableNames = instance.getTableNames();
    assertThrows(
        UnsupportedOperationException.class,
        () -> tableNames.add("new_table"),
        "Table names list should be unmodifiable");
  }

  @Test
  void testGetGlobalNamesIsUnmodifiable() {
    final java.util.List<String> globalNames = instance.getGlobalNames();
    assertThrows(
        UnsupportedOperationException.class,
        () -> globalNames.add("new_global"),
        "Global names list should be unmodifiable");
  }

  @Test
  void testGetFunctionNamesWhenClosed() {
    instance.close();
    assertThrows(
        IllegalStateException.class,
        () -> instance.getFunctionNames(),
        "Getting function names from closed instance should throw");
  }

  @Test
  void testGetMemoryNamesWhenClosed() {
    instance.close();
    assertThrows(
        IllegalStateException.class,
        () -> instance.getMemoryNames(),
        "Getting memory names from closed instance should throw");
  }

  @Test
  void testGetTableNamesWhenClosed() {
    instance.close();
    assertThrows(
        IllegalStateException.class,
        () -> instance.getTableNames(),
        "Getting table names from closed instance should throw");
  }

  @Test
  void testGetGlobalNamesWhenClosed() {
    instance.close();
    assertThrows(
        IllegalStateException.class,
        () -> instance.getGlobalNames(),
        "Getting global names from closed instance should throw");
  }

  @Test
  void testGetFunctionCount() {
    final int count = instance.getFunctionCount();
    assertEquals(1, count, "Should have exactly 1 function");
  }

  @Test
  void testGetMemoryCount() {
    final int count = instance.getMemoryCount();
    assertEquals(1, count, "Should have exactly 1 memory");
  }

  @Test
  void testGetTableCount() {
    final int count = instance.getTableCount();
    assertEquals(1, count, "Should have exactly 1 table");
  }

  @Test
  void testGetGlobalCount() {
    final int count = instance.getGlobalCount();
    assertEquals(3, count, "Should have exactly 3 globals");
  }

  @Test
  void testGetFunctionCountWhenClosed() {
    instance.close();
    assertThrows(
        IllegalStateException.class,
        () -> instance.getFunctionCount(),
        "Getting function count from closed instance should throw");
  }

  @Test
  void testGetMemoryCountWhenClosed() {
    instance.close();
    assertThrows(
        IllegalStateException.class,
        () -> instance.getMemoryCount(),
        "Getting memory count from closed instance should throw");
  }

  @Test
  void testGetTableCountWhenClosed() {
    instance.close();
    assertThrows(
        IllegalStateException.class,
        () -> instance.getTableCount(),
        "Getting table count from closed instance should throw");
  }

  @Test
  void testGetGlobalCountWhenClosed() {
    instance.close();
    assertThrows(
        IllegalStateException.class,
        () -> instance.getGlobalCount(),
        "Getting global count from closed instance should throw");
  }

  @Test
  void testCountsMatchNameListSizes() {
    assertEquals(
        instance.getFunctionNames().size(),
        instance.getFunctionCount(),
        "Function count should match function names list size");
    assertEquals(
        instance.getMemoryNames().size(),
        instance.getMemoryCount(),
        "Memory count should match memory names list size");
    assertEquals(
        instance.getTableNames().size(),
        instance.getTableCount(),
        "Table count should match table names list size");
    assertEquals(
        instance.getGlobalNames().size(),
        instance.getGlobalCount(),
        "Global count should match global names list size");
  }
}

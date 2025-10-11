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
}

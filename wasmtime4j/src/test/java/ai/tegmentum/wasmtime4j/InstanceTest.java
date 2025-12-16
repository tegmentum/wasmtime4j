package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Instance interface.
 *
 * <p>Tests verify instance creation, export retrieval, function calls, and resource lifecycle
 * management.
 */
@DisplayName("Instance Interface Tests")
class InstanceTest {

  /** Minimal valid WebAssembly module (empty module). */
  private static final byte[] MINIMAL_WASM = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};

  /** Simple WebAssembly module that exports an add function. */
  private static final String SIMPLE_ADD_WAT =
      "(module\n"
          + "  (func $add (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add))\n";

  /** Module with exported memory. */
  private static final String MEMORY_WAT = "(module\n" + "  (memory (export \"memory\") 1 10))\n";

  /** Module with exported global. */
  private static final String GLOBAL_WAT =
      "(module\n" + "  (global $g (export \"counter\") (mut i32) (i32.const 42)))\n";

  /** Module with exported table. */
  private static final String TABLE_WAT =
      "(module\n" + "  (table (export \"table\") 10 funcref))\n";

  /** Module with multiple exports. */
  private static final String MULTI_EXPORT_WAT =
      "(module\n"
          + "  (memory (export \"memory\") 1)\n"
          + "  (global $g (export \"counter\") (mut i32) (i32.const 0))\n"
          + "  (func $get (export \"get\") (result i32)\n"
          + "    global.get $g)\n"
          + "  (func $set (export \"set\") (param i32)\n"
          + "    local.get 0\n"
          + "    global.set $g))\n";

  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp() throws WasmException {
    engine = Engine.create();
    store = engine.createStore();
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
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance from minimal module")
    void shouldCreateInstanceFromMinimalModule() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final Instance instance = store.createInstance(module);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should create instance from WAT module")
    void shouldCreateInstanceFromWatModule() throws WasmException {
      final Module module = engine.compileWat(SIMPLE_ADD_WAT);
      final Instance instance = store.createInstance(module);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should create instance using static create method")
    void shouldCreateInstanceUsingStaticCreateMethod() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final Instance instance = Instance.create(store, module);

      assertNotNull(instance, "Instance should not be null");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Function Export Tests")
  class FunctionExportTests {

    @Test
    @DisplayName("should get exported function by name")
    void shouldGetExportedFunctionByName() throws WasmException {
      final Module module = engine.compileWat(SIMPLE_ADD_WAT);
      final Instance instance = store.createInstance(module);

      final Optional<WasmFunction> func = instance.getFunction("add");
      assertTrue(func.isPresent(), "Function should be present");
      assertNotNull(func.get(), "Function should not be null");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should return empty for non-existent function")
    void shouldReturnEmptyForNonExistentFunction() throws WasmException {
      final Module module = engine.compileWat(SIMPLE_ADD_WAT);
      final Instance instance = store.createInstance(module);

      final Optional<WasmFunction> func = instance.getFunction("nonexistent");
      assertFalse(func.isPresent(), "Non-existent function should return empty");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Function Call Tests")
  class FunctionCallTests {

    @Test
    @DisplayName("should call function with callFunction convenience method")
    void shouldCallFunctionWithCallFunctionMethod() throws WasmException {
      final Module module = engine.compileWat(SIMPLE_ADD_WAT);
      final Instance instance = store.createInstance(module);

      final WasmValue[] result = instance.callFunction("add", WasmValue.i32(10), WasmValue.i32(20));

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Result should have one value");
      assertEquals(30, result[0].asI32(), "10 + 20 should equal 30");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should call function with callI32Function")
    void shouldCallFunctionWithCallI32Function() throws WasmException {
      final Module module = engine.compileWat(SIMPLE_ADD_WAT);
      final Instance instance = store.createInstance(module);

      final int result = instance.callI32Function("add", 5, 7);
      assertEquals(12, result, "5 + 7 should equal 12");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should call function via WasmFunction interface")
    void shouldCallFunctionViaWasmFunctionInterface() throws WasmException {
      final Module module = engine.compileWat(SIMPLE_ADD_WAT);
      final Instance instance = store.createInstance(module);

      final Optional<WasmFunction> funcOpt = instance.getFunction("add");
      assertTrue(funcOpt.isPresent(), "Function should be present");

      final WasmFunction func = funcOpt.get();
      final WasmValue[] result = func.call(WasmValue.i32(100), WasmValue.i32(200));

      assertEquals(1, result.length, "Result should have one value");
      assertEquals(300, result[0].asI32(), "100 + 200 should equal 300");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Memory Export Tests")
  class MemoryExportTests {

    @Test
    @DisplayName("should get exported memory by name")
    void shouldGetExportedMemoryByName() throws WasmException {
      final Module module = engine.compileWat(MEMORY_WAT);
      final Instance instance = store.createInstance(module);

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory should be present");
      assertEquals(1, memOpt.get().getSize(), "Memory should have 1 page");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should get default memory")
    void shouldGetDefaultMemory() throws WasmException {
      final Module module = engine.compileWat(MEMORY_WAT);
      final Instance instance = store.createInstance(module);

      final Optional<WasmMemory> memOpt = instance.getDefaultMemory();
      assertTrue(memOpt.isPresent(), "Default memory should be present");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should return empty for module without memory")
    void shouldReturnEmptyForModuleWithoutMemory() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final Instance instance = store.createInstance(module);

      final Optional<WasmMemory> memOpt = instance.getDefaultMemory();
      assertFalse(memOpt.isPresent(), "Minimal module should not have memory");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Global Export Tests")
  class GlobalExportTests {

    @Test
    @DisplayName("should get exported global by name")
    void shouldGetExportedGlobalByName() throws WasmException {
      final Module module = engine.compileWat(GLOBAL_WAT);
      final Instance instance = store.createInstance(module);

      final Optional<WasmGlobal> globalOpt = instance.getGlobal("counter");
      assertTrue(globalOpt.isPresent(), "Global should be present");
      assertEquals(42, globalOpt.get().get().asI32(), "Global value should be 42");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should return empty for non-existent global")
    void shouldReturnEmptyForNonExistentGlobal() throws WasmException {
      final Module module = engine.compileWat(GLOBAL_WAT);
      final Instance instance = store.createInstance(module);

      final Optional<WasmGlobal> globalOpt = instance.getGlobal("nonexistent");
      assertFalse(globalOpt.isPresent(), "Non-existent global should return empty");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Table Export Tests")
  class TableExportTests {

    @Test
    @DisplayName("should get exported table by name")
    void shouldGetExportedTableByName() throws WasmException {
      final Module module = engine.compileWat(TABLE_WAT);
      final Instance instance = store.createInstance(module);

      final Optional<WasmTable> tableOpt = instance.getTable("table");
      assertTrue(tableOpt.isPresent(), "Table should be present");
      assertEquals(10, tableOpt.get().getSize(), "Table should have 10 elements");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Export Information Tests")
  class ExportInformationTests {

    @Test
    @DisplayName("should get all export names")
    void shouldGetAllExportNames() throws WasmException {
      final Module module = engine.compileWat(MULTI_EXPORT_WAT);
      final Instance instance = store.createInstance(module);

      final String[] exportNames = instance.getExportNames();
      assertNotNull(exportNames, "Export names should not be null");
      assertEquals(4, exportNames.length, "Should have 4 exports");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should get export descriptors")
    void shouldGetExportDescriptors() throws WasmException {
      final Module module = engine.compileWat(MULTI_EXPORT_WAT);
      final Instance instance = store.createInstance(module);

      final List<ExportDescriptor> descriptors = instance.getExportDescriptors();
      assertNotNull(descriptors, "Export descriptors should not be null");
      assertEquals(4, descriptors.size(), "Should have 4 export descriptors");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should check if export exists")
    void shouldCheckIfExportExists() throws WasmException {
      final Module module = engine.compileWat(SIMPLE_ADD_WAT);
      final Instance instance = store.createInstance(module);

      assertTrue(instance.hasExport("add"), "Should have 'add' export");
      assertFalse(instance.hasExport("nonexistent"), "Should not have 'nonexistent' export");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should get all exports as map")
    void shouldGetAllExportsAsMap() throws WasmException {
      final Module module = engine.compileWat(MULTI_EXPORT_WAT);
      final Instance instance = store.createInstance(module);

      final Map<String, Object> exports = instance.getAllExports();
      assertNotNull(exports, "Exports map should not be null");
      assertEquals(4, exports.size(), "Should have 4 exports");
      assertTrue(exports.containsKey("memory"), "Should contain memory export");
      assertTrue(exports.containsKey("counter"), "Should contain counter export");
      assertTrue(exports.containsKey("get"), "Should contain get export");
      assertTrue(exports.containsKey("set"), "Should contain set export");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Type Information Tests")
  class TypeInformationTests {

    @Test
    @DisplayName("should get function type for exported function")
    void shouldGetFunctionTypeForExportedFunction() throws WasmException {
      final Module module = engine.compileWat(SIMPLE_ADD_WAT);
      final Instance instance = store.createInstance(module);

      final Optional<FuncType> funcType = instance.getFunctionType("add");
      assertTrue(funcType.isPresent(), "Function type should be present");

      final FuncType type = funcType.get();
      assertEquals(2, type.getParamCount(), "Should have 2 parameters");
      assertEquals(1, type.getResultCount(), "Should have 1 result");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should get global type for exported global")
    void shouldGetGlobalTypeForExportedGlobal() throws WasmException {
      final Module module = engine.compileWat(GLOBAL_WAT);
      final Instance instance = store.createInstance(module);

      final Optional<GlobalType> globalType = instance.getGlobalType("counter");
      assertTrue(globalType.isPresent(), "Global type should be present");
      assertTrue(globalType.get().isMutable(), "Global should be mutable");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should get memory type for exported memory")
    void shouldGetMemoryTypeForExportedMemory() throws WasmException {
      final Module module = engine.compileWat(MEMORY_WAT);
      final Instance instance = store.createInstance(module);

      final Optional<MemoryType> memType = instance.getMemoryType("memory");
      assertTrue(memType.isPresent(), "Memory type should be present");
      assertEquals(1, memType.get().getMinimum(), "Minimum should be 1");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should get table type for exported table")
    void shouldGetTableTypeForExportedTable() throws WasmException {
      final Module module = engine.compileWat(TABLE_WAT);
      final Instance instance = store.createInstance(module);

      final Optional<TableType> tableType = instance.getTableType("table");
      assertTrue(tableType.isPresent(), "Table type should be present");
      assertEquals(10, tableType.get().getMinimum(), "Minimum should be 10");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Instance References Tests")
  class InstanceReferencesTests {

    @Test
    @DisplayName("should return reference to parent module")
    void shouldReturnReferenceToParentModule() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final Instance instance = store.createInstance(module);

      final Module instanceModule = instance.getModule();
      assertNotNull(instanceModule, "Module reference should not be null");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should return reference to parent store")
    void shouldReturnReferenceToParentStore() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final Instance instance = store.createInstance(module);

      final Store instanceStore = instance.getStore();
      assertNotNull(instanceStore, "Store reference should not be null");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Instance Lifecycle Tests")
  class InstanceLifecycleTests {

    @Test
    @DisplayName("should be valid before close")
    void shouldBeValidBeforeClose() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final Instance instance = store.createInstance(module);

      assertTrue(instance.isValid(), "Instance should be valid before close");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should be invalid after close")
    void shouldBeInvalidAfterClose() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final Instance instance = store.createInstance(module);

      instance.close();
      assertFalse(instance.isValid(), "Instance should be invalid after close");

      module.close();
    }

    @Test
    @DisplayName("should handle multiple close calls")
    void shouldHandleMultipleCloseCalls() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final Instance instance = store.createInstance(module);

      instance.close();
      // Second close should not throw
      instance.close();

      module.close();
    }

    @Test
    @DisplayName("should dispose instance")
    void shouldDisposeInstance() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final Instance instance = store.createInstance(module);

      final boolean disposed = instance.dispose();
      assertTrue(disposed, "First dispose should return true");
      assertTrue(instance.isDisposed(), "Instance should be disposed");

      module.close();
    }

    @Test
    @DisplayName("should cleanup instance")
    void shouldCleanupInstance() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final Instance instance = store.createInstance(module);

      final boolean cleaned = instance.cleanup();
      assertTrue(cleaned, "First cleanup should return true");

      module.close();
    }
  }

  @Nested
  @DisplayName("Instance State Tests")
  class InstanceStateTests {

    @Test
    @DisplayName("should return instance state")
    void shouldReturnInstanceState() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final Instance instance = store.createInstance(module);

      final InstanceState state = instance.getState();
      assertNotNull(state, "Instance state should not be null");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should return creation timestamp")
    void shouldReturnCreationTimestamp() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final long beforeCreate = System.currentTimeMillis() * 1000;
      final Instance instance = store.createInstance(module);
      final long afterCreate = System.currentTimeMillis() * 1000 + 1000;

      final long createdAt = instance.getCreatedAtMicros();
      assertTrue(
          createdAt >= beforeCreate - 1000000 && createdAt <= afterCreate + 1000000,
          "Creation timestamp should be within reasonable range");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Instance Statistics Tests")
  class InstanceStatisticsTests {

    @Test
    @DisplayName("should return instance statistics")
    void shouldReturnInstanceStatistics() throws WasmException {
      final Module module = engine.compileModule(MINIMAL_WASM);
      final Instance instance = store.createInstance(module);

      final InstanceStatistics stats = instance.getStatistics();
      assertNotNull(stats, "Instance statistics should not be null");

      instance.close();
      module.close();
    }
  }
}

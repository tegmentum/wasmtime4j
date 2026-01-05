package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.testing.RequiresWasmRuntime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the Linker interface.
 *
 * <p>Tests verify linker creation, host function definition, import resolution, module
 * instantiation, and WASI support. These tests require the native Wasmtime runtime to be available.
 */
@DisplayName("Linker Interface Tests")
@RequiresWasmRuntime
class LinkerTest {

  /** Module that imports a function from env. */
  private static final String IMPORT_FUNC_WAT =
      "(module\n"
          + "  (import \"env\" \"add\" (func $add (param i32 i32) (result i32)))\n"
          + "  (func (export \"test\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    call $add))\n";

  /** Module that imports a memory. */
  private static final String IMPORT_MEMORY_WAT =
      "(module\n"
          + "  (import \"env\" \"memory\" (memory 1))\n"
          + "  (func (export \"read\") (param i32) (result i32)\n"
          + "    local.get 0\n"
          + "    i32.load))\n";

  /** Module that imports a global. */
  private static final String IMPORT_GLOBAL_WAT =
      "(module\n"
          + "  (import \"env\" \"counter\" (global $counter (mut i32)))\n"
          + "  (func (export \"get\") (result i32)\n"
          + "    global.get $counter))\n";

  /** Simple standalone module. */
  private static final String STANDALONE_WAT =
      "(module\n"
          + "  (func (export \"double\") (param i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 0\n"
          + "    i32.add))\n";

  private Engine engine;
  private Store store;
  private Linker<Object> linker;

  @BeforeEach
  void setUp() throws WasmException {
    engine = Engine.create();
    store = engine.createStore();
    linker = Linker.create(engine);
  }

  @AfterEach
  void tearDown() {
    if (linker != null) {
      linker.close();
      linker = null;
    }
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
  @DisplayName("Linker Creation Tests")
  class LinkerCreationTests {

    @Test
    @DisplayName("should create linker from engine")
    void shouldCreateLinkerFromEngine() throws WasmException {
      try (Linker<Object> newLinker = Linker.create(engine)) {
        assertNotNull(newLinker, "Linker should not be null");
        assertTrue(newLinker.isValid(), "Linker should be valid");
      }
    }

    @Test
    @DisplayName("should return reference to parent engine")
    void shouldReturnReferenceToParentEngine() {
      final Engine linkerEngine = linker.getEngine();
      assertNotNull(linkerEngine, "Engine reference should not be null");
      assertTrue(linkerEngine.same(engine), "Should reference the same engine");
    }
  }

  @Nested
  @DisplayName("Host Function Definition Tests")
  class HostFunctionDefinitionTests {

    @Test
    @DisplayName("should define host function")
    void shouldDefineHostFunction() throws WasmException {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      linker.defineHostFunction(
          "env",
          "add",
          funcType,
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asI32() + params[1].asI32())});

      assertTrue(linker.hasImport("env", "add"), "Host function should be defined");
    }

    @Test
    @DisplayName("should instantiate module with host function")
    void shouldInstantiateModuleWithHostFunction() throws WasmException {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      linker.defineHostFunction(
          "env",
          "add",
          funcType,
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asI32() + params[1].asI32())});

      final Module module = engine.compileWat(IMPORT_FUNC_WAT);
      final Instance instance = linker.instantiate(store, module);

      final WasmValue[] result =
          instance.callFunction("test", WasmValue.i32(10), WasmValue.i32(20));
      assertEquals(30, result[0].asI32(), "10 + 20 should equal 30 via host function");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Memory Definition Tests")
  class MemoryDefinitionTests {

    @Test
    @DisplayName("should define memory")
    void shouldDefineMemory() throws WasmException {
      final WasmMemory memory = store.createMemory(1, 10);
      linker.defineMemory(store, "env", "memory", memory);

      assertTrue(linker.hasImport("env", "memory"), "Memory should be defined");
    }

    @Test
    @DisplayName("should instantiate module with imported memory")
    void shouldInstantiateModuleWithImportedMemory() throws WasmException {
      final WasmMemory memory = store.createMemory(1, 10);

      // Write a test value to memory
      memory.writeInt32(0, 42);

      linker.defineMemory(store, "env", "memory", memory);

      final Module module = engine.compileWat(IMPORT_MEMORY_WAT);
      final Instance instance = linker.instantiate(store, module);

      final int result = instance.callI32Function("read", 0);
      assertEquals(42, result, "Should read 42 from imported memory");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Global Definition Tests")
  class GlobalDefinitionTests {

    @Test
    @DisplayName("should define global")
    void shouldDefineGlobal() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(100));
      linker.defineGlobal(store, "env", "counter", global);

      assertTrue(linker.hasImport("env", "counter"), "Global should be defined");
    }

    @Test
    @DisplayName("should instantiate module with imported global")
    void shouldInstantiateModuleWithImportedGlobal() throws WasmException {
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(999));
      linker.defineGlobal(store, "env", "counter", global);

      final Module module = engine.compileWat(IMPORT_GLOBAL_WAT);
      final Instance instance = linker.instantiate(store, module);

      final int result = instance.callI32Function("get");
      assertEquals(999, result, "Should get 999 from imported global");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Module Instantiation Tests")
  class ModuleInstantiationTests {

    @Test
    @DisplayName("should instantiate standalone module")
    void shouldInstantiateStandaloneModule() throws WasmException {
      final Module module = engine.compileWat(STANDALONE_WAT);
      final Instance instance = linker.instantiate(store, module);

      assertNotNull(instance, "Instance should not be null");

      final int result = instance.callI32Function("double", 21);
      assertEquals(42, result, "double(21) should equal 42");

      instance.close();
      module.close();
    }

    @Test
    @DisplayName("should instantiate module with name")
    void shouldInstantiateModuleWithName() throws WasmException {
      final Module module = engine.compileWat(STANDALONE_WAT);
      final Instance instance = linker.instantiate(store, "mymodule", module);

      assertNotNull(instance, "Instance should not be null");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Pre-instantiation Tests")
  class PreInstantiationTests {

    @Test
    @DisplayName("should create instance pre")
    void shouldCreateInstancePre() throws WasmException {
      final Module module = engine.compileWat(STANDALONE_WAT);
      final InstancePre instancePre = linker.instantiatePre(module);

      assertNotNull(instancePre, "InstancePre should not be null");

      module.close();
    }
  }

  @Nested
  @DisplayName("Import Check Tests")
  class ImportCheckTests {

    @Test
    @DisplayName("should check for defined import")
    void shouldCheckForDefinedImport() throws WasmException {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

      linker.defineHostFunction(
          "test", "func", funcType, (params) -> new WasmValue[] {WasmValue.i32(0)});

      assertTrue(linker.hasImport("test", "func"), "Defined import should exist");
      assertFalse(linker.hasImport("test", "nonexistent"), "Non-existent import should not exist");
      assertFalse(
          linker.hasImport("nonexistent", "func"), "Non-existent module should not have imports");
    }
  }

  @Nested
  @DisplayName("Import Registry Tests")
  class ImportRegistryTests {

    @Test
    @DisplayName("should return import registry")
    void shouldReturnImportRegistry() throws WasmException {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

      linker.defineHostFunction(
          "env", "func1", funcType, (params) -> new WasmValue[] {WasmValue.i32(0)});

      final List<ImportInfo> registry = linker.getImportRegistry();
      assertNotNull(registry, "Import registry should not be null");
      assertFalse(registry.isEmpty(), "Import registry should not be empty");
    }
  }

  @Nested
  @DisplayName("Linker Configuration Tests")
  class LinkerConfigurationTests {

    @Test
    @DisplayName("should allow shadowing")
    void shouldAllowShadowing() throws WasmException {
      linker.allowShadowing(true);

      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

      linker.defineHostFunction(
          "env", "func", funcType, (params) -> new WasmValue[] {WasmValue.i32(1)});

      // Should not throw when redefining with shadowing allowed
      linker.defineHostFunction(
          "env", "func", funcType, (params) -> new WasmValue[] {WasmValue.i32(2)});

      assertTrue(linker.hasImport("env", "func"), "Function should still be defined");
    }

    @Test
    @DisplayName("should allow unknown exports")
    void shouldAllowUnknownExports() {
      // Just verify the method exists and doesn't throw
      linker.allowUnknownExports(true);
    }
  }

  @Nested
  @DisplayName("Linker Iteration Tests")
  class LinkerIterationTests {

    @Test
    @DisplayName("should iterate over definitions")
    void shouldIterateOverDefinitions() throws WasmException {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

      linker.defineHostFunction(
          "env", "func1", funcType, (params) -> new WasmValue[] {WasmValue.i32(0)});
      linker.defineHostFunction(
          "env", "func2", funcType, (params) -> new WasmValue[] {WasmValue.i32(0)});

      int count = 0;
      for (Linker.LinkerDefinition def : linker.iter()) {
        assertNotNull(def.getModuleName(), "Module name should not be null");
        assertNotNull(def.getName(), "Name should not be null");
        count++;
      }

      assertTrue(count >= 2, "Should have at least 2 definitions");
    }
  }

  @Nested
  @DisplayName("Linker Lifecycle Tests")
  class LinkerLifecycleTests {

    @Test
    @DisplayName("should be valid before close")
    void shouldBeValidBeforeClose() {
      assertTrue(linker.isValid(), "Linker should be valid before close");
    }

    @Test
    @DisplayName("should handle multiple close calls")
    void shouldHandleMultipleCloseCalls() throws WasmException {
      final Linker<Object> tempLinker = Linker.create(engine);
      tempLinker.close();
      // Second close should not throw
      tempLinker.close();
    }
  }

  @Nested
  @DisplayName("WASI Support Tests")
  class WasiSupportTests {

    @Test
    @DisplayName("should enable WASI")
    void shouldEnableWasi() throws WasmException {
      // Should not throw
      linker.enableWasi();
    }
  }

  @Nested
  @DisplayName("Instance Definition Tests")
  class InstanceDefinitionTests {

    @Test
    @DisplayName("should define instance")
    void shouldDefineInstance() throws WasmException {
      final Module module = engine.compileWat(STANDALONE_WAT);
      final Instance instance = linker.instantiate(store, module);

      // Define the instance under a module name
      linker.defineInstance("myinstance", instance);

      // The exports from the instance should now be available
      assertTrue(linker.hasImport("myinstance", "double"), "Instance export should be available");

      instance.close();
      module.close();
    }
  }

  @Nested
  @DisplayName("Alias Tests")
  class AliasTests {

    @Test
    @DisplayName("should create alias for export")
    void shouldCreateAliasForExport() throws WasmException {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

      linker.defineHostFunction(
          "original", "func", funcType, (params) -> new WasmValue[] {WasmValue.i32(0)});

      linker.alias("original", "func", "aliased", "aliased_func");

      assertTrue(linker.hasImport("aliased", "aliased_func"), "Alias should exist");
    }
  }
}

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral smoke tests for the Panama native bridge.
 *
 * <p>These tests exercise the full native pipeline through Panama FFI: create engine, compile WAT,
 * create store, instantiate module, call functions, and verify results. They complement the
 * existing PanamaFunctionTest/PanamaEngineTest by covering host functions via linker, memory
 * access, globals, and fuel metering.
 */
@DisplayName("Panama Native Bridge Smoke Tests")
class PanamaNativeBridgeSmokeTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaNativeBridgeSmokeTest.class.getName());

  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void loadNativeLibrary() {
    final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    assertTrue(loader.isLoaded(), "Native library should be loaded");
    LOGGER.info("Native library loaded for PanamaNativeBridgeSmokeTest");
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

  private PanamaEngine createEngine(final EngineConfig config) throws Exception {
    final PanamaEngine engine = new PanamaEngine(config);
    resources.add(engine);
    return engine;
  }

  private PanamaStore createStore(final PanamaEngine engine) throws Exception {
    final PanamaStore store = new PanamaStore(engine);
    resources.add(store);
    return store;
  }

  private PanamaModule compileWat(final PanamaEngine engine, final String wat) throws Exception {
    final PanamaModule module = (PanamaModule) engine.compileWat(wat);
    resources.add(module);
    return module;
  }

  private PanamaInstance createInstance(final PanamaModule module, final PanamaStore store)
      throws Exception {
    final PanamaInstance instance = new PanamaInstance(module, store);
    resources.add(instance);
    return instance;
  }

  @Test
  @DisplayName("Engine create and close - native handle is valid")
  void testEngineCreateAndClose() throws Exception {
    final PanamaEngine engine = createEngine();

    assertNotNull(engine, "Engine should not be null");
    assertTrue(engine.isValid(), "Engine should be valid after creation");

    LOGGER.info("Created Panama engine successfully");
  }

  @Test
  @DisplayName("Module compile WAT - validates native compilation")
  void testModuleCompileWat() throws Exception {
    final PanamaEngine engine = createEngine();

    final String wat =
        "(module\n"
            + "  (func (export \"test\") (result i32)\n"
            + "    i32.const 42\n"
            + "  )\n"
            + ")";

    final PanamaModule module = compileWat(engine, wat);

    assertNotNull(module, "Compiled module should not be null");
    assertTrue(module.isValid(), "Module should be valid after compilation");
    assertTrue(module.hasExport("test"), "Module should export 'test' function");

    LOGGER.info("Compiled WAT module via Panama successfully");
  }

  @Test
  @DisplayName("Store create and close - validates native store lifecycle")
  void testStoreCreateAndClose() throws Exception {
    final PanamaEngine engine = createEngine();
    final PanamaStore store = createStore(engine);

    assertNotNull(store, "Store should not be null");
    assertTrue(store.isValid(), "Store should be valid after creation");

    LOGGER.info("Created Panama store successfully");
  }

  @Test
  @DisplayName("Full pipeline: engine -> compile -> store -> instantiate -> call -> verify")
  void testInstantiateAndCallFunction() throws Exception {
    final PanamaEngine engine = createEngine();

    final String wat =
        "(module\n"
            + "  (func (export \"add\") (param i32 i32) (result i32)\n"
            + "    local.get 0\n"
            + "    local.get 1\n"
            + "    i32.add\n"
            + "  )\n"
            + ")";

    final PanamaModule module = compileWat(engine, wat);
    final PanamaStore store = createStore(engine);
    final PanamaInstance instance = createInstance(module, store);

    final WasmValue[] results = instance.callFunction("add", WasmValue.i32(5), WasmValue.i32(3));

    assertEquals(1, results.length, "Should return exactly 1 result");
    assertEquals(8, results[0].asInt(), "5 + 3 should equal 8");

    LOGGER.info("Full Panama pipeline executed: add(5, 3) = " + results[0].asInt());
  }

  @Test
  @DisplayName("Host function callback via Linker - Java callback invoked from WASM")
  void testHostFunctionCallback() throws Exception {
    final PanamaEngine engine = createEngine();

    final String wat =
        "(module\n"
            + "  (import \"env\" \"double_it\" (func $double_it (param i32) (result i32)))\n"
            + "  (func (export \"call_host\") (param i32) (result i32)\n"
            + "    local.get 0\n"
            + "    call $double_it\n"
            + "  )\n"
            + ")";

    final PanamaModule module = compileWat(engine, wat);

    final PanamaLinker<Void> linker = new PanamaLinker<>(engine);
    resources.add(linker);

    linker.defineHostFunction(
        "env",
        "double_it",
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32}),
        (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() * 2)});

    final PanamaStore store = createStore(engine);
    final Instance instance = linker.instantiate(store, module);
    resources.add(instance);

    final WasmValue[] results = instance.callFunction("call_host", WasmValue.i32(21));

    assertEquals(1, results.length, "Should return exactly 1 result");
    assertEquals(42, results[0].asInt(), "Host function should double 21 to 42");

    LOGGER.info("Host function callback via Panama: double_it(21) = " + results[0].asInt());
  }

  @Test
  @DisplayName("Memory read/write - native memory operations via Panama")
  void testMemoryReadWrite() throws Exception {
    final PanamaEngine engine = createEngine();

    final String wat =
        "(module\n"
            + "  (memory (export \"memory\") 1)\n"
            + "  (func (export \"store_i32\") (param i32 i32)\n"
            + "    local.get 0\n"
            + "    local.get 1\n"
            + "    i32.store\n"
            + "  )\n"
            + "  (func (export \"load_i32\") (param i32) (result i32)\n"
            + "    local.get 0\n"
            + "    i32.load\n"
            + "  )\n"
            + ")";

    final PanamaModule module = compileWat(engine, wat);
    final PanamaStore store = createStore(engine);
    final PanamaInstance instance = createInstance(module, store);

    // Write via WASM function
    instance.callFunction("store_i32", WasmValue.i32(0), WasmValue.i32(12345));

    // Read back via WASM function
    final WasmValue[] results = instance.callFunction("load_i32", WasmValue.i32(0));
    assertEquals(12345, results[0].asInt(), "Should read back the stored value");

    // Also test direct memory access via WasmMemory API
    final Optional<WasmMemory> memOpt = instance.getMemory("memory");
    assertTrue(memOpt.isPresent(), "Exported memory 'memory' should be accessible");

    final WasmMemory memory = memOpt.get();
    assertTrue(memory.size() >= 1, "Memory should have at least 1 page");

    // Write bytes directly
    final byte[] writeData = {0x48, 0x65, 0x6C, 0x6C, 0x6F}; // "Hello"
    memory.writeBytes(100, writeData, 0, writeData.length);

    // Read bytes back
    final byte[] readData = new byte[5];
    memory.readBytes(100, readData, 0, readData.length);

    for (int i = 0; i < writeData.length; i++) {
      assertEquals(
          writeData[i], readData[i], "Byte at offset " + i + " should match written value");
    }

    LOGGER.info("Memory read/write via Panama verified");
  }

  @Test
  @DisplayName("Global get/set - mutable global operations via Panama")
  void testGlobalGetSet() throws Exception {
    final PanamaEngine engine = createEngine();

    final String wat =
        "(module\n"
            + "  (global (export \"counter\") (mut i32) (i32.const 0))\n"
            + "  (func (export \"get_counter\") (result i32)\n"
            + "    global.get 0\n"
            + "  )\n"
            + "  (func (export \"set_counter\") (param i32)\n"
            + "    local.get 0\n"
            + "    global.set 0\n"
            + "  )\n"
            + ")";

    final PanamaModule module = compileWat(engine, wat);
    final PanamaStore store = createStore(engine);
    final PanamaInstance instance = createInstance(module, store);

    // Initial value should be 0
    WasmValue[] results = instance.callFunction("get_counter");
    assertEquals(0, results[0].asInt(), "Initial counter should be 0");

    // Set via WASM function
    instance.callFunction("set_counter", WasmValue.i32(99));
    results = instance.callFunction("get_counter");
    assertEquals(99, results[0].asInt(), "Counter should be 99 after set");

    // Also test direct global access via WasmGlobal API
    final Optional<WasmGlobal> globalOpt = instance.getGlobal("counter");
    assertTrue(globalOpt.isPresent(), "Exported global 'counter' should be accessible");

    final WasmGlobal global = globalOpt.get();
    final WasmValue globalVal = global.get();
    assertEquals(99, globalVal.asInt(), "Direct global read should return 99");

    global.set(WasmValue.i32(200));
    results = instance.callFunction("get_counter");
    assertEquals(200, results[0].asInt(), "Counter should be 200 after direct global set");

    LOGGER.info("Global get/set via Panama verified");
  }

  @Test
  @DisplayName("Table size - funcref table operations via Panama")
  void testTableSize() throws Exception {
    final PanamaEngine engine = createEngine();

    final String wat =
        "(module\n"
            + "  (table (export \"my_table\") 3 funcref)\n"
            + "  (func (export \"table_size\") (result i32)\n"
            + "    table.size 0\n"
            + "  )\n"
            + ")";

    final PanamaModule module = compileWat(engine, wat);
    final PanamaStore store = createStore(engine);
    final PanamaInstance instance = createInstance(module, store);

    final WasmValue[] results = instance.callFunction("table_size");
    assertEquals(3, results[0].asInt(), "Table should have 3 elements");

    LOGGER.info("Table size via Panama verified: size = " + results[0].asInt());
  }

  @Test
  @DisplayName("Fuel consumption - fuel metering via Panama")
  void testFuelConsumption() throws Exception {
    final EngineConfig config = new EngineConfig().consumeFuel(true);
    final PanamaEngine engine = createEngine(config);

    assertTrue(engine.isFuelEnabled(), "Fuel should be enabled on the engine");

    final String wat = "(module\n" + "  (func (export \"noop\")\n" + "    nop\n" + "  )\n" + ")";

    final PanamaModule module = compileWat(engine, wat);
    final PanamaStore store = createStore(engine);
    store.setFuel(10000);

    final long fuelBefore = store.getFuel();
    assertTrue(fuelBefore > 0, "Fuel should be positive after setting: got " + fuelBefore);

    final PanamaInstance instance = createInstance(module, store);
    instance.callFunction("noop");

    final long fuelAfter = store.getFuel();
    assertTrue(
        fuelAfter < fuelBefore,
        "Fuel should decrease after execution: before=" + fuelBefore + ", after=" + fuelAfter);

    LOGGER.info(
        "Fuel consumption via Panama verified: before="
            + fuelBefore
            + ", after="
            + fuelAfter
            + ", consumed="
            + (fuelBefore - fuelAfter));
  }

  @Test
  @DisplayName("Use after close throws - validates defensive checks")
  void testUseAfterCloseThrows() throws Exception {
    final PanamaEngine engine = new PanamaEngine();

    final String wat =
        "(module\n"
            + "  (func (export \"test\") (result i32)\n"
            + "    i32.const 42\n"
            + "  )\n"
            + ")";

    final PanamaModule module = (PanamaModule) engine.compileWat(wat);
    final PanamaStore store = new PanamaStore(engine);
    final PanamaInstance instance = new PanamaInstance(module, store);

    // Verify the instance works before closing
    final WasmValue[] results = instance.callFunction("test");
    assertEquals(42, results[0].asInt(), "Function should work before close");

    // Close resources
    instance.close();
    store.close();
    module.close();
    engine.close();
    assertFalse(engine.isValid(), "Engine should be invalid after close");

    // Compiling on a closed engine should throw
    assertThrows(
        Exception.class,
        () -> engine.compileWat(wat),
        "Should throw when compiling on a closed engine");

    LOGGER.info("Use-after-close defensive checks verified via Panama");
  }
}

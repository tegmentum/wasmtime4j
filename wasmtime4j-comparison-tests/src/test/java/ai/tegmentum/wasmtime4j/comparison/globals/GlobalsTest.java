package ai.tegmentum.wasmtime4j.comparison.globals;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for WebAssembly globals import/export. */
public class GlobalsTest {

  private Engine engine;
  private Store store;

  @BeforeEach
  public void setUp() throws Exception {
    engine = Engine.create();
    store = engine.createStore();
  }

  @AfterEach
  public void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  @DisplayName("Export immutable i32 global")
  public void testExportImmutableI32Global() throws Exception {
    final String wat =
        """
        (module
          (global $const (export "my_const") i32 (i32.const 42))
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmGlobal global = instance.getGlobal("my_const");
    assertNotNull(global);
    assertFalse(global.isMutable());
    assertEquals(WasmValueType.I32, global.getType());
    assertEquals(42, global.get().asInt());

    instance.close();
  }

  @Test
  @DisplayName("Export mutable i32 global")
  public void testExportMutableI32Global() throws Exception {
    final String wat =
        """
        (module
          (global $counter (export "counter") (mut i32) (i32.const 0))
          (func (export "increment")
            global.get $counter
            i32.const 1
            i32.add
            global.set $counter
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmGlobal global = instance.getGlobal("counter");
    assertNotNull(global);
    assertTrue(global.isMutable());
    assertEquals(0, global.get().asInt());

    // Call increment function
    instance.callFunction("increment");
    assertEquals(1, global.get().asInt());

    // Modify from Java
    global.set(WasmValue.i32(10));
    assertEquals(10, global.get().asInt());

    // Call increment again
    instance.callFunction("increment");
    assertEquals(11, global.get().asInt());

    instance.close();
  }

  @Test
  @DisplayName("Export i64 global")
  public void testExportI64Global() throws Exception {
    final String wat =
        """
        (module
          (global $big_num (export "big_num") (mut i64) (i64.const 9223372036854775807))
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmGlobal global = instance.getGlobal("big_num");
    assertEquals(WasmValueType.I64, global.getType());
    assertEquals(9223372036854775807L, global.get().asLong());

    global.set(WasmValue.i64(123456789L));
    assertEquals(123456789L, global.get().asLong());

    instance.close();
  }

  @Test
  @DisplayName("Export f32 global")
  public void testExportF32Global() throws Exception {
    final String wat =
        """
        (module
          (global $pi (export "pi") (mut f32) (f32.const 3.14159))
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmGlobal global = instance.getGlobal("pi");
    assertEquals(WasmValueType.F32, global.getType());
    assertEquals(3.14159f, global.get().asFloat(), 0.00001f);

    global.set(WasmValue.f32(2.71828f));
    assertEquals(2.71828f, global.get().asFloat(), 0.00001f);

    instance.close();
  }

  @Test
  @DisplayName("Export f64 global")
  public void testExportF64Global() throws Exception {
    final String wat =
        """
        (module
          (global $e (export "e") (mut f64) (f64.const 2.718281828))
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmGlobal global = instance.getGlobal("e");
    assertEquals(WasmValueType.F64, global.getType());
    assertEquals(2.718281828, global.get().asDouble(), 0.000000001);

    global.set(WasmValue.f64(3.14159));
    assertEquals(3.14159, global.get().asDouble(), 0.00001);

    instance.close();
  }

  @Test
  @DisplayName("Multiple exported globals")
  public void testMultipleExportedGlobals() throws Exception {
    final String wat =
        """
        (module
          (global $a (export "a") i32 (i32.const 10))
          (global $b (export "b") (mut i32) (i32.const 20))
          (global $c (export "c") i64 (i64.const 30))
          (global $d (export "d") (mut f32) (f32.const 40.5))
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmGlobal a = instance.getGlobal("a");
    final WasmGlobal b = instance.getGlobal("b");
    final WasmGlobal c = instance.getGlobal("c");
    final WasmGlobal d = instance.getGlobal("d");

    assertFalse(a.isMutable());
    assertTrue(b.isMutable());
    assertFalse(c.isMutable());
    assertTrue(d.isMutable());

    assertEquals(10, a.get().asInt());
    assertEquals(20, b.get().asInt());
    assertEquals(30L, c.get().asLong());
    assertEquals(40.5f, d.get().asFloat(), 0.01f);

    instance.close();
  }

  @Test
  @DisplayName("Import immutable i32 global")
  public void testImportImmutableI32Global() throws Exception {
    // Create a global in Java
    final WasmGlobal importedGlobal =
        store.createImmutableGlobal(WasmValueType.I32, WasmValue.i32(42));

    // Create linker and define the global
    final Linker linker = Linker.create(engine);
    linker.defineGlobal("env", "imported_const", importedGlobal);

    final String wat =
        """
        (module
          (import "env" "imported_const" (global $imported i32))
          (func (export "get_imported") (result i32)
            global.get $imported
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("get_imported");
    assertEquals(42, results[0].asInt());

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("Import mutable i32 global")
  public void testImportMutableI32Global() throws Exception {
    // Create a mutable global in Java
    final WasmGlobal importedGlobal =
        store.createMutableGlobal(WasmValueType.I32, WasmValue.i32(0));

    final Linker linker = Linker.create(engine);
    linker.defineGlobal("env", "shared_counter", importedGlobal);

    final String wat =
        """
        (module
          (import "env" "shared_counter" (global $counter (mut i32)))
          (func (export "increment")
            global.get $counter
            i32.const 1
            i32.add
            global.set $counter
          )
          (func (export "get_counter") (result i32)
            global.get $counter
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = linker.instantiate(store, module);

    // Initial value
    WasmValue[] results = instance.callFunction("get_counter");
    assertEquals(0, results[0].asInt());

    // Increment from WASM
    instance.callFunction("increment");
    results = instance.callFunction("get_counter");
    assertEquals(1, results[0].asInt());

    // Check from Java side
    assertEquals(1, importedGlobal.get().asInt());

    // Modify from Java
    importedGlobal.set(WasmValue.i32(100));

    // Check from WASM side
    results = instance.callFunction("get_counter");
    assertEquals(100, results[0].asInt());

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("Import i64 global")
  public void testImportI64Global() throws Exception {
    final WasmGlobal importedGlobal =
        store.createMutableGlobal(WasmValueType.I64, WasmValue.i64(1000000000000L));

    final Linker linker = Linker.create(engine);
    linker.defineGlobal("env", "big_num", importedGlobal);

    final String wat =
        """
        (module
          (import "env" "big_num" (global $big (mut i64)))
          (func (export "double")
            global.get $big
            i64.const 2
            i64.mul
            global.set $big
          )
          (func (export "get") (result i64)
            global.get $big
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = linker.instantiate(store, module);

    instance.callFunction("double");
    WasmValue[] results = instance.callFunction("get");
    assertEquals(2000000000000L, results[0].asLong());
    assertEquals(2000000000000L, importedGlobal.get().asLong());

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("Import f32 global")
  public void testImportF32Global() throws Exception {
    final WasmGlobal importedGlobal =
        store.createMutableGlobal(WasmValueType.F32, WasmValue.f32(1.5f));

    final Linker linker = Linker.create(engine);
    linker.defineGlobal("env", "scale", importedGlobal);

    final String wat =
        """
        (module
          (import "env" "scale" (global $scale (mut f32)))
          (func (export "apply_scale") (param f32) (result f32)
            local.get 0
            global.get $scale
            f32.mul
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = linker.instantiate(store, module);

    WasmValue[] results = instance.callFunction("apply_scale", WasmValue.f32(10.0f));
    assertEquals(15.0f, results[0].asFloat(), 0.01f);

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("Import and export globals in same module")
  public void testImportAndExportGlobals() throws Exception {
    final WasmGlobal importedGlobal =
        store.createMutableGlobal(WasmValueType.I32, WasmValue.i32(5));

    final Linker linker = Linker.create(engine);
    linker.defineGlobal("env", "input", importedGlobal);

    final String wat =
        """
        (module
          (import "env" "input" (global $input (mut i32)))
          (global $output (export "output") (mut i32) (i32.const 0))
          (func (export "process")
            global.get $input
            i32.const 10
            i32.mul
            global.set $output
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = linker.instantiate(store, module);

    final WasmGlobal output = instance.getGlobal("output");

    // Process
    instance.callFunction("process");
    assertEquals(50, output.get().asInt());

    // Change input and process again
    importedGlobal.set(WasmValue.i32(7));
    instance.callFunction("process");
    assertEquals(70, output.get().asInt());

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("Global used in multiple functions")
  public void testGlobalSharedAcrossFunctions() throws Exception {
    final String wat =
        """
        (module
          (global $state (export "state") (mut i32) (i32.const 0))
          (func (export "add") (param i32)
            global.get $state
            local.get 0
            i32.add
            global.set $state
          )
          (func (export "multiply") (param i32)
            global.get $state
            local.get 0
            i32.mul
            global.set $state
          )
          (func (export "reset")
            i32.const 0
            global.set $state
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmGlobal state = instance.getGlobal("state");

    // (0 + 10) = 10
    instance.callFunction("add", WasmValue.i32(10));
    assertEquals(10, state.get().asInt());

    // (10 * 3) = 30
    instance.callFunction("multiply", WasmValue.i32(3));
    assertEquals(30, state.get().asInt());

    // (30 + 12) = 42
    instance.callFunction("add", WasmValue.i32(12));
    assertEquals(42, state.get().asInt());

    // Reset
    instance.callFunction("reset");
    assertEquals(0, state.get().asInt());

    instance.close();
  }

  @Test
  @DisplayName("Attempt to set immutable global throws exception")
  public void testSetImmutableGlobalThrows() throws Exception {
    final String wat =
        """
        (module
          (global $const (export "const") i32 (i32.const 42))
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmGlobal global = instance.getGlobal("const");
    assertThrows(Exception.class, () -> global.set(WasmValue.i32(100)));

    instance.close();
  }
}

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test for Panama instance function calls. */
public class PanamaInstanceCallFunctionTest {

  @Test
  @DisplayName("Call function with no parameters returning i32")
  public void testCallFunctionNoParams() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    final WasmValue[] results = instance.callFunction("get42");

    assertNotNull(results, "Results should not be null");
    assertEquals(1, results.length, "Should return 1 value");
    assertEquals(42, results[0].asInt(), "Should return 42");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Call add function with two i32 parameters")
  public void testCallFunctionWithParams() throws WasmException {
    // WASM bytecode for: (module (func (export "add") (param i32 i32) (result i32)
    //   local.get 0 local.get 1 i32.add))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x07, 0x01, 0x60, 0x02, 0x7f,
          0x7f, 0x01, 0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64,
          0x00, 0x00, 0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    final WasmValue[] results = instance.callFunction("add", WasmValue.i32(10), WasmValue.i32(32));

    assertNotNull(results, "Results should not be null");
    assertEquals(1, results.length, "Should return 1 value");
    assertEquals(42, results[0].asInt(), "Should return 10 + 32 = 42");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Call function with i64 parameters")
  public void testCallFunctionI64() throws WasmException {
    // WASM bytecode for: (module (func (export "add64") (param i64 i64) (result i64)
    //   local.get 0 local.get 1 i64.add))
    final byte[] wasmBytes =
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6d, // magic number
          0x01,
          0x00,
          0x00,
          0x00, // version 1
          // Type section (id=1)
          0x01,
          0x07, // section id and size
          0x01, // number of types
          0x60,
          0x02,
          0x7e,
          0x7e,
          0x01,
          0x7e, // (i64, i64) -> i64
          // Function section (id=3)
          0x03,
          0x02, // section id and size
          0x01, // number of functions
          0x00, // function 0: type 0
          // Export section (id=7)
          0x07,
          0x09, // section id and size
          0x01, // number of exports
          0x05,
          0x61,
          0x64,
          0x64,
          0x36,
          0x34, // "add64"
          0x00,
          0x00, // function export, index 0
          // Code section (id=10)
          0x0a,
          0x09, // section id and size
          0x01, // number of function bodies
          0x07, // function body size
          0x00, // local variable count
          0x20,
          0x00, // local.get 0
          0x20,
          0x01, // local.get 1
          0x7c, // i64.add
          0x0b // end
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    final WasmValue[] results =
        instance.callFunction("add64", WasmValue.i64(1000000000L), WasmValue.i64(2000000000L));

    assertNotNull(results, "Results should not be null");
    assertEquals(1, results.length, "Should return 1 value");
    assertEquals(
        3000000000L, results[0].asLong(), "Should return 1000000000 + 2000000000 = 3000000000");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Call function with f32 parameters")
  public void testCallFunctionF32() throws WasmException {
    // WASM bytecode for: (module (func (export "addf") (param f32 f32) (result f32)
    //   local.get 0 local.get 1 f32.add))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x07, 0x01, 0x60, 0x02, 0x7d,
          0x7d, 0x01, 0x7d, 0x03, 0x02, 0x01, 0x00, 0x07, 0x08, 0x01, 0x04, 0x61, 0x64, 0x64,
          0x66, 0x00, 0x00, 0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, (byte) 0x92, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    final WasmValue[] results =
        instance.callFunction("addf", WasmValue.f32(10.5f), WasmValue.f32(31.5f));

    assertNotNull(results, "Results should not be null");
    assertEquals(1, results.length, "Should return 1 value");
    assertEquals(42.0f, results[0].asFloat(), 0.001f, "Should return 10.5 + 31.5 = 42.0");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Call nonexistent function throws exception")
  public void testCallNonexistentFunction() throws WasmException {
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    assertThrows(
        WasmException.class,
        () -> instance.callFunction("nonexistent"),
        "Should throw exception for nonexistent function");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Call function with null name throws exception")
  public void testCallFunctionNullName() throws WasmException {
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    assertThrows(
        IllegalArgumentException.class,
        () -> instance.callFunction(null),
        "Should throw exception for null function name");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Call i32 function with no parameters")
  public void testCallI32FunctionNoParams() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    final int result = instance.callI32Function("get42");

    assertEquals(42, result, "Should return 42");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Call i32 function with parameters")
  public void testCallI32FunctionWithParams() throws WasmException {
    // WASM bytecode for: (module (func (export "add") (param i32 i32) (result i32)
    //   local.get 0 local.get 1 i32.add))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x07, 0x01, 0x60, 0x02, 0x7f,
          0x7f, 0x01, 0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64,
          0x00, 0x00, 0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    final int result = instance.callI32Function("add", 10, 32);

    assertEquals(42, result, "Should return 10 + 32 = 42");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Call i32 function with single parameter")
  public void testCallI32FunctionWithSingleParam() throws WasmException {
    // WASM bytecode for: (module (func (export "double") (param i32) (result i32)
    //   local.get 0 local.get 0 i32.add))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x06, 0x01, 0x60, 0x01, 0x7f,
          0x01, 0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x0a, 0x01, 0x06, 0x64, 0x6f, 0x75, 0x62,
          0x6c, 0x65, 0x00, 0x00, 0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x00, 0x6a,
          0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    final int result = instance.callI32Function("double", 21);

    assertEquals(42, result, "Should return 21 * 2 = 42");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Call i32 function on nonexistent function throws exception")
  public void testCallI32FunctionNonexistent() throws WasmException {
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    assertThrows(
        WasmException.class,
        () -> instance.callI32Function("nonexistent"),
        "Should throw exception for nonexistent function");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Call i32 function with null name throws exception")
  public void testCallI32FunctionNullName() throws WasmException {
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    assertThrows(
        IllegalArgumentException.class,
        () -> instance.callI32Function(null),
        "Should throw exception for null function name");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Call i32 function that returns f32 throws exception")
  public void testCallI32FunctionWrongReturnType() throws WasmException {
    // WASM bytecode for: (module (func (export "getf32") (result f32) f32.const 42.0))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7d, 0x03, 0x02, 0x01, 0x00, 0x07, 0x0a, 0x01, 0x06, 0x67, 0x65, 0x74, 0x66, 0x33,
          0x32, 0x00, 0x00, 0x0a, 0x09, 0x01, 0x07, 0x00, 0x43, 0x00, 0x00, 0x28, 0x42, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    assertThrows(
        WasmException.class,
        () -> instance.callI32Function("getf32"),
        "Should throw exception when function returns non-i32 type");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Call i32 function that returns no value throws exception")
  public void testCallI32FunctionNoReturnValue() throws WasmException {
    // WASM bytecode for: (module (func (export "noop") (nop)))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x04, 0x01, 0x60, 0x00, 0x00,
          0x03, 0x02, 0x01, 0x00, 0x07, 0x08, 0x01, 0x04, 0x6e, 0x6f, 0x6f, 0x70, 0x00, 0x00,
          0x0a, 0x05, 0x01, 0x03, 0x00, 0x01, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    final PanamaInstance instance = new PanamaInstance(module, store);

    assertThrows(
        WasmException.class,
        () -> instance.callI32Function("noop"),
        "Should throw exception when function returns no value");

    instance.close();
    store.close();
    module.close();
    engine.close();
  }
}

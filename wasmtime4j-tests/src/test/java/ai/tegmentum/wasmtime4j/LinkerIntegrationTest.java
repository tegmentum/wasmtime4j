package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link Linker} functionality.
 *
 * <p>These tests verify the complete linker workflow including host function binding, import
 * resolution, and module instantiation using real WebAssembly modules.
 *
 * <p>The tests use simple WebAssembly modules to verify linker behavior without requiring complex
 * native setup.
 */
class LinkerIntegrationTest {

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private Linker linker;

  // Simple WebAssembly module that imports a function called "add" from module "env"
  // (add (param i32 i32) (result i32))
  // and exports a function called "call_add" that calls the imported function
  private static final byte[] WASM_MODULE_WITH_IMPORTS = {
    0x00,
    0x61,
    0x73,
    0x6d, // WASM magic number
    0x01,
    0x00,
    0x00,
    0x00, // Version 1

    // Type section
    0x01,
    0x07, // Section id=1, length=7
    0x01, // 1 type
    0x60,
    0x02,
    0x7f,
    0x7f,
    0x01,
    0x7f, // (i32, i32) -> i32

    // Import section
    0x02,
    0x0b, // Section id=2, length=11
    0x01, // 1 import
    0x03,
    0x65,
    0x6e,
    0x76, // Module name "env"
    0x03,
    0x61,
    0x64,
    0x64, // Field name "add"
    0x00,
    0x00, // Import type: function index 0

    // Function section
    0x03,
    0x02, // Section id=3, length=2
    0x01,
    0x00, // 1 function, type index 0

    // Export section
    0x07,
    0x0c, // Section id=7, length=12
    0x01, // 1 export
    0x08,
    0x63,
    0x61,
    0x6c,
    0x6c,
    0x5f,
    0x61,
    0x64,
    0x64, // Name "call_add"
    0x00,
    0x01, // Export type: function index 1 (0 is imported, 1 is local)

    // Code section
    0x0a,
    0x09, // Section id=10, length=9
    0x01, // 1 function body
    0x07, // Body length=7
    0x00, // Local declarations count=0
    0x20,
    0x00, // local.get 0 (first parameter)
    0x20,
    0x01, // local.get 1 (second parameter)
    0x10,
    0x00, // call 0 (call imported function)
    0x0b // end
  };

  // Simple WebAssembly module with no imports
  // Exports a function called "get_answer" that returns 42
  private static final byte[] WASM_MODULE_NO_IMPORTS = {
    0x00,
    0x61,
    0x73,
    0x6d, // WASM magic number
    0x01,
    0x00,
    0x00,
    0x00, // Version 1

    // Type section
    0x01,
    0x05, // Section id=1, length=5
    0x01, // 1 type
    0x60,
    0x00,
    0x01,
    0x7f, // () -> i32

    // Function section
    0x03,
    0x02, // Section id=3, length=2
    0x01,
    0x00, // 1 function, type index 0

    // Export section
    0x07,
    0x0e, // Section id=7, length=14
    0x01, // 1 export
    0x0a,
    0x67,
    0x65,
    0x74,
    0x5f,
    0x61,
    0x6e,
    0x73,
    0x77,
    0x65,
    0x72, // Name "get_answer"
    0x00,
    0x00, // Export type: function index 0

    // Code section
    0x0a,
    0x06, // Section id=10, length=6
    0x01, // 1 function body
    0x04, // Body length=4
    0x00, // Local declarations count=0
    0x41,
    0x2a, // i32.const 42
    0x0b // end
  };

  @BeforeEach
  void setUp() throws WasmException {
    runtime = WasmRuntimeFactory.create();
    engine = runtime.createEngine();
    store = runtime.createStore(engine);
    linker = runtime.createLinker(engine);
  }

  @AfterEach
  void tearDown() {
    if (linker != null) {
      linker.close();
    }
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    if (runtime != null) {
      runtime.close();
    }
  }

  @Test
  void testLinkerCreation() {
    assertNotNull(linker);
    assertTrue(linker.isValid());
    assertEquals(engine, linker.getEngine());
  }

  @Test
  void testDefineHostFunction() throws WasmException {
    // Define a simple host function that adds two integers
    final FunctionType addType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});

    final HostFunction addFunction =
        (params) -> {
          final int a = params[0].asI32();
          final int b = params[1].asI32();
          return new WasmValue[] {WasmValue.ofI32(a + b)};
        };

    // This should not throw
    linker.defineHostFunction("env", "add", addType, addFunction);
  }

  @Test
  void testInstantiateModuleWithoutImports() throws WasmException {
    final Module module = runtime.compileModule(engine, WASM_MODULE_NO_IMPORTS);
    final Instance instance = linker.instantiate(store, module);

    assertNotNull(instance);
    assertTrue(instance.isValid());

    // Should be able to get the exported function
    final Optional<WasmFunction> function = instance.getFunction("get_answer");
    assertTrue(function.isPresent());

    // Call the function and verify result
    final WasmValue[] result = function.get().call();
    assertNotNull(result);
    assertEquals(1, result.length);
    assertEquals(42, result[0].asI32());
  }

  @Test
  void testInstantiateModuleWithImports() throws WasmException {
    // Define the required host function
    final FunctionType addType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});

    final HostFunction addFunction =
        (params) -> {
          final int a = params[0].asI32();
          final int b = params[1].asI32();
          return new WasmValue[] {WasmValue.ofI32(a + b)};
        };

    linker.defineHostFunction("env", "add", addType, addFunction);

    // Compile and instantiate the module
    final Module module = runtime.compileModule(engine, WASM_MODULE_WITH_IMPORTS);
    final Instance instance = linker.instantiate(store, module);

    assertNotNull(instance);
    assertTrue(instance.isValid());

    // Should be able to get the exported function
    final Optional<WasmFunction> function = instance.getFunction("call_add");
    assertTrue(function.isPresent());

    // Call the function with parameters and verify result
    final WasmValue[] result = function.get().call(WasmValue.ofI32(10), WasmValue.ofI32(32));
    assertNotNull(result);
    assertEquals(1, result.length);
    assertEquals(42, result[0].asI32()); // 10 + 32 = 42
  }

  @Test
  void testInstantiateModuleWithMissingImports() throws WasmException {
    // Try to instantiate a module with imports without defining the required functions
    final Module module = runtime.compileModule(engine, WASM_MODULE_WITH_IMPORTS);

    // This should fail because the "add" function is not defined
    assertThrows(WasmException.class, () -> linker.instantiate(store, module));
  }

  @Test
  void testDefineInstance() throws WasmException {
    // First, create an instance of a simple module
    final Module module = runtime.compileModule(engine, WASM_MODULE_NO_IMPORTS);
    final Instance instance = linker.instantiate(store, module);

    // Define the instance in the linker under a module name
    linker.defineInstance("simple_module", instance);

    // This should not throw - the instance has been registered
    assertNotNull(instance);
  }

  @Test
  void testInstantiateWithModuleName() throws WasmException {
    final Module module = runtime.compileModule(engine, WASM_MODULE_NO_IMPORTS);
    final Instance instance = linker.instantiate(store, "test_module", module);

    assertNotNull(instance);
    assertTrue(instance.isValid());

    // The instance should also be registered in the linker for future use
    // This is verified by the fact that instantiate succeeded
  }

  @Test
  void testAlias() throws WasmException {
    // Define a host function
    final FunctionType addType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});

    final HostFunction addFunction =
        (params) -> {
          final int a = params[0].asI32();
          final int b = params[1].asI32();
          return new WasmValue[] {WasmValue.ofI32(a + b)};
        };

    linker.defineHostFunction("env", "add", addType, addFunction);

    // Create an alias for the function
    linker.alias("env", "add", "math", "plus");

    // This should not throw - the alias has been created
  }

  @Test
  void testMultipleHostFunctions() throws WasmException {
    // Define multiple host functions
    final FunctionType mathType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});

    linker.defineHostFunction(
        "env",
        "add",
        mathType,
        (params) -> {
          final int a = params[0].asI32();
          final int b = params[1].asI32();
          return new WasmValue[] {WasmValue.ofI32(a + b)};
        });

    linker.defineHostFunction(
        "env",
        "sub",
        mathType,
        (params) -> {
          final int a = params[0].asI32();
          final int b = params[1].asI32();
          return new WasmValue[] {WasmValue.ofI32(a - b)};
        });

    linker.defineHostFunction(
        "env",
        "mul",
        mathType,
        (params) -> {
          final int a = params[0].asI32();
          final int b = params[1].asI32();
          return new WasmValue[] {WasmValue.ofI32(a * b)};
        });

    // All definitions should succeed
    assertNotNull(linker);
  }

  @Test
  void testLinkerResourceManagement() {
    assertTrue(linker.isValid());

    linker.close();
    assertFalse(linker.isValid());

    // Operations after close should throw
    assertThrows(
        IllegalStateException.class,
        () ->
            linker.defineHostFunction(
                "env",
                "test",
                new FunctionType(new WasmValueType[0], new WasmValueType[0]),
                (params) -> new WasmValue[0]));
  }

  @Test
  void testParameterValidation() {
    // Test null parameters
    assertThrows(
        IllegalArgumentException.class,
        () ->
            linker.defineHostFunction(
                null,
                "test",
                new FunctionType(new WasmValueType[0], new WasmValueType[0]),
                (params) -> new WasmValue[0]));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            linker.defineHostFunction(
                "env",
                null,
                new FunctionType(new WasmValueType[0], new WasmValueType[0]),
                (params) -> new WasmValue[0]));

    assertThrows(
        IllegalArgumentException.class,
        () -> linker.defineHostFunction("env", "test", null, (params) -> new WasmValue[0]));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            linker.defineHostFunction(
                "env", "test", new FunctionType(new WasmValueType[0], new WasmValueType[0]), null));

    // Test empty strings
    assertThrows(
        IllegalArgumentException.class,
        () ->
            linker.defineHostFunction(
                "",
                "test",
                new FunctionType(new WasmValueType[0], new WasmValueType[0]),
                (params) -> new WasmValue[0]));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            linker.defineHostFunction(
                "env",
                "",
                new FunctionType(new WasmValueType[0], new WasmValueType[0]),
                (params) -> new WasmValue[0]));
  }

  @Test
  void testLinkerStaticFactoryMethod() throws WasmException {
    final Linker staticLinker = Linker.create(engine);
    assertNotNull(staticLinker);
    assertTrue(staticLinker.isValid());
    assertEquals(engine, staticLinker.getEngine());
    staticLinker.close();
  }

  @Test
  void testLinkerWithInvalidEngine() {
    // Test with null engine
    assertThrows(IllegalArgumentException.class, () -> Linker.create(null));
  }
}

package ai.tegmentum.wasmtime4j.comparison.module;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests importing multiple host functions in WebAssembly modules.
 *
 * <p>This test validates that wasmtime4j correctly handles multiple host function imports via the
 * Linker API, matching Wasmtime's behavior.
 *
 * <p>Expected Wasmtime behavior:
 *
 * <ul>
 *   <li>Multiple host functions can be defined in a single Linker
 *   <li>WebAssembly module can import multiple functions from same or different modules
 *   <li>Each import is resolved independently by module and name
 *   <li>All imported functions work correctly when called from WebAssembly
 * </ul>
 *
 * <p>Reference: <a
 * href="https://docs.wasmtime.dev/api/wasmtime/struct.Linker.html#method.func_wrap">https://docs.wasmtime.dev/api/wasmtime/struct.Linker.html#method.func_wrap</a>
 *
 * <p>This test runs on both JNI and Panama runtimes to ensure both correctly implement Wasmtime's
 * behavior.
 */
public class ModuleImportMultipleTest extends DualRuntimeTest {

  private Engine engine;
  private Store store;

  @AfterEach
  void cleanupRuntime() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    clearRuntimeSelection();
  }

  private void setupRuntime() throws Exception {
    engine = Engine.create();
    store = engine.createStore();
  }

  /**
   * Tests importing multiple functions from the host.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import multiple functions from host")
  public void testImportMultipleFunctions(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final Linker linker = Linker.create(engine);

    final HostFunction addFunc =
        HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() + params[1].asInt()));
    final FunctionType addType =
        FunctionType.multiValue(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});
    linker.defineHostFunction("math", "add", addType, addFunc);

    final HostFunction mulFunc =
        HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() * params[1].asInt()));
    final FunctionType mulType =
        FunctionType.multiValue(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});
    linker.defineHostFunction("math", "mul", mulType, mulFunc);

    final String wat =
        """
        (module
          (import "math" "add" (func $add (param i32 i32) (result i32)))
          (import "math" "mul" (func $mul (param i32 i32) (result i32)))
          (func (export "compute") (param i32 i32) (result i32)
            ;; (a + b) * 2
            local.get 0
            local.get 1
            call $add
            i32.const 2
            call $mul
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results =
        instance.callFunction("compute", WasmValue.i32(10), WasmValue.i32(11));
    assertEquals(42, results[0].asInt()); // (10 + 11) * 2 = 42

    instance.close();
    linker.close();
  }

  /**
   * Tests a mix of imported and internal functions in a WebAssembly module.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Mix of imported and internal functions")
  public void testMixOfImportedAndInternalFunctions(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final Linker linker = Linker.create(engine);

    final HostFunction addFunc =
        HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() + params[1].asInt()));
    final FunctionType funcType =
        FunctionType.multiValue(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});
    linker.defineHostFunction("env", "external_add", funcType, addFunc);

    final String wat =
        """
        (module
          (import "env" "external_add" (func $external_add (param i32 i32) (result i32)))
          (func $internal_mul (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.mul
          )
          (func (export "compute") (param i32 i32 i32) (result i32)
            ;; (a + b) * c using imported add and internal multiply
            local.get 0
            local.get 1
            call $external_add
            local.get 2
            call $internal_mul
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results =
        instance.callFunction("compute", WasmValue.i32(3), WasmValue.i32(4), WasmValue.i32(6));
    assertEquals(42, results[0].asInt()); // (3 + 4) * 6 = 42

    instance.close();
    linker.close();
  }
}

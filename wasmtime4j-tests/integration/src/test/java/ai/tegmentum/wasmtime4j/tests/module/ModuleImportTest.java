package ai.tegmentum.wasmtime4j.tests.module;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests WebAssembly module import functionality for host functions.
 *
 * <p>This test validates that wasmtime4j correctly implements Wasmtime's import behavior for host
 * functions provided via the Linker API.
 *
 * <p>Expected Wasmtime behavior:
 *
 * <ul>
 *   <li>Host functions can be defined in a Linker with module and name
 *   <li>WebAssembly modules can import and call host functions
 *   <li>Function signatures must match between import declaration and host definition
 *   <li>Host functions receive parameters and return results correctly
 *   <li>Host functions can access and modify Store data
 * </ul>
 *
 * <p>Reference: <a
 * href="https://docs.wasmtime.dev/api/wasmtime/struct.Linker.html">https://docs.wasmtime.dev/api/wasmtime/struct.Linker.html</a>
 *
 * <p>This test runs on both JNI and Panama runtimes to ensure both correctly implement Wasmtime's
 * behavior.
 */
public class ModuleImportTest extends DualRuntimeTest {

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
   * Tests importing a function from the host into a WebAssembly module.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import function from host")
  public void testImportFunctionFromHost(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final Linker<Void> linker = Linker.create(engine);

    // Define host function
    final HostFunction getAnswer = HostFunction.singleValue(params -> WasmValue.i32(42));
    final FunctionType funcType =
        FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
    linker.defineHostFunction("env", "get_answer", funcType, getAnswer);

    final String wat =
        """
        (module
          (import "env" "get_answer" (func $get_answer (result i32)))
          (func (export "call_host") (result i32)
            call $get_answer
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("call_host");
    assertEquals(42, results[0].asInt());

    instance.close();
    linker.close();
  }

  /**
   * Tests importing a function with parameters from the host.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import function with parameters from host")
  public void testImportFunctionWithParameters(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final Linker<Void> linker = Linker.create(engine);

    // Define host function that multiplies by 2
    final HostFunction doubleFunc =
        HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() * 2));
    final FunctionType funcType =
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
    linker.defineHostFunction("env", "double", funcType, doubleFunc);

    final String wat =
        """
        (module
          (import "env" "double" (func $double (param i32) (result i32)))
          (func (export "call_double") (param i32) (result i32)
            local.get 0
            call $double
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("call_double", WasmValue.i32(21));
    assertEquals(42, results[0].asInt());

    instance.close();
    linker.close();
  }
}

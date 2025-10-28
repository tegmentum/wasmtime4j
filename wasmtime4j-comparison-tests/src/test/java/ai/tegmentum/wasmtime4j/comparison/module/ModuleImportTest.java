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

/** Tests for module imports from host across JNI and Panama implementations. */
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

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import function from host")
  public void testImportFunctionFromHost(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final Linker linker = Linker.create(engine);

    // Define host function
    final HostFunction getAnswer = HostFunction.singleValue(params -> WasmValue.i32(42));
    final FunctionType funcType =
        FunctionType.multiValue(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
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

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import function with parameters from host")
  public void testImportFunctionWithParameters(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final Linker linker = Linker.create(engine);

    // Define host function that multiplies by 2
    final HostFunction doubleFunc =
        HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() * 2));
    final FunctionType funcType =
        FunctionType.multiValue(
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

package ai.tegmentum.wasmtime4j.comparison.module;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/** Tests for module exports across JNI and Panama implementations. */
public class ModuleExportTest extends DualRuntimeTest {

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
   * Tests exporting a single function from a WebAssembly module.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Export single function")
  public void testExportSingleFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final String wat =
        """
        (module
          (func (export "add") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.add
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    assertTrue(instance.getFunction("add").isPresent());
    final WasmValue[] results = instance.callFunction("add", WasmValue.i32(10), WasmValue.i32(32));
    assertEquals(42, results[0].asInt());

    instance.close();
  }

  /**
   * Tests exporting multiple functions from a WebAssembly module.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Export multiple functions")
  public void testExportMultipleFunctions(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final String wat =
        """
        (module
          (func (export "add") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.add
          )
          (func (export "sub") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.sub
          )
          (func (export "mul") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.mul
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    // Test all three exported functions
    assertTrue(instance.getFunction("add").isPresent());
    assertTrue(instance.getFunction("sub").isPresent());
    assertTrue(instance.getFunction("mul").isPresent());

    WasmValue[] results = instance.callFunction("add", WasmValue.i32(10), WasmValue.i32(32));
    assertEquals(42, results[0].asInt());

    results = instance.callFunction("sub", WasmValue.i32(50), WasmValue.i32(8));
    assertEquals(42, results[0].asInt());

    results = instance.callFunction("mul", WasmValue.i32(6), WasmValue.i32(7));
    assertEquals(42, results[0].asInt());

    instance.close();
  }

  /**
   * Tests exporting memory from a WebAssembly module.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Export memory")
  public void testExportMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final String wat =
        """
        (module
          (memory (export "mem") 1)
          (func (export "store") (param i32 i32)
            local.get 0
            local.get 1
            i32.store
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    assertTrue(instance.getMemory("mem").isPresent());
    assertTrue(instance.getFunction("store").isPresent());

    instance.close();
  }

  /**
   * Tests exporting a table from a WebAssembly module.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Export table")
  public void testExportTable(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final String wat =
        """
        (module
          (table (export "tbl") 10 funcref)
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    assertTrue(instance.getTable("tbl").isPresent());

    instance.close();
  }

  /**
   * Tests exporting the same function with multiple names from a WebAssembly module.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Export same function with different names")
  public void testExportSameFunctionMultipleTimes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final String wat =
        """
        (module
          (func $double (export "double") (export "times_two") (export "multiply_by_2")
                (param i32) (result i32)
            local.get 0
            i32.const 2
            i32.mul
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    // All three names should refer to the same function
    assertTrue(instance.getFunction("double").isPresent());
    assertTrue(instance.getFunction("times_two").isPresent());
    assertTrue(instance.getFunction("multiply_by_2").isPresent());

    // All three names should work
    WasmValue[] results = instance.callFunction("double", WasmValue.i32(21));
    assertEquals(42, results[0].asInt());

    results = instance.callFunction("times_two", WasmValue.i32(21));
    assertEquals(42, results[0].asInt());

    results = instance.callFunction("multiply_by_2", WasmValue.i32(21));
    assertEquals(42, results[0].asInt());

    instance.close();
  }
}

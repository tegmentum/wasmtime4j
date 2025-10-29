package ai.tegmentum.wasmtime4j.comparison.module;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/** Tests for module edge cases. */
public class ModuleEdgeCasesTest extends DualRuntimeTest {
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
   * Tests that querying for non-existent exports returns empty.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Get non-existent export returns empty")
  public void testGetNonExistentExport(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final String wat =
        """
        (module
          (func (export "exists") (result i32)
            i32.const 42
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    assertTrue(instance.getFunction("exists").isPresent());
    assertFalse(instance.getFunction("does_not_exist").isPresent());
    assertFalse(instance.getGlobal("does_not_exist").isPresent());
    assertFalse(instance.getMemory("does_not_exist").isPresent());
    assertFalse(instance.getTable("does_not_exist").isPresent());

    instance.close();
  }

  /**
   * Tests instantiating a module with no imports or exports.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Module with no imports or exports")
  public void testModuleWithNoImportsOrExports(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final String wat =
        """
        (module
          (func $internal
            nop
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    assertNotNull(instance);
    instance.close();
  }
}

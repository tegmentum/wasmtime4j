package ai.tegmentum.wasmtime4j.comparison.module;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests memory sharing between WebAssembly instances.
 *
 * <p>This test validates that wasmtime4j correctly implements Wasmtime's memory sharing behavior.
 * Per Wasmtime documentation, memory exported from one instance can be imported by another
 * instance, and modifications are visible across both instances.
 *
 * <p>Reference: <a
 * href="https://docs.wasmtime.dev/api/wasmtime/struct.Memory.html">https://docs.wasmtime.dev/api/wasmtime/struct.Memory.html</a>
 *
 * <p>This test runs on both JNI and Panama runtimes to ensure both correctly implement Wasmtime's
 * behavior.
 */
public class ModuleMemoryTest extends DualRuntimeTest {

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
   * Tests importing and exporting memory in WebAssembly modules.
   *
   * <p>Expected Wasmtime behavior:
   *
   * <ul>
   *   <li>Memory exported from one instance can be retrieved via getMemory()
   *   <li>Exported memory can be defined in a linker for import by other modules
   *   <li>Memory handle remains valid when passed through linker
   *   <li>Modifications in one instance are visible when accessed through imported memory
   * </ul>
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import and export memory in same module")
  public void testImportAndExportMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    // First create a module that exports memory
    final String wat1 =
        """
        (module
          (memory (export "shared_mem") 1)
          (func (export "init_mem")
            i32.const 0
            i32.const 42
            i32.store
          )
        )
        """;

    final Module module1 = engine.compileWat(wat1);
    final Instance instance1 = module1.instantiate(store);

    // Initialize memory
    instance1.callFunction("init_mem");

    // Get memory and keep a reference to it
    final var sharedMemory = instance1.getMemory("shared_mem").orElseThrow();

    // Now create a linker and add the memory
    final Linker linker = Linker.create(engine);
    linker.defineMemory(store, "env", "imported_mem", sharedMemory);

    // Second module imports the memory
    final String wat2 =
        """
        (module
          (import "env" "imported_mem" (memory 1))
          (func (export "read_mem") (result i32)
            i32.const 0
            i32.load
          )
        )
        """;

    final Module module2 = engine.compileWat(wat2);
    final Instance instance2 = linker.instantiate(store, module2);

    // Read from imported memory
    final WasmValue[] results = instance2.callFunction("read_mem");
    assertEquals(42, results[0].asInt());

    instance2.close();
    instance1.close();
    linker.close();
  }
}

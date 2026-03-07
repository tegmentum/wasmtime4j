/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.tests.module;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests edge cases in WebAssembly module handling.
 *
 * <p>This test validates that wasmtime4j correctly handles edge cases and boundary conditions in
 * module instantiation, export retrieval, and function invocation, matching Wasmtime's behavior.
 *
 * <p>Expected Wasmtime behavior:
 *
 * <ul>
 *   <li>Empty modules can be compiled and instantiated
 *   <li>Accessing non-existent exports returns empty Optional
 *   <li>Multiple instances of same module are independent
 *   <li>Zero-parameter and zero-result functions work correctly
 *   <li>Module names and export names handle special characters correctly
 * </ul>
 *
 * <p>Reference: <a
 * href="https://docs.wasmtime.dev/api/wasmtime/struct.Module.html">https://docs.wasmtime.dev/api/wasmtime/struct.Module.html</a>
 *
 * <p>This test runs on both JNI and Panama runtimes to ensure both correctly implement Wasmtime's
 * behavior.
 */
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

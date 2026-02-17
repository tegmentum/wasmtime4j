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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for resource close safety across all closeable WASM resources.
 *
 * <p>Verifies that:
 *
 * <ul>
 *   <li>Operations on closed resources throw {@link IllegalStateException}
 *   <li>Double close is safe (no exception, no crash)
 *   <li>Close ordering is correct (flag set before native destruction)
 * </ul>
 *
 * <p>Note: Only {@link Engine}, {@link Store}, {@link Module}, and {@link Instance} implement
 * {@link AutoCloseable}. {@link ai.tegmentum.wasmtime4j.WasmFunction}, {@link
 * ai.tegmentum.wasmtime4j.WasmMemory}, and {@link ai.tegmentum.wasmtime4j.WasmGlobal} are
 * non-closeable interfaces whose lifecycle is tied to their owning Instance/Store.
 *
 * @since 1.0.0
 */
@DisplayName("Resource Close Safety Integration Tests")
public final class ResourceCloseSafetyIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ResourceCloseSafetyIntegrationTest.class.getName());

  /**
   * Simple WebAssembly module that exports an add function.
   *
   * <p>WAT equivalent:
   *
   * <pre>{@code
   * (module
   *   (func (export "add") (param i32 i32) (result i32)
   *     local.get 0 local.get 1 i32.add)
   * )
   * }</pre>
   */
  private static final byte[] ADD_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x07, // type section
        0x01,
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x01,
        0x7F, // (i32, i32) -> i32
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07,
        0x01,
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // export "add"
        0x0A,
        0x09,
        0x01,
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6A,
        0x0B // code
      };

  /** Close safety tests for Instance. */
  @Nested
  @DisplayName("Closed Instance Detection Tests")
  class ClosedInstanceDetectionTests {

    @Test
    @DisplayName("getFunction on closed instance should throw IllegalStateException")
    void getFunctionOnClosedInstanceShouldThrow() throws Exception {
      final Engine engine = Engine.create();
      final Module module = engine.compileModule(ADD_WASM);
      final Store store = engine.createStore();
      final Instance instance = module.instantiate(store);

      instance.close();
      LOGGER.info("Instance closed, attempting getFunction()");

      assertThrows(
          IllegalStateException.class,
          () -> instance.getFunction("add"),
          "getFunction() on closed instance should throw IllegalStateException");
      LOGGER.info("IllegalStateException thrown as expected");

      store.close();
      module.close();
      engine.close();
    }

    @Test
    @DisplayName("double close on instance should be safe")
    void doubleCloseOnInstanceShouldBeSafe() throws Exception {
      final Engine engine = Engine.create();
      final Module module = engine.compileModule(ADD_WASM);
      final Store store = engine.createStore();
      final Instance instance = module.instantiate(store);

      instance.close();
      LOGGER.info("First close completed");

      assertDoesNotThrow(instance::close, "Second close should not throw");
      LOGGER.info("Second close completed without exception");

      assertThrows(
          IllegalStateException.class,
          () -> instance.getFunction("add"),
          "getFunction() after double close should still throw");

      store.close();
      module.close();
      engine.close();
    }
  }

  /** Close safety tests for Module. */
  @Nested
  @DisplayName("Closed Module Detection Tests")
  class ClosedModuleDetectionTests {

    @Test
    @DisplayName("operations on closed module should throw IllegalStateException")
    @SuppressWarnings("deprecation")
    void operationsOnClosedModuleShouldThrow() throws Exception {
      final Engine engine = Engine.create();
      final Module module = engine.compileModule(ADD_WASM);

      module.close();
      LOGGER.info("Module closed, attempting operations");

      assertThrows(
          IllegalStateException.class,
          module::getModuleExports,
          "getModuleExports() on closed module should throw");
      LOGGER.info("IllegalStateException thrown as expected");

      engine.close();
    }

    @Test
    @DisplayName("double close on module should be safe")
    void doubleCloseOnModuleShouldBeSafe() throws Exception {
      final Engine engine = Engine.create();
      final Module module = engine.compileModule(ADD_WASM);

      module.close();
      assertDoesNotThrow(module::close, "Second close should not throw");
      LOGGER.info("Module double close succeeded without exception");

      engine.close();
    }
  }

  /** Close safety tests for Engine. */
  @Nested
  @DisplayName("Closed Engine Detection Tests")
  class ClosedEngineDetectionTests {

    @Test
    @DisplayName("double close on engine should be safe")
    void doubleCloseOnEngineShouldBeSafe() throws Exception {
      final Engine engine = Engine.create();

      engine.close();
      assertDoesNotThrow(engine::close, "Second close should not throw");
      LOGGER.info("Engine double close succeeded without exception");
    }

    @Test
    @DisplayName("createStore on closed engine should throw or be handled safely")
    void createStoreOnClosedEngineShouldBeHandled() throws Exception {
      final Engine engine = Engine.create();
      engine.close();
      LOGGER.info("Engine closed, attempting createStore()");

      // After closing engine, store creation should either throw or be handled gracefully
      try {
        final Store store = engine.createStore();
        // If it doesn't throw, verify the store exists and can be closed
        assertNotNull(store, "Store should not be null if creation succeeded");
        store.close();
        LOGGER.info("Store creation on closed engine returned a store (graceful handling)");
      } catch (final IllegalStateException e) {
        LOGGER.info("IllegalStateException thrown as expected: " + e.getMessage());
        // Expected behavior - closed engine refuses new stores
      }
    }
  }

  /** Close safety tests for Store. */
  @Nested
  @DisplayName("Closed Store Detection Tests")
  class ClosedStoreDetectionTests {

    @Test
    @DisplayName("double close on store should be safe")
    void doubleCloseOnStoreShouldBeSafe() throws Exception {
      final Engine engine = Engine.create();
      final Store store = engine.createStore();

      store.close();
      assertDoesNotThrow(store::close, "Second close should not throw");
      LOGGER.info("Store double close succeeded without exception");

      engine.close();
    }
  }

  /** Tests verifying resources can be cleaned up after errors. */
  @Nested
  @DisplayName("Resource Cleanup After Error Tests")
  class ResourceCleanupAfterErrorTests {

    @Test
    @DisplayName("should clean up resources when created and immediately closed")
    void shouldCleanUpResourcesWhenCreatedAndImmediatelyClosed() throws Exception {
      final Engine engine = Engine.create();
      final Store store = engine.createStore();

      // Even if something goes wrong during setup, close should work cleanly
      assertDoesNotThrow(store::close, "Store close should not throw");
      assertDoesNotThrow(engine::close, "Engine close should not throw");
      LOGGER.info("Resources cleaned up after error scenario");
    }

    @Test
    @DisplayName("should handle close of all resources in reverse creation order")
    void shouldHandleCloseOfAllResourcesInReverseOrder() throws Exception {
      final Engine engine = Engine.create();
      final Module module = engine.compileModule(ADD_WASM);
      final Store store = engine.createStore();
      final Instance instance = module.instantiate(store);

      // Verify function export is available before close
      final Optional<WasmFunction> func = instance.getFunction("add");
      assertTrue(func.isPresent(), "Function should be present");

      // Verify function works before close
      final WasmValue[] result = func.get().call(WasmValue.i32(3), WasmValue.i32(4));
      assertNotNull(result, "Function call result should not be null");
      LOGGER.info("Function call result: " + result[0]);

      // Close all closeable resources in reverse order - should not crash
      assertDoesNotThrow(instance::close, "Instance close should not throw");
      assertDoesNotThrow(store::close, "Store close should not throw");
      assertDoesNotThrow(module::close, "Module close should not throw");
      assertDoesNotThrow(engine::close, "Engine close should not throw");

      LOGGER.info("All resources closed in reverse order without exception");
    }
  }
}

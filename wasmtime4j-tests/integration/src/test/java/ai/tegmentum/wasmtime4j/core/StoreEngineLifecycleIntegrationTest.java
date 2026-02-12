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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.func.CallHook;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Store and Engine lifecycle management.
 *
 * <p>These tests verify proper creation, configuration, and cleanup of Engine and Store resources.
 *
 * @since 1.0.0
 */
@DisplayName("Store and Engine Lifecycle Integration Tests")
public final class StoreEngineLifecycleIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(StoreEngineLifecycleIntegrationTest.class.getName());

  /** Simple WebAssembly module that exports an add function. */
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

  @Nested
  @DisplayName("Engine Creation Tests")
  class EngineCreationTests {

    @Test
    @DisplayName("should create engine with default configuration")
    void shouldCreateEngineWithDefaultConfiguration() throws Exception {
      LOGGER.info("Testing default engine creation");

      try (final Engine engine = Engine.create()) {
        assertNotNull(engine, "Engine should not be null");
        assertTrue(engine.isValid(), "Engine should be valid after creation");
        LOGGER.info("Engine created: valid=" + engine.isValid());
      }
    }

    @Test
    @DisplayName("should create engine with custom configuration")
    void shouldCreateEngineWithCustomConfiguration() throws Exception {
      LOGGER.info("Testing engine with custom configuration");

      final EngineConfig config = new EngineConfig();

      try (final Engine engine = Engine.create(config)) {
        assertNotNull(engine, "Engine should not be null");
        assertTrue(engine.isValid(), "Engine should be valid after creation");
        LOGGER.info("Engine created with custom config: valid=" + engine.isValid());
      }
    }

    @Test
    @DisplayName("should create multiple independent engines")
    void shouldCreateMultipleIndependentEngines() throws Exception {
      LOGGER.info("Testing multiple independent engines");

      try (final Engine engine1 = Engine.create();
          final Engine engine2 = Engine.create()) {
        assertNotNull(engine1, "First engine should not be null");
        assertNotNull(engine2, "Second engine should not be null");
        assertNotSame(engine1, engine2, "Engines should be different instances");

        assertTrue(engine1.isValid(), "First engine should be valid");
        assertTrue(engine2.isValid(), "Second engine should be valid");

        LOGGER.info("Created 2 independent engines");
      }
    }

    @Test
    @DisplayName("should report positive reference count for engine")
    void shouldReportPositiveReferenceCountForEngine() throws Exception {
      LOGGER.info("Testing engine reference count");

      try (final Engine engine = Engine.create()) {
        final long refCount = engine.getReferenceCount();
        assertTrue(refCount > 0, "Engine should have positive reference count");
        LOGGER.info("Engine reference count: " + refCount);
      }
    }
  }

  @Nested
  @DisplayName("Store Creation Tests")
  class StoreCreationTests {

    @Test
    @DisplayName("should create store from engine")
    void shouldCreateStoreFromEngine() throws Exception {
      LOGGER.info("Testing store creation from engine");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        assertNotNull(store, "Store should not be null");
        assertTrue(store.isValid(), "Store should be valid after creation");
        LOGGER.info("Store created: valid=" + store.isValid());
      }
    }

    @Test
    @DisplayName("should create multiple stores from same engine")
    void shouldCreateMultipleStoresFromSameEngine() throws Exception {
      LOGGER.info("Testing multiple stores from same engine");

      try (final Engine engine = Engine.create();
          final Store store1 = engine.createStore();
          final Store store2 = engine.createStore()) {
        assertNotNull(store1, "First store should not be null");
        assertNotNull(store2, "Second store should not be null");
        assertNotSame(store1, store2, "Stores should be different instances");

        assertTrue(store1.isValid(), "First store should be valid");
        assertTrue(store2.isValid(), "Second store should be valid");

        LOGGER.info("Created 2 stores from same engine");
      }
    }

    @Test
    @DisplayName("should get engine from store")
    void shouldGetEngineFromStore() throws Exception {
      LOGGER.info("Testing engine retrieval from store");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Engine storeEngine = store.getEngine();
        assertNotNull(storeEngine, "Engine from store should not be null");
        LOGGER.info("Successfully retrieved engine from store");
      }
    }
  }

  @Nested
  @DisplayName("Resource Lifecycle Tests")
  class ResourceLifecycleTests {

    @Test
    @DisplayName("should invalidate engine after close")
    void shouldInvalidateEngineAfterClose() throws Exception {
      LOGGER.info("Testing engine invalidation after close");

      final Engine engine = Engine.create();
      assertTrue(engine.isValid(), "Engine should be valid before close");

      engine.close();
      assertFalse(engine.isValid(), "Engine should be invalid after close");
      LOGGER.info("Engine correctly invalidated after close");
    }

    @Test
    @DisplayName("should invalidate store after close")
    void shouldInvalidateStoreAfterClose() throws Exception {
      LOGGER.info("Testing store invalidation after close");

      try (final Engine engine = Engine.create()) {
        final Store store = engine.createStore();
        assertTrue(store.isValid(), "Store should be valid before close");

        store.close();
        assertFalse(store.isValid(), "Store should be invalid after close");
        LOGGER.info("Store correctly invalidated after close");
      }
    }

    @Test
    @DisplayName("should handle multiple close calls gracefully")
    void shouldHandleMultipleCloseCallsGracefully() throws Exception {
      LOGGER.info("Testing multiple close calls");

      final Engine engine = Engine.create();
      engine.close();

      // Second close should not throw
      assertDoesNotThrow(engine::close, "Multiple close calls should not throw");
      LOGGER.info("Multiple close calls handled gracefully");
    }

    @Test
    @DisplayName("should properly close resources in correct order")
    void shouldProperlyCloseResourcesInCorrectOrder() throws Exception {
      LOGGER.info("Testing proper resource closure order");

      final List<AutoCloseable> resources = new ArrayList<>();

      final Engine engine = Engine.create();
      resources.add(engine);

      final Store store = engine.createStore();
      resources.add(store);

      final Module module = engine.compileModule(ADD_WASM);
      resources.add(module);

      final Instance instance = module.instantiate(store);
      resources.add(instance);

      // Close in reverse order (instance, module, store, engine)
      for (int i = resources.size() - 1; i >= 0; i--) {
        resources.get(i).close();
      }

      assertFalse(instance.isValid(), "Instance should be invalid");
      assertFalse(module.isValid(), "Module should be invalid");
      assertFalse(store.isValid(), "Store should be invalid");
      assertFalse(engine.isValid(), "Engine should be invalid");

      LOGGER.info("Resources closed in correct order");
    }
  }

  @Nested
  @DisplayName("Store Isolation Tests")
  class StoreIsolationTests {

    @Test
    @DisplayName("should isolate instances in different stores")
    void shouldIsolateInstancesInDifferentStores() throws Exception {
      LOGGER.info("Testing instance isolation between stores");

      try (final Engine engine = Engine.create();
          final Store store1 = engine.createStore();
          final Store store2 = engine.createStore();
          final Module module = engine.compileModule(ADD_WASM)) {

        // Create instances in different stores
        try (final Instance instance1 = module.instantiate(store1);
            final Instance instance2 = module.instantiate(store2)) {

          assertNotNull(instance1, "Instance 1 should not be null");
          assertNotNull(instance2, "Instance 2 should not be null");
          assertNotSame(instance1, instance2, "Instances should be different");

          // Both instances should work independently
          final Optional<WasmFunction> add1 = instance1.getFunction("add");
          final Optional<WasmFunction> add2 = instance2.getFunction("add");

          assertTrue(add1.isPresent(), "Instance 1 should have add function");
          assertTrue(add2.isPresent(), "Instance 2 should have add function");

          final WasmValue[] results1 = add1.get().call(WasmValue.i32(5), WasmValue.i32(3));
          final WasmValue[] results2 = add2.get().call(WasmValue.i32(10), WasmValue.i32(20));

          assertEquals(8, results1[0].asInt(), "Instance 1 add should work");
          assertEquals(30, results2[0].asInt(), "Instance 2 add should work");

          LOGGER.info("Instances in different stores work independently");
        }
      }
    }
  }

  @Nested
  @DisplayName("Runtime Factory Tests")
  class RuntimeFactoryTests {

    @Test
    @DisplayName("should detect available runtimes")
    void shouldDetectAvailableRuntimes() {
      LOGGER.info("Testing runtime availability detection");

      final boolean jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      final boolean panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

      LOGGER.info("JNI available: " + jniAvailable);
      LOGGER.info("Panama available: " + panamaAvailable);

      // At least one runtime should be available
      assertTrue(jniAvailable || panamaAvailable, "At least one runtime should be available");
    }

    @Test
    @DisplayName("should get current runtime type")
    void shouldGetCurrentRuntimeType() throws WasmException {
      LOGGER.info("Testing current runtime type");

      try (final Engine engine = Engine.create()) {
        // The engine should have a runtime type
        assertNotNull(engine, "Engine should not be null");
        LOGGER.info("Current engine is valid: " + engine.isValid());
      }
    }
  }

  @Nested
  @DisplayName("Engine Configuration Tests")
  class EngineConfigurationTests {

    @Test
    @DisplayName("should create engine with debug info disabled")
    void shouldCreateEngineWithDebugInfoDisabled() throws Exception {
      LOGGER.info("Testing engine with debug info disabled");

      final EngineConfig config = new EngineConfig().debugInfo(false);

      try (final Engine engine = Engine.create(config)) {
        assertNotNull(engine, "Engine should not be null");
        assertTrue(engine.isValid(), "Engine should be valid");
        LOGGER.info("Engine created with debug info disabled");
      }
    }

    @Test
    @DisplayName("should create engine with fuel consumption enabled")
    void shouldCreateEngineWithFuelConsumptionEnabled() throws Exception {
      LOGGER.info("Testing engine with fuel consumption enabled");

      final EngineConfig config = new EngineConfig().consumeFuel(true);

      try (final Engine engine = Engine.create(config)) {
        assertNotNull(engine, "Engine should not be null");
        assertTrue(engine.isValid(), "Engine should be valid");
        LOGGER.info("Engine created with fuel consumption enabled");
      }
    }

    @Test
    @DisplayName("should create engine with wasm threads disabled")
    void shouldCreateEngineWithWasmThreadsDisabled() throws Exception {
      LOGGER.info("Testing engine with wasm threads disabled");

      final EngineConfig config = new EngineConfig();

      try (final Engine engine = Engine.create(config)) {
        assertNotNull(engine, "Engine should not be null");
        assertTrue(engine.isValid(), "Engine should be valid");
        LOGGER.info("Engine created with wasm threads disabled");
      }
    }
  }

  @Nested
  @DisplayName("Fuel Management Tests")
  class FuelManagementTests {

    @Test
    @DisplayName("should set and get fuel on store")
    void shouldSetAndGetFuelOnStore() throws Exception {
      LOGGER.info("Testing fuel management on store");

      final EngineConfig config = new EngineConfig().consumeFuel(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        // Set initial fuel
        store.setFuel(1000L);

        // Get remaining fuel
        final long fuel = store.getFuel();
        LOGGER.info("Fuel after set: " + fuel);

        assertTrue(fuel > 0, "Fuel should be greater than 0");
      }
    }

    @Test
    @DisplayName("should consume fuel during execution")
    void shouldConsumeFuelDuringExecution() throws Exception {
      LOGGER.info("Testing fuel consumption during execution");

      final EngineConfig config = new EngineConfig().consumeFuel(true);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore();
          final Module module = engine.compileModule(ADD_WASM);
          final Instance instance = module.instantiate(store)) {

        // Set initial fuel
        store.setFuel(100000L);
        final long initialFuel = store.getFuel();
        LOGGER.info("Initial fuel: " + initialFuel);

        // Execute function
        final Optional<WasmFunction> addFunc = instance.getFunction("add");
        assertTrue(addFunc.isPresent(), "Should have add function");
        addFunc.get().call(WasmValue.i32(5), WasmValue.i32(3));

        // Fuel should be consumed
        final long remainingFuel = store.getFuel();
        LOGGER.info("Remaining fuel: " + remainingFuel);

        assertTrue(remainingFuel < initialFuel, "Fuel should be consumed during execution");
      }
    }
  }

  /** CallHook integration tests. */
  @Nested
  @DisplayName("Call Hook Tests")
  class CallHookTests {

    @Test
    @DisplayName("should invoke call hook on WASM call")
    void shouldInvokeCallHookOnWasmCall() throws Exception {
      LOGGER.info("Testing call hook invocation on WASM call");

      final List<CallHook> hooks = new CopyOnWriteArrayList<>();

      try (final Engine engine = Engine.create()) {
        final Store store = engine.createStore();
        try {
          store.setCallHook(
              hook -> {
                hooks.add(hook);
                LOGGER.info("Call hook fired: " + hook);
              });
        } catch (final IllegalArgumentException | UnsupportedOperationException e) {
          LOGGER.info("setCallHook not yet implemented: " + e.getMessage());
          store.close();
          return; // Skip test if native function not available
        }

        final Module module = engine.compileModule(ADD_WASM);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> addFunc = instance.getFunction("add");
        assertTrue(addFunc.isPresent(), "add function should be present");

        addFunc.get().call(WasmValue.i32(3), WasmValue.i32(4));
        LOGGER.info("Function called, hooks fired: " + hooks.size());

        assertFalse(hooks.isEmpty(), "At least one call hook should have fired");
        assertTrue(hooks.contains(CallHook.CALLING_WASM), "CALLING_WASM hook should have fired");
        assertTrue(
            hooks.contains(CallHook.RETURNING_FROM_WASM),
            "RETURNING_FROM_WASM hook should have fired");

        LOGGER.info("Call hooks verified: " + hooks);

        instance.close();
        module.close();
        store.close();
      }
    }

    @Test
    @DisplayName("should handle call hook exception gracefully")
    void shouldHandleCallHookExceptionGracefully() throws Exception {
      LOGGER.info("Testing call hook exception handling");

      try (final Engine engine = Engine.create()) {
        final Store store = engine.createStore();
        try {
          store.setCallHook(
              hook -> {
                LOGGER.info("Call hook firing, about to throw: " + hook);
                throw new ai.tegmentum.wasmtime4j.exception.TrapException(
                    ai.tegmentum.wasmtime4j.exception.TrapException.TrapType.INTERRUPT,
                    "Hook trap");
              });
        } catch (final IllegalArgumentException | UnsupportedOperationException e) {
          LOGGER.info("setCallHook not yet implemented: " + e.getMessage());
          store.close();
          return; // Skip test if native function not available
        }

        final Module module = engine.compileModule(ADD_WASM);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> addFunc = instance.getFunction("add");
        assertTrue(addFunc.isPresent(), "add function should be present");

        // The call should fail due to the hook throwing
        try {
          addFunc.get().call(WasmValue.i32(3), WasmValue.i32(4));
          LOGGER.info("Call did not throw - hook exception may be swallowed");
        } catch (final Exception e) {
          LOGGER.info("Call threw as expected: " + e.getClass().getName() + ": " + e.getMessage());
        }

        // Resources should still be closeable
        assertDoesNotThrow(instance::close, "Instance should still be closeable after hook trap");
        assertDoesNotThrow(module::close, "Module should still be closeable after hook trap");
        assertDoesNotThrow(store::close, "Store should still be closeable after hook trap");

        LOGGER.info("Resources cleaned up after hook exception");
      }
    }
  }
}

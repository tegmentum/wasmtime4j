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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for WebAssembly Engine configuration options.
 *
 * <p>Tests various engine configuration settings including optimization levels, compilation
 * strategies, and feature flags.
 *
 * @since 1.0.0
 */
@DisplayName("Engine Configuration Integration Tests")
@SuppressWarnings("deprecation")
public class EngineConfigurationTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(EngineConfigurationTest.class.getName());

  // Simple add function for testing: (param i32 i32) (result i32)
  private static final byte[] ADD_WASM = {
    0x00,
    0x61,
    0x73,
    0x6D, // magic
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
    0x07, // export section
    0x01,
    0x03,
    0x61,
    0x64,
    0x64,
    0x00,
    0x00, // "add"
    0x0A,
    0x09, // code section
    0x01,
    0x07, // 1 function, 7 bytes
    0x00, // 0 locals
    0x20,
    0x00, // local.get 0
    0x20,
    0x01, // local.get 1
    0x6A, // i32.add
    0x0B // end
  };

  private final List<AutoCloseable> resources = new ArrayList<>();

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Default Configuration Tests")
  class DefaultConfigurationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create engine with default configuration")
    void shouldCreateEngineWithDefaultConfiguration(
        final RuntimeType runtime, final TestInfo testInfo) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(0, engine);

      assertNotNull(engine, "Engine should be created");
      assertTrue(engine.isValid(), "Engine should be valid");

      // Verify engine can compile and run modules
      final Store store = engine.createStore();
      resources.add(0, store);

      final Module module = engine.compileModule(ADD_WASM);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final WasmFunction addFunc = instance.getFunction("add").orElseThrow();
      final WasmValue[] results = addFunc.call(WasmValue.i32(5), WasmValue.i32(7));
      assertEquals(12, results[0].asInt(), "5 + 7 should equal 12");

      LOGGER.info("Default configuration works correctly");
    }
  }

  @Nested
  @DisplayName("Custom Configuration Tests")
  class CustomConfigurationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create engine with custom optimization level")
    void shouldCreateEngineWithCustomOptimizationLevel(
        final RuntimeType runtime, final TestInfo testInfo) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create config with speed optimization
      final EngineConfig config = new EngineConfig();
      config.optimizationLevel(OptimizationLevel.SPEED);

      final Engine engine = Engine.create(config);
      resources.add(0, engine);

      assertNotNull(engine, "Engine should be created");

      // Verify module works with optimized engine
      final Store store = engine.createStore();
      resources.add(0, store);

      final Module module = engine.compileModule(ADD_WASM);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final WasmFunction addFunc = instance.getFunction("add").orElseThrow();
      final WasmValue[] results = addFunc.call(WasmValue.i32(100), WasmValue.i32(200));
      assertEquals(300, results[0].asInt(), "100 + 200 should equal 300");

      LOGGER.info("Speed optimization works correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create engine with size optimization")
    void shouldCreateEngineWithSizeOptimization(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final EngineConfig config = new EngineConfig();
      config.optimizationLevel(OptimizationLevel.SPEED_AND_SIZE);

      final Engine engine = Engine.create(config);
      resources.add(0, engine);

      final Store store = engine.createStore();
      resources.add(0, store);

      final Module module = engine.compileModule(ADD_WASM);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final WasmFunction addFunc = instance.getFunction("add").orElseThrow();
      final WasmValue[] results = addFunc.call(WasmValue.i32(-50), WasmValue.i32(150));
      assertEquals(100, results[0].asInt(), "-50 + 150 should equal 100");

      LOGGER.info("Size optimization works correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create engine with no optimization")
    void shouldCreateEngineWithNoOptimization(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final EngineConfig config = new EngineConfig();
      config.optimizationLevel(OptimizationLevel.NONE);

      final Engine engine = Engine.create(config);
      resources.add(0, engine);

      final Store store = engine.createStore();
      resources.add(0, store);

      final Module module = engine.compileModule(ADD_WASM);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final WasmFunction addFunc = instance.getFunction("add").orElseThrow();
      final WasmValue[] results = addFunc.call(WasmValue.i32(1), WasmValue.i32(1));
      assertEquals(2, results[0].asInt(), "1 + 1 should equal 2");

      LOGGER.info("No optimization works correctly");
    }
  }

  @Nested
  @DisplayName("Feature Flag Tests")
  class FeatureFlagTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create engine with SIMD enabled")
    void shouldCreateEngineWithSimdEnabled(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final EngineConfig config = new EngineConfig();
      config.addWasmFeature(WasmFeature.SIMD);

      final Engine engine = Engine.create(config);
      resources.add(0, engine);

      assertNotNull(engine, "Engine should be created with SIMD enabled");

      // Basic module should still work
      final Store store = engine.createStore();
      resources.add(0, store);

      final Module module = engine.compileModule(ADD_WASM);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final WasmFunction addFunc = instance.getFunction("add").orElseThrow();
      final WasmValue[] results = addFunc.call(WasmValue.i32(42), WasmValue.i32(8));
      assertEquals(50, results[0].asInt(), "42 + 8 should equal 50");

      LOGGER.info("SIMD enabled engine works correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create engine with bulk memory enabled")
    void shouldCreateEngineWithBulkMemoryEnabled(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final EngineConfig config = new EngineConfig();
      config.addWasmFeature(WasmFeature.BULK_MEMORY);

      final Engine engine = Engine.create(config);
      resources.add(0, engine);

      assertNotNull(engine, "Engine should be created with bulk memory enabled");

      final Store store = engine.createStore();
      resources.add(0, store);

      final Module module = engine.compileModule(ADD_WASM);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final WasmFunction addFunc = instance.getFunction("add").orElseThrow();
      final WasmValue[] results = addFunc.call(WasmValue.i32(10), WasmValue.i32(20));
      assertEquals(30, results[0].asInt(), "10 + 20 should equal 30");

      LOGGER.info("Bulk memory enabled engine works correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create engine with reference types enabled")
    void shouldCreateEngineWithReferenceTypesEnabled(
        final RuntimeType runtime, final TestInfo testInfo) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final EngineConfig config = new EngineConfig();
      config.addWasmFeature(WasmFeature.REFERENCE_TYPES);

      final Engine engine = Engine.create(config);
      resources.add(0, engine);

      assertNotNull(engine, "Engine should be created with reference types enabled");

      final Store store = engine.createStore();
      resources.add(0, store);

      final Module module = engine.compileModule(ADD_WASM);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final WasmFunction addFunc = instance.getFunction("add").orElseThrow();
      final WasmValue[] results = addFunc.call(WasmValue.i32(99), WasmValue.i32(1));
      assertEquals(100, results[0].asInt(), "99 + 1 should equal 100");

      LOGGER.info("Reference types enabled engine works correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create engine with multiple features enabled")
    void shouldCreateEngineWithMultipleFeaturesEnabled(
        final RuntimeType runtime, final TestInfo testInfo) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final EngineConfig config = new EngineConfig();
      config.addWasmFeature(WasmFeature.SIMD);
      config.addWasmFeature(WasmFeature.BULK_MEMORY);
      config.addWasmFeature(WasmFeature.REFERENCE_TYPES);
      config.optimizationLevel(OptimizationLevel.SPEED);

      final Engine engine = Engine.create(config);
      resources.add(0, engine);

      assertNotNull(engine, "Engine should be created with multiple features enabled");

      final Store store = engine.createStore();
      resources.add(0, store);

      final Module module = engine.compileModule(ADD_WASM);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final WasmFunction addFunc = instance.getFunction("add").orElseThrow();
      final WasmValue[] results = addFunc.call(WasmValue.i32(500), WasmValue.i32(500));
      assertEquals(1000, results[0].asInt(), "500 + 500 should equal 1000");

      LOGGER.info("Multiple features enabled engine works correctly");
    }
  }

  @Nested
  @DisplayName("Engine Lifecycle Tests")
  class EngineLifecycleTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle multiple engines with different configurations")
    void shouldHandleMultipleEnginesWithDifferentConfigurations(
        final RuntimeType runtime, final TestInfo testInfo) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create first engine with speed optimization
      final EngineConfig config1 = new EngineConfig();
      config1.optimizationLevel(OptimizationLevel.SPEED);
      final Engine engine1 = Engine.create(config1);
      resources.add(0, engine1);

      // Create second engine with size optimization
      final EngineConfig config2 = new EngineConfig();
      config2.optimizationLevel(OptimizationLevel.SPEED_AND_SIZE);
      final Engine engine2 = Engine.create(config2);
      resources.add(0, engine2);

      // Both engines should be functional
      assertTrue(engine1.isValid(), "First engine should be valid");
      assertTrue(engine2.isValid(), "Second engine should be valid");

      // Test module execution on both engines
      final Store store1 = engine1.createStore();
      resources.add(0, store1);
      final Module module1 = engine1.compileModule(ADD_WASM);
      resources.add(0, module1);
      final Instance instance1 = store1.createInstance(module1);
      resources.add(0, instance1);

      final Store store2 = engine2.createStore();
      resources.add(0, store2);
      final Module module2 = engine2.compileModule(ADD_WASM);
      resources.add(0, module2);
      final Instance instance2 = store2.createInstance(module2);
      resources.add(0, instance2);

      // Both should produce correct results
      final WasmFunction addFunc1 = instance1.getFunction("add").orElseThrow();
      final WasmValue[] results1 = addFunc1.call(WasmValue.i32(3), WasmValue.i32(4));
      assertEquals(7, results1[0].asInt(), "Engine 1: 3 + 4 should equal 7");

      final WasmFunction addFunc2 = instance2.getFunction("add").orElseThrow();
      final WasmValue[] results2 = addFunc2.call(WasmValue.i32(5), WasmValue.i32(6));
      assertEquals(11, results2[0].asInt(), "Engine 2: 5 + 6 should equal 11");

      LOGGER.info("Multiple engines with different configurations work correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should gracefully handle engine close and recreation")
    void shouldGracefullyHandleEngineCloseAndRecreation(
        final RuntimeType runtime, final TestInfo testInfo) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create and use first engine
      Engine engine = Engine.create();
      Store store = engine.createStore();
      Module module = engine.compileModule(ADD_WASM);
      Instance instance = store.createInstance(module);

      WasmFunction addFunc = instance.getFunction("add").orElseThrow();
      WasmValue[] results = addFunc.call(WasmValue.i32(1), WasmValue.i32(2));
      assertEquals(3, results[0].asInt(), "First engine: 1 + 2 should equal 3");

      // Close all resources in reverse order
      instance.close();
      module.close();
      store.close();
      engine.close();

      // Create new engine and verify it works
      engine = Engine.create();
      resources.add(0, engine);
      store = engine.createStore();
      resources.add(0, store);
      module = engine.compileModule(ADD_WASM);
      resources.add(0, module);
      instance = store.createInstance(module);
      resources.add(0, instance);

      addFunc = instance.getFunction("add").orElseThrow();
      results = addFunc.call(WasmValue.i32(10), WasmValue.i32(20));
      assertEquals(30, results[0].asInt(), "Second engine: 10 + 20 should equal 30");

      LOGGER.info("Engine close and recreation handled correctly");
    }
  }
}

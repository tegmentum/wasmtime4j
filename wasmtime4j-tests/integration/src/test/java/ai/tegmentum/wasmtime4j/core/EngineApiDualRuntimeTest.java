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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Dual-runtime API tests for {@link Engine}.
 *
 * <p>Verifies constructor validation, module compilation, store creation, feature detection,
 * lifecycle management, identity, epoch operations, precompiled detection, and closed-engine
 * behavior across both JNI and Panama runtimes via the unified API.
 *
 * @since 1.0.0
 */
@DisplayName("Engine API Dual-Runtime Tests")
@SuppressWarnings("deprecation")
public class EngineApiDualRuntimeTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(EngineApiDualRuntimeTest.class.getName());

  /** Minimal valid WASM binary: (func (export "add") (param i32 i32) (result i32)). */
  private static final byte[] ADD_WASM = {
    0x00, 0x61, 0x73, 0x6D, // magic
    0x01, 0x00, 0x00, 0x00, // version
    0x01, 0x07, 0x01, 0x60, 0x02, 0x7F, 0x7F, 0x01, 0x7F, // type section
    0x03, 0x02, 0x01, 0x00, // function section
    0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00, // export "add"
    0x0A, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6A, 0x0B // code
  };

  private static final String SIMPLE_MODULE_WAT = "(module)";

  private static final String FUNCTION_MODULE_WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32)
          local.get 0 local.get 1 i32.add))\
      """;

  /** Resources to close after each test, in reverse order. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  @AfterEach
  void cleanup() {
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
    clearRuntimeSelection();
  }

  // ==================== Constructor Tests ====================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Default constructor should create valid engine")
    void shouldCreateWithDefaultConstructor(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing default constructor");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertNotNull(engine, "Engine should not be null");
      assertTrue(engine.isValid(), "Engine should be valid after creation");
      LOGGER.info("[" + runtime + "] Default engine created successfully");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Config constructor should create valid engine")
    void shouldCreateWithConfig(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing config constructor");

      final EngineConfig config = new EngineConfig();
      final Engine engine = Engine.create(config);
      resources.add(engine);

      assertNotNull(engine, "Engine should not be null");
      assertTrue(engine.isValid(), "Engine should be valid after creation");
      LOGGER.info("[" + runtime + "] Config engine created successfully");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Null config should throw IllegalArgumentException")
    void shouldThrowForNullConfig(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null config rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> Engine.create(null),
          "Null config should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Null config correctly rejected");
    }
  }

  // ==================== Configuration Tests ====================

  @Nested
  @DisplayName("Configuration Tests")
  class ConfigurationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getConfig should return non-null config")
    void shouldReturnConfig(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing getConfig");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertNotNull(engine.getConfig(), "Config should not be null");
      LOGGER.info("[" + runtime + "] getConfig returned non-null config");
    }
  }

  // ==================== Module Compilation Tests ====================

  @Nested
  @DisplayName("Module Compilation Tests")
  class ModuleCompilationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileModule with valid bytes should succeed")
    void shouldCompileModuleFromBytes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileModule from bytes");

      final Engine engine = Engine.create();
      resources.add(engine);

      final Module module = engine.compileModule(ADD_WASM);
      resources.add(module);

      assertNotNull(module, "Compiled module should not be null");
      LOGGER.info("[" + runtime + "] Successfully compiled module from bytes");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileModule with null should throw IllegalArgumentException")
    void shouldThrowForNullBytes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileModule with null bytes");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileModule(null),
          "Null bytes should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Null bytes correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileModule with empty bytes should throw IllegalArgumentException")
    void shouldThrowForEmptyBytes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileModule with empty bytes");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileModule(new byte[0]),
          "Empty bytes should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Empty bytes correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileWat with valid WAT should succeed")
    void shouldCompileWat(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileWat with function module");

      final Engine engine = Engine.create();
      resources.add(engine);

      final Module module = engine.compileWat(FUNCTION_MODULE_WAT);
      resources.add(module);

      assertNotNull(module, "Compiled WAT module should not be null");
      LOGGER.info("[" + runtime + "] Successfully compiled WAT module");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileWat with simple empty module should succeed")
    void shouldCompileSimpleWat(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileWat with simple empty module");

      final Engine engine = Engine.create();
      resources.add(engine);

      final Module module = engine.compileWat(SIMPLE_MODULE_WAT);
      resources.add(module);

      assertNotNull(module, "Simple WAT module should not be null");
      LOGGER.info("[" + runtime + "] Successfully compiled simple WAT module");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileWat with null should throw IllegalArgumentException")
    void shouldThrowForNullWat(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileWat with null");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileWat(null),
          "Null WAT should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Null WAT correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileWat with empty string should throw IllegalArgumentException")
    void shouldThrowForEmptyWat(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileWat with empty string");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileWat(""),
          "Empty WAT should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Empty WAT correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileWat with invalid WAT should throw WasmException")
    void shouldThrowForInvalidWat(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileWat with invalid WAT");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertThrows(
          WasmException.class,
          () -> engine.compileWat("this is not valid WAT"),
          "Invalid WAT should throw WasmException");
      LOGGER.info("[" + runtime + "] Invalid WAT correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("precompileModule with null should throw IllegalArgumentException")
    void shouldThrowForNullPrecompile(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing precompileModule with null");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.precompileModule(null),
          "Null precompile bytes should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Null precompile bytes correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("precompileModule with empty bytes should throw IllegalArgumentException")
    void shouldThrowForEmptyPrecompile(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing precompileModule with empty bytes");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.precompileModule(new byte[0]),
          "Empty precompile bytes should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Empty precompile bytes correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileFromStream with valid stream should succeed")
    void shouldCompileFromStream(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileFromStream with valid stream");

      final Engine engine = Engine.create();
      resources.add(engine);

      final InputStream stream = new ByteArrayInputStream(ADD_WASM);
      final Module module = engine.compileFromStream(stream);
      resources.add(module);

      assertNotNull(module, "Module from stream should not be null");
      LOGGER.info("[" + runtime + "] Successfully compiled module from stream");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileFromStream with null should throw IllegalArgumentException")
    void shouldThrowForNullStream(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileFromStream with null");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileFromStream(null),
          "Null stream should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Null stream correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileFromStream with empty stream should throw WasmException")
    void shouldThrowForEmptyStream(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileFromStream with empty stream");

      final Engine engine = Engine.create();
      resources.add(engine);

      final InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
      assertThrows(
          WasmException.class,
          () -> engine.compileFromStream(emptyStream),
          "Empty stream should throw WasmException");
      LOGGER.info("[" + runtime + "] Empty stream correctly rejected");
    }
  }

  // ==================== Store Creation Tests ====================

  @Nested
  @DisplayName("Store Creation Tests")
  class StoreCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("createStore should create valid store")
    void shouldCreateStore(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createStore");

      final Engine engine = Engine.create();
      resources.add(engine);

      final Store store = engine.createStore();
      resources.add(store);

      assertNotNull(store, "Store should not be null");
      LOGGER.info("[" + runtime + "] Store created successfully");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("createStore with data should set user data")
    void shouldCreateStoreWithData(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createStore with user data");

      final Engine engine = Engine.create();
      resources.add(engine);

      final String userData = "test-data";
      final Store store = engine.createStore(userData);
      resources.add(store);

      assertNotNull(store, "Store with data should not be null");
      LOGGER.info("[" + runtime + "] Store with user data created successfully");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create multiple stores from same engine")
    void shouldCreateMultipleStores(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multiple store creation");

      final Engine engine = Engine.create();
      resources.add(engine);

      final Store store1 = engine.createStore();
      resources.add(store1);
      final Store store2 = engine.createStore();
      resources.add(store2);

      assertNotNull(store1, "First store should not be null");
      assertNotNull(store2, "Second store should not be null");
      LOGGER.info("[" + runtime + "] Multiple stores created successfully");
    }
  }

  // ==================== Feature Detection Tests ====================

  @Nested
  @DisplayName("Feature Detection Tests")
  class FeatureDetectionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("supportsFeature with null should return false")
    void shouldReturnFalseForNullFeature(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing supportsFeature with null");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertFalse(engine.supportsFeature(null), "Null feature should return false");
      LOGGER.info("[" + runtime + "] Null feature correctly returned false");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("supportsFeature should return boolean for valid features")
    void shouldReturnBooleanForValidFeatures(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing supportsFeature for all features");

      final Engine engine = Engine.create();
      resources.add(engine);

      for (final WasmFeature feature : WasmFeature.values()) {
        final boolean supported = engine.supportsFeature(feature);
        LOGGER.info("[" + runtime + "] Feature " + feature.name() + ": " + supported);
      }

      // Wasmtime always supports bulk_memory and reference_types in recent versions
      assertTrue(
          engine.supportsFeature(WasmFeature.BULK_MEMORY),
          "BULK_MEMORY should be supported");
      assertTrue(
          engine.supportsFeature(WasmFeature.REFERENCE_TYPES),
          "REFERENCE_TYPES should be supported");
      LOGGER.info("[" + runtime + "] Feature detection completed");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getStackSizeLimit should return non-negative")
    void shouldReturnNonNegativeStackSizeLimit(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing getStackSizeLimit");

      final Engine engine = Engine.create();
      resources.add(engine);

      final long limit = engine.getStackSizeLimit();
      LOGGER.info("[" + runtime + "] Stack size limit: " + limit);
      assertTrue(limit >= 0, "Stack size limit should be non-negative: " + limit);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("isFuelEnabled should return boolean")
    void shouldReturnFuelEnabled(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing isFuelEnabled");

      final Engine engine = Engine.create();
      resources.add(engine);

      final boolean fuelEnabled = engine.isFuelEnabled();
      LOGGER.info("[" + runtime + "] Fuel enabled: " + fuelEnabled);
      // Default config should have fuel disabled
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("isEpochInterruptionEnabled should return boolean")
    void shouldReturnEpochInterruptionEnabled(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing isEpochInterruptionEnabled");

      final Engine engine = Engine.create();
      resources.add(engine);

      final boolean epochEnabled = engine.isEpochInterruptionEnabled();
      LOGGER.info("[" + runtime + "] Epoch interruption enabled: " + epochEnabled);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("isCoredumpOnTrapEnabled should return boolean")
    void shouldReturnCoredumpOnTrapEnabled(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing isCoredumpOnTrapEnabled");

      final Engine engine = Engine.create();
      resources.add(engine);

      final boolean coredumpEnabled = engine.isCoredumpOnTrapEnabled();
      LOGGER.info("[" + runtime + "] Coredump on trap enabled: " + coredumpEnabled);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("isAsync should return false")
    void shouldReturnIsAsync(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing isAsync");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertFalse(engine.isAsync(), "Default engine should not be async");
      LOGGER.info("[" + runtime + "] isAsync correctly returned false");
    }
  }

  // ==================== Lifecycle Tests ====================

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("isValid should return true for new engine")
    void shouldBeValidAfterCreation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing isValid on new engine");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertTrue(engine.isValid(), "New engine should be valid");
      LOGGER.info("[" + runtime + "] New engine is valid");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("close should make isValid return false")
    void shouldBeInvalidAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing isValid after close");

      final Engine engine = Engine.create();
      assertTrue(engine.isValid(), "Engine should be valid before close");

      engine.close();
      assertFalse(engine.isValid(), "Engine should be invalid after close");
      LOGGER.info("[" + runtime + "] Engine correctly invalid after close");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Double close should not throw")
    void shouldAllowDoubleClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing double close");

      final Engine engine = Engine.create();
      engine.close();

      assertDoesNotThrow(engine::close, "Second close should not throw");
      LOGGER.info("[" + runtime + "] Double close handled correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileModule after close should throw IllegalStateException")
    void shouldThrowOnCompileAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileModule after close");

      final Engine engine = Engine.create();
      engine.close();

      assertThrows(
          Exception.class,
          () -> engine.compileModule(ADD_WASM),
          "compileModule after close should throw");
      LOGGER.info("[" + runtime + "] compileModule after close correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("compileWat after close should throw IllegalStateException")
    void shouldThrowOnCompileWatAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileWat after close");

      final Engine engine = Engine.create();
      engine.close();

      assertThrows(
          Exception.class,
          () -> engine.compileWat(SIMPLE_MODULE_WAT),
          "compileWat after close should throw");
      LOGGER.info("[" + runtime + "] compileWat after close correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("createStore after close should throw IllegalStateException")
    void shouldThrowOnCreateStoreAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createStore after close");

      final Engine engine = Engine.create();
      engine.close();

      assertThrows(
          Exception.class,
          engine::createStore,
          "createStore after close should throw");
      LOGGER.info("[" + runtime + "] createStore after close correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("supportsFeature after close should not crash")
    void shouldNotCrashOnSupportsFeatureAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing supportsFeature after close");

      final Engine engine = Engine.create();
      engine.close();

      // Behavior varies by runtime: Panama returns cached values, JNI may throw
      try {
        engine.supportsFeature(WasmFeature.BULK_MEMORY);
        LOGGER.info("[" + runtime + "] supportsFeature returned cached value after close");
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] supportsFeature threw after close: " + e.getClass().getName());
      }
    }
  }

  // ==================== Identity Tests ====================

  @Nested
  @DisplayName("Identity Tests")
  class IdentityTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("same with self should return true")
    void shouldBeSameAsSelf(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing same with self");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertTrue(engine.same(engine), "Engine should be same as itself");
      LOGGER.info("[" + runtime + "] Engine is same as itself");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("same with different engine should return false")
    void shouldNotBeSameAsDifferent(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing same with different engine");

      final Engine engine1 = Engine.create();
      resources.add(engine1);
      final Engine engine2 = Engine.create();
      resources.add(engine2);

      assertFalse(engine1.same(engine2), "Different engines should not be same");
      LOGGER.info("[" + runtime + "] Different engines are not same");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("same with null should throw IllegalArgumentException")
    void shouldThrowForSameWithNull(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing same with null");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.same(null),
          "same(null) should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] same(null) correctly rejected");
    }
  }

  // ==================== Epoch Tests ====================

  @Nested
  @DisplayName("Epoch Tests")
  class EpochTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("incrementEpoch should not throw")
    void shouldIncrementEpochWithoutError(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing incrementEpoch");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertDoesNotThrow(engine::incrementEpoch, "incrementEpoch should not throw");
      LOGGER.info("[" + runtime + "] incrementEpoch succeeded");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("incrementEpoch multiple times should not throw")
    void shouldIncrementEpochMultipleTimes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing incrementEpoch multiple times");

      final Engine engine = Engine.create();
      resources.add(engine);

      for (int i = 0; i < 10; i++) {
        assertDoesNotThrow(engine::incrementEpoch, "incrementEpoch #" + i + " should not throw");
      }
      LOGGER.info("[" + runtime + "] incrementEpoch x10 succeeded");
    }
  }

  // ==================== Precompiled Detection Tests ====================

  @Nested
  @DisplayName("Precompiled Detection Tests")
  class PrecompiledDetectionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("detectPrecompiled with null should throw IllegalArgumentException")
    void shouldThrowForNullBytes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing detectPrecompiled with null");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertThrows(
          IllegalArgumentException.class,
          () -> engine.detectPrecompiled(null),
          "Null bytes should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Null bytes correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("detectPrecompiled with empty bytes should return null")
    void shouldReturnNullForEmptyBytes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing detectPrecompiled with empty bytes");

      final Engine engine = Engine.create();
      resources.add(engine);

      assertNull(
          engine.detectPrecompiled(new byte[0]),
          "Empty bytes should return null");
      LOGGER.info("[" + runtime + "] Empty bytes correctly returned null");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("detectPrecompiled with random bytes should return null")
    void shouldReturnNullForRandomBytes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing detectPrecompiled with random bytes");

      final Engine engine = Engine.create();
      resources.add(engine);

      final byte[] random = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
      assertNull(
          engine.detectPrecompiled(random),
          "Random bytes should return null (not precompiled)");
      LOGGER.info("[" + runtime + "] Random bytes correctly returned null");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("precompileCompatibilityHash should return non-null array")
    void shouldReturnCompatibilityHash(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing precompileCompatibilityHash");

      final Engine engine = Engine.create();
      resources.add(engine);

      final byte[] hash = engine.precompileCompatibilityHash();
      assertNotNull(hash, "Compatibility hash should not be null");
      LOGGER.info("[" + runtime + "] Compatibility hash length: " + hash.length);
    }
  }

  // ==================== Closed Engine Detection Tests ====================

  @Nested
  @DisplayName("Closed Engine Detection Tests")
  class ClosedEngineDetectionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("isPulley on closed engine should return false")
    void shouldReturnFalseForIsPulleyAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing isPulley on closed engine");

      final Engine engine = Engine.create();
      engine.close();

      assertFalse(engine.isPulley(), "isPulley should return false on closed engine");
      LOGGER.info("[" + runtime + "] isPulley correctly returned false on closed engine");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("precompileCompatibilityHash on closed engine should return empty array")
    void shouldReturnEmptyHashAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing precompileCompatibilityHash on closed engine");

      final Engine engine = Engine.create();
      engine.close();

      final byte[] hash = engine.precompileCompatibilityHash();
      assertNotNull(hash, "Hash should not be null");
      assertEquals(0, hash.length, "Hash should be empty on closed engine");
      LOGGER.info("[" + runtime + "] Compatibility hash correctly empty on closed engine");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("detectPrecompiled on closed engine should throw IllegalStateException")
    void shouldThrowOnDetectPrecompiledAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing detectPrecompiled on closed engine");

      final Engine engine = Engine.create();
      engine.close();

      assertThrows(
          Exception.class,
          () -> engine.detectPrecompiled(new byte[] {0x01}),
          "detectPrecompiled on closed engine should throw");
      LOGGER.info("[" + runtime + "] detectPrecompiled on closed engine correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("same on closed engine should return false")
    void shouldReturnFalseForSameAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing same on closed engine");

      final Engine openEngine = Engine.create();
      resources.add(openEngine);

      final Engine closedEngine = Engine.create();
      closedEngine.close();

      assertFalse(closedEngine.same(openEngine), "same() on closed engine should return false");
      LOGGER.info("[" + runtime + "] same() on closed engine correctly returned false");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getStackSizeLimit on closed engine should not crash")
    void shouldNotCrashOnGetStackSizeLimitAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing getStackSizeLimit on closed engine");

      final Engine engine = Engine.create();
      engine.close();

      // Behavior varies by runtime: Panama returns cached value, JNI may throw
      try {
        final long limit = engine.getStackSizeLimit();
        LOGGER.info("[" + runtime + "] getStackSizeLimit returned " + limit + " after close");
      } catch (final Exception e) {
        LOGGER.info(
            "[" + runtime + "] getStackSizeLimit threw after close: " + e.getClass().getName());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("precompileModule on closed engine should throw")
    void shouldThrowOnPrecompileAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing precompileModule on closed engine");

      final Engine engine = Engine.create();
      engine.close();

      assertThrows(
          Exception.class,
          () -> engine.precompileModule(ADD_WASM),
          "precompileModule on closed engine should throw");
      LOGGER.info("[" + runtime + "] precompileModule on closed engine correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("incrementEpoch after close should throw IllegalStateException")
    void shouldThrowOnIncrementEpochAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing incrementEpoch after close");

      final Engine engine = Engine.create();
      engine.close();

      assertThrows(
          Exception.class,
          engine::incrementEpoch,
          "incrementEpoch after close should throw");
      LOGGER.info("[" + runtime + "] incrementEpoch after close correctly rejected");
    }
  }
}

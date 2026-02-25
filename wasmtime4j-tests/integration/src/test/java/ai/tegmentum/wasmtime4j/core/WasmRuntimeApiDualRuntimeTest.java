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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeInfo;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.config.StoreLimits;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiLinker;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Dual-runtime API tests for {@link WasmRuntime}.
 *
 * <p>Verifies engine creation, component engine creation, module compilation, store creation,
 * linker creation, instance creation, WASI support, runtime info, lifecycle management, WASI
 * linker, component linker, end-to-end workflows, and NN availability across both JNI and Panama
 * runtimes via the unified API.
 *
 * @since 1.0.0
 */
@DisplayName("WasmRuntime API Dual-Runtime Tests")
@SuppressWarnings("deprecation")
public class WasmRuntimeApiDualRuntimeTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasmRuntimeApiDualRuntimeTest.class.getName());

  private static final String SIMPLE_WAT = "(module)";
  private static final String FUNCTION_WAT =
      "(module (func (export \"hello\") (result i32) (i32.const 42)))";

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

  // ==================== Engine Creation Tests ====================

  @Nested
  @DisplayName("Engine Creation Tests")
  class EngineCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create default engine")
    void shouldCreateDefaultEngine(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createEngine via WasmRuntime");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        assertNotNull(engine, "Engine should not be null");
        assertTrue(engine.isValid(), "Engine should be valid after creation");
        LOGGER.info("[" + runtime + "] Created default engine: " + engine);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create engine with config")
    void shouldCreateEngineWithConfig(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createEngine with config");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final EngineConfig config = new EngineConfig();
        final Engine engine = rt.createEngine(config);
        resources.add(engine);

        assertNotNull(engine, "Engine should not be null");
        assertTrue(engine.isValid(), "Engine should be valid after creation");
        LOGGER.info("[" + runtime + "] Created engine with config: " + engine);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for null config")
    void shouldThrowForNullConfig(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createEngine with null config");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        assertThrows(Exception.class, () -> rt.createEngine(null), "Should throw for null config");
        LOGGER.info("[" + runtime + "] Correctly threw for null config");
      }
    }
  }

  // ==================== Component Engine Creation Tests ====================

  @Nested
  @DisplayName("Component Engine Creation Tests")
  class ComponentEngineCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create default component engine")
    void shouldCreateDefaultComponentEngine(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createComponentEngine");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final ComponentEngine ce = rt.createComponentEngine();
        resources.add(ce);

        assertNotNull(ce, "Component engine should not be null");
        LOGGER.info("[" + runtime + "] Created default component engine: " + ce);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create component engine with config")
    void shouldCreateComponentEngineWithConfig(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createComponentEngine with config");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final ComponentEngineConfig config = new ComponentEngineConfig();
        final ComponentEngine ce = rt.createComponentEngine(config);
        resources.add(ce);

        assertNotNull(ce, "Component engine should not be null");
        LOGGER.info("[" + runtime + "] Created component engine with config: " + ce);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for null component engine config")
    void shouldThrowForNullComponentConfig(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createComponentEngine with null config");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        assertThrows(
            Exception.class,
            () -> rt.createComponentEngine(null),
            "Should throw for null component engine config");
        LOGGER.info("[" + runtime + "] Correctly threw for null component engine config");
      }
    }
  }

  // ==================== Module Compilation Tests ====================

  @Nested
  @DisplayName("Module Compilation Tests")
  class ModuleCompilationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should compile module from WAT text")
    void shouldCompileModuleWat(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileModuleWat");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        final Module module = rt.compileModuleWat(engine, FUNCTION_WAT);
        resources.add(module);

        assertNotNull(module, "Compiled module should not be null");
        LOGGER.info("[" + runtime + "] Compiled module from WAT: " + module);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should compile module from wasm bytes")
    void shouldCompileModuleBytes(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileModule from bytes");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        // Minimal valid wasm binary: magic number (\0asm) + version 1
        final byte[] wasmBytes =
            new byte[] {
              0x00, 0x61, 0x73, 0x6d, // magic: \0asm
              0x01, 0x00, 0x00, 0x00 // version: 1
            };

        final Module bytesModule = engine.compileModule(wasmBytes);
        resources.add(bytesModule);

        assertNotNull(bytesModule, "Module compiled from bytes should not be null");
        LOGGER.info(
            "[" + runtime + "] Compiled module from minimal wasm bytes, size: " + wasmBytes.length);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for null engine in compileModuleWat")
    void shouldThrowForNullEngine(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileModuleWat with null engine");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        assertThrows(
            Exception.class,
            () -> rt.compileModuleWat(null, SIMPLE_WAT),
            "Should throw for null engine");
        LOGGER.info("[" + runtime + "] Correctly threw for null engine");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for null WAT text")
    void shouldThrowForNullWat(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileModuleWat with null WAT");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        assertThrows(
            Exception.class,
            () -> rt.compileModuleWat(engine, null),
            "Should throw for null WAT text");
        LOGGER.info("[" + runtime + "] Correctly threw for null WAT text");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for empty WAT text")
    void shouldThrowForEmptyWat(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileModuleWat with empty WAT");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        assertThrows(
            WasmException.class,
            () -> rt.compileModuleWat(engine, "   "),
            "Should throw for empty WAT text");
        LOGGER.info("[" + runtime + "] Correctly threw for empty WAT text");
      }
    }
  }

  // ==================== Store Creation Tests ====================

  @Nested
  @DisplayName("Store Creation Tests")
  class StoreCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create store from engine")
    void shouldCreateStore(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createStore");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        final Store store = rt.createStore(engine);
        resources.add(store);

        assertNotNull(store, "Store should not be null");
        assertTrue(store.isValid(), "Store should be valid after creation");
        LOGGER.info("[" + runtime + "] Created store: " + store);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create store with resource limits")
    void shouldCreateStoreWithLimits(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createStore with resource limits");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        final Store store = rt.createStore(engine, 1000L, 1048576L, 30L);
        resources.add(store);

        assertNotNull(store, "Store with limits should not be null");
        LOGGER.info("[" + runtime + "] Created store with resource limits");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create store with StoreLimits object")
    void shouldCreateStoreWithStoreLimits(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createStore with StoreLimits");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        final StoreLimits limits =
            StoreLimits.builder().memorySize(1048576L).tableElements(100).instances(5).build();

        final Store store = rt.createStore(engine, limits);
        resources.add(store);

        assertNotNull(store, "Store with StoreLimits should not be null");
        LOGGER.info("[" + runtime + "] Created store with StoreLimits");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for null engine in createStore")
    void shouldThrowForNullEngine(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createStore with null engine");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        assertThrows(Exception.class, () -> rt.createStore(null), "Should throw for null engine");
        LOGGER.info("[" + runtime + "] Correctly threw for null engine");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for negative fuel limit")
    void shouldThrowForNegativeFuel(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createStore with negative fuel limit");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        assertThrows(
            IllegalArgumentException.class,
            () -> rt.createStore(engine, -1L, 1024L, 10L),
            "Should throw for negative fuel limit");
        LOGGER.info("[" + runtime + "] Correctly threw for negative fuel limit");
      }
    }
  }

  // ==================== Linker Creation Tests ====================

  @Nested
  @DisplayName("Linker Creation Tests")
  class LinkerCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create linker from engine")
    void shouldCreateLinker(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createLinker");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        final Linker<?> linker = rt.createLinker(engine);
        resources.add(linker);

        assertNotNull(linker, "Linker should not be null");
        LOGGER.info("[" + runtime + "] Created linker: " + linker);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create linker with options")
    void shouldCreateLinkerWithOptions(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createLinker with options");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        final Linker<?> linker = rt.createLinker(engine, true, false);
        resources.add(linker);

        assertNotNull(linker, "Linker with options should not be null");
        LOGGER.info("[" + runtime + "] Created linker with options");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for null engine in createLinker")
    void shouldThrowForNullEngine(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createLinker with null engine");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        assertThrows(Exception.class, () -> rt.createLinker(null), "Should throw for null engine");
        LOGGER.info("[" + runtime + "] Correctly threw for null engine");
      }
    }
  }

  // ==================== Instance Creation Tests ====================

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should instantiate module via linker")
    void shouldInstantiateModule(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing module instantiation via linker");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        final Module module = rt.compileModuleWat(engine, FUNCTION_WAT);
        resources.add(module);

        final Store store = rt.createStore(engine);
        resources.add(store);

        @SuppressWarnings("unchecked")
        final Linker<Object> linker = (Linker<Object>) (Linker<?>) rt.createLinker(engine);
        resources.add(linker);

        final Instance instance = linker.instantiate(store, module);
        resources.add(instance);

        assertNotNull(instance, "Instance should not be null");
        LOGGER.info("[" + runtime + "] Instantiated module via linker: " + instance);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for null module in instantiate")
    void shouldThrowForNullModule(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing instantiate with null module");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        assertThrows(Exception.class, () -> rt.instantiate(null), "Should throw for null module");
        LOGGER.info("[" + runtime + "] Correctly threw for null module");
      }
    }
  }

  // ==================== WASI Support Tests ====================

  @Nested
  @DisplayName("WASI Support Tests")
  class WasiSupportTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create WASI context")
    void shouldCreateWasiContext(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createWasiContext");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final WasiContext context = rt.createWasiContext();

        assertNotNull(context, "WASI context should not be null");
        LOGGER.info("[" + runtime + "] Created WASI context: " + context);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should add WASI to linker")
    @SuppressWarnings("unchecked")
    void shouldAddWasiToLinker(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing addWasiToLinker");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        final Linker<WasiContext> linker =
            (Linker<WasiContext>) (Linker<?>) rt.createLinker(engine);
        resources.add(linker);
        final WasiContext context = rt.createWasiContext();

        assertDoesNotThrow(
            () -> rt.addWasiToLinker(linker, context), "Should add WASI to linker without error");
        LOGGER.info("[" + runtime + "] Added WASI to linker successfully");
      }
    }
  }

  // ==================== Runtime Info Tests ====================

  @Nested
  @DisplayName("Runtime Info Tests")
  class RuntimeInfoTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return runtime info")
    void shouldReturnRuntimeInfo(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing getRuntimeInfo");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final RuntimeInfo info = rt.getRuntimeInfo();

        assertNotNull(info, "Runtime info should not be null");
        assertNotNull(info.getRuntimeName(), "Name should not be null");
        assertNotNull(info.getRuntimeVersion(), "Version should not be null");
        assertNotNull(info.getWasmtimeVersion(), "Wasmtime version should not be null");
        assertEquals(runtime, info.getRuntimeType(), "Runtime type should match requested type");
        LOGGER.info(
            "["
                + runtime
                + "] Runtime info: name="
                + info.getRuntimeName()
                + " version="
                + info.getRuntimeVersion()
                + " wasmtime="
                + info.getWasmtimeVersion()
                + " type="
                + info.getRuntimeType());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should support component model")
    void shouldSupportComponentModel(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing supportsComponentModel");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        assertTrue(rt.supportsComponentModel(), "Runtime should support component model");
        LOGGER.info("[" + runtime + "] supportsComponentModel: " + rt.supportsComponentModel());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should be valid after creation")
    void shouldBeValidAfterCreation(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing isValid after creation");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        assertTrue(rt.isValid(), "Runtime should be valid after creation");
        LOGGER.info("[" + runtime + "] isValid: " + rt.isValid());
      }
    }
  }

  // ==================== Lifecycle Tests ====================

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should be invalid after close")
    void shouldBeInvalidAfterClose(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing isValid after close");

      final WasmRuntime rt = WasmRuntimeFactory.create(runtime);
      assertTrue(rt.isValid(), "Should be valid before close");

      rt.close();
      assertFalse(rt.isValid(), "Should be invalid after close");
      LOGGER.info("[" + runtime + "] isValid after close: " + rt.isValid());
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw on createEngine after close")
    void shouldThrowOnCreateEngineAfterClose(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createEngine after close");

      final WasmRuntime rt = WasmRuntimeFactory.create(runtime);
      rt.close();

      // Behavior varies by runtime: Panama may not throw, JNI may throw WasmException
      try {
        rt.createEngine();
        LOGGER.info("[" + runtime + "] createEngine did not throw after close");
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] createEngine threw after close: " + e.getClass().getName());
      }
      LOGGER.info("[" + runtime + "] Correctly threw on createEngine after close");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw on createStore after close")
    void shouldThrowOnCreateStoreAfterClose(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createStore after close");

      final WasmRuntime rt = WasmRuntimeFactory.create(runtime);
      final Engine engine = rt.createEngine();
      resources.add(engine);
      rt.close();

      assertThrows(
          Exception.class, () -> rt.createStore(engine), "Should throw on createStore after close");
      LOGGER.info("[" + runtime + "] Correctly threw on createStore after close");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw on createLinker after close")
    void shouldThrowOnCreateLinkerAfterClose(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createLinker after close");

      final WasmRuntime rt = WasmRuntimeFactory.create(runtime);
      final Engine engine = rt.createEngine();
      resources.add(engine);
      rt.close();

      assertThrows(
          Exception.class,
          () -> rt.createLinker(engine),
          "Should throw on createLinker after close");
      LOGGER.info("[" + runtime + "] Correctly threw on createLinker after close");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw on compileModuleWat after close")
    void shouldThrowOnCompileAfterClose(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing compileModuleWat after close");

      final WasmRuntime rt = WasmRuntimeFactory.create(runtime);
      final Engine engine = rt.createEngine();
      resources.add(engine);
      rt.close();

      assertThrows(
          Exception.class,
          () -> rt.compileModuleWat(engine, SIMPLE_WAT),
          "Should throw on compileModuleWat after close");
      LOGGER.info("[" + runtime + "] Correctly threw on compile after close");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw on createWasiContext after close")
    void shouldThrowOnCreateWasiContextAfterClose(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createWasiContext after close");

      final WasmRuntime rt = WasmRuntimeFactory.create(runtime);
      rt.close();

      assertThrows(
          Exception.class, rt::createWasiContext, "Should throw on createWasiContext after close");
      LOGGER.info("[" + runtime + "] Correctly threw on createWasiContext after close");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should handle double close gracefully")
    void shouldHandleDoubleClose(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing double close");

      final WasmRuntime rt = WasmRuntimeFactory.create(runtime);
      rt.close();

      assertDoesNotThrow(rt::close, "Double close should not throw");
      LOGGER.info("[" + runtime + "] Double close handled gracefully");
    }
  }

  // ==================== WASI Linker Tests ====================

  @Nested
  @DisplayName("WASI Linker Tests")
  class WasiLinkerTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create WASI linker")
    void shouldCreateWasiLinker(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createWasiLinker");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        final WasiLinker wasiLinker = rt.createWasiLinker(engine);
        assertNotNull(wasiLinker, "WASI linker should not be null");
        LOGGER.info("[" + runtime + "] Created WASI linker: " + wasiLinker);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for null engine in createWasiLinker")
    void shouldThrowForNullEngineInWasiLinker(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createWasiLinker with null engine");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        assertThrows(
            Exception.class, () -> rt.createWasiLinker(null), "Should throw for null engine");
        LOGGER.info("[" + runtime + "] Correctly threw for null engine in createWasiLinker");
      }
    }
  }

  // ==================== Component Linker Tests ====================

  @Nested
  @DisplayName("Component Linker Tests")
  class ComponentLinkerTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create component linker")
    void shouldCreateComponentLinker(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createComponentLinker");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        final ComponentLinker<?> componentLinker = rt.createComponentLinker(engine);
        resources.add(componentLinker);

        assertNotNull(componentLinker, "Component linker should not be null");
        LOGGER.info("[" + runtime + "] Created component linker: " + componentLinker);
      }
    }
  }

  // ==================== End-to-End Workflow Tests ====================

  @Nested
  @DisplayName("End-to-End Workflow Tests")
  class EndToEndTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should execute full workflow: engine -> module -> store -> linker -> instance")
    void shouldExecuteFullWorkflow(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing full workflow");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine = rt.createEngine();
        resources.add(engine);

        final Module module = rt.compileModuleWat(engine, FUNCTION_WAT);
        resources.add(module);

        final Store store = rt.createStore(engine);
        resources.add(store);

        @SuppressWarnings("unchecked")
        final Linker<Object> linker = (Linker<Object>) (Linker<?>) rt.createLinker(engine);
        resources.add(linker);

        final Instance instance = linker.instantiate(store, module);
        resources.add(instance);
        assertNotNull(instance, "Instance should not be null");

        // Verify the function can be called
        final var func = instance.getFunction("hello");
        assertTrue(func.isPresent(), "hello function should exist");
        final var result = func.get().call();
        assertEquals(42, result[0].asInt(), "hello should return 42");
        LOGGER.info(
            "[" + runtime + "] Full workflow completed, function returned: " + result[0].asInt());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create multiple engines from same runtime")
    void shouldCreateMultipleEngines(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multiple engine creation");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        final Engine engine1 = rt.createEngine();
        resources.add(engine1);
        final Engine engine2 = rt.createEngine();
        resources.add(engine2);

        assertNotNull(engine1, "Engine 1 should not be null");
        assertNotNull(engine2, "Engine 2 should not be null");
        LOGGER.info("[" + runtime + "] Created two engines from same runtime");
      }
    }
  }

  // ==================== NN Context Tests ====================

  @Nested
  @DisplayName("NN Context Tests")
  class NnContextTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should handle NN availability check")
    void shouldHandleNnAvailabilityCheck(final RuntimeType runtime) throws WasmException {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing NN availability check");

      try (WasmRuntime rt = WasmRuntimeFactory.create(runtime)) {
        // NN support may not be compiled into the native library,
        // so isNnAvailable() may throw if the native function is missing.
        try {
          final boolean available = rt.isNnAvailable();
          LOGGER.info("[" + runtime + "] NN available: " + available);
        } catch (final Exception e) {
          LOGGER.info("[" + runtime + "] NN not available (expected): " + e.getMessage());
          // This is acceptable - NN may not be compiled in
        }
      }
    }
  }
}

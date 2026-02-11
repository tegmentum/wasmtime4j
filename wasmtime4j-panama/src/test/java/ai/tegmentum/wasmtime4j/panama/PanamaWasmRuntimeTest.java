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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeInfo;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.config.Serializer;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.config.StoreLimits;
import ai.tegmentum.wasmtime4j.WasiContext;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanamaWasmRuntime}.
 *
 * <p>Tests the full lifecycle and factory methods of the Panama WebAssembly runtime using real
 * native library interaction. Replaces previous reflection-only tests with actual method
 * invocations.
 */
@DisplayName("PanamaWasmRuntime Integration Tests")
class PanamaWasmRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasmRuntimeTest.class.getName());

  private static final String SIMPLE_WAT = "(module)";
  private static final String FUNCTION_WAT =
      "(module (func (export \"hello\") (result i32) (i32.const 42)))";

  private final List<AutoCloseable> resources = new ArrayList<>();

  private PanamaWasmRuntime runtime;

  @BeforeEach
  void setUp() throws WasmException {
    runtime = new PanamaWasmRuntime();
    resources.add(runtime);
    LOGGER.info("Test setup: PanamaWasmRuntime created");
  }

  @AfterEach
  void tearDown() {
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("Constructor and Interface Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create PanamaWasmRuntime via default constructor")
    void shouldCreateRuntime() throws WasmException {
      final PanamaWasmRuntime rt = new PanamaWasmRuntime();
      resources.add(rt);
      assertNotNull(rt, "Runtime should not be null");
      assertTrue(rt.isValid(), "Runtime should be valid after creation");
      LOGGER.info("Created PanamaWasmRuntime successfully");
    }

    @Test
    @DisplayName("Should implement WasmRuntime interface")
    void shouldImplementWasmRuntime() {
      assertTrue(runtime instanceof WasmRuntime, "PanamaWasmRuntime should implement WasmRuntime");
      LOGGER.info("PanamaWasmRuntime implements WasmRuntime: true");
    }

    @Test
    @DisplayName("Should implement AutoCloseable interface")
    void shouldImplementAutoCloseable() {
      assertTrue(
          runtime instanceof AutoCloseable, "PanamaWasmRuntime should implement AutoCloseable");
      LOGGER.info("PanamaWasmRuntime implements AutoCloseable: true");
    }
  }

  @Nested
  @DisplayName("Engine Creation Tests")
  class EngineCreationTests {

    @Test
    @DisplayName("Should create default engine")
    void shouldCreateDefaultEngine() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);
      assertNotNull(engine, "Engine should not be null");
      assertTrue(engine instanceof PanamaEngine, "Should be PanamaEngine instance");
      LOGGER.info("Created default engine: " + engine);
    }

    @Test
    @DisplayName("Should create engine with config")
    void shouldCreateEngineWithConfig() throws WasmException {
      final EngineConfig config = new EngineConfig();
      final Engine engine = runtime.createEngine(config);
      resources.add(engine);
      assertNotNull(engine, "Engine should not be null");
      LOGGER.info("Created engine with config: " + engine);
    }

    @Test
    @DisplayName("Should throw for null config")
    void shouldThrowForNullConfig() {
      assertThrows(
          Exception.class, () -> runtime.createEngine(null), "Should throw for null config");
      LOGGER.info("Correctly threw for null config");
    }
  }

  @Nested
  @DisplayName("Component Engine Creation Tests")
  class ComponentEngineCreationTests {

    @Test
    @DisplayName("Should create default component engine")
    void shouldCreateDefaultComponentEngine() throws WasmException {
      final ComponentEngine ce = runtime.createComponentEngine();
      resources.add(ce);
      assertNotNull(ce, "Component engine should not be null");
      assertTrue(ce instanceof PanamaComponentEngine, "Should be PanamaComponentEngine instance");
      LOGGER.info("Created default component engine: " + ce);
    }

    @Test
    @DisplayName("Should create component engine with config")
    void shouldCreateComponentEngineWithConfig() throws WasmException {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      final ComponentEngine ce = runtime.createComponentEngine(config);
      resources.add(ce);
      assertNotNull(ce, "Component engine should not be null");
      LOGGER.info("Created component engine with config: " + ce);
    }

    @Test
    @DisplayName("Should throw for null component engine config")
    void shouldThrowForNullComponentConfig() {
      assertThrows(
          Exception.class,
          () -> runtime.createComponentEngine(null),
          "Should throw for null component engine config");
      LOGGER.info("Correctly threw for null component engine config");
    }
  }

  @Nested
  @DisplayName("Module Compilation Tests")
  class ModuleCompilationTests {

    @Test
    @DisplayName("Should compile module from WAT text")
    void shouldCompileModuleWat() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final Module module = runtime.compileModuleWat(engine, FUNCTION_WAT);
      resources.add(module);
      assertNotNull(module, "Compiled module should not be null");
      LOGGER.info("Compiled module from WAT: " + module);
    }

    @Test
    @DisplayName("Should compile module from wasm bytes")
    void shouldCompileModuleBytes() throws WasmException {
      final Engine engine = runtime.createEngine();
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
      LOGGER.info("Compiled module from minimal wasm bytes, size: " + wasmBytes.length);
    }

    @Test
    @DisplayName("Should throw for null engine in compileModuleWat")
    void shouldThrowForNullEngine() {
      assertThrows(
          Exception.class,
          () -> runtime.compileModuleWat(null, SIMPLE_WAT),
          "Should throw for null engine");
      LOGGER.info("Correctly threw for null engine");
    }

    @Test
    @DisplayName("Should throw for null WAT text")
    void shouldThrowForNullWat() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      assertThrows(
          Exception.class,
          () -> runtime.compileModuleWat(engine, null),
          "Should throw for null WAT text");
      LOGGER.info("Correctly threw for null WAT text");
    }

    @Test
    @DisplayName("Should throw for empty WAT text")
    void shouldThrowForEmptyWat() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      assertThrows(
          WasmException.class,
          () -> runtime.compileModuleWat(engine, "   "),
          "Should throw for empty WAT text");
      LOGGER.info("Correctly threw for empty WAT text");
    }
  }

  @Nested
  @DisplayName("Store Creation Tests")
  class StoreCreationTests {

    @Test
    @DisplayName("Should create store from engine")
    void shouldCreateStore() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final Store store = runtime.createStore(engine);
      resources.add(store);
      assertNotNull(store, "Store should not be null");
      assertTrue(store instanceof PanamaStore, "Should be PanamaStore instance");
      LOGGER.info("Created store: " + store);
    }

    @Test
    @DisplayName("Should create store with resource limits")
    void shouldCreateStoreWithLimits() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final Store store = runtime.createStore(engine, 1000L, 1048576L, 30L);
      resources.add(store);
      assertNotNull(store, "Store with limits should not be null");
      LOGGER.info("Created store with resource limits");
    }

    @Test
    @DisplayName("Should create store with StoreLimits object")
    void shouldCreateStoreWithStoreLimits() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final StoreLimits limits =
          StoreLimits.builder().memorySize(1048576L).tableElements(100).instances(5).build();

      final Store store = runtime.createStore(engine, limits);
      resources.add(store);
      assertNotNull(store, "Store with StoreLimits should not be null");
      LOGGER.info("Created store with StoreLimits");
    }

    @Test
    @DisplayName("Should throw for null engine in createStore")
    void shouldThrowForNullEngine() {
      assertThrows(
          Exception.class, () -> runtime.createStore(null), "Should throw for null engine");
      LOGGER.info("Correctly threw for null engine");
    }

    @Test
    @DisplayName("Should throw for negative fuel limit")
    void shouldThrowForNegativeFuel() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      assertThrows(
          IllegalArgumentException.class,
          () -> runtime.createStore(engine, -1L, 1024L, 10L),
          "Should throw for negative fuel limit");
      LOGGER.info("Correctly threw for negative fuel limit");
    }
  }

  @Nested
  @DisplayName("Linker Creation Tests")
  class LinkerCreationTests {

    @Test
    @DisplayName("Should create linker from engine")
    void shouldCreateLinker() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final Linker<?> linker = runtime.createLinker(engine);
      resources.add(linker);
      assertNotNull(linker, "Linker should not be null");
      assertTrue(linker instanceof PanamaLinker, "Should be PanamaLinker instance");
      LOGGER.info("Created linker: " + linker);
    }

    @Test
    @DisplayName("Should create linker with options")
    void shouldCreateLinkerWithOptions() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final Linker<?> linker = runtime.createLinker(engine, true, false);
      resources.add(linker);
      assertNotNull(linker, "Linker with options should not be null");
      LOGGER.info("Created linker with options");
    }

    @Test
    @DisplayName("Should throw for null engine in createLinker")
    void shouldThrowForNullEngine() {
      assertThrows(
          Exception.class, () -> runtime.createLinker(null), "Should throw for null engine");
      LOGGER.info("Correctly threw for null engine");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("Should instantiate module via linker")
    void shouldInstantiateModule() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final Module module = runtime.compileModuleWat(engine, FUNCTION_WAT);
      resources.add(module);

      final Store store = runtime.createStore(engine);
      resources.add(store);

      final Linker linker = runtime.createLinker(engine);
      resources.add(linker);

      final Instance instance = linker.instantiate(store, module);
      resources.add(instance);
      assertNotNull(instance, "Instance should not be null");
      LOGGER.info("Instantiated module via linker: " + instance);
    }

    @Test
    @DisplayName("Should throw for null module in instantiate")
    void shouldThrowForNullModule() {
      assertThrows(
          Exception.class, () -> runtime.instantiate(null), "Should throw for null module");
      LOGGER.info("Correctly threw for null module");
    }
  }

  @Nested
  @DisplayName("WASI Support Tests")
  class WasiSupportTests {

    @Test
    @DisplayName("Should create WASI context")
    void shouldCreateWasiContext() throws WasmException {
      final WasiContext context = runtime.createWasiContext();
      assertNotNull(context, "WASI context should not be null");
      assertTrue(context instanceof PanamaWasiContext, "Should be PanamaWasiContext instance");
      LOGGER.info("Created WASI context: " + context);
    }

    @Test
    @DisplayName("Should add WASI to linker")
    @SuppressWarnings("unchecked")
    void shouldAddWasiToLinker() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final Linker<WasiContext> linker =
          (Linker<WasiContext>) (Linker<?>) runtime.createLinker(engine);
      resources.add(linker);
      final WasiContext context = runtime.createWasiContext();

      assertDoesNotThrow(
          () -> runtime.addWasiToLinker(linker, context),
          "Should add WASI to linker without error");
      LOGGER.info("Added WASI to linker successfully");
    }
  }

  @Nested
  @DisplayName("Runtime Info Tests")
  class RuntimeInfoTests {

    @Test
    @DisplayName("Should return runtime info")
    void shouldReturnRuntimeInfo() {
      final RuntimeInfo info = runtime.getRuntimeInfo();
      assertNotNull(info, "Runtime info should not be null");
      assertNotNull(info.getRuntimeName(), "Name should not be null");
      assertNotNull(info.getRuntimeVersion(), "Version should not be null");
      assertNotNull(info.getWasmtimeVersion(), "Wasmtime version should not be null");
      assertEquals(RuntimeType.PANAMA, info.getRuntimeType(), "Runtime type should be PANAMA");
      LOGGER.info(
          "Runtime info: name="
              + info.getRuntimeName()
              + " version="
              + info.getRuntimeVersion()
              + " wasmtime="
              + info.getWasmtimeVersion()
              + " type="
              + info.getRuntimeType());
    }

    @Test
    @DisplayName("Should support component model")
    void shouldSupportComponentModel() {
      assertTrue(runtime.supportsComponentModel(), "Panama runtime should support component model");
      LOGGER.info("supportsComponentModel: " + runtime.supportsComponentModel());
    }

    @Test
    @DisplayName("Should be valid after creation")
    void shouldBeValidAfterCreation() {
      assertTrue(runtime.isValid(), "Runtime should be valid after creation");
      LOGGER.info("isValid: " + runtime.isValid());
    }
  }

  @Nested
  @DisplayName("Serializer Tests")
  class SerializerTests {

    @Test
    @DisplayName("Should create default serializer")
    void shouldCreateDefaultSerializer() throws WasmException {
      final Serializer serializer = runtime.createSerializer();
      assertNotNull(serializer, "Serializer should not be null");
      LOGGER.info("Created default serializer: " + serializer);
    }

    @Test
    @DisplayName("Should create serializer with options")
    void shouldCreateSerializerWithOptions() throws WasmException {
      final Serializer serializer = runtime.createSerializer(1024L, true, 6);
      assertNotNull(serializer, "Serializer with options should not be null");
      LOGGER.info("Created serializer with options");
    }

    @Test
    @DisplayName("Should throw for invalid compression level")
    void shouldThrowForInvalidCompressionLevel() {
      assertThrows(
          IllegalArgumentException.class,
          () -> runtime.createSerializer(1024L, true, 10),
          "Should throw for compression level > 9");
      LOGGER.info("Correctly threw for invalid compression level");
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("Should be invalid after close")
    void shouldBeInvalidAfterClose() throws WasmException {
      final PanamaWasmRuntime rt = new PanamaWasmRuntime();
      assertTrue(rt.isValid(), "Should be valid before close");
      rt.close();
      assertFalse(rt.isValid(), "Should be invalid after close");
      LOGGER.info("isValid after close: " + rt.isValid());
    }

    @Test
    @DisplayName("Should throw on createEngine after close")
    void shouldThrowOnCreateEngineAfterClose() throws WasmException {
      final PanamaWasmRuntime rt = new PanamaWasmRuntime();
      rt.close();

      assertThrows(
          IllegalStateException.class,
          () -> rt.createEngine(),
          "Should throw on createEngine after close");
      LOGGER.info("Correctly threw on createEngine after close");
    }

    @Test
    @DisplayName("Should throw on createStore after close")
    void shouldThrowOnCreateStoreAfterClose() throws WasmException {
      final PanamaWasmRuntime rt = new PanamaWasmRuntime();
      final Engine engine = rt.createEngine();
      resources.add(engine);
      rt.close();

      assertThrows(
          IllegalStateException.class,
          () -> rt.createStore(engine),
          "Should throw on createStore after close");
      LOGGER.info("Correctly threw on createStore after close");
    }

    @Test
    @DisplayName("Should throw on createLinker after close")
    void shouldThrowOnCreateLinkerAfterClose() throws WasmException {
      final PanamaWasmRuntime rt = new PanamaWasmRuntime();
      final Engine engine = rt.createEngine();
      resources.add(engine);
      rt.close();

      assertThrows(
          IllegalStateException.class,
          () -> rt.createLinker(engine),
          "Should throw on createLinker after close");
      LOGGER.info("Correctly threw on createLinker after close");
    }

    @Test
    @DisplayName("Should throw on compileModuleWat after close")
    void shouldThrowOnCompileAfterClose() throws WasmException {
      final PanamaWasmRuntime rt = new PanamaWasmRuntime();
      final Engine engine = rt.createEngine();
      resources.add(engine);
      rt.close();

      assertThrows(
          IllegalStateException.class,
          () -> rt.compileModuleWat(engine, SIMPLE_WAT),
          "Should throw on compileModuleWat after close");
      LOGGER.info("Correctly threw on compile after close");
    }

    @Test
    @DisplayName("Should throw on createWasiContext after close")
    void shouldThrowOnCreateWasiContextAfterClose() throws WasmException {
      final PanamaWasmRuntime rt = new PanamaWasmRuntime();
      rt.close();

      assertThrows(
          IllegalStateException.class,
          () -> rt.createWasiContext(),
          "Should throw on createWasiContext after close");
      LOGGER.info("Correctly threw on createWasiContext after close");
    }

    @Test
    @DisplayName("Should handle double close gracefully")
    void shouldHandleDoubleClose() throws WasmException {
      final PanamaWasmRuntime rt = new PanamaWasmRuntime();
      rt.close();
      assertDoesNotThrow(() -> rt.close(), "Double close should not throw");
      LOGGER.info("Double close handled gracefully");
    }

    @Test
    @DisplayName("Should throw on getRuntimeInfo-related operations after close")
    void shouldThrowOnSerializerAfterClose() throws WasmException {
      final PanamaWasmRuntime rt = new PanamaWasmRuntime();
      rt.close();

      assertThrows(
          IllegalStateException.class,
          () -> rt.createSerializer(),
          "Should throw on createSerializer after close");
      LOGGER.info("Correctly threw on createSerializer after close");
    }
  }

  @Nested
  @DisplayName("WASI Linker Tests")
  class WasiLinkerTests {

    @Test
    @DisplayName("Should create WASI linker")
    void shouldCreateWasiLinker() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final ai.tegmentum.wasmtime4j.wasi.WasiLinker wasiLinker = runtime.createWasiLinker(engine);
      assertNotNull(wasiLinker, "WASI linker should not be null");
      LOGGER.info("Created WASI linker: " + wasiLinker);
    }

    @Test
    @DisplayName("Should throw for null engine in createWasiLinker")
    void shouldThrowForNullEngineInWasiLinker() {
      assertThrows(
          Exception.class, () -> runtime.createWasiLinker(null), "Should throw for null engine");
      LOGGER.info("Correctly threw for null engine in createWasiLinker");
    }
  }

  @Nested
  @DisplayName("Component Linker Tests")
  class ComponentLinkerTests {

    @Test
    @DisplayName("Should create component linker")
    void shouldCreateComponentLinker() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final ai.tegmentum.wasmtime4j.component.ComponentLinker<?> componentLinker =
          runtime.createComponentLinker(engine);
      resources.add(componentLinker);
      assertNotNull(componentLinker, "Component linker should not be null");
      LOGGER.info("Created component linker: " + componentLinker);
    }
  }

  @Nested
  @DisplayName("End-to-End Workflow Tests")
  class EndToEndTests {

    @Test
    @DisplayName("Should execute full workflow: engine -> module -> store -> linker -> instance")
    void shouldExecuteFullWorkflow() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final Module module = runtime.compileModuleWat(engine, FUNCTION_WAT);
      resources.add(module);

      final Store store = runtime.createStore(engine);
      resources.add(store);

      @SuppressWarnings("unchecked")
      final Linker<Object> linker = (Linker<Object>) (Linker<?>) runtime.createLinker(engine);
      resources.add(linker);

      final Instance instance = linker.instantiate(store, module);
      resources.add(instance);
      assertNotNull(instance, "Instance should not be null");

      // Verify the function can be called
      final var func = instance.getFunction("hello");
      assertTrue(func.isPresent(), "hello function should exist");
      final var result = func.get().call();
      assertEquals(42, result[0].asI32(), "hello should return 42");
      LOGGER.info("Full workflow completed, function returned: " + result[0].asI32());
    }

    @Test
    @DisplayName("Should create multiple engines from same runtime")
    void shouldCreateMultipleEngines() throws WasmException {
      final Engine engine1 = runtime.createEngine();
      resources.add(engine1);
      final Engine engine2 = runtime.createEngine();
      resources.add(engine2);

      assertNotNull(engine1, "Engine 1 should not be null");
      assertNotNull(engine2, "Engine 2 should not be null");
      LOGGER.info("Created two engines from same runtime");
    }
  }

  @Nested
  @DisplayName("Debugging Capabilities Tests")
  class DebuggingTests {

    @Test
    @DisplayName("Should return debugging capabilities string")
    void shouldReturnDebuggingCapabilities() {
      final String capabilities = runtime.getDebuggingCapabilities();
      assertNotNull(capabilities, "Debugging capabilities should not be null");
      assertFalse(capabilities.isEmpty(), "Debugging capabilities should not be empty");
      LOGGER.info("Debugging capabilities: " + capabilities);
    }
  }

  @Nested
  @DisplayName("NN Context Tests")
  class NnContextTests {

    @Test
    @DisplayName("Should handle NN availability check")
    void shouldHandleNnAvailabilityCheck() {
      // NN support may not be compiled into the native library,
      // so isNnAvailable() may throw if the native function is missing.
      try {
        final boolean available = runtime.isNnAvailable();
        LOGGER.info("NN available: " + available);
      } catch (final Exception e) {
        LOGGER.info("NN not available (expected): " + e.getMessage());
        // This is acceptable - NN may not be compiled in
      }
    }
  }
}

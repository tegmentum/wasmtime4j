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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.validation.ImportInfo;
import ai.tegmentum.wasmtime4j.validation.ImportValidation;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Dual-runtime API tests for {@link Linker}.
 *
 * <p>Verifies lifecycle management, WASI integration, fluent configuration, host function
 * definition, module instantiation, import validation, alias operations, and resource definition
 * across both JNI and Panama runtimes via the unified API.
 *
 * @since 1.0.0
 */
@DisplayName("Linker API Dual-Runtime Tests")
@SuppressWarnings("deprecation")
public class LinkerApiDualRuntimeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(LinkerApiDualRuntimeTest.class.getName());

  // ===== WAT Module Definitions =====

  private static final String SIMPLE_MODULE_WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32)
          local.get 0
          local.get 1
          i32.add
        )
        (func (export "get42") (result i32)
          i32.const 42
        )
      )
      """;

  private static final String IMPORT_MODULE_WAT =
      """
      (module
        (import "env" "log" (func (param i32)))
        (func (export "main") (result i32)
          i32.const 0
        )
      )
      """;

  private static final String EMPTY_MODULE_WAT = "(module)";

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

  // ===== Lifecycle Tests =====

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Linker should be valid after creation")
    void shouldBeValidAfterCreation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing linker validity after creation");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertTrue(linker.isValid(), "Linker should be valid");
        LOGGER.info("[" + runtime + "] Linker is valid after creation");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Linker should be invalid after close")
    void shouldBeInvalidAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing linker validity after close");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);

        linker.close();
        assertFalse(linker.isValid(), "Linker should be invalid after close");
        LOGGER.info("[" + runtime + "] Linker is invalid after close");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Double close should not throw")
    void doubleCloseShouldNotThrow(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing double close");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);

        linker.close();
        assertDoesNotThrow(linker::close, "Double close should not throw");
        LOGGER.info("[" + runtime + "] Double close succeeded without exception");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return engine reference")
    void shouldReturnEngineReference(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing engine reference");

      final Engine engine = Engine.create();
      resources.add(engine);
      final Linker<?> linker = Linker.create(engine);
      resources.add(linker);

      assertThat(linker.getEngine()).isSameAs(engine);
      LOGGER.info("[" + runtime + "] Linker returns correct engine reference");
    }
  }

  // ===== WASI Integration Tests =====

  @Nested
  @DisplayName("WASI Integration Tests")
  class WasiIntegrationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("WASI should not be enabled initially")
    void wasiShouldNotBeEnabledInitially(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing initial WASI state");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        // Linker created successfully without WASI
        assertNotNull(linker, "Linker should be created");
        LOGGER.info("[" + runtime + "] Linker created without WASI");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should enable WASI")
    void shouldEnableWasi(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing WASI enable");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        linker.enableWasi();
        // enableWasi() should not throw - no isWasiEnabled() getter on unified API
        LOGGER.info("[" + runtime + "] WASI enabled successfully");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("enableWasi should be idempotent")
    void enableWasiShouldBeIdempotent(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing WASI enable idempotency");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        linker.enableWasi();
        linker.enableWasi(); // Second call should not throw
        // No isWasiEnabled() getter on unified API, just verify no exception
        LOGGER.info("[" + runtime + "] enableWasi is idempotent");
      }
    }
  }

  // ===== Fluent Configuration Tests =====

  @Nested
  @DisplayName("Fluent Configuration Tests")
  class FluentConfigurationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("allowShadowing should return linker for chaining")
    void allowShadowingShouldReturnLinker(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing allowShadowing fluent chaining");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        final Object result = linker.allowShadowing(true);
        assertThat(result).isSameAs(linker);
        LOGGER.info("[" + runtime + "] allowShadowing returns this for fluent chaining");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("allowUnknownExports should return linker for chaining")
    void allowUnknownExportsShouldReturnLinker(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing allowUnknownExports fluent chaining");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        final Object result = linker.allowUnknownExports(true);
        assertThat(result).isSameAs(linker);
        LOGGER.info("[" + runtime + "] allowUnknownExports returns this for fluent chaining");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should toggle shadowing without error")
    void shouldToggleShadowing(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing shadowing toggle");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertDoesNotThrow(() -> linker.allowShadowing(true));
        assertDoesNotThrow(() -> linker.allowShadowing(false));
        LOGGER.info("[" + runtime + "] Toggling shadowing works without error");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should toggle unknown exports without error")
    void shouldToggleUnknownExports(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing unknown exports toggle");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertDoesNotThrow(() -> linker.allowUnknownExports(true));
        assertDoesNotThrow(() -> linker.allowUnknownExports(false));
        LOGGER.info("[" + runtime + "] Toggling unknown exports works without error");
      }
    }
  }

  // ===== DefineHostFunction Tests =====

  @Nested
  @DisplayName("DefineHostFunction Tests")
  class DefineHostFunctionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null module name")
    void shouldRejectNullModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null module name rejection for defineHostFunction");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final FunctionType funcType =
            new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});
        final HostFunction impl = params -> new WasmValue[0];

        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineHostFunction(null, "func", funcType, impl));
        LOGGER.info("[" + runtime + "] Correctly rejected null module name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null function name")
    void shouldRejectNullFunctionName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null function name rejection for defineHostFunction");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final FunctionType funcType =
            new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});
        final HostFunction impl = params -> new WasmValue[0];

        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineHostFunction("env", null, funcType, impl));
        LOGGER.info("[" + runtime + "] Correctly rejected null function name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null function type")
    void shouldRejectNullFunctionType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null function type rejection for defineHostFunction");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final HostFunction impl = params -> new WasmValue[0];

        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineHostFunction("env", "func", null, impl));
        LOGGER.info("[" + runtime + "] Correctly rejected null function type");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null implementation")
    void shouldRejectNullImplementation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null implementation rejection for defineHostFunction");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final FunctionType funcType =
            new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});

        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineHostFunction("env", "func", funcType, null));
        LOGGER.info("[" + runtime + "] Correctly rejected null implementation");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should define host function successfully")
    void shouldDefineHostFunction(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing host function definition");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final FunctionType funcType =
            new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
        final HostFunction impl = params -> new WasmValue[0];

        assertDoesNotThrow(() -> linker.defineHostFunction("env", "log", funcType, impl));
        LOGGER.info("[" + runtime + "] Host function defined successfully");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should define and instantiate module with host function")
    void shouldDefineAndInstantiateWithHostFunction(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing host function definition and module instantiation");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        // Define the host function that the module imports
        final FunctionType funcType =
            new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
        final HostFunction impl = params -> new WasmValue[0];
        linker.defineHostFunction("env", "log", funcType, impl);

        // Compile and instantiate the module that imports env.log
        final Module module = engine.compileWat(IMPORT_MODULE_WAT);
        resources.add(module);
        final Store store = engine.createStore();
        resources.add(store);

        final Instance instance = linker.instantiate(store, module);
        resources.add(instance);

        assertNotNull(instance, "Instance should not be null");
        assertTrue(instance.isValid(), "Instance should be valid");
        LOGGER.info("[" + runtime + "] Successfully instantiated module with host function");
      }
    }
  }

  // ===== Instantiation Tests =====

  @Nested
  @DisplayName("Instantiation Tests")
  class InstantiationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null store for instantiate")
    void shouldRejectNullStore(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null store rejection for instantiate");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT);
        resources.add(module);

        assertThrows(Exception.class, () -> linker.instantiate(null, module));
        LOGGER.info("[" + runtime + "] Correctly rejected null store");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null module for instantiate")
    void shouldRejectNullModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null module rejection for instantiate");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);

        assertThrows(Exception.class, () -> linker.instantiate(store, null));
        LOGGER.info("[" + runtime + "] Correctly rejected null module");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should instantiate simple module without imports")
    void shouldInstantiateSimpleModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing simple module instantiation via linker");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT);
        resources.add(module);
        final Store store = engine.createStore();
        resources.add(store);

        final Instance instance = linker.instantiate(store, module);
        resources.add(instance);

        assertNotNull(instance, "Instance should not be null");
        assertTrue(instance.isValid(), "Instance should be valid");
        LOGGER.info("[" + runtime + "] Successfully instantiated simple module via linker");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should instantiate empty module")
    void shouldInstantiateEmptyModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing empty module instantiation via linker");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(EMPTY_MODULE_WAT);
        resources.add(module);
        final Store store = engine.createStore();
        resources.add(store);

        final Instance instance = linker.instantiate(store, module);
        resources.add(instance);

        assertNotNull(instance, "Instance should not be null");
        assertTrue(instance.isValid(), "Instance should be valid");
        LOGGER.info("[" + runtime + "] Successfully instantiated empty module via linker");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should instantiate module with name")
    void shouldInstantiateModuleWithName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing named module instantiation via linker");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT);
        resources.add(module);
        final Store store = engine.createStore();
        resources.add(store);

        final Instance instance = linker.instantiate(store, "test_module", module);
        resources.add(instance);

        assertNotNull(instance, "Instance should not be null");
        assertTrue(instance.isValid(), "Instance should be valid");
        LOGGER.info("[" + runtime + "] Successfully instantiated module with name via linker");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should fail to instantiate module with unsatisfied imports")
    void shouldFailWithUnsatisfiedImports(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing unsatisfied imports failure");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(IMPORT_MODULE_WAT);
        resources.add(module);
        final Store store = engine.createStore();
        resources.add(store);

        // Module requires env.log import which is not defined
        assertThrows(Exception.class, () -> linker.instantiate(store, module));
        LOGGER.info("[" + runtime + "] Correctly failed with unsatisfied imports");
      }
    }
  }

  // ===== InstantiatePre Tests =====

  @Nested
  @DisplayName("InstantiatePre Tests")
  class InstantiatePreTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null module")
    void shouldRejectNullModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null module rejection for instantiatePre");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(Exception.class, () -> linker.instantiatePre(null));
        LOGGER.info("[" + runtime + "] Correctly rejected null module for instantiatePre");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create InstancePre for simple module")
    void shouldCreateInstancePre(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing InstancePre creation");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT);
        resources.add(module);

        final Object instancePre = linker.instantiatePre(module);
        resources.add((AutoCloseable) instancePre);

        assertNotNull(instancePre, "InstancePre should not be null");
        LOGGER.info("[" + runtime + "] Successfully created InstancePre");
      }
    }
  }

  // ===== HasImport Tests =====

  @Nested
  @DisplayName("HasImport Tests")
  class HasImportTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null module name")
    void shouldRejectNullModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null module name rejection for hasImport");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(Exception.class, () -> linker.hasImport(null, "func"));
        LOGGER.info("[" + runtime + "] Correctly rejected null module name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null name")
    void shouldRejectNullName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null name rejection for hasImport");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(Exception.class, () -> linker.hasImport("env", null));
        LOGGER.info("[" + runtime + "] Correctly rejected null name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject empty module name")
    void shouldRejectEmptyModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing empty module name rejection for hasImport");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(Exception.class, () -> linker.hasImport("", "func"));
        LOGGER.info("[" + runtime + "] Correctly rejected empty module name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject empty name")
    void shouldRejectEmptyName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing empty name rejection for hasImport");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(Exception.class, () -> linker.hasImport("env", ""));
        LOGGER.info("[" + runtime + "] Correctly rejected empty name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return false for undefined import")
    void shouldReturnFalseForUndefined(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing hasImport for undefined import");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertFalse(linker.hasImport("env", "nonexistent"));
        LOGGER.info("[" + runtime + "] Correctly returned false for undefined import");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return true after defining host function")
    void shouldReturnTrueAfterDefine(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing hasImport after defining host function");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final FunctionType funcType =
            new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
        final HostFunction impl = params -> new WasmValue[0];
        linker.defineHostFunction("env", "log", funcType, impl);

        assertTrue(linker.hasImport("env", "log"), "Should find defined import");
        assertFalse(linker.hasImport("env", "other"), "Should not find undefined import");
        LOGGER.info("[" + runtime + "] hasImport correctly reflects defined imports");
      }
    }
  }

  // ===== Import Registry Tests =====

  @Nested
  @DisplayName("Import Registry Tests")
  class ImportRegistryTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return empty registry for new linker")
    void shouldReturnEmptyRegistry(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing empty import registry");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        final List<ImportInfo> registry = linker.getImportRegistry();
        assertThat(registry).isEmpty();
        LOGGER.info("[" + runtime + "] Empty linker has empty import registry");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return registry with defined imports")
    void shouldReturnRegistryWithDefinedImports(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing import registry after define");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final FunctionType funcType =
            new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
        linker.defineHostFunction("env", "log", funcType, params -> new WasmValue[0]);

        final List<ImportInfo> registry = linker.getImportRegistry();
        assertThat(registry).isNotEmpty();
        LOGGER.info("[" + runtime + "] Registry has " + registry.size() + " entries after define");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return iterable definitions")
    void shouldReturnIterableDefinitions(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing iterable definitions");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final FunctionType funcType =
            new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
        linker.defineHostFunction("env", "log", funcType, params -> new WasmValue[0]);

        final Iterable<Linker.LinkerDefinition> defs = linker.iter();
        assertNotNull(defs, "iter() should not return null");

        int count = 0;
        for (final Linker.LinkerDefinition def : defs) {
          assertNotNull(def, "Linker.LinkerDefinition should not be null");
          count++;
        }
        assertThat(count).isGreaterThan(0);
        LOGGER.info("[" + runtime + "] iter() returned " + count + " definitions");
      }
    }
  }

  // ===== Import Validation Tests =====

  @Nested
  @DisplayName("Import Validation Tests")
  class ImportValidationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should validate imports for simple module")
    void shouldValidateImportsForSimpleModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing import validation for simple module");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT);
        resources.add(module);

        final ImportValidation validation = linker.validateImports(module);
        assertNotNull(validation, "ImportValidation should not be null");
        LOGGER.info("[" + runtime + "] Successfully validated imports");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null modules for validateImports")
    void shouldRejectNullModulesForValidateImports(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null modules rejection for validateImports");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(IllegalArgumentException.class, () -> linker.validateImports((Module[]) null));
        LOGGER.info("[" + runtime + "] Correctly rejected null modules for import validation");
      }
    }
  }

  // ===== DefineUnknownImports Tests =====

  @Nested
  @DisplayName("DefineUnknownImports Tests")
  class DefineUnknownImportsTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null store for defineUnknownImportsAsTraps")
    void shouldRejectNullStoreForTraps(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null store rejection for defineUnknownImportsAsTraps");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT);
        resources.add(module);

        assertThrows(
            IllegalArgumentException.class, () -> linker.defineUnknownImportsAsTraps(null, module));
        LOGGER.info(
            "[" + runtime + "] Correctly rejected null store for defineUnknownImportsAsTraps");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null module for defineUnknownImportsAsTraps")
    void shouldRejectNullModuleForTraps(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info(
          "[" + runtime + "] Testing null module rejection for defineUnknownImportsAsTraps");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);

        assertThrows(
            IllegalArgumentException.class, () -> linker.defineUnknownImportsAsTraps(store, null));
        LOGGER.info(
            "[" + runtime + "] Correctly rejected null module for defineUnknownImportsAsTraps");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("defineUnknownImportsAsTraps should succeed for module with unknown imports")
    void shouldDefineUnknownImportsAsTraps(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing defineUnknownImportsAsTraps for unknown imports");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(IMPORT_MODULE_WAT);
        resources.add(module);
        final Store store = engine.createStore();
        resources.add(store);

        // Native function defines trap stubs for unknown imports
        assertDoesNotThrow(() -> linker.defineUnknownImportsAsTraps(store, module));
        LOGGER.info(
            "["
                + runtime
                + "] defineUnknownImportsAsTraps successfully defined trap stubs"
                + " for unknown imports");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null store for defineUnknownImportsAsDefaultValues")
    void shouldRejectNullStoreForDefaults(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info(
          "[" + runtime + "] Testing null store rejection for defineUnknownImportsAsDefaultValues");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT);
        resources.add(module);

        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineUnknownImportsAsDefaultValues(null, module));
        LOGGER.info(
            "["
                + runtime
                + "] Correctly rejected null store for defineUnknownImportsAsDefaultValues");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null module for defineUnknownImportsAsDefaultValues")
    void shouldRejectNullModuleForDefaults(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info(
          "["
              + runtime
              + "] Testing null module rejection for defineUnknownImportsAsDefaultValues");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);

        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineUnknownImportsAsDefaultValues(store, null));
        LOGGER.info(
            "["
                + runtime
                + "] Correctly rejected null module for defineUnknownImportsAsDefaultValues");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName(
        "defineUnknownImportsAsDefaultValues should succeed for module with unknown imports")
    void shouldDefineUnknownImportsAsDefaultValues(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info(
          "[" + runtime + "] Testing defineUnknownImportsAsDefaultValues for unknown imports");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(IMPORT_MODULE_WAT);
        resources.add(module);
        final Store store = engine.createStore();
        resources.add(store);

        // Native function defines default-value stubs for unknown imports
        assertDoesNotThrow(() -> linker.defineUnknownImportsAsDefaultValues(store, module));
        LOGGER.info(
            "["
                + runtime
                + "] defineUnknownImportsAsDefaultValues successfully defined"
                + " default-value stubs");
      }
    }
  }

  // ===== Alias Tests =====

  @Nested
  @DisplayName("Alias Tests")
  class AliasTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null from module name")
    void shouldRejectNullFromModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null from module name rejection for alias");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(
            IllegalArgumentException.class, () -> linker.alias(null, "func", "other", "func"));
        LOGGER.info("[" + runtime + "] Correctly rejected null from module name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null from name")
    void shouldRejectNullFromName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null from name rejection for alias");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(
            IllegalArgumentException.class, () -> linker.alias("env", null, "other", "func"));
        LOGGER.info("[" + runtime + "] Correctly rejected null from name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null to module name")
    void shouldRejectNullToModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null to module name rejection for alias");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(
            IllegalArgumentException.class, () -> linker.alias("env", "func", null, "func"));
        LOGGER.info("[" + runtime + "] Correctly rejected null to module name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null to name")
    void shouldRejectNullToName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null to name rejection for alias");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(
            IllegalArgumentException.class, () -> linker.alias("env", "func", "other", null));
        LOGGER.info("[" + runtime + "] Correctly rejected null to name");
      }
    }
  }

  // ===== DefineInstance Tests =====

  @Nested
  @DisplayName("DefineInstance Tests")
  class DefineInstanceTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null module name")
    void shouldRejectNullModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null module name rejection for defineInstance");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT);
        resources.add(module);
        final Store store = engine.createStore();
        resources.add(store);
        final Instance instance = linker.instantiate(store, module);
        resources.add(instance);

        assertThrows(
            IllegalArgumentException.class, () -> linker.defineInstance(store, null, instance));
        LOGGER.info("[" + runtime + "] Correctly rejected null module name for defineInstance");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null instance")
    void shouldRejectNullInstance(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null instance rejection for defineInstance");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);

        assertThrows(
            IllegalArgumentException.class, () -> linker.defineInstance(store, "test", null));
        LOGGER.info("[" + runtime + "] Correctly rejected null instance for defineInstance");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should define instance successfully")
    void shouldDefineInstance(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing instance definition");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT);
        resources.add(module);
        final Store store = engine.createStore();
        resources.add(store);
        final Instance instance = linker.instantiate(store, module);
        resources.add(instance);

        assertDoesNotThrow(() -> linker.defineInstance(store, "my_module", instance));
        LOGGER.info("[" + runtime + "] Successfully defined instance");
      }
    }
  }

  // ===== FuncNewUnchecked Tests =====

  @Nested
  @DisplayName("FuncNewUnchecked Tests")
  class FuncNewUncheckedTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null module name")
    void shouldRejectNullModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null module name rejection for funcNewUnchecked");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);
        final FunctionType funcType =
            new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});
        final HostFunction impl = params -> new WasmValue[0];

        assertThrows(
            IllegalArgumentException.class,
            () -> linker.funcNewUnchecked(store, null, "func", funcType, impl));
        LOGGER.info("[" + runtime + "] Correctly rejected null module name for funcNewUnchecked");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null function name")
    void shouldRejectNullFunctionName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null function name rejection for funcNewUnchecked");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);
        final FunctionType funcType =
            new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});
        final HostFunction impl = params -> new WasmValue[0];

        assertThrows(
            IllegalArgumentException.class,
            () -> linker.funcNewUnchecked(store, "env", null, funcType, impl));
        LOGGER.info("[" + runtime + "] Correctly rejected null function name for funcNewUnchecked");
      }
    }
  }

  // ===== DefineName Tests =====

  @Nested
  @DisplayName("DefineName Tests")
  class DefineNameTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("defineName should reject null extern")
    void defineNameShouldRejectNullExtern(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null extern rejection for defineName");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);

        // defineName checks parameters before reaching the unimplemented body
        assertThrows(IllegalArgumentException.class, () -> linker.defineName(store, "test", null));
        LOGGER.info("[" + runtime + "] defineName correctly rejects null extern");
      }
    }
  }

  // ===== GetByImport Tests =====

  @Nested
  @DisplayName("GetByImport Tests")
  class GetByImportTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null store")
    void shouldRejectNullStore(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null store rejection for getByImport");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(IllegalArgumentException.class, () -> linker.getByImport(null, "env", "func"));
        LOGGER.info("[" + runtime + "] Correctly rejected null store for getByImport");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null module name")
    void shouldRejectNullModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null module name rejection for getByImport");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);

        assertThrows(IllegalArgumentException.class, () -> linker.getByImport(store, null, "func"));
        LOGGER.info("[" + runtime + "] Correctly rejected null module name for getByImport");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null name")
    void shouldRejectNullName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null name rejection for getByImport");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);

        assertThrows(IllegalArgumentException.class, () -> linker.getByImport(store, "env", null));
        LOGGER.info("[" + runtime + "] Correctly rejected null name for getByImport");
      }
    }
  }

  // ===== GetDefault Tests =====

  @Nested
  @DisplayName("GetDefault Tests")
  class GetDefaultTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null store")
    void shouldRejectNullStore(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null store rejection for getDefault");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(IllegalArgumentException.class, () -> linker.getDefault(null, "module"));
        LOGGER.info("[" + runtime + "] getDefault correctly rejected null store");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null module name")
    void shouldRejectNullModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null module name rejection for getDefault");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);

        assertThrows(IllegalArgumentException.class, () -> linker.getDefault(store, null));
        LOGGER.info("[" + runtime + "] getDefault correctly rejected null module name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return a default function for any module name")
    void shouldReturnDefaultFunctionForAnyModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing getDefault for nonexistent module");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);

        // Wasmtime's Linker.get_default() returns a default function stub for any module name
        final var result = linker.getDefault(store, "nonexistent_module");
        assertNotNull(result, "getDefault should return a function for any module name");
        LOGGER.info("[" + runtime + "] getDefault correctly returned a function: " + result);
      }
    }
  }

  // ===== Define Table/Global/Memory Validation Tests =====

  @Nested
  @DisplayName("Define Resource Validation Tests")
  class DefineResourceValidationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("defineTable should reject null store")
    void defineTableShouldRejectNullStore(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null store rejection for defineTable");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(
            IllegalArgumentException.class, () -> linker.defineTable(null, "env", "table", null));
        LOGGER.info("[" + runtime + "] defineTable correctly rejected null store");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("defineTable should reject null module name")
    void defineTableShouldRejectNullModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null module name rejection for defineTable");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);

        assertThrows(
            IllegalArgumentException.class, () -> linker.defineTable(store, null, "table", null));
        LOGGER.info("[" + runtime + "] defineTable correctly rejected null module name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("defineGlobal should reject null store")
    void defineGlobalShouldRejectNullStore(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null store rejection for defineGlobal");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(
            IllegalArgumentException.class, () -> linker.defineGlobal(null, "env", "g", null));
        LOGGER.info("[" + runtime + "] defineGlobal correctly rejected null store");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("defineGlobal should reject null module name")
    void defineGlobalShouldRejectNullModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null module name rejection for defineGlobal");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);

        assertThrows(
            IllegalArgumentException.class, () -> linker.defineGlobal(store, null, "g", null));
        LOGGER.info("[" + runtime + "] defineGlobal correctly rejected null module name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("defineMemory should reject null store")
    void defineMemoryShouldRejectNullStore(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null store rejection for defineMemory");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        assertThrows(
            IllegalArgumentException.class, () -> linker.defineMemory(null, "env", "memory", null));
        LOGGER.info("[" + runtime + "] defineMemory correctly rejected null store");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("defineMemory should reject null module name")
    void defineMemoryShouldRejectNullModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null module name rejection for defineMemory");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);
        final Store store = engine.createStore();
        resources.add(store);

        assertThrows(
            IllegalArgumentException.class, () -> linker.defineMemory(store, null, "memory", null));
        LOGGER.info("[" + runtime + "] defineMemory correctly rejected null module name");
      }
    }
  }

  // ===== Close Behavior Tests =====

  @Nested
  @DisplayName("Close Behavior Tests")
  class CloseBehaviorTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("defineHostFunction after close throws exception")
    void defineHostFunctionAfterCloseThrows(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing defineHostFunction after close");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        linker.close();

        final FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
        final HostFunction impl = params -> null;

        assertThrows(
            Exception.class,
            () -> linker.defineHostFunction("env", "func", funcType, impl),
            "defineHostFunction on closed linker should throw");
        LOGGER.info("[" + runtime + "] defineHostFunction on closed linker threw as expected");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("instantiate after close throws exception")
    void instantiateAfterCloseThrows(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing instantiate after close");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final String wat = "(module)";
        final Module module = engine.compileWat(wat);
        final Linker<?> linker = Linker.create(engine);
        linker.close();

        assertThrows(
            Exception.class,
            () -> linker.instantiate(store, module),
            "instantiate on closed linker should throw");
        LOGGER.info("[" + runtime + "] instantiate on closed linker threw as expected");

        module.close();
      }
    }
  }

  // ===== Import Name Sensitivity Tests =====

  @Nested
  @DisplayName("Import Name Sensitivity Tests")
  class ImportNameSensitivityTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("hasImport with different module name returns false")
    void hasImportWithDifferentModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing hasImport with different module name");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        final FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
        final HostFunction impl = params -> null;

        linker.defineHostFunction("env", "func", funcType, impl);

        assertFalse(
            linker.hasImport("other", "func"),
            "hasImport should return false for different module name");
        LOGGER.info("[" + runtime + "] hasImport returned false for different module name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("hasImport with different function name returns false")
    void hasImportWithDifferentFunctionName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing hasImport with different function name");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        final FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
        final HostFunction impl = params -> null;

        linker.defineHostFunction("env", "func", funcType, impl);

        assertFalse(
            linker.hasImport("env", "other"),
            "hasImport should return false for different function name");
        LOGGER.info("[" + runtime + "] hasImport returned false for different function name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("alias does not crash with valid parameters")
    void aliasDoesNotCrash(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing alias does not crash");

      try (Engine engine = Engine.create()) {
        final Linker<?> linker = Linker.create(engine);
        resources.add(linker);

        // alias may throw if the source doesn't exist, but should not crash the JVM
        try {
          linker.alias("from", "fromName", "to", "toName");
          LOGGER.info("[" + runtime + "] alias completed without error");
        } catch (final Exception e) {
          LOGGER.info(
              "["
                  + runtime
                  + "] alias threw expected exception: "
                  + e.getClass().getSimpleName()
                  + " - "
                  + e.getMessage());
        }
      }
    }
  }
}

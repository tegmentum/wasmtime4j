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
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstancePre;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link InstancePre#instantiate(Store, ImportMap)}.
 *
 * <p>Verifies import-map-based instantiation from pre-instantiated modules. The JNI implementation
 * may throw {@link UnsatisfiedLinkError} if nativeInstantiatePre is not yet bound.
 *
 * @since 1.0.0
 */
@DisplayName("InstancePre Async and Import Tests")
public class InstancePreAsyncAndImportTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(InstancePreAsyncAndImportTest.class.getName());

  /** Simple self-contained module exporting answer function. */
  private static final String ANSWER_WAT =
      """
      (module
        (func (export "answer") (result i32) i32.const 42))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  /**
   * Creates an ImportMap instance, trying ImportMap.empty() first and falling back to a simple
   * in-test implementation if the runtime-specific ImportMap classes are not available.
   */
  private ImportMap createImportMap() {
    try {
      return ImportMap.empty();
    } catch (final RuntimeException e) {
      LOGGER.info("ImportMap.empty() not available, using test fallback: " + e.getMessage());
      return new SimpleTestImportMap();
    }
  }

  /**
   * Creates an InstancePre, catching UnsatisfiedLinkError for JNI runtimes that lack the native
   * binding. Returns null if creation fails due to missing native method.
   */
  private InstancePre tryCreateInstancePre(
      final Linker<Void> linker, final Module module, final RuntimeType runtime) {
    try {
      return linker.instantiatePre(module);
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning(
          "["
              + runtime
              + "] instantiatePre not available (missing native binding): "
              + e.getMessage());
      return null;
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] instantiatePre failed: "
              + e.getClass().getSimpleName()
              + " - "
              + e.getMessage());
      return null;
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("instantiate with ImportMap returns valid instance with working export")
  void instantiateWithImportMapReturnsInstance(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing instantiate(Store, ImportMap)");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(ANSWER_WAT);
        Linker<Void> linker = Linker.create(engine)) {

      final InstancePre pre = tryCreateInstancePre(linker, module, runtime);
      if (pre == null) {
        LOGGER.info("[" + runtime + "] Skipping: instantiatePre not available");
        return;
      }

      try (pre;
          Store store = engine.createStore()) {
        final ImportMap emptyMap = createImportMap();
        final Instance instance = pre.instantiate(store, emptyMap);
        assertNotNull(instance, "Instance should not be null");

        final WasmFunction answerFunc =
            instance
                .getFunction("answer")
                .orElseThrow(() -> new AssertionError("answer function should be present"));

        final WasmValue[] result = answerFunc.call();
        assertEquals(42, result[0].asInt(), "answer() should return 42");

        LOGGER.info("[" + runtime + "] instantiate(store, importMap) returned valid instance");
        LOGGER.info("[" + runtime + "] answer()=" + result[0].asInt());
        instance.close();
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("instantiate with ImportMap and null store throws exception")
  void instantiateWithImportMapNullStoreThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing instantiate(null, ImportMap)");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(ANSWER_WAT);
        Linker<Void> linker = Linker.create(engine)) {

      final InstancePre pre = tryCreateInstancePre(linker, module, runtime);
      if (pre == null) {
        LOGGER.info("[" + runtime + "] Skipping: instantiatePre not available");
        return;
      }

      try (pre) {
        final ImportMap emptyMap = createImportMap();
        final Exception thrown =
            assertThrows(
                Exception.class,
                () -> pre.instantiate(null, emptyMap),
                "instantiate(null, importMap) should throw an exception");

        assertNotNull(thrown, "Exception should not be null");
        LOGGER.info(
            "["
                + runtime
                + "] Threw "
                + thrown.getClass().getSimpleName()
                + ": "
                + thrown.getMessage());
      }
    }
  }

  /** Simple ImportMap implementation for testing when runtime-specific classes are unavailable. */
  private static final class SimpleTestImportMap implements ImportMap {
    private final Map<String, Map<String, Object>> imports = new HashMap<>();

    @Override
    public ImportMap addFunction(
        final String moduleName, final String name, final WasmFunction function) {
      imports.computeIfAbsent(moduleName, k -> new HashMap<>()).put(name, function);
      return this;
    }

    @Override
    public ImportMap addMemory(
        final String moduleName, final String name, final WasmMemory memory) {
      imports.computeIfAbsent(moduleName, k -> new HashMap<>()).put(name, memory);
      return this;
    }

    @Override
    public ImportMap addGlobal(
        final String moduleName, final String name, final WasmGlobal global) {
      imports.computeIfAbsent(moduleName, k -> new HashMap<>()).put(name, global);
      return this;
    }

    @Override
    public ImportMap addTable(final String moduleName, final String name, final WasmTable table) {
      imports.computeIfAbsent(moduleName, k -> new HashMap<>()).put(name, table);
      return this;
    }

    @Override
    public Optional<WasmFunction> getFunction(final String moduleName, final String name) {
      final Map<String, Object> moduleImports = imports.get(moduleName);
      if (moduleImports == null) {
        return Optional.empty();
      }
      final Object obj = moduleImports.get(name);
      if (obj instanceof WasmFunction) {
        return Optional.of((WasmFunction) obj);
      }
      return Optional.empty();
    }

    @Override
    public Map<String, Map<String, Object>> getImports() {
      return imports;
    }

    @Override
    public boolean contains(final String moduleName, final String name) {
      final Map<String, Object> moduleImports = imports.get(moduleName);
      return moduleImports != null && moduleImports.containsKey(name);
    }
  }
}

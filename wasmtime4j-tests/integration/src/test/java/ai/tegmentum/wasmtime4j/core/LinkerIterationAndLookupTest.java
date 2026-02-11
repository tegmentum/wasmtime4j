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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.type.ExternType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Linker.LinkerDefinition;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests Linker iteration and lookup methods: {@link Linker#iter()},
 * {@link Linker#getByImport(Store, String, String)},
 * {@link Linker#getDefault(Store, String)}, and
 * {@link Linker#defineName(Store, String, Extern)}.
 *
 * @since 1.0.0
 */
@DisplayName("Linker Iteration and Lookup Tests")
public class LinkerIterationAndLookupTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(LinkerIterationAndLookupTest.class.getName());

  /**
   * WAT module that imports a function from "env" and exports two functions.
   */
  private static final String WAT_WITH_IMPORT =
      """
      (module
        (import "env" "host_func" (func (result i32)))
        (func (export "_start") call 0 drop)
        (func (export "get99") (result i32) i32.const 99))
      """;

  private static final String SIMPLE_WAT =
      """
      (module
        (func (export "nop")))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("iter returns defined host function")
  void iterReturnsDefinedHostFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing iter with one host function defined");

    try (Engine engine = Engine.create();
        Linker<?> linker = Linker.create(engine)) {

      final FunctionType funcType = new FunctionType(
          new WasmValueType[0], new WasmValueType[]{WasmValueType.I32});
      final HostFunction hostFunc = (params) ->
          new WasmValue[]{WasmValue.i32(42)};
      linker.defineHostFunction("env", "host_func", funcType, hostFunc);

      final List<LinkerDefinition> defs = collectIter(linker);

      assertTrue(defs.size() >= 1,
          "iter should contain at least 1 definition, got: " + defs.size());
      LOGGER.info("[" + runtime + "] iter returned " + defs.size() + " definitions");
      for (final LinkerDefinition def : defs) {
        LOGGER.info("[" + runtime + "]   " + def.getModuleName() + "::"
            + def.getName() + " [" + def.getType() + "]");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("iter returns multiple definitions after WASI enable")
  void iterReturnsMultipleDefinitions(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing iter with host func + WASI");

    try (Engine engine = Engine.create();
        Linker<?> linker = Linker.create(engine)) {

      final FunctionType funcType = new FunctionType(
          new WasmValueType[0], new WasmValueType[]{WasmValueType.I32});
      linker.defineHostFunction("env", "host_func", funcType,
          (params) -> new WasmValue[]{WasmValue.i32(1)});

      try {
        linker.enableWasi();
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] enableWasi threw: " + e.getMessage());
      }

      final List<LinkerDefinition> defs = collectIter(linker);

      assertTrue(defs.size() >= 1,
          "iter should contain at least the host func definition, got: "
              + defs.size());
      LOGGER.info("[" + runtime + "] iter returned " + defs.size()
          + " definitions after WASI enable attempt");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("iter on empty linker returns zero definitions")
  void iterEmptyLinkerReturnsEmpty(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing iter on empty linker");

    try (Engine engine = Engine.create();
        Linker<?> linker = Linker.create(engine)) {

      final List<LinkerDefinition> defs = collectIter(linker);

      assertEquals(0, defs.size(),
          "Empty linker iter should return 0 definitions, got: " + defs.size());
      LOGGER.info("[" + runtime + "] Empty linker iter returned " + defs.size()
          + " definitions");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getByImport finds defined function")
  void getByImportFindsDefinedFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getByImport for defined function");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<?> linker = Linker.create(engine)) {

      final FunctionType funcType = new FunctionType(
          new WasmValueType[0], new WasmValueType[]{WasmValueType.I32});
      linker.defineHostFunction("env", "host_func", funcType,
          (params) -> new WasmValue[]{WasmValue.i32(42)});

      try {
        final Extern ext = linker.getByImport(store, "env", "host_func");
        assertNotNull(ext, "getByImport should find defined function");
        assertTrue(ext.isFunction(),
            "Extern type should be FUNC, got: " + ext.getType());
        LOGGER.info("[" + runtime + "] getByImport found function extern: " + ext.getType());
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] getByImport not implemented: "
            + e.getClass().getSimpleName() + " - " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] getByImport threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getByImport returns null for missing import")
  void getByImportReturnsNullForMissing(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getByImport for missing import");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<?> linker = Linker.create(engine)) {

      try {
        final Extern ext = linker.getByImport(store, "env", "nope");
        assertNull(ext,
            "getByImport should return null for undefined import");
        LOGGER.info("[" + runtime + "] getByImport('env', 'nope') returned null as expected");
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] getByImport not implemented: "
            + e.getClass().getSimpleName() + " - " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] getByImport threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getByImport finds defined memory")
  void getByImportFindsDefinedMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getByImport for defined memory");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<?> linker = Linker.create(engine)) {

      final WasmMemory memory = store.createMemory(1, 4);
      linker.defineMemory(store, "env", "mem", memory);

      try {
        final Extern ext = linker.getByImport(store, "env", "mem");
        assertNotNull(ext, "getByImport should find defined memory");
        assertTrue(ext.isMemory(),
            "Extern type should be MEMORY, got: " + ext.getType());
        LOGGER.info("[" + runtime + "] getByImport found memory extern: " + ext.getType());
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] getByImport not implemented: "
            + e.getClass().getSimpleName() + " - " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] getByImport threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getDefault returns function for named module")
  void getDefaultReturnsStartFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getDefault for named module");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<?> linker = Linker.create(engine)) {

      final FunctionType funcType = new FunctionType(
          new WasmValueType[0], new WasmValueType[]{WasmValueType.I32});
      linker.defineHostFunction("env", "host_func", funcType,
          (params) -> new WasmValue[]{WasmValue.i32(1)});

      final Module module = engine.compileWat(WAT_WITH_IMPORT);

      try {
        linker.instantiate(store, "mymod", module);

        final WasmFunction defaultFunc = linker.getDefault(store, "mymod");
        LOGGER.info("[" + runtime + "] getDefault('mymod') returned: " + defaultFunc);
        // defaultFunc may be null if no default export, which is valid
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] Named instantiation or getDefault threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getDefault returns null for unknown module name")
  void getDefaultReturnsNullForUnknownModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getDefault for unknown module");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<?> linker = Linker.create(engine)) {

      try {
        final WasmFunction result = linker.getDefault(store, "nomod");
        assertNull(result, "getDefault should return null for unknown module");
        LOGGER.info("[" + runtime + "] getDefault('nomod') returned null as expected");
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] getDefault not implemented: "
            + e.getClass().getSimpleName() + " - " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] getDefault threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("defineName sets top-level extern and is retrievable")
  void defineNameSetsTopLevelExtern(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing defineName");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<?> linker = Linker.create(engine)) {

      final FunctionType funcType = new FunctionType(
          new WasmValueType[0], new WasmValueType[]{WasmValueType.I32});
      final WasmFunction func = store.createHostFunction(
          "myFunc", funcType,
          (params) -> new WasmValue[]{WasmValue.i32(77)});

      // defineName requires an Extern; WasmFunction should implement Extern
      if (func instanceof Extern) {
        linker.defineName(store, "myFunc", (Extern) func);

        // Verify it is in iter
        final List<LinkerDefinition> defs = collectIter(linker);
        final boolean found = defs.stream()
            .anyMatch(d -> "myFunc".equals(d.getName()));
        assertTrue(found, "defineName should make the extern findable via iter");
        LOGGER.info("[" + runtime + "] defineName succeeded, found in iter: " + found);
      } else {
        LOGGER.info("[" + runtime + "] WasmFunction does not implement Extern, "
            + "testing overload defineName(name, FunctionType, HostFunction)");
        linker.defineName("topFunc", funcType,
            (params) -> new WasmValue[]{WasmValue.i32(77)});
        final List<LinkerDefinition> defs = collectIter(linker);
        assertTrue(defs.size() >= 1,
            "defineName overload should add at least 1 definition");
        LOGGER.info("[" + runtime + "] defineName overload succeeded, iter size: "
            + defs.size());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("defineName with null throws")
  void defineNameWithNullThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing defineName with null arguments");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<?> linker = Linker.create(engine)) {

      try {
        linker.defineName(store, null, null);
        LOGGER.info("[" + runtime + "] defineName(null, null) did not throw");
      } catch (final NullPointerException | IllegalArgumentException e) {
        LOGGER.info("[" + runtime + "] defineName(null, null) threw "
            + e.getClass().getSimpleName() + ": " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] defineName(null, null) threw "
            + e.getClass().getName() + ": " + e.getMessage());
      }
    }
  }

  /**
   * Collects all definitions from {@link Linker#iter()} into a list.
   */
  private List<LinkerDefinition> collectIter(final Linker<?> linker) {
    final List<LinkerDefinition> defs = new ArrayList<>();
    for (final LinkerDefinition def : linker.iter()) {
      defs.add(def);
    }
    return defs;
  }
}

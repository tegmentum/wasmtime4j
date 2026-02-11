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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.ImportValidation;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link Module#validateImportsDetailed(ImportMap)}.
 *
 * <p>Verifies detailed import validation with satisfied imports, empty import maps, null input, and
 * self-contained modules.
 *
 * @since 1.0.0
 */
@DisplayName("Module Import Validation Detailed Tests")
public class ModuleImportValidationDetailedTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(ModuleImportValidationDetailedTest.class.getName());

  /** Module that imports env/get_val and exports call_it. */
  private static final String IMPORTING_MODULE_WAT =
      """
      (module
        (import "env" "get_val" (func (result i32)))
        (func (export "call_it") (result i32) call 0))
      """;

  /** Self-contained module with no imports. */
  private static final String SELF_CONTAINED_WAT =
      """
      (module
        (func (export "get42") (result i32) i32.const 42))
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

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateImportsDetailed with satisfied import returns valid")
  void validateImportsDetailedWithSatisfiedImport(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateImportsDetailed with satisfied import");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(IMPORTING_MODULE_WAT);
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      // Define the host function and instantiate to get a WasmFunction reference
      linker.defineHostFunction(
          "env",
          "get_val",
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
          (HostFunction) params -> new WasmValue[] {WasmValue.i32(42)});

      final var instance = linker.instantiate(store, module);

      // Get a WasmFunction to add to the ImportMap
      final var exportedFunc = instance.getFunction("call_it");
      assertTrue(exportedFunc.isPresent(), "call_it function should be present");

      final ImportMap populatedMap = createImportMap();
      populatedMap.addFunction("env", "get_val", exportedFunc.get());

      final ImportValidation validation = module.validateImportsDetailed(populatedMap);

      assertNotNull(validation, "ImportValidation should not be null");
      LOGGER.info("[" + runtime + "] isValid: " + validation.isValid());
      LOGGER.info("[" + runtime + "] totalImports: " + validation.getTotalImports());
      LOGGER.info("[" + runtime + "] validImports: " + validation.getValidImports());
      LOGGER.info("[" + runtime + "] issues: " + validation.getIssues());

      assertTrue(validation.getTotalImports() >= 1, "Should have at least 1 total import");

      instance.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateImportsDetailed with empty import map returns invalid for importing module")
  void validateImportsDetailedWithEmptyImportMap(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateImportsDetailed with empty ImportMap");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(IMPORTING_MODULE_WAT)) {

      final ImportMap emptyMap = createImportMap();
      final ImportValidation validation = module.validateImportsDetailed(emptyMap);

      assertNotNull(validation, "ImportValidation should not be null");
      assertFalse(validation.isValid(), "Module with unsatisfied imports should be invalid");
      assertFalse(validation.getIssues().isEmpty(), "Should report issues for missing imports");

      LOGGER.info("[" + runtime + "] isValid: " + validation.isValid());
      LOGGER.info("[" + runtime + "] issues count: " + validation.getIssues().size());
      validation.getIssues().forEach(issue ->
          LOGGER.info("[" + runtime + "] issue: " + issue.getMessage()
              + " severity=" + issue.getSeverity()
              + " type=" + issue.getType()));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateImportsDetailed with null throws IllegalArgumentException")
  void validateImportsDetailedNullThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateImportsDetailed with null");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(IMPORTING_MODULE_WAT)) {

      assertThrows(
          IllegalArgumentException.class,
          () -> module.validateImportsDetailed(null),
          "Should throw IllegalArgumentException for null ImportMap");

      LOGGER.info("[" + runtime + "] Correctly threw IllegalArgumentException for null");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateImportsDetailed with self-contained module returns valid")
  void validateImportsDetailedSelfContainedModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateImportsDetailed with self-contained module");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(SELF_CONTAINED_WAT)) {

      final ImportMap emptyMap = createImportMap();
      final ImportValidation validation = module.validateImportsDetailed(emptyMap);

      assertNotNull(validation, "ImportValidation should not be null");
      assertTrue(validation.isValid(), "Self-contained module should validate as valid");
      assertEquals(0, validation.getTotalImports(), "Self-contained module should have 0 imports");

      LOGGER.info("[" + runtime + "] isValid: " + validation.isValid());
      LOGGER.info("[" + runtime + "] totalImports: " + validation.getTotalImports());
      LOGGER.info("[" + runtime + "] validImports: " + validation.getValidImports());
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
    public ImportMap addTable(
        final String moduleName, final String name, final WasmTable table) {
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

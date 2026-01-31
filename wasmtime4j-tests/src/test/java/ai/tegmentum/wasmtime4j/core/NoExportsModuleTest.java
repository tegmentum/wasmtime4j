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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for modules with no exports. Verifies that empty modules, import-only modules, and start
 * function modules compile and instantiate correctly even without exports.
 *
 * @since 1.0.0
 */
@DisplayName("No-Exports Module Tests")
public class NoExportsModuleTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(NoExportsModuleTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Compile and instantiate module with no exports")
  void moduleWithNoExports(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing module with no exports");

    final String wat =
        """
        (module
          (func (nop)))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Module with no exports should compile");

      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Module with no exports should instantiate");

      final var exports = module.getExports();
      assertNotNull(exports, "getExports() should not return null");
      assertTrue(exports.isEmpty(), "getExports() should return empty list");
      LOGGER.info("[" + runtime + "] Module with no exports: exports=" + exports.size());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getFunction on no-exports module returns empty")
  void getFunctionOnNoExportsModuleReturnsEmpty(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getFunction on no-exports module");

    final String wat =
        """
        (module
          (func (nop)))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final var func = instance.getFunction("any_name");
      assertFalse(func.isPresent(), "getFunction should return empty for non-existent export");
      LOGGER.info("[" + runtime + "] getFunction returned empty as expected");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Module with only imports (no exports)")
  void moduleWithOnlyImports(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing module with only imports");

    final String wat =
        """
        (module
          (import "env" "log" (func (param i32)))
          (func
            i32.const 42
            call 0))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Module with only imports should compile");

      final var imports = module.getImports();
      assertFalse(imports.isEmpty(), "Module should have imports");

      final var exports = module.getExports();
      assertTrue(exports.isEmpty(), "Module should have no exports");
      LOGGER.info(
          "["
              + runtime
              + "] Import-only module: imports="
              + imports.size()
              + ", exports="
              + exports.size());

      // Instantiate with linker providing the import
      final Linker<Void> linker = Linker.create(engine);
      linker.defineHostFunction(
          "env",
          "log",
          FunctionType.of(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {}),
          HostFunction.voidFunction(params -> {}));

      final Instance instance = linker.instantiate(store, module);
      assertNotNull(instance, "Import-only module should instantiate with linker");

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Empty module (truly empty)")
  void emptyModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing truly empty module");

    final String wat = "(module)";

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Empty module should compile");

      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Empty module should instantiate");

      assertTrue(module.getExports().isEmpty(), "Empty module should have no exports");
      assertTrue(module.getImports().isEmpty(), "Empty module should have no imports");
      LOGGER.info("[" + runtime + "] Empty module compiled and instantiated");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("hasExport returns false for non-existent export")
  void hasExportReturnsFalseForNonExistent(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing hasExport on no-exports module");

    final String wat = "(module)";

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      assertFalse(instance.hasExport("nonexistent"), "hasExport should return false");
      LOGGER.info("[" + runtime + "] hasExport returned false as expected");

      instance.close();
      module.close();
    }
  }
}

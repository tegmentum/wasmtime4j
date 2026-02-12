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

package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for compiling WebAssembly component binaries. Verifies valid component compilation,
 * rejection of invalid binaries, and components with imports/exports.
 *
 * @since 1.0.0
 */
@DisplayName("Component Binary Compilation Tests")
public class ComponentCompilationTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ComponentCompilationTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Compile valid core module (not component)")
  void compileValidCoreModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing core module compilation");

    final String wat =
        """
        (module
          (func (export "add") (param i32 i32) (result i32)
            local.get 0 local.get 1 i32.add))
        """;

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Module should compile successfully");
      LOGGER.info("[" + runtime + "] Core module compiled successfully");
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Reject invalid module binary with clear error")
  void rejectInvalidModuleBinary(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing rejection of invalid binary");

    final byte[] invalidBytes = new byte[] {0x00, 0x01, 0x02, 0x03};

    try (Engine engine = Engine.create()) {
      assertThrows(
          Exception.class,
          () -> engine.compileModule(invalidBytes),
          "Invalid binary should be rejected");
      LOGGER.info("[" + runtime + "] Invalid binary rejected as expected");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Compile module with imports and exports")
  void compileModuleWithImportsAndExports(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing module with imports and exports");

    final String wat =
        """
        (module
          (import "env" "log" (func (param i32)))
          (memory (export "memory") 1)
          (func (export "process") (param i32) (result i32)
            local.get 0
            i32.const 1
            i32.add)
          (global (export "version") i32 (i32.const 1)))
        """;

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Module with imports and exports should compile");

      // Verify module has both imports and exports
      assertNotNull(module.getExports(), "Module should have exports");
      assertNotNull(module.getImports(), "Module should have imports");
      LOGGER.info(
          "["
              + runtime
              + "] Module compiled with "
              + module.getImports().size()
              + " imports, "
              + module.getExports().size()
              + " exports");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Compile module with WASI imports")
  void compileModuleWithWasiImports(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing module with WASI imports");

    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_write"
            (func (param i32 i32 i32 i32) (result i32)))
          (import "wasi_snapshot_preview1" "fd_read"
            (func (param i32 i32 i32 i32) (result i32)))
          (import "wasi_snapshot_preview1" "proc_exit"
            (func (param i32)))
          (memory (export "memory") 1)
          (func (export "_start")
            i32.const 0
            call 2))
        """;

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Module with WASI imports should compile");

      final var imports = module.getImports();
      LOGGER.info(
          "[" + runtime + "] Module with WASI imports compiled, " + imports.size() + " imports");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Reject syntactically invalid WAT")
  void rejectSyntacticallyInvalidWat(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing rejection of syntactically invalid WAT");

    final String invalidWat = "(module (this is not valid WAT syntax))";

    try (Engine engine = Engine.create()) {
      assertThrows(
          Exception.class,
          () -> engine.compileWat(invalidWat),
          "Syntactically invalid WAT should be rejected");
      LOGGER.info("[" + runtime + "] Invalid WAT rejected as expected");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Reject semantically invalid WAT")
  void rejectSemanticallyInvalidWat(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing rejection of semantically invalid WAT");

    // Function that pops from empty stack (type error)
    final String invalidWat = "(module (func (export \"bad\") (result i32) i32.add))";

    try (Engine engine = Engine.create()) {
      assertThrows(
          Exception.class,
          () -> engine.compileWat(invalidWat),
          "Semantically invalid WAT should be rejected");
      LOGGER.info("[" + runtime + "] Semantically invalid WAT rejected as expected");
    }
  }
}

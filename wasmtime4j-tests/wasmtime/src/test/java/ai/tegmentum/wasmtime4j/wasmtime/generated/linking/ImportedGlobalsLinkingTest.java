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
package ai.tegmentum.wasmtime4j.wasmtime.generated.linking;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: winch/imported_globals_fuzzbug.wast
 *
 * <p>Tests importing globals from one module to another, with extensive stack operations on the
 * imported global. Originally a fuzzbug test for the Winch compiler.
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/winch/imported_globals_fuzzbug.wast
 */
public final class ImportedGlobalsLinkingTest extends DualRuntimeTest {

  private static final String MODULE_EXPORTER =
      "(module\n" + "  (global (export \"b\") i32 (i32.const 0))\n" + ")";

  private static final String MODULE_IMPORTER =
      "(module\n"
          + "  (import \"a\" \"b\" (global i32))\n"
          + "  (func (export \"start\")\n"
          + "    (local i32 i32 i32)\n"
          + "    local.get 2\n"
          + "    local.get 2\n"
          + "    local.get 2\n"
          + "    local.get 2\n"
          + "    local.get 2\n"
          + "    local.get 2\n"
          + "    local.get 2\n"
          + "    local.get 2\n"
          + "    global.get 0\n"
          + "    global.get 0\n"
          + "    global.get 0\n"
          + "    global.get 0\n"
          + "    global.get 0\n"
          + "    global.get 0\n"
          + "    global.get 0\n"
          + "    global.get 0\n"
          + "    global.get 0\n"
          + "    global.get 0\n"
          + "    global.get 0\n"
          + "    global.get 0\n"
          + "    local.get 2\n"
          + "    global.get 0\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "    drop\n"
          + "  )\n"
          + ")";

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import global and access with stack operations")
  public void testImportGlobalStackOperations(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module
      runner.compileAndInstantiate("a", MODULE_EXPORTER);
      runner.registerModule("a");

      // Create the importer module that uses the imported global
      runner.compileAndInstantiate(MODULE_IMPORTER);

      // Invoke start function - should complete without error
      runner.assertReturn("start", new WasmValue[0]);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import global type mismatch fails - i32 vs i64")
  public void testGlobalTypeMismatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Export an i32 global
      runner.compileAndInstantiate("a", MODULE_EXPORTER);
      runner.registerModule("a");

      // Try to import as i64 - should fail
      final String badImport =
          "(module\n"
              + "  (import \"a\" \"b\" (global i64))\n"
              + "  (func (export \"test\"))\n"
              + ")";
      runner.assertUnlinkable(badImport, "incompatible");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import mutable global as immutable fails")
  public void testMutabilityMismatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Export an immutable global
      runner.compileAndInstantiate("a", MODULE_EXPORTER);
      runner.registerModule("a");

      // Try to import as mutable - should fail
      final String badImport =
          "(module\n"
              + "  (import \"a\" \"b\" (global (mut i32)))\n"
              + "  (func (export \"test\"))\n"
              + ")";
      runner.assertUnlinkable(badImport, "incompatible");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import global from non-existent module fails")
  public void testNonExistentModuleFails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Try to import from a module that doesn't exist
      final String badImport =
          "(module\n"
              + "  (import \"nonexistent\" \"b\" (global i32))\n"
              + "  (func (export \"test\"))\n"
              + ")";
      // Error messages vary between runtimes - just verify linking fails
      runner.assertUnlinkable(badImport, null);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import non-existent global fails")
  public void testNonExistentGlobalFails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Register a module
      runner.compileAndInstantiate("a", MODULE_EXPORTER);
      runner.registerModule("a");

      // Try to import a global that doesn't exist
      final String badImport =
          "(module\n"
              + "  (import \"a\" \"nonexistent\" (global i32))\n"
              + "  (func (export \"test\"))\n"
              + ")";
      // Error messages vary between runtimes - just verify linking fails
      runner.assertUnlinkable(badImport, null);
    }
  }
}

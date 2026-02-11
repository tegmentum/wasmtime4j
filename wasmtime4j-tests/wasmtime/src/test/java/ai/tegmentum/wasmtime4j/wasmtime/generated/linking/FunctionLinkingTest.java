/*
 * Copyright 2024 Tegmentum AI
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
 * Tests for linking functions between modules.
 *
 * <p>Verifies that function imports/exports work correctly across module boundaries, including type
 * checking and signature validation.
 */
public final class FunctionLinkingTest extends DualRuntimeTest {

  private static final String MODULE_EXPORTER =
      "(module\n"
          + "  (func (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add\n"
          + "  )\n"
          + "  (func (export \"sub\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.sub\n"
          + "  )\n"
          + "  (func (export \"nop\") nop)\n"
          + ")";

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import and call exported function")
  public void testImportAndCallFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module
      runner.compileAndInstantiate("math", MODULE_EXPORTER);
      runner.registerModule("math");

      // Create importer module
      final String importer =
          "(module\n"
              + "  (import \"math\" \"add\" (func $add (param i32 i32) (result i32)))\n"
              + "  (func (export \"call_add\") (param i32 i32) (result i32)\n"
              + "    local.get 0\n"
              + "    local.get 1\n"
              + "    call $add\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(importer);

      // Test the imported function
      runner.assertReturn(
          "call_add", new WasmValue[] {WasmValue.i32(15)}, WasmValue.i32(10), WasmValue.i32(5));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import multiple functions from same module")
  public void testImportMultipleFunctions(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module
      runner.compileAndInstantiate("math", MODULE_EXPORTER);
      runner.registerModule("math");

      // Create importer module that imports multiple functions
      final String importer =
          "(module\n"
              + "  (import \"math\" \"add\" (func $add (param i32 i32) (result i32)))\n"
              + "  (import \"math\" \"sub\" (func $sub (param i32 i32) (result i32)))\n"
              + "  (func (export \"compute\") (param i32 i32) (result i32)\n"
              + "    local.get 0\n"
              + "    local.get 1\n"
              + "    call $add\n"
              + "    local.get 1\n"
              + "    call $sub\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(importer);

      // 10 + 5 = 15, then 15 - 5 = 10
      runner.assertReturn(
          "compute", new WasmValue[] {WasmValue.i32(10)}, WasmValue.i32(10), WasmValue.i32(5));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Function signature mismatch fails - wrong param count")
  public void testFunctionSignatureMismatchParamCount(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module
      runner.compileAndInstantiate("math", MODULE_EXPORTER);
      runner.registerModule("math");

      // Try to import with wrong number of params
      final String badImport =
          "(module\n"
              + "  (import \"math\" \"add\" (func $add (param i32) (result i32)))\n"
              + "  (func (export \"test\") (result i32) i32.const 0)\n"
              + ")";
      runner.assertUnlinkable(badImport, "incompatible");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Function signature mismatch fails - wrong param type")
  public void testFunctionSignatureMismatchParamType(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module
      runner.compileAndInstantiate("math", MODULE_EXPORTER);
      runner.registerModule("math");

      // Try to import with wrong param type
      final String badImport =
          "(module\n"
              + "  (import \"math\" \"add\" (func $add (param i64 i64) (result i32)))\n"
              + "  (func (export \"test\") (result i32) i32.const 0)\n"
              + ")";
      runner.assertUnlinkable(badImport, "incompatible");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Function signature mismatch fails - wrong result type")
  public void testFunctionSignatureMismatchResultType(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module
      runner.compileAndInstantiate("math", MODULE_EXPORTER);
      runner.registerModule("math");

      // Try to import with wrong result type
      final String badImport =
          "(module\n"
              + "  (import \"math\" \"add\" (func $add (param i32 i32) (result i64)))\n"
              + "  (func (export \"test\") (result i32) i32.const 0)\n"
              + ")";
      runner.assertUnlinkable(badImport, "incompatible");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Chain function calls across three modules")
  public void testChainFunctionCalls(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Module A: exports double function
      final String moduleA =
          "(module\n"
              + "  (func (export \"double\") (param i32) (result i32)\n"
              + "    local.get 0\n"
              + "    local.get 0\n"
              + "    i32.add\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate("A", moduleA);
      runner.registerModule("A");

      // Module B: imports double from A, exports triple
      final String moduleB =
          "(module\n"
              + "  (import \"A\" \"double\" (func $double (param i32) (result i32)))\n"
              + "  (func (export \"triple\") (param i32) (result i32)\n"
              + "    local.get 0\n"
              + "    call $double\n"
              + "    local.get 0\n"
              + "    i32.add\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate("B", moduleB);
      runner.registerModule("B");

      // Module C: imports triple from B, exports compute
      final String moduleC =
          "(module\n"
              + "  (import \"B\" \"triple\" (func $triple (param i32) (result i32)))\n"
              + "  (func (export \"compute\") (param i32) (result i32)\n"
              + "    local.get 0\n"
              + "    call $triple\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(moduleC);

      // 5 * 3 = 15
      runner.assertReturn("compute", new WasmValue[] {WasmValue.i32(15)}, WasmValue.i32(5));
    }
  }
}

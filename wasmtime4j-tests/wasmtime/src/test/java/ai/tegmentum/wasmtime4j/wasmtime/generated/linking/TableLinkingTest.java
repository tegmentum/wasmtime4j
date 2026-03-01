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
 * Tests for linking tables between modules.
 *
 * <p>Verifies that table imports/exports work correctly across module boundaries, including element
 * type and size compatibility checking.
 */
public final class TableLinkingTest extends DualRuntimeTest {

  private static final String MODULE_EXPORTER =
      "(module\n"
          + "  (func $f (result i32) i32.const 42)\n"
          + "  (func $g (result i32) i32.const 99)\n"
          + "  (table (export \"table\") funcref (elem $f $g))\n"
          + ")";

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import and use exported table")
  public void testImportAndUseTable(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module
      runner.compileAndInstantiate("t", MODULE_EXPORTER);
      runner.registerModule("t");

      // Create importer module
      final String importer =
          "(module\n"
              + "  (import \"t\" \"table\" (table $tab 2 funcref))\n"
              + "  (type $fn (func (result i32)))\n"
              + "  (func (export \"call_0\") (result i32)\n"
              + "    i32.const 0\n"
              + "    call_indirect $tab (type $fn)\n"
              + "  )\n"
              + "  (func (export \"call_1\") (result i32)\n"
              + "    i32.const 1\n"
              + "    call_indirect $tab (type $fn)\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(importer);

      // Test calling functions via imported table
      runner.assertReturn("call_0", new WasmValue[] {WasmValue.i32(42)});
      runner.assertReturn("call_1", new WasmValue[] {WasmValue.i32(99)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Table size too large fails")
  public void testTableSizeTooLarge(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module (table has 2 elements)
      runner.compileAndInstantiate("t", MODULE_EXPORTER);
      runner.registerModule("t");

      // Try to import with larger minimum size - should fail
      final String badImport =
          "(module\n"
              + "  (import \"t\" \"table\" (table 10 funcref))\n"
              + "  (func (export \"test\"))\n"
              + ")";
      runner.assertUnlinkable(badImport, "incompatible");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Table element type mismatch fails")
  public void testTableElementTypeMismatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module (funcref table)
      runner.compileAndInstantiate("t", MODULE_EXPORTER);
      runner.registerModule("t");

      // Try to import as externref - should fail
      final String badImport =
          "(module\n"
              + "  (import \"t\" \"table\" (table 2 externref))\n"
              + "  (func (export \"test\"))\n"
              + ")";
      runner.assertUnlinkable(badImport, "incompatible");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Modify imported table")
  public void testModifyImportedTable(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create exporter with mutable table
      final String exporter =
          "(module\n"
              + "  (func $f (result i32) i32.const 42)\n"
              + "  (table (export \"table\") 2 funcref)\n"
              + "  (elem (i32.const 0) $f)\n"
              + ")";
      runner.compileAndInstantiate("t", exporter);
      runner.registerModule("t");

      // Create importer that modifies table
      final String importer =
          "(module\n"
              + "  (import \"t\" \"table\" (table $tab 2 funcref))\n"
              + "  (type $fn (func (result i32)))\n"
              + "  (func $new (result i32) i32.const 100)\n"
              + "  (elem declare func $new)\n"
              + "  (func (export \"set_and_call\") (result i32)\n"
              + "    i32.const 1\n"
              + "    ref.func $new\n"
              + "    table.set $tab\n"
              + "    i32.const 1\n"
              + "    call_indirect $tab (type $fn)\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(importer);

      // Set slot 1 to $new and call it
      runner.assertReturn("set_and_call", new WasmValue[] {WasmValue.i32(100)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Share table between three modules")
  public void testShareTableBetweenModules(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Module A: creates and exports table
      final String moduleA =
          "(module\n"
              + "  (func $a (result i32) i32.const 1)\n"
              + "  (table (export \"shared\") 3 funcref)\n"
              + "  (elem (i32.const 0) $a)\n"
              + ")";
      runner.compileAndInstantiate("A", moduleA);
      runner.registerModule("A");

      // Module B: imports table, adds its own function
      final String moduleB =
          "(module\n"
              + "  (import \"A\" \"shared\" (table $tab 3 funcref))\n"
              + "  (func $b (result i32) i32.const 2)\n"
              + "  (elem declare func $b)\n"
              + "  (func (export \"add_b\")\n"
              + "    i32.const 1\n"
              + "    ref.func $b\n"
              + "    table.set $tab\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate("B", moduleB);
      runner.registerModule("B");

      // Module C: imports table, can call all functions
      final String moduleC =
          "(module\n"
              + "  (import \"A\" \"shared\" (table $tab 3 funcref))\n"
              + "  (import \"B\" \"add_b\" (func $add_b))\n"
              + "  (type $fn (func (result i32)))\n"
              + "  (func (export \"init_and_call_1\") (result i32)\n"
              + "    call $add_b\n"
              + "    i32.const 1\n"
              + "    call_indirect $tab (type $fn)\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(moduleC);

      // Call add_b to set slot 1, then call slot 1
      runner.assertReturn("init_and_call_1", new WasmValue[] {WasmValue.i32(2)});
    }
  }
}

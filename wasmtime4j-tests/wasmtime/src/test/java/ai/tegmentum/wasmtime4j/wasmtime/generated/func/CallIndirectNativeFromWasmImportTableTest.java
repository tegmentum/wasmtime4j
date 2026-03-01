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
package ai.tegmentum.wasmtime4j.wasmtime.generated.func;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Equivalent Java test for Wasmtime test: func::call_indirect_native_from_wasm_import_table
 *
 * <p>Original source: func.rs:272 Category: func
 *
 * <p>This test validates table imports using the Linker.defineTable() API. We export a table from
 * one module and import it into another module via the linker.
 *
 * <p>Note: The original Wasmtime test puts a host function directly into a table. This variant
 * tests table sharing between modules, which exercises the defineTable API without requiring
 * host-function-in-table support.
 */
public final class CallIndirectNativeFromWasmImportTableTest extends DualRuntimeTest {

  @ParameterizedTest(name = "{0}")
  @EnumSource(RuntimeType.class)
  @DisplayName("func::call_indirect_native_from_wasm_import_table - table import via linker")
  public void testCallIndirectNativeFromWasmImportTable(final RuntimeType runtime)
      throws Exception {

    try (final WastTestRunner runner = new WastTestRunner(runtime)) {
      // Module 1: Creates and exports a table with a function in it
      final String wat1 =
          """
          (module
            (table (export "shared_table") 2 funcref)
            (func $add_42 (param i32) (result i32)
              local.get 0
              i32.const 42
              i32.add
            )
            (func $mul_2 (param i32) (result i32)
              local.get 0
              i32.const 2
              i32.mul
            )
            (elem (i32.const 0) $add_42 $mul_2)
          )
          """;

      // Module 2: Imports the table and calls functions through it via call_indirect
      final String wat2 =
          """
          (module
            (import "mod1" "shared_table" (table 2 funcref))
            (type $sig (func (param i32) (result i32)))
            (func (export "call_table") (param $idx i32) (param $val i32) (result i32)
              local.get $val
              local.get $idx
              call_indirect (type $sig)
            )
          )
          """;

      // Compile and instantiate module 1
      final Module module1 = runner.getEngine().compileWat(wat1);
      final Instance instance1 = module1.instantiate(runner.getStore());

      // Get the exported table from module 1
      final Optional<WasmTable> tableOpt = instance1.getTable("shared_table");
      assertTrue(tableOpt.isPresent(), "Table 'shared_table' should be exported from module 1");

      final WasmTable sharedTable = tableOpt.get();
      assertEquals(2, sharedTable.getSize(), "Table should have 2 elements");

      // Create linker and define the table so module 2 can import it
      final Linker<Void> linker = Linker.create(runner.getEngine());
      linker.defineTable(runner.getStore(), "mod1", "shared_table", sharedTable);

      // Compile and instantiate module 2 using the linker
      final Module module2 = runner.getEngine().compileWat(wat2);
      final Instance instance2 = linker.instantiate(runner.getStore(), module2);

      // Call function at index 0 (add_42) with value 10
      WasmValue[] results =
          instance2.callFunction("call_table", WasmValue.i32(0), WasmValue.i32(10));
      assertEquals(1, results.length, "Should return one value");
      assertEquals(52, results[0].asInt(), "add_42(10) should return 52");

      // Call function at index 1 (mul_2) with value 10
      results = instance2.callFunction("call_table", WasmValue.i32(1), WasmValue.i32(10));
      assertEquals(1, results.length, "Should return one value");
      assertEquals(20, results[0].asInt(), "mul_2(10) should return 20");

      // Cleanup
      instance2.close();
      linker.close();
      instance1.close();
      module2.close();
      module1.close();
    }
  }
}

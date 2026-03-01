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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Equivalent Java test for Wasmtime test:
 * func::call_indirect_native_from_wasm_import_func_returns_funcref
 *
 * <p>Original source: func.rs:302 Category: func
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case. Tests calling a native function via call_indirect where the
 * funcref is returned from an imported host function.
 *
 * <p>Note: This test requires funcref return values from host functions, which requires FUNCREF
 * type support in WasmValue marshalling.
 */
public final class CallIndirectNativeFromWasmImportFuncReturnsFuncrefTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(CallIndirectNativeFromWasmImportFuncReturnsFuncrefTest.class.getName());

  @ParameterizedTest(name = "{0}")
  @EnumSource(RuntimeType.class)
  @DisplayName("func::call_indirect_native_from_wasm_import_func_returns_funcref")
  public void testCallIndirectNativeFromWasmImportFuncReturnsFuncref(final RuntimeType runtime)
      throws Exception {
    setRuntime(runtime);

    LOGGER.info("Testing funcref return from host function with runtime: " + runtime);

    // Original WAT from the Wasmtime test:
    // (module
    //   (import "" "" (func (result funcref)))
    //   (table 1 1 funcref)
    //   (func (export "run") (result i32 i32 i32)
    //     i32.const 0
    //     call 0         ;; calls imported host function, gets funcref on stack
    //     table.set      ;; sets the funcref in table at index 0
    //     i32.const 0
    //     call_indirect (result i32 i32 i32)  ;; calls through table
    //   )
    // )

    // WAT module that imports a function returning funcref, stores it in a table,
    // and calls it via call_indirect
    final String wat =
        """
        (module
          (type $target_type (func (result i32 i32 i32)))
          (import "" "" (func $get_funcref (result funcref)))
          (table $t 1 1 funcref)
          (func (export "run") (result i32 i32 i32)
            i32.const 0
            call $get_funcref
            table.set $t
            i32.const 0
            call_indirect $t (type $target_type)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      try (final Store store = engine.createStore();
          final Linker<Void> linker = Linker.create(engine)) {

        // Create the target function that returns (10, 20, 30)
        final HostFunction targetFunc =
            params -> {
              LOGGER.fine("Target function called, returning (10, 20, 30)");
              return new WasmValue[] {WasmValue.i32(10), WasmValue.i32(20), WasmValue.i32(30)};
            };

        final FunctionType targetFuncType =
            new FunctionType(
                new WasmValueType[] {},
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32, WasmValueType.I32});

        // Create a FunctionReference for the target function
        final FunctionReference targetFuncRef =
            store.createFunctionReference(targetFunc, targetFuncType);
        assertNotNull(targetFuncRef, "Target function reference should not be null");
        LOGGER.info("Created target function reference with ID: " + targetFuncRef.getId());

        // Create the host function that returns the funcref
        final HostFunction getFuncrefFunc =
            params -> {
              LOGGER.fine("get_funcref called, returning funcref ID: " + targetFuncRef.getId());
              return new WasmValue[] {WasmValue.funcref(targetFuncRef)};
            };

        final FunctionType getFuncrefType =
            new FunctionType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.FUNCREF});

        // Define the host function as import "" ""
        linker.defineHostFunction("", "", getFuncrefType, getFuncrefFunc);
        LOGGER.info("Defined host function returning funcref");

        // Compile and instantiate the module
        final Module module = engine.compileWat(wat);
        final Instance instance = linker.instantiate(store, module);
        assertNotNull(instance, "Instance should not be null");
        LOGGER.info("Module instantiated successfully");

        // Call "run" function
        final WasmValue[] results = instance.callFunction("run");

        // Verify results are (10, 20, 30)
        assertNotNull(results, "Results should not be null");
        assertEquals(3, results.length, "Should have 3 results");
        assertEquals(10, results[0].asInt(), "First result should be 10");
        assertEquals(20, results[1].asInt(), "Second result should be 20");
        assertEquals(30, results[2].asInt(), "Third result should be 30");

        LOGGER.info(
            "Test passed: funcref returned from host function, called via call_indirect returned ("
                + results[0].asInt()
                + ", "
                + results[1].asInt()
                + ", "
                + results[2].asInt()
                + ")");

        module.close();
      }
    }
  }
}

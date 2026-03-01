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

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Equivalent Java test for Wasmtime test: func::call_indirect_native_from_exported_global
 *
 * <p>Original source: func.rs:360 Category: func
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class CallIndirectNativeFromExportedGlobalTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::call_indirect_native_from_exported_global")
  public void testCallIndirectNativeFromExportedGlobal(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    // WAT module that exports a mutable funcref global and calls it indirectly
    final String wat =
        """
            (module
              (global (export "global") (mut funcref) (ref.null func))
              (table 1 1 funcref)
              (func (export "run") (result i32 i32 i32)
                i32.const 0
                global.get 0
                table.set
                i32.const 0
                call_indirect (result i32 i32 i32)
              )
            )
        """;

    try (final ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner runner =
        new ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner()) {

      // Compile and instantiate the module
      final ai.tegmentum.wasmtime4j.Instance instance = runner.compileAndInstantiate(wat);

      // Get the exported global
      final ai.tegmentum.wasmtime4j.WasmGlobal global =
          instance
              .getGlobal("global")
              .orElseThrow(() -> new AssertionError("Global 'global' not found"));

      // Verify global is initially null
      org.junit.jupiter.api.Assertions.assertNull(
          global.get().asFuncref(), "Global should be null initially");

      // Create a host function that returns (10, 20, 30)
      final ai.tegmentum.wasmtime4j.type.FunctionType funcType =
          new ai.tegmentum.wasmtime4j.type.FunctionType(
              new ai.tegmentum.wasmtime4j.WasmValueType[] {},
              new ai.tegmentum.wasmtime4j.WasmValueType[] {
                ai.tegmentum.wasmtime4j.WasmValueType.I32,
                ai.tegmentum.wasmtime4j.WasmValueType.I32,
                ai.tegmentum.wasmtime4j.WasmValueType.I32
              });

      final ai.tegmentum.wasmtime4j.func.HostFunction hostFunc =
          (args) ->
              new ai.tegmentum.wasmtime4j.WasmValue[] {
                ai.tegmentum.wasmtime4j.WasmValue.i32(10),
                ai.tegmentum.wasmtime4j.WasmValue.i32(20),
                ai.tegmentum.wasmtime4j.WasmValue.i32(30)
              };

      // Create a function reference from the host function
      final ai.tegmentum.wasmtime4j.func.FunctionReference funcRef =
          runner.getStore().createFunctionReference(hostFunc, funcType);

      // Set the global to point to our host function via funcref
      global.set(ai.tegmentum.wasmtime4j.WasmValue.funcref(funcRef));

      // Call the exported "run" function
      final ai.tegmentum.wasmtime4j.WasmValue[] results = runner.invoke("run");

      org.junit.jupiter.api.Assertions.assertEquals(3, results.length, "Should return 3 values");
      org.junit.jupiter.api.Assertions.assertEquals(
          10, results[0].asInt(), "First result should be 10");
      org.junit.jupiter.api.Assertions.assertEquals(
          20, results[1].asInt(), "Second result should be 20");
      org.junit.jupiter.api.Assertions.assertEquals(
          30, results[2].asInt(), "Third result should be 30");
    }
  }
}

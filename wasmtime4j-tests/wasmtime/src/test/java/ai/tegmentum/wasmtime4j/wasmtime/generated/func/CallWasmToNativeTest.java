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

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: func::call_wasm_to_native
 *
 * <p>Original source: func.rs:39 Category: func
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class CallWasmToNativeTest {

  @Test
  @DisplayName("func::call_wasm_to_native")
  public void testCallWasmToNative() throws Exception {
    // WAT code from original Wasmtime test:
    // (module
    //             (import "" "" (func (result i32 i32 i32)))
    //             (func (export "run") (result i32 i32 i32)
    //                 call 0
    //             )
    //           )

    final String wat =
        """
            (module
                        (import "host" "triple" (func (result i32 i32 i32)))
                        (func (export "run") (result i32 i32 i32)
                            call 0
                        )
                      )
        """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Define host function that returns (1, 2, 3)
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {}, // No parameters
              new WasmValueType[] {
                WasmValueType.I32, WasmValueType.I32, WasmValueType.I32
              } // Three i32 returns
              );

      runner.defineHostFunction(
          "host", // Module name
          "triple", // Function name
          funcType,
          (params) -> {
            // Return (1, 2, 3)
            return new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3)};
          });

      // Compile and instantiate module with host import
      runner.compileAndInstantiate(wat);

      // Call the exported "run" function which calls the host function
      // Expected results: (1, 2, 3)
      runner.assertReturn(
          "run", new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3)});
    }
  }
}

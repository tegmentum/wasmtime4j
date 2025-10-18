package ai.tegmentum.wasmtime4j.comparison.generated.func;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: func::call_wasm_to_array
 *
 * <p>Original source: func.rs:65 Category: func
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class CallWasmToArrayTest {

  @Test
  @DisplayName("func::call_wasm_to_array")
  public void testCallWasmToArray() throws Exception {
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
                    (import "host" "array" (func (result i32 i32 i32)))
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
          "array", // Function name
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

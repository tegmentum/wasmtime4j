package ai.tegmentum.wasmtime4j.comparison.generated.func;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: func::call_wasm_to_wasm
 *
 * <p>Original source: func.rs:10 Category: func
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class CallWasmToWasmTest {

  @Test
  @DisplayName("func::call_wasm_to_wasm")
  public void testCallWasmToWasm() throws Exception {
    // WAT code from original Wasmtime test:
    // (module
    //             (func (result i32 i32 i32)
    //               i32.const 1
    //               i32.const 2
    //               i32.const 3
    //             )
    //             (func (export "run") (result i32 i32 i32)
    //                 call 0
    //             )
    //           )

    final String wat =
        """
        (module
          (func (result i32 i32 i32)
            i32.const 1
            i32.const 2
            i32.const 3
          )
          (func (export "run") (result i32 i32 i32)
            call 0
          )
        )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      runner.compileAndInstantiate(wat);

      // Call exported function and verify results: (1, 2, 3)
      runner.assertReturn(
          "run",
          new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3)});
    }
  }
}

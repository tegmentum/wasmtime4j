package ai.tegmentum.wasmtime4j.comparison.generated.func;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: func::call_array_to_wasm
 *
 * <p>Original source: func.rs:162 Category: func
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class CallArrayToWasmTest {

  @Test
  @DisplayName("func::call_array_to_wasm")
  public void testCallArrayToWasm() throws Exception {
    // WAT code from original Wasmtime test:
    // (module
    //             (func (export "run") (param i32 i32 i32) (result i32 i32 i32)
    //               local.get 1
    //               local.get 2
    //               local.get 0
    //             )
    //           )

    final String wat =
        """
        (module
                    (func (export "run") (param i32 i32 i32) (result i32 i32 i32)
                      local.get 1
                      local.get 2
                      local.get 0
                    )
                  )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Compile and instantiate module
      runner.compileAndInstantiate(wat);

      // Function takes params (0, 1, 2) and returns (1, 2, 0)
      // Testing with input (100, 200, 300) should return (200, 300, 100)
      runner.assertReturn(
          "run",
          new WasmValue[] {WasmValue.i32(200), WasmValue.i32(300), WasmValue.i32(100)},
          WasmValue.i32(100),
          WasmValue.i32(200),
          WasmValue.i32(300));
    }
  }
}

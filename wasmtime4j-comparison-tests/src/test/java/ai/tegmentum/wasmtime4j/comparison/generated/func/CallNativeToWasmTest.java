package ai.tegmentum.wasmtime4j.comparison.generated.func;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: func::call_native_to_wasm
 *
 * <p>Original source: func.rs:101 Category: func
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class CallNativeToWasmTest {

  @Test
  @DisplayName("func::call_native_to_wasm")
  public void testCallNativeToWasm() throws Exception {
    // WAT code from original Wasmtime test:
    // (module
    //             (func (export "run") (result i32 i32 i32)
    //                 i32.const 42
    //                 i32.const 420
    //                 i32.const 4200
    //             )
    //           )

    final String wat =
        """
        (module
                    (func (export "run") (result i32 i32 i32)
                        i32.const 42
                        i32.const 420
                        i32.const 4200
                    )
                  )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Compile and instantiate module
      runner.compileAndInstantiate(wat);

      // Expected results: (42, 420, 4200)
      runner.assertReturn(
          "run",
          new WasmValue[] {WasmValue.i32(42), WasmValue.i32(420), WasmValue.i32(4200)});
    }
  }
}

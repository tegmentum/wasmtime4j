package ai.tegmentum.wasmtime4j.comparison.generated.hostfuncs;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: host_funcs::call_wasm_many_args
 *
 * <p>Original source: host_funcs.rs:385 Category: host_funcs
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class CallWasmManyArgsTest {

  @Test
  @DisplayName("host_funcs::call_wasm_many_args")
  public void testCallWasmManyArgs() throws Exception {
    // WAT code from original Wasmtime test:
    // (func (export "run") (param i32 i32 i32 i32 i32 i32 i32 i32 i32 i32)
    //                 i32.const 1
    //                 local.get 0
    //                 i32.ne
    //                 if
    //                     unreachable
    //                 end
    //
    //                 i32.const 10
    //                 local.get 9
    //                 i32.ne
    //                 if
    //                     unreachable
    //                 end
    //             )
    //
    //             (func (export "test")
    //                 i32.const 1
    //                 i32.const 2
    //                 i32.const 3
    //                 i32.const 4
    //                 i32.const 5
    //                 i32.const 6
    //                 i32.const 7
    //                 i32.const 8
    //                 i32.const 9
    //                 i32.const 10
    //                 call 0
    //             )

    final String wat =
        """
        (module
          (func (export "run") (param i32 i32 i32 i32 i32 i32 i32 i32 i32 i32)
            i32.const 1
            local.get 0
            i32.ne
            if
              unreachable
            end

            i32.const 10
            local.get 9
            i32.ne
            if
              unreachable
            end
          )

          (func (export "test")
            i32.const 1
            i32.const 2
            i32.const 3
            i32.const 4
            i32.const 5
            i32.const 6
            i32.const 7
            i32.const 8
            i32.const 9
            i32.const 10
            call 0
          )
        )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      runner.compileAndInstantiate(wat);

      // Test calling function with 10 arguments (1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
      // The "run" function validates that arg[0] == 1 and arg[9] == 10
      // If validation fails, it executes unreachable (trap)
      runner.assertReturn(
          "run",
          new WasmValue[] {},
          WasmValue.i32(1),
          WasmValue.i32(2),
          WasmValue.i32(3),
          WasmValue.i32(4),
          WasmValue.i32(5),
          WasmValue.i32(6),
          WasmValue.i32(7),
          WasmValue.i32(8),
          WasmValue.i32(9),
          WasmValue.i32(10));

      // Test the "test" function which calls "run" with the correct arguments
      runner.assertReturn("test", new WasmValue[] {});
    }
  }
}

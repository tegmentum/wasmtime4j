package ai.tegmentum.wasmtime4j.comparison.generated.hostfuncs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: host_funcs::call_import_many_args
 *
 * <p>Original source: host_funcs.rs:325 Category: host_funcs
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class CallImportManyArgsTest {

  @Test
  @DisplayName("host_funcs::call_import_many_args")
  public void testCallImportManyArgs() throws Exception {
    // WAT code from original Wasmtime test:
    // (import "" "host" (func (param i32 i32 i32 i32 i32 i32 i32 i32 i32 i32)))
    //             (func (export "run")
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
          (import "host" "validate" (func (param i32 i32 i32 i32 i32 i32 i32 i32 i32 i32)))
          (func (export "run")
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
      // Define the function type for host function with 10 i32 parameters and no returns
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {
                WasmValueType.I32,
                WasmValueType.I32,
                WasmValueType.I32,
                WasmValueType.I32,
                WasmValueType.I32,
                WasmValueType.I32,
                WasmValueType.I32,
                WasmValueType.I32,
                WasmValueType.I32,
                WasmValueType.I32
              },
              new WasmValueType[] {});

      // Track how many times host function is called and validate parameters
      final AtomicInteger callCount = new AtomicInteger(0);

      // Define host function that validates all 10 parameters
      runner.defineHostFunction(
          "host",
          "validate",
          funcType,
          (params) -> {
            callCount.incrementAndGet();

            // Validate that we received exactly 10 parameters
            assertEquals(10, params.length, "Expected 10 parameters");

            // Validate each parameter matches expected value (1 through 10)
            for (int i = 0; i < 10; i++) {
              assertEquals(
                  i + 1,
                  params[i].asInt(),
                  "Parameter " + (i + 1) + " should be " + (i + 1));
            }

            return new WasmValue[] {};
          });

      runner.compileAndInstantiate(wat);

      // Call the run function which should invoke the host function with 10 parameters
      runner.assertReturn("run", new WasmValue[] {});

      // Verify the host function was called exactly once
      assertEquals(1, callCount.get(), "Host function should be called exactly once");
    }
  }
}

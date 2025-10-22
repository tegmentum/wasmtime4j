package ai.tegmentum.wasmtime4j.comparison.generated.misctestsuite;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: misc_testsuite::float-comparison
 *
 * <p>Original source: float-comparison.wast:1 Category: misc_testsuite
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class FloatComparisonTest {

  @Test
  @DisplayName("misc_testsuite::float-comparison")
  public void testFloatComparison() throws Exception {
    // WAT code from original Wasmtime test:
    // (module
    //   (func (result i32 i32 i32)
    //     i32.const 1
    //     i32.eqz
    //     f64.const 0
    //     f64.const 1
    //     f64.ne
    //     i32.const 1111
    //   )
    //   (export "d" (func 0))
    // )
    //
    // (assert_return (invoke "d") (i32.const 0) (i32.const 1) (i32.const 1111))

    final String wat =
        """
        (module
          (func (result i32 i32 i32)
            i32.const 1
            i32.eqz
            f64.const 0
            f64.const 1
            f64.ne
            i32.const 1111
          )
          (export "d" (func 0))
        )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Compile and instantiate module
      runner.compileAndInstantiate(wat);

      // Execute assertion: (assert_return (invoke "d") (i32.const 0) (i32.const 1) (i32.const
      // 1111))
      runner.assertReturn(
          "d", new WasmValue[] {WasmValue.i32(0), WasmValue.i32(1), WasmValue.i32(1111)});
    }
  }
}

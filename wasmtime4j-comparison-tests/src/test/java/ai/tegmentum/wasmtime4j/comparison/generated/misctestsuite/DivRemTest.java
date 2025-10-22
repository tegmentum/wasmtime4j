package ai.tegmentum.wasmtime4j.comparison.generated.misctestsuite;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: misc_testsuite::div-rem
 *
 * <p>Original source: div-rem.wast:1 Category: misc_testsuite
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class DivRemTest {

  @Test
  @DisplayName("misc_testsuite::div-rem")
  public void testDivRem() throws Exception {
    // WAT code from original Wasmtime test:
    // (module
    //   (func (export "i32.div_s") (param i32) (param i32) (result i32)
    //     (i32.div_s (local.get 0) (local.get 1))
    //   )
    // )
    //
    // (assert_return (invoke "i32.div_s" (i32.const -1) (i32.const -1)) (i32.const 1))
    //
    // (module
    //   (func (export "i32.rem_s") (param i32) (param i32) (result i32)
    //     (i32.rem_s (local.get 0) (local.get 1))
    //   )
    // )
    //
    // (assert_return (invoke "i32.rem_s" (i32.const 123121) (i32.const -1)) (i32.const 0))
    //
    // (module
    //   (func (export "i64.div_s") (param i64) (param i64) (result i64)
    //     (i64.div_s (local.get 0) (local.get 1))
    //   )
    // )
    //
    // (assert_return (invoke "i64.div_s" (i64.const -1) (i64.const -1)) (i64.const 1))
    //
    // (module
    //   (func (export "i64.rem_s") (param i64) (param i64) (result i64)
    //     (i64.rem_s (local.get 0) (local.get 1))
    //   )
    // )
    //
    // (assert_return (invoke "i64.rem_s" (i64.const 123121) (i64.const -1)) (i64.const 0))

    // Test i32.div_s: -1 / -1 = 1
    final String wat1 =
        """
        (module
          (func (export "i32.div_s") (param i32) (param i32) (result i32)
            (i32.div_s (local.get 0) (local.get 1))
          )
        )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      runner.compileAndInstantiate(wat1);
      runner.assertReturn(
          "i32.div_s", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(-1), WasmValue.i32(-1));
    }

    // Test i32.rem_s: 123121 % -1 = 0
    final String wat2 =
        """
        (module
          (func (export "i32.rem_s") (param i32) (param i32) (result i32)
            (i32.rem_s (local.get 0) (local.get 1))
          )
        )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      runner.compileAndInstantiate(wat2);
      runner.assertReturn(
          "i32.rem_s",
          new WasmValue[] {WasmValue.i32(0)},
          WasmValue.i32(123121),
          WasmValue.i32(-1));
    }

    // Test i64.div_s: -1 / -1 = 1
    final String wat3 =
        """
        (module
          (func (export "i64.div_s") (param i64) (param i64) (result i64)
            (i64.div_s (local.get 0) (local.get 1))
          )
        )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      runner.compileAndInstantiate(wat3);
      runner.assertReturn(
          "i64.div_s", new WasmValue[] {WasmValue.i64(1L)}, WasmValue.i64(-1L), WasmValue.i64(-1L));
    }

    // Test i64.rem_s: 123121 % -1 = 0
    final String wat4 =
        """
        (module
          (func (export "i64.rem_s") (param i64) (param i64) (result i64)
            (i64.rem_s (local.get 0) (local.get 1))
          )
        )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      runner.compileAndInstantiate(wat4);
      runner.assertReturn(
          "i64.rem_s",
          new WasmValue[] {WasmValue.i64(0L)},
          WasmValue.i64(123121L),
          WasmValue.i64(-1L));
    }
  }
}

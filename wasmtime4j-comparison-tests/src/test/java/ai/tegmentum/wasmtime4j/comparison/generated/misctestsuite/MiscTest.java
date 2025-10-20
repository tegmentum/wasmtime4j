package ai.tegmentum.wasmtime4j.comparison.generated.misctestsuite;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: misc_testsuite::misc
 *
 * <p>Original source: misc.wast:1 Category: misc_testsuite
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class MiscTest {

  @Test
  @DisplayName("misc_testsuite::misc")
  public void testMisc() throws Exception {
    // WAT code from original Wasmtime test:
    // ;; Additional run tests for Winch not covered in the official spec test suite.
    //
    // (module
    //   (func (export "br-table-ensure-sp") (result i32)
    //     (block (result i32)
    //        (i32.const 0)
    //     )
    //     (i32.const 0)
    //     (i32.const 0)
    //     (br_table 0)
    //   )
    // )
    //
    // (assert_return (invoke "br-table-ensure-sp") (i32.const 0))

    final String wat =
        """
        (module
          (func (export "br-table-ensure-sp") (result i32)
            (block (result i32)
               (i32.const 0)
            )
            (i32.const 0)
            (i32.const 0)
            (br_table 0)
          )
        )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      runner.compileAndInstantiate(wat);

      // Test br_table instruction ensuring stack pointer is correct
      // The function should return 0
      runner.assertReturn("br-table-ensure-sp", new WasmValue[] {WasmValue.i32(0)});
    }
  }
}

package ai.tegmentum.wasmtime4j.comparison.generated.traps;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: traps::trap_start_function_import
 *
 * <p>Original source: traps.rs:373 Category: traps
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class TrapStartFunctionImportTest {

  @Test
  @DisplayName("traps::trap_start_function_import")
  public void testTrapStartFunctionImport() throws Exception {
    // WAT code from original Wasmtime test:
    // (module $a
    //                 (import "" "" (func $foo))
    //                 (start $foo)
    //             )

    final String wat =
        """
        (module $a
          (import "host" "trap" (func $foo))
          (start $foo)
        )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Define the function type for host function with no parameters and no returns
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});

      // Define host function that always traps
      runner.defineHostFunction(
          "host",
          "trap",
          funcType,
          (params) -> {
            throw new WasmException("Host function trap");
          });

      // Module should fail to instantiate because the start function traps
      runner.assertUnlinkable(wat, null);
    }
  }
}

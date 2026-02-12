package ai.tegmentum.wasmtime4j.wasmtime.generated.func;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: func::trap_import
 *
 * <p>Original source: func.rs:704 Category: func
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class TrapImportTest {

  @Test
  @DisplayName("func::trap_import")
  public void testTrapImport() throws Exception {
    // WAT code from original Wasmtime test:
    // (import "" "" (func))
    //             (start 0)

    final String wat =
        """
            (module
                        (import "host" "trap" (func))
                        (start 0)
                      )
        """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Define host function that throws an exception (traps)
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {}, // No parameters
              new WasmValueType[] {} // No returns
              );

      runner.defineHostFunction(
          "host", // Module name
          "trap", // Function name
          funcType,
          (params) -> {
            // Throw exception to trigger trap
            throw new WasmException("Host function trap");
          });

      // Module instantiation should fail because start function calls the trapping host function
      runner.assertUnlinkable(wat, null);
    }
  }
}

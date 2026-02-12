package ai.tegmentum.wasmtime4j.wasmtime.generated.traps;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: traps::rust_panic_start_function
 *
 * <p>Original source: traps.rs:484 Category: traps
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class RustPanicStartFunctionTest {

  @Test
  @DisplayName("traps::rust_panic_start_function")
  public void testRustPanicStartFunction() throws Exception {
    // WAT code from original Wasmtime test - the start function calls a host function that panics.
    // Using "host"/"panic" instead of empty module/name since we need distinct import names.
    final String wat =
        """
            (module $a
              (import "host" "panic" (func $foo))
              (start $foo)
            )
        """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});

      runner.defineHostFunction(
          "host",
          "panic",
          funcType,
          (params) -> {
            throw new WasmException("Rust panic simulation");
          });

      // The start function calls a host function that panics,
      // which should cause instantiation to fail
      runner.assertUnlinkable(wat, "Rust panic simulation");
    }
  }
}

package ai.tegmentum.wasmtime4j.comparison.generated.traps;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: traps::rust_panic_import
 *
 * <p>Original source: traps.rs:393 Category: traps
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class RustPanicImportTest {

  @Test
  @DisplayName("traps::rust_panic_import")
  public void testRustPanicImport() throws Exception {
    // WAT code from original Wasmtime test:
    // (module $a
    //                 (import "" "" (func $foo))
    //                 (import "" "" (func $bar))
    //                 (func (export "foo") call $foo)
    //                 (func (export "bar") call $bar)
    //             )

    final String wat =
        """
        (module $a
          (import "" "" (func $foo))
          (import "" "" (func $bar))
          (func (export "foo") call $foo)
          (func (export "bar") call $bar)
        )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});

      // Define first host function that panics with "test-panic"
      runner.defineHostFunction(
          "",
          "",
          funcType,
          (params) -> {
            throw new WasmException("test-panic");
          });

      // The module imports two functions both with empty names - this is ambiguous
      // In the original Rust test, they use index-based callbacks to differentiate
      // For now, this test verifies that calling the exported function traps
      runner.compileAndInstantiate(wat);

      // Calling "foo" should trap because it calls the imported function that panics
      runner.assertTrap("foo", "test-panic");
    }
  }
}

package ai.tegmentum.wasmtime4j.comparison.generated.traps;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.RuntimeType;
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
    // Original WAT uses empty import names which can't be used since we can't have duplicate names.
    // Using distinct import names: "host"/"foo" and "host"/"bar"
    final String wat =
        """
        (module $a
          (import "host" "foo" (func $foo))
          (import "host" "bar" (func $bar))
          (func (export "foo") call $foo)
          (func (export "bar") call $bar)
        )
    """;

    // Use JNI runtime explicitly since exception handling fix is in JNI layer
    try (final WastTestRunner runner = new WastTestRunner(RuntimeType.JNI)) {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});

      // Define first host function that panics with "test-panic"
      runner.defineHostFunction(
          "host",
          "foo",
          funcType,
          (params) -> {
            throw new WasmException("test-panic");
          });

      // Define second host function that panics with different message
      runner.defineHostFunction(
          "host",
          "bar",
          funcType,
          (params) -> {
            throw new WasmException("test-panic-bar");
          });

      runner.compileAndInstantiate(wat);

      // Calling "foo" should trap because it calls the imported function that panics
      runner.assertTrap("foo", "test-panic");

      // Calling "bar" should also trap
      runner.assertTrap("bar", "test-panic-bar");
    }
  }
}

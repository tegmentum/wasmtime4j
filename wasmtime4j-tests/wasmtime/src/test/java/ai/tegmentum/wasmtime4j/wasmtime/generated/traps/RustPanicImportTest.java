package ai.tegmentum.wasmtime4j.wasmtime.generated.traps;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
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
  @DisplayName("traps::rust_panic_import_jni")
  public void testRustPanicImportJni() throws Exception {
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

  @Test
  @DisplayName("traps::rust_panic_import_panama")
  public void testRustPanicImportPanama() throws Exception {
    System.out.println("[TEST] Starting Panama test");
    System.out.flush();

    // Test exception handling with Panama runtime
    final String wat =
        """
        (module $a
          (import "host" "foo" (func $foo))
          (import "host" "bar" (func $bar))
          (func (export "foo") call $foo)
          (func (export "bar") call $bar)
        )
    """;

    // Use Panama runtime explicitly to test exception handling fix
    System.out.println("[TEST] Creating WastTestRunner with PANAMA runtime");
    System.out.flush();
    try (final WastTestRunner runner = new WastTestRunner(RuntimeType.PANAMA)) {
      System.out.println("[TEST] WastTestRunner created");
      System.out.flush();

      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});

      // Define first host function that panics with "test-panic"
      System.out.println("[TEST] Defining host function 'foo'");
      System.out.flush();
      runner.defineHostFunction(
          "host",
          "foo",
          funcType,
          (params) -> {
            System.out.println("[TEST] Host function 'foo' called - about to throw");
            System.out.flush();
            throw new WasmException("test-panic");
          });

      // Define second host function that panics with different message
      System.out.println("[TEST] Defining host function 'bar'");
      System.out.flush();
      runner.defineHostFunction(
          "host",
          "bar",
          funcType,
          (params) -> {
            System.out.println("[TEST] Host function 'bar' called - about to throw");
            System.out.flush();
            throw new WasmException("test-panic-bar");
          });

      System.out.println("[TEST] Compiling and instantiating module");
      System.out.flush();
      runner.compileAndInstantiate(wat);
      System.out.println("[TEST] Module instantiated");
      System.out.flush();

      // Calling "foo" should trap because it calls the imported function that panics
      System.out.println("[TEST] Calling assertTrap for 'foo'");
      System.out.flush();
      runner.assertTrap("foo", "test-panic");
      System.out.println("[TEST] assertTrap 'foo' completed");
      System.out.flush();

      // Calling "bar" should also trap
      System.out.println("[TEST] Calling assertTrap for 'bar'");
      System.out.flush();
      runner.assertTrap("bar", "test-panic-bar");
      System.out.println("[TEST] assertTrap 'bar' completed");
      System.out.flush();
    }
    System.out.println("[TEST] Test completed successfully");
    System.out.flush();
  }
}

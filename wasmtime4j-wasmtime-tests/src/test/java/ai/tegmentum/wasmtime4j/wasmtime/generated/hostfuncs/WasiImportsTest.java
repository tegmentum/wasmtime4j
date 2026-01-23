package ai.tegmentum.wasmtime4j.wasmtime.generated.hostfuncs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: host_funcs::wasi_imports
 *
 * <p>Original source: host_funcs.rs:716 Category: host_funcs
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class WasiImportsTest {

  /** Custom exception to simulate WASI proc_exit behavior. */
  private static class ProcExitException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int exitCode;

    ProcExitException(final int exitCode) {
      super("proc_exit called with code: " + exitCode);
      this.exitCode = exitCode;
    }

    int getExitCode() {
      return exitCode;
    }
  }

  @Test
  @DisplayName("host_funcs::wasi_imports")
  public void testWasiImports() throws Exception {
    // Track the exit code passed to proc_exit
    final AtomicInteger exitCode = new AtomicInteger(-1);

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Define a mock WASI proc_exit function
      // In the original Wasmtime test, proc_exit causes the runtime to exit with the given code
      runner.defineHostFunction(
          "wasi_snapshot_preview1",
          "proc_exit",
          FunctionType.of(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {}),
          (args) -> {
            exitCode.set(args[0].asI32());
            // proc_exit should terminate execution, so we throw an exception
            throw new ProcExitException(exitCode.get());
          });

      // The WAT module calls proc_exit with exit code 123
      final String wat =
          "(module "
              + "(import \"wasi_snapshot_preview1\" \"proc_exit\" "
              + "  (func $__wasi_proc_exit (param i32))) "
              + "(memory (export \"memory\") 0) "
              + "(func (export \"_start\") "
              + "  (call $__wasi_proc_exit (i32.const 123)) "
              + ")"
              + ")";

      runner.compileAndInstantiate(wat);

      // Calling _start should result in proc_exit being called
      // The host function exception gets wrapped in a WasmException by the runtime,
      // so we need to catch the generic Exception and verify the error message
      try {
        runner.invoke("_start");
        fail("_start should call proc_exit which throws an exception");
      } catch (final Exception e) {
        // Verify the exit code was captured
        assertEquals(123, exitCode.get(), "proc_exit should be called with exit code 123");

        // Verify the exception message contains the expected text
        assertTrue(
            e.getMessage().contains("proc_exit called with code: 123"),
            "Exception message should contain proc_exit message, got: " + e.getMessage());
      }
    }
  }
}

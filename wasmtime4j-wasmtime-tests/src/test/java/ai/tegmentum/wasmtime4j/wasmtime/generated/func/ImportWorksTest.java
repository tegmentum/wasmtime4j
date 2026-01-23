package ai.tegmentum.wasmtime4j.wasmtime.generated.func;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: func::import_works
 *
 * <p>Original source: func.rs:560 Category: func
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case. Tests importing multiple host functions with different
 * signatures and calling them from WebAssembly.
 *
 * <p>Note: This test uses advanced WebAssembly GC types (externref, funcref, anyref, i31ref) which
 * require the GC proposal to be enabled. It is disabled until full GC support is implemented.
 */
public final class ImportWorksTest {

  @Test
  @Disabled(
      "Requires WebAssembly GC proposal types (externref, funcref, anyref, i31ref) - not yet"
          + " implemented")
  @DisplayName("func::import_works - full test with GC types")
  public void testImportWorksWithGcTypes() throws Exception {
    // This test requires GC types that are not yet supported
    // Keeping as placeholder for when GC support is added
  }

  @Test
  @DisplayName("func::import_works - basic imports without GC types")
  public void testImportWorksBasic() throws Exception {
    // Track host function invocations
    final AtomicInteger hits = new AtomicInteger(0);

    // Simplified WAT that tests basic import functionality without GC types
    final String wat =
        """
        (module
          (import "test" "func0" (func))
          (import "test" "func1" (func (param i32) (result i32)))
          (import "test" "func2" (func (param i32) (param i64)))

          (func (export "run") (result i32)
            ;; Call func0 (no params, no results)
            call 0

            ;; Call func1 with 0, expect result + 1 = 1, add 1 = 2
            i32.const 0
            call 1
            i32.const 1
            i32.add

            ;; Call func2 with separate values (doesn't affect stack result)
            i32.const 3
            i64.const 4
            call 2

            ;; The stack still has the result from func1 + 1 = 2
            ;; which is what we return
          )
        )
        """;

    // Function type for func0: () -> ()
    final FunctionType func0Type = new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});

    // Function type for func1: (i32) -> (i32)
    final FunctionType func1Type =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

    // Function type for func2: (i32, i64) -> ()
    final FunctionType func2Type =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I64}, new WasmValueType[] {});

    // Host function implementations
    final HostFunction hostFunc0 =
        HostFunction.voidFunction(
            (params) -> {
              hits.incrementAndGet();
            });

    final HostFunction hostFunc1 =
        HostFunction.singleValue(
            (params) -> {
              hits.incrementAndGet();
              final int x = params[0].asInt();
              // Return x + 1
              return WasmValue.i32(x + 1);
            });

    final HostFunction hostFunc2 =
        HostFunction.voidFunction(
            (params) -> {
              hits.incrementAndGet();
              final int a = params[0].asInt();
              final long b = params[1].asLong();
              // Just verify we received the expected values
              assertEquals(3, a, "First param should be 3");
              assertEquals(4L, b, "Second param should be 4");
            });

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Define all host functions
      runner.defineHostFunction("test", "func0", func0Type, hostFunc0);
      runner.defineHostFunction("test", "func1", func1Type, hostFunc1);
      runner.defineHostFunction("test", "func2", func2Type, hostFunc2);

      // Compile and instantiate
      runner.compileAndInstantiate(wat);

      // Verify no functions called yet
      assertEquals(0, hits.get(), "No functions should be called before run()");

      // Call the exported run function
      final WasmValue[] results = runner.invoke("run");

      // Verify all three host functions were called
      assertEquals(3, hits.get(), "All three host functions should have been called");

      // Verify the result
      assertEquals(1, results.length, "Should return one value");
      assertEquals(2, results[0].asInt(), "Result should be 2");
    }
  }
}

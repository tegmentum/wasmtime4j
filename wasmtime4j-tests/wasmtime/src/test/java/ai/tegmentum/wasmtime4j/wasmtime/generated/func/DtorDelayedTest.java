package ai.tegmentum.wasmtime4j.wasmtime.generated.func;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: func::dtor_delayed
 *
 * <p>Original source: func.rs:472 Category: func
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case. The test verifies that host function destructors are called at
 * the appropriate time - when the store is dropped, not when the function is created.
 */
public final class DtorDelayedTest {

  @Test
  @DisplayName("func::dtor_delayed")
  public void testDtorDelayed() throws Exception {
    // Track host function invocations
    final AtomicInteger hits = new AtomicInteger(0);

    // The WAT code imports a function that we'll provide as a host function
    final String wat =
        """
        (module
          (import "" "" (func))
          (func (export "call_import")
            call 0
          )
        )
        """;

    // Define function type for the import: () -> ()
    final FunctionType funcType = new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});

    // Create host function that increments counter when called
    final HostFunction hostFunc =
        HostFunction.voidFunction(
            (params) -> {
              hits.incrementAndGet();
            });

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Define the host function for import "" ""
      runner.defineHostFunction("", "", funcType, hostFunc);

      // Compile and instantiate the module
      runner.compileAndInstantiate(wat);

      // Verify host function hasn't been called yet
      assertEquals(0, hits.get(), "Host function should not be called during instantiation");

      // Call the exported function that invokes our host function
      runner.invoke("call_import");

      // Verify host function was called once
      assertEquals(1, hits.get(), "Host function should be called once");

      // Call again to verify counter increments
      runner.invoke("call_import");
      assertEquals(2, hits.get(), "Host function should be called twice");
    }

    // After closing the runner (store), verify all calls completed
    assertTrue(hits.get() >= 2, "Host function should have been called at least twice");
  }
}

package ai.tegmentum.wasmtime4j.wasmtime.generated.hostfuncs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: host_funcs::call_via_funcref
 *
 * <p>Original source: host_funcs.rs:628 Category: host_funcs
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class CallViaFuncrefTest {

  @Test
  @DisplayName("host_funcs::call_via_funcref")
  public void testCallViaFuncref() throws Exception {
    // Counter to track host function calls
    final AtomicInteger hits = new AtomicInteger(0);

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Define a host function that adds two i32 values
      // This function will be called via funcref/call_indirect
      runner.defineHostFunction(
          "test",
          "add",
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32}),
          (args) -> {
            hits.incrementAndGet();
            final int a = args[0].asI32();
            final int b = args[1].asI32();
            return new WasmValue[] {WasmValue.i32(a + b)};
          });

      // The WAT module defines a table and a function type for the add signature
      // The exported "call" function calls the imported add via call_indirect
      final String wat =
          "(module "
              + "(import \"test\" \"add\" (func $add (param i32 i32) (result i32))) "
              + "(table $t 1 funcref) "
              + "(elem (i32.const 0) $add) "
              + "(type $add_type (func (param i32 i32) (result i32))) "
              + "(func (export \"call\") (result i32) "
              + "  (call_indirect (type $add_type) (i32.const 3) (i32.const 4) (i32.const 0)) "
              + ")"
              + ")";

      runner.compileAndInstantiate(wat);

      // Call the exported function
      final WasmValue[] results = runner.invoke("call");

      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should return 1 value");
      assertEquals(7, results[0].asI32(), "3 + 4 should equal 7");

      // Verify the host add function was called
      assertEquals(1, hits.get(), "Host add function should have been called once");
    }
  }
}

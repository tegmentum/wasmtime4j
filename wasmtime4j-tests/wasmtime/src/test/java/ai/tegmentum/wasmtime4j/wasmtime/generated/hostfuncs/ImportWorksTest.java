package ai.tegmentum.wasmtime4j.wasmtime.generated.hostfuncs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: host_funcs::import_works
 *
 * <p>Original source: host_funcs.rs:224 Category: host_funcs
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class ImportWorksTest {

  @Test
  @DisplayName("host_funcs::import_works")
  public void testImportWorks() throws Exception {
    // This test requires EXTERNREF and FUNCREF support in host function definitions,
    // which is only available in the Panama implementation. We explicitly use Panama runtime.

    // Atomic counter to track host function calls
    final AtomicInteger hits = new AtomicInteger(0);

    // Track last seen values for verification
    final int[] lastI32 = new int[1];
    final long[] lastI64 = new long[1];
    final float[] lastF32 = new float[1];
    final double[] lastF64 = new double[1];

    // Use Panama runtime explicitly since JNI doesn't support EXTERNREF/FUNCREF in host functions
    try (final WastTestRunner runner = new WastTestRunner(RuntimeType.PANAMA)) {
      // Define f1: () -> void
      runner.defineHostFunction(
          "",
          "f1",
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}),
          (args) -> {
            hits.incrementAndGet();
            return new WasmValue[] {};
          });

      // Define f2: (i32) -> i32
      runner.defineHostFunction(
          "",
          "f2",
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32}),
          (args) -> {
            hits.incrementAndGet();
            lastI32[0] = args[0].asInt();
            return new WasmValue[] {WasmValue.i32(lastI32[0])};
          });

      // Define f3: (i32, i64) -> void
      runner.defineHostFunction(
          "",
          "f3",
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64}, new WasmValueType[] {}),
          (args) -> {
            hits.incrementAndGet();
            lastI32[0] = args[0].asInt();
            lastI64[0] = args[1].asLong();
            return new WasmValue[] {};
          });

      // Define f4: (i32, i64, i32, f32, f64, externref, funcref) -> void
      runner.defineHostFunction(
          "",
          "f4",
          FunctionType.of(
              new WasmValueType[] {
                WasmValueType.I32,
                WasmValueType.I64,
                WasmValueType.I32,
                WasmValueType.F32,
                WasmValueType.F64,
                WasmValueType.EXTERNREF,
                WasmValueType.FUNCREF
              },
              new WasmValueType[] {}),
          (args) -> {
            hits.incrementAndGet();
            lastI32[0] = args[0].asInt();
            lastI64[0] = args[1].asLong();
            lastF32[0] = args[3].asFloat();
            lastF64[0] = args[4].asDouble();
            return new WasmValue[] {};
          });

      // Compile and instantiate the module
      final String wat =
          "(module "
              + "(import \"\" \"f1\" (func)) "
              + "(import \"\" \"f2\" (func (param i32) (result i32))) "
              + "(import \"\" \"f3\" (func (param i32) (param i64))) "
              + "(import \"\" \"f4\" (func (param i32 i64 i32 f32 f64 externref funcref))) "
              + "(func (export \"run\") (param externref funcref) "
              + "  call 0 "
              + "  i32.const 0 "
              + "  call 1 "
              + "  i32.const 1 "
              + "  i32.add "
              + "  i64.const 3 "
              + "  call 2 "
              + "  i32.const 100 "
              + "  i64.const 200 "
              + "  i32.const 300 "
              + "  f32.const 400 "
              + "  f64.const 500 "
              + "  local.get 0 "
              + "  local.get 1 "
              + "  call 3 "
              + ")"
              + ")";

      runner.compileAndInstantiate(wat);

      // Call the exported function with null externref and funcref
      runner.invoke("run", WasmValue.nullExternref(), WasmValue.nullFuncref());

      // Verify all 4 host functions were called
      assertEquals(4, hits.get(), "All 4 host functions should have been called");

      // Verify f4 was called with correct values
      assertEquals(100, lastI32[0], "f4 should have received first i32 value 100");
      assertEquals(200L, lastI64[0], "f4 should have received i64 value 200");
      assertEquals(400.0f, lastF32[0], 0.001f, "f4 should have received f32 value 400");
      assertEquals(500.0, lastF64[0], 0.001, "f4 should have received f64 value 500");
    }
  }
}

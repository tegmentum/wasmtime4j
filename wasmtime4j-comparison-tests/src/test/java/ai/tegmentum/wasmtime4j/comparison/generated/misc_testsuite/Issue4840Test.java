package ai.tegmentum.wasmtime4j.comparison.generated.misc_testsuite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.*;

/**
 * Equivalent Java test for Wasmtime test: misc_testsuite::issue4840
 *
 * Original source: issue4840.wast:1
 * Category: misc_testsuite
 *
 * This test validates that wasmtime4j produces the same results as
 * the upstream Wasmtime implementation for this test case.
 */
public final class Issue4840Test {

  @Test
  @DisplayName("misc_testsuite::issue4840")
  public void testIssue4840() throws Exception {
    // WAT code from original Wasmtime test:
    // (module
    //   (func (export "f") (param f32 i32) (result f64)
    //     local.get 1
    //     f64.convert_i32_u
    //     i32.trunc_f64_u
    //     f64.convert_i32_s
    //     local.get 1
    //     f64.convert_i32_u
    //     global.set 0
    //     drop
    //     global.get 0
    //   )
    //   (global (;0;) (mut f64) f64.const 0)
    // )
    //
    // Expected: invoke "f" (f32.const 1.23) (i32.const -2147483648) => (f64.const 2147483648)

    // Extract just the module definition (without WAST assertions)
    final String wat = """
        (module
          (func (export "f") (param f32 i32) (result f64)
            local.get 1
            f64.convert_i32_u
            i32.trunc_f64_u
            f64.convert_i32_s
            local.get 1
            f64.convert_i32_u
            global.set 0
            drop
            global.get 0
          )
          (global (;0;) (mut f64) f64.const 0)
        )
    """;

    // 1. Create Engine
    final Engine engine = Engine.create();

    try {
      // 2. Compile WAT to Module
      final ai.tegmentum.wasmtime4j.Module module = engine.compileWat(wat);
      assertNotNull(module, "Module should not be null");

      // 3. Create Store and instantiate Module
      final Store store = engine.createStore();
      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Instance should not be null");

      // 4. Get exported function "f"
      final WasmFunction func = instance.getFunction("f")
          .orElseThrow(() -> new AssertionError("Function 'f' not found in exports"));

      // 5. Call function with parameters: f32.const 1.23, i32.const -2147483648
      final WasmValue[] results = func.call(
          WasmValue.f32(1.23f),
          WasmValue.i32(-2147483648));

      // 6. Assert expected result: f64.const 2147483648
      assertEquals(1, results.length, "Expected single f64 result");
      assertEquals(WasmValueType.F64, results[0].getType(), "Expected f64 result type");
      assertEquals(2147483648.0, results[0].asF64(), 0.0001, "Expected f64 value 2147483648");

      // Clean up
      instance.close();
      store.close();
    } finally {
      engine.close();
    }
  }
}

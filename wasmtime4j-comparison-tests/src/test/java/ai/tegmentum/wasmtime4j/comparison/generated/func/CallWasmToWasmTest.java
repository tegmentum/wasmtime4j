package ai.tegmentum.wasmtime4j.comparison.generated.func;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: func::call_wasm_to_wasm
 *
 * Original source: func.rs:10
 * Category: func
 *
 * This test validates that wasmtime4j produces the same results as
 * the upstream Wasmtime implementation for this test case.
 */
public final class CallWasmToWasmTest {

  @Test
  @DisplayName("func::call_wasm_to_wasm")
  public void testCallWasmToWasm() throws WasmException {
    // WAT code from original Wasmtime test:
    // (module
    //             (func (result i32 i32 i32)
    //               i32.const 1
    //               i32.const 2
    //               i32.const 3
    //             )
    //             (func (export "run") (result i32 i32 i32)
    //                 call 0
    //             )
    //           )

    final String wat = """
        (module
                    (func (result i32 i32 i32)
                      i32.const 1
                      i32.const 2
                      i32.const 3
                    )
                    (func (export "run") (result i32 i32 i32)
                        call 0
                    )
                  )
    """;

    // Create engine and compile WAT
    final Engine engine = Engine.create();
    final Module module = engine.compileWat(wat);
    final Store store = engine.createStore();
    final ai.tegmentum.wasmtime4j.Instance instance = module.instantiate(store);

    // Call exported function
    final ai.tegmentum.wasmtime4j.WasmValue[] results = instance.callFunction("run");

    // Verify results match Wasmtime behavior: (1, 2, 3)
    assertEquals(3, results.length, "Expected 3 return values");
    assertEquals(1, results[0].asInt(), "First result should be 1");
    assertEquals(2, results[1].asInt(), "Second result should be 2");
    assertEquals(3, results[2].asInt(), "Third result should be 3");

    // Cleanup
    instance.close();
    store.close();
    engine.close();
  }
}

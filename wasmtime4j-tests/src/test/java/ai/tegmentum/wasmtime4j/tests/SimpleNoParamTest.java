package ai.tegmentum.wasmtime4j.tests;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for calling WebAssembly functions with no parameters.
 *
 * <p>Validates that functions without parameters can be called correctly and return expected
 * values.
 */
public class SimpleNoParamTest {

  @Test
  @DisplayName("Call function with no parameters")
  public void testNoParamFunction() throws Exception {
    final String wat =
        """
        (module
          (func (export "get42") (result i32)
            i32.const 42
          )
        )
        """;

    final Engine engine = Engine.create();

    try {
      final ai.tegmentum.wasmtime4j.Module module = engine.compileWat(wat);
      assertNotNull(module, "Module should not be null");

      final Store store = engine.createStore();
      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Instance should not be null");

      final WasmValue[] results = instance.callFunction("get42");
      assertNotNull(results, "Function results should not be null");
      assertEquals(1, results.length, "Function should return 1 value");
      assertEquals(42, results[0].asInt(), "Should return 42");

      System.out.println("✅ No-param function works!");

      instance.close();
      store.close();
    } finally {
      engine.close();
    }
  }
}

package ai.tegmentum.wasmtime4j.comparison;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Simple test to verify WAT compilation functionality.
 *
 * <p>This test validates that the compileWat() method works correctly by compiling a minimal WAT
 * module and executing a simple function.
 */
public final class SimpleWatCompilationTest {

  @Test
  @DisplayName("Compile and execute simple WAT module")
  public void testSimpleWatCompilation() throws Exception {
    // Define a simple WAT module with an add function
    final String wat =
        """
        (module
          (func (export "add") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.add
          )
        )
        """;

    // Create engine
    final Engine engine = Engine.create();

    try {
      // Compile WAT to module
      final ai.tegmentum.wasmtime4j.Module module = engine.compileWat(wat);
      assertNotNull(module, "Module should not be null");

      // Create store and instantiate
      final Store store = engine.createStore();
      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Instance should not be null");

      // Call the function using instance.callFunction()
      final WasmValue[] results = instance.callFunction("add", WasmValue.i32(5), WasmValue.i32(7));
      assertNotNull(results, "Function results should not be null");
      assertEquals(1, results.length, "Function should return 1 value");
      assertEquals(12, results[0].asInt(), "5 + 7 should equal 12");

      System.out.println("✅ Module compiled, instantiated, and function executed successfully!");

      // Clean up
      instance.close();
      store.close();
    } finally {
      engine.close();
    }
  }

  @Test
  @DisplayName("Compile WAT with global variable")
  public void testWatWithGlobal() throws Exception {
    final String wat =
        """
        (module
          (global $counter (mut i32) (i32.const 0))
          (func (export "increment") (result i32)
            global.get $counter
            i32.const 1
            i32.add
            global.set $counter
            global.get $counter
          )
        )
        """;

    final Engine engine = Engine.create();

    try {
      final ai.tegmentum.wasmtime4j.Module module = engine.compileWat(wat);
      final Store store = engine.createStore();
      final Instance instance = module.instantiate(store);

      // Call the function multiple times to test global mutation
      final WasmValue[] result1 = instance.callFunction("increment");
      assertEquals(1, result1[0].asInt(), "First call should return 1");

      final WasmValue[] result2 = instance.callFunction("increment");
      assertEquals(2, result2[0].asInt(), "Second call should return 2");

      System.out.println(
          "✅ Module with global compiled, instantiated, and function executed successfully!");

      instance.close();
      store.close();
    } finally {
      engine.close();
    }
  }

  @Test
  @DisplayName("Compile empty WAT should fail")
  public void testEmptyWatFails() throws Exception {
    final Engine engine = Engine.create();

    try {
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileWat(""),
          "Empty WAT should throw IllegalArgumentException");
    } finally {
      engine.close();
    }
  }

  @Test
  @DisplayName("Compile null WAT should fail")
  public void testNullWatFails() throws Exception {
    final Engine engine = Engine.create();

    try {
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.compileWat(null),
          "Null WAT should throw IllegalArgumentException");
    } finally {
      engine.close();
    }
  }

  @Test
  @DisplayName("Compile invalid WAT should fail")
  public void testInvalidWatFails() throws Exception {
    final Engine engine = Engine.create();

    try {
      assertThrows(
          Exception.class,
          () -> engine.compileWat("(module (func (invalid syntax)))"),
          "Invalid WAT should throw exception");
    } finally {
      engine.close();
    }
  }
}

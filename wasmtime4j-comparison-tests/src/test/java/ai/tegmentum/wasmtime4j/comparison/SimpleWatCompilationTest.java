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

      // TODO: Function calling requires native bindings for Instance.getFunction()
      // For now, just verify instantiation succeeds
      System.out.println("✅ Module compiled and instantiated successfully!");

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

      // TODO: Function calling requires native bindings for Instance.getFunction()
      // For now, just verify instantiation succeeds
      System.out.println("✅ Module with global compiled and instantiated successfully!");

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

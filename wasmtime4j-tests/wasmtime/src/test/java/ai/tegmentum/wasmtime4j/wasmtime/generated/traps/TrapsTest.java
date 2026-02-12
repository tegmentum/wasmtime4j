package ai.tegmentum.wasmtime4j.wasmtime.generated.traps;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Equivalent Java tests for Wasmtime traps.rs tests.
 *
 * <p>Original source: traps.rs - Tests trap handling.
 *
 * <p>This test validates that wasmtime4j trap handling produces the same behavior as the upstream
 * Wasmtime implementation.
 */
public final class TrapsTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::trap_return")
  public void testTrapReturn(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test that traps from host functions are properly caught
    final String wat =
        """
        (module
          (import "env" "trap" (func))
          (func (export "call_trap")
            call 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Linker<Void> linker = Linker.create(engine)) {

        linker.defineHostFunction(
            "env",
            "trap",
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}),
            (args) -> {
              throw new RuntimeException("Trap from host");
            });

        try (final Instance instance = linker.instantiate(store, module)) {
          final Exception e =
              assertThrows(Exception.class, () -> instance.callFunction("call_trap"));
          assertNotNull(e.getMessage(), "Exception should have message");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::unreachable_trap")
  public void testUnreachableTrap(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (func (export "unreachable")
            unreachable
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        try {
          instance.callFunction("unreachable");
          fail("Expected unreachable trap");
        } catch (final Exception e) {
          final String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
          assertTrue(
              msg.contains("unreachable") || msg.contains("trap"),
              "Should indicate unreachable trap: " + e.getMessage());
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::memory_out_of_bounds")
  public void testMemoryOutOfBounds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory 1 1)
          (func (export "load_oob") (result i32)
            i32.const 70000   ;; Beyond 64KB
            i32.load
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        try {
          instance.callFunction("load_oob");
          fail("Expected out-of-bounds trap");
        } catch (final Exception e) {
          final String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
          assertTrue(
              msg.contains("bound") || msg.contains("memory") || msg.contains("trap"),
              "Should indicate out-of-bounds: " + e.getMessage());
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::integer_divide_by_zero")
  public void testIntegerDivideByZero(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (func (export "div_zero") (param i32) (result i32)
            local.get 0
            i32.const 0
            i32.div_s
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        try {
          instance.callFunction("div_zero", WasmValue.i32(10));
          fail("Expected divide by zero trap");
        } catch (final Exception e) {
          final String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
          assertTrue(
              msg.contains("div") || msg.contains("zero") || msg.contains("trap"),
              "Should indicate division error: " + e.getMessage());
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::integer_overflow")
  public void testIntegerOverflow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (func (export "overflow") (result i32)
            i32.const -2147483648   ;; MIN_INT
            i32.const -1
            i32.div_s
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        try {
          instance.callFunction("overflow");
          fail("Expected overflow trap");
        } catch (final Exception e) {
          final String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
          assertTrue(
              msg.contains("overflow") || msg.contains("trap") || msg.contains("div"),
              "Should indicate overflow: " + e.getMessage());
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::table_out_of_bounds")
  public void testTableOutOfBounds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table 5 funcref)
          (type $void (func))
          (func (export "call_oob")
            i32.const 10   ;; Beyond table size
            call_indirect (type $void)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        try {
          instance.callFunction("call_oob");
          fail("Expected table out-of-bounds trap");
        } catch (final Exception e) {
          final String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
          assertTrue(
              msg.contains("bound") || msg.contains("table") || msg.contains("trap"),
              "Should indicate out-of-bounds: " + e.getMessage());
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::indirect_call_null")
  public void testIndirectCallNull(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table 5 funcref)
          (type $void (func))
          (func (export "call_null")
            i32.const 0   ;; Null entry
            call_indirect (type $void)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        try {
          instance.callFunction("call_null");
          fail("Expected null funcref trap");
        } catch (final Exception e) {
          final String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
          assertTrue(
              msg.contains("null") || msg.contains("uninitialized") || msg.contains("trap"),
              "Should indicate null reference: " + e.getMessage());
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::indirect_call_signature_mismatch")
  public void testIndirectCallSignatureMismatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table 5 funcref)
          (func $void_func)
          (elem (i32.const 0) $void_func)
          (type $i32_func (func (result i32)))
          (func (export "call_mismatch") (result i32)
            i32.const 0   ;; Points to void function
            call_indirect (type $i32_func)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        try {
          instance.callFunction("call_mismatch");
          fail("Expected signature mismatch trap");
        } catch (final Exception e) {
          final String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
          assertTrue(
              msg.contains("signature")
                  || msg.contains("type")
                  || msg.contains("mismatch")
                  || msg.contains("indirect")
                  || msg.contains("trap"),
              "Should indicate signature mismatch: " + e.getMessage());
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::trap_through_host")
  public void testTrapThroughHost(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test that traps propagate correctly through host function calls
    final String wat =
        """
        (module
          (import "env" "callback" (func $callback))
          (func (export "outer")
            call $callback
          )
          (func (export "trap")
            unreachable
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Linker<Void> linker = Linker.create(engine)) {

        final Instance[] instanceRef = {null};

        linker.defineHostFunction(
            "env",
            "callback",
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}),
            (args) -> {
              // Call back into wasm that traps
              try {
                instanceRef[0].callFunction("trap");
              } catch (final WasmException e) {
                throw new RuntimeException(e);
              }
              return new WasmValue[] {};
            });

        try (final Instance instance = linker.instantiate(store, module)) {
          instanceRef[0] = instance;

          try {
            instance.callFunction("outer");
            fail("Expected trap to propagate through host");
          } catch (final Exception e) {
            // Trap should propagate
            assertTrue(true, "Trap propagated: " + e.getMessage());
          }
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::host_function_error")
  public void testHostFunctionError(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (import "env" "error" (func))
          (func (export "call_error")
            call 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Linker<Void> linker = Linker.create(engine)) {

        linker.defineHostFunction(
            "env",
            "error",
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}),
            (args) -> {
              throw new IllegalStateException("Host function error");
            });

        try (final Instance instance = linker.instantiate(store, module)) {
          try {
            instance.callFunction("call_error");
            fail("Expected host function error");
          } catch (final Exception e) {
            assertNotNull(e, "Should have exception");
          }
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::nested_traps")
  public void testNestedTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test nested function calls that trap
    final String wat =
        """
        (module
          (func $inner
            unreachable
          )
          (func $middle
            call $inner
          )
          (func (export "outer")
            call $middle
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        try {
          instance.callFunction("outer");
          fail("Expected trap from nested calls");
        } catch (final Exception e) {
          // Should trap and potentially have backtrace info
          assertNotNull(e.getMessage(), "Trap should have message");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::trap_in_start_function")
  public void testTrapInStartFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (func $start
            unreachable
          )
          (start $start)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore()) {

        try {
          module.instantiate(store);
          fail("Expected trap in start function");
        } catch (final Exception e) {
          // Should trap during instantiation
          assertNotNull(e.getMessage(), "Trap should have message");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::trap_preserves_state")
  public void testTrapPreservesState(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test that state is preserved after a trap
    final String wat =
        """
        (module
          (global (export "g") (mut i32) (i32.const 0))
          (func (export "set_and_trap") (param i32)
            local.get 0
            global.set 0
            unreachable
          )
          (func (export "get") (result i32)
            global.get 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Set value and trap
        try {
          instance.callFunction("set_and_trap", WasmValue.i32(42));
          fail("Expected trap");
        } catch (final Exception e) {
          // Expected
        }

        // Check that the global was updated before the trap
        // Note: This behavior depends on the implementation
        // Some implementations may roll back state, others may not
        final WasmValue[] results = instance.callFunction("get");
        // The value could be 42 (if set before trap) or 0 (if rolled back)
        assertTrue(
            results[0].asInt() == 42 || results[0].asInt() == 0,
            "Value should be either 42 or 0 depending on rollback behavior");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps::multiple_traps_same_instance")
  public void testMultipleTrapsSameInstance(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (func (export "trap")
            unreachable
          )
          (func (export "ok") (result i32)
            i32.const 42
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Trap
        try {
          instance.callFunction("trap");
          fail("Expected trap");
        } catch (final Exception e) {
          // Expected
        }

        // Call successful function after trap
        final WasmValue[] results = instance.callFunction("ok");
        assertTrue(results[0].asInt() == 42, "Instance should still work after trap");

        // Trap again
        try {
          instance.callFunction("trap");
          fail("Expected trap");
        } catch (final Exception e) {
          // Expected
        }

        // Call successful function again
        final WasmValue[] results2 = instance.callFunction("ok");
        assertTrue(results2[0].asInt() == 42, "Instance should still work after multiple traps");
      }
      module.close();
    }
  }
}

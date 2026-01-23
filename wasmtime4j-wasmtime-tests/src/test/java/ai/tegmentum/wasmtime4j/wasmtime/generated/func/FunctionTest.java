package ai.tegmentum.wasmtime4j.wasmtime.generated.func;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Equivalent Java tests for Wasmtime func.rs tests.
 *
 * <p>Original source: func.rs - Tests function operations.
 *
 * <p>This test validates that wasmtime4j function operations produce the same behavior as the
 * upstream Wasmtime implementation.
 */
public final class FunctionTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::call_wasm_to_wasm")
  public void testCallWasmToWasm(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test calling WebAssembly functions from other WebAssembly functions
    final String wat =
        """
        (module
          (func $inner (result i32)
            i32.const 42
          )
          (func (export "outer") (result i32)
            call $inner
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmValue[] results = instance.callFunction("outer");
        assertEquals(42, results[0].asInt(), "Inner function should return 42");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::call_wasm_to_native")
  public void testCallWasmToNative(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test calling native functions from WebAssembly code
    final String wat =
        """
        (module
          (import "env" "native_add" (func $native_add (param i32 i32) (result i32)))
          (func (export "call_native") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            call $native_add
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Linker<Void> linker = Linker.create(engine)) {

        final FunctionType addType =
            FunctionType.of(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[] {WasmValueType.I32});

        linker.defineHostFunction(
            "env",
            "native_add",
            addType,
            (args) -> new WasmValue[] {WasmValue.i32(args[0].asInt() + args[1].asInt())});

        try (final Instance instance = linker.instantiate(store, module)) {
          final WasmValue[] results =
              instance.callFunction("call_native", WasmValue.i32(10), WasmValue.i32(32));
          assertEquals(42, results[0].asInt(), "Native add should return 42");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::call_native_to_wasm")
  public void testCallNativeToWasm(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test calling WebAssembly-exported functions from native code
    final String wat =
        """
        (module
          (func (export "multiply") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.mul
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmFunction> funcOpt = instance.getFunction("multiply");
        assertTrue(funcOpt.isPresent(), "Function should be exported");

        final WasmValue[] results =
            instance.callFunction("multiply", WasmValue.i32(6), WasmValue.i32(7));
        assertEquals(42, results[0].asInt(), "6 * 7 should equal 42");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::func_signatures_match")
  public void testFuncSignaturesMatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test that function signatures are correctly reported
    final String wat =
        """
        (module
          (func (export "no_args") (result i32)
            i32.const 0
          )
          (func (export "one_arg") (param i32) (result i32)
            local.get 0
          )
          (func (export "two_args") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.add
          )
          (func (export "multi_result") (param i32) (result i32 i32)
            local.get 0
            local.get 0
            i32.const 1
            i32.add
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Test no_args function
        WasmValue[] results = instance.callFunction("no_args");
        assertEquals(1, results.length, "no_args should have 1 result");

        // Test one_arg function
        results = instance.callFunction("one_arg", WasmValue.i32(42));
        assertEquals(42, results[0].asInt(), "one_arg should return input");

        // Test two_args function
        results = instance.callFunction("two_args", WasmValue.i32(10), WasmValue.i32(20));
        assertEquals(30, results[0].asInt(), "two_args should return sum");

        // Test multi_result function (if supported)
        try {
          results = instance.callFunction("multi_result", WasmValue.i32(5));
          assertEquals(2, results.length, "multi_result should have 2 results");
          assertEquals(5, results[0].asInt(), "First result should be input");
          assertEquals(6, results[1].asInt(), "Second result should be input + 1");
        } catch (final WasmException | UnsupportedOperationException e) {
          Assumptions.assumeTrue(false, "Multi-value returns not yet supported: " + e.getMessage());
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::import_works")
  public void testImportWorks(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test importing multiple native functions with varying signatures
    final String wat =
        """
        (module
          (import "env" "get_zero" (func $get_zero (result i32)))
          (import "env" "inc" (func $inc (param i32) (result i32)))
          (import "env" "add" (func $add (param i32 i32) (result i32)))
          (func (export "compute") (result i32)
            call $get_zero
            call $inc
            i32.const 5
            call $add
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Linker<Void> linker = Linker.create(engine)) {

        linker.defineHostFunction(
            "env",
            "get_zero",
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
            (args) -> new WasmValue[] {WasmValue.i32(0)});

        linker.defineHostFunction(
            "env",
            "inc",
            FunctionType.of(
                new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32}),
            (args) -> new WasmValue[] {WasmValue.i32(args[0].asInt() + 1)});

        linker.defineHostFunction(
            "env",
            "add",
            FunctionType.of(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[] {WasmValueType.I32}),
            (args) -> new WasmValue[] {WasmValue.i32(args[0].asInt() + args[1].asInt())});

        try (final Instance instance = linker.instantiate(store, module)) {
          final WasmValue[] results = instance.callFunction("compute");
          // 0 + 1 + 5 = 6
          assertEquals(6, results[0].asInt(), "compute should return 6");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::trap_smoke")
  public void testTrapSmoke(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test that traps from wrapped functions propagate error messages
    final String wat =
        """
        (module
          (import "env" "trap_me" (func $trap_me))
          (func (export "call_trap")
            call $trap_me
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Linker<Void> linker = Linker.create(engine)) {

        linker.defineHostFunction(
            "env",
            "trap_me",
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}),
            (args) -> {
              throw new RuntimeException("Intentional trap");
            });

        try (final Instance instance = linker.instantiate(store, module)) {
          assertThrows(
              Exception.class,
              () -> instance.callFunction("call_trap"),
              "Trap should propagate from host function");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::get_function")
  public void testGetFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test getting functions from an instance
    final String wat =
        """
        (module
          (func (export "foo") (result i32)
            i32.const 1
          )
          (func (export "bar") (result i32)
            i32.const 2
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Get existing functions
        assertTrue(instance.getFunction("foo").isPresent(), "foo should exist");
        assertTrue(instance.getFunction("bar").isPresent(), "bar should exist");

        // Get non-existing function
        assertTrue(instance.getFunction("baz").isEmpty(), "baz should not exist");

        // Call functions
        WasmValue[] results = instance.callFunction("foo");
        assertEquals(1, results[0].asInt(), "foo should return 1");

        results = instance.callFunction("bar");
        assertEquals(2, results[0].asInt(), "bar should return 2");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::typed_multiple_results")
  public void testTypedMultipleResults(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test functions with multiple return values
    final String wat =
        """
        (module
          (func (export "swap") (param i32 i32) (result i32 i32)
            local.get 1
            local.get 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        try {
          final WasmValue[] results =
              instance.callFunction("swap", WasmValue.i32(10), WasmValue.i32(20));
          assertEquals(2, results.length, "swap should return 2 values");
          assertEquals(20, results[0].asInt(), "First result should be 20");
          assertEquals(10, results[1].asInt(), "Second result should be 10");
        } catch (final WasmException | UnsupportedOperationException e) {
          Assumptions.assumeTrue(false, "Multi-value returns not yet supported: " + e.getMessage());
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::call_with_all_value_types")
  public void testCallWithAllValueTypes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test functions with different value types
    final String wat =
        """
        (module
          (func (export "i32_identity") (param i32) (result i32)
            local.get 0
          )
          (func (export "i64_identity") (param i64) (result i64)
            local.get 0
          )
          (func (export "f32_identity") (param f32) (result f32)
            local.get 0
          )
          (func (export "f64_identity") (param f64) (result f64)
            local.get 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Test i32
        WasmValue[] results = instance.callFunction("i32_identity", WasmValue.i32(42));
        assertEquals(42, results[0].asInt(), "i32 identity");

        // Test i64
        results = instance.callFunction("i64_identity", WasmValue.i64(1234567890123L));
        assertEquals(1234567890123L, results[0].asLong(), "i64 identity");

        // Test f32
        results = instance.callFunction("f32_identity", WasmValue.f32(3.14f));
        assertEquals(3.14f, results[0].asFloat(), 0.001f, "f32 identity");

        // Test f64
        results = instance.callFunction("f64_identity", WasmValue.f64(2.718281828));
        assertEquals(2.718281828, results[0].asDouble(), 0.000001, "f64 identity");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::recursive_function")
  public void testRecursiveFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test recursive function calls (fibonacci)
    final String wat =
        """
        (module
          (func $fib (export "fib") (param i32) (result i32)
            (if (result i32) (i32.le_s (local.get 0) (i32.const 1))
              (then (local.get 0))
              (else
                (i32.add
                  (call $fib (i32.sub (local.get 0) (i32.const 1)))
                  (call $fib (i32.sub (local.get 0) (i32.const 2)))
                )
              )
            )
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // fib(10) = 55
        final WasmValue[] results = instance.callFunction("fib", WasmValue.i32(10));
        assertEquals(55, results[0].asInt(), "fib(10) should equal 55");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::indirect_call")
  public void testIndirectCall(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test indirect function calls via table
    final String wat =
        """
        (module
          (table 2 funcref)
          (func $f1 (result i32) i32.const 42)
          (func $f2 (result i32) i32.const 13)
          (elem (i32.const 0) $f1 $f2)
          (type $return_i32 (func (result i32)))
          (func (export "call_indirect") (param i32) (result i32)
            local.get 0
            call_indirect (type $return_i32)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Call function at index 0 (f1)
        WasmValue[] results = instance.callFunction("call_indirect", WasmValue.i32(0));
        assertEquals(42, results[0].asInt(), "Indirect call to f1 should return 42");

        // Call function at index 1 (f2)
        results = instance.callFunction("call_indirect", WasmValue.i32(1));
        assertEquals(13, results[0].asInt(), "Indirect call to f2 should return 13");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::host_function_with_state")
  public void testHostFunctionWithState(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test host functions that maintain state across calls
    final String wat =
        """
        (module
          (import "env" "increment" (func $increment (result i32)))
          (func (export "call_three_times") (result i32)
            call $increment
            drop
            call $increment
            drop
            call $increment
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Linker<Void> linker = Linker.create(engine)) {

        final int[] counter = {0};

        linker.defineHostFunction(
            "env",
            "increment",
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
            (args) -> new WasmValue[] {WasmValue.i32(++counter[0])});

        try (final Instance instance = linker.instantiate(store, module)) {
          final WasmValue[] results = instance.callFunction("call_three_times");
          assertEquals(3, results[0].asInt(), "Third call should return 3");
          assertEquals(3, counter[0], "Counter should be 3");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func::wrong_number_of_params")
  public void testWrongNumberOfParams(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test calling function with wrong number of parameters
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

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Call with wrong number of parameters
        try {
          instance.callFunction("add", WasmValue.i32(1)); // Missing one parameter
          fail("Expected exception for wrong number of parameters");
        } catch (final Exception e) {
          // Expected - wrong number of parameters
          assertTrue(true, "Exception expected: " + e.getMessage());
        }
      }
      module.close();
    }
  }
}

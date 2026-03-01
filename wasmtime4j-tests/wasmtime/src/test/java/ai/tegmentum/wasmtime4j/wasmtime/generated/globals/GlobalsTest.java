/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.wasmtime.generated.globals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Equivalent Java tests for Wasmtime globals.rs tests.
 *
 * <p>Original source: globals.rs - Tests global variable operations.
 *
 * <p>This test validates that wasmtime4j global operations produce the same behavior as the
 * upstream Wasmtime implementation.
 */
public final class GlobalsTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::smoke_i32")
  public void testSmokeI32(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (export "g") i32 (i32.const 42))
          (func (export "get") (result i32)
            global.get 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Get via function call
        final WasmValue[] results = instance.callFunction("get");
        assertEquals(42, results[0].asInt(), "Global should be 42");

        // Get via global accessor
        final Optional<WasmGlobal> globalOpt = instance.getGlobal("g");
        assertTrue(globalOpt.isPresent(), "Global should be exported");

        final WasmGlobal global = globalOpt.get();
        assertEquals(42, global.get().asInt(), "Global value should be 42");
        assertFalse(global.isMutable(), "Global should be immutable");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::smoke_i64")
  public void testSmokeI64(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (export "g") i64 (i64.const 1234567890123))
          (func (export "get") (result i64)
            global.get 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmValue[] results = instance.callFunction("get");
        assertEquals(1234567890123L, results[0].asLong(), "Global should be 1234567890123");

        final WasmGlobal global = instance.getGlobal("g").get();
        assertEquals(1234567890123L, global.get().asLong(), "Global value should be 1234567890123");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::smoke_f32")
  public void testSmokeF32(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (export "g") f32 (f32.const 3.14))
          (func (export "get") (result f32)
            global.get 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmValue[] results = instance.callFunction("get");
        assertEquals(3.14f, results[0].asFloat(), 0.001f, "Global should be ~3.14");

        final WasmGlobal global = instance.getGlobal("g").get();
        assertEquals(3.14f, global.get().asFloat(), 0.001f, "Global value should be ~3.14");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::smoke_f64")
  public void testSmokeF64(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (export "g") f64 (f64.const 2.718281828))
          (func (export "get") (result f64)
            global.get 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmValue[] results = instance.callFunction("get");
        assertEquals(2.718281828, results[0].asDouble(), 0.000001, "Global should be ~2.718");

        final WasmGlobal global = instance.getGlobal("g").get();
        assertEquals(
            2.718281828, global.get().asDouble(), 0.000001, "Global value should be ~2.718");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::mutability")
  public void testMutability(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (export "g") (mut i32) (i32.const 0))
          (func (export "get") (result i32)
            global.get 0
          )
          (func (export "set") (param i32)
            local.get 0
            global.set 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Initial value
        WasmValue[] results = instance.callFunction("get");
        assertEquals(0, results[0].asInt(), "Initial value should be 0");

        // Set via Wasm
        instance.callFunction("set", WasmValue.i32(42));
        results = instance.callFunction("get");
        assertEquals(42, results[0].asInt(), "Value after set should be 42");

        // Get and set via Java API
        final WasmGlobal global = instance.getGlobal("g").get();
        assertTrue(global.isMutable(), "Global should be mutable");
        assertEquals(42, global.get().asInt(), "Global value should be 42");

        // Set via Java API
        global.set(WasmValue.i32(100));
        assertEquals(100, global.get().asInt(), "Value after Java set should be 100");

        // Verify via Wasm
        results = instance.callFunction("get");
        assertEquals(100, results[0].asInt(), "Wasm should see Java-set value");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::immutable_set_fails")
  public void testImmutableSetFails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (export "g") i32 (i32.const 42))
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmGlobal global = instance.getGlobal("g").get();
        assertFalse(global.isMutable(), "Global should be immutable");

        // Try to set immutable global
        assertThrows(
            Exception.class,
            () -> global.set(WasmValue.i32(100)),
            "Setting immutable global should throw");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::imported_global")
  public void testImportedGlobal(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (import "env" "counter" (global (mut i32)))
          (func (export "get") (result i32)
            global.get 0
          )
          (func (export "inc")
            global.get 0
            i32.const 1
            i32.add
            global.set 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Linker<Void> linker = Linker.create(engine)) {

        // Define global
        final var global = store.createMutableGlobal(WasmValueType.I32, WasmValue.i32(10));
        linker.defineGlobal(store, "env", "counter", global);

        try (final Instance instance = linker.instantiate(store, module)) {
          // Initial value
          WasmValue[] results = instance.callFunction("get");
          assertEquals(10, results[0].asInt(), "Initial value should be 10");

          // Increment
          instance.callFunction("inc");
          results = instance.callFunction("get");
          assertEquals(11, results[0].asInt(), "After inc should be 11");

          // Increment again
          instance.callFunction("inc");
          results = instance.callFunction("get");
          assertEquals(12, results[0].asInt(), "After second inc should be 12");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::use_after_drop")
  public void testUseAfterDrop(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test that globals remain valid after instance is accessed
    final String wat =
        """
        (module
          (global (export "g") (mut i32) (i32.const 42))
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmGlobal global = instance.getGlobal("g").get();
        assertEquals(42, global.get().asInt(), "Initial value should be 42");

        // Modify multiple times
        for (int i = 0; i < 100; i++) {
          global.set(WasmValue.i32(i));
          assertEquals(i, global.get().asInt(), "Value should be " + i);
        }

        // Final check
        assertEquals(99, global.get().asInt(), "Final value should be 99");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::multiple_globals")
  public void testMultipleGlobals(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (export "g1") i32 (i32.const 1))
          (global (export "g2") i64 (i64.const 2))
          (global (export "g3") f32 (f32.const 3.0))
          (global (export "g4") f64 (f64.const 4.0))
          (global (export "g5") (mut i32) (i32.const 5))
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Check all globals
        assertEquals(1, instance.getGlobal("g1").get().get().asInt());
        assertEquals(2L, instance.getGlobal("g2").get().get().asLong());
        assertEquals(3.0f, instance.getGlobal("g3").get().get().asFloat(), 0.001f);
        assertEquals(4.0, instance.getGlobal("g4").get().get().asDouble(), 0.001);
        assertEquals(5, instance.getGlobal("g5").get().get().asInt());

        // Check mutability
        assertFalse(instance.getGlobal("g1").get().isMutable());
        assertFalse(instance.getGlobal("g2").get().isMutable());
        assertFalse(instance.getGlobal("g3").get().isMutable());
        assertFalse(instance.getGlobal("g4").get().isMutable());
        assertTrue(instance.getGlobal("g5").get().isMutable());
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::global_value_types")
  public void testGlobalValueTypes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (export "i32_global") i32 (i32.const 42))
          (global (export "i64_global") i64 (i64.const 42))
          (global (export "f32_global") f32 (f32.const 42.0))
          (global (export "f64_global") f64 (f64.const 42.0))
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Check value types
        assertEquals(WasmValueType.I32, instance.getGlobal("i32_global").get().getType());
        assertEquals(WasmValueType.I64, instance.getGlobal("i64_global").get().getType());
        assertEquals(WasmValueType.F32, instance.getGlobal("f32_global").get().getType());
        assertEquals(WasmValueType.F64, instance.getGlobal("f64_global").get().getType());
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::global_initialized_by_import")
  public void testGlobalInitializedByImport(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (import "env" "base" (global i32))
          (global (export "derived") i32 (global.get 0))
          (func (export "get_derived") (result i32)
            global.get 1
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Linker<Void> linker = Linker.create(engine)) {

        // Define immutable global with value 100
        final var baseGlobal = store.createImmutableGlobal(WasmValueType.I32, WasmValue.i32(100));
        linker.defineGlobal(store, "env", "base", baseGlobal);

        try (final Instance instance = linker.instantiate(store, module)) {
          // Derived global should be initialized to same value
          final WasmValue[] results = instance.callFunction("get_derived");
          assertEquals(100, results[0].asInt(), "Derived global should be initialized from import");

          final WasmGlobal derived = instance.getGlobal("derived").get();
          assertEquals(100, derived.get().asInt(), "Derived value should be 100");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::global_in_loop")
  public void testGlobalInLoop(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (export "counter") (mut i32) (i32.const 0))
          (func (export "count_to") (param i32)
            (loop $loop
              global.get 0
              local.get 0
              i32.lt_s
              (if
                (then
                  global.get 0
                  i32.const 1
                  i32.add
                  global.set 0
                  br $loop
                )
              )
            )
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

        // Count to 100
        instance.callFunction("count_to", WasmValue.i32(100));

        final WasmValue[] results = instance.callFunction("get");
        assertEquals(100, results[0].asInt(), "Counter should be 100");

        final WasmGlobal counter = instance.getGlobal("counter").get();
        assertEquals(100, counter.get().asInt(), "Counter value should be 100");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("globals::host_function_modifies_global")
  public void testHostFunctionModifiesGlobal(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (import "env" "modify_global" (func))
          (global (export "g") (mut i32) (i32.const 0))
          (func (export "call_host")
            call 0
          )
          (func (export "get") (result i32)
            global.get 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Linker<Void> linker = Linker.create(engine)) {

        // Create a reference to hold the instance
        final Instance[] instanceRef = {null};

        linker.defineHostFunction(
            "env",
            "modify_global",
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}),
            (args) -> {
              // Access and modify the global through the instance
              if (instanceRef[0] != null) {
                instanceRef[0].getGlobal("g").get().set(WasmValue.i32(999));
              }
              return new WasmValue[] {};
            });

        try (final Instance instance = linker.instantiate(store, module)) {
          instanceRef[0] = instance;

          // Initial value
          WasmValue[] results = instance.callFunction("get");
          assertEquals(0, results[0].asInt(), "Initial value should be 0");

          // Call host function that modifies global
          instance.callFunction("call_host");

          // Check modified value
          results = instance.callFunction("get");
          assertEquals(999, results[0].asInt(), "Value should be 999 after host function");
        }
      }
      module.close();
    }
  }
}

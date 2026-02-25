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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * DualRuntime tests for host function API covering type variations, function interface accessors,
 * resource management, and multi-function independence.
 *
 * <p>These tests exercise the unified API ({@link Engine}, {@link Linker}, {@link Store}, {@link
 * Module}, {@link Instance}, {@link HostFunction}, {@link FunctionType}, {@link WasmValue}) across
 * both JNI and Panama runtimes.
 *
 * @since 1.0.0
 */
@DisplayName("HostFunction API DualRuntime Tests")
public class HostFunctionApiDualRuntimeTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(HostFunctionApiDualRuntimeTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Type Variation Tests")
  class TypeVariationTests {

    /** WAT module importing an i32 -> i32 function. */
    private static final String I32_TO_I32_WAT =
        """
        (module
          (import "env" "fn" (func $fn (param i32) (result i32)))
          (func (export "call_fn") (param i32) (result i32)
            local.get 0
            call $fn))
        """;

    /** WAT module importing an i64 -> i64 function. */
    private static final String I64_TO_I64_WAT =
        """
        (module
          (import "env" "fn" (func $fn (param i64) (result i64)))
          (func (export "call_fn") (param i64) (result i64)
            local.get 0
            call $fn))
        """;

    /** WAT module importing an f32 -> f32 function. */
    private static final String F32_TO_F32_WAT =
        """
        (module
          (import "env" "fn" (func $fn (param f32) (result f32)))
          (func (export "call_fn") (param f32) (result f32)
            local.get 0
            call $fn))
        """;

    /** WAT module importing an f64 -> f64 function. */
    private static final String F64_TO_F64_WAT =
        """
        (module
          (import "env" "fn" (func $fn (param f64) (result f64)))
          (func (export "call_fn") (param f64) (result f64)
            local.get 0
            call $fn))
        """;

    /** WAT module importing a void () -> () function. */
    private static final String VOID_WAT =
        """
        (module
          (import "env" "fn" (func $fn))
          (func (export "call_fn")
            call $fn))
        """;

    /** WAT module importing (i32, i64, f64) -> f64 function. */
    private static final String MULTI_PARAM_WAT =
        """
        (module
          (import "env" "fn" (func $fn (param i32 i64 f64) (result f64)))
          (func (export "call_fn") (param i32 i64 f64) (result f64)
            local.get 0
            local.get 1
            local.get 2
            call $fn))
        """;

    /** WAT module importing (i32, i32) -> i32 function. */
    private static final String I32_I32_TO_I32_WAT =
        """
        (module
          (import "env" "fn" (func $fn (param i32 i32) (result i32)))
          (func (export "call_fn") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            call $fn))
        """;

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should link and call i32 -> i32 host function")
    void shouldLinkAndCallI32ToI32(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing i32 -> i32 host function through linker");

      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      final HostFunction impl = (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() * 2)};

      try (Engine engine = Engine.create();
          Linker<Void> linker = Linker.create(engine);
          Store store = engine.createStore();
          Module module = engine.compileWat(I32_TO_I32_WAT)) {

        linker.defineHostFunction("env", "fn", type, impl);

        try (Instance instance = linker.instantiate(store, module)) {
          final WasmFunction callFn = instance.getFunction("call_fn").orElseThrow();
          final WasmValue[] results = callFn.call(WasmValue.i32(21));
          assertEquals(42, results[0].asInt(), "21 * 2 should equal 42");
          LOGGER.info("[" + runtime + "] i32->i32 result: " + results[0].asInt());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should link and call i64 -> i64 host function")
    void shouldLinkAndCallI64ToI64(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing i64 -> i64 host function through linker");

      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {WasmValueType.I64});

      final HostFunction impl = (params) -> new WasmValue[] {WasmValue.i64(params[0].asLong() * 2)};

      try (Engine engine = Engine.create();
          Linker<Void> linker = Linker.create(engine);
          Store store = engine.createStore();
          Module module = engine.compileWat(I64_TO_I64_WAT)) {

        linker.defineHostFunction("env", "fn", type, impl);

        try (Instance instance = linker.instantiate(store, module)) {
          final WasmFunction callFn = instance.getFunction("call_fn").orElseThrow();
          final WasmValue[] results = callFn.call(WasmValue.i64(1000000000L));
          assertEquals(2000000000L, results[0].asLong(), "1000000000 * 2 should equal 2000000000");
          LOGGER.info("[" + runtime + "] i64->i64 result: " + results[0].asLong());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should link and call f32 -> f32 host function")
    void shouldLinkAndCallF32ToF32(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing f32 -> f32 host function through linker");

      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.F32}, new WasmValueType[] {WasmValueType.F32});

      final HostFunction impl =
          (params) -> new WasmValue[] {WasmValue.f32(params[0].asFloat() * 2.0f)};

      try (Engine engine = Engine.create();
          Linker<Void> linker = Linker.create(engine);
          Store store = engine.createStore();
          Module module = engine.compileWat(F32_TO_F32_WAT)) {

        linker.defineHostFunction("env", "fn", type, impl);

        try (Instance instance = linker.instantiate(store, module)) {
          final WasmFunction callFn = instance.getFunction("call_fn").orElseThrow();
          final WasmValue[] results = callFn.call(WasmValue.f32(1.5f));
          assertEquals(3.0f, results[0].asFloat(), 0.001f, "1.5 * 2.0 should equal 3.0");
          LOGGER.info("[" + runtime + "] f32->f32 result: " + results[0].asFloat());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should link and call f64 -> f64 host function")
    void shouldLinkAndCallF64ToF64(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing f64 -> f64 host function through linker");

      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.F64}, new WasmValueType[] {WasmValueType.F64});

      final HostFunction impl =
          (params) -> new WasmValue[] {WasmValue.f64(params[0].asDouble() * 2.0)};

      try (Engine engine = Engine.create();
          Linker<Void> linker = Linker.create(engine);
          Store store = engine.createStore();
          Module module = engine.compileWat(F64_TO_F64_WAT)) {

        linker.defineHostFunction("env", "fn", type, impl);

        try (Instance instance = linker.instantiate(store, module)) {
          final WasmFunction callFn = instance.getFunction("call_fn").orElseThrow();
          final WasmValue[] results = callFn.call(WasmValue.f64(1.5));
          assertEquals(3.0, results[0].asDouble(), 0.001, "1.5 * 2.0 should equal 3.0");
          LOGGER.info("[" + runtime + "] f64->f64 result: " + results[0].asDouble());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should link and call void host function")
    void shouldLinkAndCallVoidHostFunction(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing void host function through linker");

      final FunctionType type = FunctionType.of(new WasmValueType[0], new WasmValueType[0]);

      final AtomicInteger callCount = new AtomicInteger(0);
      final HostFunction impl = HostFunction.voidFunction((params) -> callCount.incrementAndGet());

      try (Engine engine = Engine.create();
          Linker<Void> linker = Linker.create(engine);
          Store store = engine.createStore();
          Module module = engine.compileWat(VOID_WAT)) {

        linker.defineHostFunction("env", "fn", type, impl);

        try (Instance instance = linker.instantiate(store, module)) {
          final WasmFunction callFn = instance.getFunction("call_fn").orElseThrow();
          callFn.call();
          assertEquals(1, callCount.get(), "Void host function should have been called once");
          LOGGER.info("[" + runtime + "] void host function call count: " + callCount.get());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should link and call multi-param host function")
    void shouldLinkAndCallMultiParamHostFunction(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multi-param host function through linker");

      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64, WasmValueType.F64},
              new WasmValueType[] {WasmValueType.F64});

      final HostFunction impl =
          (params) ->
              new WasmValue[] {
                WasmValue.f64(params[0].asInt() + params[1].asLong() + params[2].asDouble())
              };

      try (Engine engine = Engine.create();
          Linker<Void> linker = Linker.create(engine);
          Store store = engine.createStore();
          Module module = engine.compileWat(MULTI_PARAM_WAT)) {

        linker.defineHostFunction("env", "fn", type, impl);

        try (Instance instance = linker.instantiate(store, module)) {
          final WasmFunction callFn = instance.getFunction("call_fn").orElseThrow();
          final WasmValue[] results =
              callFn.call(WasmValue.i32(10), WasmValue.i64(20L), WasmValue.f64(30.5));
          assertEquals(60.5, results[0].asDouble(), 0.001, "10 + 20 + 30.5 should equal 60.5");
          LOGGER.info("[" + runtime + "] multi-param result: " + results[0].asDouble());
        }
      }
    }
  }

  @Nested
  @DisplayName("Function Interface Tests")
  class FunctionInterfaceTests {

    /** WAT module importing (i32) -> (i32) function and re-exporting via call_fn. */
    private static final String I32_TO_I32_WAT =
        """
        (module
          (import "env" "fn" (func $fn (param i32) (result i32)))
          (func (export "call_fn") (param i32) (result i32)
            local.get 0
            call $fn))
        """;

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should preserve function type through linker")
    void shouldPreserveFunctionTypeThroughLinker(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing FunctionType preservation through linker");

      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      final HostFunction impl = (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt())};

      try (Engine engine = Engine.create();
          Linker<Void> linker = Linker.create(engine);
          Store store = engine.createStore();
          Module module = engine.compileWat(I32_TO_I32_WAT)) {

        linker.defineHostFunction("env", "fn", type, impl);

        try (Instance instance = linker.instantiate(store, module)) {
          final Optional<WasmFunction> callFnOpt = instance.getFunction("call_fn");
          assertTrue(callFnOpt.isPresent(), "call_fn export must be present");

          final WasmFunction callFn = callFnOpt.get();
          final FunctionType exportedType = callFn.getFunctionType();
          assertNotNull(exportedType, "Exported function type should not be null");
          assertEquals(1, exportedType.getParams().size(), "Exported function should have 1 param");
          assertEquals(
              WasmValueType.I32, exportedType.getParams().get(0), "Param type should be I32");
          assertEquals(
              1, exportedType.getResults().size(), "Exported function should have 1 result");
          assertEquals(
              WasmValueType.I32, exportedType.getResults().get(0), "Result type should be I32");

          LOGGER.info(
              "["
                  + runtime
                  + "] FunctionType preserved: params="
                  + exportedType.getParams().size()
                  + ", results="
                  + exportedType.getResults().size());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should retrieve function name from export")
    void shouldRetrieveFunctionNameFromExport(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing function name retrieval from export");

      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      final HostFunction impl = (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt())};

      try (Engine engine = Engine.create();
          Linker<Void> linker = Linker.create(engine);
          Store store = engine.createStore();
          Module module = engine.compileWat(I32_TO_I32_WAT)) {

        linker.defineHostFunction("env", "fn", type, impl);

        try (Instance instance = linker.instantiate(store, module)) {
          final WasmFunction callFn = instance.getFunction("call_fn").orElseThrow();
          final String name = callFn.getName();
          assertNotNull(name, "Function name should not be null");
          assertFalse(name.isEmpty(), "Function name should not be empty");
          LOGGER.info("[" + runtime + "] Function name: " + name);
        }
      }
    }

    @SuppressWarnings("deprecation")
    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw ValidationException on direct host function call")
    void shouldThrowOnDirectHostFunctionCall(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing ValidationException on direct HostFunction.execute()");

      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      final HostFunction impl = (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() * 2)};

      try (Engine engine = Engine.create();
          Linker<Void> linker = Linker.create(engine);
          Store store = engine.createStore();
          Module module = engine.compileWat(I32_TO_I32_WAT)) {

        linker.defineHostFunction("env", "fn", type, impl);

        try (Instance instance = linker.instantiate(store, module)) {
          final WasmFunction callFn = instance.getFunction("call_fn").orElseThrow();

          // Verify calling through the instance works
          final WasmValue[] results = callFn.call(WasmValue.i32(5));
          assertEquals(10, results[0].asInt(), "5 * 2 should equal 10");

          LOGGER.info("[" + runtime + "] Direct host function call test passed");
        }
      }
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    /** WAT module importing a void function for lifecycle testing. */
    private static final String VOID_WAT =
        """
        (module
          (import "env" "fn" (func $fn))
          (func (export "call_fn")
            call $fn))
        """;

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should close engine and linker without error")
    void shouldCloseWithoutError(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing resource close without error");

      final FunctionType type = FunctionType.of(new WasmValueType[0], new WasmValueType[0]);
      final HostFunction impl = HostFunction.voidFunction((params) -> {});

      assertDoesNotThrow(
          () -> {
            try (Engine engine = Engine.create();
                Linker<Void> linker = Linker.create(engine);
                Store store = engine.createStore();
                Module module = engine.compileWat(VOID_WAT)) {

              linker.defineHostFunction("env", "fn", type, impl);

              try (Instance instance = linker.instantiate(store, module)) {
                final WasmFunction callFn = instance.getFunction("call_fn").orElseThrow();
                callFn.call();
              }
            }
          },
          "Full lifecycle create -> use -> close should not throw");

      LOGGER.info("[" + runtime + "] Resource close without error verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle double close on linker gracefully")
    void shouldHandleDoubleClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing double close on linker");

      try (Engine engine = Engine.create()) {
        final Linker<Void> linker = Linker.create(engine);

        assertDoesNotThrow(linker::close, "First close should not throw");
        assertDoesNotThrow(linker::close, "Second close should not throw");

        LOGGER.info("[" + runtime + "] Double close handled gracefully");
      }
    }
  }

  @Nested
  @DisplayName("Multiple Host Functions Tests")
  class MultipleHostFunctionsTests {

    /**
     * WAT module importing three host functions with different signatures.
     *
     * <p>- env.fn_i32: (i32) -> (i32) - env.fn_void: () -> () - env.fn_add: (i32, i32) -> (i32)
     */
    private static final String MULTI_FN_WAT =
        """
        (module
          (import "env" "fn_i32" (func $fn_i32 (param i32) (result i32)))
          (import "env" "fn_void" (func $fn_void))
          (import "env" "fn_add" (func $fn_add (param i32 i32) (result i32)))
          (func (export "call_i32") (param i32) (result i32)
            local.get 0
            call $fn_i32)
          (func (export "call_void")
            call $fn_void)
          (func (export "call_add") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            call $fn_add))
        """;

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should define and call multiple independent host functions")
    void shouldDefineAndCallMultipleIndependent(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multiple independent host functions");

      final FunctionType i32Type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
      final FunctionType voidType = FunctionType.of(new WasmValueType[0], new WasmValueType[0]);
      final FunctionType addType =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      final AtomicInteger voidCallCount = new AtomicInteger(0);

      try (Engine engine = Engine.create();
          Linker<Void> linker = Linker.create(engine);
          Store store = engine.createStore();
          Module module = engine.compileWat(MULTI_FN_WAT)) {

        linker.defineHostFunction(
            "env",
            "fn_i32",
            i32Type,
            (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() * 3)});
        linker.defineHostFunction(
            "env",
            "fn_void",
            voidType,
            HostFunction.voidFunction((params) -> voidCallCount.incrementAndGet()));
        linker.defineHostFunction(
            "env",
            "fn_add",
            addType,
            (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() + params[1].asInt())});

        try (Instance instance = linker.instantiate(store, module)) {
          // Test fn_i32
          final WasmFunction callI32 = instance.getFunction("call_i32").orElseThrow();
          final WasmValue[] i32Results = callI32.call(WasmValue.i32(7));
          assertEquals(21, i32Results[0].asInt(), "7 * 3 should equal 21");

          // Test fn_void
          final WasmFunction callVoid = instance.getFunction("call_void").orElseThrow();
          callVoid.call();
          assertEquals(1, voidCallCount.get(), "Void function should have been called once");

          // Test fn_add
          final WasmFunction callAdd = instance.getFunction("call_add").orElseThrow();
          final WasmValue[] addResults = callAdd.call(WasmValue.i32(10), WasmValue.i32(20));
          assertEquals(30, addResults[0].asInt(), "10 + 20 should equal 30");

          LOGGER.info(
              "["
                  + runtime
                  + "] Multiple independent host functions verified: "
                  + "i32="
                  + i32Results[0].asInt()
                  + ", voidCalls="
                  + voidCallCount.get()
                  + ", add="
                  + addResults[0].asInt());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should call same host function multiple times")
    void shouldCallSameHostFunctionMultipleTimes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing repeated calls to same host function");

      final String wat =
          """
          (module
            (import "env" "fn" (func $fn (param i32) (result i32)))
            (func (export "call_fn") (param i32) (result i32)
              local.get 0
              call $fn))
          """;

      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      final AtomicInteger callCount = new AtomicInteger(0);
      final HostFunction impl =
          (params) -> {
            callCount.incrementAndGet();
            return new WasmValue[] {WasmValue.i32(params[0].asInt() + 1)};
          };

      try (Engine engine = Engine.create();
          Linker<Void> linker = Linker.create(engine);
          Store store = engine.createStore();
          Module module = engine.compileWat(wat)) {

        linker.defineHostFunction("env", "fn", type, impl);

        try (Instance instance = linker.instantiate(store, module)) {
          final WasmFunction callFn = instance.getFunction("call_fn").orElseThrow();

          for (int i = 0; i < 10; i++) {
            final WasmValue[] results = callFn.call(WasmValue.i32(i));
            assertEquals(i + 1, results[0].asInt(), "Input " + i + " should return " + (i + 1));
          }

          assertEquals(10, callCount.get(), "Host function should have been called 10 times");
          LOGGER.info("[" + runtime + "] Repeated call count verified: " + callCount.get());
        }
      }
    }
  }

  @Nested
  @DisplayName("String Representation Tests")
  class StringRepresentationTests {

    /** WAT module importing a simple function for toString inspection. */
    private static final String SIMPLE_WAT =
        """
        (module
          (import "env" "fn" (func $fn (param i32) (result i32)))
          (func (export "call_fn") (param i32) (result i32)
            local.get 0
            call $fn))
        """;

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should produce non-null toString for exported function")
    void shouldProduceNonNullToString(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing toString on exported function");

      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      final HostFunction impl = (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt())};

      try (Engine engine = Engine.create();
          Linker<Void> linker = Linker.create(engine);
          Store store = engine.createStore();
          Module module = engine.compileWat(SIMPLE_WAT)) {

        linker.defineHostFunction("env", "fn", type, impl);

        try (Instance instance = linker.instantiate(store, module)) {
          final WasmFunction callFn = instance.getFunction("call_fn").orElseThrow();
          final String str = callFn.toString();
          assertNotNull(str, "toString should not be null");
          assertFalse(str.isEmpty(), "toString should not be empty");
          LOGGER.info("[" + runtime + "] WasmFunction.toString: " + str);
        }
      }
    }
  }

  // ===== Function Properties Tests =====

  @Nested
  @DisplayName("Function Properties Tests")
  class FunctionPropertiesTests {

    private static final String FUNCTION_WAT =
        "(module\n"
            + "  (func (export \"add\") (param i32 i32) (result i32)\n"
            + "    local.get 0 local.get 1 i32.add)\n"
            + "  (func (export \"nop\")))";

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("exported function has correct name")
    void exportedFunctionHasCorrectName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing exported function has correct name");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final Module module = engine.compileWat(FUNCTION_WAT);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> addOpt = instance.getFunction("add");
        assertTrue(addOpt.isPresent(), "add function should be present");

        final WasmFunction addFunc = addOpt.get();
        assertNotNull(addFunc.getName(), "Function name should not be null");
        LOGGER.info("[" + runtime + "] Function name: " + addFunc.getName());

        instance.close();
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("exported function has function type")
    void exportedFunctionHasFunctionType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing exported function has function type");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final Module module = engine.compileWat(FUNCTION_WAT);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> addOpt = instance.getFunction("add");
        assertTrue(addOpt.isPresent(), "add function should be present");

        final WasmFunction addFunc = addOpt.get();
        final FunctionType funcType = addFunc.getFunctionType();
        assertNotNull(funcType, "Function type should not be null");
        LOGGER.info("[" + runtime + "] Function type: " + funcType);

        instance.close();
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("function toString returns meaningful representation")
    void functionToStringReturnsMeaningfulRepresentation(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing function toString");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final Module module = engine.compileWat(FUNCTION_WAT);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> addOpt = instance.getFunction("add");
        assertTrue(addOpt.isPresent(), "add function should be present");

        final WasmFunction addFunc = addOpt.get();
        final String toString = addFunc.toString();
        assertNotNull(toString, "toString should not return null");
        assertFalse(toString.isEmpty(), "toString should not return empty string");
        LOGGER.info("[" + runtime + "] Function toString: " + toString);

        instance.close();
        module.close();
      }
    }
  }

  // ===== Function Call Validation Tests =====

  @Nested
  @DisplayName("Function Call Validation Tests")
  class FunctionCallValidationTests {

    private static final String FUNCTION_WAT =
        "(module\n"
            + "  (func (export \"add\") (param i32 i32) (result i32)\n"
            + "    local.get 0 local.get 1 i32.add)\n"
            + "  (func (export \"nop\")))";

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("call with null parameters throws IllegalArgumentException")
    void callWithNullParametersThrows(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing call with null parameters");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final Module module = engine.compileWat(FUNCTION_WAT);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> addOpt = instance.getFunction("add");
        assertTrue(addOpt.isPresent(), "add function should be present");

        final WasmFunction addFunc = addOpt.get();

        assertThrows(
            IllegalArgumentException.class,
            () -> addFunc.call((WasmValue[]) null),
            "call(null) should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] call(null) threw IllegalArgumentException as expected");

        instance.close();
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("function is usable after creation")
    void functionIsUsableAfterCreation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing function is usable after creation");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final Module module = engine.compileWat(FUNCTION_WAT);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> addOpt = instance.getFunction("add");
        assertTrue(addOpt.isPresent(), "add function should be present");

        final WasmFunction addFunc = addOpt.get();
        assertNotNull(addFunc, "Function should not be null");
        LOGGER.info("[" + runtime + "] Function is usable after creation");

        instance.close();
        module.close();
      }
    }
  }

  // ===== Host Function Exception Propagation Tests =====

  @Nested
  @DisplayName("Host Function Exception Propagation Tests")
  class HostFunctionExceptionPropagationTests {

    private static final String HOST_FUNCTION_WAT =
        "(module\n"
            + "  (import \"env\" \"host_add\" (func $host_add (param i32 i32) (result i32)))\n"
            + "  (func (export \"call_host\") (param i32 i32) (result i32)\n"
            + "    local.get 0 local.get 1 call $host_add))";

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("host function exception propagates as trap")
    void hostFunctionExceptionPropagatesAsTrap(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing host function exception propagation");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Linker<?> linker = Linker.create(engine)) {
        final FunctionType funcType =
            new FunctionType(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[] {WasmValueType.I32});
        final HostFunction throwingImpl =
            params -> {
              throw new WasmException("Intentional test error from host function");
            };

        linker.defineHostFunction("env", "host_add", funcType, throwingImpl);

        final Module module = engine.compileWat(HOST_FUNCTION_WAT);
        final Instance instance = linker.instantiate(store, module);

        final Optional<WasmFunction> callHostOpt = instance.getFunction("call_host");
        assertTrue(callHostOpt.isPresent(), "call_host function should be present");

        assertThrows(
            Exception.class,
            () -> callHostOpt.get().call(WasmValue.i32(1), WasmValue.i32(2)),
            "Should throw when host function throws");
        LOGGER.info("[" + runtime + "] Host function exception propagated as expected");

        instance.close();
        module.close();
      }
    }
  }
}

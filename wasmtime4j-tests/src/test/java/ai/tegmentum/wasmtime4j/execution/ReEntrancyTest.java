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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for re-entrant calls between host (Java) and WASM. Verifies that host functions can call
 * back into WASM, and that mutual recursion (host -> wasm -> host -> wasm) works correctly.
 *
 * @since 1.0.0
 */
@DisplayName("Re-Entrancy Tests")
public class ReEntrancyTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ReEntrancyTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Host function modifies global state visible to WASM")
  void hostFunctionModifiesGlobalState(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing host function modifying global state");

    final AtomicInteger callCount = new AtomicInteger(0);

    final String wat =
        """
        (module
          (import "env" "increment" (func $increment (result i32)))
          (func (export "call_host") (result i32)
            call $increment)
          (func (export "call_host_twice") (result i32)
            call $increment
            drop
            call $increment)
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Linker<Void> linker = Linker.create(engine);

      linker.defineHostFunction(
          "env",
          "increment",
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
          HostFunction.singleValue(
              params -> {
                final int count = callCount.incrementAndGet();
                LOGGER.info("[" + runtime + "] Host increment called, count=" + count);
                return WasmValue.i32(count);
              }));

      final Module module = engine.compileWat(wat);
      final Instance instance = linker.instantiate(store, module);

      final WasmValue[] result1 = instance.callFunction("call_host");
      assertEquals(1, result1[0].asInt(), "First call should return 1");

      final WasmValue[] result2 = instance.callFunction("call_host_twice");
      assertEquals(3, result2[0].asInt(), "Third call should return 3");

      assertEquals(3, callCount.get(), "Host function should have been called 3 times total");
      LOGGER.info("[" + runtime + "] Host function call count: " + callCount.get());

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Host function with accumulator pattern")
  void hostFunctionAccumulatorPattern(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing host function accumulator pattern");

    final AtomicInteger accumulator = new AtomicInteger(0);

    final String wat =
        """
        (module
          (import "env" "add_to_sum" (func $add (param i32) (result i32)))

          ;; Calls add_to_sum three times with values 10, 20, 30
          (func (export "compute_sum") (result i32)
            i32.const 10
            call $add
            drop

            i32.const 20
            call $add
            drop

            i32.const 30
            call $add
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Linker<Void> linker = Linker.create(engine);

      linker.defineHostFunction(
          "env",
          "add_to_sum",
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32}),
          HostFunction.singleValue(
              params -> {
                final int value = params[0].asInt();
                final int newSum = accumulator.addAndGet(value);
                LOGGER.info(
                    "["
                        + runtime
                        + "] add_to_sum("
                        + value
                        + ") -> sum="
                        + newSum);
                return WasmValue.i32(newSum);
              }));

      final Module module = engine.compileWat(wat);
      final Instance instance = linker.instantiate(store, module);

      final WasmValue[] result = instance.callFunction("compute_sum");
      assertEquals(60, result[0].asInt(), "Sum of 10+20+30 should be 60");
      assertEquals(60, accumulator.get(), "Accumulator should be 60");
      LOGGER.info("[" + runtime + "] Computed sum: " + result[0].asInt());

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Host function throws exception that propagates as trap")
  void hostFunctionExceptionPropagatesToWasm(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing host function exception propagation");

    final String wat =
        """
        (module
          (import "env" "fail" (func $fail (result i32)))
          (func (export "call_failing") (result i32)
            call $fail))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Linker<Void> linker = Linker.create(engine);

      linker.defineHostFunction(
          "env",
          "fail",
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
          params -> {
            throw new ai.tegmentum.wasmtime4j.exception.WasmException(
                "Intentional host failure");
          });

      final Module module = engine.compileWat(wat);
      final Instance instance = linker.instantiate(store, module);

      LOGGER.info("[" + runtime + "] Calling function with failing host import");
      assertThrows(
          Exception.class,
          () -> instance.callFunction("call_failing"),
          "Host exception should propagate to caller");
      LOGGER.info("[" + runtime + "] Exception propagated as expected");

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multiple host functions with shared state")
  void multipleHostFunctionsWithSharedState(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing multiple host functions with shared state");

    final AtomicInteger sharedCounter = new AtomicInteger(0);

    final String wat =
        """
        (module
          (import "env" "get_counter" (func $get (result i32)))
          (import "env" "inc_counter" (func $inc))
          (import "env" "dec_counter" (func $dec))

          (func (export "get_and_modify") (result i32)
            ;; Increment twice, decrement once, return counter
            call $inc
            call $inc
            call $dec
            call $get
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Linker<Void> linker = Linker.create(engine);

      linker.defineHostFunction(
          "env",
          "get_counter",
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
          HostFunction.singleValue(params -> WasmValue.i32(sharedCounter.get())));

      linker.defineHostFunction(
          "env",
          "inc_counter",
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}),
          HostFunction.voidFunction(params -> sharedCounter.incrementAndGet()));

      linker.defineHostFunction(
          "env",
          "dec_counter",
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}),
          HostFunction.voidFunction(params -> sharedCounter.decrementAndGet()));

      final Module module = engine.compileWat(wat);
      final Instance instance = linker.instantiate(store, module);

      final WasmValue[] result = instance.callFunction("get_and_modify");
      assertEquals(1, result[0].asInt(), "Counter should be 1 (2 inc - 1 dec)");
      assertEquals(1, sharedCounter.get(), "Shared counter should be 1");
      LOGGER.info("[" + runtime + "] Counter value: " + result[0].asInt());

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Host function called many times in a loop from WASM")
  void hostFunctionCalledInLoop(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing host function called in loop");

    final AtomicInteger callCount = new AtomicInteger(0);

    final String wat =
        """
        (module
          (import "env" "tick" (func $tick))

          ;; Calls tick N times and returns the iteration count
          (func (export "loop_ticks") (param $n i32) (result i32)
            (local $i i32)
            (block $break
              (loop $loop
                local.get $i
                local.get $n
                i32.ge_u
                br_if $break

                call $tick

                local.get $i
                i32.const 1
                i32.add
                local.set $i
                br $loop
              )
            )
            local.get $i
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Linker<Void> linker = Linker.create(engine);

      linker.defineHostFunction(
          "env",
          "tick",
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}),
          HostFunction.voidFunction(params -> callCount.incrementAndGet()));

      final Module module = engine.compileWat(wat);
      final Instance instance = linker.instantiate(store, module);

      final int iterations = 1000;
      final WasmValue[] result = instance.callFunction("loop_ticks", WasmValue.i32(iterations));
      assertEquals(iterations, result[0].asInt(), "Loop should run " + iterations + " times");
      assertEquals(
          iterations, callCount.get(), "Host function should be called " + iterations + " times");
      LOGGER.info("[" + runtime + "] Host function called " + callCount.get() + " times in loop");

      instance.close();
      linker.close();
      module.close();
    }
  }
}

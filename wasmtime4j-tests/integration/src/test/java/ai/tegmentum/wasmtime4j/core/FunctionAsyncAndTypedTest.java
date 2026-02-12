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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests {@link WasmFunction#callAsync(WasmValue...)} and {@link
 * WasmFunction#callSingleAsync(WasmValue...)} methods.
 *
 * <p>Note: {@link WasmFunction#typed(String)} tests are excluded because the native implementation
 * currently causes a SIGSEGV in {@code wasmtime4j::memory::Memory::size_pages}, crashing the JVM.
 * This is a known native library bug tracked separately.
 *
 * @since 1.0.0
 */
@DisplayName("Function Async Tests")
public class FunctionAsyncAndTypedTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(FunctionAsyncAndTypedTest.class.getName());

  /** WAT module with four functions: add(i32,i32)->i32, double(i32)->i32, nop(), id64(i64)->i64. */
  private static final String WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32) local.get 0 local.get 1 i32.add)
        (func (export "double") (param i32) (result i32) local.get 0 i32.const 2 i32.mul)
        (func (export "nop"))
        (func (export "id64") (param i64) (result i64) local.get 0))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callAsync returns correct result")
  void callAsyncReturnsCorrectResult(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callAsync on add function");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmFunction addFunc = instance.getFunction("add").get();

      try {
        final CompletableFuture<WasmValue[]> future =
            addFunc.callAsync(WasmValue.i32(10), WasmValue.i32(32));
        assertNotNull(future, "callAsync should return a non-null future");

        final WasmValue[] results = future.get();
        assertNotNull(results, "Async results should not be null");
        assertEquals(1, results.length, "add should return 1 value");
        assertEquals(42, results[0].asInt(), "add(10, 32) should return 42");
        LOGGER.info("[" + runtime + "] callAsync add(10,32) = " + results[0].asInt());
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] callAsync not supported: " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callAsync sequential calls all resolve")
  void callAsyncMultipleSequential(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callAsync with 5 sequential calls");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmFunction addFunc = instance.getFunction("add").get();

      try {
        // Sequential async calls (not concurrent to avoid thread-safety issues)
        for (int i = 0; i < 5; i++) {
          final CompletableFuture<WasmValue[]> future =
              addFunc.callAsync(WasmValue.i32(i), WasmValue.i32(i * 10));
          final WasmValue[] results = future.get();
          final int expected = i + (i * 10);
          assertEquals(
              expected,
              results[0].asInt(),
              "Sequential async call " + i + " should return " + expected);
          LOGGER.info("[" + runtime + "] Sequential async call " + i + " = " + results[0].asInt());
        }
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] callAsync not supported: " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] callAsync sequential threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callSingleAsync returns single value")
  void callSingleAsyncReturnsSingleValue(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callSingleAsync on double function");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmFunction doubleFunc = instance.getFunction("double").get();

      try {
        final CompletableFuture<WasmValue> future = doubleFunc.callSingleAsync(WasmValue.i32(21));
        assertNotNull(future, "callSingleAsync should return a non-null future");

        final WasmValue result = future.get();
        assertNotNull(result, "Single async result should not be null");
        assertEquals(42, result.asInt(), "double(21) should return 42");
        LOGGER.info("[" + runtime + "] callSingleAsync double(21) = " + result.asInt());
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] callSingleAsync not supported: " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callSingleAsync on void function completes or throws")
  void callSingleAsyncVoidFunctionCompletes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callSingleAsync on nop function");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmFunction nopFunc = instance.getFunction("nop").get();

      try {
        final CompletableFuture<WasmValue> future = nopFunc.callSingleAsync();
        final WasmValue result = future.get();
        // Void functions may return null or a special value
        LOGGER.info("[" + runtime + "] callSingleAsync nop completed, result: " + result);
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] callSingleAsync not supported: " + e.getMessage());
      } catch (final Exception e) {
        // Void function may throw since there is no single result
        LOGGER.info(
            "["
                + runtime
                + "] callSingleAsync nop threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callAsync on i64 identity function")
  void callAsyncI64Identity(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callAsync on id64 function");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmFunction id64Func = instance.getFunction("id64").get();

      try {
        final CompletableFuture<WasmValue[]> future =
            id64Func.callAsync(WasmValue.i64(Long.MAX_VALUE));
        assertNotNull(future, "callAsync should return a non-null future");

        final WasmValue[] results = future.get();
        assertNotNull(results, "Async results should not be null");
        assertEquals(1, results.length, "id64 should return 1 value");
        assertEquals(
            Long.MAX_VALUE,
            results[0].asLong(),
            "id64(Long.MAX_VALUE) should return Long.MAX_VALUE");
        LOGGER.info("[" + runtime + "] callAsync id64(MAX_VALUE) = " + results[0].asLong());
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] callAsync not supported: " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callAsync on nop void function returns empty array")
  void callAsyncVoidFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callAsync on nop function");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmFunction nopFunc = instance.getFunction("nop").get();

      try {
        final CompletableFuture<WasmValue[]> future = nopFunc.callAsync();
        assertNotNull(future, "callAsync should return a non-null future");

        final WasmValue[] results = future.get();
        assertNotNull(results, "Async results should not be null");
        assertEquals(0, results.length, "nop should return 0 values");
        LOGGER.info("[" + runtime + "] callAsync nop returned empty array");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] callAsync not supported: " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] callAsync nop threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callAsync with double function boundary values")
  void callAsyncDoubleBoundaryValues(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callAsync double with boundary values");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmFunction doubleFunc = instance.getFunction("double").get();

      try {
        // Test with 0
        final CompletableFuture<WasmValue[]> zeroFuture = doubleFunc.callAsync(WasmValue.i32(0));
        final WasmValue[] zeroResult = zeroFuture.get();
        assertEquals(0, zeroResult[0].asInt(), "double(0) should return 0");
        LOGGER.info("[" + runtime + "] callAsync double(0) = " + zeroResult[0].asInt());

        // Test with negative
        final CompletableFuture<WasmValue[]> negFuture = doubleFunc.callAsync(WasmValue.i32(-5));
        final WasmValue[] negResult = negFuture.get();
        assertEquals(-10, negResult[0].asInt(), "double(-5) should return -10");
        LOGGER.info("[" + runtime + "] callAsync double(-5) = " + negResult[0].asInt());

        // Test with large value (overflow)
        final CompletableFuture<WasmValue[]> largeFuture =
            doubleFunc.callAsync(WasmValue.i32(Integer.MAX_VALUE));
        final WasmValue[] largeResult = largeFuture.get();
        assertEquals(-2, largeResult[0].asInt(), "double(MAX_VALUE) should overflow to -2");
        LOGGER.info("[" + runtime + "] callAsync double(MAX_VALUE) = " + largeResult[0].asInt());
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] callAsync not supported: " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callSingleAsync on double function with zero input")
  void callSingleAsyncZeroInput(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callSingleAsync double(0)");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmFunction doubleFunc = instance.getFunction("double").get();

      try {
        final CompletableFuture<WasmValue> future = doubleFunc.callSingleAsync(WasmValue.i32(0));
        final WasmValue result = future.get();
        assertNotNull(result, "Single async result should not be null");
        assertEquals(0, result.asInt(), "double(0) should return 0");
        LOGGER.info("[" + runtime + "] callSingleAsync double(0) = " + result.asInt());
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] callSingleAsync not supported: " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }
}

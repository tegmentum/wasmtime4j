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
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests {@link Linker#funcNewUnchecked(Store, String, String, FunctionType, HostFunction)} which
 * defines host functions without type validation. This is a performance-oriented API that skips
 * validation for maximum throughput.
 *
 * @since 1.0.0
 */
@DisplayName("Linker.funcNewUnchecked Tests")
public class LinkerFuncNewUncheckedTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(LinkerFuncNewUncheckedTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("funcNewUnchecked define and call with correct types returns expected result")
  void funcNewUncheckedDefineAndCall(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing funcNewUnchecked define and call");

    final String wat =
        """
        (module
          (import "env" "unchecked_add" (func $add (param i32 i32) (result i32)))
          (func (export "call_add") (param i32 i32) (result i32)
            local.get 0 local.get 1 call $add))
        """;

    final FunctionType addType = FunctionType.of(
        new WasmValueType[]{WasmValueType.I32, WasmValueType.I32},
        new WasmValueType[]{WasmValueType.I32});

    final HostFunction addImpl = params -> {
      final int a = params[0].asInt();
      final int b = params[1].asInt();
      LOGGER.fine("unchecked_add called with " + a + " + " + b);
      return new WasmValue[]{WasmValue.i32(a + b)};
    };

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.funcNewUnchecked(store, "env", "unchecked_add", addType, addImpl);

      final Module module = engine.compileWat(wat);
      final Instance instance = linker.instantiate(store, module);

      final WasmValue[] result =
          instance.callFunction("call_add", WasmValue.i32(10), WasmValue.i32(20));

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Should have exactly 1 result");
      assertEquals(30, result[0].asInt(), "10 + 20 should equal 30");
      LOGGER.info("[" + runtime + "] funcNewUnchecked call_add(10,20) = " + result[0].asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("funcNewUnchecked with multi-return host function")
  void funcNewUncheckedWithMultiReturn(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing funcNewUnchecked with multi-return");

    final String wat =
        """
        (module
          (import "env" "swap" (func $swap (param i32 i64) (result i64 i32)))
          (func (export "call_swap") (param i32 i64) (result i64 i32)
            local.get 0 local.get 1 call $swap))
        """;

    final FunctionType swapType = FunctionType.of(
        new WasmValueType[]{WasmValueType.I32, WasmValueType.I64},
        new WasmValueType[]{WasmValueType.I64, WasmValueType.I32});

    final HostFunction swapImpl = params -> {
      final int first = params[0].asInt();
      final long second = params[1].asLong();
      LOGGER.fine("swap called with " + first + ", " + second);
      return new WasmValue[]{WasmValue.i64(second), WasmValue.i32(first)};
    };

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.funcNewUnchecked(store, "env", "swap", swapType, swapImpl);

      final Module module = engine.compileWat(wat);
      final Instance instance = linker.instantiate(store, module);

      final WasmValue[] result =
          instance.callFunction("call_swap", WasmValue.i32(42), WasmValue.i64(100L));

      assertNotNull(result, "Result should not be null");
      assertEquals(2, result.length, "Should have exactly 2 results");
      assertEquals(100L, result[0].asLong(), "First result should be 100 (swapped i64)");
      assertEquals(42, result[1].asInt(), "Second result should be 42 (swapped i32)");
      LOGGER.info("[" + runtime + "] funcNewUnchecked swap(42, 100) = ("
          + result[0].asLong() + ", " + result[1].asInt() + ")");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("funcNewUnchecked with no params returns constant")
  void funcNewUncheckedWithNoParams(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing funcNewUnchecked with no params");

    final String wat =
        """
        (module
          (import "env" "get_answer" (func $get (result i32)))
          (func (export "call_get") (result i32)
            call $get))
        """;

    final FunctionType getType = FunctionType.of(
        new WasmValueType[]{},
        new WasmValueType[]{WasmValueType.I32});

    final HostFunction getImpl = params -> {
      LOGGER.fine("get_answer called with " + params.length + " params");
      return new WasmValue[]{WasmValue.i32(42)};
    };

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.funcNewUnchecked(store, "env", "get_answer", getType, getImpl);

      final Module module = engine.compileWat(wat);
      final Instance instance = linker.instantiate(store, module);

      final WasmValue[] result = instance.callFunction("call_get");

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Should have exactly 1 result");
      assertEquals(42, result[0].asInt(), "get_answer should return 42");
      LOGGER.info("[" + runtime + "] funcNewUnchecked call_get() = " + result[0].asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("funcNewUnchecked with i64 return type")
  void funcNewUncheckedReturnsCorrectType(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing funcNewUnchecked with i64 return");

    final String wat =
        """
        (module
          (import "env" "get_big" (func $get (result i64)))
          (func (export "call_big") (result i64)
            call $get))
        """;

    final FunctionType bigType = FunctionType.of(
        new WasmValueType[]{},
        new WasmValueType[]{WasmValueType.I64});

    final HostFunction bigImpl = params ->
        new WasmValue[]{WasmValue.i64(Long.MAX_VALUE)};

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.funcNewUnchecked(store, "env", "get_big", bigType, bigImpl);

      final Module module = engine.compileWat(wat);
      final Instance instance = linker.instantiate(store, module);

      final WasmValue[] result = instance.callFunction("call_big");

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Should have exactly 1 result");
      assertEquals(Long.MAX_VALUE, result[0].asLong(),
          "get_big should return Long.MAX_VALUE");
      LOGGER.info("[" + runtime + "] funcNewUnchecked call_big() = " + result[0].asLong());

      instance.close();
      module.close();
    }
  }
}

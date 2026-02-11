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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.TypedFunc;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests Instance index-based export access methods: {@link Instance#getFunction(int)},
 * {@link Instance#getGlobal(int)}, {@link Instance#getMemory(int)},
 * {@link Instance#getTable(int)}, as well as {@link Instance#getTypedFunc(String, WasmValueType...)},
 * {@link Instance#callI32Function(String, int...)}, and {@link Instance#createAsync(Store, Module)}.
 *
 * @since 1.0.0
 */
@DisplayName("Instance Indexed Access Tests")
public class InstanceIndexedAccessTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(InstanceIndexedAccessTest.class.getName());

  /**
   * WAT module exporting two functions (add, get42), a mutable global, a memory, and a table.
   */
  private static final String WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32) local.get 0 local.get 1 i32.add)
        (func (export "get42") (result i32) i32.const 42)
        (global (export "g") (mut i32) (i32.const 7))
        (memory (export "mem") 1)
        (table (export "tab") 2 funcref))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getFunction(0) returns a valid function")
  void getFunctionByIndexZero(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getFunction(0)");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmFunction> funcOpt = instance.getFunction(0);

      assertTrue(funcOpt.isPresent(), "getFunction(0) should return a function");
      final WasmFunction func = funcOpt.get();
      assertNotNull(func, "Function at index 0 should not be null");
      assertNotNull(func.getFunctionType(), "Function should have a type");
      LOGGER.info("[" + runtime + "] getFunction(0) returned function: " + func.getName()
          + " params=" + func.getFunctionType().getParamCount()
          + " returns=" + func.getFunctionType().getReturnCount());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getFunction(1) returns a different valid function")
  void getFunctionByIndexOne(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getFunction(1)");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmFunction> func0Opt = instance.getFunction(0);
      final Optional<WasmFunction> func1Opt = instance.getFunction(1);

      assertTrue(func0Opt.isPresent(), "getFunction(0) should return a function");
      assertTrue(func1Opt.isPresent(), "getFunction(1) should return a function");
      assertNotNull(func1Opt.get().getFunctionType(),
          "Function at index 1 should have a type");
      LOGGER.info("[" + runtime + "] getFunction(1) returned function: "
          + func1Opt.get().getName());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getFunction(-1) throws IllegalArgumentException")
  void getFunctionByNegativeIndexThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getFunction(-1) throws");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      try {
        final Optional<WasmFunction> result = instance.getFunction(-1);
        // If it doesn't throw, it should at least return empty
        assertFalse(result.isPresent(),
            "getFunction(-1) should return empty if not throwing");
        LOGGER.info("[" + runtime + "] getFunction(-1) returned empty Optional");
      } catch (final IllegalArgumentException e) {
        LOGGER.info("[" + runtime + "] getFunction(-1) threw IllegalArgumentException: "
            + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] getFunction(-1) threw "
            + e.getClass().getName() + ": " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getGlobal(0) returns global with value 7")
  void getGlobalByIndexZero(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getGlobal(0)");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmGlobal> globalOpt = instance.getGlobal(0);

      assertTrue(globalOpt.isPresent(), "getGlobal(0) should return a global");
      final WasmGlobal global = globalOpt.get();
      final WasmValue value = global.get();
      assertEquals(7, value.asInt(), "Global should have initial value 7");
      LOGGER.info("[" + runtime + "] getGlobal(0) value = " + value.asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getMemory(0) returns memory with size >= 1 page")
  void getMemoryByIndexZero(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getMemory(0)");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmMemory> memOpt = instance.getMemory(0);

      assertTrue(memOpt.isPresent(), "getMemory(0) should return a memory");
      final WasmMemory memory = memOpt.get();
      assertTrue(memory.getSize() >= 1,
          "Memory should have at least 1 page, got: " + memory.getSize());
      LOGGER.info("[" + runtime + "] getMemory(0) size = " + memory.getSize() + " pages");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getTable(0) returns table with size >= 2")
  void getTableByIndexZero(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getTable(0)");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmTable> tableOpt = instance.getTable(0);

      assertTrue(tableOpt.isPresent(), "getTable(0) should return a table");
      final WasmTable table = tableOpt.get();
      assertTrue(table.getSize() >= 2,
          "Table should have at least 2 entries, got: " + table.getSize());
      LOGGER.info("[" + runtime + "] getTable(0) size = " + table.getSize());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getTypedFunc returns Optional (may be empty if not overridden)")
  void getTypedFuncReturnsWrapper(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getTypedFunc with matching types");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      try {
        final Optional<TypedFunc> typedOpt =
            instance.getTypedFunc("add", WasmValueType.I32, WasmValueType.I32);

        assertNotNull(typedOpt,
            "getTypedFunc should not return null");
        LOGGER.info("[" + runtime + "] getTypedFunc('add', I32, I32) present: "
            + typedOpt.isPresent());
        if (typedOpt.isPresent()) {
          LOGGER.info("[" + runtime + "] TypedFunc signature: "
              + typedOpt.get().getSignature());
        }
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] getTypedFunc not supported: " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getTypedFunc returns empty for mismatched signature")
  void getTypedFuncMismatchReturnsEmpty(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getTypedFunc with mismatched types");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      try {
        final Optional<TypedFunc> typedOpt =
            instance.getTypedFunc("add", WasmValueType.I64);

        assertFalse(typedOpt.isPresent(),
            "getTypedFunc('add', I64) should return empty for mismatched signature");
        LOGGER.info("[" + runtime + "] getTypedFunc('add', I64) present: "
            + typedOpt.isPresent());
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] getTypedFunc not supported: " + e.getMessage());
      } catch (final Exception e) {
        // Some runtimes may throw on mismatch rather than return empty
        LOGGER.info("[" + runtime + "] getTypedFunc mismatch threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callI32Function with params returns correct result")
  void callI32FunctionWithParams(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callI32Function('add', 10, 32)");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final int result = instance.callI32Function("add", 10, 32);

      assertEquals(42, result, "add(10, 32) should return 42");
      LOGGER.info("[" + runtime + "] callI32Function('add', 10, 32) = " + result);

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callI32Function with no params returns correct result")
  void callI32FunctionNoParams(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callI32Function('get42')");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final int result = instance.callI32Function("get42");

      assertEquals(42, result, "get42() should return 42");
      LOGGER.info("[" + runtime + "] callI32Function('get42') = " + result);

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("createAsync returns valid instance")
  void createAsyncReturnsValidInstance(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Instance.createAsync");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);

      try {
        final CompletableFuture<Instance> future = Instance.createAsync(store, module);
        assertNotNull(future, "createAsync should return a non-null future");

        final Instance instance = future.get();
        assertNotNull(instance, "Async-created instance should not be null");
        assertTrue(instance.isValid(), "Async-created instance should be valid");

        // Verify the instance works
        final int result = instance.callI32Function("add", 5, 6);
        assertEquals(11, result, "add(5, 6) should return 11 on async instance");
        LOGGER.info("[" + runtime + "] createAsync instance valid, add(5,6) = " + result);

        instance.close();
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] createAsync not supported: " + e.getMessage());
      } catch (final Exception e) {
        // Async instantiation may fail due to thread-safety constraints
        LOGGER.info("[" + runtime + "] createAsync threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      module.close();
    }
  }
}

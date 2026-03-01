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

package ai.tegmentum.wasmtime4j.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for input validation error handling.
 *
 * <p>These tests verify that the runtime properly validates inputs and throws appropriate
 * exceptions for null, invalid, or out-of-bounds parameters.
 */
@DisplayName("Invalid Input Validation Tests")
@Tag("integration")
class InvalidInputValidationTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(InvalidInputValidationTest.class.getName());

  private Engine engine;
  private Store store;

  // Simple WASM with function, memory, and table
  private static final byte[] SIMPLE_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version

        // Type section (id=1)
        0x01,
        0x07, // section size
        0x02, // number of types
        0x60,
        0x01,
        0x7F,
        0x01,
        0x7F, // type 0: i32 -> i32
        0x60,
        0x00,
        0x00, // type 1: () -> ()

        // Function section (id=3)
        0x03,
        0x03, // section size
        0x02, // number of functions
        0x00, // add1: type 0
        0x01, // noop: type 1

        // Memory section (id=5)
        0x05,
        0x03, // section size
        0x01, // number of memories
        0x00, // limits: no max
        0x01, // min: 1 page

        // Export section (id=7)
        0x07,
        0x15, // section size
        0x03, // number of exports
        0x04, // name length
        'a',
        'd',
        'd',
        '1',
        0x00, // export kind: function
        0x00, // function index
        0x04, // name length
        'n',
        'o',
        'o',
        'p',
        0x00, // export kind: function
        0x01, // function index
        0x06, // name length
        'm',
        'e',
        'm',
        'o',
        'r',
        'y',
        0x02, // export kind: memory
        0x00, // memory index

        // Code section (id=10)
        0x0A,
        0x0C, // section size
        0x02, // number of functions

        // add1 function
        0x07, // body size
        0x00, // locals count
        0x20,
        0x00, // local.get 0
        0x41,
        0x01, // i32.const 1
        0x6A, // i32.add
        0x0B, // end

        // noop function
        0x02, // body size
        0x00, // locals count
        0x0B // end
      };

  @AfterEach
  void tearDown() {
    if (store != null) {
      try {
        store.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing store: " + e.getMessage());
      }
    }
    if (engine != null) {
      try {
        engine.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing engine: " + e.getMessage());
      }
    }
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Null Parameter Validation Tests")
  class NullParameterValidationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for null WASM bytes")
    void shouldThrowExceptionForNullWasmBytes(final RuntimeType runtime) {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      assertThatThrownBy(() -> engine.compileModule(null))
          .satisfies(
              e -> {
                LOGGER.info("Exception for null WASM: " + e.getClass().getName());
                assertThat(e)
                    .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
              });
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for null linker in instantiate")
    void shouldThrowExceptionForNullLinker(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(SIMPLE_WASM);
      try {
        assertThatThrownBy(() -> ((Linker) null).instantiate(store, module))
            .isInstanceOf(NullPointerException.class);
      } finally {
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for null store in instantiate")
    void shouldThrowExceptionForNullStore(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(SIMPLE_WASM);
      try {
        assertThatThrownBy(() -> ((Store) null).createInstance(module))
            .isInstanceOf(NullPointerException.class);
      } finally {
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for null function name lookup")
    void shouldThrowExceptionForNullFunctionName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(SIMPLE_WASM);
      try {
        final Instance instance = store.createInstance(module);

        assertThatThrownBy(() -> instance.getFunction(null))
            .satisfies(
                e -> {
                  LOGGER.info("Exception for null function name: " + e.getClass().getName());
                  assertThat(e)
                      .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
                });
      } finally {
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for null global value type")
    void shouldThrowExceptionForNullGlobalValueType(final RuntimeType runtime) {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      assertThatThrownBy(() -> store.createGlobal(null, true, WasmValue.i32(0)))
          .satisfies(
              e -> {
                LOGGER.info("Exception for null value type: " + e.getClass().getName());
                assertThat(e)
                    .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
              });
    }
  }

  @Nested
  @DisplayName("Invalid Function Call Parameter Tests")
  class InvalidFunctionCallParameterTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for wrong number of parameters")
    void shouldThrowExceptionForWrongNumberOfParameters(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(SIMPLE_WASM);
      try {
        final Instance instance = store.createInstance(module);
        final WasmFunction add1Func = instance.getFunction("add1").orElse(null);

        assertThat(add1Func).isNotNull();

        // add1 expects 1 parameter, calling with 0
        assertThatThrownBy(() -> add1Func.call())
            .satisfies(
                e -> {
                  LOGGER.info("Exception for missing parameter: " + e.getClass().getName());
                  LOGGER.info("Message: " + e.getMessage());
                });

        // add1 expects 1 parameter, calling with 2
        assertThatThrownBy(() -> add1Func.call(WasmValue.i32(1), WasmValue.i32(2)))
            .satisfies(
                e -> {
                  LOGGER.info("Exception for extra parameter: " + e.getClass().getName());
                  LOGGER.info("Message: " + e.getMessage());
                });
      } finally {
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for wrong parameter type")
    void shouldThrowExceptionForWrongParameterType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(SIMPLE_WASM);
      try {
        final Instance instance = store.createInstance(module);
        final WasmFunction add1Func = instance.getFunction("add1").orElse(null);

        assertThat(add1Func).isNotNull();

        // add1 expects i32, passing an f64 (wrong type)
        assertThatThrownBy(() -> add1Func.call(WasmValue.f64(3.14)))
            .satisfies(
                e -> {
                  LOGGER.info("Exception for wrong type: " + e.getClass().getName());
                  LOGGER.info("Message: " + e.getMessage());
                });
      } finally {
        module.close();
      }
    }
  }

  @Nested
  @DisplayName("Export Name Validation Tests")
  class ExportNameValidationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return empty for non-existent function")
    void shouldReturnEmptyForNonExistentFunction(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(SIMPLE_WASM);
      try {
        final Instance instance = store.createInstance(module);
        final java.util.Optional<WasmFunction> nonExistent = instance.getFunction("doesNotExist");

        // Should return empty Optional, not throw exception
        assertThat(nonExistent).isEmpty();
        LOGGER.info("Non-existent function correctly returned empty");
      } finally {
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return empty for non-existent memory")
    void shouldReturnEmptyForNonExistentMemory(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(SIMPLE_WASM);
      try {
        final Instance instance = store.createInstance(module);
        final java.util.Optional<WasmMemory> nonExistent = instance.getMemory("doesNotExist");

        // Should return empty Optional, not throw exception
        assertThat(nonExistent).isEmpty();
        LOGGER.info("Non-existent memory correctly returned empty");
      } finally {
        module.close();
      }
    }
  }

  @Nested
  @DisplayName("Memory Access Validation Tests")
  class MemoryAccessValidationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for negative memory offset")
    void shouldThrowExceptionForNegativeMemoryOffset(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(SIMPLE_WASM);
      try {
        final Instance instance = store.createInstance(module);
        final WasmMemory memory = instance.getMemory("memory").orElse(null);

        assertThat(memory).isNotNull();

        assertThatThrownBy(() -> memory.readByte(-1))
            .satisfies(
                e -> {
                  LOGGER.info("Exception for negative offset: " + e.getClass().getName());
                  LOGGER.info("Message: " + e.getMessage());
                });
      } finally {
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for out of bounds memory access")
    void shouldThrowExceptionForOutOfBoundsMemoryAccess(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(SIMPLE_WASM);
      try {
        final Instance instance = store.createInstance(module);
        final WasmMemory memory = instance.getMemory("memory").orElse(null);

        assertThat(memory).isNotNull();

        // Memory is 1 page (65536 bytes), access beyond that
        final long outOfBoundsOffset = 65536 * 2;

        assertThatThrownBy(() -> memory.readByte((int) outOfBoundsOffset))
            .satisfies(
                e -> {
                  LOGGER.info("Exception for OOB access: " + e.getClass().getName());
                  LOGGER.info("Message: " + e.getMessage());
                });
      } finally {
        module.close();
      }
    }
  }

  @Nested
  @DisplayName("Table Access Validation Tests")
  class TableAccessValidationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for negative table index")
    void shouldThrowExceptionForNegativeTableIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final WasmTable table = store.createTable(WasmValueType.FUNCREF, 10, 100);

      assertThat(table).isNotNull();

      assertThatThrownBy(() -> table.get(-1))
          .satisfies(
              e -> {
                LOGGER.info("Exception for negative index: " + e.getClass().getName());
                LOGGER.info("Message: " + e.getMessage());
              });
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for out of bounds table index")
    void shouldThrowExceptionForOutOfBoundsTableIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final WasmTable table = store.createTable(WasmValueType.FUNCREF, 10, 100);

      assertThat(table).isNotNull();
      assertThat(table.getSize()).isEqualTo(10);

      // Index 10 is out of bounds (valid indices are 0-9)
      assertThatThrownBy(() -> table.get(10))
          .satisfies(
              e -> {
                LOGGER.info("Exception for OOB table index: " + e.getClass().getName());
                LOGGER.info("Message: " + e.getMessage());
              });
    }
  }

  @Nested
  @DisplayName("Global Value Type Validation Tests")
  class GlobalValueTypeValidationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for setting immutable global")
    void shouldThrowExceptionForSettingImmutableGlobal(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final WasmGlobal immutableGlobal =
          store.createGlobal(WasmValueType.I32, false, WasmValue.i32(42));

      assertThat(immutableGlobal).isNotNull();
      assertThat(immutableGlobal.isMutable()).isFalse();

      assertThatThrownBy(() -> immutableGlobal.set(WasmValue.i32(100)))
          .satisfies(
              e -> {
                LOGGER.info("Exception for immutable global: " + e.getClass().getName());
                LOGGER.info("Message: " + e.getMessage());
              });
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for wrong global type access")
    void shouldThrowExceptionForWrongGlobalTypeAccess(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final WasmGlobal i32Global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(42));

      assertThat(i32Global).isNotNull();

      // Try to read i64 from an i32 global - should throw when extracting wrong type
      assertThatThrownBy(() -> i32Global.get().asLong())
          .satisfies(
              e -> {
                LOGGER.info("Exception for wrong type access: " + e.getClass().getName());
                LOGGER.info("Message: " + e.getMessage());
              });
    }
  }

  @Nested
  @DisplayName("Invalid Configuration Tests")
  class InvalidConfigurationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for invalid table size")
    void shouldThrowExceptionForInvalidTableSize(final RuntimeType runtime) {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      // Max size less than initial size
      assertThatThrownBy(() -> store.createTable(WasmValueType.FUNCREF, 100, 10))
          .satisfies(
              e -> {
                LOGGER.info("Exception for invalid table size: " + e.getClass().getName());
                LOGGER.info("Message: " + e.getMessage());
              });
    }
  }
}

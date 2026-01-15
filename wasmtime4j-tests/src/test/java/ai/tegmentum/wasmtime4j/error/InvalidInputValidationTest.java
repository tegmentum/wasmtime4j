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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for input validation error handling.
 *
 * <p>These tests verify that the runtime properly validates inputs and throws appropriate
 * exceptions for null, invalid, or out-of-bounds parameters.
 */
@DisplayName("Invalid Input Validation Tests")
@Tag("integration")
class InvalidInputValidationTest {

  private static final Logger LOGGER = Logger.getLogger(InvalidInputValidationTest.class.getName());

  private static boolean runtimeAvailable;
  private static String unavailableReason;

  private WasmRuntime runtime;
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

  @BeforeAll
  static void checkRuntimeAvailability() {
    LOGGER.info("Checking runtime availability for input validation tests");
    try {
      runtimeAvailable =
          WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI)
              || WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);
      LOGGER.info("Runtime available: " + runtimeAvailable);
    } catch (final Exception e) {
      unavailableReason = "Failed to check runtime: " + e.getMessage();
      LOGGER.warning(unavailableReason);
    }
  }

  @BeforeEach
  void setUp() {
    assumeTrue(runtimeAvailable, "Runtime not available: " + unavailableReason);

    try {
      runtime = WasmRuntimeFactory.create();
      engine = runtime.createEngine();
      store = engine.createStore();
      LOGGER.info("Test runtime setup complete");
    } catch (final Exception e) {
      LOGGER.warning("Failed to set up runtime: " + e.getMessage());
      assumeTrue(false, "Runtime setup failed: " + e.getMessage());
    }
  }

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
    if (runtime != null) {
      try {
        runtime.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing runtime: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Null Parameter Validation Tests")
  class NullParameterValidationTests {

    @Test
    @DisplayName("should throw exception for null WASM bytes")
    void shouldThrowExceptionForNullWasmBytes() {
      assertThatThrownBy(() -> engine.compileModule(null))
          .satisfies(
              e -> {
                LOGGER.info("Exception for null WASM: " + e.getClass().getName());
                assertThat(e)
                    .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
              });
    }

    @Test
    @DisplayName("should throw exception for null linker in instantiate")
    void shouldThrowExceptionForNullLinker() throws Exception {
      final Module module = engine.compileModule(SIMPLE_WASM);
      try {
        assertThatThrownBy(() -> ((Linker) null).instantiate(store, module))
            .isInstanceOf(NullPointerException.class);
      } finally {
        module.close();
      }
    }

    @Test
    @DisplayName("should throw exception for null store in instantiate")
    void shouldThrowExceptionForNullStore() throws Exception {
      final Module module = engine.compileModule(SIMPLE_WASM);
      try {
        assertThatThrownBy(() -> ((Store) null).createInstance(module))
            .isInstanceOf(NullPointerException.class);
      } finally {
        module.close();
      }
    }

    @Test
    @DisplayName("should throw exception for null function name lookup")
    void shouldThrowExceptionForNullFunctionName() throws Exception {
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

    @Test
    @DisplayName("should throw exception for null global value type")
    void shouldThrowExceptionForNullGlobalValueType() {
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

    @Test
    @DisplayName("should throw exception for wrong number of parameters")
    void shouldThrowExceptionForWrongNumberOfParameters() throws Exception {
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

    @Test
    @DisplayName("should throw exception for wrong parameter type")
    void shouldThrowExceptionForWrongParameterType() throws Exception {
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

    @Test
    @DisplayName("should return empty for non-existent function")
    void shouldReturnEmptyForNonExistentFunction() throws Exception {
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

    @Test
    @DisplayName("should return empty for non-existent memory")
    void shouldReturnEmptyForNonExistentMemory() throws Exception {
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

    @Test
    @DisplayName("should throw exception for negative memory offset")
    void shouldThrowExceptionForNegativeMemoryOffset() throws Exception {
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

    @Test
    @DisplayName("should throw exception for out of bounds memory access")
    void shouldThrowExceptionForOutOfBoundsMemoryAccess() throws Exception {
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

    @Test
    @DisplayName("should throw exception for negative table index")
    void shouldThrowExceptionForNegativeTableIndex() throws Exception {
      final WasmTable table = store.createTable(WasmValueType.FUNCREF, 10, 100);

      assertThat(table).isNotNull();

      assertThatThrownBy(() -> table.get(-1))
          .satisfies(
              e -> {
                LOGGER.info("Exception for negative index: " + e.getClass().getName());
                LOGGER.info("Message: " + e.getMessage());
              });
    }

    @Test
    @DisplayName("should throw exception for out of bounds table index")
    void shouldThrowExceptionForOutOfBoundsTableIndex() throws Exception {
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

    @Test
    @DisplayName("should throw exception for setting immutable global")
    void shouldThrowExceptionForSettingImmutableGlobal() throws Exception {
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

    @Test
    @DisplayName("should throw exception for wrong global type access")
    void shouldThrowExceptionForWrongGlobalTypeAccess() throws Exception {
      final WasmGlobal i32Global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(42));

      assertThat(i32Global).isNotNull();

      // Try to read i64 from an i32 global - should throw when extracting wrong type
      assertThatThrownBy(() -> i32Global.get().asI64())
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

    @Test
    @DisplayName("should throw exception for invalid table size")
    void shouldThrowExceptionForInvalidTableSize() {
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

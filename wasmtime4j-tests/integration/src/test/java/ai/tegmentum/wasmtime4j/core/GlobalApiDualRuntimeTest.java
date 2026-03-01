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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmTypeException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Dual-runtime integration tests for generic WasmGlobal API operations.
 *
 * <p>These tests verify global get/set behavior, type checking, mutability, and boundary values
 * across both JNI and Panama runtimes using the unified API.
 *
 * @since 1.0.0
 */
@DisplayName("Global API DualRuntime Tests")
@SuppressWarnings("deprecation")
public class GlobalApiDualRuntimeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(GlobalApiDualRuntimeTest.class.getName());

  /** WAT module with mutable i32 global initialized to 42. */
  private static final String GLOBAL_I32_WAT =
      "(module\n" + "  (global $g_i32 (export \"g_i32\") (mut i32) (i32.const 42))\n" + ")";

  /** WAT module with mutable i64 global initialized to 100. */
  private static final String GLOBAL_I64_WAT =
      "(module\n" + "  (global $g_i64 (export \"g_i64\") (mut i64) (i64.const 100))\n" + ")";

  /** WAT module with mutable f32 global initialized to approximately 3.14. */
  private static final String GLOBAL_F32_WAT =
      "(module\n" + "  (global $g_f32 (export \"g_f32\") (mut f32) (f32.const 3.14))\n" + ")";

  /** WAT module with mutable globals of all three types for multi-type tests. */
  private static final String GLOBAL_ALL_TYPES_WAT =
      "(module\n"
          + "  (global $g_i32 (export \"g_i32\") (mut i32) (i32.const 42))\n"
          + "  (global $g_i64 (export \"g_i64\") (mut i64) (i64.const 100))\n"
          + "  (global $g_f32 (export \"g_f32\") (mut f32) (f32.const 3.14))\n"
          + ")";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  // ==================== Get Global Value Tests ====================

  @Nested
  @DisplayName("Get Global Value Tests")
  class GetGlobalValueTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Get I32 global value")
    void shouldGetI32GlobalValue(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing get I32 global value");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_I32_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
        assertTrue(globalOpt.isPresent(), "Global g_i32 should be present");

        final WasmGlobal global = globalOpt.get();
        assertEquals(WasmValueType.I32, global.getType(), "Global should be I32 type");
        assertTrue(global.isMutable(), "Global should be mutable");

        final WasmValue value = global.get();
        assertEquals(WasmValueType.I32, value.getType(), "Value should be I32 type");
        assertEquals(42, value.asInt(), "Initial value should be 42");
        LOGGER.info("[" + runtime + "] I32 global value: " + value.asInt());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Get I64 global value")
    void shouldGetI64GlobalValue(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing get I64 global value");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_I64_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i64");
        assertTrue(globalOpt.isPresent(), "Global g_i64 should be present");

        final WasmGlobal global = globalOpt.get();
        assertEquals(WasmValueType.I64, global.getType(), "Global should be I64 type");
        assertTrue(global.isMutable(), "Global should be mutable");

        final WasmValue value = global.get();
        assertEquals(WasmValueType.I64, value.getType(), "Value should be I64 type");
        assertEquals(100L, value.asLong(), "Initial value should be 100");
        LOGGER.info("[" + runtime + "] I64 global value: " + value.asLong());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Get F32 global value")
    void shouldGetF32GlobalValue(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing get F32 global value");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_F32_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_f32");
        assertTrue(globalOpt.isPresent(), "Global g_f32 should be present");

        final WasmGlobal global = globalOpt.get();
        assertEquals(WasmValueType.F32, global.getType(), "Global should be F32 type");
        assertTrue(global.isMutable(), "Global should be mutable");

        final WasmValue value = global.get();
        assertEquals(WasmValueType.F32, value.getType(), "Value should be F32 type");
        assertEquals(3.14f, value.asFloat(), 0.01f, "Initial value should be approximately 3.14");
        LOGGER.info("[" + runtime + "] F32 global value: " + value.asFloat());
      }
    }
  }

  // ==================== Set Global Value Tests ====================

  @Nested
  @DisplayName("Set Global Value Tests")
  class SetGlobalValueTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Set I32 global value")
    void shouldSetI32GlobalValue(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing set I32 global value");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_I32_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
        assertTrue(globalOpt.isPresent());

        final WasmGlobal global = globalOpt.get();
        global.set(WasmValue.i32(123));

        final WasmValue newValue = global.get();
        assertEquals(123, newValue.asInt(), "Value should be updated to 123");
        LOGGER.info("[" + runtime + "] I32 global updated to: " + newValue.asInt());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Set I64 global value")
    void shouldSetI64GlobalValue(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing set I64 global value");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_I64_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i64");
        assertTrue(globalOpt.isPresent());

        final WasmGlobal global = globalOpt.get();
        global.set(WasmValue.i64(999L));

        final WasmValue newValue = global.get();
        assertEquals(999L, newValue.asLong(), "Value should be updated to 999");
        LOGGER.info("[" + runtime + "] I64 global updated to: " + newValue.asLong());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Set F32 global value")
    void shouldSetF32GlobalValue(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing set F32 global value");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_F32_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_f32");
        assertTrue(globalOpt.isPresent());

        final WasmGlobal global = globalOpt.get();
        global.set(WasmValue.f32(1.23f));

        final WasmValue newValue = global.get();
        assertEquals(
            1.23f, newValue.asFloat(), 0.01f, "Value should be updated to approximately 1.23");
        LOGGER.info("[" + runtime + "] F32 global updated to: " + newValue.asFloat());
      }
    }
  }

  // ==================== Error Handling Tests ====================

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Set global with wrong type throws WasmTypeException")
    void shouldThrowOnWrongType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing set global with wrong type");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_I32_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
        assertTrue(globalOpt.isPresent());

        final WasmGlobal global = globalOpt.get();
        assertThrows(
            WasmTypeException.class,
            () -> global.set(WasmValue.i64(999L)),
            "Setting global with wrong type should throw WasmTypeException");
        LOGGER.info("[" + runtime + "] WasmTypeException thrown as expected for type mismatch");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Set null value throws IllegalArgumentException")
    void shouldThrowOnNullValue(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing set null value");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_I32_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
        assertTrue(globalOpt.isPresent());

        final WasmGlobal global = globalOpt.get();
        assertThrows(
            IllegalArgumentException.class,
            () -> global.set(null),
            "Setting null value should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] IllegalArgumentException thrown as expected for null value");
      }
    }
  }

  // ==================== Multiple Set Operations Tests ====================

  @Nested
  @DisplayName("Multiple Set Operations Tests")
  class MultipleSetOperationsTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Multiple set operations on same global")
    void shouldSupportMultipleSets(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multiple set operations");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_I32_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
        assertTrue(globalOpt.isPresent());

        final WasmGlobal global = globalOpt.get();

        global.set(WasmValue.i32(1));
        assertEquals(1, global.get().asInt());
        LOGGER.info("[" + runtime + "] Set to 1, got: " + global.get().asInt());

        global.set(WasmValue.i32(2));
        assertEquals(2, global.get().asInt());
        LOGGER.info("[" + runtime + "] Set to 2, got: " + global.get().asInt());

        global.set(WasmValue.i32(3));
        assertEquals(3, global.get().asInt());
        LOGGER.info("[" + runtime + "] Set to 3, got: " + global.get().asInt());
      }
    }
  }

  // ==================== Negative Value Tests ====================

  @Nested
  @DisplayName("Negative Value Tests")
  class NegativeValueTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Negative I32 global value")
    void shouldSupportNegativeI32(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing negative I32 global value");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_I32_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_i32");
        assertTrue(globalOpt.isPresent());

        final WasmGlobal global = globalOpt.get();
        global.set(WasmValue.i32(-456));

        final WasmValue value = global.get();
        assertEquals(-456, value.asInt(), "Value should be -456");
        LOGGER.info("[" + runtime + "] Negative I32 value: " + value.asInt());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Negative F32 global value")
    void shouldSupportNegativeF32(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing negative F32 global value");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_F32_WAT);
          Instance instance = module.instantiate(store)) {

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("g_f32");
        assertTrue(globalOpt.isPresent());

        final WasmGlobal global = globalOpt.get();
        global.set(WasmValue.f32(-3.14f));

        final WasmValue value = global.get();
        assertEquals(-3.14f, value.asFloat(), 0.01f, "Value should be approximately -3.14");
        LOGGER.info("[" + runtime + "] Negative F32 value: " + value.asFloat());
      }
    }
  }

  // ==================== Zero Value Tests ====================

  @Nested
  @DisplayName("Zero Value Tests")
  class ZeroValueTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Zero values for all types")
    void shouldSupportZeroValues(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing zero values for all types");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_ALL_TYPES_WAT);
          Instance instance = module.instantiate(store)) {

        // Test I32 zero
        final Optional<WasmGlobal> i32Opt = instance.getGlobal("g_i32");
        assertTrue(i32Opt.isPresent());
        i32Opt.get().set(WasmValue.i32(0));
        assertEquals(0, i32Opt.get().get().asInt());
        LOGGER.info("[" + runtime + "] I32 zero value verified");

        // Test I64 zero
        final Optional<WasmGlobal> i64Opt = instance.getGlobal("g_i64");
        assertTrue(i64Opt.isPresent());
        i64Opt.get().set(WasmValue.i64(0L));
        assertEquals(0L, i64Opt.get().get().asLong());
        LOGGER.info("[" + runtime + "] I64 zero value verified");

        // Test F32 zero
        final Optional<WasmGlobal> f32Opt = instance.getGlobal("g_f32");
        assertTrue(f32Opt.isPresent());
        f32Opt.get().set(WasmValue.f32(0.0f));
        assertEquals(0.0f, f32Opt.get().get().asFloat(), 0.0001f);
        LOGGER.info("[" + runtime + "] F32 zero value verified");
      }
    }
  }

  // ==================== Boundary Value Tests ====================

  @Nested
  @DisplayName("Boundary Value Tests")
  class BoundaryValueTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Max values for integer types")
    void shouldSupportMaxIntegerValues(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing max integer values");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_ALL_TYPES_WAT);
          Instance instance = module.instantiate(store)) {

        // Test I32 max
        final Optional<WasmGlobal> i32Opt = instance.getGlobal("g_i32");
        assertTrue(i32Opt.isPresent());
        i32Opt.get().set(WasmValue.i32(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, i32Opt.get().get().asInt());
        LOGGER.info("[" + runtime + "] I32 max value verified: " + Integer.MAX_VALUE);

        // Test I64 max
        final Optional<WasmGlobal> i64Opt = instance.getGlobal("g_i64");
        assertTrue(i64Opt.isPresent());
        i64Opt.get().set(WasmValue.i64(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, i64Opt.get().get().asLong());
        LOGGER.info("[" + runtime + "] I64 max value verified: " + Long.MAX_VALUE);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Min values for integer types")
    void shouldSupportMinIntegerValues(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing min integer values");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(GLOBAL_ALL_TYPES_WAT);
          Instance instance = module.instantiate(store)) {

        // Test I32 min
        final Optional<WasmGlobal> i32Opt = instance.getGlobal("g_i32");
        assertTrue(i32Opt.isPresent());
        i32Opt.get().set(WasmValue.i32(Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, i32Opt.get().get().asInt());
        LOGGER.info("[" + runtime + "] I32 min value verified: " + Integer.MIN_VALUE);

        // Test I64 min
        final Optional<WasmGlobal> i64Opt = instance.getGlobal("g_i64");
        assertTrue(i64Opt.isPresent());
        i64Opt.get().set(WasmValue.i64(Long.MIN_VALUE));
        assertEquals(Long.MIN_VALUE, i64Opt.get().get().asLong());
        LOGGER.info("[" + runtime + "] I64 min value verified: " + Long.MIN_VALUE);
      }
    }
  }
}

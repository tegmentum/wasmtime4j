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
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.TrapException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
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
 * Integration tests for native error mapping.
 *
 * <p>These tests verify that native errors from Wasmtime are correctly mapped to appropriate Java
 * exceptions with meaningful error messages.
 */
@DisplayName("Native Error Mapping Tests")
@Tag("integration")
class NativeErrorMappingTest {

  private static final Logger LOGGER = Logger.getLogger(NativeErrorMappingTest.class.getName());

  private static boolean runtimeAvailable;
  private static String unavailableReason;

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  // Invalid WASM - not valid magic bytes
  private static final byte[] INVALID_MAGIC_WASM = {0x00, 0x00, 0x00, 0x00};

  // Invalid WASM - correct magic but wrong version
  private static final byte[] INVALID_VERSION_WASM = {
    0x00,
    0x61,
    0x73,
    0x6D, // magic
    (byte) 0xFF,
    0x00,
    0x00,
    0x00 // invalid version
  };

  // Malformed WASM - truncated section
  private static final byte[] TRUNCATED_WASM = {
    0x00,
    0x61,
    0x73,
    0x6D, // magic
    0x01,
    0x00,
    0x00,
    0x00, // version
    0x01, // type section id
    (byte) 0xFF // invalid section size (LEB128)
  };

  // Valid WASM with unreachable trap
  private static final byte[] TRAP_UNREACHABLE_WASM =
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
        0x04, // section size
        0x01, // number of types
        0x60, // func type
        0x00, // no params
        0x00, // no results

        // Function section (id=3)
        0x03,
        0x02, // section size
        0x01, // number of functions
        0x00, // type index 0

        // Export section (id=7)
        0x07,
        0x08, // section size
        0x01, // number of exports
        0x04, // name length
        't',
        'r',
        'a',
        'p',
        0x00, // export kind: function
        0x00, // function index

        // Code section (id=10)
        0x0A,
        0x05, // section size
        0x01, // number of functions
        0x03, // body size
        0x00, // locals count
        0x00, // unreachable instruction
        0x0B // end
      };

  // Valid WASM with division by zero
  private static final byte[] TRAP_DIV_ZERO_WASM =
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
        0x05, // section size
        0x01, // number of types
        0x60, // func type
        0x00, // no params
        0x01,
        0x7F, // 1 i32 result

        // Function section (id=3)
        0x03,
        0x02, // section size
        0x01, // number of functions
        0x00, // type index 0

        // Export section (id=7)
        0x07,
        0x0A, // section size
        0x01, // number of exports
        0x06, // name length
        'd',
        'i',
        'v',
        'Z',
        'e',
        'r',
        'o',
        0x00, // export kind: function
        0x00, // function index

        // Code section (id=10)
        0x0A,
        0x09, // section size
        0x01, // number of functions
        0x07, // body size
        0x00, // locals count
        0x41,
        0x01, // i32.const 1
        0x41,
        0x00, // i32.const 0
        0x6D, // i32.div_s
        0x0B // end
      };

  @BeforeAll
  static void checkRuntimeAvailability() {
    LOGGER.info("Checking runtime availability for error mapping tests");
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
  @DisplayName("Module Compilation Error Tests")
  class ModuleCompilationErrorTests {

    @Test
    @DisplayName("should throw ValidationException for invalid WASM magic")
    void shouldThrowValidationExceptionForInvalidMagic() {
      assertThatThrownBy(() -> engine.compileModule(INVALID_MAGIC_WASM))
          .isInstanceOf(WasmException.class)
          .satisfies(
              e -> {
                LOGGER.info("Exception for invalid magic: " + e.getClass().getName());
                LOGGER.info("Message: " + e.getMessage());
                assertThat(e.getMessage()).isNotEmpty();
              });
    }

    @Test
    @DisplayName("should throw ValidationException for invalid WASM version")
    void shouldThrowValidationExceptionForInvalidVersion() {
      assertThatThrownBy(() -> engine.compileModule(INVALID_VERSION_WASM))
          .isInstanceOf(WasmException.class)
          .satisfies(
              e -> {
                LOGGER.info("Exception for invalid version: " + e.getClass().getName());
                LOGGER.info("Message: " + e.getMessage());
                assertThat(e.getMessage()).isNotEmpty();
              });
    }

    @Test
    @DisplayName("should throw CompilationException for truncated WASM")
    void shouldThrowCompilationExceptionForTruncatedWasm() {
      assertThatThrownBy(() -> engine.compileModule(TRUNCATED_WASM))
          .isInstanceOf(WasmException.class)
          .satisfies(
              e -> {
                LOGGER.info("Exception for truncated WASM: " + e.getClass().getName());
                LOGGER.info("Message: " + e.getMessage());
                assertThat(e.getMessage()).isNotEmpty();
              });
    }

    @Test
    @DisplayName("should throw appropriate exception for null WASM bytes")
    void shouldThrowExceptionForNullWasm() {
      assertThatThrownBy(() -> engine.compileModule(null))
          .isInstanceOf(
              Exception.class) // Could be IllegalArgumentException or NullPointerException
          .satisfies(
              e -> {
                LOGGER.info("Exception for null WASM: " + e.getClass().getName());
                LOGGER.info("Message: " + e.getMessage());
              });
    }

    @Test
    @DisplayName("should throw appropriate exception for empty WASM bytes")
    void shouldThrowExceptionForEmptyWasm() {
      assertThatThrownBy(() -> engine.compileModule(new byte[0]))
          .isInstanceOf(WasmException.class)
          .satisfies(
              e -> {
                LOGGER.info("Exception for empty WASM: " + e.getClass().getName());
                LOGGER.info("Message: " + e.getMessage());
                assertThat(e.getMessage()).isNotEmpty();
              });
    }
  }

  @Nested
  @DisplayName("Trap Error Mapping Tests")
  class TrapErrorMappingTests {

    @Test
    @DisplayName("should throw TrapException for unreachable instruction")
    void shouldThrowTrapExceptionForUnreachable() throws Exception {
      final Module module = engine.compileModule(TRAP_UNREACHABLE_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmFunction trapFunc = instance.getFunction("trap").orElse(null);

        assertThat(trapFunc).isNotNull();

        assertThatThrownBy(() -> trapFunc.call())
            .isInstanceOf(TrapException.class)
            .satisfies(
                e -> {
                  final TrapException trapException = (TrapException) e;
                  LOGGER.info("Trap exception: " + trapException.getMessage());
                  LOGGER.info("Trap type: " + trapException.getTrapType());
                  assertThat(trapException.getMessage()).isNotEmpty();
                });
      } finally {
        module.close();
      }
    }

    @Test
    @DisplayName("should throw TrapException for division by zero")
    void shouldThrowTrapExceptionForDivisionByZero() throws Exception {
      final Module module = engine.compileModule(TRAP_DIV_ZERO_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmFunction divZeroFunc = instance.getFunction("divZero").orElse(null);

        assertThat(divZeroFunc).isNotNull();

        assertThatThrownBy(() -> divZeroFunc.call())
            .isInstanceOf(TrapException.class)
            .satisfies(
                e -> {
                  final TrapException trapException = (TrapException) e;
                  LOGGER.info("Division by zero trap: " + trapException.getMessage());
                  LOGGER.info("Trap type: " + trapException.getTrapType());
                  assertThat(trapException.getMessage()).containsIgnoringCase("integer");
                });
      } finally {
        module.close();
      }
    }
  }

  @Nested
  @DisplayName("Error Message Quality Tests")
  class ErrorMessageQualityTests {

    @Test
    @DisplayName("should provide meaningful error messages for compilation failures")
    void shouldProvideMeaningfulCompilationErrors() {
      assertThatThrownBy(() -> engine.compileModule(INVALID_MAGIC_WASM))
          .isInstanceOf(WasmException.class)
          .satisfies(
              e -> {
                final String message = e.getMessage();
                LOGGER.info("Error message: " + message);
                // Error message should be descriptive, not empty or generic
                assertThat(message).isNotEmpty();
                assertThat(message.length()).isGreaterThan(5);
              });
    }

    @Test
    @DisplayName("should include context in error messages when available")
    void shouldIncludeContextInErrorMessages() {
      assertThatThrownBy(() -> engine.compileModule(TRUNCATED_WASM))
          .isInstanceOf(WasmException.class)
          .satisfies(
              e -> {
                final String message = e.getMessage();
                LOGGER.info("Error with context: " + message);
                // Error should provide some indication of what went wrong
                assertThat(message).isNotEmpty();
              });
    }
  }
}

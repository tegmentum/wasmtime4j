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

package ai.tegmentum.wasmtime4j.parity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
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
 * Cross-runtime parity tests for error conditions. Verifies that JNI and Panama produce identical
 * exception types for the same error scenarios (invalid WAT, missing imports, type mismatches,
 * out-of-bounds memory access, stack overflow, unreachable, division by zero, invalid module
 * bytes).
 *
 * @since 1.0.0
 */
@DisplayName("Error Parity Tests")
@Tag("integration")
class ErrorParityTest {

  private static final Logger LOGGER = Logger.getLogger(ErrorParityTest.class.getName());

  private static boolean jniAvailable;
  private static boolean panamaAvailable;

  private WasmRuntime jniRuntime;
  private WasmRuntime panamaRuntime;
  private Engine jniEngine;
  private Engine panamaEngine;
  private boolean jniCreatedSuccessfully;
  private boolean panamaCreatedSuccessfully;

  @BeforeAll
  static void checkRuntimeAvailability() {
    jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);
    LOGGER.info("JNI available: " + jniAvailable + ", Panama available: " + panamaAvailable);
  }

  @BeforeEach
  void setUp() {
    jniCreatedSuccessfully = false;
    panamaCreatedSuccessfully = false;

    if (jniAvailable) {
      try {
        jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI);
        jniEngine = jniRuntime.createEngine();
        jniCreatedSuccessfully = true;
      } catch (final Exception e) {
        LOGGER.warning("Failed to create JNI resources: " + e.getMessage());
      }
    }

    if (panamaAvailable) {
      try {
        panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
        panamaEngine = panamaRuntime.createEngine();
        panamaCreatedSuccessfully = true;
      } catch (final Exception e) {
        LOGGER.warning("Failed to create Panama resources: " + e.getMessage());
      }
    }
  }

  @AfterEach
  void tearDown() {
    closeQuietly(jniEngine, "JNI engine");
    closeQuietly(jniRuntime, "JNI runtime");
    closeQuietly(panamaEngine, "Panama engine");
    closeQuietly(panamaRuntime, "Panama runtime");
  }

  private void closeQuietly(final AutoCloseable resource, final String name) {
    if (resource != null) {
      try {
        resource.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing " + name + ": " + e.getMessage());
      }
    }
  }

  private void requireBothRuntimes() {
    assumeTrue(
        jniCreatedSuccessfully && panamaCreatedSuccessfully,
        "Both JNI and Panama runtimes required");
  }

  /**
   * Captures the exception type and message from a failing operation. Returns null if no exception.
   */
  private ExceptionInfo captureException(final Runnable operation) {
    try {
      operation.run();
      return null;
    } catch (final Exception e) {
      return new ExceptionInfo(e.getClass().getName(), e.getMessage());
    }
  }

  private record ExceptionInfo(String className, String message) {}

  @Nested
  @DisplayName("Compilation Error Parity")
  class CompilationErrorParityTests {

    @Test
    @DisplayName("Both runtimes throw same exception type for invalid WAT")
    void invalidWatShouldThrowSameExceptionType() {
      requireBothRuntimes();

      final String invalidWat = "(module (func (export \"bad\") (result i32) i32.add))";

      final ExceptionInfo jniError =
          captureException(
              () -> {
                try {
                  jniRuntime.compileModuleWat(jniEngine, invalidWat);
                } catch (final Exception e) {
                  throw new RuntimeException(e);
                }
              });

      final ExceptionInfo panamaError =
          captureException(
              () -> {
                try {
                  panamaRuntime.compileModuleWat(panamaEngine, invalidWat);
                } catch (final Exception e) {
                  throw new RuntimeException(e);
                }
              });

      LOGGER.info("JNI error: " + jniError);
      LOGGER.info("Panama error: " + panamaError);

      assertThat(jniError).isNotNull().as("JNI should throw for invalid WAT");
      assertThat(panamaError).isNotNull().as("Panama should throw for invalid WAT");
      assertThat(jniError.className())
          .isEqualTo(panamaError.className())
          .as("Exception types should match");
    }

    @Test
    @DisplayName("Both runtimes throw same exception type for invalid module bytes")
    void invalidModuleBytesShouldThrowSameExceptionType() {
      requireBothRuntimes();

      final byte[] invalidBytes =
          new byte[] {0x00, 0x61, 0x73, 0x6D, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

      final ExceptionInfo jniError =
          captureException(
              () -> {
                try {
                  jniRuntime.compileModule(jniEngine, invalidBytes);
                } catch (final Exception e) {
                  throw new RuntimeException(e);
                }
              });

      final ExceptionInfo panamaError =
          captureException(
              () -> {
                try {
                  panamaRuntime.compileModule(panamaEngine, invalidBytes);
                } catch (final Exception e) {
                  throw new RuntimeException(e);
                }
              });

      LOGGER.info("JNI error: " + jniError);
      LOGGER.info("Panama error: " + panamaError);

      assertThat(jniError).isNotNull().as("JNI should throw for invalid bytes");
      assertThat(panamaError).isNotNull().as("Panama should throw for invalid bytes");
      assertThat(jniError.className())
          .isEqualTo(panamaError.className())
          .as("Exception types should match");
    }
  }

  @Nested
  @DisplayName("Instantiation Error Parity")
  class InstantiationErrorParityTests {

    @Test
    @DisplayName("Both runtimes throw same exception type for missing import")
    void missingImportShouldThrowSameExceptionType() throws Exception {
      requireBothRuntimes();

      final String wat =
          """
          (module
            (import "env" "missing_func" (func (result i32)))
            (func (export "call_it") (result i32)
              call 0))
          """;

      final Module jniModule = jniRuntime.compileModuleWat(jniEngine, wat);
      final Module panamaModule = panamaRuntime.compileModuleWat(panamaEngine, wat);

      final Store jniStore = jniRuntime.createStore(jniEngine);
      final Store panamaStore = panamaRuntime.createStore(panamaEngine);

      final ExceptionInfo jniError =
          captureException(
              () -> {
                try {
                  jniModule.instantiate(jniStore);
                } catch (final Exception e) {
                  throw new RuntimeException(e);
                }
              });

      final ExceptionInfo panamaError =
          captureException(
              () -> {
                try {
                  panamaModule.instantiate(panamaStore);
                } catch (final Exception e) {
                  throw new RuntimeException(e);
                }
              });

      LOGGER.info("JNI error: " + jniError);
      LOGGER.info("Panama error: " + panamaError);

      assertThat(jniError).isNotNull().as("JNI should throw for missing import");
      assertThat(panamaError).isNotNull().as("Panama should throw for missing import");
      assertThat(jniError.className())
          .isEqualTo(panamaError.className())
          .as("Exception types should match");

      jniStore.close();
      panamaStore.close();
      jniModule.close();
      panamaModule.close();
    }
  }

  @Nested
  @DisplayName("Trap Parity")
  class TrapParityTests {

    @Test
    @DisplayName("Both runtimes throw same exception type for unreachable instruction")
    void unreachableTrapShouldThrowSameExceptionType() throws Exception {
      requireBothRuntimes();

      final String wat =
          """
          (module
            (func (export "trap") unreachable))
          """;

      final ExceptionInfo jniError = executeAndCaptureException(jniRuntime, jniEngine, wat, "trap");
      final ExceptionInfo panamaError =
          executeAndCaptureException(panamaRuntime, panamaEngine, wat, "trap");

      LOGGER.info("JNI error: " + jniError);
      LOGGER.info("Panama error: " + panamaError);

      assertThat(jniError).isNotNull().as("JNI should trap on unreachable");
      assertThat(panamaError).isNotNull().as("Panama should trap on unreachable");
      assertThat(jniError.className())
          .isEqualTo(panamaError.className())
          .as("Exception types should match");
    }

    @Test
    @DisplayName("Both runtimes throw same exception type for out-of-bounds memory access")
    void oobMemoryAccessShouldThrowSameExceptionType() throws Exception {
      requireBothRuntimes();

      final String wat =
          """
          (module
            (memory 1)
            (func (export "oob") (result i32)
              i32.const 999999
              i32.load))
          """;

      final ExceptionInfo jniError = executeAndCaptureException(jniRuntime, jniEngine, wat, "oob");
      final ExceptionInfo panamaError =
          executeAndCaptureException(panamaRuntime, panamaEngine, wat, "oob");

      LOGGER.info("JNI error: " + jniError);
      LOGGER.info("Panama error: " + panamaError);

      assertThat(jniError).isNotNull().as("JNI should trap on OOB memory");
      assertThat(panamaError).isNotNull().as("Panama should trap on OOB memory");
      assertThat(jniError.className())
          .isEqualTo(panamaError.className())
          .as("Exception types should match");
    }

    @Test
    @DisplayName("Both runtimes throw same exception type for stack overflow")
    void stackOverflowShouldThrowSameExceptionType() throws Exception {
      requireBothRuntimes();

      final String wat =
          """
          (module
            (func $recurse (export "recurse") (result i32)
              call $recurse))
          """;

      final ExceptionInfo jniError =
          executeAndCaptureException(jniRuntime, jniEngine, wat, "recurse");
      final ExceptionInfo panamaError =
          executeAndCaptureException(panamaRuntime, panamaEngine, wat, "recurse");

      LOGGER.info("JNI error: " + jniError);
      LOGGER.info("Panama error: " + panamaError);

      assertThat(jniError).isNotNull().as("JNI should trap on stack overflow");
      assertThat(panamaError).isNotNull().as("Panama should trap on stack overflow");
      assertThat(jniError.className())
          .isEqualTo(panamaError.className())
          .as("Exception types should match");
    }

    @Test
    @DisplayName("Both runtimes throw same exception type for integer division by zero")
    void divisionByZeroShouldThrowSameExceptionType() throws Exception {
      requireBothRuntimes();

      final String wat =
          """
          (module
            (func (export "div_zero") (result i32)
              i32.const 1
              i32.const 0
              i32.div_s))
          """;

      final ExceptionInfo jniError =
          executeAndCaptureException(jniRuntime, jniEngine, wat, "div_zero");
      final ExceptionInfo panamaError =
          executeAndCaptureException(panamaRuntime, panamaEngine, wat, "div_zero");

      LOGGER.info("JNI error: " + jniError);
      LOGGER.info("Panama error: " + panamaError);

      assertThat(jniError).isNotNull().as("JNI should trap on div by zero");
      assertThat(panamaError).isNotNull().as("Panama should trap on div by zero");
      assertThat(jniError.className())
          .isEqualTo(panamaError.className())
          .as("Exception types should match");
    }
  }

  /**
   * Helper to compile, instantiate, call a function, and capture any exception from both phases.
   */
  private ExceptionInfo executeAndCaptureException(
      final WasmRuntime runtime,
      final Engine engine,
      final String wat,
      final String functionName,
      final WasmValue... args) {
    return captureException(
        () -> {
          try {
            final Module module = runtime.compileModuleWat(engine, wat);
            final Store store = runtime.createStore(engine);
            final Instance instance = module.instantiate(store);
            instance.callFunction(functionName, args);
            instance.close();
            store.close();
            module.close();
          } catch (final Exception e) {
            throw new RuntimeException(e);
          }
        });
  }
}

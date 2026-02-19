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

package ai.tegmentum.wasmtime4j.jni.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import ai.tegmentum.wasmtime4j.exception.WasmSecurityException;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Comprehensive tests for {@link JniExceptionMapper}. */
@DisplayName("JniExceptionMapper Tests")
class JniExceptionMapperTest {

  @Nested
  @DisplayName("mapNativeError Tests - All Error Codes")
  class MapNativeErrorTests {

    @ParameterizedTest(name = "Error code {0} should produce message containing \"{1}\"")
    @CsvSource({
      "0, No error occurred",
      "-1, WebAssembly compilation failed",
      "-2, WebAssembly module validation failed",
      "-3, WebAssembly runtime error",
      "-4, Engine configuration error",
      "-5, Store error",
      "-6, Instance error",
      "-7, Memory access or allocation error",
      "-8, Function invocation error",
      "-9, Import or export resolution error",
      "-10, Type conversion or validation error",
      "-11, Resource management error",
      "-12, I/O operation error",
      "-13, Invalid parameter",
      "-14, Threading or concurrency error",
      "-15, WASI error",
      "-16, Security and permission violation error",
      "-17, Component model error",
      "-18, Interface definition or binding error",
      "-19, Network operation error",
      "-20, Process execution error",
      "-21, Internal system error",
      "-22, Security violation error",
      "-23, Invalid data format error",
      "-24, I/O operation error",
      "-25, Unsupported operation",
      "-26, Operation would block"
    })
    @DisplayName("Should map all 27 error codes correctly")
    void shouldMapAllErrorCodes(final int errorCode, final String expectedMessagePart) {
      final WasmException ex = JniExceptionMapper.mapNativeError(errorCode, "test detail");
      assertNotNull(ex, "Exception should not be null for error code " + errorCode);
      assertTrue(
          ex.getMessage().contains(expectedMessagePart),
          "Error code "
              + errorCode
              + " should produce message containing '"
              + expectedMessagePart
              + "' but got: "
              + ex.getMessage());
    }

    @Test
    @DisplayName("Error code -1 should map to CompilationException")
    void shouldMapMinusOneToCompilationException() {
      final WasmException ex = JniExceptionMapper.mapNativeError(-1, "compile failed");
      assertTrue(
          ex instanceof CompilationException,
          "Error code -1 should produce CompilationException, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("Error code -16 should map to WasmSecurityException")
    void shouldMapMinusSixteenToSecurity() {
      final WasmException ex = JniExceptionMapper.mapNativeError(-16, "access denied");
      assertTrue(
          ex instanceof WasmSecurityException,
          "Error code -16 must map to WasmSecurityException. Got: " + ex.getClass().getName());
      assertTrue(
          ex.getMessage().contains("access denied"), "Should include the error message detail");
    }

    @Test
    @DisplayName("Error code -17 should map to WasmRuntimeException (Component)")
    void shouldMapMinusSeventeenToComponent() {
      final WasmException ex = JniExceptionMapper.mapNativeError(-17, "component failure");
      assertTrue(
          ex instanceof WasmRuntimeException,
          "Error code -17 must map to WasmRuntimeException. Got: " + ex.getClass().getName());
      assertTrue(
          ex.getMessage().contains("Component model error"),
          "Error code -17 must contain ComponentError. Got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Error code -18 should map to WasmRuntimeException (Interface)")
    void shouldMapMinusEighteenToInterface() {
      final WasmException ex = JniExceptionMapper.mapNativeError(-18, "binding error");
      assertTrue(
          ex instanceof WasmRuntimeException,
          "Error code -18 must map to WasmRuntimeException. Got: " + ex.getClass().getName());
      assertTrue(
          ex.getMessage().contains("Interface definition or binding error"),
          "Error code -18 must contain InterfaceError. Got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Error code -21 should map to WasmRuntimeException (Internal)")
    void shouldMapMinusTwentyOneToInternal() {
      final WasmException ex = JniExceptionMapper.mapNativeError(-21, "internal failure");
      assertTrue(
          ex instanceof WasmRuntimeException,
          "Error code -21 must map to WasmRuntimeException. Got: " + ex.getClass().getName());
      assertTrue(
          ex.getMessage().contains("Internal system error"),
          "Error code -21 must contain InternalError. Got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should include error message in all mapped exceptions")
    void shouldIncludeErrorMessageInAllMappedExceptions() {
      final String testMessage = "specific error detail";
      for (int code = -1; code >= -26; code--) {
        final WasmException ex = JniExceptionMapper.mapNativeError(code, testMessage);
        assertTrue(
            ex.getMessage().contains(testMessage),
            "Error code " + code + " should include the message. Got: " + ex.getMessage());
      }
    }

    @Test
    @DisplayName("Should handle unknown error code")
    void shouldHandleUnknownErrorCode() {
      final WasmException ex = JniExceptionMapper.mapNativeError(-999, "unknown");
      assertNotNull(ex, "Exception should not be null");
      assertTrue(ex.getMessage().contains("Unknown native error"), "Should indicate unknown error");
      assertTrue(ex.getMessage().contains("-999"), "Should include error code");
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
      final WasmException ex = JniExceptionMapper.mapNativeError(-1, null);
      assertNotNull(ex, "Exception should not be null");
      assertTrue(
          ex instanceof CompilationException,
          "Should still produce CompilationException with null message");
    }

    @Test
    @DisplayName("Should map error code without message")
    void shouldMapErrorCodeWithoutMessage() {
      final WasmException ex = JniExceptionMapper.mapNativeError(-3, null);
      assertNotNull(ex, "Exception should not be null");
      assertTrue(
          ex instanceof WasmRuntimeException,
          "Should produce WasmRuntimeException for runtime error");
    }
  }

  @Nested
  @DisplayName("wrapNativeException Tests")
  class WrapNativeExceptionTests {

    @Test
    @DisplayName("Should wrap exception with operation name")
    void shouldWrapExceptionWithOperationName() {
      final RuntimeException cause = new RuntimeException("root cause");
      final JniException ex = JniExceptionMapper.wrapNativeException("create_engine", cause);

      assertNotNull(ex, "Exception should not be null");
      assertTrue(ex.getMessage().contains("create_engine"), "Should include operation name");
      assertEquals(cause, ex.getCause(), "Should preserve cause");
    }

    @Test
    @DisplayName("Should wrap exception with null operation name")
    void shouldWrapExceptionWithNullOperationName() {
      final RuntimeException cause = new RuntimeException("error");
      final JniException ex = JniExceptionMapper.wrapNativeException(null, cause);

      assertTrue(
          ex.getMessage().contains("Native operation failed"), "Should have generic message");
    }
  }

  @Nested
  @DisplayName("validateNativeHandle Tests")
  class ValidateNativeHandleTests {

    @Test
    @DisplayName("Should throw on zero handle")
    void shouldThrowOnZeroHandle() {
      assertThrows(
          JniResourceException.class,
          () -> JniExceptionMapper.validateNativeHandle(0, "Engine"),
          "Should throw on zero handle");
    }

    @Test
    @DisplayName("Should not throw on valid handle")
    void shouldNotThrowOnValidHandle() {
      JniExceptionMapper.validateNativeHandle(12345L, "Engine");
      // No exception means success
    }

    @Test
    @DisplayName("Should include resource type in error message")
    void shouldIncludeResourceTypeInErrorMessage() {
      try {
        JniExceptionMapper.validateNativeHandle(0, "Module");
      } catch (JniResourceException ex) {
        assertTrue(ex.getMessage().contains("Module"), "Should include resource type");
      }
    }
  }

  @Nested
  @DisplayName("mapException Tests")
  class MapExceptionTests {

    @Test
    @DisplayName("Should return JniException as-is")
    void shouldReturnJniExceptionAsIs() {
      final JniException original = new JniException("original");
      final JniException result = JniExceptionMapper.mapException(original);
      assertEquals(original, result, "Should return same instance");
    }

    @Test
    @DisplayName("Should map IllegalArgumentException")
    void shouldMapIllegalArgumentException() {
      final IllegalArgumentException cause = new IllegalArgumentException("bad arg");
      final JniException result = JniExceptionMapper.mapException(cause);
      assertTrue(
          result.getMessage().contains("Invalid parameter"), "Should indicate invalid parameter");
    }

    @Test
    @DisplayName("Should map IllegalStateException")
    void shouldMapIllegalStateException() {
      final IllegalStateException cause = new IllegalStateException("bad state");
      final JniException result = JniExceptionMapper.mapException(cause);
      assertTrue(
          result.getMessage().contains("Resource in invalid state"),
          "Should indicate invalid state");
    }

    @Test
    @DisplayName("Should map IndexOutOfBoundsException")
    void shouldMapIndexOutOfBoundsException() {
      final IndexOutOfBoundsException cause = new IndexOutOfBoundsException("10");
      final JniException result = JniExceptionMapper.mapException(cause);
      assertTrue(
          result.getMessage().contains("Index out of bounds"), "Should indicate index error");
    }

    @Test
    @DisplayName("Should map NullPointerException")
    void shouldMapNullPointerException() {
      final NullPointerException cause = new NullPointerException("param was null");
      final JniException result = JniExceptionMapper.mapException(cause);
      assertTrue(
          result.getMessage().contains("Null pointer error"), "Should indicate null pointer");
    }

    @Test
    @DisplayName("Should map null exception")
    void shouldMapNullException() {
      final JniException result = JniExceptionMapper.mapException(null);
      assertTrue(result.getMessage().contains("Unknown error"), "Should indicate unknown error");
    }
  }
}

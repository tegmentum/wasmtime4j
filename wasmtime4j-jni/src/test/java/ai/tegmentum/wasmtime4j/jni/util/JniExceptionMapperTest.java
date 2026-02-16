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
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniExceptionMapper should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniExceptionMapper.class.getModifiers()),
          "JniExceptionMapper should be final");
    }

    @Test
    @DisplayName("JniExceptionMapper should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final java.lang.reflect.Constructor<?> constructor =
          JniExceptionMapper.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");
    }
  }

  @Nested
  @DisplayName("mapNativeError Tests - All Error Codes")
  class MapNativeErrorTests {

    @ParameterizedTest(name = "Error code {0} should produce message containing \"{1}\"")
    @CsvSource({
      "0, No error occurred",
      "-1, Compilation failed",
      "-2, Validation failed",
      "-3, Runtime error",
      "-4, Engine configuration error",
      "-5, Store error",
      "-6, Instance error",
      "-7, Memory access error",
      "-8, Function invocation failed",
      "-9, Import/Export error",
      "-10, Type error",
      "-11, Resource error",
      "-12, I/O error",
      "-13, Invalid parameter",
      "-14, Concurrency error",
      "-15, WASI error",
      "-16, Security error",
      "-17, Component error",
      "-18, Interface error",
      "-19, Network error",
      "-20, Process error",
      "-21, Internal error",
      "-22, Security violation",
      "-23, Invalid data",
      "-24, I/O operation error",
      "-25, Unsupported operation",
      "-26, Would block"
    })
    @DisplayName("Should map all 27 error codes correctly")
    void shouldMapAllErrorCodes(final int errorCode, final String expectedMessagePart) {
      final JniException ex = JniExceptionMapper.mapNativeError(errorCode, "test detail");
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
    @DisplayName("Error code -16 should map to Security, not Component (bug fix verification)")
    void shouldMapMinusSixteenToSecurity() {
      final JniException ex = JniExceptionMapper.mapNativeError(-16, "access denied");
      assertTrue(
          ex.getMessage().contains("Security error"),
          "Error code -16 must map to 'Security error' (Rust SecurityError), "
              + "not 'Component error'. Got: "
              + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("access denied"), "Should include the error message detail");
    }

    @Test
    @DisplayName("Error code -17 should map to Component, not Interface (bug fix verification)")
    void shouldMapMinusSeventeenToComponent() {
      final JniException ex = JniExceptionMapper.mapNativeError(-17, "component failure");
      assertTrue(
          ex.getMessage().contains("Component error"),
          "Error code -17 must map to 'Component error' (Rust ComponentError), "
              + "not 'Interface error'. Got: "
              + ex.getMessage());
    }

    @Test
    @DisplayName("Error code -18 should map to Interface, not Internal (bug fix verification)")
    void shouldMapMinusEighteenToInterface() {
      final JniException ex = JniExceptionMapper.mapNativeError(-18, "binding error");
      assertTrue(
          ex.getMessage().contains("Interface error"),
          "Error code -18 must map to 'Interface error' (Rust InterfaceError), "
              + "not 'Internal error'. Got: "
              + ex.getMessage());
    }

    @Test
    @DisplayName("Error code -21 should map to Internal (previously unmapped)")
    void shouldMapMinusTwentyOneToInternal() {
      final JniException ex = JniExceptionMapper.mapNativeError(-21, "internal failure");
      assertTrue(
          ex.getMessage().contains("Internal error"),
          "Error code -21 must map to 'Internal error' (Rust InternalError). Got: "
              + ex.getMessage());
    }

    @Test
    @DisplayName("All mapped error codes should include the native error code")
    void allMappedErrorCodesShouldPreserveCode() {
      for (int code = -1; code >= -26; code--) {
        final JniException ex = JniExceptionMapper.mapNativeError(code, "test");
        assertNotNull(ex, "Exception should not be null for code " + code);
        assertTrue(
            ex.hasNativeErrorCode(),
            "Exception for code " + code + " should have native error code");
        assertEquals(
            code,
            ex.getNativeErrorCode(),
            "Exception for code " + code + " should preserve the native error code");
      }
    }

    @Test
    @DisplayName("Should include error message in all mapped exceptions")
    void shouldIncludeErrorMessageInAllMappedExceptions() {
      final String testMessage = "specific error detail";
      for (int code = -1; code >= -26; code--) {
        final JniException ex = JniExceptionMapper.mapNativeError(code, testMessage);
        assertTrue(
            ex.getMessage().contains(testMessage),
            "Error code " + code + " should include the message. Got: " + ex.getMessage());
      }
    }

    @Test
    @DisplayName("Should handle unknown error code")
    void shouldHandleUnknownErrorCode() {
      final JniException ex = JniExceptionMapper.mapNativeError(-999, "unknown");
      assertNotNull(ex, "Exception should not be null");
      assertTrue(ex.getMessage().contains("Unknown native error"), "Should indicate unknown error");
      assertTrue(ex.getMessage().contains("-999"), "Should include error code");
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
      final JniException ex = JniExceptionMapper.mapNativeError(-1, null);
      assertNotNull(ex, "Exception should not be null");
      assertTrue(ex.getMessage().contains("Unknown native error"), "Should have default message");
    }

    @Test
    @DisplayName("Should map error code without message")
    void shouldMapErrorCodeWithoutMessage() {
      final JniException ex = JniExceptionMapper.mapNativeError(-3);
      assertNotNull(ex, "Exception should not be null");
      assertTrue(ex.getMessage().contains("Runtime error"), "Should indicate runtime error");
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

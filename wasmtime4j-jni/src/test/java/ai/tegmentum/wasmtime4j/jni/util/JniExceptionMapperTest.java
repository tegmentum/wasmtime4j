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

/**
 * Comprehensive tests for {@link JniExceptionMapper}.
 */
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
  @DisplayName("mapNativeError Tests")
  class MapNativeErrorTests {

    @Test
    @DisplayName("Should map NATIVE_ERROR_NONE")
    void shouldMapErrorNone() {
      final JniException ex = JniExceptionMapper.mapNativeError(0, "test message");
      assertNotNull(ex, "Exception should not be null");
      assertTrue(ex.getMessage().contains("No error occurred"), "Should indicate no error");
    }

    @Test
    @DisplayName("Should map NATIVE_ERROR_COMPILATION")
    void shouldMapCompilationError() {
      final JniException ex = JniExceptionMapper.mapNativeError(-1, "bad wasm");
      assertNotNull(ex, "Exception should not be null");
      assertTrue(ex.getMessage().contains("Compilation failed"), "Should indicate compilation");
      assertTrue(ex.getMessage().contains("bad wasm"), "Should include message");
    }

    @Test
    @DisplayName("Should map NATIVE_ERROR_VALIDATION")
    void shouldMapValidationError() {
      final JniException ex = JniExceptionMapper.mapNativeError(-2, "invalid module");
      assertTrue(ex.getMessage().contains("Validation failed"), "Should indicate validation");
    }

    @Test
    @DisplayName("Should map NATIVE_ERROR_RUNTIME")
    void shouldMapRuntimeError() {
      final JniException ex = JniExceptionMapper.mapNativeError(-3, "trap");
      assertTrue(ex.getMessage().contains("Runtime error"), "Should indicate runtime error");
    }

    @Test
    @DisplayName("Should map NATIVE_ERROR_MEMORY to JniResourceException")
    void shouldMapMemoryError() {
      final JniException ex = JniExceptionMapper.mapNativeError(-7, "out of bounds");
      assertTrue(ex instanceof JniResourceException,
          "Should be JniResourceException for memory errors");
      assertTrue(ex.getMessage().contains("Memory access error"), "Should indicate memory error");
    }

    @Test
    @DisplayName("Should map NATIVE_ERROR_RESOURCE to JniResourceException")
    void shouldMapResourceError() {
      final JniException ex = JniExceptionMapper.mapNativeError(-11, "resource exhausted");
      assertTrue(ex instanceof JniResourceException,
          "Should be JniResourceException for resource errors");
    }

    @Test
    @DisplayName("Should handle unknown error code")
    void shouldHandleUnknownErrorCode() {
      final JniException ex = JniExceptionMapper.mapNativeError(-999, "unknown");
      assertNotNull(ex, "Exception should not be null");
      assertTrue(ex.getMessage().contains("Unknown native error"),
          "Should indicate unknown error");
      assertTrue(ex.getMessage().contains("-999"), "Should include error code");
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
      final JniException ex = JniExceptionMapper.mapNativeError(-1, null);
      assertNotNull(ex, "Exception should not be null");
      assertTrue(ex.getMessage().contains("Unknown native error"),
          "Should have default message");
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

      assertTrue(ex.getMessage().contains("Native operation failed"),
          "Should have generic message");
    }
  }

  @Nested
  @DisplayName("validateNativeHandle Tests")
  class ValidateNativeHandleTests {

    @Test
    @DisplayName("Should throw on zero handle")
    void shouldThrowOnZeroHandle() {
      assertThrows(JniResourceException.class,
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
        assertTrue(ex.getMessage().contains("Module"),
            "Should include resource type");
      }
    }
  }

  @Nested
  @DisplayName("validateNativeResult Tests")
  class ValidateNativeResultTests {

    @Test
    @DisplayName("Should throw on false result")
    void shouldThrowOnFalseResult() {
      assertThrows(JniException.class,
          () -> JniExceptionMapper.validateNativeResult(false, "invoke function"),
          "Should throw on false result");
    }

    @Test
    @DisplayName("Should not throw on true result")
    void shouldNotThrowOnTrueResult() {
      JniExceptionMapper.validateNativeResult(true, "invoke function");
      // No exception means success
    }
  }

  @Nested
  @DisplayName("getSafeErrorMessage Tests")
  class GetSafeErrorMessageTests {

    @Test
    @DisplayName("Should return native message when present")
    void shouldReturnNativeMessageWhenPresent() {
      final String result = JniExceptionMapper.getSafeErrorMessage("native error", "default");
      assertEquals("native error", result, "Should return native message");
    }

    @Test
    @DisplayName("Should return default message when native is null")
    void shouldReturnDefaultMessageWhenNativeIsNull() {
      final String result = JniExceptionMapper.getSafeErrorMessage(null, "default error");
      assertEquals("default error", result, "Should return default message");
    }

    @Test
    @DisplayName("Should return default message when native is empty")
    void shouldReturnDefaultMessageWhenNativeIsEmpty() {
      final String result = JniExceptionMapper.getSafeErrorMessage("", "default error");
      assertEquals("default error", result, "Should return default message");
    }

    @Test
    @DisplayName("Should return default message when native is whitespace")
    void shouldReturnDefaultMessageWhenNativeIsWhitespace() {
      final String result = JniExceptionMapper.getSafeErrorMessage("   ", "default error");
      assertEquals("default error", result, "Should return default message");
    }

    @Test
    @DisplayName("Should return Unknown error when both are null")
    void shouldReturnUnknownErrorWhenBothAreNull() {
      final String result = JniExceptionMapper.getSafeErrorMessage(null, null);
      assertEquals("Unknown error", result, "Should return Unknown error");
    }
  }

  @Nested
  @DisplayName("createCleanupException Tests")
  class CreateCleanupExceptionTests {

    @Test
    @DisplayName("Should create cleanup exception with resource type")
    void shouldCreateCleanupExceptionWithResourceType() {
      final JniResourceException ex = JniExceptionMapper.createCleanupException("Store", null);
      assertTrue(ex.getMessage().contains("cleanup"),
          "Should indicate cleanup");
      assertTrue(ex.getMessage().contains("Store"),
          "Should include resource type");
    }

    @Test
    @DisplayName("Should create cleanup exception with cause")
    void shouldCreateCleanupExceptionWithCause() {
      final RuntimeException cause = new RuntimeException("cleanup error");
      final JniResourceException ex = JniExceptionMapper.createCleanupException("Store", cause);
      assertEquals(cause, ex.getCause(), "Should preserve cause");
    }
  }

  @Nested
  @DisplayName("createInvalidStateException Tests")
  class CreateInvalidStateExceptionTests {

    @Test
    @DisplayName("Should create invalid state exception")
    void shouldCreateInvalidStateException() {
      final JniException ex = JniExceptionMapper.createInvalidStateException("Engine", "closed");
      assertTrue(ex.getMessage().contains("Engine"), "Should include resource type");
      assertTrue(ex.getMessage().contains("closed"), "Should include state");
    }

    @Test
    @DisplayName("Should handle null resource type")
    void shouldHandleNullResourceType() {
      final JniException ex = JniExceptionMapper.createInvalidStateException(null, "disposed");
      assertTrue(ex.getMessage().contains("Resource"), "Should use default Resource");
    }

    @Test
    @DisplayName("Should handle null state")
    void shouldHandleNullState() {
      final JniException ex = JniExceptionMapper.createInvalidStateException("Module", null);
      assertTrue(ex.getMessage().contains("invalid state"),
          "Should use default state");
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
      assertTrue(result.getMessage().contains("Invalid parameter"),
          "Should indicate invalid parameter");
    }

    @Test
    @DisplayName("Should map IllegalStateException to JniResourceException")
    void shouldMapIllegalStateException() {
      final IllegalStateException cause = new IllegalStateException("bad state");
      final JniException result = JniExceptionMapper.mapException(cause);
      assertTrue(result instanceof JniResourceException,
          "Should be JniResourceException");
    }

    @Test
    @DisplayName("Should map IndexOutOfBoundsException")
    void shouldMapIndexOutOfBoundsException() {
      final IndexOutOfBoundsException cause = new IndexOutOfBoundsException("10");
      final JniException result = JniExceptionMapper.mapException(cause);
      assertTrue(result.getMessage().contains("Index out of bounds"),
          "Should indicate index error");
    }

    @Test
    @DisplayName("Should map NullPointerException")
    void shouldMapNullPointerException() {
      final NullPointerException cause = new NullPointerException("param was null");
      final JniException result = JniExceptionMapper.mapException(cause);
      assertTrue(result.getMessage().contains("Null pointer error"),
          "Should indicate null pointer");
    }

    @Test
    @DisplayName("Should map null exception")
    void shouldMapNullException() {
      final JniException result = JniExceptionMapper.mapException(null);
      assertTrue(result.getMessage().contains("Unknown error"),
          "Should indicate unknown error");
    }
  }

  @Nested
  @DisplayName("getErrorCodeDescription Tests")
  class GetErrorCodeDescriptionTests {

    @Test
    @DisplayName("Should describe NATIVE_ERROR_NONE")
    void shouldDescribeErrorNone() {
      assertEquals("No error", JniExceptionMapper.getErrorCodeDescription(0));
    }

    @Test
    @DisplayName("Should describe NATIVE_ERROR_COMPILATION")
    void shouldDescribeCompilationError() {
      assertEquals("Compilation error", JniExceptionMapper.getErrorCodeDescription(-1));
    }

    @Test
    @DisplayName("Should describe unknown error code")
    void shouldDescribeUnknownErrorCode() {
      final String desc = JniExceptionMapper.getErrorCodeDescription(-999);
      assertTrue(desc.contains("Unknown error"), "Should indicate unknown");
      assertTrue(desc.contains("-999"), "Should include error code");
    }

    @Test
    @DisplayName("Should describe all known error codes")
    void shouldDescribeAllKnownErrorCodes() {
      // Test all known error codes from -1 to -18
      for (int i = -1; i >= -18; i--) {
        final String desc = JniExceptionMapper.getErrorCodeDescription(i);
        assertNotNull(desc, "Description should not be null for code " + i);
        assertTrue(!desc.contains("Unknown"), "Code " + i + " should be known");
      }
    }
  }
}

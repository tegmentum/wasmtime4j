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
      assertTrue(
          ex instanceof JniResourceException, "Should be JniResourceException for memory errors");
      assertTrue(ex.getMessage().contains("Memory access error"), "Should indicate memory error");
    }

    @Test
    @DisplayName("Should map NATIVE_ERROR_RESOURCE to JniResourceException")
    void shouldMapResourceError() {
      final JniException ex = JniExceptionMapper.mapNativeError(-11, "resource exhausted");
      assertTrue(
          ex instanceof JniResourceException, "Should be JniResourceException for resource errors");
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
    @DisplayName("Should map IllegalStateException to JniResourceException")
    void shouldMapIllegalStateException() {
      final IllegalStateException cause = new IllegalStateException("bad state");
      final JniException result = JniExceptionMapper.mapException(cause);
      assertTrue(result instanceof JniResourceException, "Should be JniResourceException");
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

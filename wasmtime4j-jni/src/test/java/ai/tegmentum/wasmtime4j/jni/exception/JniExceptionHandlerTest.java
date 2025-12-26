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

package ai.tegmentum.wasmtime4j.jni.exception;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmtimeException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JniExceptionHandler} class.
 *
 * <p>This test class verifies JniExceptionHandler static methods and utility class behavior.
 */
@DisplayName("JniExceptionHandler Tests")
class JniExceptionHandlerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniExceptionHandler should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<JniExceptionHandler> constructor =
          JniExceptionHandler.class.getDeclaredConstructor();
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("Private constructor should be invocable via reflection")
    void privateConstructorShouldBeInvocableViaReflection() throws Exception {
      final Constructor<JniExceptionHandler> constructor =
          JniExceptionHandler.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      assertNotNull(constructor.newInstance(), "Should be able to create instance via reflection");
    }
  }

  @Nested
  @DisplayName("handleNativeException(String) Tests")
  class HandleNativeExceptionStringTests {

    @Test
    @DisplayName("Should handle valid native exception message")
    void shouldHandleValidNativeExceptionMessage() {
      final WasmtimeException exception =
          JniExceptionHandler.handleNativeException("Memory allocation failed");

      assertNotNull(exception, "Exception should not be null");
      assertTrue(
          exception.getMessage().contains("Memory allocation failed"),
          "Message should contain original error");
    }

    @Test
    @DisplayName("Should handle null native exception")
    void shouldHandleNullNativeException() {
      final WasmtimeException exception = JniExceptionHandler.handleNativeException(null);

      assertNotNull(exception, "Exception should not be null");
      assertTrue(
          exception.getMessage().contains("Unknown"), "Message should indicate unknown exception");
    }

    @Test
    @DisplayName("Should handle empty native exception")
    void shouldHandleEmptyNativeException() {
      final WasmtimeException exception = JniExceptionHandler.handleNativeException("");

      assertNotNull(exception, "Exception should not be null");
      assertTrue(
          exception.getMessage().contains("Unknown"), "Message should indicate unknown exception");
    }

    @Test
    @DisplayName("Should prefix message with Native exception")
    void shouldPrefixMessageWithNativeException() {
      final WasmtimeException exception = JniExceptionHandler.handleNativeException("Some error");

      assertTrue(
          exception.getMessage().startsWith("Native exception:"),
          "Message should start with 'Native exception:'");
    }
  }

  @Nested
  @DisplayName("handleNativeException(int, String) Tests")
  class HandleNativeExceptionErrorCodeTests {

    @Test
    @DisplayName("Should handle error code and message")
    void shouldHandleErrorCodeAndMessage() {
      final WasmtimeException exception =
          JniExceptionHandler.handleNativeException(42, "Resource not found");

      assertNotNull(exception, "Exception should not be null");
      assertTrue(exception.getMessage().contains("42"), "Message should contain error code");
      assertTrue(
          exception.getMessage().contains("Resource not found"), "Message should contain message");
    }

    @Test
    @DisplayName("Should format error code correctly")
    void shouldFormatErrorCodeCorrectly() {
      final WasmtimeException exception = JniExceptionHandler.handleNativeException(-1, "Error");

      assertTrue(
          exception.getMessage().contains("[-1]"), "Message should contain formatted error code");
    }

    @Test
    @DisplayName("Should handle zero error code")
    void shouldHandleZeroErrorCode() {
      final WasmtimeException exception = JniExceptionHandler.handleNativeException(0, "Success");

      assertTrue(exception.getMessage().contains("[0]"), "Message should contain zero error code");
    }

    @Test
    @DisplayName("Should handle null message with error code")
    void shouldHandleNullMessageWithErrorCode() {
      final WasmtimeException exception = JniExceptionHandler.handleNativeException(100, null);

      assertNotNull(exception, "Exception should not be null");
      assertTrue(exception.getMessage().contains("[100]"), "Message should contain error code");
    }
  }

  @Nested
  @DisplayName("wrapException Tests")
  class WrapExceptionTests {

    @Test
    @DisplayName("Should return same exception if already WasmtimeException")
    void shouldReturnSameExceptionIfAlreadyWasmtimeException() {
      final WasmtimeException original = new WasmtimeException("Original error");
      final WasmtimeException result = JniExceptionHandler.wrapException(original, "Context");

      assertSame(original, result, "Should return same exception instance");
    }

    @Test
    @DisplayName("Should wrap non-WasmtimeException with context")
    void shouldWrapNonWasmtimeExceptionWithContext() {
      final RuntimeException original = new RuntimeException("Runtime error");
      final WasmtimeException result =
          JniExceptionHandler.wrapException(original, "During initialization");

      assertNotNull(result, "Result should not be null");
      assertTrue(
          result.getMessage().contains("During initialization"), "Message should contain context");
      assertTrue(
          result.getMessage().contains("Runtime error"), "Message should contain original message");
    }

    @Test
    @DisplayName("Should preserve cause in wrapped exception")
    void shouldPreserveCauseInWrappedException() {
      final IllegalArgumentException original = new IllegalArgumentException("Invalid arg");
      final WasmtimeException result =
          JniExceptionHandler.wrapException(original, "Validation failed");

      assertSame(original, result.getCause(), "Cause should be preserved");
    }

    @Test
    @DisplayName("Should handle exception with null message")
    void shouldHandleExceptionWithNullMessage() {
      final NullPointerException original = new NullPointerException();
      final WasmtimeException result = JniExceptionHandler.wrapException(original, "Context");

      assertNotNull(result, "Result should not be null");
      assertTrue(result.getMessage().contains("Context"), "Message should contain context");
    }

    @Test
    @DisplayName("Should handle different exception types")
    void shouldHandleDifferentExceptionTypes() {
      final Exception[] exceptions = {
        new RuntimeException("Runtime"),
        new IllegalArgumentException("Illegal arg"),
        new IllegalStateException("Illegal state"),
        new NullPointerException("NPE")
      };

      for (Exception e : exceptions) {
        final WasmtimeException result = JniExceptionHandler.wrapException(e, "Wrapper");
        assertNotNull(result, "Result should not be null for " + e.getClass().getSimpleName());
        assertSame(
            e, result.getCause(), "Cause should be preserved for " + e.getClass().getSimpleName());
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Multiple calls should create independent exceptions")
    void multipleCallsShouldCreateIndependentExceptions() {
      final WasmtimeException e1 = JniExceptionHandler.handleNativeException("Error 1");
      final WasmtimeException e2 = JniExceptionHandler.handleNativeException("Error 2");

      assertTrue(e1 != e2, "Exceptions should be different instances");
      assertTrue(!e1.getMessage().equals(e2.getMessage()), "Messages should be different");
    }

    @Test
    @DisplayName("Full exception chain should be preservable")
    void fullExceptionChainShouldBePreservable() {
      final Exception root = new IllegalStateException("Root cause");
      final Exception middle = new RuntimeException("Middle cause", root);
      final WasmtimeException result = JniExceptionHandler.wrapException(middle, "Final context");

      assertSame(middle, result.getCause(), "Direct cause should be middle");
      assertSame(root, result.getCause().getCause(), "Root cause should be preserved");
    }
  }
}

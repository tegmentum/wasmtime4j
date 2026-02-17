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

package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ValidationException} class.
 *
 * <p>ValidationException is thrown when WebAssembly validation fails.
 */
@DisplayName("ValidationException Tests")
class ValidationExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with message")
    void shouldHaveConstructorWithMessage() throws NoSuchMethodException {
      final Constructor<ValidationException> constructor =
          ValidationException.class.getConstructor(String.class);
      assertNotNull(constructor, "Constructor with message should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message and cause")
    void shouldHaveConstructorWithMessageAndCause() throws NoSuchMethodException {
      final Constructor<ValidationException> constructor =
          ValidationException.class.getConstructor(String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with message and cause should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final String message = "Type mismatch in function";
      final ValidationException exception = new ValidationException(message);

      assertEquals(message, exception.getMessage(), "Message should be set correctly");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Failed to validate module";
      final RuntimeException cause = new RuntimeException("Type error");
      final ValidationException exception = new ValidationException(message, cause);

      assertEquals(message, exception.getMessage(), "Message should be set correctly");
      assertSame(cause, exception.getCause(), "Cause should be set correctly");
    }
  }

  @Nested
  @DisplayName("Exception Behavior Tests")
  class ExceptionBehaviorTests {

    @Test
    @DisplayName("should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      boolean caught = false;
      try {
        throw new ValidationException("Validation failed");
      } catch (final WasmException e) {
        caught = true;
        assertTrue(e instanceof ValidationException, "Should be instance of ValidationException");
      }
      assertTrue(caught, "Exception should be caught as WasmException");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new ValidationException("Validation failed");
      } catch (final Exception e) {
        caught = true;
        assertTrue(e instanceof ValidationException, "Should be instance of ValidationException");
      }
      assertTrue(caught, "Exception should be caught as Exception");
    }

    @Test
    @DisplayName("should preserve stack trace")
    void shouldPreserveStackTrace() {
      final ValidationException exception = new ValidationException("Test");
      final StackTraceElement[] stackTrace = exception.getStackTrace();

      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
    }
  }

  @Nested
  @DisplayName("Validation Error Scenario Tests")
  class ValidationErrorScenarioTests {

    @Test
    @DisplayName("should handle type mismatch error")
    void shouldHandleTypeMismatchError() {
      final ValidationException exception =
          new ValidationException("Type mismatch: expected i32, got i64");

      assertTrue(
          exception.getMessage().contains("Type mismatch"),
          "Message should describe type mismatch");
    }

    @Test
    @DisplayName("should handle invalid function signature error")
    void shouldHandleInvalidFunctionSignatureError() {
      final ValidationException exception =
          new ValidationException("Invalid function signature at index 5");

      assertTrue(
          exception.getMessage().contains("function signature"),
          "Message should describe function signature error");
    }

    @Test
    @DisplayName("should handle stack underflow error")
    void shouldHandleStackUnderflowError() {
      final ValidationException exception =
          new ValidationException("Stack underflow: expected 2 values, got 1");

      assertTrue(
          exception.getMessage().contains("underflow"), "Message should describe stack underflow");
    }

    @Test
    @DisplayName("should handle invalid branch target error")
    void shouldHandleInvalidBranchTargetError() {
      final ValidationException exception =
          new ValidationException("Invalid branch target: label 3 out of range");

      assertTrue(
          exception.getMessage().contains("branch"), "Message should describe branch target error");
    }
  }
}

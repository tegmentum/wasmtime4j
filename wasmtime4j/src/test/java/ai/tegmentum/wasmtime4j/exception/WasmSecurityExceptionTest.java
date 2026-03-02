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
 * Tests for {@link WasmSecurityException} class.
 *
 * <p>WasmSecurityException is thrown when WebAssembly security violations occur.
 */
@DisplayName("WasmSecurityException Tests")
class WasmSecurityExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with message")
    void shouldHaveConstructorWithMessage() throws NoSuchMethodException {
      final Constructor<WasmSecurityException> constructor =
          WasmSecurityException.class.getConstructor(String.class);
      assertNotNull(constructor, "Constructor with message should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message and cause")
    void shouldHaveConstructorWithMessageAndCause() throws NoSuchMethodException {
      final Constructor<WasmSecurityException> constructor =
          WasmSecurityException.class.getConstructor(String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with message and cause should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final String message = "Unauthorized host function access";
      final WasmSecurityException exception = new WasmSecurityException(message);

      assertEquals(message, exception.getMessage(), "Message should be set correctly");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Sandbox violation";
      final RuntimeException cause = new RuntimeException("Forbidden system call");
      final WasmSecurityException exception = new WasmSecurityException(message, cause);

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
        throw new WasmSecurityException("Security violation");
      } catch (final WasmException e) {
        caught = true;
        assertTrue(
            e instanceof WasmSecurityException, "Should be instance of WasmSecurityException");
      }
      assertTrue(caught, "Exception should be caught as WasmException");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new WasmSecurityException("Security violation");
      } catch (final Exception e) {
        caught = true;
        assertTrue(
            e instanceof WasmSecurityException, "Should be instance of WasmSecurityException");
      }
      assertTrue(caught, "Exception should be caught as Exception");
    }

    @Test
    @DisplayName("should preserve stack trace")
    void shouldPreserveStackTrace() {
      final WasmSecurityException exception = new WasmSecurityException("Test");
      final StackTraceElement[] stackTrace = exception.getStackTrace();

      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
    }
  }

  @Nested
  @DisplayName("Security Error Scenario Tests")
  class SecurityErrorScenarioTests {

    @Test
    @DisplayName("should handle unauthorized access error")
    void shouldHandleUnauthorizedAccessError() {
      final WasmSecurityException exception =
          new WasmSecurityException("Unauthorized access to host function: fs_read");

      assertTrue(
          exception.getMessage().contains("Unauthorized"),
          "Message should describe unauthorized access");
    }

    @Test
    @DisplayName("should handle sandbox escape attempt error")
    void shouldHandleSandboxEscapeAttemptError() {
      final WasmSecurityException exception =
          new WasmSecurityException("Sandbox escape attempt: memory access outside bounds");

      assertTrue(
          exception.getMessage().contains("Sandbox"), "Message should describe sandbox violation");
    }

    @Test
    @DisplayName("should handle forbidden system call error")
    void shouldHandleForbiddenSystemCallError() {
      final WasmSecurityException exception =
          new WasmSecurityException("Forbidden system call: network access not permitted");

      assertTrue(
          exception.getMessage().contains("Forbidden"),
          "Message should describe forbidden operation");
    }
  }
}

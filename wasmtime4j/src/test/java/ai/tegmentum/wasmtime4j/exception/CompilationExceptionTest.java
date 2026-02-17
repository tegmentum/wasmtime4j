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
 * Tests for {@link CompilationException} class.
 *
 * <p>CompilationException is thrown when WebAssembly compilation fails.
 */
@DisplayName("CompilationException Tests")
class CompilationExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with message")
    void shouldHaveConstructorWithMessage() throws NoSuchMethodException {
      final Constructor<CompilationException> constructor =
          CompilationException.class.getConstructor(String.class);
      assertNotNull(constructor, "Constructor with message should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message and cause")
    void shouldHaveConstructorWithMessageAndCause() throws NoSuchMethodException {
      final Constructor<CompilationException> constructor =
          CompilationException.class.getConstructor(String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with message and cause should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final String message = "Invalid magic number";
      final CompilationException exception = new CompilationException(message);

      assertEquals(message, exception.getMessage(), "Message should be set correctly");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Failed to compile module";
      final RuntimeException cause = new RuntimeException("Parser error");
      final CompilationException exception = new CompilationException(message, cause);

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
        throw new CompilationException("Compilation failed");
      } catch (final WasmException e) {
        caught = true;
        assertTrue(e instanceof CompilationException, "Should be instance of CompilationException");
      }
      assertTrue(caught, "Exception should be caught as WasmException");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new CompilationException("Compilation failed");
      } catch (final Exception e) {
        caught = true;
        assertTrue(e instanceof CompilationException, "Should be instance of CompilationException");
      }
      assertTrue(caught, "Exception should be caught as Exception");
    }

    @Test
    @DisplayName("should preserve stack trace")
    void shouldPreserveStackTrace() {
      final CompilationException exception = new CompilationException("Test");
      final StackTraceElement[] stackTrace = exception.getStackTrace();

      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
    }
  }

  @Nested
  @DisplayName("Compilation Error Scenario Tests")
  class CompilationErrorScenarioTests {

    @Test
    @DisplayName("should handle invalid magic number error")
    void shouldHandleInvalidMagicNumberError() {
      final CompilationException exception =
          new CompilationException("Invalid magic number: expected 0x0061736d");

      assertTrue(
          exception.getMessage().contains("magic number"),
          "Message should describe magic number error");
    }

    @Test
    @DisplayName("should handle unsupported feature error")
    void shouldHandleUnsupportedFeatureError() {
      final CompilationException exception =
          new CompilationException("Unsupported feature: reference types");

      assertTrue(
          exception.getMessage().contains("Unsupported"),
          "Message should describe unsupported feature");
    }

    @Test
    @DisplayName("should handle malformed bytecode error")
    void shouldHandleMalformedBytecodeError() {
      final CompilationException exception =
          new CompilationException("Malformed bytecode at offset 0x100");

      assertTrue(
          exception.getMessage().contains("Malformed"),
          "Message should describe malformed bytecode");
    }
  }
}

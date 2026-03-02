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
 * Tests for {@link WasmRuntimeException} class.
 *
 * <p>WasmRuntimeException is thrown when WebAssembly runtime errors occur.
 */
@DisplayName("WasmRuntimeException Tests")
class WasmRuntimeExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with message")
    void shouldHaveConstructorWithMessage() throws NoSuchMethodException {
      final Constructor<WasmRuntimeException> constructor =
          WasmRuntimeException.class.getConstructor(String.class);
      assertNotNull(constructor, "Constructor with message should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message and cause")
    void shouldHaveConstructorWithMessageAndCause() throws NoSuchMethodException {
      final Constructor<WasmRuntimeException> constructor =
          WasmRuntimeException.class.getConstructor(String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with message and cause should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final String message = "Out of bounds memory access";
      final WasmRuntimeException exception = new WasmRuntimeException(message);

      assertEquals(message, exception.getMessage(), "Message should be set correctly");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Function call failed";
      final RuntimeException cause = new RuntimeException("Stack overflow");
      final WasmRuntimeException exception = new WasmRuntimeException(message, cause);

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
        throw new WasmRuntimeException("Runtime error");
      } catch (final WasmException e) {
        caught = true;
        assertTrue(e instanceof WasmRuntimeException, "Should be instance of WasmRuntimeException");
      }
      assertTrue(caught, "Exception should be caught as WasmException");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new WasmRuntimeException("Runtime error");
      } catch (final Exception e) {
        caught = true;
        assertTrue(e instanceof WasmRuntimeException, "Should be instance of WasmRuntimeException");
      }
      assertTrue(caught, "Exception should be caught as Exception");
    }

    @Test
    @DisplayName("should preserve stack trace")
    void shouldPreserveStackTrace() {
      final WasmRuntimeException exception = new WasmRuntimeException("Test");
      final StackTraceElement[] stackTrace = exception.getStackTrace();

      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
    }
  }

  @Nested
  @DisplayName("Runtime Error Scenario Tests")
  class RuntimeErrorScenarioTests {

    @Test
    @DisplayName("should handle out of bounds memory access error")
    void shouldHandleOutOfBoundsMemoryAccessError() {
      final WasmRuntimeException exception =
          new WasmRuntimeException("Out of bounds memory access at offset 0x1000");

      assertTrue(
          exception.getMessage().contains("Out of bounds"),
          "Message should describe out of bounds error");
    }

    @Test
    @DisplayName("should handle indirect call type mismatch error")
    void shouldHandleIndirectCallTypeMismatchError() {
      final WasmRuntimeException exception =
          new WasmRuntimeException("Indirect call type mismatch: expected (i32) -> i32");

      assertTrue(
          exception.getMessage().contains("type mismatch"),
          "Message should describe type mismatch error");
    }

    @Test
    @DisplayName("should handle unreachable instruction error")
    void shouldHandleUnreachableInstructionError() {
      final WasmRuntimeException exception =
          new WasmRuntimeException("Unreachable instruction executed at function index 5");

      assertTrue(
          exception.getMessage().contains("Unreachable"),
          "Message should describe unreachable instruction");
    }
  }
}

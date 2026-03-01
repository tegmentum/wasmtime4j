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
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmTypeException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmTypeException} class.
 *
 * <p>WasmTypeException is thrown when WebAssembly type errors occur.
 */
@DisplayName("WasmTypeException Tests")
class WasmTypeExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with message")
    void shouldHaveConstructorWithMessage() throws NoSuchMethodException {
      final Constructor<WasmTypeException> constructor =
          WasmTypeException.class.getConstructor(String.class);
      assertNotNull(constructor, "Constructor with message should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message and cause")
    void shouldHaveConstructorWithMessageAndCause() throws NoSuchMethodException {
      final Constructor<WasmTypeException> constructor =
          WasmTypeException.class.getConstructor(String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with message and cause should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final String message = "Type mismatch: expected i32, got i64";
      final WasmTypeException exception = new WasmTypeException(message);

      assertEquals(message, exception.getMessage(), "Message should be set correctly");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Failed to convert type";
      final RuntimeException cause = new RuntimeException("Conversion error");
      final WasmTypeException exception = new WasmTypeException(message, cause);

      assertEquals(message, exception.getMessage(), "Message should be set correctly");
      assertSame(cause, exception.getCause(), "Cause should be set correctly");
    }
  }

  @Nested
  @DisplayName("Exception Behavior Tests")
  class ExceptionBehaviorTests {

    @Test
    @DisplayName("should be unchecked exception")
    void shouldBeUncheckedException() {
      // RuntimeException subclasses are unchecked - no try/catch required at compile time
      assertTrue(
          RuntimeException.class.isAssignableFrom(WasmTypeException.class),
          "WasmTypeException should be an unchecked exception");
    }

    @Test
    @DisplayName("should be catchable as RuntimeException")
    void shouldBeCatchableAsRuntimeException() {
      boolean caught = false;
      try {
        throw new WasmTypeException("Type error");
      } catch (final RuntimeException e) {
        caught = true;
        assertTrue(e instanceof WasmTypeException, "Should be instance of WasmTypeException");
      }
      assertTrue(caught, "Exception should be caught as RuntimeException");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new WasmTypeException("Type error");
      } catch (final Exception e) {
        caught = true;
        assertTrue(e instanceof WasmTypeException, "Should be instance of WasmTypeException");
      }
      assertTrue(caught, "Exception should be caught as Exception");
    }

    @Test
    @DisplayName("should preserve stack trace")
    void shouldPreserveStackTrace() {
      final WasmTypeException exception = new WasmTypeException("Test");
      final StackTraceElement[] stackTrace = exception.getStackTrace();

      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
    }
  }

  @Nested
  @DisplayName("Type Error Scenario Tests")
  class TypeErrorScenarioTests {

    @Test
    @DisplayName("should handle value type mismatch")
    void shouldHandleValueTypeMismatch() {
      final WasmTypeException exception =
          new WasmTypeException("Type mismatch: expected i32, got f64");

      assertTrue(
          exception.getMessage().contains("Type mismatch"),
          "Message should describe type mismatch");
    }

    @Test
    @DisplayName("should handle function signature mismatch")
    void shouldHandleFunctionSignatureMismatch() {
      final WasmTypeException exception =
          new WasmTypeException(
              "Function signature mismatch: expected (i32, i32) -> i32, got (i32) -> i32");

      assertTrue(
          exception.getMessage().contains("signature"),
          "Message should describe signature mismatch");
    }

    @Test
    @DisplayName("should handle table type mismatch")
    void shouldHandleTableTypeMismatch() {
      final WasmTypeException exception =
          new WasmTypeException("Table type mismatch: expected funcref, got externref");

      assertTrue(
          exception.getMessage().contains("Table"), "Message should describe table type mismatch");
    }

    @Test
    @DisplayName("should handle memory type mismatch")
    void shouldHandleMemoryTypeMismatch() {
      final WasmTypeException exception =
          new WasmTypeException("Memory type mismatch: 32-bit memory expected, 64-bit provided");

      assertTrue(
          exception.getMessage().contains("Memory"),
          "Message should describe memory type mismatch");
    }

    @Test
    @DisplayName("should handle global mutability mismatch")
    void shouldHandleGlobalMutabilityMismatch() {
      final WasmTypeException exception =
          new WasmTypeException(
              "Global type mismatch: mutable global required, but const provided");

      assertTrue(
          exception.getMessage().contains("Global"),
          "Message should describe global type mismatch");
    }

    @Test
    @DisplayName("should handle invalid type conversion")
    void shouldHandleInvalidTypeConversion() {
      final WasmTypeException exception =
          new WasmTypeException("Cannot convert externref to funcref");

      assertTrue(
          exception.getMessage().contains("convert"), "Message should describe conversion error");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle null message")
    void shouldHandleNullMessage() {
      final WasmTypeException exception = new WasmTypeException(null);
      assertNull(exception.getMessage(), "Null message should be preserved");
    }

    @Test
    @DisplayName("should handle empty message")
    void shouldHandleEmptyMessage() {
      final WasmTypeException exception = new WasmTypeException("");
      assertEquals("", exception.getMessage(), "Empty message should be preserved");
    }

    @Test
    @DisplayName("should handle unicode message")
    void shouldHandleUnicodeMessage() {
      final String unicodeMessage = "Type mismatch: 型の不一致";
      final WasmTypeException exception = new WasmTypeException(unicodeMessage);
      assertEquals(unicodeMessage, exception.getMessage(), "Unicode message should be preserved");
    }

    @Test
    @DisplayName("should chain causes correctly")
    void shouldChainCausesCorrectly() {
      final IllegalArgumentException root = new IllegalArgumentException("Root");
      final RuntimeException middle = new RuntimeException("Middle", root);
      final WasmTypeException exception = new WasmTypeException("Top", middle);

      assertSame(middle, exception.getCause(), "Immediate cause should be middle");
      assertSame(root, exception.getCause().getCause(), "Root cause should be accessible");
    }
  }
}

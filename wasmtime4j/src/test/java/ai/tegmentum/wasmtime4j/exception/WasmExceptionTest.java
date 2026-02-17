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
 * Tests for {@link WasmException} class.
 *
 * <p>WasmException is the base class for all WebAssembly-related exceptions.
 */
@DisplayName("WasmException Tests")
class WasmExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with message")
    void shouldHaveConstructorWithMessage() throws NoSuchMethodException {
      final Constructor<WasmException> constructor =
          WasmException.class.getConstructor(String.class);
      assertNotNull(constructor, "Constructor with message should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message and cause")
    void shouldHaveConstructorWithMessageAndCause() throws NoSuchMethodException {
      final Constructor<WasmException> constructor =
          WasmException.class.getConstructor(String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with message and cause should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with cause only")
    void shouldHaveConstructorWithCauseOnly() throws NoSuchMethodException {
      final Constructor<WasmException> constructor =
          WasmException.class.getConstructor(Throwable.class);
      assertNotNull(constructor, "Constructor with cause should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final String message = "Test error message";
      final WasmException exception = new WasmException(message);

      assertEquals(message, exception.getMessage(), "Message should be set correctly");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Test error message";
      final RuntimeException cause = new RuntimeException("Root cause");
      final WasmException exception = new WasmException(message, cause);

      assertEquals(message, exception.getMessage(), "Message should be set correctly");
      assertSame(cause, exception.getCause(), "Cause should be set correctly");
    }

    @Test
    @DisplayName("should create exception with cause only")
    void shouldCreateExceptionWithCauseOnly() {
      final RuntimeException cause = new RuntimeException("Root cause");
      final WasmException exception = new WasmException(cause);

      assertSame(cause, exception.getCause(), "Cause should be set correctly");
      assertTrue(
          exception.getMessage().contains("Root cause"), "Message should contain cause message");
    }
  }

  @Nested
  @DisplayName("Exception Behavior Tests")
  class ExceptionBehaviorTests {

    @Test
    @DisplayName("should be throwable and catchable")
    void shouldBeThrowableAndCatchable() {
      boolean caught = false;
      try {
        throw new WasmException("Test exception");
      } catch (final WasmException e) {
        caught = true;
        assertEquals("Test exception", e.getMessage(), "Message should be preserved");
      }
      assertTrue(caught, "Exception should be caught");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new WasmException("Test exception");
      } catch (final Exception e) {
        caught = true;
        assertTrue(e instanceof WasmException, "Should be instance of WasmException");
      }
      assertTrue(caught, "Exception should be caught as Exception");
    }

    @Test
    @DisplayName("should preserve stack trace")
    void shouldPreserveStackTrace() {
      final WasmException exception = new WasmException("Test message");
      final StackTraceElement[] stackTrace = exception.getStackTrace();

      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
    }

    @Test
    @DisplayName("should chain causes correctly")
    void shouldChainCausesCorrectly() {
      final IllegalArgumentException root = new IllegalArgumentException("Root");
      final RuntimeException middle = new RuntimeException("Middle", root);
      final WasmException exception = new WasmException("Top", middle);

      assertSame(middle, exception.getCause(), "Immediate cause should be middle");
      assertSame(root, exception.getCause().getCause(), "Root cause should be accessible");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle null message")
    void shouldHandleNullMessage() {
      final WasmException exception = new WasmException((String) null);
      assertNull(exception.getMessage(), "Null message should be preserved");
    }

    @Test
    @DisplayName("should handle empty message")
    void shouldHandleEmptyMessage() {
      final WasmException exception = new WasmException("");
      assertEquals("", exception.getMessage(), "Empty message should be preserved");
    }

    @Test
    @DisplayName("should handle very long message")
    void shouldHandleVeryLongMessage() {
      final String longMessage = "a".repeat(10000);
      final WasmException exception = new WasmException(longMessage);
      assertEquals(longMessage, exception.getMessage(), "Long message should be preserved");
    }

    @Test
    @DisplayName("should handle unicode message")
    void shouldHandleUnicodeMessage() {
      final String unicodeMessage = "エラーメッセージ 中文错误";
      final WasmException exception = new WasmException(unicodeMessage);
      assertEquals(unicodeMessage, exception.getMessage(), "Unicode message should be preserved");
    }

    @Test
    @DisplayName("should handle null cause")
    void shouldHandleNullCause() {
      final WasmException exception = new WasmException("Message", null);
      assertNull(exception.getCause(), "Null cause should be preserved");
    }
  }
}

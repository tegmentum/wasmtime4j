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

package ai.tegmentum.wasmtime4j.panama.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaException} class.
 *
 * <p>This test class verifies the base exception for Panama FFI implementation errors.
 */
@DisplayName("PanamaException Tests")
class PanamaExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaException should extend Exception")
    void shouldExtendException() {
      assertTrue(Exception.class.isAssignableFrom(PanamaException.class),
          "PanamaException should extend Exception");
    }

    @Test
    @DisplayName("PanamaException should not be final")
    void shouldNotBeFinal() {
      assertFalse(java.lang.reflect.Modifier.isFinal(PanamaException.class.getModifiers()),
          "PanamaException should not be final (to allow subclasses)");
    }

    @Test
    @DisplayName("PanamaException should be a checked exception")
    void shouldBeCheckedException() {
      assertFalse(RuntimeException.class.isAssignableFrom(PanamaException.class),
          "PanamaException should not extend RuntimeException");
    }

    @Test
    @DisplayName("PanamaException should have serialVersionUID")
    void shouldHaveSerialVersionUID() throws NoSuchFieldException {
      final var field = PanamaException.class.getDeclaredField("serialVersionUID");
      assertNotNull(field, "Should have serialVersionUID field");
      assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()),
          "serialVersionUID should be static");
      assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()),
          "serialVersionUID should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests - Message Only")
  class MessageConstructorTests {

    @Test
    @DisplayName("Constructor should accept message")
    void constructorShouldAcceptMessage() {
      final PanamaException exception = new PanamaException("Test message");
      assertEquals("Test message", exception.getMessage(), "Message should match");
    }

    @Test
    @DisplayName("Constructor should accept null message")
    void constructorShouldAcceptNullMessage() {
      final PanamaException exception = new PanamaException((String) null);
      assertNull(exception.getMessage(), "Message should be null");
    }

    @Test
    @DisplayName("Constructor should accept empty message")
    void constructorShouldAcceptEmptyMessage() {
      final PanamaException exception = new PanamaException("");
      assertEquals("", exception.getMessage(), "Message should be empty");
    }

    @Test
    @DisplayName("Constructor should have null cause")
    void constructorShouldHaveNullCause() {
      final PanamaException exception = new PanamaException("Test message");
      assertNull(exception.getCause(), "Cause should be null");
    }
  }

  @Nested
  @DisplayName("Constructor Tests - Message and Cause")
  class MessageAndCauseConstructorTests {

    @Test
    @DisplayName("Constructor should accept message and cause")
    void constructorShouldAcceptMessageAndCause() {
      final RuntimeException cause = new RuntimeException("Root cause");
      final PanamaException exception = new PanamaException("Test message", cause);
      assertEquals("Test message", exception.getMessage(), "Message should match");
      assertSame(cause, exception.getCause(), "Cause should match");
    }

    @Test
    @DisplayName("Constructor should accept null cause with message")
    void constructorShouldAcceptNullCauseWithMessage() {
      final PanamaException exception = new PanamaException("Test message", null);
      assertEquals("Test message", exception.getMessage(), "Message should match");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor should accept null message with cause")
    void constructorShouldAcceptNullMessageWithCause() {
      final RuntimeException cause = new RuntimeException("Root cause");
      final PanamaException exception = new PanamaException(null, cause);
      assertNull(exception.getMessage(), "Message should be null");
      assertSame(cause, exception.getCause(), "Cause should match");
    }

    @Test
    @DisplayName("Constructor should accept both null message and cause")
    void constructorShouldAcceptBothNullMessageAndCause() {
      final PanamaException exception = new PanamaException(null, null);
      assertNull(exception.getMessage(), "Message should be null");
      assertNull(exception.getCause(), "Cause should be null");
    }
  }

  @Nested
  @DisplayName("Constructor Tests - Cause Only")
  class CauseConstructorTests {

    @Test
    @DisplayName("Constructor should accept cause")
    void constructorShouldAcceptCause() {
      final RuntimeException cause = new RuntimeException("Root cause");
      final PanamaException exception = new PanamaException(cause);
      assertSame(cause, exception.getCause(), "Cause should match");
    }

    @Test
    @DisplayName("Constructor with cause should derive message from cause")
    void constructorWithCauseShouldDeriveMessageFromCause() {
      final RuntimeException cause = new RuntimeException("Root cause");
      final PanamaException exception = new PanamaException(cause);
      assertNotNull(exception.getMessage(), "Message should not be null");
      assertTrue(exception.getMessage().contains("RuntimeException")
              || exception.getMessage().contains("Root cause"),
          "Message should contain cause info");
    }

    @Test
    @DisplayName("Constructor should accept null cause")
    void constructorShouldAcceptNullCause() {
      final PanamaException exception = new PanamaException((Throwable) null);
      assertNull(exception.getCause(), "Cause should be null");
    }
  }

  @Nested
  @DisplayName("Exception Chain Tests")
  class ExceptionChainTests {

    @Test
    @DisplayName("Exception should be throwable")
    void exceptionShouldBeThrowable() {
      try {
        throw new PanamaException("Test exception");
      } catch (PanamaException e) {
        assertEquals("Test exception", e.getMessage(), "Should catch with correct message");
      }
    }

    @Test
    @DisplayName("Exception chain should be preserved")
    void exceptionChainShouldBePreserved() {
      final IllegalStateException root = new IllegalStateException("Root");
      final RuntimeException middle = new RuntimeException("Middle", root);
      final PanamaException exception = new PanamaException("Top", middle);

      assertSame(middle, exception.getCause(), "Immediate cause should match");
      assertSame(root, exception.getCause().getCause(), "Root cause should match");
    }

    @Test
    @DisplayName("Exception should support initCause")
    void exceptionShouldSupportInitCause() {
      final PanamaException exception = new PanamaException("Test");
      final RuntimeException cause = new RuntimeException("Late cause");

      exception.initCause(cause);
      assertSame(cause, exception.getCause(), "Cause should be set by initCause");
    }
  }

  @Nested
  @DisplayName("Stack Trace Tests")
  class StackTraceTests {

    @Test
    @DisplayName("Exception should have stack trace")
    void exceptionShouldHaveStackTrace() {
      final PanamaException exception = new PanamaException("Test");
      final StackTraceElement[] stackTrace = exception.getStackTrace();
      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should have elements");
    }

    @Test
    @DisplayName("Exception should support setStackTrace")
    void exceptionShouldSupportSetStackTrace() {
      final PanamaException exception = new PanamaException("Test");
      final StackTraceElement[] newTrace = new StackTraceElement[0];
      exception.setStackTrace(newTrace);
      assertEquals(0, exception.getStackTrace().length, "Stack trace should be empty");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include class name")
    void toStringShouldIncludeClassName() {
      final PanamaException exception = new PanamaException("Test message");
      final String str = exception.toString();
      assertTrue(str.contains("PanamaException"), "toString should include class name");
    }

    @Test
    @DisplayName("toString should include message")
    void toStringShouldIncludeMessage() {
      final PanamaException exception = new PanamaException("Test message");
      final String str = exception.toString();
      assertTrue(str.contains("Test message"), "toString should include message");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Exception should work in try-catch")
    void exceptionShouldWorkInTryCatch() {
      boolean caught = false;
      try {
        throw new PanamaException("Test");
      } catch (PanamaException e) {
        caught = true;
        assertEquals("Test", e.getMessage(), "Message should match");
      }
      assertTrue(caught, "Exception should be caught");
    }

    @Test
    @DisplayName("Exception should work with try-with-resources style cleanup")
    void exceptionShouldWorkWithResourceCleanup() {
      boolean cleanedUp = false;
      try {
        throw new PanamaException("Resource failure");
      } catch (PanamaException e) {
        // Simulate resource cleanup
        cleanedUp = true;
      }
      assertTrue(cleanedUp, "Resource cleanup should occur");
    }

    @Test
    @DisplayName("Exception should support suppressed exceptions")
    void exceptionShouldSupportSuppressedExceptions() {
      final PanamaException exception = new PanamaException("Primary");
      final RuntimeException suppressed = new RuntimeException("Suppressed");

      exception.addSuppressed(suppressed);

      final Throwable[] suppressedExceptions = exception.getSuppressed();
      assertEquals(1, suppressedExceptions.length, "Should have 1 suppressed exception");
      assertSame(suppressed, suppressedExceptions[0], "Suppressed exception should match");
    }
  }
}

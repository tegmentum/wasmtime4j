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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaResourceException} class.
 *
 * <p>This test class verifies the exception for native resource management issues.
 */
@DisplayName("PanamaResourceException Tests")
class PanamaResourceExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaResourceException should extend PanamaException")
    void shouldExtendPanamaException() {
      assertTrue(
          PanamaException.class.isAssignableFrom(PanamaResourceException.class),
          "PanamaResourceException should extend PanamaException");
    }

    @Test
    @DisplayName("PanamaResourceException should be final")
    void shouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaResourceException.class.getModifiers()),
          "PanamaResourceException should be final");
    }

    @Test
    @DisplayName("PanamaResourceException should have serialVersionUID")
    void shouldHaveSerialVersionUID() throws NoSuchFieldException {
      final var field = PanamaResourceException.class.getDeclaredField("serialVersionUID");
      assertNotNull(field, "Should have serialVersionUID field");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(field.getModifiers()),
          "serialVersionUID should be static");
      assertTrue(
          java.lang.reflect.Modifier.isFinal(field.getModifiers()),
          "serialVersionUID should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests - Message Only")
  class MessageConstructorTests {

    @Test
    @DisplayName("Constructor should accept message")
    void constructorShouldAcceptMessage() {
      final PanamaResourceException exception =
          new PanamaResourceException("Resource allocation failed");
      assertEquals("Resource allocation failed", exception.getMessage(), "Message should match");
    }

    @Test
    @DisplayName("Constructor should accept null message")
    void constructorShouldAcceptNullMessage() {
      final PanamaResourceException exception = new PanamaResourceException((String) null);
      assertNull(exception.getMessage(), "Message should be null");
    }

    @Test
    @DisplayName("Constructor should accept empty message")
    void constructorShouldAcceptEmptyMessage() {
      final PanamaResourceException exception = new PanamaResourceException("");
      assertEquals("", exception.getMessage(), "Message should be empty");
    }

    @Test
    @DisplayName("Constructor should have null cause")
    void constructorShouldHaveNullCause() {
      final PanamaResourceException exception = new PanamaResourceException("Test message");
      assertNull(exception.getCause(), "Cause should be null");
    }
  }

  @Nested
  @DisplayName("Constructor Tests - Message and Cause")
  class MessageAndCauseConstructorTests {

    @Test
    @DisplayName("Constructor should accept message and cause")
    void constructorShouldAcceptMessageAndCause() {
      final OutOfMemoryError cause = new OutOfMemoryError("Native heap exhausted");
      final PanamaResourceException exception =
          new PanamaResourceException("Resource allocation failed", cause);
      assertEquals("Resource allocation failed", exception.getMessage(), "Message should match");
      assertSame(cause, exception.getCause(), "Cause should match");
    }

    @Test
    @DisplayName("Constructor should accept null cause with message")
    void constructorShouldAcceptNullCauseWithMessage() {
      final PanamaResourceException exception = new PanamaResourceException("Test message", null);
      assertEquals("Test message", exception.getMessage(), "Message should match");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor should accept null message with cause")
    void constructorShouldAcceptNullMessageWithCause() {
      final RuntimeException cause = new RuntimeException("Root cause");
      final PanamaResourceException exception = new PanamaResourceException(null, cause);
      assertNull(exception.getMessage(), "Message should be null");
      assertSame(cause, exception.getCause(), "Cause should match");
    }
  }

  @Nested
  @DisplayName("Constructor Tests - Message and Native Error Code")
  class MessageAndNativeErrorCodeConstructorTests {

    @Test
    @DisplayName("Constructor should accept message and error code")
    void constructorShouldAcceptMessageAndErrorCode() {
      final PanamaResourceException exception =
          new PanamaResourceException("Memory allocation failed", -1);
      assertNotNull(exception.getMessage(), "Message should not be null");
      assertTrue(
          exception.getMessage().contains("Memory allocation failed"),
          "Message should contain original message");
      assertTrue(exception.getMessage().contains("-1"), "Message should contain error code");
      assertTrue(
          exception.getMessage().contains("native error code"),
          "Message should contain 'native error code'");
    }

    @Test
    @DisplayName("Constructor should include zero error code")
    void constructorShouldIncludeZeroErrorCode() {
      final PanamaResourceException exception = new PanamaResourceException("Operation failed", 0);
      assertTrue(exception.getMessage().contains("0"), "Message should contain error code 0");
    }

    @Test
    @DisplayName("Constructor should include positive error code")
    void constructorShouldIncludePositiveErrorCode() {
      final PanamaResourceException exception =
          new PanamaResourceException("Operation failed", 12345);
      assertTrue(
          exception.getMessage().contains("12345"), "Message should contain positive error code");
    }

    @Test
    @DisplayName("Constructor should include large negative error code")
    void constructorShouldIncludeLargeNegativeErrorCode() {
      final PanamaResourceException exception =
          new PanamaResourceException("Operation failed", Integer.MIN_VALUE);
      assertTrue(
          exception.getMessage().contains(String.valueOf(Integer.MIN_VALUE)),
          "Message should contain MIN_VALUE error code");
    }

    @Test
    @DisplayName("Constructor should have null cause with error code")
    void constructorShouldHaveNullCauseWithErrorCode() {
      final PanamaResourceException exception = new PanamaResourceException("Test", 1);
      assertNull(exception.getCause(), "Cause should be null");
    }
  }

  @Nested
  @DisplayName("Constructor Tests - Message, Cause, and Native Error Code")
  class MessageCauseAndNativeErrorCodeConstructorTests {

    @Test
    @DisplayName("Constructor should accept message, cause, and error code")
    void constructorShouldAcceptMessageCauseAndErrorCode() {
      final RuntimeException cause = new RuntimeException("Underlying error");
      final PanamaResourceException exception =
          new PanamaResourceException("Resource failed", cause, -42);

      assertNotNull(exception.getMessage(), "Message should not be null");
      assertTrue(
          exception.getMessage().contains("Resource failed"),
          "Message should contain original message");
      assertTrue(exception.getMessage().contains("-42"), "Message should contain error code");
      assertSame(cause, exception.getCause(), "Cause should match");
    }

    @Test
    @DisplayName("Constructor should handle null cause with error code")
    void constructorShouldHandleNullCauseWithErrorCode() {
      final PanamaResourceException exception =
          new PanamaResourceException("Resource failed", null, -42);

      assertTrue(
          exception.getMessage().contains("Resource failed"),
          "Message should contain original message");
      assertTrue(exception.getMessage().contains("-42"), "Message should contain error code");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor should handle null message with cause and error code")
    void constructorShouldHandleNullMessageWithCauseAndErrorCode() {
      final RuntimeException cause = new RuntimeException("Root cause");
      final PanamaResourceException exception = new PanamaResourceException(null, cause, 100);

      assertTrue(exception.getMessage().contains("100"), "Message should contain error code");
      assertSame(cause, exception.getCause(), "Cause should match");
    }
  }

  @Nested
  @DisplayName("Exception Hierarchy Tests")
  class ExceptionHierarchyTests {

    @Test
    @DisplayName("Should be catchable as PanamaException")
    void shouldBeCatchableAsPanamaException() {
      try {
        throw new PanamaResourceException("Resource error");
      } catch (PanamaException e) {
        assertTrue(
            e instanceof PanamaResourceException, "Should be instanceof PanamaResourceException");
      }
    }

    @Test
    @DisplayName("Should be catchable as Exception")
    void shouldBeCatchableAsException() {
      try {
        throw new PanamaResourceException("Resource error");
      } catch (Exception e) {
        assertTrue(
            e instanceof PanamaResourceException, "Should be instanceof PanamaResourceException");
      }
    }

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      try {
        throw new PanamaResourceException("Resource error");
      } catch (Throwable t) {
        assertTrue(
            t instanceof PanamaResourceException, "Should be instanceof PanamaResourceException");
      }
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include class name")
    void toStringShouldIncludeClassName() {
      final PanamaResourceException exception = new PanamaResourceException("Test message");
      final String str = exception.toString();
      assertTrue(str.contains("PanamaResourceException"), "toString should include class name");
    }

    @Test
    @DisplayName("toString should include message")
    void toStringShouldIncludeMessage() {
      final PanamaResourceException exception = new PanamaResourceException("Resource test");
      final String str = exception.toString();
      assertTrue(str.contains("Resource test"), "toString should include message");
    }

    @Test
    @DisplayName("toString with error code should include code")
    void toStringWithErrorCodeShouldIncludeCode() {
      final PanamaResourceException exception = new PanamaResourceException("Fail", -500);
      final String str = exception.toString();
      assertTrue(str.contains("-500"), "toString should include error code");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Exception should work for allocation failure scenario")
    void exceptionShouldWorkForAllocationFailureScenario() {
      boolean caught = false;
      try {
        throw new PanamaResourceException("Native memory allocation failed", -1);
      } catch (PanamaResourceException e) {
        caught = true;
        assertTrue(
            e.getMessage().contains("allocation"), "Message should describe allocation issue");
        assertTrue(e.getMessage().contains("-1"), "Message should include error code");
      }
      assertTrue(caught, "Exception should be caught");
    }

    @Test
    @DisplayName("Exception should work for deallocation failure scenario")
    void exceptionShouldWorkForDeallocationFailureScenario() {
      try {
        throw new PanamaResourceException("Native resource deallocation failed");
      } catch (PanamaResourceException e) {
        assertTrue(
            e.getMessage().contains("deallocation"), "Message should describe deallocation issue");
      }
    }

    @Test
    @DisplayName("Exception should work for use-after-free scenario")
    void exceptionShouldWorkForUseAfterFreeScenario() {
      try {
        throw new PanamaResourceException("Native resource accessed after being freed");
      } catch (PanamaResourceException e) {
        assertTrue(
            e.getMessage().contains("freed"), "Message should describe use-after-free issue");
      }
    }

    @Test
    @DisplayName("Exception should work with wrapped native error")
    void exceptionShouldWorkWithWrappedNativeError() {
      final IllegalStateException nativeError =
          new IllegalStateException("Native library returned error");

      try {
        throw new PanamaResourceException("Failed to allocate", nativeError, -12);
      } catch (PanamaResourceException e) {
        assertNotNull(e.getCause(), "Should have cause");
        assertTrue(e.getMessage().contains("-12"), "Should include native error code");
      }
    }

    @Test
    @DisplayName("Exception should support suppressed exceptions")
    void exceptionShouldSupportSuppressedExceptions() {
      final PanamaResourceException exception = new PanamaResourceException("Primary failure");
      final RuntimeException cleanupError = new RuntimeException("Cleanup failed");

      exception.addSuppressed(cleanupError);

      final Throwable[] suppressed = exception.getSuppressed();
      assertEquals(1, suppressed.length, "Should have 1 suppressed exception");
      assertSame(cleanupError, suppressed[0], "Suppressed exception should match");
    }

    @Test
    @DisplayName("Exception chain with resource errors should work")
    void exceptionChainWithResourceErrorsShouldWork() {
      final IllegalStateException invalidState = new IllegalStateException("Invalid arena state");
      final PanamaResourceException resourceError =
          new PanamaResourceException("Arena operation failed", invalidState);
      final PanamaResourceException wrapper =
          new PanamaResourceException("Resource management failed", resourceError);

      assertEquals(resourceError, wrapper.getCause(), "Immediate cause should match");
      assertEquals(invalidState, wrapper.getCause().getCause(), "Root cause should match");
    }
  }
}

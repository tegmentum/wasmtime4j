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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcException} class.
 *
 * <p>GcException is thrown when WebAssembly GC operations fail.
 */
@DisplayName("GcException Tests")
class GcExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(GcException.class.getModifiers()), "GcException should be public");
    }

    @Test
    @DisplayName("should extend RuntimeException")
    void shouldExtendRuntimeException() {
      assertTrue(
          ai.tegmentum.wasmtime4j.exception.RuntimeException.class.isAssignableFrom(
              GcException.class),
          "GcException should extend RuntimeException");
    }

    @Test
    @DisplayName("should have serialVersionUID field")
    void shouldHaveSerialVersionUID() throws NoSuchFieldException {
      final var field = GcException.class.getDeclaredField("serialVersionUID");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "serialVersionUID should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "serialVersionUID should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "serialVersionUID should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with message")
    void shouldHaveConstructorWithMessage() throws NoSuchMethodException {
      final Constructor<GcException> constructor = GcException.class.getConstructor(String.class);
      assertNotNull(constructor, "Constructor with message should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message and cause")
    void shouldHaveConstructorWithMessageAndCause() throws NoSuchMethodException {
      final Constructor<GcException> constructor =
          GcException.class.getConstructor(String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with message and cause should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final String message = "GC heap overflow";
      final GcException exception = new GcException(message);

      assertTrue(
          exception.getMessage().contains(message), "Message should contain the original message");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Failed to allocate struct";
      final java.lang.RuntimeException cause = new java.lang.RuntimeException("Out of memory");
      final GcException exception = new GcException(message, cause);

      assertTrue(
          exception.getMessage().contains(message), "Message should contain the original message");
      assertSame(cause, exception.getCause(), "Cause should be set correctly");
    }
  }

  @Nested
  @DisplayName("Exception Behavior Tests")
  class ExceptionBehaviorTests {

    @Test
    @DisplayName("should be unchecked exception")
    void shouldBeUncheckedException() {
      assertTrue(
          ai.tegmentum.wasmtime4j.exception.RuntimeException.class.isAssignableFrom(
              GcException.class),
          "GcException should be an unchecked exception");
    }

    @Test
    @DisplayName("should be catchable as RuntimeException")
    void shouldBeCatchableAsRuntimeException() {
      boolean caught = false;
      try {
        throw new GcException("GC error");
      } catch (final ai.tegmentum.wasmtime4j.exception.RuntimeException e) {
        caught = true;
        assertTrue(e instanceof GcException, "Should be instance of GcException");
      }
      assertTrue(caught, "Exception should be caught as RuntimeException");
    }

    @Test
    @DisplayName("should preserve stack trace")
    void shouldPreserveStackTrace() {
      final GcException exception = new GcException("Test");
      final StackTraceElement[] stackTrace = exception.getStackTrace();

      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
    }
  }

  @Nested
  @DisplayName("GC Error Scenario Tests")
  class GcErrorScenarioTests {

    @Test
    @DisplayName("should handle struct field access error")
    void shouldHandleStructFieldAccessError() {
      final GcException exception =
          new GcException("Cannot access field 5: struct only has 3 fields");

      assertTrue(
          exception.getMessage().contains("field"), "Message should describe field access error");
    }

    @Test
    @DisplayName("should handle array bounds error")
    void shouldHandleArrayBoundsError() {
      final GcException exception = new GcException("Array index 10 out of bounds for length 5");

      assertTrue(exception.getMessage().contains("bounds"), "Message should describe bounds error");
    }

    @Test
    @DisplayName("should handle type cast error")
    void shouldHandleTypeCastError() {
      final GcException exception = new GcException("Cannot cast structref to arrayref");

      assertTrue(exception.getMessage().contains("cast"), "Message should describe cast error");
    }

    @Test
    @DisplayName("should handle null reference error")
    void shouldHandleNullReferenceError() {
      final GcException exception = new GcException("Cannot dereference null reference");

      assertTrue(
          exception.getMessage().contains("null"), "Message should describe null reference error");
    }

    @Test
    @DisplayName("should handle allocation failure error")
    void shouldHandleAllocationFailureError() {
      final GcException exception = new GcException("Failed to allocate GC object: heap exhausted");

      assertTrue(
          exception.getMessage().contains("allocate"),
          "Message should describe allocation failure");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should reject null message")
    void shouldRejectNullMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new GcException((String) null),
          "Null message should be rejected");
    }

    @Test
    @DisplayName("should reject empty message")
    void shouldRejectEmptyMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new GcException(""),
          "Empty message should be rejected");
    }

    @Test
    @DisplayName("should chain causes correctly")
    void shouldChainCausesCorrectly() {
      final IllegalArgumentException root = new IllegalArgumentException("Root");
      final RuntimeException middle = new RuntimeException("Middle", root);
      final GcException exception = new GcException("Top", middle);

      assertSame(middle, exception.getCause(), "Immediate cause should be middle");
      assertSame(root, exception.getCause().getCause(), "Root cause should be accessible");
    }
  }
}

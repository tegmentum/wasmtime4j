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
 * Tests for {@link ResourceException} class.
 *
 * <p>ResourceException is thrown when WebAssembly resource management errors occur.
 */
@DisplayName("ResourceException Tests")
class ResourceExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with message")
    void shouldHaveConstructorWithMessage() throws NoSuchMethodException {
      final Constructor<ResourceException> constructor =
          ResourceException.class.getConstructor(String.class);
      assertNotNull(constructor, "Constructor with message should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message and cause")
    void shouldHaveConstructorWithMessageAndCause() throws NoSuchMethodException {
      final Constructor<ResourceException> constructor =
          ResourceException.class.getConstructor(String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with message and cause should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final String message = "Failed to allocate memory resource";
      final ResourceException exception = new ResourceException(message);

      assertEquals(message, exception.getMessage(), "Message should be set correctly");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Resource deallocation failed";
      final RuntimeException cause = new RuntimeException("Double free detected");
      final ResourceException exception = new ResourceException(message, cause);

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
        throw new ResourceException("Resource error");
      } catch (final WasmException e) {
        caught = true;
        assertTrue(e instanceof ResourceException, "Should be instance of ResourceException");
      }
      assertTrue(caught, "Exception should be caught as WasmException");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new ResourceException("Resource error");
      } catch (final Exception e) {
        caught = true;
        assertTrue(e instanceof ResourceException, "Should be instance of ResourceException");
      }
      assertTrue(caught, "Exception should be caught as Exception");
    }

    @Test
    @DisplayName("should preserve stack trace")
    void shouldPreserveStackTrace() {
      final ResourceException exception = new ResourceException("Test");
      final StackTraceElement[] stackTrace = exception.getStackTrace();

      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
    }
  }

  @Nested
  @DisplayName("Resource Error Scenario Tests")
  class ResourceErrorScenarioTests {

    @Test
    @DisplayName("should handle memory allocation failure error")
    void shouldHandleMemoryAllocationFailureError() {
      final ResourceException exception =
          new ResourceException("Memory allocation failed: exceeded maximum of 65536 pages");

      assertTrue(
          exception.getMessage().contains("allocation failed"),
          "Message should describe allocation failure");
    }

    @Test
    @DisplayName("should handle resource handle invalid error")
    void shouldHandleResourceHandleInvalidError() {
      final ResourceException exception =
          new ResourceException("Invalid resource handle: handle 42 has been deallocated");

      assertTrue(
          exception.getMessage().contains("Invalid resource handle"),
          "Message should describe invalid handle");
    }

    @Test
    @DisplayName("should handle resource limit exceeded error")
    void shouldHandleResourceLimitExceededError() {
      final ResourceException exception =
          new ResourceException("Resource limit exceeded: maximum table entries reached");

      assertTrue(
          exception.getMessage().contains("limit exceeded"),
          "Message should describe limit exceeded error");
    }
  }
}

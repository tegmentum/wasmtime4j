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
 * Tests for {@link InstantiationException} class.
 *
 * <p>InstantiationException is thrown when WebAssembly module instantiation fails.
 */
@DisplayName("InstantiationException Tests")
class InstantiationExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with message")
    void shouldHaveConstructorWithMessage() throws NoSuchMethodException {
      final Constructor<InstantiationException> constructor =
          InstantiationException.class.getConstructor(String.class);
      assertNotNull(constructor, "Constructor with message should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message and cause")
    void shouldHaveConstructorWithMessageAndCause() throws NoSuchMethodException {
      final Constructor<InstantiationException> constructor =
          InstantiationException.class.getConstructor(String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with message and cause should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
      final String message = "Missing required import: env.memory";
      final InstantiationException exception = new InstantiationException(message);

      assertEquals(message, exception.getMessage(), "Message should be set correctly");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Failed to instantiate module";
      final RuntimeException cause = new RuntimeException("Import resolution failed");
      final InstantiationException exception = new InstantiationException(message, cause);

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
        throw new InstantiationException("Instantiation failed");
      } catch (final WasmException e) {
        caught = true;
        assertTrue(
            e instanceof InstantiationException, "Should be instance of InstantiationException");
      }
      assertTrue(caught, "Exception should be caught as WasmException");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new InstantiationException("Instantiation failed");
      } catch (final Exception e) {
        caught = true;
        assertTrue(
            e instanceof InstantiationException, "Should be instance of InstantiationException");
      }
      assertTrue(caught, "Exception should be caught as Exception");
    }

    @Test
    @DisplayName("should preserve stack trace")
    void shouldPreserveStackTrace() {
      final InstantiationException exception = new InstantiationException("Test");
      final StackTraceElement[] stackTrace = exception.getStackTrace();

      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
    }
  }

  @Nested
  @DisplayName("Instantiation Error Scenario Tests")
  class InstantiationErrorScenarioTests {

    @Test
    @DisplayName("should handle missing import error")
    void shouldHandleMissingImportError() {
      final InstantiationException exception =
          new InstantiationException("Missing required import: env.memory");

      assertTrue(
          exception.getMessage().contains("Missing"),
          "Message should describe missing import error");
    }

    @Test
    @DisplayName("should handle incompatible import type error")
    void shouldHandleIncompatibleImportTypeError() {
      final InstantiationException exception =
          new InstantiationException(
              "Incompatible import type for env.table: expected funcref, got externref");

      assertTrue(
          exception.getMessage().contains("Incompatible"),
          "Message should describe import type mismatch");
    }

    @Test
    @DisplayName("should handle resource allocation failure error")
    void shouldHandleResourceAllocationFailureError() {
      final InstantiationException exception =
          new InstantiationException("Failed to allocate linear memory: requested 256 pages");

      assertTrue(
          exception.getMessage().contains("allocate"),
          "Message should describe allocation failure");
    }
  }
}

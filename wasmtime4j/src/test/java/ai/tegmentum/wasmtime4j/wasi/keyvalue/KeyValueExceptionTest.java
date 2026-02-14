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

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link KeyValueException} class.
 *
 * <p>KeyValueException is thrown when key-value operations fail in WASI-keyvalue.
 */
@DisplayName("KeyValueException Tests")
class KeyValueExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(KeyValueException.class.getModifiers()),
          "KeyValueException should be public");
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(KeyValueException.class),
          "KeyValueException should extend WasmException");
    }

    @Test
    @DisplayName("should have serialVersionUID field")
    void shouldHaveSerialVersionUID() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          KeyValueException.class.getDeclaredField("serialVersionUID");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "serialVersionUID should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "serialVersionUID should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "serialVersionUID should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      final String message = "Key-value operation failed";
      final KeyValueException exception = new KeyValueException(message);

      assertEquals(message, exception.getMessage(), "Message should match the provided message");
      assertNull(exception.getCause(), "Cause should be null when not provided");
      assertEquals(
          KeyValueErrorCode.UNKNOWN,
          exception.getErrorCode(),
          "Error code should default to UNKNOWN when not provided");
    }

    @Test
    @DisplayName("should create exception with message and error code")
    void shouldCreateExceptionWithMessageAndErrorCode() {
      final String message = "Key not found in store";
      final KeyValueErrorCode errorCode = KeyValueErrorCode.KEY_NOT_FOUND;
      final KeyValueException exception = new KeyValueException(message, errorCode);

      assertEquals(message, exception.getMessage(), "Message should match the provided message");
      assertNull(exception.getCause(), "Cause should be null when not provided");
      assertEquals(
          errorCode, exception.getErrorCode(), "Error code should match the provided error code");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Storage backend failure";
      final RuntimeException cause = new RuntimeException("Connection refused");
      final KeyValueException exception = new KeyValueException(message, cause);

      assertEquals(message, exception.getMessage(), "Message should match the provided message");
      assertSame(cause, exception.getCause(), "Cause should match the provided cause");
      assertEquals(
          KeyValueErrorCode.UNKNOWN,
          exception.getErrorCode(),
          "Error code should default to UNKNOWN when not provided");
    }

    @Test
    @DisplayName("should create exception with message, error code, and cause")
    void shouldCreateExceptionWithMessageErrorCodeAndCause() {
      final String message = "Connection failed";
      final KeyValueErrorCode errorCode = KeyValueErrorCode.CONNECTION_FAILED;
      final RuntimeException cause = new RuntimeException("Concurrent modification");
      final KeyValueException exception = new KeyValueException(message, errorCode, cause);

      assertEquals(message, exception.getMessage(), "Message should match the provided message");
      assertEquals(
          errorCode, exception.getErrorCode(), "Error code should match the provided error code");
      assertSame(cause, exception.getCause(), "Cause should match the provided cause");
    }
  }

  @Nested
  @DisplayName("Error Code Tests")
  class ErrorCodeTests {

    @Test
    @DisplayName("should return correct error code via getErrorCode")
    void shouldReturnCorrectErrorCode() {
      final KeyValueException exception =
          new KeyValueException("Timeout", KeyValueErrorCode.TIMEOUT);

      assertEquals(
          KeyValueErrorCode.TIMEOUT,
          exception.getErrorCode(),
          "getErrorCode should return the error code set at construction");
    }

    @Test
    @DisplayName("should default to UNKNOWN error code")
    void shouldDefaultToUnknownErrorCode() {
      final KeyValueException exception = new KeyValueException("Unknown error");

      assertEquals(
          KeyValueErrorCode.UNKNOWN,
          exception.getErrorCode(),
          "Error code should default to UNKNOWN for message-only constructor");
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
        throw new KeyValueException("Key-value error");
      } catch (final WasmException e) {
        caught = true;
        assertTrue(e instanceof KeyValueException, "Should be instance of KeyValueException");
      }
      assertTrue(caught, "Exception should be caught as WasmException");
    }

    @Test
    @DisplayName("should be catchable as Exception")
    void shouldBeCatchableAsException() {
      boolean caught = false;
      try {
        throw new KeyValueException("Key-value error");
      } catch (final Exception e) {
        caught = true;
        assertTrue(e instanceof KeyValueException, "Should be instance of KeyValueException");
      }
      assertTrue(caught, "Exception should be caught as Exception");
    }

    @Test
    @DisplayName("should preserve stack trace")
    void shouldPreserveStackTrace() {
      final KeyValueException exception = new KeyValueException("Test");
      final StackTraceElement[] stackTrace = exception.getStackTrace();

      assertNotNull(stackTrace, "Stack trace should not be null");
      assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
    }
  }

  @Nested
  @DisplayName("Error Scenario Tests")
  class ErrorScenarioTests {

    @Test
    @DisplayName("should handle key not found error message")
    void shouldHandleKeyNotFoundErrorMessage() {
      final KeyValueException exception =
          new KeyValueException(
              "Key 'user:12345' not found in store", KeyValueErrorCode.KEY_NOT_FOUND);

      assertTrue(
          exception.getMessage().contains("not found"),
          "Message should contain 'not found' keyword");
      assertTrue(exception.getMessage().contains("Key"), "Message should contain 'Key' keyword");
    }

    @Test
    @DisplayName("should handle capacity exceeded error message")
    void shouldHandleCapacityExceededErrorMessage() {
      final KeyValueException exception =
          new KeyValueException(
              "Storage capacity exceeded: 10GB limit reached", KeyValueErrorCode.CAPACITY_EXCEEDED);

      assertTrue(
          exception.getMessage().contains("capacity"), "Message should contain 'capacity' keyword");
      assertTrue(
          exception.getMessage().contains("exceeded"), "Message should contain 'exceeded' keyword");
    }

    @Test
    @DisplayName("should handle connection failure error message")
    void shouldHandleConnectionFailureErrorMessage() {
      final KeyValueException exception =
          new KeyValueException(
              "Connection to storage backend failed after 3 retries",
              KeyValueErrorCode.CONNECTION_FAILED);

      assertTrue(
          exception.getMessage().contains("Connection"),
          "Message should contain 'Connection' keyword");
      assertTrue(
          exception.getMessage().contains("failed"), "Message should contain 'failed' keyword");
    }

    @Test
    @DisplayName("should handle read-only store error message")
    void shouldHandleReadOnlyStoreErrorMessage() {
      final KeyValueException exception =
          new KeyValueException(
              "Cannot write to read-only storage backend", KeyValueErrorCode.READ_ONLY);

      assertTrue(
          exception.getMessage().contains("read-only"),
          "Message should contain 'read-only' keyword");
    }
  }
}

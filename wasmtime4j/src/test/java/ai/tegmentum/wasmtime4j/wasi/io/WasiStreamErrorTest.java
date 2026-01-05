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

package ai.tegmentum.wasmtime4j.wasi.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiStreamError class.
 *
 * <p>Tests the static factory methods, instance methods, and error type handling of
 * WasiStreamError.
 */
@DisplayName("WasiStreamError Class Tests")
class WasiStreamErrorTest {

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      WasiStreamError error = WasiStreamError.closed();
      assertTrue(error instanceof WasmException, "Should extend WasmException");
    }

    @Test
    @DisplayName("should extend Exception")
    void shouldExtendException() {
      WasiStreamError error = WasiStreamError.closed();
      assertTrue(error instanceof Exception, "Should extend Exception");
    }
  }

  // ========================================================================
  // ErrorType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ErrorType Enum Tests")
  class ErrorTypeEnumTests {

    @Test
    @DisplayName("should have LAST_OPERATION_FAILED type")
    void shouldHaveLastOperationFailedType() {
      assertNotNull(
          WasiStreamError.ErrorType.LAST_OPERATION_FAILED,
          "LAST_OPERATION_FAILED type should exist");
    }

    @Test
    @DisplayName("should have CLOSED type")
    void shouldHaveClosedType() {
      assertNotNull(WasiStreamError.ErrorType.CLOSED, "CLOSED type should exist");
    }

    @Test
    @DisplayName("should have exactly 2 error types")
    void shouldHaveExactly2ErrorTypes() {
      assertEquals(2, WasiStreamError.ErrorType.values().length, "Should have 2 error types");
    }
  }

  // ========================================================================
  // Factory Method Tests - lastOperationFailed
  // ========================================================================

  @Nested
  @DisplayName("lastOperationFailed Factory Method Tests")
  class LastOperationFailedFactoryTests {

    @Test
    @DisplayName("should create error with message and details")
    void shouldCreateErrorWithMessageAndDetails() {
      Object details = new Object();
      WasiStreamError error = WasiStreamError.lastOperationFailed("Test error", details);

      assertNotNull(error, "Error should not be null");
      assertEquals("Test error", error.getMessage(), "Message should match");
      assertEquals(
          WasiStreamError.ErrorType.LAST_OPERATION_FAILED,
          error.getErrorType(),
          "Error type should be LAST_OPERATION_FAILED");
      assertTrue(error.getErrorDetails().isPresent(), "Error details should be present");
      assertEquals(details, error.getErrorDetails().get(), "Error details should match");
    }

    @Test
    @DisplayName("should create error with message only")
    void shouldCreateErrorWithMessageOnly() {
      WasiStreamError error = WasiStreamError.lastOperationFailed("Test error");

      assertNotNull(error, "Error should not be null");
      assertEquals("Test error", error.getMessage(), "Message should match");
      assertEquals(
          WasiStreamError.ErrorType.LAST_OPERATION_FAILED,
          error.getErrorType(),
          "Error type should be LAST_OPERATION_FAILED");
      assertFalse(error.getErrorDetails().isPresent(), "Error details should not be present");
    }

    @Test
    @DisplayName("should create error with null details")
    void shouldCreateErrorWithNullDetails() {
      WasiStreamError error = WasiStreamError.lastOperationFailed("Test error", null);

      assertNotNull(error, "Error should not be null");
      assertFalse(error.getErrorDetails().isPresent(), "Error details should not be present");
    }
  }

  // ========================================================================
  // Factory Method Tests - closed
  // ========================================================================

  @Nested
  @DisplayName("closed Factory Method Tests")
  class ClosedFactoryTests {

    @Test
    @DisplayName("should create closed error with message")
    void shouldCreateClosedErrorWithMessage() {
      WasiStreamError error = WasiStreamError.closed("Stream closed unexpectedly");

      assertNotNull(error, "Error should not be null");
      assertEquals("Stream closed unexpectedly", error.getMessage(), "Message should match");
      assertEquals(
          WasiStreamError.ErrorType.CLOSED, error.getErrorType(), "Error type should be CLOSED");
      assertFalse(error.getErrorDetails().isPresent(), "Error details should not be present");
    }

    @Test
    @DisplayName("should create closed error with default message")
    void shouldCreateClosedErrorWithDefaultMessage() {
      WasiStreamError error = WasiStreamError.closed();

      assertNotNull(error, "Error should not be null");
      assertEquals("Stream is closed", error.getMessage(), "Default message should be used");
      assertEquals(
          WasiStreamError.ErrorType.CLOSED, error.getErrorType(), "Error type should be CLOSED");
      assertFalse(error.getErrorDetails().isPresent(), "Error details should not be present");
    }
  }

  // ========================================================================
  // Instance Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Method Tests")
  class InstanceMethodTests {

    @Test
    @DisplayName("isClosed should return true for closed errors")
    void isClosedShouldReturnTrueForClosedErrors() {
      WasiStreamError error = WasiStreamError.closed();
      assertTrue(error.isClosed(), "isClosed should return true for closed errors");
    }

    @Test
    @DisplayName("isClosed should return false for operation failed errors")
    void isClosedShouldReturnFalseForOperationFailedErrors() {
      WasiStreamError error = WasiStreamError.lastOperationFailed("Error");
      assertFalse(error.isClosed(), "isClosed should return false for operation failed errors");
    }

    @Test
    @DisplayName("isOperationFailed should return true for operation failed errors")
    void isOperationFailedShouldReturnTrueForOperationFailedErrors() {
      WasiStreamError error = WasiStreamError.lastOperationFailed("Error");
      assertTrue(
          error.isOperationFailed(),
          "isOperationFailed should return true for operation failed errors");
    }

    @Test
    @DisplayName("isOperationFailed should return false for closed errors")
    void isOperationFailedShouldReturnFalseForClosedErrors() {
      WasiStreamError error = WasiStreamError.closed();
      assertFalse(
          error.isOperationFailed(), "isOperationFailed should return false for closed errors");
    }
  }

  // ========================================================================
  // toString Tests
  // ========================================================================

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include error type and message for closed error")
    void toStringShouldIncludeErrorTypeAndMessageForClosedError() {
      WasiStreamError error = WasiStreamError.closed("Test closed");
      String result = error.toString();

      assertTrue(result.contains("WasiStreamError"), "Should contain class name");
      assertTrue(result.contains("CLOSED"), "Should contain error type");
      assertTrue(result.contains("Test closed"), "Should contain message");
    }

    @Test
    @DisplayName("toString should include error type and message for operation failed error")
    void toStringShouldIncludeErrorTypeAndMessageForOperationFailedError() {
      WasiStreamError error = WasiStreamError.lastOperationFailed("Test failed");
      String result = error.toString();

      assertTrue(result.contains("WasiStreamError"), "Should contain class name");
      assertTrue(result.contains("LAST_OPERATION_FAILED"), "Should contain error type");
      assertTrue(result.contains("Test failed"), "Should contain message");
    }

    @Test
    @DisplayName("toString should include error details when present")
    void toStringShouldIncludeErrorDetailsWhenPresent() {
      String details = "Detailed error info";
      WasiStreamError error = WasiStreamError.lastOperationFailed("Test failed", details);
      String result = error.toString();

      assertTrue(result.contains("errorDetails"), "Should contain errorDetails key");
      assertTrue(result.contains(details), "Should contain error details value");
    }

    @Test
    @DisplayName("toString should not include error details when absent")
    void toStringShouldNotIncludeErrorDetailsWhenAbsent() {
      WasiStreamError error = WasiStreamError.lastOperationFailed("Test failed");
      String result = error.toString();

      assertFalse(
          result.contains("errorDetails"), "Should not contain errorDetails when not present");
    }
  }

  // ========================================================================
  // Error Details Tests
  // ========================================================================

  @Nested
  @DisplayName("Error Details Tests")
  class ErrorDetailsTests {

    @Test
    @DisplayName("should handle string error details")
    void shouldHandleStringErrorDetails() {
      String details = "String details";
      WasiStreamError error = WasiStreamError.lastOperationFailed("Error", details);

      assertTrue(error.getErrorDetails().isPresent(), "Details should be present");
      assertEquals(details, error.getErrorDetails().get(), "Details should match");
    }

    @Test
    @DisplayName("should handle complex object error details")
    void shouldHandleComplexObjectErrorDetails() {
      Object complexDetails =
          new Object() {
            @Override
            public String toString() {
              return "ComplexDetails";
            }
          };
      WasiStreamError error = WasiStreamError.lastOperationFailed("Error", complexDetails);

      assertTrue(error.getErrorDetails().isPresent(), "Details should be present");
      assertEquals(complexDetails, error.getErrorDetails().get(), "Details should match");
    }

    @Test
    @DisplayName("should handle integer error details")
    void shouldHandleIntegerErrorDetails() {
      Integer details = 42;
      WasiStreamError error = WasiStreamError.lastOperationFailed("Error", details);

      assertTrue(error.getErrorDetails().isPresent(), "Details should be present");
      assertEquals(details, error.getErrorDetails().get(), "Details should match");
    }
  }
}

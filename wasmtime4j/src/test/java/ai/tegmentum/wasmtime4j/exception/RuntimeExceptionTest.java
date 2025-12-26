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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.RuntimeException.RuntimeErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link RuntimeException} class.
 *
 * <p>This test class verifies the construction and behavior of WebAssembly runtime exceptions,
 * including error types and recovery suggestions.
 */
@DisplayName("RuntimeException Tests")
class RuntimeExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("RuntimeException should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(RuntimeException.class),
          "RuntimeException should extend WasmException");
    }

    @Test
    @DisplayName("RuntimeException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(RuntimeException.class),
          "RuntimeException should be serializable");
    }

    @Test
    @DisplayName("RuntimeException should NOT extend java.lang.RuntimeException")
    void shouldNotExtendJavaRuntimeException() {
      // This class is ai.tegmentum.wasmtime4j.exception.RuntimeException
      // It should not be confused with java.lang.RuntimeException
      assertFalse(
          java.lang.RuntimeException.class.isAssignableFrom(RuntimeException.class),
          "RuntimeException should NOT extend java.lang.RuntimeException");
    }
  }

  @Nested
  @DisplayName("RuntimeErrorType Enum Tests")
  class RuntimeErrorTypeEnumTests {

    @Test
    @DisplayName("Should have TRAP value")
    void shouldHaveTrapValue() {
      assertNotNull(RuntimeErrorType.valueOf("TRAP"), "Should have TRAP value");
    }

    @Test
    @DisplayName("Should have FUNCTION_EXECUTION_FAILED value")
    void shouldHaveFunctionExecutionFailedValue() {
      assertNotNull(
          RuntimeErrorType.valueOf("FUNCTION_EXECUTION_FAILED"),
          "Should have FUNCTION_EXECUTION_FAILED value");
    }

    @Test
    @DisplayName("Should have HOST_FUNCTION_FAILED value")
    void shouldHaveHostFunctionFailedValue() {
      assertNotNull(
          RuntimeErrorType.valueOf("HOST_FUNCTION_FAILED"),
          "Should have HOST_FUNCTION_FAILED value");
    }

    @Test
    @DisplayName("Should have MEMORY_ACCESS_VIOLATION value")
    void shouldHaveMemoryAccessViolationValue() {
      assertNotNull(
          RuntimeErrorType.valueOf("MEMORY_ACCESS_VIOLATION"),
          "Should have MEMORY_ACCESS_VIOLATION value");
    }

    @Test
    @DisplayName("Should have STACK_ERROR value")
    void shouldHaveStackErrorValue() {
      assertNotNull(RuntimeErrorType.valueOf("STACK_ERROR"), "Should have STACK_ERROR value");
    }

    @Test
    @DisplayName("Should have TIMEOUT value")
    void shouldHaveTimeoutValue() {
      assertNotNull(RuntimeErrorType.valueOf("TIMEOUT"), "Should have TIMEOUT value");
    }

    @Test
    @DisplayName("Should have RESOURCE_EXHAUSTED value")
    void shouldHaveResourceExhaustedValue() {
      assertNotNull(
          RuntimeErrorType.valueOf("RESOURCE_EXHAUSTED"), "Should have RESOURCE_EXHAUSTED value");
    }

    @Test
    @DisplayName("Should have INTERRUPTED value")
    void shouldHaveInterruptedValue() {
      assertNotNull(RuntimeErrorType.valueOf("INTERRUPTED"), "Should have INTERRUPTED value");
    }

    @Test
    @DisplayName("Should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(RuntimeErrorType.valueOf("UNKNOWN"), "Should have UNKNOWN value");
    }

    @Test
    @DisplayName("Each error type should have description")
    void eachErrorTypeShouldHaveDescription() {
      for (final RuntimeErrorType type : RuntimeErrorType.values()) {
        assertNotNull(type.getDescription(), type.name() + " should have description");
        assertFalse(
            type.getDescription().isEmpty(), type.name() + " should have non-empty description");
      }
    }

    @Test
    @DisplayName("Should have 9 error types")
    void shouldHave9ErrorTypes() {
      assertEquals(9, RuntimeErrorType.values().length, "Should have 9 error types");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message only should set defaults")
    void constructorWithMessageOnly() {
      final RuntimeException exception = new RuntimeException("Runtime error occurred");

      assertTrue(
          exception.getMessage().contains("Runtime error occurred"),
          "Message should contain error text");
      assertEquals(
          RuntimeErrorType.UNKNOWN,
          exception.getErrorType(),
          "Error type should default to UNKNOWN");
      assertNull(exception.getFunctionName(), "Function name should be null");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor with message and cause should set both")
    void constructorWithMessageAndCause() {
      final Throwable cause = new Exception("Root cause");
      final RuntimeException exception = new RuntimeException("Error message", cause);

      assertTrue(
          exception.getMessage().contains("Error message"), "Message should contain error text");
      assertSame(cause, exception.getCause(), "Cause should be set");
      assertEquals(
          RuntimeErrorType.UNKNOWN,
          exception.getErrorType(),
          "Error type should default to UNKNOWN");
    }

    @Test
    @DisplayName("Constructor with error type and message should set both")
    void constructorWithErrorTypeAndMessage() {
      final RuntimeException exception =
          new RuntimeException(RuntimeErrorType.TRAP, "Trap occurred");

      assertEquals(RuntimeErrorType.TRAP, exception.getErrorType(), "Error type should be TRAP");
      assertTrue(
          exception.getMessage().contains("Trap occurred"), "Message should contain error text");
    }

    @Test
    @DisplayName("Full constructor should set all fields")
    void fullConstructorShouldSetAllFields() {
      final Throwable cause = new Exception("Root cause");
      final RuntimeException exception =
          new RuntimeException(
              RuntimeErrorType.FUNCTION_EXECUTION_FAILED, "Function failed", "my_function", cause);

      assertEquals(
          RuntimeErrorType.FUNCTION_EXECUTION_FAILED,
          exception.getErrorType(),
          "Error type should match");
      assertEquals(
          "my_function", exception.getFunctionName(), "Function name should be 'my_function'");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Constructor should handle null error type")
    void constructorShouldHandleNullErrorType() {
      final RuntimeException exception = new RuntimeException(null, "Error message", null, null);

      assertEquals(
          RuntimeErrorType.UNKNOWN,
          exception.getErrorType(),
          "Null error type should default to UNKNOWN");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getErrorType should return error type")
    void getErrorTypeShouldReturnErrorType() {
      final RuntimeException exception =
          new RuntimeException(RuntimeErrorType.MEMORY_ACCESS_VIOLATION, "Memory error");

      assertEquals(
          RuntimeErrorType.MEMORY_ACCESS_VIOLATION,
          exception.getErrorType(),
          "getErrorType should return MEMORY_ACCESS_VIOLATION");
    }

    @Test
    @DisplayName("getFunctionName should return function name")
    void getFunctionNameShouldReturnFunctionName() {
      final RuntimeException exception =
          new RuntimeException(RuntimeErrorType.UNKNOWN, "Error", "test_func", null);

      assertEquals(
          "test_func", exception.getFunctionName(), "getFunctionName should return 'test_func'");
    }

    @Test
    @DisplayName("getRecoverySuggestion should return non-null suggestion")
    void getRecoverySuggestionShouldReturnNonNull() {
      final RuntimeException exception = new RuntimeException(RuntimeErrorType.TIMEOUT, "Timeout");

      assertNotNull(exception.getRecoverySuggestion(), "Recovery suggestion should not be null");
      assertFalse(
          exception.getRecoverySuggestion().isEmpty(), "Recovery suggestion should not be empty");
    }
  }

  @Nested
  @DisplayName("Error Category Check Tests")
  class ErrorCategoryCheckTests {

    @Test
    @DisplayName("isTrapError should return true for TRAP type")
    void isTrapErrorShouldReturnTrueForTrapType() {
      final RuntimeException exception = new RuntimeException(RuntimeErrorType.TRAP, "Trap");

      assertTrue(exception.isTrapError(), "TRAP should be trap error");
    }

    @Test
    @DisplayName("isTrapError should return false for non-trap types")
    void isTrapErrorShouldReturnFalseForNonTrapTypes() {
      final RuntimeException exception = new RuntimeException(RuntimeErrorType.TIMEOUT, "Timeout");

      assertFalse(exception.isTrapError(), "TIMEOUT should not be trap error");
    }

    @Test
    @DisplayName("isFunctionError should return true for function errors")
    void isFunctionErrorShouldReturnTrueForFunctionErrors() {
      final RuntimeException funcExec =
          new RuntimeException(RuntimeErrorType.FUNCTION_EXECUTION_FAILED, "Error");
      final RuntimeException hostFunc =
          new RuntimeException(RuntimeErrorType.HOST_FUNCTION_FAILED, "Error");

      assertTrue(funcExec.isFunctionError(), "FUNCTION_EXECUTION_FAILED should be function error");
      assertTrue(hostFunc.isFunctionError(), "HOST_FUNCTION_FAILED should be function error");
    }

    @Test
    @DisplayName("isMemoryError should return true for memory errors")
    void isMemoryErrorShouldReturnTrueForMemoryErrors() {
      final RuntimeException memAccess =
          new RuntimeException(RuntimeErrorType.MEMORY_ACCESS_VIOLATION, "Error");
      final RuntimeException stack = new RuntimeException(RuntimeErrorType.STACK_ERROR, "Error");

      assertTrue(memAccess.isMemoryError(), "MEMORY_ACCESS_VIOLATION should be memory error");
      assertTrue(stack.isMemoryError(), "STACK_ERROR should be memory error");
    }

    @Test
    @DisplayName("isResourceError should return true for resource errors")
    void isResourceErrorShouldReturnTrueForResourceErrors() {
      final RuntimeException resourceExhausted =
          new RuntimeException(RuntimeErrorType.RESOURCE_EXHAUSTED, "Error");
      final RuntimeException timeout = new RuntimeException(RuntimeErrorType.TIMEOUT, "Error");

      assertTrue(
          resourceExhausted.isResourceError(), "RESOURCE_EXHAUSTED should be resource error");
      assertTrue(timeout.isResourceError(), "TIMEOUT should be resource error");
    }
  }

  @Nested
  @DisplayName("Recovery Suggestion Tests")
  class RecoverySuggestionTests {

    @Test
    @DisplayName("Each error type should have recovery suggestion")
    void eachErrorTypeShouldHaveRecoverySuggestion() {
      for (final RuntimeErrorType type : RuntimeErrorType.values()) {
        final RuntimeException exception = new RuntimeException(type, "Test error");
        assertNotNull(
            exception.getRecoverySuggestion(), type.name() + " should have recovery suggestion");
      }
    }
  }
}

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

  // ============================================================================
  // MUTATION TESTING COVERAGE TESTS
  // ============================================================================
  // The following tests are specifically designed to kill PIT mutations that
  // survive basic functionality tests. They test:
  // 1. Boolean return value mutations for category methods
  // 2. Exact count verification for error type categories
  // 3. formatMessage edge cases (null/empty functionName)
  // 4. generateRecoverySuggestion distinct suggestions
  // 5. Getter return value exactness
  // ============================================================================

  @Nested
  @DisplayName("Error Category Boolean Return Mutation Tests")
  class ErrorCategoryBooleanReturnMutationTests {

    // -------------------------------------------------------------------------
    // isTrapError() - Tests for false returns on non-TRAP types
    // This kills mutations that change "return false" to "return true"
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isTrapError returns false for FUNCTION_EXECUTION_FAILED")
    void isTrapErrorReturnsFalseForFunctionExecutionFailed() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.FUNCTION_EXECUTION_FAILED, "test");
      assertFalse(ex.isTrapError(), "FUNCTION_EXECUTION_FAILED should NOT be trap error");
    }

    @Test
    @DisplayName("isTrapError returns false for HOST_FUNCTION_FAILED")
    void isTrapErrorReturnsFalseForHostFunctionFailed() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.HOST_FUNCTION_FAILED, "test");
      assertFalse(ex.isTrapError(), "HOST_FUNCTION_FAILED should NOT be trap error");
    }

    @Test
    @DisplayName("isTrapError returns false for MEMORY_ACCESS_VIOLATION")
    void isTrapErrorReturnsFalseForMemoryAccessViolation() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.MEMORY_ACCESS_VIOLATION, "test");
      assertFalse(ex.isTrapError(), "MEMORY_ACCESS_VIOLATION should NOT be trap error");
    }

    @Test
    @DisplayName("isTrapError returns false for STACK_ERROR")
    void isTrapErrorReturnsFalseForStackError() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.STACK_ERROR, "test");
      assertFalse(ex.isTrapError(), "STACK_ERROR should NOT be trap error");
    }

    @Test
    @DisplayName("isTrapError returns false for TIMEOUT")
    void isTrapErrorReturnsFalseForTimeout() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.TIMEOUT, "test");
      assertFalse(ex.isTrapError(), "TIMEOUT should NOT be trap error");
    }

    @Test
    @DisplayName("isTrapError returns false for RESOURCE_EXHAUSTED")
    void isTrapErrorReturnsFalseForResourceExhausted() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.RESOURCE_EXHAUSTED, "test");
      assertFalse(ex.isTrapError(), "RESOURCE_EXHAUSTED should NOT be trap error");
    }

    @Test
    @DisplayName("isTrapError returns false for INTERRUPTED")
    void isTrapErrorReturnsFalseForInterrupted() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.INTERRUPTED, "test");
      assertFalse(ex.isTrapError(), "INTERRUPTED should NOT be trap error");
    }

    @Test
    @DisplayName("isTrapError returns false for UNKNOWN")
    void isTrapErrorReturnsFalseForUnknown() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.UNKNOWN, "test");
      assertFalse(ex.isTrapError(), "UNKNOWN should NOT be trap error");
    }

    // -------------------------------------------------------------------------
    // isFunctionError() - Tests for false returns on non-function types
    // This kills mutations that change "return false" to "return true"
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isFunctionError returns false for TRAP")
    void isFunctionErrorReturnsFalseForTrap() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.TRAP, "test");
      assertFalse(ex.isFunctionError(), "TRAP should NOT be function error");
    }

    @Test
    @DisplayName("isFunctionError returns false for MEMORY_ACCESS_VIOLATION")
    void isFunctionErrorReturnsFalseForMemoryAccessViolation() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.MEMORY_ACCESS_VIOLATION, "test");
      assertFalse(ex.isFunctionError(), "MEMORY_ACCESS_VIOLATION should NOT be function error");
    }

    @Test
    @DisplayName("isFunctionError returns false for STACK_ERROR")
    void isFunctionErrorReturnsFalseForStackError() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.STACK_ERROR, "test");
      assertFalse(ex.isFunctionError(), "STACK_ERROR should NOT be function error");
    }

    @Test
    @DisplayName("isFunctionError returns false for TIMEOUT")
    void isFunctionErrorReturnsFalseForTimeout() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.TIMEOUT, "test");
      assertFalse(ex.isFunctionError(), "TIMEOUT should NOT be function error");
    }

    @Test
    @DisplayName("isFunctionError returns false for RESOURCE_EXHAUSTED")
    void isFunctionErrorReturnsFalseForResourceExhausted() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.RESOURCE_EXHAUSTED, "test");
      assertFalse(ex.isFunctionError(), "RESOURCE_EXHAUSTED should NOT be function error");
    }

    @Test
    @DisplayName("isFunctionError returns false for INTERRUPTED")
    void isFunctionErrorReturnsFalseForInterrupted() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.INTERRUPTED, "test");
      assertFalse(ex.isFunctionError(), "INTERRUPTED should NOT be function error");
    }

    @Test
    @DisplayName("isFunctionError returns false for UNKNOWN")
    void isFunctionErrorReturnsFalseForUnknown() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.UNKNOWN, "test");
      assertFalse(ex.isFunctionError(), "UNKNOWN should NOT be function error");
    }

    // -------------------------------------------------------------------------
    // isMemoryError() - Tests for false returns on non-memory types
    // This kills mutations that change "return false" to "return true"
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isMemoryError returns false for TRAP")
    void isMemoryErrorReturnsFalseForTrap() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.TRAP, "test");
      assertFalse(ex.isMemoryError(), "TRAP should NOT be memory error");
    }

    @Test
    @DisplayName("isMemoryError returns false for FUNCTION_EXECUTION_FAILED")
    void isMemoryErrorReturnsFalseForFunctionExecutionFailed() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.FUNCTION_EXECUTION_FAILED, "test");
      assertFalse(ex.isMemoryError(), "FUNCTION_EXECUTION_FAILED should NOT be memory error");
    }

    @Test
    @DisplayName("isMemoryError returns false for HOST_FUNCTION_FAILED")
    void isMemoryErrorReturnsFalseForHostFunctionFailed() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.HOST_FUNCTION_FAILED, "test");
      assertFalse(ex.isMemoryError(), "HOST_FUNCTION_FAILED should NOT be memory error");
    }

    @Test
    @DisplayName("isMemoryError returns false for TIMEOUT")
    void isMemoryErrorReturnsFalseForTimeout() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.TIMEOUT, "test");
      assertFalse(ex.isMemoryError(), "TIMEOUT should NOT be memory error");
    }

    @Test
    @DisplayName("isMemoryError returns false for RESOURCE_EXHAUSTED")
    void isMemoryErrorReturnsFalseForResourceExhausted() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.RESOURCE_EXHAUSTED, "test");
      assertFalse(ex.isMemoryError(), "RESOURCE_EXHAUSTED should NOT be memory error");
    }

    @Test
    @DisplayName("isMemoryError returns false for INTERRUPTED")
    void isMemoryErrorReturnsFalseForInterrupted() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.INTERRUPTED, "test");
      assertFalse(ex.isMemoryError(), "INTERRUPTED should NOT be memory error");
    }

    @Test
    @DisplayName("isMemoryError returns false for UNKNOWN")
    void isMemoryErrorReturnsFalseForUnknown() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.UNKNOWN, "test");
      assertFalse(ex.isMemoryError(), "UNKNOWN should NOT be memory error");
    }

    // -------------------------------------------------------------------------
    // isResourceError() - Tests for false returns on non-resource types
    // This kills mutations that change "return false" to "return true"
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isResourceError returns false for TRAP")
    void isResourceErrorReturnsFalseForTrap() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.TRAP, "test");
      assertFalse(ex.isResourceError(), "TRAP should NOT be resource error");
    }

    @Test
    @DisplayName("isResourceError returns false for FUNCTION_EXECUTION_FAILED")
    void isResourceErrorReturnsFalseForFunctionExecutionFailed() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.FUNCTION_EXECUTION_FAILED, "test");
      assertFalse(ex.isResourceError(), "FUNCTION_EXECUTION_FAILED should NOT be resource error");
    }

    @Test
    @DisplayName("isResourceError returns false for HOST_FUNCTION_FAILED")
    void isResourceErrorReturnsFalseForHostFunctionFailed() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.HOST_FUNCTION_FAILED, "test");
      assertFalse(ex.isResourceError(), "HOST_FUNCTION_FAILED should NOT be resource error");
    }

    @Test
    @DisplayName("isResourceError returns false for MEMORY_ACCESS_VIOLATION")
    void isResourceErrorReturnsFalseForMemoryAccessViolation() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.MEMORY_ACCESS_VIOLATION, "test");
      assertFalse(ex.isResourceError(), "MEMORY_ACCESS_VIOLATION should NOT be resource error");
    }

    @Test
    @DisplayName("isResourceError returns false for STACK_ERROR")
    void isResourceErrorReturnsFalseForStackError() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.STACK_ERROR, "test");
      assertFalse(ex.isResourceError(), "STACK_ERROR should NOT be resource error");
    }

    @Test
    @DisplayName("isResourceError returns false for INTERRUPTED")
    void isResourceErrorReturnsFalseForInterrupted() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.INTERRUPTED, "test");
      assertFalse(ex.isResourceError(), "INTERRUPTED should NOT be resource error");
    }

    @Test
    @DisplayName("isResourceError returns false for UNKNOWN")
    void isResourceErrorReturnsFalseForUnknown() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.UNKNOWN, "test");
      assertFalse(ex.isResourceError(), "UNKNOWN should NOT be resource error");
    }
  }

  @Nested
  @DisplayName("Error Category Count Verification Tests")
  class ErrorCategoryCountVerificationTests {

    @Test
    @DisplayName("Should have exactly 1 trap error type")
    void shouldHaveExactlyOneTrapErrorType() {
      int count = 0;
      for (final RuntimeErrorType type : RuntimeErrorType.values()) {
        final RuntimeException ex = new RuntimeException(type, "test");
        if (ex.isTrapError()) {
          count++;
        }
      }
      assertEquals(1, count, "Should have exactly 1 trap error type (TRAP)");
    }

    @Test
    @DisplayName("Should have exactly 2 function error types")
    void shouldHaveExactlyTwoFunctionErrorTypes() {
      int count = 0;
      for (final RuntimeErrorType type : RuntimeErrorType.values()) {
        final RuntimeException ex = new RuntimeException(type, "test");
        if (ex.isFunctionError()) {
          count++;
        }
      }
      assertEquals(
          2,
          count,
          "Should have exactly 2 function error types "
              + "(FUNCTION_EXECUTION_FAILED, HOST_FUNCTION_FAILED)");
    }

    @Test
    @DisplayName("Should have exactly 2 memory error types")
    void shouldHaveExactlyTwoMemoryErrorTypes() {
      int count = 0;
      for (final RuntimeErrorType type : RuntimeErrorType.values()) {
        final RuntimeException ex = new RuntimeException(type, "test");
        if (ex.isMemoryError()) {
          count++;
        }
      }
      assertEquals(
          2,
          count,
          "Should have exactly 2 memory error types " + "(MEMORY_ACCESS_VIOLATION, STACK_ERROR)");
    }

    @Test
    @DisplayName("Should have exactly 2 resource error types")
    void shouldHaveExactlyTwoResourceErrorTypes() {
      int count = 0;
      for (final RuntimeErrorType type : RuntimeErrorType.values()) {
        final RuntimeException ex = new RuntimeException(type, "test");
        if (ex.isResourceError()) {
          count++;
        }
      }
      assertEquals(
          2, count, "Should have exactly 2 resource error types (RESOURCE_EXHAUSTED, TIMEOUT)");
    }
  }

  @Nested
  @DisplayName("FormatMessage Edge Case Mutation Tests")
  class FormatMessageEdgeCaseMutationTests {

    @Test
    @DisplayName("formatMessage with null functionName should not include function suffix")
    void formatMessageWithNullFunctionName() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.TRAP, "Test message", null, null);
      assertFalse(
          ex.getMessage().contains("(function:"),
          "Message should NOT contain function suffix when functionName is null");
      assertTrue(ex.getMessage().contains("[TRAP]"), "Message should contain error type prefix");
      assertTrue(
          ex.getMessage().contains("Test message"), "Message should contain the message text");
    }

    @Test
    @DisplayName("formatMessage with empty functionName should not include function suffix")
    void formatMessageWithEmptyFunctionName() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.TRAP, "Test message", "", null);
      assertFalse(
          ex.getMessage().contains("(function:"),
          "Message should NOT contain function suffix when functionName is empty");
    }

    @Test
    @DisplayName("formatMessage with valid functionName should include function suffix")
    void formatMessageWithValidFunctionName() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.TRAP, "Test message", "my_func", null);
      assertTrue(
          ex.getMessage().contains("(function: my_func)"),
          "Message should contain function suffix: " + ex.getMessage());
    }

    @Test
    @DisplayName("formatMessage with null errorType should work without prefix")
    void formatMessageWithNullErrorType() {
      // When errorType is null, formatMessage receives null and does NOT add a prefix.
      // The constructor normalizes null to UNKNOWN for the field AFTER calling formatMessage.
      // So the message will NOT have [UNKNOWN] prefix even though getErrorType() returns UNKNOWN.
      final RuntimeException ex = new RuntimeException(null, "Test message", null, null);
      assertTrue(
          ex.getMessage().contains("Test message"),
          "Message should contain the message text even with null errorType");
      // formatMessage does not add prefix when errorType is null
      assertFalse(
          ex.getMessage().contains("["),
          "Message should NOT have bracket prefix when null errorType is passed to formatMessage");
      // But the field itself is normalized to UNKNOWN
      assertEquals(
          RuntimeErrorType.UNKNOWN, ex.getErrorType(), "Error type field should be UNKNOWN");
    }

    @Test
    @DisplayName("formatMessage message is preserved exactly")
    void formatMessagePreservesMessageExactly() {
      final String originalMessage = "This is my specific error message!";
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.TIMEOUT, originalMessage, null, null);
      assertTrue(
          ex.getMessage().contains(originalMessage),
          "Original message should be preserved in formatted message");
    }
  }

  @Nested
  @DisplayName("GenerateRecoverySuggestion Mutation Tests")
  class GenerateRecoverySuggestionMutationTests {

    @Test
    @DisplayName("TRAP suggestion should mention trap and TrapException")
    void trapSuggestionShouldMentionTrapAndTrapException() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.TRAP, "test");
      assertTrue(
          ex.getRecoverySuggestion().contains("trap"),
          "TRAP suggestion should mention 'trap': " + ex.getRecoverySuggestion());
      assertTrue(
          ex.getRecoverySuggestion().contains("TrapException"),
          "TRAP suggestion should mention 'TrapException': " + ex.getRecoverySuggestion());
    }

    @Test
    @DisplayName("FUNCTION_EXECUTION_FAILED suggestion should mention function and parameters")
    void functionExecutionFailedSuggestion() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.FUNCTION_EXECUTION_FAILED, "test");
      assertTrue(
          ex.getRecoverySuggestion().contains("function"),
          "FUNCTION_EXECUTION_FAILED suggestion should mention 'function': "
              + ex.getRecoverySuggestion());
      assertTrue(
          ex.getRecoverySuggestion().contains("parameters"),
          "FUNCTION_EXECUTION_FAILED suggestion should mention 'parameters': "
              + ex.getRecoverySuggestion());
    }

    @Test
    @DisplayName("HOST_FUNCTION_FAILED suggestion should mention host function")
    void hostFunctionFailedSuggestion() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.HOST_FUNCTION_FAILED, "test");
      assertTrue(
          ex.getRecoverySuggestion().contains("host function"),
          "HOST_FUNCTION_FAILED suggestion should mention 'host function': "
              + ex.getRecoverySuggestion());
    }

    @Test
    @DisplayName("MEMORY_ACCESS_VIOLATION suggestion should mention memory and bounds")
    void memoryAccessViolationSuggestion() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.MEMORY_ACCESS_VIOLATION, "test");
      assertTrue(
          ex.getRecoverySuggestion().contains("memory"),
          "MEMORY_ACCESS_VIOLATION suggestion should mention 'memory': "
              + ex.getRecoverySuggestion());
      assertTrue(
          ex.getRecoverySuggestion().contains("bounds"),
          "MEMORY_ACCESS_VIOLATION suggestion should mention 'bounds': "
              + ex.getRecoverySuggestion());
    }

    @Test
    @DisplayName("STACK_ERROR suggestion should mention recursion or stack")
    void stackErrorSuggestion() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.STACK_ERROR, "test");
      assertTrue(
          ex.getRecoverySuggestion().contains("recursion")
              || ex.getRecoverySuggestion().contains("stack"),
          "STACK_ERROR suggestion should mention 'recursion' or 'stack': "
              + ex.getRecoverySuggestion());
    }

    @Test
    @DisplayName("TIMEOUT suggestion should mention timeout")
    void timeoutSuggestion() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.TIMEOUT, "test");
      assertTrue(
          ex.getRecoverySuggestion().contains("timeout"),
          "TIMEOUT suggestion should mention 'timeout': " + ex.getRecoverySuggestion());
    }

    @Test
    @DisplayName("RESOURCE_EXHAUSTED suggestion should mention resource")
    void resourceExhaustedSuggestion() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.RESOURCE_EXHAUSTED, "test");
      assertTrue(
          ex.getRecoverySuggestion().contains("resource"),
          "RESOURCE_EXHAUSTED suggestion should mention 'resource': " + ex.getRecoverySuggestion());
    }

    @Test
    @DisplayName("INTERRUPTED suggestion should mention interruption")
    void interruptedSuggestion() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.INTERRUPTED, "test");
      assertTrue(
          ex.getRecoverySuggestion().contains("interrupt"),
          "INTERRUPTED suggestion should mention 'interrupt': " + ex.getRecoverySuggestion());
    }

    @Test
    @DisplayName("UNKNOWN suggestion should mention execution context")
    void unknownSuggestion() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.UNKNOWN, "test");
      assertTrue(
          ex.getRecoverySuggestion().contains("execution"),
          "UNKNOWN suggestion should mention 'execution': " + ex.getRecoverySuggestion());
    }

    @Test
    @DisplayName("All error types should have distinct recovery suggestions")
    void allErrorTypesShouldHaveDistinctRecoverySuggestions() {
      final java.util.Set<String> suggestions = new java.util.HashSet<>();
      for (final RuntimeErrorType type : RuntimeErrorType.values()) {
        final RuntimeException ex = new RuntimeException(type, "test");
        final String suggestion = ex.getRecoverySuggestion();
        assertFalse(
            suggestions.contains(suggestion),
            "Recovery suggestion for " + type.name() + " should be distinct: " + suggestion);
        suggestions.add(suggestion);
      }
      assertEquals(
          RuntimeErrorType.values().length,
          suggestions.size(),
          "All error types should have distinct suggestions");
    }
  }

  @Nested
  @DisplayName("Getter Return Value Mutation Tests")
  class GetterReturnValueMutationTests {

    @Test
    @DisplayName("getErrorType returns exact error type set in constructor")
    void getErrorTypeReturnsExactValue() {
      for (final RuntimeErrorType type : RuntimeErrorType.values()) {
        final RuntimeException ex = new RuntimeException(type, "test");
        assertSame(
            type,
            ex.getErrorType(),
            "getErrorType should return exact same instance for " + type.name());
      }
    }

    @Test
    @DisplayName("getFunctionName returns exact function name set in constructor")
    void getFunctionNameReturnsExactValue() {
      final String functionName = "unique_function_name_12345";
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.TRAP, "test", functionName, null);
      assertSame(
          functionName,
          ex.getFunctionName(),
          "getFunctionName should return exact same string instance");
    }

    @Test
    @DisplayName("getFunctionName returns null when not provided")
    void getFunctionNameReturnsNullWhenNotProvided() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.TRAP, "test");
      assertNull(ex.getFunctionName(), "getFunctionName should return null when not provided");
    }

    @Test
    @DisplayName("getCause returns exact cause set in constructor")
    void getCauseReturnsExactValue() {
      final Throwable cause = new java.lang.RuntimeException("original cause");
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.TRAP, "test", "func", cause);
      assertSame(cause, ex.getCause(), "getCause should return exact same Throwable instance");
    }

    @Test
    @DisplayName("getCause returns null when not provided")
    void getCauseReturnsNullWhenNotProvided() {
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.TRAP, "test", "func", null);
      assertNull(ex.getCause(), "getCause should return null when not provided");
    }
  }

  @Nested
  @DisplayName("RuntimeErrorType Description Mutation Tests")
  class RuntimeErrorTypeDescriptionMutationTests {

    @Test
    @DisplayName("TRAP description should mention trap")
    void trapDescriptionShouldMentionTrap() {
      assertTrue(
          RuntimeErrorType.TRAP.getDescription().toLowerCase().contains("trap"),
          "TRAP description should mention 'trap': " + RuntimeErrorType.TRAP.getDescription());
    }

    @Test
    @DisplayName("FUNCTION_EXECUTION_FAILED description should mention function")
    void functionExecutionFailedDescription() {
      assertTrue(
          RuntimeErrorType.FUNCTION_EXECUTION_FAILED
              .getDescription()
              .toLowerCase()
              .contains("function"),
          "FUNCTION_EXECUTION_FAILED description should mention 'function': "
              + RuntimeErrorType.FUNCTION_EXECUTION_FAILED.getDescription());
    }

    @Test
    @DisplayName("HOST_FUNCTION_FAILED description should mention host or function")
    void hostFunctionFailedDescription() {
      final String desc = RuntimeErrorType.HOST_FUNCTION_FAILED.getDescription().toLowerCase();
      assertTrue(
          desc.contains("host") || desc.contains("function"),
          "HOST_FUNCTION_FAILED description should mention 'host' or 'function': "
              + RuntimeErrorType.HOST_FUNCTION_FAILED.getDescription());
    }

    @Test
    @DisplayName("MEMORY_ACCESS_VIOLATION description should mention memory")
    void memoryAccessViolationDescription() {
      assertTrue(
          RuntimeErrorType.MEMORY_ACCESS_VIOLATION
              .getDescription()
              .toLowerCase()
              .contains("memory"),
          "MEMORY_ACCESS_VIOLATION description should mention 'memory': "
              + RuntimeErrorType.MEMORY_ACCESS_VIOLATION.getDescription());
    }

    @Test
    @DisplayName("STACK_ERROR description should mention stack")
    void stackErrorDescription() {
      assertTrue(
          RuntimeErrorType.STACK_ERROR.getDescription().toLowerCase().contains("stack"),
          "STACK_ERROR description should mention 'stack': "
              + RuntimeErrorType.STACK_ERROR.getDescription());
    }

    @Test
    @DisplayName("TIMEOUT description should mention timeout")
    void timeoutDescription() {
      assertTrue(
          RuntimeErrorType.TIMEOUT.getDescription().toLowerCase().contains("timeout"),
          "TIMEOUT description should mention 'timeout': "
              + RuntimeErrorType.TIMEOUT.getDescription());
    }

    @Test
    @DisplayName("RESOURCE_EXHAUSTED description should mention resource")
    void resourceExhaustedDescription() {
      assertTrue(
          RuntimeErrorType.RESOURCE_EXHAUSTED.getDescription().toLowerCase().contains("resource"),
          "RESOURCE_EXHAUSTED description should mention 'resource': "
              + RuntimeErrorType.RESOURCE_EXHAUSTED.getDescription());
    }

    @Test
    @DisplayName("INTERRUPTED description should mention interrupt")
    void interruptedDescription() {
      assertTrue(
          RuntimeErrorType.INTERRUPTED.getDescription().toLowerCase().contains("interrupt"),
          "INTERRUPTED description should mention 'interrupt': "
              + RuntimeErrorType.INTERRUPTED.getDescription());
    }

    @Test
    @DisplayName("UNKNOWN description should mention unknown or error")
    void unknownDescription() {
      final String desc = RuntimeErrorType.UNKNOWN.getDescription().toLowerCase();
      assertTrue(
          desc.contains("unknown") || desc.contains("error"),
          "UNKNOWN description should mention 'unknown' or 'error': "
              + RuntimeErrorType.UNKNOWN.getDescription());
    }

    @Test
    @DisplayName("All descriptions should be distinct")
    void allDescriptionsShouldBeDistinct() {
      final java.util.Set<String> descriptions = new java.util.HashSet<>();
      for (final RuntimeErrorType type : RuntimeErrorType.values()) {
        final String desc = type.getDescription();
        assertFalse(
            descriptions.contains(desc),
            "Description for " + type.name() + " should be distinct: " + desc);
        descriptions.add(desc);
      }
      assertEquals(
          RuntimeErrorType.values().length,
          descriptions.size(),
          "All error types should have distinct descriptions");
    }
  }

  @Nested
  @DisplayName("Message Formatting Mutation Tests")
  class MessageFormattingMutationTests {

    @Test
    @DisplayName("Message should contain error type in brackets")
    void messageShouldContainErrorTypeInBrackets() {
      for (final RuntimeErrorType type : RuntimeErrorType.values()) {
        final RuntimeException ex = new RuntimeException(type, "test message");
        assertTrue(
            ex.getMessage().contains("[" + type.name() + "]"),
            "Message should contain [" + type.name() + "]: " + ex.getMessage());
      }
    }

    @Test
    @DisplayName("Message with function name should include parenthesized function")
    void messageShouldIncludeParenthesizedFunction() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.TRAP, "error", "testFunc", null);
      assertTrue(
          ex.getMessage().contains("(function: testFunc)"),
          "Message should include '(function: testFunc)': " + ex.getMessage());
    }

    @Test
    @DisplayName("Message format is [TYPE] message (function: name)")
    void messageFormatIsCorrect() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.TIMEOUT, "time exceeded", "slowFunc", null);
      final String expected = "[TIMEOUT] time exceeded (function: slowFunc)";
      assertEquals(expected, ex.getMessage(), "Message format should match expected format");
    }

    @Test
    @DisplayName("Message format without function is [TYPE] message")
    void messageFormatWithoutFunctionIsCorrect() {
      final RuntimeException ex =
          new RuntimeException(RuntimeErrorType.TRAP, "trap occurred", null, null);
      final String expected = "[TRAP] trap occurred";
      assertEquals(
          expected, ex.getMessage(), "Message format without function should match expected");
    }
  }

  @Nested
  @DisplayName("Null Error Type Handling Mutation Tests")
  class NullErrorTypeHandlingMutationTests {

    @Test
    @DisplayName("Null error type is normalized to UNKNOWN in constructor")
    void nullErrorTypeIsNormalizedToUnknown() {
      final RuntimeException ex = new RuntimeException(null, "test", null, null);
      assertEquals(
          RuntimeErrorType.UNKNOWN,
          ex.getErrorType(),
          "Null error type should be normalized to UNKNOWN");
    }

    @Test
    @DisplayName("Null error type results in UNKNOWN recovery suggestion")
    void nullErrorTypeResultsInUnknownRecoverySuggestion() {
      final RuntimeException ex = new RuntimeException(null, "test", null, null);
      final RuntimeException unknownEx =
          new RuntimeException(RuntimeErrorType.UNKNOWN, "test", null, null);
      assertEquals(
          unknownEx.getRecoverySuggestion(),
          ex.getRecoverySuggestion(),
          "Null error type should have same recovery suggestion as UNKNOWN");
    }
  }

  @Nested
  @DisplayName("Message Validation Mutation Tests")
  class MessageValidationMutationTests {

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for null message")
    void constructorShouldThrowForNullMessage() {
      final IllegalArgumentException ex =
          org.junit.jupiter.api.Assertions.assertThrows(
              IllegalArgumentException.class,
              () -> new RuntimeException(RuntimeErrorType.TRAP, null, null, null),
              "Should throw IllegalArgumentException for null message");
      assertTrue(
          ex.getMessage().contains("null") || ex.getMessage().contains("empty"),
          "Exception message should mention null or empty: " + ex.getMessage());
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for empty message")
    void constructorShouldThrowForEmptyMessage() {
      final IllegalArgumentException ex =
          org.junit.jupiter.api.Assertions.assertThrows(
              IllegalArgumentException.class,
              () -> new RuntimeException(RuntimeErrorType.TRAP, "", null, null),
              "Should throw IllegalArgumentException for empty message");
      assertTrue(
          ex.getMessage().contains("null") || ex.getMessage().contains("empty"),
          "Exception message should mention null or empty: " + ex.getMessage());
    }

    @Test
    @DisplayName("Constructor with message-only should throw for null")
    void constructorWithMessageOnlyShouldThrowForNull() {
      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalArgumentException.class,
          () -> new RuntimeException((String) null),
          "Should throw IllegalArgumentException for null message");
    }

    @Test
    @DisplayName("Constructor with message-only should throw for empty")
    void constructorWithMessageOnlyShouldThrowForEmpty() {
      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalArgumentException.class,
          () -> new RuntimeException(""),
          "Should throw IllegalArgumentException for empty message");
    }

    @Test
    @DisplayName("Constructor with message and cause should throw for null message")
    void constructorWithMessageAndCauseShouldThrowForNullMessage() {
      final Throwable cause = new java.lang.RuntimeException("cause");
      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalArgumentException.class,
          () -> new RuntimeException((String) null, cause),
          "Should throw IllegalArgumentException for null message");
    }

    @Test
    @DisplayName("Constructor with error type and message should throw for null message")
    void constructorWithErrorTypeAndMessageShouldThrowForNullMessage() {
      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalArgumentException.class,
          () -> new RuntimeException(RuntimeErrorType.TRAP, null),
          "Should throw IllegalArgumentException for null message");
    }

    @Test
    @DisplayName("Constructor with error type and message should throw for empty message")
    void constructorWithErrorTypeAndMessageShouldThrowForEmptyMessage() {
      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalArgumentException.class,
          () -> new RuntimeException(RuntimeErrorType.TRAP, ""),
          "Should throw IllegalArgumentException for empty message");
    }

    @Test
    @DisplayName("Valid non-empty message should not throw")
    void validNonEmptyMessageShouldNotThrow() {
      // This should not throw
      final RuntimeException ex = new RuntimeException(RuntimeErrorType.TRAP, "Valid message");
      assertNotNull(ex.getMessage(), "Should have a message");
    }
  }
}

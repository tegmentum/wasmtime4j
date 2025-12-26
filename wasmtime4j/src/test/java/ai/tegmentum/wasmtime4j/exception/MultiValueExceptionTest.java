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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link MultiValueException} class.
 *
 * <p>This test class verifies the construction and behavior of multi-value WebAssembly exceptions,
 * including count tracking and factory methods.
 */
@DisplayName("MultiValueException Tests")
class MultiValueExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("MultiValueException should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(MultiValueException.class),
          "MultiValueException should extend WasmException");
    }

    @Test
    @DisplayName("MultiValueException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(MultiValueException.class),
          "MultiValueException should be serializable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message only should set message and default counts")
    void constructorWithMessageOnly() {
      final MultiValueException exception = new MultiValueException("Test error");

      assertEquals("Test error", exception.getMessage(), "Message should be set");
      assertEquals(-1, exception.getExpectedCount(), "Expected count should default to -1");
      assertEquals(-1, exception.getActualCount(), "Actual count should default to -1");
      assertNull(exception.getOperation(), "Operation should be null");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor with message and cause should set both")
    void constructorWithMessageAndCause() {
      final Throwable cause = new RuntimeException("Root cause");
      final MultiValueException exception = new MultiValueException("Test error", cause);

      assertEquals("Test error", exception.getMessage(), "Message should be set");
      assertSame(cause, exception.getCause(), "Cause should be set");
      assertEquals(-1, exception.getExpectedCount(), "Expected count should default to -1");
      assertEquals(-1, exception.getActualCount(), "Actual count should default to -1");
    }

    @Test
    @DisplayName("Constructor with counts should set expected and actual counts")
    void constructorWithCounts() {
      final MultiValueException exception = new MultiValueException("Count mismatch", 3, 5);

      assertEquals("Count mismatch", exception.getMessage(), "Message should be set");
      assertEquals(3, exception.getExpectedCount(), "Expected count should be 3");
      assertEquals(5, exception.getActualCount(), "Actual count should be 5");
      assertNull(exception.getOperation(), "Operation should be null");
    }

    @Test
    @DisplayName("Constructor with full details should set all fields")
    void constructorWithFullDetails() {
      final MultiValueException exception =
          new MultiValueException("Full details", 2, 4, "multiply");

      assertEquals("Full details", exception.getMessage(), "Message should be set");
      assertEquals(2, exception.getExpectedCount(), "Expected count should be 2");
      assertEquals(4, exception.getActualCount(), "Actual count should be 4");
      assertEquals("multiply", exception.getOperation(), "Operation should be 'multiply'");
    }

    @Test
    @DisplayName("Constructor with full details and cause should set all fields")
    void constructorWithFullDetailsAndCause() {
      final Throwable cause = new RuntimeException("Root cause");
      final MultiValueException exception =
          new MultiValueException("Full details", 1, 3, "add", cause);

      assertEquals("Full details", exception.getMessage(), "Message should be set");
      assertEquals(1, exception.getExpectedCount(), "Expected count should be 1");
      assertEquals(3, exception.getActualCount(), "Actual count should be 3");
      assertEquals("add", exception.getOperation(), "Operation should be 'add'");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getExpectedCount should return expected count")
    void getExpectedCountShouldReturnExpectedCount() {
      final MultiValueException exception = new MultiValueException("Test", 5, 3);

      assertEquals(5, exception.getExpectedCount(), "getExpectedCount should return 5");
    }

    @Test
    @DisplayName("getActualCount should return actual count")
    void getActualCountShouldReturnActualCount() {
      final MultiValueException exception = new MultiValueException("Test", 5, 3);

      assertEquals(3, exception.getActualCount(), "getActualCount should return 3");
    }

    @Test
    @DisplayName("getOperation should return operation name")
    void getOperationShouldReturnOperationName() {
      final MultiValueException exception = new MultiValueException("Test", 2, 4, "function_call");

      assertEquals(
          "function_call", exception.getOperation(), "getOperation should return 'function_call'");
    }
  }

  @Nested
  @DisplayName("Info Check Method Tests")
  class InfoCheckMethodTests {

    @Test
    @DisplayName("hasCountInfo should return true when counts are valid")
    void hasCountInfoShouldReturnTrueWhenCountsValid() {
      final MultiValueException exception = new MultiValueException("Test", 0, 0);

      assertTrue(exception.hasCountInfo(), "hasCountInfo should return true when counts are >= 0");
    }

    @Test
    @DisplayName("hasCountInfo should return false when counts are default")
    void hasCountInfoShouldReturnFalseWhenCountsDefault() {
      final MultiValueException exception = new MultiValueException("Test");

      assertFalse(exception.hasCountInfo(), "hasCountInfo should return false when counts are -1");
    }

    @Test
    @DisplayName("hasCountInfo should return false when expected is negative")
    void hasCountInfoShouldReturnFalseWhenExpectedNegative() {
      final MultiValueException exception = new MultiValueException("Test", -1, 5);

      assertFalse(
          exception.hasCountInfo(), "hasCountInfo should return false when expected is negative");
    }

    @Test
    @DisplayName("hasOperationInfo should return true when operation is set")
    void hasOperationInfoShouldReturnTrueWhenOperationSet() {
      final MultiValueException exception = new MultiValueException("Test", 1, 2, "op");

      assertTrue(
          exception.hasOperationInfo(),
          "hasOperationInfo should return true when operation is set");
    }

    @Test
    @DisplayName("hasOperationInfo should return false when operation is null")
    void hasOperationInfoShouldReturnFalseWhenOperationNull() {
      final MultiValueException exception = new MultiValueException("Test", 1, 2);

      assertFalse(
          exception.hasOperationInfo(),
          "hasOperationInfo should return false when operation is null");
    }

    @Test
    @DisplayName("hasOperationInfo should return false when operation is empty")
    void hasOperationInfoShouldReturnFalseWhenOperationEmpty() {
      final MultiValueException exception = new MultiValueException("Test", 1, 2, "");

      assertFalse(
          exception.hasOperationInfo(),
          "hasOperationInfo should return false when operation is empty");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include class name and message")
    void toStringShouldIncludeClassNameAndMessage() {
      final MultiValueException exception = new MultiValueException("Test error");
      final String result = exception.toString();

      assertTrue(result.contains("MultiValueException"), "toString should include class name");
      assertTrue(result.contains("Test error"), "toString should include message");
    }

    @Test
    @DisplayName("toString should include count info when present")
    void toStringShouldIncludeCountInfoWhenPresent() {
      final MultiValueException exception = new MultiValueException("Test", 3, 5);
      final String result = exception.toString();

      assertTrue(result.contains("expected: 3"), "toString should include expected count");
      assertTrue(result.contains("actual: 5"), "toString should include actual count");
    }

    @Test
    @DisplayName("toString should include operation info when present")
    void toStringShouldIncludeOperationInfoWhenPresent() {
      final MultiValueException exception = new MultiValueException("Test", 3, 5, "myFunc");
      final String result = exception.toString();

      assertTrue(result.contains("operation: myFunc"), "toString should include operation");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("countMismatch should create exception with counts")
    void countMismatchShouldCreateExceptionWithCounts() {
      final MultiValueException exception = MultiValueException.countMismatch(3, 5);

      assertNotNull(exception, "Exception should not be null");
      assertEquals(3, exception.getExpectedCount(), "Expected count should be 3");
      assertEquals(5, exception.getActualCount(), "Actual count should be 5");
      assertTrue(exception.getMessage().contains("3"), "Message should include expected count");
      assertTrue(exception.getMessage().contains("5"), "Message should include actual count");
    }

    @Test
    @DisplayName("countMismatch with operation should include operation")
    void countMismatchWithOperationShouldIncludeOperation() {
      final MultiValueException exception = MultiValueException.countMismatch(2, 4, "divide");

      assertNotNull(exception, "Exception should not be null");
      assertEquals(2, exception.getExpectedCount(), "Expected count should be 2");
      assertEquals(4, exception.getActualCount(), "Actual count should be 4");
      assertEquals("divide", exception.getOperation(), "Operation should be 'divide'");
      assertTrue(exception.getMessage().contains("divide"), "Message should include operation");
    }

    @Test
    @DisplayName("typeValidationError should create exception with type info")
    void typeValidationErrorShouldCreateExceptionWithTypeInfo() {
      final MultiValueException exception =
          MultiValueException.typeValidationError(2, "i32", "f64");

      assertNotNull(exception, "Exception should not be null");
      assertTrue(exception.getMessage().contains("index 2"), "Message should include index");
      assertTrue(exception.getMessage().contains("i32"), "Message should include expected type");
      assertTrue(exception.getMessage().contains("f64"), "Message should include actual type");
    }

    @Test
    @DisplayName("marshalingError should create exception with cause")
    void marshalingErrorShouldCreateExceptionWithCause() {
      final Throwable cause = new RuntimeException("Marshaling failed");
      final MultiValueException exception =
          MultiValueException.marshalingError("serialize", 5, cause);

      assertNotNull(exception, "Exception should not be null");
      assertEquals("serialize", exception.getOperation(), "Operation should be 'serialize'");
      assertEquals(5, exception.getActualCount(), "Value count should be 5");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("limitExceeded should create exception with limit info")
    void limitExceededShouldCreateExceptionWithLimitInfo() {
      final MultiValueException exception = MultiValueException.limitExceeded(100, 50);

      assertNotNull(exception, "Exception should not be null");
      assertEquals(50, exception.getExpectedCount(), "Max allowed should be 50");
      assertEquals(100, exception.getActualCount(), "Actual count should be 100");
      assertTrue(exception.getMessage().contains("limit"), "Message should mention limit");
    }

    @Test
    @DisplayName("invalidValueArray should create exception for operation")
    void invalidValueArrayShouldCreateExceptionForOperation() {
      final MultiValueException exception = MultiValueException.invalidValueArray("processValues");

      assertNotNull(exception, "Exception should not be null");
      assertEquals(
          "processValues", exception.getOperation(), "Operation should be 'processValues'");
      assertTrue(
          exception.getMessage().contains("Invalid value array"),
          "Message should indicate invalid array");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle zero counts")
    void shouldHandleZeroCounts() {
      final MultiValueException exception = new MultiValueException("No values", 0, 0);

      assertEquals(0, exception.getExpectedCount(), "Expected count should be 0");
      assertEquals(0, exception.getActualCount(), "Actual count should be 0");
      assertTrue(exception.hasCountInfo(), "hasCountInfo should return true for zero counts");
    }

    @Test
    @DisplayName("Should handle large counts")
    void shouldHandleLargeCounts() {
      final MultiValueException exception =
          new MultiValueException("Large values", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);

      assertEquals(
          Integer.MAX_VALUE, exception.getExpectedCount(), "Should handle max int for expected");
      assertEquals(
          Integer.MAX_VALUE - 1, exception.getActualCount(), "Should handle large actual count");
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
      final MultiValueException exception = new MultiValueException(null);

      assertNull(exception.getMessage(), "Message should be null");
      assertNotNull(exception.toString(), "toString should not return null");
    }
  }
}

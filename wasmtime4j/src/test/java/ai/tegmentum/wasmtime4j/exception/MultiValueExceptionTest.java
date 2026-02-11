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

  @Nested
  @DisplayName("hasCountInfo Conditional Mutation Tests")
  class HasCountInfoConditionalMutationTests {

    @Test
    @DisplayName("hasCountInfo should return false when expected is -1 and actual is positive")
    void hasCountInfoShouldReturnFalseWhenExpectedNegativeAndActualPositive() {
      // This kills line 137 mutation: replaced comparison check with true
      // If expectedCount >= 0 becomes true, then hasCountInfo() would return true
      // when expectedCount is -1 but actualCount is >= 0
      final MultiValueException exception = new MultiValueException("Test", -1, 5);

      assertEquals(-1, exception.getExpectedCount(), "Expected count should be -1");
      assertEquals(5, exception.getActualCount(), "Actual count should be 5");

      // The first condition (expectedCount >= 0) must be evaluated
      // If mutated to true, this would incorrectly return true
      assertFalse(
          exception.hasCountInfo(),
          "hasCountInfo should return false when expectedCount is -1, even if actualCount >= 0");
    }

    @Test
    @DisplayName("hasCountInfo should return false when expected is positive and actual is -1")
    void hasCountInfoShouldReturnFalseWhenExpectedPositiveAndActualNegative() {
      // Test the symmetric case for the second condition
      final MultiValueException exception = new MultiValueException("Test", 5, -1);

      assertEquals(5, exception.getExpectedCount(), "Expected count should be 5");
      assertEquals(-1, exception.getActualCount(), "Actual count should be -1");

      assertFalse(
          exception.hasCountInfo(),
          "hasCountInfo should return false when actualCount is -1, even if expectedCount >= 0");
    }

    @Test
    @DisplayName("hasCountInfo should require BOTH conditions to be true")
    void hasCountInfoShouldRequireBothConditionsToBeTrue() {
      // Both conditions satisfied
      final MultiValueException bothValid = new MultiValueException("Test", 0, 0);
      assertTrue(bothValid.hasCountInfo(), "Should return true when both counts are >= 0");

      // First condition fails
      final MultiValueException firstFails = new MultiValueException("Test", -1, 0);
      assertFalse(firstFails.hasCountInfo(), "Should return false when expectedCount < 0");

      // Second condition fails
      final MultiValueException secondFails = new MultiValueException("Test", 0, -1);
      assertFalse(secondFails.hasCountInfo(), "Should return false when actualCount < 0");

      // Both conditions fail
      final MultiValueException bothFail = new MultiValueException("Test");
      assertFalse(bothFail.hasCountInfo(), "Should return false when both counts are -1");
    }
  }

  @Nested
  @DisplayName("Factory Method Field Assignment Mutation Tests")
  class FactoryMethodFieldAssignmentMutationTests {

    @Test
    @DisplayName("invalidValueArray should set expectedCount to exactly -1")
    void invalidValueArrayShouldSetExpectedCountToExactlyMinus1() {
      // This kills line 260 mutation: Substituted -1 with 0 for expectedCount
      final MultiValueException exception = MultiValueException.invalidValueArray("testOp");

      assertEquals(
          -1,
          exception.getExpectedCount(),
          "invalidValueArray should set expectedCount to -1, not 0");
      assertFalse(
          exception.hasCountInfo(), "hasCountInfo should be false since expectedCount is -1");
    }

    @Test
    @DisplayName("invalidValueArray should set actualCount to exactly -1")
    void invalidValueArrayShouldSetActualCountToExactlyMinus1() {
      // This kills line 260 mutation: Substituted -1 with 0 for actualCount
      final MultiValueException exception = MultiValueException.invalidValueArray("testOp");

      assertEquals(
          -1, exception.getActualCount(), "invalidValueArray should set actualCount to -1, not 0");
      assertFalse(exception.hasCountInfo(), "hasCountInfo should be false since actualCount is -1");
    }

    @Test
    @DisplayName("marshalingError should set expectedCount to exactly -1")
    void marshalingErrorShouldSetExpectedCountToExactlyMinus1() {
      // This kills line 230 mutation: Substituted -1 with 0
      final Throwable cause = new RuntimeException("test");
      final MultiValueException exception =
          MultiValueException.marshalingError("marshal", 10, cause);

      assertEquals(
          -1,
          exception.getExpectedCount(),
          "marshalingError should set expectedCount to -1, not 0");
    }

    @Test
    @DisplayName("marshalingError should set actualCount to the valueCount parameter")
    void marshalingErrorShouldSetActualCountToValueCountParameter() {
      // This verifies the correct parameter mapping
      final Throwable cause = new RuntimeException("test");
      final MultiValueException exception =
          MultiValueException.marshalingError("marshal", 42, cause);

      assertEquals(
          42,
          exception.getActualCount(),
          "marshalingError should set actualCount to the valueCount parameter (42)");
      assertEquals(
          "marshal", exception.getOperation(), "marshalingError should set operation correctly");
    }

    @Test
    @DisplayName(
        "limitExceeded should map parameters correctly: expected=maxAllowed, actual=actualCount")
    void limitExceededShouldMapParametersCorrectly() {
      // This kills mutations that swap the constructor argument indices
      // limitExceeded(actualCount=100, maxAllowed=50) should create:
      // expectedCount=50 (maxAllowed), actualCount=100 (actualCount param)
      final MultiValueException exception = MultiValueException.limitExceeded(100, 50);

      assertEquals(
          50,
          exception.getExpectedCount(),
          "limitExceeded should set expectedCount to maxAllowed (50), not actualCount");
      assertEquals(
          100,
          exception.getActualCount(),
          "limitExceeded should set actualCount to actualCount param (100), not maxAllowed");
    }

    @Test
    @DisplayName("limitExceeded with distinct values verifies exact field mapping")
    void limitExceededWithDistinctValuesVerifiesExactFieldMapping() {
      // Use very distinct values to ensure correct mapping
      final MultiValueException exception = MultiValueException.limitExceeded(999, 111);

      // If arguments were swapped, expected would be 999 and actual would be 111
      assertEquals(111, exception.getExpectedCount(), "expectedCount should be maxAllowed (111)");
      assertEquals(
          999, exception.getActualCount(), "actualCount should be actualCount param (999)");
    }
  }

  @Nested
  @DisplayName("Factory Method Format String Mutation Tests")
  class FactoryMethodFormatStringMutationTests {

    @Test
    @DisplayName("countMismatch format should have expected before actual in message")
    void countMismatchFormatShouldHaveExpectedBeforeActual() {
      // Format: "Value count mismatch: expected %d, got %d"
      final MultiValueException exception = MultiValueException.countMismatch(111, 999);

      final String message = exception.getMessage();
      final int expectedIndex = message.indexOf("111");
      final int actualIndex = message.indexOf("999");

      assertTrue(expectedIndex >= 0, "Message should contain expected value 111");
      assertTrue(actualIndex >= 0, "Message should contain actual value 999");
      assertTrue(
          expectedIndex < actualIndex,
          "Expected value should appear before actual value in message: " + message);
    }

    @Test
    @DisplayName("countMismatch with operation format should have operation first")
    void countMismatchWithOperationFormatShouldHaveOperationFirst() {
      // Format: "Value count mismatch in %s: expected %d, got %d"
      final MultiValueException exception =
          MultiValueException.countMismatch(111, 999, "testOperation");

      final String message = exception.getMessage();
      final int operationIndex = message.indexOf("testOperation");
      final int expectedIndex = message.indexOf("111");
      final int actualIndex = message.indexOf("999");

      assertTrue(operationIndex >= 0, "Message should contain operation");
      assertTrue(
          operationIndex < expectedIndex,
          "Operation should appear before expected count: " + message);
      assertTrue(expectedIndex < actualIndex, "Expected should appear before actual: " + message);
    }

    @Test
    @DisplayName(
        "typeValidationError format should have index, expected type, actual type in order")
    void typeValidationErrorFormatShouldHaveCorrectOrder() {
      // Format: "Type validation failed at index %d: expected %s, got %s"
      final MultiValueException exception =
          MultiValueException.typeValidationError(42, "expectedType", "actualType");

      final String message = exception.getMessage();
      final int indexPos = message.indexOf("42");
      final int expectedTypePos = message.indexOf("expectedType");
      final int actualTypePos = message.indexOf("actualType");

      assertTrue(indexPos >= 0, "Message should contain index");
      assertTrue(expectedTypePos >= 0, "Message should contain expected type");
      assertTrue(actualTypePos >= 0, "Message should contain actual type");
      assertTrue(
          indexPos < expectedTypePos, "Index should appear before expected type: " + message);
      assertTrue(
          expectedTypePos < actualTypePos,
          "Expected type should appear before actual type: " + message);
    }

    @Test
    @DisplayName("marshalingError format should have operation before valueCount")
    void marshalingErrorFormatShouldHaveOperationBeforeValueCount() {
      // Format: "Marshaling failed for %s with %d values"
      final Throwable cause = new RuntimeException("test");
      final MultiValueException exception =
          MultiValueException.marshalingError("serializeOp", 77, cause);

      final String message = exception.getMessage();
      final int operationIndex = message.indexOf("serializeOp");
      final int countIndex = message.indexOf("77");

      assertTrue(operationIndex >= 0, "Message should contain operation");
      assertTrue(countIndex >= 0, "Message should contain value count");
      assertTrue(
          operationIndex < countIndex, "Operation should appear before value count: " + message);
    }

    @Test
    @DisplayName("limitExceeded format should have actualCount before maxAllowed in message")
    void limitExceededFormatShouldHaveActualCountBeforeMaxAllowed() {
      // Format: "Multi-value limit exceeded: %d values (max allowed: %d)"
      final MultiValueException exception = MultiValueException.limitExceeded(888, 222);

      final String message = exception.getMessage();
      final int actualIndex = message.indexOf("888");
      final int maxIndex = message.indexOf("222");

      assertTrue(actualIndex >= 0, "Message should contain actual count 888");
      assertTrue(maxIndex >= 0, "Message should contain max allowed 222");
      assertTrue(
          actualIndex < maxIndex,
          "Actual count should appear before max allowed in message: " + message);
    }
  }

  @Nested
  @DisplayName("marshalingError Field Mapping Mutation Tests")
  class MarshalingErrorFieldMappingMutationTests {

    @Test
    @DisplayName("marshalingError expectedCount should be -1 not valueCount")
    void marshalingErrorExpectedCountShouldBeMinus1NotValueCount() {
      // This catches if the constructor args are swapped
      final Throwable cause = new RuntimeException("test");
      final MultiValueException exception = MultiValueException.marshalingError("op", 123, cause);

      // If mutation swapped args, expectedCount would be 123 instead of -1
      assertEquals(
          -1, exception.getExpectedCount(), "expectedCount should be -1, not the valueCount (123)");
      assertEquals(123, exception.getActualCount(), "actualCount should be valueCount (123)");
    }

    @Test
    @DisplayName("marshalingError hasCountInfo should return false due to -1 expected")
    void marshalingErrorHasCountInfoShouldReturnFalseDueToMinus1Expected() {
      final Throwable cause = new RuntimeException("test");
      final MultiValueException exception = MultiValueException.marshalingError("op", 50, cause);

      // expectedCount is -1, so hasCountInfo should be false
      // If -1 was mutated to 0, hasCountInfo would be true
      assertFalse(
          exception.hasCountInfo(), "hasCountInfo should be false because expectedCount is -1");
    }
  }
}

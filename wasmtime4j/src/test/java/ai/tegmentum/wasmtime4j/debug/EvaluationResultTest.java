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

package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.EvaluationResult.EvaluationStatus;
import ai.tegmentum.wasmtime4j.debug.VariableValue.ValueType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EvaluationResult} interface.
 *
 * <p>EvaluationResult provides the result of expression evaluation during WebAssembly debugging
 * including status, value, error message, and duration information.
 */
@DisplayName("EvaluationResult Tests")
class EvaluationResultTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(EvaluationResult.class.isInterface(), "EvaluationResult should be an interface");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      final Method method = EvaluationResult.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(
          EvaluationStatus.class, method.getReturnType(), "Should return EvaluationStatus");
    }

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      final Method method = EvaluationResult.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(VariableValue.class, method.getReturnType(), "Should return VariableValue");
    }

    @Test
    @DisplayName("should have getErrorMessage method")
    void shouldHaveGetErrorMessageMethod() throws NoSuchMethodException {
      final Method method = EvaluationResult.class.getMethod("getErrorMessage");
      assertNotNull(method, "getErrorMessage method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      final Method method = EvaluationResult.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getExpression method")
    void shouldHaveGetExpressionMethod() throws NoSuchMethodException {
      final Method method = EvaluationResult.class.getMethod("getExpression");
      assertNotNull(method, "getExpression method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have exactly five methods")
    void shouldHaveExactlyFiveMethods() {
      final Method[] methods = EvaluationResult.class.getDeclaredMethods();
      assertEquals(5, methods.length, "EvaluationResult should have exactly 5 methods");
    }
  }

  @Nested
  @DisplayName("EvaluationStatus Enum Tests")
  class EvaluationStatusEnumTests {

    @Test
    @DisplayName("should have SUCCESS value")
    void shouldHaveSuccessValue() {
      assertNotNull(EvaluationStatus.valueOf("SUCCESS"), "SUCCESS should exist");
    }

    @Test
    @DisplayName("should have FAILED value")
    void shouldHaveFailedValue() {
      assertNotNull(EvaluationStatus.valueOf("FAILED"), "FAILED should exist");
    }

    @Test
    @DisplayName("should have TIMEOUT value")
    void shouldHaveTimeoutValue() {
      assertNotNull(EvaluationStatus.valueOf("TIMEOUT"), "TIMEOUT should exist");
    }

    @Test
    @DisplayName("should have INVALID_EXPRESSION value")
    void shouldHaveInvalidExpressionValue() {
      assertNotNull(
          EvaluationStatus.valueOf("INVALID_EXPRESSION"), "INVALID_EXPRESSION should exist");
    }

    @Test
    @DisplayName("should have exactly four values")
    void shouldHaveExactlyFourValues() {
      assertEquals(
          4, EvaluationStatus.values().length, "EvaluationStatus should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock should return successful evaluation result")
    void mockShouldReturnSuccessfulEvaluationResult() {
      final MockVariableValue value = new MockVariableValue("42", ValueType.INTEGER);
      final MockEvaluationResult result =
          new MockEvaluationResult(EvaluationStatus.SUCCESS, value, null, 10L, "x + 1");

      assertEquals(EvaluationStatus.SUCCESS, result.getStatus(), "Status should be SUCCESS");
      assertNotNull(result.getValue(), "Value should not be null");
      assertNull(result.getErrorMessage(), "Error message should be null on success");
      assertEquals(10L, result.getDuration(), "Duration should match");
      assertEquals("x + 1", result.getExpression(), "Expression should match");
    }

    @Test
    @DisplayName("mock should return failed evaluation result")
    void mockShouldReturnFailedEvaluationResult() {
      final MockEvaluationResult result =
          new MockEvaluationResult(
              EvaluationStatus.FAILED, null, "Variable not found", 5L, "unknownVar");

      assertEquals(EvaluationStatus.FAILED, result.getStatus(), "Status should be FAILED");
      assertNull(result.getValue(), "Value should be null on failure");
      assertEquals("Variable not found", result.getErrorMessage(), "Error message should match");
    }

    @Test
    @DisplayName("mock should return timeout evaluation result")
    void mockShouldReturnTimeoutEvaluationResult() {
      final MockEvaluationResult result =
          new MockEvaluationResult(
              EvaluationStatus.TIMEOUT,
              null,
              "Evaluation timed out after 5000ms",
              5000L,
              "infiniteLoop()");

      assertEquals(EvaluationStatus.TIMEOUT, result.getStatus(), "Status should be TIMEOUT");
      assertEquals(5000L, result.getDuration(), "Duration should be timeout duration");
    }

    @Test
    @DisplayName("mock should return invalid expression result")
    void mockShouldReturnInvalidExpressionResult() {
      final MockEvaluationResult result =
          new MockEvaluationResult(
              EvaluationStatus.INVALID_EXPRESSION,
              null,
              "Syntax error: unexpected token",
              1L,
              "x + + y");

      assertEquals(
          EvaluationStatus.INVALID_EXPRESSION,
          result.getStatus(),
          "Status should be INVALID_EXPRESSION");
      assertEquals("x + + y", result.getExpression(), "Expression should match");
    }

    @Test
    @DisplayName("mock variable value should return correct properties")
    void mockVariableValueShouldReturnCorrectProperties() {
      final MockVariableValue value = new MockVariableValue("Hello, World!", ValueType.STRING);

      assertEquals("Hello, World!", value.asString(), "String value should match");
      assertEquals(ValueType.STRING, value.getValueType(), "Type should match");
    }

    @Test
    @DisplayName("all status values should be iterable")
    void allStatusValuesShouldBeIterable() {
      int count = 0;
      for (final EvaluationStatus status : EvaluationStatus.values()) {
        assertNotNull(status, "Status should not be null");
        count++;
      }
      assertEquals(4, count, "Should have iterated over 4 status values");
    }
  }

  /** Mock implementation of EvaluationResult for testing. */
  private static class MockEvaluationResult implements EvaluationResult {
    private final EvaluationStatus status;
    private final VariableValue value;
    private final String errorMessage;
    private final long duration;
    private final String expression;

    MockEvaluationResult(
        final EvaluationStatus status,
        final VariableValue value,
        final String errorMessage,
        final long duration,
        final String expression) {
      this.status = status;
      this.value = value;
      this.errorMessage = errorMessage;
      this.duration = duration;
      this.expression = expression;
    }

    @Override
    public EvaluationStatus getStatus() {
      return status;
    }

    @Override
    public VariableValue getValue() {
      return value;
    }

    @Override
    public String getErrorMessage() {
      return errorMessage;
    }

    @Override
    public long getDuration() {
      return duration;
    }

    @Override
    public String getExpression() {
      return expression;
    }
  }

  /** Mock implementation of VariableValue for testing. */
  private static class MockVariableValue implements VariableValue {
    private final String stringValue;
    private final ValueType valueType;

    MockVariableValue(final String stringValue, final ValueType valueType) {
      this.stringValue = stringValue;
      this.valueType = valueType;
    }

    @Override
    public String asString() {
      return stringValue;
    }

    @Override
    public int asInt() {
      try {
        return Integer.parseInt(stringValue);
      } catch (NumberFormatException e) {
        return 0;
      }
    }

    @Override
    public long asLong() {
      try {
        return Long.parseLong(stringValue);
      } catch (NumberFormatException e) {
        return 0L;
      }
    }

    @Override
    public float asFloat() {
      try {
        return Float.parseFloat(stringValue);
      } catch (NumberFormatException e) {
        return 0.0f;
      }
    }

    @Override
    public double asDouble() {
      try {
        return Double.parseDouble(stringValue);
      } catch (NumberFormatException e) {
        return 0.0;
      }
    }

    @Override
    public boolean asBoolean() {
      return Boolean.parseBoolean(stringValue);
    }

    @Override
    public Object getRawValue() {
      return stringValue;
    }

    @Override
    public ValueType getValueType() {
      return valueType;
    }
  }
}

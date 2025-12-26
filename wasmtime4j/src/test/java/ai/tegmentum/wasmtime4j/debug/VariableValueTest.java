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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.VariableValue.ValueType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link VariableValue} interface.
 *
 * <p>VariableValue provides type-safe access to variable values in WebAssembly debugging including
 * conversion methods for different data types.
 */
@DisplayName("VariableValue Tests")
class VariableValueTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(VariableValue.class.isInterface(), "VariableValue should be an interface");
    }

    @Test
    @DisplayName("should have asString method")
    void shouldHaveAsStringMethod() throws NoSuchMethodException {
      final Method method = VariableValue.class.getMethod("asString");
      assertNotNull(method, "asString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have asInt method")
    void shouldHaveAsIntMethod() throws NoSuchMethodException {
      final Method method = VariableValue.class.getMethod("asInt");
      assertNotNull(method, "asInt method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have asLong method")
    void shouldHaveAsLongMethod() throws NoSuchMethodException {
      final Method method = VariableValue.class.getMethod("asLong");
      assertNotNull(method, "asLong method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have asFloat method")
    void shouldHaveAsFloatMethod() throws NoSuchMethodException {
      final Method method = VariableValue.class.getMethod("asFloat");
      assertNotNull(method, "asFloat method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }

    @Test
    @DisplayName("should have asDouble method")
    void shouldHaveAsDoubleMethod() throws NoSuchMethodException {
      final Method method = VariableValue.class.getMethod("asDouble");
      assertNotNull(method, "asDouble method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have asBoolean method")
    void shouldHaveAsBooleanMethod() throws NoSuchMethodException {
      final Method method = VariableValue.class.getMethod("asBoolean");
      assertNotNull(method, "asBoolean method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getRawValue method")
    void shouldHaveGetRawValueMethod() throws NoSuchMethodException {
      final Method method = VariableValue.class.getMethod("getRawValue");
      assertNotNull(method, "getRawValue method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
    }

    @Test
    @DisplayName("should have getValueType method")
    void shouldHaveGetValueTypeMethod() throws NoSuchMethodException {
      final Method method = VariableValue.class.getMethod("getValueType");
      assertNotNull(method, "getValueType method should exist");
      assertEquals(ValueType.class, method.getReturnType(), "Should return ValueType");
    }
  }

  @Nested
  @DisplayName("ValueType Enum Tests")
  class ValueTypeEnumTests {

    @Test
    @DisplayName("should have INTEGER value")
    void shouldHaveIntegerValue() {
      assertNotNull(ValueType.valueOf("INTEGER"), "INTEGER should exist");
    }

    @Test
    @DisplayName("should have LONG value")
    void shouldHaveLongValue() {
      assertNotNull(ValueType.valueOf("LONG"), "LONG should exist");
    }

    @Test
    @DisplayName("should have FLOAT value")
    void shouldHaveFloatValue() {
      assertNotNull(ValueType.valueOf("FLOAT"), "FLOAT should exist");
    }

    @Test
    @DisplayName("should have DOUBLE value")
    void shouldHaveDoubleValue() {
      assertNotNull(ValueType.valueOf("DOUBLE"), "DOUBLE should exist");
    }

    @Test
    @DisplayName("should have BOOLEAN value")
    void shouldHaveBooleanValue() {
      assertNotNull(ValueType.valueOf("BOOLEAN"), "BOOLEAN should exist");
    }

    @Test
    @DisplayName("should have STRING value")
    void shouldHaveStringValue() {
      assertNotNull(ValueType.valueOf("STRING"), "STRING should exist");
    }

    @Test
    @DisplayName("should have OBJECT value")
    void shouldHaveObjectValue() {
      assertNotNull(ValueType.valueOf("OBJECT"), "OBJECT should exist");
    }

    @Test
    @DisplayName("should have exactly seven values")
    void shouldHaveExactlySevenValues() {
      assertEquals(7, ValueType.values().length, "Should have exactly 7 ValueType values");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock value should return integer value")
    void mockValueShouldReturnIntegerValue() {
      final MockVariableValue value = new MockVariableValue(42);

      assertEquals(42, value.asInt(), "Int value should match");
      assertEquals(42L, value.asLong(), "Long value should match");
      assertEquals(42.0f, value.asFloat(), 0.001, "Float value should match");
      assertEquals(42.0, value.asDouble(), 0.001, "Double value should match");
      assertEquals("42", value.asString(), "String value should match");
      assertEquals(ValueType.INTEGER, value.getValueType(), "Value type should be INTEGER");
    }

    @Test
    @DisplayName("mock value should return long value")
    void mockValueShouldReturnLongValue() {
      final MockVariableValue value = new MockVariableValue(Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, value.asLong(), "Long value should match");
      assertEquals(ValueType.LONG, value.getValueType(), "Value type should be LONG");
    }

    @Test
    @DisplayName("mock value should return float value")
    void mockValueShouldReturnFloatValue() {
      final MockVariableValue value = new MockVariableValue(3.14f);

      assertEquals(3.14f, value.asFloat(), 0.001, "Float value should match");
      assertEquals(3.14, value.asDouble(), 0.01, "Double value should match");
      assertEquals(ValueType.FLOAT, value.getValueType(), "Value type should be FLOAT");
    }

    @Test
    @DisplayName("mock value should return double value")
    void mockValueShouldReturnDoubleValue() {
      final MockVariableValue value = new MockVariableValue(2.718281828);

      assertEquals(2.718281828, value.asDouble(), 0.0001, "Double value should match");
      assertEquals(ValueType.DOUBLE, value.getValueType(), "Value type should be DOUBLE");
    }

    @Test
    @DisplayName("mock value should return boolean value")
    void mockValueShouldReturnBooleanValue() {
      final MockVariableValue trueValue = new MockVariableValue(true);
      final MockVariableValue falseValue = new MockVariableValue(false);

      assertTrue(trueValue.asBoolean(), "Boolean should be true");
      assertFalse(falseValue.asBoolean(), "Boolean should be false");
      assertEquals(ValueType.BOOLEAN, trueValue.getValueType(), "Value type should be BOOLEAN");
    }

    @Test
    @DisplayName("mock value should return string value")
    void mockValueShouldReturnStringValue() {
      final MockVariableValue value = new MockVariableValue("hello world");

      assertEquals("hello world", value.asString(), "String value should match");
      assertEquals(ValueType.STRING, value.getValueType(), "Value type should be STRING");
    }

    @Test
    @DisplayName("mock value should return raw value")
    void mockValueShouldReturnRawValue() {
      final Object original = new Object();
      final MockVariableValue value = new MockVariableValue(original);

      assertEquals(original, value.getRawValue(), "Raw value should match");
      assertEquals(ValueType.OBJECT, value.getValueType(), "Value type should be OBJECT");
    }
  }

  /** Mock implementation of VariableValue for testing. */
  private static class MockVariableValue implements VariableValue {
    private final Object value;

    MockVariableValue(final Object value) {
      this.value = value;
    }

    @Override
    public String asString() {
      return String.valueOf(value);
    }

    @Override
    public int asInt() {
      if (value instanceof Number) {
        return ((Number) value).intValue();
      }
      throw new IllegalStateException("Cannot convert to int");
    }

    @Override
    public long asLong() {
      if (value instanceof Number) {
        return ((Number) value).longValue();
      }
      throw new IllegalStateException("Cannot convert to long");
    }

    @Override
    public float asFloat() {
      if (value instanceof Number) {
        return ((Number) value).floatValue();
      }
      throw new IllegalStateException("Cannot convert to float");
    }

    @Override
    public double asDouble() {
      if (value instanceof Number) {
        return ((Number) value).doubleValue();
      }
      throw new IllegalStateException("Cannot convert to double");
    }

    @Override
    public boolean asBoolean() {
      if (value instanceof Boolean) {
        return (Boolean) value;
      }
      throw new IllegalStateException("Cannot convert to boolean");
    }

    @Override
    public Object getRawValue() {
      return value;
    }

    @Override
    public ValueType getValueType() {
      if (value instanceof Integer) {
        return ValueType.INTEGER;
      }
      if (value instanceof Long) {
        return ValueType.LONG;
      }
      if (value instanceof Float) {
        return ValueType.FLOAT;
      }
      if (value instanceof Double) {
        return ValueType.DOUBLE;
      }
      if (value instanceof Boolean) {
        return ValueType.BOOLEAN;
      }
      if (value instanceof String) {
        return ValueType.STRING;
      }
      return ValueType.OBJECT;
    }
  }
}

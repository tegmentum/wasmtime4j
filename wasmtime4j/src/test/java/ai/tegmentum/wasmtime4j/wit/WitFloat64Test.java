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
package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitFloat64} class.
 *
 * <p>WitFloat64 represents a WIT 64-bit IEEE 754 floating-point value. Values are immutable and
 * thread-safe.
 */
@DisplayName("WitFloat64 Tests")
class WitFloat64Test {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create WitFloat64 from positive value")
    void shouldCreateFromPositiveValue() {
      final WitFloat64 value = WitFloat64.of(3.14159265358979);
      assertNotNull(value, "Should create WitFloat64");
      assertEquals(3.14159265358979, value.getValue(), 0.0000001, "Value should be pi");
    }

    @Test
    @DisplayName("should create WitFloat64 from negative value")
    void shouldCreateFromNegativeValue() {
      final WitFloat64 value = WitFloat64.of(-2.718281828);
      assertNotNull(value, "Should create WitFloat64");
      assertEquals(-2.718281828, value.getValue(), 0.0000001, "Value should be -e");
    }

    @Test
    @DisplayName("should create WitFloat64 from zero")
    void shouldCreateFromZero() {
      final WitFloat64 value = WitFloat64.of(0.0);
      assertEquals(0.0, value.getValue(), "Value should be 0.0");
    }

    @Test
    @DisplayName("should create WitFloat64 from positive infinity")
    void shouldCreateFromPositiveInfinity() {
      final WitFloat64 value = WitFloat64.of(Double.POSITIVE_INFINITY);
      assertEquals(Double.POSITIVE_INFINITY, value.getValue(), "Value should be positive infinity");
    }

    @Test
    @DisplayName("should create WitFloat64 from negative infinity")
    void shouldCreateFromNegativeInfinity() {
      final WitFloat64 value = WitFloat64.of(Double.NEGATIVE_INFINITY);
      assertEquals(Double.NEGATIVE_INFINITY, value.getValue(), "Value should be negative infinity");
    }

    @Test
    @DisplayName("should create WitFloat64 from NaN")
    void shouldCreateFromNaN() {
      final WitFloat64 value = WitFloat64.of(Double.NaN);
      assertTrue(Double.isNaN(value.getValue()), "Value should be NaN");
    }

    @Test
    @DisplayName("should create WitFloat64 from max value")
    void shouldCreateFromMaxValue() {
      final WitFloat64 value = WitFloat64.of(Double.MAX_VALUE);
      assertEquals(Double.MAX_VALUE, value.getValue(), "Value should be max");
    }

    @Test
    @DisplayName("should create WitFloat64 from min value")
    void shouldCreateFromMinValue() {
      final WitFloat64 value = WitFloat64.of(Double.MIN_VALUE);
      assertEquals(Double.MIN_VALUE, value.getValue(), "Value should be min");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getValue should return correct value")
    void getValueShouldReturnCorrectValue() {
      final WitFloat64 value = WitFloat64.of(1.5);
      assertEquals(1.5, value.getValue(), "Should return correct value");
    }

    @Test
    @DisplayName("toJava should return Double")
    void toJavaShouldReturnDouble() {
      final WitFloat64 value = WitFloat64.of(2.5);
      assertEquals(Double.valueOf(2.5), value.toJava(), "toJava should return Double");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same value should be equal")
    void sameValueShouldBeEqual() {
      final WitFloat64 value1 = WitFloat64.of(3.14159265358979);
      final WitFloat64 value2 = WitFloat64.of(3.14159265358979);
      assertEquals(value1, value2, "Same values should be equal");
    }

    @Test
    @DisplayName("different values should not be equal")
    void differentValuesShouldNotBeEqual() {
      final WitFloat64 value1 = WitFloat64.of(3.14159265358979);
      final WitFloat64 value2 = WitFloat64.of(2.718281828);
      assertNotEquals(value1, value2, "Different values should not be equal");
    }

    @Test
    @DisplayName("NaN values should be equal")
    void nanValuesShouldBeEqual() {
      final WitFloat64 value1 = WitFloat64.of(Double.NaN);
      final WitFloat64 value2 = WitFloat64.of(Double.NaN);
      assertEquals(value1, value2, "NaN values should be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same value should have same hash code")
    void sameValueShouldHaveSameHashCode() {
      final WitFloat64 value1 = WitFloat64.of(3.14159265358979);
      final WitFloat64 value2 = WitFloat64.of(3.14159265358979);
      assertEquals(value1.hashCode(), value2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain value")
    void toStringShouldContainValue() {
      final WitFloat64 value = WitFloat64.of(3.14);
      final String str = value.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("3.14"), "toString should contain value");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitFloat64 value = WitFloat64.of(3.14);
      assertNotNull(value.getType(), "Should have WitType");
    }

    @Test
    @DisplayName("should report as floating point")
    void shouldReportAsFloatingPoint() {
      final WitFloat64 value = WitFloat64.of(3.14);
      assertTrue(value.isFloatingPoint(), "Should report as floating point");
    }
  }
}

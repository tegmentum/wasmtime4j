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
 * Tests for {@link WitFloat32} class.
 *
 * <p>WitFloat32 represents a WIT 32-bit IEEE 754 floating-point value. Values are immutable and
 * thread-safe.
 */
@DisplayName("WitFloat32 Tests")
class WitFloat32Test {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create WitFloat32 from positive value")
    void shouldCreateFromPositiveValue() {
      final WitFloat32 value = WitFloat32.of(3.14f);
      assertNotNull(value, "Should create WitFloat32");
      assertEquals(3.14f, value.getValue(), 0.001f, "Value should be 3.14");
    }

    @Test
    @DisplayName("should create WitFloat32 from negative value")
    void shouldCreateFromNegativeValue() {
      final WitFloat32 value = WitFloat32.of(-2.71f);
      assertNotNull(value, "Should create WitFloat32");
      assertEquals(-2.71f, value.getValue(), 0.001f, "Value should be -2.71");
    }

    @Test
    @DisplayName("should create WitFloat32 from zero")
    void shouldCreateFromZero() {
      final WitFloat32 value = WitFloat32.of(0.0f);
      assertEquals(0.0f, value.getValue(), "Value should be 0.0");
    }

    @Test
    @DisplayName("should create WitFloat32 from positive infinity")
    void shouldCreateFromPositiveInfinity() {
      final WitFloat32 value = WitFloat32.of(Float.POSITIVE_INFINITY);
      assertEquals(Float.POSITIVE_INFINITY, value.getValue(), "Value should be positive infinity");
    }

    @Test
    @DisplayName("should create WitFloat32 from negative infinity")
    void shouldCreateFromNegativeInfinity() {
      final WitFloat32 value = WitFloat32.of(Float.NEGATIVE_INFINITY);
      assertEquals(Float.NEGATIVE_INFINITY, value.getValue(), "Value should be negative infinity");
    }

    @Test
    @DisplayName("should create WitFloat32 from NaN")
    void shouldCreateFromNaN() {
      final WitFloat32 value = WitFloat32.of(Float.NaN);
      assertTrue(Float.isNaN(value.getValue()), "Value should be NaN");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getValue should return correct value")
    void getValueShouldReturnCorrectValue() {
      final WitFloat32 value = WitFloat32.of(1.5f);
      assertEquals(1.5f, value.getValue(), "Should return correct value");
    }

    @Test
    @DisplayName("toJava should return Float")
    void toJavaShouldReturnFloat() {
      final WitFloat32 value = WitFloat32.of(2.5f);
      assertEquals(Float.valueOf(2.5f), value.toJava(), "toJava should return Float");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same value should be equal")
    void sameValueShouldBeEqual() {
      final WitFloat32 value1 = WitFloat32.of(3.14f);
      final WitFloat32 value2 = WitFloat32.of(3.14f);
      assertEquals(value1, value2, "Same values should be equal");
    }

    @Test
    @DisplayName("different values should not be equal")
    void differentValuesShouldNotBeEqual() {
      final WitFloat32 value1 = WitFloat32.of(3.14f);
      final WitFloat32 value2 = WitFloat32.of(2.71f);
      assertNotEquals(value1, value2, "Different values should not be equal");
    }

    @Test
    @DisplayName("NaN values should be equal")
    void nanValuesShouldBeEqual() {
      final WitFloat32 value1 = WitFloat32.of(Float.NaN);
      final WitFloat32 value2 = WitFloat32.of(Float.NaN);
      assertEquals(value1, value2, "NaN values should be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same value should have same hash code")
    void sameValueShouldHaveSameHashCode() {
      final WitFloat32 value1 = WitFloat32.of(3.14f);
      final WitFloat32 value2 = WitFloat32.of(3.14f);
      assertEquals(value1.hashCode(), value2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain value")
    void toStringShouldContainValue() {
      final WitFloat32 value = WitFloat32.of(3.14f);
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
      final WitFloat32 value = WitFloat32.of(3.14f);
      assertNotNull(value.getType(), "Should have WitType");
    }

    @Test
    @DisplayName("should report as floating point")
    void shouldReportAsFloatingPoint() {
      final WitFloat32 value = WitFloat32.of(3.14f);
      assertTrue(value.isFloatingPoint(), "Should report as floating point");
    }
  }
}

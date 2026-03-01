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
 * Tests for {@link WitS32} class.
 *
 * <p>WitS32 represents a WIT signed 32-bit integer value. Values are immutable and thread-safe.
 */
@DisplayName("WitS32 Tests")
class WitS32Test {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create WitS32 from positive value")
    void shouldCreateFromPositiveValue() {
      final WitS32 value = WitS32.of(100000);
      assertNotNull(value, "Should create WitS32");
      assertEquals(100000, value.getValue(), "Value should be 100000");
    }

    @Test
    @DisplayName("should create WitS32 from negative value")
    void shouldCreateFromNegativeValue() {
      final WitS32 value = WitS32.of(-100000);
      assertNotNull(value, "Should create WitS32");
      assertEquals(-100000, value.getValue(), "Value should be -100000");
    }

    @Test
    @DisplayName("should create WitS32 from zero")
    void shouldCreateFromZero() {
      final WitS32 value = WitS32.of(0);
      assertEquals(0, value.getValue(), "Value should be 0");
    }

    @Test
    @DisplayName("should create WitS32 from max value")
    void shouldCreateFromMaxValue() {
      final WitS32 value = WitS32.of(Integer.MAX_VALUE);
      assertEquals(Integer.MAX_VALUE, value.getValue(), "Value should be max");
    }

    @Test
    @DisplayName("should create WitS32 from min value")
    void shouldCreateFromMinValue() {
      final WitS32 value = WitS32.of(Integer.MIN_VALUE);
      assertEquals(Integer.MIN_VALUE, value.getValue(), "Value should be min");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getValue should return correct value")
    void getValueShouldReturnCorrectValue() {
      final WitS32 value = WitS32.of(123456);
      assertEquals(123456, value.getValue(), "Should return correct value");
    }

    @Test
    @DisplayName("toJava should return Integer")
    void toJavaShouldReturnInteger() {
      final WitS32 value = WitS32.of(5000);
      assertEquals(Integer.valueOf(5000), value.toJava(), "toJava should return Integer");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same value should be equal")
    void sameValueShouldBeEqual() {
      final WitS32 value1 = WitS32.of(100000);
      final WitS32 value2 = WitS32.of(100000);
      assertEquals(value1, value2, "Same values should be equal");
    }

    @Test
    @DisplayName("different values should not be equal")
    void differentValuesShouldNotBeEqual() {
      final WitS32 value1 = WitS32.of(100000);
      final WitS32 value2 = WitS32.of(100001);
      assertNotEquals(value1, value2, "Different values should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same value should have same hash code")
    void sameValueShouldHaveSameHashCode() {
      final WitS32 value1 = WitS32.of(100000);
      final WitS32 value2 = WitS32.of(100000);
      assertEquals(value1.hashCode(), value2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain value")
    void toStringShouldContainValue() {
      final WitS32 value = WitS32.of(100000);
      final String str = value.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("100000"), "toString should contain value");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitS32 value = WitS32.of(42);
      assertNotNull(value.getType(), "Should have WitType");
    }
  }
}

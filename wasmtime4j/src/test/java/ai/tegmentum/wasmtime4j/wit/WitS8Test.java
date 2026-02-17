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
 * Tests for {@link WitS8} class.
 *
 * <p>WitS8 represents a WIT signed 8-bit integer value. Values are immutable and thread-safe.
 */
@DisplayName("WitS8 Tests")
class WitS8Test {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create WitS8 from positive value")
    void shouldCreateFromPositiveValue() {
      final WitS8 value = WitS8.of((byte) 42);
      assertNotNull(value, "Should create WitS8");
      assertEquals((byte) 42, value.getValue(), "Value should be 42");
    }

    @Test
    @DisplayName("should create WitS8 from negative value")
    void shouldCreateFromNegativeValue() {
      final WitS8 value = WitS8.of((byte) -42);
      assertNotNull(value, "Should create WitS8");
      assertEquals((byte) -42, value.getValue(), "Value should be -42");
    }

    @Test
    @DisplayName("should create WitS8 from zero")
    void shouldCreateFromZero() {
      final WitS8 value = WitS8.of((byte) 0);
      assertEquals((byte) 0, value.getValue(), "Value should be 0");
    }

    @Test
    @DisplayName("should create WitS8 from max value")
    void shouldCreateFromMaxValue() {
      final WitS8 value = WitS8.of(Byte.MAX_VALUE);
      assertEquals(Byte.MAX_VALUE, value.getValue(), "Value should be max");
    }

    @Test
    @DisplayName("should create WitS8 from min value")
    void shouldCreateFromMinValue() {
      final WitS8 value = WitS8.of(Byte.MIN_VALUE);
      assertEquals(Byte.MIN_VALUE, value.getValue(), "Value should be min");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getValue should return correct value")
    void getValueShouldReturnCorrectValue() {
      final WitS8 value = WitS8.of((byte) 100);
      assertEquals((byte) 100, value.getValue(), "Should return correct value");
    }

    @Test
    @DisplayName("toJava should return Byte")
    void toJavaShouldReturnByte() {
      final WitS8 value = WitS8.of((byte) 50);
      assertEquals(Byte.valueOf((byte) 50), value.toJava(), "toJava should return Byte");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same value should be equal")
    void sameValueShouldBeEqual() {
      final WitS8 value1 = WitS8.of((byte) 42);
      final WitS8 value2 = WitS8.of((byte) 42);
      assertEquals(value1, value2, "Same values should be equal");
    }

    @Test
    @DisplayName("different values should not be equal")
    void differentValuesShouldNotBeEqual() {
      final WitS8 value1 = WitS8.of((byte) 42);
      final WitS8 value2 = WitS8.of((byte) 43);
      assertNotEquals(value1, value2, "Different values should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same value should have same hash code")
    void sameValueShouldHaveSameHashCode() {
      final WitS8 value1 = WitS8.of((byte) 42);
      final WitS8 value2 = WitS8.of((byte) 42);
      assertEquals(value1.hashCode(), value2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain value")
    void toStringShouldContainValue() {
      final WitS8 value = WitS8.of((byte) 42);
      final String str = value.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("42"), "toString should contain value");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitS8 value = WitS8.of((byte) 42);
      assertNotNull(value.getType(), "Should have WitType");
    }
  }
}

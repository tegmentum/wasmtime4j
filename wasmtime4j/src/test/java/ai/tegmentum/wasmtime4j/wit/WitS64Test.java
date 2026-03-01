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
 * Tests for {@link WitS64} class.
 *
 * <p>WitS64 represents a WIT signed 64-bit integer value. Values are immutable and thread-safe.
 */
@DisplayName("WitS64 Tests")
class WitS64Test {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create WitS64 from positive value")
    void shouldCreateFromPositiveValue() {
      final WitS64 value = WitS64.of(10000000000L);
      assertNotNull(value, "Should create WitS64");
      assertEquals(10000000000L, value.getValue(), "Value should be correct");
    }

    @Test
    @DisplayName("should create WitS64 from negative value")
    void shouldCreateFromNegativeValue() {
      final WitS64 value = WitS64.of(-10000000000L);
      assertNotNull(value, "Should create WitS64");
      assertEquals(-10000000000L, value.getValue(), "Value should be correct");
    }

    @Test
    @DisplayName("should create WitS64 from zero")
    void shouldCreateFromZero() {
      final WitS64 value = WitS64.of(0L);
      assertEquals(0L, value.getValue(), "Value should be 0");
    }

    @Test
    @DisplayName("should create WitS64 from max value")
    void shouldCreateFromMaxValue() {
      final WitS64 value = WitS64.of(Long.MAX_VALUE);
      assertEquals(Long.MAX_VALUE, value.getValue(), "Value should be max");
    }

    @Test
    @DisplayName("should create WitS64 from min value")
    void shouldCreateFromMinValue() {
      final WitS64 value = WitS64.of(Long.MIN_VALUE);
      assertEquals(Long.MIN_VALUE, value.getValue(), "Value should be min");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getValue should return correct value")
    void getValueShouldReturnCorrectValue() {
      final WitS64 value = WitS64.of(123456789012L);
      assertEquals(123456789012L, value.getValue(), "Should return correct value");
    }

    @Test
    @DisplayName("toJava should return Long")
    void toJavaShouldReturnLong() {
      final WitS64 value = WitS64.of(5000000000L);
      assertEquals(Long.valueOf(5000000000L), value.toJava(), "toJava should return Long");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same value should be equal")
    void sameValueShouldBeEqual() {
      final WitS64 value1 = WitS64.of(10000000000L);
      final WitS64 value2 = WitS64.of(10000000000L);
      assertEquals(value1, value2, "Same values should be equal");
    }

    @Test
    @DisplayName("different values should not be equal")
    void differentValuesShouldNotBeEqual() {
      final WitS64 value1 = WitS64.of(10000000000L);
      final WitS64 value2 = WitS64.of(10000000001L);
      assertNotEquals(value1, value2, "Different values should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same value should have same hash code")
    void sameValueShouldHaveSameHashCode() {
      final WitS64 value1 = WitS64.of(10000000000L);
      final WitS64 value2 = WitS64.of(10000000000L);
      assertEquals(value1.hashCode(), value2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain value")
    void toStringShouldContainValue() {
      final WitS64 value = WitS64.of(10000000000L);
      final String str = value.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("10000000000"), "toString should contain value");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitS64 value = WitS64.of(42L);
      assertNotNull(value.getType(), "Should have WitType");
    }
  }
}

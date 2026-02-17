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

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitS16} class.
 *
 * <p>WitS16 represents a WIT signed 16-bit integer value. Values are immutable and thread-safe.
 */
@DisplayName("WitS16 Tests")
class WitS16Test {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have of factory method")
    void shouldHaveOfFactoryMethod() throws NoSuchMethodException {
      final Method method = WitS16.class.getMethod("of", short.class);
      assertNotNull(method, "Should have of(short) method");
      assertEquals(WitS16.class, method.getReturnType(), "Should return WitS16");
    }

    @Test
    @DisplayName("should create WitS16 from positive value")
    void shouldCreateFromPositiveValue() {
      final WitS16 value = WitS16.of((short) 1000);
      assertNotNull(value, "Should create WitS16");
      assertEquals((short) 1000, value.getValue(), "Value should be 1000");
    }

    @Test
    @DisplayName("should create WitS16 from negative value")
    void shouldCreateFromNegativeValue() {
      final WitS16 value = WitS16.of((short) -1000);
      assertNotNull(value, "Should create WitS16");
      assertEquals((short) -1000, value.getValue(), "Value should be -1000");
    }

    @Test
    @DisplayName("should create WitS16 from zero")
    void shouldCreateFromZero() {
      final WitS16 value = WitS16.of((short) 0);
      assertEquals((short) 0, value.getValue(), "Value should be 0");
    }

    @Test
    @DisplayName("should create WitS16 from max value")
    void shouldCreateFromMaxValue() {
      final WitS16 value = WitS16.of(Short.MAX_VALUE);
      assertEquals(Short.MAX_VALUE, value.getValue(), "Value should be max");
    }

    @Test
    @DisplayName("should create WitS16 from min value")
    void shouldCreateFromMinValue() {
      final WitS16 value = WitS16.of(Short.MIN_VALUE);
      assertEquals(Short.MIN_VALUE, value.getValue(), "Value should be min");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getValue should return correct value")
    void getValueShouldReturnCorrectValue() {
      final WitS16 value = WitS16.of((short) 12345);
      assertEquals((short) 12345, value.getValue(), "Should return correct value");
    }

    @Test
    @DisplayName("toJava should return Short")
    void toJavaShouldReturnShort() {
      final WitS16 value = WitS16.of((short) 500);
      assertEquals(Short.valueOf((short) 500), value.toJava(), "toJava should return Short");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same value should be equal")
    void sameValueShouldBeEqual() {
      final WitS16 value1 = WitS16.of((short) 1000);
      final WitS16 value2 = WitS16.of((short) 1000);
      assertEquals(value1, value2, "Same values should be equal");
    }

    @Test
    @DisplayName("different values should not be equal")
    void differentValuesShouldNotBeEqual() {
      final WitS16 value1 = WitS16.of((short) 1000);
      final WitS16 value2 = WitS16.of((short) 1001);
      assertNotEquals(value1, value2, "Different values should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same value should have same hash code")
    void sameValueShouldHaveSameHashCode() {
      final WitS16 value1 = WitS16.of((short) 1000);
      final WitS16 value2 = WitS16.of((short) 1000);
      assertEquals(value1.hashCode(), value2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain value")
    void toStringShouldContainValue() {
      final WitS16 value = WitS16.of((short) 1000);
      final String str = value.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("1000"), "toString should contain value");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitS16 value = WitS16.of((short) 42);
      assertNotNull(value.getType(), "Should have WitType");
    }
  }
}

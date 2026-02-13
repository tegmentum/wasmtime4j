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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WitMarshalingException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitString} class.
 *
 * <p>WitString represents a WIT string value (UTF-8 encoded string). Values are immutable and
 * thread-safe.
 */
@DisplayName("WitString Tests")
class WitStringTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitString.class.getModifiers()), "WitString should be final");
    }

    @Test
    @DisplayName("should extend WitPrimitiveValue")
    void shouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitString.class),
          "WitString should extend WitPrimitiveValue");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create WitString from valid string")
    void shouldCreateFromValidString() throws WitMarshalingException {
      final WitString witString = WitString.of("hello");
      assertNotNull(witString, "Should create WitString from valid string");
      assertEquals("hello", witString.getValue(), "Value should be 'hello'");
    }

    @Test
    @DisplayName("should create WitString from empty string")
    void shouldCreateFromEmptyString() throws WitMarshalingException {
      final WitString witString = WitString.of("");
      assertNotNull(witString, "Should create WitString from empty string");
      assertEquals("", witString.getValue(), "Value should be empty");
    }

    @Test
    @DisplayName("should create WitString from unicode string")
    void shouldCreateFromUnicodeString() throws WitMarshalingException {
      final String unicode = "Hello 世界 🌍";
      final WitString witString = WitString.of(unicode);
      assertEquals(unicode, witString.getValue(), "Should preserve unicode content");
    }

    @Test
    @DisplayName("should throw on null value")
    void shouldThrowOnNullValue() {
      assertThrows(
          WitMarshalingException.class,
          () -> WitString.of(null),
          "Should throw WitMarshalingException for null");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getValue should return correct value")
    void getValueShouldReturnCorrectValue() throws WitMarshalingException {
      final WitString witString = WitString.of("test");
      assertEquals("test", witString.getValue(), "Should return correct value");
    }

    @Test
    @DisplayName("toJava should return same string")
    void toJavaShouldReturnSameString() throws WitMarshalingException {
      final WitString witString = WitString.of("test");
      assertEquals("test", witString.toJava(), "toJava should return same string");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same string value should be equal")
    void sameStringValueShouldBeEqual() throws WitMarshalingException {
      final WitString str1 = WitString.of("hello");
      final WitString str2 = WitString.of("hello");
      assertEquals(str1, str2, "Same string values should be equal");
    }

    @Test
    @DisplayName("different string values should not be equal")
    void differentStringValuesShouldNotBeEqual() throws WitMarshalingException {
      final WitString str1 = WitString.of("hello");
      final WitString str2 = WitString.of("world");
      assertNotEquals(str1, str2, "Different string values should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() throws WitMarshalingException {
      final WitString witString = WitString.of("test");
      assertNotEquals(null, witString, "Should not equal null");
    }

    @Test
    @DisplayName("should not equal object of different type")
    void shouldNotEqualDifferentType() throws WitMarshalingException {
      final WitString witString = WitString.of("test");
      assertNotEquals("test", witString, "Should not equal plain String");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same string value should have same hash code")
    void sameStringValueShouldHaveSameHashCode() throws WitMarshalingException {
      final WitString str1 = WitString.of("hello");
      final WitString str2 = WitString.of("hello");
      assertEquals(str1.hashCode(), str2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain the string value")
    void toStringShouldContainValue() throws WitMarshalingException {
      final WitString witString = WitString.of("hello");
      final String str = witString.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("hello"), "toString should contain the value");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() throws WitMarshalingException {
      final WitString witString = WitString.of("test");
      assertNotNull(witString.getType(), "Should have WitType");
    }
  }
}

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitEnum} class.
 *
 * <p>WitEnum represents a WIT enum value (discriminated choice without payload).
 */
@DisplayName("WitEnum Tests")
class WitEnumTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WitEnum.class.getModifiers()), "WitEnum should be final");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitEnum.class),
          "WitEnum should extend WitValue");
    }

    @Test
    @DisplayName("should have of factory method")
    void shouldHaveOfFactoryMethod() throws NoSuchMethodException {
      final Method method = WitEnum.class.getMethod(
          "of", ai.tegmentum.wasmtime4j.WitType.class, String.class);
      assertNotNull(method, "Should have of(WitType, String) method");
      assertEquals(WitEnum.class, method.getReturnType(), "Should return WitEnum");
    }

    @Test
    @DisplayName("should have getDiscriminant method")
    void shouldHaveGetDiscriminantMethod() throws NoSuchMethodException {
      final Method method = WitEnum.class.getMethod("getDiscriminant");
      assertNotNull(method, "Should have getDiscriminant() method");
      assertEquals(
          String.class, method.getReturnType(), "getDiscriminant should return String");
    }
  }

  @Nested
  @DisplayName("Factory Method Null Validation Tests")
  class FactoryMethodNullTests {

    @Test
    @DisplayName("of with null discriminant should throw IllegalArgumentException")
    void ofWithNullDiscriminantShouldThrow() {
      final var enumType = ai.tegmentum.wasmtime4j.WitType.enumType(
          "color", java.util.Arrays.asList("red", "green", "blue"));
      assertThrows(
          IllegalArgumentException.class,
          () -> WitEnum.of(enumType, null),
          "of with null discriminant should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("of with empty discriminant should throw IllegalArgumentException")
    void ofWithEmptyDiscriminantShouldThrow() {
      final var enumType = ai.tegmentum.wasmtime4j.WitType.enumType(
          "color", java.util.Arrays.asList("red", "green", "blue"));
      assertThrows(
          IllegalArgumentException.class,
          () -> WitEnum.of(enumType, ""),
          "of with empty discriminant should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = WitEnum.class.getMethod("equals", Object.class);
      assertNotNull(method, "Should have equals(Object) method");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = WitEnum.class.getMethod("hashCode");
      assertNotNull(method, "Should have hashCode() method");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = WitEnum.class.getMethod("toString");
      assertNotNull(method, "Should have toString() method");
    }
  }

  @Nested
  @DisplayName("ToJava Tests")
  class ToJavaTests {

    @Test
    @DisplayName("should have toJava method")
    void shouldHaveToJavaMethod() throws NoSuchMethodException {
      final Method method = WitEnum.class.getMethod("toJava");
      assertNotNull(method, "Should have toJava() method");
    }
  }
}

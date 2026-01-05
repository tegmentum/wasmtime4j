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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Precompiled enum.
 *
 * <p>Precompiled indicates the type of precompiled WebAssembly artifact detected. This test
 * verifies the enum structure and method signatures.
 */
@DisplayName("Precompiled Enum Tests")
class PrecompiledTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(Precompiled.class.isEnum(), "Precompiled should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(Precompiled.class.getModifiers()), "Precompiled should be public");
    }
  }

  // ========================================================================
  // Enum Values Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have MODULE value")
    void shouldHaveModuleValue() {
      Precompiled module = Precompiled.MODULE;
      assertNotNull(module, "Precompiled.MODULE should exist");
    }

    @Test
    @DisplayName("should have COMPONENT value")
    void shouldHaveComponentValue() {
      Precompiled component = Precompiled.COMPONENT;
      assertNotNull(component, "Precompiled.COMPONENT should exist");
    }

    @Test
    @DisplayName("should have exactly 2 values")
    void shouldHaveExactly2Values() {
      assertEquals(2, Precompiled.values().length, "Precompiled should have exactly 2 values");
    }

    @Test
    @DisplayName("MODULE should be the first value")
    void moduleShouldBeFirstValue() {
      assertEquals(Precompiled.MODULE, Precompiled.values()[0], "MODULE should be the first value");
    }

    @Test
    @DisplayName("COMPONENT should be the second value")
    void componentShouldBeSecondValue() {
      assertEquals(
          Precompiled.COMPONENT, Precompiled.values()[1], "COMPONENT should be the second value");
    }
  }

  // ========================================================================
  // Instance Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Methods Tests")
  class InstanceMethodsTests {

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      final Method method = Precompiled.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(int.class, method.getReturnType(), "getValue should return int");
    }

    @Test
    @DisplayName("MODULE getValue should return 0")
    void moduleGetValueShouldReturn0() {
      assertEquals(0, Precompiled.MODULE.getValue(), "MODULE.getValue() should return 0");
    }

    @Test
    @DisplayName("COMPONENT getValue should return 1")
    void componentGetValueShouldReturn1() {
      assertEquals(1, Precompiled.COMPONENT.getValue(), "COMPONENT.getValue() should return 1");
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have static fromValue method")
    void shouldHaveFromValueMethod() throws NoSuchMethodException {
      final Method method = Precompiled.class.getMethod("fromValue", int.class);
      assertNotNull(method, "fromValue method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromValue should be static");
      assertEquals(
          Precompiled.class, method.getReturnType(), "fromValue should return Precompiled");
    }

    @Test
    @DisplayName("fromValue(0) should return MODULE")
    void fromValue0ShouldReturnModule() {
      assertEquals(
          Precompiled.MODULE, Precompiled.fromValue(0), "fromValue(0) should return MODULE");
    }

    @Test
    @DisplayName("fromValue(1) should return COMPONENT")
    void fromValue1ShouldReturnComponent() {
      assertEquals(
          Precompiled.COMPONENT, Precompiled.fromValue(1), "fromValue(1) should return COMPONENT");
    }

    @Test
    @DisplayName("fromValue with unknown value should return null")
    void fromValueWithUnknownValueShouldReturnNull() {
      assertNull(Precompiled.fromValue(-1), "fromValue(-1) should return null");
      assertNull(Precompiled.fromValue(2), "fromValue(2) should return null");
      assertNull(Precompiled.fromValue(100), "fromValue(100) should return null");
    }
  }

  // ========================================================================
  // Enum Standard Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Standard Methods Tests")
  class EnumStandardMethodsTests {

    @Test
    @DisplayName("should have values method")
    void shouldHaveValuesMethod() throws NoSuchMethodException {
      final Method method = Precompiled.class.getMethod("values");
      assertNotNull(method, "values method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "values should be static");
      assertEquals(
          Precompiled[].class, method.getReturnType(), "values should return Precompiled[]");
    }

    @Test
    @DisplayName("should have valueOf method")
    void shouldHaveValueOfMethod() throws NoSuchMethodException {
      final Method method = Precompiled.class.getMethod("valueOf", String.class);
      assertNotNull(method, "valueOf method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "valueOf should be static");
      assertEquals(Precompiled.class, method.getReturnType(), "valueOf should return Precompiled");
    }

    @Test
    @DisplayName("valueOf should work correctly")
    void valueOfShouldWorkCorrectly() {
      assertEquals(
          Precompiled.MODULE, Precompiled.valueOf("MODULE"), "valueOf('MODULE') should work");
      assertEquals(
          Precompiled.COMPONENT,
          Precompiled.valueOf("COMPONENT"),
          "valueOf('COMPONENT') should work");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods = Set.of("getValue", "fromValue", "values", "valueOf");

      Set<String> actualMethods =
          Arrays.stream(Precompiled.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Precompiled should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Ordinal Tests
  // ========================================================================

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("MODULE ordinal should be 0")
    void moduleOrdinalShouldBe0() {
      assertEquals(0, Precompiled.MODULE.ordinal(), "MODULE ordinal should be 0");
    }

    @Test
    @DisplayName("COMPONENT ordinal should be 1")
    void componentOrdinalShouldBe1() {
      assertEquals(1, Precompiled.COMPONENT.ordinal(), "COMPONENT ordinal should be 1");
    }
  }

  // ========================================================================
  // Name Tests
  // ========================================================================

  @Nested
  @DisplayName("Name Tests")
  class NameTests {

    @Test
    @DisplayName("MODULE name should be 'MODULE'")
    void moduleNameShouldBeModule() {
      assertEquals("MODULE", Precompiled.MODULE.name(), "MODULE name should be 'MODULE'");
    }

    @Test
    @DisplayName("COMPONENT name should be 'COMPONENT'")
    void componentNameShouldBeComponent() {
      assertEquals(
          "COMPONENT", Precompiled.COMPONENT.name(), "COMPONENT name should be 'COMPONENT'");
    }
  }
}

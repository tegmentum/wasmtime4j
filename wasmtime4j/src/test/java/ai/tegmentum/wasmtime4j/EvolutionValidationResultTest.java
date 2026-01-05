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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the EvolutionValidationResult interface.
 *
 * <p>EvolutionValidationResult provides validation results for WebAssembly module evolution
 * operations.
 */
@DisplayName("EvolutionValidationResult Interface Tests")
class EvolutionValidationResultTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          EvolutionValidationResult.class.isInterface(),
          "EvolutionValidationResult should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(EvolutionValidationResult.class.getModifiers()),
          "EvolutionValidationResult should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(EvolutionValidationResult.class.getModifiers()),
          "EvolutionValidationResult should not be final (interfaces cannot be final)");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interfaces")
    void shouldNotExtendAnyInterfaces() {
      assertEquals(
          0,
          EvolutionValidationResult.class.getInterfaces().length,
          "EvolutionValidationResult should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = EvolutionValidationResult.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertFalse(method.isDefault(), "isValid should be abstract");
    }

    @Test
    @DisplayName("should have getErrors method")
    void shouldHaveGetErrorsMethod() throws NoSuchMethodException {
      Method method = EvolutionValidationResult.class.getMethod("getErrors");
      assertNotNull(method, "getErrors method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
      assertFalse(method.isDefault(), "getErrors should be abstract");
    }

    @Test
    @DisplayName("should have exactly 2 abstract methods")
    void shouldHaveExactly2AbstractMethods() {
      long abstractMethods =
          Arrays.stream(EvolutionValidationResult.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(
          2, abstractMethods, "EvolutionValidationResult should have exactly 2 abstract methods");
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
      Set<String> expectedMethods = Set.of("isValid", "getErrors");

      Set<String> actualMethods =
          Arrays.stream(EvolutionValidationResult.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected),
            "EvolutionValidationResult should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 2 declared methods")
    void shouldHaveExactly2DeclaredMethods() {
      long methodCount =
          Arrays.stream(EvolutionValidationResult.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(
          2, methodCount, "EvolutionValidationResult should have exactly 2 declared methods");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultMethods =
          Arrays.stream(EvolutionValidationResult.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethods, "EvolutionValidationResult should have no default methods");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticMethods =
          Arrays.stream(EvolutionValidationResult.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "EvolutionValidationResult should have no static methods");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0,
          EvolutionValidationResult.class.getDeclaredFields().length,
          "EvolutionValidationResult should have no declared fields");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          EvolutionValidationResult.class.getDeclaredClasses().length,
          "EvolutionValidationResult should have no nested classes");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("isValid should have no parameters")
    void isValidShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = EvolutionValidationResult.class.getMethod("isValid");
      assertEquals(0, method.getParameterCount(), "isValid should have no parameters");
    }

    @Test
    @DisplayName("getErrors should have no parameters")
    void getErrorsShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = EvolutionValidationResult.class.getMethod("getErrors");
      assertEquals(0, method.getParameterCount(), "getErrors should have no parameters");
    }
  }

  // ========================================================================
  // Method Visibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Visibility Tests")
  class MethodVisibilityTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      Arrays.stream(EvolutionValidationResult.class.getDeclaredMethods())
          .filter(m -> !m.isSynthetic())
          .forEach(
              m ->
                  assertTrue(
                      Modifier.isPublic(m.getModifiers()),
                      "Method " + m.getName() + " should be public"));
    }
  }

  // ========================================================================
  // Semantic Tests
  // ========================================================================

  @Nested
  @DisplayName("Semantic Tests")
  class SemanticTests {

    @Test
    @DisplayName("isValid should return primitive boolean")
    void isValidShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = EvolutionValidationResult.class.getMethod("isValid");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isValid should return primitive boolean, not Boolean");
    }

    @Test
    @DisplayName("getErrors should return List type")
    void getErrorsShouldReturnListType() throws NoSuchMethodException {
      Method method = EvolutionValidationResult.class.getMethod("getErrors");
      assertEquals(List.class, method.getReturnType(), "getErrors should return List");
    }
  }
}

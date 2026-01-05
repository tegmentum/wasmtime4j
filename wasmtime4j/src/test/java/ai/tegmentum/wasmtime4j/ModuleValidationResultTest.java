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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
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
 * Comprehensive test suite for the ModuleValidationResult class.
 *
 * <p>ModuleValidationResult encapsulates the result of validating WebAssembly bytecode. This test
 * verifies the class structure and method signatures.
 */
@DisplayName("ModuleValidationResult Class Tests")
class ModuleValidationResultTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertTrue(
          !ModuleValidationResult.class.isInterface(), "ModuleValidationResult should be a class");
      assertTrue(
          !ModuleValidationResult.class.isEnum(), "ModuleValidationResult should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ModuleValidationResult.class.getModifiers()),
          "ModuleValidationResult should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(ModuleValidationResult.class.getModifiers()),
          "ModuleValidationResult should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with 3 parameters")
    void shouldHavePublicConstructorWith3Parameters() throws NoSuchMethodException {
      Constructor<?> constructor =
          ModuleValidationResult.class.getConstructor(boolean.class, List.class, List.class);
      assertNotNull(constructor, "Constructor with 3 parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("constructor should have correct parameter types")
    void constructorShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      Constructor<?> constructor =
          ModuleValidationResult.class.getConstructor(boolean.class, List.class, List.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(3, paramTypes.length, "Constructor should have 3 parameters");
      assertEquals(boolean.class, paramTypes[0], "First parameter should be boolean (isValid)");
      assertEquals(List.class, paramTypes[1], "Second parameter should be List (errors)");
      assertEquals(List.class, paramTypes[2], "Third parameter should be List (warnings)");
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have static success method")
    void shouldHaveStaticSuccessMethod() throws NoSuchMethodException {
      final Method method = ModuleValidationResult.class.getMethod("success");
      assertNotNull(method, "success method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "success should be static");
      assertEquals(
          ModuleValidationResult.class,
          method.getReturnType(),
          "success should return ModuleValidationResult");
    }

    @Test
    @DisplayName("success method should have no parameters")
    void successMethodShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = ModuleValidationResult.class.getMethod("success");
      assertEquals(0, method.getParameterCount(), "success should have no parameters");
    }

    @Test
    @DisplayName("should have static failure method")
    void shouldHaveStaticFailureMethod() throws NoSuchMethodException {
      final Method method = ModuleValidationResult.class.getMethod("failure", List.class);
      assertNotNull(method, "failure method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "failure should be static");
      assertEquals(
          ModuleValidationResult.class,
          method.getReturnType(),
          "failure should return ModuleValidationResult");
    }

    @Test
    @DisplayName("failure method should accept List parameter")
    void failureMethodShouldAcceptListParameter() throws NoSuchMethodException {
      final Method method = ModuleValidationResult.class.getMethod("failure", List.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "failure should have 1 parameter");
      assertEquals(List.class, paramTypes[0], "failure parameter should be List");
    }

    @Test
    @DisplayName("should have static successWithWarnings method")
    void shouldHaveStaticSuccessWithWarningsMethod() throws NoSuchMethodException {
      final Method method =
          ModuleValidationResult.class.getMethod("successWithWarnings", List.class);
      assertNotNull(method, "successWithWarnings method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "successWithWarnings should be static");
      assertEquals(
          ModuleValidationResult.class,
          method.getReturnType(),
          "successWithWarnings should return ModuleValidationResult");
    }

    @Test
    @DisplayName("successWithWarnings method should accept List parameter")
    void successWithWarningsMethodShouldAcceptListParameter() throws NoSuchMethodException {
      final Method method =
          ModuleValidationResult.class.getMethod("successWithWarnings", List.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "successWithWarnings should have 1 parameter");
      assertEquals(List.class, paramTypes[0], "successWithWarnings parameter should be List");
    }
  }

  // ========================================================================
  // Accessor Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = ModuleValidationResult.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have getErrors method")
    void shouldHaveGetErrorsMethod() throws NoSuchMethodException {
      final Method method = ModuleValidationResult.class.getMethod("getErrors");
      assertNotNull(method, "getErrors method should exist");
      assertEquals(List.class, method.getReturnType(), "getErrors should return List");
    }

    @Test
    @DisplayName("should have getWarnings method")
    void shouldHaveGetWarningsMethod() throws NoSuchMethodException {
      final Method method = ModuleValidationResult.class.getMethod("getWarnings");
      assertNotNull(method, "getWarnings method should exist");
      assertEquals(List.class, method.getReturnType(), "getWarnings should return List");
    }

    @Test
    @DisplayName("should have hasErrors method")
    void shouldHaveHasErrorsMethod() throws NoSuchMethodException {
      final Method method = ModuleValidationResult.class.getMethod("hasErrors");
      assertNotNull(method, "hasErrors method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasErrors should return boolean");
    }

    @Test
    @DisplayName("should have hasWarnings method")
    void shouldHaveHasWarningsMethod() throws NoSuchMethodException {
      final Method method = ModuleValidationResult.class.getMethod("hasWarnings");
      assertNotNull(method, "hasWarnings method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasWarnings should return boolean");
    }

    @Test
    @DisplayName("all accessor methods should have no parameters")
    void allAccessorMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          ModuleValidationResult.class.getMethod("isValid").getParameterCount(),
          "isValid should have 0 params");
      assertEquals(
          0,
          ModuleValidationResult.class.getMethod("getErrors").getParameterCount(),
          "getErrors should have 0 params");
      assertEquals(
          0,
          ModuleValidationResult.class.getMethod("getWarnings").getParameterCount(),
          "getWarnings should have 0 params");
      assertEquals(
          0,
          ModuleValidationResult.class.getMethod("hasErrors").getParameterCount(),
          "hasErrors should have 0 params");
      assertEquals(
          0,
          ModuleValidationResult.class.getMethod("hasWarnings").getParameterCount(),
          "hasWarnings should have 0 params");
    }
  }

  // ========================================================================
  // Object Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Methods Tests")
  class ObjectMethodsTests {

    @Test
    @DisplayName("should override toString")
    void shouldOverrideToString() throws NoSuchMethodException {
      final Method method = ModuleValidationResult.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          ModuleValidationResult.class,
          method.getDeclaringClass(),
          "toString should be declared in ModuleValidationResult");
    }

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      final Method method = ModuleValidationResult.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(
          ModuleValidationResult.class,
          method.getDeclaringClass(),
          "equals should be declared in ModuleValidationResult");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      final Method method = ModuleValidationResult.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          ModuleValidationResult.class,
          method.getDeclaringClass(),
          "hashCode should be declared in ModuleValidationResult");
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
      Set<String> expectedMethods =
          Set.of(
              "success",
              "failure",
              "successWithWarnings",
              "isValid",
              "getErrors",
              "getWarnings",
              "hasErrors",
              "hasWarnings",
              "toString",
              "equals",
              "hashCode");

      Set<String> actualMethods =
          Arrays.stream(ModuleValidationResult.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected),
            "ModuleValidationResult should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 12 declared methods")
    void shouldHave12DeclaredMethods() {
      assertEquals(
          12,
          ModuleValidationResult.class.getDeclaredMethods().length,
          "ModuleValidationResult should have exactly 12 methods");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have private final fields")
    void shouldHavePrivateFinalFields() {
      Set<String> expectedFields = Set.of("isValid", "errors", "warnings");

      for (String fieldName : expectedFields) {
        try {
          var field = ModuleValidationResult.class.getDeclaredField(fieldName);
          assertTrue(
              Modifier.isPrivate(field.getModifiers()), fieldName + " field should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), fieldName + " field should be final");
        } catch (NoSuchFieldException e) {
          // Field doesn't exist - fail the test
          assertTrue(false, "Field " + fieldName + " should exist");
        }
      }
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object directly")
    void shouldExtendObjectDirectly() {
      assertEquals(
          Object.class,
          ModuleValidationResult.class.getSuperclass(),
          "ModuleValidationResult should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          ModuleValidationResult.class.getInterfaces().length,
          "ModuleValidationResult should not implement any interfaces");
    }
  }
}

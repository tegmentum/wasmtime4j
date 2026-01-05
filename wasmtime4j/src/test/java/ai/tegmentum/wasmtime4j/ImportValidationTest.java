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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the ImportValidation class.
 *
 * <p>ImportValidation represents the result of import validation for WebAssembly modules.
 */
@DisplayName("ImportValidation Class Tests")
class ImportValidationTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(ImportValidation.class.isInterface(), "Should be a class, not interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ImportValidation.class.getModifiers()),
          "ImportValidation should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(ImportValidation.class.getModifiers()),
          "ImportValidation should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with 6 parameters")
    void shouldHavePublicConstructorWith6Parameters() {
      Constructor<?>[] constructors = ImportValidation.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");

      Constructor<?> constructor = constructors[0];
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
      assertEquals(6, constructor.getParameterCount(), "Should have 6 parameters");
    }

    @Test
    @DisplayName("constructor should have correct parameter types")
    void constructorShouldHaveCorrectParameterTypes() {
      Constructor<?>[] constructors = ImportValidation.class.getDeclaredConstructors();
      Constructor<?> constructor = constructors[0];
      Class<?>[] paramTypes = constructor.getParameterTypes();

      assertEquals(boolean.class, paramTypes[0], "First param should be boolean (valid)");
      assertEquals(List.class, paramTypes[1], "Second param should be List (issues)");
      assertEquals(List.class, paramTypes[2], "Third param should be List (validatedImports)");
      assertEquals(int.class, paramTypes[3], "Fourth param should be int (totalImports)");
      assertEquals(int.class, paramTypes[4], "Fifth param should be int (validImports)");
      assertEquals(
          Duration.class, paramTypes[5], "Sixth param should be Duration (validationTime)");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have 6 private fields")
    void shouldHave6PrivateFields() {
      long privateFields =
          Arrays.stream(ImportValidation.class.getDeclaredFields())
              .filter(f -> Modifier.isPrivate(f.getModifiers()))
              .filter(f -> !f.isSynthetic())
              .count();
      assertEquals(6, privateFields, "Should have 6 private fields");
    }

    @Test
    @DisplayName("should have valid field")
    void shouldHaveValidField() throws NoSuchFieldException {
      Field field = ImportValidation.class.getDeclaredField("valid");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "valid should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "valid should be final");
      assertEquals(boolean.class, field.getType(), "valid should be boolean");
    }

    @Test
    @DisplayName("should have issues field")
    void shouldHaveIssuesField() throws NoSuchFieldException {
      Field field = ImportValidation.class.getDeclaredField("issues");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "issues should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "issues should be final");
      assertEquals(List.class, field.getType(), "issues should be List");
    }

    @Test
    @DisplayName("should have validationTime field")
    void shouldHaveValidationTimeField() throws NoSuchFieldException {
      Field field = ImportValidation.class.getDeclaredField("validationTime");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "validationTime should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "validationTime should be final");
      assertEquals(Duration.class, field.getType(), "validationTime should be Duration");
    }
  }

  // ========================================================================
  // Getter Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = ImportValidation.class.getMethod("isValid");
      assertNotNull(method, "isValid should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("should have getIssues method")
    void shouldHaveGetIssuesMethod() throws NoSuchMethodException {
      Method method = ImportValidation.class.getMethod("getIssues");
      assertNotNull(method, "getIssues should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getValidatedImports method")
    void shouldHaveGetValidatedImportsMethod() throws NoSuchMethodException {
      Method method = ImportValidation.class.getMethod("getValidatedImports");
      assertNotNull(method, "getValidatedImports should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getTotalImports method")
    void shouldHaveGetTotalImportsMethod() throws NoSuchMethodException {
      Method method = ImportValidation.class.getMethod("getTotalImports");
      assertNotNull(method, "getTotalImports should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getValidImports method")
    void shouldHaveGetValidImportsMethod() throws NoSuchMethodException {
      Method method = ImportValidation.class.getMethod("getValidImports");
      assertNotNull(method, "getValidImports should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getValidationTime method")
    void shouldHaveGetValidationTimeMethod() throws NoSuchMethodException {
      Method method = ImportValidation.class.getMethod("getValidationTime");
      assertNotNull(method, "getValidationTime should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }
  }

  // ========================================================================
  // Computed Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Computed Method Tests")
  class ComputedMethodTests {

    @Test
    @DisplayName("should have getValidationRate method")
    void shouldHaveGetValidationRateMethod() throws NoSuchMethodException {
      Method method = ImportValidation.class.getMethod("getValidationRate");
      assertNotNull(method, "getValidationRate should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("should have getIssuesBySeverity method")
    void shouldHaveGetIssuesBySeverityMethod() throws NoSuchMethodException {
      Method method =
          ImportValidation.class.getMethod("getIssuesBySeverity", ImportIssue.Severity.class);
      assertNotNull(method, "getIssuesBySeverity should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have hasCriticalIssues method")
    void shouldHaveHasCriticalIssuesMethod() throws NoSuchMethodException {
      Method method = ImportValidation.class.getMethod("hasCriticalIssues");
      assertNotNull(method, "hasCriticalIssues should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }
  }

  // ========================================================================
  // Object Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should override toString")
    void shouldOverrideToString() throws NoSuchMethodException {
      Method method = ImportValidation.class.getMethod("toString");
      assertEquals(ImportValidation.class, method.getDeclaringClass(), "Should override toString");
    }

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      Method method = ImportValidation.class.getMethod("equals", Object.class);
      assertEquals(ImportValidation.class, method.getDeclaringClass(), "Should override equals");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      Method method = ImportValidation.class.getMethod("hashCode");
      assertEquals(ImportValidation.class, method.getDeclaringClass(), "Should override hashCode");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected public methods")
    void shouldHaveAllExpectedPublicMethods() {
      Set<String> expectedMethods =
          Set.of(
              "isValid",
              "getIssues",
              "getValidatedImports",
              "getTotalImports",
              "getValidImports",
              "getValidationTime",
              "getValidationRate",
              "getIssuesBySeverity",
              "hasCriticalIssues",
              "toString",
              "equals",
              "hashCode");

      Set<String> actualMethods =
          Arrays.stream(ImportValidation.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
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
    @DisplayName("should extend Object")
    void shouldExtendObject() {
      assertEquals(
          Object.class, ImportValidation.class.getSuperclass(), "Should extend Object directly");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0, ImportValidation.class.getInterfaces().length, "Should not implement any interfaces");
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
          ImportValidation.class.getDeclaredClasses().length,
          "ImportValidation should have no nested classes");
    }
  }
}

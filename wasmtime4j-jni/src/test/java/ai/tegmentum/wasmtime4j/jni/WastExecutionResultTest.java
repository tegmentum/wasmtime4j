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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WastExecutionResult class.
 *
 * <p>WastExecutionResult contains aggregate statistics and detailed results for all directives in a
 * WAST (WebAssembly Script) file execution.
 */
@DisplayName("WastExecutionResult Class Tests")
class WastExecutionResultTest {

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
          !WastExecutionResult.class.isInterface() && !WastExecutionResult.class.isEnum(),
          "WastExecutionResult should be a class");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WastExecutionResult.class.getModifiers()),
          "WastExecutionResult should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WastExecutionResult.class.getModifiers()),
          "WastExecutionResult should be final");
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
          Object.class,
          WastExecutionResult.class.getSuperclass(),
          "WastExecutionResult should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          WastExecutionResult.class.getInterfaces().length,
          "WastExecutionResult should not implement any interfaces");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with all parameters")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          WastExecutionResult.class.getConstructor(
              String.class,
              int.class,
              int.class,
              int.class,
              String.class,
              WastDirectiveResult[].class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("constructor should have 6 parameters")
    void constructorShouldHave6Parameters() throws NoSuchMethodException {
      Constructor<?> constructor =
          WastExecutionResult.class.getConstructor(
              String.class,
              int.class,
              int.class,
              int.class,
              String.class,
              WastDirectiveResult[].class);
      assertEquals(6, constructor.getParameterCount(), "Constructor should have 6 parameters");
    }

    @Test
    @DisplayName("should have exactly 1 constructor")
    void shouldHaveExactlyOneConstructor() {
      assertEquals(
          1,
          WastExecutionResult.class.getConstructors().length,
          "Should have exactly 1 public constructor");
    }
  }

  // ========================================================================
  // Getter Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("should have getFilePath method")
    void shouldHaveGetFilePathMethod() throws NoSuchMethodException {
      Method method = WastExecutionResult.class.getMethod("getFilePath");
      assertNotNull(method, "getFilePath method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getTotalDirectives method")
    void shouldHaveGetTotalDirectivesMethod() throws NoSuchMethodException {
      Method method = WastExecutionResult.class.getMethod("getTotalDirectives");
      assertNotNull(method, "getTotalDirectives method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getPassedDirectives method")
    void shouldHaveGetPassedDirectivesMethod() throws NoSuchMethodException {
      Method method = WastExecutionResult.class.getMethod("getPassedDirectives");
      assertNotNull(method, "getPassedDirectives method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getFailedDirectives method")
    void shouldHaveGetFailedDirectivesMethod() throws NoSuchMethodException {
      Method method = WastExecutionResult.class.getMethod("getFailedDirectives");
      assertNotNull(method, "getFailedDirectives method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getExecutionError method")
    void shouldHaveGetExecutionErrorMethod() throws NoSuchMethodException {
      Method method = WastExecutionResult.class.getMethod("getExecutionError");
      assertNotNull(method, "getExecutionError method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getDirectiveResults method")
    void shouldHaveGetDirectiveResultsMethod() throws NoSuchMethodException {
      Method method = WastExecutionResult.class.getMethod("getDirectiveResults");
      assertNotNull(method, "getDirectiveResults method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WastDirectiveResult[].class,
          method.getReturnType(),
          "Should return WastDirectiveResult[]");
    }
  }

  // ========================================================================
  // Computed Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Computed Method Tests")
  class ComputedMethodTests {

    @Test
    @DisplayName("should have allPassed method")
    void shouldHaveAllPassedMethod() throws NoSuchMethodException {
      Method method = WastExecutionResult.class.getMethod("allPassed");
      assertNotNull(method, "allPassed method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getPassRate method")
    void shouldHaveGetPassRateMethod() throws NoSuchMethodException {
      Method method = WastExecutionResult.class.getMethod("getPassRate");
      assertNotNull(method, "getPassRate method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  // ========================================================================
  // Object Method Override Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Method Override Tests")
  class ObjectMethodOverrideTests {

    @Test
    @DisplayName("should override toString method")
    void shouldOverrideToStringMethod() throws NoSuchMethodException {
      Method method = WastExecutionResult.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertEquals(
          WastExecutionResult.class,
          method.getDeclaringClass(),
          "Should be declared in WastExecutionResult");
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
          new HashSet<>(
              Arrays.asList(
                  "getFilePath",
                  "getTotalDirectives",
                  "getPassedDirectives",
                  "getFailedDirectives",
                  "getExecutionError",
                  "getDirectiveResults",
                  "allPassed",
                  "getPassRate",
                  "toString"));

      Set<String> actualMethods =
          Arrays.stream(WastExecutionResult.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 9 declared methods")
    void shouldHaveExactly9DeclaredMethods() {
      long methodCount =
          Arrays.stream(WastExecutionResult.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(9, methodCount, "Should have exactly 9 declared methods");
    }

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticCount =
          Arrays.stream(WastExecutionResult.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticCount, "Should have no static methods");
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
          WastExecutionResult.class.getDeclaredClasses().length,
          "WastExecutionResult should have no nested classes");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have exactly 6 declared fields")
    void shouldHaveExactly6DeclaredFields() {
      assertEquals(
          6,
          WastExecutionResult.class.getDeclaredFields().length,
          "WastExecutionResult should have exactly 6 declared fields");
    }

    @Test
    @DisplayName("all fields should be private")
    void allFieldsShouldBePrivate() {
      boolean allPrivate =
          Arrays.stream(WastExecutionResult.class.getDeclaredFields())
              .allMatch(f -> Modifier.isPrivate(f.getModifiers()));
      assertTrue(allPrivate, "All fields should be private");
    }

    @Test
    @DisplayName("all fields should be final")
    void allFieldsShouldBeFinal() {
      boolean allFinal =
          Arrays.stream(WastExecutionResult.class.getDeclaredFields())
              .allMatch(f -> Modifier.isFinal(f.getModifiers()));
      assertTrue(allFinal, "All fields should be final");
    }
  }
}

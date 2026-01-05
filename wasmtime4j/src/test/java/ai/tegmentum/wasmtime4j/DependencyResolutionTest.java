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
 * Comprehensive test suite for the DependencyResolution class.
 *
 * <p>DependencyResolution provides the result of dependency resolution analysis for WebAssembly
 * modules.
 */
@DisplayName("DependencyResolution Class Tests")
class DependencyResolutionTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(
          DependencyResolution.class.isInterface(),
          "DependencyResolution should not be an interface");
      assertFalse(
          DependencyResolution.class.isEnum(), "DependencyResolution should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(DependencyResolution.class.getModifiers()),
          "DependencyResolution should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(DependencyResolution.class.getModifiers()),
          "DependencyResolution should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have exactly 1 constructor")
    void shouldHaveExactly1Constructor() {
      assertEquals(
          1,
          DependencyResolution.class.getDeclaredConstructors().length,
          "DependencyResolution should have exactly 1 constructor");
    }

    @Test
    @DisplayName("constructor should be public")
    void constructorShouldBePublic() {
      Constructor<?>[] constructors = DependencyResolution.class.getDeclaredConstructors();
      assertTrue(Modifier.isPublic(constructors[0].getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("constructor should have 8 parameters")
    void constructorShouldHave8Parameters() {
      Constructor<?>[] constructors = DependencyResolution.class.getDeclaredConstructors();
      assertEquals(8, constructors[0].getParameterCount(), "Constructor should have 8 parameters");
    }

    @Test
    @DisplayName("constructor parameters should have correct types")
    void constructorParametersShouldHaveCorrectTypes() {
      Constructor<?>[] constructors = DependencyResolution.class.getDeclaredConstructors();
      Class<?>[] paramTypes = constructors[0].getParameterTypes();
      assertEquals(List.class, paramTypes[0], "First param should be List (instantiationOrder)");
      assertEquals(List.class, paramTypes[1], "Second param should be List (dependencies)");
      assertEquals(
          boolean.class, paramTypes[2], "Third param should be boolean (hasCircularDependencies)");
      assertEquals(
          List.class, paramTypes[3], "Fourth param should be List (circularDependencyChains)");
      assertEquals(int.class, paramTypes[4], "Fifth param should be int (totalModules)");
      assertEquals(int.class, paramTypes[5], "Sixth param should be int (resolvedDependencies)");
      assertEquals(
          Duration.class, paramTypes[6], "Seventh param should be Duration (analysisTime)");
      assertEquals(
          boolean.class, paramTypes[7], "Eighth param should be boolean (resolutionSuccessful)");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have 8 declared fields")
    void shouldHave8DeclaredFields() {
      long fieldCount =
          Arrays.stream(DependencyResolution.class.getDeclaredFields())
              .filter(f -> !f.isSynthetic())
              .count();
      assertEquals(8, fieldCount, "DependencyResolution should have 8 declared fields");
    }

    @Test
    @DisplayName("all fields should be private")
    void allFieldsShouldBePrivate() {
      Arrays.stream(DependencyResolution.class.getDeclaredFields())
          .filter(f -> !f.isSynthetic())
          .forEach(
              f ->
                  assertTrue(
                      Modifier.isPrivate(f.getModifiers()),
                      "Field " + f.getName() + " should be private"));
    }

    @Test
    @DisplayName("all fields should be final")
    void allFieldsShouldBeFinal() {
      Arrays.stream(DependencyResolution.class.getDeclaredFields())
          .filter(f -> !f.isSynthetic())
          .forEach(
              f ->
                  assertTrue(
                      Modifier.isFinal(f.getModifiers()),
                      "Field " + f.getName() + " should be final"));
    }
  }

  // ========================================================================
  // Accessor Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getInstantiationOrder method")
    void shouldHaveGetInstantiationOrderMethod() throws NoSuchMethodException {
      Method method = DependencyResolution.class.getMethod("getInstantiationOrder");
      assertNotNull(method, "getInstantiationOrder method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getDependencies method")
    void shouldHaveGetDependenciesMethod() throws NoSuchMethodException {
      Method method = DependencyResolution.class.getMethod("getDependencies");
      assertNotNull(method, "getDependencies method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have hasCircularDependencies method")
    void shouldHaveHasCircularDependenciesMethod() throws NoSuchMethodException {
      Method method = DependencyResolution.class.getMethod("hasCircularDependencies");
      assertNotNull(method, "hasCircularDependencies method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getCircularDependencyChains method")
    void shouldHaveGetCircularDependencyChainsMethod() throws NoSuchMethodException {
      Method method = DependencyResolution.class.getMethod("getCircularDependencyChains");
      assertNotNull(method, "getCircularDependencyChains method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getTotalModules method")
    void shouldHaveGetTotalModulesMethod() throws NoSuchMethodException {
      Method method = DependencyResolution.class.getMethod("getTotalModules");
      assertNotNull(method, "getTotalModules method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getResolvedDependencies method")
    void shouldHaveGetResolvedDependenciesMethod() throws NoSuchMethodException {
      Method method = DependencyResolution.class.getMethod("getResolvedDependencies");
      assertNotNull(method, "getResolvedDependencies method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getAnalysisTime method")
    void shouldHaveGetAnalysisTimeMethod() throws NoSuchMethodException {
      Method method = DependencyResolution.class.getMethod("getAnalysisTime");
      assertNotNull(method, "getAnalysisTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have isResolutionSuccessful method")
    void shouldHaveIsResolutionSuccessfulMethod() throws NoSuchMethodException {
      Method method = DependencyResolution.class.getMethod("isResolutionSuccessful");
      assertNotNull(method, "isResolutionSuccessful method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  // ========================================================================
  // Computed Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Computed Method Tests")
  class ComputedMethodTests {

    @Test
    @DisplayName("should have getResolutionRate method")
    void shouldHaveGetResolutionRateMethod() throws NoSuchMethodException {
      Method method = DependencyResolution.class.getMethod("getResolutionRate");
      assertNotNull(method, "getResolutionRate method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
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
      Method method = DependencyResolution.class.getMethod("toString");
      assertEquals(
          DependencyResolution.class,
          method.getDeclaringClass(),
          "toString should be declared in DependencyResolution");
    }

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      Method method = DependencyResolution.class.getMethod("equals", Object.class);
      assertEquals(
          DependencyResolution.class,
          method.getDeclaringClass(),
          "equals should be declared in DependencyResolution");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      Method method = DependencyResolution.class.getMethod("hashCode");
      assertEquals(
          DependencyResolution.class,
          method.getDeclaringClass(),
          "hashCode should be declared in DependencyResolution");
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
              "getInstantiationOrder",
              "getDependencies",
              "hasCircularDependencies",
              "getCircularDependencyChains",
              "getTotalModules",
              "getResolvedDependencies",
              "getAnalysisTime",
              "isResolutionSuccessful",
              "getResolutionRate",
              "toString",
              "equals",
              "hashCode");

      Set<String> actualMethods =
          Arrays.stream(DependencyResolution.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected),
            "DependencyResolution should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 12 declared methods")
    void shouldHaveExactly12DeclaredMethods() {
      long methodCount =
          Arrays.stream(DependencyResolution.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(12, methodCount, "DependencyResolution should have exactly 12 declared methods");
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
          DependencyResolution.class.getSuperclass(),
          "DependencyResolution should extend Object directly");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          DependencyResolution.class.getInterfaces().length,
          "DependencyResolution should not implement any interfaces");
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
          DependencyResolution.class.getDeclaredClasses().length,
          "DependencyResolution should have no nested classes");
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
          Arrays.stream(DependencyResolution.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "DependencyResolution should have no static methods");
    }
  }
}

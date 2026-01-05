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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the DependencyEdge class.
 *
 * <p>DependencyEdge represents a dependency relationship between two WebAssembly modules.
 */
@DisplayName("DependencyEdge Class Tests")
class DependencyEdgeTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(DependencyEdge.class.isInterface(), "DependencyEdge should not be an interface");
      assertFalse(DependencyEdge.class.isEnum(), "DependencyEdge should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(DependencyEdge.class.getModifiers()),
          "DependencyEdge should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(DependencyEdge.class.getModifiers()), "DependencyEdge should be final");
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
          DependencyEdge.class.getDeclaredConstructors().length,
          "DependencyEdge should have exactly 1 constructor");
    }

    @Test
    @DisplayName("constructor should be public")
    void constructorShouldBePublic() {
      Constructor<?>[] constructors = DependencyEdge.class.getDeclaredConstructors();
      assertTrue(Modifier.isPublic(constructors[0].getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("constructor should have 6 parameters")
    void constructorShouldHave6Parameters() {
      Constructor<?>[] constructors = DependencyEdge.class.getDeclaredConstructors();
      assertEquals(6, constructors[0].getParameterCount(), "Constructor should have 6 parameters");
    }

    @Test
    @DisplayName("constructor parameters should have correct types")
    void constructorParametersShouldHaveCorrectTypes() {
      Constructor<?>[] constructors = DependencyEdge.class.getDeclaredConstructors();
      Class<?>[] paramTypes = constructors[0].getParameterTypes();
      assertEquals(Module.class, paramTypes[0], "First param should be Module (dependent)");
      assertEquals(Module.class, paramTypes[1], "Second param should be Module (dependency)");
      assertEquals(String.class, paramTypes[2], "Third param should be String (importModule)");
      assertEquals(String.class, paramTypes[3], "Fourth param should be String (importName)");
      assertEquals(
          DependencyEdge.DependencyType.class,
          paramTypes[4],
          "Fifth param should be DependencyType");
      assertEquals(boolean.class, paramTypes[5], "Sixth param should be boolean (resolved)");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have 6 declared fields")
    void shouldHave6DeclaredFields() {
      long fieldCount =
          Arrays.stream(DependencyEdge.class.getDeclaredFields())
              .filter(f -> !f.isSynthetic())
              .count();
      assertEquals(6, fieldCount, "DependencyEdge should have 6 declared fields");
    }

    @Test
    @DisplayName("all fields should be private")
    void allFieldsShouldBePrivate() {
      Arrays.stream(DependencyEdge.class.getDeclaredFields())
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
      Arrays.stream(DependencyEdge.class.getDeclaredFields())
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
    @DisplayName("should have getDependent method")
    void shouldHaveGetDependentMethod() throws NoSuchMethodException {
      Method method = DependencyEdge.class.getMethod("getDependent");
      assertNotNull(method, "getDependent method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("should have getDependency method")
    void shouldHaveGetDependencyMethod() throws NoSuchMethodException {
      Method method = DependencyEdge.class.getMethod("getDependency");
      assertNotNull(method, "getDependency method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("should have getImportModule method")
    void shouldHaveGetImportModuleMethod() throws NoSuchMethodException {
      Method method = DependencyEdge.class.getMethod("getImportModule");
      assertNotNull(method, "getImportModule method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getImportName method")
    void shouldHaveGetImportNameMethod() throws NoSuchMethodException {
      Method method = DependencyEdge.class.getMethod("getImportName");
      assertNotNull(method, "getImportName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getDependencyType method")
    void shouldHaveGetDependencyTypeMethod() throws NoSuchMethodException {
      Method method = DependencyEdge.class.getMethod("getDependencyType");
      assertNotNull(method, "getDependencyType method should exist");
      assertEquals(
          DependencyEdge.DependencyType.class,
          method.getReturnType(),
          "Should return DependencyType");
    }

    @Test
    @DisplayName("should have isResolved method")
    void shouldHaveIsResolvedMethod() throws NoSuchMethodException {
      Method method = DependencyEdge.class.getMethod("isResolved");
      assertNotNull(method, "isResolved method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getDependencyString method")
    void shouldHaveGetDependencyStringMethod() throws NoSuchMethodException {
      Method method = DependencyEdge.class.getMethod("getDependencyString");
      assertNotNull(method, "getDependencyString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
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
      Method method = DependencyEdge.class.getMethod("toString");
      assertEquals(
          DependencyEdge.class,
          method.getDeclaringClass(),
          "toString should be declared in DependencyEdge");
    }

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      Method method = DependencyEdge.class.getMethod("equals", Object.class);
      assertEquals(
          DependencyEdge.class,
          method.getDeclaringClass(),
          "equals should be declared in DependencyEdge");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      Method method = DependencyEdge.class.getMethod("hashCode");
      assertEquals(
          DependencyEdge.class,
          method.getDeclaringClass(),
          "hashCode should be declared in DependencyEdge");
    }
  }

  // ========================================================================
  // Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Enum Tests")
  class NestedEnumTests {

    @Test
    @DisplayName("should have DependencyType nested enum")
    void shouldHaveDependencyTypeNestedEnum() {
      Class<?>[] declaredClasses = DependencyEdge.class.getDeclaredClasses();
      assertEquals(1, declaredClasses.length, "DependencyEdge should have 1 nested class");
      assertEquals(
          "DependencyType",
          declaredClasses[0].getSimpleName(),
          "Nested class should be DependencyType");
      assertTrue(declaredClasses[0].isEnum(), "DependencyType should be an enum");
    }

    @Test
    @DisplayName("DependencyType should have FUNCTION constant")
    void dependencyTypeShouldHaveFunctionConstant() {
      assertNotNull(DependencyEdge.DependencyType.FUNCTION, "FUNCTION constant should exist");
    }

    @Test
    @DisplayName("DependencyType should have MEMORY constant")
    void dependencyTypeShouldHaveMemoryConstant() {
      assertNotNull(DependencyEdge.DependencyType.MEMORY, "MEMORY constant should exist");
    }

    @Test
    @DisplayName("DependencyType should have TABLE constant")
    void dependencyTypeShouldHaveTableConstant() {
      assertNotNull(DependencyEdge.DependencyType.TABLE, "TABLE constant should exist");
    }

    @Test
    @DisplayName("DependencyType should have GLOBAL constant")
    void dependencyTypeShouldHaveGlobalConstant() {
      assertNotNull(DependencyEdge.DependencyType.GLOBAL, "GLOBAL constant should exist");
    }

    @Test
    @DisplayName("DependencyType should have INSTANCE constant")
    void dependencyTypeShouldHaveInstanceConstant() {
      assertNotNull(DependencyEdge.DependencyType.INSTANCE, "INSTANCE constant should exist");
    }

    @Test
    @DisplayName("DependencyType should have exactly 5 constants")
    void dependencyTypeShouldHave5Constants() {
      assertEquals(
          5,
          DependencyEdge.DependencyType.values().length,
          "DependencyType should have exactly 5 constants");
    }

    @Test
    @DisplayName("DependencyType should have all expected constants")
    void dependencyTypeShouldHaveAllExpectedConstants() {
      Set<String> expectedConstants = Set.of("FUNCTION", "MEMORY", "TABLE", "GLOBAL", "INSTANCE");

      Set<String> actualConstants =
          Arrays.stream(DependencyEdge.DependencyType.values())
              .map(Enum::name)
              .collect(Collectors.toSet());

      assertEquals(expectedConstants, actualConstants, "Should have all expected enum constants");
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
              "getDependent",
              "getDependency",
              "getImportModule",
              "getImportName",
              "getDependencyType",
              "isResolved",
              "getDependencyString",
              "toString",
              "equals",
              "hashCode");

      Set<String> actualMethods =
          Arrays.stream(DependencyEdge.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "DependencyEdge should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 10 declared methods")
    void shouldHaveExactly10DeclaredMethods() {
      long methodCount =
          Arrays.stream(DependencyEdge.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(10, methodCount, "DependencyEdge should have exactly 10 declared methods");
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
          DependencyEdge.class.getSuperclass(),
          "DependencyEdge should extend Object directly");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          DependencyEdge.class.getInterfaces().length,
          "DependencyEdge should not implement any interfaces");
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
          Arrays.stream(DependencyEdge.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "DependencyEdge should have no static methods");
    }
  }
}

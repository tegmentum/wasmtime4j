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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the StoreLimits class.
 *
 * <p>StoreLimits provides configuration for resource limits on WebAssembly stores. This test
 * verifies the class structure, builder pattern, and method signatures.
 */
@DisplayName("StoreLimits Class Tests")
class StoreLimitsTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertTrue(!StoreLimits.class.isInterface(), "StoreLimits should be a class");
      assertTrue(!StoreLimits.class.isEnum(), "StoreLimits should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(StoreLimits.class.getModifiers()), "StoreLimits should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(Modifier.isFinal(StoreLimits.class.getModifiers()), "StoreLimits should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() {
      Constructor<?>[] constructors = StoreLimits.class.getDeclaredConstructors();
      boolean hasPrivateConstructor = false;
      for (Constructor<?> constructor : constructors) {
        if (Modifier.isPrivate(constructor.getModifiers())) {
          hasPrivateConstructor = true;
          break;
        }
      }
      assertTrue(hasPrivateConstructor, "StoreLimits should have a private constructor");
    }

    @Test
    @DisplayName("should have exactly one declared constructor")
    void shouldHaveExactlyOneDeclaredConstructor() {
      assertEquals(
          1,
          StoreLimits.class.getDeclaredConstructors().length,
          "StoreLimits should have exactly 1 constructor");
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final Method method = StoreLimits.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "builder should be public");
    }

    @Test
    @DisplayName("builder method should return Builder")
    void builderMethodShouldReturnBuilder() throws NoSuchMethodException {
      final Method method = StoreLimits.class.getMethod("builder");
      assertTrue(
          method.getReturnType().getName().contains("Builder"),
          "builder should return Builder type");
    }
  }

  // ========================================================================
  // Accessor Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("should have getMemorySize method")
    void shouldHaveGetMemorySizeMethod() throws NoSuchMethodException {
      final Method method = StoreLimits.class.getMethod("getMemorySize");
      assertNotNull(method, "getMemorySize method should exist");
      assertEquals(long.class, method.getReturnType(), "getMemorySize should return long");
    }

    @Test
    @DisplayName("should have getTableElements method")
    void shouldHaveGetTableElementsMethod() throws NoSuchMethodException {
      final Method method = StoreLimits.class.getMethod("getTableElements");
      assertNotNull(method, "getTableElements method should exist");
      assertEquals(long.class, method.getReturnType(), "getTableElements should return long");
    }

    @Test
    @DisplayName("should have getInstances method")
    void shouldHaveGetInstancesMethod() throws NoSuchMethodException {
      final Method method = StoreLimits.class.getMethod("getInstances");
      assertNotNull(method, "getInstances method should exist");
      assertEquals(long.class, method.getReturnType(), "getInstances should return long");
    }

    @Test
    @DisplayName("all accessor methods should have no parameters")
    void allAccessorMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          StoreLimits.class.getMethod("getMemorySize").getParameterCount(),
          "getMemorySize should have 0 params");
      assertEquals(
          0,
          StoreLimits.class.getMethod("getTableElements").getParameterCount(),
          "getTableElements should have 0 params");
      assertEquals(
          0,
          StoreLimits.class.getMethod("getInstances").getParameterCount(),
          "getInstances should have 0 params");
    }
  }

  // ========================================================================
  // Nested Builder Class Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Builder Class Tests")
  class NestedBuilderClassTests {

    @Test
    @DisplayName("should have public static final nested Builder class")
    void shouldHavePublicStaticFinalNestedBuilderClass() {
      Class<?>[] declaredClasses = StoreLimits.class.getDeclaredClasses();
      boolean hasBuilder = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Builder")) {
          hasBuilder = true;
          assertTrue(Modifier.isPublic(clazz.getModifiers()), "Builder should be public");
          assertTrue(Modifier.isStatic(clazz.getModifiers()), "Builder should be static");
          assertTrue(Modifier.isFinal(clazz.getModifiers()), "Builder should be final");
          break;
        }
      }
      assertTrue(hasBuilder, "StoreLimits should have a nested Builder class");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      Class<?>[] declaredClasses = StoreLimits.class.getDeclaredClasses();
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Builder")) {
          Method buildMethod = clazz.getMethod("build");
          assertNotNull(buildMethod, "Builder should have build method");
          assertEquals(
              StoreLimits.class, buildMethod.getReturnType(), "build should return StoreLimits");
        }
      }
    }

    @Test
    @DisplayName("Builder should have fluent setter methods")
    void builderShouldHaveFluentSetterMethods() {
      Class<?>[] declaredClasses = StoreLimits.class.getDeclaredClasses();
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Builder")) {
          Set<String> expectedMethods = Set.of("memorySize", "tableElements", "instances");
          Set<String> actualMethods =
              Arrays.stream(clazz.getDeclaredMethods())
                  .map(Method::getName)
                  .collect(Collectors.toSet());
          for (String expected : expectedMethods) {
            assertTrue(actualMethods.contains(expected), "Builder should have method: " + expected);
          }
        }
      }
    }

    @Test
    @DisplayName("Builder fluent setters should return Builder")
    void builderFluentSettersShouldReturnBuilder() {
      Class<?>[] declaredClasses = StoreLimits.class.getDeclaredClasses();
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Builder")) {
          Set<String> fluentMethods = Set.of("memorySize", "tableElements", "instances");
          for (Method method : clazz.getDeclaredMethods()) {
            if (fluentMethods.contains(method.getName())
                && Modifier.isPublic(method.getModifiers())) {
              assertEquals(
                  clazz,
                  method.getReturnType(),
                  method.getName() + " should return Builder for fluent API");
            }
          }
        }
      }
    }

    @Test
    @DisplayName("Builder fluent setters should accept long parameters")
    void builderFluentSettersShouldAcceptLongParameters() {
      Class<?>[] declaredClasses = StoreLimits.class.getDeclaredClasses();
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Builder")) {
          Set<String> fluentMethods = Set.of("memorySize", "tableElements", "instances");
          for (Method method : clazz.getDeclaredMethods()) {
            if (fluentMethods.contains(method.getName())
                && Modifier.isPublic(method.getModifiers())) {
              Class<?>[] paramTypes = method.getParameterTypes();
              assertEquals(1, paramTypes.length, method.getName() + " should have 1 parameter");
              assertEquals(
                  long.class, paramTypes[0], method.getName() + " parameter should be long");
            }
          }
        }
      }
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
          Set.of("getMemorySize", "getTableElements", "getInstances", "builder");

      Set<String> actualMethods =
          Arrays.stream(StoreLimits.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "StoreLimits should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 4 declared methods")
    void shouldHaveAtLeast4DeclaredMethods() {
      assertTrue(
          StoreLimits.class.getDeclaredMethods().length >= 4,
          "StoreLimits should have at least 4 methods");
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
      Set<String> expectedFields = Set.of("memorySize", "tableElements", "instances");

      for (String fieldName : expectedFields) {
        try {
          Field field = StoreLimits.class.getDeclaredField(fieldName);
          assertTrue(
              Modifier.isPrivate(field.getModifiers()), fieldName + " field should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), fieldName + " field should be final");
        } catch (NoSuchFieldException e) {
          assertTrue(false, "Field " + fieldName + " should exist");
        }
      }
    }

    @Test
    @DisplayName("memorySize field should be of type long")
    void memorySizeFieldShouldBeOfTypeLong() throws NoSuchFieldException {
      Field field = StoreLimits.class.getDeclaredField("memorySize");
      assertEquals(long.class, field.getType(), "memorySize field should be of type long");
    }

    @Test
    @DisplayName("tableElements field should be of type long")
    void tableElementsFieldShouldBeOfTypeLong() throws NoSuchFieldException {
      Field field = StoreLimits.class.getDeclaredField("tableElements");
      assertEquals(long.class, field.getType(), "tableElements field should be of type long");
    }

    @Test
    @DisplayName("instances field should be of type long")
    void instancesFieldShouldBeOfTypeLong() throws NoSuchFieldException {
      Field field = StoreLimits.class.getDeclaredField("instances");
      assertEquals(long.class, field.getType(), "instances field should be of type long");
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
          Object.class, StoreLimits.class.getSuperclass(), "StoreLimits should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          StoreLimits.class.getInterfaces().length,
          "StoreLimits should not implement any interfaces");
    }
  }

  // ========================================================================
  // Nested Class Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Class Count Tests")
  class NestedClassCountTests {

    @Test
    @DisplayName("should have exactly 1 nested class")
    void shouldHaveExactly1NestedClass() {
      assertEquals(
          1,
          StoreLimits.class.getDeclaredClasses().length,
          "StoreLimits should have exactly 1 nested class (Builder)");
    }
  }
}

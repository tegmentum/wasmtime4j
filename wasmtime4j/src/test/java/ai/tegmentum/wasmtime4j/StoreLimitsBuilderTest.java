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
 * Comprehensive test suite for the StoreLimitsBuilder class.
 *
 * <p>StoreLimitsBuilder provides a fluent API for creating StoreLimits instances with custom
 * resource limits. This test verifies the class structure and method signatures.
 */
@DisplayName("StoreLimitsBuilder Class Tests")
class StoreLimitsBuilderTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertTrue(!StoreLimitsBuilder.class.isInterface(), "StoreLimitsBuilder should be a class");
      assertTrue(!StoreLimitsBuilder.class.isEnum(), "StoreLimitsBuilder should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(StoreLimitsBuilder.class.getModifiers()),
          "StoreLimitsBuilder should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(StoreLimitsBuilder.class.getModifiers()),
          "StoreLimitsBuilder should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public no-arg constructor")
    void shouldHavePublicNoArgConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = StoreLimitsBuilder.class.getConstructor();
      assertNotNull(constructor, "No-arg constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have exactly one declared constructor")
    void shouldHaveExactlyOneDeclaredConstructor() {
      assertEquals(
          1,
          StoreLimitsBuilder.class.getDeclaredConstructors().length,
          "StoreLimitsBuilder should have exactly 1 constructor");
    }
  }

  // ========================================================================
  // Fluent Builder Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Fluent Builder Methods Tests")
  class FluentBuilderMethodsTests {

    @Test
    @DisplayName("should have memorySize method")
    void shouldHaveMemorySizeMethod() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("memorySize", long.class);
      assertNotNull(method, "memorySize method should exist");
      assertEquals(
          StoreLimitsBuilder.class,
          method.getReturnType(),
          "memorySize should return StoreLimitsBuilder");
    }

    @Test
    @DisplayName("should have tableElements method")
    void shouldHaveTableElementsMethod() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("tableElements", long.class);
      assertNotNull(method, "tableElements method should exist");
      assertEquals(
          StoreLimitsBuilder.class,
          method.getReturnType(),
          "tableElements should return StoreLimitsBuilder");
    }

    @Test
    @DisplayName("should have instances method")
    void shouldHaveInstancesMethod() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("instances", long.class);
      assertNotNull(method, "instances method should exist");
      assertEquals(
          StoreLimitsBuilder.class,
          method.getReturnType(),
          "instances should return StoreLimitsBuilder");
    }

    @Test
    @DisplayName("should have tables method")
    void shouldHaveTablesMethod() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("tables", long.class);
      assertNotNull(method, "tables method should exist");
      assertEquals(
          StoreLimitsBuilder.class,
          method.getReturnType(),
          "tables should return StoreLimitsBuilder");
    }

    @Test
    @DisplayName("should have memories method")
    void shouldHaveMemoriesMethod() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("memories", long.class);
      assertNotNull(method, "memories method should exist");
      assertEquals(
          StoreLimitsBuilder.class,
          method.getReturnType(),
          "memories should return StoreLimitsBuilder");
    }

    @Test
    @DisplayName("all fluent methods should return StoreLimitsBuilder for chaining")
    void allFluentMethodsShouldReturnStoreLimitsBuilder() {
      Set<String> fluentMethods =
          Set.of("memorySize", "tableElements", "instances", "tables", "memories");

      for (Method method : StoreLimitsBuilder.class.getDeclaredMethods()) {
        if (fluentMethods.contains(method.getName()) && Modifier.isPublic(method.getModifiers())) {
          assertEquals(
              StoreLimitsBuilder.class,
              method.getReturnType(),
              method.getName() + " should return StoreLimitsBuilder for fluent API");
        }
      }
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
      final Method method = StoreLimitsBuilder.class.getMethod("getMemorySize");
      assertNotNull(method, "getMemorySize method should exist");
      assertEquals(long.class, method.getReturnType(), "getMemorySize should return long");
    }

    @Test
    @DisplayName("should have getTableElements method")
    void shouldHaveGetTableElementsMethod() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("getTableElements");
      assertNotNull(method, "getTableElements method should exist");
      assertEquals(long.class, method.getReturnType(), "getTableElements should return long");
    }

    @Test
    @DisplayName("should have getInstances method")
    void shouldHaveGetInstancesMethod() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("getInstances");
      assertNotNull(method, "getInstances method should exist");
      assertEquals(long.class, method.getReturnType(), "getInstances should return long");
    }

    @Test
    @DisplayName("should have getTables method")
    void shouldHaveGetTablesMethod() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("getTables");
      assertNotNull(method, "getTables method should exist");
      assertEquals(long.class, method.getReturnType(), "getTables should return long");
    }

    @Test
    @DisplayName("should have getMemories method")
    void shouldHaveGetMemoriesMethod() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("getMemories");
      assertNotNull(method, "getMemories method should exist");
      assertEquals(long.class, method.getReturnType(), "getMemories should return long");
    }

    @Test
    @DisplayName("all accessor methods should have no parameters")
    void allAccessorMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          StoreLimitsBuilder.class.getMethod("getMemorySize").getParameterCount(),
          "getMemorySize should have 0 params");
      assertEquals(
          0,
          StoreLimitsBuilder.class.getMethod("getTableElements").getParameterCount(),
          "getTableElements should have 0 params");
      assertEquals(
          0,
          StoreLimitsBuilder.class.getMethod("getInstances").getParameterCount(),
          "getInstances should have 0 params");
      assertEquals(
          0,
          StoreLimitsBuilder.class.getMethod("getTables").getParameterCount(),
          "getTables should have 0 params");
      assertEquals(
          0,
          StoreLimitsBuilder.class.getMethod("getMemories").getParameterCount(),
          "getMemories should have 0 params");
    }
  }

  // ========================================================================
  // Build Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Build Method Tests")
  class BuildMethodTests {

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(StoreLimits.class, method.getReturnType(), "build should return StoreLimits");
    }

    @Test
    @DisplayName("build method should be public")
    void buildMethodShouldBePublic() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("build");
      assertTrue(Modifier.isPublic(method.getModifiers()), "build should be public");
    }

    @Test
    @DisplayName("build method should have no parameters")
    void buildMethodShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("build");
      assertEquals(0, method.getParameterCount(), "build should have no parameters");
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
      final Method method = StoreLimitsBuilder.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          StoreLimitsBuilder.class,
          method.getDeclaringClass(),
          "toString should be declared in StoreLimitsBuilder");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("memorySize should accept long parameter")
    void memorySizeShouldAcceptLongParameter() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("memorySize", long.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "memorySize should have 1 parameter");
      assertEquals(long.class, paramTypes[0], "memorySize parameter should be long");
    }

    @Test
    @DisplayName("tableElements should accept long parameter")
    void tableElementsShouldAcceptLongParameter() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("tableElements", long.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "tableElements should have 1 parameter");
      assertEquals(long.class, paramTypes[0], "tableElements parameter should be long");
    }

    @Test
    @DisplayName("instances should accept long parameter")
    void instancesShouldAcceptLongParameter() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("instances", long.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "instances should have 1 parameter");
      assertEquals(long.class, paramTypes[0], "instances parameter should be long");
    }

    @Test
    @DisplayName("tables should accept long parameter")
    void tablesShouldAcceptLongParameter() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("tables", long.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "tables should have 1 parameter");
      assertEquals(long.class, paramTypes[0], "tables parameter should be long");
    }

    @Test
    @DisplayName("memories should accept long parameter")
    void memoriesShouldAcceptLongParameter() throws NoSuchMethodException {
      final Method method = StoreLimitsBuilder.class.getMethod("memories", long.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "memories should have 1 parameter");
      assertEquals(long.class, paramTypes[0], "memories parameter should be long");
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
              "memorySize",
              "tableElements",
              "instances",
              "tables",
              "memories",
              "getMemorySize",
              "getTableElements",
              "getInstances",
              "getTables",
              "getMemories",
              "build",
              "toString");

      Set<String> actualMethods =
          Arrays.stream(StoreLimitsBuilder.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "StoreLimitsBuilder should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have correct number of public methods")
    void shouldHaveCorrectNumberOfPublicMethods() {
      long publicMethodCount =
          Arrays.stream(StoreLimitsBuilder.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      // 5 setters + 5 getters + 1 build + 1 toString = 12
      assertEquals(12, publicMethodCount, "StoreLimitsBuilder should have 12 public methods");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have private fields")
    void shouldHavePrivateFields() {
      Set<String> expectedFields =
          Set.of("memorySize", "tableElements", "instances", "tables", "memories");

      for (String fieldName : expectedFields) {
        try {
          Field field = StoreLimitsBuilder.class.getDeclaredField(fieldName);
          assertTrue(
              Modifier.isPrivate(field.getModifiers()), fieldName + " field should be private");
        } catch (NoSuchFieldException e) {
          assertTrue(false, "Field " + fieldName + " should exist");
        }
      }
    }

    @Test
    @DisplayName("all limit fields should be of type long")
    void allLimitFieldsShouldBeOfTypeLong() {
      Set<String> expectedFields =
          Set.of("memorySize", "tableElements", "instances", "tables", "memories");

      for (String fieldName : expectedFields) {
        try {
          Field field = StoreLimitsBuilder.class.getDeclaredField(fieldName);
          assertEquals(long.class, field.getType(), fieldName + " field should be of type long");
        } catch (NoSuchFieldException e) {
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
          StoreLimitsBuilder.class.getSuperclass(),
          "StoreLimitsBuilder should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          StoreLimitsBuilder.class.getInterfaces().length,
          "StoreLimitsBuilder should not implement any interfaces");
    }
  }
}

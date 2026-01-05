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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the ConfigProperties class.
 *
 * <p>ConfigProperties provides a type-safe way to access configuration properties with default
 * values, type conversion, and validation. This test verifies the class structure and methods.
 */
@DisplayName("ConfigProperties Class Tests")
class ConfigPropertiesTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(ConfigProperties.class.getModifiers()),
          "ConfigProperties should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ConfigProperties.class.getModifiers()),
          "ConfigProperties should be public");
    }

    @Test
    @DisplayName("should not be abstract")
    void shouldNotBeAbstract() {
      assertTrue(
          !Modifier.isAbstract(ConfigProperties.class.getModifiers()),
          "ConfigProperties should not be abstract");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have Map constructor")
    void shouldHaveMapConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = ConfigProperties.class.getConstructor(Map.class);
      assertNotNull(constructor, "Map constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Map constructor should be public");
    }

    @Test
    @DisplayName("should have Properties constructor")
    void shouldHavePropertiesConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = ConfigProperties.class.getConstructor(Properties.class);
      assertNotNull(constructor, "Properties constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "Properties constructor should be public");
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have empty factory method")
    void shouldHaveEmptyFactoryMethod() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("empty");
      assertNotNull(method, "empty method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "empty should be static");
      assertEquals(
          ConfigProperties.class, method.getReturnType(), "empty should return ConfigProperties");
    }

    @Test
    @DisplayName("should have fromSystemProperties factory method")
    void shouldHaveFromSystemPropertiesFactoryMethod() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("fromSystemProperties", String.class);
      assertNotNull(method, "fromSystemProperties method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromSystemProperties should be static");
      assertEquals(
          ConfigProperties.class,
          method.getReturnType(),
          "fromSystemProperties should return ConfigProperties");
    }
  }

  // ========================================================================
  // String Getter Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("String Getter Methods Tests")
  class StringGetterMethodsTests {

    @Test
    @DisplayName("should have getString method returning Optional")
    void shouldHaveGetStringMethodReturningOptional() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("getString", String.class);
      assertNotNull(method, "getString method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getString should return Optional");
    }

    @Test
    @DisplayName("should have getString method with default")
    void shouldHaveGetStringMethodWithDefault() throws NoSuchMethodException {
      final Method method =
          ConfigProperties.class.getMethod("getString", String.class, String.class);
      assertNotNull(method, "getString with default method should exist");
      assertEquals(String.class, method.getReturnType(), "getString should return String");
    }
  }

  // ========================================================================
  // Integer Getter Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Integer Getter Methods Tests")
  class IntegerGetterMethodsTests {

    @Test
    @DisplayName("should have getInt method returning Optional")
    void shouldHaveGetIntMethodReturningOptional() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("getInt", String.class);
      assertNotNull(method, "getInt method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getInt should return Optional");
    }

    @Test
    @DisplayName("should have getInt method with default")
    void shouldHaveGetIntMethodWithDefault() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("getInt", String.class, int.class);
      assertNotNull(method, "getInt with default method should exist");
      assertEquals(int.class, method.getReturnType(), "getInt should return int");
    }
  }

  // ========================================================================
  // Long Getter Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Long Getter Methods Tests")
  class LongGetterMethodsTests {

    @Test
    @DisplayName("should have getLong method returning Optional")
    void shouldHaveGetLongMethodReturningOptional() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("getLong", String.class);
      assertNotNull(method, "getLong method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getLong should return Optional");
    }

    @Test
    @DisplayName("should have getLong method with default")
    void shouldHaveGetLongMethodWithDefault() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("getLong", String.class, long.class);
      assertNotNull(method, "getLong with default method should exist");
      assertEquals(long.class, method.getReturnType(), "getLong should return long");
    }
  }

  // ========================================================================
  // Double Getter Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Double Getter Methods Tests")
  class DoubleGetterMethodsTests {

    @Test
    @DisplayName("should have getDouble method returning Optional")
    void shouldHaveGetDoubleMethodReturningOptional() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("getDouble", String.class);
      assertNotNull(method, "getDouble method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getDouble should return Optional");
    }

    @Test
    @DisplayName("should have getDouble method with default")
    void shouldHaveGetDoubleMethodWithDefault() throws NoSuchMethodException {
      final Method method =
          ConfigProperties.class.getMethod("getDouble", String.class, double.class);
      assertNotNull(method, "getDouble with default method should exist");
      assertEquals(double.class, method.getReturnType(), "getDouble should return double");
    }
  }

  // ========================================================================
  // Boolean Getter Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Boolean Getter Methods Tests")
  class BooleanGetterMethodsTests {

    @Test
    @DisplayName("should have getBoolean method returning Optional")
    void shouldHaveGetBooleanMethodReturningOptional() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("getBoolean", String.class);
      assertNotNull(method, "getBoolean method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getBoolean should return Optional");
    }

    @Test
    @DisplayName("should have getBoolean method with default")
    void shouldHaveGetBooleanMethodWithDefault() throws NoSuchMethodException {
      final Method method =
          ConfigProperties.class.getMethod("getBoolean", String.class, boolean.class);
      assertNotNull(method, "getBoolean with default method should exist");
      assertEquals(boolean.class, method.getReturnType(), "getBoolean should return boolean");
    }
  }

  // ========================================================================
  // Utility Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Utility Methods Tests")
  class UtilityMethodsTests {

    @Test
    @DisplayName("should have containsKey method")
    void shouldHaveContainsKeyMethod() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("containsKey", String.class);
      assertNotNull(method, "containsKey method should exist");
      assertEquals(boolean.class, method.getReturnType(), "containsKey should return boolean");
    }

    @Test
    @DisplayName("should have keySet method")
    void shouldHaveKeySetMethod() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("keySet");
      assertNotNull(method, "keySet method should exist");
      assertEquals(Set.class, method.getReturnType(), "keySet should return Set");
    }

    @Test
    @DisplayName("should have size method")
    void shouldHaveSizeMethod() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertEquals(int.class, method.getReturnType(), "size should return int");
    }

    @Test
    @DisplayName("should have isEmpty method")
    void shouldHaveIsEmptyMethod() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("isEmpty");
      assertNotNull(method, "isEmpty method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isEmpty should return boolean");
    }

    @Test
    @DisplayName("should have toMap method")
    void shouldHaveToMapMethod() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("toMap");
      assertNotNull(method, "toMap method should exist");
      assertEquals(Map.class, method.getReturnType(), "toMap should return Map");
    }
  }

  // ========================================================================
  // Immutable Modification Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Immutable Modification Methods Tests")
  class ImmutableModificationMethodsTests {

    @Test
    @DisplayName("should have with method")
    void shouldHaveWithMethod() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("with", String.class, String.class);
      assertNotNull(method, "with method should exist");
      assertEquals(
          ConfigProperties.class, method.getReturnType(), "with should return ConfigProperties");
    }

    @Test
    @DisplayName("should have without method")
    void shouldHaveWithoutMethod() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("without", String.class);
      assertNotNull(method, "without method should exist");
      assertEquals(
          ConfigProperties.class, method.getReturnType(), "without should return ConfigProperties");
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
      final Method method = ConfigProperties.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
      assertEquals(
          ConfigProperties.class,
          method.getDeclaringClass(),
          "toString should be declared in ConfigProperties");
    }

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "equals should return boolean");
      assertEquals(
          ConfigProperties.class,
          method.getDeclaringClass(),
          "equals should be declared in ConfigProperties");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(int.class, method.getReturnType(), "hashCode should return int");
      assertEquals(
          ConfigProperties.class,
          method.getDeclaringClass(),
          "hashCode should be declared in ConfigProperties");
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
              "getString",
              "getInt",
              "getLong",
              "getDouble",
              "getBoolean",
              "containsKey",
              "keySet",
              "size",
              "isEmpty",
              "with",
              "without",
              "toMap",
              "empty",
              "fromSystemProperties",
              "toString",
              "equals",
              "hashCode");

      Set<String> actualMethods =
          Arrays.stream(ConfigProperties.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "ConfigProperties should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Generic Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Return Type Tests")
  class GenericReturnTypeTests {

    @Test
    @DisplayName("getString should return Optional<String>")
    void getStringShouldReturnOptionalOfString() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("getString", String.class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(String.class, typeArgs[0], "Type argument should be String");
    }

    @Test
    @DisplayName("getInt should return Optional<Integer>")
    void getIntShouldReturnOptionalOfInteger() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("getInt", String.class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(Integer.class, typeArgs[0], "Type argument should be Integer");
    }

    @Test
    @DisplayName("getLong should return Optional<Long>")
    void getLongShouldReturnOptionalOfLong() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("getLong", String.class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(Long.class, typeArgs[0], "Type argument should be Long");
    }

    @Test
    @DisplayName("getDouble should return Optional<Double>")
    void getDoubleShouldReturnOptionalOfDouble() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("getDouble", String.class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(Double.class, typeArgs[0], "Type argument should be Double");
    }

    @Test
    @DisplayName("getBoolean should return Optional<Boolean>")
    void getBooleanShouldReturnOptionalOfBoolean() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("getBoolean", String.class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(Boolean.class, typeArgs[0], "Type argument should be Boolean");
    }

    @Test
    @DisplayName("keySet should return Set<String>")
    void keySetShouldReturnSetOfString() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("keySet");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Set.class, paramType.getRawType(), "Raw type should be Set");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(String.class, typeArgs[0], "Type argument should be String");
    }

    @Test
    @DisplayName("toMap should return Map<String, String>")
    void toMapShouldReturnMapOfStringString() throws NoSuchMethodException {
      final Method method = ConfigProperties.class.getMethod("toMap");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Map.class, paramType.getRawType(), "Raw type should be Map");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(2, typeArgs.length, "Should have 2 type arguments");
      assertEquals(String.class, typeArgs[0], "First type argument should be String");
      assertEquals(String.class, typeArgs[1], "Second type argument should be String");
    }
  }
}

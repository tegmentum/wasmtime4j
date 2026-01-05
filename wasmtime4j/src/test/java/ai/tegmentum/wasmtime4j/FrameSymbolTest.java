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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the FrameSymbol class.
 *
 * <p>FrameSymbol provides debug symbol information for WebAssembly stack frames. This test verifies
 * the class structure and method signatures.
 */
@DisplayName("FrameSymbol Class Tests")
class FrameSymbolTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertTrue(!FrameSymbol.class.isInterface(), "FrameSymbol should be a class");
      assertTrue(!FrameSymbol.class.isEnum(), "FrameSymbol should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(FrameSymbol.class.getModifiers()), "FrameSymbol should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(Modifier.isFinal(FrameSymbol.class.getModifiers()), "FrameSymbol should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with 4 parameters")
    void shouldHavePublicConstructorWith4Parameters() throws NoSuchMethodException {
      Constructor<?> constructor =
          FrameSymbol.class.getConstructor(
              String.class, String.class, Integer.class, Integer.class);
      assertNotNull(constructor, "Constructor with 4 parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("constructor should have correct parameter types")
    void constructorShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      Constructor<?> constructor =
          FrameSymbol.class.getConstructor(
              String.class, String.class, Integer.class, Integer.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(4, paramTypes.length, "Constructor should have 4 parameters");
      assertEquals(String.class, paramTypes[0], "First parameter should be String (name)");
      assertEquals(String.class, paramTypes[1], "Second parameter should be String (file)");
      assertEquals(Integer.class, paramTypes[2], "Third parameter should be Integer (line)");
      assertEquals(Integer.class, paramTypes[3], "Fourth parameter should be Integer (column)");
    }

    @Test
    @DisplayName("should have exactly one declared constructor")
    void shouldHaveExactlyOneDeclaredConstructor() {
      assertEquals(
          1,
          FrameSymbol.class.getDeclaredConstructors().length,
          "FrameSymbol should have exactly 1 constructor");
    }
  }

  // ========================================================================
  // Accessor Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("should have getName method returning Optional")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = FrameSymbol.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getName should return Optional");
    }

    @Test
    @DisplayName("should have getFile method returning Optional")
    void shouldHaveGetFileMethod() throws NoSuchMethodException {
      final Method method = FrameSymbol.class.getMethod("getFile");
      assertNotNull(method, "getFile method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getFile should return Optional");
    }

    @Test
    @DisplayName("should have getLine method returning Optional")
    void shouldHaveGetLineMethod() throws NoSuchMethodException {
      final Method method = FrameSymbol.class.getMethod("getLine");
      assertNotNull(method, "getLine method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getLine should return Optional");
    }

    @Test
    @DisplayName("should have getColumn method returning Optional")
    void shouldHaveGetColumnMethod() throws NoSuchMethodException {
      final Method method = FrameSymbol.class.getMethod("getColumn");
      assertNotNull(method, "getColumn method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getColumn should return Optional");
    }

    @Test
    @DisplayName("all accessor methods should have no parameters")
    void allAccessorMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          FrameSymbol.class.getMethod("getName").getParameterCount(),
          "getName should have 0 params");
      assertEquals(
          0,
          FrameSymbol.class.getMethod("getFile").getParameterCount(),
          "getFile should have 0 params");
      assertEquals(
          0,
          FrameSymbol.class.getMethod("getLine").getParameterCount(),
          "getLine should have 0 params");
      assertEquals(
          0,
          FrameSymbol.class.getMethod("getColumn").getParameterCount(),
          "getColumn should have 0 params");
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
      final Method method = FrameSymbol.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          FrameSymbol.class,
          method.getDeclaringClass(),
          "toString should be declared in FrameSymbol");
    }

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      final Method method = FrameSymbol.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(
          FrameSymbol.class,
          method.getDeclaringClass(),
          "equals should be declared in FrameSymbol");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      final Method method = FrameSymbol.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          FrameSymbol.class,
          method.getDeclaringClass(),
          "hashCode should be declared in FrameSymbol");
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
          Set.of("getName", "getFile", "getLine", "getColumn", "toString", "equals", "hashCode");

      Set<String> actualMethods =
          Arrays.stream(FrameSymbol.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "FrameSymbol should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 7 declared methods")
    void shouldHaveAtLeast7DeclaredMethods() {
      assertTrue(
          FrameSymbol.class.getDeclaredMethods().length >= 7,
          "FrameSymbol should have at least 7 methods");
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
      Set<String> expectedFields = Set.of("name", "file", "line", "column");

      for (String fieldName : expectedFields) {
        try {
          Field field = FrameSymbol.class.getDeclaredField(fieldName);
          assertTrue(
              Modifier.isPrivate(field.getModifiers()), fieldName + " field should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), fieldName + " field should be final");
        } catch (NoSuchFieldException e) {
          assertTrue(false, "Field " + fieldName + " should exist");
        }
      }
    }

    @Test
    @DisplayName("name field should be of type String")
    void nameFieldShouldBeOfTypeString() throws NoSuchFieldException {
      Field field = FrameSymbol.class.getDeclaredField("name");
      assertEquals(String.class, field.getType(), "name field should be of type String");
    }

    @Test
    @DisplayName("file field should be of type String")
    void fileFieldShouldBeOfTypeString() throws NoSuchFieldException {
      Field field = FrameSymbol.class.getDeclaredField("file");
      assertEquals(String.class, field.getType(), "file field should be of type String");
    }

    @Test
    @DisplayName("line field should be of type Integer")
    void lineFieldShouldBeOfTypeInteger() throws NoSuchFieldException {
      Field field = FrameSymbol.class.getDeclaredField("line");
      assertEquals(Integer.class, field.getType(), "line field should be of type Integer");
    }

    @Test
    @DisplayName("column field should be of type Integer")
    void columnFieldShouldBeOfTypeInteger() throws NoSuchFieldException {
      Field field = FrameSymbol.class.getDeclaredField("column");
      assertEquals(Integer.class, field.getType(), "column field should be of type Integer");
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
          Object.class, FrameSymbol.class.getSuperclass(), "FrameSymbol should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          FrameSymbol.class.getInterfaces().length,
          "FrameSymbol should not implement any interfaces");
    }
  }
}

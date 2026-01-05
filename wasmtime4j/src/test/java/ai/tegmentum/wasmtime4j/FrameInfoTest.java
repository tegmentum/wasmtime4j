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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the FrameInfo class.
 *
 * <p>FrameInfo provides information about a WebAssembly stack frame, including function
 * information, module context, and debug symbols. This test verifies the class structure and method
 * signatures.
 */
@DisplayName("FrameInfo Class Tests")
class FrameInfoTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertTrue(!FrameInfo.class.isInterface(), "FrameInfo should be a class");
      assertTrue(!FrameInfo.class.isEnum(), "FrameInfo should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(FrameInfo.class.getModifiers()), "FrameInfo should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(Modifier.isFinal(FrameInfo.class.getModifiers()), "FrameInfo should be final");
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
    void shouldHavePublicConstructorWith6Parameters() throws NoSuchMethodException {
      Constructor<?> constructor =
          FrameInfo.class.getConstructor(
              int.class, Module.class, String.class, Integer.class, Integer.class, List.class);
      assertNotNull(constructor, "Constructor with 6 parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("constructor should have correct parameter types")
    void constructorShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      Constructor<?> constructor =
          FrameInfo.class.getConstructor(
              int.class, Module.class, String.class, Integer.class, Integer.class, List.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(6, paramTypes.length, "Constructor should have 6 parameters");
      assertEquals(int.class, paramTypes[0], "First parameter should be int (funcIndex)");
      assertEquals(Module.class, paramTypes[1], "Second parameter should be Module (module)");
      assertEquals(String.class, paramTypes[2], "Third parameter should be String (funcName)");
      assertEquals(
          Integer.class, paramTypes[3], "Fourth parameter should be Integer (moduleOffset)");
      assertEquals(Integer.class, paramTypes[4], "Fifth parameter should be Integer (funcOffset)");
      assertEquals(List.class, paramTypes[5], "Sixth parameter should be List (symbols)");
    }
  }

  // ========================================================================
  // Accessor Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("should have getFuncIndex method")
    void shouldHaveGetFuncIndexMethod() throws NoSuchMethodException {
      final Method method = FrameInfo.class.getMethod("getFuncIndex");
      assertNotNull(method, "getFuncIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "getFuncIndex should return int");
    }

    @Test
    @DisplayName("should have getModule method")
    void shouldHaveGetModuleMethod() throws NoSuchMethodException {
      final Method method = FrameInfo.class.getMethod("getModule");
      assertNotNull(method, "getModule method should exist");
      assertEquals(Module.class, method.getReturnType(), "getModule should return Module");
    }

    @Test
    @DisplayName("should have getFuncName method returning Optional")
    void shouldHaveGetFuncNameMethod() throws NoSuchMethodException {
      final Method method = FrameInfo.class.getMethod("getFuncName");
      assertNotNull(method, "getFuncName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getFuncName should return Optional");
    }

    @Test
    @DisplayName("should have getModuleOffset method returning Optional")
    void shouldHaveGetModuleOffsetMethod() throws NoSuchMethodException {
      final Method method = FrameInfo.class.getMethod("getModuleOffset");
      assertNotNull(method, "getModuleOffset method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "getModuleOffset should return Optional");
    }

    @Test
    @DisplayName("should have getFuncOffset method returning Optional")
    void shouldHaveGetFuncOffsetMethod() throws NoSuchMethodException {
      final Method method = FrameInfo.class.getMethod("getFuncOffset");
      assertNotNull(method, "getFuncOffset method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getFuncOffset should return Optional");
    }

    @Test
    @DisplayName("should have getSymbols method")
    void shouldHaveGetSymbolsMethod() throws NoSuchMethodException {
      final Method method = FrameInfo.class.getMethod("getSymbols");
      assertNotNull(method, "getSymbols method should exist");
      assertEquals(List.class, method.getReturnType(), "getSymbols should return List");
    }

    @Test
    @DisplayName("all accessor methods should have no parameters")
    void allAccessorMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          FrameInfo.class.getMethod("getFuncIndex").getParameterCount(),
          "getFuncIndex should have 0 params");
      assertEquals(
          0,
          FrameInfo.class.getMethod("getModule").getParameterCount(),
          "getModule should have 0 params");
      assertEquals(
          0,
          FrameInfo.class.getMethod("getFuncName").getParameterCount(),
          "getFuncName should have 0 params");
      assertEquals(
          0,
          FrameInfo.class.getMethod("getModuleOffset").getParameterCount(),
          "getModuleOffset should have 0 params");
      assertEquals(
          0,
          FrameInfo.class.getMethod("getFuncOffset").getParameterCount(),
          "getFuncOffset should have 0 params");
      assertEquals(
          0,
          FrameInfo.class.getMethod("getSymbols").getParameterCount(),
          "getSymbols should have 0 params");
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
      final Method method = FrameInfo.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          FrameInfo.class, method.getDeclaringClass(), "toString should be declared in FrameInfo");
    }

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      final Method method = FrameInfo.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(
          FrameInfo.class, method.getDeclaringClass(), "equals should be declared in FrameInfo");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      final Method method = FrameInfo.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          FrameInfo.class, method.getDeclaringClass(), "hashCode should be declared in FrameInfo");
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
              "getFuncIndex",
              "getModule",
              "getFuncName",
              "getModuleOffset",
              "getFuncOffset",
              "getSymbols",
              "toString",
              "equals",
              "hashCode");

      Set<String> actualMethods =
          Arrays.stream(FrameInfo.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "FrameInfo should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 9 declared methods")
    void shouldHaveAtLeast9DeclaredMethods() {
      assertTrue(
          FrameInfo.class.getDeclaredMethods().length >= 9,
          "FrameInfo should have at least 9 methods");
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
      Set<String> expectedFields =
          Set.of("funcIndex", "module", "funcName", "moduleOffset", "funcOffset", "symbols");

      for (String fieldName : expectedFields) {
        try {
          Field field = FrameInfo.class.getDeclaredField(fieldName);
          assertTrue(
              Modifier.isPrivate(field.getModifiers()), fieldName + " field should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), fieldName + " field should be final");
        } catch (NoSuchFieldException e) {
          assertTrue(false, "Field " + fieldName + " should exist");
        }
      }
    }

    @Test
    @DisplayName("funcIndex field should be of type int")
    void funcIndexFieldShouldBeOfTypeInt() throws NoSuchFieldException {
      Field field = FrameInfo.class.getDeclaredField("funcIndex");
      assertEquals(int.class, field.getType(), "funcIndex field should be of type int");
    }

    @Test
    @DisplayName("module field should be of type Module")
    void moduleFieldShouldBeOfTypeModule() throws NoSuchFieldException {
      Field field = FrameInfo.class.getDeclaredField("module");
      assertEquals(Module.class, field.getType(), "module field should be of type Module");
    }

    @Test
    @DisplayName("symbols field should be of type List")
    void symbolsFieldShouldBeOfTypeList() throws NoSuchFieldException {
      Field field = FrameInfo.class.getDeclaredField("symbols");
      assertEquals(List.class, field.getType(), "symbols field should be of type List");
    }
  }

  // ========================================================================
  // Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("getFuncName should return Optional of String")
    void getFuncNameShouldReturnOptionalOfString() throws NoSuchMethodException {
      final Method method = FrameInfo.class.getMethod("getFuncName");
      assertEquals(Optional.class, method.getReturnType(), "getFuncName should return Optional");
    }

    @Test
    @DisplayName("getModuleOffset should return Optional of Integer")
    void getModuleOffsetShouldReturnOptionalOfInteger() throws NoSuchMethodException {
      final Method method = FrameInfo.class.getMethod("getModuleOffset");
      assertEquals(
          Optional.class, method.getReturnType(), "getModuleOffset should return Optional");
    }

    @Test
    @DisplayName("getFuncOffset should return Optional of Integer")
    void getFuncOffsetShouldReturnOptionalOfInteger() throws NoSuchMethodException {
      final Method method = FrameInfo.class.getMethod("getFuncOffset");
      assertEquals(Optional.class, method.getReturnType(), "getFuncOffset should return Optional");
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
      assertEquals(Object.class, FrameInfo.class.getSuperclass(), "FrameInfo should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          FrameInfo.class.getInterfaces().length,
          "FrameInfo should not implement any interfaces");
    }
  }
}

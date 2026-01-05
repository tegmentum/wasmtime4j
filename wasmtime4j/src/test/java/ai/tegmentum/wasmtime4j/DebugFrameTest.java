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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the DebugFrame class.
 *
 * <p>DebugFrame provides detailed information about a single stack frame during WebAssembly
 * execution. This test verifies the class structure and method signatures.
 */
@DisplayName("DebugFrame Class Tests")
class DebugFrameTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertTrue(!DebugFrame.class.isInterface(), "DebugFrame should be a class");
      assertTrue(!DebugFrame.class.isEnum(), "DebugFrame should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(DebugFrame.class.getModifiers()), "DebugFrame should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(Modifier.isFinal(DebugFrame.class.getModifiers()), "DebugFrame should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with 7 parameters")
    void shouldHavePublicConstructorWith7Parameters() throws NoSuchMethodException {
      Constructor<?> constructor =
          DebugFrame.class.getConstructor(
              int.class, String.class, String.class, long.class, List.class, List.class, Map.class);
      assertNotNull(constructor, "Constructor with 7 parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have public constructor with 2 parameters (minimal)")
    void shouldHavePublicConstructorWith2Parameters() throws NoSuchMethodException {
      Constructor<?> constructor = DebugFrame.class.getConstructor(int.class, long.class);
      assertNotNull(constructor, "Minimal constructor with 2 parameters should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "Minimal constructor should be public");
    }

    @Test
    @DisplayName("full constructor should have correct parameter types")
    void fullConstructorShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      Constructor<?> constructor =
          DebugFrame.class.getConstructor(
              int.class, String.class, String.class, long.class, List.class, List.class, Map.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(7, paramTypes.length, "Constructor should have 7 parameters");
      assertEquals(int.class, paramTypes[0], "First parameter should be int (functionIndex)");
      assertEquals(String.class, paramTypes[1], "Second parameter should be String (functionName)");
      assertEquals(String.class, paramTypes[2], "Third parameter should be String (moduleName)");
      assertEquals(
          long.class, paramTypes[3], "Fourth parameter should be long (instructionOffset)");
      assertEquals(List.class, paramTypes[4], "Fifth parameter should be List (locals)");
      assertEquals(List.class, paramTypes[5], "Sixth parameter should be List (operandStack)");
      assertEquals(Map.class, paramTypes[6], "Seventh parameter should be Map (attributes)");
    }
  }

  // ========================================================================
  // Accessor Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("should have getFunctionIndex method")
    void shouldHaveGetFunctionIndexMethod() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("getFunctionIndex");
      assertNotNull(method, "getFunctionIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "getFunctionIndex should return int");
    }

    @Test
    @DisplayName("should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("getFunctionName");
      assertNotNull(method, "getFunctionName method should exist");
      assertEquals(String.class, method.getReturnType(), "getFunctionName should return String");
    }

    @Test
    @DisplayName("should have getModuleName method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("getModuleName");
      assertNotNull(method, "getModuleName method should exist");
      assertEquals(String.class, method.getReturnType(), "getModuleName should return String");
    }

    @Test
    @DisplayName("should have getInstructionOffset method")
    void shouldHaveGetInstructionOffsetMethod() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("getInstructionOffset");
      assertNotNull(method, "getInstructionOffset method should exist");
      assertEquals(long.class, method.getReturnType(), "getInstructionOffset should return long");
    }

    @Test
    @DisplayName("should have getLocals method")
    void shouldHaveGetLocalsMethod() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("getLocals");
      assertNotNull(method, "getLocals method should exist");
      assertEquals(List.class, method.getReturnType(), "getLocals should return List");
    }

    @Test
    @DisplayName("should have getLocal method with int parameter")
    void shouldHaveGetLocalMethod() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("getLocal", int.class);
      assertNotNull(method, "getLocal method should exist");
      assertEquals(WasmValue.class, method.getReturnType(), "getLocal should return WasmValue");
    }

    @Test
    @DisplayName("should have getLocalCount method")
    void shouldHaveGetLocalCountMethod() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("getLocalCount");
      assertNotNull(method, "getLocalCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getLocalCount should return int");
    }

    @Test
    @DisplayName("should have getOperandStack method")
    void shouldHaveGetOperandStackMethod() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("getOperandStack");
      assertNotNull(method, "getOperandStack method should exist");
      assertEquals(List.class, method.getReturnType(), "getOperandStack should return List");
    }

    @Test
    @DisplayName("should have getStackDepth method")
    void shouldHaveGetStackDepthMethod() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("getStackDepth");
      assertNotNull(method, "getStackDepth method should exist");
      assertEquals(int.class, method.getReturnType(), "getStackDepth should return int");
    }

    @Test
    @DisplayName("should have getAttributes method")
    void shouldHaveGetAttributesMethod() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("getAttributes");
      assertNotNull(method, "getAttributes method should exist");
      assertEquals(Map.class, method.getReturnType(), "getAttributes should return Map");
    }

    @Test
    @DisplayName("should have getAttribute method with String parameter")
    void shouldHaveGetAttributeMethod() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("getAttribute", String.class);
      assertNotNull(method, "getAttribute method should exist");
      assertEquals(Object.class, method.getReturnType(), "getAttribute should return Object");
    }

    @Test
    @DisplayName("should have hasAttribute method with String parameter")
    void shouldHaveHasAttributeMethod() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("hasAttribute", String.class);
      assertNotNull(method, "hasAttribute method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasAttribute should return boolean");
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
      final Method method = DebugFrame.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }

    @Test
    @DisplayName("builder method should return Builder")
    void builderMethodShouldReturnBuilder() throws NoSuchMethodException {
      final Method method = DebugFrame.class.getMethod("builder");
      assertTrue(
          method.getReturnType().getName().contains("Builder"),
          "builder should return Builder type");
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
      Class<?>[] declaredClasses = DebugFrame.class.getDeclaredClasses();
      boolean hasBuilder = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Builder")) {
          hasBuilder = true;
          assertTrue(Modifier.isPublic(clazz.getModifiers()), "Builder should be public");
          assertTrue(Modifier.isStatic(clazz.getModifiers()), "Builder should be static");
          assertTrue(Modifier.isFinal(clazz.getModifiers()), "Builder should be final");
        }
      }
      assertTrue(hasBuilder, "DebugFrame should have a nested Builder class");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      Class<?>[] declaredClasses = DebugFrame.class.getDeclaredClasses();
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Builder")) {
          Method buildMethod = clazz.getMethod("build");
          assertNotNull(buildMethod, "Builder should have build method");
          assertEquals(
              DebugFrame.class, buildMethod.getReturnType(), "build should return DebugFrame");
        }
      }
    }

    @Test
    @DisplayName("Builder should have fluent setter methods")
    void builderShouldHaveFluentSetterMethods() {
      Class<?>[] declaredClasses = DebugFrame.class.getDeclaredClasses();
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Builder")) {
          Set<String> expectedMethods =
              Set.of(
                  "functionIndex",
                  "functionName",
                  "moduleName",
                  "instructionOffset",
                  "locals",
                  "operandStack",
                  "attributes");
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
      final Method method = DebugFrame.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          DebugFrame.class,
          method.getDeclaringClass(),
          "toString should be declared in DebugFrame");
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
              "getFunctionIndex",
              "getFunctionName",
              "getModuleName",
              "getInstructionOffset",
              "getLocals",
              "getLocal",
              "getLocalCount",
              "getOperandStack",
              "getStackDepth",
              "getAttributes",
              "getAttribute",
              "hasAttribute",
              "toString",
              "builder");

      Set<String> actualMethods =
          Arrays.stream(DebugFrame.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "DebugFrame should have method: " + expected);
      }
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
          Set.of(
              "functionIndex",
              "functionName",
              "moduleName",
              "instructionOffset",
              "locals",
              "operandStack",
              "attributes");

      for (String fieldName : expectedFields) {
        try {
          Field field = DebugFrame.class.getDeclaredField(fieldName);
          assertTrue(
              Modifier.isPrivate(field.getModifiers()), fieldName + " field should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), fieldName + " field should be final");
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
          Object.class, DebugFrame.class.getSuperclass(), "DebugFrame should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          DebugFrame.class.getInterfaces().length,
          "DebugFrame should not implement any interfaces");
    }
  }
}

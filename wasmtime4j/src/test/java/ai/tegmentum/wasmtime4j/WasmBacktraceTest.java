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
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmBacktrace class.
 *
 * <p>WasmBacktrace captures the call stack at a specific point during WebAssembly execution. This
 * test verifies the class structure and method signatures.
 */
@DisplayName("WasmBacktrace Class Tests")
class WasmBacktraceTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertTrue(!WasmBacktrace.class.isInterface(), "WasmBacktrace should be a class");
      assertTrue(!WasmBacktrace.class.isEnum(), "WasmBacktrace should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmBacktrace.class.getModifiers()), "WasmBacktrace should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasmBacktrace.class.getModifiers()), "WasmBacktrace should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with 2 parameters")
    void shouldHavePublicConstructorWith2Parameters() throws NoSuchMethodException {
      Constructor<?> constructor = WasmBacktrace.class.getConstructor(List.class, boolean.class);
      assertNotNull(constructor, "Constructor with 2 parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("constructor should have correct parameter types")
    void constructorShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      Constructor<?> constructor = WasmBacktrace.class.getConstructor(List.class, boolean.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(2, paramTypes.length, "Constructor should have 2 parameters");
      assertEquals(List.class, paramTypes[0], "First parameter should be List (frames)");
      assertEquals(
          boolean.class, paramTypes[1], "Second parameter should be boolean (forceCapture)");
    }
  }

  // ========================================================================
  // Accessor Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("should have getFrames method")
    void shouldHaveGetFramesMethod() throws NoSuchMethodException {
      final Method method = WasmBacktrace.class.getMethod("getFrames");
      assertNotNull(method, "getFrames method should exist");
      assertEquals(List.class, method.getReturnType(), "getFrames should return List");
    }

    @Test
    @DisplayName("should have isForceCapture method")
    void shouldHaveIsForceCaptureMethod() throws NoSuchMethodException {
      final Method method = WasmBacktrace.class.getMethod("isForceCapture");
      assertNotNull(method, "isForceCapture method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isForceCapture should return boolean");
    }

    @Test
    @DisplayName("should have getFrameCount method")
    void shouldHaveGetFrameCountMethod() throws NoSuchMethodException {
      final Method method = WasmBacktrace.class.getMethod("getFrameCount");
      assertNotNull(method, "getFrameCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getFrameCount should return int");
    }

    @Test
    @DisplayName("should have isEmpty method")
    void shouldHaveIsEmptyMethod() throws NoSuchMethodException {
      final Method method = WasmBacktrace.class.getMethod("isEmpty");
      assertNotNull(method, "isEmpty method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isEmpty should return boolean");
    }

    @Test
    @DisplayName("all accessor methods should have no parameters")
    void allAccessorMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          WasmBacktrace.class.getMethod("getFrames").getParameterCount(),
          "getFrames should have 0 params");
      assertEquals(
          0,
          WasmBacktrace.class.getMethod("isForceCapture").getParameterCount(),
          "isForceCapture should have 0 params");
      assertEquals(
          0,
          WasmBacktrace.class.getMethod("getFrameCount").getParameterCount(),
          "getFrameCount should have 0 params");
      assertEquals(
          0,
          WasmBacktrace.class.getMethod("isEmpty").getParameterCount(),
          "isEmpty should have 0 params");
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
      final Method method = WasmBacktrace.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          WasmBacktrace.class,
          method.getDeclaringClass(),
          "toString should be declared in WasmBacktrace");
    }

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      final Method method = WasmBacktrace.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(
          WasmBacktrace.class,
          method.getDeclaringClass(),
          "equals should be declared in WasmBacktrace");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      final Method method = WasmBacktrace.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          WasmBacktrace.class,
          method.getDeclaringClass(),
          "hashCode should be declared in WasmBacktrace");
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
              "getFrames",
              "isForceCapture",
              "getFrameCount",
              "isEmpty",
              "toString",
              "equals",
              "hashCode");

      Set<String> actualMethods =
          Arrays.stream(WasmBacktrace.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "WasmBacktrace should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 7 declared methods")
    void shouldHaveAtLeast7DeclaredMethods() {
      assertTrue(
          WasmBacktrace.class.getDeclaredMethods().length >= 7,
          "WasmBacktrace should have at least 7 methods");
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
      Set<String> expectedFields = Set.of("frames", "forceCapture");

      for (String fieldName : expectedFields) {
        try {
          Field field = WasmBacktrace.class.getDeclaredField(fieldName);
          assertTrue(
              Modifier.isPrivate(field.getModifiers()), fieldName + " field should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), fieldName + " field should be final");
        } catch (NoSuchFieldException e) {
          assertTrue(false, "Field " + fieldName + " should exist");
        }
      }
    }

    @Test
    @DisplayName("frames field should be of type List")
    void framesFieldShouldBeOfTypeList() throws NoSuchFieldException {
      Field field = WasmBacktrace.class.getDeclaredField("frames");
      assertEquals(List.class, field.getType(), "frames field should be of type List");
    }

    @Test
    @DisplayName("forceCapture field should be of type boolean")
    void forceCaptureFieldShouldBeOfTypeBoolean() throws NoSuchFieldException {
      Field field = WasmBacktrace.class.getDeclaredField("forceCapture");
      assertEquals(boolean.class, field.getType(), "forceCapture field should be of type boolean");
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
          Object.class, WasmBacktrace.class.getSuperclass(), "WasmBacktrace should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          WasmBacktrace.class.getInterfaces().length,
          "WasmBacktrace should not implement any interfaces");
    }
  }
}

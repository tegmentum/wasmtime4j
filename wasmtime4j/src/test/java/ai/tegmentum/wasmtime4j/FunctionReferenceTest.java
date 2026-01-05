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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the FunctionReference interface.
 *
 * <p>FunctionReference represents a WebAssembly function reference that can be passed, stored in
 * tables, and called indirectly.
 */
@DisplayName("FunctionReference Interface Tests")
class FunctionReferenceTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(FunctionReference.class.isInterface(), "FunctionReference should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(FunctionReference.class.getModifiers()),
          "FunctionReference should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(FunctionReference.class.getModifiers()),
          "FunctionReference should not be final (interfaces cannot be final)");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interfaces")
    void shouldNotExtendAnyInterfaces() {
      assertEquals(
          0,
          FunctionReference.class.getInterfaces().length,
          "FunctionReference should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getFunctionType method")
    void shouldHaveGetFunctionTypeMethod() throws NoSuchMethodException {
      Method method = FunctionReference.class.getMethod("getFunctionType");
      assertNotNull(method, "getFunctionType method should exist");
      assertEquals(FunctionType.class, method.getReturnType(), "Should return FunctionType");
      assertFalse(method.isDefault(), "getFunctionType should be abstract");
    }

    @Test
    @DisplayName("should have call method")
    void shouldHaveCallMethod() throws NoSuchMethodException {
      Method method = FunctionReference.class.getMethod("call", WasmValue[].class);
      assertNotNull(method, "call method should exist");
      assertEquals(WasmValue[].class, method.getReturnType(), "Should return WasmValue[]");
      assertFalse(method.isDefault(), "call should be abstract");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = FunctionReference.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertFalse(method.isDefault(), "getName should be abstract");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = FunctionReference.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertFalse(method.isDefault(), "isValid should be abstract");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = FunctionReference.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getId should be abstract");
    }

    @Test
    @DisplayName("should have exactly 5 abstract methods")
    void shouldHaveExactly5AbstractMethods() {
      long abstractMethods =
          Arrays.stream(FunctionReference.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(5, abstractMethods, "FunctionReference should have exactly 5 abstract methods");
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
          Set.of("getFunctionType", "call", "getName", "isValid", "getId");

      Set<String> actualMethods =
          Arrays.stream(FunctionReference.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "FunctionReference should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 5 declared methods")
    void shouldHaveExactly5DeclaredMethods() {
      long methodCount =
          Arrays.stream(FunctionReference.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(5, methodCount, "FunctionReference should have exactly 5 declared methods");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultMethods =
          Arrays.stream(FunctionReference.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethods, "FunctionReference should have no default methods");
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
          Arrays.stream(FunctionReference.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "FunctionReference should have no static methods");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0,
          FunctionReference.class.getDeclaredFields().length,
          "FunctionReference should have no declared fields");
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
          FunctionReference.class.getDeclaredClasses().length,
          "FunctionReference should have no nested classes");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("call method should have varargs parameter")
    void callMethodShouldHaveVarargsParameter() throws NoSuchMethodException {
      Method method = FunctionReference.class.getMethod("call", WasmValue[].class);
      assertTrue(method.isVarArgs(), "call method should be varargs");
    }

    @Test
    @DisplayName("getFunctionType should have no parameters")
    void getFunctionTypeShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = FunctionReference.class.getMethod("getFunctionType");
      assertEquals(0, method.getParameterCount(), "getFunctionType should have no parameters");
    }

    @Test
    @DisplayName("getName should have no parameters")
    void getNameShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = FunctionReference.class.getMethod("getName");
      assertEquals(0, method.getParameterCount(), "getName should have no parameters");
    }

    @Test
    @DisplayName("isValid should have no parameters")
    void isValidShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = FunctionReference.class.getMethod("isValid");
      assertEquals(0, method.getParameterCount(), "isValid should have no parameters");
    }

    @Test
    @DisplayName("getId should have no parameters")
    void getIdShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = FunctionReference.class.getMethod("getId");
      assertEquals(0, method.getParameterCount(), "getId should have no parameters");
    }
  }

  // ========================================================================
  // Method Visibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Visibility Tests")
  class MethodVisibilityTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      Arrays.stream(FunctionReference.class.getDeclaredMethods())
          .filter(m -> !m.isSynthetic())
          .forEach(
              m ->
                  assertTrue(
                      Modifier.isPublic(m.getModifiers()),
                      "Method " + m.getName() + " should be public"));
    }
  }
}

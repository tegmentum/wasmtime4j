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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Extern interface.
 *
 * <p>Extern represents an external value that can be imported or exported by a WebAssembly module.
 * This test verifies the interface structure and method signatures.
 */
@DisplayName("Extern Interface Tests")
class ExternTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Extern.class.isInterface(), "Extern should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(Extern.class.getModifiers()), "Extern should be public");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = Extern.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "getType should return ExternType");
    }

    @Test
    @DisplayName("getType should be abstract")
    void getTypeShouldBeAbstract() throws NoSuchMethodException {
      final Method method = Extern.class.getMethod("getType");
      assertTrue(
          Modifier.isAbstract(method.getModifiers()), "getType should be an abstract method");
    }
  }

  // ========================================================================
  // Default Type Check Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Type Check Methods Tests")
  class DefaultTypeCheckMethodsTests {

    @Test
    @DisplayName("should have isFunction default method")
    void shouldHaveIsFunctionMethod() throws NoSuchMethodException {
      final Method method = Extern.class.getMethod("isFunction");
      assertNotNull(method, "isFunction method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isFunction should return boolean");
      assertTrue(method.isDefault(), "isFunction should be a default method");
    }

    @Test
    @DisplayName("should have isTable default method")
    void shouldHaveIsTableMethod() throws NoSuchMethodException {
      final Method method = Extern.class.getMethod("isTable");
      assertNotNull(method, "isTable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isTable should return boolean");
      assertTrue(method.isDefault(), "isTable should be a default method");
    }

    @Test
    @DisplayName("should have isMemory default method")
    void shouldHaveIsMemoryMethod() throws NoSuchMethodException {
      final Method method = Extern.class.getMethod("isMemory");
      assertNotNull(method, "isMemory method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isMemory should return boolean");
      assertTrue(method.isDefault(), "isMemory should be a default method");
    }

    @Test
    @DisplayName("should have isGlobal default method")
    void shouldHaveIsGlobalMethod() throws NoSuchMethodException {
      final Method method = Extern.class.getMethod("isGlobal");
      assertNotNull(method, "isGlobal method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isGlobal should return boolean");
      assertTrue(method.isDefault(), "isGlobal should be a default method");
    }
  }

  // ========================================================================
  // Default Cast Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Cast Methods Tests")
  class DefaultCastMethodsTests {

    @Test
    @DisplayName("should have asFunction default method")
    void shouldHaveAsFunctionMethod() throws NoSuchMethodException {
      final Method method = Extern.class.getMethod("asFunction");
      assertNotNull(method, "asFunction method should exist");
      assertEquals(
          WasmFunction.class, method.getReturnType(), "asFunction should return WasmFunction");
      assertTrue(method.isDefault(), "asFunction should be a default method");
    }

    @Test
    @DisplayName("should have asTable default method")
    void shouldHaveAsTableMethod() throws NoSuchMethodException {
      final Method method = Extern.class.getMethod("asTable");
      assertNotNull(method, "asTable method should exist");
      assertEquals(WasmTable.class, method.getReturnType(), "asTable should return WasmTable");
      assertTrue(method.isDefault(), "asTable should be a default method");
    }

    @Test
    @DisplayName("should have asMemory default method")
    void shouldHaveAsMemoryMethod() throws NoSuchMethodException {
      final Method method = Extern.class.getMethod("asMemory");
      assertNotNull(method, "asMemory method should exist");
      assertEquals(WasmMemory.class, method.getReturnType(), "asMemory should return WasmMemory");
      assertTrue(method.isDefault(), "asMemory should be a default method");
    }

    @Test
    @DisplayName("should have asGlobal default method")
    void shouldHaveAsGlobalMethod() throws NoSuchMethodException {
      final Method method = Extern.class.getMethod("asGlobal");
      assertNotNull(method, "asGlobal method should exist");
      assertEquals(WasmGlobal.class, method.getReturnType(), "asGlobal should return WasmGlobal");
      assertTrue(method.isDefault(), "asGlobal should be a default method");
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
              "getType",
              "isFunction",
              "isTable",
              "isMemory",
              "isGlobal",
              "asFunction",
              "asTable",
              "asMemory",
              "asGlobal");

      Set<String> actualMethods =
          Arrays.stream(Extern.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Extern should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 10 declared methods")
    void shouldHaveTenDeclaredMethods() {
      assertEquals(
          10, Extern.class.getDeclaredMethods().length, "Extern should have exactly 10 methods");
    }

    @Test
    @DisplayName("should have 8 default methods")
    void shouldHaveEightDefaultMethods() {
      long defaultMethodCount =
          Arrays.stream(Extern.class.getDeclaredMethods()).filter(Method::isDefault).count();
      assertEquals(8, defaultMethodCount, "Extern should have 8 default methods");
    }

    @Test
    @DisplayName("should have 1 abstract method")
    void shouldHaveOneAbstractMethod() {
      long abstractMethodCount =
          Arrays.stream(Extern.class.getDeclaredMethods())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(1, abstractMethodCount, "Extern should have 1 abstract method");
    }
  }

  // ========================================================================
  // Method Signature Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("all type check methods should have no parameters")
    void allTypeCheckMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          Extern.class.getMethod("isFunction").getParameterCount(),
          "isFunction should have 0 params");
      assertEquals(
          0, Extern.class.getMethod("isTable").getParameterCount(), "isTable should have 0 params");
      assertEquals(
          0,
          Extern.class.getMethod("isMemory").getParameterCount(),
          "isMemory should have 0 params");
      assertEquals(
          0,
          Extern.class.getMethod("isGlobal").getParameterCount(),
          "isGlobal should have 0 params");
    }

    @Test
    @DisplayName("all cast methods should have no parameters")
    void allCastMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          Extern.class.getMethod("asFunction").getParameterCount(),
          "asFunction should have 0 params");
      assertEquals(
          0, Extern.class.getMethod("asTable").getParameterCount(), "asTable should have 0 params");
      assertEquals(
          0,
          Extern.class.getMethod("asMemory").getParameterCount(),
          "asMemory should have 0 params");
      assertEquals(
          0,
          Extern.class.getMethod("asGlobal").getParameterCount(),
          "asGlobal should have 0 params");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interface")
    void shouldNotExtendAnyInterface() {
      assertEquals(
          0, Extern.class.getInterfaces().length, "Extern should not extend any interface");
    }
  }
}

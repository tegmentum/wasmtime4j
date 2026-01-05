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
 * Comprehensive test suite for the WasmGlobal interface.
 *
 * <p>WasmGlobal represents a WebAssembly global variable that can be read and (if mutable) written.
 * This test verifies the interface structure and method signatures.
 */
@DisplayName("WasmGlobal Interface Tests")
class WasmGlobalTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmGlobal.class.isInterface(), "WasmGlobal should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WasmGlobal.class.getModifiers()), "WasmGlobal should be public");
    }
  }

  // ========================================================================
  // Abstract Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Methods Tests")
  class AbstractMethodsTests {

    @Test
    @DisplayName("should have get method")
    void shouldHaveGetMethod() throws NoSuchMethodException {
      final Method method = WasmGlobal.class.getMethod("get");
      assertNotNull(method, "get method should exist");
      assertEquals(WasmValue.class, method.getReturnType(), "get should return WasmValue");
    }

    @Test
    @DisplayName("should have set method")
    void shouldHaveSetMethod() throws NoSuchMethodException {
      final Method method = WasmGlobal.class.getMethod("set", WasmValue.class);
      assertNotNull(method, "set method should exist");
      assertEquals(void.class, method.getReturnType(), "set should return void");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = WasmGlobal.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(
          WasmValueType.class, method.getReturnType(), "getType should return WasmValueType");
    }

    @Test
    @DisplayName("should have isMutable method")
    void shouldHaveIsMutableMethod() throws NoSuchMethodException {
      final Method method = WasmGlobal.class.getMethod("isMutable");
      assertNotNull(method, "isMutable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isMutable should return boolean");
    }

    @Test
    @DisplayName("should have getGlobalType method")
    void shouldHaveGetGlobalTypeMethod() throws NoSuchMethodException {
      final Method method = WasmGlobal.class.getMethod("getGlobalType");
      assertNotNull(method, "getGlobalType method should exist");
      assertEquals(
          GlobalType.class, method.getReturnType(), "getGlobalType should return GlobalType");
    }
  }

  // ========================================================================
  // Default Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Methods Tests")
  class DefaultMethodsTests {

    @Test
    @DisplayName("should have getValue default method")
    void shouldHaveGetValueDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmGlobal.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertTrue(method.isDefault(), "getValue should be a default method");
      assertEquals(WasmValue.class, method.getReturnType(), "getValue should return WasmValue");
    }
  }

  // ========================================================================
  // Method Parameters Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameters Tests")
  class MethodParametersTests {

    @Test
    @DisplayName("get should have no parameters")
    void getShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = WasmGlobal.class.getMethod("get");
      assertEquals(0, method.getParameterCount(), "get should have 0 parameters");
    }

    @Test
    @DisplayName("set should have 1 WasmValue parameter")
    void setShouldHave1Parameter() throws NoSuchMethodException {
      final Method method = WasmGlobal.class.getMethod("set", WasmValue.class);
      assertEquals(1, method.getParameterCount(), "set should have 1 parameter");
      assertEquals(WasmValue.class, method.getParameterTypes()[0], "Parameter should be WasmValue");
    }

    @Test
    @DisplayName("getType should have no parameters")
    void getTypeShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = WasmGlobal.class.getMethod("getType");
      assertEquals(0, method.getParameterCount(), "getType should have 0 parameters");
    }

    @Test
    @DisplayName("isMutable should have no parameters")
    void isMutableShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = WasmGlobal.class.getMethod("isMutable");
      assertEquals(0, method.getParameterCount(), "isMutable should have 0 parameters");
    }

    @Test
    @DisplayName("getGlobalType should have no parameters")
    void getGlobalTypeShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = WasmGlobal.class.getMethod("getGlobalType");
      assertEquals(0, method.getParameterCount(), "getGlobalType should have 0 parameters");
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
          Set.of("get", "getValue", "set", "getType", "isMutable", "getGlobalType");

      Set<String> actualMethods =
          Arrays.stream(WasmGlobal.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "WasmGlobal should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 6 declared methods")
    void shouldHaveAtLeast6DeclaredMethods() {
      assertTrue(
          WasmGlobal.class.getDeclaredMethods().length >= 6,
          "WasmGlobal should have at least 6 methods");
    }

    @Test
    @DisplayName("should have at least 1 default method")
    void shouldHaveAtLeast1DefaultMethod() {
      long defaultMethodCount =
          Arrays.stream(WasmGlobal.class.getDeclaredMethods()).filter(Method::isDefault).count();
      assertTrue(defaultMethodCount >= 1, "WasmGlobal should have at least 1 default method");
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
          0, WasmGlobal.class.getInterfaces().length, "WasmGlobal should not extend any interface");
    }
  }
}

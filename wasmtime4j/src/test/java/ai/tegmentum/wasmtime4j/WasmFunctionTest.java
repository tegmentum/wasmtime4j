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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmFunction interface.
 *
 * <p>WasmFunction represents a WebAssembly function that can be called from Java code. This test
 * verifies the interface structure and method signatures.
 */
@DisplayName("WasmFunction Interface Tests")
class WasmFunctionTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmFunction.class.isInterface(), "WasmFunction should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmFunction.class.getModifiers()), "WasmFunction should be public");
    }
  }

  // ========================================================================
  // Abstract Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Methods Tests")
  class AbstractMethodsTests {

    @Test
    @DisplayName("should have call method")
    void shouldHaveCallMethod() throws NoSuchMethodException {
      final Method method = WasmFunction.class.getMethod("call", WasmValue[].class);
      assertNotNull(method, "call method should exist");
      assertEquals(WasmValue[].class, method.getReturnType(), "call should return WasmValue[]");
    }

    @Test
    @DisplayName("should have getFunctionType method")
    void shouldHaveGetFunctionTypeMethod() throws NoSuchMethodException {
      final Method method = WasmFunction.class.getMethod("getFunctionType");
      assertNotNull(method, "getFunctionType method should exist");
      assertEquals(
          FunctionType.class, method.getReturnType(), "getFunctionType should return FunctionType");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = WasmFunction.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("should have callAsync method")
    void shouldHaveCallAsyncMethod() throws NoSuchMethodException {
      final Method method = WasmFunction.class.getMethod("callAsync", WasmValue[].class);
      assertNotNull(method, "callAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "callAsync should return CompletableFuture");
    }
  }

  // ========================================================================
  // Default Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Methods Tests")
  class DefaultMethodsTests {

    @Test
    @DisplayName("should have callSingleAsync default method")
    void shouldHaveCallSingleAsyncDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmFunction.class.getMethod("callSingleAsync", WasmValue[].class);
      assertNotNull(method, "callSingleAsync method should exist");
      assertTrue(method.isDefault(), "callSingleAsync should be a default method");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "callSingleAsync should return CompletableFuture");
    }

    @Test
    @DisplayName("should have typed default method")
    void shouldHaveTypedDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmFunction.class.getMethod("typed", String.class);
      assertNotNull(method, "typed method should exist");
      assertTrue(method.isDefault(), "typed should be a default method");
      assertEquals(TypedFunc.class, method.getReturnType(), "typed should return TypedFunc");
    }

    @Test
    @DisplayName("should have matchesType default method")
    void shouldHaveMatchesTypeDefaultMethod() throws NoSuchMethodException {
      final Method method =
          WasmFunction.class.getMethod("matchesType", WasmValueType[].class, WasmValueType[].class);
      assertNotNull(method, "matchesType method should exist");
      assertTrue(method.isDefault(), "matchesType should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "matchesType should return boolean");
    }

    @Test
    @DisplayName("should have callUnchecked default method")
    void shouldHaveCallUncheckedDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmFunction.class.getMethod("callUnchecked", ValRaw[].class);
      assertNotNull(method, "callUnchecked method should exist");
      assertTrue(method.isDefault(), "callUnchecked should be a default method");
      assertEquals(ValRaw[].class, method.getReturnType(), "callUnchecked should return ValRaw[]");
    }

    @Test
    @DisplayName("should have getNativeHandle default method")
    void shouldHaveGetNativeHandleDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmFunction.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertTrue(method.isDefault(), "getNativeHandle should be a default method");
      assertEquals(long.class, method.getReturnType(), "getNativeHandle should return long");
    }
  }

  // ========================================================================
  // Method Parameters Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameters Tests")
  class MethodParametersTests {

    @Test
    @DisplayName("call should accept varargs WasmValue")
    void callShouldAcceptVarargsWasmValue() throws NoSuchMethodException {
      final Method method = WasmFunction.class.getMethod("call", WasmValue[].class);
      assertEquals(1, method.getParameterCount(), "call should have 1 varargs parameter");
      assertTrue(method.isVarArgs(), "call should be varargs");
    }

    @Test
    @DisplayName("getFunctionType should have no parameters")
    void getFunctionTypeShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = WasmFunction.class.getMethod("getFunctionType");
      assertEquals(0, method.getParameterCount(), "getFunctionType should have 0 parameters");
    }

    @Test
    @DisplayName("getName should have no parameters")
    void getNameShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = WasmFunction.class.getMethod("getName");
      assertEquals(0, method.getParameterCount(), "getName should have 0 parameters");
    }

    @Test
    @DisplayName("typed should have 1 String parameter")
    void typedShouldHave1StringParameter() throws NoSuchMethodException {
      final Method method = WasmFunction.class.getMethod("typed", String.class);
      assertEquals(1, method.getParameterCount(), "typed should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
    }

    @Test
    @DisplayName("matchesType should have 2 WasmValueType array parameters")
    void matchesTypeShouldHave2Parameters() throws NoSuchMethodException {
      final Method method =
          WasmFunction.class.getMethod("matchesType", WasmValueType[].class, WasmValueType[].class);
      assertEquals(2, method.getParameterCount(), "matchesType should have 2 parameters");
      assertEquals(
          WasmValueType[].class,
          method.getParameterTypes()[0],
          "First parameter should be WasmValueType[]");
      assertEquals(
          WasmValueType[].class,
          method.getParameterTypes()[1],
          "Second parameter should be WasmValueType[]");
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
              "call",
              "getFunctionType",
              "getName",
              "callAsync",
              "callSingleAsync",
              "typed",
              "matchesType",
              "callUnchecked",
              "getNativeHandle");

      Set<String> actualMethods =
          Arrays.stream(WasmFunction.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "WasmFunction should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 9 declared methods")
    void shouldHaveAtLeast9DeclaredMethods() {
      assertTrue(
          WasmFunction.class.getDeclaredMethods().length >= 9,
          "WasmFunction should have at least 9 methods");
    }

    @Test
    @DisplayName("should have at least 5 default methods")
    void shouldHaveAtLeast5DefaultMethods() {
      long defaultMethodCount =
          Arrays.stream(WasmFunction.class.getDeclaredMethods()).filter(Method::isDefault).count();
      assertTrue(defaultMethodCount >= 5, "WasmFunction should have at least 5 default methods");
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
          0,
          WasmFunction.class.getInterfaces().length,
          "WasmFunction should not extend any interface");
    }
  }
}

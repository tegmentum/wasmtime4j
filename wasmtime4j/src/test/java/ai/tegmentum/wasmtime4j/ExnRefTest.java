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
 * Comprehensive test suite for the ExnRef interface.
 *
 * <p>ExnRef represents a WebAssembly exception reference that can be thrown or caught. This test
 * verifies the interface structure and method signatures.
 */
@DisplayName("ExnRef Interface Tests")
class ExnRefTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExnRef.class.isInterface(), "ExnRef should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(ExnRef.class.getModifiers()), "ExnRef should be public");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getTag method")
    void shouldHaveGetTagMethod() throws NoSuchMethodException {
      final Method method = ExnRef.class.getMethod("getTag", Store.class);
      assertNotNull(method, "getTag method should exist");
      assertEquals(Tag.class, method.getReturnType(), "getTag should return Tag");
    }

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = ExnRef.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "getNativeHandle should return long");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = ExnRef.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("getTag should declare WasmException")
    void getTagShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ExnRef.class.getMethod("getTag", Store.class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "getTag should declare one exception");
      assertEquals(
          "ai.tegmentum.wasmtime4j.exception.WasmException",
          exceptions[0].getName(),
          "getTag should declare WasmException");
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
      Set<String> expectedMethods = Set.of("getTag", "getNativeHandle", "isValid");

      Set<String> actualMethods =
          Arrays.stream(ExnRef.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "ExnRef should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 3 declared methods")
    void shouldHaveThreeDeclaredMethods() {
      assertEquals(
          3, ExnRef.class.getDeclaredMethods().length, "ExnRef should have exactly 3 methods");
    }

    @Test
    @DisplayName("all methods should be abstract")
    void allMethodsShouldBeAbstract() {
      for (Method method : ExnRef.class.getDeclaredMethods()) {
        assertTrue(
            Modifier.isAbstract(method.getModifiers()), method.getName() + " should be abstract");
      }
    }

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultMethodCount =
          Arrays.stream(ExnRef.class.getDeclaredMethods()).filter(Method::isDefault).count();
      assertEquals(0, defaultMethodCount, "ExnRef should have no default methods");
    }
  }

  // ========================================================================
  // Method Signature Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("getTag should accept Store parameter")
    void getTagShouldAcceptStoreParameter() throws NoSuchMethodException {
      final Method method = ExnRef.class.getMethod("getTag", Store.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "getTag should have 1 parameter");
      assertEquals(Store.class, paramTypes[0], "getTag parameter should be Store");
    }

    @Test
    @DisplayName("getNativeHandle should have no parameters")
    void getNativeHandleShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = ExnRef.class.getMethod("getNativeHandle");
      assertEquals(0, method.getParameterCount(), "getNativeHandle should have no parameters");
    }

    @Test
    @DisplayName("isValid should have no parameters")
    void isValidShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = ExnRef.class.getMethod("isValid");
      assertEquals(0, method.getParameterCount(), "isValid should have no parameters");
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
          0, ExnRef.class.getInterfaces().length, "ExnRef should not extend any interface");
    }
  }
}

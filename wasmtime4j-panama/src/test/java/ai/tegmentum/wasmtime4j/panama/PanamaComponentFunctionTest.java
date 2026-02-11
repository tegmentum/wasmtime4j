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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentFunction;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link PanamaComponentFunction}.
 *
 * <p>These tests verify class structure, method signatures, and field definitions using reflection.
 * This approach allows testing without requiring native library loading.
 */
@DisplayName("PanamaComponentFunction Tests")
class PanamaComponentFunctionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaComponentFunction should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      int modifiers = PanamaComponentFunction.class.getModifiers();
      assertTrue(Modifier.isFinal(modifiers), "PanamaComponentFunction should be final");
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "PanamaComponentFunction should be package-private");
    }

    @Test
    @DisplayName("PanamaComponentFunction should implement ComponentFunction")
    void shouldImplementComponentFunction() {
      assertTrue(
          ComponentFunction.class.isAssignableFrom(PanamaComponentFunction.class),
          "PanamaComponentFunction should implement ComponentFunction");
    }

    @Test
    @DisplayName("Class should be in correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama",
          PanamaComponentFunction.class.getPackage().getName(),
          "Should be in panama package");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have functionName field")
    void shouldHaveFunctionNameField() throws NoSuchFieldException {
      Field field = PanamaComponentFunction.class.getDeclaredField("functionName");
      assertNotNull(field, "functionName field should exist");
      assertEquals(String.class, field.getType(), "Should be String type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have instance field")
    void shouldHaveInstanceField() throws NoSuchFieldException {
      Field field = PanamaComponentFunction.class.getDeclaredField("instance");
      assertNotNull(field, "instance field should exist");
      assertEquals(
          PanamaComponentInstance.class, field.getType(), "Should be PanamaComponentInstance type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have package-private constructor with String and PanamaComponentInstance")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          PanamaComponentFunction.class.getDeclaredConstructor(
              String.class, PanamaComponentInstance.class);
      assertNotNull(constructor, "Constructor should exist");
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("Constructor should have two parameters")
    void constructorShouldHaveTwoParameters() throws NoSuchMethodException {
      Constructor<?> constructor =
          PanamaComponentFunction.class.getDeclaredConstructor(
              String.class, PanamaComponentInstance.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(2, paramTypes.length, "Should have 2 parameters");
      assertEquals(String.class, paramTypes[0], "First parameter should be String");
      assertEquals(
          PanamaComponentInstance.class,
          paramTypes[1],
          "Second parameter should be PanamaComponentInstance");
    }
  }

  @Nested
  @DisplayName("Interface Method Tests")
  class InterfaceMethodTests {

    @Test
    @DisplayName("getName should exist and return String")
    void getNameShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentFunction.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("call should exist with varargs parameter")
    void callShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentFunction.class.getMethod("call", Object[].class);
      assertNotNull(method, "call method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("isValid should exist and return boolean")
    void isValidShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentFunction.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("getInstance should exist and return ComponentInstance")
    void getInstanceShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentFunction.class.getMethod("getInstance");
      assertNotNull(method, "getInstance method should exist");
      assertEquals(
          ComponentInstance.class, method.getReturnType(), "Should return ComponentInstance");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }
  }

  @Nested
  @DisplayName("Additional Method Tests")
  class AdditionalMethodTests {

    @Test
    @DisplayName("toString should exist and return String")
    void toStringShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentFunction.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("Should implement all ComponentFunction interface methods")
    void shouldImplementAllInterfaceMethods() {
      for (Method interfaceMethod : ComponentFunction.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : PanamaComponentFunction.class.getMethods()) {
            if (implMethod.getName().equals(interfaceMethod.getName())
                && arrayEquals(
                    implMethod.getParameterTypes(), interfaceMethod.getParameterTypes())) {
              found = true;
              break;
            }
          }
          assertTrue(found, "Should implement interface method: " + interfaceMethod.getName());
        }
      }
    }

    private boolean arrayEquals(final Class<?>[] a, final Class<?>[] b) {
      if (a.length != b.length) {
        return false;
      }
      for (int i = 0; i < a.length; i++) {
        if (!a[i].equals(b[i])) {
          return false;
        }
      }
      return true;
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("Should have expected number of declared methods")
    void shouldHaveExpectedMethodCount() {
      Method[] methods = PanamaComponentFunction.class.getDeclaredMethods();
      // Expected: getName, call, isValid, getInstance, toString
      assertTrue(methods.length >= 4, "Should have at least 4 declared methods");
    }
  }

  @Nested
  @DisplayName("Field Count Tests")
  class FieldCountTests {

    @Test
    @DisplayName("Should have expected number of fields")
    void shouldHaveExpectedFieldCount() {
      Field[] fields = PanamaComponentFunction.class.getDeclaredFields();
      assertEquals(2, fields.length, "Should have exactly 2 fields");
    }
  }

  @Nested
  @DisplayName("Constructor Count Tests")
  class ConstructorCountTests {

    @Test
    @DisplayName("Should have exactly one constructor")
    void shouldHaveExactlyOneConstructor() {
      Constructor<?>[] constructors = PanamaComponentFunction.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");
    }
  }

  @Nested
  @DisplayName("Call Method Signature Tests")
  class CallMethodSignatureTests {

    @Test
    @DisplayName("call method should declare WasmException")
    void callMethodShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = PanamaComponentFunction.class.getMethod("call", Object[].class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getName().contains("WasmException")) {
          declaresWasmException = true;
          break;
        }
      }
      assertTrue(declaresWasmException, "call should declare WasmException");
    }
  }
}

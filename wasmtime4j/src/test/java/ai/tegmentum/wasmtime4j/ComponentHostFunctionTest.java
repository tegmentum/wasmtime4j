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
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentHostFunction} interface.
 *
 * <p>ComponentHostFunction represents a host function that can be called from WebAssembly
 * components.
 */
@DisplayName("ComponentHostFunction Tests")
class ComponentHostFunctionTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentHostFunction.class.getModifiers()),
          "ComponentHostFunction should be public");
      assertTrue(
          ComponentHostFunction.class.isInterface(),
          "ComponentHostFunction should be an interface");
    }

    @Test
    @DisplayName("should be a functional interface")
    void shouldBeFunctionalInterface() {
      assertTrue(
          ComponentHostFunction.class.isAnnotationPresent(FunctionalInterface.class),
          "ComponentHostFunction should be annotated with @FunctionalInterface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have execute method")
    void shouldHaveExecuteMethod() throws NoSuchMethodException {
      final Method method = ComponentHostFunction.class.getMethod("execute", List.class);
      assertNotNull(method, "execute method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have create static method")
    void shouldHaveCreateStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentHostFunction.class.getMethod("create", ComponentHostFunction.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(
          ComponentHostFunction.class,
          method.getReturnType(),
          "Should return ComponentHostFunction");
    }

    @Test
    @DisplayName("should have voidFunction static method")
    void shouldHaveVoidFunctionStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentHostFunction.class.getMethod(
              "voidFunction", ComponentHostFunction.VoidComponentHostFunction.class);
      assertNotNull(method, "voidFunction method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "voidFunction should be static");
      assertEquals(
          ComponentHostFunction.class,
          method.getReturnType(),
          "Should return ComponentHostFunction");
    }

    @Test
    @DisplayName("should have voidFunctionWithParams static method")
    void shouldHaveVoidFunctionWithParamsStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentHostFunction.class.getMethod(
              "voidFunctionWithParams",
              ComponentHostFunction.VoidComponentHostFunctionWithParams.class);
      assertNotNull(method, "voidFunctionWithParams method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "voidFunctionWithParams should be static");
      assertEquals(
          ComponentHostFunction.class,
          method.getReturnType(),
          "Should return ComponentHostFunction");
    }

    @Test
    @DisplayName("should have singleValue static method")
    void shouldHaveSingleValueStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentHostFunction.class.getMethod(
              "singleValue", ComponentHostFunction.SingleValueComponentHostFunction.class);
      assertNotNull(method, "singleValue method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "singleValue should be static");
      assertEquals(
          ComponentHostFunction.class,
          method.getReturnType(),
          "Should return ComponentHostFunction");
    }

    @Test
    @DisplayName("should have withCaller static method")
    void shouldHaveWithCallerStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentHostFunction.class.getMethod(
              "withCaller", ComponentHostFunction.ComponentHostFunctionWithCaller.class);
      assertNotNull(method, "withCaller method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "withCaller should be static");
      assertEquals(
          ComponentHostFunction.class,
          method.getReturnType(),
          "Should return ComponentHostFunction");
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have VoidComponentHostFunction nested interface")
    void shouldHaveVoidComponentHostFunctionNestedInterface() {
      final var nestedClasses = ComponentHostFunction.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("VoidComponentHostFunction")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "VoidComponentHostFunction should be an interface");
          assertTrue(
              nestedClass.isAnnotationPresent(FunctionalInterface.class),
              "VoidComponentHostFunction should be a functional interface");
          break;
        }
      }
      assertTrue(found, "Should have VoidComponentHostFunction nested interface");
    }

    @Test
    @DisplayName("should have VoidComponentHostFunctionWithParams nested interface")
    void shouldHaveVoidComponentHostFunctionWithParamsNestedInterface() {
      final var nestedClasses = ComponentHostFunction.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("VoidComponentHostFunctionWithParams")) {
          found = true;
          assertTrue(
              nestedClass.isInterface(),
              "VoidComponentHostFunctionWithParams should be an interface");
          assertTrue(
              nestedClass.isAnnotationPresent(FunctionalInterface.class),
              "VoidComponentHostFunctionWithParams should be a functional interface");
          break;
        }
      }
      assertTrue(found, "Should have VoidComponentHostFunctionWithParams nested interface");
    }

    @Test
    @DisplayName("should have SingleValueComponentHostFunction nested interface")
    void shouldHaveSingleValueComponentHostFunctionNestedInterface() {
      final var nestedClasses = ComponentHostFunction.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("SingleValueComponentHostFunction")) {
          found = true;
          assertTrue(
              nestedClass.isInterface(), "SingleValueComponentHostFunction should be an interface");
          assertTrue(
              nestedClass.isAnnotationPresent(FunctionalInterface.class),
              "SingleValueComponentHostFunction should be a functional interface");
          break;
        }
      }
      assertTrue(found, "Should have SingleValueComponentHostFunction nested interface");
    }

    @Test
    @DisplayName("should have ComponentHostFunctionWithCaller nested interface")
    void shouldHaveComponentHostFunctionWithCallerNestedInterface() {
      final var nestedClasses = ComponentHostFunction.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ComponentHostFunctionWithCaller")) {
          found = true;
          assertTrue(
              nestedClass.isInterface(), "ComponentHostFunctionWithCaller should be an interface");
          assertTrue(
              nestedClass.isAnnotationPresent(FunctionalInterface.class),
              "ComponentHostFunctionWithCaller should be a functional interface");
          break;
        }
      }
      assertTrue(found, "Should have ComponentHostFunctionWithCaller nested interface");
    }

    @Test
    @DisplayName("should have CallerAwareComponentHostFunction nested class")
    void shouldHaveCallerAwareComponentHostFunctionNestedClass() {
      final var nestedClasses = ComponentHostFunction.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("CallerAwareComponentHostFunction")) {
          found = true;
          assertTrue(
              !nestedClass.isInterface(), "CallerAwareComponentHostFunction should be a class");
          assertTrue(
              ComponentHostFunction.class.isAssignableFrom(nestedClass),
              "CallerAwareComponentHostFunction should implement ComponentHostFunction");
          break;
        }
      }
      assertTrue(found, "Should have CallerAwareComponentHostFunction nested class");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("execute should declare WasmException")
    void executeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentHostFunction.class.getMethod("execute", List.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "execute should declare WasmException");
    }
  }
}

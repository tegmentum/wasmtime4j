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
 * Tests for {@link ComponentFunc} interface.
 *
 * <p>ComponentFunc represents a typed function exported by a WebAssembly component.
 */
@DisplayName("ComponentFunc Tests")
class ComponentFuncTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentFunc.class.getModifiers()), "ComponentFunc should be public");
      assertTrue(ComponentFunc.class.isInterface(), "ComponentFunc should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = ComponentFunc.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getParameterTypes method")
    void shouldHaveGetParameterTypesMethod() throws NoSuchMethodException {
      final Method method = ComponentFunc.class.getMethod("getParameterTypes");
      assertNotNull(method, "getParameterTypes method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getResultTypes method")
    void shouldHaveGetResultTypesMethod() throws NoSuchMethodException {
      final Method method = ComponentFunc.class.getMethod("getResultTypes");
      assertNotNull(method, "getResultTypes method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getParameterCount method")
    void shouldHaveGetParameterCountMethod() throws NoSuchMethodException {
      final Method method = ComponentFunc.class.getMethod("getParameterCount");
      assertNotNull(method, "getParameterCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertTrue(method.isDefault(), "Should be a default method");
    }

    @Test
    @DisplayName("should have getResultCount method")
    void shouldHaveGetResultCountMethod() throws NoSuchMethodException {
      final Method method = ComponentFunc.class.getMethod("getResultCount");
      assertNotNull(method, "getResultCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertTrue(method.isDefault(), "Should be a default method");
    }

    @Test
    @DisplayName("should have call method with varargs")
    void shouldHaveCallMethodWithVarargs() throws NoSuchMethodException {
      final Method method = ComponentFunc.class.getMethod("call", ComponentVal[].class);
      assertNotNull(method, "call method with varargs should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
      assertTrue(method.isVarArgs(), "Should be a varargs method");
    }

    @Test
    @DisplayName("should have call method with List")
    void shouldHaveCallMethodWithList() throws NoSuchMethodException {
      final Method method = ComponentFunc.class.getMethod("call", List.class);
      assertNotNull(method, "call method with List should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have call method with no arguments (default)")
    void shouldHaveCallMethodNoArgs() throws NoSuchMethodException {
      // The no-args call is a default method that delegates to call(List.of())
      // It's declared in the interface
      final Method[] methods = ComponentFunc.class.getMethods();
      boolean foundNoArgsCall = false;
      for (Method m : methods) {
        if (m.getName().equals("call") && m.getParameterCount() == 0) {
          foundNoArgsCall = true;
          assertTrue(m.isDefault(), "No-args call should be a default method");
          break;
        }
      }
      assertTrue(foundNoArgsCall, "Should have no-args call method");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = ComponentFunc.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("getParameterCount should be default method")
    void getParameterCountShouldBeDefaultMethod() throws NoSuchMethodException {
      final Method method = ComponentFunc.class.getMethod("getParameterCount");
      assertTrue(method.isDefault(), "getParameterCount should be a default method");
    }

    @Test
    @DisplayName("getResultCount should be default method")
    void getResultCountShouldBeDefaultMethod() throws NoSuchMethodException {
      final Method method = ComponentFunc.class.getMethod("getResultCount");
      assertTrue(method.isDefault(), "getResultCount should be a default method");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("call with varargs should declare WasmException")
    void callWithVarargsShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentFunc.class.getMethod("call", ComponentVal[].class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "call with varargs should declare WasmException");
    }

    @Test
    @DisplayName("call with List should declare WasmException")
    void callWithListShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentFunc.class.getMethod("call", List.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "call with List should declare WasmException");
    }
  }
}

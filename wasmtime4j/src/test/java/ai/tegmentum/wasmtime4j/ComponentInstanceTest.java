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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentInstance} interface.
 *
 * <p>ComponentInstance represents an instantiated WebAssembly component.
 */
@DisplayName("ComponentInstance Tests")
class ComponentInstanceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentInstance.class.getModifiers()),
          "ComponentInstance should be public");
      assertTrue(ComponentInstance.class.isInterface(), "ComponentInstance should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ComponentInstance.class),
          "ComponentInstance should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getComponent method")
    void shouldHaveGetComponentMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("getComponent");
      assertNotNull(method, "getComponent method should exist");
      assertEquals(ComponentSimple.class, method.getReturnType(), "Should return ComponentSimple");
    }

    @Test
    @DisplayName("should have getState method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(
          ComponentInstanceState.class,
          method.getReturnType(),
          "Should return ComponentInstanceState");
    }

    @Test
    @DisplayName("should have invoke method")
    void shouldHaveInvokeMethod() throws NoSuchMethodException {
      final Method method =
          ComponentInstance.class.getMethod("invoke", String.class, Object[].class);
      assertNotNull(method, "invoke method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
      assertTrue(method.isVarArgs(), "Should be a varargs method");
    }

    @Test
    @DisplayName("should have hasFunction method")
    void shouldHaveHasFunctionMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("hasFunction", String.class);
      assertNotNull(method, "hasFunction method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getFunc method")
    void shouldHaveGetFuncMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("getFunc", String.class);
      assertNotNull(method, "getFunc method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getExportedFunctions method")
    void shouldHaveGetExportedFunctionsMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("getExportedFunctions");
      assertNotNull(method, "getExportedFunctions method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getExportedInterfaces method")
    void shouldHaveGetExportedInterfacesMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("getExportedInterfaces");
      assertNotNull(method, "getExportedInterfaces method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have bindInterface method")
    void shouldHaveBindInterfaceMethod() throws NoSuchMethodException {
      final Method method =
          ComponentInstance.class.getMethod("bindInterface", String.class, Object.class);
      assertNotNull(method, "bindInterface method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          ComponentInstanceConfig.class,
          method.getReturnType(),
          "Should return ComponentInstanceConfig");
    }

    @Test
    @DisplayName("should have getResourceUsage method")
    void shouldHaveGetResourceUsageMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("getResourceUsage");
      assertNotNull(method, "getResourceUsage method should exist");
      assertEquals(
          ComponentResourceUsage.class,
          method.getReturnType(),
          "Should return ComponentResourceUsage");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have pause method")
    void shouldHavePauseMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("pause");
      assertNotNull(method, "pause method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have resume method")
    void shouldHaveResumeMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("resume");
      assertNotNull(method, "resume method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have stop method")
    void shouldHaveStopMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("stop");
      assertNotNull(method, "stop method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("invoke should declare WasmException")
    void invokeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          ComponentInstance.class.getMethod("invoke", String.class, Object[].class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "invoke should declare WasmException");
    }

    @Test
    @DisplayName("getFunc should declare WasmException")
    void getFuncShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("getFunc", String.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "getFunc should declare WasmException");
    }

    @Test
    @DisplayName("pause should declare WasmException")
    void pauseShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("pause");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "pause should declare WasmException");
    }

    @Test
    @DisplayName("resume should declare WasmException")
    void resumeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("resume");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "resume should declare WasmException");
    }

    @Test
    @DisplayName("stop should declare WasmException")
    void stopShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentInstance.class.getMethod("stop");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "stop should declare WasmException");
    }
  }
}

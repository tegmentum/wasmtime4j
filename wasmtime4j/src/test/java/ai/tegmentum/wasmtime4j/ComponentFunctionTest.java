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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentFunction} interface.
 *
 * <p>ComponentFunction represents a WebAssembly Component Model function that can be invoked.
 */
@DisplayName("ComponentFunction Tests")
class ComponentFunctionTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentFunction.class.getModifiers()),
          "ComponentFunction should be public");
      assertTrue(ComponentFunction.class.isInterface(), "ComponentFunction should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = ComponentFunction.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have call method with varargs")
    void shouldHaveCallMethodWithVarargs() throws NoSuchMethodException {
      final Method method = ComponentFunction.class.getMethod("call", Object[].class);
      assertNotNull(method, "call method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
      assertTrue(method.isVarArgs(), "Should be a varargs method");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = ComponentFunction.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getInstance method")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      final Method method = ComponentFunction.class.getMethod("getInstance");
      assertNotNull(method, "getInstance method should exist");
      assertEquals(
          ComponentInstance.class, method.getReturnType(), "Should return ComponentInstance");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("call should declare WasmException")
    void callShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentFunction.class.getMethod("call", Object[].class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "call should declare WasmException");
    }
  }
}

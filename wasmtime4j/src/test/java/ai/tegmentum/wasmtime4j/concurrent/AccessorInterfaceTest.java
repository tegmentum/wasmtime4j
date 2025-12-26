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

package ai.tegmentum.wasmtime4j.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Accessor} interface.
 *
 * <p>Accessor provides concurrent access to store data from multiple threads.
 */
@DisplayName("Accessor Interface Tests")
class AccessorInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Accessor.class.isInterface(), "Accessor should be an interface");
    }

    @Test
    @DisplayName("should have generic type parameter T")
    void shouldHaveGenericTypeParameterT() {
      final TypeVariable<?>[] typeParams = Accessor.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Should have one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("should have getData method")
    void shouldHaveGetDataMethod() throws NoSuchMethodException {
      final Method method = Accessor.class.getMethod("getData");
      assertNotNull(method, "getData method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (due to erasure)");
    }

    @Test
    @DisplayName("should have getData method with Class parameter")
    void shouldHaveGetDataMethodWithClassParameter() throws NoSuchMethodException {
      final Method method = Accessor.class.getMethod("getData", Class.class);
      assertNotNull(method, "getData(Class) method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (due to erasure)");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = Accessor.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getStoreId method")
    void shouldHaveGetStoreIdMethod() throws NoSuchMethodException {
      final Method method = Accessor.class.getMethod("getStoreId");
      assertNotNull(method, "getStoreId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have complete method")
    void shouldHaveCompleteMethod() throws NoSuchMethodException {
      final Method method = Accessor.class.getMethod("complete");
      assertNotNull(method, "complete method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have fail method")
    void shouldHaveFailMethod() throws NoSuchMethodException {
      final Method method = Accessor.class.getMethod("fail", Throwable.class);
      assertNotNull(method, "fail method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getData with Class should have one parameter")
    void getDataWithClassShouldHaveOneParameter() throws NoSuchMethodException {
      final Method method = Accessor.class.getMethod("getData", Class.class);
      assertEquals(1, method.getParameterCount(), "Should have one parameter");
      assertEquals(Class.class, method.getParameterTypes()[0], "Parameter should be Class");
    }

    @Test
    @DisplayName("fail should have Throwable parameter")
    void failShouldHaveThrowableParameter() throws NoSuchMethodException {
      final Method method = Accessor.class.getMethod("fail", Throwable.class);
      assertEquals(1, method.getParameterCount(), "Should have one parameter");
      assertEquals(Throwable.class, method.getParameterTypes()[0], "Parameter should be Throwable");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("getData with Class should be default method")
    void getDataWithClassShouldBeDefaultMethod() throws NoSuchMethodException {
      final Method method = Accessor.class.getMethod("getData", Class.class);
      assertTrue(method.isDefault(), "getData(Class) should be a default method");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("complete should declare WasmException")
    void completeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = Accessor.class.getMethod("complete");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasWasmException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "complete method should declare WasmException");
    }

    @Test
    @DisplayName("fail should declare WasmException")
    void failShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = Accessor.class.getMethod("fail", Throwable.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasWasmException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "fail method should declare WasmException");
    }
  }
}

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
import java.lang.reflect.TypeVariable;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentPipelineStream} interface.
 *
 * <p>ComponentPipelineStream represents a stream of data flowing through a component pipeline,
 * providing asynchronous methods for sending and receiving data.
 */
@DisplayName("ComponentPipelineStream Interface Tests")
class ComponentPipelineStreamTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ComponentPipelineStream.class.isInterface(),
          "ComponentPipelineStream should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      final Class<?>[] interfaces = ComponentPipelineStream.class.getInterfaces();
      boolean extendsAutoCloseable = false;
      for (final Class<?> iface : interfaces) {
        if (iface == AutoCloseable.class) {
          extendsAutoCloseable = true;
          break;
        }
      }
      assertTrue(extendsAutoCloseable, "Should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have generic type parameter T")
    void shouldHaveGenericTypeParameterT() {
      final TypeVariable<?>[] typeParams = ComponentPipelineStream.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Should have one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have send method")
    void shouldHaveSendMethod() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("send", Object.class);
      assertNotNull(method, "send method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have receive method")
    void shouldHaveReceiveMethod() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("receive");
      assertNotNull(method, "receive method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have isOpen method")
    void shouldHaveIsOpenMethod() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("isOpen");
      assertNotNull(method, "isOpen method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("send should have one parameter")
    void sendShouldHaveOneParameter() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("send", Object.class);
      assertEquals(1, method.getParameterCount(), "Should have one parameter");
      assertEquals(
          Object.class,
          method.getParameterTypes()[0],
          "Parameter should be Object (due to erasure)");
    }

    @Test
    @DisplayName("receive should have no parameters")
    void receiveShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("receive");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("isOpen should have no parameters")
    void isOpenShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("isOpen");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("close should have no parameters")
    void closeShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("close");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("send should declare WasmException")
    void sendShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("send", Object.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasWasmException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "send method should declare WasmException");
    }

    @Test
    @DisplayName("receive should declare WasmException")
    void receiveShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("receive");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasWasmException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "receive method should declare WasmException");
    }

    @Test
    @DisplayName("close should declare WasmException")
    void closeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("close");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasWasmException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "close method should declare WasmException");
    }

    @Test
    @DisplayName("isOpen should not declare any checked exceptions")
    void isOpenShouldNotDeclareCheckedExceptions() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("isOpen");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(0, exceptionTypes.length, "isOpen should not declare checked exceptions");
    }
  }

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("send should return CompletableFuture<Void>")
    void sendShouldReturnCompletableFutureVoid() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("send", Object.class);
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "send should return CompletableFuture");
    }

    @Test
    @DisplayName("receive should return CompletableFuture<T>")
    void receiveShouldReturnCompletableFutureT() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("receive");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "receive should return CompletableFuture");
    }

    @Test
    @DisplayName("isOpen should return boolean")
    void isOpenShouldReturnBoolean() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("isOpen");
      assertEquals(boolean.class, method.getReturnType(), "isOpen should return boolean");
    }

    @Test
    @DisplayName("close should return void")
    void closeShouldReturnVoid() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("close");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }
}

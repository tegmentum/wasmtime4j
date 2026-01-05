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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentPipelineStream} interface.
 *
 * <p>ComponentPipelineStream represents a stream of data flowing through a component pipeline.
 */
@DisplayName("ComponentPipelineStream Tests")
class ComponentPipelineStreamTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentPipelineStream.class.getModifiers()),
          "ComponentPipelineStream should be public");
      assertTrue(
          ComponentPipelineStream.class.isInterface(),
          "ComponentPipelineStream should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ComponentPipelineStream.class),
          "ComponentPipelineStream should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have type parameter T")
    void shouldHaveTypeParameterT() {
      final TypeVariable<?>[] typeParameters = ComponentPipelineStream.class.getTypeParameters();
      assertEquals(1, typeParameters.length, "Should have exactly one type parameter");
      assertEquals("T", typeParameters[0].getName(), "Type parameter should be named T");
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
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("send method should declare WasmException")
    void sendMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("send", Object.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.equals(WasmException.class)) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "send should declare WasmException");
    }

    @Test
    @DisplayName("receive method should declare WasmException")
    void receiveMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("receive");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.equals(WasmException.class)) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "receive should declare WasmException");
    }

    @Test
    @DisplayName("close method should declare WasmException")
    void closeMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ComponentPipelineStream.class.getMethod("close");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean hasWasmException = false;
      for (Class<?> exType : exceptionTypes) {
        if (exType.equals(WasmException.class)) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "close should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have exactly 4 declared methods")
    void shouldHaveExactlyFourDeclaredMethods() {
      final Method[] methods = ComponentPipelineStream.class.getDeclaredMethods();
      assertEquals(4, methods.length, "Should have exactly 4 declared methods");
    }
  }
}

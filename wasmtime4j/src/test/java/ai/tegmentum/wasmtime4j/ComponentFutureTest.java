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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentFuture} interface.
 *
 * <p>ComponentFuture represents an asynchronous value in the WebAssembly Component Model, providing
 * integration with WASI polling.
 */
@DisplayName("ComponentFuture Tests")
class ComponentFutureTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentFuture.class.getModifiers()),
          "ComponentFuture should be public");
      assertTrue(ComponentFuture.class.isInterface(), "ComponentFuture should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ComponentFuture.class),
          "ComponentFuture should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have type parameter")
    void shouldHaveTypeParameter() {
      assertEquals(
          1,
          ComponentFuture.class.getTypeParameters().length,
          "ComponentFuture should have one type parameter");
      assertEquals(
          "T",
          ComponentFuture.class.getTypeParameters()[0].getName(),
          "Type parameter should be named T");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getPayloadType method")
    void shouldHaveGetPayloadTypeMethod() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("getPayloadType");
      assertNotNull(method, "getPayloadType method should exist");
      assertEquals(WitType.class, method.getReturnType(), "Should return WitType");
    }

    @Test
    @DisplayName("should have isReady method")
    void shouldHaveIsReadyMethod() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("isReady");
      assertNotNull(method, "isReady method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have get method without parameters")
    void shouldHaveGetMethodWithoutParams() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("get");
      assertNotNull(method, "get method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have get method with timeout")
    void shouldHaveGetMethodWithTimeout() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("get", long.class, TimeUnit.class);
      assertNotNull(method, "get(timeout, unit) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getBlocking method")
    void shouldHaveGetBlockingMethod() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("getBlocking");
      assertNotNull(method, "getBlocking method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (T)");
    }

    @Test
    @DisplayName("should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
    }

    @Test
    @DisplayName("should have isDone method")
    void shouldHaveIsDoneMethod() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("isDone");
      assertNotNull(method, "isDone method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isCompletedExceptionally method")
    void shouldHaveIsCompletedExceptionallyMethod() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("isCompletedExceptionally");
      assertNotNull(method, "isCompletedExceptionally method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getException method")
    void shouldHaveGetExceptionMethod() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("getException");
      assertNotNull(method, "getException method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have toCompletableFuture method")
    void shouldHaveToCompletableFutureMethod() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("toCompletableFuture");
      assertNotNull(method, "toCompletableFuture method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have getState method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(ComponentFuture.State.class, method.getReturnType(), "Should return State");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("State Enum Tests")
  class StateEnumTests {

    @Test
    @DisplayName("should have PENDING state")
    void shouldHavePendingState() {
      final var state = ComponentFuture.State.PENDING;
      assertNotNull(state, "PENDING state should exist");
    }

    @Test
    @DisplayName("should have COMPLETED state")
    void shouldHaveCompletedState() {
      final var state = ComponentFuture.State.COMPLETED;
      assertNotNull(state, "COMPLETED state should exist");
    }

    @Test
    @DisplayName("should have FAILED state")
    void shouldHaveFailedState() {
      final var state = ComponentFuture.State.FAILED;
      assertNotNull(state, "FAILED state should exist");
    }

    @Test
    @DisplayName("should have CANCELLED state")
    void shouldHaveCancelledState() {
      final var state = ComponentFuture.State.CANCELLED;
      assertNotNull(state, "CANCELLED state should exist");
    }

    @Test
    @DisplayName("should have CLOSED state")
    void shouldHaveClosedState() {
      final var state = ComponentFuture.State.CLOSED;
      assertNotNull(state, "CLOSED state should exist");
    }

    @Test
    @DisplayName("should have exactly 5 states")
    void shouldHaveExactlyFiveStates() {
      assertEquals(5, ComponentFuture.State.values().length, "Should have exactly 5 states");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("get with timeout should have correct parameter types")
    void getWithTimeoutShouldHaveCorrectParameters() throws NoSuchMethodException {
      final Method method = ComponentFuture.class.getMethod("get", long.class, TimeUnit.class);
      final var params = method.getParameterTypes();

      assertEquals(2, params.length, "Should have 2 parameters");
      assertEquals(long.class, params[0], "First parameter should be long");
      assertEquals(TimeUnit.class, params[1], "Second parameter should be TimeUnit");
    }
  }

  @Nested
  @DisplayName("Interface Completeness Tests")
  class InterfaceCompletenessTests {

    @Test
    @DisplayName("should have all required methods")
    void shouldHaveAllRequiredMethods() {
      final var methods = ComponentFuture.class.getMethods();
      final var methodNames =
          java.util.Arrays.stream(methods)
              .map(Method::getName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(methodNames.contains("getPayloadType"), "Should have getPayloadType");
      assertTrue(methodNames.contains("isReady"), "Should have isReady");
      assertTrue(methodNames.contains("get"), "Should have get");
      assertTrue(methodNames.contains("getBlocking"), "Should have getBlocking");
      assertTrue(methodNames.contains("subscribe"), "Should have subscribe");
      assertTrue(methodNames.contains("isDone"), "Should have isDone");
      assertTrue(
          methodNames.contains("isCompletedExceptionally"), "Should have isCompletedExceptionally");
      assertTrue(methodNames.contains("getException"), "Should have getException");
      assertTrue(methodNames.contains("toCompletableFuture"), "Should have toCompletableFuture");
      assertTrue(methodNames.contains("getState"), "Should have getState");
      assertTrue(methodNames.contains("close"), "Should have close");
    }
  }
}

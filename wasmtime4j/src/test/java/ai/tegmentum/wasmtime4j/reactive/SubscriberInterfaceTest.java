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

package ai.tegmentum.wasmtime4j.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Subscriber} interface.
 *
 * <p>Subscriber is a Java 8 compatible reactive streams subscriber interface.
 */
@DisplayName("Subscriber Interface Tests")
class SubscriberInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Subscriber.class.isInterface(), "Subscriber should be an interface");
    }

    @Test
    @DisplayName("should have generic type parameter")
    void shouldHaveGenericTypeParameter() {
      final TypeVariable<?>[] typeParameters = Subscriber.class.getTypeParameters();
      assertEquals(1, typeParameters.length, "Should have one type parameter");
      assertEquals("T", typeParameters[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("should have onSubscribe method")
    void shouldHaveOnSubscribeMethod() throws NoSuchMethodException {
      final Method method = Subscriber.class.getMethod("onSubscribe", Subscription.class);
      assertNotNull(method, "onSubscribe method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have onNext method")
    void shouldHaveOnNextMethod() throws NoSuchMethodException {
      final Method method = Subscriber.class.getMethod("onNext", Object.class);
      assertNotNull(method, "onNext method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have onError method")
    void shouldHaveOnErrorMethod() throws NoSuchMethodException {
      final Method method = Subscriber.class.getMethod("onError", Throwable.class);
      assertNotNull(method, "onError method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have onComplete method")
    void shouldHaveOnCompleteMethod() throws NoSuchMethodException {
      final Method method = Subscriber.class.getMethod("onComplete");
      assertNotNull(method, "onComplete method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have exactly four methods")
    void shouldHaveExactlyFourMethods() {
      final Method[] methods = Subscriber.class.getDeclaredMethods();
      assertEquals(4, methods.length, "Subscriber should have exactly four methods");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("onSubscribe should accept Subscription parameter")
    void onSubscribeShouldAcceptSubscriptionParameter() throws NoSuchMethodException {
      final Method method = Subscriber.class.getMethod("onSubscribe", Subscription.class);
      final Class<?>[] parameterTypes = method.getParameterTypes();
      assertEquals(1, parameterTypes.length, "Should have one parameter");
      assertEquals(Subscription.class, parameterTypes[0], "Parameter should be Subscription");
    }

    @Test
    @DisplayName("onNext should accept generic type parameter")
    void onNextShouldAcceptGenericTypeParameter() throws NoSuchMethodException {
      final Method method = Subscriber.class.getMethod("onNext", Object.class);
      final Class<?>[] parameterTypes = method.getParameterTypes();
      assertEquals(1, parameterTypes.length, "Should have one parameter");
      // Due to type erasure, the parameter type is Object at runtime
      assertEquals(Object.class, parameterTypes[0], "Parameter should be Object (due to erasure)");
    }

    @Test
    @DisplayName("onError should accept Throwable parameter")
    void onErrorShouldAcceptThrowableParameter() throws NoSuchMethodException {
      final Method method = Subscriber.class.getMethod("onError", Throwable.class);
      final Class<?>[] parameterTypes = method.getParameterTypes();
      assertEquals(1, parameterTypes.length, "Should have one parameter");
      assertEquals(Throwable.class, parameterTypes[0], "Parameter should be Throwable");
    }

    @Test
    @DisplayName("onComplete should have no parameters")
    void onCompleteShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = Subscriber.class.getMethod("onComplete");
      final Class<?>[] parameterTypes = method.getParameterTypes();
      assertEquals(0, parameterTypes.length, "Should have no parameters");
    }
  }
}

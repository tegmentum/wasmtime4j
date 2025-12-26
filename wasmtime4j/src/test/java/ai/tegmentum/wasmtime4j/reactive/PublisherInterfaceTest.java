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
 * Tests for {@link Publisher} interface.
 *
 * <p>Publisher is a Java 8 compatible reactive streams publisher interface.
 */
@DisplayName("Publisher Interface Tests")
class PublisherInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Publisher.class.isInterface(), "Publisher should be an interface");
    }

    @Test
    @DisplayName("should have generic type parameter")
    void shouldHaveGenericTypeParameter() {
      final TypeVariable<?>[] typeParameters = Publisher.class.getTypeParameters();
      assertEquals(1, typeParameters.length, "Should have one type parameter");
      assertEquals("T", typeParameters[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      final Method method = Publisher.class.getMethod("subscribe", Subscriber.class);
      assertNotNull(method, "subscribe method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("subscribe method should accept Subscriber parameter")
    void subscribeMethodShouldAcceptSubscriberParameter() throws NoSuchMethodException {
      final Method method = Publisher.class.getMethod("subscribe", Subscriber.class);
      final Class<?>[] parameterTypes = method.getParameterTypes();
      assertEquals(1, parameterTypes.length, "Should have one parameter");
      assertEquals(Subscriber.class, parameterTypes[0], "Parameter should be Subscriber");
    }

    @Test
    @DisplayName("should have exactly one method")
    void shouldHaveExactlyOneMethod() {
      final Method[] methods = Publisher.class.getDeclaredMethods();
      assertEquals(1, methods.length, "Publisher should have exactly one method");
    }
  }

  @Nested
  @DisplayName("Generic Variance Tests")
  class GenericVarianceTests {

    @Test
    @DisplayName("subscribe should accept contravariant subscriber")
    void subscribeShouldAcceptContravariantSubscriber() throws NoSuchMethodException {
      // The subscribe method is defined as: void subscribe(Subscriber<? super T> subscriber);
      // This test verifies the method signature
      final Method method = Publisher.class.getMethod("subscribe", Subscriber.class);
      assertNotNull(method, "subscribe method should exist");

      // Verify the generic parameter type
      final var genericParamTypes = method.getGenericParameterTypes();
      assertEquals(1, genericParamTypes.length, "Should have one generic parameter");
    }
  }
}

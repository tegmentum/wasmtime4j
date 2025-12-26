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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Subscription} interface.
 *
 * <p>Subscription is a Java 8 compatible reactive streams subscription interface for flow control.
 */
@DisplayName("Subscription Interface Tests")
class SubscriptionInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Subscription.class.isInterface(), "Subscription should be an interface");
    }

    @Test
    @DisplayName("should have no type parameters")
    void shouldHaveNoTypeParameters() {
      assertEquals(
          0, Subscription.class.getTypeParameters().length, "Should have no type parameters");
    }

    @Test
    @DisplayName("should have request method")
    void shouldHaveRequestMethod() throws NoSuchMethodException {
      final Method method = Subscription.class.getMethod("request", long.class);
      assertNotNull(method, "request method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have cancel method")
    void shouldHaveCancelMethod() throws NoSuchMethodException {
      final Method method = Subscription.class.getMethod("cancel");
      assertNotNull(method, "cancel method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have exactly two methods")
    void shouldHaveExactlyTwoMethods() {
      final Method[] methods = Subscription.class.getDeclaredMethods();
      assertEquals(2, methods.length, "Subscription should have exactly two methods");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("request should accept long parameter")
    void requestShouldAcceptLongParameter() throws NoSuchMethodException {
      final Method method = Subscription.class.getMethod("request", long.class);
      final Class<?>[] parameterTypes = method.getParameterTypes();
      assertEquals(1, parameterTypes.length, "Should have one parameter");
      assertEquals(long.class, parameterTypes[0], "Parameter should be long");
    }

    @Test
    @DisplayName("cancel should have no parameters")
    void cancelShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = Subscription.class.getMethod("cancel");
      final Class<?>[] parameterTypes = method.getParameterTypes();
      assertEquals(0, parameterTypes.length, "Should have no parameters");
    }
  }

  @Nested
  @DisplayName("Reactive Streams Compatibility Tests")
  class ReactiveStreamsCompatibilityTests {

    @Test
    @DisplayName("should be compatible with reactive streams pattern")
    void shouldBeCompatibleWithReactiveStreamsPattern() {
      // Verify the interface follows the reactive streams pattern:
      // - request(n): Request n items from the publisher
      // - cancel(): Cancel the subscription

      // Check request method exists with correct signature
      try {
        final Method request = Subscription.class.getMethod("request", long.class);
        assertEquals(void.class, request.getReturnType(), "request should return void");
        assertEquals(1, request.getParameterCount(), "request should have one parameter");
      } catch (NoSuchMethodException e) {
        throw new AssertionError("request(long) method should exist", e);
      }

      // Check cancel method exists with correct signature
      try {
        final Method cancel = Subscription.class.getMethod("cancel");
        assertEquals(void.class, cancel.getReturnType(), "cancel should return void");
        assertEquals(0, cancel.getParameterCount(), "cancel should have no parameters");
      } catch (NoSuchMethodException e) {
        throw new AssertionError("cancel() method should exist", e);
      }
    }

    @Test
    @DisplayName("request parameter should be long for high-volume streams")
    void requestParameterShouldBeLongForHighVolumeStreams() throws NoSuchMethodException {
      // Reactive streams use long for request counts to support very large numbers
      final Method method = Subscription.class.getMethod("request", long.class);
      final Class<?> paramType = method.getParameterTypes()[0];
      assertEquals(
          long.class, paramType, "Parameter type should be long (not int) for high volume");
    }
  }
}
